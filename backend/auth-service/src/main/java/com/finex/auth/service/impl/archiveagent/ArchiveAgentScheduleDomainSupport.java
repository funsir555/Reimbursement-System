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

public class ArchiveAgentScheduleDomainSupport extends AbstractArchiveAgentSupport {

    public ArchiveAgentScheduleDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

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
