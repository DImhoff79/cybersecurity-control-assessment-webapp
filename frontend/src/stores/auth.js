import { defineStore } from 'pinia'
import api from '../services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    loading: false,
    _fetchUserPromise: null
  }),
  getters: {
    isAdmin(state) {
      return state.user?.role === 'ADMIN'
    },
    userPermissions(state) {
      return new Set(state.user?.permissions || [])
    },
    canAccessAdmin(state) {
      const perms = new Set(state.user?.permissions || [])
      return perms.has('USER_MANAGEMENT') ||
        perms.has('APPLICATION_MANAGEMENT') ||
        perms.has('AUDIT_MANAGEMENT') ||
        perms.has('REPORT_VIEW')
    },
    hasPermission(state) {
      const perms = new Set(state.user?.permissions || [])
      return (permission) => perms.has(permission)
    },
    hasRole(state) {
      return (role) => state.user?.role === role
    },
    hasCredentials() {
      const cred = localStorage.getItem('auth_credentials')
      const mode = localStorage.getItem('auth_mode')
      return !!cred || mode === 'oauth'
    }
  },
  actions: {
    async setCredentials(email, password) {
      const encoded = btoa(`${email}:${password}`)
      localStorage.setItem('auth_credentials', encoded)
      localStorage.setItem('auth_mode', 'basic')
      const user = await this.fetchUser()
      if (!user) {
        this.clearCredentials()
      }
      return user
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
      if (this._fetchUserPromise) {
        return this._fetchUserPromise
      }
      if (!this.hasCredentials) return Promise.resolve()
      this._fetchUserPromise = (async () => {
        this.loading = true
        try {
          const res = await api.get('/api/auth/me')
          this.user = res.data?.id != null ? res.data : null
          return this.user
        } catch (err) {
          if (err?.response?.status === 401) {
            this.user = null
            return null
          }
          throw err
        } finally {
          this.loading = false
          this._fetchUserPromise = null
        }
      })()
      return this._fetchUserPromise
    }
  }
})
