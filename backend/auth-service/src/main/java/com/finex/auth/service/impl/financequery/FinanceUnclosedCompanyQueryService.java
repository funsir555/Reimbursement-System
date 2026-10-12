package com.finex.auth.service.impl.financequery;

import com.finex.auth.service.port.FinanceUnclosedCompanyQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finance owns the filtered cross-company period statistic. */
@Service
@RequiredArgsConstructor
public class FinanceUnclosedCompanyQueryService implements FinanceUnclosedCompanyQueryPort {
    private final JdbcTemplate jdbc;

    @Override
    public int countUnclosed(QueryAuthorization authorization, int year, int period) {
        if (authorization == null || !QueryAuthorizationService.FINANCE_CLOSE_QUERY.equals(authorization.permissionCode())
                || authorization.isEmpty()) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        if (year < 2000 || year > 2100 || period < 1 || period > 12)
            throw new IllegalArgumentException("请提供有效的会计年度与期间");
        List<String> companies = authorization.companyIds().stream()
                .filter(id -> authorization.allows(id, null)).sorted().toList();
        if (companies.isEmpty()) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        String placeholders = String.join(",", Collections.nCopies(companies.size(), "?"));
        List<Object> parameters = new ArrayList<>(companies);
        parameters.add(year);
        parameters.add(period);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_company c
                WHERE c.status=1 AND c.company_id IN (%s)
                  AND NOT EXISTS (
                    SELECT 1 FROM gl_period_close close_state
                    WHERE close_state.company_id=c.company_id AND close_state.iyear=?
                      AND close_state.iperiod=? AND close_state.status='CLOSED')
                """.formatted(placeholders), Integer.class, parameters.toArray());
        return count == null ? 0 : count;
    }
}
