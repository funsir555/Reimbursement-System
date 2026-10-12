<template>
  <div class="digital-employee-page">
    <aside class="employee-sidebar">
      <div class="rounded-2xl bg-slate-900 p-5 text-white">
        <div class="text-xs tracking-widest text-amber-300">ENTERPRISE SERVICE</div>
        <h1 class="mt-2 text-xl font-semibold">数字员工</h1>
        <p class="mt-2 text-sm text-slate-300">把已审核的工作流组织成可分配的查询服务。</p>
        <div class="mt-4 grid grid-cols-2 gap-3 text-center text-sm">
          <div><strong class="block text-xl">{{ activeCount }}</strong>已启用</div>
          <div><strong class="block text-xl">{{ assignedCount }}</strong>用户分配</div>
          <div><strong class="block text-xl">{{ subscriberCount }}</strong>订阅用户</div>
          <div><strong class="block text-xl">{{ exceptionCount }}</strong>近 7 天异常</div>
        </div>
      </div>
      <div class="mt-5 flex gap-2">
        <el-button v-if="canCreate" type="primary" class="flex-1" @click="createVisible = true">新建数字员工</el-button>
        <el-button @click="reload">刷新</el-button>
      </div>
      <el-input v-model="keyword" class="mt-4" clearable placeholder="搜索名称或说明" />
      <el-select v-model="statusFilter" class="mt-2 w-full" clearable placeholder="全部状态">
        <el-option v-for="status in statuses" :key="status" :value="status" :label="statusName(status)" />
      </el-select>
      <div v-loading="loading" class="mt-4 space-y-2 overflow-y-auto">
        <button v-for="item in filtered" :key="item.id" type="button"
          class="w-full rounded-xl border p-4 text-left transition"
          :class="selectedId === item.id ? 'border-amber-500 bg-amber-50' : 'border-slate-200 bg-white hover:border-slate-300'"
          @click="select(item.id)">
          <div class="flex items-center justify-between gap-2">
            <strong class="truncate text-slate-800">{{ item.name }}</strong>
            <el-tag size="small" :type="item.lifecycle_status === 'ACTIVE' ? 'success' : 'info'">{{ statusName(item.lifecycle_status) }}</el-tag>
          </div>
          <div class="mt-1 line-clamp-2 text-xs text-slate-500">{{ item.description || '尚未填写服务说明' }}</div>
          <div class="mt-2 text-xs text-slate-500">{{ item.workflow_count || 0 }} 项能力 · {{ item.assigned_count || 0 }} 位使用者</div>
        </button>
        <el-empty v-if="!filtered.length && !loading" description="暂无数字员工" />
      </div>
    </aside>

    <main class="employee-main">
      <el-empty v-if="!selected" description="选择左侧数字员工，或新建一项服务" />
      <template v-else>
        <header class="rounded-2xl border border-slate-200 bg-white p-5">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div class="text-xs font-medium uppercase tracking-widest text-amber-600">DIGITAL EMPLOYEE</div>
              <h2 class="mt-1 text-2xl font-semibold text-slate-900">{{ selected.name }}</h2>
              <p class="mt-2 text-sm text-slate-500">{{ selected.description || '为这位数字员工补充职责说明。' }}</p>
              <div class="mt-2 text-xs text-slate-400">管理归属公司：{{ companyName(selected.company_id) }} · 配置版本 {{ selected.version_no }}</div>
            </div>
            <div class="flex gap-2">
              <el-button v-if="canStatus && selected.lifecycle_status !== 'ARCHIVED'"
                :type="selected.lifecycle_status === 'ACTIVE' ? 'warning' : 'success'"
                @click="changeStatus(selected.lifecycle_status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE')">
                {{ selected.lifecycle_status === 'ACTIVE' ? '暂停服务' : '启用服务' }}
              </el-button>
              <el-button v-if="canStatus && selected.lifecycle_status !== 'ARCHIVED'" type="danger" plain @click="changeStatus('ARCHIVED')">归档</el-button>
            </div>
          </div>
        </header>
        <el-tabs v-model="tab" class="mt-5">
          <el-tab-pane label="配置与服务能力" name="config">
            <DigitalEmployeeConfigPanel :employee="selected" :directory="directory" :shares="shares"
              :can-edit="canEdit" :can-assign="canAssign" :can-review="canReview" @refresh="refreshSelected" />
          </el-tab-pane>
          <el-tab-pane label="定时计划" name="plans">
            <DigitalEmployeePlanPanel :employee="selected" :shares="shares" :can-plan="canPlan" @refresh="refreshSelected" />
          </el-tab-pane>
          <el-tab-pane label="运行与审计" name="operations">
            <DigitalEmployeeOperationsPanel :employee="selected" :can-audit="canAudit" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </main>

    <el-dialog v-model="createVisible" title="新建数字员工" width="520px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="draft.name" maxlength="100" placeholder="例如：财务结账助手" /></el-form-item>
        <el-form-item label="职责说明"><el-input v-model="draft.description" type="textarea" :rows="3" maxlength="500" /></el-form-item>
        <el-form-item label="责任人">
          <el-select v-model="draft.responsibleUserId" filterable class="w-full" placeholder="选择责任人">
            <el-option v-for="user in directory.users" :key="user.id" :value="user.id" :label="`${user.name} (${user.username})`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="create">创建草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { digitalEmployeeApi, type DigitalEmployee, type EmployeeDirectory, type WorkflowShare } from '@/api/modules/digital-employee'
import { readStoredUser, hasAnyPermission } from '@/utils/permissions'
import DigitalEmployeeConfigPanel from './components/DigitalEmployeeConfigPanel.vue'
import DigitalEmployeePlanPanel from './components/DigitalEmployeePlanPanel.vue'
import DigitalEmployeeOperationsPanel from './components/DigitalEmployeeOperationsPanel.vue'

const permissions = readStoredUser()?.permissionCodes || []
const canCreate = hasAnyPermission(['agent:digital_employee:create'], permissions)
const canEdit = hasAnyPermission(['agent:digital_employee:edit'], permissions)
const canAssign = hasAnyPermission(['agent:digital_employee:assign'], permissions)
const canStatus = hasAnyPermission(['agent:digital_employee:status'], permissions)
const canPlan = hasAnyPermission(['agent:digital_employee:plan'], permissions)
const canAudit = hasAnyPermission(['agent:digital_employee:logs'], permissions)
const canReview = hasAnyPermission(['agent:workbench:share_approve'], permissions)
const loading = ref(false)
const items = ref<DigitalEmployee[]>([])
const selectedId = ref<number>()
const selected = ref<DigitalEmployee>()
const shares = ref<WorkflowShare[]>([])
const directory = ref<EmployeeDirectory>({ users: [], companies: [] })
const keyword = ref('')
const statusFilter = ref('')
const tab = ref('config')
const createVisible = ref(false)
const draft = ref({ name: '', description: '', responsibleUserId: undefined as number | undefined })
const statuses = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'ARCHIVED']
const filtered = computed(() => items.value.filter(item =>
  (!statusFilter.value || item.lifecycle_status === statusFilter.value)
  && (!keyword.value || `${item.name} ${item.description || ''}`.toLowerCase().includes(keyword.value.toLowerCase()))))
const activeCount = computed(() => items.value.filter(item => item.lifecycle_status === 'ACTIVE').length)
const assignedCount = computed(() => items.value.reduce((sum, item) => sum + (item.assigned_count || 0), 0))
const subscriberCount = computed(() => items.value.reduce((sum, item) => sum + (item.subscriber_count || 0), 0))
const exceptionCount = computed(() => items.value.reduce((sum, item) => sum + (item.exception_count || 0), 0))

function statusName(status: string) {
  return ({ DRAFT: '草稿', ACTIVE: '启用', SUSPENDED: '暂停', ARCHIVED: '归档' } as Record<string, string>)[status] || status
}
function companyName(id: string) { return directory.value.companies.find(item => item.company_id === id)?.company_name || id }

async function reload() {
  loading.value = true
  try {
    const [list, directoryResult, shareResult] = await Promise.all([
      digitalEmployeeApi.list(), digitalEmployeeApi.directory(), digitalEmployeeApi.shares()
    ])
    items.value = list.data || []
    directory.value = directoryResult.data
    shares.value = shareResult.data || []
    if (selectedId.value) await select(selectedId.value)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '数字员工加载失败') }
  finally { loading.value = false }
}
async function select(id: number) {
  selectedId.value = id
  try { selected.value = (await digitalEmployeeApi.detail(id)).data }
  catch (error) { selected.value = undefined; ElMessage.error(error instanceof Error ? error.message : '详情加载失败') }
}
async function refreshSelected() { await reload() }
async function create() {
  if (!draft.value.name.trim() || !draft.value.responsibleUserId) { ElMessage.warning('请填写名称和责任人'); return }
  try {
    const result = await digitalEmployeeApi.create({ name: draft.value.name.trim(), description: draft.value.description,
      responsibleUserId: draft.value.responsibleUserId })
    createVisible.value = false
    selectedId.value = result.data.id
    await reload()
    ElMessage.success('数字员工草稿已创建')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '创建失败') }
}
async function changeStatus(status: string) {
  if (!selected.value) return
  try {
    if (status === 'ARCHIVED') await ElMessageBox.confirm('归档后不能恢复，已有历史将保留。', '确认归档', { type: 'warning' })
    await digitalEmployeeApi.status(selected.value.id, status, selected.value.version_no)
    await reload()
    ElMessage.success('状态已更新')
  } catch (error) { if (status !== 'ARCHIVED') ElMessage.error(error instanceof Error ? error.message : '状态更新失败') }
}
onMounted(reload)
</script>

<style scoped>
.digital-employee-page { display: grid; grid-template-columns: minmax(260px, 320px) minmax(0, 1fr); gap: 20px; min-height: 100%; padding: 20px; background: #f8fafc; }
.employee-sidebar, .employee-main { min-width: 0; }
.employee-sidebar { max-height: calc(100vh - 130px); overflow-y: auto; }
@media (max-width: 900px) { .digital-employee-page { grid-template-columns: 1fr; } .employee-sidebar { max-height: none; } }
</style>
