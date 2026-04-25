<template>
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

      <div
        v-if="isManualApproverSelectionPending"
        class="rounded-[24px] border border-amber-200 bg-amber-50 p-5 space-y-4"
        data-testid="manual-approver-selection-card"
      >
        <div class="space-y-1">
          <div class="flex flex-wrap items-center gap-2">
            <p class="text-sm font-semibold text-slate-800">当前节点手动选择审批人</p>
            <el-tag size="small" type="warning" effect="plain">待处理</el-tag>
          </div>
          <p class="text-sm text-slate-600">
            当前流程停留在“{{ manualApproverNodeName }}”节点，
            需要由提单人指定本节点审批人后继续流转。
          </p>
        </div>
        <template v-if="canSubmitManualApproverSelection">
          <el-select
            :model-value="manualApproverUserIds"
            class="w-full"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            clearable
            placeholder="请选择当前节点审批人"
            data-testid="manual-approver-selection-select"
            @update:model-value="handleManualApproverChange"
          >
            <el-option
              v-for="item in manualApproverOptions"
              :key="String(item.value || '')"
              :label="item.label"
              :value="Number(item.value)"
            />
          </el-select>
          <div class="flex justify-end">
            <el-button
              type="primary"
              :loading="manualApproverSubmitting"
              @click="emit('submit-manual-approver-selection')"
            >
              提交审批人
            </el-button>
          </div>
        </template>
        <p v-else class="text-xs leading-6 text-slate-500">
          当前节点等待提单人完成手动选人；你可查看全流程轨迹，但不能代为提交。
        </p>
      </div>

      <div class="space-y-3">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-slate-800">&#30495;&#23454;&#20219;&#21153;&#29366;&#24577;</p>
          <el-tag size="small" effect="plain">{{ approvalNodeStatuses.length }} &#26465;</el-tag>
        </div>

        <div
          v-if="approvalNodeStatuses.length"
          class="approval-node-status-list"
          data-testid="approval-node-status-list"
        >
          <div
            v-for="item in approvalNodeStatuses"
            :key="item.nodeKey"
            class="approval-node-status-card"
            :class="{
              'approval-node-status-card--pending': item.status === 'PENDING' || item.status === 'PAYMENT_PENDING' || item.status === 'MANUAL_SELECTION_PENDING',
              'approval-node-status-card--future': item.status === 'NOT_REACHED'
            }"
            data-testid="approval-node-status-item"
          >
            <div class="approval-node-status-card__content">
              <div class="flex flex-wrap items-center gap-2">
                <p class="text-sm font-semibold text-slate-800">{{ item.nodeName || item.nodeKey }}</p>
                <el-tag size="small" effect="plain" :type="approvalStatusTagType(item.status)">
                  {{ item.statusLabel || approvalStatusLabel(item.status) }}
                </el-tag>
              </div>
              <p v-if="item.description" class="text-xs leading-6 text-slate-500">{{ item.description }}</p>
              <p v-else-if="item.assigneeNames?.length" class="text-xs leading-6 text-slate-500">
                &#22788;&#29702;&#20154;&#65306;{{ item.assigneeNames.join('\u3001') }}
              </p>
            </div>
            <span v-if="item.occurredAt" class="approval-node-status-card__time">{{ item.occurredAt }}</span>
          </div>
        </div>
        <el-empty v-else description="&#26242;&#26080;&#30495;&#23454;&#20219;&#21153;&#29366;&#24577;" :image-size="72" />
      </div>

      <div class="space-y-3">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-slate-800">&#23457;&#25209;&#36712;&#36857;</p>
          <el-tag size="small" effect="plain">{{ approvalTimelineItems.length }} &#26465;</el-tag>
        </div>

        <el-timeline v-if="approvalTimelineItems.length" data-testid="approval-timeline-list">
          <el-timeline-item
            v-for="item in approvalTimelineItems"
            :key="item.key"
            :timestamp="item.timestamp"
            placement="top"
            data-testid="approval-timeline-item"
          >
            <div class="space-y-2">
              <div class="flex flex-wrap items-center gap-2">
                <p class="text-sm font-semibold text-slate-800">{{ item.title }}</p>
                <el-tag v-if="item.statusLabel" size="small" effect="plain" :type="approvalStatusTagType(item.status)">
                  {{ item.statusLabel }}
                </el-tag>
              </div>
              <p v-if="item.description" class="text-xs leading-6 text-slate-500">{{ item.description }}</p>
              <div v-if="item.attachmentNames?.length" class="flex flex-wrap gap-2">
                <el-tag
                  v-for="name in item.attachmentNames || []"
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
        <el-empty v-else description="&#26242;&#26080;&#23457;&#25209;&#36712;&#36857;" :image-size="72" />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { ProcessFormOption } from '@/api'
import type { ExpenseApprovalNodeStatus, ExpenseApprovalTimelineItem } from '@/api/modules/expense-types'
import type { SummaryItem } from '../composables/useExpenseDocumentDetailDisplayOwner'

defineProps<{
  summaryItems: SummaryItem[]
  isManualApproverSelectionPending: boolean
  manualApproverNodeName: string
  canSubmitManualApproverSelection: boolean
  manualApproverUserIds: number[]
  manualApproverOptions: ProcessFormOption[]
  manualApproverSubmitting: boolean
  approvalNodeStatuses: ExpenseApprovalNodeStatus[]
  approvalTimelineItems: ExpenseApprovalTimelineItem[]
  approvalStatusTagType: (status?: string) => '' | 'primary' | 'success' | 'warning' | 'danger' | 'info'
  approvalStatusLabel: (status?: string) => string
}>()

const emit = defineEmits<{
  'update:manual-approver-user-ids': [value: number[]]
  'submit-manual-approver-selection': []
}>()

function handleManualApproverChange(value: Array<number | string>) {
  emit(
    'update:manual-approver-user-ids',
    value.map((item) => Number(item)).filter((item) => Number.isFinite(item))
  )
}
</script>
