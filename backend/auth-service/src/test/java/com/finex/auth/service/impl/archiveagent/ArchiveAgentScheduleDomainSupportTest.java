package com.finex.auth.service.impl.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentScheduleDomainSupportTest {

    @Mock private ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
    @Mock private ArchiveAgentVersionMapper archiveAgentVersionMapper;
    @Mock private ArchiveAgentTriggerMapper archiveAgentTriggerMapper;
    @Mock private ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper;
    @Mock private ArchiveAgentRunMapper archiveAgentRunMapper;
    @Mock private ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
    @Mock private ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
    @Mock private ArchiveAgentScheduleMapper archiveAgentScheduleMapper;
    @Mock private TriggerDispatcher triggerDispatcher;

    private ArchiveAgentScheduleDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new ArchiveAgentScheduleDomainSupport(AbstractArchiveAgentSupport.dependencies(
                archiveAgentDefinitionMapper,
                archiveAgentVersionMapper,
                archiveAgentTriggerMapper,
                archiveAgentToolBindingMapper,
                archiveAgentRunMapper,
                archiveAgentRunStepMapper,
                archiveAgentRunArtifactMapper,
                archiveAgentScheduleMapper,
                new ObjectMapper(),
                triggerDispatcher
        ));
    }

    @Test
    void runDueSchedulesDisablesScheduleWhenTriggerMissing() {
        ArchiveAgentSchedule schedule = new ArchiveAgentSchedule();
        schedule.setId(1L);
        schedule.setTriggerId(2L);
        schedule.setAgentId(3L);
        schedule.setScheduleStatus(ArchiveAgentSupport.SCHEDULE_STATUS_IDLE);
        schedule.setNextFireAt(LocalDateTime.now().minusMinutes(1));

        when(archiveAgentScheduleMapper.selectList(any())).thenReturn(List.of(schedule));
        when(archiveAgentTriggerMapper.selectById(2L)).thenReturn(null);

        support.runDueSchedules();

        ArgumentCaptor<ArchiveAgentSchedule> captor = ArgumentCaptor.forClass(ArchiveAgentSchedule.class);
        verify(archiveAgentScheduleMapper).updateById(captor.capture());
        assertEquals(ArchiveAgentSupport.SCHEDULE_STATUS_DISABLED, captor.getValue().getScheduleStatus());
    }

    @Test
    void runDueSchedulesCreatesPendingRunForReadyAgent() {
        LocalDateTime nextFireAt = LocalDateTime.now().minusMinutes(1);
        ArchiveAgentSchedule schedule = new ArchiveAgentSchedule();
        schedule.setId(1L);
        schedule.setTriggerId(2L);
        schedule.setAgentId(3L);
        schedule.setScheduleStatus(ArchiveAgentSupport.SCHEDULE_STATUS_IDLE);
        schedule.setNextFireAt(nextFireAt);

        ArchiveAgentTrigger trigger = new ArchiveAgentTrigger();
        trigger.setId(2L);
        trigger.setEnabled(1);
        trigger.setScheduleMode(ArchiveAgentSupport.SCHEDULE_MODE_INTERVAL);
        trigger.setIntervalMinutes(15);

        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(3L);
        definition.setOwnerUserId(9L);
        definition.setStatus(ArchiveAgentSupport.AGENT_STATUS_READY);

        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(4L);

        when(archiveAgentScheduleMapper.selectList(any())).thenReturn(List.of(schedule));
        when(archiveAgentTriggerMapper.selectById(2L)).thenReturn(trigger);
        when(archiveAgentDefinitionMapper.selectById(3L)).thenReturn(definition);
        when(archiveAgentVersionMapper.selectOne(any())).thenReturn(version);
        doAnswer(invocation -> {
            ArchiveAgentRun run = invocation.getArgument(0);
            run.setId(100L);
            return 1;
        }).when(archiveAgentRunMapper).insert(any(ArchiveAgentRun.class));

        support.runDueSchedules();

        verify(archiveAgentRunMapper).insert(any(ArchiveAgentRun.class));
        verify(triggerDispatcher).dispatch(any(ArchiveAgentRun.class));
        ArgumentCaptor<ArchiveAgentSchedule> scheduleCaptor = ArgumentCaptor.forClass(ArchiveAgentSchedule.class);
        verify(archiveAgentScheduleMapper).updateById(scheduleCaptor.capture());
        assertEquals(100L, scheduleCaptor.getValue().getLastRunId());
        assertEquals(ArchiveAgentSupport.SCHEDULE_STATUS_IDLE, scheduleCaptor.getValue().getScheduleStatus());
    }
}
