// 辅助核算规则选项配置
export const AUXILIARY_RULE_OPTIONS = {
  person: [
    { value: 'SUBMITTER', label: '按提单人' },
    { value: 'PAYEE', label: '按收款人' }
  ],
  supplier: [
    { value: 'PAYEE_COMPANY', label: '按收款单位' }
  ],
  dept: [
    { value: 'SUBMITTER_DEPT', label: '按提单人部门' },
    { value: 'EXPENSE_DEPT', label: '按承担部门' }
  ],
  project: [
    { value: 'BY_PROJECT', label: '按项目' }
  ]
}

export interface AuxiliaryConfig {
  showPerson: boolean
  showSupplier: boolean
  showDept: boolean
  showProject: boolean
}
