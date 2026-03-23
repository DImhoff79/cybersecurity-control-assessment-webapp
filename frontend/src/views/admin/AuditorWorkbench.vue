<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">Triage</h1>
      <p class="text-muted mb-0">Snapshot of program health and what needs attention next.</p>
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else>
      <div v-if="loadError" class="alert alert-danger py-2 d-flex justify-content-between align-items-center gap-2">
        <span>{{ loadError }}</span>
        <button type="button" class="btn btn-outline-danger btn-sm" @click="load">Retry</button>
      </div>

      <!-- How we're doing -->
      <div class="row g-2 mb-2">
        <div class="col-6 col-lg-3" v-for="card in cards" :key="card.label">
          <div
            class="card shadow-sm h-100"
            :class="{
              'border-danger': card.variant === 'danger',
              'border-warning': card.variant === 'warning'
            }"
          >
            <div class="card-body py-3">
              <div class="text-muted small">{{ card.label }}</div>
              <div class="h3 mb-0">{{ card.value }}</div>
            </div>
          </div>
        </div>
      </div>
      <p class="small text-muted mb-3 border-start border-3 ps-2" :class="healthBorderClass">{{ healthMessage }}</p>

      <!-- Audits -->
      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <h2 class="h5 mb-2">Audits needing attention</h2>
          <p v-if="isAuditorSession" class="small text-muted mb-3">
            Defaults to <strong>your assignments</strong> and <strong>all open items</strong>. Adjust below anytime.
          </p>
          <div class="audit-filter-toolbar border rounded-3 bg-light p-3 mb-3">
            <div class="row g-2 align-items-end">
              <div class="col-sm-6 col-lg-3">
                <label class="form-label small text-muted mb-1">Assignment</label>
                <select v-model="auditFilter.queue" class="form-select form-select-sm" data-testid="audit-queue-filter">
                  <option value="all">Everyone</option>
                  <option value="mine">Assigned to me</option>
                  <option value="unassigned">Unassigned</option>
                  <option value="others">Assigned to others</option>
                  <option value="overdue">Overdue</option>
                </select>
              </div>
              <div class="col-sm-6 col-lg-3">
                <label class="form-label small text-muted mb-1">Stage</label>
                <select
                  v-model="auditFilter.statusPreset"
                  class="form-select form-select-sm"
                  data-testid="audit-status-preset"
                  title="Draft through Attested — excludes audits already in the Complete stage."
                >
                  <option value="active">All open items</option>
                  <option value="all">All stages</option>
                  <option value="DRAFT">Draft</option>
                  <option value="IN_PROGRESS">In progress</option>
                  <option value="SUBMITTED">Submitted</option>
                  <option value="ATTESTED">Attested</option>
                  <option value="COMPLETE">Complete</option>
                </select>
              </div>
              <div class="col-lg-6">
                <label class="form-label small text-muted mb-1">Search</label>
                <input
                  v-model="auditFilter.search"
                  class="form-control form-control-sm"
                  type="search"
                  placeholder="Application, assignee…"
                  autocomplete="off"
                />
              </div>
            </div>
          </div>

          <details class="mb-3 border rounded px-2 py-1 bg-light">
            <summary class="small fw-semibold py-1 user-select-none">More filters &amp; saved views</summary>
            <div class="row g-2 pt-2 pb-1">
              <div class="col-md-3">
                <label class="form-label small mb-1">Project</label>
                <select v-model="auditFilter.projectId" class="form-select form-select-sm" data-testid="audit-project-filter">
                  <option value="all">All</option>
                  <option value="none">No project (legacy)</option>
                  <option v-for="p in projectOptions" :key="`ap-${p.id}`" :value="String(p.id)">{{ p.name }}</option>
                </select>
              </div>
              <div class="col-md-3">
                <label class="form-label small mb-1">Framework</label>
                <select v-model="auditFilter.framework" class="form-select form-select-sm">
                  <option value="all">All</option>
                  <option v-for="f in frameworkOptions" :key="`af-${f}`" :value="f">{{ f }}</option>
                </select>
              </div>
              <div class="col-md-6">
                <label class="form-label small mb-1">Saved filter</label>
                <div class="d-flex gap-1 flex-wrap align-items-center">
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
          </details>

          <div class="small text-muted mb-2">
            Showing {{ filteredAudits.length }} of {{ statusScopedAuditsCount }}
          </div>
          <div class="table-responsive">
            <table class="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>Application</th>
                  <th>Status</th>
                  <th>Due</th>
                  <th>Assignee</th>
                  <th class="text-nowrap">Pending evidence</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in sortedAudits" :key="a.auditId">
                  <td>
                    <div class="fw-medium">{{ a.applicationName }}</div>
                    <div class="small text-muted">{{ a.year }} · {{ a.projectName || 'No project' }}</div>
                  </td>
                  <td>
                    <span class="badge status-badge" :class="statusBadge(a.status)">{{ auditStatusLabel(a.status) }}</span>
                    <div class="small text-muted mt-1">{{ auditStageLabel(a.status) }}</div>
                  </td>
                  <td>
                    <span v-if="isAuditOverdue(a)" class="badge text-bg-danger me-1">Overdue</span>
                    {{ formatDate(a.dueAt) }}
                  </td>
                  <td>{{ a.assignedToEmail || '—' }}</td>
                  <td>{{ a.pendingEvidenceCount }}</td>
                  <td class="text-nowrap">
                    <router-link :to="`/admin/audits/${a.auditId}`" class="btn btn-outline-primary btn-sm me-1">Open</router-link>
                    <button
                      class="btn btn-outline-primary btn-sm me-1"
                      :disabled="!canManageAudits"
                      :title="!canManageAudits ? 'Requires AUDIT_MANAGEMENT permission' : ''"
                      @click="remind(a.auditId)"
                    >
                      Remind
                    </button>
                    <button
                      class="btn btn-outline-success btn-sm"
                      :disabled="!canManageAudits || (a.status !== 'SUBMITTED' && a.status !== 'ATTESTED')"
                      :title="!canManageAudits ? 'Requires AUDIT_MANAGEMENT permission' : ''"
                      @click="attest(a.auditId)"
                    >
                      Attest
                    </button>
                  </td>
                </tr>
                <tr v-if="!sortedAudits.length">
                  <td colspan="6" class="text-muted text-center py-3">No audits match current filters.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Evidence -->
      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <h2 class="h5 mb-3">Evidence to review</h2>
          <div class="d-flex flex-wrap gap-2 mb-3">
            <div class="flex-grow-1" style="min-width: 200px">
              <label class="form-label small mb-1">Search</label>
              <input v-model="evidenceFilter.search" class="form-control form-control-sm" placeholder="Application, control, file…" />
            </div>
          </div>
          <details class="mb-3 border rounded px-2 py-1 bg-light">
            <summary class="small fw-semibold py-1 user-select-none">More evidence filters</summary>
            <div class="row g-2 pt-2 pb-1">
              <div class="col-md-4">
                <label class="form-label small mb-1">Project</label>
                <select v-model="evidenceFilter.projectId" class="form-select form-select-sm" data-testid="evidence-project-filter">
                  <option value="all">All</option>
                  <option value="none">No project (legacy)</option>
                  <option v-for="p in projectOptions" :key="`ep-${p.id}`" :value="String(p.id)">{{ p.name }}</option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="form-label small mb-1">Framework</label>
                <select v-model="evidenceFilter.framework" class="form-select form-select-sm">
                  <option value="all">All</option>
                  <option v-for="f in frameworkOptions" :key="`ef-${f}`" :value="f">{{ f }}</option>
                </select>
              </div>
              <div class="col-md-4 d-flex align-items-end">
                <button class="btn btn-outline-secondary btn-sm" @click="resetEvidenceFilters">Reset filters</button>
              </div>
            </div>
          </details>

          <div class="small text-muted mb-2">
            Showing {{ filteredEvidence.length }} of {{ (dashboard.evidenceQueue || []).length }}
          </div>
          <div class="table-responsive">
            <table class="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>Application</th>
                  <th>Control</th>
                  <th>File</th>
                  <th>Expires</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in sortedEvidence" :key="e.evidenceId">
                  <td>
                    <div class="fw-medium">{{ e.applicationName }}</div>
                    <div class="small text-muted">{{ e.year }} · {{ e.projectName || 'No project' }}</div>
                  </td>
                  <td>
                    <span class="font-monospace small">{{ e.controlControlId }}</span>
                    <div class="small text-muted">{{ e.framework || '—' }}</div>
                  </td>
                  <td>{{ e.fileName || e.title || '—' }}</td>
                  <td>
                    <span v-if="e.stale" class="badge text-bg-danger me-1">Stale</span>
                    {{ formatDateTime(e.expiresAt) }}
                  </td>
                  <td class="text-nowrap">
                    <button
                      type="button"
                      class="btn btn-outline-secondary btn-sm me-1"
                      :disabled="!e.uri || e.lifecycleStatus === 'DISPOSED'"
                      @click="downloadEvidence(e)"
                    >
                      Download
                    </button>
                    <button
                      class="btn btn-outline-success btn-sm me-1"
                      :disabled="!canManageAudits"
                      :title="!canManageAudits ? 'Requires AUDIT_MANAGEMENT permission' : ''"
                      @click="reviewEvidence(e.evidenceId, 'ACCEPTED')"
                    >
                      Accept
                    </button>
                    <button
                      class="btn btn-outline-danger btn-sm me-1"
                      :disabled="!canManageAudits"
                      :title="!canManageAudits ? 'Requires AUDIT_MANAGEMENT permission' : ''"
                      @click="reviewEvidence(e.evidenceId, 'REJECTED')"
                    >
                      Reject
                    </button>
                    <button
                      class="btn btn-outline-secondary btn-sm"
                      :disabled="!canManageAudits || e.lifecycleStatus === 'ARCHIVED' || e.lifecycleStatus === 'DISPOSED' || e.legalHold"
                      :title="!canManageAudits ? 'Requires AUDIT_MANAGEMENT permission' : ''"
                      @click="archiveEvidence(e.evidenceId)"
                    >
                      Archive
                    </button>
                  </td>
                </tr>
                <tr v-if="!sortedEvidence.length">
                  <td colspan="5" class="text-muted text-center py-3">No evidence items match current filters.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Activity (collapsed by default) -->
      <details class="card shadow-sm" data-testid="recent-activity-panel">
        <summary class="card-header user-select-none mb-0 list-group-item-action py-3">
          Recent activity · {{ (dashboard.recentActivity || []).length }} events
        </summary>
        <div class="card-body border-top">
          <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
            <button class="btn btn-outline-primary btn-sm" @click="exportRecentActivityCsv">Export CSV</button>
            <div class="d-flex gap-2 align-items-center flex-wrap">
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
              <input
                v-model="activityFilter.search"
                class="form-control form-control-sm"
                data-testid="activity-search"
                placeholder="Search details or actor..."
              />
              <button class="btn btn-outline-secondary btn-sm" @click="resetActivityFilters">Reset</button>
            </div>
          </div>
          <div class="small text-muted mb-2">
            Showing {{ filteredActivity.length }} of {{ (dashboard.recentActivity || []).length }}
          </div>
          <div class="table-responsive">
            <table class="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Application</th>
                  <th>Type</th>
                  <th>Details</th>
                  <th>Actor</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="event in sortedActivity" :key="event.id">
                  <td>{{ formatDateTime(event.createdAt) }}</td>
                  <td>
                    <div>{{ event.applicationName }}</div>
                    <div class="small text-muted">{{ event.year }} · {{ event.projectName || '—' }}</div>
                  </td>
                  <td><span class="badge text-bg-light border">{{ event.activityType }}</span></td>
                  <td>{{ event.details || '—' }}</td>
                  <td>{{ event.actorEmail || 'system' }}</td>
                  <td class="text-nowrap">
                    <router-link :to="`/admin/audits/${event.auditId}`" class="btn btn-outline-primary btn-sm">Open audit</router-link>
                  </td>
                </tr>
                <tr v-if="!sortedActivity.length">
                  <td colspan="6" class="text-muted text-center py-3">No activity events match current filters.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </details>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'
import { useAuthStore } from '../../stores/auth'
import { auditStageLabel, auditStatusBadgeClass, auditStatusLabel } from '../../utils/auditStatus'

const loading = ref(true)
const loadError = ref('')
const dashboard = ref({ summary: {}, auditsNeedingAttention: [], evidenceQueue: [] })
const currentUserEmail = ref('')
/** Set from /api/auth/me on load — drives auditor-specific defaults */
const sessionRole = ref(null)
const auditorDefaultsApplied = ref(false)
const authStore = useAuthStore()
const canManageAudits = computed(() => authStore.hasPermission('AUDIT_MANAGEMENT'))
const isAuditorSession = computed(() => sessionRole.value === 'AUDITOR')
const filterName = ref('')
const shareFilter = ref(true)
const savedFilters = ref([])

/** Stages shown for preset `active` (“all open items” — everything except Complete) */
const DEFAULT_ACTIVE_STATUSES = ['DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'ATTESTED']
const ALL_AUDIT_STATUSES = ['DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'ATTESTED', 'COMPLETE']

const auditFilter = reactive({
  queue: 'all',
  /** active | all | DRAFT | … — drives which statuses appear */
  statusPreset: 'active',
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
  const pending = (dashboard.value.evidenceQueue || []).length
  return [
    { label: 'Open audits', value: s.openAudits ?? 0, variant: 'default' },
    { label: 'Overdue', value: s.overdueAudits ?? 0, variant: (s.overdueAudits ?? 0) > 0 ? 'danger' : 'default' },
    { label: 'Awaiting review', value: s.submittedAudits ?? 0, variant: 'default' },
    { label: 'Evidence to review', value: pending, variant: pending > 0 ? 'warning' : 'default' }
  ]
})

const pendingEvidenceCount = computed(() => (dashboard.value.evidenceQueue || []).length)

const healthMessage = computed(() => {
  const s = dashboard.value.summary || {}
  const overdue = s.overdueAudits ?? 0
  const submitted = s.submittedAudits ?? 0
  const ev = pendingEvidenceCount.value
  if (overdue > 0) {
    return `Watch: ${overdue} audit(s) are past due — confirm owners and dates before work stacks up.`
  }
  if (submitted > 0) {
    return `Next: ${submitted} submitted audit(s) need review or attestation.`
  }
  if (ev > 0) {
    return `Next: ${ev} evidence item(s) are waiting for accept/reject.`
  }
  return 'Looking clear — no overdue audits or evidence backlog on this snapshot.'
})

const healthBorderClass = computed(() => {
  const s = dashboard.value.summary || {}
  if ((s.overdueAudits ?? 0) > 0) return 'border-danger'
  if ((s.submittedAudits ?? 0) > 0 || pendingEvidenceCount.value > 0) return 'border-warning'
  return 'border-success'
})

function isAuditOverdue(a) {
  if (!a?.dueAt) return false
  const st = a.status
  if (st === 'COMPLETE') return false
  return new Date(a.dueAt).getTime() < Date.now()
}

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
    sessionRole.value = meRes.data?.role || null
    if (!auditorDefaultsApplied.value && sessionRole.value === 'AUDITOR') {
      auditFilter.queue = 'mine'
      auditorDefaultsApplied.value = true
    }
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
    queue: sessionRole.value === 'AUDITOR' ? 'mine' : 'all',
    statusPreset: 'active',
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

async function archiveEvidence(evidenceId) {
  try {
    await api.put(`/api/evidences/${evidenceId}/lifecycle/archive`)
    await load()
    toastSuccess('Evidence archived.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to archive evidence')
  }
}

async function downloadEvidence(evidence) {
  if (!evidence?.uri || evidence.lifecycleStatus === 'DISPOSED') {
    toastError('Disposed evidence is no longer downloadable.')
    return
  }
  try {
    const cred = localStorage.getItem('auth_credentials')
    const response = await fetch(evidence.uri, {
      method: 'GET',
      credentials: 'include',
      headers: cred ? { Authorization: `Basic ${cred}` } : {}
    })
    if (!response.ok) {
      throw new Error(`Download failed (${response.status})`)
    }
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = evidence.fileName || evidence.title || 'evidence.bin'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to download evidence')
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
  return auditStatusBadgeClass(status)
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

const effectiveStatuses = computed(() => {
  const p = auditFilter.statusPreset || 'active'
  if (p === 'active') return [...DEFAULT_ACTIVE_STATUSES]
  if (p === 'all') return [...ALL_AUDIT_STATUSES]
  return [p]
})

function matchesQueue(a) {
  const q = auditFilter.queue || 'all'
  if (q === 'all') return true
  const now = Date.now()
  const email = currentUserEmail.value
  if (q === 'unassigned') return !a.assignedToEmail
  if (q === 'mine') return !!email && a.assignedToEmail === email
  if (q === 'others') return !!a.assignedToEmail && a.assignedToEmail !== email
  if (q === 'overdue') {
    if (!a.dueAt) return false
    if (a.status === 'COMPLETE') return false
    return new Date(a.dueAt).getTime() < now
  }
  return true
}

/** Count before queue/search/project/framework (matches status selection only) */
const statusScopedAuditsCount = computed(() => {
  const rows = dashboard.value.auditsNeedingAttention || []
  const statuses = effectiveStatuses.value
  return rows.filter((a) => statuses.includes(a.status)).length
})

const filteredAudits = computed(() => {
  return (dashboard.value.auditsNeedingAttention || []).filter((a) => {
    if (!effectiveStatuses.value.includes(a.status)) return false
    if (!matchesQueue(a)) return false
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

const { sortedRows: sortedAudits } = useTableSort(filteredAudits, {
  initialKey: 'dueAt'
})

const { sortedRows: sortedEvidence } = useTableSort(filteredEvidence, {
  initialKey: 'createdAt',
  initialDirection: 'desc'
})

const { sortedRows: sortedActivity } = useTableSort(filteredActivity, {
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

function normalizeAuditFilterState(raw) {
  if (!raw || typeof raw !== 'object') return null
  const out = { ...raw }
  delete out.queueBuckets
  delete out.statuses

  if (Array.isArray(raw.queueBuckets)) {
    const b = raw.queueBuckets
    if (b.length === 0) out.queue = 'all'
    else if (b.length === 1) out.queue = b[0]
    else out.queue = 'all'
  }
  if (out.queue === undefined || out.queue === null) out.queue = 'all'

  if (raw.statuses && Array.isArray(raw.statuses)) {
    const sorted = [...raw.statuses].sort().join(',')
    const activeSorted = [...DEFAULT_ACTIVE_STATUSES].sort().join(',')
    const allSorted = [...ALL_AUDIT_STATUSES].sort().join(',')
    if (sorted === activeSorted) out.statusPreset = 'active'
    else if (sorted === allSorted) out.statusPreset = 'all'
    else if (raw.statuses.length === 1) out.statusPreset = raw.statuses[0]
    else out.statusPreset = 'active'
  } else if (raw.statusPreset !== undefined) {
    out.statusPreset = raw.statusPreset
  } else if (typeof raw.status === 'string') {
    out.statusPreset = raw.status === 'all' ? 'active' : raw.status
  } else {
    out.statusPreset = 'active'
  }

  delete out.status
  return out
}

function loadSavedFilter(id) {
  const found = savedFilters.value.find((f) => f.id === id)
  if (!found) return
  const state = found.filterState || {}
  const audit = normalizeAuditFilterState(state.auditFilter)
  if (audit) Object.assign(auditFilter, audit)
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

<style scoped>
.audit-filter-toolbar {
  border-color: var(--bs-border-color-translucent, rgba(0, 0, 0, 0.08)) !important;
}
</style>
