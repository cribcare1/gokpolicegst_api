package com.dvl.tdsddo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MonthlyGstFilingRequest {

    private Integer id; // null for add, non-null for update

    private Integer ddoId;
    private String filingMonth;
    private String arnNo;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate arnDate;

    private BigDecimal declaredAmount;
    private BigDecimal paidAmount;
    private BigDecimal penaltyAmount;

    private String remark;
}
