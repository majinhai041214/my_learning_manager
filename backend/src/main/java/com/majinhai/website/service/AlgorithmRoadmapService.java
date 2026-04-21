package com.majinhai.website.service;

import com.majinhai.website.model.dto.AlgorithmRoadmapRequest;
import com.majinhai.website.model.dto.AlgorithmRoadmapResponse;

public interface AlgorithmRoadmapService {

    AlgorithmRoadmapResponse get();

    AlgorithmRoadmapResponse update(AlgorithmRoadmapRequest request);
}
