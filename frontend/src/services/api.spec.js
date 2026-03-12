import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockUseRequest = vi.fn()
const mockUseResponse = vi.fn()
const mockCreate = vi.fn(() => ({
  interceptors: {
    request: { use: mockUseRequest },
    response: { use: mockUseResponse }
  }
}))

vi.mock('axios', () => ({
  default: {
    create: mockCreate
  }
}))

describe('api service interceptors', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    localStorage.clear()
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { pathname: '/admin/audits', href: '' }
    })
  })

  it('adds basic auth header when stored credentials are present', async () => {
    localStorage.setItem('auth_credentials', 'abc123')

    await import('./api.js')
    const requestHandler = mockUseRequest.mock.calls[0][0]
    const config = requestHandler({ headers: {} })

    expect(config.headers.Authorization).toBe('Basic abc123')
  })

  it('clears auth and redirects on 401 responses', async () => {
    localStorage.setItem('auth_credentials', 'abc123')
    localStorage.setItem('auth_mode', 'basic')

    await import('./api.js')
    const responseErrorHandler = mockUseResponse.mock.calls[0][1]
    await expect(responseErrorHandler({ response: { status: 401 } })).rejects.toBeTruthy()

    expect(localStorage.getItem('auth_credentials')).toBeNull()
    expect(localStorage.getItem('auth_mode')).toBeNull()
    expect(window.location.href).toContain('/login?redirect=')
  })
})
