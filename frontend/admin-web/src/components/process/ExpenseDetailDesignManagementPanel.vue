<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4" data-testid="expense-detail-summary-grid">
      <el-card
        v-for="stat in detailDesignStats"
        :key="stat.label"
        class="stat-card stat-card--compact !rounded-3xl !shadow-sm"
      >
        <div class="stat-card__content">
          <div>
            <p class="text-sm text-slate-500">{{ stat.label }}</p>
            <p class="mt-1.5 text-3xl font-bold leading-none text-slate-800">{{ stat.value }}</p>
          </div>
          <span class="stat-card__icon" :class="`stat-card__icon--${stat.tone}`">
            <el-icon :size="22">
              <component :is="stat.icon" />
            </el-icon>
          </span>
        </div>
      </el-card>
    </section>

    <el-card class="!rounded-3xl !shadow-sm">
      <div class="flex flex-col justify-between gap-4 xl:flex-row xl:items-center">
        <div>
          <p class="text-lg font-semibold text-slate-800">费用明细表单</p>
          <p class="mt-1 text-sm text-slate-400">按表单名称、编码和明细类型快速定位设计稿，延续单据与流程的卡片风格与修改入口。</p>
        </div>

        <div class="flex w-full flex-col gap-3 sm:flex-row xl:w-auto">
          <el-button
            type="primary"
            :icon="Plus"
            data-testid="expense-detail-toolbar-create"
            @click="goCreate"
          >
            增加费用明细表单
          </el-button>
          <el-input
            v-model="keyword"
            clearable
            :prefix-icon="Search"
            placeholder="搜索明细表单名称或编码"
            class="w-full sm:w-80"
          />

          <el-segmented v-model="typeFilter" :options="typeOptions" />
        </div>
      </div>
    </el-card>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      <el-card
        v-for="item in filteredItems"
        :key="item.id"
        class="template-card resource-card !rounded-3xl !shadow-sm"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="truncate text-base font-semibold text-slate-800">{{ item.detailName }}</p>
            <p class="mt-1 truncate text-xs text-slate-400">{{ item.detailCode }}</p>
          </div>
          <el-tag size="small" type="primary" effect="plain">{{ resolveDetailTypeLabel(item.detailType, item.detailTypeLabel) }}</el-tag>
        </div>

        <p class="mt-3 min-h-[40px] text-sm leading-5 text-slate-500">{{ item.detailDescription || '暂无说明' }}</p>

        <div class="mt-4 space-y-2 rounded-2xl bg-slate-50 px-3.5 py-3">
          <div class="flex items-center justify-between text-sm">
            <span class="text-slate-400">明细类型</span>
            <span class="font-medium text-slate-700">{{ resolveDetailTypeLabel(item.detailType, item.detailTypeLabel) }}</span>
          </div>
          <div class="flex items-center justify-between text-sm">
            <span class="text-slate-400">更新时间</span>
            <span class="text-slate-700">{{ item.updatedAt || '-' }}</span>
          </div>
        </div>

        <div class="mt-4 flex flex-wrap justify-end gap-2 process-card-footer expense-detail-design-card__footer" data-testid="expense-detail-card-footer">
          <el-button text type="danger" @click="removeItem(item)">删除</el-button>
          <el-button type="primary" text @click="goEdit(item.id)">编辑</el-button>
          <el-button
            text
            class="expense-detail-design-card__copy-button"
            data-testid="expense-detail-copy-button"
            @click="goCopy(item.id)"
          >
            复制模板
          </el-button>
        </div>
      </el-card>
    </div>

    <el-card v-if="!loading && filteredItems.length === 0" class="!rounded-3xl !shadow-sm">
      <div class="flex min-h-[220px] items-center justify-center text-sm text-slate-400">
        <el-empty :description="items.length ? '暂无匹配费用明细表单' : '暂无费用明细表单'" :image-size="96">
          <el-button type="primary" @click="goCreate">增加费用明细表单</el-button>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  Document,
  Files,
  Plus,
  Search
} from '@element-plus/icons-vue'
import { processApi, type ProcessExpenseDetailDesignSummary } from '@/api'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const typeFilter = ref('all')
const items = ref<ProcessExpenseDetailDesignSummary[]>([])

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '普通报销', value: 'NORMAL_REIMBURSEMENT' },
  { label: '企业往来', value: 'ENTERPRISE_TRANSACTION' }
]

const filteredItems = computed(() => {
  const text = keyword.value.trim()
  return items.value.filter((item) => {
    const matchesKeyword = !text || item.detailName.includes(text) || item.detailCode.includes(text)
    const matchesType = typeFilter.value === 'all' || item.detailType === typeFilter.value
    return matchesKeyword && matchesType
  })
})

const detailDesignStats = computed(() => [
  {
    label: '全部设计',
    value: items.value.length,
    hint: '当前可用于模板绑定的费用明细设计总数',
    icon: Files,
    tone: 'blue'
  },
  {
    label: '普通报销',
    value: items.value.filter((item) => item.detailType === 'NORMAL_REIMBURSEMENT').length,
    hint: '适用于常规报销场景的明细设计',
    icon: Document,
    tone: 'amber'
  },
  {
    label: '企业往来',
    value: items.value.filter((item) => item.detailType === 'ENTERPRISE_TRANSACTION').length,
    hint: '适用于到票支付与预付未到票场景',
    icon: CircleCheckFilled,
    tone: 'green'
  },
  {
    label: '当前筛选',
    value: filteredItems.value.length,
    hint: '经过关键字和类型过滤后的结果数量',
    icon: Search,
    tone: 'rose'
  }
])

onMounted(() => {
  void loadItems()
})

async function loadItems() {
  loading.value = true
  try {
    const res = await processApi.listExpenseDetailDesigns()
    items.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载费用明细表单失败'))
  } finally {
    loading.value = false
  }
}

function resolveDetailTypeLabel(detailType?: string, fallback?: string) {
  if (detailType === 'ENTERPRISE_TRANSACTION') return '企业往来'
  if (detailType === 'NORMAL_REIMBURSEMENT') return '普通报销'
  return fallback || '费用明细'
}

function goCreate() {
  void router.push({ name: 'expense-workbench-process-expense-detail-create' })
}

function goEdit(id: number) {
  void router.push({ name: 'expense-workbench-process-expense-detail-edit', params: { id } })
}

function goCopy(id: number) {
  void router.push({
    name: 'expense-workbench-process-expense-detail-create',
    query: { copyFromId: String(id) }
  })
}

async function removeItem(item: ProcessExpenseDetailDesignSummary) {
  try {
    await ElMessageBox.confirm(
      `确认删除“${item.detailName}”吗？删除后将不能再被模板绑定，已绑定模板需要重新选择。`,
      '删除费用明细表单',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    await processApi.deleteExpenseDetailDesign(item.id)
    ElMessage.success('费用明细表单已删除')
    await loadItems()
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '删除费用明细表单失败'))
  }
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<style scoped>
.stat-card {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.95) !important;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.96) 100%);
}

.stat-card::after {
  content: '';
  position: absolute;
  inset: auto 12px 0;
  height: 46px;
  border-radius: 999px 999px 0 0;
  background: linear-gradient(180deg, rgba(191, 219, 254, 0) 0%, rgba(191, 219, 254, 0.32) 100%);
  pointer-events: none;
}

.stat-card--compact {
  min-height: 108px;
}

:deep(.stat-card .el-card__body) {
  position: relative;
  z-index: 1;
  padding: 18px 20px;
}

.stat-card__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.stat-card__icon {
  display: flex;
  height: 44px;
  width: 44px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #fff;
}

.stat-card__icon--blue {
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
}

.stat-card__icon--amber {
  background: linear-gradient(135deg, #ea580c 0%, #fdba74 100%);
}

.stat-card__icon--green {
  background: linear-gradient(135deg, #0f766e 0%, #2dd4bf 100%);
}

.stat-card__icon--rose {
  background: linear-gradient(135deg, #e11d48 0%, #fb7185 100%);
}

.template-card {
  border: 1px solid #e2e8f0 !important;
}

:deep(.resource-card .el-card__body) {
  padding: 18px;
}

.resource-card {
  border: 1px solid #e2e8f0 !important;
}

.expense-detail-design-card__footer {
  justify-content: flex-end;
  flex-wrap: nowrap;
}

:deep(.expense-detail-design-card__copy-button.el-button) {
  color: #1f2937;
}

:deep(.expense-detail-design-card__copy-button.el-button:hover),
:deep(.expense-detail-design-card__copy-button.el-button:focus-visible) {
  color: #111827;
}
</style>
