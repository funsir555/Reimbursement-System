import type { ArchiveAgentWorkflowEdge, ArchiveAgentWorkflowNode } from '@/api'

type CanvasNode = ArchiveAgentWorkflowNode & { config: Record<string, any> }
export type Point = { x: number; y: number }

/** Keep server and canvas shape rules aligned for controlled query workflows. */
export function validateCanvasGraph(nodes: CanvasNode[], edges: ArchiveAgentWorkflowEdge[], toolCodes: Set<string>): string[] {
  const messages: string[] = []
  const byKey = new Map(nodes.map(node => [node.nodeKey, node]))
  const starts = nodes.filter(node => node.nodeType === 'start')
  const llms = nodes.filter(node => node.nodeType === 'llm')
  const ends = nodes.filter(node => node.nodeType === 'end')
  const tools = nodes.filter(node => node.nodeType === 'tool')
  if (starts.length !== 1) messages.push('流程必须有且只有一个开始节点')
  if (!ends.length) messages.push('流程至少需要一个结束节点')
  const incoming = new Map(nodes.map(node => [node.nodeKey, 0]))
  const outgoing = new Map(nodes.map(node => [node.nodeKey, [] as string[]]))
  edges.forEach(edge => {
    if (!byKey.has(edge.source) || !byKey.has(edge.target)) {
      messages.push(`连线 ${edge.source} → ${edge.target} 引用了不存在的节点`)
      return
    }
    incoming.set(edge.target, (incoming.get(edge.target) || 0) + 1)
    if (outgoing.get(edge.source)?.includes(edge.target)) messages.push('工作流存在重复连线')
    else outgoing.get(edge.source)?.push(edge.target)
  })
  tools.forEach(node => {
    if (!node.config.toolCode) messages.push(`${node.label} 未选择工具编码`)
    if (!node.config.queryTemplateCode) messages.push(`${node.label} 未配置查询模板编码`)
    if (node.config.variableMapping) {
      try { JSON.parse(node.config.variableMapping) } catch { messages.push(`${node.label} 的变量映射不是合法 JSON`) }
    }
  })
  if (starts.length === 1) {
    const visited = new Set<string>()
    const queue = [starts[0]!.nodeKey]
    while (queue.length) {
      const current = queue.shift()!
      if (visited.has(current)) continue
      visited.add(current)
      queue.push(...(outgoing.get(current) || []))
    }
    if (visited.size !== nodes.length) messages.push('存在未从开始节点连通的节点')
  }
  if (!tools.some(node => toolCodes.has(node.config.toolCode))) return [...new Set(messages)]
  if (tools.length < 1 || tools.length > 9 || llms.length !== 1 || ends.length !== 1 || starts.length !== 1)
    messages.push('受控查询流程需要一个开始、1 至 9 个工具、一个 LLM 和一个结束节点')
  if (tools.some(node => !toolCodes.has(node.config.toolCode))) messages.push('受控查询流程不能混用历史工具')
  if (nodes.some(node => !['start', 'tool', 'llm', 'end'].includes(node.nodeType)))
    messages.push('受控查询流程当前仅支持开始、工具、LLM 和结束节点')
  if (messages.length || !starts[0] || !llms[0] || !ends[0]) return [...new Set(messages)]
  const llm = llms[0].nodeKey
  const end = ends[0].nodeKey
  if ((outgoing.get(llm) || []).join() !== end || incoming.get(end) !== 1 || (outgoing.get(end) || []).length)
    messages.push('受控查询流程必须在 LLM 汇合后直接结束')
  const prefix: string[] = []
  const branches: string[] = []
  let cursor = starts[0].nodeKey
  const visited = new Set<string>()
  let reachedJoin = false
  while (!visited.has(cursor) && !messages.length) {
    visited.add(cursor)
    const next = outgoing.get(cursor) || []
    if (next.length > 1) {
      if (next.length < 2 || next.length > 4) messages.push('并行区只允许 2 至 4 个查询分支')
      next.forEach(key => {
        if (byKey.get(key)?.nodeType !== 'tool' || incoming.get(key) !== 1 || (outgoing.get(key) || []).join() !== llm)
          messages.push('每条并行分支必须恰好一个查询节点，并直接汇合到 LLM')
        else { branches.push(key); validateReferences(byKey.get(key)!, prefix, messages) }
      })
      if (incoming.get(llm) !== next.length) messages.push('并行分支必须全部汇合到同一个 LLM 节点')
      reachedJoin = true
      break
    }
    if (next.length !== 1) { messages.push('受控查询流程存在悬空节点或不支持的分支'); break }
    const key = next[0]!
    if (key === llm) {
      if (incoming.get(llm) !== 1) messages.push('LLM 节点存在不支持的入线')
      reachedJoin = true
      break
    }
    const node = byKey.get(key)
    if (node?.nodeType !== 'tool' || incoming.get(key) !== 1) {
      messages.push('顺序阶段只允许单链路查询节点'); break
    }
    validateReferences(node, prefix, messages)
    prefix.push(key)
    cursor = key
  }
  if (!reachedJoin && !messages.length)
    messages.push('工作流存在循环连线')
  if (prefix.length + branches.length !== tools.length)
    messages.push('并行区之后不得继续查询，且所有工具节点必须在有效路径上')
  return [...new Set(messages)]
}

function validateReferences(node: CanvasNode, available: string[], messages: string[]) {
  const raw = node.config.variableMapping
  if (!raw) return
  for (const match of String(raw).matchAll(/\{\{steps\.([A-Za-z0-9_-]+)\.record\.[A-Za-z0-9_]+}}/g)) {
    if (!available.includes(match[1]!)) messages.push(`${node.label} 只能引用已完成的上游查询结果`)
  }
}

/** Spread edge endpoints by the other node's vertical position and stable key. */
export function edgePoint(nodeKey: string, peerKey: string | null, side: 'input' | 'output',
                          nodes: CanvasNode[], edges: ArchiveAgentWorkflowEdge[]): Point {
  const byKey = new Map(nodes.map(node => [node.nodeKey, node]))
  const node = byKey.get(nodeKey)
  const position = node?.config.canvasPosition || { x: 0, y: 0 }
  const peers = edges.filter(edge => side === 'input' ? edge.target === nodeKey : edge.source === nodeKey)
    .map(edge => side === 'input' ? edge.source : edge.target)
    .sort((a, b) => {
      const ay = byKey.get(a)?.config.canvasPosition?.y || 0
      const by = byKey.get(b)?.config.canvasPosition?.y || 0
      return ay - by || a.localeCompare(b)
    })
  const index = peerKey === null ? -1 : peers.indexOf(peerKey)
  const offset = index < 0 || peers.length < 2 ? 48 : 20 + (index * 56) / (peers.length - 1)
  return { x: position.x + (side === 'output' ? 210 : 0), y: position.y + offset }
}
