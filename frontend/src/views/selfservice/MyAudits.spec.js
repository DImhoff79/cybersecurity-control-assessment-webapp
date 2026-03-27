import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyAudits from './MyAudits.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('MyAudits', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/my-audits') {
        return Promise.resolve({
          data: [
            { id: 1, applicationName: 'App One', year: 2026, status: 'IN_PROGRESS', completionPct: 100 },
            { id: 2, applicationName: 'App Two', year: 2024, status: 'DRAFT', completionPct: 10 }
          ]
        })
      }
      return Promise.resolve({ data: [] })
    })
  })

  it('calculates completion by distinct question prompts', async () => {
    const wrapper = mount(MyAudits, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/api/my-audits')
    expect(wrapper.text()).toContain('100%')
    expect(wrapper.text()).toContain('Resume audit')
    expect(wrapper.text()).toContain('Fieldwork')
  })

  it('links application name and action to the respond route (including read-only submissions)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/my-audits') {
        return Promise.resolve({
          data: [
            { id: 7, applicationName: 'Locked App', year: 2025, status: 'PENDING_APPROVAL', completionPct: 100 }
          ]
        })
      }
      return Promise.resolve({ data: [] })
    })
    const wrapper = mount(MyAudits, {
      global: {
        stubs: {
          RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] }
        }
      }
    })
    await flushPromises()

    const links = wrapper.findAll('a[href="/audits/7/respond"]')
    expect(links.length).toBeGreaterThanOrEqual(2)
    expect(wrapper.text()).toContain('View submission')
  })

  it('sorts rows when year header is clicked', async () => {
    const wrapper = mount(MyAudits, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    const yearHeaderBtn = wrapper.findAll('button').find((b) => b.text().includes('Year'))
    await yearHeaderBtn.trigger('click')
    await flushPromises()

    const rows = wrapper.findAll('tbody tr')
    expect(rows[0].text()).toContain('2024')
    expect(rows[1].text()).toContain('2026')

    await yearHeaderBtn.trigger('click')
    await flushPromises()
    const rowsDesc = wrapper.findAll('tbody tr')
    expect(rowsDesc[0].text()).toContain('2026')
    expect(rowsDesc[1].text()).toContain('2024')
  })

  it('hides completed audits by default and shows them when filter is All', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/my-audits') {
        return Promise.resolve({
          data: [
            { id: 1, applicationName: 'Active App', year: 2026, status: 'IN_PROGRESS', completionPct: 50 },
            { id: 2, applicationName: 'Done App', year: 2025, status: 'COMPLETE', completionPct: 100 }
          ]
        })
      }
      return Promise.resolve({ data: [] })
    })
    const wrapper = mount(MyAudits, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.text()).toContain('Active App')
    expect(wrapper.text()).not.toContain('Done App')

    await wrapper.find('#my-audits-filter').setValue('all')
    await flushPromises()

    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('Done App')
  })
})
