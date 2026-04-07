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
          <el-button :icon="RefreshRight" @click="loadVendors">刷新</el-button>
          <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openCreateDialog">新增供应商</el-button>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),220px,160px]">
        <el-input v-model="keyword" clearable placeholder="请输入供应商名称或编码" @keyup.enter="loadVendors">
          <template #append>
            <el-button :icon="Search" @click="loadVendors" />
          </template>
        </el-input>

        <el-switch
          v-model="includeDisabled"
          inline-prompt
          active-text="含停用"
          inactive-text="仅启用"
          @change="loadVendors"
        />

        <div class="flex justify-end">
          <el-button :icon="RefreshRight" @click="resetFilters">重置筛选</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="vendors" style="width: 100%">
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
              <template v-for="field in section.fields" :key="field.key">
                <el-form-item
                  :label="field.label"
                  class="!mb-0"
                  :class="field.span === 2 ? 'xl:col-span-2' : field.span === 3 ? 'xl:col-span-3' : ''"
                >
                  <el-input v-if="field.type === 'text'" v-model="vendorForm[field.key]" :placeholder="`请输入${field.label}`" />
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
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type VendorFieldType = 'text' | 'textarea' | 'number' | 'money' | 'date' | 'switch'
type VendorFieldConfig = {
  key: string
  label: string
  type: VendorFieldType
  span?: 1 | 2 | 3
}

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const includeDisabled = ref(false)
const vendors = ref<FinanceVendorSummary[]>([])
const editingVendorCode = ref('')
const activeSections = ref(['basic', 'contact', 'bank', 'finance'])
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

const vendorSections: Array<{ key: string; label: string; fields: VendorFieldConfig[] }> = [
  {
    key: 'basic',
    label: '基础信息',
    fields: [
      { key: 'cVenCode', label: '供应商编码', type: 'text' },
      { key: 'cVenName', label: '供应商名称', type: 'text' },
      { key: 'cVenAbbName', label: '供应商简称', type: 'text' },
      { key: 'cVCCode', label: '分类编码', type: 'text' },
      { key: 'cTrade', label: '行业', type: 'text' },
      { key: 'cVenRegCode', label: '工商注册号', type: 'text' },
      { key: 'cBarCode', label: '条形码', type: 'text' },
      { key: 'cMemo', label: '备注', type: 'textarea', span: 3 }
    ]
  },
  {
    key: 'contact',
    label: '联系信息',
    fields: [
      { key: 'cVenPerson', label: '联系人', type: 'text' },
      { key: 'cVenPhone', label: '联系电话', type: 'text' },
      { key: 'cVenHand', label: '手机', type: 'text' },
      { key: 'cVenEmail', label: '电子邮箱', type: 'text' },
      { key: 'cVenAddress', label: '联系地址', type: 'text', span: 2 },
      { key: 'cVenIAddress', label: '开票地址', type: 'text', span: 2 },
      { key: 'cVenPostCode', label: '邮政编码', type: 'text' },
      { key: 'cVenBP', label: '邮箱/邮编', type: 'text' },
      { key: 'cVenFax', label: '传真', type: 'text' },
      { key: 'cVenLPerson', label: '法人代表', type: 'text' },
      { key: 'cVenPPerson', label: '采购联系人', type: 'text' },
      { key: 'cVenDepart', label: '所属部门', type: 'text' }
    ]
  },
  {
    key: 'bank',
    label: '收款与税务',
    fields: [
      { key: 'cVenBank', label: '开户银行', type: 'text' },
      { key: 'cVenAccount', label: '银行账号', type: 'text' },
      { key: 'cTaxCode', label: '税号', type: 'text' },
      { key: 'cVenDCode', label: '地区编码', type: 'text' },
      { key: 'cVenCCCCode', label: '联行号', type: 'text' },
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
      { key: 'cRelCustomer', label: '关联客户', type: 'text' },
      { key: 'cVenHeadCode', label: '上级供应商编码', type: 'text' },
      { key: 'cDCCode', label: '地区编码', type: 'text' },
      { key: 'cVenTradeCCode', label: '行业分类', type: 'text' },
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
    await loadVendors()
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

async function loadVendors() {
  if (!currentCompanyId.value) {
    vendors.value = []
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
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载供应商档案失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  includeDisabled.value = false
  void loadVendors()
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
}

function openCreateDialog() {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前公司未设置，无法维护供应商')
    return
  }
  editingVendorCode.value = ''
  resetVendorForm()
  activeSections.value = ['basic', 'contact', 'bank', 'finance']
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
      vendorForm[key] = value as string | number | undefined
    })
    defaultSwitchFields.forEach((key) => {
      vendorForm[key] = Number(vendorForm[key] || 0)
    })
    activeSections.value = ['basic', 'contact', 'bank', 'finance']
  } catch (error: unknown) {
    dialogVisible.value = false
    ElMessage.error(resolveErrorMessage(error, '加载供应商详情失败'))
  }
}

async function saveSupplier() {
  if (!String(vendorForm.cVenName || '').trim()) {
    ElMessage.warning('请先填写供应商名称')
    return
  }
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

function buildVendorPayload(): FinanceVendorSavePayload {
  const payload: Record<string, unknown> = {
    companyId: currentCompanyId.value
  }
  Object.entries(vendorForm).forEach(([key, value]) => {
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
