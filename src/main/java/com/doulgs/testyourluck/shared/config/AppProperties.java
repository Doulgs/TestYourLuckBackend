package com.doulgs.testyourluck.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Security security) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Security(Jwt jwt) {
    }

    public record Jwt(String secret, long expirationMinutes) {
    }
}
