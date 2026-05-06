<template>
  <div class="space-y-4">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">客户档案</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前公司</span>
            <strong>{{ currentCompanyName || '未设置' }}</strong>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <el-button :icon="RefreshRight" @click="loadCustomers(true)">刷新</el-button>
          <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openCreateDialog">新增客户</el-button>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),220px,160px]">
        <el-input v-model="keyword" clearable placeholder="请输入客户名称或编码" @keyup.enter="loadCustomers(true)">
          <template #append>
            <el-button :icon="Search" @click="loadCustomers(true)" />
          </template>
        </el-input>

        <el-switch
          v-model="includeDisabled"
          inline-prompt
          active-text="含停用"
          inactive-text="仅启用"
          @change="loadCustomers(true)"
        />

        <div class="flex justify-end">
          <el-button :icon="RefreshRight" @click="resetFilters">重置筛选</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="paginatedCustomers" style="width: 100%">
        <el-table-column prop="cCusCode" label="客户编码" width="170" />
        <el-table-column prop="cCusName" label="客户名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="cCusAbbName" label="简称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="cCusPerson" label="联系人" min-width="120" show-overflow-tooltip />
        <el-table-column prop="cCusHand" label="手机" min-width="140" show-overflow-tooltip />
        <el-table-column label="开户银行/账号" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ customerAccountText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="应收余额" min-width="140" align="right">
          <template #default="{ row }">
            <span class="font-semibold text-slate-700">¥ {{ formatListMoney(row.iARMoney) }}</span>
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
            <el-button v-if="canEdit" type="primary" link @click="openEditDialog(row.cCusCode)">编辑</el-button>
            <el-button v-if="canDisable && row.active" type="danger" link @click="disableCustomer(row.cCusCode)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 flex justify-start">
        <el-pagination
          v-model:current-page="customerPagination.currentPage.value"
          v-model:page-size="customerPagination.pageSize.value"
          layout="total, sizes, prev, pager, next"
          :total="customerPagination.total.value"
          :page-sizes="customerPagination.pageSizes"
        />
      </div>
    </el-card>

    <finance-customer-archive-dialog
      ref="customerDialogRef"
      :company-id="currentCompanyId"
      :company-name="currentCompanyName"
      @saved="handleCustomerSaved"
      @closed="dialogOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { financeArchiveApi, type FinanceCustomerSummary } from '@/api'
import FinanceCustomerArchiveDialog from '@/components/finance/FinanceCustomerArchiveDialog.vue'
import { useLocalPagination } from '@/composables/useLocalPagination'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { formatMoney } from '@/utils/money'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const keyword = ref('')
const includeDisabled = ref(false)
const customers = ref<FinanceCustomerSummary[]>([])
const customerPagination = useLocalPagination(customers)
const financeCompany = useFinanceCompanyStore()
const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const paginatedCustomers = computed(() => customerPagination.paginatedRows.value)
const canCreate = computed(() => hasPermission('finance:archives:customers:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:customers:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:customers:delete', permissionCodes.value))

const COMPANY_SWITCH_GUARD_KEY = 'finance-customer-archive'
const customerDialogRef = ref<InstanceType<typeof FinanceCustomerArchiveDialog> | null>(null)
const dialogOpen = ref(false)
const fallbackCustomerForm = reactive<Record<string, string | number | undefined>>({})
let guardRegistered = false

const customerForm = computed<Record<string, string | number | undefined>>(
  () => customerDialogRef.value?.customerForm || fallbackCustomerForm
)
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
    await loadCustomers(true)
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

async function loadCustomers(resetPage = false) {
  if (resetPage) {
    customerPagination.resetToFirstPage()
  }
  if (!currentCompanyId.value) {
    customers.value = []
    customerPagination.clampCurrentPage()
    return
  }
  loading.value = true
  try {
    const res = await financeArchiveApi.listCustomers({
      companyId: currentCompanyId.value,
      keyword: keyword.value.trim(),
      includeDisabled: includeDisabled.value
    })
    customers.value = res.data
    customerPagination.clampCurrentPage()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载客户档案失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  includeDisabled.value = false
  void loadCustomers(true)
}

function openCreateDialog() {
  dialogOpen.value = true
  customerDialogRef.value?.openCreateDialog()
}

async function openEditDialog(customerCode: string) {
  dialogOpen.value = true
  await customerDialogRef.value?.openEditDialog(customerCode)
}

async function saveCustomer() {
  await customerDialogRef.value?.saveCustomer()
}

function closeDialog() {
  dialogOpen.value = false
  customerDialogRef.value?.closeDialog()
}

async function handleCustomerSaved() {
  dialogOpen.value = false
  await loadCustomers()
}

async function disableCustomer(customerCode: string) {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前公司未设置，无法停用客户')
    return
  }
  try {
    await ElMessageBox.confirm('停用后该客户将不能在财务档案中作为启用客户继续使用，确定继续吗？', '停用客户', {
      type: 'warning',
      confirmButtonText: '确定停用',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await financeArchiveApi.disableCustomer(currentCompanyId.value, customerCode)
    ElMessage.success('客户已停用')
    await loadCustomers()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '停用客户失败'))
  }
}

function customerAccountText(row: FinanceCustomerSummary) {
  const parts = [row.cCusBank, maskAccountNo(row.cCusAccount)].filter(Boolean)
  return parts.length ? parts.join(' / ') : '未维护开户信息'
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

function formatListMoney(value?: string) {
  return formatMoney(value || '0.00')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function confirmCompanySwitch() {
  if (!hasPendingEdit.value) {
    return true
  }
  try {
    await ElMessageBox.confirm('切换公司后将关闭当前客户编辑窗口，并按新公司重新加载档案列表，是否继续？', '切换公司', {
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
