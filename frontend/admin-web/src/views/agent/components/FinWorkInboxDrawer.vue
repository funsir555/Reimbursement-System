<template>
  <el-drawer :model-value="modelValue" title="FinWork 定时收件箱" size="min(680px, 100%)"
    @update:model-value="$emit('update:modelValue', $event)">
    <div class="space-y-6">
      <section>
        <div class="mb-3 flex items-center justify-between"><h3 class="font-semibold text-slate-900">我的订阅</h3><el-button text @click="reload">刷新</el-button></div>
        <p class="mb-3 text-sm text-slate-500">订阅后，每次定时查询都按您当时的权限单独运行。</p>
        <div v-for="plan in subscriptions" :key="plan.plan_id" class="mb-2 flex items-center justify-between rounded-xl border border-slate-200 p-3">
          <div>
            <strong>{{ plan.employee_name }}</strong><div class="text-xs text-slate-500">{{ plan.agent_name }} · {{ frequencyName(plan.frequency) }} {{ pad(plan.run_hour) }}:{{ pad(plan.run_minute) }}</div>
          </div>
          <el-switch :model-value="plan.subscribed === 1" @change="toggle(plan.plan_id, $event as boolean)" />
        </div>
        <el-empty v-if="!subscriptions.length" description="暂无可订阅的查询计划" />
      </section>
      <section>
        <h3 class="mb-3 font-semibold text-slate-900">查询结果</h3>
        <div v-for="item in inbox" :key="item.id" class="mb-3 rounded-xl border border-slate-200 bg-white p-4">
          <div class="flex justify-between gap-2"><strong>{{ item.employee_name }}</strong><el-tag size="small" :type="item.status === 'SUCCESS' || item.status === 'ZERO' ? 'success' : 'warning'">{{ statusName(item.status) }}</el-tag></div>
          <div class="mt-1 text-xs text-slate-500">{{ item.scheduled_at }}</div>
          <p v-if="item.result_text" class="mt-3 whitespace-pre-wrap text-sm text-slate-700">{{ item.result_text }}</p>
          <p v-else-if="item.status === 'NO_PERMISSION'" class="mt-3 text-sm text-amber-700">系统查询到您没有相关权限，请换一个问题</p>
          <p v-else class="mt-3 text-sm text-slate-500">本次未产生可送达的结果，请联系数字员工管理员查看运行记录。</p>
        </div>
        <el-empty v-if="!inbox.length" description="暂无定时查询结果" />
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { agentChatApi } from '@/api/modules/agent-chat'
const props = defineProps<{ modelValue: boolean }>()
defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()
const subscriptions = ref<Awaited<ReturnType<typeof agentChatApi.subscriptions>>['data']>([])
const inbox = ref<Awaited<ReturnType<typeof agentChatApi.inbox>>['data']>([])
watch(() => props.modelValue, open => { if (open) reload() })
function pad(value: number) { return String(value).padStart(2, '0') }
function frequencyName(value: string) { return ({ DAILY: '每日', WEEKLY: '每周', MONTHLY: '每月' } as Record<string, string>)[value] || value }
function statusName(value: string) { return ({ SUCCESS: '已送达', ZERO: '零结果', NO_PERMISSION: '权限已失效', EXECUTION_FAILED: '执行失败', RUNNING: '执行中' } as Record<string, string>)[value] || value }
async function reload() {
  try { const [a, b] = await Promise.all([agentChatApi.subscriptions(), agentChatApi.inbox()]); subscriptions.value = a.data || []; inbox.value = b.data || [] }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '收件箱加载失败') }
}
async function toggle(planId: number, enabled: boolean) {
  try { await agentChatApi.setSubscription(planId, enabled); await reload(); ElMessage.success(enabled ? '已订阅' : '已取消订阅') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '订阅更新失败') }
}
</script>
