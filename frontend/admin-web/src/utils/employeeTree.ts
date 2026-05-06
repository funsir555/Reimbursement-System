import type { EmployeeDepartmentRef, EmployeeDirectoryEntry } from '@/api'

export type EmployeeTreeValue = string | number

export type EmployeeTreeValueType = 'string' | 'number' | 'raw'

export interface EmployeeTreeDepartmentLike {
  value?: EmployeeTreeValue | null
  label?: string | null
  code?: string | null
  name?: string | null
  parentValue?: EmployeeTreeValue | null
  id?: EmployeeTreeValue | null
  deptCode?: string | null
  deptName?: string | null
  parentId?: EmployeeTreeValue | null
  children?: EmployeeTreeDepartmentLike[] | null
}

export interface EmployeeTreeEmployeeLike extends Partial<EmployeeDirectoryEntry> {
  value?: EmployeeTreeValue | null
  label?: string | null
  id?: EmployeeTreeValue | null
}

export interface EmployeeTreeExtraOptionLike {
  value: EmployeeTreeValue
  label: string
  keywords?: Array<string | number | null | undefined>
  groupLabel?: string
}

export interface EmployeeTreeNode {
  value: EmployeeTreeValue
  label: string
  disabled?: boolean
  selectable?: boolean
  children: EmployeeTreeNode[]
  keywords?: string[]
  deptId?: EmployeeTreeValue
  deptName?: string
  isDepartment?: boolean
  isEmployee?: boolean
}

const EMPLOYEE_TREE_EXTRA_GROUP_PREFIX = '__EMPLOYEE_TREE_EXTRA_GROUP__'
const EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_VALUE = '__EMPLOYEE_TREE_UNASSIGNED__'
const EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_LABEL = '未分配部门'
export type EmployeeTreeLabelMode = 'default' | 'finance-assist'

export function normalizeEmployeeTreeValue(
  value: unknown,
  valueType: EmployeeTreeValueType = 'string'
): EmployeeTreeValue | undefined {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  if (valueType === 'number') {
    const numeric = Number(value)
    return Number.isFinite(numeric) ? numeric : undefined
  }
  if (valueType === 'raw' && (typeof value === 'string' || typeof value === 'number')) {
    return value
  }
  return String(value)
}

export function buildEmployeeTreeOptions(
  departments: EmployeeTreeDepartmentLike[] = [],
  employees: EmployeeTreeEmployeeLike[] = [],
  options: {
    valueType?: EmployeeTreeValueType
    extraOptions?: EmployeeTreeExtraOptionLike[]
    labelMode?: EmployeeTreeLabelMode
  } = {}
) {
  const valueType = options.valueType || 'string'
  const roots = buildDepartmentTreeSkeleton(departments, valueType)
  const departmentNodeMap = new Map<EmployeeTreeValue, EmployeeTreeNode>()
  collectDepartmentNodes(roots, departmentNodeMap)

  const employeeNodesByDepartment = new Map<EmployeeTreeValue, EmployeeTreeNode[]>()
  const syntheticDepartmentSeeds = new Map<EmployeeTreeValue, { label: string; keywords: string[] }>()

  employees.forEach((employee) => {
    const employeeNode = toEmployeeNode(employee, valueType, options.labelMode || 'default')
    if (!employeeNode) {
      return
    }
    const { deptValue, deptLabel, departmentKeywords } = resolveEmployeeDepartment(employee, valueType)
    if (deptValue !== undefined && !departmentNodeMap.has(deptValue)) {
      syntheticDepartmentSeeds.set(deptValue, {
        label: deptLabel || EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_LABEL,
        keywords: departmentKeywords
      })
    }
    const bucketValue = deptValue ?? EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_VALUE
    const bucket = employeeNodesByDepartment.get(bucketValue) || []
    bucket.push(employeeNode)
    employeeNodesByDepartment.set(bucketValue, bucket)
  })

  syntheticDepartmentSeeds.forEach((seed, deptValue) => {
    if (departmentNodeMap.has(deptValue)) {
      return
    }
    const node = createDepartmentNode({
      value: deptValue,
      label: seed.label,
      keywords: seed.keywords
    })
    roots.push(node)
    departmentNodeMap.set(deptValue, node)
  })

  if (employeeNodesByDepartment.has(EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_VALUE)) {
    const unassignedNode = createDepartmentNode({
      value: EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_VALUE,
      label: EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_LABEL,
      keywords: [EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_LABEL]
    })
    roots.push(unassignedNode)
    departmentNodeMap.set(EMPLOYEE_TREE_UNASSIGNED_DEPARTMENT_VALUE, unassignedNode)
  }

  employeeNodesByDepartment.forEach((nodes, deptValue) => {
    const departmentNode = departmentNodeMap.get(deptValue)
    if (!departmentNode) {
      return
    }
    nodes.sort(compareEmployeeTreeNode)
    departmentNode.children.push(...nodes)
  })

  const extraGroups = buildExtraOptionGroups(options.extraOptions || [], valueType)
  return [...extraGroups, ...roots]
}

export function appendMissingEmployeeTreeOptions(
  treeOptions: EmployeeTreeNode[],
  values: unknown[] = [],
  valueType: EmployeeTreeValueType = 'string'
) {
  const existingValues = new Set<EmployeeTreeValue>()
  walkEmployeeTree(treeOptions, (node) => {
    if (node.selectable) {
      existingValues.add(node.value)
    }
  })

  const missingNodes: EmployeeTreeNode[] = []
  values.forEach((item) => {
    const value = normalizeEmployeeTreeValue(item, valueType)
    if (value === undefined || existingValues.has(value)) {
      return
    }
    existingValues.add(value)
    missingNodes.push({
      value,
      label: String(value),
      selectable: true,
      isEmployee: true,
      keywords: [String(value)],
      children: []
    })
  })

  if (!missingNodes.length) {
    return treeOptions
  }

  return [
    ...treeOptions,
    createDepartmentNode({
      value: '__EMPLOYEE_TREE_LEGACY__',
      label: '历史值',
      keywords: ['历史值'],
      children: missingNodes
    })
  ]
}

export function filterEmployeeTreeNode(query: string, data?: Partial<EmployeeTreeNode>) {
  const keyword = normalizeText(query)?.toLowerCase()
  if (!keyword) {
    return true
  }
  return [
    data?.label,
    data?.deptName,
    data?.deptId,
    ...(data?.keywords || [])
  ]
    .map((item) => normalizeText(item))
    .filter((item): item is string => Boolean(item))
    .some((item) => item.toLowerCase().includes(keyword))
}

export function flattenSelectableEmployeeTreeNodes(nodes: EmployeeTreeNode[]) {
  const result: Array<{ value: EmployeeTreeValue; label: string }> = []
  walkEmployeeTree(nodes, (node) => {
    if (node.selectable) {
      result.push({ value: node.value, label: node.label })
    }
  })
  return result
}

function buildDepartmentTreeSkeleton(
  departments: EmployeeTreeDepartmentLike[],
  valueType: EmployeeTreeValueType
) {
  const flatDepartments: EmployeeTreeDepartmentLike[] = []
  flattenDepartmentOptions(departments, flatDepartments)

  const nodeMap = new Map<EmployeeTreeValue, EmployeeTreeNode>()
  const order: EmployeeTreeValue[] = []

  flatDepartments.forEach((item) => {
    const value = normalizeEmployeeTreeValue(item.value ?? item.id, valueType)
    if (value === undefined || nodeMap.has(value)) {
      return
    }
    const label = formatDepartmentLabel(item) || String(value)
    const keywords = compactTexts([
      item.label,
      item.name,
      item.deptName,
      item.code,
      item.deptCode,
      value
    ])
    nodeMap.set(value, createDepartmentNode({ value, label, keywords }))
    order.push(value)
  })

  const roots: EmployeeTreeNode[] = []
  order.forEach((value) => {
    const node = nodeMap.get(value)
    if (!node) {
      return
    }
    const source = flatDepartments.find((item) => normalizeEmployeeTreeValue(item.value ?? item.id, valueType) === value)
    const parentValue = normalizeEmployeeTreeValue(source?.parentValue ?? source?.parentId, valueType)
    if (parentValue !== undefined && parentValue !== value) {
      const parentNode = nodeMap.get(parentValue)
      if (parentNode) {
        parentNode.children.push(node)
        return
      }
    }
    roots.push(node)
  })
  return roots
}

function buildExtraOptionGroups(
  options: EmployeeTreeExtraOptionLike[],
  valueType: EmployeeTreeValueType
) {
  const groupMap = new Map<string, EmployeeTreeNode[]>()
  options.forEach((item) => {
    const value = normalizeEmployeeTreeValue(item.value, valueType)
    if (value === undefined) {
      return
    }
    const groupLabel = normalizeText(item.groupLabel) || '系统内置'
    const nodes = groupMap.get(groupLabel) || []
    nodes.push({
      value,
      label: item.label,
      selectable: true,
      isEmployee: true,
      keywords: compactTexts([item.label, value, ...(item.keywords || [])]),
      children: []
    })
    groupMap.set(groupLabel, nodes)
  })

  return Array.from(groupMap.entries()).map(([groupLabel, children], index) => (
    createDepartmentNode({
      value: `${EMPLOYEE_TREE_EXTRA_GROUP_PREFIX}_${index}`,
      label: groupLabel,
      keywords: [groupLabel],
      children: children.sort(compareEmployeeTreeNode)
    })
  ))
}

function createDepartmentNode(seed: {
  value: EmployeeTreeValue
  label: string
  keywords?: string[]
  children?: EmployeeTreeNode[]
}) {
  return {
    value: seed.value,
    label: seed.label,
    disabled: true,
    selectable: false,
    isDepartment: true,
    keywords: compactTexts([seed.label, ...(seed.keywords || [])]),
    children: seed.children || []
  } satisfies EmployeeTreeNode
}

function toEmployeeNode(
  employee: EmployeeTreeEmployeeLike,
  valueType: EmployeeTreeValueType,
  labelMode: EmployeeTreeLabelMode
) {
  const value = normalizeEmployeeTreeValue(
    employee.userId ?? employee.value ?? employee.id,
    valueType
  )
  if (value === undefined) {
    return null
  }
  const label = (labelMode === 'finance-assist' ? formatEmployeeAssistLabel(employee) : formatEmployeeLabel(employee)) || String(value)
  return {
    value,
    label,
    selectable: true,
    isEmployee: true,
    deptId: normalizeEmployeeTreeValue(employee.deptId, valueType),
    deptName: normalizeText(employee.deptName),
    keywords: compactTexts([
      label,
      employee.label,
      employee.name,
      employee.username,
      employee.phone,
      employee.email,
      employee.deptName,
      ...((employee.departments || []).map((item) => item?.deptName))
    ]),
    children: []
  } satisfies EmployeeTreeNode
}

function resolveEmployeeDepartment(
  employee: EmployeeTreeEmployeeLike,
  valueType: EmployeeTreeValueType
) {
  const departmentRefs = normalizeEmployeeDepartmentRefs(employee.departments)
  const primaryDepartmentValue =
    normalizeEmployeeTreeValue(employee.deptId, valueType)
    ?? normalizeEmployeeTreeValue(departmentRefs[0]?.deptId, valueType)
  const primaryDepartmentName =
    normalizeText(employee.deptName)
    ?? normalizeText(departmentRefs[0]?.deptName)
  return {
    deptValue: primaryDepartmentValue,
    deptLabel: primaryDepartmentName,
    departmentKeywords: compactTexts([
      primaryDepartmentName,
      ...departmentRefs.map((item) => item.deptName)
    ])
  }
}

function normalizeEmployeeDepartmentRefs(departments?: EmployeeDepartmentRef[] | null) {
  return Array.isArray(departments) ? departments.filter(Boolean) : []
}

function collectDepartmentNodes(nodes: EmployeeTreeNode[], target: Map<EmployeeTreeValue, EmployeeTreeNode>) {
  nodes.forEach((node) => {
    if (node.isDepartment) {
      target.set(node.value, node)
    }
    if (node.children.length) {
      collectDepartmentNodes(node.children, target)
    }
  })
}

function flattenDepartmentOptions(source: EmployeeTreeDepartmentLike[], target: EmployeeTreeDepartmentLike[]) {
  source.forEach((item) => {
    target.push(item)
    if (item.children?.length) {
      flattenDepartmentOptions(item.children, target)
    }
  })
}

function walkEmployeeTree(nodes: EmployeeTreeNode[], visitor: (node: EmployeeTreeNode) => void) {
  nodes.forEach((node) => {
    visitor(node)
    if (node.children.length) {
      walkEmployeeTree(node.children, visitor)
    }
  })
}

function formatDepartmentLabel(option?: EmployeeTreeDepartmentLike | null) {
  if (!option) {
    return ''
  }
  const code = normalizeText(option.code ?? option.deptCode)
  const name = normalizeText(option.name ?? option.deptName)
  const label = normalizeText(option.label)
  const value = normalizeText(option.value ?? option.id)
  if (code && name) {
    return `${code}  ${name}`
  }
  return name || code || label || value || ''
}

function formatEmployeeLabel(employee?: EmployeeTreeEmployeeLike | null) {
  if (!employee) {
    return ''
  }
  const explicitLabel = normalizeText(employee.label)
  if (explicitLabel) {
    return explicitLabel
  }
  const name = normalizeText(employee.name)
  const username = normalizeText(employee.username)
  if (name && username && name !== username) {
    return `${name} (${username})`
  }
  return name || username || ''
}

function formatEmployeeAssistLabel(employee?: EmployeeTreeEmployeeLike | null) {
  if (!employee) {
    return ''
  }
  const username = normalizeText(employee.username)
  const name = normalizeText(employee.name)
  if (username && name) {
    return `${username}  ${name}`
  }
  return formatEmployeeLabel(employee)
}

function compactTexts(values: Array<string | number | null | undefined>) {
  return values
    .map((item) => normalizeText(item))
    .filter((item): item is string => Boolean(item))
}

function compareEmployeeTreeNode(left: EmployeeTreeNode, right: EmployeeTreeNode) {
  return left.label.localeCompare(right.label, 'zh-CN')
}

function normalizeText(value?: unknown) {
  const text = String(value ?? '').trim()
  return text || undefined
}
