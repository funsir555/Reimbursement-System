<template>
  <article class="expense-print-sheet" data-testid="expense-print-sheet">
    <header class="expense-print-sheet__header">
      <div>
        <p class="expense-print-sheet__eyebrow">报销单打印版</p>
        <h1 class="expense-print-sheet__title">{{ detail.documentTitle || detail.documentCode }}</h1>
        <p class="expense-print-sheet__subtitle">
          单据编号：{{ detail.documentCode }}
          <span class="expense-print-sheet__divider">/</span>
          模板：{{ detail.templateName || '-' }}
        </p>
      </div>
      <div class="expense-print-sheet__amount">
        <span class="expense-print-sheet__amount-label">金额</span>
        <strong class="expense-print-sheet__amount-value">{{ amountText }}</strong>
      </div>
    </header>

    <section class="expense-print-sheet__panel">
      <div class="expense-print-sheet__section-head">
        <h2>单据摘要</h2>
      </div>
      <div class="expense-print-sheet__summary-grid">
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">单据状态</span>
          <span class="expense-print-sheet__summary-value">{{ detail.statusLabel || detail.status || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">提单人</span>
          <span class="expense-print-sheet__summary-value">{{ detail.submitterName || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">提交时间</span>
          <span class="expense-print-sheet__summary-value">{{ detail.submittedAt || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">当前节点</span>
          <span class="expense-print-sheet__summary-value">{{ detail.currentNodeName || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">模板类型</span>
          <span class="expense-print-sheet__summary-value">{{ detail.templateType || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">完成时间</span>
          <span class="expense-print-sheet__summary-value">{{ detail.finishedAt || '-' }}</span>
        </div>
      </div>
      <div v-if="detail.documentReason" class="expense-print-sheet__reason">
        <span class="expense-print-sheet__summary-label">事由</span>
        <p>{{ detail.documentReason }}</p>
      </div>
    </section>

    <section class="expense-print-sheet__panel">
      <div class="expense-print-sheet__section-head">
        <h2>单据表单</h2>
      </div>
      <ExpenseFormReadonlyRenderer
        :schema="detail.formSchemaSnapshot"
        :form-data="detail.formData"
        :company-options="detail.companyOptions"
        :department-options="detail.departmentOptions"
        :summary-visible="false"
        :vendor-option-map="vendorOptionMap"
        :payee-option-map="payeeOptionMap"
        :payee-account-option-map="payeeAccountOptionMap"
      />
    </section>

    <section class="expense-print-sheet__panel">
      <div class="expense-print-sheet__section-head">
        <h2>费用明细</h2>
        <span>{{ expenseDetails.length }} 条</span>
      </div>
      <div v-if="expenseDetails.length" class="expense-print-sheet__details">
        <section
          v-for="item in expenseDetails"
          :key="`${detail.documentCode}-${item.detailNo}`"
          class="expense-print-sheet__detail-card"
        >
          <div class="expense-print-sheet__detail-head">
            <div>
              <h3>{{ item.detailTitle || item.detailNo }}</h3>
              <p>
                明细编号：{{ item.detailNo }}
                <span class="expense-print-sheet__divider">/</span>
                排序：{{ item.sortOrder || '-' }}
                <span class="expense-print-sheet__divider">/</span>
                更新时间：{{ item.updatedAt || item.createdAt || '-' }}
              </p>
            </div>
            <div class="expense-print-sheet__detail-tags">
              <span class="expense-print-sheet__tag">
                {{ resolvePrintExpenseDetailTypeLabel(item.detailType, item.detailTypeLabel) }}
              </span>
              <span v-if="item.enterpriseModeLabel" class="expense-print-sheet__tag expense-print-sheet__tag--warning">
                {{ item.enterpriseModeLabel }}
              </span>
            </div>
          </div>

          <ExpenseFormReadonlyRenderer
            :schema="item.schemaSnapshot"
            :form-data="item.formData"
            :company-options="detail.companyOptions"
            :department-options="detail.departmentOptions"
            :detail-type="item.detailType"
            :default-business-scenario="item.businessSceneMode || ''"
            :summary-visible="false"
            :vendor-option-map="vendorOptionMap"
            :payee-option-map="payeeOptionMap"
            :payee-account-option-map="payeeAccountOptionMap"
          />
        </section>
      </div>
      <p v-else class="expense-print-sheet__empty">暂无费用明细</p>
    </section>

    <section v-if="detail.bankPayment || detail.bankReceipts?.length" class="expense-print-sheet__panel">
      <div class="expense-print-sheet__section-head">
        <h2>支付与回单</h2>
      </div>
      <div v-if="detail.bankPayment" class="expense-print-sheet__summary-grid">
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">支付状态</span>
          <span class="expense-print-sheet__summary-value">{{ detail.bankPayment.paymentStatusLabel || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">直连账户</span>
          <span class="expense-print-sheet__summary-value">{{ detail.bankPayment.companyBankAccountName || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">回单状态</span>
          <span class="expense-print-sheet__summary-value">{{ detail.bankPayment.receiptStatusLabel || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">支付时间</span>
          <span class="expense-print-sheet__summary-value">{{ detail.bankPayment.paidAt || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">银行流水号</span>
          <span class="expense-print-sheet__summary-value">{{ detail.bankPayment.bankFlowNo || '-' }}</span>
        </div>
        <div class="expense-print-sheet__summary-item">
          <span class="expense-print-sheet__summary-label">支付方式</span>
          <span class="expense-print-sheet__summary-value">{{ detail.bankPayment.manualPaid ? '手动支付' : '银行回调' }}</span>
        </div>
      </div>

      <div class="expense-print-sheet__receipt-list">
        <div class="expense-print-sheet__section-subhead">
          <h3>银行回单</h3>
          <span>{{ detail.bankReceipts?.length || 0 }} 份</span>
        </div>
        <div v-if="detail.bankReceipts?.length" class="expense-print-sheet__receipt-items">
          <div
            v-for="receipt in detail.bankReceipts"
            :key="receipt.attachmentId || receipt.fileName"
            class="expense-print-sheet__receipt-item"
          >
            <p class="expense-print-sheet__receipt-name">{{ receipt.fileName }}</p>
            <p class="expense-print-sheet__receipt-meta">
              {{ receipt.receivedAt || '-' }}
              <span class="expense-print-sheet__divider">/</span>
              {{ receipt.contentType || '-' }}
            </p>
          </div>
        </div>
        <p v-else class="expense-print-sheet__empty">暂无银行回单</p>
      </div>
    </section>

    <section class="expense-print-sheet__panel">
      <div class="expense-print-sheet__section-head">
        <h2>审批轨迹</h2>
        <span>{{ timelineItems.length }} 条</span>
      </div>
      <div v-if="timelineItems.length" class="expense-print-sheet__timeline">
        <div
          v-for="item in timelineItems"
          :key="item.key"
          class="expense-print-sheet__timeline-item"
        >
          <div class="expense-print-sheet__timeline-time">{{ item.timestamp || '-' }}</div>
          <div class="expense-print-sheet__timeline-content">
            <p class="expense-print-sheet__timeline-title">{{ item.title }}</p>
            <p v-if="item.description" class="expense-print-sheet__timeline-desc">{{ item.description }}</p>
          </div>
        </div>
      </div>
      <p v-else class="expense-print-sheet__empty">暂无审批轨迹</p>
    </section>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExpenseCreatePayeeAccountOption, ExpenseCreatePayeeOption, ExpenseCreateVendorOption, ExpenseDetailInstanceDetail, ExpenseDocumentDetail } from '@/api'
import ExpenseFormReadonlyRenderer from './ExpenseFormReadonlyRenderer.vue'
import { resolvePrintExpenseDetailTypeLabel, buildPrintTimelineItems } from '../expensePrintSupport'
import { formatMoney } from '@/utils/money'

const props = withDefaults(defineProps<{
  detail: ExpenseDocumentDetail
  expenseDetails?: ExpenseDetailInstanceDetail[]
  vendorOptionMap?: Record<string, ExpenseCreateVendorOption>
  payeeOptionMap?: Record<string, ExpenseCreatePayeeOption>
  payeeAccountOptionMap?: Record<string, ExpenseCreatePayeeAccountOption>
}>(), {
  expenseDetails: () => [],
  vendorOptionMap: () => ({}),
  payeeOptionMap: () => ({}),
  payeeAccountOptionMap: () => ({})
})

const amountText = computed(() => `￥${formatMoney(props.detail.totalAmount || 0)}`)
const timelineItems = computed(() => buildPrintTimelineItems(props.detail))
</script>

<style scoped>
.expense-print-sheet {
  width: 100%;
  border: 1px solid #d9e2ec;
  border-radius: 24px;
  background: #ffffff;
  padding: 28px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  color: #0f172a;
}

.expense-print-sheet__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.expense-print-sheet__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #64748b;
}

.expense-print-sheet__title {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 700;
}

.expense-print-sheet__subtitle {
  margin: 10px 0 0;
  font-size: 14px;
  color: #475569;
}

.expense-print-sheet__divider {
  margin: 0 8px;
  color: #94a3b8;
}

.expense-print-sheet__amount {
  min-width: 180px;
  border-radius: 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  padding: 14px 18px;
  text-align: right;
}

.expense-print-sheet__amount-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.expense-print-sheet__amount-value {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.1;
}

.expense-print-sheet__panel {
  margin-top: 22px;
  break-inside: avoid;
}

.expense-print-sheet__section-head,
.expense-print-sheet__section-subhead,
.expense-print-sheet__detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.expense-print-sheet__section-head {
  margin-bottom: 14px;
}

.expense-print-sheet__section-head h2,
.expense-print-sheet__section-subhead h3,
.expense-print-sheet__detail-head h3 {
  margin: 0;
  font-size: 20px;
  line-height: 1.3;
  font-weight: 700;
}

.expense-print-sheet__section-head span,
.expense-print-sheet__section-subhead span {
  font-size: 13px;
  color: #64748b;
}

.expense-print-sheet__summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.expense-print-sheet__summary-item,
.expense-print-sheet__receipt-item {
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #f8fafc;
  padding: 14px 16px;
}

.expense-print-sheet__summary-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.expense-print-sheet__summary-value {
  display: block;
  margin-top: 8px;
  font-size: 15px;
  line-height: 1.5;
  font-weight: 600;
}

.expense-print-sheet__reason {
  margin-top: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #f8fafc;
  padding: 14px 16px;
}

.expense-print-sheet__reason p {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
}

.expense-print-sheet__details {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.expense-print-sheet__detail-card {
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #ffffff;
  padding: 18px;
}

.expense-print-sheet__detail-head {
  margin-bottom: 14px;
}

.expense-print-sheet__detail-head p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #64748b;
}

.expense-print-sheet__detail-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.expense-print-sheet__tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  padding: 4px 10px;
  font-size: 12px;
  color: #334155;
  background: #f8fafc;
}

.expense-print-sheet__tag--warning {
  border-color: #fbbf24;
  background: #fef3c7;
  color: #92400e;
}

.expense-print-sheet__receipt-list {
  margin-top: 16px;
}

.expense-print-sheet__receipt-items {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.expense-print-sheet__receipt-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.expense-print-sheet__receipt-meta {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
}

.expense-print-sheet__timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.expense-print-sheet__timeline-item {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 16px;
  border-left: 3px solid #bfdbfe;
  padding-left: 14px;
}

.expense-print-sheet__timeline-time {
  font-size: 12px;
  color: #64748b;
}

.expense-print-sheet__timeline-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.expense-print-sheet__timeline-desc {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
  white-space: pre-wrap;
}

.expense-print-sheet__empty {
  margin: 0;
  border: 1px dashed #cbd5e1;
  border-radius: 18px;
  background: #f8fafc;
  padding: 18px;
  font-size: 14px;
  color: #64748b;
}

@media (max-width: 960px) {
  .expense-print-sheet {
    padding: 20px;
  }

  .expense-print-sheet__header,
  .expense-print-sheet__section-head,
  .expense-print-sheet__section-subhead,
  .expense-print-sheet__detail-head {
    flex-direction: column;
  }

  .expense-print-sheet__amount {
    width: 100%;
    text-align: left;
  }

  .expense-print-sheet__summary-grid,
  .expense-print-sheet__receipt-items {
    grid-template-columns: 1fr;
  }

  .expense-print-sheet__timeline-item {
    grid-template-columns: 1fr;
  }
}

@media print {
  .expense-print-sheet {
    border: none;
    border-radius: 0;
    box-shadow: none;
    padding: 0;
  }

  .expense-print-sheet__panel,
  .expense-print-sheet__detail-card,
  .expense-print-sheet__summary-item,
  .expense-print-sheet__receipt-item,
  .expense-print-sheet__reason,
  .expense-print-sheet__empty {
    break-inside: avoid;
  }
}
</style>
