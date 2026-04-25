<template>
  <div class="space-y-5">
    <SystemSettingsCompanyAccountsTab
      :can-create="permissions.canCreate"
      :can-edit="permissions.canEdit"
      :can-delete="permissions.canDelete"
      :company-options="state.companyOptions"
      :company-account-company-filter="state.companyAccountCompanyFilter"
      :company-account-status-filter="state.companyAccountStatusFilter"
      :company-account-direct-connect-filter="state.companyAccountDirectConnectFilter"
      :filtered-company-bank-accounts="state.filteredCompanyBankAccounts"
      :resolve-company-name="state.resolveCompanyName"
      :mask-account-no="state.maskAccountNo"
      :format-boolean-tag="state.formatBooleanTag"
      :format-status-label="state.formatStatusLabel"
      @update:company-account-company-filter="$emit('update:companyAccountCompanyFilter', $event)"
      @update:company-account-status-filter="$emit('update:companyAccountStatusFilter', $event)"
      @update:company-account-direct-connect-filter="$emit('update:companyAccountDirectConnectFilter', $event)"
      @create="actions.openCompanyBankAccountDialog()"
      @edit="actions.openCompanyBankAccountDialog($event)"
      @set-default="actions.setCompanyBankAccountDefault($event)"
      @toggle-status="actions.toggleCompanyBankAccountStatus($event.row, $event.status)"
      @delete-account="actions.handleDeleteCompanyBankAccount($event)"
    />

    <el-dialog
      :model-value="state.companyBankAccountDialogVisible"
      :title="state.editingCompanyBankAccount ? '编辑公司账户' : '新增公司账户'"
      width="820px"
      :close-on-press-escape="false"
      @closed="actions.resetCompanyBankAccountForm"
      @update:model-value="$emit('update:companyBankAccountDialogVisible', $event)"
    >
      <el-form label-width="110px">
        <div class="space-y-4">
          <div class="rounded-2xl border border-slate-200 p-4">
            <div class="mb-4 text-sm font-semibold text-slate-900">基础账户信息</div>
            <div class="grid gap-4 md:grid-cols-2">
              <el-form-item label="所属公司" required>
                <el-select v-model="state.companyBankAccountForm.companyId" class="w-full">
                  <el-option
                    v-for="item in state.companyOptions"
                    :key="item.companyId"
                    :label="item.label"
                    :value="item.companyId"
                  />
                </el-select>
              </el-form-item>
              <div class="md:col-span-2">
                <SupplierPaymentInfoFields
                  :form-state="state.companyBankAccountForm"
                  :required="true"
                  :field-map="companyBankAccountFieldMap"
                  account-name-label="账户名"
                  business-scope="PUBLIC"
                />
              </div>
              <el-form-item label="账户类型">
                <el-input v-model="state.companyBankAccountForm.accountType" />
              </el-form-item>
              <el-form-item label="账户用途">
                <el-input v-model="state.companyBankAccountForm.accountUsage" />
              </el-form-item>
              <el-form-item label="币种">
                <el-input v-model="state.companyBankAccountForm.currencyCode" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="state.companyBankAccountForm.status" class="w-full">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-form-item>
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <el-form-item label="默认账户">
                <el-switch
                  v-model="state.companyBankAccountForm.defaultAccount"
                  :active-value="1"
                  :inactive-value="0"
                />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="state.companyBankAccountForm.remark" type="textarea" :rows="3" />
              </el-form-item>
            </div>
          </div>

          <div class="rounded-2xl border border-slate-200 p-4">
            <div class="mb-4 flex items-center justify-between gap-3">
              <div class="text-sm font-semibold text-slate-900">银企直连预留信息</div>
              <el-switch
                v-model="state.companyBankAccountForm.directConnectEnabled"
                :active-value="1"
                :inactive-value="0"
                active-text="直连启用"
              />
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <el-form-item label="提供方">
                <el-input v-model="state.companyBankAccountForm.directConnectProvider" />
              </el-form-item>
              <el-form-item label="渠道">
                <el-input v-model="state.companyBankAccountForm.directConnectChannel" />
              </el-form-item>
              <el-form-item label="协议">
                <el-input v-model="state.companyBankAccountForm.directConnectProtocol" />
              </el-form-item>
              <el-form-item label="客户号">
                <el-input v-model="state.companyBankAccountForm.directConnectCustomerNo" />
              </el-form-item>
              <el-form-item label="App ID">
                <el-input v-model="state.companyBankAccountForm.directConnectAppId" />
              </el-form-item>
              <el-form-item label="账户别名">
                <el-input v-model="state.companyBankAccountForm.directConnectAccountAlias" />
              </el-form-item>
              <el-form-item label="认证方式">
                <el-input v-model="state.companyBankAccountForm.directConnectAuthMode" />
              </el-form-item>
              <el-form-item label="API 地址">
                <el-input v-model="state.companyBankAccountForm.directConnectApiBaseUrl" />
              </el-form-item>
              <el-form-item label="证书引用">
                <el-input v-model="state.companyBankAccountForm.directConnectCertRef" />
              </el-form-item>
              <el-form-item label="密钥引用">
                <el-input v-model="state.companyBankAccountForm.directConnectSecretRef" />
              </el-form-item>
              <el-form-item label="签名方式">
                <el-input v-model="state.companyBankAccountForm.directConnectSignType" />
              </el-form-item>
              <el-form-item label="加密方式">
                <el-input v-model="state.companyBankAccountForm.directConnectEncryptType" />
              </el-form-item>
              <el-form-item label="最近同步时间">
                <el-input v-model="state.companyBankAccountForm.directConnectLastSyncAt" />
              </el-form-item>
              <el-form-item label="最近同步状态">
                <el-input v-model="state.companyBankAccountForm.directConnectLastSyncStatus" />
              </el-form-item>
              <el-form-item label="最近同步报错" class="md:col-span-2">
                <el-input
                  v-model="state.companyBankAccountForm.directConnectLastErrorMsg"
                  type="textarea"
                  :rows="2"
                />
              </el-form-item>
              <el-form-item label="扩展 JSON" class="md:col-span-2">
                <el-input v-model="state.companyBankAccountForm.directConnectExtJson" type="textarea" :rows="3" />
              </el-form-item>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="$emit('update:companyBankAccountDialogVisible', false)">取消</el-button>
        <el-button type="primary" @click="actions.saveCompanyBankAccount()">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'
import type { CompanyBankAccountRecord } from '@/api'
import type { CompanyBankAccountFormState, CompanyOption } from '../systemSettingsShared'
import { companyBankAccountFieldMap } from '../systemSettingsShared'
import SystemSettingsCompanyAccountsTab from './SystemSettingsCompanyAccountsTab.vue'

defineProps<{
  permissions: {
    canCreate: boolean
    canEdit: boolean
    canDelete: boolean
  }
  state: {
    companyOptions: CompanyOption[]
    companyAccountCompanyFilter?: string
    companyAccountStatusFilter?: number
    companyAccountDirectConnectFilter?: number
    filteredCompanyBankAccounts: CompanyBankAccountRecord[]
    resolveCompanyName: (companyId: string) => string
    maskAccountNo: (accountNo?: string) => string
    formatBooleanTag: (value: number) => string
    formatStatusLabel: (status: number) => string
    companyBankAccountDialogVisible: boolean
    editingCompanyBankAccount?: CompanyBankAccountRecord
    companyBankAccountForm: CompanyBankAccountFormState
  }
  actions: {
    openCompanyBankAccountDialog: (row?: CompanyBankAccountRecord) => void
    setCompanyBankAccountDefault: (row: CompanyBankAccountRecord) => Promise<void>
    toggleCompanyBankAccountStatus: (row: CompanyBankAccountRecord, status: number) => Promise<void>
    handleDeleteCompanyBankAccount: (row: CompanyBankAccountRecord) => Promise<void>
    resetCompanyBankAccountForm: () => void
    saveCompanyBankAccount: () => Promise<void>
  }
}>()

defineEmits<{
  (e: 'update:companyAccountCompanyFilter', value?: string): void
  (e: 'update:companyAccountStatusFilter', value?: number): void
  (e: 'update:companyAccountDirectConnectFilter', value?: number): void
  (e: 'update:companyBankAccountDialogVisible', value: boolean): void
}>()
</script>
