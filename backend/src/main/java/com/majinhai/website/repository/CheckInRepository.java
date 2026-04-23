package com.majinhai.website.repository;

import com.majinhai.website.model.entity.CheckIn;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository {

    List<CheckIn> findAll();

    Optional<CheckIn> findById(Long id);

    CheckIn save(CheckIn checkIn);

    CheckIn update(CheckIn checkIn);

    void deleteById(Long id);
}
