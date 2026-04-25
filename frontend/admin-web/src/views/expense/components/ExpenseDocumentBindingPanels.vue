<template>
  <el-card
    v-for="panel in panels"
    :key="panel.key"
    class="expense-wb-panel"
    :data-testid="panel.cardTestId"
  >
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-lg font-semibold text-slate-800">{{ panel.title }}</p>
          <p class="mt-1 text-sm text-slate-500">{{ panel.description }}</p>
        </div>
        <div class="binding-card-header-actions">
          <el-tag effect="plain">{{ panel.count }} {{ bindingCountSuffix }}</el-tag>
          <button
            type="button"
            class="binding-card-toggle"
            :data-testid="panel.toggleTestId"
            @click="panel.toggle()"
          >
            {{ panel.toggleText }}
          </button>
        </div>
      </div>
    </template>

    <div v-if="panel.expanded" class="space-y-5">
      <div
        v-for="section in panel.sections"
        :key="section.key"
        class="space-y-3"
      >
        <div class="flex items-center justify-between gap-3">
          <p class="text-sm font-semibold text-slate-800">{{ section.title }}</p>
          <el-tag size="small" effect="plain">{{ section.count }} {{ bindingCountSuffix }}</el-tag>
        </div>
        <div v-if="section.items.length" class="space-y-3">
          <div
            v-for="item in section.items"
            :key="item.key"
            class="expense-wb-detail-card"
            :data-testid="section.itemTestId"
          >
            <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div class="space-y-2">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="text-base font-semibold text-slate-800">{{ item.title }}</p>
                  <el-tag size="small" effect="plain">{{ item.templateTypeLabel }}</el-tag>
                  <el-tag v-if="item.statusLabel" size="small" effect="plain">{{ item.statusLabel }}</el-tag>
                </div>
                <p class="text-sm text-slate-500">{{ item.metaLine }}</p>
                <p class="text-xs leading-6 text-slate-500">{{ item.detailLine }}</p>
              </div>
              <div class="expense-wb-compact-actions">
                <el-button
                  plain
                  :data-testid="`open-bound-document-${item.documentCode}`"
                  @click="emit('open-bound-document', item.documentCode)"
                >
                  {{ viewBoundDocumentLabel }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <p
          v-else
          class="binding-card-inline-empty"
          :data-testid="`${section.key}-empty`"
        >
          {{ section.emptyText }}
        </p>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { BindingPanelDisplay } from '../composables/useExpenseDocumentDetailDisplayOwner'

defineProps<{
  panels: BindingPanelDisplay[]
  bindingCountSuffix: string
  viewBoundDocumentLabel: string
}>()

const emit = defineEmits<{
  'open-bound-document': [documentCode: string]
}>()
</script>
