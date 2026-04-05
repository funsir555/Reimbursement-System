import { beforeEach, describe, expect, it } from 'vitest'
import {
  EXPENSE_WORKBENCH_COLUMN_WIDTHS_STORAGE_KEY,
  EXPENSE_WORKBENCH_DEFAULT_COLUMN_ORDER,
  buildColumnGridDisplayOrder,
  loadColumnOrder,
  loadColumnWidths,
  moveColumnOrder,
  resolveVisibleColumnDefinitions,
  saveColumnWidth,
  type ExpenseWorkbenchColumnKey
} from '@/views/expense/expenseWorkbenchListHelper'

const allowedColumns: ExpenseWorkbenchColumnKey[] = [
  'documentCode',
  'documentTitle',
  'submitterName',
  'amount',
  'taskCreatedAt'
]

describe('expenseWorkbenchListHelper', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('builds column-first display order for the field grid', () => {
    expect(buildColumnGridDisplayOrder(['A', 'B', 'C', 'D', 'E', 'F'])).toEqual(['A', 'D', 'B', 'E', 'C', 'F'])
  })

  it('moves dragged columns ahead of the drop target', () => {
    expect(moveColumnOrder(['documentCode', 'amount', 'submitterName'], 'amount', 'documentCode')).toEqual([
      'amount',
      'documentCode',
      'submitterName'
    ])
  })

  it('hydrates missing fields when loading stored column order', () => {
    window.localStorage.setItem('expense:list:column-order', JSON.stringify(['amount', 'documentCode']))

    expect(loadColumnOrder('expense:list:column-order', allowedColumns, allowedColumns)).toEqual([
      'amount',
      'documentCode',
      'documentTitle',
      'submitterName',
      'taskCreatedAt'
    ])
  })

  it('loads only valid shared column widths', () => {
    window.localStorage.setItem(
      EXPENSE_WORKBENCH_COLUMN_WIDTHS_STORAGE_KEY,
      JSON.stringify({
        documentCode: 260,
        amount: 'bad',
        unknown: 999,
        submitterName: 0
      })
    )

    expect(loadColumnWidths(allowedColumns)).toEqual({
      documentCode: 260
    })
  })

  it('resolves visible columns by saved order and shared width', () => {
    const columnWidths = saveColumnWidth('documentCode', 260, {})

    expect(
      resolveVisibleColumnDefinitions(
        ['documentCode', 'amount'],
        moveColumnOrder(EXPENSE_WORKBENCH_DEFAULT_COLUMN_ORDER, 'amount', 'documentCode'),
        columnWidths
      ).map((column) => ({ key: column.key, width: column.width }))
    ).toEqual([
      { key: 'amount', width: 140 },
      { key: 'documentCode', width: 260 }
    ])
  })
})
