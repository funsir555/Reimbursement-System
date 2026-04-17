<template>
  <div class="expense-wb-page expense-wb-page--list finance-review-voucher-page">
    <el-card class="expense-wb-toolbar expense-wb-toolbar--compact finance-review-voucher-toolbar">
      <div class="expense-wb-toolbar__row expense-wb-toolbar__row--compact finance-review-voucher-toolbar__top">
        <div class="expense-wb-toolbar__heading expense-wb-toolbar__heading--compact expense-wb-toolbar__heading--inline">
          <div>
            <p class="expense-wb-toolbar__title">审核凭证</p>
            <div class="expense-wb-toolbar__meta">
              <span class="expense-wb-soft-badge">当前公司 {{ financeCompany.currentCompanyName || '未设置' }}</span>
              <span class="expense-wb-soft-badge">当前月份 {{ filters.billMonth }}</span>
              <span class="expense-wb-soft-badge expense-wb-soft-badge--success">当前结果 {{ pager.total }}</span>
            </div>
          </div>
        </div>

        <div class="expense-wb-toolbar__actions">
          <el-button :loading="loading" @click="loadVouchers">刷新</el-button>
          <el-button type="primary" :disabled="!canReview" @click="handleVoucherStateAction('REVIEW')">审核</el-button>
          <el-button :disabled="!canUnreview" @click="handleVoucherStateAction('UNREVIEW')">反审核</el-button>
          <el-button :disabled="!canMarkError" @click="handleVoucherStateAction('TOGGLE_ERROR')">{{ effectiveErrorActionLabel }}</el-button>
          <el-button @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <div class="expense-wb-advanced-panel expense-wb-advanced-panel--embedded">
        <div class="expense-wb-advanced-grid finance-review-voucher-grid">
          <el-input v-model="filters.voucherNo" clearable placeholder="凭证号" />
          <el-select v-model="filters.csign" clearable placeholder="凭证类型">
            <el-option v-for="item in voucherTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-date-picker
            v-model="filters.billMonth"
            type="month"
            value-format="YYYY-MM"
            format="YYYY-MM"
            clearable
            placeholder="制单月份"
          />
          <el-input v-model="filters.summary" clearable placeholder="摘要" />
        </div>
      </div>
    </el-card>

    <el-card class="expense-wb-panel expense-wb-table-shell expense-wb-table-shell--compact finance-review-voucher-table">
      <el-table
        ref="tableRef"
        :data="pager.items"
        v-loading="loading"
        stripe
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @current-change="handleCurrentChange"
        @row-click="handleCurrentChange"
        @row-dblclick="openDetail"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="displayVoucherNo" label="凭证号" min-width="130" />
        <el-table-column prop="voucherTypeLabel" label="凭证类型" min-width="120" />
        <el-table-column prop="dbillDate" label="制单日期" min-width="120" />
        <el-table-column prop="iperiod" label="会计期间" min-width="100" />
        <el-table-column prop="summary" label="摘要" min-width="240" show-overflow-tooltip />
        <el-table-column prop="cbill" label="制单人" min-width="120" />
        <el-table-column prop="checkerName" label="审核人" min-width="120" />
        <el-table-column prop="idoc" label="附件张数" min-width="100" />
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="resolveStatusTagType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="expense-wb-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pager.total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadVouchers"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { financeApi, type FinanceVoucherMeta, type FinanceVoucherQueryParams, type FinanceVoucherSummary } from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type ReviewAction = 'REVIEW' | 'UNREVIEW' | 'TOGGLE_ERROR'
type EffectiveReviewAction = 'REVIEW' | 'UNREVIEW' | 'MARK_ERROR' | 'CLEAR_ERROR'

const DEFAULT_STATUS_FILTER = 'UNPOSTED,REVIEWED,ERROR'
const currentMonth = `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`

const router = useRouter()
const financeCompany = useFinanceCompanyStore()
const currentUser = readStoredUser()
const tableRef = ref<{ clearSelection?: () => void } | null>(null)

const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const voucherMeta = ref<FinanceVoucherMeta | null>(null)
const currentRow = ref<FinanceVoucherSummary | null>(null)
const selectedRows = ref<FinanceVoucherSummary[]>([])
const pager = reactive({
  total: 0,
  items: [] as FinanceVoucherSummary[]
})

const filters = reactive({
  voucherNo: '',
  csign: '',
  billMonth: currentMonth,
  summary: ''
})

const voucherTypeOptions = computed(() => voucherMeta.value?.voucherTypeOptions || [])
const canReview = computed(() => hasPermission('finance:general_ledger:review_voucher:review', currentUser))
const canUnreview = computed(() => hasPermission('finance:general_ledger:review_voucher:unreview', currentUser))
const canMarkError = computed(() => hasPermission('finance:general_ledger:review_voucher:mark_error', currentUser))
const effectiveErrorActionLabel = computed(() => {
  const targets = resolveActionTargets(false)
  if (!targets.length) {
    return '标记错误'
  }
  const statuses = new Set(targets.map((item) => item.status))
  return statuses.size === 1 && statuses.has('ERROR') ? '取消错误' : '标记错误'
})

watch(
  () => financeCompany.currentCompanyId,
  async (companyId, previousCompanyId) => {
    if (!companyId) return
    if (companyId !== previousCompanyId) {
      currentPage.value = 1
    }
    await loadVoucherMeta(companyId)
    await loadVouchers()
  },
  { immediate: true }
)

function buildQueryParams(): FinanceVoucherQueryParams | null {
  const companyId = financeCompany.currentCompanyId
  if (!companyId) {
    return null
  }
  return {
    companyId,
    voucherNo: filters.voucherNo.trim() || undefined,
    status: DEFAULT_STATUS_FILTER,
    csign: filters.csign || undefined,
    billMonth: filters.billMonth || undefined,
    summary: filters.summary.trim() || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
}

async function loadVoucherMeta(companyId = financeCompany.currentCompanyId) {
  if (!companyId) return
  try {
    const res = await financeApi.getVoucherMeta({ companyId })
    voucherMeta.value = res.data
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载凭证筛选项失败')
  }
}

async function loadVouchers() {
  const params = buildQueryParams()
  if (!params) return

  loading.value = true
  try {
    const res = await financeApi.listVouchers(params)
    pager.total = res.data.total
    pager.items = res.data.items || []
    currentRow.value = pager.items.find((item) => item.voucherNo === currentRow.value?.voucherNo) || pager.items[0] || null
    selectedRows.value = []
    tableRef.value?.clearSelection?.()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载审核凭证列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadVouchers()
}

function handleReset() {
  filters.voucherNo = ''
  filters.csign = ''
  filters.billMonth = currentMonth
  filters.summary = ''
  currentPage.value = 1
  loadVouchers()
}

function handlePageSizeChange() {
  currentPage.value = 1
  loadVouchers()
}

function handleSelectionChange(rows: FinanceVoucherSummary[]) {
  selectedRows.value = rows || []
}

function handleCurrentChange(row?: FinanceVoucherSummary | null) {
  currentRow.value = row || null
}

function resolveActionTargets(showWarning = true) {
  const targets = selectedRows.value.length ? selectedRows.value : currentRow.value ? [currentRow.value] : []
  if (!targets.length && showWarning) {
    ElMessage.warning('请先选择需要处理的凭证')
  }
  return targets
}

function validateTargetsForAction(action: EffectiveReviewAction, targets: FinanceVoucherSummary[]) {
  const statuses = Array.from(new Set(targets.map((item) => item.status)))
  if (statuses.length > 1) {
    ElMessage.warning('勾选凭证的状态不一致，请按同一状态分批处理')
    return false
  }

  const currentStatus = statuses[0]
  if (!currentStatus) {
    return false
  }
  switch (action) {
    case 'REVIEW':
      if (currentStatus !== 'UNPOSTED') {
        ElMessage.warning('只有未记账凭证可以审核')
        return false
      }
      return true
    case 'UNREVIEW':
      if (currentStatus !== 'REVIEWED') {
        ElMessage.warning('只有已审核凭证可以反审核')
        return false
      }
      return true
    case 'MARK_ERROR':
      if (!['UNPOSTED', 'REVIEWED'].includes(currentStatus)) {
        ElMessage.warning('只有未记账或已审核凭证可以标记错误')
        return false
      }
      return true
    case 'CLEAR_ERROR':
      if (currentStatus !== 'ERROR') {
        ElMessage.warning('只有错误标记凭证可以取消错误')
        return false
      }
      return true
    default:
      return false
  }
}

function resolveEffectiveAction(action: ReviewAction, targets: FinanceVoucherSummary[]): EffectiveReviewAction | null {
  if (action === 'REVIEW' || action === 'UNREVIEW') {
    return action
  }
  const statuses = new Set(targets.map((item) => item.status))
  if (statuses.size > 1) {
    ElMessage.warning('勾选凭证的状态不一致，请按同一状态分批处理')
    return null
  }
  return statuses.has('ERROR') ? 'CLEAR_ERROR' : 'MARK_ERROR'
}

async function handleVoucherStateAction(action: ReviewAction) {
  const companyId = financeCompany.currentCompanyId
  const targets = resolveActionTargets()
  if (!companyId || !targets.length) {
    return
  }

  const effectiveAction = resolveEffectiveAction(action, targets)
  if (!effectiveAction || !validateTargetsForAction(effectiveAction, targets)) {
    return
  }

  loading.value = true
  try {
    if (targets.length === 1) {
      const voucherNo = targets[0]?.voucherNo
      if (!voucherNo) return
      await runSingleAction(effectiveAction, companyId, voucherNo)
    } else {
      await financeApi.batchUpdateVoucherState({
        companyId,
        action: effectiveAction,
        voucherNos: targets.map((item) => item.voucherNo)
      })
      ElMessage.success(resolveActionSuccessText(effectiveAction, true))
    }
    await loadVouchers()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '更新凭证审核状态失败')
  } finally {
    loading.value = false
  }
}

async function runSingleAction(action: EffectiveReviewAction, companyId: string, voucherNo: string) {
  switch (action) {
    case 'REVIEW':
      await financeApi.reviewVoucher(companyId, voucherNo)
      break
    case 'UNREVIEW':
      await financeApi.unreviewVoucher(companyId, voucherNo)
      break
    case 'MARK_ERROR':
      await financeApi.markVoucherError(companyId, voucherNo)
      break
    case 'CLEAR_ERROR':
      await financeApi.clearVoucherError(companyId, voucherNo)
      break
  }
  ElMessage.success(resolveActionSuccessText(action, false))
}

function resolveActionSuccessText(action: EffectiveReviewAction, isBatch: boolean) {
  const suffix = isBatch ? '批量处理成功' : '处理成功'
  switch (action) {
    case 'REVIEW':
      return `审核${suffix}`
    case 'UNREVIEW':
      return `反审核${suffix}`
    case 'MARK_ERROR':
      return `标记错误${suffix}`
    case 'CLEAR_ERROR':
      return `取消错误${suffix}`
  }
}

function openDetail(row: FinanceVoucherSummary) {
  if (!row?.voucherNo) return
  router.push({
    name: 'finance-review-voucher-detail',
    params: { voucherNo: row.voucherNo }
  })
}

function resolveStatusTagType(status: string) {
  switch (status) {
    case 'POSTED':
      return 'success'
    case 'ERROR':
      return 'danger'
    case 'REVIEWED':
      return 'warning'
    default:
      return 'info'
  }
}

defineExpose({
  currentRow,
  selectedRows,
  effectiveErrorActionLabel,
  handleVoucherStateAction,
  openDetail,
  pager
})
</script>

<style scoped>
.finance-review-voucher-page {
  gap: 14px;
}

.finance-review-voucher-toolbar,
.finance-review-voucher-table {
  border-radius: 24px;
}

.finance-review-voucher-toolbar__top {
  align-items: flex-start;
}

.finance-review-voucher-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1400px) {
  .finance-review-voucher-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .finance-review-voucher-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}
</style>
