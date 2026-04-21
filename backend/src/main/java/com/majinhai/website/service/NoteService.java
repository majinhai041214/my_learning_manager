package com.majinhai.website.service;

import com.majinhai.website.model.dto.StudyNoteUpdateRequest;
import com.majinhai.website.model.dto.StudyNoteResponse;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface NoteService {

    List<StudyNoteResponse> listAll();

    StudyNoteResponse getById(Long id);

    StudyNoteResponse upload(MultipartFile file, String title, String description, String tags);

    StudyNoteResponse update(Long id, StudyNoteUpdateRequest request);

    void delete(Long id);

    Resource loadAsResource(Long id);
}
