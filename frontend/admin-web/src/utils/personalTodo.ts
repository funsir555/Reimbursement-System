export const PERSONAL_TODO_COUNTS_CHANGED_EVENT = 'personal-todo-counts-changed'

export function notifyPersonalTodoCountsChanged() {
  if (typeof window === 'undefined') {
    return
  }
  window.dispatchEvent(new CustomEvent(PERSONAL_TODO_COUNTS_CHANGED_EVENT))
}

export function countUniqueTodoDocuments(items: Array<{ documentCode?: string }> | undefined) {
  return new Set(
    (items || [])
      .map((item) => String(item.documentCode || '').trim())
      .filter(Boolean)
  ).size
}
