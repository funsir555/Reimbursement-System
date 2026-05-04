<template>
  <div class="space-y-6">
    <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="text-base font-semibold text-slate-800">分支泳道</p>
          <p class="mt-1 text-sm text-slate-500">
            当前分支块至少保留 2 条分支泳道。
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <el-button plain @click="actions.addRouteLane(state.route.sourceNodeKey)">新增分支</el-button>
          <el-button type="danger" plain :disabled="state.currentBranchRoutes.length <= 2" @click="actions.removeSelectedItem()">
            删除当前分支
          </el-button>
          <el-button type="danger" plain data-testid="remove-branch-block-button" @click="actions.removeActiveBranchBlock()">
            删除整个分支块
          </el-button>
        </div>
      </div>

      <el-form-item label="分支名称" class="!mb-0">
        <el-input v-model="state.route.routeName" maxlength="64" show-word-limit placeholder="请输入分支名称" />
      </el-form-item>

      <div class="rounded-2xl border border-slate-200 bg-white p-4">
        <div class="flex items-start justify-between gap-4">
          <div class="space-y-1">
            <p class="text-sm font-semibold text-slate-800">附带下方节点</p>
            <p class="text-xs leading-6 text-slate-500">开启后，当前分支以下的公共节点会跟随这条分支一起显示。</p>
          </div>
          <el-switch
            data-testid="attach-below-switch"
            :model-value="state.attachBelowEnabled"
            @update:model-value="actions.updateAttachBelowEnabled"
          />
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-white p-4">
        <div class="flex items-start justify-between gap-4">
          <div class="space-y-1">
            <p class="text-sm font-semibold text-slate-800">不满足所有条件时进入该分支</p>
            <p class="text-xs leading-6 text-slate-500">开启后，当前分支会作为 else 分支，在其他分支都不满足时进入。</p>
          </div>
          <el-switch
            data-testid="default-route-switch"
            :model-value="state.route.defaultRoute"
            @update:model-value="actions.updateDefaultRouteEnabled"
          />
        </div>
      </div>

    </div>

    <div v-if="!state.route.defaultRoute" class="rounded-[24px] border border-slate-200 bg-white p-5">
      <ProcessConditionGroupEditor
        :groups="state.route.conditionGroups"
        :fields="meta.branchConditionFields"
        :operator-options="meta.branchOperatorOptions"
        :option-sources="optionSources"
        title="条件设置"
        summary=""
        :handlers="conditionHandlers"
      />
    </div>

    <div v-else class="rounded-[24px] border border-emerald-200 bg-emerald-50 p-5">
      <p class="text-sm font-semibold text-emerald-900">条件设置</p>
      <p class="mt-2 text-sm leading-6 text-emerald-800">
        当前分支已作为 else 分支，当其他分支条件都不满足时进入该分支。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import type { ProcessFlowConditionGroup, ProcessFlowMeta, ProcessFlowRoute } from '@/api'
import ProcessConditionGroupEditor from '@/components/process/ProcessConditionGroupEditor.vue'

type EditableProcessFlowRoute = ProcessFlowRoute

const props = defineProps({
  state: {
    type: Object as PropType<{
      route: EditableProcessFlowRoute
      currentBranchRoutes: EditableProcessFlowRoute[]
      attachBelowEnabled: boolean
    }>,
    required: true
  },
  meta: {
    type: Object as PropType<{
      branchConditionFields: ProcessFlowMeta['branchConditionFields']
      branchOperatorOptions: ProcessFlowMeta['branchOperatorOptions']
      companyOptions: ProcessFlowMeta['companyOptions']
      departmentOptions: ProcessFlowMeta['departmentOptions']
      userOptions: ProcessFlowMeta['userOptions']
      expenseTypeOptions: ProcessFlowMeta['expenseTypeOptions']
      archiveOptions: ProcessFlowMeta['archiveOptions']
      branchConditionValueOptions?: ProcessFlowMeta['branchConditionValueOptions']
    }>,
    required: true
  },
  actions: {
    type: Object as PropType<{
      addRouteLane: (branchNodeKey: string) => void
      removeSelectedItem: () => void
      removeActiveBranchBlock: () => void
      updateAttachBelowEnabled: (enabled: boolean) => void
      updateDefaultRouteEnabled: (enabled: boolean) => void
      addConditionGroup: (route: EditableProcessFlowRoute) => void
      removeConditionGroup: (route: EditableProcessFlowRoute, groupNo: number) => void
      addCondition: (group: ProcessFlowConditionGroup) => void
      removeCondition: (group: ProcessFlowConditionGroup, index: number) => void
    }>,
    required: true
  }
})

const optionSources = computed(() => ({
  company: props.meta.companyOptions || [],
  department: props.meta.departmentOptions || [],
  user: props.meta.userOptions || [],
  expenseType: props.meta.expenseTypeOptions || [],
  archive: props.meta.archiveOptions || [],
  ...(props.meta.branchConditionValueOptions || {})
}))

const conditionHandlers = {
  addGroup: () => props.actions.addConditionGroup(props.state.route),
  removeGroup: (groupNo: number) => props.actions.removeConditionGroup(props.state.route, groupNo),
  addCondition: (groupNo: number) => {
    const group = props.state.route.conditionGroups.find((item) => item.groupNo === groupNo)
    if (group) {
      props.actions.addCondition(group)
    }
  },
  removeCondition: (groupNo: number, index: number) => {
    const group = props.state.route.conditionGroups.find((item) => item.groupNo === groupNo)
    if (group) {
      props.actions.removeCondition(group, index)
    }
  }
}
</script>
