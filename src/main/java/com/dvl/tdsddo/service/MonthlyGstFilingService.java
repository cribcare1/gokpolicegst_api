package com.dvl.tdsddo.service;

import com.dvl.tdsddo.request.MonthlyGstFilingRequest;
import com.dvl.tdsddo.response.MonthlyGstFilingResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface MonthlyGstFilingService {

    MonthlyGstFilingResponse saveOrUpdate(MonthlyGstFilingRequest request, MultipartFile file) throws IOException;

    MonthlyGstFilingResponse getById(Integer id);
    public List<MonthlyGstFilingResponse> getByDdoId(Integer ddoId);

    List<MonthlyGstFilingResponse> getAll();
    public void delete(Integer id);
}
