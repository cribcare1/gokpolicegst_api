package com.dvl.tdsddo.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MonthlyGstFilingResponse {

    private Integer id;
    private Integer ddoId; // added DDO id

    private String filingMonth;
    private String arnNo;
    private LocalDate arnDate;

    private BigDecimal declaredAmount;
    private BigDecimal paidAmount;
    private BigDecimal penaltyAmount;

    private BigDecimal differenceAmount;
    private String remarks;

    private String ackDocument;
    private String financialYear;
}
