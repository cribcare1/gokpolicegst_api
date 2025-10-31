package com.dvl.tdsddo.security;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.repository.UserRepository;

@Component
public class UserInfoUserDetailsService implements UserDetailsService {
	@Autowired
	UserRepository userRepository;

	private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

	private static final String MOBILE_REGEX = "^[0-9]{10}$";
	private static final Pattern MOBILE_PATTERN = Pattern.compile(MOBILE_REGEX);

	public static boolean isValidEmail(String email) {
		if (email == null)
			return false;
		Matcher matcher = EMAIL_PATTERN.matcher(email);
		return matcher.matches();
	}

	public static boolean isValidMobileNumber(String mobileNumber) {
		if (mobileNumber == null)
			return false;
		Matcher matcher = MOBILE_PATTERN.matcher(mobileNumber);
		return matcher.matches();
	}

	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		Optional<User> user;

		if (isValidEmail(identifier)) {
			user = userRepository.findByEmailIdAndStatus(identifier);
		} else if (isValidMobileNumber(identifier)) {
			user = userRepository.findByMobileNumberAndStatus(identifier);
		} else {
			System.err.println("UserName password Login");
			user = userRepository.findByUserNameAndStatus(identifier);
		}

		return user.map(UserInfoUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
	}
}