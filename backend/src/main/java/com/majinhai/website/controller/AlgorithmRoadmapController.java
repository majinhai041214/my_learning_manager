package com.majinhai.website.controller;

import com.majinhai.website.model.dto.AlgorithmRoadmapRequest;
import com.majinhai.website.model.dto.AlgorithmRoadmapResponse;
import com.majinhai.website.model.dto.ApiResponse;
import com.majinhai.website.service.AlgorithmRoadmapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/algorithms/roadmap")
public class AlgorithmRoadmapController {

    private final AlgorithmRoadmapService algorithmRoadmapService;

    public AlgorithmRoadmapController(AlgorithmRoadmapService algorithmRoadmapService) {
        this.algorithmRoadmapService = algorithmRoadmapService;
    }

    @GetMapping
    public ApiResponse<AlgorithmRoadmapResponse> getRoadmap() {
        return ApiResponse.success(
                "ALGORITHM_ROADMAP_OK",
                "获取算法学习路线图成功",
                algorithmRoadmapService.get()
        );
    }

    @PutMapping
    public ApiResponse<AlgorithmRoadmapResponse> updateRoadmap(@RequestBody AlgorithmRoadmapRequest request) {
        return ApiResponse.success(
                "ALGORITHM_ROADMAP_UPDATED",
                "更新算法学习路线图成功",
                algorithmRoadmapService.update(request)
        );
    }
}
