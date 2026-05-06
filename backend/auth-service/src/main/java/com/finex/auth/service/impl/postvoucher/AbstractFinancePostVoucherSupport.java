package com.finex.auth.service.impl.postvoucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinancePostVoucherMetaVO;
import com.finex.auth.dto.FinancePostVoucherTaskStatusVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinanceOpeningBalanceState;
import com.finex.auth.entity.FinancePeriodClose;
import com.finex.auth.entity.FinancePostVoucherState;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.AsyncTaskSupport;
import com.finex.auth.support.FinanceBalanceDirectionSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractFinancePostVoucherSupport {

    protected static final String ACCOUNT_SET_STATUS_ACTIVE = "ACTIVE";
    protected static final String OPENING_STATUS_OPENED = "OPENED";
    protected static final String VOUCHER_STATUS_UNPOSTED = "UNPOSTED";
    protected static final String VOUCHER_STATUS_REVIEWED = "REVIEWED";
    protected static final String VOUCHER_STATUS_ERROR = "ERROR";
    protected static final String VOUCHER_STATUS_POSTED = "POSTED";
    protected static final String POST_STATUS_NOT_POSTED = "NOT_POSTED";
    protected static final String POST_STATUS_POSTING = "POSTING";
    protected static final String POST_STATUS_PARTIALLY_POSTED = "PARTIALLY_POSTED";
    protected static final String POST_STATUS_FULLY_POSTED = "FULLY_POSTED";
    protected static final String POST_STATUS_FAILED = "FAILED";
    protected static final String CLOSE_STATUS_CLOSED = "CLOSED";
    protected static final int ERROR_FLAG = 1;
    protected static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    protected static final BigDecimal ZERO_QTY = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper;
    private final FinancePostVoucherStateMapper financePostVoucherStateMapper;
    private final FinancePeriodCloseMapper financePeriodCloseMapper;
    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final GlAccvouchMapper glAccvouchMapper;
    private final GlAccsumMapper glAccsumMapper;
    private final GlAccassMapper glAccassMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final UserMapper userMapper;

    protected AbstractFinancePostVoucherSupport(
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper
    ) {
        this.financeAccountSetMapper = financeAccountSetMapper;
        this.financeAccountSubjectMapper = financeAccountSubjectMapper;
        this.financeOpeningBalanceStateMapper = financeOpeningBalanceStateMapper;
        this.financePostVoucherStateMapper = financePostVoucherStateMapper;
        this.financePeriodCloseMapper = financePeriodCloseMapper;
        this.asyncTaskRecordMapper = asyncTaskRecordMapper;
        this.glAccvouchMapper = glAccvouchMapper;
        this.glAccsumMapper = glAccsumMapper;
        this.glAccassMapper = glAccassMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.userMapper = userMapper;
    }

    protected FinancePostVoucherMetaVO buildMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        SystemCompany company = resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        FinanceAccountSet accountSet = requireActiveAccountSet(company.getCompanyId());
        Map<VoucherKey, List<GlAccvouch>> voucherGroups = loadVoucherGroups(company.getCompanyId(), effectiveYear, effectivePeriod);
        PostingCounts counts = summarizeVoucherGroups(voucherGroups);
        FinancePostVoucherState state = findPostState(company.getCompanyId(), effectiveYear, effectivePeriod);
        String status = resolveEffectivePostStatus(state, counts);

        FinancePostVoucherMetaVO meta = new FinancePostVoucherMetaVO();
        meta.setCompanyId(company.getCompanyId());
        meta.setCompanyName(company.getCompanyName());
        meta.setIyear(effectiveYear);
        meta.setIperiod(effectivePeriod);
        meta.setIyperiod(buildYearPeriod(effectiveYear, effectivePeriod));
        meta.setPeriodLabel(effectiveYear + "-" + String.format(Locale.ROOT, "%02d", effectivePeriod));
        meta.setStatus(status);
        meta.setStatusLabel(resolvePostStatusLabel(status));
        meta.setUnpostedVoucherCount(counts.unpostedCount());
        meta.setUnpostedSampleVoucherNos(buildSampleVoucherNos(counts.unpostedKeys()));
        meta.setErrorVoucherCount(counts.errorCount());
        meta.setErrorSampleVoucherNos(buildSampleVoucherNos(counts.errorKeys()));
        meta.setReviewableVoucherCount(counts.reviewableCount());
        meta.setPostedVoucherCount(counts.postedCount());
        meta.setCanPost(false);
        meta.setLastTaskNo(state == null ? null : trimToNull(state.getLastTaskNo()));
        meta.setLastTaskStatus(state == null ? null : trimToNull(state.getLastTaskStatus()));
        meta.setLastTaskMessage(state == null ? null : trimToNull(state.getLastErrorMessage()));

        if (counts.unpostedCount() > 0) {
            meta.setBlockedReason("当前期间存在未审核凭证，不能继续记账");
            return meta;
        }
        if (counts.errorCount() > 0) {
            meta.setBlockedReason("当前期间存在错误凭证，不能继续记账");
            return meta;
        }
        try {
            validatePeriodForPosting(company.getCompanyId(), effectiveYear, effectivePeriod, accountSet);
        } catch (IllegalStateException ex) {
            meta.setBlockedReason(ex.getMessage());
            return meta;
        }
        if (counts.reviewableCount() <= 0) {
            meta.setBlockedReason("当前期间没有可记账凭证");
            return meta;
        }
        meta.setCanPost(true);
        return meta;
    }

    protected PostVoucherRunContext prepareRun(String companyId, Integer iyear, Integer iperiod) {
        SystemCompany company = resolveEffectiveCompany(null, companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        FinanceAccountSet accountSet = requireActiveAccountSet(company.getCompanyId());
        validatePeriodForPosting(company.getCompanyId(), effectiveYear, effectivePeriod, accountSet);
        Map<VoucherKey, List<GlAccvouch>> voucherGroups = loadVoucherGroups(company.getCompanyId(), effectiveYear, effectivePeriod);
        PostingCounts counts = summarizeVoucherGroups(voucherGroups);
        if (counts.unpostedCount() > 0) {
            throw new IllegalStateException(buildBlockingMessage("当前期间存在未审核凭证，不能继续记账", counts.unpostedKeys()));
        }
        if (counts.errorCount() > 0) {
            throw new IllegalStateException(buildBlockingMessage("当前期间存在错误凭证，不能继续记账", counts.errorKeys()));
        }
        if (counts.reviewableCount() <= 0) {
            throw new IllegalStateException("当前期间没有可记账凭证");
        }

        FinancePostVoucherState state = ensurePostState(company.getCompanyId(), effectiveYear, effectivePeriod);
        List<Map.Entry<VoucherKey, List<GlAccvouch>>> reviewableEntries = voucherGroups.entrySet().stream()
                .filter(entry -> Objects.equals(resolveVoucherStatus(entry.getValue().get(0)), VOUCHER_STATUS_REVIEWED))
                .toList();
        return new PostVoucherRunContext(
                company.getCompanyId(),
                company.getCompanyName(),
                effectiveYear,
                effectivePeriod,
                buildYearPeriod(effectiveYear, effectivePeriod),
                reviewableEntries,
                counts.postedCount(),
                state
        );
    }

    protected void markPostingStarted(PostVoucherRunContext context, AsyncTaskRecord task) {
        FinancePostVoucherState state = context.getState();
        state.setStatus(POST_STATUS_POSTING);
        state.setPostedVoucherCount(context.getPostedVoucherCount());
        state.setLastTaskNo(task.getTaskNo());
        state.setLastTaskStatus(AsyncTaskSupport.TASK_STATUS_RUNNING);
        state.setLastErrorMessage(null);
        upsertPostState(state);
    }

    protected void postVoucherGroup(VoucherKey voucherKey, List<GlAccvouch> rows, String operatorName) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String companyId = voucherKey.companyId();
        int iyear = voucherKey.iyear();
        int iperiod = voucherKey.iperiod();
        Map<String, FinanceAccountSubject> subjectMap = loadEnabledSubjects(companyId).stream()
                .collect(Collectors.toMap(FinanceAccountSubject::getSubjectCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (GlAccvouch row : rows) {
            FinanceAccountSubject subject = subjectMap.get(trimToNull(row.getCcode()));
            if (subject == null) {
                throw new IllegalStateException("凭证存在无效科目，无法记账: " + trimToNull(row.getCcode()));
            }
            applyToAccsum(row, subject);
            if (hasAssistDimension(row)) {
                applyToAccass(row, subject);
            }
        }
        recalculateAncestorRows(companyId, iyear, iperiod, subjectMap.values());
        markVoucherPosted(voucherKey, operatorName);
    }

    protected void markPostingSuccess(PostVoucherRunContext context, String operatorName, String taskNo) {
        PostingCounts counts = summarizeVoucherGroups(loadVoucherGroups(context.getCompanyId(), context.getIyear(), context.getIperiod()));
        FinancePostVoucherState state = ensurePostState(context.getCompanyId(), context.getIyear(), context.getIperiod());
        state.setStatus(counts.reviewableCount() == 0 ? POST_STATUS_FULLY_POSTED : POST_STATUS_PARTIALLY_POSTED);
        state.setPostedVoucherCount(counts.postedCount());
        state.setLastTaskNo(taskNo);
        state.setLastTaskStatus(AsyncTaskSupport.TASK_STATUS_SUCCESS);
        state.setLastErrorMessage(null);
        state.setLastPostedBy(operatorName);
        state.setLastPostedAt(LocalDateTime.now());
        upsertPostState(state);
    }

    protected void markPostingFailed(PostVoucherRunContext context, String taskNo, String errorMessage) {
        FinancePostVoucherState state = ensurePostState(context.getCompanyId(), context.getIyear(), context.getIperiod());
        PostingCounts counts = summarizeVoucherGroups(loadVoucherGroups(context.getCompanyId(), context.getIyear(), context.getIperiod()));
        state.setStatus(POST_STATUS_FAILED);
        state.setPostedVoucherCount(counts.postedCount());
        state.setLastTaskNo(taskNo);
        state.setLastTaskStatus(AsyncTaskSupport.TASK_STATUS_FAILED);
        state.setLastErrorMessage(trimToNull(errorMessage));
        upsertPostState(state);
    }

    protected FinancePostVoucherTaskStatusVO buildTaskStatus(String taskNo) {
        AsyncTaskRecord task = asyncTaskRecordMapper.selectOne(Wrappers.<AsyncTaskRecord>lambdaQuery()
                .eq(AsyncTaskRecord::getTaskNo, trimToNull(taskNo))
                .last("limit 1"));
        if (task == null) {
            throw new IllegalArgumentException("记账任务不存在");
        }
        FinancePostVoucherState state = findPostState(task.getCompanyId(), resolveYear(task.getBusinessKey()), resolvePeriod(task.getBusinessKey()));
        PostingCounts counts = summarizeVoucherGroups(loadVoucherGroups(task.getCompanyId(), resolveYear(task.getBusinessKey()), resolvePeriod(task.getBusinessKey())));
        FinancePostVoucherTaskStatusVO vo = new FinancePostVoucherTaskStatusVO();
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskType(task.getTaskType());
        vo.setBusinessType(task.getBusinessType());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setResultMessage(task.getResultMessage());
        String periodStatus = resolveEffectivePostStatus(state, counts);
        vo.setPeriodStatus(periodStatus);
        vo.setPeriodStatusLabel(resolvePostStatusLabel(periodStatus));
        vo.setPostedVoucherCount(counts.postedCount());
        vo.setReviewableVoucherCount(counts.reviewableCount());
        vo.setFinished(!AsyncTaskSupport.isActive(task.getStatus()));
        vo.setCreatedAt(formatDateTime(task.getCreatedAt()));
        vo.setUpdatedAt(formatDateTime(task.getUpdatedAt()));
        vo.setFinishedAt(formatDateTime(task.getFinishedAt()));
        return vo;
    }

    protected FinancePostVoucherState findActiveState(String companyId, int iyear, int iperiod) {
        return financePostVoucherStateMapper.selectOne(Wrappers.<FinancePostVoucherState>lambdaQuery()
                .eq(FinancePostVoucherState::getCompanyId, companyId)
                .eq(FinancePostVoucherState::getIyear, iyear)
                .eq(FinancePostVoucherState::getIperiod, iperiod)
                .last("limit 1"));
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

    protected FinancePostVoucherState ensurePostState(String companyId, int iyear, int iperiod) {
        FinancePostVoucherState state = findPostState(companyId, iyear, iperiod);
        if (state != null) {
            return state;
        }
        FinancePostVoucherState created = new FinancePostVoucherState();
        created.setCompanyId(companyId);
        created.setIyear(iyear);
        created.setIperiod(iperiod);
        created.setIyperiod(buildYearPeriod(iyear, iperiod));
        created.setStatus(POST_STATUS_NOT_POSTED);
        created.setPostedVoucherCount(0);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        financePostVoucherStateMapper.insert(created);
        return created;
    }

    protected FinancePostVoucherState findPostState(String companyId, int iyear, int iperiod) {
        return financePostVoucherStateMapper.selectOne(Wrappers.<FinancePostVoucherState>lambdaQuery()
                .eq(FinancePostVoucherState::getCompanyId, companyId)
                .eq(FinancePostVoucherState::getIyear, iyear)
                .eq(FinancePostVoucherState::getIperiod, iperiod)
                .last("limit 1"));
    }

    protected void upsertPostState(FinancePostVoucherState state) {
        state.setUpdatedAt(LocalDateTime.now());
        if (state.getId() == null) {
            if (state.getCreatedAt() == null) {
                state.setCreatedAt(LocalDateTime.now());
            }
            financePostVoucherStateMapper.insert(state);
            return;
        }
        financePostVoucherStateMapper.updateById(state);
    }

    protected void validatePeriodForPosting(String companyId, int iyear, int iperiod, FinanceAccountSet accountSet) {
        FinancePeriodClose periodClose = financePeriodCloseMapper.selectOne(Wrappers.<FinancePeriodClose>lambdaQuery()
                .eq(FinancePeriodClose::getCompanyId, companyId)
                .eq(FinancePeriodClose::getIyear, iyear)
                .eq(FinancePeriodClose::getIperiod, iperiod)
                .eq(FinancePeriodClose::getStatus, CLOSE_STATUS_CLOSED)
                .last("limit 1"));
        if (periodClose != null) {
            throw new IllegalStateException("当前期间已结账，不能继续记账");
        }
        if (Objects.equals(accountSet.getEnabledYear(), iyear) && Objects.equals(accountSet.getEnabledPeriod(), iperiod)) {
            FinanceOpeningBalanceState openingState = financeOpeningBalanceStateMapper.selectOne(Wrappers.<FinanceOpeningBalanceState>lambdaQuery()
                    .eq(FinanceOpeningBalanceState::getCompanyId, companyId)
                    .eq(FinanceOpeningBalanceState::getIyear, iyear)
                    .eq(FinanceOpeningBalanceState::getIperiod, iperiod)
                    .last("limit 1"));
            if (openingState == null || !Objects.equals(trimToNull(openingState.getStatus()), OPENING_STATUS_OPENED)) {
                throw new IllegalStateException("首期记账前必须先完成期初开账");
            }
            return;
        }
        YearPeriod previous = previousPeriod(iyear, iperiod);
        FinancePostVoucherState previousState = findPostState(companyId, previous.year(), previous.period());
        if (previousState == null || !Objects.equals(trimToNull(previousState.getStatus()), POST_STATUS_FULLY_POSTED)) {
            throw new IllegalStateException("非首期记账前必须确保上一期间已完成记账");
        }
    }

    protected Map<VoucherKey, List<GlAccvouch>> loadVoucherGroups(String companyId, int iyear, int iperiod) {
        return glAccvouchMapper.selectList(Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIyear, iyear)
                        .eq(GlAccvouch::getIperiod, iperiod)
                        .orderByAsc(GlAccvouch::getInoId, GlAccvouch::getInid, GlAccvouch::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        row -> new VoucherKey(row.getCompanyId(), row.getIyear(), row.getIyperiod(), row.getIperiod(), row.getCsign(), row.getInoId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    protected PostingCounts summarizeVoucherGroups(Map<VoucherKey, List<GlAccvouch>> voucherGroups) {
        List<VoucherKey> unposted = new ArrayList<>();
        List<VoucherKey> error = new ArrayList<>();
        List<VoucherKey> reviewed = new ArrayList<>();
        List<VoucherKey> posted = new ArrayList<>();
        for (Map.Entry<VoucherKey, List<GlAccvouch>> entry : voucherGroups.entrySet()) {
            String status = resolveVoucherStatus(entry.getValue().get(0));
            switch (status) {
                case VOUCHER_STATUS_POSTED -> posted.add(entry.getKey());
                case VOUCHER_STATUS_ERROR -> error.add(entry.getKey());
                case VOUCHER_STATUS_REVIEWED -> reviewed.add(entry.getKey());
                default -> unposted.add(entry.getKey());
            }
        }
        return new PostingCounts(unposted, error, reviewed, posted);
    }

    protected String resolveEffectivePostStatus(FinancePostVoucherState state, PostingCounts counts) {
        String raw = trimToNull(state == null ? null : state.getStatus());
        if (Objects.equals(raw, POST_STATUS_POSTING) || Objects.equals(raw, POST_STATUS_FAILED)) {
            return raw;
        }
        if (counts.reviewableCount() == 0 && counts.postedCount() > 0) {
            return POST_STATUS_FULLY_POSTED;
        }
        if (counts.postedCount() > 0) {
            return POST_STATUS_PARTIALLY_POSTED;
        }
        return POST_STATUS_NOT_POSTED;
    }

    protected List<String> buildSampleVoucherNos(List<VoucherKey> keys) {
        return keys.stream()
                .limit(3)
                .map(this::buildVoucherNo)
                .toList();
    }

    protected String buildBlockingMessage(String prefix, List<VoucherKey> keys) {
        List<String> samples = buildSampleVoucherNos(keys);
        if (samples.isEmpty()) {
            return prefix;
        }
        return prefix + "：" + String.join("、", samples);
    }

    protected String buildVoucherNo(VoucherKey key) {
        return key.companyId() + "~" + key.iyear() + "~" + key.iperiod() + "~" + key.csign() + "~" + key.inoId();
    }

    protected void applyToAccsum(GlAccvouch voucherRow, FinanceAccountSubject subject) {
        GlAccsum row = loadOrCreateAccsumRow(voucherRow, subject);
        row.setMd(money(row.getMd()).add(money(voucherRow.getMd())));
        row.setMc(money(row.getMc()).add(money(voucherRow.getMc())));
        row.setMdF(money(row.getMdF()).add(money(voucherRow.getMdF())));
        row.setMcF(money(row.getMcF()).add(money(voucherRow.getMcF())));
        row.setNdS(qty(row.getNdS()).add(qty(voucherRow.getNdS())));
        row.setNcS(qty(row.getNcS()).add(qty(voucherRow.getNcS())));
        fillEndingAmounts(row, subject);
        glAccsumMapper.updateById(row);
    }

    protected void applyToAccass(GlAccvouch voucherRow, FinanceAccountSubject subject) {
        GlAccass row = loadOrCreateAccassRow(voucherRow, subject);
        row.setMd(money(row.getMd()).add(money(voucherRow.getMd())));
        row.setMc(money(row.getMc()).add(money(voucherRow.getMc())));
        row.setMdF(money(row.getMdF()).add(money(voucherRow.getMdF())));
        row.setMcF(money(row.getMcF()).add(money(voucherRow.getMcF())));
        row.setNdS(qty(row.getNdS()).add(qty(voucherRow.getNdS())));
        row.setNcS(qty(row.getNcS()).add(qty(voucherRow.getNcS())));
        fillEndingAmounts(row, subject);
        glAccassMapper.updateById(row);
    }

    protected GlAccsum loadOrCreateAccsumRow(GlAccvouch voucherRow, FinanceAccountSubject subject) {
        String currencyCode = resolveCurrencyCode(voucherRow.getCurrencyCode(), voucherRow.getCexchName(), subject.getCexchName());
        GlAccsum current = glAccsumMapper.selectOne(Wrappers.<GlAccsum>lambdaQuery()
                .eq(GlAccsum::getCompanyId, voucherRow.getCompanyId())
                .eq(GlAccsum::getIyear, voucherRow.getIyear())
                .eq(GlAccsum::getIperiod, voucherRow.getIperiod())
                .eq(GlAccsum::getCcode, voucherRow.getCcode())
                .eq(GlAccsum::getCurrencyCode, currencyCode)
                .last("limit 1"));
        if (current != null) {
            return current;
        }

        GlAccsum created = new GlAccsum();
        created.setCompanyId(voucherRow.getCompanyId());
        created.setIyear(voucherRow.getIyear());
        created.setIperiod(voucherRow.getIperiod());
        created.setIyperiod(voucherRow.getIyperiod());
        created.setCcode(voucherRow.getCcode());
        created.setCexchName(resolveCurrencyName(voucherRow.getCexchName(), subject.getCexchName()));
        created.setCurrencyCode(currencyCode);
        GlAccsum previous = loadPreviousAccsum(voucherRow.getCompanyId(), voucherRow.getIyear(), voucherRow.getIperiod(), voucherRow.getCcode(), currencyCode);
        if (previous != null) {
            created.setMb(money(previous.getMe()));
            created.setMbF(money(previous.getMeF()));
            created.setNbS(qty(previous.getNeS()));
        } else {
            created.setMb(ZERO);
            created.setMbF(ZERO);
            created.setNbS(ZERO_QTY);
        }
        created.setMd(ZERO);
        created.setMc(ZERO);
        created.setMdF(ZERO);
        created.setMcF(ZERO);
        created.setNdS(ZERO_QTY);
        created.setNcS(ZERO_QTY);
        fillEndingAmounts(created, subject);
        glAccsumMapper.insert(created);
        return created;
    }

    protected GlAccass loadOrCreateAccassRow(GlAccvouch voucherRow, FinanceAccountSubject subject) {
        String currencyCode = resolveCurrencyCode(voucherRow.getCurrencyCode(), voucherRow.getCexchName(), subject.getCexchName());
        var query = Wrappers.<GlAccass>lambdaQuery()
                .eq(GlAccass::getCompanyId, voucherRow.getCompanyId())
                .eq(GlAccass::getIyear, voucherRow.getIyear())
                .eq(GlAccass::getIperiod, voucherRow.getIperiod())
                .eq(GlAccass::getCcode, voucherRow.getCcode())
                .eq(GlAccass::getCurrencyCode, currencyCode);
        applyNullableCondition(query, GlAccass::getCdeptId, voucherRow.getCdeptId());
        applyNullableCondition(query, GlAccass::getCpersonId, voucherRow.getCpersonId());
        applyNullableCondition(query, GlAccass::getCcusId, voucherRow.getCcusId());
        applyNullableCondition(query, GlAccass::getCsupId, voucherRow.getCsupId());
        applyNullableCondition(query, GlAccass::getCitemClass, voucherRow.getCitemClass());
        applyNullableCondition(query, GlAccass::getCitemId, voucherRow.getCitemId());
        GlAccass current = glAccassMapper.selectOne(query.last("limit 1"));
        if (current != null) {
            return current;
        }

        GlAccass created = new GlAccass();
        created.setCompanyId(voucherRow.getCompanyId());
        created.setIyear(voucherRow.getIyear());
        created.setIperiod(voucherRow.getIperiod());
        created.setIyperiod(voucherRow.getIyperiod());
        created.setCcode(voucherRow.getCcode());
        created.setCdeptId(trimToNull(voucherRow.getCdeptId()));
        created.setCpersonId(trimToNull(voucherRow.getCpersonId()));
        created.setCcusId(trimToNull(voucherRow.getCcusId()));
        created.setCsupId(trimToNull(voucherRow.getCsupId()));
        created.setCitemClass(trimToNull(voucherRow.getCitemClass()));
        created.setCitemId(trimToNull(voucherRow.getCitemId()));
        created.setCexchName(resolveCurrencyName(voucherRow.getCexchName(), subject.getCexchName()));
        created.setCurrencyCode(currencyCode);
        GlAccass previous = loadPreviousAccass(voucherRow, currencyCode);
        if (previous != null) {
            created.setMb(money(previous.getMe()));
            created.setMbF(money(previous.getMeF()));
            created.setNbS(qty(previous.getNeS()));
        } else {
            created.setMb(ZERO);
            created.setMbF(ZERO);
            created.setNbS(ZERO_QTY);
        }
        created.setMd(ZERO);
        created.setMc(ZERO);
        created.setMdF(ZERO);
        created.setMcF(ZERO);
        created.setNdS(ZERO_QTY);
        created.setNcS(ZERO_QTY);
        fillEndingAmounts(created, subject);
        glAccassMapper.insert(created);
        return created;
    }

    protected GlAccsum loadPreviousAccsum(String companyId, int iyear, int iperiod, String subjectCode, String currencyCode) {
        YearPeriod previous = previousPeriod(iyear, iperiod);
        return glAccsumMapper.selectOne(Wrappers.<GlAccsum>lambdaQuery()
                .eq(GlAccsum::getCompanyId, companyId)
                .eq(GlAccsum::getIyear, previous.year())
                .eq(GlAccsum::getIperiod, previous.period())
                .eq(GlAccsum::getCcode, subjectCode)
                .eq(GlAccsum::getCurrencyCode, currencyCode)
                .last("limit 1"));
    }

    protected GlAccass loadPreviousAccass(GlAccvouch voucherRow, String currencyCode) {
        YearPeriod previous = previousPeriod(voucherRow.getIyear(), voucherRow.getIperiod());
        var query = Wrappers.<GlAccass>lambdaQuery()
                .eq(GlAccass::getCompanyId, voucherRow.getCompanyId())
                .eq(GlAccass::getIyear, previous.year())
                .eq(GlAccass::getIperiod, previous.period())
                .eq(GlAccass::getCcode, voucherRow.getCcode())
                .eq(GlAccass::getCurrencyCode, currencyCode);
        applyNullableCondition(query, GlAccass::getCdeptId, voucherRow.getCdeptId());
        applyNullableCondition(query, GlAccass::getCpersonId, voucherRow.getCpersonId());
        applyNullableCondition(query, GlAccass::getCcusId, voucherRow.getCcusId());
        applyNullableCondition(query, GlAccass::getCsupId, voucherRow.getCsupId());
        applyNullableCondition(query, GlAccass::getCitemClass, voucherRow.getCitemClass());
        applyNullableCondition(query, GlAccass::getCitemId, voucherRow.getCitemId());
        return glAccassMapper.selectOne(query.last("limit 1"));
    }

    protected void recalculateAncestorRows(String companyId, int iyear, int iperiod, Collection<FinanceAccountSubject> subjects) {
        List<FinanceAccountSubject> orderedSubjects = new ArrayList<>(subjects);
        orderedSubjects.sort(Comparator.comparing(FinanceAccountSubject::getSubjectLevel, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FinanceAccountSubject::getSubjectCode));
        Map<String, List<FinanceAccountSubject>> childrenMap = new HashMap<>();
        for (FinanceAccountSubject subject : orderedSubjects) {
            String parentCode = trimToNull(subject.getParentSubjectCode());
            if (parentCode != null) {
                childrenMap.computeIfAbsent(parentCode, key -> new ArrayList<>()).add(subject);
            }
        }
        Map<String, List<GlAccsum>> sumRowsBySubject = loadAccsumRows(companyId, iyear, iperiod).stream()
                .collect(Collectors.groupingBy(GlAccsum::getCcode, LinkedHashMap::new, Collectors.toList()));

        for (FinanceAccountSubject subject : orderedSubjects) {
            if (isLeaf(subject)) {
                continue;
            }
            List<FinanceAccountSubject> children = childrenMap.getOrDefault(subject.getSubjectCode(), List.of());
            Map<String, List<GlAccsum>> childRowsByCurrency = new LinkedHashMap<>();
            for (FinanceAccountSubject child : children) {
                for (GlAccsum childRow : sumRowsBySubject.getOrDefault(child.getSubjectCode(), List.of())) {
                    childRowsByCurrency.computeIfAbsent(resolveCurrencyCode(childRow.getCurrencyCode(), childRow.getCexchName(), subject.getCexchName()), key -> new ArrayList<>())
                            .add(childRow);
                }
            }
            if (childRowsByCurrency.isEmpty()) {
                childRowsByCurrency.put(resolveCurrencyCode(null, null, subject.getCexchName()), List.of());
            }
            for (Map.Entry<String, List<GlAccsum>> entry : childRowsByCurrency.entrySet()) {
                upsertAncestorAccsum(companyId, iyear, iperiod, subject, entry.getKey(), entry.getValue());
            }
            sumRowsBySubject.put(subject.getSubjectCode(), glAccsumMapper.selectList(Wrappers.<GlAccsum>lambdaQuery()
                    .eq(GlAccsum::getCompanyId, companyId)
                    .eq(GlAccsum::getIyear, iyear)
                    .eq(GlAccsum::getIperiod, iperiod)
                    .eq(GlAccsum::getCcode, subject.getSubjectCode())
                    .orderByAsc(GlAccsum::getId)));
        }
    }

    protected void upsertAncestorAccsum(
            String companyId,
            int iyear,
            int iperiod,
            FinanceAccountSubject subject,
            String currencyCode,
            List<GlAccsum> childRows
    ) {
        GlAccsum current = glAccsumMapper.selectOne(Wrappers.<GlAccsum>lambdaQuery()
                .eq(GlAccsum::getCompanyId, companyId)
                .eq(GlAccsum::getIyear, iyear)
                .eq(GlAccsum::getIperiod, iperiod)
                .eq(GlAccsum::getCcode, subject.getSubjectCode())
                .eq(GlAccsum::getCurrencyCode, currencyCode)
                .last("limit 1"));
        if (current == null) {
            current = new GlAccsum();
            current.setCompanyId(companyId);
            current.setIyear(iyear);
            current.setIperiod(iperiod);
            current.setIyperiod(buildYearPeriod(iyear, iperiod));
            current.setCcode(subject.getSubjectCode());
            current.setCurrencyCode(currencyCode);
            current.setCexchName(resolveCurrencyName(null, subject.getCexchName()));
            GlAccsum previous = loadPreviousAccsum(companyId, iyear, iperiod, subject.getSubjectCode(), currencyCode);
            current.setMb(previous == null ? ZERO : money(previous.getMe()));
            current.setMbF(previous == null ? ZERO : money(previous.getMeF()));
            current.setNbS(previous == null ? ZERO_QTY : qty(previous.getNeS()));
            current.setMd(ZERO);
            current.setMc(ZERO);
            current.setMdF(ZERO);
            current.setMcF(ZERO);
            current.setNdS(ZERO_QTY);
            current.setNcS(ZERO_QTY);
            fillEndingAmounts(current, subject);
            glAccsumMapper.insert(current);
        }
        BigDecimal totalMd = ZERO;
        BigDecimal totalMc = ZERO;
        BigDecimal totalMdF = ZERO;
        BigDecimal totalMcF = ZERO;
        BigDecimal totalNdS = ZERO_QTY;
        BigDecimal totalNcS = ZERO_QTY;
        BigDecimal totalMb = ZERO;
        BigDecimal totalMbF = ZERO;
        BigDecimal totalNbS = ZERO_QTY;
        for (GlAccsum childRow : childRows) {
            totalMb = totalMb.add(money(childRow.getMb()));
            totalMbF = totalMbF.add(money(childRow.getMbF()));
            totalNbS = totalNbS.add(qty(childRow.getNbS()));
            totalMd = totalMd.add(money(childRow.getMd()));
            totalMc = totalMc.add(money(childRow.getMc()));
            totalMdF = totalMdF.add(money(childRow.getMdF()));
            totalMcF = totalMcF.add(money(childRow.getMcF()));
            totalNdS = totalNdS.add(qty(childRow.getNdS()));
            totalNcS = totalNcS.add(qty(childRow.getNcS()));
        }
        current.setMb(totalMb);
        current.setMbF(totalMbF);
        current.setNbS(totalNbS);
        current.setMd(totalMd);
        current.setMc(totalMc);
        current.setMdF(totalMdF);
        current.setMcF(totalMcF);
        current.setNdS(totalNdS);
        current.setNcS(totalNcS);
        fillEndingAmounts(current, subject);
        glAccsumMapper.updateById(current);
    }

    protected void markVoucherPosted(VoucherKey voucherKey, String operatorName) {
        glAccvouchMapper.update(
                null,
                Wrappers.<GlAccvouch>lambdaUpdate()
                        .eq(GlAccvouch::getCompanyId, voucherKey.companyId())
                        .eq(GlAccvouch::getIyear, voucherKey.iyear())
                        .eq(GlAccvouch::getIperiod, voucherKey.iperiod())
                        .eq(GlAccvouch::getCsign, voucherKey.csign())
                        .eq(GlAccvouch::getInoId, voucherKey.inoId())
                        .set(GlAccvouch::getIbook, 1)
                        .set(GlAccvouch::getCbook, operatorName)
                        .set(GlAccvouch::getPostedAt, LocalDateTime.now())
        );
    }

    protected List<GlAccsum> loadAccsumRows(String companyId, int iyear, int iperiod) {
        return glAccsumMapper.selectList(Wrappers.<GlAccsum>lambdaQuery()
                .eq(GlAccsum::getCompanyId, companyId)
                .eq(GlAccsum::getIyear, iyear)
                .eq(GlAccsum::getIperiod, iperiod));
    }

    protected SystemCompany resolveEffectiveCompany(Long currentUserId, String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId != null) {
            SystemCompany company = systemCompanyMapper.selectOne(Wrappers.<SystemCompany>lambdaQuery()
                    .eq(SystemCompany::getCompanyId, normalizedCompanyId)
                    .eq(SystemCompany::getStatus, 1)
                    .last("limit 1"));
            if (company == null) {
                throw new IllegalArgumentException("当前公司不存在或已停用");
            }
            return company;
        }
        User currentUser = currentUserId == null ? null : userMapper.selectById(currentUserId);
        String currentUserCompanyId = trimToNull(currentUser == null ? null : currentUser.getCompanyId());
        if (currentUserCompanyId != null) {
            SystemCompany company = systemCompanyMapper.selectOne(Wrappers.<SystemCompany>lambdaQuery()
                    .eq(SystemCompany::getCompanyId, currentUserCompanyId)
                    .eq(SystemCompany::getStatus, 1)
                    .last("limit 1"));
            if (company != null) {
                return company;
            }
        }
        SystemCompany fallback = systemCompanyMapper.selectOne(Wrappers.<SystemCompany>lambdaQuery()
                .eq(SystemCompany::getStatus, 1)
                .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
                .last("limit 1"));
        if (fallback == null) {
            throw new IllegalStateException("当前没有可用公司");
        }
        return fallback;
    }

    protected FinanceAccountSet requireActiveAccountSet(String companyId) {
        FinanceAccountSet accountSet = financeAccountSetMapper.selectOne(Wrappers.<FinanceAccountSet>lambdaQuery()
                .eq(FinanceAccountSet::getCompanyId, companyId)
                .eq(FinanceAccountSet::getStatus, ACCOUNT_SET_STATUS_ACTIVE)
                .last("limit 1"));
        if (accountSet == null) {
            throw new IllegalStateException("当前公司未启用账套，无法记账");
        }
        return accountSet;
    }

    protected List<FinanceAccountSubject> loadEnabledSubjects(String companyId) {
        return financeAccountSubjectMapper.selectList(Wrappers.<FinanceAccountSubject>lambdaQuery()
                .eq(FinanceAccountSubject::getCompanyId, companyId)
                .eq(FinanceAccountSubject::getStatus, 1)
                .ne(FinanceAccountSubject::getBclose, 1)
                .orderByAsc(FinanceAccountSubject::getSubjectLevel, FinanceAccountSubject::getSortOrder, FinanceAccountSubject::getSubjectCode));
    }

    protected boolean hasAssistDimension(GlAccvouch row) {
        return trimToNull(row.getCdeptId()) != null
                || trimToNull(row.getCpersonId()) != null
                || trimToNull(row.getCcusId()) != null
                || trimToNull(row.getCsupId()) != null
                || trimToNull(row.getCitemClass()) != null
                || trimToNull(row.getCitemId()) != null;
    }

    protected boolean isLeaf(FinanceAccountSubject subject) {
        if (subject == null) {
            return false;
        }
        Long count = financeAccountSubjectMapper.selectCount(Wrappers.<FinanceAccountSubject>lambdaQuery()
                .eq(FinanceAccountSubject::getCompanyId, subject.getCompanyId())
                .eq(FinanceAccountSubject::getParentSubjectCode, subject.getSubjectCode())
                .eq(FinanceAccountSubject::getStatus, 1)
                .ne(FinanceAccountSubject::getBclose, 1));
        return count == null || count == 0;
    }

    protected int normalizeYear(Integer iyear) {
        int value = iyear == null ? LocalDate.now().getYear() : iyear;
        if (value < 2000 || value > 2099) {
            throw new IllegalArgumentException("年度不合法");
        }
        return value;
    }

    protected int normalizePeriod(Integer iperiod) {
        int value = iperiod == null ? 1 : iperiod;
        if (value < 1 || value > 12) {
            throw new IllegalArgumentException("期间不合法");
        }
        return value;
    }

    protected int buildYearPeriod(int iyear, int iperiod) {
        return iyear * 100 + iperiod;
    }

    protected YearPeriod previousPeriod(Integer iyear, Integer iperiod) {
        if (iperiod == null || iperiod <= 1) {
            return new YearPeriod((iyear == null ? LocalDate.now().getYear() : iyear) - 1, 12);
        }
        return new YearPeriod(iyear, iperiod - 1);
    }

    protected String resolveVoucherStatus(GlAccvouch row) {
        if (Objects.equals(row.getIbook(), 1) || row.getPostedAt() != null) {
            return VOUCHER_STATUS_POSTED;
        }
        if (Objects.equals(row.getIflag(), ERROR_FLAG)) {
            return VOUCHER_STATUS_ERROR;
        }
        if (trimToNull(row.getCcheck()) != null || row.getCheckedAt() != null) {
            return VOUCHER_STATUS_REVIEWED;
        }
        return VOUCHER_STATUS_UNPOSTED;
    }

    protected String resolvePostStatusLabel(String status) {
        return switch (trimToNull(status) == null ? POST_STATUS_NOT_POSTED : status) {
            case POST_STATUS_POSTING -> "记账中";
            case POST_STATUS_PARTIALLY_POSTED -> "部分记账";
            case POST_STATUS_FULLY_POSTED -> "已全部记账";
            case POST_STATUS_FAILED -> "记账失败";
            default -> "未记账";
        };
    }

    protected String resolveCurrencyCode(String currencyCode, String voucherCurrencyName, String subjectCurrencyName) {
        String normalizedCode = trimToNull(currencyCode);
        if (normalizedCode != null) {
            return normalizedCode.toUpperCase(Locale.ROOT);
        }
        String normalizedName = trimToNull(voucherCurrencyName);
        if (normalizedName == null) {
            normalizedName = trimToNull(subjectCurrencyName);
        }
        if (normalizedName == null || normalizedName.contains("人民币")) {
            return "CNY";
        }
        return normalizedName.toUpperCase(Locale.ROOT);
    }

    protected String resolveCurrencyName(String voucherCurrencyName, String subjectCurrencyName) {
        String normalizedName = trimToNull(voucherCurrencyName);
        if (normalizedName != null) {
            return normalizedName;
        }
        normalizedName = trimToNull(subjectCurrencyName);
        return normalizedName == null ? "人民币" : normalizedName;
    }

    protected void fillEndingAmounts(GlAccsum row, FinanceAccountSubject subject) {
        row.setCbegindC(resolveBeginDirection(row.getMb(), subject));
        row.setCbegindCEngl(resolveBeginDirectionEn(row.getMb(), subject));
        row.setMe(money(row.getMb()).add(money(row.getMd())).subtract(money(row.getMc())));
        row.setMeF(money(row.getMbF()).add(money(row.getMdF())).subtract(money(row.getMcF())));
        row.setNeS(qty(row.getNbS()).add(qty(row.getNdS())).subtract(qty(row.getNcS())));
        row.setCenddC(resolveEndDirection(row.getMe(), subject));
        row.setCenddCEngl(FinanceBalanceDirectionSupport.resolveActualDirectionCode(subject == null ? null : subject.getBalanceDirection(), row.getMe()));
    }

    protected void fillEndingAmounts(GlAccass row, FinanceAccountSubject subject) {
        row.setCbegindC(resolveBeginDirection(row.getMb(), subject));
        row.setCbegindCEngl(resolveBeginDirectionEn(row.getMb(), subject));
        row.setMe(money(row.getMb()).add(money(row.getMd())).subtract(money(row.getMc())));
        row.setMeF(money(row.getMbF()).add(money(row.getMdF())).subtract(money(row.getMcF())));
        row.setNeS(qty(row.getNbS()).add(qty(row.getNdS())).subtract(qty(row.getNcS())));
        row.setCenddC(resolveEndDirection(row.getMe(), subject));
        row.setCenddCEngl(FinanceBalanceDirectionSupport.resolveActualDirectionCode(subject == null ? null : subject.getBalanceDirection(), row.getMe()));
    }

    protected String resolveBeginDirection(BigDecimal beginningAmount, FinanceAccountSubject subject) {
        return FinanceBalanceDirectionSupport.resolveActualDirectionLabel(subject == null ? null : subject.getBalanceDirection(), beginningAmount);
    }

    protected String resolveBeginDirectionEn(BigDecimal beginningAmount, FinanceAccountSubject subject) {
        return FinanceBalanceDirectionSupport.resolveActualDirectionCode(subject == null ? null : subject.getBalanceDirection(), beginningAmount);
    }

    protected String resolveEndDirection(BigDecimal endingAmount, FinanceAccountSubject subject) {
        return FinanceBalanceDirectionSupport.resolveActualDirectionLabel(subject == null ? null : subject.getBalanceDirection(), endingAmount);
    }

    protected boolean isDebitDirection(String balanceDirection) {
        return FinanceBalanceDirectionSupport.isDebitDirection(balanceDirection);
    }

    protected int resolveYear(String businessKey) {
        String[] parts = splitBusinessKey(businessKey);
        return Integer.parseInt(parts[1]);
    }

    protected int resolvePeriod(String businessKey) {
        String[] parts = splitBusinessKey(businessKey);
        return Integer.parseInt(parts[2]);
    }

    protected String[] splitBusinessKey(String businessKey) {
        String normalized = trimToNull(businessKey);
        if (normalized == null) {
            throw new IllegalStateException("任务业务键缺失");
        }
        String[] parts = normalized.split("#");
        if (parts.length < 4) {
            throw new IllegalStateException("任务业务键不合法");
        }
        return parts;
    }

    protected String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    protected <T> void applyNullableCondition(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<T> query,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, String> column,
            String value
    ) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            query.isNull(column);
        } else {
            query.eq(column, normalized);
        }
    }

    protected BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal qty(BigDecimal value) {
        return value == null ? ZERO_QTY : value.setScale(6, RoundingMode.HALF_UP);
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected record VoucherKey(String companyId, Integer iyear, Integer iyperiod, Integer iperiod, String csign, Integer inoId) {
    }

    protected record YearPeriod(int year, int period) {
    }

    protected record PostingCounts(
            List<VoucherKey> unpostedKeys,
            List<VoucherKey> errorKeys,
            List<VoucherKey> reviewableKeys,
            List<VoucherKey> postedKeys
    ) {
        int unpostedCount() {
            return unpostedKeys.size();
        }

        int errorCount() {
            return errorKeys.size();
        }

        int reviewableCount() {
            return reviewableKeys.size();
        }

        int postedCount() {
            return postedKeys.size();
        }
    }
}
