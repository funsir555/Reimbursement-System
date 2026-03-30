<template>
  <div class="space-y-6">
    <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h1 class="text-3xl font-bold text-slate-800">待我审批</h1>
          <p class="mt-3 text-sm leading-7 text-slate-500">处理当前分配给你的审批任务，可直接进入单据详情查看表单与真实轨迹。</p>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <el-button @click="loadPending">刷新</el-button>
          <el-button @click="router.push('/expense/list')">返回我的报销</el-button>
        </div>
      </div>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="flex flex-wrap gap-4">
        <el-input v-model="keyword" placeholder="搜索单号、标题或提单人" class="w-72" clearable />
        <el-button type="primary" @click="currentPage = 1">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <el-table :data="pagedItems" style="width: 100%" v-loading="loading">
        <el-table-column prop="documentCode" label="单号" min-width="160">
          <template #default="{ row }">
            <button class="font-medium text-blue-600 hover:underline" type="button" @click="openDetail(row.documentCode)">
              {{ row.documentCode }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="documentTitle" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="submitterName" label="提单人" width="120" />
        <el-table-column prop="nodeName" label="当前节点" width="150" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">¥ {{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row.documentCode)">查看</el-button>
            <el-button link type="success" size="small" @click="handleAction(row.taskId, 'approve')">通过</el-button>
            <el-button link type="danger" size="small" @click="handleAction(row.taskId, 'reject')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-6 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredItems.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { expenseApprovalApi, type ExpenseApprovalPendingItem } from '@/api'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const items = ref<ExpenseApprovalPendingItem[]>([])

void loadPending()

const filteredItems = computed(() => {
  const search = keyword.value.trim()
  if (!search) {
    return items.value
  }
  return items.value.filter((item) =>
    [item.documentCode, item.documentTitle, item.submitterName, item.documentReason]
      .filter(Boolean)
      .some((field) => String(field).includes(search))
  )
})

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

async function loadPending() {
  loading.value = true
  try {
    const res = await expenseApprovalApi.listPending()
    items.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载待我审批失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  currentPage.value = 1
}

function openDetail(documentCode: string) {
  void router.push(`/expense/documents/${encodeURIComponent(documentCode)}`)
}

async function handleAction(taskId: number, action: 'approve' | 'reject') {
  try {
    const { value } = await ElMessageBox.prompt(
      action === 'approve' ? '可选填写审批意见' : '请填写驳回原因',
      action === 'approve' ? '通过审批' : '驳回审批',
      {
        inputType: 'textarea',
        inputPlaceholder: action === 'approve' ? '请输入审批意见（可空）' : '请输入驳回原因',
        confirmButtonText: action === 'approve' ? '通过' : '驳回',
        cancelButtonText: '取消'
      }
    )
    const api = action === 'approve' ? expenseApprovalApi.approve : expenseApprovalApi.reject
    await api(taskId, { comment: value || '' })
    ElMessage.success(action === 'approve' ? '审批已通过' : '审批已驳回')
    await loadPending()
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, action === 'approve' ? '审批通过失败' : '审批驳回失败'))
  }
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>
