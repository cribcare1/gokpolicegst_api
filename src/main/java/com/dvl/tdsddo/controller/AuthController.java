package com.dvl.tdsddo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.request.AuthRequest;
import com.dvl.tdsddo.request.OTPVerifyClass;
import com.dvl.tdsddo.service.UserService;
import com.dvl.tdsddo.serviceImpl.OtpService;

@RestController
@RequestMapping("/tds/auth")
public class AuthController {
	@Autowired
	UserService userService;

	@Autowired
	private OtpService otpService;

	@PostMapping("/loginUsingUserNamePassword")
	public Map<String, Object> loginUsingUserNamePassword(@RequestBody AuthRequest authRequest) {
		return userService.loginUsingUserNamePassword(authRequest);
	}

	@PostMapping("/addAdmin")
	public Map<String, Object> addAdmin(@RequestBody User user) {
		return userService.addAdmin(user);
	}

	@GetMapping("/send-otp")
	public Map<String, Object> sendOtp(@RequestParam String userNameOrEmail) {
		return otpService.sendOtpForForgotPassword(userNameOrEmail);
	}

	@PostMapping("/verify-otp")
	public Map<String, Object> verifyOtp(@RequestBody OTPVerifyClass otpClass) {
		return otpService.verifyOtp(otpClass.getEmail(), otpClass.getOtp(), otpClass.getPassword());
	}

	@PostMapping("/resetPassword")
	public Map<String, Object> resetPassword(@RequestBody AuthRequest authRequest) {
		return otpService.changePassword(authRequest.getUserName(), authRequest.getPassword());
	}
}
