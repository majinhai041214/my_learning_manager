package com.majinhai.website.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.majinhai.website.config.StorageProperties;
import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.entity.NoteAnnotation;
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
public class FileNoteAnnotationRepository implements NoteAnnotationRepository {

    private static final TypeReference<List<NoteAnnotation>> ANNOTATION_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Path metadataFile;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<NoteAnnotation> storage = new ArrayList<>();

    public FileNoteAnnotationRepository(StorageProperties storageProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.metadataFile = Path.of(storageProperties.getBaseDir(), storageProperties.getNotesDir(), "annotations.json");
        loadExistingData();
    }

    @Override
    public synchronized List<NoteAnnotation> findByNoteId(Long noteId) {
        return storage.stream()
                .filter(annotation -> annotation.getNoteId() != null && annotation.getNoteId().equals(noteId))
                .sorted(Comparator.comparing(NoteAnnotation::getCreatedAt))
                .toList();
    }

    @Override
    public synchronized Optional<NoteAnnotation> findById(Long id) {
        return storage.stream()
                .filter(annotation -> annotation.getId() != null && annotation.getId().equals(id))
                .findFirst();
    }

    @Override
    public synchronized NoteAnnotation save(NoteAnnotation annotation) {
        annotation.setId(idGenerator.getAndIncrement());
        storage.add(annotation);
        persist();
        return annotation;
    }

    @Override
    public synchronized NoteAnnotation update(NoteAnnotation annotation) {
        for (int index = 0; index < storage.size(); index++) {
            NoteAnnotation current = storage.get(index);
            if (current.getId() != null && current.getId().equals(annotation.getId())) {
                storage.set(index, annotation);
                persist();
                return annotation;
            }
        }

        throw new BusinessException("NOTE_ANNOTATION_NOT_FOUND", "未找到对应的笔记批注");
    }

    private void loadExistingData() {
        try {
            Files.createDirectories(metadataFile.getParent());

            if (Files.notExists(metadataFile) || Files.size(metadataFile) == 0L) {
                persist();
                return;
            }

            List<NoteAnnotation> savedAnnotations = objectMapper.readValue(metadataFile.toFile(), ANNOTATION_LIST_TYPE);
            storage.clear();
            storage.addAll(savedAnnotations);

            long maxId = storage.stream()
                    .map(NoteAnnotation::getId)
                    .filter(id -> id != null)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
            idGenerator.set(maxId + 1);
        } catch (IOException exception) {
            throw new BusinessException("NOTE_ANNOTATION_INIT_FAILED", "初始化笔记批注存储失败");
        }
    }

    private void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), storage);
        } catch (IOException exception) {
            throw new BusinessException("NOTE_ANNOTATION_WRITE_FAILED", "写入笔记批注存储失败");
        }
    }
}
