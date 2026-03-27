<template>
  <div class="grid grid-cols-1 gap-6 xl:grid-cols-[320px,minmax(0,1fr)]">
    <el-card class="!rounded-3xl !shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-slate-800">自定义档案</h2>
            <p class="mt-1 text-sm text-slate-400">维护标签设置、分期付款等可复用档案，也支持按规则自动划分结果。</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">添加档案</el-button>
        </div>
      </template>

      <div class="space-y-4">
        <el-input v-model="keyword" placeholder="搜索档案名称或编码" :prefix-icon="Search" clearable />
        <el-segmented v-model="statusFilter" :options="statusOptions" block />

        <div v-loading="loadingList" class="space-y-3">
          <button
            v-for="archive in filteredArchives"
            :key="archive.id"
            type="button"
            class="archive-item w-full rounded-2xl border p-4 text-left transition-all"
            :class="archive.id === activeArchiveId ? 'archive-item-active' : ''"
            @click="selectArchive(archive.id)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="truncate text-sm font-semibold text-slate-800">{{ archive.archiveName }}</span>
                  <el-tag size="small" effect="plain">{{ archive.archiveTypeLabel }}</el-tag>
                </div>
                <p class="mt-2 truncate font-mono text-xs text-slate-400">{{ archive.archiveCode }}</p>
              </div>
              <el-tag :type="archive.status === 1 ? 'success' : 'info'" size="small">
                {{ archive.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </div>

            <p class="mt-3 line-clamp-2 min-h-[40px] text-sm leading-6 text-slate-500">
              {{ archive.archiveDescription || '暂无说明' }}
            </p>

            <div class="mt-4 flex items-center justify-between text-xs text-slate-400">
              <span>{{ archive.itemCount }} 个结果项</span>
              <span>{{ archive.updatedAt || '暂无更新时间' }}</span>
            </div>
          </button>

          <el-empty v-if="!loadingList && filteredArchives.length === 0" description="暂无档案">
            <el-button type="primary" @click="openCreateDialog">添加档案</el-button>
          </el-empty>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <template #header>
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div v-if="form">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-xl font-semibold text-slate-800">{{ form.archiveName || '新建档案' }}</h2>
              <el-tag effect="plain">{{ archiveTypeLabel }}</el-tag>
              <el-tag v-if="form.archiveCode === meta?.tagArchiveCode" type="warning" effect="plain">默认标签档案</el-tag>
              <el-tag v-if="form.archiveCode === meta?.installmentArchiveCode" type="success" effect="plain">
                默认分期付款档案
              </el-tag>
            </div>
            <p class="mt-2 font-mono text-sm text-slate-400">{{ archiveCodeDisplay }}</p>
          </div>

          <div v-else>
            <h2 class="text-xl font-semibold text-slate-800">档案配置</h2>
            <p class="mt-2 text-sm text-slate-400">先从左侧选择一个档案，或新增一个新的自定义档案开始配置。</p>
          </div>

          <div class="flex flex-wrap gap-3">
            <el-button @click="refreshAll">刷新</el-button>
            <el-button v-if="form?.id" type="danger" plain :icon="Delete" @click="removeArchive">删除</el-button>
            <el-button type="primary" :icon="Check" :loading="saving" :disabled="!form" @click="saveArchive">保存</el-button>
          </div>
        </div>
      </template>

      <div v-if="loadingDetail" class="py-8">
        <el-skeleton animated :rows="10" />
      </div>

      <template v-else-if="form">
        <el-form label-position="top" class="space-y-6">
          <el-card class="section-card !rounded-3xl !shadow-none">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-semibold text-slate-800">基础信息</span>
                <el-switch
                  v-model="statusSwitch"
                  inline-prompt
                  active-text="启用"
                  inactive-text="停用"
                  @change="changeStatus"
                />
              </div>
            </template>

            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <el-form-item label="档案名称" required>
                <el-input v-model="form.archiveName" placeholder="请输入档案名称" />
              </el-form-item>

              <el-form-item label="档案类型">
                <el-select v-model="form.archiveType" disabled>
                  <el-option v-for="type in archiveTypeCards" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
              </el-form-item>

              <el-form-item label="编码生成逻辑" class="xl:col-span-2">
                <div class="code-preview">
                  <p class="font-mono text-sm text-slate-700">{{ archiveCodeDisplay }}</p>
                  <p class="mt-2 text-xs text-slate-500">系统保存时自动生成：`CA + 年月日 + 4位流水`。</p>
                </div>
              </el-form-item>

              <el-form-item label="类型说明" class="xl:col-span-2">
                <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-600">
                  {{ archiveTypeDescription }}
                </div>
              </el-form-item>

              <el-form-item label="档案说明" class="xl:col-span-2">
                <el-input
                  v-model="form.archiveDescription"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入这个档案的用途、展示范围或维护说明"
                />
              </el-form-item>
            </div>
          </el-card>

          <el-card class="section-card !rounded-3xl !shadow-none">
            <template #header>
              <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <span class="font-semibold text-slate-800">结果配置</span>
                  <p class="mt-1 text-sm text-slate-400">
                    {{
                      form.archiveType === 'SELECT'
                        ? '维护一组可选结果，业务页面会直接作为选择项展示。'
                        : '为每个结果项配置规则组。组内多条规则按 AND，同一结果项多个规则组按 OR。'
                    }}
                  </p>
                </div>
                <el-button type="primary" plain :icon="Plus" @click="addItem">添加结果项</el-button>
              </div>
            </template>

            <div class="space-y-5">
              <el-card
                v-for="(item, itemIndex) in form.items"
                :key="`${item.id || 'new'}-${itemIndex}`"
                class="result-card !rounded-3xl !shadow-sm"
              >
                <template #header>
                  <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                    <div class="flex items-center gap-3">
                      <span class="result-index">{{ itemIndex + 1 }}</span>
                      <div>
                        <p class="font-semibold text-slate-800">{{ item.itemName || `结果项 ${itemIndex + 1}` }}</p>
                        <p class="text-xs text-slate-400">结果编码由系统生成，用于模板和解析结果的稳定引用。</p>
                      </div>
                    </div>
                    <div class="flex flex-wrap items-center gap-3">
                      <el-switch
                        v-model="item.status"
                        :active-value="1"
                        :inactive-value="0"
                        inline-prompt
                        active-text="启用"
                        inactive-text="停用"
                      />
                      <el-button type="danger" plain size="small" @click="removeItem(itemIndex)">删除结果项</el-button>
                    </div>
                  </div>
                </template>

                <div class="grid grid-cols-1 gap-6 xl:grid-cols-3">
                  <el-form-item label="结果名称" required>
                    <el-input v-model="item.itemName" placeholder="请输入结果项名称" />
                  </el-form-item>

                  <el-form-item label="结果编码">
                    <div class="code-preview">
                      <p class="font-mono text-sm text-slate-700">{{ itemCodeDisplay(item) }}</p>
                      <p class="mt-2 text-xs text-slate-500">系统保存时自动生成：`CI + 年月日 + 4位流水`。</p>
                    </div>
                  </el-form-item>

                  <el-form-item v-if="form.archiveType === 'AUTO_RULE'" label="优先级">
                    <el-input-number v-model="item.priority" :min="1" :step="1" class="w-full" />
                  </el-form-item>
                </div>

                <div v-if="form.archiveType === 'AUTO_RULE'" class="mt-2 rounded-3xl bg-slate-50 p-4">
                  <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                    <div>
                      <p class="font-semibold text-slate-800">规则配置</p>
                      <p class="mt-1 text-sm text-slate-400">同一组内全部命中才算通过；同一个结果项命中任意一组即可返回。</p>
                    </div>
                    <el-button plain :icon="Plus" @click="addRuleGroup(item)">添加规则组</el-button>
                  </div>

                  <div class="mt-4 space-y-4">
                    <div
                      v-for="group in groupRules(item.rules)"
                      :key="group.groupNo"
                      class="rounded-3xl border border-slate-200 bg-white p-4"
                    >
                      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                        <div>
                          <p class="font-medium text-slate-800">规则组 {{ group.groupNo }}</p>
                          <p class="mt-1 text-xs text-slate-400">组内多条规则按 AND 计算；不同规则组之间按 OR 计算。</p>
                        </div>
                        <div class="flex gap-2">
                          <el-button plain size="small" @click="addRule(item, group.groupNo)">添加规则</el-button>
                          <el-button type="danger" plain size="small" @click="removeRuleGroup(item, group.groupNo)">
                            删除规则组
                          </el-button>
                        </div>
                      </div>

                      <div class="mt-4 space-y-3">
                        <div
                          v-for="(rule, ruleIndex) in group.rules"
                          :key="rule.id || `${group.groupNo}-${ruleIndex}`"
                          class="grid grid-cols-1 gap-3 rounded-2xl border border-slate-100 bg-slate-50 p-3 xl:grid-cols-[110px,160px,160px,minmax(0,1fr),88px]"
                        >
                          <el-input-number v-model="rule.groupNo" :min="1" :step="1" controls-position="right" />

                          <el-select v-model="rule.fieldKey" @change="handleFieldChange(rule)">
                            <el-option
                              v-for="field in meta?.ruleFields || []"
                              :key="field.key"
                              :label="field.label"
                              :value="field.key"
                            />
                          </el-select>

                          <el-select v-model="rule.operator" @change="handleOperatorChange(rule)">
                            <el-option
                              v-for="operator in operatorOptions(rule)"
                              :key="operator.key"
                              :label="operator.label"
                              :value="operator.key"
                            />
                          </el-select>

                          <el-select
                            v-if="isDepartmentField(rule) && isMultipleRule(rule)"
                            v-model="rule.compareValue"
                            multiple
                            filterable
                            collapse-tags
                            collapse-tags-tooltip
                            placeholder="请选择部门"
                          >
                            <el-option
                              v-for="dept in meta?.departmentOptions || []"
                              :key="dept.value"
                              :label="dept.label"
                              :value="dept.value"
                            />
                          </el-select>

                          <el-select
                            v-else-if="isDepartmentField(rule)"
                            v-model="rule.compareValue"
                            filterable
                            placeholder="请选择部门"
                          >
                            <el-option
                              v-for="dept in meta?.departmentOptions || []"
                              :key="dept.value"
                              :label="dept.label"
                              :value="dept.value"
                            />
                          </el-select>

                          <div
                            v-else-if="rule.operator === 'BETWEEN'"
                            class="grid grid-cols-[minmax(0,1fr),24px,minmax(0,1fr)] items-center gap-2"
                          >
                            <el-input-number
                              :model-value="betweenValue(rule, 0)"
                              :controls="false"
                              class="w-full"
                              @update:model-value="updateBetween(rule, 0, $event)"
                            />
                            <span class="text-center text-slate-400">至</span>
                            <el-input-number
                              :model-value="betweenValue(rule, 1)"
                              :controls="false"
                              class="w-full"
                              @update:model-value="updateBetween(rule, 1, $event)"
                            />
                          </div>

                          <el-input-number
                            v-else-if="isNumberField(rule)"
                            v-model="rule.compareValue"
                            :controls="false"
                            class="w-full"
                            placeholder="请输入数值"
                          />

                          <el-select
                            v-else-if="isMultipleRule(rule)"
                            v-model="rule.compareValue"
                            multiple
                            filterable
                            allow-create
                            default-first-option
                            collapse-tags
                            collapse-tags-tooltip
                            placeholder="请输入或选择多个值"
                          />

                          <el-input v-else v-model="rule.compareValue" placeholder="请输入比较值" />

                          <el-button type="danger" plain @click="removeRule(item, rule)">删除</el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </el-card>

              <el-empty v-if="form.items.length === 0" description="暂无结果项">
                <el-button type="primary" plain @click="addItem">添加结果项</el-button>
              </el-empty>
            </div>
          </el-card>
        </el-form>
      </template>

      <el-empty v-else description="请先在左侧选择档案，或新增一个自定义档案。">
        <el-button type="primary" @click="openCreateDialog">添加档案</el-button>
      </el-empty>
    </el-card>

    <el-dialog v-model="typeDialogVisible" title="选择档案类型" width="560px">
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <button
          v-for="type in archiveTypeCards"
          :key="type.value"
          type="button"
          class="type-card rounded-3xl border p-5 text-left transition-all"
          :class="pendingArchiveType === type.value ? 'type-card-active' : ''"
          @click="pendingArchiveType = type.value"
        >
          <p class="font-semibold text-slate-800">{{ type.label }}</p>
          <p class="mt-1 font-mono text-xs text-slate-400">{{ type.subtitle }}</p>
          <p class="mt-2 text-sm leading-6 text-slate-500">{{ type.description }}</p>
        </button>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="typeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="createDraft">开始配置</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Plus, Search } from '@element-plus/icons-vue'
import {
  processApi,
  type ProcessCustomArchiveDetail,
  type ProcessCustomArchiveItem,
  type ProcessCustomArchiveMeta,
  type ProcessCustomArchiveOperator,
  type ProcessCustomArchiveRule,
  type ProcessCustomArchiveSavePayload,
  type ProcessCustomArchiveSummary
} from '@/api'

type ArchiveTypeValue = 'SELECT' | 'AUTO_RULE'

type ArchiveTypeCard = {
  value: ArchiveTypeValue
  label: string
  subtitle: string
  description: string
}

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '启用', value: 'enabled' },
  { label: '停用', value: 'disabled' }
]

const defaultArchiveTypeCards: ArchiveTypeCard[] = [
  {
    value: 'SELECT',
    label: '提供选择',
    subtitle: 'SELECT',
    description: '维护一组可选结果，业务页面会直接作为下拉或多选项使用。'
  },
  {
    value: 'AUTO_RULE',
    label: '自动划分',
    subtitle: 'AUTO_RULE',
    description: '根据提单上下文和规则自动命中结果，可返回一个或多个符合条件的结果项。'
  }
]

const loadingList = ref(false)
const loadingDetail = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('all')
const typeDialogVisible = ref(false)
const pendingArchiveType = ref<ArchiveTypeValue>('SELECT')
const activeArchiveId = ref<number | null>(null)

const archiveList = ref<ProcessCustomArchiveSummary[]>([])
const meta = ref<ProcessCustomArchiveMeta | null>(null)
const form = ref<ProcessCustomArchiveDetail | null>(null)

const archiveTypeCards = computed<ArchiveTypeCard[]>(() => {
  const backendMap = new Map((meta.value?.archiveTypeOptions || []).map((item) => [item.value, item.label]))
  return defaultArchiveTypeCards.map((item) => ({
    ...item,
    label: backendMap.get(item.value) || item.label
  }))
})

const filteredArchives = computed(() =>
  archiveList.value.filter((archive) => {
    const text = keyword.value.trim().toLowerCase()
    const matchKeyword =
      !text ||
      archive.archiveName.toLowerCase().includes(text) ||
      archive.archiveCode.toLowerCase().includes(text)

    if (!matchKeyword) {
      return false
    }
    if (statusFilter.value === 'enabled') {
      return archive.status === 1
    }
    if (statusFilter.value === 'disabled') {
      return archive.status === 0
    }
    return true
  })
)

const archiveTypeLabel = computed(() => resolveArchiveTypeLabel(form.value?.archiveType))

const archiveTypeDescription = computed(() => {
  const matched = archiveTypeCards.value.find((item) => item.value === form.value?.archiveType)
  return matched?.description || ''
})

const archiveCodeDisplay = computed(() => form.value?.archiveCode || '保存后自动生成：CA + 年月日 + 4位流水')

const statusSwitch = computed({
  get: () => (form.value?.status ?? 1) === 1,
  set: (value: boolean) => {
    if (form.value) {
      form.value.status = value ? 1 : 0
    }
  }
})

onMounted(refreshAll)

function resolveArchiveTypeLabel(type?: string) {
  const matched = archiveTypeCards.value.find((item) => item.value === type)
  return matched?.label || form.value?.archiveTypeLabel || type || ''
}

function itemCodeDisplay(item: ProcessCustomArchiveItem) {
  return item.itemCode || '保存后自动生成：CI + 年月日 + 4位流水'
}

async function refreshAll() {
  try {
    await Promise.all([loadMeta(), loadList()])
  } catch (error: any) {
    ElMessage.error(error.message || '加载自定义档案失败')
  }
}

async function loadMeta() {
  const res = await processApi.getCustomArchiveMeta()
  if (res.code === 200) {
    meta.value = res.data
  }
}

async function loadList(preferredId?: number | null) {
  loadingList.value = true

  try {
    const res = await processApi.listCustomArchives()
    if (res.code !== 200) {
      return
    }

    archiveList.value = res.data

    const targetId = preferredId || activeArchiveId.value || archiveList.value[0]?.id
    if (targetId && (!form.value?.id || preferredId || activeArchiveId.value !== targetId)) {
      await loadDetail(targetId)
      return
    }

    if (!targetId && !form.value?.id) {
      form.value = null
      activeArchiveId.value = null
    }
  } finally {
    loadingList.value = false
  }
}

async function loadDetail(id: number) {
  loadingDetail.value = true

  try {
    const res = await processApi.getCustomArchiveDetail(id)
    if (res.code === 200) {
      activeArchiveId.value = id
      form.value = structuredClone(res.data)
    }
  } finally {
    loadingDetail.value = false
  }
}

async function selectArchive(id: number) {
  try {
    await loadDetail(id)
  } catch (error: any) {
    ElMessage.error(error.message || '加载档案详情失败')
  }
}

function openCreateDialog() {
  pendingArchiveType.value = archiveTypeCards.value[0]?.value || 'SELECT'
  typeDialogVisible.value = true
}

function createDraft() {
  const type = pendingArchiveType.value
  const selectedType = archiveTypeCards.value.find((item) => item.value === type)

  typeDialogVisible.value = false
  activeArchiveId.value = null
  form.value = {
    archiveName: '',
    archiveType: type,
    archiveTypeLabel: selectedType?.label || type,
    archiveDescription: '',
    status: 1,
    items: [createItem(type, 0)]
  }
}

function createItem(type: string, index: number): ProcessCustomArchiveItem {
  return {
    itemName: '',
    priority: index + 1,
    status: 1,
    rules: type === 'AUTO_RULE' ? [createRule(1)] : []
  }
}

function createRule(groupNo: number): ProcessCustomArchiveRule {
  const fieldKey = meta.value?.ruleFields[0]?.key || 'submitterDeptId'
  const operator = fieldOperators(fieldKey)[0] || 'EQ'

  return {
    groupNo,
    fieldKey,
    operator,
    compareValue: defaultCompareValue(fieldKey, operator)
  }
}

function addItem() {
  if (!form.value) {
    return
  }
  form.value.items.push(createItem(form.value.archiveType, form.value.items.length))
}

function removeItem(index: number) {
  form.value?.items.splice(index, 1)
}

function addRuleGroup(item: ProcessCustomArchiveItem) {
  const nextGroupNo = item.rules.reduce((max, rule) => Math.max(max, rule.groupNo || 1), 0) + 1
  item.rules.push(createRule(nextGroupNo))
}

function addRule(item: ProcessCustomArchiveItem, groupNo: number) {
  item.rules.push(createRule(groupNo))
}

function removeRule(item: ProcessCustomArchiveItem, rule: ProcessCustomArchiveRule) {
  item.rules = item.rules.filter((entry) => entry !== rule)
}

function removeRuleGroup(item: ProcessCustomArchiveItem, groupNo: number) {
  item.rules = item.rules.filter((rule) => rule.groupNo !== groupNo)
}

function groupRules(rules: ProcessCustomArchiveRule[]) {
  const map = new Map<number, ProcessCustomArchiveRule[]>()

  rules
    .slice()
    .sort((a, b) => (a.groupNo || 1) - (b.groupNo || 1))
    .forEach((rule) => {
      const groupNo = rule.groupNo || 1
      map.set(groupNo, [...(map.get(groupNo) || []), rule])
    })

  return Array.from(map.entries()).map(([groupNo, groupedRules]) => ({
    groupNo,
    rules: groupedRules
  }))
}

function fieldOperators(fieldKey: string) {
  return meta.value?.ruleFields.find((field) => field.key === fieldKey)?.operatorKeys || []
}

function operatorOptions(rule: ProcessCustomArchiveRule): ProcessCustomArchiveOperator[] {
  const keys = fieldOperators(rule.fieldKey)
  return (meta.value?.operatorOptions || []).filter((operator) => keys.includes(operator.key))
}

function handleFieldChange(rule: ProcessCustomArchiveRule) {
  const operators = fieldOperators(rule.fieldKey)
  if (!operators.includes(rule.operator)) {
    rule.operator = operators[0] || 'EQ'
  }
  rule.compareValue = defaultCompareValue(rule.fieldKey, rule.operator)
}

function handleOperatorChange(rule: ProcessCustomArchiveRule) {
  rule.compareValue = defaultCompareValue(rule.fieldKey, rule.operator)
}

function fieldType(rule: ProcessCustomArchiveRule) {
  return meta.value?.ruleFields.find((field) => field.key === rule.fieldKey)?.valueType || 'text'
}

function defaultCompareValue(fieldKey: string, operator: string) {
  const type = meta.value?.ruleFields.find((field) => field.key === fieldKey)?.valueType || 'text'
  if (operator === 'BETWEEN') {
    return [null, null]
  }
  if (operator === 'IN' || operator === 'NOT_IN') {
    return []
  }
  if (type === 'number') {
    return null
  }
  return ''
}

function isDepartmentField(rule: ProcessCustomArchiveRule) {
  return fieldType(rule) === 'department'
}

function isNumberField(rule: ProcessCustomArchiveRule) {
  return fieldType(rule) === 'number' && rule.operator !== 'BETWEEN'
}

function isMultipleRule(rule: ProcessCustomArchiveRule) {
  return rule.operator === 'IN' || rule.operator === 'NOT_IN'
}

function betweenValue(rule: ProcessCustomArchiveRule, index: number) {
  return Array.isArray(rule.compareValue) ? rule.compareValue[index] ?? null : null
}

function updateBetween(rule: ProcessCustomArchiveRule, index: number, value?: number) {
  const values = Array.isArray(rule.compareValue) ? [...rule.compareValue] : [null, null]
  values[index] = value ?? null
  rule.compareValue = values
}

function validate(current: ProcessCustomArchiveDetail) {
  if (!current.archiveName.trim()) {
    throw new Error('请输入档案名称')
  }
  if (current.items.length === 0) {
    throw new Error('请至少保留一个结果项')
  }

  current.items.forEach((item, index) => {
    if (!item.itemName.trim()) {
      throw new Error(`请输入第 ${index + 1} 个结果项名称`)
    }

    if (current.archiveType === 'AUTO_RULE') {
      if (!item.rules.length) {
        throw new Error(`第 ${index + 1} 个结果项至少需要一条规则`)
      }

      item.rules.forEach((rule, ruleIndex) => {
        if (!rule.fieldKey) {
          throw new Error(`第 ${index + 1} 个结果项的第 ${ruleIndex + 1} 条规则缺少字段`)
        }
        if (!rule.operator) {
          throw new Error(`第 ${index + 1} 个结果项的第 ${ruleIndex + 1} 条规则缺少运算符`)
        }
      })
    }
  })
}

function buildPayload(current: ProcessCustomArchiveDetail): ProcessCustomArchiveSavePayload {
  return {
    archiveName: current.archiveName.trim(),
    archiveType: current.archiveType,
    archiveDescription: current.archiveDescription?.trim() || '',
    status: current.status ?? 1,
    items: current.items.map((item, itemIndex) => ({
      id: item.id,
      itemName: item.itemName.trim(),
      priority: current.archiveType === 'AUTO_RULE' ? item.priority ?? itemIndex + 1 : undefined,
      status: item.status ?? 1,
      rules:
        current.archiveType === 'AUTO_RULE'
          ? item.rules.map((rule) => ({
              id: rule.id,
              groupNo: rule.groupNo || 1,
              fieldKey: rule.fieldKey,
              operator: rule.operator,
              compareValue: normalizeRuleValue(rule)
            }))
          : []
    }))
  }
}

function normalizeRuleValue(rule: ProcessCustomArchiveRule) {
  if (rule.operator === 'BETWEEN') {
    const values = Array.isArray(rule.compareValue) ? rule.compareValue : [null, null]
    return [values[0] ?? null, values[1] ?? null]
  }

  if (rule.operator === 'IN' || rule.operator === 'NOT_IN') {
    return Array.isArray(rule.compareValue)
      ? rule.compareValue.map((value) => String(value).trim()).filter(Boolean)
      : []
  }

  if (fieldType(rule) === 'number') {
    return rule.compareValue ?? null
  }

  return String(rule.compareValue ?? '').trim()
}

async function saveArchive() {
  if (!form.value) {
    return
  }

  try {
    validate(form.value)
    saving.value = true

    const payload = buildPayload(form.value)
    const res = form.value.id
      ? await processApi.updateCustomArchive(form.value.id, payload)
      : await processApi.createCustomArchive(payload)

    if (res.code === 200) {
      form.value = structuredClone(res.data)
      activeArchiveId.value = res.data.id || null
      await loadList(res.data.id || null)
      ElMessage.success('档案保存成功')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '档案保存失败')
  } finally {
    saving.value = false
  }
}

async function changeStatus(value: string | number | boolean) {
  if (!form.value) {
    return
  }

  form.value.status = value ? 1 : 0
  if (!form.value.id) {
    return
  }

  try {
    await processApi.updateCustomArchiveStatus(form.value.id, { status: form.value.status })
    await loadList(form.value.id)
    ElMessage.success(form.value.status === 1 ? '档案已启用' : '档案已停用')
  } catch (error: any) {
    form.value.status = form.value.status === 1 ? 0 : 1
    ElMessage.error(error.message || '更新档案状态失败')
  }
}

async function removeArchive() {
  if (!form.value?.id) {
    return
  }

  try {
    await ElMessageBox.confirm('删除后不可恢复，结果项和规则也会一并删除，是否继续？', '删除档案', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    await processApi.deleteCustomArchive(form.value.id)
    form.value = null
    activeArchiveId.value = null
    await loadList()
    ElMessage.success('档案已删除')
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除档案失败')
  }
}
</script>

<style scoped>
.archive-item {
  border-color: #e2e8f0;
}

.archive-item:hover {
  border-color: #93c5fd;
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.08);
}

.archive-item-active {
  border-color: #2563eb;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.92) 0%, #ffffff 100%);
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.12);
}

.section-card {
  border: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.result-card {
  border: 1px solid #dbeafe;
}

.result-index {
  display: inline-flex;
  height: 32px;
  width: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}

.type-card {
  border-color: #e2e8f0;
}

.type-card:hover,
.type-card-active {
  border-color: #2563eb;
  background: #eff6ff;
}

.code-preview {
  width: 100%;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  background: #f8fafc;
  padding: 12px 16px;
}
</style>
