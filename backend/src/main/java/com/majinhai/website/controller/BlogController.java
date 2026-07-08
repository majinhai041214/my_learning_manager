package com.majinhai.website.controller;

import com.majinhai.website.model.dto.ApiResponse;
import com.majinhai.website.model.dto.BlogPostDetailResponse;
import com.majinhai.website.model.dto.BlogPostSummaryResponse;
import com.majinhai.website.service.BlogService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<BlogPostSummaryResponse>> listPosts() {
        return ApiResponse.success(
                "BLOG_POST_LIST_OK",
                "获取博客文章列表成功",
                blogService.listAll()
        );
    }

    @GetMapping("/posts/{slug}")
    public ApiResponse<BlogPostDetailResponse> getPost(@PathVariable String slug) {
        return ApiResponse.success(
                "BLOG_POST_DETAIL_OK",
                "获取博客文章详情成功",
                blogService.getBySlug(slug)
        );
    }

    @PostMapping("/posts/upload")
    public ApiResponse<BlogPostSummaryResponse> uploadPost(
            @RequestParam("post") MultipartFile post,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        return ApiResponse.success(
                "BLOG_POST_UPLOAD_OK",
                "博客文章上传成功",
                blogService.uploadPost(post, images)
        );
    }

    @GetMapping("/images/**")
    public ResponseEntity<Resource> viewImage(HttpServletRequest request) throws IOException {
        String prefix = request.getContextPath() + "/api/blog/images/";
        String requestUri = request.getRequestURI();
        String imagePath = requestUri.startsWith(prefix)
                ? URLDecoder.decode(requestUri.substring(prefix.length()), StandardCharsets.UTF_8)
                : "";
        Resource resource = blogService.loadImage(imagePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(resource.getFilename()))
                .contentType(resolveMediaType(resource))
                .body(resource);
    }

    private MediaType resolveMediaType(Resource resource) throws IOException {
        String filename = resource.getFilename();
        if (!StringUtils.hasText(filename)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String inlineDisposition(String filename) {
        return ContentDisposition.inline()
                .filename(filename == null ? "image" : filename, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
