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
    @PostMapping("/delete/{id}/{updatedBy}")
    public ResponseEntity<ApiResponse> deleteHSN(@PathVariable Integer id,
                                                 @PathVariable Integer updatedBy) {
        ApiResponse response = hsnService.deleteHSN(id, updatedBy);
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    // 🔹 Get All Active HSN records
    @GetMapping("/getAllHSN")
    public ResponseEntity<ApiResponse> getAllHSN() {
        ApiResponse response = hsnService.getAllHSN();
        if (TdsDdoConstant.SUCCESS.equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
