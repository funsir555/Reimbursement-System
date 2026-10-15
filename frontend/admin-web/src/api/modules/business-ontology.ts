import request from './core'

export type OntologyKind = 'ENTITY' | 'FIELD' | 'ENUM' | 'RELATION' | 'RULE' | 'METRIC' | 'CAPABILITY'
export type OntologyStatus = 'DRAFT' | 'PUBLISHED' | 'RETIRED'

export interface OntologyItem {
  id: number
  itemKey: string
  kind: OntologyKind
  entityKey: string | null
  displayName: string
  description: string
  aliases: string[]
  valueCode: string | null
  targetKey: string | null
  cardinality: string | null
  semanticType: string | null
  unitName: string | null
  toolCode: string | null
  ownerDomain: string
  sourceRef: string
  versionNo: number
  status: OntologyStatus
  effectiveFrom: string
  effectiveUntil: string | null
  createdBy: number
  reviewedBy: number | null
}

export type OntologyDraft = Omit<OntologyItem, 'id' | 'versionNo' | 'status' | 'createdBy' | 'reviewedBy'>

export interface OntologyResultLink {
  sourceToolCode: string
  sourceField: string
  sourceLabel: string
  targetToolCode: string
  targetField: string
  targetLabel: string
  relationKind: 'REFERENCE' | 'HIERARCHY' | 'AGGREGATE'
  explanation: string
  scopeNote: string
  versionNo: number
}

const base = '/auth/agent/business-ontology'
export const businessOntologyApi = {
  list: (keyword = '') => request<OntologyItem[]>(`${base}?keyword=${encodeURIComponent(keyword)}`),
  resultLinks: () => request<OntologyResultLink[]>(`${base}/result-links`),
  saveDraft: (item: OntologyDraft) => request<OntologyItem>(`${base}/drafts`, {
    method: 'POST', body: JSON.stringify(item)
  }),
  publish: (id: number) => request<unknown>(`${base}/${id}/publish`, { method: 'POST' }),
  retire: (id: number) => request<unknown>(`${base}/${id}/retire`, { method: 'POST' })
}
