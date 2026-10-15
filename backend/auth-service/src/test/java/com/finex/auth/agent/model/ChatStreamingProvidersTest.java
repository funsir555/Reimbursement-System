package com.finex.auth.agent.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.model.impl.LocalModelProvider;
import com.finex.auth.support.archiveagent.DeepSeekModelProviderAdapter;
import com.finex.auth.support.archiveagent.ModelProviderAdapter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatStreamingProvidersTest {
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void deepSeekParsesIncrementalSseDeltas() throws Exception {
        String endpoint = serve("data: {\"choices\":[{\"delta\":{\"content\":\"你\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"好\"}}]}\n\n"
                + "data: [DONE]\n\n", "text/event-stream");
        DeepSeekModelProviderAdapter adapter = new DeepSeekModelProviderAdapter(new ObjectMapper());
        ReflectionTestUtils.setField(adapter, "endpoint", endpoint);
        ReflectionTestUtils.setField(adapter, "apiKey", "test-key");
        List<String> chunks = new ArrayList<>();
        ModelProviderAdapter.InvocationResult result = adapter.invokeStream(
                new ModelProviderAdapter.InvocationContext(null, "finwork-chat",
                        Map.of("model", "deepseek-chat"), Map.of("messages", List.of(
                        Map.of("role", "user", "content", "你好"))), Map.of(), Map.of()),
                chunks::add);
        assertThat(chunks).containsExactly("你", "好");
        assertThat(result.content()).isEqualTo("你好");
    }

    @Test
    void localModelParsesIncrementalNdjson() throws Exception {
        String endpoint = serve("{\"message\":{\"content\":\"你\"},\"done\":false}\n"
                + "{\"message\":{\"content\":\"好\"},\"done\":true}\n", "application/x-ndjson");
        ModelRoutingConfig config = mock(ModelRoutingConfig.class);
        when(config.getProviders()).thenReturn(List.of());
        LocalModelProvider provider = new LocalModelProvider(config, new ObjectMapper());
        ReflectionTestUtils.setField(provider, "endpoint", endpoint);
        ReflectionTestUtils.setField(provider, "modelName", "local-test");
        List<String> chunks = new ArrayList<>();
        ChatModelProvider.ChatResponse response = provider.chatStream(ChatModelProvider.ChatRequest.builder()
                .messages(List.of(ChatModelProvider.ChatMessage.builder()
                        .role("user").content("你好").build())).build(), chunks::add);
        assertThat(response.isSuccess()).isTrue();
        assertThat(chunks).containsExactly("你", "好");
        assertThat(response.getContent()).isEqualTo("你好");
    }

    private String serve(String text, String contentType) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", exchange -> {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                int split = Math.min(bytes.length, Math.max(1, bytes.length / 2));
                output.write(bytes, 0, split);
                output.flush();
                output.write(bytes, split, bytes.length - split);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/chat";
    }
}
