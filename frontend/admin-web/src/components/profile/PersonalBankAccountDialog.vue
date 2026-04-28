<template>
  <el-dialog
    v-model="dialogVisible"
    :title="mode === 'create' ? '新增对私收款账户' : '编辑对私收款账户'"
    width="760px"
    destroy-on-close
  >
    <el-form label-position="top" class="space-y-5">
      <SupplierPaymentInfoFields
        :form-state="bankForm"
        :required="true"
        :field-map="bankFieldMap"
        account-name-label="账户名"
        business-scope="PRIVATE"
      />

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <el-form-item label="账户类型" class="!mb-0">
          <el-input v-model="bankForm.accountType" placeholder="默认对私账户" />
        </el-form-item>
        <el-form-item label="账户状态" class="!mb-0">
          <el-switch
            v-model="bankForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="默认账户" class="!mb-0 md:col-span-2">
          <el-switch
            v-model="bankForm.defaultAccount"
            :active-value="1"
            :inactive-value="0"
            active-text="设为默认"
            inactive-text="非默认"
          />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button data-testid="personal-bank-account-cancel" @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          data-testid="personal-bank-account-save"
          :loading="savingBankAccount"
          @click="submitBankAccount"
        >
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'
import {
  profileApi,
  type UserBankAccountRecord,
  type UserBankAccountSavePayload
} from '@/api'

defineOptions({ name: 'PersonalBankAccountDialog' })

const props = withDefaults(defineProps<{
  modelValue: boolean
  mode?: 'create' | 'edit'
  account?: UserBankAccountRecord | null
  existingAccountsCount?: number | null
}>(), {
  mode: 'create',
  account: null,
  existingAccountsCount: null
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [payload: {
    mode: 'create' | 'edit'
    accountName: string
    record: UserBankAccountRecord
  }]
}>()

const bankFieldMap = {
  accountName: 'accountName',
  bankCode: 'bankCode',
  bankName: 'bankName',
  province: 'province',
  city: 'city',
  branchCode: 'branchCode',
  branchName: 'branchName',
  accountNo: 'accountNo'
} as const

const savingBankAccount = ref(false)
const bankForm = reactive<UserBankAccountSavePayload>(createEmptyBankForm())

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

watch(
  () => [props.modelValue, props.mode, props.account?.id, props.existingAccountsCount] as const,
  ([visible]) => {
    if (!visible) {
      return
    }
    if (props.mode === 'edit' && props.account) {
      hydrateBankForm(props.account)
      return
    }
    resetBankForm()
  },
  { immediate: true }
)

function createEmptyBankForm() {
  return {
    accountName: '',
    accountNo: '',
    accountType: '对私账户',
    bankName: '',
    bankCode: '',
    province: '',
    city: '',
    branchName: '',
    branchCode: '',
    defaultAccount: 0,
    status: 1
  } satisfies UserBankAccountSavePayload
}

function resetBankForm() {
  Object.assign(bankForm, createEmptyBankForm(), {
    defaultAccount: props.existingAccountsCount === 0 ? 1 : 0
  })
}

function hydrateBankForm(account: UserBankAccountRecord) {
  Object.assign(bankForm, createEmptyBankForm(), {
    accountName: account.accountName || '',
    accountNo: account.accountNo || '',
    accountType: account.accountType || '对私账户',
    bankName: account.bankName || '',
    bankCode: account.bankCode || '',
    province: account.province || '',
    city: account.city || '',
    branchName: account.branchName || '',
    branchCode: account.branchCode || '',
    defaultAccount: account.defaultAccount ? 1 : 0,
    status: account.status ?? 1
  })
}

function validateBankForm() {
  const incompleteBankDirectoryMessage = '请选择开户银行、开户省、开户市与开户网点后再保存'
  if (!String(bankForm.accountName || '').trim()) return '请填写账户名'
  if (!String(bankForm.accountNo || '').trim()) return '请填写银行账号'
  if (
    !String(bankForm.bankCode || '').trim()
    || !String(bankForm.bankName || '').trim()
    || !String(bankForm.province || '').trim()
    || !String(bankForm.city || '').trim()
    || !String(bankForm.branchCode || '').trim()
    || !String(bankForm.branchName || '').trim()
  ) {
    return incompleteBankDirectoryMessage
  }
  return ''
}

async function submitBankAccount() {
  const validationMessage = validateBankForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }
  if (bankForm.status !== 1) {
    bankForm.defaultAccount = 0
  }
  savingBankAccount.value = true
  try {
    let record: UserBankAccountRecord
    if (props.mode === 'edit' && props.account?.id) {
      const res = await profileApi.updateBankAccount(props.account.id, bankForm)
      record = res.data
      ElMessage.success('个人银行账户已更新')
    } else {
      const res = await profileApi.createBankAccount(bankForm)
      record = res.data
      ElMessage.success('个人银行账户已新增')
    }
    emit('saved', {
      mode: props.mode,
      accountName: String(bankForm.accountName || '').trim(),
      record
    })
    dialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '保存个人银行账户失败')
  } finally {
    savingBankAccount.value = false
  }
}

defineExpose({
  bankForm,
  validateBankForm,
  submitBankAccount
})
</script>
