import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditorWorkbench from './AuditorWorkbench.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('AuditorWorkbench', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.URL.createObjectURL = vi.fn(() => 'blob:mock')
    global.URL.revokeObjectURL = vi.fn()
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({ data: { email: 'auditor@test.com' } })
      }
      if (url === '/api/reports/saved-filters') {
        return Promise.resolve({ data: [{ id: 100, name: 'Shared Queue', shared: true, filterState: { auditFilter: { queue: 'unassigned' }, evidenceFilter: {} } }] })
      }
      return Promise.resolve({
        data: {
          summary: { totalAudits: 10, openAudits: 3, overdueAudits: 1, submittedAudits: 2 },
          auditsNeedingAttention: [
            {
              auditId: 1,
              projectId: 10,
              projectName: 'PCI 2026',
              applicationName: 'App A',
              year: 2026,
              status: 'SUBMITTED',
              assignedToEmail: 'auditor@test.com',
              dueAt: null,
              pendingEvidenceCount: 2,
              frameworks: 'NIST_800_53_LOW'
            },
            {
              auditId: 2,
              projectId: null,
              projectName: null,
              applicationName: 'App B',
              year: 2026,
              status: 'IN_PROGRESS',
              assignedToEmail: null,
              dueAt: null,
              pendingEvidenceCount: 0,
              frameworks: 'HIPAA'
            }
          ],
          evidenceQueue: [
            {
              evidenceId: 9,
              projectId: 10,
              projectName: 'PCI 2026',
              applicationName: 'App A',
              year: 2026,
              controlControlId: 'AC-1',
              controlName: 'Access',
              framework: 'NIST_800_53_LOW',
              title: 'Policy',
              evidenceType: 'DOCUMENT',
              createdAt: null,
              uri: '/api/evidences/9/download'
            }
          ],
          recentActivity: [
            {
              id: 700,
              auditId: 1,
              projectId: 10,
              projectName: 'PCI 2026',
              applicationName: 'App A',
              year: 2026,
              activityType: 'FINDING_CREATED',
              details: 'Finding created from assessment',
              actorEmail: 'auditor@test.com',
              createdAt: null
            }
          ]
        }
      })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
    api.delete.mockResolvedValue({ data: {} })
  })

  it('loads dashboard and can review evidence', async () => {
    const wrapper = mount(AuditorWorkbench, {
      global: {
        stubs: { RouterLink: { template: '<a><slot /></a>' } }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Audits Needing Attention')
    expect(wrapper.text()).toContain('Evidence Review Queue')

    const acceptBtn = wrapper.findAll('button').find((b) => b.text() === 'Accept')
    await acceptBtn.trigger('click')
    await flushPromises()
    expect(api.put).toHaveBeenCalledWith('/api/evidences/9/review', { reviewStatus: 'ACCEPTED' })

    const queueSelect = wrapper.findAll('select').at(0)
    await queueSelect.setValue('unassigned')
    await flushPromises()
    expect(wrapper.text()).toContain('App B')

    const projectFilter = wrapper.get('[data-testid="audit-project-filter"]')
    await projectFilter.setValue('10')
    await flushPromises()
    expect(wrapper.text()).toContain('Showing 0 of')

    const nameInput = wrapper.find('input[placeholder="Filter name"]')
    await nameInput.setValue('Unassigned queue')
    const saveBtn = wrapper.findAll('button').find((b) => b.text() === 'Save')
    await saveBtn.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/api/reports/saved-filters', expect.any(Object))

    expect(wrapper.text()).toContain('Recent Activity Feed')
    expect(wrapper.text()).toContain('Finding created from assessment')
  })

  it('exports recent activity csv with selected filters', async () => {
    const wrapper = mount(AuditorWorkbench, {
      global: {
        stubs: { RouterLink: { template: '<a><slot /></a>' } }
      }
    })
    await flushPromises()

    const activityProjectSelect = wrapper.findAll('select').find((s) =>
      s.element.options?.[0]?.text?.includes('All projects'))
    await activityProjectSelect.setValue('10')

    const activityCategorySelect = wrapper.findAll('select').find((s) =>
      s.element.options?.[0]?.text?.includes('All events'))
    await activityCategorySelect.setValue('finding')

    const activitySearch = wrapper.find('input[placeholder="Search details or actor..."]')
    await activitySearch.setValue('Finding')

    api.get.mockImplementationOnce((url, options) => {
      if (url === '/api/reports/recent-activity.csv') {
        return Promise.resolve({ data: new Blob(['a,b']) })
      }
      return Promise.resolve({ data: {} })
    })

    const exportBtn = wrapper.findAll('button').find((b) => b.text() === 'Export CSV')
    await exportBtn.trigger('click')
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/api/reports/recent-activity.csv', {
      params: {
        category: 'finding',
        search: 'Finding',
        projectId: 10
      },
      responseType: 'blob'
    })
  })
})
