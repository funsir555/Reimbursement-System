import { ElMessage } from 'element-plus'
import type { ComputedRef, Ref } from 'vue'
import type { FinanceVoucherEntry, FinanceVoucherOption } from '@/api'

export type FinanceVoucherEntryRow = FinanceVoucherEntry & { localId: string }

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
}

export function useFinanceNewVoucherRowOwner(options: UseFinanceNewVoucherRowOwnerOptions) {
  let entrySeed = 0

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

  function handleAmountKeydown(event: KeyboardEvent, index: number, field: 'md' | 'mc') {
    handleGridKeydown(event, index)
    if (options.isReadonlyMode.value) return
    const row = options.getEntries()[index]
    if (!row) return
    if (field === 'md' && row.md) {
      row.mc = ''
      row.ncS = undefined
    }
    if (field === 'mc' && row.mc) {
      row.md = ''
      row.ndS = undefined
    }
  }

  return {
    createEntry,
    createEntryFromValue,
    ensureMinimumRows,
    selectRow,
    handleEntryFieldFocus,
    insertEntryAfter,
    removeSelectedEntry,
    handleGridKeydown,
    handleAmountKeydown
  }
}
