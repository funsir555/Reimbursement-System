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
