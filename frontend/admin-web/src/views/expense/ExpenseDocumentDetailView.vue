<template>
  <div class="expense-wb-page expense-wb-page--detail detail-page space-y-6">
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
          <el-card class="expense-wb-panel">
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">单据表单</p>
                  <p class="mt-1 text-sm text-slate-500">根据提交时保存的表单快照回看单据内容。</p>
                </div>
                <el-tag effect="plain">金额：{{ amountText }}</el-tag>
              </div>
            </template>

            <ExpenseFormReadonlyRenderer
              v-if="detail"
              :schema="detail.formSchemaSnapshot"
              :form-data="detail.formData"
              :company-options="detail.companyOptions"
              :department-options="detail.departmentOptions"
              :vendor-option-map="vendorOptionMap"
              :payee-option-map="payeeOptionMap"
              :payee-account-option-map="payeeAccountOptionMap"
            />
            <el-empty v-else description="暂无单据数据" :image-size="96" />
          </el-card>

          <el-card v-if="detail?.expenseDetails?.length" class="expense-wb-panel">
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">费用明细</p>
                  <p class="mt-1 text-sm text-slate-500">这里展示随单据一并提交并归档的费用明细快照，点击任一明细可在当前页展开其发票工作区。</p>
                </div>
                <el-tag effect="plain">{{ detail?.expenseDetails?.length || 0 }} 条</el-tag>
              </div>
            </template>

            <div class="space-y-4">
              <div
                v-for="item in detail?.expenseDetails || []"
                :key="item.detailNo"
                class="expense-wb-detail-card expense-wb-detail-card--clickable"
                :class="{ 'expense-wb-detail-card--selected': activeExpenseDetailNo === item.detailNo }"
                data-testid="expense-detail-card"
                @click="selectExpenseDetail(item.detailNo)"
              >
                <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-base font-semibold text-slate-800">{{ item.detailTitle || item.detailNo }}</p>
                      <el-tag effect="plain">{{ item.detailTypeLabel }}</el-tag>
                      <el-tag v-if="item.enterpriseModeLabel" type="warning" effect="plain">{{ item.enterpriseModeLabel }}</el-tag>
                      <el-tag v-if="activeExpenseDetailNo === item.detailNo" type="primary" effect="plain">发票工作区已展开</el-tag>
                    </div>
                    <p class="mt-2 text-sm text-slate-500">
                      明细编号：{{ item.detailNo }} ｜ 排序：{{ item.sortOrder || '-' }} ｜ 创建时间：{{ item.createdAt || '-' }}
                    </p>
                  </div>

                  <div class="expense-wb-compact-actions">
                    <el-button plain @click.stop="selectExpenseDetail(item.detailNo)">查看发票</el-button>
                    <el-button plain @click.stop="openExpenseDetail(item.detailNo)">查看明细</el-button>
                  </div>
                </div>
              </div>

              <div v-if="activeExpenseDetailNo" class="expense-document-invoice-shell">
                <div class="expense-wb-summary-strip">
                  <div class="expense-wb-summary-grid">
                    <div class="expense-wb-summary-item">
                      <span class="expense-wb-summary-item__label">当前明细</span>
                      <span class="expense-wb-summary-item__value">{{ activeExpenseDetail?.detailTitle || activeExpenseDetailSummary?.detailTitle || activeExpenseDetailNo }}</span>
                    </div>
                    <div class="expense-wb-summary-item">
                      <span class="expense-wb-summary-item__label">明细编号</span>
                      <span class="expense-wb-summary-item__value">{{ activeExpenseDetail?.detailNo || activeExpenseDetailNo }}</span>
                    </div>
                    <div class="expense-wb-summary-item">
                      <span class="expense-wb-summary-item__label">加载状态</span>
                      <span class="expense-wb-summary-item__value">
                        {{
                          expenseDetailLoadingNo === activeExpenseDetailNo && !activeExpenseDetail
                            ? '加载中'
                            : activeExpenseDetailError
                              ? '加载失败'
                              : '已就绪'
                        }}
                      </span>
                    </div>
                  </div>
                </div>

                <div class="mt-6">
                  <ExpenseInvoiceWorkbench
                    :schema="activeExpenseDetail?.schemaSnapshot || emptyExpenseDetailSchema"
                    :form-data="activeExpenseDetail?.formData || {}"
                    :detail-title="activeExpenseDetail?.detailTitle || activeExpenseDetailSummary?.detailTitle || ''"
                    :detail-no="activeExpenseDetail?.detailNo || activeExpenseDetailNo"
                    :loading="expenseDetailLoadingNo === activeExpenseDetailNo && !activeExpenseDetail"
                    :error-message="activeExpenseDetailError"
                  />
                </div>
              </div>
            </div>
          </el-card>

          <el-card
            v-if="detail?.bankPayment || detail?.bankReceipts?.length"
            class="expense-wb-panel"
            data-testid="detail-bank-section"
          >
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">银行付款 / 银行回单</p>
                  <p class="mt-1 text-sm text-slate-500">这里展示银企直连付款状态，以及已回传到单据里的银行回单附件。</p>
                </div>
                <el-tag effect="plain">{{ detail?.bankPayment?.paymentStatusLabel || '暂无状态' }}</el-tag>
              </div>
            </template>

            <div class="space-y-5">
              <div v-if="detail?.bankPayment" class="expense-wb-summary-strip">
                <div class="expense-wb-summary-grid">
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">付款状态</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.paymentStatusLabel || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">直连账户</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.companyBankAccountName || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">回单状态</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.receiptStatusLabel || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">支付时间</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.paidAt || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">银行流水号</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.bankFlowNo || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">支付方式</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.manualPaid ? '手动支付' : '银行回调' }}</span>
                  </div>
                </div>
              </div>

              <div>
                <div class="mb-3 flex items-center justify-between gap-3">
                  <p class="text-sm font-semibold text-slate-800">银行回单</p>
                  <el-tag size="small" effect="plain">{{ detail?.bankReceipts?.length || 0 }} 份</el-tag>
                </div>
                <div v-if="detail?.bankReceipts?.length" class="space-y-3">
                  <div
                    v-for="receipt in detail.bankReceipts"
                    :key="receipt.attachmentId || receipt.fileName"
                    class="expense-wb-detail-card"
                  >
                    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                      <div class="space-y-2">
                        <div class="flex flex-wrap items-center gap-2">
                          <p class="text-base font-semibold text-slate-800">{{ receipt.fileName }}</p>
                          <el-tag effect="plain">{{ receipt.receivedAt || '待生成' }}</el-tag>
                        </div>
                        <p class="text-sm text-slate-500">
                          {{ receipt.contentType || '未知类型' }} · {{ formatAttachmentSize(receipt.fileSize) }}
                        </p>
                      </div>
                      <div class="expense-wb-compact-actions">
                        <el-button
                          v-if="receipt.previewUrl"
                          plain
                          tag="a"
                          target="_blank"
                          :href="buildAuthorizedAttachmentPreviewUrl(receipt.previewUrl)"
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
        </div>

        <el-card class="expense-wb-panel">
          <template #header>
            <div>
              <p class="text-lg font-semibold text-slate-800">审批流程</p>
              <p class="mt-1 text-sm text-slate-500">真实任务状态与审批轨迹</p>
            </div>
          </template>

          <div class="approval-scroll space-y-5">
            <div class="expense-wb-summary-strip">
              <div class="expense-wb-summary-grid">
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">当前节点</span>
                  <span class="expense-wb-summary-item__value">{{ detail.currentNodeName || '未开始' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">模板名称</span>
                  <span class="expense-wb-summary-item__value">{{ detail.templateName || '-' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">当前状态</span>
                  <span class="expense-wb-summary-item__value">{{ detail.statusLabel || '-' }}</span>
                </div>
              </div>
            </div>

            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <p class="text-sm font-semibold text-slate-800">审批轨迹</p>
                <el-tag size="small" effect="plain">{{ approvalTimelineItems.length }} 条</el-tag>
              </div>

              <el-timeline v-if="approvalTimelineItems.length">
                <el-timeline-item
                  v-for="item in approvalTimelineItems"
                  :key="item.key"
                  :timestamp="item.timestamp"
                  placement="top"
                >
                  <div class="space-y-2">
                    <p class="text-sm font-semibold text-slate-800">{{ item.title }}</p>
                    <p v-if="item.description" class="text-xs leading-6 text-slate-500">{{ item.description }}</p>
                    <div v-if="item.attachmentNames.length" class="flex flex-wrap gap-2">
                      <el-tag
                        v-for="name in item.attachmentNames"
                        :key="name"
                        size="small"
                        effect="plain"
                        type="info"
                      >
                        {{ name }}
                      </el-tag>
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无审批轨迹" :image-size="72" />
            </div>
          </div>
        </el-card>
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

    <el-dialog v-model="userActionDialogVisible" :title="userActionDialogTitle" width="520px">
      <div class="space-y-4">
        <el-form-item :label="userActionDialogLabel" required>
          <el-select
            v-model="userActionForm.targetUserId"
            class="w-full"
            filterable
            remote
            reserve-keyword
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
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  expenseApi,
  expenseApprovalApi,
  type ExpenseActionUserOption,
  type ExpenseApprovalLog,
  type ExpenseApprovalTask,
  type ExpenseDetailInstanceDetail,
  type ExpenseDocumentDetail,
  type ExpenseDocumentNavigation,
  type ProcessFormDesignSchema
} from '@/api'
import ExpenseFormReadonlyRenderer from './components/ExpenseFormReadonlyRenderer.vue'
import ExpenseInvoiceWorkbench from './components/ExpenseInvoiceWorkbench.vue'
import { buildAuthorizedAttachmentPreviewUrl } from './expenseInvoicePreview'
import { useReadonlyPayeeLookups } from './useReadonlyPayeeLookups'
import {
  resolveDisabledExpenseDetailActionHint,
  resolveExpenseDetailActions,
  type ExpenseDetailActionItem as ActionItem,
  type ExpenseDetailActionKey as ActionKey
} from './expenseDetailActionMatrix'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { formatMoney } from '@/utils/money'

type UserActionMode = 'transfer' | 'add-sign' | ''
type ApprovalTimelineItem = {
  key: string
  timestamp: string
  title: string
  description: string
  attachmentNames: string[]
}

const route = useRoute()
const router = useRouter()
const detailLoading = ref(false)
const navigationLoading = ref(false)
const detail = ref<ExpenseDocumentDetail | null>(null)
const detailLoadError = ref('')
const navigation = ref<ExpenseDocumentNavigation>({})
const activeExpenseDetailNo = ref('')
const expenseDetailLoadingNo = ref('')
const expenseDetailCache = ref<Record<string, ExpenseDetailInstanceDetail>>({})
const expenseDetailErrors = ref<Record<string, string>>({})
const { vendorOptionMap, payeeOptionMap, payeeAccountOptionMap, syncReadonlyPayeeLookups } = useReadonlyPayeeLookups()
const storedUser = (readStoredUser() || {}) as { userId?: number; permissionCodes?: string[] }
const commentDialogVisible = ref(false)
const commentSubmitting = ref(false)
const commentFileInput = ref<HTMLInputElement | null>(null)
const commentForm = ref({
  comment: '',
  attachmentFileNames: [] as string[]
})
const userActionDialogVisible = ref(false)
const userActionMode = ref<UserActionMode>('')
const userActionSubmitting = ref(false)
const userOptionsLoading = ref(false)
const userOptions = ref<ExpenseActionUserOption[]>([])
const userActionForm = ref({
  targetUserId: undefined as number | undefined,
  remark: ''
})
const emptyExpenseDetailSchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }
let detailRequestVersion = 0
let navigationRequestVersion = 0

const amountText = computed(() => `¥ ${formatDetailMoney(detail.value?.totalAmount)}`)
const activeExpenseDetail = computed(() => (
  activeExpenseDetailNo.value ? expenseDetailCache.value[activeExpenseDetailNo.value] || null : null
))
const activeExpenseDetailError = computed(() => (
  activeExpenseDetailNo.value ? expenseDetailErrors.value[activeExpenseDetailNo.value] || '' : ''
))
const activeExpenseDetailSummary = computed(() => (
  detail.value?.expenseDetails?.find((item) => item.detailNo === activeExpenseDetailNo.value) || null
))
const currentUserId = computed(() => Number(storedUser.userId || 0))
const permissionCodes = computed(() => storedUser.permissionCodes || [])
const approvableTasks = computed(() =>
  (detail.value?.currentTasks || []).filter((task) => task.assigneeUserId === currentUserId.value && task.nodeType === 'APPROVAL')
)
const canApprovalView = computed(() =>
  hasPermission('expense:approval:view', permissionCodes.value)
  || hasPermission('expense:approval:approve', permissionCodes.value)
  || hasPermission('expense:approval:reject', permissionCodes.value)
)
const isSubmitter = computed(() => detail.value?.submitterUserId === currentUserId.value)
const isActiveApprover = computed(() => approvableTasks.value.length > 0)
const isFlowParticipant = computed(() => {
  if (!detail.value) {
    return false
  }
  if (isSubmitter.value || isActiveApprover.value) {
    return true
  }
  const userId = currentUserId.value
  return detail.value.actionLogs.some((log) => {
    if (log.actorUserId === userId) {
      return true
    }
    const approverUserIds = Array.isArray(log.payload?.approverUserIds) ? log.payload.approverUserIds : []
    return approverUserIds.some((item) => Number(item) === userId)
  })
})
const canComment = computed(() => isSubmitter.value || isFlowParticipant.value)
const statusBucket = computed<'pending' | 'exception' | 'terminal' | 'other'>(() => {
  const status = detail.value?.status || ''
  if (status === 'PENDING_APPROVAL') {
    return 'pending'
  }
  if (status === 'EXCEPTION') {
    return 'exception'
  }
  if (
    status === 'APPROVED'
    || status === 'PAID'
    || status === 'PENDING_PAYMENT'
    || status === 'PAYING'
    || status === 'PAYMENT_COMPLETED'
    || status === 'PAYMENT_EXCEPTION'
  ) {
    return 'terminal'
  }
  return 'other'
})

const approvalTimelineItems = computed<ApprovalTimelineItem[]>(() => {
  if (!detail.value) {
    return []
  }
  const logItems = (detail.value.actionLogs || []).map((log, index) => ({
    key: `log-${log.id ?? index}`,
    timestamp: log.createdAt || '',
    title: timelineTitle(log, detail.value),
    description: timelineDescription(log),
    attachmentNames: commentAttachmentNames(log)
  }))
  return logItems.concat(buildPendingTimelineItems(detail.value.currentTasks || []))
})
const actionItems = computed<ActionItem[]>(() => {
  if (!detail.value) {
    return []
  }
  return resolveExpenseDetailActions({
    statusBucket: statusBucket.value,
    isSubmitter: isSubmitter.value,
    isActiveApprover: isActiveApprover.value,
    isFlowParticipant: isFlowParticipant.value,
    canComment: canComment.value,
    canApprovalView: canApprovalView.value,
    prevDocumentCode: navigation.value.prevDocumentCode,
    nextDocumentCode: navigation.value.nextDocumentCode
  })
})
const secondaryActionItems = computed(() => actionItems.value.filter((item) => !item.primary))
const primaryActionItems = computed(() => actionItems.value.filter((item) => item.primary))
const disabledActionHint = computed(() => resolveDisabledExpenseDetailActionHint(actionItems.value))
const userActionDialogTitle = computed(() => userActionMode.value === 'transfer' ? '转交审批任务' : '发起前加签')
const userActionDialogLabel = computed(() => userActionMode.value === 'transfer' ? '转交给' : '加签人')
const userActionDialogConfirm = computed(() => userActionMode.value === 'transfer' ? '确认转交' : '确认加签')
const userActionDialogPlaceholder = computed(() => userActionMode.value === 'transfer' ? '可选填写转交说明' : '可选填写加签说明')

watch(
  () => route.params.documentCode,
  () => {
    void loadDetail()
  },
  { immediate: true }
)

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push('/expense/list')
}

function openExpenseDetail(detailNo: string) {
  void router.push({
    name: 'expense-document-expense-detail',
    params: {
      documentCode: String(route.params.documentCode || ''),
      detailNo
    }
  })
}

async function selectExpenseDetail(detailNo: string) {
  if (!detailNo) {
    return
  }

  if (activeExpenseDetailNo.value === detailNo) {
    activeExpenseDetailNo.value = ''
    return
  }

  activeExpenseDetailNo.value = detailNo
  if (expenseDetailCache.value[detailNo] || expenseDetailLoadingNo.value === detailNo) {
    return
  }

  const nextErrors = { ...expenseDetailErrors.value }
  delete nextErrors[detailNo]
  expenseDetailErrors.value = nextErrors
  expenseDetailLoadingNo.value = detailNo

  try {
    const res = await expenseApi.getExpenseDetail(String(route.params.documentCode || ''), detailNo)
    expenseDetailCache.value = {
      ...expenseDetailCache.value,
      [detailNo]: res.data
    }
  } catch (error: unknown) {
    expenseDetailErrors.value = {
      ...expenseDetailErrors.value,
      [detailNo]: resolveErrorMessage(error, '加载费用明细发票信息失败')
    }
  } finally {
    if (expenseDetailLoadingNo.value === detailNo) {
      expenseDetailLoadingNo.value = ''
    }
  }
}

async function loadDetail() {
  const requestVersion = ++detailRequestVersion
  detailLoading.value = true
  navigationRequestVersion += 1
  navigationLoading.value = false
  detailLoadError.value = ''
  detail.value = null
  navigation.value = {}
  activeExpenseDetailNo.value = ''
  expenseDetailLoadingNo.value = ''
  expenseDetailCache.value = {}
  expenseDetailErrors.value = {}
  try {
    const res = await expenseApi.getDetail(String(route.params.documentCode || ''))
    if (requestVersion !== detailRequestVersion) {
      return
    }
    detail.value = res.data
    void syncReadonlyPayeeLookups(res.data.formSchemaSnapshot)
    void loadNavigation(res.data.documentCode, requestVersion)
  } catch (error: unknown) {
    if (requestVersion === detailRequestVersion) {
      detailLoadError.value = resolveErrorMessage(error, '加载单据详情失败')
      ElMessage.error(detailLoadError.value)
    }
  } finally {
    if (requestVersion === detailRequestVersion) {
      detailLoading.value = false
    }
  }
}

async function loadNavigation(documentCode: string, requestVersion: number) {
  const navigationVersion = ++navigationRequestVersion
  if (!documentCode || !canApprovalView.value) {
    navigation.value = {}
    navigationLoading.value = false
    return
  }
  navigationLoading.value = true
  try {
    const res = await expenseApi.getNavigation(documentCode)
    if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
      navigation.value = res.data
    }
  } catch {
    if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
      navigation.value = {}
    }
  } finally {
    if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
      navigationLoading.value = false
    }
  }
}

function timelineTitle(log: ExpenseApprovalLog, documentDetail?: ExpenseDocumentDetail | null) {
  const actorName = log.actorName || '\u5ba1\u6279\u4eba'
  const nodeName = asString(log.nodeName) || '\u8282\u70b9'
  const approverNames = resolveApproverNamesForTimelineLog(log)
  const approverText = approverNames.length ? approverNames.join('\u3001') : '\u672a\u67e5\u8be2\u5230\u5ba1\u6279\u4eba'
  const actionMap: Record<string, string> = {
    SUBMIT: `${asString(documentDetail?.submitterName) || asString(log.actorName) || '\u63d0\u5355\u4eba'} \u63d0\u4ea4\u5355\u636e`,
    RECALL: actorName + ' \u53ec\u56de\u5355\u636e',
    RESUBMIT: actorName + ' \u91cd\u65b0\u63d0\u4ea4',
    ROUTE_HIT: ('\u547d\u4e2d\u5206\u652f ' + String(log.payload?.routeName || '')).trim(),
    APPROVAL_PENDING: `${nodeName} ${approverText} \u5ba1\u6279\u4e2d`,
    APPROVE: `${nodeName} ${actorName} \u5ba1\u6279\u901a\u8fc7`,
    REJECT: `${nodeName} ${actorName} \u5ba1\u6279\u9a73\u56de`,
    MODIFY: actorName + ' \u4fee\u6539\u5355\u636e',
    COMMENT: actorName + ' \u53d1\u8868\u8bc4\u8bba',
    REMIND: actorName + ' \u53d1\u8d77\u50ac\u529e',
    TRANSFER: actorName + ' \u8f6c\u4ea4\u5ba1\u6279',
    ADD_SIGN: actorName + ' \u53d1\u8d77\u52a0\u7b7e',
    AUTO_SKIP: ('\u81ea\u52a8\u8df3\u8fc7 ' + (log.nodeName || '')).trim(),
    CC_REACHED: ('\u5230\u8fbe\u6284\u9001\u8282\u70b9 ' + (log.nodeName || '')).trim(),
    PAYMENT_REACHED: ('\u5230\u8fbe\u652f\u4ed8\u8282\u70b9 ' + (log.nodeName || '')).trim(),
    PAYMENT_PENDING: ('\u8fdb\u5165\u5f85\u652f\u4ed8 ' + (log.nodeName || '')).trim(),
    PAYMENT_START: actorName + ' \u53d1\u8d77\u652f\u4ed8',
    PAYMENT_COMPLETE: actorName + ' \u786e\u8ba4\u5df2\u652f\u4ed8',
    PAYMENT_EXCEPTION: actorName + ' \u6807\u8bb0\u652f\u4ed8\u5f02\u5e38',
    FINISH: '\u5ba1\u6279\u5b8c\u6210',
    EXCEPTION: '\u6d41\u7a0b\u5f02\u5e38'
  }
  return actionMap[log.actionType] || log.actionType
}
function timelineDescription(log: ExpenseApprovalLog) {
  if (log.actionType === 'COMMENT') {
    return String(log.payload?.comment || log.actionComment || '')
  }
  if (['SUBMIT', 'APPROVE', 'REJECT', 'APPROVAL_PENDING'].includes(log.actionType)) {
    return asString(log.actionComment)
  }
  const parts = [log.actionComment]
  if (log.nodeName && !['APPROVE', 'REJECT', 'APPROVAL_PENDING', 'RECALL', 'RESUBMIT', 'COMMENT'].includes(log.actionType)) {
    parts.unshift(log.nodeName)
  }
  if (log.actionType === 'TRANSFER' && log.payload?.targetUserName) {
    parts.push(`\u8f6c\u4ea4\u7ed9 ${String(log.payload.targetUserName)}`)
  }
  if (log.actionType === 'ADD_SIGN' && log.payload?.targetUserName) {
    parts.push(`\u52a0\u7b7e\u7ed9 ${String(log.payload.targetUserName)}`)
  }
  return parts.filter(Boolean).join(' / ')
}
function commentAttachmentNames(log: ExpenseApprovalLog) {
  const raw = log.payload?.attachmentFileNames
  return Array.isArray(raw) ? raw.map((item) => String(item)) : []
}

function buildPendingTimelineItems(tasks: ExpenseApprovalTask[]): ApprovalTimelineItem[] {
  const deduped = new Map<string, ApprovalTimelineItem>()
  tasks.forEach((task, index) => {
    const nodeName = asString(task.nodeName) || '\u8282\u70b9'
    const assigneeName = asString(task.assigneeName) || '\u672a\u67e5\u8be2\u5230\u5904\u7406\u4eba'
    const pendingLabel = task.nodeType === 'PAYMENT' ? '\u5f85\u652f\u4ed8' : '\u5f85\u5ba1\u6279'
    const dedupeKey = `${asString(task.nodeKey) || 'pending'}::${assigneeName}`
    if (deduped.has(dedupeKey)) {
      return
    }
    deduped.set(dedupeKey, {
      key: `pending-${task.id ?? index}-${dedupeKey}`,
      timestamp: task.createdAt || '',
      title: `${nodeName} ${assigneeName} ${pendingLabel}`,
      description: '',
      attachmentNames: []
    })
  })
  return Array.from(deduped.values())
}

function resolveApproverNamesForTimelineLog(log: ExpenseApprovalLog) {
  const names: string[] = []
  if (['APPROVE', 'REJECT'].includes(log.actionType)) {
    const actorName = asString(log.actorName)
    if (actorName) {
      names.push(actorName)
    }
  }
  if (log.actionType === 'APPROVAL_PENDING') {
    const approverNames = Array.isArray(log.payload?.approverNames) ? log.payload.approverNames : []
    approverNames.forEach((name) => {
      const normalizedName = asString(name)
      if (normalizedName && !names.includes(normalizedName)) {
        names.push(normalizedName)
      }
    })
  }
  return names
}
async function handleTaskAction(action: 'approve' | 'reject') {
  if (!detail.value || !approvableTasks.value.length) {
    return
  }
  const permissionCode = action === 'approve' ? 'expense:approval:approve' : 'expense:approval:reject'
  if (!hasPermission(permissionCode, permissionCodes.value)) {
    ElMessage.warning('\u5f53\u524d\u8d26\u53f7\u6ca1\u6709\u5904\u7406\u8be5\u5ba1\u6279\u7684\u6743\u9650')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      action === 'approve' ? '\u53ef\u9009\u586b\u5199\u5ba1\u6279\u610f\u89c1' : '\u8bf7\u586b\u5199\u9a73\u56de\u539f\u56e0',
      action === 'approve' ? '\u901a\u8fc7\u5ba1\u6279' : '\u9a73\u56de\u5ba1\u6279',
      {
        inputType: 'textarea',
        inputValue: action === 'approve' ? '\u901a\u8fc7' : '\u9a73\u56de',
        inputPlaceholder: action === 'approve' ? '\u8bf7\u8f93\u5165\u5ba1\u6279\u610f\u89c1\uff08\u53ef\u7a7a\uff09' : '\u8bf7\u8f93\u5165\u9a73\u56de\u539f\u56e0',
        confirmButtonText: action === 'approve' ? '\u901a\u8fc7' : '\u9a73\u56de',
        cancelButtonText: '\u53d6\u6d88'
      }
    )
    const task = approvableTasks.value[0]
    if (!task) {
      return
    }
    const api = action === 'approve' ? expenseApprovalApi.approve : expenseApprovalApi.reject
    const res = await api(task.id, { comment: value || '' })
    await refreshAfterAction(res.data)
    ElMessage.success(action === 'approve' ? '\u5ba1\u6279\u5df2\u901a\u8fc7' : '\u5ba1\u6279\u5df2\u9a73\u56de')
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, action === 'approve' ? '\u5ba1\u6279\u901a\u8fc7\u5931\u8d25' : '\u5ba1\u6279\u9a73\u56de\u5931\u8d25'))
  }
}
async function handleActionClick(action: ActionItem) {
  if (action.disabled) {
    ElMessage.warning(action.reason || '当前动作暂不可用')
    return
  }

  switch (action.key) {
    case 'recall':
      await handleRecall()
      return
    case 'print':
    case 'download':
      ElMessage.info('功能建设中')
      return
    case 'comment':
      openCommentDialog()
      return
    case 'remind':
      await handleRemind()
      return
    case 'approve':
      await handleTaskAction('approve')
      return
    case 'reject':
      await handleTaskAction('reject')
      return
    case 'prev':
      await navigateDetail(navigation.value.prevDocumentCode)
      return
    case 'next':
      await navigateDetail(navigation.value.nextDocumentCode)
      return
    case 'modify':
      await openModifyPage()
      return
    case 'add-sign':
    case 'transfer':
      await openUserActionDialog(action.key)
      return
  }
}

async function handleRecall() {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('召回后会回到草稿编辑页，并沿用当前单号重新提交，确认继续吗？', '召回单据', {
      type: 'warning',
      confirmButtonText: '确认召回',
      cancelButtonText: '取消'
    })
    await expenseApi.recall(detail.value.documentCode)
    ElMessage.success('单据已召回，正在进入重提编辑页')
    await router.push({
      name: 'expense-document-resubmit',
      params: { documentCode: detail.value.documentCode }
    })
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '召回单据失败'))
  }
}

async function handleRemind() {
  if (!detail.value) {
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('可选填写催办备注', '催办审批', {
      inputType: 'textarea',
      inputPlaceholder: '例如：这笔单据今天需要完成处理',
      confirmButtonText: '发送催办',
      cancelButtonText: '取消'
    })
    const res = await expenseApi.remind(detail.value.documentCode, { remark: value || '' })
    await refreshAfterAction(res.data)
    ElMessage.success('已向当前审批人发送催办')
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '催办失败'))
  }
}

function openCommentDialog() {
  commentForm.value = {
    comment: '',
    attachmentFileNames: []
  }
  commentDialogVisible.value = true
}

async function submitComment() {
  if (!detail.value) {
    return
  }
  if (!commentForm.value.comment.trim() && commentForm.value.attachmentFileNames.length === 0) {
    ElMessage.warning('请先输入评论或添加附件名')
    return
  }
  commentSubmitting.value = true
  try {
    const res = await expenseApi.comment(detail.value.documentCode, {
      comment: commentForm.value.comment.trim(),
      attachmentFileNames: commentForm.value.attachmentFileNames
    })
    commentDialogVisible.value = false
    await refreshAfterAction(res.data)
    ElMessage.success('评论已发布')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '发表评论失败'))
  } finally {
    commentSubmitting.value = false
  }
}

function pickCommentFiles() {
  commentFileInput.value?.click()
}

function handleCommentFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  if (files.length === 0) {
    return
  }
  const merged = new Set([
    ...commentForm.value.attachmentFileNames,
    ...files.map((file) => file.name).filter(Boolean)
  ])
  commentForm.value.attachmentFileNames = Array.from(merged)
  target.value = ''
}

function removeCommentAttachment(name: string) {
  commentForm.value.attachmentFileNames = commentForm.value.attachmentFileNames.filter((item) => item !== name)
}

async function openModifyPage() {
  const task = approvableTasks.value[0]
  if (!task) {
    ElMessage.warning('当前没有可修改的待办任务')
    return
  }
  await router.push({
    name: 'expense-approval-task-modify',
    params: { taskId: task.id }
  })
}

async function openUserActionDialog(actionKey: 'add-sign' | 'transfer') {
  const task = approvableTasks.value[0]
  if (!task) {
    ElMessage.warning('当前没有可处理的待办任务')
    return
  }
  userActionMode.value = actionKey
  userActionForm.value = {
    targetUserId: undefined,
    remark: ''
  }
  userActionDialogVisible.value = true
  await loadActionUsers('')
}

function closeUserActionDialog() {
  userActionDialogVisible.value = false
  userActionMode.value = ''
  userActionForm.value = {
    targetUserId: undefined,
    remark: ''
  }
}

async function loadActionUsers(keyword: string) {
  userOptionsLoading.value = true
  try {
    const res = await expenseApprovalApi.listActionUsers(keyword)
    userOptions.value = res.data
  } finally {
    userOptionsLoading.value = false
  }
}

async function submitUserAction() {
  const task = approvableTasks.value[0]
  if (!task) {
    ElMessage.warning('当前没有可处理的待办任务')
    return
  }
  if (!userActionForm.value.targetUserId) {
    ElMessage.warning('请先选择目标处理人')
    return
  }
  userActionSubmitting.value = true
  try {
    const mode = userActionMode.value
    const payload = {
      targetUserId: userActionForm.value.targetUserId,
      remark: userActionForm.value.remark.trim()
    }
    const res = mode === 'transfer'
      ? await expenseApprovalApi.transfer(task.id, payload)
      : await expenseApprovalApi.addSign(task.id, payload)
    closeUserActionDialog()
    await refreshAfterAction(res.data)
    ElMessage.success(mode === 'transfer' ? '审批任务已转交' : '已发起加签')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, userActionMode.value === 'transfer' ? '转交审批失败' : '加签失败'))
  } finally {
    userActionSubmitting.value = false
  }
}

async function navigateDetail(documentCode?: string) {
  if (!documentCode) {
    ElMessage.warning('已经没有更多单据了')
    return
  }
  await router.push(`/expense/documents/${encodeURIComponent(documentCode)}`)
}

async function refreshAfterAction(nextDetail?: ExpenseDocumentDetail) {
  if (nextDetail) {
    detailLoadError.value = ''
    detail.value = nextDetail
    navigation.value = {}
    void syncReadonlyPayeeLookups(nextDetail.formSchemaSnapshot)
    await loadNavigation(nextDetail.documentCode, detailRequestVersion)
    return
  }
  await loadDetail()
}

function asString(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : ''
}

function formatDetailMoney(value: unknown) {
  try {
    return formatMoney(value as string | number | null | undefined)
  } catch {
    return '0.00'
  }
}

function formatAttachmentSize(value?: number) {
  if (!value || Number.isNaN(Number(value))) {
    return '大小未知'
  }
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<style scoped>
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
</style>
