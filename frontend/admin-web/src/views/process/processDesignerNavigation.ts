import type { LocationQuery, LocationQueryRaw } from 'vue-router'

export function buildReturnToQuery(returnTo: string | undefined, query: LocationQueryRaw = {}): LocationQueryRaw {
  if (!returnTo) {
    return { ...query }
  }
  return {
    ...query,
    returnTo
  }
}

export function resolveReturnToQuery(query: LocationQuery) {
  const raw = query.returnTo
  if (typeof raw === 'string') {
    return raw
  }
  if (Array.isArray(raw) && typeof raw[0] === 'string') {
    return raw[0]
  }
  return ''
}
