package com.finex.auth.agent.model.impl;

import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.support.archiveagent.ModelProviderAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeepSeekProviderTest {

    @Test
    void passesSelectedModelToAdapter() {
        ModelProviderAdapter adapter = mock(ModelProviderAdapter.class);
        when(adapter.invoke(any())).thenAnswer(invocation -> {
            ModelProviderAdapter.InvocationContext context = invocation.getArgument(0);
            assertEquals("deepseek-reasoner", context.modelConfig().get("model"));
            return new ModelProviderAdapter.InvocationResult("ok", Map.of());
        });
        DeepSeekProvider provider = new DeepSeekProvider(adapter);

        ChatModelProvider.ChatResponse response = provider.chat(ChatModelProvider.ChatRequest.builder()
                .messages(List.of())
                .extraParams(Map.of("model", "deepseek-reasoner"))
                .build());

        assertTrue(response.isSuccess());
        assertEquals("deepseek-reasoner", response.getModel());
    }
}
