<template>
  <el-table :data="jobs" size="small">
    <el-table-column prop="jobNo" label="任务号" min-width="180" />
    <el-table-column label="平台" width="100">
      <template #default="{ row }">
        {{ sourceLabelMap[row.platformCode] || row.platformCode }}
      </template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="100" />
    <el-table-column prop="summary" label="结果摘要" min-width="220" />
  </el-table>
</template>

<script setup lang="ts">
import type { SyncJobRecord } from '@/api'

defineProps<{
  jobs: SyncJobRecord[]
  sourceLabelMap: Record<string, string>
}>()
</script>
