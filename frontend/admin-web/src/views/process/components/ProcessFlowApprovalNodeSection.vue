<template>
  <div class="space-y-6">
    <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
      <div class="grid grid-cols-1 gap-4">
        <div class="grid grid-cols-[minmax(0,1fr),auto] items-end gap-3">
          <el-form-item label="流程场景" class="!mb-0">
            <el-select v-model="state.node.sceneId" placeholder="请选择流程场景" clearable>
              <el-option v-for="item in state.meta.sceneOptions" :key="item.id" :label="item.sceneName" :value="item.id" />
            </el-select>
          </el-form-item>
          <div>
            <el-button plain @click="actions.openSceneDialog">添加</el-button>
          </div>
        </div>

        <el-form-item label="审批人类型" class="!mb-0">
          <el-radio-group v-model="state.node.config.approverType" class="flex flex-wrap gap-3">
            <el-radio-button v-for="item in state.approvalApproverTypes" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </div>
    </div>

    <div v-if="state.node.config.approverType === 'MANAGER'" class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
      <div class="grid grid-cols-1 gap-4">
        <el-form-item label="主管规则" class="!mb-0">
          <el-select v-model="state.node.config.managerConfig.ruleMode" placeholder="请选择主管规则">
            <el-option
              v-for="item in state.meta.approvalManagerRuleModeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="部门来源" class="!mb-0">
          <el-select v-model="state.node.config.managerConfig.deptSource" placeholder="请选择部门来源">
            <el-option
              v-for="item in state.meta.approvalManagerDeptSourceOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <p
          v-if="state.node.config.managerConfig.deptSource === 'UNDERTAKE_DEPT'"
          class="rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-xs leading-6 text-blue-600"
        >
          “承担部门”来源于表单中的“承担部门”组件结果；若提单时未选到承担部门，当前仍会兼容回退到提单人部门。
        </p>

        <el-form-item label="部门级次" class="!mb-0">
          <el-select v-model="state.node.config.managerConfig.managerLevel" placeholder="请选择主管级次">
            <el-option
              v-for="item in state.meta.approvalManagerLevelOptions"
              :key="item.value"
              :label="item.label"
              :value="Number(item.value)"
            />
          </el-select>
        </el-form-item>

        <p class="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs leading-6 text-amber-700">
          {{ state.managerApprovalHint }}
        </p>

        <div class="rounded-2xl bg-white p-4">
          <div class="flex flex-wrap items-center gap-3">
            <el-checkbox v-model="state.node.config.managerConfig.orgTreeLookupEnabled">
              按照组织架构树向上查找
            </el-checkbox>
            <span class="text-sm text-slate-500">未命中时继续向上查找上级主管</span>
          </div>
        </div>

        <el-form-item label="向上查找级次" class="!mb-0">
          <el-select v-model="state.node.config.managerConfig.orgTreeLookupLevel" placeholder="请选择查找级次">
            <el-option
              v-for="item in state.meta.approvalManagerLookupLevelOptions"
              :key="item.value"
              :label="item.label"
              :value="Number(item.value)"
            />
          </el-select>
        </el-form-item>
      </div>
    </div>

    <div v-else-if="state.node.config.approverType === 'DESIGNATED_MEMBER'" class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
      <el-form-item label="指定成员" class="!mb-0">
        <el-select
          v-model="state.node.config.designatedMemberConfig.userIds"
          multiple
          filterable
          clearable
          placeholder="请选择固定审批成员"
        >
          <el-option
            v-for="item in state.meta.userOptions"
            :key="item.value"
            :label="item.label"
            :value="Number(item.value)"
          />
        </el-select>
      </el-form-item>
    </div>

    <div v-else class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
      <el-form-item label="候选范围" class="!mb-0">
        <el-select v-model="state.node.config.manualSelectConfig.candidateScope" placeholder="请选择候选范围">
          <el-option
            v-for="item in state.meta.approvalManualCandidateScopeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <p class="mt-3 text-sm leading-6 text-slate-500">
        提单时由提单人在候选范围内手动选择审批人，本轮默认全体有效成员。
      </p>
    </div>

    <div class="rounded-[24px] border border-slate-200 bg-white p-5 space-y-5">
      <el-form-item label="找不到审批人时" class="!mb-0">
        <el-select v-model="state.node.config.missingHandler" placeholder="请选择处理策略">
          <el-option v-for="item in state.meta.missingHandlerOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="操作类型" class="!mb-0">
        <el-radio-group v-model="state.node.config.approvalMode" class="flex flex-wrap gap-3">
          <el-radio-button
            v-for="item in state.meta.approvalModeOptions"
            :key="item.value"
            :label="item.value"
            :disabled="state.isManagerMultiLevelApproval && item.value !== 'AND_SIGN'"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <p
        v-if="state.isManagerMultiLevelApproval"
        class="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs leading-6 text-amber-700"
      >
        第 1..N 级主管共同审批：多级主管按同一审批节点会签处理，会自动包含第 1 到第 N 级主管，所有命中的主管都审批通过后，当前节点才会通过。
      </p>

      <el-form-item label="审批意见默认值" class="!mb-0">
        <el-select
          v-model="state.node.config.opinionDefaults"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="请输入或选择审批意见"
        >
          <el-option v-for="item in state.approvalOpinionCandidates" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item label="特殊设置" class="!mb-0">
        <el-checkbox-group v-model="state.node.config.specialSettings" class="flex flex-col gap-3">
          <el-checkbox v-for="item in state.meta.approvalSpecialOptions" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import type { ProcessFlowMeta, ProcessFlowNode } from '@/api'

defineProps({
  state: {
    type: Object as PropType<{
      node: ProcessFlowNode
      meta: ProcessFlowMeta
      approvalApproverTypes: ProcessFlowMeta['approvalApproverTypeOptions']
      approvalOpinionCandidates: string[]
      isManagerMultiLevelApproval: boolean
      managerApprovalHint: string
    }>,
    required: true
  },
  actions: {
    type: Object as PropType<{
      openSceneDialog: () => void
    }>,
    required: true
  }
})
</script>
