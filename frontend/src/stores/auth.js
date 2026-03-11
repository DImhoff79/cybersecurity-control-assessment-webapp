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
    hasCredentials() {
      const cred = localStorage.getItem('auth_credentials')
      const mode = localStorage.getItem('auth_mode')
      return !!cred || mode === 'oauth'
    }
  },
  actions: {
    async setCredentials(email, password) {
      const safeEmail = (email || '').trim()
      const encoded = btoa(`${safeEmail}:${(password || '').trim()}`)
      this.loading = true
      try {
        const res = await api.get('/api/auth/me', {
          headers: {
            Authorization: `Basic ${encoded}`
          }
        })
        const user = res.data?.id != null ? res.data : null
        this.user = user
        if (user) {
          localStorage.setItem('auth_credentials', encoded)
          localStorage.setItem('auth_mode', 'basic')
          return user
        }
        this.clearCredentials()
        return null
      } catch (err) {
        if (err?.response?.status === 401) {
          this.clearCredentials()
          return null
        }
        throw err
      } finally {
        this.loading = false
      }
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
