// 这里定义 download-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

// 这是 DownloadRecord 的数据结构。
export interface DownloadRecord {
  id: number
  fileName: string
  businessType: string
  status: string
  progress: number
  fileSize: string
  createdAt: string
  finishedAt?: string
  downloadUrl?: string
  downloadable?: boolean
}

export interface DownloadCenterData {
  inProgress: DownloadRecord[]
  history: DownloadRecord[]
}
