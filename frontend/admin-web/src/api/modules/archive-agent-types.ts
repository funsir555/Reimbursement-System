// 这里定义 archive-agent-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

export interface ArchiveAgentVersionRecord {
  id: number
  versionNo: number
  versionLabel?: string
  published: boolean
  createdByName?: string
  createdAt?: string
}

export interface ArchiveAgentTriggerConfig {
  triggerType: 'MANUAL' | 'SCHEDULE' | 'EVENT'
  enabled?: boolean
  scheduleMode?: 'CRON' | 'INTERVAL'
  cronExpression?: string
  intervalMinutes?: number
  eventCode?: string
}

export interface ArchiveAgentWorkflowNode {
  nodeKey: string
  nodeType: 'start' | 'llm' | 'condition' | 'tool' | 'transform' | 'notify' | 'end'
  label: string
  config?: Record<string, unknown>
}

export interface ArchiveAgentWorkflowEdge {
  source: string
  target: string
}

export interface ArchiveAgentToolDefinition {
  toolCode: string
  label: string
  category?: string
  available?: boolean
  enabled?: boolean
  credentialRefCode?: string
  url?: string
  title?: string
  content?: string
  [key: string]: unknown
}

export interface ArchiveAgentToolTestResult {
  toolCode: string
  queryTemplateCode: string
  summary: string
  scopeDescription: string
  totalCount?: number
  totalAmount?: number | string
  records: Array<Record<string, unknown>>
  evidenceRefs: string[]
  page?: number
  pageSize?: number
  hasMore?: boolean
}

// 这是 ArchiveAgentSummary 的数据结构。
export interface ArchiveAgentSummary {
  id: number
  agentCode: string
  agentName: string
  agentDescription?: string
  iconKey?: string
  themeKey?: string
  coverColor?: string
  tags: string[]
  status: 'DRAFT' | 'READY' | 'DISABLED' | 'ARCHIVED'
  latestVersionNo?: number
  publishedVersionNo?: number
  runtimeStatus?: 'DRAFT' | 'READY' | 'RUNNING' | 'FAILED' | 'DISABLED'
  lastRunStatus?: string
  lastRunSummary?: string
  lastRunAt?: string
  enabledTriggerCount?: number
}

export interface ArchiveAgentDetail extends ArchiveAgentSummary {
  promptConfig: Record<string, unknown>
  modelConfig: Record<string, unknown>
  tools: ArchiveAgentToolDefinition[]
  workflow: {
    nodes: ArchiveAgentWorkflowNode[]
    edges: ArchiveAgentWorkflowEdge[]
  }
  triggers: ArchiveAgentTriggerConfig[]
  inputSchema: Record<string, unknown>
  versions: ArchiveAgentVersionRecord[]
}

export interface ArchiveAgentSavePayload {
  agentName: string
  agentDescription?: string
  iconKey?: string
  themeKey?: string
  coverColor?: string
  tags: string[]
  promptConfig: Record<string, unknown>
  modelConfig: Record<string, unknown>
  tools: Array<Record<string, unknown>>
  workflow: {
    nodes: ArchiveAgentWorkflowNode[]
    edges: ArchiveAgentWorkflowEdge[]
  }
  triggers: Array<Record<string, unknown>>
  inputSchema: Record<string, unknown>
}

export interface ArchiveAgentRunRecord {
  id: number
  runNo: string
  agentId: number
  triggerType: string
  triggerSource?: string
  status: string
  summary?: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  durationMs?: number
}

export interface ArchiveAgentRunStepRecord {
  stepNo: number
  nodeKey: string
  nodeType: string
  nodeLabel?: string
  status: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  durationMs?: number
  inputPayload: Record<string, unknown>
  outputPayload: Record<string, unknown>
}

export interface ArchiveAgentRunDetail extends ArchiveAgentRunRecord {
  agentName?: string
  agentVersionNo?: number
  inputPayload: Record<string, unknown>
  outputPayload: Record<string, unknown>
  steps: ArchiveAgentRunStepRecord[]
  artifacts: Array<Record<string, unknown>>
}

export interface ArchiveAgentTestRunPayload {
  triggerSource?: string
  expectedVersionId?: number
  usePublishedVersion?: boolean
  inputPayload: Record<string, unknown>
}

export interface ArchiveAgentRunPreflight {
  mode: 'READY' | 'CLARIFY' | 'NO_CAPABILITY'
  message: string
  agentId?: number
  versionId?: number
  agentName?: string
  usePublishedVersion: boolean
  parameters: Record<string, Record<string, unknown>>
  companyOptions: Array<{ companyId: string; companyName: string }>
  workflowOptions: Array<{ agentId: number; versionId: number; name: string }>
  supplierNodeKey?: string | null
  supplierOptions?: Array<{ code: string; name: string }>
}

export interface ArchiveAgentMeta {
  modelProviders: Array<Record<string, unknown>>
  modelOptions: Array<Record<string, unknown>>
  tools: Array<Record<string, unknown>>
  nodeTypes: Array<Record<string, unknown>>
  triggerTypes: Array<Record<string, unknown>>
  iconOptions: Array<Record<string, unknown>>
  themeOptions: Array<Record<string, unknown>>
  defaultSystemPrompt: string
}
