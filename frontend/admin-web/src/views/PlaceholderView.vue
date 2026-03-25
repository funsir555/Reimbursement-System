<template>
  <div class="min-h-[480px] flex items-center justify-center">
    <el-card class="w-full max-w-3xl">
      <div class="flex items-start gap-4">
        <div class="w-14 h-14 rounded-2xl bg-blue-50 flex items-center justify-center flex-shrink-0">
          <el-icon :size="28" class="text-blue-600"><Tools /></el-icon>
        </div>
        <div class="flex-1">
          <h1 class="text-2xl font-bold text-gray-800">{{ pageTitle }}</h1>
          <p class="text-gray-500 mt-2 leading-7">
            {{ pageDescription }}
          </p>
          <div class="mt-6 flex gap-3">
            <el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button>
            <el-button @click="router.back()">返回上一页</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tools } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => String(route.meta.title || '功能页面'))
const pageDescription = computed(() =>
  String(route.meta.description || '这个页面的业务功能正在建设中，后续会继续完善。')
)
</script>
