package com.dvl.tdsddo.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Form16UploadRequest {
	private String financialYear;
	private String tanNumber;
	private String formType;
	private List<String> fileNames;
	private String panNumber;
}
