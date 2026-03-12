import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Findings from './Findings.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('Findings', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/my-audits') {
        return Promise.resolve({ data: [{ id: 1, applicationName: 'App One', year: 2026 }] })
      }
      if (url === '/api/users') {
        return Promise.resolve({ data: [{ id: 10, email: 'owner@test.com', displayName: 'Owner' }] })
      }
      if (url === '/api/findings') {
        return Promise.resolve({
          data: [{
            id: 100,
            auditId: 1,
            auditControlId: 55,
            applicationName: 'App One',
            title: 'Gap found',
            controlId: 'AC-1',
            controlName: 'Access',
            severity: 'HIGH',
            status: 'OPEN',
            ownerUserId: 10,
            ownerEmail: 'owner@test.com',
            dueAt: null
          }]
        })
      }
      if (url === '/api/audits/1/controls') {
        return Promise.resolve({ data: [{ id: 55, controlControlId: 'AC-1', controlName: 'Access' }] })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  it('loads findings and creates a new finding', async () => {
    const wrapper = mount(Findings, {
      global: {
        stubs: {
          BsModal: {
            props: ['modelValue', 'title'],
            emits: ['update:modelValue'],
            template: '<div><slot /><slot name="footer" /></div>'
          }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Findings & Remediation')
    expect(wrapper.text()).toContain('Gap found')
    expect(api.get).toHaveBeenCalledWith('/api/findings', { params: {} })

    const addBtn = wrapper.findAll('button').find((b) => b.text() === 'Add finding')
    await addBtn.trigger('click')
    await flushPromises()

    const titleInput = wrapper.get('#finding-form input[maxlength="500"]')
    await titleInput.setValue('New finding from test')
    const dueInput = wrapper.get('#finding-form input[type="datetime-local"]')
    await dueInput.setValue('2026-12-31T12:00')

    const auditSelect = wrapper.get('#finding-form select[required]')
    await auditSelect.setValue('1')
    await flushPromises()
    const formSelects = wrapper.findAll('#finding-form select')
    await formSelects[1].setValue('55')
    await formSelects[3].setValue('10')

    await wrapper.get('#finding-form').trigger('submit')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/findings', expect.objectContaining({
      auditId: 1,
      auditControlId: 55,
      title: 'New finding from test'
    }))
  })
})
