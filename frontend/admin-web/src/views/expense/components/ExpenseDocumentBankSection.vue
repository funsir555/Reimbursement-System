<template>
  <el-card
    v-if="visible"
    class="expense-wb-panel"
    data-testid="detail-bank-section"
  >
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-lg font-semibold text-slate-800">银行付款 / 银行回单</p>
          <p class="mt-1 text-sm text-slate-500">这里展示银企直连付款状态，以及已回传到单据里的银行回单附件。</p>
        </div>
        <el-tag effect="plain">{{ paymentStatusLabel || '暂无状态' }}</el-tag>
      </div>
    </template>

    <div class="space-y-5">
      <div v-if="paymentSummaryItems.length" class="expense-wb-summary-strip">
        <div class="expense-wb-summary-grid">
          <div
            v-for="item in paymentSummaryItems"
            :key="item.key"
            class="expense-wb-summary-item"
          >
            <span class="expense-wb-summary-item__label">{{ item.label }}</span>
            <span class="expense-wb-summary-item__value">{{ item.value }}</span>
          </div>
        </div>
      </div>

      <div>
        <div class="mb-3 flex items-center justify-between gap-3">
          <p class="text-sm font-semibold text-slate-800">银行回单</p>
          <el-tag size="small" effect="plain">{{ receiptItems.length }} 份</el-tag>
        </div>
        <div v-if="receiptItems.length" class="space-y-3">
          <div
            v-for="receipt in receiptItems"
            :key="receipt.key"
            class="expense-wb-detail-card"
          >
            <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div class="space-y-2">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="text-base font-semibold text-slate-800">{{ receipt.fileName }}</p>
                  <el-tag effect="plain">{{ receipt.receivedAt }}</el-tag>
                </div>
                <p class="text-sm text-slate-500">{{ receipt.metaLine }}</p>
              </div>
              <div class="expense-wb-compact-actions">
                <el-button
                  v-if="receipt.previewHref"
                  plain
                  tag="a"
                  target="_blank"
                  :href="receipt.previewHref"
                >
                  预览回单
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无银行回单" :image-size="72" />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type {
  BankReceiptDisplay,
  SummaryItem
} from '../composables/useExpenseDocumentDetailDisplayOwner'

defineProps<{
  visible: boolean
  paymentStatusLabel: string
  paymentSummaryItems: SummaryItem[]
  receiptItems: BankReceiptDisplay[]
}>()
</script>
