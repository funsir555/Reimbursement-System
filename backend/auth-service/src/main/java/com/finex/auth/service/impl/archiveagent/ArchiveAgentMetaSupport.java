// 业务域：档案代理与归档任务
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl.archiveagent;

import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import com.finex.auth.agent.workflow.ControlledQueryCatalog;

import java.util.List;
import java.util.Map;

/**
 * ArchiveAgentMetaSupport：通用支撑类。
 * 封装 档案代理这块可复用的业务能力。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
public class ArchiveAgentMetaSupport extends AbstractArchiveAgentSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ArchiveAgentMetaSupport(Dependencies dependencies) {
        super(dependencies);
    }

    /**
     * 获取元数据。
     */
    public ArchiveAgentMetaVO getMeta() {
        ArchiveAgentMetaVO meta = new ArchiveAgentMetaVO();
        meta.setModelProviders(List.of(
                Map.of("code", "MOCK", "label", "Mock 模型", "available", true, "requiresCredential", false, "defaultModel", "mock-agent-v1"),
                Map.of("code", "DEEPSEEK", "label", "DeepSeek", "available", true, "requiresCredential", false, "credentialMode", "PLATFORM_ENV", "defaultModel", "deepseek-chat"),
                Map.of("code", "KIMI", "label", "Kimi", "available", false, "requiresCredential", true, "defaultModel", "moonshot-v1-8k"),
                Map.of("code", "LOCAL_7B", "label", "本地模型（Ollama）", "available", true, "requiresCredential", false, "defaultModel", "deepseek-r1:7b")
        ));
        meta.setModelOptions(List.of(
                Map.of("provider", "MOCK", "model", "mock-agent-v1", "label", "Mock Agent v1", "available", true),
                Map.of("provider", "DEEPSEEK", "model", "deepseek-chat", "label", "DeepSeek Chat", "available", true),
                Map.of("provider", "DEEPSEEK", "model", "deepseek-reasoner", "label", "DeepSeek Reasoner", "available", true),
                Map.of("provider", "KIMI", "model", "moonshot-v1-8k", "label", "Moonshot v1 8K", "available", false),
                Map.of("provider", "LOCAL_7B", "model", "deepseek-r1:7b", "label", "本地 DeepSeek R1 7B", "available", true)
        ));
        meta.setTools(ControlledQueryCatalog.TOOLS.stream()
                .map(ControlledQueryCatalog.Definition::metadata).toList());
        meta.setNodeTypes(List.of(
                Map.of("code", "start", "label", "开始"),
                Map.of("code", "llm", "label", "LLM"),
                Map.of("code", "condition", "label", "条件判断"),
                Map.of("code", "tool", "label", "工具调用"),
                Map.of("code", "transform", "label", "变量转换"),
                Map.of("code", "notify", "label", "通知"),
                Map.of("code", "end", "label", "结束")
        ));
        meta.setTriggerTypes(List.of(
                Map.of("code", ArchiveAgentSupport.TRIGGER_TYPE_MANUAL, "label", "手动触发", "available", true),
                Map.of("code", ArchiveAgentSupport.TRIGGER_TYPE_SCHEDULE, "label", "定时触发", "available", true),
                Map.of("code", ArchiveAgentSupport.TRIGGER_TYPE_EVENT, "label", "事件触发", "available", false)
        ));
        meta.setIconOptions(List.of(
                Map.of("code", "pixel-duck", "label", "像素助手"),
                Map.of("code", "duck-terminal", "label", "终端助手"),
                Map.of("code", "duck-orbit", "label", "轨道助手")
        ));
        meta.setThemeOptions(List.of(
                Map.of("code", "duck-sunrise", "label", "日出黄"),
                Map.of("code", "duck-lake", "label", "湖水青"),
                Map.of("code", "duck-brass", "label", "黄铜灰")
        ));
        meta.setDefaultSystemPrompt("你是 FinEx Agent 工作流助手。只在已授权范围内调用工具，优先输出结构化结论，并保留必要的证据引用。");
        return meta;
    }
}
