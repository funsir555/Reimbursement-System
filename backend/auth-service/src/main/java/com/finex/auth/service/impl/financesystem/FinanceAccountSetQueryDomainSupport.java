package com.finex.auth.service.impl.financesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FinanceAccountSetQueryDomainSupport extends AbstractFinanceSystemManagementSupport {

    public FinanceAccountSetQueryDomainSupport(
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
        super(
                financeAccountSetMapper,
                financeAccountSetCodeRuleMapper,
                financeAccountSetTemplateMapper,
                financeAccountSetTemplateSubjectMapper,
                systemCompanyMapper,
                userMapper,
                asyncTaskRecordMapper,
                financeAccountSetTaskWorker,
                objectMapper
        );
    }

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
}
