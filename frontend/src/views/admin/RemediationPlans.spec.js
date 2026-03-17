import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import RemediationPlans from './RemediationPlans.vue'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('RemediationPlans', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.user = { id: 1, role: 'ADMIN', permissions: ['REMEDIATION_MANAGEMENT'] }
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/remediation-plans') {
        return Promise.resolve({
          data: [{
            id: 5,
            title: 'Privileged access remediation',
            status: 'IN_PROGRESS',
            riskId: 1,
            approvalStatus: 'APPROVED'
          }]
        })
      }
      if (url === '/api/risks') return Promise.resolve({ data: [{ id: 1, title: 'Risk 1' }] })
      if (url === '/api/users') return Promise.resolve({ data: [{ id: 2, email: 'owner@example.com' }] })
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  it('loads remediation plans and can create plan/action', async () => {
    const wrapper = mount(RemediationPlans)
    await flushPromises()

    expect(wrapper.text()).toContain('Privileged access remediation')
    expect(api.get).toHaveBeenCalledWith('/api/remediation-plans')
    expect(wrapper.text()).toContain('Approval')

    const buttons = wrapper.findAll('button').filter((b) => b.text().includes('Create'))
    await buttons[0].trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalled()

    const openButton = wrapper.findAll('button').find((b) => b.text() === 'Open')
    await openButton.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Actions - Privileged access remediation')
  })

  it('shows approval actions to audit managers on submitted plans', async () => {
    const authStore = useAuthStore()
    authStore.user = { id: 2, role: 'AUDIT_MANAGER', permissions: ['REMEDIATION_MANAGEMENT'] }
    api.get.mockImplementation((url) => {
      if (url === '/api/remediation-plans') {
        return Promise.resolve({
          data: [{
            id: 7,
            title: 'Encryption remediation',
            status: 'DRAFT',
            riskId: 1,
            approvalStatus: 'SUBMITTED'
          }]
        })
      }
      if (url === '/api/risks') return Promise.resolve({ data: [{ id: 1, title: 'Risk 1' }] })
      if (url === '/api/users') return Promise.resolve({ data: [{ id: 2, email: 'owner@example.com' }] })
      return Promise.resolve({ data: [] })
    })

    const wrapper = mount(RemediationPlans)
    await flushPromises()

    expect(wrapper.text()).toContain('Approve')
    expect(wrapper.text()).toContain('Reject')
  })
})
