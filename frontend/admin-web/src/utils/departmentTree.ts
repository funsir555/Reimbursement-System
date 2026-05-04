export type DepartmentTreeValue = string | number

export type DepartmentTreeValueType = 'string' | 'number' | 'raw'

export interface DepartmentOptionLike {
  value?: DepartmentTreeValue | null
  label?: string | null
  code?: string | null
  name?: string | null
  parentValue?: DepartmentTreeValue | null
  id?: DepartmentTreeValue | null
  deptCode?: string | null
  deptName?: string | null
  parentId?: DepartmentTreeValue | null
  children?: DepartmentOptionLike[] | null
}

export interface DepartmentTreeOption {
  value: DepartmentTreeValue
  label: string
  code?: string
  name?: string
  parentValue?: DepartmentTreeValue
  children: DepartmentTreeOption[]
}

export function normalizeDepartmentValue(
  value: unknown,
  valueType: DepartmentTreeValueType = 'string'
): DepartmentTreeValue | undefined {
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

export function formatDepartmentOptionLabel(option?: DepartmentOptionLike | null) {
  if (!option) return ''
  const code = normalizeText(option.code ?? option.deptCode)
  const name = normalizeText(option.name ?? option.deptName)
  const label = normalizeText(option.label)
  const value = normalizeText(option.value ?? option.id)
  if (code && name) return `${code}  ${name}`
  return name || code || label || value || ''
}

export function buildDepartmentTreeOptions(
  optionList: DepartmentOptionLike[] = [],
  options: {
    valueType?: DepartmentTreeValueType
    filter?: (item: DepartmentOptionLike) => boolean
  } = {}
) {
  const valueType = options.valueType || 'string'
  const flatOptions: DepartmentOptionLike[] = []
  flattenDepartmentOptions(optionList, flatOptions, options.filter)

  const nodeMap = new Map<DepartmentTreeValue, DepartmentTreeOption>()
  const order: DepartmentTreeValue[] = []

  flatOptions.forEach((item) => {
    const value = normalizeDepartmentValue(item.value ?? item.id, valueType)
    if (value === undefined || nodeMap.has(value)) {
      return
    }
    const parentValue = normalizeDepartmentValue(item.parentValue ?? item.parentId, valueType)
    nodeMap.set(value, {
      value,
      label: formatDepartmentOptionLabel(item) || String(value),
      code: normalizeText(item.code ?? item.deptCode),
      name: normalizeText(item.name ?? item.deptName),
      parentValue,
      children: []
    })
    order.push(value)
  })

  const roots: DepartmentTreeOption[] = []
  order.forEach((value) => {
    const node = nodeMap.get(value)
    if (!node) return
    const parentValue = node.parentValue
    if (parentValue !== undefined && parentValue !== node.value) {
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

export function appendMissingDepartmentTreeOptions(
  treeOptions: DepartmentTreeOption[],
  values: unknown[] = [],
  valueType: DepartmentTreeValueType = 'string'
) {
  const existingValues = new Set<DepartmentTreeValue>()
  walkDepartmentTree(treeOptions, (node) => {
    existingValues.add(node.value)
  })

  const missingNodes: DepartmentTreeOption[] = []
  values.forEach((item) => {
    const value = normalizeDepartmentValue(item, valueType)
    if (value === undefined || existingValues.has(value)) {
      return
    }
    existingValues.add(value)
    missingNodes.push({
      value,
      label: String(value),
      children: []
    })
  })

  return missingNodes.length ? [...treeOptions, ...missingNodes] : treeOptions
}

export function filterDepartmentTreeNode(query: string, data?: Partial<DepartmentTreeOption>) {
  const keyword = normalizeText(query)?.toLowerCase()
  if (!keyword) return true
  return [data?.label, data?.code, data?.name, data?.value]
    .map((item) => normalizeText(item))
    .filter((item): item is string => Boolean(item))
    .some((item) => item.toLowerCase().includes(keyword))
}

export function buildDepartmentLabelMap(
  options: DepartmentOptionLike[] | DepartmentTreeOption[] = [],
  valueType: DepartmentTreeValueType = 'string'
) {
  const treeOptions = buildDepartmentTreeOptions(options, { valueType })
  const labelMap = new Map<DepartmentTreeValue, string>()
  walkDepartmentTree(treeOptions, (node) => {
    labelMap.set(node.value, node.label)
  })
  return labelMap
}

function flattenDepartmentOptions(
  source: DepartmentOptionLike[],
  target: DepartmentOptionLike[],
  filter?: (item: DepartmentOptionLike) => boolean
) {
  source.forEach((item) => {
    if (!filter || filter(item)) {
      target.push(item)
    }
    if (item.children?.length) {
      flattenDepartmentOptions(item.children, target, filter)
    }
  })
}

function walkDepartmentTree(nodes: DepartmentTreeOption[], visitor: (node: DepartmentTreeOption) => void) {
  nodes.forEach((node) => {
    visitor(node)
    if (node.children.length) {
      walkDepartmentTree(node.children, visitor)
    }
  })
}

function normalizeText(value?: unknown) {
  const text = String(value ?? '').trim()
  return text || undefined
}
