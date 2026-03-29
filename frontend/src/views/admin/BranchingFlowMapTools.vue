<script setup>
import { nextTick, watch } from 'vue'
import { Panel, useVueFlow } from '@vue-flow/core'

const props = defineProps({
  layoutFitTick: { type: Number, default: 0 }
})

const emit = defineEmits(['add-step', 'auto-layout'])

const { fitView } = useVueFlow()

watch(
  () => props.layoutFitTick,
  (v) => {
    if (v > 0) {
      nextTick(() => fitView({ padding: 0.2, duration: 280 }))
    }
  }
)

function onAdd() {
  emit('add-step')
}

function onFit() {
  fitView({ padding: 0.2, duration: 200 })
}

function onAutoLayout() {
  emit('auto-layout')
}
</script>

<template>
  <Panel position="top-right" class="branching-map-tools p-2 rounded-3 shadow">
    <div class="d-flex flex-column gap-2">
      <button type="button" class="btn btn-primary btn-sm shadow-sm" title="Add a new step on the map" @click.stop="onAdd">
        Add step
      </button>
      <button
        type="button"
        class="btn btn-outline-secondary btn-sm bg-body shadow-sm"
        title="Arrange steps left-to-right along branches (automatic layout)"
        @click.stop="onAutoLayout"
      >
        Tidy layout
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
