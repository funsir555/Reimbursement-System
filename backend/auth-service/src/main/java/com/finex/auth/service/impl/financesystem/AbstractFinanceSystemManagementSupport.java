// 业务域：财务系统管理
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 财务系统设置和账套相关接口，下游会继续协调 账套、同步任务和财务上下文基础数据。
// 风险提醒：改坏后最容易影响 账套切换、基础数据同步和下游系统连接。

package com.finex.auth.service.impl.financesystem;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetTaskPayload;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetCodeRule;
import com.finex.auth.entity.FinanceAccountSetTemplate;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.impl.FinanceAccountSetTaskWorker;
import com.finex.auth.support.AsyncTaskSupport;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AbstractFinanceSystemManagementSupport：通用支撑类。
 * 封装 财务系统管理这块可复用的业务能力。
 * 改这里时，要特别关注 账套切换、基础数据同步和下游系统连接是否会被一起带坏。
 */
public abstract class AbstractFinanceSystemManagementSupport {

    protected static final String DEFAULT_SUBJECT_SCHEME = "4-2-2-2";
    protected static final String CREATE_MODE_BLANK = "BLANK";
    protected static final String CREATE_MODE_REFERENCE = "REFERENCE";
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper;
    private final FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper;
    private final FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final UserMapper userMapper;
    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final FinanceAccountSetTaskWorker financeAccountSetTaskWorker;
    private final ObjectMapper objectMapper;

    /**
     * 初始化这个类所需的依赖组件。
     */
    protected AbstractFinanceSystemManagementSupport(
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper,
            FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper,
            FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            FinanceAccountSetTaskWorker financeAccountSetTaskWorker,
            ObjectMapper objectMapper
    ) {
        this.financeAccountSetMapper = financeAccountSetMapper;
        this.financeAccountSetCodeRuleMapper = financeAccountSetCodeRuleMapper;
        this.financeAccountSetTemplateMapper = financeAccountSetTemplateMapper;
        this.financeAccountSetTemplateSubjectMapper = financeAccountSetTemplateSubjectMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.userMapper = userMapper;
        this.asyncTaskRecordMapper = asyncTaskRecordMapper;
        this.financeAccountSetTaskWorker = financeAccountSetTaskWorker;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected FinanceAccountSetMapper financeAccountSetMapper() {
        return financeAccountSetMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper() {
        return financeAccountSetCodeRuleMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper() {
        return financeAccountSetTemplateMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper() {
        return financeAccountSetTemplateSubjectMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected SystemCompanyMapper systemCompanyMapper() {
        return systemCompanyMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected UserMapper userMapper() {
        return userMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected AsyncTaskRecordMapper asyncTaskRecordMapper() {
        return asyncTaskRecordMapper;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected FinanceAccountSetTaskWorker financeAccountSetTaskWorker() {
        return financeAccountSetTaskWorker;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * 加载账户Sets。
     */
    protected List<FinanceAccountSet> loadAccountSets() {
        return financeAccountSetMapper.selectList(
                Wrappers.<FinanceAccountSet>lambdaQuery()
                        .orderByDesc(FinanceAccountSet::getUpdatedAt, FinanceAccountSet::getCreatedAt)
        );
    }

    /**
     * 加载公司映射。
     */
    protected Map<String, SystemCompany> loadCompanyMap() {
        return systemCompanyMapper.selectList(Wrappers.<SystemCompany>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(SystemCompany::getCompanyId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 加载模板映射。
     */
    protected Map<String, FinanceAccountSetTemplate> loadTemplateMap() {
        return financeAccountSetTemplateMapper.selectList(Wrappers.<FinanceAccountSetTemplate>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(FinanceAccountSetTemplate::getTemplateCode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 加载用户映射。
     */
    protected Map<Long, User> loadUserMap() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 加载编码Rule映射。
     */
    protected Map<String, FinanceAccountSetCodeRule> loadCodeRuleMap() {
        return financeAccountSetCodeRuleMapper.selectList(
                        Wrappers.<FinanceAccountSetCodeRule>lambdaQuery()
                                .eq(FinanceAccountSetCodeRule::getRuleType, FinanceAccountSetTaskWorker.RULE_TYPE_ACCOUNT_SUBJECT)
                ).stream()
                .collect(Collectors.toMap(FinanceAccountSetCodeRule::getCompanyId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 加载Latest任务映射。
     */
    protected Map<String, AsyncTaskRecord> loadLatestTaskMap(List<String> taskNos) {
        if (taskNos == null || taskNos.isEmpty()) {
            return Map.of();
        }
        return asyncTaskRecordMapper.selectList(
                        Wrappers.<AsyncTaskRecord>lambdaQuery().in(AsyncTaskRecord::getTaskNo, taskNos)
                ).stream()
                .collect(Collectors.toMap(AsyncTaskRecord::getTaskNo, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected FinanceContextCompanyOptionVO toCompanyOption(SystemCompany company) {
        FinanceContextCompanyOptionVO option = new FinanceContextCompanyOptionVO();
        option.setCompanyId(company.getCompanyId());
        option.setCompanyCode(company.getCompanyCode());
        option.setCompanyName(company.getCompanyName());
        option.setValue(company.getCompanyId());
        option.setLabel(firstNonBlank(trimToNull(company.getCompanyCode()), company.getCompanyId()) + " - " + company.getCompanyName());
        return option;
    }

    /**
     * 解析科目编码Scheme。
     */
    protected String resolveSubjectCodeScheme(FinanceAccountSet accountSet, Map<String, FinanceAccountSetCodeRule> codeRuleMap) {
        String fromAccountSet = trimToNull(accountSet.getSubjectCodeScheme());
        if (fromAccountSet != null) {
            return fromAccountSet;
        }
        FinanceAccountSetCodeRule rule = codeRuleMap.get(accountSet.getCompanyId());
        return rule == null ? DEFAULT_SUBJECT_SCHEME : firstNonBlank(trimToNull(rule.getScheme()), DEFAULT_SUBJECT_SCHEME);
    }

    /**
     * 解析账户SetStatusLabel。
     */
    protected String resolveAccountSetStatusLabel(String status) {
        return switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "已启用";
            case "INITIALIZING" -> "创建中";
            case "FAILED" -> "创建失败";
            default -> "未创建";
        };
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected FinanceAccountSetTaskStatusVO toTaskStatus(AsyncTaskRecord task, String accountSetStatus) {
        FinanceAccountSetTaskStatusVO status = new FinanceAccountSetTaskStatusVO();
        status.setTaskNo(task.getTaskNo());
        status.setCompanyId(task.getCompanyId());
        status.setTaskType(task.getTaskType());
        status.setStatus(task.getStatus());
        status.setProgress(task.getProgress());
        status.setResultMessage(task.getResultMessage());
        status.setAccountSetStatus(accountSetStatus);
        status.setFinished(
                AsyncTaskSupport.TASK_STATUS_SUCCESS.equalsIgnoreCase(task.getStatus())
                        || AsyncTaskSupport.TASK_STATUS_FAILED.equalsIgnoreCase(task.getStatus())
        );
        status.setCreatedAt(formatDateTime(task.getCreatedAt()));
        status.setUpdatedAt(formatDateTime(task.getUpdatedAt()));
        status.setFinishedAt(formatDateTime(task.getFinishedAt()));
        return status;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected SystemCompany requireEnabledCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null || !Integer.valueOf(1).equals(company.getStatus())) {
            throw new IllegalStateException("目标公司不存在或未启用");
        }
        return company;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected User requireSupervisor(Long supervisorUserId) {
        if (supervisorUserId == null) {
            throw new IllegalArgumentException("账套主管不能为空");
        }
        User user = userMapper.selectById(supervisorUserId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalStateException("账套主管不存在或未启用");
        }
        return user;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String requireEnabledYearMonth(String enabledYearMonth) {
        String normalized = requireText(enabledYearMonth, "启用年月不能为空");
        try {
            YearMonth.parse(normalized);
            return normalized;
        } catch (Exception ex) {
            throw new IllegalArgumentException("启用年月格式必须为 YYYY-MM");
        }
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String normalizeCreateMode(String createMode) {
        String normalized = requireText(createMode, "创建方式不能为空").toUpperCase(Locale.ROOT);
        if (!Set.of(CREATE_MODE_BLANK, CREATE_MODE_REFERENCE).contains(normalized)) {
            throw new IllegalArgumentException("不支持的创建方式");
        }
        return normalized;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String normalizeScheme(String scheme) {
        return trimToNull(scheme) == null ? DEFAULT_SUBJECT_SCHEME : scheme.trim();
    }

    /**
     * 校验Scheme。
     */
    protected void validateScheme(String scheme) {
        String normalized = normalizeScheme(scheme);
        String[] parts = normalized.split("-");
        if (parts.length == 0 || !"4".equals(parts[0])) {
            throw new IllegalArgumentException("科目编码规则必须以 4 开头");
        }
        for (String part : parts) {
            try {
                int length = Integer.parseInt(part);
                if (length <= 0) {
                    throw new IllegalArgumentException("科目编码规则必须为正整数段长");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("科目编码规则格式不正确");
            }
        }
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String writePayload(FinanceAccountSetTaskPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("任务参数序列化失败", ex);
        }
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String formatYearMonth(Integer enabledYear, Integer enabledPeriod) {
        if (enabledYear == null || enabledPeriod == null) {
            return null;
        }
        return String.format(Locale.ROOT, "%04d-%02d", enabledYear, enabledPeriod);
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    /**
     * 处理财务系统管理中的这一步。
     */
    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
