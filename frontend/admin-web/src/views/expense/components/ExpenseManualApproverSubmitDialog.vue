<template>
  <el-dialog
    :model-value="modelValue"
    title="提交前手动选择审批人"
    width="720px"
    destroy-on-close
    @close="emit('update:modelValue', false)"
  >
    <div class="space-y-6" data-testid="expense-manual-approver-dialog">
      <div class="space-y-2">
        <p class="text-sm font-semibold text-slate-800">本次提交命中的审批轨迹</p>
        <p class="text-sm leading-6 text-slate-500">
          请先为下方手动节点选择候选范围内的审批人，确认后单据才会进入审批中。
        </p>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
        <el-timeline v-if="approvalTimelineItems.length" data-testid="expense-manual-approver-timeline">
          <el-timeline-item
            v-for="item in approvalTimelineItems"
            :key="item.key"
            :timestamp="item.timestamp"
            placement="top"
          >
            <div class="space-y-1">
              <p class="text-sm font-semibold text-slate-800">{{ item.title }}</p>
              <p v-if="item.statusLabel" class="text-xs text-slate-500">{{ item.statusLabel }}</p>
              <p v-if="item.description" class="text-xs leading-6 text-slate-500">{{ item.description }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="当前没有需要预览的审批轨迹" :image-size="72" />
      </div>

      <div class="space-y-4">
        <div
          v-for="node in manualNodes"
          :key="node.nodeKey"
          class="space-y-3 rounded-2xl border border-amber-200 bg-amber-50 p-4"
          data-testid="expense-manual-approver-node"
        >
          <div class="space-y-1">
            <p class="text-sm font-semibold text-slate-800">{{ node.nodeName || node.nodeKey }}</p>
            <p class="text-xs leading-6 text-slate-500">只能选择该节点候选范围内的审批人，可多选。</p>
          </div>
          <el-select
            :model-value="selections[node.nodeKey] || []"
            class="w-full"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            clearable
            :placeholder="`请选择${node.nodeName || '该节点'}审批人`"
            @update:model-value="handleSelectionChange(node.nodeKey, $event)"
          >
            <el-option
              v-for="option in node.candidateOptions || []"
              :key="String(option.value || '')"
              :label="option.label"
              :value="String(option.value || '')"
            />
          </el-select>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button :loading="savingDraft" @click="emit('save-draft')">保存草稿</el-button>
        <el-button type="primary" :disabled="!canConfirm" :loading="submitting" @click="emit('confirm-submit')">
          确认提交
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { ExpenseApprovalTimelineItem, ExpenseManualApproverPreviewNode } from '@/api'

defineProps<{
  modelValue: boolean
  approvalTimelineItems: ExpenseApprovalTimelineItem[]
  manualNodes: ExpenseManualApproverPreviewNode[]
  selections: Record<string, string[]>
  canConfirm: boolean
  submitting: boolean
  savingDraft: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'update-selection': [nodeKey: string, userIds: Array<string | number>]
  'confirm-submit': []
  'save-draft': []
}>()

function handleSelectionChange(nodeKey: string, userIds: Array<string | number>) {
  emit('update-selection', nodeKey, userIds)
}
</script>
