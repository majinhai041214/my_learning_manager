package com.majinhai.website.repository;

import com.majinhai.website.model.entity.NoteAnnotation;
import java.util.List;
import java.util.Optional;

public interface NoteAnnotationRepository {

    List<NoteAnnotation> findByNoteId(Long noteId);

    Optional<NoteAnnotation> findById(Long id);

    NoteAnnotation save(NoteAnnotation annotation);

    NoteAnnotation update(NoteAnnotation annotation);
}
