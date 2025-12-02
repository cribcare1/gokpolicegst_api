package com.dvl.tdsddo.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceSubmitRequest {
    private Integer invoiceId;

    // Optional entry from UI when submitting
    private Double paidAmount;
    private Double balanceAmount;

    private String remarks;
}
