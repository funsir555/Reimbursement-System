<template>
  <div class="space-y-5">
    <el-card shadow="never">
      <template #header><div class="flex items-center justify-between"><div class="font-semibold">运营指标</div><el-radio-group v-model="days" size="small"><el-radio-button :value="7">近 7 天</el-radio-button><el-radio-button :value="30">近 30 天</el-radio-button></el-radio-group></div></template>
      <div v-if="metrics" class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div class="metric"><span>使用覆盖</span><strong>{{ metrics.subscribers }} / {{ metrics.assignedUsers }}</strong><small>订阅者 / 已分配使用者</small></div>
        <div class="metric"><span>任务结果</span><strong>{{ metrics.successfulRuns }}</strong><small>成功 · 零结果 {{ metrics.zeroResults }} · 失败 {{ metrics.executionFailures }}</small></div>
        <div class="metric"><span>权限安全</span><strong>{{ metrics.permissionDenials }}</strong><small>无权拒绝次数</small></div>
        <div class="metric"><span>时效与成本</span><strong>{{ metrics.onTimeDeliveries }}</strong><small>按时送达 · 今日 {{ metrics.runsToday }}/{{ metrics.dailyRunLimit || '未设上限' }} 次</small></div>
      </div>
      <el-alert v-if="metrics?.pausedPlans" class="mt-4" type="warning" :closable="false" :title="`${metrics.pausedPlans} 个计划因预算或授权失效暂停，请检查计划与审核状态`" />
      <p class="mt-3 text-xs text-slate-500">以上运行指标仅统计定时投递；按时送达指触发后 10 分钟内完成。运行次数用于预算，不推算模型费用。</p>
    </el-card>
    <el-card v-if="canAudit" shadow="never">
      <template #header><div class="flex items-center justify-between"><div class="font-semibold">配置与运行审计</div><el-button @click="reload">刷新</el-button></div></template>
      <el-table :data="audit" empty-text="暂无记录">
        <el-table-column prop="created_at" label="时间" min-width="160" />
        <el-table-column prop="action_type" label="动作" width="150" />
        <el-table-column prop="decision_reason" label="说明" min-width="230" />
        <el-table-column prop="responsible_user_id" label="操作者 ID" width="110" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { digitalEmployeeApi, type DigitalEmployee, type EmployeeMetrics } from '@/api/modules/digital-employee'
const props = defineProps<{ employee: DigitalEmployee; canAudit: boolean }>()
const days = ref<7 | 30>(7)
const metrics = ref<EmployeeMetrics>()
const audit = ref<Array<Record<string, string | number>>>([])
watch([() => props.employee.id, days], reload, { immediate: true })
async function reload() {
  try {
    metrics.value = (await digitalEmployeeApi.metrics(props.employee.id, days.value)).data
    if (props.canAudit) audit.value = (await digitalEmployeeApi.audit(props.employee.id)).data || []
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '运行信息加载失败') }
}
</script>

<style scoped>
.metric { border: 1px solid #e2e8f0; border-radius: 14px; padding: 16px; background: #f8fafc; }
.metric span, .metric small { display: block; color: #64748b; font-size: 12px; }
.metric strong { display: block; margin: 9px 0; font-size: 23px; color: #0f172a; }
</style>
