<template>
  <div class="space-y-5">
    <SystemSettingsEmployeesTab
      :can-create="permissions.canCreate"
      :can-edit="permissions.canEdit"
      :can-delete="permissions.canDelete"
      :can-sync-config="permissions.canSyncConfig"
      :can-run-sync="permissions.canRunSync"
      :employee-keyword="state.employeeKeyword"
      :employee-status-filter="state.employeeStatusFilter"
      :filtered-employees="state.filteredEmployees"
      :selected-employee="state.selectedEmployee"
      :selected-employee-sync-locked="state.selectedEmployeeSyncLocked"
      :source-label-map="state.sourceLabelMap"
      :connectors="state.connectors"
      :jobs="state.jobs"
      :is-wecom-connector="state.isWecomConnector"
      :resolve-connector-platform-name="state.resolveConnectorPlatformName"
      :resolve-employee-role-names="state.resolveEmployeeRoleNames"
      @update:employee-keyword="$emit('update:employeeKeyword', $event)"
      @update:employee-status-filter="$emit('update:employeeStatusFilter', $event)"
      @select-employee="$emit('update:selectedEmployee', $event)"
      @create="actions.openEmployeeDialog()"
      @edit="actions.openEmployeeDialog($event)"
      @delete-selected="actions.handleDeleteEmployee()"
      @save-connector="actions.saveConnector($event)"
      @run-connector="actions.runConnectorSync($event)"
    />
  </div>
</template>

<script setup lang="ts">
import type { EmployeeRecord, SyncConnectorConfig, SyncJobRecord } from '@/api'
import SystemSettingsEmployeesTab from './SystemSettingsEmployeesTab.vue'

defineProps<{
  permissions: {
    canCreate: boolean
    canEdit: boolean
    canDelete: boolean
    canSyncConfig: boolean
    canRunSync: boolean
  }
  state: {
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
  }
  actions: {
    openEmployeeDialog: (item?: EmployeeRecord) => void
    handleDeleteEmployee: () => Promise<void>
    saveConnector: (connector: SyncConnectorConfig) => Promise<void>
    runConnectorSync: (platformCode: string) => Promise<void>
  }
}>()

defineEmits<{
  (e: 'update:employeeKeyword', value: string): void
  (e: 'update:employeeStatusFilter', value?: number): void
  (e: 'update:selectedEmployee', value?: EmployeeRecord): void
}>()
</script>
