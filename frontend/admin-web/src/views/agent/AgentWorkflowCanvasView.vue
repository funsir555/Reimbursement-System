<template>
  <div class="workflow-canvas-page" @click="closeNodeInspector">
    <header class="canvas-topbar">
      <div class="topbar-title">
        <el-button :icon="ArrowLeft" text @click="goBack">返回工作台</el-button>
        <div>
          <div class="eyebrow">AGENT WORKFLOW CANVAS</div>
          <h2>{{ agentDetail?.agentName || '流程编排画布' }}</h2>
        </div>
      </div>
      <div class="topbar-actions">
        <el-button :icon="RefreshRight" @click="loadDetail">刷新</el-button>
        <el-button :icon="Aim" @click="fitView">整理视图</el-button>
        <el-button type="primary" :icon="DocumentChecked" :loading="saving" @click="saveWorkflow">保存画布</el-button>
      </div>
    </header>

    <div class="canvas-shell" v-loading="loading">
      <aside class="node-palette">
        <div class="panel-title">
          <div class="eyebrow">Nodes</div>
          <strong>节点库</strong>
        </div>
        <el-tree
          class="node-tree"
          :data="paletteTree"
          node-key="id"
          default-expand-all
          :expand-on-click-node="false"
          @node-click="handlePaletteNodeClick"
        >
          <template #default="{ data }">
            <div class="tree-node" :class="{ category: !data.type }">
              <span v-if="data.short" class="palette-icon" :class="data.type">{{ data.short }}</span>
              <span class="tree-node-text">
                <strong>{{ data.label }}</strong>
                <small v-if="data.description">{{ data.description }}</small>
              </span>
            </div>
          </template>
        </el-tree>

        <section class="validation-box">
          <div class="panel-title compact">
            <div class="eyebrow">Validation</div>
            <strong>流程检查</strong>
          </div>
          <ul>
            <li v-for="item in validationMessages" :key="item" class="warning">{{ item }}</li>
            <li v-if="!validationMessages.length" class="ok">流程结构正常，可以保存。</li>
          </ul>
        </section>
      </aside>

      <main ref="canvasRef" class="canvas-board" @mousemove="handleCanvasMouseMove" @mouseup="cancelTempEdge">
        <div class="canvas-grid" :style="{ width: `${canvasSize.width}px`, height: `${canvasSize.height}px` }">
          <svg class="edge-layer" :width="canvasSize.width" :height="canvasSize.height">
            <defs>
              <marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
                <path d="M0,0 L0,6 L8,3 z" fill="#0f766e" />
              </marker>
            </defs>
            <path
              v-for="edge in edgePaths"
              :key="edge.id"
              class="edge-path"
              :class="{ selected: selectedEdgeId === edge.id }"
              :d="edge.path"
              marker-end="url(#arrow)"
              @click.stop="selectEdge(edge.id)"
              @contextmenu.prevent.stop="openEdgeMenu(edge.id, $event)"
            />
            <path v-if="tempEdgePath" class="edge-path temp" :d="tempEdgePath" marker-end="url(#arrow)" />
          </svg>

          <article
            v-for="node in nodes"
            :key="node.nodeKey"
            class="canvas-node"
            :class="[node.nodeType, { selected: selectedNodeKey === node.nodeKey }]"
            :style="{ transform: `translate(${nodePosition(node).x}px, ${nodePosition(node).y}px)` }"
            tabindex="0"
            @mousedown.left.stop="startDragNode(node, $event)"
            @click.stop="focusNode(node.nodeKey)"
            @dblclick.stop="openNodeInspector(node.nodeKey)"
            @keydown.enter.stop="openNodeInspector(node.nodeKey)"
          >
            <button
              v-if="node.nodeType !== 'start'"
              class="handle input"
              type="button"
              title="拖入连线"
              @mouseup.stop="finishTempEdge(node.nodeKey)"
            />
            <div class="node-head">
              <span class="node-icon">{{ nodeIcon(node.nodeType) }}</span>
              <div>
                <strong>{{ node.label }}</strong>
                <small>{{ node.nodeType }} · {{ node.nodeKey }}</small>
              </div>
            </div>
            <p>{{ nodeSummary(node) }}</p>
            <button
              v-if="node.nodeType !== 'end'"
              class="handle output"
              type="button"
              title="拖出连线"
              @mousedown.stop.prevent="startTempEdge(node.nodeKey, $event)"
            />
          </article>

          <div
            v-if="edgeMenu.visible"
            class="edge-context-menu"
            :style="{ left: `${edgeMenu.x}px`, top: `${edgeMenu.y}px` }"
            @click.stop
          >
            <button type="button" @click="removeContextEdge">删除连接</button>
            <button type="button" @click="reverseContextEdge">反转连接</button>
            <button type="button" @click="hideEdgeMenu">取消</button>
          </div>
        </div>
      </main>
    </div>

    <el-drawer v-model="nodeDrawerVisible" direction="rtl" size="390px" class="node-config-drawer" @closed="clearNodeSelection">
      <template #header>
        <div>
          <div class="eyebrow">Node Inspector</div>
          <strong>{{ selectedNode?.label || '节点配置' }}</strong>
        </div>
      </template>
      <template v-if="selectedNode">
        <el-form label-position="top">
          <el-form-item label="节点名称"><el-input v-model="selectedNode.label" /></el-form-item>
          <el-form-item label="节点类型">
            <el-select v-model="selectedNode.nodeType" :disabled="isProtectedNode(selectedNode.nodeType)" @change="resetNodeConfig(selectedNode)">
              <el-option v-for="item in paletteNodes" :key="item.type" :label="item.label" :value="item.type" />
            </el-select>
          </el-form-item>
          <template v-if="selectedNode.nodeType === 'tool'">
            <el-form-item label="工具编码">
              <el-select v-model="selectedNode.config.toolCode" filterable>
                <el-option v-for="tool in toolOptions" :key="tool.toolCode" :label="tool.label" :value="tool.toolCode" />
              </el-select>
            </el-form-item>
            <el-form-item label="查询模板编码"><el-input v-model="selectedNode.config.queryTemplateCode" /></el-form-item>
            <el-form-item label="变量映射"><el-input v-model="selectedNode.config.variableMapping" type="textarea" :rows="5" /></el-form-item>
          </template>
          <el-form-item v-if="selectedNode.nodeType === 'llm'" label="Prompt 模板"><el-input v-model="selectedNode.config.promptTemplate" type="textarea" :rows="6" /></el-form-item>
          <el-form-item v-if="selectedNode.nodeType === 'condition'" label="条件表达式"><el-input v-model="selectedNode.config.expression" /></el-form-item>
          <el-form-item v-if="selectedNode.nodeType === 'transform'" label="转换模板"><el-input v-model="selectedNode.config.template" type="textarea" :rows="5" /></el-form-item>
          <el-form-item v-if="selectedNode.nodeType === 'notify'" label="通知内容"><el-input v-model="selectedNode.config.message" type="textarea" :rows="5" /></el-form-item>
        </el-form>
        <div class="drawer-actions">
          <el-button :disabled="isProtectedNode(selectedNode.nodeType)" type="danger" plain @click="removeSelectedNode">删除节点</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, ArrowLeft, DocumentChecked, RefreshRight } from '@element-plus/icons-vue'
import { archiveAgentApi, type ArchiveAgentDetail, type ArchiveAgentSavePayload, type ArchiveAgentWorkflowEdge, type ArchiveAgentWorkflowNode } from '@/api'

type NodeType = ArchiveAgentWorkflowNode['nodeType']
type CanvasNode = ArchiveAgentWorkflowNode & { config: Record<string, any> }
type PaletteNode = { type: NodeType; label: string; short: string; description: string }
type PaletteTreeNode = { id: string; label: string; type?: NodeType; short?: string; description?: string; children?: PaletteTreeNode[] }
type Point = { x: number; y: number }

const route = useRoute()
const router = useRouter()
const canvasRef = ref<HTMLElement>()
const loading = ref(false)
const saving = ref(false)
const agentDetail = ref<ArchiveAgentDetail | null>(null)
const nodes = ref<CanvasNode[]>([])
const edges = ref<ArchiveAgentWorkflowEdge[]>([])
const selectedNodeKey = ref('')
const selectedEdgeId = ref('')
const nodeDrawerVisible = ref(false)
const dragState = ref<{ nodeKey: string; start: Point; origin: Point } | null>(null)
const tempEdge = ref<{ source: string; point: Point } | null>(null)
const edgeMenu = ref({ visible: false, x: 0, y: 0, edgeId: '' })
const canvasSize = { width: 1800, height: 1100 }

const paletteNodes: PaletteNode[] = [
  { type: 'start', label: '开始', short: 'S', description: '工作流入口' },
  { type: 'transform', label: '参数映射', short: 'P', description: '抽取关键词和变量' },
  { type: 'tool', label: '受控查询', short: 'T', description: '调用注册工具' },
  { type: 'condition', label: '条件判断', short: 'C', description: '分支和规则' },
  { type: 'llm', label: 'LLM 分析', short: 'L', description: '分析脱敏数据' },
  { type: 'notify', label: '通知', short: 'N', description: '发送站内消息' },
  { type: 'end', label: '结束', short: 'E', description: '输出结果' }
]
const paletteTree: PaletteTreeNode[] = [
  {
    id: 'trigger',
    label: '触发',
    children: [
      toTreeNode('start')
    ]
  },
  {
    id: 'logic',
    label: '理解与参数',
    children: [
      toTreeNode('transform'),
      toTreeNode('condition')
    ]
  },
  {
    id: 'tools',
    label: '工具',
    children: [
      toTreeNode('tool'),
      toTreeNode('notify')
    ]
  },
  {
    id: 'model',
    label: '模型与输出',
    children: [
      toTreeNode('llm'),
      toTreeNode('end')
    ]
  }
]

const toolOptions = computed(() => (agentDetail.value?.tools || []).map((tool) => ({
  toolCode: tool.toolCode,
  label: tool.label || tool.toolCode
})))
const selectedNode = computed(() => nodes.value.find((node) => node.nodeKey === selectedNodeKey.value))
const selectedEdge = computed(() => edges.value.find((edge) => edgeId(edge) === selectedEdgeId.value))
const edgePaths = computed(() => edges.value.map((edge) => ({ id: edgeId(edge), path: buildEdgePath(edge.source, edge.target) })))
const tempEdgePath = computed(() => {
  if (!tempEdge.value) return ''
  const source = outputPoint(tempEdge.value.source)
  return bezierPath(source, tempEdge.value.point)
})
const validationMessages = computed(() => validateWorkflow())

onMounted(loadDetail)

async function loadDetail() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    ElMessage.error('无效的 Agent ID')
    goBack()
    return
  }
  loading.value = true
  try {
    const res = await archiveAgentApi.getDetail(id)
    agentDetail.value = res.data
    nodes.value = normalizeNodes(res.data.workflow?.nodes || [])
    edges.value = Array.isArray(res.data.workflow?.edges) ? res.data.workflow.edges.map((item) => ({ source: item.source, target: item.target })) : []
    if (!selectedNodeKey.value) selectedNodeKey.value = nodes.value[0]?.nodeKey || ''
    fitView()
  } catch (error: any) {
    ElMessage.error(error?.message || '画布加载失败')
  } finally {
    loading.value = false
  }
}

async function saveWorkflow() {
  if (!agentDetail.value) return
  const errors = validateWorkflow()
  if (errors.length) {
    ElMessage.warning(errors[0])
    return
  }
  saving.value = true
  try {
    const payload: ArchiveAgentSavePayload = {
      agentName: agentDetail.value.agentName,
      agentDescription: agentDetail.value.agentDescription,
      iconKey: agentDetail.value.iconKey,
      themeKey: agentDetail.value.themeKey,
      coverColor: agentDetail.value.coverColor,
      tags: agentDetail.value.tags || [],
      promptConfig: agentDetail.value.promptConfig || {},
      modelConfig: agentDetail.value.modelConfig || {},
      tools: agentDetail.value.tools || [],
      workflow: {
        nodes: nodes.value.map((node) => ({
          nodeKey: node.nodeKey,
          nodeType: node.nodeType,
          label: node.label,
          config: { ...node.config, canvasPosition: nodePosition(node) }
        })),
        edges: edges.value.map((edge) => ({ source: edge.source, target: edge.target }))
      },
      triggers: agentDetail.value.triggers || [],
      inputSchema: agentDetail.value.inputSchema || {}
    }
    const res = await archiveAgentApi.update(agentDetail.value.id, payload)
    agentDetail.value = res.data
    nodes.value = normalizeNodes(res.data.workflow.nodes)
    edges.value = res.data.workflow.edges
    ElMessage.success('流程画布已保存')
  } catch (error: any) {
    ElMessage.error(error?.message || '流程画布保存失败')
  } finally {
    saving.value = false
  }
}

function normalizeNodes(source: ArchiveAgentWorkflowNode[]): CanvasNode[] {
  return source.map((node, index) => ({
    ...node,
    config: {
      ...(node.config || {}),
      canvasPosition: readPosition(node, index)
    }
  }))
}

function readPosition(node: ArchiveAgentWorkflowNode, index: number): Point {
  const position = (node.config as Record<string, any> | undefined)?.canvasPosition
  if (position && typeof position.x === 'number' && typeof position.y === 'number') {
    return { x: position.x, y: position.y }
  }
  return { x: 120 + index * 250, y: 260 + (index % 2) * 90 }
}

function addNode(type: NodeType) {
  const node: CanvasNode = {
    nodeKey: `${type}_${Date.now().toString(36)}`,
    nodeType: type,
    label: defaultNodeLabel(type),
    config: { ...defaultNodeConfig(type), canvasPosition: { x: 220 + nodes.value.length * 40, y: 160 + nodes.value.length * 36 } }
  }
  nodes.value.push(node)
  selectedNodeKey.value = node.nodeKey
  selectedEdgeId.value = ''
  nodeDrawerVisible.value = false
}

function handlePaletteNodeClick(data: PaletteTreeNode) {
  if (!data.type) return
  addNode(data.type)
}

function startDragNode(node: CanvasNode, event: MouseEvent) {
  if ((event.target as HTMLElement).classList.contains('handle')) return
  dragState.value = {
    nodeKey: node.nodeKey,
    start: canvasPoint(event),
    origin: nodePosition(node)
  }
  window.addEventListener('mousemove', dragNode)
  window.addEventListener('mouseup', stopDragNode, { once: true })
}

function dragNode(event: MouseEvent) {
  if (!dragState.value) return
  const node = nodes.value.find((item) => item.nodeKey === dragState.value?.nodeKey)
  if (!node) return
  const point = canvasPoint(event)
  setNodePosition(node, {
    x: Math.max(24, dragState.value.origin.x + point.x - dragState.value.start.x),
    y: Math.max(24, dragState.value.origin.y + point.y - dragState.value.start.y)
  })
}

function stopDragNode() {
  dragState.value = null
  window.removeEventListener('mousemove', dragNode)
}

function startTempEdge(source: string, event: MouseEvent) {
  tempEdge.value = { source, point: canvasPoint(event) }
}

function handleCanvasMouseMove(event: MouseEvent) {
  if (!tempEdge.value) return
  tempEdge.value.point = canvasPoint(event)
}

function finishTempEdge(target?: string) {
  if (!tempEdge.value) return
  if (target && target !== tempEdge.value.source && !edges.value.some((edge) => edge.source === tempEdge.value?.source && edge.target === target)) {
    edges.value.push({ source: tempEdge.value.source, target })
  }
  tempEdge.value = null
}

function cancelTempEdge() {
  tempEdge.value = null
}

function focusNode(nodeKey: string) {
  selectedNodeKey.value = nodeKey
  selectedEdgeId.value = ''
  hideEdgeMenu()
}

function openNodeInspector(nodeKey: string) {
  focusNode(nodeKey)
  nodeDrawerVisible.value = true
}

function closeNodeInspector() {
  hideEdgeMenu()
  if (nodeDrawerVisible.value) {
    nodeDrawerVisible.value = false
    return
  }
  selectedNodeKey.value = ''
}

function clearNodeSelection() {
  if (!nodeDrawerVisible.value) selectedNodeKey.value = ''
}

function selectEdge(id: string) {
  selectedEdgeId.value = id
  selectedNodeKey.value = ''
  nodeDrawerVisible.value = false
  hideEdgeMenu()
}

function removeSelectedNode() {
  if (!selectedNode.value || isProtectedNode(selectedNode.value.nodeType)) return
  const nodeKey = selectedNode.value.nodeKey
  nodes.value = nodes.value.filter((node) => node.nodeKey !== nodeKey)
  edges.value = edges.value.filter((edge) => edge.source !== nodeKey && edge.target !== nodeKey)
  selectedNodeKey.value = nodes.value[0]?.nodeKey || ''
  nodeDrawerVisible.value = false
}

function removeSelectedEdge() {
  if (!selectedEdgeId.value) return
  edges.value = edges.value.filter((edge) => edgeId(edge) !== selectedEdgeId.value)
  selectedEdgeId.value = ''
}

function openEdgeMenu(id: string, event: MouseEvent) {
  selectedEdgeId.value = id
  selectedNodeKey.value = ''
  nodeDrawerVisible.value = false
  const rect = canvasRef.value?.getBoundingClientRect()
  edgeMenu.value = {
    visible: true,
    x: event.clientX - (rect?.left || 0) + (canvasRef.value?.scrollLeft || 0),
    y: event.clientY - (rect?.top || 0) + (canvasRef.value?.scrollTop || 0),
    edgeId: id
  }
}

function removeContextEdge() {
  if (!edgeMenu.value.edgeId) return
  edges.value = edges.value.filter((edge) => edgeId(edge) !== edgeMenu.value.edgeId)
  selectedEdgeId.value = ''
  hideEdgeMenu()
}

function reverseContextEdge() {
  const targetEdge = edges.value.find((edge) => edgeId(edge) === edgeMenu.value.edgeId)
  if (!targetEdge) return
  const reversed = { source: targetEdge.target, target: targetEdge.source }
  edges.value = edges.value.filter((edge) => edgeId(edge) !== edgeMenu.value.edgeId)
  if (!edges.value.some((edge) => edge.source === reversed.source && edge.target === reversed.target)) {
    edges.value.push(reversed)
  }
  selectedEdgeId.value = edgeId(reversed)
  hideEdgeMenu()
}

function hideEdgeMenu() {
  edgeMenu.value.visible = false
}

function resetNodeConfig(node: CanvasNode) {
  node.config = { ...defaultNodeConfig(node.nodeType), canvasPosition: nodePosition(node) }
  node.label = defaultNodeLabel(node.nodeType)
}

function fitView() {
  nodes.value.forEach((node, index) => {
    if (!node.config.canvasPosition) {
      setNodePosition(node, { x: 120 + index * 250, y: 260 + (index % 2) * 90 })
    }
  })
}

function validateWorkflow() {
  const messages: string[] = []
  if (nodes.value.filter((node) => node.nodeType === 'start').length !== 1) messages.push('流程必须有且只有一个开始节点')
  if (!nodes.value.some((node) => node.nodeType === 'end')) messages.push('流程至少需要一个结束节点')
  const incoming = new Map(nodes.value.map((node) => [node.nodeKey, 0]))
  const outgoing = new Map(nodes.value.map((node) => [node.nodeKey, [] as string[]]))
  edges.value.forEach((edge) => {
    if (!nodes.value.some((node) => node.nodeKey === edge.source) || !nodes.value.some((node) => node.nodeKey === edge.target)) {
      messages.push(`连线 ${edge.source} → ${edge.target} 引用了不存在的节点`)
      return
    }
    incoming.set(edge.target, (incoming.get(edge.target) || 0) + 1)
    outgoing.get(edge.source)?.push(edge.target)
  })
  nodes.value.filter((node) => node.nodeType === 'tool').forEach((node) => {
    if (!node.config.toolCode) messages.push(`${node.label} 未选择工具编码`)
    if (!node.config.queryTemplateCode) messages.push(`${node.label} 未配置查询模板编码`)
    if (node.config.variableMapping) {
      try { JSON.parse(node.config.variableMapping) } catch { messages.push(`${node.label} 的变量映射不是合法 JSON`) }
    }
  })
  const startNode = nodes.value.find((node) => node.nodeType === 'start')
  if (startNode) {
    const visited = new Set<string>()
    const queue = [startNode.nodeKey]
    while (queue.length) {
      const current = queue.shift()!
      if (visited.has(current)) continue
      visited.add(current)
      queue.push(...(outgoing.get(current) || []))
    }
    if (visited.size !== nodes.value.length) messages.push('存在未从开始节点连通的节点')
  }
  const controlledTools = nodes.value.filter((node) => node.nodeType === 'tool' && node.config.toolCode === 'expense.query_my_expenses')
  if (controlledTools.length) {
    const llmNodes = nodes.value.filter((node) => node.nodeType === 'llm')
    if (controlledTools.length !== 1 || llmNodes.length !== 1) {
      messages.push('受控查询流程必须包含且只能包含一个查询工具节点和一个 LLM 节点')
    }
    if (nodes.value.some((node) => !['start', 'tool', 'llm', 'end'].includes(node.nodeType))) {
      messages.push('受控查询流程当前仅支持开始、工具、LLM 和结束节点')
    }
    if (nodes.value.filter((node) => node.nodeType === 'end').length !== 1) messages.push('受控查询流程必须有且只有一个结束节点')
    nodes.value.forEach((node) => {
      const inCount = incoming.get(node.nodeKey) || 0
      const outCount = outgoing.get(node.nodeKey)?.length || 0
      if (node.nodeType === 'start' && outCount !== 1) messages.push('受控查询的开始节点必须连接一个后续节点')
      else if (node.nodeType === 'end' && (inCount !== 1 || outCount !== 0)) messages.push('受控查询的结束节点必须只有一条入线且不能有出线')
      else if (!['start', 'end'].includes(node.nodeType) && (inCount !== 1 || outCount !== 1)) messages.push(`${node.label} 必须各有一条入线和出线`)
    })
    const controlledTool = controlledTools[0]
    const llmNode = llmNodes[0]
    if (startNode && controlledTool && llmNode) {
      const path: string[] = []
      let current: string | undefined = startNode.nodeKey
      while (current && !path.includes(current)) {
        path.push(current)
        current = outgoing.get(current)?.[0]
      }
      if (path.indexOf(controlledTool.nodeKey) > path.indexOf(llmNode.nodeKey)) {
        messages.push('受控查询流程必须先执行查询工具，再执行 LLM 节点')
      }
    }
  }
  return Array.from(new Set(messages))
}

function nodePosition(node: CanvasNode): Point {
  return node.config.canvasPosition || { x: 0, y: 0 }
}

function setNodePosition(node: CanvasNode, point: Point) {
  node.config.canvasPosition = { x: point.x, y: point.y }
}

function inputPoint(nodeKey: string): Point {
  const node = nodes.value.find((item) => item.nodeKey === nodeKey)
  const position = node ? nodePosition(node) : { x: 0, y: 0 }
  return { x: position.x, y: position.y + 48 }
}

function outputPoint(nodeKey: string): Point {
  const node = nodes.value.find((item) => item.nodeKey === nodeKey)
  const position = node ? nodePosition(node) : { x: 0, y: 0 }
  return { x: position.x + 210, y: position.y + 48 }
}

function buildEdgePath(source: string, target: string) {
  return bezierPath(outputPoint(source), inputPoint(target))
}

function bezierPath(source: Point, target: Point) {
  const delta = Math.max(80, Math.abs(target.x - source.x) * 0.45)
  return `M ${source.x} ${source.y} C ${source.x + delta} ${source.y}, ${target.x - delta} ${target.y}, ${target.x} ${target.y}`
}

function canvasPoint(event: MouseEvent): Point {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return { x: event.clientX, y: event.clientY }
  return { x: event.clientX - rect.left + (canvasRef.value?.scrollLeft || 0), y: event.clientY - rect.top + (canvasRef.value?.scrollTop || 0) }
}

function edgeId(edge: ArchiveAgentWorkflowEdge) {
  return `${edge.source}->${edge.target}`
}

function defaultNodeLabel(type: NodeType) {
  return ({ start: '开始', transform: '参数映射', tool: '受控查询', condition: '条件判断', llm: 'LLM 分析', notify: '通知', end: '结束' } as Record<NodeType, string>)[type]
}

function defaultNodeConfig(type: NodeType) {
  if (type === 'tool') return { toolCode: toolOptions.value[0]?.toolCode || '', queryTemplateCode: 'expense.accessible_expenses.v2', variableMapping: '{"scope":"{{input.scope}}","keyword":"{{input.keyword}}","status":"{{input.status}}","startDate":"{{input.startDate}}","endDate":"{{input.endDate}}","minAmount":"{{input.minAmount}}","maxAmount":"{{input.maxAmount}}","limit":"{{input.limit}}"}' }
  if (type === 'llm') return { promptTemplate: '基于脱敏后的 JSON 输出 answer 和 evidenceRefs。' }
  if (type === 'condition') return { expression: '' }
  if (type === 'transform') return { template: type === 'transform' ? 'extract_query_variables' : '' }
  if (type === 'notify') return { message: '' }
  return {}
}

function toTreeNode(type: NodeType): PaletteTreeNode {
  const item = paletteNodes.find((node) => node.type === type)
  return {
    id: item?.type || type,
    type,
    label: item?.label || type,
    short: item?.short || '',
    description: item?.description || ''
  }
}

function nodeIcon(type: NodeType) {
  return ({ start: 'S', transform: 'P', tool: 'T', condition: 'C', llm: 'L', notify: 'N', end: 'E' } as Record<NodeType, string>)[type]
}

function nodeSummary(node: CanvasNode) {
  if (node.nodeType === 'tool') return node.config.toolCode || '选择受控工具'
  if (node.nodeType === 'llm') return node.config.promptTemplate || '配置模型分析 Prompt'
  if (node.nodeType === 'transform') return node.config.template || '变量转换 / 脱敏 / 还原'
  if (node.nodeType === 'condition') return node.config.expression || '配置判断表达式'
  return '流程节点'
}

function isProtectedNode(type: NodeType) {
  return type === 'start' || type === 'end'
}

function goBack() {
  router.push({ name: 'agent-workbench' })
}
</script>

<style scoped>
.workflow-canvas-page { --ink: #273047; --mint: #1f9f9f; --amber: #d97706; display: flex; flex-direction: column; height: 100%; min-height: 0; background: radial-gradient(circle at top left, rgba(255,212,77,.18), transparent 28%), radial-gradient(circle at 78% 8%, rgba(31,159,159,.14), transparent 24%), #f8fafc; }
.canvas-topbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 16px 20px; background: radial-gradient(circle at top right, rgba(31,159,159,.2), transparent 34%), linear-gradient(135deg, rgba(255,212,77,.26), rgba(255,255,255,.98)); border-bottom: 1px solid rgba(39,48,71,.08); box-shadow: 0 12px 30px rgba(39,48,71,.06); }
.topbar-title, .topbar-actions { display: flex; align-items: center; gap: 12px; }
.topbar-title h2 { margin: 2px 0 0; font-size: 20px; color: var(--ink); }
.eyebrow { font-size: 11px; font-weight: 800; letter-spacing: .12em; color: rgba(39,48,71,.55); text-transform: uppercase; }
.canvas-shell { flex: 1; min-height: 0; display: grid; grid-template-columns: 270px minmax(0, 1fr); gap: 16px; padding: 16px; }
.node-palette { min-height: 0; overflow: auto; padding: 16px; border: 1px solid rgba(39,48,71,.08); border-radius: 24px; background: rgba(255,255,255,.95); box-shadow: 0 18px 40px rgba(39,48,71,.08); }
.panel-title { margin-bottom: 14px; } .panel-title strong { display: block; margin-top: 4px; color: var(--ink); }
.panel-title.compact { margin: 18px 0 8px; }
.node-tree { --el-tree-node-hover-bg-color: rgba(31,159,159,.1); background: transparent; color: var(--ink); }
.node-tree :deep(.el-tree-node__content) { height: auto; min-height: 38px; border-radius: 12px; margin: 3px 0; transition: background .16s ease, color .16s ease; }
.node-tree :deep(.el-tree-node__content:hover) { background: rgba(31,159,159,.1); }
.tree-node { width: 100%; display: flex; align-items: center; gap: 10px; padding: 7px 8px; border-radius: 12px; }
.tree-node.category { padding-left: 0; font-weight: 800; color: rgba(39,48,71,.7); }
.tree-node-text strong { display: block; font-size: 13px; color: var(--ink); }
.tree-node-text small { display: block; margin-top: 2px; color: rgba(39,48,71,.5); font-size: 12px; }
.palette-icon, .node-icon { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; border-radius: 10px; background: #0f766e; color: #fff; font-weight: 900; }
.canvas-board { position: relative; min-width: 0; overflow: auto; border: 1px solid rgba(39,48,71,.08); border-radius: 24px; background: #fffaf1; box-shadow: 0 18px 40px rgba(39,48,71,.08); }
.canvas-grid { position: relative; background: linear-gradient(rgba(39,48,71,.05) 1px, transparent 1px), linear-gradient(90deg, rgba(39,48,71,.05) 1px, transparent 1px), #fffaf1; background-size: 18px 18px; }
.edge-layer { position: absolute; inset: 0; pointer-events: none; }
.edge-path { fill: none; stroke: #0f766e; stroke-width: 2.5; pointer-events: stroke; cursor: pointer; }
.edge-path.selected { stroke: #d97706; stroke-width: 3.5; }
.edge-path.temp { stroke-dasharray: 8 6; opacity: .7; }
.canvas-node { position: absolute; width: 210px; min-height: 96px; padding: 14px; border: 1px solid rgba(39,48,71,.1); border-radius: 16px; background: #fff; box-shadow: 0 16px 32px rgba(39,48,71,.12); cursor: grab; user-select: none; transition: border-color .16s ease, box-shadow .16s ease; }
.canvas-node:focus-visible { outline: 2px solid rgba(31,159,159,.5); outline-offset: 3px; }
.canvas-node.selected { border-color: rgba(217,119,6,.55); box-shadow: 0 18px 34px rgba(217,119,6,.16); }
.canvas-node:active { cursor: grabbing; }
.canvas-node.start { border-top: 5px solid #16a34a; } .canvas-node.tool { border-top: 5px solid #0f766e; } .canvas-node.llm { border-top: 5px solid #7c3aed; } .canvas-node.transform { border-top: 5px solid #2563eb; } .canvas-node.condition { border-top: 5px solid #ca8a04; } .canvas-node.notify { border-top: 5px solid #db2777; } .canvas-node.end { border-top: 5px solid #334155; }
.node-head { display: flex; gap: 10px; align-items: center; }
.node-head strong { display: block; color: var(--ink); }
.node-head small { display: block; margin-top: 2px; color: rgba(39,48,71,.52); }
.canvas-node p { margin: 10px 0 0; color: rgba(39,48,71,.62); font-size: 12px; line-height: 1.5; word-break: break-word; }
.handle { position: absolute; width: 14px; height: 14px; border: 2px solid #fff; border-radius: 999px; background: #0f766e; box-shadow: 0 0 0 2px rgba(15,118,110,.28); cursor: crosshair; }
.handle.input { left: -7px; top: 44px; }
.handle.output { right: -7px; top: 44px; }
.drawer-actions { margin-top: 8px; }
.edge-context-menu { position: absolute; z-index: 10; display: grid; min-width: 138px; padding: 6px; border: 1px solid rgba(39,48,71,.12); border-radius: 12px; background: rgba(255,255,255,.98); box-shadow: 0 18px 34px rgba(39,48,71,.16); }
.edge-context-menu button { padding: 9px 10px; border: 0; border-radius: 9px; background: transparent; text-align: left; color: var(--ink); cursor: pointer; }
.edge-context-menu button:hover { background: rgba(31,159,159,.1); color: #0f766e; }
.validation-box ul { margin: 0; padding-left: 18px; font-size: 13px; line-height: 1.7; }
.warning { color: #b45309; } .ok { color: #15803d; } .muted { color: rgba(39,48,71,.62); }
@media (max-width: 1180px) {
  .canvas-shell { grid-template-columns: 220px minmax(0, 1fr); gap: 12px; padding: 12px; }
}
</style>
