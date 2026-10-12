<template>
  <div class="tool-page">
    <header class="topbar">
      <div class="title-row">
        <el-button :icon="ArrowLeft" text @click="goBack">返回工作台</el-button>
        <div><div class="eyebrow">AGENT TOOLS</div><h2>{{ detail?.agentName || '工具配置' }}</h2></div>
      </div>
      <div class="title-row">
        <el-tag effect="plain">最新配置 v{{ detail?.latestVersionNo || '-' }}</el-tag>
        <el-button :icon="RefreshRight" @click="load">刷新</el-button>
        <el-button type="primary" :loading="saving" :disabled="!canEdit || !dirty" @click="save">保存工具绑定</el-button>
      </div>
    </header>

    <div v-loading="loading" class="shell">
      <aside class="catalog">
        <div class="panel-title"><div class="eyebrow">Catalog</div><strong>受控工具</strong></div>
        <button v-for="tool in catalog" :key="tool.toolCode" type="button" class="catalog-item"
          :class="{ active: selectedCode === tool.toolCode }" @click="selectedCode = tool.toolCode">
          <strong>{{ tool.label }}</strong>
          <small>{{ tool.domain }} · {{ draftCodes.includes(tool.toolCode) ? '已绑定' : '未绑定' }}</small>
        </button>
        <section v-if="legacyTools.length" class="legacy">
          <div class="eyebrow">历史工具</div>
          <p>已从新工具目录下架；原有绑定与流程保持原状。</p>
          <div v-for="tool in legacyTools" :key="tool.toolCode" class="legacy-item">
            <strong>{{ tool.label || tool.toolCode }}</strong><small>{{ tool.toolCode }}</small>
          </div>
        </section>
      </aside>

      <main v-if="selectedTool" class="inspector">
        <section class="intro">
          <div class="eyebrow">Controlled Tool</div>
          <div class="intro-head"><h3>{{ selectedTool.label }}</h3><el-tag type="success" effect="plain">只读查询</el-tag></div>
          <p>{{ selectedTool.description }}</p>
        </section>

        <section class="card">
          <div class="section-head"><h4>说明与入口</h4><el-tag effect="plain">{{ selectedTool.domain }}业务域</el-tag></div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="工具编码">{{ selectedTool.toolCode }}</el-descriptions-item>
            <el-descriptions-item label="内部处理入口">{{ selectedTool.entry }}（只读，不提供 HTTP 地址配置）</el-descriptions-item>
            <el-descriptions-item label="所需查询权限">{{ selectedTool.permissionCode }}</el-descriptions-item>
            <el-descriptions-item label="查询模板">{{ selectedTool.queryTemplateCode }}</el-descriptions-item>
          </el-descriptions>
          <p class="hint">工作流实际使用的查询模板和变量映射在流程节点中配置；工具测试使用下方填写的参数。</p>
        </section>

        <section class="card">
          <div class="section-head"><h4>Agent 绑定</h4>
            <el-switch :model-value="draftCodes.includes(selectedTool.toolCode)" :disabled="!canEdit"
              @change="setBinding(selectedTool.toolCode, Boolean($event))" />
          </div>
          <p class="hint">仅控制当前 Agent 是否绑定该工具。保存后生成新的草稿版本；已发布版本保持原样。</p>
        </section>

        <section class="card">
          <div class="section-head"><h4>单工具测试</h4><el-tag type="warning" effect="plain">使用当前登录者权限</el-tag></div>
          <template v-if="selectedTool.toolCode === 'expense.query_my_expenses'">
            <div class="fields">
              <el-form-item label="查询模板"><el-select v-model="expense.queryTemplateCode">
                <el-option label="可见报销单及待办 v2" value="expense.accessible_expenses.v2" />
                <el-option label="本人提交的报销单 v1" value="expense.my_expenses.v1" />
              </el-select></el-form-item>
              <el-form-item label="对象范围"><el-select v-model="expense.scope">
                <el-option label="当前可见" value="VISIBLE" /><el-option label="本人提交" value="SUBMITTED_BY_ME" />
                <el-option label="分配给我的待办" value="ASSIGNED_TO_ME" />
              </el-select></el-form-item>
              <el-form-item label="关键词"><el-input v-model="expense.keyword" clearable /></el-form-item>
              <el-form-item label="单据状态"><el-input v-model="expense.status" placeholder="可选，例如 PENDING_APPROVAL" /></el-form-item>
              <el-form-item label="起始时间"><el-input v-model="expense.startDate" placeholder="可选，ISO 时间，例如 2026-10-01T00:00:00Z" /></el-form-item>
              <el-form-item label="结束时间"><el-input v-model="expense.endDate" placeholder="可选，ISO 时间" /></el-form-item>
              <el-form-item label="最小金额"><el-input-number v-model="expense.minAmount" :min="0" :controls="false" /></el-form-item>
              <el-form-item label="最大金额"><el-input-number v-model="expense.maxAmount" :min="0" :controls="false" /></el-form-item>
              <el-form-item label="展示条数"><el-input-number v-model="expense.limit" :min="1" :max="50" /></el-form-item>
            </div>
          </template>
          <div v-else-if="selectedTool.toolCode === 'finance.query_unclosed_companies'" class="fields">
            <el-form-item label="会计年度（必填）"><el-input-number v-model="finance.year" :min="2000" :max="2099" /></el-form-item>
            <el-form-item label="会计期间（必填）"><el-input-number v-model="finance.period" :min="1" :max="12" /></el-form-item>
          </div>
          <div v-else class="fields">
            <el-form-item v-for="field in visibleTestFields" :key="field.key" :label="`${field.label}${field.required ? '（必填）' : ''}`">
              <el-select v-if="field.type === 'supplier'" v-model="testValues[field.key]" filterable remote clearable
                :remote-method="searchSuppliers" :loading="supplierLoading" :disabled="!testValues.companyId"
                placeholder="输入供应商名称或简称搜索">
                <el-option v-for="item in supplierOptions" :key="item.code"
                  :label="`${item.name}（${item.code}）`" :value="item.code" />
              </el-select>
              <el-select v-else-if="field.type === 'voucherStatus'" v-model="testValues[field.key]" clearable placeholder="选择凭证范围">
                <el-option label="仅已记账" value="POSTED_ONLY" />
                <el-option label="包含已保存的未记账凭证" value="INCLUDE_SAVED_UNPOSTED" />
              </el-select>
              <el-select v-else-if="field.type === 'assistDisplay'" v-model="testValues[field.key]" clearable placeholder="留空为名称">
                <el-option label="名称" value="NAME" /><el-option label="编码" value="CODE" />
                <el-option label="编码＋名称" value="CODE_NAME" />
              </el-select>
              <el-input-number v-else-if="field.type === 'integer'" v-model="testValues[field.key]" :min="1" :controls="false" />
              <el-input v-else v-model="testValues[field.key]" clearable :placeholder="field.type === 'company' ? '输入账套的 company_id' : undefined" />
            </el-form-item>
          </div>
          <div class="test-actions">
            <el-button type="primary" :icon="VideoPlay" :loading="testing"
              :disabled="!canRun || dirty || !savedBound" @click="testTool">测试运行</el-button>
            <span class="hint">{{ dirty ? '请先保存绑定变更' : !savedBound ? '请先绑定并保存工具' : '测试只执行当前工具，不调用整条流程' }}</span>
          </div>
          <div v-if="result" class="result">
            <strong>{{ result.summary }}</strong>
            <p>{{ result.scopeDescription }}</p>
            <div class="result-counts"><span>匹配数量：{{ result.totalCount ?? 0 }}</span><span v-if="result.totalAmount !== null && result.totalAmount !== undefined">金额合计：{{ result.totalAmount }}</span><span>展示条数：{{ result.records.length }}</span><span v-if="result.page">第 {{ result.page }} 页{{ result.hasMore ? '，仍有后续页' : '' }}</span></div>
            <pre v-if="result.records.length">{{ JSON.stringify(result.records, null, 2) }}</pre>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, RefreshRight, VideoPlay } from '@element-plus/icons-vue'
import { archiveAgentApi, type ArchiveAgentDetail, type ArchiveAgentToolTestResult } from '@/api'
import { hasAnyPermission, readStoredUser } from '@/utils/permissions'

type TestField = { key: string; label: string; type: string; required: boolean }
type ToolMeta = { toolCode: string; label: string; description: string; domain: string; entry: string; permissionCode: string; queryTemplateCode: string; testFields?: TestField[] }
const route = useRoute()
const router = useRouter()
const permissions = readStoredUser()?.permissionCodes || []
const canEdit = hasAnyPermission(['agent:workbench:edit', 'agents:edit'], permissions)
const canRun = hasAnyPermission(['agent:workbench:run', 'agents:run'], permissions)
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const detail = ref<ArchiveAgentDetail | null>(null)
const catalog = ref<ToolMeta[]>([])
const selectedCode = ref('expense.query_my_expenses')
const draftCodes = ref<string[]>([])
const result = ref<ArchiveAgentToolTestResult | null>(null)
const expense = reactive({ queryTemplateCode: 'expense.accessible_expenses.v2', scope: 'VISIBLE', keyword: '', status: '', startDate: '', endDate: '', minAmount: undefined as number | undefined, maxAmount: undefined as number | undefined, limit: 10 })
const finance = reactive({ year: new Date().getFullYear(), period: new Date().getMonth() + 1 })
const testValues = reactive<Record<string, string | number | undefined>>({ page: 1, pageSize: 50 })
const supplierOptions = ref<Array<{ code: string; name: string }>>([])
const supplierLoading = ref(false)
let supplierSearchId = 0
const supported = computed(() => catalog.value.map(tool => tool.toolCode))
const selectedTool = computed(() => catalog.value.find((tool) => tool.toolCode === selectedCode.value))
const visibleTestFields = computed(() => (selectedTool.value?.testFields || []).filter(field =>
  selectedCode.value !== 'finance.query_supplier_detail_ledger' || field.key !== 'supplierName'))
const legacyTools = computed(() => (detail.value?.tools || []).filter((tool) => !supported.value.includes(tool.toolCode)))
const savedCodes = computed(() => (detail.value?.tools || []).filter((tool) => supported.value.includes(tool.toolCode) && tool.enabled !== false).map((tool) => tool.toolCode))
const dirty = computed(() => [...draftCodes.value].sort().join('|') !== [...savedCodes.value].sort().join('|'))
const savedBound = computed(() => savedCodes.value.includes(selectedCode.value))
watch(selectedCode, () => {
  result.value = null
  testValues.supplierCode = undefined
  supplierOptions.value = []
  supplierLoading.value = false
  supplierSearchId++
})
watch(() => testValues.companyId, () => {
  testValues.supplierCode = undefined
  supplierOptions.value = []
  supplierLoading.value = false
  supplierSearchId++
})

async function searchSuppliers(keyword: string) {
  const id = detail.value?.id
  const companyId = String(testValues.companyId || '').trim()
  const searchId = ++supplierSearchId
  supplierOptions.value = []
  if (!id || !companyId || !keyword.trim()) {
    supplierLoading.value = false
    return
  }
  supplierLoading.value = true
  try {
    const response = await archiveAgentApi.searchToolSuppliers(id, companyId, keyword.trim())
    if (searchId === supplierSearchId) supplierOptions.value = response.data || []
  } catch (error: any) {
    if (searchId === supplierSearchId) ElMessage.error(error?.message || '搜索供应商失败')
  } finally {
    if (searchId === supplierSearchId) supplierLoading.value = false
  }
}

onMounted(load)
async function load() {
  const id = Number(route.params.id)
  if (!Number.isSafeInteger(id) || id <= 0) return ElMessage.error('无效的 Agent ID')
  loading.value = true
  result.value = null
  try {
    const [metaResponse, detailResponse] = await Promise.all([archiveAgentApi.getMeta(), archiveAgentApi.getDetail(id)])
    catalog.value = (metaResponse.data.tools || []).filter((tool) => tool.available !== false).map((tool) => ({
      toolCode: String(tool.toolCode), label: String(tool.label), description: String(tool.description || ''),
      domain: String(tool.domain || ''), entry: String(tool.entry || ''), permissionCode: String(tool.permissionCode || ''),
      queryTemplateCode: String(tool.queryTemplateCode || ''), testFields: (tool.testFields || []) as TestField[]
    }))
    detail.value = detailResponse.data
    draftCodes.value = savedCodes.value.slice()
    if (!catalog.value.some((tool) => tool.toolCode === selectedCode.value)) selectedCode.value = catalog.value[0]?.toolCode || ''
  } catch (error: any) { ElMessage.error(error?.message || '工具配置加载失败') }
  finally { loading.value = false }
}
function setBinding(code: string, enabled: boolean) {
  draftCodes.value = enabled ? [...new Set([...draftCodes.value, code])] : draftCodes.value.filter((item) => item !== code)
  result.value = null
}
async function save() {
  if (!detail.value) return
  const removed = savedCodes.value.filter((code) => !draftCodes.value.includes(code))
  if (removed.some((code) => detail.value?.workflow?.nodes.some((node) => node.nodeType === 'tool' && node.config?.toolCode === code))) {
    try { await ElMessageBox.confirm('流程节点仍引用将解除绑定的工具。保存后该草稿流程可能无法测试或发布。', '确认解除绑定', { type: 'warning' }) }
    catch { return }
  }
  saving.value = true
  try {
    const response = await archiveAgentApi.updateTools(detail.value.id, { expectedVersionNo: detail.value.latestVersionNo || 0, toolCodes: draftCodes.value })
    detail.value = response.data
    draftCodes.value = savedCodes.value.slice()
    result.value = null
    ElMessage.success('工具绑定已保存，新版本待发布')
  } catch (error: any) { ElMessage.error(error?.message || '工具绑定保存失败，请刷新后重试') }
  finally { saving.value = false }
}
async function testTool() {
  if (!detail.value || !selectedTool.value || dirty.value || !savedBound.value) return
  testing.value = true
  result.value = null
  const testedCode = selectedCode.value
  try {
    const payload = selectedCode.value === 'expense.query_my_expenses'
      ? { queryTemplateCode: expense.queryTemplateCode, parameters: {
          scope: expense.scope, keyword: expense.keyword || null, status: expense.status || null,
          startDate: expense.startDate || null, endDate: expense.endDate || null,
          minAmount: expense.minAmount ?? null, maxAmount: expense.maxAmount ?? null, limit: expense.limit
        } }
      : selectedCode.value === 'finance.query_unclosed_companies'
        ? { queryTemplateCode: 'finance.unclosed_companies.v1', parameters: { year: finance.year, period: finance.period } }
        : { queryTemplateCode: selectedTool.value.queryTemplateCode,
            parameters: Object.fromEntries(visibleTestFields.value.filter(field =>
              testValues[field.key] !== undefined && testValues[field.key] !== '').map(field => [field.key, testValues[field.key]])) }
    const response = await archiveAgentApi.testTool(detail.value.id, testedCode, payload)
    if (selectedCode.value === testedCode) result.value = response.data
  } catch (error: any) { ElMessage.error(error?.message || '单工具测试失败') }
  finally { testing.value = false }
}
async function goBack() {
  if (dirty.value) {
    try { await ElMessageBox.confirm('工具绑定尚未保存，返回后修改会丢失。', '未保存的修改', { type: 'warning' }) }
    catch { return }
  }
  router.push({ name: 'agent-workbench', query: { agentId: route.params.id as string } })
}
</script>

<style scoped>
.tool-page { --ink:#273047; --mint:#1f9f9f; --amber:#d97706; min-height:100%; background:radial-gradient(circle at top left,rgba(255,212,77,.18),transparent 28%),#f8fafc; color:var(--ink); }
.topbar { display:flex; align-items:center; justify-content:space-between; gap:16px; padding:16px 20px; background:linear-gradient(135deg,rgba(255,212,77,.26),rgba(255,255,255,.98)); border-bottom:1px solid rgba(39,48,71,.08); }
.title-row,.intro-head,.section-head,.test-actions,.result-counts { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
.title-row h2 { margin:2px 0 0; font-size:20px; }.eyebrow { color:rgba(39,48,71,.55); font-size:11px; font-weight:800; letter-spacing:.12em; text-transform:uppercase; }
.shell { display:grid; grid-template-columns:minmax(240px,280px) minmax(0,1fr); gap:16px; padding:16px; min-height:calc(100vh - 160px); }
.catalog,.inspector { min-width:0; }.catalog { padding:18px; border-radius:24px; background:#fff; box-shadow:0 18px 40px rgba(39,48,71,.08); }.panel-title { margin-bottom:16px; }.panel-title strong { font-size:18px; }
.catalog-item { display:block; width:100%; margin-bottom:10px; padding:14px; text-align:left; border:1px solid rgba(39,48,71,.12); border-radius:16px; background:#fffaf1; cursor:pointer; color:var(--ink); }
.catalog-item.active { border-color:var(--mint); background:#effcfa; }.catalog-item strong,.catalog-item small,.legacy-item strong,.legacy-item small { display:block; }.catalog-item small,.legacy-item small { margin-top:5px; color:rgba(39,48,71,.6); }
.legacy { margin-top:24px; padding-top:16px; border-top:1px solid rgba(39,48,71,.1); }.legacy p,.hint { color:rgba(39,48,71,.65); font-size:13px; line-height:1.6; }.legacy-item { padding:10px 0; border-bottom:1px solid rgba(39,48,71,.07); }
.inspector { display:flex; flex-direction:column; gap:16px; }.intro,.card { padding:20px; border:1px solid rgba(39,48,71,.08); border-radius:24px; background:#fff; box-shadow:0 18px 40px rgba(39,48,71,.06); }.intro { background:linear-gradient(135deg,#fff7e8,#fff); }.intro h3 { margin:0; font-size:23px; }.intro p { margin-bottom:0; color:rgba(39,48,71,.72); }.card h4 { margin:0; font-size:18px; }.section-head { justify-content:space-between; margin-bottom:16px; }
.fields { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:4px 16px; }.fields :deep(.el-form-item) { margin-bottom:12px; }.fields :deep(.el-form-item__content) { width:100%; }.fields :deep(.el-input),.fields :deep(.el-input-number),.fields :deep(.el-select) { width:100%; }.result { margin-top:18px; padding:16px; border-radius:16px; background:#effcfa; }.result p { color:rgba(39,48,71,.7); }.result-counts { margin:12px 0; }.result-counts span { padding:6px 10px; border-radius:10px; background:#fff; }.result pre { max-height:360px; overflow:auto; padding:12px; border-radius:12px; background:#f8fafc; white-space:pre-wrap; word-break:break-word; }
@media (max-width:800px) { .topbar { align-items:flex-start; flex-direction:column; }.shell { grid-template-columns:1fr; }.fields { grid-template-columns:1fr; } }
</style>
