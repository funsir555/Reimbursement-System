<template>
  <div class="space-y-6">
    <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="space-y-5">
        <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回我的报销
        </button>

        <div class="pt-1">
          <div class="flex flex-wrap items-center gap-3">
            <h1 class="text-3xl font-bold text-slate-800">{{ detail?.documentTitle || route.params.documentCode }}</h1>
            <el-tag :type="statusTagType(detail?.status || '')" effect="plain">{{ detail?.statusLabel || '未知' }}</el-tag>
          </div>
          <p class="mt-3 text-sm leading-7 text-slate-500">
            单号：{{ detail?.documentCode || route.params.documentCode }} ｜ 提单人：{{ detail?.submitterName || '-' }} ｜ 提交时间：{{ detail?.submittedAt || '-' }}
          </p>
        </div>
      </div>
    </section>

    <div v-loading="loading" class="detail-layout grid grid-cols-1 gap-6 xl:grid-cols-[3fr_1fr]">
      <el-card class="!rounded-3xl !shadow-sm">
        <template #header>
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-lg font-semibold text-slate-800">单据表单</p>
              <p class="mt-1 text-sm text-slate-500">根据提交时保存的表单快照回看单据内容。</p>
            </div>
            <el-tag effect="plain">金额：{{ amountText }}</el-tag>
          </div>
        </template>

        <div class="form-scroll">
          <ExpenseFormReadonlyRenderer
            v-if="detail"
            :schema="detail.formSchemaSnapshot"
            :form-data="detail.formData"
            :department-options="detail.departmentOptions"
          />
          <el-empty v-else description="暂无单据数据" :image-size="96" />
        </div>
      </el-card>

      <el-card class="!rounded-3xl !shadow-sm">
        <template #header>
          <div>
            <p class="text-lg font-semibold text-slate-800">审批流程</p>
            <p class="mt-1 text-sm text-slate-500">真实任务状态与审批轨迹</p>
          </div>
        </template>

        <div class="approval-scroll space-y-5">
          <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-4">
            <div class="grid grid-cols-1 gap-3 text-sm text-slate-600">
              <div>当前节点：{{ detail?.currentNodeName || '未开始' }}</div>
              <div>模板名称：{{ detail?.templateName || '-' }}</div>
              <div>当前状态：{{ detail?.statusLabel || '-' }}</div>
            </div>
          </div>

          <div v-if="approvableTasks.length" class="rounded-[24px] border border-amber-200 bg-amber-50 p-4">
            <p class="text-sm font-semibold text-amber-800">待我审批</p>
            <p class="mt-1 text-xs leading-6 text-amber-700">当前有 {{ approvableTasks.length }} 条待处理任务</p>
            <div class="mt-3 flex flex-wrap gap-2">
              <el-button size="small" type="success" @click="handleTaskAction('approve')">通过</el-button>
              <el-button size="small" type="danger" @click="handleTaskAction('reject')">驳回</el-button>
            </div>
          </div>

          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-slate-800">流程概览</p>
              <el-tag size="small" effect="plain">{{ flowOutline.length }} 个节点</el-tag>
            </div>
            <div v-if="flowOutline.length" class="space-y-2">
              <div
                v-for="item in flowOutline"
                :key="item.key"
                class="rounded-2xl border px-3 py-3"
                :class="outlineClass(item.status)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="text-sm font-semibold text-slate-800">{{ item.label }}</p>
                    <p v-if="item.description" class="mt-1 text-xs leading-5 text-slate-500">{{ item.description }}</p>
                  </div>
                  <el-tag size="small" :type="outlineTagType(item.status)" effect="plain">{{ outlineStatusText(item.status) }}</el-tag>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无流程节点" :image-size="72" />
          </div>

          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-slate-800">审批轨迹</p>
              <el-tag size="small" effect="plain">{{ detail?.actionLogs.length || 0 }} 条</el-tag>
            </div>

            <el-timeline v-if="detail?.actionLogs.length">
              <el-timeline-item
                v-for="log in detail?.actionLogs"
                :key="log.id"
                :timestamp="log.createdAt"
                placement="top"
              >
                <div class="space-y-1">
                  <p class="text-sm font-semibold text-slate-800">{{ timelineTitle(log) }}</p>
                  <p v-if="timelineDescription(log)" class="text-xs leading-6 text-slate-500">{{ timelineDescription(log) }}</p>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无审批轨迹" :image-size="72" />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { expenseApi, expenseApprovalApi, type ExpenseApprovalLog, type ExpenseApprovalTask, type ExpenseDocumentDetail, type ProcessFlowNode, type ProcessFlowRoute } from '@/api'
import ExpenseFormReadonlyRenderer from './components/ExpenseFormReadonlyRenderer.vue'
import { hasPermission, readStoredUser } from '@/utils/permissions'

type FlowOutlineItem = {
  key: string
  label: string
  description: string
  status: 'completed' | 'current' | 'rejected' | 'exception' | 'matched' | 'pending'
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<ExpenseDocumentDetail | null>(null)
const storedUser = (readStoredUser() || {}) as { userId?: number; permissionCodes?: string[] }

const amountText = computed(() => `¥ ${(detail.value?.totalAmount || 0).toFixed(2)}`)
const currentUserId = computed(() => Number(storedUser.userId || 0))
const permissionCodes = computed(() => storedUser.permissionCodes || [])
const approvableTasks = computed(() =>
  (detail.value?.currentTasks || []).filter((task) => task.assigneeUserId === currentUserId.value)
)

const flowOutline = computed(() => {
  if (!detail.value?.flowSnapshot) {
    return []
  }
  const nodes = Array.isArray(detail.value.flowSnapshot.nodes) ? detail.value.flowSnapshot.nodes : []
  const routes = Array.isArray(detail.value.flowSnapshot.routes) ? detail.value.flowSnapshot.routes : []
  const logs = detail.value.actionLogs || []
  const pendingNodeKeys = new Set((detail.value.currentTasks || []).map((task) => task.nodeKey))
  const approvedNodeKeys = new Set(logs.filter((log) => ['APPROVE', 'AUTO_SKIP', 'CC_REACHED', 'PAYMENT_REACHED'].includes(log.actionType)).map((log) => log.nodeKey || ''))
  const rejectedNodeKeys = new Set(logs.filter((log) => log.actionType === 'REJECT').map((log) => log.nodeKey || ''))
  const exceptionNodeKeys = new Set(logs.filter((log) => log.actionType === 'EXCEPTION').map((log) => log.nodeKey || ''))
  const hitRouteKeys = new Set(
    logs
      .filter((log) => log.actionType === 'ROUTE_HIT')
      .map((log) => String(log.payload?.routeKey || ''))
      .filter(Boolean)
  )
  return buildFlowOutline(nodes, routes, null, pendingNodeKeys, approvedNodeKeys, rejectedNodeKeys, exceptionNodeKeys, hitRouteKeys)
})

void loadDetail()

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push('/expense/list')
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await expenseApi.getDetail(String(route.params.documentCode || ''))
    detail.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载单据详情失败'))
  } finally {
    loading.value = false
  }
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    APPROVED: 'success',
    REJECTED: 'danger',
    EXCEPTION: 'warning',
    PENDING_APPROVAL: 'warning'
  }
  return map[status] || 'info'
}

function outlineClass(status: string) {
  const map: Record<string, string> = {
    completed: 'border-emerald-200 bg-emerald-50',
    current: 'border-amber-200 bg-amber-50',
    rejected: 'border-rose-200 bg-rose-50',
    exception: 'border-orange-200 bg-orange-50',
    matched: 'border-sky-200 bg-sky-50',
    pending: 'border-slate-200 bg-white'
  }
  return map[status] || map.pending
}

function outlineTagType(status: string) {
  const map: Record<string, string> = {
    completed: 'success',
    current: 'warning',
    rejected: 'danger',
    exception: 'warning',
    matched: 'primary',
    pending: 'info'
  }
  return map[status] || 'info'
}

function outlineStatusText(status: string) {
  const map: Record<string, string> = {
    completed: '\u5df2\u5b8c\u6210',
    current: '\u8fdb\u884c\u4e2d',
    rejected: '\u5df2\u9a73\u56de',
    exception: '\u5f02\u5e38',
    matched: '\u5df2\u547d\u4e2d',
    pending: '\u672a\u5f00\u59cb'
  }
  return map[status] || '\u672a\u5f00\u59cb'
}
function timelineTitle(log: ExpenseApprovalLog) {
  const actorName = log.actorName || '\u5ba1\u6279\u4eba'
  const actionMap: Record<string, string> = {
    SUBMIT: '\u63d0\u4ea4\u5355\u636e',
    ROUTE_HIT: ('\u547d\u4e2d\u5206\u652f ' + String(log.payload?.routeName || '')).trim(),
    APPROVAL_PENDING: ('\u5230\u8fbe\u5ba1\u6279\u8282\u70b9 ' + (log.nodeName || '')).trim(),
    APPROVE: actorName + ' \u901a\u8fc7',
    REJECT: actorName + ' \u9a73\u56de',
    AUTO_SKIP: ('\u81ea\u52a8\u8df3\u8fc7 ' + (log.nodeName || '')).trim(),
    CC_REACHED: ('\u5230\u8fbe\u6284\u9001\u8282\u70b9 ' + (log.nodeName || '')).trim(),
    PAYMENT_REACHED: ('\u5230\u8fbe\u652f\u4ed8\u8282\u70b9 ' + (log.nodeName || '')).trim(),
    FINISH: '\u5ba1\u6279\u5b8c\u6210',
    EXCEPTION: '\u6d41\u7a0b\u5f02\u5e38'
  }
  return actionMap[log.actionType] || log.actionType
}
function timelineDescription(log: ExpenseApprovalLog) {
  const parts = [log.actionComment]
  if (log.nodeName && !['APPROVE', 'REJECT', 'APPROVAL_PENDING'].includes(log.actionType)) {
    parts.unshift(log.nodeName)
  }
  return parts.filter(Boolean).join(' / ')
}
async function handleTaskAction(action: 'approve' | 'reject') {
  if (!detail.value || !approvableTasks.value.length) {
    return
  }
  const permissionCode = action === 'approve' ? 'expense:approval:approve' : 'expense:approval:reject'
  if (!hasPermission(permissionCode, permissionCodes.value)) {
    ElMessage.warning('\u5f53\u524d\u8d26\u53f7\u6ca1\u6709\u5904\u7406\u8be5\u5ba1\u6279\u7684\u6743\u9650')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      action === 'approve' ? '\u53ef\u9009\u586b\u5199\u5ba1\u6279\u610f\u89c1' : '\u8bf7\u586b\u5199\u9a73\u56de\u539f\u56e0',
      action === 'approve' ? '\u901a\u8fc7\u5ba1\u6279' : '\u9a73\u56de\u5ba1\u6279',
      {
        inputType: 'textarea',
        inputPlaceholder: action === 'approve' ? '\u8bf7\u8f93\u5165\u5ba1\u6279\u610f\u89c1\uff08\u53ef\u7a7a\uff09' : '\u8bf7\u8f93\u5165\u9a73\u56de\u539f\u56e0',
        confirmButtonText: action === 'approve' ? '\u901a\u8fc7' : '\u9a73\u56de',
        cancelButtonText: '\u53d6\u6d88'
      }
    )
    const task = approvableTasks.value[0]
    if (!task) {
      return
    }
    const api = action === 'approve' ? expenseApprovalApi.approve : expenseApprovalApi.reject
    const res = await api(task.id, { comment: value || '' })
    detail.value = res.data
    ElMessage.success(action === 'approve' ? '\u5ba1\u6279\u5df2\u901a\u8fc7' : '\u5ba1\u6279\u5df2\u9a73\u56de')
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, action === 'approve' ? '\u5ba1\u6279\u901a\u8fc7\u5931\u8d25' : '\u5ba1\u6279\u9a73\u56de\u5931\u8d25'))
  }
}
function buildFlowOutline(
  nodes: ProcessFlowNode[],
  routes: ProcessFlowRoute[],
  containerKey: string | null,
  pendingNodeKeys: Set<string>,
  approvedNodeKeys: Set<string>,
  rejectedNodeKeys: Set<string>,
  exceptionNodeKeys: Set<string>,
  hitRouteKeys: Set<string>
): FlowOutlineItem[] {
  const children = nodes
    .filter((item) => String(item.parentNodeKey || '') === String(containerKey || ''))
    .sort((left, right) => left.displayOrder - right.displayOrder)

  return children.flatMap((node) => {
    const status: FlowOutlineItem['status'] = exceptionNodeKeys.has(node.nodeKey)
      ? 'exception'
      : rejectedNodeKeys.has(node.nodeKey)
        ? 'rejected'
        : pendingNodeKeys.has(node.nodeKey)
          ? 'current'
          : approvedNodeKeys.has(node.nodeKey)
            ? 'completed'
            : 'pending'

    const baseItem: FlowOutlineItem = {
      key: node.nodeKey,
      label: node.nodeName,
      description: node.nodeType === 'BRANCH' ? '分支判断节点' : '',
      status
    }

    if (node.nodeType !== 'BRANCH') {
      return [baseItem]
    }

    const routeItems: FlowOutlineItem[] = routes
      .filter((route) => route.sourceNodeKey === node.nodeKey)
      .sort((left, right) => left.priority - right.priority)
      .flatMap((route) => {
        const routeStatus: FlowOutlineItem['status'] = hitRouteKeys.has(route.routeKey) ? 'matched' : 'pending'
        const childrenItems: FlowOutlineItem[] = buildFlowOutline(
          nodes,
          routes,
          route.routeKey,
          pendingNodeKeys,
          approvedNodeKeys,
          rejectedNodeKeys,
          exceptionNodeKeys,
          hitRouteKeys
        )
        return [
          {
            key: route.routeKey,
            label: route.routeName,
            description: '分支泳道',
            status: routeStatus
          },
          ...childrenItems
        ]
      })

    return [baseItem, ...routeItems]
  })
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<style scoped>
.form-scroll,
.approval-scroll {
  max-height: calc(100vh - 240px);
  overflow-y: auto;
  padding-right: 6px;
}

.form-scroll::-webkit-scrollbar,
.approval-scroll::-webkit-scrollbar {
  width: 8px;
}

.form-scroll::-webkit-scrollbar-thumb,
.approval-scroll::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.7);
  border-radius: 999px;
}

.form-scroll::-webkit-scrollbar-track,
.approval-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.7);
  border-radius: 999px;
}

@media (max-width: 1279px) {
  .form-scroll,
  .approval-scroll {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
}
</style>


