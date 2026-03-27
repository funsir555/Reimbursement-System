package com.finex.auth.service.impl;

import com.finex.auth.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemSettingsSyncScheduler {

    private final SystemSettingsService systemSettingsService;

    @Scheduled(fixedDelay = 60000L)
    public void runDueSyncJobs() {
        try {
            systemSettingsService.runDueSyncJobs();
        } catch (Exception ex) {
            log.warn("系统设置定时同步执行失败: {}", ex.getMessage());
        }
    }
}
