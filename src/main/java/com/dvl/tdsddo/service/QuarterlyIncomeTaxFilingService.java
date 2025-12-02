package com.dvl.tdsddo.service;

import com.dvl.tdsddo.request.QuarterlyIncomeTaxFilingRequest;
import com.dvl.tdsddo.response.QuarterlyIncomeTaxFilingResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuarterlyIncomeTaxFilingService {
    QuarterlyIncomeTaxFilingResponse saveOrUpdate(QuarterlyIncomeTaxFilingRequest request, MultipartFile file);
    QuarterlyIncomeTaxFilingResponse getById(Long id);
    List<QuarterlyIncomeTaxFilingResponse> getAll();
    void delete(Long id);
    List<QuarterlyIncomeTaxFilingResponse> getByDdoId(Integer ddoId);
}

