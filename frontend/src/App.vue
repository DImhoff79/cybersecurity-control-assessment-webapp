<template>
  <div class="min-vh-100 bg-light">
    <router-view />
    <AppToasts />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import AppToasts from './components/AppToasts.vue'
import {
  startWorkspaceTour,
  startApplicationOwnerWorkspaceTour,
  startAdminTour
} from './composables/productTour'

const authStore = useAuthStore()
const route = useRoute()

const isWorkspaceShellRoute = () =>
  authStore.user &&
  route.name !== 'Login' &&
  !route.path.startsWith('/admin')

const didWorkspaceTour = ref(false)
const didAdminTour = ref(false)

function scheduleProductTour() {
  if (!authStore.user || route.name === 'Login') return
  if (route.path === '/start' || route.name === 'RoleHub') return
  nextTick(() => {
    setTimeout(() => {
      if (route.path.startsWith('/admin')) {
        if (!didAdminTour.value) {
          didAdminTour.value = true
          startAdminTour()
        }
      } else if (isWorkspaceShellRoute()) {
        if (!didWorkspaceTour.value) {
          didWorkspaceTour.value = true
          if (authStore.user?.role === 'APPLICATION_OWNER') {
            startApplicationOwnerWorkspaceTour()
          } else {
            startWorkspaceTour()
          }
        }
      }
    }, 750)
  })
}

watch(
  () => [authStore.user?.id, route.fullPath],
  () => {
    if (!authStore.user) {
      didWorkspaceTour.value = false
      didAdminTour.value = false
      return
    }
    scheduleProductTour()
  },
  { immediate: true }
)
</script>
