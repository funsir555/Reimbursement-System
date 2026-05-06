<template>
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
                当前维护公司：<strong>{{ companyName || companyId || '未设置' }}</strong>
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
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSupplier">保存供应商</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { financeArchiveApi, type FinanceVendorSavePayload } from '@/api'
import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'
import MoneyInput from '@/components/inputs/MoneyInput.vue'

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

const props = withDefaults(defineProps<{
  companyId?: string
  companyName?: string
}>(), {
  companyId: '',
  companyName: ''
})

const emit = defineEmits<{
  saved: [vendorCode: string]
  closed: []
}>()

const dialogVisible = ref(false)
const saving = ref(false)
const editingVendorCode = ref('')
const activeSections = ref(['basic', 'bank'])
const vendorForm = reactive<Record<string, string | number | undefined>>({})

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
      { key: 'cVenPerson', label: '联系人', type: 'text', maxLength: 64 },
      { key: 'cVenPhone', label: '联系电话', type: 'text', maxLength: 32 },
      { key: 'cVenHand', label: '手机', type: 'text', maxLength: 32 },
      { key: 'cVenEmail', label: '电子邮箱', type: 'text' },
      { key: 'cVenAddress', label: '联系地址', type: 'text', span: 2 },
      { key: 'cVenIAddress', label: '开票地址', type: 'text', span: 2 },
      { key: 'cVenPostCode', label: '邮政编码', type: 'text', maxLength: 16 },
      { key: 'cVenBP', label: '呼机', type: 'text', maxLength: 32 },
      { key: 'cVenFax', label: '传真', type: 'text', maxLength: 32 },
      { key: 'cVenLPerson', label: '法人代表', type: 'text', maxLength: 64 },
      { key: 'cVenPPerson', label: '采购联系人', type: 'text', maxLength: 64 },
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

function toMoneyModelValue(value: string | number | undefined) {
  return typeof value === 'string' ? value : value == null ? '' : String(value)
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
  if (!props.companyId) {
    ElMessage.warning('当前公司未设置，无法维护供应商')
    return
  }
  editingVendorCode.value = ''
  resetVendorForm()
  activeSections.value = ['basic', 'bank']
  dialogVisible.value = true
}

async function openEditDialog(vendorCode: string) {
  if (!props.companyId) return
  dialogVisible.value = true
  editingVendorCode.value = vendorCode
  resetVendorForm()
  saving.value = false
  try {
    const res = await financeArchiveApi.getSupplierDetail(props.companyId, vendorCode)
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
  if (!props.companyId) {
    ElMessage.warning('当前公司未设置，无法维护供应商')
    return
  }
  saving.value = true
  try {
    const payload = buildVendorPayload()
    if (editingVendorCode.value) {
      await financeArchiveApi.updateSupplier(props.companyId, editingVendorCode.value, payload)
      ElMessage.success('供应商档案已更新')
      emit('saved', editingVendorCode.value)
    } else {
      const res = await financeArchiveApi.createSupplier(props.companyId, payload)
      const createdCode = String(res.data?.cVenCode || payload.cVenCode || '').trim()
      ElMessage.success('供应商档案已创建')
      emit('saved', createdCode)
    }
    closeDialog()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存供应商档案失败'))
  } finally {
    saving.value = false
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
    companyId: props.companyId
  }
  Array.from(allowedVendorFieldKeys).forEach((key) => {
    const value = vendorForm[key]
    const normalizedValue = typeof value === 'string' ? value.trim() : value
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
  emit('closed')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

defineExpose({
  dialogVisible,
  activeSections,
  vendorSections,
  vendorForm,
  openCreateDialog,
  openEditDialog,
  saveSupplier,
  closeDialog
})
</script>
