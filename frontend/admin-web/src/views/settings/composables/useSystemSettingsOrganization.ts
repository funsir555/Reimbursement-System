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
  isManualEmployee,
  removeDepartmentNode,
  type CompanyOption,
  type DepartmentConfigFormState,
  type DepartmentFormState,
  type DepartmentTreeExpandStrategy,
  type EmployeeOption
} from '../systemSettingsShared'
import {
  buildOrganizationTreeNodes,
  createOrganizationDepartmentNodeKey,
  findOrganizationTreeNodeByKey,
  isOrganizationDepartmentNode,
  isOrganizationEmployeeNode,
  type OrganizationTreeNode
} from '../systemSettingsOrganizationTree'

export function useSystemSettingsOrganization(params: {
  departments: Ref<DepartmentTreeNode[]>
  employees: Ref<EmployeeRecord[]>
  companyOptions: ComputedRef<CompanyOption[]>
  can: (code: string) => boolean
  loadBootstrap: () => Promise<void>
}) {
  const { departments, employees, can, loadBootstrap } = params

  const selectedDepartmentId = ref<number>()
  const selectedOrganizationNodeKey = ref<string>()
  const departmentExpandedKeys = ref<string[]>([])
  const nextDepartmentExpandStrategy = ref<DepartmentTreeExpandStrategy>('default')
  const nextDepartmentFocusId = ref<number>()
  const departmentDialogVisible = ref(false)

  const departmentForm = reactive<DepartmentFormState>(createDepartmentFormState())
  const departmentConfigForm = reactive<DepartmentConfigFormState>(createDepartmentConfigFormState())

  const organizationTreeNodes = computed(() =>
    buildOrganizationTreeNodes(departments.value, employees.value)
  )
  const selectedOrganizationNode = computed(() =>
    findOrganizationTreeNodeByKey(organizationTreeNodes.value, selectedOrganizationNodeKey.value)
  )
  const selectedOrganizationEmployee = computed(() =>
    isOrganizationEmployeeNode(selectedOrganizationNode.value)
      ? selectedOrganizationNode.value.employee
      : undefined
  )
  const selectedOrganizationNodeIsDepartment = computed(() =>
    isOrganizationDepartmentNode(selectedOrganizationNode.value)
  )
  const selectedOrganizationNodeIsEmployee = computed(() =>
    isOrganizationEmployeeNode(selectedOrganizationNode.value)
  )

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
      selectedOrganizationNodeIsDepartment.value &&
      can('settings:organization:edit') &&
      isManualDepartment(selectedDepartment.value)
  )
  const departmentCoreFieldsReadonly = computed(() => !departmentConfigEditable.value)
  const selectedDepartmentSyncLocked = computed(
    () =>
      !!selectedDepartment.value &&
      selectedOrganizationNodeIsDepartment.value &&
      !isManualDepartment(selectedDepartment.value)
  )
  const departmentStatEditable = computed(
    () =>
      !!selectedDepartment.value &&
      selectedOrganizationNodeIsDepartment.value &&
      can('settings:organization:edit')
  )
  const selectedOrganizationEmployeeSyncLocked = computed(
    () => !!selectedOrganizationEmployee.value && !isManualEmployee(selectedOrganizationEmployee.value)
  )

  watch(
    selectedDepartment,
    (department) => {
      fillDepartmentConfigForm(department)
    },
    { immediate: true }
  )

  function applyDepartmentBootstrap() {
    const availableNodeKeys = new Set(collectOrganizationNodeKeys(organizationTreeNodes.value))
    if (
      selectedOrganizationNodeKey.value &&
      !availableNodeKeys.has(selectedOrganizationNodeKey.value)
    ) {
      selectedOrganizationNodeKey.value = undefined
    }
    if (selectedDepartmentId.value && !findDepartmentById(departments.value, selectedDepartmentId.value)) {
      selectedDepartmentId.value = undefined
    }
    if (!selectedOrganizationNodeKey.value && departments.value[0]) {
      const firstDepartmentId = departments.value[0].id
      selectedDepartmentId.value = firstDepartmentId
      selectedOrganizationNodeKey.value = createOrganizationDepartmentNodeKey(firstDepartmentId)
    }
    applyDepartmentTreeExpandedKeys(departments.value)
  }

  function handleDepartmentSelect(node: DepartmentTreeNode) {
    selectedDepartmentId.value = node.id
    selectedOrganizationNodeKey.value = createOrganizationDepartmentNodeKey(node.id)
  }

  function handleOrganizationNodeSelect(node: OrganizationTreeNode) {
    selectedOrganizationNodeKey.value = node.nodeKey
    if (isOrganizationDepartmentNode(node)) {
      selectedDepartmentId.value = node.department.id
    }
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
    selectedDepartmentId.value = res.data.id
    selectedOrganizationNodeKey.value = createOrganizationDepartmentNodeKey(res.data.id)
    if (closeAfterSave) {
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
    preserveDepartmentTreeOnNextBootstrap(
      selectedDepartment.value.parentId ? selectedDepartment.value.id : undefined
    )
    await systemSettingsApi.updateDepartment(selectedDepartment.value.id, payload)
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
    if (selectedOrganizationNodeIsDepartment.value) {
      selectedOrganizationNodeKey.value = undefined
    }
    await loadBootstrap()
  }

  async function loadBootstrapKeepingDepartmentTreeState() {
    preserveDepartmentTreeOnNextBootstrap()
    await loadBootstrap()
  }

  function handleDepartmentNodeExpand(node: OrganizationTreeNode) {
    if (!isOrganizationDepartmentNode(node)) {
      return
    }
    departmentExpandedKeys.value = mergeDepartmentExpandedKeys(
      departmentExpandedKeys.value,
      node.nodeKey
    )
  }

  function handleDepartmentNodeCollapse(node: OrganizationTreeNode) {
    if (!isOrganizationDepartmentNode(node)) {
      return
    }
    collapseDepartmentSubtree(node)
  }

  function collapseDepartmentSubtree(node: OrganizationTreeNode) {
    const collapsedKeys = new Set(collectDepartmentSubtreeNodeKeys(node))
    departmentExpandedKeys.value = departmentExpandedKeys.value.filter(
      (key) => !collapsedKeys.has(key)
    )
  }

  function preserveDepartmentTreeOnNextBootstrap(focusDepartmentId?: number) {
    nextDepartmentExpandStrategy.value = 'preserve'
    nextDepartmentFocusId.value = focusDepartmentId
  }

  function applyDepartmentTreeExpandedKeys(tree: DepartmentTreeNode[]) {
    if (nextDepartmentExpandStrategy.value === 'focusPath' && nextDepartmentFocusId.value) {
      departmentExpandedKeys.value = buildDepartmentPathIds(tree, nextDepartmentFocusId.value).map((id) =>
        createOrganizationDepartmentNodeKey(id)
      )
    } else if (nextDepartmentExpandStrategy.value === 'preserve') {
      const availableDepartmentKeys = new Set(
        flattenDepartments(tree).map((item) => createOrganizationDepartmentNodeKey(item.id))
      )
      const focusPathKeys = nextDepartmentFocusId.value
        ? buildDepartmentPathIds(tree, nextDepartmentFocusId.value).map((id) =>
            createOrganizationDepartmentNodeKey(id)
          )
        : []
      departmentExpandedKeys.value = mergeDepartmentExpandedKeys(
        departmentExpandedKeys.value.filter((key) => availableDepartmentKeys.has(key)),
        ...focusPathKeys
      )
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
    selectedOrganizationNodeKey,
    selectedOrganizationNode,
    selectedOrganizationEmployee,
    selectedOrganizationEmployeeSyncLocked,
    selectedOrganizationNodeIsDepartment,
    selectedOrganizationNodeIsEmployee,
    organizationTreeNodes,
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
    handleOrganizationNodeSelect,
    handleDepartmentNodeExpand,
    handleDepartmentNodeCollapse,
    openDepartmentDialog,
    saveDepartment,
    saveDepartmentConfig,
    handleDeleteDepartment,
    loadBootstrapKeepingDepartmentTreeState
  }
}

function collectOrganizationNodeKeys(nodes: OrganizationTreeNode[]): string[] {
  return nodes.flatMap((node) =>
    isOrganizationDepartmentNode(node)
      ? [node.nodeKey, ...collectOrganizationNodeKeys(node.children)]
      : [node.nodeKey]
  )
}

function mergeDepartmentExpandedKeys(keys: string[], ...nextKeys: string[]) {
  return [...new Set([...keys, ...nextKeys])]
}

function collectDepartmentSubtreeNodeKeys(node: OrganizationTreeNode): string[] {
  if (!isOrganizationDepartmentNode(node)) {
    return []
  }
  return [
    node.nodeKey,
    ...node.children.flatMap((child) => collectDepartmentSubtreeNodeKeys(child))
  ]
}
