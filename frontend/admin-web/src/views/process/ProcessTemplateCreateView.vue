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
                <el-input v-model="form.templateName" placeholder="请输入单据名称" />
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
                  <el-input-number
                    v-model="form.amountMin"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    class="w-full"
                    placeholder="最小值"
                  />
                  <span class="text-center text-slate-400">至</span>
                  <el-input-number
                    v-model="form.amountMax"
                    :min="0"
                    :precision="2"
                    :controls="false"
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
  type ProcessExpenseTypeTreeNode,
  type ProcessFlowSummary,
  type ProcessFormOption,
  type ProcessTemplateDetail,
  type ProcessTemplateFormOptions,
  type ProcessTemplateSavePayload
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import ProcessWorkbenchSidebar from '@/components/process/ProcessWorkbenchSidebar.vue'

type SingleOptionField = 'formDesign' | 'approvalFlow' | 'printMode' | 'paymentMode' | 'allocationForm'

type LabeledValue = {
  label: string
  value: string
}

type ExpenseTypeTreeInstance = {
  filter: (keyword: string) => void
  setCheckedKeys: (keys: string[]) => void
}

const TEMPLATE_DRAFT_PREFIX = 'process-template-create-draft:'

const route = useRoute()
const router = useRouter()

const defaultNumberingRulePreview = 'FX+年+月+日+4位数字（如：FX202503251234）'

const navItems = ref<ProcessCenterNavItem[]>([])
const options = ref<ProcessTemplateFormOptions | null>(null)
const flowSummaries = ref<ProcessFlowSummary[]>([])
const saving = ref(false)
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])

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
    printMode: '',
    approvalFlow: '',
    paymentMode: '',
    allocationForm: '',
    aiAuditMode: '',
    scopeDeptIds: [],
    scopeExpenseTypeCodes: [],
    amountMin: undefined,
    amountMax: undefined,
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
    printMode: detail.printMode || defaults.printMode,
    approvalFlow: detail.approvalFlow || '',
    paymentMode: detail.paymentMode || defaults.paymentMode,
    allocationForm: detail.allocationForm || defaults.allocationForm,
    aiAuditMode: detail.aiAuditMode || defaults.aiAuditMode,
    scopeDeptIds: Array.isArray(detail.scopeDeptIds) ? detail.scopeDeptIds.map(String) : [],
    scopeExpenseTypeCodes: Array.isArray(detail.scopeExpenseTypeCodes) ? detail.scopeExpenseTypeCodes.map(String) : [],
    amountMin: detail.amountMin ?? undefined,
    amountMax: detail.amountMax ?? undefined,
    tagOption: detail.tagOption || '',
    installmentOption: detail.installmentOption || ''
  }
}

async function loadPage() {
  try {
    const detailRequest: Promise<{ data: ProcessTemplateDetail } | null> = templateId.value !== null
      ? processApi.getTemplateDetail(templateId.value)
      : Promise.resolve(null)

    const [overviewRes, optionRes, detailRes, flowRes] = await Promise.all([
      processApi.getOverview(),
      processApi.getFormOptions(templateType.value),
      detailRequest,
      processApi.listFlows()
    ])

    navItems.value = overviewRes.data.navItems
    options.value = optionRes.data
    flowSummaries.value = flowRes.data
    initializeForm(optionRes.data, detailRes?.data || null)
    applyCreatedFlowCodeFromRoute()
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

function optionListByField(field: SingleOptionField) {
  switch (field) {
    case 'formDesign':
      return options.value?.formDesignOptions || []
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
  return optionListByField(field).find((item) => item.value === currentValue)?.label || ''
}

function openOptionDialog(field: SingleOptionField) {
  const config = optionDialogConfig[field]
  optionDialog.field = field
  optionDialog.title = config.title
  optionDialog.createLabel = config.createLabel
  optionDialog.options = optionListByField(field)
  optionDialog.pendingValue = form[field]
  optionDialog.visible = true
}

function confirmOptionDialog() {
  form[optionDialog.field] = optionDialog.pendingValue
  optionDialog.visible = false
}

function showCreateEntry() {
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

const saveTemplate = async () => {
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

  if (form.amountMin !== undefined && form.amountMax !== undefined && form.amountMin > form.amountMax) {
    ElMessage.warning('限定金额区间不合法，最小金额不能大于最大金额')
    return
  }

  saving.value = true
  try {
    const payload = clonePayload(form)
    const res = templateId.value !== null
      ? await processApi.updateTemplate(templateId.value, payload)
      : await processApi.createTemplate(payload)
    clearTemplateDraft()
    ElMessage.success(`${templateId.value !== null ? '模板修改成功' : '模板已保存'}：${res.data.templateCode}`)
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
