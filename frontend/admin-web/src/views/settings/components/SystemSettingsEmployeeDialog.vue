<template>
  <el-dialog
    :model-value="modelValue"
    :title="state.employeeForm.userId ? '编辑员工' : '新增员工'"
    width="560px"
    :close-on-press-escape="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form label-width="92px">
      <el-alert
        v-if="state.employeeSyncLocked"
        type="warning"
        :closable="false"
        title="该员工由同步接管，仅允许维护统计口径字段与角色绑定"
        class="mb-4"
      />
      <el-form-item label="用户名">
        <el-input
          v-model="state.employeeForm.username"
          data-testid="employee-username-input"
          :disabled="state.employeeCoreFieldsReadonly"
        />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="state.employeeForm.name" :disabled="state.employeeCoreFieldsReadonly" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="state.employeeForm.phone" :disabled="state.employeeCoreFieldsReadonly" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="state.employeeForm.email" :disabled="state.employeeCoreFieldsReadonly" />
      </el-form-item>
      <el-form-item label="所属部门">
        <el-tree-select
          v-model="state.employeeForm.deptIds"
          :data="state.departmentOptions"
          node-key="id"
          multiple
          check-strictly
          clearable
          collapse-tags
          collapse-tags-tooltip
          :tag-tooltip="globalCollapseTagTooltipProps"
          :disabled="state.employeeCoreFieldsReadonly"
          :props="{ label: 'deptName', children: 'children', value: 'id' }"
        />
      </el-form-item>
      <el-form-item label="所属公司">
        <el-select
          v-model="state.employeeForm.companyId"
          clearable
          class="w-full"
          :disabled="state.employeeCoreFieldsReadonly"
        >
          <el-option
            v-for="item in state.companyOptions"
            :key="item.companyId"
            :label="item.label"
            :value="item.companyId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="岗位">
        <el-input v-model="state.employeeForm.position" :disabled="state.employeeCoreFieldsReadonly" />
      </el-form-item>
      <el-form-item label="劳动关系">
        <el-input
          v-model="state.employeeForm.laborRelationBelong"
          :disabled="state.employeeCoreFieldsReadonly"
        />
      </el-form-item>
      <el-form-item label="统计部门归属">
        <el-input
          v-model="state.employeeForm.statDepartmentBelong"
          data-testid="employee-stat-department-input"
        />
      </el-form-item>
      <el-form-item label="统计大区归属">
        <el-input v-model="state.employeeForm.statRegionBelong" />
      </el-form-item>
      <el-form-item label="统计区域归属">
        <el-input v-model="state.employeeForm.statAreaBelong" />
      </el-form-item>
      <el-form-item label="角色设置">
        <el-select
          v-model="state.employeeForm.roleIds"
          multiple
          filterable v-bind="globalFilterableSelectProps"
          clearable
          class="w-full"
          placeholder="选择角色"
          :disabled="!state.canAssignEmployeeRoles"
        >
          <el-option
            v-for="item in state.employeeRoleOptions"
            :key="item.id"
            :label="`${item.roleName} (${item.roleCode})`"
            :value="item.id"
            :disabled="item.status !== 1 || state.isSuperAdminRole(item)"
          />
        </el-select>
      </el-form-item>
      <div v-if="state.employeeHasSuperAdminRole" class="text-xs text-amber-600">
        超级管理员角色仅展示当前绑定状态，前端无法修改。
      </div>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <template v-if="state.employeeForm.userId">
        <el-button type="primary" @click="actions.saveEmployee(true)">保存</el-button>
      </template>
      <template v-else>
        <el-button type="primary" plain @click="actions.saveEmployee(true)">保存并退出</el-button>
        <el-button type="primary" @click="actions.saveEmployee(false)">保存并新增</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { DepartmentTreeNode, EmployeeEditorFormState, RoleRecord } from '@/api'
import { globalCollapseTagTooltipProps } from '@/utils/collapseTagTooltip'
import type { CompanyOption } from '../systemSettingsShared'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'


defineProps<{
  modelValue: boolean
  state: {
    employeeForm: EmployeeEditorFormState
    employeeSyncLocked: boolean
    employeeCoreFieldsReadonly: boolean
    canAssignEmployeeRoles: boolean
    employeeRoleOptions: RoleRecord[]
    employeeHasSuperAdminRole: boolean
    departmentOptions: DepartmentTreeNode[]
    companyOptions: CompanyOption[]
    isSuperAdminRole: (role?: Pick<RoleRecord, 'roleCode'>) => boolean
  }
  actions: {
    saveEmployee: (closeAfterSave: boolean) => Promise<void>
  }
}>()

defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()
</script>
