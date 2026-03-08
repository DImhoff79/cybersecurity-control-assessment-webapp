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

          <div v-if="authStore.isAdmin" class="nav-item dropdown-hover">
            <span class="nav-link text-white px-2 dropdown-toggle">Admin</span>
            <div class="dropdown-menu show border-0 shadow-sm">
              <router-link to="/admin/applications" class="dropdown-item">Applications</router-link>
              <router-link to="/admin/audits" class="dropdown-item">Audits</router-link>
              <router-link to="/admin/review-queue" class="dropdown-item">Review Queue</router-link>
              <router-link to="/admin/reports" class="dropdown-item">Reports</router-link>
              <router-link to="/admin/auditor-workbench" class="dropdown-item">Auditor Workbench</router-link>
              <router-link to="/admin/questionnaire-templates" class="dropdown-item">Questionnaire Templates</router-link>
              <router-link to="/admin/controls" class="dropdown-item">Controls</router-link>
              <router-link to="/admin/questions" class="dropdown-item">Questions</router-link>
            </div>
          </div>

          <div class="nav-item dropdown-hover">
            <span class="nav-link text-white px-2 dropdown-toggle">Account</span>
            <div class="dropdown-menu dropdown-menu-end show border-0 shadow-sm">
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
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth'
import AppToasts from './components/AppToasts.vue'

const authStore = useAuthStore()

onMounted(() => {
  authStore.fetchUser()
})

function logout() {
  authStore.clearCredentials()
  window.location.href = '/login'
}
</script>

<style scoped>
.dropdown-hover {
  position: relative;
}

.dropdown-hover > .dropdown-menu {
  display: none;
  margin-top: 0;
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 12rem;
}

.dropdown-hover:hover > .dropdown-menu {
  display: block;
}

.dropdown-menu-end {
  right: 0;
  left: auto;
}
</style>
