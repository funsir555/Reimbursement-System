<template>
  <div class="agent-workbench">
    <aside class="panel sidebar">
      <section class="hero-card">
        <div class="eyebrow">PERSONAL AGENT LAB</div>
        <div class="hero-main">
          <div class="hero-icon"><pixel-duck-bot-icon class="h-16 w-16" /></div>
          <div>
            <h1>Agent 工作台</h1>
            <p>把 Prompt、模型、工具和流程编排放进同一个个人自动化实验台。</p>
          </div>
        </div>
        <div class="hero-stats">
          <div><span>我的 Agent</span><strong>{{ filteredAgents.length }}</strong></div>
          <div><span>运行中</span><strong>{{ runningAgentCount }}</strong></div>
          <div><span>异常</span><strong>{{ failedAgentCount }}</strong></div>
        </div>
      </section>

      <section class="panel-section">
        <div class="row between wrap">
          <el-button type="primary" :icon="Plus" :disabled="!canCreate" @click="startCreateAgent">新建 Agent</el-button>
          <el-button :icon="RefreshRight" @click="loadAgents(false)">刷新</el-button>
        </div>
        <div class="stack gap-sm mt-md">
          <el-input v-model="filters.keyword" clearable placeholder="搜索 Agent" @keyup.enter="loadAgents()">
            <template #append><el-button :icon="Search" @click="loadAgents()" /></template>
          </el-input>
          <el-select v-model="filters.status" clearable placeholder="状态筛选" @change="loadAgents()">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.tag" clearable filterable placeholder="标签筛选">
            <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </div>

        <div v-loading="listLoading" class="agent-list">
          <button
            v-for="agent in filteredAgents"
            :key="agent.id"
            type="button"
            class="agent-card"
            :class="{ active: selectedAgentId === agent.id }"
            @click="selectAgent(agent.id)"
          >
            <div class="row gap-sm start">
              <div class="duck-badge" :style="{ '--cover': agent.coverColor || '#d97706' }">
                <pixel-duck-bot-icon class="h-6 w-6" />
              </div>
              <div class="flex-1 min-w-0 text-left">
                <strong class="truncate block">{{ agent.agentName }}</strong>
                <div class="muted xs truncate">{{ agent.agentCode }}</div>
              </div>
              <el-tag size="small" :type="summaryStatusType(agent.runtimeStatus || agent.status)">{{ runtimeStatusLabel(agent.runtimeStatus || agent.status) }}</el-tag>
            </div>
            <p class="muted sm mt-sm">{{ agent.agentDescription || '尚未填写描述。' }}</p>
            <div class="agent-meta"><span>最近运行</span><strong>{{ agent.lastRunAt || '暂无' }}</strong></div>
            <div class="agent-meta"><span>最近结果</span><strong>{{ agent.lastRunSummary || agent.lastRunStatus || '待命中' }}</strong></div>
            <div class="row wrap gap-xs mt-sm">
              <el-tag v-for="tag in agent.tags.slice(0, 3)" :key="`${agent.id}-${tag}`" size="small" effect="plain">{{ tag }}</el-tag>
              <span v-if="!agent.tags.length" class="muted xs">未打标签</span>
            </div>
          </button>
          <div v-if="!filteredAgents.length && !listLoading" class="empty-state">
            <pixel-duck-bot-icon class="h-14 w-14" />
            <strong>还没有可见的 Agent</strong>
            <p>先建一个像素机器鸭子，让个人自动化真正落地。</p>
          </div>
        </div>
      </section>
    </aside>

    <main class="panel editor">
      <section class="section hero-editor" :style="{ '--cover': editableAgent.coverColor || '#d97706' }">
        <div class="row between start wrap gap-md">
          <div class="row start gap-md">
            <div class="hero-editor__icon"><pixel-duck-bot-icon class="h-20 w-20" /></div>
            <div>
              <div class="eyebrow">PIXEL DUCK / OPEN PLATFORM SKELETON</div>
              <div class="row gap-sm wrap mt-xs">
                <h2>{{ editableAgent.agentName || '未命名 Agent' }}</h2>
                <el-tag :type="summaryStatusType(editableAgent.status)">{{ runtimeStatusLabel(editableAgent.status) }}</el-tag>
              </div>
              <p>{{ editableAgent.agentDescription || '这是一个个人私有 Agent，可配置模型、Prompt、工具与流程。' }}</p>
              <div class="muted xs mt-sm row wrap gap-sm">
                <span>编码：{{ editableAgent.agentCode || '新建后生成' }}</span>
                <span>最新版本：v{{ editableAgent.latestVersionNo || 1 }}</span>
                <span>已发布：{{ editableAgent.publishedVersionNo ? `v${editableAgent.publishedVersionNo}` : '未发布' }}</span>
              </div>
            </div>
          </div>
          <div class="row wrap gap-sm">
            <el-button :icon="DocumentCopy" :disabled="!editableAgent.id || !canCreate" @click="duplicateAgent">复制</el-button>
            <el-button :icon="SwitchButton" :disabled="!editableAgent.id || !canEdit" @click="toggleAgentStatus(editableAgent.status === 'DISABLED' ? 'READY' : 'DISABLED')">
              {{ editableAgent.status === 'DISABLED' ? '启用' : '停用' }}
            </el-button>
            <el-button :icon="FolderDelete" :disabled="!editableAgent.id || !canDelete" @click="archiveAgent">归档</el-button>
            <el-button type="primary" :loading="saveLoading" :disabled="!canSave" @click="saveAgent">保存配置</el-button>
            <el-button type="warning" :loading="publishLoading" :disabled="!editableAgent.id || !canPublish" @click="publishAgent">发布版本</el-button>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div><div class="eyebrow">Basics</div><h3>基本信息</h3></div>
          <div class="row wrap gap-xs">
            <el-tag v-for="version in editableAgent.versions.slice(0, 4)" :key="version.id" size="small" :type="version.published ? 'warning' : 'info'">v{{ version.versionNo }}</el-tag>
          </div>
        </div>
        <div class="grid two">
          <el-form-item label="Agent 名称"><el-input v-model="editableAgent.agentName" maxlength="60" show-word-limit placeholder="例如：报销周报鸭" /></el-form-item>
          <el-form-item label="封面主色">
            <div class="row gap-sm">
              <input v-model="editableAgent.coverColor" class="color-input" type="color" />
              <el-input v-model="editableAgent.coverColor" placeholder="#d97706" />
            </div>
          </el-form-item>
          <el-form-item label="描述"><el-input v-model="editableAgent.agentDescription" type="textarea" :rows="3" maxlength="240" show-word-limit placeholder="说明这个 Agent 主要解决什么问题。" /></el-form-item>
          <el-form-item label="标签">
            <el-select v-model="editableAgent.tags" multiple filterable allow-create default-first-option placeholder="输入并回车创建标签">
              <el-option v-for="tag in availableTags" :key="`tag-${tag}`" :label="tag" :value="tag" />
            </el-select>
          </el-form-item>
          <el-form-item label="鸭子头像"><el-select v-model="editableAgent.iconKey" placeholder="选择图标方案"><el-option v-for="item in iconOptions" :key="item.code" :label="item.label" :value="item.code" /></el-select></el-form-item>
          <el-form-item label="主题方案"><el-select v-model="editableAgent.themeKey" placeholder="选择主题方案"><el-option v-for="item in themeOptions" :key="item.code" :label="item.label" :value="item.code" /></el-select></el-form-item>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div><div class="eyebrow">Prompt</div><h3>Prompt 配置</h3></div>
          <el-tag type="warning" effect="plain">混合模式</el-tag>
        </div>
        <div class="grid two">
          <el-form-item label="系统提示词"><el-input v-model="editableAgent.promptConfig.systemPrompt" type="textarea" :rows="4" placeholder="定义 Agent 的行为边界与输出风格。" /></el-form-item>
          <el-form-item label="角色说明"><el-input v-model="editableAgent.promptConfig.rolePrompt" type="textarea" :rows="4" placeholder="例如：你是一个只服务当前用户的档案助手。" /></el-form-item>
          <el-form-item label="输出格式约束"><el-input v-model="editableAgent.promptConfig.outputSchema" type="textarea" :rows="3" placeholder='例如：{"summary":"","actions":[]}' /></el-form-item>
          <el-form-item label="Guardrails"><el-input v-model="editableAgent.promptConfig.guardrails" type="textarea" :rows="3" placeholder="补充安全约束、越权限制和脱敏规则。" /></el-form-item>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div><div class="eyebrow">Model</div><h3>模型配置</h3></div>
          <el-tag type="info" effect="plain">Provider / Model / Credential 分离</el-tag>
        </div>
        <div class="grid three">
          <el-form-item label="模型提供商"><el-select v-model="editableAgent.modelConfig.provider" placeholder="选择 Provider"><el-option v-for="item in providerOptions" :key="item.code" :label="item.label" :value="item.code" /></el-select></el-form-item>
          <el-form-item label="模型名称"><el-input v-model="editableAgent.modelConfig.model" placeholder="duck-agent-v1" /></el-form-item>
          <el-form-item label="凭据引用"><el-input v-model="editableAgent.modelConfig.credentialCode" placeholder="如需外部模型，请填写凭据编码" /></el-form-item>
          <el-form-item label="温度"><el-input-number v-model="editableAgent.modelConfig.temperature" :min="0" :max="2" :step="0.1" /></el-form-item>
          <el-form-item label="最大 Token"><el-input-number v-model="editableAgent.modelConfig.maxTokens" :min="128" :max="8192" :step="128" /></el-form-item>
          <el-form-item label="超时（秒）"><el-input-number v-model="editableAgent.modelConfig.timeoutSeconds" :min="5" :max="300" :step="5" /></el-form-item>
        </div>
      </section>
      <section class="section">
        <div class="section-head">
          <div><div class="eyebrow">Tools</div><h3>工具配置</h3></div>
          <el-tag type="success" effect="plain">业务工具 + 通用工具</el-tag>
        </div>
        <div class="tool-grid">
          <article v-for="tool in toolCards" :key="tool.toolCode" class="mini-card" :class="{ active: tool.enabled }">
            <div class="row between start gap-sm">
              <div>
                <strong>{{ tool.label }}</strong>
                <div class="muted xs mt-xs">{{ tool.categoryLabel }} · {{ tool.toolCode }}</div>
              </div>
              <el-switch :model-value="tool.enabled" :disabled="!tool.available" @change="toggleTool(tool.toolCode, Boolean($event))" />
            </div>
            <div v-if="tool.enabled && tool.binding" class="stack gap-sm mt-sm">
              <el-input v-model="tool.binding.credentialRefCode" placeholder="凭据编码（可选）" />
              <el-input v-if="tool.toolCode === 'http.mock_request'" v-model="tool.binding.url" placeholder="模拟 HTTP 地址" />
              <el-input v-if="tool.toolCode === 'notify.send_message'" v-model="tool.binding.title" placeholder="通知标题" />
            </div>
          </article>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div><div class="eyebrow">Workflow</div><h3>流程编排</h3></div>
          <div class="row wrap gap-xs">
            <el-button size="small" :icon="Plus" @click="addWorkflowNode('llm')">添加 LLM</el-button>
            <el-button size="small" :icon="Plus" @click="addWorkflowNode('tool')">添加工具</el-button>
            <el-button size="small" :icon="Plus" @click="addWorkflowNode('condition')">添加条件</el-button>
            <el-button size="small" :icon="Connection" @click="addWorkflowEdge">添加连线</el-button>
          </div>
        </div>
        <div class="workflow-board">
          <div class="workflow-nodes">
            <article v-for="node in editableAgent.workflow.nodes" :key="node.nodeKey" class="mini-card node-card">
              <div class="row between start gap-sm">
                <div>
                  <div class="muted xs">{{ node.nodeKey }}</div>
                  <strong>{{ node.label }}</strong>
                </div>
                <el-button v-if="!isProtectedNode(node.nodeType)" link type="danger" @click="removeWorkflowNode(node.nodeKey)">删除</el-button>
              </div>
              <div class="stack gap-sm mt-sm">
                <el-select v-model="node.nodeType" @change="handleNodeTypeChange(node)"><el-option v-for="item in nodeTypeOptions" :key="item.code" :label="item.label" :value="item.code" /></el-select>
                <el-input v-model="node.label" placeholder="节点标题" />
                <el-input v-if="node.nodeType === 'llm'" v-model="node.config.promptTemplate" type="textarea" :rows="3" placeholder="节点 Prompt" />
                <el-input v-if="node.nodeType === 'condition'" v-model="node.config.expression" placeholder="条件表达式" />
                <el-select v-if="node.nodeType === 'tool'" v-model="node.config.toolCode" placeholder="选择工具"><el-option v-for="tool in enabledToolOptions" :key="tool.toolCode" :label="tool.label" :value="tool.toolCode" /></el-select>
                <el-input v-if="node.nodeType === 'transform'" v-model="node.config.template" type="textarea" :rows="2" placeholder="变量转换模板" />
                <el-input v-if="node.nodeType === 'notify'" v-model="node.config.message" type="textarea" :rows="2" placeholder="通知内容" />
              </div>
            </article>
          </div>
          <div class="edge-box">
            <div class="row between gap-sm"><strong>流程连线</strong><span class="muted xs">至少保证 start → end 可达</span></div>
            <div v-if="editableAgent.workflow.edges.length" class="stack gap-sm mt-sm">
              <div v-for="(edge, index) in editableAgent.workflow.edges" :key="`${edge.source}-${edge.target}-${index}`" class="edge-row">
                <el-select v-model="edge.source" placeholder="起点"><el-option v-for="node in editableAgent.workflow.nodes" :key="`s-${node.nodeKey}`" :label="node.label" :value="node.nodeKey" /></el-select>
                <span class="muted">→</span>
                <el-select v-model="edge.target" placeholder="终点"><el-option v-for="node in editableAgent.workflow.nodes" :key="`t-${node.nodeKey}`" :label="node.label" :value="node.nodeKey" /></el-select>
                <el-button link type="danger" @click="removeWorkflowEdge(index)">移除</el-button>
              </div>
            </div>
            <div v-else class="empty-inline">尚未配置连线，运行时将无法通过流程校验。</div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div><div class="eyebrow">Triggers</div><h3>触发器</h3></div>
          <div class="row wrap gap-xs">
            <el-button size="small" :icon="Plus" @click="addTrigger('MANUAL')">手动</el-button>
            <el-button size="small" :icon="Clock" @click="addTrigger('SCHEDULE')">定时</el-button>
            <el-button size="small" :icon="Bell" @click="addTrigger('EVENT')">事件预留</el-button>
          </div>
        </div>
        <div class="tool-grid">
          <article v-for="(trigger, index) in editableAgent.triggers" :key="`${trigger.triggerType}-${index}`" class="mini-card">
            <div class="row between start gap-sm">
              <div><strong>{{ triggerLabel(trigger.triggerType) }}</strong><div class="muted xs mt-xs">{{ trigger.triggerType }}</div></div>
              <div class="row gap-xs"><el-switch v-model="trigger.enabled" /><el-button link type="danger" @click="removeTrigger(index)">删除</el-button></div>
            </div>
            <div class="stack gap-sm mt-sm">
              <template v-if="trigger.triggerType === 'SCHEDULE'">
                <el-radio-group v-model="trigger.scheduleMode"><el-radio-button label="INTERVAL">固定间隔</el-radio-button><el-radio-button label="CRON">Cron</el-radio-button></el-radio-group>
                <el-input-number v-if="trigger.scheduleMode === 'INTERVAL'" v-model="trigger.intervalMinutes" :min="5" :max="1440" :step="5" />
                <el-input v-else v-model="trigger.cronExpression" placeholder="例如：0 */2 * * * ?" />
              </template>
              <template v-else-if="trigger.triggerType === 'EVENT'">
                <el-input v-model="trigger.eventCode" placeholder="预留字段，例如 expense.document.approved" />
                <el-alert type="info" :closable="false" title="事件触发当前仅预留接口，首版先以手动试跑和定时执行为主。" />
              </template>
              <el-alert v-else type="success" :closable="false" title="手动触发可在右侧调试面板立即试跑。" />
            </div>
          </article>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div><div class="eyebrow">Schema</div><h3>输入 Schema</h3></div>
          <el-button text @click="resetInputSchema">恢复默认</el-button>
        </div>
        <el-input v-model="inputSchemaText" class="monospace" type="textarea" :rows="8" placeholder='{"type":"object","properties":{"query":{"type":"string"}}}' />
      </section>
    </main>

    <aside class="panel runtime">
      <section class="panel-section accent">
        <div class="section-head"><div><div class="eyebrow">Debug</div><h3>测试运行</h3></div><el-tag type="warning">手动试跑</el-tag></div>
        <el-form-item label="触发来源"><el-input v-model="runForm.triggerSource" placeholder="workbench.manual" /></el-form-item>
        <el-form-item label="测试入参 JSON"><el-input v-model="runForm.inputPayloadText" class="monospace" type="textarea" :rows="8" placeholder='{"query":"帮我整理待报销事项"}' /></el-form-item>
        <div class="row between wrap gap-sm mt-sm">
          <el-button :icon="RefreshRight" @click="resetRunInput">重置入参</el-button>
          <el-button type="primary" :icon="VideoPlay" :loading="runLoading" :disabled="!editableAgent.id || !canRun" @click="runAgentNow">立即执行</el-button>
        </div>
      </section>

      <section class="panel-section">
        <div class="section-head"><div><div class="eyebrow">Runs</div><h3>最近运行记录</h3></div><el-tag size="small" effect="plain">{{ runRecords.length }} 条</el-tag></div>
        <div v-if="runPermissionMessage" class="empty-state small"><p>{{ runPermissionMessage }}</p></div>
        <div v-else-if="runsLoading" class="empty-state small"><p>正在加载运行记录...</p></div>
        <div v-else-if="runRecords.length" class="stack gap-sm">
          <button v-for="run in runRecords" :key="run.id" type="button" class="mini-card run-card" :class="{ active: selectedRunId === run.id }" @click="loadRunDetail(run.id)">
            <div class="row between gap-sm"><strong>{{ run.runNo }}</strong><el-tag size="small" :type="runStatusType(run.status)">{{ run.status }}</el-tag></div>
            <div class="stack gap-xs mt-sm muted xs">
              <span>来源：{{ run.triggerSource || run.triggerType }}</span>
              <span>耗时：{{ run.durationMs ? `${run.durationMs}ms` : '执行中' }}</span>
              <span>摘要：{{ run.summary || run.errorMessage || '无摘要' }}</span>
            </div>
          </button>
        </div>
        <div v-else class="empty-state small"><p>还没有运行记录，保存并试跑一次后这里会开始沉淀日志。</p></div>
      </section>

      <section class="panel-section">
        <div class="section-head"><div><div class="eyebrow">Logs</div><h3>步骤日志</h3></div><el-tag size="small" effect="plain">{{ currentRunDetail?.steps.length || 0 }} steps</el-tag></div>
        <div v-if="runDetailLoading" class="empty-state small"><p>正在加载运行详情...</p></div>
        <template v-else-if="currentRunDetail">
          <div class="log-summary">
            <div><span>Agent 版本</span><strong>v{{ currentRunDetail.agentVersionNo || '-' }}</strong></div>
            <div><span>运行结果</span><strong>{{ currentRunDetail.summary || currentRunDetail.status }}</strong></div>
            <div><span>输出摘要</span><strong>{{ stringifyInline(currentRunDetail.outputPayload) }}</strong></div>
          </div>
          <div class="stack gap-sm mt-sm">
            <article v-for="step in currentRunDetail.steps" :key="`${step.stepNo}-${step.nodeKey}`" class="mini-card">
              <div class="row between gap-sm"><div><div class="muted xs">STEP {{ step.stepNo }}</div><strong>{{ step.nodeLabel || step.nodeKey }}</strong></div><el-tag size="small" :type="runStatusType(step.status)" effect="plain">{{ step.status }}</el-tag></div>
              <div class="row wrap gap-sm muted xs mt-xs"><span>{{ step.nodeType }}</span><span>{{ step.durationMs ? `${step.durationMs}ms` : '执行中' }}</span></div>
              <div class="payload-grid mt-sm"><div><label>输入</label><pre>{{ prettyJson(step.inputPayload) }}</pre></div><div><label>输出</label><pre>{{ prettyJson(step.outputPayload) }}</pre></div></div>
              <p v-if="step.errorMessage" class="error mt-sm">{{ step.errorMessage }}</p>
            </article>
          </div>
        </template>
        <div v-else class="empty-state small"><p>点选一条运行记录后，这里会展示每个节点的输入、输出和错误摘要。</p></div>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Clock, Connection, DocumentCopy, FolderDelete, Plus, RefreshRight, Search, SwitchButton, VideoPlay } from '@element-plus/icons-vue'
import PixelDuckBotIcon from '@/components/icons/PixelDuckBotIcon.vue'
import { archiveAgentApi, type ArchiveAgentDetail, type ArchiveAgentMeta, type ArchiveAgentRunDetail, type ArchiveAgentRunRecord, type ArchiveAgentSavePayload, type ArchiveAgentSummary, type ArchiveAgentVersionRecord, type ArchiveAgentWorkflowEdge, type ArchiveAgentWorkflowNode } from '@/api'
import { hasAnyPermission, readStoredUser } from '@/utils/permissions'

type PromptConfig = { systemPrompt: string; rolePrompt: string; outputSchema: string; guardrails: string }
type ModelConfig = { provider: string; model: string; temperature: number; maxTokens: number; timeoutSeconds: number; credentialCode: string }
type ToolBinding = { toolCode: string; label?: string; category?: string; credentialRefCode?: string; url?: string; title?: string }
type TriggerModel = { triggerType: 'MANUAL' | 'SCHEDULE' | 'EVENT'; enabled: boolean; scheduleMode?: 'CRON' | 'INTERVAL'; cronExpression?: string; intervalMinutes?: number; eventCode?: string }
type NodeModel = ArchiveAgentWorkflowNode & { config: Record<string, any> }
type EditableAgent = {
  id?: number
  agentCode?: string
  agentName: string
  agentDescription: string
  iconKey: string
  themeKey: string
  coverColor: string
  tags: string[]
  status: 'DRAFT' | 'READY' | 'DISABLED' | 'ARCHIVED'
  latestVersionNo?: number
  publishedVersionNo?: number
  promptConfig: PromptConfig
  modelConfig: ModelConfig
  tools: ToolBinding[]
  workflow: { nodes: NodeModel[]; edges: ArchiveAgentWorkflowEdge[] }
  triggers: TriggerModel[]
  versions: ArchiveAgentVersionRecord[]
}
type OptionItem = { code: string; label: string; available?: boolean; category?: string }

const filters = ref({ keyword: '', status: '', tag: '' })
const meta = ref<ArchiveAgentMeta | null>(null)
const listLoading = ref(false)
const saveLoading = ref(false)
const publishLoading = ref(false)
const runLoading = ref(false)
const runsLoading = ref(false)
const runDetailLoading = ref(false)
const agentList = ref<ArchiveAgentSummary[]>([])
const selectedAgentId = ref<number>()
const runRecords = ref<ArchiveAgentRunRecord[]>([])
const selectedRunId = ref<number>()
const currentRunDetail = ref<ArchiveAgentRunDetail | null>(null)
const runPermissionMessage = ref('')
const inputSchemaText = ref('')
const runForm = ref({ triggerSource: 'workbench.manual', inputPayloadText: defaultRunInputText() })
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const canCreate = computed(() => hasAnyPermission(['agents:create'], permissionCodes.value))
const canEdit = computed(() => hasAnyPermission(['agents:edit'], permissionCodes.value))
const canDelete = computed(() => hasAnyPermission(['agents:delete'], permissionCodes.value))
const canRun = computed(() => hasAnyPermission(['agents:run'], permissionCodes.value))
const canPublish = computed(() => hasAnyPermission(['agents:publish'], permissionCodes.value))
const canViewLogs = computed(() => hasAnyPermission(['agents:view_logs'], permissionCodes.value))
const canSave = computed(() => (editableAgent.value.id ? canEdit.value : canCreate.value))
const statusOptions = [{ label: '待命', value: 'READY' }, { label: '草稿', value: 'DRAFT' }, { label: '停用', value: 'DISABLED' }, { label: '归档', value: 'ARCHIVED' }]
const editableAgent = ref<EditableAgent>(createDefaultAgent())
const filteredAgents = computed(() => filters.value.tag ? agentList.value.filter((item) => item.tags.includes(filters.value.tag)) : agentList.value)
const availableTags = computed(() => Array.from(new Set(agentList.value.flatMap((item) => item.tags))))
const runningAgentCount = computed(() => agentList.value.filter((item) => item.runtimeStatus === 'RUNNING').length)
const failedAgentCount = computed(() => agentList.value.filter((item) => item.runtimeStatus === 'FAILED').length)
const providerOptions = computed(() => toOptions(meta.value?.modelProviders, 'code', 'label'))
const iconOptions = computed(() => toOptions(meta.value?.iconOptions, 'code', 'label'))
const themeOptions = computed(() => toOptions(meta.value?.themeOptions, 'code', 'label'))
const nodeTypeOptions = computed(() => toOptions(meta.value?.nodeTypes, 'code', 'label'))
const toolCards = computed(() => {
  const selected = new Map(editableAgent.value.tools.map((item) => [item.toolCode, item]))
  return toOptions(meta.value?.tools, 'toolCode', 'label').map((item) => ({ ...item, toolCode: item.code, categoryLabel: item.category === 'BUSINESS' ? '业务工具' : '通用工具', enabled: selected.has(item.code), binding: selected.get(item.code) }))
})
const enabledToolOptions = computed(() => toolCards.value.filter((item) => item.enabled).map((item) => ({ toolCode: item.toolCode, label: item.label })))

onMounted(async () => {
  await loadMeta()
  await loadAgents(false)
})

async function loadMeta() {
  try {
    const res = await archiveAgentApi.getMeta()
    meta.value = res.data
    if (!selectedAgentId.value) startCreateAgent()
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 元数据加载失败')
  }
}

async function loadAgents(keepSelection = true) {
  listLoading.value = true
  try {
    const res = await archiveAgentApi.list({ keyword: normalizeText(filters.value.keyword), status: normalizeText(filters.value.status) })
    agentList.value = res.data || []
    if (!agentList.value.length) {
      startCreateAgent()
      return
    }
    const targetId = keepSelection && selectedAgentId.value && agentList.value.some((item) => item.id === selectedAgentId.value) ? selectedAgentId.value : agentList.value[0]?.id
    if (targetId) await selectAgent(targetId)
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 列表加载失败')
  } finally {
    listLoading.value = false
  }
}

async function selectAgent(id: number) {
  selectedAgentId.value = id
  try {
    const res = await archiveAgentApi.getDetail(id)
    editableAgent.value = detailToEditable(res.data)
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    runForm.value = { triggerSource: 'workbench.manual', inputPayloadText: defaultRunInputText() }
    currentRunDetail.value = null
    selectedRunId.value = undefined
    await loadRuns(id)
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 详情加载失败')
  }
}
function startCreateAgent() {
  selectedAgentId.value = undefined
  selectedRunId.value = undefined
  currentRunDetail.value = null
  editableAgent.value = createDefaultAgent(meta.value)
  inputSchemaText.value = prettyJson(defaultInputSchema())
  runForm.value = { triggerSource: 'workbench.manual', inputPayloadText: defaultRunInputText() }
  runRecords.value = []
  runPermissionMessage.value = canViewLogs.value ? '' : '当前账号没有查看运行日志的权限。'
}

async function saveAgent() {
  const payload = buildSavePayload()
  if (!payload) return
  saveLoading.value = true
  try {
    const isUpdate = Boolean(editableAgent.value.id)
    const res = isUpdate ? await archiveAgentApi.update(editableAgent.value.id!, payload) : await archiveAgentApi.create(payload)
    editableAgent.value = detailToEditable(res.data)
    selectedAgentId.value = res.data.id
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    await loadAgents(true)
    ElMessage.success(isUpdate ? 'Agent 配置已保存' : 'Agent 已创建')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 保存失败')
  } finally {
    saveLoading.value = false
  }
}

async function publishAgent() {
  if (!editableAgent.value.id) return ElMessage.warning('请先保存 Agent 再发布')
  publishLoading.value = true
  try {
    const res = await archiveAgentApi.publish(editableAgent.value.id)
    editableAgent.value = detailToEditable(res.data)
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    await loadAgents(true)
    ElMessage.success('Agent 已发布为最新版本')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 发布失败')
  } finally {
    publishLoading.value = false
  }
}

async function duplicateAgent() {
  if (!editableAgent.value.id) return ElMessage.warning('请先选择一个已保存的 Agent')
  try {
    await ElMessageBox.confirm('将基于当前配置创建一个副本，并保留独立的后续版本。', '复制 Agent', { type: 'info' })
  } catch {
    return
  }
  saveLoading.value = true
  try {
    const payload = buildSavePayload(`${editableAgent.value.agentName} 副本`)
    if (!payload) return
    const res = await archiveAgentApi.create(payload)
    editableAgent.value = detailToEditable(res.data)
    selectedAgentId.value = res.data.id
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    await loadAgents(true)
    ElMessage.success('Agent 副本已创建')
  } catch (error: any) {
    ElMessage.error(error?.message || '复制 Agent 失败')
  } finally {
    saveLoading.value = false
  }
}

async function toggleAgentStatus(status: EditableAgent['status']) {
  if (!editableAgent.value.id) return ElMessage.warning('请先保存 Agent')
  try {
    const res = await archiveAgentApi.toggleStatus(editableAgent.value.id, status)
    editableAgent.value = detailToEditable(res.data)
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    await loadAgents(true)
    ElMessage.success(status === 'DISABLED' ? 'Agent 已停用' : 'Agent 已启用')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 状态更新失败')
  }
}

async function archiveAgent() {
  if (!editableAgent.value.id) return ElMessage.warning('请先保存 Agent')
  try {
    await ElMessageBox.confirm('归档后 Agent 不会再被手动或定时触发。是否继续？', '归档 Agent', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await archiveAgentApi.toggleStatus(editableAgent.value.id, 'ARCHIVED')
    editableAgent.value = detailToEditable(res.data)
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    await loadAgents(true)
    ElMessage.success('Agent 已归档')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 归档失败')
  }
}

async function runAgentNow() {
  if (!editableAgent.value.id) return ElMessage.warning('请先保存 Agent 再试跑')
  const inputPayload = parseJson(runForm.value.inputPayloadText, '测试入参 JSON 不合法')
  if (!inputPayload) return
  runLoading.value = true
  try {
    const res = await archiveAgentApi.run(editableAgent.value.id, { triggerSource: normalizeText(runForm.value.triggerSource) || 'workbench.manual', inputPayload })
    ElMessage.success(`运行请求已提交：${res.data.runNo}`)
    await loadRuns(editableAgent.value.id)
    await loadRunDetail(res.data.id)
    await loadAgents(true)
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 试跑失败')
  } finally {
    runLoading.value = false
  }
}

async function loadRuns(agentId: number) {
  runPermissionMessage.value = ''
  runRecords.value = []
  if (!canViewLogs.value) {
    runPermissionMessage.value = '当前账号没有查看运行日志的权限。'
    return
  }
  runsLoading.value = true
  try {
    const res = await archiveAgentApi.listRuns(agentId)
    runRecords.value = res.data || []
    const firstRun = runRecords.value[0]
    if (firstRun) await loadRunDetail(firstRun.id)
  } catch (error: any) {
    runPermissionMessage.value = error?.message || '运行日志加载失败'
  } finally {
    runsLoading.value = false
  }
}

async function loadRunDetail(runId: number) {
  selectedRunId.value = runId
  runDetailLoading.value = true
  try {
    const res = await archiveAgentApi.getRunDetail(runId)
    currentRunDetail.value = res.data
  } catch (error: any) {
    ElMessage.error(error?.message || '运行详情加载失败')
  } finally {
    runDetailLoading.value = false
  }
}

function toggleTool(toolCode: string, enabled: boolean) {
  const current = editableAgent.value.tools.find((item) => item.toolCode === toolCode)
  if (enabled && !current) {
    const metaTool = toolCards.value.find((item) => item.toolCode === toolCode)
    editableAgent.value.tools.push({ toolCode, label: metaTool?.label, category: metaTool?.category })
    return
  }
  if (!enabled) {
    editableAgent.value.tools = editableAgent.value.tools.filter((item) => item.toolCode !== toolCode)
    editableAgent.value.workflow.nodes.forEach((node) => {
      if (node.nodeType === 'tool' && node.config.toolCode === toolCode) node.config.toolCode = ''
    })
  }
}

function addWorkflowNode(nodeType: NodeModel['nodeType']) { editableAgent.value.workflow.nodes.push(buildNode(nodeType)) }
function removeWorkflowNode(nodeKey: string) {
  editableAgent.value.workflow.nodes = editableAgent.value.workflow.nodes.filter((item) => item.nodeKey !== nodeKey)
  editableAgent.value.workflow.edges = editableAgent.value.workflow.edges.filter((item) => item.source !== nodeKey && item.target !== nodeKey)
}
function addWorkflowEdge() {
  const nodes = editableAgent.value.workflow.nodes
  if (nodes.length < 2) return ElMessage.warning('至少需要两个节点才能连线')
  const firstNode = nodes[0]
  const lastNode = nodes[nodes.length - 1]
  if (!firstNode || !lastNode) return
  editableAgent.value.workflow.edges.push({ source: firstNode.nodeKey, target: lastNode.nodeKey })
}
function removeWorkflowEdge(index: number) { editableAgent.value.workflow.edges.splice(index, 1) }
function handleNodeTypeChange(node: NodeModel) { node.config = defaultNodeConfig(node.nodeType); if (!node.label || node.label.startsWith('新')) node.label = defaultNodeLabel(node.nodeType) }
function addTrigger(triggerType: TriggerModel['triggerType']) { editableAgent.value.triggers.push(buildTrigger(triggerType)) }
function removeTrigger(index: number) { editableAgent.value.triggers.splice(index, 1) }
function resetInputSchema() { inputSchemaText.value = prettyJson(defaultInputSchema()) }
function resetRunInput() { runForm.value = { triggerSource: 'workbench.manual', inputPayloadText: defaultRunInputText() } }

function buildSavePayload(nameOverride?: string): ArchiveAgentSavePayload | null {
  const inputSchema = parseJson(inputSchemaText.value, '输入 Schema JSON 不合法')
  const agentName = normalizeText(nameOverride || editableAgent.value.agentName)
  if (!inputSchema || !agentName) {
    if (!agentName) ElMessage.warning('Agent 名称不能为空')
    return null
  }
  return {
    agentName,
    agentDescription: normalizeText(editableAgent.value.agentDescription),
    iconKey: normalizeText(editableAgent.value.iconKey),
    themeKey: normalizeText(editableAgent.value.themeKey),
    coverColor: normalizeText(editableAgent.value.coverColor),
    tags: editableAgent.value.tags.filter((item) => normalizeText(item)).map((item) => item.trim()),
    promptConfig: { ...editableAgent.value.promptConfig },
    modelConfig: { ...editableAgent.value.modelConfig, credentialCode: normalizeText(editableAgent.value.modelConfig.credentialCode) },
    tools: editableAgent.value.tools.map((item) => ({ toolCode: item.toolCode, label: item.label, category: item.category, credentialRefCode: normalizeText(item.credentialRefCode), url: normalizeText(item.url), title: normalizeText(item.title) })),
    workflow: { nodes: editableAgent.value.workflow.nodes.map((item) => ({ nodeKey: item.nodeKey, nodeType: item.nodeType, label: item.label, config: pruneEmpty(item.config) })), edges: editableAgent.value.workflow.edges.map((item) => ({ source: item.source, target: item.target })) },
    triggers: editableAgent.value.triggers.map((item) => ({ triggerType: item.triggerType, enabled: item.enabled, scheduleMode: item.scheduleMode, cronExpression: normalizeText(item.cronExpression), intervalMinutes: item.intervalMinutes, eventCode: normalizeText(item.eventCode) })),
    inputSchema
  }
}

function detailToEditable(detail: ArchiveAgentDetail): EditableAgent {
  const fallbackWorkflow = defaultWorkflow()
  return {
    id: detail.id,
    agentCode: detail.agentCode,
    agentName: detail.agentName || '',
    agentDescription: detail.agentDescription || '',
    iconKey: stringFrom(detail.iconKey, 'pixel-duck'),
    themeKey: stringFrom(detail.themeKey, 'duck-sunrise'),
    coverColor: stringFrom(detail.coverColor, '#d97706'),
    tags: detail.tags || [],
    status: (detail.status as EditableAgent['status']) || 'DRAFT',
    latestVersionNo: detail.latestVersionNo,
    publishedVersionNo: detail.publishedVersionNo,
    promptConfig: { systemPrompt: stringFrom(detail.promptConfig?.systemPrompt, meta.value?.defaultSystemPrompt || ''), rolePrompt: stringFrom(detail.promptConfig?.rolePrompt), outputSchema: stringFrom(detail.promptConfig?.outputSchema), guardrails: stringFrom(detail.promptConfig?.guardrails) },
    modelConfig: { provider: stringFrom(detail.modelConfig?.provider, 'MOCK'), model: stringFrom(detail.modelConfig?.model, 'duck-agent-v1'), temperature: numberFrom(detail.modelConfig?.temperature, 0.3), maxTokens: numberFrom(detail.modelConfig?.maxTokens, 1200), timeoutSeconds: numberFrom(detail.modelConfig?.timeoutSeconds, 30), credentialCode: stringFrom(detail.modelConfig?.credentialCode) },
    tools: (detail.tools || []).map((item) => ({ toolCode: stringFrom(item.toolCode), label: stringFrom(item.label), category: stringFrom(item.category), credentialRefCode: stringFrom(item.credentialRefCode), url: stringFrom(item.url), title: stringFrom(item.title) })),
    workflow: { nodes: Array.isArray(detail.workflow?.nodes) ? detail.workflow.nodes.map((item) => ({ nodeKey: item.nodeKey, nodeType: item.nodeType, label: item.label, config: item.config ? clone(item.config) : defaultNodeConfig(item.nodeType) })) : fallbackWorkflow.nodes, edges: Array.isArray(detail.workflow?.edges) ? detail.workflow.edges.map((item) => ({ source: item.source, target: item.target })) : fallbackWorkflow.edges },
    triggers: Array.isArray(detail.triggers) && detail.triggers.length ? detail.triggers.map((item) => ({ triggerType: stringFrom(item.triggerType, 'MANUAL') as TriggerModel['triggerType'], enabled: item.enabled !== false, scheduleMode: stringFrom(item.scheduleMode, 'INTERVAL') as TriggerModel['scheduleMode'], cronExpression: stringFrom(item.cronExpression), intervalMinutes: numberFrom(item.intervalMinutes, 60), eventCode: stringFrom(item.eventCode) })) : [buildTrigger('MANUAL'), buildTrigger('SCHEDULE')],
    versions: detail.versions || []
  }
}
function createDefaultAgent(sourceMeta?: ArchiveAgentMeta | null): EditableAgent {
  return {
    agentName: '我的自动化鸭子',
    agentDescription: '个人自动化 Agent，适合先从手动试跑和定时任务开始。',
    iconKey: 'pixel-duck',
    themeKey: 'duck-sunrise',
    coverColor: '#d97706',
    tags: ['个人', '自动化'],
    status: 'DRAFT',
    latestVersionNo: 1,
    promptConfig: { systemPrompt: sourceMeta?.defaultSystemPrompt || '你是一个个人自动化 Agent。优先输出结构化结论，并在需要时调用工具补充事实。', rolePrompt: '你只服务当前登录用户，不能越权查看其他人的数据。', outputSchema: '{"summary":"","actions":[]}', guardrails: '不泄露敏感信息；调用工具前先确认是否必要。' },
    modelConfig: { provider: 'MOCK', model: 'duck-agent-v1', temperature: 0.3, maxTokens: 1200, timeoutSeconds: 30, credentialCode: '' },
    tools: [{ toolCode: 'expense.query_my_expenses', label: '查询我的报销单', category: 'BUSINESS' }, { toolCode: 'notify.send_message', label: '发送站内通知', category: 'BUSINESS', title: 'Agent 执行通知' }],
    workflow: defaultWorkflow(),
    triggers: [buildTrigger('MANUAL'), buildTrigger('SCHEDULE')],
    versions: []
  }
}
function defaultWorkflow() { return { nodes: [buildNode('start', 'start_1', '开始'), buildNode('llm', 'llm_1', '意图分析'), buildNode('end', 'end_1', '结束')], edges: [{ source: 'start_1', target: 'llm_1' }, { source: 'llm_1', target: 'end_1' }] } }
function buildNode(nodeType: NodeModel['nodeType'], nodeKey?: string, label?: string): NodeModel { return { nodeKey: nodeKey || `${nodeType}_${Date.now()}_${Math.random().toString(36).slice(2, 5)}`, nodeType, label: label || defaultNodeLabel(nodeType), config: defaultNodeConfig(nodeType) } }
function buildTrigger(triggerType: TriggerModel['triggerType']): TriggerModel { if (triggerType === 'SCHEDULE') return { triggerType, enabled: false, scheduleMode: 'INTERVAL', intervalMinutes: 60 }; if (triggerType === 'EVENT') return { triggerType, enabled: false, eventCode: '' }; return { triggerType, enabled: true } }
function defaultNodeLabel(nodeType: NodeModel['nodeType']) { return ({ start: '开始', llm: 'LLM 节点', condition: '条件判断', tool: '工具调用', transform: '变量转换', notify: '发送通知', end: '结束' } as Record<string, string>)[nodeType] || '新节点' }
function defaultNodeConfig(nodeType: NodeModel['nodeType']) { if (nodeType === 'llm') return { promptTemplate: '' }; if (nodeType === 'condition') return { expression: '' }; if (nodeType === 'tool') return { toolCode: enabledToolOptions.value[0]?.toolCode || '' }; if (nodeType === 'transform') return { template: '' }; if (nodeType === 'notify') return { message: '' }; return {} }
function defaultInputSchema() { return { type: 'object', properties: { query: { type: 'string', description: '本次想让 Agent 处理的问题' }, context: { type: 'string', description: '补充上下文' } } } }
function defaultRunInputText() { return prettyJson({ query: '帮我总结最近待处理的报销事项', context: '优先关注待补件和待付款单据' }) }
function triggerLabel(triggerType: string) { return ({ MANUAL: '手动触发', SCHEDULE: '定时触发', EVENT: '事件触发' } as Record<string, string>)[triggerType] || triggerType }
function runtimeStatusLabel(status?: string) { return ({ READY: '待命', RUNNING: '运行中', FAILED: '异常', DISABLED: '已停用', ARCHIVED: '已归档', DRAFT: '草稿' } as Record<string, string>)[status || 'DRAFT'] || '草稿' }
function summaryStatusType(status?: string) { return ({ READY: 'success', RUNNING: 'warning', FAILED: 'danger', DISABLED: 'info', ARCHIVED: 'info', DRAFT: 'info' } as Record<string, string>)[status || 'DRAFT'] || 'info' }
function runStatusType(status?: string) { return ({ SUCCESS: 'success', COMPLETED: 'success', RUNNING: 'warning', PENDING: 'warning', FAILED: 'danger' } as Record<string, string>)[status || ''] || 'info' }
function isProtectedNode(nodeType: string) { return nodeType === 'start' || nodeType === 'end' }
function prettyJson(value: unknown) { try { return JSON.stringify(value ?? {}, null, 2) } catch { return '{}' } }
function stringifyInline(value: unknown) { try { const text = JSON.stringify(value ?? {}); return text.length > 72 ? `${text.slice(0, 72)}...` : text } catch { return '{}' } }
function parseJson(text: string, message: string) { try { return JSON.parse(text || '{}') as Record<string, unknown> } catch { ElMessage.warning(message); return null } }
function pruneEmpty(source: Record<string, any>) { return Object.fromEntries(Object.entries(source || {}).filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== '')) }
function toOptions(list: Array<Record<string, unknown>> | undefined, codeKey: string, labelKey: string): OptionItem[] { return (list || []).map((item) => ({ code: stringFrom(item[codeKey]), label: stringFrom(item[labelKey]), available: Boolean(item.available ?? true), category: stringFrom(item.category) })) }
function clone<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T }
function normalizeText(value?: string | null) { const text = String(value || '').trim(); return text || undefined }
function stringFrom(value: unknown, fallback = '') { return typeof value === 'string' ? value : fallback }
function numberFrom(value: unknown, fallback = 0) { return typeof value === 'number' && !Number.isNaN(value) ? value : fallback }
</script>

<style scoped>
.agent-workbench { --ink: #273047; --accent: #d97706; --mint: #1f9f9f; display: grid; grid-template-columns: minmax(280px,320px) minmax(0,1.5fr) minmax(320px,380px); gap: 18px; min-height: 100%; }
.panel { min-width: 0; display: flex; flex-direction: column; gap: 18px; }
.hero-card, .panel-section, .section { border: 1px solid rgba(39,48,71,.08); border-radius: 28px; background: rgba(255,255,255,.95); box-shadow: 0 18px 40px rgba(39,48,71,.08); }
.hero-card, .panel-section, .section { padding: 18px; }
.hero-card { background: radial-gradient(circle at top right, rgba(31,159,159,.2), transparent 34%), linear-gradient(135deg, rgba(255,212,77,.26), rgba(255,255,255,.98)); }
.hero-main, .row { display: flex; align-items: center; }
.row.start { align-items: flex-start; }
.row.between { justify-content: space-between; }
.row.wrap { flex-wrap: wrap; }
.gap-xs { gap: 6px; } .gap-sm { gap: 10px; } .gap-md { gap: 14px; } .stack { display: flex; flex-direction: column; } .flex-1 { flex: 1; } .min-w-0 { min-width: 0; } .mt-xs { margin-top: 4px; } .mt-sm { margin-top: 10px; } .mt-md { margin-top: 14px; }
.eyebrow { font-size: 11px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; color: rgba(39,48,71,.62); }
.hero-icon, .duck-badge, .hero-editor__icon { display: inline-flex; align-items: center; justify-content: center; border-radius: 20px; color: var(--ink); }
.hero-icon { width: 82px; height: 82px; background: linear-gradient(135deg, rgba(255,255,255,.9), rgba(255,212,77,.48)); }
.hero-card h1, .hero-editor h2 { margin: 0; font-size: 28px; font-weight: 800; color: var(--ink); }
.hero-card p, .hero-editor p { margin: 8px 0 0; color: rgba(39,48,71,.72); line-height: 1.6; }
.hero-stats, .log-summary { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 10px; margin-top: 16px; }
.hero-stats div, .log-summary div { padding: 12px 14px; border-radius: 18px; background: rgba(255,255,255,.74); border: 1px solid rgba(39,48,71,.08); }
.hero-stats span, .log-summary span, .agent-meta span { display: block; font-size: 12px; color: rgba(39,48,71,.55); } .hero-stats strong, .log-summary strong, .agent-meta strong { display: block; margin-top: 4px; color: var(--ink); }
.agent-list { display: flex; flex-direction: column; gap: 12px; max-height: calc(100vh - 280px); overflow: auto; padding-right: 4px; }
.agent-card, .mini-card { border: 1px solid rgba(39,48,71,.08); border-radius: 22px; background: linear-gradient(180deg, #fff 0%, #fffaf1 100%); }
.agent-card { padding: 16px; text-align: left; transition: transform .16s ease, box-shadow .16s ease, border-color .16s ease; }
.agent-card:hover, .agent-card.active, .run-card.active { transform: translateY(-2px); border-color: rgba(217,119,6,.28); box-shadow: 0 12px 24px rgba(217,119,6,.12); }
.duck-badge { --cover: #d97706; width: 42px; height: 42px; background: linear-gradient(135deg, var(--cover), #ffe17c); }
.agent-meta { display: flex; justify-content: space-between; gap: 10px; margin-top: 6px; }
.empty-state { display: flex; min-height: 160px; flex-direction: column; align-items: center; justify-content: center; gap: 10px; border: 1px dashed rgba(39,48,71,.16); border-radius: 24px; background: rgba(255,250,241,.72); color: rgba(39,48,71,.62); text-align: center; padding: 20px; }
.empty-state.small { min-height: 120px; }
.hero-editor { --cover: #d97706; background: radial-gradient(circle at top left, rgba(255,225,124,.38), transparent 26%), radial-gradient(circle at 80% 12%, rgba(31,159,159,.18), transparent 28%), linear-gradient(135deg, rgba(255,255,255,.96), rgba(255,248,231,.96)); }
.hero-editor__icon { width: 104px; height: 104px; background: linear-gradient(135deg, rgba(255,255,255,.96), color-mix(in srgb, var(--cover) 36%, white)); border: 1px solid rgba(39,48,71,.08); }
.section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.section-head h3 { margin: 4px 0 0; font-size: 20px; font-weight: 800; color: var(--ink); }
.grid { display: grid; gap: 14px; } .grid.two { grid-template-columns: repeat(2, minmax(0,1fr)); } .grid.three { grid-template-columns: repeat(3, minmax(0,1fr)); }
.color-input { width: 52px; height: 40px; border: 1px solid rgba(39,48,71,.12); border-radius: 12px; background: transparent; padding: 2px; }
.tool-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0,1fr)); }
.mini-card { padding: 14px; } .mini-card.active { border-color: rgba(31,159,159,.28); box-shadow: 0 10px 24px rgba(31,159,159,.1); }
.workflow-board { display: grid; gap: 14px; } .workflow-nodes { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(230px,1fr)); padding: 12px; border-radius: 24px; background: linear-gradient(rgba(39,48,71,.05) 1px, transparent 1px), linear-gradient(90deg, rgba(39,48,71,.05) 1px, transparent 1px), #fffaf1; background-size: 18px 18px; }
.edge-box { padding: 16px; border-radius: 24px; border: 1px dashed rgba(39,48,71,.14); background: rgba(255,255,255,.84); } .edge-row { display: grid; grid-template-columns: minmax(0,1fr) auto minmax(0,1fr) auto; gap: 10px; align-items: center; } .empty-inline { font-size: 13px; color: rgba(39,48,71,.58); margin-top: 10px; }
.accent { background: radial-gradient(circle at top right, rgba(255,212,77,.28), transparent 30%), linear-gradient(180deg, #fffdf7 0%, #fff 100%); }
.run-card { text-align: left; }
.payload-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 10px; } .payload-grid label { display: block; margin-bottom: 6px; font-size: 12px; font-weight: 700; color: rgba(39,48,71,.58); } .payload-grid pre, .monospace :deep(textarea) { font-family: 'Consolas', 'SFMono-Regular', monospace; } .payload-grid pre { min-height: 108px; margin: 0; padding: 10px; border-radius: 16px; background: #f8fafc; white-space: pre-wrap; word-break: break-word; font-size: 12px; color: rgba(39,48,71,.72); }
.muted { color: rgba(39,48,71,.6); } .muted.sm { font-size: 13px; } .muted.xs { font-size: 12px; } .truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .block { display: block; } .error { color: #dc2626; }
@media (max-width: 1500px) { .agent-workbench { grid-template-columns: minmax(260px,300px) minmax(0,1fr); } .runtime { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); align-items: start; } }
@media (max-width: 1120px) { .agent-workbench, .runtime, .grid.two, .grid.three, .tool-grid, .hero-stats, .log-summary, .payload-grid, .edge-row { grid-template-columns: 1fr; } .hero-main, .section-head, .row.between { align-items: flex-start; } }
</style>
