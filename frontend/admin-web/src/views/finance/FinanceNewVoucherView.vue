<template>
  <div class="voucher-page">
    <section class="voucher-toolbar-panel">
      <div v-for="group in toolbarGroups" :key="group.key" class="toolbar-group">
        <el-button
          v-for="action in group.actions"
          :key="action.key"
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
          <el-icon :size="action.emphasis ? 18 : 16"><component :is="action.icon" /></el-icon>
          <span>{{ action.label }}</span>
        </el-button>
      </div>
    </section>

    <div class="voucher-content-scroll">
      <div class="voucher-shell">
        <header class="voucher-page-header">
          <h1>{{ pageTitle }}</h1>
        </header>

        <section class="voucher-info-band">
          <div class="voucher-info-main">
            <div class="voucher-info-grid">
              <label class="voucher-info-field">
                <span>公司</span>
                <div class="voucher-code-box voucher-company-box">{{ currentCompanyName }}</div>
              </label>
              <label class="voucher-info-field voucher-info-code">
                <span>凭证编号</span>
                <div class="voucher-number-group">
                  <el-select v-model="form.csign" placeholder="类别" :disabled="voucherHeaderLocked">
                    <el-option v-for="item in voucherMeta?.voucherTypeOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                  <span class="voucher-number-separator">-</span>
                  <el-input v-model="voucherNoInput" placeholder="请输入凭证号" :readonly="voucherHeaderLocked" />
                </div>
              </label>
              <label class="voucher-info-field voucher-info-period">
                <span>期间</span>
                <el-input-number v-model="form.iperiod" :min="1" :max="12" :controls="false" :disabled="voucherHeaderLocked" />
              </label>
              <label class="voucher-info-field">
                <span>制单日期</span>
                <el-date-picker v-model="form.dbillDate" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" :disabled="isReadonlyMode" />
              </label>
              <label class="voucher-info-field voucher-info-period">
                <span>附件张数</span>
                <el-input-number v-model="form.idoc" :min="0" :controls="false" :disabled="isReadonlyMode" />
              </label>
              <label class="voucher-info-field">
                <span>制单人</span>
                <el-input v-model="form.cbill" readonly />
              </label>
              <label class="voucher-info-field voucher-info-field-note">
                <span>备注</span>
                <el-input v-model="remarkText" placeholder="请输入备注" :readonly="isReadonlyMode" />
              </label>
            </div>
          </div>

          <aside class="voucher-info-side">
            <div class="status-chip" :class="isBalanced ? 'status-chip-balanced' : 'status-chip-warning'">
              <span>平衡状态</span>
              <strong>{{ isBalanced ? '已平衡' : '待调整' }}</strong>
            </div>
            <div class="status-chip">
              <span>有效分录</span>
              <strong>{{ effectiveEntryCount }}</strong>
            </div>
            <div class="status-chip">
              <span>{{ isDetailRoute ? '凭证状态' : '草稿状态' }}</span>
              <strong>{{ statusChipText }}</strong>
            </div>
          </aside>
        </section>

        <section class="voucher-ledger-card">
          <div class="voucher-ledger-header">
            <div class="voucher-ledger-title">凭证明细</div>
            <div class="voucher-ledger-summary">
              <span>借方合计 {{ moneyText(totalDebit) }}</span>
              <span>贷方合计 {{ moneyText(totalCredit) }}</span>
              <span :class="isZeroMoney(balanceGap) ? 'text-emerald-600' : 'text-amber-600'">差额 {{ moneyText(absMoney(balanceGap)) }}</span>
            </div>
          </div>

          <div class="voucher-grid">
            <div class="voucher-grid-header voucher-grid-layout">
              <div>摘要</div>
              <div>会计科目</div>
              <div>借方金额</div>
              <div>贷方金额</div>
            </div>

            <div class="voucher-grid-body">
              <div
                v-for="(row, index) in form.entries"
                :key="row.localId"
                class="voucher-grid-row voucher-grid-layout"
                :class="{ 'voucher-grid-row-active': selectedRowIndex === index, 'voucher-grid-row-readonly': isReadonlyMode }"
                tabindex="0"
                @click="selectRow(index)"
                @focus="selectRow(index)"
                @keydown="handleGridKeydown($event, index)"
              >
                <div class="voucher-cell voucher-cell-digest">
                  <div class="voucher-inline-field">
                    <div class="voucher-row-index">{{ index + 1 }}</div>
                    <el-input v-model="row.cdigest" placeholder="请输入摘要" :readonly="isReadonlyMode" @focus="selectRow(index)" />
                  </div>
                </div>
                <div class="voucher-cell">
                  <el-select v-model="row.ccode" filterable clearable placeholder="请选择科目" :disabled="isReadonlyMode" @focus="selectRow(index)" @visible-change="selectRow(index)">
                    <el-option v-for="item in voucherMeta?.accountOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </div>
                <div class="voucher-cell">
                  <money-input v-model="row.md" placeholder="0.00" :readonly="isReadonlyMode" :disabled="isReadonlyMode" @focus="selectRow(index)" @keydown="handleAmountKeydown($event, index, 'md')" />
                </div>
                <div class="voucher-cell">
                  <money-input v-model="row.mc" placeholder="0.00" :readonly="isReadonlyMode" :disabled="isReadonlyMode" @focus="selectRow(index)" @keydown="handleAmountKeydown($event, index, 'mc')" />
                </div>
              </div>
            </div>

            <div class="voucher-grid-footer voucher-grid-layout">
              <div>合计</div>
              <div>{{ currentRowLabel }}</div>
              <div class="voucher-footer-amount">{{ moneyText(totalDebit) }}</div>
              <div class="voucher-footer-amount">{{ moneyText(totalCredit) }}</div>
            </div>
          </div>
        </section>

        <section class="voucher-lower voucher-lower-full">
          <div class="voucher-assist-card">
            <div class="voucher-section-head">
              <h2>辅助核算</h2>
              <div class="voucher-current-row">当前行 {{ selectedRowIndex + 1 }}</div>
            </div>

            <div class="assist-grid">
              <label class="assist-field">
                <span>部门</span>
                <el-select v-model="selectedRow.cdeptId" filterable clearable placeholder="请选择部门" :disabled="isReadonlyMode">
                  <el-option v-for="item in voucherMeta?.departmentOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span>人员</span>
                <el-select v-model="selectedRow.cpersonId" filterable clearable placeholder="请选择人员" :disabled="isReadonlyMode">
                  <el-option v-for="item in voucherMeta?.employeeOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span>客户</span>
                <el-select v-model="selectedRow.ccusId" filterable clearable placeholder="请选择客户" :disabled="isReadonlyMode">
                  <el-option v-for="item in voucherMeta?.customerOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span>供应商</span>
                <el-select v-model="selectedRow.csupId" filterable clearable placeholder="请选择供应商" :disabled="isReadonlyMode">
                  <el-option v-for="item in voucherMeta?.supplierOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span>项目分类</span>
                <el-select v-model="selectedRow.citemClass" filterable clearable placeholder="请选择项目分类" :disabled="isReadonlyMode">
                  <el-option v-for="item in voucherMeta?.projectClassOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span>项目</span>
                <el-select v-model="selectedRow.citemId" filterable clearable placeholder="请选择项目" :disabled="isReadonlyMode">
                  <el-option v-for="item in voucherMeta?.projectOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>
          </div>
        </section>

        <footer class="voucher-signature">
          <span>审核：</span>
          <span>记账：</span>
          <span>出纳：</span>
          <span>制单：{{ form.cbill || '未填写' }}</span>
          <span>主管：</span>
        </footer>
      </div>
    </div>

    <el-dialog v-model="actionDialog.visible" :title="actionDialog.title" width="420px" destroy-on-close>
      <div class="action-dialog-content">
        <p>{{ actionDialog.description }}</p>
        <p class="action-dialog-subtle">当前为第一阶段工作台能力，后续可继续接入正式业务流程。</p>
      </div>
      <template #footer>
        <el-button @click="actionDialog.visible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleClose,
  Coin,
  Delete,
  DocumentCopy,
  Download,
  Edit,
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
  type FinanceVoucherDetail,
  type FinanceVoucherEntry,
  type FinanceVoucherForm,
  type FinanceVoucherMeta,
  type FinanceVoucherSavePayload
} from '@/api'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { absMoney, addMoney, formatMoney, isZeroMoney, normalizeMoneyValue } from '@/utils/money'

type ToolbarActionKey = 'new' | 'modify' | 'print' | 'export' | 'copy' | 'reverse' | 'void' | 'insert' | 'delete' | 'searchReplace' | 'cashFlow' | 'save' | 'assist' | 'balance' | 'calculator'
type VoucherEntryRow = FinanceVoucherEntry & { localId: string }
type VoucherFormState = Omit<FinanceVoucherForm, 'entries'> & { entries: VoucherEntryRow[] }
type VoucherPageMode = 'create' | 'detail'

interface ToolbarAction {
  key: ToolbarActionKey
  label: string
  icon: Component
  emphasis?: 'primary' | 'secondary'
}

const DRAFT_STORAGE_KEY = 'finance-new-voucher-draft'
const MIN_ENTRY_ROWS = 8
const COMPANY_SWITCH_GUARD_KEY = 'finance-new-voucher'

const props = withDefaults(defineProps<{ pageMode?: VoucherPageMode; voucherNo?: string }>(), {
  pageMode: 'create',
  voucherNo: ''
})
const router = useRouter()
const financeCompany = useFinanceCompanyStore()
const currentUser = readStoredUser()

const loading = ref(false)
const saving = ref(false)
const initializing = ref(false)
const voucherMeta = ref<FinanceVoucherMeta | null>(null)
const voucherDetail = ref<FinanceVoucherDetail | null>(null)
const validationErrors = ref<string[]>([])
const hasDraft = ref(false)
const selectedRowIndex = ref(0)
const editingExisting = ref(false)
const lastCommittedSnapshot = ref('')
const actionDialog = reactive({ visible: false, title: '', description: '' })
const viewActive = ref(false)
let entrySeed = 0
let loadSequence = 0
let guardRegistered = false

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
const isDetailRoute = computed(() => props.pageMode === 'detail')
const detailVoucherNo = computed(() => String(props.voucherNo || ''))
const canEditExisting = computed(() => hasPermission('finance:general_ledger:query_voucher:edit', currentUser))
const isReadonlyMode = computed(() => isDetailRoute.value && !editingExisting.value)
const voucherHeaderLocked = computed(() => isDetailRoute.value)
const pageTitle = computed(() => {
  if (!isDetailRoute.value) return '新建凭证'
  return editingExisting.value ? '修改凭证' : '凭证详情'
})
const toolbarGroups = computed<Array<{ key: string; actions: ToolbarAction[] }>>(() => {
  const primaryActions: ToolbarAction[] = [{ key: 'new', label: '新增', icon: Plus, emphasis: 'secondary' }]
  if (isDetailRoute.value && !editingExisting.value && voucherDetail.value?.editable && canEditExisting.value) {
    primaryActions.push({ key: 'modify', label: '修改', icon: Edit, emphasis: 'primary' })
  }

  const editActions: ToolbarAction[] = [
    { key: 'print', label: '打印', icon: Printer },
    { key: 'export', label: '导出', icon: Download },
    { key: 'copy', label: '复制', icon: DocumentCopy },
    { key: 'reverse', label: '冲销', icon: RefreshLeft },
    { key: 'void', label: '作废', icon: CircleClose }
  ]
  if (!isReadonlyMode.value) {
    editActions.push({ key: 'insert', label: '插入行', icon: Top })
    editActions.push({ key: 'delete', label: '删行', icon: Delete })
  }

  const actionGroup: ToolbarAction[] = [
    { key: 'searchReplace', label: '查找替换', icon: Search },
    { key: 'cashFlow', label: '现金流量', icon: TrendCharts }
  ]
  if (!isReadonlyMode.value) {
    actionGroup.push({ key: 'save', label: '保存', icon: Select, emphasis: 'primary' })
  }

  return [
    { key: 'primary', actions: primaryActions },
    { key: 'edit', actions: editActions },
    { key: 'actions', actions: actionGroup },
    {
      key: 'tools',
      actions: [
        { key: 'assist', label: '辅助核算', icon: Tickets },
        { key: 'balance', label: '平衡', icon: Coin },
        { key: 'calculator', label: '计算器', icon: Tools }
      ]
    }
  ]
})

const effectiveRows = computed(() => form.entries.filter((item) => !isEntryBlank(item)))
const effectiveEntryCount = computed(() => effectiveRows.value.length)
const totalDebit = computed(() => sumRows(effectiveRows.value, 'md'))
const totalCredit = computed(() => sumRows(effectiveRows.value, 'mc'))
const balanceGap = computed(() => subtractVoucherAmount(totalDebit.value, totalCredit.value))
const isBalanced = computed(() => effectiveEntryCount.value >= 2 && isZeroMoney(balanceGap.value) && validationErrors.value.length === 0)
const selectedRow = computed(() => form.entries[Math.min(selectedRowIndex.value, Math.max(form.entries.length - 1, 0))] as VoucherEntryRow)
const currentRowLabel = computed(() => (selectedRow.value?.ccode ? resolveAccountLabel(selectedRow.value.ccode) : '当前行'))
const currentCompanyName = computed(() => financeCompany.currentCompanyName || resolveCompanyName(form.companyId))
const hasUnsavedChanges = computed(() => Boolean(voucherMeta.value) && buildSnapshot() !== lastCommittedSnapshot.value)
const statusChipText = computed(() => (isDetailRoute.value ? voucherDetail.value?.statusLabel || '未记账' : hasDraft.value ? '已暂存' : '未暂存'))
const remarkText = computed({
  get: () => form.ctext2 || form.ctext1 || '',
  set: (value: string) => {
    form.ctext1 = ''
    form.ctext2 = value
  }
})
const voucherNoInput = computed({
  get: () => (form.inoId === undefined || form.inoId === null ? '' : String(form.inoId)),
  set: (value: string) => {
    if (voucherHeaderLocked.value) return
    const digits = String(value || '').replace(/\D/g, '')
    form.inoId = digits ? Number(digits) : undefined
  }
})

watch(() => form.dbillDate, (value) => {
  if (initializing.value || voucherHeaderLocked.value) return
  const nextPeriod = inferPeriod(value)
  if (nextPeriod) form.iperiod = nextPeriod
})

watch(() => [form.dbillDate, form.csign] as const, async () => {
  if (initializing.value || loading.value || !voucherMeta.value || voucherHeaderLocked.value) return
  await refreshSuggestedVoucherNo()
})

watch(() => form.entries.length, () => {
  if (selectedRowIndex.value >= form.entries.length) {
    selectedRowIndex.value = Math.max(0, form.entries.length - 1)
  }
})

watch(() => financeCompany.currentCompanyId, async (companyId, previousCompanyId) => {
  if (!viewActive.value || !companyId || companyId === previousCompanyId) return
  await initializePage()
})

watch(() => [props.pageMode, detailVoucherNo.value] as const, async ([pageMode, voucherNo], previousValue) => {
  if (!viewActive.value) return
  if (previousValue && pageMode === previousValue[0] && voucherNo === previousValue[1]) return
  await initializePage()
})

onMounted(activateView)
onActivated(activateView)
onDeactivated(deactivateView)

onBeforeUnmount(() => {
  deactivateView()
})

async function initializePage() {
  const companyId = financeCompany.currentCompanyId
  if (!companyId || !viewActive.value) return
  const loadId = beginLoad()

  if (isDetailRoute.value) {
    const voucherCompanyId = parseVoucherCompanyId(detailVoucherNo.value)
    if (voucherCompanyId && voucherCompanyId !== companyId) {
      if (!isLiveLoad(loadId)) return
      editingExisting.value = false
      voucherDetail.value = null
      await router.replace({ name: 'finance-query-voucher' })
      return
    }
    await loadDetail(companyId, detailVoucherNo.value, loadId)
    return
  }

  if (!isLiveLoad(loadId)) return
  editingExisting.value = false
  voucherDetail.value = null
  await loadMeta(companyId, loadId)
}

async function loadMeta(companyId = financeCompany.currentCompanyId, loadId = beginLoad()) {
  if (!companyId) return
  loading.value = true
  initializing.value = true
  try {
    const res = await financeApi.getVoucherMeta({ companyId })
    if (!isLiveLoad(loadId)) return
    voucherMeta.value = res.data
    const draft = readDraft(companyId)
    hasDraft.value = Boolean(draft)
    if (draft) {
      applyDraft(draft, res.data, companyId)
      ElMessage.success('已恢复暂存草稿')
    } else {
      resetFormFromMeta(res.data, companyId)
    }
    validationErrors.value = []
    markCommitted()
  } catch (error: unknown) {
    if (isLiveLoad(loadId)) {
      ElMessage.error(resolveErrorMessage(error, '加载凭证配置失败'))
    }
  } finally {
    if (isLiveLoad(loadId)) {
      initializing.value = false
      loading.value = false
    }
  }
}

async function loadDetail(companyId: string, voucherNo: string, loadId = beginLoad()) {
  if (!companyId || !voucherNo) return
  loading.value = true
  initializing.value = true
  try {
    const detailRes = await financeApi.getVoucherDetail(companyId, voucherNo)
    if (!isLiveLoad(loadId)) return
    const metaRes = await financeApi.getVoucherMeta({ companyId, billDate: detailRes.data.dbillDate, csign: detailRes.data.csign })
    if (!isLiveLoad(loadId)) return
    voucherMeta.value = metaRes.data
    voucherDetail.value = detailRes.data
    applyDetail(detailRes.data, metaRes.data)
    editingExisting.value = false
    hasDraft.value = false
    validationErrors.value = []
    markCommitted()
  } catch (error: unknown) {
    if (isLiveLoad(loadId)) {
      ElMessage.error(resolveErrorMessage(error, '加载凭证详情失败'))
    }
  } finally {
    if (isLiveLoad(loadId)) {
      initializing.value = false
      loading.value = false
    }
  }
}

function resetFormFromMeta(meta: FinanceVoucherMeta, companyId = financeCompany.currentCompanyId) {
  form.companyId = companyId || meta.defaultCompanyId || ''
  form.iperiod = meta.defaultPeriod
  form.csign = meta.defaultVoucherType
  form.inoId = meta.suggestedVoucherNo
  form.dbillDate = meta.defaultBillDate
  form.idoc = meta.defaultAttachedDocCount
  form.cbill = meta.defaultMaker
  form.ctext1 = ''
  form.ctext2 = ''
  form.entries = ensureMinimumRows([createEntry(meta.defaultCurrency, 1), createEntry(meta.defaultCurrency, 2)], meta.defaultCurrency)
  selectedRowIndex.value = 0
}

function applyDraft(draft: FinanceVoucherSavePayload, meta: FinanceVoucherMeta, companyId = financeCompany.currentCompanyId) {
  form.companyId = companyId || draft.companyId || meta.defaultCompanyId || ''
  form.iperiod = draft.iperiod || meta.defaultPeriod
  form.csign = draft.csign || meta.defaultVoucherType
  form.inoId = draft.inoId || meta.suggestedVoucherNo
  form.dbillDate = draft.dbillDate || meta.defaultBillDate
  form.idoc = draft.idoc ?? meta.defaultAttachedDocCount
  form.cbill = draft.cbill || meta.defaultMaker
  form.ctext1 = draft.ctext1 || ''
  form.ctext2 = draft.ctext2 || ''
  form.entries = ensureMinimumRows((draft.entries?.length ? draft.entries : [createEntry(meta.defaultCurrency, 1), createEntry(meta.defaultCurrency, 2)]).map((item, index) => createEntryFromValue(item, meta.defaultCurrency, index + 1)), meta.defaultCurrency)
  selectedRowIndex.value = 0
}

function applyDetail(detail: FinanceVoucherDetail, meta: FinanceVoucherMeta) {
  form.companyId = detail.companyId
  form.iperiod = detail.iperiod
  form.csign = detail.csign
  form.inoId = detail.inoId
  form.dbillDate = detail.dbillDate
  form.idoc = detail.idoc
  form.cbill = detail.cbill
  form.ctext1 = detail.ctext1 || ''
  form.ctext2 = detail.ctext2 || ''
  form.entries = ensureMinimumRows(detail.entries.map((item, index) => createEntryFromValue(item, meta.defaultCurrency, index + 1)), meta.defaultCurrency, Math.max(detail.entries.length, 2))
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
    md: '',
    mc: '',
    ndS: undefined,
    ncS: undefined
  }
}

function createEntryFromValue(entry: FinanceVoucherEntry, defaultCurrency: string, rowNo: number): VoucherEntryRow {
  return {
    ...createEntry(defaultCurrency, rowNo),
    ...entry,
    inid: rowNo,
    cexchName: entry.cexchName || defaultCurrency,
    nfrat: entry.nfrat ?? 1
  }
}

function ensureMinimumRows(entries: VoucherEntryRow[], defaultCurrency: string, minRows = MIN_ENTRY_ROWS) {
  const nextEntries = [...entries]
  while (nextEntries.length < minRows) nextEntries.push(createEntry(defaultCurrency, nextEntries.length + 1))
  return nextEntries.map((item, index) => ({ ...item, inid: index + 1 }))
}

function readDraft(companyId = financeCompany.currentCompanyId): FinanceVoucherSavePayload | null {
  const raw = window.sessionStorage.getItem(buildDraftStorageKey(companyId))
  if (!raw) return null
  try {
    return JSON.parse(raw) as FinanceVoucherSavePayload
  } catch {
    window.sessionStorage.removeItem(buildDraftStorageKey(companyId))
    return null
  }
}

function clearDraft(companyId = financeCompany.currentCompanyId) {
  window.sessionStorage.removeItem(buildDraftStorageKey(companyId))
  hasDraft.value = false
}

function buildPayload(includeBlankRows = false): FinanceVoucherSavePayload {
  const note = (remarkText.value || '').trim()
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
    md: normalizeMoneyField(item.md),
    mc: normalizeMoneyField(item.mc),
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
    ctext1: '',
    ctext2: note,
    entries
  }
}

function validateVoucher(showToast = false) {
  const errors: string[] = []
  const entries = effectiveRows.value

  if (!form.companyId) errors.push('当前公司未设置')
  if (!form.dbillDate) errors.push('请选择制单日期')
  if (!form.csign) errors.push('请选择凭证类别')
  if (!form.iperiod || form.iperiod < 1 || form.iperiod > 12) errors.push('会计期间必须在 1 到 12 之间')
  if (entries.length < 2) errors.push('至少需要两条有效分录')

  entries.forEach((row, index) => {
    const rowNo = index + 1
    const debit = normalizeMoneyField(row.md)
    const credit = normalizeMoneyField(row.mc)
    if (!row.cdigest.trim()) errors.push(`第 ${rowNo} 行摘要不能为空`)
    if (!row.ccode) errors.push(`第 ${rowNo} 行请选择科目`)
    if (debit && credit) errors.push(`第 ${rowNo} 行借贷不能同时填写`)
    if (!debit && !credit) errors.push(`第 ${rowNo} 行借方或贷方至少填写一项`)
    if ((row.nfrat ?? 1) <= 0) errors.push(`第 ${rowNo} 行汇率必须大于 0`)
  })

  if (entries.length >= 2 && !isZeroMoney(balanceGap.value)) errors.push('借方合计必须等于贷方合计')

  validationErrors.value = Array.from(new Set(errors))
  if (showToast) {
    validationErrors.value.length ? ElMessage.warning(validationErrors.value[0]) : ElMessage.success('凭证校验通过，借贷已平衡')
  }
  return validationErrors.value.length === 0
}

function isEntryBlank(row: VoucherEntryRow) {
  return !row.cdigest.trim() && !row.ccode && !row.cdeptId && !row.cpersonId && !row.ccusId && !row.csupId && !row.citemClass && !row.citemId && !normalizeMoneyField(row.md) && !normalizeMoneyField(row.mc) && !row.ndS && !row.ncS
}

function sumRows(rows: FinanceVoucherEntry[], field: 'md' | 'mc') {
  return rows.reduce((total, row) => addMoney(total, normalizeMoneyField(row[field]) || '0.00'), '0.00')
}

function normalizeDecimal(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value)) || Number(value) === 0) return undefined
  return Number(Number(value).toFixed(2))
}

function normalizeMoneyField(value?: string) {
  if (!value) return undefined
  const normalized = normalizeMoneyValue(value, { fallback: '' })
  return isZeroMoney(normalized) ? undefined : normalized
}

function subtractVoucherAmount(left: string, right: string) {
  return addMoney(left, right ? `-${right}` : '0.00')
}

function normalizeQuantity(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value)) || Number(value) === 0) return undefined
  return Number(Number(value).toFixed(6))
}

function inferPeriod(value: string) {
  const month = Number(value?.split('-')?.[1])
  return Number.isFinite(month) && month >= 1 && month <= 12 ? month : undefined
}

async function refreshSuggestedVoucherNo() {
  try {
    const companyId = financeCompany.currentCompanyId || form.companyId
    const res = await financeApi.getVoucherMeta({ companyId, billDate: form.dbillDate, csign: form.csign })
    voucherMeta.value = res.data
    form.companyId = companyId || ''
    form.inoId = res.data.suggestedVoucherNo
    if (!form.cbill) form.cbill = res.data.defaultMaker
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '刷新凭证编号失败'))
  }
}
function selectRow(index: number) {
  selectedRowIndex.value = Math.max(0, Math.min(index, form.entries.length - 1))
}

function insertEntryAfter(index: number) {
  if (isReadonlyMode.value) return
  const currency = voucherMeta.value?.defaultCurrency || 'CNY'
  form.entries.splice(index + 1, 0, createEntry(currency, index + 2))
  form.entries = ensureMinimumRows(form.entries, currency, Math.max(form.entries.length, MIN_ENTRY_ROWS))
  selectRow(index + 1)
}

function removeSelectedEntry() {
  if (isReadonlyMode.value) return
  if (effectiveRows.value.length <= 2 && !isEntryBlank(selectedRow.value)) {
    ElMessage.warning('至少保留两条有效分录')
    return
  }
  form.entries.splice(selectedRowIndex.value, 1)
  form.entries = ensureMinimumRows(form.entries, voucherMeta.value?.defaultCurrency || 'CNY', Math.max(form.entries.length, 2))
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

  editingExisting.value = false
  voucherDetail.value = null
  clearDraft()
  validationErrors.value = []

  if (isDetailRoute.value) {
    await router.push({ name: 'finance-new-voucher' })
    return
  }

  if (voucherMeta.value) {
    resetFormFromMeta(voucherMeta.value, financeCompany.currentCompanyId)
    markCommitted()
  } else {
    await loadMeta(financeCompany.currentCompanyId)
  }
}

function enterEditMode() {
  if (!voucherDetail.value?.editable) {
    ElMessage.warning('当前凭证状态不允许修改')
    return
  }
  if (!canEditExisting.value) {
    ElMessage.warning('当前账号没有修改凭证权限')
    return
  }
  editingExisting.value = true
}

async function handleSave() {
  if (!validateVoucher(true)) return

  saving.value = true
  try {
    if (isDetailRoute.value && detailVoucherNo.value) {
      const res = await financeApi.updateVoucher(financeCompany.currentCompanyId || form.companyId, detailVoucherNo.value, buildPayload())
      ElMessage.success(`凭证修改成功：${res.data.voucherNo}`)
      await loadDetail(financeCompany.currentCompanyId || form.companyId, detailVoucherNo.value)
      return
    }

    const currentContext = { companyId: financeCompany.currentCompanyId || form.companyId, billDate: form.dbillDate, csign: form.csign }
    const res = await financeApi.createVoucher(buildPayload())
    clearDraft()
    ElMessage.success(`凭证保存成功：${res.data.voucherNo}`)
    const nextMeta = await financeApi.getVoucherMeta(currentContext)
    voucherMeta.value = nextMeta.data
    resetFormFromMeta(nextMeta.data, currentContext.companyId)
    validationErrors.value = []
    markCommitted()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, isDetailRoute.value ? '修改凭证失败' : '保存凭证失败'))
  } finally {
    saving.value = false
  }
}

function handleToolbarAction(action: ToolbarActionKey) {
  if (action === 'new') return void handleNewVoucher()
  if (action === 'modify') return void enterEditMode()
  if (action === 'insert') return insertEntryAfter(selectedRowIndex.value)
  if (action === 'delete') return removeSelectedEntry()
  if (action === 'save') return void handleSave()

  const descriptions: Record<Exclude<ToolbarActionKey, 'new' | 'modify' | 'insert' | 'delete' | 'save'>, string> = {
    print: '后续可接入正式打印模板与套打配置。',
    export: '后续可扩展为 Excel、PDF 或外部接口输出。',
    copy: '后续可按原凭证复制摘要、科目和金额。',
    reverse: '后续可接入红字冲销与反向凭证生成流程。',
    void: '后续可接入作废状态流转和权限校验。',
    searchReplace: '后续可在分录摘要、科目和辅助项中做批量查找替换。',
    cashFlow: '后续可关联现金流量项目并形成补录面板。',
    assist: '当前下方辅助核算区域已可录入基础信息，后续可扩展为侧边明细抽屉。',
    balance: '后续可联动余额查询与科目实时余额提示。',
    calculator: '后续可接入悬浮计算器或公式辅助输入能力。'
  }

  actionDialog.title = toolbarGroups.value.flatMap((group) => group.actions).find((item) => item.key === action)?.label || '提示'
  actionDialog.description = descriptions[action]
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
  if (!isReadonlyMode.value && event.key === 'Insert') {
    event.preventDefault()
    insertEntryAfter(index)
  }
}

function handleAmountKeydown(event: KeyboardEvent, index: number, field: 'md' | 'mc') {
  handleGridKeydown(event, index)
  if (isReadonlyMode.value) return
  const row = form.entries[index]
  if (!row) return
  if (field === 'md' && row.md) {
    row.mc = ''
    row.ncS = undefined
  }
  if (field === 'mc' && row.mc) {
    row.md = ''
    row.ndS = undefined
  }
}

function resolveAccountLabel(code?: string) {
  if (!code) return '当前行'
  return voucherMeta.value?.accountOptions.find((item) => item.value === code)?.label || code
}

function resolveCompanyName(companyId?: string) {
  if (!companyId) return '未设置'
  return voucherMeta.value?.companyOptions.find((item) => item.value === companyId)?.label || companyId
}

function buildDraftStorageKey(companyId = financeCompany.currentCompanyId) {
  return `${DRAFT_STORAGE_KEY}:${companyId || 'default'}`
}

function buildSnapshot() {
  return JSON.stringify(buildPayload(true))
}

function markCommitted() {
  lastCommittedSnapshot.value = buildSnapshot()
}

async function confirmCompanySwitch() {
  if (!hasUnsavedChanges.value) {
    return true
  }
  try {
    await ElMessageBox.confirm('切换公司后将丢弃当前凭证未保存内容，并按新公司重新加载，是否继续？', '切换公司', {
      type: 'warning',
      confirmButtonText: '继续切换',
      cancelButtonText: '取消'
    })
    if (isDetailRoute.value) {
      editingExisting.value = false
      await router.replace({ name: 'finance-query-voucher' })
    }
    return true
  } catch {
    return false
  }
}

function parseVoucherCompanyId(voucherNo: string) {
  const parts = String(voucherNo || '').split('~')
  return parts.length === 4 ? parts[0] : ''
}

function activateView() {
  if (viewActive.value) return
  viewActive.value = true
  registerCompanySwitchGuard()
  void initializePage()
}

function deactivateView() {
  if (!viewActive.value && !guardRegistered) return
  viewActive.value = false
  loading.value = false
  initializing.value = false
  invalidatePendingLoads()
  unregisterCompanySwitchGuard()
}

function beginLoad() {
  loadSequence += 1
  return loadSequence
}

function invalidatePendingLoads() {
  loadSequence += 1
}

function isLiveLoad(loadId: number) {
  return viewActive.value && loadId === loadSequence
}

function registerCompanySwitchGuard() {
  if (guardRegistered) return
  financeCompany.registerSwitchGuard(COMPANY_SWITCH_GUARD_KEY, confirmCompanySwitch)
  guardRegistered = true
}

function unregisterCompanySwitchGuard() {
  if (!guardRegistered) return
  financeCompany.unregisterSwitchGuard(COMPANY_SWITCH_GUARD_KEY)
  guardRegistered = false
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function moneyText(value: string) {
  return formatMoney(value)
}
</script>
<style scoped>
.voucher-page { height: 100%; display: flex; min-height: 0; flex-direction: column; gap: 12px; overflow: hidden; }
.voucher-content-scroll { min-height: 0; flex: 1; overflow: auto; padding-bottom: 8px; }
.voucher-shell { display: flex; min-height: 100%; flex-direction: column; gap: 16px; border-radius: 28px; background: radial-gradient(circle at top right, rgba(96,165,250,.1), transparent 28%), linear-gradient(180deg, #f8fbff 0%, #f3f6fb 100%); padding: 18px; }
.voucher-page-header { display: flex; justify-content: center; }
.voucher-page-header h1 { font-size: 28px; font-weight: 700; color: #1e3a5f; letter-spacing: .28em; }
.voucher-toolbar-panel { position: sticky; top: 0; z-index: 20; display: flex; flex-wrap: wrap; gap: 14px; border-bottom: 1px solid rgba(216,226,240,.9); border-radius: 22px; background: rgba(255,255,255,.92); padding: 14px 16px; backdrop-filter: blur(10px); box-shadow: 0 16px 30px rgba(15,23,42,.08); }
.toolbar-group { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.toolbar-group + .toolbar-group { position: relative; padding-left: 16px; }
.toolbar-group + .toolbar-group::before { position: absolute; left: 0; top: 6px; height: 36px; width: 1px; background: linear-gradient(180deg, transparent 0%, #d5deea 22%, #d5deea 78%, transparent 100%); content: ''; }
.toolbar-button { height: 40px; min-width: 96px; border-radius: 14px; border-color: #d6e0ec; background: #fff; color: #365070; font-weight: 600; }
.toolbar-button-large { height: 52px; min-width: 130px; padding: 0 20px; font-size: 15px; }
.toolbar-button-accent { border-color: #9cbbe3; background: linear-gradient(180deg, #f0f7ff 0%, #e4efff 100%); color: #24528a; box-shadow: 0 12px 24px rgba(59,130,246,.14); }
.toolbar-button-primary { box-shadow: 0 16px 30px rgba(37,99,235,.2); }
.voucher-info-band, .voucher-lower { display: grid; grid-template-columns: minmax(0,1fr) 260px; gap: 16px; }
.voucher-lower-full { grid-template-columns: minmax(0,1fr); }
.voucher-info-main, .voucher-info-side, .voucher-ledger-card, .voucher-assist-card, .voucher-side-card { border-radius: 24px; border: 1px solid #d8e2f0; background: rgba(255,255,255,.94); box-shadow: 0 12px 28px rgba(15,23,42,.04); padding: 18px; }
.voucher-info-grid, .assist-grid { display: grid; grid-template-columns: repeat(12, minmax(0,1fr)); gap: 12px 14px; }
.voucher-info-field, .assist-field { display: flex; flex-direction: column; gap: 8px; grid-column: span 3; }
.voucher-info-field span, .assist-field span { font-size: 12px; font-weight: 600; color: #5f7391; }
.voucher-info-code, .voucher-info-period { grid-column: span 2; }
.voucher-info-field-note { grid-column: span 6; }
.voucher-code-box { display: flex; height: 40px; align-items: center; justify-content: center; border-radius: 14px; border: 1px solid #cfe0f5; background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%); font-weight: 700; color: #24466f; }
.voucher-company-box { justify-content: flex-start; padding: 0 14px; font-weight: 600; }
.voucher-number-group { display: grid; grid-template-columns: minmax(88px,108px) auto minmax(0,1fr); align-items: center; gap: 8px; }
.voucher-number-separator { display: inline-flex; align-items: center; justify-content: center; color: #5f7391; font-weight: 700; }
.voucher-info-side { display: flex; flex-direction: column; gap: 12px; }
.status-chip { display: flex; flex-direction: column; gap: 4px; border-radius: 18px; border: 1px solid #d8e2f0; background: linear-gradient(180deg, #f8fbff 0%, #f2f6fc 100%); padding: 12px 14px; }
.status-chip span { font-size: 12px; color: #67809d; }
.status-chip strong { color: #1d3557; font-size: 16px; }
.status-chip-balanced { border-color: #bae6c7; background: linear-gradient(180deg, #f0fdf4 0%, #ecfdf3 100%); }
.status-chip-warning { border-color: #fde68a; background: linear-gradient(180deg, #fff9db 0%, #fff7c4 100%); }
.voucher-ledger-card { display: flex; min-height: 0; flex-direction: column; }
.voucher-ledger-header, .voucher-section-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.voucher-ledger-title, .voucher-section-head h2 { font-size: 18px; font-weight: 700; color: #1d3557; }
.voucher-section-head p, .voucher-ledger-summary { color: #5d7491; font-size: 13px; }
.voucher-ledger-summary { display: flex; flex-wrap: wrap; gap: 14px; }
.voucher-grid { margin-top: 14px; display: flex; min-height: 0; flex: 1; flex-direction: column; overflow: hidden; border-radius: 20px; border: 1px solid #d7e0eb; background: #fdfefe; }
.voucher-grid-layout { display: grid; grid-template-columns: minmax(220px,1.2fr) minmax(280px,1.4fr) minmax(160px,.8fr) minmax(160px,.8fr); }
.voucher-grid-header, .voucher-grid-footer { flex-shrink: 0; background: linear-gradient(180deg, #f3f7fd 0%, #edf3fb 100%); color: #49627f; font-size: 13px; font-weight: 700; }
.voucher-grid-header > div, .voucher-grid-footer > div { padding: 10px 14px; }
.voucher-grid-body { min-height: 0; flex: 1; overflow: auto; background: linear-gradient(180deg, rgba(248,251,255,.56) 0%, rgba(255,255,255,.92) 100%); }
.voucher-grid-row { min-height: 58px; border-top: 1px solid #e4ebf4; transition: background-color .16s ease, box-shadow .16s ease; }
.voucher-grid-row:hover { background: rgba(239,246,255,.72); }
.voucher-grid-row-active { background: rgba(219,234,254,.5); box-shadow: inset 4px 0 0 #4f8ad8; }
.voucher-grid-row-readonly:hover { background: rgba(219,234,254,.5); }
.voucher-grid-row:focus { outline: none; }
.voucher-cell { display: flex; flex-direction: column; justify-content: center; gap: 4px; padding: 7px 12px; }
.voucher-cell-digest { padding-right: 6px; }
.voucher-inline-field { display: flex; align-items: center; gap: 10px; }
.voucher-row-index { display: inline-flex; min-width: 22px; align-items: center; justify-content: center; color: #788ca6; font-size: 12px; font-weight: 700; }
.voucher-footer-amount { text-align: right; color: #173a61; font-family: Consolas, Monaco, monospace; }
.voucher-current-row { border-radius: 999px; background: #edf4ff; padding: 6px 11px; color: #31598d; font-size: 12px; font-weight: 700; }
.voucher-signature { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 12px; border-radius: 18px; border: 1px solid #d8e2f0; background: rgba(255,255,255,.92); padding: 14px 16px; color: #4a627f; font-size: 13px; }
.action-dialog-content { color: #506680; line-height: 1.8; }
.action-dialog-subtle { margin-top: 8px; color: #8a9bb1; font-size: 12px; }
:deep(.voucher-info-field .el-input__wrapper), :deep(.voucher-info-field .el-select__wrapper), :deep(.voucher-info-field .el-date-editor), :deep(.assist-field .el-input__wrapper), :deep(.assist-field .el-select__wrapper), :deep(.voucher-cell .el-input__wrapper), :deep(.voucher-cell .el-select__wrapper) { border-radius: 12px; box-shadow: 0 0 0 1px #d8e2f0 inset; }
:deep(.voucher-cell .el-input-number), :deep(.voucher-cell .el-input-number .el-input__wrapper), :deep(.assist-field .el-input-number), :deep(.assist-field .el-input-number .el-input__wrapper), :deep(.voucher-info-field .el-input-number), :deep(.voucher-info-field .el-input-number .el-input__wrapper) { width: 100%; }
:deep(.voucher-number-group .el-select__wrapper), :deep(.voucher-number-group .el-input__wrapper) { min-height: 40px; }
:deep(.voucher-cell .el-input__wrapper), :deep(.voucher-cell .el-select__wrapper), :deep(.voucher-cell .money-input__control) { min-height: 38px; }
@media (max-width: 1440px) { .voucher-info-band, .voucher-lower { grid-template-columns: 1fr; } }
@media (max-width: 1024px) { .voucher-info-grid, .assist-grid { grid-template-columns: repeat(6, minmax(0,1fr)); } .voucher-info-field, .assist-field, .voucher-info-field-note { grid-column: span 3; } .voucher-grid-layout { min-width: 860px; } }
@media (max-width: 768px) {
  .voucher-page-header h1 { font-size: 24px; letter-spacing: .18em; }
  .toolbar-group { width: 100%; }
  .toolbar-group + .toolbar-group { padding-left: 0; padding-top: 10px; }
  .toolbar-group + .toolbar-group::before { left: 0; top: 0; height: 1px; width: 100%; }
  .voucher-info-grid, .assist-grid { grid-template-columns: repeat(2, minmax(0,1fr)); }
  .voucher-info-field, .assist-field, .voucher-info-code, .voucher-info-period, .voucher-info-field-note { grid-column: span 2; }
  .voucher-signature { flex-direction: column; align-items: flex-start; }
}
</style>
