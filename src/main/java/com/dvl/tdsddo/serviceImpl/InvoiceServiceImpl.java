package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.Exception.ResourceNotFoundException;
import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.*;
import com.dvl.tdsddo.repository.*;
import com.dvl.tdsddo.request.*;
import com.dvl.tdsddo.response.*;
import com.dvl.tdsddo.service.InvoiceService;
import com.dvl.tdsddo.service.UserService;
import com.dvl.tdsddo.util.FileServiceUtil;
import com.dvl.tdsddo.util.TdsUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceMasterRepository invoiceRepo;
    private final InvoiceItemRepository itemRepo;
    private final GstSnapshotRepository gstSnapshotRepo;
    private final BankSnapshotRepository bankSnapshotRepo;
    private final CreditNoteRepository creditNoteRepo;

    private final GSTRepository gstRepo;
    private final UserRepository ddoRepo;
    private final FileServiceUtil fileServiceUtil;
    // ============================
    // SAVE OR UPDATE INVOICE
    // ============================
    @Transactional
    @Override
    public InvoiceMaster saveOrUpdateInvoice(InvoiceRequest req, MultipartFile file) throws IOException {

        InvoiceMaster invoice;

        // --------------------------------------
        // CREATE NEW INVOICE
        // --------------------------------------
        if (req.getInvoiceId() == null) {

            if (req.getGstId() == null || req.getDdoId() == null) {
                throw new RuntimeException("GST ID & DDO ID are required for new invoice");
            }

            String invoiceNo = generateInvoiceNumberWithoutTable(req.getGstId(), req.getDdoId());
            String financialYear=getCurrentFinancialYear();
            invoice = InvoiceMaster.builder()
                    .invoiceNumber(invoiceNo)
                    .invoiceStatus("SAVED")
                    .paidAmount(0.0)
                    .balanceAmount(0.0)
                    .invoiceDate(TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal().toLocalDate().toString())
                    .financialYear(financialYear)
                    .notificationDetails(req.getNotificationDetails())
                    .status("pending")
                    .build();
        }

        // --------------------------------------
        // UPDATE EXISTING INVOICE
        // --------------------------------------
        else {
            invoice = invoiceRepo.findById(req.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
        }


        // --------------------------------------
        // SIMPLE FIELDS UPDATE
        // --------------------------------------
        if (req.getDdoId() != null) invoice.setDdoId(req.getDdoId());

        if (req.getBankId() != null) invoice.setBankId(req.getBankId());
        if (req.getGstId() != null) invoice.setGstId(req.getGstId());
        if (req.getCustomerId() != null) invoice.setCustomerId(req.getCustomerId());
        if (req.getInvoiceDate() != null) invoice.setInvoiceDate(req.getInvoiceDate());
        if (req.getRemarks() != null) invoice.setRemarks(req.getRemarks());
        if (req.getInvoiceNumber() != null) invoice.setInvoiceNumber(req.getInvoiceNumber());


        // STATUS TRANSITION
        if (req.getInvoiceStatus() != null) {
           // validateStatusTransition(invoice.getInvoiceStatus(), req.getInvoiceStatus());
            invoice.setInvoiceStatus(req.getInvoiceStatus());
        }

        // --------------------------------------
        // TOTALS FROM FRONTEND (DIRECT)
        // --------------------------------------
        if (req.getTotalAmount() != null) invoice.setTotalAmount(req.getTotalAmount());
        if (req.getTotalCgst() != null) invoice.setTotalCgst(req.getTotalCgst());
        if (req.getTotalSgst() != null) invoice.setTotalSgst(req.getTotalSgst());
        if (req.getTotalIgst() != null) invoice.setTotalIgst(req.getTotalIgst());
        if (req.getGrandTotal() != null) invoice.setGrandTotal(req.getGrandTotal());
        if (req.getPaidAmount() != null) invoice.setPaidAmount(req.getPaidAmount());
        if (req.getBalanceAmount() != null) invoice.setBalanceAmount(req.getBalanceAmount());

        invoice = invoiceRepo.save(invoice);


        // ============================
        // ITEMS (FRONTEND CALCULATED)
        // ============================
        if (req.getItems() != null) {

            itemRepo.deleteByInvoiceId(invoice.getId());

            for (InvoiceItemRequest ir : req.getItems()) {

                InvoiceItem item = InvoiceItem.builder()
                        .invoiceId(invoice.getId())
                        .hsnId(ir.getHsnId())
                        .serviceName(ir.getServiceName())
                        .quantity(ir.getQuantity())
                        .rate(ir.getRate())
                        .amount(ir.getAmount())
                        .cgstRate(ir.getCgstRate())
                        .sgstRate(ir.getSgstRate())
                        .igstRate(ir.getIgstRate())
                        .build();

                itemRepo.save(item);
            }
        }


        // ============================
        // GST SNAPSHOT
        // ============================
        if (req.getGstSnapshot() != null) {

            GSTSnapshot snap = gstSnapshotRepo.findByInvoiceId(invoice.getId())
                    .orElse(new GSTSnapshot());

            snap.setInvoiceId(invoice.getId());
            snap.setGstName(req.getGstSnapshot().getGstName());
            snap.setGstNumber(req.getGstSnapshot().getGstNumber());
            snap.setStateCode(req.getGstSnapshot().getStateCode());
            snap.setGstHolderName(req.getGstSnapshot().getGstHolderName());

            gstSnapshotRepo.save(snap);
        }


        // ============================
        // BANK SNAPSHOT
        // ============================
        if (req.getBankSnapshot() != null) {

            BankSnapshot bank = bankSnapshotRepo.findByInvoiceId(invoice.getId())
                    .orElse(new BankSnapshot());

            bank.setInvoiceId(invoice.getId());
            bank.setBankName(req.getBankSnapshot().getBankName());
            bank.setBranchName(req.getBankSnapshot().getBranchName());
            bank.setAccountNumber(req.getBankSnapshot().getAccountNumber());
            bank.setIfscCode(req.getBankSnapshot().getIfscCode());

            bankSnapshotRepo.save(bank);
        }

        if(file!=null && !file.isEmpty()){
         String filePath=   fileServiceUtil.uploadFile(file,TdsDdoConstant.GST);
         invoice.setSignPath(filePath);
        }

        // ============================
        // CREDIT NOTE
        // ============================
        if (req.getCreditNote() != null) {

            CreditNote note = creditNoteRepo.findByInvoiceId(invoice.getId())
                    .orElse(new CreditNote());

            note.setInvoiceId(invoice.getId());
            note.setCreditNoteNumber(req.getCreditNote().getCreditNoteNumber());
            note.setCreditNoteAmount(req.getCreditNote().getCreditNoteAmount());
            note.setMismatchAmount(req.getCreditNote().getMismatchAmount());
            note.setReason(req.getCreditNote().getReason());

            creditNoteRepo.save(note);
        }

        return invoice;
    }


    @Transactional
    @Override
    public InvoiceMaster createShortfallInvoice(Integer parentInvoiceId,Double totalAmount) {

        InvoiceMaster parent = invoiceRepo.findById(parentInvoiceId)
                .orElseThrow(() -> new RuntimeException("Parent invoice not found"));

        // ---------------------------
        // CLONE INVOICE MASTER
        // ---------------------------
        InvoiceMaster shortfall = InvoiceMaster.builder()
                .invoiceNumber(parent.getInvoiceNumber()) // SAME
                .ddoId(parent.getDdoId())
                .gstId(parent.getGstId())
                .bankId(parent.getBankId())
                .customerId(parent.getCustomerId())
                .receiptNumber(parent.getReceiptNumber())
                .paymentType(parent.getPaymentType())
                .referenceNumber(parent.getReferenceNumber())
                .paidDate(parent.getPaidDate())
                .invoiceDate(parent.getInvoiceDate())
                .finalInvoiceNumber(parent.getFinalInvoiceNumber())
                .submittedDate(parent.getSubmittedDate())
                .signPath(parent.getSignPath())
                .gstType(parent.getGstType())
                .financialYear(parent.getFinancialYear())
                .invoiceStatus("SAVED")
                .totalAmount(totalAmount)
                .totalIgst(parent.getTotalIgst())
                .totalCgst(parent.getTotalCgst())
                .totalSgst(parent.getTotalSgst())
                .grandTotal(parent.getGrandTotal())
                .paidAmount(0.0)
                .balanceAmount(0.0)
                .remarks(parent.getRemarks())
                .notificationDetails(parent.getNotificationDetails())
                .status("pending")
                .isShortfall(true)
                .build();

        shortfall = invoiceRepo.save(shortfall);

        // ---------------------------
        // CLONE ITEMS
        // ---------------------------
        List<InvoiceItem> parentItems = itemRepo.findAllById(parent.getId());
        for (InvoiceItem item : parentItems) {
            InvoiceItem copy = InvoiceItem.builder()
                    .invoiceId(shortfall.getId())
                    .hsnId(item.getHsnId())
                    .serviceName(item.getServiceName())
                    .quantity(item.getQuantity())
                    .rate(item.getRate())
                    .amount(item.getAmount())
                    .cgstRate(item.getCgstRate())
                    .sgstRate(item.getSgstRate())
                    .igstRate(item.getIgstRate())
                    .cgstValue(item.getCgstValue())
                    .sgstValue(item.getSgstValue())
                    .igstValue(item.getIgstValue())
                    .build();
            itemRepo.save(copy);
        }

        // ---------------------------
        // CLONE GST SNAPSHOT
        // ---------------------------
        InvoiceMaster finalShortfall = shortfall;
        gstSnapshotRepo.findByInvoiceId(parent.getId()).ifPresent(gs -> {
            GSTSnapshot copy = new GSTSnapshot();
            copy.setInvoiceId(finalShortfall.getId());
            copy.setGstName(gs.getGstName());
            copy.setGstNumber(gs.getGstNumber());
            copy.setStateCode(gs.getStateCode());
            copy.setGstHolderName(gs.getGstHolderName());
            gstSnapshotRepo.save(copy);
        });

        // ---------------------------
        // CLONE BANK SNAPSHOT
        // ---------------------------
        InvoiceMaster finalShortfall1 = shortfall;
        bankSnapshotRepo.findByInvoiceId(parent.getId()).ifPresent(bs -> {
            BankSnapshot copy = new BankSnapshot();
            copy.setInvoiceId(finalShortfall1.getId());
            copy.setBankName(bs.getBankName());
            copy.setBranchName(bs.getBranchName());
            copy.setAccountNumber(bs.getAccountNumber());
            copy.setIfscCode(bs.getIfscCode());
            bankSnapshotRepo.save(copy);
        });

        // ---------------------------
        // CLONE CREDIT NOTE (IF EXISTS)
        // ---------------------------
        InvoiceMaster finalShortfall2 = shortfall;
        creditNoteRepo.findByInvoiceId(parent.getId()).ifPresent(cn -> {
            CreditNote copy = new CreditNote();
            copy.setInvoiceId(finalShortfall2.getId());
            copy.setCreditNoteNumber(cn.getCreditNoteNumber());
            copy.setCreditNoteAmount(cn.getCreditNoteAmount());
            copy.setMismatchAmount(cn.getMismatchAmount());
            copy.setReason(cn.getReason());
            creditNoteRepo.save(copy);
        });

        return shortfall;
    }


    @Transactional
    public InvoiceMaster submitInvoice(InvoiceSubmitRequest req) {

        if (req.getInvoiceId() == null) {
            throw new IllegalArgumentException("Invoice ID is required for submission!");
        }

        InvoiceMaster invoice = invoiceRepo.findById(req.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found for ID: " + req.getInvoiceId()));

        // Validate status transition
        if (!"SAVED".equalsIgnoreCase(invoice.getInvoiceStatus())) {
            throw new RuntimeException(
                    "Invoice already submitted or not in a valid status for submission! Current Status: "
                            + invoice.getInvoiceStatus()
            );
        }

        if (invoice.getGstId() == null || invoice.getDdoId() == null) {
            throw new RuntimeException("GST ID and DDO ID required before submission!");
        }

        // Generate Final Submitted Invoice No
        String finalInvoiceNumber = generateSubmittedInvoiceNumber(invoice.getGstId(), invoice.getDdoId());
        invoice.setFinalInvoiceNumber(finalInvoiceNumber);

        // Update Amounts
//        invoice.setPaidAmount(req.getPaidAmount() != null ? req.getPaidAmount() : invoice.getPaidAmount());
//        invoice.setBalanceAmount(req.getBalanceAmount() != null ? req.getBalanceAmount() : invoice.getBalanceAmount());

        // Update Submission State
        invoice.setInvoiceStatus("SUBMITTED");
        invoice.setSubmittedDate(TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal().toLocalDate().toString()); // Store as String (your requirement)

        // Prevent further editing of invoiceNumber after submission
        if (invoice.getInvoiceNumber() == null) {
            invoice.setInvoiceNumber(finalInvoiceNumber);
        }

        return invoiceRepo.save(invoice);
    }


    // ============================
    // GENERATE UNIQUE INVOICE NUMBER
    // ============================

    @Override
    public String generateInvoiceNumberWithoutTable(Integer gstId, Integer ddoId) {

        // GST last 3 chars
        String gstNum = gstRepo.findById(gstId)
                .map(GSTMaster::getGstNumber)
                .orElseThrow(() -> new RuntimeException("GST not found"));

        String last3 = gstNum.substring(gstNum.length() - 3);

        // DDO code
        String ddoCode = ddoRepo.findById(ddoId)
                .map(User::getDdoCode)
                .orElseThrow(() -> new RuntimeException("DDO not found"));

        // Financial year
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        String fy = (today.getMonthValue() >= 4)
                ? year + "-" + (year + 1)
                : (year - 1) + "-" + year;

        String prefix = last3 + "/" + ddoCode + "/" + fy + "/";

        Optional<InvoiceMaster> lastInvoice =
                invoiceRepo.findTopByInvoiceNumberStartingWithOrderByIdDesc(prefix);

        int next = 1;

        if (lastInvoice.isPresent()) {
            String inv = lastInvoice.get().getInvoiceNumber();
            String lastNo = inv.substring(inv.lastIndexOf("/") + 1);
            next = Integer.parseInt(lastNo) + 1;
        }

        return prefix + String.format("%04d", next);
    }


    // ============================
    // STATUS VALIDATION
    // ============================
    private void validateStatusTransition(String oldStatus, String newStatus) {
        if (oldStatus == null) return;

        var order = List.of("SAVE", "PENDING", "FINAL", "PAID", "CLOSED");

        if (order.indexOf(newStatus) < order.indexOf(oldStatus)) {
            throw new RuntimeException("Invalid status change: " + oldStatus + " → " + newStatus);
        }
    }


    @Override

    public Map<String, Object> getInvoiceList(Integer ddoId, Integer gstId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Fetch data
            List<InvoiceMaster> list = invoiceRepo.findByFilters(ddoId, gstId,List.of("inactive"));

            // Convert to lightweight response list
            List<InvoiceListResponse> result = list.stream()
                    .map(inv -> InvoiceListResponse.builder()
                            .invoiceId(inv.getId())
                            .invoiceNumber(inv.getInvoiceNumber())
                            .ddoId(inv.getDdoId())
                            .customerId(inv.getCustomerId())
                            .invoiceDate(inv.getInvoiceDate())
                            .invoiceStatus(inv.getInvoiceStatus())
                            .totalAmount(inv.getTotalAmount() != null ? BigDecimal.valueOf(inv.getTotalAmount()) : null)
                            .grandTotal(inv.getGrandTotal() != null ? BigDecimal.valueOf(inv.getGrandTotal()) : null)
                            .paidAmount(inv.getPaidAmount() != null ? BigDecimal.valueOf(inv.getPaidAmount()) : null)
                            .balanceAmount(inv.getBalanceAmount() != null ? BigDecimal.valueOf(inv.getBalanceAmount()) : null)
                            .build()
                    )
                    .collect(Collectors.toList());

            // Prepare success response
            response.put("status", TdsDdoConstant.SUCCESS);
            response.put("message", "Invoice list fetched successfully");
            response.put("count", result.size());
            response.put("data", result);

        } catch (Exception ex) {
            // Prepare failure response
            response.put("status", TdsDdoConstant.ERROR);
            response.put("message", "Failed to fetch invoice list: " + ex.getMessage());
            response.put("data", null);
        }

        return response;
    }


    @Override
    public Map<String, Object> getInvoiceDetails(Integer invoiceId) {

        Map<String, Object> response = new HashMap<>();

        try {

            // Validate invoice id
            if (invoiceId == null) {
                response.put("status", TdsDdoConstant.ERROR);
                response.put("message", "Invoice ID is required");
                response.put("data", null);
                return response;
            }

            // Fetch invoice
            InvoiceMaster inv = invoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            // Map to detailed response
            InvoiceResponse invoiceResponse = mapToInvoiceResponse(inv);

            // SUCCESS RESPONSE
            response.put("status", TdsDdoConstant.SUCCESS);
            response.put("message", "Invoice details fetched successfully");
            response.put("data", invoiceResponse);

        } catch (Exception ex) {
            // ERROR RESPONSE
            response.put("status", TdsDdoConstant.ERROR);
            response.put("message", "Failed to fetch invoice details: " + ex.getMessage());
            response.put("data", null);
        }

        return response;
    }

    private InvoiceResponse mapToInvoiceResponse(InvoiceMaster inv) {

        return InvoiceResponse.builder()
                .invoiceId(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .ddoId(inv.getDdoId())
                .gstId(inv.getGstId())
                .customerId(inv.getCustomerId())
                .invoiceDate(inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : null)
                .invoiceStatus(inv.getInvoiceStatus())
                .remarks(inv.getRemarks())

                // ===== TOTALS (SAFE) =====
                .totalAmount(toBigDecimal(inv.getTotalAmount()))
                .totalCgst(toBigDecimal(inv.getTotalCgst()))
                .totalSgst(toBigDecimal(inv.getTotalSgst()))
                .totalIgst(toBigDecimal(inv.getTotalIgst()))
                .grandTotal(toBigDecimal(inv.getGrandTotal()))
                .paidAmount(toBigDecimal(inv.getPaidAmount()))
                .balanceAmount(toBigDecimal(inv.getBalanceAmount()))

                // ===== ITEMS =====
                .items(
                        itemRepo.findByInvoiceId(inv.getId())
                                .stream()
                                .map(item -> InvoiceItemResponse.builder()
                                        .itemId(item.getId())
                                        .hsnId(item.getHsnId())

                                        .serviceName(item.getServiceName())
                                        .quantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                                        .rate(item.getRate() != null ? item.getRate() : BigDecimal.ZERO)
                                        .amount(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO)

                                        .cgstRate(nz(item.getCgstRate()))
                                        .sgstRate(nz(item.getSgstRate()))
                                        .igstRate(nz(item.getIgstRate()))

                                        .cgstValue(nz(item.getCgstValue()))
                                        .sgstValue(nz(item.getSgstValue()))
                                        .igstValue(nz(item.getIgstValue()))
                                        .build()
                                ).collect(Collectors.toList())
                )

                // ===== GST SNAPSHOT =====
                .gstSnapshot(
                        gstSnapshotRepo.findByInvoiceId(inv.getId())
                                .map(s -> GstSnapshotResponse.builder()
                                        .gstName(s.getGstName())
                                        .gstNumber(s.getGstNumber())
                                        .stateCode(s.getStateCode())
                                        .gstHolderName(s.getGstHolderName())
                                        .build())
                                .orElse(null)
                )

                // ===== BANK SNAPSHOT =====
                .bankSnapshot(
                        bankSnapshotRepo.findByInvoiceId(inv.getId())
                                .map(b -> BankSnapshotResponse.builder()
                                        .bankName(b.getBankName())
                                        .branchName(b.getBranchName())
                                        .accountNumber(b.getAccountNumber())
                                        .ifscCode(b.getIfscCode())
                                        .build())
                                .orElse(null)
                )

                // ===== CREDIT NOTE =====
                .creditNote(
                        creditNoteRepo.findByInvoiceId(inv.getId())
                                .map(c -> CreditNoteResponse.builder()
                                        .creditNoteNumber(c.getCreditNoteNumber())
                                        .creditNoteAmount(toBigDecimal(c.getCreditNoteAmount()))
                                        .mismatchAmount(toBigDecimal(c.getMismatchAmount()))
                                        .reason(c.getReason())
                                        .build())
                                .orElse(null)
                )

                .build();
    }
    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

//    private InvoiceResponse mapToInvoiceResponse(InvoiceMaster inv) {
//
//        return InvoiceResponse.builder()
//                .invoiceId(inv.getId())
//                .invoiceNumber(inv.getInvoiceNumber())
//                .ddoId(inv.getDdoId())
//                .gstId(inv.getGstId())
//                .customerId(inv.getCustomerId())
//                .invoiceDate(inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : null)
//                .invoiceStatus(inv.getInvoiceStatus())
//                .remarks(inv.getRemarks())
//                .totalAmount(BigDecimal.valueOf(inv.getTotalAmount()))
//                .totalCgst(BigDecimal.valueOf(inv.getTotalCgst()))
//                .totalSgst(BigDecimal.valueOf(inv.getTotalSgst()))
//                .totalIgst(BigDecimal.valueOf(inv.getTotalIgst()))
//                .grandTotal(BigDecimal.valueOf(inv.getGrandTotal()))
//                .paidAmount(BigDecimal.valueOf(inv.getPaidAmount()))
//                .balanceAmount(BigDecimal.valueOf(inv.getBalanceAmount()))
//
//                // Items
//                .items(
//                        itemRepo.findByInvoiceId(inv.getId())
//                                .stream()
//                                .map(item -> InvoiceItemResponse.builder()
//                                        .itemId(item.getId())
//                                        .hsnId(item.getHsnId())
//                                        .serviceName(item.getServiceName())
//                                        .quantity(item.getQuantity())
//                                        .rate(item.getRate())
//                                        .amount(item.getAmount())
//                                        .cgstRate(item.getCgstRate())
//                                        .sgstRate(item.getSgstRate())
//                                        .igstRate(item.getIgstRate())
//                                        .cgstValue(item.getCgstValue())
//                                        .sgstValue(item.getSgstValue())
//                                        .igstValue(item.getIgstValue())
//                                        .build()
//                                ).collect(Collectors.toList())
//                )
//
//                // GST Snapshot
//                .gstSnapshot(
//                        gstSnapshotRepo.findByInvoiceId(inv.getId())
//                                .map(s -> GstSnapshotResponse.builder()
//                                        .gstName(s.getGstName())
//                                        .gstNumber(s.getGstNumber())
//                                        .stateCode(s.getStateCode())
//                                        .gstHolderName(s.getGstHolderName())
//                                        .build())
//                                .orElse(null)
//                )
//
//                // Bank Snapshot
//                .bankSnapshot(
//                        bankSnapshotRepo.findByInvoiceId(inv.getId())
//                                .map(b -> BankSnapshotResponse.builder()
//                                        .bankName(b.getBankName())
//                                        .branchName(b.getBranchName())
//                                        .accountNumber(b.getAccountNumber())
//                                        .ifscCode(b.getIfscCode())
//                                        .build())
//                                .orElse(null)
//                )
//
//                // Credit Note
//                .creditNote(
//                        creditNoteRepo.findByInvoiceId(inv.getId())
//                                .map(c -> CreditNoteResponse.builder()
//                                        .creditNoteNumber(c.getCreditNoteNumber())
//                                        .creditNoteAmount(BigDecimal.valueOf(c.getCreditNoteAmount()))
//                                        .mismatchAmount(BigDecimal.valueOf(c.getMismatchAmount()))
//                                        .reason(c.getReason())
//                                        .build())
//                                .orElse(null)
//                )
//
//                .build();
//    }


    @Override
    public String generateSavedInvoiceNumber(Integer gstId, Integer ddoId) {

        if (gstId == null || ddoId == null) {
            throw new RuntimeException("GST ID and DDO ID must not be null");
        }

        // Load GST
        GSTMaster gst = gstRepo.findById(gstId)
                .orElseThrow(() -> new RuntimeException("GST not found: " + gstId));

        // Load DDO
        User ddo = ddoRepo.findById(ddoId)
                .orElseThrow(() -> new RuntimeException("DDO not found: " + ddoId));

        // Prepare prefix
        String gstNum = gst.getGstNumber();
        String ddoCode = ddo.getDdoCode();

        String last3 = gstNum.substring(gstNum.length() - 3);
        String last4 = ddoCode.substring(ddoCode.length() - 4);

        LocalDate today = LocalDate.now();
        int year = today.getYear() % 100;
        String fy = (today.getMonthValue() >= 4)
                ? String.valueOf(year)
                : String.format("%02d", year - 1);

        String prefix = last3 + last4 + "/" + fy + "/PA";

        // Fetch last receipt
        String lastReceipt = invoiceRepo.findLastSavedInvoice(gstId, ddoId);

        int next = 1;
        if (lastReceipt != null) {
            String seq = lastReceipt.substring(lastReceipt.lastIndexOf("PA") + 2);
            next = Integer.parseInt(seq) + 1;
        }

        // Final generated receipt
        return prefix + String.format("%03d", next);
    }



    //TODO
    @Override
    public String generateSubmittedInvoiceNumber(Integer gstId, Integer ddoId) {

        if (gstId == null || ddoId == null) {
            throw new RuntimeException("GST ID and DDO ID must not be null");
        }

        // Load GST
        GSTMaster gst = gstRepo.findById(gstId)
                .orElseThrow(() -> new RuntimeException("GST not found: " + gstId));

        // Load DDO
        User ddo = ddoRepo.findById(ddoId)
                .orElseThrow(() -> new RuntimeException("DDO not found: " + ddoId));

        // Prepare prefix
        String gstNum = gst.getGstNumber();
        String ddoCode = ddo.getDdoCode();

        String last3 = gstNum.substring(gstNum.length() - 3);
        String last4 = ddoCode.substring(ddoCode.length() - 4);

        LocalDate today = LocalDate.now();
        int year = today.getYear() % 100;
        String fy = (today.getMonthValue() >= 4)
                ? String.valueOf(year)
                : String.format("%02d", year - 1);

        String prefix = last3 + last4 + "/" + fy + "/IN";

        // Fetch last receipt
        String lastReceipt = invoiceRepo.findLastSubmittedInvoice(gstId, ddoId);

        int next = 1;
        if (lastReceipt != null) {
            String seq = lastReceipt.substring(lastReceipt.lastIndexOf("IN") + 2);
            next = Integer.parseInt(seq) + 1;
        }

        // Final generated receipt
        return prefix + String.format("%03d", next);
    }

//    @Override
//    public String generateSavedInvoiceNumber(Integer gstId, Integer ddoId) {
//
//        String gstNum = gstRepo.findById(gstId)
//                .map(GSTMaster::getGstNumber)
//                .orElseThrow(() -> new RuntimeException("GST not found"));
//
//        String last3 = gstNum.substring(gstNum.length() - 3);
//
//        String ddoCode = ddoRepo.findById(ddoId)
//                .map(User::getDdoCode)
//                .orElseThrow(() -> new RuntimeException("DDO not found"));
//
//        // 🔹 FY in 25-26 format
//        LocalDate today = LocalDate.now();
//        String financialYearDet=getCurrentFinancialYear();
//        int year = today.getYear() % 100;
//        String financialYear = (today.getMonthValue() >= 4)
//                ? year + "-" + String.format("%02d", year + 1)
//                : String.format("%02d", year - 1) + "-" + year;
//
//        String prefix = last3 + "/" + ddoCode + "/" + financialYear + "/PA";
//
//        // 🔥 Now filter by FY + Status + Prefix
//        Optional<InvoiceMaster> lastSaved =
//                invoiceRepo.findTopByFinancialYearAndInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(
//                        financialYearDet, prefix, "SAVED"
//                );
//
//
//        int next = lastSaved.map(inv -> {
//            String lastNo = inv.getInvoiceNumber()
//                    .substring(inv.getInvoiceNumber().lastIndexOf("PI") + 2);
//            return Integer.parseInt(lastNo) + 1;
//        }).orElse(1);
//
//        return prefix + String.format("%04d", next);
//    }

//    @Override
//    public String generateSavedInvoiceNumber(Integer gstId, Integer ddoId) {
//
//        // ------- GST last 7 characters (example: "1ZX0032") -------
//        String gstNum = gstRepo.findById(gstId)
//                .map(GSTMaster::getGstNumber)
//                .orElseThrow(() -> new RuntimeException("GST not found"));
//
//        // Take last 7 chars for invoice: 1234567 -> "34567"
//        String gstCode = gstNum.substring(gstNum.length() - 7);
//
//        // ------- Current Year (YY) -------
//        int yearYY = LocalDate.now().getYear() % 100; // example: 2025 → 25
//
//        // ------- Prefix Format -------
//        // Example: 1ZX0032/25/PA
//        String prefix = gstCode + "/" + yearYY + "/PA";
//
//        // ------- Fetch last saved invoice -------
//        Optional<InvoiceMaster> lastSaved =
//                invoiceRepo.findTopByInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(
//                        prefix, "SAVED"
//                );
//
//        // ------- Calculate next running number -------
//        int next = lastSaved.map(inv -> {
//
//            String lastInv = inv.getInvoiceNumber();      // example: 1ZX0032/25/PA0021
//
//            // Extract the numeric part after "PA"
//            String number = lastInv.substring(lastInv.lastIndexOf("PA") + 2); // "0021"
//
//            return Integer.parseInt(number) + 1;
//
//        }).orElse(1);
//
//        // ------- Build final invoice number -------
//        return prefix + String.format("%04d", next);  // PA0001
//    }



    //    @Override
//    public String generateSavedInvoiceNumber(Integer gstId, Integer ddoId) {
//
//        String gstNum = gstRepo.findById(gstId)
//                .map(GSTMaster::getGstNumber)
//                .orElseThrow(() -> new RuntimeException("GST not found"));
//
//        String last3 = gstNum.substring(gstNum.length() - 3);
//
//        String ddoCode = ddoRepo.findById(ddoId)
//                .map(User::getDdoCode)
//                .orElseThrow(() -> new RuntimeException("DDO not found"));
//
//        // FY → 25-26 format
//        LocalDate today = LocalDate.now();
//        int year = today.getYear() % 100;
//        String fy = (today.getMonthValue() >= 4)
//                ? year + "-" + String.format("%02d", year + 1)
//                : String.format("%02d", year - 1) + "-" + year;
//
//        String prefix = last3 + "/" + ddoCode + "/" + fy + "/PI";
//
//        // 🔹 Only count PI invoices
//        Optional<InvoiceMaster> lastSaved =
//                invoiceRepo.findTopByInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(
//                        prefix, "SAVED");
//
//        int next = lastSaved.map(inv -> {
//            String lastNo = inv.getInvoiceNumber().substring(inv.getInvoiceNumber().lastIndexOf("PI") + 2);
//            return Integer.parseInt(lastNo) + 1;
//        }).orElse(1);
//
//        return prefix + String.format("%04d", next);
//    }
@Override
@Transactional
public String generateFinalInvoiceNumber(Integer gstId, Integer ddoId) {

    // GST last 3 digits
    String gstNum = gstRepo.findById(gstId)
            .map(GSTMaster::getGstNumber)
            .orElseThrow(() -> new RuntimeException("GST not found"));
    String last3 = gstNum.substring(gstNum.length() - 3);

    // DDO Code
    String ddoCode = ddoRepo.findById(ddoId)
            .map(User::getDdoCode)
            .orElseThrow(() -> new RuntimeException("DDO not found"));

    // Financial Year (FY) format → 25-26
    LocalDate today = LocalDate.now();
    int yr = today.getYear() % 100;
    String fy = (today.getMonthValue() >= 4)
            ? String.format("%02d-%02d", yr, yr + 1)
            : String.format("%02d-%02d", yr - 1, yr);

    // PREFIX format → 123/DDO01/25-26/
    String prefix = last3 + "/" + ddoCode + "/" + fy + "/";
    String finalYear=getCurrentFinancialYear();
    // Fetch latest SUBMITTED invoice of same FY
    Optional<InvoiceMaster> lastFinal =
            invoiceRepo.findTopByFinalInvoiceNumberStartingWithAndInvoiceStatusAndFinancialYearOrderByIdDesc(
                    prefix, "SUBMITTED", finalYear);

    int nextNumber = lastFinal.map(inv -> {
        String seq = inv.getFinalInvoiceNumber()
                .substring(inv.getFinalInvoiceNumber().lastIndexOf("/") + 1)
                .trim();
        return Integer.parseInt(seq) + 1;
    }).orElse(1);

    return prefix + String.format("%04d", nextNumber);
}
    public static String getCurrentFinancialYear() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        if (today.getMonthValue() >= 4) {
            // April to December → current year - next abbreviated year
            return year + "-" + String.format("%02d", (year + 1) % 100);
        } else {
            // January to March → previous year - current abbreviated year
            return (year - 1) + "-" + String.format("%02d", year % 100);
        }
    }

//    @Override
//    public String generateSubmittedInvoiceNumber(Integer gstId, Integer ddoId) {
//
//        String gstNum = gstRepo.findById(gstId)
//                .map(GSTMaster::getGstNumber)
//                .orElseThrow(() -> new RuntimeException("GST not found"));
//
//        String last3 = gstNum.substring(gstNum.length() - 3);
//
//        String ddoCode = ddoRepo.findById(ddoId)
//                .map(User::getDdoCode)
//                .orElseThrow(() -> new RuntimeException("DDO not found"));
//
//        // FY → 25-26 format
//        LocalDate today = LocalDate.now();
//        int year = today.getYear() % 100;
//        String fy = (today.getMonthValue() >= 4)
//                ? year + "-" + String.format("%02d", year + 1)
//                : String.format("%02d", year - 1) + "-" + year;
//
//        String prefix = last3 + "/" + ddoCode + "/" + fy + "/";
//
//        // 🔥 Count only SUBMITTED invoices
//        Optional<InvoiceMaster> lastSubmitted =
//                invoiceRepo.findTopByInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(
//                        prefix, "SUBMITTED");
//
//        int next = lastSubmitted.map(inv -> {
//            String lastNo = inv.getInvoiceNumber().substring(inv.getInvoiceNumber().lastIndexOf("/") + 1);
//            return Integer.parseInt(lastNo) + 1;
//        }).orElse(1);
//
//        return prefix + String.format("%04d", next);
//    }

//    @Override
//    public String generateSubmittedInvoiceNumber(Integer gstId, Integer ddoId) {
//
//        // ------- Get Full GST Number -------
//        String gstCode = gstRepo.findById(gstId)
//                .map(GSTMaster::getGstNumber)
//                .orElseThrow(() -> new RuntimeException("GST not found"));
//
//        // ------- Extract Last 7 Characters (your example 1ZX0032) -------
//        String lastPart = gstCode.substring(gstCode.length() - 7);
//
//        // ------- Current Financial Year (YY) -------
//        int yearYY = LocalDate.now().getYear() % 100; // 2025 => 25
//
//        // ------- Invoice Prefix -------
//        // Example: 1ZX0032/25/IN
//        String prefix = lastPart + "/" + yearYY + "/IN";
//
//        // ------- Get Last Submitted Invoice -------
//        Optional<InvoiceMaster> lastSubmitted =
//                invoiceRepo.findTopByInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(
//                        prefix, "SUBMITTED"
//                );
//
//        // ------- Extract Next Running Number -------
//        int next = lastSubmitted.map(inv -> {
//            String invoice = inv.getInvoiceNumber();     // example: 1ZX0032/25/IN003
//            String numberPart = invoice.substring(invoice.lastIndexOf("IN") + 2);  // "003"
//            return Integer.parseInt(numberPart) + 1;
//        }).orElse(1);
//
//        // ------- Final Invoice Format -------
//        return prefix + String.format("%03d", next);  // IN001
//    }

    @Override
    public List<InvoiceResponse> getInvoices(Integer ddoId, Integer gstId,  String status,Boolean isShortfall) {

        try {
            List<Object[]> result = invoiceRepo.fetchInvoices(ddoId, gstId,  status,isShortfall);

            if (result == null || result.isEmpty()) {
                throw new ResourceNotFoundException("No invoices found for given filters!");
            }

            Map<Integer, InvoiceResponse> map = new LinkedHashMap<>();

            for (Object[] row : result) {

                InvoiceMaster inv = (InvoiceMaster) row[0];
                GSTSnapshot gstS = (GSTSnapshot) row[1];
                BankSnapshot bankS = (BankSnapshot) row[2];
                CustomerMaster cust = (CustomerMaster) row[3];
                CreditNote cn = (CreditNote) row[4];
                InvoiceItem item = (InvoiceItem) row[5];
                HSNMaster hsn = (HSNMaster) row[6];


                if (inv == null) continue;

                map.putIfAbsent(inv.getId(),
                        InvoiceResponse.builder()
                                .invoiceId(inv.getId())
                                .signImage(inv.getSignPath())
                                .ddoId(inv.getDdoId())
                                .gstId(inv.getGstId())
                                .customerId(inv.getCustomerId())
                                .invoiceNumber(inv.getInvoiceNumber())
                                .invoiceStatus(inv.getInvoiceStatus())
                                .invoiceDate(inv.getInvoiceDate())
                                .finalInvoiceNumber(inv.getFinalInvoiceNumber())
                                .finalInvoiceDate(inv.getSubmittedDate())
                                .receiptInvoiceDate(inv.getPaidDate())
                                .receiptInvoiceNumber(inv.getReceiptNumber())
                                .paymentType(inv.getPaymentType())
                                .paymentReferenceNumber(inv.getReferenceNumber())
                                .remarks(inv.getRemarks())
                                .status(inv.getStatus())
                                .notificationDetails(inv.getNotificationDetails())
                                .totalAmount(BigDecimal.valueOf(inv.getTotalAmount() == null ? 0.0 : inv.getTotalAmount()))
                                .totalCgst(BigDecimal.valueOf(inv.getTotalCgst() == null ? 0.0 : inv.getTotalCgst()))
                                .totalSgst(BigDecimal.valueOf(inv.getTotalSgst() == null ? 0.0 : inv.getTotalSgst()))
                                .totalIgst(BigDecimal.valueOf(inv.getTotalIgst() == null ? 0.0 : inv.getTotalIgst()))
                                .grandTotal(BigDecimal.valueOf(inv.getGrandTotal() == null ? 0.0 : inv.getGrandTotal()))
                                .paidAmount(BigDecimal.valueOf(inv.getPaidAmount() == null ? 0.0 : inv.getPaidAmount()))
                                .balanceAmount(BigDecimal.valueOf(inv.getBalanceAmount() == null ? 0.0 : inv.getBalanceAmount()))
                                .items(new ArrayList<>())
                                .build()
                );

                InvoiceResponse dto = map.get(inv.getId());

                if (dto.getGstSnapshot() == null && gstS != null) {
                    dto.setGstSnapshot(new GstSnapshotResponse(
                            gstS.getGstName(),
                            gstS.getGstNumber(),
                            gstS.getStateCode(),
                            gstS.getGstHolderName()
                    ));
                }

                if (dto.getBankSnapshot() == null && bankS != null) {
                    dto.setBankSnapshot(new BankSnapshotResponse(
                            bankS.getBankName(),
                            bankS.getBranchName(),
                            bankS.getAccountNumber(),
                            bankS.getIfscCode()
                    ));
                }

                if (dto.getCustomerResponse() == null && cust != null) {
                    dto.setCustomerResponse(new CustomerResponse(
                            cust.getId(),
                            cust.getCustomerName(),
                            cust.getServiceType(),
                            cust.getStateCode(),
                            cust.getGstNumber(),
                            cust.getAddress()
                    ));
                }

                if (dto.getCreditNote() == null && cn != null) {
                    dto.setCreditNote(new CreditNoteResponse(
                            cn.getCreditNoteNumber(),
                            toBigDecimal(cn.getCreditNoteAmount())  ,
                           toBigDecimal(cn.getMismatchAmount()),
                            cn.getReason()
                    ));
                }

//                if (item != null) {
//                    dto.getItems().add(
//                            InvoiceItemResponse.builder()
//                                    .itemId(item.getId())
//                                    .hsnId(item.getHsnId())
//                                    .serviceName(item.getServiceName())
//                                    .quantity(item.getQuantity())
//                                    .rate(item.getRate())
//                                    .amount(item.getAmount())
//                                    .cgstRate(item.getCgstRate())
//                                    .sgstRate(item.getSgstRate())
//                                    .igstRate(item.getIgstRate())
//                                    .cgstValue(item.getCgstValue())
//                                    .sgstValue(item.getSgstValue())
//                                    .igstValue(item.getIgstValue())
//                                    .build()
//                    );
//                }
                if (item != null) {
                    dto.getItems().add(
                            InvoiceItemResponse.builder()
                                    .itemId(item.getId())
                                    .hsnId(item.getHsnId())
                                    .serviceName(item.getServiceName())
                                    .quantity(item.getQuantity())
                                    .rate(item.getRate())
                                    .amount(item.getAmount())
                                    .cgstRate(item.getCgstRate())
                                    .sgstRate(item.getSgstRate())
                                    .igstRate(item.getIgstRate())
                                    .cgstValue(item.getCgstValue())
                                    .sgstValue(item.getSgstValue())
                                    .igstValue(item.getIgstValue())

                                    // ✅ HSN mapping
                                    .hsnCode(hsn != null ? hsn.getHsnCode() : null)
                                    .build()
                    );
                }

            }

            return new ArrayList<>(map.values());

        } catch (ResourceNotFoundException ex) {
            //log.error("Invoice fetch failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception e) {
           // log.error("Unexpected error while fetching invoices", e);
            throw new RuntimeException("Unexpected error while fetching invoices!");
        }
    }


    @Override
    public Map<String, Object> generateBulkReceiptNumbers(ReceiptGenerateRequest request) {

        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new RuntimeException("Entries cannot be empty");
        }

        // Extract all GST + DDO IDs
        Set<Integer> gstIds = request.getEntries().stream()
                .map(ReceiptGenerateRequest.ReceiptEntry::getGstId)
                .collect(Collectors.toSet());

        Set<Integer> ddoIds = request.getEntries().stream()
                .map(ReceiptGenerateRequest.ReceiptEntry::getDdoId)
                .collect(Collectors.toSet());

        // ------------------------------------------
        // 1️⃣ BATCH LOAD ALL GST RECORDS (ONE QUERY)
        // ------------------------------------------
        Map<Integer, String> gstMap = gstRepo.findByIdIn(gstIds).stream()
                .collect(Collectors.toMap(GSTMaster::getId, GSTMaster::getGstNumber));

        // ------------------------------------------
        // 2️⃣ BATCH LOAD ALL DDO RECORDS (ONE QUERY)
        // ------------------------------------------
        Map<Integer, String> ddoMap = ddoRepo.findByIdIn(ddoIds).stream()
                .collect(Collectors.toMap(User::getId, User::getDdoCode));

        // ------------------------------------------
        // Prepare prefixes
        // ------------------------------------------
        Map<String, String> prefixMap = new HashMap<>();

        LocalDate today = LocalDate.now();
        int year = today.getYear() % 100;
        String fy = (today.getMonthValue() >= 4)
                ? String.valueOf(year)
                : String.format("%02d", year - 1);

        Set<String> keys = new HashSet<>();

        for (ReceiptGenerateRequest.ReceiptEntry entry : request.getEntries()) {

            Integer gstId = entry.getGstId();
            Integer ddoId = entry.getDdoId();

            String gstNum = gstMap.get(gstId);
            if (gstNum == null) throw new RuntimeException("GST not found: " + gstId);

            String ddoCode = ddoMap.get(ddoId);
            if (ddoCode == null) throw new RuntimeException("DDO not found: " + ddoId);

            String last3 = gstNum.substring(gstNum.length() - 3);
            String last4DDO = ddoCode.substring(ddoCode.length() - 4);

            // Format → 1ZX0032/25/PR
            String prefix = last3 + last4DDO + "/" + fy + "/PR";

            String key = gstId + "-" + ddoId;
            prefixMap.put(key, prefix);
            keys.add(key);
        }

        // ------------------------------------------
        // 3️⃣ Fetch last receipts for all keys (ONE QUERY)
        // ------------------------------------------
        List<Object[]> dbResults = invoiceRepo.findLastReceiptsForKeys(keys);

        Map<String, String> lastMap = new HashMap<>();
        for (Object[] row : dbResults) {
            Integer ddoId = (Integer) row[0];
            Integer gstId = (Integer) row[1];
            String rec = (String) row[2];
            lastMap.put(gstId + "-" + ddoId, rec);
        }

        // ------------------------------------------
        // 4️⃣ Generate new receipt numbers
        // ------------------------------------------
        Map<String, String> receiptMap = new HashMap<>();

        for (String key : keys) {
            String prefix = prefixMap.get(key);
            String last = lastMap.get(key);

            int next = 1;
            if (last != null) {
                String seq = last.substring(last.lastIndexOf("PR") + 2);
                next = Integer.parseInt(seq) + 1;
            }

            receiptMap.put(key, prefix + String.format("%03d", next));
        }

        // Final Response
        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("generatedReceipts", receiptMap);

        return res;
    }



    public String generateSingleReceiptNumber(Integer gstId, Integer ddoId) {

        if (gstId == null || ddoId == null) {
            throw new RuntimeException("GST ID and DDO ID must not be null");
        }

        // Load GST
        GSTMaster gst = gstRepo.findById(gstId)
                .orElseThrow(() -> new RuntimeException("GST not found: " + gstId));

        // Load DDO
        User ddo = ddoRepo.findById(ddoId)
                .orElseThrow(() -> new RuntimeException("DDO not found: " + ddoId));

        // Prepare prefix
        String gstNum = gst.getGstNumber();
        String ddoCode = ddo.getDdoCode();

        String last3 = gstNum.substring(gstNum.length() - 3);
        String last4 = ddoCode.substring(ddoCode.length() - 4);

        LocalDate today = LocalDate.now();
        int year = today.getYear() % 100;
        String fy = (today.getMonthValue() >= 4)
                ? String.valueOf(year)
                : String.format("%02d", year - 1);

        String prefix = last3 + last4 + "/" + fy + "/PR";

        // Fetch last receipt
        String lastReceipt = invoiceRepo.findLastReceipt(gstId, ddoId);

        int next = 1;
        if (lastReceipt != null) {
            String seq = lastReceipt.substring(lastReceipt.lastIndexOf("PR") + 2);
            next = Integer.parseInt(seq) + 1;
        }

        // Final generated receipt
        return prefix + String.format("%03d", next);
    }



    @Transactional
    @Override
    public Map<String, Object> createBulkReceipts(ReceiptBulkCreateRequest request) {

        Map<String, Object> response = new HashMap<>();

        if (request.getReceipts() == null || request.getReceipts().isEmpty()) {
            response.put("status", "error");
            response.put("message", "Receipts list cannot be empty");
            return response;
        }

        // 1️⃣ Collect invoice IDs
        List<Integer> invoiceIds = request.getReceipts().stream()
                .map(ReceiptCreationRequest::getInvoiceId)
                .toList();

        // 2️⃣ Fetch invoices from DB
        List<InvoiceMaster> invoices = invoiceRepo.findAllByIds(invoiceIds);

        if (invoices.isEmpty()) {
            response.put("status", "error");
            response.put("message", "No invoices found for provided IDs");
            return response;
        }

        Map<Integer, InvoiceMaster> invoiceMap =
                invoices.stream().collect(Collectors.toMap(InvoiceMaster::getId, i -> i));

        List<Map<String, String>> updatedInvoices = new ArrayList<>();

        // 3️⃣ Process every invoice ONE BY ONE
        for (ReceiptCreationRequest r : request.getReceipts()) {

            InvoiceMaster invoice = invoiceMap.get(r.getInvoiceId());
            if (invoice == null) continue;

            Integer gstId = invoice.getGstId();
            Integer ddoId = invoice.getDdoId();

            // ⭐ 4️⃣ Generate receipt individually → NO KEY VALUE
            String receiptNumber = generateSingleReceiptNumber(gstId, ddoId);

            // 5️⃣ Update invoice fields
            invoice.setReceiptNumber(receiptNumber);
            invoice.setPaymentType(r.getType());
            invoice.setReferenceNumber(r.getReferenceNumber());
            invoice.setPaidDate(r.getPaymentDate());
            invoice.setDifferenceReason(r.getDifferenceReason());

            invoice.setPaidAmount(r.getAmountPaid());
            invoice.setBalanceAmount(invoice.getGrandTotal() - r.getAmountPaid());

            invoice.setInvoiceStatus("SUBMITTED");

            // Response item
            Map<String, String> map = new HashMap<>();
            map.put("invoiceNumber", invoice.getInvoiceNumber());
            map.put("receiptNumber", receiptNumber);
            updatedInvoices.add(map);
        }

        // 6️⃣ Save all
        invoiceRepo.saveAll(invoiceMap.values());

        response.put("status", "success");
        response.put("message", "Receipts created and invoices updated successfully");
        response.put("updatedInvoices", updatedInvoices);
        return response;
    }

    @Override
    public Map<String, Object> deleteOrCancelInvoice(Integer invoiceId, String status) {
        if (status==null || (!status.equalsIgnoreCase("CANCEL") && !status.equalsIgnoreCase("DELETE"))) {
            throw new RuntimeException("Invalid status. Must be either 'Cancel' or 'Delete'.");
        }
        Map<String,Object> response=new HashMap<>();
        InvoiceMaster invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
        if(status.equalsIgnoreCase("cancel")){
            invoice.setTotalAmount(0.0);
            invoice.setStatus("cancel");
            invoiceRepo.save(invoice);
            response.put("message", "Invoice cancelled successfully.");
            response.put("status", "success");
        } else if(status.equalsIgnoreCase("delete")){
            invoice.setStatus("delete");
            invoiceRepo.save(invoice);
            response.put("message", "Invoice deleted successfully.");
            response.put("status", "success");
        }
        return response;
    }

//    public Map<String, Object> createBulkReceipts(ReceiptBulkCreateRequest request) {
//        System.err.println("Inside createBulkReceipts");
//
//        Map<String, Object> response = new HashMap<>();
//
//        // 1️⃣ Validate request
//        if (request.getReceipts() == null || request.getReceipts().isEmpty()) {
//            System.err.println("Receipts list is empty");
//            response.put("status", "error");
//            response.put("message", "Receipts list cannot be empty");
//            response.put("data", null);
//            return response;
//        }
//
//        // 2️⃣ Collect invoice IDs
//        List<Integer> invoiceIds = request.getReceipts().stream()
//                .map(ReceiptCreationRequest::getInvoiceId)
//                .toList();
//        System.err.println("Invoice IDs from request: " + invoiceIds);
//
//        // 3️⃣ Fetch invoices in batch
//        List<InvoiceMaster> invoices = invoiceRepo.findAllByIds(invoiceIds);
//        System.err.println("Invoices fetched from DB: " + invoices);
//
//        if (invoices.isEmpty()) {
//            System.err.println("No invoices found for provided IDs");
//            response.put("status", "error");
//            response.put("message", "No invoices found for provided IDs");
//            response.put("data", null);
//            return response;
//        }
//
//        Map<Integer, InvoiceMaster> invoiceMap = invoices.stream()
//                .collect(Collectors.toMap(InvoiceMaster::getId, i -> i));
//        System.err.println("Invoice Map: " + invoiceMap);
//
//        // 4️⃣ Build ReceiptGenerateRequest entries
//        List<ReceiptGenerateRequest.ReceiptEntry> entries = new ArrayList<>();
//        for (ReceiptCreationRequest r : request.getReceipts()) {
//            InvoiceMaster invoice = invoiceMap.get(r.getInvoiceId());
//            if (invoice == null) {
//                System.err.println("Invoice not found in map for ID: " + r.getInvoiceId());
//                continue;
//            }
//            if (invoice.getGstId() == null || invoice.getDdoId() == null) {
//                System.err.println("Skipping invoice due to null GST/DDO: " + invoice.getInvoiceNumber());
//                continue;
//            }
//            entries.add(new ReceiptGenerateRequest.ReceiptEntry(invoice.getGstId(), invoice.getDdoId()));
//        }
//
//        // 5️⃣ Check if any entries are valid
//        if (entries.isEmpty()) {
//            System.err.println("No valid invoices found for receipt generation");
//            response.put("status", "error");
//            response.put("message", "No valid invoices found to generate receipts. Please check invoice IDs and GST/DDO values.");
//            response.put("data", null);
//            return response;
//        }
//
//        // Optional: add a readable toString for logging
//        System.err.println("ReceiptGenerateRequest entries: " + entries);
//
//        ReceiptGenerateRequest genReq = new ReceiptGenerateRequest();
//        genReq.setEntries(entries);
//
//        // 6️⃣ Generate receipts
//        Map<String, Object> generated;
//        try {
//            generated = generateBulkReceiptNumbers(genReq);
//            System.err.println("Generated receipts: " + generated);
//        } catch (RuntimeException e) {
//            System.err.println("Error in generateBulkReceiptNumbers: " + e.getMessage());
//            response.put("status", "error");
//            response.put("message", e.getMessage());
//            response.put("data", null);
//            return response;
//        }
//
//        Map<String, String> generatedMap = (Map<String, String>) generated.get("generatedReceipts");
//        System.err.println("Generated Receipts Map: " + generatedMap);
//
//        // 7️⃣ Update invoices
//        List<Map<String, String>> updatedInvoices = new ArrayList<>();
//        for (ReceiptCreationRequest r : request.getReceipts()) {
//            InvoiceMaster invoice = invoiceMap.get(r.getInvoiceId());
//            if (invoice == null) {
//                System.err.println("Skipping invoiceId not found in map: " + r.getInvoiceId());
//                continue;
//            }
//
//            String key = invoice.getGstId() + "-" + invoice.getDdoId();
//            String generatedReceipt = generatedMap.get(key);
//            if (generatedReceipt == null) {
//                System.err.println("No generated receipt found for key: " + key);
//                continue;
//            }
//
//            System.err.println("Setting receipt for invoice " + invoice.getInvoiceNumber() + ": " + generatedReceipt);
//
//            invoice.setReceiptNumber(generatedReceipt);
//            invoice.setPaymentType(r.getType());
//            invoice.setReferenceNumber(r.getReferenceNumber());
//            invoice.setPaidDate(r.getPaymentDate());
//
//            // Handle amounts safely
//            double previousPaid = invoice.getPaidAmount() == null ? 0.0 : invoice.getPaidAmount();
//            double newPaidAmount = previousPaid + (r.getAmountPaid() != null ? r.getAmountPaid() : 0.0);
//            invoice.setPaidAmount(newPaidAmount);
//            invoice.setBalanceAmount(invoice.getGrandTotal() - newPaidAmount);
//
//            invoice.setInvoiceStatus("RECEIPT");
//
//            Map<String, String> map = new HashMap<>();
//            map.put("invoiceNumber", invoice.getInvoiceNumber());
//            map.put("receiptNumber", generatedReceipt);
//            updatedInvoices.add(map);
//        }
//
//        // 8️⃣ Save all invoices in batch
//        invoiceRepo.saveAll(invoiceMap.values());
//
//        response.put("status", "success");
//        response.put("updatedInvoices", updatedInvoices);
//
//        System.err.println("Completed createBulkReceipts method");
//        return response;
//    }

//    public Map<String, Object> createBulkReceipts(ReceiptBulkCreateRequest request) {
//        System.err.println("Inside the method createBulkReceipts");
//
//        Map<String, Object> response = new HashMap<>();
//
//        if (request.getReceipts() == null || request.getReceipts().isEmpty()) {
//            System.err.println("Receipts list is empty");
//            response.put("status", "error");
//            response.put("message", "Receipts list cannot be empty");
//            response.put("data", null);
//            return response;
//        }
//
//        // Collect invoice IDs
//        List<Integer> invoiceIds = request.getReceipts().stream()
//                .map(ReceiptCreationRequest::getInvoiceId)
//                .toList();
//        System.err.println("Invoice IDs from request: " + invoiceIds);
//
//        // Fetch invoices
//        List<InvoiceMaster> invoices = invoiceRepo.findAllByIds(invoiceIds);
//        System.err.println("Invoices fetched from DB: " + invoices);
//
//        if (invoices.isEmpty()) {
//            System.err.println("No invoices found for provided IDs");
//            response.put("status", "error");
//            response.put("message", "No invoices found for provided IDs");
//            response.put("data", null);
//            return response;
//        }
//
//        Map<Integer, InvoiceMaster> invoiceMap = invoices.stream()
//                .collect(Collectors.toMap(InvoiceMaster::getId, i -> i));
//        System.err.println("Invoice Map: " + invoiceMap);
//
//        // Build ReceiptGenerateRequest
//        List<ReceiptGenerateRequest.ReceiptEntry> entries = new ArrayList<>();
//        for (ReceiptCreationRequest r : request.getReceipts()) {
//            System.err.println("Processing invoiceId: " + r.getInvoiceId());
//            InvoiceMaster invoice = invoiceMap.get(r.getInvoiceId());
//            if (invoice == null) {
//                System.err.println("Invoice not found in map for ID: " + r.getInvoiceId());
//                continue;
//            }
//            System.err.println("Invoice found: " + invoice.getInvoiceNumber() + " | GST: " + invoice.getGstId() + " | DDO: " + invoice.getDdoId());
//            entries.add(new ReceiptGenerateRequest.ReceiptEntry(invoice.getGstId(), invoice.getDdoId()));
//        }
//        System.err.println("ReceiptGenerateRequest entries: " + entries);
//
//        if (entries.isEmpty()) {
//            System.err.println("No valid invoices found for receipt generation");
//            response.put("status", "error");
//            response.put("message", "No valid invoices found to generate receipts. Please check invoice IDs.");
//            response.put("data", null);
//            return response;
//        }
//
//        ReceiptGenerateRequest genReq = new ReceiptGenerateRequest();
//        genReq.setEntries(entries);
//
//        System.err.println(genReq);
//        System.err.println("entes"+entries);
//
//        // Generate receipts
//        Map<String, Object> generated = generateBulkReceiptNumbers(genReq);
//        System.err.println(generated);
//        Map<String, String> generatedMap = (Map<String, String>) generated.get("generatedReceipts");
//        System.err.println("Generated Receipts Map: " + generatedMap);
//
//        List<Map<String, String>> updatedInvoices = new ArrayList<>();
//
//        for (ReceiptCreationRequest r : request.getReceipts()) {
//            InvoiceMaster invoice = invoiceMap.get(r.getInvoiceId());
//            if (invoice == null) {
//                System.err.println("Skipping invoiceId not found in map: " + r.getInvoiceId());
//                continue;
//            }
//
//            String key = invoice.getGstId() + "-" + invoice.getDdoId();
//            System.err.println("Key for generated receipt: " + key);
//
//            String generatedReceipt = generatedMap.get(key);
//            if (generatedReceipt == null) {
//                System.err.println("No generated receipt found for key: " + key);
//                continue;
//            }
//
//            System.err.println("Setting receipt for invoice " + invoice.getInvoiceNumber() + ": " + generatedReceipt);
//
//            invoice.setReceiptNumber(generatedReceipt);
//            invoice.setPaymentType(r.getType());
//            invoice.setReferenceNumber(r.getReferenceNumber());
//            invoice.setPaidDate(r.getPaymentDate());
//
//            invoice.setPaidAmount(r.getAmountPaid());
//            invoice.setBalanceAmount(invoice.getGrandTotal() - r.getAmountPaid());
//
//            invoice.setInvoiceStatus("RECEIPT");
//
//            Map<String, String> map = new HashMap<>();
//            map.put("invoiceNumber", invoice.getInvoiceNumber());
//            map.put("receiptNumber", generatedReceipt);
//            updatedInvoices.add(map);
//        }
//
//        invoiceRepo.saveAll(invoiceMap.values());
//
//        response.put("status", "success");
//        response.put("updatedInvoices", updatedInvoices);
//
//        System.err.println("Completed createBulkReceipts method");
//        return response;
//    }



//    public Map<String, Object> createBulkReceipts(ReceiptBulkCreateRequest request) {
//
//        Map<String, Object> response = new HashMap<>();
//        List<String> updatedReceipts = new ArrayList<>();
//
//        // ---------------------------------------------------------
//        // 1️⃣ Collect all invoice IDs
//        // ---------------------------------------------------------
//        List<Integer> invoiceIds = request.getReceipts()
//                .stream()
//                .map(ReceiptCreationRequest::getInvoiceId)
//                .toList();
//
//        // ---------------------------------------------------------
//        // 2️⃣ Fetch all invoices at once
//        // ---------------------------------------------------------
//        List<InvoiceMaster> invoices = invoiceRepo.findAllByIds(invoiceIds);
//
//        Map<Integer, InvoiceMaster> invoiceMap = invoices.stream()
//                .collect(Collectors.toMap(InvoiceMaster::getId, inv -> inv));
//
//        // ---------------------------------------------------------
//        // 3️⃣ Build ReceiptGenerateRequest
//        // ---------------------------------------------------------
//        List<ReceiptGenerateRequest.ReceiptEntry> entries = new ArrayList<>();
//
//        for (ReceiptCreationRequest entry : request.getReceipts()) {
//
//            InvoiceMaster invoice = invoiceMap.get(entry.getInvoiceId());
//            if (invoice == null) {
//                throw new RuntimeException("Invoice not found: " + entry.getInvoiceId());
//            }
//
//            entries.add(new ReceiptGenerateRequest.ReceiptEntry(
//                    invoice.getGstId(),
//                    invoice.getDdoId()
//            ));
//        }
//
//        ReceiptGenerateRequest genReq = new ReceiptGenerateRequest();
//        genReq.setEntries(entries);
//
//        // ---------------------------------------------------------
//        // 4️⃣ Generate bulk receipt numbers
//        // ---------------------------------------------------------
//        Map<String, Object> generated = generateBulkReceiptNumbers(genReq);
//
//        Map<String, String> generatedMap =
//                (Map<String, String>) generated.get("generatedReceipts");
//
//        // ---------------------------------------------------------
//        // 5️⃣ Update invoice records
//        // ---------------------------------------------------------
//        for (ReceiptCreationRequest entry : request.getReceipts()) {
//
//            InvoiceMaster invoice = invoiceMap.get(entry.getInvoiceId());
//
//            String key = invoice.getGstId() + "-" + invoice.getDdoId();
//            String generatedReceipt = generatedMap.get(key);
//
//            if (generatedReceipt == null) {
//                throw new RuntimeException("Receipt not generated for: " + key);
//            }
//
//            // Receipt number
//            invoice.setReceiptNumber(generatedReceipt);
//
//            // Payment details
//            invoice.setPaymentType(entry.getType());
//            invoice.setReferenceNumber(entry.getReferenceNumber());
//            invoice.setPaidDate(entry.getPaymentDate());
//
//            // Amounts
//            double previousPaid = invoice.getPaidAmount() == null ? 0.0 : invoice.getPaidAmount();
//            double newPaidAmount = previousPaid + entry.getAmountPaid();
//
//            invoice.setPaidAmount(newPaidAmount);
//            invoice.setBalanceAmount(invoice.getGrandTotal() - newPaidAmount);
//
//            // Status update
//            invoice.setInvoiceStatus("RECEIPT");
//
//            updatedReceipts.add(invoice.getInvoiceNumber());
//        }
//
//        // ---------------------------------------------------------
//        // 6️⃣ Save all invoices in batch
//        // ---------------------------------------------------------
//        invoiceRepo.saveAll(invoices);
//
//        // ---------------------------------------------------------
//        // 7️⃣ Final Response
//        // ---------------------------------------------------------
//        response.put("status", "success");
//        response.put("updatedInvoices", updatedReceipts);
//
//        return response;
//    }





    @Transactional
    @Override
    public void createShortfallInvoices(List<ShortfallRequest> requests) {

        Map<Integer, Double> amountMap = requests.stream()
                .collect(Collectors.toMap(
                        ShortfallRequest::getInvoiceId,
                        ShortfallRequest::getAmount
                ));

        List<Integer> invoiceIds = new ArrayList<>(amountMap.keySet());

        List<Object[]> rows =
                invoiceRepo.fetchInvoicesForShortfall(invoiceIds);

        if (rows.isEmpty()) {
            throw new RuntimeException("No invoices found");
        }

        // Group data by invoiceId
        Map<Integer, List<Object[]>> grouped =
                rows.stream().collect(Collectors.groupingBy(
                        r -> ((InvoiceMaster) r[0]).getId()
                ));

        List<InvoiceMaster> invoicesToSave = new ArrayList<>();
        List<InvoiceItem> itemsToSave = new ArrayList<>();
        List<GSTSnapshot> gstToSave = new ArrayList<>();
        List<BankSnapshot> bankToSave = new ArrayList<>();
        List<CreditNote> creditToSave = new ArrayList<>();

        for (Map.Entry<Integer, List<Object[]>> entry : grouped.entrySet()) {

            Integer parentId = entry.getKey();
            List<Object[]> data = entry.getValue();

            InvoiceMaster parent = (InvoiceMaster) data.get(0)[0];
            Double shortfallAmount = amountMap.get(parentId);

            if (shortfallAmount == null) continue;

            // ---------------------------
            // CREATE SHORTFALL INVOICE
            // ---------------------------
            InvoiceMaster shortfall = InvoiceMaster.builder()
                    .invoiceNumber(parent.getInvoiceNumber())
                    .ddoId(parent.getDdoId())
                    .gstId(parent.getGstId())
                    .bankId(parent.getBankId())
                    .customerId(parent.getCustomerId())
                    .invoiceDate(parent.getInvoiceDate())
                    .gstType(parent.getGstType())
                    .financialYear(parent.getFinancialYear())
                    .invoiceStatus("SAVED")
                    .totalAmount(shortfallAmount)
                    .totalIgst(parent.getTotalIgst())
                    .totalCgst(parent.getTotalCgst())
                    .totalSgst(parent.getTotalSgst())
                    .grandTotal(parent.getGrandTotal())
                    .paidAmount(0.0)
                    .balanceAmount(0.0)
                    .remarks(parent.getRemarks())
                    .notificationDetails(parent.getNotificationDetails())
                    .status("pending")
                    .isShortfall(true)
                    .build();

            invoicesToSave.add(shortfall);

            // ---------------------------
            // ITEMS
            // ---------------------------
            for (Object[] row : data) {
                InvoiceItem item = (InvoiceItem) row[4];
                if (item != null) {
                    itemsToSave.add(InvoiceItem.builder()
                            .invoiceId(shortfall.getId()) // set later
                            .hsnId(item.getHsnId())
                            .serviceName(item.getServiceName())
                            .quantity(item.getQuantity())
                            .rate(item.getRate())
                            .amount(item.getAmount())
                            .cgstRate(item.getCgstRate())
                            .sgstRate(item.getSgstRate())
                            .igstRate(item.getIgstRate())
                            .cgstValue(item.getCgstValue())
                            .sgstValue(item.getSgstValue())
                            .igstValue(item.getIgstValue())
                            .build());
                }
            }

            // ---------------------------
            // SNAPSHOTS (ONCE)
            // ---------------------------
            Object[] first = data.get(0);

            if (first[1] != null) {
                GSTSnapshot gs = (GSTSnapshot) first[1];
                gstToSave.add(new GSTSnapshot(
                        null,
                        shortfall.getId(),
                        gs.getGstName(),
                        gs.getGstNumber(),
                        gs.getStateCode(),
                        gs.getGstHolderName()
                ));
            }

            if (first[2] != null) {
                BankSnapshot bs = (BankSnapshot) first[2];
                bankToSave.add(new BankSnapshot(
                        null,
                        shortfall.getId(),
                        bs.getBankName(),
                        bs.getBranchName(),
                        bs.getAccountNumber(),
                        bs.getIfscCode()
                ));
            }

            if (first[3] != null) {
                CreditNote cn = (CreditNote) first[3];
                creditToSave.add(new CreditNote(
                        null,
                        shortfall.getId(),
                        cn.getCreditNoteNumber(),
                        cn.getCreditNoteAmount(),
                        cn.getMismatchAmount(),
                        cn.getReason()
                ));
            }
        }

        // ---------------------------
        // SAVE EVERYTHING (BATCH)
        // ---------------------------
        invoiceRepo.saveAll(invoicesToSave);

        itemRepo.saveAll(itemsToSave);
        gstSnapshotRepo.saveAll(gstToSave);
        bankSnapshotRepo.saveAll(bankToSave);
        creditNoteRepo.saveAll(creditToSave);
    }


}

