package com.dvl.tdsddo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {

    private Integer id;  // null for create, non-null for update

    private String ddoName;   // ddoName


    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile;  // Mobile

    @Email(message = "Invalid email address")
    private String email;

    private String policeStation;

    private String ddoCode;

    private String address;

    private String area;

    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must be 6 digits")
    private String pinCode;

    private String city;

    private Integer gstInUserId;
    private Integer gstId;

    private String ddoTan;
    private String tanGstIn;

}

