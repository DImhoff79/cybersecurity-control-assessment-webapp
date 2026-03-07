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
      return !!cred
    }
  },
  actions: {
    setCredentials(email, password) {
      const encoded = btoa(`${email}:${password}`)
      localStorage.setItem('auth_credentials', encoded)
      return this.fetchUser()
    },
    clearCredentials() {
      localStorage.removeItem('auth_credentials')
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
