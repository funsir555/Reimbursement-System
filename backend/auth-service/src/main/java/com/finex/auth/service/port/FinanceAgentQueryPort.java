package com.finex.auth.service.port;

import com.finex.auth.service.queryauth.QueryAuthorization;
import java.util.List;
import java.util.Map;

/** Finance-owned read models for fixed Agent query templates. */
public interface FinanceAgentQueryPort {
    record AccountSet(String companyId, String companyName, int startYear, int startPeriod,
                      int endYear, int endPeriod) { }
    record Supplier(String code, String name) { }
    record Page(long total, int page, int pageSize, List<Map<String, Object>> records) {
        public boolean hasMore() { return (long) page * pageSize < total; }
    }
    Page query(String kind, QueryAuthorization authorization, Map<String, Object> parameters);
    List<AccountSet> accountSets(QueryAuthorization authorization);
    List<Supplier> suppliers(QueryAuthorization authorization, String companyId, String name);
}
