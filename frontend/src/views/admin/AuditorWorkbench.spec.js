import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditorWorkbench from './AuditorWorkbench.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('AuditorWorkbench', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({ data: { email: 'auditor@test.com' } })
      }
      return Promise.resolve({
        data: {
          summary: { totalAudits: 10, openAudits: 3, overdueAudits: 1, submittedAudits: 2 },
          auditsNeedingAttention: [
            {
              auditId: 1,
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
          ]
        }
      })
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
    localStorage.clear()
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

    const nameInput = wrapper.find('input[placeholder="Filter name"]')
    await nameInput.setValue('Unassigned queue')
    const saveBtn = wrapper.findAll('button').find((b) => b.text() === 'Save')
    await saveBtn.trigger('click')
    await flushPromises()
    expect(localStorage.getItem('auditor_workbench_saved_filters')).toContain('Unassigned queue')
  })
})
