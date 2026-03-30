<template>
  <div class="space-y-6">
    <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 class="text-3xl font-bold text-slate-800">供应商档案</h1>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
            档案数据直接维护在 <code>gl_Vender</code>，提单页的往来单位、收款人和收款账户都会读取这里的数据。
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <el-button :icon="RefreshRight" @click="loadVendors">刷新</el-button>
          <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openCreateDialog">新增供应商</el-button>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),220px,160px]">
        <el-input v-model="keyword" clearable placeholder="搜索编码、名称、简称" @keyup.enter="loadVendors">
          <template #append>
            <el-button :icon="Search" @click="loadVendors" />
          </template>
        </el-input>

        <el-switch
          v-model="includeDisabled"
          inline-prompt
          active-text="含停用"
          inactive-text="仅有效"
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
              {{ row.active ? '有效' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit" type="primary" link @click="openEditDialog(row.cVenCode)">编辑</el-button>
            <el-button
              v-if="canDisable && row.active"
              type="danger"
              link
              @click="disableSupplier(row.cVenCode)"
            >
              停用
            </el-button>
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
          <el-collapse-item
            v-for="section in vendorSections"
            :key="section.key"
            :name="section.key"
          >
            <template #title>
              <span class="text-base font-semibold text-slate-800">{{ section.label }}</span>
            </template>

            <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
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
                  />
                  <el-input
                    v-else-if="field.type === 'textarea'"
                    v-model="vendorForm[field.key]"
                    type="textarea"
                    :rows="3"
                    :placeholder="`请输入${field.label}`"
                  />
                  <el-input-number
                    v-else-if="field.type === 'number'"
                    v-model="vendorForm[field.key]"
                    :controls="false"
                    class="w-full"
                  />
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
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import {
  financeArchiveApi,
  type FinanceVendorSavePayload,
  type FinanceVendorSummary
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type VendorFieldType = 'text' | 'textarea' | 'number' | 'date' | 'switch'
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
const activeSections = ref(['basic', 'contact', 'bank', 'qualification'])
const vendorForm = reactive<Record<string, string | number | undefined>>({})

const canCreate = computed(() => hasPermission('finance:archives:suppliers:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:suppliers:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:suppliers:delete', permissionCodes.value))

const vendorSections: Array<{ key: string; label: string; fields: VendorFieldConfig[] }> = [
  {
    key: 'basic',
    label: '基础信息',
    fields: [
      { key: 'cVenCode', label: '供应商编码', type: 'text' },
      { key: 'cVenName', label: '供应商名称', type: 'text' },
      { key: 'cVenAbbName', label: '供应商简称', type: 'text' },
      { key: 'cVCCode', label: '供应商分类编码', type: 'text' },
      { key: 'cTrade', label: '所属行业', type: 'text' },
      { key: 'companyId', label: '公司主体编码', type: 'text' },
      { key: 'cVenRegCode', label: '纳税人登记号', type: 'text' },
      { key: 'cBarCode', label: '条形码', type: 'text' },
      { key: 'cMemo', label: '备注', type: 'textarea', span: 3 }
    ]
  },
  {
    key: 'contact',
    label: '联系方式',
    fields: [
      { key: 'cVenPerson', label: '联系人', type: 'text' },
      { key: 'cVenPhone', label: '电话', type: 'text' },
      { key: 'cVenHand', label: '手机', type: 'text' },
      { key: 'cVenEmail', label: '邮箱', type: 'text' },
      { key: 'cVenAddress', label: '地址', type: 'text', span: 2 },
      { key: 'cVenIAddress', label: '到货地址', type: 'text', span: 2 },
      { key: 'cVenPostCode', label: '邮政编码', type: 'text' },
      { key: 'cVenBP', label: '传真/总机', type: 'text' },
      { key: 'cVenFax', label: '传真号', type: 'text' },
      { key: 'cVenLPerson', label: '法人', type: 'text' },
      { key: 'cVenPPerson', label: '主营业务员', type: 'text' },
      { key: 'cVenDepart', label: '分管部门', type: 'text' }
    ]
  },
  {
    key: 'bank',
    label: '银行信息',
    fields: [
      { key: 'cVenBank', label: '开户银行', type: 'text' },
      { key: 'cVenAccount', label: '银行账号', type: 'text' },
      { key: 'cVenBankNub', label: '银行行号', type: 'text' },
      { key: 'cVenBankCode', label: '所属银行编码', type: 'text' },
      { key: 'cVenPayCond', label: '付款条件编码', type: 'text' },
      { key: 'cVenWhCode', label: '到货仓库', type: 'text' },
      { key: 'cVenIType', label: '到货方式', type: 'text' }
    ]
  },
  {
    key: 'qualification',
    label: '资质期限',
    fields: [
      { key: 'dBusinessSDate', label: '经营许可生效日期', type: 'date' },
      { key: 'dBusinessEDate', label: '经营许可到期日期', type: 'date' },
      { key: 'dLicenceSDate', label: '营业执照生效日期', type: 'date' },
      { key: 'dLicenceEDate', label: '营业执照到期日期', type: 'date' },
      { key: 'dProxySDate', label: '法人委托书生效日期', type: 'date' },
      { key: 'dProxyEDate', label: '法人委托书到期日期', type: 'date' },
      { key: 'dVenDevDate', label: '发展日期', type: 'date' },
      { key: 'dEndDate', label: '停用日期', type: 'date' },
      { key: 'bBusinessDate', label: '经营许可证期限管理', type: 'switch' },
      { key: 'bLicenceDate', label: '营业执照期限管理', type: 'switch' },
      { key: 'bPassGMP', label: '通过 GMP 认证', type: 'switch' },
      { key: 'bProxyDate', label: '法人委托书期限管理', type: 'switch' },
      { key: 'bProxyForeign', label: '是否委外', type: 'switch' },
      { key: 'bVenCargo', label: '是否货物', type: 'switch' },
      { key: 'bVenService', label: '是否服务', type: 'switch' },
      { key: 'bVenTax', label: '单价含税', type: 'switch' }
    ]
  },
  {
    key: 'finance',
    label: '财务信用',
    fields: [
      { key: 'fRegistFund', label: '注册资金', type: 'number' },
      { key: 'iAPMoney', label: '应付余额', type: 'number' },
      { key: 'iLastMoney', label: '最后交易金额', type: 'number' },
      { key: 'iLRMoney', label: '最后付款金额', type: 'number' },
      { key: 'iVenCreLine', label: '信用额度', type: 'number' },
      { key: 'iVenDisRate', label: '折率', type: 'number' },
      { key: 'iVenCreGrade', label: '信用等级', type: 'text' },
      { key: 'iVenCreDate', label: '信用期限', type: 'number' },
      { key: 'iBusinessADays', label: '经营许可预警天数', type: 'number' },
      { key: 'iLicenceADays', label: '营业执照预警天数', type: 'number' },
      { key: 'iProxyADays', label: '法人委托书预警天数', type: 'number' },
      { key: 'iEmployeeNum', label: '员工人数', type: 'number' },
      { key: 'iFrequency', label: '使用频度', type: 'number' },
      { key: 'iGradeABC', label: 'ABC 等级', type: 'number' },
      { key: 'iId', label: '所属权限组', type: 'number' }
    ]
  },
  {
    key: 'custom',
    label: '自定义项',
    fields: [
      { key: 'cRelCustomer', label: '对应客户', type: 'text' },
      { key: 'cVenHeadCode', label: '供应商总公司编码', type: 'text' },
      { key: 'cDCCode', label: '地区编码', type: 'text' },
      { key: 'cVenTradeCCode', label: '行业编码', type: 'text' },
      { key: 'cVenDefine3', label: '自定义项 3', type: 'text' },
      { key: 'cVenDefine4', label: '自定义项 4', type: 'text' },
      { key: 'cVenDefine5', label: '自定义项 5', type: 'text' },
      { key: 'cVenDefine6', label: '自定义项 6', type: 'text' },
      { key: 'cVenDefine7', label: '自定义项 7', type: 'text' },
      { key: 'cVenDefine8', label: '自定义项 8', type: 'text' },
      { key: 'cVenDefine9', label: '自定义项 9', type: 'text' },
      { key: 'cVenDefine10', label: '自定义项 10', type: 'text' },
      { key: 'cVenDefine11', label: '自定义项 11', type: 'number' },
      { key: 'cVenDefine12', label: '自定义项 12', type: 'number' },
      { key: 'cVenDefine13', label: '自定义项 13', type: 'number' },
      { key: 'cVenDefine14', label: '自定义项 14', type: 'number' },
      { key: 'cVenDefine15', label: '自定义项 15', type: 'date' },
      { key: 'cVenDefine16', label: '自定义项 16', type: 'date' },
      { key: 'dLastDate', label: '最后交易日期', type: 'date' },
      { key: 'dLRDate', label: '最后付款日期', type: 'date' },
      { key: 'dModifyDate', label: '变更日期', type: 'date' }
    ]
  }
]

const numericFields = new Set(
  vendorSections.flatMap((section) => section.fields.filter((field) => field.type === 'number').map((field) => field.key))
)
const dateFields = new Set(
  vendorSections.flatMap((section) => section.fields.filter((field) => field.type === 'date').map((field) => field.key))
)

const defaultSwitchFields = vendorSections
  .flatMap((section) => section.fields)
  .filter((field) => field.type === 'switch')
  .map((field) => field.key)

resetVendorForm()
void loadVendors()

async function loadVendors() {
  loading.value = true
  try {
    const res = await financeArchiveApi.listSuppliers({
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
  editingVendorCode.value = ''
  resetVendorForm()
  activeSections.value = ['basic', 'contact', 'bank', 'qualification']
  dialogVisible.value = true
}

async function openEditDialog(vendorCode: string) {
  dialogVisible.value = true
  editingVendorCode.value = vendorCode
  resetVendorForm()
  saving.value = false
  try {
    const res = await financeArchiveApi.getSupplierDetail(vendorCode)
    Object.entries(res.data).forEach(([key, value]) => {
      vendorForm[key] = value as string | number | undefined
    })
    defaultSwitchFields.forEach((key) => {
      vendorForm[key] = Number(vendorForm[key] || 0)
    })
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
      await financeArchiveApi.updateSupplier(editingVendorCode.value, payload)
      ElMessage.success('供应商档案已更新')
    } else {
      await financeArchiveApi.createSupplier(payload)
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
    await financeArchiveApi.disableSupplier(vendorCode)
    ElMessage.success('供应商已停用')
    await loadVendors()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '停用供应商失败'))
  }
}

function buildVendorPayload(): FinanceVendorSavePayload {
  const payload: Record<string, unknown> = {}
  Object.entries(vendorForm).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
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
