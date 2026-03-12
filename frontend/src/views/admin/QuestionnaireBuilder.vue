<template>
  <div>
    <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Questionnaire Builder</h1>
        <p class="text-muted mb-0">Manage controls and questions in one place, then publish from governance.</p>
      </div>
      <router-link :to="{ name: 'AdminQuestionnaireTemplates' }" class="btn btn-outline-primary btn-sm">
        Go to Governance
      </router-link>
    </div>

    <div class="card shadow-sm mb-3 sticky-rail">
      <div class="card-body py-2">
        <div class="d-flex flex-wrap align-items-center gap-2">
          <span class="badge" :class="workflowStepClass(1)">1. Build Content</span>
          <span class="text-muted small">-></span>
          <router-link :to="{ name: 'AdminQuestionnaireTemplates' }" class="badge text-decoration-none" :class="workflowStepClass(2)">
            2. Create Working Snapshot
          </router-link>
          <span class="text-muted small">-></span>
          <router-link :to="{ name: 'AdminQuestionnaireTemplates' }" class="badge text-decoration-none" :class="workflowStepClass(3)">
            3. Review and Publish
          </router-link>
          <span class="small text-muted ms-auto">
            Current tab: <strong>{{ activeTabLabel }}</strong>
          </span>
        </div>
      </div>
    </div>

    <ul class="nav nav-tabs mb-3">
      <li class="nav-item">
        <button
          class="nav-link"
          :class="{ active: activeTab === 'controls' }"
          type="button"
          @click="setTab('controls')"
        >
          Controls
        </button>
      </li>
      <li class="nav-item">
        <button
          class="nav-link"
          :class="{ active: activeTab === 'questions' }"
          type="button"
          @click="setTab('questions')"
        >
          Questions
        </button>
      </li>
    </ul>

    <section v-show="activeTab === 'controls'" class="tab-panel">
      <ControlCatalog embedded />
    </section>
    <section v-show="activeTab === 'questions'" class="tab-panel">
      <QuestionManager embedded />
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ControlCatalog from './ControlCatalog.vue'
import QuestionManager from './QuestionManager.vue'

const route = useRoute()
const router = useRouter()

const activeTab = computed(() => (route.query.tab === 'questions' ? 'questions' : 'controls'))
const activeTabLabel = computed(() => (activeTab.value === 'questions' ? 'Questions' : 'Controls'))

function setTab(tab) {
  router.replace({
    name: 'AdminQuestionnaireBuilder',
    query: {
      ...route.query,
      tab
    }
  })
}

function workflowStepClass(step) {
  if (step === 1) return 'text-bg-primary'
  return 'text-bg-light border text-dark'
}
</script>

<style scoped>
.sticky-rail {
  position: sticky;
  top: 0.5rem;
  z-index: 10;
}

.tab-panel {
  min-height: 300px;
}
</style>
