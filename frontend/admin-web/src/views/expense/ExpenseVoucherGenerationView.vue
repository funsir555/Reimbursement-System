<template>
  <div class="voucher-generation-page space-y-5">
    <el-tabs v-model="activeTab" class="voucher-tabs" data-testid="voucher-generation-tabs" @tab-change="handleTabChange">
      <el-tab-pane v-if="canMappingView" label="凭证科目映射" name="mapping">
        <div class="space-y-5">
          <el-card class="!rounded-3xl !shadow-sm">
            <div class="mb-4 flex items-center justify-between">
              <div>
                <h2 class="text-lg font-semibold text-slate-800">模板级统一贷方策略</h2>
                <p class="mt-1 text-sm text-slate-400">按“公司 + 报销模板”维护统一贷方科目、凭证类别与摘要规则。</p>
              </div>
              <el-button v-if="canMappingEdit" data-testid="voucher-mapping-add-policy" type="primary" :icon="Plus" @click="openTemplatePolicyDialog()">新增模板策略</el-button>
            </div>
            <el-table :data="templatePolicyPage.items" style="width: 100%" v-loading="loading.templatePolicies">
              <el-table-column prop="companyName" label="公司" min-width="180" show-overflow-tooltip />
              <el-table-column prop="templateName" label="报销模板" min-width="180" show-overflow-tooltip />
              <el-table-column prop="creditAccountName" label="统一贷方科目" min-width="180" show-overflow-tooltip />
              <el-table-column prop="voucherTypeLabel" label="凭证类别" width="120" />
              <el-table-column prop="summaryRule" label="摘要规则" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
              <el-table-column v-if="canMappingEdit" label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openTemplatePolicyDialog(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="table-pagination">
              <el-pagination v-model:current-page="templatePolicyQuery.page" v-model:page-size="templatePolicyQuery.pageSize" :total="templatePolicyPage.total" layout="total, sizes, prev, pager, next" :page-sizes="[5,10,20,50]" @change="loadTemplatePolicies" />
            </div>
          </el-card>

          <el-card class="!rounded-3xl !shadow-sm">
            <div class="mb-4 flex items-center justify-between">
              <div>
                <h2 class="text-lg font-semibold text-slate-800">费用类型借方映射</h2>
                <p class="mt-1 text-sm text-slate-400">按“公司 + 报销模板 + 费用类型”维护借方科目，缺失映射时推送会被阻断。</p>
              </div>
              <el-button v-if="canMappingEdit" data-testid="voucher-mapping-add-subject" type="primary" plain :icon="Plus" @click="openSubjectMappingDialog()">新增费用映射</el-button>
            </div>
            <el-table :data="subjectMappingPage.items" style="width: 100%" v-loading="loading.subjectMappings">
              <el-table-column prop="companyName" label="公司" min-width="160" show-overflow-tooltip />
              <el-table-column prop="templateName" label="报销模板" min-width="160" show-overflow-tooltip />
              <el-table-column prop="expenseTypeName" label="费用类型" min-width="160" show-overflow-tooltip />
              <el-table-column prop="debitAccountName" label="借方科目" min-width="180" show-overflow-tooltip />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
              <el-table-column v-if="canMappingEdit" label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openSubjectMappingDialog(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="table-pagination">
              <el-pagination v-model:current-page="subjectMappingQuery.page" v-model:page-size="subjectMappingQuery.pageSize" :total="subjectMappingPage.total" layout="total, sizes, prev, pager, next" :page-sizes="[5,10,20,50]" @change="loadSubjectMappings" />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canPushView" label="推送凭证" name="push">
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">待推送单据池</h2>
              <p class="mt-1 text-sm text-slate-400">仅展示已审批且未推送成功的报销单，据此生成总账凭证。</p>
            </div>
            <div class="flex gap-3">
              <el-button data-testid="voucher-push-refresh" :icon="RefreshRight" @click="loadPushDocuments">刷新</el-button>
              <el-button v-if="canPushExecute" data-testid="voucher-push-batch" type="primary" :icon="Promotion" @click="pushSelected">批量推送</el-button>
            </div>
          </div>
          <el-table :data="pushDocumentPage.items" style="width: 100%" v-loading="loading.pushDocuments" @selection-change="handlePushSelectionChange">
            <el-table-column v-if="canPushExecute" type="selection" width="48" />
            <el-table-column prop="documentCode" label="单据号" min-width="160" show-overflow-tooltip />
            <el-table-column prop="templateName" label="报销模板" min-width="160" show-overflow-tooltip />
            <el-table-column prop="submitterName" label="申请人" min-width="120" show-overflow-tooltip />
            <el-table-column prop="companyName" label="公司" min-width="160" show-overflow-tooltip />
            <el-table-column label="总金额" min-width="120">
              <template #default="{ row }">{{ moneyText(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="expenseSummary" label="费用类型汇总" min-width="260" show-overflow-tooltip />
            <el-table-column prop="finishedAt" label="审批完成时间" min-width="180" />
            <el-table-column label="推送状态" min-width="120">
              <template #default="{ row }">
                <el-tag :type="row.pushStatus === 'SUCCESS' ? 'success' : row.pushStatus === 'FAILED' ? 'danger' : 'info'" effect="plain">{{ row.pushStatusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button v-if="canPushExecute" link type="primary" :disabled="!row.canPush" @click="pushSingle(row.documentCode)">推送</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination v-model:current-page="pushDocumentQuery.page" v-model:page-size="pushDocumentQuery.pageSize" :total="pushDocumentPage.total" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @change="loadPushDocuments" />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canQueryView" label="凭证查询" name="query">
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">推送结果查询</h2>
              <p class="mt-1 text-sm text-slate-400">默认仅查询本模块推送记录，可下钻查看来源单据、分录快照和总账凭证详情。</p>
            </div>
            <el-button data-testid="voucher-query-refresh" :icon="RefreshRight" @click="loadGeneratedRecords">刷新</el-button>
          </div>
          <el-table :data="generatedRecordPage.items" style="width: 100%" v-loading="loading.generatedRecords">
            <el-table-column prop="documentCode" label="单据号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="templateName" label="报销模板" min-width="150" show-overflow-tooltip />
            <el-table-column prop="companyName" label="公司" min-width="150" show-overflow-tooltip />
            <el-table-column label="金额" min-width="120">
              <template #default="{ row }">{{ moneyText(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="voucherNo" label="凭证号" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" min-width="110">
              <template #default="{ row }">
                <el-tag :type="row.pushStatus === 'SUCCESS' ? 'success' : 'danger'" effect="plain">{{ row.pushStatusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="pushedAt" label="推送时间" min-width="180" />
            <el-table-column prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openGeneratedDetail(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination v-model:current-page="generatedRecordQuery.page" v-model:page-size="generatedRecordQuery.pageSize" :total="generatedRecordPage.total" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @change="loadGeneratedRecords" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="templatePolicyDialog.visible" :title="templatePolicyDialog.editingId ? '编辑模板策略' : '新增模板策略'" width="720px" destroy-on-close>
      <div class="grid gap-4 md:grid-cols-2">
        <el-select v-model="templatePolicyDialog.form.companyId" filterable placeholder="公司">
          <el-option v-for="item in meta.companyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="templatePolicyDialog.form.templateCode" filterable placeholder="报销模板">
          <el-option v-for="item in meta.templateOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="templatePolicyDialog.form.creditAccountCode" filterable placeholder="统一贷方科目">
          <el-option v-for="item in meta.accountOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="templatePolicyDialog.form.voucherType" placeholder="凭证类别">
          <el-option v-for="item in meta.voucherTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="templatePolicyDialog.form.summaryRule" class="md:col-span-2" placeholder="摘要规则，例如：报销单${documentCode}-${expenseTypeName}" />
        <el-switch v-model="templatePolicyDialog.form.enabled" :active-value="1" :inactive-value="0" inline-prompt active-text="启用" inactive-text="停用" />
      </div>
      <template #footer>
        <el-button @click="templatePolicyDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveTemplatePolicy" @click="submitTemplatePolicy">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="subjectMappingDialog.visible" :title="subjectMappingDialog.editingId ? '编辑费用映射' : '新增费用映射'" width="720px" destroy-on-close>
      <div class="grid gap-4 md:grid-cols-2">
        <el-select v-model="subjectMappingDialog.form.companyId" filterable placeholder="公司">
          <el-option v-for="item in meta.companyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="subjectMappingDialog.form.templateCode" filterable placeholder="报销模板">
          <el-option v-for="item in meta.templateOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="subjectMappingDialog.form.expenseTypeCode" filterable placeholder="费用类型">
          <el-option v-for="item in meta.expenseTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="subjectMappingDialog.form.debitAccountCode" filterable placeholder="借方科目">
          <el-option v-for="item in meta.accountOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-switch v-model="subjectMappingDialog.form.enabled" :active-value="1" :inactive-value="0" inline-prompt active-text="启用" inactive-text="停用" />
      </div>
      <template #footer>
        <el-button @click="subjectMappingDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveSubjectMapping" @click="submitSubjectMapping">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="generatedDetailDrawer.visible" title="推送记录详情" size="58%" destroy-on-close>
      <div v-if="generatedDetailDrawer.detail" class="space-y-4">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="单据号">{{ generatedDetailDrawer.detail.record.documentCode }}</el-descriptions-item>
          <el-descriptions-item label="凭证号">{{ generatedDetailDrawer.detail.record.voucherNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公司">{{ generatedDetailDrawer.detail.record.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="推送状态">{{ generatedDetailDrawer.detail.record.pushStatusLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模板">{{ generatedDetailDrawer.detail.record.templateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="推送时间">{{ generatedDetailDrawer.detail.record.pushedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="失败原因" :span="2">{{ generatedDetailDrawer.detail.record.failureReason || '无' }}</el-descriptions-item>
        </el-descriptions>
        <el-card class="!rounded-3xl !shadow-sm">
          <template #header>分录快照</template>
          <el-table :data="generatedDetailDrawer.detail.entries" style="width: 100%">
            <el-table-column prop="entryNo" label="序号" width="80" />
            <el-table-column prop="direction" label="方向" width="100" />
            <el-table-column prop="digest" label="摘要" min-width="220" show-overflow-tooltip />
            <el-table-column prop="accountName" label="科目" min-width="180" show-overflow-tooltip />
            <el-table-column prop="expenseTypeName" label="费用类型" min-width="140" show-overflow-tooltip />
            <el-table-column label="金额" min-width="120">
              <template #default="{ row }">{{ moneyText(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Promotion, RefreshRight } from '@element-plus/icons-vue'
import {
  expenseVoucherGenerationApi,
  type PageResult,
  type VoucherGeneratedDetail,
  type VoucherGeneratedRecord,
  type VoucherGenerationMeta,
  type VoucherPushDocument,
  type VoucherSubjectMapping,
  type VoucherSubjectMappingPayload,
  type VoucherTemplatePolicy,
  type VoucherTemplatePolicyPayload
} from '@/api'
import { formatMoney } from '@/utils/money'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const canMappingView = computed(() => hasPermission('expense:voucher_generation:mapping:view', permissionCodes.value) || hasPermission('expense:voucher_generation:view', permissionCodes.value))
const canMappingEdit = computed(() => hasPermission('expense:voucher_generation:mapping:edit', permissionCodes.value))
const canPushView = computed(() => hasPermission('expense:voucher_generation:push:view', permissionCodes.value) || hasPermission('expense:voucher_generation:view', permissionCodes.value))
const canPushExecute = computed(() => hasPermission('expense:voucher_generation:push:execute', permissionCodes.value))
const canQueryView = computed(() => hasPermission('expense:voucher_generation:query:view', permissionCodes.value) || hasPermission('expense:voucher_generation:view', permissionCodes.value))

const availableTabs = computed(() => [
  canMappingView.value ? 'mapping' : '',
  canPushView.value ? 'push' : '',
  canQueryView.value ? 'query' : ''
].filter(Boolean))

const activeTab = ref('mapping')
const meta = reactive<VoucherGenerationMeta>({
  companyOptions: [],
  templateOptions: [],
  expenseTypeOptions: [],
  accountOptions: [],
  voucherTypeOptions: [],
  pushStatusOptions: [],
  pendingPushCount: 0,
  pushedVoucherCount: 0,
  pushFailureCount: 0,
  pendingPushAmount: '0.00'
})
const loading = reactive({
  templatePolicies: false,
  subjectMappings: false,
  pushDocuments: false,
  generatedRecords: false,
  saveTemplatePolicy: false,
  saveSubjectMapping: false,
  generatedDetail: false
})
const templatePolicyQuery = reactive({ page: 1, pageSize: 5 })
const subjectMappingQuery = reactive({ page: 1, pageSize: 5 })
const pushDocumentQuery = reactive({ page: 1, pageSize: 10 })
const generatedRecordQuery = reactive({ page: 1, pageSize: 10 })
const templatePolicyPage = reactive<PageResult<VoucherTemplatePolicy>>({ total: 0, page: 1, pageSize: 5, items: [] })
const subjectMappingPage = reactive<PageResult<VoucherSubjectMapping>>({ total: 0, page: 1, pageSize: 5, items: [] })
const pushDocumentPage = reactive<PageResult<VoucherPushDocument>>({ total: 0, page: 1, pageSize: 10, items: [] })
const generatedRecordPage = reactive<PageResult<VoucherGeneratedRecord>>({ total: 0, page: 1, pageSize: 10, items: [] })
const selectedPushDocumentCodes = ref<string[]>([])

const templatePolicyDialog = reactive({
  visible: false,
  editingId: 0,
  form: { companyId: '', templateCode: '', creditAccountCode: '', voucherType: '', summaryRule: '', enabled: 1 }
})
const subjectMappingDialog = reactive({
  visible: false,
  editingId: 0,
  form: { companyId: '', templateCode: '', expenseTypeCode: '', debitAccountCode: '', enabled: 1 }
})
const generatedDetailDrawer = reactive<{ visible: boolean; detail: VoucherGeneratedDetail | null }>({ visible: false, detail: null })

onMounted(async () => {
  await loadMeta()
  activeTab.value = availableTabs.value[0] || 'mapping'
  await loadCurrentTab()
})

function moneyText(value?: string) {
  return formatMoney(value || '0.00')
}

function syncPageState<T>(target: PageResult<T>, source: PageResult<T>) {
  target.total = source.total
  target.page = source.page
  target.pageSize = source.pageSize
  target.items = source.items
}

async function loadMeta() {
  const response = await expenseVoucherGenerationApi.getMeta()
  Object.assign(meta, response.data)
}

async function loadTemplatePolicies() {
  if (!canMappingView.value) return
  loading.templatePolicies = true
  try {
    const response = await expenseVoucherGenerationApi.getTemplatePolicies({ page: templatePolicyQuery.page, pageSize: templatePolicyQuery.pageSize })
    syncPageState(templatePolicyPage, response.data)
  } finally {
    loading.templatePolicies = false
  }
}

async function loadSubjectMappings() {
  if (!canMappingView.value) return
  loading.subjectMappings = true
  try {
    const response = await expenseVoucherGenerationApi.getSubjectMappings({ page: subjectMappingQuery.page, pageSize: subjectMappingQuery.pageSize })
    syncPageState(subjectMappingPage, response.data)
  } finally {
    loading.subjectMappings = false
  }
}

async function loadPushDocuments() {
  if (!canPushView.value) return
  loading.pushDocuments = true
  try {
    const response = await expenseVoucherGenerationApi.getPushDocuments({ page: pushDocumentQuery.page, pageSize: pushDocumentQuery.pageSize })
    syncPageState(pushDocumentPage, response.data)
  } finally {
    loading.pushDocuments = false
  }
}

async function loadGeneratedRecords() {
  if (!canQueryView.value) return
  loading.generatedRecords = true
  try {
    const response = await expenseVoucherGenerationApi.getGeneratedVouchers({ page: generatedRecordQuery.page, pageSize: generatedRecordQuery.pageSize })
    syncPageState(generatedRecordPage, response.data)
  } finally {
    loading.generatedRecords = false
  }
}

async function loadCurrentTab() {
  if (activeTab.value === 'mapping') {
    await Promise.all([loadTemplatePolicies(), loadSubjectMappings()])
    return
  }
  if (activeTab.value === 'push') {
    await loadPushDocuments()
    return
  }
  await loadGeneratedRecords()
}

function handleTabChange() {
  loadCurrentTab()
}

function handlePushSelectionChange(rows: VoucherPushDocument[]) {
  selectedPushDocumentCodes.value = rows.map((item) => item.documentCode)
}

function fillTemplateName(templateCode: string) {
  return meta.templateOptions.find((item) => item.value === templateCode)?.label?.split(' - ').pop() || meta.templateOptions.find((item) => item.value === templateCode)?.label || ''
}

function fillExpenseTypeName(expenseTypeCode: string) {
  return meta.expenseTypeOptions.find((item) => item.value === expenseTypeCode)?.label || ''
}

function fillAccountName(accountCode: string) {
  return meta.accountOptions.find((item) => item.value === accountCode)?.label || accountCode
}

function openTemplatePolicyDialog(row?: VoucherTemplatePolicy) {
  templatePolicyDialog.visible = true
  templatePolicyDialog.editingId = row?.id || 0
  templatePolicyDialog.form = {
    companyId: row?.companyId || meta.defaultCompanyId || '',
    templateCode: row?.templateCode || '',
    creditAccountCode: row?.creditAccountCode || '',
    voucherType: row?.voucherType || meta.voucherTypeOptions[0]?.value || '记',
    summaryRule: row?.summaryRule || '报销单${documentCode}-${expenseTypeName}',
    enabled: row?.enabled ? 1 : 0
  }
}

function openSubjectMappingDialog(row?: VoucherSubjectMapping) {
  subjectMappingDialog.visible = true
  subjectMappingDialog.editingId = row?.id || 0
  subjectMappingDialog.form = {
    companyId: row?.companyId || meta.defaultCompanyId || '',
    templateCode: row?.templateCode || '',
    expenseTypeCode: row?.expenseTypeCode || '',
    debitAccountCode: row?.debitAccountCode || '',
    enabled: row?.enabled ? 1 : 0
  }
}

async function submitTemplatePolicy() {
  const payload: VoucherTemplatePolicyPayload = {
    ...templatePolicyDialog.form,
    templateName: fillTemplateName(templatePolicyDialog.form.templateCode),
    creditAccountName: fillAccountName(templatePolicyDialog.form.creditAccountCode)
  }
  loading.saveTemplatePolicy = true
  try {
    if (templatePolicyDialog.editingId) {
      await expenseVoucherGenerationApi.updateTemplatePolicy(templatePolicyDialog.editingId, payload)
      ElMessage.success('模板策略更新成功')
    } else {
      await expenseVoucherGenerationApi.createTemplatePolicy(payload)
      ElMessage.success('模板策略保存成功')
    }
    templatePolicyDialog.visible = false
    await Promise.all([loadMeta(), loadTemplatePolicies()])
  } finally {
    loading.saveTemplatePolicy = false
  }
}

async function submitSubjectMapping() {
  const payload: VoucherSubjectMappingPayload = {
    ...subjectMappingDialog.form,
    templateName: fillTemplateName(subjectMappingDialog.form.templateCode),
    expenseTypeName: fillExpenseTypeName(subjectMappingDialog.form.expenseTypeCode),
    debitAccountName: fillAccountName(subjectMappingDialog.form.debitAccountCode)
  }
  loading.saveSubjectMapping = true
  try {
    if (subjectMappingDialog.editingId) {
      await expenseVoucherGenerationApi.updateSubjectMapping(subjectMappingDialog.editingId, payload)
      ElMessage.success('费用映射更新成功')
    } else {
      await expenseVoucherGenerationApi.createSubjectMapping(payload)
      ElMessage.success('费用映射保存成功')
    }
    subjectMappingDialog.visible = false
    await loadSubjectMappings()
  } finally {
    loading.saveSubjectMapping = false
  }
}

async function pushSelected() {
  if (!selectedPushDocumentCodes.value.length) {
    ElMessage.warning('请先选择需要推送的单据')
    return
  }
  await confirmAndPush(selectedPushDocumentCodes.value)
}

async function pushSingle(documentCode: string) {
  await confirmAndPush([documentCode])
}

async function confirmAndPush(documentCodes: string[]) {
  try {
    await ElMessageBox.confirm(`确认推送 ${documentCodes.length} 张单据生成凭证吗？`, '推送确认', { type: 'warning' })
    const response = await expenseVoucherGenerationApi.pushDocuments(documentCodes)
    const failed = response.data.failureCount || 0
    ElMessage.success(failed > 0 ? `推送完成，成功 ${response.data.successCount} 条，失败 ${failed} 条` : `推送完成，共成功 ${response.data.successCount} 条`)
    selectedPushDocumentCodes.value = []
    await Promise.all([loadMeta(), loadPushDocuments(), loadGeneratedRecords()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

async function openGeneratedDetail(row: VoucherGeneratedRecord) {
  loading.generatedDetail = true
  try {
    const response = await expenseVoucherGenerationApi.getGeneratedVoucherDetail(row.id)
    generatedDetailDrawer.detail = response.data
    generatedDetailDrawer.visible = true
  } finally {
    loading.generatedDetail = false
  }
}
</script>

<style scoped>
.voucher-generation-page :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.voucher-generation-page :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
