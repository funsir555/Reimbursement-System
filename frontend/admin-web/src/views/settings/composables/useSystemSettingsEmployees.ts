import { computed, reactive, ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  systemSettingsApi,
  type EmployeeEditorFormState,
  type EmployeeRecord,
  type EmployeeSavePayload,
  type RoleRecord
} from '@/api'
import {
  isManualEmployee,
  isSuperAdminRole,
  sanitizeEditableRoleIds
} from '../systemSettingsShared'

export function useSystemSettingsEmployees(params: {
  employees: Ref<EmployeeRecord[]>
  roles: Ref<RoleRecord[]>
  can: (code: string) => boolean
  loadBootstrap: () => Promise<void>
}) {
  const { employees, roles, can, loadBootstrap } = params

  const selectedEmployee = ref<EmployeeRecord>()
  const employeeSyncLocked = ref(false)
  const employeeKeyword = ref('')
  const employeeStatusFilter = ref<number>()
  const employeeDialogVisible = ref(false)
  const employeeForm = reactive<EmployeeEditorFormState>({
    username: '',
    name: '',
    statDepartmentBelong: '',
    statRegionBelong: '',
    statAreaBelong: '',
    roleIds: []
  })

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

  const employeeRoleOptions = computed(() => roles.value)
  const canAssignEmployeeRoles = computed(() => can('settings:roles:assign_users'))
  const selectedEmployeeSyncLocked = computed(
    () => !!selectedEmployee.value && !isManualEmployee(selectedEmployee.value)
  )
  const employeeCoreFieldsReadonly = computed(() => employeeSyncLocked.value)
  const employeeHasSuperAdminRole = computed(() =>
    employeeForm.roleIds.some((roleId) =>
      roles.value.some((role) => role.id === roleId && isSuperAdminRole(role))
    )
  )
  const roleNameByCode = computed<Record<string, string>>(() =>
    Object.fromEntries(roles.value.map((item) => [item.roleCode, item.roleName]))
  )

  function applyEmployeesBootstrap() {
    selectedEmployee.value = selectedEmployee.value
      ? employees.value.find((item) => item.userId === selectedEmployee.value?.userId)
      : undefined
  }

  function openEmployeeDialog(item?: EmployeeRecord) {
    employeeSyncLocked.value = !!item && !isManualEmployee(item)
    employeeForm.userId = item?.userId
    employeeForm.username = item?.username || ''
    employeeForm.name = item?.name || ''
    employeeForm.phone = item?.phone
    employeeForm.email = item?.email
    employeeForm.deptId = item?.deptId
    employeeForm.companyId = item?.companyId
    employeeForm.position = item?.position
    employeeForm.laborRelationBelong = item?.laborRelationBelong
    employeeForm.statDepartmentBelong = item?.statDepartmentBelong || ''
    employeeForm.statRegionBelong = item?.statRegionBelong || ''
    employeeForm.statAreaBelong = item?.statAreaBelong || ''
    employeeForm.roleIds = resolveEmployeeRoleIds(item)
    employeeDialogVisible.value = true
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
      await systemSettingsApi.assignUserRoles(
        savedUserId,
        sanitizeEditableRoleIds(roleIds, roles.value)
      )
      if (!userId && !closeAfterSave) {
        resetEmployeeForm()
      }
      ElMessage.success('员工与角色已保存')
    } catch (error: any) {
      ElMessage.warning(error?.message || '员工已保存，但角色保存失败')
    }

    await loadBootstrap()
  }

  async function handleDeleteEmployee() {
    if (!selectedEmployee.value) {
      return
    }
    if (selectedEmployeeSyncLocked.value) {
      ElMessage.warning('同步员工不能直接删除，请通过同步清理')
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

  function resetEmployeeForm() {
    employeeSyncLocked.value = false
    employeeForm.userId = undefined
    employeeForm.username = ''
    employeeForm.name = ''
    employeeForm.phone = undefined
    employeeForm.email = undefined
    employeeForm.deptId = undefined
    employeeForm.companyId = undefined
    employeeForm.position = undefined
    employeeForm.laborRelationBelong = undefined
    employeeForm.statDepartmentBelong = ''
    employeeForm.statRegionBelong = ''
    employeeForm.statAreaBelong = ''
    employeeForm.roleIds = []
  }

  return {
    selectedEmployee,
    employeeSyncLocked,
    employeeKeyword,
    employeeStatusFilter,
    employeeDialogVisible,
    employeeForm,
    filteredEmployees,
    employeeRoleOptions,
    canAssignEmployeeRoles,
    selectedEmployeeSyncLocked,
    employeeCoreFieldsReadonly,
    employeeHasSuperAdminRole,
    applyEmployeesBootstrap,
    openEmployeeDialog,
    saveEmployee,
    handleDeleteEmployee,
    resolveEmployeeRoleNames
  }
}
