package com.finex.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Set;

@Configuration
public class RuntimeConfigValidator {

    static final int MIN_JWT_SECRET_LENGTH = 32;

    private static final Set<String> DB_PASSWORD_PLACEHOLDERS = Set.of(
            "change-me",
            "replace-with-your-db-password"
    );

    private static final Set<String> JWT_SECRET_PLACEHOLDERS = Set.of(
            "replace-with-a-long-random-secret",
            "replace-with-a-long-random-secret-at-least-32-chars"
    );

    public RuntimeConfigValidator(
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${finex.security.jwt-secret:}") String jwtSecret
    ) {
        validateDbPassword(dbPassword);
        validateJwtSecret(jwtSecret);
    }

    static void validateDbPassword(String dbPassword) {
        String normalized = normalize(dbPassword);
        if (normalized.isEmpty()) {
            throw new IllegalStateException(
                    "FINEX_DB_PASSWORD is required. Set it in backend/.env.local.cmd or the process environment before starting auth-service."
            );
        }
        if (DB_PASSWORD_PLACEHOLDERS.contains(normalized.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "FINEX_DB_PASSWORD still uses the example placeholder. Replace it with the real database password before starting auth-service."
            );
        }
    }

    static void validateJwtSecret(String jwtSecret) {
        String normalized = normalize(jwtSecret);
        if (normalized.isEmpty()) {
            throw new IllegalStateException(
                    "FINEX_JWT_SECRET is required. Set a stable secret in backend/.env.local.cmd for development and inject it from the environment in production."
            );
        }
        if (JWT_SECRET_PLACEHOLDERS.contains(normalized.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "FINEX_JWT_SECRET still uses the example placeholder. Replace it with a real secret before starting auth-service."
            );
        }
        if (normalized.length() < MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "FINEX_JWT_SECRET must be at least 32 characters long."
            );
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
