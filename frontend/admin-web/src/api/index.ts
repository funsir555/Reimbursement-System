const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export type MoneyValue = string

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
  bankName?: string
  bankAccountName?: string
  bankAccountNo?: string
  status: number
  children: CompanyRecord[]
}

export interface CompanySavePayload {
  companyId?: string
  companyCode?: string
  companyName: string
  invoiceTitle?: string
  taxNo?: string
  bankName?: string
  bankAccountName?: string
  bankAccountNo?: string
  status?: number
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
  connectors: SyncConnectorConfig[]
  jobs: SyncJobRecord[]
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
  label: string
}

export interface FinanceVoucherEntry {
  inid?: number
  cdigest: string
  ccode: string
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

export interface FinanceVoucherDetail {
  voucherNo: string
  companyId: string
  iperiod: number
  csign: string
  inoId: number
  dbillDate: string
  idoc: number
  cbill: string
  ctext1?: string
  ctext2?: string
  status: string
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

export interface BankAccount {
  id: number
  bankName: string
  branchName?: string
  accountName: string
  accountNoMasked: string
  accountType: string
  defaultAccount: boolean
  status: string
}

export interface PersonalCenterData {
  user: UserProfile
  bankAccounts: BankAccount[]
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

export interface NotificationSummary {
  unreadCount: number
  latestTitle?: string
  latestContent?: string
  latestCreatedAt?: string
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

interface RequestOptions extends RequestInit {
  timeoutMs?: number
  timeoutMessage?: string
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  const token = localStorage.getItem('token')
  const { timeoutMs, timeoutMessage, signal, ...fetchOptions } = options
  const isFormDataBody = typeof FormData !== 'undefined' && fetchOptions.body instanceof FormData
  const headers: Record<string, string> = {
    ...(fetchOptions.headers as Record<string, string>)
  }

  if (!isFormDataBody && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let timeoutHandle: ReturnType<typeof window.setTimeout> | undefined
  let didTimeout = false
  let requestSignal = signal

  if (typeof timeoutMs === 'number' && timeoutMs > 0) {
    const controller = new AbortController()
    requestSignal = controller.signal

    if (signal) {
      if (signal.aborted) {
        controller.abort(signal.reason)
      } else {
        signal.addEventListener('abort', () => controller.abort(signal.reason), { once: true })
      }
    }

    timeoutHandle = window.setTimeout(() => {
      didTimeout = true
      controller.abort(new DOMException('Request timeout', 'AbortError'))
    }, timeoutMs)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${url}`, {
      ...fetchOptions,
      headers,
      signal: requestSignal
    })
  } catch (error: unknown) {
    if (timeoutHandle) {
      window.clearTimeout(timeoutHandle)
    }
    if (didTimeout) {
      throw new Error(timeoutMessage || '请求超时，请稍后重试')
    }
    throw error
  }

  if (timeoutHandle) {
    window.clearTimeout(timeoutHandle)
  }

  const result = await response.json().catch(() => ({ message: '请求失败' }))

  if (response.status === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error(result.message || '登录已过期，请重新登录')
  }

  if (!response.ok) {
    throw new Error(result.message || `HTTP ${response.status}`)
  }

  const payload = result as ApiResponse<T>

  if (payload.code === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error(payload.message || '登录已过期，请重新登录')
  }

  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }

  return payload
}

function buildQueryString(params: Record<string, string | number | boolean | undefined | null>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    search.append(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export const authApi = {
  loginByPassword: (username: string, password: string) =>
    request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }),
  getCurrentUser: () => request<UserProfile>('/auth/me')
}

export const dashboardApi = {
  getOverview: () => request<DashboardData>('/auth/dashboard')
}

export const expenseApi = {
  list: () => request<ExpenseSummary[]>('/auth/expenses'),
  queryDocuments: () => request<ExpenseSummary[]>('/auth/expenses/query-documents'),
  getDetail: (documentCode: string) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}`, {
      timeoutMs: 10000,
      timeoutMessage: '加载单据详情超时，请稍后重试'
    }),
  getExpenseDetail: (documentCode: string, detailNo: string) =>
    request<ExpenseDetailInstanceDetail>(
      `/auth/expenses/${encodeURIComponent(documentCode)}/details/${encodeURIComponent(detailNo)}`,
      {
        timeoutMs: 10000,
        timeoutMessage: '加载费用明细超时，请稍后重试'
      }
    ),
  getDocumentPicker: (params: {
    relationType: 'RELATED' | 'WRITEOFF'
    templateTypes?: string[]
    keyword?: string
    page?: number
    pageSize?: number
    excludeDocumentCode?: string
  }) => {
    const search = new URLSearchParams()
    search.append('relationType', params.relationType)
    params.templateTypes?.forEach((item) => {
      if (item) {
        search.append('templateTypes', item)
      }
    })
    if (params.keyword) {
      search.append('keyword', params.keyword)
    }
    if (params.page) {
      search.append('page', String(params.page))
    }
    if (params.pageSize) {
      search.append('pageSize', String(params.pageSize))
    }
    if (params.excludeDocumentCode) {
      search.append('excludeDocumentCode', params.excludeDocumentCode)
    }
    return request<ExpenseDocumentPickerResult>(`/auth/expenses/document-picker?${search.toString()}`)
  },
  recall: (documentCode: string) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}/recall`, {
      method: 'POST'
    }),
  comment: (documentCode: string, payload: ExpenseDocumentCommentPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}/comments`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  remind: (documentCode: string, payload: ExpenseDocumentReminderPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}/reminders`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getNavigation: (documentCode: string) =>
    request<ExpenseDocumentNavigation>(`/auth/expenses/${encodeURIComponent(documentCode)}/navigation`, {
      timeoutMs: 5000,
      timeoutMessage: '加载单据导航超时，请稍后重试'
    }),
  getEditContext: (documentCode: string) =>
    request<ExpenseDocumentEditContext>(`/auth/expenses/${encodeURIComponent(documentCode)}/edit-context`),
  resubmit: (documentCode: string, payload: ExpenseDocumentUpdatePayload) =>
    request<ExpenseDocumentSubmitResult>(`/auth/expenses/${encodeURIComponent(documentCode)}/resubmit`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
}

export const expenseVoucherGenerationApi = {
  getMeta: () => request<VoucherGenerationMeta>('/auth/expenses/voucher-generation/meta'),
  getTemplatePolicies: (params: { companyId?: string; templateCode?: string; enabled?: number; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherTemplatePolicy>>(`/auth/expenses/voucher-generation/mappings${buildQueryString({ type: 'template', ...params })}`),
  createTemplatePolicy: (payload: VoucherTemplatePolicyPayload) =>
    request<VoucherTemplatePolicy>('/auth/expenses/voucher-generation/mappings/template-policy', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateTemplatePolicy: (id: number, payload: VoucherTemplatePolicyPayload) =>
    request<VoucherTemplatePolicy>(`/auth/expenses/voucher-generation/mappings/template-policy/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  getSubjectMappings: (params: { companyId?: string; templateCode?: string; expenseTypeCode?: string; enabled?: number; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherSubjectMapping>>(`/auth/expenses/voucher-generation/mappings${buildQueryString({ type: 'subject', ...params })}`),
  createSubjectMapping: (payload: VoucherSubjectMappingPayload) =>
    request<VoucherSubjectMapping>('/auth/expenses/voucher-generation/mappings/subject-lines', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateSubjectMapping: (id: number, payload: VoucherSubjectMappingPayload) =>
    request<VoucherSubjectMapping>(`/auth/expenses/voucher-generation/mappings/subject-lines/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  getPushDocuments: (params: { companyId?: string; templateCode?: string; keyword?: string; pushStatus?: string; dateFrom?: string; dateTo?: string; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherPushDocument>>(`/auth/expenses/voucher-generation/push-documents${buildQueryString(params)}`),
  pushDocuments: (documentCodes: string[]) =>
    request<VoucherPushBatchResult>('/auth/expenses/voucher-generation/push', {
      method: 'POST',
      body: JSON.stringify({ documentCodes })
    }),
  getGeneratedVouchers: (params: { companyId?: string; templateCode?: string; documentCode?: string; voucherNo?: string; pushStatus?: string; dateFrom?: string; dateTo?: string; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherGeneratedRecord>>(`/auth/expenses/voucher-generation/vouchers${buildQueryString(params)}`),
  getGeneratedVoucherDetail: (id: number) =>
    request<VoucherGeneratedDetail>(`/auth/expenses/voucher-generation/vouchers/${id}`)
}

export const expenseApprovalApi = {
  listPending: () => request<ExpenseApprovalPendingItem[]>('/auth/expense-approval/pending'),
  approve: (taskId: number, payload: ExpenseApprovalActionPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/approve`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  reject: (taskId: number, payload: ExpenseApprovalActionPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/reject`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getModifyContext: (taskId: number) =>
    request<ExpenseDocumentEditContext>(`/auth/expense-approval/tasks/${taskId}/modify-context`),
  modify: (taskId: number, payload: ExpenseDocumentUpdatePayload) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/modify`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  addSign: (taskId: number, payload: ExpenseTaskAddSignPayload) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/add-sign`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  transfer: (taskId: number, payload: ExpenseTaskTransferPayload) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/transfer`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listActionUsers: (keyword?: string) =>
    request<ExpenseActionUserOption[]>(`/auth/expense-approval/action-users${buildQueryString({ keyword })}`)
}

export const invoiceApi = {
  list: () => request<InvoiceSummary[]>('/auth/invoices')
}

export const financeApi = {
  getVoucherMeta: (params: { companyId?: string; billDate?: string; csign?: string } = {}) =>
    request<FinanceVoucherMeta>(`/auth/finance/vouchers/meta${buildQueryString(params)}`),
  getVoucherDetail: (voucherNo: string) =>
    request<FinanceVoucherDetail>(`/auth/finance/vouchers/${encodeURIComponent(voucherNo)}`),
  createVoucher: (payload: FinanceVoucherSavePayload) =>
    request<FinanceVoucherSaveResult>('/auth/finance/vouchers', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}

export const financeArchiveApi = {
  listSuppliers: (params: { keyword?: string; includeDisabled?: boolean } = {}) =>
    request<FinanceVendorSummary[]>(`/auth/finance/archives/suppliers${buildQueryString(params)}`),
  getSupplierDetail: (vendorCode: string) =>
    request<FinanceVendorDetail>(`/auth/finance/archives/suppliers/${encodeURIComponent(vendorCode)}`),
  createSupplier: (payload: FinanceVendorSavePayload) =>
    request<FinanceVendorDetail>('/auth/finance/archives/suppliers', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateSupplier: (vendorCode: string, payload: FinanceVendorSavePayload) =>
    request<FinanceVendorDetail>(`/auth/finance/archives/suppliers/${encodeURIComponent(vendorCode)}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  disableSupplier: (vendorCode: string) =>
    request<boolean>(`/auth/finance/archives/suppliers/${encodeURIComponent(vendorCode)}`, {
      method: 'DELETE'
    }),
  getEmployeeMeta: () =>
    request<FinanceEmployeeArchiveMeta>('/auth/finance/archives/employees/meta'),
  queryEmployees: (payload: EmployeeQueryPayload = {}) =>
    request<EmployeeRecord[]>('/auth/finance/archives/employees/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}

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

export const expenseCreateApi = {
  listTemplates: () =>
    request<ExpenseCreateTemplateSummary[]>('/auth/expenses/create/templates', {
      timeoutMs: 10000,
      timeoutMessage: '?????????????????????'
    }),
  getTemplateDetail: (templateCode: string) =>
    request<ExpenseCreateTemplateDetail>(`/auth/expenses/create/templates/${encodeURIComponent(templateCode)}`, {
      timeoutMs: 10000,
      timeoutMessage: '???????????????????????'
    }),
  listVendorOptions: (keyword?: string) =>
    request<ExpenseCreateVendorOption[]>(`/auth/expenses/create/vendors/options${buildQueryString({ keyword })}`),
  createVendor: (payload: FinanceVendorSavePayload) =>
    request<FinanceVendorDetail>('/auth/expenses/create/vendors', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listPayeeOptions: (keyword?: string) =>
    request<ExpenseCreatePayeeOption[]>(`/auth/expenses/create/payees/options${buildQueryString({ keyword })}`),
  listPayeeAccountOptions: (keyword?: string) =>
    request<ExpenseCreatePayeeAccountOption[]>(`/auth/expenses/create/payee-accounts/options${buildQueryString({ keyword })}`),
  uploadAttachment: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<ExpenseAttachmentMeta>('/auth/expenses/attachments', {
      method: 'POST',
      body: formData
    })
  },
  submit: (payload: ExpenseDocumentSubmitPayload) =>
    request<ExpenseDocumentSubmitResult>('/auth/expenses/create/documents', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}

export const processApi = {
  getOverview: () => request<ProcessCenterOverview>('/auth/process-management/overview'),
  getTemplateTypes: () => request<ProcessTemplateTypeOption[]>('/auth/process-management/template-types'),
  getFormOptions: (templateType: string) =>
    request<ProcessTemplateFormOptions>(`/auth/process-management/form-options?templateType=${templateType}`),
  getTemplateDetail: (id: number) =>
    request<ProcessTemplateDetail>(`/auth/process-management/templates/${id}`),
  createTemplate: (payload: ProcessTemplateSavePayload) =>
    request<ProcessTemplateSaveResult>('/auth/process-management/templates', {
      method: 'POST',
      body: JSON.stringify(payload),
      timeoutMs: 15000,
      timeoutMessage: '保存模板超时，请检查后端服务或稍后重试'
    }),
  updateTemplate: (id: number, payload: ProcessTemplateSavePayload) =>
    request<ProcessTemplateSaveResult>(`/auth/process-management/templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      timeoutMs: 15000,
      timeoutMessage: '保存模板超时，请检查后端服务或稍后重试'
    }),
  deleteTemplate: (id: number) =>
    request<boolean>(`/auth/process-management/templates/${id}`, {
      method: 'DELETE'
    }),
  listExpenseDetailDesigns: () =>
    request<ProcessExpenseDetailDesignSummary[]>('/auth/process-management/expense-detail-designs'),
  getExpenseDetailDesignDetail: (id: number) =>
    request<ProcessExpenseDetailDesignDetail>(`/auth/process-management/expense-detail-designs/${id}`),
  createExpenseDetailDesign: (payload: ProcessExpenseDetailDesignSavePayload) =>
    request<ProcessExpenseDetailDesignDetail>('/auth/process-management/expense-detail-designs', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateExpenseDetailDesign: (id: number, payload: ProcessExpenseDetailDesignSavePayload) =>
    request<ProcessExpenseDetailDesignDetail>(`/auth/process-management/expense-detail-designs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteExpenseDetailDesign: (id: number) =>
    request<boolean>(`/auth/process-management/expense-detail-designs/${id}`, {
      method: 'DELETE'
    }),
  listFormDesigns: (templateType?: string) =>
    request<ProcessFormDesignSummary[]>(`/auth/process-management/form-designs${buildQueryString({ templateType })}`),
  getFormDesignDetail: (id: number) =>
    request<ProcessFormDesignDetail>(`/auth/process-management/form-designs/${id}`),
  createFormDesign: (payload: ProcessFormDesignSavePayload) =>
    request<ProcessFormDesignDetail>('/auth/process-management/form-designs', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateFormDesign: (id: number, payload: ProcessFormDesignSavePayload) =>
    request<ProcessFormDesignDetail>(`/auth/process-management/form-designs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteFormDesign: (id: number) =>
    request<boolean>(`/auth/process-management/form-designs/${id}`, {
      method: 'DELETE'
    }),
  listFlows: () =>
    request<ProcessFlowSummary[]>('/auth/process-management/flows'),
  getFlowMeta: () =>
    request<ProcessFlowMeta>('/auth/process-management/flows/meta'),
  getFlowDetail: (id: number) =>
    request<ProcessFlowDetail>(`/auth/process-management/flows/${id}`),
  createFlow: (payload: ProcessFlowSavePayload) =>
    request<ProcessFlowDetail>('/auth/process-management/flows', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateFlow: (id: number, payload: ProcessFlowSavePayload) =>
    request<ProcessFlowDetail>(`/auth/process-management/flows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  publishFlow: (id: number) =>
    request<ProcessFlowDetail>(`/auth/process-management/flows/${id}/publish`, {
      method: 'POST'
    }),
  updateFlowStatus: (id: number, payload: ProcessFlowStatusPayload) =>
    request<boolean>(`/auth/process-management/flows/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }),
  resolveFlowApprovers: (payload: ProcessFlowResolveApproversPayload) =>
    request<ProcessFlowResolveApproversResult>('/auth/process-management/flows/resolve-approvers', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  createFlowScene: (payload: ProcessFlowSceneSavePayload) =>
    request<ProcessFlowScene>('/auth/process-management/flow-scenes', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listCustomArchives: () =>
    request<ProcessCustomArchiveSummary[]>('/auth/process-management/custom-archives'),
  getCustomArchiveMeta: () =>
    request<ProcessCustomArchiveMeta>('/auth/process-management/custom-archives/meta'),
  getCustomArchiveDetail: (id: number) =>
    request<ProcessCustomArchiveDetail>(`/auth/process-management/custom-archives/${id}`),
  createCustomArchive: (payload: ProcessCustomArchiveSavePayload) =>
    request<ProcessCustomArchiveDetail>('/auth/process-management/custom-archives', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCustomArchive: (id: number, payload: ProcessCustomArchiveSavePayload) =>
    request<ProcessCustomArchiveDetail>(`/auth/process-management/custom-archives/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  updateCustomArchiveStatus: (id: number, payload: ProcessCustomArchiveStatusPayload) =>
    request<boolean>(`/auth/process-management/custom-archives/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }),
  deleteCustomArchive: (id: number) =>
    request<boolean>(`/auth/process-management/custom-archives/${id}`, {
      method: 'DELETE'
    }),
  resolveCustomArchive: (payload: ProcessCustomArchiveResolvePayload) =>
    request<ProcessCustomArchiveResolveResult>('/auth/process-management/custom-archives/resolve', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listExpenseTypesTree: () =>
    request<ProcessExpenseTypeTreeNode[]>('/auth/process-management/expense-types/tree'),
  getExpenseTypeMeta: () =>
    request<ProcessExpenseTypeMeta>('/auth/process-management/expense-types/meta'),
  getExpenseTypeDetail: (id: number) =>
    request<ProcessExpenseTypeDetail>(`/auth/process-management/expense-types/${id}`),
  createExpenseType: (payload: ProcessExpenseTypeSavePayload) =>
    request<ProcessExpenseTypeDetail>('/auth/process-management/expense-types', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateExpenseType: (id: number, payload: ProcessExpenseTypeSavePayload) =>
    request<ProcessExpenseTypeDetail>(`/auth/process-management/expense-types/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  updateExpenseTypeStatus: (id: number, payload: ProcessExpenseTypeStatusPayload) =>
    request<boolean>(`/auth/process-management/expense-types/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }),
  deleteExpenseType: (id: number) =>
    request<boolean>(`/auth/process-management/expense-types/${id}`, {
      method: 'DELETE'
    })
}

export const profileApi = {
  getOverview: () => request<PersonalCenterData>('/auth/user-center/profile'),
  changePassword: (payload: ChangePasswordPayload) =>
    request<boolean>('/auth/user-center/password', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}

export const downloadApi = {
  getCenter: () => request<DownloadCenterData>('/auth/user-center/downloads')
}

export const asyncTaskApi = {
  exportInvoices: () =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/exports/invoices', {
      method: 'POST'
    }),
  verifyInvoice: (payload: InvoiceTaskPayload) =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/invoices/verify', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  ocrInvoice: (payload: InvoiceTaskPayload) =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/invoices/ocr', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}

export const notificationApi = {
  getSummary: () => request<NotificationSummary>('/auth/async-tasks/notifications/summary')
}

export const systemSettingsApi = {
  getBootstrap: () => request<SystemSettingsBootstrapData>('/auth/system-settings/bootstrap'),
  listDepartments: () => request<DepartmentTreeNode[]>('/auth/system-settings/departments'),
  createDepartment: (payload: DepartmentSavePayload) =>
    request<DepartmentTreeNode>('/auth/system-settings/departments', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateDepartment: (id: number, payload: DepartmentSavePayload) =>
    request<DepartmentTreeNode>(`/auth/system-settings/departments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteDepartment: (id: number) =>
    request<boolean>(`/auth/system-settings/departments/${id}`, {
      method: 'DELETE'
    }),
  queryEmployees: (payload: EmployeeQueryPayload = {}) =>
    request<EmployeeRecord[]>('/auth/system-settings/employees/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  createEmployee: (payload: EmployeeSavePayload) =>
    request<EmployeeRecord>('/auth/system-settings/employees', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateEmployee: (id: number, payload: EmployeeSavePayload) =>
    request<EmployeeRecord>(`/auth/system-settings/employees/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteEmployee: (id: number) =>
    request<boolean>(`/auth/system-settings/employees/${id}`, {
      method: 'DELETE'
    }),
  listRoles: () => request<RoleRecord[]>('/auth/system-settings/roles'),
  createRole: (payload: RoleSavePayload) =>
    request<RoleRecord>('/auth/system-settings/roles', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateRole: (id: number, payload: RoleSavePayload) =>
    request<RoleRecord>(`/auth/system-settings/roles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteRole: (id: number) =>
    request<boolean>(`/auth/system-settings/roles/${id}`, {
      method: 'DELETE'
    }),
  assignRolePermissions: (id: number, permissionCodes: string[]) =>
    request<boolean>(`/auth/system-settings/roles/${id}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionCodes })
    }),
  assignUserRoles: (id: number, roleIds: number[]) =>
    request<boolean>(`/auth/system-settings/users/${id}/roles`, {
      method: 'POST',
      body: JSON.stringify({ roleIds })
    }),
  getPermissionTree: () => request<PermissionTreeNode[]>('/auth/system-settings/permissions/tree'),
  listCompanies: () => request<CompanyRecord[]>('/auth/system-settings/companies'),
  createCompany: (payload: CompanySavePayload) =>
    request<CompanyRecord>('/auth/system-settings/companies', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCompany: (companyId: string, payload: CompanySavePayload) =>
    request<CompanyRecord>(`/auth/system-settings/companies/${companyId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteCompany: (companyId: string) =>
    request<boolean>(`/auth/system-settings/companies/${companyId}`, {
      method: 'DELETE'
    }),
  listSyncConnectors: () => request<SyncConnectorConfig[]>('/auth/system-settings/sync/connectors'),
  updateSyncConnector: (payload: SyncConnectorSavePayload) =>
    request<SyncConnectorConfig>('/auth/system-settings/sync/connectors', {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  runSync: (platformCodes: string[], triggerType = 'MANUAL') =>
    request<SyncJobRecord>('/auth/system-settings/sync/run', {
      method: 'POST',
      body: JSON.stringify({ platformCodes, triggerType })
    }),
  listSyncJobs: () => request<SyncJobRecord[]>('/auth/system-settings/sync/jobs')
}

export default request
