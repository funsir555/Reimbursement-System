import {
  onActivated,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  ref,
  watch,
  type ComputedRef,
  type Ref
} from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  financeApi,
  type FinanceVoucherDetail,
  type FinanceVoucherMeta,
  type FinanceVoucherSavePayload
} from '@/api'
import type { Router } from 'vue-router'

type FinanceCompanyLike = {
  currentCompanyId?: string
  registerSwitchGuard: (key: string, guard: () => boolean | Promise<boolean>) => void
  unregisterSwitchGuard: (key: string) => void
}

type RouterLike = Pick<Router, 'push' | 'replace'>

type UseFinanceNewVoucherBootstrapOptions = {
  financeCompany: FinanceCompanyLike
  router: RouterLike
  companySwitchGuardKey: string
  pageMode: ComputedRef<'create' | 'detail' | 'review'>
  detailVoucherNo: ComputedRef<string>
  isDetailRoute: ComputedRef<boolean>
  isReviewMode: ComputedRef<boolean>
  backToListRouteName: ComputedRef<string>
  hasUnsavedChanges: () => boolean
  validationErrors: Ref<string[]>
  readDraft: (companyId?: string) => FinanceVoucherSavePayload | null
  resetFormFromMeta: (meta: FinanceVoucherMeta, companyId?: string) => void
  applyDraft: (draft: FinanceVoucherSavePayload, meta: FinanceVoucherMeta, companyId?: string) => void
  applyDetail: (detail: FinanceVoucherDetail, meta: FinanceVoucherMeta) => void
  markCommitted: () => void
  parseVoucherCompanyId: (voucherNo: string) => string
  resolveErrorMessage: (error: unknown, fallback: string) => string
}

export function useFinanceNewVoucherBootstrap(options: UseFinanceNewVoucherBootstrapOptions) {
  const loading = ref(false)
  const initializing = ref(false)
  const voucherMeta = ref<FinanceVoucherMeta | null>(null)
  const voucherDetail = ref<FinanceVoucherDetail | null>(null)
  const hasDraft = ref(false)
  const editingExisting = ref(false)
  const viewActive = ref(false)

  let loadSequence = 0
  let guardRegistered = false

  watch(() => options.financeCompany.currentCompanyId, async (companyId, previousCompanyId) => {
    if (!viewActive.value || !companyId || companyId === previousCompanyId) return
    await initializePage()
  })

  watch(() => [options.pageMode.value, options.detailVoucherNo.value] as const, async ([pageMode, voucherNo], previousValue) => {
    if (!viewActive.value) return
    if (previousValue && pageMode === previousValue[0] && voucherNo === previousValue[1]) return
    await initializePage()
  })

  onMounted(activateView)
  onActivated(activateView)
  onDeactivated(deactivateView)
  onBeforeUnmount(() => {
    deactivateView()
  })

  async function initializePage() {
    const companyId = options.financeCompany.currentCompanyId
    if (!companyId || !viewActive.value) return
    const loadId = beginLoad()

    if (options.isDetailRoute.value || options.isReviewMode.value) {
      const voucherCompanyId = options.parseVoucherCompanyId(options.detailVoucherNo.value)
      if (voucherCompanyId && voucherCompanyId !== companyId) {
        if (!isLiveLoad(loadId)) return
        editingExisting.value = false
        voucherDetail.value = null
        await options.router.replace({ name: options.backToListRouteName.value })
        return
      }
      await loadDetail(companyId, options.detailVoucherNo.value, loadId)
      return
    }

    if (!isLiveLoad(loadId)) return
    editingExisting.value = false
    voucherDetail.value = null
    await loadMeta(companyId, loadId)
  }

  async function loadMeta(companyId = options.financeCompany.currentCompanyId, loadId = beginLoad()) {
    if (!companyId) return
    loading.value = true
    initializing.value = true
    try {
      const res = await financeApi.getVoucherMeta({ companyId })
      if (!isLiveLoad(loadId)) return
      voucherMeta.value = res.data
      const draft = options.readDraft(companyId)
      hasDraft.value = Boolean(draft)
      if (draft) {
        options.applyDraft(draft, res.data, companyId)
        ElMessage.success('已恢复暂存草稿')
      } else {
        options.resetFormFromMeta(res.data, companyId)
      }
      options.validationErrors.value = []
      options.markCommitted()
    } catch (error: unknown) {
      if (isLiveLoad(loadId)) {
        ElMessage.error(options.resolveErrorMessage(error, '加载凭证配置失败'))
      }
    } finally {
      if (isLiveLoad(loadId)) {
        initializing.value = false
        loading.value = false
      }
    }
  }

  async function loadDetail(companyId: string, voucherNo: string, loadId = beginLoad()) {
    if (!companyId || !voucherNo) return
    loading.value = true
    initializing.value = true
    try {
      const detailRes = await financeApi.getVoucherDetail(companyId, voucherNo)
      if (!isLiveLoad(loadId)) return
      const metaRes = await financeApi.getVoucherMeta({
        companyId,
        billDate: detailRes.data.dbillDate,
        csign: detailRes.data.csign
      })
      if (!isLiveLoad(loadId)) return
      voucherMeta.value = metaRes.data
      voucherDetail.value = detailRes.data
      options.applyDetail(detailRes.data, metaRes.data)
      editingExisting.value = false
      hasDraft.value = false
      options.validationErrors.value = []
      options.markCommitted()
    } catch (error: unknown) {
      if (isLiveLoad(loadId)) {
        ElMessage.error(options.resolveErrorMessage(error, '加载凭证详情失败'))
      }
    } finally {
      if (isLiveLoad(loadId)) {
        initializing.value = false
        loading.value = false
      }
    }
  }

  async function confirmCompanySwitch() {
    if (!options.hasUnsavedChanges()) {
      return true
    }
    try {
      await ElMessageBox.confirm('切换公司后将丢弃当前凭证未保存内容，并按新公司重新加载，是否继续？', '切换公司', {
        type: 'warning',
        confirmButtonText: '继续切换',
        cancelButtonText: '取消'
      })
      if (options.isDetailRoute.value || options.isReviewMode.value) {
        editingExisting.value = false
        await options.router.replace({ name: options.backToListRouteName.value })
      }
      return true
    } catch {
      return false
    }
  }

  function activateView() {
    if (viewActive.value) return
    viewActive.value = true
    registerCompanySwitchGuard()
    void initializePage()
  }

  function deactivateView() {
    if (!viewActive.value && !guardRegistered) return
    viewActive.value = false
    loading.value = false
    initializing.value = false
    invalidatePendingLoads()
    unregisterCompanySwitchGuard()
  }

  function beginLoad() {
    loadSequence += 1
    return loadSequence
  }

  function invalidatePendingLoads() {
    loadSequence += 1
  }

  function isLiveLoad(loadId: number) {
    return viewActive.value && loadId === loadSequence
  }

  function registerCompanySwitchGuard() {
    if (guardRegistered) return
    options.financeCompany.registerSwitchGuard(options.companySwitchGuardKey, confirmCompanySwitch)
    guardRegistered = true
  }

  function unregisterCompanySwitchGuard() {
    if (!guardRegistered) return
    options.financeCompany.unregisterSwitchGuard(options.companySwitchGuardKey)
    guardRegistered = false
  }

  return {
    loading,
    initializing,
    voucherMeta,
    voucherDetail,
    hasDraft,
    editingExisting,
    initializePage,
    loadMeta,
    loadDetail
  }
}
