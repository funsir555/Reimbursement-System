<template>
  <div class="space-y-4">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">员工档案</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前公司</span>
            <strong>{{ currentCompanyName || '未设置' }}</strong>
          </div>
        </div>

        <div class="flex flex-wrap gap-2">
          <div class="archive-stat-chip">
            <span>员工总数</span>
            <strong>{{ employees.length }}</strong>
          </div>
          <div class="archive-stat-chip archive-stat-chip-success">
            <span>启用</span>
            <strong>{{ enabledCount }}</strong>
          </div>
          <div class="archive-stat-chip archive-stat-chip-muted">
            <span>停用</span>
            <strong>{{ disabledCount }}</strong>
          </div>
          <div class="archive-stat-chip archive-stat-chip-info">
            <span>同步来源</span>
            <strong>{{ syncedCount }}</strong>
          </div>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.6fr),260px,160px,140px]">
        <el-input v-model="filters.keyword" clearable placeholder="搜索姓名 / 用户名 / 手机号 / 邮箱" @keyup.enter="loadEmployees(true)">
          <template #append>
            <el-button :icon="Search" @click="loadEmployees(true)" />
          </template>
        </el-input>

        <el-select v-model="filters.deptId" clearable filterable placeholder="部门">
          <el-option
            v-for="item in departmentOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>

        <el-select v-model="filters.status" clearable placeholder="状态">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>

        <div class="flex justify-end gap-2">
          <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
          <el-button type="primary" :icon="RefreshRight" @click="loadEmployees(true)">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="paginatedEmployees" style="width: 100%">
        <el-table-column prop="name" label="姓名" min-width="140" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column prop="deptName" label="部门" min-width="140" show-overflow-tooltip />
        <el-table-column prop="companyName" label="公司" min-width="160" show-overflow-tooltip />
        <el-table-column prop="position" label="岗位" min-width="140" show-overflow-tooltip />
        <el-table-column prop="laborRelationBelong" label="劳动关系归属" min-width="160" show-overflow-tooltip />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.sourceType)" effect="plain">
              {{ sourceLabelMap[row.sourceType] || row.sourceType || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色标签" min-width="220">
          <template #default="{ row }">
            <div class="flex flex-wrap gap-1">
              <el-tag
                v-for="roleCode in row.roleCodes"
                :key="`${row.userId}-${roleCode}`"
                size="small"
                effect="plain"
              >
                {{ roleCode }}
              </el-tag>
              <span v-if="!row.roleCodes?.length" class="text-xs text-slate-400">未分配</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最近同步时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastSyncAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 flex justify-start">
        <el-pagination
          v-model:current-page="employeePagination.currentPage.value"
          v-model:page-size="employeePagination.pageSize.value"
          layout="total, sizes, prev, pager, next"
          :total="employeePagination.total.value"
          :page-sizes="employeePagination.pageSizes"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="员工档案详情" size="520px">
      <template v-if="currentEmployee">
        <div class="grid grid-cols-1 gap-4">
          <div class="detail-item"><span>姓名</span><strong>{{ currentEmployee.name || '-' }}</strong></div>
          <div class="detail-item"><span>用户名</span><strong>{{ currentEmployee.username || '-' }}</strong></div>
          <div class="detail-item"><span>手机号</span><strong>{{ currentEmployee.phone || '-' }}</strong></div>
          <div class="detail-item"><span>邮箱</span><strong>{{ currentEmployee.email || '-' }}</strong></div>
          <div class="detail-item"><span>公司</span><strong>{{ currentEmployee.companyName || '-' }}</strong></div>
          <div class="detail-item"><span>部门</span><strong>{{ currentEmployee.deptName || '-' }}</strong></div>
          <div class="detail-item"><span>岗位</span><strong>{{ currentEmployee.position || '-' }}</strong></div>
          <div class="detail-item"><span>劳动关系归属</span><strong>{{ currentEmployee.laborRelationBelong || '-' }}</strong></div>
          <div class="detail-item"><span>来源</span><strong>{{ sourceLabelMap[currentEmployee.sourceType] || currentEmployee.sourceType || '-' }}</strong></div>
          <div class="detail-item"><span>状态</span><strong>{{ currentEmployee.status === 1 ? '启用' : '停用' }}</strong></div>
          <div class="detail-item"><span>同步管理</span><strong>{{ currentEmployee.syncManaged ? '是' : '否' }}</strong></div>
          <div class="detail-item"><span>最近同步时间</span><strong>{{ formatDateTime(currentEmployee.lastSyncAt) }}</strong></div>
          <div class="detail-item items-start">
            <span>角色标签</span>
            <div class="flex flex-wrap gap-2">
              <el-tag
                v-for="roleCode in currentEmployee.roleCodes"
                :key="`detail-${currentEmployee.userId}-${roleCode}`"
                effect="plain"
              >
                {{ roleCode }}
              </el-tag>
              <strong v-if="!currentEmployee.roleCodes?.length">未分配</strong>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import {
  financeArchiveApi,
  type DepartmentTreeNode,
  type EmployeeQueryPayload,
  type EmployeeRecord
} from '@/api'
import { useLocalPagination } from '@/composables/useLocalPagination'
import { useFinanceCompanyStore } from '@/stores/financeCompany'

type DepartmentOption = {
  id: number
  label: string
}

const loading = ref(false)
const employees = ref<EmployeeRecord[]>([])
const employeePagination = useLocalPagination(employees)
const departments = ref<DepartmentTreeNode[]>([])
const currentEmployee = ref<EmployeeRecord>()
const detailVisible = ref(false)
const financeCompany = useFinanceCompanyStore()

const filters = ref<EmployeeQueryPayload>({
  keyword: '',
  deptId: undefined,
  status: undefined
})

const sourceLabelMap: Record<string, string> = {
  MANUAL: '手工',
  DINGTALK: '钉钉',
  WECOM: '企微',
  FEISHU: '飞书'
}

const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const departmentOptions = computed(() => flattenDepartments(departments.value, currentCompanyId.value))
const enabledCount = computed(() => employees.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => employees.value.filter((item) => item.status !== 1).length)
const syncedCount = computed(() => employees.value.filter((item) => item.sourceType && item.sourceType !== 'MANUAL').length)
const paginatedEmployees = computed(() => employeePagination.paginatedRows.value)

onMounted(async () => {
  await loadMeta()
})

watch(
  () => financeCompany.currentCompanyId,
  async (companyId, previousCompanyId) => {
    if (!companyId) return
    if (companyId !== previousCompanyId) {
      filters.value.deptId = undefined
    }
    await loadEmployees(true)
  },
  { immediate: true }
)

async function loadMeta() {
  try {
    const res = await financeArchiveApi.getEmployeeMeta()
    departments.value = res.data.departments || []
  } catch (error: any) {
    ElMessage.error(error?.message || '员工档案筛选项加载失败')
  }
}

async function loadEmployees(resetPage = false) {
  if (resetPage) {
    employeePagination.resetToFirstPage()
  }
  loading.value = true
  try {
    const payload: EmployeeQueryPayload = {
      keyword: normalizeText(filters.value.keyword),
      companyId: normalizeText(financeCompany.currentCompanyId),
      deptId: filters.value.deptId,
      status: filters.value.status
    }
    const res = await financeArchiveApi.queryEmployees(payload)
    employees.value = res.data || []
    employeePagination.clampCurrentPage()
  } catch (error: any) {
    ElMessage.error(error?.message || '员工档案加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = {
    keyword: '',
    deptId: undefined,
    status: undefined
  }
  void loadEmployees(true)
}

function openDetail(row: EmployeeRecord) {
  currentEmployee.value = row
  detailVisible.value = true
}

function normalizeText(value?: string) {
  const text = String(value || '').trim()
  return text || undefined
}

function sourceTagType(sourceType?: string) {
  if (sourceType === 'MANUAL' || !sourceType) {
    return 'info'
  }
  if (sourceType === 'WECOM') {
    return 'success'
  }
  if (sourceType === 'DINGTALK') {
    return 'warning'
  }
  return 'primary'
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function flattenDepartments(items: DepartmentTreeNode[], companyId?: string, level = 0, result: DepartmentOption[] = []) {
  items.forEach((item) => {
    if (companyId && item.companyId && item.companyId !== companyId) {
      return
    }
    result.push({
      id: item.id,
      label: `${'—'.repeat(level)}${level > 0 ? ' ' : ''}${item.deptName}`
    })
    if (item.children?.length) {
      flattenDepartments(item.children, companyId, level + 1, result)
    }
  })
  return result
}
</script>

<style scoped>
.archive-stat-chip {
  display: inline-flex;
  min-width: 84px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #f8fafc;
  padding: 8px 12px;
  color: #475569;
}

.archive-stat-chip span {
  font-size: 12px;
}

.archive-stat-chip strong {
  font-size: 14px;
  color: #0f172a;
}

.archive-stat-chip-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.archive-stat-chip-success strong {
  color: #047857;
}

.archive-stat-chip-muted strong {
  color: #475569;
}

.archive-stat-chip-info {
  border-color: #bae6fd;
  background: #f0f9ff;
}

.archive-stat-chip-info strong {
  color: #0369a1;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #f8fafc;
}

.detail-item span {
  font-size: 12px;
  color: #64748b;
}

.detail-item strong {
  font-size: 14px;
  line-height: 1.6;
  color: #0f172a;
  word-break: break-word;
}
</style>
