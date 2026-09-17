<template>
  <agent-placeholder
    title="业务本体查询"
    description="查询和管理报销系统的业务数据结构，为 AI 提供准确的业务知识"
    :phase="1"
    icon-type="search"
    :features="[
      '查看报销单、发票、员工等业务实体定义',
      '理解业务实体之间的关系',
      '查询业务规则和约束条件',
      '为 AI Agent 提供业务上下文',
      '业务术语标准化管理'
    ]"
  />
</template>

<script setup lang="ts">
import AgentPlaceholder from '@/components/common/AgentPlaceholder.vue'
</script>
