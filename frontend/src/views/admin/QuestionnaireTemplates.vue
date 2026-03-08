<template>
  <div>
    <h1 class="h3 mb-3">Questionnaire Templates</h1>
    <div class="alert alert-info">
      <div class="fw-semibold mb-1">What this does</div>
      <div class="small">
        Templates freeze your current question-control mappings into versioned releases. New audits snapshot from the latest published template so in-flight audits stay stable even if live mappings change later.
      </div>
      <div class="small mt-2">
        Recommended flow: 1) update controls/questions/mappings, 2) create draft from current, 3) review items, 4) publish, 5) kickoff audits.
      </div>
    </div>
    <div class="card shadow-sm mb-3">
      <div class="card-body d-flex gap-2 align-items-center">
        <input v-model="draftNotes" class="form-control" placeholder="Draft notes (optional)" />
        <button class="btn btn-primary" @click="createDraft">Create Draft from Current</button>
        <button
          v-if="!loading && templates.length === 0"
          class="btn btn-outline-success"
          @click="bootstrapInitial"
        >
          Create Initial Snapshot
        </button>
      </div>
    </div>
    <div class="card shadow-sm">
      <div class="card-body">
        <div v-if="loading" class="text-muted">Loading...</div>
        <div v-else class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th>Version</th>
                <th>Status</th>
                <th>Items</th>
                <th>Created</th>
                <th>Published</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in templates" :key="t.id">
                <td>v{{ t.versionNo }}</td>
                <td><span class="badge" :class="badgeClass(t.status)">{{ t.status }}</span></td>
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
              <th>Control</th>
              <th>Question</th>
              <th>Weight</th>
              <th>Rationale</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
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
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import BsModal from '../../components/BsModal.vue'
import { toastError, toastSuccess } from '../../services/toast'

const loading = ref(true)
const templates = ref([])
const draftNotes = ref('')
const showItems = ref(false)
const items = ref([])

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/questionnaire-templates')
    templates.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function createDraft() {
  try {
    await api.post('/api/questionnaire-templates/draft-from-current', { notes: draftNotes.value || null })
    toastSuccess('Draft template created.')
    draftNotes.value = ''
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create draft')
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
</script>
