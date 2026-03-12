<template>
  <div>
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Operations Queue</h1>
        <p class="text-muted mb-0">
          Triage submitted audits, evidence reviews, and activity from one execution workspace.
        </p>
      </div>
    </div>

    <ul class="nav nav-pills mb-3">
      <li class="nav-item">
        <button type="button" class="nav-link" :class="{ active: activeTab === 'triage' }" @click="activeTab = 'triage'">
          Triage Hub
        </button>
      </li>
      <li v-if="canManageAudits" class="nav-item">
        <button type="button" class="nav-link" :class="{ active: activeTab === 'reviews' }" @click="activeTab = 'reviews'">
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
import { ref } from 'vue'
import AuditorWorkbench from './AuditorWorkbench.vue'
import ReviewQueue from './ReviewQueue.vue'
import { useAuthStore } from '../../stores/auth'

const activeTab = ref('triage')
const authStore = useAuthStore()
const canManageAudits = authStore.hasPermission('AUDIT_MANAGEMENT')
</script>
