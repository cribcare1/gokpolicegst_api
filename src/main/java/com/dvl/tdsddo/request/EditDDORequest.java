package com.dvl.tdsddo.request;

import lombok.Data;

@Data
public class EditDDORequest {
	private String name;
	private String contactNumber;
	private String contactPerson;
	private String ddocode;
	private String responsiblePerson;
	private String tanNumber;
	private String designation;
}
