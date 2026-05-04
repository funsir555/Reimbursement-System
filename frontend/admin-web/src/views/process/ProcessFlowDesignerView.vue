<template>
  <div class="space-y-6">
    <div class="mb-6 flex justify-start">
      <div class="flex flex-wrap items-center gap-3">
        <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回上一级
        </button>
        <el-button :icon="RefreshRight" @click="reloadPageData">刷新</el-button>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-6 xl:grid-cols-[300px,minmax(0,1fr)]">
      <el-card class="!rounded-3xl !shadow-sm">
        <template #header>
          <div class="flex items-center gap-3">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">流程列表</h2>
              <p class="mt-1 text-sm text-slate-400">选择已有流程，直接进入编辑状态继续设计</p>
            </div>
          </div>
        </template>

        <div class="space-y-3" v-loading="listLoading">
          <el-input v-model="keyword" placeholder="搜索流程名称或编码" clearable />
          <button
            v-for="item in filteredFlows"
            :key="item.id"
            type="button"
            class="w-full rounded-2xl border border-slate-200 px-4 py-4 text-left transition hover:border-sky-300 hover:bg-sky-50"
            :class="item.id === working.id ? 'border-sky-300 bg-sky-50 shadow-sm' : ''"
            @click="openFlow(item.id)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate text-sm font-semibold text-slate-800">{{ item.flowName }}</p>
                <p class="mt-1 truncate text-xs text-slate-400">{{ item.flowCode }}</p>
                <p v-if="item.updatedAt" class="mt-2 text-xs text-slate-400">最近更新：{{ item.updatedAt }}</p>
              </div>
              <el-tag size="small" :type="item.status === 'ENABLED' ? 'success' : item.status === 'DISABLED' ? 'info' : 'warning'">
                {{ item.statusLabel }}
              </el-tag>
            </div>
          </button>
          <el-empty v-if="!listLoading && filteredFlows.length === 0" description="暂无流程" />
        </div>
      </el-card>

      <el-card class="designer-side-card !rounded-3xl !shadow-sm">
        <div class="designer-shell space-y-6">
          <div
            class="flow-meta-grid grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1.15fr),minmax(0,1fr),240px]"
            data-testid="flow-meta-grid"
          >
            <el-form-item label="流程名称" required class="flow-meta-item !mb-0">
              <el-input v-model="working.flowName" maxlength="64" show-word-limit placeholder="请输入流程名称" />
            </el-form-item>
            <el-form-item label="流程说明" class="flow-meta-item !mb-0">
              <el-input v-model="working.flowDescription" placeholder="请输入流程说明" />
            </el-form-item>
            <el-form-item label="流程编码" class="flow-meta-item !mb-0">
              <div class="flow-meta-readonly rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-600">
                {{ working.flowCode || '保存后自动生成' }}
              </div>
            </el-form-item>
          </div>

          <div class="flow-canvas-stage">
            <div class="flow-canvas-toolbar" data-testid="flow-canvas-toolbar">
              <el-button
                type="primary"
                :icon="Check"
                :loading="saving"
                @click="saveFlow"
              >
                保存草稿
              </el-button>
              <el-button
                type="success"
                :loading="publishing"
                @click="publishCurrentFlow"
              >
                发布流程
              </el-button>
              <el-button
                type="danger"
                plain
                :loading="disabling"
                :disabled="!working.id"
                @click="disableCurrentFlow"
              >
                停用流程
              </el-button>
            </div>

            <button
              v-if="!drawerVisible"
              type="button"
              class="designer-config-drawer-handle"
              data-testid="designer-config-drawer-handle"
              @click="toggleDrawer"
            >
              &gt;
            </button>

            <div
              class="flow-canvas-shell rounded-[32px] border border-slate-100 bg-slate-50/80 px-4 py-6 lg:px-8"
              :class="{
                'canvas-muted': drawerVisible,
                'is-panning': isCanvasPanning
              }"
              data-testid="flow-canvas-shell"
            >
              <div
                :ref="setCanvasScrollRef"
                class="flow-canvas-scroll"
                data-testid="flow-canvas-scroll"
                @pointerdown="handleCanvasPointerDown"
                @pointermove="handleCanvasPointerMove"
                @pointerup="handleCanvasPointerUp"
                @pointercancel="handleCanvasPointerCancel"
              >
                <div class="flow-canvas-surface" data-testid="flow-canvas-surface">
                  <div class="flow-track" data-testid="flow-track">
                    <div class="flow-step is-first">
                      <div class="terminal-node start-node">
                        <span class="terminal-title">开始</span>
                        <span class="terminal-desc">提交单据后进入流程</span>
                      </div>
                      <div class="flow-step-connector flow-connector flow-connector--vertical flow-step-connector--after" aria-hidden="true"></div>
                    </div>

                    <ProcessFlowCanvasRenderer
                      :blocks="canvasBlocks"
                      :selected-node-key="selectedNodeKey"
                      :selected-route-key="selectedRouteKey"
                      :dragging-node-key="draggingNodeKey"
                      :drop-target-key="dropTargetKey"
                      :scene-name-by-id="sceneName"
                      :node-type-label="nodeTypeLabel"
                      :node-card-class="nodeCardClass"
                      @insert-node="handleCanvasInsert"
                      @select-node="selectNode"
                      @select-route="selectRoute"
                      @add-route-lane="addRouteLane"
                      @drag-node-start="handleCanvasDragStart"
                      @drag-node-end="handleCanvasDragEnd"
                      @drag-node-over="handleCanvasDragOver"
                      @drop-node="handleCanvasDrop"
                    />

                    <div class="flow-step is-last">
                      <div class="terminal-node end-node">
                        <span class="terminal-title">结束</span>
                        <span class="terminal-desc">流程流转完成</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <aside
              class="designer-config-drawer"
              :class="drawerVisible ? 'is-open' : ''"
              data-testid="designer-config-drawer"
            >
              <div class="designer-config-drawer__header">
                <button
                  type="button"
                  class="designer-config-drawer__toggle"
                  data-testid="designer-config-drawer-toggle"
                  @click="toggleDrawer"
                >
                  &lt;
                </button>
                <div class="min-w-0">
                  <h2 class="text-lg font-semibold text-slate-800">{{ panelTitle }}</h2>
                  <p class="mt-1 text-sm text-slate-400">{{ panelDescription }}</p>
                </div>
                <el-button
                  v-if="selectedNode"
                  type="danger"
                  text
                  :icon="Delete"
                  :disabled="panelRemoveDisabled"
                  @click="removeSelectedItem"
                >
                  {{ removeButtonLabel }}
                </el-button>
              </div>

              <div class="designer-side-scroll" data-testid="designer-side-panel">
                <ProcessFlowRouteConditionEditorBlock
                  v-if="selectedRoute"
                  v-bind="routeConditionEditorBindings"
                />

                <div v-else-if="selectedNode" class="space-y-6">
                  <el-form-item v-if="selectedNode.nodeType !== 'BRANCH'" label="节点名称" class="!mb-0">
                    <el-input v-model="selectedNode.nodeName" maxlength="64" show-word-limit placeholder="请输入节点名称" />
                  </el-form-item>

                  <ProcessFlowApprovalNodeSection
                    v-if="selectedNode.nodeType === 'APPROVAL'"
                    v-bind="approvalSectionBindings"
                  />

                  <ProcessFlowCcNodeSection
                    v-else-if="selectedNode.nodeType === 'CC'"
                    v-bind="ccSectionBindings"
                  />

                  <ProcessFlowPaymentNodeSection
                    v-else-if="selectedNode.nodeType === 'PAYMENT'"
                    v-bind="paymentSectionBindings"
                  />

                  <ProcessFlowBranchNodeSection
                    v-else
                    v-bind="branchSectionBindings"
                  />
                </div>

                <el-empty v-else description="请先点击中间流程图中的节点或条件头卡片" />
              </div>
            </aside>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="sceneDialog.visible" title="新增流程场景" width="460px">
      <div class="space-y-4">
        <el-form-item label="场景名称" required class="!mb-0">
          <el-input v-model="sceneDialog.sceneName" maxlength="64" show-word-limit placeholder="请输入场景名称" />
        </el-form-item>
        <el-form-item label="场景说明" class="!mb-0">
          <el-input v-model="sceneDialog.sceneDescription" type="textarea" :rows="3" placeholder="请输入场景说明" />
        </el-form-item>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="resetSceneDialog">取消</el-button>
          <el-button type="primary" :loading="sceneSaving" @click="submitScene">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Check, Delete, RefreshRight } from '@element-plus/icons-vue'
import ProcessFlowCanvasRenderer from '@/components/process/ProcessFlowCanvasRenderer.vue'
import ProcessFlowApprovalNodeSection from '@/views/process/components/ProcessFlowApprovalNodeSection.vue'
import ProcessFlowBranchNodeSection from '@/views/process/components/ProcessFlowBranchNodeSection.vue'
import ProcessFlowCcNodeSection from '@/views/process/components/ProcessFlowCcNodeSection.vue'
import ProcessFlowPaymentNodeSection from '@/views/process/components/ProcessFlowPaymentNodeSection.vue'
import ProcessFlowRouteConditionEditorBlock from '@/views/process/components/ProcessFlowRouteConditionEditorBlock.vue'
import { useProcessFlowDesignerPageOrchestration } from '@/views/process/composables/useProcessFlowDesignerPageOrchestration'

const {
  drawerVisible,
  isCanvasPanning,
  listLoading,
  saving,
  publishing,
  disabling,
  sceneSaving,
  keyword,
  working,
  sceneDialog,
  selectedNodeKey,
  selectedRouteKey,
  draggingNodeKey,
  dropTargetKey,
  hasSelection,
  filteredFlows,
  canvasBlocks,
  selectedNode,
  selectedRoute,
  panelTitle,
  panelDescription,
  removeButtonLabel,
  nodeTypeLabel,
  nodeCardClass,
  sceneName,
  toggleDrawer,
  setCanvasScrollRef,
  handleCanvasPointerDown,
  handleCanvasPointerMove,
  handleCanvasPointerUp,
  handleCanvasPointerCancel,
  selectNode,
  selectRoute,
  handleCanvasDragStart,
  handleCanvasDragEnd,
  handleCanvasDragOver,
  handleCanvasDrop,
  handleCanvasInsert,
  addRouteLane,
  openFlow,
  reloadPageData,
  removeSelectedItem,
  saveFlow,
  publishCurrentFlow,
  disableCurrentFlow,
  resetSceneDialog,
  submitScene,
  goBack,
  panelRemoveDisabled,
  routeConditionEditorBindings,
  approvalSectionBindings,
  ccSectionBindings,
  paymentSectionBindings,
  branchSectionBindings
} = useProcessFlowDesignerPageOrchestration()
</script>

<style scoped>
.flow-meta-grid {
  align-items: start;
}

:deep(.flow-meta-item .el-form-item__content) {
  min-height: 44px;
}

:deep(.flow-meta-item .el-input__wrapper) {
  min-height: 44px;
}

.flow-meta-readonly {
  display: flex;
  min-height: 44px;
  align-items: center;
}

.canvas-muted {
  background: linear-gradient(180deg, rgba(241, 245, 249, 0.95), rgba(248, 250, 252, 0.92));
}

:deep(.designer-side-card) {
  overflow: hidden;
}

:deep(.designer-side-card .el-card__body) {
  padding-top: 0;
}

.designer-shell {
  position: relative;
}

.flow-canvas-stage {
  --designer-toolbar-top: 22px;
  --designer-toolbar-side: 24px;
  --designer-toolbar-clearance: 64px;
  --designer-drawer-edge: 12px;
  position: relative;
}

.flow-canvas-toolbar {
  position: absolute;
  right: var(--designer-toolbar-side);
  top: var(--designer-toolbar-top);
  z-index: 5;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.designer-config-drawer-handle {
  position: absolute;
  right: var(--designer-toolbar-side);
  top: calc(var(--designer-toolbar-top) + var(--designer-toolbar-clearance));
  z-index: 5;
  display: inline-flex;
  height: 40px;
  width: 40px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.designer-config-drawer-handle:hover {
  transform: translateY(-1px);
  border-color: #38bdf8;
  box-shadow: 0 14px 28px rgba(14, 165, 233, 0.14);
}

.flow-canvas-shell {
  overflow: hidden;
  padding-bottom: 20px;
  padding-top: calc(var(--designer-toolbar-top) + var(--designer-toolbar-clearance));
  transition: background 0.18s ease;
}

.flow-canvas-shell.is-panning {
  cursor: grabbing;
}

.flow-canvas-scroll {
  max-height: calc(100vh - 320px);
  overflow-x: auto;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 4px 18px 34px 4px;
  cursor: grab;
  touch-action: none;
}

.flow-canvas-scroll::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.flow-canvas-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.85);
}

.flow-canvas-scroll::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.92);
}

.flow-canvas-surface {
  display: flex;
  justify-content: center;
  width: max-content;
  min-width: 100%;
}

.flow-track {
  --flow-connector-size: 2px;
  --flow-connector-paint: #a8b6c7;
  --flow-connector-gap: 18px;
  --flow-connector-center-offset: calc(50% - (var(--flow-connector-size) / 2));
  display: flex;
  min-height: 560px;
  width: max-content;
  min-width: 100%;
  flex-direction: column;
  align-items: center;
  gap: var(--flow-connector-gap);
}

.flow-step {
  position: relative;
  display: flex;
  width: 100%;
  justify-content: center;
}

.flow-connector {
  pointer-events: none;
  border-radius: 999px;
  background: var(--flow-connector-paint);
}

.flow-connector--vertical {
  width: var(--flow-connector-size);
}

.flow-step-connector {
  position: absolute;
  left: var(--flow-connector-center-offset);
  z-index: 0;
  height: var(--flow-connector-gap);
}

.flow-step-connector--after {
  top: 100%;
}

.terminal-node {
  display: flex;
  min-width: 92px;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-radius: 24px;
  padding: 12px 14px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.start-node {
  background: linear-gradient(135deg, #e0f2fe, #f8fafc);
  color: #0f172a;
}

.end-node {
  background: linear-gradient(135deg, #dcfce7, #f8fafc);
  color: #14532d;
}

.terminal-title {
  font-size: 18px;
  font-weight: 700;
}

.terminal-desc {
  font-size: 13px;
  color: rgba(15, 23, 42, 0.64);
}

.designer-side-scroll {
  width: 100%;
  height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  padding-right: 8px;
}

.designer-config-drawer {
  position: absolute;
  right: var(--designer-drawer-edge);
  top: calc(var(--designer-toolbar-top) + var(--designer-toolbar-clearance));
  bottom: var(--designer-drawer-edge);
  z-index: 6;
  display: flex;
  width: min(630px, 78vw);
  flex-direction: column;
  gap: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 24px 56px rgba(15, 23, 42, 0.14);
  padding: 22px;
  transform: translateX(calc(100% + 24px));
  opacity: 0;
  pointer-events: none;
  transition: transform 0.22s ease, opacity 0.22s ease;
}

.designer-config-drawer.is-open {
  transform: translateX(0);
  opacity: 1;
  pointer-events: auto;
}

.designer-config-drawer__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 14px;
}

.designer-config-drawer__toggle {
  display: inline-flex;
  height: 36px;
  width: 36px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 999px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.designer-config-drawer__toggle:hover {
  transform: translateY(-1px);
  border-color: #38bdf8;
  box-shadow: 0 10px 22px rgba(14, 165, 233, 0.12);
}

@media (max-width: 1279px) {
  .flow-canvas-stage {
    --designer-toolbar-clearance: 72px;
  }

  .flow-meta-grid {
    grid-template-columns: 1fr;
  }

  .flow-canvas-shell {
    overflow: visible;
    padding-bottom: 0;
  }

  .flow-canvas-scroll {
    max-height: none;
    overflow: auto;
    padding: 0;
  }

  .flow-canvas-surface {
    display: block;
    width: auto;
  }

  .flow-track {
    width: auto;
    min-width: 0;
  }

  .designer-config-drawer {
    width: min(630px, calc(100% - 24px));
  }

}

@media (max-width: 767px) {
  .flow-canvas-stage {
    --designer-toolbar-top: 18px;
    --designer-toolbar-side: 20px;
    --designer-toolbar-clearance: 112px;
  }

  .flow-canvas-toolbar {
    left: 20px;
    right: 20px;
  }

  .designer-config-drawer-handle {
    right: var(--designer-toolbar-side);
  }

  .designer-config-drawer {
    left: var(--designer-drawer-edge);
    width: auto;
  }

  .designer-config-drawer__header {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .designer-config-drawer__header :deep(.el-button) {
    grid-column: 1 / -1;
    justify-self: end;
  }
}
</style>

