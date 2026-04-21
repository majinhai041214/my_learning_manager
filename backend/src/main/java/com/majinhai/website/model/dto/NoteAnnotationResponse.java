package com.majinhai.website.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record NoteAnnotationResponse(
        Long id,
        Long noteId,
        String quotedText,
        String comment,
        Integer pageNumber,
        Double anchorX,
        Double anchorY,
        List<NoteAnnotationSelectionRect> selectionRects,
        OffsetDateTime createdAt
) {
}
