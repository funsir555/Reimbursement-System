import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProcessFormDesignerView from '@/views/process/ProcessFormDesignerView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    name: 'expense-workbench-process-form-create',
    params: {},
    query: { templateType: 'report' }
  },
  router: {
    push: vi.fn(),
    replace: vi.fn()
  },
  processApi: {
    getFlowMeta: vi.fn(),
    listCustomArchives: vi.fn(),
    getCustomArchiveDetail: vi.fn(),
    getFormDesignDetail: vi.fn(),
    createFormDesign: vi.fn(),
    updateFormDesign: vi.fn(),
    getExpenseDetailDesignDetail: vi.fn(),
    createExpenseDetailDesign: vi.fn(),
    updateExpenseDetailDesign: vi.fn()
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

mocks.route = reactive({
  name: 'expense-workbench-process-form-create',
  params: {},
  query: { templateType: 'report' }
})

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  processApi: mocks.processApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@/utils/permissions', () => ({
  readStoredUser: () => ({
    permissionCodes: [
      'expense:process_management:view',
      'expense:process_management:create',
      'expense:process_management:edit'
    ]
  }),
  hasPermission: (permissionCode: string, source?: { permissionCodes?: string[] } | string[] | null) => {
    const ownedCodes = Array.isArray(source)
      ? source
      : source?.permissionCodes || [
        'expense:process_management:view',
        'expense:process_management:create',
        'expense:process_management:edit'
      ]
    return ownedCodes.includes(permissionCode)
  }
}))

vi.mock('@element-plus/icons-vue', () => ({
  ArrowLeft: { template: '<span />' },
  Check: { template: '<span />' },
  Delete: { template: '<span />' },
  RefreshRight: { template: '<span />' }
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
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

const TagStub = defineComponent({
  template: '<span data-testid="tag"><slot /></span>'
})

const EmptyStub = defineComponent({
  props: {
    description: {
      type: String,
      default: ''
    }
  },
  template: '<div>{{ description }}</div>'
})

const TooltipStub = defineComponent({
  template: '<div><slot /></div>'
})

function buildFlowMeta() {
  return {
    sceneOptions: [],
    departmentOptions: []
  }
}

function buildFormDesignDetail(overrides: Record<string, unknown> = {}) {
  return {
    id: 8,
    formCode: 'FORM-001',
    formName: '差旅报销表单',
    templateType: 'report',
    templateTypeLabel: '报销单',
    formDescription: 'description',
    updatedAt: '2026-04-05 10:00',
    schema: {
      blocks: []
    },
    ...overrides
  }
}

function buildExpenseDetailDesignDetail(overrides: Record<string, unknown> = {}) {
  return {
    id: 9,
    detailCode: 'DETAIL-001',
    detailName: '交通明细表单',
    detailDescription: 'detail description',
    detailType: 'NORMAL_REIMBURSEMENT',
    detailTypeLabel: '普通报销',
    updatedAt: '2026-04-05 10:00',
    schema: {
      blocks: []
    },
    ...overrides
  }
}

async function mountView() {
  mocks.processApi.getFlowMeta.mockResolvedValue({ data: buildFlowMeta() })
  mocks.processApi.listCustomArchives.mockResolvedValue({ data: [] })
  mocks.processApi.getFormDesignDetail.mockResolvedValue({ data: buildFormDesignDetail() })
  mocks.processApi.getExpenseDetailDesignDetail.mockResolvedValue({ data: buildExpenseDetailDesignDetail() })

  const wrapper = mount(ProcessFormDesignerView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-form-item': FormItemStub,
        'el-tag': TagStub,
        'el-icon': SimpleContainer,
        'el-empty': EmptyStub,
        'el-tooltip': TooltipStub,
        'el-tabs': SimpleContainer,
        'el-tab-pane': SimpleContainer,
        'el-switch': true,
        'el-select': SimpleContainer,
        'el-option': true,
        'el-input-number': true,
        'el-checkbox': true,
        'el-checkbox-group': SimpleContainer,
        'el-radio-group': SimpleContainer,
        'el-radio-button': SimpleContainer,
        'el-dialog': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })

  await flushPromises()
  mountedWrappers.push(wrapper)
  return wrapper
}

const mountedWrappers: Array<{ unmount: () => void }> = []

describe('ProcessFormDesignerView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.name = 'expense-workbench-process-form-create'
    mocks.route.params = {}
    mocks.route.query = { templateType: 'report' }
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
  })

  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop()?.unmount()
    }
  })

  it('moves save actions into the floating footer and keeps only the tag in the header actions', async () => {
    const wrapper = await mountView()
    const headerActions = wrapper.get('[data-testid="process-form-designer-header-actions"]')
    const floatingBar = wrapper.get('[data-testid="process-form-designer-floating-bar"]')
    const floatingInner = wrapper.get('[data-testid="process-form-designer-floating-bar-inner"]')

    expect(headerActions.text()).toContain('报销单')
    expect(headerActions.text()).not.toContain('保存表单设计')
    expect(headerActions.text()).not.toContain('保存费用明细表单')

    expect(floatingBar.classes()).toContain('process-form-designer-floating-bar')
    expect(floatingInner.classes()).toContain('process-form-designer-floating-bar__inner')
    expect(floatingBar.text()).toContain('保存草稿')
    expect(floatingBar.text()).toContain('保存表单设计')

    const footerButtons = wrapper.findAll('button').filter((item) => (
      item.text().includes('保存草稿') || item.text().includes('保存表单设计')
    ))
    expect(footerButtons).toHaveLength(2)
    expect(footerButtons.every((item) => item.classes().includes('process-form-designer-floating-bar__button'))).toBe(true)
    expect(footerButtons.some((item) => item.classes().includes('process-form-designer-floating-bar__button--success'))).toBe(true)
  })

  it('saves draft through the existing create api and redirects to edit mode in create mode', async () => {
    mocks.processApi.createFormDesign.mockResolvedValue({
      data: buildFormDesignDetail({ id: 18, formName: '差旅草稿表单' })
    })

    const wrapper = await mountView()

    await wrapper.find('input').setValue('差旅草稿表单')
    await wrapper.findAll('button').find((item) => item.text().includes('保存草稿'))!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.createFormDesign).toHaveBeenCalledWith(expect.objectContaining({
      templateType: 'report',
      formName: '差旅草稿表单'
    }))
    expect(mocks.elMessage.success).toHaveBeenCalledWith('表单草稿已保存')
    expect(mocks.router.replace).toHaveBeenCalledWith({
      name: 'expense-workbench-process-form-edit',
      params: { id: 18 },
      query: mocks.route.query
    })
  })

  it('adapts the final save label for expense detail designers and reuses the update api', async () => {
    mocks.route.name = 'expense-workbench-process-expense-detail-edit'
    mocks.route.params = { id: '9' }
    mocks.processApi.updateExpenseDetailDesign.mockResolvedValue({
      data: buildExpenseDetailDesignDetail({ id: 9, detailName: '交通明细表单' })
    })

    const wrapper = await mountView()
    const floatingBar = wrapper.get('[data-testid="process-form-designer-floating-bar"]')

    expect(floatingBar.text()).toContain('保存草稿')
    expect(floatingBar.text()).toContain('保存费用明细表单')

    await wrapper.findAll('button').find((item) => item.text().includes('保存费用明细表单'))!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateExpenseDetailDesign).toHaveBeenCalledWith(9, expect.objectContaining({
      detailName: '交通明细表单',
      detailType: 'NORMAL_REIMBURSEMENT'
    }))
    expect(mocks.elMessage.success).toHaveBeenCalledWith('费用明细表单已更新')
  })
})
