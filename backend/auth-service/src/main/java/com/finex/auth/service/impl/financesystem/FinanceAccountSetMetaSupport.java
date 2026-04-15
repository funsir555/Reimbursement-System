// 业务域：财务系统管理
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 财务系统设置和账套相关接口，下游会继续协调 账套、同步任务和财务上下文基础数据。
// 风险提醒：改坏后最容易影响 账套切换、基础数据同步和下游系统连接。

package com.finex.auth.service.impl.financesystem;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetOptionVO;
import com.finex.auth.dto.FinanceAccountSetReferenceOptionVO;
import com.finex.auth.dto.FinanceAccountSetTemplateSummaryVO;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
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
import com.finex.auth.service.impl.FinanceAccountSetTaskWorker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FinanceAccountSetMetaSupport：通用支撑类。
 * 封装 财务账户Set这块可复用的业务能力。
 * 改这里时，要特别关注 账套切换、基础数据同步和下游系统连接是否会被一起带坏。
 */
public class FinanceAccountSetMetaSupport extends AbstractFinanceSystemManagementSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceAccountSetMetaSupport(
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

    /**
     * 获取元数据。
     */
    public FinanceAccountSetMetaVO getMeta() {
        FinanceAccountSetMetaVO meta = new FinanceAccountSetMetaVO();
        meta.setCompanyOptions(loadCompanyOptions());
        meta.setSupervisorOptions(loadSupervisorOptions());
        meta.setTemplateOptions(loadTemplateOptions());
        meta.setReferenceOptions(buildReferenceOptions(loadAccountSets(), loadCompanyMap(), loadTemplateMap(), loadCodeRuleMap()));
        meta.setDefaultSubjectCodeScheme(DEFAULT_SUBJECT_SCHEME);
        return meta;
    }

    /**
     * 加载公司选项。
     */
    private List<FinanceContextCompanyOptionVO> loadCompanyOptions() {
        return systemCompanyMapper().selectList(
                        Wrappers.<SystemCompany>lambdaQuery()
                                .eq(SystemCompany::getStatus, 1)
                                .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
                ).stream()
                .map(this::toCompanyOption)
                .toList();
    }

    /**
     * 加载Supervisor选项。
     */
    private List<FinanceAccountSetOptionVO> loadSupervisorOptions() {
        return userMapper().selectList(
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

    /**
     * 加载模板选项。
     */
    private List<FinanceAccountSetTemplateSummaryVO> loadTemplateOptions() {
        List<FinanceAccountSetTemplate> templates = financeAccountSetTemplateMapper().selectList(
                Wrappers.<FinanceAccountSetTemplate>lambdaQuery()
                        .eq(FinanceAccountSetTemplate::getStatus, 1)
                        .orderByAsc(FinanceAccountSetTemplate::getTemplateCode)
        );
        Map<String, List<FinanceAccountSetTemplateSubject>> subjectsByTemplate = financeAccountSetTemplateSubjectMapper().selectList(
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

    /**
     * 组装Reference选项。
     */
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
}
