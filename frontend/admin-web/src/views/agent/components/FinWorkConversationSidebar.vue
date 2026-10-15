<template>
  <aside class="conversation-sidebar" :class="{ 'sidebar-open': open }">
    <div class="sidebar-header">
      <strong>沟通记录</strong>
      <el-button size="small" :icon="Plus" :disabled="busy" @click="emit('new')">新建</el-button>
    </div>
    <div class="conversation-list">
      <template v-if="pinnedConversations.length">
        <div class="group-label">置顶</div>
        <div v-for="conversation in pinnedConversations" :key="conversation.sessionId"
          class="conversation-row" @contextmenu.prevent="showMenu($event, conversation)">
          <button type="button" class="conversation-item"
            :class="{ active: conversation.sessionId === selectedId }" :disabled="busy"
            @click="emit('select', conversation.sessionId)">
            <span class="conversation-title"><el-icon><StarFilled /></el-icon><span class="title-text">{{ conversation.title || '新会话' }}</span></span>
            <small>{{ companyAndTime(conversation) }}</small>
          </button>
          <button type="button" class="row-actions" :disabled="busy"
            :aria-label="`管理会话 ${conversation.title || '新会话'}`"
            @click.stop="showMenu($event, conversation)"><el-icon><MoreFilled /></el-icon></button>
        </div>
      </template>
      <div v-if="conversations.length" class="group-label">最近</div>
      <div v-for="conversation in conversations" :key="conversation.sessionId"
        class="conversation-row" @contextmenu.prevent="showMenu($event, conversation)">
        <button type="button" class="conversation-item"
          :class="{ active: conversation.sessionId === selectedId }" :disabled="busy"
          @click="emit('select', conversation.sessionId)">
          <span class="conversation-title"><span class="title-text">{{ conversation.title || '新会话' }}</span></span>
          <small>{{ companyAndTime(conversation) }}</small>
        </button>
        <button type="button" class="row-actions" :disabled="busy"
          :aria-label="`管理会话 ${conversation.title || '新会话'}`"
          @click.stop="showMenu($event, conversation)"><el-icon><MoreFilled /></el-icon></button>
      </div>
      <div v-if="!loading && !conversations.length && !pinnedConversations.length"
        class="sidebar-empty">暂无沟通记录</div>
    </div>
    <el-button v-if="hasMore" class="load-more" text :loading="loading"
      @click="emit('load-more')">加载更多</el-button>
    <div v-if="menuTarget" ref="menuRef" class="conversation-menu" role="menu" :style="menuStyle"
      @keydown.esc="closeMenu">
      <button type="button" role="menuitem" @click="renameTarget">重命名</button>
      <button type="button" role="menuitem" @click="pinTarget">{{ menuTarget.pinned ? '取消置顶' : '置顶' }}</button>
      <button type="button" role="menuitem" class="delete-action" @click="deleteTarget">删除</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { MoreFilled, Plus, StarFilled } from '@element-plus/icons-vue'
import type { ConversationVO } from '@/api/modules/agent-chat-types'

const props = defineProps<{
  open: boolean
  selectedId?: string
  pinnedConversations: ConversationVO[]
  conversations: ConversationVO[]
  hasMore: boolean
  loading: boolean
  busy: boolean
}>()

const emit = defineEmits<{
  select: [id: string]
  new: []
  'load-more': []
  rename: [id: string, title: string]
  pin: [id: string, pinned: boolean]
  delete: [id: string]
}>()

const menuTarget = ref<ConversationVO>()
const menuRef = ref<HTMLElement>()
const menuStyle = ref({ left: '0px', top: '0px' })

async function showMenu(event: MouseEvent, conversation: ConversationVO) {
  if (props.busy) return
  event.preventDefault()
  menuTarget.value = conversation
  const anchor = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = event.clientX || anchor.right
  const y = event.clientY || anchor.bottom
  menuStyle.value = {
    left: `${Math.max(8, Math.min(x, window.innerWidth - 176))}px`,
    top: `${Math.max(8, Math.min(y, window.innerHeight - 140))}px`
  }
  await nextTick()
  menuRef.value?.querySelector<HTMLButtonElement>('button')?.focus()
}

function closeMenu() { menuTarget.value = undefined }

function onOutside(event: PointerEvent) {
  if (menuTarget.value && !menuRef.value?.contains(event.target as Node)) closeMenu()
}

function onEscape(event: KeyboardEvent) {
  if (event.key === 'Escape') closeMenu()
}

onMounted(() => {
  document.addEventListener('pointerdown', onOutside)
  document.addEventListener('keydown', onEscape)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onOutside)
  document.removeEventListener('keydown', onEscape)
})

async function renameTarget() {
  const target = menuTarget.value
  closeMenu()
  if (!target) return
  try {
    const { value } = await ElMessageBox.prompt('请输入会话名称', '重命名会话', {
      inputValue: target.title || '',
      inputValidator: value => {
        const normalized = value.trim().replace(/\s+/g, ' ')
        return normalized && [...normalized].length <= 60 ? true : '请输入 1～60 个字符'
      },
      confirmButtonText: '保存', cancelButtonText: '取消'
    })
    emit('rename', target.sessionId, value.trim().replace(/\s+/g, ' '))
  } catch { /* 用户取消 */ }
}

function pinTarget() {
  const target = menuTarget.value
  closeMenu()
  if (target) emit('pin', target.sessionId, !target.pinned)
}

async function deleteTarget() {
  const target = menuTarget.value
  closeMenu()
  if (!target) return
  try {
    await ElMessageBox.confirm(`从沟通记录中删除“${target.title || '新会话'}”？`, '删除会话', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    emit('delete', target.sessionId)
  } catch { /* 用户取消 */ }
}

function companyAndTime(conversation: ConversationVO) {
  const value = conversation.lastMessageAt || conversation.createdAt
  const time = value ? new Date(value).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  }) : ''
  return `${conversation.companyName || conversation.companyId || '当前公司'} · ${time}`
}
</script>

<style scoped>
.conversation-sidebar { width: 260px; flex: none; display: flex; flex-direction: column; min-height: 0; background: #fff; border-right: 1px solid #e7ebf1; }
.sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 14px; color: #273047; }
.conversation-list { flex: 1; min-height: 0; overflow-y: auto; padding: 0 8px; }
.group-label { padding: 10px 12px 4px; color: #8490a3; font-size: 11px; }
.conversation-row { display: flex; align-items: center; border-radius: 10px; }
.conversation-row:hover, .conversation-row:focus-within { background: #eef6f6; }
.conversation-item { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; padding: 11px 12px; text-align: left; border: 0; border-radius: 10px; background: transparent; color: #273047; cursor: pointer; }
.conversation-item.active { background: #eef6f6; }
.conversation-item:disabled { cursor: not-allowed; opacity: .65; }
.conversation-title { display: flex; align-items: center; gap: 4px; min-width: 0; width: 100%; font-size: 13px; font-weight: 600; }
.title-text { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conversation-title .el-icon { flex: none; color: #d97706; }
.conversation-item small { color: #778296; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; width: 100%; }
.row-actions { flex: none; margin-right: 5px; padding: 6px; border: 0; border-radius: 6px; background: transparent; color: #778296; cursor: pointer; }
.row-actions:hover { background: #dcebea; }
.row-actions:disabled { opacity: .4; cursor: not-allowed; }
.sidebar-empty { color: #8490a3; text-align: center; padding: 28px 8px; font-size: 12px; }
.load-more { margin: 10px; }
.conversation-menu { position: fixed; z-index: 3000; display: flex; flex-direction: column; width: 168px; padding: 5px; border: 1px solid #e7ebf1; border-radius: 9px; background: #fff; box-shadow: 0 10px 24px #27304720; }
.conversation-menu button { padding: 9px 12px; border: 0; border-radius: 5px; background: transparent; text-align: left; color: #273047; cursor: pointer; }
.conversation-menu button:hover, .conversation-menu button:focus { background: #eef6f6; }
.conversation-menu .delete-action { color: #c2413b; }
@media (max-width: 900px) {
  .conversation-sidebar { display: none; position: absolute; z-index: 20; top: 0; bottom: 0; left: 0; box-shadow: 10px 0 30px #27304720; }
  .conversation-sidebar.sidebar-open { display: flex; }
}
</style>
