package com.majinhai.website.model.dto;

import java.util.List;

public record CheckInTagsUpdateRequest(
        List<String> tags
) {
}
