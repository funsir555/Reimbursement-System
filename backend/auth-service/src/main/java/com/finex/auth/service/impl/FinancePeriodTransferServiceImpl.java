package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinancePeriodTransferGenerateDTO;
import com.finex.auth.dto.FinancePeriodTransferGenerateResultVO;
import com.finex.auth.dto.FinancePeriodTransferMetaVO;
import com.finex.auth.dto.FinancePeriodTransferPreviewDTO;
import com.finex.auth.dto.FinancePeriodTransferPreviewDetailVO;
import com.finex.auth.dto.FinancePeriodTransferPreviewResultVO;
import com.finex.auth.dto.FinancePeriodTransferRuleDTO;
import com.finex.auth.dto.FinancePeriodTransferRuleLineDTO;
import com.finex.auth.dto.FinancePeriodTransferRuleLineVO;
import com.finex.auth.dto.FinancePeriodTransferRuleVO;
import com.finex.auth.dto.FinancePeriodTransferRunDetailVO;
import com.finex.auth.dto.FinancePeriodTransferRunVO;
import com.finex.auth.dto.FinanceVoucherEntryDTO;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinancePeriodClose;
import com.finex.auth.entity.FinancePeriodTransferRule;
import com.finex.auth.entity.FinancePeriodTransferRuleLine;
import com.finex.auth.entity.FinancePeriodTransferRun;
import com.finex.auth.entity.FinancePeriodTransferRunDetail;
import com.finex.auth.entity.FinancePeriodTransferVoucherLink;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePeriodTransferRuleLineMapper;
import com.finex.auth.mapper.FinancePeriodTransferRuleMapper;
import com.finex.auth.mapper.FinancePeriodTransferRunDetailMapper;
import com.finex.auth.mapper.FinancePeriodTransferRunMapper;
import com.finex.auth.mapper.FinancePeriodTransferVoucherLinkMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinancePeriodTransferService;
import com.finex.auth.service.FinanceVoucherService;
import com.finex.auth.support.FinanceModuleEnableSupport;
import com.finex.auth.support.FinanceVoucherAmountSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancePeriodTransferServiceImpl implements FinancePeriodTransferService {

    private static final String VIEW_PERMISSION = "finance:general_ledger:period_transfer:view";
    private static final String RULE_TYPE_PROFIT = "PROFIT";
    private static final String RULE_TYPE_CUSTOM = "CUSTOM";
    private static final String RULE_TYPE_MANUFACTURE = "MANUFACTURE";
    private static final String STATUS_PREVIEWED = "PREVIEWED";
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String DEFAULT_VOUCHER_TYPE = "转";
    private static final String REGENERATE_STRATEGY = "REPLACE_UNPOSTED";
    private static final String GRANULARITY_SUBJECT = "SUBJECT";
    private static final String GRANULARITY_ASSIST = "ASSIST";
    private static final String ALLOCATION_FULL = "FULL";
    private static final String ALLOCATION_MANUAL_RATIO = "MANUAL_RATIO";
    private static final String ALLOCATION_PROJECT = "PROJECT";
    private static final String METRIC_ENDING_BALANCE = "ENDING_BALANCE";
    private static final String METRIC_CURRENT_DEBIT = "CURRENT_DEBIT";
    private static final String METRIC_CURRENT_CREDIT = "CURRENT_CREDIT";
    private static final String METRIC_CURRENT_NET = "CURRENT_NET";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String SUMMARY_SUBJECT_CODE = "_SUMMARY_";

    private final FinancePeriodTransferRuleMapper ruleMapper;
    private final FinancePeriodTransferRuleLineMapper ruleLineMapper;
    private final FinancePeriodTransferRunMapper runMapper;
    private final FinancePeriodTransferRunDetailMapper runDetailMapper;
    private final FinancePeriodTransferVoucherLinkMapper voucherLinkMapper;
    private final FinanceAccountSubjectMapper accountSubjectMapper;
    private final FinanceProjectClassMapper projectClassMapper;
    private final FinanceProjectArchiveMapper projectArchiveMapper;
    private final GlAccsumMapper glAccsumMapper;
    private final GlAccassMapper glAccassMapper;
    private final GlAccvouchMapper glAccvouchMapper;
    private final FinanceVoucherService financeVoucherService;
    private final SystemCompanyMapper systemCompanyMapper;
    private final UserMapper userMapper;
    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper;
    private final FinancePeriodCloseMapper financePeriodCloseMapper;

    @Override
    public FinancePeriodTransferMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        SystemCompany company = resolveEffectiveCompany(currentUserId, companyId);
        requireActiveAccountSet(company.getCompanyId());
        int year = normalizeYear(iyear);
        int period = normalizePeriod(iperiod);
        FinancePeriodTransferMetaVO meta = new FinancePeriodTransferMetaVO();
        meta.setCompanyId(company.getCompanyId());
        meta.setCompanyName(company.getCompanyName());
        meta.setIyear(year);
        meta.setIperiod(period);
        meta.setIyperiod(year * 100 + period);
        meta.setPeriodLabel(year + "-" + String.format(Locale.ROOT, "%02d", period));
        meta.setDefaultVoucherType(DEFAULT_VOUCHER_TYPE);
        meta.setDefaultVoucherTypeLabel("转账凭证");
        meta.setAccountOptions(loadAccountOptions(company.getCompanyId()));
        meta.setProjectClassOptions(loadProjectClassOptions(company.getCompanyId()));
        meta.setProjectOptions(loadProjectOptions(company.getCompanyId()));
        meta.setMetricOptions(List.of(
                option(METRIC_ENDING_BALANCE, "期末余额"),
                option(METRIC_CURRENT_DEBIT, "本期借方发生额"),
                option(METRIC_CURRENT_CREDIT, "本期贷方发生额"),
                option(METRIC_CURRENT_NET, "本期净发生额")
        ));
        meta.setTransferGranularityOptions(List.of(
                option(GRANULARITY_SUBJECT, "按科目"),
                option(GRANULARITY_ASSIST, "按辅助核算")
        ));
        meta.setAllocationModeOptions(List.of(
                option(ALLOCATION_FULL, "全额结转"),
                option(ALLOCATION_MANUAL_RATIO, "手工比例分配"),
                option(ALLOCATION_PROJECT, "按项目辅助分配")
        ));
        meta.setRuleTypeOptions(List.of(
                option(RULE_TYPE_PROFIT, "期间损益结转"),
                option(RULE_TYPE_CUSTOM, "自定义比例/公式结转"),
                option(RULE_TYPE_MANUFACTURE, "制造费用结转")
        ));
        return meta;
    }

    @Override
    public List<FinancePeriodTransferRuleVO> listRules(String companyId) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireActiveAccountSet(normalizedCompanyId);
        List<FinancePeriodTransferRule> rules = ruleMapper.selectList(
                Wrappers.<FinancePeriodTransferRule>lambdaQuery()
                        .eq(FinancePeriodTransferRule::getCompanyId, normalizedCompanyId)
                        .orderByAsc(FinancePeriodTransferRule::getRuleType)
        );
        if (rules.isEmpty()) {
            return buildDefaultRules(normalizedCompanyId);
        }
        Map<Long, List<FinancePeriodTransferRuleLine>> linesByRuleId = loadRuleLines(rules.stream().map(FinancePeriodTransferRule::getId).toList());
        List<FinancePeriodTransferRuleVO> result = rules.stream()
                .map(rule -> toRuleVO(rule, linesByRuleId.getOrDefault(rule.getId(), List.of())))
                .toList();
        List<FinancePeriodTransferRuleVO> merged = mergeMissingDefaults(normalizedCompanyId, result);
        applyDefaultProfitSubjectLevel(normalizedCompanyId, merged);
        return merged;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinancePeriodTransferRuleVO saveRule(FinancePeriodTransferRuleDTO dto, Long currentUserId, String currentUsername) {
        validateRulePayload(dto);
        String companyId = requireCompanyId(dto.getCompanyId());
        requireActiveAccountSet(companyId);
        String operatorName = resolveOperatorName(currentUserId, currentUsername);
        Map<String, FinanceAccountSubject> subjectMap = loadSubjectMap(companyId);
        Map<String, FinanceProjectClass> projectClassMap = loadProjectClassMap(companyId);
        Map<String, FinanceProjectArchive> projectMap = loadProjectMap(companyId);
        Integer defaultProfitSubjectLevel = resolveDefaultProfitSubjectLevel(subjectMap);
        FinancePeriodTransferRule entity = dto.getId() == null
                ? new FinancePeriodTransferRule()
                : requireRule(companyId, dto.getId());
        applyRuleDTO(entity, dto, operatorName);
        if (RULE_TYPE_PROFIT.equals(entity.getRuleType()) && (entity.getSubjectLevel() == null || entity.getSubjectLevel() <= 0)) {
            entity.setSubjectLevel(defaultProfitSubjectLevel);
        }
        validateRuleBusiness(entity, dto.getLines(), subjectMap, projectClassMap, projectMap);
        if (entity.getId() == null) {
            ruleMapper.insert(entity);
        } else {
            ruleMapper.updateById(entity);
            ruleLineMapper.delete(Wrappers.<FinancePeriodTransferRuleLine>lambdaQuery().eq(FinancePeriodTransferRuleLine::getRuleId, entity.getId()));
        }
        List<FinancePeriodTransferRuleLine> lineEntities = buildRuleLines(entity.getId(), dto.getLines());
        for (FinancePeriodTransferRuleLine line : lineEntities) {
            ruleLineMapper.insert(line);
        }
        FinancePeriodTransferRuleVO vo = toRuleVO(requireRule(companyId, entity.getId()), lineEntities);
        applyDefaultProfitSubjectLevel(companyId, List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinancePeriodTransferPreviewResultVO preview(FinancePeriodTransferPreviewDTO dto, Long currentUserId, String currentUsername) {
        String companyId = requireCompanyId(dto.getCompanyId());
        requireActiveAccountSet(companyId);
        int year = normalizeYear(dto.getIyear());
        int period = normalizePeriod(dto.getIperiod());
        ensurePeriodNotClosed(companyId, year, period);
        FinancePeriodTransferRule rule = requireRule(companyId, dto.getRuleId());
        List<FinancePeriodTransferRuleLine> lines = loadRuleLines(List.of(rule.getId())).getOrDefault(rule.getId(), List.of());
        CalculationContext context = buildCalculationContext(companyId, year, period, isEnabled(rule.getIncludeUnposted()));
        PreviewBundle bundle = buildPreviewBundle(rule, lines, context);
        FinancePeriodTransferRun run = new FinancePeriodTransferRun();
        run.setCompanyId(companyId);
        run.setRuleId(rule.getId());
        run.setRuleType(rule.getRuleType());
        run.setRuleName(defaultString(rule.getRuleName(), resolveRuleTypeLabel(rule.getRuleType())));
        run.setIyear(year);
        run.setIperiod(period);
        run.setIyperiod(year * 100 + period);
        run.setStatus(STATUS_PREVIEWED);
        run.setPreviewToken(UUID.randomUUID().toString().replace("-", ""));
        run.setIncludeUnposted(defaultFlag(rule.getIncludeUnposted()));
        run.setVoucherType(defaultString(rule.getVoucherType(), DEFAULT_VOUCHER_TYPE));
        run.setGeneratedEntryCount(bundle.generatedCount());
        run.setSkippedEntryCount(bundle.skippedCount());
        run.setTotalAmount(bundle.totalAmount());
        run.setBlockedMessage(bundle.message());
        run.setRuleUpdatedAt(rule.getUpdatedAt());
        run.setPreviewedBy(resolveOperatorName(currentUserId, currentUsername));
        run.setPreviewedAt(LocalDateTime.now());
        runMapper.insert(run);
        int lineNo = 1;
        for (PreviewRow row : bundle.rows()) {
            FinancePeriodTransferRunDetail detail = toRunDetail(run, row, lineNo++);
            runDetailMapper.insert(detail);
            row.setPersistedId(detail.getId());
        }
        return toPreviewResult(run, bundle.rows(), bundle.message());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinancePeriodTransferGenerateResultVO generate(FinancePeriodTransferGenerateDTO dto, Long currentUserId, String currentUsername) {
        String companyId = requireCompanyId(dto.getCompanyId());
        requireActiveAccountSet(companyId);
        int year = normalizeYear(dto.getIyear());
        int period = normalizePeriod(dto.getIperiod());
        ensurePeriodNotClosed(companyId, year, period);
        FinancePeriodTransferRun run = requireRun(companyId, dto.getRunId());
        if (!Objects.equals(run.getIyear(), year) || !Objects.equals(run.getIperiod(), period)) {
            throw new IllegalArgumentException("预览结果与当前期间不一致，请重新预览");
        }
        if (!Objects.equals(trimToNull(run.getPreviewToken()), trimToNull(dto.getPreviewToken()))) {
            throw new IllegalArgumentException("预览令牌已失效，请重新预览");
        }
        if (!Objects.equals(run.getStatus(), STATUS_PREVIEWED)) {
            throw new IllegalStateException("当前预览结果已生成或已失效，请重新预览");
        }
        FinancePeriodTransferRule rule = requireRule(companyId, run.getRuleId());
        if (!Objects.equals(rule.getUpdatedAt(), run.getRuleUpdatedAt())) {
            throw new IllegalStateException("规则已发生变化，请重新预览后再生成");
        }
        handleRegeneration(companyId, year, period, rule.getRuleType());
        List<FinancePeriodTransferRunDetail> details = runDetailMapper.selectList(
                Wrappers.<FinancePeriodTransferRunDetail>lambdaQuery()
                        .eq(FinancePeriodTransferRunDetail::getRunId, run.getId())
                        .orderByAsc(FinancePeriodTransferRunDetail::getLineNo, FinancePeriodTransferRunDetail::getId)
        );
        Map<String, FinanceAccountSubject> subjectMap = loadSubjectMap(companyId);
        List<FinanceVoucherEntryDTO> voucherEntries = buildVoucherEntriesForRun(details, rule, subjectMap);
        if (voucherEntries.isEmpty()) {
            run.setStatus(STATUS_GENERATED);
            run.setGeneratedBy(resolveOperatorName(currentUserId, currentUsername));
            run.setGeneratedAt(LocalDateTime.now());
            run.setBlockedMessage("本期所有分录均不满足结转条件，已按跳过完成");
            runMapper.updateById(run);
            FinancePeriodTransferGenerateResultVO result = new FinancePeriodTransferGenerateResultVO();
            result.setRunId(run.getId());
            result.setGeneratedEntryCount(0);
            result.setSkippedEntryCount(defaultInt(run.getSkippedEntryCount()));
            result.setTotalAmount(run.getTotalAmount());
            result.setMessage("本期所有分录均不满足结转条件，未生成凭证");
            return result;
        }
        FinanceVoucherSaveDTO saveDTO = buildVoucherSaveDTO(companyId, year, period, rule, voucherEntries, resolveOperatorName(currentUserId, currentUsername));
        FinanceVoucherSaveResultVO saveResult = financeVoucherService.saveVoucher(saveDTO, currentUserId, currentUsername);
        for (int index = 0; index < details.size(); index++) {
            FinancePeriodTransferRunDetail detail = details.get(index);
            if (detail.getSkipReason() != null) {
                continue;
            }
            FinancePeriodTransferVoucherLink link = new FinancePeriodTransferVoucherLink();
            link.setRunId(run.getId());
            link.setRunDetailId(detail.getId());
            link.setVoucherNo(saveResult.getVoucherNo());
            link.setEntryNo(index * 2 + 1);
            voucherLinkMapper.insert(link);
        }
        run.setStatus(STATUS_GENERATED);
        run.setVoucherNo(saveResult.getVoucherNo());
        run.setGeneratedBy(resolveOperatorName(currentUserId, currentUsername));
        run.setGeneratedAt(LocalDateTime.now());
        runMapper.updateById(run);
        FinancePeriodTransferGenerateResultVO result = new FinancePeriodTransferGenerateResultVO();
        result.setRunId(run.getId());
        result.setVoucherNo(saveResult.getVoucherNo());
        result.setDisplayVoucherNo(buildDisplayVoucherNo(saveResult.getCsign(), saveResult.getInoId()));
        result.setGeneratedEntryCount(defaultInt(run.getGeneratedEntryCount()));
        result.setSkippedEntryCount(defaultInt(run.getSkippedEntryCount()));
        result.setTotalAmount(run.getTotalAmount());
        result.setMessage("期末结转凭证已生成");
        return result;
    }

    @Override
    public List<FinancePeriodTransferRunVO> listRuns(String companyId, Integer iyear, Integer iperiod, String ruleType) {
        String normalizedCompanyId = requireCompanyId(companyId);
        int year = normalizeYear(iyear);
        int period = normalizePeriod(iperiod);
        List<FinancePeriodTransferRun> runs = runMapper.selectList(
                Wrappers.<FinancePeriodTransferRun>lambdaQuery()
                        .eq(FinancePeriodTransferRun::getCompanyId, normalizedCompanyId)
                        .eq(FinancePeriodTransferRun::getIyear, year)
                        .eq(FinancePeriodTransferRun::getIperiod, period)
                        .eq(trimToNull(ruleType) != null, FinancePeriodTransferRun::getRuleType, trimToNull(ruleType))
                        .orderByDesc(FinancePeriodTransferRun::getCreatedAt, FinancePeriodTransferRun::getId)
                        .last("limit 50")
        );
        return runs.stream().map(this::toRunVO).toList();
    }

    @Override
    public FinancePeriodTransferRunDetailVO getRunDetail(String companyId, Long runId) {
        FinancePeriodTransferRun run = requireRun(requireCompanyId(companyId), runId);
        List<FinancePeriodTransferRunDetail> details = runDetailMapper.selectList(
                Wrappers.<FinancePeriodTransferRunDetail>lambdaQuery()
                        .eq(FinancePeriodTransferRunDetail::getRunId, run.getId())
                        .orderByAsc(FinancePeriodTransferRunDetail::getLineNo, FinancePeriodTransferRunDetail::getId)
        );
        FinancePeriodTransferRunDetailVO vo = new FinancePeriodTransferRunDetailVO();
        vo.setRun(toRunVO(run));
        vo.setDetails(details.stream().map(this::toPreviewDetailVO).toList());
        return vo;
    }

    @Override
    public boolean hasCompletedRunForPeriod(String companyId, int iyear, int iperiod) {
        List<FinancePeriodTransferRule> enabledRules = ruleMapper.selectList(
                Wrappers.<FinancePeriodTransferRule>lambdaQuery()
                        .eq(FinancePeriodTransferRule::getCompanyId, companyId)
                        .eq(FinancePeriodTransferRule::getEnabled, 1)
        );
        if (enabledRules.isEmpty()) {
            return true;
        }
        Set<Long> completedRuleIds = runMapper.selectList(
                Wrappers.<FinancePeriodTransferRun>lambdaQuery()
                        .eq(FinancePeriodTransferRun::getCompanyId, companyId)
                        .eq(FinancePeriodTransferRun::getIyear, iyear)
                        .eq(FinancePeriodTransferRun::getIperiod, iperiod)
                        .eq(FinancePeriodTransferRun::getStatus, STATUS_GENERATED)
        ).stream().map(FinancePeriodTransferRun::getRuleId).collect(Collectors.toSet());
        return enabledRules.stream().allMatch(rule -> completedRuleIds.contains(rule.getId()));
    }

    @Override
    public String resolveValidationMessage(String companyId, int iyear, int iperiod) {
        List<FinancePeriodTransferRule> enabledRules = ruleMapper.selectList(
                Wrappers.<FinancePeriodTransferRule>lambdaQuery()
                        .eq(FinancePeriodTransferRule::getCompanyId, companyId)
                        .eq(FinancePeriodTransferRule::getEnabled, 1)
        );
        if (enabledRules.isEmpty()) {
            return "当前期间未启用期末结转规则";
        }
        Set<Long> completedRuleIds = runMapper.selectList(
                Wrappers.<FinancePeriodTransferRun>lambdaQuery()
                        .eq(FinancePeriodTransferRun::getCompanyId, companyId)
                        .eq(FinancePeriodTransferRun::getIyear, iyear)
                        .eq(FinancePeriodTransferRun::getIperiod, iperiod)
                        .eq(FinancePeriodTransferRun::getStatus, STATUS_GENERATED)
        ).stream().map(FinancePeriodTransferRun::getRuleId).collect(Collectors.toSet());
        List<String> missing = enabledRules.stream()
                .filter(rule -> !completedRuleIds.contains(rule.getId()))
                .map(rule -> defaultString(rule.getRuleName(), resolveRuleTypeLabel(rule.getRuleType())))
                .toList();
        if (missing.isEmpty()) {
            return "本期期末结转已完成";
        }
        return "以下期末结转规则尚未完成：" + String.join("、", missing);
    }

    private void validateRulePayload(FinancePeriodTransferRuleDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("期末结转规则不能为空");
        }
        if (!Set.of(RULE_TYPE_PROFIT, RULE_TYPE_CUSTOM, RULE_TYPE_MANUFACTURE).contains(trimToNull(dto.getRuleType()))) {
            throw new IllegalArgumentException("期末结转规则类型不合法");
        }
    }

    private void applyRuleDTO(FinancePeriodTransferRule entity, FinancePeriodTransferRuleDTO dto, String operatorName) {
        entity.setCompanyId(trimToNull(dto.getCompanyId()));
        entity.setRuleType(trimToNull(dto.getRuleType()));
        entity.setRuleName(trimToNull(dto.getRuleName()));
        entity.setEnabled(dto.getEnabled() != null && dto.getEnabled() ? 1 : 0);
        entity.setVoucherType(defaultString(dto.getVoucherType(), DEFAULT_VOUCHER_TYPE));
        entity.setIncludeUnposted(dto.getIncludeUnposted() != null && dto.getIncludeUnposted() ? 1 : 0);
        entity.setTransferGranularity(trimToNull(dto.getTransferGranularity()));
        entity.setSubjectLevel(dto.getSubjectLevel());
        entity.setSourceSubjectCode(trimToNull(dto.getSourceSubjectCode()));
        entity.setSourceSubjectName(trimToNull(dto.getSourceSubjectName()));
        entity.setTargetSubjectCode(trimToNull(dto.getTargetSubjectCode()));
        entity.setTargetSubjectName(trimToNull(dto.getTargetSubjectName()));
        entity.setAllocationMode(trimToNull(dto.getAllocationMode()));
        entity.setRegenerateStrategy(REGENERATE_STRATEGY);
        if (entity.getId() == null) {
            entity.setCreatedBy(operatorName);
        }
        entity.setUpdatedBy(operatorName);
    }

    private void validateRuleBusiness(
            FinancePeriodTransferRule rule,
            List<FinancePeriodTransferRuleLineDTO> lines,
            Map<String, FinanceAccountSubject> subjectMap,
            Map<String, FinanceProjectClass> projectClassMap,
            Map<String, FinanceProjectArchive> projectMap
    ) {
        FinanceAccountSubject targetSubject = null;
        if (trimToNull(rule.getTargetSubjectCode()) != null) {
            targetSubject = requireLeafSubject(subjectMap, rule.getTargetSubjectCode(), "转入科目");
        }
        switch (rule.getRuleType()) {
            case RULE_TYPE_PROFIT -> {
                requireLeafSubject(subjectMap, rule.getTargetSubjectCode(), "本年利润科目");
                if (trimToNull(rule.getTransferGranularity()) == null) {
                    rule.setTransferGranularity(GRANULARITY_SUBJECT);
                }
            }
            case RULE_TYPE_CUSTOM -> {
                if (lines == null || lines.isEmpty()) {
                    throw new IllegalArgumentException("自定义比例/公式结转至少需要一条规则明细");
                }
                for (FinancePeriodTransferRuleLineDTO line : lines) {
                    FinanceAccountSubject source = requireLeafSubject(subjectMap, line.getSourceSubjectCode(), "转出科目");
                    FinanceAccountSubject lineTarget = requireLeafSubject(subjectMap, line.getTargetSubjectCode(), "转入科目");
                    validateAssistSubset(source, lineTarget);
                    validateMetricType(line.getMetricType());
                    if (line.getRatio() == null || line.getRatio().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("自定义比例/公式结转比例系数必须大于 0");
                    }
                }
            }
            case RULE_TYPE_MANUFACTURE -> {
                requireLeafSubject(subjectMap, rule.getTargetSubjectCode(), "制造费用转入科目");
                if (trimToNull(rule.getSourceSubjectCode()) == null) {
                    rule.setSourceSubjectCode("4101");
                    rule.setSourceSubjectName("制造费用");
                }
                String allocationMode = defaultString(rule.getAllocationMode(), ALLOCATION_FULL);
                rule.setAllocationMode(allocationMode);
                if (ALLOCATION_MANUAL_RATIO.equals(allocationMode) || ALLOCATION_PROJECT.equals(allocationMode)) {
                    if (lines == null || lines.isEmpty()) {
                        throw new IllegalArgumentException("制造费用分配至少需要一条分配明细");
                    }
                    BigDecimal totalRatio = ZERO;
                    for (FinancePeriodTransferRuleLineDTO line : lines) {
                        if (line.getRatio() == null || line.getRatio().compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("制造费用分配比例必须大于 0");
                        }
                        totalRatio = totalRatio.add(line.getRatio()).setScale(6, RoundingMode.HALF_UP);
                        if (ALLOCATION_PROJECT.equals(allocationMode)) {
                            String projectId = trimToNull(line.getAllocationProjectId());
                            if (projectId == null) {
                                throw new IllegalArgumentException("按项目辅助分配时必须选择项目");
                            }
                            FinanceProjectArchive project = projectMap.get(projectId);
                            if (project == null) {
                                throw new IllegalArgumentException("制造费用分配项目不存在");
                            }
                            String projectClassCode = trimToNull(line.getAllocationProjectClass());
                            if (projectClassCode == null) {
                                projectClassCode = trimToNull(project.getCitemccode());
                                line.setAllocationProjectClass(projectClassCode);
                            }
                            if (!projectClassMap.containsKey(projectClassCode)) {
                                throw new IllegalArgumentException("制造费用分配项目分类不存在");
                            }
                        }
                    }
                    if (totalRatio.compareTo(new BigDecimal("100")) != 0) {
                        throw new IllegalArgumentException("制造费用分配比例合计必须等于 100");
                    }
                }
                if (targetSubject != null && trimToNull(targetSubject.getCassItem()) != null && ALLOCATION_PROJECT.equals(allocationMode)) {
                    for (FinancePeriodTransferRuleLineDTO line : lines) {
                        if (!Objects.equals(trimToNull(targetSubject.getCassItem()), trimToNull(line.getAllocationProjectClass()))) {
                            throw new IllegalArgumentException("制造费用转入科目的项目分类与分配项目分类不一致");
                        }
                    }
                }
            }
            default -> throw new IllegalArgumentException("不支持的期末结转规则类型");
        }
    }

    private List<FinancePeriodTransferRuleLine> buildRuleLines(Long ruleId, List<FinancePeriodTransferRuleLineDTO> lines) {
        List<FinancePeriodTransferRuleLine> result = new ArrayList<>();
        if (lines == null) {
            return result;
        }
        int lineNo = 1;
        for (FinancePeriodTransferRuleLineDTO dto : lines) {
            FinancePeriodTransferRuleLine line = new FinancePeriodTransferRuleLine();
            line.setRuleId(ruleId);
            line.setLineNo(dto.getLineNo() == null ? lineNo : dto.getLineNo());
            line.setSourceSubjectCode(trimToNull(dto.getSourceSubjectCode()));
            line.setSourceSubjectName(trimToNull(dto.getSourceSubjectName()));
            line.setTargetSubjectCode(trimToNull(dto.getTargetSubjectCode()));
            line.setTargetSubjectName(trimToNull(dto.getTargetSubjectName()));
            line.setMetricType(trimToNull(dto.getMetricType()));
            line.setRatio(dto.getRatio() == null ? null : dto.getRatio().setScale(6, RoundingMode.HALF_UP));
            line.setAllocationProjectClass(trimToNull(dto.getAllocationProjectClass()));
            line.setAllocationProjectId(trimToNull(dto.getAllocationProjectId()));
            line.setAllocationProjectName(trimToNull(dto.getAllocationProjectName()));
            line.setRemark(trimToNull(dto.getRemark()));
            result.add(line);
            lineNo++;
        }
        return result;
    }

    private PreviewBundle buildPreviewBundle(
            FinancePeriodTransferRule rule,
            List<FinancePeriodTransferRuleLine> lines,
            CalculationContext context
    ) {
        List<PreviewRow> rows = switch (rule.getRuleType()) {
            case RULE_TYPE_PROFIT -> buildProfitPreview(rule, context);
            case RULE_TYPE_CUSTOM -> buildCustomPreview(rule, lines, context);
            case RULE_TYPE_MANUFACTURE -> buildManufacturePreview(rule, lines, context);
            default -> throw new IllegalArgumentException("不支持的期末结转规则类型");
        };
        int generatedCount = (int) rows.stream().filter(item -> item.skipReason == null).count();
        int skippedCount = rows.size() - generatedCount;
        BigDecimal totalAmount = rows.stream()
                .filter(item -> item.skipReason == null)
                .map(item -> item.amount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String message;
        if (generatedCount == 0) {
            message = RULE_TYPE_PROFIT.equals(rule.getRuleType())
                    ? "本期无可结转损益发生额"
                    : "本期所有分录均不满足结转条件";
        } else {
            message = "已生成 " + generatedCount + " 条结转预览";
        }
        return new PreviewBundle(rows, generatedCount, skippedCount, totalAmount, message);
    }

    private List<PreviewRow> buildProfitPreview(FinancePeriodTransferRule rule, CalculationContext context) {
        List<PreviewRow> rows = new ArrayList<>();
        FinanceAccountSubject targetSubject = context.subjectMap.get(trimToNull(rule.getTargetSubjectCode()));
        boolean assistMode = GRANULARITY_ASSIST.equals(defaultString(rule.getTransferGranularity(), GRANULARITY_SUBJECT));
        Integer maxSubjectLevel = normalizeNullableSubjectLevel(rule.getSubjectLevel());
        if (maxSubjectLevel == null) {
            maxSubjectLevel = resolveDefaultProfitSubjectLevel(context.subjectMap);
        }
        List<ProfitSnapshot> snapshots = buildProfitSnapshots(rule, context, assistMode, maxSubjectLevel);
        for (ProfitSnapshot snapshot : snapshots) {
            FinanceAccountSubject subject = context.subjectMap.get(snapshot.subjectCode());
            if (subject == null || !Objects.equals(subject.getCompanyId(), rule.getCompanyId())) {
                continue;
            }
            if (!isProfitLike(subject)) {
                continue;
            }
            rows.add(buildTransferRow(rule, subject, targetSubject, snapshot.assist(), snapshot.amount(), snapshot.direction(), snapshot.metricType(), snapshot.traceKey()));
        }
        return rows;
    }

    private List<PreviewRow> buildCustomPreview(FinancePeriodTransferRule rule, List<FinancePeriodTransferRuleLine> lines, CalculationContext context) {
        List<PreviewRow> rows = new ArrayList<>();
        for (FinancePeriodTransferRuleLine line : lines) {
            FinanceAccountSubject sourceSubject = context.subjectMap.get(trimToNull(line.getSourceSubjectCode()));
            FinanceAccountSubject targetSubject = context.subjectMap.get(trimToNull(line.getTargetSubjectCode()));
            List<AssistSnapshot> assists = collectAssistSnapshots(context, sourceSubject.getSubjectCode());
            if (!assists.isEmpty()) {
                for (AssistSnapshot assist : assists) {
                    BigDecimal amount = resolveMetricAmount(assist, line.getMetricType()).multiply(defaultRatio(line.getRatio())).setScale(2, RoundingMode.HALF_UP);
                    rows.add(buildTransferRowFromLine(rule, line, sourceSubject, targetSubject, assist, amount, assist.direction(), line.getMetricType(), assist.key()));
                }
                continue;
            }
            SubjectSnapshot snapshot = context.subjectSnapshots.get(sourceSubject.getSubjectCode());
            if (snapshot == null) {
                rows.add(buildSkipRow(rule, targetSubject, "转出科目【" + sourceSubject.getSubjectCode() + "】不存在可结转余额", line, sourceSubject));
                continue;
            }
            BigDecimal amount = resolveMetricAmount(snapshot.asAssistSnapshot(), line.getMetricType()).multiply(defaultRatio(line.getRatio())).setScale(2, RoundingMode.HALF_UP);
            rows.add(buildTransferRowFromLine(rule, line, sourceSubject, targetSubject, snapshot.asAssistSnapshot(), amount, snapshot.direction(), line.getMetricType(), snapshot.subjectCode()));
        }
        return rows;
    }

    private List<PreviewRow> buildManufacturePreview(FinancePeriodTransferRule rule, List<FinancePeriodTransferRuleLine> lines, CalculationContext context) {
        FinanceAccountSubject targetSubject = context.subjectMap.get(trimToNull(rule.getTargetSubjectCode()));
        List<AssistSnapshot> sourceAssists = collectAssistSnapshotsByParent(context, defaultString(rule.getSourceSubjectCode(), "4101"));
        if (sourceAssists.isEmpty()) {
            SubjectSnapshot summary = collectSubjectSummaryByParent(context, defaultString(rule.getSourceSubjectCode(), "4101"));
            if (summary != null) {
                sourceAssists = List.of(summary.asAssistSnapshot());
            }
        }
        if (sourceAssists.isEmpty()) {
            return List.of(buildSkipRow(rule, targetSubject, "制造费用科目当前期间无可结转余额"));
        }
        String allocationMode = defaultString(rule.getAllocationMode(), ALLOCATION_FULL);
        if (ALLOCATION_FULL.equals(allocationMode)) {
            return sourceAssists.stream()
                    .map(assist -> buildTransferRow(rule, context.subjectMap.get(resolveAssistSubjectCode(assist)), targetSubject, assist, assist.amount(), assist.direction(), METRIC_ENDING_BALANCE, assist.key()))
                    .toList();
        }
        BigDecimal total = sourceAssists.stream().map(AssistSnapshot::amount).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        List<PreviewRow> rows = new ArrayList<>();
        for (FinancePeriodTransferRuleLine line : lines) {
            BigDecimal amount = total.multiply(defaultRatio(line.getRatio())).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            AssistSnapshot synthetic = AssistSnapshot.synthetic(
                    SUMMARY_SUBJECT_CODE,
                    "制造费用汇总",
                    null,
                    null,
                    null,
                    null,
                    ALLOCATION_PROJECT.equals(allocationMode) ? trimToNull(line.getAllocationProjectClass()) : null,
                    ALLOCATION_PROJECT.equals(allocationMode) ? trimToNull(line.getAllocationProjectId()) : null,
                    amount,
                    amount.compareTo(ZERO) >= 0 ? "DEBIT" : "CREDIT",
                    ZERO,
                    ZERO
            );
            rows.add(buildTransferRowFromLine(rule, line, null, targetSubject, synthetic, amount, "DEBIT", METRIC_ENDING_BALANCE, "MANUFACTURE_TOTAL"));
        }
        return rows;
    }

    private List<ProfitSnapshot> buildProfitSnapshots(
            FinancePeriodTransferRule rule,
            CalculationContext context,
            boolean assistMode,
            Integer maxSubjectLevel
    ) {
        List<GlAccvouch> voucherRows = glAccvouchMapper.selectList(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, rule.getCompanyId())
                        .eq(GlAccvouch::getIyear, context.iyear())
                        .eq(GlAccvouch::getIperiod, context.iperiod())
                        .orderByAsc(GlAccvouch::getCcode, GlAccvouch::getInid, GlAccvouch::getId)
        );
        Map<String, ProfitAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (GlAccvouch row : voucherRows) {
            String status = resolveVoucherStatus(row);
            if (Objects.equals(status, "ERROR")) {
                continue;
            }
            if (!isEnabled(rule.getIncludeUnposted()) && !Objects.equals(status, "POSTED")) {
                continue;
            }
            String subjectCode = trimToNull(row.getCcode());
            FinanceAccountSubject subject = context.subjectMap.get(subjectCode);
            if (subject == null || !Objects.equals(subject.getCompanyId(), rule.getCompanyId())) {
                continue;
            }
            if (!isProfitLike(subject)) {
                continue;
            }
            Integer subjectLevel = normalizeNullableSubjectLevel(subject.getSubjectLevel());
            if (maxSubjectLevel != null && subjectLevel != null && subjectLevel > maxSubjectLevel) {
                continue;
            }
            String assistKey = assistMode ? ProfitAccumulator.assistKeyOf(row) : null;
            String mapKey = subjectCode + "|" + defaultString(assistKey, "");
            ProfitAccumulator accumulator = accumulatorMap.computeIfAbsent(mapKey, unused -> ProfitAccumulator.create(subject, row, assistMode));
            accumulator.apply(row);
        }
        return accumulatorMap.values().stream()
                .map(ProfitAccumulator::toSnapshot)
                .filter(item -> item != null && item.amount().compareTo(ZERO) > 0)
                .toList();
    }

    private PreviewRow buildTransferRow(
            FinancePeriodTransferRule rule,
            FinanceAccountSubject sourceSubject,
            FinanceAccountSubject targetSubject,
            AssistSnapshot sourceAssist,
            BigDecimal amount,
            String sourceDirection,
            String metricType,
            String sourceTraceKey
    ) {
        return buildTransferRowFromLine(rule, null, sourceSubject, targetSubject, sourceAssist, amount, sourceDirection, metricType, sourceTraceKey);
    }

    private PreviewRow buildTransferRowFromLine(
            FinancePeriodTransferRule rule,
            FinancePeriodTransferRuleLine line,
            FinanceAccountSubject sourceSubject,
            FinanceAccountSubject targetSubject,
            AssistSnapshot sourceAssist,
            BigDecimal amount,
            String sourceDirection,
            String metricType,
            String sourceTraceKey
    ) {
        PreviewRow row = new PreviewRow();
        row.ruleLineId = line == null ? null : line.getId();
        row.sourceSubjectCode = sourceSubject == null ? defaultString(line == null ? null : line.getSourceSubjectCode(), rule.getSourceSubjectCode()) : sourceSubject.getSubjectCode();
        row.sourceSubjectName = sourceSubject == null ? defaultString(line == null ? null : line.getSourceSubjectName(), rule.getSourceSubjectName()) : sourceSubject.getSubjectName();
        row.sourceAssist = sourceAssist;
        row.targetSubjectCode = line != null && trimToNull(line.getTargetSubjectCode()) != null ? line.getTargetSubjectCode() : rule.getTargetSubjectCode();
        row.targetSubjectName = line != null && trimToNull(line.getTargetSubjectName()) != null ? line.getTargetSubjectName() : rule.getTargetSubjectName();
        row.metricType = defaultString(metricType, METRIC_ENDING_BALANCE);
        row.sourceTraceKey = sourceTraceKey;
        BigDecimal normalizedAmount = amount == null ? ZERO : amount.abs().setScale(2, RoundingMode.HALF_UP);
        if (normalizedAmount.compareTo(ZERO) <= 0) {
            row.skipReason = "不满足结转条件";
            row.amount = ZERO;
            row.direction = reverseDirection(sourceDirection);
            return row;
        }
        row.amount = normalizedAmount;
        row.direction = reverseDirection(sourceDirection);
        applyTargetAssist(row, rule, line, targetSubject, sourceAssist);
        return row;
    }

    private void applyTargetAssist(
            PreviewRow row,
            FinancePeriodTransferRule rule,
            FinancePeriodTransferRuleLine line,
            FinanceAccountSubject targetSubject,
            AssistSnapshot sourceAssist
    ) {
        String allocationMode = defaultString(rule.getAllocationMode(), ALLOCATION_FULL);
        if (RULE_TYPE_MANUFACTURE.equals(rule.getRuleType()) && ALLOCATION_PROJECT.equals(allocationMode) && line != null) {
            row.targetCitemClass = trimToNull(line.getAllocationProjectClass());
            row.targetCitemId = trimToNull(line.getAllocationProjectId());
            return;
        }
        boolean useAssist = GRANULARITY_ASSIST.equals(defaultString(rule.getTransferGranularity(), GRANULARITY_SUBJECT))
                || RULE_TYPE_CUSTOM.equals(rule.getRuleType())
                || (RULE_TYPE_MANUFACTURE.equals(rule.getRuleType()) && ALLOCATION_FULL.equals(allocationMode));
        if (!useAssist || sourceAssist == null || targetSubject == null) {
            return;
        }
        if (isEnabled(targetSubject.getBdept())) {
            row.targetCdeptId = sourceAssist.cdeptId;
        }
        if (isEnabled(targetSubject.getBperson())) {
            row.targetCpersonId = sourceAssist.cpersonId;
        }
        if (isEnabled(targetSubject.getBcus())) {
            row.targetCcusId = sourceAssist.ccusId;
        }
        if (isEnabled(targetSubject.getBsup())) {
            row.targetCsupId = sourceAssist.csupId;
        }
        if (isEnabled(targetSubject.getBitem())) {
            row.targetCitemClass = trimToNull(targetSubject.getCassItem()) != null ? trimToNull(targetSubject.getCassItem()) : sourceAssist.citemClass;
            row.targetCitemId = sourceAssist.citemId;
        }
    }

    private PreviewRow buildSkipRow(FinancePeriodTransferRule rule, FinanceAccountSubject targetSubject, String reason) {
        return buildSkipRow(rule, targetSubject, reason, null, null);
    }

    private PreviewRow buildSkipRow(
            FinancePeriodTransferRule rule,
            FinanceAccountSubject targetSubject,
            String reason,
            FinancePeriodTransferRuleLine line,
            FinanceAccountSubject sourceSubject
    ) {
        PreviewRow row = new PreviewRow();
        row.ruleLineId = line == null ? null : line.getId();
        row.sourceSubjectCode = sourceSubject == null ? (line == null ? rule.getSourceSubjectCode() : line.getSourceSubjectCode()) : sourceSubject.getSubjectCode();
        row.sourceSubjectName = sourceSubject == null ? (line == null ? rule.getSourceSubjectName() : line.getSourceSubjectName()) : sourceSubject.getSubjectName();
        row.targetSubjectCode = targetSubject == null ? rule.getTargetSubjectCode() : targetSubject.getSubjectCode();
        row.targetSubjectName = targetSubject == null ? rule.getTargetSubjectName() : targetSubject.getSubjectName();
        row.metricType = line == null ? METRIC_ENDING_BALANCE : defaultString(line.getMetricType(), METRIC_ENDING_BALANCE);
        row.skipReason = reason;
        row.amount = ZERO;
        row.direction = "DEBIT";
        row.sourceTraceKey = row.sourceSubjectCode;
        return row;
    }

    private FinancePeriodTransferRunDetail toRunDetail(FinancePeriodTransferRun run, PreviewRow row, int lineNo) {
        FinancePeriodTransferRunDetail detail = new FinancePeriodTransferRunDetail();
        detail.setRunId(run.getId());
        detail.setRuleId(run.getRuleId());
        detail.setRuleLineId(row.ruleLineId);
        detail.setLineNo(lineNo);
        detail.setSourceSubjectCode(trimToNull(row.sourceSubjectCode));
        detail.setSourceSubjectName(trimToNull(row.sourceSubjectName));
        detail.setSourceAssistKey(trimToNull(encodeAssistKey(row.sourceAssist)));
        detail.setSourceAssistLabel(trimToNull(sourceAssistLabel(row.sourceAssist)));
        detail.setTargetSubjectCode(trimToNull(row.targetSubjectCode));
        detail.setTargetSubjectName(trimToNull(row.targetSubjectName));
        detail.setTargetCdeptId(trimToNull(row.targetCdeptId));
        detail.setTargetCpersonId(trimToNull(row.targetCpersonId));
        detail.setTargetCcusId(trimToNull(row.targetCcusId));
        detail.setTargetCsupId(trimToNull(row.targetCsupId));
        detail.setTargetCitemClass(trimToNull(row.targetCitemClass));
        detail.setTargetCitemId(trimToNull(row.targetCitemId));
        detail.setMetricType(trimToNull(row.metricType));
        detail.setDirection(trimToNull(row.direction));
        detail.setAmount(row.amount == null ? ZERO : row.amount.setScale(2, RoundingMode.HALF_UP));
        detail.setSkipReason(trimToNull(row.skipReason));
        detail.setSourceTraceKey(trimToNull(row.sourceTraceKey));
        return detail;
    }

    private FinancePeriodTransferPreviewResultVO toPreviewResult(FinancePeriodTransferRun run, List<PreviewRow> rows, String message) {
        FinancePeriodTransferPreviewResultVO vo = new FinancePeriodTransferPreviewResultVO();
        vo.setRunId(run.getId());
        vo.setRuleId(run.getRuleId());
        vo.setRuleType(run.getRuleType());
        vo.setRuleTypeLabel(resolveRuleTypeLabel(run.getRuleType()));
        vo.setRuleName(run.getRuleName());
        vo.setPreviewToken(run.getPreviewToken());
        vo.setVoucherType(run.getVoucherType());
        vo.setGeneratedEntryCount(defaultInt(run.getGeneratedEntryCount()));
        vo.setSkippedEntryCount(defaultInt(run.getSkippedEntryCount()));
        vo.setTotalAmount(defaultMoney(run.getTotalAmount()));
        vo.setMessage(message);
        vo.setDetails(rows.stream().map(this::toPreviewDetailVO).toList());
        return vo;
    }

    private FinancePeriodTransferPreviewDetailVO toPreviewDetailVO(PreviewRow row) {
        FinancePeriodTransferPreviewDetailVO vo = new FinancePeriodTransferPreviewDetailVO();
        vo.setDetailId(row.persistedId);
        vo.setLineNo(row.lineNo);
        vo.setSourceSubjectCode(row.sourceSubjectCode);
        vo.setSourceSubjectName(row.sourceSubjectName);
        vo.setSourceAssistLabel(sourceAssistLabel(row.sourceAssist));
        vo.setTargetSubjectCode(row.targetSubjectCode);
        vo.setTargetSubjectName(row.targetSubjectName);
        vo.setMetricType(row.metricType);
        vo.setMetricTypeLabel(resolveMetricTypeLabel(row.metricType));
        vo.setDirection(row.direction);
        vo.setDirectionLabel(resolveDirectionLabel(row.direction));
        vo.setAmount(defaultMoney(row.amount));
        vo.setSkipReason(row.skipReason);
        vo.setSourceTraceKey(row.sourceTraceKey);
        return vo;
    }

    private FinancePeriodTransferPreviewDetailVO toPreviewDetailVO(FinancePeriodTransferRunDetail detail) {
        FinancePeriodTransferPreviewDetailVO vo = new FinancePeriodTransferPreviewDetailVO();
        vo.setDetailId(detail.getId());
        vo.setLineNo(detail.getLineNo());
        vo.setSourceSubjectCode(detail.getSourceSubjectCode());
        vo.setSourceSubjectName(detail.getSourceSubjectName());
        vo.setSourceAssistLabel(detail.getSourceAssistLabel());
        vo.setTargetSubjectCode(detail.getTargetSubjectCode());
        vo.setTargetSubjectName(detail.getTargetSubjectName());
        vo.setMetricType(detail.getMetricType());
        vo.setMetricTypeLabel(resolveMetricTypeLabel(detail.getMetricType()));
        vo.setDirection(detail.getDirection());
        vo.setDirectionLabel(resolveDirectionLabel(detail.getDirection()));
        vo.setAmount(defaultMoney(detail.getAmount()));
        vo.setSkipReason(detail.getSkipReason());
        vo.setSourceTraceKey(detail.getSourceTraceKey());
        return vo;
    }

    private FinancePeriodTransferRunVO toRunVO(FinancePeriodTransferRun run) {
        FinancePeriodTransferRunVO vo = new FinancePeriodTransferRunVO();
        vo.setId(run.getId());
        vo.setRuleId(run.getRuleId());
        vo.setRuleType(run.getRuleType());
        vo.setRuleTypeLabel(resolveRuleTypeLabel(run.getRuleType()));
        vo.setRuleName(run.getRuleName());
        vo.setIyear(run.getIyear());
        vo.setIperiod(run.getIperiod());
        vo.setIyperiod(run.getIyperiod());
        vo.setStatus(run.getStatus());
        vo.setStatusLabel(resolveRunStatusLabel(run.getStatus()));
        vo.setPreviewToken(run.getPreviewToken());
        vo.setIncludeUnposted(isEnabled(run.getIncludeUnposted()));
        vo.setVoucherType(run.getVoucherType());
        vo.setVoucherNo(run.getVoucherNo());
        vo.setGeneratedEntryCount(defaultInt(run.getGeneratedEntryCount()));
        vo.setSkippedEntryCount(defaultInt(run.getSkippedEntryCount()));
        vo.setTotalAmount(defaultMoney(run.getTotalAmount()));
        vo.setBlockedMessage(run.getBlockedMessage());
        vo.setPreviewedBy(run.getPreviewedBy());
        vo.setPreviewedAt(formatDateTime(run.getPreviewedAt()));
        vo.setGeneratedBy(run.getGeneratedBy());
        vo.setGeneratedAt(formatDateTime(run.getGeneratedAt()));
        return vo;
    }

    private FinancePeriodTransferRuleVO toRuleVO(FinancePeriodTransferRule rule, List<FinancePeriodTransferRuleLine> lines) {
        FinancePeriodTransferRuleVO vo = new FinancePeriodTransferRuleVO();
        vo.setId(rule.getId());
        vo.setCompanyId(rule.getCompanyId());
        vo.setRuleType(rule.getRuleType());
        vo.setRuleTypeLabel(resolveRuleTypeLabel(rule.getRuleType()));
        vo.setRuleName(rule.getRuleName());
        vo.setEnabled(isEnabled(rule.getEnabled()));
        vo.setVoucherType(defaultString(rule.getVoucherType(), DEFAULT_VOUCHER_TYPE));
        vo.setIncludeUnposted(isEnabled(rule.getIncludeUnposted()));
        vo.setTransferGranularity(defaultString(rule.getTransferGranularity(), GRANULARITY_SUBJECT));
        vo.setSubjectLevel(rule.getSubjectLevel());
        vo.setSourceSubjectCode(rule.getSourceSubjectCode());
        vo.setSourceSubjectName(rule.getSourceSubjectName());
        vo.setTargetSubjectCode(rule.getTargetSubjectCode());
        vo.setTargetSubjectName(rule.getTargetSubjectName());
        vo.setAllocationMode(defaultString(rule.getAllocationMode(), ALLOCATION_FULL));
        vo.setRegenerateStrategy(defaultString(rule.getRegenerateStrategy(), REGENERATE_STRATEGY));
        vo.setCreatedAt(formatDateTime(rule.getCreatedAt()));
        vo.setUpdatedAt(formatDateTime(rule.getUpdatedAt()));
        vo.setLines(lines.stream().sorted(Comparator.comparing(FinancePeriodTransferRuleLine::getLineNo, Comparator.nullsLast(Integer::compareTo))).map(this::toRuleLineVO).toList());
        return vo;
    }

    private FinancePeriodTransferRuleLineVO toRuleLineVO(FinancePeriodTransferRuleLine line) {
        FinancePeriodTransferRuleLineVO vo = new FinancePeriodTransferRuleLineVO();
        vo.setId(line.getId());
        vo.setLineNo(line.getLineNo());
        vo.setSourceSubjectCode(line.getSourceSubjectCode());
        vo.setSourceSubjectName(line.getSourceSubjectName());
        vo.setTargetSubjectCode(line.getTargetSubjectCode());
        vo.setTargetSubjectName(line.getTargetSubjectName());
        vo.setMetricType(line.getMetricType());
        vo.setMetricTypeLabel(resolveMetricTypeLabel(line.getMetricType()));
        vo.setRatio(line.getRatio());
        vo.setAllocationProjectClass(line.getAllocationProjectClass());
        vo.setAllocationProjectId(line.getAllocationProjectId());
        vo.setAllocationProjectName(line.getAllocationProjectName());
        vo.setRemark(line.getRemark());
        return vo;
    }

    private List<FinancePeriodTransferRuleVO> buildDefaultRules(String companyId) {
        Integer defaultSubjectLevel = resolveDefaultProfitSubjectLevel(loadSubjectMap(companyId));
        List<FinancePeriodTransferRuleVO> defaults = new ArrayList<>();
        defaults.add(defaultRuleVO(companyId, RULE_TYPE_PROFIT, "期间损益结转", defaultSubjectLevel));
        defaults.add(defaultRuleVO(companyId, RULE_TYPE_CUSTOM, "自定义比例/公式结转", null));
        FinancePeriodTransferRuleVO manufacture = defaultRuleVO(companyId, RULE_TYPE_MANUFACTURE, "制造费用结转", null);
        manufacture.setSourceSubjectCode("4101");
        manufacture.setSourceSubjectName("制造费用");
        manufacture.setAllocationMode(ALLOCATION_FULL);
        defaults.add(manufacture);
        return defaults;
    }

    private List<FinancePeriodTransferRuleVO> mergeMissingDefaults(String companyId, List<FinancePeriodTransferRuleVO> source) {
        Map<String, FinancePeriodTransferRuleVO> byType = source.stream().collect(Collectors.toMap(FinancePeriodTransferRuleVO::getRuleType, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (FinancePeriodTransferRuleVO item : buildDefaultRules(companyId)) {
            byType.putIfAbsent(item.getRuleType(), item);
        }
        return new ArrayList<>(byType.values());
    }

    private FinancePeriodTransferRuleVO defaultRuleVO(String companyId, String ruleType, String ruleName, Integer defaultSubjectLevel) {
        FinancePeriodTransferRuleVO vo = new FinancePeriodTransferRuleVO();
        vo.setCompanyId(companyId);
        vo.setRuleType(ruleType);
        vo.setRuleTypeLabel(resolveRuleTypeLabel(ruleType));
        vo.setRuleName(ruleName);
        vo.setEnabled(false);
        vo.setVoucherType(DEFAULT_VOUCHER_TYPE);
        vo.setIncludeUnposted(false);
        vo.setTransferGranularity(GRANULARITY_SUBJECT);
        vo.setSubjectLevel(RULE_TYPE_PROFIT.equals(ruleType) ? defaultSubjectLevel : null);
        vo.setAllocationMode(RULE_TYPE_MANUFACTURE.equals(ruleType) ? ALLOCATION_FULL : null);
        vo.setRegenerateStrategy(REGENERATE_STRATEGY);
        return vo;
    }

    private FinanceVoucherSaveDTO buildVoucherSaveDTO(
            String companyId,
            int year,
            int period,
            FinancePeriodTransferRule rule,
            List<FinanceVoucherEntryDTO> entries,
            String operatorName
    ) {
        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId(companyId);
        dto.setIyear(year);
        dto.setIyperiod(year * 100 + period);
        dto.setIperiod(period);
        dto.setCsign(defaultString(rule.getVoucherType(), DEFAULT_VOUCHER_TYPE));
        dto.setDbillDate(LocalDate.of(year, period, 1).withDayOfMonth(LocalDate.of(year, period, 1).lengthOfMonth()).toString());
        dto.setIdoc(0);
        dto.setCbill(operatorName);
        dto.setCtext1("期末结转");
        dto.setCtext2(defaultString(rule.getRuleName(), resolveRuleTypeLabel(rule.getRuleType())));
        dto.setEntries(entries);
        return dto;
    }

    private List<FinanceVoucherEntryDTO> buildVoucherEntriesForRun(
            List<FinancePeriodTransferRunDetail> details,
            FinancePeriodTransferRule rule,
            Map<String, FinanceAccountSubject> subjectMap
    ) {
        List<FinanceVoucherEntryDTO> entries = new ArrayList<>();
        for (FinancePeriodTransferRunDetail detail : details) {
            if (trimToNull(detail.getSkipReason()) != null) {
                continue;
            }
            FinanceVoucherEntryDTO sourceEntry = new FinanceVoucherEntryDTO();
            sourceEntry.setCdigest(buildVoucherDigest(rule, detail));
            sourceEntry.setCcode(detail.getSourceSubjectCode());
            applyAssistDimensions(sourceEntry, decodeAssistKey(detail.getSourceAssistKey()));
            applyDirectionAmount(sourceEntry, reverseDirection(detail.getDirection()), defaultMoney(detail.getAmount()));
            entries.add(sourceEntry);

            FinanceVoucherEntryDTO targetEntry = new FinanceVoucherEntryDTO();
            targetEntry.setCdigest(buildVoucherDigest(rule, detail));
            targetEntry.setCcode(detail.getTargetSubjectCode());
            targetEntry.setCdeptId(detail.getTargetCdeptId());
            targetEntry.setCpersonId(detail.getTargetCpersonId());
            targetEntry.setCcusId(detail.getTargetCcusId());
            targetEntry.setCsupId(detail.getTargetCsupId());
            targetEntry.setCitemClass(detail.getTargetCitemClass());
            targetEntry.setCitemId(detail.getTargetCitemId());
            applyDirectionAmount(targetEntry, detail.getDirection(), defaultMoney(detail.getAmount()));
            entries.add(targetEntry);
        }
        for (FinanceVoucherEntryDTO entry : entries) {
            FinanceAccountSubject subject = subjectMap.get(trimToNull(entry.getCcode()));
            if (subject != null && trimToNull(subject.getCassItem()) != null && trimToNull(entry.getCitemClass()) == null) {
                entry.setCitemClass(subject.getCassItem());
            }
        }
        return entries;
    }

    private void handleRegeneration(String companyId, int year, int period, String ruleType) {
        List<FinancePeriodTransferRun> oldRuns = runMapper.selectList(
                Wrappers.<FinancePeriodTransferRun>lambdaQuery()
                        .eq(FinancePeriodTransferRun::getCompanyId, companyId)
                        .eq(FinancePeriodTransferRun::getIyear, year)
                        .eq(FinancePeriodTransferRun::getIperiod, period)
                        .eq(FinancePeriodTransferRun::getRuleType, ruleType)
                        .eq(FinancePeriodTransferRun::getStatus, STATUS_GENERATED)
                        .orderByDesc(FinancePeriodTransferRun::getGeneratedAt, FinancePeriodTransferRun::getId)
        );
        for (FinancePeriodTransferRun oldRun : oldRuns) {
            String voucherNo = trimToNull(oldRun.getVoucherNo());
            if (voucherNo == null) {
                oldRun.setStatus(STATUS_CANCELLED);
                runMapper.updateById(oldRun);
                continue;
            }
            List<GlAccvouch> rows = loadVoucherRowsByVoucherNo(companyId, voucherNo);
            if (rows.isEmpty()) {
                oldRun.setStatus(STATUS_CANCELLED);
                runMapper.updateById(oldRun);
                continue;
            }
            String status = resolveVoucherStatus(rows.get(0));
            if (!Objects.equals(status, "UNPOSTED")) {
                throw new IllegalStateException("当前期间已存在已审核或已记账的期末结转凭证，请先弃审或取消记账后再重生成");
            }
            deleteVoucherByVoucherNo(companyId, voucherNo);
            oldRun.setStatus(STATUS_CANCELLED);
            oldRun.setBlockedMessage("已被新的期末结转结果替换");
            runMapper.updateById(oldRun);
        }
    }

    private List<GlAccvouch> loadVoucherRowsByVoucherNo(String companyId, String voucherNo) {
        VoucherNoParts parts = parseVoucherNo(voucherNo);
        return glAccvouchMapper.selectList(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIyear, parts.iyear)
                        .eq(GlAccvouch::getIperiod, parts.iperiod)
                        .eq(GlAccvouch::getCsign, parts.csign)
                        .eq(GlAccvouch::getInoId, parts.inoId)
                        .orderByAsc(GlAccvouch::getInid, GlAccvouch::getId)
        );
    }

    private void deleteVoucherByVoucherNo(String companyId, String voucherNo) {
        VoucherNoParts parts = parseVoucherNo(voucherNo);
        glAccvouchMapper.delete(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIyear, parts.iyear)
                        .eq(GlAccvouch::getIperiod, parts.iperiod)
                        .eq(GlAccvouch::getCsign, parts.csign)
                        .eq(GlAccvouch::getInoId, parts.inoId)
        );
    }

    private CalculationContext buildCalculationContext(String companyId, int iyear, int iperiod, boolean includeUnposted) {
        Map<String, FinanceAccountSubject> subjectMap = loadSubjectMap(companyId);
        Map<String, SubjectSnapshot> subjectSnapshots = new LinkedHashMap<>();
        for (GlAccsum row : glAccsumMapper.selectList(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, iyear)
                        .eq(GlAccsum::getIperiod, iperiod)
        )) {
            String subjectCode = trimToNull(row.getCcode());
            FinanceAccountSubject subject = subjectMap.get(subjectCode);
            if (subject == null) {
                continue;
            }
            subjectSnapshots.put(subjectCode, SubjectSnapshot.fromPosted(row, subject));
        }
        Map<String, AssistSnapshot> assistSnapshots = new LinkedHashMap<>();
        for (GlAccass row : glAccassMapper.selectList(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, iyear)
                        .eq(GlAccass::getIperiod, iperiod)
        )) {
            String subjectCode = trimToNull(row.getCcode());
            FinanceAccountSubject subject = subjectMap.get(subjectCode);
            if (subject == null) {
                continue;
            }
            AssistSnapshot snapshot = AssistSnapshot.fromPosted(row, subject);
            assistSnapshots.put(snapshot.key(), snapshot);
        }
        if (includeUnposted) {
            List<GlAccvouch> voucherRows = glAccvouchMapper.selectList(
                    Wrappers.<GlAccvouch>lambdaQuery()
                            .eq(GlAccvouch::getCompanyId, companyId)
                            .eq(GlAccvouch::getIyear, iyear)
                            .eq(GlAccvouch::getIperiod, iperiod)
                            .ne(GlAccvouch::getIbook, 1)
            );
            for (GlAccvouch row : voucherRows) {
                if (Objects.equals(resolveVoucherStatus(row), "ERROR")) {
                    continue;
                }
                String subjectCode = trimToNull(row.getCcode());
                FinanceAccountSubject subject = subjectMap.get(subjectCode);
                if (subject == null) {
                    continue;
                }
                SubjectSnapshot subjectSnapshot = subjectSnapshots.computeIfAbsent(subjectCode, unused -> SubjectSnapshot.empty(subject));
                subjectSnapshot.applyVoucher(row);
                if (hasAssist(row)) {
                    AssistSnapshot assistSnapshot = assistSnapshots.computeIfAbsent(
                            AssistSnapshot.keyOf(row),
                            unused -> AssistSnapshot.empty(subject, row)
                    );
                    assistSnapshot.applyVoucher(row);
                }
            }
        }
        return new CalculationContext(companyId, iyear, iperiod, subjectMap, subjectSnapshots, assistSnapshots);
    }

    private List<AssistSnapshot> collectAssistSnapshots(CalculationContext context, String subjectCode) {
        return context.assistSnapshots.values().stream()
                .filter(item -> Objects.equals(item.subjectCode, trimToNull(subjectCode)))
                .filter(item -> item.amount().compareTo(ZERO) != 0)
                .sorted(Comparator.comparing(AssistSnapshot::key))
                .toList();
    }

    private List<AssistSnapshot> collectAssistSnapshotsByParent(CalculationContext context, String parentSubjectCode) {
        Set<String> subjectCodes = descendantSubjectCodes(context.subjectMap, parentSubjectCode);
        return context.assistSnapshots.values().stream()
                .filter(item -> subjectCodes.contains(item.subjectCode))
                .filter(item -> item.amount().compareTo(ZERO) != 0)
                .sorted(Comparator.comparing(AssistSnapshot::key))
                .toList();
    }

    private SubjectSnapshot collectSubjectSummaryByParent(CalculationContext context, String parentSubjectCode) {
        Set<String> subjectCodes = descendantSubjectCodes(context.subjectMap, parentSubjectCode);
        SubjectSnapshot summary = null;
        for (SubjectSnapshot item : context.subjectSnapshots.values()) {
            if (!subjectCodes.contains(item.subjectCode())) {
                continue;
            }
            if (summary == null) {
                FinanceAccountSubject subject = context.subjectMap.get(item.subjectCode());
                summary = SubjectSnapshot.empty(subject);
            }
            summary.merge(item);
        }
        if (summary == null || summary.amount().compareTo(ZERO) == 0) {
            return null;
        }
        return summary;
    }

    private Set<String> descendantSubjectCodes(Map<String, FinanceAccountSubject> subjectMap, String rootCode) {
        String normalizedRootCode = trimToNull(rootCode);
        if (normalizedRootCode == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (FinanceAccountSubject subject : subjectMap.values()) {
            if (Objects.equals(subject.getSubjectCode(), normalizedRootCode)
                    || (trimToNull(subject.getSubjectCode()) != null && subject.getSubjectCode().startsWith(normalizedRootCode))) {
                result.add(subject.getSubjectCode());
            }
        }
        return result;
    }

    private BigDecimal resolveMetricAmount(AssistSnapshot snapshot, String metricType) {
        return switch (defaultString(metricType, METRIC_ENDING_BALANCE)) {
            case METRIC_CURRENT_DEBIT -> defaultMoney(snapshot.currentDebit);
            case METRIC_CURRENT_CREDIT -> defaultMoney(snapshot.currentCredit);
            case METRIC_CURRENT_NET -> defaultMoney(snapshot.currentDebit).subtract(defaultMoney(snapshot.currentCredit)).abs().setScale(2, RoundingMode.HALF_UP);
            default -> snapshot.amount();
        };
    }

    private FinanceAccountSubject requireLeafSubject(Map<String, FinanceAccountSubject> subjectMap, String subjectCode, String fieldName) {
        String normalizedCode = trimToNull(subjectCode);
        FinanceAccountSubject subject = normalizedCode == null ? null : subjectMap.get(normalizedCode);
        if (subject == null) {
            throw new IllegalArgumentException(fieldName + "不存在");
        }
        if (!isLeaf(subject)) {
            throw new IllegalArgumentException(fieldName + "必须是末级科目");
        }
        return subject;
    }

    private void validateAssistSubset(FinanceAccountSubject source, FinanceAccountSubject target) {
        if (source == null || target == null) {
            return;
        }
        if (isEnabled(target.getBdept()) && !isEnabled(source.getBdept())) {
            throw new IllegalArgumentException("转入科目不能新增部门辅助核算");
        }
        if (isEnabled(target.getBperson()) && !isEnabled(source.getBperson())) {
            throw new IllegalArgumentException("转入科目不能新增人员辅助核算");
        }
        if (isEnabled(target.getBcus()) && !isEnabled(source.getBcus())) {
            throw new IllegalArgumentException("转入科目不能新增客户辅助核算");
        }
        if (isEnabled(target.getBsup()) && !isEnabled(source.getBsup())) {
            throw new IllegalArgumentException("转入科目不能新增供应商辅助核算");
        }
        if (isEnabled(target.getBitem()) && !isEnabled(source.getBitem())) {
            throw new IllegalArgumentException("转入科目不能新增项目辅助核算");
        }
    }

    private void validateMetricType(String metricType) {
        if (!Set.of(METRIC_ENDING_BALANCE, METRIC_CURRENT_DEBIT, METRIC_CURRENT_CREDIT, METRIC_CURRENT_NET).contains(defaultString(metricType, METRIC_ENDING_BALANCE))) {
            throw new IllegalArgumentException("取数指标不合法");
        }
    }

    private SystemCompany resolveEffectiveCompany(Long currentUserId, String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId != null) {
            SystemCompany company = systemCompanyMapper.selectOne(
                    Wrappers.<SystemCompany>lambdaQuery()
                            .eq(SystemCompany::getCompanyId, normalizedCompanyId)
                            .eq(SystemCompany::getStatus, 1)
                            .last("limit 1")
            );
            if (company == null) {
                throw new IllegalArgumentException("当前公司不存在或已停用");
            }
            requireGeneralLedgerEnabled(company.getCompanyId());
            return company;
        }
        User currentUser = currentUserId == null ? null : userMapper.selectById(currentUserId);
        if (currentUser != null && trimToNull(currentUser.getCompanyId()) != null) {
            return resolveEffectiveCompany(null, currentUser.getCompanyId());
        }
        SystemCompany fallback = systemCompanyMapper.selectOne(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
                        .last("limit 1")
        );
        if (fallback == null) {
            throw new IllegalStateException("当前没有可用公司");
        }
        requireGeneralLedgerEnabled(fallback.getCompanyId());
        return fallback;
    }

    private void requireActiveAccountSet(String companyId) {
        requireGeneralLedgerEnabled(companyId);
        FinanceAccountSet accountSet = financeAccountSetMapper.selectOne(
                Wrappers.<FinanceAccountSet>lambdaQuery()
                        .eq(FinanceAccountSet::getCompanyId, companyId)
                        .eq(FinanceAccountSet::getStatus, "ACTIVE")
                        .last("limit 1")
        );
        if (accountSet == null) {
            throw new IllegalStateException("当前公司未启用账套");
        }
    }

    private void requireGeneralLedgerEnabled(String companyId) {
        new FinanceModuleEnableSupport(financeAccountSetModuleEnableMapper)
                .requireEnabled(companyId, FinanceModuleEnableSupport.GENERAL_LEDGER);
    }

    private Map<String, FinanceAccountSubject> loadSubjectMap(String companyId) {
        return accountSubjectMapper.selectList(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, companyId)
                        .eq(FinanceAccountSubject::getStatus, 1)
                        .ne(FinanceAccountSubject::getBclose, 1)
        ).stream().collect(Collectors.toMap(FinanceAccountSubject::getSubjectCode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private List<FinanceVoucherOptionVO> loadAccountOptions(String companyId) {
        return accountSubjectMapper.selectList(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, companyId)
                        .eq(FinanceAccountSubject::getStatus, 1)
                        .ne(FinanceAccountSubject::getBclose, 1)
                        .orderByAsc(FinanceAccountSubject::getSubjectCode)
        ).stream().map(subject -> {
            FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
            option.setValue(subject.getSubjectCode());
            option.setCode(subject.getSubjectCode());
            option.setName(subject.getSubjectName());
            option.setLabel(subject.getSubjectCode() + "  " + defaultString(subject.getSubjectName(), ""));
            option.setSubjectCategory(subject.getSubjectCategory());
            option.setBdept(subject.getBdept());
            option.setBperson(subject.getBperson());
            option.setBcus(subject.getBcus());
            option.setBsup(subject.getBsup());
            option.setBitem(subject.getBitem());
            option.setCassItem(subject.getCassItem());
            option.setLeafFlag(subject.getLeafFlag());
            return option;
        }).toList();
    }

    private List<FinanceVoucherOptionVO> loadProjectClassOptions(String companyId) {
        return projectClassMapper.selectList(
                Wrappers.<FinanceProjectClass>lambdaQuery()
                        .eq(FinanceProjectClass::getCompanyId, companyId)
                        .eq(FinanceProjectClass::getStatus, 1)
                        .orderByAsc(FinanceProjectClass::getSortOrder, FinanceProjectClass::getProjectClassCode)
        ).stream().map(projectClass -> {
            FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
            option.setValue(projectClass.getProjectClassCode());
            option.setCode(projectClass.getProjectClassCode());
            option.setName(projectClass.getProjectClassName());
            option.setLabel(projectClass.getProjectClassCode() + "  " + defaultString(projectClass.getProjectClassName(), ""));
            return option;
        }).toList();
    }

    private List<FinanceVoucherOptionVO> loadProjectOptions(String companyId) {
        return projectArchiveMapper.selectList(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, companyId)
                        .eq(FinanceProjectArchive::getStatus, 1)
                        .ne(FinanceProjectArchive::getBclose, 1)
                        .orderByAsc(FinanceProjectArchive::getCitemccode, FinanceProjectArchive::getSortOrder, FinanceProjectArchive::getCitemcode)
        ).stream().map(project -> {
            FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
            option.setValue(project.getCitemcode());
            option.setCode(project.getCitemcode());
            option.setName(project.getCitemname());
            option.setParentValue(project.getCitemccode());
            option.setLabel(project.getCitemcode() + "  " + defaultString(project.getCitemname(), ""));
            return option;
        }).toList();
    }

    private Map<String, FinanceProjectClass> loadProjectClassMap(String companyId) {
        return projectClassMapper.selectList(
                Wrappers.<FinanceProjectClass>lambdaQuery()
                        .eq(FinanceProjectClass::getCompanyId, companyId)
                        .eq(FinanceProjectClass::getStatus, 1)
        ).stream().collect(Collectors.toMap(FinanceProjectClass::getProjectClassCode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, FinanceProjectArchive> loadProjectMap(String companyId) {
        return projectArchiveMapper.selectList(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, companyId)
                        .eq(FinanceProjectArchive::getStatus, 1)
                        .ne(FinanceProjectArchive::getBclose, 1)
        ).stream().collect(Collectors.toMap(FinanceProjectArchive::getCitemcode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, List<FinancePeriodTransferRuleLine>> loadRuleLines(List<Long> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return Map.of();
        }
        return ruleLineMapper.selectList(
                Wrappers.<FinancePeriodTransferRuleLine>lambdaQuery()
                        .in(FinancePeriodTransferRuleLine::getRuleId, ruleIds)
                        .orderByAsc(FinancePeriodTransferRuleLine::getLineNo, FinancePeriodTransferRuleLine::getId)
        ).stream().collect(Collectors.groupingBy(FinancePeriodTransferRuleLine::getRuleId, LinkedHashMap::new, Collectors.toList()));
    }

    private FinancePeriodTransferRule requireRule(String companyId, Long ruleId) {
        FinancePeriodTransferRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || !Objects.equals(trimToNull(rule.getCompanyId()), trimToNull(companyId))) {
            throw new IllegalStateException("期末结转规则不存在");
        }
        return rule;
    }

    private FinancePeriodTransferRun requireRun(String companyId, Long runId) {
        FinancePeriodTransferRun run = runMapper.selectById(runId);
        if (run == null || !Objects.equals(trimToNull(run.getCompanyId()), trimToNull(companyId))) {
            throw new IllegalStateException("期末结转运行记录不存在");
        }
        return run;
    }

    private void ensurePeriodNotClosed(String companyId, int iyear, int iperiod) {
        Long closedCount = financeVoucherService == null ? 0L : null;
        if (closedCount != null) {
            // no-op marker to avoid accidental field pruning by static cleanup
        }
        // 复用凭证域对已结账期间的写保护口径。
        // 这里只做轻量校验，避免把结转逻辑散落到别的 service。
        Long exists = financeAccountSetMapper.selectCount(
                Wrappers.<FinanceAccountSet>lambdaQuery()
                        .eq(FinanceAccountSet::getCompanyId, companyId)
                        .eq(FinanceAccountSet::getStatus, "ACTIVE")
        );
        if (exists == null || exists <= 0) {
            throw new IllegalStateException("当前公司未启用账套");
        }
        // close-ledger 状态表已存在时，结账期间禁止继续写入。
        // 这里直接查凭证关闭状态，避免依赖其他 service 的 protected 方法。
        Long closeCount = financePeriodCloseMapper.selectCount(
                Wrappers.<FinancePeriodClose>lambdaQuery()
                        .eq(FinancePeriodClose::getCompanyId, companyId)
                        .eq(FinancePeriodClose::getIyear, iyear)
                        .eq(FinancePeriodClose::getIperiod, iperiod)
                        .eq(FinancePeriodClose::getStatus, "CLOSED")
        );
        if (closeCount != null && closeCount > 0) {
            throw new IllegalStateException("当前期间已结账，不能继续执行期末结转");
        }
    }

    private String resolveRuleTypeLabel(String ruleType) {
        return switch (defaultString(ruleType, "")) {
            case RULE_TYPE_PROFIT -> "期间损益结转";
            case RULE_TYPE_CUSTOM -> "自定义比例/公式结转";
            case RULE_TYPE_MANUFACTURE -> "制造费用结转";
            default -> "期末结转";
        };
    }

    private String resolveMetricTypeLabel(String metricType) {
        return switch (defaultString(metricType, METRIC_ENDING_BALANCE)) {
            case METRIC_CURRENT_DEBIT -> "本期借方发生额";
            case METRIC_CURRENT_CREDIT -> "本期贷方发生额";
            case METRIC_CURRENT_NET -> "本期净发生额";
            default -> "期末余额";
        };
    }

    private String resolveRunStatusLabel(String status) {
        return switch (defaultString(status, STATUS_PREVIEWED)) {
            case STATUS_GENERATED -> "已生成";
            case STATUS_CANCELLED -> "已作废";
            default -> "已预览";
        };
    }

    private String resolveDirectionLabel(String direction) {
        return Objects.equals(defaultString(direction, "DEBIT"), "CREDIT") ? "贷方" : "借方";
    }

    private String resolveVoucherStatus(GlAccvouch row) {
        if (Objects.equals(row.getIbook(), 1) || row.getPostedAt() != null) {
            return "POSTED";
        }
        if (Objects.equals(row.getIflag(), 1)) {
            return "ERROR";
        }
        if (trimToNull(row.getCcheck()) != null || row.getCheckedAt() != null) {
            return "REVIEWED";
        }
        return "UNPOSTED";
    }

    private boolean isProfitLike(FinanceAccountSubject subject) {
        String category = trimToNull(subject.getSubjectCategory());
        return Objects.equals(category, "PROFIT") || Objects.equals(category, "COST");
    }

    private boolean hasAssist(GlAccvouch row) {
        return trimToNull(row.getCdeptId()) != null
                || trimToNull(row.getCpersonId()) != null
                || trimToNull(row.getCcusId()) != null
                || trimToNull(row.getCsupId()) != null
                || trimToNull(row.getCitemClass()) != null
                || trimToNull(row.getCitemId()) != null;
    }

    private boolean isLeaf(FinanceAccountSubject subject) {
        return subject != null && (subject.getLeafFlag() == null || subject.getLeafFlag() == 1);
    }

    private Integer resolveDefaultProfitSubjectLevel(Map<String, FinanceAccountSubject> subjectMap) {
        if (subjectMap == null || subjectMap.isEmpty()) {
            return 1;
        }
        return subjectMap.values().stream()
                .map(FinanceAccountSubject::getSubjectLevel)
                .filter(Objects::nonNull)
                .filter(item -> item > 0)
                .max(Integer::compareTo)
                .orElse(1);
    }

    private void applyDefaultProfitSubjectLevel(String companyId, List<FinancePeriodTransferRuleVO> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        Integer defaultSubjectLevel = resolveDefaultProfitSubjectLevel(loadSubjectMap(companyId));
        for (FinancePeriodTransferRuleVO rule : rules) {
            if (rule == null || !RULE_TYPE_PROFIT.equals(rule.getRuleType())) {
                continue;
            }
            if (rule.getSubjectLevel() == null || rule.getSubjectLevel() <= 0) {
                rule.setSubjectLevel(defaultSubjectLevel);
            }
        }
    }

    private Integer normalizeNullableSubjectLevel(Integer subjectLevel) {
        if (subjectLevel == null || subjectLevel <= 0) {
            return null;
        }
        return subjectLevel;
    }

    private void applyDirectionAmount(FinanceVoucherEntryDTO entry, String direction, BigDecimal amount) {
        BigDecimal normalized = defaultMoney(amount);
        if (Objects.equals(direction, "CREDIT")) {
            entry.setMd(ZERO);
            entry.setMc(normalized);
            return;
        }
        entry.setMd(normalized);
        entry.setMc(ZERO);
    }

    private void applyAssistDimensions(FinanceVoucherEntryDTO entry, AssistSnapshot assist) {
        if (entry == null || assist == null) {
            return;
        }
        entry.setCdeptId(assist.cdeptId);
        entry.setCpersonId(assist.cpersonId);
        entry.setCcusId(assist.ccusId);
        entry.setCsupId(assist.csupId);
        entry.setCitemClass(assist.citemClass);
        entry.setCitemId(assist.citemId);
    }

    private String buildVoucherDigest(FinancePeriodTransferRule rule, FinancePeriodTransferRunDetail detail) {
        String ruleName = defaultString(rule.getRuleName(), resolveRuleTypeLabel(rule.getRuleType()));
        String sourceName = defaultString(detail.getSourceSubjectName(), detail.getSourceSubjectCode());
        return ruleName + " " + sourceName;
    }

    private String buildDisplayVoucherNo(String csign, Integer inoId) {
        String number = inoId == null ? "" : String.format(Locale.ROOT, "%04d", inoId);
        return defaultString(csign, DEFAULT_VOUCHER_TYPE) + "-" + number;
    }

    private VoucherNoParts parseVoucherNo(String voucherNo) {
        String[] parts = defaultString(voucherNo, "").split("~");
        if (parts.length != 5) {
            throw new IllegalArgumentException("凭证号格式不正确");
        }
        return new VoucherNoParts(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3], Integer.parseInt(parts[4]));
    }

    private String reverseDirection(String direction) {
        return Objects.equals(defaultString(direction, "DEBIT"), "CREDIT") ? "DEBIT" : "CREDIT";
    }

    private FinanceVoucherOptionVO option(String value, String label) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(value);
        option.setCode(value);
        option.setName(label);
        option.setLabel(label);
        return option;
    }

    private String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("公司主体不能为空");
        }
        return normalized;
    }

    private int normalizeYear(Integer iyear) {
        int value = iyear == null ? LocalDate.now().getYear() : iyear;
        if (value < 2000 || value > 2099) {
            throw new IllegalArgumentException("年度不合法");
        }
        return value;
    }

    private int normalizePeriod(Integer iperiod) {
        int value = iperiod == null ? LocalDate.now().getMonthValue() : iperiod;
        if (value < 1 || value > 12) {
            throw new IllegalArgumentException("期间不合法");
        }
        return value;
    }

    private BigDecimal defaultRatio(BigDecimal ratio) {
        return ratio == null ? BigDecimal.ONE : ratio.setScale(6, RoundingMode.HALF_UP);
    }

    private Integer defaultFlag(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private String defaultString(String value, String fallback) {
        return trimToNull(value) == null ? fallback : trimToNull(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString().replace('T', ' ');
    }

    private String resolveOperatorName(Long currentUserId, String fallbackUsername) {
        User currentUser = currentUserId == null ? null : userMapper.selectById(currentUserId);
        if (currentUser != null && trimToNull(currentUser.getName()) != null) {
            return currentUser.getName().trim();
        }
        if (currentUser != null && trimToNull(currentUser.getUsername()) != null) {
            return currentUser.getUsername().trim();
        }
        return defaultString(fallbackUsername, "system");
    }

    private String encodeAssistKey(AssistSnapshot assist) {
        if (assist == null) {
            return null;
        }
        return String.join("|",
                defaultString(assist.cdeptId, ""),
                defaultString(assist.cpersonId, ""),
                defaultString(assist.ccusId, ""),
                defaultString(assist.csupId, ""),
                defaultString(assist.citemClass, ""),
                defaultString(assist.citemId, "")
        );
    }

    private AssistSnapshot decodeAssistKey(String key) {
        String normalized = trimToNull(key);
        if (normalized == null) {
            return null;
        }
        String[] parts = normalized.split("\\|", -1);
        return AssistSnapshot.synthetic(
                null,
                null,
                emptyToNull(parts, 0),
                emptyToNull(parts, 1),
                emptyToNull(parts, 2),
                emptyToNull(parts, 3),
                emptyToNull(parts, 4),
                emptyToNull(parts, 5),
                ZERO,
                "DEBIT",
                ZERO,
                ZERO
        );
    }

    private String emptyToNull(String[] parts, int index) {
        if (parts == null || index >= parts.length) {
            return null;
        }
        return trimToNull(parts[index]);
    }

    private String sourceAssistLabel(AssistSnapshot assist) {
        if (assist == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        addAssistLabel(parts, "部门", assist.cdeptId);
        addAssistLabel(parts, "人员", assist.cpersonId);
        addAssistLabel(parts, "客户", assist.ccusId);
        addAssistLabel(parts, "供应商", assist.csupId);
        addAssistLabel(parts, "项目分类", assist.citemClass);
        addAssistLabel(parts, "项目", assist.citemId);
        return String.join(" / ", parts);
    }

    private void addAssistLabel(Collection<String> parts, String label, String value) {
        if (trimToNull(value) != null) {
            parts.add(label + ":" + value);
        }
    }

    private String resolveAssistSubjectCode(AssistSnapshot assist) {
        return trimToNull(assist == null ? null : assist.subjectCode);
    }

    private record ProfitSnapshot(
            String subjectCode,
            AssistSnapshot assist,
            BigDecimal amount,
            String direction,
            String metricType,
            String traceKey
    ) {
    }

    private record VoucherNoParts(String companyId, int iyear, int iperiod, String csign, int inoId) {
    }

    private record PreviewBundle(
            List<PreviewRow> rows,
            int generatedCount,
            int skippedCount,
            BigDecimal totalAmount,
            String message
    ) {
    }

    private record CalculationContext(
            String companyId,
            int iyear,
            int iperiod,
            Map<String, FinanceAccountSubject> subjectMap,
            Map<String, SubjectSnapshot> subjectSnapshots,
            Map<String, AssistSnapshot> assistSnapshots
    ) {
    }

    private static final class PreviewRow {
        private Long persistedId;
        private Integer lineNo;
        private Long ruleLineId;
        private String sourceSubjectCode;
        private String sourceSubjectName;
        private AssistSnapshot sourceAssist;
        private String targetSubjectCode;
        private String targetSubjectName;
        private String targetCdeptId;
        private String targetCpersonId;
        private String targetCcusId;
        private String targetCsupId;
        private String targetCitemClass;
        private String targetCitemId;
        private String metricType;
        private String direction;
        private BigDecimal amount;
        private String skipReason;
        private String sourceTraceKey;

        private void setPersistedId(Long persistedId) {
            this.persistedId = persistedId;
        }
    }

    private static final class ProfitAccumulator {
        private final String subjectCode;
        private final String subjectName;
        private final boolean assistMode;
        private final String cdeptId;
        private final String cpersonId;
        private final String ccusId;
        private final String csupId;
        private final String citemClass;
        private final String citemId;
        private BigDecimal currentDebit;
        private BigDecimal currentCredit;

        private ProfitAccumulator(
                String subjectCode,
                String subjectName,
                boolean assistMode,
                String cdeptId,
                String cpersonId,
                String ccusId,
                String csupId,
                String citemClass,
                String citemId
        ) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.assistMode = assistMode;
            this.cdeptId = cdeptId;
            this.cpersonId = cpersonId;
            this.ccusId = ccusId;
            this.csupId = csupId;
            this.citemClass = citemClass;
            this.citemId = citemId;
            this.currentDebit = ZERO;
            this.currentCredit = ZERO;
        }

        private static ProfitAccumulator create(FinanceAccountSubject subject, GlAccvouch row, boolean assistMode) {
            return new ProfitAccumulator(
                    subject.getSubjectCode(),
                    subject.getSubjectName(),
                    assistMode,
                    assistMode ? trim(row.getCdeptId()) : null,
                    assistMode ? trim(row.getCpersonId()) : null,
                    assistMode ? trim(row.getCcusId()) : null,
                    assistMode ? trim(row.getCsupId()) : null,
                    assistMode ? trim(row.getCitemClass()) : null,
                    assistMode ? trim(row.getCitemId()) : null
            );
        }

        private void apply(GlAccvouch row) {
            BigDecimal debit = FinanceVoucherAmountSupport.effectiveDebit(row.getMd(), row.getMc());
            BigDecimal credit = FinanceVoucherAmountSupport.effectiveCredit(row.getMd(), row.getMc());
            this.currentDebit = money(this.currentDebit.add(debit));
            this.currentCredit = money(this.currentCredit.add(credit));
        }

        private ProfitSnapshot toSnapshot() {
            BigDecimal net = money(currentDebit.subtract(currentCredit));
            if (net.compareTo(ZERO) == 0) {
                return null;
            }
            String direction = net.compareTo(ZERO) >= 0 ? "DEBIT" : "CREDIT";
            BigDecimal amount = money(net.abs());
            AssistSnapshot assist = AssistSnapshot.synthetic(
                    subjectCode,
                    subjectName,
                    assistMode ? cdeptId : null,
                    assistMode ? cpersonId : null,
                    assistMode ? ccusId : null,
                    assistMode ? csupId : null,
                    assistMode ? citemClass : null,
                    assistMode ? citemId : null,
                    amount,
                    direction,
                    currentDebit,
                    currentCredit
            );
            String traceKey = assistMode
                    ? assist.key()
                    : subjectCode;
            return new ProfitSnapshot(subjectCode, assistMode ? assist : null, amount, direction, METRIC_CURRENT_NET, traceKey);
        }

        private static String assistKeyOf(GlAccvouch row) {
            return AssistSnapshot.keyOf(
                    trim(row.getCcode()),
                    trim(row.getCdeptId()),
                    trim(row.getCpersonId()),
                    trim(row.getCcusId()),
                    trim(row.getCsupId()),
                    trim(row.getCitemClass()),
                    trim(row.getCitemId())
            );
        }
    }

    private static final class SubjectSnapshot {
        private final String subjectCode;
        private final String subjectName;
        private final FinanceAccountSubject subject;
        private BigDecimal currentDebit;
        private BigDecimal currentCredit;
        private BigDecimal endingSigned;

        private SubjectSnapshot(FinanceAccountSubject subject, BigDecimal currentDebit, BigDecimal currentCredit, BigDecimal endingSigned) {
            this.subjectCode = subject == null ? null : subject.getSubjectCode();
            this.subjectName = subject == null ? null : subject.getSubjectName();
            this.subject = subject;
            this.currentDebit = currentDebit;
            this.currentCredit = currentCredit;
            this.endingSigned = endingSigned;
        }

        private static SubjectSnapshot fromPosted(GlAccsum row, FinanceAccountSubject subject) {
            BigDecimal ending = signedEnding(row.getMe(), row.getCenddC(), subject);
            return new SubjectSnapshot(subject, money(row.getMd()), money(row.getMc()), ending);
        }

        private static SubjectSnapshot empty(FinanceAccountSubject subject) {
            return new SubjectSnapshot(subject, ZERO, ZERO, ZERO);
        }

        private void applyVoucher(GlAccvouch row) {
            BigDecimal debit = FinanceVoucherAmountSupport.effectiveDebit(row.getMd(), row.getMc());
            BigDecimal credit = FinanceVoucherAmountSupport.effectiveCredit(row.getMd(), row.getMc());
            this.currentDebit = money(this.currentDebit.add(debit));
            this.currentCredit = money(this.currentCredit.add(credit));
            this.endingSigned = money(this.endingSigned.add(debit.subtract(credit)));
        }

        private void merge(SubjectSnapshot other) {
            this.currentDebit = money(this.currentDebit.add(other.currentDebit));
            this.currentCredit = money(this.currentCredit.add(other.currentCredit));
            this.endingSigned = money(this.endingSigned.add(other.endingSigned));
        }

        private BigDecimal amount() {
            return money(this.endingSigned.abs());
        }

        private String direction() {
            return this.endingSigned.compareTo(ZERO) >= 0 ? "DEBIT" : "CREDIT";
        }

        private String subjectCode() {
            return subjectCode;
        }

        private AssistSnapshot asAssistSnapshot() {
            return AssistSnapshot.synthetic(subjectCode, subjectName, null, null, null, null, null, null, amount(), direction(), currentDebit, currentCredit);
        }
    }

    private static final class AssistSnapshot {
        private final String subjectCode;
        private final String subjectName;
        private final String cdeptId;
        private final String cpersonId;
        private final String ccusId;
        private final String csupId;
        private final String citemClass;
        private final String citemId;
        private BigDecimal amount;
        private String direction;
        private BigDecimal currentDebit;
        private BigDecimal currentCredit;
        private BigDecimal endingSigned;

        private AssistSnapshot(
                String subjectCode,
                String subjectName,
                String cdeptId,
                String cpersonId,
                String ccusId,
                String csupId,
                String citemClass,
                String citemId,
                BigDecimal amount,
                String direction,
                BigDecimal currentDebit,
                BigDecimal currentCredit,
                BigDecimal endingSigned
        ) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.cdeptId = cdeptId;
            this.cpersonId = cpersonId;
            this.ccusId = ccusId;
            this.csupId = csupId;
            this.citemClass = citemClass;
            this.citemId = citemId;
            this.amount = amount;
            this.direction = direction;
            this.currentDebit = currentDebit;
            this.currentCredit = currentCredit;
            this.endingSigned = endingSigned;
        }

        private static AssistSnapshot fromPosted(GlAccass row, FinanceAccountSubject subject) {
            BigDecimal ending = signedEnding(row.getMe(), row.getCenddC(), subject);
            return new AssistSnapshot(
                    subject.getSubjectCode(),
                    subject.getSubjectName(),
                    trim(row.getCdeptId()),
                    trim(row.getCpersonId()),
                    trim(row.getCcusId()),
                    trim(row.getCsupId()),
                    trim(row.getCitemClass()),
                    trim(row.getCitemId()),
                    money(ending.abs()),
                    ending.compareTo(ZERO) >= 0 ? "DEBIT" : "CREDIT",
                    money(row.getMd()),
                    money(row.getMc()),
                    ending
            );
        }

        private static AssistSnapshot empty(FinanceAccountSubject subject, GlAccvouch row) {
            return new AssistSnapshot(
                    subject.getSubjectCode(),
                    subject.getSubjectName(),
                    trim(row.getCdeptId()),
                    trim(row.getCpersonId()),
                    trim(row.getCcusId()),
                    trim(row.getCsupId()),
                    trim(row.getCitemClass()),
                    trim(row.getCitemId()),
                    ZERO,
                    "DEBIT",
                    ZERO,
                    ZERO,
                    ZERO
            );
        }

        private static AssistSnapshot synthetic(
                String subjectCode,
                String subjectName,
                String cdeptId,
                String cpersonId,
                String ccusId,
                String csupId,
                String citemClass,
                String citemId,
                BigDecimal amount,
                String direction,
                BigDecimal currentDebit,
                BigDecimal currentCredit
        ) {
            BigDecimal endingSigned = "CREDIT".equals(direction) ? money(amount).negate() : money(amount);
            return new AssistSnapshot(subjectCode, subjectName, cdeptId, cpersonId, ccusId, csupId, citemClass, citemId, money(amount), direction, money(currentDebit), money(currentCredit), endingSigned);
        }

        private void applyVoucher(GlAccvouch row) {
            BigDecimal debit = FinanceVoucherAmountSupport.effectiveDebit(row.getMd(), row.getMc());
            BigDecimal credit = FinanceVoucherAmountSupport.effectiveCredit(row.getMd(), row.getMc());
            this.currentDebit = money(this.currentDebit.add(debit));
            this.currentCredit = money(this.currentCredit.add(credit));
            this.endingSigned = money(this.endingSigned.add(debit.subtract(credit)));
            this.amount = money(this.endingSigned.abs());
            this.direction = this.endingSigned.compareTo(ZERO) >= 0 ? "DEBIT" : "CREDIT";
        }

        private String key() {
            return keyOf(subjectCode, cdeptId, cpersonId, ccusId, csupId, citemClass, citemId);
        }

        private BigDecimal amount() {
            return money(this.amount);
        }

        private String direction() {
            return this.direction;
        }

        private static String keyOf(GlAccvouch row) {
            return keyOf(trim(row.getCcode()), trim(row.getCdeptId()), trim(row.getCpersonId()), trim(row.getCcusId()), trim(row.getCsupId()), trim(row.getCitemClass()), trim(row.getCitemId()));
        }

        private static String keyOf(String subjectCode, String cdeptId, String cpersonId, String ccusId, String csupId, String citemClass, String citemId) {
            return String.join("#",
                    defaultPart(subjectCode),
                    defaultPart(cdeptId),
                    defaultPart(cpersonId),
                    defaultPart(ccusId),
                    defaultPart(csupId),
                    defaultPart(citemClass),
                    defaultPart(citemId)
            );
        }

        private static String defaultPart(String value) {
            return value == null ? "" : value;
        }
    }

    private static BigDecimal signedEnding(BigDecimal ending, String endDirection, FinanceAccountSubject subject) {
        BigDecimal amount = money(ending);
        if (amount.compareTo(ZERO) == 0) {
            return ZERO;
        }
        String direction = trim(endDirection);
        if (direction == null) {
            direction = resolveSubjectBalanceDirection(subject);
        }
        if (isCreditDirection(direction)) {
            return amount.negate();
        }
        return amount;
    }

    private static String resolveSubjectBalanceDirection(FinanceAccountSubject subject) {
        return trim(subject == null ? null : subject.getBalanceDirection());
    }

    private static boolean isCreditDirection(String direction) {
        String normalized = trim(direction);
        return Objects.equals(normalized, "贷") || Objects.equals(normalized, "CREDIT");
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
