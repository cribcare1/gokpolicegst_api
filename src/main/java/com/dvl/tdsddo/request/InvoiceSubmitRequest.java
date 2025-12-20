package com.dvl.tdsddo.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceSubmitRequest {
    private Integer invoiceId;

    private String remarks;
}
