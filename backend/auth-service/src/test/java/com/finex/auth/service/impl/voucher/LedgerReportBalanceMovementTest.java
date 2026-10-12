package com.finex.auth.service.impl.voucher;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerReportBalanceMovementTest {

    @Mock private GlAccvouchMapper voucherMapper;
    @Mock private GlAccsumMapper sumMapper;
    @Mock private FinanceAccountSubjectMapper subjectMapper;
    @Mock private SystemCompanyMapper companyMapper;
    @Mock private FinanceModuleEnableSupport moduleSupport;
    @Mock private VoucherContextSupport contextSupport;

    private LedgerReportDomainSupport report;

    @BeforeEach
    void setUp() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, GlAccsum.class);
        TableInfoHelper.initTableInfo(assistant, GlAccvouch.class);
        TableInfoHelper.initTableInfo(assistant, FinanceAccountSubject.class);
        TableInfoHelper.initTableInfo(assistant, SystemCompany.class);
        report = new LedgerReportDomainSupport(voucherMapper, sumMapper, null, subjectMapper,
                null, null, null, null, null, companyMapper, null, null, null, moduleSupport, contextSupport);
        when(companyMapper.selectCount(any())).thenReturn(1L);
        FinanceContextCompanyOptionVO company = new FinanceContextCompanyOptionVO();
        company.setCompanyId("A");
        company.setPeriodStartYear(2026);
        company.setPeriodStartMonth(1);
        company.setPeriodEndYear(2026);
        company.setPeriodEndMonth(12);
        FinanceContextMetaVO meta = new FinanceContextMetaVO();
        meta.setCompanyOptions(List.of(company));
        when(contextSupport.getMeta(null)).thenReturn(meta);
        when(subjectMapper.selectList(any())).thenReturn(List.of(subject("56", null, 1), subject("5601", "56", 2)));
    }

    @Test
    void unpostedChildMovementReachesSelectedParentAndCategoryOnce() {
        when(sumMapper.selectList(any())).thenReturn(List.of(balance("56", "50.00", "0.00", "0.00", "50.00")));
        when(voucherMapper.selectList(any())).thenReturn(List.of(voucher("5601", "100.00", "0.00", false)));

        FinanceLedgerReportQueryDTO query = query();
        query.setAccountCodeFrom("56");
        query.setAccountCodeTo("56");
        query.setSubjectLevelRange("1-1");
        List<FinanceBalanceSheetRowVO> rows = report.queryBalanceSheet(query).getItems();
        FinanceBalanceSheetRowVO parent = rows.stream().filter(row -> "SUBJECT".equals(row.getRowType())).findFirst().orElseThrow();
        FinanceBalanceSheetRowVO total = rows.stream().filter(row -> "CATEGORY_TOTAL".equals(row.getRowType())
                && "PROFIT".equals(row.getSubjectCategory())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("50.00"), parent.getBeginDebit());
        assertEquals(new BigDecimal("100.00"), parent.getPeriodDebit());
        assertEquals(new BigDecimal("150.00"), parent.getEndDebit());
        assertEquals(parent.getPeriodDebit(), total.getPeriodDebit());
    }

    @Test
    void postedMovementComesFromSnapshotAndIsNotAddedTwice() {
        when(sumMapper.selectList(any())).thenReturn(List.of(balance("56", "50.00", "100.00", "0.00", "150.00")));
        when(voucherMapper.selectList(any())).thenReturn(List.of(voucher("5601", "100.00", "0.00", true)));

        FinanceBalanceSheetRowVO parent = report.queryBalanceSheet(query()).getItems().stream()
                .filter(row -> "SUBJECT".equals(row.getRowType())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("100.00"), parent.getPeriodDebit());
        assertEquals(new BigDecimal("150.00"), parent.getEndDebit());
    }

    @Test
    void unpostedRedMovementReducesOriginalSideAndKeepsBalanceColumnsNonnegative() {
        when(sumMapper.selectList(any())).thenReturn(List.of());
        when(voucherMapper.selectList(any())).thenReturn(List.of(voucher("5601", "-20.00", "0.00", false)));

        List<FinanceBalanceSheetRowVO> rows = report.queryBalanceSheet(query()).getItems();
        FinanceBalanceSheetRowVO parent = rows.stream()
                .filter(row -> "56".equals(row.getSubjectCode())).findFirst().orElseThrow();
        FinanceBalanceSheetRowVO child = rows.stream()
                .filter(row -> "5601".equals(row.getSubjectCode())).findFirst().orElseThrow();
        FinanceBalanceSheetRowVO total = rows.stream()
                .filter(row -> "CATEGORY_TOTAL".equals(row.getRowType())
                        && "PROFIT".equals(row.getSubjectCategory())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("-20.00"), parent.getPeriodDebit());
        assertEquals(new BigDecimal("-20.00"), child.getPeriodDebit());
        assertEquals(new BigDecimal("0.00"), parent.getPeriodCredit());
        assertEquals(new BigDecimal("20.00"), parent.getEndCredit());
        assertEquals(new BigDecimal("-20.00"), total.getPeriodDebit());
        assertEquals(new BigDecimal("0.00"), total.getPeriodCredit());
    }

    @Test
    void postedRedMovementUsesSignedSnapshotWithoutAddingVoucherTwice() {
        when(sumMapper.selectList(any())).thenReturn(List.of(
                balance("56", "0.00", "-2273513.79", "0.00", "-2273513.79")));
        when(voucherMapper.selectList(any())).thenReturn(List.of(
                voucher("5601", "-2273513.79", "0.00", true)));

        FinanceBalanceSheetRowVO parent = report.queryBalanceSheet(query()).getItems().stream()
                .filter(row -> "56".equals(row.getSubjectCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-2273513.79"), parent.getPeriodDebit());
        assertEquals(new BigDecimal("0.00"), parent.getPeriodCredit());
        assertEquals(new BigDecimal("2273513.79"), parent.getEndCredit());
    }

    @Test
    void exportUsesTheSameSignedOccurrenceAsBalanceQuery() throws Exception {
        when(sumMapper.selectList(any())).thenReturn(List.of());
        when(voucherMapper.selectList(any())).thenReturn(List.of(voucher("5601", "-20.00", "0.00", false)));

        FinanceBalanceSheetRowVO child = report.queryBalanceSheet(query()).getItems().stream()
                .filter(row -> "5601".equals(row.getSubjectCode())).findFirst().orElseThrow();
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(report.exportBalanceSheet(query())))) {
            var sheet = workbook.getSheetAt(0);
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                var row = sheet.getRow(index);
                if ("5601".equals(row.getCell(0).getStringCellValue())) {
                    assertEquals(child.getPeriodDebit().doubleValue(), row.getCell(5).getNumericCellValue());
                    assertEquals(child.getPeriodCredit().doubleValue(), row.getCell(6).getNumericCellValue());
                    return;
                }
            }
        }
        throw new AssertionError("导出的余额表缺少 5601 科目");
    }

    private FinanceLedgerReportQueryDTO query() {
        FinanceLedgerReportQueryDTO query = new FinanceLedgerReportQueryDTO();
        query.setCompanyId("A");
        query.setIyearFrom(2026);
        query.setIperiodFrom(12);
        query.setIyearTo(2026);
        query.setIperiodTo(12);
        query.setIncludeUnposted(true);
        return query;
    }

    private FinanceAccountSubject subject(String code, String parent, int level) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId("A");
        subject.setSubjectCode(code);
        subject.setSubjectName(code);
        subject.setParentSubjectCode(parent);
        subject.setSubjectLevel(level);
        subject.setSubjectCategory("PROFIT");
        subject.setBalanceDirection("DEBIT");
        return subject;
    }

    private GlAccsum balance(String code, String mb, String md, String mc, String me) {
        GlAccsum row = new GlAccsum();
        row.setCcode(code);
        row.setIyear(2026);
        row.setIperiod(12);
        row.setMb(new BigDecimal(mb));
        row.setMd(new BigDecimal(md));
        row.setMc(new BigDecimal(mc));
        row.setMe(new BigDecimal(me));
        return row;
    }

    private GlAccvouch voucher(String code, String md, String mc, boolean posted) {
        GlAccvouch row = new GlAccvouch();
        row.setCcode(code);
        row.setIyear(2026);
        row.setIperiod(12);
        row.setMd(new BigDecimal(md));
        row.setMc(new BigDecimal(mc));
        row.setIbook(posted ? 1 : 0);
        return row;
    }
}
