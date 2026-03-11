<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center px-3">
    <div class="row w-100 justify-content-center">
      <div class="col-12 col-sm-10 col-md-6 col-lg-4">
        <div class="card shadow-sm">
          <div class="card-body p-4">
            <h1 class="h4 mb-2">Cybersecurity Control Assessment</h1>
            <p class="text-muted mb-4">Sign in to continue</p>
            <div v-if="oauthInfo" class="alert alert-info py-2">{{ oauthInfo }}</div>
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
            <hr class="my-4" />
            <div class="d-grid gap-2">
              <button
                type="button"
                class="btn btn-outline-danger"
                :disabled="!providers.google?.enabled"
                @click="startSocialLogin('google')"
              >
                Continue with Google
              </button>
              <button
                type="button"
                class="btn btn-outline-primary"
                :disabled="!providers.facebook?.enabled"
                @click="startSocialLogin('facebook')"
              >
                Continue with Facebook
              </button>
            </div>
            <p class="small text-muted mt-3 mb-0">
              First-time social sign-ins create an access request for admin approval.
            </p>
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
const oauthInfo = ref('')
const providers = ref({
  google: { enabled: false, url: '/oauth2/authorization/google' },
  facebook: { enabled: false, url: '/oauth2/authorization/facebook' }
})
const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN || 'http://localhost:8080'

onMounted(() => {
  loadProviders()
  handleOauthCallbackState()
  if (authStore.user) router.replace(route.query.redirect || '/my-audits')
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await authStore.setCredentials(email.value, password.value)
    if (authStore.user) {
      router.replace(route.query.redirect || defaultLandingRoute())
    } else {
      error.value = 'Invalid email or password.'
    }
  } catch (e) {
    error.value = e.response?.data?.error || 'Login failed.'
  } finally {
    loading.value = false
  }
}

async function loadProviders() {
  try {
    const res = await fetch('/api/auth/providers', { credentials: 'include' })
    if (!res.ok) return
    providers.value = await res.json()
  } catch {
    // Keep defaults disabled if API is unavailable.
  }
}

async function handleOauthCallbackState() {
  const oauthStatus = route.query.oauth
  if (oauthStatus === 'success') {
    await authStore.setOAuthSession()
    if (authStore.user) {
      router.replace(route.query.redirect || defaultLandingRoute())
      return
    }
    oauthInfo.value = 'Social sign-in succeeded, but no matching approved account was found.'
  } else if (oauthStatus === 'pending') {
    oauthInfo.value = 'Your access request was submitted. An admin must approve access before you can sign in.'
  } else if (oauthStatus === 'error') {
    oauthInfo.value = 'Social sign-in failed. Please try again or use email/password.'
  }
}

function startSocialLogin(providerKey) {
  const provider = providers.value?.[providerKey]
  if (!provider?.enabled) return
  window.location.href = `${backendOrigin}${provider.url}`
}

function defaultLandingRoute() {
  if (authStore.hasPermission('APPLICATION_MANAGEMENT')) return '/admin/applications'
  if (authStore.hasPermission('AUDIT_MANAGEMENT')) return '/admin/audits'
  if (authStore.hasPermission('REPORT_VIEW')) return '/admin/reports'
  if (authStore.hasPermission('USER_MANAGEMENT')) return '/admin/users'
  return '/my-audits'
}
</script>
