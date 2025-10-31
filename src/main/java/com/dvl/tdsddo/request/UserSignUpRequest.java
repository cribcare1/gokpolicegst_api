package com.dvl.tdsddo.request;

import com.dvl.tdsddo.model.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignUpRequest {
	private User user;
	private Integer adminId;
}
