<template>
  <agent-placeholder
    title="企业知识库管理"
    description="构建和管理企业专属知识库，让 AI 助手具备企业特有的专业知识"
    :phase="2"
    icon-type="reading"
    :features="[
      '上传企业文档和制度文件',
      '构建企业知识图谱',
      '向量化存储和语义检索',
      '知识库版本管理和更新',
      '知识库权限控制和安全',
      '知识库使用统计和优化'
    ]"
  />
</template>

<script setup lang="ts">
import AgentPlaceholder from '@/components/common/AgentPlaceholder.vue'
</script>
