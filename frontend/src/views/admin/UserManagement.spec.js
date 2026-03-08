import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UserManagement from './UserManagement.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('UserManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/users') {
        return Promise.resolve({ data: [{ id: 1, email: 'admin@test.com', role: 'ADMIN' }] })
      }
      if (url === '/api/admin/access-requests') {
        return Promise.resolve({ data: [{ id: 7, email: 'new@test.com', provider: 'GOOGLE', requestedAt: null }] })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
  })

  it('loads pending requests and approves one', async () => {
    const wrapper = mount(UserManagement)
    await flushPromises()

    expect(wrapper.text()).toContain('Pending Social Access Requests')
    expect(wrapper.text()).toContain('new@test.com')

    const approveBtn = wrapper.findAll('button').find((b) => b.text() === 'Approve')
    await approveBtn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/admin/access-requests/7/approve', { role: 'APPLICATION_OWNER' })
  })
})
