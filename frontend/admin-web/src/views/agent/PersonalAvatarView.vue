<template>
  <agent-placeholder
    title="个人数字分身管理"
    description="创建和管理您的个人 AI 数字分身，让 AI 代理人以您的风格处理日常事务"
    :phase="1"
    icon-type="user"
    :features="[
      '创建个人数字分身配置',
      '设置个人工作风格和偏好',
      '训练数字分身的决策模式',
      '查看数字分身的执行历史',
      '个人数据隐私保护'
    ]"
  />
</template>

<script setup lang="ts">
import AgentPlaceholder from '@/components/common/AgentPlaceholder.vue'
</script>
