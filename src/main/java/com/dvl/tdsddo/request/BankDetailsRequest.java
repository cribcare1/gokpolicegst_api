package com.dvl.tdsddo.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankDetailsRequest {
    private Integer id;
    private String bankName;
    private String branchName;
    private String accountNumber;
    private String accountType;
    private String accountName;
    private String ifscCode;
    private String micrCode;
    private String status;
    private Integer gstId;
    private Integer createdBy;
    private Integer ddoId;
}

