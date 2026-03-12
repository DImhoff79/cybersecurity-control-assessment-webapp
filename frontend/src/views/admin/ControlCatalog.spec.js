import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ControlCatalog from './ControlCatalog.vue'
import api from '../../services/api'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { from: 'governance' } })
}))

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

const ModalStub = defineComponent({
  props: {
    modelValue: Boolean
  },
  emits: ['update:modelValue'],
  template: '<div><slot /><slot name="footer" /></div>'
})

describe('ControlCatalog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))
    vi.stubGlobal('prompt', vi.fn(() => 'Updated question text'))

    api.get.mockImplementation((url) => {
      if (url === '/api/controls?includeQuestions=false') {
        return Promise.resolve({
          data: [
            { id: 1, controlId: 'AC-1', name: 'Access Control', framework: 'PCI_DSS_V4', enabled: true },
            { id: 2, controlId: 'LG-1', name: 'Logging', framework: 'SOX', enabled: false }
          ]
        })
      }
      if (url === '/api/controls/1/questions') {
        return Promise.resolve({
          data: [{ id: 11, questionText: 'Do you review access?' }]
        })
      }
      if (url === '/api/controls/1?includeQuestions=true') {
        return Promise.resolve({
          data: {
            id: 1,
            controlId: 'AC-1',
            name: 'Access Control',
            framework: 'PCI_DSS_V4',
            category: 'Identity',
            description: 'Control description',
            questions: [{ id: 11, questionText: 'Do you review access?', helpText: 'Check quarterly.' }]
          }
        })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
    api.patch.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
    api.delete.mockResolvedValue({ data: {} })
  })

  it('supports create, toggle, question management, and delete flows', async () => {
    const wrapper = mount(ControlCatalog, {
      global: {
        stubs: {
          BsModal: ModalStub,
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    await wrapper.find('button.btn.btn-primary').trigger('click')
    const createInputs = wrapper.findAll('#create-control-form input')
    await createInputs[0].setValue('CM-1')
    await createInputs[1].setValue('Configuration Management')
    await wrapper.find('#create-control-form select').setValue('HIPAA')
    await wrapper.find('#create-control-form').trigger('submit.prevent')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/controls', expect.objectContaining({
      controlId: 'CM-1',
      name: 'Configuration Management',
      framework: 'HIPAA'
    }))

    const enabledCheckbox = wrapper.find('tbody tr input.form-check-input')
    await enabledCheckbox.trigger('change')
    await flushPromises()
    expect(api.patch).toHaveBeenCalledWith('/api/controls/1', { enabled: false })

    const questionsBtn = wrapper.findAll('button').find((b) => b.text() === 'Questions')
    await questionsBtn.trigger('click')
    await flushPromises()

    await wrapper.find('input[placeholder="Plain English question"]').setValue('Is MFA enabled?')
    await wrapper.find('form.border-top').trigger('submit.prevent')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/controls/1/questions', { questionText: 'Is MFA enabled?' })

  })

  it('supports edit/save, details view, and control delete actions', async () => {
    const wrapper = mount(ControlCatalog, {
      global: {
        stubs: {
          BsModal: ModalStub,
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    const editBtn = wrapper.findAll('button').find((b) => b.text() === 'Edit')
    await editBtn.trigger('click')
    await flushPromises()
    await wrapper.find('#edit-control-form input').setValue('Access Control Updated')
    await wrapper.find('#edit-control-form').trigger('submit.prevent')
    await flushPromises()
    expect(api.patch).toHaveBeenCalledWith('/api/controls/1', expect.objectContaining({
      name: 'Access Control Updated'
    }))

    const detailsBtn = wrapper.findAll('button').find((b) => b.text().includes('Access Control'))
    await detailsBtn.trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/api/controls/1?includeQuestions=true')

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    await deleteBtn.trigger('click')
    await flushPromises()
    expect(api.delete).toHaveBeenCalledWith('/api/controls/1')
  })
})
