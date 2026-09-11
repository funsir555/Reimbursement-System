<template>
  <div class="expense-wb-page expense-wb-page--list finance-ledger-report-page">
    <el-card class="expense-wb-toolbar expense-wb-toolbar--compact finance-ledger-report-toolbar">
      <div class="expense-wb-toolbar__row expense-wb-toolbar__row--compact finance-ledger-report-toolbar__top">
        <div class="expense-wb-toolbar__heading expense-wb-toolbar__heading--compact expense-wb-toolbar__heading--inline">
          <div>
            <p class="expense-wb-toolbar__title">{{ reportTitle }}</p>
            <div class="expense-wb-toolbar__meta">
              <span class="expense-wb-soft-badge">当前公司 {{ financeCompany.currentCompanyName || '未设置' }}</span>
              <span class="expense-wb-soft-badge">默认期间 {{ financePeriod.currentMonthText || '未设置' }}</span>
              <span class="expense-wb-soft-badge expense-wb-soft-badge--success">本地查询 {{ filters.iyear }}-{{ String(filters.iperiod).padStart(2, '0') }}</span>
            </div>
          </div>
        </div>

        <div class="expense-wb-toolbar__actions">
          <el-checkbox v-model="filters.includeUnposted">包含未记账凭证</el-checkbox>
          <el-button :loading="metaLoading || loading" @click="reloadCurrent">刷新</el-button>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button :loading="exporting" :disabled="!canExport" @click="handleExport">导出 Excel</el-button>
        </div>
      </div>

      <div class="expense-wb-advanced-panel expense-wb-advanced-panel--embedded">
        <div class="expense-wb-advanced-grid finance-ledger-report-grid">
          <el-select v-model="filters.companyId" filterable placeholder="公司">
            <el-option
              v-for="item in companyOptions"
              :key="item.companyId"
              :label="item.label"
              :value="item.companyId"
            />
          </el-select>
          <el-select v-model="filters.iyear" placeholder="会计年度">
            <el-option v-for="item in yearOptions" :key="item" :label="`${item}年`" :value="item" />
          </el-select>
          <el-select v-model="filters.iperiod" placeholder="会计月份">
            <el-option v-for="item in monthOptions" :key="item" :label="`${item}月`" :value="item" />
          </el-select>
          <el-select v-model="filters.accountCodeFrom" filterable clearable placeholder="科目范围起">
            <el-option v-for="item in accountOptions" :key="`from-${item.value}`" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.accountCodeTo" filterable clearable placeholder="科目范围止">
            <el-option v-for="item in accountOptions" :key="`to-${item.value}`" :label="item.label" :value="item.value" />
          </el-select>
          <template v-if="isBalanceReport">
            <el-select v-model="filters.balanceAssistDisplay" clearable placeholder="辅助项">
              <el-option label="名称" value="NAME" />
              <el-option label="编码" value="CODE" />
              <el-option label="编码+名称" value="CODE_NAME" />
            </el-select>
            <el-select v-model="filters.subjectLevelRange" clearable placeholder="级次">
              <el-option
                v-for="item in subjectLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
          <template v-else>
            <el-select v-model="filters.cdeptId" filterable clearable placeholder="部门">
              <el-option v-for="item in departmentOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.cpersonId" filterable clearable placeholder="个人">
              <el-option v-for="item in employeeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.ccusId" filterable clearable placeholder="客户">
              <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.csupId" filterable clearable placeholder="供应商">
              <el-option v-for="item in supplierOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.citemClass" filterable clearable placeholder="项目分类">
              <el-option v-for="item in projectClassOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.citemId" filterable clearable placeholder="项目">
              <el-option
                v-for="item in filteredProjectOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
          <template v-if="isSequenceReport">
            <el-input v-model="filters.voucherNo" clearable placeholder="凭证号" />
            <el-select v-model="filters.csign" clearable placeholder="凭证类型">
              <el-option v-for="item in voucherTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.cbill" filterable clearable placeholder="制单人">
              <el-option v-for="item in makerOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-input v-model="filters.summary" clearable placeholder="摘要" />
          </template>
        </div>
      </div>
    </el-card>

    <el-card v-if="isBalanceReport" class="expense-wb-panel expense-wb-table-shell finance-ledger-report-panel">
      <el-table
        :data="balanceItems"
        v-loading="loading"
        stripe
        :row-class-name="balanceRowClassName"
      >
        <el-table-column prop="subjectCode" label="科目编码" min-width="130" />
        <el-table-column label="科目名称" min-width="180">
          <template #default="{ row }">
            <span :class="{ 'finance-balance-assist-name': row.rowType === 'ASSIST' }">
              {{ row.subjectName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="subjectLevel" label="级次" min-width="80" align="center" />
        <el-table-column label="期初借方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.beginDebit) }}</template>
        </el-table-column>
        <el-table-column label="期初贷方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.beginCredit) }}</template>
        </el-table-column>
        <el-table-column label="本期借方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.periodDebit) }}</template>
        </el-table-column>
        <el-table-column label="本期贷方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.periodCredit) }}</template>
        </el-table-column>
        <el-table-column label="期末借方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.endDebit) }}</template>
        </el-table-column>
        <el-table-column label="期末贷方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.endCredit) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <template v-else-if="isGeneralLedgerReport">
      <el-card
        v-for="section in generalLedgerItems"
        :key="section.subjectCode"
        class="expense-wb-panel expense-wb-table-shell finance-ledger-report-panel finance-ledger-report-panel--section"
      >
        <div class="finance-ledger-report-section__header">
          <div>
            <h3>{{ section.subjectCode }} {{ section.subjectName }}</h3>
            <p>
              期初 {{ formatMoney(section.beginDebit) }}/{{ formatMoney(section.beginCredit) }}
              · 本期 {{ formatMoney(section.totalDebit) }}/{{ formatMoney(section.totalCredit) }}
              ·
              <button class="finance-ledger-report-link" type="button" @click="openDetailLedger(section)">
                期末 {{ formatMoney(section.endDebit) }}/{{ formatMoney(section.endCredit) }}
              </button>
            </p>
          </div>
          <span class="expense-wb-soft-badge">明细 {{ section.rowCount }} 行</span>
        </div>
        <el-table :data="section.rows" stripe>
          <el-table-column prop="rowType" label="行类型" min-width="90" />
          <el-table-column prop="dbillDate" label="日期" min-width="120" />
          <el-table-column label="凭证号" min-width="130">
            <template #default="{ row }">
              <button
                v-if="row.rowType === 'ENTRY' && row.voucherNo"
                class="finance-ledger-report-link"
                type="button"
                @click="openSequenceLedger(row)"
              >
                {{ row.displayVoucherNo }}
              </button>
              <span v-else>{{ row.displayVoucherNo || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
          <el-table-column label="借方" min-width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.debit) }}</template>
          </el-table-column>
          <el-table-column label="贷方" min-width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.credit) }}</template>
          </el-table-column>
          <el-table-column label="余额" min-width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.balance) }}</template>
          </el-table-column>
          <el-table-column prop="balanceDirection" label="方向" min-width="80" align="center" />
        </el-table>
      </el-card>
    </template>

    <el-card v-else-if="isSequenceReport" class="expense-wb-panel expense-wb-table-shell finance-ledger-report-panel">
      <el-table :data="sequenceItems" v-loading="loading" stripe @row-dblclick="openVoucherDetail">
        <el-table-column label="凭证号" min-width="130">
          <template #default="{ row }">
            <button class="finance-ledger-report-link" type="button" @click="openVoucherDetail(row)">
              {{ row.displayVoucherNo }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="voucherTypeLabel" label="凭证类型" min-width="110" />
        <el-table-column prop="dbillDate" label="制单日期" min-width="120" />
        <el-table-column prop="iyperiod" label="会计期间" min-width="100" />
        <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="cbill" label="制单人" min-width="120" />
        <el-table-column prop="idoc" label="附件张数" min-width="100" align="center" />
        <el-table-column label="借方合计" min-width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalDebit) }}</template>
        </el-table-column>
        <el-table-column label="贷方合计" min-width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalCredit) }}</template>
        </el-table-column>
        <el-table-column prop="statusLabel" label="状态" min-width="110" />
      </el-table>
    </el-card>

    <el-card v-else class="expense-wb-panel expense-wb-table-shell finance-ledger-report-panel">
      <el-table :data="detailItems" v-loading="loading" stripe>
        <el-table-column prop="groupKey" label="分组" min-width="160" show-overflow-tooltip />
        <el-table-column prop="rowType" label="行类型" min-width="90" />
        <el-table-column prop="subjectCode" label="科目编码" min-width="130" />
        <el-table-column prop="subjectName" label="科目名称" min-width="180" />
        <el-table-column prop="assistLabel" label="辅助项" min-width="160" show-overflow-tooltip />
        <el-table-column prop="dbillDate" label="日期" min-width="120" />
        <el-table-column label="凭证号" min-width="130">
          <template #default="{ row }">
            <button
              v-if="row.rowType === 'ENTRY' && row.voucherNo"
              class="finance-ledger-report-link"
              type="button"
              @click="openSequenceLedger(row)"
            >
              {{ row.displayVoucherNo }}
            </button>
            <span v-else>{{ row.displayVoucherNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="借方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.debit) }}</template>
        </el-table-column>
        <el-table-column label="贷方" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.credit) }}</template>
        </el-table-column>
        <el-table-column label="余额" min-width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.balance) }}</template>
        </el-table-column>
        <el-table-column prop="balanceDirection" label="方向" min-width="80" align="center" />
        <template v-if="showQuantityColumns">
          <el-table-column label="数量借方" min-width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.quantityDebit) }}</template>
          </el-table-column>
          <el-table-column label="数量贷方" min-width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.quantityCredit) }}</template>
          </el-table-column>
          <el-table-column label="数量余额" min-width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.quantityBalance) }}</template>
          </el-table-column>
          <el-table-column prop="measureUnit" label="计量单位" min-width="100" />
        </template>
      </el-table>
    </el-card>

    <div class="expense-wb-pagination">
      <el-pagination
        v-model:current-page="pager.page"
        v-model:page-size="pager.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pager.total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadReport"
        @size-change="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ledgerReportApi,
  type DetailLedgerKind,
  type FinanceBalanceSheetRow,
  type FinanceDetailLedgerRow,
  type FinanceGeneralLedgerSection,
  type FinanceLedgerReportMeta,
  type FinanceLedgerReportQueryParams,
  type FinanceSequenceLedgerRow,
  type LedgerReportKind
} from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const props = defineProps<{
  reportKind: LedgerReportKind
}>()

const route = useRoute()
const router = useRouter()
const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()

const REPORT_CONFIG: Record<LedgerReportKind, { title: string; detailKind?: DetailLedgerKind; exportPermission: string }> = {
  BALANCE_SHEET: {
    title: '余额表',
    exportPermission: 'finance:general_ledger:balance_sheet:export'
  },
  GENERAL_LEDGER: {
    title: '总分类账',
    exportPermission: 'finance:general_ledger:general_ledger:export'
  },
  DETAIL_LEDGER: {
    title: '明细账',
    detailKind: 'DETAIL',
    exportPermission: 'finance:general_ledger:detail_ledger:export'
  },
  PROJECT_DETAIL_LEDGER: {
    title: '项目明细账',
    detailKind: 'PROJECT',
    exportPermission: 'finance:general_ledger:project_detail_ledger:export'
  },
  SUPPLIER_DETAIL_LEDGER: {
    title: '供应商明细账',
    detailKind: 'SUPPLIER',
    exportPermission: 'finance:general_ledger:supplier_detail_ledger:export'
  },
  CUSTOMER_DETAIL_LEDGER: {
    title: '客户明细账',
    detailKind: 'CUSTOMER',
    exportPermission: 'finance:general_ledger:customer_detail_ledger:export'
  },
  PERSONAL_DETAIL_LEDGER: {
    title: '个人明细账',
    detailKind: 'PERSONAL',
    exportPermission: 'finance:general_ledger:personal_detail_ledger:export'
  },
  QUANTITY_AMOUNT_DETAIL_LEDGER: {
    title: '数量金额明细账',
    detailKind: 'QUANTITY_AMOUNT',
    exportPermission: 'finance:general_ledger:quantity_amount_detail_ledger:export'
  },
  SEQUENCE_LEDGER: {
    title: '序时账',
    exportPermission: 'finance:general_ledger:sequence_ledger:export'
  }
}

const metaLoading = ref(false)
const loading = ref(false)
const exporting = ref(false)
const ledgerMeta = ref<FinanceLedgerReportMeta | null>(null)
const pager = reactive({
  total: 0,
  page: 1,
  pageSize: 20,
  items: [] as unknown[]
})

const filters = reactive({
  companyId: '',
  iyear: 0,
  iperiod: 0,
  accountCodeFrom: '',
  accountCodeTo: '',
  cdeptId: '',
  cpersonId: '',
  ccusId: '',
  csupId: '',
  citemClass: '',
  citemId: '',
  balanceAssistDisplay: '' as '' | 'NAME' | 'CODE' | 'CODE_NAME',
  subjectLevelRange: '',
  includeUnposted: false,
  voucherNo: '',
  csign: '',
  summary: '',
  cbill: ''
})

const reportConfig = computed(() => REPORT_CONFIG[props.reportKind])
const reportTitle = computed(() => reportConfig.value.title)
const isBalanceReport = computed(() => props.reportKind === 'BALANCE_SHEET')
const isGeneralLedgerReport = computed(() => props.reportKind === 'GENERAL_LEDGER')
const isSequenceReport = computed(() => props.reportKind === 'SEQUENCE_LEDGER')
const isDetailLedgerReport = computed(() => Boolean(reportConfig.value.detailKind))
const showQuantityColumns = computed(() => reportConfig.value.detailKind === 'QUANTITY_AMOUNT')
const canExport = computed(() => hasPermission(reportConfig.value.exportPermission, readStoredUser()))

const companyOptions = computed(() => ledgerMeta.value?.companyOptions || [])
const departmentOptions = computed(() => ledgerMeta.value?.departmentOptions || [])
const employeeOptions = computed(() => ledgerMeta.value?.employeeOptions || [])
const makerOptions = computed(() => ledgerMeta.value?.makerOptions || [])
const voucherTypeOptions = computed(() => ledgerMeta.value?.voucherTypeOptions || [])
const accountOptions = computed(() => ledgerMeta.value?.accountOptions || [])
const customerOptions = computed(() => ledgerMeta.value?.customerOptions || [])
const supplierOptions = computed(() => ledgerMeta.value?.supplierOptions || [])
const projectClassOptions = computed(() => ledgerMeta.value?.projectClassOptions || [])
const projectOptions = computed(() => ledgerMeta.value?.projectOptions || [])
const filteredProjectOptions = computed(() => {
  if (!filters.citemClass) {
    return projectOptions.value
  }
  return projectOptions.value.filter((item) => item.parentValue === filters.citemClass)
})
const subjectLevelOptions = computed(() => {
  const maxLevel = Math.max(
    1,
    ...accountOptions.value
      .map(item => Number(item.subjectLevel || 0))
      .filter(item => Number.isFinite(item))
  )
  return Array.from({ length: maxLevel }, (_, index) => {
    const level = index + 1
    return { value: `1-${level}`, label: `1-${level}级` }
  })
})

const currentCompanyOption = computed(() => companyOptions.value.find((item) => item.companyId === filters.companyId))
const yearOptions = computed(() => {
  const option = currentCompanyOption.value
  if (!option?.periodStartYear || !option.periodEndYear) {
    return [] as number[]
  }
  const result: number[] = []
  for (let year = option.periodStartYear; year <= option.periodEndYear; year += 1) {
    result.push(year)
  }
  return result
})
const monthOptions = computed(() => {
  const option = currentCompanyOption.value
  if (!option || !filters.iyear) {
    return [] as number[]
  }
  const startMonth = filters.iyear === option.periodStartYear ? option.periodStartMonth || 1 : 1
  const endMonth = filters.iyear === option.periodEndYear ? option.periodEndMonth || 12 : 12
  const result: number[] = []
  for (let month = startMonth; month <= endMonth; month += 1) {
    result.push(month)
  }
  return result
})

const balanceItems = computed(() => pager.items as FinanceBalanceSheetRow[])
const generalLedgerItems = computed(() => pager.items as FinanceGeneralLedgerSection[])
const detailItems = computed(() => pager.items as FinanceDetailLedgerRow[])
const sequenceItems = computed(() => pager.items as FinanceSequenceLedgerRow[])

watch(
  () => [financeCompany.currentCompanyId, financePeriod.currentYear, financePeriod.currentPeriod] as const,
  async () => {
    applyDefaultsFromStores(false)
    await loadMeta()
    await loadReport()
  },
  { immediate: true }
)

watch(
  () => route.fullPath,
  async () => {
    applyRouteQuery()
    await loadMeta()
    await loadReport()
  }
)

watch(
  () => filters.iyear,
  () => {
    if (!monthOptions.value.includes(filters.iperiod)) {
      filters.iperiod = monthOptions.value[0] ?? 0
    }
  }
)

watch(
  () => filters.citemClass,
  () => {
    if (filters.citemId && !filteredProjectOptions.value.some((item) => item.value === filters.citemId)) {
      filters.citemId = ''
    }
  }
)

onMounted(() => {
  applyRouteQuery()
  loadMeta()
  loadReport()
})

function applyDefaultsFromStores(force: boolean) {
  if (force || !filters.companyId) {
    filters.companyId = financeCompany.currentCompanyId || filters.companyId
  }
  if (force || !filters.iyear) {
    filters.iyear = financePeriod.currentYear || filters.iyear
  }
  if (force || !filters.iperiod) {
    filters.iperiod = financePeriod.currentPeriod || filters.iperiod
  }
}

function applyRouteQuery() {
  const query = route.query
  const queryCompanyId = normalizeText(query.companyId)
  const queryYear = toPositiveInteger(query.iyear)
  const queryPeriod = toPositiveInteger(query.iperiod)
  if (queryCompanyId) {
    filters.companyId = queryCompanyId
  }
  if (queryYear) {
    filters.iyear = queryYear
  }
  if (queryPeriod) {
    filters.iperiod = queryPeriod
  }
  filters.accountCodeFrom = normalizeText(query.accountCodeFrom)
  filters.accountCodeTo = normalizeText(query.accountCodeTo)
  filters.cdeptId = normalizeText(query.cdeptId)
  filters.cpersonId = normalizeText(query.cpersonId)
  filters.ccusId = normalizeText(query.ccusId)
  filters.csupId = normalizeText(query.csupId)
  filters.citemClass = normalizeText(query.citemClass)
  filters.citemId = normalizeText(query.citemId)
  filters.balanceAssistDisplay = normalizeBalanceAssistDisplay(query.balanceAssistDisplay)
  filters.subjectLevelRange = normalizeText(query.subjectLevelRange)
  filters.voucherNo = normalizeText(query.voucherNo)
  filters.csign = normalizeText(query.csign)
  filters.summary = normalizeText(query.summary)
  filters.cbill = normalizeText(query.cbill)
}

async function loadMeta() {
  if (!filters.companyId || !filters.iyear || !filters.iperiod) {
    return
  }
  metaLoading.value = true
  try {
    const res = await ledgerReportApi.getMeta({
      companyId: filters.companyId,
      iyear: filters.iyear,
      iperiod: filters.iperiod
    })
    ledgerMeta.value = res.data
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载账簿筛选项失败')
  } finally {
    metaLoading.value = false
  }
}

function buildPayload(withPaging = true): FinanceLedgerReportQueryParams | null {
  if (!filters.companyId || !filters.iyear || !filters.iperiod) {
    return null
  }
  return {
    companyId: filters.companyId,
    iyear: filters.iyear,
    iperiod: filters.iperiod,
    ledgerKind: reportConfig.value.detailKind,
    accountCodeFrom: normalizeText(filters.accountCodeFrom) || undefined,
    accountCodeTo: normalizeText(filters.accountCodeTo) || undefined,
    cdeptId: isBalanceReport.value ? undefined : normalizeText(filters.cdeptId) || undefined,
    cpersonId: isBalanceReport.value ? undefined : normalizeText(filters.cpersonId) || undefined,
    ccusId: isBalanceReport.value ? undefined : normalizeText(filters.ccusId) || undefined,
    csupId: isBalanceReport.value ? undefined : normalizeText(filters.csupId) || undefined,
    citemClass: isBalanceReport.value ? undefined : normalizeText(filters.citemClass) || undefined,
    citemId: isBalanceReport.value ? undefined : normalizeText(filters.citemId) || undefined,
    balanceAssistDisplay: isBalanceReport.value ? filters.balanceAssistDisplay || undefined : undefined,
    subjectLevelRange: isBalanceReport.value ? normalizeText(filters.subjectLevelRange) || undefined : undefined,
    includeUnposted: filters.includeUnposted,
    voucherNo: isSequenceReport.value ? normalizeText(filters.voucherNo) || undefined : undefined,
    csign: isSequenceReport.value ? normalizeText(filters.csign) || undefined : undefined,
    summary: isSequenceReport.value ? normalizeText(filters.summary) || undefined : undefined,
    cbill: isSequenceReport.value ? normalizeText(filters.cbill) || undefined : undefined,
    page: withPaging ? pager.page : undefined,
    pageSize: withPaging ? pager.pageSize : undefined
  }
}

async function loadReport() {
  const payload = buildPayload(true)
  if (!payload) {
    return
  }
  loading.value = true
  try {
    if (isBalanceReport.value) {
      const res = await ledgerReportApi.queryBalanceSheet(payload)
      assignPage(res.data)
      return
    }
    if (isGeneralLedgerReport.value) {
      const res = await ledgerReportApi.queryGeneralLedger(payload)
      assignPage(res.data)
      return
    }
    if (isSequenceReport.value) {
      const res = await ledgerReportApi.querySequenceLedger(payload)
      assignPage(res.data)
      return
    }
    const res = await ledgerReportApi.queryDetailLedger(payload)
    assignPage(res.data)
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载账簿报表失败')
  } finally {
    loading.value = false
  }
}

function assignPage(data: { total: number; page: number; pageSize: number; items: unknown[] }) {
  pager.total = data.total || 0
  pager.page = data.page || 1
  pager.pageSize = data.pageSize || pager.pageSize
  pager.items = data.items || []
}

async function reloadCurrent() {
  await loadMeta()
  await loadReport()
}

function handleSearch() {
  pager.page = 1
  loadReport()
}

function handleReset() {
  applyDefaultsFromStores(true)
  filters.accountCodeFrom = ''
  filters.accountCodeTo = ''
  filters.cdeptId = ''
  filters.cpersonId = ''
  filters.ccusId = ''
  filters.csupId = ''
  filters.citemClass = ''
  filters.citemId = ''
  filters.balanceAssistDisplay = ''
  filters.subjectLevelRange = ''
  filters.includeUnposted = false
  filters.voucherNo = ''
  filters.csign = ''
  filters.summary = ''
  filters.cbill = ''
  pager.page = 1
  loadMeta()
  loadReport()
}

function handlePageSizeChange() {
  pager.page = 1
  loadReport()
}

async function handleExport() {
  const payload = buildPayload(false)
  if (!payload) {
    return
  }
  exporting.value = true
  try {
    if (isBalanceReport.value) {
      await ledgerReportApi.exportBalanceSheet(payload)
    } else if (isGeneralLedgerReport.value) {
      await ledgerReportApi.exportGeneralLedger(payload)
    } else if (isSequenceReport.value) {
      await ledgerReportApi.exportSequenceLedger(payload)
    } else {
      await ledgerReportApi.exportDetailLedger(payload)
    }
    ElMessage.success('报表导出已开始下载')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '导出账簿报表失败')
  } finally {
    exporting.value = false
  }
}

function openDetailLedger(section: FinanceGeneralLedgerSection) {
  router.push({
    name: 'finance-detail-ledger',
    query: {
      companyId: filters.companyId,
      iyear: String(filters.iyear),
      iperiod: String(filters.iperiod),
      accountCodeFrom: section.subjectCode,
      accountCodeTo: section.subjectCode,
      cdeptId: filters.cdeptId || undefined,
      cpersonId: filters.cpersonId || undefined,
      ccusId: filters.ccusId || undefined,
      csupId: filters.csupId || undefined,
      citemClass: filters.citemClass || undefined,
      citemId: filters.citemId || undefined
    }
  })
}

function openSequenceLedger(row: FinanceDetailLedgerRow) {
  if (!row.voucherNo) {
    return
  }
  router.push({
    name: 'finance-sequence-ledger',
    query: {
      companyId: filters.companyId,
      iyear: String(filters.iyear),
      iperiod: String(filters.iperiod),
      voucherNo: row.displayVoucherNo || row.voucherNo
    }
  })
}

function openVoucherDetail(row: FinanceSequenceLedgerRow) {
  if (!row.voucherNo) {
    return
  }
  router.push({
    name: 'finance-query-voucher-detail',
    params: {
      voucherNo: row.voucherNo
    }
  })
}

function formatMoney(value: unknown) {
  const numeric = Number(value ?? 0)
  return Number.isFinite(numeric)
    ? numeric.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '0.00'
}

function normalizeText(value: unknown) {
  return String(value || '').trim()
}

function toPositiveInteger(value: unknown) {
  const numeric = Number(value)
  return Number.isInteger(numeric) && numeric > 0 ? numeric : 0
}

function normalizeBalanceAssistDisplay(value: unknown): '' | 'NAME' | 'CODE' | 'CODE_NAME' {
  const normalized = normalizeText(value).toUpperCase()
  return normalized === 'NAME' || normalized === 'CODE' || normalized === 'CODE_NAME' ? normalized : ''
}

function balanceRowClassName({ row }: { row: FinanceBalanceSheetRow }) {
  if (row.rowType === 'CATEGORY_TOTAL') {
    return 'finance-balance-row--total'
  }
  if (row.rowType === 'ASSIST') {
    return 'finance-balance-row--assist'
  }
  return ''
}
</script>

<style scoped>
.finance-ledger-report-page {
  gap: 14px;
}

.finance-ledger-report-toolbar,
.finance-ledger-report-panel {
  border-radius: 24px;
}

.finance-ledger-report-toolbar__top {
  align-items: flex-start;
}

.finance-ledger-report-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.finance-ledger-report-panel--section {
  padding-bottom: 6px;
}

.finance-ledger-report-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.finance-ledger-report-section__header h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
}

.finance-ledger-report-section__header p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.finance-ledger-report-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #1f5f8b;
  cursor: pointer;
  font: inherit;
}

.finance-ledger-report-link:hover {
  color: #14435f;
  text-decoration: underline;
}

:deep(.finance-balance-row--total td) {
  font-weight: 700;
  color: #111827;
}

:deep(.finance-balance-row--assist .cell) {
  padding-left: 28px;
}

.finance-balance-assist-name {
  display: inline-block;
  padding-left: 20px;
}

@media (max-width: 1500px) {
  .finance-ledger-report-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .finance-ledger-report-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .finance-ledger-report-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }

  .finance-ledger-report-section__header {
    flex-direction: column;
  }
}
</style>
