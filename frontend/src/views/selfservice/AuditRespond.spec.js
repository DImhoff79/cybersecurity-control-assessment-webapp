import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditRespond from './AuditRespond.vue'
import api from '../../services/api'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { auditId: '1' } })
}))

vi.mock('../../services/toast', () => ({
  toastError: vi.fn(),
  toastSuccess: vi.fn(),
  toastWarning: vi.fn()
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
      if (/^\/api\/audit-controls\/\d+\/evidences$/.test(url)) {
        return Promise.resolve({ data: [] })
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
      if (url === '/api/audits/1/submit') return Promise.resolve({ data: { status: 'PENDING_APPROVAL' } })
      return Promise.resolve({ data: {} })
    })

    const wrapper = mount(AuditRespond)
    await flushPromises()

    const select = wrapper.find('select.form-select')
    await select.setValue('YES')
    const submitBtn = wrapper.findAll('button').find((b) => b.text() === 'Submit for review')
    await submitBtn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/audits/1/submit')
  })

  it('shows questionnaire read-only when audit is submitted or in review', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/audits/1') {
        return Promise.resolve({ data: { id: 1, applicationName: 'App', year: 2026, status: 'PENDING_APPROVAL' } })
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
              existingAnswerText: 'YES'
            }
          ]
        })
      }
      if (url === '/api/audits/1/controls') {
        return Promise.resolve({ data: [] })
      }
      if (/^\/api\/audit-controls\/\d+\/evidences$/.test(url)) {
        return Promise.resolve({ data: [] })
      }
      return Promise.resolve({ data: [] })
    })

    const wrapper = mount(AuditRespond)
    await flushPromises()

    expect(wrapper.text()).toContain('Do you review access annually')
    expect(wrapper.text()).toContain('view only')
    expect(wrapper.find('select.form-select').attributes('disabled')).toBeDefined()
    expect(wrapper.findAll('button').find((b) => b.text() === 'Save draft')).toBeUndefined()
  })

  it('shows revisions-requested banner and remains editable', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/audits/1') {
        return Promise.resolve({ data: { id: 1, applicationName: 'App', year: 2026, status: 'REVISIONS_REQUESTED' } })
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
              existingAnswerText: 'YES'
            }
          ]
        })
      }
      if (url === '/api/audits/1/controls') {
        return Promise.resolve({ data: [] })
      }
      if (/^\/api\/audit-controls\/\d+\/evidences$/.test(url)) {
        return Promise.resolve({ data: [] })
      }
      return Promise.resolve({ data: [] })
    })

    const wrapper = mount(AuditRespond)
    await flushPromises()

    expect(wrapper.text()).toContain('Revisions requested')
    expect(wrapper.text()).toContain('auditor has requested changes')
    expect(wrapper.find('.respond-revisions-banner').exists()).toBe(true)
    expect(wrapper.find('select.form-select').attributes('disabled')).toBeUndefined()
    expect(wrapper.findAll('button').find((b) => b.text() === 'Save draft')).toBeDefined()
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
      if (/^\/api\/audit-controls\/\d+\/evidences$/.test(url)) {
        return Promise.resolve({ data: [] })
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
