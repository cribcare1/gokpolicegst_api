package com.dvl.tdsddo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class EncryptionConfig {
    @Value("${encryption.secret.key}")
    private String secretKey;

}
