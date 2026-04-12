// 这里定义 notification-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

// 这是 NotificationSummary 的数据结构。
export interface NotificationSummary {
  unreadCount: number
  latestTitle?: string
  latestContent?: string
  latestCreatedAt?: string
}

export interface NotificationItem {
  id: number
  title?: string
  content?: string
  type?: string
  status: string
  relatedTaskNo?: string
  createdAt?: string
  readAt?: string
}
