package com.dvl.tdsddo.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankSnapshotRequest {

    private String bankName;
    private String branchName;

    private String accountNumber;
    private String ifscCode;
}

