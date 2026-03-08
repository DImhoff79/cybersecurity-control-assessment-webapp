<template>
  <div>
    <h1 class="h3 mb-3">Audits</h1>

    <section class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Kick off new audit</h2>
        <form @submit.prevent="createAudit" class="row g-3 align-items-end">
          <div class="col-md-5">
            <label class="form-label">Application</label>
            <select v-model="createForm.applicationId" required class="form-select">
              <option :value="null">- Select -</option>
              <option v-for="a in applications" :key="a.id" :value="a.id">{{ a.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label">Year</label>
            <input v-model.number="createForm.year" type="number" min="2020" max="2030" required class="form-control" />
          </div>
          <div class="col-md-3">
            <label class="form-label">Due Date</label>
            <input v-model="createForm.dueAt" type="date" class="form-control" />
          </div>
          <div class="col-12 col-md-3 col-lg-1">
            <button type="submit" class="btn btn-primary w-100 w-lg-auto">Create audit</button>
          </div>
        </form>
      </div>
    </section>

    <section class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Audit history by application</h2>
        <div v-for="app in applications" :key="app.id" class="mb-4">
          <h3 class="h6 mb-2">{{ app.name }}</h3>
          <div class="table-responsive">
            <table class="table table-striped table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>
                    <input type="checkbox" :checked="allSelected(app.id)" @change="toggleSelectAll(app.id, $event.target.checked)" />
                  </th>
                  <th>Year</th>
                  <th>Status</th>
                  <th>Assigned to</th>
                  <th>Due</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in auditsByApp[app.id] || []" :key="a.id">
                  <td><input type="checkbox" :checked="isSelected(a.id)" @change="toggleSelected(a.id, $event.target.checked)" /></td>
                  <td>{{ a.year }}</td>
                  <td>
                    <span class="badge status-badge" :class="statusBadgeClass(a.status)">
                      {{ formatAuditStatus(a.status) }}
                    </span>
                  </td>
                  <td>{{ a.assignedToDisplayName || a.assignedToEmail || '-' }}</td>
                  <td>{{ formatDate(a.dueAt) }}</td>
                  <td class="audit-actions-cell">
                    <div class="audit-actions">
                      <button class="btn btn-secondary btn-sm" @click="openAssignModal(a)">Assign / Send</button>
                      <button class="btn btn-outline-primary btn-sm" @click="remind(a.id)">Remind</button>
                      <button
                        class="btn btn-outline-success btn-sm"
                        :disabled="a.status !== 'SUBMITTED' && a.status !== 'ATTESTED'"
                        @click="attest(a.id)"
                      >
                        Attest
                      </button>
                      <router-link :to="`/admin/audits/${a.id}`" class="btn btn-primary btn-sm">View / Edit</router-link>
                      <button class="btn btn-danger btn-sm" @click="deleteAudit(a.id)">Delete</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-3 bulk-actions">
            <select v-model="bulkUserId" class="form-select form-select-sm bulk-user-select">
              <option :value="null">Bulk assign selected to...</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
            </select>
            <button class="btn btn-outline-primary btn-sm" :disabled="!selectedAuditIds.length || !bulkUserId" @click="bulkAssign(false)">
              Bulk Assign
            </button>
            <button class="btn btn-outline-primary btn-sm" :disabled="!selectedAuditIds.length || !bulkUserId" @click="bulkAssign(true)">
              Bulk Assign + Send
            </button>
          </div>
        </div>
      </div>
    </section>

    <BsModal v-model="isAssignModalOpen" title="Assign and send audit">
      <p v-if="assignModal" class="text-muted">{{ assignModal.applicationName }} - {{ assignModal.year }}</p>
      <form @submit.prevent>
        <div class="mb-3">
          <label class="form-label">Assign to</label>
          <select v-model="assignForm.userId" class="form-select">
            <option :value="null">- Select -</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
          </select>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isAssignModalOpen = false">Cancel</button>
        <button type="button" class="btn btn-primary" @click="assignAndSend">Assign only</button>
        <button type="button" class="btn btn-primary" @click="sendToOwner">Assign and send to owner</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const applications = ref([])
const users = ref([])
const createForm = reactive({ applicationId: null, year: new Date().getFullYear(), dueAt: '' })
const assignModal = ref(null)
const assignForm = reactive({ userId: null })
const auditsByApp = ref({})
const selectedAuditIds = ref([])
const bulkUserId = ref(null)

const isAssignModalOpen = computed({
  get: () => !!assignModal.value,
  set: (open) => {
    if (!open) assignModal.value = null
  }
})

onMounted(load)

async function load() {
  const [appsRes, usersRes] = await Promise.all([
    api.get('/api/applications'),
    api.get('/api/users')
  ])
  applications.value = appsRes.data || []
  users.value = usersRes.data || []
  for (const app of applications.value) {
    const res = await api.get(`/api/applications/${app.id}/audits`)
    auditsByApp.value[app.id] = res.data || []
  }
}

async function createAudit() {
  try {
    const payload = { year: createForm.year }
    if (createForm.dueAt) {
      payload.dueAt = `${createForm.dueAt}T23:59:59Z`
    }
    await api.post(`/api/applications/${createForm.applicationId}/audits`, payload)
    toastSuccess('Audit created.')
    load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create audit')
  }
}

function openAssignModal(audit) {
  assignModal.value = audit
  assignForm.userId = audit.assignedToUserId ?? null
}

async function assignAndSend() {
  if (!assignModal.value) return
  try {
    await api.put(`/api/audits/${assignModal.value.id}/assign`, { userId: assignForm.userId })
    toastSuccess('Audit assigned.')
    load()
    assignModal.value = null
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to assign')
  }
}

async function sendToOwner() {
  if (!assignModal.value) return
  try {
    await api.put(`/api/audits/${assignModal.value.id}/assign`, { userId: assignForm.userId })
    await api.post(`/api/audits/${assignModal.value.id}/send`)
    toastSuccess('Audit sent to owner.')
    load()
    assignModal.value = null
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to send')
  }
}

async function deleteAudit(auditId) {
  if (!confirm('Delete this audit and all of its responses? This cannot be undone.')) return
  try {
    await api.delete(`/api/audits/${auditId}`)
    await load()
    toastSuccess('Audit deleted.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete audit')
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
  const statement = prompt('Optional attestation statement', 'Audit reviewed and attested by security.')
  try {
    await api.post(`/api/audits/${auditId}/attest`, { statement: statement || '' })
    await load()
    toastSuccess('Audit attested.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to attest audit')
  }
}

function toggleSelected(auditId, selected) {
  if (selected) {
    if (!selectedAuditIds.value.includes(auditId)) selectedAuditIds.value.push(auditId)
  } else {
    selectedAuditIds.value = selectedAuditIds.value.filter((x) => x !== auditId)
  }
}

function isSelected(auditId) {
  return selectedAuditIds.value.includes(auditId)
}

function allSelected(appId) {
  const ids = (auditsByApp.value[appId] || []).map((a) => a.id)
  return ids.length > 0 && ids.every((id) => selectedAuditIds.value.includes(id))
}

function toggleSelectAll(appId, checked) {
  const ids = (auditsByApp.value[appId] || []).map((a) => a.id)
  if (checked) {
    selectedAuditIds.value = Array.from(new Set([...selectedAuditIds.value, ...ids]))
  } else {
    selectedAuditIds.value = selectedAuditIds.value.filter((id) => !ids.includes(id))
  }
}

async function bulkAssign(sendNow) {
  try {
    await api.post('/api/audits/bulk-assign', {
      auditIds: selectedAuditIds.value,
      userId: bulkUserId.value,
      sendNow
    })
    selectedAuditIds.value = []
    await load()
    toastSuccess(sendNow ? 'Bulk assignment and send complete.' : 'Bulk assignment complete.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to bulk assign')
  }
}

function formatAuditStatus(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'Completed - pending admin review'
    case 'ATTESTED':
      return 'Attested - pending final completion'
    case 'COMPLETE':
      return 'Validated complete'
    case 'IN_PROGRESS':
      return 'In progress'
    case 'DRAFT':
      return 'Draft'
    default:
      return status || '-'
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function statusBadgeClass(status) {
  switch (status) {
    case 'COMPLETE':
      return 'text-bg-success'
    case 'ATTESTED':
      return 'text-bg-primary'
    case 'SUBMITTED':
      return 'text-bg-info'
    case 'IN_PROGRESS':
      return 'text-bg-warning'
    case 'DRAFT':
      return 'text-bg-secondary'
    default:
      return 'text-bg-secondary'
  }
}
</script>

<style scoped>
.audit-actions-cell {
  min-width: 520px;
}

.audit-actions {
  display: flex;
  gap: 0.375rem;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.bulk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.bulk-user-select {
  max-width: 300px;
}
</style>
