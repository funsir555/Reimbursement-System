// 业务域：档案代理与归档任务
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl.archiveagent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentRunDetailVO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunArtifact;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            throw new IllegalStateException("褰撳墠 Agent 宸插仠鐢ㄦ垨褰掓。锛屾棤娉曡繍琛?");
        }
        ArchiveAgentVersion version = resolveRunnableVersion(definition);
        ArchiveAgentRun run = createPendingRun(
                definition,
                version,
                ArchiveAgentSupport.TRIGGER_TYPE_MANUAL,
                dto == null ? null : dto.getTriggerSource(),
                "Agent 璇曡窇宸叉彁浜?",
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
        return archiveAgentRunMapper.selectList(
                Wrappers.<ArchiveAgentRun>lambdaQuery()
                        .eq(ArchiveAgentRun::getOwnerUserId, ownerUserId)
                        .eq(ArchiveAgentRun::getAgentId, agentId)
                        .orderByDesc(ArchiveAgentRun::getCreatedAt, ArchiveAgentRun::getId)
                        .last("limit 20")
        ).stream().map(this::toRunVo).toList();
    }

    /**
     * 获取执行明细。
     */
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
