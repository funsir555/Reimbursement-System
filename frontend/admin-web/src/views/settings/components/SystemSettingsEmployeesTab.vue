<template>
  <div class="grid gap-6 xl:grid-cols-[1.2fr,1fr]">
    <el-card shadow="never">
      <template #header>
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div class="flex flex-wrap gap-3">
            <el-input
              :model-value="employeeKeyword"
              placeholder="搜索姓名 / 用户名 / 手机号 / 邮箱"
              clearable
              class="w-72"
              @update:model-value="$emit('update:employeeKeyword', $event || '')"
            />
            <el-select
              :model-value="employeeStatusFilter"
              clearable
              placeholder="状态"
              class="w-32"
              @update:model-value="$emit('update:employeeStatusFilter', $event)"
            >
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </div>
          <div class="flex gap-2">
            <el-button v-if="canCreate" type="primary" @click="$emit('create')">
              新增员工
            </el-button>
            <el-button v-if="canEdit" :disabled="!selectedEmployee" @click="$emit('edit', selectedEmployee)">
              编辑
            </el-button>
            <el-button
              v-if="canDelete"
              :disabled="!selectedEmployee || selectedEmployeeSyncLocked"
              type="danger"
              plain
              @click="$emit('delete-selected')"
            >
              删除
            </el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="filteredEmployees"
        highlight-current-row
        @current-change="$emit('select-employee', $event)"
        @row-dblclick="$emit('edit', $event)"
      >
        <el-table-column prop="name" label="姓名" min-width="140" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="部门" min-width="180">
          <template #default="{ row }">
            {{ formatEmployeeDepartmentNames(row) || '未设置' }}
          </template>
        </el-table-column>
        <el-table-column prop="companyName" label="公司" min-width="150" />
        <el-table-column prop="statDepartmentBelong" label="统计部门归属" min-width="150" />
        <el-table-column prop="statRegionBelong" label="统计大区归属" min-width="150" />
        <el-table-column prop="statAreaBelong" label="统计区域归属" min-width="150" />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            {{ sourceLabelMap[row.sourceType] || row.sourceType }}
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-tag
              v-for="roleName in resolveEmployeeRoleNames(row.roleCodes)"
              :key="`${row.userId}-${roleName}`"
              size="small"
              class="mr-1 mb-1"
            >
              {{ roleName }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="space-y-6">
      <el-card shadow="never">
        <template #header>
          <div class="font-semibold text-slate-900">员工同步配置</div>
        </template>
        <SystemSettingsSyncConnectorList
          :connectors="connectors"
          :can-sync-config="canSyncConfig"
          :can-run-sync="canRunSync"
          :is-wecom-connector="isWecomConnector"
          :resolve-connector-platform-name="resolveConnectorPlatformName"
          title-test-id="employee-sync-connector-title"
          save-test-id="employee-sync-connector-save"
          run-test-id="employee-sync-connector-run"
          item-key-prefix="employee"
          @save="$emit('save-connector', $event)"
          @run="$emit('run-connector', $event)"
        />
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="font-semibold text-slate-900">同步日志</div>
        </template>
        <SystemSettingsSyncJobTable :jobs="jobs" :source-label-map="sourceLabelMap" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { EmployeeRecord, SyncConnectorConfig, SyncJobRecord } from '@/api'
import { formatEmployeeDepartmentNames } from '../systemSettingsShared'
import SystemSettingsSyncConnectorList from './SystemSettingsSyncConnectorList.vue'
import SystemSettingsSyncJobTable from './SystemSettingsSyncJobTable.vue'

defineProps<{
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  canSyncConfig: boolean
  canRunSync: boolean
  employeeKeyword: string
  employeeStatusFilter?: number
  filteredEmployees: EmployeeRecord[]
  selectedEmployee?: EmployeeRecord
  selectedEmployeeSyncLocked: boolean
  sourceLabelMap: Record<string, string>
  connectors: SyncConnectorConfig[]
  jobs: SyncJobRecord[]
  isWecomConnector: (connector: SyncConnectorConfig) => boolean
  resolveConnectorPlatformName: (connector: SyncConnectorConfig) => string
  resolveEmployeeRoleNames: (roleCodes?: string[]) => string[]
}>()

defineEmits<{
  (e: 'update:employeeKeyword', value: string): void
  (e: 'update:employeeStatusFilter', value?: number): void
  (e: 'select-employee', employee?: EmployeeRecord): void
  (e: 'create'): void
  (e: 'edit', employee?: EmployeeRecord): void
  (e: 'delete-selected'): void
  (e: 'save-connector', connector: SyncConnectorConfig): void
  (e: 'run-connector', platformCode: string): void
}>()
</script>
