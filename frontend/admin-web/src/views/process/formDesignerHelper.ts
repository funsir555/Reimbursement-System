import type {
  ProcessCustomArchiveSummary,
  ProcessFormDesignBlock,
  ProcessFormDesignSchema,
  ProcessFormFieldPermission,
  ProcessFormPermissionStage,
  ProcessFormPermissionValue
} from '@/api'

export type FormDesignerPaletteItem = {
  key: string
  label: string
  description: string
  category?: string
  kind: ProcessFormDesignBlock['kind']
  span?: number
  disabled?: boolean
  props: Record<string, unknown>
}

export type BusinessComponentDefinition = {
  code: string
  label: string
  description: string
  previewFields: string[]
  defaultDisplayLabel?: string
}

export type DocumentTemplateTypeOption = {
  value: string
  label: string
}

export type FormBlockQuickActionState = 'expandable' | 'collapsible' | 'hidden'

export const FORM_PERMISSION_STAGE_OPTIONS: Array<{ key: ProcessFormPermissionStage; label: string }> = [
  { key: 'DRAFT_BEFORE_SUBMIT', label: '发起人提交前' },
  { key: 'RESUBMIT_AFTER_RETURN', label: '驳回/召回后重填' },
  { key: 'IN_APPROVAL', label: '审批中' },
  { key: 'ARCHIVED', label: '归档后' }
]

export const FORM_PERMISSION_VALUE_OPTIONS: Array<{ value: ProcessFormPermissionValue; label: string }> = [
  { value: 'EDITABLE', label: '可编辑' },
  { value: 'READONLY', label: '只读' },
  { value: 'HIDDEN', label: '隐藏' }
]

export const CONTROL_PALETTE_ITEMS: FormDesignerPaletteItem[] = [
  createPaletteItem('text', '单行文本', '输入简短文本内容', 1, { controlType: 'TEXT', placeholder: '请输入内容' }, false, '基础输入'),
  createPaletteItem('textarea', '多行文本', '适合填写较长说明', 1, { controlType: 'TEXTAREA', placeholder: '请输入详细说明' }, false, '基础输入'),
  createPaletteItem('number', '数字', '填写整数或普通数值', 1, { controlType: 'NUMBER', placeholder: '请输入数字' }, false, '基础输入'),
  createPaletteItem('amount', '金额', '带两位小数的金额输入', 1, { controlType: 'AMOUNT', placeholder: '请输入金额', precision: 2 }, false, '基础输入'),
  createPaletteItem('date', '日期', '选择单个日期', 1, { controlType: 'DATE' }, false, '基础输入'),
  createPaletteItem('daterange', '日期区间', '选择开始与结束日期', 1, { controlType: 'DATE_RANGE' }, false, '基础输入'),
  createPaletteItem('select', '下拉单选', '从固定选项中选择一项', 1, { controlType: 'SELECT', options: buildDefaultOptions() }, false, '选项选择'),
  createPaletteItem('multi-select', '下拉多选', '从固定选项中选择多项', 1, { controlType: 'MULTI_SELECT', options: buildDefaultOptions() }, false, '选项选择'),
  createPaletteItem('radio', '单选组', '平铺展示单选项', 1, { controlType: 'RADIO', options: buildDefaultOptions() }, false, '选项选择'),
  createPaletteItem('checkbox', '复选组', '平铺展示复选项', 1, { controlType: 'CHECKBOX', options: buildDefaultOptions() }, false, '选项选择'),
  createPaletteItem('switch', '开关', '是/否切换', 1, { controlType: 'SWITCH' }, false, '选项选择'),
  createPaletteItem('attachment', '附件', '上传附件文件', 1, {
    controlType: 'ATTACHMENT',
    maxCount: 5,
    maxSizeMb: 10,
    accept: '.pdf,.doc,.docx,.xls,.xlsx,.zip'
  }, false, '上传展示'),
  createPaletteItem('image', '图片', '上传图片文件', 1, {
    controlType: 'IMAGE',
    maxCount: 9,
    maxSizeMb: 5,
    accept: '.jpg,.jpeg,.png,.webp'
  }, false, '上传展示'),
  createPaletteItem('section', '标题/说明', '用于组织表单分区与说明', 1, {
    controlType: 'SECTION',
    content: '请在右侧填写说明内容'
  }, false, '上传展示'),
  createPaletteItem('detail', '明细', '首版暂不开放，后续扩展', 1, { controlType: 'DETAIL' }, true, '上传展示')
]

export const CONTROL_PALETTE_CATEGORIES = [
  '基础输入',
  '选项选择',
  '上传展示'
] as const

export const DOCUMENT_TEMPLATE_TYPE_OPTIONS: DocumentTemplateTypeOption[] = [
  { value: 'report', label: '报销单' },
  { value: 'application', label: '申请单' },
  { value: 'contract', label: '合同单' },
  { value: 'loan', label: '借款单' }
]

export const RELATED_DOCUMENT_ALLOWED_TEMPLATE_TYPES = ['report', 'application', 'contract', 'loan'] as const
export const WRITEOFF_DOCUMENT_ALLOWED_TEMPLATE_TYPES = ['report', 'loan'] as const

export const BUSINESS_COMPONENT_DEFINITIONS: BusinessComponentDefinition[] = [
  {
    code: 'counterparty',
    label: '往来单位',
    description: '从供应商档案选择往来单位，提单时支持现场新增并立即回填。',
    previewFields: ['供应商编码', '往来单位名称', '供应商简称']
  },
  {
    code: 'payee',
    label: '收款人',
    description: '混合选择供应商名称与员工姓名，作为收款主体。',
    previewFields: ['供应商名称', '员工姓名', '来源类型']
  },
  {
    code: 'payee-account',
    label: '收款账户',
    description: '从供应商档案或员工档案读取已维护好的收款账户信息。',
    previewFields: ['开户行', '账户名称', '银行账号']
  },
  {
    code: 'payment-company',
    label: '付款公司',
    description: '从系统维护的公司主数据中选择付款公司，提交时保存绑定的公司记录。',
    previewFields: ['公司名称', '公司编码', '付款主体']
  },
  {
    code: 'undertake-department',
    label: '承担部门',
    description: '用于归集费用归属部门，可设置固定默认部门或提单人所在部门。',
    previewFields: ['费用归属部门', '默认部门', '提单人所在部门']
  },
  {
    code: 'related-document',
    label: '关联单据',
    description: '弹窗选择并关联已审批通过的报销单、申请单、合同单或借款单。',
    previewFields: ['单据编号', '单据标题', '单据状态', '单据类型']
  },
  {
    code: 'writeoff-document',
    label: '核销单据',
    description: '选择借款单或可核销的报销单，并逐条填写本次核销金额。',
    previewFields: ['单据编号', '可核销余额', '核销来源', '核销金额']
  },
  {
    code: 'bank-push-summary',
    label: '银行推送摘要',
    description: '用于后续银企直连接口推送银行摘要字段的单行文本。',
    previewFields: ['银行摘要', '摘要长度控制', '接口预留字段'],
    defaultDisplayLabel: '事由'
  }
]

export function buildBusinessComponentPaletteItems() {
  return BUSINESS_COMPONENT_DEFINITIONS.map<FormDesignerPaletteItem>((definition) => ({
    key: definition.code,
    label: definition.label,
    description: definition.description,
    kind: 'BUSINESS_COMPONENT',
    span: 1,
    props: buildBusinessComponentProps(definition.code)
  }))
}

function buildBusinessComponentProps(componentCode: string) {
  if (componentCode === 'undertake-department') {
    return { componentCode, defaultDeptMode: 'NONE', defaultDeptId: '' }
  }
  if (componentCode === 'related-document') {
    return {
      componentCode,
      allowedTemplateTypes: [...RELATED_DOCUMENT_ALLOWED_TEMPLATE_TYPES]
    }
  }
  if (componentCode === 'writeoff-document') {
    return {
      componentCode,
      allowedTemplateTypes: [...WRITEOFF_DOCUMENT_ALLOWED_TEMPLATE_TYPES]
    }
  }
  return { componentCode }
}

export function normalizeBusinessComponentAllowedTemplateTypes(componentCode: string, rawValue: unknown) {
  const allowedValues = componentCode === 'writeoff-document'
    ? [...WRITEOFF_DOCUMENT_ALLOWED_TEMPLATE_TYPES]
    : [...RELATED_DOCUMENT_ALLOWED_TEMPLATE_TYPES]

  if (!Array.isArray(rawValue) || rawValue.length === 0) {
    return allowedValues
  }

  const normalized = rawValue
    .map((item) => normalizeDocumentTemplateType(item))
    .filter((item, index, list) => allowedValues.includes(item) && list.indexOf(item) === index)

  return normalized.length ? normalized : allowedValues
}

function normalizeDocumentTemplateType(value: unknown) {
  const normalized = typeof value === 'string' ? value.trim() : String(value || '').trim()
  if (normalized === 'application' || normalized === 'contract' || normalized === 'loan') {
    return normalized
  }
  return 'report'
}

export function buildSharedFieldPaletteItems(archives: ProcessCustomArchiveSummary[]) {
  return archives
    .filter((item) => item.status === 1 && item.archiveType === 'SELECT')
    .map<FormDesignerPaletteItem>((item) => ({
      key: item.archiveCode,
      label: item.archiveName,
      description: item.archiveDescription || `${item.itemCount} 个共享选项`,
      kind: 'SHARED_FIELD',
      span: 1,
      props: { archiveCode: item.archiveCode }
    }))
}

export function createEmptyFormSchema(): ProcessFormDesignSchema {
  return {
    layoutMode: 'TWO_COLUMN',
    blocks: []
  }
}

export function createDefaultNewFormSchema(): ProcessFormDesignSchema {
  const bankPushPalette = buildBusinessComponentPaletteItems().find((item) => item.key === 'bank-push-summary')
  const remarkPalette = CONTROL_PALETTE_ITEMS.find((item) => item.key === 'text')
  const blocks: ProcessFormDesignBlock[] = []

  if (bankPushPalette) {
    blocks.push(createBlockFromPaletteItem(bankPushPalette))
  }

  if (remarkPalette) {
    const remarkBlock = createBlockFromPaletteItem(remarkPalette)
    remarkBlock.label = '备注'
    blocks.push(remarkBlock)
  }

  return {
    layoutMode: 'TWO_COLUMN',
    blocks
  }
}

export function normalizeFormSchema(schema?: Partial<ProcessFormDesignSchema> | null): ProcessFormDesignSchema {
  const blocks = Array.isArray(schema?.blocks) ? schema.blocks : []
  return {
    layoutMode: typeof schema?.layoutMode === 'string' && schema.layoutMode ? schema.layoutMode : 'TWO_COLUMN',
    blocks: blocks.map((item, index) => normalizeBlock(item as ProcessFormDesignBlock, index))
  }
}

export function createBlockFromPaletteItem(item: FormDesignerPaletteItem): ProcessFormDesignBlock {
  const fieldSeed = buildFieldSeed(item)
  return normalizeBlock(
    {
      blockId: createUniqueId(`block-${fieldSeed}`),
      fieldKey: createUniqueId(fieldSeed),
      kind: item.kind,
      label: resolveDefaultBlockLabel(item),
      span: item.span ?? 1,
      required: false,
      helpText: '',
      defaultValue: undefined,
      props: cloneValue(item.props),
      permission: createDefaultFieldPermission()
    },
    0
  )
}

export function insertBlockAt(schema: ProcessFormDesignSchema, item: FormDesignerPaletteItem, index: number): ProcessFormDesignSchema {
  const next = normalizeFormSchema(schema)
  const block = createBlockFromPaletteItem(item)
  const targetIndex = clamp(index, 0, next.blocks.length)
  next.blocks.splice(targetIndex, 0, block)
  return next
}

export function moveBlock(schema: ProcessFormDesignSchema, fromIndex: number, toIndex: number): ProcessFormDesignSchema {
  const next = normalizeFormSchema(schema)
  if (fromIndex === toIndex || fromIndex < 0 || fromIndex >= next.blocks.length) {
    return next
  }
  const targetIndex = clamp(toIndex, 0, next.blocks.length - 1)
  const [block] = next.blocks.splice(fromIndex, 1)
  if (!block) {
    return next
  }
  next.blocks.splice(targetIndex, 0, block)
  return next
}

export function moveBlockByOffset(schema: ProcessFormDesignSchema, blockId: string, offset: number): ProcessFormDesignSchema {
  const currentIndex = schema.blocks.findIndex((item) => item.blockId === blockId)
  if (currentIndex === -1) {
    return normalizeFormSchema(schema)
  }
  return moveBlock(schema, currentIndex, currentIndex + offset)
}

export function removeBlock(schema: ProcessFormDesignSchema, blockId: string): ProcessFormDesignSchema {
  const next = normalizeFormSchema(schema)
  next.blocks = next.blocks.filter((item) => item.blockId !== blockId)
  return next
}

export function updateBlock(
  schema: ProcessFormDesignSchema,
  blockId: string,
  updater: (block: ProcessFormDesignBlock) => ProcessFormDesignBlock
): ProcessFormDesignSchema {
  const next = normalizeFormSchema(schema)
  next.blocks = next.blocks.map((item) => (item.blockId === blockId ? normalizeBlock(updater(cloneValue(item)), 0) : item))
  return next
}

export function getBlockQuickActionStates(schema: ProcessFormDesignSchema): Record<string, FormBlockQuickActionState> {
  const next = normalizeFormSchema(schema)
  const actionStates: Record<string, FormBlockQuickActionState> = {}
  let pendingLeftBlockId = ''
  let currentColumn = 1

  next.blocks.forEach((block) => {
    const span = block.span === 2 ? 2 : 1
    if (span === 2) {
      actionStates[block.blockId] = 'collapsible'
      if (currentColumn === 2 && pendingLeftBlockId) {
        actionStates[pendingLeftBlockId] = 'expandable'
      }
      pendingLeftBlockId = ''
      currentColumn = 1
      return
    }

    if (currentColumn === 1) {
      actionStates[block.blockId] = 'hidden'
      pendingLeftBlockId = block.blockId
      currentColumn = 2
      return
    }

    actionStates[block.blockId] = 'hidden'
    pendingLeftBlockId = ''
    currentColumn = 1
  })

  if (currentColumn === 2 && pendingLeftBlockId) {
    actionStates[pendingLeftBlockId] = 'expandable'
  }

  return actionStates
}

export function createDefaultFieldPermission(): ProcessFormFieldPermission {
  return {
    fixedStages: {
      DRAFT_BEFORE_SUBMIT: 'EDITABLE',
      RESUBMIT_AFTER_RETURN: 'EDITABLE',
      IN_APPROVAL: 'READONLY',
      ARCHIVED: 'READONLY'
    },
    sceneOverrides: []
  }
}

export function getBusinessComponentDefinition(componentCode?: string) {
  const normalizedCode = componentCode === 'payee-info' ? 'payee-account' : componentCode
  return BUSINESS_COMPONENT_DEFINITIONS.find((item) => item.code === normalizedCode)
}

export function getBusinessComponentDefaultDisplayLabel(componentCode?: string) {
  return getBusinessComponentDefinition(componentCode)?.defaultDisplayLabel
}

export function getControlType(block: ProcessFormDesignBlock) {
  return String(block.props.controlType || '')
}

export function getSharedArchiveCode(block: ProcessFormDesignBlock) {
  return String(block.props.archiveCode || '')
}

export function getOptionItems(block: ProcessFormDesignBlock): Array<{ label: string; value: string }> {
  return Array.isArray(block.props.options) ? (block.props.options as Array<{ label: string; value: string }>) : []
}

function normalizeBlock(block: ProcessFormDesignBlock, index: number): ProcessFormDesignBlock {
  const props = isRecord(block?.props) ? cloneValue(block.props) : {}
  if (block?.kind === 'BUSINESS_COMPONENT' && props.componentCode === 'payee-info') {
    props.componentCode = 'payee-account'
  }
  return {
    blockId: block?.blockId || createUniqueId(`block-${index + 1}`),
    fieldKey: block?.fieldKey || createUniqueId(`field-${index + 1}`),
    kind: block?.kind || 'CONTROL',
    label: block?.label || '未命名字段',
    span: block?.span === 2 ? 2 : 1,
    helpText: typeof block?.helpText === 'string' ? block.helpText : '',
    required: Boolean(block?.required),
    defaultValue: block?.defaultValue,
    props,
    permission: normalizePermission(block?.permission)
  }
}

function normalizePermission(permission?: ProcessFormFieldPermission | null): ProcessFormFieldPermission {
  const fallback = createDefaultFieldPermission()
  const fixedStages = FORM_PERMISSION_STAGE_OPTIONS.reduce<Record<ProcessFormPermissionStage, ProcessFormPermissionValue>>((result, stage) => {
    const nextValue = permission?.fixedStages?.[stage.key]
    result[stage.key] = isPermissionValue(nextValue) ? nextValue : fallback.fixedStages[stage.key]
    return result
  }, {} as Record<ProcessFormPermissionStage, ProcessFormPermissionValue>)

  const seenSceneIds = new Set<number>()
  const sceneOverrides = Array.isArray(permission?.sceneOverrides)
    ? permission.sceneOverrides
        .filter((item): item is { sceneId: number; permission: ProcessFormPermissionValue } => {
          if (!item || typeof item.sceneId !== 'number' || !Number.isFinite(item.sceneId)) {
            return false
          }
          if (!isPermissionValue(item.permission) || seenSceneIds.has(item.sceneId)) {
            return false
          }
          seenSceneIds.add(item.sceneId)
          return true
        })
        .map((item) => ({ sceneId: item.sceneId, permission: item.permission }))
    : []

  return { fixedStages, sceneOverrides }
}

function createPaletteItem(
  key: string,
  label: string,
  description: string,
  span: number,
  props: Record<string, unknown>,
  disabled = false,
  category?: string
): FormDesignerPaletteItem {
  return { key, label, description, category, kind: 'CONTROL', span, disabled, props }
}

function resolveDefaultBlockLabel(item: FormDesignerPaletteItem) {
  if (item.kind === 'BUSINESS_COMPONENT') {
    const componentCode = String(item.props.componentCode || '')
    return getBusinessComponentDefaultDisplayLabel(componentCode) || item.label
  }
  return item.label
}

function createUniqueId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
}

function buildFieldSeed(item: FormDesignerPaletteItem) {
  return item.key.replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-+|-+$/g, '').toLowerCase() || 'field'
}

function buildDefaultOptions() {
  return [
    { label: '选项 1', value: 'option-1' },
    { label: '选项 2', value: 'option-2' }
  ]
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function isPermissionValue(value: unknown): value is ProcessFormPermissionValue {
  return value === 'EDITABLE' || value === 'READONLY' || value === 'HIDDEN'
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function cloneValue<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}
