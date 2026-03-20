<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Findings</h1>
        <p class="text-muted mb-0">
          Track control gaps, assign owners, and manage remediation due dates.
        </p>
      </div>
      <div class="d-flex gap-2 flex-wrap">
        <router-link :to="issueHubLink" class="btn btn-outline-secondary btn-sm">Issue program hub</router-link>
        <router-link to="/admin/risk-register" class="btn btn-outline-primary btn-sm">Go to Risk Register</router-link>
        <router-link to="/admin/remediation-plans" class="btn btn-outline-primary btn-sm">Go to Remediation Plans</router-link>
        <router-link to="/admin/control-exceptions" class="btn btn-outline-secondary btn-sm">Go to Exceptions</router-link>
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
            <button class="btn btn-primary" @click="openModal()">Add finding</button>
          </div>
          <div class="col-md-auto">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="resetFilters">Reset</button>
          </div>
        </div>

        <div v-if="loadError" class="alert alert-danger py-2 d-flex justify-content-between align-items-center gap-2">
          <span>{{ loadError }}</span>
          <button type="button" class="btn btn-outline-danger btn-sm" @click="load">Retry</button>
        </div>
        <div v-if="loading" class="text-muted">Loading findings...</div>
        <div v-else-if="!findings.length" class="text-muted">No findings match the current filter.</div>
        <div v-else class="table-responsive">
          <div class="small text-muted mb-2">Showing {{ findings.length }} findings</div>
          <table class="table table-striped table-hover align-middle mb-0">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('applicationName')">Application {{ sortIndicator('applicationName') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('title')">Title {{ sortIndicator('title') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('severity')">Severity {{ sortIndicator('severity') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('status')">Status {{ sortIndicator('status') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('slaState')">SLA {{ sortIndicator('slaState') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('owner')">Owner {{ sortIndicator('owner') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('dueAt')">Due {{ sortIndicator('dueAt') }}</button></th>
                <th>Exceptions</th>
                <th>Handoff</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="finding in sortedRows" :key="finding.id">
                <td>{{ finding.applicationName || '-' }}</td>
                <td>
                  <div class="fw-semibold">{{ finding.title }}</div>
                  <div class="text-muted small">{{ finding.controlId ? `${finding.controlId} - ${finding.controlName}` : 'General finding' }}</div>
                </td>
                <td>
                  <span class="badge" :class="severityClass(finding.severity)">{{ finding.severity }}</span>
                </td>
                <td>
                  <span class="badge" :class="statusClass(finding.status)">{{ formatFindingStatus(finding.status) }}</span>
                </td>
                <td>
                  <span class="badge" :class="slaClass(finding.slaState)">{{ finding.slaState || '-' }}</span>
                </td>
                <td>{{ finding.ownerDisplayName || finding.ownerEmail || '-' }}</td>
                <td>{{ formatDate(finding.dueAt) }}</td>
                <td class="text-nowrap small">
                  <router-link
                    v-if="finding.auditId"
                    class="btn btn-outline-secondary btn-sm"
                    :to="{
                      path: '/admin/control-exceptions',
                      query: {
                        auditId: String(finding.auditId),
                        findingId: String(finding.id)
                      }
                    }"
                  >
                    <span v-if="(finding.linkedExceptionCount ?? 0) > 0">{{ finding.linkedExceptionCount }} linked</span>
                    <span v-else>Link / request</span>
                  </router-link>
                  <span v-else class="text-muted">—</span>
                </td>
                <td class="text-nowrap">
                  <router-link :to="riskRegisterLink(finding)" class="btn btn-outline-primary btn-sm me-2">
                    Open in Risk Register
                  </router-link>
                  <router-link :to="remediationLink(finding)" class="btn btn-outline-primary btn-sm">
                    Open in Remediation
                  </router-link>
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-secondary btn-sm" @click="openModal(finding)">Edit</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="showModal" :title="`${editing?.id ? 'Edit' : 'Add'} Finding`">
      <form id="finding-form" @submit.prevent="save">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">Audit</label>
            <select v-model="form.auditId" class="form-select" required @change="loadAuditControls">
              <option :value="null" disabled>- Select -</option>
              <option v-for="audit in audits" :key="audit.id" :value="audit.id">
                {{ audit.applicationName }} ({{ audit.year }})
              </option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Control (optional)</label>
            <select v-model="form.auditControlId" class="form-select">
              <option :value="null">General finding (no control)</option>
              <option v-for="control in auditControls" :key="control.id" :value="control.id">
                {{ control.controlControlId }} - {{ control.controlName }}
              </option>
            </select>
          </div>
          <div class="col-12">
            <label class="form-label">Title</label>
            <input v-model="form.title" required class="form-control" maxlength="500" />
          </div>
          <div class="col-12">
            <label class="form-label">Description</label>
            <textarea v-model="form.description" rows="3" class="form-control" maxlength="4000" />
          </div>
          <div class="col-md-4">
            <label class="form-label">Severity</label>
            <select v-model="form.severity" class="form-select" required>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">Status</label>
            <select v-model="form.status" class="form-select">
              <option value="OPEN">OPEN</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="ACCEPTED_RISK">ACCEPTED_RISK</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">Owner</label>
            <select v-model="form.ownerUserId" class="form-select">
              <option :value="null">Unassigned</option>
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.displayName || user.email }}
              </option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Due date</label>
            <input v-model="form.dueAtLocal" type="datetime-local" class="form-control" />
          </div>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button type="submit" form="finding-form" class="btn btn-primary">Save</button>
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
import { useTableSort } from '../../composables/useTableSort'

const route = useRoute()

const findings = ref([])
const audits = ref([])
const users = ref([])
const auditControls = ref([])
const loading = ref(true)
const loadError = ref('')
const showModal = ref(false)
const editing = ref(null)
const filters = reactive({
  auditId: null
})

const issueHubLink = computed(() => {
  if (filters.auditId) {
    return { path: '/admin/issue-program', query: { auditId: String(filters.auditId) } }
  }
  return '/admin/issue-program'
})

const form = reactive({
  auditId: null,
  auditControlId: null,
  title: '',
  description: '',
  severity: 'MEDIUM',
  status: 'OPEN',
  ownerUserId: null,
  dueAtLocal: ''
})

const sortableRows = computed(() => findings.value)
const { sortedRows, toggleSort, sortIndicator } = useTableSort(sortableRows, {
  initialKey: 'dueAt',
  valueGetters: {
    owner: (row) => row.ownerDisplayName || row.ownerEmail || ''
  }
})

function applyAuditIdFromRoute() {
  const raw = route.query.auditId
  if (raw != null && raw !== '') {
    const n = Number(raw)
    filters.auditId = Number.isNaN(n) ? null : n
  } else {
    filters.auditId = null
  }
}

onMounted(async () => {
  applyAuditIdFromRoute()
  await Promise.all([loadLookupData(), load()])
})

watch(
  () => route.query.auditId,
  async () => {
    applyAuditIdFromRoute()
    await load()
  }
)

async function loadLookupData() {
  try {
    const [auditsRes, usersRes] = await Promise.all([
      api.get('/api/my-audits'),
      api.get('/api/users')
    ])
    audits.value = auditsRes.data || []
    users.value = usersRes.data || []
  } catch (e) {
    loadError.value = e.response?.data?.error || 'Failed to load findings lookup data.'
    toastError(loadError.value)
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const params = {}
    if (filters.auditId) {
      params.auditId = filters.auditId
    }
    const res = await api.get('/api/findings', { params })
    findings.value = res.data || []
  } catch (e) {
    loadError.value = e.response?.data?.error || 'Failed to load findings.'
    toastError(loadError.value)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.auditId = null
  load()
}

function openModal(finding = null) {
  editing.value = finding
  form.auditId = finding?.auditId ?? null
  form.auditControlId = finding?.auditControlId ?? null
  form.title = finding?.title ?? ''
  form.description = finding?.description ?? ''
  form.severity = finding?.severity ?? 'MEDIUM'
  form.status = finding?.status ?? 'OPEN'
  form.ownerUserId = finding?.ownerUserId ?? null
  form.dueAtLocal = toLocalDateTimeInput(finding?.dueAt)
  showModal.value = true
  loadAuditControls()
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
    if (form.auditControlId && !auditControls.value.some((c) => c.id === form.auditControlId)) {
      form.auditControlId = null
    }
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load controls for audit.')
  }
}

async function save() {
  try {
    const payload = {
      auditId: form.auditId,
      auditControlId: form.auditControlId || null,
      title: form.title,
      description: form.description || null,
      severity: form.severity,
      status: form.status,
      ownerUserId: form.ownerUserId || null,
      dueAt: form.dueAtLocal ? new Date(form.dueAtLocal).toISOString() : null
    }
    if (editing.value?.id) {
      await api.put(`/api/findings/${editing.value.id}`, payload)
      toastSuccess('Finding updated.')
    } else {
      await api.post('/api/findings', payload)
      toastSuccess('Finding created.')
    }
    showModal.value = false
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save finding.')
  }
}

function toLocalDateTimeInput(value) {
  if (!value) return ''
  const dt = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function severityClass(severity) {
  switch (severity) {
    case 'CRITICAL': return 'text-bg-danger'
    case 'HIGH': return 'text-bg-warning'
    case 'MEDIUM': return 'text-bg-primary'
    default: return 'text-bg-secondary'
  }
}

function statusClass(status) {
  switch (status) {
    case 'RESOLVED': return 'text-bg-success'
    case 'IN_PROGRESS': return 'text-bg-warning'
    case 'ACCEPTED_RISK': return 'text-bg-info'
    default: return 'text-bg-secondary'
  }
}

function slaClass(state) {
  switch (state) {
    case 'BREACHED': return 'text-bg-danger'
    case 'AT_RISK': return 'text-bg-warning'
    case 'ON_TRACK': return 'text-bg-success'
    case 'RESOLVED': return 'text-bg-primary'
    default: return 'text-bg-secondary'
  }
}

function formatFindingStatus(status) {
  if (status === 'IN_PROGRESS') return 'In progress'
  if (status === 'ACCEPTED_RISK') return 'Accepted risk'
  if (status === 'RESOLVED') return 'Resolved'
  if (status === 'OPEN') return 'Open'
  return status || '-'
}

function riskRegisterLink(finding) {
  const params = new URLSearchParams()
  params.set('findingId', String(finding.id))
  if (finding.title) params.set('findingTitle', finding.title)
  if (finding.applicationName) params.set('applicationName', finding.applicationName)
  if (finding.auditId) params.set('auditId', String(finding.auditId))
  return `/admin/risk-register?${params.toString()}`
}

function remediationLink(finding) {
  const params = new URLSearchParams()
  params.set('findingId', String(finding.id))
  if (finding.title) params.set('findingTitle', finding.title)
  if (finding.applicationName) params.set('applicationName', finding.applicationName)
  if (finding.auditId) params.set('auditId', String(finding.auditId))
  return `/admin/remediation-plans?${params.toString()}`
}
</script>
