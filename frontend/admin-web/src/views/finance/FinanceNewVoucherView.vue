<template>
  <div class="voucher-page">
    <section class="voucher-toolbar-panel">
      <header class="voucher-page-header">
        <h1>记账凭证</h1>
      </header>

      <section class="voucher-toolbar-panel">
        <div
          v-for="group in toolbarGroups"
          :key="group.key"
          class="toolbar-group"
          :class="group.key === 'primary' ? 'toolbar-group-primary' : ''"
        >
          <template v-for="action in group.actions" :key="action.key">
            <el-button
              :type="action.emphasis === 'primary' ? 'primary' : action.emphasis === 'secondary' ? 'info' : 'default'"
              :plain="action.emphasis === 'secondary'"
              :loading="action.key === 'save' ? saving : false"
              class="toolbar-button"
              :class="{
                'toolbar-button-large toolbar-button-accent': action.emphasis === 'secondary',
                'toolbar-button-large toolbar-button-primary': action.emphasis === 'primary'
              }"
              @click="handleToolbarAction(action.key)"
            >
              <el-icon :size="action.emphasis ? 18 : 16">
                <component :is="action.icon" />
              </el-icon>
              <span>{{ action.label }}</span>
            </el-button>
          </template>
        </div>
      </section>

      <section class="voucher-info-band">
        <div class="voucher-info-main">
          <div class="voucher-info-grid">
            <label class="voucher-info-field">
              <span>公司</span>
              <el-select v-model="form.companyId" filterable placeholder="请选择公司">
                <el-option
                  v-for="item in voucherMeta?.companyOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="voucher-info-field voucher-info-code">
              <span>凭证字号</span>
              <div class="voucher-code-box">{{ voucherCodeText }}</div>
            </label>

            <label class="voucher-info-field">
              <span>凭证类别</span>
              <el-select v-model="form.csign" placeholder="请选择类别">
                <el-option
                  v-for="item in voucherMeta?.voucherTypeOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="voucher-info-field voucher-info-period">
              <span>期间</span>
              <el-input-number v-model="form.iperiod" :min="1" :max="12" :controls="false" />
            </label>

            <label class="voucher-info-field">
              <span>制单日期</span>
              <el-date-picker
                v-model="form.dbillDate"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
              />
            </label>

            <label class="voucher-info-field voucher-info-attach">
              <span>附件张数</span>
              <el-input-number v-model="form.idoc" :min="0" :controls="false" />
            </label>

            <label class="voucher-info-field">
              <span>制单</span>
              <el-input v-model="form.cbill" readonly />
            </label>

            <label class="voucher-info-field">
              <span>说明一</span>
              <el-input v-model="form.ctext1" placeholder="可录入批次、来源等信息" />
            </label>

            <label class="voucher-info-field">
              <span>说明二</span>
              <el-input v-model="form.ctext2" placeholder="可录入补充说明" />
            </label>
          </div>
        </div>

        <aside class="voucher-info-side">
          <div class="status-chip" :class="isBalanced ? 'status-chip-balanced' : 'status-chip-warning'">
            <span>平衡状态</span>
            <strong>{{ isBalanced ? '已平衡' : '待校验' }}</strong>
          </div>
          <div class="status-chip">
            <span>有效分录</span>
            <strong>{{ effectiveEntryCount }}</strong>
          </div>
          <div class="status-chip">
            <span>暂存状态</span>
            <strong>{{ hasDraft ? '已暂存' : '未暂存' }}</strong>
          </div>
          <div class="voucher-validation-tip">
            <p v-if="validationErrors.length === 0">当前无阻断问题，可直接保存。</p>
            <p v-else>当前有 {{ validationErrors.length }} 个待处理问题。</p>
          </div>
        </aside>
      </section>

      <section class="voucher-ledger-card">
        <div class="voucher-ledger-header">
          <div class="voucher-ledger-title">记账区域</div>
          <div class="voucher-ledger-summary">
            <span>借方合计 {{ moneyText(totalDebit) }}</span>
            <span>贷方合计 {{ moneyText(totalCredit) }}</span>
            <span :class="balanceGap === 0 ? 'text-emerald-600' : 'text-amber-600'">
              差额 {{ moneyText(Math.abs(balanceGap)) }}
            </span>
          </div>
        </div>

        <div class="voucher-grid">
          <div class="voucher-grid-header voucher-grid-layout">
            <div>摘要</div>
            <div>科目名称</div>
            <div>借方金额</div>
            <div>贷方金额</div>
          </div>

          <div class="voucher-grid-body">
            <div
              v-for="(row, index) in form.entries"
              :key="row.localId"
              class="voucher-grid-row voucher-grid-layout"
              :class="{ 'voucher-grid-row-active': selectedRowIndex === index }"
              tabindex="0"
              @click="selectRow(index)"
              @focus="selectRow(index)"
              @keydown="handleGridKeydown($event, index)"
            >
              <div class="voucher-cell">
                <div class="voucher-row-index">{{ index + 1 }}</div>
                <el-input
                  v-model="row.cdigest"
                  placeholder="请输入摘要"
                  @focus="selectRow(index)"
                  @keydown="handleGridKeydown($event, index)"
                />
              </div>

              <div class="voucher-cell">
                <el-select
                  v-model="row.ccode"
                  filterable
                  clearable
                  placeholder="选择科目"
                  @focus="selectRow(index)"
                  @visible-change="selectRow(index)"
                >
                  <el-option
                    v-for="item in voucherMeta?.accountOptions || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <span class="voucher-subject-tip">{{ resolveAccountLabel(row.ccode) }}</span>
              </div>

              <div class="voucher-cell amount-cell">
                <el-input-number
                  v-model="row.md"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  placeholder="0.00"
                  @focus="selectRow(index)"
                  @keydown="handleAmountKeydown($event, index, 'md')"
                />
              </div>

              <div class="voucher-cell amount-cell">
                <el-input-number
                  v-model="row.mc"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  placeholder="0.00"
                  @focus="selectRow(index)"
                  @keydown="handleAmountKeydown($event, index, 'mc')"
                />
              </div>
            </div>
          </div>

          <div class="voucher-grid-footer voucher-grid-layout">
            <div class="voucher-footer-label">合计</div>
            <div class="voucher-footer-label">{{ currentRowLabel }}</div>
            <div class="voucher-footer-amount">{{ moneyText(totalDebit) }}</div>
            <div class="voucher-footer-amount">{{ moneyText(totalCredit) }}</div>
          </div>
        </div>
      </section>

      <section class="voucher-lower">
        <div class="voucher-assist-card">
          <div class="voucher-section-head">
            <div>
              <h2>辅助信息</h2>
              <p>围绕当前选中分录录入部门、人员、客商、项目与扩展信息。</p>
            </div>
            <div class="voucher-current-row">当前行 {{ selectedRowIndex + 1 }}</div>
          </div>

          <div class="assist-grid">
            <label class="assist-field">
              <span>部门</span>
              <el-select v-model="selectedRow.cdeptId" filterable clearable placeholder="选择部门">
                <el-option
                  v-for="item in voucherMeta?.departmentOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="assist-field">
              <span>个人</span>
              <el-select v-model="selectedRow.cpersonId" filterable clearable placeholder="选择人员">
                <el-option
                  v-for="item in voucherMeta?.employeeOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="assist-field">
              <span>客户</span>
              <el-select v-model="selectedRow.ccusId" filterable clearable placeholder="选择客户">
                <el-option
                  v-for="item in voucherMeta?.customerOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="assist-field">
              <span>供应商</span>
              <el-select v-model="selectedRow.csupId" filterable clearable placeholder="选择供应商">
                <el-option
                  v-for="item in voucherMeta?.supplierOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="assist-field">
              <span>项目大类</span>
              <el-select v-model="selectedRow.citemClass" filterable clearable placeholder="选择项目大类">
                <el-option
                  v-for="item in voucherMeta?.projectClassOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="assist-field">
              <span>项目</span>
              <el-select v-model="selectedRow.citemId" filterable clearable placeholder="选择项目">
                <el-option
                  v-for="item in voucherMeta?.projectOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="assist-field">
              <span>币种</span>
              <el-select v-model="selectedRow.cexchName" placeholder="币种">
                <el-option
                  v-for="item in voucherMeta?.currencyOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </label>

            <label class="assist-field">
              <span>汇率</span>
              <el-input-number v-model="selectedRow.nfrat" :min="0.000001" :precision="6" :controls="false" />
            </label>

            <label class="assist-field">
              <span>数量</span>
              <el-input-number
                v-model="selectedQuantity"
                :min="0"
                :precision="6"
                :controls="false"
                placeholder="数量"
              />
            </label>

            <label class="assist-field">
              <span>单价</span>
              <el-input-number
                v-model="selectedRow.unitPrice"
                :min="0"
                :precision="6"
                :controls="false"
                placeholder="单价"
              />
            </label>

            <label class="assist-field">
              <span>票号</span>
              <el-input v-model="selectedRow.ticketNo" placeholder="录入票号" />
            </label>

            <label class="assist-field">
              <span>业务日期</span>
              <el-date-picker
                v-model="selectedRow.bizDate"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
              />
            </label>
          </div>

          <label class="assist-field assist-field-wide">
            <span>备注</span>
            <el-input v-model="selectedRow.lineRemark" type="textarea" :rows="3" placeholder="当前分录备注" />
          </label>
        </div>

        <div class="voucher-side-card">
          <div class="voucher-section-head">
            <div>
              <h2>校验与提示</h2>
              <p>保存前会按借贷平衡、行内容完整性与汇率规则自动校验。</p>
            </div>
          </div>

          <div v-if="validationErrors.length" class="validation-list">
            <div v-for="item in validationErrors" :key="item" class="validation-item">
              {{ item }}
            </div>
          </div>
          <el-empty v-else description="当前尚无阻断项" />

          <div class="side-summary">
            <div class="side-summary-item">
              <span>借方合计</span>
              <strong>{{ moneyText(totalDebit) }}</strong>
            </div>
            <div class="side-summary-item">
              <span>贷方合计</span>
              <strong>{{ moneyText(totalCredit) }}</strong>
            </div>
            <div class="side-summary-item">
              <span>最后校验</span>
              <strong>{{ lastValidatedAt || '尚未校验' }}</strong>
            </div>
          </div>
        </div>
      </section>

      <footer class="voucher-signature">
        <span>记账：&nbsp;&nbsp;&nbsp;&nbsp;</span>
        <span>审核：&nbsp;&nbsp;&nbsp;&nbsp;</span>
        <span>出纳：&nbsp;&nbsp;&nbsp;&nbsp;</span>
        <span>制单：{{ form.cbill || '当前用户' }}</span>
        <span>末级：&nbsp;&nbsp;&nbsp;&nbsp;</span>
      </footer>
    </div>

    <el-dialog
      v-model="actionDialog.visible"
      :title="actionDialog.title"
      width="420px"
      destroy-on-close
    >
      <div class="action-dialog-content">
        <p>{{ actionDialog.description }}</p>
        <p class="action-dialog-subtle">本轮先保留成熟入口，后续可直接接入正式业务流程。</p>
      </div>
      <template #footer>
        <el-button @click="actionDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleClose,
  Coin,
  Delete,
  DocumentCopy,
  Download,
  Plus,
  Printer,
  RefreshLeft,
  Search,
  Select,
  Tickets,
  Top,
  TrendCharts,
  Tools
} from '@element-plus/icons-vue'
import {
  financeApi,
  type FinanceVoucherEntry,
  type FinanceVoucherForm,
  type FinanceVoucherMeta,
  type FinanceVoucherSavePayload
} from '@/api'

type ToolbarActionKey =
  | 'new'
  | 'print'
  | 'export'
  | 'copy'
  | 'reverse'
  | 'void'
  | 'insert'
  | 'delete'
  | 'searchReplace'
  | 'cashFlow'
  | 'save'
  | 'assist'
  | 'balance'
  | 'calculator'

type VoucherEntryRow = FinanceVoucherEntry & {
  localId: string
  ticketNo?: string
  bizDate?: string
  lineRemark?: string
  unitPrice?: number
}

type VoucherFormState = Omit<FinanceVoucherForm, 'entries'> & {
  entries: VoucherEntryRow[]
}

interface ToolbarAction {
  key: ToolbarActionKey
  label: string
  icon: Component
  emphasis?: 'primary' | 'secondary'
}

const DRAFT_STORAGE_KEY = 'finance-new-voucher-draft'
const MIN_ENTRY_ROWS = 8

const loading = ref(false)
const saving = ref(false)
const initializing = ref(false)
const voucherMeta = ref<FinanceVoucherMeta | null>(null)
const validationErrors = ref<string[]>([])
const hasDraft = ref(false)
const lastValidatedAt = ref('')
const selectedRowIndex = ref(0)
const actionDialog = reactive({
  visible: false,
  title: '',
  description: ''
})
let entrySeed = 0

const form = reactive<VoucherFormState>({
  companyId: '',
  iperiod: 1,
  csign: '记',
  inoId: undefined,
  dbillDate: '',
  idoc: 0,
  cbill: '',
  ctext1: '',
  ctext2: '',
  entries: ensureMinimumRows([createEntry('CNY', 1), createEntry('CNY', 2)], 'CNY')
})

const toolbarGroups: Array<{ key: string; actions: ToolbarAction[] }> = [
  {
    key: 'primary',
    actions: [
      { key: 'new', label: '增加', icon: Plus, emphasis: 'secondary' as const }
    ]
  },
  {
    key: 'edit',
    actions: [
      { key: 'print', label: '打印', icon: Printer },
      { key: 'export', label: '输出', icon: Download },
      { key: 'copy', label: '复制', icon: DocumentCopy },
      { key: 'reverse', label: '冲销', icon: RefreshLeft },
      { key: 'void', label: '作废', icon: CircleClose },
      { key: 'insert', label: '插入行', icon: Top },
      { key: 'delete', label: '删除行', icon: Delete }
    ]
  },
  {
    key: 'actions',
    actions: [
      { key: 'searchReplace', label: '查找替换', icon: Search },
      { key: 'cashFlow', label: '现金流量', icon: TrendCharts },
      { key: 'save', label: '保存', icon: Select, emphasis: 'primary' as const }
    ]
  },
  {
    key: 'tools',
    actions: [
      { key: 'assist', label: '辅助明细', icon: Tickets },
      { key: 'balance', label: '余额', icon: Coin },
      { key: 'calculator', label: '计算器', icon: Tools }
    ]
  }
]

const effectiveRows = computed(() => form.entries.filter((item) => !isEntryBlank(item)))
const effectiveEntryCount = computed(() => effectiveRows.value.length)
const totalDebit = computed(() => sumRows(effectiveRows.value, 'md'))
const totalCredit = computed(() => sumRows(effectiveRows.value, 'mc'))
const balanceGap = computed(() => Number((totalDebit.value - totalCredit.value).toFixed(2)))
const isBalanced = computed(() => effectiveEntryCount.value >= 2 && balanceGap.value === 0 && validationErrors.value.length === 0)
const voucherCodeText = computed(() => `${form.csign || '记'}-${String(form.inoId || '').padStart(4, '0') || '0000'}`)

const selectedRow = computed<VoucherEntryRow>(() => {
  const index = Math.min(selectedRowIndex.value, form.entries.length - 1)
  return form.entries[index] as VoucherEntryRow
})

const selectedQuantity = computed<number | undefined>({
  get() {
    return selectedRow.value.md ? selectedRow.value.ndS : selectedRow.value.ncS
  },
  set(value) {
    if (selectedRow.value.md) {
      selectedRow.value.ndS = value
      selectedRow.value.ncS = undefined
      return
    }
    if (selectedRow.value.mc) {
      selectedRow.value.ncS = value
      selectedRow.value.ndS = undefined
      return
    }
    selectedRow.value.ndS = value
    selectedRow.value.ncS = undefined
  }
})

const currentRowLabel = computed(() => {
  const row = selectedRow.value
  return row.ccode ? resolveAccountLabel(row.ccode) : '当前行'
})

watch(
  () => form.dbillDate,
  (value) => {
    if (initializing.value) {
      return
    }
    const nextPeriod = inferPeriod(value)
    if (nextPeriod) {
      form.iperiod = nextPeriod
    }
  }
)

watch(
  () => [form.companyId, form.dbillDate, form.csign] as const,
  async () => {
    if (initializing.value || loading.value || !voucherMeta.value) {
      return
    }
    await refreshSuggestedVoucherNo()
  }
)

watch(
  () => form.entries.length,
  () => {
    if (selectedRowIndex.value >= form.entries.length) {
      selectedRowIndex.value = Math.max(0, form.entries.length - 1)
    }
  }
)

onMounted(async () => {
  await loadMeta()
})

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadMeta() {
  loading.value = true
  initializing.value = true
  try {
    const res = await financeApi.getVoucherMeta()
    voucherMeta.value = res.data
    const draft = readDraft()
    if (draft) {
      applyDraft(draft, res.data)
      hasDraft.value = true
      ElMessage.success('已恢复暂存草稿')
    } else {
      resetFormFromMeta(res.data)
      hasDraft.value = false
    }
    validationErrors.value = []
    lastValidatedAt.value = ''
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载凭证配置失败'))
  } finally {
    initializing.value = false
    loading.value = false
  }
}

function resetFormFromMeta(meta: FinanceVoucherMeta) {
  form.companyId = meta.defaultCompanyId || ''
  form.iperiod = meta.defaultPeriod
  form.csign = meta.defaultVoucherType
  form.inoId = meta.suggestedVoucherNo
  form.dbillDate = meta.defaultBillDate
  form.idoc = meta.defaultAttachedDocCount
  form.cbill = meta.defaultMaker
  form.ctext1 = ''
  form.ctext2 = ''
  form.entries = ensureMinimumRows([
    createEntry(meta.defaultCurrency, 1),
    createEntry(meta.defaultCurrency, 2)
  ], meta.defaultCurrency)
  selectedRowIndex.value = 0
}

function applyDraft(draft: FinanceVoucherSavePayload, meta: FinanceVoucherMeta) {
  form.companyId = draft.companyId || meta.defaultCompanyId || ''
  form.iperiod = draft.iperiod || meta.defaultPeriod
  form.csign = draft.csign || meta.defaultVoucherType
  form.inoId = draft.inoId || meta.suggestedVoucherNo
  form.dbillDate = draft.dbillDate || meta.defaultBillDate
  form.idoc = draft.idoc ?? meta.defaultAttachedDocCount
  form.cbill = draft.cbill || meta.defaultMaker
  form.ctext1 = draft.ctext1 || ''
  form.ctext2 = draft.ctext2 || ''
  form.entries = ensureMinimumRows(
    draft.entries?.length
      ? draft.entries.map((item, index) => createEntryFromValue(item, meta.defaultCurrency, index + 1))
      : [createEntry(meta.defaultCurrency, 1), createEntry(meta.defaultCurrency, 2)],
    meta.defaultCurrency
  )
  selectedRowIndex.value = 0
}

function createEntry(defaultCurrency: string, rowNo: number): VoucherEntryRow {
  entrySeed += 1
  return {
    localId: `entry-${Date.now()}-${entrySeed}`,
    inid: rowNo,
    cdigest: '',
    ccode: '',
    cdeptId: '',
    cpersonId: '',
    ccusId: '',
    csupId: '',
    citemClass: '',
    citemId: '',
    cexchName: defaultCurrency,
    nfrat: 1,
    md: undefined,
    mc: undefined,
    ndS: undefined,
    ncS: undefined,
    ticketNo: '',
    bizDate: '',
    lineRemark: '',
    unitPrice: undefined
  }
}

function createEntryFromValue(entry: FinanceVoucherEntry, defaultCurrency: string, rowNo: number): VoucherEntryRow {
  const next = createEntry(defaultCurrency, rowNo)
  return {
    ...next,
    ...entry,
    inid: rowNo,
    cexchName: entry.cexchName || defaultCurrency,
    nfrat: entry.nfrat ?? 1
  }
}

function ensureMinimumRows(entries: VoucherEntryRow[], defaultCurrency: string) {
  const nextEntries = [...entries]
  while (nextEntries.length < MIN_ENTRY_ROWS) {
    nextEntries.push(createEntry(defaultCurrency, nextEntries.length + 1))
  }
  return nextEntries.map((item, index) => ({
    ...item,
    inid: index + 1
  }))
}

function readDraft(): FinanceVoucherSavePayload | null {
  const raw = window.sessionStorage.getItem(DRAFT_STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as FinanceVoucherSavePayload
  } catch {
    window.sessionStorage.removeItem(DRAFT_STORAGE_KEY)
    return null
  }
}

function writeDraft() {
  window.sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(buildPayload(true)))
  hasDraft.value = true
}

function clearDraft() {
  window.sessionStorage.removeItem(DRAFT_STORAGE_KEY)
  hasDraft.value = false
}

function buildPayload(includeBlankRows = false): FinanceVoucherSavePayload {
  const entries = (includeBlankRows ? form.entries : effectiveRows.value).map((item, index) => ({
    inid: index + 1,
    cdigest: (item.cdigest || '').trim(),
    ccode: item.ccode || '',
    cdeptId: item.cdeptId || undefined,
    cpersonId: item.cpersonId || undefined,
    ccusId: item.ccusId || undefined,
    csupId: item.csupId || undefined,
    citemClass: item.citemClass || undefined,
    citemId: item.citemId || undefined,
    cexchName: item.cexchName || voucherMeta.value?.defaultCurrency || 'CNY',
    nfrat: normalizeDecimal(item.nfrat),
    md: normalizeDecimal(item.md),
    mc: normalizeDecimal(item.mc),
    ndS: normalizeQuantity(item.ndS),
    ncS: normalizeQuantity(item.ncS)
  }))

  return {
    companyId: form.companyId,
    iperiod: form.iperiod,
    csign: form.csign,
    inoId: form.inoId,
    dbillDate: form.dbillDate,
    idoc: form.idoc,
    cbill: form.cbill,
    ctext1: (form.ctext1 || '').trim(),
    ctext2: (form.ctext2 || '').trim(),
    entries
  }
}

function validateVoucher(showToast = false) {
  const errors: string[] = []
  const entries = effectiveRows.value

  if (!form.companyId) {
    errors.push('请选择公司主体')
  }
  if (!form.dbillDate) {
    errors.push('请选择制单日期')
  }
  if (!form.csign) {
    errors.push('请选择凭证类别')
  }
  if (!form.iperiod || form.iperiod < 1 || form.iperiod > 12) {
    errors.push('会计期间必须在 1 到 12 之间')
  }
  if (entries.length < 2) {
    errors.push('至少需要两条有效分录')
  }

  entries.forEach((row, index) => {
    const rowNo = index + 1
    const debit = normalizeDecimal(row.md) || 0
    const credit = normalizeDecimal(row.mc) || 0
    const qtyDebit = normalizeQuantity(row.ndS)
    const qtyCredit = normalizeQuantity(row.ncS)

    if (!(row.cdigest || '').trim()) {
      errors.push(`第 ${rowNo} 行摘要不能为空`)
    }
    if (!row.ccode) {
      errors.push(`第 ${rowNo} 行请选择科目`)
    }
    if (debit > 0 && credit > 0) {
      errors.push(`第 ${rowNo} 行借贷不能同时填写`)
    }
    if (debit === 0 && credit === 0) {
      errors.push(`第 ${rowNo} 行借方或贷方至少填写一项`)
    }
    if ((row.nfrat ?? 1) <= 0) {
      errors.push(`第 ${rowNo} 行汇率必须大于 0`)
    }
    if ((qtyDebit ?? 0) > 0 && (qtyCredit ?? 0) > 0) {
      errors.push(`第 ${rowNo} 行数量借贷不能同时填写`)
    }
    if ((qtyDebit ?? 0) < 0 || (qtyCredit ?? 0) < 0) {
      errors.push(`第 ${rowNo} 行数量不能为负数`)
    }
  })

  if (entries.length >= 2 && Number((totalDebit.value - totalCredit.value).toFixed(2)) !== 0) {
    errors.push('借方合计必须等于贷方合计')
  }

  validationErrors.value = Array.from(new Set(errors))
  lastValidatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (showToast) {
    if (validationErrors.value.length > 0) {
      ElMessage.warning(validationErrors.value[0])
    } else {
      ElMessage.success('凭证校验通过，借贷已平衡')
    }
  }

  return validationErrors.value.length === 0
}

function isEntryBlank(row: VoucherEntryRow) {
  return !row.cdigest?.trim() &&
    !row.ccode &&
    !row.cdeptId &&
    !row.cpersonId &&
    !row.ccusId &&
    !row.csupId &&
    !row.citemClass &&
    !row.citemId &&
    !row.md &&
    !row.mc &&
    !row.ndS &&
    !row.ncS &&
    !row.ticketNo &&
    !row.lineRemark &&
    !row.unitPrice
}

function sumRows(rows: FinanceVoucherEntry[], field: 'md' | 'mc') {
  return Number(
    rows
      .reduce((total, row) => total + (normalizeDecimal(row[field]) || 0), 0)
      .toFixed(2)
  )
}

function normalizeDecimal(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value)) || Number(value) === 0) {
    return undefined
  }
  return Number(Number(value).toFixed(2))
}

function normalizeQuantity(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value)) || Number(value) === 0) {
    return undefined
  }
  return Number(Number(value).toFixed(6))
}

function inferPeriod(value: string) {
  if (!value) {
    return undefined
  }
  const parts = value.split('-')
  if (parts.length < 2) {
    return undefined
  }
  const month = Number(parts[1])
  return Number.isFinite(month) && month >= 1 && month <= 12 ? month : undefined
}

async function refreshSuggestedVoucherNo() {
  try {
    const res = await financeApi.getVoucherMeta({
      companyId: form.companyId,
      billDate: form.dbillDate,
      csign: form.csign
    })
    voucherMeta.value = res.data
    form.inoId = res.data.suggestedVoucherNo
    if (!form.cbill) {
      form.cbill = res.data.defaultMaker
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '刷新凭证编号失败'))
  }
}

function selectRow(index: number) {
  selectedRowIndex.value = Math.max(0, Math.min(index, form.entries.length - 1))
}

function appendEntry() {
  const currency = voucherMeta.value?.defaultCurrency || 'CNY'
  form.entries.push(createEntry(currency, form.entries.length + 1))
  form.entries = ensureMinimumRows(form.entries, currency)
  selectRow(form.entries.length - 1)
}

function insertEntryAfter(index: number) {
  const currency = voucherMeta.value?.defaultCurrency || 'CNY'
  form.entries.splice(index + 1, 0, createEntry(currency, index + 2))
  form.entries = ensureMinimumRows(form.entries, currency)
  selectRow(index + 1)
}

function removeSelectedEntry() {
  if (!form.entries.length) {
    return
  }

  const effectiveCount = effectiveRows.value.length
  if (effectiveCount <= 2 && !isEntryBlank(selectedRow.value)) {
    ElMessage.warning('至少保留两条有效分录')
    return
  }

  form.entries.splice(selectedRowIndex.value, 1)
  form.entries = ensureMinimumRows(form.entries, voucherMeta.value?.defaultCurrency || 'CNY')
  selectRow(Math.max(0, selectedRowIndex.value - 1))
}

async function handleNewVoucher() {
  try {
    await ElMessageBox.confirm('将清空当前录入内容并开始新的凭证，是否继续？', '新增凭证', {
      type: 'warning',
      confirmButtonText: '继续',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  clearDraft()
  if (voucherMeta.value) {
    resetFormFromMeta(voucherMeta.value)
    validationErrors.value = []
    lastValidatedAt.value = ''
  } else {
    await loadMeta()
  }
}

function handleSaveDraft() {
  writeDraft()
  ElMessage.success('凭证草稿已暂存')
}

function handleValidate() {
  validateVoucher(true)
}

async function handleSave() {
  if (!validateVoucher(true)) {
    return
  }

  saving.value = true
  try {
    const currentContext = {
      companyId: form.companyId,
      billDate: form.dbillDate,
      csign: form.csign
    }
    const res = await financeApi.createVoucher(buildPayload())
    clearDraft()
    ElMessage.success(`凭证保存成功：${res.data.voucherNo}`)
    const nextMeta = await financeApi.getVoucherMeta(currentContext)
    voucherMeta.value = nextMeta.data
    resetFormFromMeta(nextMeta.data)
    validationErrors.value = []
    lastValidatedAt.value = ''
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存凭证失败'))
  } finally {
    saving.value = false
  }
}

function handleToolbarAction(action: ToolbarActionKey) {
  if (action === 'new') {
    void handleNewVoucher()
    return
  }
  if (action === 'insert') {
    insertEntryAfter(selectedRowIndex.value)
    return
  }
  if (action === 'delete') {
    removeSelectedEntry()
    return
  }
  if (action === 'save') {
    void handleSave()
    return
  }

  const placeholders: Record<Exclude<ToolbarActionKey, 'new' | 'insert' | 'delete' | 'save'>, { title: string; description: string }> = {
    print: { title: '打印', description: '后续可接入正式打印模板与套打配置，本期先保留统一入口。' },
    export: { title: '输出', description: '后续可扩展为 Excel、PDF 或外部接口输出。' },
    copy: { title: '复制', description: '后续可按原凭证复制摘要、科目和金额，支持快速成单。' },
    reverse: { title: '冲销', description: '后续可接入红字冲销与反向凭证生成流程。' },
    void: { title: '作废', description: '后续可接入作废状态流转和权限校验。' },
    searchReplace: { title: '查找替换', description: '后续可在分录摘要、科目和辅助项中做批量查找替换。' },
    cashFlow: { title: '现金流量', description: '后续可关联现金流量项目并形成补录面板。' },
    assist: { title: '辅助明细', description: '当前下方辅助区已可录入基础信息，后续可扩展为侧边明细抽屉。' },
    balance: { title: '余额', description: '后续可联动余额查询与科目实时余额提示。' },
    calculator: { title: '计算器', description: '后续可接入悬浮计算器或公式辅助输入能力。' }
  }

  const config = placeholders[action]
  actionDialog.title = config.title
  actionDialog.description = config.description
  actionDialog.visible = true
}

function handleGridKeydown(event: KeyboardEvent, index: number) {
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    selectRow(index - 1)
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    selectRow(index + 1)
  }
  if (event.key === 'Insert') {
    event.preventDefault()
    insertEntryAfter(index)
  }
}

function handleAmountKeydown(event: KeyboardEvent, index: number, field: 'md' | 'mc') {
  handleGridKeydown(event, index)
  const row = form.entries[index]
  if (!row) {
    return
  }
  if (field === 'md' && row.md) {
    row.mc = undefined
    row.ncS = undefined
  }
  if (field === 'mc' && row.mc) {
    row.md = undefined
    row.ndS = undefined
  }
}

function resolveAccountLabel(code?: string) {
  if (!code) {
    return '未选择科目'
  }
  const option = voucherMeta.value?.accountOptions.find((item) => item.value === code)
  return option?.label || code
}

function moneyText(value: number) {
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}
</script>

<style scoped>
.voucher-page {
  height: 100%;
}

.voucher-shell {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 16px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(96, 165, 250, 0.1), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #f3f6fb 100%);
  padding: 22px;
}

.voucher-page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 0;
}

.voucher-page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1e3a5f;
  letter-spacing: 0.28em;
}

.voucher-toolbar-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  border-radius: 22px;
  border: 1px solid #d8e2f0;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px 16px;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.05);
}

.toolbar-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.toolbar-group + .toolbar-group {
  position: relative;
  padding-left: 16px;
}

.toolbar-group + .toolbar-group::before {
  position: absolute;
  left: 0;
  top: 6px;
  height: 36px;
  width: 1px;
  background: linear-gradient(180deg, transparent 0%, #d5deea 22%, #d5deea 78%, transparent 100%);
  content: '';
}

.toolbar-button {
  height: 40px;
  min-width: 96px;
  border-radius: 14px;
  border-color: #d6e0ec;
  background: #fff;
  color: #365070;
  font-weight: 600;
}

.toolbar-button-large {
  height: 52px;
  min-width: 130px;
  padding: 0 20px;
  font-size: 15px;
}

.toolbar-button-accent {
  border-color: #9cbbe3;
  background: linear-gradient(180deg, #f0f7ff 0%, #e4efff 100%);
  color: #24528a;
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.14);
}

.toolbar-button-primary {
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.2);
}

.voucher-info-band {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 16px;
}

.voucher-info-main,
.voucher-info-side,
.voucher-ledger-card,
.voucher-assist-card,
.voucher-side-card {
  border-radius: 24px;
  border: 1px solid #d8e2f0;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.04);
}

.voucher-info-main,
.voucher-assist-card,
.voucher-side-card {
  padding: 18px;
}

.voucher-info-grid,
.assist-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px 16px;
}

.voucher-info-field,
.assist-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  grid-column: span 3;
}

.voucher-info-field span,
.assist-field span {
  font-size: 12px;
  font-weight: 600;
  color: #5f7391;
}

.voucher-info-code,
.voucher-info-attach,
.voucher-info-period {
  grid-column: span 2;
}

.voucher-code-box {
  display: flex;
  height: 40px;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  border: 1px solid #cfe0f5;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  font-weight: 700;
  color: #24466f;
}

.voucher-info-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
}

.status-chip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-radius: 18px;
  border: 1px solid #d8e2f0;
  background: linear-gradient(180deg, #f8fbff 0%, #f2f6fc 100%);
  padding: 12px 14px;
}

.status-chip span {
  font-size: 12px;
  color: #67809d;
}

.status-chip strong {
  color: #1d3557;
  font-size: 16px;
}

.status-chip-balanced {
  border-color: #bae6c7;
  background: linear-gradient(180deg, #f0fdf4 0%, #ecfdf3 100%);
}

.status-chip-warning {
  border-color: #fde68a;
  background: linear-gradient(180deg, #fff9db 0%, #fff7c4 100%);
}

.voucher-validation-tip {
  margin-top: auto;
  border-radius: 18px;
  background: #f5f8fd;
  padding: 12px 14px;
  color: #546a87;
  font-size: 13px;
  line-height: 1.7;
}

.voucher-ledger-card {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  padding: 18px;
}

.voucher-ledger-header,
.voucher-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.voucher-ledger-title,
.voucher-section-head h2 {
  font-size: 18px;
  font-weight: 700;
  color: #1d3557;
}

.voucher-ledger-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #5d7491;
  font-size: 13px;
}

.voucher-grid {
  margin-top: 16px;
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid #d7e0eb;
  background: #fdfefe;
}

.voucher-grid-layout {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) minmax(280px, 1.4fr) minmax(160px, 0.8fr) minmax(160px, 0.8fr);
}

.voucher-grid-header,
.voucher-grid-footer {
  flex-shrink: 0;
  background: linear-gradient(180deg, #f3f7fd 0%, #edf3fb 100%);
  color: #49627f;
  font-size: 13px;
  font-weight: 700;
}

.voucher-grid-header > div,
.voucher-grid-footer > div {
  padding: 12px 14px;
}

.voucher-grid-body {
  min-height: 0;
  flex: 1;
  overflow: auto;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.56) 0%, rgba(255, 255, 255, 0.92) 100%);
}

.voucher-grid-row {
  min-height: 72px;
  border-top: 1px solid #e4ebf4;
  transition: background-color 0.16s ease, box-shadow 0.16s ease;
}

.voucher-grid-row:first-child {
  border-top: 0;
}

.voucher-grid-row:hover {
  background: rgba(239, 246, 255, 0.72);
}

.voucher-grid-row-active {
  background: rgba(219, 234, 254, 0.5);
  box-shadow: inset 4px 0 0 #4f8ad8;
}

.voucher-grid-row:focus {
  outline: none;
}

.voucher-cell {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
}

.voucher-row-index {
  font-size: 12px;
  color: #7c8fa7;
}

.voucher-subject-tip {
  min-height: 18px;
  color: #788ca6;
  font-size: 12px;
  line-height: 1.5;
}

.voucher-footer-label {
  color: #39557a;
}

.voucher-footer-amount {
  text-align: right;
  color: #173a61;
  font-family: Consolas, Monaco, monospace;
}

.voucher-lower {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.voucher-section-head p {
  margin-top: 6px;
  color: #7b8ea6;
  font-size: 13px;
}

.voucher-current-row {
  border-radius: 999px;
  background: #edf4ff;
  padding: 8px 12px;
  color: #31598d;
  font-size: 12px;
  font-weight: 700;
}

.assist-field-wide {
  margin-top: 14px;
}

.validation-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.validation-item {
  border-radius: 16px;
  border: 1px solid #f2d59c;
  background: #fff8e8;
  padding: 11px 12px;
  color: #9a6700;
  font-size: 13px;
  line-height: 1.7;
}

.side-summary {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.side-summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 16px;
  background: #f5f8fd;
  padding: 11px 12px;
  color: #5d7390;
  font-size: 13px;
}

.side-summary-item strong {
  color: #173a61;
  font-size: 15px;
}

.voucher-signature {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  border-radius: 18px;
  border: 1px solid #d8e2f0;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px 16px;
  color: #4a627f;
  font-size: 13px;
}

.action-dialog-content {
  color: #506680;
  line-height: 1.8;
}

.action-dialog-subtle {
  margin-top: 8px;
  color: #8a9bb1;
  font-size: 12px;
}

:deep(.voucher-info-field .el-input__wrapper),
:deep(.voucher-info-field .el-select__wrapper),
:deep(.voucher-info-field .el-date-editor),
:deep(.assist-field .el-input__wrapper),
:deep(.assist-field .el-select__wrapper),
:deep(.assist-field .el-date-editor),
:deep(.voucher-cell .el-input__wrapper),
:deep(.voucher-cell .el-select__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #d8e2f0 inset;
}

:deep(.voucher-cell .el-input-number),
:deep(.voucher-cell .el-input-number .el-input__wrapper),
:deep(.assist-field .el-input-number),
:deep(.assist-field .el-input-number .el-input__wrapper),
:deep(.voucher-info-field .el-input-number),
:deep(.voucher-info-field .el-input-number .el-input__wrapper) {
  width: 100%;
}

@media (max-width: 1440px) {
  .voucher-info-band,
  .voucher-lower {
    grid-template-columns: 1fr;
  }

  .voucher-info-side {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  .voucher-validation-tip {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1024px) {
  .voucher-shell {
    padding: 16px;
  }

  .voucher-page-header h1 {
    font-size: 24px;
    letter-spacing: 0.18em;
  }

  .voucher-info-grid,
  .assist-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .voucher-info-field,
  .assist-field {
    grid-column: span 3;
  }

  .voucher-grid-layout {
    min-width: 860px;
  }
}

@media (max-width: 768px) {
  .voucher-toolbar-panel {
    gap: 10px;
  }

  .toolbar-group {
    width: 100%;
  }

  .toolbar-group + .toolbar-group {
    padding-left: 0;
    padding-top: 10px;
  }

  .toolbar-group + .toolbar-group::before {
    left: 0;
    top: 0;
    height: 1px;
    width: 100%;
  }

  .voucher-info-grid,
  .assist-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .voucher-info-field,
  .assist-field,
  .voucher-info-code,
  .voucher-info-attach,
  .voucher-info-period {
    grid-column: span 2;
  }

  .voucher-info-side {
    grid-template-columns: 1fr;
  }

  .voucher-signature {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
