<template>
  <div class="flex items-start gap-6">
    <process-workbench-sidebar
      :items="navItems"
      active-key="document-flow"
      @select="handleSidebarSelect"
    />

    <div class="flex flex-1 items-start gap-6">
      <div class="min-w-0 flex-1 space-y-6">
        <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
                <el-icon><ArrowLeft /></el-icon>
                返回单据与流程
              </button>
              <h1 class="mt-3 text-3xl font-bold text-slate-800">
                新建{{ options?.templateTypeLabel || '单据' }}模板
              </h1>
              <p class="mt-3 max-w-3xl leading-7 text-slate-500">
                这里用于配置模板的基础信息、单据与流程、适用范围，以及标签设置和分期付款来源。
              </p>
            </div>

            <div class="flex gap-3">
              <el-button @click="goBack">取消</el-button>
              <el-button v-if="canCreate" type="primary" :loading="saving" @click="saveTemplate">
                保存模板
              </el-button>
            </div>
          </div>
        </section>

        <section
          v-if="saveBlockers.length"
          id="save-blockers"
          ref="saveBlockersRef"
          class="save-blockers-panel rounded-[28px] border border-amber-200 bg-amber-50 px-6 py-5 shadow-sm"
        >
          <div class="flex items-start gap-4">
            <div class="save-blockers-icon">!</div>
            <div class="min-w-0 flex-1 space-y-3">
              <div>
                <p class="text-base font-semibold text-amber-950">当前还不能保存模板</p>
                <p class="mt-1 text-sm text-amber-800">
                  下面这些问题处理完后，系统才会真正发起保存请求并把模板入库。
                </p>
              </div>
              <ul class="space-y-2 text-sm text-amber-900">
                <li v-for="blocker in saveBlockers" :key="blocker" class="save-blocker-item">
                  {{ blocker }}
                </li>
              </ul>
            </div>
          </div>
        </section>

        <el-form label-position="top" class="space-y-6">
          <el-card id="basic" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-semibold text-slate-800">基础设置</span>
                <el-tag type="primary" effect="plain">{{ options?.templateTypeLabel || '模板' }}</el-tag>
              </div>
            </template>

            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <el-form-item label="单据名称" required>
                <el-input
                  v-model="form.templateName"
                  :maxlength="PM_NAME_MAX_LENGTH"
                  show-word-limit
                  placeholder="请输入单据名称"
                />
              </el-form-item>

              <el-form-item label="所属分类" required>
                <el-select v-model="form.category" placeholder="请选择分类">
                  <el-option
                    v-for="item in options?.categoryOptions || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="单据说明" class="xl:col-span-2">
                <el-input
                  v-model="form.templateDescription"
                  type="textarea"
                  :rows="3"
                  placeholder="建议说明单据适用场景和维护说明"
                />
              </el-form-item>

              <el-form-item label="编码规则">
                <div class="rule-preview">
                  <span>{{ options?.numberingRulePreview || defaultNumberingRulePreview }}</span>
                </div>
              </el-form-item>

              <el-form-item label="启用状态">
                <el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="停用" />
              </el-form-item>
            </div>
          </el-card>

          <el-card id="flow" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">单据与流程</span>
            </template>

            <div class="grid grid-cols-1 gap-5 xl:grid-cols-2">
              <el-form-item label="表单设计">
                <button type="button" class="selection-trigger" @click="openOptionDialog('formDesign')">
                  {{ singleOptionLabel('formDesign') || '请选择表单设计' }}
                </button>
              </el-form-item>

              <el-form-item label="审批流程">
                <button type="button" class="selection-trigger" @click="openOptionDialog('approvalFlow')">
                  {{ singleOptionLabel('approvalFlow') || '请选择审批流程' }}
                </button>
              </el-form-item>

              <el-form-item v-if="isReportTemplate" label="费用明细表单" required>
                <button type="button" class="selection-trigger" @click="openOptionDialog('expenseDetailDesign')">
                  {{ singleOptionLabel('expenseDetailDesign') || '请选择费用明细表单' }}
                </button>
              </el-form-item>

              <el-form-item
                v-if="isReportTemplate && selectedExpenseDetailType === 'ENTERPRISE_TRANSACTION'"
                label="企业往来默认模式"
                required
              >
                <el-select v-model="form.expenseDetailModeDefault" placeholder="请选择企业往来默认模式">
                  <el-option
                    v-for="item in options?.expenseDetailModeOptions || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="表单打印">
                <button type="button" class="selection-trigger" @click="openOptionDialog('printMode')">
                  {{ singleOptionLabel('printMode') || '请选择表单打印' }}
                </button>
              </el-form-item>

              <el-form-item label="付款单设置">
                <button type="button" class="selection-trigger" @click="openOptionDialog('paymentMode')">
                  {{ singleOptionLabel('paymentMode') || '请选择付款单设置' }}
                </button>
              </el-form-item>

              <el-form-item label="分摊表单">
                <button type="button" class="selection-trigger" @click="openOptionDialog('allocationForm')">
                  {{ singleOptionLabel('allocationForm') || '请选择分摊表单' }}
                </button>
              </el-form-item>
            </div>
          </el-card>

          <el-card id="scope" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">规则与适用范围</span>
            </template>

            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <el-form-item label="限定部门使用">
                <button
                  type="button"
                  class="selection-trigger selection-trigger-subtle"
                  @click="openDepartmentDialog"
                >
                  {{ form.scopeDeptIds.length ? `已选择 ${form.scopeDeptIds.length} 个部门` : '点击选择部门' }}
                </button>
                <div v-if="selectedDepartments.length" class="selection-tags">
                  <el-tag v-for="item in selectedDepartments" :key="item.value" effect="plain">
                    {{ item.label }}
                  </el-tag>
                </div>
              </el-form-item>

              <el-form-item label="限定费用类型使用">
                <button
                  type="button"
                  class="selection-trigger selection-trigger-subtle"
                  @click="openExpenseTypeDialog"
                >
                  {{
                    form.scopeExpenseTypeCodes.length
                      ? `已选择 ${form.scopeExpenseTypeCodes.length} 个费用类型`
                      : '点击选择费用类型'
                  }}
                </button>
                <div v-if="selectedExpenseTypes.length" class="selection-tags">
                  <el-tag v-for="item in selectedExpenseTypes" :key="item.value" effect="plain">
                    {{ item.label }}
                  </el-tag>
                </div>
              </el-form-item>

              <el-form-item label="AI 审核策略">
                <el-select v-model="form.aiAuditMode" placeholder="请选择 AI 审核策略">
                  <el-option
                    v-for="item in options?.aiAuditModes || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="限定金额">
                <div class="grid grid-cols-[minmax(0,1fr),36px,minmax(0,1fr)] items-center gap-3">
                  <money-input
                    v-model="form.amountMin"
                    class="w-full"
                    placeholder="最小值"
                  />
                  <span class="text-center text-slate-400">至</span>
                  <money-input
                    v-model="form.amountMax"
                    class="w-full"
                    placeholder="最大值"
                  />
                </div>
                <p class="mt-2 text-xs text-slate-400">默认不限定金额，留空即可。</p>
              </el-form-item>
            </div>
          </el-card>

          <el-card id="tag" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">标签设置</span>
            </template>

            <el-form-item label="标签档案">
              <el-select v-model="form.tagOption" clearable placeholder="请选择上级自定义档案">
                <el-option
                  v-for="item in options?.tagOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <p class="mt-3 text-xs text-slate-400">
                选择一个上级自定义档案作为该模板的标签来源。
              </p>
            </el-form-item>
          </el-card>

          <el-card id="installment" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">分期付款</span>
            </template>

            <el-form-item label="分期付款档案">
              <el-select v-model="form.installmentOption" clearable placeholder="请选择上级自定义档案">
                <el-option
                  v-for="item in options?.installmentOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <p class="mt-3 text-xs text-slate-400">
                选择一个上级自定义档案作为该模板的分期付款来源。
              </p>
            </el-form-item>
          </el-card>
        </el-form>

        <div
          class="sticky bottom-4 z-10 flex justify-end gap-3 rounded-2xl border border-slate-200 bg-white/90 px-6 py-4 shadow-sm backdrop-blur"
        >
          <el-button @click="goBack">取消</el-button>
          <el-button v-if="canCreate" type="primary" :loading="saving" @click="saveTemplate">
            保存模板
          </el-button>
        </div>
      </div>

      <aside class="sticky top-24 w-64 shrink-0">
        <el-card class="border border-slate-100 !rounded-3xl !shadow-sm">
          <div class="space-y-3">
            <div
              v-for="anchor in anchorSections"
              :key="anchor.id"
              class="anchor-link"
              @click="scrollToSection(anchor.id)"
            >
              {{ anchor.label }}
            </div>
          </div>
        </el-card>
      </aside>
    </div>

    <el-dialog v-model="optionDialog.visible" :title="optionDialog.title" width="720px">
      <div class="space-y-4">
        <button type="button" class="create-entry" @click="showCreateEntry">
          <el-icon><Plus /></el-icon>
          <span>{{ optionDialog.createLabel }}</span>
        </button>

        <div class="space-y-3">
          <button
            v-for="item in optionDialog.options"
            :key="item.value"
            type="button"
            class="option-row"
            :class="item.value === optionDialog.pendingValue ? 'option-row-active' : ''"
            @click="optionDialog.pendingValue = item.value"
          >
            <div class="min-w-0 text-left">
              <p class="truncate text-base font-semibold text-slate-800">{{ item.label }}</p>
            </div>
            <el-button text type="primary" @click.stop="showOptionEdit(item)">编辑</el-button>
          </button>

          <el-empty v-if="optionDialog.options.length === 0" description="暂无可选项" />
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="optionDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="confirmOptionDialog">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="departmentDialogVisible" title="限定部门使用" width="960px" destroy-on-close>
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr),320px]">
        <div class="rounded-3xl border border-slate-200 bg-slate-50 p-4">
          <el-input v-model="departmentKeyword" placeholder="搜索部门名称" clearable />
          <div class="mt-4 max-h-[420px] overflow-y-auto rounded-2xl bg-white p-3">
            <el-checkbox-group v-model="draftScopeDeptIds" class="flex flex-col gap-3">
              <el-checkbox v-for="item in filteredDepartmentOptions" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="filteredDepartmentOptions.length === 0" description="暂无匹配部门" />
          </div>
        </div>

        <div class="rounded-3xl border border-slate-200 bg-white p-4">
          <div class="flex items-center justify-between">
            <p class="font-semibold text-slate-800">已选部门</p>
            <span class="text-xs text-slate-400">{{ draftScopeDeptIds.length }} 项</span>
          </div>
          <div class="mt-4 space-y-3">
            <div v-for="item in draftSelectedDepartments" :key="item.value" class="selected-item">
              <span class="truncate">{{ item.label }}</span>
              <el-button text @click="removeDepartment(item.value)">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            <el-empty v-if="draftSelectedDepartments.length === 0" description="暂未选择部门" />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="departmentDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmDepartmentDialog">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="expenseTypeDialogVisible" title="限定费用类型使用" width="1080px" destroy-on-close>
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr),340px]">
        <div class="rounded-3xl border border-slate-200 bg-slate-50 p-4">
          <el-input v-model="expenseTypeKeyword" placeholder="搜索费用类型名称" clearable />
          <div class="mt-4 max-h-[440px] overflow-y-auto rounded-2xl bg-white p-3">
            <el-tree
              ref="expenseTypeTreeRef"
              :data="options?.expenseTypes || []"
              node-key="expenseCode"
              show-checkbox
              check-strictly
              default-expand-all
              :expand-on-click-node="false"
              :props="{ label: 'expenseName', children: 'children' }"
              :filter-node-method="filterExpenseTypeNode"
              @check="handleExpenseTypeCheck"
            >
              <template #default="{ data }">
                <div class="min-w-0 py-1">
                  <p class="truncate text-sm text-slate-800">{{ data.expenseName }}</p>
                </div>
              </template>
            </el-tree>
          </div>
        </div>

        <div class="rounded-3xl border border-slate-200 bg-white p-4">
          <div class="flex items-center justify-between">
            <p class="font-semibold text-slate-800">已选费用类型</p>
            <span class="text-xs text-slate-400">{{ draftScopeExpenseTypeCodes.length }} 项</span>
          </div>
          <div class="mt-4 space-y-3">
            <div v-for="item in draftSelectedExpenseTypes" :key="item.value" class="selected-item">
              <div class="min-w-0">
                <p class="truncate">{{ item.label }}</p>
              </div>
              <el-button text @click="removeExpenseType(item.value)">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            <el-empty v-if="draftSelectedExpenseTypes.length === 0" description="暂未选择费用类型" />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="expenseTypeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmExpenseTypeDialog">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Close, Plus } from '@element-plus/icons-vue'
import {
  processApi,
  type ProcessCenterNavItem,
  type ProcessExpenseDetailDesignSummary,
  type ProcessExpenseTypeTreeNode,
  type ProcessFormDesignSummary,
  type ProcessFlowSummary,
  type ProcessFormOption,
  type ProcessTemplateDetail,
  type ProcessTemplateFormOptions,
  type ProcessTemplateSavePayload
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { compareMoney } from '@/utils/money'
import ProcessWorkbenchSidebar from '@/components/process/ProcessWorkbenchSidebar.vue'
import {
  PM_NAME_MAX_LENGTH,
  collectTemplateBindingIssues,
  validateTemplateBindingValue
} from '@/views/process/pmValidation'

type SingleOptionField = 'formDesign' | 'expenseDetailDesign' | 'approvalFlow' | 'printMode' | 'paymentMode' | 'allocationForm'

type LabeledValue = {
  label: string
  value: string
}

type ExpenseTypeTreeInstance = {
  filter: (keyword: string) => void
  setCheckedKeys: (keys: string[]) => void
}

const TEMPLATE_DRAFT_PREFIX = 'process-template-create-draft:'
const DEFAULT_ENTERPRISE_TRANSACTION_MODE = 'PREPAY_UNBILLED'

const route = useRoute()
const router = useRouter()

const defaultNumberingRulePreview = 'FX+年+月+日+4位数字（如：FX202503251234）'

const navItems = ref<ProcessCenterNavItem[]>([])
const options = ref<ProcessTemplateFormOptions | null>(null)
const flowSummaries = ref<ProcessFlowSummary[]>([])
const formDesignSummaries = ref<ProcessFormDesignSummary[]>([])
const saving = ref(false)
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const saveBlockersRef = ref<HTMLElement | null>(null)

const templateType = computed(() => String(route.params.templateType || 'report'))
const templateId = computed(() => {
  const raw = route.query.templateId
  if (typeof raw !== 'string' || !raw.trim()) {
    return null
  }
  const id = Number(raw)
  return Number.isFinite(id) && id > 0 ? id : null
})
const isEditMode = computed(() => templateId.value !== null)
const isReportTemplate = computed(() => templateType.value === 'report')
const canCreate = computed(() =>
  isEditMode.value
    ? hasPermission('expense:process_management:edit', permissionCodes.value)
    : hasPermission('expense:process_management:create', permissionCodes.value)
)

const form = reactive<ProcessTemplateSavePayload>(createEmptyForm(templateType.value))

const anchorSections = [
  { id: 'basic', label: '基础设置' },
  { id: 'flow', label: '单据与流程' },
  { id: 'scope', label: '规则与适用范围' },
  { id: 'tag', label: '标签设置' },
  { id: 'installment', label: '分期付款' }
]

const optionDialog = reactive<{
  visible: boolean
  field: SingleOptionField
  title: string
  createLabel: string
  options: ProcessFormOption[]
  pendingValue: string
}>({
  visible: false,
  field: 'formDesign',
  title: '',
  createLabel: '',
  options: [],
  pendingValue: ''
})

const departmentDialogVisible = ref(false)
const departmentKeyword = ref('')
const draftScopeDeptIds = ref<string[]>([])

const expenseTypeDialogVisible = ref(false)
const expenseTypeKeyword = ref('')
const draftScopeExpenseTypeCodes = ref<string[]>([])
const expenseTypeTreeRef = ref<ExpenseTypeTreeInstance | null>(null)

const optionDialogConfig: Record<SingleOptionField, { title: string; createLabel: string }> = {
  formDesign: { title: '选择表单设计', createLabel: '新建表单设计' },
  expenseDetailDesign: { title: '选择费用明细表单', createLabel: '新建费用明细表单' },
  approvalFlow: { title: '选择审批流程', createLabel: '新建审批流程' },
  printMode: { title: '选择表单打印', createLabel: '新建表单打印' },
  paymentMode: { title: '选择付款单设置', createLabel: '新建付款单设置' },
  allocationForm: { title: '选择分摊表单', createLabel: '新建分摊表单' }
}

const filteredDepartmentOptions = computed(() => {
  const keyword = departmentKeyword.value.trim().toLowerCase()
  return (options.value?.departmentOptions || []).filter((item) => !keyword || item.label.toLowerCase().includes(keyword))
})

const selectedDepartments = computed(() => resolveLabeledValues(form.scopeDeptIds, options.value?.departmentOptions || []))
const draftSelectedDepartments = computed(() =>
  resolveLabeledValues(draftScopeDeptIds.value, options.value?.departmentOptions || [])
)
const expenseTypeMap = computed(() => buildExpenseTypeMap(options.value?.expenseTypes || []))
const selectedExpenseTypes = computed(() => resolveLabeledValues(form.scopeExpenseTypeCodes, expenseTypeMap.value))
const draftSelectedExpenseTypes = computed(() =>
  resolveLabeledValues(draftScopeExpenseTypeCodes.value, expenseTypeMap.value)
)
const flowSummaryMap = computed(() => {
  const map = new Map<string, ProcessFlowSummary>()
  flowSummaries.value.forEach((item) => {
    if (item.flowCode) {
      map.set(item.flowCode, item)
    }
  })
  return map
})
const expenseDetailSummaryMap = computed(() => {
  const map = new Map<string, ProcessExpenseDetailDesignSummary>()
  ;(options.value?.expenseDetailDesignOptions || []).forEach((item) => {
    if (item.detailCode) {
      map.set(item.detailCode, item)
    }
  })
  return map
})
const formDesignSummaryMap = computed(() => {
  const map = new Map<string, ProcessFormDesignSummary>()
  formDesignSummaries.value.forEach((item) => {
    if (item.formCode) {
      map.set(item.formCode, item)
    }
  })
  return map
})
const selectedExpenseDetailType = computed(() => expenseDetailSummaryMap.value.get(form.expenseDetailDesign || '')?.detailType || '')
const templateBindingIssues = computed(() => collectTemplateBindingIssues(form, options.value, isReportTemplate.value))
const saveBlockers = computed(() => {
  const blockers: string[] = []

  if (!canCreate.value) {
    blockers.push(templateId.value !== null ? '当前账号没有修改模板权限。' : '当前账号没有新增流程模板权限。')
  }
  if (!form.templateName.trim()) {
    blockers.push('请先填写单据名称。')
  }
  if (!form.approvalFlow) {
    blockers.push('请先选择审批流程。')
  }
  if (!form.formDesign) {
    blockers.push('请先选择表单设计。')
  }
  if (isReportTemplate.value && !form.expenseDetailDesign) {
    blockers.push('报销模板必须绑定费用明细表单。')
  }
  if (form.amountMin && form.amountMax && compareMoney(form.amountMin, form.amountMax) > 0) {
    blockers.push('限定金额区间不合法，最小金额不能大于最大金额。')
  }

  blockers.push(...templateBindingIssues.value)

  return blockers
})

function clearInvalidTemplateBindings(showMessage = false) {
  if (!options.value) {
    return false
  }

  const clearedLabels: string[] = []
  const clearField = (field: 'formDesign' | 'approvalFlow' | 'expenseDetailDesign', label: string, optionList: Array<{ value?: string; detailCode?: string }> | string[]) => {
    const currentValue = String(form[field] || '').trim()
    if (!currentValue) {
      return
    }
    const issue = validateTemplateBindingValue(currentValue, optionList as any, label)
    if (!issue) {
      return
    }
    form[field] = '' as never
    if (field === 'expenseDetailDesign') {
      form.expenseDetailModeDefault = ''
    }
    clearedLabels.push(label)
  }

  clearField('formDesign', '\u8868\u5355\u8bbe\u8ba1', options.value.formDesignOptions || [])
  clearField('approvalFlow', '\u5ba1\u6279\u6d41\u7a0b', options.value.approvalFlows || [])
  if (isReportTemplate.value) {
    clearField('expenseDetailDesign', '\u8d39\u7528\u660e\u7ec6\u8868\u5355', options.value.expenseDetailDesignOptions || [])
  } else if (form.expenseDetailDesign) {
    form.expenseDetailDesign = ''
    form.expenseDetailModeDefault = ''
    clearedLabels.push('\u8d39\u7528\u660e\u7ec6\u8868\u5355')
  }

  if (showMessage && clearedLabels.length) {
    ElMessage.warning(`${clearedLabels.join('\u3001')}\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9`)
  }
  return clearedLabels.length > 0
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

watch(expenseTypeKeyword, (value) => {
  expenseTypeTreeRef.value?.filter(value)
})

watch(
  () => [route.params.templateType, route.query.templateId],
  async () => {
    await loadPage()
  }
)

watch(
  () => route.query.createdFlowCode,
  () => {
    applyCreatedFlowCodeFromRoute()
  }
)

watch(
  () => route.query.createdFormCode,
  () => {
    applyCreatedFormCodeFromRoute()
  }
)

watch(
  () => route.query.createdExpenseDetailCode,
  () => {
    applyCreatedExpenseDetailCodeFromRoute()
  }
)

watch(selectedExpenseDetailType, (value) => {
  if (value === 'ENTERPRISE_TRANSACTION') {
    if (!form.expenseDetailModeDefault) {
      form.expenseDetailModeDefault = DEFAULT_ENTERPRISE_TRANSACTION_MODE
    }
    return
  }
  if (form.expenseDetailModeDefault) {
    form.expenseDetailModeDefault = ''
  }
})

onMounted(async () => {
  await loadPage()
})

function createEmptyForm(type: string): ProcessTemplateSavePayload {
  return {
    templateType: type,
    templateName: '',
    templateDescription: '',
    category: '',
    enabled: true,
    formDesign: '',
    expenseDetailDesign: '',
    expenseDetailModeDefault: '',
    printMode: '',
    approvalFlow: '',
    paymentMode: '',
    allocationForm: '',
    aiAuditMode: '',
    scopeDeptIds: [],
    scopeExpenseTypeCodes: [],
    amountMin: '',
    amountMax: '',
    tagOption: '',
    installmentOption: ''
  }
}

function buildDefaultForm(optionData: ProcessTemplateFormOptions): ProcessTemplateSavePayload {
  return {
    ...createEmptyForm(optionData.templateType),
    category: optionData.categoryOptions[0]?.value || '',
    formDesign: optionData.formDesignOptions[0]?.value || '',
    approvalFlow: optionData.approvalFlows[0]?.value || '',
    printMode: optionData.printModes[0]?.value || '',
    paymentMode: optionData.paymentModes[0]?.value || '',
    allocationForm: optionData.allocationForms[0]?.value || '',
    aiAuditMode: optionData.aiAuditModes[0]?.value || ''
  }
}

function draftStorageKey(type = templateType.value) {
  if (templateId.value !== null) {
    return `${TEMPLATE_DRAFT_PREFIX}edit:${templateId.value}`
  }
  return `${TEMPLATE_DRAFT_PREFIX}${type}`
}

function clonePayload(payload: ProcessTemplateSavePayload): ProcessTemplateSavePayload {
  return {
    ...payload,
    scopeDeptIds: [...payload.scopeDeptIds],
    scopeExpenseTypeCodes: [...payload.scopeExpenseTypeCodes]
  }
}

function readTemplateDraft(type = templateType.value): ProcessTemplateSavePayload | null {
  const raw = window.sessionStorage.getItem(draftStorageKey(type))
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as Partial<ProcessTemplateSavePayload>
    return {
      ...createEmptyForm(type),
      ...parsed,
      templateType: type,
      scopeDeptIds: Array.isArray(parsed.scopeDeptIds) ? parsed.scopeDeptIds.map(String) : [],
      scopeExpenseTypeCodes: Array.isArray(parsed.scopeExpenseTypeCodes)
        ? parsed.scopeExpenseTypeCodes.map(String)
        : []
    }
  } catch {
    window.sessionStorage.removeItem(draftStorageKey(type))
    return null
  }
}

function saveTemplateDraft() {
  window.sessionStorage.setItem(draftStorageKey(), JSON.stringify(clonePayload(form)))
}

function clearTemplateDraft(type = templateType.value) {
  window.sessionStorage.removeItem(draftStorageKey(type))
}

function normalizeTemplateDetail(detail: ProcessTemplateDetail, optionData: ProcessTemplateFormOptions): ProcessTemplateSavePayload {
  const defaults = buildDefaultForm(optionData)
  return {
    ...defaults,
    templateType: optionData.templateType,
    templateName: detail.templateName || '',
    templateDescription: detail.templateDescription || '',
    category: detail.category || defaults.category,
    enabled: detail.enabled ?? true,
    formDesign: detail.formDesign || defaults.formDesign,
    expenseDetailDesign: detail.expenseDetailDesign || '',
    expenseDetailModeDefault: detail.expenseDetailModeDefault || '',
    printMode: detail.printMode || defaults.printMode,
    approvalFlow: detail.approvalFlow || '',
    paymentMode: detail.paymentMode || defaults.paymentMode,
    allocationForm: detail.allocationForm || defaults.allocationForm,
    aiAuditMode: detail.aiAuditMode || defaults.aiAuditMode,
    scopeDeptIds: Array.isArray(detail.scopeDeptIds) ? detail.scopeDeptIds.map(String) : [],
    scopeExpenseTypeCodes: Array.isArray(detail.scopeExpenseTypeCodes) ? detail.scopeExpenseTypeCodes.map(String) : [],
    amountMin: detail.amountMin ?? '',
    amountMax: detail.amountMax ?? '',
    tagOption: detail.tagOption || '',
    installmentOption: detail.installmentOption || ''
  }
}

async function loadPage() {
  try {
    const detailRequest: Promise<{ data: ProcessTemplateDetail } | null> = templateId.value !== null
      ? processApi.getTemplateDetail(templateId.value)
      : Promise.resolve(null)

    const [overviewRes, optionRes, detailRes, flowRes, formDesignRes] = await Promise.all([
      processApi.getOverview(),
      processApi.getFormOptions(templateType.value),
      detailRequest,
      processApi.listFlows(),
      processApi.listFormDesigns(templateType.value)
    ])

    navItems.value = overviewRes.data.navItems
    options.value = optionRes.data
    flowSummaries.value = flowRes.data
    formDesignSummaries.value = formDesignRes.data
    initializeForm(optionRes.data, detailRes?.data || null)
    applyCreatedFlowCodeFromRoute()
    applyCreatedFormCodeFromRoute()
    applyCreatedExpenseDetailCodeFromRoute()
    clearInvalidTemplateBindings(false)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载模板配置页面失败'))
  }
}

function initializeForm(optionData: ProcessTemplateFormOptions, detail: ProcessTemplateDetail | null) {
  const next = readTemplateDraft(optionData.templateType) || (detail ? normalizeTemplateDetail(detail, optionData) : buildDefaultForm(optionData))
  Object.assign(form, buildDefaultForm(optionData), next, {
    templateType: optionData.templateType,
    scopeDeptIds: [...(next.scopeDeptIds || [])],
    scopeExpenseTypeCodes: [...(next.scopeExpenseTypeCodes || [])]
  })
  if (optionData.templateType !== 'report') {
    form.expenseDetailDesign = ''
    form.expenseDetailModeDefault = ''
  } else if (selectedExpenseDetailType.value === 'ENTERPRISE_TRANSACTION' && !form.expenseDetailModeDefault) {
    form.expenseDetailModeDefault = DEFAULT_ENTERPRISE_TRANSACTION_MODE
  }
}

function applyCreatedFlowCodeFromRoute() {
  const createdFlowCode = typeof route.query.createdFlowCode === 'string' ? route.query.createdFlowCode : ''
  if (!createdFlowCode) {
    return
  }

  form.approvalFlow = createdFlowCode
  const nextQuery = { ...route.query }
  delete nextQuery.createdFlowCode
  router.replace({ query: nextQuery })
}

function applyCreatedFormCodeFromRoute() {
  const createdFormCode = typeof route.query.createdFormCode === 'string' ? route.query.createdFormCode : ''
  if (!createdFormCode) {
    return
  }

  form.formDesign = createdFormCode
  const nextQuery = { ...route.query }
  delete nextQuery.createdFormCode
  router.replace({ query: nextQuery })
}

function applyCreatedExpenseDetailCodeFromRoute() {
  const createdExpenseDetailCode = typeof route.query.createdExpenseDetailCode === 'string'
    ? route.query.createdExpenseDetailCode
    : ''
  if (!createdExpenseDetailCode) {
    return
  }

  form.expenseDetailDesign = createdExpenseDetailCode
  if (expenseDetailSummaryMap.value.get(createdExpenseDetailCode)?.detailType === 'ENTERPRISE_TRANSACTION' && !form.expenseDetailModeDefault) {
    form.expenseDetailModeDefault = DEFAULT_ENTERPRISE_TRANSACTION_MODE
  }
  const nextQuery = { ...route.query }
  delete nextQuery.createdExpenseDetailCode
  router.replace({ query: nextQuery })
}

function optionListByField(field: SingleOptionField) {
  switch (field) {
    case 'formDesign':
      return options.value?.formDesignOptions || []
    case 'expenseDetailDesign':
      return (options.value?.expenseDetailDesignOptions || []).map((item) => ({
        label: item.detailName,
        value: item.detailCode
      }))
    case 'approvalFlow':
      return options.value?.approvalFlows || []
    case 'printMode':
      return options.value?.printModes || []
    case 'paymentMode':
      return options.value?.paymentModes || []
    case 'allocationForm':
      return options.value?.allocationForms || []
  }
}

function singleOptionLabel(field: SingleOptionField) {
  const currentValue = form[field]
  if (field === 'expenseDetailDesign') {
    return expenseDetailSummaryMap.value.get(currentValue || '')?.detailName || ''
  }
  return optionListByField(field).find((item) => item.value === currentValue)?.label || ''
}

function openOptionDialog(field: SingleOptionField) {
  const config = optionDialogConfig[field]
  optionDialog.field = field
  optionDialog.title = config.title
  optionDialog.createLabel = config.createLabel
  optionDialog.options = optionListByField(field)
  optionDialog.pendingValue = String(form[field] || '')
  optionDialog.visible = true
}

function confirmOptionDialog() {
  form[optionDialog.field] = optionDialog.pendingValue
  optionDialog.visible = false
}

function showCreateEntry() {
  if (optionDialog.field === 'formDesign') {
    saveTemplateDraft()
    optionDialog.visible = false
    router.push({
      name: 'expense-workbench-process-form-create',
      query: {
        from: templateId.value !== null ? 'template-edit' : 'template-create',
        templateType: templateType.value,
        returnTo: route.fullPath
      }
    })
    return
  }

  if (optionDialog.field === 'expenseDetailDesign') {
    saveTemplateDraft()
    optionDialog.visible = false
    router.push({
      name: 'expense-workbench-process-expense-detail-create',
      query: {
        from: templateId.value !== null ? 'template-edit' : 'template-create',
        templateType: templateType.value,
        returnTo: route.fullPath
      }
    })
    return
  }

  if (optionDialog.field === 'approvalFlow') {
    saveTemplateDraft()
    optionDialog.visible = false
    router.push({
      name: 'expense-workbench-process-flow-create',
      query: {
        from: templateId.value !== null ? 'template-edit' : 'template-create',
        templateType: templateType.value,
        returnTo: route.fullPath
      }
    })
    return
  }

  ElMessage.info(`${optionDialog.createLabel}入口本期先做静态展示`)
}

function showOptionEdit(item: ProcessFormOption) {
  if (optionDialog.field === 'formDesign') {
    const formDesign = formDesignSummaryMap.value.get(item.value)
    if (!formDesign?.id) {
      ElMessage.warning('当前表单设计缺少可编辑详情，请刷新后重试')
      return
    }

    saveTemplateDraft()
    optionDialog.visible = false
    router.push({
      name: 'expense-workbench-process-form-edit',
      params: { id: formDesign.id },
      query: {
        from: templateId.value !== null ? 'template-edit' : 'template-create',
        templateType: templateType.value,
        returnTo: route.fullPath
      }
    })
    return
  }

  if (optionDialog.field === 'expenseDetailDesign') {
    const expenseDetailDesign = expenseDetailSummaryMap.value.get(item.value)
    if (!expenseDetailDesign?.id) {
      ElMessage.warning('当前费用明细表单缺少可编辑详情，请刷新后重试')
      return
    }

    saveTemplateDraft()
    optionDialog.visible = false
    router.push({
      name: 'expense-workbench-process-expense-detail-edit',
      params: { id: expenseDetailDesign.id },
      query: {
        from: templateId.value !== null ? 'template-edit' : 'template-create',
        templateType: templateType.value,
        returnTo: route.fullPath
      }
    })
    return
  }

  if (optionDialog.field === 'approvalFlow') {
    const flow = flowSummaryMap.value.get(item.value)
    if (!flow?.id) {
      ElMessage.warning('当前流程缺少可编辑的详情，请刷新后重试')
      return
    }

    saveTemplateDraft()
    optionDialog.visible = false
    router.push({
      name: 'expense-workbench-process-flow-edit',
      params: { id: flow.id },
      query: {
        from: templateId.value !== null ? 'template-edit' : 'template-create',
        templateType: templateType.value,
        returnTo: route.fullPath
      }
    })
    return
  }

  showStaticEdit(item.label)
}

function showStaticEdit(label: string) {
  ElMessage.info(`“${label}”的编辑入口本期先做静态展示`)
}

function openDepartmentDialog() {
  draftScopeDeptIds.value = [...form.scopeDeptIds]
  departmentKeyword.value = ''
  departmentDialogVisible.value = true
}

function confirmDepartmentDialog() {
  form.scopeDeptIds = [...draftScopeDeptIds.value]
  departmentDialogVisible.value = false
}

function removeDepartment(value: string) {
  draftScopeDeptIds.value = draftScopeDeptIds.value.filter((item) => item !== value)
}

async function openExpenseTypeDialog() {
  draftScopeExpenseTypeCodes.value = [...form.scopeExpenseTypeCodes]
  expenseTypeKeyword.value = ''
  expenseTypeDialogVisible.value = true
  await nextTick()
  expenseTypeTreeRef.value?.setCheckedKeys(draftScopeExpenseTypeCodes.value)
}

function handleExpenseTypeCheck(_: ProcessExpenseTypeTreeNode, payload: { checkedKeys: string[] }) {
  draftScopeExpenseTypeCodes.value = payload.checkedKeys.map(String)
}

function confirmExpenseTypeDialog() {
  form.scopeExpenseTypeCodes = [...draftScopeExpenseTypeCodes.value]
  expenseTypeDialogVisible.value = false
}

function removeExpenseType(value: string) {
  draftScopeExpenseTypeCodes.value = draftScopeExpenseTypeCodes.value.filter((item) => item !== value)
  expenseTypeTreeRef.value?.setCheckedKeys(draftScopeExpenseTypeCodes.value)
}

function filterExpenseTypeNode(keyword: string, data: ProcessExpenseTypeTreeNode) {
  if (!keyword) {
    return true
  }
  return data.expenseName.toLowerCase().includes(keyword.trim().toLowerCase())
}

function buildExpenseTypeMap(nodes: ProcessExpenseTypeTreeNode[]): LabeledValue[] {
  const result: LabeledValue[] = []
  const walk = (items: ProcessExpenseTypeTreeNode[]) => {
    items.forEach((item) => {
      result.push({ label: item.expenseName, value: item.expenseCode })
      if (item.children?.length) {
        walk(item.children)
      }
    })
  }
  walk(nodes)
  return result
}

function resolveLabeledValues(selectedValues: string[], optionsList: LabeledValue[] | ProcessFormOption[]) {
  const map = new Map(optionsList.map((item) => [item.value, item.label]))
  return selectedValues.map((value) => ({ value, label: map.get(value) || value }))
}

const handleSidebarSelect = (section: string) => {
  if (section === 'document-flow') {
    router.push('/expense/workbench/process-management')
    return
  }

  router.push({
    path: '/expense/workbench/process-management',
    query: { section }
  })
}

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const goBack = () => {
  router.push('/expense/workbench/process-management')
}

const scrollToSaveBlockers = async () => {
  await nextTick()
  saveBlockersRef.value?.scrollIntoView?.({ behavior: 'smooth', block: 'center' })
}

function resolveTemplateSaveSuccessMessage(templateCode: string) {
  if (templateId.value !== null) {
    if (isReportTemplate.value && form.enabled) {
      return `模板修改成功，可在新建报销中直接使用：${templateCode}`
    }
    if (isReportTemplate.value && !form.enabled) {
      return `模板修改成功，但当前未启用，不会出现在新建报销中：${templateCode}`
    }
    return `模板修改成功：${templateCode}`
  }

  if (isReportTemplate.value && form.enabled) {
    return `模板已保存，可在新建报销中直接使用：${templateCode}`
  }
  if (isReportTemplate.value && !form.enabled) {
    return `模板已保存，但当前未启用，不会出现在新建报销中：${templateCode}`
  }
  return `模板已保存：${templateCode}`
}

const saveTemplate = async () => {
  if (saveBlockers.value.length) {
    await scrollToSaveBlockers()
    ElMessage.warning(saveBlockers.value[0])
    return
  }

  if (!canCreate.value) {
    ElMessage.warning(templateId.value !== null ? '当前账号没有修改模板权限' : '当前账号没有新增流程模板权限')
    return
  }

  if (!form.templateName.trim()) {
    ElMessage.warning('请先填写单据名称')
    return
  }

  if (!form.approvalFlow) {
    ElMessage.warning('请先选择审批流程')
    return
  }

  if (!form.formDesign) {
    ElMessage.warning('请先选择表单设计')
    return
  }

  if (isReportTemplate.value && !form.expenseDetailDesign) {
    ElMessage.warning('报销模板必须绑定费用明细表单')
    return
  }

  if (form.amountMin && form.amountMax && compareMoney(form.amountMin, form.amountMax) > 0) {
    ElMessage.warning('限定金额区间不合法，最小金额不能大于最大金额')
    return
  }

  saving.value = true
  try {
    if (isReportTemplate.value) {
      if (selectedExpenseDetailType.value === 'ENTERPRISE_TRANSACTION' && !form.expenseDetailModeDefault) {
        form.expenseDetailModeDefault = DEFAULT_ENTERPRISE_TRANSACTION_MODE
      }
    } else {
      form.expenseDetailDesign = ''
      form.expenseDetailModeDefault = ''
    }
    const payload = clonePayload(form)
    const res = templateId.value !== null
      ? await processApi.updateTemplate(templateId.value, payload)
      : await processApi.createTemplate(payload)
    clearTemplateDraft()
    ElMessage.success(resolveTemplateSaveSuccessMessage(res.data.templateCode))
    router.push('/expense/workbench/process-management')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, templateId.value !== null ? '保存修改失败' : '保存模板失败'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.anchor-card {
  scroll-margin-top: 88px;
}

.anchor-link {
  cursor: pointer;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 14px;
  color: #64748b;
  transition: all 0.2s ease;
}

.anchor-link:hover {
  background: #eff6ff;
  color: #2563eb;
}

.rule-preview {
  min-height: 40px;
  display: flex;
  align-items: center;
  border: 1px solid #dbe2eb;
  border-radius: 12px;
  background: #f8fafc;
  padding: 0 14px;
  color: #334155;
}

.save-blockers-panel {
  scroll-margin-top: 96px;
}

.save-blockers-icon {
  display: flex;
  height: 28px;
  width: 28px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: #f59e0b;
  color: #fff;
  font-weight: 700;
}

.save-blocker-item {
  position: relative;
  padding-left: 18px;
}

.save-blocker-item::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 9px;
  height: 6px;
  width: 6px;
  border-radius: 9999px;
  background: #b45309;
}

.selection-trigger {
  min-height: 38px;
  width: 100%;
  display: flex;
  align-items: center;
  border: 1px solid #dbe2eb;
  border-radius: 14px;
  background: #f8fafc;
  padding: 8px 14px;
  text-align: left;
  color: #0f172a;
  font-size: 17px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.selection-trigger:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.selection-trigger-subtle {
  font-size: 14px;
  font-weight: 500;
}

.selection-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.create-entry {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed #93c5fd;
  border-radius: 20px;
  background: #eff6ff;
  padding: 14px 16px;
  color: #2563eb;
  font-weight: 600;
}

.option-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  padding: 12px 16px;
  transition: all 0.2s ease;
}

.option-row:hover,
.option-row-active {
  border-color: #2563eb;
  background: #eff6ff;
}

.selected-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 10px 12px;
}
</style>
