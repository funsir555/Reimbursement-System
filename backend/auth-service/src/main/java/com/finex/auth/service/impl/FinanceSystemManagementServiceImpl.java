
package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetOptionVO;
import com.finex.auth.dto.FinanceAccountSetReferenceOptionVO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceAccountSetTaskPayload;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.dto.FinanceAccountSetTemplateSummaryVO;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetCodeRule;
import com.finex.auth.entity.FinanceAccountSetTemplate;
import com.finex.auth.entity.FinanceAccountSetTemplateSubject;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceSystemManagementService;
import com.finex.auth.support.AsyncTaskSupport;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinanceSystemManagementServiceImpl implements FinanceSystemManagementService {

    private static final String DEFAULT_SUBJECT_SCHEME = "4-2-2-2";
    private static final String CREATE_MODE_BLANK = "BLANK";
    private static final String CREATE_MODE_REFERENCE = "REFERENCE";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper;
    private final FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper;
    private final FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final UserMapper userMapper;
    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final FinanceAccountSetTaskWorker financeAccountSetTaskWorker;
    private final ObjectMapper objectMapper;

    @Override
    public FinanceAccountSetMetaVO getMeta() {
        FinanceAccountSetMetaVO meta = new FinanceAccountSetMetaVO();
        meta.setCompanyOptions(loadCompanyOptions());
        meta.setSupervisorOptions(loadSupervisorOptions());
        meta.setTemplateOptions(loadTemplateOptions());
        meta.setReferenceOptions(buildReferenceOptions(loadAccountSets(), loadCompanyMap(), loadTemplateMap(), loadCodeRuleMap()));
        meta.setDefaultSubjectCodeScheme(DEFAULT_SUBJECT_SCHEME);
        return meta;
    }

    @Override
    public List<FinanceAccountSetSummaryVO> listAccountSets() {
        List<FinanceAccountSet> accountSets = loadAccountSets();
        Map<String, SystemCompany> companyMap = loadCompanyMap();
        Map<String, FinanceAccountSetTemplate> templateMap = loadTemplateMap();
        Map<Long, User> userMap = loadUserMap();
        Map<String, FinanceAccountSetCodeRule> codeRuleMap = loadCodeRuleMap();
        Map<String, AsyncTaskRecord> taskMap = loadLatestTaskMap(
                accountSets.stream().map(FinanceAccountSet::getLastTaskNo).filter(Objects::nonNull).toList()
        );
        return accountSets.stream()
                .sorted(Comparator.comparing(FinanceAccountSet::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> toSummary(item, companyMap, templateMap, userMap, codeRuleMap, taskMap))
                .toList();
    }

    @Override
    public FinanceAccountSetTaskStatusVO submitCreateTask(Long currentUserId, FinanceAccountSetCreateDTO dto) {
        FinanceAccountSetTaskPayload payload = normalizePayload(dto);
        validateBeforeSubmit(payload);

        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo(AsyncTaskSupport.buildTaskNo(AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE));
        task.setUserId(currentUserId);
        task.setCompanyId(payload.getTargetCompanyId());
        task.setTaskType(AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE);
        task.setBusinessType(AsyncTaskSupport.BUSINESS_TYPE_FINANCE_ACCOUNT_SET);
        task.setBusinessKey(payload.getTargetCompanyId());
        task.setDisplayName("新建账套");
        task.setStatus(AsyncTaskSupport.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setResultMessage("账套创建任务已提交");
        task.setResultPayload(writePayload(payload));
        asyncTaskRecordMapper.insert(task);

        financeAccountSetTaskWorker.runCreateAccountSetTask(task.getId());
        return toTaskStatus(task, null);
    }

    @Override
    public FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo) {
        String normalizedTaskNo = trimToNull(taskNo);
        if (normalizedTaskNo == null) {
            throw new IllegalArgumentException("任务号不能为空");
        }
        AsyncTaskRecord task = asyncTaskRecordMapper.selectOne(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getTaskNo, normalizedTaskNo)
                        .last("limit 1")
        );
        if (task == null) {
            throw new IllegalStateException("账套任务不存在");
        }
        FinanceAccountSet accountSet = task.getCompanyId() == null ? null : financeAccountSetMapper.selectById(task.getCompanyId());
        return toTaskStatus(task, accountSet == null ? null : accountSet.getStatus());
    }

    private List<FinanceContextCompanyOptionVO> loadCompanyOptions() {
        return systemCompanyMapper.selectList(
                        Wrappers.<SystemCompany>lambdaQuery()
                                .eq(SystemCompany::getStatus, 1)
                                .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
                ).stream()
                .map(this::toCompanyOption)
                .toList();
    }

    private List<FinanceAccountSetOptionVO> loadSupervisorOptions() {
        return userMapper.selectList(
                        Wrappers.<User>lambdaQuery()
                                .eq(User::getStatus, 1)
                                .orderByAsc(User::getName, User::getUsername, User::getId)
                ).stream()
                .map(user -> {
                    FinanceAccountSetOptionVO option = new FinanceAccountSetOptionVO();
                    option.setValue(String.valueOf(user.getId()));
                    option.setLabel(firstNonBlank(trimToNull(user.getName()), trimToNull(user.getUsername()), String.valueOf(user.getId())));
                    return option;
                })
                .toList();
    }

    private List<FinanceAccountSetTemplateSummaryVO> loadTemplateOptions() {
        List<FinanceAccountSetTemplate> templates = financeAccountSetTemplateMapper.selectList(
                Wrappers.<FinanceAccountSetTemplate>lambdaQuery()
                        .eq(FinanceAccountSetTemplate::getStatus, 1)
                        .orderByAsc(FinanceAccountSetTemplate::getTemplateCode)
        );
        Map<String, List<FinanceAccountSetTemplateSubject>> subjectsByTemplate = financeAccountSetTemplateSubjectMapper.selectList(
                        Wrappers.<FinanceAccountSetTemplateSubject>lambdaQuery()
                                .eq(FinanceAccountSetTemplateSubject::getStatus, 1)
                                .orderByAsc(FinanceAccountSetTemplateSubject::getTemplateCode, FinanceAccountSetTemplateSubject::getSortOrder, FinanceAccountSetTemplateSubject::getId)
                ).stream()
                .collect(Collectors.groupingBy(FinanceAccountSetTemplateSubject::getTemplateCode, LinkedHashMap::new, Collectors.toList()));

        List<FinanceAccountSetTemplateSummaryVO> result = new ArrayList<>();
        for (FinanceAccountSetTemplate template : templates) {
            List<FinanceAccountSetTemplateSubject> subjects = subjectsByTemplate.getOrDefault(template.getTemplateCode(), List.of());
            FinanceAccountSetTemplateSummaryVO item = new FinanceAccountSetTemplateSummaryVO();
            item.setTemplateCode(template.getTemplateCode());
            item.setTemplateName(template.getTemplateName());
            item.setAccountingStandard(template.getAccountingStandard());
            item.setLevel1SubjectCount((int) subjects.stream().filter(subject -> Integer.valueOf(1).equals(subject.getSubjectLevel())).count());
            item.setCommonSubjectCount((int) subjects.stream().filter(subject -> !Integer.valueOf(1).equals(subject.getSubjectLevel())).count());
            result.add(item);
        }
        return result;
    }
    private List<FinanceAccountSetReferenceOptionVO> buildReferenceOptions(
            List<FinanceAccountSet> accountSets,
            Map<String, SystemCompany> companyMap,
            Map<String, FinanceAccountSetTemplate> templateMap,
            Map<String, FinanceAccountSetCodeRule> codeRuleMap
    ) {
        return accountSets.stream()
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .sorted(Comparator.comparing(FinanceAccountSet::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> {
                    SystemCompany company = companyMap.get(item.getCompanyId());
                    FinanceAccountSetTemplate template = templateMap.get(item.getTemplateCode());
                    FinanceAccountSetReferenceOptionVO option = new FinanceAccountSetReferenceOptionVO();
                    option.setCompanyId(item.getCompanyId());
                    option.setCompanyName(company == null ? item.getCompanyId() : company.getCompanyName());
                    option.setTemplateCode(item.getTemplateCode());
                    option.setTemplateName(template == null ? item.getTemplateCode() : template.getTemplateName());
                    option.setEnabledYearMonth(formatYearMonth(item.getEnabledYear(), item.getEnabledPeriod()));
                    option.setSubjectCodeScheme(resolveSubjectCodeScheme(item, codeRuleMap));
                    option.setLabel(firstNonBlank(option.getCompanyName(), item.getCompanyId())
                            + " / " + firstNonBlank(option.getTemplateName(), item.getTemplateCode())
                            + " / " + firstNonBlank(option.getEnabledYearMonth(), "未设置启用期间"));
                    return option;
                })
                .toList();
    }

    private FinanceAccountSetSummaryVO toSummary(
            FinanceAccountSet item,
            Map<String, SystemCompany> companyMap,
            Map<String, FinanceAccountSetTemplate> templateMap,
            Map<Long, User> userMap,
            Map<String, FinanceAccountSetCodeRule> codeRuleMap,
            Map<String, AsyncTaskRecord> taskMap
    ) {
        FinanceAccountSetSummaryVO summary = new FinanceAccountSetSummaryVO();
        SystemCompany company = companyMap.get(item.getCompanyId());
        SystemCompany referenceCompany = companyMap.get(item.getReferenceCompanyId());
        FinanceAccountSetTemplate template = templateMap.get(item.getTemplateCode());
        User supervisor = item.getSupervisorUserId() == null ? null : userMap.get(item.getSupervisorUserId());
        AsyncTaskRecord task = taskMap.get(item.getLastTaskNo());

        summary.setCompanyId(item.getCompanyId());
        summary.setCompanyCode(company == null ? null : trimToNull(company.getCompanyCode()));
        summary.setCompanyName(company == null ? item.getCompanyId() : company.getCompanyName());
        summary.setStatus(item.getStatus());
        summary.setStatusLabel(resolveAccountSetStatusLabel(item.getStatus()));
        summary.setEnabledYearMonth(formatYearMonth(item.getEnabledYear(), item.getEnabledPeriod()));
        summary.setTemplateCode(item.getTemplateCode());
        summary.setTemplateName(template == null ? item.getTemplateCode() : template.getTemplateName());
        summary.setSupervisorUserId(item.getSupervisorUserId());
        summary.setSupervisorName(supervisor == null ? null : firstNonBlank(trimToNull(supervisor.getName()), trimToNull(supervisor.getUsername())));
        summary.setCreateMode(item.getCreateMode());
        summary.setReferenceCompanyId(item.getReferenceCompanyId());
        summary.setReferenceCompanyName(referenceCompany == null ? null : referenceCompany.getCompanyName());
        summary.setSubjectCodeScheme(resolveSubjectCodeScheme(item, codeRuleMap));
        summary.setSubjectCount(item.getSubjectCount());
        summary.setLastTaskNo(item.getLastTaskNo());
        if (task != null) {
            summary.setLastTaskStatus(task.getStatus());
            summary.setLastTaskProgress(task.getProgress());
            summary.setLastTaskMessage(task.getResultMessage());
        }
        summary.setUpdatedAt(formatDateTime(item.getUpdatedAt()));
        return summary;
    }

    private FinanceAccountSetTaskPayload normalizePayload(FinanceAccountSetCreateDTO dto) {
        FinanceAccountSetTaskPayload payload = new FinanceAccountSetTaskPayload();
        payload.setCreateMode(normalizeCreateMode(dto.getCreateMode()));
        payload.setReferenceCompanyId(trimToNull(dto.getReferenceCompanyId()));
        payload.setTargetCompanyId(requireText(dto.getTargetCompanyId(), "目标公司不能为空"));
        payload.setEnabledYearMonth(requireEnabledYearMonth(dto.getEnabledYearMonth()));
        payload.setTemplateCode(trimToNull(dto.getTemplateCode()));
        payload.setSupervisorUserId(dto.getSupervisorUserId());
        payload.setSubjectCodeScheme(normalizeScheme(dto.getSubjectCodeScheme()));
        return payload;
    }

    private void validateBeforeSubmit(FinanceAccountSetTaskPayload payload) {
        requireEnabledCompany(payload.getTargetCompanyId());
        requireSupervisor(payload.getSupervisorUserId());

        if (financeAccountSetMapper.selectById(payload.getTargetCompanyId()) != null) {
            throw new IllegalStateException("目标公司已存在账套，不能重复创建");
        }

        AsyncTaskRecord activeTask = asyncTaskRecordMapper.selectOne(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getTaskType, AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE)
                        .eq(AsyncTaskRecord::getBusinessKey, payload.getTargetCompanyId())
                        .in(AsyncTaskRecord::getStatus, AsyncTaskSupport.TASK_STATUS_PENDING, AsyncTaskSupport.TASK_STATUS_RUNNING)
                        .orderByDesc(AsyncTaskRecord::getCreatedAt, AsyncTaskRecord::getId)
                        .last("limit 1")
        );
        if (activeTask != null) {
            throw new IllegalStateException("当前公司已有账套创建任务正在执行");
        }

        if (CREATE_MODE_REFERENCE.equals(payload.getCreateMode())) {
            String referenceCompanyId = requireText(payload.getReferenceCompanyId(), "参照账套不能为空");
            if (referenceCompanyId.equals(payload.getTargetCompanyId())) {
                throw new IllegalArgumentException("目标公司与参照账套公司不能相同");
            }
            FinanceAccountSet referenceAccountSet = financeAccountSetMapper.selectById(referenceCompanyId);
            if (referenceAccountSet == null || !"ACTIVE".equalsIgnoreCase(referenceAccountSet.getStatus())) {
                throw new IllegalStateException("参照账套不存在或尚未启用");
            }
            requireEnabledCompany(referenceCompanyId);
            return;
        }

        String templateCode = requireText(payload.getTemplateCode(), "账套模板不能为空");
        FinanceAccountSetTemplate template = financeAccountSetTemplateMapper.selectById(templateCode);
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
            throw new IllegalStateException("账套模板不存在");
        }
        validateScheme(payload.getSubjectCodeScheme());
    }

    private SystemCompany requireEnabledCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null || !Integer.valueOf(1).equals(company.getStatus())) {
            throw new IllegalStateException("目标公司不存在或未启用");
        }
        return company;
    }

    private User requireSupervisor(Long supervisorUserId) {
        if (supervisorUserId == null) {
            throw new IllegalArgumentException("账套主管不能为空");
        }
        User user = userMapper.selectById(supervisorUserId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalStateException("账套主管不存在或未启用");
        }
        return user;
    }

    private String requireEnabledYearMonth(String enabledYearMonth) {
        String normalized = requireText(enabledYearMonth, "启用年月不能为空");
        try {
            YearMonth.parse(normalized);
            return normalized;
        } catch (Exception ex) {
            throw new IllegalArgumentException("启用年月格式必须为 YYYY-MM");
        }
    }

    private String normalizeCreateMode(String createMode) {
        String normalized = requireText(createMode, "创建方式不能为空").toUpperCase(Locale.ROOT);
        if (!Set.of(CREATE_MODE_BLANK, CREATE_MODE_REFERENCE).contains(normalized)) {
            throw new IllegalArgumentException("不支持的创建方式");
        }
        return normalized;
    }

    private String normalizeScheme(String scheme) {
        return trimToNull(scheme) == null ? DEFAULT_SUBJECT_SCHEME : scheme.trim();
    }

    private void validateScheme(String scheme) {
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

    private String writePayload(FinanceAccountSetTaskPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("账套创建任务参数序列化失败", ex);
        }
    }

    private FinanceContextCompanyOptionVO toCompanyOption(SystemCompany company) {
        FinanceContextCompanyOptionVO option = new FinanceContextCompanyOptionVO();
        option.setCompanyId(company.getCompanyId());
        option.setCompanyCode(company.getCompanyCode());
        option.setCompanyName(company.getCompanyName());
        option.setValue(company.getCompanyId());
        option.setLabel(firstNonBlank(trimToNull(company.getCompanyCode()), company.getCompanyId()) + " - " + company.getCompanyName());
        return option;
    }
    private FinanceAccountSetTaskStatusVO toTaskStatus(AsyncTaskRecord task, String accountSetStatus) {
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

    private Map<String, SystemCompany> loadCompanyMap() {
        return systemCompanyMapper.selectList(Wrappers.<SystemCompany>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(SystemCompany::getCompanyId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, FinanceAccountSetTemplate> loadTemplateMap() {
        return financeAccountSetTemplateMapper.selectList(Wrappers.<FinanceAccountSetTemplate>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(FinanceAccountSetTemplate::getTemplateCode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, User> loadUserMap() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, FinanceAccountSetCodeRule> loadCodeRuleMap() {
        return financeAccountSetCodeRuleMapper.selectList(
                        Wrappers.<FinanceAccountSetCodeRule>lambdaQuery()
                                .eq(FinanceAccountSetCodeRule::getRuleType, FinanceAccountSetTaskWorker.RULE_TYPE_ACCOUNT_SUBJECT)
                ).stream()
                .collect(Collectors.toMap(FinanceAccountSetCodeRule::getCompanyId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private List<FinanceAccountSet> loadAccountSets() {
        return financeAccountSetMapper.selectList(
                Wrappers.<FinanceAccountSet>lambdaQuery()
                        .orderByDesc(FinanceAccountSet::getUpdatedAt, FinanceAccountSet::getCreatedAt)
        );
    }

    private Map<String, AsyncTaskRecord> loadLatestTaskMap(List<String> taskNos) {
        if (taskNos == null || taskNos.isEmpty()) {
            return Map.of();
        }
        return asyncTaskRecordMapper.selectList(
                        Wrappers.<AsyncTaskRecord>lambdaQuery().in(AsyncTaskRecord::getTaskNo, taskNos)
                ).stream()
                .collect(Collectors.toMap(AsyncTaskRecord::getTaskNo, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private String resolveSubjectCodeScheme(FinanceAccountSet accountSet, Map<String, FinanceAccountSetCodeRule> codeRuleMap) {
        String fromAccountSet = trimToNull(accountSet.getSubjectCodeScheme());
        if (fromAccountSet != null) {
            return fromAccountSet;
        }
        FinanceAccountSetCodeRule rule = codeRuleMap.get(accountSet.getCompanyId());
        return rule == null ? DEFAULT_SUBJECT_SCHEME : firstNonBlank(trimToNull(rule.getScheme()), DEFAULT_SUBJECT_SCHEME);
    }

    private String resolveAccountSetStatusLabel(String status) {
        return switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "已启用";
            case "INITIALIZING" -> "创建中";
            case "FAILED" -> "创建失败";
            default -> "未创建";
        };
    }

    private String formatYearMonth(Integer enabledYear, Integer enabledPeriod) {
        if (enabledYear == null || enabledPeriod == null) {
            return null;
        }
        return String.format(Locale.ROOT, "%04d-%02d", enabledYear, enabledPeriod);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

