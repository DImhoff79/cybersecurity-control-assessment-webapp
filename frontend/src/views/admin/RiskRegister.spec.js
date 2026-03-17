import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RiskRegister from './RiskRegister.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('RiskRegister', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/risks') {
        return Promise.resolve({
          data: [{ id: 1, title: 'Privileged access risk', inherentRiskScore: 20, residualRiskScore: 8, status: 'OPEN' }]
        })
      }
      if (url === '/api/applications') return Promise.resolve({ data: [] })
      if (url === '/api/users') return Promise.resolve({ data: [] })
      if (url === '/api/reports/risk-kpis') return Promise.resolve({ data: { openRisks: 1, highRisks: 1, overdueRemediationActions: 0 } })
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  it('loads risk register and can create a risk item', async () => {
    const wrapper = mount(RiskRegister)
    await flushPromises()

    expect(wrapper.text()).toContain('Risk title')
    expect(wrapper.text()).toContain('Likelihood (1-5)')
    expect(wrapper.text()).toContain('Impact (1-5)')

    expect(wrapper.text()).toContain('Privileged access risk')
    expect(api.get).toHaveBeenCalledWith('/api/risks')

    const createButton = wrapper.find('form button.btn-primary')
    await createButton.trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalled()
  })
})
