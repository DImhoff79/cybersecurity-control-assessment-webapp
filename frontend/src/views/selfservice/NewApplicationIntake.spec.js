import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import NewApplicationIntake from './NewApplicationIntake.vue'
import api from '../../services/api'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: push, push })
}))

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

vi.mock('../../services/toast', () => ({
  toastSuccess: vi.fn(),
  toastError: vi.fn()
}))

describe('NewApplicationIntake', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    push.mockClear()
  })

  it('loads wizard from API and renders first step when steps exist', async () => {
    api.get.mockResolvedValue({
      data: [
        {
          questionId: 1,
          stepKey: 'app_name',
          inputType: 'TEXT',
          questionText: 'Application name',
          helpText: 'What do you call it?',
          choicesJson: null,
          displayOrder: 0
        }
      ]
    })
    const wrapper = mount(NewApplicationIntake)
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/api/application-intake/wizard')
    expect(wrapper.text()).toContain('Application name')
  })

  it('falls back to static flow when wizard returns empty', async () => {
    api.get.mockResolvedValue({ data: [] })
    const wrapper = mount(NewApplicationIntake)
    await flushPromises()
    expect(wrapper.text()).toContain('Application name')
    expect(wrapper.text()).toContain('Step 1 of 3')
    expect(wrapper.find('#app-name').exists()).toBe(true)
  })
})
