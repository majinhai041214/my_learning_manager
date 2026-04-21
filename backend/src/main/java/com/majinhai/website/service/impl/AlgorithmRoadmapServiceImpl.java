package com.majinhai.website.service.impl;

import com.majinhai.website.model.dto.AlgorithmRoadmapRequest;
import com.majinhai.website.model.dto.AlgorithmRoadmapResponse;
import com.majinhai.website.model.entity.AlgorithmRoadmap;
import com.majinhai.website.repository.AlgorithmRoadmapRepository;
import com.majinhai.website.service.AlgorithmRoadmapService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlgorithmRoadmapServiceImpl implements AlgorithmRoadmapService {

    private static final Set<String> ALLOWED_TONES = Set.of("cyan", "gold", "emerald", "violet", "rose", "slate");

    private final AlgorithmRoadmapRepository algorithmRoadmapRepository;

    public AlgorithmRoadmapServiceImpl(AlgorithmRoadmapRepository algorithmRoadmapRepository) {
        this.algorithmRoadmapRepository = algorithmRoadmapRepository;
    }

    @Override
    public AlgorithmRoadmapResponse get() {
        return toResponse(algorithmRoadmapRepository.get());
    }

    @Override
    public AlgorithmRoadmapResponse update(AlgorithmRoadmapRequest request) {
        AlgorithmRoadmap roadmap = new AlgorithmRoadmap();
        roadmap.setTitle(normalizeText(request.title(), "算法学习主路线", 80));
        roadmap.setSubtitle(normalizeText(request.subtitle(), "从基础到专题，再到综合能力", 200));
        roadmap.setBranches(normalizeBranches(request.branches()));
        roadmap.setMilestones(normalizeMilestones(request.milestones()));
        return toResponse(algorithmRoadmapRepository.save(roadmap));
    }

    private List<AlgorithmRoadmap.RoadmapBranch> normalizeBranches(
            List<AlgorithmRoadmapRequest.RoadmapBranchRequest> branches
    ) {
        List<AlgorithmRoadmap.RoadmapBranch> normalized = new ArrayList<>();
        if (branches == null) {
            return normalized;
        }

        for (AlgorithmRoadmapRequest.RoadmapBranchRequest branchRequest : branches) {
            if (branchRequest == null) {
                continue;
            }

            AlgorithmRoadmap.RoadmapBranch branch = new AlgorithmRoadmap.RoadmapBranch();
            branch.setId(normalizeId(branchRequest.id(), "branch"));
            branch.setTitle(normalizeText(branchRequest.title(), "未命名主线", 60));
            branch.setTone(normalizeTone(branchRequest.tone()));
            branch.setSummary(normalizeText(branchRequest.summary(), "这条主线还可以继续补充你的学习说明。", 240));
            branch.setGroups(normalizeGroups(branchRequest.groups()));
            normalized.add(branch);
        }

        return normalized;
    }

    private List<AlgorithmRoadmap.RoadmapGroup> normalizeGroups(
            List<AlgorithmRoadmapRequest.RoadmapGroupRequest> groups
    ) {
        List<AlgorithmRoadmap.RoadmapGroup> normalized = new ArrayList<>();
        if (groups == null) {
            return normalized;
        }

        for (AlgorithmRoadmapRequest.RoadmapGroupRequest groupRequest : groups) {
            if (groupRequest == null) {
                continue;
            }

            AlgorithmRoadmap.RoadmapGroup group = new AlgorithmRoadmap.RoadmapGroup();
            group.setId(normalizeId(groupRequest.id(), "group"));
            group.setName(normalizeText(groupRequest.name(), "未命名节点组", 60));
            group.setItems(normalizeItems(groupRequest.items()));
            normalized.add(group);
        }

        return normalized;
    }

    private List<String> normalizeItems(List<String> items) {
        List<String> normalized = new ArrayList<>();
        if (items == null) {
            return normalized;
        }

        for (String item : items) {
            String value = normalizeText(item, null, 80);
            if (StringUtils.hasText(value)) {
                normalized.add(value);
            }
        }

        return normalized;
    }

    private List<String> normalizeMilestones(List<String> milestones) {
        List<String> normalized = new ArrayList<>();
        if (milestones == null) {
            return normalized;
        }

        for (String milestone : milestones) {
            String value = normalizeText(milestone, null, 120);
            if (StringUtils.hasText(value)) {
                normalized.add(value);
            }
        }

        return normalized;
    }

    private String normalizeTone(String tone) {
        String normalizedTone = normalizeText(tone, "cyan", 20);
        return ALLOWED_TONES.contains(normalizedTone) ? normalizedTone : "cyan";
    }

    private String normalizeId(String value, String prefix) {
        String normalized = normalizeText(value, null, 80);
        return StringUtils.hasText(normalized) ? normalized : prefix + "-" + UUID.randomUUID();
    }

    private String normalizeText(String value, String fallback, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private AlgorithmRoadmapResponse toResponse(AlgorithmRoadmap roadmap) {
        return new AlgorithmRoadmapResponse(
                roadmap.getTitle(),
                roadmap.getSubtitle(),
                roadmap.getBranches().stream()
                        .map(branch -> new AlgorithmRoadmapResponse.RoadmapBranchResponse(
                                branch.getId(),
                                branch.getTitle(),
                                branch.getTone(),
                                branch.getSummary(),
                                branch.getGroups().stream()
                                        .map(group -> new AlgorithmRoadmapResponse.RoadmapGroupResponse(
                                                group.getId(),
                                                group.getName(),
                                                group.getItems()
                                        ))
                                        .toList()
                        ))
                        .toList(),
                roadmap.getMilestones()
        );
    }
}
