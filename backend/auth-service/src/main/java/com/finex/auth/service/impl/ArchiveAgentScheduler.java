// 业务域：档案代理与归档任务
// 文件角色：定时调度类
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl;

import com.finex.auth.service.ArchiveAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ArchiveAgentScheduler：定时调度类。
 * 负责按触发时机执行 档案代理相关后台任务。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveAgentScheduler {

    private final ArchiveAgentService archiveAgentService;

    /**
     * 处理档案代理中的这一步。
     */
    @Scheduled(fixedDelay = 60000L)
    public void dispatchDueSchedules() {
        try {
            archiveAgentService.runDueSchedules();
        } catch (Exception ex) {
            log.warn("Agent ????????: {}", ex.getMessage());
        }
    }
}
