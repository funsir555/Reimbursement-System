<template>
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
                当前维护公司：<strong>{{ companyName || companyId || '未设置' }}</strong>
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
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { financeArchiveApi, type FinanceCustomerSavePayload } from '@/api'
import MoneyInput from '@/components/inputs/MoneyInput.vue'

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

const props = withDefaults(defineProps<{
  companyId?: string
  companyName?: string
}>(), {
  companyId: '',
  companyName: ''
})

const emit = defineEmits<{
  saved: [customerCode: string]
  closed: []
}>()

const dialogVisible = ref(false)
const saving = ref(false)
const editingCustomerCode = ref('')
const activeSections = ref(['basic', 'contact', 'bank', 'finance'])
const customerForm = reactive<Record<string, string | number | undefined>>({})

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
      { key: 'cCusPerson', label: '联系人', type: 'text', maxLength: 64 },
      { key: 'cCusHand', label: '手机', type: 'text', maxLength: 32 },
      { key: 'cCusAddress', label: '联系地址', type: 'text', span: 2 },
      { key: 'cCusPostCode', label: '邮政编码', type: 'text', maxLength: 16 },
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
      { key: 'cCusBank', label: '开户银行', type: 'text', maxLength: 128 },
      { key: 'cCusAccount', label: '银行账号', type: 'text', maxLength: 64 },
      { key: 'cCusBankCode', label: '所属银行编码', type: 'text', maxLength: 64 },
      { key: 'cInvoiceCompany', label: '开票单位', type: 'text' },
      { key: 'cCusLPerson', label: '法人代表', type: 'text', maxLength: 64 }
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

function toMoneyModelValue(value: string | number | undefined) {
  return typeof value === 'string' ? value : value == null ? '' : String(value)
}

function openCreateDialog() {
  if (!props.companyId) {
    ElMessage.warning('当前公司未设置，无法维护客户')
    return
  }
  editingCustomerCode.value = ''
  resetCustomerForm()
  activeSections.value = ['basic', 'contact', 'bank', 'finance']
  dialogVisible.value = true
}

async function openEditDialog(customerCode: string) {
  if (!props.companyId) {
    return
  }
  dialogVisible.value = true
  editingCustomerCode.value = customerCode
  resetCustomerForm()
  saving.value = false
  try {
    const res = await financeArchiveApi.getCustomerDetail(props.companyId, customerCode)
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
  if (!props.companyId) {
    ElMessage.warning('当前公司未设置，无法维护客户')
    return
  }
  saving.value = true
  try {
    const payload = buildCustomerPayload()
    if (editingCustomerCode.value) {
      await financeArchiveApi.updateCustomer(props.companyId, editingCustomerCode.value, payload)
      ElMessage.success('客户档案已更新')
      emit('saved', editingCustomerCode.value)
    } else {
      const res = await financeArchiveApi.createCustomer(props.companyId, payload)
      const createdCode = String(res.data?.cCusCode || payload.cCusCode || '').trim()
      ElMessage.success('客户档案已创建')
      emit('saved', createdCode)
    }
    closeDialog()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存客户档案失败'))
  } finally {
    saving.value = false
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
    companyId: props.companyId
  }
  Array.from(allowedFormFieldKeys).forEach((key) => {
    const rawValue = customerForm[key]
    const value = typeof rawValue === 'string' ? rawValue.trim() : rawValue
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
  emit('closed')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

defineExpose({
  dialogVisible,
  customerForm,
  openCreateDialog,
  openEditDialog,
  saveCustomer,
  closeDialog
})
</script>
