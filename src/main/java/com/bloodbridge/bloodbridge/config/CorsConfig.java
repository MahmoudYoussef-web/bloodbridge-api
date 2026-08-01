package com.bloodbridge.bloodbridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig {

    @Value("${bloodbridge.cors.allowed-origins:*}")
    private String allowedOrigins;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }
}
