<template>
  <div class="space-y-6">
    <section class="rounded-3xl bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-sm text-slate-500">System Settings</p>
          <h1 class="mt-2 text-3xl font-bold text-slate-900">系统设置中心</h1>
          <p class="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
            统一维护组织架构、员工管理、权限管理和公司管理，同时提供三方组织同步配置。
          </p>
        </div>
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div class="rounded-2xl bg-slate-50 px-4 py-3"><div class="text-xs text-slate-500">部门数</div><div class="mt-2 text-2xl font-semibold text-slate-900">{{ departmentCount }}</div></div>
          <div class="rounded-2xl bg-slate-50 px-4 py-3"><div class="text-xs text-slate-500">员工数</div><div class="mt-2 text-2xl font-semibold text-slate-900">{{ employees.length }}</div></div>
          <div class="rounded-2xl bg-slate-50 px-4 py-3"><div class="text-xs text-slate-500">角色数</div><div class="mt-2 text-2xl font-semibold text-slate-900">{{ roles.length }}</div></div>
          <div class="rounded-2xl bg-slate-50 px-4 py-3"><div class="text-xs text-slate-500">公司数</div><div class="mt-2 text-2xl font-semibold text-slate-900">{{ companyCount }}</div></div>
        </div>
      </div>
    </section>

    <el-card v-loading="loading" shadow="never" class="rounded-3xl border-0">
      <el-empty
        v-if="!loading && bootstrapError"
        description="系统设置初始化失败，请联系管理员或查看后端日志"
      />
      <el-empty
        v-else-if="!loading && !visibleTabs.length"
        description="当前账号未被授予系统设置访问权限"
      />

      <el-tabs v-else v-model="activeTab" class="settings-tabs">
        <el-tab-pane v-if="can('settings:organization:view')" label="组织架构" name="organization">
          <div class="grid gap-6 xl:grid-cols-[1.2fr,1fr]">
            <el-card shadow="never">
              <template #header>
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <div class="text-base font-semibold text-slate-900">部门树</div>
                    <div class="text-xs text-slate-500">手工部门不会被自动同步覆盖</div>
                  </div>
                  <div class="flex gap-2">
                    <el-button v-if="can('settings:organization:create')" type="primary" @click="openDepartmentDialog()">新增部门</el-button>
                    <el-button v-if="can('settings:organization:delete')" :disabled="!selectedDepartment" type="danger" plain @click="handleDeleteDepartment">删除</el-button>
                  </div>
                </div>
              </template>

              <el-tree
                :data="departments"
                node-key="id"
                highlight-current
                default-expand-all
                :current-node-key="selectedDepartmentId"
                @node-click="handleDepartmentSelect"
              >
                <template #default="{ data }">
                  <div class="flex w-full items-center justify-between gap-3 py-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-slate-800">{{ data.deptName }}</span>
                      <el-tag size="small" :type="data.syncManaged ? 'warning' : 'success'">
                        {{ sourceLabelMap[data.syncSource] || data.syncSource }}
                      </el-tag>
                    </div>
                    <span class="text-xs text-slate-400">{{ data.deptCode }}</span>
                  </div>
                </template>
              </el-tree>
            </el-card>

            <div class="space-y-6">
              <el-card shadow="never">
                <template #header><div class="font-semibold text-slate-900">部门配置</div></template>
                <el-empty v-if="!selectedDepartment" description="请选择左侧部门节点" />
                <div v-else class="space-y-4">
                  <el-alert
                    v-if="selectedDepartmentSyncLocked"
                    type="warning"
                    :closable="false"
                    title="该部门由同步接管，不能手工修改"
                  />
                  <el-form label-width="92px">
                    <el-form-item label="部门名称">
                      <el-input v-model="departmentConfigForm.deptName" :disabled="!departmentConfigEditable" />
                    </el-form-item>
                    <el-form-item label="上级部门">
                      <el-tree-select
                        v-model="departmentConfigForm.parentId"
                        :data="departmentParentOptions"
                        node-key="id"
                        check-strictly
                        clearable
                        :disabled="!departmentConfigEditable"
                        :props="{ label: 'deptName', children: 'children', value: 'id' }"
                      />
                    </el-form-item>
                    <el-form-item label="所属公司">
                      <el-select v-model="departmentConfigForm.companyId" clearable class="w-full" :disabled="!departmentConfigEditable">
                        <el-option v-for="item in companyOptions" :key="item.companyId" :label="item.label" :value="item.companyId" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="部门负责人">
                      <el-select v-model="departmentConfigForm.leaderUserId" clearable filterable class="w-full" :disabled="!departmentConfigEditable">
                        <el-option v-for="item in employeeOptions" :key="item.userId" :label="item.label" :value="item.userId" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="部门编码">
                      <el-input :model-value="selectedDepartment.deptCode" disabled />
                    </el-form-item>
                    <el-form-item label="同步来源">
                      <el-input :model-value="sourceLabelMap[selectedDepartment.syncSource] || selectedDepartment.syncSource" disabled />
                    </el-form-item>
                  </el-form>
                  <div v-if="departmentConfigEditable" class="flex justify-end">
                    <el-button type="primary" @click="saveDepartmentConfig">保存配置</el-button>
                  </div>
                </div>
              </el-card>

              <el-card shadow="never">
                <template #header><div class="font-semibold text-slate-900">同步连接配置</div></template>
                <div class="space-y-4">
                  <div v-for="connector in connectors" :key="connector.platformCode" class="rounded-2xl border border-slate-200 p-4">
                    <div class="mb-3 flex items-center justify-between">
                      <div>
                        <div class="font-semibold text-slate-900">{{ connector.platformName }}</div>
                        <div class="text-xs text-slate-500">{{ connector.lastSyncMessage || '尚未同步' }}</div>
                      </div>
                      <div class="flex items-center gap-3">
                        <el-switch v-model="connector.enabled" active-text="启用" />
                        <el-switch v-model="connector.autoSyncEnabled" active-text="自动同步" />
                      </div>
                    </div>
                    <div class="grid gap-3 md:grid-cols-2">
                      <el-input v-model="connector.appKey" placeholder="App Key / Client ID" />
                      <el-input v-model="connector.appSecret" placeholder="App Secret" show-password />
                      <el-input v-model="connector.appId" placeholder="App ID" />
                      <el-input v-model="connector.corpId" placeholder="Corp / Tenant ID" />
                      <el-input v-model="connector.agentId" placeholder="Agent ID" />
                      <el-input-number v-model="connector.syncIntervalMinutes" :min="5" :max="1440" class="w-full" />
                    </div>
                    <div class="mt-4 flex gap-2">
                      <el-button v-if="can('settings:organization:sync_config')" type="primary" @click="saveConnector(connector)">保存配置</el-button>
                      <el-button v-if="can('settings:organization:run_sync')" @click="runConnectorSync(connector.platformCode)">立即同步</el-button>
                    </div>
                  </div>
                </div>
              </el-card>

              <el-card shadow="never">
                <template #header><div class="font-semibold text-slate-900">同步日志</div></template>
                <el-table :data="jobs" size="small">
                  <el-table-column prop="jobNo" label="任务号" min-width="180" />
                  <el-table-column prop="platformCode" label="平台" width="100" />
                  <el-table-column prop="status" label="状态" width="100" />
                  <el-table-column prop="summary" label="结果摘要" min-width="220" />
                </el-table>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="can('settings:employees:view')" label="员工管理" name="employees">
          <el-card shadow="never">
            <template #header>
              <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div class="flex flex-wrap gap-3">
                  <el-input v-model="employeeKeyword" placeholder="搜索姓名/用户名/手机号/邮箱" clearable class="w-72" />
                  <el-select v-model="employeeStatusFilter" clearable placeholder="状态" class="w-32">
                    <el-option label="启用" :value="1" />
                    <el-option label="停用" :value="0" />
                  </el-select>
                </div>
                <div class="flex gap-2">
                  <el-button v-if="can('settings:employees:create')" type="primary" @click="openEmployeeDialog()">新增员工</el-button>
                  <el-button v-if="can('settings:employees:edit')" :disabled="!selectedEmployee" @click="openEmployeeDialog(selectedEmployee)">编辑</el-button>
                  <el-button v-if="can('settings:employees:delete')" :disabled="!selectedEmployee" type="danger" plain @click="handleDeleteEmployee">删除</el-button>
                </div>
              </div>
            </template>
            <el-table :data="filteredEmployees" highlight-current-row @current-change="selectedEmployee = $event">
              <el-table-column prop="name" label="姓名" min-width="140" />
              <el-table-column prop="username" label="用户名" min-width="140" />
              <el-table-column prop="phone" label="手机号" width="140" />
              <el-table-column prop="deptName" label="部门" min-width="120" />
              <el-table-column prop="companyName" label="公司" min-width="150" />
              <el-table-column prop="sourceType" label="来源" width="100" />
              <el-table-column label="角色" min-width="220">
                <template #default="{ row }">
                  <el-tag v-for="roleCode in row.roleCodes" :key="roleCode" size="small" class="mr-1 mb-1">{{ roleCode }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <el-tab-pane v-if="can('settings:roles:view')" label="权限管理" name="roles">
          <div class="grid gap-6 xl:grid-cols-[1fr,1.2fr]">
            <el-card shadow="never">
              <template #header>
                <div class="flex items-center justify-between">
                  <div class="font-semibold text-slate-900">角色列表</div>
                  <div class="flex gap-2">
                    <el-button v-if="can('settings:roles:create')" type="primary" @click="openRoleDialog()">新增角色</el-button>
                    <el-button v-if="can('settings:roles:edit')" :disabled="!selectedRole" @click="openRoleDialog(selectedRole)">编辑</el-button>
                    <el-button v-if="can('settings:roles:delete')" :disabled="!selectedRole" type="danger" plain @click="handleDeleteRole">删除</el-button>
                  </div>
                </div>
              </template>
              <el-table :data="roles" highlight-current-row @current-change="handleRoleSelect">
                <el-table-column prop="roleName" label="角色名称" min-width="150" />
                <el-table-column prop="roleCode" label="编码" min-width="150" />
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
                </el-table-column>
              </el-table>
            </el-card>

            <div class="space-y-6">
              <el-card shadow="never">
                <template #header>
                  <div class="flex items-center justify-between">
                    <div class="font-semibold text-slate-900">菜单与按钮权限</div>
                    <el-button v-if="can('settings:roles:assign_permissions')" :disabled="!selectedRole" type="primary" @click="saveRolePermissions">保存权限</el-button>
                  </div>
                </template>
                <el-empty v-if="!selectedRole" description="请选择左侧角色" />
                <el-tree
                  v-else
                  ref="permissionTreeRef"
                  :data="permissions"
                  node-key="permissionCode"
                  show-checkbox
                  default-expand-all
                  :props="{ label: 'permissionName', children: 'children' }"
                />
              </el-card>

              <el-card shadow="never">
                <template #header>
                  <div class="flex items-center justify-between">
                    <div class="font-semibold text-slate-900">用户角色分配</div>
                    <el-button v-if="can('settings:roles:assign_users')" :disabled="!selectedRole" type="primary" @click="saveRoleUsers">保存用户</el-button>
                  </div>
                </template>
                <el-empty v-if="!selectedRole" description="请选择左侧角色" />
                <el-select v-else v-model="selectedRoleUserIds" multiple filterable placeholder="选择员工" class="w-full">
                  <el-option v-for="employee in employees" :key="employee.userId" :label="`${employee.name} (${employee.username})`" :value="employee.userId" />
                </el-select>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="can('settings:companies:view')" label="公司管理" name="companies">
          <el-card shadow="never">
            <template #header>
              <div class="flex items-center justify-between">
                <div class="font-semibold text-slate-900">公司主体与抬头</div>
                <div class="flex gap-2">
                  <el-button v-if="can('settings:companies:create')" type="primary" @click="openCompanyDialog()">新增公司</el-button>
                  <el-button v-if="can('settings:companies:edit')" :disabled="!selectedCompany" @click="openCompanyDialog(selectedCompany)">编辑</el-button>
                  <el-button v-if="can('settings:companies:delete')" :disabled="!selectedCompany" type="danger" plain @click="handleDeleteCompany">删除</el-button>
                </div>
              </div>
            </template>
            <el-table :data="flatCompanies" highlight-current-row @current-change="selectedCompany = $event">
              <el-table-column label="公司名称" min-width="180">
                <template #default="{ row }">
                  <div :style="{ paddingLeft: `${row.level * 20}px` }">{{ row.companyName }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="companyCode" label="编号" min-width="120" />
              <el-table-column prop="invoiceTitle" label="公司抬头" min-width="180" />
              <el-table-column prop="taxNo" label="税号" min-width="180" />
              <el-table-column prop="bankName" label="开户行" min-width="180" />
            </el-table>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="departmentDialogVisible" title="新增部门" width="520px">
      <el-form label-width="92px">
        <div class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">部门编码将在保存后自动生成</div>
        <el-form-item label="部门名称"><el-input v-model="departmentForm.deptName" /></el-form-item>
        <el-form-item label="上级部门"><el-tree-select v-model="departmentForm.parentId" :data="departmentOptions" node-key="id" check-strictly clearable :props="{ label: 'deptName', children: 'children', value: 'id' }" /></el-form-item>
        <el-form-item label="所属公司"><el-select v-model="departmentForm.companyId" clearable class="w-full"><el-option v-for="item in companyOptions" :key="item.companyId" :label="item.label" :value="item.companyId" /></el-select></el-form-item>
        <el-form-item label="部门负责人"><el-select v-model="departmentForm.leaderUserId" clearable filterable class="w-full"><el-option v-for="item in employeeOptions" :key="item.userId" :label="item.label" :value="item.userId" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="departmentDialogVisible = false">取消</el-button><el-button type="primary" @click="saveDepartment">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="employeeDialogVisible" :title="employeeForm.userId ? '编辑员工' : '新增员工'" width="560px">
      <el-form label-width="92px">
        <el-form-item label="用户名"><el-input v-model="employeeForm.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="employeeForm.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="employeeForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="employeeForm.email" /></el-form-item>
        <el-form-item label="所属部门"><el-tree-select v-model="employeeForm.deptId" :data="departmentOptions" node-key="id" check-strictly clearable :props="{ label: 'deptName', children: 'children', value: 'id' }" /></el-form-item>
        <el-form-item label="所属公司"><el-select v-model="employeeForm.companyId" clearable class="w-full"><el-option v-for="item in companyOptions" :key="item.companyId" :label="item.label" :value="item.companyId" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="employeeDialogVisible = false">取消</el-button><el-button type="primary" @click="saveEmployee">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="roleForm.id ? '编辑角色' : '新增角色'" width="520px">
      <el-form label-width="92px">
        <el-form-item label="角色编码"><el-input v-model="roleForm.roleCode" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="roleForm.roleName" /></el-form-item>
        <el-form-item label="角色说明"><el-input v-model="roleForm.roleDescription" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="roleDialogVisible = false">取消</el-button><el-button type="primary" @click="saveRole">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="companyDialogVisible" :title="selectedCompany ? '编辑公司' : '新增公司'" width="620px">
      <el-form label-width="100px">
        <div class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
          <template v-if="selectedCompany">
            <div>主体编码：{{ selectedCompany.companyId }}</div>
            <div class="mt-1">公司编号：{{ selectedCompany.companyCode }}</div>
          </template>
          <template v-else>主体编码和公司编号将在保存后自动生成</template>
        </div>
        <el-form-item label="公司名称"><el-input v-model="companyForm.companyName" /></el-form-item>
        <el-form-item label="公司抬头"><el-input v-model="companyForm.invoiceTitle" /></el-form-item>
        <el-form-item label="税号"><el-input v-model="companyForm.taxNo" /></el-form-item>
        <el-form-item label="开户行"><el-input v-model="companyForm.bankName" /></el-form-item>
        <el-form-item label="账户名"><el-input v-model="companyForm.bankAccountName" /></el-form-item>
        <el-form-item label="银行账号"><el-input v-model="companyForm.bankAccountNo" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="companyDialogVisible = false">取消</el-button><el-button type="primary" @click="saveCompany">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hasAnyPermission, hasPermission } from '@/utils/permissions'
import {
  systemSettingsApi,
  type CompanyRecord,
  type CompanySavePayload,
  type DepartmentTreeNode,
  type DepartmentSavePayload,
  type EmployeeRecord,
  type EmployeeSavePayload,
  type RoleRecord,
  type RoleSavePayload,
  type SyncConnectorConfig,
  type SystemSettingsBootstrapData
} from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const bootstrapError = ref(false)
const activeTab = ref('organization')
const departments = ref<DepartmentTreeNode[]>([])
const employees = ref<EmployeeRecord[]>([])
const roles = ref<RoleRecord[]>([])
const permissions = ref<any[]>([])
const companies = ref<CompanyRecord[]>([])
const connectors = ref<SyncConnectorConfig[]>([])
const jobs = ref<any[]>([])
const currentUser = ref<SystemSettingsBootstrapData['currentUser'] | null>(null)

const selectedDepartmentId = ref<number>()
const selectedEmployee = ref<EmployeeRecord>()
const selectedRole = ref<RoleRecord>()
const selectedCompany = ref<any>()
const selectedRoleUserIds = ref<number[]>([])
const permissionTreeRef = ref<any>()

const employeeKeyword = ref('')
const employeeStatusFilter = ref<number>()

const departmentDialogVisible = ref(false)
const employeeDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const companyDialogVisible = ref(false)

const departmentForm = reactive<{ deptName: string; parentId?: number; companyId?: string; leaderUserId?: number }>({ deptName: '' })
const departmentConfigForm = reactive<{ deptName: string; parentId?: number; companyId?: string; leaderUserId?: number }>({ deptName: '' })
const employeeForm = reactive<{ userId?: number; username: string; name: string; phone?: string; email?: string; deptId?: number; companyId?: string }>({ username: '', name: '' })
const roleForm = reactive<{ id?: number; roleCode: string; roleName: string; roleDescription?: string }>({ roleCode: '', roleName: '' })
const companyForm = reactive<CompanySavePayload>({ companyName: '' })

const sourceLabelMap: Record<string, string> = { MANUAL: '手工', DINGTALK: '钉钉', WECOM: '企微', FEISHU: '飞书' }

const selectedDepartment = computed(() => findDepartmentById(departments.value, selectedDepartmentId.value))
const visibleTabs = computed(() =>
  [
    can('settings:organization:view') && 'organization',
    can('settings:employees:view') && 'employees',
    can('settings:roles:view') && 'roles',
    can('settings:companies:view') && 'companies'
  ].filter(Boolean)
)
const departmentCount = computed(() => flattenDepartments(departments.value).length)
const companyCount = computed(() => flattenCompanies(companies.value).length)
const companyNameById = computed(() => Object.fromEntries(flattenCompanies(companies.value).map((item) => [item.companyId, item.companyName])))
const companyOptions = computed(() => flattenCompanies(companies.value).map((item) => ({ companyId: item.companyId, label: `${'— '.repeat(item.level)}${item.companyName}` })))
const departmentOptions = computed(() => departments.value)
const departmentParentOptions = computed(() => removeDepartmentNode(departments.value, selectedDepartmentId.value))
const flatCompanies = computed(() => flattenCompanies(companies.value))
const employeeOptions = computed(() =>
  employees.value.map((item) => ({
    userId: item.userId,
    label: `${item.name} (${item.username})${item.status === 0 ? ' [停用]' : ''}`
  }))
)
const departmentConfigEditable = computed(
  () => !!selectedDepartment.value && can('settings:organization:edit') && isManualDepartment(selectedDepartment.value)
)
const selectedDepartmentSyncLocked = computed(
  () => !!selectedDepartment.value && !isManualDepartment(selectedDepartment.value)
)
const filteredEmployees = computed(() =>
  employees.value.filter((item) => {
    const keyword = employeeKeyword.value.trim().toLowerCase()
    const matchesKeyword =
      !keyword ||
      [item.name, item.username, item.phone, item.email].some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchesStatus = employeeStatusFilter.value === undefined || item.status === employeeStatusFilter.value
    return matchesKeyword && matchesStatus
  })
)

watch(activeTab, (value) => {
  router.replace({ query: { ...route.query, tab: value } })
})

watch(selectedRole, (role) => {
  selectedRoleUserIds.value = role?.userIds ? [...role.userIds] : []
  permissionTreeRef.value?.setCheckedKeys(role?.permissionCodes || [])
})

watch(selectedDepartment, (department) => {
  fillDepartmentConfigForm(department)
}, { immediate: true })

function can(code: string) {
  const codes = currentUser.value?.permissionCodes || []
  if (code.endsWith(':view')) {
    return hasAnyPermission(['settings:menu', code], codes)
  }
  return hasPermission(code, codes)
}

function syncTabFromRoute() {
  const queryTab = String(route.query.tab || '')
  activeTab.value = visibleTabs.value.includes(queryTab) ? queryTab : String(visibleTabs.value[0] || 'organization')
}

async function loadBootstrap() {
  loading.value = true
  bootstrapError.value = false
  try {
    const res = await systemSettingsApi.getBootstrap()
    currentUser.value = res.data.currentUser
    departments.value = res.data.departments
    employees.value = res.data.employees
    roles.value = res.data.roles
    permissions.value = res.data.permissions
    companies.value = res.data.companies
    connectors.value = res.data.connectors
    jobs.value = res.data.jobs
    localStorage.setItem('user', JSON.stringify(res.data.currentUser))
    if (selectedDepartmentId.value && !findDepartmentById(departments.value, selectedDepartmentId.value)) {
      selectedDepartmentId.value = undefined
    }
    if (!selectedDepartmentId.value && departments.value[0]) {
      selectedDepartmentId.value = departments.value[0].id
    }
    syncTabFromRoute()
  } catch (error: any) {
    bootstrapError.value = true
    const message = error?.message || '系统设置初始化失败，请联系管理员或查看后端日志'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function handleDepartmentSelect(node: DepartmentTreeNode) {
  selectedDepartmentId.value = node.id
}

function handleRoleSelect(role?: RoleRecord) {
  selectedRole.value = role
}

function openDepartmentDialog() {
  departmentForm.deptName = ''
  departmentForm.parentId = undefined
  departmentForm.companyId = undefined
  departmentForm.leaderUserId = undefined
  departmentDialogVisible.value = true
}

function openEmployeeDialog(item?: EmployeeRecord) {
  employeeForm.userId = item?.userId
  employeeForm.username = item?.username || ''
  employeeForm.name = item?.name || ''
  employeeForm.phone = item?.phone
  employeeForm.email = item?.email
  employeeForm.deptId = item?.deptId
  employeeForm.companyId = item?.companyId
  employeeDialogVisible.value = true
}

function openRoleDialog(item?: RoleRecord) {
  roleForm.id = item?.id
  roleForm.roleCode = item?.roleCode || ''
  roleForm.roleName = item?.roleName || ''
  roleForm.roleDescription = item?.roleDescription
  roleDialogVisible.value = true
}

function openCompanyDialog(item?: CompanyRecord) {
  selectedCompany.value = item
  companyForm.companyName = item?.companyName || ''
  companyForm.invoiceTitle = item?.invoiceTitle
  companyForm.taxNo = item?.taxNo
  companyForm.bankName = item?.bankName
  companyForm.bankAccountName = item?.bankAccountName
  companyForm.bankAccountNo = item?.bankAccountNo
  companyForm.status = item?.status ?? 1
  companyDialogVisible.value = true
}

async function saveDepartment() {
  const payload: DepartmentSavePayload = { ...departmentForm, status: 1, syncEnabled: 1 }
  const res = await systemSettingsApi.createDepartment(payload)
  selectedDepartmentId.value = res.data.id
  departmentDialogVisible.value = false
  ElMessage.success('部门已保存')
  await loadBootstrap()
}

async function saveDepartmentConfig() {
  if (!selectedDepartment.value) return
  const payload: DepartmentSavePayload = {
    ...departmentConfigForm,
    status: selectedDepartment.value.status,
    sortOrder: selectedDepartment.value.sortOrder,
    syncEnabled: selectedDepartment.value.syncEnabled ? 1 : 0
  }
  await systemSettingsApi.updateDepartment(selectedDepartment.value.id, payload)
  ElMessage.success('部门配置已保存')
  await loadBootstrap()
}

async function saveEmployee() {
  const payload: EmployeeSavePayload = { ...employeeForm, status: 1 }
  if (employeeForm.userId) {
    await systemSettingsApi.updateEmployee(employeeForm.userId, payload)
  } else {
    await systemSettingsApi.createEmployee(payload)
  }
  employeeDialogVisible.value = false
  ElMessage.success('员工已保存')
  await loadBootstrap()
}

async function saveRole() {
  const payload: RoleSavePayload = { ...roleForm, status: 1 }
  if (roleForm.id) {
    await systemSettingsApi.updateRole(roleForm.id, payload)
  } else {
    await systemSettingsApi.createRole(payload)
  }
  roleDialogVisible.value = false
  ElMessage.success('角色已保存')
  await loadBootstrap()
}

async function saveCompany() {
  if (selectedCompany.value?.companyId) {
    await systemSettingsApi.updateCompany(selectedCompany.value.companyId, { ...companyForm, status: 1 })
  } else {
    await systemSettingsApi.createCompany({ ...companyForm, status: 1 })
  }
  companyDialogVisible.value = false
  ElMessage.success('公司已保存')
  await loadBootstrap()
}

async function saveConnector(connector: SyncConnectorConfig) {
  await systemSettingsApi.updateSyncConnector({
    platformCode: connector.platformCode,
    enabled: connector.enabled ? 1 : 0,
    autoSyncEnabled: connector.autoSyncEnabled ? 1 : 0,
    syncIntervalMinutes: connector.syncIntervalMinutes,
    appKey: connector.appKey,
    appSecret: connector.appSecret,
    appId: connector.appId,
    corpId: connector.corpId,
    agentId: connector.agentId
  })
  ElMessage.success(`${connector.platformName} 配置已保存`)
  await loadBootstrap()
}

async function runConnectorSync(platformCode: string) {
  await systemSettingsApi.runSync([platformCode])
  ElMessage.success('同步任务已执行')
  await loadBootstrap()
}

async function saveRolePermissions() {
  if (!selectedRole.value) return
  const checkedKeys = permissionTreeRef.value?.getCheckedKeys(false) || []
  await systemSettingsApi.assignRolePermissions(selectedRole.value.id, checkedKeys)
  ElMessage.success('角色权限已更新')
  await loadBootstrap()
}

async function saveRoleUsers() {
  if (!selectedRole.value) return
  for (const employee of employees.value) {
    const shouldHave = selectedRoleUserIds.value.includes(employee.userId)
    const hasRole = employee.roleCodes.includes(selectedRole.value.roleCode)
    if (shouldHave === hasRole) continue
    const roleIds = shouldHave
      ? [...new Set([...roles.value.filter((role) => employee.roleCodes.includes(role.roleCode)).map((role) => role.id), selectedRole.value.id])]
      : roles.value.filter((role) => employee.roleCodes.includes(role.roleCode) && role.id !== selectedRole.value?.id).map((role) => role.id)
    await systemSettingsApi.assignUserRoles(employee.userId, roleIds)
  }
  ElMessage.success('用户角色已更新')
  await loadBootstrap()
}

async function handleDeleteDepartment() {
  if (!selectedDepartment.value) return
  await ElMessageBox.confirm(`确认删除部门“${selectedDepartment.value.deptName}”吗？`, '提示', { type: 'warning' })
  await systemSettingsApi.deleteDepartment(selectedDepartment.value.id)
  ElMessage.success('部门已删除')
  selectedDepartmentId.value = undefined
  await loadBootstrap()
}

async function handleDeleteEmployee() {
  if (!selectedEmployee.value) return
  await ElMessageBox.confirm(`确认删除员工“${selectedEmployee.value.name}”吗？`, '提示', { type: 'warning' })
  await systemSettingsApi.deleteEmployee(selectedEmployee.value.userId)
  ElMessage.success('员工已删除')
  selectedEmployee.value = undefined
  await loadBootstrap()
}

async function handleDeleteRole() {
  if (!selectedRole.value) return
  await ElMessageBox.confirm(`确认删除角色“${selectedRole.value.roleName}”吗？`, '提示', { type: 'warning' })
  await systemSettingsApi.deleteRole(selectedRole.value.id)
  ElMessage.success('角色已删除')
  selectedRole.value = undefined
  await loadBootstrap()
}

async function handleDeleteCompany() {
  if (!selectedCompany.value) return
  await ElMessageBox.confirm(`确认删除公司“${selectedCompany.value.companyName}”吗？`, '提示', { type: 'warning' })
  await systemSettingsApi.deleteCompany(selectedCompany.value.companyId)
  ElMessage.success('公司已删除')
  selectedCompany.value = undefined
  await loadBootstrap()
}

function findDepartmentById(tree: DepartmentTreeNode[], id?: number): DepartmentTreeNode | undefined {
  for (const item of tree) {
    if (item.id === id) return item
    const child = findDepartmentById(item.children || [], id)
    if (child) return child
  }
  return undefined
}

function flattenDepartments(tree: DepartmentTreeNode[]): DepartmentTreeNode[] {
  return tree.flatMap((item) => [item, ...flattenDepartments(item.children || [])])
}

function removeDepartmentNode(tree: DepartmentTreeNode[], id?: number): DepartmentTreeNode[] {
  return tree
    .filter((item) => item.id !== id)
    .map((item) => ({ ...item, children: removeDepartmentNode(item.children || [], id) }))
}

function fillDepartmentConfigForm(item?: DepartmentTreeNode) {
  departmentConfigForm.deptName = item?.deptName || ''
  departmentConfigForm.parentId = item?.parentId
  departmentConfigForm.companyId = item?.companyId
  departmentConfigForm.leaderUserId = item?.leaderUserId
}

function isManualDepartment(item: DepartmentTreeNode) {
  return item.syncSource === 'MANUAL' || !item.syncManaged
}

function flattenCompanies(tree: CompanyRecord[], level = 0): Array<CompanyRecord & { level: number; label: string }> {
  return tree.flatMap((item) => [
    { ...item, level, label: `${'— '.repeat(level)}${item.companyName}` },
    ...flattenCompanies(item.children || [], level + 1)
  ])
}

onMounted(loadBootstrap)
</script>

<style scoped>
:deep(.settings-tabs .el-tabs__header) {
  margin-bottom: 24px;
}
</style>
