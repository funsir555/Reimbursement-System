<template>
  <div class="space-y-5">
    <SystemSettingsOrganizationTab
      :can-create="permissions.canCreate"
      :can-delete="permissions.canDelete"
      :can-sync-config="permissions.canSyncConfig"
      :can-run-sync="permissions.canRunSync"
      :departments="state.departments"
      :department-expanded-keys="state.departmentExpandedKeys"
      :selected-department-id="state.selectedDepartmentId"
      :selected-department="state.selectedDepartment"
      :selected-department-sync-locked="state.selectedDepartmentSyncLocked"
      :department-config-form="state.departmentConfigForm"
      :department-parent-options="state.departmentParentOptions"
      :company-options="state.companyOptions"
      :employee-options="state.employeeOptions"
      :department-core-fields-readonly="state.departmentCoreFieldsReadonly"
      :department-stat-editable="state.departmentStatEditable"
      :connectors="state.connectors"
      :jobs="state.jobs"
      :source-label-map="state.sourceLabelMap"
      :is-wecom-connector="state.isWecomConnector"
      :resolve-connector-platform-name="state.resolveConnectorPlatformName"
      @create="actions.openDepartmentDialog()"
      @delete-selected="actions.handleDeleteDepartment()"
      @select-department="actions.handleDepartmentSelect($event)"
      @save-config="actions.saveDepartmentConfig()"
      @save-connector="actions.saveConnector($event)"
      @run-connector="actions.runConnectorSync($event)"
    />

    <el-dialog
      :model-value="state.departmentDialogVisible"
      title="新增部门"
      width="520px"
      :close-on-press-escape="false"
      @update:model-value="$emit('update:departmentDialogVisible', $event)"
    >
      <el-form label-width="92px">
        <div class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
          部门编码将在保存后自动生成
        </div>
        <el-form-item label="部门名称">
          <el-input v-model="state.departmentForm.deptName" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="state.departmentForm.parentId"
            :data="state.departmentOptions"
            node-key="id"
            check-strictly
            clearable
            :props="{ label: 'deptName', children: 'children', value: 'id' }"
          />
        </el-form-item>
        <el-form-item label="所属公司">
          <el-select v-model="state.departmentForm.companyId" clearable class="w-full">
            <el-option
              v-for="item in state.companyOptions"
              :key="item.companyId"
              :label="item.label"
              :value="item.companyId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="部门负责人">
          <el-select v-model="state.departmentForm.leaderUserId" clearable filterable class="w-full">
            <el-option
              v-for="item in state.employeeOptions"
              :key="item.userId"
              :label="item.label"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="统计部门归属">
          <el-input v-model="state.departmentForm.statDepartmentBelong" />
        </el-form-item>
        <el-form-item label="统计大区归属">
          <el-input v-model="state.departmentForm.statRegionBelong" />
        </el-form-item>
        <el-form-item label="统计区域归属">
          <el-input v-model="state.departmentForm.statAreaBelong" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="$emit('update:departmentDialogVisible', false)">取消</el-button>
        <el-button type="primary" plain @click="actions.saveDepartment(true)">保存并退出</el-button>
        <el-button type="primary" @click="actions.saveDepartment(false)">保存并新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { DepartmentTreeNode, SyncConnectorConfig, SyncJobRecord } from '@/api'
import type {
  CompanyOption,
  DepartmentConfigFormState,
  DepartmentFormState,
  EmployeeOption
} from '../systemSettingsShared'
import SystemSettingsOrganizationTab from './SystemSettingsOrganizationTab.vue'

defineProps<{
  permissions: {
    canCreate: boolean
    canDelete: boolean
    canSyncConfig: boolean
    canRunSync: boolean
  }
  state: {
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
    departmentDialogVisible: boolean
    departmentForm: DepartmentFormState
    departmentOptions: DepartmentTreeNode[]
  }
  actions: {
    openDepartmentDialog: () => void
    handleDeleteDepartment: () => Promise<void>
    handleDepartmentSelect: (node: DepartmentTreeNode) => void
    saveDepartmentConfig: () => Promise<void>
    saveConnector: (connector: SyncConnectorConfig) => Promise<void>
    runConnectorSync: (platformCode: string) => Promise<void>
    saveDepartment: (closeAfterSave: boolean) => Promise<void>
  }
}>()

defineEmits<{
  (e: 'update:departmentDialogVisible', value: boolean): void
}>()
</script>
