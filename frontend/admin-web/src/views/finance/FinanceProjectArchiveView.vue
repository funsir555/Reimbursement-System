<template>
  <div class="space-y-4">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">项目档案</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前公司</span>
            <strong>{{ currentCompanyName || '未选择' }}</strong>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <el-button :icon="RefreshRight" @click="reloadCurrentTab">刷新</el-button>
          <el-button v-if="activeTab === 'classes' && canCreate" type="primary" :icon="Plus" @click="openClassDialog('create')">新建分类</el-button>
          <el-button v-else-if="activeTab === 'projects' && canCreate" type="primary" :icon="Plus" @click="openProjectDialog('create')">新建项目</el-button>
          <el-button v-else-if="activeTab === 'cashFlows' && canCreate" type="primary" :icon="Plus" @click="openCashFlowDrawer('create')">新增现金流量</el-button>
        </div>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="project-archive-tabs">
      <el-tab-pane label="项目分类" name="classes">
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),minmax(0,1fr),180px,160px]">
            <el-input v-model="classFilters.keyword" clearable placeholder="分类编码 / 分类名称" @keyup.enter="loadProjectClasses(true)">
              <template #append><el-button :icon="Search" @click="loadProjectClasses(true)" /></template>
            </el-input>
            <div />
            <el-select v-model="classFilters.status" clearable placeholder="启用状态" @change="loadProjectClasses(true)">
              <el-option v-for="item in meta.statusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
            <div class="flex justify-end"><el-button :icon="RefreshRight" @click="resetClassFilters">重置</el-button></div>
          </div>
        </el-card>

        <el-card class="!rounded-3xl !shadow-sm">
          <el-table v-loading="loadingClasses" :data="paginatedProjectClasses" style="width: 100%">
            <el-table-column prop="project_class_code" label="分类编码" min-width="180" />
            <el-table-column prop="project_class_name" label="分类名称" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="已挂项目" width="120">
              <template #default="{ row }"><el-tag :type="row.has_projects ? 'warning' : 'success'" effect="plain">{{ row.has_projects ? '是' : '否' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="updated_at" label="更新时间" min-width="180" />
            <el-table-column label="操作" min-width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="canEdit" link type="primary" @click="openClassDialog('edit', row)">编辑</el-button>
                <el-button v-if="canDisable" link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleProjectClassStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="mt-4 flex justify-start">
            <el-pagination v-model:current-page="classPagination.currentPage.value" v-model:page-size="classPagination.pageSize.value" layout="total, sizes, prev, pager, next" :total="classPagination.total.value" :page-sizes="classPagination.pageSizes" />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="项目档案" name="projects">
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),minmax(0,1fr),220px,160px,160px,160px]">
            <el-input v-model="projectFilters.keyword" clearable placeholder="项目编码 / 项目名称" @keyup.enter="loadProjects(true)">
              <template #append><el-button :icon="Search" @click="loadProjects(true)" /></template>
            </el-input>
            <div />
            <el-select v-model="projectFilters.projectClassCode" clearable placeholder="项目分类" @change="loadProjects(true)">
              <el-option v-for="item in meta.projectClassOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="projectFilters.status" clearable placeholder="启用状态" @change="loadProjects(true)">
              <el-option v-for="item in meta.statusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
            <el-select v-model="projectFilters.bclose" clearable placeholder="封存状态" @change="loadProjects(true)">
              <el-option v-for="item in meta.closeStatusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
            <div class="flex justify-end"><el-button :icon="RefreshRight" @click="resetProjectFilters">重置</el-button></div>
          </div>
        </el-card>

        <el-card class="!rounded-3xl !shadow-sm">
          <el-table v-loading="loadingProjects" :data="paginatedProjects" style="width: 100%">
            <el-table-column prop="citemcode" label="项目编码" min-width="160" />
            <el-table-column prop="citemname" label="项目名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="project_class_name" label="项目分类" min-width="180" show-overflow-tooltip />
            <el-table-column label="启用状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column label="封存状态" width="110"><template #default="{ row }"><el-tag :type="row.bclose === 1 ? 'warning' : 'success'" effect="plain">{{ row.bclose === 1 ? '已封存' : '未封存' }}</el-tag></template></el-table-column>
            <el-table-column prop="d_end_date" label="结束日期" min-width="140" />
            <el-table-column prop="updated_at" label="更新时间" min-width="180" />
            <el-table-column label="操作" min-width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openProjectDialog('detail', row)">详情</el-button>
                <el-button v-if="canEdit" link type="primary" @click="openProjectDialog('edit', row)">编辑</el-button>
                <el-button v-if="canDisable" link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleProjectStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                <el-button v-if="canClose" link :type="row.bclose === 1 ? 'success' : 'warning'" @click="toggleProjectClose(row)">{{ row.bclose === 1 ? '解封' : '封存' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="mt-4 flex justify-start">
            <el-pagination v-model:current-page="projectPagination.currentPage.value" v-model:page-size="projectPagination.pageSize.value" layout="total, sizes, prev, pager, next" :total="projectPagination.total.value" :page-sizes="projectPagination.pageSizes" />
          </div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="现金流量" name="cashFlows">
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),180px,180px,160px]">
            <el-input v-model="cashFlowFilters.keyword" clearable placeholder="编码 / 名称" @keyup.enter="loadCashFlows(true)">
              <template #append><el-button :icon="Search" @click="loadCashFlows(true)" /></template>
            </el-input>
            <el-select v-model="cashFlowFilters.direction" clearable placeholder="流量方向" @change="loadCashFlows(true)">
              <el-option v-for="item in cashFlowDirectionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="cashFlowFilters.status" clearable placeholder="启用状态" @change="loadCashFlows(true)">
              <el-option v-for="item in meta.statusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
            <div class="flex justify-end"><el-button :icon="RefreshRight" @click="resetCashFlowFilters">重置</el-button></div>
          </div>
        </el-card>

        <el-card class="!rounded-3xl !shadow-sm">
          <el-table v-loading="loadingCashFlows" :data="paginatedCashFlows" style="width: 100%">
            <el-table-column prop="cash_flow_code" label="编码" min-width="140" />
            <el-table-column prop="cash_flow_name" label="名称" min-width="280" show-overflow-tooltip />
            <el-table-column label="方向" width="120"><template #default="{ row }"><el-tag :type="row.direction === 'INFLOW' ? 'success' : 'warning'" effect="plain">{{ formatCashFlowDirection(row.direction) }}</el-tag></template></el-table-column>
            <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column prop="updated_at" label="更新时间" min-width="180" />
            <el-table-column label="操作" min-width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="canEdit" link type="primary" @click="openCashFlowDrawer('edit', row)">编辑</el-button>
                <el-button v-if="canDisable" link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleCashFlowStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="mt-4 flex justify-start">
            <el-pagination v-model:current-page="cashFlowPagination.currentPage.value" v-model:page-size="cashFlowPagination.pageSize.value" layout="total, sizes, prev, pager, next" :total="cashFlowPagination.total.value" :page-sizes="cashFlowPagination.pageSizes" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="classDialogVisible" :title="classDialogTitle" width="560px" destroy-on-close>
      <el-form label-position="top" class="grid grid-cols-1 gap-4">
        <el-form-item label="所属公司" class="!mb-0"><el-input :model-value="currentCompanyName || currentCompanyId || ''" disabled /></el-form-item>
        <el-form-item label="分类编码" class="!mb-0"><el-input v-model="classForm.project_class_code" :disabled="classDialogMode === 'edit'" maxlength="2" placeholder="请输入1-2位数字分类编码" /></el-form-item>
        <el-form-item label="分类名称" class="!mb-0"><el-input v-model="classForm.project_class_name" placeholder="请输入项目分类名称" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeClassDialog">取消</el-button>
          <el-button type="primary" :loading="savingClass" @click="saveProjectClass">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <finance-project-archive-dialog
      ref="projectDialogRef"
      :company-id="currentCompanyId"
      :company-name="currentCompanyName"
      :project-class-options="meta.projectClassOptions"
      @saved="handleProjectSaved"
      @closed="projectDialogVisible = false"
    />

    <el-drawer v-model="cashFlowDrawerVisible" :title="cashFlowDrawerTitle" size="520px" destroy-on-close>
      <el-form label-position="top" class="grid grid-cols-1 gap-4">
        <el-form-item label="所属公司" class="!mb-0"><el-input :model-value="currentCompanyName || currentCompanyId || ''" disabled /></el-form-item>
        <el-form-item label="编码" class="!mb-0"><el-input v-model="cashFlowForm.cash_flow_code" :disabled="cashFlowDrawerMode === 'edit'" maxlength="32" placeholder="请输入现金流量编码" /></el-form-item>
        <el-form-item label="名称" class="!mb-0"><el-input v-model="cashFlowForm.cash_flow_name" maxlength="200" placeholder="请输入现金流量名称" /></el-form-item>
        <el-form-item label="方向" class="!mb-0"><el-select v-model="cashFlowForm.direction" placeholder="请选择现金流量方向"><el-option v-for="item in cashFlowDirectionOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="状态" class="!mb-0"><el-select v-model="cashFlowForm.status" placeholder="请选择状态"><el-option v-for="item in meta.statusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" /></el-select></el-form-item>
        <el-form-item label="排序" class="!mb-0"><el-input-number v-model="cashFlowForm.sort_order" :min="0" class="w-full" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeCashFlowDrawer">取消</el-button>
          <el-button type="primary" :loading="savingCashFlow" @click="saveCashFlow">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import {
  financeArchiveApi,
  type FinanceCashFlowItem,
  type FinanceCashFlowSavePayload,
  type FinanceProjectArchiveMeta,
  type FinanceProjectClassSavePayload,
  type FinanceProjectClassSummary,
  type FinanceProjectDetail,
  type FinanceProjectSavePayload,
  type FinanceProjectSummary
} from '@/api'
import FinanceProjectArchiveDialog from '@/components/finance/FinanceProjectArchiveDialog.vue'
import { useLocalPagination } from '@/composables/useLocalPagination'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type ClassDialogMode = 'create' | 'edit'
type ProjectDialogMode = 'create' | 'edit' | 'detail'
type CashFlowDrawerMode = 'create' | 'edit'

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const financeCompany = useFinanceCompanyStore()
const meta = reactive<FinanceProjectArchiveMeta>({ statusOptions: [], closeStatusOptions: [], projectClassOptions: [] })
const activeTab = ref<'classes' | 'projects' | 'cashFlows'>('classes')
const loadingClasses = ref(false)
const loadingProjects = ref(false)
const loadingCashFlows = ref(false)
const savingClass = ref(false)
const savingProject = ref(false)
const savingCashFlow = ref(false)
const projectClasses = ref<FinanceProjectClassSummary[]>([])
const projects = ref<FinanceProjectSummary[]>([])
const cashFlows = ref<FinanceCashFlowItem[]>([])
const classPagination = useLocalPagination(projectClasses)
const projectPagination = useLocalPagination(projects)
const cashFlowPagination = useLocalPagination(cashFlows)
const classDialogVisible = ref(false)
const projectDialogVisible = ref(false)
const cashFlowDrawerVisible = ref(false)
const classDialogMode = ref<ClassDialogMode>('create')
const cashFlowDrawerMode = ref<CashFlowDrawerMode>('create')
const editingProjectClassCode = ref('')
const editingCashFlowId = ref<number | null>(null)
const COMPANY_SWITCH_GUARD_KEY = 'finance-project-archive'
const projectDialogRef = ref<InstanceType<typeof FinanceProjectArchiveDialog> | null>(null)
const fallbackProjectForm = reactive<FinanceProjectDetail>(createDefaultProjectForm())
let guardRegistered = false

const cashFlowDirectionOptions = [
  { value: 'INFLOW', label: '流入' },
  { value: 'OUTFLOW', label: '流出' }
] as const
const classFilters = reactive({ keyword: '', status: undefined as number | undefined })
const projectFilters = reactive({ keyword: '', projectClassCode: '', status: undefined as number | undefined, bclose: undefined as number | undefined })
const cashFlowFilters = reactive({ keyword: '', direction: '' as '' | 'INFLOW' | 'OUTFLOW', status: undefined as number | undefined })
const classForm = reactive<FinanceProjectClassSavePayload>({ project_class_code: '', project_class_name: '' })
const cashFlowForm = reactive<FinanceCashFlowItem>(createDefaultCashFlowForm())

const canCreate = computed(() => hasPermission('finance:archives:projects:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:projects:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:projects:disable', permissionCodes.value))
const canClose = computed(() => hasPermission('finance:archives:projects:close', permissionCodes.value))
const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const paginatedProjectClasses = computed(() => classPagination.paginatedRows.value)
const paginatedProjects = computed(() => projectPagination.paginatedRows.value)
const paginatedCashFlows = computed(() => cashFlowPagination.paginatedRows.value)
const classDialogTitle = computed(() => (classDialogMode.value === 'create' ? '新建项目分类' : '编辑项目分类'))
const cashFlowDrawerTitle = computed(() => (cashFlowDrawerMode.value === 'create' ? '新增现金流量' : '编辑现金流量'))
const projectForm = computed(() => projectDialogRef.value?.projectForm || fallbackProjectForm)

onMounted(registerCompanySwitchGuard)
onActivated(registerCompanySwitchGuard)
onDeactivated(unregisterCompanySwitchGuard)
onBeforeUnmount(() => unregisterCompanySwitchGuard())

watch(
  () => financeCompany.currentCompanyId,
  async (companyId, previousCompanyId) => {
    if (!companyId) {
      projectClasses.value = []
      projects.value = []
      cashFlows.value = []
      return
    }
    if (companyId !== previousCompanyId) {
      closeClassDialog()
      closeProjectDialog()
      closeCashFlowDrawer()
    }
    await loadMeta()
    await Promise.all([loadProjectClasses(true), loadProjects(true), loadCashFlows(true)])
  },
  { immediate: true }
)

async function loadMeta() {
  if (!currentCompanyId.value) return
  try {
    const res = await financeArchiveApi.getProjectArchiveMeta(currentCompanyId.value)
    meta.statusOptions = res.data.statusOptions || []
    meta.closeStatusOptions = res.data.closeStatusOptions || []
    meta.projectClassOptions = res.data.projectClassOptions || []
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载项目档案元数据失败'))
  }
}

async function loadProjectClasses(resetPage = false) {
  if (resetPage) classPagination.resetToFirstPage()
  if (!currentCompanyId.value) {
    projectClasses.value = []
    classPagination.clampCurrentPage()
    return
  }
  loadingClasses.value = true
  try {
    const res = await financeArchiveApi.listProjectClasses({ companyId: currentCompanyId.value, keyword: classFilters.keyword.trim() || undefined, status: classFilters.status })
    projectClasses.value = res.data || []
    classPagination.clampCurrentPage()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载项目分类失败'))
  } finally {
    loadingClasses.value = false
  }
}

async function loadProjects(resetPage = false) {
  if (resetPage) projectPagination.resetToFirstPage()
  if (!currentCompanyId.value) {
    projects.value = []
    projectPagination.clampCurrentPage()
    return
  }
  loadingProjects.value = true
  try {
    const res = await financeArchiveApi.listProjects({
      companyId: currentCompanyId.value,
      keyword: projectFilters.keyword.trim() || undefined,
      projectClassCode: projectFilters.projectClassCode || undefined,
      status: projectFilters.status,
      bclose: projectFilters.bclose
    })
    projects.value = res.data || []
    projectPagination.clampCurrentPage()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载项目档案失败'))
  } finally {
    loadingProjects.value = false
  }
}

async function loadCashFlows(resetPage = false) {
  if (resetPage) cashFlowPagination.resetToFirstPage()
  if (!currentCompanyId.value) {
    cashFlows.value = []
    cashFlowPagination.clampCurrentPage()
    return
  }
  loadingCashFlows.value = true
  try {
    const res = await financeArchiveApi.listCashFlows({
      companyId: currentCompanyId.value,
      keyword: cashFlowFilters.keyword.trim() || undefined,
      direction: cashFlowFilters.direction || undefined,
      status: cashFlowFilters.status
    })
    cashFlows.value = res.data || []
    cashFlowPagination.clampCurrentPage()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载现金流量失败'))
  } finally {
    loadingCashFlows.value = false
  }
}

function resetClassFilters() {
  classFilters.keyword = ''
  classFilters.status = undefined
  void loadProjectClasses(true)
}

function resetProjectFilters() {
  projectFilters.keyword = ''
  projectFilters.projectClassCode = ''
  projectFilters.status = undefined
  projectFilters.bclose = undefined
  void loadProjects(true)
}

function resetCashFlowFilters() {
  cashFlowFilters.keyword = ''
  cashFlowFilters.direction = ''
  cashFlowFilters.status = undefined
  void loadCashFlows(true)
}

function reloadCurrentTab() {
  if (activeTab.value === 'classes') return void loadProjectClasses(true)
  if (activeTab.value === 'projects') return void loadProjects(true)
  void loadCashFlows(true)
}

function openClassDialog(mode: ClassDialogMode, row?: FinanceProjectClassSummary) {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前财务公司缺失，无法维护项目分类')
    return
  }
  classDialogMode.value = mode
  editingProjectClassCode.value = row?.project_class_code || ''
  classForm.project_class_code = row?.project_class_code || ''
  classForm.project_class_name = row?.project_class_name || ''
  classDialogVisible.value = true
}

function closeClassDialog() {
  classDialogVisible.value = false
  editingProjectClassCode.value = ''
  classForm.project_class_code = ''
  classForm.project_class_name = ''
}

async function saveProjectClass() {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前财务公司缺失，无法保存项目分类')
    return
  }
  if (!classForm.project_class_code.trim()) return ElMessage.warning('项目分类编码不能为空')
  if (!/^\d{1,2}$/.test(classForm.project_class_code.trim())) return ElMessage.warning('项目分类编码必须为1-2位数字文本')
  if (!classForm.project_class_name.trim()) return ElMessage.warning('项目分类名称不能为空')
  savingClass.value = true
  try {
    const payload = { project_class_code: classForm.project_class_code.trim(), project_class_name: classForm.project_class_name.trim() }
    if (classDialogMode.value === 'create') {
      await financeArchiveApi.createProjectClass(currentCompanyId.value, payload)
      ElMessage.success('项目分类创建成功')
    } else {
      await financeArchiveApi.updateProjectClass(currentCompanyId.value, editingProjectClassCode.value, payload)
      ElMessage.success('项目分类更新成功')
    }
    closeClassDialog()
    await loadMeta()
    await loadProjectClasses()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存项目分类失败'))
  } finally {
    savingClass.value = false
  }
}

async function toggleProjectClassStatus(row: FinanceProjectClassSummary) {
  if (!currentCompanyId.value) return
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}项目分类 ${row.project_class_code} - ${row.project_class_name} 吗？`, `${actionText}项目分类`, { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
  } catch {
    return
  }
  try {
    await financeArchiveApi.updateProjectClassStatus(currentCompanyId.value, row.project_class_code, nextStatus)
    ElMessage.success(`项目分类${actionText}成功`)
    await loadMeta()
    await loadProjectClasses()
    await loadProjects()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, `${actionText}项目分类失败`))
  }
}

function openProjectDialog(mode: ProjectDialogMode, row?: FinanceProjectSummary) {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前财务公司缺失，无法维护项目档案')
    return
  }
  projectDialogVisible.value = true
  if (mode === 'create') {
    projectDialogRef.value?.openCreateDialog()
    return
  }
  if (!row?.citemcode) {
    return
  }
  if (mode === 'edit') {
    void projectDialogRef.value?.openEditDialog(row.citemcode)
    return
  }
  void projectDialogRef.value?.openDetailDialog(row.citemcode)
}
function closeProjectDialog() {
  projectDialogVisible.value = false
  projectDialogRef.value?.closeProjectDialog()
}

async function saveProject() {
  await projectDialogRef.value?.saveProject()
}

async function toggleProjectStatus(row: FinanceProjectSummary) {
  if (!currentCompanyId.value) return
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}项目 ${row.citemcode} - ${row.citemname} 吗？`, `${actionText}项目`, { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
  } catch {
    return
  }
  try {
    await financeArchiveApi.updateProjectStatus(currentCompanyId.value, row.citemcode, nextStatus)
    ElMessage.success(`项目${actionText}成功`)
    await loadProjects()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, `${actionText}项目失败`))
  }
}

async function toggleProjectClose(row: FinanceProjectSummary) {
  if (!currentCompanyId.value) return
  const nextClose = row.bclose === 1 ? 0 : 1
  const actionText = nextClose === 1 ? '封存' : '解封'
  try {
    await ElMessageBox.confirm(`确认${actionText}项目 ${row.citemcode} - ${row.citemname} 吗？`, `${actionText}项目`, { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
  } catch {
    return
  }
  try {
    await financeArchiveApi.updateProjectClose(currentCompanyId.value, row.citemcode, nextClose)
    ElMessage.success(`项目${actionText}成功`)
    await loadProjects()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, `${actionText}项目失败`))
  }
}

function openCashFlowDrawer(mode: CashFlowDrawerMode, row?: FinanceCashFlowItem) {
  if (!currentCompanyId.value) return ElMessage.warning('当前财务公司缺失，无法维护现金流量')
  cashFlowDrawerMode.value = mode
  editingCashFlowId.value = row?.id || null
  Object.assign(cashFlowForm, createDefaultCashFlowForm(), row || {})
  cashFlowDrawerVisible.value = true
}

function closeCashFlowDrawer() {
  cashFlowDrawerVisible.value = false
  editingCashFlowId.value = null
  Object.assign(cashFlowForm, createDefaultCashFlowForm())
}

function buildProjectPayload(): FinanceProjectSavePayload {
  return projectDialogRef.value?.buildProjectPayload() || {
    citemcode: String(projectForm.value.citemcode || '').trim(),
    citemname: String(projectForm.value.citemname || '').trim(),
    citemccode: String(projectForm.value.citemccode || '').trim(),
    iotherused: normalizeNumber(projectForm.value.iotherused, 0),
    d_end_date: trimString(projectForm.value.d_end_date)
  }
}

async function handleProjectSaved() {
  projectDialogVisible.value = false
  await loadProjects()
}

function buildCashFlowPayload(): FinanceCashFlowSavePayload {
  return {
    cash_flow_code: String(cashFlowForm.cash_flow_code || '').trim(),
    cash_flow_name: String(cashFlowForm.cash_flow_name || '').trim(),
    direction: (cashFlowForm.direction || 'INFLOW') as 'INFLOW' | 'OUTFLOW',
    status: normalizeNumber(cashFlowForm.status, 1),
    sort_order: normalizeNumber(cashFlowForm.sort_order, 0)
  }
}

async function saveCashFlow() {
  if (!currentCompanyId.value) return ElMessage.warning('当前财务公司缺失，无法保存现金流量')
  if (!String(cashFlowForm.cash_flow_code || '').trim()) return ElMessage.warning('现金流量编码不能为空')
  if (!String(cashFlowForm.cash_flow_name || '').trim()) return ElMessage.warning('现金流量名称不能为空')
  if (!cashFlowForm.direction) return ElMessage.warning('现金流量方向不能为空')
  savingCashFlow.value = true
  try {
    const payload = buildCashFlowPayload()
    if (cashFlowDrawerMode.value === 'create') {
      await financeArchiveApi.createCashFlow(currentCompanyId.value, payload)
      ElMessage.success('现金流量创建成功')
    } else {
      await financeArchiveApi.updateCashFlow(currentCompanyId.value, Number(editingCashFlowId.value), payload)
      ElMessage.success('现金流量更新成功')
    }
    closeCashFlowDrawer()
    await loadCashFlows()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存现金流量失败'))
  } finally {
    savingCashFlow.value = false
  }
}

async function toggleCashFlowStatus(row: FinanceCashFlowItem) {
  if (!currentCompanyId.value || row.id == null) return
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}现金流量 ${row.cash_flow_code} - ${row.cash_flow_name} 吗？`, `${actionText}现金流量`, { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
  } catch {
    return
  }
  try {
    await financeArchiveApi.updateCashFlowStatus(currentCompanyId.value, row.id, nextStatus)
    ElMessage.success(`现金流量${actionText}成功`)
    await loadCashFlows()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, `${actionText}现金流量失败`))
  }
}

function formatCashFlowDirection(direction?: string) {
  return direction === 'OUTFLOW' ? '流出' : '流入'
}

function registerCompanySwitchGuard() {
  if (guardRegistered) return
  financeCompany.registerSwitchGuard(COMPANY_SWITCH_GUARD_KEY, confirmCompanySwitch)
  guardRegistered = true
}

function unregisterCompanySwitchGuard() {
  if (!guardRegistered) return
  financeCompany.unregisterSwitchGuard(COMPANY_SWITCH_GUARD_KEY)
  guardRegistered = false
}

async function confirmCompanySwitch() {
  if (!classDialogVisible.value && !projectDialogVisible.value && !cashFlowDrawerVisible.value) return true
  try {
    await ElMessageBox.confirm('切换公司会关闭当前项目档案弹窗，是否继续？', '切换公司', { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' })
    return true
  } catch {
    return false
  }
}

function normalizeNumber(value: number | undefined, fallback: number) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function trimString(value?: string) {
  const normalized = String(value || '').trim()
  return normalized || undefined
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function createDefaultProjectForm(): FinanceProjectDetail {
  return { citemcode: '', citemname: '', citemccode: '', bclose: 0, status: 1, iotherused: 0, d_end_date: '', referenced_by_voucher: false }
}

function createDefaultCashFlowForm(): FinanceCashFlowItem {
  return { cash_flow_code: '', cash_flow_name: '', direction: 'INFLOW', status: 1, sort_order: 0 }
}

defineExpose({
  activeTab,
  meta,
  classFilters,
  projectFilters,
  cashFlowFilters,
  projectForm,
  cashFlowForm,
  projectClasses,
  projects,
  cashFlows,
  classPagination,
  projectPagination,
  cashFlowPagination,
  paginatedProjectClasses,
  paginatedProjects,
  paginatedCashFlows,
  loadProjectClasses,
  loadProjects,
  loadCashFlows,
  openClassDialog,
  openProjectDialog,
  openCashFlowDrawer,
  buildProjectPayload,
  buildCashFlowPayload,
  saveProject,
  saveCashFlow,
  toggleProjectStatus,
  toggleProjectClose,
  toggleCashFlowStatus
})
</script>

<style scoped>
:deep(.project-archive-tabs .el-tabs__header) {
  margin-bottom: 0;
}
</style>
