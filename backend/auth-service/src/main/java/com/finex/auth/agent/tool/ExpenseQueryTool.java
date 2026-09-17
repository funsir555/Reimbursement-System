package com.finex.auth.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报销单查询工具
 * 第一个真实的 Agent 工具
 */
@Slf4j
@Component
public class ExpenseQueryTool {

    private final ProcessDocumentInstanceMapper documentMapper;

    public ExpenseQueryTool(ProcessDocumentInstanceMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    /**
     * 查询用户的报销单
     */
    public String queryUserExpenses(QueryParams params) {
        // 权限校验：只能查询自己的报销单
        Long currentUserId = params.getUserId();
        if (currentUserId == null) {
            return "错误：无法获取当前用户信息";
        }

        try {
            // 构建查询条件
            LambdaQueryWrapper<ProcessDocumentInstance> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProcessDocumentInstance::getSubmitterUserId, currentUserId);

            // 时间范围过滤
            if (params.getStartDate() != null) {
                LocalDateTime startDateTime = LocalDateTime.ofInstant(params.getStartDate(), ZoneId.systemDefault());
                wrapper.ge(ProcessDocumentInstance::getCreatedAt, startDateTime);
            }
            if (params.getEndDate() != null) {
                LocalDateTime endDateTime = LocalDateTime.ofInstant(params.getEndDate(), ZoneId.systemDefault());
                wrapper.le(ProcessDocumentInstance::getCreatedAt, endDateTime);
            }

            // 状态过滤
            if (params.getStatus() != null && !params.getStatus().isEmpty()) {
                wrapper.eq(ProcessDocumentInstance::getStatus, params.getStatus());
            }

            // 金额范围过滤
            if (params.getMinAmount() != null) {
                wrapper.ge(ProcessDocumentInstance::getTotalAmount, params.getMinAmount());
            }
            if (params.getMaxAmount() != null) {
                wrapper.le(ProcessDocumentInstance::getTotalAmount, params.getMaxAmount());
            }

            // 只查询报销单类型
            wrapper.eq(ProcessDocumentInstance::getTemplateType, "EXPENSE");

            // 排序和限制
            wrapper.orderByDesc(ProcessDocumentInstance::getCreatedAt);

            int limit = params.getLimit() != null ? params.getLimit() : 10;
            if (limit > 50) {
                limit = 50; // 最多返回50条
            }
            wrapper.last("LIMIT " + limit);

            // 执行查询
            List<ProcessDocumentInstance> documents = documentMapper.selectList(wrapper);

            // 格式化结果
            return formatDocuments(documents);

        } catch (Exception e) {
            log.error("查询报销单失败", e);
            return "抱歉，查询报销单时发生错误：" + e.getMessage();
        }
    }

    /**
     * 格式化报销单列表
     */
    private String formatDocuments(List<ProcessDocumentInstance> documents) {
        if (documents.isEmpty()) {
            return "未找到符合条件的报销单";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(documents.size()).append(" 个报销单：\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < documents.size(); i++) {
            ProcessDocumentInstance doc = documents.get(i);
            sb.append(i + 1).append(". ");
            sb.append("单据号：").append(doc.getDocumentCode()).append("\n");

            if (doc.getTotalAmount() != null) {
                sb.append("   金额：").append(doc.getTotalAmount()).append(" 元\n");
            }

            sb.append("   状态：").append(translateStatus(doc.getStatus())).append("\n");

            if (doc.getCreatedAt() != null) {
                LocalDate date = doc.getCreatedAt().toLocalDate();
                sb.append("   提交时间：").append(date.format(formatter)).append("\n");
            }

            if (doc.getDocumentTitle() != null && !doc.getDocumentTitle().isEmpty()) {
                sb.append("   标题：").append(doc.getDocumentTitle()).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 翻译状态
     */
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

    /**
     * 查询参数
     */
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
