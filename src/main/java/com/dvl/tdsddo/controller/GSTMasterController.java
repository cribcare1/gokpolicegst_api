package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.request.DdoMigrationRequest;
import com.dvl.tdsddo.request.GSTMasterRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.service.GSTService;
import com.dvl.tdsddo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/tds/gst")
@RequiredArgsConstructor
public class GSTMasterController {
    private final GSTService gstService;
    private final UserService userService;


    /**
     *  Create or Update GST details
     */
    @PostMapping("/saveOrUpdate")
    public ApiResponse saveOrUpdateGST(@Valid @RequestPart GSTMasterRequest request,@RequestPart(required = false)  MultipartFile file) {
        try {
            return gstService.saveOrUpdateGST(request,file);
        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, "Failed to save/update GST details: " + e.getMessage(), null);
        }
    }


    @PostMapping("/saveOrUpdateGST")
    public ApiResponse saveOrUpdateGST(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            // 🔹 Convert JSON string to object
            ObjectMapper mapper = new ObjectMapper();
            GSTMasterRequest request = mapper.readValue(requestJson, GSTMasterRequest.class);

            // 🔹 Pass to service
            return gstService.saveOrUpdateGST(request, file);

        } catch (Exception e) {
            return new ApiResponse(
                    TdsDdoConstant.ERROR,
                    "Failed to save/update GST details: " + e.getMessage(),
                    null
            );
        }
    }


    /**
     * ✅ Delete GST record (soft delete)
     */
    @PostMapping("/delete/{gstId}")
    public ApiResponse deleteGST(@PathVariable Integer gstId, @RequestParam(required = false) Integer createdBy) {
        try {
            return gstService.deleteGST(gstId, createdBy);
        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, "Failed to delete GST details: " + e.getMessage(), null);
        }
    }

    @GetMapping("/activeGSTDetails")
    public ApiResponse getAllActiveGst() {
        return gstService.getAllActiveGstDetails();
    }

    @PostMapping("/migrate-ddos")
    public ApiResponse migrateDdosBetweenGsts(@Valid @RequestBody DdoMigrationRequest request) {
        return userService.migrateDdosBetweenGsts(request);
    }


    @GetMapping("/ddoList")
    public ResponseEntity<ApiResponse> getAllDdos(@RequestParam(required = false) Integer gstId) {
        ApiResponse response = gstService.getAllDdosByGstId(gstId);
        return ResponseEntity.ok(response);
    }
}
