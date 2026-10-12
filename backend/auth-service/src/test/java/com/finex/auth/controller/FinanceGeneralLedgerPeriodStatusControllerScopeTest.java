package com.finex.auth.controller;

import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceGeneralLedgerPeriodStatusService;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceGeneralLedgerPeriodStatusControllerScopeTest {
    private final FinanceGeneralLedgerPeriodStatusService finance = mock(FinanceGeneralLedgerPeriodStatusService.class);
    private final AccessControlService access = mock(AccessControlService.class);
    private final QueryAuthorizationService authorizations = mock(QueryAuthorizationService.class);
    private final FinanceGeneralLedgerPeriodStatusController controller =
            new FinanceGeneralLedgerPeriodStatusController(finance, access, authorizations);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void clientCompanyOutsideScopeNeverReachesFinanceService() {
        when(request.getAttribute("currentUserId")).thenReturn(11L);
        when(authorizations.resolveForSelf(11L, QueryAuthorizationService.FINANCE_CLOSE_QUERY))
                .thenReturn(access(Set.of("B")));

        assertThatThrownBy(() -> controller.overview("A", 2026, 2026, 9, request))
                .isInstanceOf(SecurityException.class)
                .hasMessage(QueryAuthorizationService.NO_PERMISSION);
        verify(finance, never()).getOverview(any(), any(), any(), any(), any());
    }

    @Test
    void missingCompanyRequiresChoiceUnlessTheAuthorizedSetIsUnique() {
        when(request.getAttribute("currentUserId")).thenReturn(11L);
        when(authorizations.resolveForSelf(11L, QueryAuthorizationService.FINANCE_CLOSE_QUERY))
                .thenReturn(access(Set.of("B", "C")), access(Set.of("B")));

        assertThatThrownBy(() -> controller.overview(null, 2026, 2026, 9, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请选择有权查看的公司");
        controller.overview(null, 2026, 2026, 9, request);
        verify(finance).getOverview(11L, "B", 2026, 2026, 9);
    }

    private QueryAuthorization access(Set<String> companies) {
        var grant = new QueryAuthorization.Grant("SELECTED_COMPANIES", companies, null);
        return new QueryAuthorization(11L, 11L, QueryAuthorizationService.FINANCE_CLOSE_QUERY,
                List.of(grant), List.of(grant), null);
    }
}
