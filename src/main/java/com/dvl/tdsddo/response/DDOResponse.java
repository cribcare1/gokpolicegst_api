package com.dvl.tdsddo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DDOResponse {
	private Integer id;
	private String fullName;
	private String tanNumber;
}
