<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center px-3">
    <div class="row w-100 justify-content-center">
      <div class="col-12 col-sm-10 col-md-6 col-lg-4">
        <div class="card shadow-sm">
          <div class="card-body p-4">
            <h1 class="h4 mb-2">Cybersecurity Control Assessment</h1>
            <p class="text-muted mb-4">Sign in to continue</p>
            <form @submit.prevent="submit">
              <div class="mb-3">
                <label for="email" class="form-label">Email</label>
                <input id="email" v-model="email" type="email" required autocomplete="username" class="form-control" />
              </div>
              <div class="mb-3">
                <label for="password" class="form-label">Password</label>
                <input id="password" v-model="password" type="password" required autocomplete="current-password" class="form-control" />
              </div>
              <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>
              <button type="submit" class="btn btn-primary w-100" :disabled="loading">Sign in</button>
            </form>
          </div>
        </div>
      </div>
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
