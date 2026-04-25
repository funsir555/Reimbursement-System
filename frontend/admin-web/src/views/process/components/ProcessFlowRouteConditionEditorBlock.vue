<template>
  <div class="space-y-6">
    <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="text-base font-semibold text-slate-800">分支泳道</p>
          <p class="mt-1 text-sm text-slate-500">
            当前属于 {{ state.activeBranchNode?.nodeName || '流程分支' }}，至少保留 2 条分支泳道。
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
          </div>
          <el-switch :model-value="state.attachBelowEnabled" @update:model-value="actions.updateAttachBelowEnabled" />
        </div>
      </div>

      <div class="route-pill-grid">
        <button
          v-for="routeItem in state.currentBranchRoutes"
          :key="routeItem.routeKey"
          type="button"
          class="route-pill"
          :class="routeItem.routeKey === state.route.routeKey ? 'is-selected' : ''"
          @click="actions.selectRoute(routeItem.routeKey)"
        >
          <div class="min-w-0 text-left">
            <div class="flex items-center gap-2">
              <p class="truncate text-sm font-semibold text-slate-800">{{ routeItem.routeName || '未命名分支' }}</p>
              <span
                v-if="routeItem.attachBelowNodes"
                class="rounded-full bg-sky-100 px-2 py-0.5 text-[11px] font-semibold text-sky-600"
              >
                附带下方节点
              </span>
            </div>
            <p class="mt-1 text-xs text-slate-400">
              优先级 {{ routeItem.priority }} · {{ helpers.describeRouteConditions(routeItem).groups }} 组条件 ·
              {{ helpers.describeRouteConditions(routeItem).conditions }} 条条件
            </p>
          </div>
        </button>
      </div>
    </div>

    <div class="rounded-[24px] border border-slate-200 bg-white p-5 space-y-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="text-base font-semibold text-slate-800">条件设置</p>
          <p class="mt-1 text-sm text-slate-500">
            当前共 {{ helpers.describeRouteConditions(state.route).groups }} 组条件，{{ helpers.describeRouteConditions(state.route).conditions }} 条条件。
          </p>
        </div>
        <el-button type="primary" plain @click="actions.addConditionGroup(state.route)">新增条件组</el-button>
      </div>
      <div v-if="state.route.conditionGroups.length" class="space-y-4">
        <div
          v-for="group in state.route.conditionGroups"
          :key="group.groupNo"
          class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-4"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-slate-800">条件组 {{ group.groupNo }}</p>
              <p class="mt-1 text-xs text-slate-400">每个条件组都可以继续添加多个条件项。</p>
            </div>
            <div class="flex flex-wrap gap-2">
              <el-button plain @click="actions.addCondition(group)">新增条件</el-button>
              <el-button type="danger" text @click="actions.removeConditionGroup(state.route, group.groupNo)">删除条件组</el-button>
            </div>
          </div>

          <div v-if="group.conditions.length" class="space-y-3">
            <div
              v-for="(condition, conditionIndex) in group.conditions"
              :key="`${group.groupNo}-${conditionIndex}`"
              class="rounded-2xl border border-slate-200 bg-white p-4 space-y-4"
            >
              <div class="space-y-4">
                <div class="process-flow-condition-primary-grid grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.2fr),minmax(0,0.9fr)]">
                  <el-form-item label="条件字段" class="!mb-0">
                    <el-select
                      v-model="condition.fieldKey"
                      placeholder="请选择条件字段"
                      @change="actions.handleConditionFieldChange(condition)"
                    >
                      <el-option
                        v-for="field in meta.branchConditionFields"
                        :key="field.key"
                        :label="field.label"
                        :value="field.key"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="比较方式" class="!mb-0">
                    <el-select
                      v-model="condition.operator"
                      placeholder="请选择比较方式"
                      @change="actions.handleConditionOperatorChange(condition)"
                    >
                      <el-option
                        v-for="item in helpers.operatorOptionsForField(condition.fieldKey)"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </el-form-item>
                </div>

                <el-form-item
                  label="比较值"
                  class="process-flow-condition-value-row !mb-0"
                  data-testid="process-flow-condition-value-row"
                >
                  <template v-if="helpers.isBetweenOperator(condition.operator)">
                    <div class="grid grid-cols-2 gap-3">
                      <el-input-number
                        v-model="condition.compareValue[0]"
                        class="!w-full"
                        :controls="false"
                        placeholder="起始值"
                      />
                      <el-input-number
                        v-model="condition.compareValue[1]"
                        class="!w-full"
                        :controls="false"
                        placeholder="结束值"
                      />
                    </div>
                  </template>

                  <el-select
                    v-else-if="helpers.isMultiOperator(condition.operator)"
                    v-model="condition.compareValue"
                    multiple
                    filterable
                    clearable
                    collapse-tags
                    collapse-tags-tooltip
                    :allow-create="!helpers.usesOptionSelect(condition)"
                    default-first-option
                    :placeholder="helpers.multiValuePlaceholder(condition)"
                  >
                    <el-option
                      v-for="item in helpers.conditionValueOptions(condition)"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>

                  <el-select
                    v-else-if="helpers.usesOptionSelect(condition)"
                    v-model="condition.compareValue"
                    filterable
                    clearable
                    :placeholder="helpers.singleValuePlaceholder(condition)"
                  >
                    <el-option
                      v-for="item in helpers.conditionValueOptions(condition)"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>

                  <el-input-number
                    v-else-if="helpers.isNumberCondition(condition)"
                    v-model="condition.compareValue"
                    class="!w-full"
                    :controls="false"
                    placeholder="请输入数值"
                  />

                  <el-input v-else v-model="condition.compareValue" :placeholder="helpers.singleValuePlaceholder(condition)" />
                </el-form-item>
              </div>

              <div class="flex justify-end">
                <el-button type="danger" text @click="actions.removeCondition(group, conditionIndex)">删除条件</el-button>
              </div>
            </div>
          </div>

          <el-empty v-else description="当前条件组还没有条件项" :image-size="56" />
        </div>
      </div>

      <el-empty v-else description="当前分支还没有条件组" :image-size="64" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import type { ProcessFlowMeta, ProcessFlowNode, ProcessFormOption } from '@/api'

type EditableProcessFlowCondition = {
  fieldKey: string
  operator: string
  compareValue: any
}

type EditableProcessFlowConditionGroup = {
  groupNo: number
  conditions: EditableProcessFlowCondition[]
}

type EditableProcessFlowRoute = {
  routeKey: string
  sourceNodeKey: string
  routeName?: string
  priority?: number
  attachBelowNodes?: boolean
  conditionGroups: EditableProcessFlowConditionGroup[]
}

defineProps({
  state: {
    type: Object as PropType<{
      route: EditableProcessFlowRoute
      activeBranchNode?: ProcessFlowNode
      currentBranchRoutes: EditableProcessFlowRoute[]
      attachBelowEnabled: boolean
    }>,
    required: true
  },
  meta: {
    type: Object as PropType<{
      branchConditionFields: ProcessFlowMeta['branchConditionFields']
    }>,
    required: true
  },
  helpers: {
    type: Object as PropType<{
      describeRouteConditions: (route?: EditableProcessFlowRoute) => { groups: number; conditions: number }
      operatorOptionsForField: (fieldKey?: string) => ProcessFormOption[]
      conditionValueOptions: (condition: EditableProcessFlowCondition) => ProcessFormOption[]
      usesOptionSelect: (condition: EditableProcessFlowCondition) => boolean
      isNumberCondition: (condition: EditableProcessFlowCondition) => boolean
      isBetweenOperator: (operator: string) => boolean
      isMultiOperator: (operator: string) => boolean
      singleValuePlaceholder: (condition: EditableProcessFlowCondition) => string
      multiValuePlaceholder: (condition: EditableProcessFlowCondition) => string
    }>,
    required: true
  },
  actions: {
    type: Object as PropType<{
      selectRoute: (routeKey: string) => void
      addRouteLane: (branchNodeKey: string) => void
      removeSelectedItem: () => void
      removeActiveBranchBlock: () => void
      updateAttachBelowEnabled: (enabled: boolean) => void
      addConditionGroup: (route: EditableProcessFlowRoute) => void
      removeConditionGroup: (route: EditableProcessFlowRoute, groupNo: number) => void
      addCondition: (group: EditableProcessFlowConditionGroup) => void
      removeCondition: (group: EditableProcessFlowConditionGroup, index: number) => void
      handleConditionFieldChange: (condition: EditableProcessFlowCondition) => void
      handleConditionOperatorChange: (condition: EditableProcessFlowCondition) => void
    }>,
    required: true
  }
})
</script>
