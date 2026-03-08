import { defineStore } from 'pinia'
import api from '../services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    loading: false
  }),
  getters: {
    isAdmin(state) {
      return state.user?.role === 'ADMIN'
    },
    hasCredentials() {
      const cred = localStorage.getItem('auth_credentials')
      const mode = localStorage.getItem('auth_mode')
      return !!cred || mode === 'oauth'
    }
  },
  actions: {
    setCredentials(email, password) {
      const encoded = btoa(`${email}:${password}`)
      localStorage.setItem('auth_credentials', encoded)
      localStorage.setItem('auth_mode', 'basic')
      return this.fetchUser()
    },
    setOAuthSession() {
      localStorage.removeItem('auth_credentials')
      localStorage.setItem('auth_mode', 'oauth')
      return this.fetchUser()
    },
    clearCredentials() {
      localStorage.removeItem('auth_credentials')
      localStorage.removeItem('auth_mode')
      this.user = null
    },
    async fetchUser() {
      if (!this.hasCredentials) return Promise.resolve()
      this.loading = true
      try {
        const res = await api.get('/api/auth/me')
        this.user = res.data?.id != null ? res.data : null
        return this.user
      } catch {
        this.user = null
        return null
      } finally {
        this.loading = false
      }
    }
  }
})
