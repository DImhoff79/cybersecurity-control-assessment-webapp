<template>
  <div>
    <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Questionnaire - Governance</h1>
        <p class="text-muted mb-0">
          Manage the published questionnaire baseline used when new audits are created.
        </p>
      </div>
      <router-link :to="{ name: 'AdminQuestionnaireHub' }" class="btn btn-outline-secondary btn-sm">
        Back to Questionnaire
      </router-link>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
          <h2 class="h5 mb-0">Governance Workflow</h2>
          <span class="badge text-bg-light border">Continuous flow</span>
        </div>
        <p class="small text-muted mb-3">
          Make content updates, create a working snapshot, review what changed, then publish.
        </p>

        <div class="workflow-grid">
          <div class="workflow-step border rounded p-3 bg-light-subtle">
            <div class="small text-muted mb-1">Step 1</div>
            <div class="fw-semibold mb-2">Update Controls & Questions</div>
            <div class="small text-muted mb-3">Keep the live library accurate before creating a new baseline.</div>
            <div class="d-flex flex-wrap gap-2">
              <router-link class="btn btn-outline-primary btn-sm" :to="{ name: 'AdminQuestionnaireBuilder', query: { tab: 'controls', from: 'governance' } }">
                Open Builder (Controls)
              </router-link>
              <router-link class="btn btn-outline-primary btn-sm" :to="{ name: 'AdminQuestionnaireBuilder', query: { tab: 'questions', from: 'governance' } }">
                Open Builder (Questions)
              </router-link>
            </div>
            <div class="small text-muted mt-3">
              <span class="me-3">Enabled controls: <strong>{{ enabledControlsCount }}</strong></span>
              <span>Questions: <strong>{{ questionCount }}</strong></span>
            </div>
          </div>

          <div class="workflow-step border rounded p-3 bg-light-subtle">
            <div class="small text-muted mb-1">Step 2</div>
            <div class="fw-semibold mb-2">Create Working Snapshot</div>
            <div class="small text-muted mb-3">Capture current mappings into a working version for review.</div>
            <input v-model="draftNotes" class="form-control form-control-sm mb-2" placeholder="Change notes (optional)" />
            <div class="d-flex flex-wrap gap-2">
              <button class="btn btn-primary btn-sm" @click="createDraft">Create Working Snapshot</button>
              <button
                v-if="!loading && templates.length === 0"
                class="btn btn-outline-success btn-sm"
                @click="bootstrapInitial"
              >
                Create Initial Baseline
              </button>
            </div>
          </div>

          <div class="workflow-step border rounded p-3 bg-light-subtle">
            <div class="small text-muted mb-1">Step 3</div>
            <div class="fw-semibold mb-2">Review & Publish</div>
            <div class="small text-muted mb-3">Validate template items and publish the version for new audits.</div>
            <div class="small">
              Latest published:
              <strong>{{ latestPublishedLabel }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <div v-if="loading" class="text-muted">Loading...</div>
        <div v-else class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleTemplateSort('versionNo')">Version {{ templateSortIndicator('versionNo') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleTemplateSort('status')">Status {{ templateSortIndicator('status') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleTemplateSort('itemCount')">Items {{ templateSortIndicator('itemCount') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleTemplateSort('createdAt')">Created {{ templateSortIndicator('createdAt') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleTemplateSort('publishedAt')">Published {{ templateSortIndicator('publishedAt') }}</button></th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in sortedTemplates" :key="t.id">
                <td>v{{ t.versionNo }}</td>
                <td><span class="badge" :class="badgeClass(t.status)">{{ statusLabel(t.status) }}</span></td>
                <td>{{ t.itemCount }}</td>
                <td>{{ formatDate(t.createdAt) }}</td>
                <td>{{ formatDate(t.publishedAt) }}</td>
                <td class="text-nowrap">
                  <button class="btn btn-outline-primary btn-sm me-2" @click="viewItems(t)">View Items</button>
                  <button
                    class="btn btn-success btn-sm"
                    :disabled="t.status !== 'DRAFT'"
                    @click="publish(t.id)"
                  >
                    Publish
                  </button>
                  <button
                    v-if="t.status === 'DRAFT'"
                    class="btn btn-outline-danger btn-sm ms-2"
                    @click="deleteSnapshot(t)"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="showItems" title="Template Items" size="xl">
      <div class="table-responsive">
        <table class="table table-sm table-striped mb-0">
          <thead>
            <tr>
              <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleItemSort('controlControlId')">Control {{ itemSortIndicator('controlControlId') }}</button></th>
              <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleItemSort('questionText')">Question {{ itemSortIndicator('questionText') }}</button></th>
              <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleItemSort('mappingWeight')">Weight {{ itemSortIndicator('mappingWeight') }}</button></th>
              <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleItemSort('mappingRationale')">Rationale {{ itemSortIndicator('mappingRationale') }}</button></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in sortedItems" :key="item.id">
              <td>{{ item.controlControlId }}</td>
              <td>{{ item.questionText }}</td>
              <td>{{ item.mappingWeight || '-' }}</td>
              <td>{{ item.mappingRationale || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <button class="btn btn-secondary" @click="showItems = false">Close</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import BsModal from '../../components/BsModal.vue'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

const loading = ref(true)
const templates = ref([])
const draftNotes = ref('')
const showItems = ref(false)
const items = ref([])
const controls = ref([])

const { sortedRows: sortedTemplates, toggleSort: toggleTemplateSort, sortIndicator: templateSortIndicator } = useTableSort(templates, {
  initialKey: 'versionNo',
  initialDirection: 'desc'
})

const { sortedRows: sortedItems, toggleSort: toggleItemSort, sortIndicator: itemSortIndicator } = useTableSort(items, {
  initialKey: 'controlControlId'
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [templateRes, controlRes] = await Promise.all([
      api.get('/api/questionnaire-templates'),
      api.get('/api/controls?includeQuestions=true')
    ])
    templates.value = templateRes.data || []
    controls.value = controlRes.data || []
  } finally {
    loading.value = false
  }
}

async function createDraft() {
  try {
    await api.post('/api/questionnaire-templates/draft-from-current', { notes: draftNotes.value || null })
    toastSuccess('Working snapshot created.')
    draftNotes.value = ''
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create working snapshot')
  }
}

async function bootstrapInitial() {
  try {
    await api.post('/api/questionnaire-templates/bootstrap-initial', {
      notes: draftNotes.value || null
    })
    toastSuccess('Initial baseline template created and published.')
    draftNotes.value = ''
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create initial snapshot')
  }
}

async function publish(templateId) {
  try {
    await api.post(`/api/questionnaire-templates/${templateId}/publish`)
    toastSuccess('Template published.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to publish template')
  }
}

async function deleteSnapshot(template) {
  if (!window.confirm(`Delete working snapshot v${template.versionNo}?`)) return
  try {
    await api.delete(`/api/questionnaire-templates/${template.id}`)
    toastSuccess('Working snapshot deleted.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete working snapshot')
  }
}

async function viewItems(template) {
  try {
    const res = await api.get(`/api/questionnaire-templates/${template.id}/items`)
    items.value = res.data || []
    showItems.value = true
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load template items')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function badgeClass(status) {
  if (status === 'PUBLISHED') return 'text-bg-success'
  if (status === 'DRAFT') return 'text-bg-warning'
  return 'text-bg-secondary'
}

function statusLabel(status) {
  if (status === 'DRAFT') return 'WORKING'
  return status
}

const enabledControlsCount = computed(() => controls.value.filter((c) => c.enabled).length)

const questionCount = computed(() => {
  const ids = new Set()
  controls.value.forEach((c) => {
    ;(c.questions || []).forEach((q) => ids.add(q.id))
  })
  return ids.size
})

const latestPublishedLabel = computed(() => {
  const published = templates.value.find((t) => t.status === 'PUBLISHED')
  if (!published) return 'None yet'
  return `v${published.versionNo} (${formatDate(published.publishedAt)})`
})
</script>

<style scoped>
.workflow-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.workflow-step {
  min-height: 180px;
}
</style>
