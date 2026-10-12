import type { ArchiveAgentDetail, ArchiveAgentMeta, ArchiveAgentSavePayload, ArchiveAgentVersionRecord, ArchiveAgentWorkflowEdge, ArchiveAgentWorkflowNode } from '@/api'

export type TriggerModel = { triggerType: 'MANUAL' | 'SCHEDULE' | 'EVENT'; enabled: boolean; scheduleMode?: 'CRON' | 'INTERVAL'; cronExpression?: string; intervalMinutes?: number; eventCode?: string }
export type NodeModel = ArchiveAgentWorkflowNode & { config: Record<string, any> }
export type EditableAgent = {
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
  promptConfig: { systemPrompt: string; rolePrompt: string; outputSchema: string; guardrails: string }
  modelConfig: { provider: string; model: string; temperature: number; maxTokens: number; timeoutSeconds: number; credentialCode: string }
  tools: Array<{ toolCode: string; label?: string; category?: string; enabled?: boolean; [key: string]: unknown }>
  workflow: { nodes: NodeModel[]; edges: ArchiveAgentWorkflowEdge[] }
  triggers: TriggerModel[]
  versions: ArchiveAgentVersionRecord[]
}

const stringFrom = (value: unknown, fallback = '') => typeof value === 'string' ? value : fallback
const numberFrom = (value: unknown, fallback = 0) => typeof value === 'number' && !Number.isNaN(value) ? value : fallback
const normalizeText = (value?: string | null) => String(value || '').trim() || undefined
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T
const pruneEmpty = (source: Record<string, any>) => Object.fromEntries(Object.entries(source || {}).filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== ''))
const defaultWorkflow = () => ({
  nodes: [{ nodeKey: 'start_1', nodeType: 'start', label: '开始', config: {} },
    { nodeKey: 'llm_1', nodeType: 'llm', label: '意图分析', config: { promptTemplate: '' } },
    { nodeKey: 'end_1', nodeType: 'end', label: '结束', config: {} }] as NodeModel[],
  edges: [{ source: 'start_1', target: 'llm_1' }, { source: 'llm_1', target: 'end_1' }]
})
const defaultTriggers = (): TriggerModel[] => [
  { triggerType: 'MANUAL', enabled: true },
  { triggerType: 'SCHEDULE', enabled: false, scheduleMode: 'INTERVAL', intervalMinutes: 60 }
]

export function createDefaultAgent(sourceMeta?: ArchiveAgentMeta | null): EditableAgent {
  return {
    agentName: '我的自动化鸭子', agentDescription: '个人自动化 Agent，适合先从手动试跑和定时任务开始。',
    iconKey: 'pixel-duck', themeKey: 'duck-sunrise', coverColor: '#d97706', tags: ['个人', '自动化'],
    status: 'DRAFT', latestVersionNo: 1,
    promptConfig: { systemPrompt: sourceMeta?.defaultSystemPrompt || '你是一个个人自动化 Agent。优先输出结构化结论，并在需要时调用工具补充事实。', rolePrompt: '你只服务当前登录用户，不能越权查看其他人的数据。', outputSchema: '{"summary":"","actions":[]}', guardrails: '不泄露敏感信息；调用工具前先确认是否必要。' },
    modelConfig: { provider: 'MOCK', model: 'duck-agent-v1', temperature: 0.3, maxTokens: 1200, timeoutSeconds: 30, credentialCode: '' },
    tools: [{ toolCode: 'expense.query_my_expenses', label: '受控查询：报销单与待办', category: 'BUSINESS' }],
    workflow: defaultWorkflow(), triggers: defaultTriggers(), versions: []
  }
}

export function detailToEditable(detail: ArchiveAgentDetail, sourceMeta?: ArchiveAgentMeta | null,
  defaultNodeConfig: (nodeType: NodeModel['nodeType']) => Record<string, any> = () => ({})): EditableAgent {
  const fallbackWorkflow = defaultWorkflow()
  return {
    id: detail.id, agentCode: detail.agentCode, agentName: detail.agentName || '', agentDescription: detail.agentDescription || '',
    iconKey: stringFrom(detail.iconKey, 'pixel-duck'), themeKey: stringFrom(detail.themeKey, 'duck-sunrise'), coverColor: stringFrom(detail.coverColor, '#d97706'),
    tags: detail.tags || [], status: (detail.status as EditableAgent['status']) || 'DRAFT',
    latestVersionNo: detail.latestVersionNo, publishedVersionNo: detail.publishedVersionNo,
    promptConfig: { systemPrompt: stringFrom(detail.promptConfig?.systemPrompt, sourceMeta?.defaultSystemPrompt || ''), rolePrompt: stringFrom(detail.promptConfig?.rolePrompt), outputSchema: stringFrom(detail.promptConfig?.outputSchema), guardrails: stringFrom(detail.promptConfig?.guardrails) },
    modelConfig: { provider: stringFrom(detail.modelConfig?.provider, 'MOCK'), model: stringFrom(detail.modelConfig?.model, 'duck-agent-v1'), temperature: numberFrom(detail.modelConfig?.temperature, 0.3), maxTokens: numberFrom(detail.modelConfig?.maxTokens, 1200), timeoutSeconds: numberFrom(detail.modelConfig?.timeoutSeconds, 30), credentialCode: stringFrom(detail.modelConfig?.credentialCode) },
    tools: (detail.tools || []).map((item) => ({ ...item, toolCode: stringFrom(item.toolCode) })),
    workflow: { nodes: Array.isArray(detail.workflow?.nodes) ? detail.workflow.nodes.map((item) => ({ nodeKey: item.nodeKey, nodeType: item.nodeType, label: item.label, config: item.config ? clone(item.config) : defaultNodeConfig(item.nodeType) })) : fallbackWorkflow.nodes, edges: Array.isArray(detail.workflow?.edges) ? detail.workflow.edges.map((item) => ({ source: item.source, target: item.target })) : fallbackWorkflow.edges },
    triggers: Array.isArray(detail.triggers) && detail.triggers.length ? detail.triggers.map((item) => ({ triggerType: stringFrom(item.triggerType, 'MANUAL') as TriggerModel['triggerType'], enabled: item.enabled !== false, scheduleMode: stringFrom(item.scheduleMode, 'INTERVAL') as TriggerModel['scheduleMode'], cronExpression: stringFrom(item.cronExpression), intervalMinutes: numberFrom(item.intervalMinutes, 60), eventCode: stringFrom(item.eventCode) })) : defaultTriggers(),
    versions: detail.versions || []
  }
}

export function supplierLedgerAgentTemplate(base: EditableAgent, sourceMeta?: ArchiveAgentMeta | null): EditableAgent {
  const provider = sourceMeta?.modelProviders?.find(item => item.available !== false && String(item.code).toUpperCase() !== 'MOCK')
  const model = sourceMeta?.modelOptions?.find(item => item.provider === provider?.code && item.available !== false)
  return {
    ...base,
    agentName: '供应商往来明细查询', agentDescription: '按授权账套、供应商、期间和凭证范围查询供应商辅助核算明细账。',
    tags: ['财务', '供应商', '只读查询'],
    promptConfig: { systemPrompt: '仅基于已授权的供应商明细账结果解释，不推断未查询的期间或分页数据。', rolePrompt: '你是财务明细账分析助手，必须说明账套、供应商、期间和凭证范围。', outputSchema: '{"answer":"","evidenceRefs":[]}', guardrails: '不得生成 SQL，不得扩大权限，不得将本页明细称为全部明细。' },
    modelConfig: provider && model ? { ...base.modelConfig, provider: String(provider.code), model: String(model.model) } : base.modelConfig,
    tools: [{ toolCode: 'finance.query_supplier_detail_ledger', label: '受控查询：供应商明细账', category: 'BUSINESS', enabled: true }],
    workflow: {
      nodes: [{ nodeKey: 'start_1', nodeType: 'start', label: '开始', config: {} },
        { nodeKey: 'supplier_1', nodeType: 'tool', label: '供应商明细账', config: { toolCode: 'finance.query_supplier_detail_ledger', queryTemplateCode: 'finance.supplier_detail_ledger.v1', variableMapping: {} } },
        { nodeKey: 'llm_1', nodeType: 'llm', label: '结果解释', config: { promptTemplate: '' } },
        { nodeKey: 'end_1', nodeType: 'end', label: '结束', config: {} }] as NodeModel[],
      edges: [{ source: 'start_1', target: 'supplier_1' }, { source: 'supplier_1', target: 'llm_1' }, { source: 'llm_1', target: 'end_1' }]
    },
    triggers: [{ triggerType: 'MANUAL', enabled: true }]
  }
}

export function buildEditorSavePayload(agent: EditableAgent, inputSchema: Record<string, unknown>, nameOverride?: string): ArchiveAgentSavePayload | null {
  const agentName = normalizeText(nameOverride || agent.agentName)
  if (!agentName) return null
  return {
    agentName, agentDescription: normalizeText(agent.agentDescription), iconKey: normalizeText(agent.iconKey),
    themeKey: normalizeText(agent.themeKey), coverColor: normalizeText(agent.coverColor),
    tags: agent.tags.filter((item) => normalizeText(item)).map((item) => item.trim()),
    promptConfig: { ...agent.promptConfig },
    modelConfig: { ...agent.modelConfig, credentialCode: normalizeText(agent.modelConfig.credentialCode) },
    tools: agent.tools.map((item) => ({ ...item, enabled: item.enabled !== false })),
    workflow: { nodes: agent.workflow.nodes.map((item) => ({ nodeKey: item.nodeKey, nodeType: item.nodeType, label: item.label, config: pruneEmpty(item.config) })), edges: agent.workflow.edges.map((item) => ({ source: item.source, target: item.target })) },
    triggers: agent.triggers.map((item) => ({ triggerType: item.triggerType, enabled: item.enabled, scheduleMode: item.scheduleMode, cronExpression: normalizeText(item.cronExpression), intervalMinutes: item.intervalMinutes, eventCode: normalizeText(item.eventCode) })),
    inputSchema
  }
}
