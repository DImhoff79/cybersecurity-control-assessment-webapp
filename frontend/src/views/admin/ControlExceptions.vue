<template>
  <div>
    <h1 class="h3 mb-3">Control Exceptions</h1>
    <p class="text-muted mb-3">
      Manage temporary control exceptions with compensating controls, approvals, and expiration.
    </p>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-3 gap-2 flex-wrap">
          <button class="btn btn-primary" @click="openModal()">Request exception</button>
          <div class="d-flex gap-2 align-items-center flex-wrap">
            <select v-model="filters.auditId" class="form-select w-auto" @change="load">
              <option :value="null">All audits</option>
              <option v-for="audit in audits" :key="audit.id" :value="audit.id">
                {{ audit.applicationName }} ({{ audit.year }})
              </option>
            </select>
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
                <th>Status</th>
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
                <td><span class="badge" :class="statusClass(item.status)">{{ item.status }}</span></td>
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
              <option :value="null">General exception (no control)</option>
              <option v-for="control in auditControls" :key="control.id" :value="control.id">
                {{ control.controlControlId }} - {{ control.controlName }}
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
import { onMounted, reactive, ref } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const exceptions = ref([])
const audits = ref([])
const auditControls = ref([])
const loading = ref(true)
const loadError = ref('')
const showModal = ref(false)
const filters = reactive({
  auditId: null
})
const form = reactive({
  auditId: null,
  auditControlId: null,
  reason: '',
  compensatingControl: '',
  expiresAtLocal: ''
})

onMounted(async () => {
  await Promise.all([loadAudits(), load()])
})

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
  load()
}

function openModal() {
  form.auditId = null
  form.auditControlId = null
  form.reason = ''
  form.compensatingControl = ''
  form.expiresAtLocal = ''
  auditControls.value = []
  showModal.value = true
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
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load controls.')
  }
}

async function save() {
  try {
    await api.post('/api/admin/control-exceptions', {
      auditId: form.auditId,
      auditControlId: form.auditControlId || null,
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
</script>
