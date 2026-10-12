package com.finex.auth.agent.tool;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.service.port.ExpenseAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报销单查询工具。
 */
@Slf4j
@Component
public class ExpenseQueryTool {

    private final ExpenseAgentQueryPort expenseQueryPort;
    private final QueryAuthorizationService authorizationService;

    public ExpenseQueryTool(ExpenseAgentQueryPort expenseQueryPort,
                            QueryAuthorizationService authorizationService) {
        this.expenseQueryPort = expenseQueryPort;
        this.authorizationService = authorizationService;
    }

    /**
     * 查询用户自己的报销单。
     */
    public String queryUserExpenses(QueryParams params) {
        Long currentUserId = TenantContext.getUserId();
        if (currentUserId == null || params == null ||
                (params.getUserId() != null && !currentUserId.equals(params.getUserId()))) {
            return "错误：无法获取当前用户信息";
        }

        try {
            int limit = params.getLimit() == null ? 10 : params.getLimit();
            var authorization = authorizationService.resolveForSelf(currentUserId,
                    QueryAuthorizationService.EXPENSE_QUERY);
            List<ExpenseAgentQueryPort.DocumentView> documents = expenseQueryPort.queryAuthorizedDocuments(
                    new ExpenseAgentQueryPort.Query(
                            currentUserId,
                            TenantContext.getEffectiveCompanyId(),
                            ExpenseAgentQueryPort.Scope.SUBMITTED_BY_USER,
                            "EXPENSE",
                            false,
                            params.getStartDate(),
                            params.getEndDate(),
                            params.getStatus(),
                            toBigDecimal(params.getMinAmount()),
                            toBigDecimal(params.getMaxAmount()),
                            null,
                            limit
                    ), authorization
            );
            return formatDocuments(documents);
        } catch (SecurityException e) {
            return QueryAuthorizationService.NO_PERMISSION;
        } catch (Exception e) {
            log.error("查询报销单失败", e);
            return "抱歉，查询报销单时发生错误，请稍后重试";
        }
    }

    private String formatDocuments(List<ExpenseAgentQueryPort.DocumentView> documents) {
        if (documents.isEmpty()) {
            return "未找到符合条件的报销单";
        }

        StringBuilder result = new StringBuilder();
        result.append("共找到 ").append(documents.size()).append(" 个报销单：\n\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int index = 0; index < documents.size(); index++) {
            ExpenseAgentQueryPort.DocumentView document = documents.get(index);
            result.append(index + 1).append(". ");
            result.append("单据号：").append(document.documentCode()).append("\n");
            if (document.amount() != null) {
                result.append("   金额：").append(document.amount()).append(" 元\n");
            }
            result.append("   状态：").append(translateStatus(document.status())).append("\n");
            if (document.createdAt() != null) {
                LocalDate date = document.createdAt().toLocalDate();
                result.append("   提交时间：").append(date.format(formatter)).append("\n");
            }
            if (document.title() != null && !document.title().isEmpty()) {
                result.append("   标题：").append(document.title()).append("\n");
            }
            result.append("\n");
        }
        return result.toString();
    }

    private String translateStatus(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已审批";
            case "REJECTED" -> "已驳回";
            case "PAID" -> "已支付";
            default -> status;
        };
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    public static class QueryParams {
        private Long userId;
        private Instant startDate;
        private Instant endDate;
        private String status;
        private Double minAmount;
        private Double maxAmount;
        private Integer limit;

        public static Builder builder() {
            return new Builder();
        }

        public Long getUserId() { return userId; }
        public Instant getStartDate() { return startDate; }
        public Instant getEndDate() { return endDate; }
        public String getStatus() { return status; }
        public Double getMinAmount() { return minAmount; }
        public Double getMaxAmount() { return maxAmount; }
        public Integer getLimit() { return limit; }

        public static class Builder {
            private final QueryParams params = new QueryParams();

            public Builder userId(Long userId) {
                params.userId = userId;
                return this;
            }

            public Builder startDate(Instant startDate) {
                params.startDate = startDate;
                return this;
            }

            public Builder endDate(Instant endDate) {
                params.endDate = endDate;
                return this;
            }

            public Builder status(String status) {
                params.status = status;
                return this;
            }

            public Builder minAmount(Double minAmount) {
                params.minAmount = minAmount;
                return this;
            }

            public Builder maxAmount(Double maxAmount) {
                params.maxAmount = maxAmount;
                return this;
            }

            public Builder limit(Integer limit) {
                params.limit = limit;
                return this;
            }

            public QueryParams build() {
                return params;
            }
        }
    }
}
