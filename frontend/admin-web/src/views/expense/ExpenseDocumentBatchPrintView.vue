<template>
  <div class="expense-print-page expense-print-page--batch" data-testid="expense-batch-print-page">
    <div v-loading="loading" class="expense-print-page__state">
      <template v-if="bundles.length">
        <ExpenseDocumentPrintSheet
          v-for="(bundle, index) in bundles"
          :key="bundle.detail.documentCode"
          class="expense-print-page__sheet"
          :class="{ 'expense-print-page__sheet--break': index < bundles.length - 1 }"
          :detail="bundle.detail"
          :expense-details="bundle.expenseDetails"
          :vendor-option-map="vendorOptionMap"
          :payee-option-map="payeeOptionMap"
          :payee-account-option-map="payeeAccountOptionMap"
        />
      </template>
      <el-empty v-else :description="error || '暂无可打印单据'" :image-size="92" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ExpenseDocumentPrintSheet from './components/ExpenseDocumentPrintSheet.vue'
import { collectPrintSchemas, loadExpenseDocumentPrintBundle, normalizeExpensePrintDocumentCodes, type ExpenseDocumentPrintBundle } from './expensePrintSupport'
import { useReadonlyPayeeLookups } from './useReadonlyPayeeLookups'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const bundles = ref<ExpenseDocumentPrintBundle[]>([])
const documentCodes = computed(() => normalizeExpensePrintDocumentCodes(route.query.documentCodes))
const { vendorOptionMap, payeeOptionMap, payeeAccountOptionMap, syncReadonlyPayeeLookupsBatch } = useReadonlyPayeeLookups()
let lastPrintedSignature = ''

watch(
  () => route.query.documentCodes,
  () => {
    void loadBundles()
  },
  { immediate: true }
)

async function loadBundles() {
  const codes = documentCodes.value
  loading.value = true
  error.value = ''
  bundles.value = []

  if (codes.length === 0) {
    error.value = '缺少可打印的单据编号'
    loading.value = false
    return
  }

  const signature = codes.join(',')

  try {
    const loadedBundles = await Promise.all(codes.map((documentCode) => loadExpenseDocumentPrintBundle(documentCode)))
    await syncReadonlyPayeeLookupsBatch(collectPrintSchemas(loadedBundles))
    bundles.value = loadedBundles
    await triggerPrint(signature)
  } catch (loadError: unknown) {
    error.value = resolveErrorMessage(loadError, '加载批量打印数据失败')
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

async function triggerPrint(signature: string) {
  if (!signature || lastPrintedSignature === signature) {
    return
  }
  lastPrintedSignature = signature
  await nextTick()
  window.print()
}

function resolveErrorMessage(loadError: unknown, fallback: string) {
  if (loadError && typeof loadError === 'object' && 'message' in loadError && typeof (loadError as { message?: unknown }).message === 'string') {
    return (loadError as { message: string }).message
  }
  return fallback
}
</script>

<style scoped>
.expense-print-page {
  min-height: 100vh;
  background: #f4f7fb;
  padding: 24px;
}

.expense-print-page__state {
  max-width: 1200px;
  margin: 0 auto;
}

.expense-print-page__sheet + .expense-print-page__sheet {
  margin-top: 22px;
}

@media print {
  .expense-print-page {
    background: #ffffff;
    padding: 0;
  }

  .expense-print-page__state {
    max-width: none;
  }

  .expense-print-page__sheet + .expense-print-page__sheet {
    margin-top: 0;
  }

  .expense-print-page__sheet--break {
    page-break-after: always;
    break-after: page;
  }
}
</style>
