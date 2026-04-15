// 业务域：系统设置
// 文件角色：定时调度类
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

package com.finex.auth.service.impl;

import com.finex.auth.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SystemSettingsSyncScheduler：定时调度类。
 * 负责按触发时机执行 系统系统设置同步相关后台任务。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemSettingsSyncScheduler {

    private final SystemSettingsService systemSettingsService;

    /**
     * 执行Due同步Jobs。
     */
    @Scheduled(fixedDelay = 60000L)
    public void runDueSyncJobs() {
        try {
            systemSettingsService.runDueSyncJobs();
        } catch (Exception ex) {
            log.warn("系统设置定时同步执行失败: {}", ex.getMessage());
        }
    }
}
