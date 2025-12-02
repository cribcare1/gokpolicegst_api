package com.dvl.tdsddo.request;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PanMasterRequest {

    private Integer id; // for update case

//    @NotBlank(message = "PAN Name is required")
    private String panName;

//    @NotBlank(message = "PAN Number is required")
    @Pattern(
            regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
            message = "Invalid PAN format (should be like ABCDE1234F)"
    )
    private String panNumber;

    private String address;

    private String email;

    private String mobile;

    private String pinCode;
    private String city;

    private String status = TdsDdoConstant.ACTIVE;

//    @NotNull(message = "CreatedBy user id is required")
    private Integer createdBy;
}
