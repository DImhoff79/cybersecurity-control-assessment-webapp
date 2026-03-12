<template>
  <div class="d-flex align-items-center justify-content-center py-5">
    <div class="card shadow-sm w-100" style="max-width: 40rem;">
      <div class="card-body p-4">
        <h1 class="h4 mb-2">Access Denied</h1>
        <p class="text-muted mb-3">
          You do not have permission to view this page.
        </p>
        <p class="small mb-0">
          <span class="text-muted">Requested path:</span>
          <code>{{ attemptedPath || '-' }}</code>
        </p>
        <p class="small mb-3" v-if="requiredPermission">
          <span class="text-muted">Required permission:</span>
          <code>{{ requiredPermission }}</code>
        </p>
        <div class="d-flex gap-2">
          <button class="btn btn-primary" type="button" @click="goToHome">Go to my dashboard</button>
          <button class="btn btn-outline-secondary" type="button" @click="goBack">Back</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const attemptedPath = computed(() => route.query.from || '')
const requiredPermission = computed(() => route.query.required || '')

function goToHome() {
  if (authStore.canAccessAdmin) {
    router.push('/admin')
    return
  }
  router.push('/my-audits')
}

function goBack() {
  router.back()
}
</script>
