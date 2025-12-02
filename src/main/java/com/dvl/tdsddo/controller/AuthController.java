package com.dvl.tdsddo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import com.dvl.tdsddo.util.FileServiceUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    FileServiceUtil fileServiceUtil;

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

    @GetMapping("/getImage/{folderName}/{fileName}")
    public ResponseEntity<?> getImage(
            @PathVariable String folderName,
            @PathVariable String fileName,
            HttpServletResponse response
    ) {
        try {
            Resource image = fileServiceUtil.fetchImages(folderName, fileName);

            String contentType = Files.probeContentType(image.getFile().toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(image);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of()
            );
        }
    }
}
