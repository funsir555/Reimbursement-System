import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import type { ArchiveAgentRunDetail } from '@/api'
import AgentRunDetailPanel from '../components/AgentRunDetailPanel.vue'

function detail(outputPayload: Record<string, unknown>, triggerSource: string): ArchiveAgentRunDetail {
  return {
    id: 1,
    runNo: 'RUN-1',
    agentId: 1,
    triggerType: 'MANUAL',
    triggerSource,
    status: 'SUCCESS',
    inputPayload: {},
    outputPayload,
    steps: [{ stepNo: 1, nodeKey: 'query', nodeType: 'tool', status: 'SUCCESS',
      inputPayload: { privateValue: 'secret-input' }, outputPayload: { privateValue: 'secret-output' } }],
    artifacts: []
  }
}

describe('AgentRunDetailPanel', () => {
  it('hides payloads in protected runs', () => {
    const wrapper = mount(AgentRunDetailPanel, {
      props: { detail: detail({ protected: true, privateValue: 'secret-result' }, 'FINWORK'), loading: false },
      global: { stubs: { 'el-tag': true } }
    })

    expect(wrapper.text()).toContain('受控查询仅展示步骤状态与耗时')
    expect(wrapper.text()).not.toContain('secret-input')
    expect(wrapper.text()).not.toContain('secret-output')
    expect(wrapper.text()).not.toContain('secret-result')
  })

  it('shows saved details for an authorized workbench test run', () => {
    const wrapper = mount(AgentRunDetailPanel, {
      props: { detail: detail({ result: 'test-result' }, 'WORKBENCH_TEST'), loading: false },
      global: { stubs: { 'el-tag': true } }
    })

    expect(wrapper.text()).toContain('test-result')
    expect(wrapper.text()).toContain('secret-input')
    expect(wrapper.text()).toContain('secret-output')
  })
})
