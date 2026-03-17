<template>
  <div class="min-vh-100 bg-light">
    <router-view v-if="route.path.startsWith('/admin')" />
    <div v-else-if="showSelfServiceShell" class="workspace-layout">
      <aside class="workspace-sidebar">
        <div class="workspace-brand">
          <div class="fw-semibold">Cybersecurity Assessment</div>
          <div class="small text-white-50">Workspace</div>
        </div>
        <nav class="workspace-nav">
          <router-link to="/my-audits" class="workspace-nav-item">My Audits</router-link>
          <router-link to="/my-tasks" class="workspace-nav-item">My Tasks</router-link>
          <router-link to="/my-policies" class="workspace-nav-item">My Policies</router-link>
          <router-link to="/profile" class="workspace-nav-item">Profile</router-link>
          <router-link v-if="authStore.canAccessAdmin" to="/admin" class="workspace-nav-item">Admin Workspace</router-link>
        </nav>
      </aside>

      <div class="workspace-main">
        <header class="workspace-utility border-bottom">
          <div class="small text-muted">
            Signed in as <strong>{{ authStore.user?.displayName || authStore.user?.email }}</strong>
          </div>
          <div class="d-flex align-items-center gap-2">
            <div class="nav-item dropdown-hover">
              <span class="btn btn-outline-secondary btn-sm dropdown-toggle">
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
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="logout">Log out</button>
          </div>
        </header>
        <main class="container py-4 flex-grow-1">
          <router-view />
        </main>
      </div>
    </div>
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

const showSelfServiceShell = computed(() => {
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
.workspace-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
}

.workspace-sidebar {
  background: #0b2340;
  color: #fff;
  padding: 1rem;
}

.workspace-brand {
  margin-bottom: 1rem;
}

.workspace-nav-item {
  display: block;
  color: rgba(255, 255, 255, 0.92);
  text-decoration: none;
  border-radius: 0.4rem;
  padding: 0.45rem 0.55rem;
  margin-bottom: 0.2rem;
}

.workspace-nav-item:hover,
.workspace-nav-item.router-link-active {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.workspace-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.workspace-utility {
  background: #fff;
  padding: 0.6rem 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

@media (max-width: 900px) {
  .workspace-layout {
    grid-template-columns: 1fr;
  }

  .workspace-sidebar {
    padding: 0.75rem;
  }
}
</style>
