import request, { buildQueryString, requestBinary } from './core'
import type {
  FinanceLedgerBalancePage,
  FinanceLedgerDetailPage,
  FinanceLedgerGeneralPage,
  FinanceLedgerReportMeta,
  FinanceLedgerReportQueryParams,
  FinanceLedgerSequencePage
} from './ledger-report-types'

export type {
  DetailLedgerKind,
  FinanceBalanceSheetRow,
  FinanceDetailLedgerRow,
  FinanceGeneralLedgerSection,
  FinanceLedgerBalancePage,
  FinanceLedgerDetailPage,
  FinanceLedgerGeneralPage,
  FinanceLedgerReportMeta,
  FinanceLedgerReportQueryParams,
  FinanceLedgerSequencePage,
  FinanceSequenceLedgerRow,
  LedgerReportKind
} from './ledger-report-types'

async function downloadPostBinaryFile(url: string, payload: FinanceLedgerReportQueryParams, fallbackFileName: string) {
  const response = await requestBinary(url, {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: {
      'Content-Type': 'application/json'
    },
    fallbackFileName
  })
  const objectUrl = window.URL.createObjectURL(response.blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = response.fileName || fallbackFileName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  window.URL.revokeObjectURL(objectUrl)
}

export const ledgerReportApi = {
  getMeta: (params: { companyId?: string; iyear?: number; iperiod?: number } = {}) =>
    request<FinanceLedgerReportMeta>(`/auth/finance/general-ledger/reports/meta${buildQueryString(params)}`),
  queryBalanceSheet: (payload: FinanceLedgerReportQueryParams) =>
    request<FinanceLedgerBalancePage>('/auth/finance/general-ledger/reports/balance-sheet/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  queryGeneralLedger: (payload: FinanceLedgerReportQueryParams) =>
    request<FinanceLedgerGeneralPage>('/auth/finance/general-ledger/reports/general-ledger/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  queryDetailLedger: (payload: FinanceLedgerReportQueryParams) =>
    request<FinanceLedgerDetailPage>('/auth/finance/general-ledger/reports/detail-ledger/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  querySequenceLedger: (payload: FinanceLedgerReportQueryParams) =>
    request<FinanceLedgerSequencePage>('/auth/finance/general-ledger/reports/sequence-ledger/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  exportBalanceSheet: (payload: FinanceLedgerReportQueryParams) =>
    downloadPostBinaryFile('/auth/finance/general-ledger/reports/balance-sheet/export', payload, '余额表.xlsx'),
  exportGeneralLedger: (payload: FinanceLedgerReportQueryParams) =>
    downloadPostBinaryFile('/auth/finance/general-ledger/reports/general-ledger/export', payload, '总分类账.xlsx'),
  exportDetailLedger: (payload: FinanceLedgerReportQueryParams) =>
    downloadPostBinaryFile('/auth/finance/general-ledger/reports/detail-ledger/export', payload, '明细账.xlsx'),
  exportSequenceLedger: (payload: FinanceLedgerReportQueryParams) =>
    downloadPostBinaryFile('/auth/finance/general-ledger/reports/sequence-ledger/export', payload, '序时账.xlsx')
}
