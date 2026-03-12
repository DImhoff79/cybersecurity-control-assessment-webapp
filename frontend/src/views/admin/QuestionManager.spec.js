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
    put: vi.fn()
  }
}))

const controlsResponse = [
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
        askOwner: true
      }
    ]
  }
]

describe('QuestionManager', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({ data: controlsResponse })
    api.put.mockResolvedValue({ data: {} })
  })

  it('shows last updated toast after toggling owner visibility', async () => {
    const wrapper = mount(QuestionManager, {
      global: {
        stubs: {
          BsModal: true,
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
})
