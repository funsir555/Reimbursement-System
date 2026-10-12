import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RoleDataScopePanel from '@/views/settings/components/RoleDataScopePanel.vue'
import type { CompanyRecord, RoleRecord } from '@/api'

const mocks = vi.hoisted(() => ({
  getRoleDataScopes: vi.fn(),
  saveRoleDataScopes: vi.fn(),
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn()
}))

vi.mock('@/api/modules/system-settings', () => ({
  systemSettingsApi: {
    getRoleDataScopes: mocks.getRoleDataScopes,
    saveRoleDataScopes: mocks.saveRoleDataScopes
  }
}))
vi.mock('element-plus', async () => ({
  ...(await vi.importActual<typeof import('element-plus')>('element-plus')),
  ElMessage: { success: mocks.success, error: mocks.error, warning: mocks.warning }
}))

const Card = defineComponent({ template: '<div><slot name="header"/><slot/></div>' })
const Button = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot/></button>'
})
const Plain = defineComponent({ template: '<div><slot/></div>' })
const Alert = defineComponent({ props: { title: String }, template: '<div>{{ title }}</div>' })
const role = { id: 7, roleCode: 'finance_reader', permissionCodes: ['finance:period_close:query'] } as RoleRecord
const companies = [{ companyId: 'B', companyName: '乙公司', status: 1 }] as CompanyRecord[]

function render(selectedRole: RoleRecord = role) {
  return mount(RoleDataScopePanel, {
    props: { role: selectedRole, companies, canAssign: true },
    global: {
      stubs: {
        'el-card': Card, 'el-button': Button, 'el-select': Plain,
        'el-option': Plain, 'el-empty': Plain, 'el-alert': Alert
      }
    }
  })
}

describe('RoleDataScopePanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getRoleDataScopes.mockResolvedValue({ data: [
      { permissionCode: 'finance:period_close:query', capabilityName: '结账查询',
        supportedScopes: ['SELECTED_COMPANIES', 'ALL_COMPANIES'], scopeType: 'SELECTED_COMPANIES', companyIds: ['B'] },
      { permissionCode: 'expense:agent:query', capabilityName: '报销查询',
        supportedScopes: ['SELF'], scopeType: null, companyIds: [] }
    ] })
    mocks.saveRoleDataScopes.mockResolvedValue({ data: [] })
  })

  it('saves only configured capabilities with their own company range', async () => {
    const wrapper = render()
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text().includes('保存范围'))!.trigger('click')
    await flushPromises()

    expect(mocks.saveRoleDataScopes).toHaveBeenCalledWith(7, [{
      permissionCode: 'finance:period_close:query', scopeType: 'SELECTED_COMPANIES', companyIds: ['B']
    }])
  })

  it('shows the super administrator range as fixed and read only', async () => {
    const wrapper = render({ ...role, roleCode: 'SUPER_ADMIN' } as RoleRecord)
    await flushPromises()

    expect(wrapper.text()).toContain('超级管理员固定可查看当前租户的全部公司')
    expect(wrapper.findAll('button').some(button => button.text().includes('保存范围'))).toBe(false)
  })
})
