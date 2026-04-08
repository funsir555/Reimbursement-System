
package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetTaskPayload;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetCodeRule;
import com.finex.auth.entity.FinanceAccountSetTemplate;
import com.finex.auth.entity.FinanceAccountSetTemplateSubject;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.NotificationService;
import com.finex.auth.support.AsyncTaskSupport;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceAccountSetTaskWorker {

    public static final String RULE_TYPE_ACCOUNT_SUBJECT = "ACCOUNT_SUBJECT";

    private static final String CREATE_MODE_BLANK = "BLANK";
    private static final String CREATE_MODE_REFERENCE = "REFERENCE";

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper;
    private final FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper;
    private final FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper;
    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Async("finexAsyncExecutor")
    public void runCreateAccountSetTask(Long taskId) {
        AsyncTaskRecord task = asyncTaskRecordMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        try {
            markTask(task, AsyncTaskSupport.TASK_STATUS_RUNNING, 10, "正在校验账套参数", null);
            FinanceAccountSetTaskPayload payload = readPayload(task);

            transactionTemplate.executeWithoutResult(status -> performCreate(task, payload));

            markTask(task, AsyncTaskSupport.TASK_STATUS_SUCCESS, 100, "账套创建完成", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "账套创建完成",
                    "新建账套任务已完成，请到财务系统管理查看结果。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("finance account set create failed, taskNo={}", task.getTaskNo(), ex);
            markFailedAccountSet(task, ex.getMessage());
            markTask(
                    task,
                    AsyncTaskSupport.TASK_STATUS_FAILED,
                    Math.max(task.getProgress() == null ? 0 : task.getProgress(), 15),
                    firstNonBlank(ex.getMessage(), "账套创建失败"),
                    LocalDateTime.now()
            );
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "账套创建失败",
                    "新建账套任务执行失败：" + firstNonBlank(ex.getMessage(), "请稍后重试"),
                    task.getTaskNo()
            );
        }
    }

    private void performCreate(AsyncTaskRecord task, FinanceAccountSetTaskPayload payload) {
        SystemCompany targetCompany = requireEnabledCompany(payload.getTargetCompanyId());
        User supervisor = requireSupervisor(payload.getSupervisorUserId());
        if (financeAccountSetMapper.selectById(targetCompany.getCompanyId()) != null) {
            throw new IllegalStateException("目标公司已存在账套，不能重复创建");
        }

        markTask(task, AsyncTaskSupport.TASK_STATUS_RUNNING, 25, "正在创建账套主信息", null);
        YearMonth enabledYearMonth = YearMonth.parse(payload.getEnabledYearMonth());

        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId(targetCompany.getCompanyId());
        accountSet.setStatus("INITIALIZING");
        accountSet.setEnabledYear(enabledYearMonth.getYear());
        accountSet.setEnabledPeriod(enabledYearMonth.getMonthValue());
        accountSet.setSupervisorUserId(supervisor.getId());
        accountSet.setCreateMode(payload.getCreateMode());
        accountSet.setReferenceCompanyId(payload.getReferenceCompanyId());
        accountSet.setLastTaskNo(task.getTaskNo());
        accountSet.setErrorMessage(null);

        List<FinanceAccountSubject> subjects;
        String subjectCodeScheme;
        String templateCode;

        if (CREATE_MODE_REFERENCE.equals(payload.getCreateMode())) {
            FinanceAccountSet sourceAccountSet = requireActiveReferenceAccountSet(payload);
            templateCode = sourceAccountSet.getTemplateCode();
            subjectCodeScheme = requireSourceScheme(sourceAccountSet.getCompanyId());
            subjects = copyReferenceSubjects(sourceAccountSet.getCompanyId(), payload.getTargetCompanyId(), templateCode);
        } else {
            templateCode = requireTemplateCode(payload.getTemplateCode());
            subjectCodeScheme = normalizeScheme(payload.getSubjectCodeScheme());
            subjects = buildBlankSubjects(templateCode, payload.getTargetCompanyId(), subjectCodeScheme);
        }
        accountSet.setTemplateCode(templateCode);
        accountSet.setSubjectCodeScheme(subjectCodeScheme);
        accountSet.setSubjectCount(subjects.size());
        financeAccountSetMapper.insert(accountSet);

        markTask(task, AsyncTaskSupport.TASK_STATUS_RUNNING, 55, "正在写入编码规则", null);
        FinanceAccountSetCodeRule codeRule = new FinanceAccountSetCodeRule();
        codeRule.setCompanyId(targetCompany.getCompanyId());
        codeRule.setRuleType(RULE_TYPE_ACCOUNT_SUBJECT);
        codeRule.setScheme(subjectCodeScheme);
        codeRule.setLevel1Length(4);
        financeAccountSetCodeRuleMapper.insert(codeRule);

        markTask(task, AsyncTaskSupport.TASK_STATUS_RUNNING, 80, "正在生成公司级会计科目", null);
        for (FinanceAccountSubject subject : subjects) {
            financeAccountSubjectMapper.insert(subject);
        }

        accountSet.setStatus("ACTIVE");
        accountSet.setErrorMessage(null);
        financeAccountSetMapper.updateById(accountSet);
    }

    private List<FinanceAccountSubject> buildBlankSubjects(String templateCode, String companyId, String scheme) {
        List<FinanceAccountSetTemplateSubject> templateSubjects = financeAccountSetTemplateSubjectMapper.selectList(
                Wrappers.<FinanceAccountSetTemplateSubject>lambdaQuery()
                        .eq(FinanceAccountSetTemplateSubject::getTemplateCode, templateCode)
                        .eq(FinanceAccountSetTemplateSubject::getStatus, 1)
                        .orderByAsc(FinanceAccountSetTemplateSubject::getSubjectLevel, FinanceAccountSetTemplateSubject::getSortOrder, FinanceAccountSetTemplateSubject::getId)
        );
        if (templateSubjects.isEmpty()) {
            throw new IllegalStateException("账套模板未配置科目体系");
        }

        int[] lengths = parseScheme(scheme);
        Map<String, String> codeByKey = new LinkedHashMap<>();
        List<FinanceAccountSubject> subjects = new ArrayList<>();
        for (FinanceAccountSetTemplateSubject templateSubject : templateSubjects) {
            int level = templateSubject.getSubjectLevel() == null ? 0 : templateSubject.getSubjectLevel();
            if (level <= 0 || level > lengths.length) {
                throw new IllegalStateException("模板科目层级超出编码规则范围");
            }

            String segment = requireText(templateSubject.getLevelSegment(), "模板科目段值不能为空");
            String parentCode = null;
            String subjectCode;
            if (level == 1) {
                if (segment.length() != 4) {
                    throw new IllegalStateException("一级科目编码必须为 4 位");
                }
                subjectCode = segment;
            } else {
                parentCode = codeByKey.get(templateSubject.getParentSubjectKey());
                if (parentCode == null) {
                    throw new IllegalStateException("模板科目父级不存在");
                }
                if (segment.length() > lengths[level - 1]) {
                    throw new IllegalStateException("模板科目段值长度超出编码规则");
                }
                subjectCode = parentCode + leftPad(segment, lengths[level - 1]);
            }
            codeByKey.put(templateSubject.getSubjectKey(), subjectCode);
            subjects.add(toAccountSubject(templateSubject, companyId, templateCode, subjectCode, parentCode));
        }
        return subjects;
    }

    private List<FinanceAccountSubject> copyReferenceSubjects(String sourceCompanyId, String targetCompanyId, String templateCode) {
        List<FinanceAccountSubject> sourceSubjects = financeAccountSubjectMapper.selectList(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, sourceCompanyId)
                        .orderByAsc(FinanceAccountSubject::getSubjectLevel, FinanceAccountSubject::getSortOrder, FinanceAccountSubject::getSubjectCode, FinanceAccountSubject::getId)
        );
        if (sourceSubjects.isEmpty()) {
            throw new IllegalStateException("参照账套缺少会计科目，不能创建");
        }
        return sourceSubjects.stream().map(source -> {
            FinanceAccountSubject target = new FinanceAccountSubject();
            target.setCompanyId(targetCompanyId);
            target.setSubjectCode(source.getSubjectCode());
            target.setSubjectName(source.getSubjectName());
            target.setParentSubjectCode(source.getParentSubjectCode());
            target.setSubjectLevel(source.getSubjectLevel());
            target.setBalanceDirection(source.getBalanceDirection());
            target.setSubjectCategory(source.getSubjectCategory());
            target.setLeafFlag(source.getLeafFlag());
            target.setStatus(source.getStatus() == null ? 1 : source.getStatus());
            target.setTemplateCode(templateCode);
            target.setSortOrder(source.getSortOrder());
            return target;
        }).toList();
    }

    private FinanceAccountSubject toAccountSubject(
            FinanceAccountSetTemplateSubject templateSubject,
            String companyId,
            String templateCode,
            String subjectCode,
            String parentCode
    ) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId(companyId);
        subject.setSubjectCode(subjectCode);
        subject.setSubjectName(templateSubject.getSubjectName());
        subject.setParentSubjectCode(parentCode);
        subject.setSubjectLevel(templateSubject.getSubjectLevel());
        subject.setBalanceDirection(templateSubject.getBalanceDirection());
        subject.setSubjectCategory(templateSubject.getSubjectCategory());
        subject.setLeafFlag(templateSubject.getLeafFlag());
        subject.setStatus(templateSubject.getStatus() == null ? 1 : templateSubject.getStatus());
        subject.setTemplateCode(templateCode);
        subject.setSortOrder(templateSubject.getSortOrder());
        return subject;
    }
    private FinanceAccountSet requireActiveReferenceAccountSet(FinanceAccountSetTaskPayload payload) {
        String referenceCompanyId = requireText(payload.getReferenceCompanyId(), "参照账套不能为空");
        if (referenceCompanyId.equals(payload.getTargetCompanyId())) {
            throw new IllegalArgumentException("目标公司与参照账套公司不能相同");
        }
        requireEnabledCompany(referenceCompanyId);
        FinanceAccountSet accountSet = financeAccountSetMapper.selectById(referenceCompanyId);
        if (accountSet == null || !"ACTIVE".equalsIgnoreCase(accountSet.getStatus())) {
            throw new IllegalStateException("参照账套不存在或尚未启用");
        }
        return accountSet;
    }

    private String requireSourceScheme(String companyId) {
        FinanceAccountSetCodeRule rule = financeAccountSetCodeRuleMapper.selectOne(
                Wrappers.<FinanceAccountSetCodeRule>lambdaQuery()
                        .eq(FinanceAccountSetCodeRule::getCompanyId, companyId)
                        .eq(FinanceAccountSetCodeRule::getRuleType, RULE_TYPE_ACCOUNT_SUBJECT)
                        .last("limit 1")
        );
        if (rule == null || trimToNull(rule.getScheme()) == null) {
            throw new IllegalStateException("参照账套缺少科目编码规则");
        }
        return rule.getScheme().trim();
    }

    private String requireTemplateCode(String templateCode) {
        String normalized = requireText(templateCode, "账套模板不能为空");
        FinanceAccountSetTemplate template = financeAccountSetTemplateMapper.selectById(normalized);
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
            throw new IllegalStateException("账套模板不存在");
        }
        return normalized;
    }

    private void markFailedAccountSet(AsyncTaskRecord task, String errorMessage) {
        if (trimToNull(task.getCompanyId()) == null) {
            return;
        }
        FinanceAccountSet accountSet = financeAccountSetMapper.selectById(task.getCompanyId());
        if (accountSet == null) {
            accountSet = new FinanceAccountSet();
            accountSet.setCompanyId(task.getCompanyId());
            accountSet.setStatus("FAILED");
            accountSet.setLastTaskNo(task.getTaskNo());
            accountSet.setErrorMessage(firstNonBlank(errorMessage, "账套创建失败"));
            financeAccountSetMapper.insert(accountSet);
            return;
        }
        accountSet.setStatus("FAILED");
        accountSet.setLastTaskNo(task.getTaskNo());
        accountSet.setErrorMessage(firstNonBlank(errorMessage, "账套创建失败"));
        financeAccountSetMapper.updateById(accountSet);
    }

    private FinanceAccountSetTaskPayload readPayload(AsyncTaskRecord task) {
        try {
            return objectMapper.readValue(task.getResultPayload(), FinanceAccountSetTaskPayload.class);
        } catch (Exception ex) {
            throw new IllegalStateException("读取账套创建任务参数失败", ex);
        }
    }

    private void markTask(AsyncTaskRecord task, String status, int progress, String message, LocalDateTime finishedAt) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setResultMessage(message);
        if (AsyncTaskSupport.TASK_STATUS_RUNNING.equalsIgnoreCase(status) && task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        task.setFinishedAt(finishedAt);
        asyncTaskRecordMapper.updateById(task);
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

    private String normalizeScheme(String scheme) {
        return trimToNull(scheme) == null ? "4-2-2-2" : scheme.trim();
    }

    private int[] parseScheme(String scheme) {
        String normalized = normalizeScheme(scheme);
        String[] parts = normalized.split("-");
        if (parts.length == 0 || !"4".equals(parts[0])) {
            throw new IllegalArgumentException("科目编码规则必须以 4 开头");
        }
        int[] result = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            try {
                result[index] = Integer.parseInt(parts[index]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("科目编码规则格式不正确");
            }
            if (result[index] <= 0) {
                throw new IllegalArgumentException("科目编码规则必须为正整数段长");
            }
        }
        return result;
    }

    private String leftPad(String value, int length) {
        if (value.length() >= length) {
            return value;
        }
        return "0".repeat(length - value.length()) + value;
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

