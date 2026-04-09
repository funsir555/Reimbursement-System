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
          <el-button v-if="activeTab === 'classes' && canCreate" type="primary" :icon="Plus" @click="openClassDialog('create')">
            新建分类
          </el-button>
          <el-button v-if="activeTab === 'projects' && canCreate" type="primary" :icon="Plus" @click="openProjectDialog('create')">
            新建项目
          </el-button>
        </div>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="project-archive-tabs">
      <el-tab-pane label="项目分类" name="classes">
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),minmax(0,1fr),180px,160px]">
            <el-input v-model="classFilters.keyword" clearable placeholder="分类编码 / 分类名称" @keyup.enter="loadProjectClasses">
              <template #append><el-button :icon="Search" @click="loadProjectClasses" /></template>
            </el-input>
            <div />
            <el-select v-model="classFilters.status" clearable placeholder="启用状态" @change="loadProjectClasses">
              <el-option v-for="item in meta.statusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
            <div class="flex justify-end">
              <el-button :icon="RefreshRight" @click="resetClassFilters">重置</el-button>
            </div>
          </div>
        </el-card>

        <el-card class="!rounded-3xl !shadow-sm">
          <el-table v-loading="loadingClasses" :data="projectClasses" style="width: 100%">
            <el-table-column prop="project_class_code" label="分类编码" min-width="180" />
            <el-table-column prop="project_class_name" label="分类名称" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="已挂项目" width="120">
              <template #default="{ row }">
                <el-tag :type="row.has_projects ? 'warning' : 'success'" effect="plain">{{ row.has_projects ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updated_at" label="更新时间" min-width="180" />
            <el-table-column label="操作" min-width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="canEdit" link type="primary" @click="openClassDialog('edit', row)">编辑</el-button>
                <el-button
                  v-if="canDisable"
                  link
                  :type="row.status === 1 ? 'warning' : 'success'"
                  @click="toggleProjectClassStatus(row)"
                >
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="项目档案" name="projects">
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),minmax(0,1fr),220px,160px,160px,160px]">
            <el-input v-model="projectFilters.keyword" clearable placeholder="项目编码 / 项目名称" @keyup.enter="loadProjects">
              <template #append><el-button :icon="Search" @click="loadProjects" /></template>
            </el-input>
            <div />
            <el-select v-model="projectFilters.projectClassCode" clearable placeholder="项目分类" @change="loadProjects">
              <el-option v-for="item in meta.projectClassOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="projectFilters.status" clearable placeholder="启用状态" @change="loadProjects">
              <el-option v-for="item in meta.statusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
            <el-select v-model="projectFilters.bclose" clearable placeholder="封存状态" @change="loadProjects">
              <el-option v-for="item in meta.closeStatusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
            </el-select>
            <div class="flex justify-end">
              <el-button :icon="RefreshRight" @click="resetProjectFilters">重置</el-button>
            </div>
          </div>
        </el-card>

        <el-card class="!rounded-3xl !shadow-sm">
          <el-table v-loading="loadingProjects" :data="projects" style="width: 100%">
            <el-table-column prop="citemcode" label="项目编码" min-width="160" />
            <el-table-column prop="citemname" label="项目名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="project_class_name" label="项目分类" min-width="180" show-overflow-tooltip />
            <el-table-column label="启用状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="封存状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.bclose === 1 ? 'warning' : 'success'" effect="plain">{{ row.bclose === 1 ? '已封存' : '未封存' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="d_end_date" label="结束日期" min-width="140" />
            <el-table-column prop="updated_at" label="更新时间" min-width="180" />
            <el-table-column label="操作" min-width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openProjectDialog('detail', row)">详情</el-button>
                <el-button v-if="canEdit" link type="primary" @click="openProjectDialog('edit', row)">编辑</el-button>
                <el-button
                  v-if="canDisable"
                  link
                  :type="row.status === 1 ? 'warning' : 'success'"
                  @click="toggleProjectStatus(row)"
                >
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button
                  v-if="canClose"
                  link
                  :type="row.bclose === 1 ? 'success' : 'warning'"
                  @click="toggleProjectClose(row)"
                >
                  {{ row.bclose === 1 ? '解封' : '封存' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="classDialogVisible" :title="classDialogTitle" width="560px" destroy-on-close>
      <el-form label-position="top" class="grid grid-cols-1 gap-4">
        <el-form-item label="所属公司" class="!mb-0">
          <el-input :model-value="currentCompanyName || currentCompanyId || ''" disabled />
        </el-form-item>
        <el-form-item label="分类编码" class="!mb-0">
          <el-input v-model="classForm.project_class_code" :disabled="classDialogMode === 'edit'" maxlength="2" placeholder="请输入2位数字分类编码" />
        </el-form-item>
        <el-form-item label="分类名称" class="!mb-0">
          <el-input v-model="classForm.project_class_name" placeholder="请输入项目分类名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeClassDialog">取消</el-button>
          <el-button type="primary" :loading="savingClass" @click="saveProjectClass">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="projectDialogVisible" :title="projectDialogTitle" width="720px" destroy-on-close>
      <div class="space-y-4">
        <div class="grid grid-cols-1 gap-4 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-4 xl:grid-cols-4">
          <div>
            <div class="text-xs text-slate-500">所属公司</div>
            <div class="mt-1 font-semibold text-slate-800">{{ currentCompanyName || currentCompanyId || '未选择' }}</div>
          </div>
          <div>
            <div class="text-xs text-slate-500">启用状态</div>
            <div class="mt-1 font-semibold text-slate-800">{{ projectForm.status === 1 ? '启用' : '停用' }}</div>
          </div>
          <div>
            <div class="text-xs text-slate-500">封存状态</div>
            <div class="mt-1 font-semibold text-slate-800">{{ projectForm.bclose === 1 ? '已封存' : '未封存' }}</div>
          </div>
          <div>
            <div class="text-xs text-slate-500">凭证引用</div>
            <div class="mt-1 font-semibold text-slate-800">{{ projectForm.referenced_by_voucher ? '已引用' : '未引用' }}</div>
          </div>
        </div>

        <el-form label-position="top" class="grid grid-cols-1 gap-4 xl:grid-cols-2">
          <el-form-item label="项目编码" class="!mb-0">
            <el-input v-model="projectForm.citemcode" :disabled="projectDialogMode !== 'create'" maxlength="6" placeholder="请输入6位数字项目编码" />
          </el-form-item>
          <el-form-item label="项目名称" class="!mb-0">
            <el-input v-model="projectForm.citemname" :disabled="isProjectDetailMode" placeholder="请输入项目名称" />
          </el-form-item>
          <el-form-item label="项目分类" class="!mb-0">
            <el-select v-model="projectForm.citemccode" :disabled="isProjectDetailMode" placeholder="请选择项目分类">
              <el-option v-for="item in meta.projectClassOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="其它系统已使用" class="!mb-0">
            <el-input-number v-model="projectForm.iotherused" :disabled="isProjectDetailMode" :min="0" class="w-full" />
          </el-form-item>
          <el-form-item label="结束日期" class="!mb-0">
            <el-date-picker
              v-model="projectForm.d_end_date"
              type="date"
              value-format="YYYY-MM-DDTHH:mm:ss"
              format="YYYY-MM-DD"
              class="w-full"
              :disabled="isProjectDetailMode"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeProjectDialog">{{ isProjectDetailMode ? '关闭' : '取消' }}</el-button>
          <el-button v-if="!isProjectDetailMode" type="primary" :loading="savingProject" @click="saveProject">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import {
  financeArchiveApi,
  type FinanceProjectArchiveMeta,
  type FinanceProjectClassSavePayload,
  type FinanceProjectClassSummary,
  type FinanceProjectDetail,
  type FinanceProjectSavePayload,
  type FinanceProjectSummary
} from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type ClassDialogMode = 'create' | 'edit'
type ProjectDialogMode = 'create' | 'edit' | 'detail'

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const financeCompany = useFinanceCompanyStore()
const meta = reactive<FinanceProjectArchiveMeta>({ statusOptions: [], closeStatusOptions: [], projectClassOptions: [] })
const activeTab = ref<'classes' | 'projects'>('classes')
const loadingClasses = ref(false)
const loadingProjects = ref(false)
const savingClass = ref(false)
const savingProject = ref(false)
const projectClasses = ref<FinanceProjectClassSummary[]>([])
const projects = ref<FinanceProjectSummary[]>([])
const classDialogVisible = ref(false)
const projectDialogVisible = ref(false)
const classDialogMode = ref<ClassDialogMode>('create')
const projectDialogMode = ref<ProjectDialogMode>('detail')
const editingProjectClassCode = ref('')
const editingProjectCode = ref('')
const COMPANY_SWITCH_GUARD_KEY = 'finance-project-archive'
let guardRegistered = false

const classFilters = reactive({
  keyword: '',
  status: undefined as number | undefined
})
const projectFilters = reactive({
  keyword: '',
  projectClassCode: '',
  status: undefined as number | undefined,
  bclose: undefined as number | undefined
})

const classForm = reactive<FinanceProjectClassSavePayload>({
  project_class_code: '',
  project_class_name: ''
})

const projectForm = reactive<FinanceProjectDetail>(createDefaultProjectForm())

const canCreate = computed(() => hasPermission('finance:archives:projects:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:projects:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:projects:disable', permissionCodes.value))
const canClose = computed(() => hasPermission('finance:archives:projects:close', permissionCodes.value))
const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const classDialogTitle = computed(() => (classDialogMode.value === 'create' ? '新建项目分类' : '编辑项目分类'))
const projectDialogTitle = computed(() => {
  if (projectDialogMode.value === 'create') return '新建项目档案'
  if (projectDialogMode.value === 'edit') return '编辑项目档案'
  return '项目档案详情'
})
const isProjectDetailMode = computed(() => projectDialogMode.value === 'detail')

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
      return
    }
    if (companyId !== previousCompanyId) {
      closeClassDialog()
      closeProjectDialog()
    }
    await loadMeta()
    await loadProjectClasses()
    await loadProjects()
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

async function loadProjectClasses() {
  if (!currentCompanyId.value) {
    projectClasses.value = []
    return
  }
  loadingClasses.value = true
  try {
    const res = await financeArchiveApi.listProjectClasses({
      companyId: currentCompanyId.value,
      keyword: classFilters.keyword.trim() || undefined,
      status: classFilters.status
    })
    projectClasses.value = res.data || []
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载项目分类失败'))
  } finally {
    loadingClasses.value = false
  }
}

async function loadProjects() {
  if (!currentCompanyId.value) {
    projects.value = []
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
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载项目档案失败'))
  } finally {
    loadingProjects.value = false
  }
}

function resetClassFilters() {
  classFilters.keyword = ''
  classFilters.status = undefined
  void loadProjectClasses()
}

function resetProjectFilters() {
  projectFilters.keyword = ''
  projectFilters.projectClassCode = ''
  projectFilters.status = undefined
  projectFilters.bclose = undefined
  void loadProjects()
}

function reloadCurrentTab() {
  if (activeTab.value === 'classes') {
    void loadProjectClasses()
    return
  }
  void loadProjects()
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
  if (!classForm.project_class_code.trim()) {
    ElMessage.warning('项目分类编码不能为空')
    return
  }
  if (!/^\d{2}$/.test(classForm.project_class_code.trim())) {
    ElMessage.warning('项目分类编码必须为2位数字文本')
    return
  }
  if (!classForm.project_class_name.trim()) {
    ElMessage.warning('项目分类名称不能为空')
    return
  }
  savingClass.value = true
  try {
    const payload = {
      project_class_code: classForm.project_class_code.trim(),
      project_class_name: classForm.project_class_name.trim()
    }
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
    await ElMessageBox.confirm(`确认${actionText}项目分类 ${row.project_class_code} - ${row.project_class_name} 吗？`, `${actionText}项目分类`, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
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
  projectDialogMode.value = mode
  editingProjectCode.value = row?.citemcode || ''
  if (!row) {
    Object.assign(projectForm, createDefaultProjectForm())
    projectDialogVisible.value = true
    return
  }
  void loadProjectDetail(row.citemcode, mode)
}

async function loadProjectDetail(projectCode: string, mode: ProjectDialogMode) {
  if (!currentCompanyId.value) return
  projectDialogMode.value = mode
  projectDialogVisible.value = true
  try {
    const res = await financeArchiveApi.getProjectDetail(currentCompanyId.value, projectCode)
    Object.assign(projectForm, createDefaultProjectForm(), res.data)
  } catch (error: unknown) {
    projectDialogVisible.value = false
    ElMessage.error(resolveErrorMessage(error, '加载项目档案详情失败'))
  }
}

function closeProjectDialog() {
  projectDialogVisible.value = false
  editingProjectCode.value = ''
  Object.assign(projectForm, createDefaultProjectForm())
}

async function saveProject() {
  if (!currentCompanyId.value) {
    ElMessage.warning('当前财务公司缺失，无法保存项目档案')
    return
  }
  if (!String(projectForm.citemcode || '').trim()) {
    ElMessage.warning('项目编码不能为空')
    return
  }
  if (!/^\d{6}$/.test(String(projectForm.citemcode || '').trim())) {
    ElMessage.warning('项目编码必须为6位数字文本')
    return
  }
  if (!String(projectForm.citemname || '').trim()) {
    ElMessage.warning('项目名称不能为空')
    return
  }
  if (!String(projectForm.citemccode || '').trim()) {
    ElMessage.warning('项目分类不能为空')
    return
  }
  if (!/^\d{2}$/.test(String(projectForm.citemccode || '').trim())) {
    ElMessage.warning('项目分类编码必须为2位数字文本')
    return
  }
  savingProject.value = true
  try {
    const payload = buildProjectPayload()
    if (projectDialogMode.value === 'create') {
      await financeArchiveApi.createProject(currentCompanyId.value, payload)
      ElMessage.success('项目档案创建成功')
    } else {
      await financeArchiveApi.updateProject(currentCompanyId.value, editingProjectCode.value, payload)
      ElMessage.success('项目档案更新成功')
    }
    closeProjectDialog()
    await loadProjects()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存项目档案失败'))
  } finally {
    savingProject.value = false
  }
}

async function toggleProjectStatus(row: FinanceProjectSummary) {
  if (!currentCompanyId.value) return
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}项目 ${row.citemcode} - ${row.citemname} 吗？`, `${actionText}项目`, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
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
    await ElMessageBox.confirm(`确认${actionText}项目 ${row.citemcode} - ${row.citemname} 吗？`, `${actionText}项目`, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
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

function buildProjectPayload(): FinanceProjectSavePayload {
  return {
    citemcode: String(projectForm.citemcode || '').trim(),
    citemname: String(projectForm.citemname || '').trim(),
    citemccode: String(projectForm.citemccode || '').trim(),
    iotherused: normalizeNumber(projectForm.iotherused, 0),
    d_end_date: trimString(projectForm.d_end_date)
  }
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
  if (!classDialogVisible.value && !projectDialogVisible.value) return true
  try {
    await ElMessageBox.confirm('切换公司会关闭当前项目档案弹窗，是否继续？', '切换公司', {
      type: 'warning',
      confirmButtonText: '继续',
      cancelButtonText: '取消'
    })
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
  return {
    citemcode: '',
    citemname: '',
    citemccode: '',
    bclose: 0,
    status: 1,
    iotherused: 0,
    d_end_date: '',
    referenced_by_voucher: false
  }
}

defineExpose({
  activeTab,
  meta,
  classFilters,
  projectFilters,
  projectForm,
  projectClasses,
  projects,
  loadProjectClasses,
  loadProjects,
  openClassDialog,
  openProjectDialog,
  buildProjectPayload,
  toggleProjectStatus,
  toggleProjectClose
})
</script>

<style scoped>
:deep(.project-archive-tabs .el-tabs__header) {
  margin-bottom: 0;
}
</style>
