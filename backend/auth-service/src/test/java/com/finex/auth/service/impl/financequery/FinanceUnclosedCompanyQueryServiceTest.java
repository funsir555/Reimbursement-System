package com.finex.auth.service.impl.financequery;

import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceUnclosedCompanyQueryServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FinanceUnclosedCompanyQueryService service = new FinanceUnclosedCompanyQueryService(jdbc);

    @Test
    void countsOnlyCompaniesInEffectiveIntersection() {
        var request = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("A", "B"), null);
        var responsible = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("B", "C"), null);
        var access = new QueryAuthorization(1L, 2L, QueryAuthorizationService.FINANCE_CLOSE_QUERY,
                List.of(request), List.of(responsible), null);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("B"), eq(2026), eq(9)))
                .thenReturn(1);

        assertThat(service.countUnclosed(access, 2026, 9)).isEqualTo(1);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sql.capture(), eq(Integer.class), eq("B"), eq(2026), eq(9));
        assertThat(sql.getValue()).contains("c.company_id IN (?)", "close_state.status='CLOSED'");
    }

    @Test
    void rejectsAbsentCapabilityAndInvalidPeriodBeforeDatabaseRead() {
        var grant = new QueryAuthorization.Grant("ALL_COMPANIES", Set.of("A"), null);
        var wrong = new QueryAuthorization(1L, 1L, QueryAuthorizationService.EXPENSE_QUERY,
                List.of(grant), List.of(grant), null);
        var finance = new QueryAuthorization(1L, 1L, QueryAuthorizationService.FINANCE_CLOSE_QUERY,
                List.of(grant), List.of(grant), null);

        assertThatThrownBy(() -> service.countUnclosed(wrong, 2026, 9)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.countUnclosed(finance, 2026, 13)).isInstanceOf(IllegalArgumentException.class);
    }
}
