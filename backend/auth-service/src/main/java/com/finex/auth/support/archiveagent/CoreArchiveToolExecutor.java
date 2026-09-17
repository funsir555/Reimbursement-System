package com.finex.auth.support.archiveagent;

import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.service.MvpDataService;
import com.finex.auth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CoreArchiveToolExecutor implements ToolExecutor {

    private final MvpDataService mvpDataService;
    private final NotificationService notificationService;

    @Override
    public List<String> supportedToolCodes() {
        return List.of(
                "expense.query_my_expenses",
                "invoice.query_my_invoices",
                "notify.send_message",
                "http.mock_request"
        );
    }

    @Override
    public ToolResult execute(ToolContext context) {
        return switch (context.toolCode()) {
            case "expense.query_my_expenses" -> queryExpenses(context.userId());
            case "invoice.query_my_invoices" -> queryInvoices(context.userId());
            case "notify.send_message" -> notifyUser(context);
            case "http.mock_request" -> mockHttp(context);
            default -> new ToolResult(
                    "工具 " + context.toolCode() + " 尚未接入真实执行器",
                    Map.of("toolCode", context.toolCode(), "mode", "MOCK")
            );
        };
    }

    private ToolResult queryExpenses(Long userId) {
        List<ExpenseSummaryVO> expenses = mvpDataService.listExpenses(userId);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("count", expenses.size());
        output.put("documents", expenses.stream().limit(3).map(ExpenseSummaryVO::getDocumentCode).toList());
        return new ToolResult("已查询到当前用户的报销单 " + expenses.size() + " 条", output);
    }

    private ToolResult queryInvoices(Long userId) {
        List<InvoiceSummaryVO> invoices = mvpDataService.listInvoices(userId);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("count", invoices.size());
        output.put("invoiceCodes", invoices.stream().limit(3).map(InvoiceSummaryVO::getCode).toList());
        return new ToolResult("已查询到当前用户的发票 " + invoices.size() + " 条", output);
    }

    private ToolResult notifyUser(ToolContext context) {
        String title = String.valueOf(context.toolConfig().getOrDefault("title", "Agent 通知"));
        String content = String.valueOf(context.toolConfig().getOrDefault("content", "你的 Agent 已完成执行"));
        notificationService.sendAsyncNotification(context.userId(), "AGENT", title, content, null);
        return new ToolResult("站内通知已发送", Map.of("title", title, "content", content));
    }

    private ToolResult mockHttp(ToolContext context) {
        String url = String.valueOf(context.toolConfig().getOrDefault("url", "https://example.invalid/mock"));
        return new ToolResult("HTTP 模拟请求已完成", Map.of("url", url, "status", 200, "body", "mock-response"));
    }
}
