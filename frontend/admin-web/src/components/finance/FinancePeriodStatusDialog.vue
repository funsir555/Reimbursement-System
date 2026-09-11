<template>
  <el-dialog
    :model-value="modelValue"
    title="总账期间状态"
    width="1040px"
    destroy-on-close
    :close-on-click-modal="false"
    @update:model-value="handleVisibilityChange"
  >
    <div class="period-status-dialog">
      <div class="period-status-dialog__summary">
        <div class="period-status-dialog__summary-item">
          <span>当前公司</span>
          <strong>{{ overview?.companyName || financeCompany.currentCompanyName || '未设置' }}</strong>
        </div>
        <div class="period-status-dialog__summary-item">
          <span>当前年度</span>
          <strong>{{ overview?.iyear ?? financePeriod.currentYear ?? '--' }}</strong>
        </div>
        <div class="period-status-dialog__summary-item">
          <span>当前期间</span>
          <strong>{{ financePeriod.currentMonthText || overview?.currentPeriodLabel || '--' }}</strong>
        </div>
        <div class="period-status-dialog__summary-item period-status-dialog__summary-item--hint">
          <span>操作说明</span>
          <strong>仅允许对当前期间及以前期间执行回退</strong>
        </div>
      </div>

      <div class="period-status-dialog__table-shell" v-loading="loading.overview">
        <el-table :data="rows" border stripe row-key="iyperiod" empty-text="暂无期间状态数据">
          <el-table-column prop="periodLabel" label="期间" width="104" />
          <el-table-column label="凭证数" min-width="136">
            <template #default="{ row }">
              <div class="period-status-dialog__metric">
                <strong>{{ row.voucherCount }}</strong>
                <span>已记账 {{ row.postedVoucherCount }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="reviewStatusLabel" label="审核状态" min-width="128" />
          <el-table-column prop="postStatusLabel" label="记账状态" min-width="128" />
          <el-table-column prop="closeStatusLabel" label="结账状态" min-width="118" />
          <el-table-column prop="periodTransferStatusLabel" label="期末结转状态" min-width="188" />
          <el-table-column label="可执行动作" min-width="240" align="left">
            <template #default="{ row }">
              <div v-if="row.allowedActions.length" class="period-status-dialog__actions">
                <el-button
                  v-for="action in row.allowedActions"
                  :key="`${row.iyperiod}-${action}`"
                  size="small"
                  type="primary"
                  plain
                  @click="openConfirm(row, action)"
                >
                  {{ resolveActionLabel(action) }}
                </el-button>
              </div>
              <span v-else class="period-status-dialog__reason">
                {{ row.blockingReason || '当前期间无可执行动作' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" :loading="loading.overview" @click="loadOverview">刷新状态</el-button>
    </template>
  </el-dialog>

  <el-dialog
    :model-value="confirmVisible"
    width="560px"
    destroy-on-close
    :close-on-click-modal="false"
    @update:model-value="handleConfirmVisibilityChange"
  >
    <template #header>
      <div class="period-status-dialog__confirm-title">{{ currentActionLabel }}</div>
    </template>

    <div v-if="confirmRow" class="period-status-dialog__confirm">
      <div class="period-status-dialog__confirm-grid">
        <div class="period-status-dialog__confirm-item">
          <span>公司</span>
          <strong>{{ overview?.companyName || financeCompany.currentCompanyName || '--' }}</strong>
        </div>
        <div class="period-status-dialog__confirm-item">
          <span>期间</span>
          <strong>{{ confirmRow.periodLabel }}</strong>
        </div>
        <div class="period-status-dialog__confirm-item">
          <span>当前状态</span>
          <strong>{{ `${confirmRow.postStatusLabel} / ${confirmRow.closeStatusLabel}` }}</strong>
        </div>
        <div class="period-status-dialog__confirm-item">
          <span>动作类型</span>
          <strong>{{ currentActionLabel }}</strong>
        </div>
      </div>

      <div class="period-status-dialog__risk">
        {{ currentRiskMessage }}
      </div>

      <el-input
        v-model="password"
        type="password"
        show-password
        clearable
        placeholder="请输入当前登录密码后执行"
        autocomplete="current-password"
        @keyup.enter="submitAction"
      />
    </div>

    <template #footer>
      <el-button @click="handleConfirmVisibilityChange(false)">取消</el-button>
      <el-button type="primary" :loading="loading.action" @click="submitAction">
        确认执行
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  generalLedgerPeriodStatusApi,
  type FinanceGeneralLedgerPeriodActionRequest,
  type FinanceGeneralLedgerPeriodActionType,
  type FinanceGeneralLedgerPeriodStatusOverview,
  type FinanceGeneralLedgerPeriodStatusRow
} from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'

defineOptions({
  name: 'FinancePeriodStatusDialog'
})

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'completed'): void
}>()

const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()

const overview = ref<FinanceGeneralLedgerPeriodStatusOverview | null>(null)
const loading = reactive({
  overview: false,
  action: false
})
const confirmVisible = ref(false)
const confirmRow = ref<FinanceGeneralLedgerPeriodStatusRow | null>(null)
const confirmAction = ref<FinanceGeneralLedgerPeriodActionType | ''>('')
const password = ref('')

const actionLabelMap: Record<FinanceGeneralLedgerPeriodActionType, string> = {
  REOPEN: '反结账',
  UNPOST: '反记账'
}

const rows = computed(() => overview.value?.rows || [])
const currentActionLabel = computed(() =>
  confirmAction.value ? actionLabelMap[confirmAction.value] : '期间状态操作'
)
const currentRiskMessage = computed(() => {
  if (confirmAction.value === 'REOPEN') {
    return '反结账只会恢复本期开放状态，不会回滚下一期间业务数据，请确认当前期间确需重新处理。'
  }
  return '反记账会把本期已记账凭证恢复为已审核未记账状态，请确认后再执行。'
})

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      await loadOverview()
      return
    }
    resetConfirmState()
  }
)

function handleVisibilityChange(value: boolean) {
  emit('update:modelValue', value)
}

function handleConfirmVisibilityChange(value: boolean) {
  confirmVisible.value = value
  if (!value) {
    resetConfirmState()
  }
}

async function loadOverview() {
  if (!financeCompany.currentCompanyId || !financePeriod.hasPeriodContext) {
    overview.value = null
    return
  }
  loading.overview = true
  try {
    const res = await generalLedgerPeriodStatusApi.getOverview({
      companyId: financeCompany.currentCompanyId,
      iyear: financePeriod.currentYear,
      currentIyear: financePeriod.currentYear,
      currentIperiod: financePeriod.currentPeriod
    })
    overview.value = res.data
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载期间状态失败')
  } finally {
    loading.overview = false
  }
}

function openConfirm(row: FinanceGeneralLedgerPeriodStatusRow, action: FinanceGeneralLedgerPeriodActionType) {
  confirmRow.value = row
  confirmAction.value = action
  password.value = ''
  confirmVisible.value = true
}

function resolveActionLabel(action: FinanceGeneralLedgerPeriodActionType | string) {
  return actionLabelMap[action as FinanceGeneralLedgerPeriodActionType] || action
}

async function submitAction() {
  if (!confirmRow.value || !confirmAction.value) {
    return
  }
  if (!password.value.trim()) {
    ElMessage.warning('请输入当前登录密码')
    return
  }
  const payload: FinanceGeneralLedgerPeriodActionRequest = {
    companyId: overview.value?.companyId || financeCompany.currentCompanyId || '',
    iyear: confirmRow.value.iyear,
    iperiod: confirmRow.value.iperiod,
    currentIyear: financePeriod.currentYear,
    currentIperiod: financePeriod.currentPeriod,
    password: password.value
  }
  loading.action = true
  try {
    if (confirmAction.value === 'REOPEN') {
      await generalLedgerPeriodStatusApi.reopen(payload)
      ElMessage.success('反结账成功')
    } else {
      await generalLedgerPeriodStatusApi.unpost(payload)
      ElMessage.success('反记账成功')
    }
    handleConfirmVisibilityChange(false)
    await loadOverview()
    emit('completed')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : `${currentActionLabel.value}失败`)
  } finally {
    loading.action = false
  }
}

function resetConfirmState() {
  confirmVisible.value = false
  confirmRow.value = null
  confirmAction.value = ''
  password.value = ''
}
</script>

<style scoped>
.period-status-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.period-status-dialog__summary,
.period-status-dialog__confirm-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.period-status-dialog__summary-item,
.period-status-dialog__confirm-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 64px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #d8e3ef;
  background: linear-gradient(180deg, #fbfdff 0%, #f3f7fb 100%);
}

.period-status-dialog__summary-item span,
.period-status-dialog__confirm-item span {
  color: #607181;
  font-size: 12px;
}

.period-status-dialog__summary-item strong,
.period-status-dialog__confirm-item strong {
  color: #18232f;
  font-size: 14px;
  line-height: 1.5;
}

.period-status-dialog__summary-item--hint {
  background: linear-gradient(180deg, #f9fcff 0%, #edf5ff 100%);
}

.period-status-dialog__table-shell {
  min-height: 280px;
}

.period-status-dialog__metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.period-status-dialog__metric strong {
  color: #18232f;
  font-size: 14px;
}

.period-status-dialog__metric span,
.period-status-dialog__reason {
  color: #617386;
  font-size: 12px;
  line-height: 1.5;
}

.period-status-dialog__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.period-status-dialog__confirm {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.period-status-dialog__confirm-title {
  font-size: 16px;
  font-weight: 700;
  color: #18232f;
}

.period-status-dialog__risk {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #f2ddac;
  background: #fff7e8;
  color: #8a5a00;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .period-status-dialog__summary,
  .period-status-dialog__confirm-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .period-status-dialog__summary,
  .period-status-dialog__confirm-grid {
    grid-template-columns: 1fr;
  }
}
</style>
