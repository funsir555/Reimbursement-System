import request from './core'
import type { NotificationItem, NotificationSummary } from './notification-types'

export const notificationApi = {
  getSummary: () => request<NotificationSummary>('/auth/async-tasks/notifications/summary'),
  list: () => request<NotificationItem[]>('/auth/async-tasks/notifications'),
  markRead: (id: number) =>
    request<boolean>(`/auth/async-tasks/notifications/${id}/read`, {
      method: 'POST'
    }),
  markAllRead: () =>
    request<boolean>('/auth/async-tasks/notifications/read-all', {
      method: 'POST'
    })
}
