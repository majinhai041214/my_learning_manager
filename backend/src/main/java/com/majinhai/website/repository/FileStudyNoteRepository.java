package com.majinhai.website.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.majinhai.website.config.StorageProperties;
import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.entity.StudyNote;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class FileStudyNoteRepository implements StudyNoteRepository {

    private static final TypeReference<List<StudyNote>> STUDY_NOTE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Path metadataFile;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<StudyNote> storage = new ArrayList<>();

    public FileStudyNoteRepository(StorageProperties storageProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.metadataFile = Path.of(storageProperties.getBaseDir(), storageProperties.getNotesDir(), "notes.json");
        loadExistingData();
    }

    @Override
    public synchronized List<StudyNote> findAll() {
        return storage.stream()
                .sorted(Comparator.comparing(StudyNote::getUploadedAt).reversed())
                .toList();
    }

    @Override
    public synchronized Optional<StudyNote> findById(Long id) {
        return storage.stream()
                .filter(note -> note.getId() != null && note.getId().equals(id))
                .findFirst();
    }

    @Override
    public synchronized StudyNote save(StudyNote studyNote) {
        if (studyNote.getId() == null) {
            studyNote.setId(idGenerator.getAndIncrement());
            storage.add(studyNote);
        } else {
            storage.removeIf(item -> item.getId() != null && item.getId().equals(studyNote.getId()));
            storage.add(studyNote);
        }
        persist();
        return studyNote;
    }

    @Override
    public synchronized void deleteById(Long id) {
        boolean removed = storage.removeIf(note -> note.getId() != null && note.getId().equals(id));
        if (!removed) {
            throw new BusinessException("NOTE_NOT_FOUND", "未找到对应的学习笔记");
        }
        persist();
    }

    private void loadExistingData() {
        try {
            Files.createDirectories(metadataFile.getParent());

            if (Files.notExists(metadataFile) || Files.size(metadataFile) == 0L) {
                persist();
                return;
            }

            List<StudyNote> savedNotes = objectMapper.readValue(metadataFile.toFile(), STUDY_NOTE_LIST_TYPE);
            storage.clear();
            storage.addAll(savedNotes);

            long maxId = storage.stream()
                    .map(StudyNote::getId)
                    .filter(id -> id != null)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
            idGenerator.set(maxId + 1);
        } catch (IOException exception) {
            throw new BusinessException("NOTE_STORAGE_INIT_FAILED", "初始化学习笔记存储失败");
        }
    }

    private void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), storage);
        } catch (IOException exception) {
            throw new BusinessException("NOTE_STORAGE_WRITE_FAILED", "写入学习笔记存储失败");
        }
    }
}
