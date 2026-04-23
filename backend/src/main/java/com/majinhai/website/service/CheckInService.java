package com.majinhai.website.service;

import com.majinhai.website.model.dto.CheckInRequest;
import com.majinhai.website.model.dto.CheckInResponse;
import com.majinhai.website.model.dto.CheckInTagsUpdateRequest;
import java.util.List;

public interface CheckInService {

    List<CheckInResponse> listAll();

    CheckInResponse create(CheckInRequest request);

    CheckInResponse update(Long id, CheckInRequest request);

    CheckInResponse updateTags(Long id, CheckInTagsUpdateRequest request);

    void delete(Long id);
}
