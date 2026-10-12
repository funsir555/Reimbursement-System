package com.finex.auth.service.impl.voucher;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerReportPeriodRangeTest {
    @Mock private GlAccvouchMapper voucherMapper;
    @Mock private GlAccsumMapper sumMapper;
    @Mock private GlAccassMapper assistMapper;
    @Mock private FinanceAccountSubjectMapper subjectMapper;
    @Mock private FinanceCustomerMapper customerMapper;
    @Mock private FinanceVendorMapper vendorMapper;
    @Mock private FinanceProjectClassMapper projectClassMapper;
    @Mock private FinanceProjectArchiveMapper projectMapper;
    @Mock private SystemCompanyMapper companyMapper;
    @Mock private SystemDepartmentMapper departmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private FinanceModuleEnableSupport moduleSupport;
    @Mock private VoucherContextSupport contextSupport;

    private LedgerReportDomainSupport report;

    @BeforeEach
    void setUp() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, GlAccsum.class);
        TableInfoHelper.initTableInfo(assistant, GlAccass.class);
        TableInfoHelper.initTableInfo(assistant, GlAccvouch.class);
        TableInfoHelper.initTableInfo(assistant, FinanceAccountSubject.class);
        TableInfoHelper.initTableInfo(assistant, FinanceCustomer.class);
        TableInfoHelper.initTableInfo(assistant, FinanceVendor.class);
        TableInfoHelper.initTableInfo(assistant, FinanceProjectClass.class);
        TableInfoHelper.initTableInfo(assistant, SystemDepartment.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, SystemCompany.class);
        report = new LedgerReportDomainSupport(voucherMapper, sumMapper, assistMapper, subjectMapper,
                null, customerMapper, vendorMapper, projectClassMapper, projectMapper, companyMapper,
                departmentMapper, userMapper, null, moduleSupport, contextSupport);
        lenient().when(companyMapper.selectCount(any())).thenReturn(1L);
        FinanceContextCompanyOptionVO company = new FinanceContextCompanyOptionVO();
        company.setCompanyId("A");
        company.setPeriodStartYear(2022);
        company.setPeriodStartMonth(11);
        company.setPeriodEndYear(2023);
        company.setPeriodEndMonth(2);
        FinanceContextMetaVO meta = new FinanceContextMetaVO();
        meta.setCompanyOptions(List.of(company));
        lenient().when(contextSupport.getMeta(null)).thenReturn(meta);
        lenient().when(subjectMapper.selectList(any())).thenReturn(List.of(subject()));
        lenient().when(sumMapper.selectList(any())).thenReturn(List.of());
        lenient().when(assistMapper.selectList(any())).thenReturn(List.of());
        lenient().when(voucherMapper.selectList(any())).thenReturn(List.of());
        lenient().when(vendorMapper.selectList(any())).thenReturn(List.of());
        lenient().when(customerMapper.selectList(any())).thenReturn(List.of());
        lenient().when(projectClassMapper.selectList(any())).thenReturn(List.of());
        lenient().when(departmentMapper.selectList(any())).thenReturn(List.of());
        lenient().when(userMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void generalLedgerUsesFirstOpeningAndOrdersSignedEntriesByAccountingPeriod() throws Exception {
        when(sumMapper.selectList(any())).thenReturn(List.of(opening()));
        GlAccvouch november = voucher(2022, 11, "2023-01-10T09:00:00", 1, "-20.00", "0.00");
        GlAccvouch december = voucher(2022, 12, "2022-12-01T09:00:00", 2, "50.00", "0.00");
        GlAccvouch january = voucher(2023, 1, "2023-01-01T09:00:00", 3, "0.00", "10.00");
        when(voucherMapper.selectList(any())).thenReturn(List.of(january, december, november));

        var section = report.queryGeneralLedger(range()).getItems().get(0);
        assertEquals(new BigDecimal("100.00"), section.getBeginDebit());
        assertEquals(new BigDecimal("30.00"), section.getTotalDebit());
        assertEquals(new BigDecimal("10.00"), section.getTotalCredit());
        assertEquals(new BigDecimal("120.00"), section.getEndDebit());
        assertEquals(List.of(new BigDecimal("-20.00"), new BigDecimal("50.00"), new BigDecimal("0.00")),
                section.getRows().stream().filter(row -> "ENTRY".equals(row.getRowType()))
                        .map(FinanceDetailLedgerRowVO::getDebit).toList());
        assertEquals(List.of(new BigDecimal("80.00"), new BigDecimal("130.00"), new BigDecimal("120.00")),
                section.getRows().stream().filter(row -> "ENTRY".equals(row.getRowType()))
                        .map(FinanceDetailLedgerRowVO::getBalance).toList());
        assertEquals("期间合计", section.getRows().stream().filter(row -> "TOTAL".equals(row.getRowType()))
                .findFirst().orElseThrow().getSummary());
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(report.exportGeneralLedger(range())))) {
            assertEquals("期间合计", workbook.getSheetAt(0).getRow(5).getCell(5).getStringCellValue());
        }

        when(voucherMapper.selectList(any())).thenReturn(List.of(november));
        FinanceLedgerReportQueryDTO legacy = new FinanceLedgerReportQueryDTO();
        legacy.setCompanyId("A");
        legacy.setIyear(2022);
        legacy.setIperiod(11);
        assertEquals("本月合计", report.queryGeneralLedger(legacy).getItems().get(0).getRows().stream()
                .filter(row -> "TOTAL".equals(row.getRowType())).findFirst().orElseThrow().getSummary());
    }

    @Test
    void allSpecialLedgersAcceptRangeWithoutLegacyYearOnQueryAndExport() {
        for (String kind : List.of("PROJECT", "SUPPLIER", "CUSTOMER", "PERSONAL", "QUANTITY_AMOUNT")) {
            FinanceLedgerReportQueryDTO query = range();
            query.setLedgerKind(kind);
            query.setCitemClass("P");
            assertEquals(0, report.queryDetailLedger(query).getTotal());
            assertEquals(true, report.exportDetailLedger(query).length > 0);
        }
    }

    @Test
    void supplierLedgerRetainsAssistFilterAndAccumulatesFromFirstMonth() {
        GlAccass opening = new GlAccass();
        opening.setCompanyId("A");
        opening.setCcode("1001");
        opening.setIyear(2022);
        opening.setIperiod(11);
        opening.setCsupId("S1");
        opening.setMb(new BigDecimal("100.00"));
        when(assistMapper.selectList(any())).thenReturn(List.of(opening));
        GlAccvouch first = voucher(2022, 11, "2022-11-20T09:00:00", 1, "-20.00", "0.00");
        first.setCsupId("S1");
        GlAccvouch second = voucher(2023, 1, "2023-01-01T09:00:00", 2, "50.00", "0.00");
        second.setCsupId("S1");
        GlAccvouch excluded = voucher(2022, 12, "2022-12-01T09:00:00", 3, "100.00", "0.00");
        excluded.setCsupId("S2");
        when(voucherMapper.selectList(any())).thenReturn(List.of(second, excluded, first));

        FinanceLedgerReportQueryDTO query = range();
        query.setLedgerKind("SUPPLIER");
        query.setCsupId("S1");
        List<FinanceDetailLedgerRowVO> rows = report.queryDetailLedger(query).getItems();
        assertEquals(List.of("OPENING", "ENTRY", "ENTRY", "TOTAL", "ENDING"),
                rows.stream().map(FinanceDetailLedgerRowVO::getRowType).toList());
        assertEquals(new BigDecimal("100.00"), rows.get(0).getBalance());
        assertEquals(List.of(new BigDecimal("80.00"), new BigDecimal("130.00")),
                rows.stream().filter(row -> "ENTRY".equals(row.getRowType()))
                        .map(FinanceDetailLedgerRowVO::getBalance).toList());
        assertEquals(new BigDecimal("30.00"), rows.get(3).getDebit());
        assertEquals("期间合计", rows.get(3).getSummary());
        assertEquals(new BigDecimal("130.00"), rows.get(4).getBalance());
    }

    @Test
    void blankAssistFiltersIncludeAllHistoricalGroupsButExcludeRowsWithoutThatDimension() throws Exception {
        for (String kind : List.of("PROJECT", "SUPPLIER", "CUSTOMER", "PERSONAL")) {
            when(assistMapper.selectList(any())).thenReturn(List.of(
                    assistOpening(kind, "A", "P1", "10.00"),
                    assistOpening(kind, "B", "P2", "20.00"),
                    assistOpening(kind, null, null, "30.00")));
            when(voucherMapper.selectList(any())).thenReturn(List.of(
                    assistVoucher(kind, "A", "P1", 1),
                    assistVoucher(kind, "B", "P2", 2),
                    assistVoucher(kind, null, null, 3)));
            FinanceLedgerReportQueryDTO query = range();
            query.setLedgerKind(kind);
            List<FinanceDetailLedgerRowVO> rows = report.queryDetailLedger(query).getItems();
            assertEquals(2, rows.stream().filter(row -> "ENTRY".equals(row.getRowType())).count(), kind);
            assertEquals(2, rows.stream().filter(row -> "ENTRY".equals(row.getRowType()))
                    .map(FinanceDetailLedgerRowVO::getGroupKey).distinct().count(), kind);
            assertEquals(2, rows.stream().filter(row -> "OPENING".equals(row.getRowType())).count(), kind);
            try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(report.exportDetailLedger(query)))) {
                var sheet = workbook.getSheetAt(0);
                assertEquals(2, IntStream.rangeClosed(1, sheet.getLastRowNum())
                        .filter(index -> "ENTRY".equals(sheet.getRow(index).getCell(4).getStringCellValue()))
                        .count(), kind);
            }

            selectAssist(query, kind, "A");
            assertEquals(1, report.queryDetailLedger(query).getItems().stream()
                    .filter(row -> "ENTRY".equals(row.getRowType())).count(), kind);
            if ("PROJECT".equals(kind)) {
                query.setCitemId(null);
                query.setCitemClass("P2");
                assertEquals(1, report.queryDetailLedger(query).getItems().stream()
                        .filter(row -> "ENTRY".equals(row.getRowType())).count());
                query.setCitemId("A");
                assertEquals(0, report.queryDetailLedger(query).getTotal());
            }
        }
    }

    @Test
    void quantityLedgerDisplaysSharedAssistModesOnlyOnEntriesAndExportsThem() throws Exception {
        FinanceCustomer customer = new FinanceCustomer();
        customer.setCCusCode("C1");
        customer.setCCusName("客户甲");
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("S1");
        vendor.setCVenName("供应商甲");
        when(customerMapper.selectList(any())).thenReturn(List.of(customer));
        when(vendorMapper.selectList(any())).thenReturn(List.of(vendor));
        when(sumMapper.selectList(any())).thenReturn(List.of(opening()));
        GlAccvouch withAssist = voucher(2022, 11, "2022-11-20T09:00:00", 1, "20.00", "0.00");
        withAssist.setCcusId("C1");
        withAssist.setCsupId("S1");
        GlAccvouch withoutAssist = voucher(2022, 12, "2022-12-01T09:00:00", 2, "30.00", "0.00");
        when(voucherMapper.selectList(any())).thenReturn(List.of(withoutAssist, withAssist));

        FinanceLedgerReportQueryDTO query = range();
        query.setLedgerKind("QUANTITY_AMOUNT");
        for (var expected : List.of(
                new String[]{"NAME", "客户：客户甲*供应商：供应商甲"},
                new String[]{"CODE", "客户：C1*供应商：S1"},
                new String[]{"CODE_NAME", "客户：C1 客户甲*供应商：S1 供应商甲"})) {
            query.setBalanceAssistDisplay(expected[0]);
            List<FinanceDetailLedgerRowVO> rows = report.queryDetailLedger(query).getItems();
            assertEquals(List.of("OPENING", "ENTRY", "ENTRY", "TOTAL", "ENDING"),
                    rows.stream().map(FinanceDetailLedgerRowVO::getRowType).toList());
            assertEquals(expected[1], rows.get(1).getAssistLabel());
            assertEquals("", rows.get(0).getAssistLabel());
            assertEquals("", rows.get(2).getAssistLabel());
            assertEquals("", rows.get(3).getAssistLabel());
            assertEquals("", rows.get(4).getAssistLabel());
            assertEquals(new BigDecimal("50.00"), rows.get(3).getDebit());
            assertEquals(new BigDecimal("150.00"), rows.get(4).getBalance());
            try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(report.exportDetailLedger(query)))) {
                assertEquals(expected[1], workbook.getSheetAt(0).getRow(2).getCell(3).getStringCellValue());
            }
        }
        query.setBalanceAssistDisplay(null);
        assertEquals("", report.queryDetailLedger(query).getItems().get(1).getAssistLabel());

        query.setCsupId("S1");
        assertEquals(4, report.queryDetailLedger(query).getItems().size());
    }

    @Test
    void incompleteReverseAndOutOfAccountSetRangesFailWhileSequenceRemainsSingleMonth() {
        FinanceLedgerReportQueryDTO incomplete = range();
        incomplete.setIperiodTo(null);
        assertEquals("会计期间范围不完整", assertThrows(IllegalArgumentException.class,
                () -> report.queryGeneralLedger(incomplete)).getMessage());
        FinanceLedgerReportQueryDTO reverse = range();
        reverse.setIyearFrom(2023);
        reverse.setIperiodFrom(2);
        assertEquals("会计期间起不能晚于期间止", assertThrows(IllegalArgumentException.class,
                () -> report.queryGeneralLedger(reverse)).getMessage());
        FinanceLedgerReportQueryDTO outOfBounds = range();
        outOfBounds.setIyearFrom(2022);
        outOfBounds.setIperiodFrom(10);
        assertEquals("期间起早于账套启用期间", assertThrows(IllegalArgumentException.class,
                () -> report.queryGeneralLedger(outOfBounds)).getMessage());
        assertEquals("会计年度不能为空", assertThrows(IllegalArgumentException.class,
                () -> report.querySequenceLedger(range())).getMessage());
    }

    private FinanceLedgerReportQueryDTO range() {
        FinanceLedgerReportQueryDTO query = new FinanceLedgerReportQueryDTO();
        query.setCompanyId("A");
        query.setIyearFrom(2022);
        query.setIperiodFrom(11);
        query.setIyearTo(2023);
        query.setIperiodTo(1);
        query.setIncludeUnposted(true);
        return query;
    }

    private FinanceAccountSubject subject() {
        FinanceAccountSubject row = new FinanceAccountSubject();
        row.setCompanyId("A");
        row.setSubjectCode("1001");
        row.setSubjectName("现金");
        row.setSubjectLevel(1);
        row.setSubjectCategory("ASSET");
        row.setBalanceDirection("DEBIT");
        row.setCmeasure("个");
        return row;
    }

    private GlAccsum opening() {
        GlAccsum row = new GlAccsum();
        row.setCompanyId("A");
        row.setCcode("1001");
        row.setIyear(2022);
        row.setIperiod(11);
        row.setMb(new BigDecimal("100.00"));
        return row;
    }

    private GlAccass assistOpening(String kind, String code, String projectClass, String amount) {
        GlAccass row = new GlAccass();
        row.setCompanyId("A");
        row.setCcode("1001");
        row.setIyear(2022);
        row.setIperiod(11);
        row.setMb(new BigDecimal(amount));
        applyAssist(row, kind, code, projectClass);
        return row;
    }

    private GlAccvouch assistVoucher(String kind, String code, String projectClass, int number) {
        GlAccvouch row = voucher(2022, 11, "2022-11-20T09:00:00", number, "5.00", "0.00");
        switch (kind) {
            case "PROJECT" -> { row.setCitemClass(projectClass); row.setCitemId(code); }
            case "SUPPLIER" -> row.setCsupId(code);
            case "CUSTOMER" -> row.setCcusId(code);
            case "PERSONAL" -> row.setCpersonId(code);
            default -> throw new IllegalArgumentException(kind);
        }
        return row;
    }

    private void applyAssist(GlAccass row, String kind, String code, String projectClass) {
        switch (kind) {
            case "PROJECT" -> { row.setCitemClass(projectClass); row.setCitemId(code); }
            case "SUPPLIER" -> row.setCsupId(code);
            case "CUSTOMER" -> row.setCcusId(code);
            case "PERSONAL" -> row.setCpersonId(code);
            default -> throw new IllegalArgumentException(kind);
        }
    }

    private void selectAssist(FinanceLedgerReportQueryDTO query, String kind, String code) {
        switch (kind) {
            case "PROJECT" -> query.setCitemId(code);
            case "SUPPLIER" -> query.setCsupId(code);
            case "CUSTOMER" -> query.setCcusId(code);
            case "PERSONAL" -> query.setCpersonId(code);
            default -> throw new IllegalArgumentException(kind);
        }
    }

    private GlAccvouch voucher(int year, int month, String date, int number, String debit, String credit) {
        GlAccvouch row = new GlAccvouch();
        row.setCompanyId("A");
        row.setCcode("1001");
        row.setIyear(year);
        row.setIperiod(month);
        row.setIyperiod(year * 100 + month);
        row.setCsign("记");
        row.setInoId(number);
        row.setInid(1);
        row.setId(number);
        row.setDbillDate(LocalDateTime.parse(date));
        row.setMd(new BigDecimal(debit));
        row.setMc(new BigDecimal(credit));
        row.setIbook(1);
        return row;
    }
}
