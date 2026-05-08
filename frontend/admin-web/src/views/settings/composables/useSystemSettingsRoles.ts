import { computed, reactive, ref, watch, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  systemSettingsApi,
  type EmployeeRecord,
  type PermissionTreeNode,
  type RoleRecord,
  type RoleSavePayload,
  type SystemSettingsBootstrapData
} from '@/api'
import { SUPER_ADMIN_ROLE_CODE, isSuperAdminRole } from '../systemSettingsShared'

type RoleFormState = {
  id?: number
  roleCode?: string
  roleName: string
  roleDescription?: string
}

export function useSystemSettingsRoles(params: {
  roles: Ref<RoleRecord[]>
  permissions: Ref<PermissionTreeNode[]>
  employees: Ref<EmployeeRecord[]>
  currentUser: Ref<SystemSettingsBootstrapData['currentUser'] | null>
  loadBootstrap: () => Promise<void>
}) {
  const { roles, employees, currentUser, loadBootstrap } = params

  const selectedRole = ref<RoleRecord>()
  const selectedRoleUserIds = ref<number[]>([])
  const roleDialogVisible = ref(false)
  const roleForm = reactive<RoleFormState>({
    roleCode: '',
    roleName: ''
  })

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

  watch(
    () => selectedRole.value?.id,
    () => {
      selectedRoleUserIds.value = selectedRole.value?.userIds ? [...selectedRole.value.userIds] : []
    }
  )

  function applyRolesBootstrap() {
    selectedRole.value = selectedRole.value
      ? roles.value.find((item) => item.id === selectedRole.value?.id)
      : undefined
  }

  function handleRoleSelect(role?: RoleRecord) {
    selectedRole.value = role
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

  async function saveRolePermissions(permissionCodes: string[]) {
    if (!selectedRole.value) {
      return
    }
    if (rolePermissionReadonly.value) {
      ElMessage.warning('超级管理员权限仅可由超级管理员修改')
      return
    }
    await systemSettingsApi.assignRolePermissions(selectedRole.value.id, permissionCodes)
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
      await systemSettingsApi.assignUserRoles(
        employee.userId,
        roleIds.filter((roleId) => {
          const role = roles.value.find((item) => item.id === roleId)
          return role ? !isSuperAdminRole(role) : true
        })
      )
    }

    ElMessage.success('用户角色已更新')
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

  function resetRoleForm() {
    roleForm.id = undefined
    roleForm.roleCode = ''
    roleForm.roleName = ''
    roleForm.roleDescription = undefined
  }

  return {
    selectedRole,
    selectedRoleUserIds,
    roleDialogVisible,
    roleForm,
    selectedRoleProtected,
    selectedRoleReadonly,
    rolePermissionReadonly,
    roleUserAssignmentReadonly,
    applyRolesBootstrap,
    handleRoleSelect,
    openRoleDialog,
    saveRole,
    saveRolePermissions,
    saveRoleUsers,
    handleDeleteRole
  }
}
