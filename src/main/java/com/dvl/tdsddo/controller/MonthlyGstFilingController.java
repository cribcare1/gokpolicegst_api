package com.dvl.tdsddo.controller;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.request.MonthlyGstFilingRequest;
import com.dvl.tdsddo.response.MonthlyGstFilingResponse;
import com.dvl.tdsddo.service.MonthlyGstFilingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tds/monthly-gst-filing")
@RequiredArgsConstructor
public class MonthlyGstFilingController {

    private final MonthlyGstFilingService service;




        // ------------------ Save Or Update ------------------
    @PostMapping("/saveOrUpdate")
    public ResponseEntity<Map<String, Object>> save(@RequestPart MonthlyGstFilingRequest request,@RequestPart MultipartFile file) {

        Map<String, Object> res = new HashMap<>();

        try {
            MonthlyGstFilingResponse response = service.saveOrUpdate(request,file);

            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", request.getId() == null ?
                    "Record Created Successfully" :
                    "Record Updated Successfully");
            res.put("data", response);

        } catch (Exception ex) {
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", ex.getMessage());
        }

        return ResponseEntity.ok(res);
    }

    // ------------------ Get By ID ------------------
    @GetMapping("/getMonthlyGstFilingById/{id}")
    public ResponseEntity<Map<String, Object>> getMonthlyGstFilingById(@PathVariable Integer id) {

        Map<String, Object> res = new HashMap<>();

        try {
            MonthlyGstFilingResponse response = service.getById(id);

            res.put("status", TdsDdoConstant
                    .SUCCESS);
            res.put("message", "Record Fetched Successfully");
            res.put("data", response);

        } catch (Exception ex) {
            ex.printStackTrace();
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to fetch record");
        }

        return ResponseEntity.ok(res);
    }

    // ------------------ Get All Records ------------------
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllMonthlyGstFiling() {

        Map<String, Object> res = new HashMap<>();

        try {
            List<MonthlyGstFilingResponse> list = service.getAll();

            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record List Fetched Successfully");
            res.put("data", list);

        } catch (Exception ex) {
            ex.printStackTrace();
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to fetch records");
        }

        return ResponseEntity.ok(res);
    }

    // ------------------ Delete Record ------------------
    @PostMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteMonthlyGstFiling(@PathVariable Integer id) {

        Map<String, Object> res = new HashMap<>();

        try {
            service.delete(id);

            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record Deleted Successfully");

        } catch (Exception ex) {
            ex.printStackTrace();
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to delete record");
        }

        return ResponseEntity.ok(res);
    }


// ------------------ Get By DDO ID ------------------
    @GetMapping("/getMonthlyGstFilingByDdoId/{ddoId}")
    public ResponseEntity<Map<String, Object>> getMonthlyGstFilingByDdoId(@PathVariable Integer ddoId) {

        Map<String, Object> res = new HashMap<>();

        try {
            List<MonthlyGstFilingResponse> list = service.getByDdoId(ddoId);

            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record List Fetched Successfully");
            res.put("data", list);

        } catch (Exception ex) {
            ex.printStackTrace();
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to fetch records for ddoId: " + ddoId);
        }

        return ResponseEntity.ok(res);
    }


    // ------------------ Get By GST ID ------------------

    @GetMapping("/getMonthlyGstFilingByGSTId/{gstId}")
    public ResponseEntity<Map<String, Object>> getMonthlyGstFilingByGSTId(@PathVariable Integer gstId) {

        Map<String, Object> res = new HashMap<>();

        try {
            List<MonthlyGstFilingResponse> list = service.getByGSTId(gstId);

            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", "Record List Fetched Successfully");
            res.put("data", list);

        } catch (Exception ex) {
            ex.printStackTrace();
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Unable to fetch records for ddoId: " + gstId);
        }

        return ResponseEntity.ok(res);
    }

    // ------------------ Save Or Update (multipart/form-data) ------------------
    @PostMapping(path = "/saveOrUpdateNew", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> save(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        Map<String, Object> res = new HashMap<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            MonthlyGstFilingRequest request = objectMapper.readValue(requestJson, MonthlyGstFilingRequest.class);

            MonthlyGstFilingResponse response = service.saveOrUpdate(request, file);

            res.put("status", TdsDdoConstant.SUCCESS);
            res.put("message", request.getId() == null ? "Record Created Successfully" : "Record Updated Successfully");
            res.put("data", response);

        } catch (Exception ex) {
            res.put("status", TdsDdoConstant.ERROR);
            res.put("message", "Something went wrong");
        }
        return ResponseEntity.ok(res);
    }

    }
