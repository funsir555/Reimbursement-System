import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { archiveAgentApi, type ArchiveAgentDetail, type ArchiveAgentWorkflowEdge } from '@/api'
import AgentWorkflowCanvasView from '../AgentWorkflowCanvasView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  useRouter: () => ({ push: vi.fn() })
}))
vi.mock('@/api', () => ({
  archiveAgentApi: { getDetail: vi.fn(), getMeta: vi.fn(), update: vi.fn() }
}))

const detail: ArchiveAgentDetail = {
  id: 1, agentCode: 'A1', agentName: '测试流程', tags: [], status: 'DRAFT',
  promptConfig: {}, modelConfig: {}, tools: [], triggers: [], inputSchema: {}, versions: [],
  workflow: {
    nodes: [
      { nodeKey: 'start', nodeType: 'start', label: '开始', config: { canvasPosition: { x: 80, y: 100 } } },
      { nodeKey: 'first', nodeType: 'transform', label: '第一步', config: { canvasPosition: { x: 350, y: 100 } } },
      { nodeKey: 'second', nodeType: 'transform', label: '第二步', config: { canvasPosition: { x: 620, y: 100 } } },
      { nodeKey: 'end', nodeType: 'end', label: '结束', config: { canvasPosition: { x: 890, y: 100 } } }
    ],
    edges: [
      { source: 'start', target: 'first' },
      { source: 'first', target: 'second' },
      { source: 'second', target: 'end' }
    ]
  }
}

const elementFromPoint = vi.fn<(...args: number[]) => Element | null>()
const originalElementFromPoint = Object.getOwnPropertyDescriptor(document, 'elementFromPoint')

async function mountCanvas(extraEdges: ArchiveAgentWorkflowEdge[] = []) {
  const data = structuredClone(detail)
  data.workflow.edges.push(...extraEdges)
  vi.mocked(archiveAgentApi.getDetail).mockResolvedValue({ data } as Awaited<ReturnType<typeof archiveAgentApi.getDetail>>)
  vi.mocked(archiveAgentApi.getMeta).mockResolvedValue({ data: { tools: [] } } as Awaited<ReturnType<typeof archiveAgentApi.getMeta>>)
  const wrapper = shallowMount(AgentWorkflowCanvasView, {
    global: {
      renderStubDefaultSlot: true,
      stubs: { ElTree: { template: '<div />' } },
      directives: { loading: () => {} }
    }
  })
  await flushPromises()
  return wrapper
}

function releaseOn(element: Element | null, x = 180, y = 150) {
  elementFromPoint.mockReturnValue(element)
  window.dispatchEvent(new MouseEvent('mouseup', { clientX: x, clientY: y }))
}

async function reconnect(wrapper: Awaited<ReturnType<typeof mountCanvas>>, source: string, oldTarget: string, newTarget: string) {
  await wrapper.find(`button[aria-label="拖动改接 ${source} 到 ${oldTarget} 的连线"]`)
    .trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
  window.dispatchEvent(new MouseEvent('mousemove', { clientX: 180, clientY: 150 }))
  releaseOn(wrapper.find(`[data-node-key="${newTarget}"]`).element)
  await nextTick()
}

describe('AgentWorkflowCanvasView arrow reconnect', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(document, 'elementFromPoint', { configurable: true, value: elementFromPoint })
  })
  afterEach(() => {
    if (originalElementFromPoint) Object.defineProperty(document, 'elementFromPoint', originalElementFromPoint)
    else Reflect.deleteProperty(document, 'elementFromPoint')
  })

  it('selects on click and changes only the target after dragging to a node', async () => {
    const wrapper = await mountCanvas()
    const arrow = wrapper.find('button[aria-label="拖动改接 start 到 first 的连线"]')
    const second = wrapper.find('[data-node-key="second"]').element

    await arrow.trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
    releaseOn(second, 100, 100)
    await nextTick()
    expect(wrapper.find('button[aria-label="拖动改接 start 到 first 的连线"]').classes()).toContain('selected')
    expect(wrapper.findAll('button.edge-arrow-handle')).toHaveLength(3)

    await arrow.trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
    window.dispatchEvent(new MouseEvent('mousemove', { clientX: 180, clientY: 150 }))
    await nextTick()
    expect(wrapper.find('path.edge-path.temp').exists()).toBe(true)
    releaseOn(second)
    await nextTick()

    expect(wrapper.find('button[aria-label="拖动改接 start 到 first 的连线"]').exists()).toBe(false)
    expect(wrapper.find('button[aria-label="拖动改接 start 到 second 的连线"]').classes()).toContain('selected')
    expect(wrapper.findAll('button.edge-arrow-handle')).toHaveLength(3)
    expect(wrapper.text()).toContain('存在未从开始节点连通的节点')
    wrapper.unmount()
  })

  it('keeps the original edge on blank, source, duplicate, or Escape', async () => {
    const wrapper = await mountCanvas([{ source: 'start', target: 'second' }])
    const arrow = wrapper.find('button[aria-label="拖动改接 start 到 first 的连线"]')
    const start = wrapper.find('[data-node-key="start"]').element
    const second = wrapper.find('[data-node-key="second"]').element

    await arrow.trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
    window.dispatchEvent(new MouseEvent('mousemove', { clientX: 180, clientY: 150 }))
    releaseOn(null)
    await nextTick()
    expect(arrow.exists()).toBe(true)

    await arrow.trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
    window.dispatchEvent(new MouseEvent('mousemove', { clientX: 180, clientY: 150 }))
    releaseOn(start)
    await nextTick()
    expect(arrow.exists()).toBe(true)

    await arrow.trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
    window.dispatchEvent(new MouseEvent('mousemove', { clientX: 180, clientY: 150 }))
    releaseOn(second)
    await nextTick()
    expect(arrow.exists()).toBe(true)

    await arrow.trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
    window.dispatchEvent(new MouseEvent('mousemove', { clientX: 180, clientY: 150 }))
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    releaseOn(second)
    await nextTick()
    expect(arrow.exists()).toBe(true)
    expect(wrapper.findAll('button.edge-arrow-handle')).toHaveLength(4)
    wrapper.unmount()
  })

  it('saves a valid chain after several arrow reconnects', async () => {
    const wrapper = await mountCanvas()
    await reconnect(wrapper, 'start', 'first', 'second')
    await reconnect(wrapper, 'first', 'second', 'end')
    await reconnect(wrapper, 'second', 'end', 'first')
    expect(wrapper.text()).toContain('流程结构正常，可以保存。')

    vi.mocked(archiveAgentApi.update).mockImplementation(async (_id, payload) => ({
      data: { ...structuredClone(detail), workflow: payload.workflow }
    } as Awaited<ReturnType<typeof archiveAgentApi.update>>))
    const saveButton = wrapper.findAll('el-button-stub').find(button => button.text().includes('保存画布'))
    expect(saveButton).toBeDefined()
    await saveButton!.trigger('click')
    await flushPromises()
    const savedEdges = vi.mocked(archiveAgentApi.update).mock.calls[0]?.[1].workflow.edges
    expect(savedEdges).toEqual([
      { source: 'start', target: 'second' },
      { source: 'first', target: 'end' },
      { source: 'second', target: 'first' }
    ])
    vi.mocked(archiveAgentApi.getDetail).mockResolvedValue({
      data: { ...structuredClone(detail), workflow: { ...detail.workflow, edges: savedEdges! } }
    } as Awaited<ReturnType<typeof archiveAgentApi.getDetail>>)
    await wrapper.findAll('el-button-stub').find(button => button.text().includes('刷新'))!.trigger('click')
    await flushPromises()
    expect(wrapper.find('button[aria-label="拖动改接 start 到 second 的连线"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('preserves output-handle creation and arrow context actions', async () => {
    const wrapper = await mountCanvas()
    await wrapper.find('[data-node-key="first"] .handle.output')
      .trigger('mousedown', { button: 0, clientX: 100, clientY: 100 })
    window.dispatchEvent(new MouseEvent('mousemove', { clientX: 180, clientY: 150 }))
    releaseOn(wrapper.find('[data-node-key="end"]').element)
    await nextTick()
    expect(wrapper.find('button[aria-label="拖动改接 first 到 end 的连线"]').exists()).toBe(true)

    await wrapper.find('button[aria-label="拖动改接 first 到 end 的连线"]')
      .trigger('contextmenu', { clientX: 180, clientY: 150 })
    await wrapper.find('.edge-context-menu button').trigger('click')
    expect(wrapper.find('button[aria-label="拖动改接 first 到 end 的连线"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
