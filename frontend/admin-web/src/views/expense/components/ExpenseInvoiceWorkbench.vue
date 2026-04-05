<template>
  <div :class="workbenchClasses" data-testid="expense-invoice-workbench">
    <div class="expense-invoice-workbench__main">
      <slot name="main-before-list" />

      <el-card class="expense-wb-panel expense-invoice-panel">
        <template #header>
          <div class="expense-invoice-panel__header">
            <div>
              <p class="expense-invoice-panel__title">上传发票列表</p>
              <p class="expense-invoice-panel__desc">按已上传附件生成发票预览列表，点击可切换右侧验真信息与原始文件预览。</p>
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
              <el-tag size="small" :type="item.verifyTone" effect="plain">{{ item.verifyStatus }}</el-tag>
            </div>

            <div class="expense-invoice-card__grid">
              <div>
                <span class="expense-invoice-card__label">发票类型</span>
                <span class="expense-invoice-card__value">{{ item.invoiceType }}</span>
              </div>
              <div>
                <span class="expense-invoice-card__label">销方名称</span>
                <span class="expense-invoice-card__value">{{ item.seller }}</span>
              </div>
              <div>
                <span class="expense-invoice-card__label">开票日期</span>
                <span class="expense-invoice-card__value">{{ item.issueDate }}</span>
              </div>
              <div>
                <span class="expense-invoice-card__label">金额</span>
                <span class="expense-invoice-card__value">{{ formatCurrency(item.amount) }}</span>
              </div>
            </div>

            <div class="expense-invoice-card__footer">
              <div class="flex items-center gap-2">
                <el-tag size="small" effect="plain" :type="item.ocrTone">{{ item.ocrStatus }}</el-tag>
                <el-tag size="small" effect="plain">{{ previewKindLabel(item.previewKind) }}</el-tag>
              </div>
              <span class="expense-invoice-card__tip">验真时间 {{ item.checkTime }}</span>
            </div>
          </button>
        </div>
        <el-empty
          v-else
          description="暂未上传发票文件，上传后这里会自动生成发票列表。"
          :image-size="90"
        />
      </el-card>
    </div>

    <div class="expense-invoice-workbench__preview">
      <el-card :class="verifyPanelClasses">
        <template #header>
          <div class="expense-invoice-panel__header">
            <div>
              <p class="expense-invoice-panel__title">税务验真结果</p>
              <p class="expense-invoice-panel__desc">当前沿用演示验真数据，后续可替换为真实验真结果。</p>
            </div>
            <el-tag v-if="activeInvoice" effect="plain" :type="activeInvoice.verifyTone">{{ activeInvoice.verifyStatus }}</el-tag>
          </div>
        </template>

        <div v-if="loading" class="expense-invoice-placeholder expense-invoice-placeholder--compact">
          <div v-for="index in 6" :key="index" class="expense-invoice-fact expense-invoice-fact--skeleton" />
        </div>
        <div v-else-if="errorMessage" class="expense-invoice-feedback expense-invoice-feedback--error">
          <p class="expense-invoice-feedback__title">无法展示验真结果</p>
          <p class="expense-invoice-feedback__desc">{{ errorMessage }}</p>
        </div>
        <div v-else-if="activeInvoice" class="expense-invoice-facts" data-testid="expense-invoice-verify-panel">
          <div v-for="fact in activeInvoiceFacts" :key="fact.label" class="expense-invoice-fact">
            <span class="expense-invoice-fact__label">{{ fact.label }}</span>
            <span class="expense-invoice-fact__value">{{ fact.value }}</span>
          </div>
        </div>
        <el-empty v-else description="请选择一张发票查看验真结果" :image-size="82" />
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
            <div class="expense-invoice-image__amount">{{ formatCurrency(activeInvoice.amount) }}</div>
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
              <span class="expense-invoice-image__value">{{ activeInvoice.issueDate }}</span>
            </div>
            <div>
              <span class="expense-invoice-image__label">税额</span>
              <span class="expense-invoice-image__value">{{ formatCurrency(activeInvoice.taxAmount) }}</span>
            </div>
          </div>

          <div class="expense-invoice-image__seller">
            <span class="expense-invoice-image__label">销方名称</span>
            <span class="expense-invoice-image__value">{{ activeInvoice.seller }}</span>
          </div>

          <div class="expense-invoice-image__footer">
            <span>{{ detailTitle || detailNo || '当前费用明细' }}</span>
            <span>{{ activeInvoice.checkTime }}</span>
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
}>(), {
  schema: null,
  formData: () => ({}),
  detailTitle: '',
  detailNo: '',
  loading: false,
  errorMessage: '',
  layout: 'default',
  compactVerify: false
})

const activeInvoiceId = ref('')

const workbenchClasses = computed(() => [
  'expense-invoice-workbench',
  {
    'expense-invoice-workbench--balanced': props.layout === 'balanced'
  }
])

const verifyPanelClasses = computed(() => [
  'expense-wb-panel',
  'expense-invoice-panel',
  {
    'expense-invoice-panel--compact-verify': props.compactVerify
  }
])

const invoiceItems = computed(() => buildExpenseInvoicePreviewItems({
  schema: props.schema,
  formData: props.formData,
  detailTitle: props.detailTitle,
  detailNo: props.detailNo
}))

const activeInvoice = computed(() => (
  invoiceItems.value.find((item) => item.id === activeInvoiceId.value) || null
))

const activePreviewUrl = computed(() => buildAuthorizedAttachmentPreviewUrl(activeInvoice.value?.previewUrl))

const activeInvoiceFacts = computed(() => {
  if (!activeInvoice.value) {
    return []
  }

  return [
    { label: '验真状态', value: activeInvoice.value.verifyStatus },
    { label: 'OCR 状态', value: activeInvoice.value.ocrStatus },
    { label: '验真时间', value: activeInvoice.value.checkTime },
    { label: '发票代码', value: activeInvoice.value.invoiceCode },
    { label: '发票号码', value: activeInvoice.value.invoiceNumber },
    { label: '开票日期', value: activeInvoice.value.issueDate },
    { label: '发票类型', value: activeInvoice.value.invoiceType },
    { label: '销方名称', value: activeInvoice.value.seller },
    { label: '发票金额', value: formatCurrency(activeInvoice.value.amount) },
    { label: '税额', value: formatCurrency(activeInvoice.value.taxAmount) }
  ]
})

watch(
  invoiceItems,
  (nextItems) => {
    if (!nextItems.length) {
      activeInvoiceId.value = ''
      return
    }
    if (!nextItems.some((item) => item.id === activeInvoiceId.value)) {
      activeInvoiceId.value = nextItems[0]?.id || ''
    }
  },
  { immediate: true }
)

function selectInvoice(invoiceId: string) {
  activeInvoiceId.value = invoiceId
}

function previewKindLabel(kind: ExpenseInvoicePreviewKind) {
  if (kind === 'image') {
    return '图片'
  }
  if (kind === 'pdf') {
    return 'PDF'
  }
  return '文件'
}

function formatCurrency(value: number) {
  return `¥ ${value.toFixed(2)}`
}
</script>
