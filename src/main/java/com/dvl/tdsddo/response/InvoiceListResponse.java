package com.dvl.tdsddo.response;

import lombok.Getter;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceListResponse {

    private Integer invoiceId;
    private String invoiceNumber;

    private Integer ddoId;
    private Integer customerId;

    private String invoiceDate;
    private String invoiceStatus;

    private BigDecimal totalAmount;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
}

