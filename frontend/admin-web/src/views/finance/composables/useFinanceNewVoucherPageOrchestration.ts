import { ElMessage, ElMessageBox } from 'element-plus'
import { reactive, ref, type ComputedRef, type Ref } from 'vue'
import { financeApi, type FinanceVoucherDetail, type FinanceVoucherMeta, type FinanceVoucherSavePayload } from '@/api'
import type { Router } from 'vue-router'
import type { FinanceVoucherEntryRow } from './useFinanceNewVoucherRowOwner'

export type FinanceNewVoucherToolbarActionKey =
  | 'new'
  | 'modify'
  | 'print'
  | 'export'
  | 'copy'
  | 'reverse'
  | 'void'
  | 'insert'
  | 'delete'
  | 'searchReplace'
  | 'cashFlow'
  | 'save'
  | 'assist'
  | 'balance'
  | 'calculator'
  | 'review'
  | 'unreview'
  | 'markError'
  | 'find'

type ActionDialogKey = Exclude<
  FinanceNewVoucherToolbarActionKey,
  'new' | 'modify' | 'insert' | 'delete' | 'save' | 'review' | 'unreview' | 'markError' | 'find'
>

type RouterLike = Pick<Router, 'push' | 'replace'>

type UseFinanceNewVoucherPageOrchestrationOptions = {
  router: RouterLike
  voucherMeta: Ref<FinanceVoucherMeta | null>
  voucherDetail: Ref<FinanceVoucherDetail | null>
  editingExisting: Ref<boolean>
  validationErrors: Ref<string[]>
  isDetailRoute: ComputedRef<boolean>
  isReviewMode: ComputedRef<boolean>
  canEditExisting: ComputedRef<boolean>
  detailVoucherNo: ComputedRef<string>
  selectedRow: ComputedRef<FinanceVoucherEntryRow>
  selectedRowIndex: Ref<number>
  currentCompanyId: () => string
  getCurrentContext: () => { companyId: string; billDate: string; csign: string }
  getEntries: () => FinanceVoucherEntryRow[]
  selectRow: (index: number) => void
  loadMeta: (companyId?: string) => Promise<void>
  loadDetail: (companyId: string, voucherNo: string) => Promise<void>
  clearDraft: (companyId?: string) => void
  resetFormFromMeta: (meta: FinanceVoucherMeta, companyId?: string) => void
  markCommitted: () => void
  buildPayload: () => FinanceVoucherSavePayload
  validateVoucher: (showToast?: boolean) => boolean
  ensureSelectedRowUsesLeafSubject: () => Promise<boolean>
  ensureRowCashFlowState: (row: FinanceVoucherEntryRow) => boolean
  handleCashFlowFieldFocus: () => void
  resolveErrorMessage: (error: unknown, fallback: string) => string
  insertEntryAfter: (index: number) => void
  removeSelectedEntry: () => void
}

export function useFinanceNewVoucherPageOrchestration(options: UseFinanceNewVoucherPageOrchestrationOptions) {
  const saving = ref(false)
  const reviewActing = ref(false)
  const currentToolbarLoadingKey = ref<FinanceNewVoucherToolbarActionKey | ''>('')
  const actionDialog = reactive({ visible: false, title: '', description: '' })

  async function handleNewVoucher() {
    try {
      await ElMessageBox.confirm('将清空当前录入内容并开始新的凭证，是否继续？', '新增凭证', {
        type: 'warning',
        confirmButtonText: '继续',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }

    options.editingExisting.value = false
    options.voucherDetail.value = null
    options.clearDraft()
    options.validationErrors.value = []

    if (options.isDetailRoute.value || options.isReviewMode.value) {
      await options.router.push({ name: 'finance-new-voucher' })
      return
    }

    if (options.voucherMeta.value) {
      options.resetFormFromMeta(options.voucherMeta.value, options.currentCompanyId())
      options.markCommitted()
    } else {
      await options.loadMeta(options.currentCompanyId())
    }
  }

  function enterEditMode() {
    if (!options.voucherDetail.value?.editable) {
      ElMessage.warning('当前凭证状态不允许修改')
      return
    }
    if (!options.canEditExisting.value) {
      ElMessage.warning('当前账号没有修改凭证权限')
      return
    }
    options.editingExisting.value = true
  }

  async function handleSave() {
    if (!(await options.ensureSelectedRowUsesLeafSubject())) return
    if (options.ensureRowCashFlowState(options.selectedRow.value)) {
      options.handleCashFlowFieldFocus()
      return
    }
    if (!options.validateVoucher(true)) return

    saving.value = true
    try {
      if (options.isDetailRoute.value && options.detailVoucherNo.value) {
        const res = await financeApi.updateVoucher(
          options.currentCompanyId(),
          options.detailVoucherNo.value,
          options.buildPayload()
        )
        ElMessage.success(`凭证修改成功：${res.data.voucherNo}`)
        await options.loadDetail(options.currentCompanyId(), options.detailVoucherNo.value)
        return
      }

      const currentContext = options.getCurrentContext()
      const res = await financeApi.createVoucher(options.buildPayload())
      options.clearDraft()
      ElMessage.success(`凭证保存成功：${res.data.voucherNo}`)
      const nextMeta = await financeApi.getVoucherMeta(currentContext)
      options.voucherMeta.value = nextMeta.data
      options.resetFormFromMeta(nextMeta.data, currentContext.companyId)
      options.validationErrors.value = []
      options.markCommitted()
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, options.isDetailRoute.value ? '修改凭证失败' : '保存凭证失败'))
    } finally {
      saving.value = false
    }
  }

  async function handleReviewVoucher() {
    const companyId = options.currentCompanyId()
    if (!companyId || !options.detailVoucherNo.value) return

    reviewActing.value = true
    currentToolbarLoadingKey.value = 'review'
    try {
      const res = await financeApi.reviewVoucher(companyId, options.detailVoucherNo.value)
      ElMessage.success(`凭证审核成功：${res.data.voucherNo}`)
      if (res.data.nextVoucherNo) {
        await options.router.replace({
          name: 'finance-review-voucher-detail',
          params: { voucherNo: res.data.nextVoucherNo }
        })
        return
      }
      await options.loadDetail(companyId, options.detailVoucherNo.value)
      if (res.data.lastVoucherOfMonth) {
        ElMessage.warning('当前是最后一张')
      }
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '审核凭证失败'))
    } finally {
      currentToolbarLoadingKey.value = ''
      reviewActing.value = false
    }
  }

  async function handleUnreviewVoucher() {
    const companyId = options.currentCompanyId()
    if (!companyId || !options.detailVoucherNo.value) return

    reviewActing.value = true
    currentToolbarLoadingKey.value = 'unreview'
    try {
      const res = await financeApi.unreviewVoucher(companyId, options.detailVoucherNo.value)
      ElMessage.success(`凭证反审核成功：${res.data.voucherNo}`)
      await options.loadDetail(companyId, options.detailVoucherNo.value)
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '反审核凭证失败'))
    } finally {
      currentToolbarLoadingKey.value = ''
      reviewActing.value = false
    }
  }

  async function handleToggleVoucherError() {
    const companyId = options.currentCompanyId()
    if (!companyId || !options.detailVoucherNo.value) return

    const clearing = options.voucherDetail.value?.status === 'ERROR'
    reviewActing.value = true
    currentToolbarLoadingKey.value = 'markError'
    try {
      const res = clearing
        ? await financeApi.clearVoucherError(companyId, options.detailVoucherNo.value)
        : await financeApi.markVoucherError(companyId, options.detailVoucherNo.value)
      ElMessage.success(clearing ? `凭证取消错误成功：${res.data.voucherNo}` : `凭证标记错误成功：${res.data.voucherNo}`)
      await options.loadDetail(companyId, options.detailVoucherNo.value)
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, clearing ? '取消错误失败' : '标记错误失败'))
    } finally {
      currentToolbarLoadingKey.value = ''
      reviewActing.value = false
    }
  }

  async function handleExportCurrentVoucher() {
    const companyId = options.currentCompanyId()
    if (!companyId || !options.detailVoucherNo.value) return
    try {
      await financeApi.exportVouchers({ companyId, voucherNo: options.detailVoucherNo.value })
      ElMessage.success('当前凭证已开始导出')
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '导出当前凭证失败'))
    }
  }

  function handleFindInEntries() {
    const keyword = window.prompt('请输入关键字（摘要 / 科目编码 / 科目名称）')
    const normalizedKeyword = String(keyword || '').trim().toLowerCase()
    if (!normalizedKeyword) {
      return
    }
    const matchedIndex = options.getEntries().findIndex((row) =>
      [row.cdigest, row.ccode, row.ccodeName]
        .filter((item): item is string => Boolean(item))
        .some((item) => item.toLowerCase().includes(normalizedKeyword))
    )
    if (matchedIndex < 0) {
      ElMessage.warning('当前凭证未找到匹配分录')
      return
    }
    options.selectRow(matchedIndex)
    ElMessage.success(`已定位到第 ${matchedIndex + 1} 行`)
  }

  function openActionDialog(action: ActionDialogKey) {
    const labels: Record<ActionDialogKey, string> = {
      print: '打印',
      export: '导出',
      copy: '复制',
      reverse: '冲销',
      void: '作废',
      searchReplace: '查找替换',
      cashFlow: '现金流量',
      assist: '辅助核算',
      balance: '平衡',
      calculator: '计算器'
    }
    const descriptions: Record<ActionDialogKey, string> = {
      print: '后续可接入正式打印模板与套打配置。',
      export: '后续可扩展为 Excel、PDF 或外部接口输出。',
      copy: '后续可按原凭证复制摘要、科目和金额。',
      reverse: '后续可接入红字冲销与反向凭证生成流程。',
      void: '后续可接入作废状态流转和权限校验。',
      searchReplace: '后续可在分录摘要、科目和辅助项中做批量查找替换。',
      cashFlow: '请选择当前分录对应的现金流量。',
      assist: '当前下方辅助核算区域已可录入基础信息，后续可扩展为侧边明细抽屉。',
      balance: '后续可联动余额查询与科目实时余额提示。',
      calculator: '后续可接入悬浮计算器或公式辅助输入能力。'
    }

    actionDialog.title = labels[action]
    actionDialog.description = descriptions[action]
    actionDialog.visible = true
  }

  function handleToolbarAction(action: FinanceNewVoucherToolbarActionKey) {
    if (action === 'new') return void handleNewVoucher()
    if (action === 'modify') return void enterEditMode()
    if (action === 'insert') return options.insertEntryAfter(options.selectedRowIndex.value)
    if (action === 'delete') return options.removeSelectedEntry()
    if (action === 'save') return void handleSave()
    if (action === 'review') return void handleReviewVoucher()
    if (action === 'unreview') return void handleUnreviewVoucher()
    if (action === 'markError') return void handleToggleVoucherError()
    if (action === 'find') return void handleFindInEntries()
    if (action === 'export' && options.isReviewMode.value) return void handleExportCurrentVoucher()
    if (action === 'cashFlow') {
      if (!options.ensureRowCashFlowState(options.selectedRow.value)) return
      options.handleCashFlowFieldFocus()
      return
    }
    openActionDialog(action)
  }

  return {
    saving,
    reviewActing,
    currentToolbarLoadingKey,
    actionDialog,
    handleToolbarAction
  }
}
