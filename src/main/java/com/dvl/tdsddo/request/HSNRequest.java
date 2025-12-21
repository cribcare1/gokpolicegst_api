package com.dvl.tdsddo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HSNRequest {
    private Integer id;
    private String hsnCode;
    private String serviceName;
    private String igst;
    private String cgst;
    private String sgst;
    private String totalGst;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveFrom;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveTo;

    private Integer createdBy;
    private Integer gstId;
}
