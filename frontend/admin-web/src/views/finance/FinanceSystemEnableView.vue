<template>
  <div class="space-y-4 finance-system-enable-page">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">系统启用</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前财务公司</span>
            <strong>{{ currentCompanyDisplay || '未设置' }}</strong>
          </div>
          <div class="inline-flex items-center gap-2 rounded-full bg-amber-50 px-3 py-1.5 text-sm text-amber-700">
            <span class="font-semibold">当前状态</span>
            <strong>静态占位</strong>
          </div>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="space-y-3">
        <div class="flex flex-wrap items-center gap-3">
          <h2 class="text-lg font-semibold text-slate-800">功能建设中</h2>
          <el-tag effect="plain" type="info">首版静态占位</el-tag>
        </div>
        <p class="text-sm leading-7 text-slate-500">
          当前页面预留给账套级系统模块开关能力，后续真实实现再接入状态读取、启停操作和审计链路。
        </p>
        <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
          现阶段仅保留模块入口与页面占位，不发起后端请求，不修改任何账套状态。
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'

const financeCompany = useFinanceCompanyStore()
const financeCompanyState = financeCompany as typeof financeCompany & { currentCompanyLabel?: string }

const currentCompanyDisplay = computed(() =>
  financeCompanyState.currentCompanyLabel || financeCompany.currentCompanyName || financeCompany.currentCompanyId || ''
)
</script>
