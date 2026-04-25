import { computed, reactive, ref, watch, type ComputedRef, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemSettingsApi, type DepartmentSavePayload, type DepartmentTreeNode, type EmployeeRecord } from '@/api'
import {
  buildDepartmentPathIds,
  createDepartmentConfigFormState,
  createDepartmentFormState,
  findDepartmentById,
  flattenDepartments,
  isManualDepartment,
  isTopLevelDepartment,
  removeDepartmentNode,
  type CompanyOption,
  type DepartmentConfigFormState,
  type DepartmentFormState,
  type DepartmentTreeExpandStrategy,
  type EmployeeOption
} from '../systemSettingsShared'

export function useSystemSettingsOrganization(params: {
  departments: Ref<DepartmentTreeNode[]>
  employees: Ref<EmployeeRecord[]>
  companyOptions: ComputedRef<CompanyOption[]>
  can: (code: string) => boolean
  loadBootstrap: () => Promise<void>
}) {
  const { departments, employees, can, loadBootstrap } = params

  const selectedDepartmentId = ref<number>()
  const departmentExpandedKeys = ref<number[]>([])
  const nextDepartmentExpandStrategy = ref<DepartmentTreeExpandStrategy>('default')
  const nextDepartmentFocusId = ref<number>()
  const departmentDialogVisible = ref(false)

  const departmentForm = reactive<DepartmentFormState>(createDepartmentFormState())
  const departmentConfigForm = reactive<DepartmentConfigFormState>(createDepartmentConfigFormState())

  const selectedDepartment = computed(() =>
    findDepartmentById(departments.value, selectedDepartmentId.value)
  )
  const departmentCount = computed(() => flattenDepartments(departments.value).length)
  const departmentOptions = computed(() => departments.value)
  const departmentParentOptions = computed(() =>
    removeDepartmentNode(departments.value, selectedDepartmentId.value)
  )
  const employeeOptions = computed<EmployeeOption[]>(() =>
    employees.value.map((item) => ({
      userId: item.userId,
      label: `${item.name} (${item.username})${item.status === 0 ? ' [停用]' : ''}`
    }))
  )
  const departmentConfigEditable = computed(
    () =>
      !!selectedDepartment.value &&
      can('settings:organization:edit') &&
      isManualDepartment(selectedDepartment.value)
  )
  const departmentCoreFieldsReadonly = computed(() => !departmentConfigEditable.value)
  const selectedDepartmentSyncLocked = computed(
    () => !!selectedDepartment.value && !isManualDepartment(selectedDepartment.value)
  )
  const departmentStatEditable = computed(
    () => !!selectedDepartment.value && can('settings:organization:edit')
  )

  watch(
    selectedDepartment,
    (department) => {
      fillDepartmentConfigForm(department)
    },
    { immediate: true }
  )

  function applyDepartmentBootstrap() {
    if (
      selectedDepartmentId.value &&
      !findDepartmentById(departments.value, selectedDepartmentId.value)
    ) {
      selectedDepartmentId.value = undefined
    }
    if (!selectedDepartmentId.value && departments.value[0]) {
      selectedDepartmentId.value = departments.value[0].id
    }
    applyDepartmentTreeExpandedKeys(departments.value)
  }

  function handleDepartmentSelect(node: DepartmentTreeNode) {
    selectedDepartmentId.value = node.id
  }

  function openDepartmentDialog() {
    resetDepartmentForm()
    departmentDialogVisible.value = true
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
    if (isTopLevelDepartment(selectedDepartment.value)) {
      nextDepartmentExpandStrategy.value = 'default'
      nextDepartmentFocusId.value = undefined
    } else {
      nextDepartmentExpandStrategy.value = 'focusPath'
      nextDepartmentFocusId.value = selectedDepartment.value.id
    }
    ElMessage.success('部门配置已保存')
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

  function applyDepartmentTreeExpandedKeys(tree: DepartmentTreeNode[]) {
    if (nextDepartmentExpandStrategy.value === 'focusPath' && nextDepartmentFocusId.value) {
      departmentExpandedKeys.value = buildDepartmentPathIds(tree, nextDepartmentFocusId.value)
    } else {
      departmentExpandedKeys.value = []
    }
    nextDepartmentExpandStrategy.value = 'default'
    nextDepartmentFocusId.value = undefined
  }

  function fillDepartmentConfigForm(item?: DepartmentTreeNode) {
    departmentConfigForm.deptName = item?.deptName || ''
    departmentConfigForm.parentId = item?.parentId
    departmentConfigForm.companyId = item?.companyId
    departmentConfigForm.leaderUserId = item?.leaderUserId
    departmentConfigForm.statDepartmentBelong = item?.statDepartmentBelong || ''
    departmentConfigForm.statRegionBelong = item?.statRegionBelong || ''
    departmentConfigForm.statAreaBelong = item?.statAreaBelong || ''
  }

  function resetDepartmentForm() {
    Object.assign(departmentForm, createDepartmentFormState())
  }

  return {
    selectedDepartmentId,
    departmentExpandedKeys,
    departmentDialogVisible,
    departmentForm,
    departmentConfigForm,
    selectedDepartment,
    departmentCount,
    departmentOptions,
    departmentParentOptions,
    employeeOptions,
    departmentCoreFieldsReadonly,
    selectedDepartmentSyncLocked,
    departmentStatEditable,
    applyDepartmentBootstrap,
    handleDepartmentSelect,
    openDepartmentDialog,
    saveDepartment,
    saveDepartmentConfig,
    handleDeleteDepartment
  }
}
