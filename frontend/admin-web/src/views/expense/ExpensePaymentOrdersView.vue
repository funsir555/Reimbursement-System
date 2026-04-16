<template>
  <div class="expense-wb-page expense-wb-page--list payment-orders-page space-y-6">
    <section class="expense-wb-stat-grid expense-wb-stat-grid--compact">
      <article
        v-for="item in statusCards"
        :key="item.tab"
        class="expense-wb-stat-card expense-wb-stat-card--compact expense-wb-stat-card--filterable"
        :class="{ 'expense-wb-stat-card--active': activeTab === item.tab }"
        :data-testid="`expense-payment-stat-${item.tab}`"
        role="button"
        tabindex="0"
        @click="switchTab(item.tab)"
        @keydown.enter.prevent="switchTab(item.tab)"
        @keydown.space.prevent="switchTab(item.tab)"
      >
        <div class="expense-wb-stat-card__top">
          <div>
            <p class="expense-wb-stat-card__label">{{ item.label }}</p>
            <p class="expense-wb-stat-card__value">{{ item.count }}</p>
          </div>
          <el-tag effect="plain">{{ item.statusCode }}</el-tag>
        </div>
      </article>
    </section>

    <el-card class="expense-wb-toolbar expense-wb-toolbar--compact">
      <div class="expense-wb-toolbar__row expense-wb-toolbar__row--compact expense-wb-toolbar__main">
        <div class="expense-wb-toolbar__heading expense-wb-toolbar__heading--compact expense-wb-toolbar__heading--inline">
          <p class="expense-wb-toolbar__title">付款单工作台</p>
          <div class="expense-wb-toolbar__meta">
            <span class="expense-wb-soft-badge">当前状态 {{ currentRows.length }}</span>
            <span class="expense-wb-soft-badge expense-wb-soft-badge--success">筛选结果 {{ filteredRows.length }}</span>
          </div>
        </div>

        <div class="expense-wb-toolbar__group">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索单据编号、标题、提单人"
            class="w-72"
          />
          <el-button :icon="Refresh" @click="reloadAll()">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="expense-wb-panel expense-wb-table-shell expense-wb-table-shell--compact">
      <el-table :data="pagedRows" v-loading="loading" style="width: 100%">
        <el-table-column v-if="isPendingTab" label="选择" width="68">
          <template #header>
            <input
              :checked="allCurrentPageSelected"
              data-testid="expense-payment-select-all"
              type="checkbox"
              @change="handleSelectAllChange"
            >
          </template>
          <template #default="{ row }">
            <input
              :checked="isTaskSelected(row.taskId)"
              :data-testid="`expense-payment-select-row-${row.taskId}`"
              type="checkbox"
              @change="handleRowCheckboxChange(row, $event)"
            >
          </template>
        </el-table-column>

        <el-table-column prop="documentCode" label="单据编号" min-width="150">
          <template #default="{ row }">
            <button
              class="cursor-pointer font-medium text-blue-600 hover:underline"
              type="button"
              @click="openDetail(row)"
            >
              {{ row.documentCode }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="documentTitle" label="单据名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="templateTypeLabel" label="单据类型" min-width="100" />
        <el-table-column prop="submitterName" label="提单人" min-width="100" />
        <el-table-column prop="paymentCompanyName" label="付款公司" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">{{ formatMoneyText(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentStatusLabel" label="付款状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.paymentStatusCode || row.documentStatus)">
              {{ row.paymentStatusLabel || row.documentStatusLabel || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiptStatusLabel" label="回单状态" min-width="110" />
        <el-table-column prop="companyBankAccountName" label="直连账户" min-width="160" show-overflow-tooltip />
        <el-table-column prop="bankFlowNo" label="银行流水号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="paidAt" label="支付时间" min-width="160" />

        <el-table-column v-if="showRowActions" label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <div class="flex flex-wrap items-center gap-2">
              <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="row.paymentStatusCode === 'PENDING_PAYMENT' || row.paymentStatusCode === 'PAYMENT_EXCEPTION'"
                link
                type="primary"
                size="small"
                @click="startPayment(row)"
              >
                发起支付
              </el-button>
              <el-button
                v-if="row.paymentStatusCode === 'PENDING_PAYMENT' || row.paymentStatusCode === 'PAYING'"
                link
                type="success"
                size="small"
                @click="completePayment(row)"
              >
                手动已支付
              </el-button>
              <el-button
                v-if="row.paymentStatusCode === 'PENDING_PAYMENT' || row.paymentStatusCode === 'PAYING'"
                link
                type="danger"
                size="small"
                @click="markException(row)"
              >
                标记异常
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="expense-wb-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredRows.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <div v-if="showFloatingBar" class="detail-floating-bar" data-testid="expense-payment-floating-bar">
      <div class="detail-floating-inner payment-floating-inner">
        <div class="payment-floating-selection">
          <span class="payment-floating-selection__count">{{ floatingSelectionText }}</span>
          <span class="payment-floating-selection__hint">{{ floatingSelectionHint }}</span>
        </div>
        <div class="detail-floating-actions" data-testid="expense-payment-floating-actions">
          <div class="detail-floating-actions__group detail-floating-actions__group--secondary">
            <el-button class="detail-floating-button" data-testid="expense-payment-bulk-start" :disabled="!hasSelectedTasks" @click="openStartPaymentDialog">发起支付</el-button>
            <el-button class="detail-floating-button" data-testid="expense-payment-bulk-download" :disabled="!hasSelectedTasks" @click="handleBulkDownload">下载</el-button>
            <el-button class="detail-floating-button" data-testid="expense-payment-bulk-manual-paid" :disabled="!hasSelectedTasks" @click="showPlaceholder('手动已支付')">手动已支付</el-button>
            <el-button class="detail-floating-button" data-testid="expense-payment-bulk-print" :disabled="!hasSelectedTasks" @click="showPlaceholder('打印')">打印</el-button>
          </div>
          <div class="detail-floating-actions__group detail-floating-actions__group--primary">
            <el-button
              class="detail-floating-button detail-floating-button--danger"
              data-testid="expense-payment-bulk-reject"
              type="danger"
              :disabled="!hasSelectedTasks"
              @click="handleBulkReject"
            >
              驳回
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="startPaymentDialogVisible" title="发起支付" width="520px">
      <div class="payment-start-dialog">
        <button
          type="button"
          class="payment-start-option"
          data-testid="expense-payment-start-option-export"
          @click="showPlaceholder('导出支付单')"
        >
          <strong>导出支付单</strong>
          <span>按已选待支付单据生成支付单文件，后续再接通真实导出支付链路。</span>
        </button>
        <button
          type="button"
          class="payment-start-option"
          data-testid="expense-payment-start-option-bank-link"
          @click="showPlaceholder('银企直连支付')"
        >
          <strong>银企直连支付</strong>
          <span>预留银企直连入口，本期先做静态展示，不触发真实支付动作。</span>
        </button>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="startPaymentDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { expensePaymentApi, type ExpensePaymentOrder, type MoneyValue } from '@/api'
import { formatMoney } from '@/utils/money'

type PaymentTab = 'pending' | 'paying' | 'paid' | 'finished' | 'exception'

const TAB_STATUS_MAP: Record<PaymentTab, string> = {
  pending: 'PENDING_PAYMENT',
  paying: 'PAYING',
  paid: 'PAYMENT_COMPLETED',
  finished: 'PAYMENT_FINISHED',
  exception: 'PAYMENT_EXCEPTION'
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const startPaymentDialogVisible = ref(false)
const rowsByTab = ref<Record<PaymentTab, ExpensePaymentOrder[]>>({
  pending: [],
  paying: [],
  paid: [],
  finished: [],
  exception: []
})
const selectedTaskIds = ref<number[]>([])
const selectedRowsSnapshot = ref<Record<number, ExpensePaymentOrder>>({})

const activeTab = computed<PaymentTab>(() => {
  const tab = String(route.query.tab || 'pending')
  return ['pending', 'paying', 'paid', 'finished', 'exception'].includes(tab) ? (tab as PaymentTab) : 'pending'
})

const isPendingTab = computed(() => activeTab.value === 'pending')
const showRowActions = computed(() => !isPendingTab.value)
const currentRows = computed(() => rowsByTab.value[activeTab.value] || [])
const filteredRows = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  if (!normalized) {
    return currentRows.value
  }
  return currentRows.value.filter((item) => (
    [item.documentCode, item.documentTitle, item.submitterName, item.paymentCompanyName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized))
  ))
})
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})
const allCurrentPageSelected = computed(() => (
  isPendingTab.value && pagedRows.value.length > 0 && pagedRows.value.every((row) => isTaskSelected(row.taskId))
))
const hasSelectedTasks = computed(() => selectedTaskIds.value.length > 0)
const showFloatingBar = computed(() => isPendingTab.value)
const floatingSelectionText = computed(() => (
  hasSelectedTasks.value ? `已选 ${selectedTaskIds.value.length} 项` : '未选择单据'
))
const floatingSelectionHint = computed(() => (
  hasSelectedTasks.value
    ? '勾选单据后可在这里批量发起支付、下载、驳回等操作。'
    : '请先勾选待支付单据，再进行批量操作'
))

const statusCards = computed(() => [
  { tab: 'pending' as const, label: '待支付', count: rowsByTab.value.pending.length, statusCode: 'PENDING_PAYMENT' },
  { tab: 'paying' as const, label: '支付中', count: rowsByTab.value.paying.length, statusCode: 'PAYING' },
  { tab: 'paid' as const, label: '已支付', count: rowsByTab.value.paid.length, statusCode: 'PAYMENT_COMPLETED' },
  { tab: 'finished' as const, label: '已完成', count: rowsByTab.value.finished.length, statusCode: 'PAYMENT_FINISHED' },
  { tab: 'exception' as const, label: '支付异常', count: rowsByTab.value.exception.length, statusCode: 'PAYMENT_EXCEPTION' }
])

watch(
  () => route.query.tab,
  () => {
    currentPage.value = 1
    if (!isPendingTab.value) {
      clearSelection()
      startPaymentDialogVisible.value = false
    }
  },
  { immediate: true }
)

watch(keyword, () => {
  currentPage.value = 1
})

watch(pageSize, () => {
  currentPage.value = 1
})

void reloadAll({ clearSelection: false })

function switchTab(tab: PaymentTab) {
  void router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab
    }
  })
}

async function reloadAll(options: { clearSelection?: boolean } = {}) {
  loading.value = true
  try {
    const entries = await Promise.all(
      Object.entries(TAB_STATUS_MAP).map(async ([tab, status]) => {
        const res = await expensePaymentApi.listOrders(status)
        return [tab, res.data || []] as const
      })
    )
    rowsByTab.value = Object.fromEntries(entries) as Record<PaymentTab, ExpensePaymentOrder[]>
    if (options.clearSelection) {
      clearSelection()
    } else {
      pruneSelection()
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载付款单失败'))
  } finally {
    loading.value = false
  }
}

function pruneSelection() {
  const visibleIds = new Set(rowsByTab.value.pending.map((item) => item.taskId))
  selectedTaskIds.value = selectedTaskIds.value.filter((taskId) => visibleIds.has(taskId))
  selectedRowsSnapshot.value = Object.fromEntries(
    Object.entries(selectedRowsSnapshot.value).filter(([taskId]) => visibleIds.has(Number(taskId)))
  )
}

function clearSelection() {
  selectedTaskIds.value = []
  selectedRowsSnapshot.value = {}
}

function isTaskSelected(taskId: number) {
  return selectedTaskIds.value.includes(taskId)
}

function toggleTaskSelection(row: ExpensePaymentOrder, checked: boolean) {
  if (checked) {
    if (!selectedTaskIds.value.includes(row.taskId)) {
      selectedTaskIds.value = [...selectedTaskIds.value, row.taskId]
    }
    selectedRowsSnapshot.value = {
      ...selectedRowsSnapshot.value,
      [row.taskId]: row
    }
    return
  }
  selectedTaskIds.value = selectedTaskIds.value.filter((taskId) => taskId !== row.taskId)
  const nextSnapshot = { ...selectedRowsSnapshot.value }
  delete nextSnapshot[row.taskId]
  selectedRowsSnapshot.value = nextSnapshot
}

function toggleCurrentPageSelection(checked: boolean) {
  pagedRows.value.forEach((row) => toggleTaskSelection(row, checked))
}

function handleSelectAllChange(event: Event) {
  toggleCurrentPageSelection((event.target as HTMLInputElement).checked)
}

function handleRowCheckboxChange(row: ExpensePaymentOrder, event: Event) {
  toggleTaskSelection(row, (event.target as HTMLInputElement).checked)
}

function openDetail(row: ExpensePaymentOrder) {
  void router.push({
    name: 'expense-document-detail',
    params: {
      documentCode: row.documentCode
    }
  })
}

async function startPayment(row: ExpensePaymentOrder) {
  try {
    await ElMessageBox.confirm(`确认将单据 ${row.documentCode} 推送到招商银行云直连？`, '发起支付', {
      confirmButtonText: '确认推送',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await expensePaymentApi.startTask(row.taskId)
    ElMessage.success('付款任务已推送至银行')
    await reloadAll({ clearSelection: true })
  } catch (error: unknown) {
    if (isCancel(error)) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '发起支付失败'))
  }
}

async function completePayment(row: ExpensePaymentOrder) {
  try {
    const { value } = await ElMessageBox.prompt(
      '可选填写备注，系统会将该单据标记为手动已支付，并跳过自动回单查询。',
      '手动标记已支付',
      {
        inputType: 'textarea',
        inputPlaceholder: '请输入备注（可空）',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )
    await expensePaymentApi.completeTask(row.taskId, { comment: value || '' })
    ElMessage.success('付款任务已标记为已支付')
    await reloadAll({ clearSelection: true })
  } catch (error: unknown) {
    if (isCancel(error)) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '手动标记已支付失败'))
  }
}

async function markException(row: ExpensePaymentOrder) {
  try {
    const { value } = await ElMessageBox.prompt('请输入异常说明。', '标记支付异常', {
      inputType: 'textarea',
      inputPlaceholder: '请输入异常原因',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    await expensePaymentApi.markException(row.taskId, { comment: value || '' })
    ElMessage.success('付款任务已标记异常')
    await reloadAll({ clearSelection: true })
  } catch (error: unknown) {
    if (isCancel(error)) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '标记支付异常失败'))
  }
}

function openStartPaymentDialog() {
  startPaymentDialogVisible.value = true
}

async function handleBulkDownload() {
  try {
    await expensePaymentApi.submitOrderExport(selectedTaskIds.value)
    ElMessage.success('下载任务已提交，请到下载中心查看进度')
    clearSelection()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '提交下载任务失败'))
  }
}

async function handleBulkReject() {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回付款单', {
      inputType: 'textarea',
      inputPlaceholder: '请输入驳回原因',
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消'
    })
    await expensePaymentApi.rejectTasks(selectedTaskIds.value, { comment: value || '' })
    ElMessage.success('付款任务已驳回')
    await reloadAll({ clearSelection: true })
  } catch (error: unknown) {
    if (isCancel(error)) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '驳回付款单失败'))
  }
}

function showPlaceholder(label: string) {
  ElMessage.info(`${label}功能建设中`)
}

function formatMoneyText(value: MoneyValue | undefined) {
  return `¥ ${formatMoney(value || 0)}`
}

function statusTagType(status?: string) {
  switch (status) {
    case 'PAYING':
      return 'warning'
    case 'PAYMENT_COMPLETED':
      return 'success'
    case 'PAYMENT_FINISHED':
      return 'primary'
    case 'PAYMENT_EXCEPTION':
      return 'danger'
    default:
      return 'info'
  }
}

function isCancel(error: unknown) {
  return error === 'cancel' || String(error).includes('cancel')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error && typeof error === 'object' && 'message' in error && typeof (error as { message?: unknown }).message === 'string') {
    return (error as { message: string }).message
  }
  return fallback
}
</script>

<style scoped>
.payment-orders-page {
  padding-bottom: 132px;
}

.detail-floating-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 24px;
  z-index: 30;
  display: flex;
  justify-content: center;
  padding: 0 20px;
}

.detail-floating-inner {
  width: min(1120px, 100%);
  border: 1px solid rgba(219, 234, 254, 0.92);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.96) 100%);
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(16px);
  padding: 20px 24px;
}

.payment-floating-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.payment-floating-selection {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
}

.payment-floating-selection__count {
  font-size: 16px;
  font-weight: 700;
  color: rgb(15 23 42);
}

.payment-floating-selection__hint {
  font-size: 13px;
  color: rgb(100 116 139);
}

.detail-floating-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
}

.detail-floating-actions__group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.detail-floating-button) {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 14px;
  font-size: 15px;
  line-height: 1.1;
}

:deep(.detail-floating-button--danger) {
  box-shadow: 0 10px 24px rgba(220, 38, 38, 0.18);
}

.payment-start-dialog {
  display: grid;
  gap: 14px;
}

.payment-start-option {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  border: 1px solid rgb(226 232 240);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.9) 0%, rgba(255, 255, 255, 1) 100%);
  padding: 18px 20px;
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.payment-start-option:hover {
  border-color: rgb(96 165 250);
  box-shadow: 0 14px 28px rgba(59, 130, 246, 0.12);
  transform: translateY(-1px);
}

.payment-start-option strong {
  font-size: 16px;
  color: rgb(15 23 42);
}

.payment-start-option span {
  font-size: 13px;
  line-height: 1.6;
  color: rgb(100 116 139);
}

@media (max-width: 1279px) {
  .payment-orders-page {
    padding-bottom: 168px;
  }

  .detail-floating-bar {
    bottom: 16px;
    padding: 0 12px;
  }

  .detail-floating-inner {
    padding: 18px 16px;
  }

  .detail-floating-actions,
  .detail-floating-actions__group {
    gap: 10px;
  }

  .detail-floating-actions__group {
    flex-basis: 100%;
  }

  :deep(.detail-floating-button) {
    min-height: 34px;
    padding: 0 14px;
    font-size: 14px;
  }
}
</style>
