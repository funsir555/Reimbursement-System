<template>
  <div class="space-y-5">
    <el-card shadow="never">
      <template #header><div class="font-semibold">运行预算</div></template>
      <p class="mb-3 text-sm text-slate-500">每位订阅者独立执行。达到每日上限时，计划自动暂停。</p>
      <div class="grid gap-3 md:grid-cols-2">
        <el-form-item label="每日运行上限"><el-input-number v-model="budget.daily" :min="1" :max="100000" :disabled="!canPlan" /></el-form-item>
        <el-form-item label="并发上限"><el-input-number v-model="budget.concurrent" :min="1" :max="1000" :disabled="!canPlan" /></el-form-item>
      </div>
      <el-button v-if="canPlan" type="primary" @click="saveBudget">保存预算</el-button>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="flex items-center justify-between"><div class="font-semibold">定时查询计划</div><el-button @click="reload">刷新</el-button></div></template>
      <el-table :data="plans" empty-text="暂无计划">
        <el-table-column label="工作流" min-width="140"><template #default="{ row }">{{ bindingName(row.workflow_binding_id) }}</template></el-table-column>
        <el-table-column label="频率" width="90"><template #default="{ row }">{{ frequencyName(row.frequency) }}</template></el-table-column>
        <el-table-column label="时间" width="100"><template #default="{ row }">{{ pad(row.run_hour) }}:{{ pad(row.run_minute) }}</template></el-table-column>
        <el-table-column prop="subscriber_count" label="订阅人数" width="100" />
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column v-if="canPlan" label="操作" width="90"><template #default="{ row }"><el-button text type="primary" @click="edit(row)">编辑</el-button></template></el-table-column>
      </el-table>
      <div v-if="canPlan" class="mt-5 rounded-xl bg-slate-50 p-4">
        <div class="mb-4 font-medium">{{ selectedPlanId ? '编辑计划' : '新建计划' }}</div>
        <div class="grid gap-3 lg:grid-cols-2">
          <el-form-item label="受控工作流">
            <el-select v-model="draft.workflowBindingId" class="w-full" placeholder="选择已审核绑定">
              <el-option v-for="binding in activeBindings" :key="binding.id" :value="binding.id" :label="`${binding.agent_name} · ${capabilityName(binding.permission_code)}`" />
            </el-select>
          </el-form-item>
          <el-form-item label="频率"><el-select v-model="draft.frequency" class="w-full"><el-option label="每日" value="DAILY" /><el-option label="每周" value="WEEKLY" /><el-option label="每月" value="MONTHLY" /></el-select></el-form-item>
          <el-form-item label="执行小时"><el-input-number v-model="draft.runHour" :min="0" :max="23" /></el-form-item>
          <el-form-item label="执行分钟"><el-input-number v-model="draft.runMinute" :min="0" :max="59" /></el-form-item>
          <el-form-item v-if="draft.frequency === 'WEEKLY'" label="星期"><el-select v-model="draft.weekDay" class="w-full"><el-option v-for="day in 7" :key="day" :value="day" :label="`星期${day === 7 ? '日' : ['一','二','三','四','五','六'][day - 1]}`" /></el-select></el-form-item>
          <el-form-item v-if="draft.frequency === 'MONTHLY'" label="每月几日"><el-input-number v-model="draft.monthDay" :min="1" :max="28" /></el-form-item>
          <template v-if="legacySelected && selectedPermission === 'finance:period_close:query'">
            <el-form-item label="会计年度"><el-input-number v-model="draft.year" :min="2000" :max="2100" /></el-form-item>
            <el-form-item label="会计期间"><el-input-number v-model="draft.period" :min="1" :max="12" /></el-form-item>
          </template>
          <el-form-item v-else-if="legacySelected && selectedPermission === 'expense:agent:query'" label="查询范围">
            <el-select v-model="draft.scope" class="w-full"><el-option label="我的报销单" value="MY" /><el-option label="待我处理" value="ASSIGNED" /><el-option label="可见范围" value="VISIBLE" /></el-select>
          </el-form-item>
        </div>
        <div v-if="!legacySelected" class="mt-3 space-y-3">
          <div v-for="node in selectedToolNodes" :key="node.nodeKey" class="rounded-lg border border-slate-200 bg-white p-3">
            <div class="mb-2 font-medium">{{ node.toolCode }} · {{ node.nodeKey }}</div>
            <div class="grid gap-3 lg:grid-cols-2">
              <el-form-item v-for="field in node.fields" :key="field.key" :label="`${field.label}${field.required ? '（必填）' : ''}`">
                <el-select v-if="field.type === 'voucherStatus'" v-model="getNodeParams(node.nodeKey)[field.key]" class="w-full" placeholder="凭证范围">
                  <el-option label="仅已记账" value="POSTED_ONLY" /><el-option label="包含已保存未记账" value="INCLUDE_SAVED_UNPOSTED" />
                </el-select>
                <el-input-number v-else-if="field.type === 'integer'" v-model="getNodeParams(node.nodeKey)[field.key]" :min="1" :controls="false" />
                <el-input v-else v-model="getNodeParams(node.nodeKey)[field.key]" />
              </el-form-item>
            </div>
          </div>
        </div>
        <div class="flex items-center justify-between gap-3">
          <el-switch v-model="draft.enabled" active-text="启用计划" inactive-text="保存草稿" />
          <div><el-button @click="reset">清空</el-button><el-button type="primary" :disabled="!draft.workflowBindingId" @click="savePlan">保存计划</el-button></div>
        </div>
        <p class="mt-3 text-xs text-slate-500">当前订阅预计每次触发 {{ estimatedSubscribers }} 人次，最多有 {{ employee.userIds?.length || 0 }} 位已分配使用者可订阅；每日运行上限 {{ employee.daily_run_limit || '未设置' }}。执行前重新检查权限。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { digitalEmployeeApi, type DigitalEmployee, type EmployeePlan, type PlanPayload, type WorkflowShare } from '@/api/modules/digital-employee'

const props = defineProps<{ employee: DigitalEmployee; shares: WorkflowShare[]; canPlan: boolean }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()
const plans = ref<EmployeePlan[]>([])
const selectedPlanId = ref<number>()
const selectedPlanVersion = ref<number>()
const budget = reactive({ daily: 1, concurrent: 1 })
const draft = reactive({ workflowBindingId: undefined as number | undefined, frequency: 'DAILY' as PlanPayload['frequency'],
  runHour: 9, runMinute: 0, weekDay: 1, monthDay: 1, year: new Date().getFullYear(), period: new Date().getMonth() + 1,
  scope: 'MY', enabled: false })
const activeBindings = computed(() => (props.employee.bindings || []).filter(item =>
  item.status === 1 && item.share_status === 'APPROVED' && item.agent_status === 'READY' && item.published === 1))
const selectedPermission = computed(() => activeBindings.value.find(item => item.id === draft.workflowBindingId)?.permission_code)
const selectedBinding = computed(() => activeBindings.value.find(item => item.id === draft.workflowBindingId))
const selectedShare = computed(() => props.shares.find(item => item.id === selectedBinding.value?.share_id))
const selectedToolNodes = computed(() => selectedShare.value?.tool_nodes || [])
const legacySelected = computed(() => selectedToolNodes.value.length === 1 &&
  ['expense.query_my_expenses', 'finance.query_unclosed_companies'].includes(selectedToolNodes.value[0]?.toolCode || ''))
const nodeParams = reactive<Record<string, Record<string, string | number | undefined>>>({})
function getNodeParams(key: string) { return nodeParams[key] ||= { page: 1, pageSize: 50 } }
watch(selectedToolNodes, nodes => { for (const node of nodes) nodeParams[node.nodeKey] ||= { page: 1, pageSize: 50 } }, { immediate: true })
const estimatedSubscribers = computed(() => selectedPlanId.value ? plans.value.find(item => item.id === selectedPlanId.value)?.subscriber_count || 0 : 0)
watch(() => props.employee.id, reload, { immediate: true })
watch(() => props.employee.version_no, () => { budget.daily = props.employee.daily_run_limit || 1; budget.concurrent = props.employee.concurrency_limit || 1 }, { immediate: true })
function pad(value: number) { return String(value).padStart(2, '0') }
function frequencyName(value: string) { return ({ DAILY: '每日', WEEKLY: '每周', MONTHLY: '每月' } as Record<string, string>)[value] || value }
function capabilityName(code: string) { return code === 'finance:period_close:query' ? '未结账公司数' : '报销单查询' }
function bindingName(id: number) { return (props.employee.bindings || []).find(item => item.id === id)?.agent_name || `工作流 #${id}` }
async function reload() {
  try { plans.value = (await digitalEmployeeApi.plans(props.employee.id)).data || [] }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '计划加载失败') }
}
async function saveBudget() {
  try { await digitalEmployeeApi.budget(props.employee.id, budget.daily, budget.concurrent, props.employee.version_no); emit('refresh'); ElMessage.success('预算已保存') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '预算保存失败') }
}
function edit(plan: EmployeePlan) {
  selectedPlanId.value = plan.id; selectedPlanVersion.value = plan.version_no
  draft.workflowBindingId = plan.workflow_binding_id; draft.frequency = plan.frequency
  draft.runHour = plan.run_hour; draft.runMinute = plan.run_minute
  draft.weekDay = plan.week_day || 1; draft.monthDay = plan.month_day || 1; draft.enabled = plan.status === 'ACTIVE'
  try { const params = JSON.parse(plan.parameter_json) as Record<string, any>
    draft.year = Number(params.year) || draft.year; draft.period = Number(params.period) || draft.period
    draft.scope = String(params.scope || 'MY')
    for (const [key, value] of Object.entries(params)) if (value && typeof value === 'object' && !Array.isArray(value))
      nodeParams[key] = { ...(value as Record<string, string | number>) }
  } catch { ElMessage.warning('计划参数无法解析，请重新填写') }
}
function reset() { selectedPlanId.value = undefined; selectedPlanVersion.value = undefined; draft.workflowBindingId = undefined; draft.enabled = false }
async function savePlan() {
  if (!draft.workflowBindingId) return
  const parameters: Record<string, unknown> = legacySelected.value
    ? selectedPermission.value === 'finance:period_close:query'
      ? { year: draft.year, period: draft.period } : { scope: draft.scope }
    : Object.fromEntries(selectedToolNodes.value.map(node => [node.nodeKey,
        Object.fromEntries(Object.entries(nodeParams[node.nodeKey] || {}).filter(([, value]) => value !== undefined && value !== ''))]))
  try {
    await digitalEmployeeApi.savePlan(props.employee.id, { workflowBindingId: draft.workflowBindingId, frequency: draft.frequency,
      runHour: draft.runHour, runMinute: draft.runMinute, weekDay: draft.frequency === 'WEEKLY' ? draft.weekDay : undefined,
      monthDay: draft.frequency === 'MONTHLY' ? draft.monthDay : undefined, parameters,
      versionNo: selectedPlanVersion.value, enabled: draft.enabled }, selectedPlanId.value)
    reset(); await reload(); ElMessage.success('计划已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '计划保存失败') }
}
</script>
