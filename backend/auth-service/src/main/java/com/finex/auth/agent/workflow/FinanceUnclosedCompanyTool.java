package com.finex.auth.agent.workflow;

import com.finex.auth.service.port.FinanceUnclosedCompanyQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class FinanceUnclosedCompanyTool implements ControlledTool {
    public static final String TOOL_CODE = "finance.query_unclosed_companies";
    public static final String TEMPLATE_CODE = "finance.unclosed_companies.v1";
    private static final Pattern PERIOD = Pattern.compile("(20\\d{2})\\s*(?:年|[-/])\\s*(1[0-2]|0?[1-9])\\s*(?:月|期)?");

    private final FinanceUnclosedCompanyQueryPort finance;

    @Override public String toolCode() { return TOOL_CODE; }
    @Override public boolean supportsQueryTemplate(String code) { return TEMPLATE_CODE.equals(code); }

    @Override
    public ControlledToolResult execute(ToolExecutionRequest request) {
        if (request == null || request.getAuthorization() == null
                || !QueryAuthorizationService.FINANCE_CLOSE_QUERY.equals(request.getAuthorization().permissionCode()))
            return failed(QueryAuthorizationService.NO_PERMISSION);
        Matcher matcher = PERIOD.matcher(request.getUserMessage() == null ? "" : request.getUserMessage());
        if (!matcher.find()) return failed("请明确会计年度和期间，例如 2026 年 9 月");
        int year = Integer.parseInt(matcher.group(1));
        int period = Integer.parseInt(matcher.group(2));
        int count = finance.countUnclosed(request.getAuthorization(), year, period);
        String summary = year + " 年 " + period + " 月，您有权查看的范围内有 " + count
                + " 家公司未结账。仅统计您有权查看的范围。";
        return ControlledToolResult.builder().success(true).summary(summary)
                .records(List.of(Map.of("year", year, "period", period, "count", count)))
                .totalCount((long) count)
                .evidenceRefs(List.of()).build();
    }

    private ControlledToolResult failed(String message) {
        return ControlledToolResult.builder().success(false).errorMessage(message)
                .records(List.of()).evidenceRefs(List.of()).build();
    }
}
