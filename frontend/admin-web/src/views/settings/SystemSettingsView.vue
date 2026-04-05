<template>
  <div class="space-y-6">
    <section class="rounded-3xl bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-sm text-slate-500">System Settings</p>
          <h1 class="mt-2 text-3xl font-bold text-slate-900">系统设置中心</h1>
          <p class="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
            统一维护组织架构、员工、权限与公司资料，并支持第三方组织同步配置与执行。
          </p>
        </div>
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div class="rounded-2xl bg-slate-50 px-4 py-3">
            <div class="text-xs text-slate-500">部门数</div>
            <div class="mt-2 text-2xl font-semibold text-slate-900">{{ departmentCount }}</div>
          </div>
          <div class="rounded-2xl bg-slate-50 px-4 py-3">
            <div class="text-xs text-slate-500">员工数</div>
            <div class="mt-2 text-2xl font-semibold text-slate-900">{{ employees.length }}</div>
          </div>
          <div class="rounded-2xl bg-slate-50 px-4 py-3">
            <div class="text-xs text-slate-500">角色数</div>
            <div class="mt-2 text-2xl font-semibold text-slate-900">{{ roles.length }}</div>
          </div>
          <div class="rounded-2xl bg-slate-50 px-4 py-3">
            <div class="text-xs text-slate-500">公司数</div>
            <div class="mt-2 text-2xl font-semibold text-slate-900">{{ companyCount }}</div>
          </div>
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
        <el-tab-pane
          v-if="can('settings:organization:view')"
          label="组织架构"
          name="organization"
        >
          <div class="grid gap-6 xl:grid-cols-[1.2fr,1fr]">
            <el-card shadow="never">
              <template #header>
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <div class="text-base font-semibold text-slate-900">部门树</div>
                    <div class="text-xs text-slate-500">手工新增部门不会被自动同步覆盖</div>
                  </div>
                  <div class="flex gap-2">
                    <el-button
                      v-if="can('settings:organization:create')"
                      type="primary"
                      @click="openDepartmentDialog"
                    >
                      新增部门
                    </el-button>
                    <el-button
                      v-if="can('settings:organization:delete')"
                      :disabled="!selectedDepartment"
                      type="danger"
                      plain
                      @click="handleDeleteDepartment"
                    >
                      删除
                    </el-button>
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
                <template #header>
                  <div class="font-semibold text-slate-900">部门配置</div>
                </template>
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
                      <el-input
                        v-model="departmentConfigForm.deptName"
                        :disabled="!departmentConfigEditable"
                      />
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
                      <el-select
                        v-model="departmentConfigForm.companyId"
                        clearable
                        class="w-full"
                        :disabled="!departmentConfigEditable"
                      >
                        <el-option
                          v-for="item in companyOptions"
                          :key="item.companyId"
                          :label="item.label"
                          :value="item.companyId"
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="部门负责人">
                      <el-select
                        v-model="departmentConfigForm.leaderUserId"
                        clearable
                        filterable
                        class="w-full"
                        :disabled="!departmentConfigEditable"
                      >
                        <el-option
                          v-for="item in employeeOptions"
                          :key="item.userId"
                          :label="item.label"
                          :value="item.userId"
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="部门编码">
                      <el-input :model-value="selectedDepartment.deptCode" disabled />
                    </el-form-item>
                    <el-form-item label="同步来源">
                      <el-input
                        :model-value="
                          sourceLabelMap[selectedDepartment.syncSource] || selectedDepartment.syncSource
                        "
                        disabled
                      />
                    </el-form-item>
                  </el-form>
                  <div v-if="departmentConfigEditable" class="flex justify-end">
                    <el-button type="primary" @click="saveDepartmentConfig">保存配置</el-button>
                  </div>
                </div>
              </el-card>

              <el-card shadow="never">
                <template #header>
                  <div class="font-semibold text-slate-900">同步连接配置</div>
                </template>
                <div class="space-y-4">
                  <div
                    v-for="connector in connectors"
                    :key="connector.platformCode"
                    class="rounded-2xl border border-slate-200 p-4"
                  >
                    <div class="mb-3 flex items-center justify-between gap-4">
                      <div>
                        <div class="font-semibold text-slate-900">{{ connector.platformName }}</div>
                        <div class="text-xs text-slate-500">
                          {{ connector.lastSyncMessage || '尚未执行同步' }}
                        </div>
                      </div>
                      <div class="flex items-center gap-3">
                        <el-switch v-model="connector.enabled" active-text="启用" />
                        <el-switch v-model="connector.autoSyncEnabled" active-text="自动同步" />
                      </div>
                    </div>

                    <div v-if="isWecomConnector(connector)" class="space-y-3">
                      <div class="grid gap-3 md:grid-cols-2">
                        <el-input v-model="connector.corpId" placeholder="企业 ID" />
                        <el-input
                          v-model="connector.appSecret"
                          placeholder="通讯录 Secret"
                          show-password
                        />
                        <el-input v-model="connector.agentId" placeholder="AgentId（可选）" />
                        <el-input-number
                          v-model="connector.syncIntervalMinutes"
                          :min="5"
                          :max="1440"
                          class="w-full"
                        />
                      </div>
                      <div class="text-xs text-slate-500">
                        AppKey / AppId 对企微同步不需要，本次同步仅使用企业 ID 与通讯录 Secret。
                      </div>
                    </div>

                    <div v-else class="grid gap-3 md:grid-cols-2">
                      <el-input v-model="connector.appKey" placeholder="App Key / Client ID" />
                      <el-input
                        v-model="connector.appSecret"
                        placeholder="App Secret"
                        show-password
                      />
                      <el-input v-model="connector.appId" placeholder="App ID" />
                      <el-input v-model="connector.corpId" placeholder="Corp / Tenant ID" />
                      <el-input v-model="connector.agentId" placeholder="Agent ID" />
                      <el-input-number
                        v-model="connector.syncIntervalMinutes"
                        :min="5"
                        :max="1440"
                        class="w-full"
                      />
                    </div>

                    <div class="mt-4 flex gap-2">
                      <el-button
                        v-if="can('settings:organization:sync_config')"
                        type="primary"
                        @click="saveConnector(connector)"
                      >
                        保存配置
                      </el-button>
                      <el-button
                        v-if="can('settings:organization:run_sync')"
                        @click="runConnectorSync(connector.platformCode)"
                      >
                        立即同步
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-card>

              <el-card shadow="never">
                <template #header>
                  <div class="font-semibold text-slate-900">同步日志</div>
                </template>
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
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane
          v-if="can('settings:employees:view')"
          label="员工管理"
          name="employees"
        >
          <el-card shadow="never">
            <template #header>
              <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div class="flex flex-wrap gap-3">
                  <el-input
                    v-model="employeeKeyword"
                    placeholder="搜索姓名 / 用户名 / 手机号 / 邮箱"
                    clearable
                    class="w-72"
                  />
                  <el-select
                    v-model="employeeStatusFilter"
                    clearable
                    placeholder="状态"
                    class="w-32"
                  >
                    <el-option label="启用" :value="1" />
                    <el-option label="停用" :value="0" />
                  </el-select>
                </div>
                <div class="flex gap-2">
                  <el-button
                    v-if="can('settings:employees:create')"
                    type="primary"
                    @click="openEmployeeDialog"
                  >
                    新增员工
                  </el-button>
                  <el-button
                    v-if="can('settings:employees:edit')"
                    :disabled="!selectedEmployee"
                    @click="openEmployeeDialog(selectedEmployee)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="can('settings:employees:delete')"
                    :disabled="!selectedEmployee"
                    type="danger"
                    plain
                    @click="handleDeleteEmployee"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </template>
            <el-table
              :data="filteredEmployees"
              highlight-current-row
              @current-change="selectedEmployee = $event"
              @row-dblclick="handleEmployeeRowDblClick"
            >
              <el-table-column prop="name" label="姓名" min-width="140" />
              <el-table-column prop="username" label="用户名" min-width="140" />
              <el-table-column prop="phone" label="手机号" width="140" />
              <el-table-column prop="deptName" label="部门" min-width="120" />
              <el-table-column prop="companyName" label="公司" min-width="150" />
              <el-table-column label="来源" width="100">
                <template #default="{ row }">
                  {{ sourceLabelMap[row.sourceType] || row.sourceType }}
                </template>
              </el-table-column>
              <el-table-column label="角色" min-width="220">
                <template #default="{ row }">
                  <el-tag
                    v-for="roleName in resolveEmployeeRoleNames(row.roleCodes)"
                    :key="`${row.userId}-${roleName}`"
                    size="small"
                    class="mr-1 mb-1"
                  >
                    {{ roleName }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <el-tab-pane
          v-if="can('settings:roles:view')"
          label="权限管理"
          name="roles"
        >
          <div class="grid gap-6 xl:grid-cols-[1fr,1.2fr]">
            <el-card shadow="never">
              <template #header>
                <div class="flex items-center justify-between">
                  <div class="font-semibold text-slate-900">角色列表</div>
                  <div class="flex gap-2">
                    <el-button
                      v-if="can('settings:roles:create')"
                      type="primary"
                      @click="openRoleDialog"
                    >
                      新增角色
                    </el-button>
                    <el-button
                      v-if="can('settings:roles:edit')"
                      :disabled="!selectedRole || selectedRoleReadonly"
                      @click="openRoleDialog(selectedRole)"
                    >
                      编辑
                    </el-button>
                    <el-button
                      v-if="can('settings:roles:delete')"
                      :disabled="!selectedRole || selectedRoleProtected"
                      type="danger"
                      plain
                      @click="handleDeleteRole"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </template>
              <el-table :data="roles" highlight-current-row @current-change="handleRoleSelect">
                <el-table-column prop="roleName" label="角色名称" min-width="180">
                  <template #default="{ row }">
                    <div class="flex items-center gap-2">
                      <span>{{ row.roleName }}</span>
                      <el-tag v-if="isSuperAdminRole(row)" size="small" type="danger">
                        系统保留
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="roleCode" label="编码" min-width="150" />
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <div class="space-y-6">
              <el-card shadow="never">
                <template #header>
                  <div class="flex items-center justify-between">
                    <div class="font-semibold text-slate-900">菜单与按钮权限</div>
                    <el-button
                      v-if="can('settings:roles:assign_permissions')"
                      :disabled="!selectedRole || rolePermissionReadonly"
                      type="primary"
                      @click="saveRolePermissions"
                    >
                      保存权限
                    </el-button>
                  </div>
                </template>
                <el-empty v-if="!selectedRole" description="请选择左侧角色" />
                <template v-else>
                  <el-alert
                    v-if="rolePermissionReadonly"
                    type="warning"
                    :closable="false"
                    title="超级管理员权限仅可由超级管理员修改"
                    class="mb-4"
                  />
                  <div :class="{ 'pointer-events-none opacity-60': rolePermissionReadonly }">
                    <el-tree
                      ref="permissionTreeRef"
                      :data="permissions"
                      node-key="permissionCode"
                      show-checkbox
                      default-expand-all
                      :props="{ label: 'permissionName', children: 'children' }"
                    />
                  </div>
                </template>
              </el-card>

              <el-card shadow="never">
                <template #header>
                  <div class="flex items-center justify-between">
                    <div class="font-semibold text-slate-900">用户角色分配</div>
                    <el-button
                      v-if="can('settings:roles:assign_users')"
                      :disabled="!selectedRole || roleUserAssignmentReadonly"
                      type="primary"
                      @click="saveRoleUsers"
                    >
                      保存用户
                    </el-button>
                  </div>
                </template>
                <el-empty v-if="!selectedRole" description="请选择左侧角色" />
                <template v-else>
                  <el-alert
                    v-if="roleUserAssignmentReadonly"
                    type="warning"
                    :closable="false"
                    title="超级管理员只能通过数据库维护"
                    class="mb-4"
                  />
                  <el-select
                    v-model="selectedRoleUserIds"
                    multiple
                    filterable
                    placeholder="选择员工"
                    class="w-full"
                    :disabled="roleUserAssignmentReadonly"
                  >
                    <el-option
                      v-for="employee in employees"
                      :key="employee.userId"
                      :label="`${employee.name} (${employee.username})`"
                      :value="employee.userId"
                    />
                  </el-select>
                </template>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane
          v-if="can('settings:companies:view')"
          label="公司管理"
          name="companies"
        >
          <el-card shadow="never">
            <template #header>
              <div class="flex items-center justify-between">
                <div class="font-semibold text-slate-900">公司主体与抬头</div>
                <div class="flex gap-2">
                  <el-button
                    v-if="can('settings:companies:create')"
                    type="primary"
                    @click="openCompanyDialog"
                  >
                    新增公司
                  </el-button>
                  <el-button
                    v-if="can('settings:companies:edit')"
                    :disabled="!selectedCompany"
                    @click="openCompanyDialog(selectedCompany)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="can('settings:companies:delete')"
                    :disabled="!selectedCompany"
                    type="danger"
                    plain
                    @click="handleDeleteCompany"
                  >
                    删除
                  </el-button>
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

    <el-dialog
      v-model="departmentDialogVisible"
      title="新增部门"
      width="520px"
      :close-on-press-escape="false"
    >
      <el-form label-width="92px">
        <div class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
          部门编码将在保存后自动生成
        </div>
        <el-form-item label="部门名称">
          <el-input v-model="departmentForm.deptName" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="departmentForm.parentId"
            :data="departmentOptions"
            node-key="id"
            check-strictly
            clearable
            :props="{ label: 'deptName', children: 'children', value: 'id' }"
          />
        </el-form-item>
        <el-form-item label="所属公司">
          <el-select v-model="departmentForm.companyId" clearable class="w-full">
            <el-option
              v-for="item in companyOptions"
              :key="item.companyId"
              :label="item.label"
              :value="item.companyId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="部门负责人">
          <el-select v-model="departmentForm.leaderUserId" clearable filterable class="w-full">
            <el-option
              v-for="item in employeeOptions"
              :key="item.userId"
              :label="item.label"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="departmentDialogVisible = false">取消</el-button>
        <el-button type="primary" plain @click="saveDepartment(true)">保存并退出</el-button>
        <el-button type="primary" @click="saveDepartment(false)">保存并新增</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="employeeDialogVisible"
      :title="employeeForm.userId ? '编辑员工' : '新增员工'"
      width="560px"
      :close-on-press-escape="false"
    >
      <el-form label-width="92px">
        <el-form-item label="用户名">
          <el-input v-model="employeeForm.username" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="employeeForm.name" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="employeeForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="employeeForm.email" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-tree-select
            v-model="employeeForm.deptId"
            :data="departmentOptions"
            node-key="id"
            check-strictly
            clearable
            :props="{ label: 'deptName', children: 'children', value: 'id' }"
          />
        </el-form-item>
        <el-form-item label="所属公司">
          <el-select v-model="employeeForm.companyId" clearable class="w-full">
            <el-option
              v-for="item in companyOptions"
              :key="item.companyId"
              :label="item.label"
              :value="item.companyId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色设置">
          <el-select
            v-model="employeeForm.roleIds"
            multiple
            filterable
            clearable
            class="w-full"
            placeholder="选择角色"
            :disabled="!canAssignEmployeeRoles"
          >
            <el-option
              v-for="item in employeeRoleOptions"
              :key="item.id"
              :label="`${item.roleName} (${item.roleCode})`"
              :value="item.id"
              :disabled="item.status !== 1 || isSuperAdminRole(item)"
            />
          </el-select>
        </el-form-item>
        <div v-if="employeeHasSuperAdminRole" class="text-xs text-amber-600">
          超级管理员角色仅展示当前绑定状态，前端无法修改。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="employeeDialogVisible = false">取消</el-button>
        <template v-if="employeeForm.userId">
          <el-button type="primary" @click="saveEmployee(true)">保存</el-button>
        </template>
        <template v-else>
          <el-button type="primary" plain @click="saveEmployee(true)">保存并退出</el-button>
          <el-button type="primary" @click="saveEmployee(false)">保存并新增</el-button>
        </template>
      </template>
    </el-dialog>

    <el-dialog
      v-model="roleDialogVisible"
      :title="roleForm.id ? '编辑角色' : '新增角色'"
      width="520px"
      :close-on-press-escape="false"
    >
      <el-form label-width="92px">
        <div
          v-if="!roleForm.id"
          class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500"
        >
          普通角色编码将在保存后自动生成，格式为 RL + 6 位数字。
        </div>
        <el-form-item v-else label="角色编码">
          <el-input :model-value="roleForm.roleCode" disabled />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.roleName" />
        </el-form-item>
        <el-form-item label="角色说明">
          <el-input v-model="roleForm.roleDescription" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <template v-if="roleForm.id">
          <el-button type="primary" @click="saveRole(true)">保存</el-button>
        </template>
        <template v-else>
          <el-button type="primary" plain @click="saveRole(true)">保存并退出</el-button>
          <el-button type="primary" @click="saveRole(false)">保存并新增</el-button>
        </template>
      </template>
    </el-dialog>

    <el-dialog
      v-model="companyDialogVisible"
      :title="selectedCompany ? '编辑公司' : '新增公司'"
      width="620px"
      :close-on-press-escape="false"
    >
      <el-form label-width="100px">
        <div class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
          <template v-if="selectedCompany">
            <div>主体编码：{{ selectedCompany.companyId }}</div>
            <div class="mt-1">公司编号：{{ selectedCompany.companyCode }}</div>
          </template>
          <template v-else>主体编码和公司编号将在保存后自动生成。</template>
        </div>
        <el-form-item label="公司名称">
          <el-input v-model="companyForm.companyName" />
        </el-form-item>
        <el-form-item label="公司抬头">
          <el-input v-model="companyForm.invoiceTitle" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="companyForm.taxNo" />
        </el-form-item>
        <el-form-item label="开户行">
          <el-input v-model="companyForm.bankName" />
        </el-form-item>
        <el-form-item label="账户名">
          <el-input v-model="companyForm.bankAccountName" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="companyForm.bankAccountNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="companyDialogVisible = false">取消</el-button>
        <template v-if="selectedCompany?.companyId">
          <el-button type="primary" @click="saveCompany(true)">保存</el-button>
        </template>
        <template v-else>
          <el-button type="primary" plain @click="saveCompany(true)">保存并退出</el-button>
          <el-button type="primary" @click="saveCompany(false)">保存并新增</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hasAnyPermission, hasPermission } from '@/utils/permissions'
import {
  systemSettingsApi,
  type CompanyRecord,
  type CompanySavePayload,
  type DepartmentTreeNode,
  type DepartmentSavePayload,
  type EmployeeEditorFormState,
  type EmployeeRecord,
  type EmployeeSavePayload,
  type PermissionTreeNode,
  type RoleRecord,
  type RoleSavePayload,
  type SyncConnectorConfig,
  type SyncJobRecord,
  type SystemSettingsBootstrapData
} from '@/api'

type FlatCompanyRecord = CompanyRecord & { level: number; label: string }

const route = useRoute()
const router = useRouter()

const SUPER_ADMIN_ROLE_CODE = 'SUPER_ADMIN'

const loading = ref(false)
const bootstrapError = ref(false)
const activeTab = ref('organization')
const departments = ref<DepartmentTreeNode[]>([])
const employees = ref<EmployeeRecord[]>([])
const roles = ref<RoleRecord[]>([])
const permissions = ref<PermissionTreeNode[]>([])
const companies = ref<CompanyRecord[]>([])
const connectors = ref<SyncConnectorConfig[]>([])
const jobs = ref<SyncJobRecord[]>([])
const currentUser = ref<SystemSettingsBootstrapData['currentUser'] | null>(null)

const selectedDepartmentId = ref<number>()
const selectedEmployee = ref<EmployeeRecord>()
const selectedRole = ref<RoleRecord>()
const selectedCompany = ref<CompanyRecord>()
const selectedRoleUserIds = ref<number[]>([])
const permissionTreeRef = ref()

const employeeKeyword = ref('')
const employeeStatusFilter = ref<number>()

const departmentDialogVisible = ref(false)
const employeeDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const companyDialogVisible = ref(false)

const departmentForm = reactive<{
  deptName: string
  parentId?: number
  companyId?: string
  leaderUserId?: number
}>({
  deptName: ''
})

const departmentConfigForm = reactive<{
  deptName: string
  parentId?: number
  companyId?: string
  leaderUserId?: number
}>({
  deptName: ''
})

const employeeForm = reactive<EmployeeEditorFormState>({
  username: '',
  name: '',
  roleIds: []
})

const roleForm = reactive<{
  id?: number
  roleCode?: string
  roleName: string
  roleDescription?: string
}>({
  roleCode: '',
  roleName: ''
})

const companyForm = reactive<CompanySavePayload>({
  companyName: ''
})

const sourceLabelMap: Record<string, string> = {
  MANUAL: '手工',
  DINGTALK: '钉钉',
  WECOM: '企微',
  FEISHU: '飞书'
}

const selectedDepartment = computed(() =>
  findDepartmentById(departments.value, selectedDepartmentId.value)
)

const visibleTabs = computed(() =>
  [
    can('settings:organization:view') ? 'organization' : null,
    can('settings:employees:view') ? 'employees' : null,
    can('settings:roles:view') ? 'roles' : null,
    can('settings:companies:view') ? 'companies' : null
  ].filter((item): item is string => !!item)
)

const departmentCount = computed(() => flattenDepartments(departments.value).length)
const flatCompanies = computed<FlatCompanyRecord[]>(() => flattenCompanies(companies.value))
const companyCount = computed(() => flatCompanies.value.length)
const roleNameByCode = computed<Record<string, string>>(() =>
  Object.fromEntries(roles.value.map((item) => [item.roleCode, item.roleName]))
)

const companyOptions = computed(() =>
  flatCompanies.value.map((item) => ({
    companyId: item.companyId,
    label: item.label
  }))
)

const departmentOptions = computed(() => departments.value)
const departmentParentOptions = computed(() =>
  removeDepartmentNode(departments.value, selectedDepartmentId.value)
)

const employeeOptions = computed(() =>
  employees.value.map((item) => ({
    userId: item.userId,
    label: `${item.name} (${item.username})${item.status === 0 ? ' [停用]' : ''}`
  }))
)

const employeeRoleOptions = computed(() => roles.value)
const canAssignEmployeeRoles = computed(() => can('settings:roles:assign_users'))
const currentUserIsSuperAdmin = computed(() =>
  (currentUser.value?.roles || []).includes(SUPER_ADMIN_ROLE_CODE)
)
const selectedRoleProtected = computed(
  () => !!selectedRole.value && isSuperAdminRole(selectedRole.value)
)
const selectedRoleReadonly = computed(
  () => !!selectedRole.value && isSuperAdminRole(selectedRole.value)
)
const rolePermissionReadonly = computed(
  () =>
    !!selectedRole.value &&
    isSuperAdminRole(selectedRole.value) &&
    !currentUserIsSuperAdmin.value
)
const roleUserAssignmentReadonly = computed(
  () => !!selectedRole.value && isSuperAdminRole(selectedRole.value)
)
const employeeHasSuperAdminRole = computed(() =>
  employeeForm.roleIds.some((roleId) =>
    roles.value.some((role) => role.id === roleId && isSuperAdminRole(role))
  )
)
const departmentConfigEditable = computed(
  () =>
    !!selectedDepartment.value &&
    can('settings:organization:edit') &&
    isManualDepartment(selectedDepartment.value)
)
const selectedDepartmentSyncLocked = computed(
  () => !!selectedDepartment.value && !isManualDepartment(selectedDepartment.value)
)

const filteredEmployees = computed(() =>
  employees.value.filter((item) => {
    const keyword = employeeKeyword.value.trim().toLowerCase()
    const matchesKeyword =
      !keyword ||
      [item.name, item.username, item.phone, item.email].some((value) =>
        String(value || '').toLowerCase().includes(keyword)
      )
    const matchesStatus =
      employeeStatusFilter.value === undefined || item.status === employeeStatusFilter.value
    return matchesKeyword && matchesStatus
  })
)

watch(activeTab, (value) => {
  router.replace({ query: { ...route.query, tab: value } })
})

watch(
  () => route.query.tab,
  () => {
    syncTabFromRoute()
  }
)

watch(
  () => [selectedRole.value?.id, permissions.value.length],
  async () => {
    selectedRoleUserIds.value = selectedRole.value?.userIds ? [...selectedRole.value.userIds] : []
    await nextTick()
    permissionTreeRef.value?.setCheckedKeys(selectedRole.value?.permissionCodes || [])
  }
)

watch(
  selectedDepartment,
  (department) => {
    fillDepartmentConfigForm(department)
  },
  { immediate: true }
)

function can(code: string) {
  const codes = currentUser.value?.permissionCodes || []
  if (code.endsWith(':view')) {
    return hasAnyPermission(['settings:menu', code], codes)
  }
  return hasPermission(code, codes)
}

function syncTabFromRoute() {
  const queryTab = String(route.query.tab || '')
  activeTab.value = visibleTabs.value.includes(queryTab)
    ? queryTab
    : String(visibleTabs.value[0] || 'organization')
}

async function loadBootstrap() {
  loading.value = true
  bootstrapError.value = false
  try {
    const res = await systemSettingsApi.getBootstrap()
    const data = res.data
    currentUser.value = data.currentUser
    departments.value = data.departments
    employees.value = data.employees
    roles.value = data.roles
    permissions.value = data.permissions
    companies.value = data.companies
    connectors.value = data.connectors
    jobs.value = data.jobs
    localStorage.setItem('user', JSON.stringify(data.currentUser))

    selectedEmployee.value = selectedEmployee.value
      ? data.employees.find((item) => item.userId === selectedEmployee.value?.userId)
      : undefined
    selectedRole.value = selectedRole.value
      ? data.roles.find((item) => item.id === selectedRole.value?.id)
      : undefined
    selectedCompany.value = selectedCompany.value
      ? flattenCompanies(data.companies).find((item) => item.companyId === selectedCompany.value?.companyId)
      : undefined

    if (
      selectedDepartmentId.value &&
      !findDepartmentById(data.departments, selectedDepartmentId.value)
    ) {
      selectedDepartmentId.value = undefined
    }
    if (!selectedDepartmentId.value && data.departments[0]) {
      selectedDepartmentId.value = data.departments[0].id
    }

    syncTabFromRoute()
  } catch (error: any) {
    bootstrapError.value = true
    ElMessage.error(error?.message || '系统设置初始化失败，请联系管理员或查看后端日志')
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

function handleEmployeeRowDblClick(row: EmployeeRecord) {
  selectedEmployee.value = row
  if (!can('settings:employees:edit')) {
    return
  }
  openEmployeeDialog(row)
}

function openDepartmentDialog() {
  resetDepartmentForm()
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
  employeeForm.position = item?.position
  employeeForm.laborRelationBelong = item?.laborRelationBelong
  employeeForm.roleIds = resolveEmployeeRoleIds(item)
  employeeDialogVisible.value = true
}

function openRoleDialog(item?: RoleRecord) {
  if (item && isSuperAdminRole(item)) {
    ElMessage.warning('超级管理员为系统保留角色，不能通过前端编辑')
    return
  }
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

async function saveDepartment(closeAfterSave: boolean) {
  const payload: DepartmentSavePayload = {
    ...departmentForm,
    status: 1,
    syncEnabled: 1
  }
  const res = await systemSettingsApi.createDepartment(payload)
  if (closeAfterSave) {
    selectedDepartmentId.value = res.data.id
    departmentDialogVisible.value = false
  } else {
    resetDepartmentForm()
  }
  ElMessage.success('部门已保存')
  await loadBootstrap()
}

async function saveDepartmentConfig() {
  if (!selectedDepartment.value) {
    return
  }
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

async function saveEmployee(closeAfterSave: boolean) {
  const { userId, roleIds, ...rest } = employeeForm
  const payload: EmployeeSavePayload = { ...rest, status: 1 }
  const employeeRes = userId
    ? await systemSettingsApi.updateEmployee(userId, payload)
    : await systemSettingsApi.createEmployee(payload)
  const savedUserId = employeeRes.data.userId

  if (closeAfterSave || !!userId) {
    employeeDialogVisible.value = false
  }

  if (!canAssignEmployeeRoles.value) {
    if (!userId && !closeAfterSave) {
      resetEmployeeForm()
    }
    ElMessage.success('员工信息已保存')
    await loadBootstrap()
    return
  }

  try {
    await systemSettingsApi.assignUserRoles(savedUserId, sanitizeEditableRoleIds(roleIds))
    if (!userId && !closeAfterSave) {
      resetEmployeeForm()
    }
    ElMessage.success('员工与角色已保存')
  } catch (error: any) {
    ElMessage.warning(error?.message || '员工已保存，但角色保存失败')
  }

  await loadBootstrap()
}

async function saveRole(closeAfterSave: boolean) {
  const payload: RoleSavePayload = {
    roleCode: roleForm.roleCode || undefined,
    roleName: roleForm.roleName,
    roleDescription: roleForm.roleDescription,
    status: 1
  }

  if (roleForm.id) {
    await systemSettingsApi.updateRole(roleForm.id, payload)
  } else {
    await systemSettingsApi.createRole(payload)
  }

  if (closeAfterSave || !!roleForm.id) {
    roleDialogVisible.value = false
  } else {
    resetRoleForm()
  }

  ElMessage.success('角色已保存')
  await loadBootstrap()
}

async function saveCompany(closeAfterSave: boolean) {
  const editingCompany = !!selectedCompany.value?.companyId
  if (editingCompany) {
    await systemSettingsApi.updateCompany(selectedCompany.value!.companyId, {
      ...companyForm,
      status: 1
    })
  } else {
    await systemSettingsApi.createCompany({ ...companyForm, status: 1 })
  }

  if (closeAfterSave || editingCompany) {
    companyDialogVisible.value = false
  } else {
    resetCompanyForm()
  }

  ElMessage.success('公司已保存')
  await loadBootstrap()
}

async function saveConnector(connector: SyncConnectorConfig) {
  const isWecom = isWecomConnector(connector)
  await systemSettingsApi.updateSyncConnector({
    platformCode: connector.platformCode,
    enabled: connector.enabled ? 1 : 0,
    autoSyncEnabled: connector.autoSyncEnabled ? 1 : 0,
    syncIntervalMinutes: connector.syncIntervalMinutes,
    appKey: isWecom ? undefined : connector.appKey,
    appSecret: connector.appSecret,
    appId: isWecom ? undefined : connector.appId,
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
  if (!selectedRole.value) {
    return
  }
  if (rolePermissionReadonly.value) {
    ElMessage.warning('超级管理员权限仅可由超级管理员修改')
    return
  }
  const checkedKeys = permissionTreeRef.value?.getCheckedKeys(false) || []
  await systemSettingsApi.assignRolePermissions(selectedRole.value.id, checkedKeys)
  ElMessage.success('角色权限已更新')
  await loadBootstrap()
}

async function saveRoleUsers() {
  if (!selectedRole.value) {
    return
  }
  if (roleUserAssignmentReadonly.value) {
    ElMessage.warning('超级管理员只能通过数据库维护')
    return
  }

  for (const employee of employees.value) {
    const shouldHave = selectedRoleUserIds.value.includes(employee.userId)
    const hasRole = employee.roleCodes.includes(selectedRole.value.roleCode)
    if (shouldHave === hasRole) {
      continue
    }
    const roleIds = shouldHave
      ? [
          ...new Set([
            ...roles.value
              .filter((role) => employee.roleCodes.includes(role.roleCode))
              .map((role) => role.id),
            selectedRole.value.id
          ])
        ]
      : roles.value
          .filter(
            (role) =>
              employee.roleCodes.includes(role.roleCode) && role.id !== selectedRole.value?.id
          )
          .map((role) => role.id)
    await systemSettingsApi.assignUserRoles(employee.userId, sanitizeEditableRoleIds(roleIds))
  }

  ElMessage.success('用户角色已更新')
  await loadBootstrap()
}

async function handleDeleteDepartment() {
  if (!selectedDepartment.value) {
    return
  }
  await ElMessageBox.confirm(`确认删除部门“${selectedDepartment.value.deptName}”吗？`, '提示', {
    type: 'warning'
  })
  await systemSettingsApi.deleteDepartment(selectedDepartment.value.id)
  ElMessage.success('部门已删除')
  selectedDepartmentId.value = undefined
  await loadBootstrap()
}

async function handleDeleteEmployee() {
  if (!selectedEmployee.value) {
    return
  }
  await ElMessageBox.confirm(`确认删除员工“${selectedEmployee.value.name}”吗？`, '提示', {
    type: 'warning'
  })
  await systemSettingsApi.deleteEmployee(selectedEmployee.value.userId)
  ElMessage.success('员工已删除')
  selectedEmployee.value = undefined
  await loadBootstrap()
}

async function handleDeleteRole() {
  if (!selectedRole.value) {
    return
  }
  if (selectedRoleProtected.value) {
    ElMessage.warning('超级管理员为系统保留角色，不能删除')
    return
  }
  await ElMessageBox.confirm(`确认删除角色“${selectedRole.value.roleName}”吗？`, '提示', {
    type: 'warning'
  })
  await systemSettingsApi.deleteRole(selectedRole.value.id)
  ElMessage.success('角色已删除')
  selectedRole.value = undefined
  await loadBootstrap()
}

async function handleDeleteCompany() {
  if (!selectedCompany.value) {
    return
  }
  await ElMessageBox.confirm(`确认删除公司“${selectedCompany.value.companyName}”吗？`, '提示', {
    type: 'warning'
  })
  await systemSettingsApi.deleteCompany(selectedCompany.value.companyId)
  ElMessage.success('公司已删除')
  selectedCompany.value = undefined
  await loadBootstrap()
}

function findDepartmentById(
  tree: DepartmentTreeNode[],
  id?: number
): DepartmentTreeNode | undefined {
  for (const item of tree) {
    if (item.id === id) {
      return item
    }
    const child = findDepartmentById(item.children || [], id)
    if (child) {
      return child
    }
  }
  return undefined
}

function flattenDepartments(tree: DepartmentTreeNode[]): DepartmentTreeNode[] {
  return tree.flatMap((item) => [item, ...flattenDepartments(item.children || [])])
}

function removeDepartmentNode(tree: DepartmentTreeNode[], id?: number): DepartmentTreeNode[] {
  return tree
    .filter((item) => item.id !== id)
    .map((item) => ({
      ...item,
      children: removeDepartmentNode(item.children || [], id)
    }))
}

function fillDepartmentConfigForm(item?: DepartmentTreeNode) {
  departmentConfigForm.deptName = item?.deptName || ''
  departmentConfigForm.parentId = item?.parentId
  departmentConfigForm.companyId = item?.companyId
  departmentConfigForm.leaderUserId = item?.leaderUserId
}

function resetDepartmentForm() {
  departmentForm.deptName = ''
  departmentForm.parentId = undefined
  departmentForm.companyId = undefined
  departmentForm.leaderUserId = undefined
}

function resetEmployeeForm() {
  employeeForm.userId = undefined
  employeeForm.username = ''
  employeeForm.name = ''
  employeeForm.phone = undefined
  employeeForm.email = undefined
  employeeForm.deptId = undefined
  employeeForm.companyId = undefined
  employeeForm.position = undefined
  employeeForm.laborRelationBelong = undefined
  employeeForm.roleIds = []
}

function resetRoleForm() {
  roleForm.id = undefined
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.roleDescription = undefined
}

function resetCompanyForm() {
  selectedCompany.value = undefined
  companyForm.companyName = ''
  companyForm.invoiceTitle = undefined
  companyForm.taxNo = undefined
  companyForm.bankName = undefined
  companyForm.bankAccountName = undefined
  companyForm.bankAccountNo = undefined
  companyForm.status = 1
}

function resolveEmployeeRoleIds(item?: EmployeeRecord) {
  if (!item?.roleCodes?.length) {
    return []
  }
  return roles.value
    .filter((role) => item.roleCodes.includes(role.roleCode))
    .map((role) => role.id)
}

function resolveEmployeeRoleNames(roleCodes: string[] = []) {
  return roleCodes.map((roleCode) => roleNameByCode.value[roleCode] || roleCode)
}

function sanitizeEditableRoleIds(roleIds: number[]) {
  return roleIds.filter((roleId) => {
    const role = roles.value.find((item) => item.id === roleId)
    return role ? !isSuperAdminRole(role) : true
  })
}

function isSuperAdminRole(role?: Pick<RoleRecord, 'roleCode'>) {
  return role?.roleCode === SUPER_ADMIN_ROLE_CODE
}

function isManualDepartment(item: DepartmentTreeNode) {
  return item.syncSource === 'MANUAL' || !item.syncManaged
}

function flattenCompanies(tree: CompanyRecord[], level = 0): FlatCompanyRecord[] {
  return tree.flatMap((item) => [
    {
      ...item,
      level,
      label: `${'-- '.repeat(level)}${item.companyName}`
    },
    ...flattenCompanies(item.children || [], level + 1)
  ])
}

function isWecomConnector(connector: SyncConnectorConfig) {
  return connector.platformCode === 'WECOM'
}

onMounted(loadBootstrap)
</script>

<style scoped>
:deep(.settings-tabs .el-tabs__header) {
  margin-bottom: 24px;
}
</style>
