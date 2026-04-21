package com.majinhai.website.model.dto;

import java.util.List;

public record StudyNoteUpdateRequest(
        String title,
        String description,
        List<String> tags
) {
}
