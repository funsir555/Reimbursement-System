<template>
  <div class="expense-pdf-preview" data-testid="expense-pdf-preview-page">
    <header class="expense-pdf-preview__toolbar">
      <div class="expense-pdf-preview__heading">
        <p class="expense-pdf-preview__eyebrow">报销管理打印预览</p>
        <h1 class="expense-pdf-preview__title">{{ pageTitle }}</h1>
        <p class="expense-pdf-preview__subtitle">{{ pageSubtitle }}</p>
      </div>

      <div class="expense-pdf-preview__actions">
        <div class="expense-pdf-preview__orientation">
          <el-button
            data-testid="expense-pdf-preview-portrait"
            :type="orientation === 'PORTRAIT' ? 'primary' : undefined"
            :plain="orientation !== 'PORTRAIT'"
            :disabled="loading"
            @click="setOrientation('PORTRAIT')"
          >
            纵向
          </el-button>
          <el-button
            data-testid="expense-pdf-preview-landscape"
            :type="orientation === 'LANDSCAPE' ? 'primary' : undefined"
            :plain="orientation !== 'LANDSCAPE'"
            :disabled="loading"
            @click="setOrientation('LANDSCAPE')"
          >
            横向
          </el-button>
        </div>

        <el-button
          data-testid="expense-pdf-preview-download"
          :disabled="!pdfObjectUrl"
          @click="downloadCurrentPdf"
        >
          下载 PDF
        </el-button>
        <el-button
          data-testid="expense-pdf-preview-retry"
          :disabled="loading"
          @click="loadPreview"
        >
          重试
        </el-button>
      </div>
    </header>

    <main v-loading="loading" class="expense-pdf-preview__body">
      <iframe
        v-if="pdfObjectUrl && !error"
        :src="viewerSrc"
        class="expense-pdf-preview__frame"
        data-testid="expense-pdf-preview-frame"
        title="报销单 PDF 预览"
      />

      <div v-else class="expense-pdf-preview__empty">
        <el-empty :description="error || emptyDescription" :image-size="96" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { expenseApi, type ExpensePrintOrientation } from '@/api'
import { normalizeExpensePrintDocumentCodes } from './expensePrintSupport'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const pdfObjectUrl = ref('')
const fileName = ref('')
const orientation = ref<ExpensePrintOrientation>(normalizeOrientation(route.query.orientation))

let requestVersion = 0

const documentCode = computed(() => {
  const rawValue = Array.isArray(route.params.documentCode)
    ? route.params.documentCode[0] || ''
    : String(route.params.documentCode || '')
  return rawValue.trim()
})

const documentCodes = computed(() => normalizeExpensePrintDocumentCodes(route.query.documentCodes))
const isBatchMode = computed(() => !documentCode.value)

const pageTitle = computed(() => (
  isBatchMode.value
    ? '批量打印预览'
    : `单据打印预览 · ${documentCode.value}`
))

const pageSubtitle = computed(() => {
  if (isBatchMode.value) {
    return `${documentCodes.value.length || 0} 张单据将合并为一个 PDF，浏览器原生查看器可继续缩放、翻页、下载和打印。`
  }
  return '当前窗口使用浏览器原生 PDF Viewer 预览最终打印版式，可直接滚轮缩放、切换页码并打印。'
})

const emptyDescription = computed(() => (
  isBatchMode.value ? '缺少可打印的单据编号' : '缺少单据编号，无法加载打印预览'
))

const viewerSrc = computed(() => (
  pdfObjectUrl.value ? `${pdfObjectUrl.value}#toolbar=1&navpanes=0&view=FitH` : ''
))

watch(
  () => [documentCode.value, documentCodes.value.join(',')],
  () => {
    void loadPreview()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  revokePdfObjectUrl()
})

async function loadPreview() {
  if (isBatchMode.value && documentCodes.value.length === 0) {
    error.value = '缺少可打印的单据编号'
    revokePdfObjectUrl()
    return
  }

  if (!isBatchMode.value && !documentCode.value) {
    error.value = '缺少单据编号，无法加载打印预览'
    revokePdfObjectUrl()
    return
  }

  const currentRequest = ++requestVersion
  loading.value = true
  error.value = ''

  try {
    const response = isBatchMode.value
      ? await expenseApi.getBatchPrintPdf(documentCodes.value, orientation.value)
      : await expenseApi.getPrintPdf(documentCode.value, orientation.value)

    if (currentRequest !== requestVersion) {
      return
    }

    fileName.value = response.fileName
    updatePdfObjectUrl(window.URL.createObjectURL(response.blob))
  } catch (loadError: unknown) {
    if (currentRequest !== requestVersion) {
      return
    }

    revokePdfObjectUrl()
    error.value = resolveErrorMessage(
      loadError,
      isBatchMode.value ? '加载批量打印 PDF 失败' : '加载打印 PDF 失败'
    )
    ElMessage.error(error.value)
  } finally {
    if (currentRequest === requestVersion) {
      loading.value = false
    }
  }
}

function updatePdfObjectUrl(nextUrl: string) {
  revokePdfObjectUrl()
  pdfObjectUrl.value = nextUrl
}

function revokePdfObjectUrl() {
  if (!pdfObjectUrl.value) {
    return
  }
  window.URL.revokeObjectURL(pdfObjectUrl.value)
  pdfObjectUrl.value = ''
}

function downloadCurrentPdf() {
  if (!pdfObjectUrl.value) {
    return
  }

  const anchor = document.createElement('a')
  anchor.href = pdfObjectUrl.value
  anchor.download = fileName.value || (isBatchMode.value ? 'expense-documents-batch.pdf' : 'expense-document.pdf')
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}

function setOrientation(nextOrientation: ExpensePrintOrientation) {
  if (orientation.value === nextOrientation) {
    return
  }

  orientation.value = nextOrientation
  void router.replace({
    query: {
      ...route.query,
      orientation: nextOrientation
    }
  })
  void loadPreview()
}

function normalizeOrientation(rawValue: unknown): ExpensePrintOrientation {
  const normalized = String(Array.isArray(rawValue) ? rawValue[0] || '' : rawValue || '').trim().toUpperCase()
  return normalized === 'LANDSCAPE' ? 'LANDSCAPE' : 'PORTRAIT'
}

function resolveErrorMessage(loadError: unknown, fallback: string) {
  if (loadError && typeof loadError === 'object' && 'message' in loadError && typeof (loadError as { message?: unknown }).message === 'string') {
    return (loadError as { message: string }).message
  }
  return fallback
}
</script>

<style scoped>
.expense-pdf-preview {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(148, 163, 184, 0.16), transparent 22rem),
    linear-gradient(180deg, #f7f9fc 0%, #eef2f7 100%);
  color: #0f172a;
}

.expense-pdf-preview__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
}

.expense-pdf-preview__heading {
  min-width: 0;
}

.expense-pdf-preview__eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #64748b;
}

.expense-pdf-preview__title {
  margin: 6px 0 0;
  font-size: 28px;
  line-height: 1.1;
}

.expense-pdf-preview__subtitle {
  margin: 8px 0 0;
  max-width: 860px;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.expense-pdf-preview__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.expense-pdf-preview__orientation {
  display: inline-flex;
  gap: 10px;
  padding: 4px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.72);
}

.expense-pdf-preview__body {
  padding: 20px 24px 24px;
}

.expense-pdf-preview__frame {
  width: 100%;
  height: calc(100vh - 132px);
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
}

.expense-pdf-preview__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 132px);
  border: 1px dashed rgba(148, 163, 184, 0.4);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
}

@media (max-width: 960px) {
  .expense-pdf-preview__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .expense-pdf-preview__actions {
    justify-content: flex-start;
  }

  .expense-pdf-preview__frame,
  .expense-pdf-preview__empty {
    min-height: calc(100vh - 220px);
    height: calc(100vh - 220px);
  }
}
</style>
