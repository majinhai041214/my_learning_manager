package com.majinhai.website.model.entity;

import java.util.ArrayList;
import java.util.List;

public class AlgorithmRoadmap {

    private String title;
    private String subtitle;
    private List<RoadmapBranch> branches = new ArrayList<>();
    private List<String> milestones = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public List<RoadmapBranch> getBranches() {
        return branches;
    }

    public void setBranches(List<RoadmapBranch> branches) {
        this.branches = branches;
    }

    public List<String> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<String> milestones) {
        this.milestones = milestones;
    }

    public static class RoadmapBranch {

        private String id;
        private String title;
        private String tone;
        private String summary;
        private List<RoadmapGroup> groups = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTone() {
            return tone;
        }

        public void setTone(String tone) {
            this.tone = tone;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<RoadmapGroup> getGroups() {
            return groups;
        }

        public void setGroups(List<RoadmapGroup> groups) {
            this.groups = groups;
        }
    }

    public static class RoadmapGroup {

        private String id;
        private String name;
        private List<String> items = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }
    }
}
