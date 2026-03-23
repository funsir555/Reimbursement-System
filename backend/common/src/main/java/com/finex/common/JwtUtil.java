package com.finex.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * JWT工具类
 */
@Slf4j
public class JwtUtil {

    /**
     * 密钥
     */
    private static final String SECRET = "finex-secret-key-2025";

    /**
     * 签发者
     */
    private static final String ISSUER = "finex";

    /**
     * Token过期时间（7天）
     */
    private static final long EXPIRE_DAYS = 7;

    /**
     * 生成Token
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expireTime = DateUtil.offsetDay(now, (int) EXPIRE_DAYS);

        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expireTime)
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 验证Token
     */
    public static boolean verify(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET))
                    .withIssuer(ISSUER)
                    .build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析Token
     */
    public static DecodedJWT decode(String token) {
        return JWT.decode(token);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId(String token) {
        DecodedJWT jwt = decode(token);
        return jwt.getClaim("userId").asLong();
    }

    /**
     * 获取用户名
     */
    public static String getUsername(String token) {
        DecodedJWT jwt = decode(token);
        return jwt.getClaim("username").asString();
    }

    /**
     * Token是否过期
     */
    public static boolean isExpired(String token) {
        DecodedJWT jwt = decode(token);
        return jwt.getExpiresAt().before(new Date());
    }
}
