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

    protected FinanceAccountSetMapper financeAccountSetMapper() {
        return financeAccountSetMapper;
    }

    protected FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper() {
        return financeAccountSetCodeRuleMapper;
    }

    protected FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper() {
        return financeAccountSetTemplateMapper;
    }

    protected FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper() {
        return financeAccountSetTemplateSubjectMapper;
    }

    protected SystemCompanyMapper systemCompanyMapper() {
        return systemCompanyMapper;
    }

    protected UserMapper userMapper() {
        return userMapper;
    }

    protected AsyncTaskRecordMapper asyncTaskRecordMapper() {
        return asyncTaskRecordMapper;
    }

    protected FinanceAccountSetTaskWorker financeAccountSetTaskWorker() {
        return financeAccountSetTaskWorker;
    }

    protected ObjectMapper objectMapper() {
        return objectMapper;
    }

    protected List<FinanceAccountSet> loadAccountSets() {
        return financeAccountSetMapper.selectList(
                Wrappers.<FinanceAccountSet>lambdaQuery()
                        .orderByDesc(FinanceAccountSet::getUpdatedAt, FinanceAccountSet::getCreatedAt)
        );
    }

    protected Map<String, SystemCompany> loadCompanyMap() {
        return systemCompanyMapper.selectList(Wrappers.<SystemCompany>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(SystemCompany::getCompanyId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    protected Map<String, FinanceAccountSetTemplate> loadTemplateMap() {
        return financeAccountSetTemplateMapper.selectList(Wrappers.<FinanceAccountSetTemplate>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(FinanceAccountSetTemplate::getTemplateCode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    protected Map<Long, User> loadUserMap() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    protected Map<String, FinanceAccountSetCodeRule> loadCodeRuleMap() {
        return financeAccountSetCodeRuleMapper.selectList(
                        Wrappers.<FinanceAccountSetCodeRule>lambdaQuery()
                                .eq(FinanceAccountSetCodeRule::getRuleType, FinanceAccountSetTaskWorker.RULE_TYPE_ACCOUNT_SUBJECT)
                ).stream()
                .collect(Collectors.toMap(FinanceAccountSetCodeRule::getCompanyId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    protected Map<String, AsyncTaskRecord> loadLatestTaskMap(List<String> taskNos) {
        if (taskNos == null || taskNos.isEmpty()) {
            return Map.of();
        }
        return asyncTaskRecordMapper.selectList(
                        Wrappers.<AsyncTaskRecord>lambdaQuery().in(AsyncTaskRecord::getTaskNo, taskNos)
                ).stream()
                .collect(Collectors.toMap(AsyncTaskRecord::getTaskNo, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    protected FinanceContextCompanyOptionVO toCompanyOption(SystemCompany company) {
        FinanceContextCompanyOptionVO option = new FinanceContextCompanyOptionVO();
        option.setCompanyId(company.getCompanyId());
        option.setCompanyCode(company.getCompanyCode());
        option.setCompanyName(company.getCompanyName());
        option.setValue(company.getCompanyId());
        option.setLabel(firstNonBlank(trimToNull(company.getCompanyCode()), company.getCompanyId()) + " - " + company.getCompanyName());
        return option;
    }

    protected String resolveSubjectCodeScheme(FinanceAccountSet accountSet, Map<String, FinanceAccountSetCodeRule> codeRuleMap) {
        String fromAccountSet = trimToNull(accountSet.getSubjectCodeScheme());
        if (fromAccountSet != null) {
            return fromAccountSet;
        }
        FinanceAccountSetCodeRule rule = codeRuleMap.get(accountSet.getCompanyId());
        return rule == null ? DEFAULT_SUBJECT_SCHEME : firstNonBlank(trimToNull(rule.getScheme()), DEFAULT_SUBJECT_SCHEME);
    }

    protected String resolveAccountSetStatusLabel(String status) {
        return switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "已启用";
            case "INITIALIZING" -> "创建中";
            case "FAILED" -> "创建失败";
            default -> "未创建";
        };
    }

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

    protected SystemCompany requireEnabledCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null || !Integer.valueOf(1).equals(company.getStatus())) {
            throw new IllegalStateException("目标公司不存在或未启用");
        }
        return company;
    }

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

    protected String requireEnabledYearMonth(String enabledYearMonth) {
        String normalized = requireText(enabledYearMonth, "启用年月不能为空");
        try {
            YearMonth.parse(normalized);
            return normalized;
        } catch (Exception ex) {
            throw new IllegalArgumentException("启用年月格式必须为 YYYY-MM");
        }
    }

    protected String normalizeCreateMode(String createMode) {
        String normalized = requireText(createMode, "创建方式不能为空").toUpperCase(Locale.ROOT);
        if (!Set.of(CREATE_MODE_BLANK, CREATE_MODE_REFERENCE).contains(normalized)) {
            throw new IllegalArgumentException("不支持的创建方式");
        }
        return normalized;
    }

    protected String normalizeScheme(String scheme) {
        return trimToNull(scheme) == null ? DEFAULT_SUBJECT_SCHEME : scheme.trim();
    }

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

    protected String writePayload(FinanceAccountSetTaskPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("任务参数序列化失败", ex);
        }
    }

    protected String formatYearMonth(Integer enabledYear, Integer enabledPeriod) {
        if (enabledYear == null || enabledPeriod == null) {
            return null;
        }
        return String.format(Locale.ROOT, "%04d-%02d", enabledYear, enabledPeriod);
    }

    protected String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    protected String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    protected String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
