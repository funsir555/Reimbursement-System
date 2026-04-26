<template>
<div ref="pageRootRef" class="expense-wb-page expense-wb-page--create space-y-6">
    <div v-if="!isCreateMode" class="expense-wb-hero">
        <div class="expense-wb-hero__row">
          <div class="expense-wb-hero__main">
            <button
              v-if="showResubmitHeroBack"
              type="button"
              class="mb-4 inline-flex items-center gap-2 text-sm font-medium text-blue-600 transition hover:text-blue-500"
              data-testid="expense-resubmit-hero-back"
              @click="goBack"
            >
              返回
            </button>
            <p class="expense-wb-hero__eyebrow">{{ heroEyebrow }}</p>
            <h1 class="expense-wb-hero__title">{{ pageTitle }}</h1>
            <p class="expense-wb-hero__desc">{{ pageDescription }}</p>
            <div class="expense-wb-meta">
              <span class="expense-wb-meta-pill">{{ modeTag?.label || '新建模式' }}</span>
              <span class="expense-wb-meta-pill">{{ heroMetaPrimary }}</span>
              <span class="expense-wb-meta-pill">{{ heroMetaSecondary }}</span>
            </div>
          </div>

        </div>

      <div v-if="showHeroStats" class="expense-wb-stat-grid">
          <article v-for="stat in showTemplateChooser ? chooserStats : editorStats" :key="stat.label" class="expense-wb-stat-card">
            <div class="expense-wb-stat-card__top">
              <div>
                <p class="expense-wb-stat-card__label">{{ stat.label }}</p>
                <p class="expense-wb-stat-card__value">{{ stat.value }}</p>
                <p class="expense-wb-stat-card__hint">{{ stat.hint }}</p>
              </div>
              <span class="expense-wb-stat-card__icon" :class="`expense-wb-stat-card__icon--${stat.tone}`">
                <el-icon :size="22">
                  <component :is="stat.icon" />
                </el-icon>
              </span>
            </div>
          </article>
      </div>
    </div>

    <div v-if="showTemplateChooser" class="space-y-4">
      <el-card class="expense-wb-toolbar">
        <div class="expense-wb-toolbar__row">
          <div class="expense-wb-toolbar__heading">
            <p class="expense-wb-toolbar__title">选择审批模板</p>
            <p class="expense-wb-toolbar__desc">先选择一个已启用模板，再根据绑定的表单和必要配置完成填写。</p>
          </div>

          <div class="expense-wb-toolbar__group">
            <el-input
              v-model="templateKeyword"
              clearable
              placeholder="搜索模板名称"
              class="w-full lg:max-w-[320px]"
            />
            <el-button @click="goBackToList">{{ backButtonLabel }}</el-button>
          </div>
        </div>
      </el-card>

      <div v-if="templateListStatus === 'loading'" class="expense-wb-status-panel">
        <div class="space-y-3 text-sm text-slate-500">
          <p class="text-lg font-semibold text-slate-800">正在加载可用单据模板</p>
          <p>系统正在读取已启用且可直接新建的模板，请稍候。</p>
        </div>
      </div>

      <div v-else-if="templateListStatus === 'error'" class="expense-wb-status-panel expense-wb-status-panel--warm">
        <div class="space-y-4">
          <div>
            <p class="text-lg font-semibold text-slate-800">模板列表加载失败</p>
            <p class="mt-2 text-sm leading-6 text-slate-500">{{ templateListErrorMessage }}</p>
          </div>
          <div class="flex flex-wrap gap-3">
            <el-button type="primary" @click="retryLoadTemplates">重新加载</el-button>
          </div>
        </div>
      </div>

      <div v-else-if="groupedTemplates.length" class="space-y-8">
        <section
          v-for="group in groupedTemplates"
          :key="group.categoryCode"
          class="space-y-4"
          data-testid="expense-template-group"
          :data-category-code="group.categoryCode"
        >
          <div class="flex items-center gap-3">
            <h3 class="text-xl font-semibold text-slate-800" data-testid="expense-template-group-title">
              {{ group.categoryName }}
            </h3>
            <span class="text-sm text-slate-400">{{ group.items.length }} 个模板</span>
          </div>

          <div class="expense-wb-choice-grid expense-wb-template-grid" data-testid="expense-template-grid">
            <button
              v-for="template in group.items"
              :key="template.templateCode"
              type="button"
              class="expense-wb-choice-card expense-wb-template-card"
              @click="chooseTemplate(template.templateCode)"
            >
              <div class="expense-wb-template-card__header">
                <div class="flex min-w-0 items-start gap-4">
                  <div class="expense-wb-template-card__icon">
                    <el-icon :size="22"><Document /></el-icon>
                  </div>
                  <div class="min-w-0 text-left">
                    <p class="expense-wb-choice-card__title">{{ template.templateName }}</p>
                    <p class="expense-wb-template-card__subtitle">{{ template.templateTypeLabel }}</p>
                  </div>
                </div>
                <el-tag effect="plain">{{ template.categoryName || template.categoryCode || '默认分类' }}</el-tag>
              </div>

              <div class="expense-wb-template-card__description">
                表单编码：{{ template.formDesignCode || '未绑定表单' }}
              </div>

              <div class="expense-wb-template-card__meta">
                <span class="expense-wb-soft-badge">模板编码 {{ template.templateCode }}</span>
                <span class="expense-wb-soft-badge expense-wb-soft-badge--success">可直接新建</span>
              </div>

              <div class="expense-wb-template-card__footer">
                <div class="expense-wb-template-card__footer-row">
                  <span class="expense-wb-template-card__footer-label">点击进入填写页</span>
                  <span class="expense-wb-template-card__footer-value">绑定表单后可直接发起</span>
                </div>
              </div>
            </button>
          </div>
        </section>
      </div>

      <div v-else class="expense-wb-empty-card">
        <el-empty
          :description="templateListStatus === 'empty' ? '当前没有可用模板' : '没有找到匹配的模板'"
          :image-size="96"
        />
      </div>
    </div>

    <template v-else>
      <template v-if="isCreateMode && templateDetailStatus !== 'success'">
        <div class="expense-wb-status-panel" v-loading="templateDetailStatus === 'loading'">
          <div class="space-y-4">
            <div>
              <p class="text-lg font-semibold text-slate-800">
                {{ templateDetailStatus === 'loading' ? '正在加载模板详情' : '模板详情加载失败' }}
              </p>
              <p class="mt-2 text-sm leading-6 text-slate-500">
                {{
                  templateDetailStatus === 'loading'
                    ? '系统正在准备表单与当前模板配置，请稍候。'
                    : templateDetailErrorMessage
                }}
              </p>
            </div>
            <div class="flex flex-wrap gap-3">
              <el-button @click="reselectTemplate">重新选择模板</el-button>
              <el-button v-if="templateDetailStatus === 'error'" type="primary" @click="retryLoadSelectedTemplate">
                重新加载
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <el-card class="expense-wb-panel" v-loading="loading">
          <div class="space-y-5">
            <div class="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
              <div>
                <button
                  v-if="isCreateMode"
                  type="button"
                  class="mb-4 text-sm font-medium text-blue-600 transition hover:text-blue-500"
                  data-testid="expense-create-back-to-chooser"
                  @click="goBack"
                >
                  返回上一层
                </button>
                <div class="flex flex-wrap items-center gap-3">
                  <h2 class="text-2xl font-semibold text-slate-800">{{ templateDetail?.templateName }}</h2>
                  <el-tag effect="plain">{{ templateDetail?.templateTypeLabel }}</el-tag>
                  <el-tag v-if="templateDetail?.flowName" type="success" effect="plain">流程：{{ templateDetail?.flowName }}</el-tag>
                  <el-tag v-if="templateDetail?.expenseDetailDesignName" type="warning" effect="plain">
                    明细：{{ templateDetail?.expenseDetailDesignName }}
                  </el-tag>
                </div>
                <p v-if="!isCreateMode" class="mt-3 text-sm leading-7 text-slate-500">
                  {{ templateDetail?.templateDescription || defaultTemplateDescription }}
                </p>
              </div>

              <div class="expense-wb-aside-stack">
                <div class="expense-wb-pill-cluster">
                <span class="expense-wb-soft-badge">模板编码 {{ templateDetail?.templateCode }}</span>
                <span class="expense-wb-soft-badge expense-wb-soft-badge--success">
                  表单 {{ templateDetail?.formName || '-' }}
                </span>
              </div>
            </div>
          </div>

          <div class="expense-wb-summary-strip">
              <div class="expense-wb-summary-grid">
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">表单名称</span>
                  <span class="expense-wb-summary-item__value">{{ templateDetail?.formName || '-' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">绑定流程</span>
                  <span class="expense-wb-summary-item__value">{{ templateDetail?.flowName || '-' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">费用明细</span>
                  <span class="expense-wb-summary-item__value">{{ templateDetail?.expenseDetailDesignName || '-' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">金额汇总</span>
                  <span class="expense-wb-summary-item__value">{{ totalAmountText }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="expense-wb-panel" v-loading="loading">
          <div class="space-y-6">
            <div class="expense-wb-toolbar__heading">
              <p class="expense-wb-toolbar__title">主表单填写区</p>
              <p class="expense-wb-toolbar__desc">按模板运行时表单填写内容，布局和校验逻辑保持原有行为。</p>
            </div>

            <div class="expense-wb-summary-strip">
              <div class="expense-wb-summary-grid">
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">表单名称</span>
                  <span class="expense-wb-summary-item__value">{{ templateDetail?.formName || '-' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">组件数量</span>
                  <span class="expense-wb-summary-item__value">{{ blocks.length }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">金额汇总</span>
                  <span class="expense-wb-summary-item__value">{{ totalAmountText }}</span>
                </div>
              </div>
            </div>

            <expense-runtime-form-editor
              ref="runtimeEditorRef"
              v-model="formValuesModel"
              :hydrating-form="isFormHydrating"
              :hydration-version="formHydrationVersion"
              :schema="templateDetail?.schema || emptySchema"
              :shared-archives="templateDetail?.sharedArchives || []"
              :company-options="templateDetail?.companyOptions || []"
              :department-options="templateDetail?.departmentOptions || []"
              :current-user-company-id="templateDetail?.currentUserCompanyId || ''"
              :approval-edit-mode="isApprovalModifyMode"
              :allow-edit-form-module="allowEditFormModule"
              :allow-edit-pay-account="allowEditPayAccount"
            />
          </div>
        </el-card>

        <el-card v-if="isReportTemplate" class="expense-wb-panel">
          <template #header>
            <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <p class="text-lg font-semibold text-slate-800">费用明细</p>
                <p class="mt-1 text-sm text-slate-500">每张报销单最多 10 份费用明细，提交前至少需要 1 份。</p>
              </div>
              <el-button type="primary" :disabled="expenseDetails.length >= 10 || !canEditExpenseDetails" @click="addExpenseDetail">
                新增费用明细
              </el-button>
            </div>
          </template>

          <div class="space-y-4">
            <div
              v-for="detail in expenseDetails"
              :key="detail.detailNo"
              class="expense-wb-detail-card"
            >
              <div class="expense-wb-detail-card__body flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <p class="text-base font-semibold text-slate-800">{{ detail.detailTitle || detail.detailNo }}</p>
                    <el-tag effect="plain">{{ detailTypeLabel(detail.detailType) }}</el-tag>
                    <el-tag v-if="detail.enterpriseMode" type="warning" effect="plain">
                      {{ enterpriseModeLabel(detail.enterpriseMode) }}
                    </el-tag>
                  </div>
                  <p class="mt-2 text-sm font-medium text-slate-600">{{ expenseDetailAmountText(detail) }}</p>
                </div>

                <div class="expense-wb-compact-actions">
                  <el-button plain :disabled="!canEditExpenseDetails" @click="editExpenseDetail(detail.detailNo || '')">编辑</el-button>
                  <el-button type="danger" text :disabled="!canEditExpenseDetails" @click="removeExpenseDetail(detail.detailNo || '')">删除</el-button>
                </div>
              </div>
            </div>

            <div v-if="expenseDetails.length === 0" class="expense-wb-empty-card">
              <el-empty description="暂未添加费用明细" :image-size="90" />
            </div>
          </div>
        </el-card>
      </template>
    </template>

    <div
      v-if="showFloatingActionBar"
      class="expense-create-floating-bar"
      data-testid="expense-create-floating-bar"
      :style="floatingBarStyle"
    >
        <div class="expense-create-floating-bar__inner">
          <div class="expense-create-floating-bar__summary" data-testid="expense-create-floating-amount">
            金额：{{ totalAmountText }}
          </div>
          <div class="expense-create-floating-bar__actions">
            <el-button
              class="expense-create-floating-bar__button"
              :loading="savingDraft"
              :disabled="loading || savingDraft || !templateDetail"
              @click="saveDraftManually"
          >
            保存草稿
          </el-button>
          <el-button
            class="expense-create-floating-bar__button expense-create-floating-bar__button--primary"
            type="primary"
            :loading="submitting"
            :disabled="loading || !canSubmit"
            @click="submitDocument"
          >
            {{ submitButtonLabel }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CircleCheckFilled,
  Document,
  Files,
  Tickets
} from '@element-plus/icons-vue'
import {
  type ExpenseCreateTemplateDetail,
  type ExpenseCreateTemplateSummary,
  type ExpenseDetailInstance,
  type ExpenseDocumentEditContext,
  type ProcessFormDesignBlock,
  type ProcessFormDesignSchema
} from '@/api'
import {
  hasPermission,
  readStoredUser
} from '@/utils/permissions'
import { formatMoney } from '@/utils/money'
import { getControlType } from '@/views/process/formDesignerHelper'
import {
  resolveDocumentTotalAmount
} from './expenseDetailRuntime'
import ExpenseRuntimeFormEditor from './components/ExpenseRuntimeFormEditor.vue'
import { useExpenseCreateBootstrap } from './composables/useExpenseCreateBootstrap'
import { useExpenseCreateDraftPersistence } from './composables/useExpenseCreateDraftPersistence'
import { useExpenseCreateExpenseDetailsOwner } from './composables/useExpenseCreateExpenseDetailsOwner'
import { useExpenseCreatePageOrchestration } from './composables/useExpenseCreatePageOrchestration'
import { useExpenseCreatePageUtils } from './composables/useExpenseCreatePageUtils'
import { useExpenseCreateValidationPayload } from './composables/useExpenseCreateValidationPayload'

type PageMode = 'create' | 'resubmit' | 'modify'
type AsyncStatus = 'idle' | 'loading' | 'success' | 'empty' | 'error'

type TemplateGroup = {
  categoryCode: string
  categoryName: string
  items: ExpenseCreateTemplateSummary[]
}

const TEMPLATE_CATEGORY_GROUP_ORDER: Array<{ categoryCode: string; categoryName: string }> = [
  { categoryCode: 'enterprise-payment', categoryName: '企业往来类' },
  { categoryCode: 'employee-expense', categoryName: '员工费用类' },
  { categoryCode: 'business-application', categoryName: '事项申请类' }
]

const route = useRoute()
const router = useRouter()
const {
  detailTypeLabel,
  enterpriseModeLabel,
  cloneRecord,
  cloneValue,
  isKnownTemplateCategory,
  isRecord,
  safeMoneyValue,
  resolveErrorMessage
} = useExpenseCreatePageUtils(TEMPLATE_CATEGORY_GROUP_ORDER)
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const submitting = ref(false)
const savingDraft = ref(false)
const templates = ref<ExpenseCreateTemplateSummary[]>([])
const templateKeyword = ref('')
const templateListStatus = ref<AsyncStatus>('idle')
const templateListErrorMessage = ref('')
const templateDetailStatus = ref<Exclude<AsyncStatus, 'empty'>>('idle')
const templateDetailErrorMessage = ref('')
const selectedTemplateCode = ref('')
const templateDetail = ref<ExpenseCreateTemplateDetail | null>(null)
const runtimeEditorRef = ref<InstanceType<typeof ExpenseRuntimeFormEditor> | null>(null)
const currentDraftKey = ref('')
const pageRootRef = ref<HTMLElement | null>(null)
const floatingBarStyle = ref<Record<string, string>>({})
const formValues = reactive<Record<string, unknown>>({})
const expenseDetails = ref<ExpenseDetailInstance[]>([])
const manualApproverSelections = reactive<Record<string, string[]>>({})
const allowEditFormModule = ref(false)
const allowEditPayAccount = ref(false)

let floatingBarResizeObserver: ResizeObserver | null = null

const emptySchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }

const pageMode = computed<PageMode>(() => {
  if (route.name === 'expense-document-resubmit') {
    return 'resubmit'
  }
  if (route.name === 'expense-approval-task-modify') {
    return 'modify'
  }
  return 'create'
})

const isCreateMode = computed(() => pageMode.value === 'create')
const showTemplateChooser = computed(() => isCreateMode.value && !selectedTemplateCode.value)
const editingDocumentCode = computed(() => String(route.params.documentCode || ''))
const modifyingTaskId = computed(() => Number(route.params.taskId || 0))
const formValuesModel = computed<Record<string, unknown>>({
  get: () => formValues,
  set: (nextValue) => {
    Object.keys(formValues).forEach((key) => {
      delete formValues[key]
    })
    Object.assign(formValues, cloneRecord(nextValue))
  }
})
const blocks = computed(() => templateDetail.value?.schema.blocks || [])
const isReportTemplate = computed(() => templateDetail.value?.templateType === 'report')
const hasExpenseDetailSection = computed(() => Boolean(templateDetail.value?.expenseDetailDesignCode))
const canSubmit = computed(() => {
  if (pageMode.value === 'modify') {
    return hasPermission('expense:approval:approve', permissionCodes.value)
  }
  return hasPermission('expense:create:submit', permissionCodes.value) || hasPermission('expense:create:create', permissionCodes.value)
})

const canEditExpenseDetails = computed(() => pageMode.value !== 'modify')
const isApprovalModifyMode = computed(() => pageMode.value === 'modify')

const filteredTemplates = computed(() => {
  const keyword = templateKeyword.value.trim()
  if (!keyword) {
    return templates.value
  }
  return templates.value.filter((item) => item.templateName.includes(keyword))
})

const groupedTemplates = computed<TemplateGroup[]>(() => {
  const groups = TEMPLATE_CATEGORY_GROUP_ORDER
    .map((group) => ({
      categoryCode: group.categoryCode,
      categoryName: filteredTemplates.value.find((item) => item.categoryCode === group.categoryCode)?.categoryName || group.categoryName,
      items: filteredTemplates.value.filter((item) => item.categoryCode === group.categoryCode)
    }))
    .filter((group) => group.items.length > 0)

  const uncategorizedItems = filteredTemplates.value.filter((item) => !isKnownTemplateCategory(item.categoryCode))
  if (uncategorizedItems.length) {
    groups.push({
      categoryCode: 'uncategorized',
      categoryName: '未分类',
      items: uncategorizedItems
    })
  }

  return groups
})

const totalAmount = computed(() =>
  resolveDocumentTotalAmount(
    blocks.value,
    formValues,
    hasExpenseDetailSection.value ? expenseDetails.value : [],
    templateDetail.value?.expenseDetailModeDefault,
    templateDetail.value?.expenseDetailSchema
  )
)

const totalAmountText = computed(() => `¥ ${formatMoney(totalAmount.value)}`)

const heroMetaPrimary = computed(() =>
  showTemplateChooser.value
    ? `可用模板 ${templates.value.length} 个`
    : `当前模板 ${templateDetail.value?.templateName || selectedTemplateCode.value}`
)

const heroMetaSecondary = computed(() =>
  showTemplateChooser.value ? `搜索结果 ${filteredTemplates.value.length} 个` : `金额汇总 ${totalAmountText.value}`
)

const isDraftEditEntry = computed(() => pageMode.value === 'resubmit' && String(route.query.entry || '') === 'draft')
const useServerDraftSave = computed(() => pageMode.value === 'resubmit' && isDraftEditEntry.value && Boolean(editingDocumentCode.value))

const heroEyebrow = computed(() => {
  if (pageMode.value === 'resubmit') {
    return isDraftEditEntry.value ? '草稿编辑' : '重新提交审批'
  }
  if (pageMode.value === 'modify') {
    return '审批修改'
  }
  return showTemplateChooser.value ? '新建审批单' : '报销草稿'
})

const pageTitle = computed(() => {
  if (pageMode.value === 'resubmit') {
    return isDraftEditEntry.value ? '编辑草稿单据' : '召回后重新提交审批单'
  }
  if (pageMode.value === 'modify') {
    return '审批修改'
  }
  return '新建审批单'
})

const showResubmitHeroBack = computed(() => pageMode.value === 'resubmit')
const showHeroStats = computed(() => pageMode.value !== 'resubmit')
const showFloatingActionBar = computed(() => !showTemplateChooser.value && Boolean(templateDetail.value))

const pageDescription = computed(() => {
  if (pageMode.value === 'resubmit') {
    return isDraftEditEntry.value
      ? '你正在编辑当前草稿，确认后将提交到审批流程。'
      : '你正在基于原单据重新编辑并重提，提交后会从流程起点重新发起。'
  }
  if (pageMode.value === 'modify') {
    return '当前修改会直接作用在原单据上，保存后审批流继续按当前单据流转。'
  }
  return '先选择模板，再按模板绑定的表单与当前配置填写内容。'
})

const modeTag = computed(() => {
  if (pageMode.value === 'resubmit') {
    return isDraftEditEntry.value
      ? { label: '草稿编辑', type: 'warning' as const }
      : { label: '重提模式', type: 'warning' as const }
  }
  if (pageMode.value === 'modify') {
    return { label: '审批修改', type: 'success' as const }
  }
  return null
})

const defaultTemplateDescription = computed(() => {
  if (pageMode.value === 'resubmit') {
    return isDraftEditEntry.value
      ? '草稿编辑完成后，提交会从流程起点重新发起审批。'
      : '召回后请确认本次重提内容，提交后系统会重新生成待办节点。'
  }
  if (pageMode.value === 'modify') {
    return '审批人修改后会直接覆盖原单据数据，轨迹中会保留修改记录。'
  }
  return '暂无模板说明'
})

const chooserStats = computed(() => [
  {
    label: '可用模板',
    value: templates.value.length,
    hint: '当前账号可直接发起审批的启用模板',
    icon: Tickets,
    tone: 'blue'
  },
  {
    label: '搜索结果',
    value: filteredTemplates.value.length,
    hint: '已根据关键词过滤当前模板范围',
    icon: Files,
    tone: 'amber'
  },
  {
    label: '已绑定表单',
    value: templates.value.filter((item) => Boolean(item.formDesignCode)).length,
    hint: '具备主表单绑定，可直接进入填写',
    icon: Document,
    tone: 'green'
  },
  {
    label: '列表状态',
    value: templateListStatus.value === 'loading'
      ? '加载中'
      : templateListStatus.value === 'error'
        ? '需重试'
        : '就绪',
    hint: '首屏模板加载状态一眼可见',
    icon: CircleCheckFilled,
    tone: 'rose'
  }
])

const editorStats = computed(() => [
  {
    label: '主表单组件',
    value: blocks.value.length,
    hint: '当前模板主表单的运行时组件数量',
    icon: Document,
    tone: 'blue'
  },
  {
    label: '费用明细',
    value: isReportTemplate.value ? expenseDetails.value.length : '未启用',
    hint: isReportTemplate.value ? '报销模板提交前至少保留 1 份' : '当前模板无需费用明细',
    icon: Files,
    tone: 'amber'
  },
  {
    label: '绑定流程',
    value: templateDetail.value?.flowName ? '已绑定' : '未绑定',
    hint: templateDetail.value?.flowName ? `当前流程：${templateDetail.value.flowName}` : '沿用模板配置的审批流程',
    icon: Tickets,
    tone: 'green'
  },
  {
    label: '提交状态',
    value: canSubmit.value ? '可提交' : '只读',
    hint: '保留原有权限控制与提交流程',
    icon: CircleCheckFilled,
    tone: 'rose'
  }
])

const {
  addExpenseDetail,
  cloneDetail,
  editExpenseDetail,
  expenseDetailAmountText,
  removeExpenseDetail,
  validateExpenseDetailAmountValues,
  validateExpenseDetailBusinessScenarios,
  validateExpenseDetailRequiredValues
} = useExpenseCreateExpenseDetailsOwner({
  route,
  router,
  templateDetail,
  expenseDetails,
  currentDraftKey,
  selectedTemplateCode,
  emptySchema,
  isReportTemplate,
  persistDraft: (options) => persistDraft(options),
  cloneRecord,
  isRecord,
  resolveErrorMessage
})

const {
  buildDocumentUpdatePayload,
  cloneManualApproverSelections
} = useExpenseCreateValidationPayload({
  formValues,
  totalAmount,
  expenseDetails,
  manualApproverSelections,
  cloneRecord,
  cloneDetail
})

const {
  clearDraft,
  formHydrationVersion,
  isFormHydrating,
  persistDraft,
  readDraft,
  restoreResubmitDraftState,
  runWithFormHydration
} = useExpenseCreateDraftPersistence({
  currentDraftKey,
  selectedTemplateCode,
  templateDetail,
  formValues,
  expenseDetails,
  manualApproverSelections,
  applyTemplateDetail,
  restoreManualApproverSelections,
  cloneDetail,
  cloneRecord,
  cloneValue,
  cloneManualApproverSelections
})

const {
  applyEditContextState,
  chooseTemplate,
  resetCreateSelectionState,
  retryLoadTemplates,
  retryLoadSelectedTemplate
} = useExpenseCreateBootstrap({
  route,
  router,
  pageMode,
  editingDocumentCode,
  modifyingTaskId,
  emptySchema,
  loading,
  templates,
  templateListStatus,
  templateListErrorMessage,
  templateDetailStatus,
  templateDetailErrorMessage,
  selectedTemplateCode,
  templateDetail,
  currentDraftKey,
  expenseDetails,
  formValues,
  applyEditContextPermissions,
  applyTemplateDetail,
  resetFormValues,
  restoreManualApproverSelections,
  persistDraft,
  readDraft,
  restoreResubmitDraftState,
  runWithFormHydration,
  cloneDetail,
  cloneRecord,
  resolveErrorMessage
})

const {
  backButtonLabel,
  goBack,
  goBackToList,
  reselectTemplate,
  saveDraftManually,
  submitButtonLabel,
  submitDocument
} = useExpenseCreatePageOrchestration({
  route,
  router,
  pageMode,
  permissionCodes,
  currentDraftKey,
  selectedTemplateCode,
  templateDetail,
  editingDocumentCode,
  modifyingTaskId,
  emptySchema,
  runtimeEditorRef,
  expenseDetailsCount: computed(() => expenseDetails.value.length),
  isReportTemplate,
  isDraftEditEntry,
  useServerDraftSave,
  savingDraft,
  submitting,
  resetCreateSelectionState,
  clearDraft,
  persistDraft,
  buildDocumentUpdatePayload,
  applyEditContextState,
  restoreManualApproverSelections,
  cloneManualApproverSelections,
  validateExpenseDetailAmountValues,
  validateExpenseDetailRequiredValues,
  validateExpenseDetailBusinessScenarios,
  resolveErrorMessage
})

watch(
  showFloatingActionBar,
  async () => {
    await nextTick()
    updateFloatingBarLayout()
  },
  { immediate: true }
)

onMounted(async () => {
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', updateFloatingBarLayout)
  }
  if (typeof ResizeObserver !== 'undefined' && pageRootRef.value) {
    floatingBarResizeObserver = new ResizeObserver(() => {
      updateFloatingBarLayout()
    })
    floatingBarResizeObserver.observe(pageRootRef.value)
  }
  await nextTick()
  updateFloatingBarLayout()
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateFloatingBarLayout)
  }
  floatingBarResizeObserver?.disconnect()
  floatingBarResizeObserver = null
})
function applyTemplateDetail(nextDetail: ExpenseCreateTemplateDetail) {
  templateDetail.value = cloneValue(nextDetail)
}

function resetFormValues() {
  Object.keys(formValues).forEach((key) => {
    delete formValues[key]
  })
  clearManualApproverSelections()
  blocks.value.forEach((block) => {
    if (block.defaultValue !== undefined) {
      formValues[block.fieldKey] = Array.isArray(block.defaultValue) ? [...block.defaultValue] : block.defaultValue
      return
    }
    const businessDefaultValue = resolveBusinessComponentDefaultValue(block)
    if (businessDefaultValue !== undefined) {
      formValues[block.fieldKey] = businessDefaultValue
      return
    }
    if (block.kind === 'CONTROL' && ['MULTI_SELECT', 'CHECKBOX', 'DATE_RANGE'].includes(controlType(block))) {
      formValues[block.fieldKey] = []
      return
    }
    if (block.kind === 'CONTROL' && controlType(block) === 'SWITCH') {
      formValues[block.fieldKey] = false
      return
    }
    formValues[block.fieldKey] = ''
  })
}

function clearManualApproverSelections() {
  Object.keys(manualApproverSelections).forEach((key) => {
    delete manualApproverSelections[key]
  })
}

function restoreManualApproverSelections(source?: Record<string, string[]>) {
  clearManualApproverSelections()
  Object.entries(source || {}).forEach(([nodeKey, userIds]) => {
    if (!nodeKey || !Array.isArray(userIds) || userIds.length === 0) {
      return
    }
    manualApproverSelections[nodeKey] = userIds
      .map((item) => String(item || '').trim())
      .filter(Boolean)
  })
}

function applyEditContextPermissions(context: ExpenseDocumentEditContext | null) {
  allowEditFormModule.value = Boolean(context?.allowEditFormModule)
  allowEditPayAccount.value = Boolean(context?.allowEditPayAccount)
}

function resolveBusinessComponentDefaultValue(block: ProcessFormDesignBlock) {
  if (block.kind !== 'BUSINESS_COMPONENT') {
    return undefined
  }
  const componentCode = String(block.props.componentCode || '')
  if (componentCode === 'payment-company') {
    const availableCompanyIds = new Set(
      (templateDetail.value?.companyOptions || [])
        .map((item) => String(item.value || '').trim())
        .filter(Boolean)
    )
    const mode = String(block.props.defaultCompanyMode || 'NONE')
    if (mode === 'FIXED_COMPANY') {
      const defaultCompanyId = String(block.props.defaultCompanyId || '').trim()
      return availableCompanyIds.has(defaultCompanyId) ? defaultCompanyId : ''
    }
    if (mode === 'SUBMITTER_COMPANY') {
      const submitterCompanyId = String(templateDetail.value?.currentUserCompanyId || '').trim()
      return availableCompanyIds.has(submitterCompanyId) ? submitterCompanyId : ''
    }
    return ''
  }
  if (componentCode === 'undertake-department') {
    const availableDepartmentIds = new Set(
      (templateDetail.value?.departmentOptions || [])
        .map((item) => String(item.value || '').trim())
        .filter(Boolean)
    )
    const mode = String(block.props.defaultDeptMode || 'NONE')
    if (mode === 'FIXED_DEPARTMENT') {
      const defaultDeptId = String(block.props.defaultDeptId || '').trim()
      return availableDepartmentIds.has(defaultDeptId) ? defaultDeptId : ''
    }
    if (mode === 'SUBMITTER_DEPARTMENT') {
      const submitterDeptId = String(templateDetail.value?.currentUserDeptId || '').trim()
      return availableDepartmentIds.has(submitterDeptId) ? submitterDeptId : ''
    }
    return ''
  }
  return undefined
}

function controlType(block: ProcessFormDesignBlock) {
  return getControlType(block)
}

function updateFloatingBarLayout() {
  if (!showFloatingActionBar.value || !pageRootRef.value) {
    floatingBarStyle.value = {}
    return
  }
  const rect = pageRootRef.value.getBoundingClientRect()
  floatingBarStyle.value = {
    left: `${Math.max(rect.left, 0)}px`,
    width: `${Math.max(rect.width, 0)}px`,
    transform: 'none'
  }
}

</script>

<style scoped>
.expense-wb-page--create {
  padding-bottom: 188px;
}

.expense-create-floating-bar {
  position: fixed;
  bottom: 24px;
  z-index: 30;
  display: flex;
  justify-content: flex-start;
  max-width: none;
}

.expense-create-floating-bar__inner {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid rgba(219, 234, 254, 0.92);
  border-radius: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.96) 100%);
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(16px);
  padding: 24px 30px;
  font-size: 18px;
}

.expense-create-floating-bar__summary {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.expense-create-floating-bar__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 18px;
}

:deep(.expense-create-floating-bar__button.el-button) {
  min-height: 54px;
  padding: 0 28px;
  border-radius: 20px;
  font-size: 18px;
  font-weight: 600;
}

:deep(.expense-create-floating-bar__button--primary.el-button) {
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.18);
}

@media (max-width: 767px) {
  .expense-wb-page--create {
    padding-bottom: 212px;
  }

  .expense-create-floating-bar {
    bottom: 16px;
  }

  .expense-create-floating-bar__inner {
    flex-wrap: wrap;
    gap: 12px;
    padding: 18px 16px;
    font-size: 16px;
  }

  .expense-create-floating-bar__summary {
    width: 100%;
    font-size: 18px;
  }

  .expense-create-floating-bar__actions {
    width: 100%;
    gap: 12px;
  }

  :deep(.expense-create-floating-bar__button.el-button) {
    flex: 1 1 calc(50% - 6px);
    min-height: 48px;
    padding: 0 16px;
    font-size: 16px;
  }
}
</style>
