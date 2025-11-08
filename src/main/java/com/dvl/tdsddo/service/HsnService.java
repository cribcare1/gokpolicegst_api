package com.dvl.tdsddo.service;

import com.dvl.tdsddo.request.HSNRequest;
import com.dvl.tdsddo.response.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public interface HsnService {
    ApiResponse saveOrUpdateHSN(HSNRequest request);
    public ApiResponse deleteHSN(Integer id, Integer updatedBy);
    public ApiResponse getAllHSN();
}
