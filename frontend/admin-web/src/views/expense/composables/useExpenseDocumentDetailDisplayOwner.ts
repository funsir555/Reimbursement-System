import { computed, type ComputedRef, type Ref } from 'vue'
import type {
  ExpenseCreatePayeeAccountOption,
  ExpenseCreatePayeeOption,
  ExpenseCreateVendorOption,
  ExpenseDetailInstanceDetail,
  ExpenseDocumentDetail,
  ProcessFormDesignSchema
} from '@/api'
import type {
  ExpenseDetailInstanceSummary,
  ExpenseDocumentRelationBinding,
  ExpenseDocumentWriteOffBinding
} from '@/api/modules/expense-types'

type VendorOptionMap = Record<string, ExpenseCreateVendorOption>
type PayeeOptionMap = Record<string, ExpenseCreatePayeeOption>
type PayeeAccountOptionMap = Record<string, ExpenseCreatePayeeAccountOption>

export type BindingPanelItem = {
  key: string
  documentCode: string
  title: string
  templateTypeLabel: string
  statusLabel?: string
  metaLine: string
  detailLine: string
}

export type BindingPanelSection = {
  key: string
  title: string
  count: number
  emptyText: string
  itemTestId: string
  items: BindingPanelItem[]
}

export type BindingPanelDisplay = {
  key: string
  title: string
  description: string
  count: number
  cardTestId: string
  toggleTestId: string
  expanded: boolean
  toggleText: string
  sections: BindingPanelSection[]
  toggle: () => void
}

export type SummaryItem = {
  key: string
  label: string
  value: string
}

export type ExpenseDetailCardDisplay = {
  detailNo: string
  title: string
  typeLabel: string
  enterpriseModeLabel: string
  isSelected: boolean
  metaLine: string
}

export type BankReceiptDisplay = {
  key: string
  fileName: string
  receivedAt: string
  metaLine: string
  previewHref: string
}

type UseExpenseDocumentDetailDisplayOwnerOptions = {
  detail: Ref<ExpenseDocumentDetail | null>
  vendorOptionMap: Ref<VendorOptionMap>
  payeeOptionMap: Ref<PayeeOptionMap>
  payeeAccountOptionMap: Ref<PayeeAccountOptionMap>
  relatedBindingsExpanded: Ref<boolean>
  writeOffBindingsExpanded: Ref<boolean>
  activeExpenseDetailNo: Ref<string>
  expenseDetailLoadingNo: Ref<string>
  activeExpenseDetail: ComputedRef<ExpenseDetailInstanceDetail | null>
  activeExpenseDetailSummary: ComputedRef<ExpenseDetailInstanceSummary | null>
  activeExpenseDetailError: ComputedRef<string>
  relatedDocumentBindings: ComputedRef<ExpenseDocumentRelationBinding[]>
  outboundRelatedBindings: ComputedRef<ExpenseDocumentRelationBinding[]>
  inboundRelatedBindings: ComputedRef<ExpenseDocumentRelationBinding[]>
  writeOffDocumentBindings: ComputedRef<ExpenseDocumentWriteOffBinding[]>
  outboundWriteOffBindings: ComputedRef<ExpenseDocumentWriteOffBinding[]>
  inboundWriteOffBindings: ComputedRef<ExpenseDocumentWriteOffBinding[]>
  bindingCountSuffix: string
  bindingInlineSeparator: string
  expandText: string
  collapseText: string
  businessDocumentLabel: string
  relatedCardTitle: string
  relatedCardDescription: string
  relatedOutboundTitle: string
  relatedInboundTitle: string
  writeOffCardTitle: string
  writeOffCardDescription: string
  writeOffOutboundTitle: string
  writeOffInboundTitle: string
  documentCodeLabel: string
  submitterLabel: string
  sourceFieldLabel: string
  bindingFieldLabel: string
  writeOffSourceLabel: string
  requestedAmountLabel: string
  effectiveAmountLabel: string
  remainingAmountLabel: string
  unknownStatusLabel: string
  relatedOutboundEmptyText: string
  relatedInboundEmptyText: string
  writeOffOutboundEmptyText: string
  writeOffInboundEmptyText: string
  emptyExpenseDetailSchema: ProcessFormDesignSchema
  resolveExpenseDetailTypeLabel: (detailType?: string, fallback?: string) => string
  formatBindingMoney: (value: unknown) => string
  writeOffSourceKindLabel: (kind?: string) => string
  formatAttachmentSize: (value?: number) => string
  buildAuthorizedAttachmentPreviewUrl: (url: string) => string
}

export function useExpenseDocumentDetailDisplayOwner(
  options: UseExpenseDocumentDetailDisplayOwnerOptions
) {
  const readonlyFormDisplay = computed(() => {
    if (!options.detail.value) {
      return null
    }
    return {
      schema: options.detail.value.formSchemaSnapshot,
      formData: options.detail.value.formData,
      companyOptions: options.detail.value.companyOptions,
      departmentOptions: options.detail.value.departmentOptions,
      vendorOptionMap: options.vendorOptionMap.value,
      payeeOptionMap: options.payeeOptionMap.value,
      payeeAccountOptionMap: options.payeeAccountOptionMap.value
    }
  })

  const bindingPanels = computed<BindingPanelDisplay[]>(() => [
    {
      key: 'related',
      title: options.relatedCardTitle,
      description: options.relatedCardDescription,
      count: options.relatedDocumentBindings.value.length,
      cardTestId: 'related-bindings-card',
      toggleTestId: 'related-bindings-toggle',
      expanded: options.relatedBindingsExpanded.value,
      toggleText: options.relatedBindingsExpanded.value ? options.collapseText : options.expandText,
      sections: [
        {
          key: 'related-outbound',
          title: options.relatedOutboundTitle,
          count: options.outboundRelatedBindings.value.length,
          emptyText: options.relatedOutboundEmptyText,
          itemTestId: 'related-binding-item',
          items: options.outboundRelatedBindings.value.map((item) =>
            buildRelatedBindingItem(item, {
              keyPrefix: 'related-outbound',
              fieldLabel: options.sourceFieldLabel,
              businessDocumentLabel: options.businessDocumentLabel,
              documentCodeLabel: options.documentCodeLabel,
              submitterLabel: options.submitterLabel,
              bindingInlineSeparator: options.bindingInlineSeparator
            })
          )
        },
        {
          key: 'related-inbound',
          title: options.relatedInboundTitle,
          count: options.inboundRelatedBindings.value.length,
          emptyText: options.relatedInboundEmptyText,
          itemTestId: 'related-binding-item',
          items: options.inboundRelatedBindings.value.map((item) =>
            buildRelatedBindingItem(item, {
              keyPrefix: 'related-inbound',
              fieldLabel: options.bindingFieldLabel,
              businessDocumentLabel: options.businessDocumentLabel,
              documentCodeLabel: options.documentCodeLabel,
              submitterLabel: options.submitterLabel,
              bindingInlineSeparator: options.bindingInlineSeparator
            })
          )
        }
      ],
      toggle: () => {
        options.relatedBindingsExpanded.value = !options.relatedBindingsExpanded.value
      }
    },
    {
      key: 'writeoff',
      title: options.writeOffCardTitle,
      description: options.writeOffCardDescription,
      count: options.writeOffDocumentBindings.value.length,
      cardTestId: 'writeoff-bindings-card',
      toggleTestId: 'writeoff-bindings-toggle',
      expanded: options.writeOffBindingsExpanded.value,
      toggleText: options.writeOffBindingsExpanded.value ? options.collapseText : options.expandText,
      sections: [
        {
          key: 'writeoff-outbound',
          title: options.writeOffOutboundTitle,
          count: options.outboundWriteOffBindings.value.length,
          emptyText: options.writeOffOutboundEmptyText,
          itemTestId: 'writeoff-binding-item',
          items: options.outboundWriteOffBindings.value.map((item) =>
            buildWriteOffBindingItem(item, {
              keyPrefix: 'writeoff-outbound',
              businessDocumentLabel: options.businessDocumentLabel,
              unknownStatusLabel: options.unknownStatusLabel,
              documentCodeLabel: options.documentCodeLabel,
              writeOffSourceLabel: options.writeOffSourceLabel,
              bindingInlineSeparator: options.bindingInlineSeparator,
              requestedAmountLabel: options.requestedAmountLabel,
              effectiveAmountLabel: options.effectiveAmountLabel,
              remainingAmountLabel: options.remainingAmountLabel,
              writeOffSourceKindLabel: options.writeOffSourceKindLabel,
              formatBindingMoney: options.formatBindingMoney
            })
          )
        },
        {
          key: 'writeoff-inbound',
          title: options.writeOffInboundTitle,
          count: options.inboundWriteOffBindings.value.length,
          emptyText: options.writeOffInboundEmptyText,
          itemTestId: 'writeoff-binding-item',
          items: options.inboundWriteOffBindings.value.map((item) =>
            buildWriteOffBindingItem(item, {
              keyPrefix: 'writeoff-inbound',
              businessDocumentLabel: options.businessDocumentLabel,
              unknownStatusLabel: options.unknownStatusLabel,
              documentCodeLabel: options.documentCodeLabel,
              writeOffSourceLabel: options.writeOffSourceLabel,
              bindingInlineSeparator: options.bindingInlineSeparator,
              requestedAmountLabel: options.requestedAmountLabel,
              effectiveAmountLabel: options.effectiveAmountLabel,
              remainingAmountLabel: options.remainingAmountLabel,
              writeOffSourceKindLabel: options.writeOffSourceKindLabel,
              formatBindingMoney: options.formatBindingMoney
            })
          )
        }
      ],
      toggle: () => {
        options.writeOffBindingsExpanded.value = !options.writeOffBindingsExpanded.value
      }
    }
  ])

  const expenseDetailCards = computed<ExpenseDetailCardDisplay[]>(() =>
    (options.detail.value?.expenseDetails || []).map((item) => ({
      detailNo: item.detailNo,
      title: item.detailTitle || item.detailNo,
      typeLabel: options.resolveExpenseDetailTypeLabel(item.detailType, item.detailTypeLabel),
      enterpriseModeLabel: item.enterpriseModeLabel || '',
      isSelected: options.activeExpenseDetailNo.value === item.detailNo,
      metaLine: `明细编号：${item.detailNo} ｜ 排序：${item.sortOrder || '-'} ｜ 创建时间：${item.createdAt || '-'}`
    }))
  )

  const expenseDetailWorkspaceVisible = computed(() => Boolean(options.activeExpenseDetailNo.value))
  const expenseDetailSummaryItems = computed<SummaryItem[]>(() => [
    {
      key: 'current-detail',
      label: '当前明细',
      value:
        options.activeExpenseDetail.value?.detailTitle
        || options.activeExpenseDetailSummary.value?.detailTitle
        || options.activeExpenseDetailNo.value
        || '-'
    },
    {
      key: 'detail-no',
      label: '明细编号',
      value: options.activeExpenseDetail.value?.detailNo || options.activeExpenseDetailNo.value || '-'
    },
    {
      key: 'load-status',
      label: '加载状态',
      value:
        options.expenseDetailLoadingNo.value === options.activeExpenseDetailNo.value
          && !options.activeExpenseDetail.value
          ? '加载中'
          : options.activeExpenseDetailError.value
            ? '加载失败'
            : '已就绪'
    }
  ])

  const expenseDetailWorkbenchDisplay = computed(() => ({
    schema: options.activeExpenseDetail.value?.schemaSnapshot || options.emptyExpenseDetailSchema,
    formData: options.activeExpenseDetail.value?.formData || {},
    detailTitle:
      options.activeExpenseDetail.value?.detailTitle
      || options.activeExpenseDetailSummary.value?.detailTitle
      || '',
    detailNo: options.activeExpenseDetail.value?.detailNo || options.activeExpenseDetailNo.value,
    loading:
      options.expenseDetailLoadingNo.value === options.activeExpenseDetailNo.value
      && !options.activeExpenseDetail.value,
    errorMessage: options.activeExpenseDetailError.value
  }))

  const bankSectionVisible = computed(
    () => Boolean(options.detail.value?.bankPayment || options.detail.value?.bankReceipts?.length)
  )

  const bankPaymentSummaryItems = computed<SummaryItem[]>(() => {
    const payment = options.detail.value?.bankPayment
    if (!payment) {
      return []
    }
    return [
      { key: 'payment-status', label: '付款状态', value: payment.paymentStatusLabel || '-' },
      { key: 'account-name', label: '直连账户', value: payment.companyBankAccountName || '-' },
      { key: 'receipt-status', label: '回单状态', value: payment.receiptStatusLabel || '-' },
      { key: 'paid-at', label: '支付时间', value: payment.paidAt || '-' },
      { key: 'bank-flow-no', label: '银行流水号', value: payment.bankFlowNo || '-' },
      { key: 'payment-mode', label: '支付方式', value: payment.manualPaid ? '手动支付' : '银行回调' }
    ]
  })

  const bankReceiptItems = computed<BankReceiptDisplay[]>(() =>
    (options.detail.value?.bankReceipts || []).map((receipt) => ({
      key: receipt.attachmentId || receipt.fileName,
      fileName: receipt.fileName,
      receivedAt: receipt.receivedAt || '待生成',
      metaLine: `${receipt.contentType || '未知类型'} · ${options.formatAttachmentSize(receipt.fileSize)}`,
      previewHref: receipt.previewUrl ? options.buildAuthorizedAttachmentPreviewUrl(receipt.previewUrl) : ''
    }))
  )

  const approvalSummaryItems = computed<SummaryItem[]>(() => [
    {
      key: 'current-node',
      label: '当前节点',
      value: options.detail.value?.currentNodeName || '未开始'
    },
    {
      key: 'template-name',
      label: '模板名称',
      value: options.detail.value?.templateName || '-'
    },
    {
      key: 'current-status',
      label: '当前状态',
      value: options.detail.value?.statusLabel || '-'
    }
  ])

  return {
    readonlyFormDisplay,
    bindingPanels,
    expenseDetailCards,
    expenseDetailWorkspaceVisible,
    expenseDetailSummaryItems,
    expenseDetailWorkbenchDisplay,
    bankSectionVisible,
    bankPaymentSummaryItems,
    bankReceiptItems,
    approvalSummaryItems
  }
}

function buildRelatedBindingItem(
  item: ExpenseDocumentRelationBinding,
  options: {
    keyPrefix: string
    fieldLabel: string
    businessDocumentLabel: string
    documentCodeLabel: string
    submitterLabel: string
    bindingInlineSeparator: string
  }
): BindingPanelItem {
  return {
    key: `${options.keyPrefix}-${item.fieldKey || 'field'}-${item.documentCode}`,
    documentCode: item.documentCode,
    title: item.documentTitle || item.documentCode,
    templateTypeLabel: item.templateTypeLabel || options.businessDocumentLabel,
    statusLabel: item.statusLabel,
    metaLine: `${options.documentCodeLabel}${item.documentCode} ${options.bindingInlineSeparator} ${options.submitterLabel}${item.submitterName || '-'}`,
    detailLine: `${options.fieldLabel}${item.fieldKey || '-'}`
  }
}

function buildWriteOffBindingItem(
  item: ExpenseDocumentWriteOffBinding,
  options: {
    keyPrefix: string
    businessDocumentLabel: string
    unknownStatusLabel: string
    documentCodeLabel: string
    writeOffSourceLabel: string
    bindingInlineSeparator: string
    requestedAmountLabel: string
    effectiveAmountLabel: string
    remainingAmountLabel: string
    writeOffSourceKindLabel: (kind?: string) => string
    formatBindingMoney: (value: unknown) => string
  }
): BindingPanelItem {
  return {
    key: `${options.keyPrefix}-${item.fieldKey || 'field'}-${item.documentCode}`,
    documentCode: item.documentCode,
    title: item.documentTitle || item.documentCode,
    templateTypeLabel: item.templateTypeLabel || options.businessDocumentLabel,
    statusLabel: item.effectiveStatusLabel || options.unknownStatusLabel,
    metaLine: `${options.documentCodeLabel}${item.documentCode} ${options.bindingInlineSeparator} ${options.writeOffSourceLabel}${options.writeOffSourceKindLabel(item.writeOffSourceKind)}`,
    detailLine: `${options.requestedAmountLabel}${options.formatBindingMoney(item.requestedAmount)} ${options.bindingInlineSeparator} ${options.effectiveAmountLabel}${options.formatBindingMoney(item.effectiveAmount)} ${options.bindingInlineSeparator} ${options.remainingAmountLabel}${options.formatBindingMoney(item.remainingAmount)}`
  }
}
