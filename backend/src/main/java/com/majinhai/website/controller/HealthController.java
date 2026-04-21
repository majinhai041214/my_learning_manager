package com.majinhai.website.controller;

import com.majinhai.website.model.dto.ApiResponse;
import com.majinhai.website.model.dto.HealthResponse;
import com.majinhai.website.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(
                "HEALTH_OK",
                "服务运行正常",
                healthService.getHealth()
        );
    }
}
