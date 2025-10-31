package com.dvl.tdsddo.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FormUploadRequest {
	private String financialYear;
	private String tanNumber;
	private String type;
}
