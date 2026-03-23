<template>
  <div>
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Audit Queue</h1>
        <p class="text-muted mb-0">Health snapshot and prioritized audits and evidence — details stay one click away.</p>
      </div>
    </div>

    <!-- Only show tabs when there are two views; otherwise "Triage Hub" looked like a broken button (already on that view). -->
    <ul v-if="canManageAudits" class="nav nav-pills mb-3" role="tablist">
      <li class="nav-item" role="presentation">
        <button
          type="button"
          class="nav-link"
          role="tab"
          :class="{ active: activeTab === 'triage' }"
          :aria-selected="activeTab === 'triage'"
          @click="activeTab = 'triage'"
        >
          Triage Hub
        </button>
      </li>
      <li class="nav-item" role="presentation">
        <button
          type="button"
          class="nav-link"
          role="tab"
          :class="{ active: activeTab === 'reviews' }"
          :aria-selected="activeTab === 'reviews'"
          @click="activeTab = 'reviews'"
        >
          Submitted Reviews
        </button>
      </li>
    </ul>

    <section v-show="activeTab === 'triage'">
      <AuditorWorkbench />
    </section>
    <section v-show="activeTab === 'reviews'">
      <ReviewQueue />
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AuditorWorkbench from './AuditorWorkbench.vue'
import ReviewQueue from './ReviewQueue.vue'
import { useAuthStore } from '../../stores/auth'

const activeTab = ref('triage')
const authStore = useAuthStore()
/** Must be computed so it updates when /api/auth/me populates permissions after first paint */
const canManageAudits = computed(() => authStore.hasPermission('AUDIT_MANAGEMENT'))
</script>
