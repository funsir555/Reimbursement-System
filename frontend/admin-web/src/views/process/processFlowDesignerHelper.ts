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

export type FlowMoveReason = 'MOVED' | 'NOT_FOUND' | 'INVALID_TARGET' | 'NOOP'

export type FlowMoveResult = FlowStateSnapshot & {
  moved: boolean
  reason: FlowMoveReason
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
  return buildContainerBlocks(nodes, routes, containerKey, 0)
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
    displayOrder: buildDisplayOrderHint(ordered, index)
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
      attachBelowNodes: false,
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

export function moveNodeIntoContainer(
  nodes: ProcessFlowNode[],
  routes: ProcessFlowRoute[],
  nodeKey: string,
  containerKey: FlowContainerKey,
  index: number
): FlowMoveResult {
  const nextNodes = cloneValue(nodes)
  const nextRoutes = cloneValue(routes)
  const targetNode = nextNodes.find((item) => item.nodeKey === nodeKey)
  if (!targetNode) {
    return {
      nodes: nextNodes,
      routes: nextRoutes,
      moved: false,
      reason: 'NOT_FOUND'
    }
  }

  const sourceContainerKey = normalizeContainerKey(targetNode.parentNodeKey)
  const targetContainerKey = normalizeContainerKey(containerKey)
  if (targetNode.nodeType === 'BRANCH' && targetContainerKey && collectBranchDescendantRouteKeys(nextNodes, nextRoutes, nodeKey).has(targetContainerKey)) {
    return {
      nodes: nextNodes,
      routes: nextRoutes,
      moved: false,
      reason: 'INVALID_TARGET'
    }
  }

  const sourceNodes = listNodesInContainer(nextNodes, sourceContainerKey)
  const sourceIndex = sourceNodes.findIndex((item) => item.nodeKey === nodeKey)
  if (sourceIndex < 0) {
    return {
      nodes: nextNodes,
      routes: nextRoutes,
      moved: false,
      reason: 'NOT_FOUND'
    }
  }

  const targetNodes = sourceContainerKey === targetContainerKey
    ? sourceNodes.filter((item) => item.nodeKey !== nodeKey)
    : listNodesInContainer(nextNodes, targetContainerKey)
  let effectiveIndex = clamp(index, 0, targetNodes.length)
  if (sourceContainerKey === targetContainerKey && index > sourceIndex) {
    effectiveIndex -= 1
  }

  if (sourceContainerKey === targetContainerKey && effectiveIndex === sourceIndex) {
    return {
      nodes: nextNodes,
      routes: nextRoutes,
      moved: false,
      reason: 'NOOP'
    }
  }

  const removalIndex = nextNodes.findIndex((item) => item.nodeKey === nodeKey)
  const [removedNode] = removalIndex >= 0 ? nextNodes.splice(removalIndex, 1) : [undefined]
  if (!removedNode) {
    return {
      nodes: nextNodes,
      routes: nextRoutes,
      moved: false,
      reason: 'NOT_FOUND'
    }
  }

  const preparedNode: ProcessFlowNode = {
    ...removedNode,
    parentNodeKey: toParentNodeKey(targetContainerKey),
    displayOrder: buildDisplayOrderHint(targetNodes, effectiveIndex)
  }
  const anchor = targetNodes[effectiveIndex]
  if (anchor) {
    const anchorIndex = nextNodes.findIndex((item) => item.nodeKey === anchor.nodeKey)
    nextNodes.splice(anchorIndex, 0, preparedNode)
  } else {
    nextNodes.push(preparedNode)
  }

  const snapshot = reindexFlowState(nextNodes, nextRoutes)
  return {
    ...snapshot,
    moved: true,
    reason: 'MOVED'
  }
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
    const ordered = orderRoutesForBranch(nextRoutes, branchNodeKey)
    ordered.forEach((item, index) => {
      const target = nextRoutes.find((route) => route.routeKey === item.routeKey)
      if (!target) {
        return
      }
      target.priority = index + 1
      target.defaultRoute = false
      target.attachBelowNodes = Boolean(index === 0 && item.attachBelowNodes)
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

function buildContainerBlocks(
  nodes: ProcessFlowNode[],
  routes: ProcessFlowRoute[],
  containerKey: FlowContainerKey,
  startIndex: number
): FlowCanvasBlock[] {
  const blocks: FlowCanvasBlock[] = [createInsertBlock(containerKey, startIndex)]
  const orderedNodes = listNodesInContainer(nodes, containerKey)

  for (const [offset, node] of orderedNodes.slice(startIndex).entries()) {
    const index = startIndex + offset
    if (node.nodeType === 'BRANCH') {
      const branchRoutes = listRoutesForBranch(routes, node.nodeKey)
      const attachedRoute = branchRoutes.find((item) => item.attachBelowNodes)
      blocks.push({
        key: `branch-${node.nodeKey}`,
        kind: 'branch',
        node,
        routes: branchRoutes.map((route) => ({
          route,
          blocks: buildRouteLaneBlocks(nodes, routes, route, containerKey, index)
        }))
      })
      if (attachedRoute) {
        break
      }
    } else {
      blocks.push({
        key: `node-${node.nodeKey}`,
        kind: 'node',
        node
      })
    }

    blocks.push(createInsertBlock(containerKey, index + 1))
  }

  return blocks
}

function buildRouteLaneBlocks(
  nodes: ProcessFlowNode[],
  routes: ProcessFlowRoute[],
  route: ProcessFlowRoute,
  parentContainerKey: FlowContainerKey,
  branchIndex: number
) {
  const laneBlocks = buildContainerBlocks(nodes, routes, route.routeKey, 0)
  if (!route.attachBelowNodes) {
    return laneBlocks
  }
  return [
    ...laneBlocks,
    ...buildContainerBlocks(nodes, routes, parentContainerKey, branchIndex + 1)
  ]
}

function listRoutesForBranch(routes: ProcessFlowRoute[], branchNodeKey: string) {
  return orderRoutesForBranch(routes, branchNodeKey)
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

function collectBranchDescendantRouteKeys(nodes: ProcessFlowNode[], routes: ProcessFlowRoute[], branchNodeKey: string) {
  const routeKeys = new Set<string>()
  const queue = [branchNodeKey]

  while (queue.length) {
    const currentNodeKey = queue.shift()
    if (!currentNodeKey) {
      continue
    }

    routes
      .filter((item) => item.sourceNodeKey === currentNodeKey)
      .forEach((route) => {
        if (routeKeys.has(route.routeKey)) {
          return
        }
        routeKeys.add(route.routeKey)
        nodes
          .filter((item) => normalizeContainerKey(item.parentNodeKey) === route.routeKey && item.nodeType === 'BRANCH')
          .forEach((node) => {
            queue.push(node.nodeKey)
          })
      })
  }

  return routeKeys
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

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function buildDisplayOrderHint(nodes: ProcessFlowNode[], index: number) {
  const previous = nodes[index - 1]
  const next = nodes[index]
  if (previous && next) {
    return ((previous.displayOrder ?? index) + (next.displayOrder ?? (index + 1))) / 2
  }
  if (next) {
    return (next.displayOrder ?? (index + 1)) - 1
  }
  if (previous) {
    return (previous.displayOrder ?? index) + 1
  }
  return 1
}

function createRouteKey(branchNodeKey: string, order: number) {
  return `${branchNodeKey}-route-${order}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
}

function orderRoutesForBranch(routes: ProcessFlowRoute[], branchNodeKey: string) {
  const candidates = routes
    .filter((item) => item.sourceNodeKey === branchNodeKey)
    .sort((left, right) => {
      const priorityGap = (left.priority ?? 0) - (right.priority ?? 0)
      if (priorityGap !== 0) {
        return priorityGap
      }
      return left.routeKey.localeCompare(right.routeKey)
    })
  const attachedRoute = candidates.find((item) => item.attachBelowNodes)
  if (!attachedRoute) {
    return candidates
  }
  return [attachedRoute, ...candidates.filter((item) => item.routeKey !== attachedRoute.routeKey)]
}
