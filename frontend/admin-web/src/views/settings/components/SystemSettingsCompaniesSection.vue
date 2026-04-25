<template>
  <div class="space-y-5">
    <SystemSettingsCompaniesTab
      :can-create="permissions.canCreate"
      :can-edit="permissions.canEdit"
      :can-delete="permissions.canDelete"
      :flat-companies="state.flatCompanies"
      :selected-company="state.selectedCompany"
      :format-status-label="state.formatStatusLabel"
      @select-company="$emit('update:selectedCompany', $event)"
      @create="actions.openCompanyDialog()"
      @edit="actions.openCompanyDialog($event)"
      @delete-selected="actions.handleDeleteCompany()"
    />

    <el-dialog
      :model-value="state.companyDialogVisible"
      :title="state.selectedCompany ? '编辑公司' : '新增公司'"
      width="620px"
      :close-on-press-escape="false"
      @update:model-value="$emit('update:companyDialogVisible', $event)"
    >
      <el-form label-width="100px">
        <div class="mb-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
          <template v-if="state.selectedCompany">
            <div>主体编码：{{ state.selectedCompany.companyId }}</div>
            <div class="mt-1">公司编号：{{ state.selectedCompany.companyCode }}</div>
          </template>
          <template v-else>主体编码和公司编号将在保存后自动生成。</template>
        </div>
        <el-form-item label="公司名称">
          <el-input v-model="state.companyForm.companyName" />
        </el-form-item>
        <el-form-item label="公司抬头">
          <el-input v-model="state.companyForm.invoiceTitle" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="state.companyForm.taxNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="$emit('update:companyDialogVisible', false)">取消</el-button>
        <template v-if="state.selectedCompany?.companyId">
          <el-button type="primary" @click="actions.saveCompany(true)">保存</el-button>
        </template>
        <template v-else>
          <el-button type="primary" plain @click="actions.saveCompany(true)">保存并退出</el-button>
          <el-button type="primary" @click="actions.saveCompany(false)">保存并新增</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { CompanyRecord, CompanySavePayload } from '@/api'
import type { FlatCompanyRecord } from '../systemSettingsShared'
import SystemSettingsCompaniesTab from './SystemSettingsCompaniesTab.vue'

defineProps<{
  permissions: {
    canCreate: boolean
    canEdit: boolean
    canDelete: boolean
  }
  state: {
    flatCompanies: FlatCompanyRecord[]
    selectedCompany?: CompanyRecord
    formatStatusLabel: (status: number) => string
    companyDialogVisible: boolean
    companyForm: CompanySavePayload
  }
  actions: {
    openCompanyDialog: (company?: CompanyRecord) => void
    handleDeleteCompany: () => Promise<void>
    saveCompany: (closeAfterSave: boolean) => Promise<void>
  }
}>()

defineEmits<{
  (e: 'update:selectedCompany', value?: CompanyRecord): void
  (e: 'update:companyDialogVisible', value: boolean): void
}>()
</script>
