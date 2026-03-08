<template>
  <div>
    <h1 class="h3 mb-3">Auditor Workbench</h1>
    <div class="alert alert-info py-2">
      Use this page to triage active audits and evidence: filter queues, send reminders, attest submitted audits, and review pending evidence from one place.
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else>
      <div class="row g-3 mb-3">
        <div class="col-md-3" v-for="card in cards" :key="card.label">
          <div class="card shadow-sm h-100">
            <div class="card-body">
              <div class="text-muted small">{{ card.label }}</div>
              <div class="h3 mb-0">{{ card.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <div class="row g-2 mb-3">
            <div class="col-md-2">
              <label class="form-label small mb-1">Queue</label>
              <select v-model="auditFilter.queue" class="form-select form-select-sm">
                <option value="all">All</option>
                <option value="unassigned">Unassigned</option>
                <option value="mine">Assigned To Me</option>
                <option value="others">Assigned To Others</option>
                <option value="overdue">Overdue</option>
              </select>
            </div>
            <div class="col-md-2">
              <label class="form-label small mb-1">Status</label>
              <select v-model="auditFilter.status" class="form-select form-select-sm">
                <option value="all">All</option>
                <option value="DRAFT">DRAFT</option>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="SUBMITTED">SUBMITTED</option>
                <option value="ATTESTED">ATTESTED</option>
                <option value="COMPLETE">COMPLETE</option>
              </select>
            </div>
            <div class="col-md-2">
              <label class="form-label small mb-1">Project</label>
              <select v-model="auditFilter.projectId" class="form-select form-select-sm" data-testid="audit-project-filter">
                <option value="all">All</option>
                <option value="none">No project (legacy)</option>
                <option v-for="p in projectOptions" :key="`ap-${p.id}`" :value="String(p.id)">{{ p.name }}</option>
              </select>
            </div>
            <div class="col-md-2">
              <label class="form-label small mb-1">Framework</label>
              <select v-model="auditFilter.framework" class="form-select form-select-sm">
                <option value="all">All</option>
                <option v-for="f in frameworkOptions" :key="`af-${f}`" :value="f">{{ f }}</option>
              </select>
            </div>
            <div class="col-md-2">
              <label class="form-label small mb-1">Search</label>
              <input v-model="auditFilter.search" class="form-control form-control-sm" placeholder="Application, assignee..." />
            </div>
            <div class="col-md-4">
              <label class="form-label small mb-1">Saved Filter</label>
              <div class="d-flex gap-1 align-items-center">
                <input v-model="filterName" class="form-control form-control-sm" placeholder="Filter name" />
                <div class="form-check form-check-inline m-0">
                  <input id="shareFilter" v-model="shareFilter" class="form-check-input" type="checkbox" />
                  <label for="shareFilter" class="form-check-label small">Share</label>
                </div>
                <button class="btn btn-outline-primary btn-sm" @click="saveCurrentFilter">Save</button>
              </div>
            </div>
            <div class="col-12" v-if="savedFilters.length">
              <div class="d-flex flex-wrap gap-2">
                <button
                  v-for="f in savedFilters"
                  :key="f.id"
                  class="btn btn-outline-secondary btn-sm"
                  @click="loadSavedFilter(f.id)"
                >
                  {{ f.name }} <span v-if="f.shared">[shared]</span>
                </button>
                <button class="btn btn-outline-danger btn-sm" @click="clearSavedFilters">Clear Saved</button>
              </div>
            </div>
          </div>

          <h2 class="h5 mb-3">Audits Needing Attention</h2>
          <div class="small text-muted mb-2">
            Showing {{ filteredAudits.length }} of {{ (dashboard.auditsNeedingAttention || []).length }} audits
          </div>
          <div class="table-responsive">
            <table class="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>Application</th>
                  <th>Project</th>
                  <th>Year</th>
                  <th>Status</th>
                  <th>Assigned</th>
                  <th>Frameworks</th>
                  <th>Due</th>
                  <th>Pending Evidence</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in filteredAudits" :key="a.auditId">
                  <td>{{ a.applicationName }}</td>
                  <td>{{ a.projectName || '-' }}</td>
                  <td>{{ a.year }}</td>
                  <td><span class="badge status-badge" :class="statusBadge(a.status)">{{ a.status }}</span></td>
                  <td>{{ a.assignedToEmail || '-' }}</td>
                  <td>{{ a.frameworks || '-' }}</td>
                  <td>{{ formatDate(a.dueAt) }}</td>
                  <td>{{ a.pendingEvidenceCount }}</td>
                  <td class="text-nowrap">
                    <router-link :to="`/admin/audits/${a.auditId}`" class="btn btn-outline-primary btn-sm me-2">Open</router-link>
                    <button class="btn btn-outline-primary btn-sm me-2" @click="remind(a.auditId)">Remind</button>
                    <button class="btn btn-outline-success btn-sm" :disabled="a.status !== 'SUBMITTED' && a.status !== 'ATTESTED'" @click="attest(a.auditId)">Attest</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card shadow-sm">
        <div class="card-body">
          <div class="row g-2 mb-3">
            <div class="col-md-3">
              <label class="form-label small mb-1">Project</label>
              <select v-model="evidenceFilter.projectId" class="form-select form-select-sm" data-testid="evidence-project-filter">
                <option value="all">All</option>
                <option value="none">No project (legacy)</option>
                <option v-for="p in projectOptions" :key="`ep-${p.id}`" :value="String(p.id)">{{ p.name }}</option>
              </select>
            </div>
            <div class="col-md-3">
              <label class="form-label small mb-1">Framework</label>
              <select v-model="evidenceFilter.framework" class="form-select form-select-sm">
                <option value="all">All</option>
                <option v-for="f in frameworkOptions" :key="`ef-${f}`" :value="f">{{ f }}</option>
              </select>
            </div>
            <div class="col-md-6">
              <label class="form-label small mb-1">Search</label>
              <input v-model="evidenceFilter.search" class="form-control form-control-sm" placeholder="Application, control, file, description..." />
            </div>
          </div>
          <h2 class="h5 mb-3">Evidence Review Queue</h2>
          <div class="small text-muted mb-2">
            Showing {{ filteredEvidence.length }} of {{ (dashboard.evidenceQueue || []).length }} documents
          </div>
          <div class="table-responsive">
            <table class="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>Application</th>
                  <th>Project</th>
                  <th>Control</th>
                  <th>Framework</th>
                  <th>File</th>
                  <th>Description</th>
                  <th>Created</th>
                  <th>Download</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in filteredEvidence" :key="e.evidenceId">
                  <td>{{ e.applicationName }} ({{ e.year }})</td>
                  <td>{{ e.projectName || '-' }}</td>
                  <td>{{ e.controlControlId }}</td>
                  <td>{{ e.framework || '-' }}</td>
                  <td>{{ e.fileName || '-' }}</td>
                  <td>{{ e.notes || '-' }}</td>
                  <td>{{ formatDateTime(e.createdAt) }}</td>
                  <td>
                    <a v-if="e.uri" :href="e.uri" target="_blank" rel="noopener noreferrer">Download</a>
                    <span v-else>-</span>
                  </td>
                  <td class="text-nowrap">
                    <button class="btn btn-outline-success btn-sm me-2" @click="reviewEvidence(e.evidenceId, 'ACCEPTED')">Accept</button>
                    <button class="btn btn-outline-danger btn-sm" @click="reviewEvidence(e.evidenceId, 'REJECTED')">Reject</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const loading = ref(true)
const dashboard = ref({ summary: {}, auditsNeedingAttention: [], evidenceQueue: [] })
const currentUserEmail = ref('')
const filterName = ref('')
const shareFilter = ref(true)
const savedFilters = ref([])
const auditFilter = reactive({
  queue: 'all',
  status: 'all',
  projectId: 'all',
  framework: 'all',
  search: ''
})
const evidenceFilter = reactive({
  projectId: 'all',
  framework: 'all',
  search: ''
})

const cards = computed(() => {
  const s = dashboard.value.summary || {}
  return [
    { label: 'Total Audits', value: s.totalAudits ?? 0 },
    { label: 'Open Audits', value: s.openAudits ?? 0 },
    { label: 'Overdue Audits', value: s.overdueAudits ?? 0 },
    { label: 'Submitted', value: s.submittedAudits ?? 0 }
  ]
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [res, meRes] = await Promise.all([
      api.get('/api/reports/auditor-dashboard'),
      api.get('/api/auth/me')
    ])
    dashboard.value = res.data || { summary: {}, auditsNeedingAttention: [], evidenceQueue: [] }
    currentUserEmail.value = meRes.data?.email || ''
    await loadSavedFilters()
  } finally {
    loading.value = false
  }
}

async function remind(auditId) {
  try {
    await api.post(`/api/audits/${auditId}/remind`)
    await load()
    toastSuccess('Reminder sent.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to send reminder')
  }
}

async function attest(auditId) {
  try {
    await api.post(`/api/audits/${auditId}/attest`, { statement: 'Attested from Auditor Workbench.' })
    await load()
    toastSuccess('Audit attested.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to attest audit')
  }
}

async function reviewEvidence(evidenceId, reviewStatus) {
  try {
    await api.put(`/api/evidences/${evidenceId}/review`, { reviewStatus })
    await load()
    toastSuccess(`Evidence ${reviewStatus === 'ACCEPTED' ? 'accepted' : 'rejected'}.`)
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to review evidence')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function statusBadge(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'text-bg-info'
    case 'ATTESTED':
      return 'text-bg-primary'
    case 'IN_PROGRESS':
      return 'text-bg-warning'
    case 'COMPLETE':
      return 'text-bg-success'
    default:
      return 'text-bg-secondary'
  }
}

const frameworkOptions = computed(() => {
  const options = new Set()
  ;(dashboard.value.auditsNeedingAttention || []).forEach((a) => {
    ;(a.frameworks || '')
      .split(',')
      .map((x) => x.trim())
      .filter(Boolean)
      .forEach((f) => options.add(f))
  })
  ;(dashboard.value.evidenceQueue || []).forEach((e) => {
    if (e.framework) options.add(e.framework)
  })
  return Array.from(options).sort()
})

const projectOptions = computed(() => {
  const byId = new Map()
  ;(dashboard.value.auditsNeedingAttention || []).forEach((a) => {
    if (a.projectId && !byId.has(a.projectId)) byId.set(a.projectId, a.projectName || `Project ${a.projectId}`)
  })
  ;(dashboard.value.evidenceQueue || []).forEach((e) => {
    if (e.projectId && !byId.has(e.projectId)) byId.set(e.projectId, e.projectName || `Project ${e.projectId}`)
  })
  return Array.from(byId.entries())
    .map(([id, name]) => ({ id, name }))
    .sort((a, b) => a.name.localeCompare(b.name))
})

const filteredAudits = computed(() => {
  const now = Date.now()
  return (dashboard.value.auditsNeedingAttention || []).filter((a) => {
    if (auditFilter.queue === 'unassigned' && !!a.assignedToEmail) return false
    if (auditFilter.queue === 'mine' && a.assignedToEmail !== currentUserEmail.value) return false
    if (auditFilter.queue === 'others' && (!a.assignedToEmail || a.assignedToEmail === currentUserEmail.value)) return false
    if (auditFilter.queue === 'overdue') {
      if (!a.dueAt) return false
      if (new Date(a.dueAt).getTime() >= now) return false
    }
    if (auditFilter.status !== 'all' && a.status !== auditFilter.status) return false
    if (auditFilter.projectId === 'none' && a.projectId) return false
    if (auditFilter.projectId !== 'all' && auditFilter.projectId !== 'none' && String(a.projectId) !== auditFilter.projectId) return false
    if (auditFilter.framework !== 'all' && !(a.frameworks || '').includes(auditFilter.framework)) return false
    const haystack = `${a.applicationName} ${a.projectName || ''} ${a.assignedToEmail || ''} ${a.status} ${a.frameworks || ''}`.toLowerCase()
    const term = auditFilter.search.trim().toLowerCase()
    if (term && !haystack.includes(term)) return false
    return true
  })
})

const filteredEvidence = computed(() => {
  return (dashboard.value.evidenceQueue || []).filter((e) => {
    if (evidenceFilter.projectId === 'none' && e.projectId) return false
    if (evidenceFilter.projectId !== 'all' && evidenceFilter.projectId !== 'none' && String(e.projectId) !== evidenceFilter.projectId) return false
    if (evidenceFilter.framework !== 'all' && e.framework !== evidenceFilter.framework) return false
    const term = evidenceFilter.search.trim().toLowerCase()
    if (!term) return true
    const haystack = `${e.applicationName} ${e.projectName || ''} ${e.controlControlId} ${e.controlName} ${e.fileName || ''} ${e.title || ''} ${e.notes || ''}`.toLowerCase()
    return haystack.includes(term)
  })
})

async function loadSavedFilters() {
  try {
    const res = await api.get('/api/reports/saved-filters')
    savedFilters.value = res.data || []
  } catch {
    savedFilters.value = []
  }
}

async function saveCurrentFilter() {
  const name = filterName.value.trim()
  if (!name) {
    toastError('Enter a filter name first.')
    return
  }
  try {
    await api.post('/api/reports/saved-filters', {
      name,
      shared: shareFilter.value,
      filterState: {
        auditFilter: { ...auditFilter },
        evidenceFilter: { ...evidenceFilter }
      }
    })
    filterName.value = ''
    await loadSavedFilters()
    toastSuccess('Filter saved.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save filter')
  }
}

function loadSavedFilter(id) {
  const found = savedFilters.value.find((f) => f.id === id)
  if (!found) return
  const state = found.filterState || {}
  Object.assign(auditFilter, state.auditFilter || {})
  Object.assign(evidenceFilter, state.evidenceFilter || {})
  toastSuccess(`Loaded filter "${found.name}".`)
}

async function clearSavedFilters() {
  try {
    await Promise.all((savedFilters.value || []).map((f) => api.delete(`/api/reports/saved-filters/${f.id}`)))
    await loadSavedFilters()
    toastSuccess('Saved filters cleared.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to clear saved filters')
  }
}

watch([auditFilter, evidenceFilter], () => {
  // Keep UI reactive and deterministic for saved-filter snapshots.
}, { deep: true })
</script>
