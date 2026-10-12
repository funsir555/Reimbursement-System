package com.finex.auth.service.port;

import com.finex.auth.service.queryauth.QueryAuthorization;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 只读报销查询端口。
 *
 * <p>调用方只能提交服务端身份与受控筛选条件，不接触业务 Entity 或 Mapper。</p>
 */
public interface ExpenseAgentQueryPort {

    List<DocumentView> queryAuthorizedDocuments(Query query, QueryAuthorization authorization);
    long countAuthorizedDocuments(Query query, QueryAuthorization authorization);

    Aggregate aggregateAuthorizedDocuments(Query query, QueryAuthorization authorization);

    String fingerprintAuthorizedDocuments(Query query, QueryAuthorization authorization);

    record Aggregate(long count, BigDecimal totalAmount) { }

    enum Scope {
        SUBMITTED_BY_USER,
        ASSIGNED_TO_USER,
        VISIBLE
    }

    record Query(
            Long userId,
            String companyId,
            Scope scope,
            String templateType,
            boolean excludeDeleted,
            Instant startDate,
            Instant endDate,
            String status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String keyword,
            int limit
    ) {
    }

    record DocumentView(
            Long id,
            String documentCode,
            String title,
            String reason,
            BigDecimal amount,
            String status,
            String templateName,
            LocalDateTime createdAt
    ) {
    }
}
