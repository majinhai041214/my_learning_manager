package com.majinhai.website.service.impl;

import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.dto.CheckInRequest;
import com.majinhai.website.model.dto.CheckInResponse;
import com.majinhai.website.model.dto.CheckInTagsUpdateRequest;
import com.majinhai.website.model.entity.CheckIn;
import com.majinhai.website.repository.CheckInRepository;
import com.majinhai.website.service.CheckInService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CheckInServiceImpl implements CheckInService {

    private final CheckInRepository checkInRepository;

    public CheckInServiceImpl(CheckInRepository checkInRepository) {
        this.checkInRepository = checkInRepository;
    }

    @Override
    public List<CheckInResponse> listAll() {
        return checkInRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CheckInResponse create(CheckInRequest request) {
        CheckIn checkIn = new CheckIn();
        applyRequest(checkIn, request);
        checkIn.setCreatedAt(OffsetDateTime.now());
        return toResponse(checkInRepository.save(checkIn));
    }

    @Override
    public CheckInResponse update(Long id, CheckInRequest request) {
        CheckIn checkIn = findCheckIn(id);
        applyRequest(checkIn, request);
        return toResponse(checkInRepository.update(checkIn));
    }

    @Override
    public CheckInResponse updateTags(Long id, CheckInTagsUpdateRequest request) {
        CheckIn checkIn = findCheckIn(id);
        checkIn.setTags(normalizeTags(request.tags()));
        return toResponse(checkInRepository.update(checkIn));
    }

    @Override
    public void delete(Long id) {
        checkInRepository.deleteById(id);
    }

    private CheckInResponse toResponse(CheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getDate(),
                checkIn.getTitle(),
                checkIn.getSummary(),
                checkIn.getCategory(),
                checkIn.getDurationMinutes(),
                checkIn.getTags(),
                checkIn.getCreatedAt()
        );
    }

    private CheckIn findCheckIn(Long id) {
        return checkInRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CHECKIN_NOT_FOUND", "未找到对应的打卡记录"));
    }

    private void applyRequest(CheckIn checkIn, CheckInRequest request) {
        checkIn.setDate(request.date());
        checkIn.setTitle(normalizeTitle(request.title()));
        checkIn.setSummary(normalizeSummary(request.summary()));
        checkIn.setCategory(request.category());
        checkIn.setDurationMinutes(request.durationMinutes());
        checkIn.setTags(normalizeTags(request.tags()));
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                .map(tag -> tag == null ? "" : tag.trim())
                .filter(tag -> !tag.isEmpty())
                .map(this::normalizeTag)
                .distinct()
                .toList();
    }

    private String normalizeTag(String tag) {
        String normalized = tag.replace('，', ',');
        if ("未完全掌握".equals(normalized) || "尚未掌握".equals(normalized)) {
            return "尚未完全掌握";
        }
        return normalized.toLowerCase(Locale.ROOT).equals("not-mastered") ? "尚未完全掌握" : normalized;
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new BusinessException("CHECKIN_TITLE_REQUIRED", "标题不能为空");
        }
        return normalized.length() > 80 ? normalized.substring(0, 80).trim() : normalized;
    }

    private String normalizeSummary(String summary) {
        if (summary == null) {
            return null;
        }

        String normalized = summary.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > 300 ? normalized.substring(0, 300).trim() : normalized;
    }
}
