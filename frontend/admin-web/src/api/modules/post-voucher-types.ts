import type { AsyncTaskSubmitResult } from './async-task-types'

export interface FinancePostVoucherMeta {
  companyId: string
  companyName: string
  iyear: number
  iperiod: number
  iyperiod: number
  periodLabel: string
  status: string
  statusLabel: string
  canPost: boolean
  blockedReason?: string
  unpostedVoucherCount: number
  unpostedSampleVoucherNos: string[]
  errorVoucherCount: number
  errorSampleVoucherNos: string[]
  reviewableVoucherCount: number
  postedVoucherCount: number
  lastTaskNo?: string
  lastTaskStatus?: string
  lastTaskMessage?: string
}

export interface FinancePostVoucherTaskRequest {
  companyId: string
  iyear: number
  iperiod: number
}

export interface FinancePostVoucherTaskStatus {
  taskNo: string
  taskType: string
  businessType: string
  status: string
  progress: number
  resultMessage?: string
  periodStatus: string
  periodStatusLabel: string
  postedVoucherCount: number
  reviewableVoucherCount: number
  finished: boolean
  createdAt?: string
  updatedAt?: string
  finishedAt?: string
}

export type FinancePostVoucherTaskSubmitResult = AsyncTaskSubmitResult
