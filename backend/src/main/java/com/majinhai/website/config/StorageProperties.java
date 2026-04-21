package com.majinhai.website.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "website.storage")
public class StorageProperties {

    private String baseDir = "uploads";
    private String notesDir = "notes";
    private String codeDir = "code";
    private String imagesDir = "images";
    private String checkInsFile = "checkins/checkins.json";
    private String roadmapFile = "roadmap/algorithm-roadmap.json";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getNotesDir() {
        return notesDir;
    }

    public void setNotesDir(String notesDir) {
        this.notesDir = notesDir;
    }

    public String getCodeDir() {
        return codeDir;
    }

    public void setCodeDir(String codeDir) {
        this.codeDir = codeDir;
    }

    public String getImagesDir() {
        return imagesDir;
    }

    public void setImagesDir(String imagesDir) {
        this.imagesDir = imagesDir;
    }

    public String getCheckInsFile() {
        return checkInsFile;
    }

    public void setCheckInsFile(String checkInsFile) {
        this.checkInsFile = checkInsFile;
    }

    public String getRoadmapFile() {
        return roadmapFile;
    }

    public void setRoadmapFile(String roadmapFile) {
        this.roadmapFile = roadmapFile;
    }
}
