package com.dvl.tdsddo.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GSTResponse {
    private Integer userId;
    private String email;
    private String address;
    private String mobile;
    private String city;
    private String pinCode;
    private Integer gstId;
    private String gstNumber;
    private String gstHolderName;
    private String gstName;
    private Integer ddoCount ;
    private Integer stateCode;
}
