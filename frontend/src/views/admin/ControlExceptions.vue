<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Issue Management - Control Exceptions</h1>
        <p class="text-muted mb-0">
          Manage temporary exceptions with compensating controls, approvals, and expiration.
        </p>
      </div>
      <div class="d-flex gap-2 flex-wrap">
        <router-link :to="issueHubLink" class="btn btn-outline-secondary btn-sm">Issue program hub</router-link>
        <router-link to="/admin/findings" class="btn btn-outline-secondary btn-sm">Go to Findings</router-link>
      </div>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2 align-items-end mb-3">
          <div class="col-md-4">
            <label class="form-label small mb-1">Audit</label>
            <select v-model="filters.auditId" class="form-select" @change="load">
              <option :value="null">All audits</option>
              <option v-for="audit in audits" :key="audit.id" :value="audit.id">
                {{ audit.applicationName }} ({{ audit.year }})
              </option>
            </select>
          </div>
          <div class="col-md-auto">
            <button class="btn btn-primary" @click="openModal()">Request exception</button>
          </div>
          <div class="col-md-auto">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="resetFilters">Reset</button>
          </div>
        </div>

        <div v-if="loadError" class="alert alert-danger py-2 d-flex justify-content-between align-items-center gap-2">
          <span>{{ loadError }}</span>
          <button type="button" class="btn btn-outline-danger btn-sm" @click="load">Retry</button>
        </div>
        <div v-if="loading" class="text-muted">Loading exceptions...</div>
        <div v-else-if="!exceptions.length" class="text-muted">No exceptions match the current filter.</div>
        <div v-else class="table-responsive">
          <div class="small text-muted mb-2">Showing {{ exceptions.length }} exceptions</div>
          <table class="table table-striped table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>Application</th>
                <th>Control</th>
                <th>Linked finding</th>
                <th>Status</th>
                <th>SLA</th>
                <th>Requested by</th>
                <th>Expires</th>
                <th>Reason</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in exceptions" :key="item.id">
                <td>{{ item.applicationName }} ({{ item.auditYear }})</td>
                <td>{{ item.controlId ? `${item.controlId} - ${item.controlName}` : 'General exception' }}</td>
                <td class="small">
                  <span v-if="item.findingId" class="text-wrap d-inline-block" style="max-width: 14rem">
                    <span class="text-muted">#{{ item.findingId }}</span>
                    <span v-if="item.findingTitle"> — {{ item.findingTitle }}</span>
                  </span>
                  <span v-else class="text-muted">—</span>
                </td>
                <td><span class="badge" :class="statusClass(item.status)">{{ item.status }}</span></td>
                <td><span class="badge" :class="slaClass(item.slaState)">{{ item.slaState || '-' }}</span></td>
                <td>{{ item.requestedByDisplayName || item.requestedByEmail || '-' }}</td>
                <td>{{ formatDate(item.expiresAt) }}</td>
                <td class="small">
                  <div>{{ item.reason }}</div>
                  <div v-if="item.compensatingControl" class="text-muted">Compensating: {{ item.compensatingControl }}</div>
                </td>
                <td class="text-nowrap">
                  <button
                    v-if="item.status === 'REQUESTED'"
                    class="btn btn-success btn-sm me-2"
                    @click="approve(item.id)"
                  >
                    Approve
                  </button>
                  <button
                    v-if="item.status === 'REQUESTED'"
                    class="btn btn-outline-danger btn-sm"
                    @click="reject(item.id)"
                  >
                    Reject
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="showModal" title="Request Control Exception">
      <form id="exception-form" @submit.prevent="save">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">Audit</label>
            <select v-model="form.auditId" class="form-select" required @change="onAuditChangeInModal">
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
        <button type="button" class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button type="submit" form="exception-form" class="btn btn-primary">Submit request</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const route = useRoute()

const exceptions = ref([])
const audits = ref([])
const auditControls = ref([])
const auditFindings = ref([])
const loading = ref(true)
const loadError = ref('')
const showModal = ref(false)
const filters = reactive({
  auditId: null,
  findingId: null
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
  async () => {
    applyFiltersFromRoute()
    await load()
  }
)

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
    const params = {}
    if (filters.auditId) params.auditId = filters.auditId
    if (filters.findingId) params.findingId = filters.findingId
    const res = await api.get('/api/admin/control-exceptions', { params })
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
  load()
}

function openModal() {
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
    const res = await api.get('/api/findings', { params: { auditId: form.auditId } })
    auditFindings.value = res.data || []
    syncControlFromLinkedFinding()
  } catch (e) {
    auditFindings.value = []
    toastError(e.response?.data?.error || 'Failed to load findings for audit.')
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
    await api.post('/api/admin/control-exceptions', {
      auditId: form.auditId,
      auditControlId: form.auditControlId || null,
      findingId: form.findingId || null,
      reason: form.reason,
      compensatingControl: form.compensatingControl || null,
      expiresAt: form.expiresAtLocal ? new Date(form.expiresAtLocal).toISOString() : null
    })
    showModal.value = false
    toastSuccess('Exception request submitted.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to submit exception request.')
  }
}

async function approve(exceptionId) {
  if (!confirm('Approve this exception request?')) return
  const notes = window.prompt('Approval notes (optional):', '') || ''
  const expiresAtLocal = window.prompt('Expiration (optional, YYYY-MM-DDTHH:mm):', '') || ''
  try {
    await api.post(`/api/admin/control-exceptions/${exceptionId}/approve`, {
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
    await api.post(`/api/admin/control-exceptions/${exceptionId}/reject`, {
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

function statusClass(status) {
  switch (status) {
    case 'APPROVED': return 'text-bg-success'
    case 'REJECTED': return 'text-bg-danger'
    case 'EXPIRED': return 'text-bg-secondary'
    default: return 'text-bg-warning'
  }
}

function slaClass(state) {
  switch (state) {
    case 'BREACHED': return 'text-bg-danger'
    case 'AT_RISK': return 'text-bg-warning'
    case 'ON_TRACK': return 'text-bg-success'
    default: return 'text-bg-secondary'
  }
}
</script>
