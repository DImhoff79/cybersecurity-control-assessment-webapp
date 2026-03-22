import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Applications from './Applications.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('../../services/toast', () => ({
  toastError: vi.fn(),
  toastSuccess: vi.fn()
}))

describe('Applications', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/applications') {
        return Promise.resolve({
          data: [
            {
              id: 1,
              name: 'Alpha',
              description: 'Desc',
              ownerDisplayName: 'Owner One',
              criticality: 'HIGH',
              lifecycleStatus: 'ACTIVE'
            }
          ]
        })
      }
      if (url === '/api/users') {
        return Promise.resolve({
          data: [{ id: 10, email: 'u@example.com', displayName: 'User Ten' }]
        })
      }
      return Promise.resolve({ data: [] })
    })
  })

  it('loads applications and users then renders the table', async () => {
    const wrapper = mount(Applications, {
      global: {
        stubs: {
          BsModal: {
            template: '<div class="bs-modal-stub"><slot /></div>'
          }
        }
      }
    })
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/api/applications')
    expect(api.get).toHaveBeenCalledWith('/api/users')
    expect(wrapper.text()).toContain('Applications')
    expect(wrapper.text()).toContain('Alpha')
    expect(wrapper.text()).toContain('Owner One')
  })

  it('opens add modal and creates an application', async () => {
    api.post.mockResolvedValue({ data: { id: 2 } })

    const wrapper = mount(Applications, {
      global: {
        stubs: {
          BsModal: {
            props: ['modelValue'],
            template:
              '<div class="bs-modal-stub" v-show="modelValue"><slot /><slot name="footer" /></div>'
          }
        }
      }
    })
    await flushPromises()

    await wrapper.get('.btn-primary').trigger('click')
    const nameInput = wrapper.find('input[required]')
    await nameInput.setValue('New App')

    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith(
      '/api/applications',
      expect.objectContaining({ name: 'New App' })
    )
    expect(toastSuccess).toHaveBeenCalled()
  })

  it('deletes an application when confirmed', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    api.delete.mockResolvedValue({})

    const wrapper = mount(Applications, {
      global: {
        stubs: {
          BsModal: true
        }
      }
    })
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    expect(deleteBtn).toBeDefined()
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(api.delete).toHaveBeenCalledWith('/api/applications/1')
    expect(toastSuccess).toHaveBeenCalled()
    confirmSpy.mockRestore()
  })
})
