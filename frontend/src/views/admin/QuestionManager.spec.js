import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import QuestionManager from './QuestionManager.vue'
import api from '../../services/api'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() })
}))

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

const ModalStub = defineComponent({
  props: { modelValue: Boolean },
  emits: ['update:modelValue'],
  template: '<div><slot /><slot name="footer" /></div>'
})

function controlsResponse() {
  return [
    {
      id: 1,
      controlId: 'AC-1',
      name: 'Access Control Policy',
      questions: [
        {
          id: 11,
          controlId: 1,
          questionText: 'Do you review access annually?',
          helpText: 'Review access rights for active users.',
          askOwner: true,
          mappingRationale: 'Initial',
          mappingWeight: 10,
          effectiveFrom: null,
          effectiveTo: null
        }
      ]
    },
    {
      id: 2,
      controlId: 'LG-1',
      name: 'Logging',
      questions: []
    }
  ]
}

describe('QuestionManager', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))
    api.get.mockResolvedValue({ data: controlsResponse() })
    api.put.mockResolvedValue({ data: {} })
    api.post.mockResolvedValue({ data: {} })
    api.delete.mockResolvedValue({ data: {} })
  })

  it('shows last updated toast after toggling owner visibility', async () => {
    const wrapper = mount(QuestionManager, {
      global: {
        stubs: {
          BsModal: ModalStub,
          RouterLink: true
        }
      }
    })

    await flushPromises()

    const toggle = wrapper.find('tbody input.form-check-input')
    expect(toggle.exists()).toBe(true)

    await toggle.setValue(false)
    await flushPromises()

    expect(api.put).toHaveBeenCalledTimes(1)
    expect(api.put.mock.calls[0][0]).toContain('/api/controls/1/questions/11')
    expect(api.put.mock.calls[0][1]).toEqual({ askOwner: false })
    expect(wrapper.text()).toContain('Last updated')
    expect(wrapper.text()).toContain('Question visibility changed to hidden for owners.')
  })

  it('saves question details and mapping lifecycle actions', async () => {
    const wrapper = mount(QuestionManager, {
      global: {
        stubs: {
          BsModal: ModalStub,
          RouterLink: true
        }
      }
    })
    await flushPromises()

    const editBtn = wrapper.findAll('button').find((b) => b.text() === 'Edit')
    await editBtn.trigger('click')
    await flushPromises()

    const questionTextArea = wrapper.find('#question-edit-form textarea')
    await questionTextArea.setValue('Updated question text')
    await wrapper.find('#question-edit-form').trigger('submit.prevent')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/api/controls/1/questions/11', expect.objectContaining({
      questionText: 'Updated question text'
    }))
    expect(api.put).toHaveBeenCalledWith('/api/controls/1/questions/11/mapping', expect.any(Object))

    const refreshedEditBtn = wrapper.findAll('button').find((b) => b.text() === 'Edit')
    await refreshedEditBtn.trigger('click')
    await flushPromises()

    const mappingSelect = wrapper.find('#question-edit-form select.form-select')
    await mappingSelect.setValue('2')
    const addMappingBtn = wrapper.findAll('button').find((b) => b.text() === 'Add mapping')
    await addMappingBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/controls/2/questions', expect.objectContaining({
      questionText: 'Do you review access annually?'
    }))

    const removeMappingBtn = wrapper.findAll('button').find((b) => b.text() === 'Remove')
    await removeMappingBtn.trigger('click')
    await flushPromises()
    expect(api.delete).toHaveBeenCalledWith('/api/controls/1/questions/11')
  })
})
