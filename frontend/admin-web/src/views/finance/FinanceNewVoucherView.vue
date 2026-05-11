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
                <el-input-number v-model="form.iperiod" :min="1" :max="12" :controls="false" :disabled="periodFieldLocked" />
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
                  <subject-tree-select
                    v-model="row.ccode"
                    :options="accountOptionsForDisplay"
                    clearable
                    placeholder="请选择科目"
                    :disabled="isReadonlyMode"
                    :data-subject-row-id="row.localId"
                    @focus="handleSubjectFieldFocus(index)"
                    @change="handleSubjectChange(index, $event)"
                    @visible-change="handleSubjectDropdownVisibleChange(index, $event)"
                  />
                </div>
                <div class="voucher-cell">
                  <money-input v-model="row.md" placeholder="0.00" :readonly="isReadonlyMode" :disabled="isReadonlyMode" @focus="handleEntryFieldFocus(index)" @blur="handleAmountBlur(index)" @keydown="handleAmountKeydown($event, index, 'md')" />
                </div>
                <div class="voucher-cell">
                  <money-input v-model="row.mc" placeholder="0.00" :readonly="isReadonlyMode" :disabled="isReadonlyMode" @focus="handleEntryFieldFocus(index)" @blur="handleAmountBlur(index)" @keydown="handleAmountKeydown($event, index, 'mc')" />
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
                  filterable v-bind="globalFilterableSelectProps"
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
                <employee-tree-select v-model="selectedRow.cpersonId" :departments="voucherMeta?.departmentOptions || []" :employees="voucherMeta?.employeeDirectory || []" clearable placeholder="请选择人员" :disabled="assistDisabledState.employee" label-mode="finance-assist" @focus="handleAssistFieldFocus" />
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">客户</span>
                <finance-assist-option-select
                  v-model="selectedRow.ccusId"
                  :options="voucherMeta?.customerOptions || []"
                  placeholder="请选择客户"
                  :disabled="assistDisabledState.customer"
                  addable
                  add-text="增加"
                  @focus="handleAssistFieldFocus"
                  @request-add="openVoucherCustomerCreateDialog"
                />
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">供应商</span>
                <finance-assist-option-select
                  v-model="selectedRow.csupId"
                  :options="voucherMeta?.supplierOptions || []"
                  placeholder="请选择供应商"
                  :disabled="assistDisabledState.supplier"
                  addable
                  add-text="增加"
                  @focus="handleAssistFieldFocus"
                  @request-add="openVoucherSupplierCreateDialog"
                />
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">项目分类</span>
                <finance-assist-option-select
                  v-model="selectedRow.citemClass"
                  :options="projectClassOptionsForDisplay"
                  placeholder="请选择项目分类"
                  :disabled="assistDisabledState.projectClass"
                  @focus="handleAssistFieldFocus"
                />
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">项目</span>
                <finance-assist-option-select
                  v-model="selectedRow.citemId"
                  :options="filteredProjectOptions"
                  placeholder="请选择项目"
                  :disabled="assistDisabledState.project"
                  addable
                  add-text="增加"
                  :add-disabled="!resolvedVoucherProjectClassCode"
                  add-disabled-message="请先选择项目分类"
                  @focus="handleAssistFieldFocus"
                  @request-add="openVoucherProjectCreateDialog"
                />
              </label>
              <label class="assist-field">
                <span class="voucher-field-label">现金流量</span>
                <el-input :model-value="selectedRow.cashFlowItemName || ''" readonly placeholder="请选择现金流量" :disabled="isReadonlyMode || !requiresRowCashFlow(selectedRow)" @focus="handleCashFlowFieldFocus" />
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

    <el-dialog v-model="cashFlowDialogVisible" title="选择现金流量" width="480px" destroy-on-close :close-on-click-modal="false" :close-on-press-escape="false" :show-close="isReadonlyMode">
      <div class="space-y-4">
        <p class="action-dialog-content">当前科目已启用现金管理，请先选择一条现金流量。</p>
        <el-radio-group v-model="cashFlowDialogSelection" class="cash-flow-radio-group">
          <el-radio v-for="item in voucherMeta?.cashFlowOptions || []" :key="item.value" :label="item.value">
            {{ formatVoucherOptionLabel(item) }}
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="closeCashFlowDialog">取消</el-button>
        <el-button type="primary" @click="confirmCashFlowSelection">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="actionDialog.visible" :title="actionDialog.title" width="420px" destroy-on-close>
      <div class="action-dialog-content">
        <p>{{ actionDialog.description }}</p>
        <p class="action-dialog-subtle">当前为第一阶段工作台能力，后续可继续接入正式业务流程。</p>
      </div>
      <template #footer>
        <el-button @click="actionDialog.visible = false">知道了</el-button>
      </template>
    </el-dialog>
    <finance-customer-archive-dialog
      ref="customerArchiveDialogRef"
      :company-id="financeCompany.currentCompanyId || form.companyId"
      :company-name="currentCompanyName"
      @saved="handleVoucherCustomerCreated"
    />
    <finance-supplier-archive-dialog
      ref="supplierArchiveDialogRef"
      :company-id="financeCompany.currentCompanyId || form.companyId"
      :company-name="currentCompanyName"
      @saved="handleVoucherSupplierCreated"
    />
    <finance-project-archive-dialog
      ref="projectArchiveDialogRef"
      :company-id="financeCompany.currentCompanyId || form.companyId"
      :company-name="currentCompanyName"
      :project-class-options="voucherMeta?.projectClassOptions || []"
      @saved="handleVoucherProjectCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
  type FinanceVoucherForm,
  type FinanceVoucherMeta,
  type FinanceVoucherOption,
  type FinanceVoucherSavePayload
} from '@/api'
import FinanceAssistOptionSelect from '@/components/finance/FinanceAssistOptionSelect.vue'
import FinanceCustomerArchiveDialog from '@/components/finance/FinanceCustomerArchiveDialog.vue'
import FinanceProjectArchiveDialog from '@/components/finance/FinanceProjectArchiveDialog.vue'
import FinanceSupplierArchiveDialog from '@/components/finance/FinanceSupplierArchiveDialog.vue'
import EmployeeTreeSelect from '@/components/inputs/EmployeeTreeSelect.vue'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import SubjectTreeSelect from '@/components/inputs/SubjectTreeSelect.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'
import { useFinanceNewVoucherAssistCashflowOwner } from './composables/useFinanceNewVoucherAssistCashflowOwner'
import { useFinanceNewVoucherBootstrap } from './composables/useFinanceNewVoucherBootstrap'
import { useFinanceNewVoucherHeaderMetaOwner } from './composables/useFinanceNewVoucherHeaderMetaOwner'
import {
  useFinanceNewVoucherPageOrchestration,
  type FinanceNewVoucherToolbarActionKey
} from './composables/useFinanceNewVoucherPageOrchestration'
import { useFinanceNewVoucherPageUtils } from './composables/useFinanceNewVoucherPageUtils'
import {
  useFinanceNewVoucherRowOwner,
  type FinanceVoucherEntryRow as VoucherEntryRow
} from './composables/useFinanceNewVoucherRowOwner'
import { useFinanceNewVoucherValidationPayload } from './composables/useFinanceNewVoucherValidationPayload'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { formatMoney } from '@/utils/money'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import { formatFinanceAssistOptionLabel } from '@/utils/financeAssistOptions'


type ToolbarActionKey = FinanceNewVoucherToolbarActionKey
type VoucherFormState = Omit<FinanceVoucherForm, 'entries'> & { entries: VoucherEntryRow[] }
type VoucherPageMode = 'create' | 'detail' | 'review'

interface ToolbarAction {
  key: ToolbarActionKey
  label: string
  icon: Component
  emphasis?: 'primary' | 'secondary'
  disabled?: boolean
}

function createBootstrapEntry(defaultCurrencyCode: string, defaultCurrencyName: string, rowNo: number): VoucherEntryRow {
  return {
    localId: `bootstrap-entry-${rowNo}`,
    inid: rowNo,
    cdigest: '',
    ccode: '',
    cdeptId: '',
    cpersonId: '',
    ccusId: '',
    csupId: '',
    citemClass: '',
    citemId: '',
    cashFlowItemId: undefined,
    cashFlowItemName: '',
    cexchName: defaultCurrencyName,
    currencyCode: defaultCurrencyCode,
    nfrat: 1,
    md: '',
    mc: '',
    ndS: undefined,
    ncS: undefined
  }
}

function createBootstrapEntries(defaultCurrencyCode: string, defaultCurrencyName: string, minRows: number) {
  return Array.from({ length: minRows }, (_, index) => createBootstrapEntry(defaultCurrencyCode, defaultCurrencyName, index + 1))
}

const DRAFT_STORAGE_KEY = 'finance-new-voucher-draft'
const MIN_ENTRY_ROWS = 8
const COMPANY_SWITCH_GUARD_KEY = 'finance-new-voucher'
const ENTRY_FIELD_MAX_LENGTH: Record<'cdigest' | 'ccode' | 'cdeptId' | 'cpersonId' | 'ccusId' | 'csupId' | 'citemClass' | 'citemId' | 'cexchName' | 'currencyCode', number> = {
  cdigest: 255,
  ccode: 64,
  cdeptId: 64,
  cpersonId: 64,
  ccusId: 64,
  csupId: 64,
  citemClass: 2,
  citemId: 6,
  cexchName: 32,
  currencyCode: 32
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
  cexchName: '币种名称',
  currencyCode: '币种编码'
}

const props = withDefaults(defineProps<{ pageMode?: VoucherPageMode; voucherNo?: string }>(), {
  pageMode: 'create',
  voucherNo: ''
})
const router = useRouter()
const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()
const currentUser = readStoredUser()
const {
  resolveErrorMessage,
  toOptionalMoney,
  toOptionalString,
  toOptionalDecimal
} = useFinanceNewVoucherPageUtils()

const validationErrors = ref<string[]>([])
const selectedRowIndex = ref(0)
const lastCommittedSnapshot = ref('')
const customerArchiveDialogRef = ref<InstanceType<typeof FinanceCustomerArchiveDialog> | null>(null)
const supplierArchiveDialogRef = ref<InstanceType<typeof FinanceSupplierArchiveDialog> | null>(null)
const projectArchiveDialogRef = ref<InstanceType<typeof FinanceProjectArchiveDialog> | null>(null)

const form = reactive<VoucherFormState>({
  companyId: '',
  iyear: undefined,
  iyperiod: undefined,
  iperiod: 1,
  csign: '记',
  inoId: undefined,
  dbillDate: '',
  idoc: 0,
  cbill: '',
  ctext1: '',
  ctext2: '',
  entries: createBootstrapEntries('CNY', '人民币', MIN_ENTRY_ROWS)
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
const periodFieldLocked = computed(() => props.pageMode === 'create' || voucherHeaderLocked.value)
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

const selectedRow = computed(() => form.entries[Math.min(selectedRowIndex.value, Math.max(form.entries.length - 1, 0))] as VoucherEntryRow)
const hasUnsavedChanges = computed(() => Boolean(voucherMeta.value) && buildSnapshot() !== lastCommittedSnapshot.value)
const defaultCurrencyCode = computed(() => voucherMeta.value?.defaultCurrencyCode || voucherMeta.value?.defaultCurrency || 'CNY')
const defaultCurrencyName = computed(() => voucherMeta.value?.defaultCurrencyName || '人民币')
const {
  loading,
  initializing,
  voucherMeta,
  voucherDetail,
  hasDraft,
  editingExisting,
  initializePage,
  loadMeta,
  loadDetail
} = useFinanceNewVoucherBootstrap({
  financeCompany,
  router,
  companySwitchGuardKey: COMPANY_SWITCH_GUARD_KEY,
  pageMode: computed(() => props.pageMode),
  detailVoucherNo,
  isDetailRoute,
  isReviewMode,
  backToListRouteName,
  hasUnsavedChanges: () => hasUnsavedChanges.value,
  validationErrors,
  readDraft,
  resetFormFromMeta,
  applyDraft,
  applyDetail,
  markCommitted,
  parseVoucherCompanyId,
  resolveErrorMessage
})
const {
  cashFlowDialogVisible,
  cashFlowDialogSelection,
  currentAssistCapability,
  assistDisabledState,
  projectClassOptionsForDisplay,
  departmentTreeOptions,
  filteredProjectOptions,
  requiresRowCashFlow,
  resetLeafSubjectHistory,
  ensureSelectedRowUsesLeafSubject,
  tryLeaveSubjectField,
  closeCashFlowDialog,
  confirmCashFlowSelection,
  ensureRowCashFlowState,
  validateEntrySelection,
  handleSubjectChange,
  handleSubjectFieldFocus,
  handleSubjectDropdownVisibleChange,
  handleAssistFieldFocus,
  handleCashFlowFieldFocus,
  handleAmountBlur,
  filterDepartmentTreeNode,
  resolveAccountLabel
} = useFinanceNewVoucherAssistCashflowOwner({
  voucherMeta,
  entries: computed(() => form.entries),
  selectedRow,
  selectedRowIndex,
  isReadonlyMode,
  selectRow
})
const {
  effectiveRows,
  totalDebit,
  totalCredit,
  buildPayload,
  validateVoucher,
  isEntryBlank,
  buildSnapshot
} = useFinanceNewVoucherValidationPayload({
  form,
  voucherMeta,
  validationErrors,
  entryFieldMaxLength: ENTRY_FIELD_MAX_LENGTH,
  entryFieldLabels: ENTRY_FIELD_LABELS,
  validateEntrySelection,
  toOptionalString,
  toOptionalMoney,
  toOptionalDecimal
})
const {
  createEntry,
  createEntryFromValue,
  ensureMinimumRows,
  handleEntryFieldFocus,
  insertEntryAfter,
  removeSelectedEntry,
  handleGridKeydown,
  handleAmountKeydown
} = useFinanceNewVoucherRowOwner({
  getEntries: () => form.entries,
  setEntries: (entries) => {
    form.entries = entries
  },
  selectedRow,
  selectedRowIndex,
  effectiveRowCount: computed(() => effectiveRows.value.length),
  isReadonlyMode,
  defaultCurrencyCode,
  defaultCurrencyName,
  minEntryRows: MIN_ENTRY_ROWS,
  isEntryBlank,
  tryLeaveSubjectField,
  resetLeafSubjectHistory
})
const {
  currentCompanyName,
  currentRowLabel,
  voucherNoticeItems,
  remarkText,
  voucherNoInput
} = useFinanceNewVoucherHeaderMetaOwner({
  form,
  financeCompany,
  voucherMeta,
  selectedRow,
  currentAssistCapability,
  voucherHeaderLocked,
  loading,
  initializing,
  resolveAccountLabel,
  resetLeafSubjectHistory,
  resolveErrorMessage
})
const {
  saving,
  reviewActing,
  currentToolbarLoadingKey,
  actionDialog,
  handleToolbarAction
} = useFinanceNewVoucherPageOrchestration({
  router,
  voucherMeta,
  voucherDetail,
  editingExisting,
  validationErrors,
  isDetailRoute,
  isReviewMode,
  canEditExisting,
  detailVoucherNo,
  selectedRow,
  selectedRowIndex,
  currentCompanyId: () => financeCompany.currentCompanyId || form.companyId,
  getCurrentContext: () => ({
    companyId: financeCompany.currentCompanyId || form.companyId,
    billDate: form.dbillDate,
    csign: form.csign
  }),
  getEntries: () => form.entries,
  selectRow,
  loadMeta,
  loadDetail,
  clearDraft,
  resetFormFromMeta,
  markCommitted,
  buildPayload,
  validateVoucher,
  ensureSelectedRowUsesLeafSubject,
  ensureRowCashFlowState,
  handleCashFlowFieldFocus,
  resolveErrorMessage,
  insertEntryAfter,
  removeSelectedEntry
})
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
const resolvedVoucherProjectClassCode = computed(
  () => currentAssistCapability.value.lockedProjectClassCode || selectedRow.value?.citemClass || ''
)

watch(() => form.entries.length, () => {
  if (selectedRowIndex.value >= form.entries.length) {
    selectedRowIndex.value = Math.max(0, form.entries.length - 1)
  }
})

watch(
  () => [props.pageMode, financePeriod.currentYearPeriod] as const,
  () => {
    if (props.pageMode !== 'create') {
      return
    }
    syncFormPeriodFromGlobal()
  },
  { immediate: true }
)

function syncFormPeriodFromGlobal() {
  if (!financePeriod.hasPeriodContext) {
    return false
  }
  form.iyear = financePeriod.currentYear
  form.iperiod = financePeriod.currentPeriod
  form.iyperiod = financePeriod.currentYearPeriod
  return true
}

function resetFormFromMeta(meta: FinanceVoucherMeta, companyId = financeCompany.currentCompanyId) {
  form.companyId = companyId || meta.defaultCompanyId || ''
  if (!syncFormPeriodFromGlobal()) {
    form.iyear = meta.defaultYear ?? new Date(meta.defaultBillDate).getFullYear()
    form.iyperiod = meta.defaultYearPeriod ?? ((form.iyear || new Date(meta.defaultBillDate).getFullYear()) * 100 + meta.defaultPeriod)
    form.iperiod = meta.defaultPeriod
  }
  form.csign = meta.defaultVoucherType
  form.inoId = meta.suggestedVoucherNo
  form.dbillDate = meta.defaultBillDate
  form.idoc = meta.defaultAttachedDocCount
  form.cbill = meta.defaultMaker
  form.ctext1 = ''
  form.ctext2 = ''
  form.entries = ensureMinimumRows(
    [createEntry(defaultCurrencyCode.value, defaultCurrencyName.value, 1), createEntry(defaultCurrencyCode.value, defaultCurrencyName.value, 2)],
    defaultCurrencyCode.value,
    defaultCurrencyName.value
  )
  resetLeafSubjectHistory(form.entries, meta.accountOptions)
  selectedRowIndex.value = 0
}

function applyDraft(draft: FinanceVoucherSavePayload, meta: FinanceVoucherMeta, companyId = financeCompany.currentCompanyId) {
  form.companyId = companyId || draft.companyId || meta.defaultCompanyId || ''
  if (!syncFormPeriodFromGlobal()) {
    form.iyear = draft.iyear ?? meta.defaultYear ?? new Date(meta.defaultBillDate).getFullYear()
    form.iyperiod = draft.iyperiod ?? meta.defaultYearPeriod ?? ((form.iyear || new Date(meta.defaultBillDate).getFullYear()) * 100 + (draft.iperiod || meta.defaultPeriod))
    form.iperiod = draft.iperiod || meta.defaultPeriod
  }
  form.csign = draft.csign || meta.defaultVoucherType
  form.inoId = draft.inoId || meta.suggestedVoucherNo
  form.dbillDate = draft.dbillDate || meta.defaultBillDate
  form.idoc = draft.idoc ?? meta.defaultAttachedDocCount
  form.cbill = draft.cbill || meta.defaultMaker
  form.ctext1 = draft.ctext1 || ''
  form.ctext2 = draft.ctext2 || ''
  form.entries = ensureMinimumRows(
    (
      draft.entries?.length
        ? draft.entries
        : [createEntry(defaultCurrencyCode.value, defaultCurrencyName.value, 1), createEntry(defaultCurrencyCode.value, defaultCurrencyName.value, 2)]
    ).map((item, index) => createEntryFromValue(item, defaultCurrencyCode.value, defaultCurrencyName.value, index + 1)),
    defaultCurrencyCode.value,
    defaultCurrencyName.value
  )
  resetLeafSubjectHistory(form.entries, meta.accountOptions)
  selectedRowIndex.value = 0
}

function applyDetail(detail: FinanceVoucherDetail, meta: FinanceVoucherMeta) {
  form.companyId = detail.companyId
  form.iyear = detail.iyear
  form.iyperiod = detail.iyperiod
  form.iperiod = detail.iperiod
  form.csign = detail.csign
  form.inoId = detail.inoId
  form.dbillDate = detail.dbillDate
  form.idoc = detail.idoc
  form.cbill = detail.cbill
  form.ctext1 = detail.ctext1 || ''
  form.ctext2 = detail.ctext2 || ''
  form.entries = ensureMinimumRows(
    detail.entries.map((item, index) => createEntryFromValue(item, defaultCurrencyCode.value, defaultCurrencyName.value, index + 1)),
    defaultCurrencyCode.value,
    defaultCurrencyName.value,
    Math.max(detail.entries.length, 2)
  )
  resetLeafSubjectHistory(form.entries, meta.accountOptions)
  selectedRowIndex.value = 0
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
function selectRow(index: number) {
  selectedRowIndex.value = Math.max(0, Math.min(index, form.entries.length - 1))
}

function formatVoucherOptionLabel(option?: FinanceVoucherOption | null) {
  return formatFinanceAssistOptionLabel(option)
}

async function refreshVoucherAssistMeta() {
  const companyId = financeCompany.currentCompanyId || form.companyId
  if (!companyId) {
    return
  }
  try {
    const res = await financeApi.getVoucherMeta({
      companyId,
      billDate: form.dbillDate,
      csign: form.csign
    })
    voucherMeta.value = res.data
    resetLeafSubjectHistory(form.entries, res.data.accountOptions)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '刷新辅助核算选项失败'))
  }
}

function openVoucherCustomerCreateDialog() {
  customerArchiveDialogRef.value?.openCreateDialog()
}

function openVoucherSupplierCreateDialog() {
  supplierArchiveDialogRef.value?.openCreateDialog()
}

function openVoucherProjectCreateDialog() {
  projectArchiveDialogRef.value?.openCreateDialog(resolvedVoucherProjectClassCode.value)
}

async function handleVoucherCustomerCreated(customerCode: string) {
  await refreshVoucherAssistMeta()
  selectedRow.value.ccusId = customerCode
}

async function handleVoucherSupplierCreated(vendorCode: string) {
  await refreshVoucherAssistMeta()
  selectedRow.value.csupId = vendorCode
}

async function handleVoucherProjectCreated(projectCode: string) {
  await refreshVoucherAssistMeta()
  if (resolvedVoucherProjectClassCode.value) {
    selectedRow.value.citemClass = resolvedVoucherProjectClassCode.value
  }
  selectedRow.value.citemId = projectCode
}

function buildDraftStorageKey(companyId = financeCompany.currentCompanyId) {
  return `${DRAFT_STORAGE_KEY}:${companyId || 'default'}`
}

function markCommitted() {
  lastCommittedSnapshot.value = buildSnapshot()
}

function parseVoucherCompanyId(voucherNo: string) {
  const parts = String(voucherNo || '').split('~')
  return parts.length === 4 || parts.length === 5 ? parts[0] ?? '' : ''
}

function moneyText(value: string) {
  return formatMoney(value)
}

defineExpose({
  assistDisabledState,
  cashFlowDialogVisible,
  currentAssistCapability,
  departmentTreeOptions,
  filteredProjectOptions,
  form,
  projectClassOptionsForDisplay,
  selectedRow,
  validateVoucher,
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
.cash-flow-radio-group { display: flex; flex-direction: column; gap: 10px; }
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
