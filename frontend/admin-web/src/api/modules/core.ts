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

export interface RequestOptions extends RequestInit {
  timeoutMs?: number
  timeoutMessage?: string
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  const token = localStorage.getItem('token')
  const { timeoutMs, timeoutMessage, signal, ...fetchOptions } = options
  const isFormDataBody = typeof FormData !== 'undefined' && fetchOptions.body instanceof FormData
  const headers: Record<string, string> = {
    ...(fetchOptions.headers as Record<string, string>)
  }

  if (!isFormDataBody && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

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

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${url}`, {
      ...fetchOptions,
      headers,
      signal: requestSignal
    })
  } catch (error: unknown) {
    if (timeoutHandle) {
      window.clearTimeout(timeoutHandle)
    }
    if (didTimeout) {
      throw new Error(timeoutMessage || '请求超时，请稍后重试')
    }
    throw error
  }

  if (timeoutHandle) {
    window.clearTimeout(timeoutHandle)
  }

  const result = await response.json().catch(() => ({ message: '请求失败' }))

  if (response.status === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error(result.message || '登录已过期，请重新登录')
  }

  if (!response.ok) {
    throw new Error(result.message || `HTTP ${response.status}`)
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

export async function downloadBinaryFile(url: string, fallbackFileName?: string) {
  const token = localStorage.getItem('token')
  const headers: Record<string, string> = {}
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    method: 'GET',
    headers
  })

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
      // Ignore JSON parsing failures for binary responses.
    }
    throw new Error(message)
  }

  const blob = await response.blob()
  const fileName = decodeContentDispositionFileName(response.headers.get('Content-Disposition')) || fallbackFileName || 'download.xlsx'
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
