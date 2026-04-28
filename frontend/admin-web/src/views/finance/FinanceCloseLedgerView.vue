<template>
  <div class="finance-close-ledger-page">
    <el-card class="fcl-shell fcl-shell--toolbar" shadow="never">
      <div class="fcl-toolbar">
        <div class="fcl-summary">
          <div class="fcl-summary__item">
            <span>当前公司</span>
            <strong>{{ financeCompany.currentCompanyName || '未设置' }}</strong>
          </div>
          <div class="fcl-summary__item">
            <span>当前期间</span>
            <strong>{{ financePeriod.currentMonthText || '未设置' }}</strong>
          </div>
          <div class="fcl-summary__item">
            <span>结账状态</span>
            <strong>{{ meta?.statusLabel || '未结账' }}</strong>
          </div>
          <div class="fcl-summary__item">
            <span>记账状态</span>
            <strong>{{ meta?.postStatusLabel || '未记账' }}</strong>
          </div>
        </div>

        <div class="fcl-actions">
          <el-button :loading="loading.meta" @click="loadMeta">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="fcl-shell" shadow="never">
      <div class="fcl-steps">
        <button
          v-for="(item, index) in stepItems"
          :key="item.key"
          type="button"
          class="fcl-step"
          :class="{ 'is-active': index === stepIndex, 'is-done': index < stepIndex }"
          @click="jumpToStep(index)"
        >
          <span class="fcl-step__index">{{ index + 1 }}</span>
          <span class="fcl-step__label">{{ item.label }}</span>
        </button>
      </div>

      <div v-if="stepIndex === 0" class="fcl-panel">
        <div class="fcl-notice-grid">
          <div class="fcl-notice-item">结账后本月不能再填制凭证</div>
          <div class="fcl-notice-item">还有未记账的凭证不能结账</div>
          <div class="fcl-notice-item">每月对账正确后才能结账</div>
        </div>

        <div class="fcl-metric-grid">
          <div class="fcl-metric">
            <span>未审核</span>
            <strong>{{ meta?.unpostedVoucherCount ?? 0 }}</strong>
          </div>
          <div class="fcl-metric">
            <span>已审核未记账</span>
            <strong>{{ meta?.reviewedVoucherCount ?? 0 }}</strong>
          </div>
          <div class="fcl-metric">
            <span>错误凭证</span>
            <strong>{{ meta?.errorVoucherCount ?? 0 }}</strong>
          </div>
          <div class="fcl-metric">
            <span>已记账</span>
            <strong>{{ meta?.postedVoucherCount ?? 0 }}</strong>
          </div>
        </div>
      </div>

      <div v-else-if="stepIndex === 1" class="fcl-panel">
        <div class="fcl-result-head">
          <strong>总账对账</strong>
          <span :class="statusClass(reconcileResult?.passed)">{{ reconcileResult?.passed ? '已通过' : '待校验' }}</span>
        </div>
        <div class="fcl-metric-grid">
          <div class="fcl-metric">
            <span>科目差异</span>
            <strong>{{ reconcileResult?.differenceSubjectCount ?? 0 }}</strong>
          </div>
          <div class="fcl-metric">
            <span>辅助差异</span>
            <strong>{{ reconcileResult?.differenceAssistCount ?? 0 }}</strong>
          </div>
          <div class="fcl-metric">
            <span>缺失辅助</span>
            <strong>{{ reconcileResult?.missingAssistCount ?? 0 }}</strong>
          </div>
          <div class="fcl-metric">
            <span>非法组合</span>
            <strong>{{ reconcileResult?.illegalAssistCount ?? 0 }}</strong>
          </div>
        </div>
        <div class="fcl-message" :class="statusClass(reconcileResult?.passed)">
          {{ reconcileResult?.summaryMessage || '点击下一步执行对账。' }}
        </div>
        <div v-if="reconcileIssues.length" class="fcl-list">
          <div v-for="item in reconcileIssues" :key="item" class="fcl-list__item">{{ item }}</div>
        </div>
      </div>

      <div v-else-if="stepIndex === 2" class="fcl-panel">
        <div class="fcl-result-head">
          <strong>总账校验</strong>
          <span :class="statusClass(validationResult?.generalPassed)">{{ validationResult?.generalPassed ? '已通过' : '待校验' }}</span>
        </div>
        <div class="fcl-check-list">
          <div v-for="item in validationResult?.generalChecks || []" :key="item.code" class="fcl-check-item">
            <div>
              <strong>{{ item.label }}</strong>
              <p>{{ item.message }}</p>
            </div>
            <span :class="statusClass(item.passed)">{{ item.passed ? '通过' : '未通过' }}</span>
          </div>
        </div>
        <div v-if="validationGeneralReasons.length" class="fcl-list">
          <div v-for="item in validationGeneralReasons" :key="item" class="fcl-list__item">{{ item }}</div>
        </div>
      </div>

      <div v-else-if="stepIndex === 3" class="fcl-panel">
        <div class="fcl-result-head">
          <strong>其他系统校验</strong>
          <span :class="statusClass(validationResult?.externalPassed)">{{ validationResult?.externalPassed ? '已通过' : '待校验' }}</span>
        </div>
        <div class="fcl-check-list">
          <div v-for="item in validationResult?.externalChecks || []" :key="item.code" class="fcl-check-item">
            <div>
              <strong>{{ item.label }}</strong>
              <p>{{ item.message }}</p>
            </div>
            <span :class="statusClass(item.passed)">{{ item.passed ? '通过' : '未通过' }}</span>
          </div>
        </div>
        <div v-if="validationExternalReasons.length" class="fcl-list">
          <div v-for="item in validationExternalReasons" :key="item" class="fcl-list__item">{{ item }}</div>
        </div>
      </div>

      <div v-else class="fcl-panel">
        <div class="fcl-result-head">
          <strong>确认结账</strong>
          <span :class="statusClass(meta?.status === 'CLOSED')">{{ meta?.status === 'CLOSED' ? '已结账' : '待执行' }}</span>
        </div>
        <div class="fcl-metric-grid">
          <div class="fcl-metric">
            <span>对账结果</span>
            <strong>{{ reconcileResult?.passed ? '通过' : '未通过' }}</strong>
          </div>
          <div class="fcl-metric">
            <span>总账校验</span>
            <strong>{{ validationResult?.generalPassed ? '通过' : '未通过' }}</strong>
          </div>
          <div class="fcl-metric">
            <span>其他系统</span>
            <strong>{{ validationResult?.externalPassed ? '通过' : '未通过' }}</strong>
          </div>
          <div class="fcl-metric">
            <span>固定资产</span>
            <strong>{{ meta?.fixedAssetStatusLabel || '未校验' }}</strong>
          </div>
        </div>
        <el-input v-model="closeNote" placeholder="可选备注" maxlength="200" clearable />
      </div>

      <div class="fcl-footer">
        <el-button :disabled="stepIndex === 0" @click="prevStep">上一步</el-button>
        <el-button
          v-if="stepIndex < stepItems.length - 1"
          type="primary"
          :loading="nextLoading"
          :disabled="!canOperate"
          @click="nextStep"
        >
          下一步
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="loading.close"
          :disabled="!canClose"
          @click="submitClose"
        >
          结账
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  closeLedgerApi,
  type FinanceCloseLedgerMeta,
  type FinanceCloseLedgerReconcileResult,
  type FinanceCloseLedgerValidationResult
} from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'

const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()

const stepItems = [
  { key: 'notice', label: '须知' },
  { key: 'reconcile', label: '对账' },
  { key: 'general', label: '总账校验' },
  { key: 'external', label: '其他系统' },
  { key: 'confirm', label: '确认结账' }
]

const stepIndex = ref(0)
const closeNote = ref('')
const meta = ref<FinanceCloseLedgerMeta | null>(null)
const reconcileResult = ref<FinanceCloseLedgerReconcileResult | null>(null)
const validationResult = ref<FinanceCloseLedgerValidationResult | null>(null)
const loading = reactive({
  meta: false,
  reconcile: false,
  validate: false,
  close: false
})

const canOperate = computed(() => Boolean(financeCompany.currentCompanyId && financePeriod.hasPeriodContext))
const nextLoading = computed(() => {
  if (stepIndex.value === 1) return loading.reconcile
  if (stepIndex.value === 2 || stepIndex.value === 3) return loading.validate
  return false
})
const canClose = computed(() =>
  canOperate.value
  && Boolean(validationResult.value?.passed)
  && meta.value?.status !== 'CLOSED'
)
const reconcileIssues = computed(() => {
  const result = reconcileResult.value
  if (!result) return [] as string[]
  return [
    ...result.differenceSubjects,
    ...result.differenceAssistKeys,
    ...result.missingAssistSubjects,
    ...result.illegalAssistMessages
  ].slice(0, 12)
})
const validationGeneralReasons = computed(() => {
  const generalChecks = validationResult.value?.generalChecks || []
  return generalChecks.filter((item) => !item.passed).map((item) => item.message).filter(Boolean)
})
const validationExternalReasons = computed(() => {
  const externalChecks = validationResult.value?.externalChecks || []
  return externalChecks.filter((item) => !item.passed).map((item) => item.message).filter(Boolean)
})

watch(
  () => [financeCompany.currentCompanyId, financePeriod.currentYearPeriod] as const,
  async ([companyId]) => {
    resetWorkflow()
    if (!companyId || !financePeriod.hasPeriodContext) {
      meta.value = null
      return
    }
    await loadMeta()
  },
  { immediate: true }
)

async function loadMeta() {
  const payload = buildPayload()
  if (!payload) return
  loading.meta = true
  try {
    const res = await closeLedgerApi.getMeta(payload)
    meta.value = res.data
    closeNote.value = res.data.closeNote || ''
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载结账信息失败')
  } finally {
    loading.meta = false
  }
}

function resetWorkflow() {
  stepIndex.value = 0
  reconcileResult.value = null
  validationResult.value = null
  closeNote.value = ''
}

function buildPayload() {
  if (!financeCompany.currentCompanyId || !financePeriod.hasPeriodContext) {
    return null
  }
  return {
    companyId: financeCompany.currentCompanyId,
    iyear: financePeriod.currentYear,
    iperiod: financePeriod.currentPeriod
  }
}

function jumpToStep(index: number) {
  if (index < 0 || index > stepIndex.value) return
  stepIndex.value = index
}

function prevStep() {
  if (stepIndex.value > 0) {
    stepIndex.value -= 1
  }
}

async function nextStep() {
  if (!canOperate.value) {
    ElMessage.warning('当前公司或会计期间未准备好')
    return
  }
  if (stepIndex.value === 0) {
    stepIndex.value = 1
    return
  }
  if (stepIndex.value === 1) {
    const passed = await runReconcile()
    if (passed) stepIndex.value = 2
    return
  }
  if (stepIndex.value === 2) {
    const result = await runValidate()
    if (result?.generalPassed) stepIndex.value = 3
    return
  }
  if (stepIndex.value === 3) {
    const result = validationResult.value || await runValidate()
    if (result?.externalPassed) {
      stepIndex.value = 4
      return
    }
    ElMessage.warning(result?.blockingReasons?.[0] || '其他系统校验未通过')
  }
}

async function runReconcile() {
  const payload = buildPayload()
  if (!payload) return false
  loading.reconcile = true
  try {
    const res = await closeLedgerApi.reconcile(payload)
    reconcileResult.value = res.data
    if (!res.data.passed) {
      ElMessage.warning(res.data.summaryMessage || '对账未通过')
      return false
    }
    ElMessage.success('总账对账通过')
    return true
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '执行对账失败')
    return false
  } finally {
    loading.reconcile = false
  }
}

async function runValidate() {
  const payload = buildPayload()
  if (!payload) return null
  loading.validate = true
  try {
    const res = await closeLedgerApi.validate(payload)
    validationResult.value = res.data
    if (!res.data.generalPassed || !res.data.externalPassed) {
      ElMessage.warning(res.data.blockingReasons?.[0] || '结账校验未通过')
    } else {
      ElMessage.success('结账校验通过')
    }
    return res.data
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '执行结账校验失败')
    return null
  } finally {
    loading.validate = false
  }
}

async function submitClose() {
  const payload = buildPayload()
  if (!payload) {
    ElMessage.warning('当前公司或会计期间未准备好')
    return
  }
  loading.close = true
  try {
    const res = await closeLedgerApi.close({
      ...payload,
      closeNote: closeNote.value || undefined
    })
    meta.value = res.data
    closeNote.value = res.data.closeNote || closeNote.value
    ElMessage.success('结账成功')
    await financeCompany.refreshContext(financeCompany.currentCompanyId)
    financePeriod.syncWithCompany(financeCompany.currentCompanyId, true)
    await loadMeta()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '执行结账失败')
  } finally {
    loading.close = false
  }
}

function statusClass(passed?: boolean) {
  if (passed === true) return 'is-success'
  if (passed === false) return 'is-danger'
  return 'is-neutral'
}
</script>

<style scoped>
.finance-close-ledger-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fcl-shell {
  border: 1px solid #dbe4ee;
}

.fcl-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.fcl-summary,
.fcl-metric-grid,
.fcl-notice-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.fcl-summary {
  flex: 1;
  min-width: 0;
}

.fcl-summary__item,
.fcl-metric,
.fcl-notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f7fafc;
}

.fcl-notice-item {
  justify-content: flex-start;
  color: #5f6f82;
  line-height: 1.5;
}

.fcl-summary__item span,
.fcl-metric span {
  color: #5f6f82;
  font-size: 13px;
  white-space: nowrap;
}

.fcl-summary__item strong,
.fcl-metric strong {
  color: #16202a;
  font-size: 14px;
  font-weight: 700;
}

.fcl-actions,
.fcl-footer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fcl-steps {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.fcl-step {
  border: 1px solid #dbe4ee;
  background: #fff;
  border-radius: 14px;
  min-height: 48px;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.fcl-step__index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #edf3f8;
  color: #496079;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.fcl-step__label {
  color: #2a3d52;
  font-size: 13px;
  font-weight: 600;
}

.fcl-step.is-active {
  border-color: #2f7ae5;
  background: #eef5ff;
}

.fcl-step.is-active .fcl-step__index,
.fcl-step.is-done .fcl-step__index {
  background: #2f7ae5;
  color: #fff;
}

.fcl-step.is-done {
  border-color: #c9d8ee;
  background: #f6faff;
}

.fcl-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fcl-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: #16202a;
}

.fcl-message,
.fcl-list__item,
.fcl-check-item {
  border-radius: 12px;
  border: 1px solid #e6edf5;
  background: #fbfdff;
}

.fcl-message {
  padding: 10px 12px;
  font-size: 13px;
}

.fcl-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fcl-list__item,
.fcl-check-item {
  padding: 10px 12px;
}

.fcl-check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fcl-check-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.fcl-check-item strong {
  display: block;
  margin-bottom: 4px;
  color: #16202a;
}

.fcl-check-item p {
  margin: 0;
  color: #5f6f82;
  font-size: 13px;
  line-height: 1.5;
}

.is-success {
  color: #1c6b3c;
}

.is-danger {
  color: #bc3e2d;
}

.is-neutral {
  color: #6d7a8c;
}

@media (max-width: 1100px) {
  .fcl-summary,
  .fcl-metric-grid,
  .fcl-notice-grid,
  .fcl-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .fcl-summary,
  .fcl-metric-grid,
  .fcl-notice-grid,
  .fcl-steps {
    grid-template-columns: 1fr;
  }

  .fcl-toolbar,
  .fcl-footer,
  .fcl-actions {
    align-items: stretch;
  }
}
</style>
