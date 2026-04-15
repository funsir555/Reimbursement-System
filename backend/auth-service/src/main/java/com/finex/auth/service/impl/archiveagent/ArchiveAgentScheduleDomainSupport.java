// 业务域：档案代理与归档任务
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl.archiveagent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentSchedule;
import com.finex.auth.entity.ArchiveAgentTrigger;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ArchiveAgentScheduleDomainSupport：领域规则支撑类。
 * 承接 档案代理调度的核心业务规则。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
public class ArchiveAgentScheduleDomainSupport extends AbstractArchiveAgentSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ArchiveAgentScheduleDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

    /**
     * 执行Due调度。
     */
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
            ArchiveAgentRun run = createPendingRun(
                    definition,
                    version,
                    ArchiveAgentSupport.TRIGGER_TYPE_SCHEDULE,
                    "system-scheduler",
                    "瀹氭椂瑙﹀彂宸叉彁浜?",
                    schedule.getNextFireAt(),
                    Map.of(
                            "scheduleTriggerId", schedule.getTriggerId(),
                            "scheduledFireAt", formatDateTime(schedule.getNextFireAt())
                    )
            );

            schedule.setLastRunId(run.getId());
            schedule.setLastFireAt(now);
            schedule.setNextFireAt(ArchiveAgentSupport.computeNextFireAt(
                    trigger.getScheduleMode(),
                    trigger.getCronExpression(),
                    trigger.getIntervalMinutes(),
                    now
            ));
            schedule.setScheduleStatus(schedule.getNextFireAt() == null
                    ? ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED
                    : ArchiveAgentSupport.SCHEDULE_STATUS_IDLE);
            archiveAgentScheduleMapper.updateById(schedule);
            dispatchAfterCommit(run);
        }
    }
}
