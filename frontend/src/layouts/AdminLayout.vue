<template>
  <div class="admin-layout">
    <div
      class="sidebar-backdrop"
      :class="{ 'is-visible': sidebarOpen }"
      aria-hidden="true"
      @click="sidebarOpen = false"
    />

    <aside id="admin-sidebar-nav" class="admin-sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <router-link to="/admin" class="brand-link" @click="sidebarOpen = false">Cybersecurity Assessment</router-link>
        <div class="brand-caption">Admin Workspace</div>
      </div>

      <nav class="sidebar-nav" aria-label="Admin sidebar">
        <section v-for="section in visibleSections" :key="section.key" class="nav-section">
          <div class="section-label">{{ section.label }}</div>
          <router-link
            v-for="item in section.items"
            :key="item.to"
            :to="item.to"
            class="nav-item"
            :class="{ active: isNavItemActive(item.to) }"
            @click="sidebarOpen = false"
          >
            {{ item.label }}
          </router-link>
        </section>
      </nav>
    </aside>

    <div class="admin-main">
      <header class="utility-bar border-bottom">
        <div class="d-flex align-items-center gap-2 flex-wrap">
          <button
            type="button"
            class="btn btn-outline-secondary btn-sm menu-toggle"
            :aria-expanded="sidebarOpen"
            aria-controls="admin-sidebar-nav"
            @click="sidebarOpen = !sidebarOpen"
          >
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

        <div class="d-flex align-items-center gap-2 flex-wrap justify-content-end utility-bar-actions">
          <div
            ref="notificationsDropdownRef"
            class="dropdown position-relative utility-dropdown"
            @mouseenter="positionUtilityDropdown(notificationsDropdownRef)"
            @mouseleave="resetUtilityDropdown(notificationsDropdownRef)"
            @focusin="positionUtilityDropdown(notificationsDropdownRef)"
            @focusout="onUtilityDropdownFocusOut"
          >
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

          <div
            ref="accountDropdownRef"
            class="dropdown position-relative utility-dropdown"
            @mouseenter="positionUtilityDropdown(accountDropdownRef)"
            @mouseleave="resetUtilityDropdown(accountDropdownRef)"
            @focusin="positionUtilityDropdown(accountDropdownRef)"
            @focusout="onUtilityDropdownFocusOut"
          >
            <button class="btn btn-outline-secondary btn-sm dropdown-toggle" type="button">Account</button>
            <div class="dropdown-menu dropdown-menu-end shadow-sm account-menu">
              <router-link to="/admin/profile" class="dropdown-item">Profile</router-link>
              <button type="button" class="dropdown-item" @click="logout">Log out</button>
            </div>
          </div>
        </div>
      </header>

      <!-- Same links as the sidebar, for narrow viewports where the drawer is hidden -->
      <nav class="admin-mobile-nav border-bottom bg-white" aria-label="Admin pages">
        <div class="admin-mobile-nav-scroll">
          <template v-for="section in visibleSections" :key="'mob-' + section.key">
            <span class="admin-mobile-nav-section-label">{{ section.label }}</span>
            <router-link
              v-for="item in section.items"
              :key="'mob-' + item.to"
              :to="item.to"
              class="admin-mobile-nav-pill"
              :class="{ active: isNavItemActive(item.to) }"
            >
              {{ item.label }}
            </router-link>
          </template>
        </div>
      </nav>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'
import { adminSections } from '../config/adminNavigation'
import { findLongestNavMatch } from '../utils/adminNavMatch'
import { useNotificationsMenu } from '../composables/useNotificationsMenu'

const authStore = useAuthStore()
const route = useRoute()
const sidebarOpen = ref(false)
const notificationsDropdownRef = ref(null)
const accountDropdownRef = ref(null)
const { notifications, unreadCount, markRead, markAllRead } = useNotificationsMenu(authStore)

/** Fixed positioning so menus escape grid overflow and stay aligned to the trigger without shifting layout */
function getDropdownRoot(refOrEl) {
  return refOrEl?.value ?? refOrEl
}

function positionUtilityDropdown(refOrEl) {
  const root = getDropdownRoot(refOrEl)
  if (!root) return
  const btn = root.querySelector('button.dropdown-toggle')
  const menu = root.querySelector('.dropdown-menu')
  if (!btn || !menu) return
  const rect = btn.getBoundingClientRect()
  const margin = 4
  menu.style.position = 'fixed'
  menu.style.top = `${Math.round(rect.bottom + margin)}px`
  menu.style.left = 'auto'
  menu.style.right = `${Math.round(window.innerWidth - rect.right)}px`
  menu.style.marginTop = '0'
  menu.style.marginLeft = '0'
  menu.style.transform = 'none'
  menu.style.zIndex = '2000'
}

function resetUtilityDropdown(refOrEl) {
  const root = getDropdownRoot(refOrEl)
  if (!root) return
  const menu = root.querySelector('.dropdown-menu')
  if (!menu) return
  ;['position', 'top', 'left', 'right', 'marginTop', 'marginLeft', 'transform', 'zIndex'].forEach((k) => {
    menu.style[k] = ''
  })
}

function onUtilityDropdownFocusOut(event) {
  const el = event.currentTarget
  const next = event.relatedTarget
  if (next && el.contains(next)) return
  resetUtilityDropdown(el)
}

function repositionOpenUtilityDropdowns() {
  if (notificationsDropdownRef.value?.matches(':hover') || notificationsDropdownRef.value?.contains(document.activeElement)) {
    positionUtilityDropdown(notificationsDropdownRef)
  }
  if (accountDropdownRef.value?.matches(':hover') || accountDropdownRef.value?.contains(document.activeElement)) {
    positionUtilityDropdown(accountDropdownRef)
  }
}

let repositionOnScroll = null
onMounted(() => {
  repositionOnScroll = () => repositionOpenUtilityDropdowns()
  window.addEventListener('scroll', repositionOnScroll, true)
  window.addEventListener('resize', repositionOnScroll)
})
onUnmounted(() => {
  if (repositionOnScroll) {
    window.removeEventListener('scroll', repositionOnScroll, true)
    window.removeEventListener('resize', repositionOnScroll)
  }
})

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

const navMatch = computed(() => findLongestNavMatch(route.path, visibleSections.value))

function isNavItemActive(itemTo) {
  return navMatch.value?.item.to === itemTo
}

/** Breadcrumb: prefer labels from the same nav config as the sidebar */
const currentSection = computed(() => navMatch.value?.section.label ?? route.meta.section ?? 'Workspace')
const currentPage = computed(() => navMatch.value?.item.label ?? route.meta.pageTitle ?? 'Overview')

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
  overflow: visible;
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
  overflow: visible;
}

.utility-bar {
  background: #fff;
  padding: 0.6rem 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  overflow: visible;
}

.utility-bar-actions {
  flex-shrink: 0;
}

/* Invisible hover bridge so moving from button → menu doesn’t close the dropdown */
.utility-dropdown {
  padding-bottom: 0.5rem;
}

.admin-content {
  padding: 0.9rem;
}

.utility-dropdown:hover > .dropdown-menu,
.utility-dropdown:focus-within > .dropdown-menu {
  display: block;
}

.notification-menu {
  width: min(24rem, calc(100vw - 1rem));
  max-width: calc(100vw - 1rem);
  max-height: min(24rem, 70vh);
  overflow: auto;
}

.account-menu {
  min-width: 11rem;
  max-width: calc(100vw - 1rem);
}

.menu-toggle {
  display: none;
}

.sidebar-backdrop {
  display: none;
}

.admin-mobile-nav {
  display: none;
}

@media (max-width: 900px) {
  .sidebar-backdrop.is-visible {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 1090;
    background: rgba(15, 23, 42, 0.45);
  }

  .admin-mobile-nav {
    display: block;
  }

  .admin-mobile-nav-scroll {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 0.35rem;
    overflow-x: auto;
    padding: 0.5rem 0.65rem;
    -webkit-overflow-scrolling: touch;
  }

  .admin-mobile-nav-section-label {
    flex-shrink: 0;
    font-size: 0.65rem;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: #6c757d;
    padding: 0 0.25rem 0 0.35rem;
    border-left: 2px solid #dee2e6;
    margin-left: 0.15rem;
  }

  .admin-mobile-nav-section-label:first-child {
    border-left: 0;
    margin-left: 0;
    padding-left: 0;
  }

  .admin-mobile-nav-pill {
    flex-shrink: 0;
    font-size: 0.8rem;
    padding: 0.25rem 0.55rem;
    border-radius: 999px;
    border: 1px solid #ced4da;
    color: #212529;
    text-decoration: none;
    background: #fff;
    white-space: nowrap;
  }

  .admin-mobile-nav-pill:hover {
    background: #f8f9fa;
  }

  .admin-mobile-nav-pill.active {
    background: #0d6efd;
    border-color: #0d6efd;
    color: #fff;
  }

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

/* Tablet: narrow sidebar column; mobile (≤900px) overrides to overlay + single column above */
@media (max-width: 1100px) and (min-width: 901px) {
  .admin-layout {
    grid-template-columns: 220px minmax(0, 1fr);
  }
}
</style>
