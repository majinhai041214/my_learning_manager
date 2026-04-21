package com.majinhai.website.model.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteAnnotationRequest(
        String quotedText,
        @NotBlank String comment,
        Integer pageNumber,
        Double anchorX,
        Double anchorY
) {
}
