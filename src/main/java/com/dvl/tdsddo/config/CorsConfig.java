package com.dvl.tdsddo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOriginPatterns("https://*.netlify.app", "https://app.dravinlabs.com",
								"https://ewingstds.com", "https://*.dravinlabs.com", // if applicable via Cloudflare
								"http://localhost:3000", "http://13.126.232.163:8888", "https://13.126.232.163:8443")
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
						.allowedHeaders("Authorization", "Content-Type", "Accept").exposedHeaders("Authorization")
						.allowCredentials(true);
			}
		};
	}
}
