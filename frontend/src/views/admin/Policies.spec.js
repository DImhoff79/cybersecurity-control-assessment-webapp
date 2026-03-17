import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Policies from './Policies.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('Policies', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({
      data: [
        {
          id: 1,
          code: 'POL-1',
          name: 'Access Policy',
          status: 'DRAFT',
          versions: [{ id: 12, versionNumber: 1, status: 'DRAFT' }]
        }
      ]
    })
    api.post.mockResolvedValue({ data: {} })
  })

  it('creates policy and publishes latest draft version', async () => {
    const wrapper = mount(Policies)
    await flushPromises()
    expect(wrapper.text()).toContain('Access Policy')

    const createBtn = wrapper.find('form button.btn-primary')
    await createBtn.trigger('submit')
    await flushPromises()

    const publishBtn = wrapper.findAll('button').find((b) => b.text().includes('Publish Latest Draft'))
    await publishBtn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/policies/1/publish', { policyVersionId: 12 })
  })
})
