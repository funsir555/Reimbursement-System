
<template>
  <div class="space-y-5">
    <SystemSettingsOverviewHero
      :department-count="departmentCount"
      :employees-count="employees.length"
      :roles-count="roles.length"
      :company-count="companyCount"
    />

    <el-card v-loading="loading" shadow="never" class="rounded-3xl border-0">
      <el-empty
        v-if="!loading && bootstrapError"
        description="系统设置初始化失败，请联系管理员或查看后端日志"
      />
      <el-empty
        v-else-if="!loading && !visibleTabs.length"
        description="当前账号未被授予系统设置访问权限"
      />

      <el-tabs v-else v-model="activeTab" class="settings-tabs">
        <el-tab-pane
          v-if="can('settings:organization:view')"
          label="组织架构"
          name="organization"
        >
          <SystemSettingsOrganizationSection
            :permissions="{
              canCreate: can('settings:organization:create'),
              canDelete: can('settings:organization:delete'),
              canSyncConfig: can('settings:organization:sync_config'),
              canRunSync: can('settings:organization:run_sync'),
              canEditEmployee: can('settings:employees:edit')
            }"
            :state="{
              organizationTreeNodes,
              departmentExpandedKeys,
              selectedOrganizationNodeKey,
              selectedOrganizationEmployee,
              selectedOrganizationEmployeeSyncLocked,
              selectedOrganizationNodeIsDepartment,
              selectedOrganizationNodeIsEmployee,
              selectedDepartment,
              selectedDepartmentSyncLocked,
              departmentConfigForm,
              departmentParentOptions,
              companyOptions,
              employees,
              employeeOptions,
              departmentCoreFieldsReadonly,
              departmentStatEditable,
              resolveEmployeeRoleNames,
              connectors,
              jobs,
              sourceLabelMap,
              isWecomConnector,
              resolveConnectorPlatformName,
              departmentDialogVisible,
              departmentForm,
              departmentOptions
            }"
            :actions="{
              openDepartmentDialog,
              openEmployeeDialog,
              handleDeleteDepartment,
              handleOrganizationNodeSelect,
              handleDepartmentNodeExpand,
              handleDepartmentNodeCollapse,
              saveDepartmentConfig,
              saveConnector,
              runConnectorSync,
              saveDepartment
            }"
            @update:department-dialog-visible="departmentDialogVisible = $event"
          />
        </el-tab-pane>

        <el-tab-pane
          v-if="can('settings:employees:view')"
          label="员工管理"
          name="employees"
        >
          <SystemSettingsEmployeesSection
            :permissions="{
              canCreate: can('settings:employees:create'),
              canEdit: can('settings:employees:edit'),
              canDelete: can('settings:employees:delete'),
              canSyncConfig: can('settings:organization:sync_config'),
              canRunSync: can('settings:organization:run_sync')
            }"
            :state="{
              employeeKeyword,
              employeeStatusFilter,
              filteredEmployees,
              selectedEmployee,
              selectedEmployeeSyncLocked,
              sourceLabelMap,
              connectors,
              jobs,
              isWecomConnector,
              resolveConnectorPlatformName,
              resolveEmployeeRoleNames
            }"
            :actions="{
              openEmployeeDialog,
              handleDeleteEmployee,
              saveConnector,
              runConnectorSync
            }"
            @update:employee-keyword="employeeKeyword = $event"
            @update:employee-status-filter="employeeStatusFilter = $event"
            @update:selected-employee="selectedEmployee = $event"
          />
        </el-tab-pane>

        <el-tab-pane
          v-if="can('settings:roles:view')"
          label="权限管理"
          name="roles"
        >
          <SystemSettingsRolesSection
            :permissions="{
              canCreate: can('settings:roles:create'),
              canEdit: can('settings:roles:edit'),
              canDelete: can('settings:roles:delete'),
              canAssignPermissions: can('settings:roles:assign_permissions'),
              canAssignUsers: can('settings:roles:assign_users')
            }"
            :state="{
              roles,
              selectedRole,
              selectedRoleProtected,
              selectedRoleReadonly,
              rolePermissionReadonly,
              roleUserAssignmentReadonly,
              permissions,
              permissionTreeRef,
              selectedRoleUserIds,
              employees,
              isSuperAdminRole,
              roleDialogVisible,
              roleForm
            }"
            :actions="{
              handleRoleSelect,
              openRoleDialog,
              handleDeleteRole,
              saveRolePermissions,
              saveRoleUsers,
              saveRole
            }"
            @update:selected-role-user-ids="selectedRoleUserIds = $event"
            @update:role-dialog-visible="roleDialogVisible = $event"
          />
        </el-tab-pane>

        <el-tab-pane v-if="can('settings:companies:view')" label="公司管理" name="companies">
          <SystemSettingsCompaniesSection
            :permissions="{
              canCreate: can('settings:companies:create'),
              canEdit: can('settings:companies:edit'),
              canDelete: can('settings:companies:delete')
            }"
            :state="{
              flatCompanies,
              selectedCompany,
              formatStatusLabel,
              companyDialogVisible,
              companyForm
            }"
            :actions="{
              openCompanyDialog,
              handleDeleteCompany,
              saveCompany
            }"
            @update:selected-company="selectedCompany = $event"
            @update:company-dialog-visible="companyDialogVisible = $event"
          />
        </el-tab-pane>
        <el-tab-pane
          v-if="can('settings:company_accounts:view')"
          label="公司账户管理"
          name="companyAccounts"
        >
          <SystemSettingsCompanyAccountsSection
            :permissions="{
              canCreate: can('settings:company_accounts:create'),
              canEdit: can('settings:company_accounts:edit'),
              canDelete: can('settings:company_accounts:delete')
            }"
            :state="{
              companyOptions,
              companyAccountCompanyFilter,
              companyAccountStatusFilter,
              companyAccountDirectConnectFilter,
              filteredCompanyBankAccounts,
              resolveCompanyName,
              maskAccountNo,
              formatBooleanTag,
              formatStatusLabel,
              companyBankAccountDialogVisible,
              editingCompanyBankAccount,
              companyBankAccountForm
            }"
            :actions="{
              openCompanyBankAccountDialog,
              setCompanyBankAccountDefault,
              toggleCompanyBankAccountStatus,
              handleDeleteCompanyBankAccount,
              resetCompanyBankAccountForm,
              saveCompanyBankAccount
            }"
            @update:company-account-company-filter="companyAccountCompanyFilter = $event"
            @update:company-account-status-filter="companyAccountStatusFilter = $event"
            @update:company-account-direct-connect-filter="companyAccountDirectConnectFilter = $event"
            @update:company-bank-account-dialog-visible="companyBankAccountDialogVisible = $event"
          />
        </el-tab-pane>

        <el-tab-pane
          v-if="can('settings:api_interfaces:view')"
          label="API接口"
          name="apiInterfaces"
        >
          <SystemSettingsApiInterfacesSection
            :permissions="{
              canOcrEdit: can('settings:api_interfaces:ocr_edit'),
              canOcrTest: can('settings:api_interfaces:ocr_test')
            }"
            :state="{
              activeApiInterface,
              apiInterfaceOptions,
              activeApiInterfaceOption,
              activeOcrProviderCode,
              ocrVendorOptions,
              activeOcrProvider,
              activeOcrProviderLabel,
              ocrRamPolicySnippet,
              activeOcrSecretHint,
              ocrForm,
              ocrSaveLoading,
              ocrTestLoading
            }"
            :actions="{
              saveOcrProvider,
              testOcrProviderConfig
            }"
            @update:active-api-interface="activeApiInterface = $event"
            @update:active-ocr-provider-code="activeOcrProviderCode = $event"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <SystemSettingsEmployeeDialog
      :model-value="employeeDialogVisible"
      :state="{
        employeeForm,
        employeeSyncLocked,
        employeeCoreFieldsReadonly,
        canAssignEmployeeRoles,
        employeeRoleOptions,
        employeeHasSuperAdminRole,
        departmentOptions,
        companyOptions,
        isSuperAdminRole
      }"
      :actions="{
        saveEmployee
      }"
      @update:model-value="employeeDialogVisible = $event"
    />
  </div>
</template>

<script setup lang="ts">
import SystemSettingsOverviewHero from './components/SystemSettingsOverviewHero.vue'
import SystemSettingsEmployeeDialog from './components/SystemSettingsEmployeeDialog.vue'
import SystemSettingsOrganizationSection from './components/SystemSettingsOrganizationSection.vue'
import SystemSettingsEmployeesSection from './components/SystemSettingsEmployeesSection.vue'
import SystemSettingsRolesSection from './components/SystemSettingsRolesSection.vue'
import SystemSettingsCompaniesSection from './components/SystemSettingsCompaniesSection.vue'
import SystemSettingsCompanyAccountsSection from './components/SystemSettingsCompanyAccountsSection.vue'
import SystemSettingsApiInterfacesSection from './components/SystemSettingsApiInterfacesSection.vue'
import { useSystemSettingsPageOrchestration } from './composables/useSystemSettingsPageOrchestration'
import { useSystemSettingsBootstrapSync } from './composables/useSystemSettingsBootstrapSync'
import { useSystemSettingsOrganization } from './composables/useSystemSettingsOrganization'
import { useSystemSettingsEmployees } from './composables/useSystemSettingsEmployees'
import { useSystemSettingsApiInterfaces } from './composables/useSystemSettingsApiInterfaces'
import { useSystemSettingsRoles } from './composables/useSystemSettingsRoles'
import { useSystemSettingsCompanies } from './composables/useSystemSettingsCompanies'
import { useSystemSettingsCompanyAccounts } from './composables/useSystemSettingsCompanyAccounts'
import {
  apiInterfaceOptions,
  formatBooleanTag,
  formatStatusLabel,
  isSuperAdminRole,
  isWecomConnector,
  maskAccountNo,
  ocrRamPolicySnippet,
  ocrVendorOptions,
  resolveConnectorPlatformName,
  sourceLabelMap
} from './systemSettingsShared'

const {
  loading,
  bootstrapError,
  activeTab,
  departments,
  employees,
  roles,
  permissions,
  companies,
  companyBankAccounts,
  connectors,
  jobs,
  ocrProviders,
  currentUser,
  visibleTabs,
  can,
  registerBootstrapCoordinator,
  loadBootstrap,
  saveConnector,
  runConnectorSync
} = useSystemSettingsPageOrchestration()

const {
  selectedCompany,
  companyDialogVisible,
  companyForm,
  flatCompanies,
  companyCount,
  companyOptions,
  applyCompaniesBootstrap,
  openCompanyDialog,
  saveCompany,
  handleDeleteCompany
} = useSystemSettingsCompanies({
  companies,
  loadBootstrap
})

const {
  selectedOrganizationNodeKey,
  selectedOrganizationEmployee,
  selectedOrganizationEmployeeSyncLocked,
  selectedOrganizationNodeIsDepartment,
  selectedOrganizationNodeIsEmployee,
  organizationTreeNodes,
  departmentExpandedKeys,
  departmentDialogVisible,
  departmentForm,
  departmentConfigForm,
  selectedDepartment,
  departmentCount,
  departmentOptions,
  departmentParentOptions,
  employeeOptions,
  departmentCoreFieldsReadonly,
  selectedDepartmentSyncLocked,
  departmentStatEditable,
  applyDepartmentBootstrap,
  handleOrganizationNodeSelect,
  handleDepartmentNodeExpand,
  handleDepartmentNodeCollapse,
  openDepartmentDialog,
  saveDepartment,
  saveDepartmentConfig,
  handleDeleteDepartment,
  loadBootstrapKeepingDepartmentTreeState
} = useSystemSettingsOrganization({
  departments,
  employees,
  companyOptions,
  can,
  loadBootstrap
})

const {
  selectedEmployee,
  employeeSyncLocked,
  employeeKeyword,
  employeeStatusFilter,
  employeeDialogVisible,
  employeeForm,
  filteredEmployees,
  employeeRoleOptions,
  canAssignEmployeeRoles,
  selectedEmployeeSyncLocked,
  employeeCoreFieldsReadonly,
  employeeHasSuperAdminRole,
  applyEmployeesBootstrap,
  openEmployeeDialog,
  saveEmployee,
  handleDeleteEmployee,
  resolveEmployeeRoleNames
} = useSystemSettingsEmployees({
  employees,
  roles,
  can,
  loadBootstrap: loadBootstrapKeepingDepartmentTreeState
})

const {
  activeApiInterface,
  activeOcrProviderCode,
  ocrSaveLoading,
  ocrTestLoading,
  ocrForm,
  activeApiInterfaceOption,
  activeOcrProvider,
  activeOcrProviderLabel,
  activeOcrSecretHint,
  saveOcrProvider,
  testOcrProviderConfig
} = useSystemSettingsApiInterfaces({
  ocrProviders,
  loadBootstrap
})

const {
  selectedRole,
  selectedRoleUserIds,
  permissionTreeRef,
  roleDialogVisible,
  roleForm,
  selectedRoleProtected,
  selectedRoleReadonly,
  rolePermissionReadonly,
  roleUserAssignmentReadonly,
  applyRolesBootstrap,
  handleRoleSelect,
  openRoleDialog,
  saveRole,
  saveRolePermissions,
  saveRoleUsers,
  handleDeleteRole
} = useSystemSettingsRoles({
  roles,
  permissions,
  employees,
  currentUser,
  loadBootstrap
})

const {
  companyAccountCompanyFilter,
  companyAccountStatusFilter,
  companyAccountDirectConnectFilter,
  editingCompanyBankAccount,
  companyBankAccountDialogVisible,
  companyBankAccountForm,
  filteredCompanyBankAccounts,
  openCompanyBankAccountDialog,
  saveCompanyBankAccount,
  handleDeleteCompanyBankAccount,
  toggleCompanyBankAccountStatus,
  setCompanyBankAccountDefault,
  resolveCompanyName,
  validateCompanyBankAccountForm,
  resetCompanyBankAccountForm
} = useSystemSettingsCompanyAccounts({
  companyBankAccounts,
  companyOptions,
  loadBootstrap
})

useSystemSettingsBootstrapSync({
  registerBootstrapCoordinator,
  applyEmployeesBootstrap,
  applyRolesBootstrap,
  applyCompaniesBootstrap,
  applyDepartmentBootstrap
})
</script>

<style scoped>
:deep(.settings-tabs .el-tabs__header) {
  margin-bottom: 24px;
}
</style>
