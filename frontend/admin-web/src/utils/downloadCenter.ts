const DOWNLOAD_CENTER_OPEN_EVENT = 'finex-download-center:open'

export function openDownloadCenter() {
  window.dispatchEvent(new CustomEvent(DOWNLOAD_CENTER_OPEN_EVENT))
}

export function onDownloadCenterOpen(handler: () => void) {
  window.addEventListener(DOWNLOAD_CENTER_OPEN_EVENT, handler)
  return () => window.removeEventListener(DOWNLOAD_CENTER_OPEN_EVENT, handler)
}
