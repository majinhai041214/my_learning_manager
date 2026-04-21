package com.majinhai.website.model.dto;

import com.majinhai.website.model.enums.StudyCategory;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CheckInResponse(
        Long id,
        LocalDate date,
        String title,
        String summary,
        StudyCategory category,
        Integer durationMinutes,
        OffsetDateTime createdAt
) {
}
