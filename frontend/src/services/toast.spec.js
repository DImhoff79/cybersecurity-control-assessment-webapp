import { describe, expect, it, vi } from 'vitest'
import { TOAST_EVENT_NAME, toastError, toastInfo, toastSuccess, toastWarning } from './toast'

describe('toast service', () => {
  it('dispatches toast events for all helper variants', () => {
    const handler = vi.fn()
    window.addEventListener(TOAST_EVENT_NAME, handler)

    toastInfo('info message')
    toastSuccess('success message')
    toastWarning('warning message')
    toastError('error message')
    toastInfo('')

    expect(handler).toHaveBeenCalledTimes(4)
    const variants = handler.mock.calls.map((c) => c[0].detail.variant)
    expect(variants).toEqual(['info', 'success', 'warning', 'danger'])

    window.removeEventListener(TOAST_EVENT_NAME, handler)
  })
})
