<template>
  <div class="expense-wb-page expense-wb-page--approval expense-wb-page--dense-list">
    <section
      class="expense-wb-stat-grid expense-wb-stat-grid--compact expense-wb-stat-grid--dense expense-wb-stat-grid--list-dense"
    >
      <article
        v-for="stat in approvalStats"
        :key="stat.label"
        class="expense-wb-stat-card expense-wb-stat-card--compact expense-wb-stat-card--dense"
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

    <el-card class="expense-wb-toolbar expense-wb-toolbar--compact expense-wb-toolbar--dense">
      <div
        class="expense-wb-toolbar__row expense-wb-toolbar__row--compact expense-wb-toolbar__row--dense expense-wb-toolbar__main"
        data-testid="expense-toolbar-main"
      >
        <div
          class="expense-wb-toolbar__heading expense-wb-toolbar__heading--compact expense-wb-toolbar__heading--inline"
          data-testid="expense-toolbar-heading"
        >
          <p class="expense-wb-toolbar__title">任务检索</p>
          <div class="expense-wb-toolbar__meta">
            <span class="expense-wb-soft-badge">待审 {{ items.length }}</span>
            <span class="expense-wb-soft-badge expense-wb-soft-badge--warn">金额合计 {{ totalAmountText }}</span>
          </div>
        </div>

        <div class="expense-wb-toolbar__group">
          <el-button
            data-testid="expense-advanced-filter-trigger"
            :type="showAdvancedFilters ? 'primary' : 'default'"
            :icon="Filter"
            @click="showAdvancedFilters = !showAdvancedFilters"
          >
            高级筛选
          </el-button>
          <el-popover placement="bottom-end" :width="360" trigger="click">
            <template #reference>
              <el-button :icon="Operation">显示字段</el-button>
            </template>
            <div class="expense-wb-column-panel">
              <div class="expense-wb-column-panel__header">
                <p class="expense-wb-column-panel__title">选择列表显示字段</p>
                <el-button link type="primary" @click="restoreDefaultColumns">恢复默认</el-button>
              </div>
              <div class="expense-wb-column-panel__grid">
                <div
                  v-for="column in columnPanelItems"
                  :key="column.key"
                  class="expense-wb-column-item"
                  :class="{
                    'is-dragging': draggingColumnKey === column.key,
                    'is-drop-target': dropTargetColumnKey === column.key
                  }"
                  draggable="true"
                  @dragstart="handleColumnDragStart(column.key)"
                  @dragover.prevent="handleColumnDragOver(column.key)"
                  @drop.prevent="handleColumnDrop(column.key)"
                  @dragend="handleColumnDragEnd"
                >
                  <span class="expense-wb-column-item__handle">⋮⋮</span>
                  <el-checkbox
                    :model-value="visibleColumns.includes(column.key)"
                    @change="handleColumnVisibilityToggle(column.key, $event)"
                  >
                    {{ column.label }}
                  </el-checkbox>
                </div>
              </div>
            </div>
          </el-popover>

          <div class="expense-wb-toolbar__actions">
            <el-button
              :icon="Download"
              :loading="exporting"
              data-testid="expense-approval-export-trigger"
              @click="handleExport"
            >
              下载
            </el-button>
          </div>
        </div>
      </div>

      <div
        v-if="showAdvancedFilters"
        class="expense-wb-advanced-panel expense-wb-advanced-panel--dropdown"
        data-testid="expense-advanced-panel"
      >
        <div class="expense-wb-advanced-grid expense-wb-advanced-grid--four-column" data-testid="expense-advanced-grid">
          <el-input v-model="filters.documentCode" clearable placeholder="单据编号" />
          <el-input v-model="filters.submitterName" clearable placeholder="提单人" />
          <el-input v-model="filters.templateName" clearable placeholder="模板名称" />
          <el-select v-model="filters.documentStatusLabel" clearable placeholder="状态">
            <el-option label="全部" value="" />
            <el-option v-for="status in statusOptions" :key="status" :label="status" :value="status" />
          </el-select>
          <el-date-picker
            v-model="filters.submittedDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="提交开始日期"
            end-placeholder="提交结束日期"
          />
          <el-date-picker
            v-model="filters.paymentDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="支付开始日期"
            end-placeholder="支付结束日期"
          />
          <el-input v-model="filters.paymentCompanyName" clearable placeholder="付款公司" />
          <el-input v-model="filters.payeeName" clearable placeholder="收款人" />
          <el-input v-model="filters.counterpartyName" clearable placeholder="往来单位" />
          <el-input v-model="filters.submitterDeptName" clearable placeholder="提单人部门" />
          <el-input v-model="filters.undertakeDepartmentName" clearable placeholder="承担部门" />
          <el-input v-model="filters.tagName" clearable placeholder="标签" />
        </div>
        <div class="expense-wb-advanced-panel__actions">
          <el-button type="primary" @click="currentPage = 1">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="expense-wb-panel expense-wb-table-shell expense-wb-table-shell--compact">
      <el-table
        :data="pagedItems"
        style="width: 100%"
        v-loading="loading"
        @header-dragend="handleHeaderDragEnd"
        @row-dblclick="handleRowDblClick"
      >
        <el-table-column
          v-for="column in visibleColumnDefinitions"
          :key="column.key"
          :column-key="column.key"
          :prop="column.key"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
          resizable
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <button
              v-if="column.key === 'documentCode'"
              class="font-medium text-blue-600 hover:underline"
              type="button"
              @click.stop="openDetail(row.documentCode)"
              @dblclick.stop
            >
              {{ resolveColumnText(row, column.key) }}
            </button>
            <span v-else-if="column.key === 'amount'">{{ formatAmount(resolveColumnText(row, column.key)) }}</span>
            <el-tag v-else-if="column.key === 'documentStatusLabel'" :type="getStatusType(resolveDocumentStatusLabel(row))">
              {{ resolveDocumentStatusLabel(row) || '-' }}
            </el-tag>
            <span v-else>{{ resolveColumnText(row, column.key) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right" :resizable="false">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row.documentCode)" @dblclick.stop>查看</el-button>
            <el-button link type="success" size="small" @click.stop="handleAction(row.taskId, 'approve')" @dblclick.stop>通过</el-button>
            <el-button link type="danger" size="small" @click.stop="handleAction(row.taskId, 'reject')" @dblclick.stop>驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="expense-wb-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredItems.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="驳回审批" width="560px">
      <div class="space-y-4">
        <el-input
          v-model="rejectForm.comment"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="请输入驳回原因"
        />
        <el-form-item v-if="rejectTargetOptions.length" label="驳回到节点" class="!mb-0">
          <el-select v-model="rejectForm.targetNodeKey" class="w-full" clearable placeholder="请选择目标审批节点">
            <el-option
              v-for="node in rejectTargetOptions"
              :key="node.nodeKey"
              :label="node.optionLabel || node.nodeName || node.nodeKey"
              :value="node.nodeKey"
            />
          </el-select>
        </el-form-item>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeRejectDialog">取消</el-button>
          <el-button type="danger" :loading="rejectSubmitting" @click="submitRejectAction">驳回</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Clock,
  Download,
  Files,
  Filter,
  Money,
  Operation,
  User
} from '@element-plus/icons-vue'
import {
  asyncTaskApi,
  expenseApi,
  expenseApprovalApi,
  type ExpenseApprovalPendingItem,
  type ExpenseDocumentDetail
} from '@/api'
import {
  EXPENSE_WORKBENCH_COLUMN_ORDER_STORAGE_KEYS,
  EXPENSE_WORKBENCH_DEFAULT_COLUMNS,
  EXPENSE_WORKBENCH_DEFAULT_COLUMN_ORDER,
  EXPENSE_WORKBENCH_STATUS_OPTIONS,
  EXPENSE_WORKBENCH_STORAGE_KEYS,
  EXPENSE_WORKBENCH_COLUMNS,
  buildColumnGridDisplayOrder,
  createExpenseWorkbenchFilters,
  filterExpenseWorkbenchRows,
  getExpenseWorkbenchStatusType,
  loadColumnOrder,
  loadColumnWidths,
  loadVisibleColumns,
  moveColumnOrder,
  resolveColumnText,
  resolveDocumentStatusLabel,
  resolveOrderedColumnDefinitions,
  resolveVisibleColumnDefinitions,
  saveColumnOrder,
  saveColumnWidth,
  saveVisibleColumns,
  sortVisibleColumnsByOrder,
  type ExpenseWorkbenchColumnKey
} from './expenseWorkbenchListHelper'
import { openDownloadCenter } from '@/utils/downloadCenter'

type RejectTargetOption = {
  nodeKey: string
  nodeName: string
  optionLabel: string
  isSubmitter?: boolean
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const exporting = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const items = ref<ExpenseApprovalPendingItem[]>([])
const filters = ref(createExpenseWorkbenchFilters())
const showAdvancedFilters = ref(false)
const rejectDialogVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectTaskId = ref<number | null>(null)
const rejectTargetOptions = ref<RejectTargetOption[]>([])
const rejectForm = ref({
  comment: '',
  targetNodeKey: ''
})

const allowedColumnKeys: ExpenseWorkbenchColumnKey[] = EXPENSE_WORKBENCH_COLUMNS.map((item) => item.key)
const columnOrder = ref<ExpenseWorkbenchColumnKey[]>(
  loadColumnOrder(
    EXPENSE_WORKBENCH_COLUMN_ORDER_STORAGE_KEYS.approval,
    EXPENSE_WORKBENCH_DEFAULT_COLUMN_ORDER,
    allowedColumnKeys
  )
)
const columnWidths = ref(loadColumnWidths(allowedColumnKeys))
const visibleColumns = ref<ExpenseWorkbenchColumnKey[]>(
  loadVisibleColumns(
    EXPENSE_WORKBENCH_STORAGE_KEYS.approval,
    EXPENSE_WORKBENCH_DEFAULT_COLUMNS.approval,
    allowedColumnKeys
  )
)
const draggingColumnKey = ref<ExpenseWorkbenchColumnKey | ''>('')
const dropTargetColumnKey = ref<ExpenseWorkbenchColumnKey | ''>('')

const statusOptions = EXPENSE_WORKBENCH_STATUS_OPTIONS.filter((item) => item !== '草稿')

void loadPending()

const filteredItems = computed(() => filterExpenseWorkbenchRows(items.value, filters.value))

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

const totalAmountText = computed(() => {
  const amount = items.value.reduce((sum, item) => sum + Number(item.amount || 0), 0)
  return formatAmount(amount)
})

const approvalStats = computed(() => [
  {
    label: '待办任务',
    value: items.value.length,
    icon: Files,
    tone: 'blue'
  },
  {
    label: '待审金额',
    value: totalAmountText.value,
    icon: Money,
    tone: 'amber'
  },
  {
    label: '提单人数',
    value: new Set(items.value.map((item) => item.submitterName).filter(Boolean)).size,
    icon: User,
    tone: 'green'
  },
  {
    label: '处理中节点',
    value: new Set(items.value.map((item) => item.nodeName).filter(Boolean)).size,
    icon: Clock,
    tone: 'rose'
  }
])

const columnOptions = computed(() => resolveOrderedColumnDefinitions(columnOrder.value))
const columnPanelItems = computed(() => buildColumnGridDisplayOrder(columnOptions.value))
const visibleColumnDefinitions = computed(() =>
  resolveVisibleColumnDefinitions(visibleColumns.value, columnOrder.value, columnWidths.value)
)

function formatAmount(value: unknown) {
  const amount = Number(value || 0)
  return `¥ ${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function getStatusType(status: string) {
  return getExpenseWorkbenchStatusType(status)
}

async function loadPending() {
  loading.value = true
  try {
    const res = await expenseApprovalApi.listPending()
    items.value = res.data || []
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载待我审批失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = createExpenseWorkbenchFilters()
  currentPage.value = 1
}

function handleVisibleColumnsChange(nextValue: ExpenseWorkbenchColumnKey[] | string[]) {
  const normalized = nextValue.filter(
    (item): item is ExpenseWorkbenchColumnKey => allowedColumnKeys.includes(item as ExpenseWorkbenchColumnKey)
  ) as ExpenseWorkbenchColumnKey[]
  if (!normalized.length) {
    ElMessage.warning('至少保留一个显示字段')
    return
  }
  visibleColumns.value = sortVisibleColumnsByOrder(normalized, columnOrder.value)
  saveVisibleColumns(EXPENSE_WORKBENCH_STORAGE_KEYS.approval, visibleColumns.value)
}

function restoreDefaultColumns() {
  visibleColumns.value = [...EXPENSE_WORKBENCH_DEFAULT_COLUMNS.approval]
  columnOrder.value = EXPENSE_WORKBENCH_DEFAULT_COLUMN_ORDER.filter((key) => allowedColumnKeys.includes(key))
  saveVisibleColumns(EXPENSE_WORKBENCH_STORAGE_KEYS.approval, visibleColumns.value)
  saveColumnOrder(EXPENSE_WORKBENCH_COLUMN_ORDER_STORAGE_KEYS.approval, columnOrder.value)
}

function handleColumnVisibilityToggle(columnKey: ExpenseWorkbenchColumnKey, checked: unknown) {
  const nextVisibleColumns = checked
    ? [...new Set([...visibleColumns.value, columnKey])]
    : visibleColumns.value.filter((key) => key !== columnKey)
  handleVisibleColumnsChange(nextVisibleColumns)
}

function handleColumnDragStart(columnKey: ExpenseWorkbenchColumnKey) {
  draggingColumnKey.value = columnKey
  dropTargetColumnKey.value = ''
}

function handleColumnDragOver(columnKey: ExpenseWorkbenchColumnKey) {
  if (!draggingColumnKey.value || draggingColumnKey.value === columnKey) {
    return
  }
  dropTargetColumnKey.value = columnKey
}

function handleColumnDrop(columnKey: ExpenseWorkbenchColumnKey) {
  if (!draggingColumnKey.value || draggingColumnKey.value === columnKey) {
    handleColumnDragEnd()
    return
  }
  columnOrder.value = moveColumnOrder(columnOrder.value, draggingColumnKey.value, columnKey)
  visibleColumns.value = sortVisibleColumnsByOrder(visibleColumns.value, columnOrder.value)
  saveColumnOrder(EXPENSE_WORKBENCH_COLUMN_ORDER_STORAGE_KEYS.approval, columnOrder.value)
  saveVisibleColumns(EXPENSE_WORKBENCH_STORAGE_KEYS.approval, visibleColumns.value)
  handleColumnDragEnd()
}

function handleColumnDragEnd() {
  draggingColumnKey.value = ''
  dropTargetColumnKey.value = ''
}

function handleHeaderDragEnd(
  newWidth: number,
  _oldWidth: number,
  column: { columnKey?: string; property?: string }
) {
  const columnKey = String(column.columnKey || column.property || '') as ExpenseWorkbenchColumnKey
  if (!allowedColumnKeys.includes(columnKey)) {
    return
  }
  columnWidths.value = saveColumnWidth(columnKey, newWidth, columnWidths.value)
}

function openDetail(documentCode: string) {
  void router.push({
    path: `/expense/documents/${encodeURIComponent(documentCode)}`,
    query: route.fullPath ? { returnTo: route.fullPath } : {}
  })
}

function handleRowDblClick(row: ExpenseApprovalPendingItem) {
  if (!row.documentCode) {
    return
  }
  openDetail(row.documentCode)
}

async function handleExport() {
  const taskIds = filteredItems.value
    .map((item) => item.taskId)
    .filter((item): item is number => typeof item === 'number')

  if (!taskIds.length) {
    ElMessage.warning('当前没有可导出的审批任务')
    return
  }

  exporting.value = true
  try {
    await asyncTaskApi.exportExpenseScene({
      scene: 'PENDING_APPROVAL',
      taskIds
    })
    ElMessage.success('导出任务已提交，请到下载中心查看进度')
    openDownloadCenter()
  } catch (error: any) {
    ElMessage.error(error.message || '提交导出任务失败')
  } finally {
    exporting.value = false
  }
}

async function handleAction(taskId: number, action: 'approve' | 'reject') {
  if (action === 'reject') {
    const item = items.value.find((entry) => entry.taskId === taskId)
    if (!item?.documentCode) {
      ElMessage.warning('未找到对应的审批任务')
      return
    }
    try {
      const detail = await expenseApi.getDetail(item.documentCode)
      rejectTargetOptions.value = resolveRejectTargetOptions(detail.data, item.nodeKey)
      rejectTaskId.value = taskId
      rejectForm.value = {
        comment: '驳回',
        targetNodeKey: rejectTargetOptions.value.length ? '__SUBMITTER__' : ''
      }
      rejectDialogVisible.value = true
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '加载驳回配置失败，请稍后重试'))
    }
    return
  }

  try {
    const { value } = await ElMessageBox.prompt(
      '可选填写审批意见',
      '通过审批',
      {
        inputType: 'textarea',
        inputPlaceholder: '请输入审批意见（可空）',
        confirmButtonText: '通过',
        cancelButtonText: '取消'
      }
    )
    await expenseApprovalApi.approve(taskId, { comment: value || '' })
    ElMessage.success('审批已通过')
    await loadPending()
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '审批通过失败'))
  }
}

function closeRejectDialog() {
  rejectDialogVisible.value = false
  rejectTaskId.value = null
  rejectTargetOptions.value = []
  rejectForm.value = {
    comment: '',
    targetNodeKey: ''
  }
}

async function submitRejectAction() {
  if (!rejectTaskId.value) {
    return
  }
  rejectSubmitting.value = true
  try {
    await expenseApprovalApi.reject(rejectTaskId.value, {
      comment: rejectForm.value.comment || '',
      ...(rejectForm.value.targetNodeKey && rejectForm.value.targetNodeKey !== '__SUBMITTER__'
        ? { targetNodeKey: rejectForm.value.targetNodeKey }
        : {})
    })
    closeRejectDialog()
    ElMessage.success('审批已驳回')
    await loadPending()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '审批驳回失败'))
  } finally {
    rejectSubmitting.value = false
  }
}

function resolveRejectTargetOptions(detail: ExpenseDocumentDetail | null | undefined, currentNodeKey: string) {
  const nodes = Array.isArray(detail?.flowSnapshot?.nodes) ? detail?.flowSnapshot?.nodes || [] : []
  const currentNode = nodes.find((node) => node.nodeKey === currentNodeKey)
  const specialSettings = Array.isArray(currentNode?.config?.specialSettings)
    ? currentNode?.config?.specialSettings || []
    : []
  if (!specialSettings.includes('REJECT_TO_ANY_NODE')) {
    return [] as RejectTargetOption[]
  }
  const upstreamApprovalNodes = (detail?.approvalNodeStatuses || [])
    .filter((item) =>
      item.nodeType === 'APPROVAL'
      && item.nodeKey !== currentNodeKey
      && item.status !== 'NOT_REACHED'
      && item.status !== 'PENDING'
      && item.status !== 'MANUAL_SELECTION_PENDING'
    )
    .map((item) => ({
      nodeKey: item.nodeKey,
      nodeName: item.nodeName || item.nodeKey,
      optionLabel: formatRejectTargetLabel(item.nodeName || item.nodeKey, item.assigneeNames)
    }))
  return [
    {
      nodeKey: '__SUBMITTER__',
      nodeName: '驳回到提单人',
      optionLabel: formatRejectTargetLabel('驳回到提单人', detail?.submitterName),
      isSubmitter: true
    },
    ...upstreamApprovalNodes
  ]
}

function formatRejectTargetLabel(nodeName: string, assigneeNames?: string[] | string) {
  const names = Array.isArray(assigneeNames)
    ? assigneeNames.filter((item) => Boolean(String(item || '').trim()))
    : [String(assigneeNames || '').trim()].filter(Boolean)
  if (!names.length) {
    return nodeName
  }
  return `${nodeName}（${names.join('、')}）`
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>
