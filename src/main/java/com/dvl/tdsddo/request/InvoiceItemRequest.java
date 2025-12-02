package com.dvl.tdsddo.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemRequest {

    private Integer hsnId;

    private String serviceName;

    private BigDecimal quantity;

    private BigDecimal rate;

    private BigDecimal amount;      // FROM FE

    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;
}

