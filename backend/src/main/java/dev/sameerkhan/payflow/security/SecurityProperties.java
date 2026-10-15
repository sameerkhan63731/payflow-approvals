package dev.sameerkhan.payflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecret,
        Duration tokenTtl,
        List<String> allowedOrigins,
        String demoPassword
) {
}
