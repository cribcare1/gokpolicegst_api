package com.dvl.tdsddo.entity;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.util.TdsUtil;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quarterly_income_tax_filing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarterlyIncomeTaxFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer ddoId; // new field for DDO id
    private String ddoCode;
    private String ddoOfficeName;
    private String tan;

    private String fiscalYear;
    private String returnType; // 24Q, 26Q, TCS-27EQ, 27Q
    private String quarter; // Q1..Q4

    private LocalDate dateOfFiling;
    private String provisionalReceiptNo; // 15-digit number as string

    private Integer deducteeCount;
    private BigDecimal totalChallanAmount;
    private BigDecimal totalTaxDeducted;

    private Boolean anyRevisionFiled;
    private String ackDocument; // stored path or url to uploaded PDF

    private BigDecimal differenceInReporting;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();

    }
}

