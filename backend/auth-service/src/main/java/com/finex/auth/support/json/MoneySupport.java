package com.finex.auth.support.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public final class MoneySupport {

    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

    private MoneySupport() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? ZERO : normalize(value);
    }

    public static BigDecimal parseInput(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.contains("e") || normalized.contains("E")) {
            throw new IllegalArgumentException("金额不支持科学计数法");
        }
        try {
            BigDecimal decimal = new BigDecimal(normalized);
            if (decimal.stripTrailingZeros().scale() > SCALE) {
                throw new IllegalArgumentException("金额最多保留两位小数");
            }
            return normalize(decimal);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("金额格式不正确");
        }
    }

    public static BigDecimal toBigDecimal(Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(value.longValue());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
