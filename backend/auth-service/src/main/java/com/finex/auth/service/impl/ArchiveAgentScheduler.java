package com.finex.auth.service.impl;

import com.finex.auth.service.ArchiveAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveAgentScheduler {

    private final ArchiveAgentService archiveAgentService;

    @Scheduled(fixedDelay = 60000L)
    public void dispatchDueSchedules() {
        try {
            archiveAgentService.runDueSchedules();
        } catch (Exception ex) {
            log.warn("Agent ????????: {}", ex.getMessage());
        }
    }
}
