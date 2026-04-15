// 业务域：首页看板与当前用户
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 MvpController 和首页页面请求，下游会继续协调 用户信息、待办汇总和发票等首页数据。
// 风险提醒：改坏后最容易影响 首页统计、个人信息与待办展示。

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

/**
 * AbstractMvpDomainSupport：领域规则支撑类。
 * 承接 首页看板与当前用户的核心业务规则。
 * 改这里时，要特别关注 首页统计、个人信息与待办展示是否会被一起带坏。
 */
public abstract class AbstractMvpDomainSupport {

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserService userService;
    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final ExpenseDocumentService expenseDocumentService;

    /**
     * 初始化这个类所需的依赖组件。
     */
    protected AbstractMvpDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService
    ) {
        this.userService = userService;
        this.asyncTaskRecordMapper = asyncTaskRecordMapper;
        this.expenseDocumentService = expenseDocumentService;
    }

    /**
     * 处理当前业务中的这一步。
     */
    protected UserService userService() {
        return userService;
    }

    /**
     * 处理当前业务中的这一步。
     */
    protected AsyncTaskRecordMapper asyncTaskRecordMapper() {
        return asyncTaskRecordMapper;
    }

    /**
     * 处理当前业务中的这一步。
     */
    protected ExpenseDocumentService expenseDocumentService() {
        return expenseDocumentService;
    }

    /**
     * 处理当前业务中的这一步。
     */
    protected User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Current user does not exist");
        }
        return user;
    }

    /**
     * 获取DisplayName。
     */
    protected String getDisplayName(User user) {
        return StrUtil.blankToDefault(user.getName(), user.getUsername());
    }

    /**
     * 处理当前业务中的这一步。
     */
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
