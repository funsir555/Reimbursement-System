<template>
  <div class="flex items-start gap-6">
    <process-workbench-sidebar
      :items="overview?.navItems || []"
      :active-key="activeSection"
      @select="handleSectionChange"
    />

    <div class="flex-1 space-y-6">
      <template v-if="activeSection === 'document-flow'">
        <section
          class="overflow-hidden rounded-[32px] bg-gradient-to-r from-blue-600 via-sky-500 to-cyan-400 text-white shadow-lg"
        >
          <div class="flex flex-col gap-6 px-8 py-8 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p class="text-sm uppercase tracking-[0.22em] text-blue-100">Flow Studio</p>
              <h1 class="mt-3 text-3xl font-bold">单据与流程</h1>
              <p class="mt-3 max-w-2xl leading-7 text-blue-50/90">
                把单据模板、审批流程、付款规则和 AI 审核能力统一收口到同一个工作区里，让财务同事能够更快梳理流程、维护模板并推动业务上线。
              </p>
            </div>

            <div class="flex flex-wrap gap-3">
              <el-button v-if="canCreateTemplates" type="primary" class="hero-primary-btn" @click="openTemplateDialog">
                添加单据
              </el-button>
              <el-button v-if="canEditTemplates" class="hero-secondary-btn">
                批量归档
              </el-button>
            </div>
          </div>
        </section>

        <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <el-card v-for="stat in summaryCards" :key="stat.label" class="stat-card !rounded-3xl !shadow-sm">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-slate-500">{{ stat.label }}</p>
                <p class="mt-2 text-3xl font-bold text-slate-800">{{ stat.value }}</p>
                <p class="mt-3 text-xs text-slate-400">{{ stat.tip }}</p>
              </div>
              <div class="flex h-12 w-12 items-center justify-center rounded-2xl" :style="{ background: stat.bg }">
                <el-icon :size="22" class="text-white">
                  <component :is="stat.icon" />
                </el-icon>
              </div>
            </div>
          </el-card>
        </section>

        <el-card class="!rounded-3xl !shadow-sm">
          <div class="flex flex-col justify-between gap-4 xl:flex-row xl:items-center">
            <div class="flex flex-wrap gap-3">
              <el-button v-if="canCreateTemplates" type="primary" :icon="Plus" @click="openTemplateDialog">
                添加单据
              </el-button>
              <el-button v-if="canEditTemplates" :icon="CopyDocument">复制分类</el-button>
            </div>

            <div class="flex w-full flex-col gap-3 sm:flex-row xl:w-auto">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索单据名称、模板类型或流程名称"
                class="w-full sm:w-80"
                :prefix-icon="Search"
                clearable
              />
              <el-select v-model="activeCategory" class="w-full sm:w-52">
                <el-option label="全部分类" value="all" />
                <el-option
                  v-for="category in overview?.categories || []"
                  :key="category.code"
                  :label="category.name"
                  :value="category.code"
                />
              </el-select>
            </div>
          </div>
        </el-card>

        <section class="space-y-8">
          <div v-for="category in filteredCategories" :key="category.code" class="space-y-4">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-lg font-semibold text-slate-800">{{ category.name }}</h2>
                <p class="mt-1 text-sm text-slate-400">{{ category.description }}</p>
              </div>
              <span class="rounded-full bg-blue-50 px-3 py-1 text-sm text-blue-500">
                {{ category.templateCount }} 个模板
              </span>
            </div>

            <div class="grid grid-cols-1 gap-5 lg:grid-cols-2 2xl:grid-cols-3">
              <el-card
                v-for="template in category.templates"
                :key="template.id"
                class="template-card !rounded-3xl !shadow-sm"
              >
                <div class="flex items-start justify-between gap-4">
                  <div class="flex items-start gap-4">
                    <div
                      class="flex h-12 w-12 items-center justify-center rounded-2xl text-white shadow-sm"
                      :style="{ background: template.color }"
                    >
                      <el-icon :size="22"><Document /></el-icon>
                    </div>
                    <div>
                      <h3 class="text-lg font-semibold text-slate-800">{{ template.name }}</h3>
                      <p class="mt-1 text-sm text-slate-400">{{ template.templateType }} / {{ template.businessDomain }}</p>
                    </div>
                  </div>

                  <el-tag size="small" type="primary" effect="plain">{{ template.templateCode }}</el-tag>
                </div>

                <p class="mt-5 min-h-[48px] text-sm leading-6 text-slate-500">{{ template.description }}</p>

                <div class="mt-4 flex flex-wrap gap-2">
                  <span
                    v-for="highlight in template.highlights"
                    :key="highlight"
                    class="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600"
                  >
                    {{ highlight }}
                  </span>
                </div>

                <div class="mt-5 space-y-2 rounded-2xl bg-slate-50 px-4 py-3">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-400">绑定流程</span>
                    <span class="font-medium text-slate-700">{{ template.flowName }}</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-400">模板负责人</span>
                    <span class="text-slate-700">{{ template.owner }}</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-400">更新时间</span>
                    <span class="text-slate-700">{{ template.updatedAt }}</span>
                  </div>
                </div>

                <div class="mt-5 flex flex-wrap justify-end gap-3">
                  <el-button v-if="canEditTemplates" text type="danger" @click="confirmDeleteTemplate(template)">
                    删除模板
                  </el-button>
                  <el-button type="primary" text @click="openTemplateEdit(template)">修改配置</el-button>
                  <el-button v-if="canEditTemplates" text>复制模板</el-button>
                </div>
              </el-card>
            </div>
          </div>
        </section>
      </template>

      <template v-else-if="activeSection === 'custom-archive'">
        <custom-archive-management-panel />
      </template>

      <template v-else-if="activeSection === 'expense-type'">
        <expense-type-management-panel />
      </template>

      <template v-else>
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="flex min-h-[520px] items-center justify-center">
            <div class="max-w-xl text-center">
              <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-blue-50 text-blue-600">
                <el-icon :size="30"><Tools /></el-icon>
              </div>
              <h2 class="mt-6 text-2xl font-semibold text-slate-800">{{ currentNavLabel }}</h2>
              <p class="mt-3 leading-7 text-slate-500">
                {{ currentNavTip }}
              </p>
            </div>
          </div>
        </el-card>
      </template>
    </div>

    <template-type-dialog
      v-model="templateDialogVisible"
      :options="templateTypes"
      @select="handleTemplateSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CopyDocument,
  Document,
  Files,
  Plus,
  Search,
  Tools,
  TrendCharts
} from '@element-plus/icons-vue'
import {
  processApi,
  type ProcessCenterOverview,
  type ProcessTemplateCard,
  type ProcessTemplateTypeOption
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import CustomArchiveManagementPanel from '@/components/process/CustomArchiveManagementPanel.vue'
import ExpenseTypeManagementPanel from '@/components/process/ExpenseTypeManagementPanel.vue'
import ProcessWorkbenchSidebar from '@/components/process/ProcessWorkbenchSidebar.vue'
import TemplateTypeDialog from '@/components/process/TemplateTypeDialog.vue'

const route = useRoute()
const router = useRouter()

const overview = ref<ProcessCenterOverview | null>(null)
const templateTypes = ref<ProcessTemplateTypeOption[]>([])
const templateDialogVisible = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('all')
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])

const canCreateTemplates = computed(() => hasPermission('expense:process_management:create', permissionCodes.value))
const canEditTemplates = computed(() =>
  hasPermission('expense:process_management:edit', permissionCodes.value) ||
  hasPermission('expense:process_management:publish', permissionCodes.value) ||
  hasPermission('expense:process_management:disable', permissionCodes.value)
)

const activeSection = computed(() => {
  const section = route.query.section
  return typeof section === 'string' ? section : 'document-flow'
})
const currentNav = computed(() => overview.value?.navItems.find((item) => item.key === activeSection.value))
const currentNavLabel = computed(() => currentNav.value?.label || '\u6d41\u7a0b\u914d\u7f6e')
const currentNavTip = computed(() => currentNav.value?.tip || '\u8fd9\u4e2a\u914d\u7f6e\u6a21\u5757\u6b63\u5728\u5efa\u8bbe\u4e2d\uff0c\u540e\u7eed\u4f1a\u7ee7\u7eed\u8865\u9f50\u771f\u5b9e\u80fd\u529b\u3002')
const summaryCards = computed(() => {
  if (!overview.value) {
    return []
  }
  return [
    {
      label: '\u6a21\u677f\u603b\u6570',
      value: overview.value.summary.totalTemplates,
      tip: '\u5df2\u7eb3\u5165\u6d41\u7a0b\u4e2d\u5fc3\u7ef4\u62a4\u7684\u5168\u90e8\u5355\u636e\u6a21\u677f',
      icon: Files,
      bg: 'linear-gradient(135deg, #2563eb 0%, #60a5fa 100%)'
    },
    {
      label: '\u5df2\u542f\u7528\u6a21\u677f',
      value: overview.value.summary.enabledTemplates,
      tip: '\u5f53\u524d\u6b63\u5f0f\u6295\u5165\u4f7f\u7528\u7684\u6a21\u677f\u6570\u91cf',
      icon: CircleCheckFilled,
      bg: 'linear-gradient(135deg, #0f766e 0%, #2dd4bf 100%)'
    },
    {
      label: '\u8349\u7a3f\u6a21\u677f',
      value: overview.value.summary.draftTemplates,
      tip: '\u5f85\u7ee7\u7eed\u5b8c\u5584\u5e76\u53d1\u5e03\u7684\u6a21\u677f',
      icon: Document,
      bg: 'linear-gradient(135deg, #ea580c 0%, #fdba74 100%)'
    },
    {
      label: 'AI \u5ba1\u6838\u63a5\u5165',
      value: overview.value.summary.aiAuditTemplates,
      tip: '\u5df2\u542f\u7528 AI \u98ce\u9669\u8bc6\u522b\u80fd\u529b\u7684\u6a21\u677f',
      icon: TrendCharts,
      bg: 'linear-gradient(135deg, #7c3aed 0%, #c4b5fd 100%)'
    }
  ]
})
const filteredCategories = computed(() => {
  if (!overview.value) {
    return []
  }
  return overview.value.categories
    .filter((category) => activeCategory.value === 'all' || category.code === activeCategory.value)
    .map((category) => {
      const keyword = searchKeyword.value.trim()
      const templates = category.templates.filter((template) => {
        if (!keyword) {
          return true
        }
        return (
          template.name.includes(keyword) ||
          template.templateType.includes(keyword) ||
          template.flowName.includes(keyword)
        )
      })
      return {
        ...category,
        templateCount: templates.length,
        templates
      }
    })
    .filter((category) => category.templates.length > 0)
})
function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
onMounted(async () => {
  await loadOverview()
})
async function loadOverview() {
  try {
    const [overviewRes, typeRes] = await Promise.all([
      processApi.getOverview(),
      processApi.getTemplateTypes()
    ])
    overview.value = overviewRes.data
    templateTypes.value = typeRes.data
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '\u52a0\u8f7d\u6d41\u7a0b\u7ba1\u7406\u9875\u9762\u5931\u8d25'))
  }
}
const handleSectionChange = (section: string) => {
  router.replace({
    path: '/expense/workbench/process-management',
    query: { section }
  })
}

const openTemplateDialog = () => {
  if (!canCreateTemplates.value) {
    ElMessage.warning('当前账号没有新增单据模板权限')
    return
  }
  templateDialogVisible.value = true
}

const handleTemplateSelect = (templateType: string) => {
  templateDialogVisible.value = false
  router.push({
    name: 'expense-workbench-process-management-create',
    params: { templateType }
  })
}

const openTemplateEdit = (template: ProcessTemplateCard) => {
  if (!template.templateTypeCode) {
    ElMessage.warning('\u5f53\u524d\u6a21\u677f\u7f3a\u5c11\u7c7b\u578b\u4fe1\u606f\uff0c\u6682\u65f6\u65e0\u6cd5\u8fdb\u5165\u4fee\u6539\u914d\u7f6e')
    return
  }
  router.push({
    name: 'expense-workbench-process-management-create',
    params: {
      templateType: template.templateTypeCode
    },
    query: {
      templateId: String(template.id)
    }
  })
}
const confirmDeleteTemplate = async (template: ProcessTemplateCard) => {
  try {
    await ElMessageBox.confirm(
      '\u786e\u8ba4\u5220\u9664\u6a21\u677f\u201c' + template.name + '\u201d\u5417\uff1f\u5220\u9664\u540e\u5361\u7247\u5c06\u4ece\u7ba1\u7406\u9875\u9690\u85cf\uff0c\u5386\u53f2\u5355\u636e\u4e0d\u53d7\u5f71\u54cd\u3002',
      '\u5220\u9664\u6a21\u677f',
      {
        type: 'warning',
        confirmButtonText: '\u786e\u8ba4\u5220\u9664',
        cancelButtonText: '\u53d6\u6d88'
      }
    )
    await processApi.deleteTemplate(template.id)
    ElMessage.success('\u6a21\u677f\u5df2\u5220\u9664')
    await loadOverview()
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '\u5220\u9664\u6a21\u677f\u5931\u8d25'))
  }
}
</script>

<style scoped>
.stat-card {
  min-height: 150px;
}

.template-card {
  border: 1px solid #e2e8f0 !important;
}

.hero-primary-btn {
  background: #ffffff !important;
  border-color: #ffffff !important;
  color: #2563eb !important;
}

.hero-secondary-btn {
  background: rgba(255, 255, 255, 0.14) !important;
  border-color: rgba(255, 255, 255, 0.22) !important;
  color: #ffffff !important;
}
</style>


