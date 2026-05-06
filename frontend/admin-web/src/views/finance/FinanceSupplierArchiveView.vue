<template>
  <div class="space-y-4">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">供应商档案</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前公司</span>
            <strong>{{ currentCompanyName || '未设置' }}</strong>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <el-button :icon="RefreshRight" @click="loadVendors(true)">刷新</el-button>
          <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openCreateDialog">新增供应商</el-button>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),220px,160px]">
        <el-input v-model="keyword" clearable placeholder="请输入供应商名称或编码" @keyup.enter="loadVendors(true)">
          <template #append>
            <el-button :icon="Search" @click="loadVendors(true)" />
          </template>
        </el-input>

        <el-switch
          v-model="includeDisabled"
          inline-prompt
          active-text="含停用"
          inactive-text="仅启用"
          @change="loadVendors(true)"
        />

        <div class="flex justify-end">
          <el-button :icon="RefreshRight" @click="resetFilters">重置筛选</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="paginatedVendors" style="width: 100%">
        <el-table-column prop="cVenCode" label="供应商编码" width="170" />
        <el-table-column prop="cVenName" label="供应商名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="cVenAbbName" label="简称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="cVenPerson" label="联系人" min-width="120" show-overflow-tooltip />
        <el-table-column prop="cVenPhone" label="联系电话" min-width="140" show-overflow-tooltip />
        <el-table-column label="收款账户" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ supplierAccountText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" effect="plain">
              {{ row.active ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit" type="primary" link @click="openEditDialog(row.cVenCode)">编辑</el-button>
            <el-button v-if="canDisable && row.active" type="danger" link @click="disableSupplier(row.cVenCode)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 flex justify-start">
        <el-pagination
          v-model:current-page="vendorPagination.currentPage.value"
          v-model:page-size="vendorPagination.pageSize.value"
          layout="total, sizes, prev, pager, next"
          :total="vendorPagination.total.value"
          :page-sizes="vendorPagination.pageSizes"
        />
      </div>
    </el-card>

    <finance-supplier-archive-dialog
      ref="vendorDialogRef"
      :company-id="currentCompanyId"
      :company-name="currentCompanyName"
      @saved="handleVendorSaved"
      @closed="dialogOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { financeArchiveApi, type FinanceVendorSummary } from '@/api'
import FinanceSupplierArchiveDialog from '@/components/finance/FinanceSupplierArchiveDialog.vue'
import { useLocalPagination } from '@/composables/useLocalPagination'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const keyword = ref('')
const includeDisabled = ref(false)
const vendors = ref<FinanceVendorSummary[]>([])
const vendorPagination = useLocalPagination(vendors)
const financeCompany = useFinanceCompanyStore()
const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const paginatedVendors = computed(() => vendorPagination.paginatedRows.value)
const canCreate = computed(() => hasPermission('finance:archives:suppliers:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:suppliers:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:suppliers:delete', permissionCodes.value))

const COMPANY_SWITCH_GUARD_KEY = 'finance-supplier-archive'
const vendorDialogRef = ref<InstanceType<typeof FinanceSupplierArchiveDialog> | null>(null)
const dialogOpen = ref(false)
const fallbackVendorForm = reactive<Record<string, string | number | undefined>>({})
let guardRegistered = false

const vendorForm = computed<Record<string, string | number | undefined>>(
  () => vendorDialogRef.value?.vendorForm || fallbackVendorForm
)
const activeSections = computed(() => vendorDialogRef.value?.activeSections || [])
const vendorSections = computed(() => vendorDialogRef.value?.vendorSections || [])
const hasPendingEdit = computed(() => dialogOpen.value)

onMounted(registerCompanySwitchGuard)
onActivated(registerCompanySwitchGuard)
onDeactivated(unregisterCompanySwitchGuard)

watch(
  () => financeCompany.currentCompanyId,
  async (companyId, previousCompanyId) => {
    if (!companyId) return
    if (companyId !== previousCompanyId) {
      closeDialog()
    }
    await loadVendors(true)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  unregisterCompanySwitchGuard()
})

function registerCompanySwitchGuard() {
  if (guardRegistered) return
  financeCompany.registerSwitchGuard(COMPANY_SWITCH_GUARD_KEY, confirmCompanySwitch)
  guardRegistered = true
}

function unregisterCompanySwitchGuard() {
  if (!guardRegistered) return
  financeCompany.unregisterSwitchGuard(COMPANY_SWITCH_GUARD_KEY)
  guardRegistered = false
}

async function loadVendors(resetPage = false) {
  if (resetPage) {
    vendorPagination.resetToFirstPage()
  }
  if (!currentCompanyId.value) {
    vendors.value = []
    vendorPagination.clampCurrentPage()
    return
  }
  loading.value = true
  try {
    const res = await financeArchiveApi.listSuppliers({
      companyId: currentCompanyId.value,
      keyword: keyword.value.trim(),
      includeDisabled: includeDisabled.value
    })
    vendors.value = res.data
    vendorPagination.clampCurrentPage()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载供应商档案失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  includeDisabled.value = false
  void loadVendors(true)
}

function openCreateDialog() {
  dialogOpen.value = true
  vendorDialogRef.value?.openCreateDialog()
}

async function openEditDialog(vendorCode: string) {
  dialogOpen.value = true
  await vendorDialogRef.value?.openEditDialog(vendorCode)
}

async function saveSupplier() {
  await vendorDialogRef.value?.saveSupplier()
}

function closeDialog() {
  dialogOpen.value = false
  vendorDialogRef.value?.closeDialog()
}

async function handleVendorSaved() {
  dialogOpen.value = false
  await loadVendors()
}

async function disableSupplier(vendorCode: string) {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前公司未设置，无法停用供应商')
    return
  }
  try {
    await ElMessageBox.confirm('停用后该供应商将不能在提单页继续被选择，确定继续吗？', '停用供应商', {
      type: 'warning',
      confirmButtonText: '确定停用',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await financeArchiveApi.disableSupplier(currentCompanyId.value, vendorCode)
    ElMessage.success('供应商已停用')
    await loadVendors()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '停用供应商失败'))
  }
}

function supplierAccountText(row: FinanceVendorSummary) {
  const parts = [row.cVenBank, maskAccountNo(row.cVenAccount)].filter(Boolean)
  return parts.length ? parts.join(' / ') : '未维护收款账户'
}

function maskAccountNo(value?: string) {
  if (!value) {
    return ''
  }
  if (value.length <= 8) {
    return value
  }
  return `${value.slice(0, 4)} **** ${value.slice(-4)}`
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function confirmCompanySwitch() {
  if (!hasPendingEdit.value) {
    return true
  }
  try {
    await ElMessageBox.confirm('切换公司后将关闭当前供应商编辑窗口，并按新公司重新加载档案列表，是否继续？', '切换公司', {
      type: 'warning',
      confirmButtonText: '继续切换',
      cancelButtonText: '取消'
    })
    return true
  } catch {
    return false
  }
}
</script>
