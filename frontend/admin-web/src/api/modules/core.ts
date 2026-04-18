const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

function clearLoginState() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export type MoneyValue = string

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  items: T[]
}

export interface RequestOptions extends RequestInit {
  timeoutMs?: number
  timeoutMessage?: string
}

export interface BinaryRequestOptions extends RequestOptions {
  fallbackFileName?: string
}

export interface BinaryFileResponse {
  blob: Blob
  fileName: string
  contentType: string
}

function withTimeout<T extends RequestOptions>(options: T) {
  const { timeoutMs, timeoutMessage, signal, ...fetchOptions } = options
  let timeoutHandle: ReturnType<typeof window.setTimeout> | undefined
  let didTimeout = false
  let requestSignal = signal

  if (typeof timeoutMs === 'number' && timeoutMs > 0) {
    const controller = new AbortController()
    requestSignal = controller.signal

    if (signal) {
      if (signal.aborted) {
        controller.abort(signal.reason)
      } else {
        signal.addEventListener('abort', () => controller.abort(signal.reason), { once: true })
      }
    }

    timeoutHandle = window.setTimeout(() => {
      didTimeout = true
      controller.abort(new DOMException('Request timeout', 'AbortError'))
    }, timeoutMs)
  }

  return {
    fetchOptions,
    requestSignal,
    timeoutMessage,
    clear() {
      if (timeoutHandle) {
        window.clearTimeout(timeoutHandle)
      }
    },
    isTimedOut() {
      return didTimeout
    }
  }
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  const token = localStorage.getItem('token')
  const isFormDataBody = typeof FormData !== 'undefined' && options.body instanceof FormData
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>)
  }

  if (!isFormDataBody && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const timeout = withTimeout(options)

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${url}`, {
      ...timeout.fetchOptions,
      headers,
      signal: timeout.requestSignal
    })
  } catch (error: unknown) {
    timeout.clear()
    if (timeout.isTimedOut()) {
      throw new Error(timeout.timeoutMessage || '请求超时，请稍后重试')
    }
    throw error
  }

  timeout.clear()

  const result = await response.json().catch(() => ({ message: '请求失败' }))

  if (response.status === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error((result as { message?: string }).message || '登录已过期，请重新登录')
  }

  if (!response.ok) {
    throw new Error((result as { message?: string }).message || `HTTP ${response.status}`)
  }

  const payload = result as ApiResponse<T>

  if (payload.code === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error(payload.message || '登录已过期，请重新登录')
  }

  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }

  return payload
}

type QueryValue = string | number | boolean | undefined | null

export function buildQueryString<T extends object>(params: T) {
  const search = new URLSearchParams()
  Object.entries(params as Record<string, QueryValue>).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    search.append(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

function decodeContentDispositionFileName(contentDisposition: string | null) {
  if (!contentDisposition) {
    return ''
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      return utf8Match[1]
    }
  }
  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
  return plainMatch?.[1] || ''
}

export async function requestBinary(url: string, options: BinaryRequestOptions = {}): Promise<BinaryFileResponse> {
  const token = localStorage.getItem('token')
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>)
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const timeout = withTimeout(options)

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${url}`, {
      ...timeout.fetchOptions,
      headers,
      signal: timeout.requestSignal
    })
  } catch (error: unknown) {
    timeout.clear()
    if (timeout.isTimedOut()) {
      throw new Error(timeout.timeoutMessage || '请求超时，请稍后重试')
    }
    throw error
  }

  timeout.clear()

  if (response.status === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error('登录已过期，请重新登录')
  }

  if (!response.ok) {
    let message = `HTTP ${response.status}`
    try {
      const payload = await response.json()
      message = payload?.message || message
    } catch {
    }
    throw new Error(message)
  }

  const blob = await response.blob()
  return {
    blob,
    fileName: decodeContentDispositionFileName(response.headers.get('Content-Disposition')) || options.fallbackFileName || 'download.bin',
    contentType: response.headers.get('Content-Type') || blob.type || ''
  }
}

export async function downloadBinaryFile(url: string, fallbackFileName?: string) {
  const { blob, fileName } = await requestBinary(url, {
    method: 'GET',
    fallbackFileName: fallbackFileName || 'download.xlsx'
  })

  const objectUrl = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  window.URL.revokeObjectURL(objectUrl)
}

export default request
