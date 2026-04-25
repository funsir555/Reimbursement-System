import { computed, reactive, ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemSettingsApi, type CompanyRecord, type CompanySavePayload } from '@/api'
import { flattenCompanies, type FlatCompanyRecord } from '../systemSettingsShared'

export function useSystemSettingsCompanies(params: {
  companies: Ref<CompanyRecord[]>
  loadBootstrap: () => Promise<void>
}) {
  const { companies, loadBootstrap } = params

  const selectedCompany = ref<CompanyRecord>()
  const companyDialogVisible = ref(false)
  const companyForm = reactive<CompanySavePayload>({
    companyName: ''
  })

  const flatCompanies = computed<FlatCompanyRecord[]>(() => flattenCompanies(companies.value))
  const companyCount = computed(() => flatCompanies.value.length)
  const companyOptions = computed(() =>
    flatCompanies.value.map((item) => ({
      companyId: item.companyId,
      label: item.label
    }))
  )

  function applyCompaniesBootstrap(nextCompanies: CompanyRecord[] = companies.value) {
    selectedCompany.value = selectedCompany.value
      ? flattenCompanies(nextCompanies).find((item) => item.companyId === selectedCompany.value?.companyId)
      : undefined
  }

  function openCompanyDialog(item?: CompanyRecord) {
    selectedCompany.value = item
    companyForm.companyName = item?.companyName || ''
    companyForm.invoiceTitle = item?.invoiceTitle
    companyForm.taxNo = item?.taxNo
    companyForm.status = item?.status ?? 1
    companyDialogVisible.value = true
  }

  async function saveCompany(closeAfterSave: boolean) {
    const editingCompany = !!selectedCompany.value?.companyId
    if (editingCompany) {
      await systemSettingsApi.updateCompany(selectedCompany.value!.companyId, {
        ...companyForm,
        status: 1
      })
    } else {
      await systemSettingsApi.createCompany({ ...companyForm, status: 1 })
    }

    if (closeAfterSave || editingCompany) {
      companyDialogVisible.value = false
    } else {
      resetCompanyForm()
    }

    ElMessage.success('公司已保存')
    await loadBootstrap()
  }

  async function handleDeleteCompany() {
    if (!selectedCompany.value) {
      return
    }
    await ElMessageBox.confirm(`确认删除公司“${selectedCompany.value.companyName}”吗？`, '提示', {
      type: 'warning'
    })
    await systemSettingsApi.deleteCompany(selectedCompany.value.companyId)
    ElMessage.success('公司已删除')
    selectedCompany.value = undefined
    await loadBootstrap()
  }

  function resetCompanyForm() {
    selectedCompany.value = undefined
    companyForm.companyName = ''
    companyForm.invoiceTitle = undefined
    companyForm.taxNo = undefined
    companyForm.status = 1
  }

  return {
    selectedCompany,
    companyDialogVisible,
    companyForm,
    flatCompanies,
    companyCount,
    companyOptions,
    applyCompaniesBootstrap,
    openCompanyDialog,
    saveCompany,
    handleDeleteCompany
  }
}
