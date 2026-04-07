import { describe, expect, it } from 'vitest'
import {
  resolveDisabledExpenseDetailActionHint,
  resolveExpenseDetailActions
} from '@/views/expense/expenseDetailActionMatrix'

describe('expenseDetailActionMatrix', () => {
  it('returns submitter pending actions with recall first and reminder last', () => {
    const actions = resolveExpenseDetailActions({
      statusBucket: 'pending',
      isSubmitter: true,
      isActiveApprover: false,
      isFlowParticipant: true,
      canComment: true,
      canApprovalView: false
    })

    expect(actions.map((item) => item.key)).toEqual(['recall', 'print', 'comment', 'download', 'remind'])
    expect(actions[0]).toMatchObject({ key: 'recall', primary: true, label: '召回' })
    expect(actions.slice(1).every((item) => !item.primary)).toBe(true)
  })

  it('returns active approver pending actions with highlighted approve and reject', () => {
    const actions = resolveExpenseDetailActions({
      statusBucket: 'pending',
      isSubmitter: false,
      isActiveApprover: true,
      isFlowParticipant: true,
      canComment: true,
      canApprovalView: true,
      prevDocumentCode: 'DOC-001',
      nextDocumentCode: 'DOC-003'
    })

    expect(actions.map((item) => item.key)).toEqual([
      'approve',
      'reject',
      'prev',
      'next',
      'modify',
      'print',
      'add-sign',
      'transfer',
      'comment',
      'download'
    ])
    expect(actions[0]).toMatchObject({ key: 'approve', primary: true, type: 'primary', label: '通过' })
    expect(actions[1]).toMatchObject({ key: 'reject', primary: true, type: 'danger', label: '驳回' })
    expect(actions.find((item) => item.key === 'prev')?.disabled).toBe(false)
    expect(actions.find((item) => item.key === 'next')?.disabled).toBe(false)
  })

  it('locks approver exception handling actions and exposes the disabled hint', () => {
    const actions = resolveExpenseDetailActions({
      statusBucket: 'exception',
      isSubmitter: false,
      isActiveApprover: false,
      isFlowParticipant: true,
      canComment: true,
      canApprovalView: true
    })

    expect(actions.map((item) => item.key)).toEqual([
      'approve',
      'reject',
      'prev',
      'next',
      'modify',
      'print',
      'add-sign',
      'transfer',
      'comment',
      'download'
    ])
    expect(actions.filter((item) => item.disabled).map((item) => item.key)).toEqual([
      'approve',
      'reject',
      'prev',
      'next',
      'modify',
      'add-sign',
      'transfer'
    ])
    expect(actions.find((item) => item.key === 'print')?.label).toBe('打印')
    expect(actions.find((item) => item.key === 'comment')?.label).toBe('评论')
    expect(resolveDisabledExpenseDetailActionHint(actions)).toContain('异常单据需由发起人召回后重提')
  })

  it('hides comment for pure query viewers who are not flow participants', () => {
    const actions = resolveExpenseDetailActions({
      statusBucket: 'terminal',
      isSubmitter: false,
      isActiveApprover: false,
      isFlowParticipant: false,
      canComment: false,
      canApprovalView: true
    })

    expect(actions.map((item) => item.key)).toEqual(['print', 'download'])
  })
})
