import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.DEV ? '' : '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const cred = localStorage.getItem('auth_credentials')
  if (cred) {
    config.headers.Authorization = `Basic ${cred}`
  }
  return config
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('auth_credentials')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
      }
    }
    return Promise.reject(err)
  }
)

export default api
