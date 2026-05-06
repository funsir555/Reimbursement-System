<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
    <div class="space-y-4">
      <div class="grid grid-cols-1 gap-4 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-4 xl:grid-cols-4">
        <div><div class="text-xs text-slate-500">所属公司</div><div class="mt-1 font-semibold text-slate-800">{{ companyName || companyId || '未选择' }}</div></div>
        <div><div class="text-xs text-slate-500">启用状态</div><div class="mt-1 font-semibold text-slate-800">{{ projectForm.status === 1 ? '启用' : '停用' }}</div></div>
        <div><div class="text-xs text-slate-500">封存状态</div><div class="mt-1 font-semibold text-slate-800">{{ projectForm.bclose === 1 ? '已封存' : '未封存' }}</div></div>
        <div><div class="text-xs text-slate-500">凭证引用</div><div class="mt-1 font-semibold text-slate-800">{{ projectForm.referenced_by_voucher ? '已引用' : '未引用' }}</div></div>
      </div>

      <el-form label-position="top" class="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <el-form-item label="项目编码" class="!mb-0"><el-input v-model="projectForm.citemcode" :disabled="dialogMode !== 'create'" maxlength="6" placeholder="请输入1-6位数字项目编码" /></el-form-item>
        <el-form-item label="项目名称" class="!mb-0"><el-input v-model="projectForm.citemname" :disabled="isDetailMode" placeholder="请输入项目名称" /></el-form-item>
        <el-form-item label="项目分类" class="!mb-0">
          <el-select v-model="projectForm.citemccode" :disabled="isDetailMode" placeholder="请选择项目分类">
            <el-option v-for="item in projectClassOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="其它系统已使用" class="!mb-0"><el-input-number v-model="projectForm.iotherused" :disabled="isDetailMode" :min="0" class="w-full" /></el-form-item>
        <el-form-item label="结束日期" class="!mb-0">
          <el-date-picker v-model="projectForm.d_end_date" type="date" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD" class="w-full" :disabled="isDetailMode" />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="closeProjectDialog">{{ isDetailMode ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!isDetailMode" type="primary" :loading="savingProject" @click="saveProject">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  financeArchiveApi,
  type FinanceProjectDetail,
  type FinanceProjectSavePayload
} from '@/api'

type ProjectDialogMode = 'create' | 'edit' | 'detail'
type ProjectClassOption = {
  value: string
  label: string
}

const props = withDefaults(defineProps<{
  companyId?: string
  companyName?: string
  projectClassOptions?: ProjectClassOption[]
}>(), {
  companyId: '',
  companyName: '',
  projectClassOptions: () => []
})

const emit = defineEmits<{
  saved: [projectCode: string]
  closed: []
}>()

const dialogVisible = ref(false)
const dialogMode = ref<ProjectDialogMode>('detail')
const editingProjectCode = ref('')
const savingProject = ref(false)
const projectForm = reactive<FinanceProjectDetail>(createDefaultProjectForm())

const isDetailMode = computed(() => dialogMode.value === 'detail')
const dialogTitle = computed(() => dialogMode.value === 'create' ? '新建项目档案' : dialogMode.value === 'edit' ? '编辑项目档案' : '项目档案详情')

function openCreateDialog(initialProjectClassCode = '') {
  if (!props.companyId) {
    ElMessage.warning('当前财务公司缺失，无法维护项目档案')
    return
  }
  dialogMode.value = 'create'
  editingProjectCode.value = ''
  Object.assign(projectForm, createDefaultProjectForm(), {
    citemccode: initialProjectClassCode || ''
  })
  dialogVisible.value = true
}

async function openEditDialog(projectCode: string) {
  await openDialogWithDetail(projectCode, 'edit')
}

async function openDetailDialog(projectCode: string) {
  await openDialogWithDetail(projectCode, 'detail')
}

async function openDialogWithDetail(projectCode: string, mode: ProjectDialogMode) {
  if (!props.companyId) {
    ElMessage.warning('当前财务公司缺失，无法维护项目档案')
    return
  }
  dialogMode.value = mode
  editingProjectCode.value = projectCode
  dialogVisible.value = true
  try {
    const res = await financeArchiveApi.getProjectDetail(props.companyId, projectCode)
    Object.assign(projectForm, createDefaultProjectForm(), res.data)
  } catch (error: unknown) {
    dialogVisible.value = false
    ElMessage.error(resolveErrorMessage(error, '加载项目档案详情失败'))
  }
}

function closeProjectDialog() {
  dialogVisible.value = false
  editingProjectCode.value = ''
  Object.assign(projectForm, createDefaultProjectForm())
  emit('closed')
}

async function saveProject() {
  if (!props.companyId) return ElMessage.warning('当前财务公司缺失，无法保存项目档案')
  if (!String(projectForm.citemcode || '').trim()) return ElMessage.warning('项目编码不能为空')
  if (!/^\d{1,6}$/.test(String(projectForm.citemcode || '').trim())) return ElMessage.warning('项目编码必须为1-6位数字文本')
  if (!String(projectForm.citemname || '').trim()) return ElMessage.warning('项目名称不能为空')
  if (!String(projectForm.citemccode || '').trim()) return ElMessage.warning('项目分类不能为空')
  if (!/^\d{1,2}$/.test(String(projectForm.citemccode || '').trim())) return ElMessage.warning('项目分类编码必须为1-2位数字文本')
  savingProject.value = true
  try {
    const payload = buildProjectPayload()
    if (dialogMode.value === 'create') {
      const res = await financeArchiveApi.createProject(props.companyId, payload)
      const createdCode = String(res.data?.citemcode || payload.citemcode || '').trim()
      ElMessage.success('项目档案创建成功')
      emit('saved', createdCode)
    } else {
      await financeArchiveApi.updateProject(props.companyId, editingProjectCode.value, payload)
      ElMessage.success('项目档案更新成功')
      emit('saved', editingProjectCode.value)
    }
    closeProjectDialog()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存项目档案失败'))
  } finally {
    savingProject.value = false
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

defineExpose({
  dialogVisible,
  projectForm,
  openCreateDialog,
  openEditDialog,
  openDetailDialog,
  saveProject,
  buildProjectPayload,
  closeProjectDialog
})
</script>
