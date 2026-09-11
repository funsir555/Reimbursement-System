package com.finex.auth.service.impl.closeledger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceCloseLedgerCheckItemVO;
import com.finex.auth.dto.FinanceCloseLedgerMetaVO;
import com.finex.auth.dto.FinanceCloseLedgerReconcileResultVO;
import com.finex.auth.dto.FinanceCloseLedgerValidationResultVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinancePeriodClose;
import com.finex.auth.entity.FinancePeriodCloseLog;
import com.finex.auth.entity.FinancePostVoucherState;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinancePeriodCloseLogMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.FinanceBalanceRowSupport;
import com.finex.auth.support.FinanceModuleEnableSupport;
import com.finex.auth.support.FinanceVoucherAmountSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractFinanceCloseLedgerSupport {

    protected static final String ACCOUNT_SET_STATUS_ACTIVE = "ACTIVE";
    protected static final String CLOSE_STATUS_CLOSED = "CLOSED";
    protected static final String POST_STATUS_NOT_POSTED = "NOT_POSTED";
    protected static final String POST_STATUS_FULLY_POSTED = "FULLY_POSTED";
    protected static final String VOUCHER_STATUS_UNPOSTED = "UNPOSTED";
    protected static final String VOUCHER_STATUS_REVIEWED = "REVIEWED";
    protected static final String VOUCHER_STATUS_ERROR = "ERROR";
    protected static final String VOUCHER_STATUS_POSTED = "POSTED";
    protected static final String VOUCHER_STATUS_VOIDED = "VOIDED";
    protected static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    protected static final BigDecimal ZERO_QTY = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemCompanyMapper systemCompanyMapper;
    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final FinancePostVoucherStateMapper financePostVoucherStateMapper;
    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final FinancePeriodCloseMapper financePeriodCloseMapper;
    private final FinancePeriodCloseLogMapper financePeriodCloseLogMapper;
    private final GlAccvouchMapper glAccvouchMapper;
    private final GlAccsumMapper glAccsumMapper;
    private final GlAccassMapper glAccassMapper;
    private final UserMapper userMapper;
    private final FinanceModuleEnableSupport financeModuleEnableSupport;

    protected AbstractFinanceCloseLedgerSupport(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            FinancePeriodCloseLogMapper financePeriodCloseLogMapper,
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            UserMapper userMapper,
            FinanceModuleEnableSupport financeModuleEnableSupport
    ) {
        this.systemCompanyMapper = systemCompanyMapper;
        this.financeAccountSetMapper = financeAccountSetMapper;
        this.financePostVoucherStateMapper = financePostVoucherStateMapper;
        this.financeAccountSubjectMapper = financeAccountSubjectMapper;
        this.financePeriodCloseMapper = financePeriodCloseMapper;
        this.financePeriodCloseLogMapper = financePeriodCloseLogMapper;
        this.glAccvouchMapper = glAccvouchMapper;
        this.glAccsumMapper = glAccsumMapper;
        this.glAccassMapper = glAccassMapper;
        this.userMapper = userMapper;
        this.financeModuleEnableSupport = financeModuleEnableSupport;
    }

    protected SystemCompany resolveEffectiveCompany(Long currentUserId, String companyId) {
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
        String currentUserCompanyId = trimToNull(currentUser == null ? null : currentUser.getCompanyId());
        if (currentUserCompanyId != null) {
            return resolveEffectiveCompany(null, currentUserCompanyId);
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
        return fallback;
    }

    protected int normalizeYear(Integer iyear) {
        int value = iyear == null ? LocalDate.now().getYear() : iyear;
        if (value < 2000 || value > 2099) {
            throw new IllegalArgumentException("年度不合法");
        }
        return value;
    }

    protected int normalizePeriod(Integer iperiod) {
        int value = iperiod == null ? LocalDate.now().getMonthValue() : iperiod;
        if (value < 1 || value > 12) {
            throw new IllegalArgumentException("期间不合法");
        }
        return value;
    }

    protected int buildYearPeriod(int iyear, int iperiod) {
        return iyear * 100 + iperiod;
    }

    protected String resolveOperatorName(Long currentUserId, String fallbackUsername) {
        User currentUser = currentUserId == null ? null : userMapper.selectById(currentUserId);
        String displayName = trimToNull(currentUser == null ? null : currentUser.getName());
        if (displayName != null) {
            return displayName;
        }
        String username = trimToNull(currentUser == null ? null : currentUser.getUsername());
        if (username != null) {
            return username;
        }
        return trimToNull(fallbackUsername) == null ? "system" : trimToNull(fallbackUsername);
    }

    protected FinanceAccountSet requireActiveAccountSet(String companyId) {
        requireGeneralLedgerEnabled(companyId);
        FinanceAccountSet accountSet = financeAccountSetMapper.selectOne(
                Wrappers.<FinanceAccountSet>lambdaQuery()
                        .eq(FinanceAccountSet::getCompanyId, companyId)
                        .eq(FinanceAccountSet::getStatus, ACCOUNT_SET_STATUS_ACTIVE)
                        .last("limit 1")
        );
        if (accountSet == null) {
            throw new IllegalStateException("当前公司未启用账套");
        }
        return accountSet;
    }

    protected void requireGeneralLedgerEnabled(String companyId) {
        financeModuleEnableSupport.requireEnabled(companyId, FinanceModuleEnableSupport.GENERAL_LEDGER);
    }

    protected Map<String, FinanceAccountSubject> loadSubjectMap(String companyId) {
        return financeAccountSubjectMapper.selectList(
                        Wrappers.<FinanceAccountSubject>lambdaQuery()
                                .eq(FinanceAccountSubject::getCompanyId, companyId)
                ).stream()
                .collect(Collectors.toMap(FinanceAccountSubject::getSubjectCode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    protected FinancePeriodClose findPeriodClose(String companyId, int iyear, int iperiod) {
        return financePeriodCloseMapper.selectOne(
                Wrappers.<FinancePeriodClose>lambdaQuery()
                        .eq(FinancePeriodClose::getCompanyId, companyId)
                        .eq(FinancePeriodClose::getIyear, iyear)
                        .eq(FinancePeriodClose::getIperiod, iperiod)
                        .last("limit 1")
        );
    }

    protected FinancePeriodClose findLatestClosedPeriod(String companyId) {
        return financePeriodCloseMapper.selectOne(
                Wrappers.<FinancePeriodClose>lambdaQuery()
                        .eq(FinancePeriodClose::getCompanyId, companyId)
                        .eq(FinancePeriodClose::getStatus, CLOSE_STATUS_CLOSED)
                        .orderByDesc(FinancePeriodClose::getIyperiod)
                        .last("limit 1")
        );
    }

    protected FinancePostVoucherState findPostState(String companyId, int iyear, int iperiod) {
        return financePostVoucherStateMapper.selectOne(
                Wrappers.<FinancePostVoucherState>lambdaQuery()
                        .eq(FinancePostVoucherState::getCompanyId, companyId)
                        .eq(FinancePostVoucherState::getIyear, iyear)
                        .eq(FinancePostVoucherState::getIperiod, iperiod)
                        .last("limit 1")
        );
    }

    protected Map<VoucherKey, List<GlAccvouch>> loadVoucherGroups(String companyId, int iyear, int iperiod) {
        return glAccvouchMapper.selectList(
                        Wrappers.<GlAccvouch>lambdaQuery()
                                .eq(GlAccvouch::getCompanyId, companyId)
                                .eq(GlAccvouch::getIyear, iyear)
                                .eq(GlAccvouch::getIperiod, iperiod)
                                .orderByAsc(GlAccvouch::getInoId, GlAccvouch::getInid, GlAccvouch::getId)
                ).stream()
                .collect(Collectors.groupingBy(
                        row -> new VoucherKey(row.getCompanyId(), row.getIyear(), row.getIperiod(), row.getCsign(), row.getInoId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    protected VoucherCounts summarizeVoucherGroups(Map<VoucherKey, List<GlAccvouch>> groups) {
        List<VoucherKey> unposted = new ArrayList<>();
        List<VoucherKey> reviewed = new ArrayList<>();
        List<VoucherKey> error = new ArrayList<>();
        List<VoucherKey> posted = new ArrayList<>();
        for (Map.Entry<VoucherKey, List<GlAccvouch>> entry : groups.entrySet()) {
            String status = resolveVoucherStatus(entry.getValue().get(0));
            switch (status) {
                case VOUCHER_STATUS_POSTED -> posted.add(entry.getKey());
                case VOUCHER_STATUS_ERROR -> error.add(entry.getKey());
                case VOUCHER_STATUS_REVIEWED -> reviewed.add(entry.getKey());
                default -> unposted.add(entry.getKey());
            }
        }
        return new VoucherCounts(unposted, reviewed, error, posted);
    }

    protected String resolveVoucherStatus(GlAccvouch row) {
        if (Objects.equals(row.getVoidFlag(), 1) || row.getVoidedAt() != null) {
            return VOUCHER_STATUS_VOIDED;
        }
        if (Objects.equals(row.getIbook(), 1) || row.getPostedAt() != null) {
            return VOUCHER_STATUS_POSTED;
        }
        if (Objects.equals(row.getIflag(), 1)) {
            return VOUCHER_STATUS_ERROR;
        }
        if (trimToNull(row.getCcheck()) != null || row.getCheckedAt() != null) {
            return VOUCHER_STATUS_REVIEWED;
        }
        return VOUCHER_STATUS_UNPOSTED;
    }

    protected FinanceCloseLedgerMetaVO buildMeta(
            SystemCompany company,
            int iyear,
            int iperiod,
            VoucherCounts counts,
            FinancePostVoucherState postState,
            FinancePeriodClose close,
            CloseLedgerExternalCheckResult fixedAssets
    ) {
        FinanceCloseLedgerMetaVO meta = new FinanceCloseLedgerMetaVO();
        meta.setCompanyId(company.getCompanyId());
        meta.setCompanyName(company.getCompanyName());
        meta.setIyear(iyear);
        meta.setIperiod(iperiod);
        meta.setIyperiod(buildYearPeriod(iyear, iperiod));
        meta.setPeriodLabel(iyear + "-" + String.format(Locale.ROOT, "%02d", iperiod));
        meta.setStatus(close != null && CLOSE_STATUS_CLOSED.equals(trimToNull(close.getStatus())) ? CLOSE_STATUS_CLOSED : "OPEN");
        meta.setStatusLabel(close != null && CLOSE_STATUS_CLOSED.equals(trimToNull(close.getStatus())) ? "已结账" : "未结账");
        meta.setCloseNote(close == null ? null : trimToNull(close.getCloseNote()));
        meta.setClosedBy(close == null ? null : trimToNull(close.getClosedBy()));
        meta.setClosedAt(formatDateTime(close == null ? null : close.getClosedAt()));
        meta.setPostStatus(trimToNull(postState == null ? null : postState.getStatus()) == null ? "NOT_POSTED" : postState.getStatus());
        meta.setPostStatusLabel(resolvePostStatusLabel(meta.getPostStatus()));
        meta.setUnpostedVoucherCount(counts.unpostedCount());
        meta.setReviewedVoucherCount(counts.reviewedCount());
        meta.setErrorVoucherCount(counts.errorCount());
        meta.setPostedVoucherCount(counts.postedCount());
        meta.setFixedAssetClosed(fixedAssets.passed());
        meta.setFixedAssetStatusLabel(fixedAssets.message());
        return meta;
    }

    protected FinanceCloseLedgerMetaVO buildMetaAfterClose(String companyId, int iyear, int iperiod) {
        SystemCompany company = resolveEffectiveCompany(null, companyId);
        VoucherCounts counts = summarizeVoucherGroups(loadVoucherGroups(companyId, iyear, iperiod));
        FinancePostVoucherState postState = findPostState(companyId, iyear, iperiod);
        FinancePeriodClose close = findPeriodClose(companyId, iyear, iperiod);
        return buildMeta(
                company,
                iyear,
                iperiod,
                counts,
                postState,
                close,
                new CloseLedgerExternalCheckResult("fixed_assets", "固定资产期间结账", true, "固定资产已完成期间结账")
        );
    }

    protected FinanceCloseLedgerReconcileResultVO reconcilePeriod(String companyId, int iyear, int iperiod) {
        Map<MovementKey, MovementTotals> voucherSubjectMap = buildVoucherSubjectMap(companyId, iyear, iperiod);
        Map<MovementKey, MovementTotals> sumMap = buildAccsumMap(companyId, iyear, iperiod);
        Map<AssistKey, MovementTotals> voucherAssistMap = buildVoucherAssistMap(companyId, iyear, iperiod);
        Map<AssistKey, MovementTotals> assistMap = buildAccassMap(companyId, iyear, iperiod);

        List<String> differenceSubjects = new ArrayList<>();
        List<String> differenceAssistKeys = new ArrayList<>();
        List<String> missingAssistSubjects = new ArrayList<>();
        List<String> illegalAssistMessages = validateAssistRows(companyId, iyear, iperiod);

        Set<MovementKey> subjectKeys = new LinkedHashSet<>();
        subjectKeys.addAll(voucherSubjectMap.keySet());
        subjectKeys.addAll(sumMap.keySet());
        for (MovementKey key : subjectKeys) {
            if (!Objects.equals(voucherSubjectMap.getOrDefault(key, MovementTotals.ZERO), sumMap.getOrDefault(key, MovementTotals.ZERO))) {
                differenceSubjects.add(key.label());
            }
        }

        Set<AssistKey> assistKeys = new LinkedHashSet<>();
        assistKeys.addAll(voucherAssistMap.keySet());
        assistKeys.addAll(assistMap.keySet());
        for (AssistKey key : assistKeys) {
            MovementTotals expected = voucherAssistMap.getOrDefault(key, MovementTotals.ZERO);
            MovementTotals actual = assistMap.getOrDefault(key, MovementTotals.ZERO);
            if (!Objects.equals(expected, actual)) {
                differenceAssistKeys.add(key.label());
                if (!assistMap.containsKey(key)) {
                    missingAssistSubjects.add(key.subjectLabel());
                }
            }
        }

        FinanceCloseLedgerReconcileResultVO result = new FinanceCloseLedgerReconcileResultVO();
        result.setDifferenceSubjects(differenceSubjects);
        result.setDifferenceAssistKeys(differenceAssistKeys);
        result.setMissingAssistSubjects(missingAssistSubjects);
        result.setIllegalAssistMessages(illegalAssistMessages);
        result.setDifferenceSubjectCount(differenceSubjects.size());
        result.setDifferenceAssistCount(differenceAssistKeys.size());
        result.setMissingAssistCount(missingAssistSubjects.size());
        result.setIllegalAssistCount(illegalAssistMessages.size());
        boolean passed = differenceSubjects.isEmpty()
                && differenceAssistKeys.isEmpty()
                && missingAssistSubjects.isEmpty()
                && illegalAssistMessages.isEmpty();
        result.setPassed(passed);
        result.setSummaryMessage(passed
                ? "总账、辅助账与当前期间凭证发生额核对一致"
                : "总账结账前对账未通过，请先处理差异后再继续");
        return result;
    }

    protected FinanceCloseLedgerValidationResultVO validateBeforeClose(
            String companyId,
            int iyear,
            int iperiod,
            FinanceCloseLedgerReconcileResultVO reconcileResult,
            List<CloseLedgerExternalCheckResult> externalResults
    ) {
        requireActiveAccountSet(companyId);
        VoucherCounts counts = summarizeVoucherGroups(loadVoucherGroups(companyId, iyear, iperiod));
        FinancePostVoucherState postState = findPostState(companyId, iyear, iperiod);
        FinancePeriodClose close = findPeriodClose(companyId, iyear, iperiod);

        List<FinanceCloseLedgerCheckItemVO> generalChecks = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>();
        boolean emptyPeriod = counts.totalCount() == 0;

        generalChecks.add(buildCheck("locked_after_close", "结账后本月不能再填制凭证", true, "结账成功后当前期间总账写操作将被锁定"));

        boolean alreadyClosed = close != null && CLOSE_STATUS_CLOSED.equals(trimToNull(close.getStatus()));
        generalChecks.add(buildCheck("already_closed", "当前期间不能重复结账", !alreadyClosed, alreadyClosed ? "当前期间已结账，无需重复执行" : "当前期间尚未结账"));
        if (alreadyClosed) {
            blockingReasons.add("当前期间已结账");
        }

        boolean noUnposted = counts.unpostedCount() == 0;
        generalChecks.add(buildCheck(
                "no_unposted",
                "没有未审核凭证才能结账",
                noUnposted,
                noUnposted
                        ? (emptyPeriod ? "当前期间无凭证，未审核凭证视为已清零" : "当前期间不存在未审核凭证")
                        : "还有未审核的凭证不能结账"
        ));
        if (!noUnposted) {
            blockingReasons.add("还有未审核的凭证不能结账");
        }

        boolean noReviewed = counts.reviewedCount() == 0;
        generalChecks.add(buildCheck(
                "no_reviewed",
                "没有未记账凭证才能结账",
                noReviewed,
                noReviewed
                        ? (emptyPeriod ? "当前期间无凭证，视为已满足记账前置条件" : "当前期间不存在已审核未记账凭证")
                        : "还有未记账的凭证不能结账"
        ));
        if (!noReviewed) {
            blockingReasons.add("还有未记账的凭证不能结账");
        }

        boolean noError = counts.errorCount() == 0;
        generalChecks.add(buildCheck(
                "no_error",
                "错误凭证处理完成后才能结账",
                noError,
                noError
                        ? (emptyPeriod ? "当前期间无凭证，错误凭证视为已清零" : "当前期间不存在错误凭证")
                        : "当前期间存在错误凭证，不能结账"
        ));
        if (!noError) {
            blockingReasons.add("当前期间存在错误凭证，不能结账");
        }

        boolean reconcilePassed = reconcileResult != null && Boolean.TRUE.equals(reconcileResult.getPassed());
        generalChecks.add(buildCheck("reconcile_passed", "每月对账正确后才能结账", reconcilePassed, reconcilePassed ? "当前期间总账对账通过" : trimToNull(reconcileResult == null ? null : reconcileResult.getSummaryMessage())));
        if (!reconcilePassed) {
            blockingReasons.add("每月对账正确后才能结账");
        }

        NextPeriodCarryForwardPlan nextPeriodPlan = inspectNextPeriodCarryForward(companyId, iyear, iperiod);
        boolean nextPeriodCarryForwardReady = nextPeriodPlan.status() != NextPeriodCarryForwardStatus.BLOCKED;
        generalChecks.add(buildCheck(
                "next_period_rollup_ready",
                "下一期间可接收本期滚转余额",
                nextPeriodCarryForwardReady,
                nextPeriodPlan.message()
        ));
        if (!nextPeriodCarryForwardReady) {
            blockingReasons.add(nextPeriodPlan.message());
        }

        List<FinanceCloseLedgerCheckItemVO> externalChecks = externalResults.stream()
                .map(item -> buildCheck(item.code(), item.label(), item.passed(), item.message()))
                .toList();
        for (CloseLedgerExternalCheckResult item : externalResults) {
            if (!item.passed()) {
                blockingReasons.add(item.message());
            }
        }

        FinanceCloseLedgerValidationResultVO result = new FinanceCloseLedgerValidationResultVO();
        result.setAlreadyClosed(alreadyClosed);
        result.setReconcilePassed(reconcilePassed);
        result.setPostStatus(trimToNull(postState == null ? null : postState.getStatus()) == null ? "NOT_POSTED" : postState.getStatus());
        result.setPostStatusLabel(resolvePostStatusLabel(result.getPostStatus()));
        result.setGeneralChecks(generalChecks);
        result.setExternalChecks(externalChecks);
        result.setBlockingReasons(blockingReasons.stream().distinct().toList());
        result.setGeneralPassed(generalChecks.stream().allMatch(item -> Boolean.TRUE.equals(item.getPassed())));
        result.setExternalPassed(externalChecks.stream().allMatch(item -> Boolean.TRUE.equals(item.getPassed())));
        result.setPassed(Boolean.TRUE.equals(result.getGeneralPassed()) && Boolean.TRUE.equals(result.getExternalPassed()));
        return result;
    }

    protected void closePeriod(
            String companyId,
            int iyear,
            int iperiod,
            String closeNote,
            String operatorName,
            FinanceCloseLedgerReconcileResultVO reconcileResult
    ) {
        CarryForwardResult carryForwardResult = carryForwardBalances(companyId, iyear, iperiod);
        FinancePeriodClose close = findPeriodClose(companyId, iyear, iperiod);
        if (close == null) {
            close = new FinancePeriodClose();
            close.setCompanyId(companyId);
            close.setIyear(iyear);
            close.setIperiod(iperiod);
            close.setIyperiod(buildYearPeriod(iyear, iperiod));
            close.setCreatedAt(LocalDateTime.now());
        }
        close.setStatus(CLOSE_STATUS_CLOSED);
        close.setClosedBy(operatorName);
        close.setClosedAt(LocalDateTime.now());
        close.setCloseNote(trimToNull(closeNote));
        close.setUpdatedAt(LocalDateTime.now());
        if (close.getId() == null) {
            financePeriodCloseMapper.insert(close);
        } else {
            financePeriodCloseMapper.updateById(close);
        }
        insertLog(companyId, iyear, iperiod, "CLOSE_SUCCESS", "SUCCESS", operatorName, "总账期间结账成功",
                "{\"differenceSubjectCount\":" + safeInt(reconcileResult == null ? null : reconcileResult.getDifferenceSubjectCount())
                        + ",\"differenceAssistCount\":" + safeInt(reconcileResult == null ? null : reconcileResult.getDifferenceAssistCount()) + "}");
        insertLog(
                companyId,
                iyear,
                iperiod,
                "CARRY_FORWARD",
                "SUCCESS",
                operatorName,
                "期末余额已滚转到下一期间",
                "{\"nextIyperiod\":" + carryForwardResult.nextIyperiod()
                        + ",\"glAccsumCount\":" + carryForwardResult.glAccsumCount()
                        + ",\"glAccassCount\":" + carryForwardResult.glAccassCount() + "}"
        );
        if (trimToNull(closeNote) != null) {
            insertLog(companyId, iyear, iperiod, "NOTE", "SUCCESS", operatorName, closeNote.trim(), null);
        }
    }

    protected CarryForwardResult carryForwardBalances(String companyId, int iyear, int iperiod) {
        NextPeriodCarryForwardPlan nextPeriodPlan = inspectNextPeriodCarryForward(companyId, iyear, iperiod);
        if (nextPeriodPlan.status() == NextPeriodCarryForwardStatus.BLOCKED) {
            throw new IllegalStateException(nextPeriodPlan.message());
        }
        YearMonth nextPeriod = nextPeriodPlan.nextPeriod();
        if (nextPeriodPlan.status() == NextPeriodCarryForwardStatus.REPLACEABLE_CARRY_FORWARD) {
            deleteNextPeriodCarryForwardRows(companyId, nextPeriod);
        }
        int nextIyperiod = buildYearPeriod(nextPeriod.getYear(), nextPeriod.getMonthValue());
        int sumCount = 0;
        for (GlAccsum target : nextPeriodPlan.expectedSums()) {
            glAccsumMapper.insert(target);
            sumCount++;
        }

        int assistCount = 0;
        for (GlAccass target : nextPeriodPlan.expectedAssists()) {
            glAccassMapper.insert(target);
            assistCount++;
        }

        return new CarryForwardResult(nextIyperiod, sumCount, assistCount);
    }

    protected NextPeriodCarryForwardPlan inspectNextPeriodCarryForward(String companyId, int iyear, int iperiod) {
        YearMonth nextPeriod = YearMonth.of(iyear, iperiod).plusMonths(1);
        long nextVoucherCount = safeLong(glAccvouchMapper.selectCount(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIyear, nextPeriod.getYear())
                        .eq(GlAccvouch::getIperiod, nextPeriod.getMonthValue())
        ));
        if (nextVoucherCount > 0) {
            return new NextPeriodCarryForwardPlan(
                    nextPeriod,
                    NextPeriodCarryForwardStatus.BLOCKED,
                    "下一期间已存在真实凭证数据，不能重复滚转，请先检查期初或期间数据",
                    List.of(),
                    List.of()
            );
        }

        FinancePostVoucherState nextPostState = findPostState(companyId, nextPeriod.getYear(), nextPeriod.getMonthValue());
        if (nextPostState != null
                && (!Objects.equals(trimToNull(nextPostState.getStatus()), POST_STATUS_NOT_POSTED)
                || safeInt(nextPostState.getPostedVoucherCount()) > 0)) {
            return new NextPeriodCarryForwardPlan(
                    nextPeriod,
                    NextPeriodCarryForwardStatus.BLOCKED,
                    "下一期间已存在已记账状态数据，不能重复滚转，请先检查期初或期间数据",
                    List.of(),
                    List.of()
            );
        }

        FinancePeriodClose nextClose = findPeriodClose(companyId, nextPeriod.getYear(), nextPeriod.getMonthValue());
        if (nextClose != null) {
            return new NextPeriodCarryForwardPlan(
                    nextPeriod,
                    NextPeriodCarryForwardStatus.BLOCKED,
                    "下一期间已存在结账记录，不能重复滚转，请先检查期初或期间数据",
                    List.of(),
                    List.of()
            );
        }

        List<GlAccsum> sourceSums = loadAccsumRows(companyId, iyear, iperiod);
        List<GlAccass> sourceAssists = loadAccassRows(companyId, iyear, iperiod);
        List<GlAccsum> nextSums = loadAccsumRows(companyId, nextPeriod.getYear(), nextPeriod.getMonthValue());
        List<GlAccass> nextAssists = loadAccassRows(companyId, nextPeriod.getYear(), nextPeriod.getMonthValue());

        if (containsEffectiveMovement(nextSums)) {
            return new NextPeriodCarryForwardPlan(
                    nextPeriod,
                    NextPeriodCarryForwardStatus.BLOCKED,
                    "下一期间总账已存在发生额，不能重复滚转，请先检查期初或期间数据",
                    List.of(),
                    List.of()
            );
        }
        if (containsEffectiveAssistMovement(nextAssists)) {
            return new NextPeriodCarryForwardPlan(
                    nextPeriod,
                    NextPeriodCarryForwardStatus.BLOCKED,
                    "下一期间辅助账已存在发生额，不能重复滚转，请先检查期初或期间数据",
                    List.of(),
                    List.of()
            );
        }

        Map<String, FinanceAccountSubject> subjectMap = loadSubjectMap(companyId);
        List<GlAccsum> expectedSums = buildCarryForwardSums(companyId, nextPeriod, subjectMap, sourceSums);
        List<GlAccass> expectedAssists = buildCarryForwardAssists(companyId, nextPeriod, subjectMap, sourceAssists);

        if (nextSums.isEmpty() && nextAssists.isEmpty()) {
            return new NextPeriodCarryForwardPlan(
                    nextPeriod,
                    NextPeriodCarryForwardStatus.EMPTY,
                    "下一期间无账表基础，可正常滚转",
                    expectedSums,
                    expectedAssists
            );
        }

        if (matchesCarryForwardSums(expectedSums, nextSums) && matchesCarryForwardAssists(expectedAssists, nextAssists)) {
            return new NextPeriodCarryForwardPlan(
                    nextPeriod,
                    NextPeriodCarryForwardStatus.REPLACEABLE_CARRY_FORWARD,
                    "下一期间仅存在可替换的旧滚转基础，结账时将自动覆盖重建",
                    expectedSums,
                    expectedAssists
            );
        }

        return new NextPeriodCarryForwardPlan(
                nextPeriod,
                NextPeriodCarryForwardStatus.BLOCKED,
                "下一期间基础数据与当前期间应滚转结果不一致，不能自动覆盖，请先检查期初或期间数据",
                expectedSums,
                expectedAssists
        );
    }

    protected List<GlAccsum> loadAccsumRows(String companyId, int iyear, int iperiod) {
        return glAccsumMapper.selectList(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, iyear)
                        .eq(GlAccsum::getIperiod, iperiod)
                        .orderByAsc(GlAccsum::getId)
        );
    }

    protected List<GlAccass> loadAccassRows(String companyId, int iyear, int iperiod) {
        return glAccassMapper.selectList(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, iyear)
                        .eq(GlAccass::getIperiod, iperiod)
                        .orderByAsc(GlAccass::getId)
        );
    }

    protected List<GlAccsum> buildCarryForwardSums(
            String companyId,
            YearMonth nextPeriod,
            Map<String, FinanceAccountSubject> subjectMap,
            List<GlAccsum> sourceSums
    ) {
        int nextIyperiod = buildYearPeriod(nextPeriod.getYear(), nextPeriod.getMonthValue());
        List<GlAccsum> result = new ArrayList<>();
        for (GlAccsum source : sourceSums) {
            FinanceAccountSubject subject = requireCarryForwardSubject(subjectMap, source.getCcode());
            GlAccsum target = new GlAccsum();
            target.setCompanyId(companyId);
            target.setIyear(nextPeriod.getYear());
            target.setIperiod(nextPeriod.getMonthValue());
            target.setIyperiod(nextIyperiod);
            target.setCcode(source.getCcode());
            FinanceBalanceRowSupport.fillBalanceRow(
                    target,
                    subject,
                    source.getMe(),
                    source.getMeF(),
                    source.getNeS(),
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO_QTY,
                    ZERO_QTY
            );
            result.add(target);
        }
        return result;
    }

    protected List<GlAccass> buildCarryForwardAssists(
            String companyId,
            YearMonth nextPeriod,
            Map<String, FinanceAccountSubject> subjectMap,
            List<GlAccass> sourceAssists
    ) {
        int nextIyperiod = buildYearPeriod(nextPeriod.getYear(), nextPeriod.getMonthValue());
        List<GlAccass> result = new ArrayList<>();
        for (GlAccass source : sourceAssists) {
            FinanceAccountSubject subject = requireCarryForwardSubject(subjectMap, source.getCcode());
            GlAccass target = new GlAccass();
            target.setCompanyId(companyId);
            target.setIyear(nextPeriod.getYear());
            target.setIperiod(nextPeriod.getMonthValue());
            target.setIyperiod(nextIyperiod);
            target.setCcode(source.getCcode());
            target.setCdeptId(trimToNull(source.getCdeptId()));
            target.setCpersonId(trimToNull(source.getCpersonId()));
            target.setCcusId(trimToNull(source.getCcusId()));
            target.setCsupId(trimToNull(source.getCsupId()));
            target.setCitemClass(trimToNull(source.getCitemClass()));
            target.setCitemId(trimToNull(source.getCitemId()));
            FinanceBalanceRowSupport.fillBalanceRow(
                    target,
                    subject,
                    source.getMe(),
                    source.getMeF(),
                    source.getNeS(),
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO_QTY,
                    ZERO_QTY
            );
            result.add(target);
        }
        return result;
    }

    protected void deleteNextPeriodCarryForwardRows(String companyId, YearMonth nextPeriod) {
        glAccsumMapper.delete(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, nextPeriod.getYear())
                        .eq(GlAccsum::getIperiod, nextPeriod.getMonthValue())
        );
        glAccassMapper.delete(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, nextPeriod.getYear())
                        .eq(GlAccass::getIperiod, nextPeriod.getMonthValue())
        );
    }

    protected boolean matchesCarryForwardSums(List<GlAccsum> expectedRows, List<GlAccsum> actualRows) {
        return buildCarryForwardSumSignatures(expectedRows).equals(buildCarryForwardSumSignatures(actualRows));
    }

    protected boolean matchesCarryForwardAssists(List<GlAccass> expectedRows, List<GlAccass> actualRows) {
        return buildCarryForwardAssistSignatures(expectedRows).equals(buildCarryForwardAssistSignatures(actualRows));
    }

    protected List<String> buildCarryForwardSumSignatures(List<GlAccsum> rows) {
        return rows.stream()
                .map(this::buildCarryForwardSumSignature)
                .sorted()
                .toList();
    }

    protected List<String> buildCarryForwardAssistSignatures(List<GlAccass> rows) {
        return rows.stream()
                .map(this::buildCarryForwardAssistSignature)
                .sorted()
                .toList();
    }

    protected String buildCarryForwardSumSignature(GlAccsum row) {
        return String.join("|",
                trimToEmpty(row.getCcode()),
                trimToEmpty(row.getCbegindC()),
                trimToEmpty(row.getCbegindCEngl()),
                trimToEmpty(row.getCenddC()),
                trimToEmpty(row.getCenddCEngl()),
                trimToEmpty(row.getCurrencyCode()),
                trimToEmpty(row.getCexchName()),
                money(row.getMb()).toPlainString(),
                money(row.getMbF()).toPlainString(),
                money(row.getMd()).toPlainString(),
                money(row.getMdF()).toPlainString(),
                money(row.getMc()).toPlainString(),
                money(row.getMcF()).toPlainString(),
                money(row.getMe()).toPlainString(),
                money(row.getMeF()).toPlainString(),
                qty(row.getNbS()).toPlainString(),
                qty(row.getNdS()).toPlainString(),
                qty(row.getNcS()).toPlainString(),
                qty(row.getNeS()).toPlainString()
        );
    }

    protected String buildCarryForwardAssistSignature(GlAccass row) {
        return String.join("|",
                trimToEmpty(row.getCcode()),
                trimToEmpty(row.getCdeptId()),
                trimToEmpty(row.getCpersonId()),
                trimToEmpty(row.getCcusId()),
                trimToEmpty(row.getCsupId()),
                trimToEmpty(row.getCitemClass()),
                trimToEmpty(row.getCitemId()),
                trimToEmpty(row.getCbegindC()),
                trimToEmpty(row.getCbegindCEngl()),
                trimToEmpty(row.getCenddC()),
                trimToEmpty(row.getCenddCEngl()),
                trimToEmpty(row.getCurrencyCode()),
                trimToEmpty(row.getCexchName()),
                money(row.getMb()).toPlainString(),
                money(row.getMbF()).toPlainString(),
                money(row.getMd()).toPlainString(),
                money(row.getMdF()).toPlainString(),
                money(row.getMc()).toPlainString(),
                money(row.getMcF()).toPlainString(),
                money(row.getMe()).toPlainString(),
                money(row.getMeF()).toPlainString(),
                qty(row.getNbS()).toPlainString(),
                qty(row.getNdS()).toPlainString(),
                qty(row.getNcS()).toPlainString(),
                qty(row.getNeS()).toPlainString()
        );
    }

    protected FinanceAccountSubject requireCarryForwardSubject(Map<String, FinanceAccountSubject> subjectMap, String subjectCode) {
        FinanceAccountSubject subject = subjectMap.get(trimToNull(subjectCode));
        if (subject == null) {
            throw new IllegalStateException("科目【" + trimToNull(subjectCode) + "】不存在，无法完成结账滚转");
        }
        return subject;
    }

    protected void logReconcile(String companyId, int iyear, int iperiod, String operatorName, FinanceCloseLedgerReconcileResultVO result) {
        insertLog(
                companyId,
                iyear,
                iperiod,
                Boolean.TRUE.equals(result.getPassed()) ? "RECONCILE_PASS" : "RECONCILE_FAIL",
                Boolean.TRUE.equals(result.getPassed()) ? "SUCCESS" : "FAILED",
                operatorName,
                result.getSummaryMessage(),
                "{\"differenceSubjectCount\":" + safeInt(result.getDifferenceSubjectCount())
                        + ",\"differenceAssistCount\":" + safeInt(result.getDifferenceAssistCount())
                        + ",\"missingAssistCount\":" + safeInt(result.getMissingAssistCount())
                        + ",\"illegalAssistCount\":" + safeInt(result.getIllegalAssistCount()) + "}"
        );
    }

    protected void logValidation(String companyId, int iyear, int iperiod, String operatorName, FinanceCloseLedgerValidationResultVO result) {
        if (Boolean.TRUE.equals(result.getPassed())) {
            return;
        }
        insertLog(
                companyId,
                iyear,
                iperiod,
                "VALIDATION_FAIL",
                "FAILED",
                operatorName,
                joinBlockingReasons(result),
                null
        );
    }

    protected String joinBlockingReasons(FinanceCloseLedgerValidationResultVO result) {
        List<String> reasons = result == null ? List.of() : result.getBlockingReasons();
        if (reasons == null || reasons.isEmpty()) {
            return "结账校验未通过";
        }
        return reasons.stream().distinct().collect(Collectors.joining("；"));
    }

    protected FinanceCloseLedgerCheckItemVO buildCheck(String code, String label, boolean passed, String message) {
        FinanceCloseLedgerCheckItemVO item = new FinanceCloseLedgerCheckItemVO();
        item.setCode(code);
        item.setLabel(label);
        item.setPassed(passed);
        item.setMessage(message);
        return item;
    }

    protected String resolvePostStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "NOT_POSTED" : status) {
            case "POSTING" -> "记账中";
            case "PARTIALLY_POSTED" -> "部分记账";
            case "FULLY_POSTED" -> "已全部记账";
            case "FAILED" -> "记账失败";
            default -> "未记账";
        };
    }

    protected List<String> validateAssistRows(String companyId, int iyear, int iperiod) {
        Map<String, FinanceAccountSubject> subjectMap = financeAccountSubjectMapper.selectList(
                        Wrappers.<FinanceAccountSubject>lambdaQuery()
                                .eq(FinanceAccountSubject::getCompanyId, companyId)
                                .eq(FinanceAccountSubject::getStatus, 1)
                ).stream()
                .collect(Collectors.toMap(FinanceAccountSubject::getSubjectCode, item -> item, (left, right) -> left));
        List<String> messages = new ArrayList<>();
        List<GlAccvouch> rows = glAccvouchMapper.selectList(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIyear, iyear)
                        .eq(GlAccvouch::getIperiod, iperiod)
                        .eq(GlAccvouch::getIbook, 1)
        );
        for (GlAccvouch row : rows) {
            FinanceAccountSubject subject = subjectMap.get(trimToNull(row.getCcode()));
            if (subject == null) {
                messages.add("科目【" + trimToNull(row.getCcode()) + "】不存在或已停用");
                continue;
            }
            if (trimToNull(row.getCdeptId()) != null && !Objects.equals(subject.getBdept(), 1)) {
                messages.add("科目【" + subject.getSubjectCode() + "】未启用部门辅助核算");
            }
            if (trimToNull(row.getCpersonId()) != null && !Objects.equals(subject.getBperson(), 1)) {
                messages.add("科目【" + subject.getSubjectCode() + "】未启用人员辅助核算");
            }
            if (trimToNull(row.getCcusId()) != null && !Objects.equals(subject.getBcus(), 1)) {
                messages.add("科目【" + subject.getSubjectCode() + "】未启用客户辅助核算");
            }
            if (trimToNull(row.getCsupId()) != null && !Objects.equals(subject.getBsup(), 1)) {
                messages.add("科目【" + subject.getSubjectCode() + "】未启用供应商辅助核算");
            }
            if ((trimToNull(row.getCitemClass()) != null || trimToNull(row.getCitemId()) != null) && !Objects.equals(subject.getBitem(), 1)) {
                messages.add("科目【" + subject.getSubjectCode() + "】未启用项目辅助核算");
            }
            String lockedProjectClass = trimToNull(subject.getCassItem());
            if (lockedProjectClass != null && trimToNull(row.getCitemClass()) != null && !Objects.equals(lockedProjectClass, trimToNull(row.getCitemClass()))) {
                messages.add("科目【" + subject.getSubjectCode() + "】项目分类必须为【" + lockedProjectClass + "】");
            }
        }
        return messages.stream().distinct().toList();
    }

    protected void insertLog(
            String companyId,
            int iyear,
            int iperiod,
            String actionType,
            String actionStatus,
            String operatorName,
            String message,
            String detailJson
    ) {
        FinancePeriodCloseLog log = new FinancePeriodCloseLog();
        log.setCompanyId(companyId);
        log.setIyear(iyear);
        log.setIperiod(iperiod);
        log.setIyperiod(buildYearPeriod(iyear, iperiod));
        log.setActionType(actionType);
        log.setActionStatus(actionStatus);
        log.setOperatorName(trimToNull(operatorName));
        log.setMessage(trimToNull(message));
        log.setDetailJson(trimToNull(detailJson));
        log.setCreatedAt(LocalDateTime.now());
        financePeriodCloseLogMapper.insert(log);
    }

    protected void deleteCloseRecord(Long id) {
        if (id == null) {
            return;
        }
        financePeriodCloseMapper.deleteById(id);
    }

    protected FinanceGeneralLedgerRollbackSnapshot rollbackPostedPeriod(
            String companyId,
            int iyear,
            int iperiod,
            String operatorName
    ) {
        requireActiveAccountSet(companyId);
        FinanceAccountSet accountSet = requireActiveAccountSet(companyId);
        boolean closedPeriodDetected = reopenClosedPeriodIfNecessary(companyId, iyear, iperiod, operatorName);
        Map<VoucherKey, List<GlAccvouch>> voucherGroups = loadVoucherGroups(companyId, iyear, iperiod);
        VoucherCounts counts = summarizeVoucherGroups(voucherGroups);
        if (counts.postedCount() <= 0) {
            throw new IllegalStateException("当前期间没有已记账凭证，无需执行专项回退");
        }

        int rolledBackVoucherCount = 0;
        for (VoucherKey voucherKey : counts.postedKeys()) {
            glAccvouchMapper.update(
                    null,
                    Wrappers.<GlAccvouch>lambdaUpdate()
                            .eq(GlAccvouch::getCompanyId, voucherKey.companyId())
                            .eq(GlAccvouch::getIyear, voucherKey.iyear())
                            .eq(GlAccvouch::getIperiod, voucherKey.iperiod())
                            .eq(GlAccvouch::getCsign, voucherKey.csign())
                            .eq(GlAccvouch::getInoId, voucherKey.inoId())
                            .set(GlAccvouch::getIbook, 0)
                            .set(GlAccvouch::getPostedAt, null)
                            .set(GlAccvouch::getCbook, null)
            );
            rolledBackVoucherCount++;
        }

        FinancePostVoucherState postState = findPostState(companyId, iyear, iperiod);
        if (postState == null) {
            postState = new FinancePostVoucherState();
            postState.setCompanyId(companyId);
            postState.setIyear(iyear);
            postState.setIperiod(iperiod);
            postState.setIyperiod(buildYearPeriod(iyear, iperiod));
            postState.setCreatedAt(LocalDateTime.now());
        }
        postState.setStatus(POST_STATUS_NOT_POSTED);
        postState.setPostedVoucherCount(0);
        postState.setLastTaskNo(null);
        postState.setLastTaskStatus("ROLLBACK_SUCCESS");
        postState.setLastErrorMessage(null);
        postState.setLastPostedBy(trimToNull(operatorName));
        postState.setLastPostedAt(LocalDateTime.now());
        postState.setUpdatedAt(LocalDateTime.now());
        if (postState.getId() == null) {
            financePostVoucherStateMapper.insert(postState);
        } else {
            financePostVoucherStateMapper.updateById(postState);
        }

        LedgerRebuildResult rebuildResult = rebuildCurrentPeriodLedgerBase(
                companyId,
                iyear,
                iperiod,
                accountSet,
                loadSubjectMap(companyId)
        );
        insertLog(
                companyId,
                iyear,
                iperiod,
                "ROLLBACK_UNPOST_SUCCESS",
                "SUCCESS",
                operatorName,
                "专项回退成功，当前期间已回退为已审核未记账",
                "{\"rolledBackVoucherCount\":" + rolledBackVoucherCount
                        + ",\"rebuildAccsumCount\":" + rebuildResult.glAccsumCount()
                        + ",\"rebuildAccassCount\":" + rebuildResult.glAccassCount()
                        + ",\"closedPeriodDetected\":" + closedPeriodDetected + "}"
        );
        return new FinanceGeneralLedgerRollbackSnapshot(
                closedPeriodDetected,
                rolledBackVoucherCount,
                rebuildResult.glAccsumCount(),
                rebuildResult.glAccassCount()
        );
    }

    protected boolean reopenClosedPeriodIfNecessary(String companyId, int iyear, int iperiod, String operatorName) {
        FinancePeriodClose close = findPeriodClose(companyId, iyear, iperiod);
        if (close == null) {
            return false;
        }
        YearMonth nextPeriod = YearMonth.of(iyear, iperiod).plusMonths(1);
        long nextVoucherCount = safeLong(glAccvouchMapper.selectCount(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIyear, nextPeriod.getYear())
                        .eq(GlAccvouch::getIperiod, nextPeriod.getMonthValue())
        ));
        if (nextVoucherCount > 0) {
            throw new IllegalStateException("下一期间已存在真实凭证数据，不能直接反结账，请先处理下一期间业务");
        }
        FinancePostVoucherState nextPostState = findPostState(companyId, nextPeriod.getYear(), nextPeriod.getMonthValue());
        if (nextPostState != null
                && (!Objects.equals(trimToNull(nextPostState.getStatus()), POST_STATUS_NOT_POSTED)
                || safeInt(nextPostState.getPostedVoucherCount()) > 0)) {
            throw new IllegalStateException("下一期间已存在记账状态数据，不能直接反结账，请先处理下一期间业务");
        }
        FinancePeriodClose nextClose = findPeriodClose(companyId, nextPeriod.getYear(), nextPeriod.getMonthValue());
        if (nextClose != null) {
            throw new IllegalStateException("下一期间已存在结账记录，不能直接反结账，请先处理下一期间业务");
        }

        List<GlAccsum> nextSums = glAccsumMapper.selectList(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, nextPeriod.getYear())
                        .eq(GlAccsum::getIperiod, nextPeriod.getMonthValue())
        );
        if (containsEffectiveMovement(nextSums)) {
            throw new IllegalStateException("下一期间总账已存在发生额，不能直接反结账，请先处理下一期间业务");
        }
        List<GlAccass> nextAssists = glAccassMapper.selectList(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, nextPeriod.getYear())
                        .eq(GlAccass::getIperiod, nextPeriod.getMonthValue())
        );
        if (containsEffectiveAssistMovement(nextAssists)) {
            throw new IllegalStateException("下一期间辅助账已存在发生额，不能直接反结账，请先处理下一期间业务");
        }

        glAccsumMapper.delete(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, nextPeriod.getYear())
                        .eq(GlAccsum::getIperiod, nextPeriod.getMonthValue())
        );
        glAccassMapper.delete(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, nextPeriod.getYear())
                        .eq(GlAccass::getIperiod, nextPeriod.getMonthValue())
        );
        financePeriodCloseMapper.deleteById(close.getId());
        insertLog(
                companyId,
                iyear,
                iperiod,
                "ROLLBACK_REOPEN_SUCCESS",
                "SUCCESS",
                operatorName,
                "专项反结账成功，已删除当前期间结账记录并清理下一期间滚转基础",
                "{\"nextIyperiod\":" + buildYearPeriod(nextPeriod.getYear(), nextPeriod.getMonthValue())
                        + ",\"deletedAccsumCount\":" + nextSums.size()
                        + ",\"deletedAccassCount\":" + nextAssists.size() + "}"
        );
        return true;
    }

    protected LedgerRebuildResult rebuildCurrentPeriodLedgerBase(
            String companyId,
            int iyear,
            int iperiod,
            FinanceAccountSet accountSet,
            Map<String, FinanceAccountSubject> subjectMap
    ) {
        List<GlAccsum> currentSums = glAccsumMapper.selectList(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, iyear)
                        .eq(GlAccsum::getIperiod, iperiod)
                        .orderByAsc(GlAccsum::getId)
        );
        List<GlAccass> currentAssists = glAccassMapper.selectList(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, iyear)
                        .eq(GlAccass::getIperiod, iperiod)
                        .orderByAsc(GlAccass::getId)
        );
        YearMonth previousPeriod = YearMonth.of(iyear, iperiod).minusMonths(1);
        boolean usePreviousBalanceAsBaseline =
                YearMonth.of(accountSet.getEnabledYear(), accountSet.getEnabledPeriod()).isBefore(YearMonth.of(iyear, iperiod));

        List<GlAccsum> previousSums = usePreviousBalanceAsBaseline
                ? glAccsumMapper.selectList(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, previousPeriod.getYear())
                        .eq(GlAccsum::getIperiod, previousPeriod.getMonthValue())
                        .orderByAsc(GlAccsum::getId)
        )
                : List.of();
        List<GlAccass> previousAssists = usePreviousBalanceAsBaseline
                ? glAccassMapper.selectList(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, previousPeriod.getYear())
                        .eq(GlAccass::getIperiod, previousPeriod.getMonthValue())
                        .orderByAsc(GlAccass::getId)
        )
                : List.of();

        glAccsumMapper.delete(
                Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, iyear)
                        .eq(GlAccsum::getIperiod, iperiod)
        );
        glAccassMapper.delete(
                Wrappers.<GlAccass>lambdaQuery()
                        .eq(GlAccass::getCompanyId, companyId)
                        .eq(GlAccass::getIyear, iyear)
                        .eq(GlAccass::getIperiod, iperiod)
        );

        int rebuiltSumCount = 0;
        if (usePreviousBalanceAsBaseline && !previousSums.isEmpty()) {
            for (GlAccsum previous : previousSums) {
                FinanceAccountSubject subject = requireCarryForwardSubject(subjectMap, previous.getCcode());
                GlAccsum target = new GlAccsum();
                target.setCompanyId(companyId);
                target.setIyear(iyear);
                target.setIperiod(iperiod);
                target.setIyperiod(buildYearPeriod(iyear, iperiod));
                target.setCcode(previous.getCcode());
                target.setCurrencyCode(normalizeCurrency(previous.getCurrencyCode(), previous.getCexchName()));
                FinanceBalanceRowSupport.fillBalanceRow(
                        target,
                        subject,
                        previous.getMe(),
                        previous.getMeF(),
                        previous.getNeS(),
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO_QTY,
                        ZERO_QTY
                );
                glAccsumMapper.insert(target);
                rebuiltSumCount++;
            }
        } else {
            for (GlAccsum current : currentSums) {
                FinanceAccountSubject subject = subjectMap.get(trimToNull(current.getCcode()));
                GlAccsum target = new GlAccsum();
                target.setCompanyId(companyId);
                target.setIyear(iyear);
                target.setIperiod(iperiod);
                target.setIyperiod(buildYearPeriod(iyear, iperiod));
                target.setCcode(current.getCcode());
                target.setCurrencyCode(normalizeCurrency(current.getCurrencyCode(), current.getCexchName()));
                FinanceBalanceRowSupport.fillBalanceRow(
                        target,
                        subject,
                        current.getMb(),
                        current.getMbF(),
                        current.getNbS(),
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO_QTY,
                        ZERO_QTY
                );
                glAccsumMapper.insert(target);
                rebuiltSumCount++;
            }
        }

        int rebuiltAssistCount = 0;
        if (usePreviousBalanceAsBaseline && !previousAssists.isEmpty()) {
            for (GlAccass previous : previousAssists) {
                FinanceAccountSubject subject = requireCarryForwardSubject(subjectMap, previous.getCcode());
                GlAccass target = new GlAccass();
                target.setCompanyId(companyId);
                target.setIyear(iyear);
                target.setIperiod(iperiod);
                target.setIyperiod(buildYearPeriod(iyear, iperiod));
                target.setCcode(previous.getCcode());
                target.setCdeptId(trimToNull(previous.getCdeptId()));
                target.setCpersonId(trimToNull(previous.getCpersonId()));
                target.setCcusId(trimToNull(previous.getCcusId()));
                target.setCsupId(trimToNull(previous.getCsupId()));
                target.setCitemClass(trimToNull(previous.getCitemClass()));
                target.setCitemId(trimToNull(previous.getCitemId()));
                target.setCurrencyCode(normalizeCurrency(previous.getCurrencyCode(), previous.getCexchName()));
                FinanceBalanceRowSupport.fillBalanceRow(
                        target,
                        subject,
                        previous.getMe(),
                        previous.getMeF(),
                        previous.getNeS(),
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO_QTY,
                        ZERO_QTY
                );
                glAccassMapper.insert(target);
                rebuiltAssistCount++;
            }
        } else {
            for (GlAccass current : currentAssists) {
                FinanceAccountSubject subject = subjectMap.get(trimToNull(current.getCcode()));
                GlAccass target = new GlAccass();
                target.setCompanyId(companyId);
                target.setIyear(iyear);
                target.setIperiod(iperiod);
                target.setIyperiod(buildYearPeriod(iyear, iperiod));
                target.setCcode(current.getCcode());
                target.setCdeptId(trimToNull(current.getCdeptId()));
                target.setCpersonId(trimToNull(current.getCpersonId()));
                target.setCcusId(trimToNull(current.getCcusId()));
                target.setCsupId(trimToNull(current.getCsupId()));
                target.setCitemClass(trimToNull(current.getCitemClass()));
                target.setCitemId(trimToNull(current.getCitemId()));
                target.setCurrencyCode(normalizeCurrency(current.getCurrencyCode(), current.getCexchName()));
                FinanceBalanceRowSupport.fillBalanceRow(
                        target,
                        subject,
                        current.getMb(),
                        current.getMbF(),
                        current.getNbS(),
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO,
                        ZERO_QTY,
                        ZERO_QTY
                );
                glAccassMapper.insert(target);
                rebuiltAssistCount++;
            }
        }
        return new LedgerRebuildResult(rebuiltSumCount, rebuiltAssistCount);
    }

    protected Map<MovementKey, MovementTotals> buildVoucherSubjectMap(String companyId, int iyear, int iperiod) {
        Map<String, FinanceAccountSubject> subjectMap = loadSubjectMap(companyId);
        Map<MovementKey, MovementTotals> movementMap = new LinkedHashMap<>();
        for (GlAccvouch row : glAccvouchMapper.selectList(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIyear, iyear)
                        .eq(GlAccvouch::getIperiod, iperiod)
                        .eq(GlAccvouch::getIbook, 1)
        )) {
            MovementTotals totals = MovementTotals.fromPostedVoucher(row);
            for (String subjectCode : resolveSubjectRollupCodes(subjectMap, row.getCcode())) {
                movementMap.merge(
                        new MovementKey(subjectCode, normalizeCurrency(row.getCurrencyCode(), row.getCexchName())),
                        totals,
                        MovementTotals::merge
                );
            }
        }
        return movementMap;
    }

    protected Map<AssistKey, MovementTotals> buildVoucherAssistMap(String companyId, int iyear, int iperiod) {
        return glAccvouchMapper.selectList(
                        Wrappers.<GlAccvouch>lambdaQuery()
                                .eq(GlAccvouch::getCompanyId, companyId)
                                .eq(GlAccvouch::getIyear, iyear)
                                .eq(GlAccvouch::getIperiod, iperiod)
                                .eq(GlAccvouch::getIbook, 1)
                ).stream()
                .filter(this::hasAssistDimension)
                .collect(Collectors.toMap(
                        row -> new AssistKey(
                                trimToNull(row.getCcode()),
                                normalizeCurrency(row.getCurrencyCode(), row.getCexchName()),
                                trimToNull(row.getCdeptId()),
                                trimToNull(row.getCpersonId()),
                                trimToNull(row.getCcusId()),
                                trimToNull(row.getCsupId()),
                                trimToNull(row.getCitemClass()),
                                trimToNull(row.getCitemId())
                        ),
                        MovementTotals::fromPostedVoucher,
                        MovementTotals::merge,
                        LinkedHashMap::new
                ));
    }

    protected Map<MovementKey, MovementTotals> buildAccsumMap(String companyId, int iyear, int iperiod) {
        return glAccsumMapper.selectList(
                        Wrappers.<GlAccsum>lambdaQuery()
                                .eq(GlAccsum::getCompanyId, companyId)
                                .eq(GlAccsum::getIyear, iyear)
                                .eq(GlAccsum::getIperiod, iperiod)
                ).stream()
                .filter(row -> hasMovement(row.getMd(), row.getMc(), row.getMdF(), row.getMcF(), row.getNdS(), row.getNcS()))
                .collect(Collectors.toMap(
                        row -> new MovementKey(trimToNull(row.getCcode()), normalizeCurrency(row.getCurrencyCode(), row.getCexchName())),
                        row -> MovementTotals.fromVoucher(row.getMd(), row.getMc(), row.getMdF(), row.getMcF(), row.getNdS(), row.getNcS()),
                        MovementTotals::merge,
                        LinkedHashMap::new
                ));
    }

    protected Map<AssistKey, MovementTotals> buildAccassMap(String companyId, int iyear, int iperiod) {
        return glAccassMapper.selectList(
                        Wrappers.<GlAccass>lambdaQuery()
                                .eq(GlAccass::getCompanyId, companyId)
                                .eq(GlAccass::getIyear, iyear)
                                .eq(GlAccass::getIperiod, iperiod)
                ).stream()
                .filter(row -> hasMovement(row.getMd(), row.getMc(), row.getMdF(), row.getMcF(), row.getNdS(), row.getNcS()))
                .collect(Collectors.toMap(
                        row -> new AssistKey(
                                trimToNull(row.getCcode()),
                                normalizeCurrency(row.getCurrencyCode(), row.getCexchName()),
                                trimToNull(row.getCdeptId()),
                                trimToNull(row.getCpersonId()),
                                trimToNull(row.getCcusId()),
                                trimToNull(row.getCsupId()),
                                trimToNull(row.getCitemClass()),
                                trimToNull(row.getCitemId())
                        ),
                        row -> MovementTotals.fromVoucher(row.getMd(), row.getMc(), row.getMdF(), row.getMcF(), row.getNdS(), row.getNcS()),
                        MovementTotals::merge,
                        LinkedHashMap::new
                ));
    }

    protected YearMonth resolveNextPeriod(String companyId, FinanceAccountSet accountSet) {
        FinancePeriodClose latestClosed = findLatestClosedPeriod(companyId);
        if (latestClosed == null) {
            return YearMonth.of(accountSet.getEnabledYear(), accountSet.getEnabledPeriod());
        }
        return YearMonth.of(latestClosed.getIyear(), latestClosed.getIperiod()).plusMonths(1);
    }

    protected boolean isClosedPeriod(String companyId, int iyear, int iperiod) {
        FinancePeriodClose close = findPeriodClose(companyId, iyear, iperiod);
        return close != null && CLOSE_STATUS_CLOSED.equals(trimToNull(close.getStatus()));
    }

    protected String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected String trimToEmpty(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized;
    }

    protected String normalizeCurrency(String currencyCode, String cexchName) {
        String normalizedCode = trimToNull(currencyCode);
        if (normalizedCode != null) {
            return normalizedCode.toUpperCase(Locale.ROOT);
        }
        String currencyName = trimToNull(cexchName);
        if (currencyName == null || currencyName.contains("人民币")) {
            return "CNY";
        }
        return currencyName.toUpperCase(Locale.ROOT);
    }

    protected boolean hasAssistDimension(GlAccvouch row) {
        return trimToNull(row.getCdeptId()) != null
                || trimToNull(row.getCpersonId()) != null
                || trimToNull(row.getCcusId()) != null
                || trimToNull(row.getCsupId()) != null
                || trimToNull(row.getCitemClass()) != null
                || trimToNull(row.getCitemId()) != null;
    }

    protected boolean hasMovement(BigDecimal md, BigDecimal mc, BigDecimal mdF, BigDecimal mcF, BigDecimal ndS, BigDecimal ncS) {
        return money(md).compareTo(ZERO) != 0
                || money(mc).compareTo(ZERO) != 0
                || money(mdF).compareTo(ZERO) != 0
                || money(mcF).compareTo(ZERO) != 0
                || qty(ndS).compareTo(ZERO_QTY) != 0
                || qty(ncS).compareTo(ZERO_QTY) != 0;
    }

    protected BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal qty(BigDecimal value) {
        return value == null ? ZERO_QTY : value.setScale(6, RoundingMode.HALF_UP);
    }

    protected List<String> resolveSubjectRollupCodes(Map<String, FinanceAccountSubject> subjectMap, String subjectCode) {
        List<String> subjectCodes = new ArrayList<>();
        String currentCode = trimToNull(subjectCode);
        Set<String> visited = new LinkedHashSet<>();
        while (currentCode != null && visited.add(currentCode)) {
            subjectCodes.add(currentCode);
            FinanceAccountSubject subject = subjectMap.get(currentCode);
            currentCode = subject == null ? null : trimToNull(subject.getParentSubjectCode());
        }
        return subjectCodes;
    }

    protected boolean containsEffectiveMovement(List<GlAccsum> rows) {
        return rows.stream().anyMatch(row -> hasMovement(row.getMd(), row.getMc(), row.getMdF(), row.getMcF(), row.getNdS(), row.getNcS()));
    }

    protected boolean containsEffectiveAssistMovement(List<GlAccass> rows) {
        return rows.stream().anyMatch(row -> hasMovement(row.getMd(), row.getMc(), row.getMdF(), row.getMcF(), row.getNdS(), row.getNcS()));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

    protected record VoucherKey(String companyId, Integer iyear, Integer iperiod, String csign, Integer inoId) {
    }

    protected record VoucherCounts(
            List<VoucherKey> unpostedKeys,
            List<VoucherKey> reviewedKeys,
            List<VoucherKey> errorKeys,
            List<VoucherKey> postedKeys
    ) {
        int unpostedCount() {
            return unpostedKeys.size();
        }

        int reviewedCount() {
            return reviewedKeys.size();
        }

        int errorCount() {
            return errorKeys.size();
        }

        int postedCount() {
            return postedKeys.size();
        }

        int totalCount() {
            return unpostedCount() + reviewedCount() + errorCount() + postedCount();
        }
    }

    protected record CarryForwardResult(int nextIyperiod, int glAccsumCount, int glAccassCount) {
    }

    protected enum NextPeriodCarryForwardStatus {
        EMPTY,
        REPLACEABLE_CARRY_FORWARD,
        BLOCKED
    }

    protected record NextPeriodCarryForwardPlan(
            YearMonth nextPeriod,
            NextPeriodCarryForwardStatus status,
            String message,
            List<GlAccsum> expectedSums,
            List<GlAccass> expectedAssists
    ) {
    }

    protected record LedgerRebuildResult(int glAccsumCount, int glAccassCount) {
    }

    protected record FinanceGeneralLedgerRollbackSnapshot(
            boolean closedPeriodDetected,
            int rolledBackVoucherCount,
            int rebuildAccsumCount,
            int rebuildAccassCount
    ) {
    }

    protected record MovementKey(String subjectCode, String currencyCode) {
        String label() {
            return (subjectCode == null ? "未知科目" : subjectCode) + "/" + (currencyCode == null ? "CNY" : currencyCode);
        }
    }

    protected record AssistKey(
            String subjectCode,
            String currencyCode,
            String cdeptId,
            String cpersonId,
            String ccusId,
            String csupId,
            String citemClass,
            String citemId
    ) {
        String label() {
            List<String> parts = new ArrayList<>();
            parts.add(subjectLabel());
            addPart(parts, "部门", cdeptId);
            addPart(parts, "人员", cpersonId);
            addPart(parts, "客户", ccusId);
            addPart(parts, "供应商", csupId);
            addPart(parts, "项目分类", citemClass);
            addPart(parts, "项目", citemId);
            return String.join(" / ", parts);
        }

        String subjectLabel() {
            return (subjectCode == null ? "未知科目" : subjectCode) + "/" + (currencyCode == null ? "CNY" : currencyCode);
        }

        private static void addPart(Collection<String> parts, String label, String value) {
            if (value != null) {
                parts.add(label + ":" + value);
            }
        }
    }

    protected record MovementTotals(
            BigDecimal md,
            BigDecimal mc,
            BigDecimal mdF,
            BigDecimal mcF,
            BigDecimal ndS,
            BigDecimal ncS
    ) {
        static final MovementTotals ZERO = new MovementTotals(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
        );

        static MovementTotals fromVoucher(BigDecimal md, BigDecimal mc, BigDecimal mdF, BigDecimal mcF, BigDecimal ndS, BigDecimal ncS) {
            return new MovementTotals(
                    scaleMoney(md),
                    scaleMoney(mc),
                    scaleMoney(mdF),
                    scaleMoney(mcF),
                    scaleQty(ndS),
                    scaleQty(ncS)
            );
        }

        static MovementTotals fromPostedVoucher(GlAccvouch row) {
            return new MovementTotals(
                    scaleMoney(FinanceVoucherAmountSupport.effectiveDebit(row.getMd(), row.getMc())),
                    scaleMoney(FinanceVoucherAmountSupport.effectiveCredit(row.getMd(), row.getMc())),
                    scaleMoney(FinanceVoucherAmountSupport.effectiveDebit(row.getMdF(), row.getMcF())),
                    scaleMoney(FinanceVoucherAmountSupport.effectiveCredit(row.getMdF(), row.getMcF())),
                    scaleQty(row.getNdS()),
                    scaleQty(row.getNcS())
            );
        }

        MovementTotals merge(MovementTotals other) {
            return new MovementTotals(
                    md.add(other.md),
                    mc.add(other.mc),
                    mdF.add(other.mdF),
                    mcF.add(other.mcF),
                    ndS.add(other.ndS),
                    ncS.add(other.ncS)
            );
        }

        private static BigDecimal scaleMoney(BigDecimal value) {
            return value == null ? ZERO.md : value.setScale(2, RoundingMode.HALF_UP);
        }

        private static BigDecimal scaleQty(BigDecimal value) {
            return value == null ? ZERO.ndS : value.setScale(6, RoundingMode.HALF_UP);
        }
    }
}
