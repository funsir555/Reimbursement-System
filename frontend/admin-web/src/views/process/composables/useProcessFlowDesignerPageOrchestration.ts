import { computed, onBeforeUnmount, onMounted, reactive, ref, watch, type ComponentPublicInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type {
  ProcessFlowDetail,
  ProcessFlowMeta,
  ProcessFlowSummary
} from '@/api'
import { useProcessFlowDesignerFlowManagement } from '@/views/process/composables/useProcessFlowDesignerFlowManagement'
import { useProcessFlowDesignerGraphState } from '@/views/process/composables/useProcessFlowDesignerGraphState'
import { useProcessFlowDesignerShellState } from '@/views/process/composables/useProcessFlowDesignerShellState'

type EditableProcessFlowRoute = ProcessFlowDetail['routes'][number]

export function useProcessFlowDesignerPageOrchestration() {
  const route = useRoute()
  const router = useRouter()

  const listLoading = ref(false)
  const saving = ref(false)
  const publishing = ref(false)
  const disabling = ref(false)
  const sceneSaving = ref(false)

  const keyword = ref('')
  const flows = ref<ProcessFlowSummary[]>([])
  const meta = ref<ProcessFlowMeta | null>(null)
  const selectedNodeKey = ref('')
  const selectedRouteKey = ref('')
  const draggingNodeKey = ref('')
  const dropTargetKey = ref('')
  const drawerVisible = ref(false)
  const canvasScrollRef = ref<HTMLElement | null>(null)
  const isCanvasPanning = ref(false)
  const canvasPanState = reactive({
    pointerId: -1,
    startX: 0,
    startY: 0,
    scrollLeft: 0,
    scrollTop: 0,
    moved: false
  })

  const working = reactive<ProcessFlowDetail>(createEmptyFlow())
  const sceneDialog = reactive({
    visible: false,
    sceneName: '',
    sceneDescription: ''
  })

  const {
    clearSelection,
    normalizeFlowDetail,
    assignCopiedFlow,
    restoreSelection,
    selectNode: selectNodeInternal,
    selectRoute: selectRouteInternal,
    handleCanvasDragStart: handleCanvasDragStartInternal,
    handleCanvasDragEnd: handleCanvasDragEndInternal,
    handleCanvasDragOver,
    handleCanvasDrop: handleCanvasDropInternal,
    handleCanvasInsert: handleCanvasInsertInternal,
    addRouteLane: addRouteLaneInternal,
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
    sortRoutes,
    nodeTypeLabel,
    nodeCardClass,
    sceneName,
    buildPayload,
    currentSelectionPreference,
    validateFlowPayloadBeforeSave,
    shouldForceManagerAndSign,
    normalizeApprovalModeSelection,
    hasOpenMessageBox,
    isEditingElement
  } = useProcessFlowDesignerGraphState({
    working,
    metaOptions: computed(() => meta.value ?? emptyMeta()),
    selectedNodeKey,
    selectedRouteKey,
    draggingNodeKey,
    dropTargetKey,
    createEmptyFlow
  })

  const {
    metaOptions,
    hasSelection,
    filteredFlows,
    canvasBlocks,
    selectedNode,
    selectedRoute,
    selectedRouteAttachBelowNodes,
    currentBranchRoutes,
    currentFlowLabel,
    panelTitle,
    panelDescription,
    removeButtonLabel,
    approvalApproverTypes,
    approvalOpinionCandidates,
    isManagerMultiLevelApproval,
    managerApprovalHint
  } = useProcessFlowDesignerShellState<EditableProcessFlowRoute>({
    keyword,
    flows,
    meta,
    selectedNodeKey,
    selectedRouteKey,
    workingNodes: computed(() => working.nodes || []),
    workingRoutes: computed(() => working.routes as EditableProcessFlowRoute[]),
    workingFlowName: computed(() => working.flowName || ''),
    emptyMeta,
    updateSelectedRouteAttachBelowNodes,
    sortRoutes,
    nodeTypeLabel,
    shouldForceManagerAndSign
  })

  const {
    parseRouteFlowId,
    parseCopyFromFlowId,
    isCreateRoute,
    createNewFlow,
    openFlow: openFlowInternal,
    openCopiedFlow,
    reloadPageData: reloadPageDataInternal,
    saveFlow,
    publishCurrentFlow,
    disableCurrentFlow,
    resetSceneDialog,
    submitScene,
    goBack
  } = useProcessFlowDesignerFlowManagement({
    route,
    router,
    working,
    flows,
    meta,
    metaOptions,
    selectedNode,
    listLoading,
    saving,
    publishing,
    disabling,
    sceneSaving,
    sceneDialog,
    createEmptyFlow,
    clearSelection,
    normalizeFlowDetail,
    restoreSelection,
    assignCopiedFlow,
    resolveErrorMessage,
    buildPayload,
    validateFlowPayloadBeforeSave,
    currentSelectionPreference
  })

  function openSceneDialog() {
    sceneDialog.visible = true
  }

  function openDrawer() {
    drawerVisible.value = true
  }

  function collapseDrawer(options: { clearCurrentSelection?: boolean } = {}) {
    drawerVisible.value = false
    if (options.clearCurrentSelection) {
      clearSelection()
    }
  }

  function toggleDrawer() {
    drawerVisible.value = !drawerVisible.value
  }

  function setCanvasScrollRef(element: Element | ComponentPublicInstance | null) {
    canvasScrollRef.value = element instanceof HTMLElement ? element : null
  }

  function isCanvasInteractiveTarget(target: EventTarget | null) {
    return target instanceof HTMLElement && Boolean(target.closest('[data-flow-interactive="true"]'))
  }

  function resetCanvasPanState() {
    canvasPanState.pointerId = -1
    canvasPanState.startX = 0
    canvasPanState.startY = 0
    canvasPanState.scrollLeft = 0
    canvasPanState.scrollTop = 0
    canvasPanState.moved = false
    isCanvasPanning.value = false
  }

  function handleCanvasPointerDown(event: PointerEvent) {
    const scrollElement = canvasScrollRef.value
    if (!scrollElement || event.button !== 0 || isCanvasInteractiveTarget(event.target)) {
      return
    }
    canvasPanState.pointerId = event.pointerId
    canvasPanState.startX = event.clientX
    canvasPanState.startY = event.clientY
    canvasPanState.scrollLeft = scrollElement.scrollLeft
    canvasPanState.scrollTop = scrollElement.scrollTop
    canvasPanState.moved = false
    scrollElement.setPointerCapture?.(event.pointerId)
  }

  function handleCanvasPointerMove(event: PointerEvent) {
    const scrollElement = canvasScrollRef.value
    if (!scrollElement || canvasPanState.pointerId !== event.pointerId) {
      return
    }
    const deltaX = event.clientX - canvasPanState.startX
    const deltaY = event.clientY - canvasPanState.startY
    if (!canvasPanState.moved && (Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4)) {
      canvasPanState.moved = true
      isCanvasPanning.value = true
    }
    if (!canvasPanState.moved) {
      return
    }
    scrollElement.scrollLeft = canvasPanState.scrollLeft - deltaX
    scrollElement.scrollTop = canvasPanState.scrollTop - deltaY
  }

  function finalizeCanvasPointer(event: PointerEvent, clearWhenTap: boolean) {
    const scrollElement = canvasScrollRef.value
    if (!scrollElement || canvasPanState.pointerId !== event.pointerId) {
      return
    }
    const tappedBlankCanvas = !canvasPanState.moved
    scrollElement.releasePointerCapture?.(event.pointerId)
    resetCanvasPanState()
    if (clearWhenTap && tappedBlankCanvas) {
      collapseDrawer({ clearCurrentSelection: true })
    }
  }

  function handleCanvasPointerUp(event: PointerEvent) {
    finalizeCanvasPointer(event, true)
  }

  function handleCanvasPointerCancel(event: PointerEvent) {
    finalizeCanvasPointer(event, false)
  }

  function selectNode(nodeKey: string) {
    selectNodeInternal(nodeKey)
    openDrawer()
  }

  function selectRoute(routeKey: string) {
    selectRouteInternal(routeKey)
    openDrawer()
  }

  function handleCanvasInsert(payload: Parameters<typeof handleCanvasInsertInternal>[0]) {
    handleCanvasInsertInternal(payload)
    openDrawer()
  }

  function addRouteLane(branchNodeKey: string) {
    addRouteLaneInternal(branchNodeKey)
    openDrawer()
  }

  function handleCanvasDragStart(payload: Parameters<typeof handleCanvasDragStartInternal>[0]) {
    handleCanvasDragStartInternal(payload)
  }

  function handleCanvasDragEnd() {
    handleCanvasDragEndInternal()
  }

  async function handleCanvasDrop(payload: Parameters<typeof handleCanvasDropInternal>[0]) {
    const outcome = await handleCanvasDropInternal(payload)
    if (outcome === 'copied') {
      openDrawer()
    }
  }

  function resolveErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  async function openFlow(flowId: number) {
    collapseDrawer()
    await openFlowInternal(flowId)
  }

  async function reloadPageData() {
    collapseDrawer()
    await reloadPageDataInternal()
  }

  function handleDesignerKeydown(event: KeyboardEvent) {
    if (event.key !== 'Delete' || (!selectedRoute.value && !selectedNode.value)) {
      return
    }
    if (hasOpenMessageBox() || isEditingElement(event.target)) {
      return
    }
    event.preventDefault()
    if (selectedRoute.value) {
      void removeSelectedRoute()
      return
    }
    void removeSelectedNode()
  }

  watch(
    () => [route.params.id, route.query.copyFromId],
    async () => {
      const routeId = parseRouteFlowId()
      if (routeId) {
        await openFlow(routeId)
        return
      }
      if (isCreateRoute()) {
        const copyFromId = parseCopyFromFlowId()
        createNewFlow(false)
        if (copyFromId !== null) {
          await openCopiedFlow(copyFromId)
        }
      }
    }
  )

  watch(
    () => [
      selectedNode.value?.nodeKey,
      selectedNode.value?.nodeType,
      selectedNode.value?.config?.approverType,
      selectedNode.value?.config?.managerConfig?.managerLevel
    ],
    () => {
      normalizeApprovalModeSelection()
    },
    { immediate: true }
  )

  onMounted(async () => {
    window.addEventListener('keydown', handleDesignerKeydown)
    await reloadPageData()
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleDesignerKeydown)
    resetCanvasPanState()
  })

  const panelRemoveDisabled = computed(() =>
    selectedRoute.value ? currentBranchRoutes.value.length <= 2 : false
  )

  const routeConditionEditorBindings = computed(() => ({
    state: {
      route: selectedRoute.value!,
      currentBranchRoutes: currentBranchRoutes.value,
      attachBelowEnabled: selectedRouteAttachBelowNodes.value
    },
    meta: {
      branchConditionFields: metaOptions.value.branchConditionFields,
      branchOperatorOptions: metaOptions.value.branchOperatorOptions,
      companyOptions: metaOptions.value.companyOptions,
      departmentOptions: metaOptions.value.departmentOptions,
      userOptions: metaOptions.value.userOptions,
      employeeDirectory: metaOptions.value.employeeDirectory,
      expenseTypeOptions: metaOptions.value.expenseTypeOptions,
      archiveOptions: metaOptions.value.archiveOptions,
      branchConditionValueOptions: metaOptions.value.branchConditionValueOptions
    },
    actions: {
      addRouteLane,
      removeSelectedItem,
      removeActiveBranchBlock,
      updateAttachBelowEnabled: updateSelectedRouteAttachBelowNodes,
      updateDefaultRouteEnabled: updateSelectedRouteDefaultRoute,
      addConditionGroup,
      removeConditionGroup,
      addCondition,
      removeCondition
    }
  }))

  const approvalSectionBindings = computed(() => ({
    state: {
      node: selectedNode.value!,
      meta: metaOptions.value,
      approvalApproverTypes: approvalApproverTypes.value,
      approvalOpinionCandidates: approvalOpinionCandidates.value,
      isManagerMultiLevelApproval: isManagerMultiLevelApproval.value,
      managerApprovalHint: managerApprovalHint.value
    },
    actions: {
      openSceneDialog
    }
  }))

  const ccSectionBindings = computed(() => ({
    state: {
      node: selectedNode.value!,
      meta: metaOptions.value,
      approvalApproverTypes: approvalApproverTypes.value,
      approvalOpinionCandidates: approvalOpinionCandidates.value
    },
    actions: {
      openSceneDialog
    }
  }))

  const paymentSectionBindings = computed(() => ({
    state: {
      node: selectedNode.value!,
      meta: metaOptions.value,
      approvalApproverTypes: approvalApproverTypes.value,
      approvalOpinionCandidates: approvalOpinionCandidates.value
    },
    actions: {
      openSceneDialog
    }
  }))

  const branchSectionBindings = computed(() => ({
    state: {
      node: selectedNode.value!,
      currentBranchRoutes: currentBranchRoutes.value,
      currentFlowLabel: currentFlowLabel.value
    },
    actions: {
      addRouteLane,
      selectRoute
    }
  }))

  return {
    drawerVisible,
    canvasScrollRef,
    isCanvasPanning,
    listLoading,
    saving,
    publishing,
    disabling,
    sceneSaving,
    keyword,
    working,
    sceneDialog,
    selectedNodeKey,
    selectedRouteKey,
    draggingNodeKey,
    dropTargetKey,
    hasSelection,
    filteredFlows,
    canvasBlocks,
    selectedNode,
    selectedRoute,
    currentBranchRoutes,
    panelTitle,
    panelDescription,
    removeButtonLabel,
    metaOptions,
    nodeTypeLabel,
    nodeCardClass,
    sceneName,
    openDrawer,
    collapseDrawer,
    toggleDrawer,
    setCanvasScrollRef,
    handleCanvasPointerDown,
    handleCanvasPointerMove,
    handleCanvasPointerUp,
    handleCanvasPointerCancel,
    selectNode,
    selectRoute,
    handleCanvasDragStart,
    handleCanvasDragEnd,
    handleCanvasDragOver,
    handleCanvasDrop,
    handleCanvasInsert,
    addRouteLane,
    openFlow,
    reloadPageData,
    removeSelectedItem,
    saveFlow,
    publishCurrentFlow,
    disableCurrentFlow,
    resetSceneDialog,
    submitScene,
    goBack,
    panelRemoveDisabled,
    routeConditionEditorBindings,
    approvalSectionBindings,
    ccSectionBindings,
    paymentSectionBindings,
    branchSectionBindings
  }
}

function createEmptyFlow(): ProcessFlowDetail {
  return {
    id: undefined,
    flowCode: '',
    flowName: '',
    flowDescription: '',
    status: 'DRAFT',
    statusLabel: '草稿',
    editableVersionId: undefined,
    editableVersionNo: undefined,
    publishedVersionId: undefined,
    publishedVersionNo: undefined,
    hasDraftVersion: false,
    nodes: [],
    routes: []
  }
}

function emptyMeta(): ProcessFlowMeta {
  return {
    nodeTypeOptions: [],
    sceneOptions: [],
    approvalApproverTypeOptions: [],
    approvalManagerRuleModeOptions: [],
    approvalManagerDeptSourceOptions: [],
    approvalManagerLevelOptions: [],
    approvalManagerLookupLevelOptions: [],
    approvalManualCandidateScopeOptions: [],
    ccReceiverTypeOptions: [],
    paymentExecutorTypeOptions: [],
    missingHandlerOptions: [],
    approvalModeOptions: [],
    defaultApprovalOpinions: ['通过', '拒绝', '加签', '转交'],
    approvalSpecialOptions: [],
    ccTimingOptions: [],
    ccSpecialOptions: [],
    paymentActionOptions: [],
    paymentSpecialOptions: [],
    branchOperatorOptions: [],
    branchConditionFields: [],
    branchConditionValueOptions: {},
    companyOptions: [],
    departmentOptions: [],
    userOptions: [],
    employeeDirectory: [],
    userGroupOptions: [],
    expenseTypeOptions: [],
    archiveOptions: []
  }
}
