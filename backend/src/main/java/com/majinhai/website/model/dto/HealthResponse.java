package com.majinhai.website.model.dto;

public record HealthResponse(
        String status,
        String service,
        String time
) {
}
