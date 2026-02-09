package com.dvl.tdsddo.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Integer invoiceId;

    private Integer ddoId;
    private Integer gstId;
    private Integer customerId;

    private String invoiceNumber;
    private String invoiceStatus;
    private String invoiceDate;
    private String remarks;


    private String finalInvoiceNumber;
    private String finalInvoiceDate;
    // Totals
    private BigDecimal totalAmount;
    private BigDecimal totalCgst;
    private BigDecimal totalSgst;
    private BigDecimal totalIgst;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String signImage;
    private String receiptInvoiceNumber;
    private String receiptInvoiceDate;
private String paymentReferenceNumber;
    private String paymentType;
    private String status;
    private String notificationDetails;
    private String shortFallReason;
    // Nested Objects
    private List<InvoiceItemResponse> items;
    private GstSnapshotResponse gstSnapshot;
    private BankSnapshotResponse bankSnapshot;
    private CreditNoteResponse creditNote;
    private CustomerResponse customerResponse;
    public InvoiceResponse(Integer invoiceId, Integer ddoId, Integer gstId, Integer customerId,
                           String invoiceNumber, String invoiceStatus, String invoiceDate, String remarks,
                           BigDecimal totalAmount, BigDecimal totalCgst, BigDecimal totalSgst,
                           BigDecimal totalIgst, BigDecimal grandTotal,
                           BigDecimal paidAmount, BigDecimal balanceAmount,

                           String gstName, String gstNumber, String stateCode, String gstHolderName,
                           String bankName, String branchName, String accountNumber, String ifscCode,
                           String creditNoteNumber, BigDecimal creditNoteAmount,
                           BigDecimal mismatchAmount, String reason) {

        this.invoiceId = invoiceId;
        this.ddoId = ddoId;
        this.gstId = gstId;
        this.customerId = customerId;
        this.invoiceNumber = invoiceNumber;
        this.invoiceStatus = invoiceStatus;
        this.invoiceDate = invoiceDate;
        this.remarks = remarks;
        this.totalAmount = totalAmount;
        this.totalCgst = totalCgst;
        this.totalSgst = totalSgst;
        this.totalIgst = totalIgst;
        this.grandTotal = grandTotal;
        this.paidAmount = paidAmount;
        this.balanceAmount = balanceAmount;

        this.gstSnapshot = new GstSnapshotResponse(
                gstName, gstNumber, stateCode, gstHolderName
        );

        this.bankSnapshot = new BankSnapshotResponse(
                bankName, branchName, accountNumber, ifscCode
        );

        this.creditNote = new CreditNoteResponse(
                creditNoteNumber, creditNoteAmount, mismatchAmount, reason
        );
    }

}
