<template>
  <div class="flex gap-6 items-start">
    <process-workbench-sidebar
      :items="overview?.navItems || []"
      :active-key="activeSection"
      @select="handleSectionChange"
    />

    <div class="flex-1 space-y-6">
      <template v-if="activeSection === 'document-flow'">
        <section class="rounded-[32px] overflow-hidden bg-gradient-to-r from-blue-600 via-sky-500 to-cyan-400 text-white shadow-lg">
          <div class="px-8 py-8 flex flex-col lg:flex-row lg:items-end lg:justify-between gap-6">
            <div>
              <p class="text-blue-100 text-sm tracking-[0.22em] uppercase">Flow Studio</p>
              <h1 class="text-3xl font-bold mt-3">单据与流程</h1>
              <p class="text-blue-50/90 mt-3 max-w-2xl leading-7">
                把单据模板、审批流、付款规则与 AI 审核能力统一放在一个工作区中，
                让财务同事能更快梳理流程、维护模板并推动业务上线。
              </p>
            </div>

            <div class="flex flex-wrap gap-3">
              <el-button type="primary" class="hero-primary-btn" @click="openTemplateDialog">
                添加单据
              </el-button>
              <el-button class="hero-secondary-btn">
                批量归档
              </el-button>
            </div>
          </div>
        </section>

        <section class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
          <el-card v-for="stat in summaryCards" :key="stat.label" class="stat-card !rounded-3xl !shadow-sm">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-slate-500">{{ stat.label }}</p>
                <p class="text-3xl font-bold text-slate-800 mt-2">{{ stat.value }}</p>
                <p class="text-xs text-slate-400 mt-3">{{ stat.tip }}</p>
              </div>
              <div class="w-12 h-12 rounded-2xl flex items-center justify-center" :style="{ background: stat.bg }">
                <el-icon :size="22" class="text-white">
                  <component :is="stat.icon" />
                </el-icon>
              </div>
            </div>
          </el-card>
        </section>

        <el-card class="!shadow-sm !rounded-3xl">
          <div class="flex flex-col xl:flex-row xl:items-center gap-4 justify-between">
            <div class="flex flex-wrap gap-3">
              <el-button type="primary" :icon="Plus" @click="openTemplateDialog">添加单据</el-button>
              <el-button :icon="CopyDocument">复制分类</el-button>
            </div>

            <div class="flex flex-col sm:flex-row gap-3 w-full xl:w-auto">
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
                <p class="text-sm text-slate-400 mt-1">{{ category.description }}</p>
              </div>
              <span class="text-sm text-blue-500 bg-blue-50 px-3 py-1 rounded-full">
                {{ category.templateCount }} 个模板
              </span>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-3 gap-5">
              <el-card
                v-for="template in category.templates"
                :key="template.id"
                class="template-card !rounded-3xl !shadow-sm"
              >
                <div class="flex items-start justify-between gap-4">
                  <div class="flex items-start gap-4">
                    <div
                      class="w-12 h-12 rounded-2xl flex items-center justify-center text-white shadow-sm"
                      :style="{ background: template.color }"
                    >
                      <el-icon :size="22"><Document /></el-icon>
                    </div>
                    <div>
                      <h3 class="text-lg font-semibold text-slate-800">{{ template.name }}</h3>
                      <p class="text-sm text-slate-400 mt-1">{{ template.templateType }} · {{ template.businessDomain }}</p>
                    </div>
                  </div>

                  <el-tag size="small" type="primary" effect="plain">{{ template.templateCode }}</el-tag>
                </div>

                <p class="text-sm text-slate-500 leading-6 mt-5 min-h-[48px]">{{ template.description }}</p>

                <div class="flex flex-wrap gap-2 mt-4">
                  <span
                    v-for="highlight in template.highlights"
                    :key="highlight"
                    class="text-xs text-slate-600 bg-slate-100 px-2.5 py-1 rounded-full"
                  >
                    {{ highlight }}
                  </span>
                </div>

                <div class="rounded-2xl bg-slate-50 px-4 py-3 mt-5 space-y-2">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-400">绑定流程</span>
                    <span class="text-slate-700 font-medium">{{ template.flowName }}</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-400">维护人</span>
                    <span class="text-slate-700">{{ template.owner }}</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-400">更新时间</span>
                    <span class="text-slate-700">{{ template.updatedAt }}</span>
                  </div>
                </div>

                <div class="flex gap-3 mt-5">
                  <el-button type="primary" text>查看配置</el-button>
                  <el-button text>复制模板</el-button>
                </div>
              </el-card>
            </div>
          </div>
        </section>
      </template>

      <template v-else>
        <el-card class="!rounded-3xl !shadow-sm">
          <div class="min-h-[520px] flex items-center justify-center">
            <div class="text-center max-w-xl">
              <div class="w-16 h-16 rounded-3xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto">
                <el-icon :size="30"><Tools /></el-icon>
              </div>
              <h2 class="text-2xl font-semibold text-slate-800 mt-6">{{ currentNavLabel }}</h2>
              <p class="text-slate-500 mt-3 leading-7">
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
import { ElMessage } from 'element-plus'
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
import { processApi, type ProcessCenterOverview, type ProcessTemplateTypeOption } from '@/api'
import ProcessWorkbenchSidebar from '@/components/process/ProcessWorkbenchSidebar.vue'
import TemplateTypeDialog from '@/components/process/TemplateTypeDialog.vue'

const route = useRoute()
const router = useRouter()

const overview = ref<ProcessCenterOverview | null>(null)
const templateTypes = ref<ProcessTemplateTypeOption[]>([])
const templateDialogVisible = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('all')

const activeSection = computed(() => {
  const section = route.query.section
  return typeof section === 'string' ? section : 'document-flow'
})

const currentNav = computed(() =>
  overview.value?.navItems.find((item) => item.key === activeSection.value)
)

const currentNavLabel = computed(() => currentNav.value?.label || '流程配置')
const currentNavTip = computed(
  () => currentNav.value?.tip || '这个配置模块正在建设中，后续会继续补齐真实能力。'
)

const summaryCards = computed(() => {
  if (!overview.value) {
    return []
  }

  return [
    {
      label: '模板总数',
      value: overview.value.summary.totalTemplates,
      tip: '已纳入流程中心维护的全部单据模板',
      icon: Files,
      bg: 'linear-gradient(135deg, #2563eb 0%, #60a5fa 100%)'
    },
    {
      label: '已启用模板',
      value: overview.value.summary.enabledTemplates,
      tip: '当前被业务部门正式使用的模板数量',
      icon: CircleCheckFilled,
      bg: 'linear-gradient(135deg, #0f766e 0%, #2dd4bf 100%)'
    },
    {
      label: '草稿模板',
      value: overview.value.summary.draftTemplates,
      tip: '待继续完善并发布的模板',
      icon: Document,
      bg: 'linear-gradient(135deg, #ea580c 0%, #fdba74 100%)'
    },
    {
      label: 'AI 审核接入',
      value: overview.value.summary.aiAuditTemplates,
      tip: '已启用 AI 风险识别能力的模板',
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
      const templates = category.templates.filter((template) => {
        const keyword = searchKeyword.value.trim()
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

onMounted(async () => {
  try {
    const [overviewRes, typeRes] = await Promise.all([
      processApi.getOverview(),
      processApi.getTemplateTypes()
    ])

    if (overviewRes.code === 200) {
      overview.value = overviewRes.data
    }

    if (typeRes.code === 200) {
      templateTypes.value = typeRes.data
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载流程管理页面失败')
  }
})

const handleSectionChange = (section: string) => {
  router.replace({
    path: '/expense/workbench/process-management',
    query: { section }
  })
}

const openTemplateDialog = () => {
  templateDialogVisible.value = true
}

const handleTemplateSelect = (templateType: string) => {
  templateDialogVisible.value = false
  router.push({
    name: 'expense-workbench-process-management-create',
    params: { templateType }
  })
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
