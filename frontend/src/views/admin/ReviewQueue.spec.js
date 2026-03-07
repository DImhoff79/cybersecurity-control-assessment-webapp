import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReviewQueue from './ReviewQueue.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn()
  }
}))

describe('ReviewQueue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({
      data: [
        { id: 1, applicationName: 'App A', year: 2026, status: 'SUBMITTED' },
        { id: 2, applicationName: 'App B', year: 2026, status: 'IN_PROGRESS' }
      ]
    })
    api.put.mockResolvedValue({ data: {} })
  })

  it('shows submitted audits only and marks reviewed', async () => {
    const wrapper = mount(ReviewQueue, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('App A')
    expect(wrapper.text()).not.toContain('App B')

    const button = wrapper.findAll('button').find((b) => b.text() === 'Mark reviewed')
    await button.trigger('click')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/api/audits/1', { status: 'COMPLETE' })
  })
})
