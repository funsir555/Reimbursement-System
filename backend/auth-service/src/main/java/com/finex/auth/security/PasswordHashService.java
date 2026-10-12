package com.finex.auth.security;

import cn.hutool.crypto.digest.DigestUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password hashing boundary. New values use bcrypt while legacy 32-character MD5 values remain readable
 * during the gradual migration period.
 */
@Component
public final class PasswordHashService {

    public static final int BCRYPT_COST = 12;

    private static final Pattern LEGACY_MD5 = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(BCRYPT_COST);

    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        return bcrypt.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        try {
            if (isBcrypt(storedHash)) {
                return bcrypt.matches(rawPassword, storedHash);
            }
            if (isLegacyMd5(storedHash)) {
                byte[] expected = storedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII);
                byte[] actual = DigestUtil.md5Hex(rawPassword).getBytes(StandardCharsets.US_ASCII);
                return MessageDigest.isEqual(expected, actual);
            }
            return false;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isLegacyMd5(String storedHash) {
        return storedHash != null && LEGACY_MD5.matcher(storedHash).matches();
    }

    public boolean isBcrypt(String storedHash) {
        return storedHash != null && BCRYPT.matcher(storedHash).matches();
    }
}
