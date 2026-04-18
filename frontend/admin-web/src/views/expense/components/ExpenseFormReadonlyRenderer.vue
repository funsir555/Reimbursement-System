<template>
  <div class="space-y-6">
    <div v-if="summaryVisible" class="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">
      <div class="grid grid-cols-1 gap-4 text-sm text-slate-600 xl:grid-cols-3">
        <div>表单组件数：{{ blocks.length }}</div>
        <div>布局模式：{{ schema?.layoutMode || 'TWO_COLUMN' }}</div>
        <div>当前视图：只读详情</div>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
      <div
        v-for="block in visibleBlocks"
        :key="block.blockId"
        class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm"
        :class="block.span === 2 ? 'md:col-span-2' : ''"
      >
        <template v-if="controlType(block) === 'SECTION'">
          <p class="text-lg font-semibold text-slate-800">{{ block.label }}</p>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-500">
            {{ String(block.props.content || block.helpText || '') || '暂无内容' }}
          </p>
        </template>

        <template v-else>
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-sm font-semibold text-slate-800">{{ block.label }}</p>
              <p v-if="block.helpText" class="mt-1 text-xs leading-6 text-slate-400">{{ block.helpText }}</p>
            </div>
            <el-tag v-if="block.required" size="small" type="danger" effect="plain">必填</el-tag>
          </div>

          <div
            v-if="isRelatedDocumentBlock(block) && relatedDocumentItems(block).length"
            class="mt-4 space-y-3"
          >
            <div
              v-for="item in relatedDocumentItems(block)"
              :key="`${block.blockId}-${item.documentCode}`"
              class="rounded-[24px] border border-slate-200 bg-slate-50 px-4 py-4"
            >
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="break-all text-sm font-semibold text-slate-800">
                    {{ item.documentTitle || item.documentCode }}
                  </p>
                  <p class="mt-1 break-all text-xs text-slate-500">单据编号：{{ item.documentCode }}</p>
                  <p class="mt-1 text-xs text-slate-500">
                    类型：{{ item.templateTypeLabel || resolveTemplateTypeLabel(item.templateType) }}
                  </p>
                </div>
                <el-tag size="small" effect="plain" type="info">{{ item.statusLabel || item.status || '已关联' }}</el-tag>
              </div>
            </div>
          </div>
          <div
            v-else-if="isWriteOffDocumentBlock(block) && writeOffDocumentItems(block).length"
            class="mt-4 space-y-3"
          >
            <div
              v-for="item in writeOffDocumentItems(block)"
              :key="`${block.blockId}-${item.documentCode}`"
              class="rounded-[24px] border border-slate-200 bg-slate-50 px-4 py-4"
            >
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="break-all text-sm font-semibold text-slate-800">
                    {{ item.documentTitle || item.documentCode }}
                  </p>
                  <p class="mt-1 break-all text-xs text-slate-500">单据编号：{{ item.documentCode }}</p>
                  <p class="mt-1 text-xs text-slate-500">
                    类型：{{ item.templateTypeLabel || resolveTemplateTypeLabel(item.templateType) }}
                  </p>
                </div>
                <el-tag size="small" effect="plain" type="warning">{{ writeOffSourceKindLabel(item.writeOffSourceKind) }}</el-tag>
              </div>

              <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
                <div class="rounded-2xl border border-white/90 bg-white/90 px-4 py-3">
                  <p class="text-xs text-slate-400">可核销余额</p>
                  <p class="mt-2 text-sm font-semibold text-slate-800">{{ formatAmount(item.availableWriteOffAmount) }}</p>
                </div>
                <div class="rounded-2xl border border-white/90 bg-white/90 px-4 py-3">
                  <p class="text-xs text-slate-400">核销金额</p>
                  <p class="mt-2 text-sm font-semibold text-slate-800">{{ formatAmount(item.writeOffAmount) }}</p>
                </div>
                <div class="rounded-2xl border border-white/90 bg-white/90 px-4 py-3">
                  <p class="text-xs text-slate-400">核销后余额</p>
                  <p class="mt-2 text-sm font-semibold text-slate-800">{{ formatAmount(item.remainingAmount) }}</p>
                </div>
              </div>
            </div>
          </div>
          <div
            v-else-if="isPayeeAccountBlock(block) && resolvePayeeAccountCard(block)"
            class="mt-4 rounded-[24px] border border-slate-200 bg-gradient-to-br from-slate-50 via-white to-sky-50 px-5 py-4 shadow-sm"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">收款账户</p>
                <p class="mt-1 text-xs leading-6 text-slate-500">已按档案信息解析为脱敏收款账户展示。</p>
              </div>
              <el-tag size="small" effect="plain" type="info">已脱敏</el-tag>
            </div>

            <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
              <div class="rounded-2xl border border-white/90 bg-white/90 px-4 py-3">
                <p class="text-xs text-slate-400">账户名</p>
                <p class="mt-2 break-words text-sm font-semibold text-slate-800">
                  {{ resolvePayeeAccountCard(block)?.ownerName || '-' }}
                </p>
              </div>
              <div class="rounded-2xl border border-white/90 bg-white/90 px-4 py-3">
                <p class="text-xs text-slate-400">银行账号</p>
                <p class="mt-2 break-all text-sm font-semibold text-slate-800">
                  {{ resolvePayeeAccountCard(block)?.accountNoMasked || '-' }}
                </p>
              </div>
              <div class="rounded-2xl border border-white/90 bg-white/90 px-4 py-3">
                <p class="text-xs text-slate-400">开户行</p>
                <p class="mt-2 break-words text-sm font-semibold text-slate-800">
                  {{ resolvePayeeAccountCard(block)?.bankName || '-' }}
                </p>
              </div>
            </div>
          </div>
          <div v-else class="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-3">
            <template v-if="displayLines(block).length">
              <p
                v-for="(line, index) in displayLines(block)"
                :key="`${block.blockId}-${index}`"
                class="break-words text-sm leading-7 text-slate-700"
              >
                {{ line }}
              </p>
            </template>
            <p v-else class="text-sm leading-7 text-slate-400">未填写</p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  ExpenseCreateVendorOption,
  ExpenseRelatedDocumentValue,
  ExpenseCreatePayeeAccountOption,
  ExpenseCreatePayeeOption,
  ExpenseWriteOffDocumentValue,
  ProcessFormDesignBlock,
  ProcessFormDesignSchema,
  ProcessFormOption
} from '@/api'
import {
  getBusinessComponentDefinition,
  getControlType,
  getOptionItems
} from '@/views/process/formDesignerHelper'
import { isExpenseDetailBlockVisible } from '@/views/expense/expenseDetailRuntime'
import { formatMoney, normalizeMoneyValue } from '@/utils/money'

type PayeeAccountCard = {
  ownerName: string
  accountNoMasked: string
  bankName: string
}

const props = withDefaults(defineProps<{
  schema: ProcessFormDesignSchema
  formData: Record<string, unknown>
  companyOptions?: ProcessFormOption[]
  departmentOptions?: ProcessFormOption[]
  detailType?: string
  defaultBusinessScenario?: string
  summaryVisible?: boolean
  vendorOptionMap?: Record<string, ExpenseCreateVendorOption>
  payeeOptionMap?: Record<string, ExpenseCreatePayeeOption>
  payeeAccountOptionMap?: Record<string, ExpenseCreatePayeeAccountOption>
}>(), {
  companyOptions: () => [],
  departmentOptions: () => [],
  detailType: '',
  defaultBusinessScenario: '',
  summaryVisible: true,
  vendorOptionMap: () => ({}),
  payeeOptionMap: () => ({}),
  payeeAccountOptionMap: () => ({})
})

const blocks = computed(() => props.schema?.blocks || [])
const visibleBlocks = computed(() => (
  props.detailType
    ? blocks.value.filter((block) => (
      isExpenseDetailBlockVisible(block, props.formData || {}, props.detailType, props.defaultBusinessScenario)
    ))
    : blocks.value
))
const companyMap = computed(() => new Map((props.companyOptions || []).map((item) => [item.value, item.label])))
const departmentMap = computed(() => new Map((props.departmentOptions || []).map((item) => [item.value, item.label])))
const firstResolvedPayeeLabel = computed(() => {
  for (const block of blocks.value) {
    if (!isPayeeBlock(block)) {
      continue
    }
    const label = trimToNull(resolvePayeeLabel(props.formData?.[block.fieldKey]))
    if (label) {
      return label
    }
  }
  return ''
})

function controlType(block: ProcessFormDesignBlock) {
  return getControlType(block)
}

function businessCode(block: ProcessFormDesignBlock) {
  return getBusinessComponentDefinition(String(block.props.componentCode || ''))?.code || String(block.props.componentCode || '')
}

function isPayeeBlock(block: ProcessFormDesignBlock) {
  return block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'payee'
}

function isRelatedDocumentBlock(block: ProcessFormDesignBlock) {
  return block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'related-document'
}

function isWriteOffDocumentBlock(block: ProcessFormDesignBlock) {
  return block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'writeoff-document'
}

function isPayeeAccountBlock(block: ProcessFormDesignBlock) {
  return block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'payee-account'
}

function relatedDocumentItems(block: ProcessFormDesignBlock): ExpenseRelatedDocumentValue[] {
  return normalizeRelatedDocumentValues(props.formData?.[block.fieldKey])
}

function writeOffDocumentItems(block: ProcessFormDesignBlock): ExpenseWriteOffDocumentValue[] {
  return normalizeWriteOffDocumentValues(props.formData?.[block.fieldKey])
}

function displayLines(block: ProcessFormDesignBlock) {
  const rawValue = props.formData?.[block.fieldKey]

  if (block.kind === 'CONTROL') {
    return controlDisplayLines(block, rawValue)
  }

  if (block.kind === 'BUSINESS_COMPONENT') {
    return businessDisplayLines(block, rawValue)
  }

  return normalizeLines(rawValue)
}

function businessDisplayLines(block: ProcessFormDesignBlock, rawValue: unknown) {
  if (businessCode(block) === 'payment-company') {
    return normalizeLines(rawValue).map((item) => companyMap.value.get(item) || item)
  }
  if (businessCode(block) === 'undertake-department') {
    return normalizeLines(rawValue).map((item) => departmentMap.value.get(item) || item)
  }
  if (businessCode(block) === 'counterparty') {
    const label = trimToNull(resolveVendorLabel(rawValue))
    return label ? [label] : []
  }
  if (isRelatedDocumentBlock(block) || isWriteOffDocumentBlock(block)) {
    return []
  }
  if (isPayeeBlock(block)) {
    const label = trimToNull(resolvePayeeLabel(rawValue))
    return label ? [label] : []
  }
  return normalizeLines(rawValue)
}

function controlDisplayLines(block: ProcessFormDesignBlock, rawValue: unknown) {
  const type = controlType(block)
  if (['SELECT', 'MULTI_SELECT', 'RADIO', 'CHECKBOX'].includes(type)) {
    const optionMap = new Map(getOptionItems(block).map((item) => [String(item.value), item.label]))
    return normalizeLines(rawValue).map((item) => optionMap.get(item) || item)
  }
  if (type === 'SWITCH') {
    return [rawValue ? '已开启' : '已关闭']
  }
  if (type === 'DATE_RANGE' && Array.isArray(rawValue)) {
    return rawValue.map((item) => String(item))
  }
  return normalizeLines(rawValue)
}

function resolvePayeeAccountCard(block: ProcessFormDesignBlock): PayeeAccountCard | null {
  const rawValue = props.formData?.[block.fieldKey]
  const option = resolvePayeeAccountOption(rawValue)
  if (option) {
    return {
      ownerName: firstNonBlank(option.ownerName, firstResolvedPayeeLabel.value, option.accountName, formatValue(rawValue)) || '-',
      accountNoMasked: firstNonBlank(option.accountNoMasked) || '-',
      bankName: firstNonBlank(option.bankName) || '-'
    }
  }

  if (isRecord(rawValue)) {
    const ownerName = firstNonBlank(
      rawValue.ownerName,
      firstResolvedPayeeLabel.value,
      rawValue.accountName,
      rawValue.label,
      rawValue.value
    )
    const accountNoMasked = firstNonBlank(rawValue.accountNoMasked)
    const bankName = firstNonBlank(rawValue.bankName)
    if (ownerName || accountNoMasked || bankName) {
      return {
        ownerName: ownerName || '-',
        accountNoMasked: accountNoMasked || '-',
        bankName: bankName || '-'
      }
    }
  }

  return null
}

function normalizeRelatedDocumentValues(value: unknown): ExpenseRelatedDocumentValue[] {
  return normalizeDocumentValueList(value)
    .map((item) => ({
      documentCode: firstNonBlank(item.documentCode, item.value) || '',
      documentTitle: firstNonBlank(item.documentTitle, item.label) || undefined,
      templateType: firstNonBlank(item.templateType) || undefined,
      templateTypeLabel: firstNonBlank(item.templateTypeLabel) || undefined,
      templateName: firstNonBlank(item.templateName) || undefined,
      status: firstNonBlank(item.status) || undefined,
      statusLabel: firstNonBlank(item.statusLabel) || undefined
    }))
    .filter((item) => item.documentCode)
}

function normalizeWriteOffDocumentValues(value: unknown): ExpenseWriteOffDocumentValue[] {
  return normalizeDocumentValueList(value)
    .map((item) => {
      const [base] = normalizeRelatedDocumentValues(item)
      return {
        ...(base || { documentCode: '' }),
        writeOffSourceKind: firstNonBlank(item.writeOffSourceKind) || undefined,
        availableWriteOffAmount: toOptionalMoney(item.availableWriteOffAmount),
        writeOffAmount: toOptionalMoney(item.writeOffAmount),
        remainingAmount: toOptionalMoney(item.remainingAmount)
      }
    })
    .filter((item) => Boolean(item?.documentCode)) as ExpenseWriteOffDocumentValue[]
}

function normalizeDocumentValueList(value: unknown): Record<string, unknown>[] {
  if (Array.isArray(value)) {
    return value.flatMap((item) => normalizeDocumentValueList(item))
  }
  if (isRecord(value)) {
    return [value]
  }
  return []
}

function resolveTemplateTypeLabel(templateType?: string) {
  if (templateType === 'application') return '申请单'
  if (templateType === 'contract') return '合同单'
  if (templateType === 'loan') return '借款单'
  return '报销单'
}

function writeOffSourceKindLabel(value?: string) {
  if (value === 'LOAN') return '借款单'
  if (value === 'PREPAY_REPORT') return '预付报销单'
  return '待识别'
}

function formatAmount(value: unknown) {
  const amount = toOptionalMoney(value)
  if (amount === undefined) {
    return '--'
  }
  return formatMoney(amount)
}

function resolveVendorLabel(rawValue: unknown) {
  const option = resolveVendorOption(rawValue)
  return option?.label || formatValue(rawValue)
}

function resolveVendorOption(rawValue: unknown) {
  const key = resolveLookupKey(rawValue)
  return key ? props.vendorOptionMap[key] : undefined
}

function resolvePayeeLabel(rawValue: unknown) {
  const option = resolvePayeeOption(rawValue)
  return option?.label || formatValue(rawValue)
}

function resolvePayeeOption(rawValue: unknown) {
  const key = resolveLookupKey(rawValue)
  return key ? props.payeeOptionMap[key] : undefined
}

function resolvePayeeAccountOption(rawValue: unknown) {
  const key = resolveLookupKey(rawValue)
  return key ? props.payeeAccountOptionMap[key] : undefined
}

function resolveLookupKey(rawValue: unknown) {
  if (typeof rawValue === 'string') {
    return trimToNull(rawValue)
  }
  if (isRecord(rawValue)) {
    return firstNonBlank(rawValue.value, rawValue.code, rawValue.id, rawValue.label)
  }
  return null
}

function normalizeLines(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value
      .map((item) => formatValue(item))
      .filter((item) => item.trim().length > 0)
  }
  const text = formatValue(value)
  return text ? text.split('\n').filter((item) => item.trim().length > 0) : []
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'object') {
    if ('label' in (value as Record<string, unknown>) && typeof (value as Record<string, unknown>).label === 'string') {
      return String((value as Record<string, unknown>).label)
    }
    if ('value' in (value as Record<string, unknown>) && typeof (value as Record<string, unknown>).value === 'string') {
      return String((value as Record<string, unknown>).value)
    }
    return JSON.stringify(value)
  }
  return String(value)
}

function firstNonBlank(...values: unknown[]) {
  for (const value of values) {
    const text = trimToNull(typeof value === 'string' ? value : value == null ? null : String(value))
    if (text) {
      return text
    }
  }
  return null
}

function toOptionalMoney(value: unknown) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return normalizeMoneyValue(String(value))
  }
  if (typeof value === 'string' && value.trim()) {
    return normalizeMoneyValue(value)
  }
  return undefined
}

function trimToNull(value: string | null | undefined) {
  if (value == null) {
    return null
  }
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
</script>
