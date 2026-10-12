package com.finex.auth.service.impl.financequery;

import com.finex.auth.service.FinanceLedgerReportService;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.FinanceLedgerReportPermissionCodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FinanceAgentQueryServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FinanceLedgerReportService reports = mock(FinanceLedgerReportService.class);
    private final FinanceAgentSupplierLedgerReadService supplier = mock(FinanceAgentSupplierLedgerReadService.class);
    private final FinanceAgentQueryService service = new FinanceAgentQueryService(jdbc, reports, supplier);

    @Test
    void rejectsCompanyOutsideThisToolAuthorizationBeforeAnyQuery() {
        assertThatThrownBy(() -> service.query("SUBJECT", grant("A"), Map.of("companyId", "B")))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(jdbc, reports);
    }

    @Test
    void archiveQueryUsesCompanyFilterAndFixedProjection() {
        when(jdbc.queryForObject(contains("fin_account_set"), eq(Integer.class), eq("A"), eq("ACTIVE")))
                .thenReturn(1);
        when(jdbc.queryForObject(contains("fin_account_subject WHERE company_id=?"),
                eq(Long.class), eq("A"))).thenReturn(1L);
        when(jdbc.queryForList(contains("FROM fin_account_subject WHERE company_id=?"),
                eq("A"), eq(50), eq(0))).thenReturn(List.of(Map.of("subject_code", "1001")));

        var page = service.query("SUBJECT", grant("A"), Map.of("companyId", "A"));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).containsExactly(Map.of("subject_code", "1001"));
    }

    @Test
    void rejectsCompanyWithoutActiveAccountSet() {
        when(jdbc.queryForObject(contains("fin_account_set"), eq(Integer.class), eq("A"), eq("ACTIVE")))
                .thenReturn(0);

        assertThatThrownBy(() -> service.query("SUBJECT", grant("A"), Map.of("companyId", "A")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账套不存在或尚未就绪");
        verifyNoInteractions(reports);
    }

    @Test
    void sequenceRejectsPeriodBeyondAccountSetBeforeVoucherRead() {
        when(jdbc.queryForObject(contains("fin_account_set"), eq(Integer.class), eq("A"), eq("ACTIVE")))
                .thenReturn(1);
        when(jdbc.queryForList(contains("SELECT enabled_year,enabled_period"), eq("A"), eq("ACTIVE")))
                .thenReturn(List.of(Map.of("enabled_year", 2026, "enabled_period", 4)));
        when(jdbc.queryForList(contains("FROM gl_period_close"), eq("A")))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.query("SEQUENCE", grant("A",
                FinanceLedgerReportPermissionCodes.SEQUENCE), Map.of(
                "companyId", "A", "yearFrom", 2026, "periodFrom", 4,
                "yearTo", 2026, "periodTo", 5, "voucherStatus", "POSTED_ONLY")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("会计期间超出");
        verify(jdbc).queryForList(contains("SELECT enabled_year,enabled_period"), eq("A"), eq("ACTIVE"));
    }

    @Test
    void sequenceReadsCurrentPeriodFromActiveAccountSet() {
        when(jdbc.queryForObject(contains("fin_account_set"), eq(Integer.class), eq("A"), eq("ACTIVE")))
                .thenReturn(1);
        when(jdbc.queryForList(contains("SELECT enabled_year,enabled_period"), eq("A"), eq("ACTIVE")))
                .thenReturn(List.of(Map.of("enabled_year", 2022, "enabled_period", 11)));
        when(jdbc.queryForList(contains("FROM gl_period_close"), eq("A")))
                .thenReturn(List.of(Map.of("iyear", 2022, "iperiod", 11)));
        when(jdbc.queryForObject(contains("SELECT COUNT(*) FROM gl_accvouch"),
                eq(Long.class), any(Object[].class))).thenReturn(1L);
        when(jdbc.queryForList(contains("FROM gl_accvouch"), any(Object[].class)))
                .thenReturn(List.of(Map.of("i_id", 42L)));

        var page = service.query("SEQUENCE", grant("A", FinanceLedgerReportPermissionCodes.SEQUENCE), Map.of(
                "companyId", "A", "yearFrom", 2022, "periodFrom", 12,
                "yearTo", 2022, "periodTo", 12,
                "voucherStatus", "INCLUDE_SAVED_UNPOSTED"));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).containsExactly(Map.of("i_id", 42L));
    }

    private QueryAuthorization grant(String company) {
        return grant(company, "finance:agent:account_subjects:query");
    }

    private QueryAuthorization grant(String company, String permission) {
        var scope = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of(company), null);
        return new QueryAuthorization(7L, 7L, permission,
                List.of(scope), List.of(scope), null);
    }
}
