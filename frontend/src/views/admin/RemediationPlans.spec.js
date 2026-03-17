import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RemediationPlans from './RemediationPlans.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('RemediationPlans', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/remediation-plans') {
        return Promise.resolve({
          data: [{ id: 5, title: 'Privileged access remediation', status: 'IN_PROGRESS', riskId: 1 }]
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

    const buttons = wrapper.findAll('button').filter((b) => b.text().includes('Create'))
    await buttons[0].trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalled()
  })
})
