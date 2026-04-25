<template>
  <div class="grid gap-6 xl:grid-cols-[1fr,1.2fr]">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="font-semibold text-slate-900">角色列表</div>
          <div class="flex gap-2">
            <el-button v-if="canCreate" type="primary" @click="$emit('create')">
              新增角色
            </el-button>
            <el-button
              v-if="canEdit"
              :disabled="!selectedRole || selectedRoleReadonly"
              @click="$emit('edit', selectedRole)"
            >
              编辑
            </el-button>
            <el-button
              v-if="canDelete"
              :disabled="!selectedRole || selectedRoleProtected"
              type="danger"
              plain
              @click="$emit('delete-selected')"
            >
              删除
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="roles" highlight-current-row @current-change="$emit('select-role', $event)">
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
              v-if="canAssignPermissions"
              :disabled="!selectedRole || rolePermissionReadonly"
              type="primary"
              @click="$emit('save-permissions')"
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
              :ref="permissionTreeRef"
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
              v-if="canAssignUsers"
              :disabled="!selectedRole || roleUserAssignmentReadonly"
              type="primary"
              @click="$emit('save-users')"
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
            :model-value="selectedRoleUserIds"
            multiple
            filterable
            placeholder="选择员工"
            class="w-full"
            :disabled="roleUserAssignmentReadonly"
            @update:model-value="$emit('update:selectedRoleUserIds', $event)"
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
</template>

<script setup lang="ts">
import type { EmployeeRecord, PermissionTreeNode, RoleRecord } from '@/api'

defineProps<{
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  canAssignPermissions: boolean
  canAssignUsers: boolean
  roles: RoleRecord[]
  selectedRole?: RoleRecord
  selectedRoleProtected: boolean
  selectedRoleReadonly: boolean
  rolePermissionReadonly: boolean
  roleUserAssignmentReadonly: boolean
  permissions: PermissionTreeNode[]
  permissionTreeRef: unknown
  selectedRoleUserIds: number[]
  employees: EmployeeRecord[]
  isSuperAdminRole: (role?: Pick<RoleRecord, 'roleCode'>) => boolean
}>()

defineEmits<{
  (e: 'select-role', role?: RoleRecord): void
  (e: 'create'): void
  (e: 'edit', role?: RoleRecord): void
  (e: 'delete-selected'): void
  (e: 'save-permissions'): void
  (e: 'save-users'): void
  (e: 'update:selectedRoleUserIds', value: number[]): void
}>()
</script>
