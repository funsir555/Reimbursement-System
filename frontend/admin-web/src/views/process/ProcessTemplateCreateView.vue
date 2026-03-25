<template>
  <div class="flex gap-6 items-start">
    <process-workbench-sidebar
      :items="navItems"
      active-key="document-flow"
      @select="handleSidebarSelect"
    />

    <div class="flex-1 flex gap-6 items-start">
      <div class="flex-1 space-y-6 min-w-0">
        <section class="bg-white rounded-[32px] shadow-sm border border-slate-100 px-8 py-7">
          <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
            <div>
              <button type="button" class="text-sm text-blue-600 flex items-center gap-2" @click="goBack">
                <el-icon><ArrowLeft /></el-icon>
                返回单据与流程
              </button>
              <h1 class="text-3xl font-bold text-slate-800 mt-3">
                新建{{ options?.templateTypeLabel || '单据' }}模板
              </h1>
              <p class="text-slate-500 mt-3 leading-7 max-w-3xl">
                这里用于配置单据的基础信息、表单与流程、适用范围以及财务校验规则。
                页面布局借鉴了费用系统的工作台思路，但视觉和分区已经重新设计，便于后续继续扩展。
              </p>
            </div>

            <div class="flex gap-3">
              <el-button @click="goBack">取消</el-button>
              <el-button type="primary" :loading="saving" @click="saveTemplate">保存模板</el-button>
            </div>
          </div>
        </section>

        <el-form label-position="top" class="space-y-6">
          <el-card id="basic" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-semibold text-slate-800">基础设置</span>
                <el-tag type="primary" effect="plain">{{ options?.templateTypeLabel || '模板' }}</el-tag>
              </div>
            </template>

            <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
              <el-form-item label="单据名称" required>
                <el-input v-model="form.templateName" placeholder="请输入单据名称" />
              </el-form-item>
              <el-form-item label="所属分类" required>
                <el-select v-model="form.category" placeholder="请选择分类">
                  <el-option
                    v-for="item in options?.categoryOptions || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="单据说明" class="xl:col-span-2">
                <el-input
                  v-model="form.templateDescription"
                  type="textarea"
                  :rows="3"
                  placeholder="建议说明单据适用场景、审核边界和主要用途"
                />
              </el-form-item>
              <el-form-item label="单据编号规则">
                <el-select v-model="form.numberingRule" placeholder="请选择编号规则">
                  <el-option
                    v-for="item in options?.numberingRules || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="主题色">
                <el-radio-group v-model="form.iconColor" class="flex flex-wrap gap-4">
                  <el-radio-button label="blue">商务蓝</el-radio-button>
                  <el-radio-button label="cyan">海岸青</el-radio-button>
                  <el-radio-button label="orange">暖橙色</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="启用状态">
                <el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="停用" />
              </el-form-item>
            </div>
          </el-card>

          <el-card id="flow" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">表单与流程</span>
            </template>

            <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
              <el-form-item label="表单打印">
                <el-select v-model="form.printMode" placeholder="请选择表单打印方式">
                  <el-option
                    v-for="item in options?.printModes || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="流程">
                <el-select v-model="form.approvalFlow" placeholder="请选择流程">
                  <el-option
                    v-for="item in options?.approvalFlows || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="付款单设置">
                <el-select v-model="form.paymentMode" placeholder="请选择付款联动策略">
                  <el-option
                    v-for="item in options?.paymentModes || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="分期付款">
                <el-switch v-model="form.splitPayment" inline-prompt active-text="支持" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="行程表单">
                <el-select v-model="form.travelForm" placeholder="请选择行程表单">
                  <el-option
                    v-for="item in options?.travelForms || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="分摊表单">
                <el-select v-model="form.allocationForm" placeholder="请选择分摊表单">
                  <el-option
                    v-for="item in options?.allocationForms || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </div>
          </el-card>

          <el-card id="scope" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">规则与适用范围</span>
            </template>

            <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
              <el-form-item label="费用类型">
                <el-select
                  v-model="form.expenseTypes"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="请选择费用类型"
                >
                  <el-option
                    v-for="item in options?.expenseTypes || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="AI 审核策略">
                <el-select v-model="form.aiAuditMode" placeholder="请选择 AI 审核策略">
                  <el-option
                    v-for="item in options?.aiAuditModes || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="适用范围" class="mt-2">
              <el-checkbox-group v-model="form.scopeOptions" class="flex flex-wrap gap-x-8 gap-y-3">
                <el-checkbox
                  v-for="item in options?.scopeOptions || []"
                  :key="item.value"
                  :label="item.value"
                >
                  {{ item.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-card>

          <el-card id="relation" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">报销单关联设置</span>
            </template>

            <el-form-item label="关联规则说明">
              <el-input
                v-model="form.relationRemark"
                type="textarea"
                :rows="4"
                placeholder="描述模板与报销、申请、借款或付款单之间的关联规则"
              />
            </el-form-item>
          </el-card>

          <el-card id="tag" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">标签设置</span>
            </template>

            <el-form-item label="模板标签">
              <el-checkbox-group v-model="form.tagOptions" class="flex flex-wrap gap-x-8 gap-y-3">
                <el-checkbox
                  v-for="item in options?.tagOptions || []"
                  :key="item.value"
                  :label="item.value"
                >
                  {{ item.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-card>

          <el-card id="validation" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">企业支付费用未税校验</span>
            </template>

            <el-form-item label="校验规则说明">
              <el-input
                v-model="form.validationRemark"
                type="textarea"
                :rows="4"
                placeholder="填写税额、未税金额或企业付款规则的校验逻辑"
              />
            </el-form-item>
          </el-card>

          <el-card id="installment" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">分期付款</span>
            </template>

            <el-form-item label="分期付款规则">
              <el-input
                v-model="form.installmentRemark"
                type="textarea"
                :rows="4"
                placeholder="填写分期触发条件、分期节点和金额策略"
              />
            </el-form-item>
          </el-card>
        </el-form>

        <div class="sticky bottom-4 z-10 bg-white/90 backdrop-blur rounded-2xl border border-slate-200 px-6 py-4 shadow-sm flex justify-end gap-3">
          <el-button @click="goBack">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveTemplate">保存模板</el-button>
        </div>
      </div>

      <aside class="w-64 sticky top-24 shrink-0">
        <el-card class="!rounded-3xl !shadow-sm border border-slate-100">
          <div class="space-y-3">
            <div
              v-for="anchor in anchorSections"
              :key="anchor.id"
              class="anchor-link"
              @click="scrollToSection(anchor.id)"
            >
              {{ anchor.label }}
            </div>
          </div>
        </el-card>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  processApi,
  type ProcessCenterNavItem,
  type ProcessTemplateFormOptions,
  type ProcessTemplateSavePayload
} from '@/api'
import ProcessWorkbenchSidebar from '@/components/process/ProcessWorkbenchSidebar.vue'

const route = useRoute()
const router = useRouter()

const navItems = ref<ProcessCenterNavItem[]>([])
const options = ref<ProcessTemplateFormOptions | null>(null)
const saving = ref(false)

const templateType = computed(() => String(route.params.templateType || 'report'))

const form = reactive<ProcessTemplateSavePayload>({
  templateType: templateType.value,
  templateName: '',
  templateDescription: '',
  category: '',
  numberingRule: '',
  iconColor: 'blue',
  enabled: true,
  printMode: '',
  approvalFlow: '',
  paymentMode: '',
  splitPayment: false,
  travelForm: '',
  allocationForm: '',
  expenseTypes: [],
  aiAuditMode: '',
  scopeOptions: [],
  tagOptions: [],
  relationRemark: '',
  validationRemark: '',
  installmentRemark: ''
})

const anchorSections = [
  { id: 'basic', label: '基础设置' },
  { id: 'flow', label: '表单与流程' },
  { id: 'scope', label: '规则与适用范围' },
  { id: 'relation', label: '报销单关联设置' },
  { id: 'tag', label: '标签设置' },
  { id: 'validation', label: '企业支付费用未税校验' },
  { id: 'installment', label: '分期付款' }
]

onMounted(async () => {
  try {
    const [overviewRes, optionRes] = await Promise.all([
      processApi.getOverview(),
      processApi.getFormOptions(templateType.value)
    ])

    if (overviewRes.code === 200) {
      navItems.value = overviewRes.data.navItems
    }

    if (optionRes.code === 200) {
      options.value = optionRes.data
      form.templateType = optionRes.data.templateType
      form.category = optionRes.data.categoryOptions[0]?.value || ''
      form.numberingRule = optionRes.data.numberingRules[0]?.value || ''
      form.printMode = optionRes.data.printModes[0]?.value || ''
      form.approvalFlow = optionRes.data.approvalFlows[0]?.value || ''
      form.paymentMode = optionRes.data.paymentModes[0]?.value || ''
      form.travelForm = optionRes.data.travelForms[0]?.value || ''
      form.allocationForm = optionRes.data.allocationForms[0]?.value || ''
      form.aiAuditMode = optionRes.data.aiAuditModes[0]?.value || ''
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载模板配置页面失败')
  }
})

const handleSidebarSelect = (section: string) => {
  if (section === 'document-flow') {
    router.push('/expense/workbench/process-management')
    return
  }

  router.push({
    path: '/expense/workbench/process-management',
    query: { section }
  })
}

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const goBack = () => {
  router.push('/expense/workbench/process-management')
}

const saveTemplate = async () => {
  if (!form.templateName.trim()) {
    ElMessage.warning('请先填写单据名称')
    return
  }

  saving.value = true
  try {
    const res = await processApi.createTemplate(form)
    if (res.code === 200) {
      ElMessage.success(`模板已保存：${res.data.templateCode}`)
      router.push('/expense/workbench/process-management')
      return
    }
    ElMessage.error(res.message || '保存模板失败')
  } catch (error: any) {
    ElMessage.error(error.message || '保存模板失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.anchor-card {
  scroll-margin-top: 88px;
}

.anchor-link {
  font-size: 14px;
  color: #64748b;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.anchor-link:hover {
  color: #2563eb;
  background: #eff6ff;
}
</style>
