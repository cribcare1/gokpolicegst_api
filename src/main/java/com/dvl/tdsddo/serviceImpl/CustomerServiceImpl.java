package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.CustomerMaster;
import com.dvl.tdsddo.repository.CustomerMasterRepository;
import com.dvl.tdsddo.request.CustomerRequest;
import com.dvl.tdsddo.service.CustomerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerMasterRepository customerRepository;
    @Override
    @Transactional
    public Map<String, Object> addOrEditCustomer(CustomerRequest request) {
        try {
            CustomerMaster customer;

            if (request.getId() != null) {
                // ✏️ Edit existing
                customer = customerRepository.findById(request.getId())
                        .orElseThrow(() -> new RuntimeException("Customer not found"));
            } else {
                // ➕ Create new
                customer = new CustomerMaster();
                customer.setStatus(TdsDdoConstant.ACTIVE);
            }

            // 🧩 Step 2: Validate duplicates only for different records
            if (request.getGstNumber() != null && !request.getGstNumber().isBlank()) {
                Optional<CustomerMaster> gstExists =
                        customerRepository.findByGstNumberAndStatus(request.getGstNumber(), TdsDdoConstant.ACTIVE);

                if (gstExists.isPresent() && !gstExists.get().getId().equals(request.getId())) {
                    return Map.of(
                            TdsDdoConstant.MESSAGE, "GST Number already exists for another customer",
                            TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                    );
                }
            }

            if (request.getExemptionNumber() != null && !request.getExemptionNumber().isBlank()) {
                Optional<CustomerMaster> exemptionExists =
                        customerRepository.findByExemptionNumberAndStatus(request.getExemptionNumber(), TdsDdoConstant.ACTIVE);

                if (exemptionExists.isPresent() && !exemptionExists.get().getId().equals(request.getId())) {
                    return Map.of(
                            TdsDdoConstant.MESSAGE, "Exemption Number already exists for another customer",
                            TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                    );
                }
            }

            //  Step 3: Copy non-null fields
            if (request.getCustomerName() != null) customer.setCustomerName(request.getCustomerName());
            if (request.getCustomerType() != null) customer.setCustomerType(request.getCustomerType());
            if (request.getCustomerEmail() != null) customer.setCustomerEmail(request.getCustomerEmail());
            if (request.getAddress() != null) customer.setAddress(request.getAddress());
            if (request.getPinCode() != null) customer.setPinCode(request.getPinCode());
            if (request.getStateCode() != null) customer.setStateCode(request.getStateCode());
            if (request.getGstNumber() != null) customer.setGstNumber(request.getGstNumber());
            if (request.getCity() != null) customer.setCity(request.getCity());
            if (request.getMobile() != null) customer.setMobile(request.getMobile());
            if (request.getExemptionNumber() != null) customer.setExemptionNumber(request.getExemptionNumber());
            if (request.getDdoId() != null) customer.setDdoId(request.getDdoId());
            if (request.getServiceType() != null) customer.setServiceType(request.getServiceType());


            CustomerMaster saved = customerRepository.save(customer);

            return Map.of(
                    TdsDdoConstant.MESSAGE, (request.getId() != null ? "Customer updated" : "Customer added") + " successfully",
                    TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS,
                    "customerId", saved.getId()
            );

        } catch (Exception e) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Operation failed: " + e.getMessage(),
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }
    }


    @Override
    public Map<String, Object> getAllActiveCustomersByDdoId(Integer ddoId) {
        try {
            List<CustomerMaster> customers = customerRepository.findByDdoIdAndStatus(ddoId, TdsDdoConstant.ACTIVE);

            if (customers.isEmpty()) {
                return Map.of(
                        TdsDdoConstant.MESSAGE, "No active customers found for this DDO",
                        TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                );
            }

            return Map.of(
                    TdsDdoConstant.MESSAGE, "Active customers fetched successfully",
                    TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS,
                    "data", customers
            );
        } catch (Exception e) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Failed to fetch customers: " + e.getMessage(),
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }
    }
    @Override
    @Transactional
    public Map<String, Object> deleteCustomerById(Integer customerId) {
        try {
            CustomerMaster customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            if (!TdsDdoConstant.ACTIVE.equalsIgnoreCase(customer.getStatus())) {
                return Map.of(
                        TdsDdoConstant.MESSAGE, "Customer already inactive or deleted",
                        TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                );
            }

            customer.setStatus(TdsDdoConstant.INACTIVE);
            customerRepository.save(customer);

            return Map.of(
                    TdsDdoConstant.MESSAGE, "Customer deleted successfully",
                    TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS
            );

        } catch (Exception e) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Failed to delete customer: " + e.getMessage(),
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }
    }

}
