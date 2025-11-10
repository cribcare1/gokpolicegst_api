package com.dvl.tdsddo.service;

import com.dvl.tdsddo.request.GSTMasterRequest;
import com.dvl.tdsddo.response.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface GSTService {
   public ApiResponse saveOrUpdateGSTNew(GSTMasterRequest request);
    public ApiResponse deleteGST(Integer gstId, Integer createdBy);
    public ApiResponse getAllGSTDetails(String status);


    public ApiResponse saveOrUpdateGST(GSTMasterRequest request);

    public ApiResponse getAllActiveGstDetails();

    public ApiResponse getAllDdosByGstId(Integer gstId);
}
