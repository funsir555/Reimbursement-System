# FinEx 财务报销系统前端

当前 `frontend/admin-web` 已不是演示型原型，而是和当前后端单体主服务协同演进的管理台前端。

## 当前状态

- 当前验证基线：`npm run test:unit` 通过，`211/211`
- 当前角色：稳定消费 `backend/auth-service` 已形成的多领域接口
- 当前阶段：不重开前端治理主批次，默认跟随后端 `backend residual hotspot second-wave`

前端当前服务的主要业务域包括：

- 登录与当前用户
- 首页概览
- 报销运行态
- 流程管理
- 财务管理 / 电子档案 / 固定资产
- 系统设置
- Archive Agent

## 技术栈

| 技术 | 版本 | 说明 |
|---|---|---|
| Vue | 3.x | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Element Plus | 2.x | UI 组件 |
| Vite | 5.x | 构建工具 |
| Vue Router | 4.x | 路由 |
| Pinia | 2.x | 状态管理 |
| Vitest | 当前仓库版本 | 单元测试 |

## 启动与验证

安装依赖：

```bash
cd frontend/admin-web
npm install
```

本地启动：

```bash
npm run dev
```

单测：

```bash
npm run test:unit
```

构建：

```bash
npm run build
```

## 当前约束

- `src/api/index.ts` 继续保留兼容导出层
- 新的 API 契约和类型 owner 继续放到 `src/api/modules/` 分域文件
- 当前不把前端 residual 与 backend residual 混做；若未来重开前端 residual，应单独立批
- 当前进度与优先级以 `C:\Users\funsir\Desktop\报销系统\执行记录\报销系统治理落地方案.md` 为准

## 相关文档

- `frontend/admin-web/项目说明.md`
- `frontend/admin-web/src/api/modules/README.md`
- `C:\Users\funsir\Desktop\报销系统\README.md`
- `C:\Users\funsir\Desktop\报销系统\docs\architecture\项目启动与初始化说明.md`
