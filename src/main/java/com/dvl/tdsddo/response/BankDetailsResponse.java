package com.dvl.tdsddo.response;

import lombok.*;

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
    private String gstName;   // ✅ New field for GST Name
    private String status;
}

