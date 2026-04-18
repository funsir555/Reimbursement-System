import { describe, expect, it } from 'vitest'
import type { ProcessFlowNode, ProcessFlowRoute } from '@/api'
import {
  appendRouteToBranch,
  buildDefaultBranchRoutes,
  buildFlowCanvasBlocks,
  insertNodeIntoContainer,
  moveNodeIntoContainer,
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
    attachBelowNodes: false,
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

  it('attaches branch tail nodes to the left route when attachBelowNodes is enabled', () => {
    const nodes = [
      createBranchNode('branch-1', 1),
      createApprovalNode('tail-1', 2),
      createApprovalNode('tail-2', 3)
    ]
    const routes = [
      {
        ...createRoute('route-1', 'branch-1', 2),
        attachBelowNodes: true
      },
      createRoute('route-2', 'branch-1', 1)
    ]

    const blocks = buildFlowCanvasBlocks(nodes, routes)

    expect(blocks.filter((item) => item.kind === 'branch')).toHaveLength(1)
    expect(blocks.filter((item) => item.kind === 'node')).toHaveLength(0)

    const branchBlock = blocks.find((item) => item.kind === 'branch')
    expect(branchBlock?.kind).toBe('branch')
    if (branchBlock?.kind === 'branch') {
      expect(branchBlock.routes[0].route.routeKey).toBe('route-1')
      const attachedNodes = branchBlock.routes[0].blocks
        .filter((item) => item.kind === 'node')
        .map((item) => item.kind === 'node' ? item.node.nodeKey : '')
      expect(attachedNodes).toEqual(['tail-1', 'tail-2'])
    }
  })

  it('merges the route-end insert trigger with the attached-tail insert trigger', () => {
    const nodes = [
      createBranchNode('branch-1', 1),
      createApprovalNode('tail-1', 2)
    ]
    const routes = [
      {
        ...createRoute('route-1', 'branch-1', 1),
        attachBelowNodes: true
      },
      createRoute('route-2', 'branch-1', 2)
    ]

    const blocks = buildFlowCanvasBlocks(nodes, routes)
    const branchBlock = blocks.find((item) => item.kind === 'branch')

    expect(branchBlock?.kind).toBe('branch')
    if (branchBlock?.kind === 'branch') {
      const insertBlocks = branchBlock.routes[0].blocks.filter((item) => item.kind === 'insert')
      expect(insertBlocks).toHaveLength(2)

      const mergedInsert = insertBlocks[0]
      expect(mergedInsert?.kind).toBe('insert')
      if (mergedInsert?.kind === 'insert') {
        expect(mergedInsert.targets).toHaveLength(2)
        expect(mergedInsert.targets?.map((item) => item.label)).toEqual(['插入当前分支', '插入附带下方节点'])
        expect(mergedInsert.depth).toBe(1)
      }
    }
  })

  it('attaches only the current nested container tail when nested branch uses attachBelowNodes', () => {
    const nodes = [
      createBranchNode('outer-branch', 1),
      createBranchNode('inner-branch', 1, 'outer-route-1'),
      createApprovalNode('inner-tail', 2, 'outer-route-1'),
      createApprovalNode('outer-tail', 2)
    ]
    const routes = [
      {
        ...createRoute('outer-route-1', 'outer-branch', 1),
        attachBelowNodes: true
      },
      createRoute('outer-route-2', 'outer-branch', 2),
      {
        ...createRoute('inner-route-1', 'inner-branch', 1),
        attachBelowNodes: true
      },
      createRoute('inner-route-2', 'inner-branch', 2)
    ]

    const blocks = buildFlowCanvasBlocks(nodes, routes)
    const outerBranch = blocks.find((item) => item.kind === 'branch')

    expect(outerBranch?.kind).toBe('branch')
    if (outerBranch?.kind === 'branch') {
      const innerBranch = outerBranch.routes[0].blocks.find((item) => item.kind === 'branch')
      expect(innerBranch?.kind).toBe('branch')
      if (innerBranch?.kind === 'branch') {
        expect(innerBranch.depth).toBe(1)
        expect(innerBranch.compact).toBe(true)
        expect(innerBranch.symmetric).toBe(true)
        const innerAttachedNodes = innerBranch.routes[0].blocks
          .filter((item) => item.kind === 'node')
          .map((item) => item.kind === 'node' ? item.node.nodeKey : '')
        expect(innerAttachedNodes).toEqual(['inner-tail'])
      }

      const outerAttachedNodes = outerBranch.routes[0].blocks
        .filter((item) => item.kind === 'node')
        .map((item) => item.kind === 'node' ? item.node.nodeKey : '')
      expect(outerAttachedNodes).toEqual(['outer-tail'])
    }
  })

  it('marks root two-lane branches as symmetric without compact mode', () => {
    const nodes = [createBranchNode('branch-1', 1)]
    const routes = [createRoute('route-1', 'branch-1', 1), createRoute('route-2', 'branch-1', 2)]

    const blocks = buildFlowCanvasBlocks(nodes, routes)
    const branchBlock = blocks.find((item) => item.kind === 'branch')

    expect(branchBlock?.kind).toBe('branch')
    if (branchBlock?.kind === 'branch') {
      expect(branchBlock.depth).toBe(0)
      expect(branchBlock.symmetric).toBe(true)
      expect(branchBlock.compact).toBe(false)
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

  it('moves the attached route to the first lane and keeps only one attached route per branch', () => {
    const routes = [
      {
        ...createRoute('route-1', 'branch-1', 1),
        attachBelowNodes: true
      },
      {
        ...createRoute('route-2', 'branch-1', 2),
        attachBelowNodes: true
      },
      createRoute('route-3', 'branch-2', 1)
    ]

    const next = reindexFlowState([], routes)

    const branchOneRoutes = next.routes
      .filter((item) => item.sourceNodeKey === 'branch-1')
      .sort((left, right) => (left.priority ?? 0) - (right.priority ?? 0))

    expect(branchOneRoutes.map((item) => item.routeKey)).toEqual(['route-1', 'route-2'])
    expect(branchOneRoutes.map((item) => item.attachBelowNodes)).toEqual([true, false])
  })

  it('moves a node across containers and reindexes both sides', () => {
    const nodes = [
      createApprovalNode('approval-root', 1),
      createBranchNode('branch-1', 2),
      createApprovalNode('approval-lane-1', 1, 'route-1'),
      createApprovalNode('approval-lane-2', 2, 'route-1')
    ]
    const routes = [createRoute('route-1', 'branch-1', 1), createRoute('route-2', 'branch-1', 2)]

    const next = moveNodeIntoContainer(nodes, routes, 'approval-lane-2', null, 0)

    expect(next.moved).toBe(true)
    expect(next.reason).toBe('MOVED')
    expect(
      next.nodes
        .filter((item) => normalizeContainerKey(item.parentNodeKey) === null)
        .sort((left, right) => (left.displayOrder ?? 0) - (right.displayOrder ?? 0))
        .map((item) => item.nodeKey)
    ).toEqual(['approval-lane-2', 'approval-root', 'branch-1'])
    expect(
      next.nodes
        .filter((item) => item.parentNodeKey === 'route-1')
        .sort((left, right) => (left.displayOrder ?? 0) - (right.displayOrder ?? 0))
        .map((item) => item.nodeKey)
    ).toEqual(['approval-lane-1'])
  })

  it('treats dropping a node onto its current adjacent insert slot as a no-op', () => {
    const nodes = [
      createApprovalNode('approval-1', 1),
      createApprovalNode('approval-2', 2),
      createApprovalNode('approval-3', 3)
    ]

    const next = moveNodeIntoContainer(nodes, [], 'approval-2', null, 2)

    expect(next.moved).toBe(false)
    expect(next.reason).toBe('NOOP')
    expect(next.nodes.map((item) => item.nodeKey)).toEqual(['approval-1', 'approval-2', 'approval-3'])
  })

  it('keeps branch descendants attached when moving a branch block', () => {
    const nodes = [
      createApprovalNode('approval-root', 1),
      createBranchNode('branch-1', 2),
      createApprovalNode('approval-1', 1, 'route-1')
    ]
    const routes = [createRoute('route-1', 'branch-1', 1), createRoute('route-2', 'branch-1', 2)]

    const next = moveNodeIntoContainer(nodes, routes, 'branch-1', null, 0)

    expect(next.moved).toBe(true)
    expect(next.nodes.find((item) => item.nodeKey === 'branch-1')?.displayOrder).toBe(1)
    expect(next.routes.map((item) => item.sourceNodeKey)).toEqual(['branch-1', 'branch-1'])
    expect(next.nodes.find((item) => item.nodeKey === 'approval-1')?.parentNodeKey).toBe('route-1')
  })

  it('rejects moving a branch block into its own descendant route', () => {
    const nodes = [
      createBranchNode('branch-1', 1),
      createApprovalNode('approval-1', 1, 'route-1')
    ]
    const routes = [createRoute('route-1', 'branch-1', 1), createRoute('route-2', 'branch-1', 2)]

    const next = moveNodeIntoContainer(nodes, routes, 'branch-1', 'route-1', 0)

    expect(next.moved).toBe(false)
    expect(next.reason).toBe('INVALID_TARGET')
  })
})
