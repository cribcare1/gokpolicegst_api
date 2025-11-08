package com.dvl.tdsddo.service;

import com.dvl.tdsddo.request.BankDetailsRequest;
import com.dvl.tdsddo.response.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public interface BankService {
  public ApiResponse saveOrUpdateBank(BankDetailsRequest request);
    ApiResponse deleteBank(Integer bankId, Integer updatedBy);
    ApiResponse getAllActiveBankDetails(Integer gstId);
}
