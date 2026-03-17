import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyPolicies from './MyPolicies.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('MyPolicies', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({
      data: [
        {
          id: 11,
          policyCode: 'POL-1',
          policyName: 'Access',
          policyVersionNumber: 2,
          policyVersionTitle: 'v2',
          status: 'PENDING',
          dueAt: null,
          acknowledgedAt: null
        }
      ]
    })
    api.post.mockResolvedValue({ data: {} })
  })

  it('loads acknowledgements and submits acknowledge action', async () => {
    const wrapper = mount(MyPolicies)
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/api/policies/my-acknowledgements')
    expect(wrapper.text()).toContain('POL-1')

    const btn = wrapper.find('button.btn-primary')
    await btn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/policies/acknowledgements/11/acknowledge')
  })
})
