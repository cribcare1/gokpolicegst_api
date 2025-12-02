package com.dvl.tdsddo.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
    private LocalDateTime effectiveDate;
    private Boolean isEditable;
}
