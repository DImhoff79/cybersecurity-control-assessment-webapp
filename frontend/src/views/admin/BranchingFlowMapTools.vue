<script setup>
import { Panel, useVueFlow } from '@vue-flow/core'

const emit = defineEmits(['add-step'])

const { fitView } = useVueFlow()

function onAdd() {
  emit('add-step')
}

function onFit() {
  fitView({ padding: 0.2, duration: 200 })
}
</script>

<template>
  <Panel position="top-right" class="branching-map-tools p-1">
    <div class="d-flex flex-column gap-1">
      <button type="button" class="btn btn-primary btn-sm shadow-sm" title="Add a new step on the map" @click.stop="onAdd">
        Add step
      </button>
      <button
        type="button"
        class="btn btn-outline-secondary btn-sm bg-body shadow-sm"
        title="Zoom to show all steps"
        @click.stop="onFit"
      >
        Fit view
      </button>
    </div>
  </Panel>
</template>

<style scoped>
.branching-map-tools {
  z-index: 20;
}
</style>
