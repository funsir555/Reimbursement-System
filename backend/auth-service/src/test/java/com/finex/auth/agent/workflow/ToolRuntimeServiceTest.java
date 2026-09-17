package com.finex.auth.agent.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRuntimeServiceTest {

    @Test
    void rejectsQueryTemplateOutsideToolWhitelist() {
        ControlledTool tool = new ControlledTool() {
            @Override
            public String toolCode() {
                return "test.query";
            }

            @Override
            public boolean supportsQueryTemplate(String queryTemplateCode) {
                return "test.v1".equals(queryTemplateCode);
            }

            @Override
            public ControlledToolResult execute(ToolExecutionRequest request) {
                return ControlledToolResult.builder().success(true).build();
            }
        };
        ToolRuntimeService runtime = new ToolRuntimeService(List.of(tool));

        ControlledToolResult rejected = runtime.execute("test.query", ToolExecutionRequest.builder()
                .queryTemplateCode("untrusted.v1")
                .build());
        ControlledToolResult accepted = runtime.execute("test.query", ToolExecutionRequest.builder()
                .queryTemplateCode("test.v1")
                .build());

        assertFalse(rejected.isSuccess());
        assertTrue(accepted.isSuccess());
    }
}
