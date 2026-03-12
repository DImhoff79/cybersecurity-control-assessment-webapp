import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ControlExceptions from './ControlExceptions.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('ControlExceptions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.spyOn(window, 'prompt').mockImplementation((msg) => {
      if (msg.includes('Expiration')) return ''
      return 'Looks good'
    })
    api.get.mockImplementation((url) => {
      if (url === '/api/my-audits') {
        return Promise.resolve({ data: [{ id: 2, applicationName: 'PCI App', year: 2027 }] })
      }
      if (url === '/api/admin/control-exceptions') {
        return Promise.resolve({
          data: [{
            id: 200,
            auditId: 2,
            applicationName: 'PCI App',
            auditYear: 2027,
            controlId: 'CM-1',
            controlName: 'Config Mgmt',
            status: 'REQUESTED',
            requestedByEmail: 'owner@test.com',
            reason: 'Temporary process'
          }]
        })
      }
      if (url === '/api/audits/2/controls') {
        return Promise.resolve({ data: [{ id: 88, controlControlId: 'CM-1', controlName: 'Config Mgmt' }] })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
  })

  it('loads exceptions and approves a request', async () => {
    const wrapper = mount(ControlExceptions)
    await flushPromises()

    expect(wrapper.text()).toContain('Control Exceptions')
    expect(wrapper.text()).toContain('Temporary process')

    const approveBtn = wrapper.findAll('button').find((b) => b.text() === 'Approve')
    await approveBtn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/admin/control-exceptions/200/approve', {
      decisionNotes: 'Looks good',
      expiresAt: null
    })
  })
})
