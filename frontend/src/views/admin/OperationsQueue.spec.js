import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import OperationsQueue from './OperationsQueue.vue'
import { useAuthStore } from '../../stores/auth'

describe('OperationsQueue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders triage hub and embeds AuditorWorkbench', async () => {
    const authStore = useAuthStore()
    authStore.user = { id: 1, permissions: [] }

    const wrapper = mount(OperationsQueue, {
      global: {
        stubs: {
          AuditorWorkbench: { template: '<div class="stub-workbench">WorkbenchStub</div>' },
          ReviewQueue: { template: '<div class="stub-reviews">ReviewStub</div>' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Operations Queue')
    expect(wrapper.text()).toContain('Triage Hub')
    expect(wrapper.find('.stub-workbench').exists()).toBe(true)
  })

  it('shows Submitted Reviews tab when user has AUDIT_MANAGEMENT', async () => {
    const authStore = useAuthStore()
    authStore.user = { id: 1, permissions: ['AUDIT_MANAGEMENT'] }

    const wrapper = mount(OperationsQueue, {
      global: {
        stubs: {
          AuditorWorkbench: { template: '<div class="stub-workbench" />' },
          ReviewQueue: { template: '<div class="stub-reviews" />' }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Submitted Reviews')
    const reviewsBtn = wrapper.findAll('button').find((b) => b.text().includes('Submitted Reviews'))
    expect(reviewsBtn).toBeDefined()
    await reviewsBtn.trigger('click')
    expect(wrapper.find('.stub-reviews').exists()).toBe(true)
  })
})
