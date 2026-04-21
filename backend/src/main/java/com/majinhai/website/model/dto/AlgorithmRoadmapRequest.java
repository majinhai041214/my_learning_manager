package com.majinhai.website.model.dto;

import java.util.List;

public record AlgorithmRoadmapRequest(
        String title,
        String subtitle,
        List<RoadmapBranchRequest> branches,
        List<String> milestones
) {

    public record RoadmapBranchRequest(
            String id,
            String title,
            String tone,
            String summary,
            List<RoadmapGroupRequest> groups
    ) {
    }

    public record RoadmapGroupRequest(
            String id,
            String name,
            List<String> items
    ) {
    }
}
