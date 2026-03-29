import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import Login from '../views/Login.vue'

function createLoginRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'Login', component: Login, meta: { public: true } },
      { path: '/', redirect: '/login' }
    ]
  })
}

describe('Login shell integration', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('mounts the login view with the expected heading and sign-in copy', async () => {
    const router = createLoginRouter()
    const pinia = createPinia()
    await router.push('/login')
    await router.isReady()

    const wrapper = mount(Login, {
      global: { plugins: [router, pinia] }
    })
    await flushPromises()

    expect(wrapper.text()).toMatch(/Cybersecurity Control Assessment/)
    expect(wrapper.text()).toMatch(/Sign in to continue/)
    expect(wrapper.find('input#email').exists()).toBe(true)
    expect(wrapper.find('input#password').exists()).toBe(true)
  })
})
