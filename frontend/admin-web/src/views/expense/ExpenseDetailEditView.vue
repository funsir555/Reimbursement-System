<template>
  <div ref="pageRootRef" class="expense-detail-edit-page space-y-6">
    <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="space-y-5">
        <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回审批单
        </button>

        <div>
          <div class="flex flex-wrap items-center gap-3">
            <h1 class="text-3xl font-bold text-slate-800">{{ detailTitle || detailNo }}</h1>
            <el-tag effect="plain">{{ templateDetail?.expenseDetailTypeLabel || '费用明细' }}</el-tag>
            <el-tag v-if="templateDetail?.expenseDetailDesignName" type="warning" effect="plain">
              {{ templateDetail.expenseDetailDesignName }}
            </el-tag>
          </div>
          <p class="mt-3 text-sm leading-7 text-slate-500">
            明细编号：{{ detailNo }}。保存后会直接写回当前草稿，并返回审批单编辑页。
          </p>
        </div>
      </div>
    </section>

    <ExpenseInvoiceWorkbench
      :schema="templateDetail?.expenseDetailSchema || emptySchema"
      :form-data="detailFormData"
      :detail-title="detailTitle"
      :detail-no="detailNo"
      :loading="loading"
      layout="balanced"
      compact-verify
    >
      <template #main-before-list>
        <el-card class="!rounded-3xl !shadow-sm" v-loading="loading">
          <div class="grid grid-cols-1 gap-5 lg:grid-cols-2">
            <el-form-item label="明细标题" required class="!mb-0">
              <el-input v-model="detailTitle" maxlength="80" placeholder="请输入明细标题" />
            </el-form-item>
          </div>
        </el-card>

        <el-card class="!rounded-3xl !shadow-sm" v-loading="loading">
          <template #header>
            <div>
              <p class="text-lg font-semibold text-slate-800">明细表单</p>
              <p class="mt-1 text-sm text-slate-500">根据模板绑定的费用明细设计填写本条费用内容。</p>
            </div>
          </template>

          <ExpenseRuntimeFormEditor
            v-if="templateDetail"
            v-model="detailFormDataModel"
            :schema="templateDetail.expenseDetailSchema || emptySchema"
            :shared-archives="templateDetail.expenseDetailSharedArchives || []"
            :company-options="templateDetail.companyOptions || []"
            :department-options="templateDetail.departmentOptions || []"
            :detail-type="templateDetail.expenseDetailType"
            :default-business-scenario="templateDetail.expenseDetailModeDefault || ''"
          />
          <el-empty v-else description="未找到明细设计" :image-size="90" />
        </el-card>
      </template>
    </ExpenseInvoiceWorkbench>

    <div
      class="expense-detail-edit-floating-bar"
      data-testid="expense-detail-edit-floating-bar"
      :style="floatingBarStyle"
    >
      <div class="expense-detail-edit-floating-bar__inner">
        <div class="expense-detail-edit-floating-bar__summary" data-testid="expense-detail-edit-floating-amount">
          金额：{{ paymentAmountText }}
        </div>
        <div class="expense-detail-edit-floating-bar__actions">
          <el-button class="expense-detail-edit-floating-bar__button" @click="goBack">取消</el-button>
          <el-button
            class="expense-detail-edit-floating-bar__button expense-detail-edit-floating-bar__button--primary"
            type="primary"
            :loading="saving"
            :disabled="loading || !templateDetail"
            @click="saveDetail"
          >
            保存并返回
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { type ExpenseCreateTemplateDetail, type ExpenseDetailInstance, type ProcessFormDesignSchema } from '@/api'
import { formatMoney, normalizeMoneyValue } from '@/utils/money'
import {
  FIELD_ACTUAL_PAYMENT_AMOUNT,
  buildExpenseDetailFormData,
  enrichExpenseDetailInstance
} from './expenseDetailRuntime'
import ExpenseInvoiceWorkbench from './components/ExpenseInvoiceWorkbench.vue'
import ExpenseRuntimeFormEditor from './components/ExpenseRuntimeFormEditor.vue'

type ExpenseCreateDraft = {
  templateCode: string
  formValues: Record<string, unknown>
  expenseDetails: ExpenseDetailInstance[]
  templateDetail?: ExpenseCreateTemplateDetail
}

const DRAFT_PREFIX = 'expense-create-draft:'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const pageRootRef = ref<HTMLElement | null>(null)
const floatingBarStyle = ref<Record<string, string>>({})
const templateDetail = ref<ExpenseCreateTemplateDetail | null>(null)
const detailTitle = ref('')
const sortOrder = ref<number | undefined>(undefined)
const detailFormData = reactive<Record<string, unknown>>({})

let floatingBarResizeObserver: ResizeObserver | null = null

const detailNo = computed(() => String(route.params.detailNo || ''))
const draftKey = computed(() => String(route.query.draftKey || ''))
const returnTo = computed(() => String(route.query.returnTo || ''))
const emptySchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }
const paymentAmountValue = computed(() => safeMoneyValue(detailFormData[FIELD_ACTUAL_PAYMENT_AMOUNT]))
const paymentAmountText = computed(() => `¥ ${formatMoney(paymentAmountValue.value)}`)
const detailFormDataModel = computed<Record<string, unknown>>({
  get: () => detailFormData,
  set: (nextValue) => {
    resetFormData(cloneRecord(nextValue))
  }
})

watch(
  () => route.fullPath,
  () => {
    void loadPage()
  }
)

watch(
  () => Boolean(templateDetail.value),
  async () => {
    await nextTick()
    updateFloatingBarLayout()
  },
  { immediate: true }
)

void loadPage()

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

async function loadPage() {
  if (!draftKey.value || !detailNo.value) {
    ElMessage.warning('缺少草稿上下文，无法编辑费用明细')
    await router.replace('/expense/create')
    return
  }

  loading.value = true
  try {
    const draft = readDraft()
    const detail = draft?.expenseDetails?.find((item) => item.detailNo === detailNo.value)
    if (!draft?.templateDetail || !detail) {
      ElMessage.warning('当前费用明细草稿不存在，已返回审批单编辑页')
      goBack()
      return
    }

    templateDetail.value = cloneValue(draft.templateDetail)
    detailTitle.value = String(detail.detailTitle || '')
    sortOrder.value = detail.sortOrder
    resetFormData(
      buildExpenseDetailFormData(
        draft.templateDetail.expenseDetailSchema,
        draft.templateDetail.expenseDetailType,
        detail.formData || {},
        draft.templateDetail.expenseDetailModeDefault
      )
    )
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载费用明细失败'))
  } finally {
    loading.value = false
  }
}

function storageKey() {
  return `${DRAFT_PREFIX}${draftKey.value}`
}

function readDraft(): ExpenseCreateDraft | null {
  const raw = window.sessionStorage.getItem(storageKey())
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as ExpenseCreateDraft
  } catch {
    window.sessionStorage.removeItem(storageKey())
    return null
  }
}

function resetFormData(nextValue: Record<string, unknown>) {
  Object.keys(detailFormData).forEach((key) => {
    delete detailFormData[key]
  })
  Object.assign(detailFormData, cloneRecord(nextValue))
}

function goBack() {
  if (returnTo.value) {
    void router.push(returnTo.value)
    return
  }
  void router.push('/expense/create')
}

function saveDetail() {
  const normalizedTitle = detailTitle.value.trim()
  if (!normalizedTitle) {
    ElMessage.warning('请先填写明细标题')
    return
  }

  const draft = readDraft()
  if (!draft) {
    ElMessage.warning('当前审批草稿不存在，无法保存明细')
    goBack()
    return
  }

  const detailIndex = draft.expenseDetails.findIndex((item) => item.detailNo === detailNo.value)
  if (detailIndex < 0) {
    ElMessage.warning('当前费用明细不存在，无法保存')
    goBack()
    return
  }

  saving.value = true
  try {
    const current = draft.expenseDetails[detailIndex]
    const nextFormData = cloneRecord(detailFormData)
    nextFormData[FIELD_ACTUAL_PAYMENT_AMOUNT] = paymentAmountValue.value
    draft.expenseDetails[detailIndex] = enrichExpenseDetailInstance(
      {
        ...current,
        detailNo: detailNo.value,
        detailDesignCode: templateDetail.value?.expenseDetailDesignCode,
        detailType: templateDetail.value?.expenseDetailType,
        detailTitle: normalizedTitle,
        sortOrder: sortOrder.value ?? current?.sortOrder,
        formData: nextFormData
      },
      templateDetail.value?.expenseDetailModeDefault
    )
    window.sessionStorage.setItem(storageKey(), JSON.stringify(draft))
    ElMessage.success('费用明细已保存')
    goBack()
  } finally {
    saving.value = false
  }
}

function cloneRecord(value: Record<string, unknown>) {
  return JSON.parse(JSON.stringify(value || {})) as Record<string, unknown>
}

function safeMoneyValue(value: unknown) {
  try {
    return normalizeMoneyValue(value as string | number | null | undefined, {
      allowNegative: true,
      fallback: '0.00'
    })
  } catch {
    return '0.00'
  }
}

function updateFloatingBarLayout() {
  if (!pageRootRef.value) {
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

function cloneValue<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<style scoped>
.expense-detail-edit-page {
  padding-bottom: 188px;
}

.expense-detail-edit-floating-bar {
  position: fixed;
  bottom: 24px;
  z-index: 30;
  display: flex;
  justify-content: flex-start;
  max-width: none;
}

.expense-detail-edit-floating-bar__inner {
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

.expense-detail-edit-floating-bar__summary {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.expense-detail-edit-floating-bar__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 18px;
}

:deep(.expense-detail-edit-floating-bar__button.el-button) {
  min-height: 54px;
  padding: 0 28px;
  border-radius: 20px;
  font-size: 18px;
  font-weight: 600;
}

:deep(.expense-detail-edit-floating-bar__button--primary.el-button) {
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.18);
}

@media (max-width: 767px) {
  .expense-detail-edit-page {
    padding-bottom: 212px;
  }

  .expense-detail-edit-floating-bar {
    bottom: 16px;
  }

  .expense-detail-edit-floating-bar__inner {
    flex-wrap: wrap;
    gap: 12px;
    padding: 18px 16px;
    font-size: 16px;
  }

  .expense-detail-edit-floating-bar__summary {
    width: 100%;
    font-size: 18px;
  }

  .expense-detail-edit-floating-bar__actions {
    width: 100%;
    gap: 12px;
  }

  :deep(.expense-detail-edit-floating-bar__button.el-button) {
    flex: 1 1 calc(50% - 6px);
    min-height: 48px;
    padding: 0 16px;
    font-size: 16px;
  }
}
</style>
