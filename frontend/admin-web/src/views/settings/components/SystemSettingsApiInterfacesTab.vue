<template>
  <div class="grid gap-4 xl:grid-cols-[1fr,9fr]">
    <el-card shadow="never">
      <template #header>
        <div class="font-semibold text-slate-900">接口列表</div>
      </template>
      <div class="space-y-2">
        <el-button
          v-for="item in apiInterfaceOptions"
          :key="item.key"
          :type="activeApiInterface === item.key ? 'primary' : 'info'"
          :plain="activeApiInterface !== item.key"
          class="api-interface-nav-btn"
          :data-testid="`api-interface-nav-${item.key}`"
          @click="$emit('update:activeApiInterface', item.key)"
        >
          <span class="api-interface-nav-content">
            <span class="font-semibold">{{ item.label }}</span>
            <span class="text-xs text-slate-500">{{ item.caption }}</span>
          </span>
        </el-button>
      </div>
    </el-card>

    <div class="space-y-4">
      <template v-if="activeApiInterface === 'ocr'">
        <section class="rounded-3xl border border-slate-200 bg-slate-50/70 px-5 py-5">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <p class="text-xs font-medium uppercase tracking-[0.24em] text-slate-400">
                OCR Cloud Provider
              </p>
              <h2 class="mt-1 text-2xl font-bold text-slate-900" data-testid="api-interface-title">
                OCR 云端接入配置
              </h2>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
                OCR 真实能力仅用于发票附件上传链路；上传成功不等于 OCR 成功，识别失败不会回滚附件上传。
              </p>
            </div>
            <div class="flex flex-wrap gap-2">
              <el-button
                v-for="vendor in ocrVendorOptions"
                :key="vendor.code"
                size="small"
                :type="activeOcrProviderCode === vendor.code ? 'primary' : 'info'"
                :plain="activeOcrProviderCode !== vendor.code"
                :data-testid="`ocr-provider-switch-${vendor.code.toLowerCase()}`"
                @click="$emit('update:activeOcrProviderCode', vendor.code)"
              >
                {{ vendor.label }}
              </el-button>
            </div>
          </div>
        </section>

        <el-card shadow="never" data-testid="ocr-ram-guidance">
          <template #header>
            <div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
              <div class="font-semibold text-slate-900">RAM 最小权限建议</div>
              <div class="text-xs text-slate-500">
                后续发票 OCR 调用请优先使用专用 RAM 用户，不要使用主账号 AccessKey
              </div>
            </div>
          </template>

          <div class="grid gap-4 xl:grid-cols-[1.5fr,1fr]">
            <div class="grid gap-4 md:grid-cols-2">
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">推荐账号模型</div>
                <div class="mt-2 text-sm font-medium text-slate-800">专用 RAM 用户：`finex-ocr-runtime`</div>
                <p class="mt-2 text-sm leading-6 text-slate-500">
                  仅用于后端调用阿里云发票 OCR，不复用主账号 AccessKey，不和其它云服务共用同一组 AK / SK。
                </p>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">最小权限策略</div>
                <div class="mt-2 text-sm font-medium text-slate-800">`FinexInvoiceOcrRuntimePolicy`</div>
                <p class="mt-2 text-sm leading-6 text-slate-500">
                  首期只放行 `ocr:RecognizeInvoice`；若未来接发票验真，再单独增加 `ocr:VerifyVATInvoice`。
                </p>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">密钥治理</div>
                <p class="mt-2 text-sm leading-6 text-slate-500">
                  Secret 只允许后端保存和测试；前端只展示脱敏值，日志中禁止打印明文 `AccessKey Secret` 或完整 `Authorization` 头。
                </p>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">轮换建议</div>
                <p class="mt-2 text-sm leading-6 text-slate-500">
                  建议保留双 Key 窗口：新增新 Key、切配置、验证成功后再禁用旧 Key；若出口 IP 固定，再额外加来源 IP 条件限制。
                </p>
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200 bg-slate-950 px-4 py-4 text-slate-100">
              <div class="text-xs uppercase tracking-[0.2em] text-slate-400">策略片段</div>
              <p class="mt-2 text-xs leading-5 text-slate-300">
                复制后可在阿里云 RAM 自定义策略中直接使用，OCR 是操作级授权，`Resource` 固定为 `*`。
              </p>
              <pre
                class="mt-3 overflow-x-auto rounded-2xl bg-slate-900/80 p-3 text-xs leading-6 text-emerald-200"
                data-testid="ocr-ram-policy-snippet"
              ><code>{{ ocrRamPolicySnippet }}</code></pre>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
              <div class="font-semibold text-slate-900">厂商配置</div>
              <div class="text-xs text-slate-500">运行时只允许一个启用中的 OCR 厂商</div>
            </div>
          </template>

          <div v-if="activeOcrProviderCode !== 'ALIYUN'" class="space-y-4">
            <div class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm leading-6 text-amber-700">
              {{ activeOcrProvider?.providerName || activeOcrProviderLabel }} OCR 将在后续版本接入；当前页面仅保留厂商切换入口，不保存真实配置。
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">当前状态</div>
                <div class="mt-2 text-sm font-medium text-slate-800">待接入</div>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">接入范围</div>
                <div class="mt-2 text-sm font-medium text-slate-800">首期仅开放阿里云 OCR 真实接入</div>
              </div>
            </div>
          </div>

          <div v-else class="space-y-5" data-testid="ocr-provider-panel">
            <div class="grid gap-4 md:grid-cols-3">
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">启用状态</div>
                <div class="mt-2 flex items-center gap-3">
                  <el-switch v-model="ocrForm.enabled" :active-value="true" :inactive-value="false" />
                  <span class="text-sm font-medium text-slate-800">
                    {{ ocrForm.enabled ? '已启用' : '未启用' }}
                  </span>
                </div>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">最近测试</div>
                <div class="mt-2 text-sm font-medium text-slate-800">
                  {{ activeOcrProvider?.lastTestStatus || 'IDLE' }}
                </div>
                <div class="mt-1 text-xs leading-5 text-slate-500">
                  {{ activeOcrProvider?.lastTestMessage || '尚未测试' }}
                </div>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">最近测试时间</div>
                <div class="mt-2 text-sm font-medium text-slate-800">
                  {{ activeOcrProvider?.lastTestAt || '--' }}
                </div>
              </div>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <el-form-item label="AccessKey ID">
                <el-input v-model="ocrForm.accessKeyId" data-testid="ocr-access-key-id" />
              </el-form-item>
              <el-form-item label="Endpoint">
                <el-input v-model="ocrForm.endpoint" data-testid="ocr-endpoint" />
              </el-form-item>
              <el-form-item label="AccessKey Secret">
                <el-input
                  v-model="ocrForm.accessKeySecret"
                  type="password"
                  show-password
                  placeholder="留空则保留当前已保存的密钥"
                  data-testid="ocr-access-key-secret"
                />
                <div class="mt-2 text-xs leading-5 text-slate-500">
                  {{ activeOcrSecretHint }}
                </div>
              </el-form-item>
              <el-form-item label="连接超时 (ms)">
                <el-input v-model.number="ocrForm.connectTimeoutMs" type="number" data-testid="ocr-connect-timeout" />
              </el-form-item>
              <el-form-item label="读取超时 (ms)">
                <el-input v-model.number="ocrForm.readTimeoutMs" type="number" data-testid="ocr-read-timeout" />
              </el-form-item>
            </div>

            <div class="flex flex-wrap gap-3">
              <el-button
                type="primary"
                :loading="ocrSaveLoading"
                :disabled="!canOcrEdit"
                data-testid="ocr-save-button"
                @click="saveOcrProvider()"
              >
                保存配置
              </el-button>
              <el-button
                :loading="ocrTestLoading"
                :disabled="!canOcrTest"
                data-testid="ocr-test-button"
                @click="testOcrProviderConfig()"
              >
                测试配置
              </el-button>
            </div>
          </div>
        </el-card>
      </template>

      <template v-else>
        <section class="rounded-3xl border border-slate-200 bg-slate-50/70 px-5 py-5">
          <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <p class="text-xs font-medium uppercase tracking-[0.24em] text-slate-400">
                API Interface
              </p>
              <h2 class="mt-1 text-2xl font-bold text-slate-900" data-testid="api-interface-title">
                {{ activeApiInterfaceOption.title }}
              </h2>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
                {{ activeApiInterfaceOption.description }}
              </p>
            </div>
            <div class="flex flex-wrap gap-2">
              <span class="api-interface-status-chip">静态占位</span>
              <span class="api-interface-status-chip api-interface-status-chip-muted">
                待接入
              </span>
            </div>
          </div>
        </section>

        <el-card shadow="never">
          <template #header>
            <div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
              <div class="font-semibold text-slate-900">预留配置项</div>
              <div class="text-xs text-slate-500">当前仅展示字段规划，不保存配置</div>
            </div>
          </template>
          <div class="grid gap-4 md:grid-cols-2">
            <div
              v-for="field in activeApiInterfaceOption.fields"
              :key="`${activeApiInterfaceOption.key}-${field.label}`"
              class="rounded-2xl border border-slate-200 bg-white px-4 py-3"
            >
              <div class="text-xs uppercase tracking-[0.2em] text-slate-400">
                {{ field.label }}
              </div>
              <div class="mt-2 text-sm font-medium leading-6 text-slate-800">
                {{ field.value }}
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="font-semibold text-slate-900">接入说明</div>
          </template>
          <div class="space-y-3">
            <div class="flex flex-wrap gap-2">
              <span
                v-for="tag in activeApiInterfaceOption.tags"
                :key="`${activeApiInterfaceOption.key}-${tag}`"
                class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600"
              >
                {{ tag }}
              </span>
            </div>
            <p class="text-sm leading-6 text-slate-500" data-testid="api-interface-note">
              {{ activeApiInterfaceOption.note }}
            </p>
          </div>
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { OcrProviderConfig } from '@/api'
import type {
  ApiInterfaceKey,
  ApiInterfaceOption,
  OcrFormState,
  OcrProviderCode
} from '../systemSettingsShared'

defineProps<{
  activeApiInterface: ApiInterfaceKey
  apiInterfaceOptions: ApiInterfaceOption[]
  activeApiInterfaceOption: ApiInterfaceOption
  activeOcrProviderCode: OcrProviderCode
  ocrVendorOptions: Array<{ code: OcrProviderCode; label: string }>
  activeOcrProvider: OcrProviderConfig | null
  activeOcrProviderLabel: string
  ocrRamPolicySnippet: string
  activeOcrSecretHint: string
  ocrForm: OcrFormState
  ocrSaveLoading: boolean
  ocrTestLoading: boolean
  canOcrEdit: boolean
  canOcrTest: boolean
  saveOcrProvider: () => Promise<void>
  testOcrProviderConfig: () => Promise<void>
}>()

defineEmits<{
  (e: 'update:activeApiInterface', value: ApiInterfaceKey): void
  (e: 'update:activeOcrProviderCode', value: OcrProviderCode): void
}>()
</script>

<style scoped>
.api-interface-nav-btn {
  width: 100%;
  height: auto;
  justify-content: flex-start;
  padding: 12px 16px;
}

:deep(.api-interface-nav-btn > span) {
  width: 100%;
}

.api-interface-nav-content {
  display: flex;
  width: 100%;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  text-align: left;
}

.api-interface-status-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  background: #e0f2fe;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #0369a1;
}

.api-interface-status-chip-muted {
  background: #e2e8f0;
  color: #475569;
}
</style>
