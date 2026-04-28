<template>
  <div class="opening-balance-page">
    <section class="ob-toolbar">
      <div class="ob-toolbar__actions">
        <el-button type="primary" :loading="toolbarLoading.save" @click="saveAll">保存</el-button>
        <el-button :loading="toolbarLoading.openBook" @click="runOpenBook">开账</el-button>
        <el-button :loading="toolbarLoading.carryForward" @click="runCarryForwardPreview">结转</el-button>
        <el-button :loading="toolbarLoading.trial" @click="runTrial">试算</el-button>
        <el-button :loading="toolbarLoading.reconcile" @click="runReconcile">对账</el-button>
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
        <div class="ob-chip">
          <span>末级科目</span>
          <strong>{{ editableLeafCount }}</strong>
        </div>
        <div class="ob-chip">
          <span>辅助科目</span>
          <strong>{{ assistLeafCount }}</strong>
        </div>
        <div class="ob-chip" :class="saveStatusClass">
          <span>保存状态</span>
          <strong>{{ saveStatusText }}</strong>
          <em class="ob-chip__hint">{{ saveStatusHint }}</em>
        </div>
      </div>
    </section>

    <section class="ob-table-card">
      <el-table
        v-loading="loading.rows"
        :data="rows"
        row-key="subjectCode"
        stripe
        :expand-row-keys="expandedRowKeys"
        :tree-props="{ children: 'children' }"
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
                <el-button link type="primary" @click="openAssistDialog(row)">
                  {{ moneyText(row.mb) }} / 录入辅助
                </el-button>
              </template>
              <template v-else>
                <money-input
                  v-model="row.draftBalance"
                  :disabled="!meta?.opened"
                  @update:model-value="handleRowDraftChange(row, $event)"
                  @blur="handleRowDraftChange(row, row.draftBalance)"
                  @change="handleRowDraftChange(row, row.draftBalance)"
                />
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="trialResult || reconcileResult" class="ob-result-card">
      <div v-if="trialResult" class="ob-result-block">
        <h2>试算结果</h2>
        <p>
          借方 {{ moneyText(trialResult.totalDebit) }}，贷方 {{ moneyText(trialResult.totalCredit) }}，差额
          {{ moneyText(trialResult.difference) }}
        </p>
      </div>
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
            <el-select v-model="row.cdeptId" filterable clearable>
              <el-option v-for="item in meta?.departmentOptions || []" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bperson" label="人员" min-width="140">
          <template #default="{ row }">
            <el-select v-model="row.cpersonId" filterable clearable>
              <el-option v-for="item in meta?.employeeOptions || []" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bcus" label="客户" min-width="160">
          <template #default="{ row }">
            <el-select v-model="row.ccusId" filterable clearable>
              <el-option v-for="item in meta?.customerOptions || []" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bsup" label="供应商" min-width="160">
          <template #default="{ row }">
            <el-select v-model="row.csupId" filterable clearable>
              <el-option v-for="item in meta?.supplierOptions || []" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bitem" label="项目分类" min-width="150">
          <template #default="{ row }">
            <el-select
              v-model="row.citemClass"
              :disabled="Boolean(activeAssistRow?.cassItem)"
              filterable
              clearable
              @change="handleAssistProjectClassChange(row)"
            >
              <el-option v-for="item in projectClassOptionsForRow(row)" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="activeAssistConfig.bitem" label="项目" min-width="170">
          <template #default="{ row }">
            <el-select v-model="row.citemId" filterable clearable>
              <el-option v-for="item in projectOptionsForRow(row)" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="余额" min-width="180">
          <template #default="{ row }">
            <money-input v-model="row.mb" />
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
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'
import { formatMoney, normalizeMoneyValue } from '@/utils/money'

type OpeningBalanceTableRow = OpeningBalanceRow & {
  draftBalance: string
  savedBalance: string
  children?: OpeningBalanceTableRow[]
}

type AssistDialogLine = OpeningAssistBalanceLine & {
  rowKey: string
  mb: string
}

type DraftSource = '' | 'manual' | 'assist' | 'carry-forward'

type PersistedExpandedMap = Record<string, string[]>

const SWITCH_GUARD_KEY = 'finance-opening-balance-draft-guard'
const EXPANDED_STORAGE_KEY = 'finance-opening-balance-expanded-keys'

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
const draftSource = ref<DraftSource>('')
const lastSavedAt = ref('')
const suppressContextWatch = ref(false)
let assistSeed = 0

const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const flatRows = computed(() => flattenRows(rows.value))
const editableLeafCount = computed(() => flatRows.value.filter((item) => item.editable).length)
const assistLeafCount = computed(() => flatRows.value.filter((item) => item.editable && item.assistRequired).length)
const hasUnsavedChanges = computed(() => hasDraftContent())
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

function decorateRows(source: OpeningBalanceRow[], balanceMap: Record<string, string>) {
  return source.map((item) => {
    const currentBalance = normalizeMoneyValue(String(item.mb ?? '0.00'), { fallback: '0.00' })
    return {
      ...item,
      mb: currentBalance,
      draftBalance: currentBalance,
      savedBalance: balanceMap[item.subjectCode] || currentBalance,
      children: decorateRows(item.children || [], balanceMap)
    } satisfies OpeningBalanceTableRow
  })
}

function collectPersistedBalances(source: OpeningBalanceRow[]) {
  const result: Record<string, string> = {}
  for (const row of flattenRows(source)) {
    result[row.subjectCode] = normalizeMoneyValue(String(row.mb ?? '0.00'), { fallback: '0.00' })
  }
  return result
}

function flattenRows(source: Array<OpeningBalanceRow | OpeningBalanceTableRow>) {
  const result: Array<OpeningBalanceRow | OpeningBalanceTableRow> = []
  for (const item of source) {
    result.push(item)
    if (item.children?.length) {
      result.push(...flattenRows(item.children))
    }
  }
  return result
}

function handleRowDraftChange(row: OpeningBalanceTableRow, value: string | number) {
  const normalized = normalizeMoneyValue(String(value ?? row.draftBalance ?? '0.00'), { fallback: '0.00' })
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
    mb: normalizeMoneyValue(String(line.mb ?? '0.00'), { fallback: '0.00' }),
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
      mb: normalizeMoneyValue(item.mb, { fallback: '0.00' })
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
      mb: normalizeMoneyValue(String(item.mb ?? '0.00'), { fallback: '0.00' })
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
    const currentBalance = normalizeMoneyValue(String(row.mb ?? '0.00'), { fallback: '0.00' })
    row.draftBalance = currentBalance
    if (row.editable && !row.assistRequired && currentBalance !== row.savedBalance) {
      manualDrafts.value[row.subjectCode] = currentBalance
    }
  }

  for (const item of preview.assistLines || []) {
    assistDrafts.value[item.subjectCode] = (item.lines || []).map((line) => ({
      ...line,
      mb: normalizeMoneyValue(String(line.mb ?? '0.00'), { fallback: '0.00' })
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
    ElMessage[res.data.balanced ? 'success' : 'warning'](
      res.data.balanced ? '期初试算平衡' : `试算不平衡，差额 ${moneyText(res.data.difference)}`
    )
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

function handleExpandChange(row: OpeningBalanceTableRow, expandedRows: OpeningBalanceTableRow[]) {
  const isExpanded = Array.isArray(expandedRows) && expandedRows.some((item) => item.subjectCode === row.subjectCode)
  if (isExpanded) {
    if (!expandedRowKeys.value.includes(row.subjectCode)) {
      expandedRowKeys.value = [...expandedRowKeys.value, row.subjectCode]
    }
  } else {
    expandedRowKeys.value = expandedRowKeys.value.filter((item) => item !== row.subjectCode)
  }
  persistExpandedRowKeys()
}

function recalculateTreeBalances(source: OpeningBalanceTableRow[]) {
  for (const row of source) {
    recalculateNodeBalance(row)
  }
}

function recalculateNodeBalance(row: OpeningBalanceTableRow) {
  if (!row.children?.length) {
    row.mb = normalizeMoneyValue(String(row.mb ?? row.draftBalance ?? '0.00'), { fallback: '0.00' })
    return toCents(row.mb)
  }
  const total = row.children.reduce((sum, child) => sum + recalculateNodeBalance(child), 0)
  row.mb = centsToMoney(total)
  row.draftBalance = row.mb
  return total
}

function flatRowsFrom(source: OpeningBalanceTableRow[]) {
  return flattenRows(source) as OpeningBalanceTableRow[]
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
  const normalized = normalizeMoneyValue(String(value ?? '0.00'), { fallback: '0.00' })
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
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 8px 12px;
}

.ob-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.ob-summary-card__item,
.ob-chip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(210, 222, 239, 0.9);
}

.ob-summary-card__item span,
.ob-chip span {
  color: #6b7280;
  font-size: 12px;
}

.ob-summary-card__item strong,
.ob-chip strong {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.ob-chip__hint {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  line-height: 1.4;
}

.ob-chip.is-dirty {
  background: #fff7ed;
  border-color: #fdba74;
}

.ob-chip.is-saved {
  background: #f0fdf4;
  border-color: #86efac;
}

.ob-table-card {
  padding: 8px 10px 12px;
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
  .ob-summary-card__main {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
