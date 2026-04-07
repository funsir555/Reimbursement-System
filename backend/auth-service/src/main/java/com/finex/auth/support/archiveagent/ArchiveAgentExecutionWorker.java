package com.finex.auth.support.archiveagent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunArtifact;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunArtifactMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveAgentExecutionWorker {

    private final ArchiveAgentRunMapper archiveAgentRunMapper;
    private final ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
    private final ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
    private final ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
    private final ArchiveAgentVersionMapper archiveAgentVersionMapper;
    private final ObjectMapper objectMapper;
    private final List<ModelProviderAdapter> modelProviderAdapters;
    private final List<ToolExecutor> toolExecutors;

    public void process(Long runId) {
        ArchiveAgentRun run = archiveAgentRunMapper.selectById(runId);
        if (run == null) {
            return;
        }

        ArchiveAgentDefinition definition = archiveAgentDefinitionMapper.selectById(run.getAgentId());
        ArchiveAgentVersion version = archiveAgentVersionMapper.selectById(run.getAgentVersionId());
        if (definition == null || version == null) {
            markRunFailed(run, "Agent ?????");
            return;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_RUNNING);
        run.setStartedAt(startedAt);
        run.setSummary("Agent ????");
        archiveAgentRunMapper.updateById(run);

        try {
            Map<String, Object> config = readMap(version.getConfigJson());
            Map<String, Object> promptConfig = readNestedMap(config, "promptConfig");
            Map<String, Object> modelConfig = readNestedMap(config, "modelConfig");
            Map<String, Object> workflow = readNestedMap(config, "workflow");
            List<Map<String, Object>> nodes = readList(workflow.get("nodes"));

            Map<String, Object> runtimeContext = new LinkedHashMap<>(readMap(run.getInputJson()));
            runtimeContext.putIfAbsent("agentName", definition.getAgentName());
            runtimeContext.putIfAbsent("triggerType", run.getTriggerType());

            String providerCode = String.valueOf(modelConfig.getOrDefault("provider", "MOCK")).toUpperCase(Locale.ROOT);
            ModelProviderAdapter modelProvider = modelProviderAdapters.stream()
                    .filter(item -> item.getProviderCode().equalsIgnoreCase(providerCode))
                    .findFirst()
                    .orElse(null);

            if (modelProvider == null) {
                throw new IllegalStateException("?? provider " + providerCode + " ????????? MOCK ????????");
            }
            if (!"MOCK".equals(providerCode) && isBlank(modelConfig.get("credentialCode"))) {
                throw new IllegalStateException("?? provider ?? credentialCode???????????");
            }

            int stepNo = 1;
            for (Map<String, Object> node : nodes) {
                String nodeType = String.valueOf(node.getOrDefault("nodeType", "")).toLowerCase(Locale.ROOT);
                String nodeKey = String.valueOf(node.getOrDefault("nodeKey", "node-" + stepNo));
                String nodeLabel = String.valueOf(node.getOrDefault("label", nodeKey));

                if (!ArchiveAgentSupport.NODE_TYPES.contains(nodeType)) {
                    throw new IllegalStateException("??????????: " + nodeType);
                }

                LocalDateTime stepStartedAt = LocalDateTime.now();
                ArchiveAgentRunStep step = new ArchiveAgentRunStep();
                step.setRunId(runId);
                step.setStepNo(stepNo++);
                step.setNodeKey(nodeKey);
                step.setNodeType(nodeType);
                step.setNodeLabel(nodeLabel);
                step.setStatus(ArchiveAgentSupport.RUN_STATUS_RUNNING);
                step.setStartedAt(stepStartedAt);
                step.setInputJson(writeJson(runtimeContext));
                archiveAgentRunStepMapper.insert(step);

                try {
                    Map<String, Object> stepOutput = executeNode(definition, modelProvider, runtimeContext, promptConfig, modelConfig, nodeType, node);
                    runtimeContext.put(nodeKey, stepOutput);
                    runtimeContext.put("lastNodeKey", nodeKey);
                    runtimeContext.put("lastOutput", stepOutput);
                    step.setStatus(ArchiveAgentSupport.RUN_STATUS_SUCCESS);
                    step.setFinishedAt(LocalDateTime.now());
                    step.setDurationMs(Duration.between(stepStartedAt, step.getFinishedAt()).toMillis());
                    step.setOutputJson(writeJson(stepOutput));
                    archiveAgentRunStepMapper.updateById(step);
                } catch (Exception ex) {
                    step.setStatus(ArchiveAgentSupport.RUN_STATUS_FAILED);
                    step.setFinishedAt(LocalDateTime.now());
                    step.setDurationMs(Duration.between(stepStartedAt, step.getFinishedAt()).toMillis());
                    step.setErrorMessage(ex.getMessage());
                    step.setOutputJson(writeJson(Map.of("error", ex.getMessage())));
                    archiveAgentRunStepMapper.updateById(step);
                    throw ex;
                }
            }

            ArchiveAgentRunArtifact artifact = new ArchiveAgentRunArtifact();
            artifact.setRunId(runId);
            artifact.setArtifactKey("final-output");
            artifact.setArtifactType("JSON");
            artifact.setArtifactName("?????");
            artifact.setSummary("Agent ??????");
            artifact.setContentJson(writeJson(runtimeContext));
            archiveAgentRunArtifactMapper.insert(artifact);

            run.setStatus(ArchiveAgentSupport.RUN_STATUS_SUCCESS);
            run.setFinishedAt(LocalDateTime.now());
            run.setDurationMs(Duration.between(startedAt, run.getFinishedAt()).toMillis());
            run.setSummary(String.valueOf(runtimeContext.getOrDefault("summary", "Agent ????")));
            run.setOutputJson(writeJson(runtimeContext));
            archiveAgentRunMapper.updateById(run);
            updateDefinitionAfterRun(definition, run, "SUCCESS");
        } catch (Exception ex) {
            log.warn("Archive agent run failed, runNo={}, message={}", run.getRunNo(), ex.getMessage(), ex);
            markRunFailed(run, ex.getMessage());
        }
    }

    private Map<String, Object> executeNode(
            ArchiveAgentDefinition definition,
            ModelProviderAdapter modelProvider,
            Map<String, Object> runtimeContext,
            Map<String, Object> promptConfig,
            Map<String, Object> modelConfig,
            String nodeType,
            Map<String, Object> node
    ) {
        Map<String, Object> nodeConfig = readNestedMap(node, "config");
        return switch (nodeType) {
            case "start" -> Map.of("message", "?????", "inputKeys", runtimeContext.keySet());
            case "llm" -> modelProvider.invoke(
                    new ModelProviderAdapter.InvocationContext(
                            definition.getOwnerUserId(),
                            definition.getAgentName(),
                            modelConfig,
                            promptConfig,
                            nodeConfig,
                            runtimeContext
                    )
            ).outputPayload();
            case "condition" -> evaluateCondition(nodeConfig, runtimeContext);
            case "tool" -> executeTool(definition.getOwnerUserId(), nodeConfig, runtimeContext);
            case "transform" -> transform(nodeConfig, runtimeContext);
            case "notify" -> notifyStep(definition.getOwnerUserId(), nodeConfig, runtimeContext);
            case "end" -> Map.of("message", "??????", "result", runtimeContext.get("lastOutput"));
            default -> throw new IllegalStateException("????????: " + nodeType);
        };
    }

    private Map<String, Object> evaluateCondition(Map<String, Object> nodeConfig, Map<String, Object> runtimeContext) {
        String field = String.valueOf(nodeConfig.getOrDefault("field", "lastNodeKey"));
        Object actual = runtimeContext.get(field);
        Object expected = nodeConfig.get("equals");
        boolean matched = expected == null ? actual != null : Objects.equals(String.valueOf(actual), String.valueOf(expected));
        return Map.of("matched", matched, "field", field, "actual", actual, "expected", expected);
    }

    private Map<String, Object> executeTool(Long userId, Map<String, Object> nodeConfig, Map<String, Object> runtimeContext) {
        String toolCode = String.valueOf(nodeConfig.getOrDefault("toolCode", ""));
        ToolExecutor executor = toolExecutors.stream()
                .filter(item -> item.supportedToolCodes().stream().anyMatch(code -> code.equalsIgnoreCase(toolCode)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("???? " + toolCode + " ????"));

        ToolExecutor.ToolResult result = executor.execute(
                new ToolExecutor.ToolContext(userId, toolCode, nodeConfig, runtimeContext)
        );
        Map<String, Object> output = new LinkedHashMap<>(result.outputPayload());
        output.put("summary", result.summary());
        output.put("toolCode", toolCode);
        return output;
    }

    private Map<String, Object> transform(Map<String, Object> nodeConfig, Map<String, Object> runtimeContext) {
        String template = String.valueOf(nodeConfig.getOrDefault("template", "Agent {{agentName}} ????????? {{lastNodeKey}}"));
        String rendered = template;
        for (Map.Entry<String, Object> entry : runtimeContext.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return Map.of("rendered", rendered);
    }

    private Map<String, Object> notifyStep(Long userId, Map<String, Object> nodeConfig, Map<String, Object> runtimeContext) {
        String title = String.valueOf(nodeConfig.getOrDefault("title", "Agent ????"));
        String content = String.valueOf(nodeConfig.getOrDefault("content", runtimeContext.getOrDefault("lastOutput", "Agent ???")));
        return executeTool(userId, Map.of("toolCode", "notify.send_message", "title", title, "content", content), runtimeContext);
    }

    private void markRunFailed(ArchiveAgentRun run, String message) {
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_FAILED);
        run.setFinishedAt(LocalDateTime.now());
        if (run.getStartedAt() != null) {
            run.setDurationMs(Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis());
        }
        run.setErrorMessage(message);
        run.setSummary(message);
        archiveAgentRunMapper.updateById(run);

        ArchiveAgentDefinition definition = archiveAgentDefinitionMapper.selectById(run.getAgentId());
        if (definition != null) {
            updateDefinitionAfterRun(definition, run, "FAILED");
        }
    }

    private void updateDefinitionAfterRun(ArchiveAgentDefinition definition, ArchiveAgentRun run, String status) {
        definition.setLastRunId(run.getId());
        definition.setLastRunStatus(status);
        definition.setLastRunSummary(run.getSummary());
        definition.setLastRunAt(run.getFinishedAt() == null ? run.getStartedAt() : run.getFinishedAt());
        archiveAgentDefinitionMapper.updateById(definition);
    }

    private Map<String, Object> readMap(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readNestedMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add(new LinkedHashMap<>((Map<String, Object>) map));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
