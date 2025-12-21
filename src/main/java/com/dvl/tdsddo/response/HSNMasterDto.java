package com.dvl.tdsddo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class HSNMasterDto {
    private Integer id;
    private String hsnCode;
    private Integer gstId;
    private String serviceName;
    private String totalGst;
    private String igst;
    private String cgst;
    private String sgst;
    private String gstNumber;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveFrom;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveTo;

    private Boolean isEditable;
}
