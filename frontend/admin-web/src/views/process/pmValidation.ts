import type {
  ProcessFlowConditionField,
  ProcessFlowSavePayload,
  ProcessExpenseDetailDesignSummary,
  ProcessFlowScene,
  ProcessFormDesignBlock,
  ProcessFormDesignSchema,
  ProcessFormOption,
  ProcessTemplateFormOptions
} from '@/api'
import { getBusinessComponentDefinition } from '@/views/process/formDesignerHelper'

export const PM_NAME_MAX_LENGTH = 64
export const PM_TITLE_MAX_LENGTH = 128
export const PM_FIELD_KEY_MAX_LENGTH = 64

const DOCUMENT_TITLE_FIELD_KEYS = new Set(['__documentTitle', 'documentTitle', 'title'])
const DOCUMENT_RELATION_COMPONENT_CODES = new Set(['related-document', 'writeoff-document'])

type TemplateBindingOption = ProcessFormOption | Pick<ProcessExpenseDetailDesignSummary, 'detailCode'> | string

function trimValue(value: unknown) {
  return typeof value === 'string' ? value.trim() : String(value ?? '').trim()
}

function hasOptionValue(option: Exclude<TemplateBindingOption, string>): option is ProcessFormOption {
  return 'value' in option
}

function hasDetailCode(
  option: Exclude<TemplateBindingOption, string>
): option is Pick<ProcessExpenseDetailDesignSummary, 'detailCode'> {
  return 'detailCode' in option
}

function optionValue(option: TemplateBindingOption) {
  if (typeof option === 'string') {
    return option
  }
  if (hasOptionValue(option)) {
    return String(option.value || '')
  }
  if (hasDetailCode(option)) {
    return String(option.detailCode || '')
  }
  return ''
}

export function validateMaxLength(value: unknown, max: number, label: string) {
  return trimValue(value).length > max ? `${label}\u6700\u591a ${max} \u4e2a\u5b57\u7b26` : ''
}

export function validateIndexedMaxLength(value: unknown, max: number, label: string, index: number) {
  return trimValue(value).length > max ? `\u7b2c ${index + 1} \u4e2a${label}\u6700\u591a ${max} \u4e2a\u5b57\u7b26` : ''
}

export function isTitleFieldKey(fieldKey?: string) {
  return DOCUMENT_TITLE_FIELD_KEYS.has(trimValue(fieldKey))
}

export function documentTitleMaxLength(block: Pick<ProcessFormDesignBlock, 'fieldKey'>) {
  return isTitleFieldKey(block.fieldKey) ? PM_TITLE_MAX_LENGTH : undefined
}

export function isRelationSourceFieldBlock(block: ProcessFormDesignBlock) {
  if (block.kind !== 'BUSINESS_COMPONENT') {
    return false
  }
  const componentCode = getBusinessComponentDefinition(String(block.props.componentCode || ''))?.code
    || String(block.props.componentCode || '')
  return DOCUMENT_RELATION_COMPONENT_CODES.has(componentCode)
}

export function validateTemplateBindingValue(
  value: string,
  options: TemplateBindingOption[],
  label: string
) {
  const normalizedValue = trimValue(value)
  if (!normalizedValue) {
    return ''
  }
  return options.some((item) => optionValue(item) === normalizedValue)
    ? ''
    : `${label}\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9`
}

export function collectTemplateBindingIssues(
  form: {
    templateName?: string
    formDesign?: string
    approvalFlow?: string
    expenseDetailDesign?: string
  },
  options: ProcessTemplateFormOptions | null,
  isReportTemplate: boolean
) {
  const issues: string[] = []
  const templateNameIssue = validateMaxLength(form.templateName, PM_NAME_MAX_LENGTH, '\u6a21\u677f\u540d\u79f0')
  if (templateNameIssue) {
    issues.push(templateNameIssue)
  }
  if (!options) {
    return issues
  }
  const formDesignIssue = validateTemplateBindingValue(form.formDesign || '', options.formDesignOptions || [], '\u8868\u5355\u8bbe\u8ba1')
  if (formDesignIssue) {
    issues.push(formDesignIssue)
  }
  const approvalFlowIssue = validateTemplateBindingValue(form.approvalFlow || '', options.approvalFlows || [], '\u5ba1\u6279\u6d41\u7a0b')
  if (approvalFlowIssue) {
    issues.push(approvalFlowIssue)
  }
  if (isReportTemplate) {
    const expenseDetailIssue = validateTemplateBindingValue(
      form.expenseDetailDesign || '',
      options.expenseDetailDesignOptions || [],
      '\u8d39\u7528\u660e\u7ec6\u8868\u5355'
    )
    if (expenseDetailIssue) {
      issues.push(expenseDetailIssue)
    }
  }
  return issues
}

export function validateFlowPayload(
  payload: ProcessFlowSavePayload,
  branchConditionFields: ProcessFlowConditionField[],
  sceneOptions: ProcessFlowScene[]
) {
  const issues: string[] = []
  const flowNameIssue = validateMaxLength(payload.flowName, PM_NAME_MAX_LENGTH, '\u6d41\u7a0b\u540d\u79f0')
  if (flowNameIssue) {
    issues.push(flowNameIssue)
  }

  const validFieldKeys = new Set(branchConditionFields.map((item) => trimValue(item.key)).filter(Boolean))
  const validSceneIds = new Set(sceneOptions.map((item) => Number(item.id)).filter((item) => Number.isFinite(item)))

  payload.nodes.forEach((node, index) => {
    const nodeNameIssue = validateIndexedMaxLength(node.nodeName, PM_NAME_MAX_LENGTH, '\u8282\u70b9\u540d\u79f0', index)
    if (nodeNameIssue) {
      issues.push(nodeNameIssue)
    }
    if (node.sceneId != null && !validSceneIds.has(Number(node.sceneId))) {
      const displayName = trimValue(node.nodeName) || `\u7b2c ${index + 1} \u4e2a\u8282\u70b9`
      issues.push(`\u8282\u70b9${displayName}\u7ed1\u5b9a\u7684\u573a\u666f\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9`)
    }
  })

  payload.routes.forEach((route, routeIndex) => {
    const routeIssue = validateIndexedMaxLength(route.routeName, PM_NAME_MAX_LENGTH, '\u5206\u652f\u540d\u79f0', routeIndex)
    if (routeIssue) {
      issues.push(routeIssue)
    }
    route.conditionGroups.forEach((group, groupIndex) => {
      group.conditions.forEach((condition, conditionIndex) => {
        const fieldKeyIssue = validateMaxLength(
          condition.fieldKey,
          PM_FIELD_KEY_MAX_LENGTH,
          `\u7b2c ${routeIndex + 1} \u6761\u5206\u652f\u7b2c ${groupIndex + 1} \u7ec4\u7b2c ${conditionIndex + 1} \u4e2a\u6761\u4ef6\u5b57\u6bb5\u6807\u8bc6`
        )
        if (fieldKeyIssue) {
          issues.push(fieldKeyIssue)
          return
        }
        const fieldKey = trimValue(condition.fieldKey)
        if (!fieldKey || !validFieldKeys.has(fieldKey)) {
          issues.push(`\u7b2c ${routeIndex + 1} \u6761\u5206\u652f\u7b2c ${groupIndex + 1} \u7ec4\u7b2c ${conditionIndex + 1} \u4e2a\u6761\u4ef6\u5b57\u6bb5\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9`)
        }
      })
    })
  })

  return issues
}

export function validateSchemaFieldKeys(schema: ProcessFormDesignSchema, subjectLabel: string) {
  const issues: string[] = []
  const seen = new Set<string>()
  schema.blocks.forEach((block, index) => {
    const fieldKey = trimValue(block.fieldKey)
    if (!fieldKey) {
      issues.push(`\u7b2c ${index + 1} \u4e2a\u5b57\u6bb5\u6807\u8bc6\u4e0d\u80fd\u4e3a\u7a7a`)
      return
    }
    const lengthIssue = validateMaxLength(fieldKey, PM_FIELD_KEY_MAX_LENGTH, `\u5b57\u6bb5\u6807\u8bc6 ${fieldKey}`)
    if (lengthIssue) {
      issues.push(lengthIssue)
    }
    if (seen.has(fieldKey)) {
      issues.push(`${subjectLabel}\u5b57\u6bb5\u6807\u8bc6 ${fieldKey} \u4e0d\u80fd\u91cd\u590d`)
      return
    }
    seen.add(fieldKey)
  })
  return issues
}

export function validateArchiveRuleFieldKey(
  fieldKey: string,
  allowedFieldKeys: string[],
  itemIndex: number,
  ruleIndex: number
) {
  const lengthIssue = validateMaxLength(
    fieldKey,
    PM_FIELD_KEY_MAX_LENGTH,
    `\u7b2c ${itemIndex + 1} \u4e2a\u7ed3\u679c\u9879\u7b2c ${ruleIndex + 1} \u6761\u89c4\u5219\u5b57\u6bb5\u6807\u8bc6`
  )
  if (lengthIssue) {
    return lengthIssue
  }
  const normalizedFieldKey = trimValue(fieldKey)
  if (!normalizedFieldKey || !allowedFieldKeys.includes(normalizedFieldKey)) {
    return `\u7b2c ${itemIndex + 1} \u4e2a\u7ed3\u679c\u9879\u7b2c ${ruleIndex + 1} \u6761\u89c4\u5219\u5b57\u6bb5\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9`
  }
  return ''
}

export function validateExpenseRuntimeSchema(schema: ProcessFormDesignSchema) {
  const issues: string[] = []
  schema.blocks.forEach((block) => {
    if (!isRelationSourceFieldBlock(block)) {
      return
    }
    const fieldKeyIssue = validateMaxLength(
      block.fieldKey,
      PM_FIELD_KEY_MAX_LENGTH,
      `\u5b57\u6bb5\u6807\u8bc6 ${trimValue(block.fieldKey) || block.label || ''}`
    )
    if (fieldKeyIssue) {
      issues.push(fieldKeyIssue)
    }
  })
  return issues
}

export function validateRuntimeTitleValues(schema: ProcessFormDesignSchema, formData: Record<string, unknown>) {
  const issues: string[] = []
  schema.blocks.forEach((block) => {
    if (!isTitleFieldKey(block.fieldKey)) {
      return
    }
    const issue = validateMaxLength(formData[block.fieldKey], PM_TITLE_MAX_LENGTH, '\u5355\u636e\u6807\u9898')
    if (issue) {
      issues.push(issue)
    }
  })
  return issues
}
