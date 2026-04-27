<template>
  <div class="opening-balance-page">
    <section class="ob-toolbar">
      <div class="ob-toolbar__title">
        <h1>期初余额</h1>
        <p>负责开账、年度期初结转、试算平衡、辅助核算对账与期初余额录入。</p>
      </div>
      <div class="ob-toolbar__actions">
        <el-button type="primary" :loading="taskLoading.openBook" @click="runOpenBook">开账</el-button>
        <el-button :loading="taskLoading.carryForward" @click="runCarryForward">结转</el-button>
        <el-button :loading="taskLoading.trial" @click="runTrial">试算</el-button>
        <el-button :loading="taskLoading.reconcile" @click="runReconcile">对账</el-button>
      </div>
    </section>

    <section class="ob-summary-card">
      <div class="ob-summary-card__main">
        <div class="ob-summary-card__item">
          <span>当前公司</span>
          <strong>{{ currentCompanyName || '未设置' }}</strong>
        </div>
        <div class="ob-summary-card__item">
          <span>当前年度</span>
          <strong>{{ filters.iyear }}</strong>
        </div>
        <div class="ob-summary-card__item">
          <span>当前期间</span>
          <strong>{{ filters.iperiod }} 月</strong>
        </div>
        <div class="ob-summary-card__item">
          <span>开账状态</span>
          <strong>{{ meta?.statusLabel || '未开账' }}</strong>
        </div>
      </div>
      <div class="ob-summary-card__side">
        <div class="ob-chip">
          <span>末级科目</span>
          <strong>{{ editableLeafCount }}</strong>
        </div>
        <div class="ob-chip">
          <span>辅助科目</span>
          <strong>{{ assistLeafCount }}</strong>
        </div>
      </div>
    </section>

    <section class="ob-filter-card">
      <label>
        <span>公司</span>
        <div class="ob-static-field">{{ currentCompanyName || '未设置' }}</div>
      </label>
      <label>
        <span>年度</span>
        <el-input-number v-model="filters.iyear" :controls="false" :min="2000" :max="2099" @change="handleFilterChange" />
      </label>
      <label>
        <span>期间</span>
        <el-input-number v-model="filters.iperiod" :controls="false" :min="1" :max="12" @change="handleFilterChange" />
      </label>
      <label>
        <span>状态</span>
        <div class="ob-static-field">{{ meta?.statusLabel || '未开账' }}</div>
      </label>
    </section>

    <section class="ob-table-card">
      <el-table v-loading="loading.rows" :data="rows" row-key="subjectCode" stripe>
        <el-table-column prop="subjectCode" label="科目编码" min-width="140" />
        <el-table-column label="科目名称" min-width="220">
          <template #default="{ row }">
            <div class="ob-subject-name" :style="{ paddingLeft: `${Math.max((Number(row.subjectLevel || 1) - 1) * 18, 0)}px` }">
              <span>{{ row.subjectName }}</span>
            </div>
          </template>
        </el-table-column>
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
                  :disabled="!meta?.opened || savingRowCode === row.subjectCode"
                  @blur="saveSingleRow(row)"
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
        <p>借方合计 {{ moneyText(trialResult.totalDebit) }}，贷方合计 {{ moneyText(trialResult.totalCredit) }}，差额 {{ moneyText(trialResult.difference) }}</p>
      </div>
      <div v-if="reconcileResult" class="ob-result-block">
        <h2>对账结果</h2>
        <p>差异科目 {{ reconcileResult.differenceSubjects.length }} 条，缺失辅助 {{ reconcileResult.missingAssistSubjects.length }} 条，非法组合 {{ reconcileResult.illegalAssistMessages.length }} 条</p>
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
            <el-select v-model="row.citemClass" :disabled="Boolean(activeAssistRow?.cassItem)" filterable clearable @change="handleAssistProjectClassChange(row)">
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
        <el-button type="primary" :loading="savingAssist" @click="saveAssistDialog">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { openingBalanceApi, type OpeningAssistBalanceLine, type OpeningBalanceMeta, type OpeningBalanceReconcileResult, type OpeningBalanceRow, type OpeningBalanceTrialResult } from '@/api'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { formatMoney, normalizeMoneyValue } from '@/utils/money'

type OpeningBalanceTableRow = OpeningBalanceRow & {
  draftBalance: string
}

type AssistDialogLine = OpeningAssistBalanceLine & {
  rowKey: string
  mb: string
}

const financeCompany = useFinanceCompanyStore()
const meta = ref<OpeningBalanceMeta | null>(null)
const rows = ref<OpeningBalanceTableRow[]>([])
const loading = reactive({ meta: false, rows: false })
const taskLoading = reactive({ openBook: false, carryForward: false, trial: false, reconcile: false })
const filters = reactive({
  companyId: '',
  iyear: new Date().getFullYear(),
  iperiod: new Date().getMonth() + 1
})
const trialResult = ref<OpeningBalanceTrialResult | null>(null)
const reconcileResult = ref<OpeningBalanceReconcileResult | null>(null)
const savingRowCode = ref('')
const assistDialogVisible = ref(false)
const activeAssistRow = ref<OpeningBalanceTableRow | null>(null)
const assistLines = ref<AssistDialogLine[]>([])
const savingAssist = ref(false)
let assistSeed = 0

const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const editableLeafCount = computed(() => rows.value.filter((item) => item.editable).length)
const assistLeafCount = computed(() => rows.value.filter((item) => item.editable && item.assistRequired).length)
const activeAssistConfig = computed(() => ({
  bdept: activeAssistRow.value?.bdept === 1,
  bperson: activeAssistRow.value?.bperson === 1,
  bcus: activeAssistRow.value?.bcus === 1,
  bsup: activeAssistRow.value?.bsup === 1,
  bitem: activeAssistRow.value?.bitem === 1
}))

watch(
  () => financeCompany.currentCompanyId,
  async (companyId) => {
    if (!companyId) return
    filters.companyId = companyId
    await loadMeta()
    await loadRows()
  },
  { immediate: true }
)

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
    filters.iyear = res.data.defaultYear
    filters.iperiod = res.data.defaultPeriod
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载期初余额元数据失败')
  } finally {
    loading.meta = false
  }
}

async function loadRows() {
  if (!filters.companyId) return
  loading.rows = true
  try {
    const res = await openingBalanceApi.listRows({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    rows.value = (res.data || []).map((item) => ({
      ...item,
      draftBalance: String(item.mb || '0.00')
    }))
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载期初余额失败')
  } finally {
    loading.rows = false
  }
}

async function handleFilterChange() {
  await loadMeta()
  await loadRows()
}

async function saveSingleRow(row: OpeningBalanceTableRow) {
  if (!meta.value?.opened) {
    row.draftBalance = String(row.mb || '0.00')
    return
  }
  const normalized = normalizeMoneyValue(row.draftBalance, { fallback: '0.00' })
  if (normalized === String(row.mb || '0.00')) {
    row.draftBalance = normalized
    return
  }
  savingRowCode.value = row.subjectCode
  try {
    await openingBalanceApi.saveRows({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod,
      rows: [{ subjectCode: row.subjectCode, mb: normalized }]
    })
    ElMessage.success('期初余额已保存')
    await loadRows()
  } catch (error: unknown) {
    row.draftBalance = String(row.mb || '0.00')
    ElMessage.error(error instanceof Error ? error.message : '保存期初余额失败')
  } finally {
    savingRowCode.value = ''
  }
}

async function openAssistDialog(row: OpeningBalanceTableRow) {
  activeAssistRow.value = row
  try {
    const res = await openingBalanceApi.getAssistBalances(row.subjectCode, {
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    assistLines.value = (res.data || []).map((item) => toAssistDialogLine(item))
    if (!assistLines.value.length) {
      appendAssistLine()
    }
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
    mb: String(line.mb || '0.00'),
    mbF: line.mbF,
    nbS: line.nbS
  }
}

function handleAssistProjectClassChange(line: AssistDialogLine) {
  line.citemId = ''
}

function projectClassOptionsForRow(row: AssistDialogLine) {
  if (activeAssistRow.value?.cassItem) {
    return (meta.value?.projectClassOptions || []).filter((item) => item.value === activeAssistRow.value?.cassItem)
  }
  return meta.value?.projectClassOptions || []
}

function projectOptionsForRow(row: AssistDialogLine) {
  const projectClass = activeAssistRow.value?.cassItem || row.citemClass
  return (meta.value?.projectOptions || []).filter((item) => item.parentValue === projectClass)
}

async function saveAssistDialog() {
  if (!activeAssistRow.value) return
  savingAssist.value = true
  try {
    const payloadLines = assistLines.value.map((item) => ({
      cdeptId: item.cdeptId || undefined,
      cpersonId: item.cpersonId || undefined,
      ccusId: item.ccusId || undefined,
      csupId: item.csupId || undefined,
      citemClass: (activeAssistRow.value?.cassItem || item.citemClass) || undefined,
      citemId: item.citemId || undefined,
      mb: normalizeMoneyValue(item.mb, { fallback: '0.00' })
    }))
    await openingBalanceApi.saveAssistBalances(activeAssistRow.value.subjectCode, {
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod,
      lines: payloadLines
    })
    assistDialogVisible.value = false
    ElMessage.success('辅助期初已保存')
    await loadRows()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '保存辅助期初失败')
  } finally {
    savingAssist.value = false
  }
}

async function runOpenBook() {
  taskLoading.openBook = true
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
    taskLoading.openBook = false
  }
}

async function runCarryForward() {
  taskLoading.carryForward = true
  try {
    const res = await openingBalanceApi.carryForward({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    ElMessage.success(res.data.message || '结转任务已提交')
    await refreshAfterTask()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '结转失败')
  } finally {
    taskLoading.carryForward = false
  }
}

async function runTrial() {
  taskLoading.trial = true
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
    taskLoading.trial = false
  }
}

async function runReconcile() {
  taskLoading.reconcile = true
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
    taskLoading.reconcile = false
  }
}

async function refreshAfterTask() {
  await new Promise((resolve) => window.setTimeout(resolve, 900))
  await loadMeta()
  await loadRows()
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
.ob-filter-card,
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
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
}

.ob-toolbar__title h1 {
  margin: 0;
  font-size: 24px;
  color: #15304d;
}

.ob-toolbar__title p {
  margin: 6px 0 0;
  color: #607086;
}

.ob-toolbar__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.ob-summary-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f7fbff, #ffffff);
}

.ob-summary-card__main {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ob-summary-card__item,
.ob-chip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(242, 247, 255, 0.95);
}

.ob-summary-card__item span,
.ob-chip span {
  font-size: 12px;
  color: #6d7a8c;
}

.ob-summary-card__item strong,
.ob-chip strong {
  color: #19324d;
  font-size: 16px;
}

.ob-summary-card__side {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ob-filter-card {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 18px 24px;
}

.ob-filter-card label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #526172;
  font-size: 13px;
}

.ob-static-field {
  min-height: 40px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  border-radius: 12px;
  border: 1px solid #d9e2ef;
  background: #f8fbff;
  color: #18324b;
}

.ob-table-card,
.ob-result-card {
  padding: 18px 20px 20px;
}

.ob-subject-name {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.ob-balance-cell {
  min-height: 36px;
  display: flex;
  align-items: center;
}

.ob-balance-cell__text {
  font-variant-numeric: tabular-nums;
}

.ob-result-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.ob-result-block h2 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #18324b;
}

.ob-result-block p {
  margin: 0;
  color: #5f7084;
}

.ob-assist-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #18324b;
}

.ob-assist-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

@media (max-width: 1100px) {
  .ob-summary-card,
  .ob-filter-card,
  .ob-result-card {
    grid-template-columns: 1fr;
  }

  .ob-summary-card__main {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .ob-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .ob-summary-card__main,
  .ob-summary-card__side,
  .ob-filter-card {
    grid-template-columns: 1fr;
  }
}
</style>
