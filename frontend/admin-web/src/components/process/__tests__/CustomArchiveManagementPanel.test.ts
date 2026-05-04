import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomArchiveManagementPanel from '@/components/process/CustomArchiveManagementPanel.vue'

const mocks = vi.hoisted(() => ({
  processApi: {
    listCustomArchives: vi.fn(),
    getCustomArchiveMeta: vi.fn(),
    getCustomArchiveDetail: vi.fn(),
    createCustomArchive: vi.fn(),
    updateCustomArchive: vi.fn(),
    updateCustomArchiveStatus: vi.fn(),
    deleteCustomArchive: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  processApi: mocks.processApi
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage,
    ElMessageBox: mocks.elMessageBox
  }
})

vi.mock('@element-plus/icons-vue', async () => {
  const actual = await vi.importActual<typeof import('@element-plus/icons-vue')>('@element-plus/icons-vue')
  return {
    ...actual,
    Check: { template: '<span />' },
    Delete: { template: '<span />' },
    Plus: { template: '<span />' },
    Search: { template: '<span />' }
  }
})

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const InputStub = defineComponent({
  inheritAttrs: false,
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: `
    <input
      class="input-stub"
      :data-placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `
})

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number, Array],
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    multiple: {
      type: Boolean,
      default: false
    },
    allowCreate: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: `
    <div
      class="select-stub"
      data-testid="select-stub"
      :data-placeholder="placeholder"
      :data-multiple="String(multiple)"
      :data-allow-create="String(allowCreate)"
    >
      <slot />
    </div>
  `
})

const OptionStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    },
    value: {
      type: [String, Number],
      default: ''
    }
  },
  template: '<div class="option-stub" :data-label="label" :data-value="value"><slot /></div>'
})

const InputNumberStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number, Array, null],
      default: null
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<div data-testid="input-number-stub" :data-placeholder="placeholder" />'
})

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: [Boolean, Number],
      default: false
    }
  },
  emits: ['change', 'update:modelValue'],
  template: '<button type="button" @click="$emit(\'update:modelValue\', !modelValue); $emit(\'change\', !modelValue)" />'
})

const SegmentedStub = defineComponent({
  template: '<div data-testid="segmented-stub" />'
})

const EmptyStub = defineComponent({
  props: {
    description: {
      type: String,
      default: ''
    }
  },
  template: '<div>{{ description }}<slot /></div>'
})

const DialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
})

const DepartmentTreeSelectStub = defineComponent({
  props: {
    placeholder: {
      type: String,
      default: ''
    },
    multiple: {
      type: Boolean,
      default: false
    },
    options: {
      type: Array,
      default: () => []
    }
  },
  template: `
    <div
      data-testid="department-tree-select"
      :data-placeholder="placeholder"
      :data-multiple="String(multiple)"
      :data-option-count="options.length"
    />
  `
})

const FormItemStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    }
  },
  template: '<label><span>{{ label }}</span><slot /></label>'
})

const archiveSummary = {
  id: 1,
  archiveCode: 'CA202605040001',
  archiveName: '自动归档规则',
  archiveType: 'AUTO_RULE',
  archiveTypeLabel: '自动划分',
  archiveDescription: '规则档案',
  status: 1,
  itemCount: 1,
  updatedAt: '2026-05-04 09:00'
}

const archiveMeta = {
  archiveTypeOptions: [
    { label: '提供选择', value: 'SELECT' },
    { label: '自动划分', value: 'AUTO_RULE' }
  ],
  operatorOptions: [
    { key: 'EQ', label: '等于' },
    { key: 'IN', label: '属于' },
    { key: 'NOT_IN', label: '不属于' },
    { key: 'BETWEEN', label: '介于' },
    { key: 'CONTAINS', label: '包含' }
  ],
  ruleFields: [
    { key: 'submitterDeptId', label: '提单人部门（含下级）', valueType: 'department', operatorKeys: ['EQ', 'IN', 'NOT_IN'] },
    { key: 'submitterUserId', label: '提单人', valueType: 'user', operatorKeys: ['EQ', 'IN', 'NOT_IN'] },
    { key: 'paymentCompanyId', label: '公司抬头', valueType: 'company', operatorKeys: ['EQ', 'IN', 'NOT_IN'] },
    { key: 'expenseTypeCode', label: '费用类型', valueType: 'expenseType', operatorKeys: ['EQ', 'IN', 'NOT_IN'] },
    { key: 'tagArchiveCode', label: '标签档案', valueType: 'archive', operatorKeys: ['EQ', 'IN', 'NOT_IN'] },
    { key: 'actualPaymentAmount', label: '实际支付金额', valueType: 'number', operatorKeys: ['EQ'] },
    { key: 'amount', label: '金额', valueType: 'number', operatorKeys: ['BETWEEN'] },
    { key: 'submitterPosition', label: '提单人岗位', valueType: 'text', operatorKeys: ['EQ', 'IN', 'NOT_IN', 'CONTAINS'] }
  ],
  companyOptions: [{ label: 'A公司', value: 'COMPANY_A' }],
  departmentOptions: [{ label: '广州团队', value: '15' }],
  userOptions: [{ label: '张三', value: '101' }],
  expenseTypeOptions: [{ label: '差旅费', value: 'TRAVEL' }],
  archiveOptions: [{ label: '标签A', value: 'TAG_ARCHIVE_A' }],
  tagArchiveCode: 'PROCESS_TAG_OPTIONS',
  installmentArchiveCode: 'PROCESS_INSTALLMENT_OPTIONS'
}

const archiveDetail = {
  id: 1,
  archiveCode: 'CA202605040001',
  archiveName: '自动归档规则',
  archiveType: 'AUTO_RULE',
  archiveTypeLabel: '自动划分',
  archiveDescription: '规则档案',
  status: 1,
  items: [
    {
      id: 11,
      itemCode: 'CI202605040001',
      itemName: '自动匹配结果',
      priority: 1,
      status: 1,
      rules: [
        { id: 101, groupNo: 1, fieldKey: 'paymentCompanyId', operator: 'EQ', compareValue: 'COMPANY_A' },
        { id: 102, groupNo: 1, fieldKey: 'submitterUserId', operator: 'IN', compareValue: ['101'] },
        { id: 103, groupNo: 1, fieldKey: 'expenseTypeCode', operator: 'IN', compareValue: ['TRAVEL'] },
        { id: 104, groupNo: 1, fieldKey: 'tagArchiveCode', operator: 'EQ', compareValue: 'TAG_ARCHIVE_A' },
        { id: 105, groupNo: 1, fieldKey: 'submitterDeptId', operator: 'IN', compareValue: ['15'] },
        { id: 106, groupNo: 1, fieldKey: 'actualPaymentAmount', operator: 'EQ', compareValue: 88.5 },
        { id: 107, groupNo: 1, fieldKey: 'amount', operator: 'BETWEEN', compareValue: [100, 500] },
        { id: 108, groupNo: 1, fieldKey: 'submitterPosition', operator: 'IN', compareValue: ['经理'] }
      ]
    }
  ]
}

async function mountPanel() {
  mocks.processApi.getCustomArchiveMeta.mockResolvedValue({ code: 200, data: archiveMeta })
  mocks.processApi.listCustomArchives.mockResolvedValue({ code: 200, data: [archiveSummary] })
  mocks.processApi.getCustomArchiveDetail.mockResolvedValue({ code: 200, data: archiveDetail })
  mocks.processApi.updateCustomArchive.mockImplementation(async (_id: number, payload: any) => ({
    code: 200,
    data: {
      ...archiveDetail,
      ...payload,
      id: 1,
      archiveCode: archiveDetail.archiveCode,
      archiveTypeLabel: archiveDetail.archiveTypeLabel
    }
  }))
  mocks.processApi.updateCustomArchiveStatus.mockResolvedValue({ code: 200, data: true })
  mocks.processApi.deleteCustomArchive.mockResolvedValue({ code: 200, data: true })

  const wrapper = mount(CustomArchiveManagementPanel, {
    global: {
      stubs: {
        DepartmentTreeSelect: DepartmentTreeSelectStub,
        'department-tree-select': DepartmentTreeSelectStub,
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-dialog': DialogStub,
        'el-empty': EmptyStub,
        'el-form': SimpleContainer,
        'el-form-item': FormItemStub,
        'el-input': InputStub,
        'el-input-number': InputNumberStub,
        'el-option': OptionStub,
        'el-segmented': SegmentedStub,
        'el-select': SelectStub,
        'el-skeleton': SimpleContainer,
        'el-switch': SwitchStub,
        'el-tag': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })

  await flushPromises()
  return wrapper
}

describe('CustomArchiveManagementPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.elMessageBox.confirm.mockResolvedValue('confirm')
  })

  it('renders rule value controls by field type and keeps option-backed multi values closed to free-create', async () => {
    const wrapper = await mountPanel()

    expect(wrapper.find('[data-placeholder="请选择公司抬头"]').exists()).toBe(true)
    expect(wrapper.find('[data-placeholder="请选择多个提单人"]').attributes('data-allow-create')).toBe('false')
    expect(wrapper.find('[data-placeholder="请选择多个费用类型"]').attributes('data-allow-create')).toBe('false')
    expect(wrapper.find('[data-placeholder="请选择标签档案"]').exists()).toBe(true)
    expect(wrapper.find('[data-placeholder="请选择多个提单人部门（含下级）"]').attributes('data-option-count')).toBe('1')
    expect(wrapper.find('[data-placeholder="请输入数值"]').exists()).toBe(true)
    expect(wrapper.find('[data-placeholder="请输入多个提单人岗位"]').attributes('data-allow-create')).toBe('true')
  })

  it('saves loaded auto-rule values without losing legacy keys or compare values', async () => {
    const wrapper = await mountPanel()

    await wrapper.findAll('button').find((item) => item.text() === '保存')!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateCustomArchive).toHaveBeenCalledTimes(1)
    expect(mocks.processApi.updateCustomArchive).toHaveBeenCalledWith(1, expect.objectContaining({
      archiveName: '自动归档规则',
      archiveType: 'AUTO_RULE'
    }))

    const payload = mocks.processApi.updateCustomArchive.mock.calls[0][1]
    const rules = payload.items[0].rules
    expect(rules).toEqual(expect.arrayContaining([
      expect.objectContaining({ fieldKey: 'submitterDeptId', operator: 'IN', compareValue: ['15'] }),
      expect.objectContaining({ fieldKey: 'paymentCompanyId', operator: 'EQ', compareValue: 'COMPANY_A' }),
      expect.objectContaining({ fieldKey: 'submitterUserId', operator: 'IN', compareValue: ['101'] }),
      expect.objectContaining({ fieldKey: 'expenseTypeCode', operator: 'IN', compareValue: ['TRAVEL'] }),
      expect.objectContaining({ fieldKey: 'tagArchiveCode', operator: 'EQ', compareValue: 'TAG_ARCHIVE_A' }),
      expect.objectContaining({ fieldKey: 'actualPaymentAmount', operator: 'EQ', compareValue: 88.5 }),
      expect.objectContaining({ fieldKey: 'amount', operator: 'BETWEEN', compareValue: [100, 500] }),
      expect.objectContaining({ fieldKey: 'submitterPosition', operator: 'IN', compareValue: ['经理'] })
    ]))
  })
})
