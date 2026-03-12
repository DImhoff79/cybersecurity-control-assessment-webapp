import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import QuestionnaireTemplates from './QuestionnaireTemplates.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

const ModalStub = defineComponent({
  props: { modelValue: Boolean },
  emits: ['update:modelValue'],
  template: '<div><slot /><slot name="footer" /></div>'
})

describe('QuestionnaireTemplates', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/questionnaire-templates') {
        return Promise.resolve({
          data: [{ id: 1, versionNo: 3, status: 'DRAFT', itemCount: 12, createdAt: '2026-03-08T10:00:00Z' }]
        })
      }
      if (url === '/api/questionnaire-templates/1/items') {
        return Promise.resolve({ data: [{ id: 7, controlControlId: 'AC-1', questionText: 'Q?', mappingWeight: 80 }] })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.delete.mockResolvedValue({ data: {} })
  })

  it('creates working snapshot and publishes template', async () => {
    const wrapper = mount(QuestionnaireTemplates, {
      global: { stubs: { BsModal: ModalStub, RouterLink: true } }
    })
    await flushPromises()

    const createBtn = wrapper.findAll('button').find((b) => b.text().includes('Create Working Snapshot'))
    await createBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/questionnaire-templates/draft-from-current', { notes: null })

    const publishBtn = wrapper.findAll('button').find((b) => b.text() === 'Publish')
    await publishBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/questionnaire-templates/1/publish')
  })

  it('deletes a working snapshot', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mount(QuestionnaireTemplates, {
      global: { stubs: { BsModal: ModalStub, RouterLink: true } }
    })
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(api.delete).toHaveBeenCalledWith('/api/questionnaire-templates/1')
  })

  it('supports initial bootstrap and viewing template items', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/questionnaire-templates') {
        return Promise.resolve({ data: [] })
      }
      if (url === '/api/controls?includeQuestions=true') {
        return Promise.resolve({ data: [{ id: 1, enabled: true, questions: [{ id: 11 }] }] })
      }
      if (url === '/api/questionnaire-templates/1/items') {
        return Promise.resolve({ data: [{ id: 5, controlControlId: 'AC-1', questionText: 'Q?', mappingWeight: 80 }] })
      }
      return Promise.resolve({ data: [] })
    })

    const wrapper = mount(QuestionnaireTemplates, {
      global: { stubs: { BsModal: ModalStub, RouterLink: true } }
    })
    await flushPromises()

    const bootstrapBtn = wrapper.findAll('button').find((b) => b.text().includes('Create Initial Baseline'))
    await bootstrapBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/questionnaire-templates/bootstrap-initial', { notes: null })

    // Simulate loaded template to trigger view items flow.
    api.get.mockImplementation((url) => {
      if (url === '/api/questionnaire-templates') {
        return Promise.resolve({
          data: [{ id: 1, versionNo: 4, status: 'PUBLISHED', itemCount: 1, createdAt: '2026-03-08T10:00:00Z', publishedAt: '2026-03-08T12:00:00Z' }]
        })
      }
      if (url === '/api/controls?includeQuestions=true') {
        return Promise.resolve({ data: [{ id: 1, enabled: true, questions: [{ id: 11 }] }] })
      }
      if (url === '/api/questionnaire-templates/1/items') {
        return Promise.resolve({ data: [{ id: 5, controlControlId: 'AC-1', questionText: 'Q?', mappingWeight: 80 }] })
      }
      return Promise.resolve({ data: [] })
    })

    const wrapperWithTemplate = mount(QuestionnaireTemplates, {
      global: { stubs: { BsModal: ModalStub, RouterLink: true } }
    })
    await flushPromises()

    const viewBtn = wrapperWithTemplate.findAll('button').find((b) => b.text() === 'View Items')
    await viewBtn.trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/api/questionnaire-templates/1/items')
    expect(wrapperWithTemplate.text()).toContain('AC-1')
  })
})
