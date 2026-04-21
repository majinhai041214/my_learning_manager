package com.majinhai.website.model.dto;

import java.time.OffsetDateTime;

public record NoteAnnotationResponse(
        Long id,
        Long noteId,
        String quotedText,
        String comment,
        Integer pageNumber,
        Double anchorX,
        Double anchorY,
        OffsetDateTime createdAt
) {
}
