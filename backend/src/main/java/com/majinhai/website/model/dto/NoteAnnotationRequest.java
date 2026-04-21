package com.majinhai.website.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record NoteAnnotationRequest(
        String quotedText,
        @NotBlank String comment,
        Integer pageNumber,
        Double anchorX,
        Double anchorY,
        List<NoteAnnotationSelectionRect> selectionRects
) {
}
