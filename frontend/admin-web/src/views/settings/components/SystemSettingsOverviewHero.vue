<template>
  <section class="rounded-3xl bg-white px-5 py-4 shadow-sm">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-xs font-medium uppercase tracking-[0.24em] text-slate-400">
          System Settings
        </p>
        <h1 class="mt-1 text-2xl font-bold text-slate-900">系统设置中心</h1>
        <p class="mt-1 max-w-3xl text-sm leading-5 text-slate-500">
          统一维护组织架构、员工、权限与公司资料，并支持第三方组织同步配置与执行。
        </p>
      </div>
      <div class="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
        <div class="rounded-2xl bg-slate-50 px-3.5 py-2.5">
          <div class="text-xs text-slate-500">部门数</div>
          <div class="mt-1 text-xl font-semibold text-slate-900">{{ departmentCount }}</div>
        </div>
        <div class="rounded-2xl bg-slate-50 px-3.5 py-2.5">
          <div class="text-xs text-slate-500">员工数</div>
          <div class="mt-1 text-xl font-semibold text-slate-900">{{ employeesCount }}</div>
        </div>
        <div class="rounded-2xl bg-slate-50 px-3.5 py-2.5">
          <div class="text-xs text-slate-500">角色数</div>
          <div class="mt-1 text-xl font-semibold text-slate-900">{{ rolesCount }}</div>
        </div>
        <div class="rounded-2xl bg-slate-50 px-3.5 py-2.5">
          <div class="text-xs text-slate-500">公司数</div>
          <div class="mt-1 text-xl font-semibold text-slate-900">{{ companyCount }}</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  departmentCount: number
  employeesCount: number
  rolesCount: number
  companyCount: number
}>()
</script>
