package com.majinhai.website.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record StudyNoteResponse(
        Long id,
        String title,
        String description,
        String originalFilename,
        String extension,
        String contentType,
        long size,
        OffsetDateTime uploadedAt,
        List<String> tags,
        String viewUrl,
        String downloadUrl
) {
}
