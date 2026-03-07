import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import KickoffAudit from './KickoffAudit.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('KickoffAudit', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))

    api.get.mockImplementation((url) => {
      if (url === '/api/applications') {
        return Promise.resolve({ data: [{ id: 1, name: 'App' }] })
      }
      if (url === '/api/users') {
        return Promise.resolve({ data: [{ id: 2, email: 'owner@example.com' }] })
      }
      if (url === '/api/applications/1/audits') {
        return Promise.resolve({
          data: [{ id: 11, year: 2026, status: 'IN_PROGRESS', assignedToEmail: 'owner@example.com' }]
        })
      }
      return Promise.resolve({ data: [] })
    })

    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
    api.delete.mockResolvedValue({ data: {} })
  })

  it('deletes audit from admin history', async () => {
    const wrapper = mount(KickoffAudit, {
      global: {
        stubs: {
          BsModal: true,
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    expect(deleteBtn).toBeTruthy()
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(api.delete).toHaveBeenCalledWith('/api/audits/11')
  })
})
