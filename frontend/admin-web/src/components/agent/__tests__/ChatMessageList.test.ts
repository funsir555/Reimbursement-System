import { describe, expect, it } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import ChatMessageList from '../ChatMessageList.vue'
import type { MessageVO } from '@/api/modules/agent-chat-types'

describe('FinWork clarification', () => {
  it('submits a partial free-text answer as message text', () => {
    const message: MessageVO = {
      role: 'assistant', kind: 'CLARIFY', content: '请补充查询条件',
      clarifications: [
        { key: 'periodRange', question: '请说明会计期间', options: [] },
        { key: 'voucherStatus', question: '请选择凭证范围', options: [
          { id: 'POSTED_ONLY', label: '仅已记账' }
        ] }
      ]
    }
    const wrapper = shallowMount(ChatMessageList, {
      props: { messages: [message], getMessageKey: () => 'answer' }
    })
    const vm = wrapper.vm as unknown as {
      answersFor: (message: MessageVO) => Record<string, string>
      submitClarification: (message: MessageVO) => void
    }
    vm.answersFor(message).periodRange = '去年一季度'
    vm.submitClarification(message)
    expect(wrapper.emitted('clarify')?.[0]).toEqual([
      { periodRange: '去年一季度' }, ['请说明会计期间：去年一季度']
    ])
    wrapper.unmount()
  })
})
