<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <div class="font-semibold text-slate-900">数据可见范围</div>
          <div class="mt-1 text-xs text-slate-500">逐项查询能力配置；不扩大使用者自身权限</div>
        </div>
        <el-button v-if="canAssign && !isSuperAdmin" type="primary" :disabled="!role || saving" :loading="saving" @click="save">
          保存范围
        </el-button>
      </div>
    </template>
    <el-empty v-if="!role" description="请选择左侧角色" />
    <div v-else v-loading="loading" class="space-y-4">
      <el-alert v-if="isSuperAdmin" title="超级管理员固定可查看当前租户的全部公司" type="info" :closable="false" />
      <el-empty v-if="!scopes.length" description="此角色尚无数据查询权限；请先保存菜单与按钮权限" />
      <div v-for="scope in scopes" :key="scope.permissionCode" class="rounded-xl border border-slate-200 p-3">
        <div class="font-medium text-slate-800">{{ scope.capabilityName }}</div>
        <div class="mb-3 text-xs text-slate-500">{{ scope.permissionCode }}</div>
        <el-alert v-if="scope.permissionCode === 'system:agent:people:query'"
          title="授予此独立权限即可按姓名查询当前租户各公司的人员；只返回规定的四项字段。" type="info" :closable="false" />
        <el-select v-else v-model="scope.scopeType" class="w-full" clearable placeholder="未配置（不可查询）" :disabled="!canAssign || isSuperAdmin">
          <el-option v-for="option in scope.supportedScopes" :key="option" :value="option" :label="scopeLabel[option] || option" />
        </el-select>
        <el-select v-if="scope.scopeType === 'SELECTED_COMPANIES'" v-model="scope.companyIds" class="mt-2 w-full"
          multiple filterable collapse-tags placeholder="选择授权公司" :disabled="!canAssign || isSuperAdmin">
          <el-option v-for="company in activeCompanies" :key="company.companyId" :value="company.companyId" :label="company.companyName" />
        </el-select>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { CompanyRecord, RoleRecord } from '@/api'
import { systemSettingsApi, type RoleQueryScope } from '@/api/modules/system-settings'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { isSuperAdminRole } from '../systemSettingsShared'

const props = defineProps<{ role?: RoleRecord; companies: CompanyRecord[]; canAssign: boolean }>()
const scopes = ref<RoleQueryScope[]>([])
const loading = ref(false)
const saving = ref(false)
const isSuperAdmin = computed(() => isSuperAdminRole(props.role))
const activeCompanies = computed(() => props.companies.filter(company => company.status === 1))
const scopeLabel: Record<string, string> = {
  SELF: '本人', DEPARTMENT_TREE: '本人所属部门及下级部门',
  SELECTED_COMPANIES: '指定公司', ALL_COMPANIES: '全部公司'
}

watch(() => [props.role?.id, props.role?.permissionCodes.join(',')], async () => {
  if (!props.role) { scopes.value = []; return }
  loading.value = true
  try { scopes.value = (await systemSettingsApi.getRoleDataScopes(props.role.id)).data }
  catch (error) { scopes.value = []; ElMessage.error(error instanceof Error ? error.message : '数据范围读取失败') }
  finally { loading.value = false }
}, { immediate: true })

async function save() {
  if (!props.role) return
  const configured = scopes.value.filter(scope => !!scope.scopeType && scope.permissionCode !== 'system:agent:people:query')
  if (configured.some(scope => scope.scopeType === 'SELECTED_COMPANIES' && !scope.companyIds.length)) {
    ElMessage.warning('指定公司范围至少选择一家公司')
    return
  }
  saving.value = true
  try {
    const payload = configured.map(scope => ({
      permissionCode: scope.permissionCode,
      scopeType: scope.scopeType,
      companyIds: scope.scopeType === 'SELECTED_COMPANIES' ? scope.companyIds : []
    }))
    scopes.value = (await systemSettingsApi.saveRoleDataScopes(props.role.id, payload)).data
    ElMessage.success('数据可见范围已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '保存失败') }
  finally { saving.value = false }
}
</script>
