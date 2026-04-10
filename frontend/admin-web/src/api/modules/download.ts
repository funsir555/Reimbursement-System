import request, { downloadBinaryFile } from './core'
import type { DownloadCenterData } from './download-types'

export const downloadApi = {
  getCenter: () => request<DownloadCenterData>('/auth/user-center/downloads'),
  downloadFile: (id: number, fileName?: string) =>
    downloadBinaryFile(`/auth/user-center/downloads/${id}/content`, fileName)
}
