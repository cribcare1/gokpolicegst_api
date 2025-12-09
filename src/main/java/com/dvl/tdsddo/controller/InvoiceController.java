package com.dvl.tdsddo.controller;


import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.InvoiceMaster;
import com.dvl.tdsddo.request.InvoiceRequest;
import com.dvl.tdsddo.request.InvoiceSubmitRequest;
import com.dvl.tdsddo.request.ReceiptBulkCreateRequest;
import com.dvl.tdsddo.request.ReceiptGenerateRequest;
import com.dvl.tdsddo.response.InvoiceResponse;
import com.dvl.tdsddo.service.InvoiceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tds/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // -----------------------------------------------------
    // 1) CREATE or UPDATE Invoice
    // -----------------------------------------------------
//    @PostMapping("/save")
//    public Map<String, Object> saveOrUpdate(@RequestPart InvoiceRequest req,@RequestPart(required = false) MultipartFile file) throws IOException {
//        invoiceService.saveOrUpdateInvoice(req,file);
//        return Map.of(
//                "status", TdsDdoConstant.SUCCESS,
//                "message", "Invoice saved successfully"
//        );
//    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveOrUpdate(
            @RequestPart("request") String invoiceJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            // Convert JSON string to InvoiceRequest object
            ObjectMapper mapper = new ObjectMapper();
            InvoiceRequest req = mapper.readValue(invoiceJson, InvoiceRequest.class);

            invoiceService.saveOrUpdateInvoice(req, file);

            return ResponseEntity.ok(Map.of(
                    "status", TdsDdoConstant.SUCCESS,
                    "message", "Invoice saved successfully"
            ));
        }
        catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", TdsDdoConstant.ERROR,
                    "message", "Invalid invoice JSON format",
                    "error", e.getOriginalMessage()
            ));
        }
        catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", TdsDdoConstant.ERROR,
                    "message", "Unable to save invoice",
                    "error", ex.getMessage()
            ));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitInvoice(@RequestBody InvoiceSubmitRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            InvoiceMaster invoice = invoiceService.submitInvoice(request);

            response.put("status", "SUCCESS");
            response.put("message", "Invoice submitted successfully!");
            response.put("invoiceId", invoice.getId());
            response.put("finalInvoiceNumber", invoice.getFinalInvoiceNumber());
            response.put("invoiceStatus", invoice.getInvoiceStatus());
            response.put("submittedDate", invoice.getSubmittedDate());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            response.put("status", "ERROR");
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (RuntimeException ex) {
            response.put("status", "ERROR");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception ex) {
            response.put("status", "ERROR");
            response.put("message", "Unexpected error occurred");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }


    @GetMapping("/generate-number")
    public ResponseEntity<Map<String, Object>> generateInvoiceNumber(
            @RequestParam Integer gstId,
            @RequestParam Integer ddoId) {

        Map<String, Object> response = new HashMap<>();

        try {
            //String invoiceNumber = invoiceService.generateInvoiceNumberWithoutTable(gstId, ddoId);
            String invoiceNumber=  invoiceService.generateSavedInvoiceNumber(gstId, ddoId);
            response.put("status", "success");
            response.put(TdsDdoConstant.MESSAGE, "Invoice Number generated successfully");
            response.put("invoiceNumber", invoiceNumber);

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            // Custom messages from your service (GST not found / DDO not found)
            response.put("status", "error");
            response.put(TdsDdoConstant.MESSAGE, ex.getMessage());
            response.put("invoiceNumber", null);

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception ex) {
            // Unexpected error
            response.put("status", "error");
            response.put(TdsDdoConstant.MESSAGE, "Something went wrong while generating invoice number");
            response.put("error", ex.getMessage());

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }

    @GetMapping("/generate-submitted-number")
    public ResponseEntity<Map<String, Object>> generateSubmittedInvoiceNumber(
            @RequestParam Integer gstId,
            @RequestParam Integer ddoId) {

        Map<String, Object> response = new HashMap<>();

        try {
            //String invoiceNumber = invoiceService.generateInvoiceNumberWithoutTable(gstId, ddoId);
            String invoiceNumber=  invoiceService.generateSubmittedInvoiceNumber(gstId, ddoId);
            response.put("status", "success");
            response.put(TdsDdoConstant.MESSAGE, "Invoice Number generated successfully");
            response.put("invoiceNumber", invoiceNumber);

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            // Custom messages from your service (GST not found / DDO not found)
            response.put("status", "error");
            response.put(TdsDdoConstant.MESSAGE, ex.getMessage());
            response.put("invoiceNumber", null);

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception ex) {
            // Unexpected error
            response.put("status", "error");
            response.put(TdsDdoConstant.MESSAGE, "Something went wrong while generating invoice number");
            response.put("error", ex.getMessage());

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }


    // -----------------------------------------------------
    // 2) LIST INVOICES (Dashboard View)
    // -----------------------------------------------------
    @GetMapping("/list")
    public Map<String, Object> getInvoiceList(
            @RequestParam(required = false) Integer ddoId,
            @RequestParam(required = false) Integer gstId
    ) {
        return invoiceService.getInvoiceList(ddoId, gstId);
    }

    // -----------------------------------------------------
    // 3) INVOICE COMPLETE DETAILS (Full Invoice)
    // -----------------------------------------------------
    @GetMapping("/details/{invoiceId}")
    public Map<String, Object> getInvoiceDetails(@PathVariable Integer invoiceId) {
        return invoiceService.getInvoiceDetails(invoiceId);
    }

    @GetMapping("/invoiceListDetails")
    public ResponseEntity<Map<String, Object>> getInvoices(
            @RequestParam(required = false) Integer ddoId,
            @RequestParam(required = false) Integer gstId,
            @RequestParam(required = false) String status
    ) {



        List<InvoiceResponse> invoices = invoiceService.getInvoices(ddoId, gstId,  status);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", TdsDdoConstant.SUCCESS);
        response.put("message", "Invoices fetched successfully");
        response.put("count", invoices.size());
        response.put("data", invoices);

        return ResponseEntity.ok(response);
    }



    @PostMapping("/generate-bulk-receipts")
    public ResponseEntity<Map<String, Object>> generateBulkReceipts(
             @RequestBody ReceiptGenerateRequest request) {

        Map<String, Object> response = invoiceService.generateBulkReceiptNumbers(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/createBulkReceipts")
    public ResponseEntity<Map<String, Object>> createBulkReceipts(
            @RequestBody ReceiptBulkCreateRequest request) {

        Map<String, Object> response = invoiceService.createBulkReceipts(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/updateInvoiceStatus/{invoiceId}/status")
    public ResponseEntity<Map<String, Object>> updateInvoiceStatus(
            @PathVariable Integer invoiceId,
            @RequestParam String status) {

        try {
            Map<String, Object> response = invoiceService.deleteOrCancelInvoice(invoiceId, status);
            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
