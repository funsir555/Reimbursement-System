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
            :placeholder="placeholderOf(block)"
            :readonly="isReadOnly(block)"
          />
          <el-input
            v-else-if="controlType(block) === 'TEXTAREA'"
            v-model="formData[block.fieldKey]"
            type="textarea"
            :rows="4"
            :placeholder="placeholderOf(block)"
            :readonly="isReadOnly(block)"
          />
          <el-input-number
            v-else-if="controlType(block) === 'NUMBER'"
            v-model="formData[block.fieldKey]"
            :controls="false"
            class="w-full"
            :disabled="isReadOnly(block)"
          />
          <money-input
            v-else-if="controlType(block) === 'AMOUNT'"
            v-model="formData[block.fieldKey]"
            class="w-full"
            :disabled="isReadOnly(block)"
          />
          <el-date-picker
            v-else-if="controlType(block) === 'DATE'"
            v-model="formData[block.fieldKey]"
            type="date"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="w-full"
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
            class="w-full"
          />
          <el-select
            v-else-if="controlType(block) === 'SELECT'"
            v-model="formData[block.fieldKey]"
            :clearable="!isReadOnly(block)"
            class="w-full"
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
            class="w-full"
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
            :accept="String(block.props.accept || '')"
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
            :placeholder="placeholderOf(block)"
            :readonly="isReadOnly(block)"
          />
        </template>

        <template v-else-if="businessCode(block) === 'counterparty'">
          <div class="space-y-3">
            <div class="flex gap-3">
              <el-select
                v-model="formData[block.fieldKey]"
                filterable
                remote
                reserve-keyword
                clearable
                class="w-full"
                placeholder="请选择往来单位"
                :remote-method="loadVendorOptions"
                :loading="vendorOptionsLoading"
              >
                <el-option
                  v-for="item in vendorOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="truncate">{{ item.label }}</span>
                    <span class="text-xs text-slate-400">{{ item.secondaryLabel }}</span>
                  </div>
                </el-option>
              </el-select>
              <el-button plain @click="openVendorDialog">新增往来单位</el-button>
            </div>
          </div>
        </template>

        <template v-else-if="businessCode(block) === 'payee'">
          <el-select
            v-model="formData[block.fieldKey]"
            filterable
            remote
            reserve-keyword
            clearable
            class="w-full"
            placeholder="请选择收款人"
            :remote-method="loadPayeeOptions"
            :loading="payeeOptionsLoading"
          >
            <el-option
              v-for="item in payeeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
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
            filterable
            remote
            reserve-keyword
            clearable
            class="w-full"
            placeholder="请选择收款账户"
            :remote-method="loadPayeeAccountOptions"
            :loading="payeeAccountOptionsLoading"
          >
            <el-option
              v-for="item in payeeAccountOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            >
              <div class="space-y-1">
                <div class="flex items-center justify-between gap-3">
                  <span class="truncate">{{ item.label }}</span>
                  <span class="text-xs text-slate-400">{{ item.accountNoMasked }}</span>
                </div>
                <p class="truncate text-xs text-slate-400">{{ item.secondaryLabel }}</p>
              </div>
            </el-option>
          </el-select>
        </template>

        <template v-else-if="businessCode(block) === 'undertake-department'">
          <el-select
            v-model="formData[block.fieldKey]"
            clearable
            filterable
            class="w-full"
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
            class="w-full"
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
                @click="openDocumentPicker(block)"
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
                    移除
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
                      class="mt-2 w-full"
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
            class="w-full"
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

    <el-dialog v-model="vendorDialogVisible" title="新增往来单位" width="760px" destroy-on-close>
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <el-form-item label="往来单位名称" required class="!mb-0">
          <el-input v-model="vendorDraft.cVenName" placeholder="请输入往来单位名称" />
        </el-form-item>
        <el-form-item label="往来单位简称" class="!mb-0">
          <el-input v-model="vendorDraft.cVenAbbName" placeholder="请输入简称" />
        </el-form-item>
        <el-form-item label="纳税人登记号" class="!mb-0">
          <el-input v-model="vendorDraft.cVenRegCode" placeholder="请输入纳税号" />
        </el-form-item>
        <el-form-item label="联系人" class="!mb-0">
          <el-input v-model="vendorDraft.cVenPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="电话" class="!mb-0">
          <el-input v-model="vendorDraft.cVenPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="公司主体编码" class="!mb-0">
          <el-input v-model="vendorDraft.companyId" placeholder="请输入公司主体编码" />
        </el-form-item>
        <el-form-item label="开户地址" class="!mb-0">
          <el-input v-model="vendorDraft.cVenAddress" placeholder="请输入开户地址" />
        </el-form-item>
        <el-form-item label="开户行" class="!mb-0">
          <el-input v-model="vendorDraft.cVenBank" placeholder="请输入开户行" />
        </el-form-item>
        <el-form-item label="银行账号" class="!mb-0">
          <el-input v-model="vendorDraft.cVenAccount" placeholder="请输入银行账号" />
        </el-form-item>
        <el-form-item label="银行行号" class="!mb-0">
          <el-input v-model="vendorDraft.cVenBankNub" placeholder="请输入银行行号" />
        </el-form-item>
        <el-form-item label="备注" class="xl:col-span-2 !mb-0">
          <el-input v-model="vendorDraft.cMemo" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="vendorDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="vendorSaving" @click="createVendor">新增往来单位</el-button>
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
          <div
            v-for="group in documentPickerDialog.groups"
            :key="`${documentPickerDialog.fieldKey}-${group.templateType}`"
            class="rounded-[24px] border border-slate-200 bg-slate-50 p-4"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-sm font-semibold text-slate-800">{{ group.templateTypeLabel }}</p>
                <p class="mt-1 text-xs text-slate-500">共 {{ group.total }} 条可选单据</p>
              </div>
              <p class="text-xs text-slate-400">
                已选 {{ selectedGroupCount(group) }} / {{ group.items.length }}
              </p>
            </div>

            <div class="mt-4 space-y-3">
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
          </div>
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
import { computed, reactive, ref, watch } from 'vue'
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
  type FinanceVendorSavePayload,
  type ProcessCustomArchiveDetail,
  type ProcessFormDesignBlock,
  type ProcessFormDesignSchema,
  type ProcessFormOption
} from '@/api'
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
  isExpenseDetailBlockReadOnly,
  isExpenseDetailBlockVisible
} from '@/views/expense/expenseDetailRuntime'

const formData = defineModel<Record<string, unknown>>({ required: true })

type DocumentRelationType = 'RELATED' | 'WRITEOFF'
type RuntimeDocumentRecord = ExpenseRelatedDocumentValue & Partial<ExpenseWriteOffDocumentValue>

const props = withDefaults(defineProps<{
  schema: ProcessFormDesignSchema
  sharedArchives?: ProcessCustomArchiveDetail[]
  companyOptions?: ProcessFormOption[]
  departmentOptions?: ProcessFormOption[]
  detailType?: string
  defaultBusinessScenario?: string
}>(), {
  sharedArchives: () => [],
  companyOptions: () => [],
  departmentOptions: () => [],
  detailType: '',
  defaultBusinessScenario: ''
})

const blocks = computed(() => props.schema?.blocks || [])
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
const documentPickerDialog = reactive<{
  visible: boolean
  fieldKey: string
  relationType: DocumentRelationType
  keyword: string
  loading: boolean
  groups: ExpenseDocumentPickerGroup[]
  selectedCodes: string[]
  itemsByCode: Record<string, RuntimeDocumentRecord>
}>({
  visible: false,
  fieldKey: '',
  relationType: 'RELATED',
  keyword: '',
  loading: false,
  groups: [],
  selectedCodes: [],
  itemsByCode: {}
})

const vendorDialogVisible = ref(false)
const vendorSaving = ref(false)
const vendorDraft = reactive<FinanceVendorSavePayload>({
  cVenName: '',
  cVenAbbName: '',
  cVenRegCode: '',
  cVenPerson: '',
  cVenPhone: '',
  cVenAddress: '',
  cVenBank: '',
  cVenAccount: '',
  cVenBankNub: '',
  companyId: '',
  cMemo: ''
})

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
  return isExpenseDetailBlockVisible(block, formData.value, props.detailType, props.defaultBusinessScenario)
}

function isReadOnly(block: ProcessFormDesignBlock) {
  return isExpenseDetailBlockReadOnly(block)
}

function placeholderOf(block: ProcessFormDesignBlock) {
  return String(block.props.placeholder || `请输入${block.label}`)
}

function businessCode(block: ProcessFormDesignBlock) {
  return getBusinessComponentDefinition(String(block.props.componentCode || ''))?.code || String(block.props.componentCode || '')
}

function departmentLabel(value: string) {
  return departmentOptions.value.find((item) => item.value === value)?.label || value
}

function sharedArchiveItems(block: ProcessFormDesignBlock) {
  return sharedArchiveMap.value.get(getSharedArchiveCode(block))?.items || []
}

const documentPickerTitle = computed(() => documentPickerDialog.relationType === 'WRITEOFF' ? '选择核销单据' : '选择关联单据')

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
    ? '支持选择借款单或可核销报销单，选中后逐条填写本次核销金额。'
    : '支持按单据类型分组选择多张已审批通过的业务单据。'
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
  documentPickerDialog.visible = true
  documentPickerDialog.fieldKey = block.fieldKey
  documentPickerDialog.relationType = documentRelationType(block)
  documentPickerDialog.keyword = ''
  documentPickerDialog.groups = []
  documentPickerDialog.selectedCodes = []
  documentPickerDialog.itemsByCode = {}

  documentRecords(block).forEach((item) => {
    if (!item.documentCode) {
      return
    }
    documentPickerDialog.selectedCodes.push(item.documentCode)
    documentPickerDialog.itemsByCode[item.documentCode] = cloneDocumentRecord(item)
  })

  void loadDocumentPicker()
}

function closeDocumentPicker() {
  documentPickerDialog.visible = false
  documentPickerDialog.fieldKey = ''
  documentPickerDialog.keyword = ''
  documentPickerDialog.groups = []
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
  vendorOptionsLoading.value = true
  try {
    const res = await expenseCreateApi.listVendorOptions(keyword)
    vendorOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载往来单位失败'))
  } finally {
    vendorOptionsLoading.value = false
  }
}

async function loadPayeeOptions(keyword: string) {
  payeeOptionsLoading.value = true
  try {
    const res = await expenseCreateApi.listPayeeOptions(keyword)
    payeeOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载收款人失败'))
  } finally {
    payeeOptionsLoading.value = false
  }
}

async function loadPayeeAccountOptions(keyword: string) {
  payeeAccountOptionsLoading.value = true
  try {
    const res = await expenseCreateApi.listPayeeAccountOptions(keyword)
    payeeAccountOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载收款账户失败'))
  } finally {
    payeeAccountOptionsLoading.value = false
  }
}

function uploadFileList(block: ProcessFormDesignBlock): UploadUserFile[] {
  return normalizeAttachments(formData.value[block.fieldKey]).map((item, index) => ({
    name: item.fileName,
    status: 'success',
    uid: index + 1
  }))
}

async function handleFileChange(block: ProcessFormDesignBlock, uploadFile: UploadFile) {
  if (!uploadFile.raw) {
    return
  }

  try {
    const res = await expenseCreateApi.uploadAttachment(uploadFile.raw)
    const current = normalizeAttachments(formData.value[block.fieldKey])
    formData.value[block.fieldKey] = [...current, res.data]
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '附件上传失败'))
  }
}

function handleFileRemove(block: ProcessFormDesignBlock, uploadFile: UploadFile) {
  const current = normalizeAttachments(formData.value[block.fieldKey])
  formData.value[block.fieldKey] = current.filter((item, index) => {
    const fallbackUid = `legacy-${block.fieldKey}-${index}-${item.fileName}`
    const currentUid = item.attachmentId || fallbackUid
    if (uploadFile.uid !== undefined && String(uploadFile.uid) === currentUid) {
      return false
    }
    return item.fileName !== uploadFile.name
  })
}

function openVendorDialog() {
  Object.assign(vendorDraft, {
    cVenName: '',
    cVenAbbName: '',
    cVenRegCode: '',
    cVenPerson: '',
    cVenPhone: '',
    cVenAddress: '',
    cVenBank: '',
    cVenAccount: '',
    cVenBankNub: '',
    companyId: '',
    cMemo: ''
  })
  vendorDialogVisible.value = true
}

async function createVendor() {
  if (!String(vendorDraft.cVenName || '').trim()) {
    ElMessage.warning('请先填写往来单位名称')
    return
  }
  vendorSaving.value = true
  try {
    const payload = Object.fromEntries(
      Object.entries(vendorDraft).filter(([, value]) => value !== undefined && value !== null && value !== '')
    ) as FinanceVendorSavePayload
    const res = await expenseCreateApi.createVendor(payload)
    ElMessage.success('往来单位已新增')
    await loadVendorOptions(String(res.data.cVenName || ''))
    vendorDialogVisible.value = false
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '新增往来单位失败'))
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
      previewUrl: firstNonBlank(record.previewUrl, record.fileUrl, record.url)
    }]
  }

  return []
}

function firstNonBlank(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) {
      return value.trim()
    }
  }
  return undefined
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
</script>
