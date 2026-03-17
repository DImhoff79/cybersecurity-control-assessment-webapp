<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Audits</h1>
        <p class="text-muted mb-0">
          Manage assignments, reminders, attestations, and completion across audit history.
        </p>
      </div>
    </div>

    <section class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-2">Kick off new audit</h2>
        <p class="text-muted mb-3">
          New audits are created from an Audit Project so scope, year, and reporting stay aligned.
        </p>
        <router-link to="/admin/audit-projects" class="btn btn-primary btn-sm">
          Create Audit Project
        </router-link>
      </div>
    </section>

    <section class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Audit history by application</h2>
        <div class="small text-muted mb-3">
          Select multiple rows to run bulk assignment actions.
        </div>
        <div class="row g-2 mb-3">
          <div class="col-md-4">
            <label class="form-label small mb-1">Filter by project</label>
            <select v-model="projectFilter" class="form-select form-select-sm">
              <option value="all">All projects</option>
              <option value="none">No project (legacy)</option>
              <option v-for="p in projects" :key="p.id" :value="String(p.id)">{{ p.name }}</option>
            </select>
          </div>
        </div>
        <div v-for="app in filteredApplications" :key="app.id" class="mb-4">
          <h3 class="h6 mb-2">{{ app.name }}</h3>
          <div class="table-responsive">
            <table class="table table-striped table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>
                    <input type="checkbox" :checked="allSelected(app.id)" @change="toggleSelectAll(app.id, $event.target.checked)" />
                  </th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('year')">Year {{ sortIndicator('year') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('projectName')">Project {{ sortIndicator('projectName') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('status')">Status {{ sortIndicator('status') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('status')">Stage {{ sortIndicator('status') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('assignedTo')">Assigned to {{ sortIndicator('assignedTo') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('dueAt')">Due {{ sortIndicator('dueAt') }}</button></th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in visibleAuditsForApp(app.id)" :key="a.id">
                  <td><input type="checkbox" :checked="isSelected(a.id)" @change="toggleSelected(a.id, $event.target.checked)" /></td>
                  <td>{{ a.year }}</td>
                  <td>{{ a.projectName || '-' }}</td>
                  <td>
                    <span class="badge status-badge" :class="statusBadgeClass(a.status)">
                      {{ formatAuditStatus(a.status) }}
                    </span>
                  </td>
                  <td><span class="badge text-bg-light border">{{ auditStageLabel(a.status) }}</span></td>
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
import { auditStageLabel, auditStatusBadgeClass, auditStatusLabel } from '../../utils/auditStatus'

const applications = ref([])
const projects = ref([])
const users = ref([])
const assignModal = ref(null)
const assignForm = reactive({ userId: null })
const auditsByApp = ref({})
const selectedAuditIds = ref([])
const bulkUserId = ref(null)
const projectFilter = ref('all')
const activeSort = ref({ key: 'year', direction: 'asc' })

const isAssignModalOpen = computed({
  get: () => !!assignModal.value,
  set: (open) => {
    if (!open) assignModal.value = null
  }
})

onMounted(load)

async function load() {
  const [appsRes, usersRes, projectsRes] = await Promise.all([
    api.get('/api/applications'),
    api.get('/api/users'),
    api.get('/api/audit-projects')
  ])
  applications.value = appsRes.data || []
  users.value = usersRes.data || []
  projects.value = projectsRes.data || []
  for (const app of applications.value) {
    const res = await api.get(`/api/applications/${app.id}/audits`)
    auditsByApp.value[app.id] = res.data || []
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
  const ids = visibleAuditsForApp(appId).map((a) => a.id)
  return ids.length > 0 && ids.every((id) => selectedAuditIds.value.includes(id))
}

function toggleSelectAll(appId, checked) {
  const ids = visibleAuditsForApp(appId).map((a) => a.id)
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
  return auditStatusLabel(status)
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function statusBadgeClass(status) {
  return auditStatusBadgeClass(status)
}

function visibleAuditsForApp(appId) {
  const rows = auditsByApp.value[appId] || []
  const filtered = projectFilter.value === 'all'
    ? rows
    : projectFilter.value === 'none'
      ? rows.filter((a) => !a.projectId)
      : rows.filter((a) => String(a.projectId) === projectFilter.value)
  const getterMap = {
    year: (row) => row.year,
    projectName: (row) => row.projectName || '',
    status: (row) => row.status || '',
    assignedTo: (row) => row.assignedToDisplayName || row.assignedToEmail || '',
    dueAt: (row) => row.dueAt || ''
  }
  const getter = getterMap[activeSort.value.key] || getterMap.year
  const ordered = [...filtered].sort((a, b) => {
    const av = getter(a)
    const bv = getter(b)
    if (av == null && bv == null) return 0
    if (av == null) return -1
    if (bv == null) return 1
    if (typeof av === 'number' && typeof bv === 'number') return av - bv
    const ad = Date.parse(av)
    const bd = Date.parse(bv)
    if (!Number.isNaN(ad) && !Number.isNaN(bd)) return ad - bd
    return String(av).localeCompare(String(bv), undefined, { sensitivity: 'base' })
  })
  return activeSort.value.direction === 'asc' ? ordered : ordered.reverse()
}

const filteredApplications = computed(() => {
  return applications.value.filter((app) => visibleAuditsForApp(app.id).length > 0)
})

function toggleSort(key) {
  if (activeSort.value.key === key) {
    activeSort.value.direction = activeSort.value.direction === 'asc' ? 'desc' : 'asc'
    return
  }
  activeSort.value.key = key
  activeSort.value.direction = 'asc'
}

function sortIndicator(key) {
  if (activeSort.value.key !== key) return '↕'
  return activeSort.value.direction === 'asc' ? '↑' : '↓'
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
