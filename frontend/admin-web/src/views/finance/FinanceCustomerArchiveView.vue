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

    <el-dialog
      v-model="dialogVisible"
      :title="editingCustomerCode ? '编辑客户档案' : '新增客户档案'"
      width="1180px"
      destroy-on-close
    >
      <el-form label-position="top" class="space-y-5">
        <el-collapse v-model="activeSections">
          <el-collapse-item v-for="section in customerSections" :key="section.key" :name="section.key">
            <template #title>
              <span class="text-base font-semibold text-slate-800">{{ section.label }}</span>
            </template>

            <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
              <div v-if="section.key === 'basic'" class="xl:col-span-3">
                <div class="rounded-2xl border border-sky-100 bg-sky-50/80 px-4 py-3 text-sm text-sky-700">
                  当前维护公司：<strong>{{ currentCompanyName || currentCompanyId || '未设置' }}</strong>
                </div>
              </div>
              <template v-for="field in section.fields" :key="field.key">
                <el-form-item
                  :label="field.label"
                  class="!mb-0"
                  :class="field.span === 2 ? 'xl:col-span-2' : field.span === 3 ? 'xl:col-span-3' : ''"
                >
                  <el-input
                    v-if="field.type === 'text'"
                    v-model="customerForm[field.key]"
                    :placeholder="`请输入${field.label}`"
                    :maxlength="field.maxLength"
                  />
                  <el-input
                    v-else-if="field.type === 'textarea'"
                    v-model="customerForm[field.key]"
                    type="textarea"
                    :rows="3"
                    :placeholder="`请输入${field.label}`"
                  />
                  <money-input
                    v-else-if="field.type === 'money'"
                    :model-value="toMoneyModelValue(customerForm[field.key])"
                    @update:model-value="customerForm[field.key] = $event"
                  />
                  <el-input-number
                    v-else-if="field.type === 'number'"
                    v-model="customerForm[field.key]"
                    :controls="false"
                    class="w-full"
                  />
                  <el-input-number
                    v-else-if="field.type === 'decimal'"
                    v-model="customerForm[field.key]"
                    :controls="false"
                    :precision="2"
                    :step="0.01"
                    class="w-full"
                  />
                  <el-date-picker
                    v-else-if="field.type === 'date'"
                    v-model="customerForm[field.key]"
                    type="date"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    class="w-full"
                    placeholder="请选择日期"
                  />
                  <el-switch
                    v-else-if="field.type === 'switch'"
                    v-model="customerForm[field.key]"
                    :active-value="1"
                    :inactive-value="0"
                    inline-prompt
                    active-text="是"
                    inactive-text="否"
                  />
                </el-form-item>
              </template>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveCustomer">保存客户</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import {
  financeArchiveApi,
  type FinanceCustomerSavePayload,
  type FinanceCustomerSummary
} from '@/api'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useLocalPagination } from '@/composables/useLocalPagination'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { formatMoney } from '@/utils/money'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type CustomerFieldType = 'text' | 'textarea' | 'number' | 'decimal' | 'money' | 'date' | 'switch'
type CustomerFieldConfig = {
  key: string
  label: string
  type: CustomerFieldType
  span?: 1 | 2 | 3
  maxLength?: number
}

const CUSTOMER_FIELD_MAX_LENGTH: Record<string, number> = {
  cCusCode: 64,
  cCusName: 128,
  cCusAbbName: 64,
  cCCCode: 64,
  cDCCode: 64,
  cCusTradeCCode: 64,
  cCusPostCode: 16,
  cCusBank: 128,
  cCusAccount: 64,
  cCusLPerson: 64,
  cCusPerson: 64,
  cCusHand: 32,
  cCusHeadCode: 64,
  cCusWhCode: 64,
  cCusDepart: 64,
  cCusBankCode: 64,
  customerKCode: 64
}

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const includeDisabled = ref(false)
const customers = ref<FinanceCustomerSummary[]>([])
const customerPagination = useLocalPagination(customers)
const editingCustomerCode = ref('')
const activeSections = ref(['basic', 'contact', 'bank', 'finance'])
const customerForm = reactive<Record<string, string | number | undefined>>({})
const financeCompany = useFinanceCompanyStore()
const COMPANY_SWITCH_GUARD_KEY = 'finance-customer-archive'
let guardRegistered = false

const canCreate = computed(() => hasPermission('finance:archives:customers:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:customers:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:customers:delete', permissionCodes.value))
const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const hasPendingEdit = computed(() => dialogVisible.value)
const paginatedCustomers = computed(() => customerPagination.paginatedRows.value)

const customerSections: Array<{ key: string; label: string; fields: CustomerFieldConfig[] }> = [
  {
    key: 'basic',
    label: '基础信息',
    fields: [
      { key: 'cCusCode', label: '客户编码', type: 'text', maxLength: 64 },
      { key: 'cCusName', label: '客户名称', type: 'text', maxLength: 128 },
      { key: 'cCusAbbName', label: '客户简称', type: 'text', maxLength: 64 },
      { key: 'cCCCode', label: '分类编码', type: 'text', maxLength: 64 },
      { key: 'cTrade', label: '所属行业', type: 'text' },
      { key: 'cCusRegCode', label: '纳税人登记号', type: 'text' },
      { key: 'cMemo', label: '备注', type: 'textarea', span: 3 }
    ]
  },
  {
    key: 'contact',
    label: '联系与发运',
    fields: [
      { key: 'cCusPerson', label: '\u8054\u7cfb\u4eba', type: 'text', maxLength: 64 },
      { key: 'cCusHand', label: '\u624b\u673a', type: 'text', maxLength: 32 },
      { key: 'cCusAddress', label: '联系地址', type: 'text', span: 2 },
      { key: 'cCusPostCode', label: '\u90ae\u653f\u7f16\u7801', type: 'text', maxLength: 16 },
      { key: 'cCusOAddress', label: '发货地址', type: 'text', span: 2 },
      { key: 'cCusOType', label: '发运方式', type: 'text' },
      { key: 'cCusDepart', label: '分管部门', type: 'text', maxLength: 64 },
      { key: 'cCusWhCode', label: '发货仓库', type: 'text', maxLength: 64 },
      { key: 'cCusHeadCode', label: '客户总公司编码', type: 'text', maxLength: 64 },
      { key: 'cCusTradeCCode', label: '行业编码', type: 'text', maxLength: 64 },
      { key: 'cDCCode', label: '地区编码', type: 'text', maxLength: 64 }
    ]
  },
  {
    key: 'bank',
    label: '银行与开票',
    fields: [
      { key: 'cCusBank', label: '\u5f00\u6237\u94f6\u884c', type: 'text', maxLength: 128 },
      { key: 'cCusAccount', label: '\u94f6\u884c\u8d26\u53f7', type: 'text', maxLength: 64 },
      { key: 'cCusBankCode', label: '所属银行编码', type: 'text', maxLength: 64 },
      { key: 'cInvoiceCompany', label: '开票单位', type: 'text' },
      { key: 'cCusLPerson', label: '\u6cd5\u4eba\u4ee3\u8868', type: 'text', maxLength: 64 }
    ]
  },
  {
    key: 'finance',
    label: '财务与扩展',
    fields: [
      { key: 'iCusCreLine', label: '信用额度', type: 'money' },
      { key: 'iARMoney', label: '应收余额', type: 'money' },
      { key: 'iLastMoney', label: '最后交易金额', type: 'money' },
      { key: 'iLRMoney', label: '最后收款金额', type: 'money' },
      { key: 'iCusCreDate', label: '信用期限', type: 'number' },
      { key: 'cCusCreGrade', label: '信用等级', type: 'text' },
      { key: 'dLastDate', label: '最后交易日期', type: 'date' },
      { key: 'dLRDate', label: '最后收款日期', type: 'date' },
      { key: 'bCredit', label: '控制信用', type: 'switch' },
      { key: 'bCreditDate', label: '控制信用期限', type: 'switch' },
      { key: 'bCreditByHead', label: '按总公司控信', type: 'switch' },
      { key: 'fCommisionRate', label: '佣金比率(%)', type: 'decimal' },
      { key: 'fInsueRate', label: '保险费率(%)', type: 'decimal' },
      { key: 'customerKCode', label: '客户级别编码', type: 'text', maxLength: 64 },
      { key: 'bCusState', label: '是否成交', type: 'switch' },
      { key: 'cCusDefine1', label: '自定义项1', type: 'text' },
      { key: 'cCusDefine2', label: '自定义项2', type: 'text' },
      { key: 'cCusDefine3', label: '自定义项3', type: 'text' },
      { key: 'cCusDefine4', label: '自定义项4', type: 'text' },
      { key: 'cCusDefine5', label: '自定义项5', type: 'text' },
      { key: 'cCusDefine6', label: '自定义项6', type: 'text' },
      { key: 'cCusDefine7', label: '自定义项7', type: 'text' },
      { key: 'cCusDefine8', label: '自定义项8', type: 'text' },
      { key: 'cCusDefine9', label: '自定义项9', type: 'text' },
      { key: 'cCusDefine10', label: '自定义项10', type: 'text' },
      { key: 'cCusDefine11', label: '自定义项11', type: 'number' },
      { key: 'cCusDefine12', label: '自定义项12', type: 'number' },
      { key: 'cCusDefine13', label: '自定义项13', type: 'decimal' },
      { key: 'cCusDefine14', label: '自定义项14', type: 'decimal' },
      { key: 'cCusDefine15', label: '自定义项15', type: 'date' },
      { key: 'cCusDefine16', label: '自定义项16', type: 'date' }
    ]
  }
]

const numericFields = new Set(
  customerSections.flatMap((section) =>
    section.fields.filter((field) => field.type === 'number' || field.type === 'decimal').map((field) => field.key)
  )
)
const moneyFields = new Set(
  customerSections.flatMap((section) => section.fields.filter((field) => field.type === 'money').map((field) => field.key))
)
const dateFields = new Set(
  customerSections.flatMap((section) => section.fields.filter((field) => field.type === 'date').map((field) => field.key))
)
const defaultSwitchFields = customerSections
  .flatMap((section) => section.fields)
  .filter((field) => field.type === 'switch')
  .map((field) => field.key)
const allowedFormFieldKeys = new Set(customerSections.flatMap((section) => section.fields.map((field) => field.key)))
const customerFieldLabels = customerSections.flatMap((section) => section.fields).reduce<Record<string, string>>((acc, field) => {
  acc[field.key] = field.label
  return acc
}, {})

resetCustomerForm()
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

function resetCustomerForm() {
  Object.keys(customerForm).forEach((key) => {
    delete customerForm[key]
  })
  customerSections.forEach((section) => {
    section.fields.forEach((field) => {
      customerForm[field.key] = field.type === 'switch' ? 0 : undefined
    })
  })
}

function openCreateDialog() {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前公司未设置，无法维护客户')
    return
  }
  editingCustomerCode.value = ''
  resetCustomerForm()
  activeSections.value = ['basic', 'contact', 'bank', 'finance']
  dialogVisible.value = true
}

async function openEditDialog(customerCode: string) {
  if (!currentCompanyId.value) return
  dialogVisible.value = true
  editingCustomerCode.value = customerCode
  resetCustomerForm()
  saving.value = false
  try {
    const res = await financeArchiveApi.getCustomerDetail(currentCompanyId.value, customerCode)
    Object.entries(res.data).forEach(([key, value]) => {
      if (allowedFormFieldKeys.has(key)) {
        customerForm[key] = value as string | number | undefined
      }
    })
    defaultSwitchFields.forEach((key) => {
      customerForm[key] = Number(customerForm[key] || 0)
    })
    activeSections.value = ['basic', 'contact', 'bank', 'finance']
  } catch (error: unknown) {
    dialogVisible.value = false
    ElMessage.error(resolveErrorMessage(error, '加载客户详情失败'))
  }
}

async function saveCustomer() {
  if (!validateCustomerForm()) return
  saving.value = true
  try {
    const payload = buildCustomerPayload()
    if (editingCustomerCode.value) {
      await financeArchiveApi.updateCustomer(currentCompanyId.value, editingCustomerCode.value, payload)
      ElMessage.success('客户档案已更新')
    } else {
      await financeArchiveApi.createCustomer(currentCompanyId.value, payload)
      ElMessage.success('客户档案已创建')
    }
    dialogVisible.value = false
    await loadCustomers()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存客户档案失败'))
  } finally {
    saving.value = false
  }
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

function validateCustomerForm() {
  if (!String(customerForm.cCusName || '').trim()) {
    ElMessage.warning('请先填写客户名称')
    return false
  }
  for (const [fieldKey, maxLength] of Object.entries(CUSTOMER_FIELD_MAX_LENGTH)) {
    const value = customerForm[fieldKey]
    if (typeof value !== 'string') {
      continue
    }
    const normalized = value.trim()
    if (normalized && normalized.length > maxLength) {
      ElMessage.warning(`${customerFieldLabels[fieldKey] || fieldKey}最多 ${maxLength} 个字符`)
      return false
    }
  }
  return true
}

function buildCustomerPayload(): FinanceCustomerSavePayload {
  const payload: Record<string, unknown> = {
    companyId: currentCompanyId.value
  }
  Array.from(allowedFormFieldKeys).forEach((key) => {
    const rawValue = customerForm[key]
    const value = typeof rawValue === 'string' ? rawValue.trim() : rawValue
    if (key === 'companyId') {
      return
    }
    if (value === undefined || value === null || value === '') {
      return
    }
    if (moneyFields.has(key)) {
      payload[key] = String(value)
      return
    }
    if (numericFields.has(key)) {
      const numericValue = Number(value)
      if (!Number.isNaN(numericValue)) {
        payload[key] = numericValue
      }
      return
    }
    if (dateFields.has(key)) {
      payload[key] = String(value)
      return
    }
    payload[key] = value
  })
  return payload as FinanceCustomerSavePayload
}

function closeDialog() {
  dialogVisible.value = false
  editingCustomerCode.value = ''
  resetCustomerForm()
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

function toMoneyModelValue(value: string | number | undefined) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }
  return typeof value === 'string' ? value : undefined
}
</script>

<style scoped>
:deep(.el-collapse-item__header) {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

:deep(.el-collapse-item__content) {
  padding-bottom: 4px;
}
</style>
