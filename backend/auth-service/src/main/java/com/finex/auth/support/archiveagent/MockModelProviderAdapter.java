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
        String systemPrompt = String.valueOf(context.promptConfig().getOrDefault("systemPrompt", "你是一个自动化 Agent。"));
        String nodePrompt = String.valueOf(context.nodeConfig().getOrDefault("promptTemplate", "请根据当前上下文生成结构化结果。"));
        String agentName = context.agentName();
        String content = "Mock 模型已执行：" + agentName + " | " + systemPrompt + " | " + nodePrompt;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("provider", getProviderCode());
        output.put("model", String.valueOf(context.modelConfig().getOrDefault("model", "mock-duck-1")));
        output.put("content", content);
        output.put("contextKeys", java.util.List.copyOf(context.runtimeContext().keySet()));
        return new InvocationResult(content, output);
    }
}
