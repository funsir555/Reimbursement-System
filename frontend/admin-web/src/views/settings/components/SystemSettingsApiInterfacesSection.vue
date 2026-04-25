<template>
  <SystemSettingsApiInterfacesTab
    :active-api-interface="state.activeApiInterface"
    :api-interface-options="state.apiInterfaceOptions"
    :active-api-interface-option="state.activeApiInterfaceOption"
    :active-ocr-provider-code="state.activeOcrProviderCode"
    :ocr-vendor-options="state.ocrVendorOptions"
    :active-ocr-provider="state.activeOcrProvider"
    :active-ocr-provider-label="state.activeOcrProviderLabel"
    :ocr-ram-policy-snippet="state.ocrRamPolicySnippet"
    :active-ocr-secret-hint="state.activeOcrSecretHint"
    :ocr-form="state.ocrForm"
    :ocr-save-loading="state.ocrSaveLoading"
    :ocr-test-loading="state.ocrTestLoading"
    :can-ocr-edit="permissions.canOcrEdit"
    :can-ocr-test="permissions.canOcrTest"
    :save-ocr-provider="actions.saveOcrProvider"
    :test-ocr-provider-config="actions.testOcrProviderConfig"
    @update:active-api-interface="$emit('update:activeApiInterface', $event)"
    @update:active-ocr-provider-code="$emit('update:activeOcrProviderCode', $event)"
  />
</template>

<script setup lang="ts">
import type { OcrProviderConfig } from '@/api'
import type {
  ApiInterfaceKey,
  ApiInterfaceOption,
  OcrFormState,
  OcrProviderCode
} from '../systemSettingsShared'
import SystemSettingsApiInterfacesTab from './SystemSettingsApiInterfacesTab.vue'

defineProps<{
  permissions: {
    canOcrEdit: boolean
    canOcrTest: boolean
  }
  state: {
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
  }
  actions: {
    saveOcrProvider: () => Promise<void>
    testOcrProviderConfig: () => Promise<void>
  }
}>()

defineEmits<{
  (e: 'update:activeApiInterface', value: ApiInterfaceKey): void
  (e: 'update:activeOcrProviderCode', value: OcrProviderCode): void
}>()
</script>
