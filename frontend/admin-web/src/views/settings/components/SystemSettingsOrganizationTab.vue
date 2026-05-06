<template>
  <div class="grid gap-6 xl:grid-cols-[1.2fr,1fr]">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <div>
            <div class="text-base font-semibold text-slate-900">部门树</div>
            <div class="text-xs text-slate-500">展开部门后可同时查看下级部门与员工，部门节点优先展示</div>
          </div>
          <div class="flex gap-2">
            <el-button v-if="canCreate" type="primary" @click="$emit('create')">
              新增部门
            </el-button>
            <el-button
              v-if="canDelete"
              data-testid="organization-delete-button"
              :disabled="!selectedOrganizationNodeIsDepartment || !selectedDepartment"
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
        :data="organizationTreeNodes"
        node-key="nodeKey"
        highlight-current
        :default-expanded-keys="departmentExpandedKeys"
        :current-node-key="selectedOrganizationNodeKey"
        :expand-on-click-node="true"
        @node-click="$emit('select-node', $event)"
        @node-expand="$emit('node-expand', $event)"
        @node-collapse="$emit('node-collapse', $event)"
      >
        <template #default="{ data }">
          <div
            :data-testid="`organization-node-${data.nodeKey}`"
            class="flex w-full items-center justify-between gap-3 py-1"
            @dblclick.stop="handleNodeDoubleClick(data)"
          >
            <template v-if="isOrganizationDepartmentNode(data)">
              <div class="flex items-center gap-2">
                <span class="font-medium text-slate-800">{{ data.department.deptName }}</span>
                <el-tag size="small" :type="data.department.syncManaged ? 'warning' : 'success'">
                  {{ sourceLabelMap[data.department.syncSource] || data.department.syncSource }}
                </el-tag>
              </div>
              <span class="text-xs text-slate-400">{{ data.department.deptCode }}</span>
            </template>
            <template v-else>
              <div class="flex items-center gap-2">
                <span class="font-medium text-slate-800">{{ data.employee.name }}</span>
                <span class="text-xs text-slate-500">({{ data.employee.username }})</span>
                <el-tag size="small" :type="data.employee.syncManaged ? 'warning' : 'success'">
                  {{ sourceLabelMap[data.employee.sourceType] || data.employee.sourceType }}
                </el-tag>
                <el-tag v-if="data.employee.status === 0" size="small" type="info">停用</el-tag>
              </div>
              <span class="text-xs text-slate-400">员工</span>
            </template>
          </div>
        </template>
      </el-tree>
    </el-card>

    <div class="space-y-6">
      <el-card shadow="never">
        <template #header>
          <div class="font-semibold text-slate-900">
            {{ selectedOrganizationNodeIsEmployee ? '员工信息' : '部门配置' }}
          </div>
        </template>

        <el-empty
          v-if="!selectedOrganizationNodeKey"
          description="请选择左侧部门或员工节点"
        />

        <div v-else-if="selectedOrganizationNodeIsEmployee && selectedOrganizationEmployee" class="space-y-4">
          <el-alert
            v-if="selectedOrganizationEmployeeSyncLocked"
            type="warning"
            :closable="false"
            title="该员工由同步接管，核心资料仅允许通过员工编辑弹窗按既有规则维护"
          />
          <div data-testid="organization-employee-info" class="space-y-3">
            <div class="rounded-2xl bg-slate-50 px-4 py-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <div class="text-lg font-semibold text-slate-900">
                    {{ selectedOrganizationEmployee.name }}
                  </div>
                  <div class="mt-1 text-sm text-slate-500">
                    {{ selectedOrganizationEmployee.username }}
                  </div>
                </div>
                <el-button
                  data-testid="organization-employee-edit-button"
                  type="primary"
                  :disabled="!canEditEmployee"
                  @click="$emit('edit-employee', selectedOrganizationEmployee)"
                >
                  编辑员工
                </el-button>
              </div>
            </div>

            <div class="grid gap-3 md:grid-cols-2">
              <div class="rounded-2xl border border-slate-200 px-4 py-3">
                <div class="text-xs text-slate-400">所属部门</div>
                <div class="mt-1 text-sm text-slate-700">
                  {{ formatEmployeeDepartmentNames(selectedOrganizationEmployee) || '未设置' }}
                </div>
              </div>
              <div class="rounded-2xl border border-slate-200 px-4 py-3">
                <div class="text-xs text-slate-400">所属公司</div>
                <div class="mt-1 text-sm text-slate-700">{{ selectedOrganizationEmployee.companyName || '未设置' }}</div>
              </div>
              <div class="rounded-2xl border border-slate-200 px-4 py-3">
                <div class="text-xs text-slate-400">岗位</div>
                <div class="mt-1 text-sm text-slate-700">{{ selectedOrganizationEmployee.position || '未设置' }}</div>
              </div>
              <div class="rounded-2xl border border-slate-200 px-4 py-3">
                <div class="text-xs text-slate-400">来源</div>
                <div class="mt-1 text-sm text-slate-700">
                  {{ sourceLabelMap[selectedOrganizationEmployee.sourceType] || selectedOrganizationEmployee.sourceType }}
                </div>
              </div>
              <div class="rounded-2xl border border-slate-200 px-4 py-3">
                <div class="text-xs text-slate-400">统计部门归属</div>
                <div class="mt-1 text-sm text-slate-700">{{ selectedOrganizationEmployee.statDepartmentBelong || '未设置' }}</div>
              </div>
              <div class="rounded-2xl border border-slate-200 px-4 py-3">
                <div class="text-xs text-slate-400">统计大区归属</div>
                <div class="mt-1 text-sm text-slate-700">{{ selectedOrganizationEmployee.statRegionBelong || '未设置' }}</div>
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200 px-4 py-3">
              <div class="text-xs text-slate-400">角色</div>
              <div class="mt-2 flex flex-wrap gap-2">
                <el-tag
                  v-for="roleName in resolveEmployeeRoleNames(selectedOrganizationEmployee.roleCodes)"
                  :key="`${selectedOrganizationEmployee.userId}-${roleName}`"
                  size="small"
                >
                  {{ roleName }}
                </el-tag>
                <span
                  v-if="!resolveEmployeeRoleNames(selectedOrganizationEmployee.roleCodes).length"
                  class="text-sm text-slate-500"
                >
                  暂未分配角色
                </span>
              </div>
            </div>
          </div>
        </div>

        <div
          v-else-if="selectedDepartment"
          data-testid="organization-department-config"
          class="space-y-4"
        >
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
              <employee-tree-select
                v-model="departmentConfigForm.leaderUserId"
                :departments="departmentOptions"
                :employees="employees"
                value-type="number"
                class="w-full"
                :disabled="departmentCoreFieldsReadonly"
              />
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
import type { DepartmentTreeNode, EmployeeRecord, SyncConnectorConfig, SyncJobRecord } from '@/api'
import EmployeeTreeSelect from '@/components/inputs/EmployeeTreeSelect.vue'
import {
  formatEmployeeDepartmentNames,
  type CompanyOption,
  type DepartmentConfigFormState,
  type EmployeeOption
} from '../systemSettingsShared'
import {
  isOrganizationDepartmentNode,
  type OrganizationTreeNode
} from '../systemSettingsOrganizationTree'
import SystemSettingsSyncConnectorList from './SystemSettingsSyncConnectorList.vue'
import SystemSettingsSyncJobTable from './SystemSettingsSyncJobTable.vue'


const props = defineProps<{
  canCreate: boolean
  canDelete: boolean
  canSyncConfig: boolean
  canRunSync: boolean
  canEditEmployee: boolean
  organizationTreeNodes: OrganizationTreeNode[]
  departmentExpandedKeys: string[]
  selectedOrganizationNodeKey?: string
  selectedOrganizationEmployee?: EmployeeRecord
  selectedOrganizationEmployeeSyncLocked: boolean
  selectedOrganizationNodeIsDepartment: boolean
  selectedOrganizationNodeIsEmployee: boolean
  selectedDepartment?: DepartmentTreeNode
  selectedDepartmentSyncLocked: boolean
  departmentConfigForm: DepartmentConfigFormState
  departmentOptions: DepartmentTreeNode[]
  departmentParentOptions: DepartmentTreeNode[]
  companyOptions: CompanyOption[]
  employees: EmployeeRecord[]
  employeeOptions: EmployeeOption[]
  departmentCoreFieldsReadonly: boolean
  departmentStatEditable: boolean
  connectors: SyncConnectorConfig[]
  jobs: SyncJobRecord[]
  sourceLabelMap: Record<string, string>
  isWecomConnector: (connector: SyncConnectorConfig) => boolean
  resolveConnectorPlatformName: (connector: SyncConnectorConfig) => string
  resolveEmployeeRoleNames: (roleCodes?: string[]) => string[]
}>()

const emit = defineEmits<{
  (e: 'create'): void
  (e: 'delete-selected'): void
  (e: 'select-node', node: OrganizationTreeNode): void
  (e: 'node-expand', node: OrganizationTreeNode): void
  (e: 'node-collapse', node: OrganizationTreeNode): void
  (e: 'edit-employee', employee?: EmployeeRecord): void
  (e: 'save-config'): void
  (e: 'save-connector', connector: SyncConnectorConfig): void
  (e: 'run-connector', platformCode: string): void
}>()

function handleNodeDoubleClick(node: OrganizationTreeNode) {
  if (!props.canEditEmployee || isOrganizationDepartmentNode(node)) {
    return
  }
  emit('edit-employee', node.employee)
}
</script>
