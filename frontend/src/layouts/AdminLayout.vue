<template>
  <div class="admin-layout">
    <aside class="admin-sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <router-link to="/admin" class="brand-link">Cybersecurity Assessment</router-link>
        <div class="brand-caption">Admin Workspace</div>
      </div>

      <nav class="sidebar-nav">
        <section v-for="section in visibleSections" :key="section.key" class="nav-section">
          <div class="section-label">{{ section.label }}</div>
          <router-link
            v-for="item in section.items"
            :key="item.to"
            :to="item.to"
            class="nav-item"
            :class="{ active: isActive(item.to) }"
          >
            {{ item.label }}
          </router-link>
        </section>
      </nav>
    </aside>

    <div class="admin-main">
      <header class="utility-bar border-bottom">
        <div class="d-flex align-items-center gap-2 flex-wrap">
          <button type="button" class="btn btn-outline-secondary btn-sm menu-toggle" @click="sidebarOpen = !sidebarOpen">
            Menu
          </button>
          <div class="small text-muted">
            <span class="d-block d-sm-inline">
              <strong>Admin Workspace</strong>
              <span class="mx-1">/</span>
              {{ currentSection }}
              <span class="mx-1">/</span>
              {{ currentPage }}
            </span>
            <span class="d-block d-sm-inline ms-sm-2">
              Signed in as <strong>{{ authStore.user?.displayName || authStore.user?.email }}</strong>
            </span>
          </div>
        </div>

        <div class="d-flex align-items-center gap-2 flex-wrap justify-content-end">
          <router-link to="/my-audits" class="btn btn-outline-secondary btn-sm">My Audits</router-link>
          <router-link to="/my-tasks" class="btn btn-outline-secondary btn-sm">My Tasks</router-link>

          <div class="dropdown position-relative">
            <button class="btn btn-outline-secondary btn-sm dropdown-toggle" type="button">
              Notifications
              <span v-if="unreadCount > 0" class="badge text-bg-danger ms-1">{{ unreadCount }}</span>
            </button>
            <div class="dropdown-menu dropdown-menu-end p-0 shadow-sm notification-menu">
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

          <div class="dropdown position-relative">
            <button class="btn btn-outline-secondary btn-sm dropdown-toggle" type="button">Account</button>
            <div class="dropdown-menu dropdown-menu-end shadow-sm">
              <router-link to="/profile" class="dropdown-item">Profile</router-link>
              <button type="button" class="dropdown-item" @click="logout">Log out</button>
            </div>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'
import { adminSections } from '../config/adminNavigation'
import { useNotificationsMenu } from '../composables/useNotificationsMenu'

const authStore = useAuthStore()
const route = useRoute()
const sidebarOpen = ref(false)
const { notifications, unreadCount, markRead, markAllRead } = useNotificationsMenu(authStore)

const visibleSections = computed(() => {
  return adminSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => {
        if (item.permission && !authStore.hasPermission(item.permission)) return false
        if (item.roles?.length && !item.roles.includes(authStore.user?.role)) return false
        return true
      })
    }))
    .filter((section) => section.items.length > 0)
})

function isActive(to) {
  return route.path === to || route.path.startsWith(`${to}/`)
}

const currentSection = computed(() => route.meta.section || 'Workspace')
const currentPage = computed(() => route.meta.pageTitle || 'Overview')

watch(
  () => route.fullPath,
  () => {
    sidebarOpen.value = false
  }
)

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
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  background: #f8f9fa;
}

.admin-sidebar {
  background: #0b2340;
  color: #fff;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1rem;
  position: sticky;
  top: 0;
  align-self: start;
  height: 100vh;
  overflow-y: auto;
}

.sidebar-brand {
  margin-bottom: 1rem;
}

.brand-link {
  color: #fff;
  font-size: 1.05rem;
  font-weight: 600;
  text-decoration: none;
}

.brand-caption {
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.85rem;
}

.nav-section + .nav-section {
  margin-top: 1rem;
}

.section-label {
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.65);
  margin-bottom: 0.35rem;
}

.nav-item {
  display: block;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  border-radius: 0.4rem;
  padding: 0.45rem 0.55rem;
  margin-bottom: 0.15rem;
}

.nav-item:hover,
.nav-item.active {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.admin-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.utility-bar {
  background: #fff;
  padding: 0.6rem 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.admin-content {
  padding: 0.9rem;
}

.dropdown:hover > .dropdown-menu,
.dropdown:focus-within > .dropdown-menu {
  display: block;
}

.notification-menu {
  width: 24rem;
  max-height: 24rem;
  overflow: auto;
}

.menu-toggle {
  display: none;
}

@media (max-width: 1100px) {
  .admin-layout {
    grid-template-columns: 220px minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    width: min(88vw, 300px);
    z-index: 1100;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }

  .admin-sidebar.open {
    transform: translateX(0);
  }

  .menu-toggle {
    display: inline-block;
  }

  .utility-bar {
    position: sticky;
    top: 0;
    z-index: 1080;
  }

  .admin-content {
    padding: 0.75rem;
  }
}
</style>
