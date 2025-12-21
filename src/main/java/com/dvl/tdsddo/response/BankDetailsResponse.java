package com.dvl.tdsddo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDetailsResponse {
    private Integer id;
    private String bankName;
    private String branchName;
    private String accountNumber;
    private String accountType;
    private String accountName;
    private String ifscCode;
    private String micrCode;
    private Integer gstId;
    private String gstName;
    private String gstNumber;
    private Boolean isEditable;
    private String status;
    private String effectiveFrom;
}

