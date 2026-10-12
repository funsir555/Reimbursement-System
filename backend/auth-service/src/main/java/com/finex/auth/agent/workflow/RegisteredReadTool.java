package com.finex.auth.agent.workflow;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.port.SystemAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorizationService;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adapter between the fixed catalog and domain-owned read ports. */
final class RegisteredReadTool implements ControlledTool {
    private final ControlledQueryCatalog.Definition definition;
    private final FinanceAgentQueryPort finance;
    private final SystemAgentQueryPort system;

    RegisteredReadTool(ControlledQueryCatalog.Definition definition,
                       FinanceAgentQueryPort finance, SystemAgentQueryPort system) {
        this.definition = definition;
        this.finance = finance;
        this.system = system;
    }

    @Override public String toolCode() { return definition.code(); }
    @Override public boolean supportsQueryTemplate(String code) { return definition.template().equals(code); }

    @Override
    public ControlledToolResult execute(ToolExecutionRequest request) {
        if (request == null || request.getAuthorization() == null || request.getUserId() == null
                || !definition.permission().equals(request.getAuthorization().permissionCode())
                || !request.getUserId().equals(request.getAuthorization().requesterUserId())
                || (TenantContext.getUserId() != null && !TenantContext.getUserId().equals(request.getUserId())))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        Map<String, Object> values = request.getParameters() == null ? Map.of() : request.getParameters();
        Set<String> allowed = definition.fields().stream().map(ControlledQueryCatalog.Field::key)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!allowed.containsAll(values.keySet())) throw new IllegalArgumentException("存在不支持的工具参数");
        FinanceAgentQueryPort.Page page = "PEOPLE".equals(definition.kind())
                || "DEPARTMENT".equals(definition.kind())
                ? system.query(definition.kind(), request.getAuthorization(), values)
                : finance.query(definition.kind(), request.getAuthorization(), values);
        Map<String, Object> descriptor = new LinkedHashMap<>(values);
        descriptor.put("toolCode", definition.code());
        String summary = "在您有权查看的范围内匹配 " + page.total() + " 条，本页展示 "
                + page.records().size() + " 条。仅统计您有权查看的范围。";
        if ("SUPPLIER_LEDGER".equals(definition.kind()))
            summary += "会计期间：" + values.get("yearFrom") + "年" + values.get("periodFrom")
                    + "月至" + values.get("yearTo") + "年" + values.get("periodTo")
                    + "月；供应商编码：" + values.get("supplierCode")
                    + "；凭证范围：" + ("POSTED_ONLY".equals(values.get("voucherStatus"))
                    ? "仅已记账" : "包含已保存未记账") + "；查询时间：" + Instant.now() + "。";
        if ("INCLUDE_SAVED_UNPOSTED".equals(values.get("voucherStatus")))
            summary += "已包含可编辑的未记账凭证；"
                    + ("SUPPLIER_LEDGER".equals(definition.kind()) ? "" : "查询时间 " + Instant.now() + "，")
                    + "结果为非最终口径。";
        return ControlledToolResult.builder().success(true).summary(summary).records(page.records())
                .totalCount(page.total()).page(page.page()).pageSize(page.pageSize())
                .hasMore(page.hasMore()).queryDescriptor(descriptor)
                .sourceDigest(digest(page.total() + "|" + page.records() + "|" + descriptor))
                .evidenceRefs(List.of()).build();
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("无法生成查询来源摘要", exception); }
    }
}
