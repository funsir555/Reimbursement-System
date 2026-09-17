package com.finex.auth.agent.workflow;

/**
 * 受控工具接口。
 */
public interface ControlledTool {

    String toolCode();

    default boolean supportsQueryTemplate(String queryTemplateCode) {
        return queryTemplateCode != null && !queryTemplateCode.isBlank();
    }

    ControlledToolResult execute(ToolExecutionRequest request);
}
