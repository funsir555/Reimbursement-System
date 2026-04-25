<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex items-center justify-between">
        <div class="font-semibold text-slate-900">公司主体与抬头</div>
        <div class="flex gap-2">
          <el-button v-if="canCreate" type="primary" @click="$emit('create')">
            新增公司
          </el-button>
          <el-button v-if="canEdit" :disabled="!selectedCompany" @click="$emit('edit', selectedCompany)">
            编辑
          </el-button>
          <el-button
            v-if="canDelete"
            :disabled="!selectedCompany"
            type="danger"
            plain
            @click="$emit('delete-selected')"
          >
            删除
          </el-button>
        </div>
      </div>
    </template>
    <el-table :data="flatCompanies" highlight-current-row @current-change="$emit('select-company', $event)">
      <el-table-column label="公司名称" min-width="180">
        <template #default="{ row }">
          <div :style="{ paddingLeft: `${row.level * 20}px` }">{{ row.companyName }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="companyCode" label="编号" min-width="120" />
      <el-table-column prop="invoiceTitle" label="公司抬头" min-width="180" />
      <el-table-column prop="taxNo" label="税号" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ formatStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import type { CompanyRecord } from '@/api'
import type { FlatCompanyRecord } from '../systemSettingsShared'

defineProps<{
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  flatCompanies: FlatCompanyRecord[]
  selectedCompany?: CompanyRecord
  formatStatusLabel: (status: number) => string
}>()

defineEmits<{
  (e: 'select-company', company?: CompanyRecord): void
  (e: 'create'): void
  (e: 'edit', company?: CompanyRecord): void
  (e: 'delete-selected'): void
}>()
</script>
