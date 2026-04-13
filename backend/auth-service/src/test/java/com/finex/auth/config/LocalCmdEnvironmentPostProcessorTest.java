package com.finex.auth.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalCmdEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsFinexVariablesFromRepoRootWorkingDirectory() throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Path backendDir = repoRoot.resolve("backend");
        Files.createDirectories(backendDir);
        Files.writeString(
                backendDir.resolve(".env.local.cmd"),
                "# comment\nset FINEX_DB_PASSWORD=local-db-pass\nset FINEX_JWT_SECRET=local-jwt-secret-value-at-least-32\n",
                java.nio.charset.StandardCharsets.UTF_8
        );

        ConfigurableEnvironment environment = new StandardEnvironment();

        new LocalCmdEnvironmentPostProcessor(repoRoot)
                .postProcessEnvironment(environment, new SpringApplication());

        assertEquals("local-db-pass", environment.getProperty("FINEX_DB_PASSWORD"));
        assertEquals("local-jwt-secret-value-at-least-32", environment.getProperty("FINEX_JWT_SECRET"));
        assertNotNull(environment.getPropertySources().get(LocalCmdEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
    }

    @Test
    void loadsFinexVariablesFromAuthServiceWorkingDirectoryWithoutOverridingHigherPriorityValues() throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Path authServiceDir = repoRoot.resolve("backend").resolve("auth-service");
        Files.createDirectories(authServiceDir);
        Files.writeString(
                repoRoot.resolve("backend").resolve(".env.local.cmd"),
                "set FINEX_DB_PASSWORD=local-db-pass\nset FINEX_JWT_SECRET=\"local-jwt-secret-value-at-least-32\"\nset IGNORED_KEY=value\n",
                java.nio.charset.StandardCharsets.UTF_8
        );

        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("testOverrides", Map.of("FINEX_DB_PASSWORD", "shell-db-pass"))
        );

        new LocalCmdEnvironmentPostProcessor(authServiceDir)
                .postProcessEnvironment(environment, new SpringApplication());

        assertEquals("shell-db-pass", environment.getProperty("FINEX_DB_PASSWORD"));
        assertEquals("local-jwt-secret-value-at-least-32", environment.getProperty("FINEX_JWT_SECRET"));
        assertNull(environment.getProperty("IGNORED_KEY"));
    }

    @Test
    void skipsMissingEnvFile() {
        ConfigurableEnvironment environment = new StandardEnvironment();

        new LocalCmdEnvironmentPostProcessor(tempDir)
                .postProcessEnvironment(environment, new SpringApplication());

        assertFalse(environment.getPropertySources().contains(LocalCmdEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
    }

    @Test
    void allowsRuntimeValidatorToPassAfterLocalEnvPreload() throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Path backendDir = repoRoot.resolve("backend");
        Files.createDirectories(backendDir);
        Files.writeString(
                backendDir.resolve(".env.local.cmd"),
                "set FINEX_DB_PASSWORD=local-db-pass\nset FINEX_JWT_SECRET=local-jwt-secret-value-at-least-32\n",
                java.nio.charset.StandardCharsets.UTF_8
        );

        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withInitializer(context -> new LocalCmdEnvironmentPostProcessor(repoRoot)
                        .postProcessEnvironment(context.getEnvironment(), new SpringApplication()))
                .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(
                        new MapPropertySource(
                                "testApplication",
                                Map.of(
                                        "spring.datasource.password", "${FINEX_DB_PASSWORD:}",
                                        "finex.security.jwt-secret", "${FINEX_JWT_SECRET:}"
                                )
                        )
                ))
                .withUserConfiguration(RuntimeConfigValidator.class);

        contextRunner.run(context -> assertNull(context.getStartupFailure()));
    }
}
