package com.dvl.tdsddo.serviceImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.repository.UserRepository;

@Service
public class OtpService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JavaMailSender javaMailSender;

	@Autowired
	private PasswordEncoder encoder;

	// Store OTPs with expiry time
	private final Map<String, OtpEntry> otpStorage = new HashMap<>();

	public Map<String, Object> sendOtpForForgotPassword(String userNameOrEmail) {
		Map<String, Object> response = new HashMap<>();

		// 🔹 Check if user exists
		User user = userRepository.findByUserName(userNameOrEmail).orElse(null);
		if (user == null) {
			response.put("status", "error");
			response.put("message", "User not found!");
			return response;
		}

		// 🔹 Generate OTP
		String otp = generateOtp();
		LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5); // OTP expires in 5 minutes

		// 🔹 Store OTP with expiry time
		otpStorage.put(user.getEmail(), new OtpEntry(otp, expiryTime));

		// 🔹 Send OTP via email
		boolean emailSent = sendOtpEmail(user.getEmail(), otp);
		if (!emailSent) {
			response.put("status", "error");
			response.put("message", "Failed to send OTP!");
			response.put("email", user.getEmail());
			return response;
		}

		response.put("status", "success");
		response.put("message", "OTP sent successfully! It is valid for 5 minutes.");
		response.put("email", user.getEmail());
		return response;
	}

	public Map<String, Object> verifyOtp(String email, String enteredOtp, String password) {
		Map<String, Object> response = new HashMap<>();

		// 🔹 Check if OTP exists
		OtpEntry otpEntry = otpStorage.get(email);
		if (otpEntry == null) {
			response.put("status", "error");
			response.put("message", "Invalid or expired OTP!");
			return response;
		}

		// 🔹 Check if OTP is expired
		if (LocalDateTime.now().isAfter(otpEntry.getExpiryTime())) {
			otpStorage.remove(email); // Remove expired OTP
			response.put("status", "error");
			response.put("message", "OTP has expired!");
			return response;
		}

		// 🔹 Check if OTP is correct
		if (!otpEntry.getOtp().equals(enteredOtp)) {
			response.put("status", "error");
			response.put("message", "Invalid OTP!");
			return response;
		}

		// 🔹 OTP is valid
		User user = userRepository.findByEmailIdAndStatus(email).orElse(null);
		if (user != null) {
			user.setPassword(encoder.encode(password));
			userRepository.save(user);
		}
		otpStorage.remove(email); // Remove OTP after successful validation
		response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
		response.put(TdsDdoConstant.MESSAGE, "Password reset successfully!");
		return response;
	}

	private String generateOtp() {
		Random random = new Random();
		int otp = 100000 + random.nextInt(900000); // Generates a 6-digit OTP
		return String.valueOf(otp);
	}

	private boolean sendOtpEmail(String email, String otp) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom("cribcareinfo@gmail.com"); // ✅ Set a valid 'from' email
			message.setTo(email);
			message.setSubject("Password Reset OTP - Secure Verification");

			message.setText("Dear User,\n\n"
					+ "You have requested to reset your password. Your One-Time Password (OTP) for verification is:\n\n"
					+ otp + "\n\n"
					+ "This OTP is valid for 5 minutes. Please do not share this code with anyone for security reasons.\n\n"
					+ "If you did not request a password reset, please ignore this email or contact our support team immediately.\n\n"
					+ "Best regards,\nTDS Application Support Team");

			javaMailSender.send(message);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}


    public boolean sendCredentialsEmail(String email, String userId, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("cribcareinfo@gmail.com"); // Valid sender email
            message.setTo(email);
            message.setSubject("Your Account Login Credentials");

            message.setText(
                    "Dear User,\n\n" +
                            "Your account has been successfully created. Please find your login credentials below:\n\n" +
                            "User ID: " + userId + "\n" +
                            "Password: " + password + "\n\n" +
                            "For security reasons, we strongly recommend that you change your password after your first login.\n\n" +
                            "If you did not request this account or believe this email was sent in error, please contact our support team immediately.\n\n" +
                            "Best regards,\n" +
                            "TDS Application Support Team"
            );

            javaMailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Inner class to store OTP with expiry time
	private static class OtpEntry {
		private final String otp;
		private final LocalDateTime expiryTime;

		public OtpEntry(String otp, LocalDateTime expiryTime) {
			this.otp = otp;
			this.expiryTime = expiryTime;
		}

		public String getOtp() {
			return otp;
		}

		public LocalDateTime getExpiryTime() {
			return expiryTime;
		}
	}

	public Map<String, Object> changePassword(String userName, String newPassword) {
		Map<String, Object> response = new HashMap<>();

		// 🔹 Find the user by email
		User user = userRepository.findByUserName(userName).orElse(null);
		if (user == null) {
			response.put(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
			response.put(TdsDdoConstant.MESSAGE, "User not found!");
			return response;
		}

		// 🔹 Encrypt and update password
		user.setPassword(encoder.encode(newPassword));
		userRepository.save(user);

		response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
		response.put(TdsDdoConstant.MESSAGE, "Password changed successfully!");
		return response;
	}

}
