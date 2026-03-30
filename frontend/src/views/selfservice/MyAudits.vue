<template>
  <div id="tour-my-audits-panel" class="my-audits-page">
    <header class="my-audits-header mb-4">
      <h1 class="h3 mb-2 fw-semibold text-dark">My Audits</h1>
      <p class="text-muted mb-0 lh-base">
        Track assigned assessments, completion progress, and next actions.
      </p>
      <p class="small text-muted mt-2 mb-0">
        Start or resume any in-progress audit, then submit when all sections are complete.
      </p>
    </header>

    <p v-if="!items.length && !loading" class="my-audits-empty-panel text-muted rounded-3 p-4 text-center border">
      No audits assigned to you.
    </p>

    <div v-else class="card my-audits-card workspace-card border-0 shadow-sm">
      <div class="card-body p-0">
        <div class="my-audits-toolbar px-3 px-md-4 py-3 border-bottom bg-light bg-opacity-50">
          <div class="d-flex flex-wrap align-items-end justify-content-between gap-3">
            <div class="d-flex flex-wrap align-items-end gap-3">
              <div>
                <label class="form-label small fw-semibold text-secondary mb-1" for="my-audits-filter">Show</label>
                <select
                  id="my-audits-filter"
                  v-model="filterShow"
                  class="form-select form-select-sm my-audits-filter-select shadow-sm"
                >
                  <option value="active">Active (hide completed)</option>
                  <option value="all">All audits</option>
                  <option value="completed">Completed only</option>
                </select>
              </div>
              <button
                type="button"
                class="btn btn-outline-secondary btn-sm ws-btn-modal-secondary"
                @click="resetFilter"
              >
                Reset filter
              </button>
            </div>
            <div v-if="filteredItems.length" class="small text-secondary fw-medium my-audits-count">
              {{ filteredItems.length }} audit{{ filteredItems.length === 1 ? '' : 's' }}
            </div>
          </div>
        </div>

        <div v-if="!filteredItems.length && !loading" class="text-center text-muted py-5 px-3">
          <p class="mb-2">No audits match this filter.</p>
          <button type="button" class="btn btn-sm btn-outline-primary" @click="filterShow = 'all'">
            Show all audits
          </button>
        </div>

        <div v-else class="table-responsive my-audits-table-responsive">
          <table class="table my-audits-table workspace-table align-middle mb-0">
            <thead>
              <tr>
                <th scope="col">
                  <button
                    type="button"
                    class="workspace-table-sort"
                    @click="toggleSort('applicationName')"
                  >
                    Application {{ sortIndicator('applicationName') }}
                  </button>
                </th>
                <th scope="col">
                  <button type="button" class="workspace-table-sort" @click="toggleSort('year')">
                    Year {{ sortIndicator('year') }}
                  </button>
                </th>
                <th scope="col">
                  <button type="button" class="workspace-table-sort" @click="toggleSort('projectName')">
                    Project {{ sortIndicator('projectName') }}
                  </button>
                </th>
                <th scope="col">
                  <button type="button" class="workspace-table-sort" @click="toggleSort('status')">
                    Status {{ sortIndicator('status') }}
                  </button>
                </th>
                <th scope="col">
                  <button type="button" class="workspace-table-sort" @click="toggleSort('status')">
                    Stage {{ sortIndicator('status') }}
                  </button>
                </th>
                <th scope="col">
                  <button type="button" class="workspace-table-sort" @click="toggleSort('completionPct')">
                    Completion {{ sortIndicator('completionPct') }}
                  </button>
                </th>
                <th scope="col" class="text-nowrap">Security review</th>
                <th scope="col" class="text-nowrap">Controls</th>
                <th scope="col" class="text-end"><span class="visually-hidden">Action</span></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in sortedRows" :key="a.id">
                <td class="my-audits-app">
                  <router-link :to="respondPath(a.id)" class="my-audits-app-link">
                    {{ a.applicationName }}
                  </router-link>
                </td>
                <td class="my-audits-meta text-secondary">{{ a.year }}</td>
                <td class="my-audits-meta">{{ a.projectName || '—' }}</td>
                <td class="my-audits-meta text-secondary" :title="auditStatusLabel(a.status)">
                  {{ auditStatusLabel(a.status) }}
                </td>
                <td>
                  <span class="small text-secondary">{{ auditStageLabel(a.status) }}</span>
                </td>
                <td>
                  <div class="d-flex align-items-center gap-2 my-audits-progress-wrap">
                    <div class="progress flex-grow-1 my-audits-progress rounded-pill">
                      <div
                        class="progress-bar rounded-pill"
                        role="progressbar"
                        :style="{ width: `${a.completionPct || 0}%` }"
                        :aria-valuenow="a.completionPct || 0"
                        aria-valuemin="0"
                        aria-valuemax="100"
                      />
                    </div>
                    <span class="my-audits-pct small tabular-nums">{{ a.completionPct || 0 }}%</span>
                  </div>
                </td>
                <td class="small">
                  <span
                    v-if="a.securityArchitectureReview"
                    class="badge"
                    :class="secReviewBadgeClass(a.securityArchitectureReview.status)"
                    :title="a.securityArchitectureReview.notes || ''"
                  >
                    {{ secReviewLabel(a.securityArchitectureReview.status) }}
                  </span>
                  <span v-else class="text-muted">—</span>
                </td>
                <td>
                  <button type="button" class="btn btn-link btn-sm p-0" @click="openControlsModal(a)">View set</button>
                </td>
                <td class="audit-action-cell text-end">
                  <router-link
                    :to="respondPath(a.id)"
                    class="btn btn-outline-primary btn-sm ws-btn-row audit-action-btn rounded-pill px-3"
                  >
                    {{ actionLabel(a) }}
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="controlsModalOpen" :title="controlsModalTitle" size="lg">
      <p class="small text-muted mb-3">
        Resolved control library for this application. When regulatory filtering is enabled, rows marked “Excluded” are not seeded into new audits based on triage flags.
      </p>
      <div v-if="controlsLoading" class="text-muted py-2">Loading…</div>
      <div v-else class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Scopes</th>
              <th>Assessment</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in assessmentControls" :key="row.id">
              <td class="text-nowrap small">{{ row.controlId }}</td>
              <td>{{ row.name }}</td>
              <td class="small">
                <span v-if="!row.regulatoryScopes?.length" class="text-muted">Baseline</span>
                <span v-else>{{ row.regulatoryScopes.join(', ') }}</span>
              </td>
              <td>
                <span class="badge" :class="row.includedUnderCurrentFilter ? 'text-bg-success' : 'text-bg-secondary'">
                  {{ row.includedUnderCurrentFilter ? 'Included' : 'Excluded' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="controlsModalOpen = false">Close</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import api from '../../services/api'
import BsModal from '../../components/BsModal.vue'
import { toastError } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'
import {
  auditStageLabel,
  auditStatusLabel,
  isAuditCompleted,
  isAuditOwnerAnswerLocked
} from '../../utils/auditStatus'

const items = ref([])
const loading = ref(true)
const controlsModalOpen = ref(false)
const controlsModalAudit = ref(null)
const assessmentControls = ref([])
const controlsLoading = ref(false)
/** 'active' = hide COMPLETE; 'all'; 'completed' = COMPLETE only */
const filterShow = ref('active')

const filteredItems = computed(() => {
  const list = items.value || []
  if (filterShow.value === 'active') {
    return list.filter((a) => !isAuditCompleted(a.status))
  }
  if (filterShow.value === 'completed') {
    return list.filter((a) => isAuditCompleted(a.status))
  }
  return list
})

const { sortedRows, toggleSort, sortIndicator } = useTableSort(filteredItems, {
  initialKey: 'applicationName'
})

function resetFilter() {
  filterShow.value = 'active'
}

onMounted(async () => {
  try {
    const res = await api.get('/api/my-audits')
    items.value = res.data || []
  } finally {
    loading.value = false
  }
})

function respondPath(auditId) {
  return `/audits/${auditId}/respond`
}

function actionLabel(audit) {
  if (audit.status === 'REVISIONS_REQUESTED') return 'Address revisions'
  if (isAuditOwnerAnswerLocked(audit.status)) return 'View submission'
  return (audit.completionPct || 0) > 0 ? 'Resume audit' : 'Start audit'
}

const controlsModalTitle = computed(() => {
  const a = controlsModalAudit.value
  return a ? `Assessment controls — ${a.applicationName}` : 'Assessment controls'
})

function secReviewLabel(status) {
  const m = {
    NOT_STARTED: 'Not started',
    IN_REVIEW: 'In review',
    APPROVED: 'Approved',
    CHANGES_REQUESTED: 'Changes requested'
  }
  return m[status] || status || '—'
}

function secReviewBadgeClass(status) {
  if (status === 'APPROVED') return 'text-bg-success'
  if (status === 'CHANGES_REQUESTED') return 'text-bg-warning text-dark'
  if (status === 'IN_REVIEW') return 'text-bg-info text-dark'
  return 'text-bg-secondary'
}

async function openControlsModal(audit) {
  controlsModalAudit.value = audit
  controlsModalOpen.value = true
  assessmentControls.value = []
  if (!audit.applicationId) return
  controlsLoading.value = true
  try {
    const res = await api.get(`/api/applications/${audit.applicationId}/assessment-controls`)
    assessmentControls.value = res.data || []
  } catch (e) {
    toastError(e.response?.data?.error || 'Could not load controls')
  } finally {
    controlsLoading.value = false
  }
}
</script>

<style scoped>
.my-audits-header h1 {
  letter-spacing: -0.02em;
}

.my-audits-filter-select {
  min-width: 11rem;
  border-radius: 0.5rem;
}

.my-audits-table-responsive {
  -webkit-overflow-scrolling: touch;
}

.my-audits-table thead th {
  white-space: nowrap;
}

.my-audits-app-link {
  color: #1e3a5f;
  font-weight: 600;
  letter-spacing: -0.01em;
  text-decoration: none;
  transition: color 0.15s ease;
}

.my-audits-app-link:hover {
  color: var(--brand-primary, #21409a);
  text-decoration: underline;
}

.my-audits-meta {
  font-size: 0.9rem;
}

.my-audits-table td:nth-child(4) {
  line-height: 1.35;
  max-width: 13rem;
  white-space: normal;
}

.my-audits-progress-wrap {
  min-width: 0;
}

.my-audits-progress {
  background-color: rgba(33, 64, 154, 0.1);
  height: 10px;
  min-width: 3rem;
}

@media (min-width: 992px) {
  .my-audits-progress {
    min-width: 6rem;
  }
}

.my-audits-progress .progress-bar {
  background: linear-gradient(90deg, var(--brand-secondary, #0468b3), var(--brand-primary, #21409a));
}

.my-audits-pct {
  color: #64748b;
  font-variant-numeric: tabular-nums;
  min-width: 2.5rem;
  text-align: right;
}

.audit-action-cell {
  vertical-align: middle;
  width: 1%;
}

@media (min-width: 992px) {
  .my-audits-table th:nth-child(1),
  .my-audits-table td:nth-child(1) {
    min-width: 12rem;
  }

  .my-audits-table th:nth-child(6),
  .my-audits-table td:nth-child(6) {
    min-width: 11rem;
  }

  .audit-action-cell {
    min-width: 9rem;
  }
}

@media (max-width: 767.98px) {
  .audit-action-cell .audit-action-btn.ws-btn-row {
    width: 100%;
  }
}

.my-audits-empty-panel {
  background: rgba(248, 250, 255, 0.9);
  border-color: rgba(33, 64, 154, 0.12) !important;
}
</style>
