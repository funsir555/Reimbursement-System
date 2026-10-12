package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceLedgerReportPageVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.support.FinanceBalanceDirectionSupport;
import com.finex.auth.support.FinanceVoucherAmountSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Builds the ordinary detail ledger from original signed voucher columns. */
final class DetailLedgerReportBuilder {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final Map<String, FinanceAccountSubject> subjects;
    private final List<GlAccsum> openings;
    private final List<GlAccvouch> vouchers;
    private final YearMonth from;
    private final YearMonth to;
    private final Set<String> selectedCodes;
    private final Function<GlAccvouch, String> assistLabel;

    DetailLedgerReportBuilder(Map<String, FinanceAccountSubject> subjects, List<GlAccsum> openings,
                              List<GlAccvouch> vouchers, YearMonth from, YearMonth to,
                              String codeFrom, String codeTo, Function<GlAccvouch, String> assistLabel) {
        this.subjects = subjects;
        this.openings = openings;
        this.vouchers = vouchers;
        this.from = from;
        this.to = to;
        String rangeFrom = requireSubjectCode(codeFrom);
        String rangeTo = requireSubjectCode(codeTo == null ? codeFrom : codeTo);
        this.assistLabel = assistLabel;
        if (rangeFrom != null && rangeTo != null && rangeFrom.compareTo(rangeTo) > 0) {
            throw new IllegalArgumentException("科目范围起不能晚于科目范围止");
        }
        this.selectedCodes = selectSubjectCodes(rangeFrom, rangeTo);
    }

    List<FinanceDetailLedgerRowVO> allRows() {
        Map<String, List<GlAccvouch>> byRoot = new HashMap<>();
        Set<String> selectedRoots = new HashSet<>();
        for (String code : selectedCodes) {
            FinanceAccountSubject root = rootOf(code);
            if (root != null) {
                selectedRoots.add(root.getSubjectCode());
            }
        }
        for (GlAccvouch voucher : vouchers) {
            FinanceAccountSubject root = rootOf(voucher.getCcode());
            if (root != null && selectedCodes.contains(voucher.getCcode())) {
                byRoot.computeIfAbsent(root.getSubjectCode(), unused -> new ArrayList<>()).add(voucher);
            }
        }
        List<FinanceDetailLedgerRowVO> result = new ArrayList<>();
        subjects.values().stream()
                .filter(this::isRoot)
                .filter(root -> selectedRoots.contains(root.getSubjectCode()))
                .sorted(Comparator.comparing(FinanceAccountSubject::getSubjectCode))
                .forEach(root -> result.addAll(buildRoot(root, byRoot.getOrDefault(root.getSubjectCode(), List.of()))));
        return result;
    }

    FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO> page(int page, int pageSize) {
        List<FinanceDetailLedgerRowVO> all = allRows();
        long total = all.stream().filter(row -> "ENTRY".equals(row.getRowType())).count();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(500, pageSize));
        long fromEntry = (long) (safePage - 1) * safeSize;
        long toEntry = fromEntry + safeSize;
        List<FinanceDetailLedgerRowVO> visible = new ArrayList<>();
        long ordinal = 0;
        int segmentStart = 0;
        while (segmentStart < all.size()) {
            String rootCode = all.get(segmentStart).getRootSubjectCode();
            int segmentEnd = segmentStart + 1;
            while (segmentEnd < all.size() && rootCode.equals(all.get(segmentEnd).getRootSubjectCode())) {
                segmentEnd++;
            }
            ordinal = appendPageSegment(all.subList(segmentStart, segmentEnd), visible, ordinal,
                    fromEntry, toEntry, safePage == 1);
            segmentStart = segmentEnd;
        }
        FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO> result = new FinanceLedgerReportPageVO<>();
        result.setTotal(Math.toIntExact(total));
        result.setPage(safePage);
        result.setPageSize(safeSize);
        result.setItems(visible);
        return result;
    }

    private long appendPageSegment(List<FinanceDetailLedgerRowVO> segment, List<FinanceDetailLedgerRowVO> visible,
                                   long firstOrdinal, long fromEntry, long toEntry, boolean firstPage) {
        List<Integer> entryPositions = new ArrayList<>();
        for (int i = 0; i < segment.size(); i++) {
            if ("ENTRY".equals(segment.get(i).getRowType())) {
                entryPositions.add(i);
            }
        }
        if (entryPositions.isEmpty()) {
            if (firstPage) {
                visible.addAll(segment);
            }
            return firstOrdinal;
        }
        long lastOrdinal = firstOrdinal + entryPositions.size();
        if (lastOrdinal <= fromEntry || firstOrdinal >= toEntry) {
            return lastOrdinal;
        }
        boolean continued = firstOrdinal < fromEntry;
        boolean carryAdded = false;
        for (int i = 0; i < segment.size(); i++) {
            int entryIndex = java.util.Collections.binarySearch(entryPositions, i);
            long owner;
            if (entryIndex >= 0) {
                owner = firstOrdinal + entryIndex;
            } else if ("OPENING".equals(segment.get(i).getRowType())) {
                owner = firstOrdinal;
            } else if ("ENDING".equals(segment.get(i).getRowType())) {
                owner = lastOrdinal - 1;
            } else {
                int insertion = -entryIndex - 1;
                owner = firstOrdinal + (insertion == 0 ? 0 : insertion - 1);
            }
            if (owner < fromEntry || owner >= toEntry) {
                continue;
            }
            if (continued && !carryAdded) {
                int firstVisibleEntry = entryPositions.get((int) (fromEntry - firstOrdinal));
                FinanceDetailLedgerRowVO entry = segment.get(firstVisibleEntry);
                visible.add(staticRow(entry.getRootSubjectCode(), entry.getRootSubjectName(),
                        "CARRY_FORWARD", "承前余额", beforeEntry(entry)));
                carryAdded = true;
            }
            visible.add(segment.get(i));
        }
        return lastOrdinal;
    }

    private List<FinanceDetailLedgerRowVO> buildRoot(FinanceAccountSubject root, List<GlAccvouch> source) {
        BigDecimal running = opening(root.getSubjectCode());
        if (running.signum() == 0 && source.isEmpty()) {
            return List.of();
        }
        List<GlAccvouch> entries = source.stream().sorted(Comparator
                .comparing((GlAccvouch row) -> YearMonth.of(row.getIyear(), row.getIperiod()))
                .thenComparing(GlAccvouch::getDbillDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(GlAccvouch::getCsign, Comparator.nullsLast(String::compareTo))
                .thenComparing(GlAccvouch::getInoId, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(GlAccvouch::getInid, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(GlAccvouch::getId, Comparator.nullsLast(Integer::compareTo))).toList();
        List<FinanceDetailLedgerRowVO> result = new ArrayList<>();
        result.add(staticRow(root.getSubjectCode(), root.getSubjectName(), "OPENING", "期初余额", running));
        int index = 0;
        for (YearMonth month = from; !month.isAfter(to); month = month.plusMonths(1)) {
            BigDecimal debitTotal = ZERO;
            BigDecimal creditTotal = ZERO;
            while (index < entries.size() && YearMonth.of(entries.get(index).getIyear(), entries.get(index).getIperiod()).equals(month)) {
                GlAccvouch voucher = entries.get(index++);
                BigDecimal debit = money(voucher.getMd());
                BigDecimal credit = money(voucher.getMc());
                running = FinanceBalanceDirectionSupport.ending(running, debit, credit);
                debitTotal = debitTotal.add(debit);
                creditTotal = creditTotal.add(credit);
                result.add(entryRow(root, voucher, running));
            }
            FinanceDetailLedgerRowVO total = staticRow(root.getSubjectCode(), root.getSubjectName(),
                    "TOTAL", "本月合计", running);
            total.setDbillDate(month.toString());
            total.setDebit(debitTotal);
            total.setCredit(creditTotal);
            result.add(total);
        }
        result.add(staticRow(root.getSubjectCode(), root.getSubjectName(), "ENDING", "期末余额", running));
        return result;
    }

    private FinanceDetailLedgerRowVO entryRow(FinanceAccountSubject root, GlAccvouch voucher, BigDecimal signed) {
        FinanceDetailLedgerRowVO row = staticRow(root.getSubjectCode(), root.getSubjectName(), "ENTRY",
                voucher.getCdigest(), signed);
        FinanceAccountSubject subject = subjects.get(voucher.getCcode());
        row.setSubjectCode(voucher.getCcode());
        row.setSubjectName(subject == null ? voucher.getCcode() : subject.getSubjectName());
        row.setAssistLabel(assistLabel.apply(voucher));
        row.setDbillDate(voucher.getDbillDate() == null ? null : voucher.getDbillDate().toLocalDate().toString());
        row.setVoucherNo(voucher.getCompanyId() + "~" + voucher.getIyear() + "~" + voucher.getIperiod()
                + "~" + voucher.getCsign() + "~" + voucher.getInoId());
        row.setDisplayVoucherNo(voucher.getCsign() + "-" + String.format(Locale.ROOT, "%04d", voucher.getInoId()));
        row.setMakerName(voucher.getCbill());
        row.setDebit(FinanceVoucherAmountSupport.zeroIfNull(voucher.getMd()));
        row.setCredit(FinanceVoucherAmountSupport.zeroIfNull(voucher.getMc()));
        return row;
    }

    private FinanceDetailLedgerRowVO staticRow(String rootCode, String rootName, String type,
                                                String summary, BigDecimal signed) {
        FinanceDetailLedgerRowVO row = new FinanceDetailLedgerRowVO();
        row.setLedgerKind("DETAIL");
        row.setRootSubjectCode(rootCode);
        row.setRootSubjectName(rootName);
        row.setSubjectCode(rootCode);
        row.setSubjectName(rootName);
        row.setRowType(type);
        row.setSummary(summary);
        row.setDebit(ZERO);
        row.setCredit(ZERO);
        row.setBalance(FinanceBalanceDirectionSupport.displayAmount(signed));
        row.setBalanceDirection(FinanceBalanceDirectionSupport.resolveActualDirectionLabel(signed));
        return row;
    }

    private BigDecimal opening(String rootCode) {
        Map<String, GlAccsum> snapshots = new LinkedHashMap<>();
        for (GlAccsum row : openings) {
            FinanceAccountSubject root = rootOf(row.getCcode());
            if (root != null && rootCode.equals(root.getSubjectCode()) && selectedCodes.contains(row.getCcode())) {
                snapshots.put(row.getCcode(), row);
            }
        }
        BigDecimal amount = ZERO;
        for (GlAccsum row : snapshots.values()) {
            FinanceAccountSubject subject = subjects.get(row.getCcode());
            boolean covered = false;
            Set<String> seen = new HashSet<>();
            while (subject != null && seen.add(subject.getSubjectCode())) {
                String parent = subject.getParentSubjectCode();
                if (snapshots.containsKey(parent)) {
                    covered = true;
                    break;
                }
                subject = parent == null ? null : subjects.get(parent);
            }
            if (!covered) {
                amount = amount.add(money(row.getMb()));
            }
        }
        return amount;
    }

    private String requireSubjectCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim();
        if (rootOf(normalized) == null) {
            throw new IllegalArgumentException("科目不存在或不属于当前公司");
        }
        return normalized;
    }

    private Set<String> selectSubjectCodes(String rangeFrom, String rangeTo) {
        Set<String> range = new HashSet<>();
        for (String code : subjects.keySet()) {
            if ((rangeFrom == null || code.compareTo(rangeFrom) >= 0)
                    && (rangeTo == null || code.compareTo(rangeTo) <= 0)
                    && (rangeTo == null || !isStrictAncestor(code, rangeTo))) {
                range.add(code);
            }
        }
        Set<String> selected = new HashSet<>(range);
        for (FinanceAccountSubject subject : subjects.values()) {
            FinanceAccountSubject ancestor = subject;
            Set<String> seen = new HashSet<>();
            while (ancestor != null && seen.add(ancestor.getSubjectCode())) {
                if (range.contains(ancestor.getSubjectCode())) {
                    selected.add(subject.getSubjectCode());
                    break;
                }
                String parent = ancestor.getParentSubjectCode();
                ancestor = parent == null ? null : subjects.get(parent);
            }
        }
        return selected;
    }

    private boolean isStrictAncestor(String ancestorCode, String descendantCode) {
        FinanceAccountSubject child = subjects.get(descendantCode);
        Set<String> seen = new HashSet<>();
        while (child != null && seen.add(child.getSubjectCode())) {
            String parent = child.getParentSubjectCode();
            child = parent == null ? null : subjects.get(parent);
            if (child != null && ancestorCode.equals(child.getSubjectCode())) {
                return true;
            }
        }
        return false;
    }

    private FinanceAccountSubject rootOf(String code) {
        FinanceAccountSubject subject = subjects.get(code);
        Set<String> seen = new HashSet<>();
        while (subject != null && seen.add(subject.getSubjectCode())) {
            if (isRoot(subject)) {
                return subject;
            }
            subject = subjects.get(subject.getParentSubjectCode());
        }
        return null;
    }

    private boolean isRoot(FinanceAccountSubject subject) {
        return Integer.valueOf(1).equals(subject.getSubjectLevel());
    }

    private static BigDecimal beforeEntry(FinanceDetailLedgerRowVO entry) {
        BigDecimal after = "贷".equals(entry.getBalanceDirection()) ? money(entry.getBalance()).negate() : money(entry.getBalance());
        return after.subtract(money(entry.getDebit())).add(money(entry.getCredit()));
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
