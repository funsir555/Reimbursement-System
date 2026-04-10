import request, { buildQueryString } from './core'
import type { FixedAssetCard, FixedAssetCardPayload, FixedAssetCategory, FixedAssetCategoryPayload, FixedAssetChangeBill, FixedAssetChangeBillPayload, FixedAssetDeprPreviewPayload, FixedAssetDeprRun, FixedAssetDisposalBill, FixedAssetDisposalBillPayload, FixedAssetMeta, FixedAssetOpeningImportPayload, FixedAssetOpeningImportResult, FixedAssetPeriodClosePayload, FixedAssetPeriodStatus, FixedAssetTemplate, FixedAssetVoucherLink } from './fixed-asset-types'

export const fixedAssetApi = {
  getMeta: (params: { companyId?: string; fiscalYear?: number; fiscalPeriod?: number } = {}) =>
    request<FixedAssetMeta>(`/auth/finance/fixed-assets/meta${buildQueryString(params)}`),
  listCategories: (companyId: string) =>
    request<FixedAssetCategory[]>(`/auth/finance/fixed-assets/categories${buildQueryString({ companyId })}`),
  createCategory: (payload: FixedAssetCategoryPayload) =>
    request<FixedAssetCategory>('/auth/finance/fixed-assets/categories', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCategory: (id: number, payload: FixedAssetCategoryPayload) =>
    request<FixedAssetCategory>(`/auth/finance/fixed-assets/categories/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  listCards: (params: { companyId: string; bookCode?: string; keyword?: string; categoryId?: number; status?: string }) =>
    request<FixedAssetCard[]>(`/auth/finance/fixed-assets/cards${buildQueryString(params)}`),
  getCard: (id: number) =>
    request<FixedAssetCard>(`/auth/finance/fixed-assets/cards/${id}`),
  createCard: (payload: FixedAssetCardPayload) =>
    request<FixedAssetCard>('/auth/finance/fixed-assets/cards', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCard: (id: number, payload: FixedAssetCardPayload) =>
    request<FixedAssetCard>(`/auth/finance/fixed-assets/cards/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  getOpeningTemplate: (params: { companyId: string; bookCode?: string; fiscalYear?: number; fiscalPeriod?: number }) =>
    request<FixedAssetTemplate>(`/auth/finance/fixed-assets/opening-import/template${buildQueryString(params)}`, {
      method: 'POST'
    }),
  importOpening: (payload: FixedAssetOpeningImportPayload) =>
    request<FixedAssetOpeningImportResult>('/auth/finance/fixed-assets/opening-import', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getOpeningImportDetail: (batchId: number) =>
    request<FixedAssetOpeningImportResult>(`/auth/finance/fixed-assets/opening-import/${batchId}`),
  listChangeBills: (params: { companyId: string; bookCode?: string; fiscalYear?: number; fiscalPeriod?: number }) =>
    request<FixedAssetChangeBill[]>(`/auth/finance/fixed-assets/change-bills${buildQueryString(params)}`),
  createChangeBill: (payload: FixedAssetChangeBillPayload) =>
    request<FixedAssetChangeBill>('/auth/finance/fixed-assets/change-bills', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  postChangeBill: (id: number) =>
    request<FixedAssetChangeBill>(`/auth/finance/fixed-assets/change-bills/${id}/post`, {
      method: 'POST'
    }),
  previewDepreciation: (payload: FixedAssetDeprPreviewPayload) =>
    request<FixedAssetDeprRun>('/auth/finance/fixed-assets/depreciation-runs/preview', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listDepreciationRuns: (params: { companyId: string; bookCode?: string; fiscalYear?: number; fiscalPeriod?: number }) =>
    request<FixedAssetDeprRun[]>(`/auth/finance/fixed-assets/depreciation-runs${buildQueryString(params)}`),
  createDepreciationRun: (payload: FixedAssetDeprPreviewPayload) =>
    request<FixedAssetDeprRun>('/auth/finance/fixed-assets/depreciation-runs', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  postDepreciationRun: (id: number) =>
    request<FixedAssetDeprRun>(`/auth/finance/fixed-assets/depreciation-runs/${id}/post`, {
      method: 'POST'
    }),
  listDisposalBills: (params: { companyId: string; bookCode?: string; fiscalYear?: number; fiscalPeriod?: number }) =>
    request<FixedAssetDisposalBill[]>(`/auth/finance/fixed-assets/disposal-bills${buildQueryString(params)}`),
  createDisposalBill: (payload: FixedAssetDisposalBillPayload) =>
    request<FixedAssetDisposalBill>('/auth/finance/fixed-assets/disposal-bills', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  postDisposalBill: (id: number) =>
    request<FixedAssetDisposalBill>(`/auth/finance/fixed-assets/disposal-bills/${id}/post`, {
      method: 'POST'
    }),
  closePeriod: (payload: FixedAssetPeriodClosePayload) =>
    request<FixedAssetPeriodStatus>('/auth/finance/fixed-assets/period-close', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getPeriodStatus: (params: { companyId: string; bookCode?: string; fiscalYear?: number; fiscalPeriod?: number }) =>
    request<FixedAssetPeriodStatus>(`/auth/finance/fixed-assets/period-close/status${buildQueryString(params)}`),
  getVoucherLink: (params: { companyId: string; businessType: string; businessId: number }) =>
    request<FixedAssetVoucherLink | null>(`/auth/finance/fixed-assets/voucher-link${buildQueryString(params)}`)
}
