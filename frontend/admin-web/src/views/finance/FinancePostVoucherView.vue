<template>
  <div class="finance-post-voucher-page">
    <el-card class="fpv-shell fpv-shell--toolbar" shadow="never">
      <div class="fpv-toolbar">
        <div class="fpv-summary">
          <div class="fpv-summary__item">
            <span>当前公司</span>
            <strong>{{ currentCompanyName || '未设置' }}</strong>
          </div>
          <div class="fpv-summary__item">
            <span>当前期间</span>
            <strong>{{ currentPeriodLabel }}</strong>
          </div>
          <div class="fpv-summary__item">
            <span>本期状态</span>
            <strong>{{ meta?.statusLabel || '未记账' }}</strong>
          </div>
          <div class="fpv-summary__item">
            <span>待记账凭证</span>
            <strong>{{ meta?.reviewableVoucherCount ?? 0 }}</strong>
          </div>
        </div>

        <div class="fpv-actions">
          <el-button :loading="loading.meta" @click="loadMeta">刷新</el-button>
          <el-button type="primary" :loading="loading.run" :disabled="!financePeriod.hasPeriodContext" @click="startPosting">
            开始记账
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="fpv-shell" shadow="never">
      <div class="fpv-overview">
        <div class="fpv-chip">
          <span>未审核</span>
          <strong>{{ meta?.unpostedVoucherCount ?? 0 }}</strong>
        </div>
        <div class="fpv-chip fpv-chip--danger">
          <span>错误凭证</span>
          <strong>{{ meta?.errorVoucherCount ?? 0 }}</strong>
        </div>
        <div class="fpv-chip fpv-chip--accent">
          <span>已记账</span>
          <strong>{{ meta?.postedVoucherCount ?? 0 }}</strong>
        </div>
        <div class="fpv-inline-message" :class="`is-${messageTone}`">
          {{ messageText }}
        </div>
      </div>

      <div v-if="taskStatus" class="fpv-progress-card">
        <div class="fpv-progress-card__head">
          <strong>记账进度</strong>
          <span>{{ taskStatus.periodStatusLabel }}</span>
        </div>
        <el-progress :percentage="taskStatus.progress || 0" :status="progressStatus" />
        <div class="fpv-progress-card__meta">
          <span>已记账 {{ taskStatus.postedVoucherCount }}</span>
          <span>待记账 {{ taskStatus.reviewableVoucherCount }}</span>
          <span>{{ taskStatus.resultMessage || '正在处理' }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { postVoucherApi, type FinancePostVoucherMeta, type FinancePostVoucherTaskStatus } from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'

const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()

const meta = ref<FinancePostVoucherMeta | null>(null)
const taskStatus = ref<FinancePostVoucherTaskStatus | null>(null)
const loading = reactive({
  meta: false,
  run: false,
  task: false
})

let pollTimer: ReturnType<typeof window.setTimeout> | null = null

const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const currentPeriodLabel = computed(() => financePeriod.currentMonthText || '未设置')
const messageTone = computed(() => {
  if ((meta.value?.unpostedVoucherCount || 0) > 0 || (meta.value?.errorVoucherCount || 0) > 0) {
    return 'warning'
  }
  if (meta.value?.canPost) {
    return 'success'
  }
  return 'neutral'
})
const messageText = computed(() => {
  if (!financeCompany.currentCompanyId) {
    return '请先选择财务公司。'
  }
  if (!financePeriod.hasPeriodContext) {
    return '当前公司未设置可用会计期间。'
  }
  if (meta.value?.blockedReason) {
    return meta.value.blockedReason
  }
  if ((meta.value?.reviewableVoucherCount || 0) <= 0 && (meta.value?.postedVoucherCount || 0) > 0) {
    return '本期已全部记账'
  }
  if (meta.value?.canPost) {
    return '当前期间可以开始记账。'
  }
  return '当前期间暂无可记账凭证。'
})
const progressStatus = computed(() => {
  if (taskStatus.value?.status === 'FAILED') {
    return 'exception'
  }
  if (taskStatus.value?.finished) {
    return 'success'
  }
  return undefined
})

watch(
  () => [financeCompany.currentCompanyId, financePeriod.currentYearPeriod] as const,
  async ([companyId]) => {
    stopPolling()
    taskStatus.value = null
    if (!companyId || !financePeriod.hasPeriodContext) {
      meta.value = null
      return
    }
    await loadMeta()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  stopPolling()
})

async function loadMeta() {
  const params = buildPeriodParams()
  if (!params) return
  loading.meta = true
  try {
    const res = await postVoucherApi.getMeta(params)
    meta.value = res.data
    if (meta.value.lastTaskNo && isActiveTaskStatus(meta.value.lastTaskStatus)) {
      await pollTask(meta.value.lastTaskNo, true)
    }
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载记账概览失败')
  } finally {
    loading.meta = false
  }
}

async function startPosting() {
  const params = buildPeriodParams()
  if (!params) {
    ElMessage.warning('当前公司或会计期间未准备好')
    return
  }
  if (!meta.value?.canPost) {
    ElMessage.warning(buildBlockingToast())
    return
  }
  loading.run = true
  try {
    const res = await postVoucherApi.runPosting(params)
    ElMessage.success(res.data.message || '记账任务已提交')
    await pollTask(res.data.taskNo, true)
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '提交记账任务失败')
  } finally {
    loading.run = false
  }
}

async function pollTask(taskNo: string, immediate = false) {
  stopPolling()
  if (immediate) {
    await fetchTask(taskNo)
  }
  if (!taskStatus.value?.finished) {
    pollTimer = window.setTimeout(() => {
      void pollTask(taskNo, true)
    }, 1200)
  }
}

async function fetchTask(taskNo: string) {
  loading.task = true
  try {
    const res = await postVoucherApi.getTaskStatus(taskNo)
    taskStatus.value = res.data
    if (taskStatus.value.finished) {
      await loadMeta()
      if (taskStatus.value.status === 'SUCCESS') {
        ElMessage.success(taskStatus.value.resultMessage || '记账完成')
      } else if (taskStatus.value.status === 'FAILED') {
        ElMessage.error(taskStatus.value.resultMessage || '记账失败')
      }
    }
  } catch (error: unknown) {
    stopPolling()
    ElMessage.error(error instanceof Error ? error.message : '加载记账进度失败')
  } finally {
    loading.task = false
  }
}

function stopPolling() {
  if (pollTimer !== null) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
}

function buildPeriodParams() {
  if (!financeCompany.currentCompanyId || !financePeriod.hasPeriodContext) {
    return null
  }
  return {
    companyId: financeCompany.currentCompanyId,
    iyear: financePeriod.currentYear,
    iperiod: financePeriod.currentPeriod
  }
}

function buildBlockingToast() {
  if ((meta.value?.unpostedVoucherCount || 0) > 0) {
    return `当前期间存在 ${meta.value?.unpostedVoucherCount || 0} 张未审核凭证，不能继续记账${buildSampleText(meta.value?.unpostedSampleVoucherNos || [])}`
  }
  if ((meta.value?.errorVoucherCount || 0) > 0) {
    return `当前期间存在 ${meta.value?.errorVoucherCount || 0} 张错误凭证，不能继续记账${buildSampleText(meta.value?.errorSampleVoucherNos || [])}`
  }
  return meta.value?.blockedReason || '当前期间暂无可记账凭证'
}

function buildSampleText(voucherNos: string[]) {
  return voucherNos.length ? `：${voucherNos.join('、')}` : ''
}

function isActiveTaskStatus(status?: string) {
  return status === 'PENDING' || status === 'RUNNING'
}
</script>

<style scoped>
.finance-post-voucher-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fpv-shell {
  border: 1px solid #dbe4ee;
}

.fpv-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.fpv-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.fpv-summary__item,
.fpv-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f7fafc;
}

.fpv-summary__item span,
.fpv-chip span {
  color: #5f6f82;
  font-size: 13px;
}

.fpv-summary__item strong,
.fpv-chip strong {
  color: #16202a;
  font-size: 14px;
  font-weight: 700;
}

.fpv-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fpv-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  align-items: stretch;
}

.fpv-chip--danger {
  background: #fff5f4;
}

.fpv-chip--accent {
  background: #eef6ff;
}

.fpv-inline-message {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  min-height: 44px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
}

.fpv-inline-message.is-warning {
  background: #fff7e8;
  color: #8a5a00;
}

.fpv-inline-message.is-success {
  background: #eff8f1;
  color: #1c6b3c;
}

.fpv-inline-message.is-neutral {
  background: #f5f7fa;
  color: #5f6f82;
}

.fpv-progress-card {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fpv-progress-card__head,
.fpv-progress-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #5f6f82;
}

.fpv-progress-card__head strong {
  color: #16202a;
  font-size: 14px;
}

@media (max-width: 1100px) {
  .fpv-summary,
  .fpv-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .fpv-toolbar,
  .fpv-actions {
    align-items: stretch;
  }

  .fpv-summary,
  .fpv-overview {
    grid-template-columns: 1fr;
  }

  .fpv-actions {
    width: 100%;
  }
}
</style>
