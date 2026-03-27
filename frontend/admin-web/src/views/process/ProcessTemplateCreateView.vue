<template>
  <div class="flex items-start gap-6">
    <process-workbench-sidebar
      :items="navItems"
      active-key="document-flow"
      @select="handleSidebarSelect"
    />

    <div class="flex flex-1 items-start gap-6">
      <div class="min-w-0 flex-1 space-y-6">
        <section class="rounded-[32px] border border-slate-100 bg-white px-8 py-7 shadow-sm">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
                <el-icon><ArrowLeft /></el-icon>
                返回单据与流程
              </button>
              <h1 class="mt-3 text-3xl font-bold text-slate-800">
                新建{{ options?.templateTypeLabel || '单据' }}模板
              </h1>
              <p class="mt-3 max-w-3xl leading-7 text-slate-500">
                在这里配置单据的基础信息、审批流、适用范围，以及费用类型、标签档案和分期付款档案映射。
              </p>
            </div>

            <div class="flex gap-3">
              <el-button @click="goBack">取消</el-button>
              <el-button v-if="canCreate" type="primary" :loading="saving" @click="saveTemplate">保存模板</el-button>
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

            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
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
                  placeholder="建议说明单据适用场景和审核边界"
                />
              </el-form-item>

              <el-form-item label="编码规则">
                <div class="rule-preview">
                  <span>{{ options?.numberingRulePreview || defaultNumberingRulePreview }}</span>
                </div>
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
              <span class="font-semibold text-slate-800">单据与流程</span>
            </template>

            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
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

              <el-form-item label="审批流程">
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

            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <el-form-item label="费用类型">
                <el-tree-select
                  v-model="form.expenseTypes"
                  :data="options?.expenseTypes || []"
                  node-key="expenseCode"
                  multiple
                  show-checkbox
                  check-strictly
                  filterable
                  clearable
                  collapse-tags
                  collapse-tags-tooltip
                  :props="{ label: 'expenseName', children: 'children', value: 'expenseCode' }"
                  placeholder="请选择费用类型"
                />
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

          <el-card id="tag" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">标签设置</span>
            </template>

            <el-form-item label="标签档案">
              <el-select v-model="form.tagOption" clearable placeholder="请选择自定义档案内容">
                <el-option
                  v-for="item in options?.tagOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-card>

          <el-card id="installment" class="anchor-card !rounded-3xl !shadow-sm">
            <template #header>
              <span class="font-semibold text-slate-800">分期付款</span>
            </template>

            <el-form-item label="分期付款档案">
              <el-select v-model="form.installmentOption" clearable placeholder="请选择自定义档案内容">
                <el-option
                  v-for="item in options?.installmentOptions || []"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-card>
        </el-form>

        <div class="sticky bottom-4 z-10 flex justify-end gap-3 rounded-2xl border border-slate-200 bg-white/90 px-6 py-4 shadow-sm backdrop-blur">
          <el-button @click="goBack">取消</el-button>
          <el-button v-if="canCreate" type="primary" :loading="saving" @click="saveTemplate">保存模板</el-button>
        </div>
      </div>

      <aside class="sticky top-24 w-64 shrink-0">
        <el-card class="border border-slate-100 !rounded-3xl !shadow-sm">
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
import { hasPermission, readStoredUser } from '@/utils/permissions'
import ProcessWorkbenchSidebar from '@/components/process/ProcessWorkbenchSidebar.vue'

const route = useRoute()
const router = useRouter()

const defaultNumberingRulePreview = 'FX+年+月+日+4位数字（如：FX202503251234）'

const navItems = ref<ProcessCenterNavItem[]>([])
const options = ref<ProcessTemplateFormOptions | null>(null)
const saving = ref(false)
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])

const canCreate = computed(() => hasPermission('expense:process_management:create', permissionCodes.value))
const templateType = computed(() => String(route.params.templateType || 'report'))

const form = reactive<ProcessTemplateSavePayload>({
  templateType: templateType.value,
  templateName: '',
  templateDescription: '',
  category: '',
  iconColor: 'blue',
  enabled: true,
  printMode: '',
  approvalFlow: '',
  paymentMode: '',
  allocationForm: '',
  expenseTypes: [],
  aiAuditMode: '',
  scopeOptions: [],
  tagOption: '',
  installmentOption: ''
})

const anchorSections = [
  { id: 'basic', label: '基础设置' },
  { id: 'flow', label: '单据与流程' },
  { id: 'scope', label: '规则与适用范围' },
  { id: 'tag', label: '标签设置' },
  { id: 'installment', label: '分期付款' }
]

onMounted(async () => {
  try {
    const [overviewRes, optionRes] = await Promise.all([
      processApi.getOverview(),
      processApi.getFormOptions(templateType.value)
    ])

    navItems.value = overviewRes.data.navItems
    options.value = optionRes.data
    form.templateType = optionRes.data.templateType
    form.category = optionRes.data.categoryOptions[0]?.value || ''
    form.printMode = optionRes.data.printModes[0]?.value || ''
    form.approvalFlow = optionRes.data.approvalFlows[0]?.value || ''
    form.paymentMode = optionRes.data.paymentModes[0]?.value || ''
    form.allocationForm = optionRes.data.allocationForms[0]?.value || ''
    form.aiAuditMode = optionRes.data.aiAuditModes[0]?.value || ''
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
  if (!canCreate.value) {
    ElMessage.warning('当前账号没有新增流程模板权限')
    return
  }

  if (!form.templateName.trim()) {
    ElMessage.warning('请先填写单据名称')
    return
  }

  saving.value = true
  try {
    const res = await processApi.createTemplate(form)
    ElMessage.success(`模板已保存：${res.data.templateCode}`)
    router.push('/expense/workbench/process-management')
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
  cursor: pointer;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 14px;
  color: #64748b;
  transition: all 0.2s ease;
}

.anchor-link:hover {
  background: #eff6ff;
  color: #2563eb;
}

.rule-preview {
  min-height: 40px;
  display: flex;
  align-items: center;
  border: 1px solid #dbe2eb;
  border-radius: 12px;
  background: #f8fafc;
  padding: 0 14px;
  color: #334155;
}
</style>
