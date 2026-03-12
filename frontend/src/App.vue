<template>
  <div class="min-vh-100 d-flex flex-column bg-light">
    <header v-if="authStore.user && $route.name !== 'Login'" class="brand-header text-white border-bottom border-primary-subtle">
      <div class="container py-1 d-flex justify-content-end">
        <span class="small opacity-75">{{ authStore.user?.displayName || authStore.user?.email }}</span>
      </div>
      <nav class="navbar navbar-expand-lg navbar-dark pt-0 pb-2 px-3">
        <router-link to="/" class="navbar-brand fw-semibold">Cybersecurity Assessment</router-link>
        <div class="navbar-nav ms-auto d-flex flex-row align-items-center gap-2">
          <router-link to="/my-audits" class="nav-link text-white px-2">My Audits</router-link>
          <router-link to="/my-tasks" class="nav-link text-white px-2">My Tasks</router-link>
          <div class="nav-item dropdown-hover">
            <span class="nav-link text-white px-2 dropdown-toggle">
              Notifications
              <span v-if="unreadCount > 0" class="badge text-bg-danger ms-1">{{ unreadCount }}</span>
            </span>
            <div class="dropdown-menu dropdown-menu-end border-0 shadow-sm notification-menu">
              <div class="d-flex justify-content-between align-items-center px-3 py-2 border-bottom">
                <strong class="small">Recent</strong>
                <button class="btn btn-link btn-sm p-0 text-decoration-none" @click="markAllRead">Mark all read</button>
              </div>
              <div v-if="!notifications.length" class="px-3 py-2 small text-muted">No notifications.</div>
              <button
                v-for="note in notifications"
                :key="note.id"
                type="button"
                class="dropdown-item small"
                @click="markRead(note.id)"
              >
                <div class="fw-semibold">{{ note.title }}</div>
                <div class="text-muted">{{ note.message }}</div>
              </button>
            </div>
          </div>

          <div v-if="authStore.canAccessAdmin" class="nav-item dropdown-hover">
            <span class="nav-link text-white px-2 dropdown-toggle">Admin</span>
            <div class="dropdown-menu dropdown-menu-end border-0 shadow-sm">
              <router-link v-if="authStore.hasPermission('APPLICATION_MANAGEMENT')" to="/admin/applications" class="dropdown-item">Applications</router-link>
              <router-link v-if="authStore.hasPermission('AUDIT_MANAGEMENT')" to="/admin/audits" class="dropdown-item">Audits</router-link>
              <router-link v-if="authStore.hasPermission('AUDIT_MANAGEMENT')" to="/admin/audit-projects" class="dropdown-item">Audit Projects</router-link>
              <router-link v-if="authStore.hasPermission('AUDIT_MANAGEMENT')" to="/admin/review-queue" class="dropdown-item">Review Queue</router-link>
              <router-link v-if="authStore.hasPermission('AUDIT_MANAGEMENT')" to="/admin/findings" class="dropdown-item">Findings</router-link>
              <router-link v-if="authStore.hasPermission('AUDIT_MANAGEMENT')" to="/admin/control-exceptions" class="dropdown-item">Control Exceptions</router-link>
              <router-link v-if="authStore.hasPermission('REPORT_VIEW')" to="/admin/reports" class="dropdown-item">Reports</router-link>
              <router-link v-if="authStore.hasPermission('REPORT_VIEW')" to="/admin/auditor-workbench" class="dropdown-item">Auditor Workbench</router-link>
              <router-link v-if="authStore.hasPermission('USER_MANAGEMENT')" to="/admin/users" class="dropdown-item">Users</router-link>
              <router-link
                v-if="authStore.hasPermission('AUDIT_MANAGEMENT') && (authStore.hasRole('ADMIN') || authStore.hasRole('AUDIT_MANAGER'))"
                to="/admin/questionnaire-templates"
                class="dropdown-item"
              >
                Questionnaire Governance
              </router-link>
              <router-link v-if="authStore.hasPermission('AUDIT_MANAGEMENT')" to="/admin/questionnaire-builder" class="dropdown-item">Questionnaire Builder</router-link>
            </div>
          </div>

          <div class="nav-item dropdown-hover">
            <span class="nav-link text-white px-2 dropdown-toggle">Account</span>
            <div class="dropdown-menu dropdown-menu-end border-0 shadow-sm">
              <router-link to="/profile" class="dropdown-item">Profile</router-link>
              <button type="button" class="dropdown-item" @click="logout">Log out</button>
            </div>
          </div>
        </div>
      </nav>
    </header>
    <main class="container py-4 flex-grow-1">
      <router-view />
    </main>
    <AppToasts />
  </div>
</template>

<script setup>
import api from './services/api'
import { onMounted, ref, watch } from 'vue'
import { useAuthStore } from './stores/auth'
import AppToasts from './components/AppToasts.vue'

const authStore = useAuthStore()
const notifications = ref([])
const unreadCount = ref(0)

onMounted(async () => {
  await authStore.fetchUser()
  await loadNotifications()
})

async function logout() {
  try {
    await api.post('/api/auth/logout')
  } catch {
    // local cleanup still happens
  }
  authStore.clearCredentials()
  window.location.href = '/login'
}

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

async function markRead(notificationId) {
  try {
    await api.put(`/api/notifications/${notificationId}/read`)
    await loadNotifications()
  } catch {
    // Keep UI resilient.
  }
}

async function markAllRead() {
  try {
    await api.put('/api/notifications/read-all')
    await loadNotifications()
  } catch {
    // Keep UI resilient.
  }
}
</script>

<style scoped>
.brand-header {
  overflow-x: clip;
}

.dropdown-hover {
  position: relative;
}

.dropdown-hover > .dropdown-menu {
  display: none;
  margin-top: 0;
  position: absolute;
  top: 100%;
  left: auto;
  right: 0;
  min-width: 12rem;
  max-width: min(22rem, calc(100vw - 1rem));
  z-index: 1050;
}

.dropdown-hover:hover > .dropdown-menu {
  display: block;
}

.dropdown-hover:focus-within > .dropdown-menu {
  display: block;
}

.dropdown-menu-end {
  right: 0 !important;
  left: auto !important;
}

.notification-menu {
  width: 24rem;
  max-height: 24rem;
  overflow: auto;
}
</style>
