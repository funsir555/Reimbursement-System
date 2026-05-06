package com.finex.auth.support;

public final class ExpenseAttachmentLimitSupport {

    private static final long DEFAULT_MAX_FILE_SIZE_MB = 1L;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private ExpenseAttachmentLimitSupport() {
    }

    public static long normalizeMaxFileSizeMb(long configuredMaxFileSizeMb) {
        return configuredMaxFileSizeMb > 0 ? configuredMaxFileSizeMb : DEFAULT_MAX_FILE_SIZE_MB;
    }

    public static long toMaxFileSizeBytes(long configuredMaxFileSizeMb) {
        return normalizeMaxFileSizeMb(configuredMaxFileSizeMb) * BYTES_PER_MB;
    }

    public static String buildSizeExceededMessage(long configuredMaxFileSizeMb) {
        return "文件大小超出 " + normalizeMaxFileSizeMb(configuredMaxFileSizeMb) + "MB";
    }
}
