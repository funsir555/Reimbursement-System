// 这里集中封装 notification.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request from './core'
import type { NotificationItem, NotificationSummary } from './notification-types'

// 这一组方法供对应页面统一调用。
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
