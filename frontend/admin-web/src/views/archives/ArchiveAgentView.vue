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
        <el-button v-if="canCreate" link type="primary" class="mt-xs" @click="startCreateSupplierLedgerAgent">
          从供应商明细账模板新建
        </el-button>
        <div class="stack gap-sm mt-md">
          <el-input v-model="filters.keyword" clearable placeholder="搜索 Agent" @keyup.enter="loadAgents()">
            <template #append><el-button :icon="Search" @click="loadAgents()" /></template>
          </el-input>
          <el-select v-model="filters.status" clearable placeholder="状态筛选" @change="loadAgents()">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.tag" clearable filterable v-bind="globalFilterableSelectProps" placeholder="标签筛选">
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
            <div v-if="agent.tags.length" class="row wrap gap-xs mt-xs compact-tags">
              <el-tag v-for="tag in agent.tags.slice(0, 3)" :key="`${agent.id}-${tag}`" size="small" effect="plain">{{ tag }}</el-tag>
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
            <AgentShareSubmitButton :agent-id="editableAgent.id" :versions="editableAgent.versions" :latest-version-no="editableAgent.latestVersionNo" :published-version-no="editableAgent.publishedVersionNo" />
            <el-button type="danger" plain :icon="Delete" :loading="deleteLoading" :disabled="!editableAgent.id || !canDelete" @click="deleteAgent">删除</el-button>
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
            <el-select v-model="editableAgent.tags" multiple filterable v-bind="globalFilterableSelectProps" allow-create default-first-option placeholder="输入并回车创建标签">
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
          <el-form-item label="模型提供商">
            <el-select v-model="editableAgent.modelConfig.provider" placeholder="选择 Provider" @change="handleProviderChange">
              <el-option v-for="item in providerOptions" :key="item.code" :label="item.label" :value="item.code" :disabled="item.available === false" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型名称">
            <el-select v-model="editableAgent.modelConfig.model" filterable allow-create default-first-option placeholder="选择或输入模型名称">
              <el-option v-for="item in currentModelOptions" :key="item.model" :label="item.label" :value="item.model" :disabled="item.available === false" />
            </el-select>
          </el-form-item>
          <el-form-item label="凭据引用（可选）"><el-input v-model="editableAgent.modelConfig.credentialCode" placeholder="预留字段；DeepSeek 当前使用平台环境变量" /></el-form-item>
          <el-form-item label="温度"><el-input-number v-model="editableAgent.modelConfig.temperature" :min="0" :max="2" :step="0.1" /></el-form-item>
          <el-form-item label="最大 Token"><el-input-number v-model="editableAgent.modelConfig.maxTokens" :min="128" :max="8192" :step="128" /></el-form-item>
          <el-form-item label="超时（秒）"><el-input-number v-model="editableAgent.modelConfig.timeoutSeconds" :min="5" :max="300" :step="5" /></el-form-item>
        </div>
      </section>
      <section class="section workflow-entry-card" role="button" tabindex="0"
        @click="openToolConfig" @keydown.enter.prevent="openToolConfig">
        <div class="section-head">
          <div><div class="eyebrow">Tools</div><h3>工具配置</h3></div>
          <el-button type="primary" :icon="Tools" :disabled="!editableAgent.id" @click.stop="openToolConfig">进入配置</el-button>
        </div>
        <div class="workflow-entry-copy">
          <strong>管理受控工具与单工具测试</strong>
          <p>查看工具用途、内部入口和所需权限；绑定保存后可填写参数测试运行。</p>
          <el-tag size="small" effect="plain">已绑定 {{ enabledToolOptions.length }} 项受控工具</el-tag>
        </div>
      </section>

      <section
        class="section workflow-entry-card"
        role="button"
        tabindex="0"
        @click="openWorkflowCanvas"
        @keydown.enter.prevent="openWorkflowCanvas"
      >
        <div class="section-head">
          <div><div class="eyebrow">Workflow</div><h3>流程编排</h3></div>
          <el-button type="primary" :icon="Connection" :disabled="!editableAgent.id" @click.stop="openWorkflowCanvas">进入画布</el-button>
        </div>
        <div class="workflow-entry-body">
          <div class="workflow-entry-visual">
            <span class="flow-dot start">Start</span>
            <span class="flow-line" />
            <span class="flow-dot tool">Tool</span>
            <span class="flow-line" />
            <span class="flow-dot llm">LLM</span>
            <span class="flow-line" />
            <span class="flow-dot end">End</span>
          </div>
          <div class="workflow-entry-copy">
            <strong>打开 Dify / Coze 风格低代码画布</strong>
            <p>拖动节点、拖拽连线，并在右侧属性面板配置查询模板、变量映射、脱敏、模型和还原节点。</p>
            <div class="row wrap gap-xs">
              <el-tag size="small" effect="plain">{{ editableAgent.workflow.nodes.length }} 个节点</el-tag>
              <el-tag size="small" effect="plain">{{ editableAgent.workflow.edges.length }} 条连线</el-tag>
              <el-tag size="small" type="warning" effect="plain">保存后发布生效</el-tag>
            </div>
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
        <el-form-item label="触发来源"><el-tag type="warning" effect="plain">工作台测试运行</el-tag></el-form-item>
        <el-form-item label="测试入参 JSON"><el-input v-model="runForm.inputPayloadText" class="monospace" type="textarea" :rows="8" placeholder='{"query":"帮我整理待报销事项"}' /></el-form-item>
        <div v-if="runPreflight" class="run-clarification">
          <el-alert :title="runPreflight.message" :type="runPreflight.mode === 'NO_CAPABILITY' ? 'warning' : 'info'" :closable="false" show-icon />
          <template v-if="runPreflight.mode === 'CLARIFY'">
            <el-select v-if="runPreflight.companyOptions.length" v-model="selectedRunCompanyId" class="mt-sm" filterable placeholder="选择有权使用的账套">
              <el-option v-for="company in runPreflight.companyOptions" :key="company.companyId" :label="`${company.companyName}（${company.companyId}）`" :value="company.companyId" />
            </el-select>
            <el-select v-else-if="runPreflight.workflowOptions.length" v-model="selectedRunVersionId" class="mt-sm" placeholder="选择工作流">
              <el-option v-for="workflow in runPreflight.workflowOptions" :key="workflow.versionId" :label="workflow.name" :value="workflow.versionId" />
            </el-select>
            <el-select v-else-if="runPreflight.supplierOptions?.length" v-model="selectedRunSupplierCode" class="mt-sm"
              filterable placeholder="选择供应商">
              <el-option v-for="supplier in runPreflight.supplierOptions" :key="supplier.code"
                :label="`${supplier.name}（${supplier.code}）`" :value="supplier.code" />
            </el-select>
            <el-input v-else v-model="runClarificationAnswer" class="mt-sm" placeholder="补充查询条件后继续" />
            <el-button class="mt-sm" type="primary" :loading="runLoading" @click="runAgentNow">继续分析并执行</el-button>
          </template>
        </div>
        <div class="row between wrap gap-sm mt-sm">
          <el-button :icon="RefreshRight" @click="resetRunInput">重置入参</el-button>
          <el-button type="primary" :icon="VideoPlay" :loading="runLoading" :disabled="!editableAgent.id || !canRun" @click="runAgentNow">立即执行</el-button>
        </div>
      </section>

      <section class="panel-section">
        <div class="section-head"><div><div class="eyebrow">Runs</div><h3>最近运行记录</h3></div><el-tag size="small" effect="plain">{{ recentRunRecords.length }} 条</el-tag></div>
        <div v-if="runPermissionMessage" class="empty-state small"><p>{{ runPermissionMessage }}</p></div>
        <div v-else-if="runsLoading" class="empty-state small"><p>正在加载运行记录...</p></div>
        <div v-else-if="runRecords.length" class="stack gap-sm">
          <button v-for="run in recentRunRecords" :key="run.id" type="button" class="mini-card run-card" :class="{ active: selectedRunId === run.id }" :aria-label="`查看运行记录 ${run.runNo} 的步骤日志`" @click="openRunDetail(run.id)" @keydown.enter="openRunDetail(run.id)">
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

    </aside>

    <el-drawer v-model="runDetailDrawerVisible" direction="rtl" size="min(920px, 92vw)" destroy-on-close>
      <template #header>
        <div class="drawer-heading">
          <div><div class="eyebrow">Logs</div><h3>步骤日志</h3></div>
          <el-tag size="small" effect="plain">{{ currentRunDetail?.steps.length || 0 }} steps</el-tag>
        </div>
      </template>
      <AgentRunDetailPanel :detail="currentRunDetail" :loading="runDetailLoading" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Clock, Connection, Delete, DocumentCopy, FolderDelete, Plus, RefreshRight, Search, SwitchButton, Tools, VideoPlay } from '@element-plus/icons-vue'
import PixelDuckBotIcon from '@/components/icons/PixelDuckBotIcon.vue'
import { archiveAgentApi, type ArchiveAgentMeta, type ArchiveAgentRunDetail, type ArchiveAgentRunPreflight, type ArchiveAgentRunRecord, type ArchiveAgentSavePayload, type ArchiveAgentSummary } from '@/api'
import AgentShareSubmitButton from './components/AgentShareSubmitButton.vue'
import AgentRunDetailPanel from './components/AgentRunDetailPanel.vue'
import { hasAnyPermission, readStoredUser } from '@/utils/permissions'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import { readAgentRunDraft, removeAgentRunDraft, writeAgentRunDraft, type AgentRunDraft } from './agentRunDraft'
import { buildEditorSavePayload, createDefaultAgent, detailToEditable, supplierLedgerAgentTemplate, type EditableAgent, type NodeModel, type TriggerModel } from './archiveAgentEditor'

type ModelOption = { provider: string; model: string; label: string; available?: boolean }
type OptionItem = { code: string; label: string; available?: boolean; category?: string }
const router = useRouter()
const route = useRoute()
const filters = ref({ keyword: '', status: '', tag: '' })
const meta = ref<ArchiveAgentMeta | null>(null)
const listLoading = ref(false)
const saveLoading = ref(false)
const publishLoading = ref(false)
const deleteLoading = ref(false)
const runLoading = ref(false)
const runsLoading = ref(false)
const runDetailLoading = ref(false)
const agentList = ref<ArchiveAgentSummary[]>([])
const selectedAgentId = ref<number>()
const runRecords = ref<ArchiveAgentRunRecord[]>([])
const selectedRunId = ref<number>()
const currentRunDetail = ref<ArchiveAgentRunDetail | null>(null)
const runDetailDrawerVisible = ref(false)
const runPermissionMessage = ref('')
const inputSchemaText = ref('')
const savedEditorSnapshot = ref('')
const storedUser = readStoredUser()
const runDraftUserKey = String(storedUser?.userId ?? storedUser?.username ?? 'anonymous')
const runForm = ref<AgentRunDraft>(defaultRunDraft())
const runPreflight = ref<ArchiveAgentRunPreflight | null>(null)
const selectedRunCompanyId = ref('')
const selectedRunVersionId = ref<number>()
const selectedRunSupplierCode = ref('')
const selectedRunSupplierCodes = ref<Record<string, string>>({})
const runClarificationAnswer = ref('')
const permissionCodes = ref(storedUser?.permissionCodes || [])
const canCreate = computed(() => hasAnyPermission(['agent:workbench:create', 'agents:create'], permissionCodes.value))
const canEdit = computed(() => hasAnyPermission(['agent:workbench:edit', 'agents:edit'], permissionCodes.value))
const canDelete = computed(() => hasAnyPermission(['agent:workbench:delete', 'agents:delete'], permissionCodes.value))
const canRun = computed(() => hasAnyPermission(['agent:workbench:run', 'agents:run'], permissionCodes.value))
const canPublish = computed(() => hasAnyPermission(['agent:workbench:publish', 'agents:publish'], permissionCodes.value))
const canViewLogs = computed(() => hasAnyPermission(['agent:workbench:view_logs', 'agents:view_logs'], permissionCodes.value))
const canSave = computed(() => (editableAgent.value.id ? canEdit.value : canCreate.value))
const statusOptions = [{ label: '待命', value: 'READY' }, { label: '草稿', value: 'DRAFT' }, { label: '停用', value: 'DISABLED' }, { label: '归档', value: 'ARCHIVED' }]
const editableAgent = ref<EditableAgent>(createDefaultAgent())
const filteredAgents = computed(() => filters.value.tag ? agentList.value.filter((item) => item.tags.includes(filters.value.tag)) : agentList.value)
const recentRunRecords = computed(() => runRecords.value.slice(0, 5))
const availableTags = computed(() => Array.from(new Set(agentList.value.flatMap((item) => item.tags))))
const runningAgentCount = computed(() => agentList.value.filter((item) => item.runtimeStatus === 'RUNNING').length)
const failedAgentCount = computed(() => agentList.value.filter((item) => item.runtimeStatus === 'FAILED').length)
const providerOptions = computed(() => toOptions(meta.value?.modelProviders, 'code', 'label'))
const currentModelOptions = computed<ModelOption[]>(() => {
  const provider = editableAgent.value.modelConfig.provider
  return (meta.value?.modelOptions || [])
    .map((item) => ({
      provider: stringFrom(item.provider),
      model: stringFrom(item.model),
      label: stringFrom(item.label, stringFrom(item.model)),
      available: Boolean(item.available ?? true)
    }))
    .filter((item) => item.provider === provider && item.model)
})
const iconOptions = computed(() => toOptions(meta.value?.iconOptions, 'code', 'label'))
const themeOptions = computed(() => toOptions(meta.value?.themeOptions, 'code', 'label'))
const nodeTypeOptions = computed(() => toOptions(meta.value?.nodeTypes, 'code', 'label'))
const enabledToolOptions = computed(() => editableAgent.value.tools
  .filter((item) => item.enabled !== false && (meta.value?.tools || []).some((tool) => tool.toolCode === item.toolCode))
  .map((item) => ({ toolCode: item.toolCode, label: item.label || item.toolCode })))

onMounted(async () => {
  await loadMeta()
  await loadAgents(false)
})

watch(runForm, () => persistCurrentRunDraft(), { deep: true })
watch(() => runForm.value.inputPayloadText, () => {
  runPreflight.value = null
  selectedRunCompanyId.value = ''
  selectedRunVersionId.value = undefined
  selectedRunSupplierCode.value = ''
  selectedRunSupplierCodes.value = {}
  runClarificationAnswer.value = ''
})
watch(selectedRunCompanyId, () => {
  selectedRunSupplierCode.value = ''
  selectedRunSupplierCodes.value = {}
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
    const returnId = Number(route.query.agentId)
    const targetId = keepSelection && selectedAgentId.value && agentList.value.some((item) => item.id === selectedAgentId.value)
      ? selectedAgentId.value : agentList.value.find((item) => item.id === returnId)?.id || agentList.value[0]?.id
    if (targetId) await selectAgent(targetId)
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 列表加载失败')
  } finally {
    listLoading.value = false
  }
}

async function selectAgent(id: number) {
  persistCurrentRunDraft()
  runPreflight.value = null
  selectedRunCompanyId.value = ''
  selectedRunVersionId.value = undefined
  selectedRunSupplierCode.value = ''
  selectedRunSupplierCodes.value = {}
  runClarificationAnswer.value = ''
  selectedAgentId.value = id
  try {
    const res = await archiveAgentApi.getDetail(id)
    editableAgent.value = detailToEditable(res.data, meta.value, defaultNodeConfig)
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    savedEditorSnapshot.value = editorSnapshot()
    runForm.value = readAgentRunDraft(sessionStorage, runDraftUserKey, id, defaultRunDraft())
    currentRunDetail.value = null
    runDetailDrawerVisible.value = false
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
  runDetailDrawerVisible.value = false
  editableAgent.value = createDefaultAgent(meta.value)
  inputSchemaText.value = prettyJson(defaultInputSchema())
  savedEditorSnapshot.value = editorSnapshot()
  runForm.value = defaultRunDraft()
  runRecords.value = []
  runPermissionMessage.value = canViewLogs.value ? '' : '当前账号没有查看运行日志的权限。'
}

function startCreateSupplierLedgerAgent() {
  startCreateAgent()
  editableAgent.value = supplierLedgerAgentTemplate(editableAgent.value, meta.value)
  inputSchemaText.value = prettyJson({ type: 'object', properties: {
    query: { type: 'string', description: '供应商往来明细查询问题' }
  } })
  savedEditorSnapshot.value = editorSnapshot()
}

async function saveAgent() {
  const payload = buildSavePayload()
  if (!payload) return
  const runDraft = { ...runForm.value }
  saveLoading.value = true
  try {
    const isUpdate = Boolean(editableAgent.value.id)
    const res = isUpdate ? await archiveAgentApi.update(editableAgent.value.id!, payload) : await archiveAgentApi.create(payload)
    editableAgent.value = detailToEditable(res.data, meta.value, defaultNodeConfig)
    selectedAgentId.value = res.data.id
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    savedEditorSnapshot.value = editorSnapshot()
    writeAgentRunDraft(sessionStorage, runDraftUserKey, res.data.id, runDraft)
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
    editableAgent.value = detailToEditable(res.data, meta.value, defaultNodeConfig)
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
    editableAgent.value = detailToEditable(res.data, meta.value, defaultNodeConfig)
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
    editableAgent.value = detailToEditable(res.data, meta.value, defaultNodeConfig)
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
    editableAgent.value = detailToEditable(res.data, meta.value, defaultNodeConfig)
    inputSchemaText.value = prettyJson(res.data.inputSchema)
    await loadAgents(true)
    ElMessage.success('Agent 已归档')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 归档失败')
  }
}

async function deleteAgent() {
  const agentId = editableAgent.value.id
  if (!agentId) return ElMessage.warning('请先选择一个已保存的 Agent')
  try {
    await ElMessageBox.confirm(
      `删除“${editableAgent.value.agentName}”后，版本、触发器、运行记录和步骤日志都将一并删除，且无法恢复。是否继续？`,
      '删除 Agent',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  deleteLoading.value = true
  try {
    await archiveAgentApi.remove(agentId)
    removeAgentRunDraft(sessionStorage, runDraftUserKey, agentId)
    selectedAgentId.value = undefined
    runDetailDrawerVisible.value = false
    await loadAgents(false)
    ElMessage.success('Agent 及其关联数据已删除')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent 删除失败')
  } finally {
    deleteLoading.value = false
  }
}

async function runAgentNow() {
  if (!editableAgent.value.id) return ElMessage.warning('请先保存 Agent 再试跑')
  const inputPayload = parseJson(runForm.value.inputPayloadText, '测试入参 JSON 不合法')
  if (!inputPayload) return
  if (runPreflight.value?.mode === 'CLARIFY') {
    if (runPreflight.value.companyOptions.length && !selectedRunCompanyId.value) return ElMessage.warning('请选择账套')
    if (runPreflight.value.workflowOptions.length && !selectedRunVersionId.value) return ElMessage.warning('请选择工作流')
    if (runPreflight.value.supplierOptions?.length && !selectedRunSupplierCode.value) return ElMessage.warning('请选择供应商')
    if (!runPreflight.value.companyOptions.length && !runPreflight.value.workflowOptions.length
      && !runPreflight.value.supplierOptions?.length && !runClarificationAnswer.value.trim())
      return ElMessage.warning('请补充查询条件')
  }
  const chosenSuppliers = { ...selectedRunSupplierCodes.value }
  if (runPreflight.value?.supplierNodeKey && selectedRunSupplierCode.value)
    chosenSuppliers[runPreflight.value.supplierNodeKey] = selectedRunSupplierCode.value
  runLoading.value = true
  try {
    const prepared = await archiveAgentApi.prepareRun(editableAgent.value.id, { inputPayload: {
      ...inputPayload,
      ...(selectedRunCompanyId.value ? { selectedCompanyId: selectedRunCompanyId.value } : {}),
      ...(selectedRunVersionId.value ? { selectedVersionId: selectedRunVersionId.value } : {}),
      ...(Object.keys(chosenSuppliers).length ? { selectedSupplierCodes: chosenSuppliers } : {}),
      ...(runClarificationAnswer.value.trim() ? { clarificationAnswer: runClarificationAnswer.value.trim() } : {})
    } })
    const plan = prepared.data
    if (plan.mode !== 'READY') {
      runPreflight.value = plan
      selectedRunSupplierCodes.value = chosenSuppliers
      selectedRunSupplierCode.value = plan.supplierNodeKey ? chosenSuppliers[plan.supplierNodeKey] || '' : ''
      if (plan.supplierNodeKey && plan.versionId) selectedRunVersionId.value = plan.versionId
      if (plan.mode === 'NO_CAPABILITY') ElMessage.warning(plan.message)
      return
    }
    if (!plan.agentId || !plan.versionId) throw new Error('工作台任务分析结果缺少工作流版本')
    const targetAgentId = plan.agentId
    const switched = targetAgentId !== editableAgent.value.id
    const res = await archiveAgentApi.run(targetAgentId, {
      expectedVersionId: plan.versionId,
      usePublishedVersion: plan.usePublishedVersion,
      inputPayload: { ...inputPayload, parameters: plan.parameters,
        ...(runClarificationAnswer.value.trim() ? { context: `${String(inputPayload.context || '')}\n${runClarificationAnswer.value.trim()}` } : {}) }
    })
    runPreflight.value = null
    selectedRunCompanyId.value = ''
    selectedRunVersionId.value = undefined
    selectedRunSupplierCode.value = ''
    selectedRunSupplierCodes.value = {}
    runClarificationAnswer.value = ''
    if (switched) {
      ElMessage.info(`已改用 ${plan.agentName} 的已发布版本执行`)
      await selectAgent(targetAgentId)
    }
    ElMessage.success(`运行请求已提交：${res.data.runNo}`)
    await loadRuns(targetAgentId)
    await waitForRunCompletion(res.data.id)
    await loadRuns(targetAgentId)
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
  } catch (error: any) {
    runPermissionMessage.value = error?.message || '运行日志加载失败'
  } finally {
    runsLoading.value = false
  }
}

async function openRunDetail(runId: number) {
  runDetailDrawerVisible.value = true
  await loadRunDetail(runId)
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

async function waitForRunCompletion(runId: number) {
  selectedRunId.value = runId
  for (let attempt = 0; attempt < 90; attempt += 1) {
    const res = await archiveAgentApi.getRunDetail(runId)
    currentRunDetail.value = res.data
    if (['SUCCESS', 'FAILED', 'COMPLETED'].includes(res.data.status)) return
    await delay(1000)
  }
  ElMessage.warning('流程仍在后台运行，可稍后从运行记录中刷新查看')
}

function handleProviderChange() {
  const provider = editableAgent.value.modelConfig.provider
  const providerMeta = (meta.value?.modelProviders || []).find((item) => stringFrom(item.code) === provider)
  const defaultModel = stringFrom(providerMeta?.defaultModel)
  const firstAvailableModel = currentModelOptions.value.find((item) => item.available !== false)?.model || currentModelOptions.value[0]?.model || ''
  editableAgent.value.modelConfig.model = defaultModel || firstAvailableModel || editableAgent.value.modelConfig.model
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
function resetRunInput() { runForm.value = defaultRunDraft(); runPreflight.value = null; selectedRunCompanyId.value = ''; selectedRunVersionId.value = undefined; selectedRunSupplierCode.value = ''; selectedRunSupplierCodes.value = {}; runClarificationAnswer.value = '' }
function openWorkflowCanvas() {
  if (!editableAgent.value.id) {
    ElMessage.warning('请先保存 Agent，再进入流程编排画布')
    return
  }
  router.push({ name: 'agent-workflow-canvas', params: { id: editableAgent.value.id } })
}
async function openToolConfig() {
  if (!editableAgent.value.id) return ElMessage.warning('请先保存 Agent，再进入工具配置')
  if (savedEditorSnapshot.value !== editorSnapshot()) {
    try {
      await ElMessageBox.confirm('当前页面有未保存的修改。请先保存，或放弃修改后进入工具配置。',
        '未保存的修改', { confirmButtonText: '放弃修改并进入', cancelButtonText: '返回保存', type: 'warning' })
    } catch { return }
  }
  router.push({ name: 'agent-tool-config', params: { id: editableAgent.value.id } })
}
function editorSnapshot() { return JSON.stringify({ agent: editableAgent.value, inputSchema: inputSchemaText.value }) }

function buildSavePayload(nameOverride?: string): ArchiveAgentSavePayload | null {
  const inputSchema = parseJson(inputSchemaText.value, '输入 Schema JSON 不合法')
  if (!inputSchema) return null
  const payload = buildEditorSavePayload(editableAgent.value, inputSchema, nameOverride)
  if (!payload) ElMessage.warning('Agent 名称不能为空')
  return payload
}
function buildNode(nodeType: NodeModel['nodeType'], nodeKey?: string, label?: string): NodeModel { return { nodeKey: nodeKey || `${nodeType}_${Date.now()}_${Math.random().toString(36).slice(2, 5)}`, nodeType, label: label || defaultNodeLabel(nodeType), config: defaultNodeConfig(nodeType) } }
function buildTrigger(triggerType: TriggerModel['triggerType']): TriggerModel { if (triggerType === 'SCHEDULE') return { triggerType, enabled: false, scheduleMode: 'INTERVAL', intervalMinutes: 60 }; if (triggerType === 'EVENT') return { triggerType, enabled: false, eventCode: '' }; return { triggerType, enabled: true } }
function defaultNodeLabel(nodeType: NodeModel['nodeType']) { return ({ start: '开始', llm: 'LLM 节点', condition: '条件判断', tool: '工具调用', transform: '变量转换', notify: '发送通知', end: '结束' } as Record<string, string>)[nodeType] || '新节点' }
function defaultNodeConfig(nodeType: NodeModel['nodeType']) { if (nodeType === 'llm') return { promptTemplate: '' }; if (nodeType === 'condition') return { expression: '' }; if (nodeType === 'tool') return { toolCode: enabledToolOptions.value[0]?.toolCode || '', queryTemplateCode: 'expense.accessible_expenses.v2', variableMapping: '{"scope":"{{input.scope}}","keyword":"{{input.keyword}}","status":"{{input.status}}","startDate":"{{input.startDate}}","endDate":"{{input.endDate}}","minAmount":"{{input.minAmount}}","maxAmount":"{{input.maxAmount}}","limit":"{{input.limit}}"}' }; if (nodeType === 'transform') return { template: '' }; if (nodeType === 'notify') return { message: '' }; return {} }
function defaultInputSchema() { return { type: 'object', properties: { query: { type: 'string', description: '本次想让 Agent 处理的问题', default: '总结我当前待处理的报销事项' }, context: { type: 'string', description: '补充上下文', default: '优先关注待审批和待付款单据' }, scope: { type: 'string', enum: ['ASSIGNED_TO_ME', 'SUBMITTED_BY_ME'], default: 'ASSIGNED_TO_ME' }, keyword: { type: ['string', 'null'], default: null }, status: { type: ['string', 'null'], default: null }, startDate: { type: ['string', 'null'], default: null }, endDate: { type: ['string', 'null'], default: null }, minAmount: { type: ['number', 'null'], default: null }, maxAmount: { type: ['number', 'null'], default: null }, limit: { type: 'integer', minimum: 1, maximum: 50, default: 10 } } } }
function defaultRunDraft(): AgentRunDraft { return { inputPayloadText: defaultRunInputText() } }
function defaultRunInputText() { return prettyJson({ query: '' }) }
function persistCurrentRunDraft() { if (selectedAgentId.value) writeAgentRunDraft(sessionStorage, runDraftUserKey, selectedAgentId.value, runForm.value) }
function triggerLabel(triggerType: string) { return ({ MANUAL: '手动触发', SCHEDULE: '定时触发', EVENT: '事件触发' } as Record<string, string>)[triggerType] || triggerType }
function runtimeStatusLabel(status?: string) { return ({ READY: '待命', RUNNING: '运行中', FAILED: '异常', DISABLED: '已停用', ARCHIVED: '已归档', DRAFT: '草稿' } as Record<string, string>)[status || 'DRAFT'] || '草稿' }
function summaryStatusType(status?: string) { return ({ READY: 'success', RUNNING: 'warning', FAILED: 'danger', DISABLED: 'info', ARCHIVED: 'info', DRAFT: 'info' } as Record<string, string>)[status || 'DRAFT'] || 'info' }
function runStatusType(status?: string) { return ({ SUCCESS: 'success', COMPLETED: 'success', RUNNING: 'warning', PENDING: 'warning', FAILED: 'danger' } as Record<string, string>)[status || ''] || 'info' }
function isProtectedNode(nodeType: string) { return nodeType === 'start' || nodeType === 'end' }
function prettyJson(value: unknown) { try { return JSON.stringify(value ?? {}, null, 2) } catch { return '{}' } }
function parseJson(text: string, message: string) { try { return JSON.parse(text || '{}') as Record<string, unknown> } catch { ElMessage.warning(message); return null } }
function toOptions(list: Array<Record<string, unknown>> | undefined, codeKey: string, labelKey: string): OptionItem[] { return (list || []).map((item) => ({ code: stringFrom(item[codeKey]), label: stringFrom(item[labelKey]), available: Boolean(item.available ?? true), category: stringFrom(item.category) })) }
function normalizeText(value?: string | null) { const text = String(value || '').trim(); return text || undefined }
function stringFrom(value: unknown, fallback = '') { return typeof value === 'string' ? value : fallback }
function delay(milliseconds: number) { return new Promise<void>((resolve) => window.setTimeout(resolve, milliseconds)) }
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
.agent-list { display: flex; flex-direction: column; align-items: stretch; gap: 8px; max-height: calc(100vh - 280px); overflow: auto; padding-right: 4px; }
.agent-card, .mini-card { border: 1px solid rgba(39,48,71,.08); border-radius: 22px; background: linear-gradient(180deg, #fff 0%, #fffaf1 100%); }
.agent-card { width: 100%; height: auto; min-height: 0; padding: 11px 12px; text-align: left; transition: transform .16s ease, box-shadow .16s ease, border-color .16s ease; }
.agent-card:hover, .agent-card.active, .run-card.active { transform: translateY(-2px); border-color: rgba(217,119,6,.28); box-shadow: 0 12px 24px rgba(217,119,6,.12); }
.duck-badge { --cover: #d97706; width: 36px; height: 36px; flex: 0 0 36px; background: linear-gradient(135deg, var(--cover), #ffe17c); }
.compact-tags { padding-left: 46px; }
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
.workflow-entry-card { cursor: pointer; background: radial-gradient(circle at top right, rgba(31,159,159,.18), transparent 36%), linear-gradient(135deg, rgba(255,212,77,.22), rgba(255,255,255,.98)); transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease, background .18s ease; }
.workflow-entry-card:hover, .workflow-entry-card:focus-visible { transform: translateY(-2px); border-color: rgba(31,159,159,.36); box-shadow: 0 18px 38px rgba(31,159,159,.16); background: radial-gradient(circle at top right, rgba(31,159,159,.25), transparent 38%), linear-gradient(135deg, rgba(255,212,77,.3), rgba(255,255,255,.98)); outline: none; }
.workflow-entry-body { display: grid; grid-template-columns: minmax(260px, .9fr) minmax(0, 1.1fr); gap: 16px; align-items: center; }
.workflow-entry-visual { min-height: 128px; display: flex; align-items: center; justify-content: center; padding: 18px; border-radius: 22px; background: linear-gradient(rgba(39,48,71,.05) 1px, transparent 1px), linear-gradient(90deg, rgba(39,48,71,.05) 1px, transparent 1px), rgba(255,255,255,.72); background-size: 18px 18px; overflow: hidden; }
.flow-dot { min-width: 58px; height: 40px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; font-size: 12px; font-weight: 800; color: #fff; box-shadow: 0 10px 24px rgba(39,48,71,.14); }
.flow-dot.start { background: #16a34a; } .flow-dot.tool { background: #0f766e; } .flow-dot.llm { background: #7c3aed; } .flow-dot.end { background: #334155; }
.flow-line { width: 54px; height: 2px; background: linear-gradient(90deg, rgba(39,48,71,.22), rgba(31,159,159,.5)); position: relative; }
.flow-line::after { content: ''; position: absolute; right: -3px; top: -4px; border-left: 7px solid rgba(31,159,159,.64); border-top: 5px solid transparent; border-bottom: 5px solid transparent; }
.workflow-entry-copy strong { display: block; font-size: 18px; color: var(--ink); }
.workflow-entry-copy p { margin: 8px 0 12px; color: rgba(39,48,71,.66); line-height: 1.7; }
.accent { background: radial-gradient(circle at top right, rgba(255,212,77,.28), transparent 30%), linear-gradient(180deg, #fffdf7 0%, #fff 100%); }
.run-card { width: 100%; cursor: pointer; text-align: left; transition: transform .16s ease, border-color .16s ease, box-shadow .16s ease, background .16s ease; }
.run-card:hover, .run-card:focus-visible { transform: translateY(-2px); border-color: rgba(31,159,159,.42); background: linear-gradient(180deg, #f0fdfa 0%, #fff7e8 100%); box-shadow: 0 12px 26px rgba(15,118,110,.14); outline: none; }
.drawer-heading { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-right: 12px; }
.drawer-heading h3 { margin: 4px 0 0; color: var(--ink); font-size: 20px; }
.payload-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 10px; } .payload-grid label { display: block; margin-bottom: 6px; font-size: 12px; font-weight: 700; color: rgba(39,48,71,.58); } .payload-grid pre, .monospace :deep(textarea) { font-family: 'Consolas', 'SFMono-Regular', monospace; } .payload-grid pre { min-height: 108px; margin: 0; padding: 10px; border-radius: 16px; background: #f8fafc; white-space: pre-wrap; word-break: break-word; font-size: 12px; color: rgba(39,48,71,.72); }
.muted { color: rgba(39,48,71,.6); } .muted.sm { font-size: 13px; } .muted.xs { font-size: 12px; } .truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .block { display: block; } .error { color: #dc2626; }
@media (max-width: 1500px) { .agent-workbench { grid-template-columns: minmax(260px,300px) minmax(0,1fr); } .runtime { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); align-items: start; } }
@media (max-width: 1120px) { .agent-workbench, .runtime, .grid.two, .grid.three, .tool-grid, .hero-stats, .log-summary, .payload-grid, .edge-row, .workflow-entry-body { grid-template-columns: 1fr; } .hero-main, .section-head, .row.between { align-items: flex-start; } }
</style>
