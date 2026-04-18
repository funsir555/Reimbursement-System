<template>
  <div class="flow-stack">
    <div
      v-for="block in blocks"
      :key="block.key"
      class="flow-step"
      :class="block.kind === 'branch' ? 'is-branch-step' : ''"
    >
      <template v-if="block.kind === 'insert'">
        <div
          class="insert-trigger-shell"
          :class="[
            dropTargetKey === block.key ? 'is-drop-target' : '',
            isMergedInsert(block) ? 'is-merged-target' : ''
          ]"
          @dragenter.prevent="emit('drag-node-over', { containerKey: block.containerKey, index: block.index, blockKey: block.key })"
          @dragover.prevent="emit('drag-node-over', { containerKey: block.containerKey, index: block.index, blockKey: block.key })"
          @drop.prevent="emit('drop-node', { containerKey: block.containerKey, index: block.index, blockKey: block.key })"
        >
          <el-dropdown trigger="click" @command="handleInsertCommand">
            <button
              type="button"
              class="insert-trigger"
              :aria-label="insertButtonAriaLabel(block)"
            >
              +
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <template v-for="(group, groupIndex) in resolveInsertGroups(block)" :key="group.key">
                  <el-dropdown-item
                    v-if="group.label"
                    class="insert-trigger-group-label"
                    :disabled="true"
                    :divided="groupIndex > 0"
                  >
                    {{ group.label }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-for="command in group.commands"
                    :key="command.key"
                    :command="command.command"
                  >
                    {{ command.label }}
                  </el-dropdown-item>
                </template>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>

      <template v-else-if="block.kind === 'node'">
        <div class="node-shell">
          <button
            type="button"
            class="flow-node-card"
            :class="[
              nodeCardClass(block.node.nodeType),
              selectedNodeKey === block.node.nodeKey ? 'is-selected' : '',
              draggingNodeKey === block.node.nodeKey ? 'is-dragging' : ''
            ]"
            draggable="true"
            @click="emit('select-node', block.node.nodeKey)"
            @dragstart="handleNodeDragStart($event, block.node.nodeKey)"
            @dragend="emit('drag-node-end')"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 text-left">
                <p class="truncate text-lg font-semibold">{{ block.node.nodeName }}</p>
                <p class="mt-2 text-sm opacity-80">{{ nodeTypeLabel(block.node.nodeType) }}</p>
              </div>
              <el-tag size="small" effect="plain" round>
                {{ sceneNameById(block.node.sceneId) || '默认场景' }}
              </el-tag>
            </div>
          </button>
        </div>
      </template>

      <template v-else>
        <div
          class="branch-shell"
          :class="[
            block.compact ? 'is-compact' : '',
            block.symmetric ? 'is-dual-lane' : '',
            isBranchActive(block) ? 'is-selected' : ''
          ]"
          :style="branchShellStyle(block.routes.length, block.depth)"
        >
          <div class="branch-top-rail">
            <div class="branch-top-rail__line" aria-hidden="true"></div>
            <button
              type="button"
              class="branch-drag-handle"
              :class="isBranchActive(block) ? 'is-selected' : ''"
              aria-label="拖动整个流程分支"
              draggable="true"
              @click="emit('select-node', block.node.nodeKey)"
              @dragstart="handleNodeDragStart($event, block.node.nodeKey)"
              @dragend="emit('drag-node-end')"
            >
              <span class="branch-drag-handle__grip"></span>
            </button>
          </div>

          <div class="branch-split-line" aria-hidden="true"></div>

          <div class="branch-lanes">
            <div
              v-for="lane in block.routes"
              :key="lane.route.routeKey"
              class="branch-lane"
              :class="lane.route.attachBelowNodes ? 'has-attached-tail' : ''"
            >
              <div class="branch-lane-entry" aria-hidden="true"></div>
              <button
                type="button"
                class="route-head-card"
                :class="[
                  selectedRouteKey === lane.route.routeKey ? 'is-selected' : '',
                  lane.route.attachBelowNodes ? 'is-attached' : ''
                ]"
                @click="emit('select-route', lane.route.routeKey)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0 text-left">
                    <div class="flex items-center gap-2">
                      <p class="truncate text-sm font-semibold text-slate-800">{{ lane.route.routeName || '未命名分支' }}</p>
                      <span
                        v-if="lane.route.attachBelowNodes"
                        class="rounded-full bg-sky-100 px-2 py-0.5 text-[11px] font-semibold text-sky-600"
                      >
                        附带下方节点
                      </span>
                    </div>
                    <p class="mt-1 text-xs text-slate-400">
                      优先级 {{ lane.route.priority || 1 }} 路 · {{ countConditions(lane.route) }} 条条件
                    </p>
                  </div>
                  <span class="route-head-link">条件设置</span>
                </div>
              </button>

              <div class="branch-lane-card-connector" aria-hidden="true"></div>
              <div class="branch-lane-body">
                <ProcessFlowCanvasRenderer
                  :blocks="lane.blocks"
                  :selected-node-key="selectedNodeKey"
                  :selected-route-key="selectedRouteKey"
                  :dragging-node-key="draggingNodeKey"
                  :drop-target-key="dropTargetKey"
                  :scene-name-by-id="sceneNameById"
                  :node-type-label="nodeTypeLabel"
                  :node-card-class="nodeCardClass"
                  @insert-node="emit('insert-node', $event)"
                  @select-node="emit('select-node', $event)"
                  @select-route="emit('select-route', $event)"
                  @drag-node-start="emit('drag-node-start', $event)"
                  @drag-node-end="emit('drag-node-end')"
                  @drag-node-over="emit('drag-node-over', $event)"
                  @drop-node="emit('drop-node', $event)"
                />
              </div>
              <div class="branch-lane-exit" aria-hidden="true"></div>
            </div>
          </div>

          <div class="branch-merge-line" aria-hidden="true"></div>
          <div class="branch-merge">
            <div class="branch-merge-entry" aria-hidden="true"></div>
            <span>分支汇合</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  FlowCanvasBlock,
  FlowCanvasBranchBlock,
  FlowCanvasInsertBlock,
  FlowContainerKey,
  FlowInsertType
} from '@/views/process/processFlowDesignerHelper'

defineOptions({
  name: 'ProcessFlowCanvasRenderer'
})

type InsertCommand = {
  containerKey: FlowContainerKey
  index: number
  nodeType: FlowInsertType
}

type CanvasDropPayload = {
  containerKey: FlowContainerKey
  index: number
  blockKey: string
}

type InsertGroup = {
  key: string
  label: string
  commands: Array<{
    key: string
    label: string
    command: InsertCommand
  }>
}

const props = defineProps<{
  blocks: FlowCanvasBlock[]
  selectedNodeKey: string
  selectedRouteKey: string
  draggingNodeKey?: string
  dropTargetKey?: string | null
  sceneNameById: (sceneId?: number) => string
  nodeTypeLabel: (nodeType: string) => string
  nodeCardClass: (nodeType: string) => string
}>()

const emit = defineEmits<{
  (event: 'insert-node', payload: InsertCommand): void
  (event: 'select-node', nodeKey: string): void
  (event: 'select-route', routeKey: string): void
  (event: 'drag-node-start', nodeKey: string): void
  (event: 'drag-node-end'): void
  (event: 'drag-node-over', payload: CanvasDropPayload): void
  (event: 'drop-node', payload: CanvasDropPayload): void
}>()

const INSERT_NODE_TYPES: FlowInsertType[] = ['APPROVAL', 'CC', 'PAYMENT', 'BRANCH']

function handleInsertCommand(command: string | number | object) {
  if (!command || typeof command !== 'object') {
    return
  }
  emit('insert-node', command as InsertCommand)
}

function handleNodeDragStart(event: DragEvent, nodeKey: string) {
  event.dataTransfer?.setData('text/plain', nodeKey)
  event.dataTransfer?.setDragImage?.((event.currentTarget as HTMLElement) || new Image(), 24, 24)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
  emit('drag-node-start', nodeKey)
}

function countConditions(route: { conditionGroups?: Array<{ conditions?: unknown[] }> }) {
  return (route.conditionGroups || []).reduce((total, group) => total + (group.conditions?.length || 0), 0)
}

function resolveInsertGroups(block: FlowCanvasInsertBlock): InsertGroup[] {
  const targets = block.targets?.length
    ? block.targets
    : [{
        key: `${block.key}-default`,
        label: '',
        containerKey: block.containerKey,
        index: block.index
      }]

  return targets.map((target) => ({
    key: target.key,
    label: target.label,
    commands: INSERT_NODE_TYPES.map((nodeType) => ({
      key: `${target.key}-${nodeType}`,
      label: insertTypeLabel(nodeType),
      command: {
        containerKey: target.containerKey,
        index: target.index,
        nodeType
      }
    }))
  }))
}

function insertTypeLabel(nodeType: FlowInsertType) {
  switch (nodeType) {
    case 'CC':
      return '抄送节点'
    case 'PAYMENT':
      return '支付节点'
    case 'BRANCH':
      return '流程分支'
    default:
      return '审批节点'
  }
}

function isMergedInsert(block: FlowCanvasInsertBlock) {
  return Boolean(block.targets?.length)
}

function insertButtonAriaLabel(block: FlowCanvasInsertBlock) {
  if (block.targets?.length) {
    return '在当前分支与附带下方节点之间插入节点'
  }
  return `在第 ${block.index + 1} 个位置插入节点`
}

function isBranchActive(block: FlowCanvasBranchBlock) {
  return props.selectedNodeKey === block.node.nodeKey || block.routes.some((lane) => lane.route.routeKey === props.selectedRouteKey)
}

function branchShellStyle(routeCount: number, depth: number) {
  const compact = depth > 0
  if (compact) {
    return {
      '--branch-lane-count': String(Math.max(routeCount, 1)),
      '--branch-lane-gap': '12px',
      '--branch-lane-min-width': '0px',
      '--branch-shell-width': '100%',
      '--branch-lane-padding': '8px'
    }
  }

  if (routeCount === 2) {
    return {
      '--branch-lane-count': '2',
      '--branch-lane-gap': '24px',
      '--branch-lane-min-width': '0px',
      '--branch-shell-width': 'min(100%, 760px)',
      '--branch-lane-padding': '12px'
    }
  }

  return {
    '--branch-lane-count': String(Math.max(routeCount, 1)),
    '--branch-lane-gap': '18px',
    '--branch-lane-min-width': '240px',
    '--branch-shell-width': 'max-content',
    '--branch-lane-padding': '12px'
  }
}
</script>

<style scoped>
.flow-stack {
  display: flex;
  width: 100%;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.flow-step {
  position: relative;
  display: flex;
  width: 100%;
  justify-content: center;
}

.flow-step::before,
.flow-step::after {
  position: absolute;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #cbd5e1, #94a3b8);
  content: '';
}

.flow-step::before {
  top: -18px;
  height: 18px;
}

.flow-step::after {
  bottom: -18px;
  height: 18px;
}

.flow-step.is-branch-step::before,
.flow-step.is-branch-step::after {
  z-index: 0;
}

.node-shell {
  display: flex;
  width: 100%;
  justify-content: center;
}

.flow-node-card {
  width: min(60%, 420px);
  border: 0;
  border-radius: 28px;
  padding: 22px 24px;
  color: #fff;
  text-align: left;
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.16);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.flow-node-card:hover {
  transform: translateY(-2px);
}

.flow-node-card.is-dragging {
  opacity: 0.55;
  transform: scale(0.98);
}

.flow-node-card.is-selected {
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.18), 0 20px 44px rgba(15, 23, 42, 0.22);
}

.flow-node-card.is-approval {
  background: linear-gradient(135deg, #2563eb, #60a5fa);
}

.flow-node-card.is-cc {
  background: linear-gradient(135deg, #7c3aed, #a78bfa);
}

.flow-node-card.is-payment {
  background: linear-gradient(135deg, #059669, #34d399);
}

.flow-node-card.is-branch {
  background: linear-gradient(135deg, #ea580c, #fb923c);
}

.insert-trigger-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 6px;
  transition: background-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.insert-trigger-shell.is-drop-target {
  background: rgba(14, 165, 233, 0.12);
  box-shadow: 0 0 0 6px rgba(14, 165, 233, 0.12);
  transform: scale(1.04);
}

.insert-trigger-shell.is-merged-target .insert-trigger {
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
}

.insert-trigger {
  display: inline-flex;
  height: 60px;
  width: 60px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #0f172a, #334155);
  color: #fff;
  font-size: 34px;
  font-weight: 300;
  line-height: 1;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.18);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.insert-trigger:hover {
  transform: scale(1.05);
  box-shadow: 0 20px 42px rgba(15, 23, 42, 0.24);
}

.insert-trigger-group-label {
  pointer-events: none;
  font-size: 12px;
  font-weight: 700;
  color: #0f766e;
}

.branch-shell {
  position: relative;
  display: flex;
  width: var(--branch-shell-width);
  max-width: 100%;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 0;
  padding-inline: 6px;
  z-index: 1;
}

.branch-shell.is-compact {
  width: 100%;
  padding-inline: 0;
}

.branch-top-rail {
  position: relative;
  display: flex;
  min-height: 34px;
  width: 100%;
  align-items: center;
  justify-content: center;
}

.branch-top-rail__line {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #cbd5e1, #94a3b8);
}

.branch-drag-handle {
  position: relative;
  z-index: 1;
  display: inline-flex;
  height: 36px;
  width: 36px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(14, 165, 233, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 10px 20px rgba(14, 165, 233, 0.12);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.branch-drag-handle:hover {
  transform: translateY(-1px);
  border-color: #38bdf8;
}

.branch-drag-handle.is-selected,
.branch-shell.is-selected .branch-drag-handle {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.12), 0 10px 20px rgba(14, 165, 233, 0.18);
}

.branch-drag-handle__grip {
  display: block;
  height: 14px;
  width: 14px;
  border-radius: 999px;
  background-image: radial-gradient(circle, #0ea5e9 1.3px, transparent 1.4px);
  background-size: 6px 6px;
  background-position: center;
}

.branch-split-line,
.branch-merge-line {
  height: 2px;
  width: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #cbd5e1, #94a3b8 50%, #cbd5e1);
}

.branch-lanes {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: repeat(var(--branch-lane-count), minmax(var(--branch-lane-min-width), 1fr));
  gap: var(--branch-lane-gap);
  margin: 0;
}

.branch-lane {
  display: flex;
  height: 100%;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 0;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.86));
  padding: var(--branch-lane-padding);
}

.branch-shell.is-compact .branch-lane {
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(255, 255, 255, 0.9));
}

.branch-lane-entry,
.branch-lane-card-connector,
.branch-lane-exit,
.branch-merge-entry {
  width: 2px;
  background: linear-gradient(180deg, #cbd5e1, #94a3b8);
}

.branch-lane-entry {
  height: 24px;
}

.branch-lane-card-connector {
  height: 18px;
}

.branch-lane-exit {
  height: 24px;
}

.route-head-card {
  width: 100%;
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff, #f8fafc);
  padding: 14px 16px;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.route-head-card:hover {
  transform: translateY(-1px);
  border-color: #7dd3fc;
}

.route-head-card.is-selected {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.12);
}

.route-head-card.is-attached {
  border-color: rgba(14, 165, 233, 0.28);
}

.route-head-link {
  flex-shrink: 0;
  color: #0284c7;
  font-size: 12px;
  font-weight: 600;
}

.branch-lane-body {
  display: flex;
  flex: 1 1 auto;
  width: 100%;
  min-width: 0;
  min-height: 110px;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.branch-shell.is-compact .branch-lane-body {
  min-height: 90px;
}

.branch-merge {
  display: flex;
  min-width: 168px;
  flex-direction: column;
  align-items: center;
  gap: 0;
  justify-content: center;
}

.branch-merge-entry {
  height: 20px;
}

.branch-merge span {
  display: flex;
  min-width: 168px;
  justify-content: center;
  border-radius: 20px;
  background: linear-gradient(135deg, #dbeafe, #f8fafc);
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #1e3a8a;
}

@media (max-width: 1279px) {
  .flow-node-card {
    width: min(86%, 420px);
  }

  .branch-shell,
  .branch-shell.is-compact {
    width: min(100%, 520px);
  }

  .branch-split-line,
  .branch-merge-line {
    width: 2px;
    height: 18px;
    background: linear-gradient(180deg, #cbd5e1, #94a3b8);
  }

  .branch-lanes {
    grid-template-columns: 1fr;
    gap: 14px;
  }
}
</style>
