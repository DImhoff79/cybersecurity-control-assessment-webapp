import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import IssueProgramHub from './IssueProgramHub.vue'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('IssueProgramHub', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.user = {
      id: 1,
      role: 'AUDIT_MANAGER',
      permissions: ['AUDIT_MANAGEMENT', 'RISK_MANAGEMENT', 'REMEDIATION_MANAGEMENT']
    }
    vi.clearAllMocks()
    api.get.mockImplementation((url, config) => {
      if (url === '/api/my-audits') {
        return Promise.resolve({
          data: [{ id: 3, applicationId: 5, applicationName: 'Core Banking', year: 2026 }]
        })
      }
      if (url === '/api/findings') {
        expect(config.params.auditId).toBe(3)
        return Promise.resolve({
          data: [{ id: 10, title: 'Gap', status: 'OPEN', severity: 'HIGH', auditId: 3, applicationName: 'Core Banking' }]
        })
      }
      if (url === '/api/admin/control-exceptions') {
        expect(config.params.auditId).toBe(3)
        return Promise.resolve({ data: [{ id: 20, status: 'REQUESTED', slaState: 'ON_TRACK' }] })
      }
      if (url === '/api/risks') {
        return Promise.resolve({
          data: [{ id: 1, title: 'R1', applicationId: 5 }]
        })
      }
      if (url === '/api/remediation-plans') {
        return Promise.resolve({
          data: [{ id: 7, title: 'Plan A', riskId: 1 }]
        })
      }
      return Promise.resolve({ data: [] })
    })
  })

  it('loads audit-scoped program data from route query', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/admin/issue-program', name: 'AdminIssueProgram', component: { template: '<div />' } }]
    })
    await router.push('/admin/issue-program?auditId=3')
    await router.isReady()

    const wrapper = mount(IssueProgramHub, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Issue Program Hub')
    expect(wrapper.text()).toContain('Core Banking')
    expect(wrapper.text()).toContain('Gap')
    expect(api.get).toHaveBeenCalledWith('/api/findings', { params: { auditId: 3 } })
    expect(api.get).toHaveBeenCalledWith('/api/admin/control-exceptions', { params: { auditId: 3 } })
  })
})
