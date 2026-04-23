package com.majinhai.website.service.impl;

import com.majinhai.website.config.StorageProperties;
import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.dto.StudyNoteContentUpdateRequest;
import com.majinhai.website.model.dto.StudyNoteResponse;
import com.majinhai.website.model.dto.StudyNoteUpdateRequest;
import com.majinhai.website.model.entity.StudyNote;
import com.majinhai.website.repository.NoteAnnotationRepository;
import com.majinhai.website.repository.StudyNoteRepository;
import com.majinhai.website.service.NoteService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoteServiceImpl implements NoteService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "md", "markdown", "pdf", "txt", "cpp", "cc", "cxx", "c", "h", "hpp", "py", "java", "js", "ts"
    );

    private final StudyNoteRepository studyNoteRepository;
    private final NoteAnnotationRepository noteAnnotationRepository;
    private final Path notesDirectory;

    public NoteServiceImpl(
            StudyNoteRepository studyNoteRepository,
            NoteAnnotationRepository noteAnnotationRepository,
            StorageProperties storageProperties
    ) {
        this.studyNoteRepository = studyNoteRepository;
        this.noteAnnotationRepository = noteAnnotationRepository;
        this.notesDirectory = Path.of(storageProperties.getBaseDir(), storageProperties.getNotesDir(), "files");
    }

    @Override
    public List<StudyNoteResponse> listAll() {
        return studyNoteRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public StudyNoteResponse getById(Long id) {
        return toResponse(findNote(id));
    }

    @Override
    public StudyNoteResponse upload(MultipartFile file, String title, String description, String tags) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("NOTE_FILE_REQUIRED", "请先选择要上传的学习笔记文件");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException("NOTE_FILENAME_INVALID", "文件名无效，请重新选择文件");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("NOTE_FILE_TYPE_UNSUPPORTED", "当前仅支持 markdown、pdf、cpp、py、java 等常见学习笔记文件");
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetPath = notesDirectory.resolve(storedFilename);

        try {
            Files.createDirectories(notesDirectory);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException("NOTE_UPLOAD_FAILED", "保存学习笔记文件失败");
        }

        StudyNote studyNote = new StudyNote();
        studyNote.setTitle(StringUtils.hasText(title) ? title.trim() : stripExtension(originalFilename));
        studyNote.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        studyNote.setOriginalFilename(originalFilename);
        studyNote.setStoredFilename(storedFilename);
        studyNote.setRelativePath(Path.of("notes", "files", storedFilename).toString().replace("\\", "/"));
        studyNote.setExtension(extension);
        studyNote.setContentType(resolveContentType(file, targetPath, extension));
        studyNote.setSize(file.getSize());
        studyNote.setUploadedAt(OffsetDateTime.now());
        studyNote.setTags(parseTags(tags));

        return toResponse(studyNoteRepository.save(studyNote));
    }

    @Override
    public StudyNoteResponse update(Long id, StudyNoteUpdateRequest request) {
        StudyNote note = findNote(id);
        note.setTitle(normalizeTitle(request.title(), note.getOriginalFilename()));
        note.setDescription(normalizeDescription(request.description()));
        note.setTags(normalizeTags(request.tags()));
        return toResponse(studyNoteRepository.save(note));
    }

    @Override
    public StudyNoteResponse updateContent(Long id, StudyNoteContentUpdateRequest request) {
        StudyNote note = findNote(id);
        if (!isEditableMarkdown(note.getExtension())) {
            throw new BusinessException("NOTE_CONTENT_UPDATE_UNSUPPORTED", "当前仅支持直接编辑 Markdown 学习笔记");
        }

        Path resourcePath = notesDirectory.resolve(note.getStoredFilename()).normalize();
        if (!Files.exists(resourcePath)) {
            throw new BusinessException("NOTE_FILE_NOT_FOUND", "学习笔记文件不存在或已被移动");
        }

        String normalizedContent = request == null || request.content() == null
                ? ""
                : request.content().replace("\r\n", "\n");

        try {
            Files.writeString(resourcePath, normalizedContent, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException("NOTE_CONTENT_UPDATE_FAILED", "保存学习笔记正文失败");
        }

        try {
            note.setSize(Files.size(resourcePath));
        } catch (IOException ignored) {
            note.setSize(normalizedContent.getBytes(StandardCharsets.UTF_8).length);
        }

        return toResponse(studyNoteRepository.save(note));
    }

    @Override
    public void delete(Long id) {
        StudyNote note = findNote(id);
        Path resourcePath = notesDirectory.resolve(note.getStoredFilename()).normalize();

        noteAnnotationRepository.deleteByNoteId(id);
        studyNoteRepository.deleteById(id);

        try {
            Files.deleteIfExists(resourcePath);
        } catch (IOException exception) {
            throw new BusinessException("NOTE_DELETE_FILE_FAILED", "学习笔记记录已删除，但文件清理失败");
        }
    }

    @Override
    public Resource loadAsResource(Long id) {
        StudyNote note = findNote(id);
        Path resourcePath = notesDirectory.resolve(note.getStoredFilename()).normalize();
        if (!Files.exists(resourcePath)) {
            throw new BusinessException("NOTE_FILE_NOT_FOUND", "学习笔记文件不存在或已被移动");
        }
        return new PathResource(resourcePath);
    }

    private StudyNote findNote(Long id) {
        return studyNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTE_NOT_FOUND", "未找到对应的学习笔记"));
    }

    private StudyNoteResponse toResponse(StudyNote note) {
        return new StudyNoteResponse(
                note.getId(),
                note.getTitle(),
                note.getDescription(),
                note.getOriginalFilename(),
                note.getExtension(),
                note.getContentType(),
                note.getSize(),
                note.getUploadedAt(),
                note.getTags(),
                "/api/notes/" + note.getId() + "/content",
                "/api/notes/" + note.getId() + "/download"
        );
    }

    private List<String> parseTags(String rawTags) {
        if (!StringUtils.hasText(rawTags)) {
            return List.of();
        }

        return rawTags.lines()
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::normalizeTag)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private String normalizeTag(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 24) {
            return normalized.substring(0, 24).trim();
        }
        return normalized;
    }

    private String normalizeTitle(String title, String originalFilename) {
        String fallback = stripExtension(originalFilename);
        if (!StringUtils.hasText(title)) {
            return fallback;
        }
        String normalized = title.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return fallback;
        }
        return normalized.length() > 80 ? normalized.substring(0, 80).trim() : normalized;
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String normalized = description.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() > 240 ? normalized.substring(0, 240).trim() : normalized;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeTag)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            throw new BusinessException("NOTE_FILE_TYPE_UNSUPPORTED", "请上传带扩展名的学习笔记文件");
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }

    private String resolveContentType(MultipartFile file, Path targetPath, String extension) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }

        try {
            String detected = Files.probeContentType(targetPath);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignored) {
        }

        return switch (extension) {
            case "md", "markdown" -> "text/markdown";
            case "py", "java", "cpp", "cc", "cxx", "c", "h", "hpp", "js", "ts", "txt" -> "text/plain";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private boolean isEditableMarkdown(String extension) {
        return "md".equals(extension) || "markdown".equals(extension);
    }

}
