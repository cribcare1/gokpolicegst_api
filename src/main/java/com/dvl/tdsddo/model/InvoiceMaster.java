package com.dvl.tdsddo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "invoice_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceMaster extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String invoiceNumber;

    private Integer ddoId;
    private Integer gstId;

    private Integer bankId;
    private Integer customerId;

    private String receiptNumber;
    private String paymentType;//CASH  / BANK_TRANSFER
    private String referenceNumber; // Cheque number / Transaction ID
    private String paidDate;


    private String invoiceDate;

    private String finalInvoiceNumber;
    private String submittedDate;
    private String signPath;

    private String gstType;  // FCM / RCM / GOVT / EXEMPT

    private String financialYear;

    // DRAFT, PENDING, FINAL, PAID, CLOSED ,RECEIPT
    private String invoiceStatus;

    private Double totalAmount;  // Total without GST
    private Double totalIgst;
    private Double totalCgst;
    private Double totalSgst;

    private Double grandTotal;   // totalAmount + GST

    private Double paidAmount = 0.0;
    private Double balanceAmount = 0.0;

    private String status; // PENDING,CANCELLED,INACTIVE

    private String DifferenceReason;
    private String remarks;
    private String notificationDetails;
    private Boolean isShortfall=false;
}