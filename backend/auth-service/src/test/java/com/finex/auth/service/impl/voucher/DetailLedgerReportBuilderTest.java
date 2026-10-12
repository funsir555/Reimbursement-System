package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetailLedgerReportBuilderTest {
    @Test
    void summarizesDescendantsByAccountingMonthAndPaginatesSignedEntries() {
        var subjects = Map.of(
                "1001", subject("1001", null, 1),
                "100101", subject("100101", "1001", 2),
                "100199", subject("100199", "1001", 2),
                "2001", subject("2001", null, 1));
        var builder = new DetailLedgerReportBuilder(subjects,
                List.of(opening("1001", "100.00"), opening("2001", "-50.00")),
                List.of(
                        voucher("100199", 2022, 12, 2, "2022-11-30T09:00:00", "-20.00", "0.00"),
                        voucher("100101", 2022, 12, 1, "2022-12-15T09:00:00", "10.00", "0.00"),
                        voucher("2001", 2023, 1, 3, "2023-01-02T09:00:00", "0.00", "-5.00")),
                YearMonth.of(2022, 12), YearMonth.of(2023, 1), null, null, row -> "客户：甲*项目：乙");

        var all = builder.allRows();
        var entries = all.stream().filter(row -> "ENTRY".equals(row.getRowType())).toList();
        assertEquals(List.of("100199", "100101", "2001"), entries.stream().map(FinanceDetailLedgerRowVO::getSubjectCode).toList());
        assertEquals(new BigDecimal("80.00"), entries.get(0).getBalance());
        assertEquals(new BigDecimal("90.00"), entries.get(1).getBalance());
        assertEquals(new BigDecimal("-20.00"), entries.get(0).getDebit());
        assertEquals("客户：甲*项目：乙", entries.get(0).getAssistLabel());
        assertEquals("贷", entries.get(2).getBalanceDirection());
        assertEquals(new BigDecimal("45.00"), entries.get(2).getBalance());
        assertEquals(4L, all.stream().filter(row -> "TOTAL".equals(row.getRowType())).count());

        var first = builder.page(1, 1);
        var second = builder.page(2, 1);
        assertEquals(3, first.getTotal());
        assertEquals("OPENING", first.getItems().get(0).getRowType());
        assertEquals("CARRY_FORWARD", second.getItems().get(0).getRowType());
        assertEquals(new BigDecimal("80.00"), second.getItems().get(0).getBalance());
        assertEquals("ENTRY", second.getItems().stream().filter(row -> "ENTRY".equals(row.getRowType())).findFirst().orElseThrow().getRowType());
        var paged = java.util.stream.IntStream.rangeClosed(1, 3)
                .boxed().flatMap(page -> builder.page(page, 1).getItems().stream())
                .filter(row -> !"CARRY_FORWARD".equals(row.getRowType()))
                .toList();
        assertEquals(all.stream().map(FinanceDetailLedgerRowVO::getSummary).toList(),
                paged.stream().map(FinanceDetailLedgerRowVO::getSummary).toList());
    }

    @Test
    void clipsBoundaryRootsAndCountsEachSelectedOpeningSubtreeOnce() {
        Map<String, FinanceAccountSubject> subjects = Map.ofEntries(
                Map.entry("1002", subject("1002", null, 1)),
                Map.entry("100201", subject("100201", "1002", 2)),
                Map.entry("100204", subject("100204", "1002", 2)),
                Map.entry("10020401", subject("10020401", "100204", 3)),
                Map.entry("100210", subject("100210", "1002", 2)),
                Map.entry("1003", subject("1003", null, 1)),
                Map.entry("100301", subject("100301", "1003", 2)),
                Map.entry("1012", subject("1012", null, 1)),
                Map.entry("101201", subject("101201", "1012", 2)),
                Map.entry("101202", subject("101202", "1012", 2)),
                Map.entry("10120201", subject("10120201", "101202", 3)),
                Map.entry("101203", subject("101203", "1012", 2)));
        List<GlAccsum> openings = List.of(
                opening("1002", "1000.00"), opening("100204", "200.00"),
                opening("10020401", "50.00"), opening("100210", "300.00"),
                opening("1003", "400.00"), opening("100301", "100.00"),
                opening("1012", "900.00"), opening("101201", "40.00"),
                opening("101202", "60.00"), opening("10120201", "10.00"),
                opening("101203", "80.00"));
        List<GlAccvouch> vouchers = List.of(
                voucher("100201", 2022, 12, 1, "2022-12-01T09:00:00", "1.00", "0.00"),
                voucher("10020401", 2022, 12, 2, "2022-12-02T09:00:00", "10.00", "0.00"),
                voucher("100210", 2022, 12, 3, "2022-12-03T09:00:00", "-5.00", "0.00"),
                voucher("100301", 2022, 12, 4, "2022-12-04T09:00:00", "20.00", "0.00"),
                voucher("101201", 2022, 12, 5, "2022-12-05T09:00:00", "2.00", "0.00"),
                voucher("10120201", 2022, 12, 6, "2022-12-06T09:00:00", "3.00", "0.00"),
                voucher("101203", 2022, 12, 7, "2022-12-07T09:00:00", "7.00", "0.00"));
        var range = builder(subjects, openings, vouchers, "100204", "101202");
        var rows = range.allRows();
        assertEquals(List.of("1002", "1003", "1012"), rows.stream()
                .filter(row -> "OPENING".equals(row.getRowType()))
                .map(FinanceDetailLedgerRowVO::getRootSubjectCode).toList());
        assertEquals(List.of(new BigDecimal("500.00"), new BigDecimal("400.00"), new BigDecimal("100.00")),
                rows.stream().filter(row -> "OPENING".equals(row.getRowType()))
                        .map(FinanceDetailLedgerRowVO::getBalance).toList());
        assertEquals(List.of("10020401", "100210", "100301", "101201", "10120201"),
                rows.stream().filter(row -> "ENTRY".equals(row.getRowType()))
                        .map(FinanceDetailLedgerRowVO::getSubjectCode).toList());
        assertEquals(List.of(new BigDecimal("505.00"), new BigDecimal("420.00"), new BigDecimal("105.00")),
                rows.stream().filter(row -> "ENDING".equals(row.getRowType()))
                        .map(FinanceDetailLedgerRowVO::getBalance).toList());
        assertEquals(5, range.page(1, 20).getTotal());
        assertEquals(List.of("10020401", "100210", "100301", "101201", "10120201"),
                range.page(1, 20).getItems().stream().filter(row -> "ENTRY".equals(row.getRowType()))
                        .map(FinanceDetailLedgerRowVO::getSubjectCode).toList());

        var sameParent = builder(subjects, openings, vouchers, "100204", "100204").allRows();
        assertEquals(new BigDecimal("200.00"), sameParent.get(0).getBalance());
        assertEquals(List.of("10020401"), sameParent.stream().filter(row -> "ENTRY".equals(row.getRowType()))
                .map(FinanceDetailLedgerRowVO::getSubjectCode).toList());
        assertEquals(sameParent.stream().map(FinanceDetailLedgerRowVO::getSummary).toList(),
                builder(subjects, openings, vouchers, "100204", null).allRows().stream()
                        .map(FinanceDetailLedgerRowVO::getSummary).toList());
        assertEquals(new BigDecimal("1000.00"), builder(subjects, openings, vouchers, null, null)
                .allRows().get(0).getBalance());
        assertThrows(IllegalArgumentException.class,
                () -> builder(subjects, openings, vouchers, "101202", "100204"));
    }

    private DetailLedgerReportBuilder builder(Map<String, FinanceAccountSubject> subjects,
            List<GlAccsum> openings, List<GlAccvouch> vouchers, String from, String to) {
        return new DetailLedgerReportBuilder(subjects, openings, vouchers,
                YearMonth.of(2022, 12), YearMonth.of(2022, 12), from, to, row -> "");
    }

    private FinanceAccountSubject subject(String code, String parent, int level) {
        var row = new FinanceAccountSubject();
        row.setSubjectCode(code);
        row.setSubjectName(code);
        row.setParentSubjectCode(parent);
        row.setSubjectLevel(level);
        return row;
    }

    private GlAccsum opening(String code, String amount) {
        var row = new GlAccsum();
        row.setCcode(code);
        row.setMb(new BigDecimal(amount));
        return row;
    }

    private GlAccvouch voucher(String code, int year, int month, int id, String date, String debit, String credit) {
        var row = new GlAccvouch();
        row.setCompanyId("c");
        row.setCcode(code);
        row.setIyear(year);
        row.setIperiod(month);
        row.setId(id);
        row.setInid(id);
        row.setInoId(id);
        row.setCsign("记");
        row.setDbillDate(LocalDateTime.parse(date));
        row.setMd(new BigDecimal(debit));
        row.setMc(new BigDecimal(credit));
        return row;
    }
}
