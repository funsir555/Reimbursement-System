package com.finex.auth.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalCmdEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "finexLocalCmdEnv";

    private static final String ENV_FILE_NAME = ".env.local.cmd";

    private final Path workingDirectory;

    public LocalCmdEnvironmentPostProcessor() {
        this(resolveWorkingDirectory());
    }

    LocalCmdEnvironmentPostProcessor(Path workingDirectory) {
        this.workingDirectory = workingDirectory == null ? resolveWorkingDirectory() : workingDirectory;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = resolveEnvFile(workingDirectory);
        if (envFile == null) {
            return;
        }

        Map<String, Object> properties = loadProperties(envFile);
        if (properties.isEmpty()) {
            return;
        }

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    static Path resolveEnvFile(Path workingDirectory) {
        for (Path candidate : resolveCandidates(workingDirectory)) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static Map<String, Object> loadProperties(Path envFile) {
        if (envFile == null || !Files.isRegularFile(envFile)) {
            return Map.of();
        }

        try {
            return parseLines(Files.readAllLines(envFile, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read local startup env file: " + envFile, ex);
        }
    }

    static Map<String, Object> parseLines(List<String> lines) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String line : lines) {
            String normalized = normalizeLine(line);
            if (normalized.isEmpty()) {
                continue;
            }
            if (startsWithIgnoreCase(normalized, "#") || startsWithIgnoreCase(normalized, "rem")) {
                continue;
            }
            if (!startsWithIgnoreCase(normalized, "set ")) {
                continue;
            }

            String assignment = normalized.substring(4).trim();
            int separator = assignment.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String key = assignment.substring(0, separator).trim();
            if (!key.startsWith("FINEX_")) {
                continue;
            }

            String value = unquote(assignment.substring(separator + 1).trim());
            if (!value.isEmpty()) {
                properties.putIfAbsent(key, value);
            }
        }
        return properties;
    }

    private static List<Path> resolveCandidates(Path workingDirectory) {
        Path base = workingDirectory == null ? resolveWorkingDirectory() : workingDirectory.toAbsolutePath().normalize();
        List<Path> rawCandidates = new ArrayList<>();
        rawCandidates.add(base.resolve("backend").resolve(ENV_FILE_NAME));
        rawCandidates.add(base.resolve(ENV_FILE_NAME));

        Path parent = base.getParent();
        if (parent != null) {
            rawCandidates.add(parent.resolve(ENV_FILE_NAME));
            rawCandidates.add(parent.resolve("backend").resolve(ENV_FILE_NAME));
        }

        Path grandParent = parent == null ? null : parent.getParent();
        if (grandParent != null) {
            rawCandidates.add(grandParent.resolve("backend").resolve(ENV_FILE_NAME));
        }

        Set<Path> candidates = new LinkedHashSet<>();
        for (Path candidate : rawCandidates) {
            candidates.add(candidate.toAbsolutePath().normalize());
        }
        return new ArrayList<>(candidates);
    }

    private static Path resolveWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    private static String normalizeLine(String line) {
        return line == null ? "" : line.trim();
    }

    private static boolean startsWithIgnoreCase(String text, String prefix) {
        return text.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
