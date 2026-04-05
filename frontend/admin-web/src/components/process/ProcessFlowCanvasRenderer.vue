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
          :class="dropTargetKey === block.key ? 'is-drop-target' : ''"
          @dragenter.prevent="emit('drag-node-over', { containerKey: block.containerKey, index: block.index, blockKey: block.key })"
          @dragover.prevent="emit('drag-node-over', { containerKey: block.containerKey, index: block.index, blockKey: block.key })"
          @drop.prevent="emit('drop-node', { containerKey: block.containerKey, index: block.index, blockKey: block.key })"
        >
          <el-dropdown trigger="click" @command="handleInsertCommand">
            <button
              type="button"
              class="insert-trigger"
              :aria-label="`在第 ${block.index + 1} 个位置插入节点`"
            >
              +
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="{ containerKey: block.containerKey, index: block.index, nodeType: 'APPROVAL' }">
                  审批节点
                </el-dropdown-item>
                <el-dropdown-item :command="{ containerKey: block.containerKey, index: block.index, nodeType: 'CC' }">
                  抄送节点
                </el-dropdown-item>
                <el-dropdown-item :command="{ containerKey: block.containerKey, index: block.index, nodeType: 'PAYMENT' }">
                  支付节点
                </el-dropdown-item>
                <el-dropdown-item :command="{ containerKey: block.containerKey, index: block.index, nodeType: 'BRANCH' }">
                  流程分支
                </el-dropdown-item>
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
        <div class="branch-shell" :style="branchShellStyle(block.routes.length)">
          <div class="branch-mainline-entry" aria-hidden="true"></div>
          <div class="branch-split-line" aria-hidden="true"></div>
          <button
            type="button"
            class="branch-node-card"
            :class="[
              selectedNodeKey === block.node.nodeKey ? 'is-selected' : '',
              draggingNodeKey === block.node.nodeKey ? 'is-dragging' : ''
            ]"
            draggable="true"
            @click="emit('select-node', block.node.nodeKey)"
            @dragstart="handleNodeDragStart($event, block.node.nodeKey)"
            @dragend="emit('drag-node-end')"
          >
            <div class="min-w-0 text-left">
              <p class="truncate text-sm font-semibold text-slate-900">{{ block.node.nodeName }}</p>
              <p class="mt-1 text-xs text-slate-500">{{ nodeTypeLabel(block.node.nodeType) }}</p>
            </div>
            <span class="branch-node-card__hint">拖动可调整位置</span>
          </button>

          <div class="branch-lanes">
            <div
              v-for="lane in block.routes"
              :key="lane.route.routeKey"
              class="branch-lane"
            >
              <div class="branch-lane-entry" aria-hidden="true"></div>
              <button
                type="button"
                class="route-head-card"
                :class="selectedRouteKey === lane.route.routeKey ? 'is-selected' : ''"
                @click="emit('select-route', lane.route.routeKey)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0 text-left">
                    <p class="truncate text-sm font-semibold text-slate-800">{{ lane.route.routeName || '未命名分支' }}</p>
                    <p class="mt-1 text-xs text-slate-400">
                      优先级 {{ lane.route.priority || 1 }} · {{ countConditions(lane.route) }} 条条件
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
import type { FlowCanvasBlock, FlowContainerKey, FlowInsertType } from '@/views/process/processFlowDesignerHelper'

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

defineProps<{
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

function branchShellStyle(routeCount: number) {
  return {
    '--branch-lane-count': String(Math.max(routeCount, 1)),
    '--branch-lane-gap': '18px',
    '--branch-lane-width': '260px'
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

.branch-shell {
  position: relative;
  display: flex;
  width: max-content;
  min-width: min(100%, 980px);
  flex-direction: column;
  align-items: center;
  gap: 0;
  z-index: 1;
}

.branch-node-card {
  display: flex;
  width: min(100%, 320px);
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 14px 0 18px;
  border: 1px solid rgba(14, 165, 233, 0.18);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.98), rgba(255, 255, 255, 0.98));
  padding: 14px 16px;
  color: #0f172a;
  text-align: left;
  box-shadow: 0 16px 34px rgba(14, 165, 233, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.branch-node-card:hover {
  transform: translateY(-1px);
}

.branch-node-card.is-selected {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.12), 0 16px 34px rgba(14, 165, 233, 0.12);
}

.branch-node-card.is-dragging {
  opacity: 0.55;
}

.branch-node-card__hint {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #0284c7;
}

.branch-mainline-entry {
  height: 22px;
  width: 2px;
  background: linear-gradient(180deg, #cbd5e1, #94a3b8);
}

.branch-split-line,
.branch-merge-line {
  height: 2px;
  align-self: stretch;
  margin-inline: calc((100% - ((100% - ((var(--branch-lane-count) - 1) * var(--branch-lane-gap))) / var(--branch-lane-count))) / 2);
  border-radius: 999px;
  background: linear-gradient(90deg, #cbd5e1, #94a3b8 50%, #cbd5e1);
}

.branch-lanes {
  display: grid;
  width: max-content;
  min-width: 100%;
  grid-template-columns: repeat(var(--branch-lane-count), minmax(var(--branch-lane-width), var(--branch-lane-width)));
  gap: var(--branch-lane-gap);
  margin: 0;
}

.branch-lane {
  display: flex;
  height: 100%;
  min-width: var(--branch-lane-width);
  flex-direction: column;
  align-items: center;
  gap: 0;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.86));
  padding: 12px;
}

.branch-lane-entry,
.branch-lane-card-connector,
.branch-lane-exit,
.branch-merge-entry {
  width: 2px;
  background: linear-gradient(180deg, #cbd5e1, #94a3b8);
}

.branch-lane-entry {
  height: 22px;
}

.branch-lane-card-connector {
  height: 18px;
}

.branch-lane-exit {
  height: 22px;
}

.route-head-card {
  width: 100%;
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
  min-height: 110px;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.branch-merge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  min-width: 168px;
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

  .branch-shell {
    width: min(100%, 520px);
    min-width: 0;
  }

  .branch-split-line,
  .branch-merge-line {
    align-self: center;
    width: 2px;
    height: 18px;
    margin-inline: 0;
    background: linear-gradient(180deg, #cbd5e1, #94a3b8);
  }

  .branch-lanes {
    width: 100%;
    min-width: 0;
    grid-template-columns: 1fr;
  }

  .branch-lane {
    min-width: 0;
  }
}
</style>
