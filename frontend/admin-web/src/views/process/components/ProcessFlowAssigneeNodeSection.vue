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
            <el-button plain @click="actions.openSceneDialog">新增</el-button>
          </div>
        </div>

        <el-form-item label="审批人类型" class="!mb-0">
          <el-radio-group v-model="effectiveApproverType" class="flex flex-wrap gap-3">
            <el-radio-button v-for="item in availableApproverTypes" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <p
          v-if="showLegacyCompatibilityNotice"
          class="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs leading-6 text-amber-700"
        >
          当前节点仍在使用历史{{ legacyRoleLabel }}类型“{{ legacyApproverTypeLabel }}”。当前值会继续保留；如需改为统一口径，请重新选择上方审批人类型。
        </p>
      </div>
    </div>

    <div
      v-if="effectiveApproverType === 'MANAGER'"
      class="rounded-[24px] border border-slate-200 bg-slate-50 p-5"
    >
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
          “承担部门”来源于表单中的承担部门组件；若提单时未选到承担部门，系统会兼容回退到提单人部门。
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
              按组织架构树向上查找
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

    <div
      v-else-if="effectiveApproverType === 'DESIGNATED_MEMBER'"
      class="rounded-[24px] border border-slate-200 bg-slate-50 p-5"
    >
      <el-form-item label="指定成员" class="!mb-0">
        <el-select
          v-model="designatedMemberConfig.userIds"
          multiple
          filterable
          v-bind="globalFilterableSelectProps"
          clearable
          placeholder="请选择固定审批成员"
        >
          <el-option
            v-for="item in designatedMemberOptions"
            :key="item.value"
            :label="item.label"
            :value="toDesignatedMemberOptionValue(item.value)"
          />
        </el-select>
      </el-form-item>
    </div>

    <div
      v-else-if="effectiveApproverType === 'DESIGNATED_USER_GROUP'"
      class="rounded-[24px] border border-slate-200 bg-slate-50 p-5"
    >
      <el-form-item label="指定用户组" class="!mb-0">
        <el-select
          v-model="designatedUserGroupConfig.groupId"
          filterable
          v-bind="globalFilterableSelectProps"
          clearable
          placeholder="请选择二级用户组"
        >
          <el-option
            v-for="item in state.meta.userGroupOptions || []"
            :key="item.value"
            :label="item.label"
            :value="Number(item.value)"
          />
        </el-select>
      </el-form-item>

      <p class="mt-4 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-xs leading-6 text-blue-600">
        系统会读取所选二级用户组下命中的三级功能组成员，并按会签方式生成审批任务。
      </p>
    </div>

    <div
      v-else-if="effectiveApproverType === 'MANUAL_SELECT'"
      class="rounded-[24px] border border-slate-200 bg-slate-50 p-5"
    >
      <el-form-item label="候选范围" class="!mb-0">
        <el-select v-model="manualSelectConfig.candidateScope" placeholder="请选择候选范围">
          <el-option
            v-for="item in state.meta.approvalManualCandidateScopeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <p class="mt-3 text-sm leading-6 text-slate-500">
        提单时由提单人在候选范围内手动选择审批人，本轮默认面向全体有效成员。
      </p>
    </div>

    <div v-if="extraOptionLabel" class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
      <el-form-item :label="extraOptionLabel" class="!mb-0">
        <el-select v-model="extraOptionValue" :placeholder="`请选择${extraOptionLabel}`">
          <el-option v-for="item in extraOptionChoices" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
    </div>

    <div class="rounded-[24px] border border-slate-200 bg-white p-5 space-y-5">
      <el-form-item :label="missingHandlerLabel" class="!mb-0">
        <el-select v-model="state.node.config.missingHandler" placeholder="请选择处理策略">
          <el-option v-for="item in missingHandlerOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <template v-if="showApprovalSettings">
        <el-form-item label="审批方式" class="!mb-0">
          <el-radio-group v-model="state.node.config.approvalMode" class="flex flex-wrap gap-3">
            <el-radio-button
              v-for="item in state.meta.approvalModeOptions"
              :key="item.value"
              :label="item.value"
              :disabled="isForcedAndSign && item.value !== 'AND_SIGN'"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <p
          v-if="isApprovalVariant && state.isManagerMultiLevelApproval"
          class="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs leading-6 text-amber-700"
        >
          第 1..N 级主管共同审批：多级主管会按同一审批节点会签处理，自动包含第 1 到第 N 级主管，全部通过后当前节点才会通过。
        </p>

        <p
          v-else-if="showGroupAndSignHint"
          class="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs leading-6 text-amber-700"
        >
          指定用户组模式固定按会签处理；若命中多个成员，全部审批通过后当前节点才会通过。
        </p>

        <el-form-item label="审批意见默认值" class="!mb-0">
          <el-select
            v-model="state.node.config.opinionDefaults"
            multiple
            filterable
            v-bind="globalFilterableSelectProps"
            allow-create
            default-first-option
            placeholder="请输入或选择审批意见"
          >
            <el-option v-for="item in state.approvalOpinionCandidates" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="特殊设置" class="!mb-0">
          <el-checkbox-group v-model="state.node.config.specialSettings" class="flex flex-col gap-3">
            <el-checkbox v-for="item in specialSettingOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import type { ProcessFlowMeta, ProcessFlowNode, ProcessFormOption } from '@/api'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import {
  buildApprovalDesignatedMemberOptions,
  toDesignatedMemberOptionValue
} from '@/views/process/processFlowDesignatedMembers'

type AssigneeVariant = 'approval' | 'payment' | 'cc'

type DesignatedUserGroupConfig = NonNullable<ProcessFlowNode['config']['designatedUserGroupConfig']>
type DesignatedMemberConfig = NonNullable<ProcessFlowNode['config']['designatedMemberConfig']>
type ManualSelectConfig = NonNullable<ProcessFlowNode['config']['manualSelectConfig']>

const props = defineProps({
  state: {
    type: Object as PropType<{
      node: ProcessFlowNode
      meta: ProcessFlowMeta
      approvalApproverTypes: ProcessFlowMeta['approvalApproverTypeOptions']
      approvalOpinionCandidates: string[]
      isManagerMultiLevelApproval?: boolean
      managerApprovalHint?: string
    }>,
    required: true
  },
  actions: {
    type: Object as PropType<{
      openSceneDialog: () => void
    }>,
    required: true
  },
  variant: {
    type: String as PropType<AssigneeVariant>,
    default: 'approval'
  }
})

const isApprovalVariant = computed(() => props.variant === 'approval')
const isPaymentVariant = computed(() => props.variant === 'payment')
const isCcVariant = computed(() => props.variant === 'cc')

function isApprovalStyleType(value: unknown) {
  return ['DESIGNATED_MEMBER', 'DESIGNATED_USER_GROUP', 'MANUAL_SELECT'].includes(String(value || '').trim())
}

const availableApproverTypes = computed(() => (
  (props.state.approvalApproverTypes || []).filter((item) => (
    isApprovalVariant.value || String(item.value || '').trim() !== 'MANAGER'
  ))
))

const legacyApproverType = computed(() => {
  if (isPaymentVariant.value) {
    return String(props.state.node.config.executorType || '').trim()
  }
  if (isCcVariant.value) {
    return String(props.state.node.config.receiverType || '').trim()
  }
  return ''
})

const effectiveApproverType = computed<string | undefined>({
  get() {
    const currentApproverType = String(props.state.node.config.approverType || '').trim()
    if (isApprovalVariant.value) {
      return currentApproverType || 'MANAGER'
    }
    if (isApprovalStyleType(currentApproverType)) {
      return currentApproverType
    }
    if (legacyApproverType.value === 'DESIGNATED_MEMBER') {
      return 'DESIGNATED_MEMBER'
    }
    return undefined
  },
  set(value) {
    props.state.node.config.approverType = value || undefined
  }
})

const showLegacyCompatibilityNotice = computed(() => (
  !isApprovalVariant.value
  && !effectiveApproverType.value
  && Boolean(legacyApproverType.value)
))

const legacyRoleLabel = computed(() => (isPaymentVariant.value ? '执行人' : '抄送人'))

const legacyApproverTypeLabel = computed(() => {
  const optionSource = isPaymentVariant.value
    ? props.state.meta.paymentExecutorTypeOptions
    : props.state.meta.ccReceiverTypeOptions
  return optionSource.find((item) => String(item.value || '').trim() === legacyApproverType.value)?.label || legacyApproverType.value
})

const designatedMemberOptions = computed(() => buildApprovalDesignatedMemberOptions(props.state.meta.userOptions))

const designatedMemberConfig = computed<DesignatedMemberConfig>(() => {
  if (props.state.node.config.designatedMemberConfig) {
    return props.state.node.config.designatedMemberConfig
  }
  const fallback: DesignatedMemberConfig = { userIds: [] }
  props.state.node.config.designatedMemberConfig = fallback
  return fallback
})

const designatedUserGroupConfig = computed<DesignatedUserGroupConfig>(() => {
  if (props.state.node.config.designatedUserGroupConfig) {
    return props.state.node.config.designatedUserGroupConfig
  }
  const fallback: DesignatedUserGroupConfig = {}
  props.state.node.config.designatedUserGroupConfig = fallback
  return fallback
})

const manualSelectConfig = computed<ManualSelectConfig>(() => {
  if (props.state.node.config.manualSelectConfig) {
    return props.state.node.config.manualSelectConfig
  }
  const fallback: ManualSelectConfig = {
    candidateScope: 'ALL_ACTIVE_USERS'
  }
  props.state.node.config.manualSelectConfig = fallback
  return fallback
})

const isForcedAndSign = computed(() => (
  props.state.node.config.approverType === 'DESIGNATED_USER_GROUP'
  || (isApprovalVariant.value && Boolean(props.state.isManagerMultiLevelApproval))
))

const showGroupAndSignHint = computed(() => (
  !isCcVariant.value && effectiveApproverType.value === 'DESIGNATED_USER_GROUP'
))

const missingHandlerLabel = computed(() => (
  isCcVariant.value ? '找不到抄送人时' : '找不到审批人时'
))

const missingHandlerOptions = computed<ProcessFormOption[]>(() => (
  (props.state.meta.missingHandlerOptions || []).map((item) => {
    if (isCcVariant.value && String(item.value || '').trim() === 'BLOCK_SUBMIT') {
      return {
        ...item,
        label: '提单时找不到抄送人不允许提交'
      }
    }
    return item
  })
))

const showApprovalSettings = computed(() => !isCcVariant.value)

const specialSettingOptions = computed(() => {
  if (!isPaymentVariant.value) {
    return props.state.meta.approvalSpecialOptions
  }
  const hiddenValues = new Set([
    'AUTO_PASS_IF_APPOVER_IS_SUBMITTER',
    'AUTO_PASS_IF_APPROVED_BEFORE'
  ])
  return props.state.meta.approvalSpecialOptions.filter((item) => !hiddenValues.has(String(item.value || '').trim()))
})

const extraOptionLabel = computed(() => {
  if (isPaymentVariant.value) {
    return '支付动作'
  }
  if (isCcVariant.value) {
    return '抄送时机'
  }
  return ''
})

const extraOptionChoices = computed(() => {
  if (isPaymentVariant.value) {
    return props.state.meta.paymentActionOptions
  }
  if (isCcVariant.value) {
    return props.state.meta.ccTimingOptions
  }
  return []
})

const extraOptionValue = computed({
  get() {
    if (isPaymentVariant.value) {
      return props.state.node.config.paymentAction
    }
    if (isCcVariant.value) {
      return props.state.node.config.timing
    }
    return undefined
  },
  set(value) {
    if (isPaymentVariant.value) {
      props.state.node.config.paymentAction = String(value || '')
      return
    }
    if (isCcVariant.value) {
      props.state.node.config.timing = String(value || '')
    }
  }
})
</script>
