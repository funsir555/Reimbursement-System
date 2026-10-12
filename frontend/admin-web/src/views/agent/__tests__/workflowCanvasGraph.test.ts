import { describe, expect, it } from 'vitest'
import type { ArchiveAgentWorkflowNode } from '@/api'
import { edgePoint, validateCanvasGraph } from '../workflowCanvasGraph'

const codes = new Set(['query'])
function node(key: string, type: ArchiveAgentWorkflowNode['nodeType'], y: number, mapping = '{}') {
  return { nodeKey: key, nodeType: type, label: key, config: {
    canvasPosition: { x: key === 'llm' ? 900 : 100, y },
    ...(type === 'tool' ? { toolCode: 'query', queryTemplateCode: 'template', variableMapping: mapping } : {})
  } }
}

describe('controlled canvas graph', () => {
  it('accepts three branches, spreads their arrows, and rejects sibling references', () => {
    const nodes = [node('start', 'start', 100), node('a', 'tool', 40),
      node('b', 'tool', 100), node('c', 'tool', 160), node('llm', 'llm', 100), node('end', 'end', 100)]
    const edges = [
      { source: 'start', target: 'a' }, { source: 'start', target: 'b' },
      { source: 'start', target: 'c' }, { source: 'a', target: 'llm' },
      { source: 'b', target: 'llm' }, { source: 'c', target: 'llm' },
      { source: 'llm', target: 'end' }
    ]
    expect(validateCanvasGraph(nodes, edges, codes)).toEqual([])
    const ys = ['a', 'b', 'c'].map(key => edgePoint('llm', key, 'input', nodes, edges).y)
    expect(ys[0]).toBeLessThan(ys[1]!)
    expect(ys[1]).toBeLessThan(ys[2]!)
    nodes[2]!.config.variableMapping = '{"companyId":"{{steps.a.record.id}}"}'
    expect(validateCanvasGraph(nodes, edges, codes).join()).toContain('只能引用已完成的上游查询结果')
  })

  it('rejects a fifth branch and a chained branch', () => {
    const nodes = [node('start', 'start', 0), ...'abcde'.split('').map((key, index) => node(key, 'tool', index * 50)),
      node('llm', 'llm', 100), node('end', 'end', 100)]
    const edges = 'abcde'.split('').flatMap(key => [
      { source: 'start', target: key }, { source: key, target: 'llm' }
    ]).concat({ source: 'llm', target: 'end' })
    expect(validateCanvasGraph(nodes, edges, codes).join()).toContain('并行区只允许 2 至 4 个查询分支')
    const four = edges.filter(edge => ![edge.source, edge.target].includes('e'))
    four.splice(four.findIndex(edge => edge.source === 'a' && edge.target === 'llm'), 1,
      { source: 'a', target: 'e' }, { source: 'e', target: 'llm' })
    expect(validateCanvasGraph(nodes, four, codes).join()).toContain('每条并行分支必须恰好一个查询节点')
  })
})
