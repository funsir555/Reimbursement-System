package com.finex.auth.support.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.desensitization.DesensitizationService;
import com.finex.auth.agent.desensitization.DesensitizedData;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelRoutingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveAgentModelRuntimeTest {

    @Test
    void routesNonMockProviderWithoutCredentialReferenceAndRestoresResponse() {
        ModelRoutingService modelRoutingService = mock(ModelRoutingService.class);
        DesensitizationService desensitizationService = mock(DesensitizationService.class);
        ArchiveAgentModelRuntime runtime = new ArchiveAgentModelRuntime(
                modelRoutingService,
                desensitizationService,
                new ObjectMapper(),
                List.of()
        );

        when(desensitizationService.desensitize(eq("run-1"), any())).thenReturn(DesensitizedData.builder()
                .success(true)
                .mappingId("mapping-1")
                .maskedText("{\"query\":\"查询[PERSON_001]的报销单\"}")
                .build());
        when(modelRoutingService.chatWithPreferredProvider(eq("LOCAL_7B"), any(), any(), any())).thenReturn(
                ChatModelProvider.ChatResponse.builder()
                        .success(true)
                        .model("deepseek-r1:7b")
                        .content("[PERSON_001]有一条待审批报销单")
                        .latencyMs(18)
                        .build()
        );
        when(desensitizationService.restore("mapping-1", "[PERSON_001]有一条待审批报销单"))
                .thenReturn("远智有一条待审批报销单");

        Map<String, Object> output = runtime.invoke(
                "run-1",
                7L,
                "费用助手",
                Map.of("provider", "LOCAL_7B", "model", "deepseek-r1:7b"),
                Map.of("systemPrompt", "仅处理当前用户数据"),
                Map.of("promptTemplate", "汇总查询结果"),
                Map.of("query", "查询远智的报销单")
        );

        assertEquals("远智有一条待审批报销单", output.get("content"));
        assertEquals("mapping-1", output.get("mappingId"));
        assertEquals("deepseek-r1:7b", output.get("model"));

        ArgumentCaptor<ChatModelProvider.ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatModelProvider.ChatRequest.class);
        verify(modelRoutingService).chatWithPreferredProvider(eq("LOCAL_7B"), any(), any(), requestCaptor.capture());
        String modelInput = requestCaptor.getValue().getMessages().get(0).getContent();
        assertTrue(modelInput.contains("[PERSON_001]"));
        assertTrue(!modelInput.contains("远智"));
    }

    @Test
    void keepsMockProviderOnExistingAdapterPath() {
        ModelRoutingService modelRoutingService = mock(ModelRoutingService.class);
        DesensitizationService desensitizationService = mock(DesensitizationService.class);
        ModelProviderAdapter mockAdapter = mock(ModelProviderAdapter.class);
        when(mockAdapter.getProviderCode()).thenReturn("MOCK");
        when(mockAdapter.invoke(any())).thenReturn(new ModelProviderAdapter.InvocationResult(
                "模拟结果",
                Map.of("content", "模拟结果")
        ));
        ArchiveAgentModelRuntime runtime = new ArchiveAgentModelRuntime(
                modelRoutingService,
                desensitizationService,
                new ObjectMapper(),
                List.of(mockAdapter)
        );

        Map<String, Object> output = runtime.invoke(
                "run-2",
                7L,
                "测试助手",
                Map.of("provider", "MOCK"),
                Map.of(),
                Map.of(),
                Map.of("query", "测试")
        );

        assertEquals("模拟结果", output.get("content"));
        verify(mockAdapter).invoke(any());
        verify(desensitizationService, never()).desensitize(any(), any());
        verify(modelRoutingService, never()).chatWithPreferredProvider(any(), any(), any(), any());
    }
}
