<template>
  <div class="space-y-4 finance-system-management-page">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">财务系统管理</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前财务公司</span>
            <strong>{{ currentCompanyDisplay || '未设置' }}</strong>
          </div>
          <div class="inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-sm text-emerald-700">
            <span class="font-semibold">已建账套</span>
            <strong>{{ accountSets.length }}</strong>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <el-button :loading="loading" @click="loadPageData">刷新</el-button>
          <el-button type="primary" :disabled="!canCreate" @click="openCreateWizard">新建账套</el-button>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div class="space-y-2">
          <h2 class="text-lg font-semibold text-slate-800">账套列表</h2>
          <p class="text-sm text-slate-500">按公司查看已创建账套、模板来源、编码规则与最近任务状态。</p>
        </div>
        <div class="flex flex-wrap items-center gap-2 text-sm text-slate-500">
          <span class="rounded-full bg-slate-100 px-3 py-1">模板：2007 企业会计制度 / 2013 小企业会计准则</span>
          <span class="rounded-full bg-slate-100 px-3 py-1">任务：异步创建</span>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="accountSets" style="width: 100%">
        <el-table-column prop="companyName" label="目标公司" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex flex-col gap-1">
              <span class="font-medium text-slate-700">{{ formatCompanyDisplay(row) }}</span>
              <span class="text-xs text-slate-400">{{ row.companyId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="enabledYearMonth" label="启用年月" min-width="110" />
        <el-table-column prop="templateName" label="账套模板" min-width="180" show-overflow-tooltip />
        <el-table-column prop="supervisorName" label="账套主管" min-width="120" show-overflow-tooltip />
        <el-table-column label="创建方式" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.createMode === 'REFERENCE' ? 'warning' : 'success'" effect="plain">
              {{ row.createMode === 'REFERENCE' ? '参照账套' : '空白账套' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subjectCodeScheme" label="科目规则" min-width="120" />
        <el-table-column prop="subjectCount" label="科目数" min-width="90" />
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="resolveStatusTagType(row.status)" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近任务" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex flex-col gap-1 text-sm">
              <span class="text-slate-700">{{ row.lastTaskNo || '暂无任务' }}</span>
              <span class="text-xs text-slate-400">{{ row.lastTaskMessage || '--' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      </el-table>
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      size="960px"
      destroy-on-close
      :close-on-click-modal="!submitting"
      :show-close="!submitting"
      title="新建账套"
      @closed="handleDrawerClosed"
    >
      <div class="space-y-6">
        <el-steps :active="currentStep" finish-status="success" simple>
          <el-step title="创建方式" />
          <el-step title="公司与期间" />
          <el-step title="模板与主管" />
          <el-step title="编码规则" />
          <el-step title="确认创建" />
          <el-step title="任务结果" />
        </el-steps>

        <section v-if="currentStep === 0" class="space-y-5">
          <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
            <div class="space-y-4">
              <div>
                <h3 class="text-base font-semibold text-slate-800">选择创建方式</h3>
                <p class="mt-1 text-sm text-slate-500">空白账套可自行选择模板与编码规则，参照账套会复制来源账套的模板、完整科目体系和编码规则。</p>
              </div>
              <el-radio-group v-model="wizardForm.createMode">
                <el-radio label="BLANK">新建空白账套</el-radio>
                <el-radio label="REFERENCE">参照已有账套</el-radio>
              </el-radio-group>
            </div>
          </div>

          <el-card v-if="wizardForm.createMode === 'REFERENCE'" class="!rounded-2xl !shadow-sm">
            <el-form label-position="top">
              <el-form-item label="参照账套">
                <el-select v-model="wizardForm.referenceCompanyId" clearable filterable placeholder="请选择参照账套公司" class="w-full">
                  <el-option
                    v-for="item in referenceOptions"
                    :key="item.companyId"
                    :label="item.label"
                    :value="item.companyId"
                  />
                </el-select>
              </el-form-item>
            </el-form>
            <div v-if="selectedReferenceOption" class="rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-700">
              将复制 <strong>{{ selectedReferenceOption.templateName }}</strong> 模板、完整科目体系和编码规则
              <strong>{{ selectedReferenceOption.subjectCodeScheme }}</strong>。
            </div>
          </el-card>
        </section>

        <section v-else-if="currentStep === 1" class="space-y-4">
          <el-form label-position="top" class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <el-form-item label="目标公司" class="!mb-0">
              <el-select v-model="wizardForm.targetCompanyId" filterable placeholder="请选择公司" class="w-full">
                <el-option
                  v-for="item in companyOptions"
                  :key="item.companyId"
                  :label="item.label"
                  :value="item.companyId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="启用会计期间" class="!mb-0">
              <el-date-picker
                v-model="wizardForm.enabledYearMonth"
                type="month"
                value-format="YYYY-MM"
                format="YYYY-MM"
                class="w-full"
                placeholder="请选择启用年月"
              />
            </el-form-item>
          </el-form>
        </section>

        <section v-else-if="currentStep === 2" class="space-y-4">
          <el-form label-position="top" class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <el-form-item label="账套模板" class="!mb-0">
              <el-select
                v-model="wizardForm.templateCode"
                filterable
                placeholder="请选择账套模板"
                class="w-full"
                :disabled="isReferenceMode"
              >
                <el-option
                  v-for="item in templateOptions"
                  :key="item.templateCode"
                  :label="`${item.templateName}（一级 ${item.level1SubjectCount} / 常用二级 ${item.commonSubjectCount}）`"
                  :value="item.templateCode"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="账套主管" class="!mb-0">
              <el-select v-model="wizardForm.supervisorUserId" filterable placeholder="请选择账套主管" class="w-full">
                <el-option
                  v-for="item in supervisorOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="Number(item.value)"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <div v-if="isReferenceMode && selectedReferenceOption" class="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
            参照模式下，模板锁定为 <strong>{{ selectedReferenceOption.templateName }}</strong>，账套主管仍可按目标公司重新指定。
          </div>
        </section>

        <section v-else-if="currentStep === 3" class="space-y-4">
          <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-[180px,minmax(0,1fr)] md:items-center">
              <div>
                <h3 class="text-base font-semibold text-slate-800">科目编码规则</h3>
                <p class="mt-1 text-sm text-slate-500">首版仅支持会计科目分级段长规则，第一级固定为 4 位。</p>
              </div>
              <div class="flex flex-col gap-3">
                <el-input v-model="wizardForm.subjectCodeScheme" :disabled="isReferenceMode" placeholder="例如 4-2-2-2" />
                <div class="rounded-2xl bg-sky-50 px-4 py-3 text-sm text-sky-700">
                  {{ isReferenceMode ? '参照模式下编码规则由来源账套继承，不可修改。' : '建议使用 4-2-2-2，后续会按模板层级自动生成公司级会计科目编码。' }}
                </div>
              </div>
            </div>
          </div>
        </section>

        <section v-else-if="currentStep === 4" class="space-y-4">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div v-for="item in confirmItems" :key="item.label" class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
              <p class="text-sm text-slate-500">{{ item.label }}</p>
              <p class="mt-2 text-base font-semibold text-slate-800">{{ item.value }}</p>
            </div>
          </div>
          <div class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            点击“开始创建”后将提交异步任务。账套、编码规则和公司级会计科目会在后台创建完成后统一激活。
          </div>
        </section>

        <section v-else class="space-y-4">
          <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
            <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <h3 class="text-base font-semibold text-slate-800">任务执行中</h3>
                <p class="mt-1 text-sm text-slate-500">任务号：{{ taskStatus?.taskNo || '--' }}</p>
              </div>
              <el-tag :type="resolveTaskTagType(taskStatus?.status)">{{ resolveTaskLabel(taskStatus?.status) }}</el-tag>
            </div>
            <el-progress class="mt-4" :percentage="taskStatus?.progress || 0" :status="progressStatus" />
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
              <p class="text-sm text-slate-500">结果消息</p>
              <p class="mt-2 text-base text-slate-800">{{ taskStatus?.resultMessage || '任务已提交，正在等待执行。' }}</p>
            </div>
            <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
              <p class="text-sm text-slate-500">账套状态</p>
              <p class="mt-2 text-base text-slate-800">{{ taskStatus?.accountSetStatus || 'INITIALIZING' }}</p>
            </div>
          </div>
        </section>
      </div>

      <template #footer>
        <div class="flex items-center justify-between gap-3 border-t border-slate-100 pt-4">
          <el-button :disabled="submitting || currentStep === 0" @click="handlePrev">上一步</el-button>
          <div class="flex items-center gap-2">
            <el-button @click="drawerVisible = false">{{ currentStep === 5 ? '关闭' : '取消' }}</el-button>
            <el-button
              v-if="currentStep < 4"
              type="primary"
              @click="handleNext"
            >下一步</el-button>
            <el-button
              v-else-if="currentStep === 4"
              type="primary"
              :loading="submitting"
              @click="submitCreateTask"
            >开始创建</el-button>
            <el-button
              v-else
              type="primary"
              :disabled="Boolean(taskStatus?.finished)"
              @click="refreshTaskStatus"
            >刷新状态</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  financeSystemManagementApi,
  type FinanceAccountSetCreatePayload,
  type FinanceAccountSetMeta,
  type FinanceAccountSetSummary,
  type FinanceAccountSetTaskStatus
} from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const financeCompany = useFinanceCompanyStore()
const financeCompanyState = financeCompany as typeof financeCompany & { currentCompanyLabel?: string }
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const currentStep = ref(0)
const meta = ref<FinanceAccountSetMeta | null>(null)
const accountSets = ref<FinanceAccountSetSummary[]>([])
const taskStatus = ref<FinanceAccountSetTaskStatus | null>(null)
let taskPollTimer: ReturnType<typeof window.setTimeout> | null = null

const wizardForm = reactive<FinanceAccountSetCreatePayload>({
  createMode: 'BLANK',
  referenceCompanyId: '',
  targetCompanyId: '',
  enabledYearMonth: '',
  templateCode: '',
  supervisorUserId: 0,
  subjectCodeScheme: ''
})

const canCreate = computed(() => hasPermission('finance:system_management:create', permissionCodes.value))
const isReferenceMode = computed(() => wizardForm.createMode === 'REFERENCE')
const companyOptions = computed(() => meta.value?.companyOptions || [])
const supervisorOptions = computed(() => meta.value?.supervisorOptions || [])
const templateOptions = computed(() => meta.value?.templateOptions || [])
const referenceOptions = computed(() => meta.value?.referenceOptions || [])
const selectedReferenceOption = computed(() =>
  referenceOptions.value.find((item) => item.companyId === wizardForm.referenceCompanyId) || null
)
const selectedTemplate = computed(() =>
  templateOptions.value.find((item) => item.templateCode === wizardForm.templateCode) || null
)
const currentCompanyDisplay = computed(() => financeCompanyState.currentCompanyLabel || financeCompany.currentCompanyName || financeCompany.currentCompanyId || '')
const progressStatus = computed(() => {
  if (taskStatus.value?.status === 'FAILED') {
    return 'exception'
  }
  if (taskStatus.value?.status === 'SUCCESS') {
    return 'success'
  }
  return undefined
})
const confirmItems = computed(() => [
  { label: '创建方式', value: isReferenceMode.value ? '参照已有账套' : '新建空白账套' },
  { label: '目标公司', value: resolveCompanyName(wizardForm.targetCompanyId) },
  { label: '启用会计期间', value: normalizeEnabledYearMonth(wizardForm.enabledYearMonth) || '--' },
  { label: '账套模板', value: selectedReferenceOption.value?.templateName || selectedTemplate.value?.templateName || '--' },
  { label: '账套主管', value: resolveSupervisorName(wizardForm.supervisorUserId) },
  { label: '科目编码规则', value: effectiveSubjectScheme.value },
  { label: '参照来源', value: selectedReferenceOption.value?.companyName || '无' }
])
const effectiveSubjectScheme = computed(() => {
  if (isReferenceMode.value) {
    return selectedReferenceOption.value?.subjectCodeScheme || '--'
  }
  return wizardForm.subjectCodeScheme || meta.value?.defaultSubjectCodeScheme || '--'
})

onMounted(() => {
  void loadPageData()
})

onBeforeUnmount(() => {
  stopTaskPolling()
})

async function loadPageData() {
  loading.value = true
  try {
    const [metaRes, listRes] = await Promise.all([
      financeSystemManagementApi.getMeta(),
      financeSystemManagementApi.listAccountSets()
    ])
    meta.value = metaRes.data
    accountSets.value = listRes.data || []
    if (!wizardForm.subjectCodeScheme) {
      wizardForm.subjectCodeScheme = metaRes.data.defaultSubjectCodeScheme || '4-2-2-2'
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载财务系统管理页面失败'))
  } finally {
    loading.value = false
  }
}

function openCreateWizard() {
  resetWizard()
  drawerVisible.value = true
}

function resetWizard() {
  stopTaskPolling()
  currentStep.value = 0
  taskStatus.value = null
  wizardForm.createMode = 'BLANK'
  wizardForm.referenceCompanyId = ''
  wizardForm.targetCompanyId = ''
  wizardForm.enabledYearMonth = ''
  wizardForm.templateCode = ''
  wizardForm.supervisorUserId = 0
  wizardForm.subjectCodeScheme = meta.value?.defaultSubjectCodeScheme || '4-2-2-2'
}

function handleDrawerClosed() {
  stopTaskPolling()
  submitting.value = false
}

function handlePrev() {
  if (currentStep.value > 0) {
    currentStep.value -= 1
  }
}

function handleNext() {
  if (!validateStep(currentStep.value)) {
    return
  }
  syncReferenceFields()
  currentStep.value += 1
}

function validateStep(step: number) {
  switch (step) {
    case 0:
      if (isReferenceMode.value && !wizardForm.referenceCompanyId) {
        ElMessage.warning('请选择参照账套')
        return false
      }
      return true
    case 1:
      if (!wizardForm.targetCompanyId) {
        ElMessage.warning('请选择目标公司')
        return false
      }
      if (!wizardForm.enabledYearMonth) {
        ElMessage.warning('请选择启用会计期间')
        return false
      }
      return true
    case 2:
      if (!isReferenceMode.value && !wizardForm.templateCode) {
        ElMessage.warning('请选择账套模板')
        return false
      }
      if (!wizardForm.supervisorUserId) {
        ElMessage.warning('请选择账套主管')
        return false
      }
      return true
    case 3:
      if (!isReferenceMode.value && !/^\d+(?:-\d+)*$/.test(wizardForm.subjectCodeScheme || '')) {
        ElMessage.warning('请填写正确的科目编码规则，例如 4-2-2-2')
        return false
      }
      return true
    default:
      return true
  }
}

function syncReferenceFields() {
  if (!isReferenceMode.value) {
    if (!wizardForm.subjectCodeScheme) {
      wizardForm.subjectCodeScheme = meta.value?.defaultSubjectCodeScheme || '4-2-2-2'
    }
    return
  }
  wizardForm.templateCode = selectedReferenceOption.value?.templateCode || ''
  wizardForm.subjectCodeScheme = selectedReferenceOption.value?.subjectCodeScheme || ''
}

async function submitCreateTask() {
  if (!validateStep(3)) {
    return
  }
  submitting.value = true
  syncReferenceFields()
  try {
    const res = await financeSystemManagementApi.createAccountSet(buildPayload())
    taskStatus.value = res.data
    currentStep.value = 5
    ElMessage.success('账套创建任务已提交')
    startTaskPolling(res.data.taskNo)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '提交账套创建任务失败'))
  } finally {
    submitting.value = false
  }
}

function buildPayload(): FinanceAccountSetCreatePayload {
  return {
    createMode: wizardForm.createMode,
    referenceCompanyId: isReferenceMode.value ? wizardForm.referenceCompanyId || undefined : undefined,
    targetCompanyId: wizardForm.targetCompanyId,
    enabledYearMonth: normalizeEnabledYearMonth(wizardForm.enabledYearMonth),
    templateCode: isReferenceMode.value ? undefined : wizardForm.templateCode || undefined,
    supervisorUserId: Number(wizardForm.supervisorUserId),
    subjectCodeScheme: isReferenceMode.value ? undefined : wizardForm.subjectCodeScheme || undefined
  }
}

function normalizeEnabledYearMonth(value: unknown) {
  if (typeof value === 'string') {
    return value.trim()
  }
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return formatDateToYearMonth(value)
  }
  if (value && typeof value === 'object') {
    const candidate = value as {
      format?: (pattern: string) => string
      toDate?: () => Date
      $d?: unknown
    }
    if (typeof candidate.format === 'function') {
      const formatted = candidate.format('YYYY-MM')
      return typeof formatted === 'string' ? formatted.trim() : ''
    }
    if (typeof candidate.toDate === 'function') {
      return normalizeEnabledYearMonth(candidate.toDate())
    }
    if (candidate.$d instanceof Date) {
      return formatDateToYearMonth(candidate.$d)
    }
  }
  return ''
}

function formatDateToYearMonth(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

function startTaskPolling(taskNo: string) {
  stopTaskPolling()
  const poll = async () => {
    await refreshTaskStatus(taskNo)
    if (!taskStatus.value?.finished) {
      taskPollTimer = window.setTimeout(poll, 2000)
    }
  }
  taskPollTimer = window.setTimeout(poll, 1200)
}

function stopTaskPolling() {
  if (taskPollTimer) {
    window.clearTimeout(taskPollTimer)
    taskPollTimer = null
  }
}

async function refreshTaskStatus(taskNo = taskStatus.value?.taskNo) {
  if (!taskNo) {
    return
  }
  try {
    const res = await financeSystemManagementApi.getTaskStatus(taskNo)
    taskStatus.value = res.data
    if (res.data.finished) {
      stopTaskPolling()
      await loadPageData()
    }
  } catch (error: unknown) {
    stopTaskPolling()
    ElMessage.error(resolveErrorMessage(error, '获取账套任务状态失败'))
  }
}

function resolveCompanyName(companyId?: string) {
  const match = companyOptions.value.find((item) => item.companyId === companyId)
  if (!match) {
    return companyId || '--'
  }
  return match.companyCode ? `${match.companyCode} - ${match.companyName}` : match.companyName || match.companyId
}

function resolveSupervisorName(userId?: number) {
  if (!userId) {
    return '--'
  }
  const match = supervisorOptions.value.find((item) => Number(item.value) === Number(userId))
  return match?.label || String(userId)
}

function formatCompanyDisplay(row: FinanceAccountSetSummary) {
  return row.companyCode ? `${row.companyCode} - ${row.companyName || row.companyId}` : row.companyName || row.companyId
}

function resolveStatusTagType(status?: string) {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'INITIALIZING':
      return 'warning'
    default:
      return 'info'
  }
}

function resolveTaskTagType(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'RUNNING':
      return 'warning'
    default:
      return 'info'
  }
}

function resolveTaskLabel(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return '执行成功'
    case 'FAILED':
      return '执行失败'
    case 'RUNNING':
      return '执行中'
    case 'PENDING':
      return '排队中'
    default:
      return '待执行'
  }
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

defineExpose({ accountSets, wizardForm, syncReferenceFields, buildPayload, submitCreateTask, formatCompanyDisplay })
</script>

<style scoped>
.finance-system-management-page :deep(.el-drawer__body) {
  padding-top: 20px;
}
</style>
