import { computed, type ComputedRef, type Ref } from 'vue'
import type {
  ProcessFlowMeta,
  ProcessFlowNode,
  ProcessFlowRoute,
  ProcessFlowSummary
} from '@/api'
import { buildFlowCanvasBlocks } from '@/views/process/processFlowDesignerHelper'

export function useProcessFlowDesignerShellState<TRoute extends ProcessFlowRoute>(params: {
  keyword: Ref<string>
  flows: Ref<ProcessFlowSummary[]>
  meta: Ref<ProcessFlowMeta | null>
  selectedNodeKey: Ref<string>
  selectedRouteKey: Ref<string>
  workingNodes: ComputedRef<ProcessFlowNode[]>
  workingRoutes: ComputedRef<TRoute[]>
  workingFlowName: ComputedRef<string>
  emptyMeta: () => ProcessFlowMeta
  updateSelectedRouteAttachBelowNodes: (enabled: boolean) => void
  sortRoutes: (routes: ProcessFlowRoute[]) => ProcessFlowRoute[]
  nodeTypeLabel: (nodeType: string) => string
  shouldForceManagerAndSign: (
    config?: Pick<ProcessFlowNode['config'], 'approverType' | 'managerConfig'>
  ) => boolean
}) {
  const {
    keyword,
    flows,
    meta,
    selectedNodeKey,
    selectedRouteKey,
    workingNodes,
    workingRoutes,
    workingFlowName,
    emptyMeta,
    updateSelectedRouteAttachBelowNodes,
    sortRoutes,
    nodeTypeLabel,
    shouldForceManagerAndSign
  } = params

  const metaOptions = computed<ProcessFlowMeta>(() => meta.value ?? emptyMeta())

  const filteredFlows = computed(() => {
    const text = keyword.value.trim().toLowerCase()
    return flows.value.filter((item) => {
      if (!text) {
        return true
      }
      return (
        item.flowName.toLowerCase().includes(text) ||
        (item.flowCode || '').toLowerCase().includes(text)
      )
    })
  })

  const canvasBlocks = computed(() =>
    buildFlowCanvasBlocks(workingNodes.value || [], workingRoutes.value || [])
  )

  const selectedNode = computed(() =>
    workingNodes.value.find((item) => item.nodeKey === selectedNodeKey.value)
  )

  const selectedRoute = computed<TRoute | undefined>(
    () => workingRoutes.value.find((item) => item.routeKey === selectedRouteKey.value) as
      | TRoute
      | undefined
  )

  const selectedRouteAttachBelowNodes = computed<boolean>({
    get: () => Boolean(selectedRoute.value?.attachBelowNodes),
    set: (value) => {
      updateSelectedRouteAttachBelowNodes(value)
    }
  })

  const selectedRouteBranchNode = computed(() => {
    if (!selectedRoute.value) {
      return undefined
    }
    return workingNodes.value.find((item) => item.nodeKey === selectedRoute.value?.sourceNodeKey)
  })

  const activeBranchNode = computed(() => {
    if (selectedRouteBranchNode.value) {
      return selectedRouteBranchNode.value
    }
    if (selectedNode.value?.nodeType === 'BRANCH') {
      return selectedNode.value
    }
    return undefined
  })

  const currentBranchRoutes = computed<TRoute[]>(() => {
    if (!activeBranchNode.value) {
      return []
    }
    return sortRoutes(
      workingRoutes.value.filter((item) => item.sourceNodeKey === activeBranchNode.value?.nodeKey)
    ) as TRoute[]
  })

  const currentFlowLabel = computed(() => workingFlowName.value.trim() || '未命名流程')
  const hasSelection = computed(() => Boolean(selectedNode.value || selectedRoute.value))

  const panelTitle = computed(() => {
    if (selectedRoute.value) {
      return selectedRoute.value.routeName || '条件分支'
    }
    if (selectedNode.value) {
      return selectedNode.value.nodeName
    }
    return '节点配置'
  })

  const panelDescription = computed(() => {
    if (selectedRoute.value) {
      return '当前正在编辑分支泳道的名称、条件和增删管理。'
    }
    if (selectedNode.value?.nodeType === 'BRANCH') {
      return '当前正在管理分支块的泳道结构，点击泳道卡片可进入条件设置。'
    }
    if (selectedNode.value) {
      return `当前正在编辑${nodeTypeLabel(selectedNode.value.nodeType)}`
    }
    return '点击中间流程图中的节点或条件头卡片后，在这里修改配置。'
  })

  const removeButtonLabel = computed(() => {
    if (selectedRoute.value) {
      return '删除当前分支'
    }
    if (selectedNode.value?.nodeType === 'BRANCH') {
      return '删除分支块'
    }
    return '删除节点'
  })

  const approvalApproverTypes = computed(() => metaOptions.value.approvalApproverTypeOptions)
  const approvalOpinionCandidates = computed(
    () => metaOptions.value.defaultApprovalOpinions || ['通过', '拒绝', '加签', '转交']
  )
  const isManagerMultiLevelApproval = computed(() =>
    shouldForceManagerAndSign(selectedNode.value?.config)
  )
  const managerApprovalHint = computed(() => {
    const managerLevel = Number(selectedNode.value?.config?.managerConfig?.managerLevel || 1)
    if (managerLevel > 1) {
      return `选择第 ${managerLevel} 级主管后，会按同一审批节点会签处理：自动包含第 1 到第 ${managerLevel} 级主管，所有命中的主管都审批通过后，当前节点才会通过。`
    }
    return '选择第 1 级主管时，仅第 1 级主管参与当前审批节点。'
  })

  return {
    metaOptions,
    hasSelection,
    filteredFlows,
    canvasBlocks,
    selectedNode,
    selectedRoute,
    selectedRouteAttachBelowNodes,
    selectedRouteBranchNode,
    activeBranchNode,
    currentBranchRoutes,
    currentFlowLabel,
    panelTitle,
    panelDescription,
    removeButtonLabel,
    approvalApproverTypes,
    approvalOpinionCandidates,
    isManagerMultiLevelApproval,
    managerApprovalHint
  }
}
