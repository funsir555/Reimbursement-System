import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
    selectNode,
    selectRoute,
    handleCanvasDragStart,
    handleCanvasDragEnd,
    handleCanvasDragOver,
    handleCanvasDrop,
    handleCanvasInsert,
    addRouteLane,
    updateSelectedRouteAttachBelowNodes,
    removeSelectedItem,
    removeSelectedRoute,
    removeSelectedNode,
    removeActiveBranchBlock,
    addConditionGroup,
    removeConditionGroup,
    addCondition,
    removeCondition,
    handleConditionFieldChange,
    handleConditionOperatorChange,
    operatorOptionsForField,
    conditionValueOptions,
    usesOptionSelect,
    isNumberCondition,
    isBetweenOperator,
    isMultiOperator,
    singleValuePlaceholder,
    multiValuePlaceholder,
    describeRouteConditions,
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
    openFlow,
    openCopiedFlow,
    reloadPageData,
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

  function resolveErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback
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
  })

  const panelRemoveDisabled = computed(() =>
    selectedRoute.value ? currentBranchRoutes.value.length <= 2 : false
  )

  const routeConditionEditorBindings = computed(() => ({
    state: {
      route: selectedRoute.value!,
      activeBranchNode: activeBranchNode.value,
      currentBranchRoutes: currentBranchRoutes.value,
      attachBelowEnabled: selectedRouteAttachBelowNodes.value
    },
    meta: {
      branchConditionFields: metaOptions.value.branchConditionFields
    },
    helpers: {
      describeRouteConditions,
      operatorOptionsForField,
      conditionValueOptions,
      usesOptionSelect,
      isNumberCondition,
      isBetweenOperator,
      isMultiOperator,
      singleValuePlaceholder,
      multiValuePlaceholder
    },
    actions: {
      selectRoute,
      addRouteLane,
      removeSelectedItem,
      removeActiveBranchBlock,
      updateAttachBelowEnabled: updateSelectedRouteAttachBelowNodes,
      addConditionGroup,
      removeConditionGroup,
      addCondition,
      removeCondition,
      handleConditionFieldChange,
      handleConditionOperatorChange
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
      meta: metaOptions.value
    },
    actions: {
      openSceneDialog
    }
  }))

  const paymentSectionBindings = computed(() => ({
    state: {
      node: selectedNode.value!,
      meta: metaOptions.value
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
    helpers: {
      describeRouteConditions
    },
    actions: {
      addRouteLane,
      selectRoute
    }
  }))

  return {
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
    companyOptions: [],
    departmentOptions: [],
    userOptions: [],
    expenseTypeOptions: [],
    archiveOptions: []
  }
}
