import { describe, expect, it } from 'vitest'
import type { ProcessFlowNode, ProcessFlowRoute } from '@/api'
import {
  appendRouteToBranch,
  buildDefaultBranchRoutes,
  buildFlowCanvasBlocks,
  insertNodeIntoContainer,
  normalizeContainerKey,
  removeNodeAndDescendants,
  removeRouteLane,
  reindexFlowState
} from '@/views/process/processFlowDesignerHelper'

function createApprovalNode(nodeKey: string, displayOrder: number, parentNodeKey = ''): ProcessFlowNode {
  return {
    nodeKey,
    nodeType: 'APPROVAL',
    nodeName: nodeKey,
    parentNodeKey,
    displayOrder,
    config: {
      managerConfig: {},
      designatedMemberConfig: { userIds: [] },
      manualSelectConfig: { candidateScope: 'ALL_ACTIVE_USERS' }
    }
  }
}

function createBranchNode(nodeKey: string, displayOrder: number, parentNodeKey = ''): ProcessFlowNode {
  return {
    nodeKey,
    nodeType: 'BRANCH',
    nodeName: nodeKey,
    parentNodeKey,
    displayOrder,
    config: {
      managerConfig: {},
      designatedMemberConfig: { userIds: [] },
      manualSelectConfig: { candidateScope: 'ALL_ACTIVE_USERS' }
    }
  }
}

function createRoute(routeKey: string, sourceNodeKey: string, priority: number): ProcessFlowRoute {
  return {
    routeKey,
    sourceNodeKey,
    targetNodeKey: undefined,
    routeName: `条件分支 ${priority}`,
    priority,
    defaultRoute: false,
    conditionGroups: []
  }
}

describe('processFlowDesignerHelper', () => {
  it('builds a branch block with two lanes', () => {
    const nodes = [createBranchNode('branch-1', 1)]
    const routes = [createRoute('route-1', 'branch-1', 1), createRoute('route-2', 'branch-1', 2)]

    const blocks = buildFlowCanvasBlocks(nodes, routes)
    const branchBlock = blocks.find((item) => item.kind === 'branch')

    expect(branchBlock?.kind).toBe('branch')
    if (branchBlock?.kind === 'branch') {
      expect(branchBlock.routes).toHaveLength(2)
      expect(branchBlock.routes[0].route.routeKey).toBe('route-1')
      expect(branchBlock.routes[1].route.routeKey).toBe('route-2')
      expect(branchBlock.routes[0].blocks[0].kind).toBe('insert')
    }
  })

  it('builds nested branch blocks inside route lanes', () => {
    const nodes = [
      createBranchNode('branch-1', 1),
      createBranchNode('branch-2', 1, 'route-1')
    ]
    const routes = [
      createRoute('route-1', 'branch-1', 1),
      createRoute('route-2', 'branch-1', 2),
      createRoute('route-3', 'branch-2', 1),
      createRoute('route-4', 'branch-2', 2)
    ]

    const blocks = buildFlowCanvasBlocks(nodes, routes)
    const branchBlock = blocks.find((item) => item.kind === 'branch')

    expect(branchBlock?.kind).toBe('branch')
    if (branchBlock?.kind === 'branch') {
      const nestedBranch = branchBlock.routes[0].blocks.find((item) => item.kind === 'branch')
      expect(nestedBranch?.kind).toBe('branch')
      if (nestedBranch?.kind === 'branch') {
        expect(nestedBranch.routes).toHaveLength(2)
      }
    }
  })

  it('inserts a node into a route container with the correct parent key', () => {
    const inserted = insertNodeIntoContainer(
      [],
      createApprovalNode('approval-1', 1),
      'route-1',
      0
    )

    expect(inserted.nodes).toHaveLength(1)
    expect(inserted.nodes[0].parentNodeKey).toBe('route-1')
    expect(inserted.nodes[0].displayOrder).toBe(1)
  })

  it('creates default branch routes under the same branch node', () => {
    const routes = buildDefaultBranchRoutes('branch-1')

    expect(routes).toHaveLength(2)
    expect(routes.every((item) => item.sourceNodeKey === 'branch-1')).toBe(true)
    expect(routes.map((item) => item.routeName)).toEqual(['条件分支 1', '条件分支 2'])
  })

  it('appends a new route lane with a unique route key', () => {
    const routes = [createRoute('branch-1-route-2-existing', 'branch-1', 1)]

    const nextRoutes = appendRouteToBranch(routes, 'branch-1')

    expect(nextRoutes).toHaveLength(2)
    expect(new Set(nextRoutes.map((item) => item.routeKey)).size).toBe(2)
    expect(nextRoutes.map((item) => item.priority)).toEqual([1, 2])
  })

  it('removes a route lane together with nested nodes and routes', () => {
    const nodes = [
      createBranchNode('branch-1', 1),
      createApprovalNode('approval-1', 1, 'route-1'),
      createBranchNode('branch-2', 2, 'route-1'),
      createApprovalNode('approval-2', 1, 'route-3')
    ]
    const routes = [
      createRoute('route-1', 'branch-1', 1),
      createRoute('route-2', 'branch-1', 2),
      createRoute('route-3', 'branch-2', 1)
    ]

    const next = removeRouteLane(nodes, routes, 'route-1')

    expect(next.routes.map((item) => item.routeKey)).toEqual(['route-2'])
    expect(next.nodes.map((item) => item.nodeKey)).toEqual(['branch-1'])
  })

  it('removes a branch node together with all descendants', () => {
    const nodes = [
      createApprovalNode('approval-root', 1),
      createBranchNode('branch-1', 2),
      createApprovalNode('approval-1', 1, 'route-1')
    ]
    const routes = [createRoute('route-1', 'branch-1', 1), createRoute('route-2', 'branch-1', 2)]

    const next = removeNodeAndDescendants(nodes, routes, 'branch-1')

    expect(next.nodes.map((item) => item.nodeKey)).toEqual(['approval-root'])
    expect(next.routes).toHaveLength(0)
  })

  it('reindexes display order per container without touching other containers', () => {
    const nodes = [
      createApprovalNode('approval-root-2', 9),
      createApprovalNode('approval-root-1', 3),
      createApprovalNode('approval-lane-2', 7, 'route-1'),
      createApprovalNode('approval-lane-1', 2, 'route-1')
    ]
    const routes = [createRoute('route-2', 'branch-1', 9), createRoute('route-1', 'branch-1', 4)]

    const next = reindexFlowState(nodes, routes)

    expect(
      next.nodes
        .filter((item) => normalizeContainerKey(item.parentNodeKey) === null)
        .sort((left, right) => (left.displayOrder ?? 0) - (right.displayOrder ?? 0))
        .map((item) => item.nodeKey)
    ).toEqual(['approval-root-1', 'approval-root-2'])

    expect(
      next.nodes
        .filter((item) => item.parentNodeKey === 'route-1')
        .sort((left, right) => (left.displayOrder ?? 0) - (right.displayOrder ?? 0))
        .map((item) => item.nodeKey)
    ).toEqual(['approval-lane-1', 'approval-lane-2'])

    expect(next.routes.map((item) => item.priority)).toEqual([2, 1])
    expect(
      next.routes
        .sort((left, right) => (left.priority ?? 0) - (right.priority ?? 0))
        .map((item) => item.routeName)
    ).toEqual(['条件分支 1', '条件分支 2'])
  })
})
