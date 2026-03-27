<template>
  <el-drawer
    :model-value="modelValue"
    title="下载中心"
    size="420px"
    @close="$emit('update:modelValue', false)"
  >
    <div class="space-y-6">
      <div class="grid grid-cols-2 gap-4">
        <div class="rounded-3xl bg-blue-50 px-4 py-4">
          <p class="text-sm text-slate-500">进行中</p>
          <p class="mt-2 text-2xl font-bold text-slate-800">{{ center?.inProgress.length || 0 }}</p>
        </div>
        <div class="rounded-3xl bg-slate-100 px-4 py-4">
          <p class="text-sm text-slate-500">历史记录</p>
          <p class="mt-2 text-2xl font-bold text-slate-800">{{ center?.history.length || 0 }}</p>
        </div>
      </div>

      <section>
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-base font-semibold text-slate-800">正在下载</h3>
          <el-button text @click="loadData">刷新</el-button>
        </div>

        <div v-if="center?.inProgress.length" class="space-y-3">
          <div
            v-for="item in center.inProgress"
            :key="item.id"
            class="rounded-3xl border border-blue-100 bg-blue-50/60 px-4 py-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-slate-800">{{ item.fileName }}</p>
                <p class="mt-1 text-sm text-slate-500">{{ item.businessType }} · {{ item.fileSize }}</p>
              </div>
              <el-tag type="primary" effect="plain">{{ item.status }}</el-tag>
            </div>
            <el-progress class="mt-4" :stroke-width="10" :percentage="item.progress" color="#2563eb" />
            <p class="mt-2 text-xs text-slate-400">创建时间：{{ item.createdAt }}</p>
          </div>
        </div>
        <el-empty v-else description="当前没有进行中的下载任务" />
      </section>

      <section>
        <h3 class="mb-3 text-base font-semibold text-slate-800">下载记录</h3>
        <div v-if="center?.history.length" class="space-y-3">
          <div
            v-for="item in center.history"
            :key="item.id"
            class="rounded-3xl border border-slate-200 bg-white px-4 py-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-slate-800">{{ item.fileName }}</p>
                <p class="mt-1 text-sm text-slate-500">{{ item.businessType }} · {{ item.fileSize }}</p>
              </div>
              <el-tag :type="item.status === '已完成' ? 'success' : 'warning'" effect="plain">
                {{ item.status }}
              </el-tag>
            </div>
            <div class="mt-3 space-y-1 text-xs text-slate-400">
              <p>创建时间：{{ item.createdAt }}</p>
              <p v-if="item.finishedAt">完成时间：{{ item.finishedAt }}</p>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无下载记录" />
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadApi, type DownloadCenterData } from '@/api'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'loaded', count: number): void
}>()

const center = ref<DownloadCenterData | null>(null)

let pollingTimer: number | null = null

const stopPolling = () => {
  if (pollingTimer !== null) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const syncPolling = () => {
  if (!props.modelValue) {
    stopPolling()
    return
  }

  if ((center.value?.inProgress.length || 0) === 0) {
    stopPolling()
    return
  }

  if (pollingTimer === null) {
    pollingTimer = window.setInterval(() => {
      loadData(true)
    }, 3000)
  }
}

const loadData = async (silent = false) => {
  try {
    const res = await downloadApi.getCenter()
    if (res.code === 200) {
      center.value = res.data
      emit('loaded', res.data.inProgress.length)
      syncPolling()
    }
  } catch (error: any) {
    if (!silent) {
      ElMessage.error(error.message || '加载下载记录失败')
    }
    stopPolling()
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      loadData()
      return
    }
    stopPolling()
  },
  { immediate: true }
)
</script>
