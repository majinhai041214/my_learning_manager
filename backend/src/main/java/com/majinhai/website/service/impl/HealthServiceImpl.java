package com.majinhai.website.service.impl;

import com.majinhai.website.model.dto.HealthResponse;
import com.majinhai.website.service.HealthService;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public HealthResponse getHealth() {
        return new HealthResponse(
                "ok",
                "website-backend",
                OffsetDateTime.now().toString()
        );
    }
}
