package com.majinhai.website.service;

import com.majinhai.website.model.dto.CheckInRequest;
import com.majinhai.website.model.dto.CheckInResponse;
import java.util.List;

public interface CheckInService {

    List<CheckInResponse> listAll();

    CheckInResponse create(CheckInRequest request);
}
