package com.finex.auth.config;

import cn.hutool.core.util.StrUtil;
import com.finex.common.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

@Slf4j
@Configuration
public class JwtSecretInitializer {

    public JwtSecretInitializer(@Value("${finex.security.jwt-secret:}") String jwtSecret) {
        if (StrUtil.isBlank(jwtSecret)) {
            return;
        }

        System.setProperty("finex.jwt.secret", jwtSecret);
        configureJwtUtilIfSupported(jwtSecret);
    }

    private void configureJwtUtilIfSupported(String jwtSecret) {
        try {
            Method method = JwtUtil.class.getMethod("configureSecret", String.class);
            method.invoke(null, jwtSecret);
        } catch (NoSuchMethodException ex) {
            log.warn("Loaded JwtUtil does not expose configureSecret(String). Falling back to system property only.");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize JWT secret", ex);
        }
    }
}
