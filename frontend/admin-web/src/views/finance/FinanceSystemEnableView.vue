<template>
  <div class="space-y-4 finance-system-enable-page">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">系统启用</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前财务公司</span>
            <strong>{{ currentCompanyDisplay || '未设置' }}</strong>
          </div>
          <div class="inline-flex items-center gap-2 rounded-full bg-amber-50 px-3 py-1.5 text-sm text-amber-700">
            <span class="font-semibold">账套状态</span>
            <strong>{{ financeCompany.currentCompanyHasActiveAccountSet ? '已建账套' : '未创建账套' }}</strong>
          </div>
        </div>
        <el-button :loading="loading" @click="refreshModules">刷新状态</el-button>
      </div>
    </section>

    <el-alert
      v-if="!financeCompany.currentCompanyHasActiveAccountSet"
      type="info"
      :closable="false"
      title="当前公司未创建账套"
      description="系统启用按当前财务公司工作。请先完成账套创建后，再维护模块启停状态。"
    />

    <div class="grid gap-4 xl:grid-cols-2">
      <el-card
        v-for="moduleItem in moduleCards"
        :key="moduleItem.moduleCode"
        class="!rounded-3xl !shadow-sm"
      >
        <div class="flex h-full flex-col gap-4">
          <div class="flex items-start justify-between gap-4">
            <div class="space-y-2">
              <div class="flex flex-wrap items-center gap-2">
                <h2 class="text-lg font-semibold text-slate-800">{{ moduleItem.moduleName }}</h2>
                <el-tag :type="moduleItem.enabled ? 'success' : 'info'" effect="light">
                  {{ moduleItem.enabled ? '已启用' : '未启用' }}
                </el-tag>
                <el-tag :type="moduleItem.implemented ? 'primary' : 'warning'" effect="plain">
                  {{ moduleItem.implemented ? '已落地' : '建设中' }}
                </el-tag>
              </div>
              <p class="text-sm leading-7 text-slate-500">
                {{ resolveModuleHint(moduleItem) }}
              </p>
            </div>
            <el-switch
              :model-value="moduleItem.enabled"
              :disabled="!canToggle(moduleItem) || togglingCode === moduleItem.moduleCode"
              inline-prompt
              active-text="开"
              inactive-text="关"
              @change="(value: string | number | boolean) => handleToggle(moduleItem, Boolean(value))"
            />
          </div>

          <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
            <div class="font-medium text-slate-700">关闭规则</div>
            <div class="mt-1">
              {{ moduleItem.implemented ? '若已存在业务数据，将阻止关闭。' : '当前模块首版仅做静态占位，不支持启停。' }}
            </div>
            <div v-if="moduleItem.blockedMessage" class="mt-2 text-amber-700">
              {{ moduleItem.blockedMessage }}
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <el-button
              v-if="showBackupButton(moduleItem)"
              :loading="backupingCode === moduleItem.moduleCode"
              :disabled="!canBackup(moduleItem)"
              @click="handleBackup(moduleItem)"
            >
              备份数据
            </el-button>
            <el-button
              v-if="showBackupRecordButton(moduleItem)"
              :disabled="!canViewBackupRecords(moduleItem)"
              @click="openBackupRecords(moduleItem)"
            >
              备份记录
            </el-button>
            <el-button
              v-if="showClearButton(moduleItem)"
              type="danger"
              plain
              :loading="clearingCode === moduleItem.moduleCode"
              :disabled="!canClear(moduleItem)"
              @click="handleClear(moduleItem)"
            >
              清除数据
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="backupRecordDialogVisible"
      width="920px"
      title="备份记录"
      destroy-on-close
    >
      <div class="mb-4 flex flex-wrap items-center gap-2 text-sm text-slate-500">
        <span>模块：</span>
        <strong class="text-slate-700">{{ backupRecordModuleName || '--' }}</strong>
      </div>

      <el-table
        v-loading="backupRecordLoading"
        :data="backupRecords"
        style="width: 100%"
        max-height="420"
      >
        <el-table-column prop="backupStartedAt" label="备份时间" min-width="170" />
        <el-table-column prop="backupFilePath" label="备份路径" min-width="260" show-overflow-tooltip />
        <el-table-column prop="backupFileName" label="文件名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="backupUserName" label="备份人" min-width="120" />
        <el-table-column prop="backupStatus" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.backupStatus === 'SUCCESS' ? 'success' : 'danger'" effect="plain">
              {{ row.backupStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="!backupRecordLoading && backupRecords.length === 0"
        class="mt-4 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500"
      >
        当前暂无备份记录。
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  financeSystemManagementApi,
  type FinanceModuleBackupRecord,
  type FinanceModuleEnableSummary
} from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'

const financeCompany = useFinanceCompanyStore()
const financeCompanyState = financeCompany as typeof financeCompany & { currentCompanyLabel?: string }
const loading = ref(false)
const togglingCode = ref('')
const backupingCode = ref('')
const clearingCode = ref('')
const backupRecordLoading = ref(false)
const backupRecordDialogVisible = ref(false)
const backupRecordModuleName = ref('')
const moduleCards = ref<FinanceModuleEnableSummary[]>([])
const backupRecords = ref<FinanceModuleBackupRecord[]>([])

const currentCompanyDisplay = computed(() =>
  financeCompanyState.currentCompanyLabel || financeCompany.currentCompanyName || financeCompany.currentCompanyId || ''
)

function resetCards() {
  moduleCards.value = []
}

async function refreshModules() {
  if (!financeCompany.currentCompanyId || !financeCompany.currentCompanyHasActiveAccountSet) {
    resetCards()
    return
  }
  loading.value = true
  try {
    const res = await financeSystemManagementApi.getModuleEnables(financeCompany.currentCompanyId)
    moduleCards.value = Array.isArray(res.data.modules) ? res.data.modules : []
  } finally {
    loading.value = false
  }
}

function canToggle(moduleItem: FinanceModuleEnableSummary) {
  return financeCompany.currentCompanyHasActiveAccountSet && moduleItem.toggleAllowed
}

function canBackup(moduleItem: FinanceModuleEnableSummary) {
  return financeCompany.currentCompanyHasActiveAccountSet && Boolean(moduleItem.backupAllowed)
}

function canViewBackupRecords(moduleItem: FinanceModuleEnableSummary) {
  return financeCompany.currentCompanyHasActiveAccountSet && Boolean(moduleItem.backupRecordAllowed)
}

function canClear(moduleItem: FinanceModuleEnableSummary) {
  return financeCompany.currentCompanyHasActiveAccountSet && Boolean(moduleItem.clearAllowed)
}

function showBackupButton(moduleItem: FinanceModuleEnableSummary) {
  return true
}

function showBackupRecordButton(moduleItem: FinanceModuleEnableSummary) {
  return true
}

function showClearButton(moduleItem: FinanceModuleEnableSummary) {
  return moduleItem.moduleCode !== 'GENERAL_LEDGER'
}

function resolveModuleHint(moduleItem: FinanceModuleEnableSummary) {
  if (!moduleItem.implemented) {
    return '当前模块仅展示占位状态，后续真实业务落地后再开放启停。'
  }
  if (moduleItem.enabled) {
    return '当前账套已启用该模块，相关导航和后端能力可正常使用。'
  }
  return '当前账套未启用该模块。若用户有导航权限，点击入口时会提示“系统未启用”。'
}

async function handleBackup(moduleItem: FinanceModuleEnableSummary) {
  if (!canBackup(moduleItem)) {
    return
  }
  backupingCode.value = moduleItem.moduleCode
  try {
    await financeSystemManagementApi.backupModuleData({
      companyId: financeCompany.currentCompanyId,
      moduleCode: moduleItem.moduleCode
    })
    ElMessage.success(`${moduleItem.moduleName}备份完成`)
  } catch (error) {
    const message = error instanceof Error && error.message ? error.message : '模块备份失败'
    ElMessage.error(message)
  } finally {
    backupingCode.value = ''
  }
}

async function openBackupRecords(moduleItem: FinanceModuleEnableSummary) {
  if (!canViewBackupRecords(moduleItem)) {
    return
  }
  backupRecordDialogVisible.value = true
  backupRecordModuleName.value = moduleItem.moduleName
  backupRecordLoading.value = true
  try {
    const res = await financeSystemManagementApi.getModuleBackupRecords(
      financeCompany.currentCompanyId,
      moduleItem.moduleCode
    )
    backupRecords.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    backupRecords.value = []
    const message = error instanceof Error && error.message ? error.message : '获取备份记录失败'
    ElMessage.error(message)
  } finally {
    backupRecordLoading.value = false
  }
}

async function handleClear(moduleItem: FinanceModuleEnableSummary) {
  if (!canClear(moduleItem)) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认清除${moduleItem.moduleName}当前账套的全部数据吗？`,
      '清除数据',
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  clearingCode.value = moduleItem.moduleCode
  try {
    const res = await financeSystemManagementApi.clearModuleData({
      companyId: financeCompany.currentCompanyId,
      moduleCode: moduleItem.moduleCode
    })
    moduleCards.value = Array.isArray(res.data.modules) ? res.data.modules : []
    await financeCompany.refreshContext(financeCompany.currentCompanyId)
    ElMessage.success(`${moduleItem.moduleName}数据已清除`)
  } catch (error) {
    const message = error instanceof Error && error.message ? error.message : '模块数据清除失败'
    ElMessage.error(message)
  } finally {
    clearingCode.value = ''
  }
}

async function handleToggle(moduleItem: FinanceModuleEnableSummary, enabled: boolean) {
  if (!canToggle(moduleItem)) {
    return
  }
  togglingCode.value = moduleItem.moduleCode
  try {
    const res = await financeSystemManagementApi.toggleModuleEnable({
      companyId: financeCompany.currentCompanyId,
      moduleCode: moduleItem.moduleCode,
      enabled
    })
    moduleCards.value = Array.isArray(res.data.modules) ? res.data.modules : []
    await financeCompany.refreshContext(financeCompany.currentCompanyId)
    ElMessage.success(enabled ? `${moduleItem.moduleName}已启用` : `${moduleItem.moduleName}已关闭`)
  } catch (error) {
    const message = error instanceof Error && error.message ? error.message : '系统启停更新失败'
    ElMessage.error(message)
  } finally {
    togglingCode.value = ''
  }
}

watch(
  () => [financeCompany.currentCompanyId, financeCompany.currentCompanyHasActiveAccountSet] as const,
  () => {
    void refreshModules()
  },
  { immediate: true }
)
</script>
