<template>
  <div class="space-y-4">
    <div
      v-for="connector in connectors"
      :key="itemKeyPrefix ? `${itemKeyPrefix}-${connector.platformCode}` : connector.platformCode"
      class="rounded-2xl border border-slate-200 p-4"
    >
      <div class="mb-3 flex items-center justify-between gap-4">
        <div>
          <div class="font-semibold text-slate-900" :data-testid="titleTestId || undefined">
            {{ resolveConnectorPlatformName(connector) }}
          </div>
          <div class="text-xs text-slate-500">
            {{ connector.lastSyncMessage || '尚未执行同步' }}
          </div>
        </div>
        <div class="flex items-center gap-3">
          <el-switch v-model="connector.enabled" active-text="启用" />
          <el-switch v-model="connector.autoSyncEnabled" active-text="自动同步" />
        </div>
      </div>

      <div v-if="isWecomConnector(connector)" class="space-y-3">
        <div class="grid gap-3 md:grid-cols-2">
          <el-input v-model="connector.corpId" placeholder="企业 ID" />
          <el-input v-model="connector.appSecret" placeholder="通讯录 Secret" show-password />
          <el-input v-model="connector.agentId" placeholder="AgentId（可选）" />
          <el-input-number
            v-model="connector.syncIntervalMinutes"
            :min="5"
            :max="1440"
            class="w-full"
          />
        </div>
        <div class="text-xs text-slate-500">
          AppKey / AppId 对企微同步不需要，本次同步仅使用企业 ID 与通讯录 Secret。
        </div>
      </div>

      <div v-else class="grid gap-3 md:grid-cols-2">
        <el-input v-model="connector.appKey" placeholder="App Key / Client ID" />
        <el-input v-model="connector.appSecret" placeholder="App Secret" show-password />
        <el-input v-model="connector.appId" placeholder="App ID" />
        <el-input v-model="connector.corpId" placeholder="Corp / Tenant ID" />
        <el-input v-model="connector.agentId" placeholder="Agent ID" />
        <el-input-number
          v-model="connector.syncIntervalMinutes"
          :min="5"
          :max="1440"
          class="w-full"
        />
      </div>

      <div class="mt-4 flex gap-2">
        <el-button
          v-if="canSyncConfig"
          type="primary"
          :data-testid="saveTestId || undefined"
          @click="$emit('save', connector)"
        >
          保存配置
        </el-button>
        <el-button
          v-if="canRunSync"
          :data-testid="runTestId || undefined"
          @click="$emit('run', connector.platformCode)"
        >
          立即同步
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SyncConnectorConfig } from '@/api'

defineProps<{
  connectors: SyncConnectorConfig[]
  canSyncConfig: boolean
  canRunSync: boolean
  isWecomConnector: (connector: SyncConnectorConfig) => boolean
  resolveConnectorPlatformName: (connector: SyncConnectorConfig) => string
  titleTestId?: string
  saveTestId?: string
  runTestId?: string
  itemKeyPrefix?: string
}>()

defineEmits<{
  (e: 'save', connector: SyncConnectorConfig): void
  (e: 'run', platformCode: string): void
}>()
</script>
