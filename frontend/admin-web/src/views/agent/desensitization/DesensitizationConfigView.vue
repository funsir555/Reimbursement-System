<template>
  <div class="desensitization-config-page">
    <el-card class="header-card">
      <h2>脱敏规则配置</h2>
      <p class="description">配置敏感数据的识别规则和脱敏策略，保护数据安全</p>
    </el-card>

    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 实体类型配置 -->
      <el-tab-pane label="实体类型配置" name="entityTypes">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>敏感实体类型</span>
              <el-button type="primary" @click="handleAddEntityType">
                <el-icon><Plus /></el-icon>
                添加实体类型
              </el-button>
            </div>
          </template>

          <el-table :data="entityTypes" stripe style="width: 100%">
            <el-table-column prop="code" label="编码" width="120" />
            <el-table-column prop="name" label="名称" width="120" />
            <el-table-column prop="description" label="描述" min-width="150" />
            <el-table-column prop="placeholderPrefix" label="占位符前缀" width="120" />
            <el-table-column label="正则表达式" min-width="200">
              <template #default="{ row }">
                <el-tag
                  v-for="(pattern, index) in row.regexPatterns"
                  :key="index"
                  size="small"
                  style="margin: 2px"
                >
                  {{ pattern }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="示例" min-width="150">
              <template #default="{ row }">
                <el-tag
                  v-for="(example, index) in row.examples"
                  :key="index"
                  type="info"
                  size="small"
                  style="margin: 2px"
                >
                  {{ example }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-switch
                  v-model="row.enabled"
                  @change="handleToggleEnabled(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleEditEntityType(row)">
                  编辑
                </el-button>
                <el-button link type="danger" @click="handleDeleteEntityType(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 脱敏测试 -->
      <el-tab-pane label="脱敏测试" name="test">
        <el-card>
          <el-form :model="testForm" label-width="100px">
            <el-form-item label="测试文本">
              <el-input
                v-model="testForm.text"
                type="textarea"
                :rows="6"
                placeholder="请输入包含敏感信息的测试文本，例如：张三的报销单EXP20240101，金额1200元，手机号13812345678"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="handleTest"
                :loading="testLoading"
              >
                执行脱敏测试
              </el-button>
              <el-button @click="handleClearTest">清空</el-button>
            </el-form-item>

            <el-divider v-if="testResult" />

            <el-form-item v-if="testResult" label="脱敏结果">
              <div class="test-result">
                <div class="result-section">
                  <div class="result-label">原始文本：</div>
                  <div class="result-content original">{{ testResult.originalText }}</div>
                </div>

                <el-icon class="arrow-icon"><ArrowDown /></el-icon>

                <div class="result-section">
                  <div class="result-label">脱敏后文本：</div>
                  <div class="result-content masked">{{ testResult.maskedText }}</div>
                </div>

                <el-icon class="arrow-icon"><ArrowDown /></el-icon>

                <div class="result-section">
                  <div class="result-label">还原后文本：</div>
                  <div class="result-content restored">{{ testResult.restoredText }}</div>
                </div>

                <div class="result-section">
                  <div class="result-label">映射关系：</div>
                  <div class="result-content">
                    <el-table :data="mappingTableData" size="small" border>
                      <el-table-column prop="placeholder" label="占位符" width="150" />
                      <el-table-column prop="original" label="原始值" />
                    </el-table>
                  </div>
                </div>

                <div class="result-section">
                  <div class="result-label">使用策略：</div>
                  <el-tag type="info">正则规则</el-tag>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 映射表管理 -->
      <el-tab-pane label="映射表管理" name="mappings">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>脱敏映射表</span>
              <div>
                <el-button type="primary" @click="handleAddMapping">
                  <el-icon><Plus /></el-icon>
                  新增映射
                </el-button>
                <el-button @click="handleRefreshMappings">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
                <el-button type="warning" @click="handleCleanupMappings">
                  <el-icon><Delete /></el-icon>
                  清理过期
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="mappings" stripe v-loading="mappingsLoading">
            <el-table-column prop="id" label="映射ID" width="280" show-overflow-tooltip />
            <el-table-column prop="sessionId" label="会话ID" width="200" show-overflow-tooltip />
            <el-table-column label="映射数量" width="100">
              <template #default="{ row }">
                {{ Object.keys(row.mappings || {}).length }}
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="过期时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.expiresAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.expired ? 'danger' : 'success'">
                  {{ row.expired ? '已过期' : '有效' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewMapping(row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            layout="total, prev, pager, next, sizes"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
            style="margin-top: 20px; justify-content: flex-end"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 实体类型编辑对话框 -->
    <el-dialog
      v-model="entityTypeDialogVisible"
      :title="entityTypeForm.id ? '编辑实体类型' : '添加实体类型'"
      :close-on-press-escape="false"
      width="700px"
    >
      <el-form
        ref="entityTypeFormRef"
        :model="entityTypeForm"
        :rules="entityTypeRules"
        label-width="120px"
      >
        <el-form-item label="编码" prop="code">
          <el-input
            v-model="entityTypeForm.code"
            placeholder="如：PERSON"
            :disabled="!!entityTypeForm.id"
          />
        </el-form-item>

        <el-form-item label="名称" prop="name">
          <el-input v-model="entityTypeForm.name" placeholder="如：人名" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="entityTypeForm.description"
            type="textarea"
            :rows="2"
            placeholder="如：员工姓名、客户姓名等"
          />
        </el-form-item>

        <el-form-item label="占位符前缀" prop="placeholderPrefix">
          <el-input
            v-model="entityTypeForm.placeholderPrefix"
            placeholder="如：PERSON"
          />
        </el-form-item>

        <el-form-item label="正则表达式" prop="regexPatterns">
          <div class="array-input">
            <el-input
              v-for="(pattern, index) in entityTypeForm.regexPatterns"
              :key="index"
              v-model="entityTypeForm.regexPatterns[index]"
              placeholder="输入正则表达式"
              style="margin-bottom: 8px"
            >
              <template #append>
                <el-button
                  @click="handleRemovePattern(index)"
                  :icon="Delete"
                />
              </template>
            </el-input>
            <el-button @click="handleAddPattern" :icon="Plus" style="width: 100%">
              添加正则表达式
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="示例" prop="examples">
          <div class="array-input">
            <el-input
              v-for="(example, index) in entityTypeForm.examples"
              :key="index"
              v-model="entityTypeForm.examples[index]"
              placeholder="输入示例"
              style="margin-bottom: 8px"
            >
              <template #append>
                <el-button
                  @click="handleRemoveExample(index)"
                  :icon="Delete"
                />
              </template>
            </el-input>
            <el-button @click="handleAddExample" :icon="Plus" style="width: 100%">
              添加示例
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="entityTypeForm.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="entityTypeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEntityType" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 映射详情对话框 -->
    <el-dialog v-model="mappingDetailDialogVisible" title="映射详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="映射ID">
          {{ currentMapping?.id }}
        </el-descriptions-item>
        <el-descriptions-item label="会话ID">
          {{ currentMapping?.sessionId }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatTime(currentMapping?.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="过期时间">
          {{ formatTime(currentMapping?.expiresAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentMapping?.expired ? 'danger' : 'success'">
            {{ currentMapping?.expired ? '已过期' : '有效' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4>映射关系</h4>
      <el-table :data="currentMappingTableData" border size="small">
        <el-table-column prop="placeholder" label="占位符" width="150" />
        <el-table-column prop="original" label="原始值" />
      </el-table>
    </el-dialog>

    <!-- 新增映射对话框 -->
    <el-dialog v-model="mappingDialogVisible" title="新增映射关系" width="680px">
      <el-form
        ref="mappingFormRef"
        :model="mappingForm"
        :rules="mappingRules"
        label-width="110px"
      >
        <el-form-item label="会话ID" prop="sessionId">
          <el-input v-model="mappingForm.sessionId" placeholder="请输入会话ID或业务追踪ID" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-input-number v-model="mappingForm.ttlHours" :min="1" :max="168" :step="1" />
          <span class="form-tip">小时</span>
        </el-form-item>
        <el-form-item label="映射关系">
          <div class="mapping-editor">
            <div v-for="(item, index) in mappingForm.items" :key="index" class="mapping-row">
              <el-input v-model="item.placeholder" placeholder="占位符，如 PERSON_001" />
              <el-input v-model="item.original" placeholder="原始值，如 张三" />
              <el-button :icon="Delete" @click="handleRemoveMappingItem(index)" />
            </div>
            <el-button :icon="Plus" @click="handleAddMappingItem">添加一行</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mappingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingMapping" @click="handleSaveMapping">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Refresh, ArrowDown } from '@element-plus/icons-vue'
import { desensitizationApi } from '../../../api/agent/desensitization'
import type { FormInstance, FormRules } from 'element-plus'

// 激活的标签页
const activeTab = ref('entityTypes')

// 实体类型列表
const entityTypes = ref<any[]>([])

// 测试表单
const testForm = reactive({
  text: '',
  strategy: 'REGEX'
})

const testLoading = ref(false)
const testResult = ref<any>(null)

const mappingTableData = computed(() => {
  if (!testResult.value?.mappings) return []
  return Object.entries(testResult.value.mappings).map(([placeholder, original]) => ({
    placeholder,
    original
  }))
})

// 映射表列表
const mappings = ref<any[]>([])
const mappingsLoading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 实体类型对话框
const entityTypeDialogVisible = ref(false)
const entityTypeFormRef = ref<FormInstance>()
const entityTypeForm = reactive({
  id: null as number | null,
  code: '',
  name: '',
  description: '',
  placeholderPrefix: '',
  regexPatterns: [''],
  examples: [''],
  enabled: true
})

const entityTypeRules: FormRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  placeholderPrefix: [{ required: true, message: '请输入占位符前缀', trigger: 'blur' }]
}

const saving = ref(false)

// 映射详情对话框
const mappingDetailDialogVisible = ref(false)
const currentMapping = ref<any>(null)
const mappingDialogVisible = ref(false)
const mappingFormRef = ref<FormInstance>()
const savingMapping = ref(false)
const mappingForm = reactive({
  sessionId: '',
  ttlHours: 24,
  items: [
    { placeholder: '', original: '' }
  ]
})

const mappingRules: FormRules = {
  sessionId: [{ required: true, message: '请输入会话ID', trigger: 'blur' }]
}

const currentMappingTableData = computed(() => {
  if (!currentMapping.value?.mappings) return []
  return Object.entries(currentMapping.value.mappings).map(([placeholder, original]) => ({
    placeholder,
    original
  }))
})

// 加载实体类型
const loadEntityTypes = async () => {
  try {
    const res = await desensitizationApi.getEntityTypes()
    entityTypes.value = res.data || []
  } catch (error) {
    ElMessage.error('加载实体类型失败')
  }
}

// 添加实体类型
const handleAddEntityType = () => {
  Object.assign(entityTypeForm, {
    id: null,
    code: '',
    name: '',
    description: '',
    placeholderPrefix: '',
    regexPatterns: [''],
    examples: [''],
    enabled: true
  })
  entityTypeDialogVisible.value = true
}

// 编辑实体类型
const handleEditEntityType = (row: any) => {
  Object.assign(entityTypeForm, {
    id: row.id,
    code: row.code,
    name: row.name,
    description: row.description,
    placeholderPrefix: row.placeholderPrefix,
    regexPatterns: [...row.regexPatterns],
    examples: [...row.examples],
    enabled: row.enabled
  })
  entityTypeDialogVisible.value = true
}

// 保存实体类型
const handleSaveEntityType = async () => {
  if (!entityTypeFormRef.value) return

  await entityTypeFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const data = {
          code: entityTypeForm.code,
          name: entityTypeForm.name,
          description: entityTypeForm.description,
          placeholderPrefix: entityTypeForm.placeholderPrefix,
          regexPatterns: entityTypeForm.regexPatterns.filter(p => p),
          examples: entityTypeForm.examples.filter(e => e),
          enabled: entityTypeForm.enabled
        }

        const res = await desensitizationApi.saveEntityType(data)
        ElMessage.success(res.message || '保存成功')
        entityTypeDialogVisible.value = false
        await loadEntityTypes()
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
      } finally {
        saving.value = false
      }
    }
  })
}

// 删除实体类型
const handleDeleteEntityType = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该实体类型吗？', '提示', {
      type: 'warning'
    })

    const res = await desensitizationApi.deleteEntityType(row.code)
    ElMessage.success(res.message || '删除成功')
    await loadEntityTypes()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 切换启用状态
const handleToggleEnabled = async (row: any) => {
  try {
    const res = await desensitizationApi.updateEntityTypeEnabled(row.code, row.enabled)
    ElMessage.success(res.message || '更新成功')
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
    row.enabled = !row.enabled
  }
}

// 添加/删除正则表达式
const handleAddPattern = () => {
  entityTypeForm.regexPatterns.push('')
}

const handleRemovePattern = (index: number) => {
  entityTypeForm.regexPatterns.splice(index, 1)
}

// 添加/删除示例
const handleAddExample = () => {
  entityTypeForm.examples.push('')
}

const handleRemoveExample = (index: number) => {
  entityTypeForm.examples.splice(index, 1)
}

// 执行测试
const handleTest = async () => {
  if (!testForm.text) {
    ElMessage.warning('请输入测试文本')
    return
  }

  testLoading.value = true
  try {
    const res = await desensitizationApi.test({
      text: testForm.text,
      strategy: testForm.strategy
    })
    testResult.value = res.data
    ElMessage.success('测试完成')
  } catch (error: any) {
    ElMessage.error(error.message || '测试失败')
  } finally {
    testLoading.value = false
  }
}

// 清空测试
const handleClearTest = () => {
  testForm.text = ''
  testResult.value = null
}

// 加载映射表
const loadMappings = async () => {
  mappingsLoading.value = true
  try {
    const res = await desensitizationApi.getMappings({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    mappings.value = res.data?.items || []
    pagination.total = res.data?.total || 0
    pagination.page = res.data?.page || pagination.page
    pagination.pageSize = res.data?.pageSize || pagination.pageSize
  } catch (error) {
    ElMessage.error('加载映射表失败')
  } finally {
    mappingsLoading.value = false
  }
}

// 刷新映射表
const handleRefreshMappings = () => {
  loadMappings()
}

// 清理过期映射
const handleCleanupMappings = async () => {
  try {
    await ElMessageBox.confirm('确认清理所有过期映射吗？', '提示', {
      type: 'warning'
    })

    const res = await desensitizationApi.cleanupMappings()
    ElMessage.success(res.message || '清理完成')
    await loadMappings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清理失败')
    }
  }
}

// 查看映射详情
const handleViewMapping = (row: any) => {
  currentMapping.value = row
  mappingDetailDialogVisible.value = true
}

const handleAddMapping = () => {
  Object.assign(mappingForm, {
    sessionId: `manual_${Date.now()}`,
    ttlHours: 24,
    items: [{ placeholder: '', original: '' }]
  })
  mappingDialogVisible.value = true
}

const handleAddMappingItem = () => {
  mappingForm.items.push({ placeholder: '', original: '' })
}

const handleRemoveMappingItem = (index: number) => {
  if (mappingForm.items.length === 1) {
    mappingForm.items[0] = { placeholder: '', original: '' }
    return
  }
  mappingForm.items.splice(index, 1)
}

const handleSaveMapping = async () => {
  if (!mappingFormRef.value) return

  await mappingFormRef.value.validate(async (valid) => {
    if (!valid) return

    const mappings = mappingForm.items.reduce<Record<string, string>>((accumulator, item) => {
      const placeholder = item.placeholder.trim().replace(/^\[/, '').replace(/\]$/, '')
      const original = item.original.trim()
      if (placeholder && original) {
        accumulator[placeholder] = original
      }
      return accumulator
    }, {})

    if (!Object.keys(mappings).length) {
      ElMessage.warning('请至少录入一条有效映射关系')
      return
    }

    savingMapping.value = true
    try {
      await desensitizationApi.createMapping({
        sessionId: mappingForm.sessionId.trim(),
        ttlHours: mappingForm.ttlHours,
        mappings
      })
      ElMessage.success('映射关系已保存')
      mappingDialogVisible.value = false
      await loadMappings()
    } catch (error: any) {
      ElMessage.error(error.message || '保存映射关系失败')
    } finally {
      savingMapping.value = false
    }
  })
}

// 分页
const handlePageChange = (page: number) => {
  pagination.page = page
  loadMappings()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.page = 1
  loadMappings()
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadEntityTypes()
  loadMappings()
})
</script>

<style scoped>
.desensitization-config-page {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;

    h2 {
      margin: 0 0 8px 0;
    }

    .description {
      margin: 0;
      color: #666;
      font-size: 14px;
    }
  }

  .config-tabs {
    ::v-deep(.el-tabs__content) {
      overflow: visible;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .array-input {
    width: 100%;
  }

  .form-tip {
    margin-left: 8px;
    color: #666;
    font-size: 13px;
  }

  .mapping-editor {
    width: 100%;

    .mapping-row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
      gap: 8px;
      margin-bottom: 8px;
    }
  }

  .test-result {
    width: 100%;

    .result-section {
      margin-bottom: 20px;

      .result-label {
        font-weight: bold;
        margin-bottom: 8px;
        color: #333;
      }

      .result-content {
        padding: 12px;
        background: #f5f7fa;
        border-radius: 4px;
        border: 1px solid #dcdfe6;
        white-space: pre-wrap;
        word-break: break-all;
        font-family: monospace;
        font-size: 14px;

        &.original {
          background: #fef0f0;
          border-color: #fbc4c4;
          color: #f56c6c;
        }

        &.masked {
          background: #f0f9ff;
          border-color: #b3d8ff;
          color: #409eff;
        }

        &.restored {
          background: #f0f9ff;
          border-color: #b3e19d;
          color: #67c23a;
        }
      }
    }

    .arrow-icon {
      display: block;
      text-align: center;
      font-size: 24px;
      color: #409eff;
      margin: 10px 0;
    }
  }
}
</style>
