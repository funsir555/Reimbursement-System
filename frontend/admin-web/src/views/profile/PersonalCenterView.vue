<template>
  <div class="flex items-start gap-6">
    <aside class="w-60 shrink-0 overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
      <div class="border-b border-slate-100 bg-slate-50/80 px-5 py-4">
        <p class="text-sm font-semibold text-slate-800">个人中心</p>
        <p class="mt-1 text-xs text-slate-400">查看个人信息、账户安全与我维护的对私收款账户</p>
      </div>

      <div class="py-3">
        <button
          v-for="item in tabs"
          :key="item.key"
          type="button"
          class="profile-side-item w-full px-5 py-3 text-left transition-colors"
          :class="{ 'profile-side-item-active': item.key === activeTab }"
          @click="switchTab(item.key)"
        >
          <span class="block text-sm font-medium">{{ item.label }}</span>
          <span class="mt-1 block text-xs text-slate-400">{{ item.tip }}</span>
        </button>
      </div>
    </aside>

    <div class="flex-1 space-y-6">
      <section class="overflow-hidden rounded-[32px] bg-gradient-to-r from-blue-600 via-sky-500 to-cyan-400 text-white shadow-lg">
        <div class="flex flex-col gap-6 px-8 py-8 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p class="text-sm uppercase tracking-[0.22em] text-blue-100">Profile Center</p>
            <h1 class="mt-3 text-3xl font-bold">个人中心</h1>
            <p class="mt-3 max-w-2xl leading-7 text-blue-50/90">
              这里集中查看你的基础资料、账户安全状态，以及你在个人中心维护的任意姓名对私收款账户。
            </p>
          </div>
          <div class="min-w-[240px] rounded-3xl bg-white/15 px-5 py-4 backdrop-blur-sm">
            <p class="text-sm text-blue-100">当前登录</p>
            <p class="mt-2 text-xl font-semibold">{{ center?.user.name || center?.user.username || '未获取到用户' }}</p>
            <p class="mt-2 text-sm text-blue-100">{{ center?.user.position || '员工' }}</p>
          </div>
        </div>
      </section>

      <template v-if="activeTab === 'info'">
        <section class="grid grid-cols-1 gap-4 xl:grid-cols-3">
          <el-card class="!rounded-3xl !shadow-sm">
            <p class="text-sm text-slate-500">姓名</p>
            <p class="mt-3 text-2xl font-semibold text-slate-800">{{ center?.user.name || '-' }}</p>
          </el-card>
          <el-card class="!rounded-3xl !shadow-sm">
            <p class="text-sm text-slate-500">岗位</p>
            <p class="mt-3 text-2xl font-semibold text-slate-800">{{ center?.user.position || '-' }}</p>
          </el-card>
          <el-card class="!rounded-3xl !shadow-sm">
            <p class="text-sm text-slate-500">劳动关系所属</p>
            <p class="mt-3 text-2xl font-semibold text-slate-800">{{ center?.user.laborRelationBelong || '-' }}</p>
          </el-card>
        </section>

        <el-card class="!rounded-3xl !shadow-sm">
          <template #header>
            <span class="font-semibold text-slate-800">基础资料</span>
          </template>

          <div class="grid grid-cols-1 gap-6 text-sm md:grid-cols-2">
            <div class="rounded-2xl bg-slate-50 px-4 py-4">
              <p class="text-slate-400">用户名</p>
              <p class="mt-2 font-medium text-slate-800">{{ center?.user.username || '-' }}</p>
            </div>
            <div class="rounded-2xl bg-slate-50 px-4 py-4">
              <p class="text-slate-400">用户编号</p>
              <p class="mt-2 font-medium text-slate-800">{{ center?.user.userId || '-' }}</p>
            </div>
            <div class="rounded-2xl bg-slate-50 px-4 py-4">
              <p class="text-slate-400">手机号</p>
              <p class="mt-2 font-medium text-slate-800">{{ center?.user.phone || '-' }}</p>
            </div>
            <div class="rounded-2xl bg-slate-50 px-4 py-4">
              <p class="text-slate-400">邮箱</p>
              <p class="mt-2 font-medium text-slate-800">{{ center?.user.email || '-' }}</p>
            </div>
          </div>
        </el-card>
      </template>

      <template v-else-if="activeTab === 'security'">
        <el-card class="!rounded-3xl !shadow-sm">
          <template #header>
            <span class="font-semibold text-slate-800">账户与安全</span>
          </template>

          <div class="space-y-4">
            <div class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 px-5 py-4">
              <div>
                <p class="font-medium text-slate-800">绑定手机号</p>
                <p class="mt-1 text-sm text-slate-500">{{ center?.user.phone || '未设置' }}</p>
              </div>
              <el-tag type="success" effect="plain">已绑定</el-tag>
            </div>

            <div class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 px-5 py-4">
              <div>
                <p class="font-medium text-slate-800">绑定邮箱</p>
                <p class="mt-1 text-sm text-slate-500">{{ center?.user.email || '未设置' }}</p>
              </div>
              <el-tag type="primary" effect="plain">可用于通知</el-tag>
            </div>

            <div class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 px-5 py-4">
              <div>
                <p class="font-medium text-slate-800">登录密码</p>
                <p class="mt-1 text-sm text-slate-500">建议定期更新密码，保障账户安全。</p>
              </div>
              <el-button v-if="canChangePassword" type="primary" @click="passwordDialogVisible = true">修改密码</el-button>
            </div>
          </div>
        </el-card>
      </template>

      <template v-else>
        <el-card class="!rounded-3xl !shadow-sm">
          <template #header>
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <span class="font-semibold text-slate-800">我维护的对私账户</span>
                <p class="mt-1 text-xs text-slate-400">这里的账户只属于你的个人中心，可自由填写任意姓名，不受员工姓名限制。</p>
              </div>
              <div class="flex items-center gap-3">
                <el-tag type="primary" effect="plain">{{ bankAccounts.length }} 个账户</el-tag>
                <el-button type="primary" @click="openCreateBankDialog">新增账户</el-button>
              </div>
            </div>
          </template>

          <div v-if="bankAccounts.length" class="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <div
              v-for="account in bankAccounts"
              :key="account.id"
              class="rounded-3xl border border-slate-200 bg-slate-50 px-5 py-5"
            >
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="text-lg font-semibold text-slate-800">{{ account.bankName }}</p>
                  <p class="mt-1 text-sm text-slate-500">{{ account.branchName || '未设置开户网点' }}</p>
                </div>
                <div class="flex flex-wrap items-center justify-end gap-2">
                  <el-tag v-if="account.defaultAccount" type="success" effect="plain">默认</el-tag>
                  <el-tag :type="account.status === 1 ? 'primary' : 'info'" effect="plain">{{ account.statusLabel }}</el-tag>
                </div>
              </div>

              <div class="mt-5 grid grid-cols-1 gap-3 text-sm md:grid-cols-2">
                <div class="rounded-2xl bg-white px-4 py-3">
                  <p class="text-slate-400">账户名</p>
                  <p class="mt-2 font-medium text-slate-800">{{ account.accountName }}</p>
                </div>
                <div class="rounded-2xl bg-white px-4 py-3">
                  <p class="text-slate-400">银行账号</p>
                  <p class="mt-2 font-medium text-slate-800">{{ account.accountNoMasked }}</p>
                </div>
                <div class="rounded-2xl bg-white px-4 py-3">
                  <p class="text-slate-400">开户地区</p>
                  <p class="mt-2 font-medium text-slate-800">{{ accountLocation(account) }}</p>
                </div>
              </div>

              <div class="mt-5 flex flex-wrap items-center gap-3">
                <el-button type="primary" plain @click="openEditBankDialog(account)">编辑</el-button>
                <el-button
                  v-if="!account.defaultAccount && account.status === 1"
                  type="success"
                  plain
                  @click="setDefaultBankAccount(account)"
                >
                  设为默认
                </el-button>
                <el-button
                  :type="account.status === 1 ? 'warning' : 'info'"
                  plain
                  @click="toggleBankAccountStatus(account)"
                >
                  {{ account.status === 1 ? '停用' : '启用' }}
                </el-button>
              </div>
            </div>
          </div>

          <el-empty v-else description="暂无对私收款账户">
            <el-button type="primary" @click="openCreateBankDialog">新增账户</el-button>
          </el-empty>
        </el-card>
      </template>
    </div>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="520px"
      destroy-on-close
    >
      <el-form label-position="top" class="space-y-2">
        <el-form-item label="当前密码" required>
          <el-input v-model="passwordForm.currentPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingPassword" @click="submitPasswordChange">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bankDialogVisible"
      :title="bankDialogMode === 'create' ? '新增对私收款账户' : '编辑对私收款账户'"
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
          <el-button @click="closeBankDialog">取消</el-button>
          <el-button type="primary" :loading="savingBankAccount" @click="submitBankAccount">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'
import {
  profileApi,
  type PersonalCenterData,
  type UserBankAccountRecord,
  type UserBankAccountSavePayload
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const route = useRoute()
const router = useRouter()

const center = ref<PersonalCenterData | null>(null)
const bankAccounts = ref<UserBankAccountRecord[]>([])
const passwordDialogVisible = ref(false)
const savingPassword = ref(false)
const bankDialogVisible = ref(false)
const savingBankAccount = ref(false)
const bankDialogMode = ref<'create' | 'edit'>('create')
const editingBankAccountId = ref<number | null>(null)
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const bankForm = reactive<UserBankAccountSavePayload>({
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
})

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

const canChangePassword = computed(() => hasPermission('profile:password:update', permissionCodes.value))

const tabs = [
  { key: 'info', label: '个人信息', tip: '查看姓名、岗位和劳动关系所属' },
  { key: 'security', label: '账户与安全', tip: '查看手机号、邮箱并修改密码' },
  { key: 'bank', label: '收款账户', tip: '维护你个人中心里的任意姓名对私账户' }
]

const activeTab = computed(() => {
  const tab = route.query.tab
  return typeof tab === 'string' ? tab : 'info'
})

onMounted(async () => {
  await Promise.all([loadCenter(), loadBankAccounts()])
})

async function loadCenter() {
  try {
    const res = await profileApi.getOverview()
    center.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载个人中心失败')
  }
}

async function loadBankAccounts() {
  try {
    const res = await profileApi.listBankAccounts()
    bankAccounts.value = res.data
    if (center.value) {
      center.value.bankAccounts = res.data
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载收款账户失败')
  }
}

function switchTab(tab: string) {
  router.replace({
    path: '/profile',
    query: { tab }
  })
}

function accountLocation(account: UserBankAccountRecord) {
  return [account.province, account.city].filter(Boolean).join(' / ') || '-'
}

function resetBankForm() {
  Object.assign(bankForm, {
    accountName: '',
    accountNo: '',
    accountType: '对私账户',
    bankName: '',
    bankCode: '',
    province: '',
    city: '',
    branchName: '',
    branchCode: '',
    defaultAccount: bankAccounts.value.length === 0 ? 1 : 0,
    status: 1
  })
}

function openCreateBankDialog() {
  bankDialogMode.value = 'create'
  editingBankAccountId.value = null
  resetBankForm()
  bankDialogVisible.value = true
}

function openEditBankDialog(account: UserBankAccountRecord) {
  bankDialogMode.value = 'edit'
  editingBankAccountId.value = account.id
  Object.assign(bankForm, {
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
  bankDialogVisible.value = true
}

function closeBankDialog() {
  bankDialogVisible.value = false
  editingBankAccountId.value = null
  resetBankForm()
}

function validateBankForm() {
  if (!String(bankForm.accountName || '').trim()) return '请填写账户名'
  if (!String(bankForm.accountNo || '').trim()) return '请填写银行账号'
  if (!String(bankForm.bankCode || '').trim() || !String(bankForm.bankName || '').trim()) return '请选择开户银行'
  if (!String(bankForm.province || '').trim() || !String(bankForm.city || '').trim()) return '请选择开户省市'
  if (!String(bankForm.branchCode || '').trim() || !String(bankForm.branchName || '').trim()) return '请选择开户网点'
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
    if (bankDialogMode.value === 'create') {
      await profileApi.createBankAccount(bankForm)
      ElMessage.success('个人银行账户已新增')
    } else if (editingBankAccountId.value) {
      await profileApi.updateBankAccount(editingBankAccountId.value, bankForm)
      ElMessage.success('个人银行账户已更新')
    }
    closeBankDialog()
    await loadBankAccounts()
  } catch (error: any) {
    ElMessage.error(error.message || '保存个人银行账户失败')
  } finally {
    savingBankAccount.value = false
  }
}

async function toggleBankAccountStatus(account: UserBankAccountRecord) {
  const nextStatus = account.status === 1 ? 0 : 1
  try {
    await profileApi.updateBankAccountStatus(account.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '个人银行账户已启用' : '个人银行账户已停用')
    await loadBankAccounts()
  } catch (error: any) {
    ElMessage.error(error.message || '更新个人银行账户状态失败')
  }
}

async function setDefaultBankAccount(account: UserBankAccountRecord) {
  try {
    await profileApi.setDefaultBankAccount(account.id)
    ElMessage.success('个人银行账户已设为默认')
    await loadBankAccounts()
  } catch (error: any) {
    ElMessage.error(error.message || '设置默认个人银行账户失败')
  }
}

async function submitPasswordChange() {
  if (!canChangePassword.value) {
    ElMessage.warning('当前账号没有修改密码权限')
    return
  }

  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请先完整填写密码信息')
    return
  }

  savingPassword.value = true
  try {
    await profileApi.changePassword(passwordForm)
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    router.push('/login')
  } catch (error: any) {
    ElMessage.error(error.message || '修改密码失败')
  } finally {
    savingPassword.value = false
  }
}
</script>

<style scoped>
.profile-side-item {
  border-left: 3px solid transparent;
}

.profile-side-item:hover {
  background: #f8fbff;
}

.profile-side-item-active {
  background: #eff6ff;
  border-left-color: #2563eb;
  color: #2563eb;
}
</style>
