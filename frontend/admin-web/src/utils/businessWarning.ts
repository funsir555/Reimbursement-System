import { ElMessageBox } from 'element-plus'

interface BusinessWarningOptions {
  title: string
  message: string
  confirmButtonText?: string
}

export function showBusinessWarning(options: BusinessWarningOptions) {
  return ElMessageBox.alert(options.message, options.title, {
    type: 'warning',
    confirmButtonText: options.confirmButtonText || '知道了',
    customClass: 'business-warning-dialog'
  })
}
