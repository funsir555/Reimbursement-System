package com.finex.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

// 这里封装 JwtUtil 的 token 生成、校验和解析能力。
// 登录成功后，很多请求会依赖它来识别当前用户。
// 如果改错，最容易影响登录态、鉴权和当前用户解析。

/**
 * 这是 JwtUtil 工具类。
 * 它负责 token 的生成、校验、解码和密钥选择。
 * 登录相关的接口通常会间接依赖这里。
 */
@Slf4j
public class JwtUtil {

    private static final String SECRET_ENV = "FINEX_JWT_SECRET";

    private static final String SECRET_PROPERTY = "finex.jwt.secret";

    private static final String ISSUER = "finex";

    private static final long EXPIRE_DAYS = 7;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile String configuredSecret;

    private static volatile String generatedFallbackSecret;

    private static volatile boolean fallbackSecretLogged;

    private JwtUtil() {
    }

    /**
     * 处理 configureSecret 请求。
     */
    public static void configureSecret(String secret) {
        if (StrUtil.isNotBlank(secret)) {
            configuredSecret = secret.trim();
        }
    }

    /**
     * 处理 generateToken 请求。
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expireTime = DateUtil.offsetDay(now, (int) EXPIRE_DAYS);
        String secret = resolveSecret();

        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expireTime)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 处理 verify 请求。
     */
    public static boolean verify(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(resolveSecret()))
                    .withIssuer(ISSUER)
                    .build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("Token verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 处理 decode 请求。
     */
    public static DecodedJWT decode(String token) {
        return JWT.decode(token);
    }

    /**
     * 处理 getUserId 请求。
     */
    public static Long getUserId(String token) {
        DecodedJWT jwt = decode(token);
        return jwt.getClaim("userId").asLong();
    }

    /**
     * 处理 getUsername 请求。
     */
    public static String getUsername(String token) {
        DecodedJWT jwt = decode(token);
        return jwt.getClaim("username").asString();
    }

    /**
     * 处理 isExpired 请求。
     */
    public static boolean isExpired(String token) {
        DecodedJWT jwt = decode(token);
        return jwt.getExpiresAt().before(new Date());
    }

    /**
     * 处理 resolveSecret 请求。
     */
    private static String resolveSecret() {
        String secret = firstNonBlank(
                configuredSecret,
                System.getProperty(SECRET_PROPERTY),
                System.getenv(SECRET_ENV)
        );
        if (StrUtil.isNotBlank(secret)) {
            return secret;
        }

        if (generatedFallbackSecret == null) {
            synchronized (JwtUtil.class) {
                if (generatedFallbackSecret == null) {
                    generatedFallbackSecret = createEphemeralSecret();
                }
            }
        }

        if (!fallbackSecretLogged) {
            synchronized (JwtUtil.class) {
                if (!fallbackSecretLogged) {
                    log.warn("JWT secret is not configured. Using an ephemeral in-memory secret for this process only.");
                    fallbackSecretLogged = true;
                }
            }
        }

        return generatedFallbackSecret;
    }

    /**
     * 处理 firstNonBlank 请求。
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 处理 createEphemeralSecret 请求。
     */
    private static String createEphemeralSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
