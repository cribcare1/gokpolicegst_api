package com.dvl.tdsddo.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankSnapshotResponse {
    private String bankName;
    private String branchName;
    private String accountNumber;
    private String ifscCode;
}
