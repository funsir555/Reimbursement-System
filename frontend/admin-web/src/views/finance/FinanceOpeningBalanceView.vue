<template>
  <div class="opening-balance-page">
    <section class="ob-toolbar">
      <div class="ob-toolbar__main">
        <div class="ob-toolbar__actions">
          <el-button type="primary" :loading="toolbarLoading.save" @click="saveAll">保存</el-button>
          <el-button :loading="toolbarLoading.openBook" @click="runOpenBook">开账</el-button>
          <el-button :loading="toolbarLoading.carryForward" @click="runCarryForwardPreview">结转</el-button>
          <el-button :loading="toolbarLoading.trial" @click="runTrial">试算</el-button>
          <el-button :loading="toolbarLoading.reconcile" @click="runReconcile">对账</el-button>
        </div>
        <div class="ob-toolbar__filter">
          <el-button :type="filterPanelVisible ? 'primary' : 'default'" @click="filterPanelVisible = !filterPanelVisible">
            高级筛选
          </el-button>
        </div>
      </div>
      <div v-if="filterPanelVisible" class="ob-toolbar__panel">
        <div class="ob-toolbar__panel-grid">
          <el-select v-model="filterDraft.subjectType" clearable placeholder="科目类型">
            <el-option label="全部类型" value="" />
            <el-option label="资产" value="ASSET" />
            <el-option label="负债" value="LIABILITY" />
            <el-option label="权益" value="EQUITY" />
            <el-option label="成本" value="COST" />
            <el-option label="损益" value="PROFIT" />
          </el-select>
          <el-input v-model="filterDraft.subjectCode" clearable placeholder="科目编码" />
          <el-input v-model="filterDraft.subjectName" clearable placeholder="科目名称" />
          <el-select v-model="filterDraft.direction" clearable placeholder="方向">
            <el-option label="全部方向" value="" />
            <el-option label="借" value="借" />
            <el-option label="贷" value="贷" />
          </el-select>
          <el-select v-model="filterDraft.balance" clearable placeholder="余额">
            <el-option label="全部余额" value="" />
            <el-option label="正数余额" value="POSITIVE" />
            <el-option label="零余额" value="ZERO" />
            <el-option label="负数余额" value="NEGATIVE" />
          </el-select>
        </div>
        <div class="ob-toolbar__panel-actions">
          <el-button type="primary" @click="applyFilters">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>
    </section>

    <section class="ob-summary-card">
      <div class="ob-summary-card__main">
        <div class="ob-summary-card__item">
          <span>当前公司</span>
          <strong>{{ currentCompanyName || '未设置' }}</strong>
        </div>
        <div class="ob-summary-card__item">
          <span>年度</span>
          <strong>{{ filters.iyear }}</strong>
        </div>
        <div class="ob-summary-card__item">
          <span>期间</span>
          <strong>{{ filters.iperiod }} 月</strong>
        </div>
        <div class="ob-summary-card__item">
          <span>状态</span>
          <strong>{{ meta?.statusLabel || '未开账' }}</strong>
        </div>
        <div class="ob-summary-card__item ob-summary-card__item--trial" :class="trialSummaryClass">
          <span>试算结果</span>
          <strong>{{ trialSummaryText }}</strong>
          <em class="ob-summary-card__hint">{{ trialSummaryHint }}</em>
        </div>
      </div>
    </section>

    <section class="ob-table-card">
      <el-table
        v-loading="loading.rows"
        :data="visibleRows"
        row-key="subjectCode"
        stripe
        height="100%"
        :expand-row-keys="expandedRowKeys"
        :tree-props="{ children: 'visibleChildren' }"
        @expand-change="handleExpandChange"
      >
        <el-table-column prop="subjectCode" label="科目编码" min-width="140" />
        <el-table-column prop="subjectName" label="科目名称" min-width="240" />
        <el-table-column prop="balanceDirectionLabel" label="方向" min-width="90" />
        <el-table-column prop="cexchName" label="币种" min-width="110" />
        <el-table-column label="余额" min-width="220">
          <template #default="{ row }">
            <div class="ob-balance-cell">
              <template v-if="!row.editable">
                <span class="ob-balance-cell__text">{{ moneyText(row.mb) }}</span>
              </template>
              <template v-else-if="row.assistRequired">
                <el-button
                  link
                  type="primary"
                  class="ob-balance-action"
                  :class="{ 'is-locked': !canEditOpeningBalance }"
                  @click="handleAssistEntryClick(row)"
                >
                  {{ moneyText(row.mb) }} / 录入辅助
                </el-button>
              </template>
              <template v-else>
                <div
                  class="ob-balance-editor"
                  :class="{ 'is-locked': !canEditOpeningBalance }"
                  @click="handleDirectBalanceClick(row)"
                >
                  <money-input
                    v-model="row.draftBalance"
                    allow-negative
                    :disabled="!canEditOpeningBalance"
                    @update:model-value="handleRowDraftChange(row, $event)"
                    @blur="handleRowDraftChange(row, row.draftBalance)"
                    @change="handleRowDraftChange(row, row.draftBalance)"
                  />
                </div>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="reconcileResult" class="ob-result-card">
      <div v-if="reconcileResult" class="ob-result-block">
        <h2>对账结果</h2>
        <p>
          差异科目 {{ reconcileResult.differenceSubjects.length }} 条，缺失辅助
          {{ reconcileResult.missingAssistSubjects.length }} 条，非法组合
          {{ reconcileResult.illegalAssistMessages.length }} 条
        </p>
      </div>
    </section>

    <el-dialog v-model="assistDialogVisible" title="辅助核算期初录入" width="980px">
      <div class="ob-assist-head">
        <span>{{ activeAssistRow?.subjectCode }}</span>
        <strong>{{ activeAssistRow?.subjectName }}</strong>
      </div>
      <div class="ob-assist-actions">
        <el-button @click="appendAssistLine">新增明细</el-button>
      </div>
      <el-table :data="assistLines" row-key="rowKey">
        <el-table-column v-if="activeAssistConfig.bdept" label="部门" min-width="140">
          <template #default="{ row }">
            <department-tree-select v-model="row.cdeptId" :options="meta?.departmentOptions || []" />
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bperson" label="人员" min-width="140">
          <template #default="{ row }">
            <employee-tree-select
              v-model="row.cpersonId"
              :departments="meta?.departmentOptions || []"
              :employees="meta?.employeeDirectory || []"
              label-mode="finance-assist"
              clearable
            />
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bcus" label="客户" min-width="160">
          <template #default="{ row }">
            <finance-assist-option-select
              v-model="row.ccusId"
              :options="meta?.customerOptions || []"
              addable
              add-text="增加"
              @request-add="openOpeningCustomerCreateDialog(row)"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bsup" label="供应商" min-width="160">
          <template #default="{ row }">
            <finance-assist-option-select
              v-model="row.csupId"
              :options="meta?.supplierOptions || []"
              addable
              add-text="增加"
              @request-add="openOpeningSupplierCreateDialog(row)"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bitem" label="项目分类" min-width="150">
          <template #default="{ row }">
            <finance-assist-option-select
              v-model="row.citemClass"
              :options="projectClassOptionsForRow(row)"
              :disabled="Boolean(activeAssistRow?.cassItem)"
              @change="handleAssistProjectClassChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bitem" label="项目" min-width="170">
          <template #default="{ row }">
            <finance-assist-option-select
              v-model="row.citemId"
              :options="projectOptionsForRow(row)"
              addable
              add-text="增加"
              :add-disabled="!resolvedOpeningProjectClassCode(row)"
              add-disabled-message="请先选择项目分类"
              @request-add="openOpeningProjectCreateDialog(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="余额" min-width="180">
          <template #default="{ row }">
            <money-input v-model="row.mb" allow-negative />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeAssistLine($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="assistDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssistDialog">确定</el-button>
      </template>
    </el-dialog>
    <finance-customer-archive-dialog
      ref="customerArchiveDialogRef"
      :company-id="filters.companyId"
      :company-name="currentCompanyName"
      @saved="handleOpeningCustomerCreated"
    />
    <finance-supplier-archive-dialog
      ref="supplierArchiveDialogRef"
      :company-id="filters.companyId"
      :company-name="currentCompanyName"
      @saved="handleOpeningSupplierCreated"
    />
    <finance-project-archive-dialog
      ref="projectArchiveDialogRef"
      :company-id="filters.companyId"
      :company-name="currentCompanyName"
      :project-class-options="meta?.projectClassOptions || []"
      @saved="handleOpeningProjectCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  openingBalanceApi,
  type OpeningAssistBalanceLine,
  type OpeningBalanceAssistDraftLine,
  type OpeningBalanceCarryForwardPreviewResult,
  type OpeningBalanceCommitPayload,
  type OpeningBalanceMeta,
  type OpeningBalanceReconcileResult,
  type OpeningBalanceRow,
  type OpeningBalanceTrialResult
} from '@/api'
import FinanceAssistOptionSelect from '@/components/finance/FinanceAssistOptionSelect.vue'
import FinanceCustomerArchiveDialog from '@/components/finance/FinanceCustomerArchiveDialog.vue'
import FinanceProjectArchiveDialog from '@/components/finance/FinanceProjectArchiveDialog.vue'
import FinanceSupplierArchiveDialog from '@/components/finance/FinanceSupplierArchiveDialog.vue'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import DepartmentTreeSelect from '@/components/inputs/DepartmentTreeSelect.vue'
import EmployeeTreeSelect from '@/components/inputs/EmployeeTreeSelect.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'
import { formatMoney, normalizeMoneyValue } from '@/utils/money'


type OpeningBalanceTableRow = Omit<OpeningBalanceRow, 'children'> & {
  draftBalance: string
  savedBalance: string
  children?: OpeningBalanceTableRow[]
  visibleChildren?: OpeningBalanceTableRow[]
}

type AssistDialogLine = OpeningAssistBalanceLine & {
  rowKey: string
  mb: string
}

type DraftSource = '' | 'manual' | 'assist' | 'carry-forward'
type OpeningBalanceFilterState = {
  subjectType: '' | 'ASSET' | 'LIABILITY' | 'EQUITY' | 'COST' | 'PROFIT'
  subjectCode: string
  subjectName: string
  direction: '' | '借' | '贷'
  balance: '' | 'POSITIVE' | 'ZERO' | 'NEGATIVE'
}

type PersistedExpandedMap = Record<string, string[]>

const SWITCH_GUARD_KEY = 'finance-opening-balance-draft-guard'
const EXPANDED_STORAGE_KEY = 'finance-opening-balance-expanded-keys'
const OPEN_BOOK_REQUIRED_MESSAGE = '当前期间尚未开账，请先开账'
const OPENING_BALANCE_MONEY_OPTIONS = Object.freeze({
  allowNegative: true,
  fallback: '0.00'
})

const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()
const meta = ref<OpeningBalanceMeta | null>(null)
const rows = ref<OpeningBalanceTableRow[]>([])
const loading = reactive({ meta: false, rows: false })
const toolbarLoading = reactive({ save: false, openBook: false, carryForward: false, trial: false, reconcile: false })
const filters = reactive({
  companyId: '',
  iyear: 0,
  iperiod: 0
})
const trialResult = ref<OpeningBalanceTrialResult | null>(null)
const reconcileResult = ref<OpeningBalanceReconcileResult | null>(null)
const assistDialogVisible = ref(false)
const activeAssistRow = ref<OpeningBalanceTableRow | null>(null)
const assistLines = ref<AssistDialogLine[]>([])
const manualDrafts = ref<Record<string, string>>({})
const assistDrafts = ref<Record<string, OpeningAssistBalanceLine[]>>({})
const persistedBalances = ref<Record<string, string>>({})
const persistedAssistSignatures = ref<Record<string, string>>({})
const expandedRowKeys = ref<string[]>([])
const filterPanelVisible = ref(false)
const filterDraft = reactive(createOpeningBalanceFilters())
const appliedFilters = ref<OpeningBalanceFilterState>(createOpeningBalanceFilters())
const draftSource = ref<DraftSource>('')
const lastSavedAt = ref('')
const suppressContextWatch = ref(false)
const customerArchiveDialogRef = ref<InstanceType<typeof FinanceCustomerArchiveDialog> | null>(null)
const supplierArchiveDialogRef = ref<InstanceType<typeof FinanceSupplierArchiveDialog> | null>(null)
const projectArchiveDialogRef = ref<InstanceType<typeof FinanceProjectArchiveDialog> | null>(null)
const activeAssistLineForCreate = ref<AssistDialogLine | null>(null)
let assistSeed = 0

const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const flatRows = computed(() => flattenRows(rows.value))
const visibleRows = computed(() => filterOpeningBalanceRows(rows.value, appliedFilters.value))
const hasUnsavedChanges = computed(() => hasDraftContent())
const canEditOpeningBalance = computed(() => Boolean(meta.value?.opened))
const activeAssistConfig = computed(() => ({
  bdept: activeAssistRow.value?.bdept === 1,
  bperson: activeAssistRow.value?.bperson === 1,
  bcus: activeAssistRow.value?.bcus === 1,
  bsup: activeAssistRow.value?.bsup === 1,
  bitem: activeAssistRow.value?.bitem === 1
}))
const saveStatusText = computed(() => {
  if (toolbarLoading.save) {
    return '保存中'
  }
  if (hasUnsavedChanges.value) {
    if (draftSource.value === 'carry-forward') {
      return '结转结果待保存'
    }
    if (draftSource.value === 'assist') {
      return '辅助明细待保存'
    }
    return '期初余额待保存'
  }
  if (lastSavedAt.value) {
    return '已保存'
  }
  return '已同步'
})
const saveStatusHint = computed(() => {
  if (toolbarLoading.save) {
    return '正在提交到数据库'
  }
  if (hasUnsavedChanges.value) {
    if (draftSource.value === 'carry-forward') {
      return '结转预览仅保留在当前页面'
    }
    if (draftSource.value === 'assist') {
      return '辅助明细尚未提交'
    }
    return '请点击保存写入数据库'
  }
  if (lastSavedAt.value) {
    return `最近保存于 ${lastSavedAt.value}`
  }
  return '当前页面与数据库一致'
})
const saveStatusClass = computed(() => ({
  'is-dirty': hasUnsavedChanges.value,
  'is-saved': !hasUnsavedChanges.value && Boolean(lastSavedAt.value)
}))
const trialSummaryText = computed(() => {
  if (!trialResult.value) {
    return '尚未试算'
  }
  return trialResult.value.balanced ? '试算平衡' : '试算不平衡'
})
const trialSummaryHint = computed(() => {
  if (!trialResult.value) {
    return '请在保存后执行试算'
  }
  return `借方 ${moneyText(trialResult.value.totalDebit)}，贷方 ${moneyText(trialResult.value.totalCredit)}，差额 ${moneyText(trialResult.value.difference)}`
})
const trialSummaryClass = computed(() => ({
  'is-success': Boolean(trialResult.value?.balanced),
  'is-warning': Boolean(trialResult.value) && !trialResult.value?.balanced
}))

watch(
  () => [financeCompany.currentCompanyId, financePeriod.currentYearPeriod] as const,
  async ([companyId, yearPeriod]) => {
    if (suppressContextWatch.value || !companyId || !financePeriod.hasPeriodContext || !yearPeriod) {
      return
    }
    const nextYear = financePeriod.currentYear
    const nextPeriod = financePeriod.currentPeriod
    const contextChanged =
      companyId !== filters.companyId || nextYear !== filters.iyear || nextPeriod !== filters.iperiod

    if (!contextChanged) {
      return
    }

    if (filters.companyId && hasUnsavedChanges.value) {
      const allowed = await confirmDiscardDrafts()
      if (!allowed) {
        suppressContextWatch.value = true
        if (companyId !== filters.companyId) {
          financeCompany.applyCurrentCompany(filters.companyId)
        }
        if (nextYear !== filters.iyear || nextPeriod !== filters.iperiod) {
          financePeriod.switchPeriod(filters.iyear, filters.iperiod)
        }
        suppressContextWatch.value = false
        return
      }
      discardDrafts()
    }

    filters.companyId = companyId
    filters.iyear = nextYear
    filters.iperiod = nextPeriod
    await loadMeta()
    await loadRowsFromServer()
  },
  { immediate: true }
)

onMounted(() => {
  financeCompany.registerSwitchGuard(SWITCH_GUARD_KEY, async () => {
    if (!hasUnsavedChanges.value) {
      return true
    }
    const allowed = await confirmDiscardDrafts()
    if (allowed) {
      discardDrafts()
    }
    return allowed
  })
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  financeCompany.unregisterSwitchGuard(SWITCH_GUARD_KEY)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

async function loadMeta() {
  if (!filters.companyId) return
  loading.meta = true
  try {
    const res = await openingBalanceApi.getMeta({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    meta.value = res.data
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载期初余额元数据失败')
  } finally {
    loading.meta = false
  }
}

async function loadRowsFromServer() {
  if (!filters.companyId) return
  loading.rows = true
  try {
    const res = await openingBalanceApi.listRows({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    persistedBalances.value = collectPersistedBalances(res.data || [])
    rows.value = decorateRows(res.data || [], persistedBalances.value)
    recalculateTreeBalances(rows.value)
    discardDrafts(false)
    expandedRowKeys.value = restoreExpandedRowKeys(rows.value)
    persistExpandedRowKeys()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载期初余额失败')
  } finally {
    loading.rows = false
  }
}

function decorateRows(source: OpeningBalanceRow[], balanceMap: Record<string, string>): OpeningBalanceTableRow[] {
  return source.map((item) => {
    const currentBalance = normalizeOpeningBalanceMoney(item.mb)
    return {
      ...item,
      mb: currentBalance,
      draftBalance: currentBalance,
      savedBalance: balanceMap[item.subjectCode] || currentBalance,
      children: decorateRows(item.children || [], balanceMap)
    } satisfies OpeningBalanceTableRow
  })
}

function collectPersistedBalances(source: OpeningBalanceRow[]): Record<string, string> {
  const result: Record<string, string> = {}
  for (const row of flattenRows(source)) {
    result[row.subjectCode] = normalizeOpeningBalanceMoney(row.mb)
  }
  return result
}

function flattenRows<T extends { children?: T[] }>(source: T[]): T[] {
  const result: T[] = []
  for (const item of source) {
    result.push(item)
    if (item.children?.length) {
      result.push(...flattenRows(item.children))
    }
  }
  return result
}

function createOpeningBalanceFilters(): OpeningBalanceFilterState {
  return {
    subjectType: '',
    subjectCode: '',
    subjectName: '',
    direction: '',
    balance: ''
  }
}

function applyFilters() {
  appliedFilters.value = {
    subjectType: filterDraft.subjectType,
    subjectCode: filterDraft.subjectCode.trim(),
    subjectName: filterDraft.subjectName.trim(),
    direction: filterDraft.direction,
    balance: filterDraft.balance
  }
}

function resetFilters() {
  Object.assign(filterDraft, createOpeningBalanceFilters())
  appliedFilters.value = createOpeningBalanceFilters()
}

function filterOpeningBalanceRows(source: OpeningBalanceTableRow[], filtersState: OpeningBalanceFilterState) {
  return source.filter((row) => applyOpeningBalanceRowFilter(row, filtersState))
}

function applyOpeningBalanceRowFilter(row: OpeningBalanceTableRow, filtersState: OpeningBalanceFilterState): boolean {
  const childMatches: OpeningBalanceTableRow[] = (row.children || []).filter((child) =>
    applyOpeningBalanceRowFilter(child, filtersState)
  )
  row.visibleChildren = childMatches
  if (!hasActiveFilters(filtersState)) {
    row.visibleChildren = row.children || []
    return true
  }
  return matchesOpeningBalanceFilters(row, filtersState) || childMatches.length > 0
}

function hasActiveFilters(filtersState: OpeningBalanceFilterState) {
  return Boolean(
    filtersState.subjectType ||
      filtersState.subjectCode ||
      filtersState.subjectName ||
      filtersState.direction ||
      filtersState.balance
  )
}

function matchesOpeningBalanceFilters(row: OpeningBalanceTableRow, filtersState: OpeningBalanceFilterState) {
  const subjectCodeMatched = !filtersState.subjectCode || row.subjectCode.includes(filtersState.subjectCode)
  const subjectNameMatched = !filtersState.subjectName || row.subjectName.includes(filtersState.subjectName)
  const directionMatched = !filtersState.direction || row.balanceDirectionLabel === filtersState.direction
  const subjectTypeMatched = !filtersState.subjectType || row.subjectCategory === filtersState.subjectType
  const balanceMatched = matchesBalanceFilter(row.mb, filtersState.balance)

  return subjectCodeMatched && subjectNameMatched && directionMatched && subjectTypeMatched && balanceMatched
}

function matchesBalanceFilter(balance: string | number, balanceFilter: OpeningBalanceFilterState['balance']) {
  const cents = toCents(balance)
  if (!balanceFilter) {
    return true
  }
  if (balanceFilter === 'POSITIVE') {
    return cents > 0
  }
  if (balanceFilter === 'ZERO') {
    return cents === 0
  }
  return cents < 0
}

function ensureOpeningBookReady() {
  if (canEditOpeningBalance.value) {
    return true
  }
  ElMessage.warning(OPEN_BOOK_REQUIRED_MESSAGE)
  return false
}

function handleDirectBalanceClick(_row: OpeningBalanceTableRow) {
  ensureOpeningBookReady()
}

function handleAssistEntryClick(row: OpeningBalanceTableRow) {
  if (!ensureOpeningBookReady()) {
    return
  }
  void openAssistDialog(row)
}

function handleRowDraftChange(row: OpeningBalanceTableRow, value: string | number) {
  if (!canEditOpeningBalance.value) {
    return
  }
  const normalized = normalizeOpeningBalanceMoney(value ?? row.draftBalance)
  row.draftBalance = normalized
  row.mb = normalized
  if (normalized === row.savedBalance) {
    delete manualDrafts.value[row.subjectCode]
  } else {
    manualDrafts.value[row.subjectCode] = normalized
    draftSource.value = 'manual'
  }
  syncDraftSource()
  recalculateTreeBalances(rows.value)
}

async function openAssistDialog(row: OpeningBalanceTableRow) {
  if (!ensureOpeningBookReady()) {
    return
  }
  activeAssistLineForCreate.value = null
  activeAssistRow.value = row
  const draftedLines = assistDrafts.value[row.subjectCode]
  if (draftedLines) {
    assistLines.value = draftedLines.length ? draftedLines.map((item) => toAssistDialogLine(item)) : [toAssistDialogLine({ citemClass: row.cassItem })]
    assistDialogVisible.value = true
    return
  }

  try {
    const res = await openingBalanceApi.getAssistBalances(row.subjectCode, {
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    const sourceLines = res.data || []
    persistedAssistSignatures.value[row.subjectCode] = buildAssistSignature(sourceLines)
    assistLines.value = sourceLines.length ? sourceLines.map((item) => toAssistDialogLine(item)) : [toAssistDialogLine({ citemClass: row.cassItem })]
    assistDialogVisible.value = true
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载辅助期初失败')
  }
}

function appendAssistLine() {
  assistLines.value.push(toAssistDialogLine({ citemClass: activeAssistRow.value?.cassItem }))
}

function removeAssistLine(index: number) {
  assistLines.value.splice(index, 1)
  if (!assistLines.value.length) {
    appendAssistLine()
  }
}

function toAssistDialogLine(line: Partial<OpeningAssistBalanceLine> = {}): AssistDialogLine {
  const projectClass = activeAssistRow.value?.cassItem || line.citemClass || ''
  return {
    rowKey: `assist-${assistSeed++}`,
    cdeptId: line.cdeptId || '',
    cpersonId: line.cpersonId || '',
    ccusId: line.ccusId || '',
    csupId: line.csupId || '',
    citemClass: projectClass,
    citemId: line.citemId || '',
    mb: normalizeOpeningBalanceMoney(line.mb),
    mbF: line.mbF,
    nbS: line.nbS
  }
}

function handleAssistProjectClassChange(line: AssistDialogLine) {
  line.citemId = ''
}

function projectClassOptionsForRow(_row: AssistDialogLine) {
  if (activeAssistRow.value?.cassItem) {
    return (meta.value?.projectClassOptions || []).filter((item) => item.value === activeAssistRow.value?.cassItem)
  }
  return meta.value?.projectClassOptions || []
}

function projectOptionsForRow(row: AssistDialogLine) {
  const projectClass = activeAssistRow.value?.cassItem || row.citemClass
  return (meta.value?.projectOptions || []).filter((item) => item.parentValue === projectClass)
}

function resolvedOpeningProjectClassCode(row?: AssistDialogLine | null) {
  return activeAssistRow.value?.cassItem || row?.citemClass || ''
}

async function refreshOpeningAssistMeta() {
  await loadMeta()
}

function openOpeningCustomerCreateDialog(row: AssistDialogLine) {
  activeAssistLineForCreate.value = row
  customerArchiveDialogRef.value?.openCreateDialog()
}

function openOpeningSupplierCreateDialog(row: AssistDialogLine) {
  activeAssistLineForCreate.value = row
  supplierArchiveDialogRef.value?.openCreateDialog()
}

function openOpeningProjectCreateDialog(row: AssistDialogLine) {
  activeAssistLineForCreate.value = row
  projectArchiveDialogRef.value?.openCreateDialog(resolvedOpeningProjectClassCode(row))
}

async function handleOpeningCustomerCreated(customerCode: string) {
  await refreshOpeningAssistMeta()
  if (activeAssistLineForCreate.value) {
    activeAssistLineForCreate.value.ccusId = customerCode
  }
}

async function handleOpeningSupplierCreated(vendorCode: string) {
  await refreshOpeningAssistMeta()
  if (activeAssistLineForCreate.value) {
    activeAssistLineForCreate.value.csupId = vendorCode
  }
}

async function handleOpeningProjectCreated(projectCode: string) {
  await refreshOpeningAssistMeta()
  if (!activeAssistLineForCreate.value) {
    return
  }
  const projectClassCode = resolvedOpeningProjectClassCode(activeAssistLineForCreate.value)
  if (projectClassCode) {
    activeAssistLineForCreate.value.citemClass = projectClassCode
  }
  activeAssistLineForCreate.value.citemId = projectCode
}

function saveAssistDialog() {
  if (!activeAssistRow.value) return
  const subjectCode = activeAssistRow.value.subjectCode
  const payloadLines = normalizeAssistPayloadLines(assistLines.value)
  const currentSignature = buildAssistSignature(payloadLines)
  const persistedSignature = persistedAssistSignatures.value[subjectCode] || '[]'

  if (currentSignature === persistedSignature) {
    delete assistDrafts.value[subjectCode]
  } else {
    assistDrafts.value[subjectCode] = payloadLines
    draftSource.value = 'assist'
  }

  const total = payloadLines.reduce((sum, item) => sum + toCents(item.mb), 0)
  activeAssistRow.value.mb = centsToMoney(total)
  activeAssistRow.value.draftBalance = activeAssistRow.value.mb
  assistDialogVisible.value = false
  activeAssistLineForCreate.value = null
  syncDraftSource()
  recalculateTreeBalances(rows.value)
}

function normalizeAssistPayloadLines(source: AssistDialogLine[]) {
  return source
    .map((item) => ({
      cdeptId: item.cdeptId || undefined,
      cpersonId: item.cpersonId || undefined,
      ccusId: item.ccusId || undefined,
      csupId: item.csupId || undefined,
      citemClass: (activeAssistRow.value?.cassItem || item.citemClass) || undefined,
      citemId: item.citemId || undefined,
      mb: normalizeOpeningBalanceMoney(item.mb)
    }))
    .filter((item) => {
      const amount = toCents(item.mb)
      return amount !== 0 || Boolean(item.cdeptId || item.cpersonId || item.ccusId || item.csupId || item.citemClass || item.citemId)
    })
}

function buildAssistSignature(source: OpeningAssistBalanceLine[]) {
  const normalized = source
    .map((item) => ({
      cdeptId: item.cdeptId || '',
      cpersonId: item.cpersonId || '',
      ccusId: item.ccusId || '',
      csupId: item.csupId || '',
      citemClass: item.citemClass || '',
      citemId: item.citemId || '',
      mb: normalizeOpeningBalanceMoney(item.mb)
    }))
    .sort((left, right) =>
      `${left.cdeptId}|${left.cpersonId}|${left.ccusId}|${left.csupId}|${left.citemClass}|${left.citemId}|${left.mb}`.localeCompare(
        `${right.cdeptId}|${right.cpersonId}|${right.ccusId}|${right.csupId}|${right.citemClass}|${right.citemId}|${right.mb}`
      )
    )
  return JSON.stringify(normalized)
}

async function saveAll() {
  if (!meta.value?.opened) {
    ElMessage.warning('当前期间尚未开账，不能保存期初余额')
    return
  }
  const payload = buildCommitPayload()
  if (!payload.rows.length && !payload.assistLines.length) {
    ElMessage.warning('当前没有需要保存的期初余额')
    return
  }

  toolbarLoading.save = true
  try {
    await openingBalanceApi.commit(payload)
    lastSavedAt.value = formatDateTime(new Date())
    draftSource.value = ''
    ElMessage.success('期初余额已保存')
    trialResult.value = null
    reconcileResult.value = null
    await loadRowsFromServer()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '保存期初余额失败')
  } finally {
    toolbarLoading.save = false
  }
}

function buildCommitPayload(): OpeningBalanceCommitPayload {
  const rowsPayload = Object.entries(manualDrafts.value).map(([subjectCode, mb]) => ({
    subjectCode,
    mb
  }))
  const assistLinesPayload: OpeningBalanceAssistDraftLine[] = Object.entries(assistDrafts.value).map(([subjectCode, lines]) => ({
    subjectCode,
    lines
  }))
  return {
    companyId: filters.companyId,
    iyear: filters.iyear,
    iperiod: filters.iperiod,
    rows: rowsPayload,
    assistLines: assistLinesPayload
  }
}

async function runOpenBook() {
  toolbarLoading.openBook = true
  try {
    const res = await openingBalanceApi.openBook({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    ElMessage.success(res.data.message || '开账任务已提交')
    await refreshAfterTask()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '开账失败')
  } finally {
    toolbarLoading.openBook = false
  }
}

async function runCarryForwardPreview() {
  if (hasUnsavedChanges.value) {
    const confirmed = await confirmOverwriteByCarryForward()
    if (!confirmed) {
      return
    }
    discardDrafts()
  }

  toolbarLoading.carryForward = true
  try {
    const res = await openingBalanceApi.carryForwardPreview({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    applyCarryForwardPreview(res.data)
    ElMessage.success('结转结果已生成，请点击保存')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '结转预览失败')
  } finally {
    toolbarLoading.carryForward = false
  }
}

function applyCarryForwardPreview(preview: OpeningBalanceCarryForwardPreviewResult) {
  const nextRows = decorateRows(preview.rows || [], persistedBalances.value)
  rows.value = nextRows
  manualDrafts.value = {}
  assistDrafts.value = {}

  for (const row of flatRowsFrom(nextRows)) {
    const currentBalance = normalizeOpeningBalanceMoney(row.mb)
    row.draftBalance = currentBalance
    if (row.editable && !row.assistRequired && currentBalance !== row.savedBalance) {
      manualDrafts.value[row.subjectCode] = currentBalance
    }
  }

  for (const item of preview.assistLines || []) {
    assistDrafts.value[item.subjectCode] = (item.lines || []).map((line) => ({
      ...line,
      mb: normalizeOpeningBalanceMoney(line.mb)
    }))
  }

  draftSource.value = hasDraftContent() ? 'carry-forward' : ''
  recalculateTreeBalances(rows.value)
  expandedRowKeys.value = restoreExpandedRowKeys(rows.value)
  persistExpandedRowKeys()
}

async function runTrial() {
  if (hasUnsavedChanges.value) {
    ElMessage.warning('请先保存期初余额后再试算')
    return
  }

  toolbarLoading.trial = true
  try {
    const res = await openingBalanceApi.trialBalance({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    trialResult.value = res.data
    if (res.data.balanced) {
      ElMessage.success('期初试算平衡')
      return
    }
    ElMessage.warning(`试算不平衡，差额 ${moneyText(res.data.difference)}`)
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '试算失败')
  } finally {
    toolbarLoading.trial = false
  }
}

async function runReconcile() {
  if (hasUnsavedChanges.value) {
    ElMessage.warning('请先保存期初余额后再对账')
    return
  }

  toolbarLoading.reconcile = true
  try {
    const res = await openingBalanceApi.reconcile({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    reconcileResult.value = res.data
    if (res.data.matched) {
      ElMessage.success('辅助核算对账一致')
      return
    }
    await ElMessageBox.alert(
      [
        `差异科目 ${res.data.differenceSubjects.length} 条`,
        `缺失辅助 ${res.data.missingAssistSubjects.length} 条`,
        `非法辅助 ${res.data.illegalAssistMessages.length} 条`
      ].join('；'),
      '对账结果',
      { type: 'warning' }
    )
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '对账失败')
  } finally {
    toolbarLoading.reconcile = false
  }
}

async function refreshAfterTask() {
  await new Promise((resolve) => window.setTimeout(resolve, 900))
  await loadMeta()
  await loadRowsFromServer()
}

function discardDrafts(resetView = true) {
  manualDrafts.value = {}
  assistDrafts.value = {}
  draftSource.value = ''
  activeAssistRow.value = null
  activeAssistLineForCreate.value = null
  assistLines.value = []
  assistDialogVisible.value = false
  if (!resetView) {
    return
  }
  for (const row of flatRows.value) {
    row.mb = row.savedBalance
    row.draftBalance = row.savedBalance
  }
  recalculateTreeBalances(rows.value)
}

async function confirmDiscardDrafts() {
  try {
    await ElMessageBox.confirm('当前页面有未保存的期初余额，是否放弃这些修改？', '未保存内容', {
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

async function confirmOverwriteByCarryForward() {
  try {
    await ElMessageBox.confirm('将用结转结果覆盖当前未保存内容，是否继续？', '结转确认', {
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!hasUnsavedChanges.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

function handleExpandChange(
  row: OpeningBalanceTableRow,
  expandedState: boolean | OpeningBalanceTableRow[]
) {
  const isExpanded =
    typeof expandedState === 'boolean'
      ? expandedState
      : Array.isArray(expandedState) && expandedState.some((item) => item.subjectCode === row.subjectCode)
  if (isExpanded) {
    if (!expandedRowKeys.value.includes(row.subjectCode)) {
      expandedRowKeys.value = [...expandedRowKeys.value, row.subjectCode]
    }
  } else {
    expandedRowKeys.value = expandedRowKeys.value.filter((item) => item !== row.subjectCode)
  }
  persistExpandedRowKeys()
}

function recalculateTreeBalances(source: OpeningBalanceTableRow[]): void {
  for (const row of source) {
    recalculateNodeBalance(row)
  }
}

function recalculateNodeBalance(row: OpeningBalanceTableRow): number {
  if (!row.children?.length) {
    row.mb = normalizeOpeningBalanceMoney(row.mb ?? row.draftBalance)
    return toCents(row.mb)
  }
  const total: number = row.children.reduce((sum, child) => sum + recalculateNodeBalance(child), 0)
  row.mb = centsToMoney(total)
  row.draftBalance = row.mb
  return total
}

function flatRowsFrom(source: OpeningBalanceTableRow[]): OpeningBalanceTableRow[] {
  return flattenRows(source)
}

function hasDraftContent() {
  return Object.keys(manualDrafts.value).length > 0 || Object.keys(assistDrafts.value).length > 0
}

function syncDraftSource() {
  if (hasDraftContent()) {
    return
  }
  draftSource.value = ''
}

function collectExpandableKeys(source: OpeningBalanceTableRow[]) {
  const result: string[] = []
  for (const row of source) {
    if (row.children?.length) {
      result.push(row.subjectCode)
      result.push(...collectExpandableKeys(row.children))
    }
  }
  return result
}

function currentExpandedStorageContext() {
  if (!filters.companyId || !filters.iyear || !filters.iperiod) {
    return ''
  }
  return `${filters.companyId}:${filters.iyear}${String(filters.iperiod).padStart(2, '0')}`
}

function readExpandedRowKeyMap(): PersistedExpandedMap {
  try {
    const raw = localStorage.getItem(EXPANDED_STORAGE_KEY)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw) as PersistedExpandedMap
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    localStorage.removeItem(EXPANDED_STORAGE_KEY)
    return {}
  }
}

function writeExpandedRowKeyMap(map: PersistedExpandedMap) {
  if (!Object.keys(map).length) {
    localStorage.removeItem(EXPANDED_STORAGE_KEY)
    return
  }
  localStorage.setItem(EXPANDED_STORAGE_KEY, JSON.stringify(map))
}

function restoreExpandedRowKeys(source: OpeningBalanceTableRow[]) {
  const allExpandableKeys = collectExpandableKeys(source)
  const contextKey = currentExpandedStorageContext()
  if (!contextKey) {
    return allExpandableKeys
  }
  const storedKeys = readExpandedRowKeyMap()[contextKey]
  if (!Array.isArray(storedKeys)) {
    return allExpandableKeys
  }
  return storedKeys.filter((item) => allExpandableKeys.includes(item))
}

function persistExpandedRowKeys() {
  const contextKey = currentExpandedStorageContext()
  if (!contextKey) {
    return
  }
  const map = readExpandedRowKeyMap()
  map[contextKey] = [...expandedRowKeys.value]
  writeExpandedRowKeyMap(map)
}

function normalizeOpeningBalanceMoney(value?: string | number | null) {
  return normalizeMoneyValue(value, OPENING_BALANCE_MONEY_OPTIONS)
}

function formatDateTime(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function toCents(value?: string | number) {
  const normalized = normalizeOpeningBalanceMoney(value)
  return Math.round(Number(normalized) * 100)
}

function centsToMoney(value: number) {
  return (value / 100).toFixed(2)
}

function moneyText(value?: string | number) {
  return formatMoney(value || '0.00')
}
</script>

<style scoped>
.opening-balance-page {
  height: 100%;
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.ob-toolbar,
.ob-summary-card,
.ob-table-card,
.ob-result-card {
  background: #fff;
  border: 1px solid #e6eaf2;
  border-radius: 18px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.ob-toolbar {
  padding: 8px 12px;
}

.ob-toolbar__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ob-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ob-toolbar__filter {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-shrink: 0;
}

.ob-toolbar__panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e6eaf2;
}

.ob-toolbar__panel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.ob-toolbar__panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.ob-summary-card {
  padding: 10px 14px;
  background: linear-gradient(135deg, #f7fbff, #ffffff);
}

.ob-summary-card__main {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.ob-summary-card__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(210, 222, 239, 0.9);
}

.ob-summary-card__item span {
  color: #6b7280;
  font-size: 12px;
}

.ob-summary-card__item strong {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.ob-summary-card__hint {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  line-height: 1.4;
}

.ob-summary-card__item--trial {
  min-height: 88px;
}

.ob-summary-card__item--trial.is-success {
  background: #f0fdf4;
  border-color: #86efac;
}

.ob-summary-card__item--trial.is-warning {
  background: #fff7ed;
  border-color: #fdba74;
}

.ob-table-card {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  padding: 8px 10px 12px;
}

.ob-table-card :deep(.el-table) {
  height: 100%;
}

.ob-balance-cell {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.ob-balance-cell__text {
  font-weight: 600;
  color: #111827;
}

.ob-balance-editor {
  width: 100%;
}

.ob-balance-editor.is-locked,
.ob-balance-action.is-locked {
  cursor: not-allowed;
}

.ob-balance-action.is-locked {
  color: #9ca3af;
}

.ob-balance-editor.is-locked :deep(input) {
  pointer-events: none;
}

.ob-result-card {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  padding: 12px 14px;
}

.ob-result-block {
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5edf6;
}

.ob-result-block h2 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #111827;
}

.ob-result-block p {
  margin: 0;
  color: #4b5563;
  line-height: 1.6;
}

.ob-assist-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #111827;
}

.ob-assist-head span {
  color: #64748b;
}

.ob-assist-actions {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 10px;
}

@media (max-width: 768px) {
  .ob-toolbar__main {
    align-items: flex-start;
    flex-direction: column;
  }

  .ob-toolbar__filter {
    justify-content: flex-start;
    width: 100%;
  }

  .ob-summary-card__main {
    grid-template-columns: 1fr;
  }

  .ob-toolbar__panel-actions {
    justify-content: flex-start;
  }
}
</style>
