import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
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
      if (url === '/api/risks') return Promise.resolve({ data: [{ id: 1, title: 'Risk 1', applicationId: 100 }] })
      if (url === '/api/users') return Promise.resolve({ data: [{ id: 2, email: 'owner@example.com' }] })
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  async function mountRemediation(path = '/admin/remediation-plans') {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/admin/remediation-plans', name: 'AdminRemediationPlans', component: { template: '<div />' } }]
    })
    await router.push(path)
    await router.isReady()
    return mount(RemediationPlans, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
  }

  it('loads remediation plans and can create plan/action', async () => {
    const wrapper = await mountRemediation()
    await flushPromises()

    expect(wrapper.text()).toContain('Privileged access remediation')
    expect(api.get).toHaveBeenCalledWith('/api/remediation-plans')
    expect(wrapper.text()).toContain('Approval')
    expect(wrapper.text()).toContain('Execution Board')
    expect(wrapper.text()).toContain('Approved / Active')

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
      if (url === '/api/risks') return Promise.resolve({ data: [{ id: 1, title: 'Risk 1', applicationId: 1 }] })
      if (url === '/api/users') return Promise.resolve({ data: [{ id: 2, email: 'owner@example.com' }] })
      return Promise.resolve({ data: [] })
    })

    const wrapper = await mountRemediation()
    await flushPromises()

    expect(wrapper.text()).toContain('Approve')
    expect(wrapper.text()).toContain('Reject')
  })

  it('prefills remediation handoff context from query params', async () => {
    const path =
      '/admin/remediation-plans?findingId=100&findingTitle=Gap%20found&riskId=1&riskTitle=Risk%201'
    const wrapper = await mountRemediation(path)
    await flushPromises()

    expect(wrapper.text()).toContain('Handoff from finding #100')
    expect(wrapper.find('input[placeholder="Plan title"]').element.value).toContain('Remediation for finding #100')
  })

  it('filters plans when applicationId matches risk application', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/remediation-plans') {
        return Promise.resolve({
          data: [
            { id: 1, title: 'In scope', status: 'IN_PROGRESS', riskId: 10, approvalStatus: 'APPROVED' },
            { id: 2, title: 'Out of scope', status: 'IN_PROGRESS', riskId: 99, approvalStatus: 'APPROVED' }
          ]
        })
      }
      if (url === '/api/risks') {
        return Promise.resolve({
          data: [
            { id: 10, title: 'R10', applicationId: 5 },
            { id: 99, title: 'R99', applicationId: 500 }
          ]
        })
      }
      if (url === '/api/users') return Promise.resolve({ data: [] })
      return Promise.resolve({ data: [] })
    })
    const wrapper = await mountRemediation('/admin/remediation-plans?applicationId=5')
    await flushPromises()

    expect(wrapper.text()).toContain('In scope')
    expect(wrapper.text()).not.toContain('Out of scope')
  })
})
