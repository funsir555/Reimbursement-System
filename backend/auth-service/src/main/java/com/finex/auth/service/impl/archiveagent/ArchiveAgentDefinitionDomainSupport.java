// 业务域：档案代理与归档任务
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl.archiveagent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.dto.ArchiveAgentToolsSaveDTO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;
import com.finex.auth.dto.ArchiveAgentVersionVO;
import com.finex.auth.agent.workflow.WorkflowGraphPlan;
import com.finex.auth.agent.workflow.ControlledQueryCatalog;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunArtifact;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentSchedule;
import com.finex.auth.entity.ArchiveAgentToolBinding;
import com.finex.auth.entity.ArchiveAgentTrigger;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ArchiveAgentDefinitionDomainSupport：领域规则支撑类。
 * 承接 档案代理定义的核心业务规则。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
public class ArchiveAgentDefinitionDomainSupport extends AbstractArchiveAgentSupport {

    private static final Set<String> CONFIGURABLE_TOOLS = ControlledQueryCatalog.TOOLS.stream()
            .map(ControlledQueryCatalog.Definition::code)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ArchiveAgentDefinitionDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

    /**
     * 查询代理列表。
     */
    public List<ArchiveAgentSummaryVO> listAgents(Long ownerUserId, String keyword, String status) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedStatus = trimToNull(status);
        String filterStatus = normalizedStatus == null ? null : normalizedStatus.toUpperCase(Locale.ROOT);
        List<ArchiveAgentDefinition> agents = archiveAgentDefinitionMapper.selectList(
                Wrappers.<ArchiveAgentDefinition>lambdaQuery()
                        .eq(ArchiveAgentDefinition::getOwnerUserId, ownerUserId)
                        .like(normalizedKeyword != null, ArchiveAgentDefinition::getAgentName, normalizedKeyword)
                        .eq(filterStatus != null, ArchiveAgentDefinition::getStatus, filterStatus)
                        .orderByDesc(ArchiveAgentDefinition::getUpdatedAt, ArchiveAgentDefinition::getId)
        );
        if (agents.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> triggerCounts = archiveAgentTriggerMapper.selectList(
                Wrappers.<ArchiveAgentTrigger>lambdaQuery()
                        .in(ArchiveAgentTrigger::getAgentId, agents.stream().map(ArchiveAgentDefinition::getId).toList())
        ).stream().collect(Collectors.groupingBy(
                ArchiveAgentTrigger::getAgentId,
                Collectors.summingInt(item -> item.getEnabled() != null && item.getEnabled() == 1 ? 1 : 0)
        ));

        Map<Long, Integer> publishedVersionNo = loadPublishedVersionNo(
                agents.stream().map(ArchiveAgentDefinition::getPublishedVersionId).filter(Objects::nonNull).toList()
        );
        List<ArchiveAgentSummaryVO> result = new ArrayList<>();
        for (ArchiveAgentDefinition agent : agents) {
            ArchiveAgentSummaryVO summary = new ArchiveAgentSummaryVO();
            summary.setId(agent.getId());
            summary.setAgentCode(agent.getAgentCode());
            summary.setAgentName(agent.getAgentName());
            summary.setAgentDescription(agent.getAgentDescription());
            summary.setIconKey(agent.getIconKey());
            summary.setThemeKey(agent.getThemeKey());
            summary.setCoverColor(agent.getCoverColor());
            summary.setTags(readStringList(agent.getTagsJson()));
            summary.setStatus(agent.getStatus());
            summary.setLatestVersionNo(agent.getLatestVersionNo());
            summary.setPublishedVersionNo(publishedVersionNo.get(agent.getPublishedVersionId()));
            summary.setLastRunStatus(agent.getLastRunStatus());
            summary.setLastRunSummary(agent.getLastRunSummary());
            summary.setLastRunAt(formatDateTime(agent.getLastRunAt()));
            summary.setEnabledTriggerCount(triggerCounts.getOrDefault(agent.getId(), 0));
            summary.setRuntimeStatus(resolveRuntimeStatus(agent));
            result.add(summary);
        }
        return result;
    }

    /**
     * 获取代理明细。
     */
    public ArchiveAgentDetailVO getAgentDetail(Long ownerUserId, Long id) {
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

    /**
     * 创建代理。
     */
    public ArchiveAgentDetailVO createAgent(Long ownerUserId, String operatorName, ArchiveAgentSaveDTO dto) {
        validateSavePayload(dto);
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setAgentCode(ArchiveAgentSupport.buildAgentCode());
        definition.setOwnerUserId(ownerUserId);
        definition.setAgentName(dto.getAgentName().trim());
        definition.setAgentDescription(trimToNull(dto.getAgentDescription()));
        definition.setIconKey(trimToNull(dto.getIconKey()));
        definition.setThemeKey(trimToNull(dto.getThemeKey()));
        definition.setCoverColor(trimToNull(dto.getCoverColor()));
        definition.setTagsJson(writeJson(dto.getTags()));
        definition.setStatus(ArchiveAgentSupport.AGENT_STATUS_DRAFT);
        definition.setLatestVersionNo(1);
        archiveAgentDefinitionMapper.insert(definition);

        createVersion(definition, 1, operatorName, dto, false);
        syncBindings(definition.getId(), dto);
        archiveAgentDefinitionMapper.updateById(definition);
        return buildDetail(requireOwnedAgent(ownerUserId, definition.getId()));
    }

    /**
     * 更新代理。
     */
    public ArchiveAgentDetailVO updateAgent(Long ownerUserId, Long id, String operatorName, ArchiveAgentSaveDTO dto) {
        validateSavePayload(dto);
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        int nextVersionNo = (definition.getLatestVersionNo() == null ? 0 : definition.getLatestVersionNo()) + 1;
        definition.setAgentName(dto.getAgentName().trim());
        definition.setAgentDescription(trimToNull(dto.getAgentDescription()));
        definition.setIconKey(trimToNull(dto.getIconKey()));
        definition.setThemeKey(trimToNull(dto.getThemeKey()));
        definition.setCoverColor(trimToNull(dto.getCoverColor()));
        definition.setTagsJson(writeJson(dto.getTags()));
        definition.setLatestVersionNo(nextVersionNo);
        archiveAgentDefinitionMapper.updateById(definition);

        createVersion(definition, nextVersionNo, operatorName, dto, false);
        syncBindings(definition.getId(), dto);
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

    /** Updates only current catalog bindings; retired bindings and every other config remain intact. */
    public ArchiveAgentDetailVO updateTools(Long ownerUserId, Long id, String operatorName,
                                            ArchiveAgentToolsSaveDTO dto) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        if (dto == null || dto.getExpectedVersionNo() == null || dto.getToolCodes() == null
                || dto.getToolCodes().stream().anyMatch(code -> code == null || !CONFIGURABLE_TOOLS.contains(code))
                || dto.getToolCodes().size() != Set.copyOf(dto.getToolCodes()).size()) {
            throw new IllegalArgumentException("工具配置无效");
        }
        int current = definition.getLatestVersionNo() == null ? 0 : definition.getLatestVersionNo();
        if (current != dto.getExpectedVersionNo()) {
            throw new IllegalStateException("Agent 配置已变化，请刷新后重试");
        }
        Map<String, Object> config = new LinkedHashMap<>(readMap(requireLatestVersion(id).getConfigJson()));
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Map<String, Object>> currentBindings = new LinkedHashMap<>();
        for (Map<String, Object> tool : readList(config.get("tools"))) {
            String code = String.valueOf(tool.get("toolCode"));
            if (!CONFIGURABLE_TOOLS.contains(code)) {
                tools.add(new LinkedHashMap<>(tool));
            } else {
                currentBindings.put(code, new LinkedHashMap<>(tool));
            }
        }
        for (String code : dto.getToolCodes()) {
            Map<String, Object> binding = new LinkedHashMap<>(currentBindings.getOrDefault(code, Map.of()));
            binding.put("toolCode", code);
            binding.put("label", ControlledQueryCatalog.require(code).label());
            binding.put("category", "BUSINESS");
            binding.put("enabled", true);
            tools.add(binding);
        }
        int updated = archiveAgentDefinitionMapper.update(null,
                Wrappers.<ArchiveAgentDefinition>lambdaUpdate()
                        .eq(ArchiveAgentDefinition::getId, id)
                        .eq(ArchiveAgentDefinition::getLatestVersionNo, current)
                        .set(ArchiveAgentDefinition::getLatestVersionNo, current + 1));
        if (updated != 1) throw new IllegalStateException("Agent 配置已变化，请刷新后重试");
        config.put("tools", tools);
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setAgentId(id);
        version.setVersionNo(current + 1);
        version.setVersionLabel("v" + (current + 1) + " draft");
        version.setConfigJson(writeJson(config));
        version.setPublished(0);
        version.setCreatedByUserId(ownerUserId);
        version.setCreatedByName(operatorName);
        archiveAgentVersionMapper.insert(version);

        archiveAgentToolBindingMapper.delete(Wrappers.<ArchiveAgentToolBinding>lambdaQuery()
                .eq(ArchiveAgentToolBinding::getAgentId, id)
                .in(ArchiveAgentToolBinding::getToolCode, CONFIGURABLE_TOOLS));
        for (Map<String, Object> tool : tools) {
            String code = String.valueOf(tool.get("toolCode"));
            if (!CONFIGURABLE_TOOLS.contains(code)) continue;
            ArchiveAgentToolBinding binding = new ArchiveAgentToolBinding();
            binding.setAgentId(id);
            binding.setToolCode(code);
            binding.setEnabled(1);
            binding.setConfigJson(writeJson(tool));
            archiveAgentToolBindingMapper.insert(binding);
        }
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

    /**
     * 发布代理。
     */
    public ArchiveAgentDetailVO publishAgent(Long ownerUserId, Long id, String operatorName) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        ArchiveAgentVersion latestVersion = requireLatestVersion(id);

        archiveAgentVersionMapper.update(null, Wrappers.<ArchiveAgentVersion>lambdaUpdate()
                .eq(ArchiveAgentVersion::getAgentId, id)
                .set(ArchiveAgentVersion::getPublished, 0));
        latestVersion.setPublished(1);
        latestVersion.setVersionLabel("发布于 " + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + " by " + operatorName);
        archiveAgentVersionMapper.updateById(latestVersion);

        definition.setPublishedVersionId(latestVersion.getId());
        if (!ArchiveAgentSupport.AGENT_STATUS_DISABLED.equals(definition.getStatus())) {
            definition.setStatus(ArchiveAgentSupport.AGENT_STATUS_READY);
        }
        archiveAgentDefinitionMapper.updateById(definition);
        refreshScheduleState(definition.getId(), definition.getStatus());
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

    /**
     * 更新代理Status。
     */
    public ArchiveAgentDetailVO updateAgentStatus(Long ownerUserId, Long id, String status) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        String normalizedStatus = ArchiveAgentSupport.normalizeStatus(status);
        if (!List.of(
                ArchiveAgentSupport.AGENT_STATUS_DRAFT,
                ArchiveAgentSupport.AGENT_STATUS_READY,
                ArchiveAgentSupport.AGENT_STATUS_DISABLED,
                ArchiveAgentSupport.AGENT_STATUS_ARCHIVED
        ).contains(normalizedStatus)) {
            throw new IllegalArgumentException("不支持的 Agent 状态: " + status);
        }
        definition.setStatus(normalizedStatus);
        archiveAgentDefinitionMapper.updateById(definition);
        refreshScheduleState(definition.getId(), normalizedStatus);
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

    /**
     * 删除 Agent 的专属数据。凭据引用可能被同一用户的其他 Agent 复用，不在这里删除。
     */
    public void deleteAgent(Long ownerUserId, Long id) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        Long activeRunCount = archiveAgentRunMapper.selectCount(
                Wrappers.<ArchiveAgentRun>lambdaQuery()
                        .eq(ArchiveAgentRun::getAgentId, definition.getId())
                        .in(ArchiveAgentRun::getStatus,
                                ArchiveAgentSupport.RUN_STATUS_PENDING,
                                ArchiveAgentSupport.RUN_STATUS_RUNNING)
        );
        if (activeRunCount != null && activeRunCount > 0) {
            throw new IllegalStateException("Agent 正在运行，请等待运行结束后再删除");
        }

        List<Long> runIds = archiveAgentRunMapper.selectList(
                Wrappers.<ArchiveAgentRun>lambdaQuery()
                        .eq(ArchiveAgentRun::getAgentId, definition.getId())
        ).stream().map(ArchiveAgentRun::getId).filter(Objects::nonNull).toList();

        if (!runIds.isEmpty()) {
            archiveAgentRunArtifactMapper.delete(Wrappers.<ArchiveAgentRunArtifact>lambdaQuery()
                    .in(ArchiveAgentRunArtifact::getRunId, runIds));
            archiveAgentRunStepMapper.delete(Wrappers.<ArchiveAgentRunStep>lambdaQuery()
                    .in(ArchiveAgentRunStep::getRunId, runIds));
        }
        archiveAgentScheduleMapper.delete(Wrappers.<ArchiveAgentSchedule>lambdaQuery()
                .eq(ArchiveAgentSchedule::getAgentId, definition.getId()));
        archiveAgentTriggerMapper.delete(Wrappers.<ArchiveAgentTrigger>lambdaQuery()
                .eq(ArchiveAgentTrigger::getAgentId, definition.getId()));
        archiveAgentToolBindingMapper.delete(Wrappers.<ArchiveAgentToolBinding>lambdaQuery()
                .eq(ArchiveAgentToolBinding::getAgentId, definition.getId()));
        archiveAgentRunMapper.delete(Wrappers.<ArchiveAgentRun>lambdaQuery()
                .eq(ArchiveAgentRun::getAgentId, definition.getId()));
        archiveAgentVersionMapper.delete(Wrappers.<ArchiveAgentVersion>lambdaQuery()
                .eq(ArchiveAgentVersion::getAgentId, definition.getId()));
        archiveAgentDefinitionMapper.deleteById(definition.getId());
    }

    /**
     * 组装明细。
     */
    private ArchiveAgentDetailVO buildDetail(ArchiveAgentDefinition definition) {
        ArchiveAgentVersion latestVersion = requireLatestVersion(definition.getId());
        Map<String, Object> config = readMap(latestVersion.getConfigJson());
        ArchiveAgentDetailVO detail = new ArchiveAgentDetailVO();
        detail.setId(definition.getId());
        detail.setAgentCode(definition.getAgentCode());
        detail.setAgentName(definition.getAgentName());
        detail.setAgentDescription(definition.getAgentDescription());
        detail.setIconKey(definition.getIconKey());
        detail.setThemeKey(definition.getThemeKey());
        detail.setCoverColor(definition.getCoverColor());
        detail.setTags(readStringList(definition.getTagsJson()));
        detail.setStatus(definition.getStatus());
        detail.setLatestVersionNo(definition.getLatestVersionNo());
        detail.setPublishedVersionNo(resolvePublishedVersionNo(definition.getPublishedVersionId()));
        detail.setPromptConfig(readNestedMap(config, "promptConfig"));
        detail.setModelConfig(readNestedMap(config, "modelConfig"));
        detail.setTools(readList(config.get("tools")));
        detail.setWorkflow(readNestedMap(config, "workflow"));
        detail.setTriggers(readList(config.get("triggers")));
        detail.setInputSchema(readNestedMap(config, "inputSchema"));
        detail.setVersions(archiveAgentVersionMapper.selectList(
                Wrappers.<ArchiveAgentVersion>lambdaQuery()
                        .eq(ArchiveAgentVersion::getAgentId, definition.getId())
                        .orderByDesc(ArchiveAgentVersion::getVersionNo, ArchiveAgentVersion::getId)
        ).stream().map(this::toVersionVo).toList());
        detail.setLastRunStatus(definition.getLastRunStatus());
        detail.setLastRunSummary(definition.getLastRunSummary());
        detail.setLastRunAt(formatDateTime(definition.getLastRunAt()));
        return detail;
    }

    /**
     * 创建Version。
     */
    private ArchiveAgentVersion createVersion(
            ArchiveAgentDefinition definition,
            int versionNo,
            String operatorName,
            ArchiveAgentSaveDTO dto,
            boolean published
    ) {
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setAgentId(definition.getId());
        version.setVersionNo(versionNo);
        version.setVersionLabel("v" + versionNo + (published ? " published" : " draft"));
        version.setConfigJson(writeJson(buildConfigMap(definition, dto)));
        version.setPublished(published ? 1 : 0);
        version.setCreatedByUserId(definition.getOwnerUserId());
        version.setCreatedByName(operatorName);
        archiveAgentVersionMapper.insert(version);
        return version;
    }

    /**
     * 组装Config映射。
     */
    private Map<String, Object> buildConfigMap(ArchiveAgentDefinition definition, ArchiveAgentSaveDTO dto) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("agentCode", definition.getAgentCode());
        meta.put("agentName", dto.getAgentName().trim());
        meta.put("agentDescription", trimToNull(dto.getAgentDescription()));
        meta.put("iconKey", trimToNull(dto.getIconKey()));
        meta.put("themeKey", trimToNull(dto.getThemeKey()));
        meta.put("coverColor", trimToNull(dto.getCoverColor()));
        meta.put("tags", dto.getTags() == null ? List.of() : dto.getTags());

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("meta", meta);
        config.put("promptConfig", dto.getPromptConfig() == null ? Map.of() : dto.getPromptConfig());
        config.put("modelConfig", dto.getModelConfig() == null ? Map.of() : dto.getModelConfig());
        config.put("tools", dto.getTools() == null ? List.of() : dto.getTools());
        config.put("workflow", dto.getWorkflow() == null ? Map.of() : dto.getWorkflow());
        config.put("triggers", dto.getTriggers() == null ? List.of() : dto.getTriggers());
        config.put("inputSchema", dto.getInputSchema() == null ? Map.of() : dto.getInputSchema());
        return config;
    }

    /**
     * 同步Bindings。
     */
    private void syncBindings(Long agentId, ArchiveAgentSaveDTO dto) {
        archiveAgentTriggerMapper.delete(Wrappers.<ArchiveAgentTrigger>lambdaQuery().eq(ArchiveAgentTrigger::getAgentId, agentId));
        archiveAgentToolBindingMapper.delete(Wrappers.<ArchiveAgentToolBinding>lambdaQuery().eq(ArchiveAgentToolBinding::getAgentId, agentId));
        archiveAgentScheduleMapper.delete(Wrappers.<ArchiveAgentSchedule>lambdaQuery().eq(ArchiveAgentSchedule::getAgentId, agentId));

        for (Map<String, Object> tool : dto.getTools() == null ? List.<Map<String, Object>>of() : dto.getTools()) {
            ArchiveAgentToolBinding binding = new ArchiveAgentToolBinding();
            binding.setAgentId(agentId);
            binding.setToolCode(String.valueOf(tool.getOrDefault("toolCode", "")));
            binding.setEnabled(asBoolean(tool.get("enabled")) ? 1 : 0);
            binding.setCredentialRefCode(trimToNull(tool.get("credentialRefCode") == null ? null : String.valueOf(tool.get("credentialRefCode"))));
            binding.setConfigJson(writeJson(tool));
            archiveAgentToolBindingMapper.insert(binding);
        }

        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> triggerItem : dto.getTriggers() == null ? List.<Map<String, Object>>of() : dto.getTriggers()) {
            ArchiveAgentTrigger trigger = new ArchiveAgentTrigger();
            trigger.setAgentId(agentId);
            trigger.setTriggerType(String.valueOf(triggerItem.getOrDefault("triggerType", ArchiveAgentSupport.TRIGGER_TYPE_MANUAL)).toUpperCase(Locale.ROOT));
            trigger.setEnabled(asBoolean(triggerItem.getOrDefault("enabled", Boolean.TRUE)) ? 1 : 0);
            trigger.setScheduleMode(trimToNull(triggerItem.get("scheduleMode") == null ? null : String.valueOf(triggerItem.get("scheduleMode")).toUpperCase(Locale.ROOT)));
            trigger.setCronExpression(trimToNull(triggerItem.get("cronExpression") == null ? null : String.valueOf(triggerItem.get("cronExpression"))));
            trigger.setIntervalMinutes(asInteger(triggerItem.get("intervalMinutes")));
            trigger.setEventCode(trimToNull(triggerItem.get("eventCode") == null ? null : String.valueOf(triggerItem.get("eventCode"))));
            trigger.setConfigJson(writeJson(triggerItem));
            archiveAgentTriggerMapper.insert(trigger);

            if (ArchiveAgentSupport.TRIGGER_TYPE_SCHEDULE.equals(trigger.getTriggerType())) {
                ArchiveAgentSchedule schedule = new ArchiveAgentSchedule();
                schedule.setTriggerId(trigger.getId());
                schedule.setAgentId(agentId);
                schedule.setScheduleStatus(trigger.getEnabled() != null && trigger.getEnabled() == 1
                        ? ArchiveAgentSupport.SCHEDULE_STATUS_IDLE
                        : ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED);
                schedule.setNextFireAt(trigger.getEnabled() != null && trigger.getEnabled() == 1
                        ? ArchiveAgentSupport.computeNextFireAt(trigger.getScheduleMode(), trigger.getCronExpression(), trigger.getIntervalMinutes(), now)
                        : null);
                archiveAgentScheduleMapper.insert(schedule);
            }
        }
    }

    /**
     * 刷新调度State。
     */
    private void refreshScheduleState(Long agentId, String agentStatus) {
        List<ArchiveAgentSchedule> schedules = archiveAgentScheduleMapper.selectList(
                Wrappers.<ArchiveAgentSchedule>lambdaQuery().eq(ArchiveAgentSchedule::getAgentId, agentId)
        );
        if (schedules.isEmpty()) {
            return;
        }
        Map<Long, ArchiveAgentTrigger> triggerMap = archiveAgentTriggerMapper.selectList(
                Wrappers.<ArchiveAgentTrigger>lambdaQuery().eq(ArchiveAgentTrigger::getAgentId, agentId)
        ).stream().collect(Collectors.toMap(ArchiveAgentTrigger::getId, item -> item));

        LocalDateTime now = LocalDateTime.now();
        for (ArchiveAgentSchedule schedule : schedules) {
            ArchiveAgentTrigger trigger = triggerMap.get(schedule.getTriggerId());
            boolean enabled = ArchiveAgentSupport.AGENT_STATUS_READY.equals(agentStatus)
                    && trigger != null
                    && trigger.getEnabled() != null
                    && trigger.getEnabled() == 1;
            schedule.setScheduleStatus(enabled ? ArchiveAgentSupport.SCHEDULE_STATUS_IDLE : ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED);
            if (enabled && schedule.getNextFireAt() == null) {
                schedule.setNextFireAt(ArchiveAgentSupport.computeNextFireAt(
                        trigger.getScheduleMode(),
                        trigger.getCronExpression(),
                        trigger.getIntervalMinutes(),
                        now
                ));
            }
            archiveAgentScheduleMapper.updateById(schedule);
        }
    }

    /**
     * 校验SavePayload。
     */
    private void validateSavePayload(ArchiveAgentSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Agent 配置不能为空");
        }
        validateModelConfig(dto.getModelConfig());
        if (dto.getInputSchema() == null || dto.getInputSchema().isEmpty()) {
            throw new IllegalArgumentException("输入 Schema 不能为空");
        }
        if (dto.getWorkflow() == null) {
            throw new IllegalArgumentException("工作流配置不能为空");
        }
        WorkflowGraphPlan graph = WorkflowGraphPlan.from(dto.getWorkflow());
        List<Map<String, Object>> nodes = readList(dto.getWorkflow().get("nodes"));
        if (nodes.stream().filter(node -> "tool".equalsIgnoreCase(String.valueOf(node.get("nodeType"))))
                .map(WorkflowGraphPlan::nodeConfig)
                .map(config -> String.valueOf(config.getOrDefault("toolCode", "")))
                .anyMatch(ControlledQueryCatalog::isRegistered)) {
            graph.requireControlledQueryPlan();
        }
        List<Map<String, Object>> edges = readList(dto.getWorkflow().get("edges"));
        boolean hasStart = nodes.stream().anyMatch(node -> "start".equalsIgnoreCase(String.valueOf(node.get("nodeType"))));
        boolean hasEnd = nodes.stream().anyMatch(node -> "end".equalsIgnoreCase(String.valueOf(node.get("nodeType"))));
        if (nodes.isEmpty() || !hasStart || !hasEnd) {
            throw new IllegalArgumentException("工作流至少需要一个 start 节点和一个 end 节点");
        }
        List<String> nodeKeys = nodes.stream().map(node -> String.valueOf(node.getOrDefault("nodeKey", ""))).toList();
        for (Map<String, Object> edge : edges) {
            String source = String.valueOf(edge.getOrDefault("source", ""));
            String target = String.valueOf(edge.getOrDefault("target", ""));
            if (!nodeKeys.contains(source) || !nodeKeys.contains(target)) {
                throw new IllegalArgumentException("工作流连线存在无效节点引用");
            }
        }
        for (Map<String, Object> trigger : dto.getTriggers() == null ? List.<Map<String, Object>>of() : dto.getTriggers()) {
            String triggerType = String.valueOf(trigger.getOrDefault("triggerType", "")).toUpperCase(Locale.ROOT);
            if (ArchiveAgentSupport.TRIGGER_TYPE_SCHEDULE.equals(triggerType)) {
                String scheduleMode = String.valueOf(trigger.getOrDefault("scheduleMode", ArchiveAgentSupport.SCHEDULE_MODE_INTERVAL)).toUpperCase(Locale.ROOT);
                String cronExpression = trimToNull(trigger.get("cronExpression") == null ? null : String.valueOf(trigger.get("cronExpression")));
                Integer intervalMinutes = asInteger(trigger.get("intervalMinutes"));
                ArchiveAgentSupport.computeNextFireAt(scheduleMode, cronExpression, intervalMinutes, LocalDateTime.now());
            }
        }
    }

    private void validateModelConfig(Map<String, Object> modelConfig) {
        if (modelConfig == null || trimToNull(stringValue(modelConfig.get("provider"))) == null) {
            throw new IllegalArgumentException("请选择模型提供商");
        }
        if (trimToNull(stringValue(modelConfig.get("model"))) == null) {
            throw new IllegalArgumentException("请选择模型");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ArchiveAgentVersionVO toVersionVo(ArchiveAgentVersion version) {
        ArchiveAgentVersionVO vo = new ArchiveAgentVersionVO();
        vo.setId(version.getId());
        vo.setVersionNo(version.getVersionNo());
        vo.setVersionLabel(version.getVersionLabel());
        vo.setPublished(version.getPublished() != null && version.getPublished() == 1);
        vo.setCreatedByName(version.getCreatedByName());
        vo.setCreatedAt(formatDateTime(version.getCreatedAt()));
        return vo;
    }
}
