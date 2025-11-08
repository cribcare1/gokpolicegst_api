package com.dvl.tdsddo.request;

import lombok.*;

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
    private Integer createdBy;
    private String totalGst;
    private Integer gstId;
}
