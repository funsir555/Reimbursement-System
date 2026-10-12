package com.finex.auth.service.impl.financequery;

import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceLedgerReportPageVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.service.FinanceLedgerReportService;
import com.finex.auth.service.queryauth.QueryAuthorization;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FinanceAgentSupplierLedgerReadServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FinanceLedgerReportService reports = mock(FinanceLedgerReportService.class);
    private final FinanceAgentSupplierLedgerReadService service =
            new FinanceAgentSupplierLedgerReadService(jdbc, reports);

    @Test
    void refusesUnauthorizedAccountBeforeReadingSupplier() {
        assertThatThrownBy(() -> service.query(grant("A"), values("B"), 1, 50))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.suppliers(grant("A"), "B", "供应商6"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(jdbc, reports);
    }

    @Test
    void usesSupplierDetailLedgerAndProjectsOnlyApprovedFields() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("A"), eq("V001"))).thenReturn(1);
        FinanceDetailLedgerRowVO row = new FinanceDetailLedgerRowVO();
        row.setRowType("DETAIL");
        row.setDbillDate("2022-12-01");
        row.setVoucherNo("记-1");
        row.setSummary("采购款");
        row.setDebit(new BigDecimal("20"));
        FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO> report = new FinanceLedgerReportPageVO<>();
        report.setTotal(1);
        report.setItems(List.of(row));
        when(reports.queryDetailLedger(eq(9L), any())).thenReturn(report);

        var page = service.query(grant("A"), values("A"), 1, 50);

        ArgumentCaptor<FinanceLedgerReportQueryDTO> input = ArgumentCaptor.forClass(FinanceLedgerReportQueryDTO.class);
        verify(reports).queryDetailLedger(eq(9L), input.capture());
        assertThat(input.getValue().getLedgerKind()).isEqualTo("SUPPLIER");
        assertThat(input.getValue().getCsupId()).isEqualTo("V001");
        assertThat(input.getValue().getIncludeUnposted()).isTrue();
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records().get(0)).containsEntry("summary", "采购款")
                .containsEntry("debit", new BigDecimal("20"))
                .doesNotContainKey("bankAccount");
    }

    private Map<String, Object> values(String company) {
        return Map.of("companyId", company, "supplierCode", "V001", "yearFrom", 2022,
                "periodFrom", 1, "yearTo", 2022, "periodTo", 12,
                "voucherStatus", "INCLUDE_SAVED_UNPOSTED");
    }

    private QueryAuthorization grant(String company) {
        var scope = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of(company), null);
        return new QueryAuthorization(9L, 8L, "finance:general_ledger:supplier_detail_ledger:view",
                List.of(scope), List.of(scope), null);
    }
}
