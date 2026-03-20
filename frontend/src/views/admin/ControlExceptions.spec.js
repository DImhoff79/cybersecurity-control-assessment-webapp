import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
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

  async function mountExceptions(path = '/admin/control-exceptions') {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/admin/control-exceptions', name: 'AdminControlExceptions', component: { template: '<div />' } },
        { path: '/admin/issue-program', name: 'AdminIssueProgram', component: { template: '<div />' } },
        { path: '/admin/findings', name: 'AdminFindings', component: { template: '<div />' } }
      ]
    })
    await router.push(path)
    await router.isReady()
    return mount(ControlExceptions, {
      global: {
        plugins: [router],
        stubs: {
          BsModal: {
            props: ['modelValue', 'title'],
            emits: ['update:modelValue'],
            template: '<div><slot /><slot name="footer" /></div>'
          }
        }
      }
    })
  }

  it('loads exceptions and approves a request', async () => {
    const wrapper = await mountExceptions()
    await flushPromises()

    expect(wrapper.text()).toContain('Issue Management - Control Exceptions')
    expect(wrapper.text()).toContain('Temporary process')

    const approveBtn = wrapper.findAll('button').find((b) => b.text() === 'Approve')
    await approveBtn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/admin/control-exceptions/200/approve', {
      decisionNotes: 'Looks good',
      expiresAt: null
    })
  })

  it('applies auditId from route query to exceptions request', async () => {
    await mountExceptions('/admin/control-exceptions?auditId=2')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/api/admin/control-exceptions', { params: { auditId: 2 } })
  })
})
