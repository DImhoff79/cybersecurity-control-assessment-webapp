import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UserManagement from './UserManagement.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('UserManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))
    api.get.mockImplementation((url) => {
      if (url === '/api/users') {
        return Promise.resolve({ data: [{ id: 1, email: 'admin@test.com', displayName: 'Admin', role: 'ADMIN' }] })
      }
      if (url === '/api/admin/access-requests') {
        return Promise.resolve({ data: [{ id: 7, email: 'new@test.com', provider: 'GOOGLE', requestedAt: null }] })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
    api.delete.mockResolvedValue({ data: {} })
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

  it('creates, updates, rejects request, and deletes user', async () => {
    const wrapper = mount(UserManagement)
    await flushPromises()

    const inputs = wrapper.findAll('form input')
    await inputs[0].setValue('owner2@test.com')
    await inputs[1].setValue('Owner Two')
    await inputs[2].setValue('ownerpass123')
    await wrapper.find('form select').setValue('APPLICATION_OWNER')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/users', expect.objectContaining({
      email: 'owner2@test.com',
      role: 'APPLICATION_OWNER'
    }))

    const rejectBtn = wrapper.findAll('button').find((b) => b.text() === 'Reject')
    await rejectBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/admin/access-requests/7/reject', {})

    const userRoleSelect = wrapper.findAll('tbody select')[1]
    await userRoleSelect.setValue('AUDIT_MANAGER')
    const saveBtn = wrapper.findAll('button').find((b) => b.text() === 'Save')
    await saveBtn.trigger('click')
    await flushPromises()
    expect(api.put).toHaveBeenCalledWith('/api/users/1', { role: 'AUDIT_MANAGER' })

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    await deleteBtn.trigger('click')
    await flushPromises()
    expect(api.delete).toHaveBeenCalledWith('/api/users/1')
  })
})
