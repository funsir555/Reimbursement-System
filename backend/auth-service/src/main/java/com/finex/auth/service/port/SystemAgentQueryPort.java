package com.finex.auth.service.port;

import com.finex.auth.service.queryauth.QueryAuthorization;
import java.util.Map;

/** System-owned personnel and organization read models for Agent tools. */
public interface SystemAgentQueryPort {
    FinanceAgentQueryPort.Page query(String kind, QueryAuthorization authorization, Map<String, Object> parameters);
}
