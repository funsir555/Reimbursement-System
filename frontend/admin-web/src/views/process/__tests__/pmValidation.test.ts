import { describe, expect, it } from 'vitest'
import { validateTemplateBindingValue } from '@/views/process/pmValidation'

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
})
