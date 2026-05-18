import { ElMessage } from 'element-plus'
import { nextTick } from 'vue'
import type { ComputedRef, Ref } from 'vue'
import type { FinanceVoucherEntry, FinanceVoucherOption } from '@/api'

export type FinanceVoucherEntryRow = FinanceVoucherEntry & { localId: string }
export type FinanceVoucherGridField = 'cdigest' | 'ccode' | 'md' | 'mc'

type UseFinanceNewVoucherRowOwnerOptions = {
  getEntries: () => FinanceVoucherEntryRow[]
  setEntries: (entries: FinanceVoucherEntryRow[]) => void
  selectedRow: ComputedRef<FinanceVoucherEntryRow>
  selectedRowIndex: Ref<number>
  effectiveRowCount: ComputedRef<number>
  isReadonlyMode: ComputedRef<boolean>
  defaultCurrencyCode: ComputedRef<string>
  defaultCurrencyName: ComputedRef<string>
  minEntryRows: number
  isEntryBlank: (row: FinanceVoucherEntryRow) => boolean
  tryLeaveSubjectField: (nextRowIndex?: number) => Promise<boolean>
  resetLeafSubjectHistory: (rows: FinanceVoucherEntryRow[], accountOptions?: FinanceVoucherOption[]) => void
  focusGridCell: (index: number, field: FinanceVoucherGridField) => Promise<void> | void
}

export function useFinanceNewVoucherRowOwner(options: UseFinanceNewVoucherRowOwnerOptions) {
  let entrySeed = 0
  const gridFieldOrder: FinanceVoucherGridField[] = ['cdigest', 'ccode', 'md', 'mc']

  function createEntry(defaultCurrencyCode: string, defaultCurrencyName: string, rowNo: number): FinanceVoucherEntryRow {
    entrySeed += 1
    return {
      localId: `entry-${Date.now()}-${entrySeed}`,
      inid: rowNo,
      cdigest: '',
      ccode: '',
      cdeptId: '',
      cpersonId: '',
      ccusId: '',
      csupId: '',
      citemClass: '',
      citemId: '',
      cashFlowItemId: undefined,
      cashFlowItemName: '',
      cashFlowSubjectCode: '',
      cashFlowSubjectName: '',
      cashFlowAmount: '',
      cexchName: defaultCurrencyName,
      currencyCode: defaultCurrencyCode,
      nfrat: 1,
      md: '',
      mc: '',
      ndS: undefined,
      ncS: undefined
    }
  }

  function createEntryFromValue(
    entry: FinanceVoucherEntry,
    defaultCurrencyCode: string,
    defaultCurrencyName: string,
    rowNo: number
  ): FinanceVoucherEntryRow {
    return {
      ...createEntry(defaultCurrencyCode, defaultCurrencyName, rowNo),
      ...entry,
      inid: rowNo,
      cexchName: entry.cexchName || defaultCurrencyName,
      currencyCode: entry.currencyCode || defaultCurrencyCode,
      nfrat: entry.nfrat ?? 1
    }
  }

  function ensureMinimumRows(
    entries: FinanceVoucherEntryRow[],
    defaultCurrencyCode: string,
    defaultCurrencyName: string,
    minRows = options.minEntryRows
  ) {
    const nextEntries = [...entries]
    while (nextEntries.length < minRows) nextEntries.push(createEntry(defaultCurrencyCode, defaultCurrencyName, nextEntries.length + 1))
    return nextEntries.map((item, index) => ({ ...item, inid: index + 1 }))
  }

  function selectRow(index: number) {
    const entries = options.getEntries()
    options.selectedRowIndex.value = Math.max(0, Math.min(index, entries.length - 1))
  }

  function handleEntryFieldFocus(index: number) {
    void options.tryLeaveSubjectField(index)
  }

  function updateAmountField(index: number, field: 'md' | 'mc', value: string) {
    if (options.isReadonlyMode.value) return
    const row = options.getEntries()[index]
    if (!row) return
    row[field] = value
    if (value) {
      if (field === 'md') {
        row.mc = ''
        row.ncS = undefined
      } else {
        row.md = ''
        row.ndS = undefined
      }
    }
  }

  function insertEntryAfter(index: number) {
    if (options.isReadonlyMode.value) return
    const entries = [...options.getEntries()]
    const currencyCode = options.defaultCurrencyCode.value
    const currencyName = options.defaultCurrencyName.value
    entries.splice(index + 1, 0, createEntry(currencyCode, currencyName, index + 2))
    options.setEntries(ensureMinimumRows(entries, currencyCode, currencyName, Math.max(entries.length, options.minEntryRows)))
    options.resetLeafSubjectHistory(options.getEntries())
    selectRow(index + 1)
  }

  function removeSelectedEntry() {
    if (options.isReadonlyMode.value) return
    if (options.effectiveRowCount.value <= 2 && !options.isEntryBlank(options.selectedRow.value)) {
      ElMessage.warning('至少保留两条有效分录')
      return
    }
    const entries = [...options.getEntries()]
    entries.splice(options.selectedRowIndex.value, 1)
    options.setEntries(ensureMinimumRows(entries, options.defaultCurrencyCode.value, options.defaultCurrencyName.value, Math.max(entries.length, 2)))
    options.resetLeafSubjectHistory(options.getEntries())
    selectRow(Math.max(0, options.selectedRowIndex.value - 1))
  }

  function handleGridKeydown(event: KeyboardEvent, index: number) {
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      void options.tryLeaveSubjectField(index - 1)
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      void options.tryLeaveSubjectField(index + 1)
    }
    if (!options.isReadonlyMode.value && event.key === 'Insert') {
      event.preventDefault()
      insertEntryAfter(index)
    }
  }

  async function handleGridCellKeydown(event: KeyboardEvent, index: number, field: FinanceVoucherGridField) {
    handleGridKeydown(event, index)
    if (event.key !== 'Enter' && event.key !== 'Tab') {
      return
    }
    event.preventDefault()
    await moveGridFocus(index, field, event.shiftKey ? -1 : 1)
  }

  function handleAmountKeydown(event: KeyboardEvent, index: number, field: 'md' | 'mc') {
    void handleGridCellKeydown(event, index, field)
  }

  async function moveGridFocus(index: number, field: FinanceVoucherGridField, direction: -1 | 1) {
    const currentFieldIndex = gridFieldOrder.indexOf(field)
    if (currentFieldIndex < 0) {
      return
    }
    const currentEntries = options.getEntries()
    let nextRowIndex = index
    let nextFieldIndex = currentFieldIndex + direction
    if (nextFieldIndex < 0) {
      nextRowIndex = Math.max(0, index - 1)
      nextFieldIndex = gridFieldOrder.length - 1
    } else if (nextFieldIndex >= gridFieldOrder.length) {
      nextRowIndex = index + 1
      nextFieldIndex = 0
    }

    if (!(await options.tryLeaveSubjectField(nextRowIndex))) {
      return
    }

    if (nextRowIndex >= currentEntries.length) {
      if (options.isReadonlyMode.value) {
        return
      }
      insertEntryAfter(currentEntries.length - 1)
      await nextTick()
      nextRowIndex = Math.min(currentEntries.length, options.getEntries().length - 1)
    }

    const boundedRowIndex = Math.max(0, Math.min(nextRowIndex, options.getEntries().length - 1))
    selectRow(boundedRowIndex)
    await nextTick()
    await options.focusGridCell(boundedRowIndex, gridFieldOrder[nextFieldIndex] as FinanceVoucherGridField)
  }

  return {
    createEntry,
    createEntryFromValue,
    ensureMinimumRows,
    selectRow,
    handleEntryFieldFocus,
    updateAmountField,
    insertEntryAfter,
    removeSelectedEntry,
    handleGridKeydown,
    handleGridCellKeydown,
    handleAmountKeydown
  }
}
