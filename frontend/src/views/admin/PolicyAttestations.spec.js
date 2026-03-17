import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import PolicyAttestations from './PolicyAttestations.vue'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('PolicyAttestations', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.user = { id: 1, role: 'AUDIT_MANAGER', permissions: ['REPORT_VIEW', 'POLICY_MANAGEMENT'] }
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/policies') {
        return Promise.resolve({ data: [{ id: 1, code: 'POL-1', name: 'Access Policy', nextReviewAt: null }] })
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
    const wrapper = mount(PolicyAttestations, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Policy Attestations')
    expect(wrapper.text()).toContain('owner@example.com')
    expect(wrapper.text()).toContain('Pending Acknowledgements')
    expect(wrapper.text()).toContain('Open Policy Workspace')

    const refreshBtn = wrapper.find('button.btn-outline-secondary')
    await refreshBtn.trigger('click')
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/api/policies/acknowledgements', { params: {} })
  })
})
