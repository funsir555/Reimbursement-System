import type {
  CompanyBankAccountSavePayload,
  CompanyRecord,
  DepartmentTreeNode,
  EmployeeRecord,
  OcrProviderSavePayload,
  RoleRecord,
  SyncConnectorConfig
} from '@/api'

export type FlatCompanyRecord = CompanyRecord & { level: number; label: string }
export type DepartmentTreeExpandStrategy = 'default' | 'focusPath'
export type OcrProviderCode = 'ALIYUN' | 'TENCENT' | 'BAIDU'
export type OcrFormState = Required<
  Pick<
    OcrProviderSavePayload,
    'accessKeyId' | 'accessKeySecret' | 'endpoint' | 'connectTimeoutMs' | 'readTimeoutMs'
  >
> & {
  enabled: boolean
}
export type ApiInterfaceKey = 'ocr' | 'invoiceVerification' | 'apiDocs'
export type ApiInterfaceOption = {
  key: ApiInterfaceKey
  label: string
  caption: string
  title: string
  description: string
  note: string
  tags: string[]
  fields: Array<{
    label: string
    value: string
  }>
}
export type CompanyBankAccountFormState = CompanyBankAccountSavePayload & {
  status: number
  defaultAccount: number
  directConnectEnabled: number
}
export type DepartmentFormState = {
  deptName: string
  parentId?: number
  companyId?: string
  leaderUserId?: number
  statDepartmentBelong: string
  statRegionBelong: string
  statAreaBelong: string
}
export type DepartmentConfigFormState = DepartmentFormState
export type CompanyOption = { companyId: string; label: string }
export type EmployeeOption = { userId: number; label: string }

export const SUPER_ADMIN_ROLE_CODE = 'SUPER_ADMIN'

export const companyBankAccountFieldMap = {
  accountName: 'accountName',
  bankCode: 'bankCode',
  bankName: 'bankName',
  province: 'province',
  city: 'city',
  branchCode: 'branchCode',
  branchName: 'branchName',
  accountNo: 'accountNo'
} as const

export const sourceLabelMap: Record<string, string> = {
  MANUAL: '手工',
  DINGTALK: '钉钉',
  WECOM: '企微',
  FEISHU: '飞书'
}

export const ocrVendorOptions: Array<{ code: OcrProviderCode; label: string }> = [
  { code: 'ALIYUN', label: '阿里云' },
  { code: 'TENCENT', label: '腾讯云' },
  { code: 'BAIDU', label: '百度云' }
]

export const apiInterfaceOptions: ApiInterfaceOption[] = [
  {
    key: 'ocr',
    label: 'OCR',
    caption: '云端票据识别配置',
    title: 'OCR 云端接入配置',
    description:
      '用于维护云端 OCR 厂商配置与测试结果，首期只将真实识别能力应用到发票附件上传链路。',
    note:
      '当前支持阿里云真实保存与测试；腾讯云、百度云仅保留切换入口与待接入说明。',
    tags: ['发票附件', '阿里云', '配置测试', '唯一启用厂商'],
    fields: [
      { label: '服务商', value: '支持阿里云 AccessKey 配置，腾讯云/百度云待后续接入。' },
      {
        label: '鉴权方式',
        value: '运行时走 AccessKey ID + AccessKey Secret；密钥读取时仅返回脱敏值。'
      },
      { label: '测试方式', value: '使用系统内置发票样张发起一次真实 OCR，并回写最近测试状态。' },
      { label: '识别场景', value: '首期仅覆盖 invoiceAttachments 上传后自动识别，不扩散到其它附件。' },
      {
        label: '返回字段',
        value: '输出发票代码、号码、日期、票种、销方、金额、税额和状态说明。'
      }
    ]
  },
  {
    key: 'invoiceVerification',
    label: '发票验真',
    caption: '校验发票真伪与状态',
    title: '发票验真接口配置',
    description:
      '用于统一管理发票查验通道与结果处理规则，当前只展示验真接入所需的静态配置占位。',
    note:
      '后续将补充查验渠道、频控策略、异常重试和结果落库逻辑；本次不接真实验真服务。',
    tags: ['验真通道', '频控', '重试机制', '结果归档'],
    fields: [
      { label: '验真通道', value: '预留税局/第三方验真渠道选择及主备通道说明。' },
      { label: '请求频控', value: '预留单票查验频率、批量查验并发数与限流阈值。' },
      { label: '超时重试', value: '预留超时阈值、重试次数与退避策略配置。' },
      {
        label: '验真结果字段',
        value: '预留真伪状态、作废状态、重复报销标识等结果定义。'
      },
      { label: '异常处理', value: '预留网络异常、额度不足、验真失败等兜底提示规则。' }
    ]
  },
  {
    key: 'apiDocs',
    label: '接口文档',
    caption: '本项目接口目录占位',
    title: '本项目接口文档',
    description:
      '用于沉淀本项目接口文档入口与模块目录，当前只提供静态说明，不跳转到真实文档地址。',
    note:
      '后续将补充认证、报销、发票、流程、财务档案等模块文档入口，并支持按环境查看。',
    tags: ['认证模块', '报销模块', '发票模块', '流程模块', '财务模块'],
    fields: [
      { label: '文档范围', value: '预留认证、报销、发票、流程、财务等模块接口目录。' },
      { label: '访问方式', value: '预留在线调试地址、环境切换与文档权限控制说明。' },
      { label: '接口规范', value: '预留鉴权规则、分页约定、错误码与幂等要求说明。' },
      { label: '示例数据', value: '预留请求示例、响应示例与联调注意事项。' },
      { label: '发布说明', value: '预留版本记录、变更日志与废弃接口提示。' }
    ]
  }
]

export const DEFAULT_API_INTERFACE_OPTION: ApiInterfaceOption = apiInterfaceOptions[0]!

export const ocrRamPolicySnippet = `{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ocr:RecognizeInvoice"
      ],
      "Resource": "*"
    }
  ]
}`

export function createDepartmentFormState(): DepartmentFormState {
  return {
    deptName: '',
    parentId: undefined,
    companyId: undefined,
    leaderUserId: undefined,
    statDepartmentBelong: '',
    statRegionBelong: '',
    statAreaBelong: ''
  }
}

export function createDepartmentConfigFormState(): DepartmentConfigFormState {
  return createDepartmentFormState()
}

export function createCompanyBankAccountFormState(): CompanyBankAccountFormState {
  return {
    companyId: '',
    bankName: '',
    province: '',
    city: '',
    accountName: '',
    accountNo: '',
    status: 1,
    defaultAccount: 0,
    directConnectEnabled: 0
  }
}

export function findDepartmentById(
  tree: DepartmentTreeNode[],
  id?: number
): DepartmentTreeNode | undefined {
  for (const item of tree) {
    if (item.id === id) {
      return item
    }
    const child = findDepartmentById(item.children || [], id)
    if (child) {
      return child
    }
  }
  return undefined
}

export function flattenDepartments(tree: DepartmentTreeNode[]): DepartmentTreeNode[] {
  return tree.flatMap((item) => [item, ...flattenDepartments(item.children || [])])
}

export function removeDepartmentNode(tree: DepartmentTreeNode[], id?: number): DepartmentTreeNode[] {
  return tree
    .filter((item) => item.id !== id)
    .map((item) => ({
      ...item,
      children: removeDepartmentNode(item.children || [], id)
    }))
}

export function buildDepartmentPathIds(tree: DepartmentTreeNode[], departmentId: number): number[] {
  const path: number[] = []
  if (collectDepartmentPathIds(tree, departmentId, path)) {
    return path
  }
  return []
}

function collectDepartmentPathIds(
  tree: DepartmentTreeNode[],
  departmentId: number,
  path: number[]
): boolean {
  for (const node of tree) {
    path.push(node.id)
    if (node.id === departmentId) {
      return true
    }
    if (collectDepartmentPathIds(node.children || [], departmentId, path)) {
      return true
    }
    path.pop()
  }
  return false
}

export function isTopLevelDepartment(department: DepartmentTreeNode) {
  return department.parentId === undefined || department.parentId === null
}

export function normalizeOcrText(value?: string | null) {
  return String(value || '').trim()
}

export function maskAccountNo(accountNo?: string) {
  if (!accountNo) {
    return ''
  }
  if (accountNo.length <= 8) {
    return accountNo
  }
  return `${accountNo.slice(0, 4)} **** **** ${accountNo.slice(-4)}`
}

export function formatBooleanTag(value: number) {
  return value === 1 ? '是' : '否'
}

export function formatStatusLabel(status: number) {
  return status === 1 ? '启用' : '停用'
}

export function sanitizeEditableRoleIds(
  roleIds: number[],
  roles: Array<Pick<RoleRecord, 'id' | 'roleCode'>>
) {
  return roleIds.filter((roleId) => {
    const role = roles.find((item) => item.id === roleId)
    return role ? !isSuperAdminRole(role) : true
  })
}

export function isSuperAdminRole(role?: Pick<RoleRecord, 'roleCode'>) {
  return role?.roleCode === SUPER_ADMIN_ROLE_CODE
}

export function isManualDepartment(item: DepartmentTreeNode) {
  return item.syncSource === 'MANUAL' || !item.syncManaged
}

export function isManualEmployee(item?: Pick<EmployeeRecord, 'sourceType' | 'syncManaged'>) {
  return !!item && (item.sourceType === 'MANUAL' || !item.syncManaged)
}

export function flattenCompanies(tree: CompanyRecord[], level = 0): FlatCompanyRecord[] {
  return tree.flatMap((item) => [
    {
      ...item,
      level,
      label: `${'-- '.repeat(level)}${item.companyName}`
    },
    ...flattenCompanies(item.children || [], level + 1)
  ])
}

export function isWecomConnector(connector: SyncConnectorConfig) {
  return connector.platformCode === 'WECOM'
}

export function resolveConnectorPlatformName(connector: SyncConnectorConfig) {
  return sourceLabelMap[connector.platformCode] || connector.platformName || connector.platformCode
}
