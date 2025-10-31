package com.dvl.tdsddo.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	UserDetailsService userDetailsService;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors().and() // ✅ Apply CORS configuration
				.csrf().disable().authorizeHttpRequests()
				.requestMatchers("/tds/auth/**", "/tds/form16/downloadPDF/*", "/tds/form16/downloadPDFFinancialYear/*",
						"/cribCare/api/user/checkTheUserNameAvailability",
						"/cribCare/api/user/sentAdminRegistrationRequest",
						"/cribCare/api/user/findAllPendingApprovalList", "/cribCare/api/user/registerAdminToUserTable",
						"/cribCare/api/section/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.permitAll().anyRequest().authenticated().and().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		// ✅ Ensure JWT filter is applied after CORS and before authentication
		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);

		// Allow all origins or specify allowed origins (frontend URLs)
//		config.setAllowedOrigins(
//				List.of("http://localhost:3000", "http://13.126.232.163:8888", "https://13.126.232.163:8443", "https://app.dravinlabs.com:8443"));
		config.setAllowedOriginPatterns(List.of("https://*.netlify.app", "https://app.dravinlabs.com",
				"https://ewingstds.com", "https://*.dravinlabs.com", "http://localhost:3000",
				"http://13.126.232.163:8888", "https://13.126.232.163:8443"));
//		config.setAllowedOriginPatterns(List.of("*"));  // Allows all origins and ports

		// Allow all headers, including Authorization
		config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "Accept"));

		// Allow necessary HTTP methods
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

		// Allow Authorization headers
		config.setExposedHeaders(List.of("Authorization"));

		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // Use BCrypt password encoder
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService); // Set user details service
		authenticationProvider.setPasswordEncoder(passwordEncoder()); // Set password encoder
		return authenticationProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager(); // Configure the authentication manager
	}
}
