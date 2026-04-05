import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
import TemplateTypeDialog from '@/components/process/TemplateTypeDialog.vue'

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /></div>'
})

describe('TemplateTypeDialog', () => {
  it('renders the contract option and emits selection', async () => {
    const wrapper = mount(TemplateTypeDialog, {
      props: {
        modelValue: true,
        options: [
          {
            code: 'contract',
            name: '合同单',
            subtitle: '合同管理',
            description: '适用于合同评审、签订流转和合同管理。',
            accent: 'emerald'
          }
        ]
      },
      global: {
        stubs: {
          'el-dialog': SimpleContainer,
          'el-icon': SimpleContainer
        }
      }
    })

    expect(wrapper.text()).toContain('合同单')
    expect(wrapper.text()).toContain('合同管理')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('select')).toEqual([['contract']])
  })
})
