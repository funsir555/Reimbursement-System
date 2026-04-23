import { describe, expect, it } from 'vitest'
import { validateSchemaFieldKeys, validateTemplateBindingValue } from '@/views/process/pmValidation'

describe('pmValidation', () => {
  it('accepts standard options with a value field', () => {
    expect(
      validateTemplateBindingValue('form-001', [{ label: 'Form 1', value: 'form-001' }], 'binding')
    ).toBe('')
  })

  it('accepts expense detail options with a detailCode field', () => {
    expect(
      validateTemplateBindingValue('detail-001', [{ detailCode: 'detail-001' }], 'binding')
    ).toBe('')
  })

  it('returns the invalid binding message when the value is missing', () => {
    expect(
      validateTemplateBindingValue('missing', [{ label: 'Form 1', value: 'form-001' }], 'binding')
    ).toBe('binding\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9')
  })

  it('rejects multiple amount controls on the main form', () => {
    expect(validateSchemaFieldKeys({
      layoutMode: 'TWO_COLUMN',
      blocks: [
        {
          blockId: 'a',
          fieldKey: 'amountA',
          kind: 'CONTROL',
          label: '金额A',
          props: { controlType: 'AMOUNT' },
          permission: { fixedStages: {}, sceneOverrides: [] }
        },
        {
          blockId: 'b',
          fieldKey: 'amountB',
          kind: 'CONTROL',
          label: '金额B',
          props: { controlType: 'AMOUNT' },
          permission: { fixedStages: {}, sceneOverrides: [] }
        }
      ]
    }, '表单设计')).toContain('表单设计只允许保留一个金额控件')
  })

  it('rejects duplicated fixed amount fields on the expense detail form', () => {
    expect(validateSchemaFieldKeys({
      layoutMode: 'TWO_COLUMN',
      blocks: [
        {
          blockId: 'system-amount',
          fieldKey: 'amount',
          kind: 'CONTROL',
          label: '金额',
          props: { controlType: 'AMOUNT', systemFieldCode: 'DETAIL_AMOUNT' },
          permission: { fixedStages: {}, sceneOverrides: [] }
        },
        {
          blockId: 'custom-amount',
          fieldKey: 'customAmount',
          kind: 'CONTROL',
          label: '自定义金额',
          props: { controlType: 'AMOUNT' },
          permission: { fixedStages: {}, sceneOverrides: [] }
        }
      ]
    }, '费用明细表单', { isExpenseDetail: true })).toContain('费用明细表单只允许保留一个金额组件')
  })

  it('requires at least one enabled business scenario for enterprise expense details', () => {
    expect(validateSchemaFieldKeys({
      layoutMode: 'TWO_COLUMN',
      blocks: [
        {
          blockId: 'scenario',
          fieldKey: 'businessScenario',
          kind: 'CONTROL',
          label: '业务场景',
          props: {
            controlType: 'SELECT',
            systemFieldCode: 'BUSINESS_SCENARIO',
            enabledSceneModes: []
          },
          permission: { fixedStages: {}, sceneOverrides: [] }
        }
      ]
    }, '费用明细表单', {
      isExpenseDetail: true,
      detailType: 'ENTERPRISE_TRANSACTION'
    })).toContain('业务场景至少保留一个开启项')
  })

  it('rejects business-scenario defaults outside the enabled modes', () => {
    expect(validateSchemaFieldKeys({
      layoutMode: 'TWO_COLUMN',
      blocks: [
        {
          blockId: 'scenario',
          fieldKey: 'businessScenario',
          kind: 'CONTROL',
          label: '业务场景',
          defaultValue: 'PREPAY_UNBILLED',
          props: {
            controlType: 'SELECT',
            systemFieldCode: 'BUSINESS_SCENARIO',
            enabledSceneModes: ['INVOICE_FULL_PAYMENT']
          },
          permission: { fixedStages: {}, sceneOverrides: [] }
        }
      ]
    }, '费用明细表单', {
      isExpenseDetail: true,
      detailType: 'ENTERPRISE_TRANSACTION'
    })).toContain('业务场景默认值必须属于当前启用场景')
  })

  it('forces normal reimbursement details to keep only full-payment scenario', () => {
    expect(validateSchemaFieldKeys({
      layoutMode: 'TWO_COLUMN',
      blocks: [
        {
          blockId: 'scenario',
          fieldKey: 'businessScenario',
          kind: 'CONTROL',
          label: '业务场景',
          defaultValue: 'PREPAY_UNBILLED',
          props: {
            controlType: 'SELECT',
            systemFieldCode: 'BUSINESS_SCENARIO',
            enabledSceneModes: ['PREPAY_UNBILLED']
          },
          permission: { fixedStages: {}, sceneOverrides: [] }
        }
      ]
    }, '费用明细表单', {
      isExpenseDetail: true,
      detailType: 'NORMAL_REIMBURSEMENT'
    })).toContain('普通报销只能保留全额付款业务场景')
  })
})
