// 业务域：档案代理与归档任务
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl.archiveagent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentRunDetailVO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.dto.ArchiveAgentStepVO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunArtifact;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ArchiveAgentRunDomainSupport：领域规则支撑类。
 * 承接 档案代理执行的核心业务规则。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
public class ArchiveAgentRunDomainSupport extends AbstractArchiveAgentSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ArchiveAgentRunDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

    /**
     * 执行代理。
     */
    public ArchiveAgentRunVO runAgent(Long ownerUserId, Long id, ArchiveAgentRunDTO dto) {
        ArchiveAgentDefinition definition = requireOwnedAgent(ownerUserId, id);
        if (ArchiveAgentSupport.AGENT_STATUS_DISABLED.equals(definition.getStatus())
                || ArchiveAgentSupport.AGENT_STATUS_ARCHIVED.equals(definition.getStatus())) {
            throw new IllegalStateException("当前 Agent 已停用或归档，无法运行");
        }
        // Manual workbench runs are previews of the latest saved canvas. Scheduled and
        // FinWork executions continue to use the published version.
        ArchiveAgentVersion version = dto != null && dto.isUsePublishedVersion()
                ? definition.getPublishedVersionId() == null ? null
                    : archiveAgentVersionMapper.selectById(definition.getPublishedVersionId())
                : requireLatestVersion(definition.getId());
        if (version == null || !definition.getId().equals(version.getAgentId())
                || (dto != null && dto.getExpectedVersionId() != null
                    && !dto.getExpectedVersionId().equals(version.getId()))) {
            throw new IllegalStateException("工作流版本已变化，请重新分析后试跑");
        }
        ArchiveAgentRun run = createPendingRun(
                definition,
                version,
                ArchiveAgentSupport.TRIGGER_TYPE_MANUAL,
                ArchiveAgentSupport.TRIGGER_SOURCE_WORKBENCH_TEST,
                "Agent 试跑已提交",
                null,
                dto == null ? Map.of() : dto.getInputPayload()
        );
        dispatchAfterCommit(run);
        return toRunVo(run);
    }

    /**
     * 查询执行列表。
     */
    public List<ArchiveAgentRunVO> listRuns(Long ownerUserId, Long agentId) {
        requireOwnedAgent(ownerUserId, agentId);
        List<ArchiveAgentRun> runs = archiveAgentRunMapper.selectList(
                Wrappers.<ArchiveAgentRun>lambdaQuery()
                        .eq(ArchiveAgentRun::getOwnerUserId, ownerUserId)
                        .eq(ArchiveAgentRun::getAgentId, agentId)
                        .orderByDesc(ArchiveAgentRun::getCreatedAt, ArchiveAgentRun::getId)
                        .last("limit 20")
        );
        Set<Long> versionIds = runs.stream().map(ArchiveAgentRun::getAgentVersionId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ArchiveAgentVersion> versions = versionIds.isEmpty() ? Map.of()
                : archiveAgentVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(ArchiveAgentVersion::getId, Function.identity()));
        return runs.stream().map(run -> {
            ArchiveAgentRunVO vo = toRunVo(run);
            if (isControlledQueryRun(run, versions.get(run.getAgentVersionId()))
                    && !isWorkbenchTestRun(run)) {
                vo.setSummary(protectedSummary(run.getStatus()));
                vo.setErrorMessage(protectedError(run.getStatus()));
            }
            return vo;
        }).toList();
    }

    /**
     * 获取执行明细。
     */
    public ArchiveAgentRunDetailVO getRunDetail(Long ownerUserId, Long runId) {
        ArchiveAgentRun run = archiveAgentRunMapper.selectById(runId);
        if (run == null || !Objects.equals(run.getOwnerUserId(), ownerUserId)) {
            throw new SecurityException("没有权限查看该运行记录");
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
        if (isControlledQueryRun(run, version) && !isWorkbenchTestRun(run)) {
            // Legacy rows may contain raw inputs, outputs, labels and errors without
            // authorization provenance. Only allowlisted execution metadata is shown.
            detail.setSummary(protectedSummary(run.getStatus()));
            detail.setInputPayload(Map.of());
            detail.setOutputPayload(Map.of("protected", true));
            detail.setErrorMessage(protectedError(run.getStatus()));
            detail.setSteps(archiveAgentRunStepMapper.selectList(
                    Wrappers.<ArchiveAgentRunStep>lambdaQuery()
                            .eq(ArchiveAgentRunStep::getRunId, runId)
                            .orderByAsc(ArchiveAgentRunStep::getStepNo, ArchiveAgentRunStep::getId)
            ).stream().map(this::toProtectedStepVo).toList());
            detail.setArtifacts(List.of());
            return detail;
        }
        detail.setInputPayload(readMap(run.getInputJson()));
        detail.setOutputPayload(readMap(run.getOutputJson()));
        detail.setSteps(archiveAgentRunStepMapper.selectList(
                Wrappers.<ArchiveAgentRunStep>lambdaQuery()
                        .eq(ArchiveAgentRunStep::getRunId, runId)
                        .orderByAsc(ArchiveAgentRunStep::getStepNo, ArchiveAgentRunStep::getId)
        ).stream().map(this::toStepVo).toList());
        detail.setArtifacts(archiveAgentRunArtifactMapper.selectList(
                Wrappers.<ArchiveAgentRunArtifact>lambdaQuery()
                        .eq(ArchiveAgentRunArtifact::getRunId, runId)
                        .orderByAsc(ArchiveAgentRunArtifact::getId)
        ).stream().map(this::toArtifact).toList());
        return detail;
    }

    private boolean isControlledQueryRun(ArchiveAgentRun run, ArchiveAgentVersion version) {
        return "FINWORK".equals(run.getTriggerSource())
                || (version != null && version.getConfigJson() != null
                && (version.getConfigJson().contains("expense.query_my_expenses")
                || version.getConfigJson().contains("finance.query_unclosed_companies")));
    }

    private boolean isWorkbenchTestRun(ArchiveAgentRun run) {
        if (!ArchiveAgentSupport.TRIGGER_TYPE_MANUAL.equals(run.getTriggerType())) {
            return false;
        }
        // These legacy sources were written only by the workbench run endpoint.
        // New runs use a server-owned marker; the client cannot select the source.
        return ArchiveAgentSupport.TRIGGER_SOURCE_WORKBENCH_TEST.equals(run.getTriggerSource())
                || "workbench.manual".equals(run.getTriggerSource())
                || "WORKBENCH".equals(run.getTriggerSource());
    }

    private String protectedSummary(String status) {
        if ("SUCCESS".equals(status) || "COMPLETED".equals(status)) {
            return "受控查询运行成功";
        }
        if ("FAILED".equals(status)) {
            return "受控查询运行失败";
        }
        if ("PENDING".equals(status)) {
            return "受控查询已提交";
        }
        return "受控查询运行中";
    }

    private String protectedError(String status) {
        return "FAILED".equals(status) ? "受控查询失败，敏感错误详情已隐藏" : null;
    }

    private ArchiveAgentStepVO toProtectedStepVo(ArchiveAgentRunStep step) {
        ArchiveAgentStepVO vo = new ArchiveAgentStepVO();
        vo.setStepNo(step.getStepNo());
        vo.setNodeKey("step_" + step.getStepNo());
        String nodeType = switch (String.valueOf(step.getNodeType())) {
            case "start", "llm", "condition", "tool", "transform", "notify", "end" -> step.getNodeType();
            default -> "unknown";
        };
        vo.setNodeType(nodeType);
        vo.setNodeLabel(switch (nodeType) {
            case "start" -> "开始";
            case "llm" -> "模型处理";
            case "condition" -> "条件判断";
            case "tool" -> "工具调用";
            case "transform" -> "数据处理";
            case "notify" -> "通知";
            case "end" -> "结束";
            default -> "工作流节点";
        });
        vo.setStatus(switch (String.valueOf(step.getStatus())) {
            case "PENDING", "RUNNING", "SUCCESS", "COMPLETED", "FAILED" -> step.getStatus();
            default -> "UNKNOWN";
        });
        vo.setStartedAt(formatDateTime(step.getStartedAt()));
        vo.setFinishedAt(formatDateTime(step.getFinishedAt()));
        vo.setDurationMs(step.getDurationMs());
        return vo;
    }

    private Map<String, Object> toArtifact(ArchiveAgentRunArtifact artifact) {
        return Map.of(
                "artifactKey", artifact.getArtifactKey(),
                "artifactType", artifact.getArtifactType(),
                "artifactName", artifact.getArtifactName() == null ? "" : artifact.getArtifactName(),
                "summary", artifact.getSummary() == null ? "" : artifact.getSummary(),
                "content", readMap(artifact.getContentJson())
        );
    }
}
