import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
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

function createRoute(routeKey: string, sourceNodeKey: string, routeName: string, priority: number, attachBelowNodes = false): ProcessFlowRoute {
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
  const rootNode = createBranchNode('branch-root', '根分支')
  const nestedNode = createBranchNode('branch-nested', '嵌套分支')
  const rootRoutes = [
    createRoute('route-root-a', 'branch-root', '分支 A', 1, true),
    createRoute('route-root-b', 'branch-root', '分支 B', 2)
  ]
  const nestedRoutes = [
    createRoute('route-nested-a', 'branch-nested', '内层 A', 1, true),
    createRoute('route-nested-b', 'branch-nested', '内层 B', 2)
  ]

  return [
    {
      key: 'branch-branch-root',
      kind: 'branch',
      node: rootNode,
      depth: 0,
      compact: false,
      symmetric: true,
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
  it('uses the branch drag handle, keeps dual-lane symmetry, and renders merged insert groups', () => {
    const wrapper = mount(ProcessFlowCanvasRenderer, {
      props: {
        blocks: buildBlocks(),
        selectedNodeKey: '',
        selectedRouteKey: 'route-root-a',
        sceneNameById: () => '',
        nodeTypeLabel: (nodeType: string) => nodeType,
        nodeCardClass: () => 'is-branch'
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

    expect(wrapper.find('.branch-node-card').exists()).toBe(false)
    expect(wrapper.find('.branch-drag-handle').exists()).toBe(true)
    expect(wrapper.find('.branch-shell.is-dual-lane').exists()).toBe(true)
    expect(wrapper.find('.branch-shell.is-compact').exists()).toBe(true)
    expect(wrapper.findAll('.branch-shell')[0]?.attributes('style')).toContain('--branch-shell-width: max-content;')
    expect(wrapper.findAll('.branch-shell')[0]?.attributes('style')).toContain('--branch-lane-min-width: 144px;')
    expect(wrapper.findAll('.branch-shell')[1]?.attributes('style')).toContain('--branch-shell-min-width: 100%;')
    expect(wrapper.findAll('.insert-trigger-shell.is-merged-target')).toHaveLength(1)
    expect(wrapper.text()).toContain('插入当前分支')
    expect(wrapper.text()).toContain('插入附带下方节点')
  })
})
