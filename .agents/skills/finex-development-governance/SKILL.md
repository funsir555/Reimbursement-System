---
name: finex-development-governance
description: Govern FinEx feature development, refactoring, architecture review, and release preparation against this repository's domain boundaries and delivery gates. Use for work on the FinEx reimbursement and finance system; do not apply to unrelated projects or general questions.
---

# FinEx 开发治理

在 FinEx 的功能开发、重构、架构审查或发布准备中使用本 skill。按任务风险调整检查深度；小范围修复不需要套用完整发布流程。用户明确的需求和授权范围优先。

## 确认依据

- 先读仓库根目录 `docs/architecture/系统最终目标与开发架构规范.md` 中与任务有关的章节；它是项目最高产品架构与开发约束。
- 按需参考 `当前实现架构图与演进路线图.md`、`2026-09-17_系统重复建设与架构边界审查报告.md`、相关领域规范和近期执行记录。它们用于了解演进方法与历史，不覆盖最高规范。
- 用当前代码、配置、测试和 `.github/workflows/p0-quality-gate.yml` 核对实现状态。发现文档与事实冲突时说明差异，区分“当前事实”“目标架构”和“尚待验证”；不要复制过期的测试数量、覆盖率或热点顺位。

## 设计与改动

1. 明确本次业务行为、验收条件、所属领域及业务真相的 owner；识别受影响的 HTTP API、权限码、数据库结构、事务和用户流程。对重大边界变化记录 ADR 或等价决策。
2. 沿用单部署的模块化单体路线。跨域能力通过现有应用 Service 或必要的窄端口调用；Controller 和 Agent Tool 不越过应用边界直连其他领域 Mapper。Process 审批工作流与 Agent 工作流保留各自语义。
3. 新逻辑进入明确的 owner；已冻结的大 Service、Support 和前端页面保持薄入口。按可验证切片迁移，避免仅为缩行数制造无职责的包装层。对外 API、权限和数据库契约默认兼容；确需改变时明确迁移与回滚方案。
4. 不凭技术偏好引入微服务、缓存、消息队列或新基础设施。先核对规范中的触发条件、实际测量和决策记录。

## 验证与交付

- 先为受影响的行为安排定向测试，再按架构规范的“测试与交付门禁”执行相应后端、前端及仓库检查。合并和发布前以现行 CI 门禁为准；未运行或受环境限制的检查必须如实报告，不推断为通过。
- 认证、公司上下文、Agent 查询、审批、付款、凭证及抽取遗留 Support 等高风险改动，按规范中的专项验收场景验证权限、隔离、事务、重试和兼容性。测试通过不自动证明真实 MySQL、银行或生产环境已经验证。
- 发布相关变更需核对数据库脚本的幂等性、备份与回滚，列出生产配置变化，保护凭据；重大边界变更保留决策记录。性能和可用性结论须基于注明环境与负载的实测证据。
- 交付说明简要列出：依据的规范、领域 owner 与契约影响、执行的验证及结果、尚未验证的风险和发布前待办。
