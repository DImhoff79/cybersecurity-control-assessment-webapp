<template>
  <div class="min-vh-100 d-flex flex-column bg-light">
    <header v-if="authStore.user && $route.name !== 'Login'" class="navbar navbar-expand-lg navbar-dark bg-primary px-3">
      <router-link to="/" class="navbar-brand fw-semibold">Cybersecurity Assessment</router-link>
      <nav class="navbar-nav ms-auto d-flex flex-row align-items-center gap-3">
        <template v-if="authStore.isAdmin">
          <router-link to="/admin/applications" class="nav-link text-white">Applications</router-link>
          <router-link to="/admin/audits" class="nav-link text-white">Audits</router-link>
          <router-link to="/admin/controls" class="nav-link text-white">Controls</router-link>
        </template>
        <router-link to="/my-audits" class="nav-link text-white">My Audits</router-link>
        <span class="text-white-50 small">{{ authStore.user?.displayName || authStore.user?.email }}</span>
        <button type="button" class="btn btn-outline-light btn-sm" @click="logout">Log out</button>
      </nav>
    </header>
    <main class="container py-4 flex-grow-1">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()

onMounted(() => {
  authStore.fetchUser()
})

function logout() {
  authStore.clearCredentials()
  window.location.href = '/login'
}
</script>
