import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import NotificationCenterDrawer from '@/components/NotificationCenterDrawer.vue'

const mocks = vi.hoisted(() => ({
  notificationApi: {
    list: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn()
  },
  elMessage: {
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  notificationApi: mocks.notificationApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

const DrawerStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  template: `
    <div data-testid="drawer" :data-visible="modelValue ? 'true' : 'false'">
      <h2>{{ title }}</h2>
      <slot />
    </div>
  `
})

const ButtonStub = defineComponent({
  inheritAttrs: false,
  emits: ['click'],
  template: '<button type="button" v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>'
})

const EmptyStub = defineComponent({
  props: {
    description: {
      type: String,
      default: ''
    }
  },
  template: '<div data-testid="empty">{{ description }}</div>'
})

async function mountView() {
  const wrapper = mount(NotificationCenterDrawer, {
    props: {
      modelValue: true
    },
    global: {
      stubs: {
        'el-drawer': DrawerStub,
        'el-button': ButtonStub,
        'el-empty': EmptyStub
      },
      directives: {
        loading: {}
      }
    }
  })

  await flushPromises()
  return wrapper
}

describe('NotificationCenterDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.notificationApi.list.mockResolvedValue({
      code: 200,
      data: [
        {
          id: 2,
          title: '第二条通知',
          content: '第二条内容',
          status: 'UNREAD',
          createdAt: '2026-04-06 11:00:00',
          readAt: ''
        },
        {
          id: 1,
          title: '第一条通知',
          content: '第一条内容',
          status: 'READ',
          createdAt: '2026-04-06 10:00:00',
          readAt: '2026-04-06 10:30:00'
        }
      ]
    })
    mocks.notificationApi.markRead.mockResolvedValue({ code: 200, data: true })
    mocks.notificationApi.markAllRead.mockResolvedValue({ code: 200, data: true })
  })

  it('renders notifications in descending order and highlights unread items', async () => {
    const wrapper = await mountView()

    const items = wrapper.findAll('[data-testid^="notification-item-"]')
    expect(items).toHaveLength(2)
    expect(items[0].text()).toContain('第二条通知')
    expect(items[1].text()).toContain('第一条通知')
    expect(wrapper.findAll('[data-testid="notification-unread-dot"]')).toHaveLength(1)
  })

  it('marks a single unread notification as read without any redirect', async () => {
    const wrapper = await mountView()

    await wrapper.find('[data-testid="notification-item-2"]').trigger('click')
    await flushPromises()

    expect(mocks.notificationApi.markRead).toHaveBeenCalledWith(2)
    expect(wrapper.emitted('changed')).toHaveLength(1)
    expect(wrapper.find('[data-testid="notification-item-2"]').text()).toContain('已读')
  })

  it('marks all notifications as read', async () => {
    const wrapper = await mountView()

    await wrapper.find('[data-testid="mark-all-read"]').trigger('click')
    await flushPromises()

    expect(mocks.notificationApi.markAllRead).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('changed')).toHaveLength(1)
    expect(wrapper.text()).not.toContain('未读')
  })

  it('shows empty state when there are no notifications', async () => {
    mocks.notificationApi.list.mockResolvedValue({
      code: 200,
      data: []
    })

    const wrapper = await mountView()

    expect(wrapper.find('[data-testid="empty"]').text()).toContain('暂无消息通知')
  })
})
