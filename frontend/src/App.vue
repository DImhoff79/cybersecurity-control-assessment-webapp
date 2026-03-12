<template>
  <div class="min-vh-100 d-flex flex-column bg-light">
    <header v-if="showSelfServiceHeader" class="brand-header text-white border-bottom border-primary-subtle">
      <div class="container py-1 d-flex justify-content-end">
        <span class="small opacity-75">{{ authStore.user?.displayName || authStore.user?.email }}</span>
      </div>
      <nav class="navbar navbar-expand-lg navbar-dark pt-0 pb-2 px-3">
        <router-link to="/my-audits" class="navbar-brand fw-semibold">Cybersecurity Assessment</router-link>
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
          <router-link v-if="authStore.canAccessAdmin" to="/admin" class="nav-link text-white px-2">Admin Workspace</router-link>

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
    <main v-if="!route.path.startsWith('/admin')" class="container py-4 flex-grow-1">
      <router-view />
    </main>
    <router-view v-else />
    <AppToasts />
  </div>
</template>

<script setup>
import api from './services/api'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import AppToasts from './components/AppToasts.vue'
import { useNotificationsMenu } from './composables/useNotificationsMenu'

const authStore = useAuthStore()
const route = useRoute()
const { notifications, unreadCount, markRead, markAllRead } = useNotificationsMenu(authStore)

const showSelfServiceHeader = computed(() => {
  if (!authStore.user || route.name === 'Login') return false
  return !route.path.startsWith('/admin')
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
