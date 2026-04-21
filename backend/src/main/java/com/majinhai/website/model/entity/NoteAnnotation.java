package com.majinhai.website.model.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class NoteAnnotation {

    private Long id;
    private Long noteId;
    private String quotedText;
    private String comment;
    private Integer pageNumber;
    private Double anchorX;
    private Double anchorY;
    private List<NoteAnnotationSelectionRect> selectionRects = new ArrayList<>();
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public String getQuotedText() {
        return quotedText;
    }

    public void setQuotedText(String quotedText) {
        this.quotedText = quotedText;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Double getAnchorX() {
        return anchorX;
    }

    public void setAnchorX(Double anchorX) {
        this.anchorX = anchorX;
    }

    public Double getAnchorY() {
        return anchorY;
    }

    public void setAnchorY(Double anchorY) {
        this.anchorY = anchorY;
    }

    public List<NoteAnnotationSelectionRect> getSelectionRects() {
        return selectionRects;
    }

    public void setSelectionRects(List<NoteAnnotationSelectionRect> selectionRects) {
        this.selectionRects = selectionRects == null ? new ArrayList<>() : new ArrayList<>(selectionRects);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
