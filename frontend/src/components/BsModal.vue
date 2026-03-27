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
  size: { type: String, default: '' }, // '', 'sm', 'lg', 'xl'
  /** Long forms: scroll body inside dialog instead of growing past the viewport */
  scrollable: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'hidden'])

const modalEl = ref(null)
let modalInstance = null

/** Sync v-model when hide starts (backdrop, Esc, programmatic). Do not emit false from hidden.bs.modal — that can fire after a new show() if the user reopened before the previous hide animation finished, which would incorrectly close the modal. */
function hideHandler(event) {
  if (event.defaultPrevented) return
  if (props.modelValue) {
    emit('update:modelValue', false)
  }
}

function hiddenHandler() {
  emit('hidden')
}

const dialogClass = computed(() => {
  const sizeClass = props.size ? ` modal-${props.size}` : ''
  const scrollClass = props.scrollable ? ' modal-dialog-scrollable' : ''
  return `modal-dialog${sizeClass}${scrollClass}`
})

onMounted(async () => {
  await nextTick()
  if (!modalEl.value) return
  modalInstance = new Modal(modalEl.value, {
    backdrop: true,
    keyboard: true,
    focus: true
  })
  modalEl.value.addEventListener('hide.bs.modal', hideHandler)
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
    modalEl.value.removeEventListener('hide.bs.modal', hideHandler)
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
