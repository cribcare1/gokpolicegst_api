package com.dvl.tdsddo.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Form16Response {
	private Integer id; // Serial Number
	private String financialYear; // Financial Year
	private String name; // Name of the Person
	private String panNumber; // PAN Number
	private String mobileNumber; // Mobile Number
	private String form16FilePath; // Form 16 PDF File Path (Download Link)
}
