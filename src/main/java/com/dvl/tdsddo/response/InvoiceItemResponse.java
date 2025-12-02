package com.dvl.tdsddo.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponse {
    private Integer itemId;
    private Integer hsnId;
    private String serviceName;
    private BigDecimal quantity;
    private BigDecimal rate;
    private BigDecimal amount;
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;
    private BigDecimal cgstValue;
    private BigDecimal sgstValue;
    private BigDecimal igstValue;
}

