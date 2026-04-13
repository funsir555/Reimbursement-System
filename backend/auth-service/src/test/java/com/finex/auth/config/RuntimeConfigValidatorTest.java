package com.finex.auth.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeConfigValidatorTest {

    @Test
    void rejectsMissingDatabasePassword() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeConfigValidator.validateDbPassword(" ")
        );

        assertEquals(
                "FINEX_DB_PASSWORD is required. auth-service will auto-load backend/.env.local.cmd for local runs, so verify that file or the process environment before starting auth-service.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsExampleDatabasePassword() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeConfigValidator.validateDbPassword("replace-with-your-db-password")
        );

        assertEquals(
                "FINEX_DB_PASSWORD still uses the example placeholder. Replace it with the real database password before starting auth-service.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsMissingJwtSecret() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeConfigValidator.validateJwtSecret(null)
        );

        assertEquals(
                "FINEX_JWT_SECRET is required. auth-service will auto-load backend/.env.local.cmd for local runs, so verify that file or inject the secret from the environment.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsShortJwtSecret() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeConfigValidator.validateJwtSecret("too-short-secret")
        );

        assertEquals("FINEX_JWT_SECRET must be at least 32 characters long.", exception.getMessage());
    }

    @Test
    void acceptsConfiguredSecrets() {
        assertDoesNotThrow(() -> new RuntimeConfigValidator(
                "db-password-for-local-dev",
                "local-dev-secret-value-at-least-32"
        ));
    }
}
