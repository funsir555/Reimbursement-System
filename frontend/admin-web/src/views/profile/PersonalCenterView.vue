<template>
  <div class="flex items-start gap-6">
    <aside class="w-60 shrink-0 overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
      <div class="border-b border-slate-100 bg-slate-50/80 px-5 py-4">
        <p class="text-sm font-semibold text-slate-800">个人中心</p>
        <p class="mt-1 text-xs text-slate-400">查看个人信息、账户安全与收款账户</p>
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
              这里集中查看你的基础信息、账户安全状态与收款账户配置，方便财务日常报销和付款协同。
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
            <span class="font-semibold text-slate-800">账号与安全</span>
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
            <div class="flex items-center justify-between">
              <span class="font-semibold text-slate-800">收款账户</span>
              <el-tag type="primary" effect="plain">{{ center?.bankAccounts.length || 0 }} 个账户</el-tag>
            </div>
          </template>

          <div v-if="center?.bankAccounts.length" class="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <div
              v-for="account in center.bankAccounts"
              :key="account.id"
              class="rounded-3xl border border-slate-200 bg-slate-50 px-5 py-5"
            >
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="text-lg font-semibold text-slate-800">{{ account.bankName }}</p>
                  <p class="mt-1 text-sm text-slate-500">{{ account.branchName || '默认支行' }}</p>
                </div>
                <div class="flex items-center gap-2">
                  <el-tag v-if="account.defaultAccount" type="success" effect="plain">默认</el-tag>
                  <el-tag type="primary" effect="plain">{{ account.status }}</el-tag>
                </div>
              </div>

              <div class="mt-5 space-y-3 text-sm">
                <div class="flex items-center justify-between">
                  <span class="text-slate-400">账户名</span>
                  <span class="text-slate-700">{{ account.accountName }}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-slate-400">银行卡号</span>
                  <span class="text-slate-700">{{ account.accountNoMasked }}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-slate-400">账户类型</span>
                  <span class="text-slate-700">{{ account.accountType }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无收款账户" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { profileApi, type PersonalCenterData } from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const route = useRoute()
const router = useRouter()

const center = ref<PersonalCenterData | null>(null)
const passwordDialogVisible = ref(false)
const savingPassword = ref(false)
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const canChangePassword = computed(() => hasPermission('profile:password:update', permissionCodes.value))

const tabs = [
  { key: 'info', label: '个人信息', tip: '查看姓名、岗位和劳动关系所属' },
  { key: 'security', label: '账号与安全', tip: '查看手机号、邮箱并修改密码' },
  { key: 'bank', label: '收款账户', tip: '查看你的个人银行账户信息' }
]

const activeTab = computed(() => {
  const tab = route.query.tab
  return typeof tab === 'string' ? tab : 'info'
})

const loadCenter = async () => {
  try {
    const res = await profileApi.getOverview()
    center.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载个人中心失败')
  }
}

onMounted(loadCenter)

const switchTab = (tab: string) => {
  router.replace({
    path: '/profile',
    query: { tab }
  })
}

const submitPasswordChange = async () => {
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
