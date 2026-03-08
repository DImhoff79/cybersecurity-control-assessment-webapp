export const TOAST_EVENT_NAME = 'app-toast'

function pushToast(message, variant = 'info', timeout = 3500) {
  if (!message) return
  window.dispatchEvent(
    new CustomEvent(TOAST_EVENT_NAME, {
      detail: {
        id: `${Date.now()}-${Math.random()}`,
        message,
        variant,
        timeout
      }
    })
  )
}

export function toastInfo(message, timeout) {
  pushToast(message, 'info', timeout)
}

export function toastSuccess(message, timeout) {
  pushToast(message, 'success', timeout)
}

export function toastWarning(message, timeout) {
  pushToast(message, 'warning', timeout)
}

export function toastError(message, timeout) {
  pushToast(message, 'danger', timeout)
}
