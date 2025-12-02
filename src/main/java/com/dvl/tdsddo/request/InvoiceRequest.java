package com.dvl.tdsddo.request;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    private Integer invoiceId;

    private Integer ddoId;
    private Integer bankId;
    private Integer gstId;
    private Integer customerId;
    private String invoiceNumber;

    private String invoiceDate;
    private String gstType;  // FCM / RCM / GOVT / EXEMPT

    private String invoiceStatus; //SAVE SUBMIT

    private String remarks;
    private Double totalAmount;
    private Double totalCgst;
    private Double totalSgst;
    private Double totalIgst;
    private Double grandTotal;


    private Double paidAmount;
    private Double balanceAmount;

    private List<InvoiceItemRequest> items;

    private GstSnapshotRequest gstSnapshot;

    private BankSnapshotRequest bankSnapshot;

    private CreditNoteRequest creditNote;
}

