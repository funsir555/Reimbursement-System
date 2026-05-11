import { formatFinanceAssistOptionLabel, normalizeFinanceAssistText, type FinanceAssistOptionLike } from './financeAssistOptions'

export interface SubjectOptionLike extends FinanceAssistOptionLike {
  subjectCategory?: string | null
  subjectCategoryLabel?: string | null
}

export interface SubjectTreeOption {
  value: string
  label: string
  code?: string
  name?: string
  parentValue?: string
  subjectCategory?: string
  subjectCategoryLabel?: string
  disabled?: boolean
  children: SubjectTreeOption[]
}

const CATEGORY_ORDER = ['ASSET', 'LIABILITY', 'EQUITY', 'COST', 'PROFIT'] as const
const CATEGORY_LABELS: Record<string, string> = {
  ASSET: '资产',
  LIABILITY: '负债',
  EQUITY: '权益',
  COST: '成本',
  PROFIT: '损益'
}
const CATEGORY_NODE_PREFIX = '__subject-category__'
const CURRENT_VOUCHER_CATEGORY_VALUE = '__subject-category__CURRENT'
const CURRENT_VOUCHER_CATEGORY_LABEL = '当前凭证科目'

export function buildSubjectTreeOptions(options: SubjectOptionLike[] = []) {
  const flatOptions: SubjectOptionLike[] = []
  flattenSubjectOptions(options, flatOptions)

  const nodeMap = new Map<string, SubjectTreeOption>()
  const categoryBuckets = new Map<string, string[]>()
  const order: string[] = []

  flatOptions.forEach((item) => {
    const value = normalizeFinanceAssistText(item.value)
    if (!value || nodeMap.has(value)) {
      return
    }
    const categoryValue = normalizeSubjectCategory(item.subjectCategory)
    const categoryLabel = resolveSubjectCategoryLabel(item.subjectCategoryLabel, categoryValue)
    nodeMap.set(value, {
      value,
      label: formatFinanceAssistOptionLabel(item) || value,
      code: normalizeFinanceAssistText(item.code),
      name: normalizeFinanceAssistText(item.name),
      parentValue: normalizeFinanceAssistText(item.parentValue),
      subjectCategory: categoryValue,
      subjectCategoryLabel: categoryLabel,
      children: []
    })
    if (!categoryBuckets.has(categoryValue)) {
      categoryBuckets.set(categoryValue, [])
    }
    categoryBuckets.get(categoryValue)?.push(value)
    order.push(value)
  })

  const categoryRoots = new Map<string, SubjectTreeOption[]>()
  order.forEach((value) => {
    const node = nodeMap.get(value)
    if (!node) {
      return
    }
    const categoryValue = node.subjectCategory || CURRENT_VOUCHER_CATEGORY_VALUE
    if (!categoryRoots.has(categoryValue)) {
      categoryRoots.set(categoryValue, [])
    }
    const parentValue = node.parentValue
    if (parentValue && parentValue !== node.value) {
      const parentNode = nodeMap.get(parentValue)
      if (parentNode && parentNode.subjectCategory === node.subjectCategory) {
        parentNode.children.push(node)
        return
      }
    }
    categoryRoots.get(categoryValue)?.push(node)
  })

  const orderedCategories = [
    ...CATEGORY_ORDER,
    ...Array.from(categoryRoots.keys()).filter((value) => !CATEGORY_ORDER.includes(value as typeof CATEGORY_ORDER[number]))
  ]

  const categoryNodes: SubjectTreeOption[] = []
  orderedCategories.forEach((categoryValue) => {
    const children = categoryRoots.get(categoryValue)
    if (!children?.length) {
      return
    }
    categoryNodes.push({
      value: `${CATEGORY_NODE_PREFIX}${categoryValue}`,
      label: resolveSubjectCategoryLabel(undefined, categoryValue),
      subjectCategory: categoryValue,
      subjectCategoryLabel: resolveSubjectCategoryLabel(undefined, categoryValue),
      disabled: true,
      children
    })
  })

  return categoryNodes
}

export function filterSubjectTreeNode(query: string, data?: Partial<SubjectTreeOption>) {
  const keyword = normalizeFinanceAssistText(query)?.toLowerCase()
  if (!keyword) {
    return true
  }
  return [
    data?.label,
    data?.code,
    data?.name,
    data?.value,
    data?.subjectCategory,
    data?.subjectCategoryLabel
  ]
    .map((item) => normalizeFinanceAssistText(item))
    .filter((item): item is string => Boolean(item))
    .some((item) => item.toLowerCase().includes(keyword))
}

export function flattenSubjectTreeOptions(nodes: SubjectTreeOption[]) {
  const result: SubjectTreeOption[] = []
  const walk = (items: SubjectTreeOption[]) => {
    items.forEach((item) => {
      result.push(item)
      if (item.children.length) {
        walk(item.children)
      }
    })
  }
  walk(nodes)
  return result
}

function flattenSubjectOptions(source: SubjectOptionLike[], target: SubjectOptionLike[]) {
  source.forEach((item) => {
    target.push(item)
  })
}

function normalizeSubjectCategory(value?: string | null) {
  return normalizeFinanceAssistText(value) || CURRENT_VOUCHER_CATEGORY_VALUE
}

function resolveSubjectCategoryLabel(subjectCategoryLabel?: string | null, subjectCategory?: string | null) {
  const explicitLabel = normalizeFinanceAssistText(subjectCategoryLabel)
  if (explicitLabel) {
    return explicitLabel
  }
  const normalizedCategory = normalizeFinanceAssistText(subjectCategory)
  if (!normalizedCategory) {
    return CURRENT_VOUCHER_CATEGORY_LABEL
  }
  if (normalizedCategory === CURRENT_VOUCHER_CATEGORY_VALUE) {
    return CURRENT_VOUCHER_CATEGORY_LABEL
  }
  return CATEGORY_LABELS[normalizedCategory] || normalizedCategory
}
