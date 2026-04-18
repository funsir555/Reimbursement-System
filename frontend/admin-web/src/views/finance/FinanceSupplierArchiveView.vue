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

    <el-dialog
      v-model="dialogVisible"
      :title="editingVendorCode ? '编辑供应商档案' : '新增供应商档案'"
      width="1180px"
      destroy-on-close
    >
      <el-form label-position="top" class="space-y-5">
        <el-collapse v-model="activeSections">
          <el-collapse-item v-for="section in vendorSections" :key="section.key" :name="section.key">
            <template #title>
              <span class="text-base font-semibold text-slate-800">{{ section.label }}</span>
            </template>

            <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
              <div v-if="section.key === 'basic'" class="xl:col-span-3">
                <div class="rounded-2xl border border-sky-100 bg-sky-50/80 px-4 py-3 text-sm text-sky-700">
                  当前维护公司：<strong>{{ currentCompanyName || currentCompanyId || '未设置' }}</strong>
                </div>
              </div>
              <div v-if="section.key === 'bank'" class="xl:col-span-3">
                <SupplierPaymentInfoFields
                  :form-state="vendorForm"
                  auto-fill-source-key="cVenName"
                  account-name-label="账户名"
                  business-scope="PUBLIC"
                />
              </div>
              <template v-for="field in section.fields" :key="field.key">
                <el-form-item
                  :label="field.label"
                  class="!mb-0"
                  :class="field.span === 2 ? 'xl:col-span-2' : field.span === 3 ? 'xl:col-span-3' : ''"
                >
                  <el-input
                    v-if="field.type === 'text'"
                    v-model="vendorForm[field.key]"
                    :placeholder="`请输入${field.label}`"
                    :maxlength="field.maxLength"
                  />
                  <el-input
                    v-else-if="field.type === 'textarea'"
                    v-model="vendorForm[field.key]"
                    type="textarea"
                    :rows="3"
                    :placeholder="`请输入${field.label}`"
                  />
                  <money-input
                    v-else-if="field.type === 'money'"
                    :model-value="toMoneyModelValue(vendorForm[field.key])"
                    @update:model-value="vendorForm[field.key] = $event"
                  />
                  <el-input-number v-else-if="field.type === 'number'" v-model="vendorForm[field.key]" :controls="false" class="w-full" />
                  <el-date-picker
                    v-else-if="field.type === 'date'"
                    v-model="vendorForm[field.key]"
                    type="date"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    class="w-full"
                    placeholder="请选择日期"
                  />
                  <el-switch
                    v-else-if="field.type === 'switch'"
                    v-model="vendorForm[field.key]"
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
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveSupplier">保存供应商</el-button>
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
  type FinanceVendorSavePayload,
  type FinanceVendorSummary
} from '@/api'
import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useLocalPagination } from '@/composables/useLocalPagination'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type VendorFieldType = 'text' | 'textarea' | 'number' | 'money' | 'date' | 'switch'
type VendorFieldConfig = {
  key: string
  label: string
  type: VendorFieldType
  span?: 1 | 2 | 3
  maxLength?: number
}

const VENDOR_FIELD_MAX_LENGTH: Record<string, number> = {
  cVenCode: 64,
  cVenName: 128,
  cVenAbbName: 64,
  cVCCode: 64,
  cVenBank: 128,
  cVenAccount: 64,
  receiptAccountName: 128,
  receiptBranchName: 128,
  cVenPerson: 64,
  cVenPhone: 32,
  cVenHand: 32,
  cVenBP: 32,
  cVenFax: 32,
  cVenLPerson: 64,
  cVenPostCode: 16,
  cVenPPerson: 64,
  cDCCode: 64,
  cRelCustomer: 64,
  cVenBankCode: 64,
  cVenHeadCode: 64,
  cVenPayCond: 64,
  cVenTradeCCode: 64,
  cVenWhCode: 64
}

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const includeDisabled = ref(false)
const vendors = ref<FinanceVendorSummary[]>([])
const vendorPagination = useLocalPagination(vendors)
const editingVendorCode = ref('')
const activeSections = ref(['basic', 'bank'])
const vendorForm = reactive<Record<string, string | number | undefined>>({})
const financeCompany = useFinanceCompanyStore()
const COMPANY_SWITCH_GUARD_KEY = 'finance-supplier-archive'
let guardRegistered = false

const canCreate = computed(() => hasPermission('finance:archives:suppliers:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:suppliers:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:suppliers:delete', permissionCodes.value))
const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const hasPendingEdit = computed(() => dialogVisible.value)
const paginatedVendors = computed(() => vendorPagination.paginatedRows.value)

const vendorSections: Array<{ key: string; label: string; fields: VendorFieldConfig[] }> = [
  {
    key: 'basic',
    label: '基础信息',
    fields: [
      { key: 'cVenCode', label: '供应商编码', type: 'text', maxLength: 64 },
      { key: 'cVenName', label: '供应商名称', type: 'text', maxLength: 128 },
      { key: 'cVenAbbName', label: '供应商简称', type: 'text', maxLength: 64 },
      { key: 'cVCCode', label: '分类编码', type: 'text', maxLength: 64 },
      { key: 'cTrade', label: '行业', type: 'text' },
      { key: 'cVenRegCode', label: '工商注册号', type: 'text' },
      { key: 'cBarCode', label: '条形码', type: 'text' },
      { key: 'cMemo', label: '备注', type: 'textarea', span: 3 }
    ]
  },
  {
    key: 'bank',
    label: '收款与税务',
    fields: [
      { key: 'cTaxCode', label: '税号', type: 'text' },
      { key: 'cVenDCode', label: '地区编码', type: 'text' },
      { key: 'cPayCode', label: '付款条件编码', type: 'text' },
      { key: 'cSCCode', label: '结算方式编码', type: 'text' },
      { key: 'bVenTax', label: '一般纳税人', type: 'switch' },
      { key: 'bProxyVen', label: '代理供应商', type: 'switch' },
      { key: 'bImportVen', label: '进口供应商', type: 'switch' },
      { key: 'bVenOverseas', label: '境外供应商', type: 'switch' },
      { key: 'cVenMne', label: '助记码', type: 'text' }
    ]
  },
  {
    key: 'contact',
    label: '联系信息',
    fields: [
      { key: 'cVenPerson', label: '\u8054\u7cfb\u4eba', type: 'text', maxLength: 64 },
      { key: 'cVenPhone', label: '\u8054\u7cfb\u7535\u8bdd', type: 'text', maxLength: 32 },
      { key: 'cVenHand', label: '\u624b\u673a', type: 'text', maxLength: 32 },
      { key: 'cVenEmail', label: '电子邮箱', type: 'text' },
      { key: 'cVenAddress', label: '联系地址', type: 'text', span: 2 },
      { key: 'cVenIAddress', label: '开票地址', type: 'text', span: 2 },
      { key: 'cVenPostCode', label: '\u90ae\u653f\u7f16\u7801', type: 'text', maxLength: 16 },
      { key: 'cVenBP', label: '\u547c\u673a', type: 'text', maxLength: 32 },
      { key: 'cVenFax', label: '\u4f20\u771f', type: 'text', maxLength: 32 },
      { key: 'cVenLPerson', label: '\u6cd5\u4eba\u4ee3\u8868', type: 'text', maxLength: 64 },
      { key: 'cVenPPerson', label: '\u91c7\u8d2d\u8054\u7cfb\u4eba', type: 'text', maxLength: 64 },
      { key: 'cVenDepart', label: '所属部门', type: 'text' }
    ]
  },
  {
    key: 'finance',
    label: '财务与扩展',
    fields: [
      { key: 'fRegistFund', label: '注册资金', type: 'money' },
      { key: 'iAPMoney', label: '应付余额', type: 'money' },
      { key: 'iLastMoney', label: '上次交易金额', type: 'money' },
      { key: 'iLRMoney', label: '最近收款金额', type: 'money' },
      { key: 'iVenCreLine', label: '信用额度', type: 'money' },
      { key: 'iVenDisRate', label: '折扣率', type: 'number' },
      { key: 'iVenCreGrade', label: '信用等级', type: 'text' },
      { key: 'iVenCreDate', label: '信用天数', type: 'number' },
      { key: 'iBusinessADays', label: '营业执照有效天数', type: 'number' },
      { key: 'iLicenceADays', label: '许可证有效天数', type: 'number' },
      { key: 'iProxyADays', label: '代理授权有效天数', type: 'number' },
      { key: 'iEmployeeNum', label: '员工人数', type: 'number' },
      { key: 'iFrequency', label: '交易频次', type: 'number' },
      { key: 'iGradeABC', label: 'ABC 分类', type: 'number' },
      { key: 'iId', label: '内部编号', type: 'number' },
      { key: 'cRelCustomer', label: '关联客户', type: 'text', maxLength: 64 },
      { key: 'cVenHeadCode', label: '上级供应商编码', type: 'text', maxLength: 64 },
      { key: 'cDCCode', label: '地区编码', type: 'text', maxLength: 64 },
      { key: 'cVenTradeCCode', label: '行业分类', type: 'text', maxLength: 64 },
      { key: 'cVenDefine3', label: '自定义项3', type: 'text' },
      { key: 'cVenDefine4', label: '自定义项4', type: 'text' },
      { key: 'cVenDefine5', label: '自定义项5', type: 'text' },
      { key: 'cVenDefine6', label: '自定义项6', type: 'text' },
      { key: 'cVenDefine7', label: '自定义项7', type: 'text' },
      { key: 'cVenDefine8', label: '自定义项8', type: 'text' },
      { key: 'cVenDefine9', label: '自定义项9', type: 'text' },
      { key: 'cVenDefine10', label: '自定义项10', type: 'text' },
      { key: 'cVenDefine11', label: '自定义项11', type: 'number' },
      { key: 'cVenDefine12', label: '自定义项12', type: 'number' },
      { key: 'cVenDefine13', label: '自定义项13', type: 'number' },
      { key: 'cVenDefine14', label: '自定义项14', type: 'number' },
      { key: 'cVenDefine15', label: '自定义项15', type: 'date' },
      { key: 'cVenDefine16', label: '自定义项16', type: 'date' },
      { key: 'dLastDate', label: '最近交易日期', type: 'date' },
      { key: 'dLRDate', label: '最近收款日期', type: 'date' },
      { key: 'dModifyDate', label: '最近修改日期', type: 'date' }
    ]
  }
]

const numericFields = new Set(
  vendorSections.flatMap((section) => section.fields.filter((field) => field.type === 'number').map((field) => field.key))
)
const moneyFields = new Set(
  vendorSections.flatMap((section) => section.fields.filter((field) => field.type === 'money').map((field) => field.key))
)
const dateFields = new Set(
  vendorSections.flatMap((section) => section.fields.filter((field) => field.type === 'date').map((field) => field.key))
)

const defaultSwitchFields = vendorSections
  .flatMap((section) => section.fields)
  .filter((field) => field.type === 'switch')
  .map((field) => field.key)
const paymentFieldKeys = [
  'receiptAccountName',
  'cVenBankCode',
  'cVenBank',
  'cVenAccount',
  'receiptBankProvince',
  'receiptBankCity',
  'receiptBranchCode',
  'receiptBranchName'
] as const
const allowedVendorFieldKeys = new Set([
  ...vendorSections.flatMap((section) => section.fields.map((field) => field.key)),
  ...paymentFieldKeys
])
const vendorFieldLabels: Record<string, string> = {
  ...vendorSections.flatMap((section) => section.fields).reduce<Record<string, string>>((acc, field) => {
    acc[field.key] = field.label
    return acc
  }, {}),
  receiptAccountName: '账户名',
  cVenBankCode: '开户银行编码',
  cVenBank: '开户银行',
  cVenAccount: '银行账号',
  receiptBankProvince: '开户省',
  receiptBankCity: '开户市',
  receiptBranchCode: '开户网点编码',
  receiptBranchName: '开户网点'
}

resetVendorForm()
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

function resetVendorForm() {
  Object.keys(vendorForm).forEach((key) => {
    delete vendorForm[key]
  })
  vendorSections.forEach((section) => {
    section.fields.forEach((field) => {
      vendorForm[field.key] = field.type === 'switch' ? 0 : undefined
    })
  })
  paymentFieldKeys.forEach((key) => {
    vendorForm[key] = undefined
  })
}

function openCreateDialog() {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前公司未设置，无法维护供应商')
    return
  }
  editingVendorCode.value = ''
  resetVendorForm()
  activeSections.value = ['basic', 'bank']
  dialogVisible.value = true
}

async function openEditDialog(vendorCode: string) {
  if (!currentCompanyId.value) return
  dialogVisible.value = true
  editingVendorCode.value = vendorCode
  resetVendorForm()
  saving.value = false
  try {
    const res = await financeArchiveApi.getSupplierDetail(currentCompanyId.value, vendorCode)
    Object.entries(res.data).forEach(([key, value]) => {
      if (allowedVendorFieldKeys.has(key)) {
        vendorForm[key] = value as string | number | undefined
      }
    })
    defaultSwitchFields.forEach((key) => {
      vendorForm[key] = Number(vendorForm[key] || 0)
    })
    activeSections.value = ['basic', 'bank']
  } catch (error: unknown) {
    dialogVisible.value = false
    ElMessage.error(resolveErrorMessage(error, '加载供应商详情失败'))
  }
}

async function saveSupplier() {
  if (!validateVendorForm()) return
  saving.value = true
  try {
    const payload = buildVendorPayload()
    if (editingVendorCode.value) {
      await financeArchiveApi.updateSupplier(currentCompanyId.value, editingVendorCode.value, payload)
      ElMessage.success('供应商档案已更新')
    } else {
      await financeArchiveApi.createSupplier(currentCompanyId.value, payload)
      ElMessage.success('供应商档案已创建')
    }
    dialogVisible.value = false
    await loadVendors()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存供应商档案失败'))
  } finally {
    saving.value = false
  }
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

function validateVendorForm() {
  if (!String(vendorForm.cVenName || '').trim()) {
    ElMessage.warning('请先填写供应商名称')
    return false
  }
  for (const [fieldKey, maxLength] of Object.entries(VENDOR_FIELD_MAX_LENGTH)) {
    const value = vendorForm[fieldKey]
    if (typeof value !== 'string') {
      continue
    }
    const normalized = value.trim()
    if (normalized && normalized.length > maxLength) {
      ElMessage.warning(`${vendorFieldLabels[fieldKey] || fieldKey}最多 ${maxLength} 个字符`)
      return false
    }
  }
  return true
}

function buildVendorPayload(): FinanceVendorSavePayload {
  const payload: Record<string, unknown> = {
    companyId: currentCompanyId.value
  }
  Array.from(allowedVendorFieldKeys).forEach((key) => {
    const value = vendorForm[key]
    const normalizedValue = typeof value === 'string' ? value.trim() : value
    if (key === 'companyId') {
      return
    }
    if (normalizedValue === undefined || normalizedValue === null || normalizedValue === '') {
      return
    }
    if (moneyFields.has(key)) {
      payload[key] = String(normalizedValue)
      return
    }
    if (numericFields.has(key)) {
      const numericValue = Number(normalizedValue)
      if (!Number.isNaN(numericValue)) {
        payload[key] = numericValue
      }
      return
    }
    if (dateFields.has(key)) {
      payload[key] = String(normalizedValue)
      return
    }
    payload[key] = normalizedValue
  })
  return payload as FinanceVendorSavePayload
}

function closeDialog() {
  dialogVisible.value = false
  editingVendorCode.value = ''
  resetVendorForm()
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
