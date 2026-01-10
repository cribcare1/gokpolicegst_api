package com.dvl.tdsddo.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptCreationRequest {
    private Integer invoiceId;
    private String type;
    private String referenceNumber;
    private Double amountPaid;
    private String paymentDate;
    public String differenceReason;
    public double differenceAmount;
    private String shortfallRemark;
}
