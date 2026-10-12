<template>
  <div class="space-y-5">
    <el-card shadow="never">
      <template #header><div class="font-semibold">基本信息</div></template>
      <div class="grid gap-4 lg:grid-cols-2">
        <el-form-item label="名称"><el-input v-model="form.name" maxlength="100" :disabled="!canEdit || archived" /></el-form-item>
        <el-form-item label="责任人">
          <el-select v-model="form.responsibleUserId" filterable class="w-full" :disabled="!canEdit || archived">
            <el-option v-for="user in directory.users" :key="user.id" :value="user.id" :label="`${user.name} (${user.username})`" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="职责说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" :disabled="!canEdit || archived" /></el-form-item>
      <el-form-item label="形象标识"><el-input v-model="form.avatarCode" placeholder="例如 finance-duck" :disabled="!canEdit || archived" /></el-form-item>
      <div class="text-right"><el-button v-if="canEdit" type="primary" :disabled="archived" @click="saveBasic">保存基本信息</el-button></div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <div><div class="font-semibold">服务能力</div><p class="text-xs text-slate-500">仅绑定已审核的固定版本；发布新版不会自动替换。</p></div>
          <el-button text type="primary" @click="router.push('/agent/workbench')">前往工作台</el-button>
        </div>
      </template>
      <div v-for="binding in employee.bindings || []" :key="binding.id" class="mb-2 flex items-center justify-between rounded-xl border border-slate-200 p-3">
        <div>
          <strong>{{ binding.agent_name }}</strong>
          <div class="text-xs text-slate-500">版本 #{{ binding.agent_version_id }} · {{ capabilityName(binding.permission_code) }} · {{ bindingReason(binding) }}</div>
        </div>
        <el-button v-if="canEdit && binding.status === 1 && !archived" type="danger" text @click="unbind(binding.id)">停用绑定</el-button>
        <el-tag v-else-if="binding.status !== 1" type="info">已停用</el-tag>
      </div>
      <el-empty v-if="!employee.bindings?.length" description="还没有工作流绑定" />
      <div v-if="canEdit && !archived" class="mt-4 rounded-xl bg-slate-50 p-4">
        <div class="mb-3 font-medium">绑定已审核工作流</div>
        <el-select v-model="selectedShareId" class="w-full" filterable placeholder="选择固定版本">
          <el-option v-for="share in approvedShares" :key="share.id" :value="share.id"
            :label="`${share.agent_name} · v${share.version_no} · ${capabilityName(share.permission_code)}`" />
        </el-select>
        <div v-for="code in selectedShareCodes" :key="code" class="mt-3">
          <div class="mb-1 text-xs text-slate-600">{{ capabilityName(code) }} · {{ code }}</div>
          <el-alert v-if="code === 'system:agent:people:query'" title="人员查询按独立的当前租户全企业权限授权" type="info" :closable="false" />
          <el-select v-else v-model="companyLimits[code]" class="w-full" multiple filterable collapse-tags placeholder="服务范围上限：留空表示仅受角色权限约束">
            <el-option v-for="company in directory.companies" :key="company.company_id" :value="company.company_id" :label="company.company_name" />
          </el-select>
        </div>
        <div class="mt-3 text-right"><el-button type="primary" :disabled="!selectedShare" @click="bind">绑定版本</el-button></div>
      </div>
    </el-card>

    <el-card v-if="canReview" shadow="never">
      <template #header><div class="font-semibold">企业共享审核</div></template>
      <div v-for="share in pendingShares" :key="share.id" class="mb-2 flex items-center justify-between gap-3 rounded-xl border p-3">
        <div>{{ share.agent_name }} · v{{ share.version_no }} <span class="text-xs text-slate-500">{{ capabilityName(share.permission_code) }}</span></div>
        <div><el-button size="small" @click="review(share.id, false)">驳回</el-button><el-button size="small" type="primary" @click="review(share.id, true)">批准</el-button></div>
      </div>
      <el-empty v-if="!pendingShares.length" description="没有待审核的工作流" />
    </el-card>

    <el-card shadow="never">
      <template #header><div class="font-semibold">服务对象与授权交集</div></template>
      <p class="mb-3 text-sm text-slate-500">分配决定谁能调用；查询结果始终取使用者、责任人和服务范围的交集。</p>
      <el-select v-model="assignedUserIds" class="w-full" multiple filterable collapse-tags placeholder="指定使用者" :disabled="!canAssign || archived">
        <el-option v-for="user in directory.users" :key="user.id" :value="user.id" :label="`${user.name} (${user.username})`" />
      </el-select>
      <div v-if="canAssign" class="mt-3 text-right"><el-button type="primary" :disabled="archived" @click="saveAssignments">保存分配</el-button></div>
      <div class="mt-5 grid gap-3 lg:grid-cols-3">
        <el-select v-model="previewUserId" filterable placeholder="选择使用者"><el-option v-for="user in assignedUsers" :key="user.id" :value="user.id" :label="user.name" /></el-select>
        <el-select v-model="previewPermission" placeholder="选择查询能力"><el-option v-for="code in boundCapabilities" :key="code" :value="code" :label="capabilityName(code)" /></el-select>
        <el-button :disabled="!previewUserId || !previewPermission" @click="loadPreview">查看有效范围</el-button>
      </div>
      <el-alert v-if="preview" class="mt-3" :closable="false" :type="preview.authorized ? 'success' : 'warning'"
        :title="preview.authorized ? `可访问 ${preview.companyCount} 家公司：${preview.companyIds?.map(companyName).join('、') || ''}` : (preview.reason || '当前没有查询权限')" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { digitalEmployeeApi, type DigitalEmployee, type EmployeeDirectory, type WorkflowBinding, type WorkflowShare } from '@/api/modules/digital-employee'

const props = defineProps<{ employee: DigitalEmployee; directory: EmployeeDirectory; shares: WorkflowShare[];
  canEdit: boolean; canAssign: boolean; canReview: boolean }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()
const router = useRouter()
const archived = computed(() => props.employee.lifecycle_status === 'ARCHIVED')
const form = reactive({ name: '', description: '', avatarCode: '', responsibleUserId: 0 })
const assignedUserIds = ref<number[]>([])
const selectedShareId = ref<number>()
const companyLimits = reactive<Record<string, string[]>>({})
const previewUserId = ref<number>()
const previewPermission = ref('')
const preview = ref<{ authorized: boolean; companyIds?: string[]; companyCount?: number; reason?: string }>()
const approvedShares = computed(() => props.shares.filter(share => share.status === 'APPROVED'))
const pendingShares = computed(() => props.shares.filter(share => share.status === 'SUBMITTED'))
const selectedShare = computed(() => approvedShares.value.find(share => share.id === selectedShareId.value))
const selectedShareCodes = computed(() => selectedShare.value?.permission_codes?.length
  ? selectedShare.value.permission_codes : selectedShare.value ? [selectedShare.value.permission_code] : [])
const boundCapabilities = computed(() => [...new Set((props.employee.bindings || []).filter(b => b.status === 1)
  .flatMap(binding => props.shares.find(share => share.id === binding.share_id)?.permission_codes || [binding.permission_code]))])
const assignedUsers = computed(() => props.directory.users.filter(user => assignedUserIds.value.includes(user.id)))
watch(() => props.employee, employee => {
  form.name = employee.name; form.description = employee.description || ''; form.avatarCode = employee.avatar_code || ''
  form.responsibleUserId = employee.responsible_user_id; assignedUserIds.value = [...(employee.userIds || [])]
  preview.value = undefined
}, { immediate: true })
watch(selectedShareCodes, codes => { for (const code of codes) companyLimits[code] ||= [] }, { immediate: true })

function capabilityName(code: string) { return code === 'expense:agent:query' ? '报销单查询' : code === 'finance:period_close:query' ? '未结账公司数' : code }
function bindingReason(binding: WorkflowBinding) {
  if (binding.status !== 1) return '绑定已停用'
  if (binding.share_status !== 'APPROVED') return '企业共享审核已失效'
  if (binding.agent_status !== 'READY') return '工作流已停用'
  if (binding.published !== 1) return '固定版本已取消发布'
  return '可用'
}
function companyName(id: string) { return props.directory.companies.find(company => company.company_id === id)?.company_name || id }
function notify(error: unknown) { ElMessage.error(error instanceof Error ? error.message : '操作失败') }
async function saveBasic() {
  try { await digitalEmployeeApi.update(props.employee.id, { ...form, versionNo: props.employee.version_no }); emit('refresh'); ElMessage.success('基本信息已保存') }
  catch (error) { notify(error) }
}
async function saveAssignments() {
  try { await digitalEmployeeApi.assign(props.employee.id, assignedUserIds.value, props.employee.version_no); emit('refresh'); ElMessage.success('使用者已更新') }
  catch (error) { notify(error) }
}
async function bind() {
  if (!selectedShare.value) return
  try {
    const limits = Object.fromEntries(selectedShareCodes.value.map(code => [code, companyLimits[code] || []]))
    await digitalEmployeeApi.bind(props.employee.id, selectedShare.value.id, selectedShare.value.permission_code,
      limits[selectedShare.value.permission_code] || [], props.employee.version_no, limits)
    emit('refresh'); ElMessage.success('固定版本已绑定')
  }
  catch (error) { notify(error) }
}
async function unbind(bindingId: number) {
  try { await digitalEmployeeApi.unbind(props.employee.id, bindingId, props.employee.version_no); emit('refresh'); ElMessage.success('绑定已停用') }
  catch (error) { notify(error) }
}
async function review(shareId: number, approve: boolean) {
  try { await digitalEmployeeApi.reviewShare(shareId, approve); emit('refresh'); ElMessage.success(approve ? '已批准共享' : '已驳回') }
  catch (error) { notify(error) }
}
async function loadPreview() {
  if (!previewUserId.value || !previewPermission.value) return
  try { preview.value = (await digitalEmployeeApi.preview(props.employee.id, previewUserId.value, previewPermission.value)).data }
  catch (error) { notify(error) }
}
</script>
