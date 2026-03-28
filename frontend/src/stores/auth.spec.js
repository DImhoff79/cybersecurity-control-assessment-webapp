import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from './auth'

const mockGet = vi.fn()

vi.mock('../services/api', () => ({
  default: {
    get: (...args) => mockGet(...args),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }
}))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockGet.mockReset()
    localStorage.clear()
  })

  it('hasCredentials is false without storage', () => {
    const store = useAuthStore()
    expect(store.hasCredentials).toBe(false)
  })

  it('setCredentials stores basic auth and fetches user', async () => {
    mockGet.mockResolvedValueOnce({
      data: { id: 1, email: 'u@test.com', displayName: 'U', role: 'ADMIN', permissions: [] }
    })
    const store = useAuthStore()
    const user = await store.setCredentials('u@test.com', 'secret')
    expect(user?.email).toBe('u@test.com')
    expect(store.hasCredentials).toBe(true)
    expect(mockGet).toHaveBeenCalledWith('/api/auth/me')
  })

  it('clearCredentials resets user', async () => {
    mockGet.mockResolvedValueOnce({
      data: { id: 2, email: 'x@test.com', displayName: 'X', role: 'AUDITOR', permissions: ['REPORT_VIEW'] }
    })
    const store = useAuthStore()
    await store.setCredentials('x@test.com', 'pw')
    store.clearCredentials()
    expect(store.user).toBeNull()
    expect(store.hasCredentials).toBe(false)
  })

  it('hasPermission reflects user permissions', async () => {
    mockGet.mockResolvedValueOnce({
      data: {
        id: 3,
        email: 'm@test.com',
        displayName: 'M',
        role: 'AUDIT_MANAGER',
        permissions: ['AUDIT_MANAGEMENT', 'REPORT_VIEW']
      }
    })
    const store = useAuthStore()
    await store.setCredentials('m@test.com', 'pw')
    expect(store.hasPermission('AUDIT_MANAGEMENT')).toBe(true)
    expect(store.hasPermission('USER_MANAGEMENT')).toBe(false)
  })
})
