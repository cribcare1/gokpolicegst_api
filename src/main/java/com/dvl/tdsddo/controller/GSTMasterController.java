package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.request.GSTMasterRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.service.GSTService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tds/gst")
@RequiredArgsConstructor
public class GSTMasterController {
    private final GSTService gstService;


    /**
     *  Create or Update GST details
     */
    @PostMapping("/saveOrUpdate")
    public ApiResponse saveOrUpdateGST(@Valid @RequestBody GSTMasterRequest request) {
        try {
            return gstService.saveOrUpdateGST(request);
        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, "Failed to save/update GST details: " + e.getMessage(), null);
        }
    }

    /**
     * ✅ Delete GST record (soft delete)
     */
    @PostMapping("/deleteGst/{gstId}/{createdBy}")
    public ApiResponse deleteGST(@PathVariable Integer gstId, @PathVariable(required = false) Integer createdBy) {
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
}
