package com.majinhai.website.service;

import com.majinhai.website.model.dto.NoteAnnotationRequest;
import com.majinhai.website.model.dto.NoteAnnotationResponse;
import java.util.List;

public interface NoteAnnotationService {

    List<NoteAnnotationResponse> listByNoteId(Long noteId);

    NoteAnnotationResponse create(Long noteId, NoteAnnotationRequest request);

    NoteAnnotationResponse update(Long noteId, Long annotationId, NoteAnnotationRequest request);

    void delete(Long noteId, Long annotationId);
}
