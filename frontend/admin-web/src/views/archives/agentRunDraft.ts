export type AgentRunDraft = {
  inputPayloadText: string
}

const STORAGE_PREFIX = 'agent-workbench:run-draft'

export function readAgentRunDraft(
  storage: Storage,
  userKey: string,
  agentId: number,
  fallback: AgentRunDraft
): AgentRunDraft {
  try {
    const raw = storage.getItem(storageKey(userKey, agentId))
    if (!raw) return { ...fallback }
    const parsed = JSON.parse(raw) as Partial<AgentRunDraft>
    return {
      inputPayloadText: typeof parsed.inputPayloadText === 'string' ? parsed.inputPayloadText : fallback.inputPayloadText
    }
  } catch {
    return { ...fallback }
  }
}

export function writeAgentRunDraft(storage: Storage, userKey: string, agentId: number, draft: AgentRunDraft) {
  storage.setItem(storageKey(userKey, agentId), JSON.stringify({
    inputPayloadText: draft.inputPayloadText
  }))
}

export function removeAgentRunDraft(storage: Storage, userKey: string, agentId: number) {
  storage.removeItem(storageKey(userKey, agentId))
}

function storageKey(userKey: string, agentId: number) {
  return `${STORAGE_PREFIX}:${normalizeText(userKey) || 'anonymous'}:${agentId}`
}

function normalizeText(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}
