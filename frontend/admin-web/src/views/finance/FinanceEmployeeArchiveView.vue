<template>
  <div class="space-y-6">
    <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 class="text-3xl font-bold text-slate-800">员工档案</h1>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
            与系统设置中的员工管理共用同一套员工主数据，这里仅提供财务档案查询与查看，不提供新增、编辑、删除。
          </p>
        </div>

        <div class="grid grid-cols-2 gap-3 md:grid-cols-4">
          <el-card class="!rounded-3xl !border-slate-100" shadow="never">
            <div class="text-xs text-slate-400">员工总数</div>
            <div class="mt-2 text-2xl font-semibold text-slate-800">{{ employees.length }}</div>
          </el-card>
          <el-card class="!rounded-3xl !border-slate-100" shadow="never">
            <div class="text-xs text-slate-400">启用员工</div>
            <div class="mt-2 text-2xl font-semibold text-emerald-600">{{ enabledCount }}</div>
          </el-card>
          <el-card class="!rounded-3xl !border-slate-100" shadow="never">
            <div class="text-xs text-slate-400">停用员工</div>
            <div class="mt-2 text-2xl font-semibold text-slate-600">{{ disabledCount }}</div>
          </el-card>
          <el-card class="!rounded-3xl !border-slate-100" shadow="never">
            <div class="text-xs text-slate-400">同步来源员工</div>
            <div class="mt-2 text-2xl font-semibold text-sky-600">{{ syncedCount }}</div>
          </el-card>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.4fr),220px,220px,160px,140px]">
        <el-input v-model="filters.keyword" clearable placeholder="搜索姓名 / 用户名 / 手机号 / 邮箱" @keyup.enter="loadEmployees">
          <template #append>
            <el-button :icon="Search" @click="loadEmployees" />
          </template>
        </el-input>

        <el-select v-model="filters.companyId" clearable filterable placeholder="公司">
          <el-option
            v-for="item in companyOptions"
            :key="item.companyId"
            :label="item.companyName"
            :value="item.companyId"
          />
        </el-select>

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
          <el-button type="primary" :icon="RefreshRight" @click="loadEmployees">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="employees" style="width: 100%">
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import {
  financeArchiveApi,
  type CompanyRecord,
  type DepartmentTreeNode,
  type EmployeeQueryPayload,
  type EmployeeRecord
} from '@/api'

type DepartmentOption = {
  id: number
  label: string
}

const loading = ref(false)
const employees = ref<EmployeeRecord[]>([])
const companies = ref<CompanyRecord[]>([])
const departments = ref<DepartmentTreeNode[]>([])
const currentEmployee = ref<EmployeeRecord>()
const detailVisible = ref(false)

const filters = ref<EmployeeQueryPayload>({
  keyword: '',
  companyId: undefined,
  deptId: undefined,
  status: undefined
})

const sourceLabelMap: Record<string, string> = {
  MANUAL: '手工',
  DINGTALK: '钉钉',
  WECOM: '企微',
  FEISHU: '飞书'
}

const companyOptions = computed(() => flattenCompanies(companies.value))
const departmentOptions = computed(() => flattenDepartments(departments.value))
const enabledCount = computed(() => employees.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => employees.value.filter((item) => item.status !== 1).length)
const syncedCount = computed(() => employees.value.filter((item) => item.sourceType && item.sourceType !== 'MANUAL').length)

onMounted(async () => {
  await Promise.all([loadMeta(), loadEmployees()])
})

async function loadMeta() {
  try {
    const res = await financeArchiveApi.getEmployeeMeta()
    companies.value = res.data.companies || []
    departments.value = res.data.departments || []
  } catch (error: any) {
    ElMessage.error(error?.message || '员工档案筛选项加载失败')
  }
}

async function loadEmployees() {
  loading.value = true
  try {
    const payload: EmployeeQueryPayload = {
      keyword: normalizeText(filters.value.keyword),
      companyId: normalizeText(filters.value.companyId),
      deptId: filters.value.deptId,
      status: filters.value.status
    }
    const res = await financeArchiveApi.queryEmployees(payload)
    employees.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '员工档案加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = {
    keyword: '',
    companyId: undefined,
    deptId: undefined,
    status: undefined
  }
  loadEmployees()
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

function flattenCompanies(items: CompanyRecord[], result: CompanyRecord[] = []) {
  items.forEach((item) => {
    result.push(item)
    if (item.children?.length) {
      flattenCompanies(item.children, result)
    }
  })
  return result
}

function flattenDepartments(items: DepartmentTreeNode[], level = 0, result: DepartmentOption[] = []) {
  items.forEach((item) => {
    result.push({
      id: item.id,
      label: `${'—'.repeat(level)}${level > 0 ? ' ' : ''}${item.deptName}`
    })
    if (item.children?.length) {
      flattenDepartments(item.children, level + 1, result)
    }
  })
  return result
}
</script>

<style scoped>
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
