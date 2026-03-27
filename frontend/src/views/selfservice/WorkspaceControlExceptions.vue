<template>
  <div class="workspace-exceptions-page">
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">
      <div>
        <h1 class="h3 mb-2 fw-semibold text-dark">{{ pageTitle }}</h1>
        <p class="text-muted mb-0 lh-base">{{ pageSubtitle }}</p>
      </div>
      <div v-if="showAdminShortcuts" class="d-flex gap-2 flex-wrap">
        <router-link :to="issueHubLink" class="btn btn-outline-secondary btn-sm rounded-pill">Issue program hub</router-link>
        <router-link to="/admin/findings" class="btn btn-outline-secondary btn-sm rounded-pill">Go to Findings</router-link>
      </div>
    </div>

    <div class="card workspace-card border-0 shadow-sm mb-3">
      <div class="card-body p-0">
        <div class="workspace-toolbar px-3 px-md-4 py-3">
          <div class="row g-3 align-items-end">
            <div class="col-md-4">
              <label class="form-label small fw-semibold text-secondary mb-1">Audit</label>
              <select v-model="filters.auditId" class="form-select form-select-sm rounded-3 shadow-sm">
                <option :value="null">All audits</option>
                <option v-for="audit in audits" :key="audit.id" :value="audit.id">
                  {{ audit.applicationName }} ({{ audit.year }})
                </option>
              </select>
            </div>
            <div class="col-md-3">
              <label class="form-label small fw-semibold text-secondary mb-1">Status</label>
              <select v-model="filters.status" class="form-select form-select-sm rounded-3 shadow-sm">
                <option value="">All statuses</option>
                <option value="REQUESTED">Pending</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="EXPIRED">Expired</option>
              </select>
            </div>
            <div class="col-md-auto">
              <button type="button" class="btn btn-primary btn-sm ws-btn-cta rounded-pill px-4" @click="openModal()">
                Request exception
              </button>
            </div>
            <div class="col-md-auto">
              <button type="button" class="btn btn-outline-secondary btn-sm ws-btn-modal-secondary" @click="resetFilters">
                Reset
              </button>
            </div>
          </div>
        </div>

        <div v-if="loadError" class="alert alert-danger py-2 mx-3 mx-md-4 mb-3 d-flex justify-content-between align-items-center gap-2">
          <span>{{ loadError }}</span>
          <button type="button" class="btn btn-outline-danger btn-sm ws-btn-modal-secondary" @click="load">Retry</button>
        </div>
        <div v-if="loading" class="text-muted px-3 px-md-4 py-4">Loading exceptions…</div>
        <div v-else-if="!filteredRows.length" class="text-muted px-3 px-md-4 py-4">No exceptions match the current filter.</div>
        <div v-else class="table-responsive">
          <div class="small text-secondary fw-medium px-3 px-md-4 pt-2 pb-1">
            Showing {{ sortedRows.length }} exception{{ sortedRows.length === 1 ? '' : 's' }}
          </div>
          <table class="table workspace-table align-middle mb-0">
            <thead>
              <tr>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('applicationSort')">
                    Application {{ sortIndicator('applicationSort') }}
                  </button>
                </th>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('controlSort')">
                    Control {{ sortIndicator('controlSort') }}
                  </button>
                </th>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('findingSort')">
                    Linked finding {{ sortIndicator('findingSort') }}
                  </button>
                </th>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('status')">
                    Status {{ sortIndicator('status') }}
                  </button>
                </th>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('slaState')">
                    SLA {{ sortIndicator('slaState') }}
                  </button>
                </th>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('requestedBySort')">
                    Requested by {{ sortIndicator('requestedBySort') }}
                  </button>
                </th>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('expiresAt')">
                    Expires {{ sortIndicator('expiresAt') }}
                  </button>
                </th>
                <th>
                  <button type="button" class="workspace-table-sort" @click="toggleSort('reason')">
                    Reason {{ sortIndicator('reason') }}
                  </button>
                </th>
                <th class="text-end"><span class="visually-hidden">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in sortedRows"
                :key="item.id"
                class="exception-table-row"
                role="button"
                tabindex="0"
                :aria-label="
                  (canEditException(item) ? 'Edit' : 'View') +
                  ' exception for ' +
                  (item.controlId || 'general')
                "
                @click="openRow(item)"
                @keydown.enter.prevent="openRow(item)"
                @keydown.space.prevent="openRow(item)"
              >
                <td class="fw-medium text-dark">{{ item.applicationName }} ({{ item.auditYear }})</td>
                <td class="text-secondary small">{{ item.controlId ? `${item.controlId} - ${item.controlName}` : 'General exception' }}</td>
                <td class="small">
                  <span v-if="item.findingId" class="d-inline-block finding-cell text-secondary" style="max-width: 14rem">
                    <span class="text-muted">#{{ item.findingId }}</span>
                    <span v-if="item.findingTitle"> — {{ item.findingTitle }}</span>
                  </span>
                  <span v-else class="text-muted">—</span>
                </td>
                <td class="small text-secondary">{{ item.status }}</td>
                <td class="small text-secondary">{{ item.slaState || '—' }}</td>
                <td class="small">{{ item.requestedByDisplayName || item.requestedByEmail || '—' }}</td>
                <td class="small text-secondary tabular-nums">{{ formatDate(item.expiresAt) }}</td>
                <td class="small text-truncate" style="max-width: 12rem" :title="item.reason">
                  <div class="text-truncate">{{ item.reason }}</div>
                  <div v-if="item.compensatingControl" class="text-muted text-truncate">Compensating: {{ item.compensatingControl }}</div>
                </td>
                <td class="exception-actions align-top" @click.stop>
                  <div class="d-flex flex-wrap gap-1 align-items-center justify-content-end">
                    <button
                      type="button"
                      class="btn btn-outline-primary btn-sm ws-btn-row rounded-pill px-3"
                      @click="canEditException(item) ? openEdit(item) : openView(item)"
                    >
                      {{ canEditException(item) ? 'Edit' : 'View' }}
                    </button>
                    <button
                      v-if="item.status === 'REQUESTED' && canShowDecisionButtons"
                      type="button"
                      class="btn btn-success btn-sm ws-btn-inline-action rounded-pill"
                      @click="approve(item.id)"
                    >
                      Approve
                    </button>
                    <button
                      v-if="item.status === 'REQUESTED' && canShowDecisionButtons"
                      type="button"
                      class="btn btn-outline-danger btn-sm ws-btn-inline-action rounded-pill"
                      @click="reject(item.id)"
                    >
                      Reject
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="showDetailModal" title="Exception details" size="xl" scrollable>
      <div v-if="detailItem" class="exception-detail">
        <dl class="row g-3 mb-0">
          <dt class="col-sm-3 text-muted">Application</dt>
          <dd class="col-sm-9">
            {{ detailItem.applicationName }} ({{ detailItem.auditYear }})
          </dd>
          <dt class="col-sm-3 text-muted">Control</dt>
          <dd class="col-sm-9">
            {{
              detailItem.controlId ? `${detailItem.controlId} — ${detailItem.controlName}` : 'General exception (no control)'
            }}
          </dd>
          <dt class="col-sm-3 text-muted">Linked finding</dt>
          <dd class="col-sm-9">
            <template v-if="detailItem.findingId">
              <span class="text-muted">#{{ detailItem.findingId }}</span>
              <span v-if="detailItem.findingTitle"> — {{ detailItem.findingTitle }}</span>
            </template>
            <span v-else class="text-muted">—</span>
          </dd>
          <dt class="col-sm-3 text-muted">Status</dt>
          <dd class="col-sm-9">
            <span class="fw-medium" :class="statusTextClass(detailItem.status)">{{ detailItem.status }}</span>
            <span v-if="detailItem.slaState" class="ms-2 fw-medium" :class="slaTextClass(detailItem.slaState)">{{
              detailItem.slaState
            }}</span>
          </dd>
          <dt class="col-sm-3 text-muted">Requested</dt>
          <dd class="col-sm-9">
            {{ detailItem.requestedByDisplayName || detailItem.requestedByEmail || '—' }}
            <span v-if="detailItem.requestedAt" class="text-muted"> · {{ formatDate(detailItem.requestedAt) }}</span>
          </dd>
          <dt class="col-sm-3 text-muted">Expires</dt>
          <dd class="col-sm-9">{{ formatDate(detailItem.expiresAt) }}</dd>
          <dt class="col-sm-3 text-muted">Reason</dt>
          <dd class="col-sm-9">
            <div class="exception-detail-text">{{ detailItem.reason || '—' }}</div>
          </dd>
          <dt v-if="detailItem.compensatingControl" class="col-sm-3 text-muted">Compensating control</dt>
          <dd v-if="detailItem.compensatingControl" class="col-sm-9">
            <div class="exception-detail-text">{{ detailItem.compensatingControl }}</div>
          </dd>
          <template v-if="detailItem.status !== 'REQUESTED'">
            <dt class="col-sm-3 text-muted">Decision</dt>
            <dd class="col-sm-9">
              <div v-if="detailItem.decidedByDisplayName || detailItem.decidedByEmail">
                By {{ detailItem.decidedByDisplayName || detailItem.decidedByEmail }}
                <span v-if="detailItem.decidedAt" class="text-muted"> · {{ formatDate(detailItem.decidedAt) }}</span>
              </div>
              <div v-if="detailItem.decisionNotes" class="exception-detail-text mt-1">{{ detailItem.decisionNotes }}</div>
              <div v-if="!detailItem.decidedAt && !detailItem.decisionNotes && !detailItem.decidedByEmail" class="text-muted">
                —
              </div>
            </dd>
          </template>
        </dl>
      </div>
      <template #footer>
        <button type="button" class="btn btn-primary btn-sm ws-btn-modal-primary" @click="closeDetailModal">Close</button>
      </template>
    </BsModal>

    <BsModal v-model="showModal" :title="modalTitle" size="xl" scrollable>
      <form id="workspace-exception-form" @submit.prevent="save">
        <div class="row g-4">
          <div class="col-md-6">
            <label class="form-label">Audit</label>
            <select
              v-model="form.auditId"
              class="form-select"
              required
              :disabled="!!editingId"
              @change="onAuditChangeInModal"
            >
              <option :value="null" disabled>- Select -</option>
              <option v-for="audit in audits" :key="audit.id" :value="audit.id">
                {{ audit.applicationName }} ({{ audit.year }})
              </option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Control (optional)</label>
            <select v-model="form.auditControlId" class="form-select" @change="onControlSelectManual">
              <option :value="null">General exception (no control)</option>
              <option v-for="control in auditControls" :key="control.id" :value="control.id">
                {{ control.controlControlId }} - {{ control.controlName }}
              </option>
            </select>
            <div v-if="form.findingId" class="form-text">Control is taken from the linked finding when it is control-specific.</div>
          </div>
          <div class="col-md-6">
            <label class="form-label">Linked finding (optional)</label>
            <select v-model="form.findingId" class="form-select" @change="onFindingLinked">
              <option :value="null">No finding link</option>
              <option v-for="f in auditFindings" :key="f.id" :value="f.id">
                #{{ f.id }} — {{ f.title }}
              </option>
            </select>
          </div>
          <div class="col-12">
            <label class="form-label">Reason</label>
            <textarea v-model="form.reason" class="form-control" rows="3" maxlength="2000" required />
          </div>
          <div class="col-12">
            <label class="form-label">Compensating control</label>
            <textarea v-model="form.compensatingControl" class="form-control" rows="2" maxlength="2000" />
          </div>
          <div class="col-md-6">
            <label class="form-label">Expires at</label>
            <input v-model="form.expiresAtLocal" type="datetime-local" class="form-control" />
          </div>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary btn-sm ws-btn-modal-secondary" @click="closeModal">
          Cancel
        </button>
        <button type="submit" form="workspace-exception-form" class="btn btn-primary btn-sm ws-btn-modal-primary">
          {{ editingId ? 'Save changes' : 'Submit request' }}
        </button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, toRaw, watch } from 'vue'
import { useRoute } from 'vue-router'
import BsModal from '../../components/BsModal.vue'
import { useTableSort } from '../../composables/useTableSort'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useAuthStore } from '../../stores/auth'

const props = defineProps({
  pageTitle: { type: String, default: 'Control exceptions' },
  pageSubtitle: {
    type: String,
    default: 'Requests for audits you can access. Approve or reject when you are authorized.'
  },
  showAdminShortcuts: { type: Boolean, default: false }
})

const route = useRoute()
const authStore = useAuthStore()

const API = '/api/workspace/control-exceptions'

const exceptions = ref([])
const audits = ref([])
const auditControls = ref([])
const auditFindings = ref([])
const loading = ref(true)
const loadError = ref('')
const showModal = ref(false)
const showDetailModal = ref(false)
/** Row selected in read-only detail dialog */
const detailItem = ref(null)
/** When set, modal is editing an existing pending request */
const editingId = ref(null)
const filters = reactive({
  auditId: null,
  findingId: null,
  status: ''
})

const modalTitle = computed(() => (editingId.value ? 'Edit exception request' : 'Request Control Exception'))

const canShowDecisionButtons = computed(() => authStore.user?.role !== 'APPLICATION_OWNER')

const filteredRows = computed(() => {
  let rows = exceptions.value
  if (filters.auditId != null) {
    rows = rows.filter((e) => e.auditId === filters.auditId)
  }
  if (filters.findingId != null) {
    rows = rows.filter((e) => e.findingId === filters.findingId)
  }
  if (filters.status) {
    rows = rows.filter((e) => e.status === filters.status)
  }
  return rows
})

const { sortedRows, toggleSort, sortIndicator } = useTableSort(filteredRows, {
  initialKey: 'requestedAt',
  initialDirection: 'desc',
  valueGetters: {
    applicationSort: (row) =>
      `${row.applicationName || ''} (${row.auditYear ?? ''})`.toLowerCase(),
    controlSort: (row) =>
      (row.controlId ? `${row.controlId} ${row.controlName || ''}` : 'general exception').toLowerCase(),
    findingSort: (row) => {
      if (row.findingId == null) return ''
      return `${row.findingId} ${row.findingTitle || ''}`.toLowerCase()
    },
    requestedBySort: (row) =>
      (row.requestedByDisplayName || row.requestedByEmail || '').toLowerCase(),
    requestedAt: (row) => row.requestedAt || ''
  }
})

const issueHubLink = computed(() => {
  const q = {}
  if (filters.auditId) q.auditId = String(filters.auditId)
  if (filters.findingId) q.findingId = String(filters.findingId)
  if (Object.keys(q).length) return { path: '/admin/issue-program', query: q }
  return '/admin/issue-program'
})

const form = reactive({
  auditId: null,
  auditControlId: null,
  findingId: null,
  reason: '',
  compensatingControl: '',
  expiresAtLocal: ''
})

function applyFiltersFromRoute() {
  const rawAudit = route.query.auditId
  if (rawAudit != null && rawAudit !== '') {
    const n = Number(rawAudit)
    filters.auditId = Number.isNaN(n) ? null : n
  } else {
    filters.auditId = null
  }
  const rawFinding = route.query.findingId
  if (rawFinding != null && rawFinding !== '') {
    const n = Number(rawFinding)
    filters.findingId = Number.isNaN(n) ? null : n
  } else {
    filters.findingId = null
  }
}

onMounted(async () => {
  applyFiltersFromRoute()
  await Promise.all([loadAudits(), load()])
})

watch(
  () => [route.query.auditId, route.query.findingId],
  () => {
    applyFiltersFromRoute()
  }
)

watch(showModal, (open) => {
  if (!open) editingId.value = null
})

function cloneExceptionRowFallback(item) {
  if (!item) return null
  try {
    return JSON.parse(JSON.stringify(toRaw(item)))
  } catch {
    return { ...toRaw(item) }
  }
}

function normalizeDetailPayload(data, id, fallbackItem) {
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    return data
  }
  if (Array.isArray(data)) {
    const row = data.find((r) => r.id === id)
    if (row) return row
  }
  return cloneExceptionRowFallback(fallbackItem)
}

async function openView(item) {
  const id = item?.id
  if (id == null) return
  try {
    const res = await api.get(`${API}/detail`, { params: { id } })
    detailItem.value = normalizeDetailPayload(res.data, id, item)
    await nextTick()
    showDetailModal.value = true
  } catch (e) {
    const status = e.response?.status
    if (status === 405 || status === 404) {
      detailItem.value = cloneExceptionRowFallback(item)
      await nextTick()
      showDetailModal.value = true
      return
    }
    toastError(e.response?.data?.error || e.message || 'Failed to load exception details.')
  }
}

function openRow(item) {
  if (canEditException(item)) {
    openEdit(item)
  } else {
    openView(item)
  }
}

function closeDetailModal() {
  showDetailModal.value = false
}

async function loadAudits() {
  try {
    const res = await api.get('/api/my-audits')
    audits.value = res.data || []
  } catch (e) {
    loadError.value = e.response?.data?.error || 'Failed to load audits.'
    toastError(loadError.value)
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await api.get(API)
    exceptions.value = res.data || []
  } catch (e) {
    loadError.value = e.response?.data?.error || 'Failed to load exceptions.'
    toastError(loadError.value)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.auditId = null
  filters.findingId = null
  filters.status = ''
}

function openModal() {
  editingId.value = null
  form.auditId = filters.auditId ?? null
  form.auditControlId = null
  form.findingId = filters.findingId ?? null
  form.reason = ''
  form.compensatingControl = ''
  form.expiresAtLocal = ''
  auditControls.value = []
  auditFindings.value = []
  showModal.value = true
  if (form.auditId) {
    loadAuditControls()
    loadAuditFindings()
  }
}

function closeModal() {
  showModal.value = false
  editingId.value = null
}

function canEditException(item) {
  if (item.status !== 'REQUESTED') return false
  const uid = authStore.user?.id
  if (!uid) return false
  if (authStore.user?.role === 'AUDITOR') return item.requestedByUserId === uid
  return true
}

function toDatetimeLocalValue(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function openEdit(item) {
  detailItem.value = null
  editingId.value = item.id
  form.auditId = item.auditId
  form.auditControlId = item.auditControlId ?? null
  form.findingId = item.findingId ?? null
  form.reason = item.reason || ''
  form.compensatingControl = item.compensatingControl || ''
  form.expiresAtLocal = toDatetimeLocalValue(item.expiresAt)
  showModal.value = true
  await loadAuditControls()
  await loadAuditFindings()
}

async function onAuditChangeInModal() {
  form.findingId = null
  await loadAuditControls()
  await loadAuditFindings()
}

async function loadAuditControls() {
  if (!form.auditId) {
    auditControls.value = []
    form.auditControlId = null
    return
  }
  try {
    const res = await api.get(`/api/audits/${form.auditId}/controls`)
    auditControls.value = res.data || []
    syncControlFromLinkedFinding()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load controls.')
  }
}

async function loadAuditFindings() {
  if (!form.auditId) {
    auditFindings.value = []
    return
  }
  try {
    const res = await api.get('/api/workspace/findings', { params: { auditId: form.auditId } })
    auditFindings.value = res.data || []
    syncControlFromLinkedFinding()
  } catch {
    auditFindings.value = []
  }
}

function onFindingLinked() {
  syncControlFromLinkedFinding()
}

function onControlSelectManual() {
  if (form.findingId) {
    const f = auditFindings.value.find((x) => x.id === form.findingId)
    if (f?.auditControlId && form.auditControlId !== f.auditControlId) {
      toastError('Pick a different linked finding or clear the finding link to choose another control.')
      syncControlFromLinkedFinding()
    }
  }
}

function syncControlFromLinkedFinding() {
  if (!form.findingId) return
  const f = auditFindings.value.find((x) => x.id === form.findingId)
  if (f?.auditControlId) {
    form.auditControlId = f.auditControlId
  }
}

async function save() {
  try {
    if (editingId.value) {
      await api.put(`${API}/${editingId.value}`, {
        auditControlId: form.auditControlId || null,
        findingId: form.findingId || null,
        reason: form.reason,
        compensatingControl: form.compensatingControl || null,
        expiresAt: form.expiresAtLocal ? new Date(form.expiresAtLocal).toISOString() : null
      })
      toastSuccess('Exception updated.')
    } else {
      await api.post(API, {
        auditId: form.auditId,
        auditControlId: form.auditControlId || null,
        findingId: form.findingId || null,
        reason: form.reason,
        compensatingControl: form.compensatingControl || null,
        expiresAt: form.expiresAtLocal ? new Date(form.expiresAtLocal).toISOString() : null
      })
      toastSuccess('Exception request submitted.')
    }
    closeModal()
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save exception request.')
  }
}

async function approve(exceptionId) {
  if (!confirm('Approve this exception request?')) return
  const notes = window.prompt('Approval notes (optional):', '') || ''
  const expiresAtLocal = window.prompt('Expiration (optional, YYYY-MM-DDTHH:mm):', '') || ''
  try {
    await api.post(`${API}/${exceptionId}/approve`, {
      decisionNotes: notes || null,
      expiresAt: expiresAtLocal ? new Date(expiresAtLocal).toISOString() : null
    })
    toastSuccess('Exception approved.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to approve exception.')
  }
}

async function reject(exceptionId) {
  if (!confirm('Reject this exception request?')) return
  const notes = window.prompt('Rejection reason:', '')
  if (notes === null) return
  try {
    await api.post(`${API}/${exceptionId}/reject`, {
      decisionNotes: notes || null
    })
    toastSuccess('Exception rejected.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to reject exception.')
  }
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function statusTextClass(status) {
  switch (status) {
    case 'APPROVED':
      return 'text-success'
    case 'REJECTED':
      return 'text-danger'
    case 'EXPIRED':
      return 'text-secondary'
    case 'REQUESTED':
      return 'text-warning'
    default:
      return 'text-body-secondary'
  }
}

function slaTextClass(state) {
  switch (state) {
    case 'BREACHED':
      return 'text-danger'
    case 'AT_RISK':
      return 'text-warning'
    case 'ON_TRACK':
      return 'text-success'
    case 'PENDING_DECISION':
      return 'text-warning'
    default:
      return 'text-body-secondary'
  }
}

</script>

<style scoped>
.exception-table-row {
  cursor: pointer;
}

.exception-table-row:focus-visible {
  outline: 2px solid var(--bs-primary, #0d6efd);
  outline-offset: -2px;
}

.exception-detail-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.finding-cell {
  word-break: break-word;
}

.tabular-nums {
  font-variant-numeric: tabular-nums;
}
</style>
