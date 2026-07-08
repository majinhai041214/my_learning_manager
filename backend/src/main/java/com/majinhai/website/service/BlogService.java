package com.majinhai.website.service;

import com.majinhai.website.model.dto.BlogPostDetailResponse;
import com.majinhai.website.model.dto.BlogPostSummaryResponse;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {

    List<BlogPostSummaryResponse> listAll();

    BlogPostDetailResponse getBySlug(String slug);

    BlogPostSummaryResponse uploadPost(MultipartFile post, List<MultipartFile> images);

    Resource loadImage(String imagePath);
}
