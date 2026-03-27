<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1 fw-semibold text-dark">Profile</h1>
      <p class="text-muted mb-0">
        Your first and last name appear to auditors and in assignment details. Keep them accurate.
      </p>
    </div>

    <div class="card workspace-card border-0 shadow-sm">
      <div class="card-body p-4">
        <div v-if="loading" class="text-muted">Loading…</div>
        <form v-else class="profile-form" @submit.prevent="save">
          <div class="row g-3 mb-3">
            <div class="col-md-6">
              <label class="form-label small fw-semibold text-secondary" for="profile-first">First name</label>
              <input
                id="profile-first"
                v-model="form.firstName"
                type="text"
                class="form-control"
                autocomplete="given-name"
                maxlength="120"
              />
            </div>
            <div class="col-md-6">
              <label class="form-label small fw-semibold text-secondary" for="profile-last">Last name</label>
              <input
                id="profile-last"
                v-model="form.lastName"
                type="text"
                class="form-control"
                autocomplete="family-name"
                maxlength="120"
              />
            </div>
          </div>
          <div class="mb-3">
            <div class="small text-secondary">Email (for sign-in)</div>
            <div class="fw-medium">{{ form.email || '—' }}</div>
          </div>
          <div class="mb-2">
            <div class="small text-secondary">Role</div>
            <div class="fw-medium">{{ form.role || '—' }}</div>
          </div>
          <div v-if="saveError" class="alert alert-danger py-2 small">{{ saveError }}</div>
          <div v-if="saveOk" class="alert alert-success py-2 small">Profile saved.</div>
          <button type="submit" class="btn btn-primary btn-sm rounded-pill px-4" :disabled="saving">
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const saveError = ref('')
const saveOk = ref(false)

const form = reactive({
  firstName: '',
  lastName: '',
  email: '',
  role: ''
})

onMounted(async () => {
  try {
    await authStore.fetchUser()
    const u = authStore.user
    if (u) {
      form.firstName = u.firstName || ''
      form.lastName = u.lastName || ''
      form.email = u.email || ''
      form.role = u.role || ''
    }
  } finally {
    loading.value = false
  }
})

async function save() {
  saveError.value = ''
  saveOk.value = false
  saving.value = true
  try {
    const body = {
      firstName: form.firstName?.trim() || '',
      lastName: form.lastName?.trim() || ''
    }
    try {
      await api.post('/api/auth/profile', body)
    } catch (e1) {
      if (e1.response?.status === 404) {
        await api.put('/api/auth/profile', body)
      } else {
        throw e1
      }
    }
    await authStore.fetchUser()
    const u = authStore.user
    if (u) {
      form.firstName = u.firstName ?? ''
      form.lastName = u.lastName ?? ''
    }
    saveOk.value = true
  } catch (e) {
    if (e.response?.status === 404) {
      saveError.value =
        'The profile API was not found (404). Restart the backend so it runs a build that includes POST/PUT /api/auth/profile, then try again.'
      return
    }
    const data = e.response?.data
    const msg =
      (typeof data === 'string' && data) ||
      data?.error ||
      data?.message ||
      e.message ||
      'Could not save profile.'
    saveError.value = msg
  } finally {
    saving.value = false
  }
}
</script>
