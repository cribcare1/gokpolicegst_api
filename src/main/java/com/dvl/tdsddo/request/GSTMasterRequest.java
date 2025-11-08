package com.dvl.tdsddo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request object for creating, updating, or partially updating GST Master.
 */
@Getter
@Setter
public class GSTMasterRequest {

    private Integer gstId;

    // 🔹 GST Holder Name
    @Size(max = 100, message = "GST holder name cannot exceed 100 characters")
    private String gstHolderName;

    // 🔹 GST Name
    @Size(max = 150, message = "GST name cannot exceed 150 characters")
    private String gstName;

    // 🔹 GST Number
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}[A-Z]{1}[0-9A-Z]{1}$",
            message = "Invalid GST number format"
    )
    private String gstNumber;



    // 🔹 Address
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    // 🔹 Email
    @Email(message = "Invalid email address")
    private String email;

    // 🔹 Mobile Number
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number format")
    private String mobile;

    // 🔹 State Code
    private Integer stateCode;

    // 🔹 PIN Code
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid PIN code format")
    private String pinCode;

    // 🔹 City
    private String city;

    // 🔹 Created By (Admin ID)
    private Integer createdBy;
}
