import { describe, expect, it } from 'vitest'
import {
  appendMissingDepartmentTreeOptions,
  buildDepartmentLabelMap,
  buildDepartmentTreeOptions,
  filterDepartmentTreeNode,
  formatDepartmentOptionLabel,
  normalizeDepartmentValue
} from '@/utils/departmentTree'

describe('departmentTree helpers', () => {
  it('builds a department tree from flat option data', () => {
    const tree = buildDepartmentTreeOptions([
      { value: '10', code: '010', name: 'Finance' },
      { value: '11', code: '0101', name: 'Shared Service', parentValue: '10' }
    ])

    expect(tree.map((item) => item.value)).toEqual(['10'])
    expect(tree[0]?.label).toBe('010  Finance')
    expect(tree[0]?.children.map((item) => item.value)).toEqual(['11'])
  })

  it('keeps orphan nodes at root instead of dropping them', () => {
    const tree = buildDepartmentTreeOptions([
      { value: '11', code: '0101', name: 'Shared Service', parentValue: '10' }
    ])

    expect(tree).toHaveLength(1)
    expect(tree[0]?.value).toBe('11')
  })

  it('normalizes system department tree nodes with number values', () => {
    const tree = buildDepartmentTreeOptions([
      {
        id: 10,
        deptCode: '010',
        deptName: 'Finance',
        children: [{ id: 11, deptCode: '0101', deptName: 'Shared Service', parentId: 10, children: [] }]
      }
    ], { valueType: 'number' })

    expect(tree[0]?.value).toBe(10)
    expect(tree[0]?.children[0]?.value).toBe(11)
    expect(normalizeDepartmentValue('11', 'number')).toBe(11)
  })

  it('searches by label, code, name, and value', () => {
    const node = { value: '11', label: '0101  Shared Service', code: '0101', name: 'Shared Service' }

    expect(filterDepartmentTreeNode('shared', node)).toBe(true)
    expect(filterDepartmentTreeNode('0101', node)).toBe(true)
    expect(filterDepartmentTreeNode('11', node)).toBe(true)
    expect(filterDepartmentTreeNode('sales', node)).toBe(false)
  })

  it('formats labels and preserves missing selected values', () => {
    expect(formatDepartmentOptionLabel({ code: '010', name: 'Finance' })).toBe('010  Finance')

    const tree = appendMissingDepartmentTreeOptions(
      buildDepartmentTreeOptions([{ value: '10', name: 'Finance' }]),
      ['10', '99']
    )

    expect(tree.map((item) => item.value)).toEqual(['10', '99'])
    expect(buildDepartmentLabelMap(tree).get('99')).toBe('99')
  })
})
