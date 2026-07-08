package com.majinhai.website.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ConfigurationProperties(prefix = "website.storage")
public class StorageProperties {

    private static final Logger log = LoggerFactory.getLogger(StorageProperties.class);

    private final AtomicBoolean prepared = new AtomicBoolean(false);

    private String baseDir = Path.of("..", "uploads").toString();
    private String notesDir = "notes";
    private String codeDir = "code";
    private String imagesDir = "images";
    private String blogPostsDir = "blog/posts";
    private String blogImagesDir = "blog/images";
    private String checkInsFile = "checkins/checkins.json";
    private String roadmapFile = "roadmap/algorithm-roadmap.json";

    public String getBaseDir() {
        ensurePrepared();
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = StringUtils.hasText(baseDir)
                ? baseDir.trim()
                : Path.of("..", "uploads").toString();
        prepared.set(false);
    }

    public String getNotesDir() {
        return notesDir;
    }

    public void setNotesDir(String notesDir) {
        this.notesDir = notesDir;
    }

    public String getCodeDir() {
        return codeDir;
    }

    public void setCodeDir(String codeDir) {
        this.codeDir = codeDir;
    }

    public String getImagesDir() {
        return imagesDir;
    }

    public void setImagesDir(String imagesDir) {
        this.imagesDir = imagesDir;
    }

    public String getBlogPostsDir() {
        return blogPostsDir;
    }

    public void setBlogPostsDir(String blogPostsDir) {
        this.blogPostsDir = blogPostsDir;
    }

    public String getBlogImagesDir() {
        return blogImagesDir;
    }

    public void setBlogImagesDir(String blogImagesDir) {
        this.blogImagesDir = blogImagesDir;
    }

    public String getCheckInsFile() {
        return checkInsFile;
    }

    public void setCheckInsFile(String checkInsFile) {
        this.checkInsFile = checkInsFile;
    }

    public String getRoadmapFile() {
        return roadmapFile;
    }

    public void setRoadmapFile(String roadmapFile) {
        this.roadmapFile = roadmapFile;
    }

    private void ensurePrepared() {
        if (prepared.get()) {
            return;
        }

        synchronized (this) {
            if (prepared.get()) {
                return;
            }

            Path targetBaseDir = Path.of(baseDir).toAbsolutePath().normalize();
            migrateLegacyDataIfNeeded(targetBaseDir);

            try {
                Files.createDirectories(targetBaseDir);
            } catch (IOException exception) {
                throw new IllegalStateException("无法初始化存储目录: " + targetBaseDir, exception);
            }

            baseDir = targetBaseDir.toString();
            prepared.set(true);
        }
    }

    private void migrateLegacyDataIfNeeded(Path targetBaseDir) {
        if (hasData(targetBaseDir)) {
            return;
        }

        Path legacySource = findLegacySource(targetBaseDir);
        if (legacySource == null) {
            return;
        }

        try {
            copyDirectoryRecursively(legacySource, targetBaseDir);
            log.info("Detected legacy storage directory at {} and copied data to {}", legacySource, targetBaseDir);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "无法将旧数据目录迁移到新的独立存储目录: " + legacySource + " -> " + targetBaseDir,
                    exception
            );
        }
    }

    private Path findLegacySource(Path targetBaseDir) {
        Path workingDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(workingDir.resolve("uploads").normalize());
        if (workingDir.getParent() != null) {
            candidates.add(workingDir.getParent().resolve("uploads").normalize());
        }
        candidates.add(workingDir.resolve("backend").resolve("uploads").normalize());

        for (Path candidate : candidates) {
            if (candidate.equals(targetBaseDir)) {
                continue;
            }
            if (hasData(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean hasData(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }

        try (Stream<Path> children = Files.list(directory)) {
            return children.findAny().isPresent();
        } catch (IOException exception) {
            return false;
        }
    }

    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.createDirectories(target);

        try (Stream<Path> paths = Files.walk(source)) {
            paths.forEach(path -> {
                Path relativePath = source.relativize(path);
                Path targetPath = target.resolve(relativePath);

                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("复制旧数据文件失败: " + path, exception);
                }
            });
        }
    }
}
