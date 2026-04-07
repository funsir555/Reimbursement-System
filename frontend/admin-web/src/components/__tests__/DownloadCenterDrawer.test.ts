import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DownloadCenterDrawer from '@/components/DownloadCenterDrawer.vue'

const mocks = vi.hoisted(() => ({
  downloadApi: {
    getCenter: vi.fn(),
    downloadFile: vi.fn()
  },
  elMessage: {
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  downloadApi: mocks.downloadApi
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

const TagStub = defineComponent({
  template: '<span><slot /></span>'
})

const ProgressStub = defineComponent({
  props: {
    percentage: {
      type: Number,
      default: 0
    }
  },
  template: '<div :data-percentage="percentage" />'
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
  const wrapper = mount(DownloadCenterDrawer, {
    props: {
      modelValue: true
    },
    global: {
      stubs: {
        'el-drawer': DrawerStub,
        'el-button': ButtonStub,
        'el-tag': TagStub,
        'el-progress': ProgressStub,
        'el-empty': EmptyStub
      }
    }
  })

  await flushPromises()
  return wrapper
}

describe('DownloadCenterDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.downloadApi.getCenter.mockResolvedValue({
      code: 200,
      data: {
        inProgress: [
          {
            id: 1,
            fileName: '导出文件.xlsx',
            businessType: '导出',
            fileSize: '1MB',
            status: '处理中',
            progress: 35,
            createdAt: '2026-04-07 09:00:00'
          }
        ],
        history: [
          {
            id: 2,
            fileName: '历史文件.xlsx',
            businessType: '导出',
            fileSize: '2MB',
            status: '完成',
            createdAt: '2026-04-07 08:00:00',
            finishedAt: '2026-04-07 08:05:00',
            downloadable: true
          }
        ]
      }
    })
  })

  it('does not render the removed static summary cards and still shows both download sections', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).not.toContain('进行中')
    expect(wrapper.text()).not.toContain('历史记录')
    expect(wrapper.text()).toContain('正在下载')
    expect(wrapper.text()).toContain('下载记录')
    expect(wrapper.text()).toContain('导出文件.xlsx')
    expect(wrapper.text()).toContain('历史文件.xlsx')
  })

  it('still renders empty states when there is no data', async () => {
    mocks.downloadApi.getCenter.mockResolvedValue({
      code: 200,
      data: {
        inProgress: [],
        history: []
      }
    })

    const wrapper = await mountView()

    expect(wrapper.findAll('[data-testid="empty"]')).toHaveLength(2)
  })
})
