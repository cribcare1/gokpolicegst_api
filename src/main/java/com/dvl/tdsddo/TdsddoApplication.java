package com.dvl.tdsddo;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TdsddoApplication {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());

		SpringApplication app = new SpringApplication(TdsddoApplication.class);
		app.setAdditionalProfiles("default"); // <--- this is key
		app.run(args);
		System.err.println("TDS application");
	}
}



