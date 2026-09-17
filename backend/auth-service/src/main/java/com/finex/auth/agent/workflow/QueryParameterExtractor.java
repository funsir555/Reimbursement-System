package com.finex.auth.agent.workflow;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从用户输入中抽取受控查询变量。
 */
@Component
public class QueryParameterExtractor {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(超过|大于|高于|不少于|低于|小于|少于|不超过)\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern DOCUMENT_CODE_PATTERN = Pattern.compile("(DOC|EXP|BX|FY)[A-Za-z0-9_-]{4,}", Pattern.CASE_INSENSITIVE);
    private static final List<String> STOP_WORDS = List.of(
            "需要我处理的", "待我处理的", "待我审批的", "帮我总结", "帮我查询", "我当前", "查一下", "这个月",
            "查询", "查看", "显示", "总结", "最近", "待处理", "待我处理", "待我审批", "我的",
            "报销事项", "报销单", "报销", "费用", "单据", "事项", "本月", "上个月",
            "待审批", "未审批", "审批中", "待付款", "待支付", "已审批", "已驳回", "被驳回",
            "已支付", "已完成", "流程异常", "支付异常", "草稿", "超过", "大于", "高于",
            "不少于", "低于", "小于", "少于", "不超过", "有哪些", "有几个", "的"
    );

    public QueryTemplateVariables extract(String message) {
        String safeMessage = message == null ? "" : message.trim();
        QueryTemplateVariables.QueryTemplateVariablesBuilder builder = QueryTemplateVariables.builder()
                .rawKeyword(resolveKeyword(safeMessage))
                .normalizedKeyword(normalizeKeyword(resolveKeyword(safeMessage)))
                .scope(resolveScope(safeMessage))
                .status(resolveStatus(safeMessage))
                .limit(10);

        applyDateRange(builder, safeMessage);
        applyAmountRange(builder, safeMessage);
        return builder.build();
    }

    private void applyDateRange(QueryTemplateVariables.QueryTemplateVariablesBuilder builder, String message) {
        LocalDate now = LocalDate.now();
        if (message.contains("上个月")) {
            LocalDate start = now.minusMonths(1).withDayOfMonth(1);
            LocalDate end = now.withDayOfMonth(1).minusDays(1);
            builder.startDate(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
            builder.endDate(end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
            return;
        }
        if (message.contains("本月") || message.contains("这个月")) {
            LocalDate start = now.withDayOfMonth(1);
            builder.startDate(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
    }

    private void applyAmountRange(QueryTemplateVariables.QueryTemplateVariablesBuilder builder, String message) {
        Matcher matcher = AMOUNT_PATTERN.matcher(message);
        if (!matcher.find()) {
            return;
        }
        double amount = Double.parseDouble(matcher.group(2));
        String operator = matcher.group(1);
        if (List.of("低于", "小于", "少于", "不超过").contains(operator)) {
            builder.maxAmount(amount);
        } else {
            builder.minAmount(amount);
        }
    }

    private String resolveStatus(String message) {
        if (message.contains("待付款") || message.contains("待支付")) {
            return "PENDING_PAYMENT";
        }
        if (message.contains("待审批") || message.contains("未审批") || message.contains("审批中") || message.contains("待我审批")) {
            return "PENDING_APPROVAL";
        }
        if (message.contains("已审批")) {
            return "APPROVED";
        }
        if (message.contains("已驳回") || message.contains("被驳回")) {
            return "REJECTED";
        }
        if (message.contains("已支付")) {
            return "PAYMENT_COMPLETED";
        }
        if (message.contains("已完成")) {
            return "PAYMENT_FINISHED";
        }
        if (message.contains("流程异常")) {
            return "EXCEPTION";
        }
        if (message.contains("支付异常")) {
            return "PAYMENT_EXCEPTION";
        }
        if (message.contains("草稿")) {
            return "DRAFT";
        }
        return null;
    }

    private String resolveScope(String message) {
        if (message.contains("待处理")
                || message.contains("待我处理")
                || message.contains("需要我处理")
                || message.contains("待我审批")) {
            return QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME;
        }
        return QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME;
    }

    private String resolveKeyword(String message) {
        Matcher codeMatcher = DOCUMENT_CODE_PATTERN.matcher(message);
        if (codeMatcher.find()) {
            return codeMatcher.group();
        }

        String keyword = message;
        for (String stopWord : STOP_WORDS) {
            keyword = keyword.replace(stopWord, " ");
        }
        keyword = keyword.replaceAll("\\d+(?:\\.\\d+)?", " ");
        keyword = keyword.replaceAll("[，。！？、,.!?;；:：()（）\\[\\]【】]", " ");
        keyword = keyword.trim().replaceAll("\\s+", " ");
        if (keyword.length() > 40) {
            keyword = keyword.substring(0, 40).trim();
        }
        return keyword.isBlank() ? null : keyword;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
