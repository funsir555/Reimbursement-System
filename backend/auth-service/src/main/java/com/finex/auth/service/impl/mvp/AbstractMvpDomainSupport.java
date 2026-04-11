package com.finex.auth.service.impl.mvp;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMvpDomainSupport {

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserService userService;
    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final ExpenseDocumentService expenseDocumentService;

    protected AbstractMvpDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService
    ) {
        this.userService = userService;
        this.asyncTaskRecordMapper = asyncTaskRecordMapper;
        this.expenseDocumentService = expenseDocumentService;
    }

    protected UserService userService() {
        return userService;
    }

    protected AsyncTaskRecordMapper asyncTaskRecordMapper() {
        return asyncTaskRecordMapper;
    }

    protected ExpenseDocumentService expenseDocumentService() {
        return expenseDocumentService;
    }

    protected User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Current user does not exist");
        }
        return user;
    }

    protected String getDisplayName(User user) {
        return StrUtil.blankToDefault(user.getName(), user.getUsername());
    }

    protected Map<String, AsyncTaskRecord> latestTaskMap(Long userId, String taskType) {
        List<AsyncTaskRecord> records = asyncTaskRecordMapper.selectList(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getUserId, userId)
                        .eq(AsyncTaskRecord::getTaskType, taskType)
                        .orderByDesc(AsyncTaskRecord::getCreatedAt, AsyncTaskRecord::getId)
        );

        Map<String, AsyncTaskRecord> latestMap = new LinkedHashMap<>();
        for (AsyncTaskRecord record : records) {
            if (record.getBusinessKey() != null && !latestMap.containsKey(record.getBusinessKey())) {
                latestMap.put(record.getBusinessKey(), record);
            }
        }
        return latestMap;
    }
}
