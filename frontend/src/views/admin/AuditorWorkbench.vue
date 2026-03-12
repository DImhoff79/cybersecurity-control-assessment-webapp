<template>
  <div>
    <h1 class="h3 mb-3">Auditor Workbench</h1>
    <div class="alert alert-info py-2">
      Use this page to triage active audits and evidence: filter queues, send reminders, attest submitted audits, and review pending evidence from one place.
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else>
      <div v-if="loadError" class="alert alert-danger py-2 d-flex justify-content-between align-items-center gap-2">
        <span>{{ loadError }}</span>
        <button type="button" class="btn btn-outline-danger btn-sm" @click="load">Retry</button>
      </div>
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
                <button class="btn btn-outline-secondary btn-sm" @click="resetAuditFilters">Reset</button>
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
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('applicationName')">Application {{ auditSortIndicator('applicationName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('projectName')">Project {{ auditSortIndicator('projectName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('year')">Year {{ auditSortIndicator('year') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('status')">Status {{ auditSortIndicator('status') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('assignedToEmail')">Assigned {{ auditSortIndicator('assignedToEmail') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('frameworks')">Frameworks {{ auditSortIndicator('frameworks') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('dueAt')">Due {{ auditSortIndicator('dueAt') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAuditSort('pendingEvidenceCount')">Pending Evidence {{ auditSortIndicator('pendingEvidenceCount') }}</button></th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in sortedAudits" :key="a.auditId">
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
                <tr v-if="!sortedAudits.length">
                  <td colspan="9" class="text-muted text-center py-3">No audits match current filters.</td>
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
            <div class="col-md-12 d-flex justify-content-end">
              <button class="btn btn-outline-secondary btn-sm" @click="resetEvidenceFilters">Reset evidence filters</button>
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
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('applicationName')">Application {{ evidenceSortIndicator('applicationName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('projectName')">Project {{ evidenceSortIndicator('projectName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('controlControlId')">Control {{ evidenceSortIndicator('controlControlId') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('framework')">Framework {{ evidenceSortIndicator('framework') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('fileName')">File {{ evidenceSortIndicator('fileName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('notes')">Description {{ evidenceSortIndicator('notes') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('expiresAt')">Expires {{ evidenceSortIndicator('expiresAt') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEvidenceSort('createdAt')">Created {{ evidenceSortIndicator('createdAt') }}</button></th>
                  <th>Download</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in sortedEvidence" :key="e.evidenceId">
                  <td>{{ e.applicationName }} ({{ e.year }})</td>
                  <td>{{ e.projectName || '-' }}</td>
                  <td>{{ e.controlControlId }}</td>
                  <td>{{ e.framework || '-' }}</td>
                  <td>{{ e.fileName || '-' }}</td>
                  <td>{{ e.notes || '-' }}</td>
                  <td>
                    <span v-if="e.stale" class="badge text-bg-danger me-1">Stale</span>
                    {{ formatDateTime(e.expiresAt) }}
                  </td>
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
                <tr v-if="!sortedEvidence.length">
                  <td colspan="10" class="text-muted text-center py-3">No evidence items match current filters.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card shadow-sm mt-3">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
            <h2 class="h5 mb-0">Recent Activity Feed</h2>
            <div class="d-flex gap-2 align-items-center flex-wrap">
              <button class="btn btn-outline-primary btn-sm" @click="exportRecentActivityCsv">Export CSV</button>
              <select v-model="activityFilter.projectId" class="form-select form-select-sm">
                <option value="all">All projects</option>
                <option value="none">No project (legacy)</option>
                <option v-for="p in projectOptions" :key="`act-${p.id}`" :value="String(p.id)">{{ p.name }}</option>
              </select>
              <select v-model="activityFilter.category" class="form-select form-select-sm">
                <option value="all">All events</option>
                <option value="finding">Findings</option>
                <option value="exception">Exceptions</option>
                <option value="evidence">Evidence</option>
                <option value="audit">Audit workflow</option>
              </select>
              <input v-model="activityFilter.search" class="form-control form-control-sm" placeholder="Search details or actor..." />
              <button class="btn btn-outline-secondary btn-sm" @click="resetActivityFilters">Reset</button>
            </div>
          </div>
          <div class="small text-muted mb-2">
            Showing {{ filteredActivity.length }} of {{ (dashboard.recentActivity || []).length }} events
          </div>
          <div class="table-responsive">
            <table class="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleActivitySort('createdAt')">When {{ activitySortIndicator('createdAt') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleActivitySort('applicationName')">Application {{ activitySortIndicator('applicationName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleActivitySort('projectName')">Project {{ activitySortIndicator('projectName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleActivitySort('activityType')">Type {{ activitySortIndicator('activityType') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleActivitySort('details')">Details {{ activitySortIndicator('details') }}</button></th>
                  <th>Actor</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="event in sortedActivity" :key="event.id">
                  <td>{{ formatDateTime(event.createdAt) }}</td>
                  <td>{{ event.applicationName }} ({{ event.year }})</td>
                  <td>{{ event.projectName || '-' }}</td>
                  <td><span class="badge text-bg-light border">{{ event.activityType }}</span></td>
                  <td>{{ event.details || '-' }}</td>
                  <td>{{ event.actorEmail || 'system' }}</td>
                  <td class="text-nowrap">
                    <router-link :to="`/admin/audits/${event.auditId}`" class="btn btn-outline-primary btn-sm">Open Audit</router-link>
                  </td>
                </tr>
                <tr v-if="!sortedActivity.length">
                  <td colspan="7" class="text-muted text-center py-3">No activity events match current filters.</td>
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
import { useTableSort } from '../../composables/useTableSort'

const loading = ref(true)
const loadError = ref('')
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
const activityFilter = reactive({
  projectId: 'all',
  category: 'all',
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
  loadError.value = ''
  try {
    const [res, meRes] = await Promise.all([
      api.get('/api/reports/auditor-dashboard'),
      api.get('/api/auth/me')
    ])
    dashboard.value = res.data || { summary: {}, auditsNeedingAttention: [], evidenceQueue: [] }
    currentUserEmail.value = meRes.data?.email || ''
    await loadSavedFilters()
  } catch (e) {
    loadError.value = e.response?.data?.error || 'Failed to load auditor dashboard data.'
    toastError(loadError.value)
  } finally {
    loading.value = false
  }
}

function resetAuditFilters() {
  Object.assign(auditFilter, {
    queue: 'all',
    status: 'all',
    projectId: 'all',
    framework: 'all',
    search: ''
  })
}

function resetEvidenceFilters() {
  Object.assign(evidenceFilter, {
    projectId: 'all',
    framework: 'all',
    search: ''
  })
}

function resetActivityFilters() {
  Object.assign(activityFilter, {
    projectId: 'all',
    category: 'all',
    search: ''
  })
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

const filteredActivity = computed(() => {
  const term = activityFilter.search.trim().toLowerCase()
  return (dashboard.value.recentActivity || []).filter((event) => {
    if (activityFilter.projectId === 'none' && event.projectId) return false
    if (activityFilter.projectId !== 'all' && activityFilter.projectId !== 'none' && String(event.projectId) !== activityFilter.projectId) return false
    const type = String(event.activityType || '')
    if (activityFilter.category !== 'all') {
      const matchesCategory =
        (activityFilter.category === 'finding' && type.startsWith('FINDING_')) ||
        (activityFilter.category === 'exception' && type.startsWith('EXCEPTION_')) ||
        (activityFilter.category === 'evidence' && type.startsWith('EVIDENCE_')) ||
        (activityFilter.category === 'audit' && type.startsWith('AUDIT_'))
      if (!matchesCategory) return false
    }
    if (!term) return true
    const haystack = `${event.applicationName} ${event.projectName || ''} ${type} ${event.details || ''} ${event.actorEmail || ''}`.toLowerCase()
    return haystack.includes(term)
  })
})

const { sortedRows: sortedAudits, toggleSort: toggleAuditSort, sortIndicator: auditSortIndicator } = useTableSort(filteredAudits, {
  initialKey: 'dueAt'
})

const { sortedRows: sortedEvidence, toggleSort: toggleEvidenceSort, sortIndicator: evidenceSortIndicator } = useTableSort(filteredEvidence, {
  initialKey: 'createdAt',
  initialDirection: 'desc'
})

const { sortedRows: sortedActivity, toggleSort: toggleActivitySort, sortIndicator: activitySortIndicator } = useTableSort(filteredActivity, {
  initialKey: 'createdAt',
  initialDirection: 'desc'
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
        evidenceFilter: { ...evidenceFilter },
        activityFilter: { ...activityFilter }
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
  Object.assign(activityFilter, state.activityFilter || {})
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

async function exportRecentActivityCsv() {
  try {
    const params = {}
    if (activityFilter.category && activityFilter.category !== 'all') {
      params.category = activityFilter.category
    }
    if (activityFilter.search?.trim()) {
      params.search = activityFilter.search.trim()
    }
    if (activityFilter.projectId && activityFilter.projectId !== 'all' && activityFilter.projectId !== 'none') {
      params.projectId = Number(activityFilter.projectId)
    }
    if (activityFilter.projectId === 'none') {
      params.noProjectOnly = true
    }
    const response = await api.get('/api/reports/recent-activity.csv', { params, responseType: 'blob' })
    const blob = new Blob([response.data], { type: 'text/csv' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'recent-activity.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to export recent activity CSV')
  }
}

watch([auditFilter, evidenceFilter, activityFilter], () => {
  // Keep UI reactive and deterministic for saved-filter snapshots.
}, { deep: true })
</script>
