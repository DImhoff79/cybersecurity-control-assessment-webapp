import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
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
      if (url === '/api/applications') return Promise.resolve({ data: [{ id: 7, name: 'Payments Platform' }] })
      if (url === '/api/users') return Promise.resolve({ data: [] })
      if (url === '/api/reports/risk-kpis') return Promise.resolve({ data: { openRisks: 1, highRisks: 1, overdueRemediationActions: 0 } })
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  async function mountRiskRegister(path = '/admin/risk-register') {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/admin/risk-register', name: 'AdminRiskRegister', component: { template: '<div />' } }]
    })
    await router.push(path)
    await router.isReady()
    return mount(RiskRegister, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
  }

  it('loads risk register and can create a risk item', async () => {
    const wrapper = await mountRiskRegister()
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
    const path =
      '/admin/risk-register?findingId=100&findingTitle=Gap%20found&applicationName=App%20One'
    const wrapper = await mountRiskRegister(path)
    await flushPromises()

    expect(wrapper.text()).toContain('Handoff context from finding #100')
    expect(wrapper.find('input[placeholder="Risk title"]').element.value).toContain('Risk from finding #100')
    expect(wrapper.find('input[placeholder="Optional details to preserve finding context"]').element.value)
      .toContain('Escalated from finding #100')
  })

  it('filters table rows when applicationId is present in the route', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/risks') {
        return Promise.resolve({
          data: [
            { id: 1, title: 'In app 7', status: 'OPEN', inherentRiskScore: 10, applicationId: 7, applicationName: 'A' },
            { id: 2, title: 'Other app', status: 'OPEN', inherentRiskScore: 5, applicationId: 99, applicationName: 'B' }
          ]
        })
      }
      if (url === '/api/applications') return Promise.resolve({ data: [{ id: 7, name: 'Payments Platform' }] })
      if (url === '/api/users') return Promise.resolve({ data: [] })
      if (url === '/api/reports/risk-kpis') return Promise.resolve({ data: {} })
      return Promise.resolve({ data: [] })
    })
    const wrapper = await mountRiskRegister('/admin/risk-register?applicationId=7&applicationName=Payments')
    await flushPromises()

    expect(wrapper.text()).toContain('In app 7')
    expect(wrapper.text()).not.toContain('Other app')
    expect(wrapper.text()).toContain('Clear application filter')
  })
})
