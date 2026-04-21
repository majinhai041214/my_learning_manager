package com.majinhai.website.repository;

import com.majinhai.website.model.entity.StudyNote;
import java.util.List;
import java.util.Optional;

public interface StudyNoteRepository {

    List<StudyNote> findAll();

    Optional<StudyNote> findById(Long id);

    StudyNote save(StudyNote studyNote);

    void deleteById(Long id);
}
