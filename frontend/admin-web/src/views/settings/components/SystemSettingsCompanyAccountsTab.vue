<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap gap-3">
          <el-select
            :model-value="companyAccountCompanyFilter"
            clearable
            placeholder="所属公司"
            class="w-52"
            @update:model-value="$emit('update:companyAccountCompanyFilter', $event)"
          >
            <el-option
              v-for="item in companyOptions"
              :key="item.companyId"
              :label="item.label"
              :value="item.companyId"
            />
          </el-select>
          <el-select
            :model-value="companyAccountStatusFilter"
            clearable
            placeholder="状态"
            class="w-32"
            @update:model-value="$emit('update:companyAccountStatusFilter', $event)"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-select
            :model-value="companyAccountDirectConnectFilter"
            clearable
            placeholder="直连启用"
            class="w-36"
            @update:model-value="$emit('update:companyAccountDirectConnectFilter', $event)"
          >
            <el-option label="已启用" :value="1" />
            <el-option label="未启用" :value="0" />
          </el-select>
        </div>
        <div class="flex gap-2">
          <el-button v-if="canCreate" type="primary" @click="$emit('create')">
            新增账户
          </el-button>
        </div>
      </div>
    </template>
    <el-table :data="filteredCompanyBankAccounts">
      <el-table-column label="所属公司" min-width="180">
        <template #default="{ row }">
          {{ row.companyName || resolveCompanyName(row.companyId) }}
        </template>
      </el-table-column>
      <el-table-column prop="bankName" label="银行名称" min-width="160" />
      <el-table-column prop="branchName" label="开户网点" min-width="160" />
      <el-table-column prop="accountName" label="账户名" min-width="160" />
      <el-table-column label="账号" min-width="180">
        <template #default="{ row }">
          {{ maskAccountNo(row.accountNo) }}
        </template>
      </el-table-column>
      <el-table-column prop="accountUsage" label="账户用途" min-width="140" />
      <el-table-column label="默认账户" width="100">
        <template #default="{ row }">
          <el-tag :type="row.defaultAccount === 1 ? 'success' : 'info'">
            {{ formatBooleanTag(row.defaultAccount) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ formatStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="直连启用" width="100">
        <template #default="{ row }">
          <el-tag :type="row.directConnectEnabled === 1 ? 'warning' : 'info'">
            {{ formatBooleanTag(row.directConnectEnabled) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="直连渠道 / 提供方" min-width="180">
        <template #default="{ row }">
          {{ [row.directConnectChannel, row.directConnectProvider].filter(Boolean).join(' / ') || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="最近直连状态 / 时间" min-width="220">
        <template #default="{ row }">
          {{ row.directConnectLastSyncStatus || '-' }} / {{ row.directConnectLastSyncAt || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="260" fixed="right">
        <template #default="{ row }">
          <div class="flex flex-wrap gap-2">
            <el-button v-if="canEdit" link type="primary" @click="$emit('edit', row)">
              编辑
            </el-button>
            <el-button
              v-if="canEdit && row.status === 1 && row.defaultAccount !== 1"
              link
              type="success"
              @click="$emit('set-default', row)"
            >
              设为默认
            </el-button>
            <el-button
              v-if="canEdit && row.status === 1"
              link
              @click="$emit('toggle-status', { row, status: 0 })"
            >
              停用
            </el-button>
            <el-button
              v-if="canEdit && row.status === 0"
              link
              type="success"
              @click="$emit('toggle-status', { row, status: 1 })"
            >
              启用
            </el-button>
            <el-button v-if="canDelete" link type="danger" @click="$emit('delete-account', row)">
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import type { CompanyBankAccountRecord } from '@/api'

type CompanyOption = { companyId: string; label: string }

defineProps<{
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  companyOptions: CompanyOption[]
  companyAccountCompanyFilter?: string
  companyAccountStatusFilter?: number
  companyAccountDirectConnectFilter?: number
  filteredCompanyBankAccounts: CompanyBankAccountRecord[]
  resolveCompanyName: (companyId: string) => string
  maskAccountNo: (accountNo?: string) => string
  formatBooleanTag: (value: number) => string
  formatStatusLabel: (status: number) => string
}>()

defineEmits<{
  (e: 'update:companyAccountCompanyFilter', value?: string): void
  (e: 'update:companyAccountStatusFilter', value?: number): void
  (e: 'update:companyAccountDirectConnectFilter', value?: number): void
  (e: 'create'): void
  (e: 'edit', row: CompanyBankAccountRecord): void
  (e: 'set-default', row: CompanyBankAccountRecord): void
  (e: 'toggle-status', payload: { row: CompanyBankAccountRecord; status: number }): void
  (e: 'delete-account', row: CompanyBankAccountRecord): void
}>()
</script>
