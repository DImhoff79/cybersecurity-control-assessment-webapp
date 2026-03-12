import { onMounted, ref, watch } from 'vue'
import api from '../services/api'

export function useNotificationsMenu(authStore) {
  const notifications = ref([])
  const unreadCount = ref(0)

  async function loadNotifications() {
    if (!authStore.hasCredentials) return
    try {
      const [itemsRes, unreadRes] = await Promise.all([
        api.get('/api/notifications'),
        api.get('/api/notifications/unread-count')
      ])
      notifications.value = itemsRes.data || []
      unreadCount.value = unreadRes.data?.unread || 0
    } catch {
      notifications.value = []
      unreadCount.value = 0
    }
  }

  async function markRead(notificationId) {
    try {
      await api.put(`/api/notifications/${notificationId}/read`)
      await loadNotifications()
    } catch {
      // Keep menu resilient when backend is unavailable.
    }
  }

  async function markAllRead() {
    try {
      await api.put('/api/notifications/read-all')
      await loadNotifications()
    } catch {
      // Keep menu resilient when backend is unavailable.
    }
  }

  onMounted(async () => {
    await authStore.fetchUser()
    await loadNotifications()
  })

  watch(
    () => authStore.user?.id,
    async (userId) => {
      if (userId) {
        await loadNotifications()
      } else {
        notifications.value = []
        unreadCount.value = 0
      }
    }
  )

  return {
    notifications,
    unreadCount,
    loadNotifications,
    markRead,
    markAllRead
  }
}
