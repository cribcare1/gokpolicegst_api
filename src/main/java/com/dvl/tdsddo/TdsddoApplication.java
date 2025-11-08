package com.dvl.tdsddo;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TdsddoApplication {

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        SpringApplication app = new SpringApplication(TdsddoApplication.class);
        app.setAdditionalProfiles("default");
        app.run(args); // ✅ This must be active for Spring Boot to start

        System.out.println("🚀 TDS application started successfully!");
        System.out.println("Key from env: " + System.getenv("ENCRYPTION_SECRET_KEY"));

    }
}
