<template>
  <el-dialog
    :model-value="modelValue"
    :title="actionLabel"
    width="920px"
    destroy-on-close
    @close="emit('update:modelValue', false)"
  >
    <div class="space-y-4">
      <div class="flex flex-wrap items-center gap-3">
        <input
          v-model.trim="keyword"
          class="min-w-[220px] flex-1 rounded-2xl border border-slate-200 px-4 py-2.5 text-sm text-slate-700 outline-none transition focus:border-sky-400"
          placeholder="搜索报销单编号、标题或模板名称"
          @keyup.enter="loadOptions"
        />
        <button
          type="button"
          class="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-sky-300 hover:bg-sky-50"
          @click="loadOptions"
        >
          搜索
        </button>
      </div>

      <div v-if="loading" class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
        正在加载可选报销单...
      </div>

      <div v-else-if="groups.length" class="space-y-4">
        <div
          v-for="group in groups"
          :key="group.templateType"
          class="rounded-[24px] border border-slate-200 bg-slate-50 p-4"
        >
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-slate-800">{{ group.templateTypeLabel }}</p>
              <p class="mt-1 text-xs text-slate-400">共 {{ group.total }} 张可选报销单</p>
            </div>
          </div>

          <div class="mt-4 space-y-3">
            <button
              v-for="item in group.items"
              :key="item.documentCode"
              type="button"
              class="w-full rounded-[20px] border px-4 py-4 text-left transition"
              :class="selectedDocumentCode === item.documentCode
                ? 'border-sky-400 bg-sky-50 shadow-sm'
                : 'border-white bg-white hover:border-slate-200'"
              @click="selectedDocumentCode = item.documentCode"
            >
              <div class="flex flex-wrap items-start justify-between gap-4">
                <div class="min-w-0 flex-1">
                  <p class="break-all text-sm font-semibold text-slate-800">
                    {{ item.documentTitle || item.documentCode }}
                  </p>
                  <p class="mt-1 break-all text-xs text-slate-500">单据编号：{{ item.documentCode }}</p>
                  <p class="mt-1 text-xs text-slate-500">
                    模板：{{ item.templateName || item.templateTypeLabel }} / 状态：{{ item.statusLabel || item.status }}
                  </p>
                </div>
                <div class="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-right">
                  <p class="text-xs text-slate-400">可用核销金额</p>
                  <p class="mt-1 text-sm font-semibold text-slate-800">¥ {{ formatAmount(item.availableWriteOffAmount) }}</p>
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>

      <div v-else class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
        暂无可用于核销的报销单
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" :disabled="!selectedDocumentCode" @click="confirmSelection">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  dashboardApi,
  type ExpenseDocumentPickerGroup,
  type ExpenseDocumentPickerItem
} from '@/api'
import { formatMoney } from '@/utils/money'

const props = defineProps<{
  modelValue: boolean
  targetDocumentCode: string
  actionLabel: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', item: ExpenseDocumentPickerItem): void
}>()

const loading = ref(false)
const keyword = ref('')
const groups = ref<ExpenseDocumentPickerGroup[]>([])
const selectedDocumentCode = ref('')

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      groups.value = []
      keyword.value = ''
      selectedDocumentCode.value = ''
      return
    }
    void loadOptions()
  }
)

async function loadOptions() {
  if (!props.targetDocumentCode) {
    return
  }
  loading.value = true
  try {
    const res = await dashboardApi.getWriteoffReportPicker(props.targetDocumentCode, keyword.value || undefined)
    groups.value = res.data.groups || []
    const availableCodes = new Set(groups.value.flatMap((group) => group.items.map((item) => item.documentCode)))
    if (!availableCodes.has(selectedDocumentCode.value)) {
      selectedDocumentCode.value = groups.value[0]?.items[0]?.documentCode || ''
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载可核销报销单失败')
  } finally {
    loading.value = false
  }
}

function confirmSelection() {
  const selectedItem = groups.value
    .flatMap((group) => group.items)
    .find((item) => item.documentCode === selectedDocumentCode.value)
  if (!selectedItem) {
    return
  }
  emit('confirm', selectedItem)
}

function formatAmount(value?: string) {
  return formatMoney(value || '0.00')
}
</script>
