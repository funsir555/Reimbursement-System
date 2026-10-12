package com.finex.auth.service.impl.settings;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owns the ambiguity rules for matching an external employee to an existing user. */
final class EmployeeSyncMatchPolicy {

    private EmployeeSyncMatchPolicy() {
    }

    static MatchResult match(Map<String, List<User>> phoneMap, Map<String, List<User>> emailMap,
                             String phoneKey, String emailKey) {
        List<User> phoneMatches = StrUtil.isBlank(phoneKey) ? List.of() : phoneMap.getOrDefault(phoneKey, List.of());
        List<User> emailMatches = StrUtil.isBlank(emailKey) ? List.of() : emailMap.getOrDefault(emailKey, List.of());
        if (phoneMatches.size() > 1) {
            return MatchResult.conflict("\u624b\u673a\u53f7\u5339\u914d\u5230\u591a\u540d\u5458\u5de5\uff0c\u8bf7\u5148\u6e05\u7406\u91cd\u590d\u624b\u673a\u53f7\u540e\u518d\u540c\u6b65");
        }
        if (emailMatches.size() > 1) {
            return MatchResult.conflict("\u90ae\u7bb1\u5339\u914d\u5230\u591a\u540d\u5458\u5de5\uff0c\u8bf7\u5148\u6e05\u7406\u91cd\u590d\u90ae\u7bb1\u540e\u518d\u540c\u6b65");
        }
        User phoneUser = phoneMatches.isEmpty() ? null : phoneMatches.get(0);
        User emailUser = emailMatches.isEmpty() ? null : emailMatches.get(0);
        if (phoneUser != null && emailUser != null && !Objects.equals(phoneUser.getId(), emailUser.getId())) {
            return MatchResult.conflict("\u624b\u673a\u53f7\u4e0e\u90ae\u7bb1\u5339\u914d\u5230\u4e0d\u540c\u5458\u5de5\uff0c\u8bf7\u5148\u5904\u7406\u5458\u5de5\u552f\u4e00\u6027\u540e\u518d\u540c\u6b65");
        }
        return new MatchResult(phoneUser != null ? phoneUser : emailUser, null);
    }

    record MatchResult(User user, String conflictMessage) {
        static MatchResult conflict(String message) {
            return new MatchResult(null, message);
        }
    }
}
