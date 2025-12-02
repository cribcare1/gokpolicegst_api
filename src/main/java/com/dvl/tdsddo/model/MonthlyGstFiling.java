package com.dvl.tdsddo.model;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_gst_filing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyGstFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer ddoId;

    private String filingMonth;   // Example: "Sep-25"

    private String arnNo;

    private LocalDate arnDate;

    private BigDecimal declaredAmount;   // GST TDS Declared

    private BigDecimal paidAmount;       // GST TDS Paid

    private BigDecimal penaltyAmount;

    private String ackDocument;          // File path or URL

    private BigDecimal differenceAmount; // Declared - Paid

    private String remarks;

    private String status= TdsDdoConstant.ACTIVE;


}

