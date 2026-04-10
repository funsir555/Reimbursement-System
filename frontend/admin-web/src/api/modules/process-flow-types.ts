import type { ProcessFormOption } from './process-template-types'

export interface ProcessFlowConfigOption {
  value: string
  label: string
  description: string
}

export interface ProcessFlowConditionField {
  key: string
  label: string
  valueType: string
  operatorKeys: string[]
}

export interface ProcessFlowCondition {
  fieldKey: string
  operator: string
  compareValue: unknown
}

export interface ProcessFlowConditionGroup {
  groupNo: number
  conditions: ProcessFlowCondition[]
}

export interface ProcessFlowRoute {
  routeKey: string
  sourceNodeKey: string
  targetNodeKey?: string
  routeName: string
  priority: number
  defaultRoute: boolean
  conditionGroups: ProcessFlowConditionGroup[]
}

export interface ProcessFlowManagerConfig {
  ruleMode?: string
  deptSource?: string
  managerLevel?: number | string
  orgTreeLookupEnabled?: boolean
  orgTreeLookupLevel?: number | string
  formDeptManagerEnabled?: boolean
}

export interface ProcessFlowDesignatedMemberConfig {
  userIds?: unknown
}

export interface ProcessFlowManualSelectConfig {
  candidateScope?: string
}

export interface ProcessFlowNodeConfig {
  approverType?: string
  missingHandler?: string
  approvalMode?: string
  opinionDefaults?: string[]
  specialSettings?: string[]
  managerConfig: ProcessFlowManagerConfig
  designatedMemberConfig: ProcessFlowDesignatedMemberConfig
  manualSelectConfig: ProcessFlowManualSelectConfig
  receiverType?: string
  receiverUserIds?: unknown
  timing?: string
  executorType?: string
  executorUserIds?: unknown
  paymentAction?: string
  [key: string]: unknown
}

export interface ProcessFlowNode {
  nodeKey: string
  nodeType: string
  nodeName: string
  sceneId?: number
  parentNodeKey?: string
  displayOrder: number
  config: ProcessFlowNodeConfig
}

export interface ProcessFlowSummary {
  id: number
  flowCode: string
  flowName: string
  flowDescription?: string
  status: string
  statusLabel: string
  currentVersionNo?: number
  updatedAt: string
}

export interface ProcessFlowScene {
  id: number
  sceneCode: string
  sceneName: string
  sceneDescription?: string
  status: number
}

export interface ProcessFlowDetail {
  id?: number
  flowCode?: string
  flowName: string
  flowDescription?: string
  status: string
  statusLabel?: string
  editableVersionId?: number
  editableVersionNo?: number
  publishedVersionId?: number
  publishedVersionNo?: number
  hasDraftVersion?: boolean
  nodes: ProcessFlowNode[]
  routes: ProcessFlowRoute[]
}

export interface ProcessFlowMeta {
  nodeTypeOptions: ProcessFormOption[]
  sceneOptions: ProcessFlowScene[]
  approvalApproverTypeOptions: ProcessFormOption[]
  approvalManagerRuleModeOptions: ProcessFormOption[]
  approvalManagerDeptSourceOptions: ProcessFormOption[]
  approvalManagerLevelOptions: ProcessFormOption[]
  approvalManagerLookupLevelOptions: ProcessFormOption[]
  approvalManualCandidateScopeOptions: ProcessFormOption[]
  ccReceiverTypeOptions: ProcessFormOption[]
  paymentExecutorTypeOptions: ProcessFormOption[]
  missingHandlerOptions: ProcessFormOption[]
  approvalModeOptions: ProcessFormOption[]
  defaultApprovalOpinions: string[]
  approvalSpecialOptions: ProcessFlowConfigOption[]
  ccTimingOptions: ProcessFormOption[]
  ccSpecialOptions: ProcessFlowConfigOption[]
  paymentActionOptions: ProcessFormOption[]
  paymentSpecialOptions: ProcessFlowConfigOption[]
  branchOperatorOptions: ProcessFormOption[]
  branchConditionFields: ProcessFlowConditionField[]
  departmentOptions: ProcessFormOption[]
  userOptions: ProcessFormOption[]
  expenseTypeOptions: ProcessFormOption[]
  archiveOptions: ProcessFormOption[]
}

export interface ProcessFlowSavePayload {
  flowName: string
  flowDescription?: string
  nodes: ProcessFlowNode[]
  routes: ProcessFlowRoute[]
}

export interface ProcessFlowStatusPayload {
  status: string
}

export interface ProcessFlowResolveApproversPayload {
  flowId: number
  nodeKey: string
  context: Record<string, unknown>
}

export interface ProcessFlowResolvedUser {
  userId: number
  userName: string
  deptId?: number
  deptName?: string
}

export interface ProcessFlowResolveApproversResult {
  resolutionType: string
  nextAction?: string
  approverUserIds: number[]
  approverUsers: ProcessFlowResolvedUser[]
  trace: string[]
}

export interface ProcessFlowSceneSavePayload {
  sceneName: string
  sceneDescription?: string
  status?: number
}
