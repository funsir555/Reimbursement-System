import { describe, expect, it } from 'vitest'
import {
  buildApprovalDesignatedMemberOptions,
  normalizeDesignatedMemberValues,
  PROCESS_FLOW_SUBMITTER_MEMBER_VALUE,
  toDesignatedMemberOptionValue
} from '@/views/process/processFlowDesignatedMembers'

describe('processFlowDesignatedMembers', () => {
  it('puts submitter first and avoids duplicate submitter options', () => {
    expect(buildApprovalDesignatedMemberOptions([
      { label: '张三', value: '101' },
      { label: '提单人', value: PROCESS_FLOW_SUBMITTER_MEMBER_VALUE },
      { label: '李四', value: '102' }
    ])).toEqual([
      { label: '提单人', value: PROCESS_FLOW_SUBMITTER_MEMBER_VALUE },
      { label: '张三', value: '101' },
      { label: '李四', value: '102' }
    ])
  })

  it('parses designated member option values', () => {
    expect(toDesignatedMemberOptionValue('101')).toBe(101)
    expect(toDesignatedMemberOptionValue(PROCESS_FLOW_SUBMITTER_MEMBER_VALUE)).toBe(PROCESS_FLOW_SUBMITTER_MEMBER_VALUE)
  })

  it('normalizes mixed designated member selections', () => {
    expect(normalizeDesignatedMemberValues([
      '101',
      202,
      PROCESS_FLOW_SUBMITTER_MEMBER_VALUE,
      null,
      'abc'
    ])).toEqual([
      101,
      202,
      PROCESS_FLOW_SUBMITTER_MEMBER_VALUE
    ])
  })
})
