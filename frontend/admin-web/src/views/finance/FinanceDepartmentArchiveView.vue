<template>
  <div class="space-y-4">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">部门档案</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前公司</span>
            <strong>{{ currentCompanyLabel || currentCompanyName || '未设置' }}</strong>
          </div>
        </div>

        <div class="flex flex-wrap gap-2">
          <div class="archive-stat-chip">
            <span>部门总数</span>
            <strong>{{ departments.length }}</strong>
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
            <span>同步托管</span>
            <strong>{{ syncManagedCount }}</strong>
          </div>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="rounded-2xl border border-sky-100 bg-sky-50/70 px-4 py-3 text-sm leading-6 text-sky-800">
        部门档案属于财务共享基础档案，当前页面仅提供查看入口；如需维护组织架构，请前往
        <strong>系统设置 &gt; 组织架构</strong>。
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.6fr),260px,160px,140px]">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索部门编码 / 部门名称 / 负责人"
          @keyup.enter="loadDepartments(true)"
        >
          <template #append>
            <el-button :icon="Search" @click="loadDepartments(true)" />
          </template>
        </el-input>

        <el-select v-model="filters.parentId" clearable filterable placeholder="上级部门">
          <el-option
            v-for="item in parentDepartmentOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>

        <el-select v-model="filters.status" clearable placeholder="状态">
          <el-option
            v-for="item in meta.statusOptions"
            :key="item.value"
            :label="item.label"
            :value="Number(item.value)"
          />
        </el-select>

        <div class="flex justify-end gap-2">
          <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
          <el-button type="primary" :icon="RefreshRight" @click="loadDepartments(true)">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="paginatedDepartments" style="width: 100%">
        <el-table-column prop="deptCode" label="部门编码" min-width="120" />
        <el-table-column prop="deptName" label="部门名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="parentDeptName" label="上级部门" min-width="160" show-overflow-tooltip />
        <el-table-column prop="companyName" label="所属公司" min-width="180" show-overflow-tooltip />
        <el-table-column prop="leaderName" label="部门负责人" min-width="140" show-overflow-tooltip />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.syncSource)" effect="plain">
              {{ sourceLabelMap[row.syncSource || ''] || row.syncSource || '-' }}
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
          v-model:current-page="departmentPagination.currentPage.value"
          v-model:page-size="departmentPagination.pageSize.value"
          layout="total, sizes, prev, pager, next"
          :total="departmentPagination.total.value"
          :page-sizes="departmentPagination.pageSizes"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="部门档案详情" size="520px">
      <template v-if="currentDepartment">
        <div class="mb-4 rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-800">
          当前页面仅用于财务查看。若需维护该部门信息，请前往 <strong>系统设置 &gt; 组织架构</strong>。
        </div>
        <div class="grid grid-cols-1 gap-4">
          <div class="detail-item"><span>部门编码</span><strong>{{ currentDepartment.deptCode || '-' }}</strong></div>
          <div class="detail-item"><span>部门名称</span><strong>{{ currentDepartment.deptName || '-' }}</strong></div>
          <div class="detail-item"><span>上级部门</span><strong>{{ currentDepartment.parentDeptName || '-' }}</strong></div>
          <div class="detail-item"><span>所属公司</span><strong>{{ currentDepartment.companyName || '-' }}</strong></div>
          <div class="detail-item"><span>部门负责人</span><strong>{{ currentDepartment.leaderName || '-' }}</strong></div>
          <div class="detail-item"><span>统计部门归属</span><strong>{{ currentDepartment.statDepartmentBelong || '-' }}</strong></div>
          <div class="detail-item"><span>统计大区归属</span><strong>{{ currentDepartment.statRegionBelong || '-' }}</strong></div>
          <div class="detail-item"><span>统计区域归属</span><strong>{{ currentDepartment.statAreaBelong || '-' }}</strong></div>
          <div class="detail-item"><span>来源</span><strong>{{ sourceLabelMap[currentDepartment.syncSource || ''] || currentDepartment.syncSource || '-' }}</strong></div>
          <div class="detail-item"><span>同步托管</span><strong>{{ yesNoText(currentDepartment.syncManaged) }}</strong></div>
          <div class="detail-item"><span>同步启用</span><strong>{{ yesNoText(currentDepartment.syncEnabled) }}</strong></div>
          <div class="detail-item"><span>同步状态</span><strong>{{ currentDepartment.syncStatus || '-' }}</strong></div>
          <div class="detail-item"><span>同步备注</span><strong>{{ currentDepartment.syncRemark || '-' }}</strong></div>
          <div class="detail-item"><span>状态</span><strong>{{ currentDepartment.status === 1 ? '启用' : '停用' }}</strong></div>
          <div class="detail-item"><span>排序</span><strong>{{ currentDepartment.sortOrder ?? '-' }}</strong></div>
          <div class="detail-item"><span>最近同步时间</span><strong>{{ formatDateTime(currentDepartment.lastSyncAt) }}</strong></div>
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
  type FinanceDepartmentArchiveMeta,
  type FinanceDepartmentQueryPayload,
  type FinanceDepartmentSummary,
  type FinanceDepartmentTreeNode
} from '@/api'
import { useLocalPagination } from '@/composables/useLocalPagination'
import { useFinanceCompanyStore } from '@/stores/financeCompany'

type DepartmentOption = {
  id: number
  label: string
}

const loading = ref(false)
const departments = ref<FinanceDepartmentSummary[]>([])
const departmentPagination = useLocalPagination(departments)
const currentDepartment = ref<FinanceDepartmentSummary>()
const detailVisible = ref(false)
const financeCompany = useFinanceCompanyStore()

const meta = ref<FinanceDepartmentArchiveMeta>({
  departments: [],
  statusOptions: []
})

const filters = ref<FinanceDepartmentQueryPayload>({
  keyword: '',
  parentId: undefined,
  status: undefined
})

const sourceLabelMap: Record<string, string> = {
  MANUAL: '手工',
  DINGTALK: '钉钉',
  WECOM: '企微',
  FEISHU: '飞书'
}

const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const currentCompanyLabel = computed(() => financeCompany.currentCompanyLabel)
const parentDepartmentOptions = computed(() => flattenDepartments(meta.value.departments))
const enabledCount = computed(() => departments.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => departments.value.filter((item) => item.status !== 1).length)
const syncManagedCount = computed(() => departments.value.filter((item) => item.syncManaged).length)
const paginatedDepartments = computed(() => departmentPagination.paginatedRows.value)

onMounted(async () => {
  await loadMeta()
  await loadDepartments(true)
})

watch(
  () => financeCompany.currentCompanyId,
  () => {
    departmentPagination.resetToFirstPage()
  }
)

async function loadMeta() {
  try {
    const res = await financeArchiveApi.getDepartmentArchiveMeta()
    meta.value = res.data || {
      departments: [],
      statusOptions: []
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '部门档案筛选项加载失败')
  }
}

async function loadDepartments(resetPage = false) {
  if (resetPage) {
    departmentPagination.resetToFirstPage()
  }
  loading.value = true
  try {
    const payload: FinanceDepartmentQueryPayload = {
      keyword: normalizeText(filters.value.keyword),
      parentId: filters.value.parentId,
      status: filters.value.status
    }
    const res = await financeArchiveApi.queryDepartments(payload)
    departments.value = res.data || []
    departmentPagination.clampCurrentPage()
  } catch (error: any) {
    ElMessage.error(error?.message || '部门档案加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = {
    keyword: '',
    parentId: undefined,
    status: undefined
  }
  void loadDepartments(true)
}

function openDetail(row: FinanceDepartmentSummary) {
  currentDepartment.value = row
  detailVisible.value = true
}

function normalizeText(value?: string) {
  const text = String(value || '').trim()
  return text || undefined
}

function flattenDepartments(items: FinanceDepartmentTreeNode[], level = 0, result: DepartmentOption[] = []) {
  items.forEach((item) => {
    result.push({
      id: item.id,
      label: `${'--'.repeat(level)}${level > 0 ? ' ' : ''}${item.deptName}`
    })
    if (item.children?.length) {
      flattenDepartments(item.children, level + 1, result)
    }
  })
  return result
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

function yesNoText(value?: boolean) {
  return value ? '是' : '否'
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
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
