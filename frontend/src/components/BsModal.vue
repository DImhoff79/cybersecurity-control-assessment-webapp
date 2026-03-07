<template>
  <teleport to="body">
    <div
      ref="modalEl"
      class="modal fade"
      tabindex="-1"
      aria-hidden="true"
    >
      <div :class="dialogClass">
        <div class="modal-content">
          <div class="modal-header">
            <h2 class="h5 mb-0">{{ title }}</h2>
            <button type="button" class="btn-close" aria-label="Close" @click="closeModal" />
          </div>
          <div class="modal-body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Modal from 'bootstrap/js/dist/modal'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, default: '' },
  size: { type: String, default: '' } // '', 'sm', 'lg', 'xl'
})

const emit = defineEmits(['update:modelValue', 'hidden'])

const modalEl = ref(null)
let modalInstance = null
const hiddenHandler = () => {
  emit('update:modelValue', false)
  emit('hidden')
}

const dialogClass = computed(() => {
  const sizeClass = props.size ? ` modal-${props.size}` : ''
  return `modal-dialog${sizeClass}`
})

onMounted(async () => {
  await nextTick()
  if (!modalEl.value) return
  modalInstance = new Modal(modalEl.value, {
    backdrop: true,
    keyboard: true,
    focus: true
  })
  modalEl.value.addEventListener('hidden.bs.modal', hiddenHandler)
  if (props.modelValue) {
    modalInstance.show()
  }
})

watch(
  () => props.modelValue,
  (open) => {
    if (!modalInstance) return
    if (open) modalInstance.show()
    else modalInstance.hide()
  }
)

onBeforeUnmount(() => {
  if (modalEl.value) {
    modalEl.value.removeEventListener('hidden.bs.modal', hiddenHandler)
  }
  if (modalInstance) {
    modalInstance.dispose()
    modalInstance = null
  }
})

function closeModal() {
  emit('update:modelValue', false)
}
</script>
