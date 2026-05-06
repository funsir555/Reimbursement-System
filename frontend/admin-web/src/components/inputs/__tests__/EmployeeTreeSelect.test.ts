import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import EmployeeTreeSelect from '@/components/inputs/EmployeeTreeSelect.vue'
import { globalCollapseTagTooltipProps } from '@/utils/collapseTagTooltip'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import type { EmployeeDirectoryEntry } from '@/api'
import type { EmployeeTreeDepartmentLike, EmployeeTreeNode, EmployeeTreeValue } from '@/utils/employeeTree'

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

const departmentOptions: EmployeeTreeDepartmentLike[] = [
  { value: '100', code: '100', name: '总部' },
  { value: '110', code: '110', name: '财务部', parentValue: '100' },
  { value: '120', code: '120', name: '行政部', parentValue: '100' }
]

const employees: EmployeeDirectoryEntry[] = [
  {
    userId: 1,
    name: '张三',
    username: 'zhangsan',
    deptId: 110,
    deptName: '财务部',
    departments: [{ deptId: 110, deptName: '财务部' }],
    status: 1
  },
  {
    userId: 2,
    name: '李四',
    username: 'lisi',
    deptId: 120,
    deptName: '行政部',
    departments: [{ deptId: 120, deptName: '行政部' }],
    status: 1
  }
]

describe('EmployeeTreeSelect', () => {
  it('keeps the same tree data reference when multi-select values change within existing options', async () => {
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: ['1'],
        departments: departmentOptions,
        employees,
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
      modelValue: ['1', '2']
    })

    const secondTreeData = wrapper.findComponent(TreeSelectStub).props('data')

    expect(secondTreeData).toBe(firstTreeData)
  })

  it('marks department nodes as non-selectable and appends employee children', () => {
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: '1',
        departments: departmentOptions,
        employees,
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const treeData = wrapper.findComponent(TreeSelectStub).props('data') as EmployeeTreeNode[]
    const financeDepartment = treeData
      .flatMap((item) => [item, ...item.children])
      .find((item) => item.label.includes('财务部'))

    expect(financeDepartment?.disabled).toBe(true)
    expect(financeDepartment?.children.some((item) => item.value === '1')).toBe(true)
  })

  it('appends missing selected values so legacy data can still be displayed', () => {
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: ['999'],
        departments: departmentOptions,
        employees,
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
      children: Array<{ value: EmployeeTreeValue }>
    }>

    expect(treeData.some((item) => item.children.some((child) => child.value === '999'))).toBe(true)
  })

  it('supports raw extra options like submitter', () => {
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: ['SUBMITTER'],
        departments: departmentOptions,
        employees,
        extraOptions: [{ label: '提单人', value: 'SUBMITTER' }],
        multiple: true,
        valueType: 'raw',
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const treeData = wrapper.findComponent(TreeSelectStub).props('data') as EmployeeTreeNode[]
    const extraGroup = treeData.find((item) => item.label === '系统内置')

    expect(extraGroup?.children.some((item) => item.value === 'SUBMITTER')).toBe(true)
  })

  it('keeps the native select fallback in test mode unless explicitly forced to render tree select', () => {
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: '1',
        departments: departmentOptions,
        employees
      }
    })

    expect(wrapper.find('select').exists()).toBe(true)
    expect(wrapper.findComponent(TreeSelectStub).exists()).toBe(false)
  })

  it('forwards the global collapsed tag tooltip config to tree select by default', () => {
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: ['1'],
        departments: departmentOptions,
        employees,
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
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: '1',
        departments: departmentOptions,
        employees,
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

  it('calls remoteMethod and refreshes grouped options in remote mode', async () => {
    const remoteMethod = vi.fn().mockResolvedValue([
      {
        userId: 3,
        name: '王五',
        username: 'wangwu',
        deptId: 110,
        deptName: '财务部',
        departments: [{ deptId: 110, deptName: '财务部' }],
        status: 1
      }
    ] satisfies EmployeeDirectoryEntry[])

    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: undefined,
        departments: departmentOptions,
        employees: [],
        remote: true,
        remoteMethod
      },
      global: {
        stubs: {
          'el-select': defineComponent({
            props: {
              modelValue: {
                type: [String, Number, Array],
                default: undefined
              }
            },
            template: '<div class="remote-select-stub"><slot /></div>'
          }),
          'el-option-group': defineComponent({
            props: { label: { type: String, default: '' } },
            template: '<div class="option-group-stub"><slot /></div>'
          }),
          'el-option': defineComponent({
            props: { label: { type: String, default: '' }, value: { type: [String, Number], default: '' } },
            template: '<div class="option-stub">{{ label }}</div>'
          })
        }
      }
    })

    await (wrapper.vm as unknown as { handleRemoteSearch: (keyword: string) => Promise<void> }).handleRemoteSearch('王')

    expect(remoteMethod).toHaveBeenCalledWith('王')
    expect(wrapper.text()).toContain('王五')
  })

  it('renders username plus name in finance-assist label mode', () => {
    const wrapper = mount(EmployeeTreeSelect, {
      props: {
        modelValue: '1',
        departments: departmentOptions,
        employees,
        labelMode: 'finance-assist',
        forceTreeSelect: true
      },
      global: {
        stubs: {
          'el-tree-select': TreeSelectStub
        }
      }
    })

    const treeData = wrapper.findComponent(TreeSelectStub).props('data') as EmployeeTreeNode[]
    const employeeNode = treeData
      .flatMap((item) => item.children)
      .flatMap((item) => item.children)
      .find((item) => item.value === '1')

    expect(employeeNode?.label).toBe('zhangsan  张三')
    expect(employeeNode?.keywords).toContain('zhangsan')
    expect(employeeNode?.keywords).toContain('张三')
  })
})
