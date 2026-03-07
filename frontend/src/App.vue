<template>
  <div class="app">
    <header v-if="authStore.user && $route.name !== 'Login'" class="header">
      <router-link to="/" class="logo">Cybersecurity Assessment</router-link>
      <nav>
        <template v-if="authStore.isAdmin">
          <router-link to="/admin/applications">Applications</router-link>
          <router-link to="/admin/audits">Audits</router-link>
          <router-link to="/admin/controls">Controls</router-link>
        </template>
        <router-link to="/my-audits">My Audits</router-link>
        <span class="user">{{ authStore.user?.displayName || authStore.user?.email }}</span>
        <button type="button" class="btn-link" @click="logout">Log out</button>
      </nav>
    </header>
    <main class="main">
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

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: system-ui, sans-serif; background: #f5f5f5; }
.app { min-height: 100vh; display: flex; flex-direction: column; }
.header {
  background: #1a365d;
  color: #fff;
  padding: 0.75rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo { color: #fff; text-decoration: none; font-weight: 700; }
.header nav { display: flex; align-items: center; gap: 1.25rem; }
.header nav a { color: #a0aec0; text-decoration: none; }
.header nav a.router-link-active { color: #fff; }
.user { color: #a0aec0; font-size: 0.9rem; }
.btn-link { background: none; border: none; color: #a0aec0; cursor: pointer; font-size: inherit; }
.btn-link:hover { color: #fff; }
.main { flex: 1; padding: 1.5rem; max-width: 1200px; margin: 0 auto; width: 100%; }
.page-title { margin: 0 0 1rem; font-size: 1.5rem; }
.card { background: #fff; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); padding: 1.25rem; margin-bottom: 1rem; }
table { width: 100%; border-collapse: collapse; }
th, td { text-align: left; padding: 0.5rem 0.75rem; border-bottom: 1px solid #e2e8f0; }
th { background: #f7fafc; font-weight: 600; }
.btn { padding: 0.5rem 1rem; border-radius: 6px; border: none; cursor: pointer; font-size: 0.9rem; }
.btn-primary { background: #2b6cb0; color: #fff; }
.btn-primary:hover { background: #2c5282; }
.btn-secondary { background: #e2e8f0; color: #1a202c; }
.btn-secondary:hover { background: #cbd5e0; }
.btn-danger { background: #c53030; color: #fff; }
.btn-danger:hover { background: #9b2c2c; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.25rem; font-weight: 500; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 0.5rem; border: 1px solid #cbd5e0; border-radius: 6px;
}
.form-group textarea { min-height: 80px; resize: vertical; }
.error { color: #c53030; font-size: 0.9rem; margin-top: 0.25rem; }
</style>
