// 业务域：报销单录入、流转与查询
// 文件角色：轻量数据载体
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

/**
 * LeaderResolution：轻量数据载体。
 * 用轻量结构承载 上级Resolution在调用链里要传递的关键数据。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
record LeaderResolution(Long departmentId, Long userId) {
}
