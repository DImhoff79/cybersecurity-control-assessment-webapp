import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ComplianceObligations from './ComplianceObligations.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('ComplianceObligations', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/api/compliance/regulations') {
        return Promise.resolve({ data: [{ id: 1, code: 'PCI', name: 'PCI DSS' }] })
      }
      if (url === '/api/compliance/requirements') {
        return Promise.resolve({ data: [{ id: 11, regulationId: 1, regulationCode: 'PCI', requirementCode: 'PCI-7.1', title: 'Access' }] })
      }
      if (url === '/api/controls') {
        return Promise.resolve({ data: [{ id: 101, controlId: 'AC-1', name: 'Access Control' }] })
      }
      if (url === '/api/policies') {
        return Promise.resolve({ data: [{ id: 201, code: 'POL-1', name: 'Policy 1' }] })
      }
      if (url === '/api/compliance/requirement-control-mappings') {
        return Promise.resolve({ data: [] })
      }
      if (url === '/api/compliance/policy-requirement-mappings') {
        return Promise.resolve({ data: [] })
      }
      return Promise.resolve({ data: [] })
    })
    api.post.mockResolvedValue({ data: {} })
  })

  it('loads data and can create regulation and mappings', async () => {
    const wrapper = mount(ComplianceObligations)
    await flushPromises()

    expect(wrapper.text()).toContain('Compliance Obligations')
    expect(api.get).toHaveBeenCalledWith('/api/compliance/regulations')

    await wrapper.find('form button.btn-outline-primary').trigger('submit')
    await flushPromises()

    const mapButtons = wrapper.findAll('button').filter((b) => b.text() === 'Map')
    await mapButtons[0].trigger('click')
    await mapButtons[1].trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalled()
  })
})
