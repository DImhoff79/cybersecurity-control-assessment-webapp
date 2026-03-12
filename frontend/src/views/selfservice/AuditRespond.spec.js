import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditRespond from './AuditRespond.vue'
import api from '../../services/api'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { auditId: '1' } })
}))

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('AuditRespond', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('alert', vi.fn())

    api.get.mockImplementation((url) => {
      if (url === '/api/audits/1') {
        return Promise.resolve({ data: { id: 1, applicationName: 'App', year: 2026, status: 'IN_PROGRESS' } })
      }
      if (url === '/api/audits/1/questions') {
        return Promise.resolve({
          data: [
            {
              questionId: 101,
              auditControlId: 201,
              controlId: 11,
              controlControlId: 'AC-1',
              controlName: 'Access Policy',
              questionText: 'Do you review access annually?',
              helpText: '',
              displayOrder: 0,
              existingAnswerText: ''
            },
            {
              questionId: 101,
              auditControlId: 202,
              controlId: 12,
              controlControlId: 'PCI-7',
              controlName: 'Restrict Access',
              questionText: 'Do you review access annually?',
              helpText: '',
              displayOrder: 0,
              existingAnswerText: ''
            }
          ]
        })
      }
      if (url === '/api/audits/1/controls') {
        return Promise.resolve({
          data: [
            { id: 201, controlId: 11, controlControlId: 'AC-1', controlName: 'Access Policy', status: 'NOT_STARTED' },
            { id: 202, controlId: 12, controlControlId: 'PCI-7', controlName: 'Restrict Access', status: 'NOT_STARTED' }
          ]
        })
      }
      if (url === '/api/controls?includeQuestions=true') {
        return Promise.resolve({
          data: [
            { id: 11, questions: [{ id: 101 }] },
            { id: 12, questions: [{ id: 101 }] }
          ]
        })
      }
      return Promise.resolve({ data: [] })
    })

    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  it('sends one owner answer to all mapped controls', async () => {
    const wrapper = mount(AuditRespond)
    await flushPromises()

    const select = wrapper.find('select.form-select')
    await select.setValue('YES')
    await wrapper.findAll('button').find((b) => b.text() === 'Save draft').trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/audits/1/answers', {
      answers: [
        { questionId: 101, auditControlId: 201, answerText: 'YES' },
        { questionId: 101, auditControlId: 202, answerText: 'YES' }
      ]
    })
  })

  it('submits assessment for admin review when complete', async () => {
    api.post.mockImplementation((url) => {
      if (url === '/api/audits/1/submit') return Promise.resolve({ data: { status: 'SUBMITTED' } })
      return Promise.resolve({ data: {} })
    })

    const wrapper = mount(AuditRespond)
    await flushPromises()

    const select = wrapper.find('select.form-select')
    await select.setValue('YES')
    const submitBtn = wrapper.findAll('button').find((b) => b.text() === 'Submit assessment for review')
    await submitBtn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/audits/1/submit')
  })

  it('groups similar guided questions into the same section', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/audits/1') {
        return Promise.resolve({ data: { id: 1, applicationName: 'App', year: 2026, status: 'IN_PROGRESS' } })
      }
      if (url === '/api/audits/1/questions') {
        return Promise.resolve({
          data: [
            {
              questionId: 201,
              auditControlId: 301,
              controlId: 21,
              controlControlId: 'AC-2',
              controlName: 'Account Management',
              questionText: 'Do you review user access regularly?',
              helpText: '',
              displayOrder: 0,
              existingAnswerText: ''
            },
            {
              questionId: 202,
              auditControlId: 302,
              controlId: 22,
              controlControlId: 'IA-2',
              controlName: 'Identification and Authentication',
              questionText: 'Do users sign in with MFA?',
              helpText: '',
              displayOrder: 1,
              existingAnswerText: ''
            }
          ]
        })
      }
      if (url === '/api/audits/1/controls') {
        return Promise.resolve({
          data: [
            { id: 301, controlId: 21, controlControlId: 'AC-2', controlName: 'Account Management', status: 'NOT_STARTED' },
            { id: 302, controlId: 22, controlControlId: 'IA-2', controlName: 'Identification and Authentication', status: 'NOT_STARTED' }
          ]
        })
      }
      return Promise.resolve({ data: [] })
    })

    const wrapper = mount(AuditRespond)
    await flushPromises()

    expect(wrapper.text()).toContain('Access and Identity')
    expect(wrapper.text()).toContain('Do you review user access regularly?')
    expect(wrapper.text()).toContain('Do users sign in with MFA?')
  })
})
