<template>
  <el-drawer
    :model-value="modelValue"
    title="单据详情"
    direction="rtl"
    size="min(920px, 92vw)"
    destroy-on-close
    data-testid="expense-document-readonly-drawer"
    @update:model-value="handleVisibleChange"
  >
    <div v-loading="loading" class="expense-readonly-drawer">
      <div v-if="detail" class="space-y-5">
        <div class="expense-readonly-drawer__hero">
          <div class="min-w-0">
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-sky-100">只读查看</p>
            <h2 class="mt-2 break-words text-2xl font-semibold text-white">
              {{ detail.documentTitle || detail.documentCode }}
            </h2>
            <p class="mt-2 break-all text-sm text-sky-100">
              {{ detail.documentCode }} · {{ detail.statusLabel || detail.status || '状态未知' }}
            </p>
          </div>
          <div class="expense-readonly-drawer__amount">
            <span>金额</span>
            <strong>{{ amountText }}</strong>
          </div>
        </div>

        <div v-if="history.length" class="flex items-center justify-between gap-3">
          <el-button plain size="small" @click="goBack">返回上一级</el-button>
          <span class="text-xs text-slate-400">已查看 {{ history.length + 1 }} 张单据</span>
        </div>

        <ExpenseDocumentReadonlyFormPanel
          :amount-text="amountText"
          :display="readonlyFormDisplay"
          @open-document-detail="openNestedDocument"
        />

        <ExpenseDocumentBindingPanels
          :panels="bindingPanels"
          binding-count-suffix="条"
          @open-bound-document="openNestedDocument"
        />

        <ExpenseDocumentExpenseDetailSection
          :cards="expenseDetailCards"
          :summary-items="expenseDetailSummaryItems"
          :workspace-visible="expenseDetailWorkspaceVisible"
          :workbench-display="expenseDetailWorkbenchDisplay"
          @select-detail="selectExpenseDetail"
          @open-detail="selectExpenseDetail"
        />

        <ExpenseDocumentBankSection
          :visible="bankSectionVisible"
          :payment-status-label="detail.bankPayment?.paymentStatusLabel || ''"
          :payment-summary-items="bankPaymentSummaryItems"
          :receipt-items="bankReceiptItems"
        />

        <ExpenseDocumentApprovalPanel
          :summary-items="approvalSummaryItems"
          :approval-timeline-items="approvalTimelineItems"
          :approval-status-tag-type="approvalStatusTagType"
        />
      </div>

      <el-empty v-else :description="errorMessage || '暂无单据数据'" :image-size="96" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { expenseApi, type ExpenseDocumentDetail, type ProcessFormDesignSchema } from '@/api'
import type { ExpenseDocumentRelationBinding, ExpenseDocumentWriteOffBinding } from '@/api/modules/expense-types'
import ExpenseDocumentApprovalPanel from './ExpenseDocumentApprovalPanel.vue'
import ExpenseDocumentBankSection from './ExpenseDocumentBankSection.vue'
import ExpenseDocumentBindingPanels from './ExpenseDocumentBindingPanels.vue'
import ExpenseDocumentExpenseDetailSection from './ExpenseDocumentExpenseDetailSection.vue'
import ExpenseDocumentReadonlyFormPanel from './ExpenseDocumentReadonlyFormPanel.vue'
import { useExpenseDocumentDetailDisplayOwner } from '../composables/useExpenseDocumentDetailDisplayOwner'
import { useReadonlyPayeeLookups } from '../useReadonlyPayeeLookups'
import { buildAuthorizedAttachmentPreviewUrl } from '../expenseInvoicePreview'
import { formatMoney } from '@/utils/money'

const props = defineProps<{
  modelValue: boolean
  documentCode?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const emptyExpenseDetailSchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }
const detail = ref<ExpenseDocumentDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const history = ref<string[]>([])
const activeDocumentCode = ref('')
const activeExpenseDetailNo = ref('')
const expenseDetailLoadingNo = ref('')
const expenseDetailCache = ref<Record<string, Awaited<ReturnType<typeof expenseApi.getExpenseDetail>>['data']>>({})
const expenseDetailErrors = ref<Record<string, string>>({})
const relatedBindingsExpanded = ref(false)
const writeOffBindingsExpanded = ref(false)
const { vendorOptionMap, payeeOptionMap, payeeAccountOptionMap, syncReadonlyPayeeLookups } = useReadonlyPayeeLookups()
let requestVersion = 0

const amountText = computed(() => `¥ ${formatMoney(detail.value?.totalAmount || 0)}`)
const activeExpenseDetail = computed(() =>
  activeExpenseDetailNo.value ? expenseDetailCache.value[activeExpenseDetailNo.value] || null : null
)
const activeExpenseDetailError = computed(() =>
  activeExpenseDetailNo.value ? expenseDetailErrors.value[activeExpenseDetailNo.value] || '' : ''
)
const activeExpenseDetailSummary = computed(() =>
  detail.value?.expenseDetails?.find((item) => item.detailNo === activeExpenseDetailNo.value) || null
)
const relatedDocumentBindings = computed<ExpenseDocumentRelationBinding[]>(() => detail.value?.relatedDocumentBindings || [])
const outboundRelatedBindings = computed(() => relatedDocumentBindings.value.filter((item) => item.direction === 'OUTBOUND'))
const inboundRelatedBindings = computed(() => relatedDocumentBindings.value.filter((item) => item.direction === 'INBOUND'))
const writeOffDocumentBindings = computed<ExpenseDocumentWriteOffBinding[]>(() => detail.value?.writeOffDocumentBindings || [])
const outboundWriteOffBindings = computed(() => writeOffDocumentBindings.value.filter((item) => item.direction === 'OUTBOUND'))
const inboundWriteOffBindings = computed(() => writeOffDocumentBindings.value.filter((item) => item.direction === 'INBOUND'))
const approvalTimelineItems = computed(() => detail.value?.approvalTimeline || [])

const displayRuntime = useExpenseDocumentDetailDisplayOwner({
  detail,
  vendorOptionMap,
  payeeOptionMap,
  payeeAccountOptionMap,
  relatedBindingsExpanded,
  writeOffBindingsExpanded,
  activeExpenseDetailNo,
  expenseDetailLoadingNo,
  activeExpenseDetail,
  activeExpenseDetailSummary,
  activeExpenseDetailError,
  relatedDocumentBindings,
  outboundRelatedBindings,
  inboundRelatedBindings,
  writeOffDocumentBindings,
  outboundWriteOffBindings,
  inboundWriteOffBindings,
  bindingCountSuffix: '条',
  bindingInlineSeparator: '·',
  expandText: '展开',
  collapseText: '收起',
  businessDocumentLabel: '业务单据',
  relatedCardTitle: '关联单据',
  relatedCardDescription: '展示当前单据与其他单据之间的业务关联。',
  relatedOutboundTitle: '当前单据主动关联',
  relatedInboundTitle: '被其它单据关联',
  writeOffCardTitle: '核销单据',
  writeOffCardDescription: '展示当前单据的核销关系和金额。',
  writeOffOutboundTitle: '当前单据主动核销',
  writeOffInboundTitle: '被其它单据核销',
  documentCodeLabel: '单据编号：',
  submitterLabel: '发起人：',
  sourceFieldLabel: '来源字段：',
  bindingFieldLabel: '关联字段：',
  writeOffSourceLabel: '核销来源：',
  requestedAmountLabel: '请求核销：',
  effectiveAmountLabel: '已生效：',
  remainingAmountLabel: '剩余金额：',
  unknownStatusLabel: '状态未知',
  relatedOutboundEmptyText: '暂无主动关联记录',
  relatedInboundEmptyText: '暂无反向关联记录',
  writeOffOutboundEmptyText: '暂无主动核销记录',
  writeOffInboundEmptyText: '暂无反向核销记录',
  emptyExpenseDetailSchema,
  resolveExpenseDetailTypeLabel: resolveExpenseDetailTypeLabel,
  formatBindingMoney: (value: unknown) => `¥ ${formatMoney(value as string | number | null | undefined)}`,
  writeOffSourceKindLabel: resolveWriteOffSourceKindLabel,
  formatAttachmentSize: resolveAttachmentSize,
  buildAuthorizedAttachmentPreviewUrl
})

const {
  readonlyFormDisplay,
  bindingPanels,
  expenseDetailCards,
  expenseDetailWorkspaceVisible,
  expenseDetailSummaryItems,
  expenseDetailWorkbenchDisplay,
  bankSectionVisible,
  bankPaymentSummaryItems,
  bankReceiptItems,
  approvalSummaryItems
} = displayRuntime

watch(
  () => [props.modelValue, props.documentCode] as const,
  ([visible, code]) => {
    if (!visible || !String(code || '').trim()) {
      return
    }
    const normalizedCode = String(code).trim()
    history.value = []
    activeDocumentCode.value = normalizedCode
    void loadDocument(normalizedCode)
  },
  { immediate: true }
)

function handleVisibleChange(value: boolean) {
  emit('update:modelValue', value)
}

function openNestedDocument(documentCode: string) {
  const normalizedCode = String(documentCode || '').trim()
  if (!normalizedCode || normalizedCode === activeDocumentCode.value) {
    return
  }
  if (activeDocumentCode.value) {
    history.value = [...history.value, activeDocumentCode.value]
  }
  activeDocumentCode.value = normalizedCode
  void loadDocument(normalizedCode)
}

function goBack() {
  const previous = history.value.at(-1)
  if (!previous) {
    return
  }
  history.value = history.value.slice(0, -1)
  activeDocumentCode.value = previous
  void loadDocument(previous)
}

async function loadDocument(documentCode: string) {
  const currentVersion = ++requestVersion
  loading.value = true
  errorMessage.value = ''
  detail.value = null
  activeExpenseDetailNo.value = ''
  expenseDetailCache.value = {}
  expenseDetailErrors.value = {}
  relatedBindingsExpanded.value = false
  writeOffBindingsExpanded.value = false
  try {
    const response = await expenseApi.getDetail(documentCode)
    if (currentVersion !== requestVersion) {
      return
    }
    detail.value = response.data
    relatedBindingsExpanded.value = Boolean(response.data.relatedDocumentBindings?.length)
    writeOffBindingsExpanded.value = Boolean(response.data.writeOffDocumentBindings?.length)
    void syncReadonlyPayeeLookups(response.data.formSchemaSnapshot)
  } catch (error: unknown) {
    if (currentVersion !== requestVersion) {
      return
    }
    errorMessage.value = error instanceof Error && error.message ? error.message : '加载单据详情失败'
    ElMessage.error(errorMessage.value)
  } finally {
    if (currentVersion === requestVersion) {
      loading.value = false
    }
  }
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
  expenseDetailLoadingNo.value = detailNo
  try {
    const response = await expenseApi.getExpenseDetail(activeDocumentCode.value, detailNo)
    expenseDetailCache.value = { ...expenseDetailCache.value, [detailNo]: response.data }
  } catch (error: unknown) {
    expenseDetailErrors.value = {
      ...expenseDetailErrors.value,
      [detailNo]: error instanceof Error && error.message ? error.message : '加载费用明细失败'
    }
  } finally {
    if (expenseDetailLoadingNo.value === detailNo) {
      expenseDetailLoadingNo.value = ''
    }
  }
}

function approvalStatusTagType(status?: string) {
  if (status === 'PENDING' || status === 'PAYMENT_PENDING' || status === 'MANUAL_SELECTION_PENDING') return 'warning'
  if (status === 'APPROVED' || status === 'PAYMENT_COMPLETED') return 'success'
  if (status === 'REJECTED' || status === 'EXCEPTION' || status === 'PAYMENT_EXCEPTION') return 'danger'
  return 'info'
}

function resolveExpenseDetailTypeLabel(detailType?: string, fallback?: string) {
  if (detailType === 'ENTERPRISE_TRANSACTION') return '企业往来'
  if (detailType === 'NORMAL_REIMBURSEMENT') return '普通报销'
  return fallback || '费用明细'
}

function resolveWriteOffSourceKindLabel(kind?: string) {
  if (kind === 'LOAN') return '借款单'
  if (kind === 'PREPAY_REPORT') return '预付报销单'
  return '-'
}

function resolveAttachmentSize(value?: number) {
  if (!value || Number.isNaN(Number(value))) return '大小未知'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style scoped>
.expense-readonly-drawer {
  min-height: 100%;
}

.expense-readonly-drawer__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0f4c81 0%, #0f766e 100%);
  padding: 20px 22px;
}

.expense-readonly-drawer__amount {
  flex: 0 0 auto;
  display: flex;
  min-width: 136px;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 18px;
  padding: 12px 14px;
  color: rgba(224, 242, 254, 0.88);
}

.expense-readonly-drawer__amount strong {
  color: #fff;
  font-size: 22px;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .expense-readonly-drawer__hero {
    flex-direction: column;
  }

  .expense-readonly-drawer__amount {
    align-items: flex-start;
  }
}
</style>
