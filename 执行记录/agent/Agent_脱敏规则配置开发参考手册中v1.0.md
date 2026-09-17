# Agent 脱敏规则配置开发参考手册 v1.0

> 基线日期：2026-09-17  
> 代码根目录：`C:\Users\funsir\Desktop\报销系统`  
> 核验范围：`frontend/admin-web`、`backend/auth-service`、`backend/sql`  
> 信息口径：以当前代码、配置、SQL 和可重复测试为准，不把预留配置写成已完成功能。  
> 文件名中的“中”按指定路径保留，不影响本文版本定义。

## 一、结论与定位

当前脱敏模块已经形成可运行的基础闭环：

1. 管理员在“脱敏规则配置”页面维护敏感实体类型、正则表达式和占位符前缀。
2. 运行时优先读取当前租户、公司的数据库规则；数据库无规则时回退到 classpath 默认规则。
3. 普通文本通过正则替换为 `[PREFIX_001]` 形式的占位符。
4. 受控查询可以先按 JSON 字段确定性脱敏，再补充执行通用正则规则。
5. 占位符与原值的映射保存到数据库，并临时缓存在本机内存。
6. 模型只接收脱敏后的文本；模型返回后按 `mappingId` 在本地还原。
7. 过期映射可以由应用定时任务、管理接口或数据库事件清理。

模块目前适合 Agent 工作台模型节点和严格受控查询链使用，但尚未达到高安全等级的生产治理标准。最关键的缺口是：后端管理接口没有执行细粒度权限校验、映射原文以明文保存和返回、部分 Mapper 未带租户条件、后验泄漏检测尚未接入运行时。

## 二、状态定义

| 状态 | 含义 |
| --- | --- |
| 已完成 | 当前代码已实现，主链路有针对性测试 |
| 部分完成 | 功能可用，但安全、边界、测试或运维能力仍有限 |
| 预留 | 配置类、字段或接口含义存在，但运行时没有实际消费 |
| 已知缺陷 | 已从代码确认，本次仅记录、未修改 |

## 三、总体架构

```text
前端 /agent/desensitization
  |
  +-- 实体类型配置
  +-- 脱敏测试
  +-- 映射表管理
  |
  v
DesensitizationController
  |
  +-- EntityTypeConfigRepository
  |     +-- ea_desensitization_entity_type
  |
  +-- DesensitizationService
  |     +-- 普通文本正则脱敏
  |     +-- JSON 字段脱敏 + 正则补充
  |     +-- mappingId 还原
  |     +-- 本机 ConcurrentHashMap 缓存
  |
  +-- MappingTableRepository
        +-- ea_desensitization_mapping

运行时调用方
  |
  +-- ArchiveAgentModelRuntime
  |     +-- 通用工作台非 MOCK 模型节点
  |
  +-- AgentWorkflowRuntimeService
        +-- 受控查询：字段脱敏、正则脱敏、模型分析、本地还原
```

### 3.1 责任边界

| 模块 | 当前职责 | 不应承担的职责 |
| --- | --- | --- |
| 配置页面 | 维护规则、测试、查看映射、发起清理 | 在浏览器执行正式业务脱敏 |
| Controller | 暴露管理 API、基础请求校验 | 直接实现正则匹配与还原 |
| DesensitizationService | 脱敏、映射生成、保存、还原、清理 | 决定业务查询权限 |
| EntityTypeConfigRepository | 按租户/公司持久化实体规则 | 调用模型 |
| MappingTableRepository | 持久化占位符与原值映射 | 把映射发送到云端 |
| Agent 运行时 | 在模型调用前后编排脱敏和还原 | 修改规则定义 |

## 四、前端实现

### 4.1 路由与导航

页面路由：

- URL：`/agent/desensitization`
- 路由名：`agent-desensitization`
- 页面权限元数据：`agent:desensitization:view`

关键文件：

- `frontend/admin-web/src/router/route-catalog.ts`
- `frontend/admin-web/src/router/route-meta.ts`
- `frontend/admin-web/src/router/navigation-config.ts`

当前 `route-catalog.ts` 中相同路径和路由名被重复声明两次。这是已知前端配置缺陷，现有路由测试没有将其识别为失败。

### 4.2 页面组件

主组件：

`frontend/admin-web/src/views/agent/desensitization/DesensitizationConfigView.vue`

页面包含三个标签页。

#### 实体类型配置

- 展示编码、名称、描述、占位符前缀、正则表达式、示例和启用状态。
- 支持新增、编辑、删除和启停。
- 编辑已有规则时前端禁止修改编码。
- 正则和示例支持多行动态录入。

#### 脱敏测试

- 输入一段测试文本。
- 展示原文、脱敏文本、模拟 AI 响应、还原结果和完整映射关系。
- 前端发送的 `strategy` 固定为 `REGEX`。
- 该功能会创建真实映射记录，不是纯前端预览。

#### 映射表管理

- 分页查看映射 ID、会话 ID、映射数量、创建时间、过期时间和状态。
- 查看详情时展示占位符与原始值。
- 支持手工新增映射，前端 TTL 限制为 1 至 168 小时。
- 支持手动清理过期映射。
- 已定义统计接口，但当前页面没有使用统计接口展示汇总指标。

### 4.3 前端 API

文件：

`frontend/admin-web/src/api/agent/desensitization.ts`

统一前缀：

`/auth/agent/desensitization`

当前 API 参数和响应大量使用 `any`，缺少前端类型约束。页面也没有根据 `manage`、`test`、`mapping` 权限分别隐藏或禁用操作按钮。

## 五、后端实现

### 5.1 核心目录

`backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/`

| 类 | 职责 |
| --- | --- |
| `DesensitizationController` | 规则、测试、映射和清理 API |
| `DesensitizationService` | 文本脱敏、JSON 字段脱敏、还原和清理 |
| `DesensitizationConfig` | 绑定 `desensitization.*` 配置 |
| `DesensitizedData` | 脱敏结果对象 |
| `MappingTable` | 双向占位符映射与本地还原 |
| `MappingTableRepository` | 映射表持久化 |
| `DesensitizationMappingMapper` | 映射表 SQL |
| `DesensitizationMappingEntity` | `ea_desensitization_mapping` 实体 |
| `EntityTypeConfigRepository` | 实体规则持久化与 DTO 转换 |
| `EntityTypeConfigMapper` | 实体规则 SQL |
| `EntityTypeConfigEntity` | `ea_desensitization_entity_type` 实体 |
| `DesensitizationDTO` | 管理 API 请求与响应对象 |
| `DesensitizationCleanupTask` | 每小时清理过期映射 |

### 5.2 配置加载

默认配置文件：

`backend/auth-service/src/main/resources/desensitization-rules.yml`

通过以下配置导入：

`backend/auth-service/src/main/resources/application.yml`

```yaml
spring:
  config:
    import:
      - optional:classpath:desensitization-rules.yml
```

运行时规则优先级：

1. 查询当前租户、当前公司的 `ea_desensitization_entity_type`。
2. 只要数据库返回至少一条记录，就以数据库结果为唯一来源，并过滤其中已停用规则。
3. 数据库返回空列表或查询异常时，回退到 YAML 默认规则。

该行为有两个重要后果：

- 数据库只配置一种自定义规则时，其他 YAML 默认规则不会自动合并。
- 删除数据库全部规则后会重新启用 YAML 默认规则；如果需要“完全不启用任何规则”，删除全部记录不能达到目的，应保留记录并全部停用。

### 5.3 默认实体类型

classpath 当前定义八类默认实体：

| 编码 | 占位符前缀 | 识别目标 |
| --- | --- | --- |
| `PERSON` | `PERSON` | 人名 |
| `AMOUNT` | `AMOUNT` | 金额 |
| `DOCUMENT_ID` | `DOC` | 报销单号、发票号等 |
| `PHONE` | `PHONE` | 手机号、部分固定电话 |
| `ID_CARD` | `ID` | 身份证号 |
| `BANK_ACCOUNT` | `BANK` | 16 至 19 位银行账号 |
| `EMAIL` | `EMAIL` | 邮箱 |
| `ADDRESS` | `ADDR` | 省市形式的地址 |

人名默认规则原本是宽泛的 2 至 4 个汉字。运行时会识别这条历史规则并收紧为“姓名后接特定业务语义词”的形式，避免将“提交了报销单”等普通短语整体识别成人名。页面显示和数据库中保存的仍可能是原始宽泛规则。

## 六、脱敏与还原算法

### 6.1 普通文本脱敏

入口：

```java
desensitize(sessionId, text)
desensitize(sessionId, text, requestedStrategy)
```

流程：

1. 创建 UUID 形式的 `mappingId`。
2. 创建有效期，默认 TTL 为 24 小时。
3. 按实体规则顺序逐个编译和执行正则。
4. 首次出现的原值分配占位符，例如 `PERSON_001`。
5. 文本中替换为带方括号形式：`[PERSON_001]`。
6. 同一原值在同次调用中复用同一占位符。
7. 将映射写入数据库和本机缓存。
8. 返回脱敏文本、`mappingId`、策略和映射。

当前 `requestedStrategy` 仅为接口兼容保留。无论传入 `REGEX` 还是其他值，实际都执行正则规则，返回策略为 `REGEX`。

无效正则不会让整个调用失败，而是记录警告并跳过该表达式。这有利于可用性，但可能造成敏感字段漏脱敏。

### 6.2 结构化 JSON 脱敏

入口：

```java
desensitizeJsonFields(sessionId, json, fieldPlaceholderPrefixes)
desensitizeJsonFields(sessionId, json, fieldPlaceholderPrefixes, propagatedPrefixes)
```

流程：

1. 使用 Jackson 解析 JSON。
2. 递归遍历对象和数组。
3. 对字段名命中白名单的值执行整体替换。
4. 可将某些字段的映射传播到同一 JSON 中的普通文本，例如把问题文本中的查询关键词一并替换。
5. 序列化字段脱敏后的 JSON。
6. 再执行数据库/YAML 正则规则，补充识别未显式配置的敏感文本。
7. 保存一份统一映射表。

受控报销查询当前会对关键词、单据 ID、单据编号、标题、金额等字段做确定性脱敏，同时保留 `evidenceRef` 和必要状态字段用于证据校验。

### 6.3 还原

入口：

```java
restore(mappingId, maskedText)
```

流程：

1. 先从本机 `ConcurrentHashMap` 查找映射。
2. 缓存未命中时按 `mappingId` 查询数据库。
3. 映射不存在或已过期时原样返回，不执行还原。
4. 使用 `\[(\w+_\d+)\]` 识别占位符并替换为原值。

还原是基于本地映射的确定性替换，不是由云端模型推断原值。模型如果删除、改写或不返回占位符，对应信息就不会被还原。

### 6.4 映射生命周期

- 默认 TTL：24 小时。
- 页面手工新增时限制为 1 至 168 小时。
- 后端手工新增接口只限制最小值 1 小时，没有最大值。
- 应用定时任务默认每小时执行一次。
- 管理接口可以立即触发清理。
- SQL 迁移还会创建 MySQL Event，每小时清理一次。

应用任务和数据库事件同时存在，形成重复清理机制。删除语句具有结果幂等性，但部署职责不清晰，建议生产环境明确只保留一种主清理机制。

## 七、关键接口

Controller：

`backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationController.java`

| 方法 | 路径 | 用途 | 当前行为 |
| --- | --- | --- | --- |
| GET | `/entity-types` | 查询当前租户/公司规则 | 返回全部启用和停用规则 |
| POST | `/entity-types` | 新增或更新规则 | 按编码保存 |
| DELETE | `/entity-types/{code}` | 删除规则 | 按租户和编码删除 |
| PUT | `/entity-types/{code}/enabled?enabled=` | 启停规则 | 按租户和编码更新 |
| POST | `/test` | 测试脱敏与还原 | 创建真实映射并返回原文、映射和结果 |
| GET | `/mappings` | 分页查询映射 | pageSize 后端限制 1 至 100 |
| POST | `/mappings` | 手工创建映射 | 原值会写入数据库 |
| GET | `/mappings/stats` | 映射统计 | 返回当前租户总数、有效数、过期数 |
| POST | `/mappings/cleanup` | 清理过期映射 | 清理本机缓存和数据库全部过期记录 |

### 7.1 设计权限

SQL 中定义：

- `agent:desensitization:view`
- `agent:desensitization:manage`
- `agent:desensitization:test`
- `agent:desensitization:mapping`

权限脚本：

`backend/sql/init_desensitization_permissions.sql`

### 7.2 实际权限状态

当前只有页面路由元数据使用 `agent:desensitization:view`。Controller 没有注入 `AccessControlService`，九个接口均没有执行上述细粒度权限校验；页面按钮也没有分别按 `manage`、`test`、`mapping` 权限控制。

因此，这四个权限目前是“已建模、未完整执行”。接口通常仍受系统统一登录认证保护，但任何能够调用这些地址的已登录用户是否可操作，取决于外围拦截器，不能依赖本模块自身保证。

## 八、运行时接入

### 8.1 Agent 工作台通用模型节点

文件：

`backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentModelRuntime.java`

非 `MOCK` Provider 的执行顺序：

1. 将整个运行上下文序列化为 JSON 文本。
2. 调用普通正则脱敏。
3. 脱敏失败或没有 `mappingId` 时终止模型调用。
4. 将脱敏文本发送给模型。
5. 模型返回后按 `mappingId` 还原。
6. 输出包含 provider、model、content、mappingId 和 latencyMs。

`MOCK` Provider 直接调用 Mock Adapter，不走此脱敏服务。

### 8.2 严格受控查询

文件：

`backend/auth-service/src/main/java/com/finex/auth/agent/workflow/AgentWorkflowRuntimeService.java`

执行顺序：

```text
受控工具查询
  -> 构造结构化上下文
  -> 已知字段确定性脱敏
  -> 通用正则补充脱敏
  -> 保存 mappingId
  -> 脱敏 JSON 发送给模型
  -> 本地还原
  -> evidenceRef 白名单校验
  -> 输出结果
```

运行步骤中会记录脱敏后的 payload 和 `mappingId`，不会主动记录映射原文；映射原文保存在脱敏映射表中。

### 8.3 当前未发现直接接入的位置

代码检索显示，除管理 Controller 外，业务运行时只有上述两个类直接调用 `DesensitizationService`。其他 FinWork 普通对话、通知或非 Agent 模型链路不能默认认为已经经过本模块。

## 九、数据库与 SQL

### 9.1 实体类型表

`ea_desensitization_entity_type`

主要字段：

- `tenant_id`
- `company_id`
- `code`
- `name`
- `description`
- `placeholder_prefix`
- `regex_patterns`
- `examples`
- `enabled`
- 创建与更新时间

唯一键为 `tenant_id + company_id + code`。

### 9.2 映射表

`ea_desensitization_mapping`

主要字段：

- `tenant_id`
- `session_id`
- `mapping_id`
- `mappings`
- `created_at`
- `expires_at`

`mappings` 保存“占位符 -> 原始值”JSON，目前没有应用层加密或字段级加密。

### 9.3 相关脚本

| 脚本 | 用途 | 当前注意事项 |
| --- | --- | --- |
| `backend/sql/migrate_desensitization.sql` | 创建规则表、映射表、默认数据和数据库事件 | 默认规则 INSERT 非幂等；重复执行可能触发唯一键冲突 |
| `backend/sql/ea_agent_platform_init.sql` | Agent 平台基础表，含映射表 | 不包含实体规则表 |
| `backend/sql/init_desensitization_permissions.sql` | 创建权限并授予超级管理员 | 权限已建模，但 Controller 未执行 |

`migrate_desensitization.sql` 将 `mappings` 定义为 `TEXT`，`ea_agent_platform_init.sql` 定义为 `JSON`。当前 Java 实体按 String 读写，两种 MySQL 类型均可承载，但不同环境可能出现结构漂移。

数据库 Event 使用 `DROP EVENT IF EXISTS` 后再创建，需要数据库 Event 权限，并依赖 MySQL Event Scheduler 已启用。该段脚本本身不满足“无副作用重复执行”的严格幂等要求。

## 十、配置项真实状态

| 配置 | 状态 | 实际说明 |
| --- | --- | --- |
| `desensitization.entityTypes` | 已完成 | 数据库为空/异常时作为默认规则 |
| `desensitization.mapping.ttlHours` | 已完成 | 新映射默认有效期 |
| `desensitization.mapping.cleanupCron` | 已完成 | 应用定时任务 Cron |
| `desensitization.strategies` | 预留 | 当前服务不读取优先级或切换策略 |
| `desensitization.postValidation` | 预留 | 当前没有执行泄漏检测或阻断 |
| `desensitization.mapping.autoCleanup` | 预留 | 定时任务没有根据该值启停 |
| API `strategy` 参数 | 兼容字段 | 当前始终执行 REGEX |

## 十一、开发进度

| 能力 | 状态 | 备注 |
| --- | --- | --- |
| 页面路由和导航 | 已完成 | 存在重复路由声明 |
| 实体规则列表、新增、编辑、删除、启停 | 已完成 | 缺少前后端细粒度权限 |
| 正则脱敏测试与还原演示 | 已完成 | 返回并展示原始映射 |
| 映射分页、详情、手工新增、清理 | 已完成 | 原值明文可见 |
| 数据库规则优先、YAML 回退 | 已完成 | 不执行合并 |
| 普通文本正则脱敏 | 已完成 | 无效正则仅跳过 |
| 结构化 JSON 字段脱敏 | 已完成 | 受控查询使用 |
| mappingId 持久化和本地还原 | 已完成 | DB + 本机缓存 |
| Agent 工作台非 MOCK 模型接入 | 已完成 | 整体上下文走正则 |
| 受控报销查询接入 | 已完成 | 字段脱敏 + 正则 + 证据校验 |
| 应用定时清理 | 已完成 | 不读取 autoCleanup |
| 数据库 Event 清理 | 部分完成 | 依赖 DB 权限和 Event Scheduler |
| 后端细粒度权限 | 未完成 | 权限码存在但 Controller 未校验 |
| 映射原文加密 | 未完成 | 数据库当前明文 JSON/TEXT |
| 后验泄漏检测 | 未完成 | 配置存在但未执行 |
| 多策略路由 | 未完成 | 仅正则策略 |
| Redis/集中缓存 | 未完成 | 当前为单机内存缓存，DB 兜底 |
| 管理操作审计 | 未完成 | 没有规则修改和映射查看审计闭环 |
| 前端强类型 DTO | 未完成 | 当前 API 使用 any |
| 页面组件测试 | 未完成 | 未发现该页面专用前端测试 |
| Controller/Repository 集成测试 | 未完成 | 当前只有核心 Service 测试 |

## 十二、验证结果

验证日期：2026-09-17

### 12.1 后端

执行：

- `DesensitizationServiceTest`
- `ArchiveAgentModelRuntimeTest`
- `AgentWorkflowRuntimeServiceTest`

结果：

- 测试类：3 个
- 测试项：7 个
- 7 通过，0 失败，0 错误，0 跳过

已覆盖：

- 宽泛人名规则的运行时收紧。
- requestedStrategy 不改变 REGEX 实际策略。
- 普通文本占位符生成。
- 结构化 JSON 敏感字段脱敏。
- evidenceRef 保留。
- 模型节点脱敏和还原。
- 受控查询链的脱敏接入。

其中一项测试明确证明：还原后的文本即使包含新的手机号，当前 `postValidation` 也不会阻断。

### 12.2 前端

执行路由和导航测试：

- 测试文件：2 个
- 测试项：17 个
- 17 项全部通过

当前未覆盖：

- 页面规则 CRUD。
- 正则测试交互。
- 映射详情和手工映射。
- 不同权限用户的按钮和接口行为。
- 浏览器端到端脱敏、模型和还原闭环。

## 十三、风险与技术债

### 13.1 高优先级

1. **后端没有执行设计权限**  
   Controller 未调用 `AccessControlService`。规则修改、脱敏测试、映射原文查看、手工新增和全局过期清理都缺少模块内授权。

2. **映射原文以明文保存并通过 API 返回**  
   `mappings` 包含姓名、手机号、证件号、账号等原文。数据库泄露或映射管理权限失控会直接暴露敏感数据。测试响应也返回完整映射。

3. **按 mappingId 查询与还原缺少租户条件**  
   `findByMappingId` 只按全局 UUID 查询，`findBySessionId` 也没有租户条件；`restore` 不校验当前租户。UUID 难以猜测，但这不是可靠的租户隔离措施。

4. **实体规则更新/删除的公司隔离不完整**  
   列表按 tenant + company 查询，唯一键也是 tenant + company + code；但 `findByCode`、启停和删除只按 tenant + code。一个租户包含多个公司且复用相同 code 时，可能读取或修改错误公司的规则。

5. **未实现模型输出后验泄漏检测**  
   YAML 定义了手机号、身份证、银行卡泄漏规则，但当前还原后不会检测模型新增的敏感信息，也不会阻断输出。

### 13.2 中优先级

1. **用户可配置任意正则，缺少安全校验**  
   保存时不预编译、不检测复杂度，也没有正则超时。错误正则会被静默跳过，恶意或灾难性回溯表达式可能造成 CPU 风险。

2. **静态规则与数据库规则的切换语义容易误解**  
   两者不是合并关系。删除全部数据库规则会恢复 YAML 默认规则，新增一条数据库规则则会屏蔽其他默认规则。

3. **清理机制重复且开关无效**  
   应用任务和 MySQL Event 都会清理；`autoCleanup=false` 不能关闭应用任务。

4. **单机缓存没有容量上限**  
   映射在过期前持续进入 `ConcurrentHashMap`，没有最大条数或基于大小的淘汰策略。多实例间缓存不共享，但数据库可以兜底读取。

5. **手工映射接口后端没有 TTL 上限**  
   前端最多 168 小时，但直接调用 API 可以提交更大的值。

6. **序列化异常处理不透明**  
   映射实体转换使用新建 ObjectMapper，并吞掉序列化/反序列化异常；可能形成空映射或数据库写入失败，日志不足。

7. **管理操作缺少审计**  
   没有可靠记录谁查看了原值、修改了规则、创建了手工映射或执行了清理。

### 13.3 已知缺陷

1. 前端路由目录重复声明 `agent-desensitization`。
2. `migrate_desensitization.sql` 默认数据插入不是幂等的。
3. 两份建表脚本对 `mappings` 使用不同列类型。
4. 空字符串通过后端测试接口时可能得到空 `mappingId`，随后还原阶段存在异常风险；前端目前通过非空检查规避。
5. `DesensitizationDTO.MappingTableStats` 与 Controller 实际返回的 Map 字段没有统一使用。
6. `findBySessionId`、`findAllEnabled` 和前端统计 API 等部分能力已定义但主页面或主链路未使用。

以上问题在本次文档任务中未修改代码。

## 十四、关键文件索引

### 14.1 前端

| 用途 | 路径 |
| --- | --- |
| 页面 | `frontend/admin-web/src/views/agent/desensitization/DesensitizationConfigView.vue` |
| API | `frontend/admin-web/src/api/agent/desensitization.ts` |
| 路由目录 | `frontend/admin-web/src/router/route-catalog.ts` |
| 路由元数据 | `frontend/admin-web/src/router/route-meta.ts` |
| 导航配置 | `frontend/admin-web/src/router/navigation-config.ts` |

### 14.2 后端

| 用途 | 路径 |
| --- | --- |
| 管理 Controller | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationController.java` |
| 核心服务 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationService.java` |
| 配置绑定 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationConfig.java` |
| 脱敏结果 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizedData.java` |
| 映射模型 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/MappingTable.java` |
| 映射仓储 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/MappingTableRepository.java` |
| 映射 Mapper | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationMappingMapper.java` |
| 规则仓储 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/EntityTypeConfigRepository.java` |
| 规则 Mapper | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/EntityTypeConfigMapper.java` |
| DTO | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationDTO.java` |
| 定时清理 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationCleanupTask.java` |
| 默认规则 | `backend/auth-service/src/main/resources/desensitization-rules.yml` |
| 工作台模型接入 | `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentModelRuntime.java` |
| 受控查询接入 | `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/AgentWorkflowRuntimeService.java` |
| 核心服务测试 | `backend/auth-service/src/test/java/com/finex/auth/agent/desensitization/DesensitizationServiceTest.java` |

### 14.3 SQL

| 用途 | 路径 |
| --- | --- |
| 脱敏表和默认数据迁移 | `backend/sql/migrate_desensitization.sql` |
| Agent 平台基础映射表 | `backend/sql/ea_agent_platform_init.sql` |
| 脱敏权限 | `backend/sql/init_desensitization_permissions.sql` |

## 十五、排障指南

### 15.1 页面无法访问

1. 检查用户是否有 `agent:desensitization:view`。
2. 检查权限缓存和导航配置。
3. 检查重复路由是否产生警告或匹配异常。
4. 检查后端统一认证是否允许访问 `/auth/agent/desensitization/**`。

### 15.2 页面规则与实际脱敏不一致

1. 确认当前 TenantContext 和 CompanyContext。
2. 查询数据库是否至少存在一条当前公司规则。
3. 有数据库规则时不会合并 YAML 默认规则。
4. 检查规则是否启用。
5. 人名宽泛规则会在运行时被特殊收紧。
6. 检查正则是否编译失败并被跳过。

### 15.3 脱敏后仍出现原值

1. 确认敏感内容是否命中字段白名单或正则。
2. 确认规则来源是数据库还是 YAML。
3. 检查匹配顺序和规则重叠。
4. 检查 Agent 是否走非 MOCK 工作台模型路径或严格受控查询路径。
5. 其他模型调用链不能默认认为已接入脱敏。
6. 当前没有后验泄漏检测，模型新增敏感文本不会被自动阻断。

### 15.4 无法还原

1. 检查返回内容是否仍保留 `[PREFIX_001]` 格式。
2. 检查 `mappingId` 是否正确。
3. 检查映射是否已经过期或被清理。
4. 检查数据库中是否存在对应映射。
5. 检查当前实例日志是否显示映射反序列化失败。
6. 模型改写占位符后无法还原属于预期限制。

### 15.5 映射数量持续增长

1. 检查应用调度是否启用。
2. 检查 `cleanupCron`。
3. 检查 MySQL Event Scheduler 是否启用。
4. 检查数据库时区与应用时间。
5. 手工调用 cleanup 接口前应先补齐并验证 mapping 权限。

## 十六、安全整改顺序

建议按以下顺序演进：

1. 为所有 Controller 接口接入 `AccessControlService`，并按 view/manage/test/mapping 分权。
2. 修复所有 Mapper 的 tenant + company 条件，restore 时验证映射归属。
3. 映射原文采用应用层信封加密或字段级加密，密钥由专用密钥管理服务托管。
4. 映射列表默认不返回原文；查看原文使用单独高权限接口、二次确认和审计。
5. 实现模型输出后验泄漏检测，高风险命中时阻断或降级。
6. 保存规则时预编译正则，限制长度、复杂度和执行时间。
7. 统一应用任务与数据库 Event，真正支持 `autoCleanup`。
8. 为缓存增加最大容量和安全淘汰策略，必要时迁移到带 TTL 的集中缓存。
9. 合并重复路由，补齐前端 DTO 和权限按钮。
10. 补充 Controller、Repository、租户隔离、权限和浏览器端到端测试。
11. 重写迁移脚本为严格幂等版本，统一 `mappings` 列类型。

## 十七、扩展与维护规则

新增实体规则时：

1. 使用稳定且大写的实体编码和占位符前缀。
2. 正则必须覆盖正常、边界和反例，不允许只验证“能命中”。
3. 特别检查中文宽泛匹配、长文本性能和灾难性回溯。
4. 明确该规则属于数据库配置还是 classpath 默认配置。
5. 使用测试接口验证脱敏和还原，但不要使用真实生产敏感数据。
6. 增加 `DesensitizationServiceTest` 用例。

修改本模块时：

- 不得把 `mappingId` 当作权限凭证。
- 不得把原始映射发送到云端模型。
- 不得在日志中记录完整原文映射。
- 不得因脱敏失败而静默把原文发送给模型。
- SQL 变更必须幂等，并注明数据库版本、Event 权限和回滚方式。
- 文档中的“已完成”必须同时有代码路径和验证依据。
- 新接入模型调用链时，必须明确调用前脱敏、调用后还原、失败策略和后验检测状态。
