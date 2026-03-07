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
      if (url === '/api/my-audits') {
        return Promise.resolve({
          data: [{ id: 1, applicationName: 'App One', year: 2026, status: 'IN_PROGRESS' }]
        })
      }
      if (url === '/api/controls?includeQuestions=true') {
        return Promise.resolve({
          data: [
            { id: 201, questions: [{ id: 101 }] },
            { id: 202, questions: [{ id: 101 }] }
          ]
        })
      }
      if (url === '/api/audits/1/questions') {
        return Promise.resolve({
          data: [
            { questionId: 101, auditControlId: 201, existingAnswerText: 'YES' },
            { questionId: 101, auditControlId: 202, existingAnswerText: 'YES' }
          ]
        })
      }
      if (url === '/api/audits/1/controls') {
        return Promise.resolve({
          data: [
            { id: 201, controlId: 11, status: 'NOT_STARTED' },
            { id: 202, controlId: 11, status: 'NOT_STARTED' }
          ]
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
