<template>
  <div class="expense-wb-page space-y-6">
    <section class="grid gap-6 xl:grid-cols-2">
      <el-card class="expense-wb-panel">
        <template #header>
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-lg font-semibold text-slate-800">直连列表</p>
              <p class="mt-1 text-sm text-slate-500">按公司账户维护招商银行云直连，同一公司同时仅允许启用一个直连账户。</p>
            </div>
            <el-tag effect="plain">{{ bankLinks.length }} 个账户</el-tag>
          </div>
        </template>

        <div v-loading="listLoading" class="space-y-3">
          <button
            v-for="item in bankLinks"
            :key="item.companyBankAccountId"
            type="button"
            class="expense-wb-detail-card expense-bank-link-card"
            :class="{ 'expense-wb-detail-card--selected': selectedAccountId === item.companyBankAccountId }"
            @click="selectBankLink(item.companyBankAccountId)"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0 space-y-2 text-left">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="text-base font-semibold text-slate-800">{{ item.companyName || item.companyId }}</p>
                  <el-tag effect="plain">{{ item.bankName }}</el-tag>
                  <el-tag :type="item.directConnectEnabled ? 'success' : 'info'" effect="plain">
                    {{ item.directConnectStatusLabel || (item.directConnectEnabled ? '已启用' : '未启用') }}
                  </el-tag>
                </div>
                <p class="text-sm text-slate-600">
                  {{ item.accountName }} ｜ {{ item.accountNo }}
                </p>
                <div class="flex flex-wrap gap-2 text-xs text-slate-500">
                  <span>最近直连：{{ item.lastDirectConnectStatus || '未推送' }}</span>
                  <span>最近回单：{{ item.lastReceiptStatus || '未生成' }}</span>
                </div>
              </div>
              <el-icon class="mt-1 text-slate-300"><ArrowRight /></el-icon>
            </div>
          </button>

          <el-empty v-if="!listLoading && !bankLinks.length" description="暂无可配置的公司账户" :image-size="84" />
        </div>
      </el-card>

      <el-card class="expense-wb-panel">
        <template #header>
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-lg font-semibold text-slate-800">招商银行云直连配置</p>
              <p class="mt-1 text-sm text-slate-500">配置保存到公司账户主档，回调成功后付款单自动进入已支付状态。</p>
            </div>
            <el-tag type="primary" effect="plain">首期仅支持招商银行云直连</el-tag>
          </div>
        </template>

        <div v-loading="detailLoading">
          <el-empty v-if="!selectedConfig" description="请选择左侧一个公司账户" :image-size="90" />

          <template v-else>
            <div class="mb-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="grid gap-3 md:grid-cols-2">
                <div>
                  <p class="text-xs uppercase tracking-[0.2em] text-slate-400">公司</p>
                  <p class="mt-1 text-sm font-semibold text-slate-700">{{ selectedConfig.companyName || selectedConfig.companyId }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-[0.2em] text-slate-400">账户</p>
                  <p class="mt-1 text-sm font-semibold text-slate-700">{{ selectedConfig.accountName }} ｜ {{ selectedConfig.accountNo }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-[0.2em] text-slate-400">最近直连结果</p>
                  <p class="mt-1 text-sm text-slate-600">{{ selectedConfig.lastDirectConnectStatus || '未推送' }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-[0.2em] text-slate-400">最近错误</p>
                  <p class="mt-1 text-sm text-slate-600">{{ selectedConfig.lastDirectConnectError || '无' }}</p>
                </div>
              </div>
            </div>

            <el-form label-position="top" class="grid gap-x-4 md:grid-cols-2">
              <el-form-item label="启用直连">
                <el-switch v-model="form.enabled" />
              </el-form-item>
              <el-form-item label="自动回单查询">
                <el-switch v-model="form.receiptQueryEnabled" />
              </el-form-item>
              <el-form-item label="银行提供方">
                <el-input :model-value="form.directConnectProvider" disabled />
              </el-form-item>
              <el-form-item label="直连通道">
                <el-input :model-value="form.directConnectChannel" disabled />
              </el-form-item>
              <el-form-item label="经办 Key" required>
                <el-input v-model="form.operatorKey" placeholder="请输入招商银行经办 Key" />
              </el-form-item>
              <el-form-item label="回调密钥" required>
                <el-input v-model="form.callbackSecret" placeholder="请输入银行回调密钥" show-password />
              </el-form-item>
              <el-form-item label="客户号">
                <el-input v-model="form.directConnectCustomerNo" placeholder="请输入客户号" />
              </el-form-item>
              <el-form-item label="应用 ID">
                <el-input v-model="form.directConnectAppId" placeholder="请输入应用 ID" />
              </el-form-item>
              <el-form-item label="账户别名">
                <el-input v-model="form.directConnectAccountAlias" placeholder="请输入账户别名" />
              </el-form-item>
              <el-form-item label="认证模式">
                <el-input v-model="form.directConnectAuthMode" placeholder="例如：RSA2" />
              </el-form-item>
              <el-form-item label="接口地址">
                <el-input v-model="form.directConnectApiBaseUrl" placeholder="请输入招行云直连接口地址" />
              </el-form-item>
              <el-form-item label="协议版本">
                <el-input v-model="form.directConnectProtocol" placeholder="例如：CMB_CLOUD_V1" />
              </el-form-item>
              <el-form-item label="证书引用">
                <el-input v-model="form.directConnectCertRef" placeholder="请输入证书引用" />
              </el-form-item>
              <el-form-item label="密钥引用">
                <el-input v-model="form.directConnectSecretRef" placeholder="请输入密钥引用" />
              </el-form-item>
              <el-form-item label="银行公钥引用">
                <el-input v-model="form.publicKeyRef" placeholder="请输入银行公钥引用" />
              </el-form-item>
              <el-form-item label="签名算法">
                <el-input v-model="form.directConnectSignType" placeholder="例如：RSA2" />
              </el-form-item>
              <el-form-item label="加密算法">
                <el-input v-model="form.directConnectEncryptType" placeholder="例如：AES256" />
              </el-form-item>
            </el-form>

            <div class="mt-6 flex flex-wrap justify-end gap-3">
              <el-button
                :disabled="!canEdit || !selectedConfig"
                @click="toggleEnabled"
              >
                {{ form.enabled ? '停用' : '启用' }}
              </el-button>
              <el-button type="primary" :disabled="!canEdit || !selectedConfig" :loading="saving" @click="saveConfig">
                保存配置
              </el-button>
            </div>
          </template>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import {
  expensePaymentApi,
  type ExpenseBankLinkConfig,
  type ExpenseBankLinkSavePayload,
  type ExpenseBankLinkSummary
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const listLoading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const bankLinks = ref<ExpenseBankLinkSummary[]>([])
const selectedAccountId = ref<number | null>(null)
const selectedConfig = ref<ExpenseBankLinkConfig | null>(null)
const permissionCodes = (readStoredUser()?.permissionCodes || []) as string[]
const canEdit = computed(() => hasPermission('expense:payment:bank_link:edit', permissionCodes))

const form = reactive<ExpenseBankLinkSavePayload>({
  enabled: false,
  directConnectProvider: 'CMB',
  directConnectChannel: 'CMB_CLOUD',
  directConnectProtocol: '',
  directConnectCustomerNo: '',
  directConnectAppId: '',
  directConnectAccountAlias: '',
  directConnectAuthMode: '',
  directConnectApiBaseUrl: '',
  directConnectCertRef: '',
  directConnectSecretRef: '',
  directConnectSignType: '',
  directConnectEncryptType: '',
  operatorKey: '',
  callbackSecret: '',
  publicKeyRef: '',
  receiptQueryEnabled: true
})

void loadBankLinks()

async function loadBankLinks() {
  listLoading.value = true
  try {
    const res = await expensePaymentApi.listBankLinks()
    bankLinks.value = res.data || []
    if (bankLinks.value.length && !selectedAccountId.value) {
      await selectBankLink(bankLinks.value[0]!.companyBankAccountId)
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载银企直连列表失败'))
  } finally {
    listLoading.value = false
  }
}

async function selectBankLink(companyBankAccountId: number) {
  selectedAccountId.value = companyBankAccountId
  detailLoading.value = true
  try {
    const res = await expensePaymentApi.getBankLink(companyBankAccountId)
    selectedConfig.value = res.data
    syncForm(res.data)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载直连配置失败'))
  } finally {
    detailLoading.value = false
  }
}

function syncForm(config: ExpenseBankLinkConfig) {
  form.enabled = Boolean(config.directConnectEnabled)
  form.directConnectProvider = config.directConnectProvider || 'CMB'
  form.directConnectChannel = config.directConnectChannel || 'CMB_CLOUD'
  form.directConnectProtocol = config.directConnectProtocol || ''
  form.directConnectCustomerNo = config.directConnectCustomerNo || ''
  form.directConnectAppId = config.directConnectAppId || ''
  form.directConnectAccountAlias = config.directConnectAccountAlias || ''
  form.directConnectAuthMode = config.directConnectAuthMode || ''
  form.directConnectApiBaseUrl = config.directConnectApiBaseUrl || ''
  form.directConnectCertRef = config.directConnectCertRef || ''
  form.directConnectSecretRef = config.directConnectSecretRef || ''
  form.directConnectSignType = config.directConnectSignType || ''
  form.directConnectEncryptType = config.directConnectEncryptType || ''
  form.operatorKey = config.operatorKey || ''
  form.callbackSecret = config.callbackSecret || ''
  form.publicKeyRef = config.publicKeyRef || ''
  form.receiptQueryEnabled = Boolean(config.receiptQueryEnabled)
}

async function saveConfig() {
  if (!selectedAccountId.value) {
    return
  }
  saving.value = true
  try {
    const res = await expensePaymentApi.updateBankLink(selectedAccountId.value, { ...form })
    selectedConfig.value = res.data
    syncForm(res.data)
    await loadBankLinks()
    ElMessage.success('银企直连配置已保存')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存直连配置失败'))
  } finally {
    saving.value = false
  }
}

async function toggleEnabled() {
  form.enabled = !form.enabled
  await saveConfig()
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error && typeof error === 'object' && 'message' in error && typeof (error as { message?: unknown }).message === 'string') {
    return (error as { message: string }).message
  }
  return fallback
}
</script>

<style scoped>
.expense-bank-link-card {
  width: 100%;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.2);
}
</style>
