import { describe, expect, it } from 'vitest'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'

describe('globalFilterableSelectProps', () => {
  it('disables keyword retention after selecting a filtered result', () => {
    expect(globalFilterableSelectProps).toEqual({
      reserveKeyword: false
    })
  })

  it('uses one shared config object for all filterable selects', () => {
    expect(globalFilterableSelectProps.reserveKeyword).toBe(false)
  })
})
