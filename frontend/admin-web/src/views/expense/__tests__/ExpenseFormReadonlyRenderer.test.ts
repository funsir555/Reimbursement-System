import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ExpenseFormReadonlyRenderer from '@/views/expense/components/ExpenseFormReadonlyRenderer.vue'

function createPermission() {
  return {
    fixedStages: {
      DRAFT_BEFORE_SUBMIT: 'EDITABLE',
      RESUBMIT_AFTER_RETURN: 'EDITABLE',
      IN_APPROVAL: 'READONLY',
      ARCHIVED: 'READONLY'
    },
    sceneOverrides: []
  } as const
}

function createBusinessBlock(fieldKey: string, label: string, componentCode: string) {
  return {
    blockId: fieldKey,
    fieldKey,
    kind: 'BUSINESS_COMPONENT' as const,
    label,
    span: 1,
    required: false,
    props: {
      componentCode
    },
    permission: createPermission()
  }
}

describe('ExpenseFormReadonlyRenderer', () => {
  it('renders counterparty label instead of raw supplier code', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('counterparty', '收款单位', 'counterparty')
          ]
        },
        formData: {
          counterparty: 'VEN-001'
        },
        vendorOptionMap: {
          'VEN-001': {
            value: 'VEN-001',
            label: '上海供应商',
            secondaryLabel: 'VEN-001 / SH',
            cVenCode: 'VEN-001',
            cVenName: '上海供应商'
          }
        }
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('上海供应商')
    expect(wrapper.text()).not.toContain('VEN-001')
  })

  it('renders payee label instead of raw encoded value', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('payee', '收款人', 'payee')
          ]
        },
        formData: {
          payee: 'USER:2'
        },
        payeeOptionMap: {
          'USER:2': {
            value: 'USER:2',
            label: '张三',
            sourceType: 'USER',
            sourceCode: '2'
          }
        }
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).not.toContain('USER:2')
  })

  it('renders snapshot payee and payee-account values without relying on lookup maps', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('payee', '收款人', 'payee'),
            createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
          ]
        },
        formData: {
          payee: {
            value: 'PERSONAL_PAYEE:张三',
            label: '张三',
            sourceType: 'PERSONAL_PRIVATE_PAYEE',
            sourceCode: '张三'
          },
          payeeAccount: {
            value: 'USER_ACCOUNT:8',
            label: '张三 / 招商银行',
            sourceType: 'USER',
            ownerName: '张三',
            accountName: '张三',
            accountNoMasked: '6222 **** 8888',
            bankName: '招商银行上海分行'
          }
        }
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('6222 **** 8888')
    expect(wrapper.text()).toContain('招商银行上海分行')
    expect(wrapper.text()).not.toContain('PERSONAL_PAYEE:张三')
    expect(wrapper.text()).not.toContain('USER_ACCOUNT:8')
  })

  it('renders payee account as a card with owner, masked account and bank', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('payee', '收款人', 'payee'),
            createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
          ]
        },
        formData: {
          payee: 'USER:2',
          payeeAccount: 'USER_ACCOUNT:8'
        },
        payeeOptionMap: {
          'USER:2': {
            value: 'USER:2',
            label: '张三',
            sourceType: 'USER',
            sourceCode: '2'
          }
        },
        payeeAccountOptionMap: {
          'USER_ACCOUNT:8': {
            value: 'USER_ACCOUNT:8',
            label: '张三 / 招商银行',
            sourceType: 'USER',
            ownerCode: '2',
            ownerName: '张三',
            bankName: '招商银行上海分行',
            accountName: '张三',
            accountNoMasked: '6222 **** 8899'
          }
        }
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('账户名')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('银行账号')
    expect(wrapper.text()).toContain('6222 **** 8899')
    expect(wrapper.text()).toContain('开户行')
    expect(wrapper.text()).toContain('招商银行上海分行')
    expect(wrapper.text()).not.toContain('USER_ACCOUNT:8')
  })

  it('falls back to raw payee account value when lookup data is missing', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
          ]
        },
        formData: {
          payeeAccount: 'USER_ACCOUNT:8'
        }
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('USER_ACCOUNT:8')
  })

  it('renders payment company label instead of stored company id', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('paymentCompany', '付款公司', 'payment-company')
          ]
        },
        formData: {
          paymentCompany: 'COMPANY-001'
        },
        companyOptions: [
          {
            label: '上海分公司',
            value: 'COMPANY-001'
          }
        ]
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('上海分公司')
    expect(wrapper.text()).not.toContain('COMPANY-001')
  })

  it('renders related document cards without falling back to raw json', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('relatedDocs', '关联单据', 'related-document')
          ]
        },
        formData: {
          relatedDocs: [
            {
              documentCode: 'DOC-REL-001',
              documentTitle: '差旅报销单',
              templateType: 'report',
              templateTypeLabel: '报销单',
              statusLabel: '已审批'
            }
          ]
        }
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('差旅报销单')
    expect(wrapper.text()).toContain('单据编号：DOC-REL-001')
    expect(wrapper.text()).toContain('类型：报销单')
    expect(wrapper.text()).toContain('已审批')
    expect(wrapper.text()).not.toContain('{"documentCode"')
  })

  it('renders writeoff document cards with amount summary fields', () => {
    const wrapper = mount(ExpenseFormReadonlyRenderer, {
      props: {
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document')
          ]
        },
        formData: {
          writeoffDocs: [
            {
              documentCode: 'DOC-WO-001',
              documentTitle: '项目借款单',
              templateType: 'loan',
              templateTypeLabel: '借款单',
              writeOffSourceKind: 'LOAN',
              availableWriteOffAmount: 500,
              writeOffAmount: 120,
              remainingAmount: 380
            }
          ]
        }
      },
      global: {
        stubs: {
          'el-tag': {
            template: '<span><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('项目借款单')
    expect(wrapper.text()).toContain('借款单')
    expect(wrapper.text()).toContain('可核销余额')
    expect(wrapper.text()).toContain('500.00')
    expect(wrapper.text()).toContain('核销金额')
    expect(wrapper.text()).toContain('120.00')
    expect(wrapper.text()).toContain('核销后余额')
    expect(wrapper.text()).toContain('380.00')
  })
})
