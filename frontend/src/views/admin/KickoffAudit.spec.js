import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import KickoffAudit from './KickoffAudit.vue'
import api from '../../services/api'
import { defineComponent } from 'vue'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
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

describe('KickoffAudit', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))
    vi.stubGlobal('prompt', vi.fn(() => 'Looks good'))

    api.get.mockImplementation((url) => {
      if (url === '/api/applications') {
        return Promise.resolve({ data: [{ id: 1, name: 'App' }] })
      }
      if (url === '/api/users') {
        return Promise.resolve({ data: [{ id: 2, email: 'owner@example.com', displayName: 'Owner' }] })
      }
      if (url === '/api/audit-projects') {
        return Promise.resolve({ data: [{ id: 8, name: 'PCI 2026' }] })
      }
      if (url === '/api/applications/1/audits') {
        return Promise.resolve({
          data: [{ id: 11, year: 2026, status: 'SUBMITTED', projectId: 8, projectName: 'PCI 2026', assignedToEmail: 'owner@example.com' }]
        })
      }
      return Promise.resolve({ data: [] })
    })

    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
    api.delete.mockResolvedValue({ data: {} })
  })

  it('deletes audit from admin history', async () => {
    const wrapper = mount(KickoffAudit, {
      global: {
        stubs: {
          BsModal: ModalStub,
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    expect(deleteBtn).toBeTruthy()
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(api.delete).toHaveBeenCalledWith('/api/audits/11')
  })

  it('supports assign, send, remind, attest, and bulk assign actions', async () => {
    const wrapper = mount(KickoffAudit, {
      global: {
        stubs: {
          BsModal: ModalStub,
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    const assignBtn = wrapper.findAll('button').find((b) => b.text() === 'Assign / Send')
    await assignBtn.trigger('click')
    await flushPromises()

    const modalSelect = wrapper.findAll('select').find((s) => s.html().includes('- Select -'))
    await modalSelect.setValue('2')

    const assignOnlyBtn = wrapper.findAll('button').find((b) => b.text() === 'Assign only')
    await assignOnlyBtn.trigger('click')
    await flushPromises()
    expect(api.put).toHaveBeenCalledWith('/api/audits/11/assign', { userId: 2 })

    await assignBtn.trigger('click')
    await flushPromises()
    const sendBtn = wrapper.findAll('button').find((b) => b.text() === 'Assign and send to owner')
    await sendBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/audits/11/send')

    const remindBtn = wrapper.findAll('button').find((b) => b.text() === 'Remind')
    await remindBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/audits/11/remind')

    const attestBtn = wrapper.findAll('button').find((b) => b.text() === 'Attest')
    await attestBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/audits/11/attest', { statement: 'Looks good' })

  })
})
