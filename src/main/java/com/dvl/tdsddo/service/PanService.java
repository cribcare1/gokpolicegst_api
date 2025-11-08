package com.dvl.tdsddo.service;

import com.dvl.tdsddo.request.PanMasterRequest;
import com.dvl.tdsddo.response.ApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public interface PanService {
    public ApiResponse saveOrUpdatePan(PanMasterRequest request);
    public ApiResponse deletePanDetails(Integer panId,Integer createdBy);
    public ApiResponse getAllPanDetails(String status);
}
