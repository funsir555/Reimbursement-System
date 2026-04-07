package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentRunDetailVO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.dto.ArchiveAgentStepVO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;
import com.finex.auth.dto.ArchiveAgentVersionVO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunArtifact;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentSchedule;
import com.finex.auth.entity.ArchiveAgentToolBinding;
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
import com.finex.auth.service.ArchiveAgentService;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import com.finex.auth.support.archiveagent.TriggerDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
public class ArchiveAgentServiceImpl implements ArchiveAgentService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
    private final ArchiveAgentVersionMapper archiveAgentVersionMapper;
    private final ArchiveAgentTriggerMapper archiveAgentTriggerMapper;
    private final ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper;
    private final ArchiveAgentRunMapper archiveAgentRunMapper;
    private final ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
    private final ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
    private final ArchiveAgentScheduleMapper archiveAgentScheduleMapper;
    private final ObjectMapper objectMapper;
    private final TriggerDispatcher triggerDispatcher;

    @Override
    public List<ArchiveAgentSummaryVO> listAgents(Long ownerUserId, String keyword, String status) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedStatus = trimToNull(status);
        List<ArchiveAgentDefinition> agents = archiveAgentDefinitionMapper.selectList(
                Wrappers.<ArchiveAgentDefinition>lambdaQuery()
                        .eq(ArchiveAgentDefinition::getOwnerUserId, ownerUserId)
                        .like(normalizedKeyword != null, ArchiveAgentDefinition::getAgentName, normalizedKeyword)
                        .eq(normalizedStatus != null, ArchiveAgentDefinition::getStatus, normalizedStatus == null ? null : normalizedStatus.toUpperCase(Locale.ROOT))
                        .orderByDesc(ArchiveAgentDefinition::getUpdatedAt, ArchiveAgentDefinition::getId)
        );
        if (agents.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> triggerCounts = archiveAgentTriggerMapper.selectList(
                Wrappers.<ArchiveAgentTrigger>lambdaQuery().in(ArchiveAgentTrigger::getAgentId, agents.stream().map(ArchiveAgentDefinition::getId).toList())
        ).stream().collect(Collectors.groupingBy(ArchiveAgentTrigger::getAgentId, Collectors.summingInt(item -> item.getEnabled() != null && item.getEnabled() == 1 ? 1 : 0)));

        Map<Long, Integer> publishedVersionNo = loadPublishedVersionNo(agents.stream().map(ArchiveAgentDefinition::getPublishedVersionId).filter(Objects::nonNull).toList());
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

    @Override
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

    @Override
    public ArchiveAgentDetailVO getAgentDetail(Long ownerUserId, Long id) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        return buildDetail(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentRunVO runAgent(Long ownerUserId, Long id, ArchiveAgentRunDTO dto) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        if (ArchiveAgentSupport.AGENT_STATUS_DISABLED.equals(definition.getStatus()) || ArchiveAgentSupport.AGENT_STATUS_ARCHIVED.equals(definition.getStatus())) {
            throw new IllegalStateException("褰撳墠 Agent 宸插仠鐢ㄦ垨褰掓。锛屾棤娉曡繍琛?");
        }
        ArchiveAgentVersion version = resolveRunnableVersion(definition);
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setRunNo(ArchiveAgentSupport.buildRunNo());
        run.setAgentId(id);
        run.setAgentVersionId(version.getId());
        run.setOwnerUserId(ownerUserId);
        run.setTriggerType(ArchiveAgentSupport.TRIGGER_TYPE_MANUAL);
        run.setTriggerSource(trimToNull(dto == null ? null : dto.getTriggerSource()));
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_PENDING);
        run.setSummary("Agent 璇曡窇宸叉彁浜?");
        run.setInputJson(writeJson(dto == null ? Map.of() : dto.getInputPayload()));
        archiveAgentRunMapper.insert(run);
        dispatchAfterCommit(run);
        return toRunVo(run);
    }

    @Override
    public List<ArchiveAgentRunVO> listRuns(Long ownerUserId, Long agentId) {
        requireOwnedAgent(ownerUserId, agentId);
        return archiveAgentRunMapper.selectList(
                Wrappers.<ArchiveAgentRun>lambdaQuery()
                        .eq(ArchiveAgentRun::getOwnerUserId, ownerUserId)
                        .eq(ArchiveAgentRun::getAgentId, agentId)
                        .orderByDesc(ArchiveAgentRun::getCreatedAt, ArchiveAgentRun::getId)
                        .last("limit 20")
        ).stream().map(this::toRunVo).toList();
    }

    @Override
    public ArchiveAgentRunDetailVO getRunDetail(Long ownerUserId, Long runId) {
        ArchiveAgentRun run = archiveAgentRunMapper.selectById(runId);
        if (run == null || !Objects.equals(run.getOwnerUserId(), ownerUserId)) {
            throw new SecurityException("娌℃湁鏉冮檺鏌ョ湅璇ヨ繍琛岃褰?");
        }
        ArchiveAgentDefinition definition = archiveAgentDefinitionMapper.selectById(run.getAgentId());
        ArchiveAgentVersion version = archiveAgentVersionMapper.selectById(run.getAgentVersionId());

        ArchiveAgentRunDetailVO detail = new ArchiveAgentRunDetailVO();
        detail.setId(run.getId());
        detail.setRunNo(run.getRunNo());
        detail.setAgentId(run.getAgentId());
        detail.setAgentName(definition == null ? "" : definition.getAgentName());
        detail.setAgentVersionNo(version == null ? null : version.getVersionNo());
        detail.setTriggerType(run.getTriggerType());
        detail.setTriggerSource(run.getTriggerSource());
        detail.setStatus(run.getStatus());
        detail.setSummary(run.getSummary());
        detail.setErrorMessage(run.getErrorMessage());
        detail.setStartedAt(formatDateTime(run.getStartedAt()));
        detail.setFinishedAt(formatDateTime(run.getFinishedAt()));
        detail.setDurationMs(run.getDurationMs());
        detail.setInputPayload(readMap(run.getInputJson()));
        detail.setOutputPayload(readMap(run.getOutputJson()));
        detail.setSteps(archiveAgentRunStepMapper.selectList(
                Wrappers.<ArchiveAgentRunStep>lambdaQuery().eq(ArchiveAgentRunStep::getRunId, runId).orderByAsc(ArchiveAgentRunStep::getStepNo, ArchiveAgentRunStep::getId)
        ).stream().map(this::toStepVo).toList());
        detail.setArtifacts(archiveAgentRunArtifactMapper.selectList(
                Wrappers.<ArchiveAgentRunArtifact>lambdaQuery().eq(ArchiveAgentRunArtifact::getRunId, runId).orderByAsc(ArchiveAgentRunArtifact::getId)
        ).stream().map(item -> Map.of(
                "artifactKey", item.getArtifactKey(),
                "artifactType", item.getArtifactType(),
                "artifactName", item.getArtifactName() == null ? "" : item.getArtifactName(),
                "summary", item.getSummary() == null ? "" : item.getSummary(),
                "content", readMap(item.getContentJson())
        )).toList());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        List<ArchiveAgentSchedule> schedules = archiveAgentScheduleMapper.selectList(
                Wrappers.<ArchiveAgentSchedule>lambdaQuery()
                        .eq(ArchiveAgentSchedule::getScheduleStatus, ArchiveAgentSupport.SCHEDULE_STATUS_IDLE)
                        .le(ArchiveAgentSchedule::getNextFireAt, now)
                        .orderByAsc(ArchiveAgentSchedule::getNextFireAt, ArchiveAgentSchedule::getId)
                        .last("limit 20")
        );

        for (ArchiveAgentSchedule schedule : schedules) {
            ArchiveAgentTrigger trigger = archiveAgentTriggerMapper.selectById(schedule.getTriggerId());
            if (trigger == null || trigger.getEnabled() == null || trigger.getEnabled() != 1) {
                schedule.setScheduleStatus(ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED);
                archiveAgentScheduleMapper.updateById(schedule);
                continue;
            }
            ArchiveAgentDefinition definition = archiveAgentDefinitionMapper.selectById(schedule.getAgentId());
            if (definition == null || !ArchiveAgentSupport.AGENT_STATUS_READY.equals(definition.getStatus())) {
                schedule.setScheduleStatus(ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED);
                archiveAgentScheduleMapper.updateById(schedule);
                continue;
            }

            ArchiveAgentVersion version = resolveRunnableVersion(definition);
            ArchiveAgentRun run = new ArchiveAgentRun();
            run.setRunNo(ArchiveAgentSupport.buildRunNo());
            run.setAgentId(definition.getId());
            run.setAgentVersionId(version.getId());
            run.setOwnerUserId(definition.getOwnerUserId());
            run.setTriggerType(ArchiveAgentSupport.TRIGGER_TYPE_SCHEDULE);
            run.setTriggerSource("system-scheduler");
            run.setStatus(ArchiveAgentSupport.RUN_STATUS_PENDING);
            run.setSummary("瀹氭椂瑙﹀彂宸叉彁浜?");
            run.setScheduledFireAt(schedule.getNextFireAt());
            run.setInputJson(writeJson(Map.of("scheduleTriggerId", schedule.getTriggerId(), "scheduledFireAt", formatDateTime(schedule.getNextFireAt()))));
            archiveAgentRunMapper.insert(run);

            schedule.setLastRunId(run.getId());
            schedule.setLastFireAt(now);
            schedule.setNextFireAt(ArchiveAgentSupport.computeNextFireAt(trigger.getScheduleMode(), trigger.getCronExpression(), trigger.getIntervalMinutes(), now));
            schedule.setScheduleStatus(schedule.getNextFireAt() == null ? ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED : ArchiveAgentSupport.SCHEDULE_STATUS_IDLE);
            archiveAgentScheduleMapper.updateById(schedule);
            dispatchAfterCommit(run);
        }
    }

    private void dispatchAfterCommit(ArchiveAgentRun run) {
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
                Wrappers.<ArchiveAgentVersion>lambdaQuery().eq(ArchiveAgentVersion::getAgentId, definition.getId()).orderByDesc(ArchiveAgentVersion::getVersionNo, ArchiveAgentVersion::getId)
        ).stream().map(this::toVersionVo).toList());
        detail.setLastRunStatus(definition.getLastRunStatus());
        detail.setLastRunSummary(definition.getLastRunSummary());
        detail.setLastRunAt(formatDateTime(definition.getLastRunAt()));
        return detail;
    }

    private ArchiveAgentDefinition requireOwnedAgent(Long ownerUserId, Long id) {
        ArchiveAgentDefinition definition = archiveAgentDefinitionMapper.selectById(id);
        if (definition == null) {
            throw new IllegalArgumentException("Agent 涓嶅瓨鍦?");
        }
        if (!Objects.equals(definition.getOwnerUserId(), ownerUserId)) {
            throw new SecurityException("娌℃湁鏉冮檺璁块棶璇?Agent");
        }
        return definition;
    }

    private ArchiveAgentVersion requireLatestVersion(Long agentId) {
        ArchiveAgentVersion version = archiveAgentVersionMapper.selectOne(
                Wrappers.<ArchiveAgentVersion>lambdaQuery().eq(ArchiveAgentVersion::getAgentId, agentId).orderByDesc(ArchiveAgentVersion::getVersionNo, ArchiveAgentVersion::getId).last("limit 1")
        );
        if (version == null) {
            throw new IllegalStateException("褰撳墠 Agent 灏氭湭鐢熸垚鐗堟湰蹇収");
        }
        return version;
    }

    private ArchiveAgentVersion resolveRunnableVersion(ArchiveAgentDefinition definition) {
        if (definition.getPublishedVersionId() != null) {
            ArchiveAgentVersion published = archiveAgentVersionMapper.selectById(definition.getPublishedVersionId());
            if (published != null) {
                return published;
            }
        }
        return requireLatestVersion(definition.getId());
    }

    private ArchiveAgentVersion createVersion(ArchiveAgentDefinition definition, int versionNo, String operatorName, ArchiveAgentSaveDTO dto, boolean published) {
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

    @Transactional(rollbackFor = Exception.class)
    protected void syncBindings(Long agentId, ArchiveAgentSaveDTO dto) {
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
                schedule.setScheduleStatus(trigger.getEnabled() != null && trigger.getEnabled() == 1 ? ArchiveAgentSupport.SCHEDULE_STATUS_IDLE : ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED);
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
                schedule.setNextFireAt(ArchiveAgentSupport.computeNextFireAt(trigger.getScheduleMode(), trigger.getCronExpression(), trigger.getIntervalMinutes(), now));
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

    private Map<Long, Integer> loadPublishedVersionNo(List<Long> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return Map.of();
        }
        return archiveAgentVersionMapper.selectBatchIds(versionIds).stream().collect(Collectors.toMap(ArchiveAgentVersion::getId, ArchiveAgentVersion::getVersionNo));
    }

    private Integer resolvePublishedVersionNo(Long versionId) {
        if (versionId == null) {
            return null;
        }
        ArchiveAgentVersion version = archiveAgentVersionMapper.selectById(versionId);
        return version == null ? null : version.getVersionNo();
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

    private ArchiveAgentRunVO toRunVo(ArchiveAgentRun run) {
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

    private ArchiveAgentStepVO toStepVo(ArchiveAgentRunStep step) {
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

    private String resolveRuntimeStatus(ArchiveAgentDefinition agent) {
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

    @SuppressWarnings("unchecked")
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

    private List<String> readStringList(String rawJson) {
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean asBoolean(Object value) {
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

    private Integer asInteger(Object value) {
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
