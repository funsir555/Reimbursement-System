<template>
  <ProcessFlowAssigneeNodeSection v-bind="props" variant="cc" />
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import type { ProcessFlowMeta, ProcessFlowNode } from '@/api'
import ProcessFlowAssigneeNodeSection from '@/views/process/components/ProcessFlowAssigneeNodeSection.vue'

const props = defineProps({
  state: {
    type: Object as PropType<{
      node: ProcessFlowNode
      meta: ProcessFlowMeta
      approvalApproverTypes: ProcessFlowMeta['approvalApproverTypeOptions']
      approvalOpinionCandidates: string[]
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
