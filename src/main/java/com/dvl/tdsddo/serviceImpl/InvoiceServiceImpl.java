package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.Exception.ResourceNotFoundException;
import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.*;
import com.dvl.tdsddo.repository.*;
import com.dvl.tdsddo.request.InvoiceItemRequest;
import com.dvl.tdsddo.request.InvoiceRequest;
import com.dvl.tdsddo.request.InvoiceSubmitRequest;
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
    public InvoiceMaster submitInvoice(InvoiceSubmitRequest req) {

        if (req.getInvoiceId() == null) {
            throw new IllegalArgumentException("Invoice ID is required for submission!");
        }

        InvoiceMaster invoice = invoiceRepo.findById(req.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found for ID: " + req.getInvoiceId()));

        // Validate status transition
        if (!"SAVE".equalsIgnoreCase(invoice.getInvoiceStatus())) {
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
        invoice.setPaidAmount(req.getPaidAmount() != null ? req.getPaidAmount() : invoice.getPaidAmount());
        invoice.setBalanceAmount(req.getBalanceAmount() != null ? req.getBalanceAmount() : invoice.getBalanceAmount());

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
            List<InvoiceMaster> list = invoiceRepo.findByFilters(ddoId, gstId);

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

        String gstNum = gstRepo.findById(gstId)
                .map(GSTMaster::getGstNumber)
                .orElseThrow(() -> new RuntimeException("GST not found"));

        String last3 = gstNum.substring(gstNum.length() - 3);

        String ddoCode = ddoRepo.findById(ddoId)
                .map(User::getDdoCode)
                .orElseThrow(() -> new RuntimeException("DDO not found"));

        // 🔹 FY in 25-26 format
        LocalDate today = LocalDate.now();
        String financialYearDet=getCurrentFinancialYear();
        int year = today.getYear() % 100;
        String financialYear = (today.getMonthValue() >= 4)
                ? year + "-" + String.format("%02d", year + 1)
                : String.format("%02d", year - 1) + "-" + year;

        String prefix = last3 + "/" + ddoCode + "/" + financialYear + "/PI";

        // 🔥 Now filter by FY + Status + Prefix
        Optional<InvoiceMaster> lastSaved =
                invoiceRepo.findTopByFinancialYearAndInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(
                        financialYearDet, prefix, "SAVED"
                );


        int next = lastSaved.map(inv -> {
            String lastNo = inv.getInvoiceNumber()
                    .substring(inv.getInvoiceNumber().lastIndexOf("PI") + 2);
            return Integer.parseInt(lastNo) + 1;
        }).orElse(1);

        return prefix + String.format("%04d", next);
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

    @Override
    public String generateSubmittedInvoiceNumber(Integer gstId, Integer ddoId) {

        String gstNum = gstRepo.findById(gstId)
                .map(GSTMaster::getGstNumber)
                .orElseThrow(() -> new RuntimeException("GST not found"));

        String last3 = gstNum.substring(gstNum.length() - 3);

        String ddoCode = ddoRepo.findById(ddoId)
                .map(User::getDdoCode)
                .orElseThrow(() -> new RuntimeException("DDO not found"));

        // FY → 25-26 format
        LocalDate today = LocalDate.now();
        int year = today.getYear() % 100;
        String fy = (today.getMonthValue() >= 4)
                ? year + "-" + String.format("%02d", year + 1)
                : String.format("%02d", year - 1) + "-" + year;

        String prefix = last3 + "/" + ddoCode + "/" + fy + "/";

        // 🔥 Count only SUBMITTED invoices
        Optional<InvoiceMaster> lastSubmitted =
                invoiceRepo.findTopByInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(
                        prefix, "SUBMITTED");

        int next = lastSubmitted.map(inv -> {
            String lastNo = inv.getInvoiceNumber().substring(inv.getInvoiceNumber().lastIndexOf("/") + 1);
            return Integer.parseInt(lastNo) + 1;
        }).orElse(1);

        return prefix + String.format("%04d", next);
    }
    @Override
    public List<InvoiceResponse> getInvoices(Integer ddoId, Integer gstId,  String status) {

        try {
            List<Object[]> result = invoiceRepo.fetchInvoices(ddoId, gstId,  status);

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
                                .remarks(inv.getRemarks())
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
                            cust.getServiceType()
//                            cust.getBillingAddress()
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

}

