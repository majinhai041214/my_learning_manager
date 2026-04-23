package com.majinhai.website.model.dto;

import com.majinhai.website.model.enums.StudyCategory;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CheckInResponse(
        Long id,
        LocalDate date,
        String title,
        String summary,
        StudyCategory category,
        Integer durationMinutes,
        List<String> tags,
        OffsetDateTime createdAt
) {
}
