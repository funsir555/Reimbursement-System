import { computed, ref, type ComputedRef, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  ProcessFlowCondition,
  ProcessFlowConditionField,
  ProcessFlowConditionGroup,
  ProcessFlowDetail,
  ProcessFlowMeta,
  ProcessFlowNode,
  ProcessFlowRoute,
  ProcessFlowSavePayload,
  ProcessFormOption
} from '@/api'
import {
  appendRouteToBranch,
  buildDefaultBranchRoutes,
  copyNodeIntoContainer,
  insertNodeIntoContainer,
  moveNodeIntoContainer,
  normalizeContainerKey,
  reindexFlowState,
  removeNodeAndDescendants,
  removeRouteLane,
  type FlowContainerKey,
  type FlowInsertType,
  type FlowMoveResult
} from '@/views/process/processFlowDesignerHelper'
import { normalizeDesignatedMemberValues } from '@/views/process/processFlowDesignatedMembers'
import { validateFlowPayload } from '@/views/process/pmValidation'

type EditableProcessFlowCondition = Omit<ProcessFlowCondition, 'compareValue'> & {
  compareValue: any
}

type EditableProcessFlowConditionGroup = Omit<ProcessFlowConditionGroup, 'conditions'> & {
  conditions: EditableProcessFlowCondition[]
}

type EditableProcessFlowRoute = Omit<ProcessFlowRoute, 'conditionGroups'> & {
  conditionGroups: EditableProcessFlowConditionGroup[]
}

type SelectionPreference = {
  nodeKey?: string
  routeKey?: string
}

type InsertCommand = {
  containerKey: FlowContainerKey
  index: number
  nodeType: FlowInsertType
}

type CanvasDropTarget = {
  containerKey: FlowContainerKey
  index: number
  blockKey: string
}

type CanvasDragMode = 'move' | 'copy'

type CanvasDragStartPayload = {
  nodeKey: string
  mode: CanvasDragMode
}

type CanvasDropOutcome = 'copied' | 'moved' | 'ignored' | 'cancelled'

export function useProcessFlowDesignerGraphState(params: {
  working: ProcessFlowDetail
  metaOptions: ComputedRef<ProcessFlowMeta>
  selectedNodeKey: Ref<string>
  selectedRouteKey: Ref<string>
  draggingNodeKey: Ref<string>
  dropTargetKey: Ref<string>
  createEmptyFlow: () => ProcessFlowDetail
}) {
  const {
    working,
    metaOptions,
    selectedNodeKey,
    selectedRouteKey,
    draggingNodeKey,
    dropTargetKey,
    createEmptyFlow
  } = params

  const workingNodes = computed(() => working.nodes || [])
  const workingRoutes = computed(() => (working.routes || []) as EditableProcessFlowRoute[])
  const draggingNodeMode = ref<CanvasDragMode>('move')
  const selectedNode = computed(() =>
    workingNodes.value.find((item) => item.nodeKey === selectedNodeKey.value)
  )
  const selectedRoute = computed<EditableProcessFlowRoute | undefined>(
    () => workingRoutes.value.find((item) => item.routeKey === selectedRouteKey.value)
  )
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

  function shouldForceManagerAndSign(config?: Pick<ProcessFlowNode['config'], 'approverType' | 'managerConfig'>) {
    if (!config || config.approverType !== 'MANAGER') {
      return false
    }
    return Number(config.managerConfig?.managerLevel || 1) > 1
  }

  function shouldForceApprovalAndSign(
    config?: Pick<ProcessFlowNode['config'], 'approverType' | 'managerConfig'>
  ) {
    if (!config) {
      return false
    }
    if (config.approverType === 'DESIGNATED_USER_GROUP') {
      return true
    }
    return shouldForceManagerAndSign(config)
  }

  function isApprovalStyleAssigneeType(value: unknown) {
    return ['DESIGNATED_MEMBER', 'DESIGNATED_USER_GROUP', 'MANUAL_SELECT'].includes(String(value || '').trim())
  }

  function normalizeApprovalStyleAssigneeType(primary: unknown, legacyFallback?: unknown) {
    const primaryValue = String(primary || '').trim()
    if (isApprovalStyleAssigneeType(primaryValue)) {
      return primaryValue
    }
    const legacyValue = String(legacyFallback || '').trim()
    return legacyValue === 'DESIGNATED_MEMBER' ? 'DESIGNATED_MEMBER' : undefined
  }

  function defaultApprovalOpinions(source: unknown) {
    return Array.isArray(source) && source.length ? source : ['通过', '拒绝', '加签', '转交']
  }

  function resolvePaymentDefaultSceneId() {
    return metaOptions.value.sceneOptions.find((item) => item.sceneName === '出纳支付')?.id
  }

  function sortRoutes(routes: ProcessFlowRoute[]) {
    return [...routes].sort((left, right) => {
      const priorityGap = (left.priority ?? 0) - (right.priority ?? 0)
      if (priorityGap !== 0) {
        return priorityGap
      }
      return left.routeKey.localeCompare(right.routeKey)
    })
  }

  function nodeTypeLabel(nodeType: string) {
    switch (nodeType) {
      case 'CC':
        return '抄送节点'
      case 'PAYMENT':
        return '支付节点'
      case 'BRANCH':
        return '流程分支'
      default:
        return '审批节点'
    }
  }

  const currentBranchRoutes = computed<EditableProcessFlowRoute[]>(() => {
    if (!activeBranchNode.value) {
      return []
    }
    return sortRoutes(
      workingRoutes.value.filter((item) => item.sourceNodeKey === activeBranchNode.value?.nodeKey)
    ) as EditableProcessFlowRoute[]
  })

  function clearSelection() {
    selectedNodeKey.value = ''
    selectedRouteKey.value = ''
  }

  function cloneValue<T>(value: T): T {
    if (value === undefined || value === null) {
      return value
    }
    return JSON.parse(JSON.stringify(value))
  }

  function createBaseNodeConfig() {
    return {
      managerConfig: {} as ProcessFlowNode['config']['managerConfig'],
      designatedMemberConfig: {
        userIds: []
      } as ProcessFlowNode['config']['designatedMemberConfig'],
      designatedUserGroupConfig: {
        groupId: undefined
      } as NonNullable<ProcessFlowNode['config']['designatedUserGroupConfig']>,
      manualSelectConfig: {
        candidateScope: 'ALL_ACTIVE_USERS'
      } as ProcessFlowNode['config']['manualSelectConfig']
    }
  }

  function normalizeNumberArray(source: unknown): number[] {
    if (!Array.isArray(source)) {
      return []
    }
    return source.map((item) => Number(item)).filter((item) => Number.isFinite(item))
  }

  function normalizeOptionalNumber(source: unknown) {
    if (source === undefined || source === null || source === '') {
      return undefined
    }
    const numeric = Number(source)
    return Number.isFinite(numeric) ? numeric : undefined
  }

  function isOptionValueType(valueType?: string) {
    return ['company', 'department', 'user', 'expenseType', 'archive'].includes(valueType || '')
  }

  function isMultiOperator(operator: string) {
    return operator === 'IN' || operator === 'NOT_IN'
  }

  function isBetweenOperator(operator: string) {
    return operator === 'BETWEEN'
  }

  function getConditionField(fieldKey?: string): ProcessFlowConditionField | undefined {
    return metaOptions.value.branchConditionFields.find((item) => item.key === fieldKey)
  }

  function operatorOptionsForField(fieldKey?: string) {
    const field = getConditionField(fieldKey)
    if (!field) {
      return metaOptions.value.branchOperatorOptions
    }
    return metaOptions.value.branchOperatorOptions.filter((item) => field.operatorKeys.includes(item.value))
  }

  function normalizeScalarCompareValue(value: unknown, valueType?: string) {
    if (valueType === 'number') {
      if (value === undefined || value === null || value === '') {
        return null
      }
      const numeric = Number(value)
      return Number.isFinite(numeric) ? numeric : null
    }

    if (isOptionValueType(valueType)) {
      if (value === undefined || value === null || value === '') {
        return ''
      }
      return String(value)
    }

    if (value === undefined || value === null) {
      return ''
    }
    return String(value)
  }

  function normalizeConditionCompareValue(value: unknown, valueType?: string, operator?: string) {
    if (operator === 'BETWEEN') {
      const source = Array.isArray(value) ? value : []
      return [
        normalizeScalarCompareValue(source[0], valueType),
        normalizeScalarCompareValue(source[1], valueType)
      ]
    }

    if (isMultiOperator(operator || '')) {
      const source = Array.isArray(value) ? value : value === undefined || value === null || value === '' ? [] : [value]
      return source.map((item) => normalizeScalarCompareValue(item, valueType))
    }

    return normalizeScalarCompareValue(value, valueType)
  }

  function normalizeCondition(source?: ProcessFlowCondition): EditableProcessFlowCondition {
    const fallbackField = metaOptions.value.branchConditionFields[0]
    const field = getConditionField(source?.fieldKey) || fallbackField
    const fieldKey = source?.fieldKey || field?.key || ''
    const operatorOptions = operatorOptionsForField(fieldKey)
    const operator = source?.operator && operatorOptions.some((item) => item.value === source.operator)
      ? source.operator
      : operatorOptions[0]?.value || 'EQ'

    return {
      fieldKey,
      operator,
      compareValue: normalizeConditionCompareValue(source?.compareValue, getConditionField(fieldKey)?.valueType, operator)
    }
  }

  function createEmptyCondition(): EditableProcessFlowCondition {
    return normalizeCondition({
      fieldKey: metaOptions.value.branchConditionFields[0]?.key || '',
      operator: metaOptions.value.branchConditionFields[0]?.operatorKeys?.[0] || 'EQ',
      compareValue: undefined
    })
  }

  function normalizeConditionGroups(source: ProcessFlowConditionGroup[]): EditableProcessFlowConditionGroup[] {
    return (source || []).map((group, index) => ({
      groupNo: group.groupNo ?? index + 1,
      conditions: (group.conditions || []).map((condition) => normalizeCondition(condition))
    }))
  }

  function normalizeRoute(route: ProcessFlowRoute, index: number): EditableProcessFlowRoute {
    return {
      ...cloneValue(route),
      routeName: route.routeName || `分支 ${index + 1}`,
      priority: route.priority ?? index + 1,
      defaultRoute: Boolean(route.defaultRoute),
      attachBelowNodes: Boolean(route.attachBelowNodes),
      conditionGroups: normalizeConditionGroups(route.conditionGroups || [])
    }
  }

  function normalizeNode(node: ProcessFlowNode, index: number): ProcessFlowNode {
    const baseConfig = createBaseNodeConfig()
    const normalized: ProcessFlowNode = {
      ...cloneValue(node),
      displayOrder: node.displayOrder ?? index + 1,
      config: {
        ...baseConfig,
        ...cloneValue(node.config || {}),
        managerConfig: {
          ...baseConfig.managerConfig,
          ...cloneValue(node.config?.managerConfig || {})
        },
        designatedMemberConfig: {
          ...baseConfig.designatedMemberConfig,
          ...cloneValue(node.config?.designatedMemberConfig || {})
        },
        designatedUserGroupConfig: {
          ...baseConfig.designatedUserGroupConfig,
          ...cloneValue(node.config?.designatedUserGroupConfig || {})
        },
        manualSelectConfig: {
          ...baseConfig.manualSelectConfig,
          ...cloneValue(node.config?.manualSelectConfig || {})
        }
      }
    }

    if (normalized.nodeType === 'APPROVAL') {
      const managerConfig = cloneValue(normalized.config.managerConfig || {})
      const approvalMode = shouldForceApprovalAndSign({
        approverType: normalized.config.approverType || 'MANAGER',
        managerConfig
      })
        ? 'AND_SIGN'
        : (normalized.config.approvalMode || 'OR_SIGN')
      normalized.config = {
        approverType: normalized.config.approverType || 'MANAGER',
        missingHandler: normalized.config.missingHandler === 'MANUAL_SELECT_ON_SUBMIT'
          ? 'BLOCK_SUBMIT'
          : (normalized.config.missingHandler || 'AUTO_SKIP'),
        approvalMode,
        opinionDefaults: Array.isArray(normalized.config.opinionDefaults) && normalized.config.opinionDefaults.length
          ? normalized.config.opinionDefaults
          : ['通过', '拒绝', '加签', '转交'],
        specialSettings: Array.isArray(normalized.config.specialSettings) ? normalized.config.specialSettings : [],
        managerConfig: {
          ruleMode: managerConfig.ruleMode || 'FORM_DEPT_MANAGER',
          deptSource: managerConfig.deptSource || 'UNDERTAKE_DEPT',
          managerLevel: Number(managerConfig.managerLevel || 1),
          orgTreeLookupEnabled: managerConfig.orgTreeLookupEnabled ?? true,
          orgTreeLookupLevel: Number(managerConfig.orgTreeLookupLevel || 1)
        },
        designatedMemberConfig: {
          userIds: normalizeDesignatedMemberValues(normalized.config.designatedMemberConfig?.userIds)
        },
        designatedUserGroupConfig: {
          groupId: normalizeOptionalNumber(normalized.config.designatedUserGroupConfig?.groupId)
        },
        manualSelectConfig: {
          candidateScope: normalized.config.manualSelectConfig?.candidateScope || 'ALL_ACTIVE_USERS'
        }
      }
      return normalized
    }

    if (normalized.nodeType === 'CC') {
      const approverType = normalizeApprovalStyleAssigneeType(
        normalized.config.approverType,
        normalized.config.receiverType
      )
      normalized.config = {
        ...createBaseNodeConfig(),
        approverType,
        missingHandler: normalized.config.missingHandler || 'AUTO_SKIP',
        designatedMemberConfig: {
          userIds: normalizeDesignatedMemberValues(
            normalized.config.designatedMemberConfig?.userIds
            ?? (normalized.config.receiverType === 'DESIGNATED_MEMBER' ? normalized.config.receiverUserIds : [])
          )
        },
        designatedUserGroupConfig: {
          groupId: normalizeOptionalNumber(normalized.config.designatedUserGroupConfig?.groupId)
        },
        manualSelectConfig: {
          candidateScope: normalized.config.manualSelectConfig?.candidateScope || 'ALL_ACTIVE_USERS'
        },
        receiverType: normalized.config.receiverType || 'DESIGNATED_MEMBER',
        receiverUserIds: normalizeNumberArray(normalized.config.receiverUserIds),
        timing: normalized.config.timing || 'ON_ENTER',
        specialSettings: Array.isArray(normalized.config.specialSettings) ? normalized.config.specialSettings : []
      }
      return normalized
    }

    if (normalized.nodeType === 'PAYMENT') {
      const approverType = normalizeApprovalStyleAssigneeType(
        normalized.config.approverType,
        normalized.config.executorType
      )
      const approvalMode = approverType === 'DESIGNATED_USER_GROUP'
        ? 'AND_SIGN'
        : (normalized.config.approvalMode || 'OR_SIGN')
      normalized.config = {
        ...createBaseNodeConfig(),
        approverType,
        missingHandler: normalized.config.missingHandler || 'AUTO_SKIP',
        approvalMode,
        opinionDefaults: defaultApprovalOpinions(normalized.config.opinionDefaults),
        specialSettings: Array.isArray(normalized.config.specialSettings) ? normalized.config.specialSettings : [],
        designatedMemberConfig: {
          userIds: normalizeDesignatedMemberValues(
            normalized.config.designatedMemberConfig?.userIds
            ?? (normalized.config.executorType === 'DESIGNATED_MEMBER' ? normalized.config.executorUserIds : [])
          )
        },
        designatedUserGroupConfig: {
          groupId: normalizeOptionalNumber(normalized.config.designatedUserGroupConfig?.groupId)
        },
        manualSelectConfig: {
          candidateScope: normalized.config.manualSelectConfig?.candidateScope || 'ALL_ACTIVE_USERS'
        },
        executorType: normalized.config.executorType || 'DESIGNATED_MEMBER',
        executorUserIds: normalizeNumberArray(normalized.config.executorUserIds),
        paymentAction: normalized.config.paymentAction || 'GENERATE_PAYMENT'
      }
      return normalized
    }

    normalized.config = {
      ...normalized.config
    }
    return normalized
  }

  function prepareGraph(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[]) {
    const normalizedNodes = (nodes || []).map((node, index) => normalizeNode(node, index))
    const normalizedRoutes = (routes || []).map((route, index) => normalizeRoute(route, index))
    const snapshot = reindexFlowState(normalizedNodes, normalizedRoutes)
    return {
      nodes: snapshot.nodes.map((node, index) => normalizeNode(node, index)),
      routes: snapshot.routes.map((route, index) => normalizeRoute(route, index))
    }
  }

  function normalizeFlowDetail(detail: ProcessFlowDetail): ProcessFlowDetail {
    const graph = prepareGraph(detail.nodes || [], detail.routes || [])
    return {
      ...createEmptyFlow(),
      ...cloneValue(detail),
      nodes: graph.nodes,
      routes: graph.routes
    }
  }

  function assignCopiedFlow(detail: ProcessFlowDetail) {
    const graph = prepareGraph(detail.nodes || [], detail.routes || [])
    const sourceName = (detail.flowName || '').trim()
    Object.assign(working, {
      ...createEmptyFlow(),
      flowName: sourceName ? `${sourceName}-副本` : '审批流程-副本',
      flowDescription: detail.flowDescription || '',
      nodes: graph.nodes,
      routes: graph.routes
    })
    restoreSelection()
  }

  function applyWorkingGraph(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[], preferred: SelectionPreference = {}) {
    const graph = prepareGraph(nodes, routes)
    working.nodes = graph.nodes
    working.routes = graph.routes
    restoreSelection(preferred)
  }

  function resolveSelectionPreference(preferred: SelectionPreference): SelectionPreference | null {
    if (preferred.routeKey && working.routes.some((item) => item.routeKey === preferred.routeKey)) {
      return {
        routeKey: preferred.routeKey
      }
    }

    if (!preferred.nodeKey) {
      return null
    }

    const node = working.nodes.find((item) => item.nodeKey === preferred.nodeKey)
    if (!node) {
      return null
    }

    if (node.nodeType === 'BRANCH') {
      const firstRoute = getFirstRouteForBranch(node.nodeKey)
      if (firstRoute) {
        return {
          routeKey: firstRoute.routeKey
        }
      }
    }

    return {
      nodeKey: node.nodeKey
    }
  }

  function applySelection(selection: SelectionPreference) {
    if (selection.routeKey) {
      selectedRouteKey.value = selection.routeKey
      selectedNodeKey.value = ''
      return
    }
    selectedNodeKey.value = selection.nodeKey || ''
    selectedRouteKey.value = ''
  }

  function restoreSelection(preferred: SelectionPreference = {}) {
    const preferredSelection = resolveSelectionPreference(preferred)
    if (preferredSelection) {
      applySelection(preferredSelection)
      return
    }

    const currentRouteSelection = resolveSelectionPreference({ routeKey: selectedRouteKey.value })
    if (currentRouteSelection) {
      applySelection(currentRouteSelection)
      return
    }

    const currentNodeSelection = resolveSelectionPreference({ nodeKey: selectedNodeKey.value })
    if (currentNodeSelection) {
      applySelection(currentNodeSelection)
      return
    }

    const initialSelection = findFirstVisibleSelection()
    if (initialSelection) {
      applySelection(initialSelection)
      return
    }

    clearSelection()
  }

  function findFirstVisibleSelection(): SelectionPreference | null {
    const firstRootNode = listNodesInContainer(working.nodes || [], null)[0]
    if (!firstRootNode) {
      return null
    }
    return resolveSelectionPreference({ nodeKey: firstRootNode.nodeKey })
  }

  function selectNode(nodeKey: string) {
    const preferredSelection = resolveSelectionPreference({ nodeKey })
    if (preferredSelection) {
      applySelection(preferredSelection)
      return
    }
    selectedNodeKey.value = nodeKey
    selectedRouteKey.value = ''
  }

  function selectRoute(routeKey: string) {
    selectedRouteKey.value = routeKey
    selectedNodeKey.value = ''
  }

  function hasOpenMessageBox() {
    return typeof document !== 'undefined' && Boolean(document.querySelector('.el-message-box'))
  }

  function isEditingElement(target: EventTarget | null) {
    if (!(target instanceof HTMLElement)) {
      return false
    }
    if (target.isContentEditable) {
      return true
    }
    const editableParent = target.closest('input, textarea, select, [contenteditable="true"]')
    return Boolean(editableParent)
  }

  function handleCanvasDragStart(payload: CanvasDragStartPayload) {
    draggingNodeKey.value = payload.nodeKey
    draggingNodeMode.value = payload.mode
    dropTargetKey.value = ''
  }

  function handleCanvasDragEnd() {
    draggingNodeKey.value = ''
    draggingNodeMode.value = 'move'
    dropTargetKey.value = ''
  }

  function handleCanvasDragOver(payload: CanvasDropTarget) {
    if (!draggingNodeKey.value) {
      return
    }
    dropTargetKey.value = payload.blockKey
  }

  function handleMoveFailure(moveResult: FlowMoveResult) {
    if (moveResult.reason === 'INVALID_TARGET') {
      ElMessage.warning('不能将当前分支移动到自己的内部位置')
    }
  }

  async function handleCanvasDrop(payload: CanvasDropTarget): Promise<CanvasDropOutcome> {
    const currentNodeKey = draggingNodeKey.value
    const currentDragMode = draggingNodeMode.value
    handleCanvasDragEnd()
    if (!currentNodeKey) {
      return 'ignored'
    }

    if (currentDragMode === 'copy') {
      const copyResult = copyNodeIntoContainer(
        working.nodes || [],
        currentNodeKey,
        payload.containerKey,
        payload.index
      )
      if (!copyResult.copied || !copyResult.copiedNodeKey) {
        return 'ignored'
      }
      applyWorkingGraph(copyResult.nodes, working.routes || [], { nodeKey: copyResult.copiedNodeKey })
      ElMessage.success('节点副本已添加')
      return 'copied'
    }

    const moveResult = moveNodeIntoContainer(
      working.nodes || [],
      working.routes || [],
      currentNodeKey,
      payload.containerKey,
      payload.index
    )
    if (!moveResult.moved) {
      handleMoveFailure(moveResult)
      return 'ignored'
    }

    try {
      await ElMessageBox.confirm(
        '确定将当前节点移动到这个位置吗？',
        '调整节点位置',
        {
          type: 'warning',
          confirmButtonText: '确认修改',
          cancelButtonText: '取消'
        }
      )
    } catch {
      return 'cancelled'
    }

    applyWorkingGraph(moveResult.nodes, moveResult.routes, { nodeKey: currentNodeKey })
    ElMessage.success('节点位置已调整')
    return 'moved'
  }

  function buildNodeByType(nodeType: FlowInsertType, index: number): ProcessFlowNode {
    const stamp = `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
    const order = index + 1

    if (nodeType === 'CC') {
      return {
        nodeKey: `cc-${stamp}`,
        nodeType: 'CC',
        nodeName: `抄送节点 ${order}`,
        sceneId: undefined,
        parentNodeKey: '',
        displayOrder: order,
        config: {
          ...createBaseNodeConfig(),
          approverType: 'DESIGNATED_MEMBER',
          receiverType: 'DESIGNATED_MEMBER',
          receiverUserIds: [],
          designatedMemberConfig: {
            userIds: []
          },
          designatedUserGroupConfig: {
            groupId: undefined
          },
          manualSelectConfig: {
            candidateScope: 'ALL_ACTIVE_USERS'
          },
          timing: 'ON_ENTER',
          missingHandler: 'AUTO_SKIP',
          specialSettings: []
        }
      }
    }

    if (nodeType === 'PAYMENT') {
      return {
        nodeKey: `payment-${stamp}`,
        nodeType: 'PAYMENT',
        nodeName: `支付节点 ${order}`,
        sceneId: resolvePaymentDefaultSceneId(),
        parentNodeKey: '',
        displayOrder: order,
        config: {
          ...createBaseNodeConfig(),
          approverType: 'DESIGNATED_MEMBER',
          executorType: 'DESIGNATED_MEMBER',
          executorUserIds: [],
          designatedMemberConfig: {
            userIds: []
          },
          designatedUserGroupConfig: {
            groupId: undefined
          },
          manualSelectConfig: {
            candidateScope: 'ALL_ACTIVE_USERS'
          },
          paymentAction: 'GENERATE_PAYMENT',
          missingHandler: 'AUTO_SKIP',
          approvalMode: 'OR_SIGN',
          opinionDefaults: ['通过', '拒绝', '加签', '转交'],
          specialSettings: []
        }
      }
    }

    if (nodeType === 'BRANCH') {
      return {
        nodeKey: `branch-${stamp}`,
        nodeType: 'BRANCH',
        nodeName: `流程分支 ${order}`,
        sceneId: undefined,
        parentNodeKey: '',
        displayOrder: order,
        config: createBaseNodeConfig()
      }
    }

    return {
      nodeKey: `approval-${stamp}`,
      nodeType: 'APPROVAL',
      nodeName: `审批节点 ${order}`,
      sceneId: undefined,
      parentNodeKey: '',
      displayOrder: order,
      config: {
        ...createBaseNodeConfig(),
        approverType: 'MANAGER',
        missingHandler: 'AUTO_SKIP',
        approvalMode: 'OR_SIGN',
        opinionDefaults: ['通过', '拒绝', '加签', '转交'],
        specialSettings: [],
        managerConfig: {
          ruleMode: 'FORM_DEPT_MANAGER',
          deptSource: 'UNDERTAKE_DEPT',
          managerLevel: 1,
          orgTreeLookupEnabled: true,
          orgTreeLookupLevel: 1
        },
        designatedMemberConfig: {
          userIds: []
        },
        designatedUserGroupConfig: {
          groupId: undefined
        },
        manualSelectConfig: {
          candidateScope: 'ALL_ACTIVE_USERS'
        }
      }
    }
  }

  function handleCanvasInsert(payload: InsertCommand) {
    const node = buildNodeByType(payload.nodeType, payload.index)
    const inserted = insertNodeIntoContainer(working.nodes || [], node, payload.containerKey, payload.index)
    let nextRoutes = cloneValue(working.routes || [])
    let preferredSelection: SelectionPreference = { nodeKey: node.nodeKey }

    if (payload.nodeType === 'BRANCH') {
      const createdRoutes = buildDefaultBranchRoutes(node.nodeKey)
      nextRoutes = [...nextRoutes, ...createdRoutes]
      preferredSelection = createdRoutes[0]
        ? { routeKey: createdRoutes[0].routeKey }
        : { nodeKey: node.nodeKey }
    }

    applyWorkingGraph(inserted.nodes, nextRoutes, preferredSelection)
  }

  function addRouteLane(branchNodeKey: string) {
    const existingKeys = new Set((working.routes || []).map((item) => item.routeKey))
    const nextRoutes = appendRouteToBranch(working.routes || [], branchNodeKey)
    const addedRoute = nextRoutes.find((item) => item.sourceNodeKey === branchNodeKey && !existingKeys.has(item.routeKey))
    applyWorkingGraph(working.nodes || [], nextRoutes, addedRoute ? { routeKey: addedRoute.routeKey } : { nodeKey: branchNodeKey })
  }

  function updateSelectedRouteAttachBelowNodes(enabled: boolean) {
    if (!selectedRoute.value) {
      return
    }
    const routeKey = selectedRoute.value.routeKey
    const sourceNodeKey = selectedRoute.value.sourceNodeKey
    const nextRoutes = (working.routes || []).map((item) => {
      if (item.sourceNodeKey !== sourceNodeKey) {
        return cloneValue(item)
      }
      if (item.routeKey === routeKey) {
        return {
          ...cloneValue(item),
          attachBelowNodes: enabled
        }
      }
      if (!enabled) {
        return cloneValue(item)
      }
      return {
        ...cloneValue(item),
        attachBelowNodes: false
      }
    })
    applyWorkingGraph(working.nodes || [], nextRoutes, { routeKey })
  }

  function updateSelectedRouteDefaultRoute(enabled: boolean) {
    if (!selectedRoute.value) {
      return
    }
    const routeKey = selectedRoute.value.routeKey
    const sourceNodeKey = selectedRoute.value.sourceNodeKey
    const nextRoutes = (working.routes || []).map((item) => {
      if (item.sourceNodeKey !== sourceNodeKey) {
        return cloneValue(item)
      }
      if (item.routeKey === routeKey) {
        return {
          ...cloneValue(item),
          defaultRoute: enabled,
          conditionGroups: enabled ? [] : cloneValue(item.conditionGroups || [])
        }
      }
      if (!enabled) {
        return cloneValue(item)
      }
      return {
        ...cloneValue(item),
        defaultRoute: false
      }
    })
    applyWorkingGraph(working.nodes || [], nextRoutes, { routeKey })
  }

  async function removeSelectedItem() {
    if (selectedRoute.value) {
      await removeSelectedRoute()
      return
    }
    if (selectedNode.value) {
      await removeSelectedNode()
    }
  }

  async function removeSelectedRoute() {
    if (!selectedRoute.value) {
      return
    }
    if (currentBranchRoutes.value.length <= 2) {
      ElMessage.warning('流程分支至少保留 2 条泳道')
      return
    }

    try {
      await ElMessageBox.confirm('删除当前分支后，该泳道下的全部节点和嵌套分支都会一并删除。确定继续吗？', '删除分支', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }

    const sourceNodeKey = selectedRoute.value.sourceNodeKey
    const snapshot = removeRouteLane(working.nodes || [], working.routes || [], selectedRoute.value.routeKey)
    const remainingRoute = sortRoutes(snapshot.routes.filter((item) => item.sourceNodeKey === sourceNodeKey))[0]
    applyWorkingGraph(snapshot.nodes, snapshot.routes, remainingRoute ? { routeKey: remainingRoute.routeKey } : { nodeKey: sourceNodeKey })
    ElMessage.success('分支已删除')
  }

  async function removeSelectedNode() {
    if (!selectedNode.value) {
      return
    }

    const currentKey = selectedNode.value.nodeKey
    const isBranchNode = selectedNode.value.nodeType === 'BRANCH'

    try {
      await ElMessageBox.confirm(
        isBranchNode ? '删除该分支块后，内部所有泳道、节点和嵌套分支都会一并删除。确定继续吗？' : '确定删除当前节点吗？',
        isBranchNode ? '删除分支块' : '删除节点',
        {
          type: 'warning',
          confirmButtonText: '确定删除',
          cancelButtonText: '取消'
        }
      )
    } catch {
      return
    }

    const snapshot = removeNodeAndDescendants(working.nodes || [], working.routes || [], currentKey)
    const adjacentNodeKey = findAdjacentNodeKeyAfterRemoval(selectedNode.value, snapshot.nodes)
    applyWorkingGraph(snapshot.nodes, snapshot.routes, adjacentNodeKey ? { nodeKey: adjacentNodeKey } : {})
    ElMessage.success(isBranchNode ? '分支块已删除' : '节点已删除')
  }

  async function removeActiveBranchBlock() {
    if (!activeBranchNode.value) {
      return
    }

    try {
      await ElMessageBox.confirm('删除该分支块后，内部所有泳道、节点和嵌套分支都会一并删除。确定继续吗？', '删除分支块', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }

    const branchNode = activeBranchNode.value
    const snapshot = removeNodeAndDescendants(working.nodes || [], working.routes || [], branchNode.nodeKey)
    const adjacentNodeKey = findAdjacentNodeKeyAfterRemoval(branchNode, snapshot.nodes)
    applyWorkingGraph(snapshot.nodes, snapshot.routes, adjacentNodeKey ? { nodeKey: adjacentNodeKey } : {})
    ElMessage.success('分支块已删除')
  }

  function addConditionGroup(route: EditableProcessFlowRoute) {
    if (route.defaultRoute) {
      return
    }
    route.conditionGroups.push({
      groupNo: route.conditionGroups.length + 1,
      conditions: [createEmptyCondition()]
    })
    reindexConditionGroups(route)
  }

  function removeConditionGroup(route: EditableProcessFlowRoute, groupNo: number) {
    route.conditionGroups = route.conditionGroups.filter((item) => item.groupNo !== groupNo)
    reindexConditionGroups(route)
  }

  function addCondition(group: EditableProcessFlowConditionGroup) {
    group.conditions.push(createEmptyCondition())
  }

  function removeCondition(group: EditableProcessFlowConditionGroup, index: number) {
    group.conditions.splice(index, 1)
  }

  function reindexConditionGroups(route: EditableProcessFlowRoute) {
    route.conditionGroups = route.conditionGroups.map((item, index) => ({
      ...item,
      groupNo: index + 1
    }))
  }

  function handleConditionFieldChange(condition: EditableProcessFlowCondition) {
    const field = getConditionField(condition.fieldKey) || metaOptions.value.branchConditionFields[0]
    condition.fieldKey = field?.key || ''
    condition.operator = field?.operatorKeys?.[0] || 'EQ'
    condition.compareValue = normalizeConditionCompareValue(undefined, field?.valueType, condition.operator)
  }

  function handleConditionOperatorChange(condition: EditableProcessFlowCondition) {
    condition.compareValue = normalizeConditionCompareValue(
      condition.compareValue,
      getConditionField(condition.fieldKey)?.valueType,
      condition.operator
    )
  }

  function conditionValueOptions(condition: EditableProcessFlowCondition): ProcessFormOption[] {
    const valueType = getConditionField(condition.fieldKey)?.valueType
    switch (valueType) {
      case 'company':
        return metaOptions.value.companyOptions
      case 'department':
        return metaOptions.value.departmentOptions
      case 'user':
        return metaOptions.value.userOptions
      case 'expenseType':
        return metaOptions.value.expenseTypeOptions
      case 'archive':
        return metaOptions.value.archiveOptions
      default:
        return []
    }
  }

  function usesOptionSelect(condition: EditableProcessFlowCondition) {
    return isOptionValueType(getConditionField(condition.fieldKey)?.valueType)
  }

  function isNumberCondition(condition: EditableProcessFlowCondition) {
    return getConditionField(condition.fieldKey)?.valueType === 'number' && !isBetweenOperator(condition.operator)
  }

  function singleValuePlaceholder(condition: EditableProcessFlowCondition) {
    return usesOptionSelect(condition) ? '请选择比较值' : '请输入比较值'
  }

  function multiValuePlaceholder(condition: EditableProcessFlowCondition) {
    return usesOptionSelect(condition) ? '请选择多个比较值' : '请输入多个比较值后回车'
  }

  function describeRouteConditions(route?: ProcessFlowRoute) {
    const groups = route?.conditionGroups?.length || 0
    const conditions = (route?.conditionGroups || []).reduce((total, item) => total + (item.conditions?.length || 0), 0)
    return {
      groups,
      conditions
    }
  }

  function getFirstRouteForBranch(branchNodeKey: string) {
    return sortRoutes((working.routes || []).filter((item) => item.sourceNodeKey === branchNodeKey))[0]
  }

  function listNodesInContainer(nodes: ProcessFlowNode[], containerKey: FlowContainerKey) {
    return [...nodes]
      .filter((item) => normalizeContainerKey(item.parentNodeKey) === containerKey)
      .sort((left, right) => {
        const orderGap = (left.displayOrder ?? 0) - (right.displayOrder ?? 0)
        if (orderGap !== 0) {
          return orderGap
        }
        return left.nodeKey.localeCompare(right.nodeKey)
      })
  }

  function findAdjacentNodeKeyAfterRemoval(removedNode: ProcessFlowNode, nodes: ProcessFlowNode[]) {
    const siblings = listNodesInContainer(nodes, normalizeContainerKey(removedNode.parentNodeKey))
    const nextSibling = siblings.find((item) => (item.displayOrder ?? 0) >= (removedNode.displayOrder ?? 0))
    if (nextSibling) {
      return nextSibling.nodeKey
    }
    return siblings[siblings.length - 1]?.nodeKey || ''
  }

  function nodeCardClass(nodeType: string) {
    switch (nodeType) {
      case 'CC':
        return 'is-cc'
      case 'PAYMENT':
        return 'is-payment'
      case 'BRANCH':
        return 'is-branch'
      default:
        return 'is-approval'
    }
  }

  function sceneName(sceneId?: number) {
    if (!sceneId) {
      return ''
    }
    return metaOptions.value.sceneOptions.find((item) => item.id === sceneId)?.sceneName || ''
  }

  function buildPayload(): ProcessFlowSavePayload {
    const graph = prepareGraph(working.nodes || [], working.routes || [])
    return {
      flowName: working.flowName.trim(),
      flowDescription: working.flowDescription?.trim() || '',
      nodes: graph.nodes.map((node, index) => cloneValue(normalizeNode(node, index))),
      routes: graph.routes.map((route, index) => {
        const normalizedRoute = normalizeRoute(route, index)
        return {
          ...cloneValue(normalizedRoute),
          defaultRoute: normalizedRoute.defaultRoute,
          conditionGroups: normalizedRoute.defaultRoute ? [] : normalizedRoute.conditionGroups
        }
      })
    }
  }

  function currentSelectionPreference(): SelectionPreference {
    return selectedRoute.value ? { routeKey: selectedRoute.value.routeKey } : { nodeKey: selectedNode.value?.nodeKey }
  }

  function validateFlowPayloadBeforeSave(payload: ProcessFlowSavePayload) {
    const issues = validateFlowPayload(payload, metaOptions.value.branchConditionFields, metaOptions.value.sceneOptions)
    if (issues.length) {
      ElMessage.warning(issues[0])
      return false
    }
    return true
  }

  function normalizeApprovalModeSelection() {
    if (
      (selectedNode.value?.nodeType === 'APPROVAL' && shouldForceApprovalAndSign(selectedNode.value.config))
      || (
        selectedNode.value?.nodeType === 'PAYMENT'
        && selectedNode.value.config.approverType === 'DESIGNATED_USER_GROUP'
      )
    ) {
      selectedNode.value.config.approvalMode = 'AND_SIGN'
    }
  }

  return {
    clearSelection,
    normalizeFlowDetail,
    assignCopiedFlow,
    prepareGraph,
    applyWorkingGraph,
    restoreSelection,
    selectNode,
    selectRoute,
    handleCanvasDragStart,
    handleCanvasDragEnd,
    handleCanvasDragOver,
    handleCanvasDrop,
    handleCanvasInsert,
    addRouteLane,
    updateSelectedRouteAttachBelowNodes,
    updateSelectedRouteDefaultRoute,
    removeSelectedItem,
    removeSelectedRoute,
    removeSelectedNode,
    removeActiveBranchBlock,
    addConditionGroup,
    removeConditionGroup,
    addCondition,
    removeCondition,
    reindexConditionGroups,
    createEmptyCondition,
    handleConditionFieldChange,
    handleConditionOperatorChange,
    normalizeConditionCompareValue,
    normalizeScalarCompareValue,
    getConditionField,
    operatorOptionsForField,
    conditionValueOptions,
    usesOptionSelect,
    isNumberCondition,
    singleValuePlaceholder,
    multiValuePlaceholder,
    isOptionValueType,
    isMultiOperator,
    isBetweenOperator,
    describeRouteConditions,
    sortRoutes,
    nodeTypeLabel,
    shouldForceManagerAndSign,
    getFirstRouteForBranch,
    listNodesInContainer,
    findAdjacentNodeKeyAfterRemoval,
    nodeCardClass,
    sceneName,
    buildPayload,
    currentSelectionPreference,
    validateFlowPayloadBeforeSave,
    normalizeApprovalModeSelection,
    hasOpenMessageBox,
    isEditingElement,
    handleMoveFailure,
    buildNodeByType,
    normalizeNode,
    normalizeRoute,
    normalizeConditionGroups,
    normalizeCondition,
    normalizeNumberArray,
    cloneValue,
    selectedNode,
    selectedRoute,
    selectedRouteBranchNode,
    activeBranchNode,
    currentBranchRoutes
  }
}
