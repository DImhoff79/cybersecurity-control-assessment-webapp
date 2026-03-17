import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PolicyAttestations from './PolicyAttestations.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('PolicyAttestations', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/policies') {
        return Promise.resolve({ data: [{ id: 1, code: 'POL-1', name: 'Access Policy' }] })
      }
      if (url === '/api/policies/acknowledgements') {
        return Promise.resolve({
          data: [
            {
              id: 3,
              policyCode: 'POL-1',
              policyVersionNumber: 2,
              policyVersionTitle: 'v2',
              userEmail: 'owner@example.com',
              status: 'OVERDUE',
              assignedAt: null,
              dueAt: null,
              acknowledgedAt: null
            }
          ]
        })
      }
      return Promise.resolve({ data: [] })
    })
  })

  it('loads attestations and supports filter refresh', async () => {
    const wrapper = mount(PolicyAttestations)
    await flushPromises()

    expect(wrapper.text()).toContain('Policy Attestations')
    expect(wrapper.text()).toContain('owner@example.com')

    const refreshBtn = wrapper.find('button.btn-outline-secondary')
    await refreshBtn.trigger('click')
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/api/policies/acknowledgements', { params: {} })
  })
})
