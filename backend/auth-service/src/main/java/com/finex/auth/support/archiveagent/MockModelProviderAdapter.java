package com.finex.auth.support.archiveagent;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MockModelProviderAdapter implements ModelProviderAdapter {

    @Override
    public String getProviderCode() {
        return "MOCK";
    }

    @Override
    public InvocationResult invoke(InvocationContext context) {
        String systemPrompt = String.valueOf(context.promptConfig().getOrDefault("systemPrompt", "??????? Agent?"));
        String nodePrompt = String.valueOf(context.nodeConfig().getOrDefault("promptTemplate", "?????????????"));
        String agentName = context.agentName();
        String content = "Mock ???????" + agentName + " | " + systemPrompt + " | " + nodePrompt;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("provider", getProviderCode());
        output.put("model", String.valueOf(context.modelConfig().getOrDefault("model", "mock-duck-1")));
        output.put("content", content);
        output.put("contextKeys", context.runtimeContext().keySet());
        return new InvocationResult(content, output);
    }
}
