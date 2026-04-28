<template>
  <el-card class="expense-wb-panel">
    <template #header>
      <div>
        <p class="text-lg font-semibold text-slate-800">审批流程</p>
        <p class="mt-1 text-sm text-slate-500">展示当前单据命中的审批轨迹与处理记录。</p>
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

      <div class="space-y-3">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-slate-800">审批轨迹</p>
          <el-tag size="small" effect="plain">{{ approvalTimelineItems.length }} 条</el-tag>
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
        <el-empty v-else description="暂无审批轨迹" :image-size="72" />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { ExpenseApprovalTimelineItem } from '@/api/modules/expense-types'
import type { SummaryItem } from '../composables/useExpenseDocumentDetailDisplayOwner'

defineProps<{
  summaryItems: SummaryItem[]
  approvalTimelineItems: ExpenseApprovalTimelineItem[]
  approvalStatusTagType: (status?: string) => '' | 'primary' | 'success' | 'warning' | 'danger' | 'info'
}>()
</script>
