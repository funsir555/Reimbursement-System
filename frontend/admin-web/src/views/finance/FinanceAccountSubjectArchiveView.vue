<template>
  <div class="space-y-4">
    <section class="rounded-[26px] border border-slate-100 bg-white px-6 py-4 shadow-sm">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-2xl font-bold text-slate-800">会计科目</h1>
          <div class="inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1.5 text-sm text-sky-700">
            <span class="font-semibold">当前公司</span>
            <strong>{{ currentCompanyDisplay || '未选择' }}</strong>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <el-button :icon="RefreshRight" @click="loadSubjects">刷新</el-button>
          <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openCreateDrawer()">新增科目</el-button>
        </div>
      </div>
    </section>

    <section
      v-if="contextMessage"
      class="rounded-[24px] border px-5 py-4 text-sm font-medium shadow-sm"
      :class="contextMessageClass"
    >
      {{ contextMessage }}
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr),180px,160px,160px,160px]">
        <el-input v-model="filters.keyword" clearable placeholder="科目编码 / 名称 / 助记码" @keyup.enter="loadSubjects">
          <template #append><el-button :icon="Search" @click="loadSubjects" /></template>
        </el-input>
        <el-select v-model="filters.subjectCategory" clearable placeholder="科目类别" @change="loadSubjects">
          <el-option v-for="item in meta.subjectCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="启用状态" @change="loadSubjects">
          <el-option v-for="item in meta.statusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
        </el-select>
        <el-select v-model="filters.bclose" clearable placeholder="封存状态" @change="loadSubjects">
          <el-option v-for="item in meta.closeStatusOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
        </el-select>
        <div class="flex justify-end"><el-button :icon="RefreshRight" @click="resetFilters">重置</el-button></div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table v-loading="loading" :data="subjectTree" row-key="subject_code" default-expand-all :tree-props="{ children: 'children' }" style="width: 100%">
        <el-table-column prop="subject_code" label="科目编码" min-width="160" />
        <el-table-column prop="subject_name" label="科目名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="chelp" label="助记码" min-width="120" show-overflow-tooltip />
        <el-table-column label="科目类别" min-width="120"><template #default="{ row }">{{ categoryLabel(row.subject_category) }}</template></el-table-column>
        <el-table-column label="辅助核算" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ row.auxiliary_summary || '未设置' }}</template></el-table-column>
        <el-table-column label="现金 / 银行" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ row.cash_bank_summary || '未设置' }}</template></el-table-column>
        <el-table-column label="启用状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="封存状态" width="100"><template #default="{ row }"><el-tag :type="row.bclose === 1 ? 'warning' : 'success'" effect="plain">{{ row.bclose === 1 ? '已封存' : '未封存' }}</el-tag></template></el-table-column>
        <el-table-column prop="updated_at" label="更新时间" min-width="180" />
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetailDrawer(row.subject_code)">详情</el-button>
            <el-button v-if="canCreate" link type="primary" @click="openCreateDrawer(row.subject_code)">新增下级</el-button>
            <el-button v-if="canEdit" link type="primary" @click="openEditDrawer(row.subject_code)">编辑</el-button>
            <el-button v-if="canDisable" link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button v-if="canClose" link :type="row.bclose === 1 ? 'success' : 'warning'" @click="toggleClose(row)">{{ row.bclose === 1 ? '解封' : '封存' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="980px" destroy-on-close>
      <div class="space-y-5">
        <div class="grid grid-cols-1 gap-4 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-4 xl:grid-cols-4">
          <div><div class="text-xs text-slate-500">所属公司</div><div class="mt-1 font-semibold text-slate-800">{{ currentCompanyDisplay || currentCompanyId || '未选择' }}</div></div>
          <div><div class="text-xs text-slate-500">启用状态</div><div class="mt-1 font-semibold text-slate-800">{{ subjectStatusText(form.status) }}</div></div>
          <div><div class="text-xs text-slate-500">封存状态</div><div class="mt-1 font-semibold text-slate-800">{{ closeStatusText(form.bclose) }}</div></div>
          <div><div class="text-xs text-slate-500">余额方向</div><div class="mt-1 font-semibold text-slate-800">{{ balanceDirectionText(balanceDirectionPreview) }}</div></div>
        </div>

        <el-form label-position="top" class="space-y-5">
          <el-collapse v-model="activeSections">
            <el-collapse-item name="basic">
              <template #title><span class="text-base font-semibold text-slate-800">编码与层级</span></template>
              <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
                <el-form-item label="父级科目" class="!mb-0"><el-input :model-value="parentDisplayText" disabled placeholder="请输入科目编码后自动匹配父级科目" /></el-form-item>
                <el-form-item label="科目编码" class="!mb-0"><el-input v-model="form.subject_code" :disabled="!isCreateMode" placeholder="请输入科目编码" @input="syncDerivedFields" /></el-form-item>
                <el-form-item label="科目名称" class="!mb-0"><el-input v-model="form.subject_name" :disabled="isDetailMode" placeholder="请输入科目名称" /></el-form-item>
                <el-form-item label="层级" class="!mb-0"><el-input :model-value="subjectLevelPreviewText" disabled /></el-form-item>
                <el-form-item label="科目类别" class="!mb-0"><el-input :model-value="categoryPreviewLabel" disabled placeholder="根据编码自动匹配" /></el-form-item>
                <el-form-item label="助记码" class="!mb-0"><el-input v-model="form.chelp" :disabled="isDetailMode" placeholder="请输入助记码" /></el-form-item>
                <el-form-item label="余额方向" class="!mb-0"><el-input :model-value="balanceDirectionText(balanceDirectionPreview)" disabled /></el-form-item>
                <div v-if="autoParentWarning" class="xl:col-span-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
                  {{ autoParentWarning }}
                </div>
              </div>
            </el-collapse-item>

            <el-collapse-item name="attribute">
              <template #title><span class="text-base font-semibold text-slate-800">科目属性</span></template>
              <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
                <el-form-item label="币种" class="!mb-0"><el-input v-model="form.cexch_name" :disabled="isDetailMode" placeholder="如 CNY" /></el-form-item>
                <el-form-item label="计量单位" class="!mb-0"><el-input v-model="form.cmeasure" :disabled="isDetailMode" placeholder="请输入计量单位" /></el-form-item>
              </div>
            </el-collapse-item>

            <el-collapse-item name="auxiliary">
              <template #title><span class="text-base font-semibold text-slate-800">辅助核算</span></template>
              <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
                <el-form-item label="人员" class="!mb-0"><flag-switch v-model="form.bperson" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="客户" class="!mb-0"><flag-switch v-model="form.bcus" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="供应商" class="!mb-0"><flag-switch v-model="form.bsup" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="部门" class="!mb-0"><flag-switch v-model="form.bdept" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="项目" class="!mb-0"><flag-switch v-model="form.bitem" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="项目分类编码" class="!mb-0"><el-input v-model="form.cass_item" :disabled="isProjectClassBindingDisabled" placeholder="请输入项目分类编码" /></el-form-item>
              </div>
            </el-collapse-item>

            <el-collapse-item name="cashBank">
              <template #title><span class="text-base font-semibold text-slate-800">现金银行与对账</span></template>
              <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
                <el-form-item label="日记账" class="!mb-0"><flag-switch v-model="form.br" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="银行账" class="!mb-0"><flag-switch v-model="form.be" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="汇兑损益" class="!mb-0"><flag-switch v-model="form.bexchange" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="现金科目" class="!mb-0"><flag-switch v-model="form.bcash" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="银行科目" class="!mb-0"><flag-switch v-model="form.bbank" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="银行业务启用" class="!mb-0"><flag-switch v-model="form.bused" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="对账方向" class="!mb-0"><flag-switch v-model="form.bd_c" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="起始时间" class="!mb-0"><el-date-picker v-model="form.dbegin" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="w-full" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="结束时间" class="!mb-0"><el-date-picker v-model="form.dend" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="w-full" :disabled="isDetailMode" /></el-form-item>
              </div>
            </el-collapse-item>

            <el-collapse-item name="control">
              <template #title><span class="text-base font-semibold text-slate-800">控制与状态</span></template>
              <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
                <el-form-item label="汇总打印" class="!mb-0"><el-switch v-model="form.cgather" :disabled="isDetailMode" active-value="1" inactive-value="0" inline-prompt active-text="是" inactive-text="否" /></el-form-item>
                <el-form-item label="特殊标记" class="!mb-0"><el-input-number v-model="form.itrans" :disabled="isDetailMode" :min="0" :max="9" class="w-full" /></el-form-item>
                <el-form-item label="受控系统" class="!mb-0"><el-input v-model="form.cother" :disabled="isDetailMode" placeholder="请输入受控系统标记" /></el-form-item>
                <el-form-item label="其他系统已使用" class="!mb-0"><el-input-number v-model="form.iotherused" :disabled="isDetailMode" :min="0" class="w-full" /></el-form-item>
                <el-form-item label="转账通知" class="!mb-0"><flag-switch v-model="form.bReport" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="工程结算" class="!mb-0"><flag-switch v-model="form.bGCJS" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="现金流量科目" class="!mb-0"><flag-switch v-model="form.bCashItem" :disabled="isDetailMode" /></el-form-item>
                <el-form-item label="视图项目类型" class="!mb-0"><el-input-number v-model="form.iViewItem" :disabled="isDetailMode" :min="0" class="w-full" /></el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-form>
      </div>
      <template #footer><div class="flex justify-end gap-3"><el-button @click="closeDrawer">关闭</el-button><el-button v-if="!isDetailMode" type="primary" :loading="saving" @click="saveSubject">保存</el-button></div></template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { financeArchiveApi, type FinanceAccountSubjectDetail, type FinanceAccountSubjectMeta, type FinanceAccountSubjectSavePayload, type FinanceAccountSubjectSummary } from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const FlagSwitch = defineComponent({
  name: 'FlagSwitch',
  props: { modelValue: { type: Number, default: 0 }, disabled: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('el-switch', { modelValue: props.modelValue, disabled: props.disabled, 'active-value': 1, 'inactive-value': 0, 'inline-prompt': true, 'active-text': 'Y', 'inactive-text': 'N', 'onUpdate:modelValue': (value: number) => emit('update:modelValue', value) })
  }
})

type DrawerMode = 'create' | 'edit' | 'detail'
type ParentOption = { value: string; label: string; subject_category?: string; subject_level: number; balance_direction?: string }
type AccountSubjectFormState = FinanceAccountSubjectDetail & { has_children?: boolean }

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const financeCompany = useFinanceCompanyStore()
const financeCompanyState = financeCompany as typeof financeCompany & {
  currentCompanyLabel?: string
  currentCompanyHasActiveAccountSet?: boolean
}
const loading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const drawerMode = ref<DrawerMode>('detail')
const editingSubjectCode = ref('')
const createParentHintCode = ref('')
const meta = reactive<FinanceAccountSubjectMeta>({ subjectCategoryOptions: [], statusOptions: [], closeStatusOptions: [], yesNoOptions: [] })
const filters = reactive({ keyword: '', subjectCategory: '', status: undefined as number | undefined, bclose: undefined as number | undefined })
const subjectTree = ref<FinanceAccountSubjectSummary[]>([])
const activeSections = ref(['basic', 'attribute', 'auxiliary', 'cashBank', 'control'])
const form = reactive<AccountSubjectFormState>(createDefaultForm())
const COMPANY_SWITCH_GUARD_KEY = 'finance-account-subject-archive'
let guardRegistered = false

const canCreate = computed(() => hasPermission('finance:archives:account_subjects:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:archives:account_subjects:edit', permissionCodes.value))
const canDisable = computed(() => hasPermission('finance:archives:account_subjects:disable', permissionCodes.value))
const canClose = computed(() => hasPermission('finance:archives:account_subjects:close', permissionCodes.value))
const currentCompanyId = computed(() => financeCompany.currentCompanyId)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const currentCompanyOption = computed(() => financeCompany.companyOptions?.find((item) => item.companyId === currentCompanyId.value))
const currentCompanyDisplay = computed(() => financeCompanyState.currentCompanyLabel || currentCompanyName.value || currentCompanyId.value || '')
const hasActiveAccountSet = computed(() => {
  if (typeof financeCompanyState.currentCompanyHasActiveAccountSet === 'boolean') return financeCompanyState.currentCompanyHasActiveAccountSet
  if (typeof currentCompanyOption.value?.hasActiveAccountSet === 'boolean') return currentCompanyOption.value.hasActiveAccountSet
  return Boolean(currentCompanyId.value)
})
const isCreateMode = computed(() => drawerMode.value === 'create')
const isDetailMode = computed(() => drawerMode.value === 'detail')
const drawerTitle = computed(() => (drawerMode.value === 'create' ? '新增会计科目' : drawerMode.value === 'edit' ? '编辑会计科目' : '会计科目详情'))
const allSubjects = computed(() => flattenTree(subjectTree.value))
const parentOptions = computed<ParentOption[]>(() => allSubjects.value.filter((item) => item.subject_code !== editingSubjectCode.value).map((item) => ({ value: item.subject_code, label: `${item.subject_code} / ${item.subject_name}`, subject_category: item.subject_category, subject_level: item.subject_level, balance_direction: item.balance_direction })))
const selectedParent = computed(() => parentOptions.value.find((item) => item.value === form.parent_subject_code))
const hintedParent = computed(() => parentOptions.value.find((item) => item.value === createParentHintCode.value))
const autoMatchedParent = computed(() => {
  if (!isCreateMode.value) return undefined
  const code = trimString(form.subject_code)
  if (!code) return hintedParent.value
  return findMatchedParent(code)
})
const effectiveParent = computed(() => isCreateMode.value ? autoMatchedParent.value : selectedParent.value)
const subjectLevelPreview = computed(() => effectiveParent.value ? effectiveParent.value.subject_level + 1 : undefined)
const subjectLevelPreviewText = computed(() => subjectLevelPreview.value ? String(subjectLevelPreview.value) : '')
const categoryPreviewValue = computed(() => {
  if (effectiveParent.value?.subject_category) {
    return effectiveParent.value.subject_category
  }
  if (isCreateMode.value) {
    return undefined
  }
  return trimString(form.subject_category) || undefined
})
const categoryPreviewLabel = computed(() => categoryPreviewValue.value ? categoryLabel(categoryPreviewValue.value) : '')
const parentDisplayText = computed(() => effectiveParent.value?.label || '')
const missingAutoParent = computed(() => isCreateMode.value && Boolean(trimString(form.subject_code)) && !autoMatchedParent.value)
const autoParentWarning = computed(() => missingAutoParent.value ? '未匹配到已存在的上级科目，请先创建上级科目，再新增当前科目。' : '')
const balanceDirectionPreview = computed(() => {
  if (effectiveParent.value?.balance_direction) return effectiveParent.value.balance_direction
  const code = String(form.subject_code || '').trim()
  if (code.startsWith('1') || code.startsWith('4') || code.startsWith('6')) return 'DEBIT'
  if (code.startsWith('2') || code.startsWith('3') || code.startsWith('5')) return 'CREDIT'
  if (categoryPreviewValue.value === 'LIABILITY' || categoryPreviewValue.value === 'EQUITY' || categoryPreviewValue.value === 'PROFIT') return 'CREDIT'
  return 'DEBIT'
})
const contextMessage = computed(() => {
  if (!currentCompanyId.value) return '当前未选择财务公司，请先选择公司后再查看会计科目。'
  if (!hasActiveAccountSet.value) return '当前公司未创建账套，请切换公司或先建账。'
  if (!loading.value && subjectTree.value.length === 0) return '当前公司账套已启用，但暂无会计科目数据，请检查账套初始化结果。'
  return ''
})
const contextMessageClass = computed(() => !currentCompanyId.value || !hasActiveAccountSet.value ? 'border-amber-200 bg-amber-50 text-amber-700' : 'border-sky-200 bg-sky-50 text-sky-700')
const projectAssistEnabled = computed(() => normalizeNumber(form.bitem, 0) === 1)
const isProjectClassBindingDisabled = computed(() => isDetailMode.value || !projectAssistEnabled.value)

onMounted(registerCompanySwitchGuard)
onActivated(registerCompanySwitchGuard)
onDeactivated(unregisterCompanySwitchGuard)
watch(() => financeCompany.currentCompanyId, async (companyId, previousCompanyId) => { if (!companyId) { subjectTree.value = []; return }; if (companyId !== previousCompanyId) closeDrawer(); await loadMeta(); await loadSubjects() }, { immediate: true })
watch(() => form.bitem, (value) => { if (normalizeNumber(value, 0) !== 1) form.cass_item = '' })
watch([() => form.subject_code, effectiveParent, () => isCreateMode.value], () => {
  syncDerivedFields()
})
onBeforeUnmount(() => unregisterCompanySwitchGuard())

function registerCompanySwitchGuard() { if (guardRegistered) return; financeCompany.registerSwitchGuard(COMPANY_SWITCH_GUARD_KEY, confirmCompanySwitch); guardRegistered = true }
function unregisterCompanySwitchGuard() { if (!guardRegistered) return; financeCompany.unregisterSwitchGuard(COMPANY_SWITCH_GUARD_KEY); guardRegistered = false }

async function loadMeta() { try { const res = await financeArchiveApi.getAccountSubjectMeta(); meta.subjectCategoryOptions = res.data.subjectCategoryOptions || []; meta.statusOptions = res.data.statusOptions || []; meta.closeStatusOptions = res.data.closeStatusOptions || []; meta.yesNoOptions = res.data.yesNoOptions || [] } catch (error: unknown) { ElMessage.error(resolveErrorMessage(error, '加载会计科目元数据失败')) } }
async function loadSubjects() { if (!currentCompanyId.value || !hasActiveAccountSet.value) { subjectTree.value = []; return }; loading.value = true; try { const res = await financeArchiveApi.listAccountSubjects({ companyId: currentCompanyId.value, keyword: filters.keyword.trim() || undefined, subjectCategory: filters.subjectCategory || undefined, status: filters.status, bclose: filters.bclose }); subjectTree.value = normalizeTree(res.data || []) } catch (error: unknown) { ElMessage.error(resolveErrorMessage(error, '加载会计科目列表失败')) } finally { loading.value = false } }
function resetFilters() { filters.keyword = ''; filters.subjectCategory = ''; filters.status = undefined; filters.bclose = undefined; void loadSubjects() }
function ensureCurrentCompanyReady(actionText: string) { if (!currentCompanyId.value) { ElMessage.warning(`当前未选择财务公司，无法${actionText}`); return false }; if (!hasActiveAccountSet.value) { ElMessage.warning('当前公司未创建账套，请切换公司或先建账。'); return false }; return true }
function resetForm(parentSubjectCode = '') { Object.assign(form, createDefaultForm()); createParentHintCode.value = parentSubjectCode || ''; form.parent_subject_code = parentSubjectCode || ''; syncDerivedFields() }
function syncDerivedFields() {
  if (isCreateMode.value) {
    form.parent_subject_code = effectiveParent.value?.value || ''
    form.subject_level = subjectLevelPreview.value || 0
    form.subject_category = categoryPreviewValue.value || ''
  }
  form.balance_direction = balanceDirectionPreview.value
  form.cclassany = form.subject_category || ''
  form.bproperty = balanceDirectionPreview.value === 'DEBIT' ? 1 : 0
  form.cbook_type = defaultBookType()
}
function openCreateDrawer(parentSubjectCode = '') { if (!ensureCurrentCompanyReady('维护会计科目')) return; drawerMode.value = 'create'; editingSubjectCode.value = ''; resetForm(parentSubjectCode); activeSections.value = ['basic', 'attribute', 'auxiliary', 'cashBank', 'control']; drawerVisible.value = true }
async function openDetailDrawer(subjectCode: string) { await loadSubjectDetail(subjectCode, 'detail') }
async function openEditDrawer(subjectCode: string) { await loadSubjectDetail(subjectCode, 'edit') }
async function loadSubjectDetail(subjectCode: string, mode: DrawerMode) { if (!ensureCurrentCompanyReady('查看会计科目')) return; drawerMode.value = mode; createParentHintCode.value = ''; editingSubjectCode.value = subjectCode; drawerVisible.value = true; activeSections.value = ['basic', 'attribute', 'auxiliary', 'cashBank', 'control']; try { const res = await financeArchiveApi.getAccountSubjectDetail(currentCompanyId.value, subjectCode); Object.assign(form, createDefaultForm(), res.data); form.parent_subject_code = form.parent_subject_code || ''; form.has_children = Boolean(res.data.has_children); syncDerivedFields() } catch (error: unknown) { drawerVisible.value = false; ElMessage.error(resolveErrorMessage(error, '加载会计科目详情失败')) } }
async function saveSubject() { if (!ensureCurrentCompanyReady('保存会计科目')) return; if (!String(form.subject_code || '').trim()) { ElMessage.warning('科目编码不能为空'); return }; if (!String(form.subject_name || '').trim()) { ElMessage.warning('科目名称不能为空'); return }; if (isCreateMode.value && missingAutoParent.value) { ElMessage.warning('请先创建上级科目，再新增当前科目'); return }; saving.value = true; try { const payload = buildPayload(); if (drawerMode.value === 'create') { await financeArchiveApi.createAccountSubject(currentCompanyId.value, payload); ElMessage.success('会计科目创建成功') } else { await financeArchiveApi.updateAccountSubject(currentCompanyId.value, editingSubjectCode.value, payload); ElMessage.success('会计科目更新成功') }; closeDrawer(); await loadSubjects() } catch (error: unknown) { ElMessage.error(resolveErrorMessage(error, '保存会计科目失败')) } finally { saving.value = false } }
async function toggleStatus(row: FinanceAccountSubjectSummary) { if (!ensureCurrentCompanyReady('更新会计科目状态')) return; const nextStatus = row.status === 1 ? 0 : 1; const actionText = nextStatus === 1 ? '启用' : '停用'; try { await ElMessageBox.confirm(`确认${actionText}会计科目 ${row.subject_code} - ${row.subject_name} 吗？`, `${actionText}会计科目`, { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }) } catch { return } try { await financeArchiveApi.updateAccountSubjectStatus(currentCompanyId.value, row.subject_code, nextStatus); ElMessage.success(`会计科目${actionText}成功`); await loadSubjects() } catch (error: unknown) { ElMessage.error(resolveErrorMessage(error, `${actionText}会计科目失败`)) } }
async function toggleClose(row: FinanceAccountSubjectSummary) { if (!ensureCurrentCompanyReady('更新会计科目封存状态')) return; const nextClose = row.bclose === 1 ? 0 : 1; const actionText = nextClose === 1 ? '封存' : '解封'; try { await ElMessageBox.confirm(`确认${actionText}会计科目 ${row.subject_code} - ${row.subject_name} 吗？`, `${actionText}会计科目`, { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }) } catch { return } try { await financeArchiveApi.updateAccountSubjectClose(currentCompanyId.value, row.subject_code, nextClose); ElMessage.success(`会计科目${actionText}成功`); await loadSubjects() } catch (error: unknown) { ElMessage.error(resolveErrorMessage(error, `${actionText}会计科目失败`)) } }
function buildPayload(): FinanceAccountSubjectSavePayload { return { subject_code: String(form.subject_code || '').trim(), subject_name: String(form.subject_name || '').trim(), parent_subject_code: trimString(form.parent_subject_code), subject_level: form.subject_level || undefined, subject_category: trimString(form.subject_category), chelp: trimString(form.chelp), cexch_name: trimString(form.cexch_name || 'CNY'), cmeasure: trimString(form.cmeasure), bperson: normalizeNumber(form.bperson, 0), bcus: normalizeNumber(form.bcus, 0), bsup: normalizeNumber(form.bsup, 0), bdept: normalizeNumber(form.bdept, 0), bitem: normalizeNumber(form.bitem, 0), cass_item: normalizeNumber(form.bitem, 0) === 1 ? trimString(form.cass_item) : undefined, br: normalizeNumber(form.br, 0), be: normalizeNumber(form.be, 0), cgather: String(form.cgather || '0'), bexchange: normalizeNumber(form.bexchange, 0), bcash: normalizeNumber(form.bcash, 0), bbank: normalizeNumber(form.bbank, 0), bused: normalizeNumber(form.bused, 0), bd_c: normalizeNumber(form.bd_c, 0), dbegin: trimString(form.dbegin), dend: trimString(form.dend), itrans: normalizeNumber(form.itrans, 0), cother: trimString(form.cother), iotherused: normalizeNumber(form.iotherused, 0), bReport: normalizeNumber(form.bReport, 0), bGCJS: normalizeNumber(form.bGCJS, 0), bCashItem: normalizeNumber(form.bCashItem, 0), iViewItem: normalizeNumber(form.iViewItem, 0) } }
function closeDrawer() { drawerVisible.value = false; editingSubjectCode.value = ''; createParentHintCode.value = ''; Object.assign(form, createDefaultForm()) }
function normalizeTree(nodes: FinanceAccountSubjectSummary[] = []): FinanceAccountSubjectSummary[] { return nodes.map((item) => ({ ...item, children: normalizeTree(item.children || []) })) }
function flattenTree(nodes: FinanceAccountSubjectSummary[]): FinanceAccountSubjectSummary[] { const result: FinanceAccountSubjectSummary[] = []; nodes.forEach((node) => { result.push(node); if (node.children?.length) result.push(...flattenTree(node.children)) }); return result }
function findMatchedParent(subjectCode: string) { const normalizedCode = trimString(subjectCode); if (!normalizedCode) return undefined; return parentOptions.value.reduce<ParentOption | undefined>((matched, candidate) => { if (candidate.value === normalizedCode || !normalizedCode.startsWith(candidate.value)) return matched; if (!matched || candidate.value.length > matched.value.length) return candidate; return matched }, undefined) }
function categoryLabel(value?: string) { return meta.subjectCategoryOptions.find((item) => item.value === value)?.label || value || '-' }
function balanceDirectionText(value?: string) { if (value === 'DEBIT') return '借方'; if (value === 'CREDIT') return '贷方'; return '-' }
function subjectStatusText(value?: number) { return value === 0 ? '停用' : '启用' }
function closeStatusText(value?: number) { return value === 1 ? '已封存' : '未封存' }
function defaultBookType() { const code = String(form.subject_code || '').trim(); if (code.startsWith('1001')) return 'CASH'; if (code.startsWith('1002')) return 'BANK'; return 'GENERAL' }
function normalizeNumber(value: number | undefined, fallback: number) { return typeof value === 'number' && Number.isFinite(value) ? value : fallback }
function trimString(value?: string) { const normalized = String(value || '').trim(); return normalized || undefined }
function resolveErrorMessage(error: unknown, fallback: string) { return error instanceof Error && error.message ? error.message : fallback }
async function confirmCompanySwitch() { if (!drawerVisible.value) return true; try { await ElMessageBox.confirm('切换公司会关闭当前会计科目抽屉，是否继续？', '切换公司', { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }); return true } catch { return false } }
function createDefaultForm(): AccountSubjectFormState { return { subject_code: '', subject_name: '', parent_subject_code: '', subject_level: 1, balance_direction: 'DEBIT', subject_category: 'ASSET', cclassany: 'ASSET', bproperty: 1, cbook_type: 'GENERAL', chelp: '', cexch_name: 'CNY', cmeasure: '', bperson: 0, bcus: 0, bsup: 0, bdept: 0, bitem: 0, cass_item: '', br: 0, be: 0, cgather: '0', leaf_flag: 1, bexchange: 0, bcash: 0, bbank: 0, bused: 0, bd_c: 0, dbegin: '', dend: '', itrans: 0, bclose: 0, cother: '', iotherused: 0, bReport: 0, bGCJS: 0, bCashItem: 0, iViewItem: 0, status: 1, has_children: false } }
defineExpose({ filters, meta, form, subjectTree, loadSubjects, openCreateDrawer, buildPayload, toggleStatus, toggleClose, saveSubject, syncDerivedFields, balanceDirectionPreview, subjectLevelPreview, parentDisplayText, categoryPreviewValue, missingAutoParent })
</script>

<style scoped>
:deep(.el-collapse-item__header) { font-size: 15px; font-weight: 600; color: #0f172a; }
:deep(.el-collapse-item__content) { padding-bottom: 4px; }
</style>
