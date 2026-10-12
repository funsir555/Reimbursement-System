package com.finex.auth.agent.workflow;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.port.SystemAgentQueryPort;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 受控工具运行时。
 */
@Service
public class ToolRuntimeService {

    private final Map<String, ControlledTool> toolRegistry;

    public ToolRuntimeService(List<ControlledTool> tools) {
        this.toolRegistry = tools.stream().collect(Collectors.toMap(ControlledTool::toolCode, Function.identity()));
    }

    @Autowired
    public ToolRuntimeService(List<ControlledTool> tools, FinanceAgentQueryPort finance,
                              SystemAgentQueryPort system) {
        this.toolRegistry = new java.util.LinkedHashMap<>(tools.stream().collect(
                Collectors.toMap(ControlledTool::toolCode, Function.identity())));
        ControlledQueryCatalog.TOOLS.stream()
                .filter(item -> !"EXPENSE".equals(item.kind()) && !"CLOSE".equals(item.kind()))
                .forEach(item -> this.toolRegistry.put(item.code(), new RegisteredReadTool(item, finance, system)));
    }

    public ControlledToolResult execute(String toolCode, ToolExecutionRequest request) {
        ControlledTool tool = toolRegistry.get(toolCode);
        if (tool == null) {
            return ControlledToolResult.builder()
                    .success(false)
                    .errorMessage("未注册或未授权的工具: " + toolCode)
                    .records(List.of())
                    .evidenceRefs(List.of())
                    .build();
        }
        String queryTemplateCode = request == null ? null : request.getQueryTemplateCode();
        if (!tool.supportsQueryTemplate(queryTemplateCode)) {
            return ControlledToolResult.builder()
                    .success(false)
                    .errorMessage("工具 " + toolCode + " 不允许使用查询模板: " + queryTemplateCode)
                    .records(List.of())
                    .evidenceRefs(List.of())
                    .build();
        }
        return tool.execute(request);
    }

    public boolean supports(String toolCode) {
        return toolRegistry.containsKey(toolCode);
    }
}
