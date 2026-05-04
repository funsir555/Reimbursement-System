import type { DepartmentTreeNode, EmployeeRecord } from '@/api'

export type OrganizationTreeNode = OrganizationDepartmentNode | OrganizationEmployeeNode

export type OrganizationDepartmentNode = {
  nodeKey: string
  nodeType: 'department'
  department: DepartmentTreeNode
  children: OrganizationTreeNode[]
}

export type OrganizationEmployeeNode = {
  nodeKey: string
  nodeType: 'employee'
  departmentId: number
  employee: EmployeeRecord
}

export function createOrganizationDepartmentNodeKey(id: number) {
  return `dept-${id}`
}

export function createOrganizationEmployeeNodeKey(userId: number, departmentId: number) {
  return `emp-${userId}-dept-${departmentId}`
}

export function isOrganizationDepartmentNode(
  node?: OrganizationTreeNode | null
): node is OrganizationDepartmentNode {
  return !!node && node.nodeType === 'department'
}

export function isOrganizationEmployeeNode(
  node?: OrganizationTreeNode | null
): node is OrganizationEmployeeNode {
  return !!node && node.nodeType === 'employee'
}

export function buildOrganizationTreeNodes(
  departments: DepartmentTreeNode[],
  employees: EmployeeRecord[]
): OrganizationDepartmentNode[] {
  const employeesByDepartmentId = new Map<number, EmployeeRecord[]>()
  for (const employee of employees) {
    const departmentIds = employee.departments?.length
      ? employee.departments.map((department) => department.deptId)
      : employee.deptId
        ? [employee.deptId]
        : []
    for (const departmentId of departmentIds) {
      const group = employeesByDepartmentId.get(departmentId) || []
      group.push(employee)
      employeesByDepartmentId.set(departmentId, group)
    }
  }

  for (const group of employeesByDepartmentId.values()) {
    group.sort(compareEmployeesByDisplayOrder)
  }

  return departments.map((department) => buildDepartmentNode(department, employeesByDepartmentId))
}

export function findOrganizationTreeNodeByKey(
  tree: OrganizationTreeNode[],
  nodeKey?: string
): OrganizationTreeNode | undefined {
  if (!nodeKey) {
    return undefined
  }
  for (const node of tree) {
    if (node.nodeKey === nodeKey) {
      return node
    }
    if (isOrganizationDepartmentNode(node)) {
      const matched = findOrganizationTreeNodeByKey(node.children, nodeKey)
      if (matched) {
        return matched
      }
    }
  }
  return undefined
}

function buildDepartmentNode(
  department: DepartmentTreeNode,
  employeesByDepartmentId: Map<number, EmployeeRecord[]>
): OrganizationDepartmentNode {
  const departmentChildren = (department.children || []).map((child) =>
    buildDepartmentNode(child, employeesByDepartmentId)
  )
  const employeeChildren = (employeesByDepartmentId.get(department.id) || []).map((employee) => ({
    nodeKey: createOrganizationEmployeeNodeKey(employee.userId, department.id),
    nodeType: 'employee' as const,
    departmentId: department.id,
    employee
  }))
  return {
    nodeKey: createOrganizationDepartmentNodeKey(department.id),
    nodeType: 'department',
    department,
    children: [...departmentChildren, ...employeeChildren]
  }
}

function compareEmployeesByDisplayOrder(left: EmployeeRecord, right: EmployeeRecord) {
  const leftName = String(left.name || '').trim()
  const rightName = String(right.name || '').trim()
  const byName = leftName.localeCompare(rightName, 'zh-CN')
  if (byName !== 0) {
    return byName
  }
  return String(left.username || '').trim().localeCompare(String(right.username || '').trim(), 'zh-CN')
}
