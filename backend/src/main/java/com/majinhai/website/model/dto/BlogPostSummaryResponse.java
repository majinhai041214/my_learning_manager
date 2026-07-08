package com.majinhai.website.model.dto;

import java.time.LocalDate;
import java.util.List;

public record BlogPostSummaryResponse(
        String slug,
        String title,
        LocalDate date,
        List<String> tags,
        String excerpt,
        String sourceFilename
) {
}
