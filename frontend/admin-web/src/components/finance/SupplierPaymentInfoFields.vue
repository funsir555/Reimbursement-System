<template>
  <div class="space-y-4">
    <div v-if="showSectionHeader" class="rounded-2xl border border-sky-100 bg-sky-50/80 px-4 py-3 text-sm text-sky-700">
      <p class="font-semibold">收款信息</p>
      <p class="mt-1 text-xs leading-6 text-sky-600">
        银行、省、市与分支行均需从银行目录中选择；联行号会在选择分支行后自动带出。
      </p>
    </div>

    <el-alert
      v-if="needsBankReselection"
      type="warning"
      :closable="false"
      title="当前记录的银行目录编码不完整，请重新选择开户银行、省市与分支行后再保存。"
    />

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <el-form-item :label="accountNameLabel" :required="required" class="!mb-0">
        <el-input
          v-model="accountNameModel"
          :maxlength="128"
          :placeholder="required ? '请输入开户名/收款人姓名' : '请输入开户名/收款人姓名'"
        />
      </el-form-item>

      <el-form-item label="联行号" class="!mb-0">
        <el-input
          :model-value="cnapsCodeModel"
          disabled
          readonly
          placeholder="选择分支行后自动带出"
        />
      </el-form-item>

      <el-form-item label="开户银行" :required="required" class="!mb-0">
        <el-select
          v-model="bankCodeModel"
          filterable
          remote
          reserve-keyword
          clearable
          class="w-full"
          placeholder="请选择开户银行"
          :remote-method="loadBankOptions"
          :loading="bankOptionsLoading"
          @change="handleBankChange"
        >
          <el-option
            v-for="item in bankOptions"
            :key="item.bankCode"
            :label="item.bankName"
            :value="item.bankCode"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="银行账号" :required="required" class="!mb-0">
        <el-input
          v-model="accountNoModel"
          :maxlength="64"
          placeholder="请输入银行账号"
        />
      </el-form-item>

      <el-form-item label="开户省" :required="required" class="!mb-0">
        <el-select
          v-model="provinceModel"
          clearable
          filterable
          class="w-full"
          placeholder="请选择开户省"
          :disabled="!bankCodeModel"
          @change="handleProvinceChange"
        >
          <el-option
            v-for="item in provinceOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="开户市" :required="required" class="!mb-0">
        <el-select
          v-model="cityModel"
          clearable
          filterable
          class="w-full"
          placeholder="请选择开户市"
          :disabled="!bankCodeModel || !provinceModel"
          @change="handleCityChange"
        >
          <el-option
            v-for="item in cityOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="分支行" :required="required" class="!mb-0 xl:col-span-2">
        <el-select
          v-model="branchCodeModel"
          filterable
          remote
          reserve-keyword
          clearable
          class="w-full"
          placeholder="请选择或搜索分支行"
          :disabled="!bankCodeModel || !provinceModel || !cityModel"
          :remote-method="loadBranchOptions"
          :loading="branchOptionsLoading"
          @change="handleBranchChange"
        >
          <el-option
            v-for="item in branchOptions"
            :key="item.branchCode"
            :label="item.label"
            :value="item.branchCode"
          />
        </el-select>
      </el-form-item>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  financeBankApi,
  type FinanceBankBranchOption,
  type FinanceBankBusinessScope,
  type FinanceBankOption
} from '@/api'

type PaymentFieldMap = {
  accountName: string
  bankCode: string
  bankName: string
  province: string
  city: string
  branchCode: string
  branchName: string
  accountNo: string
  cnapsCode: string
}

const DEFAULT_FIELD_MAP: PaymentFieldMap = {
  accountName: 'receiptAccountName',
  bankCode: 'cVenBankCode',
  bankName: 'cVenBank',
  province: 'receiptBankProvince',
  city: 'receiptBankCity',
  branchCode: 'receiptBranchCode',
  branchName: 'receiptBranchName',
  accountNo: 'cVenAccount',
  cnapsCode: 'cVenBankNub'
}

const props = withDefaults(defineProps<{
  formState: Record<string, unknown>
  required?: boolean
  showSectionHeader?: boolean
  accountNameLabel?: string
  fieldMap?: Partial<PaymentFieldMap>
  autoFillSourceKey?: string
  businessScope?: FinanceBankBusinessScope
}>(), {
  required: false,
  showSectionHeader: false,
  accountNameLabel: '开户名',
  fieldMap: () => ({}),
  autoFillSourceKey: '',
  businessScope: 'BOTH'
})

const resolvedFieldMap = computed<PaymentFieldMap>(() => ({
  ...DEFAULT_FIELD_MAP,
  ...(props.fieldMap || {})
}))

const bankOptions = ref<FinanceBankOption[]>([])
const provinceOptions = ref<string[]>([])
const cityOptions = ref<string[]>([])
const branchOptions = ref<FinanceBankBranchOption[]>([])
const bankOptionsLoading = ref(false)
const branchOptionsLoading = ref(false)

const accountNameModel = computed({
  get: () => mappedStringValue('accountName'),
  set: (value: string) => setMappedField('accountName', value)
})

const cnapsCodeModel = computed({
  get: () => mappedStringValue('cnapsCode'),
  set: (value: string) => setMappedField('cnapsCode', value)
})

const bankCodeModel = computed({
  get: () => mappedStringValue('bankCode'),
  set: (value: string) => setMappedField('bankCode', value)
})

const bankNameModel = computed({
  get: () => mappedStringValue('bankName'),
  set: (value: string) => setMappedField('bankName', value)
})

const accountNoModel = computed({
  get: () => mappedStringValue('accountNo'),
  set: (value: string) => setMappedField('accountNo', value)
})

const provinceModel = computed({
  get: () => mappedStringValue('province'),
  set: (value: string) => setMappedField('province', value)
})

const cityModel = computed({
  get: () => mappedStringValue('city'),
  set: (value: string) => setMappedField('city', value)
})

const branchCodeModel = computed({
  get: () => mappedStringValue('branchCode'),
  set: (value: string) => setMappedField('branchCode', value)
})

const branchNameModel = computed({
  get: () => mappedStringValue('branchName'),
  set: (value: string) => setMappedField('branchName', value)
})

const needsBankReselection = computed(() => {
  const hasAnyBankValue = [
    bankNameModel.value,
    provinceModel.value,
    cityModel.value,
    branchNameModel.value,
    cnapsCodeModel.value
  ].some((item) => !!item)
  if (!hasAnyBankValue) {
    return false
  }
  return !bankCodeModel.value || !provinceModel.value || !cityModel.value || !branchCodeModel.value
})

watch(
  () => autoFillSourceValue(),
  (value) => {
    if (!props.autoFillSourceKey) {
      return
    }
    if (!accountNameModel.value && value) {
      accountNameModel.value = value
    }
  },
  { immediate: true }
)

watch(
  () => bankCodeModel.value,
  async (value) => {
    if (!value) {
      provinceOptions.value = []
      cityOptions.value = []
      branchOptions.value = []
      return
    }
    await ensureBankOptionLoaded()
    await loadProvinceOptions()
    if (provinceModel.value) {
      await loadCityOptions()
    }
    if (provinceModel.value && cityModel.value) {
      await loadBranchOptions('')
    }
  },
  { immediate: true }
)

watch(
  () => [provinceModel.value, cityModel.value],
  async ([province, city]) => {
    if (!bankCodeModel.value || !province) {
      cityOptions.value = []
      branchOptions.value = []
      return
    }
    await loadCityOptions()
    if (city) {
      await loadBranchOptions('')
    }
  },
  { immediate: true }
)

void loadBankOptions('')

async function loadBankOptions(keyword: string) {
  bankOptionsLoading.value = true
  try {
    const res = await financeBankApi.listBanks({
      keyword: keyword || undefined,
      businessScope: props.businessScope
    })
    bankOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载开户银行失败'))
  } finally {
    bankOptionsLoading.value = false
  }
}

async function handleBankChange(nextBankCode: string | undefined) {
  const bank = bankOptions.value.find((item) => item.bankCode === nextBankCode)
  bankNameModel.value = bank?.bankName || ''
  provinceModel.value = ''
  cityModel.value = ''
  branchCodeModel.value = ''
  branchNameModel.value = ''
  cnapsCodeModel.value = ''
  if (!nextBankCode) {
    setMappedField('bankCode', '')
    setMappedField('bankName', '')
  }
  await loadProvinceOptions()
}

async function handleProvinceChange() {
  cityModel.value = ''
  branchCodeModel.value = ''
  branchNameModel.value = ''
  cnapsCodeModel.value = ''
  await loadCityOptions()
}

async function handleCityChange() {
  branchCodeModel.value = ''
  branchNameModel.value = ''
  cnapsCodeModel.value = ''
  await loadBranchOptions('')
}

async function loadProvinceOptions() {
  if (!bankCodeModel.value) {
    provinceOptions.value = []
    return
  }
  try {
    const res = await financeBankApi.listBankProvinces({
      bankCode: bankCodeModel.value,
      businessScope: props.businessScope
    })
    provinceOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载开户地址失败'))
  }
}

async function loadCityOptions() {
  if (!bankCodeModel.value || !provinceModel.value) {
    cityOptions.value = []
    return
  }
  try {
    const res = await financeBankApi.listBankCities({
      bankCode: bankCodeModel.value,
      province: provinceModel.value,
      businessScope: props.businessScope
    })
    cityOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载开户城市失败'))
  }
}

async function loadBranchOptions(keyword: string) {
  if (!bankCodeModel.value || !provinceModel.value || !cityModel.value) {
    branchOptions.value = []
    return
  }
  branchOptionsLoading.value = true
  try {
    const res = await financeBankApi.listBankBranches({
      bankCode: bankCodeModel.value,
      province: provinceModel.value,
      city: cityModel.value,
      keyword: keyword || undefined,
      businessScope: props.businessScope
    })
    branchOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载分支行失败'))
  } finally {
    branchOptionsLoading.value = false
  }
}

function handleBranchChange(nextBranchCode: string | undefined) {
  const branch = branchOptions.value.find((item) => item.branchCode === nextBranchCode)
  if (!branch) {
    setMappedField('branchCode', '')
    setMappedField('branchName', '')
    setMappedField('cnapsCode', '')
    return
  }
  applyBranchSelection(branch)
}

async function ensureBankOptionLoaded(bankCode = bankCodeModel.value, bankName = bankNameModel.value) {
  if (!bankCode && !bankName) {
    return
  }
  if (bankCode && bankOptions.value.some((item) => item.bankCode === bankCode)) {
    return
  }
  try {
    const res = await financeBankApi.listBanks({
      keyword: bankName || bankCode || undefined,
      businessScope: props.businessScope
    })
    bankOptions.value = mergeBankOptions(bankOptions.value, res.data)
    const matched = res.data.find((item) => item.bankCode === bankCode || item.bankName === bankName)
    if (matched) {
      setMappedField('bankCode', matched.bankCode)
      setMappedField('bankName', matched.bankName)
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载开户银行失败'))
  }
}

function applyBranchSelection(branch: FinanceBankBranchOption) {
  setMappedField('bankCode', branch.bankCode)
  setMappedField('bankName', branch.bankName)
  setMappedField('province', branch.province)
  setMappedField('city', branch.city)
  setMappedField('branchCode', branch.branchCode)
  setMappedField('branchName', branch.branchName)
  setMappedField('cnapsCode', branch.cnapsCode || '')
}

function mappedStringValue(key: keyof PaymentFieldMap) {
  return stringValue(resolvedFieldMap.value[key])
}

function setMappedField(key: keyof PaymentFieldMap, value: unknown) {
  const fieldKey = resolvedFieldMap.value[key]
  props.formState[fieldKey] = value == null ? '' : value
}

function autoFillSourceValue() {
  return props.autoFillSourceKey ? stringValue(props.autoFillSourceKey) : ''
}

function mergeBankOptions(current: FinanceBankOption[], next: FinanceBankOption[]) {
  const map = new Map<string, FinanceBankOption>()
  current.forEach((item) => map.set(item.bankCode, item))
  next.forEach((item) => map.set(item.bankCode, item))
  return Array.from(map.values())
}

function stringValue(key: string) {
  const raw = props.formState[key]
  return typeof raw === 'string' ? raw : raw == null ? '' : String(raw)
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>
