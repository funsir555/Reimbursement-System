import type { LedgerReportKind } from '@/api'

type AssistFilter = 'cdeptId' | 'cpersonId' | 'ccusId' | 'csupId' | 'citemClass' | 'citemId'

const filtersByLedger: Record<LedgerReportKind, readonly AssistFilter[]> = {
  BALANCE_SHEET: [], GENERAL_LEDGER: [], DETAIL_LEDGER: [],
  PROJECT_DETAIL_LEDGER: ['citemClass', 'citemId'],
  SUPPLIER_DETAIL_LEDGER: ['csupId'], CUSTOMER_DETAIL_LEDGER: ['ccusId'],
  PERSONAL_DETAIL_LEDGER: ['cpersonId'], QUANTITY_AMOUNT_DETAIL_LEDGER: [],
  SEQUENCE_LEDGER: ['cdeptId', 'cpersonId', 'ccusId', 'csupId', 'citemClass', 'citemId']
}

export function ledgerReportAssistFilters(kind: LedgerReportKind): readonly AssistFilter[] {
  return filtersByLedger[kind]
}
