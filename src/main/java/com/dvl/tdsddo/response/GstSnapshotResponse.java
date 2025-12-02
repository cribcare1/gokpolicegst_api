package com.dvl.tdsddo.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstSnapshotResponse {
    private String gstName;
    private String gstNumber;
    private String stateCode;
    private String gstHolderName;
}

