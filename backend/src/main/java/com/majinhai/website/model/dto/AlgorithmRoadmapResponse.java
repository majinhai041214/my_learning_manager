package com.majinhai.website.model.dto;

import java.util.List;

public record AlgorithmRoadmapResponse(
        String title,
        String subtitle,
        List<RoadmapBranchResponse> branches,
        List<String> milestones
) {

    public record RoadmapBranchResponse(
            String id,
            String title,
            String tone,
            String summary,
            List<RoadmapGroupResponse> groups
    ) {
    }

    public record RoadmapGroupResponse(
            String id,
            String name,
            List<String> items
    ) {
    }
}
