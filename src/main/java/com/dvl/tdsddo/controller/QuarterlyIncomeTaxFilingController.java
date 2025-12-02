package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.request.QuarterlyIncomeTaxFilingRequest;
import com.dvl.tdsddo.response.QuarterlyIncomeTaxFilingResponse;
import com.dvl.tdsddo.service.QuarterlyIncomeTaxFilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tds/quarterly-income-tax")
public class QuarterlyIncomeTaxFilingController {

    private final QuarterlyIncomeTaxFilingService service;

    // Save or Update (supports multipart file)
    @PostMapping(value = "/saveOrUpdate")
    public ResponseEntity<Map<String, Object>> saveOrUpdate(@RequestPart(value = "request") String requestJson,
                                                             @RequestPart(value = "file", required = false) MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            // deserialize requestJson
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            QuarterlyIncomeTaxFilingRequest request = om.readValue(requestJson, QuarterlyIncomeTaxFilingRequest.class);

            QuarterlyIncomeTaxFilingResponse response = service.saveOrUpdate(request, file);

            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", request.getId() == null ? "Record Created Successfully" : "Record Updated Successfully");
            res.put("data", response);
        } catch (Exception ex) {
            ex.printStackTrace();
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Something went wrong");
        }
        return ResponseEntity.ok(res);
    }

    // Get all
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> res = new HashMap<>();
        try {
            List<QuarterlyIncomeTaxFilingResponse> list = service.getAll();
            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record List Fetched Successfully");
            res.put("data", list);
        } catch (Exception ex) {
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to fetch records");
        }
        return ResponseEntity.ok(res);
    }

    // Get by DDO Id
    @GetMapping("/ddo/{ddoId}")
    public ResponseEntity<Map<String, Object>> getByDdo(@PathVariable Integer ddoId) {
        Map<String, Object> res = new HashMap<>();
        try {
            List<QuarterlyIncomeTaxFilingResponse> list = service.getByDdoId(ddoId);
            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record List Fetched Successfully");
            res.put("data", list);
        } catch (Exception ex) {
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to fetch records for ddoId: " + ddoId);
        }
        return ResponseEntity.ok(res);
    }

    // Get by id
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            QuarterlyIncomeTaxFilingResponse r = service.getById(id);
            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record Fetched Successfully");
            res.put("data", r);
        } catch (Exception ex) {
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to fetch record");
        }
        return ResponseEntity.ok(res);
    }

    // Delete
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            service.delete(id);
            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record Deleted Successfully");
        } catch (Exception ex) {
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to delete record");
        }
        return ResponseEntity.ok(res);
    }
}

