<template>
  <template v-for="node in nodes" :key="node.key">
    <el-menu-item
      v-if="!node.children?.length"
      :index="node.index"
      :class="resolveLevelClass(level)"
    >
      <span v-if="level === 1 && node.iconKey === 'Agent'" class="flex items-center gap-2">
        <pixel-duck-bot-icon class="h-[18px] w-[18px] text-amber-600" />
        <span>{{ node.title }}</span>
      </span>
      <template v-else>
        <el-icon v-if="level === 1 && node.iconKey"><component :is="resolveMenuIcon(node.iconKey)" /></el-icon>
        <el-badge
          v-if="node.badgeCount && node.badgeCount > 0"
          :value="node.badgeCount"
          :max="9999"
          class="navigation-node-badge"
          :data-testid="`navigation-badge-${node.key}`"
        >
          <span>{{ node.title }}</span>
        </el-badge>
        <span v-else>{{ node.title }}</span>
      </template>
    </el-menu-item>

    <el-sub-menu
      v-else
      :index="node.index"
      :class="resolveLevelClass(level)"
    >
      <template #title>
        <template v-if="level === 1">
          <el-icon v-if="node.iconKey"><component :is="resolveMenuIcon(node.iconKey)" /></el-icon>
          <button
            v-if="node.landingIndex"
            type="button"
            class="menu-group-landing"
            :data-landing-index="node.landingIndex"
            @click="handleGroupTitleClick(node.landingIndex)"
          >
            {{ node.title }}
          </button>
          <span v-else>{{ node.title }}</span>
        </template>
        <button
          v-else-if="node.landingIndex"
          type="button"
          class="menu-group-landing"
          :data-landing-index="node.landingIndex"
          @click="handleGroupTitleClick(node.landingIndex)"
        >
          {{ node.title }}
        </button>
        <span v-else>{{ node.title }}</span>
      </template>

      <MainNavigationTree :nodes="node.children" :level="level + 1" />
    </el-sub-menu>
  </template>
</template>

<script setup lang="ts">
import { House, Wallet, Coin, FolderOpened, Setting } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import PixelDuckBotIcon from '@/components/icons/PixelDuckBotIcon.vue'
import type { NavigationIconKey, NavigationMenuNode } from '@/router/navigation-config'

defineOptions({
  name: 'MainNavigationTree'
})

const props = withDefaults(defineProps<{
  nodes: NavigationMenuNode[]
  level?: number
}>(), {
  level: 1
})

const router = useRouter()

const MENU_ICON_MAP = {
  House,
  Wallet,
  Coin,
  FolderOpened,
  Setting
} satisfies Record<Exclude<NavigationIconKey, 'Agent'>, unknown>

function resolveMenuIcon(iconKey?: NavigationIconKey) {
  if (!iconKey || iconKey === 'Agent') {
    return null
  }
  return MENU_ICON_MAP[iconKey]
}

function resolveLevelClass(level: number) {
  return `menu-level-${Math.min(level, 4)}`
}

function handleGroupTitleClick(path: string) {
  const targetPath = String(path || '')
  if (!targetPath) {
    return
  }
  void router.push(targetPath)
}
</script>

<style scoped>
.menu-group-landing {
  appearance: none;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  padding: 0;
}

.navigation-node-badge {
  display: inline-flex;
  align-items: center;
}

:deep(.navigation-node-badge .el-badge__content) {
  top: 0;
  transform: translateY(0) translateX(100%);
  background-color: #f04444;
  border-color: #fff;
  box-shadow: 0 0 0 1px rgba(240, 68, 68, 0.08);
}
</style>
