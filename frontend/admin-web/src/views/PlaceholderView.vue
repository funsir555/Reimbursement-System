<template>
  <div
    class="placeholder-page"
    :class="isFinanceArchivePlaceholder ? 'placeholder-page-compact' : 'min-h-[480px] flex items-center justify-center'"
  >
    <el-card :class="isFinanceArchivePlaceholder ? 'w-full rounded-[26px]' : 'w-full max-w-3xl'">
      <div :class="isFinanceArchivePlaceholder ? 'placeholder-compact' : 'flex items-start gap-4'">
        <div
          :class="isFinanceArchivePlaceholder ? 'placeholder-compact-icon' : 'w-14 h-14 rounded-2xl bg-blue-50 flex items-center justify-center flex-shrink-0'"
        >
          <el-icon :size="isFinanceArchivePlaceholder ? 18 : 28" class="text-blue-600"><Tools /></el-icon>
        </div>
        <div class="flex-1">
          <div :class="isFinanceArchivePlaceholder ? 'placeholder-compact-head' : ''">
            <h1 :class="isFinanceArchivePlaceholder ? 'text-xl font-bold text-gray-800' : 'text-2xl font-bold text-gray-800'">
              {{ pageTitle }}
            </h1>
            <span v-if="isFinanceArchivePlaceholder" class="placeholder-status">功能建设中</span>
          </div>
          <p v-if="!isFinanceArchivePlaceholder" class="mt-2 text-gray-500 leading-7">
            {{ pageDescription }}
          </p>
          <div :class="isFinanceArchivePlaceholder ? 'mt-4 flex flex-wrap gap-2' : 'mt-6 flex gap-3'">
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

const PLACEHOLDER_TEXT_MAP: Record<string, { title: string; description: string }> = {
  '/finance/general-ledger/review-voucher': { title: '审核凭证', description: '功能建设中' },
  '/finance/general-ledger/balance-sheet': { title: '总账余额表', description: '功能建设中' },
  '/finance/general-ledger/detail-ledger': { title: '明细账', description: '功能建设中' },
  '/finance/general-ledger/general-ledger': { title: '总分类账', description: '功能建设中' },
  '/finance/general-ledger/project-detail-ledger': { title: '项目明细账', description: '功能建设中' },
  '/finance/general-ledger/supplier-detail-ledger': { title: '供应商明细账', description: '功能建设中' },
  '/finance/general-ledger/customer-detail-ledger': { title: '客户明细账', description: '功能建设中' },
  '/finance/general-ledger/personal-detail-ledger': { title: '个人明细账', description: '功能建设中' },
  '/finance/general-ledger/quantity-amount-detail-ledger': { title: '数量金额明细账', description: '功能建设中' },
  '/finance/archives/departments': { title: '部门档案', description: '功能建设中' },
  '/finance/archives/account-subjects': { title: '会计科目', description: '功能建设中' }
}

const pageTitle = computed(() => {
  const fallback = PLACEHOLDER_TEXT_MAP[route.path]?.title
  return String(fallback || route.meta.title || '功能页面')
})

const pageDescription = computed(() => {
  const fallback = PLACEHOLDER_TEXT_MAP[route.path]?.description
  return String(fallback || route.meta.description || '功能建设中')
})

const isFinanceArchivePlaceholder = computed(() => route.path.startsWith('/finance/archives/'))
</script>

<style scoped>
.placeholder-page-compact {
  min-height: auto;
}

.placeholder-compact {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px 4px;
}

.placeholder-compact-icon {
  display: flex;
  height: 38px;
  width: 38px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: #eff6ff;
}

.placeholder-compact-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.placeholder-status {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #eff6ff;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
}
</style>
