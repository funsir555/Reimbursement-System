<template>
  <div class="expense-wb-page expense-wb-page--list space-y-6">
    <section class="expense-wb-stat-grid expense-wb-stat-grid--compact">
      <article
        v-for="item in statusCards"
        :key="item.tab"
        class="expense-wb-stat-card expense-wb-stat-card--compact cursor-pointer"
        :class="{ 'ring-2 ring-blue-500/60': activeTab === item.tab }"
        @click="switchTab(item.tab)"
      >
        <div class="expense-wb-stat-card__top">
          <div>
            <p class="expense-wb-stat-card__label">{{ item.label }}</p>
            <p class="expense-wb-stat-card__value">{{ item.count }}</p>
          </div>
          <el-tag effect="plain">{{ item.statusCode }}</el-tag>
        </div>
      </article>
    </section>

    <el-card class="expense-wb-toolbar expense-wb-toolbar--compact">
      <div class="expense-wb-toolbar__row expense-wb-toolbar__row--compact expense-wb-toolbar__main">
        <div class="expense-wb-toolbar__heading expense-wb-toolbar__heading--compact expense-wb-toolbar__heading--inline">
          <p class="expense-wb-toolbar__title">付款单工作台</p>
          <div class="expense-wb-toolbar__meta">
            <span class="expense-wb-soft-badge">当前状态 {{ currentRows.length }}</span>
            <span class="expense-wb-soft-badge expense-wb-soft-badge--success">筛选结果 {{ filteredRows.length }}</span>
          </div>
        </div>

        <div class="expense-wb-toolbar__group">
          <el-input v-model="keyword" clearable placeholder="搜索单据编号、标题、提单人" class="w-72" />
          <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="expense-wb-panel expense-wb-table-shell expense-wb-table-shell--compact">
      <el-table :data="pagedRows" v-loading="loading" style="width: 100%">
        <el-table-column prop="documentCode" label="单据编号" min-width="150">
          <template #default="{ row }">
            <button class="cursor-pointer font-medium text-blue-600 hover:underline" type="button" @click="openDetail(row)">
              {{ row.documentCode }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="documentTitle" label="单据名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="templateTypeLabel" label="单据类型" min-width="100" />
        <el-table-column prop="submitterName" label="提单人" min-width="100" />
        <el-table-column prop="paymentCompanyName" label="付款公司" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">{{ formatMoneyText(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentStatusLabel" label="付款状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.paymentStatusCode || row.documentStatus)">{{ row.paymentStatusLabel || row.documentStatusLabel || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiptStatusLabel" label="回单状态" min-width="110" />
        <el-table-column prop="companyBankAccountName" label="直连账户" min-width="160" show-overflow-tooltip />
        <el-table-column prop="bankFlowNo" label="银行流水号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="paidAt" label="支付时间" min-width="160" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <div class="flex flex-wrap items-center gap-2">
              <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="row.paymentStatusCode === 'PENDING_PAYMENT' || row.paymentStatusCode === 'PAYMENT_EXCEPTION'"
                link
                type="primary"
                size="small"
                @click="startPayment(row)"
              >
                发起支付
              </el-button>
              <el-button
                v-if="row.paymentStatusCode === 'PENDING_PAYMENT' || row.paymentStatusCode === 'PAYING'"
                link
                type="success"
                size="small"
                @click="completePayment(row)"
              >
                手动已支付
              </el-button>
              <el-button
                v-if="row.paymentStatusCode === 'PENDING_PAYMENT' || row.paymentStatusCode === 'PAYING'"
                link
                type="danger"
                size="small"
                @click="markException(row)"
              >
                标记异常
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="expense-wb-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredRows.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { expensePaymentApi, type ExpensePaymentOrder, type MoneyValue } from '@/api'
import { formatMoney } from '@/utils/money'

type PaymentTab = 'pending' | 'paying' | 'paid' | 'finished' | 'exception'

const TAB_STATUS_MAP: Record<PaymentTab, string> = {
  pending: 'PENDING_PAYMENT',
  paying: 'PAYING',
  paid: 'PAYMENT_COMPLETED',
  finished: 'PAYMENT_FINISHED',
  exception: 'PAYMENT_EXCEPTION'
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const rowsByTab = ref<Record<PaymentTab, ExpensePaymentOrder[]>>({
  pending: [],
  paying: [],
  paid: [],
  finished: [],
  exception: []
})

const activeTab = computed<PaymentTab>(() => {
  const tab = String(route.query.tab || 'pending')
  return ['pending', 'paying', 'paid', 'finished', 'exception'].includes(tab) ? (tab as PaymentTab) : 'pending'
})

const currentRows = computed(() => rowsByTab.value[activeTab.value] || [])
const filteredRows = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  if (!normalized) {
    return currentRows.value
  }
  return currentRows.value.filter((item) => (
    [item.documentCode, item.documentTitle, item.submitterName, item.paymentCompanyName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized))
  ))
})
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const statusCards = computed(() => [
  { tab: 'pending' as const, label: '待支付', count: rowsByTab.value.pending.length, statusCode: 'PENDING_PAYMENT' },
  { tab: 'paying' as const, label: '支付中', count: rowsByTab.value.paying.length, statusCode: 'PAYING' },
  { tab: 'paid' as const, label: '已支付', count: rowsByTab.value.paid.length, statusCode: 'PAYMENT_COMPLETED' },
  { tab: 'finished' as const, label: '已完成', count: rowsByTab.value.finished.length, statusCode: 'PAYMENT_FINISHED' },
  { tab: 'exception' as const, label: '支付异常', count: rowsByTab.value.exception.length, statusCode: 'PAYMENT_EXCEPTION' }
])

watch(
  () => route.query.tab,
  () => {
    currentPage.value = 1
  },
  { immediate: true }
)

void reloadAll()

function switchTab(tab: PaymentTab) {
  void router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab
    }
  })
}

async function reloadAll() {
  loading.value = true
  try {
    const entries = await Promise.all(
      Object.entries(TAB_STATUS_MAP).map(async ([tab, status]) => {
        const res = await expensePaymentApi.listOrders(status)
        return [tab, res.data || []] as const
      })
    )
    rowsByTab.value = Object.fromEntries(entries) as Record<PaymentTab, ExpensePaymentOrder[]>
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载付款单失败'))
  } finally {
    loading.value = false
  }
}

function openDetail(row: ExpensePaymentOrder) {
  void router.push({
    name: 'expense-document-detail',
    params: {
      documentCode: row.documentCode
    }
  })
}

async function startPayment(row: ExpensePaymentOrder) {
  try {
    await ElMessageBox.confirm(`确认将单据 ${row.documentCode} 推送到招商银行云直连？`, '发起支付', {
      confirmButtonText: '确认推送',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await expensePaymentApi.startTask(row.taskId)
    ElMessage.success('付款任务已推送至银行')
    await reloadAll()
  } catch (error: unknown) {
    if (isCancel(error)) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '发起支付失败'))
  }
}

async function completePayment(row: ExpensePaymentOrder) {
  try {
    const { value } = await ElMessageBox.prompt('可选填写备注，系统会将该单据标记为手动已支付，并跳过自动回单查询。', '手动标记已支付', {
      inputType: 'textarea',
      inputPlaceholder: '请输入备注（可空）',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    await expensePaymentApi.completeTask(row.taskId, { comment: value || '' })
    ElMessage.success('付款任务已标记为已支付')
    await reloadAll()
  } catch (error: unknown) {
    if (isCancel(error)) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '手动标记已支付失败'))
  }
}

async function markException(row: ExpensePaymentOrder) {
  try {
    const { value } = await ElMessageBox.prompt('请输入异常说明。', '标记支付异常', {
      inputType: 'textarea',
      inputPlaceholder: '请输入异常原因',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    await expensePaymentApi.markException(row.taskId, { comment: value || '' })
    ElMessage.success('付款任务已标记异常')
    await reloadAll()
  } catch (error: unknown) {
    if (isCancel(error)) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '标记支付异常失败'))
  }
}

function formatMoneyText(value: MoneyValue | undefined) {
  return `¥ ${formatMoney(value || 0)}`
}

function statusTagType(status?: string) {
  switch (status) {
    case 'PAYING':
      return 'warning'
    case 'PAYMENT_COMPLETED':
      return 'success'
    case 'PAYMENT_FINISHED':
      return 'primary'
    case 'PAYMENT_EXCEPTION':
      return 'danger'
    default:
      return 'info'
  }
}

function isCancel(error: unknown) {
  return error === 'cancel' || String(error).includes('cancel')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error && typeof error === 'object' && 'message' in error && typeof (error as { message?: unknown }).message === 'string') {
    return (error as { message: string }).message
  }
  return fallback
}
</script>
