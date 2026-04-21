package com.majinhai.website.service.impl;

import com.majinhai.website.model.dto.CheckInRequest;
import com.majinhai.website.model.dto.CheckInResponse;
import com.majinhai.website.model.entity.CheckIn;
import com.majinhai.website.repository.CheckInRepository;
import com.majinhai.website.service.CheckInService;
import java.time.OffsetDateTime;
import java.util.List;
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
        checkIn.setDate(request.date());
        checkIn.setTitle(request.title());
        checkIn.setSummary(request.summary());
        checkIn.setCategory(request.category());
        checkIn.setDurationMinutes(request.durationMinutes());
        checkIn.setCreatedAt(OffsetDateTime.now());
        return toResponse(checkInRepository.save(checkIn));
    }

    private CheckInResponse toResponse(CheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getDate(),
                checkIn.getTitle(),
                checkIn.getSummary(),
                checkIn.getCategory(),
                checkIn.getDurationMinutes(),
                checkIn.getCreatedAt()
        );
    }
}
