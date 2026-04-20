<template>
  <div :class="workbenchClasses" data-testid="expense-invoice-workbench">
    <div class="expense-invoice-workbench__main">
      <slot name="main-before-list" />

      <el-card class="expense-wb-panel expense-invoice-panel">
        <template #header>
          <div class="expense-invoice-panel__header">
            <div>
              <p class="expense-invoice-panel__title">发票附件列表</p>
              <p class="expense-invoice-panel__desc">上传发票后会自动触发 OCR，识别结果会直接写回当前发票列表。</p>
            </div>
            <el-tag effect="plain">{{ invoiceItems.length }} 张</el-tag>
          </div>
        </template>

        <div v-if="loading" class="expense-invoice-placeholder">
          <div v-for="index in 3" :key="index" class="expense-invoice-card expense-invoice-card--skeleton" />
        </div>
        <div v-else-if="errorMessage" class="expense-invoice-feedback expense-invoice-feedback--error">
          <p class="expense-invoice-feedback__title">发票信息加载失败</p>
          <p class="expense-invoice-feedback__desc">{{ errorMessage }}</p>
        </div>
        <div v-else-if="invoiceItems.length" class="expense-invoice-list" data-testid="expense-invoice-list">
          <button
            v-for="item in invoiceItems"
            :key="item.id"
            type="button"
            class="expense-invoice-card"
            :class="{ 'is-active': item.id === activeInvoiceId }"
            data-testid="expense-invoice-item"
            @click="selectInvoice(item.id)"
          >
            <div class="expense-invoice-card__row">
              <div class="min-w-0">
                <p class="expense-invoice-card__name">{{ item.fileName }}</p>
                <p class="expense-invoice-card__meta">发票代码 {{ item.invoiceCode }} · 发票号码 {{ item.invoiceNumber }}</p>
              </div>
              <el-tag size="small" :type="item.statusTone" effect="plain">{{ item.statusLabel }}</el-tag>
            </div>

            <div class="expense-invoice-card__grid">
              <div>
                <span class="expense-invoice-card__label">发票类型</span>
                <span class="expense-invoice-card__value">{{ item.invoiceType }}</span>
              </div>
              <div>
                <span class="expense-invoice-card__label">销方名称</span>
                <span class="expense-invoice-card__value">{{ item.sellerName }}</span>
              </div>
              <div>
                <span class="expense-invoice-card__label">开票日期</span>
                <span class="expense-invoice-card__value">{{ item.invoiceDate }}</span>
              </div>
              <div>
                <span class="expense-invoice-card__label">含税金额</span>
                <span class="expense-invoice-card__value">{{ formatCurrency(item.totalAmount) }}</span>
              </div>
            </div>

            <div class="expense-invoice-card__footer">
              <div class="flex items-center gap-2">
                <el-tag size="small" effect="plain">{{ previewKindLabel(item.previewKind) }}</el-tag>
                <el-tag size="small" effect="plain">{{ item.providerName }}</el-tag>
              </div>
              <span class="expense-invoice-card__tip">{{ item.recognizedAt === '--' ? '等待识别' : `识别时间 ${item.recognizedAt}` }}</span>
            </div>
          </button>
        </div>
        <el-empty
          v-else
          description="暂未上传发票文件，上传后这里会展示真实 OCR 识别结果。"
          :image-size="90"
        />
      </el-card>
    </div>

    <div class="expense-invoice-workbench__preview">
      <el-card :class="resultPanelClasses">
        <template #header>
          <div class="expense-invoice-panel__header">
            <div>
              <p class="expense-invoice-panel__title">{{ resultPanelTitle }}</p>
              <p class="expense-invoice-panel__desc">{{ resultPanelDesc }}</p>
            </div>
            <el-tag
              v-if="activeInvoice && !isVerificationPlaceholderMode"
              effect="plain"
              :type="activeInvoice.statusTone"
            >
              {{ activeInvoice.statusLabel }}
            </el-tag>
          </div>
        </template>

        <div v-if="loading" class="expense-invoice-placeholder expense-invoice-placeholder--compact">
          <div v-for="index in 6" :key="index" class="expense-invoice-fact expense-invoice-fact--skeleton" />
        </div>
        <div
          v-else-if="isVerificationPlaceholderMode"
          class="expense-invoice-feedback"
          data-testid="expense-invoice-verify-panel"
        >
          <p class="expense-invoice-feedback__title">发票验真（预留）</p>
          <p class="expense-invoice-feedback__desc">当前版本暂未接入真实发票验真能力，后续将在这里展示验真结果。</p>
          <p v-if="activeInvoice" class="expense-invoice-feedback__desc">当前已选择：{{ activeInvoice.fileName }}</p>
        </div>
        <div v-else-if="errorMessage" class="expense-invoice-feedback expense-invoice-feedback--error">
          <p class="expense-invoice-feedback__title">无法展示 OCR 结果</p>
          <p class="expense-invoice-feedback__desc">{{ errorMessage }}</p>
        </div>
        <div v-else-if="activeInvoice" class="expense-invoice-facts" data-testid="expense-invoice-verify-panel">
          <div v-for="fact in activeInvoiceFacts" :key="fact.label" class="expense-invoice-fact">
            <span class="expense-invoice-fact__label">{{ fact.label }}</span>
            <span class="expense-invoice-fact__value">{{ fact.value }}</span>
          </div>
        </div>
        <el-empty v-else :description="resultPanelEmptyDescription" :image-size="82" />
      </el-card>

      <el-card class="expense-wb-panel expense-invoice-panel">
        <template #header>
          <div class="expense-invoice-panel__header">
            <div>
              <p class="expense-invoice-panel__title">发票图像预览</p>
              <p class="expense-invoice-panel__desc">图片显示原图，PDF 直接内嵌预览；历史旧数据会自动降级为文件提示。</p>
            </div>
          </div>
        </template>

        <div v-if="loading" class="expense-invoice-image expense-invoice-image--skeleton" />
        <div v-else-if="errorMessage" class="expense-invoice-feedback expense-invoice-feedback--error">
          <p class="expense-invoice-feedback__title">无法展示发票预览</p>
          <p class="expense-invoice-feedback__desc">{{ errorMessage }}</p>
        </div>
        <div v-else-if="activeInvoice" class="expense-invoice-image" data-testid="expense-invoice-image-panel">
          <div class="expense-invoice-image__header">
            <div>
              <p class="expense-invoice-image__title">{{ activeInvoice.invoiceType }}</p>
              <p class="expense-invoice-image__file" data-testid="expense-invoice-preview-file">{{ activeInvoice.fileName }}</p>
            </div>
            <div class="expense-invoice-image__amount">{{ formatCurrency(activeInvoice.totalAmount) }}</div>
          </div>

          <div class="expense-invoice-image__preview-shell">
            <img
              v-if="activeInvoice.isImage && activePreviewUrl"
              :src="activePreviewUrl"
              :alt="activeInvoice.fileName"
              class="expense-invoice-image__preview-media"
              data-testid="expense-invoice-preview-image"
            />
            <iframe
              v-else-if="activeInvoice.isPdf && activePreviewUrl"
              :src="activePreviewUrl"
              class="expense-invoice-image__preview-media expense-invoice-image__preview-media--pdf"
              title="Invoice PDF Preview"
              data-testid="expense-invoice-preview-pdf"
            />
            <div v-else class="expense-invoice-image__fallback" data-testid="expense-invoice-preview-fallback">
              <el-tag effect="plain">{{ previewKindLabel(activeInvoice.previewKind) }}</el-tag>
              <p class="expense-invoice-image__fallback-title">当前文件暂不支持内嵌预览</p>
              <p class="expense-invoice-image__fallback-desc">
                {{
                  activeInvoice.previewUrl
                    ? '该附件不是图片或 PDF，当前仅展示文件信息。'
                    : '历史单据仅保存了文件名，暂无可回放的原始文件。'
                }}
              </p>
            </div>
          </div>

          <div class="expense-invoice-image__grid">
            <div>
              <span class="expense-invoice-image__label">发票代码</span>
              <span class="expense-invoice-image__value">{{ activeInvoice.invoiceCode }}</span>
            </div>
            <div>
              <span class="expense-invoice-image__label">发票号码</span>
              <span class="expense-invoice-image__value">{{ activeInvoice.invoiceNumber }}</span>
            </div>
            <div>
              <span class="expense-invoice-image__label">开票日期</span>
              <span class="expense-invoice-image__value">{{ activeInvoice.invoiceDate }}</span>
            </div>
            <div>
              <span class="expense-invoice-image__label">税额</span>
              <span class="expense-invoice-image__value">{{ formatCurrency(activeInvoice.taxAmount) }}</span>
            </div>
          </div>

          <div class="expense-invoice-image__seller">
            <span class="expense-invoice-image__label">销方名称</span>
            <span class="expense-invoice-image__value">{{ activeInvoice.sellerName }}</span>
          </div>

          <div class="expense-invoice-image__footer">
            <span>{{ detailTitle || detailNo || '当前费用明细' }}</span>
            <span>{{ activeInvoice.statusMessage }}</span>
          </div>
        </div>
        <el-empty v-else description="请选择一张发票查看图像预览" :image-size="82" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ProcessFormDesignSchema } from '@/api'
import {
  buildAuthorizedAttachmentPreviewUrl,
  buildExpenseInvoicePreviewItems,
  type ExpenseInvoicePreviewItem,
  type ExpenseInvoicePreviewKind
} from '@/views/expense/expenseInvoicePreview'

const props = withDefaults(defineProps<{
  schema?: ProcessFormDesignSchema | null
  formData?: Record<string, unknown> | null
  detailTitle?: string
  detailNo?: string
  loading?: boolean
  errorMessage?: string
  layout?: 'default' | 'balanced'
  compactVerify?: boolean
  resultMode?: 'ocr' | 'verification-placeholder'
}>(), {
  schema: null,
  formData: () => ({}),
  detailTitle: '',
  detailNo: '',
  loading: false,
  errorMessage: '',
  layout: 'default',
  compactVerify: false,
  resultMode: 'ocr'
})

const activeInvoiceId = ref('')

const workbenchClasses = computed(() => [
  'expense-invoice-workbench',
  {
    'expense-invoice-workbench--balanced': props.layout === 'balanced'
  }
])

const resultPanelClasses = computed(() => [
  'expense-wb-panel',
  'expense-invoice-panel',
  {
    'expense-invoice-panel--compact-verify': props.compactVerify
  }
])

const isVerificationPlaceholderMode = computed(() => props.resultMode === 'verification-placeholder')

const resultPanelTitle = computed(() => (
  isVerificationPlaceholderMode.value ? '发票验真' : 'OCR 识别结果'
))

const resultPanelDesc = computed(() => (
  isVerificationPlaceholderMode.value
    ? '预留发票验真展示区域，当前版本暂未接入真实验真能力。'
    : '仅展示当前附件的真实 OCR 快照，不再显示文件名推导的模拟数据。'
))

const resultPanelEmptyDescription = computed(() => (
  isVerificationPlaceholderMode.value
    ? '发票验真能力预留中'
    : '请选择一张发票查看 OCR 识别结果'
))

const invoiceItems = computed(() => buildExpenseInvoicePreviewItems({
  schema: props.schema,
  formData: props.formData
}))

const activeInvoice = computed<ExpenseInvoicePreviewItem | null>(() => (
  invoiceItems.value.find((item) => item.id === activeInvoiceId.value) || null
))

const activePreviewUrl = computed(() => buildAuthorizedAttachmentPreviewUrl(activeInvoice.value?.previewUrl))

const activeInvoiceFacts = computed(() => {
  if (!activeInvoice.value) {
    return []
  }

  return [
    { label: 'OCR 状态', value: activeInvoice.value.statusLabel },
    { label: '状态说明', value: activeInvoice.value.statusMessage },
    { label: 'OCR 厂商', value: activeInvoice.value.providerName },
    { label: '发票代码', value: activeInvoice.value.invoiceCode },
    { label: '发票号码', value: activeInvoice.value.invoiceNumber },
    { label: '发票日期', value: activeInvoice.value.invoiceDate },
    { label: '票种', value: activeInvoice.value.invoiceType },
    { label: '销方名称', value: activeInvoice.value.sellerName },
    { label: '含税金额', value: formatCurrency(activeInvoice.value.totalAmount) },
    { label: '税额', value: formatCurrency(activeInvoice.value.taxAmount) },
    { label: '识别时间', value: activeInvoice.value.recognizedAt }
  ]
})

watch(invoiceItems, (items) => {
  if (!items.length) {
    activeInvoiceId.value = ''
    return
  }
  if (!items.some((item) => item.id === activeInvoiceId.value)) {
    activeInvoiceId.value = items[0]?.id || ''
  }
}, { immediate: true })

function selectInvoice(invoiceId: string) {
  activeInvoiceId.value = invoiceId
}

function previewKindLabel(kind: ExpenseInvoicePreviewKind) {
  switch (kind) {
    case 'image':
      return '图片附件'
    case 'pdf':
      return 'PDF 附件'
    default:
      return '文件附件'
  }
}

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--'
  }
  return `¥ ${value.toFixed(2)}`
}
</script>
