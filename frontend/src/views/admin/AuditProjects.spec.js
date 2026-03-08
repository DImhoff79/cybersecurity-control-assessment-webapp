import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditProjects from './AuditProjects.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('AuditProjects', () => {
  beforeEach(() => {
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
  })

  it('supports paging and selecting all filtered results', async () => {
    const wrapper = mount(AuditProjects)
    await flushPromises()

    expect(wrapper.text()).toContain('Existing projects')
    expect(wrapper.text()).toContain('PCI 2026')

    expect(wrapper.text()).toContain('Page 1 of 2')
    const selectAllResultsBtn = wrapper.findAll('button').find((b) => b.text().includes('Select all results'))
    await selectAllResultsBtn.trigger('click')
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
})
