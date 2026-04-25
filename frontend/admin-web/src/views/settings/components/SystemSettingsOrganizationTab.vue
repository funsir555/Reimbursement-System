<template>
  <div class="grid gap-6 xl:grid-cols-[1.2fr,1fr]">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <div>
            <div class="text-base font-semibold text-slate-900">部门树</div>
            <div class="text-xs text-slate-500">手工新增部门不会被自动同步覆盖</div>
          </div>
          <div class="flex gap-2">
            <el-button v-if="canCreate" type="primary" @click="$emit('create')">
              新增部门
            </el-button>
            <el-button
              v-if="canDelete"
              :disabled="!selectedDepartment"
              type="danger"
              plain
              @click="$emit('delete-selected')"
            >
              删除
            </el-button>
          </div>
        </div>
      </template>

      <el-tree
        :data="departments"
        node-key="id"
        highlight-current
        :default-expanded-keys="departmentExpandedKeys"
        :current-node-key="selectedDepartmentId"
        @node-click="$emit('select-department', $event)"
      >
        <template #default="{ data }">
          <div class="flex w-full items-center justify-between gap-3 py-1">
            <div class="flex items-center gap-2">
              <span class="font-medium text-slate-800">{{ data.deptName }}</span>
              <el-tag size="small" :type="data.syncManaged ? 'warning' : 'success'">
                {{ sourceLabelMap[data.syncSource] || data.syncSource }}
              </el-tag>
            </div>
            <span class="text-xs text-slate-400">{{ data.deptCode }}</span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <div class="space-y-6">
      <el-card shadow="never">
        <template #header>
          <div class="font-semibold text-slate-900">部门配置</div>
        </template>
        <el-empty v-if="!selectedDepartment" description="请选择左侧部门节点" />
        <div v-else class="space-y-4">
          <el-alert
            v-if="selectedDepartmentSyncLocked"
            type="warning"
            :closable="false"
            title="该部门由同步接管，核心资料不可手工修改，但可维护统计口径字段"
          />
          <el-form label-width="92px">
            <el-form-item label="部门名称">
              <el-input
                v-model="departmentConfigForm.deptName"
                :disabled="departmentCoreFieldsReadonly"
              />
            </el-form-item>
            <el-form-item label="上级部门">
              <el-tree-select
                v-model="departmentConfigForm.parentId"
                :data="departmentParentOptions"
                node-key="id"
                check-strictly
                clearable
                :disabled="departmentCoreFieldsReadonly"
                :props="{ label: 'deptName', children: 'children', value: 'id' }"
              />
            </el-form-item>
            <el-form-item label="所属公司">
              <el-select
                v-model="departmentConfigForm.companyId"
                clearable
                class="w-full"
                :disabled="departmentCoreFieldsReadonly"
              >
                <el-option
                  v-for="item in companyOptions"
                  :key="item.companyId"
                  :label="item.label"
                  :value="item.companyId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="部门负责人">
              <el-select
                v-model="departmentConfigForm.leaderUserId"
                clearable
                filterable
                class="w-full"
                :disabled="departmentCoreFieldsReadonly"
              >
                <el-option
                  v-for="item in employeeOptions"
                  :key="item.userId"
                  :label="item.label"
                  :value="item.userId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="部门编码">
              <el-input :model-value="selectedDepartment.deptCode" disabled />
            </el-form-item>
            <el-form-item label="同步来源">
              <el-input
                :model-value="sourceLabelMap[selectedDepartment.syncSource] || selectedDepartment.syncSource"
                disabled
              />
            </el-form-item>
            <el-form-item label="统计部门归属">
              <el-input
                v-model="departmentConfigForm.statDepartmentBelong"
                data-testid="department-stat-department-input"
                :disabled="!departmentStatEditable"
              />
            </el-form-item>
            <el-form-item label="统计大区归属">
              <el-input
                v-model="departmentConfigForm.statRegionBelong"
                :disabled="!departmentStatEditable"
              />
            </el-form-item>
            <el-form-item label="统计区域归属">
              <el-input
                v-model="departmentConfigForm.statAreaBelong"
                :disabled="!departmentStatEditable"
              />
            </el-form-item>
          </el-form>
          <div v-if="departmentStatEditable" class="flex justify-end">
            <el-button type="primary" @click="$emit('save-config')">保存配置</el-button>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="font-semibold text-slate-900">同步连接配置</div>
        </template>
        <SystemSettingsSyncConnectorList
          :connectors="connectors"
          :can-sync-config="canSyncConfig"
          :can-run-sync="canRunSync"
          :is-wecom-connector="isWecomConnector"
          :resolve-connector-platform-name="resolveConnectorPlatformName"
          title-test-id="sync-connector-title"
          save-test-id="sync-connector-save"
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
import type { DepartmentTreeNode, SyncConnectorConfig, SyncJobRecord } from '@/api'
import type { CompanyOption, DepartmentConfigFormState, EmployeeOption } from '../systemSettingsShared'
import SystemSettingsSyncConnectorList from './SystemSettingsSyncConnectorList.vue'
import SystemSettingsSyncJobTable from './SystemSettingsSyncJobTable.vue'

defineProps<{
  canCreate: boolean
  canDelete: boolean
  canSyncConfig: boolean
  canRunSync: boolean
  departments: DepartmentTreeNode[]
  departmentExpandedKeys: number[]
  selectedDepartmentId?: number
  selectedDepartment?: DepartmentTreeNode
  selectedDepartmentSyncLocked: boolean
  departmentConfigForm: DepartmentConfigFormState
  departmentParentOptions: DepartmentTreeNode[]
  companyOptions: CompanyOption[]
  employeeOptions: EmployeeOption[]
  departmentCoreFieldsReadonly: boolean
  departmentStatEditable: boolean
  connectors: SyncConnectorConfig[]
  jobs: SyncJobRecord[]
  sourceLabelMap: Record<string, string>
  isWecomConnector: (connector: SyncConnectorConfig) => boolean
  resolveConnectorPlatformName: (connector: SyncConnectorConfig) => string
}>()

defineEmits<{
  (e: 'create'): void
  (e: 'delete-selected'): void
  (e: 'select-department', node: DepartmentTreeNode): void
  (e: 'save-config'): void
  (e: 'save-connector', connector: SyncConnectorConfig): void
  (e: 'run-connector', platformCode: string): void
}>()
</script>
