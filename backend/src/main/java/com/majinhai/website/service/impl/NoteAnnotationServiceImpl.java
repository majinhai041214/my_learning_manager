package com.majinhai.website.service.impl;

import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.dto.NoteAnnotationRequest;
import com.majinhai.website.model.dto.NoteAnnotationResponse;
import com.majinhai.website.model.entity.NoteAnnotation;
import com.majinhai.website.repository.NoteAnnotationRepository;
import com.majinhai.website.repository.StudyNoteRepository;
import com.majinhai.website.service.NoteAnnotationService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NoteAnnotationServiceImpl implements NoteAnnotationService {

    private final NoteAnnotationRepository noteAnnotationRepository;
    private final StudyNoteRepository studyNoteRepository;

    public NoteAnnotationServiceImpl(
            NoteAnnotationRepository noteAnnotationRepository,
            StudyNoteRepository studyNoteRepository
    ) {
        this.noteAnnotationRepository = noteAnnotationRepository;
        this.studyNoteRepository = studyNoteRepository;
    }

    @Override
    public List<NoteAnnotationResponse> listByNoteId(Long noteId) {
        ensureNoteExists(noteId);
        return noteAnnotationRepository.findByNoteId(noteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public NoteAnnotationResponse create(Long noteId, NoteAnnotationRequest request) {
        ensureNoteExists(noteId);

        String quotedText = normalizeQuotedText(request);
        String comment = normalizeComment(request);

        NoteAnnotation annotation = new NoteAnnotation();
        annotation.setNoteId(noteId);
        annotation.setQuotedText(quotedText);
        annotation.setComment(comment);
        annotation.setPageNumber(request.pageNumber());
        annotation.setAnchorX(normalizeAnchorCoordinate(request.anchorX(), "X"));
        annotation.setAnchorY(normalizeAnchorCoordinate(request.anchorY(), "Y"));
        annotation.setCreatedAt(OffsetDateTime.now());

        return toResponse(noteAnnotationRepository.save(annotation));
    }

    @Override
    public NoteAnnotationResponse update(Long noteId, Long annotationId, NoteAnnotationRequest request) {
        ensureNoteExists(noteId);

        NoteAnnotation annotation = noteAnnotationRepository.findById(annotationId)
                .orElseThrow(() -> new BusinessException("NOTE_ANNOTATION_NOT_FOUND", "未找到对应的笔记批注"));

        if (!noteId.equals(annotation.getNoteId())) {
            throw new BusinessException("NOTE_ANNOTATION_NOT_FOUND", "未找到对应的笔记批注");
        }

        annotation.setQuotedText(normalizeQuotedText(request));
        annotation.setComment(normalizeComment(request));
        annotation.setPageNumber(request.pageNumber());
        annotation.setAnchorX(normalizeAnchorCoordinate(request.anchorX(), "X"));
        annotation.setAnchorY(normalizeAnchorCoordinate(request.anchorY(), "Y"));

        return toResponse(noteAnnotationRepository.update(annotation));
    }

    private void ensureNoteExists(Long noteId) {
        if (studyNoteRepository.findById(noteId).isEmpty()) {
            throw new BusinessException("NOTE_NOT_FOUND", "未找到对应的学习笔记");
        }
    }

    private String normalizeQuotedText(NoteAnnotationRequest request) {
        String quotedText = request.quotedText() == null ? "" : request.quotedText().trim();
        if (!StringUtils.hasText(quotedText)) {
            if (request.pageNumber() != null && request.anchorX() != null && request.anchorY() != null) {
                return "";
            }
            throw new BusinessException("NOTE_ANNOTATION_QUOTE_REQUIRED", "请先选择需要批注的内容");
        }
        return quotedText.length() > 300 ? quotedText.substring(0, 300) : quotedText;
    }

    private String normalizeComment(NoteAnnotationRequest request) {
        String comment = request.comment().trim();
        if (!StringUtils.hasText(comment)) {
            throw new BusinessException("NOTE_ANNOTATION_COMMENT_REQUIRED", "请输入批注内容");
        }
        return comment.length() > 1000 ? comment.substring(0, 1000) : comment;
    }

    private Double normalizeAnchorCoordinate(Double value, String axisName) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 1) {
            throw new BusinessException("NOTE_ANNOTATION_ANCHOR_INVALID", "PDF 批注锚点 " + axisName + " 坐标无效");
        }
        return value;
    }

    private NoteAnnotationResponse toResponse(NoteAnnotation annotation) {
        return new NoteAnnotationResponse(
                annotation.getId(),
                annotation.getNoteId(),
                annotation.getQuotedText(),
                annotation.getComment(),
                annotation.getPageNumber(),
                annotation.getAnchorX(),
                annotation.getAnchorY(),
                annotation.getCreatedAt()
        );
    }
}
