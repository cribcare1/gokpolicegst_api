package com.dvl.tdsddo.service;

import com.dvl.tdsddo.model.HsnGstHistory;
import com.dvl.tdsddo.request.HSNRequest;
import com.dvl.tdsddo.response.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface HsnService {
    ApiResponse saveOrUpdateHSN(HSNRequest request);
    public ApiResponse deleteHSN(Integer id, Integer updatedBy);
    public ApiResponse getAllHSN(Integer gstId);

    public ApiResponse getHsnGstHistory(Integer hsnId);
}
