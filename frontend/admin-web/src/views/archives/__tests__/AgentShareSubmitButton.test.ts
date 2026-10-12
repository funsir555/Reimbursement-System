import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { digitalEmployeeApi } from '@/api/modules/digital-employee'
import AgentShareSubmitButton from '../components/AgentShareSubmitButton.vue'

vi.mock('@/utils/permissions', () => ({
  hasAnyPermission: () => true,
  readStoredUser: () => ({ permissionCodes: ['agent:workbench:share_submit'] })
}))
vi.mock('@/api/modules/digital-employee', () => ({
  digitalEmployeeApi: { submitShare: vi.fn() }
}))

describe('AgentShareSubmitButton', () => {
  it('shows the published version and submits its fixed version ID', async () => {
    vi.mocked(digitalEmployeeApi.submitShare).mockResolvedValue({ data: {} } as never)
    const wrapper = mount(AgentShareSubmitButton, {
      props: {
        agentId: 3,
        latestVersionNo: 7,
        publishedVersionNo: 6,
        versions: [
          { id: 28, versionNo: 7, published: false },
          { id: 27, versionNo: 6, published: true }
        ]
      },
      global: {
        stubs: {
          'el-button': {
            props: ['disabled'],
            template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('提交企业共享 · v6')
    expect(wrapper.text()).toContain('当前画布为 v7，尚未发布')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(digitalEmployeeApi.submitShare).toHaveBeenCalledWith(3, 27)
  })
})
