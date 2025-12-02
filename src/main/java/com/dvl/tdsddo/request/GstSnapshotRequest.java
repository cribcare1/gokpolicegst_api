package com.dvl.tdsddo.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstSnapshotRequest {

    private String gstName;
    private String gstNumber;
    private String stateCode;
    private String gstHolderName;
}

