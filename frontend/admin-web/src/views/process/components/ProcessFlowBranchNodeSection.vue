<template>
  <div class="space-y-5">
    <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="text-base font-semibold text-slate-800">分支管理</p>
          <p class="mt-1 text-sm text-slate-500">
            当前分支块包含 {{ state.currentBranchRoutes.length }} 条泳道，点击泳道卡片可进入各自的条件设置。
          </p>
        </div>
        <el-button type="primary" plain @click="actions.addRouteLane(state.node.nodeKey)">新增分支</el-button>
      </div>

      <div class="rounded-2xl border border-dashed border-slate-300 bg-white p-4 text-sm leading-7 text-slate-500">
        <p>分支内部允许继续插入审批、抄送、支付和新的流程分支。</p>
        <p class="mt-2">泳道顶部的“条件设置头卡片”对应当前 route，不会新增额外的后端节点类型。</p>
      </div>

      <div v-if="state.currentBranchRoutes.length" class="space-y-3">
        <button
          v-for="routeItem in state.currentBranchRoutes"
          :key="routeItem.routeKey"
          type="button"
          class="route-summary-card"
          @click="actions.selectRoute(routeItem.routeKey)"
        >
          <div class="flex items-center justify-between gap-3">
            <div class="min-w-0 text-left">
              <p class="truncate text-sm font-semibold text-slate-800">{{ routeItem.routeName || '未命名分支' }}</p>
              <p class="mt-1 text-xs text-slate-400">
                优先级 {{ routeItem.priority }} · {{ helpers.describeRouteConditions(routeItem).groups }} 组条件 ·
                {{ helpers.describeRouteConditions(routeItem).conditions }} 条条件
              </p>
            </div>
            <el-tag size="small" effect="plain">{{ state.currentFlowLabel }}</el-tag>
          </div>
        </button>
      </div>

      <el-empty v-else description="当前分支块还没有泳道" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import type { ProcessFlowNode } from '@/api'

type EditableProcessFlowRoute = {
  routeKey: string
  routeName?: string
  priority?: number
  conditionGroups?: Array<{ conditions?: unknown[] }>
}

defineProps({
  state: {
    type: Object as PropType<{
      node: ProcessFlowNode
      currentBranchRoutes: EditableProcessFlowRoute[]
      currentFlowLabel: string
    }>,
    required: true
  },
  helpers: {
    type: Object as PropType<{
      describeRouteConditions: (route?: EditableProcessFlowRoute) => { groups: number; conditions: number }
    }>,
    required: true
  },
  actions: {
    type: Object as PropType<{
      addRouteLane: (branchNodeKey: string) => void
      selectRoute: (routeKey: string) => void
    }>,
    required: true
  }
})
</script>
