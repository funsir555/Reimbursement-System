<template>







  <div class="grid grid-cols-1 gap-5 md:grid-cols-2">







    <div







      v-for="block in visibleBlocks"







      :key="block.blockId"







      class="form-runtime-block"







      :class="block.span === 2 ? 'md:col-span-2' : ''"







    >







      <template v-if="controlType(block) === 'SECTION'">







        <div class="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">







          <p class="text-lg font-semibold text-slate-800">{{ block.label }}</p>







          <p class="mt-2 text-sm leading-6 text-slate-500">{{ String(block.props.content || block.helpText || '') }}</p>







        </div>







      </template>















      <el-form-item







        v-else







        :label="block.label"







        :required="block.required"







        class="!mb-0"







      >







        <template v-if="block.kind === 'CONTROL'">







          <el-input







            v-if="controlType(block) === 'TEXT'"







            v-model="formData[block.fieldKey]"







            class="expense-runtime-control"







            :maxlength="documentTitleMaxLength(block)"







            :show-word-limit="Boolean(documentTitleMaxLength(block))"







            :placeholder="placeholderOf(block)"







            :readonly="isReadOnly(block)"







          />







          <el-input







            v-else-if="controlType(block) === 'TEXTAREA'"







            v-model="formData[block.fieldKey]"







            type="textarea"







            :rows="4"







            :maxlength="documentTitleMaxLength(block)"







            :show-word-limit="Boolean(documentTitleMaxLength(block))"







            :placeholder="placeholderOf(block)"







            :readonly="isReadOnly(block)"







          />







          <el-input-number







            v-else-if="controlType(block) === 'NUMBER'"







            v-model="formData[block.fieldKey]"







            :controls="false"







            class="w-full expense-runtime-control"







            :disabled="isReadOnly(block)"







          />







          <money-input







            v-else-if="controlType(block) === 'AMOUNT'"







            :model-value="toOptionalString(formData[block.fieldKey])"







            class="w-full expense-runtime-control"







            :disabled="isReadOnly(block)"







            @update:model-value="formData[block.fieldKey] = $event"







          />







          <el-date-picker







            v-else-if="controlType(block) === 'DATE'"







            v-model="formData[block.fieldKey]"







            type="date"







            value-format="YYYY-MM-DDTHH:mm:ss"







            class="w-full expense-runtime-control"







            placeholder="请选择日期"







          />







          <el-date-picker







            v-else-if="controlType(block) === 'DATE_RANGE'"







            v-model="formData[block.fieldKey]"







            type="daterange"







            value-format="YYYY-MM-DDTHH:mm:ss"







            range-separator="至"







            start-placeholder="开始日期"







            end-placeholder="结束日期"







            class="w-full expense-runtime-control"







          />







          <el-select







            v-else-if="controlType(block) === 'SELECT'"







            v-model="formData[block.fieldKey]"







            :clearable="!isReadOnly(block)"







            class="w-full expense-runtime-control"







            :placeholder="placeholderOf(block)"







            :disabled="isReadOnly(block)"







          >







            <el-option







              v-for="item in optionItems(block)"







              :key="item.value"







              :label="item.label"







              :value="item.value"







            />







          </el-select>







          <el-select







            v-else-if="controlType(block) === 'MULTI_SELECT'"







            v-model="formData[block.fieldKey]"







            multiple







            :clearable="!isReadOnly(block)"







            collapse-tags







            collapse-tags-tooltip







            class="w-full expense-runtime-control"







            :placeholder="placeholderOf(block)"







            :disabled="isReadOnly(block)"







          >







            <el-option







              v-for="item in optionItems(block)"







              :key="item.value"







              :label="item.label"







              :value="item.value"







            />







          </el-select>







          <el-radio-group







            v-else-if="controlType(block) === 'RADIO'"







            v-model="formData[block.fieldKey]"







            class="flex flex-wrap gap-3"







            :disabled="isReadOnly(block)"







          >







            <el-radio







              v-for="item in optionItems(block)"







              :key="item.value"







              :label="item.value"







            >







              {{ item.label }}







            </el-radio>







          </el-radio-group>







          <el-checkbox-group







            v-else-if="controlType(block) === 'CHECKBOX'"







            v-model="formData[block.fieldKey]"







            class="flex flex-wrap gap-3"







            :disabled="isReadOnly(block)"







          >







            <el-checkbox







              v-for="item in optionItems(block)"







              :key="item.value"







              :label="item.value"







            >







              {{ item.label }}







            </el-checkbox>







          </el-checkbox-group>







          <el-switch







            v-else-if="controlType(block) === 'SWITCH'"







            v-model="formData[block.fieldKey]"







            :disabled="isReadOnly(block)"







            inline-prompt
            active-text="开"
            inactive-text="关"






          />







          <el-upload







            v-else-if="controlType(block) === 'ATTACHMENT' || controlType(block) === 'IMAGE'"







            action="#"







            :auto-upload="false"







            :accept="uploadAccept(block)"







            :multiple="true"







            :limit="Number(block.props.maxCount || 1)"







            :show-file-list="true"







            :file-list="uploadFileList(block)"







            :disabled="isReadOnly(block)"







            @change="handleFileChange(block, $event)"







            @remove="handleFileRemove(block, $event)"







          >







            <el-button>选择文件</el-button>







            <template #tip>







              <div class="mt-2 text-xs text-slate-400">







                最多 {{ Number(block.props.maxCount || 1) }} 个文件，单个不超过 {{ Number(block.props.maxSizeMb || 1) }} MB







              </div>







            </template>







          </el-upload>







          <el-input







            v-else







            v-model="formData[block.fieldKey]"







            class="expense-runtime-control"







            :placeholder="placeholderOf(block)"







            :readonly="isReadOnly(block)"







          />







        </template>















        <template v-else-if="businessCode(block) === 'counterparty'">







          <el-select







            v-model="formData[block.fieldKey]"
            :ref="(instance) => setCounterpartySelectRef(block.fieldKey, instance)"







            :data-testid="`counterparty-select-${block.fieldKey}`"







            filterable







            remote



            



            






            clearable
            :persistent="false"
            :teleported="false"







            class="w-full expense-runtime-control expense-runtime-counterparty-select"







            :placeholder="counterpartyPlaceholder"







            :remote-method="loadVendorOptions"







            :loading="vendorOptionsLoading"







            :disabled="isCounterpartyDisabled(block)"
            @change="handleCounterpartySelection(block.fieldKey, $event)"







          >







            <el-option







              v-for="item in visibleVendorOptions"







              :key="item.value"







              :label="item.label"







              :value="item.value"







            >







              <div class="flex items-center justify-between gap-3">







                <span class="truncate">{{ item.label }}</span>







                <span class="text-xs text-slate-400">{{ item.secondaryLabel }}</span>







              </div>







            </el-option>







            <template #footer>







              <button







                type="button"







                :data-testid="`counterparty-create-vendor-${block.fieldKey}`"







                class="flex w-full items-center justify-center rounded-xl border border-dashed border-sky-200 bg-sky-50 px-3 py-2 text-sm font-medium text-sky-700 transition hover:border-sky-300 hover:bg-sky-100"







                :disabled="!effectivePaymentCompanyId || isReadOnly(block)"







                @click.stop="openVendorDialog(block.fieldKey)"







              >







                新增供应商







              </button>







            </template>







          </el-select>







        </template>







        <template v-else-if="businessCode(block) === 'payee'">







          <el-select







            v-model="formData[block.fieldKey]"







            value-key="value"







            filterable







            remote



            



            reserve-keyword







            clearable







            class="w-full expense-runtime-control"







            :placeholder="PAYEE_PLACEHOLDER"







            :remote-method="loadPayeeOptions"







            :loading="payeeOptionsLoading"







            :disabled="isReadOnly(block)"







            @change="handlePayeeSelection(block.fieldKey, $event)"







          >







            <el-option







              v-for="item in payeeOptions"







              :key="item.value"







              :label="item.label"







              :value="buildPayeeSnapshot(item)"







            >







              <div class="flex items-center justify-between gap-3">







                <span class="truncate">{{ item.label }}</span>







                <span class="text-xs text-slate-400">{{ item.secondaryLabel }}</span>







              </div>







            </el-option>







          </el-select>







        </template>















        <template v-else-if="businessCode(block) === 'payee-account'">







          <el-select







            v-model="formData[block.fieldKey]"
            :ref="(instance) => setPayeeAccountSelectRef(block.fieldKey, instance)"







            :data-testid="`payee-account-select-${block.fieldKey}`"







            value-key="value"







            filterable







            remote







            






            clearable
            :persistent="false"
            :teleported="false"







            class="w-full expense-runtime-control"







            :placeholder="payeeAccountPlaceholder"







            :remote-method="loadPayeeAccountOptions"







            :loading="payeeAccountOptionsLoading"







            :disabled="isPayeeAccountDisabled(block)"







            @change="handlePayeeAccountSelection(block.fieldKey, $event)"







          >







            <el-option







              v-for="item in visiblePayeeAccountOptions"







              :key="item.value"







              :label="item.label"







              :value="buildPayeeAccountSnapshot(item)"







            >







              <div class="space-y-1">







                <div class="flex items-center justify-between gap-3">







                  <span class="truncate">{{ item.label }}</span>







                  <span class="text-xs text-slate-400">{{ item.accountNoMasked }}</span>







                </div>







                <p class="truncate text-xs text-slate-400">{{ item.secondaryLabel }}</p>







              </div>







            </el-option>







            <template v-if="showVendorAccountMaintenanceEntry" #footer>







              <button







                type="button"







                data-testid="payee-account-maintain-vendor"







                class="flex w-full items-center justify-center rounded-xl border border-dashed border-sky-200 bg-sky-50 px-3 py-2 text-sm font-medium text-sky-700 transition hover:border-sky-300 hover:bg-sky-100"







                :disabled="isReadOnly(block)"







                @click.stop="openVendorAccountDialog"







              >







                维护收款账户







              </button>







            </template>







          </el-select>







        </template>







        <template v-else-if="businessCode(block) === 'undertake-department'">







          <el-select







            v-model="formData[block.fieldKey]"







            clearable







            filterable







            class="w-full expense-runtime-control"







            :placeholder="`请选择${block.label}`"







          >







            <el-option







              v-for="item in departmentOptions"







              :key="item.value"







              :label="item.label"







              :value="item.value"







            />







          </el-select>







          <p v-if="String(formData[block.fieldKey] || '').trim()" class="mt-2 text-xs leading-6 text-slate-400">







            当前归属部门：{{ departmentLabel(String(formData[block.fieldKey] || '')) }}







          </p>







        </template>















        <template v-else-if="businessCode(block) === 'payment-company'">







          <el-select







            v-model="formData[block.fieldKey]"







            clearable







            filterable







            class="w-full expense-runtime-control"







            :placeholder="placeholderOf(block)"







          >







            <el-option







              v-for="item in companyOptions"







              :key="item.value"







              :label="item.label"







              :value="item.value"







            />







          </el-select>







        </template>















        <template v-else-if="businessCode(block) === 'bank-push-summary'">







          <el-input







            v-model="formData[block.fieldKey]"







            class="expense-runtime-control"







            maxlength="120"







            show-word-limit







            placeholder="请输入银行推送摘要"







          />







        </template>















        <template v-else-if="isDocumentBusinessBlock(block)">







          <div class="space-y-3">







            <div class="flex flex-wrap items-center justify-between gap-3">







              <p class="text-sm leading-6 text-slate-500">







                {{ documentBlockHint(block) }}







              </p>







              <button







                type="button"







                class="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-sky-300 hover:bg-sky-50"







                :data-testid="`open-document-picker-${block.fieldKey}`"







                :disabled="isReadOnly(block)"







                @mousedown.capture="prepareDocumentPickerOpen"
                @click.stop.prevent="openDocumentPicker(block)"







              >







                选择单据







              </button>







            </div>















            <div v-if="documentRecords(block).length" class="space-y-3">







              <div







                v-for="item in documentRecords(block)"







                :key="`${block.fieldKey}-${item.documentCode}`"







                class="rounded-[24px] border border-slate-200 bg-slate-50 px-4 py-4"







                :data-testid="`selected-document-${block.fieldKey}-${item.documentCode}`"







              >







                <div class="flex flex-wrap items-start justify-between gap-3">







                  <div class="min-w-0">







                    <p class="break-all text-sm font-semibold text-slate-800">







                      {{ item.documentTitle || item.documentCode }}







                    </p>







                    <p class="mt-1 break-all text-xs text-slate-500">







                      单据编号：{{ item.documentCode }}







                    </p>







                    <p class="mt-1 text-xs text-slate-500">







                      类型：{{ item.templateTypeLabel || resolveTemplateTypeLabel(item.templateType) }} / 状态：{{ item.statusLabel || item.status || '已审批' }}







                    </p>







                  </div>







                  <button







                    type="button"







                    class="text-xs font-medium text-rose-500"







                    :data-testid="`remove-document-${block.fieldKey}-${item.documentCode}`"







                    :disabled="isReadOnly(block)"







                    @click="removeDocumentRecord(block, item.documentCode)"







                  >







                    删除







                  </button>







                </div>















                <div







                  v-if="isWriteOffDocumentBlock(block)"







                  class="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-[minmax(0,1fr),minmax(0,1fr),160px]"







                >







                  <div class="rounded-2xl border border-white/80 bg-white px-4 py-3">







                    <p class="text-xs text-slate-400">核销来源</p>







                    <p class="mt-2 text-sm font-medium text-slate-700">







                      {{ writeOffSourceKindLabel(item.writeOffSourceKind) }}







                    </p>







                  </div>







                  <div class="rounded-2xl border border-white/80 bg-white px-4 py-3">







                    <p class="text-xs text-slate-400">可核销余额</p>







                    <p class="mt-2 text-sm font-medium text-slate-700">







                      {{ formatAmount(item.availableWriteOffAmount) }}







                    </p>







                  </div>







                  <div class="rounded-2xl border border-white/80 bg-white px-4 py-3">







                    <p class="text-xs text-slate-400">核销金额</p>







                    <money-input







                      :model-value="item.writeOffAmount || ''"







                      class="mt-2 w-full expense-runtime-control"







                      :data-testid="`writeoff-amount-${block.fieldKey}-${item.documentCode}`"







                      :disabled="isReadOnly(block)"







                      @update:model-value="updateWriteOffAmount(block, item.documentCode, $event)"







                    />







                  </div>







                </div>







              </div>







            </div>















            <div







              v-else







              class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-400"







            >







              暂未选择单据







            </div>







          </div>







        </template>















        <template v-else-if="block.kind === 'SHARED_FIELD'">







          <el-select







            v-model="formData[block.fieldKey]"







            clearable







            class="w-full expense-runtime-control"







            :placeholder="`请选择${block.label}`"







          >







            <el-option







              v-for="item in sharedArchiveItems(block)"







              :key="item.itemCode || item.itemName"







              :label="item.itemName"







              :value="item.itemCode || item.itemName"







            />







          </el-select>







        </template>







      </el-form-item>















      <p v-if="block.helpText && controlType(block) !== 'SECTION'" class="mt-2 text-xs leading-6 text-slate-400">







        {{ block.helpText }}







      </p>







    </div>















    <el-dialog v-model="vendorDialogVisible" :title="vendorDialogTitle" width="920px" destroy-on-close>







      <div class="space-y-5">







        <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">







          <el-form-item label="供应商名称" required class="!mb-0">







            <el-input v-model="vendorDraft.cVenName" maxlength="128" placeholder="请输入供应商名称" />







          </el-form-item>







          <el-form-item label="供应商简称" class="!mb-0">







            <el-input v-model="vendorDraft.cVenAbbName" maxlength="64" placeholder="请输入供应商简称" />







          </el-form-item>







          <el-form-item label="工商注册号" class="!mb-0">







            <el-input v-model="vendorDraft.cVenRegCode" placeholder="请输入工商注册号" />







          </el-form-item>







          <el-form-item label="联系人" class="!mb-0">







            <el-input v-model="vendorDraft.cVenPerson" maxlength="64" placeholder="请输入联系人" />







          </el-form-item>







          <el-form-item label="联系电话" class="!mb-0">







            <el-input v-model="vendorDraft.cVenPhone" maxlength="32" placeholder="请输入联系电话" />







          </el-form-item>







          <el-form-item label="联系地址" class="!mb-0">







            <el-input v-model="vendorDraft.cVenAddress" placeholder="请输入联系地址" />







          </el-form-item>







        </div>















        <SupplierPaymentInfoFields







          :form-state="vendorDraft"







          :required="true"







          :show-section-header="true"







          auto-fill-source-key="cVenName"







          account-name-label="账户名"







        />















        <el-form-item label="备注" class="!mb-0">







          <el-input v-model="vendorDraft.cMemo" type="textarea" :rows="3" placeholder="请输入备注" />







        </el-form-item>







      </div>















      <template #footer>







        <div class="flex justify-end gap-3">







          <el-button @click="closeVendorDialog">取消</el-button>







          <el-button type="primary" :loading="vendorDialogLoading || vendorSaving" @click="saveVendor">{{ vendorDialogSubmitText }}</el-button>







        </div>







      </template>







    </el-dialog>















    <el-dialog v-model="documentPickerDialog.visible" :title="documentPickerTitle" width="920px" destroy-on-close>
      <div class="space-y-4">
        <div class="flex flex-wrap items-center gap-3">
          <input
            v-model.trim="documentPickerDialog.keyword"
            class="min-w-[220px] flex-1 rounded-2xl border border-slate-200 px-4 py-2.5 text-sm text-slate-700 outline-none transition focus:border-sky-400"
            placeholder="搜索单据编号、标题或模板名称"
          />
          <button
            type="button"
            class="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-sky-300 hover:bg-sky-50"
            data-testid="search-document-picker"
            @click="loadDocumentPicker"
          >
            搜索
          </button>
        </div>

        <div v-if="documentPickerDialog.loading" class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
          正在加载可选单据...
        </div>

        <div v-else-if="documentPickerDialog.groups.length" class="space-y-4">
          <el-tabs v-model="documentPickerDialog.activeTemplateType" class="document-picker-tabs">
            <el-tab-pane
              v-for="group in documentPickerDialog.groups"
              :key="`${documentPickerDialog.fieldKey}-${group.templateType}`"
              :label="`${group.templateTypeLabel}（${group.total}）`"
              :name="group.templateType"
            >
              <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-4" :data-testid="`document-picker-panel-${group.templateType}`">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-semibold text-slate-800">{{ group.templateTypeLabel }}</p>
                    <p class="mt-1 text-xs text-slate-500">点击上方页签切换单据类型，当前共 {{ group.total }} 条可选单据</p>
                  </div>
                  <p class="text-xs text-slate-400">
                    已选 {{ selectedGroupCount(group) }} / {{ group.items.length }}
                  </p>
                </div>

                <div v-if="group.items.length" class="mt-4 space-y-3">
                  <button
                    v-for="item in group.items"
                    :key="item.documentCode"
                    type="button"
                    class="w-full rounded-2xl border px-4 py-4 text-left transition"
                    :class="isDocumentSelected(item.documentCode) ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white hover:border-sky-200'"
                    :data-testid="`toggle-document-picker-${item.documentCode}`"
                    @click="toggleDocumentSelection(item)"
                  >
                    <div class="flex flex-wrap items-start justify-between gap-3">
                      <div class="min-w-0">
                        <p class="break-all text-sm font-semibold text-slate-800">
                          {{ item.documentTitle || item.documentCode }}
                        </p>
                        <p class="mt-1 break-all text-xs text-slate-500">单据编号：{{ item.documentCode }}</p>
                        <p class="mt-1 text-xs text-slate-500">
                          模板：{{ item.templateName || group.templateTypeLabel }} / 状态：{{ item.statusLabel }}
                        </p>
                      </div>
                      <div class="text-right text-xs text-slate-500">
                        <p>金额：{{ formatAmount(item.totalAmount) }}</p>
                        <p v-if="documentPickerDialog.relationType === 'WRITEOFF'" class="mt-1">
                          可核销：{{ formatAmount(item.availableWriteOffAmount) }}
                        </p>
                      </div>
                    </div>
                  </button>
                </div>
                <div v-else class="mt-4 rounded-2xl border border-dashed border-slate-200 bg-white px-4 py-6 text-center text-sm text-slate-400" data-testid="document-picker-tab-empty">
                  当前类型暂无可选单据
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div v-else class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-400">
          暂无可选单据
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeDocumentPicker">取消</el-button>
          <el-button type="primary" data-testid="confirm-document-picker" @click="confirmDocumentPicker">确认选择</el-button>
        </div>
      </template>
    </el-dialog>







  </div>







</template>















<script setup lang="ts">







import { computed, nextTick, reactive, ref, watch } from 'vue'







import { ElMessage, type UploadFile, type UploadUserFile } from 'element-plus'







import {







  expenseApi,







  expenseCreateApi,







  type ExpenseAttachmentMeta,







  type ExpenseCreatePayeeAccountOption,







  type ExpenseCreatePayeeOption,







  type ExpenseCreateVendorOption,







  type ExpenseDocumentPickerGroup,







  type ExpenseDocumentPickerItem,







  type ExpenseRelatedDocumentValue,







  type ExpenseWriteOffDocumentValue,







  type FinanceVendorDetail,







  type FinanceVendorSavePayload,







  type ProcessCustomArchiveDetail,







  type ProcessFormDesignBlock,







  type ProcessFormDesignSchema,







  type ProcessFormOption







} from '@/api'







import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'







import MoneyInput from '@/components/inputs/MoneyInput.vue'







import { compareMoney, formatMoney, normalizeMoneyValue, subtractMoney } from '@/utils/money'







import {







  getBusinessComponentDefinition,







  getControlType,







  normalizeBusinessComponentAllowedTemplateTypes,







  getOptionItems,







  getSharedArchiveCode







} from '@/views/process/formDesignerHelper'







import {
  ensureExpenseDetailFormDefaults,
  FIELD_INVOICE_ATTACHMENTS,
  isExpenseDetailBlockReadOnly,
  isExpenseDetailBlockVisible,
  resolveInvoiceOcrTotal,
  syncInvoiceAmountWithOcr
} from '@/views/expense/expenseDetailRuntime'







import { documentTitleMaxLength, validateRuntimeTitleValues } from '@/views/process/pmValidation'















const formData = defineModel<Record<string, unknown>>({ required: true })















type DocumentRelationType = 'RELATED' | 'WRITEOFF'







type RuntimeDocumentRecord = ExpenseRelatedDocumentValue & Partial<ExpenseWriteOffDocumentValue>

type FocusManagedSelectInstance = {
  blur?: () => void
  handleClose?: () => void
  handleQueryChange?: (value: string) => void
  toggleMenu?: () => void
  expanded?: boolean
  query?: string
  previousQuery?: string
  states?: { inputValue?: string }
  inputRef?: { blur?: () => void; input?: HTMLInputElement | null } | null
  $el?: Element | null
}







type PayeeAccountLinkageMode = 'EMPLOYEE' | 'ENTERPRISE'















type RuntimePayeeSnapshot = {







  value: string







  label: string







  sourceType: string







  sourceCode: string







}















type RuntimePayeeAccountSnapshot = {







  value: string







  label: string







  sourceType: string







  ownerCode?: string







  ownerName?: string







  accountName?: string







  accountNoMasked?: string







  bankName?: string







}















const PERSONAL_PAYEE_PREFIX = 'PERSONAL_PAYEE:'







const ENTERPRISE_DETAIL_TYPE = 'ENTERPRISE_TRANSACTION'







const PAYEE_PLACEHOLDER = '\u8bf7\u9009\u62e9\u6536\u6b3e\u4eba'







const PAYEE_ACCOUNT_PLACEHOLDER = '\u8bf7\u9009\u62e9\u6536\u6b3e\u8d26\u6237'







const LOAD_PAYEE_ERROR = '\u52a0\u8f7d\u6536\u6b3e\u4eba\u5931\u8d25'







const LOAD_PAYEE_ACCOUNT_ERROR = '\u52a0\u8f7d\u6536\u6b3e\u8d26\u6237\u5931\u8d25'







const INVOICE_ATTACHMENT_ALLOWED_EXTENSIONS = new Set(['.pdf', '.png', '.jpg', '.jpeg'])







const INVOICE_ATTACHMENT_ALLOWED_MIME_TYPES = new Set(['application/pdf', 'image/png', 'image/jpeg'])







const INVOICE_ATTACHMENT_INVALID_MESSAGE = '发票附件仅支持 PDF、PNG、JPG、JPEG 文件'















const props = withDefaults(defineProps<{







  schema: ProcessFormDesignSchema







  sharedArchives?: ProcessCustomArchiveDetail[]







  companyOptions?: ProcessFormOption[]







  departmentOptions?: ProcessFormOption[]







  currentUserCompanyId?: string

  approvalEditMode?: boolean

  allowEditFormModule?: boolean

  allowEditPayAccount?: boolean

  detailType?: string







  defaultBusinessScenario?: string







}>(), {







  sharedArchives: () => [],







  companyOptions: () => [],







  departmentOptions: () => [],







  currentUserCompanyId: '',

  approvalEditMode: false,

  allowEditFormModule: false,

  allowEditPayAccount: false,

  detailType: '',







  defaultBusinessScenario: ''







})















const blocks = computed(() => props.schema?.blocks || [])















function validateBeforeSubmit() {







  const issues = validateRuntimeTitleValues(props.schema, formData.value || {})







  if (issues.length) {







    ElMessage.warning(issues[0])







    return false







  }







  return true







}















defineExpose({ validateBeforeSubmit })







const visibleBlocks = computed(() => (







  props.detailType







    ? blocks.value.filter((block) => isVisible(block))







    : blocks.value

))











const sharedArchiveMap = computed(() => new Map((props.sharedArchives || []).map((item) => [item.archiveCode, item])))







const companyOptions = computed(() => props.companyOptions || [])







const departmentOptions = computed(() => props.departmentOptions || [])















const vendorOptions = ref<ExpenseCreateVendorOption[]>([])







const vendorOptionsLoading = ref(false)







const payeeOptions = ref<ExpenseCreatePayeeOption[]>([])







const payeeOptionsLoading = ref(false)







const payeeAccountOptions = ref<ExpenseCreatePayeeAccountOption[]>([])







const payeeAccountOptionsLoading = ref(false)
const counterpartySelectRefs = ref<Record<string, FocusManagedSelectInstance | null>>({})
const payeeAccountSelectRefs = ref<Record<string, FocusManagedSelectInstance | null>>({})
const suppressLinkedFieldReset = ref(false)
let linkedFieldResetSyncToken = 0







const documentPickerDialog = reactive<{
  visible: boolean
  fieldKey: string
  relationType: DocumentRelationType
  keyword: string
  loading: boolean
  groups: ExpenseDocumentPickerGroup[]
  activeTemplateType: string
  selectedCodes: string[]
  itemsByCode: Record<string, RuntimeDocumentRecord>
}>({
  visible: false,
  fieldKey: '',
  relationType: 'RELATED',
  keyword: '',
  loading: false,
  groups: [],
  activeTemplateType: '',
  selectedCodes: [],
  itemsByCode: {}
})

const vendorDialogVisible = ref(false)







const vendorDialogFieldKey = ref('')







const vendorSaving = ref(false)







const vendorDialogMode = ref<'create' | 'edit'>('create')







const vendorDialogLoading = ref(false)







const vendorDialogVendorCode = ref('')







const EXPENSE_VENDOR_FIELD_MAX_LENGTH: Record<string, number> = {







  cVenName: 128,







  cVenAbbName: 64,







  cVenPerson: 64,







  cVenPhone: 32,







  receiptAccountName: 128,







  cVenBank: 128,







  cVenAccount: 64,







  receiptBranchName: 128







}







const emptyVendorDraft = (): FinanceVendorSavePayload => ({







  cVenName: '',







  cVenAbbName: '',







  cVenRegCode: '',







  cVenPerson: '',







  cVenPhone: '',







  cVenAddress: '',







  receiptAccountName: '',







  cVenBankCode: '',







  cVenBank: '',







  receiptBankProvince: '',







  receiptBankCity: '',







  receiptBranchCode: '',







  receiptBranchName: '',







  cVenAccount: '',







  cMemo: ''







})







const vendorDraft = reactive<FinanceVendorSavePayload>(emptyVendorDraft())







const paymentCompanyFieldKeys = computed(() => findBusinessFieldKeys('payment-company'))







const payeeFieldKeys = computed(() => findBusinessFieldKeys('payee'))







const counterpartyFieldKeys = computed(() => findBusinessFieldKeys('counterparty'))







const payeeAccountFieldKeys = computed(() => findBusinessFieldKeys('payee-account'))







const hasCounterpartyField = computed(() => counterpartyFieldKeys.value.length > 0)







const selectedPaymentCompanyId = computed(() => resolveSelectedPaymentCompanyId())







const effectivePaymentCompanyId = computed(() => (







  selectedPaymentCompanyId.value || String(props.currentUserCompanyId || '').trim()

))











const selectedPayeeName = computed(() => resolveSelectedPayeeName())







const selectedCounterpartyCode = computed(() => resolveSelectedCounterpartyCode())







const payeeAccountLinkageMode = computed<PayeeAccountLinkageMode>(() => resolvePayeeAccountLinkageMode())







const visibleVendorOptions = computed(() => mergeCurrentVendorOption(vendorOptions.value))







const visiblePayeeAccountOptions = computed(() => mergeCurrentPayeeAccountOption(payeeAccountOptions.value))







const counterpartyPlaceholder = computed(() => (







  effectivePaymentCompanyId.value ? '请选择收款单位' : '请先选择付款公司'

))











const payeeAccountPlaceholder = computed(() => {







  if (payeeAccountLinkageMode.value === 'ENTERPRISE') {







    if (!effectivePaymentCompanyId.value) {







      return '请先选择付款公司'







    }







    if (!selectedCounterpartyCode.value) {







      return '请先选择收款单位'







    }







  }







  return PAYEE_ACCOUNT_PLACEHOLDER







})







const showVendorAccountMaintenanceEntry = computed(() => (



  payeeAccountLinkageMode.value === 'ENTERPRISE'



    && Boolean(effectivePaymentCompanyId.value)



    && Boolean(selectedCounterpartyCode.value)



    && !payeeAccountOptionsLoading.value



    && payeeAccountOptions.value.length === 0



))



const vendorDialogTitle = computed(() => (



  vendorDialogMode.value === 'edit' ? '维护收款账户' : '新增供应商'



))



const vendorDialogSubmitText = computed(() => (
  vendorDialogMode.value === 'edit' ? '保存收款账户' : '保存供应商'
))

void loadVendorOptions('')

void loadPayeeOptions('')

void loadPayeeAccountOptions('')















watch(







  () => [props.schema, props.detailType, props.defaultBusinessScenario],







  () => {







    if (!props.detailType) {







      return







    }







    ensureExpenseDetailFormDefaults(formData.value, props.schema, props.detailType, props.defaultBusinessScenario)







  },







  { immediate: true, deep: true }







)

watch(
  () => formData.value,
  () => {
    syncLinkedLookupsFromExternalState()
  },
  { immediate: true }
)
















watch(







  () => selectedPaymentCompanyId.value,







  (nextCompanyId, prevCompanyId) => {







    if (nextCompanyId === prevCompanyId) {







      return







    }







    if (suppressLinkedFieldReset.value) {
      void loadVendorOptions('')
      void loadPayeeAccountOptions('', { settleAfterLoad: true })
      return
    }
    clearCounterpartySelections()







    clearPayeeAccountSelections()







    vendorOptions.value = []







    payeeAccountOptions.value = []







    if (effectivePaymentCompanyId.value) {







      void loadVendorOptions('')







    }







  },







  { immediate: false }







)















watch(







  () => props.currentUserCompanyId,







  (nextCompanyId, prevCompanyId) => {







    if (selectedPaymentCompanyId.value) {







      return







    }







    const nextValue = String(nextCompanyId || '').trim()







    const prevValue = String(prevCompanyId || '').trim()







    if (nextValue === prevValue) {







      return







    }







    vendorOptions.value = []







    if (nextValue) {







      void loadVendorOptions('')







    }







  },







  { immediate: false }







)















watch(







  () => [payeeAccountLinkageMode.value, selectedPayeeName.value, selectedCounterpartyCode.value],







  ([nextMode, nextPayeeName, nextCounterpartyCode], [prevMode, prevPayeeName, prevCounterpartyCode]) => {







    const changed = nextMode !== prevMode || nextPayeeName !== prevPayeeName || nextCounterpartyCode !== prevCounterpartyCode







    if (!changed) {







      return







    }







    if (suppressLinkedFieldReset.value) {
      void loadPayeeAccountOptions('', { settleAfterLoad: true })
      return
    }
    settleLinkedSelectInteractions()

    clearPayeeAccountSelections()

    void nextTick(() => settleLinkedSelectInteractions())

    void loadPayeeAccountOptions('', { settleAfterLoad: true })







  },







  { immediate: false }







)















function controlType(block: ProcessFormDesignBlock) {







  return getControlType(block)







}















function optionItems(block: ProcessFormDesignBlock) {







  return getOptionItems(block)







}















function isVisible(block: ProcessFormDesignBlock) {







  if (!props.detailType) {







    return true







  }







  return isExpenseDetailBlockVisible(block, formData.value, props.detailType, props.defaultBusinessScenario, props.schema)







}















function isApprovalEditableBlock(block: ProcessFormDesignBlock) {

  if (!props.approvalEditMode) {

    return false

  }

  if (block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'payee-account') {

    return Boolean(props.allowEditPayAccount)

  }

  return Boolean(props.allowEditFormModule)

    && String(block.permission?.fixedStages?.IN_APPROVAL || 'READONLY') === 'EDITABLE'

}







function isReadOnly(block: ProcessFormDesignBlock) {

  if (props.approvalEditMode) {

    return !isApprovalEditableBlock(block)

  }

  return isExpenseDetailBlockReadOnly(block)

}















function placeholderOf(block: ProcessFormDesignBlock) {







  return String(block.props.placeholder || `请输入${block.label}`)







}















function businessCode(block: ProcessFormDesignBlock) {







  return getBusinessComponentDefinition(String(block.props.componentCode || ''))?.code || String(block.props.componentCode || '')







}















function findBusinessFieldKeys(code: string) {







  return blocks.value







    .filter((block) => block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === code)







    .map((block) => block.fieldKey)







}















function resolveSelectedPayeeName() {







  for (const fieldKey of payeeFieldKeys.value) {







    const value = normalizePayeeName(formData.value[fieldKey])







    if (value) {







      return value







    }







  }







  return ''







}















function resolveSelectedPaymentCompanyId() {







  for (const fieldKey of paymentCompanyFieldKeys.value) {







    const value = resolveLookupValue(formData.value[fieldKey])







    if (value) {







      return value







    }







  }







  return ''







}















function resolveSelectedCounterpartyCode() {







  for (const fieldKey of counterpartyFieldKeys.value) {







    const value = resolveLookupValue(formData.value[fieldKey])







    if (value) {







      return value







    }







  }







  return ''







}
















function syncLinkedLookupsFromExternalState() {
  const syncToken = ++linkedFieldResetSyncToken
  suppressLinkedFieldReset.value = true
  void nextTick(() => {
    if (syncToken !== linkedFieldResetSyncToken) {
      return
    }
    if (effectivePaymentCompanyId.value) {
      void loadVendorOptions('')
    } else {
      vendorOptions.value = []
    }
    void loadPayeeAccountOptions('', { settleAfterLoad: true })
    void nextTick(() => {
      if (syncToken === linkedFieldResetSyncToken) {
        suppressLinkedFieldReset.value = false
      }
    })
  })
}

function resolveCurrentCounterpartyOption() {
  const selectedCode = selectedCounterpartyCode.value
  if (!selectedCode) {
    return null
  }
  const existing = vendorOptions.value.find((item) => item.value === selectedCode)
  if (existing) {
    return existing
  }
  for (const fieldKey of counterpartyFieldKeys.value) {
    const rawValue = formData.value[fieldKey]
    const value = resolveLookupValue(rawValue)
    if (!value || value !== selectedCode) {
      continue
    }
    const label = isRecord(rawValue)
      ? firstNonEmptyString(rawValue.label, rawValue.name, rawValue.cVenName, rawValue.cVenAbbName, rawValue.value, rawValue.code)
      : value
    return {
      value: selectedCode,
      label: label || selectedCode,
      cVenCode: selectedCode,
      cVenName: label || selectedCode,
      cVenAbbName: label || selectedCode
    }
  }
  return {
    value: selectedCode,
    label: selectedCode,
    cVenCode: selectedCode,
    cVenName: selectedCode,
    cVenAbbName: selectedCode
  }
}

function mergeCurrentVendorOption(options: ExpenseCreateVendorOption[]) {
  const current = resolveCurrentCounterpartyOption()
  if (!current || options.some((item) => item.value === current.value)) {
    return options
  }
  return [current, ...options]
}

function resolveCurrentPayeeAccountSnapshot(): RuntimePayeeAccountSnapshot | null {
  for (const fieldKey of payeeAccountFieldKeys.value) {
    const rawValue = formData.value[fieldKey]
    const value = resolveLookupValue(rawValue)
    if (!value) {
      continue
    }
    if (isRecord(rawValue)) {
      return {
        value,
        label: firstNonEmptyString(rawValue.label, rawValue.accountName, rawValue.ownerName, rawValue.value) || value,
        sourceType: firstNonEmptyString(rawValue.sourceType) || '',
        ownerCode: firstNonEmptyString(rawValue.ownerCode) || undefined,
        ownerName: firstNonEmptyString(rawValue.ownerName) || undefined,
        accountName: firstNonEmptyString(rawValue.accountName) || undefined,
        accountNoMasked: firstNonEmptyString(rawValue.accountNoMasked) || undefined
      }
    }
    return {
      value,
      label: value,
      sourceType: ''
    }
  }
  return null
}

function buildPayeeAccountOptionFromSnapshot(snapshot: RuntimePayeeAccountSnapshot): ExpenseCreatePayeeAccountOption {
  return {
    value: snapshot.value,
    label: snapshot.label || snapshot.value,
    sourceType: snapshot.sourceType || '',
    ownerCode: snapshot.ownerCode || '',
    ownerName: snapshot.ownerName || snapshot.label || snapshot.value,
    accountName: snapshot.accountName,
    accountNoMasked: snapshot.accountNoMasked,
    secondaryLabel: firstNonEmptyString(snapshot.accountName, snapshot.accountNoMasked) || undefined
  }
}

function mergeCurrentPayeeAccountOption(options: ExpenseCreatePayeeAccountOption[]) {
  const current = resolveCurrentPayeeAccountSnapshot()
  if (!current || options.some((item) => item.value === current.value)) {
    return options
  }
  return [buildPayeeAccountOptionFromSnapshot(current), ...options]
}

function resolvePayeeAccountLinkageMode(): PayeeAccountLinkageMode {







  if (props.detailType === ENTERPRISE_DETAIL_TYPE || hasCounterpartyField.value) {







    return 'ENTERPRISE'







  }







  return 'EMPLOYEE'







}















function clearPayeeAccountSelections() {







  payeeAccountFieldKeys.value.forEach((fieldKey) => {







    formData.value[fieldKey] = ''







  })







}















function clearCounterpartySelections() {







  counterpartyFieldKeys.value.forEach((fieldKey) => {







    formData.value[fieldKey] = ''







  })







}















function isCounterpartyDisabled(block: ProcessFormDesignBlock) {







  return isReadOnly(block) || !effectivePaymentCompanyId.value







}















function isPayeeAccountDisabled(block: ProcessFormDesignBlock) {







  if (isReadOnly(block)) {







    return true







  }







  if (payeeAccountLinkageMode.value === 'ENTERPRISE') {







    return !effectivePaymentCompanyId.value || !selectedCounterpartyCode.value







  }







  return false







}















function buildPayeeSnapshot(option: ExpenseCreatePayeeOption): RuntimePayeeSnapshot {







  return {







    value: option.value,







    label: option.label,







    sourceType: option.sourceType,







    sourceCode: option.sourceCode







  }







}















function buildPayeeAccountSnapshot(option: ExpenseCreatePayeeAccountOption): RuntimePayeeAccountSnapshot {







  return {







    value: option.value,







    label: option.label,







    sourceType: option.sourceType,







    ownerCode: option.ownerCode,







    ownerName: option.ownerName,







    accountName: option.accountName,







    accountNoMasked: option.accountNoMasked,







    bankName: option.bankName







  }







}















function clearActiveFieldInteraction() {
  if (typeof document === 'undefined') {
    return
  }
  const activeElement = document.activeElement
  if (activeElement instanceof HTMLElement) {
    activeElement.blur()
  }
}

function setCounterpartySelectRef(fieldKey: string, instance: unknown) {
  counterpartySelectRefs.value[fieldKey] = instance && typeof instance === 'object'
    ? instance as FocusManagedSelectInstance
    : null
}

function setPayeeAccountSelectRef(fieldKey: string, instance: unknown) {
  payeeAccountSelectRefs.value[fieldKey] = instance && typeof instance === 'object'
    ? instance as FocusManagedSelectInstance
    : null
}

function clearManagedSelectQuery(instance: FocusManagedSelectInstance | null | undefined) {
  if (!instance) {
    return
  }
  if (typeof instance.handleQueryChange === 'function') {
    instance.handleQueryChange('')
  }
  if (typeof instance.query === 'string') {
    instance.query = ''
  }
  if (typeof instance.previousQuery === 'string') {
    instance.previousQuery = ''
  }
  if (instance.states && typeof instance.states === 'object' && 'inputValue' in instance.states) {
    instance.states.inputValue = ''
  }
  const inputElement = resolveManagedSelectInputElement(instance)
  if (inputElement) {
    inputElement.value = ''
  }
}

function resolveManagedSelectInputElement(instance: FocusManagedSelectInstance | null | undefined) {
  if (!instance) {
    return null
  }
  if (instance.inputRef?.input instanceof HTMLInputElement) {
    return instance.inputRef.input
  }
  if (instance.$el instanceof HTMLElement) {
    const inputElement = instance.$el.querySelector('input')
    if (inputElement instanceof HTMLInputElement) {
      return inputElement
    }
  }
  return null
}

function closeManagedSelect(instance: FocusManagedSelectInstance | null | undefined) {
  if (!instance) {
    return
  }
  clearManagedSelectQuery(instance)
  if (typeof instance.handleClose === 'function') {
    instance.handleClose()
  } else if (instance.expanded && typeof instance.toggleMenu === 'function') {
    instance.toggleMenu()
  }
  const inputElement = resolveManagedSelectInputElement(instance)
  if (inputElement) {
    inputElement.blur()
    inputElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
  }
  if (instance.inputRef && typeof instance.inputRef.blur === 'function') {
    instance.inputRef.blur()
  }
  if (typeof instance.blur === 'function') {
    instance.blur()
  }
}

function settleLinkedSelectInteractions() {
  Object.values(counterpartySelectRefs.value).forEach((instance) => closeManagedSelect(instance))
  Object.values(payeeAccountSelectRefs.value).forEach((instance) => closeManagedSelect(instance))
  clearActiveFieldInteraction()
}

function prepareDocumentPickerOpen() {
    if (suppressLinkedFieldReset.value) {
      void loadPayeeAccountOptions('', { settleAfterLoad: true })
      return
    }
  settleLinkedSelectInteractions()
}

function handleCounterpartySelection(fieldKey: string, value: string | '' | null | undefined) {
  formData.value[fieldKey] = value || ''
  settleLinkedSelectInteractions()
  void nextTick(() => settleLinkedSelectInteractions())
}

function handlePayeeSelection(fieldKey: string, value: RuntimePayeeSnapshot | '' | null | undefined) {







  formData.value[fieldKey] = value || ''







}















function handlePayeeAccountSelection(fieldKey: string, value: RuntimePayeeAccountSnapshot | '' | null | undefined) {







  formData.value[fieldKey] = value || ''







}















function departmentLabel(value: string) {







  return departmentOptions.value.find((item) => item.value === value)?.label || value







}















function sharedArchiveItems(block: ProcessFormDesignBlock) {







  return sharedArchiveMap.value.get(getSharedArchiveCode(block))?.items || []







}















const documentPickerTitle = computed(() => documentPickerDialog.relationType === 'WRITEOFF' ? '选择核销单据' : '选择关联单据')

function resolveDocumentPickerActiveTemplateType(groups: ExpenseDocumentPickerGroup[]) {
  if (!groups.length) {
    return ''
  }
  if (groups.some((group) => group.templateType === documentPickerDialog.activeTemplateType)) {
    return documentPickerDialog.activeTemplateType
  }
  const selectedGroup = groups.find((group) =>
    group.items.some((item) => documentPickerDialog.selectedCodes.includes(item.documentCode))
  )
  return selectedGroup?.templateType || groups[0]?.templateType || ''
}

function isRelatedDocumentBlock(block: ProcessFormDesignBlock) {







  return block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'related-document'







}















function isWriteOffDocumentBlock(block: ProcessFormDesignBlock) {







  return block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'writeoff-document'







}















function isDocumentBusinessBlock(block: ProcessFormDesignBlock) {







  return isRelatedDocumentBlock(block) || isWriteOffDocumentBlock(block)







}















function documentBlockHint(block: ProcessFormDesignBlock) {







  return isWriteOffDocumentBlock(block)







    ? '支持点击页签切换报销单与借款单，选中后逐条填写本次核销金额。'







    : '支持点击页签切换报销单、申请单、合同单与借款单，并同时关联多张已审批通过的单据。'







}















function documentRelationType(block: ProcessFormDesignBlock): DocumentRelationType {







  return isWriteOffDocumentBlock(block) ? 'WRITEOFF' : 'RELATED'







}















function documentAllowedTemplateTypes(block: ProcessFormDesignBlock) {







  return normalizeBusinessComponentAllowedTemplateTypes(







    businessCode(block),







    block.props.allowedTemplateTypes







  )







}















function documentRecords(block: ProcessFormDesignBlock) {







  const rawValue = formData.value[block.fieldKey]







  return isWriteOffDocumentBlock(block)







    ? normalizeWriteOffDocumentValues(rawValue)







    : normalizeRelatedDocumentValues(rawValue)







}















function openDocumentPicker(block: ProcessFormDesignBlock) {
  prepareDocumentPickerOpen()
  documentPickerDialog.visible = true
  documentPickerDialog.fieldKey = block.fieldKey
  documentPickerDialog.relationType = documentRelationType(block)
  documentPickerDialog.keyword = ''
  documentPickerDialog.groups = []
  documentPickerDialog.activeTemplateType = ''
  documentPickerDialog.selectedCodes = []
  documentPickerDialog.itemsByCode = {}

  documentRecords(block).forEach((item) => {
    if (!item.documentCode) {
      return
    }
    documentPickerDialog.selectedCodes.push(item.documentCode)
    documentPickerDialog.itemsByCode[item.documentCode] = cloneDocumentRecord(item)
  })

  void nextTick(() => loadDocumentPicker())
}

function closeDocumentPicker() {
  documentPickerDialog.visible = false
  documentPickerDialog.fieldKey = ''
  documentPickerDialog.keyword = ''
  documentPickerDialog.groups = []
  documentPickerDialog.activeTemplateType = ''
  documentPickerDialog.selectedCodes = []
  documentPickerDialog.itemsByCode = {}
}

async function loadDocumentPicker() {
  if (!documentPickerDialog.fieldKey) {
    return
  }
  const block = blocks.value.find((item) => item.fieldKey === documentPickerDialog.fieldKey)
  if (!block) {
    return
  }
  documentPickerDialog.loading = true
  try {
    const res = await expenseApi.getDocumentPicker({
      relationType: documentPickerDialog.relationType,
      templateTypes: documentAllowedTemplateTypes(block),
      keyword: documentPickerDialog.keyword || undefined
    })
    documentPickerDialog.groups = res.data.groups || []
    documentPickerDialog.activeTemplateType = resolveDocumentPickerActiveTemplateType(documentPickerDialog.groups)
    documentPickerDialog.groups.forEach((group) => {
      group.items.forEach((item) => {
        documentPickerDialog.itemsByCode[item.documentCode] = mergeDocumentRecord(
          documentPickerDialog.itemsByCode[item.documentCode],
          toDocumentRecord(item)
        )
      })
    })
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载可选单据失败'))
  } finally {
    documentPickerDialog.loading = false
  }
}

function isDocumentSelected(documentCode: string) {







  return documentPickerDialog.selectedCodes.includes(documentCode)







}















function selectedGroupCount(group: ExpenseDocumentPickerGroup) {







  return group.items.filter((item) => isDocumentSelected(item.documentCode)).length







}















function toggleDocumentSelection(item: ExpenseDocumentPickerItem) {







  const existingIndex = documentPickerDialog.selectedCodes.indexOf(item.documentCode)







  if (existingIndex >= 0) {







    documentPickerDialog.selectedCodes.splice(existingIndex, 1)







    return







  }







  documentPickerDialog.selectedCodes.push(item.documentCode)







  documentPickerDialog.itemsByCode[item.documentCode] = mergeDocumentRecord(







    documentPickerDialog.itemsByCode[item.documentCode],







    toDocumentRecord(item)







  )







}















function confirmDocumentPicker() {







  const block = blocks.value.find((item) => item.fieldKey === documentPickerDialog.fieldKey)







  if (!block) {







    closeDocumentPicker()







    return







  }















  const nextRecords = documentPickerDialog.selectedCodes







    .map((documentCode) => documentPickerDialog.itemsByCode[documentCode])







    .filter((item): item is RuntimeDocumentRecord => Boolean(item?.documentCode))







    .map((item) => cloneDocumentRecord(item))















  formData.value[block.fieldKey] = isWriteOffDocumentBlock(block)







    ? nextRecords.map((item) => toWriteOffDocumentValue(item))







    : nextRecords.map((item) => toRelatedDocumentValue(item))







  closeDocumentPicker()







}















function removeDocumentRecord(block: ProcessFormDesignBlock, documentCode: string) {







  const next = documentRecords(block).filter((item) => item.documentCode !== documentCode)







  formData.value[block.fieldKey] = isWriteOffDocumentBlock(block)







    ? next.map((item) => toWriteOffDocumentValue(item))







    : next.map((item) => toRelatedDocumentValue(item))







}















function updateWriteOffAmount(block: ProcessFormDesignBlock, documentCode: string, value: string) {







  const next = normalizeWriteOffDocumentValues(formData.value[block.fieldKey]).map((item) => {







    if (item.documentCode !== documentCode) {







      return item







    }







    const writeOffAmount = toOptionalMoney(value)







    const availableAmount = item.availableWriteOffAmount







    return {







      ...item,







      writeOffAmount,







      remainingAmount: !availableAmount || !writeOffAmount







        ? undefined







        : compareMoney(availableAmount, writeOffAmount) >= 0







          ? subtractMoney(availableAmount, writeOffAmount)







          : '0.00'







    }







  })







  formData.value[block.fieldKey] = next.map((item) => toWriteOffDocumentValue(item))







}















function normalizeRelatedDocumentValues(value: unknown): RuntimeDocumentRecord[] {







  const records = normalizeDocumentValueList(value)







  return records







    .map((item: Record<string, unknown>) => ({







      documentCode: firstNonBlank(item.documentCode, item.value) || '',







      documentTitle: firstNonBlank(item.documentTitle, item.label),







      templateType: firstNonBlank(item.templateType),







      templateTypeLabel: firstNonBlank(item.templateTypeLabel),







      templateName: firstNonBlank(item.templateName),







      status: firstNonBlank(item.status),







      statusLabel: firstNonBlank(item.statusLabel)







    }))







    .filter((item: RuntimeDocumentRecord) => Boolean(item.documentCode))







}















function normalizeWriteOffDocumentValues(value: unknown): RuntimeDocumentRecord[] {







  const records = normalizeDocumentValueList(value)







  return records







    .map((item: Record<string, unknown>) => {







      const availableWriteOffAmount = toOptionalMoney(item.availableWriteOffAmount)







      const writeOffAmount = toOptionalMoney(item.writeOffAmount)







      return {







        documentCode: firstNonBlank(item.documentCode, item.value) || '',







        documentTitle: firstNonBlank(item.documentTitle, item.label),







        templateType: firstNonBlank(item.templateType),







        templateTypeLabel: firstNonBlank(item.templateTypeLabel),







        templateName: firstNonBlank(item.templateName),







        status: firstNonBlank(item.status),







        statusLabel: firstNonBlank(item.statusLabel),







        writeOffSourceKind: firstNonBlank(item.writeOffSourceKind),







        availableWriteOffAmount,







        writeOffAmount,







        remainingAmount: toOptionalMoney(item.remainingAmount) ?? (







          !availableWriteOffAmount || !writeOffAmount







            ? undefined







            : compareMoney(availableWriteOffAmount, writeOffAmount) >= 0







              ? subtractMoney(availableWriteOffAmount, writeOffAmount)







              : '0.00'







        )







      }







    })







    .filter((item: RuntimeDocumentRecord) => Boolean(item.documentCode))







}















function normalizeDocumentValueList(value: unknown): Record<string, unknown>[] {







  if (Array.isArray(value)) {







    return value.flatMap((item) => normalizeDocumentValueList(item))







  }







  if (value && typeof value === 'object') {







    return [value as Record<string, unknown>]







  }







  return []







}















function toDocumentRecord(item: ExpenseDocumentPickerItem): RuntimeDocumentRecord {







  return {







    documentCode: item.documentCode,







    documentTitle: item.documentTitle,







    templateType: item.templateType,







    templateTypeLabel: item.templateTypeLabel,







    templateName: item.templateName,







    status: item.status,







    statusLabel: item.statusLabel,







    writeOffSourceKind: item.writeOffSourceKind,







    availableWriteOffAmount: item.availableWriteOffAmount







  }







}















function mergeDocumentRecord(current: RuntimeDocumentRecord | undefined, next: RuntimeDocumentRecord) {







  if (!current) {







    return cloneDocumentRecord(next)







  }







  return {







    ...current,







    ...next,







    writeOffAmount: current.writeOffAmount ?? next.writeOffAmount,







    remainingAmount: current.remainingAmount ?? next.remainingAmount







  }







}















function cloneDocumentRecord(item: RuntimeDocumentRecord): RuntimeDocumentRecord {







  return { ...item }







}















function toRelatedDocumentValue(item: RuntimeDocumentRecord): ExpenseRelatedDocumentValue {







  return {







    documentCode: item.documentCode,







    documentTitle: item.documentTitle,







    templateType: item.templateType,







    templateTypeLabel: item.templateTypeLabel,







    templateName: item.templateName,







    status: item.status,







    statusLabel: item.statusLabel







  }







}















function toWriteOffDocumentValue(item: RuntimeDocumentRecord): ExpenseWriteOffDocumentValue {







  return {







    ...toRelatedDocumentValue(item),







    writeOffSourceKind: item.writeOffSourceKind,







    availableWriteOffAmount: item.availableWriteOffAmount,







    writeOffAmount: item.writeOffAmount,







    remainingAmount: item.remainingAmount







  }







}















function resolveTemplateTypeLabel(templateType?: string) {







  if (templateType === 'application') return '申请单'







  if (templateType === 'contract') return '合同单'







  if (templateType === 'loan') return '借款单'







  return '报销单'







}















function writeOffSourceKindLabel(value?: string) {







  if (value === 'LOAN') return '借款单'







  if (value === 'PREPAY_REPORT') return '预付报销单'







  return '待识别'







}















function formatAmount(value: unknown) {







  const amount = toOptionalMoney(value)







  if (amount === undefined) {







    return '--'







  }







  return formatMoney(amount)







}















async function loadVendorOptions(keyword: string) {







  if (!effectivePaymentCompanyId.value) {







    vendorOptions.value = []







    return







  }







  vendorOptionsLoading.value = true







  try {







    const res = await expenseCreateApi.listVendorOptions({







      keyword: keyword || undefined,







      paymentCompanyId: effectivePaymentCompanyId.value







    })







    vendorOptions.value = res.data







  } catch (error: unknown) {







    ElMessage.error(resolveErrorMessage(error, '加载收款单位失败'))







  } finally {







    vendorOptionsLoading.value = false







  }







}















async function loadPayeeOptions(keyword: string) {







  payeeOptionsLoading.value = true







  try {







    const res = await expenseCreateApi.listPayeeOptions({







      keyword,







      personalOnly: true







    })







    payeeOptions.value = res.data







  } catch (error: unknown) {







    ElMessage.error(resolveErrorMessage(error, LOAD_PAYEE_ERROR))







  } finally {







    payeeOptionsLoading.value = false







  }







}















async function loadPayeeAccountOptions(keyword: string, options?: { settleAfterLoad?: boolean }) {







  if (payeeAccountLinkageMode.value === 'ENTERPRISE') {







    if (!effectivePaymentCompanyId.value || !selectedCounterpartyCode.value) {







      payeeAccountOptions.value = []
      if (options?.settleAfterLoad) {
        settleLinkedSelectInteractions()
      }







      return







    }







  }







  payeeAccountOptionsLoading.value = true







  try {







    const res = await expenseCreateApi.listPayeeAccountOptions({







      keyword,







      linkageMode: payeeAccountLinkageMode.value,







      payeeName: selectedPayeeName.value || undefined,







      counterpartyCode: selectedCounterpartyCode.value || undefined,







      paymentCompanyId: effectivePaymentCompanyId.value || undefined







    })







    payeeAccountOptions.value = res.data







  } catch (error: unknown) {







    ElMessage.error(resolveErrorMessage(error, LOAD_PAYEE_ACCOUNT_ERROR))







  } finally {







    payeeAccountOptionsLoading.value = false
    if (options?.settleAfterLoad) {
      settleLinkedSelectInteractions()
      void nextTick(() => settleLinkedSelectInteractions())
    }







  }







}















function normalizePayeeName(value: unknown) {







  if (!value) {







    return ''







  }







  if (typeof value === 'string') {







    return value.startsWith(PERSONAL_PAYEE_PREFIX) ? value.slice(PERSONAL_PAYEE_PREFIX.length) : value







  }







  if (isRecord(value)) {







    const label = firstNonEmptyString(value.label, value.sourceCode)







    if (label) {







      return label.startsWith(PERSONAL_PAYEE_PREFIX) ? label.slice(PERSONAL_PAYEE_PREFIX.length) : label







    }







    const rawValue = firstNonEmptyString(value.value)







    if (rawValue) {







      return rawValue.startsWith(PERSONAL_PAYEE_PREFIX) ? rawValue.slice(PERSONAL_PAYEE_PREFIX.length) : rawValue







    }







  }







  return ''







}















function resolveLookupValue(value: unknown) {







  if (typeof value === 'string') {







    return value







  }







  if (isRecord(value)) {







    return firstNonEmptyString(value.value, value.code, value.id)







  }







  return ''







}















function uploadFileList(block: ProcessFormDesignBlock): UploadUserFile[] {







  return normalizeAttachments(formData.value[block.fieldKey]).map((item, index) => ({







    name: item.fileName,







    status: 'success',







    uid: index + 1







  }))







}















function uploadAccept(block: ProcessFormDesignBlock) {







  const accept = String(block.props.accept || '').trim()







  return accept || undefined







}















async function handleFileChange(block: ProcessFormDesignBlock, uploadFile: UploadFile) {







  if (!uploadFile.raw) {







    return







  }















  if (isInvoiceAttachmentBlock(block) && !isAllowedInvoiceAttachmentFile(uploadFile.raw)) {







    ElMessage.warning(INVOICE_ATTACHMENT_INVALID_MESSAGE)







    return







  }















  try {







    const uploadRes = await expenseCreateApi.uploadAttachment(uploadFile.raw)
    const uploadedAttachment = attachResolvedOcr(
      uploadRes.data,
      isInvoiceAttachmentBlock(block)
        ? await resolveAttachmentOcrSnapshot(uploadRes.data)
        : undefined
    )
    const current = normalizeAttachments(formData.value[block.fieldKey])
    const nextAttachments = [...current, uploadedAttachment]
    formData.value[block.fieldKey] = nextAttachments
    syncInvoiceAmountFromAttachments(block, current, nextAttachments)







  } catch (error: unknown) {







    ElMessage.error(resolveErrorMessage(error, '附件上传失败'))







  }







}















async function resolveAttachmentOcrSnapshot(attachment: ExpenseAttachmentMeta): Promise<ExpenseAttachmentMeta['ocr']> {
  const attachmentId = typeof attachment.attachmentId === 'string' ? attachment.attachmentId.trim() : ''
  if (!attachmentId) {
    return {
      status: 'FAILED',
      message: '附件上传成功，但未返回附件标识'
    }
  }

  try {
    const res = await expenseCreateApi.recognizeAttachmentOcr(attachmentId)
    return normalizeOcrSnapshot(res.data)
  } catch (error: unknown) {
    return {
      status: 'FAILED',
      message: resolveErrorMessage(error, 'OCR 识别失败，请稍后重试')
    }
  }
}

function attachResolvedOcr(
  attachment: ExpenseAttachmentMeta,
  snapshot?: ExpenseAttachmentMeta['ocr']
): ExpenseAttachmentMeta {
  if (!snapshot) {
    return attachment
  }
  return {
    ...attachment,
    ocr: snapshot
  }
}

function syncInvoiceAmountFromAttachments(
  block: ProcessFormDesignBlock,
  previousAttachments: ExpenseAttachmentMeta[],
  nextAttachments: ExpenseAttachmentMeta[]
) {
  if (!isInvoiceAttachmentBlock(block) || block.fieldKey !== FIELD_INVOICE_ATTACHMENTS) {
    return
  }
  syncInvoiceAmountWithOcr(
    formData.value,
    resolveInvoiceOcrTotal(previousAttachments),
    resolveInvoiceOcrTotal(nextAttachments)
  )
}

function handleFileRemove(block: ProcessFormDesignBlock, uploadFile: UploadFile) {
  const current = normalizeAttachments(formData.value[block.fieldKey])
  const nextAttachments = current.filter((item, index) => {
    const fallbackUid = `legacy-${block.fieldKey}-${index}-${item.fileName}`
    const currentUid = item.attachmentId || fallbackUid
    if (uploadFile.uid !== undefined && String(uploadFile.uid) === currentUid) {
      return false
    }
    return item.fileName !== uploadFile.name
  })
  formData.value[block.fieldKey] = nextAttachments
  syncInvoiceAmountFromAttachments(block, current, nextAttachments)
}

function isInvoiceAttachmentBlock(block: ProcessFormDesignBlock) {







  if (block.fieldKey === FIELD_INVOICE_ATTACHMENTS) {







    return true







  }







  const tokens = normalizeAcceptTokens(block.props.accept)







  return tokens.length > 0







    && tokens.every((token) => INVOICE_ATTACHMENT_ALLOWED_EXTENSIONS.has(token))







    && tokens.includes('.pdf')







}















function isAllowedInvoiceAttachmentFile(file: File) {







  const mimeType = String(file.type || '').trim().toLowerCase()







  if (mimeType && INVOICE_ATTACHMENT_ALLOWED_MIME_TYPES.has(mimeType)) {







    return true







  }







  const extension = resolveFileExtension(file.name)







  return Boolean(extension && INVOICE_ATTACHMENT_ALLOWED_EXTENSIONS.has(extension))







}















function normalizeAcceptTokens(value: unknown) {







  return String(value || '')







    .split(',')







    .map((item) => item.trim().toLowerCase())







    .filter(Boolean)







}















function resolveFileExtension(fileName: string) {







  const normalized = String(fileName || '').trim().toLowerCase()







  const dotIndex = normalized.lastIndexOf('.')







  if (dotIndex < 0) {







    return ''







  }







  return normalized.slice(dotIndex)







}















function openVendorDialog(fieldKey = '') {







  if (!effectivePaymentCompanyId.value) {







    ElMessage.warning('请先选择付款公司')







    return







  }







  vendorDialogMode.value = 'create'







  vendorDialogFieldKey.value = fieldKey







  vendorDialogVendorCode.value = ''







  Object.assign(vendorDraft, emptyVendorDraft())







  vendorDialogVisible.value = true







}















function closeVendorDialog() {







  vendorDialogVisible.value = false







  vendorDialogFieldKey.value = ''







  vendorDialogVendorCode.value = ''







  vendorDialogMode.value = 'create'







  vendorDialogLoading.value = false







  Object.assign(vendorDraft, emptyVendorDraft())







}















async function openVendorAccountDialog() {







  if (!effectivePaymentCompanyId.value) {







    ElMessage.warning('请先选择付款公司')







    return







  }







  if (!selectedCounterpartyCode.value) {







    ElMessage.warning('请先选择收款单位')







    return







  }







  vendorDialogMode.value = 'edit'







  vendorDialogFieldKey.value = ''







  vendorDialogVendorCode.value = selectedCounterpartyCode.value







  vendorDialogLoading.value = true







  vendorDialogVisible.value = true







  try {







    const res = await expenseCreateApi.getVendorDetail(effectivePaymentCompanyId.value, selectedCounterpartyCode.value)







    hydrateVendorDraft(res.data)







  } catch (error: unknown) {







    vendorDialogVisible.value = false







    ElMessage.error(resolveErrorMessage(error, '加载供应商信息失败'))







  } finally {







    vendorDialogLoading.value = false







  }







}















function validateVendorDraft() {







  const incompleteBankDirectoryMessage = '请选择开户银行、开户省、开户市与开户网点后再保存'







  if (!String(vendorDraft.cVenName || '').trim()) {







    return '请输入供应商名称'







  }







  if (!String(vendorDraft.receiptAccountName || vendorDraft.cVenName || '').trim()) {







    return '请输入账户名'







  }







  if (!String(vendorDraft.cVenAccount || '').trim()) {







    return '请先填写银行账号'







  }







  if (







    !String(vendorDraft.cVenBank || '').trim()







    || !String(vendorDraft.cVenBankCode || '').trim()







    || !String(vendorDraft.receiptBankProvince || '').trim()







    || !String(vendorDraft.receiptBankCity || '').trim()







    || !String(vendorDraft.receiptBranchName || '').trim()







    || !String(vendorDraft.receiptBranchCode || '').trim()







  ) {







    return incompleteBankDirectoryMessage







  }







  const lengthRules = [







    { key: 'cVenName', label: '供应商名称', max: 128 },







    { key: 'cVenAbbName', label: '供应商简称', max: 64 },







    { key: 'cVenPerson', label: '联系人', max: 64 },







    { key: 'cVenPhone', label: '联系电话', max: 32 },







    { key: 'receiptAccountName', label: '账户名', max: 128 },







    { key: 'cVenBank', label: '开户银行', max: 128 },







    { key: 'cVenAccount', label: '银行账号', max: 64 },







    { key: 'receiptBranchName', label: '开户网点', max: 128 }







  ] as const







  for (const rule of lengthRules) {







    const value = String(vendorDraft[rule.key] || '').trim()







    if (value.length > rule.max) {







      return `${rule.label}最多 ${rule.max} 个字符`







    }







  }







  return ''







}















function hydrateVendorDraft(detail: Partial<FinanceVendorDetail>) {







  Object.assign(vendorDraft, emptyVendorDraft(), {







    cVenName: detail.cVenName || '',







    cVenAbbName: detail.cVenAbbName || '',







    cVenRegCode: detail.cVenRegCode || '',







    cVenPerson: detail.cVenPerson || '',







    cVenPhone: detail.cVenPhone || '',







    cVenAddress: detail.cVenAddress || '',







    receiptAccountName: detail.receiptAccountName || '',







    cVenBankCode: detail.cVenBankCode || '',







    cVenBank: detail.cVenBank || '',







    receiptBankProvince: detail.receiptBankProvince || '',







    receiptBankCity: detail.receiptBankCity || '',







    receiptBranchCode: detail.receiptBranchCode || '',







    receiptBranchName: detail.receiptBranchName || '',







    cVenAccount: detail.cVenAccount || '',







    cMemo: detail.cMemo || ''







  })







}















function buildVendorCreatePayload() {







  return Object.fromEntries(







    Object.entries(vendorDraft).filter(([, value]) => value !== undefined && value !== null && value !== '')







  ) as FinanceVendorSavePayload







}















function assignFirstPayeeAccountOption() {







  const firstOption = payeeAccountOptions.value[0]







  if (!firstOption) {







    return







  }







  const snapshot = buildPayeeAccountSnapshot(firstOption)







  payeeAccountFieldKeys.value.forEach((fieldKey) => {







    formData.value[fieldKey] = snapshot







  })







}















async function saveVendor() {







  const validationMessage = validateVendorDraft()







  if (validationMessage) {







    ElMessage.warning(validationMessage)







    return







  }







  vendorSaving.value = true







  try {







    if (!effectivePaymentCompanyId.value) {







      ElMessage.warning('请先选择付款公司')







      return







    }







    if (vendorDialogMode.value === 'edit') {







      const vendorCode = vendorDialogVendorCode.value || selectedCounterpartyCode.value







      if (!vendorCode) {







        ElMessage.warning('请先选择收款单位')







        return







      }







      await expenseCreateApi.updateVendor(effectivePaymentCompanyId.value, vendorCode, { ...vendorDraft })







      await loadVendorOptions(String(vendorDraft.cVenName || vendorCode))







      await loadPayeeAccountOptions('')














      ElMessage.success('供应商收款信息已更新')







    } else {







      const res = await expenseCreateApi.createVendor(effectivePaymentCompanyId.value, buildVendorCreatePayload())







      await loadVendorOptions(String(res.data.cVenName || res.data.cVenCode || ''))







      if (vendorDialogFieldKey.value) {







        formData.value[vendorDialogFieldKey.value] = res.data.cVenCode







      }







      clearPayeeAccountSelections()














      ElMessage.success('供应商及收款信息已保存')







    }







    closeVendorDialog()







  } catch (error: unknown) {







    ElMessage.error(resolveErrorMessage(
      error,
      vendorDialogMode.value === 'edit' ? '维护供应商收款信息失败' : '新增供应商及收款信息失败'
    ))







  } finally {







    vendorSaving.value = false







  }







}















function resolveErrorMessage(error: unknown, fallback: string) {







  return error instanceof Error && error.message ? error.message : fallback







}















function normalizeAttachments(value: unknown): ExpenseAttachmentMeta[] {







  if (Array.isArray(value)) {







    return value.flatMap((item) => normalizeAttachments(item))







  }















  if (typeof value === 'string') {







    const fileName = value.trim()







    return fileName ? [{ fileName }] : []







  }















  if (value && typeof value === 'object') {







    const record = value as Record<string, unknown>







    const fileName = firstNonBlank(record.fileName, record.name, record.label, record.value, record.url)







    if (!fileName) {







      return []







    }







    return [{







      attachmentId: firstNonBlank(record.attachmentId, record.id),







      fileName,







      contentType: firstNonBlank(record.contentType, record.mimeType, record.type),







      fileSize: toOptionalNumber(record.fileSize, record.size),







      previewUrl: firstNonBlank(record.previewUrl, record.fileUrl, record.url),
      ocr: normalizeOcrSnapshot(record.ocr)







    }]







  }















  return []







}

function normalizeOcrSnapshot(value: unknown): ExpenseAttachmentMeta['ocr'] {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined
  }

  const record = value as Record<string, unknown>
  const status = firstNonBlank(record.status)
  if (!status) {
    return undefined
  }

  return {
    status,
    providerCode: firstNonBlank(record.providerCode),
    providerName: firstNonBlank(record.providerName),
    requestId: firstNonBlank(record.requestId),
    recognizedAt: firstNonBlank(record.recognizedAt),
    invoiceCode: firstNonBlank(record.invoiceCode),
    invoiceNumber: firstNonBlank(record.invoiceNumber),
    invoiceDate: firstNonBlank(record.invoiceDate),
    invoiceType: firstNonBlank(record.invoiceType),
    sellerName: firstNonBlank(record.sellerName),
    totalAmount: toOptionalNumber(record.totalAmount),
    taxAmount: toOptionalNumber(record.taxAmount),
    message: firstNonBlank(record.message)
  }
}















function firstNonBlank(...values: unknown[]) {







  for (const value of values) {







    if (typeof value === 'string' && value.trim()) {







      return value.trim()







    }







  }







  return undefined







}















function firstNonEmptyString(...values: unknown[]) {







  return firstNonBlank(...values) || ''







}















function isRecord(value: unknown): value is Record<string, any> {







  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)







}















function toOptionalNumber(...values: unknown[]) {







  for (const value of values) {







    if (typeof value === 'number' && Number.isFinite(value)) {







      return value







    }







    if (typeof value === 'string' && value.trim()) {







      const parsed = Number(value)







      if (Number.isFinite(parsed)) {







        return parsed







      }







    }







  }







  return undefined







}















function toOptionalMoney(value: unknown) {







  if (typeof value === 'number' && Number.isFinite(value)) {







    return normalizeMoneyValue(String(value))







  }







  if (typeof value === 'string' && value.trim()) {







    return normalizeMoneyValue(value)







  }







  return undefined







}















function toOptionalString(value: unknown) {







  if (typeof value === 'string') {







    return value







  }







  if (typeof value === 'number' && Number.isFinite(value)) {







    return String(value)







  }







  return undefined







}







</script>















<style scoped>
:deep(.document-picker-tabs .el-tabs__header) {
  margin-bottom: 16px;
}

:deep(.document-picker-tabs .el-tabs__nav-wrap::after) {
  opacity: 0;
}

:deep(.document-picker-tabs .el-tabs__item) {
  height: 40px;
  border-radius: 999px;
  padding: 0 16px;
  color: #475569;
}

:deep(.document-picker-tabs .el-tabs__item.is-active) {
  color: #0369a1;
}

:deep(.document-picker-tabs .el-tabs__active-bar) {
  background-color: #0ea5e9;
}

:deep(.expense-runtime-control) {







  width: 100%;







}















:deep(.expense-runtime-control .el-input__wrapper),







:deep(.expense-runtime-control .el-select__wrapper),







:deep(.expense-runtime-control.el-date-editor.el-input__wrapper),







:deep(.expense-runtime-control.el-date-editor--daterange) {







  min-height: 40px;







}















:deep(.expense-runtime-control .el-select__selection),







:deep(.expense-runtime-control .el-input__inner),







:deep(.expense-runtime-control.el-date-editor .el-range-input),







:deep(.expense-runtime-control.el-date-editor .el-range-separator) {







  min-height: 38px;







  line-height: 38px;







  align-items: center;







}















:deep(.expense-runtime-control.el-input-number) {







  width: 100%;







}















:deep(.expense-runtime-control.el-input-number .el-input__wrapper) {







  min-height: 40px;







}







</style>














