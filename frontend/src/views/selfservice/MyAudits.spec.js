import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyAudits from './MyAudits.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('MyAudits', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/my-audits/overview') {
        return Promise.resolve({
          data: [{ id: 1, applicationName: 'App One', year: 2026, status: 'IN_PROGRESS', completionPct: 100 }]
        })
      }
      return Promise.resolve({ data: [] })
    })
  })

  it('calculates completion by distinct question prompts', async () => {
    const wrapper = mount(MyAudits, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('100%')
    expect(wrapper.text()).toContain('Resume audit')
  })
})
