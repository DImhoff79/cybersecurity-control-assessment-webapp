import { createPinia, setActivePinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import router from '../router'
import App from '../App.vue'

/**
 * Integration-style checks: real App + router + Pinia. Tours are not started because there is no user session.
 */
describe('App router integration', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 503
        })
      )
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('navigates to the login route and renders the login shell', async () => {
    const wrapper = mount(App, {
      global: {
        plugins: [router],
        stubs: {
          AppToasts: { template: '<div />' }
        }
      }
    })

    await router.push('/login')
    await router.isReady()
    await flushPromises()
    await nextTick()

    expect(router.currentRoute.value.path).toBe('/login')
    expect(wrapper.text()).toMatch(/Cybersecurity Control Assessment/)
  })
})
