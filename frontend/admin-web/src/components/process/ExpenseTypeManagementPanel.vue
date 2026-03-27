<template>
  <div class="grid grid-cols-1 gap-6 xl:grid-cols-[320px,minmax(0,1fr)]">
    <el-card class="!rounded-3xl !shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-slate-800">费用类型</h2>
            <p class="mt-1 text-sm text-slate-400">左侧按树形维护费用类型，右侧配置基础信息和发票税规则</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="createDraft">新增费用类型</el-button>
        </div>
      </template>

      <div class="space-y-4">
        <el-input v-model="keyword" placeholder="搜索类型名称或编码" :prefix-icon="Search" clearable />

        <div v-loading="loadingTree" class="min-h-[480px]">
          <el-tree
            v-if="filteredTree.length"
            ref="treeRef"
            :data="filteredTree"
            node-key="id"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            :props="{ label: 'expenseName', children: 'children' }"
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <div class="flex min-w-0 flex-1 items-center justify-between gap-3 py-1">
                <div class="min-w-0">
                  <p class="truncate text-sm font-medium text-slate-800">{{ data.expenseName }}</p>
                  <p class="truncate font-mono text-xs text-slate-400">{{ data.expenseCode }}</p>
                </div>
                <el-tag :type="data.status === 1 ? 'success' : 'info'" size="small">
                  {{ data.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
            </template>
          </el-tree>

          <el-empty v-else description="暂无费用类型">
            <el-button type="primary" @click="createDraft">新增费用类型</el-button>
          </el-empty>
        </div>
      </div>
    </el-card>

    <el-card class="!rounded-3xl !shadow-sm">
      <template #header>
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-xl font-semibold text-slate-800">{{ formTitle }}</h2>
              <el-tag v-if="form" effect="plain">{{ form?.status === 1 ? '启用中' : '已停用' }}</el-tag>
            </div>
            <p class="mt-2 font-mono text-sm text-slate-400">{{ form?.expenseCode || '请先录入 6 位或 8 位完整类型编码' }}</p>
          </div>

          <div class="flex flex-wrap gap-3">
            <el-button @click="refreshData">刷新</el-button>
            <el-button v-if="form?.id" type="danger" plain :icon="Delete" @click="deleteExpenseType">删除</el-button>
            <el-button type="primary" :icon="Check" :loading="saving" :disabled="!form" @click="saveExpenseType">保存</el-button>
          </div>
        </div>
      </template>

      <div v-if="loadingDetail" class="py-8">
        <el-skeleton animated :rows="10" />
      </div>

      <template v-else-if="form">
        <div class="mb-6 flex flex-wrap items-center gap-3">
          <el-button :type="activeAnchor === 'basic' ? 'primary' : 'default'" plain @click="scrollToSection('basic')">
            基础信息
          </el-button>
          <el-button :type="activeAnchor === 'invoice-tax' ? 'primary' : 'default'" plain @click="scrollToSection('invoice-tax')">
            发票与税
          </el-button>
        </div>

        <el-form label-position="top" class="space-y-6">
          <el-card id="basic" class="section-card !rounded-3xl !shadow-none">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-semibold text-slate-800">基础信息</span>
                <el-switch v-model="statusSwitch" inline-prompt active-text="启用" inactive-text="停用" />
              </div>
            </template>

            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <el-form-item label="类型名称" required>
                <el-input v-model="form.expenseName" placeholder="请输入费用类型名称" />
              </el-form-item>

              <el-form-item label="完整类型编码" required>
                <el-input v-model="form.expenseCode" maxlength="8" placeholder="请输入 6 位或 8 位数字编码" />
                <div class="mt-2 rounded-2xl bg-slate-50 px-4 py-3 text-xs leading-6 text-slate-500">
                  6 位为一级类型，8 位为二级类型；8 位编码会按前 6 位自动挂到上级，前 4 位仅用于同大类归组。
                </div>
              </el-form-item>

              <el-form-item label="类型说明" class="xl:col-span-2">
                <el-input
                  v-model="form.expenseDescription"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入费用类型说明"
                />
              </el-form-item>

              <el-form-item label="限定以下部门使用">
                <el-select
                  v-model="form.scopeDeptIds"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="不选则默认全部部门可用"
                >
                  <el-option
                    v-for="item in meta?.departmentOptions || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="限定以下人员使用">
                <el-select
                  v-model="form.scopeUserIds"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="不选则默认全部人员可用"
                >
                  <el-option
                    v-for="item in meta?.userOptions || []"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </div>
          </el-card>

          <el-card id="invoice-tax" class="section-card !rounded-3xl !shadow-none">
            <template #header>
              <span class="font-semibold text-slate-800">发票与税</span>
            </template>

            <div class="space-y-6">
              <div>
                <p class="mb-3 text-sm font-medium text-slate-700">是否免票</p>
                <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <button
                    v-for="option in meta?.invoiceFreeOptions || []"
                    :key="option.value"
                    type="button"
                    class="config-card rounded-3xl border p-5 text-left transition-all"
                    :class="form.invoiceFreeMode === option.value ? 'config-card-active' : ''"
                    @click="form.invoiceFreeMode = option.value"
                  >
                    <p class="font-semibold text-slate-800">{{ option.label }}</p>
                    <p class="mt-2 text-sm leading-6 text-slate-500">{{ option.description }}</p>
                  </button>
                </div>
              </div>

              <div>
                <p class="mb-3 text-sm font-medium text-slate-700">税额抵扣与转出</p>
                <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
                  <button
                    v-for="option in meta?.taxDeductionOptions || []"
                    :key="option.value"
                    type="button"
                    class="config-card rounded-3xl border p-5 text-left transition-all"
                    :class="form.taxDeductionMode === option.value ? 'config-card-active' : ''"
                    @click="form.taxDeductionMode = option.value"
                  >
                    <p class="font-semibold text-slate-800">{{ option.label }}</p>
                    <p class="mt-2 text-sm leading-6 text-slate-500">{{ option.description }}</p>
                  </button>
                </div>
              </div>

              <div>
                <p class="mb-3 text-sm font-medium text-slate-700">价税分离规则</p>
                <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <button
                    v-for="option in meta?.taxSeparationOptions || []"
                    :key="option.value"
                    type="button"
                    class="config-card rounded-3xl border p-5 text-left transition-all"
                    :class="form.taxSeparationMode === option.value ? 'config-card-active' : ''"
                    @click="form.taxSeparationMode = option.value"
                  >
                    <p class="font-semibold text-slate-800">{{ option.label }}</p>
                    <p class="mt-2 text-sm leading-6 text-slate-500">{{ option.description }}</p>
                  </button>
                </div>
              </div>
            </div>
          </el-card>
        </el-form>
      </template>

      <el-empty v-else description="请先在左侧选择费用类型或新增一个类型">
        <el-button type="primary" @click="createDraft">新增费用类型</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Plus, Search } from '@element-plus/icons-vue'
import {
  processApi,
  type ProcessExpenseTypeDetail,
  type ProcessExpenseTypeMeta,
  type ProcessExpenseTypeTreeNode
} from '@/api'

const treeRef = ref<any>()
const loadingTree = ref(false)
const loadingDetail = ref(false)
const saving = ref(false)
const keyword = ref('')
const activeExpenseTypeId = ref<number>()
const activeAnchor = ref<'basic' | 'invoice-tax'>('basic')

const treeData = ref<ProcessExpenseTypeTreeNode[]>([])
const meta = ref<ProcessExpenseTypeMeta | null>(null)
const form = ref<ProcessExpenseTypeDetail | null>(null)

const createEmptyForm = (): ProcessExpenseTypeDetail => ({
  expenseCode: '',
  expenseName: '',
  expenseDescription: '',
  scopeDeptIds: [],
  scopeUserIds: [],
  invoiceFreeMode: meta.value?.invoiceFreeOptions[0]?.value || 'FREE',
  taxDeductionMode: meta.value?.taxDeductionOptions[0]?.value || 'DEFAULT',
  taxSeparationMode: meta.value?.taxSeparationOptions[0]?.value || 'SEPARATE',
  status: 1
})

const formTitle = computed(() => {
  if (!form.value) {
    return '费用类型配置'
  }
  return form.value.expenseName || '新建费用类型'
})

const statusSwitch = computed({
  get: () => form.value?.status === 1,
  set: (value: boolean) => {
    if (!form.value) {
      return
    }
    form.value.status = value ? 1 : 0
  }
})

const filteredTree = computed(() => filterTree(treeData.value, keyword.value.trim().toLowerCase()))

onMounted(async () => {
  await loadInitialData()
})

const loadInitialData = async (preferredId?: number) => {
  loadingTree.value = true
  try {
    const [treeRes, metaRes] = await Promise.all([
      processApi.listExpenseTypesTree(),
      processApi.getExpenseTypeMeta()
    ])
    treeData.value = treeRes.data || []
    meta.value = metaRes.data

    const targetId = preferredId || activeExpenseTypeId.value || treeData.value[0]?.id
    if (targetId) {
      await selectExpenseType(targetId)
    } else {
      form.value = createEmptyForm()
      activeExpenseTypeId.value = undefined
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载费用类型失败')
  } finally {
    loadingTree.value = false
  }
}

const refreshData = async () => {
  await loadInitialData(activeExpenseTypeId.value)
}

const handleNodeClick = (node: ProcessExpenseTypeTreeNode) => {
  selectExpenseType(node.id)
}

const selectExpenseType = async (id: number) => {
  loadingDetail.value = true
  try {
    const res = await processApi.getExpenseTypeDetail(id)
    form.value = {
      ...res.data,
      scopeDeptIds: res.data.scopeDeptIds || [],
      scopeUserIds: res.data.scopeUserIds || []
    }
    activeExpenseTypeId.value = id
    await nextTick()
    treeRef.value?.setCurrentKey(id)
  } catch (error: any) {
    ElMessage.error(error.message || '加载费用类型详情失败')
  } finally {
    loadingDetail.value = false
  }
}

const createDraft = () => {
  activeExpenseTypeId.value = undefined
  form.value = createEmptyForm()
  nextTick(() => treeRef.value?.setCurrentKey(null))
}

const saveExpenseType = async () => {
  if (!form.value) {
    return
  }
  if (!form.value.expenseName.trim()) {
    ElMessage.warning('请先输入类型名称')
    return
  }
  if (!/^\\d{6}(\\d{2})?$/.test(form.value.expenseCode.trim())) {
    ElMessage.warning('请输入 6 位或 8 位数字编码')
    return
  }

  saving.value = true
  try {
    const payload = {
      expenseName: form.value.expenseName.trim(),
      expenseDescription: form.value.expenseDescription?.trim() || '',
      expenseCode: form.value.expenseCode.trim(),
      scopeDeptIds: form.value.scopeDeptIds || [],
      scopeUserIds: form.value.scopeUserIds || [],
      invoiceFreeMode: form.value.invoiceFreeMode,
      taxDeductionMode: form.value.taxDeductionMode,
      taxSeparationMode: form.value.taxSeparationMode,
      status: form.value.status ?? 1
    }

    const res = form.value.id
      ? await processApi.updateExpenseType(form.value.id, payload)
      : await processApi.createExpenseType(payload)

    ElMessage.success(form.value.id ? '费用类型已更新' : '费用类型已创建')
    await loadInitialData(res.data.id)
  } catch (error: any) {
    ElMessage.error(error.message || '保存费用类型失败')
  } finally {
    saving.value = false
  }
}

const deleteExpenseType = async () => {
  if (!form.value?.id) {
    return
  }
  try {
    await ElMessageBox.confirm('删除后不可恢复，确认删除当前费用类型吗？', '删除确认', {
      type: 'warning'
    })
    await processApi.deleteExpenseType(form.value.id)
    ElMessage.success('费用类型已删除')
    form.value = null
    activeExpenseTypeId.value = undefined
    await loadInitialData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除费用类型失败')
    }
  }
}

const scrollToSection = (id: 'basic' | 'invoice-tax') => {
  activeAnchor.value = id
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const filterTree = (nodes: ProcessExpenseTypeTreeNode[], search: string): ProcessExpenseTypeTreeNode[] => {
  if (!search) {
    return nodes
  }

  return nodes
    .map((node) => {
      const children = filterTree(node.children || [], search)
      const matched = `${node.expenseName}${node.expenseCode}`.toLowerCase().includes(search)
      if (!matched && !children.length) {
        return null
      }
      return {
        ...node,
        children
      }
    })
    .filter(Boolean) as ProcessExpenseTypeTreeNode[]
}
</script>

<style scoped>
.section-card {
  scroll-margin-top: 88px;
}

.config-card {
  border-color: rgb(226 232 240 / 1);
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.config-card:hover {
  border-color: rgb(59 130 246 / 0.45);
  transform: translateY(-1px);
}

.config-card-active {
  border-color: rgb(37 99 235 / 1);
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.12);
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
}
</style>
