# 添加脱敏功能到前端系统 - 实施计划

## 问题分析

脱敏功能的后端实现已完成，包括：
- ✅ 后端 Controller、Service、DTO 等代码
- ✅ 前端 Vue 组件 (`DesensitizationConfigView.vue`)
- ✅ 前端 API 文件 (`desensitization.ts`)
- ✅ 数据库迁移脚本 (`migrate_desensitization.sql`)

但缺少以下关键配置：
- ❌ 路由未注册（`route-catalog.ts`）
- ❌ 路由元信息未定义（`route-meta.ts`）
- ❌ 导航菜单未添加（`navigation-config.ts`）
- ❌ 权限码未在数据库中初始化
- ❌ 数据库迁移可能未执行

## 实施步骤

### 1. 数据库层面
**目标**：确保数据库表和权限码已正确初始化

#### 1.1 执行数据库迁移
- 检查 `ea_desensitization_entity_type` 和 `ea_desensitization_mapping` 表是否存在
- 如果不存在，执行 `backend/sql/migrate_desensitization.sql`

#### 1.2 添加权限码
创建权限初始化脚本 `backend/sql/init_desensitization_permissions.sql`：
```sql
-- 脱敏管理权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_code, sort_order, status)
VALUES 
('agent:desensitization:view', '脱敏配置-查看', 'PAGE', 'agent:menu', 600, 1),
('agent:desensitization:manage', '脱敏配置-管理', 'BUTTON', 'agent:desensitization:view', 601, 1);

-- 授予超级管理员角色这些权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission 
WHERE permission_code IN ('agent:desensitization:view', 'agent:desensitization:manage');
```

### 2. 前端路由配置
**目标**：注册脱敏页面路由

#### 2.1 在 `route-catalog.ts` 添加路由
在 Agent 相关路由区域（约第443行后）添加：
```typescript
{
  path: 'agent/desensitization',
  name: 'agent-desensitization',
  component: () => import('../views/agent/desensitization/DesensitizationConfigView.vue'),
  metaKey: 'agent-desensitization'
}
```

#### 2.2 在 `route-meta.ts` 添加元信息
在 `ROUTE_META_REGISTRY` 对象中（约第621行后）添加：
```typescript
'agent-desensitization': routeMeta({
  title: '脱敏规则配置',
  tabTitle: '脱敏规则配置',
  menuTitle: '脱敏规则配置',
  description: '配置敏感数据的识别规则和脱敏策略',
  permissionCode: 'agent:desensitization:view'
})
```

### 3. 前端导航菜单
**目标**：在 Agent 菜单组中添加脱敏配置入口

#### 3.1 在 `navigation-config.ts` 更新 Agent 菜单
修改 Agent 菜单组（第193-204行），添加脱敏配置项：
```typescript
createMenuGroup(
  '/agent',
  'Agent',
  [
    createRouteMenuNode('agent-chat'),
    createRouteMenuNode('agent-workbench'),
    createRouteMenuNode('agent-desensitization'),  // 新增
    createRouteMenuNode('agent-personal-avatar'),
    createRouteMenuNode('agent-digital-employee'),
    createRouteMenuNode('agent-business-ontology'),
    createRouteMenuNode('agent-knowledge-base')
  ],
  'Agent'
)
```

### 4. 验证和测试
- 重启后端服务（确保权限码加载）
- 重启前端服务（确保路由配置生效）
- 以超级管理员身份登录
- 检查 Agent 菜单中是否出现"脱敏规则配置"
- 点击进入，验证页面功能：
  - 实体类型配置
  - 脱敏测试
  - 映射表管理

## 文件清单

### 需要创建的文件
1. `backend/sql/init_desensitization_permissions.sql` - 权限初始化脚本

### 需要修改的文件
1. `frontend/admin-web/src/router/route-catalog.ts` - 添加路由
2. `frontend/admin-web/src/router/route-meta.ts` - 添加路由元信息
3. `frontend/admin-web/src/router/navigation-config.ts` - 添加菜单项

## 风险评估

- **低风险**：这是纯增量操作，不会影响现有功能
- **回滚简单**：如果出问题，移除新增的路由和菜单配置即可
- **权限隔离**：只有被授予权限的用户才能访问

## 成功标准

- [ ] 数据库表已创建
- [ ] 权限码已初始化并授予管理员
- [ ] Agent 菜单中显示"脱敏规则配置"入口
- [ ] 点击可正常进入脱敏配置页面
- [ ] 实体类型配置功能正常
- [ ] 脱敏测试功能正常
- [ ] 映射表管理功能正常
- [ ] 系统设置中权限配置正常显示
