<template>
  <div class="run-detail-drawer">
    <div v-if="loading" class="empty-state small"><p>正在加载运行详情...</p></div>
    <template v-else-if="detail">
      <div v-if="isProtected" class="protected-log-notice" role="status">
        受控查询仅展示步骤状态与耗时。输入、输出和敏感错误详情已隐藏；这里的步骤数表示实际执行记录数。
      </div>
      <div v-else-if="isWorkbenchTest" class="protected-log-notice" role="status">
        工作台测试日志仅对同时是运行创建者且拥有日志查看权限的用户开放；业务查询仍按测试者的数据权限执行。历史运行只展示当时实际保存的内容。
      </div>
      <div class="log-summary">
        <div><span>Agent 版本</span><strong>v{{ detail.agentVersionNo || '-' }}</strong></div>
        <div><span>运行结果</span><strong>{{ detail.summary || detail.status }}</strong></div>
        <div><span>输出摘要</span><strong>{{ isProtected ? '受权限保护' : stringifyInline(detail.outputPayload) }}</strong></div>
      </div>
      <div v-if="isWorkbenchTest" class="debug-output mt-sm"><label>测试运行输出</label><pre>{{ prettyJson(detail.outputPayload) }}</pre></div>
      <div class="stack gap-sm mt-sm">
        <article v-for="step in detail.steps" :key="`${step.stepNo}-${step.nodeKey}`" class="mini-card">
          <div class="row between gap-sm"><div><div class="muted xs">STEP {{ step.stepNo }}</div><strong>{{ step.nodeLabel || step.nodeKey }}</strong></div><el-tag size="small" :type="runStatusType(step.status)" effect="plain">{{ step.status }}</el-tag></div>
          <div class="row wrap gap-sm muted xs mt-xs"><span>{{ step.nodeType }}</span><span>{{ step.durationMs != null ? `${step.durationMs}ms` : '执行中' }}</span></div>
          <div v-if="!isProtected" class="payload-grid mt-sm"><div><label>输入</label><pre>{{ prettyJson(step.inputPayload) }}</pre></div><div><label>输出</label><pre>{{ prettyJson(step.outputPayload) }}</pre></div></div>
          <p v-if="step.errorMessage" class="error mt-sm">{{ step.errorMessage }}</p>
        </article>
        <div v-if="isProtected && !detail.steps.length" class="empty-state small"><p>本次运行没有生成步骤记录。</p></div>
      </div>
    </template>
    <div v-else class="empty-state small"><p>运行详情暂不可用。</p></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ArchiveAgentRunDetail } from '@/api'

const props = defineProps<{ detail: ArchiveAgentRunDetail | null; loading: boolean }>()
const isProtected = computed(() => props.detail?.outputPayload?.protected === true)
const isWorkbenchTest = computed(() => !isProtected.value && props.detail?.triggerType === 'MANUAL'
  && ['WORKBENCH_TEST', 'workbench.manual', 'WORKBENCH'].includes(props.detail?.triggerSource || ''))

function prettyJson(value: unknown) { try { return JSON.stringify(value ?? {}, null, 2) } catch { return '{}' } }
function stringifyInline(value: unknown) { try { const text = JSON.stringify(value ?? {}); return text.length > 72 ? `${text.slice(0, 72)}...` : text } catch { return '{}' } }
function runStatusType(status?: string) { return ({ SUCCESS: 'success', COMPLETED: 'success', RUNNING: 'warning', PENDING: 'warning', FAILED: 'danger' } as Record<string, string>)[status || ''] || 'info' }
</script>

<style scoped>
.run-detail-drawer { padding: 0 4px 18px; }
.row { display: flex; align-items: center; } .row.between { justify-content: space-between; } .row.wrap { flex-wrap: wrap; }
.stack { display: flex; flex-direction: column; } .gap-sm { gap: 10px; } .mt-xs { margin-top: 4px; } .mt-sm { margin-top: 10px; }
.log-summary { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 10px; margin-top: 16px; }
.log-summary div { padding: 12px 14px; border-radius: 18px; background: rgba(255,255,255,.74); border: 1px solid rgba(39,48,71,.08); }
.log-summary span { display: block; font-size: 12px; color: rgba(39,48,71,.55); } .log-summary strong { display: block; margin-top: 4px; color: var(--ink); }
.mini-card { padding: 14px; border: 1px solid rgba(39,48,71,.08); border-radius: 22px; background: linear-gradient(180deg, #fff 0%, #fffaf1 100%); }
.empty-state { display: flex; min-height: 160px; flex-direction: column; align-items: center; justify-content: center; gap: 10px; border: 1px dashed rgba(39,48,71,.16); border-radius: 24px; background: rgba(255,250,241,.72); color: rgba(39,48,71,.62); text-align: center; padding: 20px; } .empty-state.small { min-height: 120px; }
.protected-log-notice { padding: 12px 14px; border-radius: 14px; background: #fff7e8; border: 1px solid rgba(217,119,6,.24); color: #80521b; font-size: 13px; line-height: 1.6; }
.debug-output { padding: 12px 14px; border-radius: 14px; background: #f8fafc; } .debug-output label { display: block; margin-bottom: 6px; font-size: 12px; font-weight: 700; color: rgba(39,48,71,.58); } .debug-output pre { max-height: 320px; overflow: auto; margin: 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; color: var(--ink); }
.payload-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 10px; } .payload-grid label { display: block; margin-bottom: 6px; font-size: 12px; font-weight: 700; color: rgba(39,48,71,.58); } .payload-grid pre { min-height: 108px; margin: 0; padding: 10px; border-radius: 16px; background: #f8fafc; white-space: pre-wrap; word-break: break-word; font: 13px Consolas, monospace; color: rgba(39,48,71,.72); }
.muted { color: rgba(39,48,71,.6); } .muted.xs { font-size: 12px; } .error { color: #dc2626; }
@media (max-width: 1120px) { .log-summary, .payload-grid { grid-template-columns: 1fr; } .row.between { align-items: flex-start; } }
</style>
