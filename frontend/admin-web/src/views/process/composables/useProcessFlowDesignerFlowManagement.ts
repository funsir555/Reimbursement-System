import { ElMessage, ElMessageBox } from 'element-plus'
import type { ComputedRef, Ref } from 'vue'
import type { LocationQuery, Router } from 'vue-router'
import { processApi, type ProcessFlowDetail, type ProcessFlowMeta, type ProcessFlowNode, type ProcessFlowSavePayload, type ProcessFlowSummary } from '@/api'
import { persistFlowDraft, publishFlowAfterPersist } from '@/views/process/flowDesignerPersistence'
import { resolveReturnToQuery } from '@/views/process/processDesignerNavigation'

type SelectionPreference = {
  nodeKey?: string
  routeKey?: string
}

type RouteLike = {
  name?: string | symbol | null
  params: Record<string, unknown>
  query: LocationQuery
}

type RouterLike = Pick<Router, 'push'> & Partial<Pick<Router, 'back'>>

type SceneDialogState = {
  visible: boolean
  sceneName: string
  sceneDescription: string
}

export function useProcessFlowDesignerFlowManagement(params: {
  route: RouteLike
  router: RouterLike
  working: ProcessFlowDetail
  flows: Ref<ProcessFlowSummary[]>
  meta: Ref<ProcessFlowMeta | null>
  metaOptions: ComputedRef<ProcessFlowMeta>
  selectedNode: ComputedRef<ProcessFlowNode | undefined>
  listLoading: Ref<boolean>
  saving: Ref<boolean>
  publishing: Ref<boolean>
  disabling: Ref<boolean>
  sceneSaving: Ref<boolean>
  sceneDialog: SceneDialogState
  createEmptyFlow: () => ProcessFlowDetail
  clearSelection: () => void
  normalizeFlowDetail: (detail: ProcessFlowDetail) => ProcessFlowDetail
  restoreSelection: (preferred?: SelectionPreference) => void
  assignCopiedFlow: (detail: ProcessFlowDetail) => void
  resolveErrorMessage: (error: unknown, fallback: string) => string
  buildPayload: () => ProcessFlowSavePayload
  validateFlowPayloadBeforeSave: (payload: ProcessFlowSavePayload) => boolean
  currentSelectionPreference: () => SelectionPreference
}) {
  const {
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
  } = params

  function parseRouteFlowId() {
    const raw = route.params.id
    if (raw === undefined || raw === null) {
      return null
    }
    const value = Number(Array.isArray(raw) ? raw[0] : raw)
    return Number.isFinite(value) ? value : null
  }

  function isCreateRoute() {
    return route.name === 'expense-workbench-process-flow-create'
  }

  function parseCopyFromFlowId() {
    if (!isCreateRoute()) {
      return null
    }
    const raw = Number(route.query.copyFromId)
    return Number.isFinite(raw) && raw > 0 ? raw : null
  }

  function createNewFlow(syncRoute = true) {
    Object.assign(working, createEmptyFlow())
    clearSelection()
    if (syncRoute && !isCreateRoute()) {
      void router.push({
        name: 'expense-workbench-process-flow-create',
        query: { ...route.query }
      })
    }
  }

  async function openFlow(id: number, showMessage = false) {
    try {
      const res = await processApi.getFlowDetail(id)
      Object.assign(working, normalizeFlowDetail(res.data))
      restoreSelection()
      if (showMessage) {
        ElMessage.success('\u6d41\u7a0b\u5df2\u5207\u6362')
      }
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '\u52a0\u8f7d\u6d41\u7a0b\u8be6\u60c5\u5931\u8d25'))
    }
  }

  async function openCopiedFlow(id: number) {
    try {
      const res = await processApi.getFlowDetail(id)
      assignCopiedFlow(res.data)
    } catch (error: unknown) {
      ElMessage.error(
        resolveErrorMessage(
          error,
          '\u590d\u5236\u6e90\u5ba1\u6279\u6d41\u7a0b\u52a0\u8f7d\u5931\u8d25\uff0c\u5df2\u4e3a\u4f60\u6253\u5f00\u7a7a\u767d\u65b0\u5efa\u9875'
        )
      )
    }
  }

  async function reloadFlowListOnly() {
    const res = await processApi.listFlows()
    flows.value = res.data
  }

  async function reloadPageData() {
    listLoading.value = true
    try {
      const [flowRes, metaRes] = await Promise.all([processApi.listFlows(), processApi.getFlowMeta()])
      flows.value = flowRes.data
      meta.value = metaRes.data

      const routeId = parseRouteFlowId()
      if (routeId) {
        await openFlow(routeId, false)
        return
      }

      if (isCreateRoute()) {
        const copyFromId = parseCopyFromFlowId()
        createNewFlow(false)
        if (copyFromId !== null) {
          await openCopiedFlow(copyFromId)
        }
        return
      }

      if (working.id && flows.value.some((item) => item.id === working.id)) {
        await openFlow(working.id, false)
        return
      }

      const firstFlow = flows.value[0]
      if (firstFlow) {
        await openFlow(firstFlow.id, false)
        return
      }

      createNewFlow(false)
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '\u52a0\u8f7d\u6d41\u7a0b\u8bbe\u8ba1\u6570\u636e\u5931\u8d25'))
    } finally {
      listLoading.value = false
    }
  }

  async function persistCurrentFlow(showSuccessMessage = true) {
    if (!working.flowName?.trim()) {
      ElMessage.warning('\u8bf7\u5148\u586b\u5199\u6d41\u7a0b\u540d\u79f0')
      return null
    }

    const payload = buildPayload()
    if (!validateFlowPayloadBeforeSave(payload)) {
      return null
    }

    saving.value = true
    try {
      const preferred = currentSelectionPreference()
      const detail = await persistFlowDraft(processApi, working.id, payload)
      Object.assign(working, normalizeFlowDetail(detail))
      restoreSelection(preferred)
      await reloadFlowListOnly()
      if (showSuccessMessage) {
        ElMessage.success('\u6d41\u7a0b\u8349\u7a3f\u5df2\u4fdd\u5b58')
      }
      return detail
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '\u4fdd\u5b58\u6d41\u7a0b\u5931\u8d25'))
      return null
    } finally {
      saving.value = false
    }
  }

  async function saveFlow() {
    await persistCurrentFlow()
  }

  async function publishCurrentFlow() {
    if (!working.flowName?.trim()) {
      ElMessage.warning('\u8bf7\u5148\u586b\u5199\u6d41\u7a0b\u540d\u79f0')
      return
    }

    const payload = buildPayload()
    if (!validateFlowPayloadBeforeSave(payload)) {
      return
    }

    publishing.value = true
    try {
      const preferred = currentSelectionPreference()
      const result = await publishFlowAfterPersist(processApi, working.id, payload, async (detail) => {
        Object.assign(working, normalizeFlowDetail(detail))
        restoreSelection(preferred)
        await reloadFlowListOnly()
      })
      Object.assign(working, normalizeFlowDetail(result.publishedDetail))
      restoreSelection(preferred)
      await reloadFlowListOnly()
      ElMessage.success('\u6d41\u7a0b\u5df2\u53d1\u5e03')

      const returnTo = resolveReturnToQuery(route.query)
      if (returnTo && working.flowCode) {
        await router.push(appendQueryParam(returnTo, 'createdFlowCode', working.flowCode))
      }
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '\u53d1\u5e03\u6d41\u7a0b\u5931\u8d25'))
    } finally {
      publishing.value = false
    }
  }

  async function disableCurrentFlow() {
    if (!working.id) {
      return
    }

    try {
      await ElMessageBox.confirm(
        '\u505c\u7528\u540e\u5f53\u524d\u6d41\u7a0b\u4e0d\u4f1a\u7ee7\u7eed\u7528\u4e8e\u65b0\u5355\u636e\uff0c\u786e\u5b9a\u7ee7\u7eed\u5417\uff1f',
        '\u505c\u7528\u6d41\u7a0b',
        {
          type: 'warning',
          confirmButtonText: '\u786e\u5b9a\u505c\u7528',
          cancelButtonText: '\u53d6\u6d88'
        }
      )
    } catch {
      return
    }

    disabling.value = true
    try {
      await processApi.updateFlowStatus(working.id, { status: 'DISABLED' })
      working.status = 'DISABLED'
      working.statusLabel = '\u5df2\u505c\u7528'
      await reloadFlowListOnly()
      ElMessage.success('\u6d41\u7a0b\u5df2\u505c\u7528')
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '\u505c\u7528\u6d41\u7a0b\u5931\u8d25'))
    } finally {
      disabling.value = false
    }
  }

  function resetSceneDialog() {
    sceneDialog.visible = false
    sceneDialog.sceneName = ''
    sceneDialog.sceneDescription = ''
  }

  async function submitScene() {
    if (!sceneDialog.sceneName.trim()) {
      ElMessage.warning('\u8bf7\u8f93\u5165\u573a\u666f\u540d\u79f0')
      return
    }

    sceneSaving.value = true
    try {
      const res = await processApi.createFlowScene({
        sceneName: sceneDialog.sceneName.trim(),
        sceneDescription: sceneDialog.sceneDescription.trim()
      })
      meta.value = {
        ...metaOptions.value,
        sceneOptions: [...metaOptions.value.sceneOptions, res.data]
      }
      if (selectedNode.value) {
        selectedNode.value.sceneId = res.data.id
      }
      resetSceneDialog()
      ElMessage.success('\u6d41\u7a0b\u573a\u666f\u5df2\u521b\u5efa')
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '\u65b0\u589e\u6d41\u7a0b\u573a\u666f\u5931\u8d25'))
    } finally {
      sceneSaving.value = false
    }
  }

  function goBack() {
    const returnTo = resolveReturnToQuery(route.query)
    if (returnTo) {
      void router.push(returnTo)
      return
    }
    if (typeof router.back === 'function' && window.history.length > 1) {
      void router.back()
      return
    }
    void router.push('/expense/workbench/process-management')
  }

  function appendQueryParam(path: string, key: string, value: string) {
    const separator = path.includes('?') ? '&' : '?'
    return `${path}${separator}${key}=${encodeURIComponent(value)}`
  }

  return {
    parseRouteFlowId,
    parseCopyFromFlowId,
    isCreateRoute,
    createNewFlow,
    openFlow,
    openCopiedFlow,
    reloadPageData,
    reloadFlowListOnly,
    persistCurrentFlow,
    saveFlow,
    publishCurrentFlow,
    disableCurrentFlow,
    resetSceneDialog,
    submitScene,
    goBack
  }
}
