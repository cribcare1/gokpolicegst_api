package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.request.PanMasterRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.service.PanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tds/pan")
@RequiredArgsConstructor
public class PanMasterController {
    private final PanService panService;


    @PostMapping("/saveOrUpdate")
    public ResponseEntity<ApiResponse> saveOrUpdatePan(@Valid @RequestBody PanMasterRequest request) {
        ApiResponse response = panService.saveOrUpdatePan(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete/{panId}")
    public ResponseEntity<ApiResponse> deletePanDetails(
            @PathVariable Integer panId,
            @RequestParam(required = false) Integer createdBy) {

        ApiResponse response = panService.deletePanDetails(panId, createdBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAllActivePan")
    public ResponseEntity<ApiResponse> getAllPanDetails(@RequestParam(required = false, defaultValue = TdsDdoConstant.ACTIVE) String status) {
        ApiResponse response = panService.getAllPanDetails(status);
        return ResponseEntity.ok(response);
    }
}
