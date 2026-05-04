import { computed, nextTick, reactive, ref, watch, type ComputedRef, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  expenseCreateApi,
  type UserBankAccountRecord,
  type ExpenseCreatePayeeAccountOption,
  type ExpenseCreatePayeeOption,
  type ExpenseCreateVendorOption,
  type FinanceVendorDetail,
  type FinanceVendorSavePayload,
  type ProcessFormDesignBlock
} from '@/api'

type FocusManagedSelectInstance = {
  blur?: () => void
  handleClose?: () => void
  handleQueryChange?: (value: string) => void
  toggleMenu?: () => void
  expanded?: boolean
  query?: string
  previousQuery?: string
  states?: { inputValue?: string }
  inputRef?: { blur?: () => void; input?: HTMLInputElement | null } | null
  $el?: Element | null
}

type PayeeAccountLinkageMode = 'EMPLOYEE' | 'ENTERPRISE'

type RuntimePayeeSnapshot = {
  value: string
  label: string
  sourceType: string
  sourceCode: string
}

type RuntimePayeeAccountSnapshot = {
  value: string
  label: string
  sourceType: string
  ownerCode?: string
  ownerName?: string
  accountName?: string
  accountNoMasked?: string
  bankName?: string
}

type ExpenseVendorLengthField = keyof Pick<
  FinanceVendorSavePayload,
  | 'cVenName'
  | 'cVenAbbName'
  | 'cVenPerson'
  | 'cVenPhone'
  | 'receiptAccountName'
  | 'cVenBank'
  | 'cVenAccount'
  | 'receiptBranchName'
>

const PERSONAL_PAYEE_PREFIX = 'PERSONAL_PAYEE:'
const ENTERPRISE_DETAIL_TYPE = 'ENTERPRISE_TRANSACTION'
const PERSONAL_PAYEE_CREATE_LABEL = '\u65b0\u589e\u6536\u6b3e\u4eba'
const MISSING_PERSONAL_PAYEE_MESSAGE = '\u672a\u7ef4\u62a4\u6536\u6b3e\u4eba\u4fe1\u606f\uff0c\u8bf7\u5148\u65b0\u589e\u6536\u6b3e\u4eba'
const PAYEE_PLACEHOLDER = '请选择收款人'
const PAYEE_ACCOUNT_PLACEHOLDER = '请选择收款账户'
const MISSING_VENDOR_BANK_INFO_MESSAGE = '未维护银行信息，请维护银行信息'
const LOAD_COUNTERPARTY_ERROR = '加载收款单位失败'
const LOAD_PAYEE_ERROR = '加载收款人失败'
const LOAD_PAYEE_ACCOUNT_ERROR = '加载收款账户失败'

const EXPENSE_VENDOR_FIELD_MAX_LENGTH: Record<ExpenseVendorLengthField, number> = {
  cVenName: 128,
  cVenAbbName: 64,
  cVenPerson: 64,
  cVenPhone: 32,
  receiptAccountName: 128,
  cVenBank: 128,
  cVenAccount: 64,
  receiptBranchName: 128
}

const emptyVendorDraft = (): FinanceVendorSavePayload => ({
  cVenName: '',
  cVenAbbName: '',
  cVenRegCode: '',
  cVenPerson: '',
  cVenPhone: '',
  cVenAddress: '',
  receiptAccountName: '',
  cVenBankCode: '',
  cVenBank: '',
  receiptBankProvince: '',
  receiptBankCity: '',
  receiptBranchCode: '',
  receiptBranchName: '',
  cVenAccount: '',
  cMemo: ''
})

export function useExpenseRuntimePaymentCounterparty(params: {
  formData: Ref<Record<string, unknown>>
  currentUserCompanyId: ComputedRef<string>
  detailType: ComputedRef<string>
  hydratingForm: ComputedRef<boolean>
  hydrationVersion: ComputedRef<number>
  findBusinessFieldKeys: (code: string) => string[]
  isReadOnly: (block: ProcessFormDesignBlock) => boolean
  resolveErrorMessage: (error: unknown, fallback: string) => string
}) {
  const {
    formData,
    currentUserCompanyId,
    detailType,
    hydratingForm,
    hydrationVersion,
    findBusinessFieldKeys,
    isReadOnly,
    resolveErrorMessage
  } = params

  const vendorOptions = ref<ExpenseCreateVendorOption[]>([])
  const vendorOptionsLoading = ref(false)
  const payeeOptions = ref<ExpenseCreatePayeeOption[]>([])
  const payeeOptionsLoading = ref(false)
  const payeeAccountOptions = ref<ExpenseCreatePayeeAccountOption[]>([])
  const payeeAccountOptionsLoading = ref(false)
  const payeeDropdownVisible = ref(false)
  const payeeAccountDropdownVisible = ref(false)
  const payeeAccountMissingInfoWarned = ref(false)
  const counterpartySelectRefs = ref<Record<string, FocusManagedSelectInstance | null>>({})
  const payeeSelectRefs = ref<Record<string, FocusManagedSelectInstance | null>>({})
  const payeeAccountSelectRefs = ref<Record<string, FocusManagedSelectInstance | null>>({})
  const suppressLinkedFieldReset = ref(false)
  let linkedFieldResetSyncToken = 0

  const vendorDialogVisible = ref(false)
  const vendorDialogFieldKey = ref('')
  const vendorSaving = ref(false)
  const vendorDialogMode = ref<'create' | 'edit'>('create')
  const vendorDialogLoading = ref(false)
  const vendorDialogVendorCode = ref('')
  const vendorDraft = reactive<FinanceVendorSavePayload>(emptyVendorDraft())
  const personalPayeeDialogVisible = ref(false)
  const personalPayeeFieldKey = ref('')
  let pendingPersonalPayeeDialogOpenTimer: ReturnType<typeof setTimeout> | null = null

  const paymentCompanyFieldKeys = computed(() => findBusinessFieldKeys('payment-company'))
  const payeeFieldKeys = computed(() => findBusinessFieldKeys('payee'))
  const counterpartyFieldKeys = computed(() => findBusinessFieldKeys('counterparty'))
  const payeeAccountFieldKeys = computed(() => findBusinessFieldKeys('payee-account'))
  const hasCounterpartyField = computed(() => counterpartyFieldKeys.value.length > 0)

  const selectedPaymentCompanyId = computed(() => resolveSelectedPaymentCompanyId())
  const effectivePaymentCompanyId = computed(
    () => selectedPaymentCompanyId.value || currentUserCompanyId.value
  )
  const selectedPayeeName = computed(() => resolveSelectedPayeeName())
  const selectedCounterpartyCode = computed(() => resolveSelectedCounterpartyCode())
  const payeeAccountLinkageMode = computed<PayeeAccountLinkageMode>(() =>
    resolvePayeeAccountLinkageMode()
  )

  const visibleVendorOptions = computed(() => mergeCurrentVendorOption(vendorOptions.value))
  const visiblePayeeOptions = computed(() => mergeCurrentPayeeOption(payeeOptions.value))
  const visiblePayeeAccountOptions = computed(() =>
    mergeCurrentPayeeAccountOption(payeeAccountOptions.value)
  )

  const counterpartyPlaceholder = computed(() =>
    effectivePaymentCompanyId.value ? '请选择收款单位' : '请先选择付款公司'
  )

  const payeeAccountPlaceholder = computed(() => {
    if (payeeAccountLinkageMode.value === 'ENTERPRISE') {
      if (!effectivePaymentCompanyId.value) {
        return '请先选择付款公司'
      }
      if (!selectedCounterpartyCode.value) {
        return '请先选择收款单位'
      }
    }
    return PAYEE_ACCOUNT_PLACEHOLDER
  })

  const showVendorAccountMaintenanceEntry = computed(
    () =>
      payeeAccountLinkageMode.value === 'ENTERPRISE' &&
      Boolean(effectivePaymentCompanyId.value) &&
      Boolean(selectedCounterpartyCode.value) &&
      !payeeAccountOptionsLoading.value &&
      payeeAccountOptions.value.length === 0
  )

  const showPersonalPayeeEmptyState = computed(
    () =>
      payeeDropdownVisible.value &&
      !payeeOptionsLoading.value &&
      payeeOptions.value.length === 0
  )

  const showPersonalPayeeCreateEntry = computed(() => payeeDropdownVisible.value)

  const vendorDialogTitle = computed(() =>
    vendorDialogMode.value === 'edit' ? '新增银行账户' : '新增供应商'
  )

  const vendorDialogSubmitText = computed(() =>
    vendorDialogMode.value === 'edit' ? '保存银行账户' : '保存供应商'
  )

  void loadVendorOptions('')
  void loadPayeeOptions('')
  void loadPayeeAccountOptions('')

  watch(
    () => hydrationVersion.value,
    () => {
      syncLinkedLookupsFromExternalState()
    },
    { immediate: true }
  )

  watch(
    () => selectedPaymentCompanyId.value,
    (nextCompanyId, prevCompanyId) => {
      if (nextCompanyId === prevCompanyId) {
        return
      }
      if (hydratingForm.value || suppressLinkedFieldReset.value) {
        void loadVendorOptions('')
        void loadPayeeAccountOptions('', { settleAfterLoad: true })
        return
      }
      clearCounterpartySelections()
      clearPayeeAccountSelections()
      vendorOptions.value = []
      payeeAccountOptions.value = []
      if (effectivePaymentCompanyId.value) {
        void loadVendorOptions('')
      }
    },
    { immediate: false }
  )

  watch(
    () => currentUserCompanyId.value,
    (nextCompanyId, prevCompanyId) => {
      if (selectedPaymentCompanyId.value) {
        return
      }
      if (nextCompanyId === prevCompanyId) {
        return
      }
      vendorOptions.value = []
      if (nextCompanyId) {
        void loadVendorOptions('')
      }
    },
    { immediate: false }
  )

  watch(
    () => [
      payeeAccountLinkageMode.value,
      selectedPayeeName.value,
      selectedCounterpartyCode.value
    ],
    ([nextMode, nextPayeeName, nextCounterpartyCode], [prevMode, prevPayeeName, prevCounterpartyCode]) => {
      const changed =
        nextMode !== prevMode ||
        nextPayeeName !== prevPayeeName ||
        nextCounterpartyCode !== prevCounterpartyCode
      if (!changed) {
        return
      }
      if (hydratingForm.value || suppressLinkedFieldReset.value) {
        void loadPayeeAccountOptions('', { settleAfterLoad: true })
        return
      }
      settleLinkedSelectInteractions()
      clearPayeeAccountSelections()
      void nextTick(() => settleLinkedSelectInteractions())
      void loadPayeeAccountOptions('', { settleAfterLoad: true })
    },
    { immediate: false }
  )

  function resolveSelectedPayeeName() {
    for (const fieldKey of payeeFieldKeys.value) {
      const value = normalizePayeeName(formData.value[fieldKey])
      if (value) {
        return value
      }
    }
    return ''
  }

  function resolveSelectedPaymentCompanyId() {
    for (const fieldKey of paymentCompanyFieldKeys.value) {
      const value = resolveLookupValue(formData.value[fieldKey])
      if (value) {
        return value
      }
    }
    return ''
  }

  function resolveSelectedCounterpartyCode() {
    for (const fieldKey of counterpartyFieldKeys.value) {
      const value = resolveLookupValue(formData.value[fieldKey])
      if (value) {
        return value
      }
    }
    return ''
  }

  function syncLinkedLookupsFromExternalState() {
    const syncToken = ++linkedFieldResetSyncToken
    suppressLinkedFieldReset.value = true
    void nextTick(() => {
      if (syncToken !== linkedFieldResetSyncToken) {
        return
      }
      if (effectivePaymentCompanyId.value) {
        void loadVendorOptions('')
      } else {
        vendorOptions.value = []
      }
      void loadPayeeAccountOptions('', { settleAfterLoad: true })
      void nextTick(() => {
        if (syncToken === linkedFieldResetSyncToken) {
          suppressLinkedFieldReset.value = false
        }
      })
    })
  }

  function resolveCurrentCounterpartyOption() {
    const selectedCode = selectedCounterpartyCode.value
    if (!selectedCode) {
      return null
    }
    const existing = vendorOptions.value.find((item) => item.value === selectedCode)
    if (existing) {
      return existing
    }
    for (const fieldKey of counterpartyFieldKeys.value) {
      const rawValue = formData.value[fieldKey]
      const value = resolveLookupValue(rawValue)
      if (!value || value !== selectedCode) {
        continue
      }
      const label = isRecord(rawValue)
        ? firstNonEmptyString(
            rawValue.label,
            rawValue.name,
            rawValue.cVenName,
            rawValue.cVenAbbName,
            rawValue.value,
            rawValue.code
          )
        : value
      return {
        value: selectedCode,
        label: label || selectedCode,
        cVenCode: selectedCode,
        cVenName: label || selectedCode,
        cVenAbbName: label || selectedCode
      }
    }
    return {
      value: selectedCode,
      label: selectedCode,
      cVenCode: selectedCode,
      cVenName: selectedCode,
      cVenAbbName: selectedCode
    }
  }

  function mergeCurrentVendorOption(options: ExpenseCreateVendorOption[]) {
    const current = resolveCurrentCounterpartyOption()
    if (!current || options.some((item) => item.value === current.value)) {
      return options
    }
    return [current, ...options]
  }

  function resolveCurrentPayeeAccountSnapshot(): RuntimePayeeAccountSnapshot | null {
    for (const fieldKey of payeeAccountFieldKeys.value) {
      const rawValue = formData.value[fieldKey]
      const value = resolveLookupValue(rawValue)
      if (!value) {
        continue
      }
      if (isRecord(rawValue)) {
        return {
          value,
          label:
            firstNonEmptyString(
              rawValue.label,
              rawValue.accountName,
              rawValue.ownerName,
              rawValue.value
            ) || value,
          sourceType: firstNonEmptyString(rawValue.sourceType) || '',
          ownerCode: firstNonEmptyString(rawValue.ownerCode) || undefined,
          ownerName: firstNonEmptyString(rawValue.ownerName) || undefined,
          accountName: firstNonEmptyString(rawValue.accountName) || undefined,
          accountNoMasked: firstNonEmptyString(rawValue.accountNoMasked) || undefined,
          bankName: firstNonEmptyString(rawValue.bankName) || undefined
        }
      }
      return {
        value,
        label: value,
        sourceType: ''
      }
    }
    return null
  }

  function resolveCurrentPayeeSnapshot(): RuntimePayeeSnapshot | null {
    for (const fieldKey of payeeFieldKeys.value) {
      const rawValue = formData.value[fieldKey]
      const value = resolveLookupValue(rawValue)
      if (!value) {
        continue
      }
      if (isRecord(rawValue)) {
        return {
          value,
          label: firstNonEmptyString(rawValue.label, rawValue.sourceCode, rawValue.value) || value,
          sourceType: firstNonEmptyString(rawValue.sourceType) || '',
          sourceCode: firstNonEmptyString(rawValue.sourceCode, normalizePayeeName(rawValue)) || ''
        }
      }
      const payeeName = normalizePayeeName(rawValue)
      return {
        value,
        label: payeeName || value,
        sourceType: '',
        sourceCode: payeeName || value
      }
    }
    return null
  }

  function mergeCurrentPayeeOption(options: ExpenseCreatePayeeOption[]) {
    const current = resolveCurrentPayeeSnapshot()
    if (!current || options.some((item) => item.value === current.value)) {
      return options
    }
    return [
      {
        value: current.value,
        label: current.label || current.sourceCode || current.value,
        sourceType: current.sourceType || '',
        sourceCode: current.sourceCode || current.label || current.value,
        secondaryLabel: current.sourceType || undefined
      },
      ...options
    ]
  }

  function buildPayeeAccountOptionFromSnapshot(
    snapshot: RuntimePayeeAccountSnapshot
  ): ExpenseCreatePayeeAccountOption {
    return {
      value: snapshot.value,
      label: snapshot.label || snapshot.value,
      sourceType: snapshot.sourceType || '',
      ownerCode: snapshot.ownerCode || '',
      ownerName: snapshot.ownerName || snapshot.label || snapshot.value,
      accountName: snapshot.accountName,
      accountNoMasked: snapshot.accountNoMasked,
      bankName: snapshot.bankName,
      secondaryLabel: firstNonEmptyString(snapshot.accountName, snapshot.accountNoMasked) || undefined
    }
  }

  function mergeCurrentPayeeAccountOption(options: ExpenseCreatePayeeAccountOption[]) {
    const current = resolveCurrentPayeeAccountSnapshot()
    if (!current || options.some((item) => item.value === current.value)) {
      return options
    }
    return [buildPayeeAccountOptionFromSnapshot(current), ...options]
  }

  function resolvePayeeAccountLinkageMode(): PayeeAccountLinkageMode {
    if (detailType.value === ENTERPRISE_DETAIL_TYPE || hasCounterpartyField.value) {
      return 'ENTERPRISE'
    }
    return 'EMPLOYEE'
  }

  function clearPayeeAccountSelections() {
    payeeAccountFieldKeys.value.forEach((fieldKey) => {
      formData.value[fieldKey] = ''
    })
  }

  function clearCounterpartySelections() {
    counterpartyFieldKeys.value.forEach((fieldKey) => {
      formData.value[fieldKey] = ''
    })
  }

  function isCounterpartyDisabled(block: ProcessFormDesignBlock) {
    return isReadOnly(block) || !effectivePaymentCompanyId.value
  }

  function isPayeeAccountDisabled(block: ProcessFormDesignBlock) {
    if (isReadOnly(block)) {
      return true
    }
    if (payeeAccountLinkageMode.value === 'ENTERPRISE') {
      return !effectivePaymentCompanyId.value || !selectedCounterpartyCode.value
    }
    return false
  }

  function handlePayeeAccountDropdownVisibleChange(visible: boolean) {
    payeeAccountDropdownVisible.value = visible
    if (!visible) {
      payeeAccountMissingInfoWarned.value = false
      return
    }
    maybeWarnMissingVendorBankInfo()
  }

  function maybeWarnMissingVendorBankInfo() {
    if (
      !payeeAccountDropdownVisible.value ||
      payeeAccountMissingInfoWarned.value ||
      !showVendorAccountMaintenanceEntry.value
    ) {
      return
    }
    payeeAccountMissingInfoWarned.value = true
    ElMessage.warning(MISSING_VENDOR_BANK_INFO_MESSAGE)
  }

  function buildPayeeSnapshot(option: ExpenseCreatePayeeOption): RuntimePayeeSnapshot {
    return {
      value: option.value,
      label: option.label,
      sourceType: option.sourceType,
      sourceCode: option.sourceCode
    }
  }

  function buildPayeeAccountSnapshot(
    option: ExpenseCreatePayeeAccountOption
  ): RuntimePayeeAccountSnapshot {
    return {
      value: option.value,
      label: option.label,
      sourceType: option.sourceType,
      ownerCode: option.ownerCode,
      ownerName: option.ownerName,
      accountName: option.accountName,
      accountNoMasked: option.accountNoMasked,
      bankName: option.bankName
    }
  }

  function clearActiveFieldInteraction() {
    if (typeof document === 'undefined') {
      return
    }
    const activeElement = document.activeElement
    if (activeElement instanceof HTMLElement) {
      activeElement.blur()
    }
  }

  function setCounterpartySelectRef(fieldKey: string, instance: unknown) {
    counterpartySelectRefs.value[fieldKey] =
      instance && typeof instance === 'object'
        ? (instance as FocusManagedSelectInstance)
        : null
  }

  function setPayeeSelectRef(fieldKey: string, instance: unknown) {
    payeeSelectRefs.value[fieldKey] =
      instance && typeof instance === 'object'
        ? (instance as FocusManagedSelectInstance)
        : null
  }

  function setPayeeAccountSelectRef(fieldKey: string, instance: unknown) {
    payeeAccountSelectRefs.value[fieldKey] =
      instance && typeof instance === 'object'
        ? (instance as FocusManagedSelectInstance)
        : null
  }

  function clearManagedSelectQuery(instance: FocusManagedSelectInstance | null | undefined) {
    if (!instance) {
      return
    }
    if (typeof instance.handleQueryChange === 'function') {
      instance.handleQueryChange('')
    }
    if (typeof instance.query === 'string') {
      instance.query = ''
    }
    if (typeof instance.previousQuery === 'string') {
      instance.previousQuery = ''
    }
    if (instance.states && typeof instance.states === 'object' && 'inputValue' in instance.states) {
      instance.states.inputValue = ''
    }
    const inputElement = resolveManagedSelectInputElement(instance)
    if (inputElement) {
      inputElement.value = ''
    }
  }

  function resolveManagedSelectInputElement(
    instance: FocusManagedSelectInstance | null | undefined
  ) {
    if (!instance) {
      return null
    }
    if (instance.inputRef?.input instanceof HTMLInputElement) {
      return instance.inputRef.input
    }
    if (instance.$el instanceof HTMLElement) {
      const inputElement = instance.$el.querySelector('input')
      if (inputElement instanceof HTMLInputElement) {
        return inputElement
      }
    }
    return null
  }

  function closeManagedSelect(instance: FocusManagedSelectInstance | null | undefined) {
    if (!instance) {
      return
    }
    clearManagedSelectQuery(instance)
    if (typeof instance.handleClose === 'function') {
      instance.handleClose()
    } else if (instance.expanded && typeof instance.toggleMenu === 'function') {
      instance.toggleMenu()
    }
    const inputElement = resolveManagedSelectInputElement(instance)
    if (inputElement) {
      inputElement.blur()
      inputElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    }
    if (instance.inputRef && typeof instance.inputRef.blur === 'function') {
      instance.inputRef.blur()
    }
    if (typeof instance.blur === 'function') {
      instance.blur()
    }
  }

  function settleLinkedSelectInteractions() {
    Object.values(counterpartySelectRefs.value).forEach((instance) => closeManagedSelect(instance))
    Object.values(payeeSelectRefs.value).forEach((instance) => closeManagedSelect(instance))
    Object.values(payeeAccountSelectRefs.value).forEach((instance) => closeManagedSelect(instance))
    clearActiveFieldInteraction()
  }

  function prepareDocumentPickerOpen() {
    if (hydratingForm.value || suppressLinkedFieldReset.value) {
      void loadPayeeAccountOptions('', { settleAfterLoad: true })
      return
    }
    settleLinkedSelectInteractions()
  }

  function handleCounterpartySelection(fieldKey: string, value: string | '' | null | undefined) {
    formData.value[fieldKey] = value || ''
    settleLinkedSelectInteractions()
    void nextTick(() => settleLinkedSelectInteractions())
  }

  function handlePayeeSelection(
    fieldKey: string,
    value: RuntimePayeeSnapshot | '' | null | undefined
  ) {
    formData.value[fieldKey] = value || ''
    settleLinkedSelectInteractions()
    void nextTick(() => settleLinkedSelectInteractions())
  }

  function handlePayeeDropdownVisibleChange(visible: boolean) {
    payeeDropdownVisible.value = visible
  }

  function handlePayeeAccountSelection(
    fieldKey: string,
    value: RuntimePayeeAccountSnapshot | '' | null | undefined
  ) {
    formData.value[fieldKey] = value || ''
  }

  async function loadVendorOptions(keyword: string) {
    if (!effectivePaymentCompanyId.value) {
      vendorOptions.value = []
      return
    }
    vendorOptionsLoading.value = true
    try {
      const res = await expenseCreateApi.listVendorOptions({
        keyword: keyword || undefined,
        paymentCompanyId: effectivePaymentCompanyId.value
      })
      vendorOptions.value = res.data
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, LOAD_COUNTERPARTY_ERROR))
    } finally {
      vendorOptionsLoading.value = false
    }
  }

  async function loadPayeeOptions(keyword: string) {
    payeeOptionsLoading.value = true
    try {
      const res = await expenseCreateApi.listPayeeOptions({
        keyword,
        personalOnly: true
      })
      payeeOptions.value = res.data
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, LOAD_PAYEE_ERROR))
    } finally {
      payeeOptionsLoading.value = false
    }
  }

  function openPersonalPayeeDialog(fieldKey: string) {
    clearPendingPersonalPayeeDialogOpen()
    personalPayeeFieldKey.value = fieldKey
    personalPayeeDialogVisible.value = true
  }

  function openPersonalPayeeDialogFromPanel(fieldKey: string) {
    clearPendingPersonalPayeeDialogOpen()
    personalPayeeFieldKey.value = fieldKey
    payeeDropdownVisible.value = false
    settleLinkedSelectInteractions()
    pendingPersonalPayeeDialogOpenTimer = setTimeout(() => {
      pendingPersonalPayeeDialogOpenTimer = null
      personalPayeeDialogVisible.value = true
    }, 0)
  }

  function handlePersonalPayeeDialogVisibleChange(visible: boolean) {
    if (visible) {
      clearPendingPersonalPayeeDialogOpen()
    }
    personalPayeeDialogVisible.value = visible
    if (!visible) {
      personalPayeeFieldKey.value = ''
    }
  }

  function clearPendingPersonalPayeeDialogOpen() {
    if (pendingPersonalPayeeDialogOpenTimer === null) {
      return
    }
    clearTimeout(pendingPersonalPayeeDialogOpenTimer)
    pendingPersonalPayeeDialogOpenTimer = null
  }

  async function handlePersonalPayeeSaved(payload: {
    accountName: string
    record: UserBankAccountRecord
  }) {
    await loadPayeeOptions(payload.accountName)
    const normalizedAccountName = String(payload.accountName || '').trim()
    const matchedOption = payeeOptions.value.find((item) => {
      const optionName = normalizePayeeName(item.value || item.label || '')
      return optionName === normalizedAccountName
    })
    const snapshot = matchedOption
      ? buildPayeeSnapshot(matchedOption)
      : {
          value: `${PERSONAL_PAYEE_PREFIX}${normalizedAccountName}`,
          label: normalizedAccountName,
          sourceType: 'PERSONAL_PRIVATE_PAYEE',
          sourceCode: normalizedAccountName
        }
    const targetFieldKey = personalPayeeFieldKey.value || payeeFieldKeys.value[0] || ''
    if (targetFieldKey) {
      formData.value[targetFieldKey] = snapshot
    }
    clearPayeeAccountSelections()
    personalPayeeDialogVisible.value = false
    personalPayeeFieldKey.value = ''
  }

  async function loadPayeeAccountOptions(keyword: string, options?: { settleAfterLoad?: boolean }) {
    if (payeeAccountLinkageMode.value === 'ENTERPRISE') {
      if (!effectivePaymentCompanyId.value || !selectedCounterpartyCode.value) {
        payeeAccountOptions.value = []
        if (options?.settleAfterLoad) {
          settleLinkedSelectInteractions()
        }
        return
      }
    }
    payeeAccountOptionsLoading.value = true
    try {
      const res = await expenseCreateApi.listPayeeAccountOptions({
        keyword,
        linkageMode: payeeAccountLinkageMode.value,
        payeeName: selectedPayeeName.value || undefined,
        counterpartyCode: selectedCounterpartyCode.value || undefined,
        paymentCompanyId: effectivePaymentCompanyId.value || undefined
      })
      payeeAccountOptions.value = res.data
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, LOAD_PAYEE_ACCOUNT_ERROR))
    } finally {
      payeeAccountOptionsLoading.value = false
      maybeWarnMissingVendorBankInfo()
      if (options?.settleAfterLoad) {
        settleLinkedSelectInteractions()
        void nextTick(() => settleLinkedSelectInteractions())
      }
    }
  }

  function openVendorDialog(fieldKey = '') {
    if (!effectivePaymentCompanyId.value) {
      ElMessage.warning('请先选择付款公司')
      return
    }
    vendorDialogMode.value = 'create'
    vendorDialogFieldKey.value = fieldKey
    vendorDialogVendorCode.value = ''
    Object.assign(vendorDraft, emptyVendorDraft())
    vendorDialogVisible.value = true
  }

  function closeVendorDialog() {
    vendorDialogVisible.value = false
    vendorDialogFieldKey.value = ''
    vendorDialogVendorCode.value = ''
    vendorDialogMode.value = 'create'
    vendorDialogLoading.value = false
    Object.assign(vendorDraft, emptyVendorDraft())
  }

  async function openVendorAccountDialog() {
    if (!effectivePaymentCompanyId.value) {
      ElMessage.warning('请先选择付款公司')
      return
    }
    if (!selectedCounterpartyCode.value) {
      ElMessage.warning('请先选择收款单位')
      return
    }
    vendorDialogMode.value = 'edit'
    vendorDialogFieldKey.value = ''
    vendorDialogVendorCode.value = selectedCounterpartyCode.value
    vendorDialogLoading.value = true
    vendorDialogVisible.value = true
    try {
      const res = await expenseCreateApi.getVendorDetail(
        effectivePaymentCompanyId.value,
        selectedCounterpartyCode.value
      )
      hydrateVendorDraft(res.data)
    } catch (error: unknown) {
      vendorDialogVisible.value = false
      ElMessage.error(resolveErrorMessage(error, '加载供应商信息失败'))
    } finally {
      vendorDialogLoading.value = false
    }
  }

  function validateVendorDraft() {
    const incompleteBankDirectoryMessage = '请选择开户银行、开户省、开户市与开户网点后再保存'
    if (!String(vendorDraft.cVenName || '').trim()) {
      return '请输入供应商名称'
    }
    if (!String(vendorDraft.receiptAccountName || vendorDraft.cVenName || '').trim()) {
      return '请输入账户名'
    }
    if (!String(vendorDraft.cVenAccount || '').trim()) {
      return '请先填写银行账号'
    }
    if (
      !String(vendorDraft.cVenBank || '').trim() ||
      !String(vendorDraft.cVenBankCode || '').trim() ||
      !String(vendorDraft.receiptBankProvince || '').trim() ||
      !String(vendorDraft.receiptBankCity || '').trim() ||
      !String(vendorDraft.receiptBranchName || '').trim() ||
      !String(vendorDraft.receiptBranchCode || '').trim()
    ) {
      return incompleteBankDirectoryMessage
    }
    const lengthRules: Array<{ key: ExpenseVendorLengthField; label: string; max: number }> = [
      { key: 'cVenName', label: '供应商名称', max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.cVenName },
      { key: 'cVenAbbName', label: '供应商简称', max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.cVenAbbName },
      { key: 'cVenPerson', label: '联系人', max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.cVenPerson },
      { key: 'cVenPhone', label: '联系电话', max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.cVenPhone },
      {
        key: 'receiptAccountName',
        label: '账户名',
        max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.receiptAccountName
      },
      { key: 'cVenBank', label: '开户银行', max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.cVenBank },
      { key: 'cVenAccount', label: '银行账号', max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.cVenAccount },
      {
        key: 'receiptBranchName',
        label: '开户网点',
        max: EXPENSE_VENDOR_FIELD_MAX_LENGTH.receiptBranchName
      }
    ]
    for (const rule of lengthRules) {
      const value = String(vendorDraft[rule.key] || '').trim()
      if (value.length > rule.max) {
        return `${rule.label}最多 ${rule.max} 个字符`
      }
    }
    return ''
  }

  function hydrateVendorDraft(detail: Partial<FinanceVendorDetail>) {
    Object.assign(vendorDraft, emptyVendorDraft(), {
      cVenName: detail.cVenName || '',
      cVenAbbName: detail.cVenAbbName || '',
      cVenRegCode: detail.cVenRegCode || '',
      cVenPerson: detail.cVenPerson || '',
      cVenPhone: detail.cVenPhone || '',
      cVenAddress: detail.cVenAddress || '',
      receiptAccountName: detail.receiptAccountName || '',
      cVenBankCode: detail.cVenBankCode || '',
      cVenBank: detail.cVenBank || '',
      receiptBankProvince: detail.receiptBankProvince || '',
      receiptBankCity: detail.receiptBankCity || '',
      receiptBranchCode: detail.receiptBranchCode || '',
      receiptBranchName: detail.receiptBranchName || '',
      cVenAccount: detail.cVenAccount || '',
      cMemo: detail.cMemo || ''
    })
  }

  function buildVendorCreatePayload() {
    return Object.fromEntries(
      Object.entries(vendorDraft).filter(([, value]) => value !== undefined && value !== null && value !== '')
    ) as FinanceVendorSavePayload
  }

  async function saveVendor() {
    const validationMessage = validateVendorDraft()
    if (validationMessage) {
      ElMessage.warning(validationMessage)
      return
    }
    vendorSaving.value = true
    try {
      if (!effectivePaymentCompanyId.value) {
        ElMessage.warning('请先选择付款公司')
        return
      }
      if (vendorDialogMode.value === 'edit') {
        const vendorCode = vendorDialogVendorCode.value || selectedCounterpartyCode.value
        if (!vendorCode) {
          ElMessage.warning('请先选择收款单位')
          return
        }
        await expenseCreateApi.updateVendor(effectivePaymentCompanyId.value, vendorCode, {
          ...vendorDraft
        })
        await loadVendorOptions(String(vendorDraft.cVenName || vendorCode))
        await loadPayeeAccountOptions('')
        ElMessage.success('供应商银行信息已更新')
      } else {
        const res = await expenseCreateApi.createVendor(
          effectivePaymentCompanyId.value,
          buildVendorCreatePayload()
        )
        await loadVendorOptions(String(res.data.cVenName || res.data.cVenCode || ''))
        if (vendorDialogFieldKey.value) {
          formData.value[vendorDialogFieldKey.value] = res.data.cVenCode
        }
        clearPayeeAccountSelections()
        ElMessage.success('供应商及收款信息已保存')
      }
      closeVendorDialog()
    } catch (error: unknown) {
      ElMessage.error(
        resolveErrorMessage(
          error,
          vendorDialogMode.value === 'edit' ? '维护供应商银行信息失败' : '新增供应商及收款信息失败'
        )
      )
    } finally {
      vendorSaving.value = false
    }
  }

  function normalizePayeeName(value: unknown) {
    if (!value) {
      return ''
    }
    if (typeof value === 'string') {
      return value.startsWith(PERSONAL_PAYEE_PREFIX)
        ? value.slice(PERSONAL_PAYEE_PREFIX.length)
        : value
    }
    if (isRecord(value)) {
      const label = firstNonEmptyString(value.label, value.sourceCode)
      if (label) {
        return label.startsWith(PERSONAL_PAYEE_PREFIX)
          ? label.slice(PERSONAL_PAYEE_PREFIX.length)
          : label
      }
      const rawValue = firstNonEmptyString(value.value)
      if (rawValue) {
        return rawValue.startsWith(PERSONAL_PAYEE_PREFIX)
          ? rawValue.slice(PERSONAL_PAYEE_PREFIX.length)
          : rawValue
      }
    }
    return ''
  }

  function resolveLookupValue(value: unknown) {
    if (typeof value === 'string') {
      return value
    }
    if (isRecord(value)) {
      return firstNonEmptyString(value.value, value.code, value.id)
    }
    return ''
  }

  function firstNonEmptyString(...values: unknown[]) {
    for (const value of values) {
      if (value === null || value === undefined) {
        continue
      }
      const text = String(value).trim()
      if (text) {
        return text
      }
    }
    return ''
  }

  function isRecord(value: unknown): value is Record<string, any> {
    return !!value && typeof value === 'object'
  }

  return {
    PAYEE_PLACEHOLDER,
    PERSONAL_PAYEE_CREATE_LABEL,
    MISSING_PERSONAL_PAYEE_MESSAGE,
    MISSING_VENDOR_BANK_INFO_MESSAGE,
    effectivePaymentCompanyId,
    selectedCounterpartyCode,
    vendorOptionsLoading,
    payeeOptions,
    visiblePayeeOptions,
    payeeOptionsLoading,
    payeeAccountOptionsLoading,
    visibleVendorOptions,
    visiblePayeeAccountOptions,
    counterpartyPlaceholder,
    payeeAccountPlaceholder,
    showPersonalPayeeEmptyState,
    showPersonalPayeeCreateEntry,
    showVendorAccountMaintenanceEntry,
    vendorDialogVisible,
    vendorDialogTitle,
    vendorDialogSubmitText,
    vendorDialogLoading,
    vendorSaving,
    vendorDraft,
    isCounterpartyDisabled,
    isPayeeAccountDisabled,
    loadVendorOptions,
    loadPayeeOptions,
    loadPayeeAccountOptions,
    handlePayeeDropdownVisibleChange,
    handlePayeeAccountDropdownVisibleChange,
    buildPayeeSnapshot,
    buildPayeeAccountSnapshot,
    setCounterpartySelectRef,
    setPayeeSelectRef,
    setPayeeAccountSelectRef,
    prepareDocumentPickerOpen,
    handleCounterpartySelection,
    handlePayeeSelection,
    handlePayeeAccountSelection,
    personalPayeeDialogVisible,
    handlePersonalPayeeDialogVisibleChange,
    openPersonalPayeeDialog,
    openPersonalPayeeDialogFromPanel,
    handlePersonalPayeeSaved,
    openVendorDialog,
    closeVendorDialog,
    openVendorAccountDialog,
    validateVendorDraft,
    saveVendor
  }
}
