package com.majinhai.website.repository;

import com.majinhai.website.model.entity.CheckIn;
import java.util.List;

public interface CheckInRepository {

    List<CheckIn> findAll();

    CheckIn save(CheckIn checkIn);
}
