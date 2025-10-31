package com.dvl.tdsddo.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ViewDDOResponse {
	private String tanNumber;
	private String name;
	private String ddocode;
	private String contactPerson;
	private String contactNumber;
	private String responsiblePerson;
	private String designation;
}
