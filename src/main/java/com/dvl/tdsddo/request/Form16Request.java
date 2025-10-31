package com.dvl.tdsddo.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Form16Request {
	private Integer ddoId; // Selected DDO ID
	private String financialYear;
	private String name;
	private String panNumber;
	private String mobileNumber;
	private String email;
}
