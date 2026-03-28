<template>
  <div class="space-y-6">
    <section class="rounded-[28px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="flex flex-col gap-6 xl:flex-row xl:items-start xl:justify-between">
        <div class="space-y-4">
          <button type="button" class="inline-flex items-center gap-2 text-sm text-blue-600" @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回上一页
          </button>
          <div class="flex flex-wrap items-center gap-3">
            <h1 class="text-3xl font-bold text-slate-800">新增凭证</h1>
            <el-tag type="warning" effect="plain">未记账</el-tag>
            <el-tag effect="plain">未审核</el-tag>
          </div>
          <p class="max-w-3xl leading-7 text-slate-500">
            按用友 U8 常见制单方式录入凭证。当前支持手工录入、校验平衡、暂存草稿与正式保存，审核与记账入口已预留扩展位。
          </p>
          <div class="flex flex-wrap gap-3">
            <el-button :icon="RefreshRight" @click="handleNewVoucher">新增</el-button>
            <el-button :icon="Files" @click="handleSaveDraft">暂存</el-button>
            <el-button :icon="CircleCheck" @click="handleValidate">校验平衡</el-button>
            <el-button type="primary" :loading="saving" :icon="Select" @click="handleSave">保存</el-button>
            <el-button @click="goVoucherList">返回列表</el-button>
          </div>
        </div>

        <div class="toolbar-secondary">
          <el-button :icon="DocumentCopy" @click="showStaticAction('复制凭证')">复制凭证</el-button>
          <el-button :icon="Upload" @click="showStaticAction('导入模板')">导入模板</el-button>
          <el-button :icon="Paperclip" @click="showStaticAction('附件')">附件</el-button>
          <el-button :icon="Printer" @click="showStaticAction('打印')">打印</el-button>
          <el-dropdown trigger="click">
            <el-button :icon="MoreFilled">更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showStaticAction('红字冲销')">红字冲销</el-dropdown-item>
                <el-dropdown-item @click="showStaticAction('自动编号策略切换')">自动编号策略切换</el-dropdown-item>
                <el-dropdown-item @click="showStaticAction('外部凭证字段')">外部凭证字段</el-dropdown-item>
                <el-dropdown-item @click="showStaticAction('打印模板')">打印模板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !border-slate-100 !shadow-sm">
      <template #header>
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p class="text-lg font-semibold text-slate-800">凭证头</p>
            <p class="mt-1 text-sm text-slate-400">制单信息与凭证编号由公司、期间、凭证类别共同确定。</p>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <div class="voucher-code-chip">
              <span class="label">凭证字号</span>
              <strong>{{ voucherCodeText }}</strong>
            </div>
            <div class="voucher-code-chip">
              <span class="label">平衡状态</span>
              <strong :class="isBalanced ? 'text-emerald-600' : 'text-amber-600'">{{ isBalanced ? '已平衡' : '待校验' }}</strong>
            </div>
          </div>
        </div>
      </template>

      <div class="grid grid-cols-1 gap-5 lg:grid-cols-2 xl:grid-cols-4">
        <el-form-item label="公司主体" required>
          <el-select v-model="form.companyId" filterable placeholder="请选择公司主体">
            <el-option
              v-for="item in voucherMeta?.companyOptions || []"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="会计期间" required>
          <el-input-number v-model="form.iperiod" :min="1" :max="12" :controls="false" class="w-full" />
        </el-form-item>

        <el-form-item label="凭证类别" required>
          <el-select v-model="form.csign" placeholder="请选择凭证类别">
            <el-option
              v-for="item in voucherMeta?.voucherTypeOptions || []"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="凭证编号">
          <el-input :model-value="String(form.inoId || '')" readonly />
        </el-form-item>

        <el-form-item label="制单日期" required>
          <el-date-picker
            v-model="form.dbillDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            class="w-full"
          />
        </el-form-item>

        <el-form-item label="附件张数">
          <el-input-number v-model="form.idoc" :min="0" :controls="false" class="w-full" />
        </el-form-item>

        <el-form-item label="制单人">
          <el-input v-model="form.cbill" readonly />
        </el-form-item>

        <el-form-item label="校验结果">
          <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm">
            <span v-if="validationErrors.length === 0" class="text-emerald-600">当前无阻断错误，可直接保存。</span>
            <span v-else class="text-amber-600">存在 {{ validationErrors.length }} 个待处理问题，请先修正。</span>
          </div>
        </el-form-item>

        <el-form-item label="凭证头自定义项 1" class="xl:col-span-2">
          <el-input v-model="form.ctext1" placeholder="例如：单据来源、业务批次" />
        </el-form-item>

        <el-form-item label="凭证头自定义项 2" class="xl:col-span-2">
          <el-input v-model="form.ctext2" placeholder="例如：凭证备注、来源说明" />
        </el-form-item>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !border-slate-100 !shadow-sm">
      <template #header>
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p class="text-lg font-semibold text-slate-800">分录明细</p>
            <p class="mt-1 text-sm text-slate-400">支持摘要、科目、辅助核算、币种汇率与数量金额联动录入。</p>
          </div>
          <div class="flex flex-wrap gap-3">
            <el-button :icon="Plus" @click="appendEntry">新增分录</el-button>
            <el-button @click="fillSampleVoucher">填充示例</el-button>
          </div>
        </div>
      </template>

      <div class="voucher-table-wrap">
        <el-table :data="form.entries" border row-key="localId" class="voucher-entry-table">
          <el-table-column label="#" width="60" fixed="left">
            <template #default="{ $index }">
              <span class="font-medium text-slate-600">{{ $index + 1 }}</span>
            </template>
          </el-table-column>

          <el-table-column label="摘要" min-width="220" fixed="left">
            <template #default="{ row }">
              <el-input v-model="row.cdigest" placeholder="请输入摘要" />
            </template>
          </el-table-column>

          <el-table-column label="科目编码" min-width="220" fixed="left">
            <template #default="{ row }">
              <el-select v-model="row.ccode" filterable placeholder="选择科目">
                <el-option
                  v-for="item in voucherMeta?.accountOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="部门" min-width="190">
            <template #default="{ row }">
              <el-select v-model="row.cdeptId" filterable clearable placeholder="部门">
                <el-option
                  v-for="item in voucherMeta?.departmentOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="职员" min-width="190">
            <template #default="{ row }">
              <el-select v-model="row.cpersonId" filterable clearable placeholder="职员">
                <el-option
                  v-for="item in voucherMeta?.employeeOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="客户" min-width="190">
            <template #default="{ row }">
              <el-select v-model="row.ccusId" filterable clearable placeholder="客户">
                <el-option
                  v-for="item in voucherMeta?.customerOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="供应商" min-width="190">
            <template #default="{ row }">
              <el-select v-model="row.csupId" filterable clearable placeholder="供应商">
                <el-option
                  v-for="item in voucherMeta?.supplierOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="项目大类" min-width="180">
            <template #default="{ row }">
              <el-select v-model="row.citemClass" filterable clearable placeholder="项目大类">
                <el-option
                  v-for="item in voucherMeta?.projectClassOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="项目" min-width="180">
            <template #default="{ row }">
              <el-select v-model="row.citemId" filterable clearable placeholder="项目">
                <el-option
                  v-for="item in voucherMeta?.projectOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="币种" min-width="130">
            <template #default="{ row }">
              <el-select v-model="row.cexchName" placeholder="币种">
                <el-option
                  v-for="item in voucherMeta?.currencyOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="汇率" min-width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.nfrat" :min="0.000001" :precision="6" :controls="false" class="w-full" />
            </template>
          </el-table-column>

          <el-table-column label="借方金额" min-width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.md" :min="0" :precision="2" :controls="false" class="w-full" />
            </template>
          </el-table-column>

          <el-table-column label="贷方金额" min-width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.mc" :min="0" :precision="2" :controls="false" class="w-full" />
            </template>
          </el-table-column>

          <el-table-column label="数量借方" min-width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.ndS" :min="0" :precision="6" :controls="false" class="w-full" />
            </template>
          </el-table-column>

          <el-table-column label="数量贷方" min-width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.ncS" :min="0" :precision="6" :controls="false" class="w-full" />
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row, $index }">
              <div class="flex items-center gap-2">
                <el-button circle :icon="Plus" @click="insertEntryAfter($index)" />
                <el-button circle :icon="Delete" @click="removeEntry(row.localId)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <div class="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr),360px]">
      <el-card class="!rounded-3xl !border-slate-100 !shadow-sm">
        <template #header>
          <div class="flex items-center gap-2 text-slate-800">
            <el-icon><Warning /></el-icon>
            <span class="font-semibold">校验与提示</span>
          </div>
        </template>

        <div v-if="validationErrors.length" class="space-y-3">
          <div v-for="item in validationErrors" :key="item" class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            {{ item }}
          </div>
        </div>
        <el-empty v-else description="当前校验通过，可继续保存或暂存草稿。" />
      </el-card>

      <el-card class="!rounded-3xl !border-slate-100 !shadow-sm">
        <template #header>
          <div class="flex items-center gap-2 text-slate-800">
            <el-icon><DataAnalysis /></el-icon>
            <span class="font-semibold">汇总区</span>
          </div>
        </template>

        <div class="summary-grid">
          <div class="summary-item">
            <span>有效分录</span>
            <strong>{{ effectiveEntryCount }}</strong>
          </div>
          <div class="summary-item">
            <span>借方合计</span>
            <strong>{{ moneyText(totalDebit) }}</strong>
          </div>
          <div class="summary-item">
            <span>贷方合计</span>
            <strong>{{ moneyText(totalCredit) }}</strong>
          </div>
          <div class="summary-item">
            <span>差额</span>
            <strong :class="balanceGap === 0 ? 'text-emerald-600' : 'text-amber-600'">{{ moneyText(balanceGap) }}</strong>
          </div>
          <div class="summary-item">
            <span>草稿状态</span>
            <strong>{{ hasDraft ? '已暂存' : '未暂存' }}</strong>
          </div>
          <div class="summary-item">
            <span>最后校验</span>
            <strong>{{ lastValidatedAt || '尚未校验' }}</strong>
          </div>
        </div>

        <div class="mt-6 flex flex-wrap gap-3">
          <el-button :icon="CircleCheck" @click="handleValidate">重新校验</el-button>
          <el-button type="primary" :loading="saving" :icon="Select" @click="handleSave">保存凭证</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  CircleCheck,
  DataAnalysis,
  Delete,
  DocumentCopy,
  Files,
  MoreFilled,
  Paperclip,
  Plus,
  Printer,
  RefreshRight,
  Select,
  Upload,
  Warning
} from '@element-plus/icons-vue'
import {
  financeApi,
  type FinanceVoucherEntry,
  type FinanceVoucherForm,
  type FinanceVoucherMeta,
  type FinanceVoucherSavePayload
} from '@/api'

type VoucherEntryRow = FinanceVoucherEntry & {
  localId: string
}

type VoucherFormState = Omit<FinanceVoucherForm, 'entries'> & {
  entries: VoucherEntryRow[]
}

const DRAFT_STORAGE_KEY = 'finance-new-voucher-draft'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const initializing = ref(false)
const voucherMeta = ref<FinanceVoucherMeta | null>(null)
const validationErrors = ref<string[]>([])
const hasDraft = ref(false)
const lastValidatedAt = ref('')
let entrySeed = 0

const form = reactive<VoucherFormState>({
  companyId: '',
  iperiod: 1,
  csign: '记',
  inoId: undefined,
  dbillDate: '',
  idoc: 0,
  cbill: '',
  ctext1: '',
  ctext2: '',
  entries: []
})

const effectiveRows = computed(() => form.entries.filter((item) => !isEntryBlank(item)))
const effectiveEntryCount = computed(() => effectiveRows.value.length)
const totalDebit = computed(() => sumRows(effectiveRows.value, 'md'))
const totalCredit = computed(() => sumRows(effectiveRows.value, 'mc'))
const balanceGap = computed(() => Number((totalDebit.value - totalCredit.value).toFixed(2)))
const isBalanced = computed(() => effectiveEntryCount.value >= 2 && balanceGap.value === 0 && validationErrors.value.length === 0)
const voucherCodeText = computed(() => `${form.csign || '--'}-${String(form.inoId || '').padStart(4, '0') || '----'}`)

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

watch(
  () => form.dbillDate,
  (value) => {
    if (initializing.value) {
      return
    }
    const nextPeriod = inferPeriod(value)
    if (nextPeriod) {
      form.iperiod = nextPeriod
    }
  }
)

watch(
  () => [form.companyId, form.dbillDate, form.csign] as const,
  async () => {
    if (initializing.value || loading.value || !voucherMeta.value) {
      return
    }
    await refreshSuggestedVoucherNo()
  }
)

onMounted(async () => {
  await loadMeta()
})

async function loadMeta() {
  loading.value = true
  initializing.value = true
  try {
    const res = await financeApi.getVoucherMeta()
    voucherMeta.value = res.data
    const draft = readDraft()
    if (draft) {
      applyDraft(draft, res.data)
      hasDraft.value = true
      ElMessage.success('已恢复暂存草稿')
    } else {
      resetFormFromMeta(res.data)
      hasDraft.value = false
    }
    validationErrors.value = []
    lastValidatedAt.value = ''
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载凭证配置失败'))
  } finally {
    initializing.value = false
    loading.value = false
  }
}

function resetFormFromMeta(meta: FinanceVoucherMeta) {
  form.companyId = meta.defaultCompanyId || ''
  form.iperiod = meta.defaultPeriod
  form.csign = meta.defaultVoucherType
  form.inoId = meta.suggestedVoucherNo
  form.dbillDate = meta.defaultBillDate
  form.idoc = meta.defaultAttachedDocCount
  form.cbill = meta.defaultMaker
  form.ctext1 = ''
  form.ctext2 = ''
  form.entries = [createEntry(meta.defaultCurrency, 1), createEntry(meta.defaultCurrency, 2)]
}

function applyDraft(draft: FinanceVoucherSavePayload, meta: FinanceVoucherMeta) {
  form.companyId = draft.companyId || meta.defaultCompanyId || ''
  form.iperiod = draft.iperiod || meta.defaultPeriod
  form.csign = draft.csign || meta.defaultVoucherType
  form.inoId = draft.inoId || meta.suggestedVoucherNo
  form.dbillDate = draft.dbillDate || meta.defaultBillDate
  form.idoc = draft.idoc ?? meta.defaultAttachedDocCount
  form.cbill = draft.cbill || meta.defaultMaker
  form.ctext1 = draft.ctext1 || ''
  form.ctext2 = draft.ctext2 || ''
  form.entries =
    draft.entries?.length > 0
      ? draft.entries.map((item, index) => createEntryFromValue(item, meta.defaultCurrency, index + 1))
      : [createEntry(meta.defaultCurrency, 1), createEntry(meta.defaultCurrency, 2)]
}

function createEntry(defaultCurrency: string, rowNo: number): VoucherEntryRow {
  entrySeed += 1
  return {
    localId: `entry-${Date.now()}-${entrySeed}`,
    inid: rowNo,
    cdigest: '',
    ccode: '',
    cdeptId: '',
    cpersonId: '',
    ccusId: '',
    csupId: '',
    citemClass: '',
    citemId: '',
    cexchName: defaultCurrency,
    nfrat: 1,
    md: undefined,
    mc: undefined,
    ndS: undefined,
    ncS: undefined
  }
}

function createEntryFromValue(entry: FinanceVoucherEntry, defaultCurrency: string, rowNo: number): VoucherEntryRow {
  const next = createEntry(defaultCurrency, rowNo)
  return {
    ...next,
    ...entry,
    inid: rowNo,
    cexchName: entry.cexchName || defaultCurrency,
    nfrat: entry.nfrat ?? 1
  }
}

function readDraft(): FinanceVoucherSavePayload | null {
  const raw = window.sessionStorage.getItem(DRAFT_STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as FinanceVoucherSavePayload
  } catch {
    window.sessionStorage.removeItem(DRAFT_STORAGE_KEY)
    return null
  }
}

function writeDraft() {
  window.sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(buildPayload(true)))
  hasDraft.value = true
}

function clearDraft() {
  window.sessionStorage.removeItem(DRAFT_STORAGE_KEY)
  hasDraft.value = false
}

function buildPayload(includeBlankRows = false): FinanceVoucherSavePayload {
  const entries = (includeBlankRows ? form.entries : effectiveRows.value).map((item, index) => ({
    inid: index + 1,
    cdigest: (item.cdigest || '').trim(),
    ccode: item.ccode || '',
    cdeptId: item.cdeptId || undefined,
    cpersonId: item.cpersonId || undefined,
    ccusId: item.ccusId || undefined,
    csupId: item.csupId || undefined,
    citemClass: item.citemClass || undefined,
    citemId: item.citemId || undefined,
    cexchName: item.cexchName || voucherMeta.value?.defaultCurrency || 'CNY',
    nfrat: normalizeDecimal(item.nfrat),
    md: normalizeDecimal(item.md),
    mc: normalizeDecimal(item.mc),
    ndS: normalizeQuantity(item.ndS),
    ncS: normalizeQuantity(item.ncS)
  }))

  return {
    companyId: form.companyId,
    iperiod: form.iperiod,
    csign: form.csign,
    inoId: form.inoId,
    dbillDate: form.dbillDate,
    idoc: form.idoc,
    cbill: form.cbill,
    ctext1: (form.ctext1 || '').trim(),
    ctext2: (form.ctext2 || '').trim(),
    entries
  }
}

function validateVoucher(showToast = false) {
  const errors: string[] = []
  const entries = effectiveRows.value

  if (!form.companyId) {
    errors.push('请选择公司主体')
  }
  if (!form.dbillDate) {
    errors.push('请选择制单日期')
  }
  if (!form.csign) {
    errors.push('请选择凭证类别')
  }
  if (!form.iperiod || form.iperiod < 1 || form.iperiod > 12) {
    errors.push('会计期间必须在 1 到 12 之间')
  }
  if (entries.length < 2) {
    errors.push('至少需要两条有效分录')
  }

  entries.forEach((row, index) => {
    const rowNo = index + 1
    const debit = normalizeDecimal(row.md) || 0
    const credit = normalizeDecimal(row.mc) || 0
    const qtyDebit = normalizeQuantity(row.ndS)
    const qtyCredit = normalizeQuantity(row.ncS)

    if (!(row.cdigest || '').trim()) {
      errors.push(`第 ${rowNo} 行摘要不能为空`)
    }
    if (!row.ccode) {
      errors.push(`第 ${rowNo} 行请选择科目编码`)
    }
    if (debit > 0 && credit > 0) {
      errors.push(`第 ${rowNo} 行借贷不能同时填写`)
    }
    if (debit === 0 && credit === 0) {
      errors.push(`第 ${rowNo} 行借方或贷方至少填写一项`)
    }
    if ((row.nfrat ?? 1) <= 0) {
      errors.push(`第 ${rowNo} 行汇率必须大于 0`)
    }
    if ((qtyDebit ?? 0) > 0 && (qtyCredit ?? 0) > 0) {
      errors.push(`第 ${rowNo} 行数量借贷不能同时填写`)
    }
    if ((qtyDebit ?? 0) < 0 || (qtyCredit ?? 0) < 0) {
      errors.push(`第 ${rowNo} 行数量不能为负数`)
    }
  })

  if (entries.length >= 2 && Number((totalDebit.value - totalCredit.value).toFixed(2)) !== 0) {
    errors.push('借方合计必须等于贷方合计')
  }

  validationErrors.value = Array.from(new Set(errors))
  lastValidatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (showToast) {
    if (validationErrors.value.length > 0) {
      ElMessage.warning(validationErrors.value[0])
    } else {
      ElMessage.success('凭证校验通过，借贷已平衡')
    }
  }

  return validationErrors.value.length === 0
}

function isEntryBlank(row: VoucherEntryRow) {
  return !row.cdigest?.trim() &&
    !row.ccode &&
    !row.cdeptId &&
    !row.cpersonId &&
    !row.ccusId &&
    !row.csupId &&
    !row.citemClass &&
    !row.citemId &&
    !row.md &&
    !row.mc &&
    !row.ndS &&
    !row.ncS
}

function sumRows(rows: FinanceVoucherEntry[], field: 'md' | 'mc') {
  return Number(
    rows
      .reduce((total, row) => total + (normalizeDecimal(row[field]) || 0), 0)
      .toFixed(2)
  )
}

function normalizeDecimal(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value)) || Number(value) === 0) {
    return undefined
  }
  return Number(Number(value).toFixed(2))
}

function normalizeQuantity(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value)) || Number(value) === 0) {
    return undefined
  }
  return Number(Number(value).toFixed(6))
}

function inferPeriod(value: string) {
  if (!value) {
    return undefined
  }
  const parts = value.split('-')
  if (parts.length < 2) {
    return undefined
  }
  const month = Number(parts[1])
  return Number.isFinite(month) && month >= 1 && month <= 12 ? month : undefined
}

async function refreshSuggestedVoucherNo() {
  try {
    const res = await financeApi.getVoucherMeta({
      companyId: form.companyId,
      billDate: form.dbillDate,
      csign: form.csign
    })
    voucherMeta.value = res.data
    form.inoId = res.data.suggestedVoucherNo
    if (!form.cbill) {
      form.cbill = res.data.defaultMaker
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '刷新凭证编号失败'))
  }
}

function appendEntry() {
  form.entries.push(createEntry(voucherMeta.value?.defaultCurrency || 'CNY', form.entries.length + 1))
}

function insertEntryAfter(index: number) {
  form.entries.splice(index + 1, 0, createEntry(voucherMeta.value?.defaultCurrency || 'CNY', index + 2))
}

function removeEntry(localId: string) {
  if (form.entries.length <= 2) {
    ElMessage.warning('至少保留两行分录')
    return
  }
  form.entries = form.entries.filter((item) => item.localId !== localId)
}

async function handleNewVoucher() {
  try {
    await ElMessageBox.confirm('将清空当前录入内容并开始新的凭证，是否继续？', '新增凭证', {
      type: 'warning',
      confirmButtonText: '继续',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  clearDraft()
  if (voucherMeta.value) {
    resetFormFromMeta(voucherMeta.value)
    validationErrors.value = []
    lastValidatedAt.value = ''
  } else {
    await loadMeta()
  }
}

function handleSaveDraft() {
  writeDraft()
  ElMessage.success('凭证草稿已暂存')
}

function handleValidate() {
  validateVoucher(true)
}

async function handleSave() {
  if (!validateVoucher(true)) {
    return
  }

  saving.value = true
  try {
    const currentContext = {
      companyId: form.companyId,
      billDate: form.dbillDate,
      csign: form.csign
    }
    const res = await financeApi.createVoucher(buildPayload())
    clearDraft()
    ElMessage.success(`凭证保存成功：${res.data.voucherNo}`)
    const nextMeta = await financeApi.getVoucherMeta(currentContext)
    voucherMeta.value = nextMeta.data
    resetFormFromMeta(nextMeta.data)
    validationErrors.value = []
    lastValidatedAt.value = ''
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存凭证失败'))
  } finally {
    saving.value = false
  }
}

function fillSampleVoucher() {
  const currency = voucherMeta.value?.defaultCurrency || 'CNY'
  form.entries = [
    createEntryFromValue(
      {
        cdigest: '报销办公用品',
        ccode: '5601',
        cdeptId: voucherMeta.value?.departmentOptions?.[0]?.value,
        cpersonId: voucherMeta.value?.employeeOptions?.[0]?.value,
        cexchName: currency,
        nfrat: 1,
        md: 1280,
        mc: undefined
      },
      currency,
      1
    ),
    createEntryFromValue(
      {
        cdigest: '支付办公用品报销',
        ccode: '1002',
        cexchName: currency,
        nfrat: 1,
        md: undefined,
        mc: 1280
      },
      currency,
      2
    )
  ]
  validationErrors.value = []
  lastValidatedAt.value = ''
}

function showStaticAction(label: string) {
  ElMessage.info(`${label}入口已预留，后续可继续扩展。`)
}

function goVoucherList() {
  router.push('/finance/general-ledger/query-voucher')
}

function goBack() {
  router.back()
}

function moneyText(value: number) {
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}
</script>

<style scoped>
.toolbar-secondary {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.voucher-code-chip {
  display: flex;
  min-width: 148px;
  flex-direction: column;
  gap: 4px;
  border-radius: 18px;
  border: 1px solid #dbeafe;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
  padding: 12px 16px;
}

.voucher-code-chip .label {
  font-size: 12px;
  color: #64748b;
}

.voucher-table-wrap {
  overflow-x: auto;
}

.voucher-entry-table {
  min-width: 1980px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 14px 16px;
}

.summary-item span {
  font-size: 12px;
  color: #64748b;
}

.summary-item strong {
  font-size: 18px;
  color: #0f172a;
}
</style>
