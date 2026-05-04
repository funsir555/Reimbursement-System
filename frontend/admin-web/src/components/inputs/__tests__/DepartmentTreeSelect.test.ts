import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
import DepartmentTreeSelect from '@/components/inputs/DepartmentTreeSelect.vue'
import { globalCollapseTagTooltipProps } from '@/utils/collapseTagTooltip'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import type { DepartmentOptionLike, DepartmentTreeValue } from '@/utils/departmentTree'

const TreeSelectStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number, Array],
      default: undefined
    },
    data: {
      type: Array,
      default: () => []
    },
    multiple: {
      type: Boolean,
      default: false
    },
    tagTooltip: {
      type: Object,
      default: undefined
    },
    reserveKeyword: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue'],
  template: '<div class="tree-select-stub" />'
})

const departmentOptions: DepartmentOptionLike[] = [
  { value: '100', code: '100', name: '总部' },
  { value: '110', code: '110', name: '财务部', parentValue: '100' },
  { value: '120', code: '120', name: '人事部', parentValue: '100' }
]

describe('DepartmentTreeSelect', () => {
  it('keeps the same tree data reference when multi-select values change within existing options', async () => {
    const wrapper = mount(DepartmentTreeSelect, {
      props: {
        modelValue: ['110'],
        options: departmentOptions,
        multiple: true,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const firstTreeData = wrapper.findComponent(TreeSelectStub).props('data')

    await wrapper.setProps({
      modelValue: ['110', '120']
    })

    const secondTreeData = wrapper.findComponent(TreeSelectStub).props('data')

    expect(secondTreeData).toBe(firstTreeData)
  })

  it('appends missing selected values so legacy data can still be displayed', () => {
    const wrapper = mount(DepartmentTreeSelect, {
      props: {
        modelValue: ['999'],
        options: departmentOptions,
        multiple: true,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const treeData = wrapper.findComponent(TreeSelectStub).props('data') as Array<{
      value: DepartmentTreeValue
      children: unknown[]
    }>

    expect(treeData.map((item) => item.value)).toContain('999')
  })

  it('keeps the native select fallback in test mode unless explicitly forced to render tree select', () => {
    const wrapper = mount(DepartmentTreeSelect, {
      props: {
        modelValue: '110',
        options: departmentOptions
      }
    })

    expect(wrapper.find('select').exists()).toBe(true)
    expect(wrapper.findComponent(TreeSelectStub).exists()).toBe(false)
  })

  it('forwards the global collapsed tag tooltip config to tree select by default', () => {
    const wrapper = mount(DepartmentTreeSelect, {
      props: {
        modelValue: ['110'],
        options: departmentOptions,
        multiple: true,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    expect(wrapper.findComponent(TreeSelectStub).props('tagTooltip')).toEqual(globalCollapseTagTooltipProps)
  })

  it('forwards the global filterable select behavior to tree select by default', () => {
    const wrapper = mount(DepartmentTreeSelect, {
      props: {
        modelValue: '110',
        options: departmentOptions,
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
