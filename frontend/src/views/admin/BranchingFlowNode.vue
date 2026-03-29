<script setup>
import { Handle, Position } from '@vue-flow/core'

defineProps({
  data: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div
    class="branching-flow-node rounded border bg-body p-2 shadow-sm small user-select-none"
    :class="{
      'branching-flow-node--start': data.isStart,
      'branching-flow-node--selected': data.isSelected,
      'branching-flow-node--run': data.isRun
    }"
    style="min-width: 148px; max-width: 240px"
  >
    <Handle type="target" :position="Position.Left" class="branching-flow-node__handle" />
    <div class="d-flex align-items-start justify-content-between gap-1 mb-1">
      <span class="badge rounded-pill text-bg-secondary">Q{{ data.num }}</span>
      <span v-if="data.isStart" class="badge rounded-pill text-bg-success" style="font-size: 0.6rem">Start</span>
    </div>
    <div class="text-muted text-uppercase" style="font-size: 0.6rem; letter-spacing: 0.03em">{{ data.typeLabel }}</div>
    <div class="fw-semibold text-break">{{ data.title }}</div>
    <div class="font-monospace text-muted text-truncate mt-1" style="font-size: 0.65rem" :title="data.stableKey">
      {{ data.stableKey }}
    </div>
    <Handle
      v-if="!data.hideSourceHandle"
      type="source"
      :position="Position.Right"
      class="branching-flow-node__handle"
    />
  </div>
</template>

<style scoped>
.branching-flow-node--selected {
  outline: 2px solid var(--bs-primary);
  outline-offset: 2px;
}
.branching-flow-node--run {
  outline: 2px solid var(--bs-success, #198754);
  outline-offset: 1px;
}
.branching-flow-node__handle {
  width: 8px;
  height: 8px;
  border: 1px solid var(--bs-secondary);
  background: var(--bs-body-bg);
}
</style>
