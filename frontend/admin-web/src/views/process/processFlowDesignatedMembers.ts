import type { ProcessFormOption } from '@/api'

export const PROCESS_FLOW_SUBMITTER_MEMBER_VALUE = 'SUBMITTER' as const

export type ProcessFlowDesignatedMemberValue = number | typeof PROCESS_FLOW_SUBMITTER_MEMBER_VALUE

const submitterOption: ProcessFormOption = {
  label: '提单人',
  value: PROCESS_FLOW_SUBMITTER_MEMBER_VALUE
}

export function buildApprovalDesignatedMemberOptions(userOptions: ProcessFormOption[] = []): ProcessFormOption[] {
  return [
    submitterOption,
    ...userOptions.filter((item) => String(item?.value || '').trim() !== PROCESS_FLOW_SUBMITTER_MEMBER_VALUE)
  ]
}

export function toDesignatedMemberOptionValue(value: unknown): ProcessFlowDesignatedMemberValue {
  if (String(value || '').trim() === PROCESS_FLOW_SUBMITTER_MEMBER_VALUE) {
    return PROCESS_FLOW_SUBMITTER_MEMBER_VALUE
  }
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : PROCESS_FLOW_SUBMITTER_MEMBER_VALUE
}

export function normalizeDesignatedMemberValues(source: unknown): ProcessFlowDesignatedMemberValue[] {
  if (!Array.isArray(source)) {
    return []
  }

  const values: ProcessFlowDesignatedMemberValue[] = []
  source.forEach((item) => {
    if (item === undefined || item === null || String(item).trim() === '') {
      return
    }
    if (String(item || '').trim() === PROCESS_FLOW_SUBMITTER_MEMBER_VALUE) {
      values.push(PROCESS_FLOW_SUBMITTER_MEMBER_VALUE)
      return
    }
    const numeric = Number(item)
    if (Number.isFinite(numeric)) {
      values.push(numeric)
    }
  })
  return values
}
