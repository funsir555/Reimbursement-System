package com.finex.auth.agent.desensitization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 脱敏映射表定时清理任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesensitizationCleanupTask {

    private final DesensitizationService desensitizationService;

    /**
     * 每小时清理过期映射表
     */
    @Scheduled(cron = "${desensitization.mapping.cleanupCron:0 0 * * * ?}")
    public void cleanupExpiredMappings() {
        log.info("执行定时清理过期映射表任务");
        try {
            desensitizationService.cleanupExpiredMappings();
        } catch (Exception e) {
            log.error("清理过期映射表失败", e);
        }
    }
}
