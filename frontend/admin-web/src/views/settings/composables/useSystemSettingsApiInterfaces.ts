import { computed, reactive, ref, watch, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemSettingsApi, type OcrProviderConfig } from '@/api'
import {
  DEFAULT_API_INTERFACE_OPTION,
  apiInterfaceOptions,
  normalizeOcrText,
  ocrVendorOptions,
  type ApiInterfaceKey,
  type OcrFormState,
  type OcrProviderCode
} from '../systemSettingsShared'

export function useSystemSettingsApiInterfaces(params: {
  ocrProviders: Ref<OcrProviderConfig[]>
  loadBootstrap: () => Promise<void>
}) {
  const { ocrProviders, loadBootstrap } = params

  const activeApiInterface = ref<ApiInterfaceKey>('ocr')
  const activeOcrProviderCode = ref<OcrProviderCode>('ALIYUN')
  const ocrSaveLoading = ref(false)
  const ocrTestLoading = ref(false)
  const ocrForm = reactive<OcrFormState>({
    enabled: false,
    accessKeyId: '',
    accessKeySecret: '',
    endpoint: 'ocr-api.cn-hangzhou.aliyuncs.com',
    connectTimeoutMs: 5000,
    readTimeoutMs: 15000
  })

  const activeApiInterfaceOption = computed(
    () => apiInterfaceOptions.find((item) => item.key === activeApiInterface.value) ?? DEFAULT_API_INTERFACE_OPTION
  )
  const activeOcrProvider = computed<OcrProviderConfig | null>(
    () => ocrProviders.value.find((item) => item.providerCode === activeOcrProviderCode.value) || null
  )
  const activeOcrProviderLabel = computed(
    () => ocrVendorOptions.find((item) => item.code === activeOcrProviderCode.value)?.label || 'OCR'
  )
  const activeOcrSecretHint = computed(() => {
    if (!activeOcrProvider.value?.hasSecret) {
      return '当前未保存密钥，保存前请填写 AccessKey Secret。'
    }
    return `仅当 AccessKey ID 不变时，留空才表示继续沿用当前已保存的 Secret：${activeOcrProvider.value.maskedSecret || '******'}`
  })

  watch(
    () => [activeOcrProviderCode.value, ocrProviders.value],
    () => {
      syncOcrFormFromProvider()
    },
    { immediate: true, deep: true }
  )

  function syncOcrFormFromProvider() {
    const provider = activeOcrProvider.value
    ocrForm.enabled = provider?.enabled || false
    ocrForm.accessKeyId = provider?.accessKeyId || ''
    ocrForm.accessKeySecret = ''
    ocrForm.endpoint = provider?.endpoint || 'ocr-api.cn-hangzhou.aliyuncs.com'
    ocrForm.connectTimeoutMs = provider?.connectTimeoutMs || 5000
    ocrForm.readTimeoutMs = provider?.readTimeoutMs || 15000
  }

  function isChangingAccessKeyIdWithoutSecret() {
    const nextAccessKeyId = normalizeOcrText(ocrForm.accessKeyId)
    const nextAccessKeySecret = normalizeOcrText(ocrForm.accessKeySecret)
    const savedAccessKeyId = normalizeOcrText(activeOcrProvider.value?.accessKeyId)
    return nextAccessKeyId !== savedAccessKeyId && nextAccessKeySecret.length === 0
  }

  async function saveOcrProvider() {
    if (activeOcrProviderCode.value !== 'ALIYUN') {
      ElMessage.warning('当前厂商待接入，暂不支持保存真实配置')
      return
    }
    if (isChangingAccessKeyIdWithoutSecret()) {
      ElMessage.warning('更换 AccessKey ID 时，必须同时重新填写 AccessKey Secret')
      return
    }
    ocrSaveLoading.value = true
    try {
      await systemSettingsApi.updateOcrProvider(activeOcrProviderCode.value, {
        enabled: ocrForm.enabled ? 1 : 0,
        accessKeyId: normalizeOcrText(ocrForm.accessKeyId),
        accessKeySecret: normalizeOcrText(ocrForm.accessKeySecret),
        endpoint: normalizeOcrText(ocrForm.endpoint),
        connectTimeoutMs: ocrForm.connectTimeoutMs,
        readTimeoutMs: ocrForm.readTimeoutMs
      })
      ElMessage.success('OCR 配置已保存')
      await loadBootstrap()
    } catch (error: unknown) {
      ElMessage.error(error instanceof Error && error.message ? error.message : 'OCR 配置保存失败')
    } finally {
      ocrSaveLoading.value = false
    }
  }

  async function testOcrProviderConfig() {
    if (activeOcrProviderCode.value !== 'ALIYUN') {
      ElMessage.warning('当前厂商待接入，暂不支持真实测试')
      return
    }
    ocrTestLoading.value = true
    try {
      const res = await systemSettingsApi.testOcrProvider(activeOcrProviderCode.value)
      ElMessage.success(
        res.data.lastTestStatus === 'SUCCESS' ? 'OCR 配置测试通过' : 'OCR 配置测试已完成'
      )
      await loadBootstrap()
    } catch (error: unknown) {
      ElMessage.error(error instanceof Error && error.message ? error.message : 'OCR 配置测试失败')
    } finally {
      ocrTestLoading.value = false
    }
  }

  return {
    activeApiInterface,
    activeOcrProviderCode,
    ocrSaveLoading,
    ocrTestLoading,
    ocrForm,
    activeApiInterfaceOption,
    activeOcrProvider,
    activeOcrProviderLabel,
    activeOcrSecretHint,
    saveOcrProvider,
    testOcrProviderConfig
  }
}
