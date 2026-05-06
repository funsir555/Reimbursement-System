import type { EmployeeDirectoryEntry } from './system-settings-types'
import type { ProcessFlowConditionField, ProcessFlowConditionGroup } from './process-flow-types'
import type { ProcessFormOption } from './process-template-types'

export interface ProcessUserGroupTreeNode {
  id: number
  parentId?: number
  groupCode: string
  groupName: string
  codeLevel: number
  children: ProcessUserGroupTreeNode[]
}

export interface ProcessUserGroupDetail {
  id?: number
  parentId?: number
  groupCode?: string
  groupName: string
  codeLevel?: number
  memberUserIds: string[]
  scopeConditionGroups: ProcessFlowConditionGroup[]
}

export interface ProcessUserGroupMeta {
  scopeConditionFields: ProcessFlowConditionField[]
  scopeOperatorOptions: ProcessFormOption[]
  companyOptions: ProcessFormOption[]
  departmentOptions: ProcessFormOption[]
  userOptions: ProcessFormOption[]
  employeeDirectory: EmployeeDirectoryEntry[]
}

export interface ProcessUserGroupSavePayload {
  parentId?: number
  groupName: string
  memberUserIds: string[]
  scopeConditionGroups: ProcessFlowConditionGroup[]
}
