package com.finex.auth.service.impl.profile;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.DownloadStorageService;

import java.time.format.DateTimeFormatter;

public abstract class AbstractProfileDomainSupport {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final UserBankAccountMapper userBankAccountMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final DownloadStorageService downloadStorageService;

    protected AbstractProfileDomainSupport(
            UserService userService,
            UserBankAccountMapper userBankAccountMapper,
            DownloadRecordMapper downloadRecordMapper,
            DownloadStorageService downloadStorageService
    ) {
        this.userService = userService;
        this.userBankAccountMapper = userBankAccountMapper;
        this.downloadRecordMapper = downloadRecordMapper;
        this.downloadStorageService = downloadStorageService;
    }

    protected UserService userService() {
        return userService;
    }

    protected UserBankAccountMapper userBankAccountMapper() {
        return userBankAccountMapper;
    }

    protected DownloadRecordMapper downloadRecordMapper() {
        return downloadRecordMapper;
    }

    protected DownloadStorageService downloadStorageService() {
        return downloadStorageService;
    }

    protected User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    protected String defaultText(String value, String fallback) {
        return value == null ? fallback : value;
    }

    protected String blankToDefault(String value, String fallback) {
        return StrUtil.blankToDefault(value, fallback);
    }
}
