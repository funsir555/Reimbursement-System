<template>
  <div class="fpt-page">
    <el-card class="fpt-shell fpt-shell--toolbar" shadow="never">
      <div class="fpt-toolbar">
        <div class="fpt-summary">
          <div class="fpt-summary__item">
            <span>当前公司</span>
            <strong>{{ financeCompany.currentCompanyName || '未设置' }}</strong>
          </div>
          <div class="fpt-summary__item">
            <span>当前期间</span>
            <strong>{{ financePeriod.currentMonthText || meta?.periodLabel || '未设置' }}</strong>
          </div>
          <div class="fpt-summary__item">
            <span>专用凭证字</span>
            <strong>{{ meta?.defaultVoucherType || '转' }}</strong>
          </div>
          <div class="fpt-summary__item">
            <span>当前规则</span>
            <strong>{{ currentRule?.ruleTypeLabel || currentRuleLabel }}</strong>
          </div>
        </div>

        <div class="fpt-actions">
          <el-button :loading="loading.runs" @click="openRunsDialog">查看本期生成记录</el-button>
          <el-button :loading="loading.meta || loading.rules" @click="refreshPage">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-alert
      v-if="!financeCompany.currentCompanyHasActiveAccountSet"
      type="info"
      :closable="false"
      title="当前公司未创建账套"
      description="期末结转按当前账套和期间执行。请先完成账套创建后，再维护结转规则与生成凭证。"
    />

    <el-alert
      v-else-if="!financePeriod.hasPeriodContext"
      type="warning"
      :closable="false"
      title="当前未选择会计期间"
      description="请先在顶部选择当前账套的会计期间，再执行期末结转。"
    />

    <div class="fpt-grid">
      <el-card class="fpt-shell fpt-rule-nav" shadow="never">
        <template #header>
          <div class="fpt-card-head">
            <div>
              <strong>结转规则</strong>
              <p>规则按账套长期保存，可按结转类型分别维护。</p>
            </div>
          </div>
        </template>

        <div class="fpt-rule-list">
          <button
            v-for="item in ruleTypeOptions"
            :key="item.value"
            type="button"
            class="fpt-rule-tab"
            :class="{ 'is-active': item.value === activeRuleType }"
            @click="changeRuleType(item.value as FinancePeriodTransferRuleType)"
          >
            <span class="fpt-rule-tab__label">{{ item.name || item.label || item.value }}</span>
            <span class="fpt-rule-tab__meta">{{ resolveRuleStateLabel(item.value as FinancePeriodTransferRuleType) }}</span>
          </button>
        </div>
      </el-card>

      <el-card class="fpt-shell" shadow="never">
        <template #header>
          <div class="fpt-card-head">
            <div>
              <strong>{{ currentRule?.ruleTypeLabel || currentRuleLabel }}</strong>
              <p>{{ currentRuleDescription }}</p>
            </div>

            <div class="fpt-inline-actions" v-if="currentRule">
              <el-switch
                v-model="currentRule.enabled"
                inline-prompt
                active-text="启用"
                inactive-text="停用"
              />
              <el-checkbox v-model="currentRule.includeUnposted">包含未记账凭证</el-checkbox>
            </div>
          </div>
        </template>

        <div v-if="currentRule" class="fpt-form">
          <div class="fpt-form-grid">
            <el-form-item label="规则名称">
              <el-input v-model="currentRule.ruleName" maxlength="60" clearable placeholder="请输入规则名称" />
            </el-form-item>
            <el-form-item label="凭证字">
              <el-input :model-value="meta?.defaultVoucherType || '转'" disabled />
            </el-form-item>
            <el-form-item label="重生成策略">
              <el-input :model-value="currentRule.regenerateStrategy || 'REPLACE_UNPOSTED'" disabled />
            </el-form-item>
            <el-form-item label="规则状态">
              <el-input :model-value="currentRule.enabled ? '启用中' : '未启用'" disabled />
            </el-form-item>
          </div>

          <div v-if="activeRuleType === 'PROFIT'" class="fpt-section">
            <div class="fpt-form-grid">
              <el-form-item label="结转粒度">
                <el-select v-model="currentRule.transferGranularity" class="w-full" placeholder="请选择结转粒度">
                  <el-option
                    v-for="item in meta?.transferGranularityOptions || []"
                    :key="item.value"
                    :label="item.name || item.label || item.value"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="本年利润科目">
                <el-select
                  v-model="currentRule.targetSubjectCode"
                  class="w-full"
                  filterable
                  clearable
                  placeholder="请选择本年利润科目"
                >
                  <el-option
                    v-for="item in leafAccountOptions"
                    :key="item.value"
                    :label="formatOptionLabel(item)"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </div>

            <div class="fpt-note">
              收入类科目会按借方冲销，成本和费用类科目会按贷方冲销，并统一结转到你指定的本年利润科目。
            </div>
          </div>

          <div v-else-if="activeRuleType === 'CUSTOM'" class="fpt-section">
            <div class="fpt-section-head">
              <strong>自定义映射明细</strong>
              <el-button type="primary" plain @click="addRuleLine('CUSTOM')">新增明细</el-button>
            </div>

            <div class="fpt-note">
              支持多对一、多对多映射。转入科目的辅助核算只能少于转出科目，不能新增新的辅助类型。
            </div>

            <el-table :data="currentRule.lines" border class="fpt-table">
              <el-table-column label="#" width="58">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column label="转出科目" min-width="220">
                <template #default="{ row }">
                  <el-select v-model="row.sourceSubjectCode" class="w-full" filterable clearable placeholder="请选择">
                    <el-option
                      v-for="item in leafAccountOptions"
                      :key="item.value"
                      :label="formatOptionLabel(item)"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="指标" min-width="160">
                <template #default="{ row }">
                  <el-select v-model="row.metricType" class="w-full" placeholder="请选择指标">
                    <el-option
                      v-for="item in meta?.metricOptions || []"
                      :key="item.value"
                      :label="item.name || item.label || item.value"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="比例系数" min-width="150">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.ratio"
                    class="w-full"
                    :min="0.000001"
                    :precision="6"
                    :step="0.1"
                    controls-position="right"
                  />
                </template>
              </el-table-column>
              <el-table-column label="转入科目" min-width="220">
                <template #default="{ row }">
                  <el-select v-model="row.targetSubjectCode" class="w-full" filterable clearable placeholder="请选择">
                    <el-option
                      v-for="item in leafAccountOptions"
                      :key="item.value"
                      :label="formatOptionLabel(item)"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.remark" maxlength="120" clearable placeholder="可选备注" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="92" fixed="right">
                <template #default="{ $index }">
                  <el-button type="danger" link @click="removeRuleLine('CUSTOM', $index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-else class="fpt-section">
            <div class="fpt-form-grid">
              <el-form-item label="制造费用来源科目">
                <el-select
                  v-model="currentRule.sourceSubjectCode"
                  class="w-full"
                  filterable
                  clearable
                  placeholder="默认取制造费用及其下级"
                >
                  <el-option
                    v-for="item in accountOptions"
                    :key="item.value"
                    :label="formatOptionLabel(item)"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="转入科目">
                <el-select
                  v-model="currentRule.targetSubjectCode"
                  class="w-full"
                  filterable
                  clearable
                  placeholder="请选择生产成本转入科目"
                >
                  <el-option
                    v-for="item in leafAccountOptions"
                    :key="item.value"
                    :label="formatOptionLabel(item)"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="分配方式">
                <el-select
                  v-model="currentRule.allocationMode"
                  class="w-full"
                  placeholder="请选择分配方式"
                >
                  <el-option
                    v-for="item in meta?.allocationModeOptions || []"
                    :key="item.value"
                    :label="item.name || item.label || item.value"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </div>

            <div class="fpt-note">
              “按项目辅助分配”会复用现有项目辅助档案，不额外新建生产订单或产品档案体系。
            </div>

            <template v-if="currentRule.allocationMode !== 'FULL'">
              <div class="fpt-section-head">
                <strong>分配明细</strong>
                <el-button type="primary" plain @click="addRuleLine('MANUFACTURE')">新增分配</el-button>
              </div>

              <el-table :data="currentRule.lines" border class="fpt-table">
                <el-table-column label="#" width="58">
                  <template #default="{ $index }">{{ $index + 1 }}</template>
                </el-table-column>
                <el-table-column v-if="currentRule.allocationMode === 'PROJECT'" label="项目" min-width="240">
                  <template #default="{ row }">
                    <el-select
                      v-model="row.allocationProjectId"
                      class="w-full"
                      filterable
                      clearable
                      placeholder="请选择项目"
                      @change="handleProjectSelected(row)"
                    >
                      <el-option
                        v-for="item in projectOptions"
                        :key="item.value"
                        :label="formatOptionLabel(item)"
                        :value="item.value"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column v-if="currentRule.allocationMode === 'PROJECT'" label="项目分类" min-width="170">
                  <template #default="{ row }">
                    <el-input :model-value="resolveProjectClassLabel(row.allocationProjectClass)" disabled />
                  </template>
                </el-table-column>
                <el-table-column label="比例(%)" min-width="160">
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.ratio"
                      class="w-full"
                      :min="0.000001"
                      :max="100"
                      :precision="6"
                      :step="0.5"
                      controls-position="right"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="备注" min-width="220">
                  <template #default="{ row }">
                    <el-input v-model="row.remark" maxlength="120" clearable placeholder="可选备注" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="92" fixed="right">
                  <template #default="{ $index }">
                    <el-button type="danger" link @click="removeRuleLine('MANUFACTURE', $index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="fpt-shell" shadow="never">
      <template #header>
        <div class="fpt-card-head">
          <div>
            <strong>转账生成预览</strong>
            <p>先预览，再生成凭证。若规则或期间数据发生变化，请重新预览。</p>
          </div>
          <div class="fpt-preview-summary" v-if="previewResult">
            <span>生成 {{ previewResult.generatedEntryCount }} 条</span>
            <span>跳过 {{ previewResult.skippedEntryCount }} 条</span>
            <span>金额 {{ formatAmount(previewResult.totalAmount) }}</span>
          </div>
        </div>
      </template>

      <el-empty
        v-if="!previewResult"
        description="保存规则后即可生成本期预览明细。"
      />

      <template v-else>
        <div class="fpt-message">{{ previewResult.message || '预览已生成，请核对后再生成凭证。' }}</div>
        <el-empty
          v-if="previewResult.details.length === 0"
          :description="previewResult.message || '当前期间没有可展示的结转明细。'"
        />
        <el-table v-else :data="previewResult.details" border class="fpt-table">
          <el-table-column prop="lineNo" label="#" width="58" />
          <el-table-column label="来源科目" min-width="220">
            <template #default="{ row }">
              <div class="fpt-cell-stack">
                <strong>{{ row.sourceSubjectCode || '--' }} {{ row.sourceSubjectName || '' }}</strong>
                <span v-if="row.sourceAssistLabel">{{ row.sourceAssistLabel }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="目标科目" min-width="200">
            <template #default="{ row }">
              <span>{{ row.targetSubjectCode || '--' }} {{ row.targetSubjectName || '' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="metricTypeLabel" label="指标" min-width="130" />
          <el-table-column prop="directionLabel" label="方向" min-width="90" />
          <el-table-column label="金额" min-width="120" align="right">
            <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="skipReason" label="跳过原因" min-width="220">
            <template #default="{ row }">
              <span :class="row.skipReason ? 'fpt-skip-reason' : 'fpt-skip-ok'">{{ row.skipReason || '可生成' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>

    <el-card class="fpt-shell" shadow="never">
      <div class="fpt-footer">
        <div class="fpt-footer__hint">
          生成凭证前会再次校验预览快照，避免用过期规则直接落账。
        </div>
        <div class="fpt-actions">
          <el-button :loading="loading.save" :disabled="!canOperate" @click="saveCurrentRule()">保存规则</el-button>
          <el-button type="primary" :loading="loading.preview" :disabled="!canOperate" @click="previewCurrentRule">
            预览生成
          </el-button>
          <el-button type="success" :loading="loading.generate" :disabled="!canGenerate" @click="generateCurrentVoucher">
            生成凭证
          </el-button>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="runsDialogVisible"
      title="本期生成记录"
      width="1120px"
      destroy-on-close
    >
      <div class="fpt-dialog-meta">
        <span>期间：{{ meta?.periodLabel || financePeriod.currentMonthText || '--' }}</span>
        <span>规则：{{ currentRule?.ruleTypeLabel || currentRuleLabel }}</span>
      </div>

      <el-table v-loading="loading.runs" :data="runs" border class="fpt-table" max-height="460">
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="statusLabel" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="resolveRunTagType(row.status)" effect="plain">
              {{ row.statusLabel || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成凭证" min-width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.voucherNo"
              type="primary"
              link
              @click="openVoucherDetail(row.voucherNo)"
            >
              {{ row.voucherNo }}
            </el-button>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="generatedEntryCount" label="生成条数" min-width="110" />
        <el-table-column prop="skippedEntryCount" label="跳过条数" min-width="110" />
        <el-table-column label="金额" min-width="120" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="blockedMessage" label="结果说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="previewedAt" label="预览时间" min-width="168" />
        <el-table-column prop="generatedAt" label="生成时间" min-width="168" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openRunDetail(row)">明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="runDetailDialogVisible"
      title="生成记录明细"
      width="1080px"
      destroy-on-close
    >
      <div class="fpt-dialog-meta" v-if="runDetail?.run">
        <span>规则：{{ runDetail.run.ruleName }}</span>
        <span>状态：{{ runDetail.run.statusLabel || runDetail.run.status }}</span>
        <span>凭证：{{ runDetail.run.voucherNo || '--' }}</span>
      </div>

      <el-table v-loading="loading.runDetail" :data="runDetail?.details || []" border class="fpt-table" max-height="460">
        <el-table-column prop="lineNo" label="#" width="58" />
        <el-table-column label="来源科目" min-width="220">
          <template #default="{ row }">
            <div class="fpt-cell-stack">
              <strong>{{ row.sourceSubjectCode || '--' }} {{ row.sourceSubjectName || '' }}</strong>
              <span v-if="row.sourceAssistLabel">{{ row.sourceAssistLabel }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="目标科目" min-width="220">
          <template #default="{ row }">{{ row.targetSubjectCode || '--' }} {{ row.targetSubjectName || '' }}</template>
        </el-table-column>
        <el-table-column prop="metricTypeLabel" label="指标" min-width="120" />
        <el-table-column prop="directionLabel" label="方向" min-width="90" />
        <el-table-column label="金额" min-width="120" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="skipReason" label="跳过原因" min-width="220">
          <template #default="{ row }">
            <span :class="row.skipReason ? 'fpt-skip-reason' : 'fpt-skip-ok'">{{ row.skipReason || '已生成' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  periodTransferApi,
  type FinancePeriodTransferMeta,
  type FinancePeriodTransferRule,
  type FinancePeriodTransferRuleLine,
  type FinancePeriodTransferRuleType,
  type FinancePeriodTransferPreviewResult,
  type FinancePeriodTransferRun,
  type FinancePeriodTransferRunDetail,
  type FinanceVoucherOption
} from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'

const RULE_TYPE_ORDER: FinancePeriodTransferRuleType[] = ['PROFIT', 'CUSTOM', 'MANUFACTURE']

const DEFAULT_RULE_NAMES: Record<FinancePeriodTransferRuleType, string> = {
  PROFIT: '期间损益结转',
  CUSTOM: '自定义比例/公式结转',
  MANUFACTURE: '制造费用结转'
}

const RULE_DESCRIPTIONS: Record<FinancePeriodTransferRuleType, string> = {
  PROFIT: '将收入、成本、费用类科目余额统一结转到本年利润科目。',
  CUSTOM: '按取数指标和比例系数，将多个科目按规则结转到目标科目。',
  MANUFACTURE: '将制造费用及其下级余额按全额、手工比例或项目辅助分配到生产成本。'
}

const financeCompany = useFinanceCompanyStore()
const financePeriod = useFinancePeriodStore()
const router = useRouter()

const meta = ref<FinancePeriodTransferMeta | null>(null)
const activeRuleType = ref<FinancePeriodTransferRuleType>('PROFIT')
const previewResult = ref<FinancePeriodTransferPreviewResult | null>(null)
const runs = ref<FinancePeriodTransferRun[]>([])
const runDetail = ref<FinancePeriodTransferRunDetail | null>(null)
const runsDialogVisible = ref(false)
const runDetailDialogVisible = ref(false)
const previewSignatures = ref<Record<string, string>>({})
const rulesByType = ref<Record<FinancePeriodTransferRuleType, FinancePeriodTransferRule>>({
  PROFIT: createEmptyRule('PROFIT'),
  CUSTOM: createEmptyRule('CUSTOM'),
  MANUFACTURE: createEmptyRule('MANUFACTURE')
})

const loading = reactive({
  meta: false,
  rules: false,
  save: false,
  preview: false,
  generate: false,
  runs: false,
  runDetail: false
})

const canOperate = computed(() =>
  Boolean(financeCompany.currentCompanyId)
  && Boolean(financeCompany.currentCompanyHasActiveAccountSet)
  && financePeriod.hasPeriodContext
)

const ruleTypeOptions = computed(() =>
  meta.value?.ruleTypeOptions?.length
    ? meta.value.ruleTypeOptions
    : RULE_TYPE_ORDER.map((value) => ({
      value,
      label: DEFAULT_RULE_NAMES[value],
      name: DEFAULT_RULE_NAMES[value]
    } as FinanceVoucherOption))
)

const accountOptions = computed(() => meta.value?.accountOptions || [])
const leafAccountOptions = computed(() => accountOptions.value.filter((item) => item.leafFlag !== 0))
const projectOptions = computed(() => meta.value?.projectOptions || [])
const projectClassOptions = computed(() => meta.value?.projectClassOptions || [])
const accountOptionMap = computed(() => new Map(accountOptions.value.map((item) => [item.value, item])))
const projectOptionMap = computed(() => new Map(projectOptions.value.map((item) => [item.value, item])))
const projectClassOptionMap = computed(() => new Map(projectClassOptions.value.map((item) => [item.value, item])))

const currentRule = computed(() => rulesByType.value[activeRuleType.value])
const currentRuleLabel = computed(() => DEFAULT_RULE_NAMES[activeRuleType.value])
const currentRuleDescription = computed(() => RULE_DESCRIPTIONS[activeRuleType.value])
const canGenerate = computed(() => {
  const rule = currentRule.value
  if (!rule || !previewResult.value || previewResult.value.ruleType !== activeRuleType.value) {
    return false
  }
  return previewSignatures.value[activeRuleType.value] === buildRuleSignature(rule)
})

watch(
  () => [
    financeCompany.currentCompanyId,
    financeCompany.currentCompanyHasActiveAccountSet,
    financePeriod.currentYearPeriod
  ] as const,
  () => {
    void refreshPage()
  },
  { immediate: true }
)

function createEmptyRule(ruleType: FinancePeriodTransferRuleType): FinancePeriodTransferRule {
  return {
    companyId: financeCompany.currentCompanyId || '',
    ruleType,
    ruleTypeLabel: DEFAULT_RULE_NAMES[ruleType],
    ruleName: DEFAULT_RULE_NAMES[ruleType],
    enabled: false,
    voucherType: meta.value?.defaultVoucherType || '转',
    includeUnposted: false,
    transferGranularity: ruleType === 'PROFIT' ? 'SUBJECT' : undefined,
    subjectLevel: undefined,
    sourceSubjectCode: ruleType === 'MANUFACTURE' ? '4101' : undefined,
    sourceSubjectName: ruleType === 'MANUFACTURE' ? '制造费用' : undefined,
    targetSubjectCode: undefined,
    targetSubjectName: undefined,
    allocationMode: ruleType === 'MANUFACTURE' ? 'FULL' : undefined,
    regenerateStrategy: 'REPLACE_UNPOSTED',
    lines: []
  }
}

function normalizeRule(rule: Partial<FinancePeriodTransferRule> | undefined, ruleType: FinancePeriodTransferRuleType) {
  const base = createEmptyRule(ruleType)
  return {
    ...base,
    ...rule,
    companyId: trimString(rule?.companyId) || financeCompany.currentCompanyId || base.companyId,
    ruleType,
    ruleTypeLabel: rule?.ruleTypeLabel || DEFAULT_RULE_NAMES[ruleType],
    ruleName: trimString(rule?.ruleName) || DEFAULT_RULE_NAMES[ruleType],
    voucherType: trimString(rule?.voucherType) || meta.value?.defaultVoucherType || base.voucherType,
    enabled: Boolean(rule?.enabled),
    includeUnposted: Boolean(rule?.includeUnposted),
    transferGranularity: (rule?.transferGranularity || base.transferGranularity) as FinancePeriodTransferRule['transferGranularity'],
    allocationMode: (rule?.allocationMode || base.allocationMode) as FinancePeriodTransferRule['allocationMode'],
    lines: Array.isArray(rule?.lines)
      ? rule!.lines.map((line, index) => normalizeRuleLine(line, index))
      : []
  } satisfies FinancePeriodTransferRule
}

function normalizeRuleLine(line: Partial<FinancePeriodTransferRuleLine> | undefined, index: number): FinancePeriodTransferRuleLine {
  return {
    id: line?.id,
    lineNo: line?.lineNo ?? index + 1,
    sourceSubjectCode: trimString(line?.sourceSubjectCode),
    sourceSubjectName: trimString(line?.sourceSubjectName),
    targetSubjectCode: trimString(line?.targetSubjectCode),
    targetSubjectName: trimString(line?.targetSubjectName),
    metricType: line?.metricType || 'ENDING_BALANCE',
    ratio: typeof line?.ratio === 'number' ? line.ratio : undefined,
    allocationProjectClass: trimString(line?.allocationProjectClass),
    allocationProjectId: trimString(line?.allocationProjectId),
    allocationProjectName: trimString(line?.allocationProjectName),
    remark: trimString(line?.remark)
  }
}

function resetTransientState() {
  previewResult.value = null
  runs.value = []
  runDetail.value = null
  runDetailDialogVisible.value = false
  previewSignatures.value = {}
}

async function refreshPage() {
  resetTransientState()
  if (!canOperate.value) {
    meta.value = null
    rulesByType.value = {
      PROFIT: createEmptyRule('PROFIT'),
      CUSTOM: createEmptyRule('CUSTOM'),
      MANUFACTURE: createEmptyRule('MANUFACTURE')
    }
    return
  }
  await Promise.all([loadMeta(), loadRules()])
}

async function loadMeta() {
  if (!canOperate.value) {
    return
  }
  loading.meta = true
  try {
    const res = await periodTransferApi.getMeta({
      companyId: financeCompany.currentCompanyId,
      iyear: financePeriod.currentYear,
      iperiod: financePeriod.currentPeriod
    })
    meta.value = res.data
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载期末结转元数据失败')
  } finally {
    loading.meta = false
  }
}

async function loadRules() {
  if (!canOperate.value || !financeCompany.currentCompanyId) {
    return
  }
  loading.rules = true
  try {
    const res = await periodTransferApi.listRules(financeCompany.currentCompanyId)
    const list = Array.isArray(res.data) ? res.data : []
    rulesByType.value = {
      PROFIT: normalizeRule(list.find((item) => item.ruleType === 'PROFIT'), 'PROFIT'),
      CUSTOM: normalizeRule(list.find((item) => item.ruleType === 'CUSTOM'), 'CUSTOM'),
      MANUFACTURE: normalizeRule(list.find((item) => item.ruleType === 'MANUFACTURE'), 'MANUFACTURE')
    }
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载期末结转规则失败')
  } finally {
    loading.rules = false
  }
}

function changeRuleType(ruleType: FinancePeriodTransferRuleType) {
  activeRuleType.value = ruleType
  previewResult.value = null
  runDetail.value = null
  runDetailDialogVisible.value = false
  if (runsDialogVisible.value) {
    void loadRuns()
  }
}

function resolveRuleStateLabel(ruleType: FinancePeriodTransferRuleType) {
  const rule = rulesByType.value[ruleType]
  return rule?.enabled ? '已启用' : '未启用'
}

function formatOptionLabel(option?: FinanceVoucherOption) {
  if (!option) {
    return ''
  }
  return [option.code, option.name].filter(Boolean).join('  ') || option.label || option.value
}

function resolveAccountName(subjectCode?: string) {
  const option = subjectCode ? accountOptionMap.value.get(subjectCode) : undefined
  return trimString(option?.name) || trimString(option?.label)
}

function resolveProjectClassLabel(projectClass?: string) {
  const option = projectClass ? projectClassOptionMap.value.get(projectClass) : undefined
  return option ? formatOptionLabel(option) : projectClass || ''
}

function addRuleLine(kind: 'CUSTOM' | 'MANUFACTURE') {
  const rule = currentRule.value
  if (!rule) {
    return
  }
  if (kind === 'CUSTOM') {
    rule.lines.push(normalizeRuleLine({ metricType: 'ENDING_BALANCE', ratio: 1 }, rule.lines.length))
    return
  }
  rule.lines.push(
    normalizeRuleLine(
      rule.allocationMode === 'PROJECT'
        ? { ratio: undefined, allocationProjectId: undefined, allocationProjectClass: undefined }
        : { ratio: undefined },
      rule.lines.length
    )
  )
}

function removeRuleLine(kind: 'CUSTOM' | 'MANUFACTURE', index: number) {
  const rule = currentRule.value
  if (!rule) {
    return
  }
  rule.lines.splice(index, 1)
  rule.lines = rule.lines.map((item, lineIndex) => ({ ...item, lineNo: lineIndex + 1 }))
}

function handleProjectSelected(line: FinancePeriodTransferRuleLine) {
  const option = line.allocationProjectId ? projectOptionMap.value.get(line.allocationProjectId) : undefined
  line.allocationProjectName = trimString(option?.name) || trimString(option?.label) || line.allocationProjectName
  line.allocationProjectClass = trimString(option?.parentValue) || line.allocationProjectClass
}

function buildRulePayload(rule: FinancePeriodTransferRule): FinancePeriodTransferRule {
  const payload: FinancePeriodTransferRule = {
    id: rule.id,
    companyId: financeCompany.currentCompanyId || rule.companyId,
    ruleType: rule.ruleType,
    ruleName: trimString(rule.ruleName) || DEFAULT_RULE_NAMES[rule.ruleType],
    enabled: Boolean(rule.enabled),
    voucherType: meta.value?.defaultVoucherType || '转',
    includeUnposted: Boolean(rule.includeUnposted),
    transferGranularity: rule.ruleType === 'PROFIT'
      ? (rule.transferGranularity || 'SUBJECT')
      : undefined,
    subjectLevel: rule.ruleType === 'PROFIT' ? rule.subjectLevel : undefined,
    sourceSubjectCode: rule.ruleType === 'MANUFACTURE' ? trimString(rule.sourceSubjectCode) || '4101' : undefined,
    sourceSubjectName: rule.ruleType === 'MANUFACTURE'
      ? resolveAccountName(trimString(rule.sourceSubjectCode) || '4101') || trimString(rule.sourceSubjectName) || '制造费用'
      : undefined,
    targetSubjectCode: trimString(rule.targetSubjectCode),
    targetSubjectName: resolveAccountName(trimString(rule.targetSubjectCode)),
    allocationMode: rule.ruleType === 'MANUFACTURE' ? (rule.allocationMode || 'FULL') : undefined,
    regenerateStrategy: rule.regenerateStrategy,
    lines: sanitizeRuleLines(rule)
  }
  return payload
}

function sanitizeRuleLines(rule: FinancePeriodTransferRule) {
  if (rule.ruleType === 'PROFIT') {
    return []
  }
  if (rule.ruleType === 'CUSTOM') {
    return rule.lines.map((item, index) => ({
      id: item.id,
      lineNo: index + 1,
      sourceSubjectCode: trimString(item.sourceSubjectCode),
      sourceSubjectName: resolveAccountName(trimString(item.sourceSubjectCode)),
      targetSubjectCode: trimString(item.targetSubjectCode),
      targetSubjectName: resolveAccountName(trimString(item.targetSubjectCode)),
      metricType: item.metricType || 'ENDING_BALANCE',
      ratio: item.ratio,
      remark: trimString(item.remark)
    }))
  }
  if (rule.allocationMode === 'FULL') {
    return []
  }
  return rule.lines.map((item, index) => ({
    id: item.id,
    lineNo: index + 1,
    ratio: item.ratio,
    allocationProjectId: rule.allocationMode === 'PROJECT' ? trimString(item.allocationProjectId) : undefined,
    allocationProjectClass: rule.allocationMode === 'PROJECT'
      ? trimString(item.allocationProjectClass) || trimString(projectOptionMap.value.get(trimString(item.allocationProjectId) || '')?.parentValue)
      : undefined,
    allocationProjectName: rule.allocationMode === 'PROJECT'
      ? trimString(item.allocationProjectName) || trimString(projectOptionMap.value.get(trimString(item.allocationProjectId) || '')?.name)
      : undefined,
    remark: trimString(item.remark)
  }))
}

function buildRuleSignature(rule: FinancePeriodTransferRule) {
  return JSON.stringify(buildRulePayload(rule))
}

async function saveCurrentRule(showSuccess = true) {
  const rule = currentRule.value
  if (!rule || !canOperate.value) {
    return null
  }
  loading.save = true
  try {
    const res = await periodTransferApi.saveRule(buildRulePayload(rule))
    const saved = normalizeRule(res.data, activeRuleType.value)
    rulesByType.value = {
      ...rulesByType.value,
      [saved.ruleType]: saved
    }
    if (showSuccess) {
      ElMessage.success('期末结转规则已保存')
    }
    return saved
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '保存期末结转规则失败')
    return null
  } finally {
    loading.save = false
  }
}

async function previewCurrentRule() {
  const savedRule = await saveCurrentRule(false)
  if (!savedRule?.id || !canOperate.value) {
    return
  }
  loading.preview = true
  try {
    const res = await periodTransferApi.preview({
      companyId: financeCompany.currentCompanyId,
      iyear: financePeriod.currentYear,
      iperiod: financePeriod.currentPeriod,
      ruleId: savedRule.id
    })
    previewResult.value = res.data
    previewSignatures.value = {
      ...previewSignatures.value,
      [savedRule.ruleType]: buildRuleSignature(savedRule)
    }
    ElMessage.success(res.data.message || '结转预览已生成')
  } catch (error: unknown) {
    previewResult.value = null
    ElMessage.error(error instanceof Error ? error.message : '生成期末结转预览失败')
  } finally {
    loading.preview = false
  }
}

async function generateCurrentVoucher() {
  if (!previewResult.value || !canOperate.value) {
    ElMessage.warning('请先生成当前规则的预览结果')
    return
  }
  if (!canGenerate.value) {
    ElMessage.warning('规则已变更，请重新预览后再生成')
    return
  }
  loading.generate = true
  try {
    const res = await periodTransferApi.generate({
      companyId: financeCompany.currentCompanyId,
      iyear: financePeriod.currentYear,
      iperiod: financePeriod.currentPeriod,
      runId: previewResult.value.runId,
      previewToken: previewResult.value.previewToken
    })
    ElMessage.success(res.data.message || '期末结转凭证已生成')
    if (runsDialogVisible.value) {
      await loadRuns()
    }
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '生成期末结转凭证失败')
  } finally {
    loading.generate = false
  }
}

async function openRunsDialog() {
  if (!canOperate.value) {
    return
  }
  runsDialogVisible.value = true
  await loadRuns()
}

async function loadRuns() {
  if (!canOperate.value) {
    return
  }
  loading.runs = true
  try {
    const res = await periodTransferApi.listRuns({
      companyId: financeCompany.currentCompanyId,
      iyear: financePeriod.currentYear,
      iperiod: financePeriod.currentPeriod,
      ruleType: activeRuleType.value
    })
    runs.value = Array.isArray(res.data) ? res.data : []
  } catch (error: unknown) {
    runs.value = []
    ElMessage.error(error instanceof Error ? error.message : '加载期末结转生成记录失败')
  } finally {
    loading.runs = false
  }
}

async function openRunDetail(run: FinancePeriodTransferRun) {
  if (!financeCompany.currentCompanyId) {
    return
  }
  runDetailDialogVisible.value = true
  loading.runDetail = true
  try {
    const res = await periodTransferApi.getRunDetail(financeCompany.currentCompanyId, run.id)
    runDetail.value = res.data
  } catch (error: unknown) {
    runDetail.value = null
    ElMessage.error(error instanceof Error ? error.message : '加载运行明细失败')
  } finally {
    loading.runDetail = false
  }
}

function openVoucherDetail(voucherNo: string) {
  if (!voucherNo) {
    return
  }
  void router.push({
    name: 'finance-query-voucher-detail',
    params: { voucherNo }
  })
}

function resolveRunTagType(status?: string) {
  if (status === 'GENERATED') {
    return 'success'
  }
  if (status === 'PREVIEWED') {
    return 'warning'
  }
  if (status === 'CANCELLED') {
    return 'info'
  }
  return 'default'
}

function formatAmount(value: number | string | undefined | null) {
  const numeric = typeof value === 'number' ? value : Number(value || 0)
  return Number.isFinite(numeric) ? numeric.toFixed(2) : '0.00'
}

function trimString(value?: string | null) {
  const normalized = typeof value === 'string' ? value.trim() : ''
  return normalized || undefined
}
</script>

<style scoped>
.fpt-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fpt-shell {
  border: 1px solid #dbe4ee;
}

.fpt-shell--toolbar {
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.94), transparent 38%),
    linear-gradient(135deg, #fbfcfe 0%, #f4f8fb 52%, #f7fafc 100%);
}

.fpt-toolbar,
.fpt-footer,
.fpt-actions,
.fpt-card-head,
.fpt-inline-actions,
.fpt-preview-summary,
.fpt-dialog-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fpt-toolbar,
.fpt-footer,
.fpt-card-head {
  justify-content: space-between;
}

.fpt-summary {
  display: grid;
  flex: 1;
  min-width: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.fpt-summary__item,
.fpt-message,
.fpt-note {
  border-radius: 14px;
  border: 1px solid #e4ecf4;
  background: #fbfdff;
}

.fpt-summary__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 10px 12px;
  gap: 12px;
}

.fpt-summary__item span,
.fpt-card-head p,
.fpt-dialog-meta,
.fpt-footer__hint,
.fpt-note {
  color: #607184;
  font-size: 13px;
}

.fpt-summary__item strong,
.fpt-card-head strong {
  color: #182430;
  font-size: 14px;
  font-weight: 700;
}

.fpt-grid {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 14px;
}

.fpt-rule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fpt-rule-tab {
  border: 1px solid #d8e3ef;
  border-radius: 18px;
  background: #fff;
  color: #284052;
  padding: 14px 16px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.fpt-rule-tab:hover {
  border-color: #99b9e4;
  box-shadow: 0 10px 22px rgba(47, 122, 229, 0.08);
  transform: translateY(-1px);
}

.fpt-rule-tab.is-active {
  border-color: #2f7ae5;
  background: linear-gradient(180deg, #f1f7ff 0%, #fbfdff 100%);
  box-shadow: 0 12px 24px rgba(47, 122, 229, 0.12);
}

.fpt-rule-tab__label {
  font-size: 14px;
  font-weight: 700;
}

.fpt-rule-tab__meta {
  font-size: 12px;
  color: #6a7d90;
}

.fpt-form,
.fpt-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fpt-form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.fpt-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.fpt-note,
.fpt-message {
  padding: 12px 14px;
  line-height: 1.7;
}

.fpt-preview-summary {
  flex-wrap: wrap;
  color: #35516e;
  font-size: 13px;
  font-weight: 600;
}

.fpt-table {
  width: 100%;
}

.fpt-cell-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fpt-cell-stack strong {
  color: #182430;
  font-size: 13px;
}

.fpt-cell-stack span {
  color: #6a7d90;
  font-size: 12px;
}

.fpt-skip-reason {
  color: #b55324;
}

.fpt-skip-ok {
  color: #20724c;
}

.fpt-footer {
  flex-wrap: wrap;
}

.fpt-footer__hint {
  flex: 1;
  min-width: 280px;
}

.fpt-dialog-meta {
  flex-wrap: wrap;
  margin-bottom: 12px;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 1200px) {
  .fpt-grid {
    grid-template-columns: 1fr;
  }

  .fpt-form-grid,
  .fpt-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .fpt-toolbar,
  .fpt-actions,
  .fpt-inline-actions,
  .fpt-card-head,
  .fpt-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .fpt-summary,
  .fpt-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
