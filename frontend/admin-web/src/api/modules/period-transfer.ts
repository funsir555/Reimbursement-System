import request, { buildQueryString } from './core'
import type {
  FinancePeriodTransferGenerateRequest,
  FinancePeriodTransferGenerateResult,
  FinancePeriodTransferMeta,
  FinancePeriodTransferPreviewRequest,
  FinancePeriodTransferPreviewResult,
  FinancePeriodTransferRule,
  FinancePeriodTransferRun,
  FinancePeriodTransferRunDetail
} from './period-transfer-types'

export type {
  FinancePeriodTransferAllocationMode,
  FinancePeriodTransferGenerateRequest,
  FinancePeriodTransferGenerateResult,
  FinancePeriodTransferGranularity,
  FinancePeriodTransferMeta,
  FinancePeriodTransferMetricType,
  FinancePeriodTransferPreviewDetail,
  FinancePeriodTransferPreviewRequest,
  FinancePeriodTransferPreviewResult,
  FinancePeriodTransferRule,
  FinancePeriodTransferRuleLine,
  FinancePeriodTransferRuleType,
  FinancePeriodTransferRun,
  FinancePeriodTransferRunDetail
} from './period-transfer-types'

export const periodTransferApi = {
  getMeta: (params: { companyId?: string; iyear?: number; iperiod?: number } = {}) =>
    request<FinancePeriodTransferMeta>(`/auth/finance/period-transfer/meta${buildQueryString(params)}`),
  listRules: (companyId: string) =>
    request<FinancePeriodTransferRule[]>(`/auth/finance/period-transfer/rules${buildQueryString({ companyId })}`),
  saveRule: (payload: FinancePeriodTransferRule) =>
    request<FinancePeriodTransferRule>('/auth/finance/period-transfer/rules', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  preview: (payload: FinancePeriodTransferPreviewRequest) =>
    request<FinancePeriodTransferPreviewResult>('/auth/finance/period-transfer/preview', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  generate: (payload: FinancePeriodTransferGenerateRequest) =>
    request<FinancePeriodTransferGenerateResult>('/auth/finance/period-transfer/generate', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listRuns: (params: { companyId: string; iyear: number; iperiod: number; ruleType?: string }) =>
    request<FinancePeriodTransferRun[]>(`/auth/finance/period-transfer/runs${buildQueryString(params)}`),
  getRunDetail: (companyId: string, runId: number) =>
    request<FinancePeriodTransferRunDetail>(`/auth/finance/period-transfer/runs/${runId}${buildQueryString({ companyId })}`)
}
