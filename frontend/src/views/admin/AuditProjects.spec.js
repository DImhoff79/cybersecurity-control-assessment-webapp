import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import AuditProjects from './AuditProjects.vue'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('AuditProjects', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.user = { id: 1, role: 'AUDIT_MANAGER', permissions: ['AUDIT_MANAGEMENT', 'REPORT_VIEW'] }
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/applications') {
        const apps = Array.from({ length: 12 }).map((_, i) => ({
          id: i + 1,
          name: `App ${i + 1}`,
          regulatoryScope: i % 2 === 0 ? 'PCI' : 'SOX'
        }))
        return Promise.resolve({
          data: apps
        })
      }
      if (url === '/api/audit-projects') {
        return Promise.resolve({ data: [{ id: 50, name: 'PCI 2026', year: 2026, scopedApplications: [{ id: 1 }], totalAudits: 1, completeAudits: 0 }] })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: { id: 99 } })
    api.put.mockResolvedValue({ data: { id: 50 } })
  })

  it('supports clear add/remove selection between available and in-project lists', async () => {
    const wrapper = mount(AuditProjects)
    await flushPromises()

    expect(wrapper.text()).toContain('Existing projects')
    expect(wrapper.text()).toContain('PCI 2026')
    expect(wrapper.text()).toContain('Live Audits')
    expect(wrapper.text()).toContain('Stage Snapshot')
    expect(wrapper.text()).toContain('Available applications')
    expect(wrapper.text()).toContain('In this project')

    const availableChecks = wrapper.findAll('input[type="checkbox"]')
    await availableChecks[0].setValue(true)
    const addCheckedBtn = wrapper.findAll('button').find((b) => b.text().includes('Add checked'))
    await addCheckedBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('1 selected')

    const removeBtn = wrapper.findAll('button').find((b) => b.text() === 'Remove')
    await removeBtn.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('No apps in this project yet.')

    const addAllVisibleBtn = wrapper.findAll('button').find((b) => b.text().includes('Add all visible'))
    await addAllVisibleBtn.trigger('click')
    await flushPromises()

    await wrapper.find('input[type="number"]').setValue(2027)
    await wrapper.find('input[required]').setValue('PCI 2027')
    await wrapper.find('input[placeholder="PCI, SOX, SOC2..."]').setValue('PCI')
    await wrapper.find('input[type="date"]').setValue('2027-12-31')

    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/audit-projects', expect.objectContaining({
      name: 'PCI 2027',
      frameworkTag: 'PCI',
      year: 2027,
      dueAt: '2027-12-31T23:59:59Z'
    }))
    const payload = api.post.mock.calls[0][1]
    expect(payload.applicationIds).toHaveLength(12)
    expect(payload.applicationIds).toEqual(expect.arrayContaining([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]))
  })

  it('loads scoped apps into edit form and sends scope on save', async () => {
    const wrapper = mount(AuditProjects)
    await flushPromises()

    const editBtn = wrapper.findAll('button').find((b) => b.text() === 'Edit')
    await editBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Edit project')
    expect(wrapper.text()).toContain('1 selected')

    const forms = wrapper.findAll('form')
    await forms[1].trigger('submit.prevent')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/api/audit-projects/50', expect.objectContaining({
      applicationIds: [1]
    }))
  })

  it('deletes project and reloads data', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    api.delete.mockResolvedValue({ data: {} })
    const wrapper = mount(AuditProjects)
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(api.delete).toHaveBeenCalledWith('/api/audit-projects/50')
    expect(api.get).toHaveBeenCalledWith('/api/audit-projects')
  })
})
