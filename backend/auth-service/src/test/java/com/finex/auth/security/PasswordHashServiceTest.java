package com.finex.auth.security;

import cn.hutool.crypto.digest.DigestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashServiceTest {

    private final PasswordHashService service = new PasswordHashService();

    @Test
    void verifiesLegacyMd5WithoutTreatingItAsANewHash() {
        String legacy = DigestUtil.md5Hex("secret");

        assertTrue(service.matches("secret", legacy));
        assertFalse(service.matches("wrong", legacy));
        assertTrue(service.isLegacyMd5(legacy));
        assertFalse(service.isBcrypt(legacy));
    }

    @Test
    void encodesNewPasswordsWithBcryptCostTwelve() {
        String encoded = service.encode("secret");

        assertTrue(service.isBcrypt(encoded));
        assertEquals("12", encoded.substring(4, 6));
        assertTrue(service.matches("secret", encoded));
        assertFalse(service.matches("wrong", encoded));
    }

    @Test
    void rejectsUnknownOrMalformedHashes() {
        assertFalse(service.matches("secret", null));
        assertFalse(service.matches("secret", "not-a-password-hash"));
        assertFalse(service.matches(null, DigestUtil.md5Hex("secret")));
    }
}
