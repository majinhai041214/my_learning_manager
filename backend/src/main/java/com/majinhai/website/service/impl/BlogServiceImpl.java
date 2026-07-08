package com.majinhai.website.service.impl;

import com.majinhai.website.config.StorageProperties;
import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.dto.BlogPostDetailResponse;
import com.majinhai.website.model.dto.BlogPostSummaryResponse;
import com.majinhai.website.service.BlogService;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BlogServiceImpl implements BlogService {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("\\A---\\s*\\R([\\s\\S]*?)\\R---\\s*\\R?");
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\(([^)]+)\\)");
    private static final Pattern HTML_IMAGE_WRAPPER_PATTERN = Pattern.compile("(?is)<p[^>]*>\\s*(<img\\b[^>]+>)\\s*</p>");
    private static final Pattern HTML_IMAGE_TAG_PATTERN = Pattern.compile("<img\\b([^>]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_ATTRIBUTE_PATTERN = Pattern.compile("(src|alt)=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATED_FILENAME_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})-(.+)\\.(md|markdown)$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final Path postsDirectory;
    private final Path imagesDirectory;

    public BlogServiceImpl(StorageProperties storageProperties) {
        Path baseDir = Path.of(storageProperties.getBaseDir());
        this.postsDirectory = baseDir.resolve(storageProperties.getBlogPostsDir()).normalize();
        this.imagesDirectory = baseDir.resolve(storageProperties.getBlogImagesDir()).normalize();
        ensureBlogDirectories();
    }

    @Override
    public List<BlogPostSummaryResponse> listAll() {
        return readPosts().stream()
                .sorted(Comparator.comparing(
                        BlogPost::date,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(post -> new BlogPostSummaryResponse(
                        post.slug(),
                        post.title(),
                        post.date(),
                        post.tags(),
                        post.excerpt(),
                        post.sourceFilename()
                ))
                .toList();
    }

    @Override
    public BlogPostDetailResponse getBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);
        BlogPost post = readPosts().stream()
                .filter(item -> item.slug().equals(normalizedSlug))
                .findFirst()
                .orElseThrow(() -> new BusinessException("BLOG_POST_NOT_FOUND", "未找到对应的博客文章"));

        return new BlogPostDetailResponse(
                post.slug(),
                post.title(),
                post.date(),
                post.tags(),
                post.excerpt(),
                rewriteImageReferences(post.content()),
                post.sourceFilename()
        );
    }

    @Override
    public BlogPostSummaryResponse uploadPost(MultipartFile post, List<MultipartFile> images) {
        if (post == null || post.isEmpty()) {
            throw new BusinessException("BLOG_POST_FILE_REQUIRED", "请先选择要上传的 Markdown 博客文章");
        }

        String originalFilename = StringUtils.cleanPath(post.getOriginalFilename() == null ? "" : post.getOriginalFilename());
        if (!StringUtils.hasText(originalFilename) || !isMarkdownFilename(originalFilename)) {
            throw new BusinessException("BLOG_POST_FILE_UNSUPPORTED", "博客文章只支持 .md 或 .markdown 文件");
        }

        try {
            Files.createDirectories(postsDirectory);
            Files.createDirectories(imagesDirectory);

            String source = new String(post.getBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            String slug = resolveUploadSlug(source, originalFilename);
            String storedFilename = resolveStoredPostFilename(source, originalFilename, slug);
            List<MultipartFile> activeImages = images == null
                    ? List.of()
                    : images.stream().filter(image -> image != null && !image.isEmpty()).toList();

            if (!activeImages.isEmpty()) {
                String imageFolder = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
                Path targetImageDirectory = imagesDirectory.resolve(imageFolder).normalize();
                if (!targetImageDirectory.startsWith(imagesDirectory)) {
                    throw new BusinessException("BLOG_IMAGE_PATH_INVALID", "博客图片目录无效");
                }
                Files.createDirectories(targetImageDirectory);

                Map<String, String> uploadedImageTargets = new LinkedHashMap<>();
                for (MultipartFile image : activeImages) {
                    String imageName = sanitizeFilename(image.getOriginalFilename());
                    validateImageFilename(imageName);
                    Path targetImage = targetImageDirectory.resolve(imageName).normalize();
                    if (!targetImage.startsWith(targetImageDirectory)) {
                        throw new BusinessException("BLOG_IMAGE_PATH_INVALID", "博客图片文件名无效");
                    }
                    Files.copy(image.getInputStream(), targetImage, StandardCopyOption.REPLACE_EXISTING);
                    uploadedImageTargets.put(imageName.toLowerCase(Locale.ROOT), imageFolder + "/" + imageName);
                }

                source = rewriteUploadedImageReferences(source, uploadedImageTargets);
            }

            Path targetPost = postsDirectory.resolve(storedFilename).normalize();
            if (!targetPost.startsWith(postsDirectory)) {
                throw new BusinessException("BLOG_POST_PATH_INVALID", "博客文章文件名无效");
            }
            Files.writeString(targetPost, source, StandardCharsets.UTF_8);

            BlogPost saved = readPost(targetPost)
                    .orElseThrow(() -> new BusinessException("BLOG_POST_READ_FAILED", "博客文章已保存，但读取解析失败"));
            return new BlogPostSummaryResponse(
                    saved.slug(),
                    saved.title(),
                    saved.date(),
                    saved.tags(),
                    saved.excerpt(),
                    saved.sourceFilename()
            );
        } catch (IOException exception) {
            throw new BusinessException("BLOG_POST_UPLOAD_FAILED", "保存博客文章失败");
        }
    }

    @Override
    public Resource loadImage(String imagePath) {
        Path image = imagesDirectory.resolve(normalizeImagePath(imagePath)).normalize();
        if (!image.startsWith(imagesDirectory) || Files.notExists(image) || Files.isDirectory(image)) {
            throw new BusinessException("BLOG_IMAGE_NOT_FOUND", "未找到对应的博客图片");
        }
        return new PathResource(image);
    }

    private List<BlogPost> readPosts() {
        ensureBlogDirectories();

        try (Stream<Path> paths = Files.list(postsDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isMarkdownFile)
                    .map(this::readPost)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } catch (IOException exception) {
            throw new BusinessException("BLOG_POST_LIST_FAILED", "读取博客文章列表失败");
        }
    }

    private Optional<BlogPost> readPost(Path path) {
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
            ParsedMarkdown parsed = parseFrontmatter(source);
            String sourceFilename = path.getFileName().toString();
            LocalDate date = resolveDate(parsed.frontmatter(), sourceFilename);
            String fallbackTitle = stripMarkdownExtension(stripDatePrefix(sourceFilename));
            String title = frontmatterValue(parsed.frontmatter(), "title").orElse(fallbackTitle);
            List<String> tags = parseTags(parsed.frontmatter().get("tags"));
            String excerpt = frontmatterValue(parsed.frontmatter(), "excerpt")
                    .orElseGet(() -> createExcerpt(parsed.content()));
            String slug = normalizeSlug(frontmatterValue(parsed.frontmatter(), "slug")
                    .orElse(stripMarkdownExtension(stripDatePrefix(sourceFilename))));

            return Optional.of(new BlogPost(
                    slug,
                    cleanQuotedValue(title),
                    date,
                    tags,
                    excerpt,
                    parsed.content().trim(),
                    sourceFilename
            ));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private ParsedMarkdown parseFrontmatter(String source) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(source);
        if (!matcher.find()) {
            return new ParsedMarkdown(Map.of(), source);
        }

        Map<String, String> frontmatter = new LinkedHashMap<>();
        for (String line : matcher.group(1).split("\\R")) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();
            frontmatter.put(key, value);
        }

        return new ParsedMarkdown(frontmatter, source.substring(matcher.end()));
    }

    private LocalDate resolveDate(Map<String, String> frontmatter, String filename) {
        Optional<String> dateValue = frontmatterValue(frontmatter, "date");
        if (dateValue.isPresent()) {
            try {
                return LocalDate.parse(dateValue.get().trim());
            } catch (RuntimeException ignored) {
            }
        }

        Matcher matcher = DATED_FILENAME_PATTERN.matcher(filename);
        if (matcher.matches()) {
            try {
                return LocalDate.parse(matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3));
            } catch (RuntimeException ignored) {
            }
        }

        return null;
    }

    private List<String> parseTags(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return List.of();
        }

        String normalized = cleanQuotedValue(rawValue)
                .replace("[", "")
                .replace("]", "");
        Set<String> tags = new LinkedHashSet<>();
        for (String tag : normalized.split(",")) {
            String cleaned = cleanQuotedValue(tag).trim();
            if (StringUtils.hasText(cleaned)) {
                tags.add(cleaned);
            }
        }

        return List.copyOf(tags);
    }

    private String createExcerpt(String content) {
        String withoutMore = content.split("<!--more-->", 2)[0];
        String plain = withoutMore
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("!\\[[^\\]]*]\\([^)]+\\)", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("[#>*_`\\[\\]()]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (!StringUtils.hasText(plain)) {
            return "这篇文章暂时还没有摘要。";
        }
        return plain.length() > 160 ? plain.substring(0, 160).trim() + "..." : plain;
    }

    private String rewriteImageReferences(String content) {
        Matcher markdownMatcher = MARKDOWN_IMAGE_PATTERN.matcher(normalizeHtmlImages(content));
        StringBuilder markdownBuffer = new StringBuilder();
        while (markdownMatcher.find()) {
            String target = markdownMatcher.group(2).trim();
            markdownMatcher.appendReplacement(
                    markdownBuffer,
                    Matcher.quoteReplacement("![" + markdownMatcher.group(1) + "](" + resolveImageUrl(target) + ")")
            );
        }
        markdownMatcher.appendTail(markdownBuffer);

        return markdownBuffer.toString();
    }

    private String rewriteUploadedImageReferences(String content, Map<String, String> uploadedImageTargets) {
        String normalizedContent = normalizeHtmlImages(content);
        Matcher imageMatcher = MARKDOWN_IMAGE_PATTERN.matcher(normalizedContent);
        StringBuilder buffer = new StringBuilder();

        while (imageMatcher.find()) {
            String target = imageMatcher.group(2).trim();
            String imageName = extractImageFilename(target);
            String nextTarget = target;
            String uploadedTarget = uploadedImageTargets.get(imageName.toLowerCase(Locale.ROOT));
            if (!isExternalImageTarget(target) && uploadedTarget != null) {
                nextTarget = uploadedTarget;
            }

            imageMatcher.appendReplacement(
                    buffer,
                    Matcher.quoteReplacement("![" + imageMatcher.group(1) + "](" + nextTarget + ")")
            );
        }
        imageMatcher.appendTail(buffer);

        return buffer.toString();
    }

    private String normalizeHtmlImages(String content) {
        String withoutParagraphWrapper = HTML_IMAGE_WRAPPER_PATTERN.matcher(content).replaceAll("$1");
        Matcher imageMatcher = HTML_IMAGE_TAG_PATTERN.matcher(withoutParagraphWrapper);
        StringBuilder buffer = new StringBuilder();

        while (imageMatcher.find()) {
            String attributes = imageMatcher.group(1);
            String src = "";
            String alt = "";
            Matcher attributeMatcher = HTML_ATTRIBUTE_PATTERN.matcher(attributes);
            while (attributeMatcher.find()) {
                if ("src".equalsIgnoreCase(attributeMatcher.group(1))) {
                    src = attributeMatcher.group(2);
                } else if ("alt".equalsIgnoreCase(attributeMatcher.group(1))) {
                    alt = attributeMatcher.group(2);
                }
            }

            if (!StringUtils.hasText(src)) {
                continue;
            }

            imageMatcher.appendReplacement(
                    buffer,
                    Matcher.quoteReplacement("![" + alt + "](" + src + ")")
            );
        }
        imageMatcher.appendTail(buffer);

        return buffer.toString();
    }

    private String resolveImageUrl(String target) {
        if (!StringUtils.hasText(target) || isExternalImageTarget(target) || target.startsWith("/api/blog/images/")) {
            return target;
        }

        return "/api/blog/images/" + normalizeImagePath(target).replace("\\", "/");
    }

    private String normalizeImagePath(String target) {
        String cleaned = stripImageUrlSuffix(target).replace("\\", "/").trim();
        if (cleaned.startsWith("./")) {
            cleaned = cleaned.substring(2);
        }
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("images/")) {
            cleaned = cleaned.substring("images/".length());
        }
        if (cleaned.startsWith("image/")) {
            cleaned = cleaned.substring("image/".length());
        }
        return cleaned;
    }

    private boolean isExternalImageTarget(String target) {
        String normalized = target.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("data:")
                || normalized.startsWith("mailto:")
                || normalized.startsWith("#");
    }

    private String extractImageFilename(String target) {
        String normalizedPath = normalizeImagePath(target);
        int lastSlash = normalizedPath.lastIndexOf('/');
        return lastSlash >= 0 ? normalizedPath.substring(lastSlash + 1) : normalizedPath;
    }

    private String stripImageUrlSuffix(String target) {
        String cleaned = target == null ? "" : target.trim();
        int hashIndex = cleaned.indexOf('#');
        if (hashIndex >= 0) {
            cleaned = cleaned.substring(0, hashIndex);
        }
        int queryIndex = cleaned.indexOf('?');
        if (queryIndex >= 0) {
            cleaned = cleaned.substring(0, queryIndex);
        }
        try {
            return URLDecoder.decode(cleaned, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return cleaned;
        }
    }

    private Optional<String> frontmatterValue(Map<String, String> frontmatter, String key) {
        return Optional.ofNullable(frontmatter.get(key))
                .map(this::cleanQuotedValue)
                .filter(StringUtils::hasText);
    }

    private String cleanQuotedValue(String value) {
        String cleaned = value == null ? "" : value.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private String normalizeSlug(String value) {
        return cleanQuotedValue(value)
                .replaceAll("\\.(md|markdown)$", "")
                .replaceAll("\\s+", "-")
                .trim();
    }

    private String stripDatePrefix(String filename) {
        Matcher matcher = DATED_FILENAME_PATTERN.matcher(filename);
        return matcher.matches() ? matcher.group(4) + "." + matcher.group(5) : filename;
    }

    private String stripMarkdownExtension(String filename) {
        return filename.replaceAll("(?i)\\.(md|markdown)$", "");
    }

    private boolean isMarkdownFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return isMarkdownFilename(filename);
    }

    private boolean isMarkdownFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    private String resolveUploadSlug(String source, String originalFilename) {
        ParsedMarkdown parsed = parseFrontmatter(source);
        return normalizeSlug(frontmatterValue(parsed.frontmatter(), "slug")
                .orElse(stripMarkdownExtension(stripDatePrefix(originalFilename))));
    }

    private String resolveStoredPostFilename(String source, String originalFilename, String slug) {
        ParsedMarkdown parsed = parseFrontmatter(source);
        LocalDate date = resolveDate(parsed.frontmatter(), originalFilename);
        String extension = originalFilename.toLowerCase(Locale.ROOT).endsWith(".markdown") ? ".markdown" : ".md";
        if (date != null) {
            return date + "-" + slug + extension;
        }
        return sanitizeFilename(stripMarkdownExtension(originalFilename)) + extension;
    }

    private String sanitizeFilename(String filename) {
        String cleaned = StringUtils.cleanPath(filename == null ? "" : filename).replace("\\", "/");
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash >= 0) {
            cleaned = cleaned.substring(lastSlash + 1);
        }
        cleaned = cleaned.trim();
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..")) {
            throw new BusinessException("BLOG_FILENAME_INVALID", "文件名无效");
        }
        return cleaned.replaceAll("[\\r\\n]", "");
    }

    private void validateImageFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        int lastDot = lower.lastIndexOf('.');
        if (lastDot < 0 || lastDot == lower.length() - 1) {
            throw new BusinessException("BLOG_IMAGE_TYPE_UNSUPPORTED", "博客图片必须带有扩展名");
        }
        String extension = lower.substring(lastDot + 1);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("BLOG_IMAGE_TYPE_UNSUPPORTED", "博客图片仅支持 png、jpg、jpeg、gif、webp");
        }
    }

    private void ensureBlogDirectories() {
        try {
            Files.createDirectories(postsDirectory);
            Files.createDirectories(imagesDirectory);
        } catch (IOException exception) {
            throw new BusinessException("BLOG_STORAGE_INIT_FAILED", "初始化博客存储目录失败");
        }
    }

    private record ParsedMarkdown(Map<String, String> frontmatter, String content) {
    }

    private record BlogPost(
            String slug,
            String title,
            LocalDate date,
            List<String> tags,
            String excerpt,
            String content,
            String sourceFilename
    ) {
    }
}
