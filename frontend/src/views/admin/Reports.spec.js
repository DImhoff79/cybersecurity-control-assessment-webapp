import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Reports from './Reports.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('Reports', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/reports/summary') {
        return Promise.resolve({ data: { totalApplications: 3, totalAudits: 5 } })
      }
      if (url === '/api/reports/by-year') {
        return Promise.resolve({ data: [{ year: 2026, total: 5, draft: 1, inProgress: 2, submitted: 1, attested: 0, complete: 1 }] })
      }
      if (url === '/api/reports/trends') {
        return Promise.resolve({ data: [{ year: 2026, total: 5, open: 3, overdue: 1, complete: 1 }] })
      }
      if (url === '/api/reports/by-project') {
        return Promise.resolve({ data: [{ projectId: 10, projectName: 'PCI 2026', year: 2026, totalAudits: 5, scopedApplications: 3, openAudits: 2, submittedAudits: 1, attestedAudits: 1, completeAudits: 1 }] })
      }
      if (url === '/api/reports/audits.csv') {
        return Promise.resolve({ data: 'audit_id,application\n1,App' })
      }
      if (url === '/api/reports/board-pack.pdf') {
        return Promise.resolve({ data: 'pdf-bytes' })
      }
      return Promise.resolve({ data: {} })
    })
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:test'),
      revokeObjectURL: vi.fn()
    })
    const originalCreateElement = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tagName) => {
      if (tagName.toLowerCase() === 'a') {
        const anchor = originalCreateElement('a')
        anchor.click = vi.fn()
        return anchor
      }
      return originalCreateElement(tagName)
    })
  })

  it('loads summary and downloads csv/pdf exports', async () => {
    const wrapper = mount(Reports)
    await flushPromises()

    expect(wrapper.text()).toContain('Applications')
    expect(wrapper.text()).toContain('By Year')

    const exportBtn = wrapper.findAll('button').find((b) => b.text().includes('Export Audits CSV'))
    await exportBtn.trigger('click')
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/api/reports/audits.csv', { responseType: 'blob' })

    const pdfBtn = wrapper.findAll('button').find((b) => b.text().includes('Board Pack PDF'))
    await pdfBtn.trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/api/reports/board-pack.pdf', { responseType: 'blob' })
  })

  it('supports by-year and by-project sort toggles', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/reports/summary') {
        return Promise.resolve({ data: { totalApplications: 3, totalAudits: 5 } })
      }
      if (url === '/api/reports/by-year') {
        return Promise.resolve({
          data: [
            { year: 2026, total: 5, draft: 1, inProgress: 2, submitted: 1, attested: 0, complete: 1 },
            { year: 2024, total: 2, draft: 0, inProgress: 1, submitted: 0, attested: 0, complete: 1 }
          ]
        })
      }
      if (url === '/api/reports/trends') {
        return Promise.resolve({ data: [{ year: 2024, total: 2, open: 1, overdue: 0, complete: 1 }, { year: 2026, total: 5, open: 3, overdue: 1, complete: 1 }] })
      }
      if (url === '/api/reports/by-project') {
        return Promise.resolve({
          data: [
            { projectId: 11, projectName: 'SOX 2024', frameworkTag: 'SOX', year: 2024, totalAudits: 2, scopedApplications: 1, openAudits: 1, submittedAudits: 0, attestedAudits: 0, completeAudits: 1 },
            { projectId: 10, projectName: 'PCI 2026', frameworkTag: 'PCI', year: 2026, totalAudits: 5, scopedApplications: 3, openAudits: 2, submittedAudits: 1, attestedAudits: 1, completeAudits: 1 }
          ]
        })
      }
      return Promise.resolve({ data: {} })
    })

    const wrapper = mount(Reports)
    await flushPromises()

    const yearBtn = wrapper.findAll('button').find((b) => b.text().includes('Year'))
    await yearBtn.trigger('click')
    await flushPromises()

    const byProjectBtn = wrapper.findAll('button').find((b) => b.text().includes('Project'))
    await byProjectBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('SOX 2024')
    expect(wrapper.text()).toContain('PCI 2026')
  })
})
