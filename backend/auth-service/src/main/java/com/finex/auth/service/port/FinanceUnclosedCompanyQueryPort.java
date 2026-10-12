package com.finex.auth.service.port;

import com.finex.auth.service.queryauth.QueryAuthorization;

public interface FinanceUnclosedCompanyQueryPort {
    int countUnclosed(QueryAuthorization authorization, int year, int period);
}
