package com.finex.auth.service.impl.archiveagent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;
import com.finex.auth.dto.ArchiveAgentVersionVO;
import com.finex.auth.entity.ArchiveAgentDefinition;
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
import java.util.stream.Collectors;

public class ArchiveAgentDefinitionDomainSupport extends AbstractArchiveAgentSupport {

    public ArchiveAgentDefinitionDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

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

    public ArchiveAgentDetailVO getAgentDetail(Long ownerUserId, Long id) {
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

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

    public ArchiveAgentDetailVO publishAgent(Long ownerUserId, Long id, String operatorName) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        ArchiveAgentVersion latestVersion = requireLatestVersion(id);

        archiveAgentVersionMapper.update(null, Wrappers.<ArchiveAgentVersion>lambdaUpdate()
                .eq(ArchiveAgentVersion::getAgentId, id)
                .set(ArchiveAgentVersion::getPublished, 0));
        latestVersion.setPublished(1);
        latestVersion.setVersionLabel("鍙戝竷浜?" + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + " by " + operatorName);
        archiveAgentVersionMapper.updateById(latestVersion);

        definition.setPublishedVersionId(latestVersion.getId());
        if (!ArchiveAgentSupport.AGENT_STATUS_DISABLED.equals(definition.getStatus())) {
            definition.setStatus(ArchiveAgentSupport.AGENT_STATUS_READY);
        }
        archiveAgentDefinitionMapper.updateById(definition);
        refreshScheduleState(definition.getId(), definition.getStatus());
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

    public ArchiveAgentDetailVO updateAgentStatus(Long ownerUserId, Long id, String status) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        String normalizedStatus = ArchiveAgentSupport.normalizeStatus(status);
        if (!List.of(
                ArchiveAgentSupport.AGENT_STATUS_DRAFT,
                ArchiveAgentSupport.AGENT_STATUS_READY,
                ArchiveAgentSupport.AGENT_STATUS_DISABLED,
                ArchiveAgentSupport.AGENT_STATUS_ARCHIVED
        ).contains(normalizedStatus)) {
            throw new IllegalArgumentException("涓嶆敮鎸佺殑 Agent 鐘舵€? " + status);
        }
        definition.setStatus(normalizedStatus);
        archiveAgentDefinitionMapper.updateById(definition);
        refreshScheduleState(definition.getId(), normalizedStatus);
        return buildDetail(requireOwnedAgent(ownerUserId, id));
    }

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

    private void validateSavePayload(ArchiveAgentSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Agent 閰嶇疆涓嶈兘涓虹┖");
        }
        if (dto.getWorkflow() == null) {
            return;
        }
        List<Map<String, Object>> nodes = readList(dto.getWorkflow().get("nodes"));
        List<Map<String, Object>> edges = readList(dto.getWorkflow().get("edges"));
        boolean hasStart = nodes.stream().anyMatch(node -> "start".equalsIgnoreCase(String.valueOf(node.get("nodeType"))));
        boolean hasEnd = nodes.stream().anyMatch(node -> "end".equalsIgnoreCase(String.valueOf(node.get("nodeType"))));
        if (!nodes.isEmpty() && (!hasStart || !hasEnd)) {
            throw new IllegalArgumentException("娴佺▼缂栨帓鑷冲皯闇€瑕佷竴涓?start 鑺傜偣鍜屼竴涓?end 鑺傜偣");
        }
        List<String> nodeKeys = nodes.stream().map(node -> String.valueOf(node.getOrDefault("nodeKey", ""))).toList();
        for (Map<String, Object> edge : edges) {
            String source = String.valueOf(edge.getOrDefault("source", ""));
            String target = String.valueOf(edge.getOrDefault("target", ""));
            if (!nodeKeys.contains(source) || !nodeKeys.contains(target)) {
                throw new IllegalArgumentException("娴佺▼杩炵嚎瀛樺湪鏃犳晥鑺傜偣寮曠敤");
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
