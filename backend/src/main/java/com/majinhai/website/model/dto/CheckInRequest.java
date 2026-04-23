package com.majinhai.website.model.dto;

import com.majinhai.website.model.enums.StudyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CheckInRequest(
        @NotNull LocalDate date,
        @NotBlank String title,
        String summary,
        @NotNull StudyCategory category,
        @Positive
        Integer durationMinutes,
        List<String> tags
) {
}
