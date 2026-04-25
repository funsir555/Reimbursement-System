import { computed, reactive, ref, type ComputedRef, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  systemSettingsApi,
  type CompanyBankAccountRecord,
  type CompanyBankAccountSavePayload
} from '@/api'
import {
  createCompanyBankAccountFormState,
  maskAccountNo,
  type CompanyBankAccountFormState,
  type CompanyOption
} from '../systemSettingsShared'

export function useSystemSettingsCompanyAccounts(params: {
  companyBankAccounts: Ref<CompanyBankAccountRecord[]>
  companyOptions: ComputedRef<CompanyOption[]>
  loadBootstrap: () => Promise<void>
}) {
  const { companyBankAccounts, companyOptions, loadBootstrap } = params

  const companyAccountCompanyFilter = ref<string>()
  const companyAccountStatusFilter = ref<number>()
  const companyAccountDirectConnectFilter = ref<number>()
  const editingCompanyBankAccount = ref<CompanyBankAccountRecord>()
  const companyBankAccountDialogVisible = ref(false)
  const companyBankAccountForm = reactive<CompanyBankAccountFormState>(
    createCompanyBankAccountFormState()
  )

  const filteredCompanyBankAccounts = computed(() =>
    companyBankAccounts.value.filter((item) => {
      const matchesCompany =
        !companyAccountCompanyFilter.value || item.companyId === companyAccountCompanyFilter.value
      const matchesStatus =
        companyAccountStatusFilter.value === undefined ||
        item.status === companyAccountStatusFilter.value
      const matchesDirectConnect =
        companyAccountDirectConnectFilter.value === undefined ||
        item.directConnectEnabled === companyAccountDirectConnectFilter.value
      return matchesCompany && matchesStatus && matchesDirectConnect
    })
  )

  function openCompanyBankAccountDialog(item?: CompanyBankAccountRecord) {
    editingCompanyBankAccount.value = item
    companyBankAccountForm.companyId = item?.companyId || ''
    companyBankAccountForm.bankName = item?.bankName || ''
    companyBankAccountForm.province = item?.province || ''
    companyBankAccountForm.city = item?.city || ''
    companyBankAccountForm.branchName = item?.branchName
    companyBankAccountForm.bankCode = item?.bankCode
    companyBankAccountForm.branchCode = item?.branchCode
    companyBankAccountForm.accountName = item?.accountName || ''
    companyBankAccountForm.accountNo = item?.accountNo || ''
    companyBankAccountForm.accountType = item?.accountType
    companyBankAccountForm.accountUsage = item?.accountUsage
    companyBankAccountForm.currencyCode = item?.currencyCode
    companyBankAccountForm.defaultAccount = item?.defaultAccount ?? 0
    companyBankAccountForm.status = item?.status ?? 1
    companyBankAccountForm.remark = item?.remark
    companyBankAccountForm.directConnectEnabled = item?.directConnectEnabled ?? 0
    companyBankAccountForm.directConnectProvider = item?.directConnectProvider
    companyBankAccountForm.directConnectChannel = item?.directConnectChannel
    companyBankAccountForm.directConnectProtocol = item?.directConnectProtocol
    companyBankAccountForm.directConnectCustomerNo = item?.directConnectCustomerNo
    companyBankAccountForm.directConnectAppId = item?.directConnectAppId
    companyBankAccountForm.directConnectAccountAlias = item?.directConnectAccountAlias
    companyBankAccountForm.directConnectAuthMode = item?.directConnectAuthMode
    companyBankAccountForm.directConnectApiBaseUrl = item?.directConnectApiBaseUrl
    companyBankAccountForm.directConnectCertRef = item?.directConnectCertRef
    companyBankAccountForm.directConnectSecretRef = item?.directConnectSecretRef
    companyBankAccountForm.directConnectSignType = item?.directConnectSignType
    companyBankAccountForm.directConnectEncryptType = item?.directConnectEncryptType
    companyBankAccountForm.directConnectLastSyncAt = item?.directConnectLastSyncAt
    companyBankAccountForm.directConnectLastSyncStatus = item?.directConnectLastSyncStatus
    companyBankAccountForm.directConnectLastErrorMsg = item?.directConnectLastErrorMsg
    companyBankAccountForm.directConnectExtJson = item?.directConnectExtJson
    companyBankAccountDialogVisible.value = true
  }

  async function saveCompanyBankAccount() {
    const validationMessage = validateCompanyBankAccountForm()
    if (validationMessage) {
      ElMessage.warning(validationMessage)
      return
    }
    const payload = buildCompanyBankAccountPayload(companyBankAccountForm)
    if (editingCompanyBankAccount.value?.id) {
      await systemSettingsApi.updateCompanyBankAccount(editingCompanyBankAccount.value.id, payload)
    } else {
      await systemSettingsApi.createCompanyBankAccount(payload)
    }
    companyBankAccountDialogVisible.value = false
    resetCompanyBankAccountForm()
    ElMessage.success('公司银行账户已保存')
    await loadBootstrap()
  }

  async function handleDeleteCompanyBankAccount(row: CompanyBankAccountRecord) {
    await ElMessageBox.confirm(
      `确认删除公司银行账户“${row.accountName} / ${maskAccountNo(row.accountNo)}”吗？`,
      '提示',
      { type: 'warning' }
    )
    await systemSettingsApi.deleteCompanyBankAccount(row.id)
    ElMessage.success('公司银行账户已删除')
    await loadBootstrap()
  }

  async function toggleCompanyBankAccountStatus(row: CompanyBankAccountRecord, status: number) {
    await systemSettingsApi.updateCompanyBankAccount(row.id, {
      ...buildCompanyBankAccountPayload(row),
      status,
      defaultAccount: status === 0 ? 0 : row.defaultAccount
    })
    ElMessage.success(status === 1 ? '公司银行账户已启用' : '公司银行账户已停用')
    await loadBootstrap()
  }

  async function setCompanyBankAccountDefault(row: CompanyBankAccountRecord) {
    await systemSettingsApi.updateCompanyBankAccount(row.id, {
      ...buildCompanyBankAccountPayload(row),
      status: 1,
      defaultAccount: 1
    })
    ElMessage.success('公司银行账户已设为默认')
    await loadBootstrap()
  }

  function resolveCompanyName(companyId: string) {
    return companyOptions.value.find((item) => item.companyId === companyId)?.label || companyId
  }

  function buildCompanyBankAccountPayload(
    source: CompanyBankAccountFormState | CompanyBankAccountRecord
  ): CompanyBankAccountSavePayload {
    return {
      companyId: source.companyId,
      bankName: source.bankName,
      province: source.province || '',
      city: source.city || '',
      branchName: source.branchName,
      bankCode: source.bankCode,
      branchCode: source.branchCode,
      accountName: source.accountName,
      accountNo: source.accountNo,
      accountType: source.accountType,
      accountUsage: source.accountUsage,
      currencyCode: source.currencyCode,
      defaultAccount: source.defaultAccount,
      status: source.status,
      remark: source.remark,
      directConnectEnabled: source.directConnectEnabled,
      directConnectProvider: source.directConnectProvider,
      directConnectChannel: source.directConnectChannel,
      directConnectProtocol: source.directConnectProtocol,
      directConnectCustomerNo: source.directConnectCustomerNo,
      directConnectAppId: source.directConnectAppId,
      directConnectAccountAlias: source.directConnectAccountAlias,
      directConnectAuthMode: source.directConnectAuthMode,
      directConnectApiBaseUrl: source.directConnectApiBaseUrl,
      directConnectCertRef: source.directConnectCertRef,
      directConnectSecretRef: source.directConnectSecretRef,
      directConnectSignType: source.directConnectSignType,
      directConnectEncryptType: source.directConnectEncryptType,
      directConnectLastSyncAt: source.directConnectLastSyncAt,
      directConnectLastSyncStatus: source.directConnectLastSyncStatus,
      directConnectLastErrorMsg: source.directConnectLastErrorMsg,
      directConnectExtJson: source.directConnectExtJson
    }
  }

  function validateCompanyBankAccountForm() {
    const incompleteBankDirectoryMessage = '请选择开户银行、开户省、开户市与开户网点后再保存'
    if (!String(companyBankAccountForm.companyId || '').trim()) return '请选择所属公司'
    if (
      !String(companyBankAccountForm.bankCode || '').trim() ||
      !String(companyBankAccountForm.bankName || '').trim() ||
      !String(companyBankAccountForm.province || '').trim() ||
      !String(companyBankAccountForm.city || '').trim() ||
      !String(companyBankAccountForm.branchCode || '').trim() ||
      !String(companyBankAccountForm.branchName || '').trim()
    ) {
      return incompleteBankDirectoryMessage
    }
    if (!String(companyBankAccountForm.accountName || '').trim()) return '请填写账户名'
    if (!String(companyBankAccountForm.accountNo || '').trim()) return '请填写银行账号'
    return ''
  }

  function resetCompanyBankAccountForm() {
    editingCompanyBankAccount.value = undefined
    Object.assign(companyBankAccountForm, createCompanyBankAccountFormState())
    companyBankAccountForm.branchName = undefined
    companyBankAccountForm.bankCode = undefined
    companyBankAccountForm.branchCode = undefined
    companyBankAccountForm.accountType = undefined
    companyBankAccountForm.accountUsage = undefined
    companyBankAccountForm.currencyCode = undefined
    companyBankAccountForm.remark = undefined
    companyBankAccountForm.directConnectProvider = undefined
    companyBankAccountForm.directConnectChannel = undefined
    companyBankAccountForm.directConnectProtocol = undefined
    companyBankAccountForm.directConnectCustomerNo = undefined
    companyBankAccountForm.directConnectAppId = undefined
    companyBankAccountForm.directConnectAccountAlias = undefined
    companyBankAccountForm.directConnectAuthMode = undefined
    companyBankAccountForm.directConnectApiBaseUrl = undefined
    companyBankAccountForm.directConnectCertRef = undefined
    companyBankAccountForm.directConnectSecretRef = undefined
    companyBankAccountForm.directConnectSignType = undefined
    companyBankAccountForm.directConnectEncryptType = undefined
    companyBankAccountForm.directConnectLastSyncAt = undefined
    companyBankAccountForm.directConnectLastSyncStatus = undefined
    companyBankAccountForm.directConnectLastErrorMsg = undefined
    companyBankAccountForm.directConnectExtJson = undefined
  }

  return {
    companyAccountCompanyFilter,
    companyAccountStatusFilter,
    companyAccountDirectConnectFilter,
    editingCompanyBankAccount,
    companyBankAccountDialogVisible,
    companyBankAccountForm,
    filteredCompanyBankAccounts,
    openCompanyBankAccountDialog,
    saveCompanyBankAccount,
    handleDeleteCompanyBankAccount,
    toggleCompanyBankAccountStatus,
    setCompanyBankAccountDefault,
    resolveCompanyName,
    validateCompanyBankAccountForm,
    resetCompanyBankAccountForm
  }
}
