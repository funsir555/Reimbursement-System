<template>
  <div class="expense-wb-page expense-wb-page--dense-list" data-testid="outstanding-page">
    <section
      class="expense-wb-stat-grid expense-wb-stat-grid--compact expense-wb-stat-grid--dense expense-wb-stat-grid--list-dense"
      data-testid="outstanding-stat-grid"
    >
      <article
        v-for="stat in stats"
        :key="stat.label"
        class="expense-wb-stat-card expense-wb-stat-card--compact expense-wb-stat-card--dense"
      >
        <div class="expense-wb-stat-card__top">
          <div>
            <p class="expense-wb-stat-card__label">{{ stat.label }}</p>
            <p class="expense-wb-stat-card__value">{{ stat.value }}</p>
          </div>
          <span class="expense-wb-stat-card__icon" :class="`expense-wb-stat-card__icon--${stat.tone}`">
            <el-icon :size="20">
              <component :is="stat.icon" />
            </el-icon>
          </span>
        </div>
      </article>
    </section>

    <el-card class="expense-wb-toolbar expense-wb-toolbar--compact expense-wb-toolbar--dense">
      <div
        class="expense-wb-toolbar__row expense-wb-toolbar__row--compact expense-wb-toolbar__row--dense expense-wb-toolbar__main"
        data-testid="outstanding-toolbar-main"
      >
        <div
          class="expense-wb-toolbar__heading expense-wb-toolbar__heading--compact expense-wb-toolbar__heading--inline"
          data-testid="outstanding-toolbar-heading"
        >
          <p class="expense-wb-toolbar__title">{{ pageTitle }}</p>
          <div class="expense-wb-toolbar__meta">
            <span class="expense-wb-soft-badge">{{ pageDescription }}</span>
          </div>
        </div>

        <div class="expense-wb-toolbar__actions">
          <el-button :icon="House" @click="goDashboard">返回首页</el-button>
          <el-button
            :icon="Download"
            :loading="exporting"
            data-testid="outstanding-export-trigger"
            @click="handleExport"
          >
            下载
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card
      class="expense-wb-panel expense-wb-table-shell expense-wb-table-shell--compact"
      data-testid="outstanding-table-shell"
    >
      <el-table :data="pagedDocuments" style="width: 100%" v-loading="loading">
        <el-table-column prop="documentCode" label="单据编号" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <button class="cursor-pointer font-medium text-blue-600 hover:underline" type="button" @click="openDetail(row)">
              {{ row.documentCode || row.no }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="documentTitle" label="单据标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="templateName" label="模板名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="documentStatusLabel" label="审批状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.documentStatusLabel || row.status)">
              {{ row.documentStatusLabel || row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交日期" width="168" show-overflow-tooltip />
        <el-table-column prop="amount" label="单据金额" width="140">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">¥ {{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="outstandingLabel" width="160">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">¥ {{ formatAmount(row.outstandingAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            <el-button
              link
              type="warning"
              size="small"
              :disabled="isZeroMoney(row.outstandingAmount)"
              @click="openWriteoffDialog(row)"
            >
              {{ actionLabel }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="expense-wb-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="documents.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <dashboard-writeoff-picker-dialog
      v-model="pickerVisible"
      :target-document-code="activeTargetDocumentCode"
      :action-label="actionLabel"
      @confirm="handleWriteoffConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  CircleCheckFilled,
  Coin,
  Download,
  House,
  Money,
  Tickets
} from '@element-plus/icons-vue'
import {
  asyncTaskApi,
  dashboardApi,
  type DashboardOutstandingKind,
  type ExpenseDocumentPickerItem,
  type ExpenseSummary
} from '@/api'
import { addMoney, formatMoney, isZeroMoney } from '@/utils/money'
import { openDownloadCenter } from '@/utils/downloadCenter'
import DashboardWriteoffPickerDialog from './components/DashboardWriteoffPickerDialog.vue'
import { notifyDashboardDataChanged } from './dashboardRecentModules'

const props = defineProps<{
  kind: DashboardOutstandingKind
}>()

const router = useRouter()
const loading = ref(false)
const exporting = ref(false)
const documents = ref<ExpenseSummary[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const pickerVisible = ref(false)
const activeTargetDocumentCode = ref('')

const pageTitle = computed(() => props.kind === 'LOAN' ? '待还款单据' : '待核销单据')
const pageDescription = computed(() => (
  props.kind === 'LOAN'
    ? '查看本人借款单中仍有待核销余额的单据'
    : '查看本人预付未到票且仍有待核销余额的报销单'
))
const actionLabel = computed(() => props.kind === 'LOAN' ? '核销还款' : '核销预付款')
const outstandingLabel = computed(() => props.kind === 'LOAN' ? '待还款金额' : '待核销金额')

const pagedDocuments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return documents.value.slice(start, start + pageSize.value)
})

const totalOutstandingAmount = computed(() =>
  documents.value.reduce((sum, item) => addMoney(sum, item.outstandingAmount || '0.00'), '0.00')
)

const stats = computed(() => [
  {
    label: '待处理单据',
    value: documents.value.length,
    icon: Tickets,
    tone: 'blue'
  },
  {
    label: outstandingLabel.value,
    value: `¥ ${formatAmount(totalOutstandingAmount.value)}`,
    icon: props.kind === 'LOAN' ? Money : Coin,
    tone: 'amber'
  },
  {
    label: '可直接处理',
    value: documents.value.filter((item) => !isZeroMoney(item.outstandingAmount)).length,
    icon: CircleCheckFilled,
    tone: 'green'
  }
])

onMounted(() => {
  void loadDocuments()
})

async function loadDocuments() {
  loading.value = true
  try {
    const res = await dashboardApi.listOutstandingDocuments(props.kind)
    documents.value = res.data || []
    currentPage.value = 1
  } catch (error: any) {
    ElMessage.error(error.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

function formatAmount(value?: string) {
  return formatMoney(value || '0.00')
}

function statusTagType(status?: string) {
  const map: Record<string, string> = {
    APPROVING: 'warning',
    审批中: 'warning',
    PENDING_PAYMENT: 'warning',
    待支付: 'warning',
    PAYING: 'warning',
    支付中: 'warning',
    APPROVED: 'success',
    已通过: 'success',
    PAYMENT_COMPLETED: 'success',
    已支付: 'success',
    COMPLETED: 'success',
    PAYMENT_FINISHED: 'success',
    已完成: 'success',
    REJECTED: 'danger',
    已驳回: 'danger',
    PAYMENT_EXCEPTION: 'danger',
    支付异常: 'danger',
    DRAFT: 'info',
    草稿: 'info',
    EXCEPTION: 'info',
    流程异常: 'info'
  }
  return map[status || ''] || 'info'
}

function openDetail(row: ExpenseSummary) {
  const documentCode = row.documentCode || row.no
  if (!documentCode) {
    return
  }
  void router.push(`/expense/documents/${encodeURIComponent(documentCode)}`)
}

function goDashboard() {
  void router.push('/dashboard')
}

async function handleExport() {
  const documentCodes = documents.value
    .map((item) => item.documentCode || item.no)
    .filter((item): item is string => Boolean(item))

  if (!documentCodes.length) {
    ElMessage.warning('当前没有可导出的单据')
    return
  }

  exporting.value = true
  try {
    await asyncTaskApi.exportExpenseScene({
      scene: 'OUTSTANDING',
      documentCodes,
      kind: props.kind
    })
    ElMessage.success('导出任务已提交，请到下载中心查看进度')
    openDownloadCenter()
  } catch (error: any) {
    ElMessage.error(error.message || '提交导出任务失败')
  } finally {
    exporting.value = false
  }
}

function openWriteoffDialog(row: ExpenseSummary) {
  activeTargetDocumentCode.value = row.documentCode || row.no || ''
  if (!activeTargetDocumentCode.value) {
    ElMessage.warning('未找到目标单据编码')
    return
  }
  pickerVisible.value = true
}

async function handleWriteoffConfirm(item: ExpenseDocumentPickerItem) {
  if (!activeTargetDocumentCode.value) {
    return
  }
  try {
    await dashboardApi.bindWriteoff({
      targetDocumentCode: activeTargetDocumentCode.value,
      sourceReportDocumentCode: item.documentCode
    })
    pickerVisible.value = false
    ElMessage.success(`${actionLabel.value}成功`)
    notifyDashboardDataChanged()
    await loadDocuments()
  } catch (error: any) {
    ElMessage.error(error.message || `${actionLabel.value}失败`)
  }
}
</script>
