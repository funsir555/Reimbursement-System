// 业务域：异步任务
// 文件角色：service 接口
// 上下游关系：上游通常来自 异步提交、查询和通知相关接口，下游会继续协调 任务记录、状态更新和通知消息。
// 风险提醒：改坏后最容易影响 任务重复提交、异步状态不准确和结果回传。

package com.finex.auth.service;

/**
 * NotificationService：service 接口。
 * 定义通知这块对外提供的业务入口能力。
 * 改这里时，要特别关注 任务重复提交、异步状态不准确和结果回传是否会被一起带坏。
 */
public interface NotificationService {

    void sendAsyncNotification(Long userId, String type, String title, String content, String relatedTaskNo);
}
