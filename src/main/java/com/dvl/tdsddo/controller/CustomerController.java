package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.request.CustomerRequest;
import com.dvl.tdsddo.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tds/customer")
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping("/addOrEditCustomer")
    public ResponseEntity<Map<String, Object>> addOrEditCustomer(@RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.addOrEditCustomer(request));
    }

    @GetMapping("/activeCustomers/{ddoId}")
    public ResponseEntity<Map<String, Object>> getActiveCustomers(@PathVariable Integer ddoId) {
        return ResponseEntity.ok(customerService.getAllActiveCustomersByDdoId(ddoId));
    }

    @PostMapping("/deleteCustomer/{customerId}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.ok(customerService.deleteCustomerById(customerId));
    }

}
