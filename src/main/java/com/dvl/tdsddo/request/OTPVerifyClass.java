package com.dvl.tdsddo.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OTPVerifyClass {
	private String email;
	private String password;
	private String otp;
}
