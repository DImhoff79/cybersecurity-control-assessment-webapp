import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyTasks from './MyTasks.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn()
  }
}))

describe('MyTasks', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({
      data: [
        {
          assignmentId: 1,
          auditId: 11,
          auditControlId: 100,
          applicationName: 'Delegated App',
          auditYear: 2026,
          controlControlId: 'AC-1',
          controlName: 'Access Control',
          status: 'IN_PROGRESS',
          notes: 'Initial notes',
          assignmentRole: 'CONTRIBUTOR'
        }
      ]
    })
    api.put.mockResolvedValue({ data: {} })
  })

  it('loads delegated tasks and updates status', async () => {
    const wrapper = mount(MyTasks, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Delegated App')
    const select = wrapper.find('tbody select')
    await select.setValue('PASS')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/api/audit-controls/100', { status: 'PASS' })
  })
})
