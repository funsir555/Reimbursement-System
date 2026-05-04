<template>
  <div class="expense-wb-page expense-wb-page--config space-y-6" data-testid="user-group-panel">
    <section class="expense-wb-stat-grid expense-wb-stat-grid--compact" data-testid="user-group-summary-grid">
      <article
        v-for="stat in summaryCards"
        :key="stat.label"
        class="expense-wb-stat-card expense-wb-stat-card--compact"
      >
        <div class="expense-wb-stat-card__top">
          <div>
            <p class="expense-wb-stat-card__label">{{ stat.label }}</p>
            <p class="expense-wb-stat-card__value">{{ stat.value }}</p>
          </div>
          <span class="expense-wb-stat-card__icon" :class="`expense-wb-stat-card__icon--${stat.tone}`">
            <el-icon :size="22">
              <component :is="stat.icon" />
            </el-icon>
          </span>
        </div>
      </article>
    </section>

    <div class="grid grid-cols-1 gap-6 xl:grid-cols-[360px,minmax(0,1fr)]">
      <el-card class="expense-wb-panel">
        <template #header>
          <div class="flex items-center justify-between gap-3">
            <h2 class="text-lg font-semibold text-slate-800">用户组</h2>
            <el-button type="primary" :icon="Plus" @click="createRootGroup">新增一级组</el-button>
          </div>
        </template>

        <div class="space-y-4">
          <el-input
            v-model="keyword"
            placeholder="搜索用户组名称"
            :prefix-icon="Search"
            clearable
          />

          <div v-loading="loadingTree" class="min-h-[520px]" data-testid="user-group-tree">
            <el-tree
              v-if="filteredTree.length"
              ref="treeRef"
              class="user-group-tree"
              :data="filteredTree"
              node-key="id"
              highlight-current
              default-expand-all
              :expand-on-click-node="false"
              :props="{ label: 'groupName', children: 'children' }"
              @node-click="handleTreeNodeClick"
            >
              <template #default="{ data }">
                <div class="user-group-tree-node">
                  <p class="truncate text-[13px] font-semibold leading-4 text-slate-800">{{ data.groupName }}</p>
                </div>
              </template>
            </el-tree>

            <el-empty v-else description="暂无用户组">
              <el-button type="primary" @click="createRootGroup">新增一级组</el-button>
            </el-empty>
          </div>
        </div>
      </el-card>

      <el-card class="expense-wb-panel" data-testid="user-group-form">
        <template #header>
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <h2 class="text-xl font-semibold text-slate-800">{{ panelTitle }}</h2>
                <el-tag v-if="editor?.codeLevel" effect="plain">{{ resolveLevelLabel(editor.codeLevel) }}</el-tag>
              </div>
              <p class="mt-2 font-mono text-sm text-slate-400">{{ codeDisplayText }}</p>
            </div>

            <div class="flex flex-wrap gap-3">
              <el-button :disabled="!editorReady" @click="createSiblingGroup">新增同级</el-button>
              <el-button :disabled="!canCreateChild" @click="createChildGroup">新增下级</el-button>
              <el-button
                v-if="editor?.id"
                type="danger"
                plain
                :icon="Delete"
                :loading="deleting"
                @click="deleteCurrentGroup"
              >
                删除
              </el-button>
              <el-button type="primary" :icon="Check" :loading="saving" :disabled="!editor" @click="saveCurrentGroup">
                保存
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="loadingDetail" class="py-8">
          <el-skeleton animated :rows="10" />
        </div>

        <template v-else-if="editor">
          <el-form label-position="top" class="space-y-6">
            <el-card class="section-card !rounded-3xl !shadow-none">
              <template #header>
                <span class="font-semibold text-slate-800">用户组明细</span>
              </template>

              <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
                <el-form-item label="用户组名称" required class="xl:col-span-2">
                  <el-input
                    v-model="editor.groupName"
                    maxlength="64"
                    show-word-limit
                    placeholder="请输入用户组名称"
                  />
                </el-form-item>

                <template v-if="editor.codeLevel === 3">
                  <el-form-item label="用户组成员" class="xl:col-span-2">
                    <el-select
                      v-model="editor.memberUserIds"
                      multiple
                      filterable v-bind="globalFilterableSelectProps"
                      clearable
                      collapse-tags
                      collapse-tags-tooltip
                      :tag-tooltip="globalCollapseTagTooltipProps"
                      placeholder="请选择用户组成员"
                    >
                      <el-option
                        v-for="item in meta?.userOptions || []"
                        :key="item.value"
                        :label="item.label"
                        :value="String(item.value)"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="用户组管理范围" class="xl:col-span-2">
                    <div class="scope-summary-card">
                      <button
                        type="button"
                        class="scope-summary-card__trigger"
                        data-testid="user-group-scope-trigger"
                        @click="openScopeDialog"
                      >
                        {{ scopeSummaryText }}
                      </button>
                      <p class="text-xs leading-6 text-slate-400">
                        管理范围采用“组间或、组内且”规则；留空表示全范围管理。
                      </p>
                    </div>
                  </el-form-item>
                </template>
              </div>
            </el-card>

            <el-card v-if="editor.codeLevel !== 3" class="section-card !rounded-3xl !shadow-none">
              <template #header>
                <span class="font-semibold text-slate-800">级别说明</span>
              </template>
              <p class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-7 text-slate-600">
                {{ levelDescriptionText }}
              </p>
            </el-card>
          </el-form>
        </template>

        <el-empty v-else description="请先在左侧选择用户组，或新建一个一级组。">
          <el-button type="primary" @click="createRootGroup">新增一级组</el-button>
        </el-empty>
      </el-card>
    </div>

    <el-dialog
      v-model="scopeDialogVisible"
      title="配置用户组管理范围"
      width="920px"
      destroy-on-close
      data-testid="user-group-scope-dialog"
    >
      <ProcessConditionGroupEditor
        :groups="scopeDialogGroups"
        :fields="meta?.scopeConditionFields || []"
        :operator-options="meta?.scopeOperatorOptions || []"
        :option-sources="scopeOptionSources"
        title="管理范围"
        :summary="scopeDialogSummary"
        group-label-prefix="范围组"
        group-hint="每组内条件同时满足即命中，任意一组命中即可纳入该用户组。"
        empty-groups-text="当前还没有范围组"
        empty-conditions-text="当前范围组还没有条件项"
        :handlers="scopeDialogHandlers"
      />

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="scopeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="applyScopeDialog">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, CircleCheckFilled, Delete, Files, Plus, Search } from '@element-plus/icons-vue'
import {
  processApi,
  type ProcessFlowCondition,
  type ProcessFlowConditionField,
  type ProcessFlowConditionGroup,
  type ProcessUserGroupDetail,
  type ProcessUserGroupMeta,
  type ProcessUserGroupSavePayload,
  type ProcessUserGroupTreeNode
} from '@/api'
import { globalCollapseTagTooltipProps } from '@/utils/collapseTagTooltip'
import { PM_NAME_MAX_LENGTH, validateMaxLength } from '@/views/process/pmValidation'
import ProcessConditionGroupEditor from '@/components/process/ProcessConditionGroupEditor.vue'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'


type UserGroupTreeInstance = {
  setCurrentKey: (key: number | null) => void
}

const treeRef = ref<UserGroupTreeInstance | null>(null)
const loadingTree = ref(false)
const loadingDetail = ref(false)
const saving = ref(false)
const deleting = ref(false)
const keyword = ref('')
const activeGroupId = ref<number>()
const treeData = ref<ProcessUserGroupTreeNode[]>([])
const meta = ref<ProcessUserGroupMeta | null>(null)
const editor = ref<ProcessUserGroupDetail | null>(null)
const scopeDialogVisible = ref(false)
const scopeDialogGroups = ref<ProcessFlowConditionGroup[]>([])

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function cloneValue<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

function normalizeEditorDetail(detail: ProcessUserGroupDetail): ProcessUserGroupDetail {
  return {
    id: detail.id,
    parentId: detail.parentId,
    groupCode: detail.groupCode || '',
    groupName: detail.groupName || '',
    codeLevel: detail.codeLevel || 1,
    memberUserIds: Array.isArray(detail.memberUserIds)
      ? detail.memberUserIds.map((item) => String(item)).filter(Boolean)
      : [],
    scopeConditionGroups: Array.isArray(detail.scopeConditionGroups)
      ? cloneValue(detail.scopeConditionGroups)
      : []
  }
}

function normalizeConditionField(field?: ProcessFlowConditionField) {
  const fieldKey = field?.key || ''
  const operator = field?.operatorKeys?.[0] || 'EQ'
  const compareValue = field?.valueType === 'number'
    ? null
    : operator === 'IN' || operator === 'NOT_IN'
      ? []
      : ''
  return {
    fieldKey,
    operator,
    compareValue
  } as ProcessFlowCondition
}

function renumberConditionGroups(groups: ProcessFlowConditionGroup[]) {
  return groups.map((group, index) => ({
    ...group,
    groupNo: index + 1
  }))
}

function createEmptyConditionGroup(): ProcessFlowConditionGroup {
  return {
    groupNo: 1,
    conditions: [normalizeConditionField(meta.value?.scopeConditionFields?.[0])]
  }
}

function flattenTree(nodes: ProcessUserGroupTreeNode[]): ProcessUserGroupTreeNode[] {
  const result: ProcessUserGroupTreeNode[] = []
  nodes.forEach((node) => {
    result.push(node)
    result.push(...flattenTree(node.children || []))
  })
  return result
}

function findTreeNodeById(nodes: ProcessUserGroupTreeNode[], id?: number): ProcessUserGroupTreeNode | undefined {
  if (!id) {
    return undefined
  }
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    const child = findTreeNodeById(node.children || [], id)
    if (child) {
      return child
    }
  }
  return undefined
}

function filterTree(nodes: ProcessUserGroupTreeNode[], searchText: string): ProcessUserGroupTreeNode[] {
  if (!searchText) {
    return nodes
  }
  return nodes
    .map((node) => {
      const children = filterTree(node.children || [], searchText)
      if (node.groupName.includes(searchText) || children.length) {
        return {
          ...node,
          children
        }
      }
      return null
    })
    .filter((node): node is ProcessUserGroupTreeNode => Boolean(node))
}

function resolveLevelLabel(level?: number) {
  switch (level) {
    case 1:
      return '一级分类组'
    case 2:
      return '二级分配组'
    case 3:
      return '三级功能组'
    default:
      return '未定级用户组'
  }
}

function resolveLevelDescription(level?: number) {
  switch (level) {
    case 1:
      return '一级组只承担分类归集作用，不配置成员和管理范围，也不会被审批流直接引用。'
    case 2:
      return '二级组只承担分配入口作用，不配置成员和管理范围；审批流只能选择这一层。'
    case 3:
      return '三级组才真正承担业务功能，可配置用户组成员与管理范围，并参与审批人命中。'
    default:
      return '请先选择或新建用户组。'
  }
}

function createDraft(parentId?: number, level = 1): ProcessUserGroupDetail {
  return {
    parentId,
    groupCode: '',
    groupName: '',
    codeLevel: level,
    memberUserIds: [],
    scopeConditionGroups: []
  }
}

async function syncTreeSelection() {
  await nextTick()
  treeRef.value?.setCurrentKey(activeGroupId.value ?? null)
}

async function loadTree() {
  loadingTree.value = true
  try {
    const { data } = await processApi.listUserGroupTree()
    treeData.value = Array.isArray(data) ? data : []
    if (activeGroupId.value && !findTreeNodeById(treeData.value, activeGroupId.value)) {
      activeGroupId.value = undefined
    }
    await syncTreeSelection()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '加载用户组列表失败'))
  } finally {
    loadingTree.value = false
  }
}

async function loadMeta() {
  try {
    const { data } = await processApi.getUserGroupMeta()
    meta.value = data
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '加载用户组元数据失败'))
  }
}

async function loadGroupDetail(id: number) {
  loadingDetail.value = true
  try {
    const { data } = await processApi.getUserGroupDetail(id)
    editor.value = normalizeEditorDetail(data)
    activeGroupId.value = id
    await syncTreeSelection()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '加载用户组详情失败'))
  } finally {
    loadingDetail.value = false
  }
}

function handleTreeNodeClick(node: ProcessUserGroupTreeNode) {
  void loadGroupDetail(node.id)
}

function clearTreeSelection() {
  activeGroupId.value = undefined
  void syncTreeSelection()
}

function createRootGroup() {
  editor.value = createDraft(undefined, 1)
  clearTreeSelection()
}

function createSiblingGroup() {
  if (!editor.value) {
    createRootGroup()
    return
  }
  editor.value = createDraft(editor.value.parentId, editor.value.codeLevel || 1)
  clearTreeSelection()
}

function createChildGroup() {
  if (!editor.value) {
    return
  }
  const currentLevel = editor.value.codeLevel || 1
  if (currentLevel >= 3) {
    ElMessage.warning('三级用户组不能再新增下级')
    return
  }
  const parentId = editor.value.id
  if (!parentId) {
    ElMessage.warning('请先保存当前用户组，再新增下级')
    return
  }
  editor.value = createDraft(parentId, currentLevel + 1)
  clearTreeSelection()
}

async function saveCurrentGroup() {
  if (!editor.value) {
    return
  }
  const nameIssue = validateMaxLength(editor.value.groupName, PM_NAME_MAX_LENGTH, '用户组名称')
  if (nameIssue) {
    ElMessage.warning(nameIssue)
    return
  }
  if (!editor.value.groupName.trim()) {
    ElMessage.warning('用户组名称不能为空')
    return
  }

  const payload: ProcessUserGroupSavePayload = {
    parentId: editor.value.parentId,
    groupName: editor.value.groupName.trim(),
    memberUserIds: editor.value.codeLevel === 3 ? editor.value.memberUserIds : [],
    scopeConditionGroups: editor.value.codeLevel === 3 ? editor.value.scopeConditionGroups : []
  }

  saving.value = true
  try {
    const response = editor.value.id
      ? await processApi.updateUserGroup(editor.value.id, payload)
      : await processApi.createUserGroup(payload)
    const saved = normalizeEditorDetail(response.data)
    editor.value = saved
    activeGroupId.value = saved.id
    await loadTree()
    await syncTreeSelection()
    ElMessage.success('用户组已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存用户组失败'))
  } finally {
    saving.value = false
  }
}

async function deleteCurrentGroup() {
  if (!editor.value?.id) {
    return
  }
  const currentId = editor.value.id
  const fallbackId = editor.value.parentId
  try {
    await ElMessageBox.confirm('删除后将不可恢复，确认删除当前用户组吗？', '删除用户组', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  deleting.value = true
  try {
    await processApi.deleteUserGroup(currentId)
    await loadTree()
    const nextId = fallbackId && findTreeNodeById(treeData.value, fallbackId)
      ? fallbackId
      : flattenTree(treeData.value)[0]?.id
    if (nextId) {
      await loadGroupDetail(nextId)
    } else {
      editor.value = null
      clearTreeSelection()
    }
    ElMessage.success('用户组已删除')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '删除用户组失败'))
  } finally {
    deleting.value = false
  }
}

function openScopeDialog() {
  if (!editor.value || editor.value.codeLevel !== 3) {
    return
  }
  scopeDialogGroups.value = cloneValue(editor.value.scopeConditionGroups || [])
  scopeDialogVisible.value = true
}

function addScopeGroup() {
  const nextGroup = createEmptyConditionGroup()
  scopeDialogGroups.value = renumberConditionGroups([
    ...scopeDialogGroups.value,
    {
      ...nextGroup,
      groupNo: scopeDialogGroups.value.length + 1
    }
  ])
}

function removeScopeGroup(groupNo: number) {
  scopeDialogGroups.value = renumberConditionGroups(
    scopeDialogGroups.value.filter((group) => group.groupNo !== groupNo)
  )
}

function addScopeCondition(groupNo: number) {
  scopeDialogGroups.value = scopeDialogGroups.value.map((group) => (
    group.groupNo === groupNo
      ? {
        ...group,
        conditions: [
          ...(group.conditions || []),
          normalizeConditionField(meta.value?.scopeConditionFields?.[0])
        ]
      }
      : group
  ))
}

function removeScopeCondition(groupNo: number, index: number) {
  scopeDialogGroups.value = scopeDialogGroups.value.map((group) => {
    if (group.groupNo !== groupNo) {
      return group
    }
    return {
      ...group,
      conditions: (group.conditions || []).filter((_, conditionIndex) => conditionIndex !== index)
    }
  })
}

function applyScopeDialog() {
  if (!editor.value) {
    scopeDialogVisible.value = false
    return
  }
  editor.value.scopeConditionGroups = cloneValue(scopeDialogGroups.value)
  scopeDialogVisible.value = false
}

const filteredTree = computed(() => filterTree(treeData.value, keyword.value.trim()))
const flattenedGroups = computed(() => flattenTree(treeData.value))
const summaryCards = computed(() => [
  {
    label: '用户组总数',
    value: flattenedGroups.value.length,
    icon: Files,
    tone: 'blue'
  },
  {
    label: '二级分配组',
    value: flattenedGroups.value.filter((item) => item.codeLevel === 2).length,
    icon: CircleCheckFilled,
    tone: 'green'
  },
  {
    label: '三级功能组',
    value: flattenedGroups.value.filter((item) => item.codeLevel === 3).length,
    icon: Files,
    tone: 'amber'
  },
  {
    label: '一级分类组',
    value: flattenedGroups.value.filter((item) => item.codeLevel === 1).length,
    icon: Search,
    tone: 'rose'
  }
])
const editorReady = computed(() => Boolean(editor.value))
const canCreateChild = computed(() => Boolean(editor.value?.id) && Number(editor.value?.codeLevel || 1) < 3)
const panelTitle = computed(() => {
  if (!editor.value) {
    return '用户组配置'
  }
  return editor.value.id ? (editor.value.groupName || '未命名用户组') : `新建${resolveLevelLabel(editor.value.codeLevel)}`
})
const codeDisplayText = computed(() => {
  if (!editor.value) {
    return '保存后自动生成 4-2-2 编码'
  }
  return editor.value.groupCode || '保存后自动生成 4-2-2 编码'
})
const levelDescriptionText = computed(() => resolveLevelDescription(editor.value?.codeLevel))
const scopeSummaryText = computed(() => {
  const groups = editor.value?.scopeConditionGroups || []
  if (!groups.length) {
    return '全范围管理'
  }
  const conditionCount = groups.reduce((total, group) => total + (group.conditions?.length || 0), 0)
  return `已配置 ${groups.length} 组条件 / ${conditionCount} 条逻辑`
})
const scopeDialogSummary = computed(() => {
  if (!scopeDialogGroups.value.length) {
    return '当前未限制管理范围，保存后将按全范围管理处理。'
  }
  const conditionCount = scopeDialogGroups.value.reduce((total, group) => total + (group.conditions?.length || 0), 0)
  return `当前共 ${scopeDialogGroups.value.length} 组范围条件，${conditionCount} 条逻辑。`
})
const scopeOptionSources = computed(() => ({
  company: meta.value?.companyOptions || [],
  department: meta.value?.departmentOptions || [],
  user: meta.value?.userOptions || []
}))
const scopeDialogHandlers = {
  addGroup: addScopeGroup,
  removeGroup: removeScopeGroup,
  addCondition: addScopeCondition,
  removeCondition: removeScopeCondition
}

onMounted(async () => {
  await Promise.all([loadTree(), loadMeta()])
  if (!activeGroupId.value) {
    const firstId = flattenTree(treeData.value)[0]?.id
    if (firstId) {
      await loadGroupDetail(firstId)
    }
  }
})
</script>

<style scoped>
.user-group-tree-node {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  padding: 0 4px 0 0;
}

:deep(.user-group-tree .el-tree-node__content) {
  height: 30px;
  min-height: 30px;
  align-items: center;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  background: transparent;
}

:deep(.user-group-tree .el-tree-node) {
  margin: 0;
}

:deep(.user-group-tree .el-tree-node__content:hover) {
  background: transparent;
}

:deep(.user-group-tree .el-tree-node.is-current > .el-tree-node__content) {
  background: transparent;
}

:deep(.user-group-tree .el-tree-node:focus > .el-tree-node__content) {
  background: transparent;
}

:deep(.user-group-tree .el-tree-node__expand-icon) {
  margin-top: 0;
}

.scope-summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  border: 1px solid rgb(226 232 240);
  border-radius: 18px;
  background: rgb(248 250 252);
  padding: 14px 16px;
}

.scope-summary-card__trigger {
  width: fit-content;
  border: none;
  background: transparent;
  padding: 0;
  color: rgb(37 99 235);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.scope-summary-card__trigger:hover {
  color: rgb(29 78 216);
}
</style>
