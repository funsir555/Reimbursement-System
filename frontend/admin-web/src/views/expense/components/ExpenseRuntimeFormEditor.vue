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

            @update:model-value="handleAmountInput(block, $event)"

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
            :ref="(instance: unknown) => setCounterpartySelectRef(block.fieldKey, instance)"

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
            :ref="(instance: unknown) => setPayeeAccountSelectRef(block.fieldKey, instance)"

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
            @visible-change="handlePayeeAccountDropdownVisibleChange"

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

            <template v-if="showVendorAccountMaintenanceEntry" #empty>
              <div class="space-y-3 px-3 py-4 text-center">
                <p class="text-sm text-slate-500">
                  {{ MISSING_VENDOR_BANK_INFO_MESSAGE }}
                </p>
                <button
                  type="button"
                  data-testid="payee-account-maintain-vendor"
                  class="flex w-full items-center justify-center rounded-xl border border-dashed border-sky-200 bg-sky-50 px-3 py-2 text-sm font-medium text-sky-700 transition hover:border-sky-300 hover:bg-sky-100"
                  :disabled="isReadOnly(block)"
                  @click.stop="openVendorAccountDialog"
                >
                  新增银行账户
                </button>
              </div>
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

import { computed, ref } from 'vue'
import {
  type ProcessCustomArchiveDetail,
  type ProcessFormDesignSchema,
  type ProcessFormOption
} from '@/api'
import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useExpenseRuntimeAttachmentOcr } from '@/views/expense/components/composables/useExpenseRuntimeAttachmentOcr'
import { useExpenseRuntimeBlockRuntime } from '@/views/expense/components/composables/useExpenseRuntimeBlockRuntime'
import { useExpenseRuntimeDocumentPicker } from '@/views/expense/components/composables/useExpenseRuntimeDocumentPicker'
import { useExpenseRuntimePageUtils } from '@/views/expense/components/composables/useExpenseRuntimePageUtils'
import { useExpenseRuntimePaymentCounterparty } from '@/views/expense/components/composables/useExpenseRuntimePaymentCounterparty'
import { ensureExpenseDetailFormDefaults } from '@/views/expense/expenseDetailRuntime'

const formData = defineModel<Record<string, unknown>>({ required: true })

const props = withDefaults(defineProps<{

  schema: ProcessFormDesignSchema

  sharedArchives?: ProcessCustomArchiveDetail[]

  companyOptions?: ProcessFormOption[]

  departmentOptions?: ProcessFormOption[]

  currentUserCompanyId?: string

  hydratingForm?: boolean

  hydrationVersion?: number

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

  hydratingForm: false,

  hydrationVersion: 0,

  approvalEditMode: false,

  allowEditFormModule: false,

  allowEditPayAccount: false,

  detailType: '',

  defaultBusinessScenario: ''

})

const {
  blocks,
  visibleBlocks,
  companyOptions,
  departmentOptions,
  controlType,
  optionItems,
  documentTitleMaxLength,
  handleAmountInput,
  validateBeforeSubmit,
  isVisible,
  isReadOnly,
  placeholderOf,
  businessCode,
  findBusinessFieldKeys,
  departmentLabel,
  sharedArchiveItems
} = useExpenseRuntimeBlockRuntime({
  schema: computed(() => props.schema),
  formData,
  sharedArchives: computed(() => props.sharedArchives || []),
  companyOptionsSource: computed(() => props.companyOptions || []),
  departmentOptionsSource: computed(() => props.departmentOptions || []),
  detailType: computed(() => String(props.detailType || '').trim()),
  defaultBusinessScenario: computed(() => String(props.defaultBusinessScenario || '').trim()),
  approvalEditMode: computed(() => props.approvalEditMode),
  allowEditFormModule: computed(() => props.allowEditFormModule),
  allowEditPayAccount: computed(() => props.allowEditPayAccount)
})

defineExpose({ validateBeforeSubmit })

const { resolveErrorMessage, toOptionalMoney, toOptionalString } = useExpenseRuntimePageUtils()

const {
  PAYEE_PLACEHOLDER,
  MISSING_VENDOR_BANK_INFO_MESSAGE,
  effectivePaymentCompanyId,
  selectedCounterpartyCode,
  vendorOptionsLoading,
  payeeOptions,
  payeeOptionsLoading,
  payeeAccountOptionsLoading,
  visibleVendorOptions,
  visiblePayeeAccountOptions,
  counterpartyPlaceholder,
  payeeAccountPlaceholder,
  showVendorAccountMaintenanceEntry,
  vendorDialogVisible,
  vendorDialogTitle,
  vendorDialogSubmitText,
  vendorDialogLoading,
  vendorSaving,
  vendorDraft,
  isCounterpartyDisabled,
  isPayeeAccountDisabled,
  loadVendorOptions,
  loadPayeeOptions,
  loadPayeeAccountOptions,
  handlePayeeAccountDropdownVisibleChange,
  buildPayeeSnapshot,
  buildPayeeAccountSnapshot,
  setCounterpartySelectRef,
  setPayeeAccountSelectRef,
  prepareDocumentPickerOpen,
  handleCounterpartySelection,
  handlePayeeSelection,
  handlePayeeAccountSelection,
  openVendorDialog,
  closeVendorDialog,
  openVendorAccountDialog,
  validateVendorDraft,
  saveVendor
} = useExpenseRuntimePaymentCounterparty({
  formData,
  currentUserCompanyId: computed(() => String(props.currentUserCompanyId || '').trim()),
  detailType: computed(() => String(props.detailType || '').trim()),
  hydratingForm: computed(() => props.hydratingForm),
  hydrationVersion: computed(() => props.hydrationVersion),
  findBusinessFieldKeys,
  isReadOnly,
  resolveErrorMessage
})

const {
  documentPickerDialog,
  documentPickerTitle,
  isRelatedDocumentBlock,
  isWriteOffDocumentBlock,
  isDocumentBusinessBlock,
  documentBlockHint,
  documentRecords,
  openDocumentPicker,
  closeDocumentPicker,
  loadDocumentPicker,
  isDocumentSelected,
  selectedGroupCount,
  toggleDocumentSelection,
  confirmDocumentPicker,
  removeDocumentRecord,
  updateWriteOffAmount,
  resolveTemplateTypeLabel,
  writeOffSourceKindLabel,
  formatAmount
} = useExpenseRuntimeDocumentPicker({
  blocks,
  formData,
  businessCode,
  prepareDocumentPickerOpen,
  resolveErrorMessage,
  toOptionalMoney
})

const {
  uploadFileList,
  uploadAccept,
  handleFileChange,
  handleFileRemove
} = useExpenseRuntimeAttachmentOcr({
  formData,
  schema: computed(() => props.schema),
  detailType: computed(() => String(props.detailType || '').trim()),
  defaultBusinessScenario: computed(() => String(props.defaultBusinessScenario || '').trim()),
  resolveErrorMessage
})

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

