<template>
  <div
    v-if="isPrintMode"
    class="expense-print-page expense-print-page--detail"
    data-testid="detail-print-mode"
  >
    <div v-loading="detailLoading || printLoading" class="expense-print-page__state">
      <ExpenseDocumentPrintSheet
        v-if="detail && !printLoadError"
        :detail="detail"
        :expense-details="printExpenseDetails"
        :vendor-option-map="vendorOptionMap"
        :payee-option-map="payeeOptionMap"
        :payee-account-option-map="payeeAccountOptionMap"
      />
      <el-empty v-else :description="printLoadError || detailLoadError || '暂无可打印单据数据'" :image-size="96" />
    </div>
  </div>
  <div v-else class="expense-wb-page expense-wb-page--detail detail-page space-y-6">
    <section class="expense-wb-hero detail-hero" data-testid="detail-hero">
      <div class="expense-wb-hero__content detail-hero__content">
        <div class="detail-hero__main">
          <button type="button" class="expense-wb-backlink detail-hero__backlink" data-testid="detail-back-button" @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </button>
          <h1 class="expense-wb-hero__title detail-hero__title">{{ detail?.documentTitle || route.params.documentCode }}</h1>
        </div>

        <div class="detail-hero__amount" data-testid="detail-hero-amount">
          <span class="detail-hero__amount-label">金额</span>
          <strong class="detail-hero__amount-value">{{ amountText }}</strong>
        </div>
      </div>
    </section>

    <div v-loading="detailLoading" class="detail-layout grid grid-cols-1 gap-6 xl:grid-cols-[3fr_1fr]">
      <template v-if="detail">
        <div class="detail-main-scroll space-y-6" data-testid="detail-main-scroll">
          <ExpenseDocumentReadonlyFormPanel
            :amount-text="amountText"
            :display="readonlyFormDisplay"
          />

          <ExpenseDocumentBindingPanels
            :panels="bindingPanels"
            :binding-count-suffix="bindingCountSuffix"
            :view-bound-document-label="viewBoundDocumentLabel"
            @open-bound-document="openBoundDocument"
          />

          <ExpenseDocumentExpenseDetailSection
            :cards="expenseDetailCards"
            :summary-items="expenseDetailSummaryItems"
            :workspace-visible="expenseDetailWorkspaceVisible"
            :workbench-display="expenseDetailWorkbenchDisplay"
            @select-detail="selectExpenseDetail"
            @open-detail="openExpenseDetail"
          />

          <ExpenseDocumentBankSection
            :visible="bankSectionVisible"
            :payment-status-label="detail?.bankPayment?.paymentStatusLabel || ''"
            :payment-summary-items="bankPaymentSummaryItems"
            :receipt-items="bankReceiptItems"
          />
        </div>

        <ExpenseDocumentApprovalPanel
          :summary-items="approvalSummaryItems"
          :approval-timeline-items="approvalTimelineItems"
          :approval-status-tag-type="approvalStatusTagType"
        />
      </template>

      <el-card v-else class="expense-wb-panel xl:col-span-2">
        <el-empty :description="detailLoadError || '暂无单据数据'" :image-size="96" />
      </el-card>
    </div>

    <div v-if="actionItems.length" class="detail-floating-bar">
      <div class="detail-floating-inner">
        <p v-if="disabledActionHint" class="detail-floating-hint">{{ disabledActionHint }}</p>
        <div class="detail-floating-actions" data-testid="detail-floating-actions">
          <div
            v-if="secondaryActionItems.length"
            class="detail-floating-actions__group detail-floating-actions__group--secondary"
            data-testid="detail-floating-secondary-actions"
          >
            <el-button
              v-for="action in secondaryActionItems"
              :key="action.key"
              :type="action.primary ? action.type || 'primary' : undefined"
              :plain="!action.primary"
              :disabled="action.disabled"
              class="detail-floating-button"
              @click="handleActionClick(action)"
            >
              {{ action.label }}
            </el-button>
          </div>
          <div
            v-if="primaryActionItems.length"
            class="detail-floating-actions__group detail-floating-actions__group--primary"
            data-testid="detail-floating-primary-actions"
          >
            <el-button
              v-for="action in primaryActionItems"
              :key="action.key"
              :type="action.primary ? action.type || 'primary' : undefined"
              :plain="!action.primary"
              :disabled="action.disabled"
              class="detail-floating-button"
              :class="{
                'detail-floating-button--colored': action.primary,
                'detail-floating-button--approve': action.key === 'approve'
              }"
              @click="handleActionClick(action)"
            >
              {{ action.label }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="commentDialogVisible" title="发表评论" width="560px">
      <div class="space-y-4">
        <el-input
          v-model="commentForm.comment"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="输入评论内容"
        />
        <div class="space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div class="flex flex-wrap items-center gap-3">
            <el-button plain @click="pickCommentFiles">添加附件名</el-button>
            <p class="text-xs text-slate-500">本次只保存附件文件名，不上传真实文件内容。</p>
          </div>
          <div v-if="commentForm.attachmentFileNames.length" class="flex flex-wrap gap-2">
            <el-tag
              v-for="name in commentForm.attachmentFileNames"
              :key="name"
              closable
              effect="plain"
              @close="removeCommentAttachment(name)"
            >
              {{ name }}
            </el-tag>
          </div>
          <el-empty v-else description="暂未添加附件名" :image-size="60" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="commentDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="commentSubmitting" @click="submitComment">发表评论</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="taskActionDialogVisible" :title="taskActionDialogTitle" width="560px">
      <div class="space-y-4">
        <el-input
          v-model="taskActionForm.comment"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          :placeholder="taskActionDialogPlaceholder"
        />
        <el-form-item v-if="taskActionMode === 'reject' && rejectTargetOptions.length" label="驳回到节点" class="!mb-0">
          <el-select v-model="taskActionForm.targetNodeKey" class="w-full" clearable placeholder="请选择目标审批节点">
            <el-option
              v-for="node in rejectTargetOptions"
              :key="node.nodeKey"
              :label="node.optionLabel || node.nodeName || node.nodeKey"
              :value="node.nodeKey"
            />
          </el-select>
        </el-form-item>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeTaskActionDialog">取消</el-button>
          <el-button type="primary" :loading="taskActionSubmitting" @click="submitTaskAction">{{ taskActionDialogConfirm }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="userActionDialogVisible" :title="userActionDialogTitle" width="520px">
      <div class="space-y-4">
        <el-form-item :label="userActionDialogLabel" required>
          <el-select
            v-model="userActionForm.targetUserId"
            class="w-full"
            filterable v-bind="globalFilterableSelectProps"
            remote
            clearable
            placeholder="搜索并选择处理人"
            :remote-method="loadActionUsers"
            :loading="userOptionsLoading"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.userId"
              :label="item.deptName ? `${item.name}（${item.deptName}）` : item.name"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="userActionForm.remark"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            :placeholder="userActionDialogPlaceholder"
          />
        </el-form-item>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeUserActionDialog">取消</el-button>
          <el-button type="primary" :loading="userActionSubmitting" @click="submitUserAction">
            {{ userActionDialogConfirm }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <input
      ref="commentFileInput"
      type="file"
      class="hidden"
      multiple
      @change="handleCommentFileChange"
    >
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { type ProcessFormDesignSchema } from '@/api'
import ExpenseDocumentApprovalPanel from './components/ExpenseDocumentApprovalPanel.vue'
import ExpenseDocumentBankSection from './components/ExpenseDocumentBankSection.vue'
import ExpenseDocumentBindingPanels from './components/ExpenseDocumentBindingPanels.vue'
import ExpenseDocumentExpenseDetailSection from './components/ExpenseDocumentExpenseDetailSection.vue'
import ExpenseDocumentPrintSheet from './components/ExpenseDocumentPrintSheet.vue'
import ExpenseDocumentReadonlyFormPanel from './components/ExpenseDocumentReadonlyFormPanel.vue'
import { buildAuthorizedAttachmentPreviewUrl } from './expenseInvoicePreview'
import { useExpenseDocumentDetailRuntime } from './composables/useExpenseDocumentDetailRuntime'
import { useExpenseDocumentDetailApprovalRuntime } from './composables/useExpenseDocumentDetailApprovalRuntime'
import { useExpenseDocumentDetailActionOwner } from './composables/useExpenseDocumentDetailActionOwner'
import { useExpenseDocumentDetailDisplayOwner } from './composables/useExpenseDocumentDetailDisplayOwner'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'


const route = useRoute()
const storedUser = (readStoredUser() || {}) as { userId?: number; permissionCodes?: string[] }
const currentUserId = computed(() => Number(storedUser.userId || 0))
const permissionCodes = computed(() => storedUser.permissionCodes || [])
const canApprovalView = computed(() =>
  hasPermission('expense:approval:view', permissionCodes.value)
  || hasPermission('expense:approval:approve', permissionCodes.value)
  || hasPermission('expense:approval:reject', permissionCodes.value)
)
const emptyExpenseDetailSchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }

const {
  detailLoading,
  navigationLoading,
  detail,
  detailLoadError,
  printLoading,
  printLoadError,
  printExpenseDetails,
  navigation,
  activeExpenseDetailNo,
  expenseDetailLoadingNo,
  activeExpenseDetail,
  activeExpenseDetailError,
  activeExpenseDetailSummary,
  vendorOptionMap,
  payeeOptionMap,
  payeeAccountOptionMap,
  amountText,
  isPrintMode,
  relatedBindingsExpanded,
  writeOffBindingsExpanded,
  relatedDocumentBindings,
  outboundRelatedBindings,
  inboundRelatedBindings,
  writeOffDocumentBindings,
  outboundWriteOffBindings,
  inboundWriteOffBindings,
  bindingCountSuffix,
  bindingInlineSeparator,
  expandText,
  collapseText,
  businessDocumentLabel,
  viewBoundDocumentLabel,
  relatedCardTitle,
  relatedCardDescription,
  relatedOutboundTitle,
  relatedInboundTitle,
  writeOffCardTitle,
  writeOffCardDescription,
  writeOffOutboundTitle,
  writeOffInboundTitle,
  documentCodeLabel,
  submitterLabel,
  sourceFieldLabel,
  bindingFieldLabel,
  writeOffSourceLabel,
  requestedAmountLabel,
  effectiveAmountLabel,
  remainingAmountLabel,
  unknownStatusLabel,
  relatedOutboundEmptyText,
  relatedInboundEmptyText,
  writeOffOutboundEmptyText,
  writeOffInboundEmptyText,
  goBack,
  buildReturnToQuery,
  openExpenseDetail,
  openBoundDocument,
  selectExpenseDetail,
  loadDetail,
  handlePrint,
  navigateDetail,
  refreshAfterAction,
  formatBindingMoney,
  writeOffSourceKindLabel,
  formatAttachmentSize,
  resolveErrorMessage,
  resolveExpenseDetailTypeLabel
} = useExpenseDocumentDetailRuntime({ canLoadNavigation: canApprovalView })

const displayRuntime = useExpenseDocumentDetailDisplayOwner({
  detail,
  vendorOptionMap,
  payeeOptionMap,
  payeeAccountOptionMap,
  relatedBindingsExpanded,
  writeOffBindingsExpanded,
  activeExpenseDetailNo,
  expenseDetailLoadingNo,
  activeExpenseDetail,
  activeExpenseDetailSummary,
  activeExpenseDetailError,
  relatedDocumentBindings,
  outboundRelatedBindings,
  inboundRelatedBindings,
  writeOffDocumentBindings,
  outboundWriteOffBindings,
  inboundWriteOffBindings,
  bindingCountSuffix,
  bindingInlineSeparator,
  expandText,
  collapseText,
  businessDocumentLabel,
  relatedCardTitle,
  relatedCardDescription,
  relatedOutboundTitle,
  relatedInboundTitle,
  writeOffCardTitle,
  writeOffCardDescription,
  writeOffOutboundTitle,
  writeOffInboundTitle,
  documentCodeLabel,
  submitterLabel,
  sourceFieldLabel,
  bindingFieldLabel,
  writeOffSourceLabel,
  requestedAmountLabel,
  effectiveAmountLabel,
  remainingAmountLabel,
  unknownStatusLabel,
  relatedOutboundEmptyText,
  relatedInboundEmptyText,
  writeOffOutboundEmptyText,
  writeOffInboundEmptyText,
  emptyExpenseDetailSchema,
  resolveExpenseDetailTypeLabel,
  formatBindingMoney,
  writeOffSourceKindLabel,
  formatAttachmentSize,
  buildAuthorizedAttachmentPreviewUrl
})

const {
  readonlyFormDisplay,
  bindingPanels,
  expenseDetailCards,
  expenseDetailWorkspaceVisible,
  expenseDetailSummaryItems,
  expenseDetailWorkbenchDisplay,
  bankSectionVisible,
  bankPaymentSummaryItems,
  bankReceiptItems,
  approvalSummaryItems
} = displayRuntime

const approvalRuntime = useExpenseDocumentDetailApprovalRuntime({
  detail,
  navigation,
  currentUserId,
  canApprovalView
})

const {
  approvableTasks,
  rejectTargetOptions,
  approvalTimelineItems,
  actionItems,
  secondaryActionItems,
  primaryActionItems,
  disabledActionHint,
  approvalStatusTagType
} = approvalRuntime

const actionRuntime = useExpenseDocumentDetailActionOwner({
  detail,
  navigation,
  approvableTasks,
  rejectTargetOptions,
  permissionCodes,
  buildReturnToQuery,
  loadDetail,
  handlePrint,
  navigateDetail,
  refreshAfterAction,
  resolveErrorMessage
})

const {
  commentDialogVisible,
  commentSubmitting,
  commentFileInput,
  commentForm,
  taskActionDialogVisible,
  taskActionMode,
  taskActionSubmitting,
  taskActionForm,
  userActionDialogVisible,
  userActionMode,
  userActionSubmitting,
  userOptionsLoading,
  userOptions,
  userActionForm,
  taskActionDialogTitle,
  taskActionDialogConfirm,
  taskActionDialogPlaceholder,
  userActionDialogTitle,
  userActionDialogLabel,
  userActionDialogConfirm,
  userActionDialogPlaceholder,
  closeTaskActionDialog,
  closeUserActionDialog,
  handleActionClick,
  submitTaskAction,
  submitComment,
  pickCommentFiles,
  handleCommentFileChange,
  removeCommentAttachment,
  loadActionUsers,
  submitUserAction
} = actionRuntime

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

.detail-page {
  padding-bottom: 132px;
}

.detail-hero {
  padding: 18px 22px;
  border-radius: 28px;
}

.detail-hero::before {
  top: -120px;
  right: -56px;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0) 72%);
}

.detail-hero::after {
  bottom: -170px;
  left: -80px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(186, 230, 253, 0.2) 0%, rgba(186, 230, 253, 0) 74%);
}

.detail-hero__content {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.detail-hero__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-hero__backlink {
  font-size: 13px;
}

.detail-hero__title {
  margin: 0;
  font-size: clamp(22px, 2.4vw, 28px);
  line-height: 1.18;
  word-break: break-word;
}

.detail-hero__amount {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  min-width: 176px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  padding: 12px 16px;
  backdrop-filter: blur(12px);
}

.detail-hero__amount-label {
  font-size: 12px;
  color: rgba(224, 242, 254, 0.88);
}

.detail-hero__amount-value {
  font-size: clamp(22px, 2.2vw, 28px);
  line-height: 1.1;
  font-weight: 700;
  color: #ffffff;
  white-space: nowrap;
}

.detail-main-scroll,
.approval-scroll {
  max-height: calc(100vh - 240px);
  overflow-y: auto;
  padding-right: 6px;
}

.detail-main-scroll::-webkit-scrollbar,
.approval-scroll::-webkit-scrollbar {
  width: 8px;
}

.detail-main-scroll::-webkit-scrollbar-thumb,
.approval-scroll::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.7);
  border-radius: 999px;
}

.detail-main-scroll::-webkit-scrollbar-track,
.approval-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.7);
  border-radius: 999px;
}

.binding-card-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.binding-card-toggle {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 13px;
  line-height: 1.5;
  color: rgb(37 99 235);
  cursor: pointer;
}

.binding-card-toggle:hover {
  color: rgb(29 78 216);
}

.binding-card-inline-empty {
  margin: 0;
  padding: 12px 14px;
  border: 1px dashed rgba(203, 213, 225, 0.9);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.82);
  font-size: 13px;
  line-height: 1.6;
  color: rgb(100 116 139);
}

.approval-node-status-list {
  display: grid;
  gap: 12px;
}

.approval-node-status-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
}

.approval-node-status-card--pending {
  border-color: rgba(251, 191, 36, 0.6);
  background: rgba(255, 251, 235, 0.96);
}

.approval-node-status-card--future {
  border-style: dashed;
}

.approval-node-status-card__content {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.approval-node-status-card__time {
  flex-shrink: 0;
  font-size: 12px;
  line-height: 1.5;
  color: rgb(100 116 139);
}

.detail-floating-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 24px;
  z-index: 30;
  display: flex;
  justify-content: center;
  padding: 0 20px;
}

.detail-floating-inner {
  width: min(1120px, 100%);
  border: 1px solid rgba(219, 234, 254, 0.92);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.96) 100%);
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(16px);
  padding: 20px 24px;
}

.detail-floating-hint {
  margin-bottom: 14px;
  font-size: 16px;
  line-height: 1.45;
  color: rgb(180 83 9);
}

.detail-floating-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
}

.detail-floating-actions__group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.detail-floating-actions__group--secondary {
  flex: 0 1 auto;
  min-width: auto;
}

.detail-floating-actions__group--primary {
  flex: 0 1 auto;
}

:deep(.detail-floating-button) {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 14px;
  font-size: 15px;
  line-height: 1.1;
}

:deep(.detail-floating-button span) {
  font-size: inherit;
}

@media (max-width: 1279px) {
  .detail-page {
    padding-bottom: 168px;
  }

  .detail-hero {
    padding: 16px 18px;
  }

  .detail-hero__content {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
  }

  .detail-hero__title {
    font-size: 22px;
  }

  .detail-hero__amount {
    min-width: 0;
    align-items: flex-start;
  }

  .detail-main-scroll,
  .approval-scroll {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }

  .detail-floating-bar {
    bottom: 16px;
    padding: 0 12px;
  }

  .detail-floating-inner {
    padding: 18px 16px;
  }

  .detail-floating-hint {
    margin-bottom: 12px;
    font-size: 14px;
  }

  .detail-floating-actions {
    gap: 10px;
  }

  .detail-floating-actions__group {
    gap: 10px;
  }

  .detail-floating-actions__group--secondary,
  .detail-floating-actions__group--primary {
    flex-basis: 100%;
    min-width: 0;
  }

  .detail-floating-actions__group--secondary,
  .detail-floating-actions__group--primary {
    justify-content: flex-end;
  }

  :deep(.detail-floating-button) {
    min-height: 34px;
    padding: 0 14px;
    font-size: 14px;
  }
}

@media print {
  .expense-print-page {
    background: #ffffff;
    padding: 0;
  }

  .expense-print-page__state {
    max-width: none;
  }
}
</style>
