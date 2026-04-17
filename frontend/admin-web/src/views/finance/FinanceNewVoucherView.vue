<template>
  <div class="voucher-page">
    <section class="voucher-toolbar-panel">
      <div v-for="group in toolbarGroups" :key="group.key" class="toolbar-group">
        <el-button
          v-for="action in group.actions"
          :key="action.key"
          :type="action.emphasis === 'primary' ? 'primary' : action.emphasis === 'secondary' ? 'info' : 'default'"
          :plain="action.emphasis === 'secondary'"
          :disabled="Boolean(action.disabled)"
          :loading="action.key === 'save' ? saving : action.key === currentToolbarLoadingKey ? reviewActing : false"
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
              <label class="voucher-info-field voucher-info-company">
                <span class="voucher-field-label">公司</span>
                <div class="voucher-code-box voucher-company-box">{{ currentCompanyName }}</div>
              </label>
              <label class="voucher-info-field voucher-info-code">
                <span class="voucher-field-label">凭证编号</span>
                <div class="voucher-number-group">
                  <el-select v-model="form.csign" placeholder="类别" :disabled="voucherHeaderLocked">
                    <el-option v-for="item in voucherMeta?.voucherTypeOptions || []" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                  <span class="voucher-number-separator">-</span>
                  <el-input v-model="voucherNoInput" placeholder="请输入凭证号" :readonly="voucherHeaderLocked" />
                </div>
              </label>
              <label class="voucher-info-field voucher-info-date">
                <span class="voucher-field-label">制单日期</span>
                <el-date-picker v-model="form.dbillDate" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" :disabled="isReadonlyMode" />
              </label>
              <label class="voucher-info-field voucher-info-period">
                <span class="voucher-field-label">期间</span>
                <el-input-number v-model="form.iperiod" :min="1" :max="12" :controls="false" :disabled="voucherHeaderLocked" />
              </label>
              <label class="voucher-info-field voucher-info-maker">
                <span class="voucher-field-label">制单人</span>
                <el-input v-model="form.cbill" readonly />
              </label>
              <label class="voucher-info-field voucher-info-docs">
                <span class="voucher-field-label">附件张数</span>
                <el-input-number v-model="form.idoc" :min="0" :controls="false" :disabled="isReadonlyMode" />
              </label>
              <label class="voucher-info-field voucher-info-field-note">
                <span class="voucher-field-label">备注</span>
                <el-input v-model="remarkText" placeholder="请输入备注" :readonly="isReadonlyMode" />
              </label>
              <div class="voucher-info-spacer" aria-hidden="true"></div>
            </div>
          </div>
        </section>

        <section v-if="voucherNoticeItems.length" class="voucher-notice-panel">
          <div
            v-for="notice in voucherNoticeItems"
            :key="notice.text"
            class="voucher-notice-item"
            :class="`voucher-notice-item-${notice.level}`"
          >
            {{ notice.text }}
          </div>
        </section>

        <section class="voucher-ledger-card">
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
                    <el-input
                      v-model="row.cdigest"
                      placeholder="请输入摘要"
                      :readonly="isReadonlyMode"
                      :maxlength="255"
                      @focus="handleEntryFieldFocus(index)"
                    />
                  </div>
                </div>
                <div class="voucher-cell">
                  <el-select
                    v-model="row.ccode"
                    filterable
                    clearable
                    placeholder="请选择科目"
                    :disabled="isReadonlyMode"
                    :data-subject-row-id="row.localId"
                    @focus="handleSubjectFieldFocus(index)"
                    @change="handleSubjectChange(index, $event)"
                    @visible-change="handleSubjectDropdownVisibleChange(index, $event)"
                  >
                    <el-option v-for="item in accountOptionsForDisplay" :key="item.value" :label="formatVoucherOptionLabel(item)" :value="item.value" />
                  </el-select>
                </div>
                <div class="voucher-cell">
                  <money-input v-model="row.md" placeholder="0.00" :readonly="isReadonlyMode" :disabled="isReadonlyMode" @focus="handleEntryFieldFocus(index)" @keydown="handleAmountKeydown($event, index, 'md')" />
                </div>
                <div class="voucher-cell">
                  <money-input v-model="row.mc" placeholder="0.00" :readonly="isReadonlyMode" :disabled="isReadonlyMode" @focus="handleEntryFieldFocus(index)" @keydown="handleAmountKeydown($event, index, 'mc')" />
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
            <div class="assist-grid">
              <label class="assist-field">
                <span class="voucher-field-label">部门</span>
                <el-tree-select
                  v-model="selectedRow.cdeptId"
                  :data="departmentTreeOptions"
                  node-key="value"
                  check-strictly
                  filterable
                  clearable
                  placeholder="请选择部门"
                  :disabled="assistDisabledState.department"
                  :props="{ label: 'label', children: 'children', value: 'value' }"
                  :filter-node-method="filterDepartmentTreeNode"
                  @focus="handleAssistFieldFocus"
                />
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">人员</span>
                <el-select v-model="selectedRow.cpersonId" filterable clearable placeholder="请选择人员" :disabled="assistDisabledState.employee" @focus="handleAssistFieldFocus">
                  <el-option v-for="item in voucherMeta?.employeeOptions || []" :key="item.value" :label="formatVoucherOptionLabel(item)" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">客户</span>
                <el-select v-model="selectedRow.ccusId" filterable clearable placeholder="请选择客户" :disabled="assistDisabledState.customer" @focus="handleAssistFieldFocus">
                  <el-option v-for="item in voucherMeta?.customerOptions || []" :key="item.value" :label="formatVoucherOptionLabel(item)" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">供应商</span>
                <el-select v-model="selectedRow.csupId" filterable clearable placeholder="请选择供应商" :disabled="assistDisabledState.supplier" @focus="handleAssistFieldFocus">
                  <el-option v-for="item in voucherMeta?.supplierOptions || []" :key="item.value" :label="formatVoucherOptionLabel(item)" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">项目分类</span>
                <el-select v-model="selectedRow.citemClass" filterable clearable placeholder="请选择项目分类" :disabled="assistDisabledState.projectClass" @focus="handleAssistFieldFocus">
                  <el-option v-for="item in projectClassOptionsForDisplay" :key="item.value" :label="formatVoucherOptionLabel(item)" :value="item.value" />
                </el-select>
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">项目</span>
                <el-select v-model="selectedRow.citemId" filterable clearable placeholder="请选择项目" :disabled="assistDisabledState.project" @focus="handleAssistFieldFocus">
                  <el-option v-for="item in filteredProjectOptions" :key="item.value" :label="formatVoucherOptionLabel(item)" :value="item.value" />
                </el-select>
              </label>
            </div>
          </div>
        </section>

        <footer class="voucher-signature">
          <span>审核：{{ voucherDetail?.checkerName || '未审核' }}</span>
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
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
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
  type FinanceVoucherOption,
  type FinanceVoucherSavePayload
} from '@/api'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { showBusinessWarning } from '@/utils/businessWarning'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { absMoney, addMoney, formatMoney, isZeroMoney, normalizeMoneyValue } from '@/utils/money'

type ToolbarActionKey = 'new' | 'modify' | 'print' | 'export' | 'copy' | 'reverse' | 'void' | 'insert' | 'delete' | 'searchReplace' | 'cashFlow' | 'save' | 'assist' | 'balance' | 'calculator' | 'review' | 'unreview' | 'markError' | 'find'
type VoucherEntryRow = FinanceVoucherEntry & { localId: string }
type DepartmentTreeOption = FinanceVoucherOption & { children: DepartmentTreeOption[] }
type VoucherFormState = Omit<FinanceVoucherForm, 'entries'> & { entries: VoucherEntryRow[] }
type VoucherPageMode = 'create' | 'detail' | 'review'
type VoucherAssistCapability = {
  department: boolean
  employee: boolean
  customer: boolean
  supplier: boolean
  project: boolean
  lockedProjectClassCode?: string
}
type LeafSubjectSnapshot = {
  code: string
  name?: string
}

interface ToolbarAction {
  key: ToolbarActionKey
  label: string
  icon: Component
  emphasis?: 'primary' | 'secondary'
  disabled?: boolean
}

const DRAFT_STORAGE_KEY = 'finance-new-voucher-draft'
const MIN_ENTRY_ROWS = 8
const COMPANY_SWITCH_GUARD_KEY = 'finance-new-voucher'
const ENTRY_FIELD_MAX_LENGTH: Record<'cdigest' | 'ccode' | 'cdeptId' | 'cpersonId' | 'ccusId' | 'csupId' | 'citemClass' | 'citemId' | 'cexchName', number> = {
  cdigest: 255,
  ccode: 64,
  cdeptId: 64,
  cpersonId: 64,
  ccusId: 64,
  csupId: 64,
  citemClass: 2,
  citemId: 6,
  cexchName: 32
}
const ENTRY_FIELD_LABELS: Record<keyof typeof ENTRY_FIELD_MAX_LENGTH, string> = {
  cdigest: '摘要',
  ccode: '科目',
  cdeptId: '部门',
  cpersonId: '人员',
  ccusId: '客户',
  csupId: '供应商',
  citemClass: '项目分类',
  citemId: '项目',
  cexchName: '币种名称'
}

const props = withDefaults(defineProps<{ pageMode?: VoucherPageMode; voucherNo?: string }>(), {
  pageMode: 'create',
  voucherNo: ''
})
const router = useRouter()
const financeCompany = useFinanceCompanyStore()
const currentUser = readStoredUser()

const loading = ref(false)
const saving = ref(false)
const reviewActing = ref(false)
const currentToolbarLoadingKey = ref<ToolbarActionKey | ''>('')
const initializing = ref(false)
const voucherMeta = ref<FinanceVoucherMeta | null>(null)
const voucherDetail = ref<FinanceVoucherDetail | null>(null)
const validationErrors = ref<string[]>([])
const hasDraft = ref(false)
const selectedRowIndex = ref(0)
const editingExisting = ref(false)
const lastCommittedSnapshot = ref('')
const actionDialog = reactive({ visible: false, title: '', description: '' })
const lastValidLeafSubjectByRow = reactive<Record<string, LeafSubjectSnapshot | undefined>>({})
const leafSubjectWarningVisible = ref(false)
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
const isReviewMode = computed(() => props.pageMode === 'review')
const detailVoucherNo = computed(() => String(props.voucherNo || ''))
const canEditExisting = computed(() => hasPermission('finance:general_ledger:query_voucher:edit', currentUser))
const canReviewVoucher = computed(() => hasPermission('finance:general_ledger:review_voucher:review', currentUser))
const canUnreviewVoucher = computed(() => hasPermission('finance:general_ledger:review_voucher:unreview', currentUser))
const canMarkVoucherError = computed(() => hasPermission('finance:general_ledger:review_voucher:mark_error', currentUser))
const isReadonlyMode = computed(() => isReviewMode.value || (isDetailRoute.value && !editingExisting.value))
const voucherHeaderLocked = computed(() => isReviewMode.value || isDetailRoute.value)
const backToListRouteName = computed(() => (isReviewMode.value ? 'finance-review-voucher' : 'finance-query-voucher'))
const pageTitle = computed(() => {
  if (isReviewMode.value) return '审核凭证'
  if (!isDetailRoute.value) return '新建凭证'
  return editingExisting.value ? '修改凭证' : '凭证详情'
})
const toolbarGroups = computed<Array<{ key: string; actions: ToolbarAction[] }>>(() => {
  if (isReviewMode.value) {
    return [
      {
        key: 'review-primary',
        actions: [
          { key: 'review', label: '审核', icon: Select, emphasis: 'secondary', disabled: !canReviewVoucher.value }
        ]
      },
      {
        key: 'review-actions',
        actions: [
          { key: 'export', label: '导出', icon: Download },
          { key: 'find', label: '查找', icon: Search },
          { key: 'unreview', label: '反审核', icon: RefreshLeft, disabled: !canUnreviewVoucher.value },
          {
            key: 'markError',
            label: voucherDetail.value?.status === 'ERROR' ? '取消错误' : '标记错误',
            icon: CircleClose,
            disabled: !canMarkVoucherError.value
          }
        ]
      }
    ]
  }

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
const totalDebit = computed(() => sumRows(effectiveRows.value, 'md'))
const totalCredit = computed(() => sumRows(effectiveRows.value, 'mc'))
const balanceGap = computed(() => subtractVoucherAmount(totalDebit.value, totalCredit.value))
const selectedRow = computed(() => form.entries[Math.min(selectedRowIndex.value, Math.max(form.entries.length - 1, 0))] as VoucherEntryRow)
const financeCompanyState = financeCompany as typeof financeCompany & {
  currentCompanyLabel?: string
  currentCompanyHasActiveAccountSet?: boolean
}
const currentCompanyOption = computed(() =>
  financeCompany.companyOptions?.find((item) => item.companyId === (financeCompany.currentCompanyId || form.companyId))
)
const currentRowLabel = computed(() => {
  if (!selectedRow.value?.ccode) return ''
  return resolveAccountLabel(selectedRow.value.ccode, selectedRow.value.ccodeName)
})
const currentCompanyName = computed(() => financeCompany.currentCompanyName || currentCompanyOption.value?.companyName || resolveCompanyName(form.companyId))
const currentCompanyHasActiveAccountSet = computed(() => {
  if (typeof financeCompanyState.currentCompanyHasActiveAccountSet === 'boolean') {
    return financeCompanyState.currentCompanyHasActiveAccountSet
  }
  if (typeof currentCompanyOption.value?.hasActiveAccountSet === 'boolean') {
    return currentCompanyOption.value.hasActiveAccountSet
  }
  return Boolean(financeCompany.currentCompanyId || form.companyId)
})
const hasUnsavedChanges = computed(() => Boolean(voucherMeta.value) && buildSnapshot() !== lastCommittedSnapshot.value)
const accountOptionMap = computed(() => new Map((voucherMeta.value?.accountOptions || []).map((item) => [item.value, item] as const)))
const selectedAccountOption = computed(() => {
  const code = selectedRow.value?.ccode
  return code ? accountOptionMap.value.get(code) : undefined
})
const currentAssistCapability = computed(() => resolveAssistCapability(selectedAccountOption.value))
const assistDisabledState = computed(() => ({
  department: isReadonlyMode.value || !currentAssistCapability.value.department,
  employee: isReadonlyMode.value || !currentAssistCapability.value.employee,
  customer: isReadonlyMode.value || !currentAssistCapability.value.customer,
  supplier: isReadonlyMode.value || !currentAssistCapability.value.supplier,
  projectClass:
    isReadonlyMode.value || !currentAssistCapability.value.project || Boolean(currentAssistCapability.value.lockedProjectClassCode),
  project: isReadonlyMode.value || !currentAssistCapability.value.project
}))
const accountOptionsForDisplay = computed(() => {
  const options = [...(voucherMeta.value?.accountOptions || [])]
  const existingValues = new Set(options.map((item) => item.value))
  form.entries.forEach((row) => {
    if (!row.ccode || existingValues.has(row.ccode)) return
    options.push({
      value: row.ccode,
      code: row.ccode,
      name: row.ccodeName,
      label: row.ccodeName ? `${row.ccode}  ${row.ccodeName}` : row.ccode
    })
    existingValues.add(row.ccode)
  })
  return options
})
const projectClassOptionsForDisplay = computed(() =>
  appendDisplayOption(voucherMeta.value?.projectClassOptions || [], currentAssistCapability.value.lockedProjectClassCode || selectedRow.value?.citemClass)
)
const departmentTreeOptions = computed(() => buildDepartmentTreeOptions(voucherMeta.value?.departmentOptions || []))
const filteredProjectOptions = computed(() => {
  const projectClassCode = currentAssistCapability.value.lockedProjectClassCode || selectedRow.value?.citemClass
  const options = voucherMeta.value?.projectOptions || []
  const filtered = !projectClassCode ? options : options.filter((item) => item.parentValue === projectClassCode)
  return appendDisplayOption(filtered, selectedRow.value?.citemId)
})
const voucherNoticeItems = computed<Array<{ level: 'warning' | 'danger' | 'info'; text: string }>>(() => {
  const notices: Array<{ level: 'warning' | 'danger' | 'info'; text: string }> = []
  if (!financeCompany.currentCompanyId) {
    notices.push({ level: 'warning', text: '当前未选择财务公司，请先选择公司后再录入凭证。' })
    return notices
  }
  if (!currentCompanyHasActiveAccountSet.value) {
    notices.push({ level: 'warning', text: '当前公司未创建账套，请切换公司或先建账。' })
    return notices
  }
  if (!voucherMeta.value) {
    return notices
  }
  if (!voucherMeta.value.accountOptions?.length) {
    notices.push({ level: 'danger', text: '当前公司账套已启用，但暂无会计科目数据，请检查账套初始化结果。' })
  }
  if (!voucherMeta.value.customerOptions?.length) {
    notices.push({ level: 'info', text: '当前公司暂无客户档案数据。' })
  }
  if (!voucherMeta.value.supplierOptions?.length) {
    notices.push({ level: 'info', text: '当前公司暂无供应商档案数据。' })
  }
  if (!voucherMeta.value.projectClassOptions?.length || !voucherMeta.value.projectOptions?.length) {
    notices.push({ level: 'info', text: '当前公司暂无项目档案数据。' })
  }
  const lockedProjectClassCode = currentAssistCapability.value.lockedProjectClassCode
  if (lockedProjectClassCode && !(voucherMeta.value.projectClassOptions || []).some((item) => item.value === lockedProjectClassCode)) {
    notices.push({ level: 'warning', text: `当前科目挂载的项目分类【${lockedProjectClassCode}】不存在或当前不可用，请先维护项目档案。` })
  }
  return notices
})
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

watch(
  () =>
    [
      selectedRowIndex.value,
      selectedRow.value?.ccode,
      selectedRow.value?.citemClass,
      selectedRow.value?.citemId,
      voucherMeta.value?.accountOptions,
      voucherMeta.value?.projectOptions,
      isReadonlyMode.value
    ] as const,
  () => {
    syncSelectedRowAssistState()
  }
)

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

  if (isDetailRoute.value || isReviewMode.value) {
    const voucherCompanyId = parseVoucherCompanyId(detailVoucherNo.value)
    if (voucherCompanyId && voucherCompanyId !== companyId) {
      if (!isLiveLoad(loadId)) return
      editingExisting.value = false
      voucherDetail.value = null
      await router.replace({ name: backToListRouteName.value })
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
  resetLeafSubjectHistory(form.entries, meta.accountOptions)
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
  resetLeafSubjectHistory(form.entries, meta.accountOptions)
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
  resetLeafSubjectHistory(form.entries, meta.accountOptions)
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

function buildOptionValueSet(options?: FinanceVoucherOption[]) {
  return new Set((options || []).map((item) => item.value).filter((value): value is string => Boolean(value)))
}

function normalizeText(value?: string | null) {
  const text = String(value || '').trim()
  return text || undefined
}

function isOptionEnabled(value?: number | null) {
  return Number(value || 0) === 1
}

function isLeafAccountOption(option?: FinanceVoucherOption | null) {
  return Number(option?.leafFlag || 0) === 1
}

function findAccountOptionByCode(code?: string | null, options = voucherMeta.value?.accountOptions || []) {
  const normalizedCode = normalizeText(code)
  return normalizedCode ? options.find((item) => item.value === normalizedCode) : undefined
}

function rememberLeafSubject(row: VoucherEntryRow, option?: FinanceVoucherOption | null) {
  if (!option || !isLeafAccountOption(option)) {
    delete lastValidLeafSubjectByRow[row.localId]
    return
  }
  lastValidLeafSubjectByRow[row.localId] = {
    code: option.value,
    name: option.name
  }
}

function clearAssistSelections(row: VoucherEntryRow) {
  row.cdeptId = ''
  row.cpersonId = ''
  row.ccusId = ''
  row.csupId = ''
  row.citemClass = ''
  row.citemId = ''
}

function syncRowAccountState(row: VoucherEntryRow, options = voucherMeta.value?.accountOptions || []) {
  const option = findAccountOptionByCode(row.ccode, options)
  if (option?.name) {
    row.ccodeName = option.name
  } else if (!row.ccode) {
    row.ccodeName = ''
  }
  if (!row.ccode) {
    delete lastValidLeafSubjectByRow[row.localId]
    return
  }
  if (isLeafAccountOption(option)) {
    rememberLeafSubject(row, option)
    return
  }
  delete lastValidLeafSubjectByRow[row.localId]
}

function resetLeafSubjectHistory(entries: VoucherEntryRow[], options = voucherMeta.value?.accountOptions || []) {
  Object.keys(lastValidLeafSubjectByRow).forEach((key) => {
    delete lastValidLeafSubjectByRow[key]
  })
  entries.forEach((row) => {
    syncRowAccountState(row, options)
  })
}

function focusSubjectField(row: VoucherEntryRow) {
  if (typeof document === 'undefined') return
  const selector = `[data-subject-row-id="${row.localId}"]`
  const host = document.querySelector(selector)
  if (!(host instanceof HTMLElement)) return
  const focusTarget = host.matches('input,select,[tabindex]') ? host : host.querySelector<HTMLElement>('input,select,[tabindex],.el-select__wrapper')
  focusTarget?.focus?.()
}

async function restoreInvalidLeafSubject(row: VoucherEntryRow, rowIndex = selectedRowIndex.value) {
  if (leafSubjectWarningVisible.value) {
    return false
  }
  const invalidLabel = resolveAccountLabel(row.ccode, row.ccodeName)
  const previousLeafSubject = lastValidLeafSubjectByRow[row.localId]
  leafSubjectWarningVisible.value = true
  try {
    await showBusinessWarning({
      title: '科目不可录入',
      message: `当前科目【${invalidLabel}】不是末级科目，不允许录入凭证，请重新选择末级科目。`,
      confirmButtonText: '返回科目'
    })
  } finally {
    leafSubjectWarningVisible.value = false
  }

  if (previousLeafSubject) {
    row.ccode = previousLeafSubject.code
    row.ccodeName = previousLeafSubject.name || findAccountOptionByCode(previousLeafSubject.code)?.name || ''
  } else {
    row.ccode = ''
    row.ccodeName = ''
  }
  clearAssistSelections(row)
  clearDisabledAssistFields(row, resolveAssistCapability(findAccountOptionByCode(row.ccode)))
  selectedRowIndex.value = Math.max(0, Math.min(rowIndex, form.entries.length - 1))
  await nextTick()
  focusSubjectField(row)
  return false
}

async function ensureSelectedRowUsesLeafSubject() {
  if (isReadonlyMode.value) return true
  const row = selectedRow.value
  if (!row?.ccode) return true
  const option = findAccountOptionByCode(row.ccode)
  if (!option || isLeafAccountOption(option)) {
    return true
  }
  return restoreInvalidLeafSubject(row, selectedRowIndex.value)
}

async function tryLeaveSubjectField(nextRowIndex = selectedRowIndex.value) {
  const canLeave = await ensureSelectedRowUsesLeafSubject()
  if (canLeave) {
    selectRow(nextRowIndex)
  }
  return canLeave
}

function resolveAssistCapability(option?: FinanceVoucherOption | null): VoucherAssistCapability {
  return {
    department: isOptionEnabled(option?.bdept),
    employee: isOptionEnabled(option?.bperson),
    customer: isOptionEnabled(option?.bcus),
    supplier: isOptionEnabled(option?.bsup),
    project: isOptionEnabled(option?.bitem),
    lockedProjectClassCode: normalizeText(option?.cassItem)
  }
}

function appendDisplayOption(options: FinanceVoucherOption[], value?: string, name?: string) {
  const normalizedValue = normalizeText(value)
  if (!normalizedValue || options.some((item) => item.value === normalizedValue)) {
    return options
  }
  return [
    ...options,
    {
      value: normalizedValue,
      code: normalizedValue,
      name,
      label: name ? `${normalizedValue}  ${name}` : normalizedValue
    }
  ]
}

function clearDisabledAssistFields(row: VoucherEntryRow, capability: VoucherAssistCapability) {
  if (!capability.department) row.cdeptId = ''
  if (!capability.employee) row.cpersonId = ''
  if (!capability.customer) row.ccusId = ''
  if (!capability.supplier) row.csupId = ''
  if (!capability.project) {
    row.citemClass = ''
    row.citemId = ''
    return
  }
  if (capability.lockedProjectClassCode && row.citemClass !== capability.lockedProjectClassCode) {
    row.citemClass = capability.lockedProjectClassCode
  }
}

function syncSelectedRowAssistState() {
  if (isReadonlyMode.value) return
  const row = selectedRow.value
  if (!row) return
  const capability = resolveAssistCapability(accountOptionMap.value.get(row.ccode || ''))
  clearDisabledAssistFields(row, capability)
  if (!row.citemId) return
  const project = (voucherMeta.value?.projectOptions || []).find((item) => item.value === row.citemId)
  if (!project) {
    row.citemId = ''
    return
  }
  const projectClassCode = capability.lockedProjectClassCode || row.citemClass
  if (projectClassCode && project.parentValue && project.parentValue !== projectClassCode) {
    row.citemId = ''
  }
}

function buildDepartmentTreeOptions(options: FinanceVoucherOption[]) {
  const nodeMap = new Map<string, DepartmentTreeOption>()
  const roots: DepartmentTreeOption[] = []

  options.forEach((item) => {
    nodeMap.set(item.value, {
      ...item,
      label: formatVoucherOptionLabel(item),
      children: []
    })
  })

  options.forEach((item) => {
    const node = nodeMap.get(item.value)
    if (!node) return
    const parentValue = normalizeText(item.parentValue)
    if (parentValue && parentValue !== item.value) {
      const parentNode = nodeMap.get(parentValue)
      if (parentNode) {
        parentNode.children.push(node)
        return
      }
    }
    roots.push(node)
  })

  return roots
}

function validateEntryLength(row: VoucherEntryRow, rowNo: number, errors: string[]) {
  ;(Object.entries(ENTRY_FIELD_MAX_LENGTH) as Array<[keyof typeof ENTRY_FIELD_MAX_LENGTH, number]>).forEach(([fieldKey, maxLength]) => {
    const value = row[fieldKey]
    if (typeof value !== 'string') {
      return
    }
    const normalized = value.trim()
    if (normalized.length > maxLength) {
      errors.push(`第 ${rowNo} 行${ENTRY_FIELD_LABELS[fieldKey]}最多 ${maxLength} 个字符`)
    }
  })
}

function validateEntrySelection(row: VoucherEntryRow, rowNo: number, errors: string[]) {
  const meta = voucherMeta.value
  if (!meta) {
    return
  }

  const accountMap = new Map((meta.accountOptions || []).map((item) => [item.value, item] as const))
  const departmentValues = buildOptionValueSet(meta.departmentOptions)
  const employeeValues = buildOptionValueSet(meta.employeeOptions)
  const customerValues = buildOptionValueSet(meta.customerOptions)
  const supplierValues = buildOptionValueSet(meta.supplierOptions)
  const projectClassValues = buildOptionValueSet(meta.projectClassOptions)
  const projectMap = new Map((meta.projectOptions || []).map((item) => [item.value, item] as const))
  const account = row.ccode ? accountMap.get(row.ccode) : undefined
  const capability = resolveAssistCapability(account)

  if (row.ccode && !account) {
    errors.push(`第 ${rowNo} 行科目不存在或当前不可用`)
  }
  if (account && !isLeafAccountOption(account)) {
    errors.push(`第 ${rowNo} 行科目【${formatVoucherOptionLabel(account)}】不是末级科目，不允许录入凭证`)
  }
  if (row.cdeptId && !departmentValues.has(row.cdeptId)) {
    errors.push(`第 ${rowNo} 行部门不存在或当前不可用`)
  }
  if (row.cpersonId && !employeeValues.has(row.cpersonId)) {
    errors.push(`第 ${rowNo} 行人员不存在或当前不可用`)
  }
  if (row.ccusId && !customerValues.has(row.ccusId)) {
    errors.push(`第 ${rowNo} 行客户不存在或当前不可用`)
  }
  if (row.csupId && !supplierValues.has(row.csupId)) {
    errors.push(`第 ${rowNo} 行供应商不存在或当前不可用`)
  }
  if (row.cdeptId && !capability.department) {
    errors.push(`第 ${rowNo} 行当前科目未启用部门辅助核算`)
  }
  if (row.cpersonId && !capability.employee) {
    errors.push(`第 ${rowNo} 行当前科目未启用人员辅助核算`)
  }
  if (row.ccusId && !capability.customer) {
    errors.push(`第 ${rowNo} 行当前科目未启用客户辅助核算`)
  }
  if (row.csupId && !capability.supplier) {
    errors.push(`第 ${rowNo} 行当前科目未启用供应商辅助核算`)
  }
  if (row.citemClass && !projectClassValues.has(row.citemClass)) {
    errors.push(`第 ${rowNo} 行项目分类不存在或当前不可用`)
  }
  if ((row.citemClass || row.citemId) && !capability.project) {
    errors.push(`第 ${rowNo} 行当前科目未启用项目辅助核算`)
  }
  if (capability.lockedProjectClassCode && row.citemClass && row.citemClass !== capability.lockedProjectClassCode) {
    errors.push(`第 ${rowNo} 行项目分类必须为科目挂载的项目分类【${capability.lockedProjectClassCode}】`)
  }
  if (row.citemId) {
    if (!row.citemClass) {
      errors.push(`第 ${rowNo} 行选择项目时必须同时选择项目分类`)
      return
    }
    const project = projectMap.get(row.citemId)
    if (!project) {
      errors.push(`第 ${rowNo} 行项目不存在或当前不可用`)
      return
    }
    if (project.parentValue && project.parentValue !== row.citemClass) {
      errors.push(`第 ${rowNo} 行项目分类与项目归属不匹配`)
    }
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
    validateEntryLength(row, rowNo, errors)
    validateEntrySelection(row, rowNo, errors)
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
    resetLeafSubjectHistory(form.entries, res.data.accountOptions)
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

function handleSubjectChange(index: number, value?: string | number) {
  selectRow(index)
  const row = form.entries[index]
  if (!row) return
  row.ccode = normalizeText(typeof value === 'number' ? String(value) : value) || ''
  const option = findAccountOptionByCode(row.ccode)
  row.ccodeName = option?.name || ''
  if (!row.ccode) {
    delete lastValidLeafSubjectByRow[row.localId]
    clearAssistSelections(row)
    return
  }
  if (isLeafAccountOption(option)) {
    rememberLeafSubject(row, option)
  }
}

function handleSubjectFieldFocus(index: number) {
  if (index === selectedRowIndex.value) {
    selectRow(index)
    return
  }
  void tryLeaveSubjectField(index)
}

function handleSubjectDropdownVisibleChange(index: number, visible: boolean) {
  if (!visible) return
  handleSubjectFieldFocus(index)
}

function handleEntryFieldFocus(index: number) {
  void tryLeaveSubjectField(index)
}

function handleAssistFieldFocus() {
  void ensureSelectedRowUsesLeafSubject()
}

function insertEntryAfter(index: number) {
  if (isReadonlyMode.value) return
  const currency = voucherMeta.value?.defaultCurrency || 'CNY'
  form.entries.splice(index + 1, 0, createEntry(currency, index + 2))
  form.entries = ensureMinimumRows(form.entries, currency, Math.max(form.entries.length, MIN_ENTRY_ROWS))
  resetLeafSubjectHistory(form.entries)
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
  resetLeafSubjectHistory(form.entries)
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

  if (isDetailRoute.value || isReviewMode.value) {
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
  if (!(await ensureSelectedRowUsesLeafSubject())) return
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

async function handleReviewVoucher() {
  const companyId = financeCompany.currentCompanyId || form.companyId
  if (!companyId || !detailVoucherNo.value) return

  reviewActing.value = true
  currentToolbarLoadingKey.value = 'review'
  try {
    const res = await financeApi.reviewVoucher(companyId, detailVoucherNo.value)
    ElMessage.success(`凭证审核成功：${res.data.voucherNo}`)
    if (res.data.nextVoucherNo) {
      await router.replace({
        name: 'finance-review-voucher-detail',
        params: { voucherNo: res.data.nextVoucherNo }
      })
      return
    }
    await loadDetail(companyId, detailVoucherNo.value)
    if (res.data.lastVoucherOfMonth) {
      ElMessage.warning('当前是最后一张')
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '审核凭证失败'))
  } finally {
    currentToolbarLoadingKey.value = ''
    reviewActing.value = false
  }
}

async function handleUnreviewVoucher() {
  const companyId = financeCompany.currentCompanyId || form.companyId
  if (!companyId || !detailVoucherNo.value) return

  reviewActing.value = true
  currentToolbarLoadingKey.value = 'unreview'
  try {
    const res = await financeApi.unreviewVoucher(companyId, detailVoucherNo.value)
    ElMessage.success(`凭证反审核成功：${res.data.voucherNo}`)
    await loadDetail(companyId, detailVoucherNo.value)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '反审核凭证失败'))
  } finally {
    currentToolbarLoadingKey.value = ''
    reviewActing.value = false
  }
}

async function handleToggleVoucherError() {
  const companyId = financeCompany.currentCompanyId || form.companyId
  if (!companyId || !detailVoucherNo.value) return

  const clearing = voucherDetail.value?.status === 'ERROR'
  reviewActing.value = true
  currentToolbarLoadingKey.value = 'markError'
  try {
    const res = clearing
      ? await financeApi.clearVoucherError(companyId, detailVoucherNo.value)
      : await financeApi.markVoucherError(companyId, detailVoucherNo.value)
    ElMessage.success(clearing ? `凭证取消错误成功：${res.data.voucherNo}` : `凭证标记错误成功：${res.data.voucherNo}`)
    await loadDetail(companyId, detailVoucherNo.value)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, clearing ? '取消错误失败' : '标记错误失败'))
  } finally {
    currentToolbarLoadingKey.value = ''
    reviewActing.value = false
  }
}

async function handleExportCurrentVoucher() {
  const companyId = financeCompany.currentCompanyId || form.companyId
  if (!companyId || !detailVoucherNo.value) return
  try {
    await financeApi.exportVouchers({ companyId, voucherNo: detailVoucherNo.value })
    ElMessage.success('当前凭证已开始导出')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '导出当前凭证失败'))
  }
}

function handleFindInEntries() {
  const keyword = window.prompt('请输入关键字（摘要 / 科目编码 / 科目名称）')
  const normalizedKeyword = normalizeText(keyword)?.toLowerCase()
  if (!normalizedKeyword) {
    return
  }
  const matchedIndex = form.entries.findIndex((row) => {
    return [row.cdigest, row.ccode, row.ccodeName]
      .filter((item): item is string => Boolean(item))
      .some((item) => item.toLowerCase().includes(normalizedKeyword))
  })
  if (matchedIndex < 0) {
    ElMessage.warning('当前凭证未找到匹配分录')
    return
  }
  selectRow(matchedIndex)
  ElMessage.success(`已定位到第 ${matchedIndex + 1} 行`)
}

function handleToolbarAction(action: ToolbarActionKey) {
  if (action === 'new') return void handleNewVoucher()
  if (action === 'modify') return void enterEditMode()
  if (action === 'insert') return insertEntryAfter(selectedRowIndex.value)
  if (action === 'delete') return removeSelectedEntry()
  if (action === 'save') return void handleSave()
  if (action === 'review') return void handleReviewVoucher()
  if (action === 'unreview') return void handleUnreviewVoucher()
  if (action === 'markError') return void handleToggleVoucherError()
  if (action === 'find') return void handleFindInEntries()
  if (action === 'export' && isReviewMode.value) return void handleExportCurrentVoucher()

  const descriptions: Record<Exclude<ToolbarActionKey, 'new' | 'modify' | 'insert' | 'delete' | 'save' | 'review' | 'unreview' | 'markError' | 'find'>, string> = {
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
    void tryLeaveSubjectField(index - 1)
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    void tryLeaveSubjectField(index + 1)
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

function formatVoucherOptionLabel(option?: FinanceVoucherOption | null) {
  if (!option) return ''
  if (option.code && option.name) return `${option.code}  ${option.name}`
  if (option.name) return option.name
  if (option.code) return option.code
  return option.label || option.value
}

function filterDepartmentTreeNode(query: string, data?: DepartmentTreeOption) {
  const keyword = normalizeText(query)?.toLowerCase()
  if (!keyword) return true
  return [data?.label, data?.code, data?.name, data?.value]
    .filter((item): item is string => Boolean(item))
    .some((item) => item.toLowerCase().includes(keyword))
}

function resolveAccountLabel(code?: string, accountName?: string) {
  if (!code) return '当前行'
  const matched = voucherMeta.value?.accountOptions.find((item) => item.value === code)
  if (matched) return formatVoucherOptionLabel(matched)
  if (accountName) return `${code}  ${accountName}`
  return code
}

function resolveCompanyName(companyId?: string) {
  if (!companyId) return '未设置'
  const matched = voucherMeta.value?.companyOptions.find((item) => item.value === companyId)
  return matched?.name || companyId
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
    if (isDetailRoute.value || isReviewMode.value) {
      editingExisting.value = false
      await router.replace({ name: backToListRouteName.value })
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

defineExpose({
  assistDisabledState,
  currentAssistCapability,
  departmentTreeOptions,
  filteredProjectOptions,
  form,
  projectClassOptionsForDisplay,
  selectedRow,
  getFilteredProjectOptions: () => filteredProjectOptions.value
})
</script>
<style scoped>
.voucher-page { height: 100%; display: flex; min-height: 0; flex-direction: column; gap: 10px; overflow: hidden; }
.voucher-content-scroll { min-height: 0; flex: 1; overflow: auto; padding-bottom: 8px; }
.voucher-shell { display: flex; min-height: 100%; flex-direction: column; gap: 12px; border-radius: 28px; background: radial-gradient(circle at top right, rgba(96,165,250,.1), transparent 28%), linear-gradient(180deg, #f8fbff 0%, #f3f6fb 100%); padding: 14px; }
.voucher-notice-panel { display: flex; flex-direction: column; gap: 8px; }
.voucher-notice-item { border-radius: 18px; border: 1px solid #d8e2f0; padding: 12px 14px; font-size: 13px; font-weight: 600; line-height: 1.6; }
.voucher-notice-item-warning { border-color: #f5d38b; background: linear-gradient(180deg, #fff8e8 0%, #fff3da 100%); color: #8a5a12; }
.voucher-notice-item-danger { border-color: #f1b4b4; background: linear-gradient(180deg, #fff3f3 0%, #ffe6e6 100%); color: #a63535; }
.voucher-notice-item-info { border-color: #bfd4f2; background: linear-gradient(180deg, #f3f8ff 0%, #e9f1ff 100%); color: #325985; }
.voucher-page-header { display: flex; justify-content: center; }
.voucher-page-header h1 { font-size: 21px; font-weight: 700; color: #1e3a5f; letter-spacing: .2em; line-height: 1.15; }
.voucher-toolbar-panel { position: sticky; top: 0; z-index: 20; display: flex; flex-wrap: wrap; gap: 11px; border-bottom: 1px solid rgba(216,226,240,.9); border-radius: 22px; background: rgba(255,255,255,.92); padding: 11px 13px; backdrop-filter: blur(10px); box-shadow: 0 12px 24px rgba(15,23,42,.07); }
.toolbar-group { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.toolbar-group + .toolbar-group { position: relative; padding-left: 13px; }
.toolbar-group + .toolbar-group::before { position: absolute; left: 0; top: 4px; height: 28px; width: 1px; background: linear-gradient(180deg, transparent 0%, #d5deea 22%, #d5deea 78%, transparent 100%); content: ''; }
.toolbar-button { height: 32px; min-width: 88px; border-radius: 12px; border-color: #d6e0ec; background: #fff; color: #365070; font-weight: 600; padding: 0 12px; }
.toolbar-button-large { height: 42px; min-width: 116px; padding: 0 16px; font-size: 14px; }
.toolbar-button-accent { border-color: #9cbbe3; background: linear-gradient(180deg, #f0f7ff 0%, #e4efff 100%); color: #24528a; box-shadow: 0 12px 24px rgba(59,130,246,.14); }
.toolbar-button-primary { box-shadow: 0 16px 30px rgba(37,99,235,.2); }
.voucher-info-band { display: grid; grid-template-columns: minmax(0,1fr); gap: 10px; }
.voucher-lower { display: grid; grid-template-columns: minmax(0,1fr) 260px; gap: 10px; }
.voucher-lower-full { grid-template-columns: minmax(0,1fr); }
.voucher-info-main, .voucher-ledger-card, .voucher-assist-card, .voucher-side-card { border-radius: 22px; border: 1px solid #d8e2f0; background: rgba(255,255,255,.94); box-shadow: 0 10px 24px rgba(15,23,42,.04); padding: 14px; }
.voucher-info-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 8px 12px; }
.assist-grid { display: grid; grid-template-columns: repeat(12, minmax(0,1fr)); gap: 8px 12px; }
.voucher-info-field, .assist-field { display: flex; align-items: center; gap: 10px; min-height: 34px; }
.voucher-field-label { flex: 0 0 auto; min-width: 56px; font-size: 12px; font-weight: 600; color: #5f7391; line-height: 1.2; }
.assist-field { grid-column: span 3; }
.voucher-info-spacer { min-height: 34px; }
.voucher-info-field > :not(.voucher-field-label), .assist-field > :not(.voucher-field-label) { flex: 1 1 auto; min-width: 0; }
.voucher-code-box { display: flex; height: 34px; align-items: center; justify-content: center; border-radius: 12px; border: 1px solid #cfe0f5; background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%); font-weight: 700; color: #24466f; }
.voucher-company-box { justify-content: flex-start; padding: 0 14px; font-weight: 600; }
.voucher-number-group { display: grid; grid-template-columns: minmax(84px,96px) auto minmax(0,1fr); align-items: center; gap: 6px; }
.voucher-number-separator { display: inline-flex; align-items: center; justify-content: center; color: #5f7391; font-weight: 700; }
.voucher-ledger-card { display: flex; min-height: 0; flex-direction: column; }
.voucher-grid { display: flex; min-height: 0; flex: 1; flex-direction: column; overflow: hidden; border-radius: 18px; border: 1px solid #d7e0eb; background: #fdfefe; }
.voucher-grid-layout { display: grid; grid-template-columns: minmax(220px,1.2fr) minmax(280px,1.4fr) minmax(160px,.8fr) minmax(160px,.8fr); }
.voucher-grid-header, .voucher-grid-footer { flex-shrink: 0; background: linear-gradient(180deg, #f3f7fd 0%, #edf3fb 100%); color: #49627f; font-size: 13px; font-weight: 700; }
.voucher-grid-header > div, .voucher-grid-footer > div { padding: 8px 12px; }
.voucher-grid-body { min-height: 0; flex: 1; overflow: auto; background: linear-gradient(180deg, rgba(248,251,255,.56) 0%, rgba(255,255,255,.92) 100%); }
.voucher-grid-row { min-height: 41px; border-top: 1px solid #e4ebf4; transition: background-color .16s ease, box-shadow .16s ease; }
.voucher-grid-row:hover { background: rgba(239,246,255,.72); }
.voucher-grid-row-active { background: rgba(219,234,254,.5); box-shadow: inset 4px 0 0 #4f8ad8; }
.voucher-grid-row-readonly:hover { background: rgba(219,234,254,.5); }
.voucher-grid-row:focus { outline: none; }
.voucher-cell { display: flex; flex-direction: column; justify-content: center; gap: 2px; padding: 4px 10px; }
.voucher-cell-digest { padding-right: 6px; }
.voucher-inline-field { display: flex; align-items: center; gap: 7px; }
.voucher-row-index { display: inline-flex; min-width: 22px; align-items: center; justify-content: center; color: #788ca6; font-size: 12px; font-weight: 700; }
.voucher-footer-amount { text-align: right; color: #173a61; font-family: Consolas, Monaco, monospace; }
.voucher-signature { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 10px; border-radius: 18px; border: 1px solid #d8e2f0; background: rgba(255,255,255,.92); padding: 12px 14px; color: #4a627f; font-size: 13px; }
.action-dialog-content { color: #506680; line-height: 1.8; }
.action-dialog-subtle { margin-top: 8px; color: #8a9bb1; font-size: 12px; }
:deep(.voucher-info-field .el-input__wrapper), :deep(.voucher-info-field .el-select__wrapper), :deep(.voucher-info-field .el-date-editor), :deep(.assist-field .el-input__wrapper), :deep(.assist-field .el-select__wrapper), :deep(.voucher-cell .el-input__wrapper), :deep(.voucher-cell .el-select__wrapper) { border-radius: 12px; box-shadow: 0 0 0 1px #d8e2f0 inset; }
:deep(.voucher-cell .el-input-number), :deep(.voucher-cell .el-input-number .el-input__wrapper), :deep(.assist-field .el-input-number), :deep(.assist-field .el-input-number .el-input__wrapper), :deep(.voucher-info-field .el-input-number), :deep(.voucher-info-field .el-input-number .el-input__wrapper) { width: 100%; }
:deep(.voucher-info-field .el-input__wrapper), :deep(.voucher-info-field .el-select__wrapper), :deep(.voucher-info-field .el-date-editor), :deep(.voucher-info-field .el-input-number .el-input__wrapper), :deep(.assist-field .el-input__wrapper), :deep(.assist-field .el-select__wrapper), :deep(.assist-field .el-input-number .el-input__wrapper) { min-height: 34px; }
:deep(.voucher-number-group .el-select__wrapper), :deep(.voucher-number-group .el-input__wrapper) { min-height: 34px; }
:deep(.voucher-cell .el-input__wrapper), :deep(.voucher-cell .el-select__wrapper), :deep(.voucher-cell .money-input__control), :deep(.voucher-cell .el-input-number .el-input__wrapper) { min-height: 32px; }
@media (max-width: 1440px) { .voucher-lower { grid-template-columns: 1fr; } }
@media (max-width: 1024px) { .voucher-info-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } .assist-grid { grid-template-columns: repeat(6, minmax(0,1fr)); } .assist-field { grid-column: span 3; } .voucher-info-spacer { display: none; } .voucher-grid-layout { min-width: 860px; } }
@media (max-width: 768px) {
  .voucher-page-header h1 { font-size: 18px; letter-spacing: .12em; }
  .toolbar-group { width: 100%; }
  .toolbar-group + .toolbar-group { padding-left: 0; padding-top: 8px; }
  .toolbar-group + .toolbar-group::before { left: 0; top: 0; height: 1px; width: 100%; }
  .voucher-info-grid, .assist-grid { grid-template-columns: repeat(2, minmax(0,1fr)); }
  .voucher-info-field, .assist-field { grid-column: span 2; }
  .voucher-info-spacer { display: none; }
  .voucher-info-field, .assist-field { gap: 8px; }
  .voucher-field-label { min-width: 52px; }
  .voucher-signature { flex-direction: column; align-items: flex-start; }
}
</style>
