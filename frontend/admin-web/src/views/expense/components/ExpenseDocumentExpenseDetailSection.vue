<template>
  <el-card v-if="cards.length" class="expense-wb-panel">
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-lg font-semibold text-slate-800">费用明细</p>
          <p class="mt-1 text-sm text-slate-500">这里展示随单据一并提交并归档的费用明细快照，点击任一明细可在当前页展开其发票工作区。</p>
        </div>
        <el-tag effect="plain">{{ cards.length }} 条</el-tag>
      </div>
    </template>

    <div class="space-y-4">
      <div
        v-for="item in cards"
        :key="item.detailNo"
        class="expense-wb-detail-card expense-wb-detail-card--clickable"
        :class="{ 'expense-wb-detail-card--selected': item.isSelected }"
        data-testid="expense-detail-card"
        @click="emit('select-detail', item.detailNo)"
      >
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <p class="text-base font-semibold text-slate-800">{{ item.title }}</p>
              <el-tag effect="plain">{{ item.typeLabel }}</el-tag>
              <el-tag v-if="item.enterpriseModeLabel" type="warning" effect="plain">{{ item.enterpriseModeLabel }}</el-tag>
              <el-tag v-if="item.isSelected" type="primary" effect="plain">发票工作区已展开</el-tag>
            </div>
            <p class="mt-2 text-sm text-slate-500">{{ item.metaLine }}</p>
          </div>

          <div class="expense-wb-compact-actions">
            <el-button plain @click.stop="emit('select-detail', item.detailNo)">查看发票</el-button>
            <el-button plain @click.stop="emit('open-detail', item.detailNo)">查看明细</el-button>
          </div>
        </div>
      </div>

      <div v-if="workspaceVisible" class="expense-document-invoice-shell">
        <div class="expense-wb-summary-strip">
          <div class="expense-wb-summary-grid">
            <div
              v-for="item in summaryItems"
              :key="item.key"
              class="expense-wb-summary-item"
            >
              <span class="expense-wb-summary-item__label">{{ item.label }}</span>
              <span class="expense-wb-summary-item__value">{{ item.value }}</span>
            </div>
          </div>
        </div>

        <div class="mt-6">
          <ExpenseInvoiceWorkbench
            :schema="workbenchDisplay.schema"
            :form-data="workbenchDisplay.formData"
            :detail-title="workbenchDisplay.detailTitle"
            :detail-no="workbenchDisplay.detailNo"
            :loading="workbenchDisplay.loading"
            :error-message="workbenchDisplay.errorMessage"
            result-mode="verification-placeholder"
          />
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { ProcessFormDesignSchema } from '@/api'
import type {
  ExpenseDetailCardDisplay,
  SummaryItem
} from '../composables/useExpenseDocumentDetailDisplayOwner'
import ExpenseInvoiceWorkbench from './ExpenseInvoiceWorkbench.vue'

type ExpenseDetailWorkbenchDisplay = {
  schema: ProcessFormDesignSchema
  formData: Record<string, unknown>
  detailTitle: string
  detailNo: string
  loading: boolean
  errorMessage: string
}

defineProps<{
  cards: ExpenseDetailCardDisplay[]
  summaryItems: SummaryItem[]
  workspaceVisible: boolean
  workbenchDisplay: ExpenseDetailWorkbenchDisplay
}>()

const emit = defineEmits<{
  'select-detail': [detailNo: string]
  'open-detail': [detailNo: string]
}>()
</script>
