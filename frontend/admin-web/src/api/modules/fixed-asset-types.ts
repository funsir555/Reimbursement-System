// 这里定义 fixed-asset-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'

export interface FixedAssetOption {
  value: string
  label: string
}

// 这是 FixedAssetMeta 的数据结构。
export interface FixedAssetMeta {
  companyOptions: FixedAssetOption[]
  departmentOptions: FixedAssetOption[]
  employeeOptions: FixedAssetOption[]
  categoryOptions: FixedAssetOption[]
  depreciationMethodOptions: FixedAssetOption[]
  cardStatusOptions: FixedAssetOption[]
  changeTypeOptions: FixedAssetOption[]
  bookOptions: FixedAssetOption[]
  defaultCompanyId?: string
  defaultBookCode: string
  defaultFiscalYear: number
  defaultFiscalPeriod: number
  periodStatus: string
  cardCount: number
  pendingDepreciationCount: number
  currentPeriodDepreciationAmount: MoneyValue
}

export interface FixedAssetCategoryPayload {
  id?: number
  companyId: string
  categoryCode: string
  categoryName: string
  shareScope: string
  depreciationMethod: string
  usefulLifeMonths: number
  residualRate: number
  depreciable?: boolean
  status?: string
  remark?: string
  bookCode?: string
  assetAccount: string
  accumDeprAccount: string
  deprExpenseAccount: string
  disposalAccount: string
  gainAccount: string
  lossAccount: string
  offsetAccount: string
}

export type FixedAssetCategory = FixedAssetCategoryPayload

// 这是 FixedAssetCardPayload 的数据结构。
export interface FixedAssetCardPayload {
  id?: number
  companyId: string
  assetCode: string
  assetName: string
  categoryId: number
  bookCode?: string
  useCompanyId?: string
  useDeptId?: number
  keeperUserId?: number
  managerUserId?: number
  sourceType?: string
  acquireDate?: string
  inServiceDate: string
  originalAmount: MoneyValue
  accumDeprAmount: MoneyValue
  salvageAmount: MoneyValue
  usefulLifeMonths: number
  depreciatedMonths: number
  remainingMonths: number
  workTotal?: number
  workUsed?: number
  status?: string
  canDepreciate?: boolean
  remark?: string
}

export interface FixedAssetCard extends FixedAssetCardPayload {
  categoryCode: string
  categoryName?: string
  depreciationMethod?: string
  useDeptName?: string
  keeperName?: string
  managerName?: string
  netAmount: MoneyValue
  lastDeprYear?: number
  lastDeprPeriod?: number
}

export interface FixedAssetOpeningImportRow {
  rowNo: number
  assetCode: string
  assetName: string
  categoryCode: string
  acquireDate?: string
  inServiceDate: string
  originalAmount: MoneyValue
  accumDeprAmount: MoneyValue
  salvageAmount: MoneyValue
  usefulLifeMonths: number
  depreciatedMonths: number
  remainingMonths: number
  useDeptId?: number
  keeperUserId?: number
  status?: string
  workTotal?: number
  workUsed?: number
  remark?: string
}

export interface FixedAssetOpeningImportPayload {
  companyId: string
  bookCode?: string
  fiscalYear?: number
  fiscalPeriod?: number
  rows: FixedAssetOpeningImportRow[]
}

export interface FixedAssetOpeningImportLine {
  rowNo: number
  assetCode?: string
  assetName?: string
  categoryCode?: string
  resultStatus: string
  errorMessage?: string
  importedAssetId?: number
}

export interface FixedAssetOpeningImportResult {
  batchId: number
  companyId: string
  batchNo: string
  bookCode: string
  fiscalYear: number
  fiscalPeriod: number
  status: string
  totalRows: number
  successRows: number
  failedRows: number
  lines: FixedAssetOpeningImportLine[]
}

export interface FixedAssetTemplate {
  fileName: string
  contentType: string
  templateContent: string
}

export interface FixedAssetChangeLinePayload {
  assetId?: number
  assetCode: string
  assetName?: string
  categoryId?: number
  categoryCode?: string
  useCompanyId?: string
  useDeptId?: number
  keeperUserId?: number
  inServiceDate?: string
  changeAmount?: MoneyValue
  newValue?: MoneyValue
  newSalvageAmount?: MoneyValue
  newUsefulLifeMonths?: number
  newRemainingMonths?: number
  remark?: string
}

export interface FixedAssetChangeBillPayload {
  companyId: string
  billType: string
  bookCode?: string
  fiscalYear?: number
  fiscalPeriod?: number
  billDate?: string
  remark?: string
  lines: FixedAssetChangeLinePayload[]
}

export interface FixedAssetVoucherLink {
  id: number
  companyId: string
  businessType: string
  businessId: number
  voucherNo: string
  iperiod: number
  csign: string
  inoId: number
  remark?: string
}

export interface FixedAssetChangeLine {
  id: number
  assetId?: number
  assetCode: string
  assetName?: string
  changeType: string
  categoryId?: number
  categoryCode?: string
  useCompanyId?: string
  useDeptId?: number
  useDeptName?: string
  keeperUserId?: number
  keeperName?: string
  inServiceDate?: string
  changeAmount?: MoneyValue
  oldValue?: MoneyValue
  newValue?: MoneyValue
  oldSalvageAmount?: MoneyValue
  newSalvageAmount?: MoneyValue
  oldUsefulLifeMonths?: number
  newUsefulLifeMonths?: number
  oldRemainingMonths?: number
  newRemainingMonths?: number
  remark?: string
}

export interface FixedAssetChangeBill {
  id: number
  companyId: string
  billNo: string
  billType: string
  bookCode: string
  fiscalYear: number
  fiscalPeriod: number
  billDate: string
  status: string
  totalAmount: MoneyValue
  remark?: string
  postedAt?: string
  voucherLink?: FixedAssetVoucherLink | null
  lines: FixedAssetChangeLine[]
}

export interface FixedAssetDeprWorkload {
  assetId: number
  workAmount: number
}

export interface FixedAssetDeprPreviewPayload {
  companyId: string
  bookCode?: string
  fiscalYear?: number
  fiscalPeriod?: number
  assetIds?: number[]
  workloads?: FixedAssetDeprWorkload[]
  remark?: string
}

export interface FixedAssetDeprLine {
  id?: number
  assetId: number
  assetCode: string
  assetName: string
  categoryId: number
  categoryName?: string
  depreciationMethod: string
  workAmount?: number
  depreciationAmount: MoneyValue
  beforeAccumAmount: MoneyValue
  afterAccumAmount: MoneyValue
  beforeNetAmount: MoneyValue
  afterNetAmount: MoneyValue
}

export interface FixedAssetDeprRun {
  id?: number
  companyId: string
  runNo?: string
  bookCode: string
  fiscalYear: number
  fiscalPeriod: number
  status: string
  assetCount: number
  totalAmount: MoneyValue
  remark?: string
  postedAt?: string
  voucherLink?: FixedAssetVoucherLink | null
  lines: FixedAssetDeprLine[]
}

export interface FixedAssetDisposalLinePayload {
  assetId?: number
  assetCode: string
  remark?: string
}

export interface FixedAssetDisposalBillPayload {
  companyId: string
  bookCode?: string
  fiscalYear?: number
  fiscalPeriod?: number
  billDate?: string
  remark?: string
  lines: FixedAssetDisposalLinePayload[]
}

export interface FixedAssetDisposalLine {
  id: number
  assetId: number
  assetCode: string
  assetName: string
  categoryId: number
  categoryName?: string
  originalAmount: MoneyValue
  accumDeprAmount: MoneyValue
  netAmount: MoneyValue
  remark?: string
}

export interface FixedAssetDisposalBill {
  id: number
  companyId: string
  billNo: string
  billType: string
  bookCode: string
  fiscalYear: number
  fiscalPeriod: number
  billDate: string
  status: string
  totalOriginalAmount: MoneyValue
  totalAccumAmount: MoneyValue
  totalNetAmount: MoneyValue
  remark?: string
  postedAt?: string
  voucherLink?: FixedAssetVoucherLink | null
  lines: FixedAssetDisposalLine[]
}

export interface FixedAssetPeriodClosePayload {
  companyId: string
  bookCode?: string
  fiscalYear?: number
  fiscalPeriod?: number
}

export interface FixedAssetPeriodStatus {
  companyId: string
  bookCode: string
  fiscalYear: number
  fiscalPeriod: number
  status: string
  closedBy?: string
  closedAt?: string
}
