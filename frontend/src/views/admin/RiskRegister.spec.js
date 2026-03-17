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
    window.history.pushState({}, '', '/admin/risk-register')
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/risks') {
        return Promise.resolve({
          data: [{ id: 1, title: 'Privileged access risk', inherentRiskScore: 20, residualRiskScore: 8, status: 'OPEN' }]
        })
      }
      if (url === '/api/applications') return Promise.resolve({ data: [{ id: 7, name: 'Payments Platform' }] })
      if (url === '/api/users') return Promise.resolve({ data: [] })
      if (url === '/api/reports/risk-kpis') return Promise.resolve({ data: { openRisks: 1, highRisks: 1, overdueRemediationActions: 0 } })
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  it('loads risk register and can create a risk item', async () => {
    const wrapper = mount(RiskRegister, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Risk title')
    expect(wrapper.text()).toContain('Likelihood (1-5)')
    expect(wrapper.text()).toContain('Impact (1-5)')
    expect(wrapper.text()).toContain('Application attribution')

    expect(wrapper.text()).toContain('Privileged access risk')
    expect(api.get).toHaveBeenCalledWith('/api/risks')

    const attributionSelect = wrapper.findAll('form select')[1]
    await attributionSelect.setValue('OTHER')
    await flushPromises()
    await wrapper.find('input[placeholder*="Describe the system"]').setValue('Shared identity service')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/risks', expect.objectContaining({
      otherApplicationText: 'Shared identity service',
      applicationId: null
    }))
  })

  it('prefills handoff context from finding query params', async () => {
    window.history.pushState({}, '', '/admin/risk-register?findingId=100&findingTitle=Gap%20found&applicationName=App%20One')
    const wrapper = mount(RiskRegister, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Handoff context from finding #100')
    expect(wrapper.find('input[placeholder="Risk title"]').element.value).toContain('Risk from finding #100')
    expect(wrapper.find('input[placeholder="Optional details to preserve finding context"]').element.value)
      .toContain('Escalated from finding #100')
  })
})
