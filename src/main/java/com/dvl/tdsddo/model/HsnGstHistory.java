package com.dvl.tdsddo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "hsn_gst_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsnGstHistory extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer hsnId;

    private String totalGst;
    private String igst;
    private String cgst;
    private String sgst;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveFrom;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveTo;
}

