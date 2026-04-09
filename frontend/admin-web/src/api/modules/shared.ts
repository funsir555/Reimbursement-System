import type { MoneyValue } from './core'

export interface UserProfile {
  userId: number
  username: string
  name: string
  phone?: string
  email?: string
  position?: string
  laborRelationBelong?: string
  companyId?: string
  roles: string[]
  permissionCodes: string[]
}

export interface LoginResponse {
  userId: number
  username: string
  name: string
  token: string
  expireIn: number
  roles: string[]
  permissionCodes: string[]
}

export interface DepartmentTreeNode {
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
  children: DepartmentTreeNode[]
}

export interface DepartmentSavePayload {
  companyId?: string
  deptCode?: string
  leaderUserId?: number
  deptName: string
  parentId?: number
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  status?: number
  sortOrder?: number
  syncEnabled?: number
}

export interface EmployeeRecord {
  userId: number
  username: string
  name: string
  phone?: string
  email?: string
  companyId?: string
  companyName?: string
  deptId?: number
  deptName?: string
  position?: string
  laborRelationBelong?: string
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  status: number
  sourceType: string
  syncManaged: boolean
  lastSyncAt?: string
  roleCodes: string[]
}

export interface EmployeeSavePayload {
  username: string
  name: string
  phone?: string
  email?: string
  companyId?: string
  deptId?: number
  position?: string
  laborRelationBelong?: string
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  status?: number
}

export interface EmployeeEditorFormState extends EmployeeSavePayload {
  userId?: number
  roleIds: number[]
}

export interface EmployeeQueryPayload {
  keyword?: string
  companyId?: string
  deptId?: number
  status?: number
}

export interface FinanceEmployeeArchiveMeta {
  companies: CompanyRecord[]
  departments: DepartmentTreeNode[]
}

export interface RoleRecord {
  id: number
  roleCode: string
  roleName: string
  roleDescription?: string
  status: number
  permissionCodes: string[]
  userIds: number[]
  userNames: string[]
}

export interface RoleSavePayload {
  roleCode?: string
  roleName: string
  roleDescription?: string
  status?: number
}

export interface PermissionTreeNode {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: string
  parentId?: number
  moduleCode?: string
  routePath?: string
  sortOrder?: number
  children: PermissionTreeNode[]
}

export interface CompanyRecord {
  companyId: string
  companyCode: string
  companyName: string
  invoiceTitle?: string
  taxNo?: string
  status: number
  children: CompanyRecord[]
}

export interface CompanyBankAccountRecord {
  id: number
  companyId: string
  companyName?: string
  bankName: string
  branchName?: string
  bankCode?: string
  branchCode?: string
  cnapsCode?: string
  accountName: string
  accountNo: string
  accountType?: string
  accountUsage?: string
  currencyCode?: string
  defaultAccount: number
  status: number
  remark?: string
  directConnectEnabled: number
  directConnectProvider?: string
  directConnectChannel?: string
  directConnectProtocol?: string
  directConnectCustomerNo?: string
  directConnectAppId?: string
  directConnectAccountAlias?: string
  directConnectAuthMode?: string
  directConnectApiBaseUrl?: string
  directConnectCertRef?: string
  directConnectSecretRef?: string
  directConnectSignType?: string
  directConnectEncryptType?: string
  directConnectLastSyncAt?: string
  directConnectLastSyncStatus?: string
  directConnectLastErrorMsg?: string
  directConnectExtJson?: string
  createdAt?: string
  updatedAt?: string
}

export interface FinanceCompanyOption {
  companyId: string
  companyCode: string
  companyName: string
  label: string
  value: string
}

export interface FinanceContextMeta {
  companyOptions: FinanceCompanyOption[]
  currentUserCompanyId?: string
  defaultCompanyId?: string
}

export interface FinanceAccountSetTemplateSummary {
  templateCode: string
  templateName: string
  accountingStandard?: string
  level1SubjectCount: number
  commonSubjectCount: number
}

export interface FinanceAccountSetReferenceOption {
  companyId: string
  companyName: string
  templateCode: string
  templateName: string
  enabledYearMonth?: string
  subjectCodeScheme?: string
  label: string
}

export interface FinanceAccountSetOption {
  value: string
  label: string
}

export interface FinanceAccountSetMeta {
  companyOptions: FinanceCompanyOption[]
  supervisorOptions: FinanceAccountSetOption[]
  templateOptions: FinanceAccountSetTemplateSummary[]
  referenceOptions: FinanceAccountSetReferenceOption[]
  defaultSubjectCodeScheme: string
}

export interface FinanceAccountSetSummary {
  companyId: string
  companyName: string
  status: string
  statusLabel: string
  enabledYearMonth?: string
  templateCode?: string
  templateName?: string
  supervisorUserId?: number
  supervisorName?: string
  createMode?: string
  referenceCompanyId?: string
  referenceCompanyName?: string
  subjectCodeScheme?: string
  subjectCount?: number
  lastTaskNo?: string
  lastTaskStatus?: string
  lastTaskProgress?: number
  lastTaskMessage?: string
  updatedAt?: string
}

export interface FinanceAccountSetCreatePayload {
  createMode: 'BLANK' | 'REFERENCE'
  referenceCompanyId?: string
  targetCompanyId: string
  enabledYearMonth: string
  templateCode?: string
  supervisorUserId: number
  subjectCodeScheme?: string
}

export interface FinanceAccountSetTaskStatus {
  taskNo: string
  companyId?: string
  taskType?: string
  status: string
  progress: number
  resultMessage?: string
  accountSetStatus?: string
  finished: boolean
  createdAt?: string
  updatedAt?: string
  finishedAt?: string
}

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

export interface CompanySavePayload {
  companyId?: string
  companyCode?: string
  companyName: string
  invoiceTitle?: string
  taxNo?: string
  status?: number
}

export interface CompanyBankAccountSavePayload {
  companyId: string
  bankName: string
  branchName?: string
  bankCode?: string
  branchCode?: string
  cnapsCode?: string
  accountName: string
  accountNo: string
  accountType?: string
  accountUsage?: string
  currencyCode?: string
  defaultAccount?: number
  status?: number
  remark?: string
  directConnectEnabled?: number
  directConnectProvider?: string
  directConnectChannel?: string
  directConnectProtocol?: string
  directConnectCustomerNo?: string
  directConnectAppId?: string
  directConnectAccountAlias?: string
  directConnectAuthMode?: string
  directConnectApiBaseUrl?: string
  directConnectCertRef?: string
  directConnectSecretRef?: string
  directConnectSignType?: string
  directConnectEncryptType?: string
  directConnectLastSyncAt?: string
  directConnectLastSyncStatus?: string
  directConnectLastErrorMsg?: string
  directConnectExtJson?: string
}

export interface SyncConnectorConfig {
  id: number
  platformCode: string
  platformName: string
  enabled: boolean
  autoSyncEnabled: boolean
  syncIntervalMinutes: number
  appKey?: string
  appSecret?: string
  appId?: string
  corpId?: string
  agentId?: string
  lastSyncAt?: string
  lastSyncStatus?: string
  lastSyncMessage?: string
}

export interface SyncConnectorSavePayload {
  platformCode: string
  enabled?: number
  autoSyncEnabled?: number
  syncIntervalMinutes?: number
  appKey?: string
  appSecret?: string
  appId?: string
  corpId?: string
  agentId?: string
}

export interface SyncJobRecord {
  id: number
  jobNo: string
  platformCode: string
  triggerType: string
  status: string
  successCount: number
  skippedCount: number
  failedCount: number
  deletedCount: number
  summary?: string
  startedAt?: string
  finishedAt?: string
}

export interface SystemSettingsBootstrapData {
  currentUser: UserProfile
  departments: DepartmentTreeNode[]
  employees: EmployeeRecord[]
  roles: RoleRecord[]
  permissions: PermissionTreeNode[]
  companies: CompanyRecord[]
  companyBankAccounts: CompanyBankAccountRecord[]
  connectors: SyncConnectorConfig[]
  jobs: SyncJobRecord[]
}

export interface ArchiveAgentVersionRecord {
  id: number
  versionNo: number
  versionLabel?: string
  published: boolean
  createdByName?: string
  createdAt?: string
}

export interface ArchiveAgentTriggerConfig {
  triggerType: 'MANUAL' | 'SCHEDULE' | 'EVENT'
  enabled?: boolean
  scheduleMode?: 'CRON' | 'INTERVAL'
  cronExpression?: string
  intervalMinutes?: number
  eventCode?: string
}

export interface ArchiveAgentWorkflowNode {
  nodeKey: string
  nodeType: 'start' | 'llm' | 'condition' | 'tool' | 'transform' | 'notify' | 'end'
  label: string
  config?: Record<string, unknown>
}

export interface ArchiveAgentWorkflowEdge {
  source: string
  target: string
}

export interface ArchiveAgentToolDefinition {
  toolCode: string
  label: string
  category?: string
  available?: boolean
  enabled?: boolean
  credentialRefCode?: string
  url?: string
  title?: string
  content?: string
}

export interface ArchiveAgentSummary {
  id: number
  agentCode: string
  agentName: string
  agentDescription?: string
  iconKey?: string
  themeKey?: string
  coverColor?: string
  tags: string[]
  status: 'DRAFT' | 'READY' | 'DISABLED' | 'ARCHIVED'
  latestVersionNo?: number
  publishedVersionNo?: number
  runtimeStatus?: 'DRAFT' | 'READY' | 'RUNNING' | 'FAILED' | 'DISABLED'
  lastRunStatus?: string
  lastRunSummary?: string
  lastRunAt?: string
  enabledTriggerCount?: number
}

export interface ArchiveAgentDetail extends ArchiveAgentSummary {
  promptConfig: Record<string, unknown>
  modelConfig: Record<string, unknown>
  tools: ArchiveAgentToolDefinition[]
  workflow: {
    nodes: ArchiveAgentWorkflowNode[]
    edges: ArchiveAgentWorkflowEdge[]
  }
  triggers: ArchiveAgentTriggerConfig[]
  inputSchema: Record<string, unknown>
  versions: ArchiveAgentVersionRecord[]
}

export interface ArchiveAgentSavePayload {
  agentName: string
  agentDescription?: string
  iconKey?: string
  themeKey?: string
  coverColor?: string
  tags: string[]
  promptConfig: Record<string, unknown>
  modelConfig: Record<string, unknown>
  tools: Array<Record<string, unknown>>
  workflow: {
    nodes: ArchiveAgentWorkflowNode[]
    edges: ArchiveAgentWorkflowEdge[]
  }
  triggers: Array<Record<string, unknown>>
  inputSchema: Record<string, unknown>
}

export interface ArchiveAgentRunRecord {
  id: number
  runNo: string
  agentId: number
  triggerType: string
  triggerSource?: string
  status: string
  summary?: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  durationMs?: number
}

export interface ArchiveAgentRunStepRecord {
  stepNo: number
  nodeKey: string
  nodeType: string
  nodeLabel?: string
  status: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  durationMs?: number
  inputPayload: Record<string, unknown>
  outputPayload: Record<string, unknown>
}

export interface ArchiveAgentRunDetail extends ArchiveAgentRunRecord {
  agentName?: string
  agentVersionNo?: number
  inputPayload: Record<string, unknown>
  outputPayload: Record<string, unknown>
  steps: ArchiveAgentRunStepRecord[]
  artifacts: Array<Record<string, unknown>>
}

export interface ArchiveAgentTestRunPayload {
  triggerSource?: string
  inputPayload: Record<string, unknown>
}

export interface ArchiveAgentMeta {
  modelProviders: Array<Record<string, unknown>>
  tools: Array<Record<string, unknown>>
  nodeTypes: Array<Record<string, unknown>>
  triggerTypes: Array<Record<string, unknown>>
  iconOptions: Array<Record<string, unknown>>
  themeOptions: Array<Record<string, unknown>>
  defaultSystemPrompt: string
}

export interface ExpenseSummary {
  documentCode: string
  no: string
  type: string
  reason: string
  documentTitle?: string
  documentReason?: string
  submitterName?: string
  submitterDeptName?: string
  templateName?: string
  templateType?: string
  templateTypeLabel?: string
  currentNodeName?: string
  documentStatus?: string
  documentStatusLabel?: string
  amount: MoneyValue
  outstandingAmount?: MoneyValue
  date: string
  status: string
  submittedAt?: string
  paymentDate?: string
  paymentCompanyName?: string
  payeeName?: string
  counterpartyName?: string
  undertakeDepartmentNames?: string[]
  tagNames?: string[]
}

export interface ExpenseApprovalTask {
  id: number
  documentCode: string
  nodeKey: string
  nodeName: string
  nodeType: string
  assigneeUserId: number
  assigneeName: string
  status: string
  taskBatchNo: string
  approvalMode?: string
  taskKind?: string
  sourceTaskId?: number
  actionComment?: string
  createdAt?: string
  handledAt?: string
}

export interface ExpenseApprovalLog {
  id: number
  documentCode: string
  nodeKey?: string
  nodeName?: string
  actionType: string
  actorUserId?: number
  actorName?: string
  actionComment?: string
  payload: Record<string, unknown>
  createdAt?: string
}

export interface ExpenseApprovalPendingItem {
  taskId: number
  documentCode: string
  documentTitle: string
  documentReason?: string
  templateName?: string
  templateType?: string
  templateTypeLabel?: string
  submitterName?: string
  submitterDeptName?: string
  amount: MoneyValue
  nodeKey: string
  nodeName: string
  status: string
  documentStatus?: string
  documentStatusLabel?: string
  submittedAt?: string
  paymentDate?: string
  paymentCompanyName?: string
  payeeName?: string
  counterpartyName?: string
  undertakeDepartmentNames?: string[]
  tagNames?: string[]
  taskCreatedAt?: string
}

export interface ExpensePaymentOrder {
  taskId: number
  documentCode: string
  documentTitle: string
  templateName?: string
  templateType?: string
  templateTypeLabel?: string
  submitterName?: string
  submitterDeptName?: string
  currentNodeName?: string
  documentStatus?: string
  documentStatusLabel?: string
  amount: MoneyValue
  submittedAt?: string
  paymentDate?: string
  paymentCompanyName?: string
  paymentStatusCode?: string
  paymentStatusLabel?: string
  manualPaid?: boolean
  paidAt?: string
  receiptStatusLabel?: string
  receiptReceivedAt?: string
  bankFlowNo?: string
  companyBankAccountName?: string
  taskCreatedAt?: string
  allowRetry?: boolean
}

export interface ExpenseBankLinkSummary {
  companyBankAccountId: number
  companyId: string
  companyName?: string
  accountName: string
  accountNo: string
  bankName: string
  accountStatus?: number
  directConnectEnabled: boolean
  directConnectProvider?: string
  directConnectChannel?: string
  directConnectStatusLabel?: string
  lastDirectConnectStatus?: string
  lastReceiptStatus?: string
}

export interface ExpenseBankLinkConfig {
  companyBankAccountId: number
  companyId: string
  companyName?: string
  accountName: string
  accountNo: string
  bankName: string
  accountStatus?: number
  directConnectEnabled: boolean
  directConnectProvider?: string
  directConnectChannel?: string
  directConnectProtocol?: string
  directConnectCustomerNo?: string
  directConnectAppId?: string
  directConnectAccountAlias?: string
  directConnectAuthMode?: string
  directConnectApiBaseUrl?: string
  directConnectCertRef?: string
  directConnectSecretRef?: string
  directConnectSignType?: string
  directConnectEncryptType?: string
  operatorKey?: string
  callbackSecret?: string
  publicKeyRef?: string
  receiptQueryEnabled?: boolean
  lastDirectConnectStatus?: string
  lastDirectConnectError?: string
}

export interface ExpenseBankLinkSavePayload {
  enabled?: boolean
  directConnectProvider: string
  directConnectChannel: string
  directConnectProtocol?: string
  directConnectCustomerNo?: string
  directConnectAppId?: string
  directConnectAccountAlias?: string
  directConnectAuthMode?: string
  directConnectApiBaseUrl?: string
  directConnectCertRef?: string
  directConnectSecretRef?: string
  directConnectSignType?: string
  directConnectEncryptType?: string
  operatorKey?: string
  callbackSecret?: string
  publicKeyRef?: string
  receiptQueryEnabled?: boolean
}

export interface ExpenseApprovalActionPayload {
  comment?: string
}

export interface ExpenseDocumentCommentPayload {
  comment?: string
  attachmentFileNames?: string[]
}

export interface ExpenseDocumentReminderPayload {
  remark?: string
}

export interface ExpenseTaskTransferPayload {
  targetUserId: number
  remark?: string
}

export interface ExpenseTaskAddSignPayload {
  targetUserId: number
  remark?: string
}

export interface ExpenseDocumentNavigation {
  prevDocumentCode?: string
  nextDocumentCode?: string
}

export interface ExpenseActionUserOption {
  userId: number
  name: string
  username?: string
  deptName?: string
  phone?: string
}

export interface ExpenseDocumentDetail {
  documentCode: string
  documentTitle: string
  documentReason?: string
  status: string
  statusLabel: string
  totalAmount: MoneyValue
  submitterUserId?: number
  submitterName?: string
  templateName?: string
  templateType?: string
  currentNodeKey?: string
  currentNodeName?: string
  currentTaskType?: string
  submittedAt?: string
  finishedAt?: string
  templateSnapshot: Record<string, unknown>
  formSchemaSnapshot: ProcessFormDesignSchema
  formData: Record<string, unknown>
  flowSnapshot: {
    flowName?: string
    flowDescription?: string
    nodes?: ProcessFlowNode[]
    routes?: ProcessFlowRoute[]
    [key: string]: unknown
  }
  companyOptions: ProcessFormOption[]
  departmentOptions: ProcessFormOption[]
  expenseDetails: ExpenseDetailInstanceSummary[]
  currentTasks: ExpenseApprovalTask[]
  actionLogs: ExpenseApprovalLog[]
  bankPayment?: ExpenseDocumentBankPayment
  bankReceipts?: ExpenseDocumentBankReceipt[]
}

export interface ExpenseDocumentBankPayment {
  bankProvider?: string
  bankChannel?: string
  companyBankAccountName?: string
  paymentStatusCode?: string
  paymentStatusLabel?: string
  manualPaid?: boolean
  paidAt?: string
  receiptStatusLabel?: string
  receiptReceivedAt?: string
  bankFlowNo?: string
  bankOrderNo?: string
  lastErrorMessage?: string
}

export interface ExpenseDocumentBankReceipt {
  attachmentId?: string
  fileName: string
  contentType?: string
  fileSize?: number
  previewUrl?: string
  receivedAt?: string
}

export interface ExpenseDetailInstance {
  detailNo?: string
  detailDesignCode?: string
  detailType?: string
  enterpriseMode?: string
  expenseTypeCode?: string
  businessSceneMode?: string
  detailTitle?: string
  sortOrder?: number
  formData: Record<string, unknown>
}

export interface ExpenseDetailInstanceSummary {
  detailNo: string
  detailDesignCode?: string
  detailType: string
  detailTypeLabel: string
  enterpriseMode?: string
  enterpriseModeLabel?: string
  detailTitle?: string
  sortOrder?: number
  createdAt?: string
}

export interface ExpenseDetailInstanceDetail {
  documentCode: string
  detailNo: string
  detailDesignCode?: string
  detailType: string
  detailTypeLabel: string
  enterpriseMode?: string
  enterpriseModeLabel?: string
  expenseTypeCode?: string
  businessSceneMode?: string
  detailTitle?: string
  sortOrder?: number
  schemaSnapshot: ProcessFormDesignSchema
  formData: Record<string, unknown>
  createdAt?: string
  updatedAt?: string
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

export interface FinanceBankOption {
  bankCode: string
  bankName: string
  value: string
  label: string
}

export interface FinanceBankBranchOption {
  id: number
  bankCode: string
  bankName: string
  province: string
  city: string
  branchCode: string
  branchName: string
  cnapsCode?: string
  value: string
  label: string
}

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

export interface ExpenseCreateTemplateSummary {
  templateCode: string
  templateName: string
  templateType: string
  templateTypeLabel: string
  categoryCode: string
  formDesignCode?: string
}

export interface ExpenseCreateVendorOption {
  value: string
  label: string
  secondaryLabel?: string
  cVenCode: string
  cVenName: string
  cVenAbbName?: string
}

export interface ExpenseCreatePayeeOption {
  value: string
  label: string
  sourceType: string
  sourceCode: string
  secondaryLabel?: string
}

export interface ExpenseCreatePayeeAccountOption {
  value: string
  label: string
  sourceType: string
  ownerCode: string
  ownerName: string
  bankName?: string
  accountName?: string
  accountNoMasked?: string
  secondaryLabel?: string
}

export interface ExpenseCreatePayeeOptionsParams {
  keyword?: string
  personalOnly?: boolean
}

export type ExpenseCreatePayeeAccountLinkageMode = 'EMPLOYEE' | 'ENTERPRISE'

export interface ExpenseCreatePayeeAccountOptionsParams {
  keyword?: string
  linkageMode?: ExpenseCreatePayeeAccountLinkageMode
  payeeName?: string
  counterpartyCode?: string
}

export interface ExpenseCreateTemplateDetail {
  templateCode: string
  templateName: string
  templateType: string
  templateTypeLabel: string
  categoryCode: string
  templateDescription?: string
  formDesignCode?: string
  approvalFlowCode?: string
  flowName?: string
  formName?: string
  schema: ProcessFormDesignSchema
  sharedArchives: ProcessCustomArchiveDetail[]
  expenseDetailDesignCode?: string
  expenseDetailDesignName?: string
  expenseDetailType?: string
  expenseDetailTypeLabel?: string
  expenseDetailModeDefault?: string
  expenseDetailSchema: ProcessFormDesignSchema
  expenseDetailSharedArchives: ProcessCustomArchiveDetail[]
  companyOptions: ProcessFormOption[]
  departmentOptions: ProcessFormOption[]
  currentUserDeptId?: string
  currentUserDeptName?: string
}

export interface ExpenseDocumentEditContext extends ExpenseCreateTemplateDetail {
  editMode: string
  documentCode: string
  taskId?: number
  formData: Record<string, unknown>
  expenseDetails: ExpenseDetailInstance[]
}

export interface ExpenseDocumentSubmitPayload {
  templateCode: string
  formData: Record<string, unknown>
  expenseDetails?: ExpenseDetailInstance[]
}

export interface ExpenseDocumentSubmitResult {
  id: number
  documentCode: string
  status: string
}

export interface ExpenseDocumentUpdatePayload {
  formData: Record<string, unknown>
  expenseDetails?: ExpenseDetailInstance[]
}

export interface ExpenseAttachmentMeta {
  attachmentId?: string
  fileName: string
  contentType?: string
  fileSize?: number
  previewUrl?: string
}

export interface ExpenseRelatedDocumentValue {
  documentCode: string
  documentTitle?: string
  templateType?: string
  templateTypeLabel?: string
  templateName?: string
  status?: string
  statusLabel?: string
}

export interface ExpenseWriteOffDocumentValue extends ExpenseRelatedDocumentValue {
  writeOffSourceKind?: string
  availableWriteOffAmount?: MoneyValue
  writeOffAmount?: MoneyValue
  remainingAmount?: MoneyValue
  effectiveStatus?: string
}

export interface ExpenseDocumentPickerItem {
  documentCode: string
  documentTitle?: string
  templateType: string
  templateTypeLabel: string
  templateName?: string
  status: string
  statusLabel: string
  totalAmount: MoneyValue
  availableWriteOffAmount?: MoneyValue
  writeOffSourceKind?: string
}

export interface ExpenseDocumentPickerGroup {
  templateType: string
  templateTypeLabel: string
  total: number
  page: number
  pageSize: number
  items: ExpenseDocumentPickerItem[]
}

export interface ExpenseDocumentPickerResult {
  relationType: 'RELATED' | 'WRITEOFF'
  groups: ExpenseDocumentPickerGroup[]
}

export interface ApprovalSummary {
  id: number
  title: string
  submitter: string
  time: string
  amount: MoneyValue
  avatar: string
}

export interface InvoiceAlert {
  id: number
  title: string
  desc: string
  time: string
}

export interface InvoiceSummary {
  code: string
  number: string
  type: string
  seller: string
  amount: MoneyValue
  date: string
  status: string
  ocrStatus: string
}

export interface FinanceVoucherOption {
  value: string
  code?: string
  name?: string
  label: string
  parentValue?: string
}

export interface FinanceVoucherEntry {
  inid?: number
  cdigest: string
  ccode: string
  ccodeName?: string
  cdeptId?: string
  cpersonId?: string
  ccusId?: string
  csupId?: string
  citemClass?: string
  citemId?: string
  cexchName?: string
  nfrat?: number
  md?: MoneyValue
  mc?: MoneyValue
  ndS?: number
  ncS?: number
}

export interface FinanceVoucherForm {
  companyId: string
  iperiod: number
  csign: string
  inoId?: number
  dbillDate: string
  idoc: number
  cbill: string
  ctext1?: string
  ctext2?: string
  entries: FinanceVoucherEntry[]
}

export type FinanceVoucherSavePayload = FinanceVoucherForm

export interface FinanceVoucherMeta {
  companyOptions: FinanceVoucherOption[]
  departmentOptions: FinanceVoucherOption[]
  employeeOptions: FinanceVoucherOption[]
  voucherTypeOptions: FinanceVoucherOption[]
  currencyOptions: FinanceVoucherOption[]
  accountOptions: FinanceVoucherOption[]
  customerOptions: FinanceVoucherOption[]
  supplierOptions: FinanceVoucherOption[]
  projectClassOptions: FinanceVoucherOption[]
  projectOptions: FinanceVoucherOption[]
  defaultCompanyId?: string
  defaultBillDate: string
  defaultPeriod: number
  defaultVoucherType: string
  suggestedVoucherNo: number
  defaultMaker: string
  defaultAttachedDocCount: number
  defaultCurrency: string
}

export interface FinanceVoucherQueryParams {
  companyId: string
  voucherNo?: string
  csign?: string
  billMonth?: string
  billMonthFrom?: string
  billMonthTo?: string
  summary?: string
  page?: number
  pageSize?: number
}

export interface FinanceVoucherSummary {
  voucherNo: string
  displayVoucherNo: string
  companyId: string
  iperiod: number
  csign: string
  voucherTypeLabel: string
  dbillDate: string
  summary: string
  cbill: string
  idoc: number
  status: string
  statusLabel: string
  editable: boolean
  entryCount: number
  totalDebit: MoneyValue
  totalCredit: MoneyValue
}

export interface FinanceVoucherDetail {
  voucherNo: string
  displayVoucherNo: string
  companyId: string
  iperiod: number
  csign: string
  voucherTypeLabel: string
  inoId: number
  dbillDate: string
  idoc: number
  cbill: string
  ctext1?: string
  ctext2?: string
  status: string
  statusLabel: string
  editable: boolean
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  entries: FinanceVoucherEntry[]
}

export interface FinanceVoucherSaveResult {
  voucherNo: string
  companyId: string
  iperiod: number
  csign: string
  inoId: number
  entryCount: number
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  status: string
}

export interface FixedAssetOption {
  value: string
  label: string
}

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

export interface DashboardData {
  user: UserProfile
  pendingApprovalCount: number
  pendingApprovalDelta: number
  pendingRepaymentCount: number
  pendingPrepayWriteOffCount: number
  unusedApplicationCount: number
  unpaidContractCount: number
  monthlyExpenseAmount: MoneyValue
  monthlyExpenseCount: number
  invoiceCount: number
  monthlyInvoiceCount: number
  budgetRemaining: MoneyValue
  budgetUsageRate: number
  recentExpenses: ExpenseSummary[]
  pendingApprovals: ApprovalSummary[]
  invoiceAlerts: InvoiceAlert[]
}

export type DashboardOutstandingKind = 'LOAN' | 'PREPAY_REPORT'

export interface DashboardWriteoffBindingPayload {
  targetDocumentCode: string
  sourceReportDocumentCode: string
}

export interface ProcessCenterNavItem {
  key: string
  label: string
  tip?: string
}

export interface ProcessCenterSummary {
  totalTemplates: number
  enabledTemplates: number
  draftTemplates: number
  aiAuditTemplates: number
}

export interface ProcessTemplateCard {
  id: number
  templateCode: string
  name: string
  templateTypeCode: string
  templateType: string
  businessDomain: string
  description: string
  highlights: string[]
  flowCode?: string
  flowName: string
  formCode?: string
  formName?: string
  expenseDetailDesignCode?: string
  expenseDetailDesignName?: string
  updatedAt: string
  owner: string
  color: string
}

export interface ProcessTemplateCategory {
  code: string
  name: string
  description: string
  templateCount: number
  templates: ProcessTemplateCard[]
}

export interface ProcessCenterOverview {
  navItems: ProcessCenterNavItem[]
  summary: ProcessCenterSummary
  categories: ProcessTemplateCategory[]
}

export interface ProcessTemplateTypeOption {
  code: string
  name: string
  subtitle: string
  description: string
  accent: string
}

export interface ProcessFormOption {
  label: string
  value: string
}

export interface ProcessTemplateFormOptions {
  templateType: string
  templateTypeLabel: string
  categoryOptions: ProcessFormOption[]
  numberingRulePreview: string
  formDesignOptions: ProcessFormOption[]
  expenseDetailDesignOptions: ProcessExpenseDetailDesignSummary[]
  expenseDetailModeOptions: ProcessFormOption[]
  printModes: ProcessFormOption[]
  approvalFlows: ProcessFormOption[]
  paymentModes: ProcessFormOption[]
  allocationForms: ProcessFormOption[]
  expenseTypes: ProcessExpenseTypeTreeNode[]
  departmentOptions: ProcessFormOption[]
  aiAuditModes: ProcessFormOption[]
  tagOptions: ProcessFormOption[]
  installmentOptions: ProcessFormOption[]
}

export interface ProcessTemplateSavePayload {
  templateType: string
  templateName: string
  templateDescription: string
  category: string
  enabled: boolean
  formDesign: string
  expenseDetailDesign?: string
  expenseDetailModeDefault?: string
  printMode: string
  approvalFlow: string
  paymentMode: string
  allocationForm: string
  aiAuditMode: string
  scopeDeptIds: string[]
  scopeExpenseTypeCodes: string[]
  amountMin?: MoneyValue
  amountMax?: MoneyValue
  tagOption: string
  installmentOption: string
}

export interface ProcessTemplateDetail extends ProcessTemplateSavePayload {
  id: number
  templateCode: string
  templateTypeLabel: string
  expenseDetailType?: string
}

export interface ProcessTemplateSaveResult {
  id: number
  templateCode: string
  templateName: string
  status: string
}

export type ProcessFormPermissionValue = 'EDITABLE' | 'READONLY' | 'HIDDEN'

export type ProcessFormPermissionStage =
  | 'DRAFT_BEFORE_SUBMIT'
  | 'RESUBMIT_AFTER_RETURN'
  | 'IN_APPROVAL'
  | 'ARCHIVED'

export interface ProcessFormSceneOverride {
  sceneId: number
  permission: ProcessFormPermissionValue
}

export interface ProcessFormFieldPermission {
  fixedStages: Record<ProcessFormPermissionStage, ProcessFormPermissionValue>
  sceneOverrides: ProcessFormSceneOverride[]
}

export interface ProcessFormDesignBlock {
  blockId: string
  fieldKey: string
  kind: 'CONTROL' | 'BUSINESS_COMPONENT' | 'SHARED_FIELD'
  label: string
  span: number
  helpText?: string
  required: boolean
  defaultValue?: unknown
  props: Record<string, unknown>
  permission: ProcessFormFieldPermission
}

export interface ProcessFormDesignSchema {
  layoutMode: string
  blocks: ProcessFormDesignBlock[]
}

export interface ProcessFormDesignSummary {
  id: number
  formCode: string
  formName: string
  templateType: string
  templateTypeLabel: string
  formDescription?: string
  updatedAt: string
}

export interface ProcessFormDesignDetail extends ProcessFormDesignSummary {
  schema: ProcessFormDesignSchema
}

export interface ProcessFormDesignSavePayload {
  templateType: string
  formName: string
  formDescription?: string
  schema: ProcessFormDesignSchema
}

export interface ProcessExpenseDetailDesignSummary {
  id: number
  detailCode: string
  detailName: string
  detailType: string
  detailTypeLabel: string
  detailDescription?: string
  updatedAt: string
}

export interface ProcessExpenseDetailDesignDetail extends ProcessExpenseDetailDesignSummary {
  schema: ProcessFormDesignSchema
}

export interface ProcessExpenseDetailDesignSavePayload {
  detailName: string
  detailType: string
  detailDescription?: string
  schema: ProcessFormDesignSchema
}

export interface ProcessFlowConfigOption {
  value: string
  label: string
  description: string
}

export interface ProcessFlowConditionField {
  key: string
  label: string
  valueType: string
  operatorKeys: string[]
}

export interface ProcessFlowCondition {
  fieldKey: string
  operator: string
  compareValue: unknown
}

export interface ProcessFlowConditionGroup {
  groupNo: number
  conditions: ProcessFlowCondition[]
}

export interface ProcessFlowRoute {
  routeKey: string
  sourceNodeKey: string
  targetNodeKey?: string
  routeName: string
  priority: number
  defaultRoute: boolean
  conditionGroups: ProcessFlowConditionGroup[]
}

export interface ProcessFlowManagerConfig {
  ruleMode?: string
  deptSource?: string
  managerLevel?: number | string
  orgTreeLookupEnabled?: boolean
  orgTreeLookupLevel?: number | string
  formDeptManagerEnabled?: boolean
}

export interface ProcessFlowDesignatedMemberConfig {
  userIds?: unknown
}

export interface ProcessFlowManualSelectConfig {
  candidateScope?: string
}

export interface ProcessFlowNodeConfig {
  approverType?: string
  missingHandler?: string
  approvalMode?: string
  opinionDefaults?: string[]
  specialSettings?: string[]
  managerConfig: ProcessFlowManagerConfig
  designatedMemberConfig: ProcessFlowDesignatedMemberConfig
  manualSelectConfig: ProcessFlowManualSelectConfig
  receiverType?: string
  receiverUserIds?: unknown
  timing?: string
  executorType?: string
  executorUserIds?: unknown
  paymentAction?: string
  [key: string]: unknown
}

export interface ProcessFlowNode {
  nodeKey: string
  nodeType: string
  nodeName: string
  sceneId?: number
  parentNodeKey?: string
  displayOrder: number
  config: ProcessFlowNodeConfig
}

export interface ProcessFlowSummary {
  id: number
  flowCode: string
  flowName: string
  flowDescription?: string
  status: string
  statusLabel: string
  currentVersionNo?: number
  updatedAt: string
}

export interface ProcessFlowScene {
  id: number
  sceneCode: string
  sceneName: string
  sceneDescription?: string
  status: number
}

export interface ProcessFlowDetail {
  id?: number
  flowCode?: string
  flowName: string
  flowDescription?: string
  status: string
  statusLabel?: string
  editableVersionId?: number
  editableVersionNo?: number
  publishedVersionId?: number
  publishedVersionNo?: number
  hasDraftVersion?: boolean
  nodes: ProcessFlowNode[]
  routes: ProcessFlowRoute[]
}

export interface ProcessFlowMeta {
  nodeTypeOptions: ProcessFormOption[]
  sceneOptions: ProcessFlowScene[]
  approvalApproverTypeOptions: ProcessFormOption[]
  approvalManagerRuleModeOptions: ProcessFormOption[]
  approvalManagerDeptSourceOptions: ProcessFormOption[]
  approvalManagerLevelOptions: ProcessFormOption[]
  approvalManagerLookupLevelOptions: ProcessFormOption[]
  approvalManualCandidateScopeOptions: ProcessFormOption[]
  ccReceiverTypeOptions: ProcessFormOption[]
  paymentExecutorTypeOptions: ProcessFormOption[]
  missingHandlerOptions: ProcessFormOption[]
  approvalModeOptions: ProcessFormOption[]
  defaultApprovalOpinions: string[]
  approvalSpecialOptions: ProcessFlowConfigOption[]
  ccTimingOptions: ProcessFormOption[]
  ccSpecialOptions: ProcessFlowConfigOption[]
  paymentActionOptions: ProcessFormOption[]
  paymentSpecialOptions: ProcessFlowConfigOption[]
  branchOperatorOptions: ProcessFormOption[]
  branchConditionFields: ProcessFlowConditionField[]
  departmentOptions: ProcessFormOption[]
  userOptions: ProcessFormOption[]
  expenseTypeOptions: ProcessFormOption[]
  archiveOptions: ProcessFormOption[]
}

export interface ProcessFlowSavePayload {
  flowName: string
  flowDescription?: string
  nodes: ProcessFlowNode[]
  routes: ProcessFlowRoute[]
}

export interface ProcessFlowStatusPayload {
  status: string
}

export interface ProcessFlowResolveApproversPayload {
  flowId: number
  nodeKey: string
  context: Record<string, unknown>
}

export interface ProcessFlowResolvedUser {
  userId: number
  userName: string
  deptId?: number
  deptName?: string
}

export interface ProcessFlowResolveApproversResult {
  resolutionType: string
  nextAction?: string
  approverUserIds: number[]
  approverUsers: ProcessFlowResolvedUser[]
  trace: string[]
}

export interface ProcessFlowSceneSavePayload {
  sceneName: string
  sceneDescription?: string
  status?: number
}

export interface ProcessCustomArchiveRule {
  id?: number
  groupNo: number
  fieldKey: string
  operator: string
  compareValue: unknown
}

export interface ProcessCustomArchiveItem {
  id?: number
  itemCode?: string
  itemName: string
  priority?: number
  status?: number
  rules: ProcessCustomArchiveRule[]
}

export interface ProcessCustomArchiveSummary {
  id: number
  archiveCode: string
  archiveName: string
  archiveType: string
  archiveTypeLabel: string
  archiveDescription?: string
  status: number
  itemCount: number
  updatedAt: string
}

export interface ProcessCustomArchiveDetail {
  id?: number
  archiveCode?: string
  archiveName: string
  archiveType: string
  archiveTypeLabel?: string
  archiveDescription?: string
  status?: number
  items: ProcessCustomArchiveItem[]
}

export interface ProcessCustomArchiveSavePayload {
  archiveName: string
  archiveType: string
  archiveDescription?: string
  status?: number
  items: Array<{
    id?: number
    itemName: string
    priority?: number
    status?: number
    rules: ProcessCustomArchiveRule[]
  }>
}

export interface ProcessCustomArchiveOperator {
  key: string
  label: string
}

export interface ProcessCustomArchiveRuleField {
  key: string
  label: string
  valueType: string
  operatorKeys: string[]
}

export interface ProcessCustomArchiveMeta {
  archiveTypeOptions: ProcessFormOption[]
  operatorOptions: ProcessCustomArchiveOperator[]
  ruleFields: ProcessCustomArchiveRuleField[]
  departmentOptions: ProcessFormOption[]
  tagArchiveCode: string
  installmentArchiveCode: string
}

export interface ProcessCustomArchiveStatusPayload {
  status: number
}

export interface ProcessCustomArchiveResolvePayload {
  archiveCode: string
  context: Record<string, unknown>
}

export interface ProcessCustomArchiveResolveItem {
  itemCode: string
  itemName: string
  priority?: number
}

export interface ProcessCustomArchiveResolveResult {
  archiveCode: string
  archiveType: string
  items: ProcessCustomArchiveResolveItem[]
}

export interface ProcessExpenseTypeTreeNode {
  id: number
  parentId?: number
  expenseCode: string
  expenseName: string
  status: number
  children: ProcessExpenseTypeTreeNode[]
}

export interface ProcessExpenseTypeConfigOption {
  value: string
  label: string
  description: string
}

export interface ProcessExpenseTypeDetail {
  id?: number
  parentId?: number
  expenseCode: string
  expenseName: string
  expenseDescription?: string
  codeLevel?: number
  codePrefix?: string
  scopeDeptIds: string[]
  scopeUserIds: string[]
  invoiceFreeMode: string
  taxDeductionMode: string
  taxSeparationMode: string
  status: number
}

export interface ProcessExpenseTypeMeta {
  invoiceFreeOptions: ProcessExpenseTypeConfigOption[]
  taxDeductionOptions: ProcessExpenseTypeConfigOption[]
  taxSeparationOptions: ProcessExpenseTypeConfigOption[]
  departmentOptions: ProcessFormOption[]
  userOptions: ProcessFormOption[]
}

export interface ProcessExpenseTypeSavePayload {
  expenseName: string
  expenseDescription?: string
  expenseCode: string
  scopeDeptIds: string[]
  scopeUserIds: string[]
  invoiceFreeMode: string
  taxDeductionMode: string
  taxSeparationMode: string
  status: number
}

export interface ProcessExpenseTypeStatusPayload {
  status: number
}

export interface UserBankAccountRecord {
  id: number
  bankCode?: string
  bankName: string
  province?: string
  city?: string
  branchCode?: string
  branchName?: string
  cnapsCode?: string
  accountName: string
  accountNo: string
  accountNoMasked: string
  accountType: string
  defaultAccount: boolean
  status: number
  statusLabel: string
  createdAt?: string
  updatedAt?: string
}

export interface UserBankAccountSavePayload {
  accountName: string
  accountNo: string
  accountType?: string
  bankName: string
  bankCode?: string
  province: string
  city: string
  branchName: string
  branchCode?: string
  cnapsCode?: string
  defaultAccount?: number
  status?: number
}

export interface PersonalCenterData {
  user: UserProfile
  bankAccounts: UserBankAccountRecord[]
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface DownloadRecord {
  id: number
  fileName: string
  businessType: string
  status: string
  progress: number
  fileSize: string
  createdAt: string
  finishedAt?: string
  downloadUrl?: string
  downloadable?: boolean
}

export interface DownloadCenterData {
  inProgress: DownloadRecord[]
  history: DownloadRecord[]
}

export interface InvoiceTaskPayload {
  code: string
  number: string
}

export interface AsyncTaskSubmitResult {
  taskNo: string
  taskType: string
  businessType: string
  status: string
  message: string
  downloadRecordId?: number
}

export type ExpenseExportScene = 'MY_EXPENSES' | 'PENDING_APPROVAL' | 'DOCUMENT_QUERY' | 'OUTSTANDING'

export interface ExpenseExportPayload {
  scene: ExpenseExportScene
  documentCodes?: string[]
  taskIds?: number[]
  kind?: 'LOAN' | 'PREPAY_REPORT'
}

export interface NotificationSummary {
  unreadCount: number
  latestTitle?: string
  latestContent?: string
  latestCreatedAt?: string
}

export interface NotificationItem {
  id: number
  title?: string
  content?: string
  type?: string
  status: string
  relatedTaskNo?: string
  createdAt?: string
  readAt?: string
}

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  items: T[]
}

export interface VoucherGenerationMeta {
  companyOptions: FinanceVoucherOption[]
  templateOptions: FinanceVoucherOption[]
  expenseTypeOptions: FinanceVoucherOption[]
  accountOptions: FinanceVoucherOption[]
  voucherTypeOptions: FinanceVoucherOption[]
  pushStatusOptions: FinanceVoucherOption[]
  defaultCompanyId?: string
  latestBatchNo?: string
  pendingPushCount: number
  pushedVoucherCount: number
  pushFailureCount: number
  pendingPushAmount: MoneyValue
}

export interface VoucherTemplatePolicy {
  id: number
  companyId: string
  companyName?: string
  templateCode: string
  templateName?: string
  creditAccountCode: string
  creditAccountName?: string
  voucherType: string
  voucherTypeLabel?: string
  summaryRule?: string
  enabled: boolean
  updatedAt?: string
}

export interface VoucherTemplatePolicyPayload {
  companyId: string
  templateCode: string
  templateName?: string
  creditAccountCode: string
  creditAccountName?: string
  voucherType: string
  summaryRule?: string
  enabled?: number
}

export interface VoucherSubjectMapping {
  id: number
  companyId: string
  companyName?: string
  templateCode: string
  templateName?: string
  expenseTypeCode: string
  expenseTypeName?: string
  debitAccountCode: string
  debitAccountName?: string
  enabled: boolean
  updatedAt?: string
}

export interface VoucherSubjectMappingPayload {
  companyId: string
  templateCode: string
  templateName?: string
  expenseTypeCode: string
  expenseTypeName?: string
  debitAccountCode: string
  debitAccountName?: string
  enabled?: number
}

export interface VoucherPushDocument {
  companyId?: string
  companyName?: string
  documentCode: string
  templateCode?: string
  templateName?: string
  submitterUserId?: number
  submitterName?: string
  totalAmount: MoneyValue
  finishedAt?: string
  expenseSummary?: string
  canPush: boolean
  pushStatus: string
  pushStatusLabel?: string
  failureReason?: string
  voucherNo?: string
}

export interface VoucherPushBatchItem {
  documentCode: string
  companyId?: string
  templateCode?: string
  templateName?: string
  pushStatus: string
  voucherNo?: string
  errorMessage?: string
}

export interface VoucherPushBatchResult {
  latestBatchNo?: string
  successCount: number
  failureCount: number
  results: VoucherPushBatchItem[]
}

export interface VoucherGeneratedRecord {
  id: number
  companyId?: string
  companyName?: string
  batchNo?: string
  documentCode: string
  templateCode?: string
  templateName?: string
  submitterName?: string
  totalAmount: MoneyValue
  pushStatus: string
  pushStatusLabel?: string
  voucherNo?: string
  voucherType?: string
  voucherNumber?: number
  billDate?: string
  pushedAt?: string
  failureReason?: string
}

export interface VoucherEntrySnapshot {
  entryNo: number
  direction: string
  digest?: string
  accountCode: string
  accountName?: string
  expenseTypeCode?: string
  expenseTypeName?: string
  amount: MoneyValue
}

export interface VoucherGeneratedDetail {
  record: VoucherGeneratedRecord
  voucherDetail?: FinanceVoucherDetail | null
  entries: VoucherEntrySnapshot[]
}

function clearLoginState() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}
