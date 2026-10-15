import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import FinWorkConversationSidebar from '../FinWorkConversationSidebar.vue'

const dialog = vi.hoisted(() => ({ prompt: vi.fn(), confirm: vi.fn() }))
vi.mock('element-plus', async importOriginal => ({
  ...(await importOriginal<typeof import('element-plus')>()), ElMessageBox: dialog
}))

const conversation = { sessionId: 'session-1', title: '原会话', companyId: 'COMPANY_A', pinned: false }

function render() {
  return mount(FinWorkConversationSidebar, {
    props: {
      open: true, selectedId: 'session-1', pinnedConversations: [],
      conversations: [conversation], hasMore: false, loading: false, busy: false
    },
    global: { stubs: { 'el-button': true, 'el-icon': true } }
  })
}

describe('FinWork conversation sidebar', () => {
  it('opens the management menu by right click and offers pin action', async () => {
    const wrapper = render()
    await wrapper.find('.conversation-row').trigger('contextmenu', { clientX: 10, clientY: 20 })
    expect(wrapper.find('[role="menu"]').exists()).toBe(true)
    await wrapper.findAll('[role="menuitem"]')[1]!.trigger('click')
    expect(wrapper.emitted('pin')).toEqual([['session-1', true]])
    wrapper.unmount()
  })

  it('renames and deletes through confirmed dialogs', async () => {
    dialog.prompt.mockResolvedValueOnce({ value: '  新会话  ' })
    dialog.confirm.mockResolvedValueOnce('confirm')
    const wrapper = render()
    await wrapper.find('.row-actions').trigger('click')
    await wrapper.findAll('[role="menuitem"]')[0]!.trigger('click')
    await vi.waitFor(() => expect(wrapper.emitted('rename')).toEqual([['session-1', '新会话']]))
    await wrapper.find('.row-actions').trigger('click')
    await wrapper.findAll('[role="menuitem"]')[2]!.trigger('click')
    await vi.waitFor(() => expect(wrapper.emitted('delete')).toEqual([['session-1']]))
    wrapper.unmount()
  })
})
