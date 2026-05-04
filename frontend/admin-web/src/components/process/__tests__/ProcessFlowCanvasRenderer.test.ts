import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import type { ProcessFlowNode, ProcessFlowRoute } from '@/api'
import type { FlowCanvasBlock, FlowCanvasInsertTarget } from '@/views/process/processFlowDesignerHelper'
import ProcessFlowCanvasRenderer from '@/components/process/ProcessFlowCanvasRenderer.vue'

const DropdownStub = defineComponent({
  emits: ['command'],
  template: `
    <div class="dropdown-stub">
      <slot />
      <div class="dropdown-stub__menu">
        <slot name="dropdown" />
      </div>
    </div>
  `
})

const DropdownMenuStub = defineComponent({
  template: '<div class="dropdown-menu-stub"><slot /></div>'
})

const DropdownItemStub = defineComponent({
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    divided: {
      type: Boolean,
      default: false
    }
  },
  template: '<div class="dropdown-item-stub" :data-disabled="disabled" :data-divided="divided"><slot /></div>'
})

const TagStub = defineComponent({
  template: '<span class="tag-stub"><slot /></span>'
})

function createBranchNode(nodeKey: string, nodeName: string): ProcessFlowNode {
  return {
    nodeKey,
    nodeName,
    nodeType: 'BRANCH',
    displayOrder: 1,
    config: {}
  }
}

function createApprovalNode(nodeKey: string, nodeName: string): ProcessFlowNode {
  return {
    nodeKey,
    nodeName,
    nodeType: 'APPROVAL',
    displayOrder: 1,
    config: {}
  }
}

function createRoute(
  routeKey: string,
  sourceNodeKey: string,
  routeName: string,
  priority: number,
  attachBelowNodes = false
): ProcessFlowRoute {
  return {
    routeKey,
    sourceNodeKey,
    routeName,
    priority,
    attachBelowNodes,
    defaultRoute: false,
    conditionGroups: []
  }
}

function createInsertTargets(containerKey: string, index: number): FlowCanvasInsertTarget[] {
  return [
    {
      key: `current-${containerKey}-${index}`,
      label: '插入当前分支',
      containerKey,
      index
    },
    {
      key: `tail-${containerKey}-${index}`,
      label: '插入附带下方节点',
      containerKey: null,
      index
    }
  ]
}

function buildBlocks(): FlowCanvasBlock[] {
  const rootNode = createBranchNode('branch-root', 'Root branch')
  const nestedNode = createBranchNode('branch-nested', 'Nested branch')
  const reviewNode = createApprovalNode('approval-lane', 'Lane approval node')
  const rootRoutes = [
    createRoute('route-root-a', 'branch-root', 'Branch A', 1, true),
    createRoute('route-root-b', 'branch-root', 'Branch B', 2)
  ]
  const nestedRoutes = [
    createRoute('route-nested-a', 'branch-nested', 'Nested A', 1),
    createRoute('route-nested-b', 'branch-nested', 'Nested B', 2)
  ]

  return [
    {
      key: 'branch-branch-root',
      kind: 'branch',
      node: rootNode,
      depth: 0,
      compact: false,
      symmetric: true,
      postMergeInsert: {
        key: 'insert-root-1-post-merge',
        kind: 'insert',
        containerKey: null,
        index: 1,
        depth: 0,
        placement: 'post-merge'
      },
      routes: [
        {
          route: rootRoutes[0],
          blocks: [
            {
              key: 'insert-merged-route-root-a',
              kind: 'insert',
              containerKey: 'route-root-a',
              index: 0,
              depth: 1,
              targets: createInsertTargets('route-root-a', 0)
            },
            {
              key: 'node-approval-lane',
              kind: 'node',
              node: reviewNode,
              depth: 1
            },
            {
              key: 'branch-branch-nested',
              kind: 'branch',
              node: nestedNode,
              depth: 1,
              compact: true,
              symmetric: true,
              routes: nestedRoutes.map((route) => ({
                route,
                blocks: [
                  {
                    key: `insert-${route.routeKey}-0`,
                    kind: 'insert',
                    containerKey: route.routeKey,
                    index: 0,
                    depth: 2
                  }
                ]
              }))
            }
          ]
        },
        {
          route: rootRoutes[1],
          blocks: [
            {
              key: 'insert-route-root-b-0',
              kind: 'insert',
              containerKey: 'route-root-b',
              index: 0,
              depth: 1
            }
          ]
        }
      ]
    }
  ]
}

describe('ProcessFlowCanvasRenderer', () => {
  it('expands nested compact branches through the parent lane while keeping branch elements aligned', async () => {
    const wrapper = mount(ProcessFlowCanvasRenderer, {
      props: {
        blocks: buildBlocks(),
        selectedNodeKey: '',
        selectedRouteKey: 'route-root-a',
        sceneNameById: () => '',
        nodeTypeLabel: (nodeType: string) => nodeType,
        nodeCardClass: (nodeType: string) => `is-${nodeType.toLowerCase()}`
      },
      global: {
        stubs: {
          'el-dropdown': DropdownStub,
          'el-dropdown-menu': DropdownMenuStub,
          'el-dropdown-item': DropdownItemStub,
          'el-tag': TagStub
        }
      }
    })

    const branchShells = wrapper.findAll('.branch-shell')

    expect(wrapper.find('.branch-drag-handle').exists()).toBe(true)
    expect(wrapper.find('.branch-shell.is-dual-lane').exists()).toBe(true)
    expect(wrapper.find('.branch-shell.is-compact').exists()).toBe(true)
    expect(branchShells[0]?.attributes('style')).toContain('--branch-shell-width: max-content;')
    expect(branchShells[0]?.attributes('style')).toContain('--branch-lane-min-width: 144px;')
    expect(branchShells[0]?.attributes('style')).toContain('--branch-element-width: 144px;')
    expect(branchShells[0]?.attributes('style')).toContain('--branch-lane-gray-strength: 0%;')
    expect(branchShells[1]?.attributes('style')).toContain('--branch-shell-width: max-content;')
    expect(branchShells[1]?.attributes('style')).toContain('--branch-shell-max-width: none;')
    expect(branchShells[1]?.attributes('style')).toContain('--branch-element-width: 144px;')
    expect(branchShells[1]?.attributes('style')).toContain('--branch-lane-gray-strength: 3%;')
    expect(branchShells[1]?.attributes('style')).not.toContain('--branch-shell-width: 100%;')
    expect(branchShells[1]?.attributes('style')).not.toContain('--branch-shell-max-width: 100%;')
    expect(wrapper.find('.branch-shell.is-compact .branch-lanes').exists()).toBe(true)
    expect(wrapper.find('.node-shell.is-lane-node .flow-node-card').exists()).toBe(true)
    expect(wrapper.find('.flow-node-card').attributes('data-flow-interactive')).toBe('true')
    expect(wrapper.find('.route-head-card').attributes('data-flow-interactive')).toBe('true')
    expect(wrapper.find('.insert-trigger').attributes('data-flow-interactive')).toBe('true')
    expect(wrapper.findAll('[data-testid="flow-step-connector"]')).toHaveLength(4)
    expect(wrapper.findAll('.flow-stack.is-center-rail-only [data-testid="flow-step-connector"]')).toHaveLength(0)
    expect(wrapper.findAll('[data-testid="branch-split-line"]')).toHaveLength(2)
    expect(wrapper.findAll('[data-testid="branch-merge-line"]')).toHaveLength(2)
    expect(wrapper.findAll('[data-testid="branch-post-merge-connector"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-testid="branch-lane-body"]')).toHaveLength(4)
    expect(wrapper.findAll('[data-testid="branch-lane-center-rail"]')).toHaveLength(4)
    expect(wrapper.find('.branch-lane-stack.is-lane-stack').exists()).toBe(true)
    expect(wrapper.findAll('.branch-lane-stack.is-distributed-lane-stack')).toHaveLength(1)
    expect(wrapper.findAll('[data-spacing-mode="distributed"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-spacing-mode="compact"]')).toHaveLength(3)
    expect(wrapper.findAll('.flow-stack.is-distributed-spacing')).toHaveLength(1)
    expect(wrapper.findAll('.flow-step.is-distributed-gap-step')).toHaveLength(1)
    expect(wrapper.findAll('.branch-line-joint-slot')).toHaveLength(0)
    expect(wrapper.findAll('[data-testid="branch-add-route-trigger"]')).toHaveLength(2)
    expect(wrapper.findAll('.insert-trigger-shell.is-merged-target')).toHaveLength(1)
    expect(wrapper.findAll('.branch-post-merge-insert .insert-trigger')).toHaveLength(1)
    expect(wrapper.find('.branch-post-merge-insert .insert-trigger').attributes('aria-label')).toContain('2')
    expect(wrapper.findAll('[data-testid="branch-add-route-trigger"]')[0]?.attributes('aria-label')).toBeTruthy()
    expect(wrapper.text()).not.toContain('条件设置')
    expect(wrapper.text()).not.toContain('默认场景')

    const routeHeadCard = wrapper.findAll('.route-head-card').find((item) => item.text().includes('Branch B'))
    await routeHeadCard!.trigger('click')
    expect(wrapper.emitted('select-route')).toEqual([['route-root-b']])

    const addRouteTriggers = wrapper.findAll('[data-testid="branch-add-route-trigger"]')
    await addRouteTriggers[addRouteTriggers.length - 1]!.trigger('click')
    expect(wrapper.emitted('add-route-lane')).toEqual([['branch-root']])

    const nestedBranchShell = wrapper.findAll('.branch-shell')[1]
    expect(nestedBranchShell?.attributes('style')).toContain('--branch-lane-mask-surface:')
  })

  it('emits copy drag payload for approval nodes when ctrl-dragging', async () => {
    const wrapper = mount(ProcessFlowCanvasRenderer, {
      props: {
        blocks: buildBlocks(),
        selectedNodeKey: '',
        selectedRouteKey: '',
        sceneNameById: () => '',
        nodeTypeLabel: (nodeType: string) => nodeType,
        nodeCardClass: (nodeType: string) => `is-${nodeType.toLowerCase()}`
      },
      global: {
        stubs: {
          'el-dropdown': DropdownStub,
          'el-dropdown-menu': DropdownMenuStub,
          'el-dropdown-item': DropdownItemStub,
          'el-tag': TagStub
        }
      }
    })

    const dataTransfer = {
      setData: vi.fn(),
      setDragImage: vi.fn(),
      effectAllowed: 'move'
    }

    await wrapper.find('.flow-node-card').trigger('dragstart', {
      ctrlKey: true,
      dataTransfer
    })

    expect(dataTransfer.setData).toHaveBeenCalledWith('text/plain', 'approval-lane')
    expect(wrapper.emitted('drag-node-start')).toEqual([
      [{ nodeKey: 'approval-lane', mode: 'copy' }]
    ])
  })

  it('keeps branch handles non-draggable and does not emit drag events from them', async () => {
    const wrapper = mount(ProcessFlowCanvasRenderer, {
      props: {
        blocks: buildBlocks(),
        selectedNodeKey: '',
        selectedRouteKey: '',
        sceneNameById: () => '',
        nodeTypeLabel: (nodeType: string) => nodeType,
        nodeCardClass: (nodeType: string) => `is-${nodeType.toLowerCase()}`
      },
      global: {
        stubs: {
          'el-dropdown': DropdownStub,
          'el-dropdown-menu': DropdownMenuStub,
          'el-dropdown-item': DropdownItemStub,
          'el-tag': TagStub
        }
      }
    })

    const branchHandle = wrapper.get('.branch-drag-handle')
    expect(branchHandle.attributes('draggable')).toBeUndefined()

    await branchHandle.trigger('dragstart')

    expect(wrapper.emitted('drag-node-start')).toBeUndefined()
  })
})
