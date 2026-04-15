<template>
  <div class="space-y-6">
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap items-center gap-3">
        <button type="button" class="flex items-center gap-2 text-sm text-blue-600" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回上一层
        </button>
        <el-button :icon="RefreshRight" @click="loadPage">刷新</el-button>
      </div>

      <div class="flex flex-wrap items-center gap-3" data-testid="process-form-designer-header-actions">
        <el-tag effect="plain">{{ templateTypeLabel }}</el-tag>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-6 xl:grid-cols-[404px,minmax(0,1fr),380px]">
      <el-card
        class="palette-card !rounded-3xl !shadow-sm"
        :class="paletteDeleteActive ? 'is-delete-target' : ''"
        v-loading="loading"
        @dragover="handlePaletteDeleteDragOver"
        @drop="handlePaletteDeleteDrop"
        @dragleave="handlePaletteDeleteDragLeave"
      >
        <template #header>
          <div class="space-y-4">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">组件库</h2>
              <p class="mt-1 text-sm leading-6" :class="paletteDeleteActive ? 'text-rose-500' : 'text-slate-400'">
                {{ paletteDeleteActive ? '松开鼠标可删除当前拖拽组件' : '左侧拖入，中间排布，右侧配置。移动端可先点插入位，再点组件。' }}
              </p>
            </div>

            <div class="palette-tabs">
              <button
                v-for="tab in paletteTabs"
                :key="tab.key"
                type="button"
                class="palette-tab"
                :class="activePaletteTab === tab.key ? 'is-active' : ''"
                @click="activePaletteTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>
        </template>

        <div class="palette-scroll">
          <div v-if="activePaletteTab === 'controls'" class="space-y-5">
            <section v-for="group in controlPaletteGroups" :key="group.category" class="space-y-3">
              <div class="flex items-center justify-between gap-3">
                <h3 class="text-sm font-semibold text-slate-700">{{ group.category }}</h3>
                <span class="text-xs text-slate-400">{{ group.items.length }} 个</span>
              </div>

              <div class="palette-grid">
                <el-tooltip
                  v-for="item in group.items"
                  :key="item.key"
                  :content="item.description"
                  placement="top"
                  :disabled="!item.description"
                >
                  <button
                    type="button"
                    class="palette-item"
                    :class="item.disabled ? 'is-disabled' : ''"
                    :draggable="!item.disabled"
                    @click="handlePaletteClick(item)"
                    @dragstart="handlePaletteDragStart(item, $event)"
                    @dragend="clearDragState"
                  >
                    <div class="flex items-start justify-between gap-2">
                      <div class="min-w-0 text-left">
                        <p class="truncate text-sm font-semibold text-slate-800">{{ item.label }}</p>
                        <p class="palette-item-description mt-1 text-xs leading-5 text-slate-500">{{ item.description }}</p>
                      </div>
                      <el-tag size="small" effect="plain">{{ paletteItemTag(item) }}</el-tag>
                    </div>
                  </button>
                </el-tooltip>
              </div>
            </section>
          </div>

          <div v-else class="palette-grid">
            <el-tooltip
              v-for="item in paletteItems"
              :key="item.key"
              :content="item.description"
              placement="top"
              :disabled="!item.description"
            >
              <button
                type="button"
                class="palette-item"
                :class="item.disabled ? 'is-disabled' : ''"
                :draggable="!item.disabled"
                @click="handlePaletteClick(item)"
                @dragstart="handlePaletteDragStart(item, $event)"
                @dragend="clearDragState"
              >
                <div class="flex items-start justify-between gap-2">
                  <div class="min-w-0 text-left">
                    <p class="truncate text-sm font-semibold text-slate-800">{{ item.label }}</p>
                    <p class="palette-item-description mt-1 text-xs leading-5 text-slate-500">{{ item.description }}</p>
                  </div>
                  <el-tag size="small" effect="plain">{{ paletteItemTag(item) }}</el-tag>
                </div>
              </button>
            </el-tooltip>
          </div>

          <div v-if="activePaletteTab === 'controls' && controlPaletteGroups.length === 0" class="mt-2">
            <el-empty description="当前分类暂无组件" :image-size="72" />
          </div>
          <div v-else-if="activePaletteTab !== 'controls' && paletteItems.length === 0" class="mt-2">
            <el-empty description="当前分类暂无组件" :image-size="72" />
          </div>
        </div>
      </el-card>

      <el-card class="!rounded-3xl !shadow-sm" v-loading="loading">
        <div class="space-y-6">
          <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,1.1fr),minmax(0,0.9fr)]">
            <el-form-item :label="designerNameLabel" required class="!mb-0">
              <el-input
                v-model="working.formName"
                :maxlength="PM_NAME_MAX_LENGTH"
                show-word-limit
                :placeholder="`请输入${designerNameLabel}`"
              />
            </el-form-item>

            <el-form-item :label="designerCodeLabel" class="!mb-0">
              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-600">
                {{ working.formCode || '保存后自动生成' }}
              </div>
            </el-form-item>
          </div>

          <el-form-item v-if="isExpenseDetailDesigner" label="明细类型" class="!mb-0">
            <el-radio-group v-model="working.detailType">
              <el-radio-button label="NORMAL_REIMBURSEMENT">普通报销</el-radio-button>
              <el-radio-button label="ENTERPRISE_TRANSACTION">企业往来</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item :label="designerDescriptionLabel" class="!mb-0">
            <el-input v-model="working.formDescription" type="textarea" :rows="3" :placeholder="`请输入${designerDescriptionLabel}`" />
          </el-form-item>

          <div class="rounded-[32px] border border-slate-100 bg-slate-50/80 px-4 py-6 lg:px-8">
            <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-base font-semibold text-slate-800">表单画布</p>
                <p class="mt-1 text-sm text-slate-500">当前共 {{ working.schema.blocks.length }} 个组件，支持拖拽排序和点击插入。</p>
              </div>
              <div class="text-sm text-slate-500">
                <span v-if="insertCursorIndex !== null">准备插入到第 {{ insertCursorIndex + 1 }} 个位置</span>
                <span v-else-if="selectedBlock">当前选中：{{ selectedBlock.label }}</span>
                <span v-else>拖拽或点击左侧组件开始设计</span>
              </div>
            </div>

            <button
              type="button"
              class="drop-zone"
              :class="insertCursorIndex === 0 ? 'is-active' : ''"
              @click="setInsertCursor(0)"
              @dragover.prevent
              @drop.prevent="handleBoundaryDrop(0)"
            >
              {{ working.schema.blocks.length ? '点击或拖到这里，插入到最前面' : '拖拽组件到这里开始设计表单' }}
            </button>

            <div v-if="working.schema.blocks.length" class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
              <div
                v-for="(block, index) in working.schema.blocks"
                :key="block.blockId"
                class="space-y-3"
                :class="block.span === 2 ? 'md:col-span-2' : ''"
              >
                <div
                  class="form-block-shell"
                  :class="selectedBlockId === block.blockId ? 'is-selected' : ''"
                >
                  <div
                    class="form-block"
                    :class="[
                      selectedBlockId === block.blockId ? 'is-selected' : '',
                      dragHover?.blockId === block.blockId && dragHover?.position === 'before' ? 'drop-before' : '',
                      dragHover?.blockId === block.blockId && dragHover?.position === 'after' ? 'drop-after' : ''
                    ]"
                    draggable="true"
                    role="button"
                    tabindex="0"
                    @click="selectBlock(block.blockId)"
                    @keydown.enter.prevent="selectBlock(block.blockId)"
                    @dragstart="handleCanvasDragStart(block.blockId, index, $event)"
                    @dragend="clearDragState"
                    @dragover.prevent="handleBlockDragOver(block.blockId, index, $event)"
                    @drop.prevent="handleBlockDrop(block.blockId, index, $event)"
                  >
                    <div class="flex items-start justify-between gap-3">
                      <div class="min-w-0 text-left">
                        <div class="flex flex-wrap items-center gap-2">
                          <el-tag size="small" effect="plain">{{ blockKindLabel(block) }}</el-tag>
                          <el-tag size="small" effect="plain">{{ block.span === 2 ? '整宽' : '半宽' }}</el-tag>
                        </div>
                        <p class="mt-3 truncate text-base font-semibold text-slate-800">{{ block.label }}</p>
                        <p class="mt-1 text-xs text-slate-400">{{ block.fieldKey }}</p>
                        <p v-if="block.helpText" class="mt-2 text-sm leading-6 text-slate-500">{{ block.helpText }}</p>
                      </div>

                      <div class="flex shrink-0 flex-col items-end gap-2 text-xs text-slate-400">
                        <span>{{ block.required ? '必填' : '选填' }}</span>
                        <span>{{ blockPermissionSummary(block) }}</span>
                      </div>
                    </div>

                    <div class="mt-4 text-left">
                      <template v-if="block.kind === 'CONTROL'">
                        <div v-if="usesOptionEditor(block)" class="flex flex-wrap gap-2">
                          <span v-for="item in optionItems(block)" :key="item.value" class="preview-chip">{{ item.label }}</span>
                        </div>
                        <div v-else-if="controlType(block) === 'ATTACHMENT' || controlType(block) === 'IMAGE'" class="preview-note">
                          最多 {{ numberProp(block, 'maxCount', 0) }} 个文件，单文件 {{ numberProp(block, 'maxSizeMb', 0) }} MB
                        </div>
                        <div v-else-if="controlType(block) === 'SECTION'" class="preview-note">
                          {{ stringProp(block, 'content', '请在右侧填写说明内容') }}
                        </div>
                        <div v-else class="preview-note">{{ placeholderPreview(block) }}</div>
                      </template>

                      <template v-else-if="block.kind === 'BUSINESS_COMPONENT'">
                        <p class="preview-note">{{ businessDefinition(block)?.description || '业务组件预览' }}</p>
                        <div class="mt-3 flex flex-wrap gap-2">
                          <span v-for="field in businessDefinition(block)?.previewFields || []" :key="field" class="preview-chip">{{ field }}</span>
                        </div>
                      </template>

                      <template v-else>
                        <p class="preview-note">引用共享字段：{{ sharedArchiveName(block) }}</p>
                        <div v-if="sharedArchiveItems(block).length" class="mt-3 flex flex-wrap gap-2">
                          <span
                            v-for="item in sharedArchiveItems(block)"
                            :key="`${block.blockId}-${item.itemCode || item.itemName}`"
                            class="preview-chip"
                          >
                            {{ item.itemName }}
                          </span>
                        </div>
                        <p v-else-if="isSharedArchiveLoading(block)" class="mt-2 text-xs text-slate-400">正在加载共享字段内容...</p>
                        <p v-else-if="sharedArchiveItemCount(block)" class="mt-2 text-xs text-slate-400">当前档案选项数：{{ sharedArchiveItemCount(block) }}</p>
                        <p v-else class="mt-2 text-xs text-slate-400">当前档案暂无可展示条目</p>
                      </template>
                    </div>
                  </div>

                  <div class="form-block-action-slot">
                    <button
                      v-if="blockQuickActionStates[block.blockId] !== 'hidden'"
                      type="button"
                      class="block-expand-action"
                      :title="blockQuickActionStates[block.blockId] === 'expandable' ? '扩展为整宽' : '收起为半宽'"
                      @click.stop="toggleBlockWidth(block.blockId)"
                    >
                      {{ blockQuickActionStates[block.blockId] === 'expandable' ? '》' : '《' }}
                    </button>
                  </div>
                </div>

                <button
                  type="button"
                  class="inline-insert"
                  :class="insertCursorIndex === index + 1 ? 'is-active' : ''"
                  @click="setInsertCursor(index + 1)"
                  @dragover.prevent
                  @drop.prevent="handleBoundaryDrop(index + 1)"
                >
                  点击或拖到这里，插入到此卡片后面
                </button>
              </div>
            </div>

            <el-empty v-else class="canvas-empty" description="还没有任何组件，先从左侧拖一个进来" :image-size="92" />
          </div>
        </div>
      </el-card>

      <el-card class="!rounded-3xl !shadow-sm" v-loading="loading">
        <template #header>
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">{{ selectedBlock ? '组件配置' : '配置面板' }}</h2>
              <p class="mt-1 text-sm text-slate-400">{{ selectedBlock ? '这里用于配置字段属性与权限。' : '请先在中间选中一个组件。' }}</p>
            </div>
            <div v-if="selectedBlock" class="flex gap-2">
              <el-button plain :disabled="selectedBlockIndex <= 0" @click="moveSelectedBlock(-1)">上移</el-button>
              <el-button plain :disabled="selectedBlockIndex >= working.schema.blocks.length - 1" @click="moveSelectedBlock(1)">下移</el-button>
              <el-button type="danger" text :icon="Delete" @click="removeSelectedBlock">删除</el-button>
            </div>
          </div>
        </template>

        <div v-if="selectedBlock" class="designer-side-scroll">
          <el-tabs v-model="activeInspectorTab" stretch>
            <el-tab-pane label="字段属性" name="properties">
              <div class="space-y-6">
                <el-form-item label="显示字段" class="!mb-0"><el-input v-model="selectedBlock.label" placeholder="请输入显示字段" /></el-form-item>
                <el-form-item label="字段标识" class="!mb-0">
                  <el-input
                    v-model="selectedBlock.fieldKey"
                    :maxlength="PM_FIELD_KEY_MAX_LENGTH"
                    show-word-limit
                    placeholder="请输入字段标识"
                  />
                </el-form-item>
                <el-form-item label="说明文案" class="!mb-0"><el-input v-model="selectedBlock.helpText" type="textarea" :rows="3" placeholder="请输入字段说明" /></el-form-item>

                <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
                  <el-form-item label="宽度" class="!mb-0">
                    <el-radio-group v-model="selectedBlock.span" class="w-full">
                      <el-radio-button :label="1">半宽</el-radio-button>
                      <el-radio-button :label="2">整宽</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="是否必填" class="!mb-0">
                    <el-switch v-model="selectedBlock.required" inline-prompt active-text="必填" inactive-text="选填" />
                  </el-form-item>
                </div>

                <template v-if="selectedBlock.kind === 'CONTROL'">
                  <el-form-item v-if="usesPlaceholder(selectedBlock)" label="占位文案" class="!mb-0">
                    <el-input :model-value="stringProp(selectedBlock, 'placeholder')" placeholder="请输入占位文案" @update:model-value="setSelectedBlockProp('placeholder', $event)" />
                  </el-form-item>

                  <el-form-item v-if="usesDefaultValue(selectedBlock)" label="默认值" class="!mb-0">
                    <el-switch v-if="controlType(selectedBlock) === 'SWITCH'" :model-value="booleanDefaultValue(selectedBlock)" inline-prompt active-text="开" inactive-text="关" @update:model-value="setSelectedBlockDefaultValue" />
                    <el-input-number v-else-if="controlType(selectedBlock) === 'NUMBER' || controlType(selectedBlock) === 'AMOUNT'" :model-value="numericDefaultValue(selectedBlock)" :precision="controlType(selectedBlock) === 'AMOUNT' ? 2 : 0" :controls="false" class="w-full" @update:model-value="setSelectedBlockDefaultValue" />
                    <el-select v-else-if="controlType(selectedBlock) === 'SELECT' || controlType(selectedBlock) === 'RADIO'" :model-value="stringDefaultValue(selectedBlock)" clearable placeholder="请选择默认值" @update:model-value="setSelectedBlockDefaultValue">
                      <el-option v-for="item in optionItems(selectedBlock)" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                    <el-select v-else-if="controlType(selectedBlock) === 'MULTI_SELECT' || controlType(selectedBlock) === 'CHECKBOX'" :model-value="arrayDefaultValue(selectedBlock)" multiple clearable collapse-tags collapse-tags-tooltip placeholder="请选择默认值" @update:model-value="setSelectedBlockDefaultValue">
                      <el-option v-for="item in optionItems(selectedBlock)" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                    <el-input v-else :model-value="stringDefaultValue(selectedBlock)" placeholder="请输入默认值" @update:model-value="setSelectedBlockDefaultValue" />
                  </el-form-item>

                  <div v-if="usesOptionEditor(selectedBlock)" class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                    <div class="flex items-center justify-between gap-3">
                      <div>
                        <p class="text-base font-semibold text-slate-800">选项配置</p>
                        <p class="mt-1 text-sm text-slate-500">静态选项会直接保存到 schema。</p>
                      </div>
                      <el-button plain @click="addOptionItem">新增选项</el-button>
                    </div>
                    <div class="mt-4 space-y-3">
                      <div v-for="(item, index) in optionItems(selectedBlock)" :key="`${selectedBlock.blockId}-${index}`" class="grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 xl:grid-cols-[minmax(0,1fr),minmax(0,1fr),72px]">
                        <el-input v-model="item.label" placeholder="选项名称" />
                        <el-input v-model="item.value" placeholder="选项值" />
                        <el-button type="danger" text @click="removeOptionItem(index)">删除</el-button>
                      </div>
                    </div>
                  </div>

                  <div v-if="controlType(selectedBlock) === 'ATTACHMENT' || controlType(selectedBlock) === 'IMAGE'" class="grid grid-cols-1 gap-4 xl:grid-cols-2">
                    <el-form-item label="数量上限" class="!mb-0"><el-input-number :model-value="numberProp(selectedBlock, 'maxCount', 1)" :min="1" :controls="false" class="w-full" @update:model-value="setSelectedBlockProp('maxCount', $event ?? 1)" /></el-form-item>
                    <el-form-item label="单文件大小 (MB)" class="!mb-0"><el-input-number :model-value="numberProp(selectedBlock, 'maxSizeMb', 1)" :min="1" :controls="false" class="w-full" @update:model-value="setSelectedBlockProp('maxSizeMb', $event ?? 1)" /></el-form-item>
                    <el-form-item label="文件类型限制" class="xl:col-span-2 !mb-0"><el-input :model-value="stringProp(selectedBlock, 'accept')" placeholder=".jpg,.png,.pdf" @update:model-value="setSelectedBlockProp('accept', $event)" /></el-form-item>
                  </div>

                  <el-form-item v-if="controlType(selectedBlock) === 'SECTION'" label="说明内容" class="!mb-0">
                    <el-input :model-value="stringProp(selectedBlock, 'content')" type="textarea" :rows="4" placeholder="请输入说明内容" @update:model-value="setSelectedBlockProp('content', $event)" />
                  </el-form-item>
                </template>

                <template v-else-if="selectedBlock.kind === 'BUSINESS_COMPONENT'">
                  <div class="space-y-6">
                    <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                      <p class="text-base font-semibold text-slate-800">{{ businessDefinition(selectedBlock)?.label || '业务组件' }}</p>
                      <p class="mt-2 text-sm leading-6 text-slate-500">{{ businessDefinition(selectedBlock)?.description || '该业务组件由系统内置提供。' }}</p>
                      <div class="mt-4 flex flex-wrap gap-2">
                        <span v-for="field in businessDefinition(selectedBlock)?.previewFields || []" :key="field" class="preview-chip">{{ field }}</span>
                      </div>
                    </div>

                    <el-form-item v-if="businessCode(selectedBlock) === 'undertake-department'" label="默认部门" class="!mb-0">
                      <el-select
                        :model-value="undertakeDepartmentDefaultValue(selectedBlock)"
                        class="w-full"
                        placeholder="请选择默认部门"
                        @update:model-value="setUndertakeDepartmentDefaultValue"
                      >
                        <el-option label="不设置默认部门" :value="undertakeDeptNoneValue" />
                        <el-option label="提单人所在部门" :value="undertakeDeptSpecialValue" />
                        <el-option
                          v-for="item in departmentOptions"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>

                    <el-form-item
                      v-if="supportsDocumentTemplateTypeConfig(selectedBlock)"
                      :label="documentTemplateTypeLabel(selectedBlock)"
                      class="!mb-0"
                    >
                      <el-checkbox-group
                        :model-value="allowedTemplateTypesValue(selectedBlock)"
                        class="flex flex-wrap gap-3"
                        @update:model-value="setDocumentTemplateTypes"
                      >
                        <el-checkbox
                          v-for="item in documentTemplateTypeOptions(selectedBlock)"
                          :key="`${selectedBlock.blockId}-${item.value}`"
                          :label="item.value"
                        >
                          {{ item.label }}
                        </el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>

                    <p
                      v-if="businessCode(selectedBlock) === 'undertake-department'"
                      class="rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-xs leading-6 text-blue-600"
                    >
                      选择“提单人所在部门”后，提单页会自动带出当前登录人的真实所属部门。
                    </p>

                    <p
                      v-if="businessCode(selectedBlock) === 'related-document'"
                      class="rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-xs leading-6 text-emerald-700"
                    >
                      提单页会按所选单据类型分组弹出选择窗口，支持同时关联多张已审批通过的单据。
                    </p>

                    <p
                      v-if="businessCode(selectedBlock) === 'writeoff-document'"
                      class="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs leading-6 text-amber-700"
                    >
                      核销单据固定只允许报销单与借款单，提单页选择后需逐条填写本次核销金额。
                    </p>
                  </div>
                </template>

                <template v-else>
                  <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                    <p class="text-base font-semibold text-slate-800">{{ sharedArchiveName(selectedBlock) }}</p>
                    <p class="mt-2 text-sm leading-6 text-slate-500">共享字段采用引用关系保存，schema 中只保留档案编码。</p>
                    <p class="mt-3 text-xs text-slate-400">档案编码：{{ getSharedArchiveCode(selectedBlock) }}</p>
                    <div v-if="sharedArchiveItems(selectedBlock).length" class="mt-4 flex flex-wrap gap-2">
                      <span
                        v-for="item in sharedArchiveItems(selectedBlock)"
                        :key="`${selectedBlock.blockId}-${item.itemCode || item.itemName}`"
                        class="preview-chip"
                      >
                        {{ item.itemName }}
                      </span>
                    </div>
                    <p v-else-if="isSharedArchiveLoading(selectedBlock)" class="mt-4 text-xs text-slate-400">正在加载共享字段内容...</p>
                    <p v-else-if="sharedArchiveLoadFailed(selectedBlock)" class="mt-4 text-xs text-amber-500">共享档案详情加载失败，当前仅显示摘要信息。</p>
                    <p v-else class="mt-4 text-xs text-slate-400">当前档案暂无可展示条目</p>
                  </div>
                </template>
              </div>
            </el-tab-pane>

            <el-tab-pane label="字段权限" name="permissions">
              <div class="space-y-6">
                <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                  <p class="text-base font-semibold text-slate-800">固定阶段权限</p>
                  <div class="mt-4 space-y-4">
                    <div v-for="stage in permissionStages" :key="stage.key" class="grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 xl:grid-cols-[minmax(0,1fr),160px]">
                      <div class="text-sm font-medium text-slate-700">{{ stage.label }}</div>
                      <el-select v-model="selectedBlock.permission.fixedStages[stage.key]">
                        <el-option v-for="item in permissionValueOptions" :key="item.value" :label="item.label" :value="item.value" />
                      </el-select>
                    </div>
                  </div>
                </div>

                <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <p class="text-base font-semibold text-slate-800">流程场景覆盖</p>
                      <p class="mt-1 text-sm text-slate-500">命中场景时，这里的权限会覆盖固定阶段权限。</p>
                    </div>
                    <el-button plain :disabled="!canAddSceneOverride" @click="addSceneOverride">新增场景覆盖</el-button>
                  </div>

                  <div v-if="selectedBlock.permission.sceneOverrides.length" class="mt-4 space-y-3">
                    <div v-for="(override, index) in selectedBlock.permission.sceneOverrides" :key="`${selectedBlock.blockId}-${index}`" class="grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 xl:grid-cols-[minmax(0,1fr),160px,72px]">
                      <el-select v-model="override.sceneId">
                        <el-option v-for="item in sceneOptionsFor(index)" :key="item.id" :label="item.sceneName" :value="item.id" />
                      </el-select>
                      <el-select v-model="override.permission">
                        <el-option v-for="item in permissionValueOptions" :key="item.value" :label="item.label" :value="item.value" />
                      </el-select>
                      <el-button type="danger" text @click="removeSceneOverride(index)">删除</el-button>
                    </div>
                  </div>

                  <el-empty v-else class="mt-2" description="当前还没有场景覆盖" :image-size="72" />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <el-empty v-else description="请先在中间画布选中一个组件" :image-size="92" />
      </el-card>
    </div>

    <div
      class="process-form-designer-floating-bar sticky bottom-4 z-10"
      data-testid="process-form-designer-floating-bar"
    >
      <div
        class="process-form-designer-floating-bar__inner"
        data-testid="process-form-designer-floating-bar-inner"
      >
        <el-button
          class="process-form-designer-floating-bar__button"
          type="primary"
          :icon="Check"
          :loading="savingDraft"
          :disabled="!canEdit || isSaving"
          @click="saveFormDesign('draft')"
        >
          保存草稿
        </el-button>
        <el-button
          class="process-form-designer-floating-bar__button process-form-designer-floating-bar__button--success"
          type="success"
          :icon="Check"
          :loading="savingFinal"
          :disabled="!canEdit || isSaving"
          @click="saveFormDesign('final')"
        >
          {{ designerSaveLabel }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Delete, RefreshRight } from '@element-plus/icons-vue'
import {
  processApi,
  type ProcessCustomArchiveDetail,
  type ProcessCustomArchiveSummary,
  type ProcessExpenseDetailDesignDetail,
  type ProcessExpenseDetailDesignSavePayload,
  type ProcessFlowScene,
  type ProcessFormDesignBlock,
  type ProcessFormDesignDetail,
  type ProcessFormDesignSchema,
  type ProcessFormDesignSavePayload,
  type ProcessFormOption
} from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import {
  CONTROL_PALETTE_ITEMS,
  CONTROL_PALETTE_CATEGORIES,
  DOCUMENT_TEMPLATE_TYPE_OPTIONS,
  FORM_PERMISSION_STAGE_OPTIONS,
  FORM_PERMISSION_VALUE_OPTIONS,
  RELATED_DOCUMENT_ALLOWED_TEMPLATE_TYPES,
  WRITEOFF_DOCUMENT_ALLOWED_TEMPLATE_TYPES,
  type FormDesignerPaletteItem,
  buildBusinessComponentPaletteItems,
  buildSharedFieldPaletteItems,
  createDefaultNewFormSchema,
  createEmptyFormSchema,
  getBlockQuickActionStates,
  getBusinessComponentDefinition,
  getControlType,
  getOptionItems,
  getSharedArchiveCode,
  insertBlockAt,
  moveBlock,
  moveBlockByOffset,
  normalizeBusinessComponentAllowedTemplateTypes,
  normalizeFormSchema,
  removeBlock
} from '@/views/process/formDesignerHelper'
import { PM_FIELD_KEY_MAX_LENGTH, PM_NAME_MAX_LENGTH, validateMaxLength, validateSchemaFieldKeys } from '@/views/process/pmValidation'

type PaletteTabKey = 'controls' | 'business' | 'shared'
type DragSource = { type: 'palette'; key: string } | { type: 'canvas'; blockId: string; index: number }
type DesignerDetailState = {
  id: number
  formCode: string
  formName: string
  templateType: string
  templateTypeLabel: string
  formDescription: string
  updatedAt: string
  schema: ProcessFormDesignSchema
  detailType: string
  detailTypeLabel: string
}

const route = useRoute()
const router = useRouter()
const initialFormId = Number(route.params.id)
const initialIsCreateMode = !Number.isFinite(initialFormId) || initialFormId <= 0
const isExpenseDetailDesigner = computed(() => String(route.name || '').includes('expense-detail'))
const loading = ref(false)
const savingMode = ref<'draft' | 'final' | null>(null)
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const activePaletteTab = ref<PaletteTabKey>('controls')
const activeInspectorTab = ref<'properties' | 'permissions'>('properties')
const selectedBlockId = ref('')
const insertCursorIndex = ref<number | null>(null)
const dragSource = ref<DragSource | null>(null)
const dragHover = ref<{ blockId: string; position: 'before' | 'after' } | null>(null)
const paletteDeleteActive = ref(false)
const sceneOptions = ref<ProcessFlowScene[]>([])
const departmentOptions = ref<ProcessFormOption[]>([])
const customArchives = ref<ProcessCustomArchiveSummary[]>([])
const customArchiveDetailMap = ref<Record<string, ProcessCustomArchiveDetail>>({})
const customArchiveDetailLoadingCodes = ref<string[]>([])
const customArchiveDetailErrorCodes = ref<string[]>([])
const working = reactive<DesignerDetailState>(createEmptyDetail(resolveRouteTemplateType(), initialIsCreateMode))
const undertakeDeptSpecialValue = '__SUBMITTER_DEPARTMENT__'
const undertakeDeptNoneValue = '__NONE__'

const paletteTabs = [
  { key: 'controls', label: '控件' },
  { key: 'business', label: '业务组件' },
  { key: 'shared', label: '共享字段' }
] as const

const formId = computed(() => {
  const raw = Number(route.params.id)
  return Number.isFinite(raw) && raw > 0 ? raw : null
})
const copyFromId = computed(() => {
  if (!isExpenseDetailDesigner.value || formId.value !== null) {
    return null
  }
  const raw = Number(route.query.copyFromId)
  return Number.isFinite(raw) && raw > 0 ? raw : null
})
const isCreateMode = computed(() => formId.value === null)
const canEdit = computed(() => isCreateMode.value ? hasPermission('expense:process_management:create', permissionCodes.value) : hasPermission('expense:process_management:edit', permissionCodes.value))
const templateTypeLabel = computed(() => isExpenseDetailDesigner.value ? (working.detailTypeLabel || resolveExpenseDetailTypeLabel(working.detailType)) : (working.templateTypeLabel || resolveTemplateTypeLabel(working.templateType)))
const designerNameLabel = computed(() => isExpenseDetailDesigner.value ? '明细表单名称' : '表单名称')
const designerCodeLabel = computed(() => isExpenseDetailDesigner.value ? '明细表单编码' : '表单编码')
const designerDescriptionLabel = computed(() => isExpenseDetailDesigner.value ? '明细表单说明' : '表单说明')
const designerSaveLabel = computed(() => isExpenseDetailDesigner.value ? '保存费用明细表单' : '保存表单设计')
const designerDraftSaveSuccessText = computed(() => isExpenseDetailDesigner.value ? '费用明细表单草稿已保存' : '表单草稿已保存')
const designerSaveSuccessText = computed(() => formId.value !== null ? (isExpenseDetailDesigner.value ? '费用明细表单已更新' : '表单设计已更新') : (isExpenseDetailDesigner.value ? '费用明细表单已创建' : '表单设计已创建'))
const designerLoadErrorText = computed(() => isExpenseDetailDesigner.value ? '加载费用明细表单失败' : '加载表单设计失败')
const isSaving = computed(() => savingMode.value !== null)
const savingDraft = computed(() => savingMode.value === 'draft')
const savingFinal = computed(() => savingMode.value === 'final')
const businessPaletteItems = computed(() => buildBusinessComponentPaletteItems())
const sharedArchiveOptions = computed(() => customArchives.value.filter((item) => item.status === 1 && item.archiveType === 'SELECT'))
const sharedPaletteItems = computed(() => buildSharedFieldPaletteItems(sharedArchiveOptions.value))
const paletteItems = computed<FormDesignerPaletteItem[]>(() => activePaletteTab.value === 'business' ? businessPaletteItems.value : activePaletteTab.value === 'shared' ? sharedPaletteItems.value : CONTROL_PALETTE_ITEMS)
const controlPaletteGroups = computed(() =>
  CONTROL_PALETTE_CATEGORIES
    .map((category) => ({
      category,
      items: CONTROL_PALETTE_ITEMS.filter((item) => item.category === category)
    }))
    .filter((group) => group.items.length > 0)
)
const selectedBlock = computed(() => working.schema.blocks.find((item) => item.blockId === selectedBlockId.value) || null)
const selectedBlockIndex = computed(() => working.schema.blocks.findIndex((item) => item.blockId === selectedBlockId.value))
const blockQuickActionStates = computed(() => getBlockQuickActionStates(working.schema))
const permissionStages = FORM_PERMISSION_STAGE_OPTIONS
const permissionValueOptions = FORM_PERMISSION_VALUE_OPTIONS
const canAddSceneOverride = computed(() => Boolean(selectedBlock.value) && sceneOptions.value.some((scene) => !selectedBlock.value?.permission.sceneOverrides.some((override) => override.sceneId === scene.id)))
const sharedArchiveMap = computed(() => new Map(sharedArchiveOptions.value.map((item) => [item.archiveCode, item])))

watch(() => [route.params.id, route.query.templateType, route.query.copyFromId], () => { void loadPage() }, { immediate: true })
watch(activePaletteTab, (nextTab) => {
  if (nextTab === 'shared') {
    void ensureSharedArchiveDetailsByCodes(sharedArchiveOptions.value.map((item) => item.archiveCode))
  }
})
watch(selectedBlock, (block) => {
  if (block?.kind === 'SHARED_FIELD') {
    void ensureSharedArchiveDetailsByCodes([getSharedArchiveCode(block)])
  }
})
watch(() => working.detailType, (detailType) => {
  if (isExpenseDetailDesigner.value) {
    working.detailTypeLabel = resolveExpenseDetailTypeLabel(detailType || 'NORMAL_REIMBURSEMENT')
  }
})

onMounted(() => {
  window.addEventListener('keydown', handleWindowKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleWindowKeydown)
})

function createEmptyDetail(templateType: string, useDefaultBlocks = false): DesignerDetailState {
  return {
    id: 0,
    formCode: '',
    formName: '',
    templateType,
    templateTypeLabel: resolveTemplateTypeLabel(templateType),
    formDescription: '',
    updatedAt: '',
    schema: !isExpenseDetailDesigner.value && useDefaultBlocks ? createDefaultNewFormSchema() : createEmptyFormSchema(),
    detailType: 'NORMAL_REIMBURSEMENT',
    detailTypeLabel: resolveExpenseDetailTypeLabel('NORMAL_REIMBURSEMENT')
  }
}

function resolveRouteTemplateType() {
  const raw = typeof route.query.templateType === 'string' ? route.query.templateType : ''
  return raw === 'application' || raw === 'loan' || raw === 'contract' ? raw : 'report'
}

function resolveTemplateTypeLabel(templateType: string) {
  if (templateType === 'application') return '申请单'
  if (templateType === 'loan') return '借款单'
  if (templateType === 'contract') return '合同单'
  return '报销单'
}

function resolveExpenseDetailTypeLabel(detailType: string) {
  return detailType === 'ENTERPRISE_TRANSACTION' ? '企业往来' : '普通报销'
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function assignDetail(detail: ProcessFormDesignDetail | ProcessExpenseDetailDesignDetail) {
  if (isExpenseDetailDesigner.value) {
    const expenseDetail = detail as ProcessExpenseDetailDesignDetail
    Object.assign(working, {
      id: expenseDetail.id || 0,
      formCode: expenseDetail.detailCode || '',
      formName: expenseDetail.detailName || '',
      templateType: 'report',
      templateTypeLabel: resolveTemplateTypeLabel('report'),
      formDescription: expenseDetail.detailDescription || '',
      updatedAt: expenseDetail.updatedAt || '',
      schema: normalizeFormSchema(expenseDetail.schema),
      detailType: expenseDetail.detailType || 'NORMAL_REIMBURSEMENT',
      detailTypeLabel: expenseDetail.detailTypeLabel || resolveExpenseDetailTypeLabel(expenseDetail.detailType || 'NORMAL_REIMBURSEMENT')
    })
    return
  }

  const formDetail = detail as ProcessFormDesignDetail
  Object.assign(working, {
    ...formDetail,
    templateType: formDetail.templateType || resolveRouteTemplateType(),
    templateTypeLabel: formDetail.templateTypeLabel || resolveTemplateTypeLabel(formDetail.templateType || resolveRouteTemplateType()),
    schema: normalizeFormSchema(formDetail.schema),
    detailType: '',
    detailTypeLabel: ''
  })
}

function assignCopiedExpenseDetail(detail: ProcessExpenseDetailDesignDetail) {
  const sourceName = (detail.detailName || '').trim()
  Object.assign(working, {
    ...createEmptyDetail('report'),
    formName: sourceName ? `${sourceName}-副本` : '费用明细表单-副本',
    formDescription: detail.detailDescription || '',
    schema: normalizeFormSchema(detail.schema),
    detailType: detail.detailType || 'NORMAL_REIMBURSEMENT',
    detailTypeLabel: detail.detailTypeLabel || resolveExpenseDetailTypeLabel(detail.detailType || 'NORMAL_REIMBURSEMENT')
  })
}

function referencedSharedArchiveCodes() {
  return Array.from(new Set(
    working.schema.blocks
      .filter((item) => item.kind === 'SHARED_FIELD')
      .map((item) => getSharedArchiveCode(item))
      .filter(Boolean)
  ))
}

function addLoadingArchiveCode(archiveCode: string) {
  if (!customArchiveDetailLoadingCodes.value.includes(archiveCode)) {
    customArchiveDetailLoadingCodes.value = [...customArchiveDetailLoadingCodes.value, archiveCode]
  }
}

function removeLoadingArchiveCode(archiveCode: string) {
  customArchiveDetailLoadingCodes.value = customArchiveDetailLoadingCodes.value.filter((item) => item !== archiveCode)
}

function markArchiveLoadFailed(archiveCode: string) {
  if (!customArchiveDetailErrorCodes.value.includes(archiveCode)) {
    customArchiveDetailErrorCodes.value = [...customArchiveDetailErrorCodes.value, archiveCode]
  }
}

function clearArchiveLoadFailed(archiveCode: string) {
  customArchiveDetailErrorCodes.value = customArchiveDetailErrorCodes.value.filter((item) => item !== archiveCode)
}

async function ensureSharedArchiveDetailsByCodes(archiveCodes: string[]) {
  const uniqueCodes = Array.from(new Set(archiveCodes.filter(Boolean)))
  await Promise.all(uniqueCodes.map(async (archiveCode) => {
    if (customArchiveDetailMap.value[archiveCode] || customArchiveDetailLoadingCodes.value.includes(archiveCode)) {
      return
    }
    const summary = sharedArchiveMap.value.get(archiveCode)
    if (!summary?.id) {
      return
    }
    addLoadingArchiveCode(archiveCode)
    try {
      const res = await processApi.getCustomArchiveDetail(summary.id)
      customArchiveDetailMap.value = {
        ...customArchiveDetailMap.value,
        [archiveCode]: res.data
      }
      clearArchiveLoadFailed(archiveCode)
    } catch {
      markArchiveLoadFailed(archiveCode)
    } finally {
      removeLoadingArchiveCode(archiveCode)
    }
  }))
}

async function loadPage() {
  loading.value = true
  try {
    const [flowMetaRes, archiveRes] = await Promise.all([processApi.getFlowMeta(), processApi.listCustomArchives()])
    sceneOptions.value = flowMetaRes.data.sceneOptions || []
    departmentOptions.value = flowMetaRes.data.departmentOptions || []
    customArchives.value = archiveRes.data || []
    customArchiveDetailMap.value = {}
    customArchiveDetailLoadingCodes.value = []
    customArchiveDetailErrorCodes.value = []
    assignDetail(createEmptyDetail(resolveRouteTemplateType(), formId.value === null))
    if (formId.value !== null) {
      const detailRes = await (isExpenseDetailDesigner.value ? processApi.getExpenseDetailDesignDetail(formId.value) : processApi.getFormDesignDetail(formId.value))
      assignDetail(detailRes.data)
    } else if (copyFromId.value !== null) {
      try {
        const detailRes = await processApi.getExpenseDetailDesignDetail(copyFromId.value)
        assignCopiedExpenseDetail(detailRes.data)
      } catch (error: unknown) {
        ElMessage.error(resolveErrorMessage(error, '复制源费用明细表单加载失败，已为你打开空白新建页'))
      }
    }
    await ensureSharedArchiveDetailsByCodes(referencedSharedArchiveCodes())
    selectedBlockId.value = working.schema.blocks[0]?.blockId || ''
    insertCursorIndex.value = null
    dragHover.value = null
    paletteDeleteActive.value = false
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, designerLoadErrorText.value))
  } finally {
    loading.value = false
  }
}

function selectBlock(blockId: string) {
  selectedBlockId.value = blockId
  insertCursorIndex.value = null
}

function setInsertCursor(index: number) {
  insertCursorIndex.value = index
  selectedBlockId.value = ''
}

function paletteItemTag(item: FormDesignerPaletteItem) {
  return item.kind === 'BUSINESS_COMPONENT' ? '业务' : item.kind === 'SHARED_FIELD' ? '共享' : '控件'
}

function clearDragState() {
  dragSource.value = null
  dragHover.value = null
  paletteDeleteActive.value = false
}

function handlePaletteDragStart(item: FormDesignerPaletteItem, event: DragEvent) {
  if (item.disabled) {
    event.preventDefault()
    return
  }
  dragSource.value = { type: 'palette', key: item.key }
  event.dataTransfer?.setData('text/plain', item.key)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copy'
}

function handleCanvasDragStart(blockId: string, index: number, event: DragEvent) {
  dragSource.value = { type: 'canvas', blockId, index }
  event.dataTransfer?.setData('text/plain', blockId)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function handlePaletteDeleteDragOver(event: DragEvent) {
  if (dragSource.value?.type !== 'canvas') {
    return
  }
  event.preventDefault()
  paletteDeleteActive.value = true
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function handlePaletteDeleteDragLeave(event: DragEvent) {
  if (dragSource.value?.type !== 'canvas') {
    return
  }
  const currentTarget = event.currentTarget as HTMLElement | null
  const relatedTarget = event.relatedTarget
  if (currentTarget && relatedTarget instanceof Node && currentTarget.contains(relatedTarget)) {
    return
  }
  paletteDeleteActive.value = false
}

function resolvePaletteItemByKey(key: string) {
  return [...CONTROL_PALETTE_ITEMS, ...businessPaletteItems.value, ...sharedPaletteItems.value].find((item) => item.key === key) || null
}

function resolveInsertIndex() {
  if (insertCursorIndex.value !== null) return insertCursorIndex.value
  if (selectedBlockIndex.value >= 0) return selectedBlockIndex.value + 1
  return working.schema.blocks.length
}

function handlePaletteClick(item: FormDesignerPaletteItem) {
  if (item.disabled) {
    ElMessage.info('该组件将在后续版本开放')
    return
  }
  const insertIndex = resolveInsertIndex()
  working.schema = insertBlockAt(working.schema, item, insertIndex)
  selectedBlockId.value = working.schema.blocks[insertIndex]?.blockId || ''
  insertCursorIndex.value = null
  if (item.kind === 'SHARED_FIELD') {
    void ensureSharedArchiveDetailsByCodes([String(item.props.archiveCode || '')])
  }
}

async function handlePaletteDeleteDrop(event: DragEvent) {
  if (dragSource.value?.type !== 'canvas') {
    return
  }
  event.preventDefault()
  const blockId = dragSource.value.blockId
  clearDragState()
  await confirmRemoveBlock(blockId)
}

function handleBoundaryDrop(index: number) {
  const source = dragSource.value
  if (!source) return
  if (source.type === 'palette') {
    const item = resolvePaletteItemByKey(source.key)
    if (item && !item.disabled) {
      working.schema = insertBlockAt(working.schema, item, index)
      selectedBlockId.value = working.schema.blocks[index]?.blockId || ''
      if (item.kind === 'SHARED_FIELD') {
        void ensureSharedArchiveDetailsByCodes([String(item.props.archiveCode || '')])
      }
    }
  } else {
    const targetIndex = source.index < index ? index - 1 : index
    working.schema = moveBlock(working.schema, source.index, targetIndex)
    selectedBlockId.value = source.blockId
  }
  insertCursorIndex.value = null
  clearDragState()
}

function handleBlockDragOver(blockId: string, index: number, event: DragEvent) {
  const position = resolveDropPosition(event)
  dragHover.value = { blockId, position }
  insertCursorIndex.value = position === 'before' ? index : index + 1
  if (event.dataTransfer) event.dataTransfer.dropEffect = dragSource.value?.type === 'canvas' ? 'move' : 'copy'
}

function handleBlockDrop(blockId: string, index: number, event: DragEvent) {
  const source = dragSource.value
  if (!source) return
  const insertIndex = resolveDropPosition(event) === 'before' ? index : index + 1
  if (source.type === 'palette') {
    const item = resolvePaletteItemByKey(source.key)
    if (item && !item.disabled) {
      working.schema = insertBlockAt(working.schema, item, insertIndex)
      selectedBlockId.value = working.schema.blocks[insertIndex]?.blockId || ''
      if (item.kind === 'SHARED_FIELD') {
        void ensureSharedArchiveDetailsByCodes([String(item.props.archiveCode || '')])
      }
    }
  } else {
    working.schema = moveBlock(working.schema, source.index, source.index < insertIndex ? insertIndex - 1 : insertIndex)
    selectedBlockId.value = source.blockId
  }
  insertCursorIndex.value = null
  clearDragState()
}

function resolveDropPosition(event: DragEvent) {
  const target = event.currentTarget as HTMLElement | null
  if (!target) return 'after'
  const rect = target.getBoundingClientRect()
  return event.clientY < rect.top + rect.height / 2 ? 'before' : 'after'
}

async function confirmRemoveBlock(blockId: string) {
  const block = working.schema.blocks.find((item) => item.blockId === blockId)
  if (!block) {
    return false
  }
  try {
    await ElMessageBox.confirm('删除该组件后，当前字段配置会一起移除。确定继续吗？', '删除组件', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return false
  }
  const currentIndex = working.schema.blocks.findIndex((item) => item.blockId === blockId)
  working.schema = removeBlock(working.schema, blockId)
  const nextBlock = working.schema.blocks[currentIndex] || working.schema.blocks[currentIndex - 1]
  selectedBlockId.value = nextBlock?.blockId || ''
  return true
}

async function removeSelectedBlock() {
  if (!selectedBlock.value) return
  await confirmRemoveBlock(selectedBlock.value.blockId)
}

function shouldIgnoreDeleteShortcut(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) {
    return false
  }
  if (target.isContentEditable) {
    return true
  }
  const tagName = target.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT', 'OPTION'].includes(tagName)) {
    return true
  }
  return Boolean(target.closest('input, textarea, select, [contenteditable=\"true\"], .el-input, .el-textarea, .el-select, .el-input-number'))
}

function handleWindowKeydown(event: KeyboardEvent) {
  if (event.key !== 'Delete' || !selectedBlock.value || shouldIgnoreDeleteShortcut(event.target)) {
    return
  }
  event.preventDefault()
  void confirmRemoveBlock(selectedBlock.value.blockId)
}

function moveSelectedBlock(offset: number) {
  if (!selectedBlock.value) return
  working.schema = moveBlockByOffset(working.schema, selectedBlock.value.blockId, offset)
}

function toggleBlockWidth(blockId: string) {
  const block = working.schema.blocks.find((item) => item.blockId === blockId)
  const nextState = blockQuickActionStates.value[blockId]
  if (!block || !nextState || nextState === 'hidden') {
    return
  }
  block.span = nextState === 'expandable' ? 2 : 1
  selectedBlockId.value = blockId
}

function controlType(block: ProcessFormDesignBlock) {
  return getControlType(block)
}

function businessDefinition(block: ProcessFormDesignBlock) {
  return getBusinessComponentDefinition(String(block.props.componentCode || ''))
}

function businessCode(block: ProcessFormDesignBlock) {
  return businessDefinition(block)?.code || String(block.props.componentCode || '')
}

function undertakeDepartmentDefaultValue(block: ProcessFormDesignBlock) {
  const mode = stringProp(block, 'defaultDeptMode', 'NONE')
  if (mode === 'SUBMITTER_DEPARTMENT') {
    return undertakeDeptSpecialValue
  }
  if (mode === 'FIXED_DEPARTMENT') {
    return stringProp(block, 'defaultDeptId') || undertakeDeptNoneValue
  }
  return undertakeDeptNoneValue
}

function setUndertakeDepartmentDefaultValue(value: string | number | boolean) {
  if (!selectedBlock.value || businessCode(selectedBlock.value) !== 'undertake-department') {
    return
  }
  const nextValue = String(value || undertakeDeptNoneValue)
  if (nextValue === undertakeDeptSpecialValue) {
    setSelectedBlockProp('defaultDeptMode', 'SUBMITTER_DEPARTMENT')
    setSelectedBlockProp('defaultDeptId', '')
    return
  }
  if (nextValue === undertakeDeptNoneValue) {
    setSelectedBlockProp('defaultDeptMode', 'NONE')
    setSelectedBlockProp('defaultDeptId', '')
    return
  }
  setSelectedBlockProp('defaultDeptMode', 'FIXED_DEPARTMENT')
  setSelectedBlockProp('defaultDeptId', nextValue)
}

function supportsDocumentTemplateTypeConfig(block: ProcessFormDesignBlock) {
  const code = businessCode(block)
  return code === 'related-document' || code === 'writeoff-document'
}

function documentTemplateTypeOptions(block: ProcessFormDesignBlock) {
  const code = businessCode(block)
  const allowedValues: Set<string> = code === 'writeoff-document'
    ? new Set(WRITEOFF_DOCUMENT_ALLOWED_TEMPLATE_TYPES)
    : new Set(RELATED_DOCUMENT_ALLOWED_TEMPLATE_TYPES)
  return DOCUMENT_TEMPLATE_TYPE_OPTIONS.filter((item) => allowedValues.has(item.value))
}

function allowedTemplateTypesValue(block: ProcessFormDesignBlock) {
  return normalizeBusinessComponentAllowedTemplateTypes(
    businessCode(block),
    block.props.allowedTemplateTypes
  )
}

function documentTemplateTypeLabel(block: ProcessFormDesignBlock) {
  return businessCode(block) === 'writeoff-document' ? '允许核销单据类型' : '允许关联单据类型'
}

function setDocumentTemplateTypes(value: string[] | number[] | boolean[]) {
  if (!selectedBlock.value || !supportsDocumentTemplateTypeConfig(selectedBlock.value)) {
    return
  }
  const normalized = normalizeBusinessComponentAllowedTemplateTypes(
    businessCode(selectedBlock.value),
    Array.isArray(value) ? value.map((item) => String(item)) : []
  )
  setSelectedBlockProp('allowedTemplateTypes', normalized)
}

function sharedArchiveName(block: ProcessFormDesignBlock) {
  return sharedArchiveMap.value.get(getSharedArchiveCode(block))?.archiveName || '未匹配到共享档案'
}

function sharedArchiveItemCount(block: ProcessFormDesignBlock) {
  return sharedArchiveMap.value.get(getSharedArchiveCode(block))?.itemCount || 0
}

function sharedArchiveItems(block: ProcessFormDesignBlock) {
  const archiveCode = getSharedArchiveCode(block)
  return customArchiveDetailMap.value[archiveCode]?.items || []
}

function isSharedArchiveLoading(block: ProcessFormDesignBlock) {
  return customArchiveDetailLoadingCodes.value.includes(getSharedArchiveCode(block))
}

function sharedArchiveLoadFailed(block: ProcessFormDesignBlock) {
  return customArchiveDetailErrorCodes.value.includes(getSharedArchiveCode(block))
}

function optionItems(block: ProcessFormDesignBlock) {
  return getOptionItems(block)
}

function usesOptionEditor(block: ProcessFormDesignBlock) {
  return ['SELECT', 'MULTI_SELECT', 'RADIO', 'CHECKBOX'].includes(controlType(block))
}

function usesPlaceholder(block: ProcessFormDesignBlock) {
  return !['DATE', 'DATE_RANGE', 'SWITCH', 'ATTACHMENT', 'IMAGE', 'SECTION'].includes(controlType(block))
}

function usesDefaultValue(block: ProcessFormDesignBlock) {
  return !['ATTACHMENT', 'IMAGE', 'SECTION'].includes(controlType(block)) && block.kind !== 'SHARED_FIELD'
}

function addOptionItem() {
  if (!selectedBlock.value) return
  const options = optionItems(selectedBlock.value)
  selectedBlock.value.props.options = options
  options.push({ label: `选项 ${options.length + 1}`, value: `option-${Date.now()}` })
}

function removeOptionItem(index: number) {
  if (!selectedBlock.value || !Array.isArray(selectedBlock.value.props.options)) return
  ;(selectedBlock.value.props.options as Array<unknown>).splice(index, 1)
}

function addSceneOverride() {
  if (!selectedBlock.value) return
  const used = new Set(selectedBlock.value.permission.sceneOverrides.map((item) => item.sceneId))
  const nextScene = sceneOptions.value.find((item) => !used.has(item.id))
  if (!nextScene) return
  selectedBlock.value.permission.sceneOverrides.push({ sceneId: nextScene.id, permission: 'READONLY' })
}

function removeSceneOverride(index: number) {
  if (!selectedBlock.value) return
  selectedBlock.value.permission.sceneOverrides.splice(index, 1)
}

function sceneOptionsFor(currentIndex: number) {
  if (!selectedBlock.value) return sceneOptions.value
  const currentSceneId = selectedBlock.value.permission.sceneOverrides[currentIndex]?.sceneId
  return sceneOptions.value.filter((item) => item.id === currentSceneId || !selectedBlock.value?.permission.sceneOverrides.some((override, index) => index !== currentIndex && override.sceneId === item.id))
}

function stringProp(block: ProcessFormDesignBlock, key: string, fallback = '') {
  const value = block.props[key]
  return typeof value === 'string' ? value : fallback
}

function numberProp(block: ProcessFormDesignBlock, key: string, fallback = 0) {
  const value = block.props[key]
  if (typeof value === 'number' && Number.isFinite(value)) return value
  const next = Number(value)
  return Number.isFinite(next) ? next : fallback
}

function setSelectedBlockProp(key: string, value: unknown) {
  if (!selectedBlock.value) return
  selectedBlock.value.props[key] = value
}

function booleanDefaultValue(block: ProcessFormDesignBlock) {
  return Boolean(block.defaultValue)
}

function numericDefaultValue(block: ProcessFormDesignBlock) {
  if (typeof block.defaultValue === 'number' && Number.isFinite(block.defaultValue)) return block.defaultValue
  if (block.defaultValue === undefined || block.defaultValue === null || block.defaultValue === '') return undefined
  const next = Number(block.defaultValue)
  return Number.isFinite(next) ? next : undefined
}

function stringDefaultValue(block: ProcessFormDesignBlock) {
  return block.defaultValue === undefined || block.defaultValue === null ? '' : String(block.defaultValue)
}

function arrayDefaultValue(block: ProcessFormDesignBlock) {
  return Array.isArray(block.defaultValue) ? block.defaultValue.map((item) => String(item)) : []
}

function setSelectedBlockDefaultValue(value: unknown) {
  if (!selectedBlock.value) return
  selectedBlock.value.defaultValue = value
}

function blockKindLabel(block: ProcessFormDesignBlock) {
  if (block.kind === 'BUSINESS_COMPONENT') return businessDefinition(block)?.label || '业务组件'
  if (block.kind === 'SHARED_FIELD') return '共享字段'
  const labels: Record<string, string> = {
    TEXT: '单行文本',
    TEXTAREA: '多行文本',
    NUMBER: '数字',
    AMOUNT: '金额',
    DATE: '日期',
    DATE_RANGE: '日期区间',
    SELECT: '下拉单选',
    MULTI_SELECT: '下拉多选',
    RADIO: '单选组',
    CHECKBOX: '复选组',
    SWITCH: '开关',
    ATTACHMENT: '附件',
    IMAGE: '图片',
    SECTION: '标题/说明'
  }
  return labels[controlType(block)] || '控件'
}

function blockPermissionSummary(block: ProcessFormDesignBlock) {
  return block.permission.sceneOverrides.length ? `覆盖 ${block.permission.sceneOverrides.length} 个场景` : '固定权限'
}

function placeholderPreview(block: ProcessFormDesignBlock) {
  if (block.defaultValue !== undefined && block.defaultValue !== null && block.defaultValue !== '') {
    return `默认值：${Array.isArray(block.defaultValue) ? block.defaultValue.join('、') : String(block.defaultValue)}`
  }
  return stringProp(block, 'placeholder', '暂无默认内容')
}

async function saveFormDesign(mode: 'draft' | 'final' = 'final') {
  if (!canEdit.value) {
    ElMessage.warning(isCreateMode.value
      ? (isExpenseDetailDesigner.value ? '当前账号没有新建费用明细表单权限' : '当前账号没有新建表单设计权限')
      : (isExpenseDetailDesigner.value ? '当前账号没有修改费用明细表单权限' : '当前账号没有修改表单设计权限'))
    return
  }
  if (!working.formName.trim()) {
    ElMessage.warning(`请先填写${designerNameLabel.value}`)
    return
  }
  const formNameIssue = validateMaxLength(working.formName, PM_NAME_MAX_LENGTH, designerNameLabel.value)
  if (formNameIssue) {
    ElMessage.warning(formNameIssue)
    return
  }
  const schema = normalizeFormSchema(working.schema)
  const schemaIssues = validateSchemaFieldKeys(
    schema,
    isExpenseDetailDesigner.value ? '费用明细表单' : '表单设计'
  )
  if (schemaIssues.length) {
    ElMessage.warning(schemaIssues[0])
    return
  }
  savingMode.value = mode
  try {
    const res = isExpenseDetailDesigner.value
      ? await (formId.value !== null
        ? processApi.updateExpenseDetailDesign(formId.value, {
          detailName: working.formName.trim(),
          detailDescription: working.formDescription?.trim() || '',
          detailType: working.detailType || 'NORMAL_REIMBURSEMENT',
          schema
        } satisfies ProcessExpenseDetailDesignSavePayload)
        : processApi.createExpenseDetailDesign({
          detailName: working.formName.trim(),
          detailDescription: working.formDescription?.trim() || '',
          detailType: working.detailType || 'NORMAL_REIMBURSEMENT',
          schema
        } satisfies ProcessExpenseDetailDesignSavePayload))
      : await (formId.value !== null
        ? processApi.updateFormDesign(formId.value, {
          templateType: working.templateType,
          formName: working.formName.trim(),
          formDescription: working.formDescription?.trim() || '',
          schema
        } satisfies ProcessFormDesignSavePayload)
        : processApi.createFormDesign({
          templateType: working.templateType,
          formName: working.formName.trim(),
          formDescription: working.formDescription?.trim() || '',
          schema
        } satisfies ProcessFormDesignSavePayload))
    assignDetail(res.data)
    selectedBlockId.value = selectedBlockId.value || working.schema.blocks[0]?.blockId || ''
    ElMessage.success(mode === 'draft' ? designerDraftSaveSuccessText.value : designerSaveSuccessText.value)
    const returnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo : ''
    const createdCode = isExpenseDetailDesigner.value
      ? String((res.data as ProcessExpenseDetailDesignDetail).detailCode || '')
      : String((res.data as ProcessFormDesignDetail).formCode || '')
    if (returnTo && createdCode) {
      await router.push(appendQueryParam(returnTo, isExpenseDetailDesigner.value ? 'createdExpenseDetailCode' : 'createdFormCode', createdCode))
      return
    }
    if (formId.value === null) {
      await router.replace({
        name: isExpenseDetailDesigner.value ? 'expense-workbench-process-expense-detail-edit' : 'expense-workbench-process-form-edit',
        params: { id: res.data.id },
        query: route.query
      })
    }
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, isCreateMode.value
      ? (isExpenseDetailDesigner.value ? '保存费用明细表单失败' : '保存表单设计失败')
      : (isExpenseDetailDesigner.value ? '更新费用明细表单失败' : '更新表单设计失败')))
  } finally {
    savingMode.value = null
  }
}

function appendQueryParam(path: string, key: string, value: string) {
  const separator = path.includes('?') ? '&' : '?'
  return `${path}${separator}${key}=${encodeURIComponent(value)}`
}

function goBack() {
  const returnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo : ''
  if (returnTo) {
    router.push(returnTo)
    return
  }
  if (isExpenseDetailDesigner.value) {
    router.push('/expense/workbench/process-management?section=expense-detail-form')
    return
  }
  router.push('/expense/workbench/process-management')
}
</script>

<style scoped>
.palette-tabs { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.palette-tab { border-radius: 999px; border: 1px solid #dbe2eb; background: #f8fafc; padding: 10px 12px; font-size: 13px; font-weight: 600; color: #475569; transition: all .2s ease; }
.palette-tab.is-active { border-color: #2563eb; background: #eff6ff; color: #1d4ed8; }
.palette-scroll { padding-right: 4px; }
.palette-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.palette-item { width: 100%; min-height: 112px; border-radius: 20px; border: 1px solid #dbe2eb; background: #fff; padding: 16px; transition: all .2s ease; }
.palette-item:hover { border-color: #93c5fd; background: #f8fbff; box-shadow: 0 10px 24px rgba(37,99,235,.08); }
.palette-item.is-disabled { cursor: not-allowed; opacity: .58; background: #f8fafc; }
.palette-item-description {
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}
.drop-zone, .inline-insert { width: 100%; border: 1px dashed #cbd5e1; border-radius: 18px; background: #fff; padding: 14px 16px; color: #64748b; font-size: 14px; transition: all .2s ease; }
.inline-insert { padding: 10px 12px; font-size: 13px; }
.drop-zone:hover, .drop-zone.is-active, .inline-insert:hover, .inline-insert.is-active { border-color: #2563eb; background: #eff6ff; color: #1d4ed8; }
.form-block-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 36px;
  gap: 8px;
  align-items: stretch;
}
.form-block-action-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
}
.form-block { position: relative; min-width: 0; border-radius: 24px; border: 1px solid #dbe2eb; background: #fff; padding: 20px; text-align: left; transition: all .2s ease; box-shadow: 0 10px 30px rgba(15,23,42,.04); }
.form-block:hover { border-color: #93c5fd; box-shadow: 0 14px 32px rgba(37,99,235,.08); }
.form-block.is-selected, .form-block-shell.is-selected .form-block { border-color: #2563eb; box-shadow: 0 16px 36px rgba(37,99,235,.12); }
.form-block.drop-before::before, .form-block.drop-after::after { content: ''; position: absolute; left: 18px; right: 18px; height: 3px; border-radius: 999px; background: #2563eb; }
.form-block.drop-before::before { top: -2px; }
.form-block.drop-after::after { bottom: -2px; }
.block-expand-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 72px;
  border-radius: 999px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  transition: all .2s ease;
}
.block-expand-action:hover { border-color: #2563eb; background: #dbeafe; }
.preview-chip { border-radius: 999px; background: #f1f5f9; padding: 6px 10px; font-size: 12px; color: #475569; }
.preview-note { border-radius: 16px; background: #f8fafc; padding: 12px 14px; font-size: 13px; line-height: 1.6; color: #475569; }
:deep(.palette-card .el-card__body) { padding-top: 0; }
.palette-scroll::-webkit-scrollbar { width: 8px; }
.palette-scroll::-webkit-scrollbar-thumb { border-radius: 999px; background: rgba(148,163,184,.85); }
.palette-scroll::-webkit-scrollbar-track { background: rgba(241,245,249,.92); }
.designer-side-scroll { max-height: calc(100vh - 220px); overflow-y: auto; padding-right: 4px; }
.process-form-designer-floating-bar { display: flex; justify-content: center; width: 100%; }
.process-form-designer-floating-bar__inner {
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
:deep(.process-form-designer-floating-bar__button.el-button) {
  min-height: 38px;
  padding: 0 20px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 600;
}
:deep(.process-form-designer-floating-bar__button--success.el-button) {
  box-shadow: 0 12px 24px rgba(34, 197, 94, 0.14);
}
@media (min-width: 1280px) { .palette-scroll { max-height: calc(100vh - 220px); overflow-y: auto; } }
@media (max-width: 1279px) {
  .palette-grid { grid-template-columns: 1fr; }
  .palette-scroll, .designer-side-scroll { max-height: none; overflow-y: visible; padding-right: 0; }
}
@media (max-width: 767px) {
  .process-form-designer-floating-bar__inner {
    width: calc(100vw - 24px);
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 14px;
    font-size: 14px;
  }
  :deep(.process-form-designer-floating-bar__button.el-button) {
    flex: 1 1 calc(50% - 5px);
    min-height: 34px;
    padding: 0 14px;
    font-size: 14px;
  }
  .form-block-shell { grid-template-columns: minmax(0, 1fr) 32px; gap: 6px; }
  .form-block-action-slot { min-width: 32px; }
  .block-expand-action { width: 28px; height: 56px; font-size: 16px; }
}
</style>
