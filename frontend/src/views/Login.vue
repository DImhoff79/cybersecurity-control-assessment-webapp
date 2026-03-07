<template>
  <div class="login-page">
    <div class="login-card card">
      <h1 class="page-title">Cybersecurity Control Assessment</h1>
      <p class="subtitle">Sign in to continue</p>
      <form @submit.prevent="submit">
        <div class="form-group">
          <label for="email">Email</label>
          <input id="email" v-model="email" type="email" required autocomplete="username" />
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input id="password" v-model="password" type="password" required autocomplete="current-password" />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" class="btn btn-primary" :disabled="loading">Sign in</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

onMounted(() => {
  if (authStore.user) router.replace(route.query.redirect || '/my-audits')
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await authStore.setCredentials(email.value, password.value)
    if (authStore.user) {
      router.replace(route.query.redirect || (authStore.isAdmin ? '/admin/applications' : '/my-audits'))
    } else {
      error.value = 'Invalid email or password.'
    }
  } catch (e) {
    error.value = e.response?.data?.error || 'Login failed.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 1rem; }
.login-card { max-width: 400px; width: 100%; }
.subtitle { color: #718096; margin-bottom: 1.5rem; }
.login-card .btn { width: 100%; margin-top: 0.5rem; }
</style>
