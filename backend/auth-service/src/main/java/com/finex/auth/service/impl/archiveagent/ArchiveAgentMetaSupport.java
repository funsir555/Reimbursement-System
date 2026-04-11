package com.finex.auth.service.impl.archiveagent;

import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;

import java.util.List;
import java.util.Map;

public class ArchiveAgentMetaSupport extends AbstractArchiveAgentSupport {

    public ArchiveAgentMetaSupport(Dependencies dependencies) {
        super(dependencies);
    }

    public ArchiveAgentMetaVO getMeta() {
        ArchiveAgentMetaVO meta = new ArchiveAgentMetaVO();
        meta.setModelProviders(List.of(
                Map.of("code", "MOCK", "label", "Mock Duck Engine", "available", true, "requiresCredential", false),
                Map.of("code", "OPENAI", "label", "OpenAI", "available", false, "requiresCredential", true),
                Map.of("code", "AZURE_OPENAI", "label", "Azure OpenAI", "available", false, "requiresCredential", true),
                Map.of("code", "LOCAL_GATEWAY", "label", "绉佹湁妯″瀷缃戝叧", "available", false, "requiresCredential", true)
        ));
        meta.setTools(List.of(
                Map.of("toolCode", "expense.query_my_expenses", "label", "???????", "category", "BUSINESS", "available", true),
                Map.of("toolCode", "invoice.query_my_invoices", "label", "??????", "category", "BUSINESS", "available", true),
                Map.of("toolCode", "notify.send_message", "label", "??????", "category", "BUSINESS", "available", true),
                Map.of("toolCode", "http.mock_request", "label", "HTTP ??", "category", "GENERAL", "available", true),
                Map.of("toolCode", "json.extract", "label", "JSON ??", "category", "GENERAL", "available", false),
                Map.of("toolCode", "async_task.submit", "label", "??????", "category", "BUSINESS", "available", false)
        ));
        meta.setNodeTypes(List.of(
                Map.of("code", "start", "label", "??"),
                Map.of("code", "llm", "label", "LLM"),
                Map.of("code", "condition", "label", "??"),
                Map.of("code", "tool", "label", "????"),
                Map.of("code", "transform", "label", "????"),
                Map.of("code", "notify", "label", "??"),
                Map.of("code", "end", "label", "??")
        ));
        meta.setTriggerTypes(List.of(
                Map.of("code", ArchiveAgentSupport.TRIGGER_TYPE_MANUAL, "label", "????", "available", true),
                Map.of("code", ArchiveAgentSupport.TRIGGER_TYPE_SCHEDULE, "label", "????", "available", true),
                Map.of("code", ArchiveAgentSupport.TRIGGER_TYPE_EVENT, "label", "????", "available", false)
        ));
        meta.setIconOptions(List.of(
                Map.of("code", "pixel-duck", "label", "?????"),
                Map.of("code", "duck-terminal", "label", "???"),
                Map.of("code", "duck-orbit", "label", "???")
        ));
        meta.setThemeOptions(List.of(
                Map.of("code", "duck-sunrise", "label", "???"),
                Map.of("code", "duck-lake", "label", "???"),
                Map.of("code", "duck-brass", "label", "???")
        ));
        meta.setDefaultSystemPrompt("????????? Agent????????????????????????????????????????");
        return meta;
    }
}
