package com.dvl.tdsddo.service;

import com.dvl.tdsddo.model.InvoiceMaster;
import com.dvl.tdsddo.request.InvoiceRequest;
import com.dvl.tdsddo.request.InvoiceSubmitRequest;
import com.dvl.tdsddo.request.ReceiptBulkCreateRequest;
import com.dvl.tdsddo.request.ReceiptGenerateRequest;
import com.dvl.tdsddo.response.InvoiceResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public interface InvoiceService {
    public InvoiceMaster saveOrUpdateInvoice(InvoiceRequest req, MultipartFile file) throws IOException;
    public Map<String, Object> getInvoiceList(Integer ddoId, Integer gstId);
    public Map<String, Object> getInvoiceDetails(Integer invoiceId);
    public String generateInvoiceNumberWithoutTable(Integer gstId, Integer ddoId);
    public String generateSubmittedInvoiceNumber(Integer gstId, Integer ddoId);
    public String generateSavedInvoiceNumber(Integer gstId, Integer ddoId) ;
    public String generateFinalInvoiceNumber(Integer gstId, Integer ddoId);
    public List<InvoiceResponse> getInvoices(Integer ddoId, Integer gstId, String status);
    public InvoiceMaster submitInvoice(InvoiceSubmitRequest req) ;

    public Map<String, Object> generateBulkReceiptNumbers(ReceiptGenerateRequest request);
    public Map<String, Object> createBulkReceipts(ReceiptBulkCreateRequest request);
    }
