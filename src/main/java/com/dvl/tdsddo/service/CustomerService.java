package com.dvl.tdsddo.service;

import com.dvl.tdsddo.request.CustomerRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface CustomerService {
    public Map<String, Object> addOrEditCustomer(CustomerRequest request);
    public Map<String, Object> getAllActiveCustomersByDdoId(Integer ddoId);
    public Map<String, Object> deleteCustomerById(Integer customerId);
}
