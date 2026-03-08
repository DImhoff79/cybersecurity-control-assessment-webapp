<template>
  <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 1200;">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="toast show align-items-center border-0 mb-2"
      :class="`text-bg-${toast.variant}`"
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
    >
      <div class="d-flex">
        <div class="toast-body">{{ toast.message }}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" @click="dismiss(toast.id)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { TOAST_EVENT_NAME } from '../services/toast'

const toasts = ref([])

function dismiss(id) {
  toasts.value = toasts.value.filter((x) => x.id !== id)
}

function onToast(event) {
  const toast = event.detail
  if (!toast?.id) return
  toasts.value.push(toast)
  setTimeout(() => dismiss(toast.id), toast.timeout || 3500)
}

onMounted(() => window.addEventListener(TOAST_EVENT_NAME, onToast))
onUnmounted(() => window.removeEventListener(TOAST_EVENT_NAME, onToast))
</script>
