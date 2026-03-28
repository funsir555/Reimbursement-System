<template>
  <div class="space-y-6">
    <div class="mb-6 flex justify-end">
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
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">流程列表</h2>
              <p class="mt-1 text-sm text-slate-400">选择已有流程，或新建一个草稿继续设计</p>
            </div>
            <el-button type="primary" :icon="Plus" @click="createNewFlow">新建流程</el-button>
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
          <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,1.2fr),minmax(0,0.8fr)]">
            <el-form-item label="流程名称" required class="!mb-0">
              <el-input v-model="working.flowName" placeholder="请输入流程名称" />
            </el-form-item>
            <el-form-item label="流程编码" class="!mb-0">
              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-600">
                {{ working.flowCode || '保存后自动生成' }}
              </div>
            </el-form-item>
          </div>

          <el-form-item label="流程说明" class="!mb-0">
            <el-input v-model="working.flowDescription" type="textarea" :rows="3" placeholder="请输入流程说明" />
          </el-form-item>

          <div class="rounded-[32px] border border-slate-100 bg-slate-50/80 px-4 py-6 lg:px-8" :class="{ 'canvas-muted': hasSelection }">
            <div class="flow-track">
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
                :scene-name-by-id="sceneName"
                :node-type-label="nodeTypeLabel"
                :node-card-class="nodeCardClass"
                @insert-node="handleCanvasInsert"
                @select-node="selectNode"
                @select-route="selectRoute"
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
      </el-card>

      <el-card class="!rounded-3xl !shadow-sm">
        <template #header>
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">{{ panelTitle }}</h2>
              <p class="mt-1 text-sm text-slate-400">{{ panelDescription }}</p>
            </div>
            <el-button
              v-if="selectedNode || selectedRoute"
              type="danger"
              text
              :icon="Delete"
              :disabled="selectedRoute ? currentBranchRoutes.length <= 2 : false"
              @click="removeSelectedItem"
            >
              {{ removeButtonLabel }}
            </el-button>
          </div>
        </template>

        <div class="designer-side-scroll">
          <div v-if="selectedRoute" class="space-y-6">
            <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p class="text-base font-semibold text-slate-800">分支泳道</p>
                  <p class="mt-1 text-sm text-slate-500">
                    当前属于 {{ activeBranchNode?.nodeName || '流程分支' }}，至少保留 2 条分支泳道。
                  </p>
                </div>
                <div class="flex flex-wrap gap-2">
                  <el-button plain @click="addRouteLane(selectedRoute.sourceNodeKey)">新增分支</el-button>
                  <el-button type="danger" plain :disabled="currentBranchRoutes.length <= 2" @click="removeSelectedItem">
                    删除当前分支
                  </el-button>
                  <el-button type="danger" text @click="removeActiveBranchBlock">
                    删除整个分支块
                  </el-button>
                </div>
              </div>

              <el-form-item label="分支名称" class="!mb-0">
                <el-input v-model="selectedRoute.routeName" placeholder="请输入分支名称" />
              </el-form-item>

              <div class="route-pill-grid">
                <button
                  v-for="routeItem in currentBranchRoutes"
                  :key="routeItem.routeKey"
                  type="button"
                  class="route-pill"
                  :class="routeItem.routeKey === selectedRoute.routeKey ? 'is-selected' : ''"
                  @click="selectRoute(routeItem.routeKey)"
                >
                  <div class="min-w-0 text-left">
                    <p class="truncate text-sm font-semibold text-slate-800">{{ routeItem.routeName || '未命名分支' }}</p>
                    <p class="mt-1 text-xs text-slate-400">
                      优先级 {{ routeItem.priority }} · {{ describeRouteConditions(routeItem).groups }} 组条件 ·
                      {{ describeRouteConditions(routeItem).conditions }} 条条件
                    </p>
                  </div>
                </button>
              </div>
            </div>

            <div class="rounded-[24px] border border-slate-200 bg-white p-5 space-y-5">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p class="text-base font-semibold text-slate-800">条件设置</p>
                  <p class="mt-1 text-sm text-slate-500">
                    当前共 {{ describeRouteConditions(selectedRoute).groups }} 组条件，{{ describeRouteConditions(selectedRoute).conditions }} 条条件。
                  </p>
                </div>
                <el-button type="primary" plain @click="addConditionGroup(selectedRoute)">新增条件组</el-button>
              </div>

              <div class="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-500">
                分支条件直接保存在当前 route 的 <code>conditionGroups</code> 上，不会新增额外节点类型。
              </div>

              <div v-if="selectedRoute.conditionGroups.length" class="space-y-4">
                <div
                  v-for="group in selectedRoute.conditionGroups"
                  :key="group.groupNo"
                  class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-4"
                >
                  <div class="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p class="text-sm font-semibold text-slate-800">条件组 {{ group.groupNo }}</p>
                      <p class="mt-1 text-xs text-slate-400">每个条件组都可以继续添加多个条件项。</p>
                    </div>
                    <div class="flex flex-wrap gap-2">
                      <el-button plain @click="addCondition(group)">新增条件</el-button>
                      <el-button type="danger" text @click="removeConditionGroup(selectedRoute, group.groupNo)">删除条件组</el-button>
                    </div>
                  </div>

                  <div v-if="group.conditions.length" class="space-y-3">
                    <div
                      v-for="(condition, conditionIndex) in group.conditions"
                      :key="`${group.groupNo}-${conditionIndex}`"
                      class="rounded-2xl border border-slate-200 bg-white p-4 space-y-4"
                    >
                      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.2fr),minmax(0,0.9fr),minmax(0,1.6fr)]">
                        <el-form-item label="条件字段" class="!mb-0">
                          <el-select
                            v-model="condition.fieldKey"
                            placeholder="请选择条件字段"
                            @change="handleConditionFieldChange(condition)"
                          >
                            <el-option
                              v-for="field in metaOptions.branchConditionFields"
                              :key="field.key"
                              :label="field.label"
                              :value="field.key"
                            />
                          </el-select>
                        </el-form-item>

                        <el-form-item label="比较方式" class="!mb-0">
                          <el-select
                            v-model="condition.operator"
                            placeholder="请选择比较方式"
                            @change="handleConditionOperatorChange(condition)"
                          >
                            <el-option
                              v-for="item in operatorOptionsForField(condition.fieldKey)"
                              :key="item.value"
                              :label="item.label"
                              :value="item.value"
                            />
                          </el-select>
                        </el-form-item>

                        <el-form-item label="比较值" class="!mb-0">
                          <template v-if="isBetweenOperator(condition.operator)">
                            <div class="grid grid-cols-2 gap-3">
                              <el-input-number
                                v-model="condition.compareValue[0]"
                                class="!w-full"
                                :controls="false"
                                placeholder="起始值"
                              />
                              <el-input-number
                                v-model="condition.compareValue[1]"
                                class="!w-full"
                                :controls="false"
                                placeholder="结束值"
                              />
                            </div>
                          </template>

                          <el-select
                            v-else-if="isMultiOperator(condition.operator)"
                            v-model="condition.compareValue"
                            multiple
                            filterable
                            clearable
                            collapse-tags
                            collapse-tags-tooltip
                            :allow-create="!usesOptionSelect(condition)"
                            default-first-option
                            :placeholder="multiValuePlaceholder(condition)"
                          >
                            <el-option
                              v-for="item in conditionValueOptions(condition)"
                              :key="item.value"
                              :label="item.label"
                              :value="item.value"
                            />
                          </el-select>

                          <el-select
                            v-else-if="usesOptionSelect(condition)"
                            v-model="condition.compareValue"
                            filterable
                            clearable
                            :placeholder="singleValuePlaceholder(condition)"
                          >
                            <el-option
                              v-for="item in conditionValueOptions(condition)"
                              :key="item.value"
                              :label="item.label"
                              :value="item.value"
                            />
                          </el-select>

                          <el-input-number
                            v-else-if="isNumberCondition(condition)"
                            v-model="condition.compareValue"
                            class="!w-full"
                            :controls="false"
                            placeholder="请输入数值"
                          />

                          <el-input
                            v-else
                            v-model="condition.compareValue"
                            :placeholder="singleValuePlaceholder(condition)"
                          />
                        </el-form-item>
                      </div>

                      <div class="flex justify-end">
                        <el-button type="danger" text @click="removeCondition(group, conditionIndex)">删除条件</el-button>
                      </div>
                    </div>
                  </div>

                  <el-empty v-else description="当前条件组还没有条件项" :image-size="56" />
                </div>
              </div>

              <el-empty v-else description="当前分支还没有条件组" :image-size="64" />
            </div>
          </div>

          <div v-else-if="selectedNode" class="space-y-6">
            <el-form-item label="节点名称" class="!mb-0">
              <el-input v-model="selectedNode.nodeName" placeholder="请输入节点名称" />
            </el-form-item>

            <div v-if="selectedNode.nodeType === 'APPROVAL'" class="space-y-6">
              <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                <div class="grid grid-cols-1 gap-4">
                  <div class="grid grid-cols-[minmax(0,1fr),auto] items-end gap-3">
                    <el-form-item label="流程场景" class="!mb-0">
                      <el-select v-model="selectedNode.sceneId" placeholder="请选择流程场景" clearable>
                        <el-option v-for="item in metaOptions.sceneOptions" :key="item.id" :label="item.sceneName" :value="item.id" />
                      </el-select>
                    </el-form-item>
                    <div>
                      <el-button plain @click="sceneDialog.visible = true">添加</el-button>
                    </div>
                  </div>

                  <el-form-item label="审批人类型" class="!mb-0">
                    <el-radio-group v-model="selectedNode.config.approverType" class="flex flex-wrap gap-3">
                      <el-radio-button v-for="item in approvalApproverTypes" :key="item.value" :label="item.value">
                        {{ item.label }}
                      </el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </div>
              </div>

              <div v-if="selectedNode.config.approverType === 'MANAGER'" class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                <div class="grid grid-cols-1 gap-4">
                  <el-form-item label="主管规则" class="!mb-0">
                    <el-select v-model="selectedNode.config.managerConfig.ruleMode" placeholder="请选择主管规则">
                      <el-option
                        v-for="item in metaOptions.approvalManagerRuleModeOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="部门来源" class="!mb-0">
                    <el-select v-model="selectedNode.config.managerConfig.deptSource" placeholder="请选择部门来源">
                      <el-option
                        v-for="item in metaOptions.approvalManagerDeptSourceOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="部门级次" class="!mb-0">
                    <el-select v-model="selectedNode.config.managerConfig.managerLevel" placeholder="请选择主管级次">
                      <el-option
                        v-for="item in metaOptions.approvalManagerLevelOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="Number(item.value)"
                      />
                    </el-select>
                  </el-form-item>

                  <div class="rounded-2xl bg-white p-4">
                    <div class="flex flex-wrap items-center gap-3">
                      <el-checkbox v-model="selectedNode.config.managerConfig.orgTreeLookupEnabled">
                        按照组织架构树向上查找
                      </el-checkbox>
                      <span class="text-sm text-slate-500">未命中时继续向上查找上级主管</span>
                    </div>
                  </div>

                  <el-form-item label="向上查找级次" class="!mb-0">
                    <el-select v-model="selectedNode.config.managerConfig.orgTreeLookupLevel" placeholder="请选择查找级次">
                      <el-option
                        v-for="item in metaOptions.approvalManagerLookupLevelOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="Number(item.value)"
                      />
                    </el-select>
                  </el-form-item>
                </div>
              </div>

              <div v-else-if="selectedNode.config.approverType === 'DESIGNATED_MEMBER'" class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                <el-form-item label="指定成员" class="!mb-0">
                  <el-select
                    v-model="selectedNode.config.designatedMemberConfig.userIds"
                    multiple
                    filterable
                    clearable
                    placeholder="请选择固定审批成员"
                  >
                    <el-option
                      v-for="item in metaOptions.userOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="Number(item.value)"
                    />
                  </el-select>
                </el-form-item>
              </div>

              <div v-else class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                <el-form-item label="候选范围" class="!mb-0">
                  <el-select v-model="selectedNode.config.manualSelectConfig.candidateScope" placeholder="请选择候选范围">
                    <el-option
                      v-for="item in metaOptions.approvalManualCandidateScopeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
                <p class="mt-3 text-sm leading-6 text-slate-500">
                  提单时由提单人在候选范围内手动选择审批人，本轮默认全体有效成员。
                </p>
              </div>

              <div class="rounded-[24px] border border-slate-200 bg-white p-5 space-y-5">
                <el-form-item label="找不到审批人时" class="!mb-0">
                  <el-select v-model="selectedNode.config.missingHandler" placeholder="请选择处理策略">
                    <el-option v-for="item in metaOptions.missingHandlerOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>

                <el-form-item label="操作类型" class="!mb-0">
                  <el-radio-group v-model="selectedNode.config.approvalMode" class="flex flex-wrap gap-3">
                    <el-radio-button v-for="item in metaOptions.approvalModeOptions" :key="item.value" :label="item.value">
                      {{ item.label }}
                    </el-radio-button>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="审批意见默认值" class="!mb-0">
                  <el-select
                    v-model="selectedNode.config.opinionDefaults"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请输入或选择审批意见"
                  >
                    <el-option v-for="item in approvalOpinionCandidates" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>

                <el-form-item label="特殊设置" class="!mb-0">
                  <el-checkbox-group v-model="selectedNode.config.specialSettings" class="flex flex-col gap-3">
                    <el-checkbox v-for="item in metaOptions.approvalSpecialOptions" :key="item.value" :label="item.value">
                      {{ item.label }}
                    </el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
              </div>
            </div>

            <div v-else-if="selectedNode.nodeType === 'CC'" class="space-y-5">
              <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
                <div class="grid grid-cols-[minmax(0,1fr),auto] items-end gap-3">
                  <el-form-item label="流程场景" class="!mb-0">
                    <el-select v-model="selectedNode.sceneId" placeholder="请选择流程场景" clearable>
                      <el-option v-for="item in metaOptions.sceneOptions" :key="item.id" :label="item.sceneName" :value="item.id" />
                    </el-select>
                  </el-form-item>
                  <div>
                    <el-button plain @click="sceneDialog.visible = true">添加</el-button>
                  </div>
                </div>

                <el-form-item label="抄送对象类型" class="!mb-0">
                  <el-select v-model="selectedNode.config.receiverType" placeholder="请选择抄送对象类型">
                    <el-option v-for="item in metaOptions.ccReceiverTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>

                <el-form-item label="指定成员" class="!mb-0">
                  <el-select v-model="selectedNode.config.receiverUserIds" multiple filterable clearable placeholder="请选择抄送成员">
                    <el-option v-for="item in metaOptions.userOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
                  </el-select>
                </el-form-item>

                <el-form-item label="抄送时机" class="!mb-0">
                  <el-select v-model="selectedNode.config.timing" placeholder="请选择抄送时机">
                    <el-option v-for="item in metaOptions.ccTimingOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>

                <el-form-item label="找不到接收人时" class="!mb-0">
                  <el-select v-model="selectedNode.config.missingHandler" placeholder="请选择处理策略">
                    <el-option v-for="item in metaOptions.missingHandlerOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>

                <el-form-item label="特殊设置" class="!mb-0">
                  <el-checkbox-group v-model="selectedNode.config.specialSettings" class="flex flex-col gap-3">
                    <el-checkbox v-for="item in metaOptions.ccSpecialOptions" :key="item.value" :label="item.value">
                      {{ item.label }}
                    </el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
              </div>
            </div>

            <div v-else-if="selectedNode.nodeType === 'PAYMENT'" class="space-y-5">
              <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
                <div class="grid grid-cols-[minmax(0,1fr),auto] items-end gap-3">
                  <el-form-item label="流程场景" class="!mb-0">
                    <el-select v-model="selectedNode.sceneId" placeholder="请选择流程场景" clearable>
                      <el-option v-for="item in metaOptions.sceneOptions" :key="item.id" :label="item.sceneName" :value="item.id" />
                    </el-select>
                  </el-form-item>
                  <div>
                    <el-button plain @click="sceneDialog.visible = true">添加</el-button>
                  </div>
                </div>

                <el-form-item label="支付执行人类型" class="!mb-0">
                  <el-select v-model="selectedNode.config.executorType" placeholder="请选择支付执行人类型">
                    <el-option
                      v-for="item in metaOptions.paymentExecutorTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="指定成员" class="!mb-0">
                  <el-select v-model="selectedNode.config.executorUserIds" multiple filterable clearable placeholder="请选择支付执行成员">
                    <el-option v-for="item in metaOptions.userOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
                  </el-select>
                </el-form-item>

                <el-form-item label="支付动作" class="!mb-0">
                  <el-select v-model="selectedNode.config.paymentAction" placeholder="请选择支付动作">
                    <el-option v-for="item in metaOptions.paymentActionOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>

                <el-form-item label="找不到执行人时" class="!mb-0">
                  <el-select v-model="selectedNode.config.missingHandler" placeholder="请选择处理策略">
                    <el-option v-for="item in metaOptions.missingHandlerOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>

                <el-form-item label="特殊设置" class="!mb-0">
                  <el-checkbox-group v-model="selectedNode.config.specialSettings" class="flex flex-col gap-3">
                    <el-checkbox v-for="item in metaOptions.paymentSpecialOptions" :key="item.value" :label="item.value">
                      {{ item.label }}
                    </el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
              </div>
            </div>

            <div v-else class="space-y-5">
              <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-5">
                <div class="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p class="text-base font-semibold text-slate-800">分支管理</p>
                    <p class="mt-1 text-sm text-slate-500">
                      当前分支块包含 {{ currentBranchRoutes.length }} 条泳道，点击泳道卡片可进入各自的条件设置。
                    </p>
                  </div>
                  <el-button type="primary" plain @click="addRouteLane(selectedNode.nodeKey)">新增分支</el-button>
                </div>

                <div class="rounded-2xl border border-dashed border-slate-300 bg-white p-4 text-sm leading-7 text-slate-500">
                  <p>分支内部允许继续插入审批、抄送、支付和新的流程分支。</p>
                  <p class="mt-2">泳道顶部的“条件设置头卡片”对应当前 route，不会新增额外的后端节点类型。</p>
                </div>

                <div v-if="currentBranchRoutes.length" class="space-y-3">
                  <button
                    v-for="routeItem in currentBranchRoutes"
                    :key="routeItem.routeKey"
                    type="button"
                    class="route-summary-card"
                    @click="selectRoute(routeItem.routeKey)"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <div class="min-w-0 text-left">
                        <p class="truncate text-sm font-semibold text-slate-800">{{ routeItem.routeName || '未命名分支' }}</p>
                        <p class="mt-1 text-xs text-slate-400">
                          优先级 {{ routeItem.priority }} · {{ describeRouteConditions(routeItem).groups }} 组条件 ·
                          {{ describeRouteConditions(routeItem).conditions }} 条条件
                        </p>
                      </div>
                      <el-tag size="small" effect="plain">编辑条件</el-tag>
                    </div>
                  </button>
                </div>

                <el-empty v-else description="当前分支块还没有泳道" :image-size="60" />
              </div>
            </div>
          </div>

          <el-empty v-else description="请先点击中间流程图中的节点或条件头卡片" />
        </div>
      </el-card>
    </div>

    <div class="flex justify-end gap-3 rounded-2xl border border-slate-200 bg-white px-6 py-4 shadow-sm">
      <el-button type="primary" :icon="Check" :loading="saving" @click="saveFlow">保存草稿</el-button>
      <el-button type="success" :loading="publishing" @click="publishCurrentFlow">发布流程</el-button>
      <el-button type="danger" plain :loading="disabling" :disabled="!working.id" @click="disableCurrentFlow">停用流程</el-button>
    </div>

    <el-dialog v-model="sceneDialog.visible" title="新增流程场景" width="460px">
      <div class="space-y-4">
        <el-form-item label="场景名称" required class="!mb-0">
          <el-input v-model="sceneDialog.sceneName" placeholder="请输入场景名称" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Delete, Plus, RefreshRight } from '@element-plus/icons-vue'
import ProcessFlowCanvasRenderer from '@/components/process/ProcessFlowCanvasRenderer.vue'
import {
  processApi,
  type ProcessFlowCondition,
  type ProcessFlowConditionField,
  type ProcessFlowConditionGroup,
  type ProcessFlowDetail,
  type ProcessFlowMeta,
  type ProcessFlowNode,
  type ProcessFlowRoute,
  type ProcessFlowSavePayload,
  type ProcessFlowSummary,
  type ProcessFormOption
} from '@/api'
import {
  appendRouteToBranch,
  buildDefaultBranchRoutes,
  buildFlowCanvasBlocks,
  insertNodeIntoContainer,
  normalizeContainerKey,
  reindexFlowState,
  removeNodeAndDescendants,
  removeRouteLane,
  type FlowContainerKey,
  type FlowInsertType
} from '@/views/process/processFlowDesignerHelper'

type EditableProcessFlowCondition = Omit<ProcessFlowCondition, 'compareValue'> & {
  compareValue: any
}

type EditableProcessFlowConditionGroup = Omit<ProcessFlowConditionGroup, 'conditions'> & {
  conditions: EditableProcessFlowCondition[]
}

type EditableProcessFlowRoute = Omit<ProcessFlowRoute, 'conditionGroups'> & {
  conditionGroups: EditableProcessFlowConditionGroup[]
}

type InsertCommand = {
  containerKey: FlowContainerKey
  index: number
  nodeType: FlowInsertType
}

type SelectionPreference = {
  nodeKey?: string
  routeKey?: string
}

const route = useRoute()
const router = useRouter()

const listLoading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const disabling = ref(false)
const sceneSaving = ref(false)

const keyword = ref('')
const flows = ref<ProcessFlowSummary[]>([])
const meta = ref<ProcessFlowMeta | null>(null)
const selectedNodeKey = ref('')
const selectedRouteKey = ref('')

const working = reactive<ProcessFlowDetail>(createEmptyFlow())
const sceneDialog = reactive({
  visible: false,
  sceneName: '',
  sceneDescription: ''
})

const metaOptions = computed<ProcessFlowMeta>(() => meta.value ?? emptyMeta())
const hasSelection = computed(() => Boolean(selectedNode.value || selectedRoute.value))

const filteredFlows = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return flows.value.filter((item) => {
    if (!text) {
      return true
    }
    return item.flowName.toLowerCase().includes(text) || (item.flowCode || '').toLowerCase().includes(text)
  })
})

const canvasBlocks = computed(() => buildFlowCanvasBlocks(working.nodes || [], working.routes || []))

const selectedNode = computed(() => working.nodes.find((item) => item.nodeKey === selectedNodeKey.value))

const selectedRoute = computed<EditableProcessFlowRoute | undefined>(
  () => working.routes.find((item) => item.routeKey === selectedRouteKey.value) as EditableProcessFlowRoute | undefined
)

const selectedRouteBranchNode = computed(() => {
  if (!selectedRoute.value) {
    return undefined
  }
  return working.nodes.find((item) => item.nodeKey === selectedRoute.value?.sourceNodeKey)
})

const activeBranchNode = computed(() => {
  if (selectedRouteBranchNode.value) {
    return selectedRouteBranchNode.value
  }
  if (selectedNode.value?.nodeType === 'BRANCH') {
    return selectedNode.value
  }
  return undefined
})

const currentBranchRoutes = computed<EditableProcessFlowRoute[]>(() => {
  if (!activeBranchNode.value) {
    return []
  }
  return sortRoutes(
    working.routes.filter((item) => item.sourceNodeKey === activeBranchNode.value?.nodeKey)
  ) as EditableProcessFlowRoute[]
})

const panelTitle = computed(() => {
  if (selectedRoute.value) {
    return selectedRoute.value.routeName || '条件分支'
  }
  if (selectedNode.value) {
    return selectedNode.value.nodeName
  }
  return '节点配置'
})

const panelDescription = computed(() => {
  if (selectedRoute.value) {
    return '当前正在编辑分支泳道的名称、条件和增删管理。'
  }
  if (selectedNode.value?.nodeType === 'BRANCH') {
    return '当前正在管理分支块的泳道结构，点击泳道卡片可进入条件设置。'
  }
  if (selectedNode.value) {
    return `当前正在编辑${nodeTypeLabel(selectedNode.value.nodeType)}`
  }
  return '点击中间流程图中的节点或条件头卡片后，在这里修改配置。'
})

const removeButtonLabel = computed(() => {
  if (selectedRoute.value) {
    return '删除当前分支'
  }
  if (selectedNode.value?.nodeType === 'BRANCH') {
    return '删除分支块'
  }
  return '删除节点'
})

const approvalApproverTypes = computed(() => metaOptions.value.approvalApproverTypeOptions)
const approvalOpinionCandidates = computed(() => metaOptions.value.defaultApprovalOpinions || ['通过', '拒绝', '加签', '转交'])

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

watch(
  () => route.params.id,
  async () => {
    const routeId = parseRouteFlowId()
    if (routeId) {
      await openFlow(routeId)
      return
    }
    if (isCreateRoute()) {
      createNewFlow(false)
    }
  }
)

onMounted(async () => {
  await reloadPageData()
})

function createEmptyFlow(): ProcessFlowDetail {
  return {
    flowName: '',
    flowDescription: '',
    status: 'DRAFT',
    statusLabel: '草稿',
    nodes: [],
    routes: []
  }
}

function createBaseNodeConfig() {
  return {
    managerConfig: {} as ProcessFlowNode['config']['managerConfig'],
    designatedMemberConfig: {
      userIds: []
    } as ProcessFlowNode['config']['designatedMemberConfig'],
    manualSelectConfig: {
      candidateScope: 'ALL_ACTIVE_USERS'
    } as ProcessFlowNode['config']['manualSelectConfig']
  }
}

function emptyMeta(): ProcessFlowMeta {
  return {
    nodeTypeOptions: [],
    sceneOptions: [],
    approvalApproverTypeOptions: [],
    approvalManagerRuleModeOptions: [],
    approvalManagerDeptSourceOptions: [],
    approvalManagerLevelOptions: [],
    approvalManagerLookupLevelOptions: [],
    approvalManualCandidateScopeOptions: [],
    ccReceiverTypeOptions: [],
    paymentExecutorTypeOptions: [],
    missingHandlerOptions: [],
    approvalModeOptions: [],
    defaultApprovalOpinions: ['通过', '拒绝', '加签', '转交'],
    approvalSpecialOptions: [],
    ccTimingOptions: [],
    ccSpecialOptions: [],
    paymentActionOptions: [],
    paymentSpecialOptions: [],
    branchOperatorOptions: [],
    branchConditionFields: [],
    departmentOptions: [],
    userOptions: [],
    expenseTypeOptions: [],
    archiveOptions: []
  }
}

async function reloadPageData() {
  listLoading.value = true
  try {
    const [flowRes, metaRes] = await Promise.all([processApi.listFlows(), processApi.getFlowMeta()])
    flows.value = flowRes.data
    meta.value = metaRes.data

    const routeId = parseRouteFlowId()
    if (routeId) {
      await openFlow(routeId, false)
      return
    }

    if (isCreateRoute()) {
      if (!working.id) {
        createNewFlow(false)
      }
      return
    }

    if (working.id && flows.value.some((item) => item.id === working.id)) {
      await openFlow(working.id, false)
      return
    }

    const firstFlow = flows.value[0]
    if (firstFlow) {
      await openFlow(firstFlow.id, false)
      return
    }

    createNewFlow(false)
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载流程设计数据失败'))
  } finally {
    listLoading.value = false
  }
}

function parseRouteFlowId() {
  const raw = route.params.id
  if (raw === undefined || raw === null) {
    return null
  }
  const value = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isFinite(value) ? value : null
}

function isCreateRoute() {
  return route.name === 'expense-workbench-process-flow-create'
}

function createNewFlow(syncRoute = true) {
  Object.assign(working, createEmptyFlow())
  selectedNodeKey.value = ''
  selectedRouteKey.value = ''
  if (syncRoute && !isCreateRoute()) {
    router.push({
      name: 'expense-workbench-process-flow-create',
      query: { ...route.query }
    })
  }
}

async function openFlow(id: number, showMessage = false) {
  try {
    const res = await processApi.getFlowDetail(id)
    const detail = normalizeFlowDetail(res.data)
    Object.assign(working, detail)
    restoreSelection()
    if (showMessage) {
      ElMessage.success('流程已切换')
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '加载流程详情失败'))
  }
}

function normalizeFlowDetail(detail: ProcessFlowDetail): ProcessFlowDetail {
  const graph = prepareGraph(detail.nodes || [], detail.routes || [])
  return {
    ...createEmptyFlow(),
    ...cloneValue(detail),
    nodes: graph.nodes,
    routes: graph.routes
  }
}

function prepareGraph(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[]) {
  const normalizedNodes = (nodes || []).map((node, index) => normalizeNode(node, index))
  const normalizedRoutes = (routes || []).map((route, index) => normalizeRoute(route, index))
  const snapshot = reindexFlowState(normalizedNodes, normalizedRoutes)
  return {
    nodes: snapshot.nodes.map((node, index) => normalizeNode(node, index)),
    routes: snapshot.routes.map((route, index) => normalizeRoute(route, index))
  }
}

function applyWorkingGraph(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[], preferred: SelectionPreference = {}) {
  const graph = prepareGraph(nodes, routes)
  working.nodes = graph.nodes
  working.routes = graph.routes
  restoreSelection(preferred)
}

function restoreSelection(preferred: SelectionPreference = {}) {
  const preferredSelection = resolveSelectionPreference(preferred)
  if (preferredSelection) {
    applySelection(preferredSelection)
    return
  }

  const currentRouteSelection = resolveSelectionPreference({ routeKey: selectedRouteKey.value })
  if (currentRouteSelection) {
    applySelection(currentRouteSelection)
    return
  }

  const currentNodeSelection = resolveSelectionPreference({ nodeKey: selectedNodeKey.value })
  if (currentNodeSelection) {
    applySelection(currentNodeSelection)
    return
  }

  const initialSelection = findFirstVisibleSelection()
  if (initialSelection) {
    applySelection(initialSelection)
    return
  }

  selectedRouteKey.value = ''
  selectedNodeKey.value = ''
}

function applySelection(selection: SelectionPreference) {
  if (selection.routeKey) {
    selectedRouteKey.value = selection.routeKey
    selectedNodeKey.value = ''
    return
  }
  selectedNodeKey.value = selection.nodeKey || ''
  selectedRouteKey.value = ''
}

function resolveSelectionPreference(preferred: SelectionPreference): SelectionPreference | null {
  if (preferred.routeKey && working.routes.some((item) => item.routeKey === preferred.routeKey)) {
    return {
      routeKey: preferred.routeKey
    }
  }

  if (!preferred.nodeKey) {
    return null
  }

  const node = working.nodes.find((item) => item.nodeKey === preferred.nodeKey)
  if (!node) {
    return null
  }

  if (node.nodeType === 'BRANCH') {
    const firstRoute = getFirstRouteForBranch(node.nodeKey)
    if (firstRoute) {
      return {
        routeKey: firstRoute.routeKey
      }
    }
  }

  return {
    nodeKey: node.nodeKey
  }
}

function findFirstVisibleSelection(): SelectionPreference | null {
  const firstRootNode = listNodesInContainer(working.nodes || [], null)[0]
  if (!firstRootNode) {
    return null
  }
  return resolveSelectionPreference({ nodeKey: firstRootNode.nodeKey })
}

function normalizeNode(node: ProcessFlowNode, index: number): ProcessFlowNode {
  const baseConfig = createBaseNodeConfig()
  const normalized: ProcessFlowNode = {
    ...cloneValue(node),
    displayOrder: node.displayOrder ?? index + 1,
    config: {
      ...baseConfig,
      ...cloneValue(node.config || {}),
      managerConfig: {
        ...baseConfig.managerConfig,
        ...cloneValue(node.config?.managerConfig || {})
      },
      designatedMemberConfig: {
        ...baseConfig.designatedMemberConfig,
        ...cloneValue(node.config?.designatedMemberConfig || {})
      },
      manualSelectConfig: {
        ...baseConfig.manualSelectConfig,
        ...cloneValue(node.config?.manualSelectConfig || {})
      }
    }
  }

  if (normalized.nodeType === 'APPROVAL') {
    const managerConfig = cloneValue(normalized.config.managerConfig || {})
    normalized.config = {
      approverType: normalized.config.approverType || 'MANAGER',
      missingHandler: normalized.config.missingHandler === 'MANUAL_SELECT_ON_SUBMIT' ? 'BLOCK_SUBMIT' : (normalized.config.missingHandler || 'AUTO_SKIP'),
      approvalMode: normalized.config.approvalMode || 'OR_SIGN',
      opinionDefaults: Array.isArray(normalized.config.opinionDefaults) && normalized.config.opinionDefaults.length
        ? normalized.config.opinionDefaults
        : ['通过', '拒绝', '加签', '转交'],
      specialSettings: Array.isArray(normalized.config.specialSettings) ? normalized.config.specialSettings : [],
      managerConfig: {
        ruleMode: managerConfig.ruleMode || 'FORM_DEPT_MANAGER',
        deptSource: managerConfig.deptSource || 'UNDERTAKE_DEPT',
        managerLevel: Number(managerConfig.managerLevel || 1),
        orgTreeLookupEnabled: managerConfig.orgTreeLookupEnabled ?? true,
        orgTreeLookupLevel: Number(managerConfig.orgTreeLookupLevel || 1)
      },
      designatedMemberConfig: {
        userIds: normalizeNumberArray(normalized.config.designatedMemberConfig?.userIds)
      },
      manualSelectConfig: {
        candidateScope: normalized.config.manualSelectConfig?.candidateScope || 'ALL_ACTIVE_USERS'
      }
    }
    return normalized
  }

  if (normalized.nodeType === 'CC') {
    normalized.config = {
      ...createBaseNodeConfig(),
      receiverType: normalized.config.receiverType || 'DESIGNATED_MEMBER',
      receiverUserIds: normalizeNumberArray(normalized.config.receiverUserIds),
      timing: normalized.config.timing || 'ON_ENTER',
      missingHandler: normalized.config.missingHandler || 'AUTO_SKIP',
      specialSettings: Array.isArray(normalized.config.specialSettings) ? normalized.config.specialSettings : []
    }
    return normalized
  }

  if (normalized.nodeType === 'PAYMENT') {
    normalized.config = {
      ...createBaseNodeConfig(),
      executorType: normalized.config.executorType || 'DESIGNATED_MEMBER',
      executorUserIds: normalizeNumberArray(normalized.config.executorUserIds),
      paymentAction: normalized.config.paymentAction || 'GENERATE_PAYMENT',
      missingHandler: normalized.config.missingHandler || 'AUTO_SKIP',
      specialSettings: Array.isArray(normalized.config.specialSettings) ? normalized.config.specialSettings : []
    }
    return normalized
  }

  normalized.config = {
    ...normalized.config
  }
  return normalized
}

function normalizeRoute(route: ProcessFlowRoute, index: number): EditableProcessFlowRoute {
  return {
    ...cloneValue(route),
    routeName: route.routeName || `条件分支 ${index + 1}`,
    priority: route.priority ?? index + 1,
    defaultRoute: false,
    conditionGroups: normalizeConditionGroups(route.conditionGroups || [])
  }
}

function normalizeConditionGroups(source: ProcessFlowConditionGroup[]): EditableProcessFlowConditionGroup[] {
  return (source || []).map((group, index) => ({
    groupNo: group.groupNo ?? index + 1,
    conditions: (group.conditions || []).map((condition) => normalizeCondition(condition))
  }))
}

function normalizeCondition(source?: ProcessFlowCondition): EditableProcessFlowCondition {
  const fallbackField = metaOptions.value.branchConditionFields[0]
  const field = getConditionField(source?.fieldKey) || fallbackField
  const fieldKey = source?.fieldKey || field?.key || ''
  const operatorOptions = operatorOptionsForField(fieldKey)
  const operator = source?.operator && operatorOptions.some((item) => item.value === source.operator)
    ? source.operator
    : operatorOptions[0]?.value || 'EQ'

  return {
    fieldKey,
    operator,
    compareValue: normalizeConditionCompareValue(source?.compareValue, getConditionField(fieldKey)?.valueType, operator)
  }
}

function normalizeNumberArray(source: unknown): number[] {
  if (!Array.isArray(source)) {
    return []
  }
  return source.map((item) => Number(item)).filter((item) => Number.isFinite(item))
}

function cloneValue<T>(value: T): T {
  if (value === undefined || value === null) {
    return value
  }
  return JSON.parse(JSON.stringify(value))
}

function selectNode(nodeKey: string) {
  selectedNodeKey.value = nodeKey
  selectedRouteKey.value = ''
}

function selectRoute(routeKey: string) {
  selectedRouteKey.value = routeKey
  selectedNodeKey.value = ''
}

function handleCanvasInsert(payload: InsertCommand) {
  const node = buildNodeByType(payload.nodeType, payload.index)
  const inserted = insertNodeIntoContainer(working.nodes || [], node, payload.containerKey, payload.index)
  let nextRoutes = cloneValue(working.routes || [])
  let preferredSelection: SelectionPreference = { nodeKey: node.nodeKey }

  if (payload.nodeType === 'BRANCH') {
    const createdRoutes = buildDefaultBranchRoutes(node.nodeKey)
    nextRoutes = [...nextRoutes, ...createdRoutes]
    preferredSelection = createdRoutes[0]
      ? { routeKey: createdRoutes[0].routeKey }
      : { nodeKey: node.nodeKey }
  }

  applyWorkingGraph(inserted.nodes, nextRoutes, preferredSelection)
}

function buildNodeByType(nodeType: FlowInsertType, index: number): ProcessFlowNode {
  const stamp = `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
  const order = index + 1

  if (nodeType === 'CC') {
    return {
      nodeKey: `cc-${stamp}`,
      nodeType: 'CC',
      nodeName: `抄送节点 ${order}`,
      sceneId: undefined,
      parentNodeKey: '',
      displayOrder: order,
      config: {
        ...createBaseNodeConfig(),
        receiverType: 'DESIGNATED_MEMBER',
        receiverUserIds: [],
        timing: 'ON_ENTER',
        missingHandler: 'AUTO_SKIP',
        specialSettings: []
      }
    }
  }

  if (nodeType === 'PAYMENT') {
    return {
      nodeKey: `payment-${stamp}`,
      nodeType: 'PAYMENT',
      nodeName: `支付节点 ${order}`,
      sceneId: undefined,
      parentNodeKey: '',
      displayOrder: order,
      config: {
        ...createBaseNodeConfig(),
        executorType: 'DESIGNATED_MEMBER',
        executorUserIds: [],
        paymentAction: 'GENERATE_PAYMENT',
        missingHandler: 'AUTO_SKIP',
        specialSettings: []
      }
    }
  }

  if (nodeType === 'BRANCH') {
    return {
      nodeKey: `branch-${stamp}`,
      nodeType: 'BRANCH',
      nodeName: `流程分支 ${order}`,
      sceneId: undefined,
      parentNodeKey: '',
      displayOrder: order,
      config: createBaseNodeConfig()
    }
  }

  return {
    nodeKey: `approval-${stamp}`,
    nodeType: 'APPROVAL',
    nodeName: `审批节点 ${order}`,
    sceneId: undefined,
    parentNodeKey: '',
    displayOrder: order,
    config: {
      ...createBaseNodeConfig(),
      approverType: 'MANAGER',
      missingHandler: 'AUTO_SKIP',
      approvalMode: 'OR_SIGN',
      opinionDefaults: ['通过', '拒绝', '加签', '转交'],
      specialSettings: [],
      managerConfig: {
        ruleMode: 'FORM_DEPT_MANAGER',
        deptSource: 'UNDERTAKE_DEPT',
        managerLevel: 1,
        orgTreeLookupEnabled: true,
        orgTreeLookupLevel: 1
      },
      designatedMemberConfig: {
        userIds: []
      },
      manualSelectConfig: {
        candidateScope: 'ALL_ACTIVE_USERS'
      }
    }
  }
}

function addRouteLane(branchNodeKey: string) {
  const existingKeys = new Set((working.routes || []).map((item) => item.routeKey))
  const nextRoutes = appendRouteToBranch(working.routes || [], branchNodeKey)
  const addedRoute = nextRoutes.find((item) => item.sourceNodeKey === branchNodeKey && !existingKeys.has(item.routeKey))
  applyWorkingGraph(working.nodes || [], nextRoutes, addedRoute ? { routeKey: addedRoute.routeKey } : { nodeKey: branchNodeKey })
}

async function removeSelectedItem() {
  if (selectedRoute.value) {
    await removeSelectedRoute()
    return
  }
  if (selectedNode.value) {
    await removeSelectedNode()
  }
}

async function removeSelectedRoute() {
  if (!selectedRoute.value) {
    return
  }
  if (currentBranchRoutes.value.length <= 2) {
    ElMessage.warning('流程分支至少保留 2 条泳道')
    return
  }

  try {
    await ElMessageBox.confirm('删除当前分支后，该泳道下的全部节点和嵌套分支都会一并删除。确定继续吗？', '删除分支', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const sourceNodeKey = selectedRoute.value.sourceNodeKey
  const snapshot = removeRouteLane(working.nodes || [], working.routes || [], selectedRoute.value.routeKey)
  const remainingRoute = sortRoutes(snapshot.routes.filter((item) => item.sourceNodeKey === sourceNodeKey))[0]
  applyWorkingGraph(snapshot.nodes, snapshot.routes, remainingRoute ? { routeKey: remainingRoute.routeKey } : { nodeKey: sourceNodeKey })
  ElMessage.success('分支已删除')
}

async function removeSelectedNode() {
  if (!selectedNode.value) {
    return
  }

  const currentKey = selectedNode.value.nodeKey
  const isBranchNode = selectedNode.value.nodeType === 'BRANCH'

  try {
    await ElMessageBox.confirm(
      isBranchNode ? '删除该分支块后，内部所有泳道、节点和嵌套分支都会一并删除。确定继续吗？' : '确定删除当前节点吗？',
      isBranchNode ? '删除分支块' : '删除节点',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  const snapshot = removeNodeAndDescendants(working.nodes || [], working.routes || [], currentKey)
  const adjacentNodeKey = findAdjacentNodeKeyAfterRemoval(selectedNode.value, snapshot.nodes)
  applyWorkingGraph(snapshot.nodes, snapshot.routes, adjacentNodeKey ? { nodeKey: adjacentNodeKey } : {})
  ElMessage.success(isBranchNode ? '分支块已删除' : '节点已删除')
}

async function removeActiveBranchBlock() {
  if (!activeBranchNode.value) {
    return
  }

  try {
    await ElMessageBox.confirm('删除该分支块后，内部所有泳道、节点和嵌套分支都会一并删除。确定继续吗？', '删除分支块', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const branchNode = activeBranchNode.value
  const snapshot = removeNodeAndDescendants(working.nodes || [], working.routes || [], branchNode.nodeKey)
  const adjacentNodeKey = findAdjacentNodeKeyAfterRemoval(branchNode, snapshot.nodes)
  applyWorkingGraph(snapshot.nodes, snapshot.routes, adjacentNodeKey ? { nodeKey: adjacentNodeKey } : {})
  ElMessage.success('分支块已删除')
}

function addConditionGroup(route: EditableProcessFlowRoute) {
  route.conditionGroups.push({
    groupNo: route.conditionGroups.length + 1,
    conditions: [createEmptyCondition()]
  })
  reindexConditionGroups(route)
}

function removeConditionGroup(route: EditableProcessFlowRoute, groupNo: number) {
  route.conditionGroups = route.conditionGroups.filter((item) => item.groupNo !== groupNo)
  reindexConditionGroups(route)
}

function addCondition(group: EditableProcessFlowConditionGroup) {
  group.conditions.push(createEmptyCondition())
}

function removeCondition(group: EditableProcessFlowConditionGroup, index: number) {
  group.conditions.splice(index, 1)
}

function reindexConditionGroups(route: EditableProcessFlowRoute) {
  route.conditionGroups = route.conditionGroups.map((item, index) => ({
    ...item,
    groupNo: index + 1
  }))
}

function createEmptyCondition(): EditableProcessFlowCondition {
  return normalizeCondition({
    fieldKey: metaOptions.value.branchConditionFields[0]?.key || '',
    operator: metaOptions.value.branchConditionFields[0]?.operatorKeys?.[0] || 'EQ',
    compareValue: undefined
  })
}

function handleConditionFieldChange(condition: EditableProcessFlowCondition) {
  const field = getConditionField(condition.fieldKey) || metaOptions.value.branchConditionFields[0]
  condition.fieldKey = field?.key || ''
  condition.operator = field?.operatorKeys?.[0] || 'EQ'
  condition.compareValue = normalizeConditionCompareValue(undefined, field?.valueType, condition.operator)
}

function handleConditionOperatorChange(condition: EditableProcessFlowCondition) {
  condition.compareValue = normalizeConditionCompareValue(
    condition.compareValue,
    getConditionField(condition.fieldKey)?.valueType,
    condition.operator
  )
}

function normalizeConditionCompareValue(value: unknown, valueType?: string, operator?: string) {
  if (operator === 'BETWEEN') {
    const source = Array.isArray(value) ? value : []
    return [
      normalizeScalarCompareValue(source[0], valueType),
      normalizeScalarCompareValue(source[1], valueType)
    ]
  }

  if (isMultiOperator(operator || '')) {
    const source = Array.isArray(value) ? value : value === undefined || value === null || value === '' ? [] : [value]
    return source.map((item) => normalizeScalarCompareValue(item, valueType))
  }

  return normalizeScalarCompareValue(value, valueType)
}

function normalizeScalarCompareValue(value: unknown, valueType?: string) {
  if (valueType === 'number') {
    if (value === undefined || value === null || value === '') {
      return null
    }
    const numeric = Number(value)
    return Number.isFinite(numeric) ? numeric : null
  }

  if (isOptionValueType(valueType)) {
    if (value === undefined || value === null || value === '') {
      return ''
    }
    return String(value)
  }

  if (value === undefined || value === null) {
    return ''
  }
  return String(value)
}

function getConditionField(fieldKey?: string): ProcessFlowConditionField | undefined {
  return metaOptions.value.branchConditionFields.find((item) => item.key === fieldKey)
}

function operatorOptionsForField(fieldKey?: string) {
  const field = getConditionField(fieldKey)
  if (!field) {
    return metaOptions.value.branchOperatorOptions
  }
  return metaOptions.value.branchOperatorOptions.filter((item) => field.operatorKeys.includes(item.value))
}

function conditionValueOptions(condition: EditableProcessFlowCondition): ProcessFormOption[] {
  const valueType = getConditionField(condition.fieldKey)?.valueType
  switch (valueType) {
    case 'department':
      return metaOptions.value.departmentOptions
    case 'user':
      return metaOptions.value.userOptions
    case 'expenseType':
      return metaOptions.value.expenseTypeOptions
    case 'archive':
      return metaOptions.value.archiveOptions
    default:
      return []
  }
}

function usesOptionSelect(condition: EditableProcessFlowCondition) {
  return isOptionValueType(getConditionField(condition.fieldKey)?.valueType)
}

function isNumberCondition(condition: EditableProcessFlowCondition) {
  return getConditionField(condition.fieldKey)?.valueType === 'number' && !isBetweenOperator(condition.operator)
}

function singleValuePlaceholder(condition: EditableProcessFlowCondition) {
  return usesOptionSelect(condition) ? '请选择比较值' : '请输入比较值'
}

function multiValuePlaceholder(condition: EditableProcessFlowCondition) {
  return usesOptionSelect(condition) ? '请选择多个比较值' : '请输入多个比较值后回车'
}

function isOptionValueType(valueType?: string) {
  return ['department', 'user', 'expenseType', 'archive'].includes(valueType || '')
}

function isMultiOperator(operator: string) {
  return operator === 'IN' || operator === 'NOT_IN'
}

function isBetweenOperator(operator: string) {
  return operator === 'BETWEEN'
}

function describeRouteConditions(route?: ProcessFlowRoute) {
  const groups = route?.conditionGroups?.length || 0
  const conditions = (route?.conditionGroups || []).reduce((total, item) => total + (item.conditions?.length || 0), 0)
  return {
    groups,
    conditions
  }
}

function sortRoutes(routes: ProcessFlowRoute[]) {
  return [...routes].sort((left, right) => {
    const priorityGap = (left.priority ?? 0) - (right.priority ?? 0)
    if (priorityGap !== 0) {
      return priorityGap
    }
    return left.routeKey.localeCompare(right.routeKey)
  })
}

function getFirstRouteForBranch(branchNodeKey: string) {
  return sortRoutes((working.routes || []).filter((item) => item.sourceNodeKey === branchNodeKey))[0]
}

function listNodesInContainer(nodes: ProcessFlowNode[], containerKey: FlowContainerKey) {
  return [...nodes]
    .filter((item) => normalizeContainerKey(item.parentNodeKey) === containerKey)
    .sort((left, right) => {
      const orderGap = (left.displayOrder ?? 0) - (right.displayOrder ?? 0)
      if (orderGap !== 0) {
        return orderGap
      }
      return left.nodeKey.localeCompare(right.nodeKey)
    })
}

function findAdjacentNodeKeyAfterRemoval(removedNode: ProcessFlowNode, nodes: ProcessFlowNode[]) {
  const siblings = listNodesInContainer(nodes, normalizeContainerKey(removedNode.parentNodeKey))
  const nextSibling = siblings.find((item) => (item.displayOrder ?? 0) >= (removedNode.displayOrder ?? 0))
  if (nextSibling) {
    return nextSibling.nodeKey
  }
  return siblings[siblings.length - 1]?.nodeKey || ''
}

function nodeTypeLabel(nodeType: string) {
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

function nodeCardClass(nodeType: string) {
  switch (nodeType) {
    case 'CC':
      return 'is-cc'
    case 'PAYMENT':
      return 'is-payment'
    case 'BRANCH':
      return 'is-branch'
    default:
      return 'is-approval'
  }
}

function sceneName(sceneId?: number) {
  if (!sceneId) {
    return ''
  }
  return metaOptions.value.sceneOptions.find((item) => item.id === sceneId)?.sceneName || ''
}

function buildPayload(): ProcessFlowSavePayload {
  const graph = prepareGraph(working.nodes || [], working.routes || [])
  return {
    flowName: working.flowName.trim(),
    flowDescription: working.flowDescription?.trim() || '',
    nodes: graph.nodes.map((node, index) => cloneValue(normalizeNode(node, index))),
    routes: graph.routes.map((route, index) => ({
      ...cloneValue(normalizeRoute(route, index)),
      defaultRoute: false
    }))
  }
}

async function saveFlow() {
  if (!working.flowName?.trim()) {
    ElMessage.warning('请先填写流程名称')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    const preferred = selectedRoute.value ? { routeKey: selectedRoute.value.routeKey } : { nodeKey: selectedNode.value?.nodeKey }
    const res = working.id ? await processApi.updateFlow(working.id, payload) : await processApi.createFlow(payload)
    Object.assign(working, normalizeFlowDetail(res.data))
    restoreSelection(preferred)
    await reloadFlowListOnly()
    ElMessage.success('流程草稿已保存')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '保存流程失败'))
  } finally {
    saving.value = false
  }
}

async function publishCurrentFlow() {
  if (!working.id) {
    await saveFlow()
  }
  if (!working.id) {
    return
  }

  publishing.value = true
  try {
    const preferred = selectedRoute.value ? { routeKey: selectedRoute.value.routeKey } : { nodeKey: selectedNode.value?.nodeKey }
    const res = await processApi.publishFlow(working.id)
    Object.assign(working, normalizeFlowDetail(res.data))
    restoreSelection(preferred)
    await reloadFlowListOnly()
    ElMessage.success('流程已发布')

    const returnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo : ''
    if (returnTo && working.flowCode) {
      await router.push(appendQueryParam(returnTo, 'createdFlowCode', working.flowCode))
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '发布流程失败'))
  } finally {
    publishing.value = false
  }
}

async function disableCurrentFlow() {
  if (!working.id) {
    return
  }

  try {
    await ElMessageBox.confirm('停用后，该流程将不能继续作为启用流程被模板选择。确定继续吗？', '停用流程', {
      type: 'warning',
      confirmButtonText: '确定停用',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  disabling.value = true
  try {
    await processApi.updateFlowStatus(working.id, { status: 'DISABLED' })
    working.status = 'DISABLED'
    working.statusLabel = '已停用'
    await reloadFlowListOnly()
    ElMessage.success('流程已停用')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '停用流程失败'))
  } finally {
    disabling.value = false
  }
}

async function reloadFlowListOnly() {
  const res = await processApi.listFlows()
  flows.value = res.data
}

function resetSceneDialog() {
  sceneDialog.visible = false
  sceneDialog.sceneName = ''
  sceneDialog.sceneDescription = ''
}

async function submitScene() {
  if (!sceneDialog.sceneName.trim()) {
    ElMessage.warning('请输入场景名称')
    return
  }

  sceneSaving.value = true
  try {
    const res = await processApi.createFlowScene({
      sceneName: sceneDialog.sceneName.trim(),
      sceneDescription: sceneDialog.sceneDescription.trim()
    })
    meta.value = {
      ...metaOptions.value,
      sceneOptions: [...metaOptions.value.sceneOptions, res.data]
    }
    if (selectedNode.value) {
      selectedNode.value.sceneId = res.data.id
    }
    resetSceneDialog()
    ElMessage.success('流程场景已添加')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '新增流程场景失败'))
  } finally {
    sceneSaving.value = false
  }
}

function goBack() {
  const returnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo : ''
  router.push(returnTo || '/expense/workbench/process-management')
}

function appendQueryParam(path: string, key: string, value: string) {
  const separator = path.includes('?') ? '&' : '?'
  return `${path}${separator}${key}=${encodeURIComponent(value)}`
}
</script>

<style scoped>
.canvas-muted {
  background: linear-gradient(180deg, rgba(241, 245, 249, 0.95), rgba(248, 250, 252, 0.92));
}

.flow-track {
  display: flex;
  min-height: 560px;
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
  min-width: 152px;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-radius: 24px;
  padding: 18px 22px;
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
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  padding-right: 8px;
}

.designer-side-scroll::-webkit-scrollbar {
  width: 8px;
}

.designer-side-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.85);
}

.designer-side-scroll::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.92);
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
  padding: 14px 16px;
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
  padding: 14px 16px;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

@media (max-width: 1279px) {
  .designer-side-scroll {
    max-height: none;
    overflow-y: visible;
    padding-right: 0;
  }
}
</style>
