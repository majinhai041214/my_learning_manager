package com.majinhai.website.controller;

import com.majinhai.website.model.dto.ApiResponse;
import com.majinhai.website.model.dto.NoteAnnotationRequest;
import com.majinhai.website.model.dto.NoteAnnotationResponse;
import com.majinhai.website.model.dto.StudyNoteResponse;
import com.majinhai.website.model.dto.StudyNoteUpdateRequest;
import com.majinhai.website.service.NoteAnnotationService;
import com.majinhai.website.service.NoteService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final NoteAnnotationService noteAnnotationService;

    public NoteController(NoteService noteService, NoteAnnotationService noteAnnotationService) {
        this.noteService = noteService;
        this.noteAnnotationService = noteAnnotationService;
    }

    @GetMapping
    public ApiResponse<List<StudyNoteResponse>> list() {
        return ApiResponse.success(
                "NOTE_LIST_OK",
                "获取学习笔记列表成功",
                noteService.listAll()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<StudyNoteResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(
                "NOTE_DETAIL_OK",
                "获取学习笔记详情成功",
                noteService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<StudyNoteResponse> update(
            @PathVariable Long id,
            @RequestBody StudyNoteUpdateRequest request
    ) {
        return ApiResponse.success(
                "NOTE_UPDATED",
                "更新学习笔记成功",
                noteService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noteService.delete(id);
        return ApiResponse.success(
                "NOTE_DELETED",
                "删除学习笔记成功",
                null
        );
    }

    @GetMapping("/{id}/annotations")
    public ApiResponse<List<NoteAnnotationResponse>> listAnnotations(@PathVariable Long id) {
        return ApiResponse.success(
                "NOTE_ANNOTATION_LIST_OK",
                "获取笔记批注列表成功",
                noteAnnotationService.listByNoteId(id)
        );
    }

    @PostMapping("/upload")
    public ApiResponse<StudyNoteResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        return ApiResponse.success(
                "NOTE_UPLOAD_OK",
                "学习笔记上传成功",
                noteService.upload(file, title, description, tags)
        );
    }

    @PostMapping("/{id}/annotations")
    public ApiResponse<NoteAnnotationResponse> createAnnotation(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody NoteAnnotationRequest request
    ) {
        return ApiResponse.success(
                "NOTE_ANNOTATION_CREATED",
                "创建笔记批注成功",
                noteAnnotationService.create(id, request)
        );
    }

    @PutMapping("/{id}/annotations/{annotationId}")
    public ApiResponse<NoteAnnotationResponse> updateAnnotation(
            @PathVariable Long id,
            @PathVariable Long annotationId,
            @jakarta.validation.Valid @RequestBody NoteAnnotationRequest request
    ) {
        return ApiResponse.success(
                "NOTE_ANNOTATION_UPDATED",
                "更新笔记批注成功",
                noteAnnotationService.update(id, annotationId, request)
        );
    }

    @DeleteMapping("/{id}/annotations/{annotationId}")
    public ApiResponse<Void> deleteAnnotation(
            @PathVariable Long id,
            @PathVariable Long annotationId
    ) {
        noteAnnotationService.delete(id, annotationId);
        return ApiResponse.success(
                "NOTE_ANNOTATION_DELETED",
                "删除笔记批注成功",
                null
        );
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> viewContent(@PathVariable Long id) {
        StudyNoteResponse note = noteService.getById(id);
        Resource resource = noteService.loadAsResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(note.originalFilename()))
                .contentType(resolveMediaType(note.contentType()))
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        StudyNoteResponse note = noteService.getById(id);
        Resource resource = noteService.loadAsResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentDisposition(note.originalFilename()))
                .contentType(resolveMediaType(note.contentType()))
                .body(resource);
    }

    private MediaType resolveMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    private String inlineDisposition(String filename) {
        return ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private String attachmentDisposition(String filename) {
        return ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
