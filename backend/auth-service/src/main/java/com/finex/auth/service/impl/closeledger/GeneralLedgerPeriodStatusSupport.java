package com.finex.auth.service.impl.closeledger;

import cn.hutool.crypto.digest.DigestUtil;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodActionResultVO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodStatusOverviewVO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodStatusRowVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinancePeriodClose;
import com.finex.auth.entity.FinancePostVoucherState;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.service.FinancePeriodTransferService;
import com.finex.auth.service.impl.closeledger.AbstractFinanceCloseLedgerSupport.FinanceGeneralLedgerRollbackSnapshot;
import com.finex.auth.service.impl.closeledger.AbstractFinanceCloseLedgerSupport.VoucherCounts;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GeneralLedgerPeriodStatusSupport {

    public static final String ACTION_REOPEN = "REOPEN";
    public static final String ACTION_UNPOST = "UNPOST";

    private static final String POST_STATUS_NOT_POSTED = "NOT_POSTED";
    private static final String POST_STATUS_PARTIALLY_POSTED = "PARTIALLY_POSTED";
    private static final String POST_STATUS_FULLY_POSTED = "FULLY_POSTED";
    private static final String POST_STATUS_POSTING = "POSTING";
    private static final String POST_STATUS_FAILED = "FAILED";
    private static final String CLOSE_STATUS_CLOSED = "CLOSED";
    private static final String CLOSE_STATUS_OPEN = "OPEN";
    private static final String REVIEW_STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String REVIEW_STATUS_NO_BUSINESS = "NO_BUSINESS";
    private static final String REVIEW_STATUS_HAS_UNPOSTED = "HAS_UNPOSTED";
    private static final String REVIEW_STATUS_REVIEWED = "REVIEWED";
    private static final String PERIOD_TRANSFER_STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String PERIOD_TRANSFER_STATUS_COMPLETED = "COMPLETED";
    private static final String PERIOD_TRANSFER_STATUS_PENDING = "PENDING";
    private static final String NEXT_STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String NEXT_STATUS_NO_BUSINESS = "NO_BUSINESS";
    private static final String NEXT_STATUS_HAS_UNPOSTED = "HAS_UNPOSTED";
    private static final String NEXT_STATUS_POSTED = "POSTED";
    private static final String NEXT_STATUS_CLOSED = "CLOSED";

    private final SharedCloseLedgerSupport support;
    private final UserMapperBridge userMapperBridge;
    private final FinancePeriodTransferService financePeriodTransferService;

    public GeneralLedgerPeriodStatusSupport(
            SharedCloseLedgerSupport support,
            UserMapperBridge userMapperBridge,
            FinancePeriodTransferService financePeriodTransferService
    ) {
        this.support = support;
        this.userMapperBridge = userMapperBridge;
        this.financePeriodTransferService = financePeriodTransferService;
    }

    public FinanceGeneralLedgerPeriodStatusOverviewVO getOverview(
            Long currentUserId,
            String companyId,
            Integer iyear,
            Integer currentIyear,
            Integer currentIperiod
    ) {
        SystemCompany company = support.resolveEffectiveCompany(currentUserId, companyId);
        FinanceAccountSet accountSet = support.requireActiveAccountSet(company.getCompanyId());
        int effectiveYear = support.normalizeYear(iyear);
        int effectiveCurrentYear = support.normalizeYear(currentIyear);
        int effectiveCurrentPeriod = support.normalizePeriod(currentIperiod);
        YearMonth periodStart = YearMonth.of(accountSet.getEnabledYear(), accountSet.getEnabledPeriod());
        YearMonth periodEnd = YearMonth.of(effectiveYear, 12);

        Map<Integer, PeriodSnapshot> snapshots = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month += 1) {
            snapshots.put(month, buildSnapshot(company.getCompanyId(), effectiveYear, month, periodStart, periodEnd));
        }

        List<FinanceGeneralLedgerPeriodStatusRowVO> rows = new ArrayList<>();
        for (int month = 1; month <= 12; month += 1) {
            PeriodSnapshot current = snapshots.get(month);
            PeriodSnapshot next = month < 12 ? snapshots.get(month + 1) : PeriodSnapshot.unavailable(effectiveYear, month);
            rows.add(toRowVO(current, next, effectiveCurrentYear, effectiveCurrentPeriod));
        }

        FinanceGeneralLedgerPeriodStatusOverviewVO overview = new FinanceGeneralLedgerPeriodStatusOverviewVO();
        overview.setCompanyId(company.getCompanyId());
        overview.setCompanyName(company.getCompanyName());
        overview.setIyear(effectiveYear);
        overview.setCurrentIyear(effectiveCurrentYear);
        overview.setCurrentIperiod(effectiveCurrentPeriod);
        overview.setCurrentPeriodLabel(effectiveCurrentYear + "-" + String.format(Locale.ROOT, "%02d", effectiveCurrentPeriod));
        overview.setRows(rows);
        return overview;
    }

    public FinanceGeneralLedgerPeriodActionResultVO reopenPeriod(
            Long currentUserId,
            String currentUsername,
            String companyId,
            Integer iyear,
            Integer iperiod,
            Integer currentIyear,
            Integer currentIperiod,
            String password
    ) {
        SystemCompany company = support.resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = support.normalizeYear(iyear);
        int effectivePeriod = support.normalizePeriod(iperiod);
        validateWithinCurrentPeriod(effectiveYear, effectivePeriod, currentIyear, currentIperiod);
        verifyCurrentPassword(currentUserId, password);

        FinancePeriodClose currentClose = support.findPeriodClose(company.getCompanyId(), effectiveYear, effectivePeriod);
        if (currentClose == null || !Objects.equals(support.trimToNull(currentClose.getStatus()), CLOSE_STATUS_CLOSED)) {
            throw new IllegalStateException("当前期间未结账，无需反结账");
        }
        YearMonth nextPeriod = YearMonth.of(effectiveYear, effectivePeriod).plusMonths(1);
        FinancePeriodClose nextClose = support.findPeriodClose(company.getCompanyId(), nextPeriod.getYear(), nextPeriod.getMonthValue());
        if (nextClose != null && Objects.equals(support.trimToNull(nextClose.getStatus()), CLOSE_STATUS_CLOSED)) {
            throw new IllegalStateException("下一期间已结账，不能反结账");
        }

        support.deleteCloseRecord(currentClose.getId());
        support.insertLog(
                company.getCompanyId(),
                effectiveYear,
                effectivePeriod,
                "REOPEN_SUCCESS",
                "SUCCESS",
                resolveOperatorName(currentUserId, currentUsername),
                "期间已反结账",
                "{\"nextIyperiod\":" + (nextPeriod.getYear() * 100 + nextPeriod.getMonthValue()) + "}"
        );
        return buildActionResult(company.getCompanyId(), effectiveYear, effectivePeriod, ACTION_REOPEN);
    }

    public FinanceGeneralLedgerPeriodActionResultVO unpostPeriod(
            Long currentUserId,
            String currentUsername,
            String companyId,
            Integer iyear,
            Integer iperiod,
            Integer currentIyear,
            Integer currentIperiod,
            String password
    ) {
        SystemCompany company = support.resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = support.normalizeYear(iyear);
        int effectivePeriod = support.normalizePeriod(iperiod);
        validateWithinCurrentPeriod(effectiveYear, effectivePeriod, currentIyear, currentIperiod);
        verifyCurrentPassword(currentUserId, password);

        FinancePeriodClose currentClose = support.findPeriodClose(company.getCompanyId(), effectiveYear, effectivePeriod);
        if (currentClose != null && Objects.equals(support.trimToNull(currentClose.getStatus()), CLOSE_STATUS_CLOSED)) {
            throw new IllegalStateException("当前期间已结账，请先反结账");
        }
        VoucherCounts currentCounts = support.summarizeVoucherGroups(support.loadVoucherGroups(company.getCompanyId(), effectiveYear, effectivePeriod));
        if (currentCounts.postedCount() <= 0) {
            throw new IllegalStateException("当前期间未记账，无需反记账");
        }
        YearMonth nextPeriod = YearMonth.of(effectiveYear, effectivePeriod).plusMonths(1);
        FinancePeriodClose nextClose = support.findPeriodClose(company.getCompanyId(), nextPeriod.getYear(), nextPeriod.getMonthValue());
        if (nextClose != null && Objects.equals(support.trimToNull(nextClose.getStatus()), CLOSE_STATUS_CLOSED)) {
            throw new IllegalStateException("下一期间已结账，不能反记账");
        }
        VoucherCounts nextCounts = support.summarizeVoucherGroups(support.loadVoucherGroups(company.getCompanyId(), nextPeriod.getYear(), nextPeriod.getMonthValue()));
        if (nextCounts.postedCount() > 0) {
            throw new IllegalStateException("下一期间已记账，不能反记账");
        }

        FinanceGeneralLedgerRollbackSnapshot snapshot = support.rollbackPostedPeriod(
                company.getCompanyId(),
                effectiveYear,
                effectivePeriod,
                resolveOperatorName(currentUserId, currentUsername)
        );
        if (snapshot.rolledBackVoucherCount() <= 0) {
            throw new IllegalStateException("当前期间未记账，无需反记账");
        }
        return buildActionResult(company.getCompanyId(), effectiveYear, effectivePeriod, ACTION_UNPOST);
    }

    private FinanceGeneralLedgerPeriodActionResultVO buildActionResult(String companyId, int iyear, int iperiod, String actionType) {
        VoucherCounts counts = support.summarizeVoucherGroups(support.loadVoucherGroups(companyId, iyear, iperiod));
        FinancePostVoucherState postState = support.findPostState(companyId, iyear, iperiod);
        FinancePeriodClose close = support.findPeriodClose(companyId, iyear, iperiod);

        FinanceGeneralLedgerPeriodActionResultVO result = new FinanceGeneralLedgerPeriodActionResultVO();
        result.setActionType(actionType);
        result.setCompanyId(companyId);
        result.setIyear(iyear);
        result.setIperiod(iperiod);
        result.setIyperiod(iyear * 100 + iperiod);
        result.setPeriodLabel(iyear + "-" + String.format(Locale.ROOT, "%02d", iperiod));
        result.setPostStatus(resolveEffectivePostStatus(postState, counts));
        result.setPostStatusLabel(resolvePostStatusLabel(result.getPostStatus(), counts));
        result.setCloseStatus(isClosed(close) ? CLOSE_STATUS_CLOSED : CLOSE_STATUS_OPEN);
        result.setCloseStatusLabel(isClosed(close) ? "已结账" : "未结账");
        return result;
    }

    private PeriodSnapshot buildSnapshot(
            String companyId,
            int iyear,
            int iperiod,
            YearMonth periodStart,
            YearMonth periodEnd
    ) {
        YearMonth current = YearMonth.of(iyear, iperiod);
        boolean available = !current.isBefore(periodStart) && !current.isAfter(periodEnd);
        if (!available) {
            return PeriodSnapshot.unavailable(iyear, iperiod);
        }
        VoucherCounts counts = support.summarizeVoucherGroups(support.loadVoucherGroups(companyId, iyear, iperiod));
        FinancePostVoucherState postState = support.findPostState(companyId, iyear, iperiod);
        FinancePeriodClose close = support.findPeriodClose(companyId, iyear, iperiod);
        boolean transferCompleted = financePeriodTransferService.hasCompletedRunForPeriod(companyId, iyear, iperiod);
        String transferMessage = financePeriodTransferService.resolveValidationMessage(companyId, iyear, iperiod);
        return new PeriodSnapshot(
                iyear,
                iperiod,
                true,
                counts,
                resolveEffectivePostStatus(postState, counts),
                isClosed(close),
                transferCompleted,
                transferMessage
        );
    }

    private FinanceGeneralLedgerPeriodStatusRowVO toRowVO(
            PeriodSnapshot current,
            PeriodSnapshot next,
            int currentIyear,
            int currentIperiod
    ) {
        FinanceGeneralLedgerPeriodStatusRowVO row = new FinanceGeneralLedgerPeriodStatusRowVO();
        row.setIyear(current.iyear());
        row.setIperiod(current.iperiod());
        row.setIyperiod(current.iyear() * 100 + current.iperiod());
        row.setPeriodLabel(current.iyear() + "-" + String.format(Locale.ROOT, "%02d", current.iperiod()));
        row.setAvailable(current.available());
        row.setVoucherCount(current.counts().totalCount());
        row.setUnpostedVoucherCount(current.counts().unpostedCount());
        row.setReviewedVoucherCount(current.counts().reviewedCount());
        row.setErrorVoucherCount(current.counts().errorCount());
        row.setPostedVoucherCount(current.counts().postedCount());
        row.setReviewStatus(resolveReviewStatus(current));
        row.setReviewStatusLabel(resolveReviewStatusLabel(current));
        row.setPostStatus(current.postStatus());
        row.setPostStatusLabel(resolvePostStatusLabel(current.postStatus(), current.counts()));
        row.setCloseStatus(current.closed() ? CLOSE_STATUS_CLOSED : CLOSE_STATUS_OPEN);
        row.setCloseStatusLabel(current.closed() ? "已结账" : "未结账");
        row.setPeriodTransferStatus(resolvePeriodTransferStatus(current));
        row.setPeriodTransferStatusLabel(resolvePeriodTransferStatusLabel(current));
        row.setNextPeriodStatus(resolveNextPeriodStatus(next));
        row.setNextPeriodStatusLabel(resolveNextPeriodStatusLabel(next));
        row.setAllowedActions(resolveAllowedActions(current, next, currentIyear, currentIperiod));
        row.setBlockingReason(resolveBlockingReason(current, next, currentIyear, currentIperiod));
        return row;
    }

    private List<String> resolveAllowedActions(PeriodSnapshot current, PeriodSnapshot next, int currentIyear, int currentIperiod) {
        List<String> actions = new ArrayList<>();
        if (!current.available() || compareYearMonth(current.iyear(), current.iperiod(), currentIyear, currentIperiod) > 0) {
            return actions;
        }
        if (current.closed()) {
            if (!next.closed()) {
                actions.add(ACTION_REOPEN);
            }
            return actions;
        }
        if (current.counts().postedCount() > 0 && !next.closed() && next.counts().postedCount() == 0) {
            actions.add(ACTION_UNPOST);
        }
        return actions;
    }

    private String resolveBlockingReason(PeriodSnapshot current, PeriodSnapshot next, int currentIyear, int currentIperiod) {
        if (!current.available()) {
            return "不在当前账套可用期间范围内";
        }
        if (compareYearMonth(current.iyear(), current.iperiod(), currentIyear, currentIperiod) > 0) {
            return "仅允许对当前期间及以前期间执行回退";
        }
        if (current.closed()) {
            if (next.closed()) {
                return "下一期间已结账，不能反结账";
            }
            return null;
        }
        if (current.counts().postedCount() <= 0) {
            return "当前期间未记账";
        }
        if (next.closed()) {
            return "下一期间已结账，不能反记账";
        }
        if (next.counts().postedCount() > 0) {
            return "下一期间已记账，不能反记账";
        }
        return null;
    }

    private String resolveReviewStatus(PeriodSnapshot snapshot) {
        if (!snapshot.available()) {
            return REVIEW_STATUS_UNAVAILABLE;
        }
        if (snapshot.counts().totalCount() == 0) {
            return REVIEW_STATUS_NO_BUSINESS;
        }
        if (snapshot.counts().unpostedCount() > 0) {
            return REVIEW_STATUS_HAS_UNPOSTED;
        }
        return REVIEW_STATUS_REVIEWED;
    }

    private String resolveReviewStatusLabel(PeriodSnapshot snapshot) {
        return switch (resolveReviewStatus(snapshot)) {
            case REVIEW_STATUS_UNAVAILABLE -> "不可用";
            case REVIEW_STATUS_NO_BUSINESS -> "无业务";
            case REVIEW_STATUS_HAS_UNPOSTED -> "有未审核";
            default -> "已审核";
        };
    }

    private String resolvePeriodTransferStatus(PeriodSnapshot snapshot) {
        if (!snapshot.available()) {
            return PERIOD_TRANSFER_STATUS_UNAVAILABLE;
        }
        return snapshot.transferCompleted() ? PERIOD_TRANSFER_STATUS_COMPLETED : PERIOD_TRANSFER_STATUS_PENDING;
    }

    private String resolvePeriodTransferStatusLabel(PeriodSnapshot snapshot) {
        if (!snapshot.available()) {
            return "不可用";
        }
        return snapshot.transferMessage();
    }

    private String resolveNextPeriodStatus(PeriodSnapshot snapshot) {
        if (!snapshot.available()) {
            return NEXT_STATUS_UNAVAILABLE;
        }
        if (snapshot.closed()) {
            return NEXT_STATUS_CLOSED;
        }
        if (snapshot.counts().postedCount() > 0) {
            return NEXT_STATUS_POSTED;
        }
        if (snapshot.counts().totalCount() > 0) {
            return NEXT_STATUS_HAS_UNPOSTED;
        }
        return NEXT_STATUS_NO_BUSINESS;
    }

    private String resolveNextPeriodStatusLabel(PeriodSnapshot snapshot) {
        return switch (resolveNextPeriodStatus(snapshot)) {
            case NEXT_STATUS_CLOSED -> "下一期已结账";
            case NEXT_STATUS_POSTED -> "下一期已记账";
            case NEXT_STATUS_HAS_UNPOSTED -> "下一期有未记账业务";
            case NEXT_STATUS_NO_BUSINESS -> "下一期无业务";
            default -> "下一期不可用";
        };
    }

    private String resolveEffectivePostStatus(FinancePostVoucherState state, VoucherCounts counts) {
        String raw = support.trimToNull(state == null ? null : state.getStatus());
        if (Objects.equals(raw, POST_STATUS_POSTING) || Objects.equals(raw, POST_STATUS_FAILED)) {
            return raw;
        }
        if (counts.reviewedCount() == 0 && counts.postedCount() > 0) {
            return POST_STATUS_FULLY_POSTED;
        }
        if (counts.postedCount() > 0) {
            return POST_STATUS_PARTIALLY_POSTED;
        }
        return POST_STATUS_NOT_POSTED;
    }

    private String resolvePostStatusLabel(String postStatus, VoucherCounts counts) {
        if (counts.totalCount() == 0 && POST_STATUS_NOT_POSTED.equals(postStatus)) {
            return "无业务";
        }
        return support.resolvePostStatusLabel(postStatus);
    }

    private boolean isClosed(FinancePeriodClose close) {
        return close != null && Objects.equals(support.trimToNull(close.getStatus()), CLOSE_STATUS_CLOSED);
    }

    private void validateWithinCurrentPeriod(int targetYear, int targetPeriod, Integer currentIyear, Integer currentIperiod) {
        int effectiveCurrentYear = support.normalizeYear(currentIyear);
        int effectiveCurrentPeriod = support.normalizePeriod(currentIperiod);
        if (compareYearMonth(targetYear, targetPeriod, effectiveCurrentYear, effectiveCurrentPeriod) > 0) {
            throw new IllegalStateException("仅允许对当前期间及以前期间执行回退");
        }
    }

    private void verifyCurrentPassword(Long currentUserId, String password) {
        User user = userMapperBridge.requireUser(currentUserId);
        if (support.trimToNull(password) == null || !Objects.equals(DigestUtil.md5Hex(password), user.getPassword())) {
            throw new IllegalArgumentException("登录密码不正确");
        }
    }

    private String resolveOperatorName(Long currentUserId, String currentUsername) {
        User user = userMapperBridge.loadUser(currentUserId);
        if (user != null && support.trimToNull(user.getName()) != null) {
            return user.getName();
        }
        if (user != null && support.trimToNull(user.getUsername()) != null) {
            return user.getUsername();
        }
        return support.trimToNull(currentUsername) == null ? "system" : currentUsername.trim();
    }

    private int compareYearMonth(int leftYear, int leftPeriod, int rightYear, int rightPeriod) {
        return leftYear * 100 + leftPeriod - (rightYear * 100 + rightPeriod);
    }

    public interface UserMapperBridge {
        User loadUser(Long userId);

        default User requireUser(Long userId) {
            User user = loadUser(userId);
            if (user == null) {
                throw new IllegalStateException("当前登录用户不存在");
            }
            return user;
        }
    }

    private record PeriodSnapshot(
            int iyear,
            int iperiod,
            boolean available,
            VoucherCounts counts,
            String postStatus,
            boolean closed,
            boolean transferCompleted,
            String transferMessage
    ) {
        private static PeriodSnapshot unavailable(int iyear, int iperiod) {
            return new PeriodSnapshot(
                    iyear,
                    iperiod,
                    false,
                    new VoucherCounts(List.of(), List.of(), List.of(), List.of()),
                    POST_STATUS_NOT_POSTED,
                    false,
                    false,
                    "不可用"
            );
        }
    }
}
