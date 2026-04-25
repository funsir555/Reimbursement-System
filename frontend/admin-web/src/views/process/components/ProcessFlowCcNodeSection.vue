<template>
  <div class="space-y-5">
    <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
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

      <el-form-item label="抄送对象类型" class="!mb-0">
        <el-select v-model="state.node.config.receiverType" placeholder="请选择抄送对象类型">
          <el-option v-for="item in state.meta.ccReceiverTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="指定成员" class="!mb-0">
        <el-select v-model="state.node.config.receiverUserIds" multiple filterable clearable placeholder="请选择抄送成员">
          <el-option v-for="item in state.meta.userOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
        </el-select>
      </el-form-item>

      <el-form-item label="抄送时机" class="!mb-0">
        <el-select v-model="state.node.config.timing" placeholder="请选择抄送时机">
          <el-option v-for="item in state.meta.ccTimingOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="找不到接收人时" class="!mb-0">
        <el-select v-model="state.node.config.missingHandler" placeholder="请选择处理策略">
          <el-option v-for="item in state.meta.missingHandlerOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="特殊设置" class="!mb-0">
        <el-checkbox-group v-model="state.node.config.specialSettings" class="flex flex-col gap-3">
          <el-checkbox v-for="item in state.meta.ccSpecialOptions" :key="item.value" :label="item.value">
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
