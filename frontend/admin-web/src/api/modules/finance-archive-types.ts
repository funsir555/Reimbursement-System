// 这里定义 finance-archive-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'

export interface FinanceAccountSubjectOption {
  value: string
  label: string
}

export interface FinanceAccountSubjectMeta {
  subjectCategoryOptions: FinanceAccountSubjectOption[]
  statusOptions: FinanceAccountSubjectOption[]
  closeStatusOptions: FinanceAccountSubjectOption[]
  yesNoOptions: FinanceAccountSubjectOption[]
}

// 这是 FinanceAccountSubjectSummary 的数据结构。
export interface FinanceAccountSubjectSummary {
  subject_code: string
  subject_name: string
  parent_subject_code?: string
  subject_level: number
  balance_direction?: string
  subject_category?: string
  chelp?: string
  leaf_flag: number
  status: number
  bclose: number
  bperson?: number
  bcus?: number
  bsup?: number
  bdept?: number
  bitem?: number
  bcash?: number
  bbank?: number
  br?: number
  be?: number
  auxiliary_summary?: string
  cash_bank_summary?: string
  has_children?: boolean
  template_code?: string
  sort_order?: number
  updated_at?: string
  bd_c?: number
  children: FinanceAccountSubjectSummary[]
}

export interface FinanceAccountSubjectDetail {
  id?: number
  company_id?: string
  subject_code: string
  subject_name: string
  parent_subject_code?: string
  subject_level: number
  balance_direction?: string
  subject_category?: string
  cclassany?: string
  bproperty?: number
  cbook_type?: string
  chelp?: string
  cexch_name?: string
  cmeasure?: string
  bperson?: number
  bcus?: number
  bsup?: number
  bdept?: number
  bitem?: number
  cass_item?: string
  br?: number
  be?: number
  cgather?: string
  leaf_flag?: number
  bexchange?: number
  bcash?: number
  bbank?: number
  bused?: number
  bd_c?: number
  dbegin?: string
  dend?: string
  itrans?: number
  bclose?: number
  cother?: string
  iotherused?: number
  bReport?: number
  bGCJS?: number
  bCashItem?: number
  iViewItem?: number
  bcDefine1?: number
  bcDefine2?: number
  bcDefine3?: number
  bcDefine4?: number
  bcDefine5?: number
  bcDefine6?: number
  bcDefine7?: number
  bcDefine8?: number
  bcDefine9?: number
  bcDefine10?: number
  bcDefine11?: number
  bcDefine12?: number
  bcDefine13?: number
  bcDefine14?: number
  bcDefine15?: number
  bcDefine16?: number
  status?: number
  template_code?: string
  sort_order?: number
  has_children?: boolean
  created_at?: string
  updated_at?: string
}

export interface FinanceAccountSubjectSavePayload {
  subject_code: string
  subject_name: string
  parent_subject_code?: string
  subject_level?: number
  subject_category?: string
  cclassany?: string
  bproperty?: number
  cbook_type?: string
  chelp?: string
  cexch_name?: string
  cmeasure?: string
  bperson?: number
  bcus?: number
  bsup?: number
  bdept?: number
  bitem?: number
  cass_item?: string
  br?: number
  be?: number
  cgather?: string
  leaf_flag?: number
  bexchange?: number
  bcash?: number
  bbank?: number
  bused?: number
  bd_c?: number
  dbegin?: string
  dend?: string
  itrans?: number
  bclose?: number
  cother?: string
  iotherused?: number
  bReport?: number
  bGCJS?: number
  bCashItem?: number
  iViewItem?: number
}

export interface FinanceAccountSubjectStatusPayload {
  status?: number
  bclose?: number
}

export interface FinanceProjectArchiveOption {
  value: string
  label: string
}

export interface FinanceProjectArchiveMeta {
  statusOptions: FinanceProjectArchiveOption[]
  closeStatusOptions: FinanceProjectArchiveOption[]
  projectClassOptions: FinanceProjectArchiveOption[]
}

export interface FinanceProjectClassSummary {
  id?: number
  company_id?: string
  project_class_code: string
  project_class_name: string
  status: number
  sort_order?: number
  has_projects?: boolean
  created_by?: string
  updated_by?: string
  created_at?: string
  updated_at?: string
}

export interface FinanceProjectClassSavePayload {
  project_class_code: string
  project_class_name: string
}

// 这是 FinanceProjectSummary 的数据结构。
export interface FinanceProjectSummary {
  id?: number
  company_id?: string
  citemcode: string
  citemname: string
  bclose: number
  citemccode: string
  project_class_name?: string
  iotherused?: number
  d_end_date?: string
  status: number
  sort_order?: number
  created_by?: string
  updated_by?: string
  created_at?: string
  updated_at?: string
  referenced_by_voucher?: boolean
}

export interface FinanceProjectDetail extends FinanceProjectSummary {}

export interface FinanceProjectSavePayload {
  citemcode: string
  citemname: string
  citemccode: string
  iotherused?: number
  d_end_date?: string
}

export interface FinanceProjectStatusPayload {
  status?: number
  bclose?: number
}

export interface FinanceDepartmentArchiveOption {
  value: string
  label: string
}

export interface FinanceDepartmentTreeNode {
  id: number
  companyId?: string
  deptCode: string
  leaderUserId?: number
  leaderName?: string
  deptName: string
  parentId?: number
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  syncSource: string
  syncManaged: boolean
  syncEnabled: boolean
  syncStatus?: string
  syncRemark?: string
  status: number
  sortOrder: number
  lastSyncAt?: string
  children: FinanceDepartmentTreeNode[]
}

export interface FinanceDepartmentArchiveMeta {
  departments: FinanceDepartmentTreeNode[]
  statusOptions: FinanceDepartmentArchiveOption[]
}

export interface FinanceDepartmentQueryPayload {
  keyword?: string
  parentId?: number
  status?: number
}

export interface FinanceDepartmentSummary {
  id: number
  deptCode: string
  deptName: string
  parentId?: number
  parentDeptName?: string
  companyId?: string
  companyName?: string
  leaderUserId?: number
  leaderName?: string
  status: number
  syncSource?: string
  syncManaged?: boolean
  syncEnabled?: boolean
  syncStatus?: string
  syncRemark?: string
  sortOrder?: number
  lastSyncAt?: string
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
}

export interface FinanceVendorSummary {
  cVenCode: string
  cVenName: string
  cVenAbbName?: string
  cVCCode?: string
  cVenPerson?: string
  cVenPhone?: string
  cVenBank?: string
  cVenAccount?: string
  companyId?: string
  active: boolean
  dEndDate?: string
  updatedAt?: string
}

export interface FinanceVendorDetail extends FinanceVendorSummary {
  cTrade?: string
  cVenAddress?: string
  cVenRegCode?: string
  cVenBankNub?: string
  receiptAccountName?: string
  receiptBankProvince?: string
  receiptBankCity?: string
  receiptBranchCode?: string
  receiptBranchName?: string
  cVenHand?: string
  cVenEmail?: string
  cMemo?: string
  cBarCode?: string
  cCreatePerson?: string
  cDCCode?: string
  cModifyPerson?: string
  cRelCustomer?: string
  cVenBankCode?: string
  cVenBP?: string
  cVenDefine3?: string
  cVenDefine4?: string
  cVenDefine5?: string
  cVenDefine6?: string
  cVenDefine7?: string
  cVenDefine8?: string
  cVenDefine9?: string
  cVenDefine10?: string
  cVenDefine11?: number
  cVenDefine12?: number
  cVenDefine13?: number
  cVenDefine14?: number
  cVenDefine15?: string
  cVenDefine16?: string
  cVenDepart?: string
  cVenFax?: string
  cVenHeadCode?: string
  cVenIAddress?: string
  cVenIType?: string
  cVenLPerson?: string
  cVenPayCond?: string
  cVenPostCode?: string
  cVenPPerson?: string
  cVenTradeCCode?: string
  cVenWhCode?: string
  dBusinessEDate?: string
  dBusinessSDate?: string
  dLastDate?: string
  dLicenceEDate?: string
  dLicenceSDate?: string
  dLRDate?: string
  dModifyDate?: string
  dProxyEDate?: string
  dProxySDate?: string
  dVenDevDate?: string
  fRegistFund?: MoneyValue
  iAPMoney?: MoneyValue
  iBusinessADays?: number
  iEmployeeNum?: number
  iFrequency?: number
  iGradeABC?: number
  iId?: number
  iLastMoney?: MoneyValue
  iLicenceADays?: number
  iLRMoney?: MoneyValue
  iProxyADays?: number
  iVenCreDate?: number
  iVenCreGrade?: string
  iVenCreLine?: MoneyValue
  iVenDisRate?: number
  bBusinessDate?: number
  bLicenceDate?: number
  bPassGMP?: number
  bProxyDate?: number
  bProxyForeign?: number
  bVenCargo?: number
  bVenService?: number
  bVenTax?: number
  createdAt?: string
  [key: string]: unknown
}

export type FinanceVendorSavePayload = Partial<FinanceVendorDetail> & {
  cVenCode?: string
  cVenName: string
}

// 这是 FinanceCustomerSummary 的数据结构。
export interface FinanceCustomerSummary {
  cCusCode: string
  cCusName: string
  cCusAbbName?: string
  cCusPerson?: string
  cCusHand?: string
  cCusBank?: string
  cCusAccount?: string
  iARMoney?: MoneyValue
  companyId?: string
  active: boolean
  dEndDate?: string
  updatedAt?: string
}

export interface FinanceCustomerDetail extends FinanceCustomerSummary {
  cCCCode?: string
  cDCCode?: string
  cCusTradeCCode?: string
  cTrade?: string
  cCusAddress?: string
  cCusPostCode?: string
  cCusRegCode?: string
  cCusLPerson?: string
  cCusCreGrade?: string
  iCusCreLine?: MoneyValue
  iCusCreDate?: number
  cCusOAddress?: string
  cCusOType?: string
  cCusHeadCode?: string
  cCusWhCode?: string
  cCusDepart?: string
  dLastDate?: string
  iLastMoney?: MoneyValue
  dLRDate?: string
  iLRMoney?: MoneyValue
  cCusBankCode?: string
  cCusDefine1?: string
  cCusDefine2?: string
  cCusDefine3?: string
  cCusDefine4?: string
  cCusDefine5?: string
  cCusDefine6?: string
  cCusDefine7?: string
  cCusDefine8?: string
  cCusDefine9?: string
  cCusDefine10?: string
  cCusDefine11?: number
  cCusDefine12?: number
  cCusDefine13?: number
  cCusDefine14?: number
  cCusDefine15?: string
  cCusDefine16?: string
  cInvoiceCompany?: string
  bCredit?: number
  bCreditDate?: number
  bCreditByHead?: number
  cMemo?: string
  fCommisionRate?: number
  fInsueRate?: number
  customerKCode?: string
  bCusState?: number
  createdAt?: string
  [key: string]: unknown
}

export type FinanceCustomerSavePayload = Partial<FinanceCustomerDetail> & {
  cCusCode?: string
  cCusName: string
}
