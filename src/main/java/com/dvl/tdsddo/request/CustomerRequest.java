package com.dvl.tdsddo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {
    private Integer id; // null for add, non-null for edit

    private String customerName;

    private String customerType;

    @Email(message = "Invalid email format")
    private String customerEmail;

    private String address;
    private String pinCode;
    private String stateCode;
    private String gstNumber;
    private String exemptionNumber;
    private String city;
    private String mobile;
    private String serviceType;

    private Integer ddoId;
}
