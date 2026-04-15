// 业务域：档案代理与归档任务
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl.archiveagent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.dto.ArchiveAgentStepVO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentSchedule;
import com.finex.auth.entity.ArchiveAgentTrigger;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunArtifactMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.mapper.ArchiveAgentScheduleMapper;
import com.finex.auth.mapper.ArchiveAgentToolBindingMapper;
import com.finex.auth.mapper.ArchiveAgentTriggerMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import com.finex.auth.support.archiveagent.TriggerDispatcher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AbstractArchiveAgentSupport：通用支撑类。
 * 封装 档案代理这块可复用的业务能力。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
public abstract class AbstractArchiveAgentSupport {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
    protected final ArchiveAgentVersionMapper archiveAgentVersionMapper;
    protected final ArchiveAgentTriggerMapper archiveAgentTriggerMapper;
    protected final ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper;
    protected final ArchiveAgentRunMapper archiveAgentRunMapper;
    protected final ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
    protected final ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
    protected final ArchiveAgentScheduleMapper archiveAgentScheduleMapper;
    protected final ObjectMapper objectMapper;
    protected final TriggerDispatcher triggerDispatcher;

    public static final class Dependencies {
        final ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
        final ArchiveAgentVersionMapper archiveAgentVersionMapper;
        final ArchiveAgentTriggerMapper archiveAgentTriggerMapper;
        final ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper;
        final ArchiveAgentRunMapper archiveAgentRunMapper;
        final ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
        final ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
        final ArchiveAgentScheduleMapper archiveAgentScheduleMapper;
        final ObjectMapper objectMapper;
        final TriggerDispatcher triggerDispatcher;

        public Dependencies(
                ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper,
                ArchiveAgentVersionMapper archiveAgentVersionMapper,
                ArchiveAgentTriggerMapper archiveAgentTriggerMapper,
                ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper,
                ArchiveAgentRunMapper archiveAgentRunMapper,
                ArchiveAgentRunStepMapper archiveAgentRunStepMapper,
                ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper,
                ArchiveAgentScheduleMapper archiveAgentScheduleMapper,
                ObjectMapper objectMapper,
                TriggerDispatcher triggerDispatcher
        ) {
            this.archiveAgentDefinitionMapper = archiveAgentDefinitionMapper;
            this.archiveAgentVersionMapper = archiveAgentVersionMapper;
            this.archiveAgentTriggerMapper = archiveAgentTriggerMapper;
            this.archiveAgentToolBindingMapper = archiveAgentToolBindingMapper;
            this.archiveAgentRunMapper = archiveAgentRunMapper;
            this.archiveAgentRunStepMapper = archiveAgentRunStepMapper;
            this.archiveAgentRunArtifactMapper = archiveAgentRunArtifactMapper;
            this.archiveAgentScheduleMapper = archiveAgentScheduleMapper;
            this.objectMapper = objectMapper;
            this.triggerDispatcher = triggerDispatcher;
        }
    }

    /**
     * 初始化这个类所需的依赖组件。
     */
    protected AbstractArchiveAgentSupport(Dependencies dependencies) {
        this.archiveAgentDefinitionMapper = dependencies.archiveAgentDefinitionMapper;
        this.archiveAgentVersionMapper = dependencies.archiveAgentVersionMapper;
        this.archiveAgentTriggerMapper = dependencies.archiveAgentTriggerMapper;
        this.archiveAgentToolBindingMapper = dependencies.archiveAgentToolBindingMapper;
        this.archiveAgentRunMapper = dependencies.archiveAgentRunMapper;
        this.archiveAgentRunStepMapper = dependencies.archiveAgentRunStepMapper;
        this.archiveAgentRunArtifactMapper = dependencies.archiveAgentRunArtifactMapper;
        this.archiveAgentScheduleMapper = dependencies.archiveAgentScheduleMapper;
        this.objectMapper = dependencies.objectMapper;
        this.triggerDispatcher = dependencies.triggerDispatcher;
    }

    /**
     * 处理档案代理中的这一步。
     */
    public static Dependencies dependencies(
            ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper,
            ArchiveAgentVersionMapper archiveAgentVersionMapper,
            ArchiveAgentTriggerMapper archiveAgentTriggerMapper,
            ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper,
            ArchiveAgentRunMapper archiveAgentRunMapper,
            ArchiveAgentRunStepMapper archiveAgentRunStepMapper,
            ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper,
            ArchiveAgentScheduleMapper archiveAgentScheduleMapper,
            ObjectMapper objectMapper,
            TriggerDispatcher triggerDispatcher
    ) {
        return new Dependencies(
                archiveAgentDefinitionMapper,
                archiveAgentVersionMapper,
                archiveAgentTriggerMapper,
                archiveAgentToolBindingMapper,
                archiveAgentRunMapper,
                archiveAgentRunStepMapper,
                archiveAgentRunArtifactMapper,
                archiveAgentScheduleMapper,
                objectMapper,
                triggerDispatcher
        );
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected ArchiveAgentDefinition requireOwnedAgent(Long ownerUserId, Long id) {
        ArchiveAgentDefinition definition = archiveAgentDefinitionMapper.selectById(id);
        if (definition == null) {
            throw new IllegalArgumentException("Agent 涓嶅瓨鍦?");
        }
        if (!Objects.equals(definition.getOwnerUserId(), ownerUserId)) {
            throw new SecurityException("娌℃湁鏉冮檺璁块棶璇?Agent");
        }
        return definition;
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected ArchiveAgentVersion requireLatestVersion(Long agentId) {
        ArchiveAgentVersion version = archiveAgentVersionMapper.selectOne(
                Wrappers.<ArchiveAgentVersion>lambdaQuery()
                        .eq(ArchiveAgentVersion::getAgentId, agentId)
                        .orderByDesc(ArchiveAgentVersion::getVersionNo, ArchiveAgentVersion::getId)
                        .last("limit 1")
        );
        if (version == null) {
            throw new IllegalStateException("褰撳墠 Agent 灏氭湭鐢熸垚鐗堟湰蹇収");
        }
        return version;
    }

    /**
     * 解析RunnableVersion。
     */
    protected ArchiveAgentVersion resolveRunnableVersion(ArchiveAgentDefinition definition) {
        if (definition.getPublishedVersionId() != null) {
            ArchiveAgentVersion published = archiveAgentVersionMapper.selectById(definition.getPublishedVersionId());
            if (published != null) {
                return published;
            }
        }
        return requireLatestVersion(definition.getId());
    }

    /**
     * 加载PublishedVersionNo。
     */
    protected Map<Long, Integer> loadPublishedVersionNo(List<Long> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return Map.of();
        }
        return archiveAgentVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(ArchiveAgentVersion::getId, ArchiveAgentVersion::getVersionNo, (left, right) -> left));
    }

    /**
     * 解析PublishedVersionNo。
     */
    protected Integer resolvePublishedVersionNo(Long versionId) {
        if (versionId == null) {
            return null;
        }
        ArchiveAgentVersion version = archiveAgentVersionMapper.selectById(versionId);
        return version == null ? null : version.getVersionNo();
    }

    /**
     * 解析运行时Status。
     */
    protected String resolveRuntimeStatus(ArchiveAgentDefinition agent) {
        if (ArchiveAgentSupport.AGENT_STATUS_DISABLED.equals(agent.getStatus())) {
            return "DISABLED";
        }
        if (ArchiveAgentSupport.RUN_STATUS_RUNNING.equalsIgnoreCase(String.valueOf(agent.getLastRunStatus()))) {
            return "RUNNING";
        }
        if (ArchiveAgentSupport.RUN_STATUS_FAILED.equalsIgnoreCase(String.valueOf(agent.getLastRunStatus()))) {
            return "FAILED";
        }
        if (ArchiveAgentSupport.AGENT_STATUS_READY.equals(agent.getStatus())) {
            return "READY";
        }
        return "DRAFT";
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected void dispatchAfterCommit(ArchiveAgentRun run) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerDispatcher.dispatch(run);
                }
            });
            return;
        }
        triggerDispatcher.dispatch(run);
    }

    /**
     * 创建Pending执行。
     */
    protected ArchiveAgentRun createPendingRun(
            ArchiveAgentDefinition definition,
            ArchiveAgentVersion version,
            String triggerType,
            String triggerSource,
            String summary,
            LocalDateTime scheduledFireAt,
            Map<String, Object> inputPayload
    ) {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setRunNo(ArchiveAgentSupport.buildRunNo());
        run.setAgentId(definition.getId());
        run.setAgentVersionId(version.getId());
        run.setOwnerUserId(definition.getOwnerUserId());
        run.setTriggerType(triggerType);
        run.setTriggerSource(trimToNull(triggerSource));
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_PENDING);
        run.setSummary(summary);
        run.setScheduledFireAt(scheduledFireAt);
        run.setInputJson(writeJson(inputPayload == null ? Map.of() : inputPayload));
        archiveAgentRunMapper.insert(run);
        return run;
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected ArchiveAgentRunVO toRunVo(ArchiveAgentRun run) {
        ArchiveAgentRunVO vo = new ArchiveAgentRunVO();
        vo.setId(run.getId());
        vo.setRunNo(run.getRunNo());
        vo.setAgentId(run.getAgentId());
        vo.setTriggerType(run.getTriggerType());
        vo.setTriggerSource(run.getTriggerSource());
        vo.setStatus(run.getStatus());
        vo.setSummary(run.getSummary());
        vo.setErrorMessage(run.getErrorMessage());
        vo.setStartedAt(formatDateTime(run.getStartedAt()));
        vo.setFinishedAt(formatDateTime(run.getFinishedAt()));
        vo.setDurationMs(run.getDurationMs());
        return vo;
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected ArchiveAgentStepVO toStepVo(ArchiveAgentRunStep step) {
        ArchiveAgentStepVO vo = new ArchiveAgentStepVO();
        vo.setStepNo(step.getStepNo());
        vo.setNodeKey(step.getNodeKey());
        vo.setNodeType(step.getNodeType());
        vo.setNodeLabel(step.getNodeLabel());
        vo.setStatus(step.getStatus());
        vo.setErrorMessage(step.getErrorMessage());
        vo.setStartedAt(formatDateTime(step.getStartedAt()));
        vo.setFinishedAt(formatDateTime(step.getFinishedAt()));
        vo.setDurationMs(step.getDurationMs());
        vo.setInputPayload(readMap(step.getInputJson()));
        vo.setOutputPayload(readMap(step.getOutputJson()));
        return vo;
    }

    /**
     * 处理档案代理中的这一步。
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> readMap(String rawJson) {
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

    /**
     * 处理档案代理中的这一步。
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> readNestedMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    /**
     * 处理档案代理中的这一步。
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> readList(Object value) {
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

    /**
     * 处理档案代理中的这一步。
     */
    protected List<String> readStringList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    /**
     * 处理档案代理中的这一步。
     */
    protected Integer asInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
