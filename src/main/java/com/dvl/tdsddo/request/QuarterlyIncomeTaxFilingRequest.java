package com.dvl.tdsddo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class QuarterlyIncomeTaxFilingRequest {
    private Long id;

    private Integer ddoId; // added

    private String fiscalYear;
    private String returnType;
    private String quarter;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateOfFiling;

    private String provisionalReceiptNo;

    private Integer deducteeCount;
    private BigDecimal totalChallanAmount;
    private BigDecimal totalTaxDeducted;

    private Boolean anyRevisionFiled;
    private String ackDocument; // optional explicit path

    private BigDecimal differenceInReporting;
    private String remarks;
}

