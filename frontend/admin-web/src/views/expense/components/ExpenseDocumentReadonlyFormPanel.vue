<template>
  <el-card class="expense-wb-panel">
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-lg font-semibold text-slate-800">单据表单</p>
          <p class="mt-1 text-sm text-slate-500">根据提交时保存的表单快照回看单据内容。</p>
        </div>
        <el-tag effect="plain">金额：{{ amountText }}</el-tag>
      </div>
    </template>

    <ExpenseFormReadonlyRenderer
      v-if="display"
      :document-code="display.documentCode"
      :schema="display.schema"
      :form-data="display.formData"
      :company-options="display.companyOptions"
      :department-options="display.departmentOptions"
      :shared-archives="display.sharedArchives"
      :vendor-option-map="display.vendorOptionMap"
      :payee-option-map="display.payeeOptionMap"
      :payee-account-option-map="display.payeeAccountOptionMap"
    />
    <el-empty v-else description="暂无单据数据" :image-size="96" />
  </el-card>
</template>

<script setup lang="ts">
import type {
  ExpenseCreatePayeeAccountOption,
  ExpenseCreatePayeeOption,
  ExpenseCreateVendorOption,
  ProcessCustomArchiveDetail,
  ProcessFormDesignSchema,
  ProcessFormOption
} from '@/api'
import ExpenseFormReadonlyRenderer from './ExpenseFormReadonlyRenderer.vue'

type ReadonlyFormDisplay = {
  documentCode: string
  schema: ProcessFormDesignSchema
  formData: Record<string, unknown>
  companyOptions: ProcessFormOption[]
  departmentOptions: ProcessFormOption[]
  sharedArchives: ProcessCustomArchiveDetail[]
  vendorOptionMap: Record<string, ExpenseCreateVendorOption>
  payeeOptionMap: Record<string, ExpenseCreatePayeeOption>
  payeeAccountOptionMap: Record<string, ExpenseCreatePayeeAccountOption>
}

defineProps<{
  amountText: string
  display: ReadonlyFormDisplay | null
}>()
</script>
