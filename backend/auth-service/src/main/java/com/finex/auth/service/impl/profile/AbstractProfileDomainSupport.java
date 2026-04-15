// 业务域：个人中心与下载
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 个人中心页面、下载中心接口，下游会继续协调 个人信息、银行卡账户和下载记录。
// 风险提醒：改坏后最容易影响 个人信息展示、银行卡维护和下载留痕。

package com.finex.auth.service.impl.profile;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.DownloadStorageService;

import java.time.format.DateTimeFormatter;

/**
 * AbstractProfileDomainSupport：领域规则支撑类。
 * 承接 个人中心的核心业务规则。
 * 改这里时，要特别关注 个人信息展示、银行卡维护和下载留痕是否会被一起带坏。
 */
public abstract class AbstractProfileDomainSupport {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final UserBankAccountMapper userBankAccountMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final DownloadStorageService downloadStorageService;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 处理个人中心中的这一步。
     */
    protected UserService userService() {
        return userService;
    }

    /**
     * 处理个人中心中的这一步。
     */
    protected UserBankAccountMapper userBankAccountMapper() {
        return userBankAccountMapper;
    }

    /**
     * 下载RecordMapper。
     */
    protected DownloadRecordMapper downloadRecordMapper() {
        return downloadRecordMapper;
    }

    /**
     * 下载StorageService。
     */
    protected DownloadStorageService downloadStorageService() {
        return downloadStorageService;
    }

    /**
     * 处理个人中心中的这一步。
     */
    protected User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    /**
     * 处理个人中心中的这一步。
     */
    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 处理个人中心中的这一步。
     */
    protected String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    /**
     * 处理个人中心中的这一步。
     */
    protected String defaultText(String value, String fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 处理个人中心中的这一步。
     */
    protected String blankToDefault(String value, String fallback) {
        return StrUtil.blankToDefault(value, fallback);
    }
}
