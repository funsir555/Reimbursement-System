import type { ProcessFlowNode, ProcessFlowRoute } from '@/api'

export type FlowInsertType = 'APPROVAL' | 'CC' | 'PAYMENT' | 'BRANCH'
export type FlowContainerKey = string | null

export type FlowCanvasInsertBlock = {
  key: string
  kind: 'insert'
  containerKey: FlowContainerKey
  index: number
}

export type FlowCanvasNodeBlock = {
  key: string
  kind: 'node'
  node: ProcessFlowNode
}

export type FlowCanvasRouteLane = {
  route: ProcessFlowRoute
  blocks: FlowCanvasBlock[]
}

export type FlowCanvasBranchBlock = {
  key: string
  kind: 'branch'
  node: ProcessFlowNode
  routes: FlowCanvasRouteLane[]
}

export type FlowCanvasBlock = FlowCanvasInsertBlock | FlowCanvasNodeBlock | FlowCanvasBranchBlock

export type FlowStateSnapshot = {
  nodes: ProcessFlowNode[]
  routes: ProcessFlowRoute[]
}

const AUTO_ROUTE_NAME_PREFIX = '条件分支 '

export function normalizeContainerKey(value?: string | null): FlowContainerKey {
  if (value === undefined || value === null) {
    return null
  }
  const text = value.trim()
  return text ? text : null
}

export function toParentNodeKey(containerKey: FlowContainerKey) {
  return containerKey ?? ''
}

export function buildFlowCanvasBlocks(
  nodes: ProcessFlowNode[],
  routes: ProcessFlowRoute[],
  containerKey: FlowContainerKey = null
): FlowCanvasBlock[] {
  const blocks: FlowCanvasBlock[] = [createInsertBlock(containerKey, 0)]
  const orderedNodes = listNodesInContainer(nodes, containerKey)

  orderedNodes.forEach((node, index) => {
    if (node.nodeType === 'BRANCH') {
      blocks.push({
        key: `branch-${node.nodeKey}`,
        kind: 'branch',
        node,
        routes: listRoutesForBranch(routes, node.nodeKey).map((route) => ({
          route,
          blocks: buildFlowCanvasBlocks(nodes, routes, route.routeKey)
        }))
      })
    } else {
      blocks.push({
        key: `node-${node.nodeKey}`,
        kind: 'node',
        node
      })
    }

    blocks.push(createInsertBlock(containerKey, index + 1))
  })

  return blocks
}

export function insertNodeIntoContainer(
  nodes: ProcessFlowNode[],
  node: ProcessFlowNode,
  containerKey: FlowContainerKey,
  index: number
) {
  const nextNodes = cloneValue(nodes)
  const ordered = listNodesInContainer(nextNodes, containerKey)
  const anchor = ordered[index]
  const preparedNode: ProcessFlowNode = {
    ...cloneValue(node),
    parentNodeKey: toParentNodeKey(containerKey),
    displayOrder: index + 1
  }

  if (anchor) {
    const anchorIndex = nextNodes.findIndex((item) => item.nodeKey === anchor.nodeKey)
    nextNodes.splice(anchorIndex, 0, preparedNode)
  } else {
    nextNodes.push(preparedNode)
  }

  return reindexFlowState(nextNodes, [])
}

export function buildDefaultBranchRoutes(branchNodeKey: string, existingCount = 0, count = 2): ProcessFlowRoute[] {
  return Array.from({ length: count }, (_, index) => {
    const order = existingCount + index + 1
    return {
      routeKey: createRouteKey(branchNodeKey, order),
      sourceNodeKey: branchNodeKey,
      targetNodeKey: undefined,
      routeName: `${AUTO_ROUTE_NAME_PREFIX}${order}`,
      priority: order,
      defaultRoute: false,
      conditionGroups: []
    }
  })
}

export function appendRouteToBranch(routes: ProcessFlowRoute[], branchNodeKey: string): ProcessFlowRoute[] {
  const nextRoutes = cloneValue(routes)
  const existingCount = nextRoutes.filter((item) => item.sourceNodeKey === branchNodeKey).length
  nextRoutes.push(...buildDefaultBranchRoutes(branchNodeKey, existingCount, 1))
  return reindexFlowState([], nextRoutes).routes
}

export function removeRouteLane(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[], routeKey: string): FlowStateSnapshot {
  const nextNodes = cloneValue(nodes)
  const nextRoutes = cloneValue(routes)
  const route = nextRoutes.find((item) => item.routeKey === routeKey)
  if (!route) {
    return {
      nodes: nextNodes,
      routes: nextRoutes
    }
  }

  const nodeKeysToRemove = new Set<string>()
  const routeKeysToRemove = new Set<string>([routeKey])

  collectContainerDescendants(nextNodes, nextRoutes, routeKey, nodeKeysToRemove, routeKeysToRemove)

  return reindexFlowState(
    nextNodes.filter((item) => !nodeKeysToRemove.has(item.nodeKey)),
    nextRoutes.filter((item) => !routeKeysToRemove.has(item.routeKey))
  )
}

export function removeNodeAndDescendants(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[], nodeKey: string): FlowStateSnapshot {
  const nextNodes = cloneValue(nodes)
  const nextRoutes = cloneValue(routes)
  const targetNode = nextNodes.find((item) => item.nodeKey === nodeKey)
  if (!targetNode) {
    return {
      nodes: nextNodes,
      routes: nextRoutes
    }
  }

  const nodeKeysToRemove = new Set<string>([nodeKey])
  const routeKeysToRemove = new Set<string>()

  if (targetNode.nodeType === 'BRANCH') {
    const branchRoutes = nextRoutes.filter((item) => item.sourceNodeKey === nodeKey)
    branchRoutes.forEach((route) => {
      routeKeysToRemove.add(route.routeKey)
      collectContainerDescendants(nextNodes, nextRoutes, route.routeKey, nodeKeysToRemove, routeKeysToRemove)
    })
  }

  return reindexFlowState(
    nextNodes.filter((item) => !nodeKeysToRemove.has(item.nodeKey)),
    nextRoutes.filter((item) => !routeKeysToRemove.has(item.routeKey))
  )
}

export function reindexFlowState(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[]): FlowStateSnapshot {
  const nextNodes = cloneValue(nodes)
  const nextRoutes = cloneValue(routes)

  const containerKeys = new Set<string | null>([null])
  nextNodes.forEach((item) => {
    containerKeys.add(normalizeContainerKey(item.parentNodeKey))
  })
  nextRoutes.forEach((item) => {
    containerKeys.add(item.routeKey)
  })

  containerKeys.forEach((containerKey) => {
    const ordered = listNodesInContainer(nextNodes, containerKey)
    ordered.forEach((item, index) => {
      const target = nextNodes.find((node) => node.nodeKey === item.nodeKey)
      if (!target) {
        return
      }
      target.parentNodeKey = toParentNodeKey(containerKey)
      target.displayOrder = index + 1
    })
  })

  const branchNodeKeys = new Set(nextRoutes.map((item) => item.sourceNodeKey))
  branchNodeKeys.forEach((branchNodeKey) => {
    const ordered = listRoutesForBranch(nextRoutes, branchNodeKey)
    ordered.forEach((item, index) => {
      const target = nextRoutes.find((route) => route.routeKey === item.routeKey)
      if (!target) {
        return
      }
      target.priority = index + 1
      target.defaultRoute = false
      if (!target.routeName || isAutoRouteName(target.routeName)) {
        target.routeName = `${AUTO_ROUTE_NAME_PREFIX}${index + 1}`
      }
    })
  })

  return {
    nodes: nextNodes,
    routes: nextRoutes
  }
}

function listNodesInContainer(nodes: ProcessFlowNode[], containerKey: FlowContainerKey) {
  return nodes
    .filter((item) => normalizeContainerKey(item.parentNodeKey) === containerKey)
    .sort((left, right) => {
      const orderGap = (left.displayOrder ?? 0) - (right.displayOrder ?? 0)
      if (orderGap !== 0) {
        return orderGap
      }
      return left.nodeKey.localeCompare(right.nodeKey)
    })
}

function listRoutesForBranch(routes: ProcessFlowRoute[], branchNodeKey: string) {
  return routes
    .filter((item) => item.sourceNodeKey === branchNodeKey)
    .sort((left, right) => {
      const priorityGap = (left.priority ?? 0) - (right.priority ?? 0)
      if (priorityGap !== 0) {
        return priorityGap
      }
      return left.routeKey.localeCompare(right.routeKey)
    })
}

function collectContainerDescendants(
  nodes: ProcessFlowNode[],
  routes: ProcessFlowRoute[],
  containerKey: string,
  nodeKeysToRemove: Set<string>,
  routeKeysToRemove: Set<string>
) {
  const childNodes = nodes.filter((item) => normalizeContainerKey(item.parentNodeKey) === containerKey)
  childNodes.forEach((node) => {
    nodeKeysToRemove.add(node.nodeKey)
    if (node.nodeType !== 'BRANCH') {
      return
    }

    const branchRoutes = routes.filter((item) => item.sourceNodeKey === node.nodeKey)
    branchRoutes.forEach((route) => {
      routeKeysToRemove.add(route.routeKey)
      collectContainerDescendants(nodes, routes, route.routeKey, nodeKeysToRemove, routeKeysToRemove)
    })
  })
}

function createInsertBlock(containerKey: FlowContainerKey, index: number): FlowCanvasInsertBlock {
  return {
    key: `insert-${containerKey ?? 'root'}-${index}`,
    kind: 'insert',
    containerKey,
    index
  }
}

function isAutoRouteName(name?: string) {
  if (!name) {
    return true
  }
  return new RegExp(`^${AUTO_ROUTE_NAME_PREFIX}\\d+$`).test(name)
}

function cloneValue<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function createRouteKey(branchNodeKey: string, order: number) {
  return `${branchNodeKey}-route-${order}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
}
