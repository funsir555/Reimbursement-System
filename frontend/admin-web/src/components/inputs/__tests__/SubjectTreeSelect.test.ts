import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
import SubjectTreeSelect from '@/components/inputs/SubjectTreeSelect.vue'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'

const TreeSelectStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: undefined
    },
    data: {
      type: Array,
      default: () => []
    },
    reserveKeyword: {
      type: Boolean,
      default: true
    },
    filterNodeMethod: {
      type: Function,
      default: undefined
    }
  },
  emits: ['update:modelValue'],
  template: '<div class="tree-select-stub" />'
})

const subjectOptions = [
  {
    value: '1001',
    code: '1001',
    name: '库存现金',
    label: '1001  库存现金',
    subjectCategory: 'ASSET',
    subjectCategoryLabel: '资产'
  },
  {
    value: '5601',
    code: '5601',
    name: '管理费用',
    label: '5601  管理费用',
    subjectCategory: 'PROFIT',
    subjectCategoryLabel: '损益'
  },
  {
    value: '560101',
    code: '560101',
    name: '办公费',
    label: '560101  办公费',
    parentValue: '5601',
    subjectCategory: 'PROFIT',
    subjectCategoryLabel: '损益'
  }
]

describe('SubjectTreeSelect', () => {
  it('groups subjects by category and rebuilds hierarchy by parentValue', () => {
    const wrapper = mount(SubjectTreeSelect, {
      props: {
        modelValue: '560101',
        options: subjectOptions,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const treeData = wrapper.findComponent(TreeSelectStub).props('data') as Array<{
      value: string
      label: string
      disabled?: boolean
      children: Array<{ value: string; children: Array<{ value: string }> }>
    }>

    expect(treeData.map((item) => item.label)).toEqual(['资产', '损益'])
    expect(treeData[0]?.disabled).toBe(true)
    expect(treeData[0]?.children.map((item) => item.value)).toEqual(['1001'])
    expect(treeData[1]?.children.map((item) => item.value)).toEqual(['5601'])
    expect(treeData[1]?.children[0]?.children.map((item) => item.value)).toEqual(['560101'])
  })

  it('matches category label, code and name in the tree filter', () => {
    const wrapper = mount(SubjectTreeSelect, {
      props: {
        modelValue: '1001',
        options: subjectOptions,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const filterNodeMethod = wrapper.findComponent(TreeSelectStub).props('filterNodeMethod') as ((query: string, data: { code?: string; name?: string; label?: string; subjectCategoryLabel?: string }) => boolean)

    expect(filterNodeMethod('资产', { subjectCategoryLabel: '资产', label: '资产' })).toBe(true)
    expect(filterNodeMethod('560101', { code: '560101', name: '办公费', label: '560101  办公费' })).toBe(true)
    expect(filterNodeMethod('办公', { code: '560101', name: '办公费', label: '560101  办公费' })).toBe(true)
    expect(filterNodeMethod('不存在', { code: '560101', name: '办公费', label: '560101  办公费' })).toBe(false)
  })

  it('appends missing selected legacy subjects under the current-voucher group', () => {
    const wrapper = mount(SubjectTreeSelect, {
      props: {
        modelValue: '9999',
        options: subjectOptions,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const treeData = wrapper.findComponent(TreeSelectStub).props('data') as Array<{
      label: string
      children: Array<{ value: string }>
    }>

    expect(treeData.map((item) => item.label)).toContain('当前凭证科目')
    expect(treeData.find((item) => item.label === '当前凭证科目')?.children.map((item) => item.value)).toContain('9999')
  })

  it('keeps the native select fallback in test mode and preserves forwarded attrs', () => {
    const wrapper = mount(SubjectTreeSelect, {
      props: {
        modelValue: '1001',
        options: subjectOptions
      },
      attrs: {
        'data-subject-row-id': 'row-1'
      }
    })

    expect(wrapper.find('select[data-subject-row-id="row-1"]').exists()).toBe(true)
    expect(wrapper.findComponent(TreeSelectStub).exists()).toBe(false)
  })

  it('forwards the global filterable behavior to tree select', () => {
    const wrapper = mount(SubjectTreeSelect, {
      props: {
        modelValue: '1001',
        options: subjectOptions,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    expect(wrapper.findComponent(TreeSelectStub).props('reserveKeyword')).toBe(globalFilterableSelectProps.reserveKeyword)
  })
})
