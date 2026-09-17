<template>
  <agent-placeholder
    title="数字员工管理"
    description="管理企业级 AI 数字员工，为团队配置专业的 AI 助手"
    :phase="2"
    icon-type="user"
    :features="[
      '创建企业数字员工（财务助手、审批专员等）',
      '配置数字员工的职责和权限',
      '分配数字员工给特定团队或部门',
      '监控数字员工的工作量和效率',
      '数字员工协作和工作流编排'
    ]"
  />
</template>

<script setup lang="ts">
import AgentPlaceholder from '@/components/common/AgentPlaceholder.vue'
</script>
