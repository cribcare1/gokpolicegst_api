package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.request.HSNRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.service.HsnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tds/hsn")
@RequiredArgsConstructor
public class HSNController {

    private final HsnService hsnService;

    // 🔹 Add or Update HSN
    @PostMapping("/saveOrUpdateHSN")
    public ResponseEntity<ApiResponse> saveOrUpdateHSN(@RequestBody HSNRequest request) {
        ApiResponse response = hsnService.saveOrUpdateHSN(request);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    // 🔹 Delete HSN by ID (soft delete)
    @PostMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteHSN(@PathVariable Integer id,
                                                 @RequestParam(required = false) Integer updatedBy) {
        ApiResponse response = hsnService.deleteHSN(id, updatedBy);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(response);
    }

    // 🔹 Get All Active HSN records
    @GetMapping("/getAllHSN")
    public ResponseEntity<ApiResponse> getAllHSN(@RequestParam(required = false) Integer gstId) {
        ApiResponse response = hsnService.getAllHSN(gstId);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAllHSNHistory/{hsnId}")
    public ResponseEntity<ApiResponse> getAllHSNHistory(@PathVariable Integer hsnId) {
        ApiResponse response = hsnService.getHsnGstHistory(hsnId);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(response);
    }
}
