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
          :title="toolbarActionTitle(action)"
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
                  <div
                    v-if="props.pageMode === 'create'"
                    ref="voucherNoSelectorRef"
                    class="voucher-no-selector"
                    :class="{ 'voucher-no-selector-open': savedVoucherDropdownVisible }"
                  >
                    <el-input
                      :model-value="voucherNoInputText"
                      placeholder="请输入凭证号"
                      :readonly="voucherHeaderLocked"
                      data-testid="voucher-no-input"
                      @update:model-value="handleVoucherNoInput"
                    />
                    <button
                      type="button"
                      class="voucher-no-trigger"
                      data-testid="voucher-no-trigger"
                      :disabled="voucherHeaderLocked"
                      :aria-expanded="savedVoucherDropdownVisible"
                      @click="toggleSavedVoucherDropdown"
                    >
                      <span class="voucher-no-trigger__caret" aria-hidden="true"></span>
                    </button>
                    <div v-if="savedVoucherDropdownVisible" class="voucher-no-dropdown" data-testid="voucher-no-dropdown">
                      <div v-if="savedVoucherSuggestionsLoading" class="voucher-no-dropdown__state">正在加载当月凭证...</div>
                      <div v-else-if="!filteredSavedVoucherSuggestions.length" class="voucher-no-dropdown__state">当月暂无匹配的已保存凭证</div>
                      <div v-else class="voucher-no-dropdown__list">
                        <button
                          v-for="item in filteredSavedVoucherSuggestions"
                          :key="item.voucherNo"
                          type="button"
                          class="voucher-no-option"
                          data-testid="voucher-no-option"
                          @mousedown.prevent="captureVoucherNoSwitchSnapshot"
                          @click="handleSavedVoucherSelect(item)"
                        >
                          <span class="voucher-no-suggestion__code">{{ item.displayVoucherNo }}</span>
                          <span class="voucher-no-suggestion__meta">{{ item.dbillDate }}</span>
                          <span class="voucher-no-suggestion__summary">{{ item.summary || '无摘要' }}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                  <el-input
                    v-else
                    v-model="voucherNoInput"
                    placeholder="请输入凭证号"
                    :readonly="voucherHeaderLocked"
                  />
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
                      :ref="makeGridCellRefBinder(row.localId, 'cdigest')"
                      v-model="row.cdigest"
                      :data-voucher-row-id="row.localId"
                      data-voucher-field="cdigest"
                      placeholder="请输入摘要"
                      :readonly="isReadonlyMode"
                      :maxlength="255"
                      @focus="handleEntryFieldFocus(index)"
                      @keydown="handleGridCellKeydown($event, index, 'cdigest')"
                    />
                  </div>
                </div>
                <div class="voucher-cell">
                  <subject-tree-select
                    :ref="makeGridCellRefBinder(row.localId, 'ccode')"
                    v-model="row.ccode"
                    :options="accountOptionsForDisplay"
                    clearable
                    placeholder="请选择科目"
                    :disabled="isReadonlyMode"
                    :data-subject-row-id="row.localId"
                    :data-voucher-row-id="row.localId"
                    data-voucher-field="ccode"
                    @focus="handleSubjectFieldFocus(index)"
                    @change="handleSubjectChange(index, $event)"
                    @visible-change="handleSubjectDropdownVisibleChange(index, $event)"
                    @keydown="handleGridCellKeydown($event, index, 'ccode')"
                  />
                </div>
                <div class="voucher-cell">
                  <money-input
                    :ref="makeGridCellRefBinder(row.localId, 'md')"
                    :model-value="row.md"
                    :data-voucher-row-id="row.localId"
                    data-voucher-field="md"
                    :allow-negative="true"
                    placeholder="0.00"
                    :readonly="isReadonlyMode"
                    :disabled="isReadonlyMode"
                    @update:modelValue="handleAmountFieldUpdate(index, 'md', $event)"
                    @focus="handleAmountFieldFocus(index, 'md')"
                    @blur="handleAmountBlur(index)"
                    @keydown="handleVoucherAmountKeydown($event, index, 'md')"
                  />
                </div>
                <div class="voucher-cell">
                  <money-input
                    :ref="makeGridCellRefBinder(row.localId, 'mc')"
                    :model-value="row.mc"
                    :data-voucher-row-id="row.localId"
                    data-voucher-field="mc"
                    :allow-negative="true"
                    placeholder="0.00"
                    :readonly="isReadonlyMode"
                    :disabled="isReadonlyMode"
                    @update:modelValue="handleAmountFieldUpdate(index, 'mc', $event)"
                    @focus="handleAmountFieldFocus(index, 'mc')"
                    @blur="handleAmountBlur(index)"
                    @keydown="handleVoucherAmountKeydown($event, index, 'mc')"
                  />
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
                <el-input
                  :model-value="selectedRowCashFlowSummary"
                  readonly
                  placeholder="点击录入现金流量明细"
                  :disabled="isReadonlyMode"
                  @focus="handleCashFlowFieldFocus"
                />
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

    <el-dialog v-model="cashFlowDialogVisible" title="现金流量明细" width="1080px" destroy-on-close :close-on-click-modal="false" :close-on-press-escape="false">
      <div class="cash-flow-editor">
        <p class="cash-flow-editor__hint">当前窗口会自动识别凭证中的现金流量科目和金额，你也可以在这里修改；保存前系统会校验是否与凭证分录保持一致。</p>
        <div v-if="cashFlowEditorLines.length" class="cash-flow-editor__list">
          <section
            v-for="line in cashFlowEditorLines"
            :key="line.row.localId"
            class="cash-flow-editor__card"
            :class="{ 'cash-flow-editor__card-active': cashFlowDialogRowIndex === line.rowIndex }"
          >
            <header class="cash-flow-editor__header">
              <span>{{ line.rowLabel }}</span>
              <span class="cash-flow-editor__voucher-subject">{{ resolveAccountLabel(line.row.ccode, line.row.ccodeName) || '未选择科目' }}</span>
            </header>
            <div class="cash-flow-editor__row">
              <label class="cash-flow-editor__field">
                <span>现金流量科目</span>
                <finance-assist-option-select
                  :model-value="line.row.cashFlowSubjectCode"
                  :options="cashAccountOptions"
                  clearable
                  placeholder="请选择现金流量科目"
                  :disabled="isReadonlyMode"
                  @update:model-value="handleCashFlowSubjectChange(line.rowIndex, $event)"
                />
              </label>
              <label class="cash-flow-editor__field">
                <span>金额</span>
                <money-input
                  :model-value="line.row.cashFlowAmount || ''"
                  placeholder="0.00"
                  :readonly="isReadonlyMode"
                  :disabled="isReadonlyMode"
                  @update:modelValue="handleCashFlowAmountChange(line.rowIndex, $event)"
                />
              </label>
              <label class="cash-flow-editor__field">
                <span>现金流量项目</span>
                <finance-assist-option-select
                  :model-value="resolveCashFlowItemSelectValue(line.row.cashFlowItemId)"
                  :options="cashFlowOptionsForDisplay"
                  clearable
                  placeholder="请选择现金流量项目"
                  :disabled="isReadonlyMode"
                  @update:model-value="handleCashFlowItemChange(line.rowIndex, $event)"
                />
              </label>
            </div>
            <div v-if="line.subjectMismatch || line.amountMismatch" class="cash-flow-editor__warning">
              <span v-if="line.subjectMismatch">现金流量科目需与凭证分录科目一致。</span>
              <span v-if="line.amountMismatch">现金流量金额需与凭证分录金额一致。</span>
            </div>
          </section>
        </div>
        <el-empty v-else description="当前凭证暂无需要录入的现金流量明细" />
      </div>
      <template #footer>
        <el-button @click="closeCashFlowDialog">取消</el-button>
        <el-button type="primary" @click="handleCashFlowDialogConfirm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="calculatorDialogVisible" title="计算器" width="360px" destroy-on-close>
      <div class="calculator-panel">
        <el-input
          v-model="calculatorExpression"
          readonly
          placeholder="请输入算式，例如 100+20/2"
        />
        <div class="calculator-shortcuts">
          <el-button v-for="token in calculatorShortcutTokens" :key="token" @click="appendCalculatorToken(token)">
            {{ token }}
          </el-button>
        </div>
        <div class="calculator-result">结果：{{ calculatorResultText }}</div>
        <p class="calculator-hint">
          {{ activeAmountTargetLabel }}
        </p>
      </div>
      <template #footer>
        <el-button @click="clearCalculator">清空</el-button>
        <el-button @click="runCalculator">计算</el-button>
        <el-button type="primary" @click="applyCalculatorResult">带回金额框</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="deleteConfirmVisible"
      title="删除分录"
      width="360px"
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="action-dialog-content">
        <p>确认删除当前分录吗？</p>
      </div>
      <template #footer>
        <el-button @click="cancelRemoveSelectedEntry">否</el-button>
        <el-button ref="deleteConfirmPrimaryButtonRef" type="primary" @click="confirmRemoveSelectedEntry">是</el-button>
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
import {
  computed,
  nextTick,
  onActivated,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  reactive,
  ref,
  watch
} from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
  type FinanceVoucherForm,
  type FinanceVoucherMeta,
  type FinanceVoucherOption,
  type FinanceVoucherSavePayload,
  type FinanceVoucherSummary
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
import { useFinanceWorkspaceStore } from '@/stores/financeWorkspace'
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
  type FinanceVoucherEntryRow as VoucherEntryRow,
  type FinanceVoucherGridField
} from './composables/useFinanceNewVoucherRowOwner'
import { useFinanceNewVoucherValidationPayload } from './composables/useFinanceNewVoucherValidationPayload'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { formatMoney } from '@/utils/money'
import {
  hasVoucherFieldAmount,
  normalizeSignedVoucherMoney,
  resolveOppositeVoucherAmountField,
  resolveVoucherAutoBalanceValue,
  toggleVoucherAmountDirection
} from '@/utils/financeVoucherAmounts'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import { formatFinanceAssistOptionLabel } from '@/utils/financeAssistOptions'
import { formatFinancePeriodMonthEnd } from '@/utils/financeVoucherPeriods'


type ToolbarActionKey = FinanceNewVoucherToolbarActionKey
type VoucherFormState = Omit<FinanceVoucherForm, 'entries'> & { entries: VoucherEntryRow[] }
type VoucherPageMode = 'create' | 'detail' | 'review'

interface ToolbarAction {
  key: ToolbarActionKey
  label: string
  icon: Component
  shortcut?: string
  emphasis?: 'primary' | 'secondary'
  disabled?: boolean
}

type GridFocusableInstance = {
  focus?: () => void
  syncFromModel?: () => void
  $el?: Element | null
} | Element | null

type SavedVoucherSuggestion = FinanceVoucherSummary & {
  sequenceNo: number
  value: string
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
const route = useRoute()
const router = useRouter()
const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()
const financeWorkspace = useFinanceWorkspaceStore()
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
const calculatorDialogVisible = ref(false)
const calculatorExpression = ref('')
const calculatorResultText = ref('0.00')
const deleteConfirmVisible = ref(false)
const activeAmountTarget = ref<{ rowIndex: number; field: 'md' | 'mc' } | null>(null)
const voucherNoSelectorRef = ref<HTMLElement | null>(null)
const savedVoucherSuggestions = ref<SavedVoucherSuggestion[]>([])
const savedVoucherSuggestionsLoading = ref(false)
const savedVoucherDropdownVisible = ref(false)
const savedVoucherFilterKeyword = ref('')
const voucherNoInputText = ref('')
const voucherNoSwitchSnapshot = ref<{ text: string; inoId?: number; filterKeyword: string }>({ text: '', inoId: undefined, filterKeyword: '' })
const customerArchiveDialogRef = ref<InstanceType<typeof FinanceCustomerArchiveDialog> | null>(null)
const supplierArchiveDialogRef = ref<InstanceType<typeof FinanceSupplierArchiveDialog> | null>(null)
const projectArchiveDialogRef = ref<InstanceType<typeof FinanceProjectArchiveDialog> | null>(null)
const deleteConfirmPrimaryButtonRef = ref<{ $el?: Element | null; focus?: () => void } | HTMLElement | null>(null)
const gridCellRefs = new Map<string, GridFocusableInstance>()
const calculatorShortcutTokens = ['7', '8', '9', '+', '4', '5', '6', '-', '1', '2', '3', '*', '0', '.', '(', ')', '/']
const workspaceTabPath = String(route.fullPath || '')

let hotkeysRegistered = false

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

  const primaryActions: ToolbarAction[] = [{ key: 'new', label: '新增', icon: Plus, emphasis: 'secondary', shortcut: 'F5' }]
  if (isDetailRoute.value && !editingExisting.value && voucherDetail.value?.editable && canEditExisting.value) {
    primaryActions.push({ key: 'modify', label: '修改', icon: Edit, emphasis: 'primary' })
  }

  const editActions: ToolbarAction[] = [
    { key: 'print', label: '打印', icon: Printer, shortcut: 'Ctrl+P' },
    { key: 'export', label: '导出', icon: Download },
    { key: 'copy', label: '复制', icon: DocumentCopy, shortcut: 'Ctrl+F' },
    { key: 'reverse', label: '冲销', icon: RefreshLeft },
    { key: 'void', label: '作废', icon: CircleClose }
  ]
  if (!isReadonlyMode.value) {
    editActions.push({ key: 'insert', label: '插入行', icon: Top })
    editActions.push({ key: 'delete', label: '删行', icon: Delete, shortcut: 'Ctrl+D' })
  }

  const actionGroup: ToolbarAction[] = [
    { key: 'searchReplace', label: '查找替换', icon: Search },
    { key: 'cashFlow', label: '现金流量', icon: TrendCharts }
  ]
  if (!isReadonlyMode.value) {
    actionGroup.push({ key: 'save', label: '保存', icon: Select, emphasis: 'primary', shortcut: 'F6' })
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
        { key: 'calculator', label: '计算器', icon: Tools, shortcut: 'F9' }
      ]
    }
  ]
})

const selectedRow = computed(() => form.entries[Math.min(selectedRowIndex.value, Math.max(form.entries.length - 1, 0))] as VoucherEntryRow)
const hasUnsavedChanges = computed(() => Boolean(voucherMeta.value) && buildSnapshot() !== lastCommittedSnapshot.value)
const defaultCurrencyCode = computed(() => voucherMeta.value?.defaultCurrencyCode || voucherMeta.value?.defaultCurrency || 'CNY')
const defaultCurrencyName = computed(() => voucherMeta.value?.defaultCurrencyName || '人民币')
const activeAmountTargetLabel = computed(() => {
  const target = activeAmountTarget.value
  if (!target) {
    return '当前未聚焦借方或贷方金额框，计算结果可先保留。'
  }
  return `当前将带回第 ${target.rowIndex + 1} 行${target.field === 'md' ? '借方金额' : '贷方金额'}`
})
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
  financePeriod,
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
  cashFlowDialogRowIndex,
  cashFlowEditorLines,
  cashAccountOptions,
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
  handleAmountValueChange,
  handleAmountBlur,
  handleCashFlowSubjectChange,
  handleCashFlowAmountChange,
  handleCashFlowItemChange,
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
  updateAmountField,
  insertEntryAfter,
  removeSelectedEntry: removeSelectedEntryImmediately,
  handleGridKeydown,
  handleGridCellKeydown,
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
  resetLeafSubjectHistory,
  focusGridCell
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
  removeSelectedEntry: requestRemoveSelectedEntry,
  copyCurrentVoucher,
  openCalculator,
  printCurrentVoucher
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
const selectedRowCashFlowSummary = computed(() => {
  if (requiresRowCashFlow(selectedRow.value)) {
    return selectedRow.value.cashFlowItemName || '点击录入现金流量明细'
  }
  return cashFlowEditorLines.value.length
    ? `已录入 ${cashFlowEditorLines.value.length} 条现金流量明细`
    : ''
})
const cashFlowOptionsForDisplay = computed(() => {
  const options = [...(voucherMeta.value?.cashFlowOptions || [])]
  const existingValues = new Set(options.map((item) => String(item.value)))
  form.entries.forEach((row) => {
    if (row.cashFlowItemId === undefined || row.cashFlowItemId === null) {
      return
    }
    const value = String(row.cashFlowItemId)
    if (existingValues.has(value)) {
      return
    }
    options.push({
      value,
      name: row.cashFlowItemName || value,
      label: row.cashFlowItemName || value
    })
    existingValues.add(value)
  })
  return options
})
const savedVoucherBillMonth = computed(() => String(form.dbillDate || '').slice(0, 7))
const filteredSavedVoucherSuggestions = computed(() => {
  const keyword = String(savedVoucherFilterKeyword.value || '').trim().toLowerCase()
  if (!keyword) {
    return savedVoucherSuggestions.value
  }
  return savedVoucherSuggestions.value.filter((item) => {
    const inoIdText = String(item.sequenceNo || '')
    const displayVoucherNo = String(item.displayVoucherNo || '').toLowerCase()
    return inoIdText.includes(keyword) || displayVoucherNo.includes(keyword)
  })
})

watch(() => form.entries.length, () => {
  if (selectedRowIndex.value >= form.entries.length) {
    selectedRowIndex.value = Math.max(0, form.entries.length - 1)
  }
})

watch(voucherNoInput, (value) => {
  const nextText = String(value || '')
  if (voucherNoInputText.value !== nextText) {
    voucherNoInputText.value = nextText
  }
}, { immediate: true })

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

watch(
  () => [props.pageMode, financeCompany.currentCompanyId, savedVoucherBillMonth.value, form.csign] as const,
  async ([pageMode]) => {
    if (pageMode !== 'create') {
      savedVoucherSuggestions.value = []
      return
    }
    await loadSavedVoucherSuggestions()
  },
  { immediate: true }
)

watch(
  () => isReadonlyMode.value,
  (readonly) => {
    if (!workspaceTabPath) {
      return
    }
    if (readonly) {
      financeWorkspace.unregisterCloseGuard(workspaceTabPath)
      return
    }
    financeWorkspace.registerCloseGuard(workspaceTabPath, () => confirmDiscardCurrentVoucher('close'))
  },
  { immediate: true }
)

onMounted(registerPageHotkeys)
onActivated(registerPageHotkeys)
onDeactivated(unregisterPageHotkeys)
onMounted(() => document.addEventListener('mousedown', handleVoucherNoDropdownClickOutside))
onBeforeUnmount(() => {
  unregisterPageHotkeys()
  document.removeEventListener('mousedown', handleVoucherNoDropdownClickOutside)
  if (workspaceTabPath) {
    financeWorkspace.unregisterCloseGuard(workspaceTabPath)
  }
})

function setGridCellRef(rowId: string, field: FinanceVoucherGridField, instance: GridFocusableInstance) {
  const key = `${rowId}:${field}`
  if (!instance) {
    gridCellRefs.delete(key)
    return
  }
  gridCellRefs.set(key, instance)
}

function makeGridCellRefBinder(rowId: string, field: FinanceVoucherGridField) {
  return (instance: GridFocusableInstance) => setGridCellRef(rowId, field, instance)
}

async function focusGridCell(index: number, field: FinanceVoucherGridField) {
  await nextTick()
  const row = form.entries[index]
  if (!row) {
    return
  }
  const target = gridCellRefs.get(`${row.localId}:${field}`)
  if (target && typeof target === 'object' && 'focus' in target && typeof target.focus === 'function') {
    target.focus()
    return
  }
  const rootElement = target instanceof Element ? target : target && typeof target === 'object' && '$el' in target ? target.$el : null
  const focusable = rootElement instanceof HTMLElement
    ? rootElement.querySelector('input, select, [tabindex]') || rootElement
    : null
  if (focusable instanceof HTMLElement) {
    focusable.focus()
  }
}

async function syncGridCellDisplayFromModel(index: number, field: FinanceVoucherGridField) {
  await nextTick()
  const row = form.entries[index]
  if (!row) {
    return
  }
  const target = gridCellRefs.get(`${row.localId}:${field}`)
  if (target && typeof target === 'object' && 'syncFromModel' in target && typeof target.syncFromModel === 'function') {
    target.syncFromModel()
  }
}

async function syncVoucherAmountFieldsFromModel(index: number, ...fields: Array<'md' | 'mc'>) {
  for (const field of fields) {
    await syncGridCellDisplayFromModel(index, field)
  }
}

function toolbarActionTitle(action: ToolbarAction) {
  return action.shortcut ? `${action.label}（${action.shortcut}）` : action.label
}

function resolveVoucherSequenceNo(item: FinanceVoucherSummary) {
  const voucherNoParts = String(item.voucherNo || '').split('~')
  const rawPart = voucherNoParts[voucherNoParts.length - 1] || String(item.displayVoucherNo || '').replace(/\D/g, '')
  const sequenceNo = Number(rawPart || 0)
  return Number.isFinite(sequenceNo) ? sequenceNo : 0
}

function captureVoucherNoSwitchSnapshot() {
  voucherNoSwitchSnapshot.value = {
    text: voucherNoInputText.value,
    inoId: form.inoId,
    filterKeyword: savedVoucherFilterKeyword.value
  }
}

function handleVoucherNoDropdownClickOutside(event: MouseEvent) {
  if (!savedVoucherDropdownVisible.value) {
    return
  }
  const target = event.target
  if (!(target instanceof Node)) {
    return
  }
  if (voucherNoSelectorRef.value?.contains(target)) {
    return
  }
  savedVoucherDropdownVisible.value = false
}

function toggleSavedVoucherDropdown() {
  if (voucherHeaderLocked.value || props.pageMode !== 'create') {
    return
  }
  captureVoucherNoSwitchSnapshot()
  savedVoucherDropdownVisible.value = !savedVoucherDropdownVisible.value
}

function handleVoucherNoInput(value: string | number) {
  const nextText = String(value || '')
  voucherNoInputText.value = nextText
  voucherNoInput.value = nextText
  savedVoucherFilterKeyword.value = nextText
  voucherNoSwitchSnapshot.value = {
    text: nextText,
    inoId: form.inoId,
    filterKeyword: nextText
  }
}

async function loadSavedVoucherSuggestions() {
  if (props.pageMode !== 'create') {
    savedVoucherSuggestions.value = []
    return
  }
  const companyId = financeCompany.currentCompanyId || form.companyId
  const billMonth = savedVoucherBillMonth.value
  if (!companyId || !billMonth || !form.csign) {
    savedVoucherSuggestions.value = []
    return
  }
  savedVoucherSuggestionsLoading.value = true
  try {
    const res = await financeApi.listVouchers({
      companyId,
      billMonth,
      csign: form.csign,
      page: 1,
      pageSize: 500
    })
    savedVoucherSuggestions.value = [...(res.data.items || [])]
      .map((item) => {
        const sequenceNo = resolveVoucherSequenceNo(item)
        return {
          ...item,
          sequenceNo,
          value: String(sequenceNo || '')
        }
      })
      .sort((left, right) => left.sequenceNo - right.sequenceNo)
  } catch (error: unknown) {
    savedVoucherSuggestions.value = []
    ElMessage.error(resolveErrorMessage(error, '加载当月凭证编号失败'))
  } finally {
    savedVoucherSuggestionsLoading.value = false
  }
}

function restoreVoucherNoSwitchSnapshot() {
  savedVoucherDropdownVisible.value = false
  voucherNoInputText.value = voucherNoSwitchSnapshot.value.text || ''
  savedVoucherFilterKeyword.value = voucherNoSwitchSnapshot.value.filterKeyword || ''
  form.inoId = voucherNoSwitchSnapshot.value.inoId
}

async function confirmDiscardCurrentVoucher(reason: 'switch' | 'close') {
  if (!hasUnsavedChanges.value) {
    return true
  }
  const config = reason === 'switch'
    ? {
        title: '切换凭证',
        message: '当前凭证未保存，切换后当前录入将丢失，确认切换吗'
      }
    : {
        title: '关闭凭证',
        message: '当前凭证未保存，关闭后当前录入将丢失，确认关闭吗'
      }
  try {
    await ElMessageBox.confirm(config.message, config.title, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    if (props.pageMode === 'create') {
      clearDraft(financeCompany.currentCompanyId || form.companyId)
    }
    return true
  } catch {
    return false
  }
}

async function handleSavedVoucherSelect(item: SavedVoucherSuggestion) {
  if (props.pageMode !== 'create' || !item?.voucherNo) {
    return
  }
  savedVoucherDropdownVisible.value = false
  const allowed = await confirmDiscardCurrentVoucher('switch')
  if (!allowed) {
    restoreVoucherNoSwitchSnapshot()
    return
  }
  clearDraft(financeCompany.currentCompanyId || form.companyId)
  validationErrors.value = []
  const targetLocation = router.resolve({
    name: 'finance-query-voucher-detail',
    params: { voucherNo: item.voucherNo }
  })
  financeWorkspace.replaceTabPath(workspaceTabPath, targetLocation.fullPath, '凭证详情')
  await router.replace({
    name: 'finance-query-voucher-detail',
    params: { voucherNo: item.voucherNo }
  })
}

function isToolbarActionEnabled(actionKey: ToolbarActionKey) {
  return toolbarGroups.value
    .flatMap((group) => group.actions)
    .some((action) => action.key === actionKey && !action.disabled)
}

function focusButtonLike(target: { $el?: Element | null; focus?: () => void } | HTMLElement | null | undefined) {
  if (!target) {
    return
  }
  if (target instanceof HTMLElement) {
    target.focus()
    return
  }
  if (typeof target.focus === 'function') {
    target.focus()
    return
  }
  const host = target.$el
  if (!(host instanceof Element)) {
    return
  }
  const button = host.matches('button') ? host : host.querySelector('button')
  if (button instanceof HTMLElement) {
    button.focus()
  }
}

function blockToolbarShortcutsWhileOverlayOpen(event: KeyboardEvent) {
  return Boolean(resolveShortcutAction(event))
}

function resolveShortcutAction(event: KeyboardEvent): ToolbarActionKey | null {
  if (event.metaKey || event.altKey) {
    return null
  }
  if (event.ctrlKey) {
    const normalizedKey = event.key.toLowerCase()
    if (normalizedKey === 'p') return 'print'
    if (normalizedKey === 'f') return 'copy'
    if (normalizedKey === 'd') return 'delete'
    return null
  }
  const normalizedKey = event.key.toUpperCase()
  if (normalizedKey === 'F5') return 'new'
  if (normalizedKey === 'F6') return 'save'
  if (normalizedKey === 'F9') return 'calculator'
  return null
}

function handleCalculatorDialogKeydown(event: KeyboardEvent) {
  if (!calculatorDialogVisible.value || event.metaKey || event.ctrlKey || event.altKey) {
    return false
  }
  if (/^[0-9+\-*/().]$/.test(event.key)) {
    event.preventDefault()
    calculatorExpression.value += event.key
    return true
  }
  if (event.key === 'Backspace') {
    event.preventDefault()
    calculatorExpression.value = calculatorExpression.value.slice(0, -1)
    return true
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    runCalculator()
    return true
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    calculatorDialogVisible.value = false
    return true
  }
  return false
}

function handleDeleteConfirmKeydown(event: KeyboardEvent) {
  if (!deleteConfirmVisible.value) {
    return false
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    void confirmRemoveSelectedEntry()
    return true
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    cancelRemoveSelectedEntry()
    return true
  }
  return false
}

function handlePageHotkey(event: KeyboardEvent) {
  if (handleDeleteConfirmKeydown(event)) {
    return
  }
  if (deleteConfirmVisible.value) {
    if (blockToolbarShortcutsWhileOverlayOpen(event)) {
      event.preventDefault()
    }
    return
  }
  if (handleCalculatorDialogKeydown(event)) {
    return
  }
  if (calculatorDialogVisible.value) {
    if (blockToolbarShortcutsWhileOverlayOpen(event)) {
      event.preventDefault()
    }
    return
  }
  const action = resolveShortcutAction(event)
  if (!action || !isToolbarActionEnabled(action)) {
    return
  }
  event.preventDefault()
  void handleToolbarAction(action)
}

function registerPageHotkeys() {
  if (hotkeysRegistered) {
    return
  }
  window.addEventListener('keydown', handlePageHotkey)
  hotkeysRegistered = true
}

function unregisterPageHotkeys() {
  if (!hotkeysRegistered) {
    return
  }
  window.removeEventListener('keydown', handlePageHotkey)
  hotkeysRegistered = false
}

function handleAmountFieldFocus(index: number, field: 'md' | 'mc') {
  activeAmountTarget.value = { rowIndex: index, field }
  handleEntryFieldFocus(index)
}

function handleAmountFieldUpdate(index: number, field: 'md' | 'mc', value: string) {
  updateAmountField(index, field, value)
  handleAmountValueChange(index)
}

function handleCashFlowDialogConfirm() {
  if (!confirmCashFlowSelection()) {
    ElMessage.warning('请先为每条需要现金流量的分录选择现金流量项目')
    return
  }
  closeCashFlowDialog()
}

function requestRemoveSelectedEntry() {
  if (isReadonlyMode.value) {
    return
  }
  deleteConfirmVisible.value = true
  void nextTick(() => {
    focusButtonLike(deleteConfirmPrimaryButtonRef.value)
  })
}

function cancelRemoveSelectedEntry() {
  deleteConfirmVisible.value = false
}

async function confirmRemoveSelectedEntry() {
  deleteConfirmVisible.value = false
  removeSelectedEntryImmediately()
  await nextTick()
}

async function handleVoucherAmountKeydown(event: KeyboardEvent, index: number, field: 'md' | 'mc') {
  if (event.key === '=') {
    event.preventDefault()
    const value = resolveVoucherAutoBalanceValue(form.entries, index, field)
    updateAmountField(index, field, normalizeSignedVoucherMoney(value) || '0.00')
    await syncGridCellDisplayFromModel(index, field)
    return
  }
  if (event.key === ' ' || event.code === 'Space') {
    event.preventDefault()
    const row = form.entries[index]
    if (!row) {
      return
    }
    const currentValue = row[field]
    const nextField = resolveOppositeVoucherAmountField(field)
    if (!hasVoucherFieldAmount(field, currentValue)) {
      selectRow(index)
      await focusGridCell(index, nextField)
      return
    }
    const toggled = toggleVoucherAmountDirection(field, currentValue)
    row.md = toggled.md
    row.mc = toggled.mc
    await syncVoucherAmountFieldsFromModel(index, field, toggled.nextField)
    selectRow(index)
    await focusGridCell(index, toggled.nextField)
    return
  }
  await handleAmountKeydown(event, index, field)
}

function openCalculator() {
  calculatorDialogVisible.value = true
}

function printCurrentVoucher() {
  window.print()
}

function appendCalculatorToken(token: string) {
  calculatorExpression.value += token
}

function clearCalculator() {
  calculatorExpression.value = ''
  calculatorResultText.value = '0.00'
}

function runCalculator() {
  calculatorResultText.value = evaluateCalculatorExpression(calculatorExpression.value)
  calculatorExpression.value = calculatorResultText.value
}

async function applyCalculatorResult() {
  const target = activeAmountTarget.value
  if (!target) {
    ElMessage.warning('请先聚焦借方或贷方金额框')
    return
  }
  const result = evaluateCalculatorExpression(calculatorExpression.value)
  calculatorResultText.value = result
  updateAmountField(target.rowIndex, target.field, result)
  calculatorDialogVisible.value = false
  selectRow(target.rowIndex)
  await focusGridCell(target.rowIndex, target.field)
}

async function copyCurrentVoucher() {
  const companyId = financeCompany.currentCompanyId || form.companyId
  const payload = buildPayload()
  const copiedDraft: FinanceVoucherSavePayload = {
    ...payload,
    companyId,
    inoId: undefined,
    cbill: voucherMeta.value?.defaultMaker || form.cbill,
    entries: payload.entries.map((entry, index) => ({
      ...entry,
      inid: index + 1
    }))
  }
  writeDraft(copiedDraft, companyId)
  if (props.pageMode !== 'create') {
    await router.push({ name: 'finance-new-voucher' })
    return
  }
  if (!voucherMeta.value) {
    return
  }
  applyDraft(copiedDraft, voucherMeta.value, companyId)
  validationErrors.value = []
  markCommitted()
}

function evaluateCalculatorExpression(expression: string) {
  const normalized = String(expression || '').trim()
  if (!normalized) {
    return '0.00'
  }
  if (!/^[\d+\-*/().\s]+$/.test(normalized)) {
    throw new Error('计算器仅支持数字、小数点、括号和四则运算符')
  }
  const result = Function(`"use strict"; return (${normalized})`)()
  if (typeof result !== 'number' || !Number.isFinite(result)) {
    throw new Error('计算结果无效，请检查算式')
  }
  return normalizeSignedVoucherMoney(String(result)) || '0.00'
}

function syncFormPeriodFromGlobal() {
  if (!financePeriod.hasPeriodContext) {
    return false
  }
  form.iyear = financePeriod.currentYear
  form.iperiod = financePeriod.currentPeriod
  form.iyperiod = financePeriod.currentYearPeriod
  return true
}

function resolveCreateDefaultBillDate(meta: FinanceVoucherMeta) {
  return formatFinancePeriodMonthEnd(financePeriod.currentYear, financePeriod.currentPeriod) || meta.defaultBillDate
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
  form.dbillDate = financePeriod.hasPeriodContext ? resolveCreateDefaultBillDate(meta) : meta.defaultBillDate
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
  form.dbillDate = draft.dbillDate || (financePeriod.hasPeriodContext ? resolveCreateDefaultBillDate(meta) : meta.defaultBillDate)
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

function writeDraft(draft: FinanceVoucherSavePayload, companyId = financeCompany.currentCompanyId || draft.companyId) {
  window.sessionStorage.setItem(buildDraftStorageKey(companyId), JSON.stringify(draft))
  hasDraft.value = true
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

function resolveCashFlowItemSelectValue(value?: number | null) {
  return value === undefined || value === null ? undefined : String(value)
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
.voucher-no-selector { position: relative; display: grid; grid-template-columns: minmax(0,1fr) 34px; align-items: stretch; min-width: 0; }
.voucher-no-selector :deep(.el-input) { min-width: 0; }
.voucher-no-selector :deep(.el-input__wrapper) { border-top-right-radius: 0; border-bottom-right-radius: 0; }
.voucher-no-selector-open :deep(.el-input__wrapper) { border-color: #8fb4ea; box-shadow: 0 0 0 1px rgba(79,138,216,.14) inset; }
.voucher-no-trigger { display: inline-flex; align-items: center; justify-content: center; border: 1px solid #c6daf6; border-left: 0; border-radius: 0 12px 12px 0; background: linear-gradient(180deg, #eef7ff 0%, #dcecff 100%); color: #386393; cursor: pointer; transition: background-color .16s ease, border-color .16s ease, color .16s ease; }
.voucher-no-trigger:hover:not(:disabled) { border-color: #9fc0eb; background: linear-gradient(180deg, #e7f2ff 0%, #d4e6ff 100%); color: #24466f; }
.voucher-no-trigger:disabled { cursor: not-allowed; opacity: .7; }
.voucher-no-trigger__caret { width: 0; height: 0; border-left: 5px solid transparent; border-right: 5px solid transparent; border-top: 6px solid currentColor; }
.voucher-no-dropdown { position: absolute; left: 0; right: 0; top: calc(100% + 6px); z-index: 16; overflow: hidden; border-radius: 14px; border: 1px solid #d6e2f3; background: #fff; box-shadow: 0 18px 32px rgba(15,23,42,.12); }
.voucher-no-dropdown__state { padding: 14px 16px; color: #7085a0; font-size: 13px; line-height: 1.6; }
.voucher-no-dropdown__list { max-height: 390px; overflow: auto; }
.voucher-no-option { display: grid; grid-template-columns: minmax(84px,auto) minmax(92px,auto) minmax(0,1fr); align-items: center; gap: 8px; width: 100%; border: 0; border-top: 1px solid #edf2f8; background: #fff; padding: 10px 14px; text-align: left; cursor: pointer; }
.voucher-no-option:first-child { border-top: 0; }
.voucher-no-option:hover { background: linear-gradient(180deg, #f6faff 0%, #edf5ff 100%); }
.voucher-no-suggestion { display: grid; grid-template-columns: minmax(84px,auto) minmax(92px,auto) minmax(0,1fr); align-items: center; gap: 8px; width: 100%; }
.voucher-no-suggestion__code { color: #24466f; font-weight: 700; }
.voucher-no-suggestion__meta { color: #7287a2; font-size: 12px; }
.voucher-no-suggestion__summary { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #5f7391; font-size: 12px; }
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
.calculator-panel { display: flex; flex-direction: column; gap: 12px; }
.calculator-shortcuts { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 8px; }
.calculator-shortcuts :deep(.el-button) { margin-left: 0; }
.calculator-result { border-radius: 12px; border: 1px solid #d8e2f0; background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%); padding: 10px 12px; color: #21466d; font-family: Consolas, Monaco, monospace; font-weight: 700; text-align: right; }
.calculator-hint { margin: 0; color: #7b8ea7; font-size: 12px; line-height: 1.6; }
.action-dialog-content { color: #506680; line-height: 1.8; }
.action-dialog-subtle { margin-top: 8px; color: #8a9bb1; font-size: 12px; }
.cash-flow-editor { display: flex; flex-direction: column; gap: 12px; }
.cash-flow-editor__hint { margin: 0; color: #5f7391; line-height: 1.7; }
.cash-flow-editor__list { display: flex; max-height: 56vh; flex-direction: column; gap: 12px; overflow: auto; padding-right: 4px; }
.cash-flow-editor__card { border-radius: 16px; border: 1px solid #d8e2f0; background: linear-gradient(180deg, #f9fbff 0%, #f3f7fd 100%); padding: 12px; }
.cash-flow-editor__card-active { border-color: #9cbbe3; box-shadow: 0 0 0 1px rgba(79,138,216,.18); }
.cash-flow-editor__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; color: #365070; font-size: 13px; font-weight: 700; }
.cash-flow-editor__voucher-subject { color: #6a7f99; font-weight: 600; }
.cash-flow-editor__row { display: grid; grid-template-columns: minmax(240px, 1.1fr) minmax(160px, .7fr) minmax(320px, 1.2fr); gap: 10px 12px; }
.cash-flow-editor__field { display: flex; min-width: 0; flex-direction: column; gap: 6px; color: #5f7391; font-size: 12px; font-weight: 600; }
.cash-flow-editor__warning { display: flex; gap: 14px; margin-top: 8px; color: #c2410c; font-size: 12px; font-weight: 600; line-height: 1.6; }
:deep(.voucher-info-field .el-input__wrapper), :deep(.voucher-info-field .el-select__wrapper), :deep(.voucher-info-field .el-date-editor), :deep(.assist-field .el-input__wrapper), :deep(.assist-field .el-select__wrapper), :deep(.voucher-cell .el-input__wrapper), :deep(.voucher-cell .el-select__wrapper) { border-radius: 12px; box-shadow: 0 0 0 1px #d8e2f0 inset; }
:deep(.voucher-cell .el-input-number), :deep(.voucher-cell .el-input-number .el-input__wrapper), :deep(.assist-field .el-input-number), :deep(.assist-field .el-input-number .el-input__wrapper), :deep(.voucher-info-field .el-input-number), :deep(.voucher-info-field .el-input-number .el-input__wrapper) { width: 100%; }
:deep(.voucher-info-field .el-input__wrapper), :deep(.voucher-info-field .el-select__wrapper), :deep(.voucher-info-field .el-date-editor), :deep(.voucher-info-field .el-input-number .el-input__wrapper), :deep(.assist-field .el-input__wrapper), :deep(.assist-field .el-select__wrapper), :deep(.assist-field .el-input-number .el-input__wrapper) { min-height: 34px; }
:deep(.voucher-number-group .el-select__wrapper), :deep(.voucher-number-group .el-input__wrapper) { min-height: 34px; }
:deep(.voucher-cell .el-input__wrapper), :deep(.voucher-cell .el-select__wrapper), :deep(.voucher-cell .money-input__control), :deep(.voucher-cell .el-input-number .el-input__wrapper) { min-height: 32px; }
:deep(.cash-flow-editor__field .el-input__wrapper), :deep(.cash-flow-editor__field .el-select__wrapper), :deep(.cash-flow-editor__field .money-input__control) { min-height: 34px; border-radius: 12px; box-shadow: 0 0 0 1px #d8e2f0 inset; }
@media (max-width: 1440px) { .voucher-lower { grid-template-columns: 1fr; } }
@media (max-width: 1024px) { .voucher-info-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } .assist-grid { grid-template-columns: repeat(6, minmax(0,1fr)); } .assist-field { grid-column: span 3; } .voucher-info-spacer { display: none; } .voucher-grid-layout { min-width: 860px; } .cash-flow-editor__row { grid-template-columns: 1fr; } }
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
  .cash-flow-editor__header { flex-direction: column; align-items: flex-start; }
}
</style>
