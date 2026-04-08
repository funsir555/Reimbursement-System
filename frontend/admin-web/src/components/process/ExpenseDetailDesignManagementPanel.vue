<template>
  <div class="expense-wb-page expense-wb-page--config space-y-6">
    <section class="expense-wb-stat-grid expense-wb-stat-grid--compact" data-testid="expense-detail-summary-grid">
      <article
        v-for="stat in detailDesignStats"
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

    <el-card class="expense-wb-toolbar">
      <div class="expense-wb-toolbar__row">
        <div class="expense-wb-toolbar__heading">
          <p class="expense-wb-toolbar__title">筛选与搜索</p>
          <p class="expense-wb-toolbar__desc">按表单名称、编码和明细类型快速定位设计稿，保留现有创建、编辑、删除链路。</p>
        </div>

        <div class="expense-wb-toolbar__group">
          <el-button
            type="primary"
            :icon="Plus"
            data-testid="expense-detail-toolbar-create"
            @click="goCreate"
          >
            新建费用明细表单
          </el-button>
          <el-input
            v-model="keyword"
            clearable
            :prefix-icon="Search"
            placeholder="搜索明细表单名称或编码"
            class="w-full lg:max-w-[360px]"
          />

          <el-segmented v-model="typeFilter" :options="typeOptions" />
        </div>
      </div>
    </el-card>

    <div class="expense-wb-choice-grid">
      <article
        v-for="item in filteredItems"
        :key="item.id"
        class="expense-wb-choice-card"
      >
        <div class="expense-wb-choice-card__header">
          <div class="min-w-0">
            <p class="expense-wb-choice-card__title">{{ item.detailName }}</p>
            <p class="expense-wb-choice-card__subtitle">{{ item.detailCode }}</p>
          </div>
          <el-tag effect="plain">{{ resolveDetailTypeLabel(item.detailType, item.detailTypeLabel) }}</el-tag>
        </div>

        <div class="expense-wb-choice-card__body">
          {{ item.detailDescription || '暂无说明' }}
        </div>

        <div class="expense-wb-choice-card__meta">
          <span class="expense-wb-soft-badge">最近更新 {{ item.updatedAt || '-' }}</span>
          <span class="expense-wb-soft-badge expense-wb-soft-badge--success">可绑定模板</span>
        </div>

        <div class="expense-wb-choice-card__footer">
          <el-button text type="danger" @click="removeItem(item)">删除</el-button>
          <el-button type="primary" text @click="goEdit(item.id)">编辑</el-button>
          <el-button text data-testid="expense-detail-copy-button" @click="goCopy(item.id)">复制模板</el-button>
        </div>
      </article>
    </div>

    <div v-if="!loading && filteredItems.length === 0" class="expense-wb-empty-card">
      <el-empty description="暂无费用明细表单" :image-size="96">
        <el-button type="primary" @click="goCreate">新建费用明细表单</el-button>
      </el-empty>
    </div>
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
