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
          <el-button :icon="House" @click="goDashboard">杩斿洖棣栭〉</el-button>
          <el-button
            :icon="Download"
            :loading="exporting"
            data-testid="outstanding-export-trigger"
            @click="handleExport"
          >
            涓嬭浇
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card
      class="expense-wb-panel expense-wb-table-shell expense-wb-table-shell--compact"
      data-testid="outstanding-table-shell"
    >
      <el-table :data="pagedDocuments" style="width: 100%" v-loading="loading">
        <el-table-column prop="documentCode" label="鍗曟嵁缂栧彿" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <button class="cursor-pointer font-medium text-blue-600 hover:underline" type="button" @click="openDetail(row)">
              {{ row.documentCode || row.no }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="documentTitle" label="鍗曟嵁鏍囬" min-width="220" show-overflow-tooltip />
        <el-table-column prop="templateName" label="妯℃澘鍚嶇О" min-width="160" show-overflow-tooltip />
        <el-table-column prop="documentStatusLabel" label="鐘舵€? width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.documentStatusLabel || row.status)">
              {{ row.documentStatusLabel || row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="鎻愪氦鏃ユ湡" width="168" show-overflow-tooltip />
        <el-table-column prop="amount" label="鍗曟嵁閲戦" width="140">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">楼 {{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="outstandingLabel" width="160">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">楼 {{ formatAmount(row.outstandingAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="鎿嶄綔" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">鏌ョ湅</el-button>
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

const pageTitle = computed(() => props.kind === 'LOAN' ? '寰呰繕娆惧崟鎹? : '寰呮牳閿€鍗曟嵁')
const pageDescription = computed(() => props.kind === 'LOAN'
  ? '鏌ョ湅鏈汉鍊熸鍗曚腑浠嶆湁寰呮牳閿€浣欓鐨勫崟鎹?
  : '鏌ョ湅鏈汉棰勪粯鏈埌绁ㄤ笖浠嶆湁寰呮牳閿€浣欓鐨勬姤閿€鍗?)
const actionLabel = computed(() => props.kind === 'LOAN' ? '鏍搁攢娆犳' : '鏍搁攢棰勪粯娆?)
const outstandingLabel = computed(() => props.kind === 'LOAN' ? '寰呰繕娆鹃噾棰? : '寰呮牳閿€閲戦')

const pagedDocuments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return documents.value.slice(start, start + pageSize.value)
})

const totalOutstandingAmount = computed(() =>
  documents.value.reduce((sum, item) => addMoney(sum, item.outstandingAmount || '0.00'), '0.00')
)

const stats = computed(() => [
  {
    label: '寰呭鐞嗗崟鎹?,
    value: documents.value.length,
    icon: Tickets,
    tone: 'blue'
  },
  {
    label: outstandingLabel.value,
    value: `楼 ${formatAmount(totalOutstandingAmount.value)}`,
    icon: props.kind === 'LOAN' ? Money : Coin,
    tone: 'amber'
  },
  {
    label: '鍙洿鎺ュ鐞?,
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
    ElMessage.error(error.message || '鍔犺浇鍒楄〃澶辫触')
  } finally {
    loading.value = false
  }
}

function formatAmount(value?: string) {
  return formatMoney(value || '0.00')
}

function statusTagType(status?: string) {
  const map: Record<string, string> = {
    审批中: 'warning',
    待支付: 'warning',
    支付中: 'warning',
    已通过: 'success',
    已支付: 'success',
    已完成: 'success',
    已驳回: 'danger',
    支付异常: 'danger',
    草稿: 'info',
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
    ElMessage.warning('褰撳墠娌℃湁鍙鍑虹殑鍗曟嵁')
    return
  }

  exporting.value = true
  try {
    await asyncTaskApi.exportExpenseScene({
      scene: 'OUTSTANDING',
      documentCodes,
      kind: props.kind
    })
    ElMessage.success('瀵煎嚭浠诲姟宸叉彁浜わ紝璇峰埌涓嬭浇涓績鏌ョ湅杩涘害')
    openDownloadCenter()
  } catch (error: any) {
    ElMessage.error(error.message || '鎻愪氦瀵煎嚭浠诲姟澶辫触')
  } finally {
    exporting.value = false
  }
}

function openWriteoffDialog(row: ExpenseSummary) {
  activeTargetDocumentCode.value = row.documentCode || row.no || ''
  if (!activeTargetDocumentCode.value) {
    ElMessage.warning('鏈壘鍒扮洰鏍囧崟鎹紪鐮?)
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
    ElMessage.success(`${actionLabel.value}鎴愬姛`)
    notifyDashboardDataChanged()
    await loadDocuments()
  } catch (error: any) {
    ElMessage.error(error.message || `${actionLabel.value}澶辫触`)
  }
}
</script>

