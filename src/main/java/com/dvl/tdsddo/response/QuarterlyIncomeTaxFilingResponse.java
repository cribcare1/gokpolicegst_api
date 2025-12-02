package com.dvl.tdsddo.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class QuarterlyIncomeTaxFilingResponse {
    private Long id;

    private Integer ddoId; // added
    private String ddoCode;
    private String ddoOfficeName;
    private String tan;

    private String fiscalYear;
    private String returnType;
    private String quarter;

    private LocalDate dateOfFiling;
    private String provisionalReceiptNo;

    private Integer deducteeCount;
    private BigDecimal totalChallanAmount;
    private BigDecimal totalTaxDeducted;

    private Boolean anyRevisionFiled;
    private String ackDocument;

    private BigDecimal differenceInReporting;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

