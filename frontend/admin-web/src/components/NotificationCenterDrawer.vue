<template>
  <el-drawer
    :model-value="modelValue"
    title="消息通知"
    size="420px"
    @close="$emit('update:modelValue', false)"
  >
    <div class="flex h-full flex-col">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <p class="text-base font-semibold text-slate-800">通知中心</p>
          <p class="mt-1 text-sm text-slate-500">仅展示最近的异步任务通知</p>
        </div>
        <el-button
          text
          :disabled="!hasUnread || loading || actionLoading"
          data-testid="mark-all-read"
          @click="handleMarkAllRead"
        >
          全部标为已读
        </el-button>
      </div>

      <div v-loading="loading" class="min-h-[240px] flex-1">
        <div v-if="notifications.length" class="space-y-3">
          <button
            v-for="item in notifications"
            :key="item.id"
            type="button"
            class="block w-full rounded-3xl border px-4 py-4 text-left transition"
            :class="item.status === 'UNREAD'
              ? 'border-blue-200 bg-blue-50/80 shadow-sm'
              : 'border-slate-200 bg-white hover:border-slate-300'"
            :data-testid="`notification-item-${item.id}`"
            @click="handleRead(item)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span
                    v-if="item.status === 'UNREAD'"
                    class="inline-block h-2.5 w-2.5 rounded-full bg-blue-500"
                    data-testid="notification-unread-dot"
                  />
                  <p class="truncate text-sm font-semibold text-slate-800">
                    {{ item.title || '异步任务通知' }}
                  </p>
                </div>
                <p class="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-slate-600">
                  {{ item.content || '暂无通知内容' }}
                </p>
              </div>
              <span
                class="shrink-0 rounded-full px-2 py-1 text-xs font-medium"
                :class="item.status === 'UNREAD'
                  ? 'bg-blue-100 text-blue-700'
                  : 'bg-slate-100 text-slate-500'"
              >
                {{ item.status === 'UNREAD' ? '未读' : '已读' }}
              </span>
            </div>
            <div class="mt-3 flex items-center justify-between gap-3 text-xs text-slate-400">
              <span>创建时间：{{ item.createdAt || '--' }}</span>
              <span v-if="item.readAt">已读时间：{{ item.readAt }}</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="暂无消息通知" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { notificationApi, type NotificationItem } from '@/api'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'changed'): void
}>()

const notifications = ref<NotificationItem[]>([])
const loading = ref(false)
const actionLoading = ref(false)

const hasUnread = computed(() => notifications.value.some((item) => item.status === 'UNREAD'))

const loadData = async (silent = false) => {
  loading.value = true
  try {
    const res = await notificationApi.list()
    if (res.code === 200) {
      notifications.value = res.data
    }
  } catch (error: any) {
    if (!silent) {
      ElMessage.error(error.message || '加载通知失败')
    }
  } finally {
    loading.value = false
  }
}

const handleRead = async (item: NotificationItem) => {
  if (item.status !== 'UNREAD' || actionLoading.value) {
    return
  }

  actionLoading.value = true
  try {
    await notificationApi.markRead(item.id)
    item.status = 'READ'
    item.readAt = item.readAt || new Date().toLocaleString('zh-CN', { hour12: false })
    emit('changed')
  } catch (error: any) {
    ElMessage.error(error.message || '标记已读失败')
  } finally {
    actionLoading.value = false
  }
}

const handleMarkAllRead = async () => {
  if (!hasUnread.value || actionLoading.value) {
    return
  }

  actionLoading.value = true
  try {
    await notificationApi.markAllRead()
    const now = new Date().toLocaleString('zh-CN', { hour12: false })
    notifications.value = notifications.value.map((item) => ({
      ...item,
      status: 'READ',
      readAt: item.readAt || now
    }))
    emit('changed')
  } catch (error: any) {
    ElMessage.error(error.message || '全部已读失败')
  } finally {
    actionLoading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      void loadData()
    }
  },
  { immediate: true }
)
</script>
