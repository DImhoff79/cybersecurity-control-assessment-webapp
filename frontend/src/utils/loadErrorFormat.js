/**
 * Human-readable message for failed API loads (demo / admin screens).
 * @param {import('axios').AxiosError} err
 * @returns {string}
 */
export function formatLoadError(err) {
  const d = err.response?.data
  if (typeof d === 'string' && d.trim()) return d
  if (d && typeof d === 'object') {
    const parts = [d.message, d.detail, d.error].filter(Boolean)
    if (parts.length) return parts.join(' — ')
  }
  if (err.response?.status === 403) {
    return 'Forbidden (403). Your account is signed in but the API denied this action — ensure your user has the right permissions in the database, then log out and back in.'
  }
  if (err.response?.status === 404) return 'Demo not found. Run Flyway through V36.'
  if (err.code === 'ERR_NETWORK' || err.message === 'Network Error') {
    return 'Cannot reach API (port 8080). Use npm run dev or set VITE_API_ORIGIN=http://localhost:8080'
  }
  return err.message || 'Failed to load.'
}
