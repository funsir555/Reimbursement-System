<template>
  <div class="space-y-6">
    <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
      <div class="flex flex-col gap-5">
        <div>
          <h1 class="text-3xl font-bold text-slate-800">新建审批单</h1>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
            先选择模板，再按绑定的表单设计动态填写。保存即生效的表单、流程和共享字段会直接在这里渲染。
          </p>
        </div>
      </div>
    </section>

    <div v-if="!selectedTemplateCode" class="space-y-4">
      <el-card class="!rounded-3xl !shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-xl font-semibold text-slate-800">选择审批模板</h2>
            <p class="mt-2 text-sm text-slate-500">请选择一个已启用的单据模板，系统会根据绑定的表单与流程自动生成提单内容。</p>
          </div>

          <el-input
            v-model="templateKeyword"
            clearable
            placeholder="搜索模板名称"
            class="max-w-[320px]"
          />
        </div>
      </el-card>

      <div class="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <button
          v-for="template in filteredTemplates"
          :key="template.templateCode"
          type="button"
          class="template-card"
          @click="chooseTemplate(template.templateCode)"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 text-left">
              <p class="text-lg font-semibold text-slate-800">{{ template.templateName }}</p>
              <p class="mt-2 text-sm text-slate-500">{{ template.templateTypeLabel }}</p>
            </div>
            <el-tag effect="plain">{{ template.categoryCode || '默认分类' }}</el-tag>
          </div>
          <p class="mt-6 text-sm leading-6 text-slate-500">
            表单编码：{{ template.formDesignCode || '未绑定表单' }}
          </p>
        </button>
      </div>

      <el-empty v-if="!filteredTemplates.length" description="当前没有可用模板" :image-size="96" />
    </div>

    <template v-else>
      <el-card class="!rounded-3xl !shadow-sm" v-loading="loading">
        <div class="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
          <div>
            <div class="flex flex-wrap items-center gap-3">
              <h2 class="text-2xl font-semibold text-slate-800">{{ templateDetail?.templateName }}</h2>
              <el-tag effect="plain">{{ templateDetail?.templateTypeLabel }}</el-tag>
              <el-tag v-if="templateDetail?.flowName" type="success" effect="plain">流程：{{ templateDetail?.flowName }}</el-tag>
            </div>
            <p class="mt-3 text-sm leading-7 text-slate-500">
              {{ templateDetail?.templateDescription || '暂无模板说明' }}
            </p>
          </div>

          <div class="flex flex-wrap items-center gap-3">
            <el-button @click="selectedTemplateCode = ''">重新选择模板</el-button>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!canSubmit"
              @click="submitDocument"
            >
              提交审批单
            </el-button>
          </div>
        </div>
      </el-card>

      <el-card class="!rounded-3xl !shadow-sm" v-loading="loading">
        <div class="space-y-6">
          <div class="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">
            <div class="grid grid-cols-1 gap-4 text-sm text-slate-600 xl:grid-cols-3">
              <div>表单名称：{{ templateDetail?.formName || '未命名表单' }}</div>
              <div>当前组件数：{{ blocks.length }}</div>
              <div>金额汇总：{{ totalAmountText }}</div>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
            <div
              v-for="block in blocks"
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
                    v-model="formValues[block.fieldKey]"
                    :placeholder="placeholderOf(block)"
                  />
                  <el-input
                    v-else-if="controlType(block) === 'TEXTAREA'"
                    v-model="formValues[block.fieldKey]"
                    type="textarea"
                    :rows="4"
                    :placeholder="placeholderOf(block)"
                  />
                  <el-input-number
                    v-else-if="controlType(block) === 'NUMBER' || controlType(block) === 'AMOUNT'"
                    v-model="formValues[block.fieldKey]"
                    :precision="controlType(block) === 'AMOUNT' ? 2 : 0"
                    :controls="false"
                    class="w-full"
                  />
                  <el-date-picker
                    v-else-if="controlType(block) === 'DATE'"
                    v-model="formValues[block.fieldKey]"
                    type="date"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    class="w-full"
                    placeholder="请选择日期"
                  />
                  <el-date-picker
                    v-else-if="controlType(block) === 'DATE_RANGE'"
                    v-model="formValues[block.fieldKey]"
                    type="daterange"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    class="w-full"
                  />
                  <el-select
                    v-else-if="controlType(block) === 'SELECT'"
                    v-model="formValues[block.fieldKey]"
                    clearable
                    class="w-full"
                    :placeholder="placeholderOf(block)"
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
                    v-model="formValues[block.fieldKey]"
                    multiple
                    clearable
                    collapse-tags
                    collapse-tags-tooltip
                    class="w-full"
                    :placeholder="placeholderOf(block)"
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
                    v-model="formValues[block.fieldKey]"
                    class="flex flex-wrap gap-3"
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
                    v-model="formValues[block.fieldKey]"
                    class="flex flex-wrap gap-3"
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
                    v-model="formValues[block.fieldKey]"
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
                    @change="handleFileChange(block, $event)"
                    @remove="handleFileRemove(block)"
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
                    v-model="formValues[block.fieldKey]"
                    :placeholder="placeholderOf(block)"
                  />
                </template>

                <template v-else-if="businessCode(block) === 'counterparty'">
                  <div class="space-y-3">
                    <el-select
                      v-model="formValues[block.fieldKey]"
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
                </template>

                <template v-else-if="businessCode(block) === 'payee'">
                  <el-select
                    v-model="formValues[block.fieldKey]"
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
                    v-model="formValues[block.fieldKey]"
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
                    v-model="formValues[block.fieldKey]"
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
                  <p
                    v-if="String(formValues[block.fieldKey] || '').trim()"
                    class="mt-2 text-xs leading-6 text-slate-400"
                  >
                    当前归属部门：{{ departmentLabel(String(formValues[block.fieldKey] || '')) }}
                  </p>
                </template>

                <template v-else-if="businessCode(block) === 'bank-push-summary'">
                  <el-input
                    v-model="formValues[block.fieldKey]"
                    maxlength="120"
                    show-word-limit
                    placeholder="请输入银行推送摘要"
                  />
                </template>

                <template v-else-if="block.kind === 'SHARED_FIELD'">
                  <el-select
                    v-model="formValues[block.fieldKey]"
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
          </div>
        </div>
      </el-card>
    </template>

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
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadFile } from 'element-plus'
import {
  expenseCreateApi,
  type ExpenseCreatePayeeAccountOption,
  type ExpenseCreatePayeeOption,
  type ExpenseCreateTemplateDetail,
  type ExpenseCreateTemplateSummary,
  type ExpenseCreateVendorOption,
  type FinanceVendorSavePayload,
  type ProcessFormOption,
  type ProcessFormDesignBlock
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import {
  getBusinessComponentDefinition,
  getControlType,
  getOptionItems,
  getSharedArchiveCode
} from '@/views/process/formDesignerHelper'

const router = useRouter()
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const loading = ref(false)
const submitting = ref(false)
const templates = ref<ExpenseCreateTemplateSummary[]>([])
const templateKeyword = ref('')
const selectedTemplateCode = ref('')
const templateDetail = ref<ExpenseCreateTemplateDetail | null>(null)
const formValues = reactive<Record<string, unknown>>({})

const vendorOptions = ref<ExpenseCreateVendorOption[]>([])
const vendorOptionsLoading = ref(false)
const payeeOptions = ref<ExpenseCreatePayeeOption[]>([])
const payeeOptionsLoading = ref(false)
const payeeAccountOptions = ref<ExpenseCreatePayeeAccountOption[]>([])
const payeeAccountOptionsLoading = ref(false)

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

const canSubmit = computed(() => hasPermission('expense:create:submit', permissionCodes.value) || hasPermission('expense:create:create', permissionCodes.value))
const filteredTemplates = computed(() => {
  const keyword = templateKeyword.value.trim()
  if (!keyword) {
    return templates.value
  }
  return templates.value.filter((item) => item.templateName.includes(keyword))
})
const blocks = computed(() => templateDetail.value?.schema.blocks || [])
const sharedArchiveMap = computed(() => new Map((templateDetail.value?.sharedArchives || []).map((item) => [item.archiveCode, item])))
const departmentOptions = computed<ProcessFormOption[]>(() => templateDetail.value?.departmentOptions || [])
const currentUserDeptId = computed(() => templateDetail.value?.currentUserDeptId || '')
const totalAmount = computed(() =>
  blocks.value.reduce((sum, block) => {
    if (block.kind !== 'CONTROL' || getControlType(block) !== 'AMOUNT') {
      return sum
    }
    const value = Number(formValues[block.fieldKey] || 0)
    return sum + (Number.isFinite(value) ? value : 0)
  }, 0)
)
const totalAmountText = computed(() => `¥ ${totalAmount.value.toFixed(2)}`)

void loadPage()

async function loadPage() {
  loading.value = true
  try {
    const res = await expenseCreateApi.listTemplates()
    templates.value = res.data
    if (selectedTemplateCode.value) {
      await loadTemplateDetail(selectedTemplateCode.value)
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载模板列表失败'))
  } finally {
    loading.value = false
  }
}

async function chooseTemplate(templateCode: string) {
  selectedTemplateCode.value = templateCode
  await loadTemplateDetail(templateCode)
}

async function loadTemplateDetail(templateCode: string) {
  loading.value = true
  try {
    const res = await expenseCreateApi.getTemplateDetail(templateCode)
    templateDetail.value = res.data
    resetFormValues()
    await Promise.all([
      loadVendorOptions(''),
      loadPayeeOptions(''),
      loadPayeeAccountOptions('')
    ])
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载模板详情失败'))
    selectedTemplateCode.value = ''
    templateDetail.value = null
  } finally {
    loading.value = false
  }
}

function resetFormValues() {
  Object.keys(formValues).forEach((key) => {
    delete formValues[key]
  })
  blocks.value.forEach((block) => {
    if (block.defaultValue !== undefined) {
      formValues[block.fieldKey] = Array.isArray(block.defaultValue)
        ? [...block.defaultValue]
        : block.defaultValue
      return
    }
    if (block.kind === 'CONTROL' && ['MULTI_SELECT', 'CHECKBOX', 'DATE_RANGE'].includes(controlType(block))) {
      formValues[block.fieldKey] = []
      return
    }
    if (block.kind === 'CONTROL' && controlType(block) === 'SWITCH') {
      formValues[block.fieldKey] = false
      return
    }
    const businessDefaultValue = resolveBusinessDefaultValue(block)
    if (businessDefaultValue !== undefined) {
      formValues[block.fieldKey] = businessDefaultValue
      return
    }
    formValues[block.fieldKey] = ''
  })
}

function controlType(block: ProcessFormDesignBlock) {
  return getControlType(block)
}

function optionItems(block: ProcessFormDesignBlock) {
  return getOptionItems(block)
}

function placeholderOf(block: ProcessFormDesignBlock) {
  return String(block.props.placeholder || `请输入${block.label}`)
}

function businessCode(block: ProcessFormDesignBlock) {
  return getBusinessComponentDefinition(String(block.props.componentCode || ''))?.code || String(block.props.componentCode || '')
}

function resolveBusinessDefaultValue(block: ProcessFormDesignBlock) {
  if (block.kind !== 'BUSINESS_COMPONENT' || businessCode(block) !== 'undertake-department') {
    return undefined
  }
  const mode = String(block.props.defaultDeptMode || 'NONE')
  if (mode === 'FIXED_DEPARTMENT') {
    const deptId = String(block.props.defaultDeptId || '').trim()
    return deptId || ''
  }
  if (mode === 'SUBMITTER_DEPARTMENT') {
    return currentUserDeptId.value || ''
  }
  return undefined
}

function departmentLabel(value: string) {
  return departmentOptions.value.find((item) => item.value === value)?.label || value
}

function sharedArchiveItems(block: ProcessFormDesignBlock) {
  return sharedArchiveMap.value.get(getSharedArchiveCode(block))?.items || []
}

async function loadVendorOptions(keyword: string) {
  vendorOptionsLoading.value = true
  try {
    const res = await expenseCreateApi.listVendorOptions(keyword)
    vendorOptions.value = res.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '\u52a0\u8f7d\u5f80\u6765\u5355\u4f4d\u5931\u8d25'))
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
    ElMessage.error(resolveErrorMessage(error, '\u52a0\u8f7d\u6536\u6b3e\u4eba\u5931\u8d25'))
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

function handleFileChange(block: ProcessFormDesignBlock, uploadFile: UploadFile) {
  const current = Array.isArray(formValues[block.fieldKey]) ? [...(formValues[block.fieldKey] as string[])] : []
  const fileName = uploadFile.name || ''
  if (fileName && !current.includes(fileName)) {
    current.push(fileName)
  }
  formValues[block.fieldKey] = current
}

function handleFileRemove(block: ProcessFormDesignBlock) {
  return (uploadFile: UploadFile) => {
    const current = Array.isArray(formValues[block.fieldKey]) ? [...(formValues[block.fieldKey] as string[])] : []
    formValues[block.fieldKey] = current.filter((item) => item !== uploadFile.name)
  }
}

async function createVendor() {
  if (!String(vendorDraft.cVenName || '').trim()) {
    ElMessage.warning('\u8bf7\u5148\u586b\u5199\u5f80\u6765\u5355\u4f4d\u540d\u79f0')
    return
  }
  vendorSaving.value = true
  try {
    const payload = Object.fromEntries(
      Object.entries(vendorDraft).filter(([, value]) => value !== undefined && value !== null && value !== '')
    ) as FinanceVendorSavePayload
    const res = await expenseCreateApi.createVendor(payload)
    ElMessage.success('往来单位已新增')
    vendorDialogVisible.value = false
    resetVendorDraft()
    await loadVendorOptions('')
    const fieldKey = counterpartyFieldKey()
    if (fieldKey) {
      formValues[fieldKey] = res.data.cVenCode
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '\u65b0\u589e\u5f80\u6765\u5355\u4f4d\u5931\u8d25'))
  } finally {
    vendorSaving.value = false
  }
}

function counterpartyFieldKey() {
  return blocks.value.find((block) => businessCode(block) === 'counterparty')?.fieldKey || ''
}

function openVendorDialog() {
  resetVendorDraft()
  vendorDialogVisible.value = true
}

function resetVendorDraft() {
  vendorDraft.cVenName = ''
  vendorDraft.cVenAbbName = ''
  vendorDraft.cVenRegCode = ''
  vendorDraft.cVenPerson = ''
  vendorDraft.cVenPhone = ''
  vendorDraft.cVenAddress = ''
  vendorDraft.cVenBank = ''
  vendorDraft.cVenAccount = ''
  vendorDraft.cVenBankNub = ''
  vendorDraft.companyId = ''
  vendorDraft.cMemo = ''
}

async function submitDocument() {
  if (!templateDetail.value) {
    return
  }
  const validationMessage = validateRequiredFields()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  submitting.value = true
  try {
    const payload = {
      templateCode: templateDetail.value.templateCode,
      formData: {
        ...formValues,
        __documentTitle: templateDetail.value.templateName,
        __documentReason: deriveDocumentReason(),
        __totalAmount: totalAmount.value
      }
    }
    const res = await expenseCreateApi.submit(payload)
    ElMessage.success(`提交成功，单号：${res.data.documentCode}`)
    selectedTemplateCode.value = ''
    templateDetail.value = null
    await router.push('/expense/list')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '\u63d0\u4ea4\u5ba1\u6279\u5355\u5931\u8d25'))
  } finally {
    submitting.value = false
  }
}

function validateRequiredFields() {
  for (const block of blocks.value) {
    if (!block.required || controlType(block) === 'SECTION') {
      continue
    }
    const value = formValues[block.fieldKey]
    if (Array.isArray(value) && value.length > 0) {
      continue
    }
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      continue
    }
    return `请填写：${block.label}`
  }
  return ''
}

function deriveDocumentReason() {
  const preferredBlock = blocks.value.find((block) => {
    const label = block.label.toLowerCase()
    return label.includes('事由') || label.includes('说明') || label.includes('摘要')
  })
  if (preferredBlock) {
    const value = formValues[preferredBlock.fieldKey]
    if (value !== undefined && value !== null && String(value).trim()) {
      return String(value).trim()
    }
  }
  return templateDetail.value?.templateDescription || templateDetail.value?.templateName || '\u5ba1\u6279\u5355\u63d0\u62a5'
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<style scoped>
.template-card {
  border-radius: 28px;
  border: 1px solid #dbe2eb;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 24px;
  text-align: left;
  transition: all 0.2s ease;
}

.template-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 16px 36px rgba(37, 99, 235, 0.08);
  transform: translateY(-2px);
}

.form-runtime-block {
  border-radius: 24px;
  border: 1px solid #dbe2eb;
  background: #fff;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
}
</style>


