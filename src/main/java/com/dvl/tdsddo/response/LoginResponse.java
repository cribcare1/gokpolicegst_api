package com.dvl.tdsddo.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
	private Integer userId;
	private String fullName;
	private String userName;
	private String mobileNumber;
	private String email;
	private String role;
	private String token;
	private Integer ddoCount;
	private Integer form16ACount;
	private Integer form16Count;
}
