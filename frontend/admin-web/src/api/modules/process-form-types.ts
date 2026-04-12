// 这里定义 process-form-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

export type ProcessFormPermissionValue = 'EDITABLE' | 'READONLY' | 'HIDDEN'

export type ProcessFormPermissionStage =
  | 'DRAFT_BEFORE_SUBMIT'
  | 'RESUBMIT_AFTER_RETURN'
  | 'IN_APPROVAL'
  | 'ARCHIVED'

export interface ProcessFormSceneOverride {
  sceneId: number
  permission: ProcessFormPermissionValue
}

export interface ProcessFormFieldPermission {
  fixedStages: Record<ProcessFormPermissionStage, ProcessFormPermissionValue>
  sceneOverrides: ProcessFormSceneOverride[]
}

export interface ProcessFormDesignBlock {
  blockId: string
  fieldKey: string
  kind: 'CONTROL' | 'BUSINESS_COMPONENT' | 'SHARED_FIELD'
  label: string
  span: number
  helpText?: string
  required: boolean
  defaultValue?: unknown
  props: Record<string, unknown>
  permission: ProcessFormFieldPermission
}

export interface ProcessFormDesignSchema {
  layoutMode: string
  blocks: ProcessFormDesignBlock[]
}

// 这是 ProcessFormDesignSummary 的数据结构。
export interface ProcessFormDesignSummary {
  id: number
  formCode: string
  formName: string
  templateType: string
  templateTypeLabel: string
  formDescription?: string
  updatedAt: string
}

export interface ProcessFormDesignDetail extends ProcessFormDesignSummary {
  schema: ProcessFormDesignSchema
}

export interface ProcessFormDesignSavePayload {
  templateType: string
  formName: string
  formDescription?: string
  schema: ProcessFormDesignSchema
}

export interface ProcessExpenseDetailDesignSummary {
  id: number
  detailCode: string
  detailName: string
  detailType: string
  detailTypeLabel: string
  detailDescription?: string
  updatedAt: string
}

export interface ProcessExpenseDetailDesignDetail extends ProcessExpenseDetailDesignSummary {
  schema: ProcessFormDesignSchema
}

export interface ProcessExpenseDetailDesignSavePayload {
  detailName: string
  detailType: string
  detailDescription?: string
  schema: ProcessFormDesignSchema
}
