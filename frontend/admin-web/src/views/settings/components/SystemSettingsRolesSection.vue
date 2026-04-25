<template>
  <div class="space-y-5">
    <SystemSettingsRolesTab
      :can-create="permissions.canCreate"
      :can-edit="permissions.canEdit"
      :can-delete="permissions.canDelete"
      :can-assign-permissions="permissions.canAssignPermissions"
      :can-assign-users="permissions.canAssignUsers"
      :roles="state.roles"
      :selected-role="state.selectedRole"
      :selected-role-protected="state.selectedRoleProtected"
      :selected-role-readonly="state.selectedRoleReadonly"
      :role-permission-readonly="state.rolePermissionReadonly"
      :role-user-assignment-readonly="state.roleUserAssignmentReadonly"
      :permissions="state.permissions"
      :permission-tree-ref="state.permissionTreeRef"
      :selected-role-user-ids="state.selectedRoleUserIds"
      :employees="state.employees"
      :is-super-admin-role="state.isSuperAdminRole"
      @select-role="actions.handleRoleSelect($event)"
      @create="actions.openRoleDialog()"
      @edit="actions.openRoleDialog($event)"
      @delete-selected="actions.handleDeleteRole()"
      @save-permissions="actions.saveRolePermissions()"
      @save-users="actions.saveRoleUsers()"
      @update:selected-role-user-ids="$emit('update:selectedRoleUserIds', $event)"
    />

    <el-dialog
      :model-value="state.roleDialogVisible"
      :title="state.roleForm.id ? '编辑角色' : '新增角色'"
      width="520px"
      :close-on-press-escape="false"
      @update:model-value="$emit('update:roleDialogVisible', $event)"
    >
      <el-form label-width="92px">
        <div
          v-if="!state.roleForm.id"
          class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500"
        >
          普通角色编码将在保存后自动生成，格式为 RL + 6 位数字。
        </div>
        <el-form-item v-else label="角色编码">
          <el-input :model-value="state.roleForm.roleCode" disabled />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="state.roleForm.roleName" />
        </el-form-item>
        <el-form-item label="角色说明">
          <el-input v-model="state.roleForm.roleDescription" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="$emit('update:roleDialogVisible', false)">取消</el-button>
        <template v-if="state.roleForm.id">
          <el-button type="primary" @click="actions.saveRole(true)">保存</el-button>
        </template>
        <template v-else>
          <el-button type="primary" plain @click="actions.saveRole(true)">保存并退出</el-button>
          <el-button type="primary" @click="actions.saveRole(false)">保存并新增</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { EmployeeRecord, PermissionTreeNode, RoleRecord } from '@/api'
import SystemSettingsRolesTab from './SystemSettingsRolesTab.vue'

defineProps<{
  permissions: {
    canCreate: boolean
    canEdit: boolean
    canDelete: boolean
    canAssignPermissions: boolean
    canAssignUsers: boolean
  }
  state: {
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
    roleDialogVisible: boolean
    roleForm: {
      id?: number
      roleCode?: string
      roleName: string
      roleDescription?: string
    }
  }
  actions: {
    handleRoleSelect: (role?: RoleRecord) => void
    openRoleDialog: (role?: RoleRecord) => void
    handleDeleteRole: () => Promise<void>
    saveRolePermissions: () => Promise<void>
    saveRoleUsers: () => Promise<void>
    saveRole: (closeAfterSave: boolean) => Promise<void>
  }
}>()

defineEmits<{
  (e: 'update:selectedRoleUserIds', value: number[]): void
  (e: 'update:roleDialogVisible', value: boolean): void
}>()
</script>
