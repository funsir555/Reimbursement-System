import { computed, nextTick, ref, watch, type ComputedRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  expenseApi,
  type ExpenseDetailInstanceDetail,
  type ExpenseDocumentDetail,
  type ExpenseDocumentNavigation,
  type ProcessFormDesignSchema
} from '@/api'
import type {
  ExpenseDocumentRelationBinding,
  ExpenseDocumentWriteOffBinding
} from '@/api/modules/expense-types'
import { formatMoney } from '@/utils/money'
import {
  buildExpenseDetailPrintHref,
  isExpenseDetailPrintMode,
  loadExpenseDocumentPrintBundle,
  openExpensePrintWindow
} from '@/views/expense/expensePrintSupport'
import { useReadonlyPayeeLookups } from '@/views/expense/useReadonlyPayeeLookups'

type UseExpenseDocumentDetailRuntimeOptions = {
  canLoadNavigation?: ComputedRef<boolean>
}

export function useExpenseDocumentDetailRuntime(options: UseExpenseDocumentDetailRuntimeOptions = {}) {
  const route = useRoute()
  const router = useRouter()

  const detailLoading = ref(false)
  const navigationLoading = ref(false)
  const detail = ref<ExpenseDocumentDetail | null>(null)
  const relatedBindingsExpanded = ref(false)
  const writeOffBindingsExpanded = ref(false)
  const detailLoadError = ref('')
  const printLoading = ref(false)
  const printLoadError = ref('')
  const printExpenseDetails = ref<ExpenseDetailInstanceDetail[]>([])
  const navigation = ref<ExpenseDocumentNavigation>({})
  const activeExpenseDetailNo = ref('')
  const expenseDetailLoadingNo = ref('')
  const expenseDetailCache = ref<Record<string, ExpenseDetailInstanceDetail>>({})
  const expenseDetailErrors = ref<Record<string, string>>({})
  const {
    vendorOptionMap,
    payeeOptionMap,
    payeeAccountOptionMap,
    syncReadonlyPayeeLookups,
    syncReadonlyPayeeLookupsBatch
  } = useReadonlyPayeeLookups()

  const emptyExpenseDetailSchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }
  let detailRequestVersion = 0
  let navigationRequestVersion = 0
  let lastPrintedDocumentCode = ''

  const amountText = computed(() => `¥ ${formatDetailMoney(detail.value?.totalAmount)}`)
  const isPrintMode = computed(() => isExpenseDetailPrintMode(route.query))
  const activeExpenseDetail = computed(() =>
    activeExpenseDetailNo.value ? expenseDetailCache.value[activeExpenseDetailNo.value] || null : null
  )
  const activeExpenseDetailError = computed(() =>
    activeExpenseDetailNo.value ? expenseDetailErrors.value[activeExpenseDetailNo.value] || '' : ''
  )
  const activeExpenseDetailSummary = computed(() =>
    detail.value?.expenseDetails?.find((item) => item.detailNo === activeExpenseDetailNo.value) || null
  )

  const relatedDocumentBindings = computed<ExpenseDocumentRelationBinding[]>(
    () => detail.value?.relatedDocumentBindings || []
  )
  const outboundRelatedBindings = computed<ExpenseDocumentRelationBinding[]>(
    () => relatedDocumentBindings.value.filter((item) => item.direction === 'OUTBOUND')
  )
  const inboundRelatedBindings = computed<ExpenseDocumentRelationBinding[]>(
    () => relatedDocumentBindings.value.filter((item) => item.direction === 'INBOUND')
  )
  const writeOffDocumentBindings = computed<ExpenseDocumentWriteOffBinding[]>(
    () => detail.value?.writeOffDocumentBindings || []
  )
  const outboundWriteOffBindings = computed<ExpenseDocumentWriteOffBinding[]>(
    () => writeOffDocumentBindings.value.filter((item) => item.direction === 'OUTBOUND')
  )
  const inboundWriteOffBindings = computed<ExpenseDocumentWriteOffBinding[]>(
    () => writeOffDocumentBindings.value.filter((item) => item.direction === 'INBOUND')
  )

  const bindingCountSuffix = '\u6761'
  const bindingInlineSeparator = '\u00b7'
  const expandText = '\u5c55\u5f00'
  const collapseText = '\u6536\u8d77'
  const businessDocumentLabel = '\u4e1a\u52a1\u5355\u636e'
  const viewBoundDocumentLabel = '\u67e5\u770b\u5355\u636e'
  const relatedCardTitle = '\u5173\u8054\u5355\u636e'
  const relatedCardDescription = '\u5c55\u793a\u5f53\u524d\u5355\u636e\u4e3b\u52a8\u5173\u8054\u4e0e\u88ab\u5176\u5b83\u5355\u636e\u53cd\u5411\u5f15\u7528\u7684\u771f\u5b9e\u4e1a\u52a1\u5173\u7cfb\u3002'
  const relatedOutboundTitle = '\u5f53\u524d\u5355\u636e\u4e3b\u52a8\u5173\u8054'
  const relatedInboundTitle = '\u88ab\u5176\u5b83\u5355\u636e\u5173\u8054'
  const writeOffCardTitle = '\u6838\u9500\u5355\u636e'
  const writeOffCardDescription = '\u5c55\u793a\u5f53\u524d\u5355\u636e\u4e3b\u52a8\u6838\u9500\u4e0e\u88ab\u5176\u5b83\u5355\u636e\u53cd\u5411\u6838\u9500\u7684\u771f\u5b9e\u91d1\u989d\u548c\u751f\u6548\u72b6\u6001\u3002'
  const writeOffOutboundTitle = '\u5f53\u524d\u5355\u636e\u4e3b\u52a8\u6838\u9500'
  const writeOffInboundTitle = '\u88ab\u5176\u5b83\u5355\u636e\u6838\u9500'
  const documentCodeLabel = '\u5355\u636e\u7f16\u53f7\uff1a'
  const submitterLabel = '\u53d1\u8d77\u4eba\uff1a'
  const sourceFieldLabel = '\u6765\u6e90\u5b57\u6bb5\uff1a'
  const bindingFieldLabel = '\u5173\u8054\u5b57\u6bb5\uff1a'
  const writeOffSourceLabel = '\u6838\u9500\u6765\u6e90\uff1a'
  const requestedAmountLabel = '\u8bf7\u6c42\u6838\u9500\uff1a'
  const effectiveAmountLabel = '\u5df2\u751f\u6548\uff1a'
  const remainingAmountLabel = '\u5269\u4f59\u91d1\u989d\uff1a'
  const unknownStatusLabel = '\u72b6\u6001\u672a\u77e5'
  const relatedOutboundEmptyText = '\u6682\u65e0\u4e3b\u52a8\u5173\u8054\u8bb0\u5f55'
  const relatedInboundEmptyText = '\u6682\u65e0\u53cd\u5411\u5173\u8054\u8bb0\u5f55'
  const writeOffOutboundEmptyText = '\u6682\u65e0\u4e3b\u52a8\u6838\u9500\u8bb0\u5f55'
  const writeOffInboundEmptyText = '\u6682\u65e0\u53cd\u5411\u6838\u9500\u8bb0\u5f55'

  watch(
    () => [route.params.documentCode, route.query.print],
    () => {
      void loadDetail()
    },
    { immediate: true }
  )

  function goBack() {
    void navigateBackWithFallback('/expense/list')
  }

  function resolveReturnToPath() {
    return typeof route.query.returnTo === 'string' && route.query.returnTo.trim()
      ? route.query.returnTo.trim()
      : ''
  }

  function buildReturnToQuery(extraQuery: Record<string, string> = {}) {
    const returnTo = resolveReturnToPath()
    return returnTo ? { ...extraQuery, returnTo } : extraQuery
  }

  function buildCurrentPageReturnToQuery(extraQuery: Record<string, string> = {}) {
    return route.fullPath ? { ...extraQuery, returnTo: route.fullPath } : extraQuery
  }

  async function navigateBackWithFallback(fallbackPath: string) {
    const returnTo = resolveReturnToPath()
    if (returnTo) {
      await router.push(returnTo)
      return
    }
    if (window.history.length > 1) {
      await router.back()
      return
    }
    await router.push(fallbackPath)
  }

  function openExpenseDetail(detailNo: string) {
    void router.push({
      name: 'expense-document-expense-detail',
      params: {
        documentCode: String(route.params.documentCode || ''),
        detailNo
      },
      query: buildCurrentPageReturnToQuery()
    })
  }

  function openBoundDocument(documentCode?: string) {
    if (!documentCode) {
      return
    }
    void router.push({
      path: `/expense/documents/${encodeURIComponent(documentCode)}`,
      query: buildCurrentPageReturnToQuery()
    })
  }

  function syncBindingPanelExpansion(nextDetail?: ExpenseDocumentDetail | null) {
    const source = nextDetail || null
    relatedBindingsExpanded.value = Boolean(source?.relatedDocumentBindings?.length)
    writeOffBindingsExpanded.value = Boolean(source?.writeOffDocumentBindings?.length)
  }

  async function selectExpenseDetail(detailNo: string) {
    if (!detailNo) {
      return
    }

    if (activeExpenseDetailNo.value === detailNo) {
      activeExpenseDetailNo.value = ''
      return
    }

    activeExpenseDetailNo.value = detailNo
    if (expenseDetailCache.value[detailNo] || expenseDetailLoadingNo.value === detailNo) {
      return
    }

    const nextErrors = { ...expenseDetailErrors.value }
    delete nextErrors[detailNo]
    expenseDetailErrors.value = nextErrors
    expenseDetailLoadingNo.value = detailNo

    try {
      const res = await expenseApi.getExpenseDetail(String(route.params.documentCode || ''), detailNo)
      expenseDetailCache.value = {
        ...expenseDetailCache.value,
        [detailNo]: res.data
      }
    } catch (error: unknown) {
      expenseDetailErrors.value = {
        ...expenseDetailErrors.value,
        [detailNo]: resolveErrorMessage(error, '加载费用明细发票信息失败')
      }
    } finally {
      if (expenseDetailLoadingNo.value === detailNo) {
        expenseDetailLoadingNo.value = ''
      }
    }
  }

  async function loadDetail() {
    const requestVersion = ++detailRequestVersion
    detailLoading.value = true
    printLoading.value = isPrintMode.value
    navigationRequestVersion += 1
    navigationLoading.value = false
    detailLoadError.value = ''
    printLoadError.value = ''
    detail.value = null
    syncBindingPanelExpansion(null)
    printExpenseDetails.value = []
    navigation.value = {}
    activeExpenseDetailNo.value = ''
    expenseDetailLoadingNo.value = ''
    expenseDetailCache.value = {}
    expenseDetailErrors.value = {}
    try {
      const documentCode = String(route.params.documentCode || '')
      if (!documentCode) {
        throw new Error('缺少单据编号')
      }

      if (isPrintMode.value) {
        const bundle = await loadExpenseDocumentPrintBundle(documentCode)
        if (requestVersion !== detailRequestVersion) {
          return
        }
        detail.value = bundle.detail
        syncBindingPanelExpansion(bundle.detail)
        printExpenseDetails.value = bundle.expenseDetails
        await syncReadonlyPayeeLookupsBatch([
          bundle.detail.formSchemaSnapshot,
          ...bundle.expenseDetails.map((item) => item.schemaSnapshot || emptyExpenseDetailSchema)
        ])
        await triggerPrint(documentCode)
      } else {
        const res = await expenseApi.getDetail(documentCode)
        if (requestVersion !== detailRequestVersion) {
          return
        }
        detail.value = res.data
        syncBindingPanelExpansion(res.data)
        void syncReadonlyPayeeLookups(res.data.formSchemaSnapshot)
        void loadNavigation(res.data.documentCode, requestVersion)
      }
    } catch (error: unknown) {
      if (requestVersion === detailRequestVersion) {
        const message = resolveErrorMessage(
          error,
          isPrintMode.value ? '加载打印数据失败' : '加载单据详情失败'
        )
        if (isPrintMode.value) {
          printLoadError.value = message
        } else {
          detailLoadError.value = message
        }
        ElMessage.error(message)
      }
    } finally {
      if (requestVersion === detailRequestVersion) {
        detailLoading.value = false
        printLoading.value = false
      }
    }
  }

  async function loadNavigation(documentCode: string, requestVersion: number) {
    const navigationVersion = ++navigationRequestVersion
    if (!documentCode || options.canLoadNavigation?.value === false) {
      navigation.value = {}
      navigationLoading.value = false
      return
    }
    navigationLoading.value = true
    try {
      const res = await expenseApi.getNavigation(documentCode)
      if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
        navigation.value = res.data
      }
    } catch {
      if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
        navigation.value = {}
      }
    } finally {
      if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
        navigationLoading.value = false
      }
    }
  }

  function handlePrint() {
    const documentCode = detail.value?.documentCode || String(route.params.documentCode || '')
    if (!documentCode) {
      ElMessage.warning('缺少单据编号，无法打开打印页')
      return
    }
    const openedWindow = openExpensePrintWindow(buildExpenseDetailPrintHref(router, documentCode))
    if (!openedWindow) {
      ElMessage.error('未能打开打印窗口，请检查浏览器弹窗拦截设置')
    }
  }

  async function triggerPrint(documentCode: string) {
    if (!documentCode || lastPrintedDocumentCode === documentCode || !detail.value || printLoadError.value) {
      return
    }
    lastPrintedDocumentCode = documentCode
    await nextTick()
    window.print()
  }

  async function navigateDetail(documentCode?: string) {
    if (!documentCode) {
      ElMessage.warning('已经没有更多单据了')
      return
    }
    await router.push(`/expense/documents/${encodeURIComponent(documentCode)}`)
  }

  async function refreshAfterAction(nextDetail?: ExpenseDocumentDetail) {
    if (nextDetail) {
      detailLoadError.value = ''
      detail.value = nextDetail
      syncBindingPanelExpansion(nextDetail)
      navigation.value = {}
      void syncReadonlyPayeeLookups(nextDetail.formSchemaSnapshot)
      await loadNavigation(nextDetail.documentCode, detailRequestVersion)
      return
    }
    await loadDetail()
  }

  function formatDetailMoney(value: unknown) {
    try {
      return formatMoney(value as string | number | null | undefined)
    } catch {
      return '0.00'
    }
  }

  function formatBindingMoney(value: unknown) {
    return `¥ ${formatDetailMoney(value)}`
  }

  function writeOffSourceKindLabel(kind?: string) {
    switch (kind) {
      case 'LOAN':
        return '借款单'
      case 'PREPAY_REPORT':
        return '预付报销单'
      default:
        return '-'
    }
  }

  function formatAttachmentSize(value?: number) {
    if (!value || Number.isNaN(Number(value))) {
      return '大小未知'
    }
    if (value < 1024) {
      return `${value} B`
    }
    if (value < 1024 * 1024) {
      return `${(value / 1024).toFixed(1)} KB`
    }
    return `${(value / (1024 * 1024)).toFixed(1)} MB`
  }

  function resolveErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  function resolveExpenseDetailTypeLabel(detailType?: string, fallback?: string) {
    if (detailType === 'ENTERPRISE_TRANSACTION') return '企业往来'
    if (detailType === 'NORMAL_REIMBURSEMENT') return '普通报销'
    return fallback || '费用明细'
  }

  return {
    detailLoading,
    navigationLoading,
    detail,
    relatedBindingsExpanded,
    writeOffBindingsExpanded,
    detailLoadError,
    printLoading,
    printLoadError,
    printExpenseDetails,
    navigation,
    activeExpenseDetailNo,
    expenseDetailLoadingNo,
    expenseDetailCache,
    expenseDetailErrors,
    vendorOptionMap,
    payeeOptionMap,
    payeeAccountOptionMap,
    amountText,
    isPrintMode,
    activeExpenseDetail,
    activeExpenseDetailError,
    activeExpenseDetailSummary,
    relatedDocumentBindings,
    outboundRelatedBindings,
    inboundRelatedBindings,
    writeOffDocumentBindings,
    outboundWriteOffBindings,
    inboundWriteOffBindings,
    bindingCountSuffix,
    bindingInlineSeparator,
    expandText,
    collapseText,
    businessDocumentLabel,
    viewBoundDocumentLabel,
    relatedCardTitle,
    relatedCardDescription,
    relatedOutboundTitle,
    relatedInboundTitle,
    writeOffCardTitle,
    writeOffCardDescription,
    writeOffOutboundTitle,
    writeOffInboundTitle,
    documentCodeLabel,
    submitterLabel,
    sourceFieldLabel,
    bindingFieldLabel,
    writeOffSourceLabel,
    requestedAmountLabel,
    effectiveAmountLabel,
    remainingAmountLabel,
    unknownStatusLabel,
    relatedOutboundEmptyText,
    relatedInboundEmptyText,
    writeOffOutboundEmptyText,
    writeOffInboundEmptyText,
    goBack,
    buildReturnToQuery,
    buildCurrentPageReturnToQuery,
    openExpenseDetail,
    openBoundDocument,
    selectExpenseDetail,
    loadDetail,
    loadNavigation,
    handlePrint,
    navigateDetail,
    refreshAfterAction,
    formatBindingMoney,
    writeOffSourceKindLabel,
    formatAttachmentSize,
    resolveErrorMessage,
    resolveExpenseDetailTypeLabel
  }
}
