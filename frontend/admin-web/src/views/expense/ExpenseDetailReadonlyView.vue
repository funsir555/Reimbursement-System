<template>
  <div class="space-y-6">
    <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="space-y-5">
        <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回单据详情
        </button>

        <div class="pt-1">
          <div class="flex flex-wrap items-center gap-3">
            <h1 class="text-3xl font-bold text-slate-800">{{ detail?.detailTitle || detailNo }}</h1>
            <el-tag effect="plain">{{ detail?.detailTypeLabel || '费用明细' }}</el-tag>
            <el-tag v-if="detail?.enterpriseModeLabel" type="warning" effect="plain">{{ detail.enterpriseModeLabel }}</el-tag>
          </div>
          <p class="mt-3 text-sm leading-7 text-slate-500">
            单据号：{{ documentCode }} · 明细编号：{{ detailNo }} · 更新时间：{{ detail?.updatedAt || detail?.createdAt || '-' }}
          </p>
        </div>
      </div>
    </section>

    <ExpenseInvoiceWorkbench
      :schema="detail?.schemaSnapshot || emptySchema"
      :form-data="detail?.formData || {}"
      :detail-title="detail?.detailTitle || ''"
      :detail-no="detailNo"
      :loading="loading"
    >
      <template #main-before-list>
        <el-card class="!rounded-3xl !shadow-sm" v-loading="loading">
          <template #header>
            <div class="flex items-center justify-between gap-3">
              <div>
                <p class="text-lg font-semibold text-slate-800">明细表单快照</p>
                <p class="mt-1 text-sm text-slate-500">这里展示的是单据提交后保存下来的费用明细内容。</p>
              </div>
              <el-tag effect="plain">排序：{{ detail?.sortOrder || '-' }}</el-tag>
            </div>
          </template>

          <ExpenseFormReadonlyRenderer
            v-if="detail"
            :schema="detail.schemaSnapshot"
            :form-data="detail.formData"
            :company-options="companyOptions"
            :department-options="departmentOptions"
            :detail-type="detail.detailType"
            :default-business-scenario="detail.businessSceneMode || ''"
            :payee-option-map="payeeOptionMap"
            :payee-account-option-map="payeeAccountOptionMap"
          />
          <el-empty v-else description="暂无费用明细数据" :image-size="90" />
        </el-card>
      </template>
    </ExpenseInvoiceWorkbench>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { expenseApi, type ExpenseDetailInstanceDetail, type ProcessFormDesignSchema, type ProcessFormOption } from '@/api'
import ExpenseInvoiceWorkbench from './components/ExpenseInvoiceWorkbench.vue'
import ExpenseFormReadonlyRenderer from './components/ExpenseFormReadonlyRenderer.vue'
import { useReadonlyPayeeLookups } from './useReadonlyPayeeLookups'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<ExpenseDetailInstanceDetail | null>(null)
const companyOptions = ref<ProcessFormOption[]>([])
const departmentOptions = ref<ProcessFormOption[]>([])
const { payeeOptionMap, payeeAccountOptionMap, syncReadonlyPayeeLookups } = useReadonlyPayeeLookups()
const emptySchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }

const documentCode = String(route.params.documentCode || '')
const detailNo = String(route.params.detailNo || '')

void loadDetail()

async function loadDetail() {
  if (!documentCode || !detailNo) {
    ElMessage.warning('缺少单据编号或明细编号')
    void router.replace('/expense/list')
    return
  }

  loading.value = true
  try {
    const [detailRes, documentRes] = await Promise.all([
      expenseApi.getExpenseDetail(documentCode, detailNo),
      expenseApi.getDetail(documentCode)
    ])
    detail.value = detailRes.data
    companyOptions.value = documentRes.data.companyOptions || []
    departmentOptions.value = documentRes.data.departmentOptions || []
    void syncReadonlyPayeeLookups(detailRes.data.schemaSnapshot)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载费用明细失败'))
  } finally {
    loading.value = false
  }
}

function goBack() {
  void router.push({
    name: 'expense-document-detail',
    params: {
      documentCode
    }
  })
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>
