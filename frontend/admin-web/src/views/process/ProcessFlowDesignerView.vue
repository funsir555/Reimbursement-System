<template>
  <div class="space-y-6 pb-36">
    <div class="mb-6 flex justify-start">
      <div class="flex flex-wrap items-center gap-3">
        <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回上一级
        </button>
        <el-button :icon="RefreshRight" @click="reloadPageData">刷新</el-button>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-6 xl:grid-cols-[300px,minmax(0,1fr),420px]">
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
        <div class="space-y-6">
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

          <div
            class="flow-canvas-shell rounded-[32px] border border-slate-100 bg-slate-50/80 px-4 py-6 lg:px-8"
            :class="{ 'canvas-muted': hasSelection }"
            data-testid="flow-canvas-shell"
          >
            <div class="flow-canvas-scroll" data-testid="flow-canvas-scroll">
              <div class="flow-canvas-surface" data-testid="flow-canvas-surface">
                <div class="flow-track" data-testid="flow-track">
                  <div class="flow-step is-first">
                    <div class="terminal-node start-node">
                      <span class="terminal-title">开始</span>
                      <span class="terminal-desc">提交单据后进入流程</span>
                    </div>
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
        </div>
      </el-card>

      <el-card class="!rounded-3xl !shadow-sm">
        <template #header>
          <div class="flex items-center justify-between gap-3">
            <div>
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
        </template>

        <div class="designer-side-scroll" data-testid="designer-side-panel">
          <ProcessFlowRouteConditionEditorBlock
            v-if="selectedRoute"
            v-bind="routeConditionEditorBindings"
          />

          <div v-else-if="selectedNode" class="space-y-6">
            <el-form-item label="节点名称" class="!mb-0">
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
      </el-card>
    </div>

    <div
      class="process-flow-designer-floating-bar sticky bottom-0 z-10 mt-10"
      data-testid="process-flow-designer-floating-bar"
    >
      <div
        class="process-flow-designer-floating-bar__inner"
        data-testid="process-flow-designer-floating-bar-inner"
      >
        <el-button
          class="process-flow-designer-floating-bar__button"
          type="primary"
          :icon="Check"
          :loading="saving"
          @click="saveFlow"
        >
          保存草稿
        </el-button>
        <el-button
          class="process-flow-designer-floating-bar__button process-flow-designer-floating-bar__button--success"
          type="success"
          :loading="publishing"
          @click="publishCurrentFlow"
        >
          发布流程
        </el-button>
        <el-button
          class="process-flow-designer-floating-bar__button"
          type="danger"
          plain
          :loading="disabling"
          :disabled="!working.id"
          @click="disableCurrentFlow"
        >
          停用流程
        </el-button>
      </div>
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

.flow-canvas-shell {
  overflow: hidden;
  padding-bottom: 20px;
}

.flow-canvas-scroll {
  max-height: calc(100vh - 320px);
  overflow-x: auto;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 4px 18px 34px 4px;
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
  display: flex;
  min-height: 560px;
  width: max-content;
  min-width: 100%;
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

.flow-step.is-first::before,
.flow-step.is-last::after {
  display: none;
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

:deep(.designer-side-card) {
  overflow: hidden;
}

:deep(.designer-side-card .el-card__body) {
  padding-top: 0;
}

.designer-side-scroll {
  width: 100%;
  overflow-x: hidden;
}

.route-pill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.route-pill {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #fff;
  padding: 12px 14px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.route-pill:hover,
.route-summary-card:hover {
  transform: translateY(-1px);
  border-color: #7dd3fc;
}

.route-pill.is-selected {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.12);
}

.route-summary-card {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #fff;
  padding: 12px 14px;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.process-flow-designer-floating-bar {
  display: flex;
  justify-content: center;
  width: 100%;
  padding-top: 12px;
}

.process-flow-designer-floating-bar__inner {
  display: flex;
  width: min(95vw, 1680px);
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12) 0%, rgba(248, 250, 252, 0.1) 100%);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
  padding: 18px 22px;
  font-size: 15px;
}

:deep(.process-flow-designer-floating-bar__button.el-button) {
  min-height: 38px;
  padding: 0 20px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 600;
}

:deep(.process-flow-designer-floating-bar__button--success.el-button) {
  box-shadow: 0 12px 24px rgba(34, 197, 94, 0.14);
}

@media (max-width: 1279px) {
  .flow-meta-grid {
    grid-template-columns: 1fr;
  }

  .flow-canvas-shell {
    overflow: visible;
    padding-bottom: 0;
  }

  .flow-canvas-scroll {
    max-height: none;
    overflow: visible;
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

}

@media (max-width: 767px) {
  .process-flow-designer-floating-bar__inner {
    width: calc(100vw - 24px);
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 14px;
    font-size: 14px;
  }

  :deep(.process-flow-designer-floating-bar__button.el-button) {
    flex: 1 1 calc(50% - 6px);
    min-height: 34px;
    padding: 0 14px;
    font-size: 14px;
  }
}
</style>

