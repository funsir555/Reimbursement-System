<template>
  <div class="ontology-page">
    <div class="page-head">
      <div><h2>业务本体</h2><p>查看业务术语、字段、关系和处理能力。这里只维护通用业务语义。</p></div>
      <div><el-button @click="load">刷新</el-button><el-button v-if="canEdit" type="primary" @click="startNew">新建定义</el-button></div>
    </div>
    <el-alert title="发布前请确认：说明、别名和来源不含公司名称、人员、供应商、银行账号、真实金额或内部政策。发布内容可能进入模型上下文。" type="warning" show-icon :closable="false" />
    <div class="filters">
      <el-input v-model="keyword" clearable placeholder="搜索名称、别名或定义" @keyup.enter="load" />
      <el-select v-model="kindFilter"><el-option label="全部类型" value="" /><el-option v-for="entry in kinds" :key="entry.value" :label="entry.label" :value="entry.value" /></el-select>
      <el-select v-model="statusFilter"><el-option label="全部状态" value="" /><el-option label="已发布" value="PUBLISHED" /><el-option label="草稿" value="DRAFT" /><el-option label="已停用" value="RETIRED" /></el-select>
      <el-button type="primary" plain @click="load">搜索</el-button>
    </div>
    <el-table v-loading="loading" :data="visibleItems" row-key="id" border>
      <el-table-column prop="displayName" label="业务名称" min-width="130" />
      <el-table-column prop="itemKey" label="定义编码" min-width="180" />
      <el-table-column label="类型" width="100"><template #default="scope">{{ kindLabel(scope.row.kind) }}</template></el-table-column>
      <el-table-column prop="description" label="业务含义" min-width="280" show-overflow-tooltip />
      <el-table-column label="状态" width="105"><template #default="scope"><el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : scope.row.status === 'DRAFT' ? 'warning' : 'info'">{{ statusLabel(scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="版本" width="75"><template #default="scope">v{{ scope.row.versionNo }}</template></el-table-column>
      <el-table-column label="操作" width="230" fixed="right"><template #default="scope">
        <el-button link type="primary" @click="showDetail(scope.row)">详情</el-button>
        <el-button v-if="canEdit" link type="primary" @click="edit(scope.row)">编辑草稿</el-button>
        <el-button v-if="canPublish && scope.row.status === 'DRAFT'" link type="success" @click="publish(scope.row)">发布</el-button>
        <el-button v-if="canPublish && scope.row.status === 'PUBLISHED'" link type="danger" @click="retire(scope.row)">停用</el-button>
      </template></el-table-column>
    </el-table>
    <el-dialog v-model="detailOpen" title="业务本体定义" width="680px"><el-descriptions v-if="selected" :column="1" border>
      <el-descriptions-item label="名称">{{ selected.displayName }}</el-descriptions-item>
      <el-descriptions-item label="含义">{{ selected.description }}</el-descriptions-item>
      <el-descriptions-item label="别名">{{ selected.aliases.join('、') || '无' }}</el-descriptions-item>
      <el-descriptions-item label="业务域">{{ selected.ownerDomain }}</el-descriptions-item>
      <el-descriptions-item label="来源">{{ selected.sourceRef }}</el-descriptions-item>
      <el-descriptions-item label="关联实体">{{ selected.entityKey || '无' }}</el-descriptions-item>
      <el-descriptions-item v-if="selected.targetKey" label="目标实体">{{ selected.targetKey }} · {{ selected.cardinality }}</el-descriptions-item>
      <el-descriptions-item v-if="selected.valueCode" label="枚举值">{{ selected.valueCode }}</el-descriptions-item>
      <el-descriptions-item v-if="selected.unitName" label="单位">{{ selected.unitName }}</el-descriptions-item>
      <el-descriptions-item label="生效日期">{{ selected.effectiveFrom }}{{ selected.effectiveUntil ? ` 至 ${selected.effectiveUntil}` : ' 起' }}</el-descriptions-item>
      <el-descriptions-item label="审核人">{{ selected.reviewedBy ?? '待审核' }}</el-descriptions-item>
    </el-descriptions></el-dialog>
    <el-dialog v-model="editorOpen" title="业务本体草稿" width="720px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="定义编码" required><el-input v-model="draft.itemKey" :disabled="editingExisting" placeholder="例如 expense_document.status" /></el-form-item>
        <el-form-item label="类型" required><el-select v-model="draft.kind" :disabled="editingExisting"><el-option v-for="entry in kinds" :key="entry.value" :label="entry.label" :value="entry.value" /></el-select></el-form-item>
        <el-form-item label="业务名称" required><el-input v-model="draft.displayName" maxlength="120" /></el-form-item>
        <el-form-item label="业务含义" required><el-input v-model="draft.description" type="textarea" :rows="3" maxlength="1200" show-word-limit /></el-form-item>
        <el-form-item label="别名"><el-input v-model="aliasText" placeholder="多个别名用中文逗号分隔，最多 8 个" /></el-form-item>
        <el-form-item label="所属实体"><el-input v-model="draft.entityKey" placeholder="业务实体编码" /></el-form-item>
        <el-form-item v-if="draft.kind === 'ENUM'" label="枚举值"><el-input v-model="draft.valueCode" /></el-form-item>
        <el-form-item v-if="draft.kind === 'RELATION'" label="目标实体"><el-input v-model="draft.targetKey" /></el-form-item>
        <el-form-item v-if="draft.kind === 'RELATION'" label="关系数量"><el-select v-model="draft.cardinality"><el-option label="一对一" value="ONE_TO_ONE" /><el-option label="一对多" value="ONE_TO_MANY" /><el-option label="多对一" value="MANY_TO_ONE" /></el-select></el-form-item>
        <el-form-item v-if="draft.kind === 'FIELD'" label="字段类型"><el-input v-model="draft.semanticType" placeholder="例如 ENUM、NUMBER、DATE" /></el-form-item>
        <el-form-item v-if="draft.kind === 'FIELD' || draft.kind === 'METRIC'" label="单位"><el-input v-model="draft.unitName" /></el-form-item>
        <el-form-item v-if="draft.kind === 'CAPABILITY'" label="受控工具" required><el-input v-model="draft.toolCode" placeholder="后端已注册的工具编码" /></el-form-item>
        <el-form-item label="负责业务域" required><el-select v-model="draft.ownerDomain"><el-option v-for="domain in domains" :key="domain" :label="domain" :value="domain" /></el-select></el-form-item>
        <el-form-item label="定义来源" required><el-input v-model="draft.sourceRef" placeholder="非保密的产品规范或业务域名称" /></el-form-item>
        <el-form-item label="生效日期" required><el-date-picker v-model="draft.effectiveFrom" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="失效日期"><el-date-picker v-model="draft.effectiveUntil" type="date" value-format="YYYY-MM-DD" clearable /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editorOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { businessOntologyApi, type OntologyDraft, type OntologyItem, type OntologyKind, type OntologyStatus } from '@/api'
import { hasPermission } from '@/utils/permissions'

const kinds: Array<{ value: OntologyKind; label: string }> = [
  { value: 'ENTITY', label: '业务实体' }, { value: 'FIELD', label: '字段字典' },
  { value: 'ENUM', label: '枚举状态' }, { value: 'RELATION', label: '业务关系' },
  { value: 'RULE', label: '规则说明' }, { value: 'METRIC', label: '指标口径' },
  { value: 'CAPABILITY', label: '处理能力' }
]
const domains = ['EXPENSE', 'FINANCE', 'PROCESS', 'SYSTEM', 'AGENT']
const canEdit = hasPermission('agent:business_ontology:edit')
const canPublish = hasPermission('agent:business_ontology:publish')
const items = ref<OntologyItem[]>([])
const keyword = ref('')
const kindFilter = ref('')
const statusFilter = ref('')
const loading = ref(false)
const saving = ref(false)
const detailOpen = ref(false)
const editorOpen = ref(false)
const editingExisting = ref(false)
const selected = ref<OntologyItem | null>(null)
const aliasText = ref('')
function emptyDraft(): OntologyDraft {
  return { itemKey: '', kind: 'ENTITY', entityKey: null, displayName: '', description: '', aliases: [],
    valueCode: null, targetKey: null, cardinality: null, semanticType: null, unitName: null,
    toolCode: null, ownerDomain: 'EXPENSE', sourceRef: '', effectiveFrom: new Date().toISOString().slice(0, 10), effectiveUntil: null }
}
const draft = reactive<OntologyDraft>(emptyDraft())
const visibleItems = computed(() => items.value.filter(item => (!kindFilter.value || item.kind === kindFilter.value)
  && (!statusFilter.value || item.status === statusFilter.value)))
function kindLabel(kind: OntologyKind) { return kinds.find(entry => entry.value === kind)?.label || kind }
function statusLabel(status: OntologyStatus) { return { DRAFT: '草稿', PUBLISHED: '已发布', RETIRED: '已停用' }[status] }
async function load() {
  loading.value = true
  try { items.value = (await businessOntologyApi.list(keyword.value)).data || [] }
  catch (error: any) { ElMessage.error(error?.message || '加载业务本体失败') }
  finally { loading.value = false }
}
function showDetail(item: OntologyItem) { selected.value = item; detailOpen.value = true }
function startNew() { Object.assign(draft, emptyDraft()); aliasText.value = ''; editingExisting.value = false; editorOpen.value = true }
function edit(item: OntologyItem) {
  Object.assign(draft, { itemKey: item.itemKey, kind: item.kind, entityKey: item.entityKey,
    displayName: item.displayName, description: item.description, aliases: [...item.aliases],
    valueCode: item.valueCode, targetKey: item.targetKey, cardinality: item.cardinality,
    semanticType: item.semanticType, unitName: item.unitName, toolCode: item.toolCode,
    ownerDomain: item.ownerDomain, sourceRef: item.sourceRef, effectiveFrom: item.effectiveFrom,
    effectiveUntil: item.effectiveUntil })
  aliasText.value = item.aliases.join('，')
  editingExisting.value = true
  editorOpen.value = true
}
async function save() {
  saving.value = true
  try {
    const payload = { ...draft, aliases: aliasText.value.split(/[，,]/).map(value => value.trim()).filter(Boolean) }
    await businessOntologyApi.saveDraft(payload)
    ElMessage.success('草稿已保存，审核发布前不会进入模型上下文')
    editorOpen.value = false
    await load()
  } catch (error: any) { ElMessage.error(error?.message || '保存失败') }
  finally { saving.value = false }
}
async function publish(item: OntologyItem) {
  try {
    await ElMessageBox.confirm(`确认“${item.displayName}”不包含公司秘密，并发布 v${item.versionNo}？`, '审核发布', { type: 'warning' })
    await businessOntologyApi.publish(item.id); ElMessage.success('已发布'); await load()
  } catch (error: any) { if (error !== 'cancel') ElMessage.error(error?.message || '发布失败') }
}
async function retire(item: OntologyItem) {
  try {
    await ElMessageBox.confirm(`停用“${item.displayName}”后，模型将不再读取此版本。`, '停用定义', { type: 'warning' })
    await businessOntologyApi.retire(item.id); ElMessage.success('已停用'); await load()
  } catch (error: any) { if (error !== 'cancel') ElMessage.error(error?.message || '停用失败') }
}
onMounted(load)
</script>

<style scoped>
.ontology-page { padding: 24px; display: grid; gap: 18px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 24px; }
.page-head h2 { margin: 0 0 8px; }
.page-head p { margin: 0; color: var(--el-text-color-secondary); }
.filters { display: flex; gap: 10px; align-items: center; }
.filters .el-input { width: min(38vw, 380px); }
.filters .el-select { width: 150px; }
</style>
