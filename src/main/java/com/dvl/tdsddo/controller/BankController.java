package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.request.BankDetailsRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tds/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    // Create or Update Bank Details
    @PostMapping("/saveOrUpdateBank")
    public ResponseEntity<ApiResponse> saveOrUpdateBank(@RequestBody BankDetailsRequest request) {
        ApiResponse response = bankService.saveOrUpdateBankNew(request);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    // Delete (Soft Delete) Bank Record
    @PostMapping("/deleteBank/{bankId}")
    public ResponseEntity<ApiResponse> deleteBank(
            @PathVariable Integer bankId,
            @RequestParam(required = false) Integer updatedBy) {

        ApiResponse response = bankService.deleteBank(bankId, updatedBy);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(response);
    }

    // Get all active bank details (or specific GST ID)
    @GetMapping("/activeBankDetails")
    public ResponseEntity<ApiResponse> getAllActiveBanks(@RequestParam(required = false) Integer gstId,@RequestParam(required = false) Integer ddoId) {

        ApiResponse response = bankService.getAllActiveBankDetails(gstId,ddoId);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(response);
    }
}
