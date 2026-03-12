<template>
  <div>
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
          <div>
            <h1 class="h3 mb-1">{{ audit?.applicationName }} - {{ audit?.year }}</h1>
            <p v-if="audit" class="text-muted assessment-meta mb-2">
              <span class="badge text-bg-light border me-2">{{ statusLabel(audit.status) }}</span>
              Assigned to {{ audit.assignedToDisplayName || audit.assignedToEmail || '-' }} | Due {{ formatDate(audit.dueAt) }}
            </p>
          </div>
          <div class="assessment-top-actions">
            <button
              class="btn btn-outline-success btn-sm"
              :disabled="!audit || (audit.status !== 'SUBMITTED' && audit.status !== 'ATTESTED')"
              @click="attestAudit"
            >
              Attest Audit
            </button>
            <button
              class="btn btn-success btn-sm"
              :disabled="!audit || (audit.status !== 'ATTESTED' && audit.status !== 'SUBMITTED')"
              @click="markComplete"
            >
              Mark Complete
            </button>
          </div>
        </div>
        <ul class="nav nav-pills mt-2">
          <li class="nav-item">
            <button type="button" class="nav-link" :class="{ active: activeWorkspaceTab === 'overview' }" @click="activeWorkspaceTab = 'overview'">
              Overview
            </button>
          </li>
          <li class="nav-item">
            <button type="button" class="nav-link" :class="{ active: activeWorkspaceTab === 'controls' }" @click="activeWorkspaceTab = 'controls'">
              Controls & Evidence
            </button>
          </li>
          <li class="nav-item">
            <button type="button" class="nav-link" :class="{ active: activeWorkspaceTab === 'timeline' }" @click="activeWorkspaceTab = 'timeline'">
              Timeline
            </button>
          </li>
        </ul>
      </div>
    </div>

    <div v-if="activeWorkspaceTab === 'overview'" class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Collaborators</h2>
        <div v-if="!assignments.length" class="text-muted small mb-2">No collaborators assigned.</div>
        <div v-for="as in assignments" :key="as.id" class="d-flex justify-content-between align-items-center border-top pt-2 mt-2 small gap-2">
          <div>
            <strong>{{ as.userDisplayName || as.userEmail }}</strong>
            <span class="badge text-bg-light border ms-2">{{ as.assignmentRole }}</span>
          </div>
          <button class="btn btn-outline-danger btn-sm" @click="removeAssignment(as.id)">Remove</button>
        </div>
        <div class="row g-2 mt-2">
          <div class="col-md-6">
            <select v-model="newAssignment.userId" class="form-select form-select-sm">
              <option :value="null">Select user</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <select v-model="newAssignment.role" class="form-select form-select-sm">
              <option value="DELEGATE">DELEGATE</option>
              <option value="REVIEWER">REVIEWER</option>
              <option value="PRIMARY">PRIMARY</option>
            </select>
          </div>
          <div class="col-md-2">
            <button class="btn btn-primary btn-sm w-100" @click="addAssignment">Add</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else-if="activeWorkspaceTab === 'controls'">
      <div v-if="!auditControls.length" class="card shadow-sm">
        <div class="card-body text-muted">No controls are linked to this audit yet.</div>
      </div>
      <div v-else class="controls-stack">
        <div v-for="ac in auditControls" :key="ac.id" class="card shadow-sm">
          <div class="card-header d-flex justify-content-between align-items-center gap-2 flex-wrap">
            <div>
              <span class="badge text-bg-light border me-2">{{ ac.controlControlId }}</span>
              <button class="btn btn-link p-0 text-decoration-none fw-semibold" type="button" @click="openControlDetails(ac.controlId)">
                {{ ac.controlName }}
              </button>
            </div>
            <div class="control-status-select">
              <select :value="ac.status" @change="updateStatus(ac, $event.target.value)" class="form-select form-select-sm">
                <option value="NOT_STARTED">Not started</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="PASS">Pass</option>
                <option value="FAIL">Fail</option>
                <option value="NA">N/A</option>
              </select>
            </div>
          </div>
          <div class="card-body">
            <div class="row g-3">
              <div class="col-12 col-xl-4">
                <h3 class="h6 mb-2">Task Delegation</h3>
                <div class="border rounded p-2">
                  <div v-if="!(ac.assignments || []).length" class="text-muted small mb-1">No task assignees.</div>
                  <div v-for="ta in ac.assignments || []" :key="ta.id" class="small d-flex justify-content-between align-items-center border-top pt-1 mt-1 gap-2">
                    <span>{{ ta.userDisplayName || ta.userEmail }} ({{ ta.assignmentRole }})</span>
                    <button class="btn btn-outline-danger btn-sm" @click="removeControlAssignment(ac, ta.id)">Remove</button>
                  </div>
                  <div class="row g-1 mt-1">
                    <div class="col-12">
                      <select v-model="controlAssignmentDraft(ac.id).userId" class="form-select form-select-sm">
                        <option :value="null">Select user</option>
                        <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
                      </select>
                    </div>
                    <div class="col-8">
                      <select v-model="controlAssignmentDraft(ac.id).role" class="form-select form-select-sm">
                        <option value="CONTRIBUTOR">CONTRIBUTOR</option>
                        <option value="REVIEWER">REVIEWER</option>
                      </select>
                    </div>
                    <div class="col-4">
                      <button class="btn btn-primary btn-sm w-100" @click="addControlAssignment(ac)">Add</button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="col-12 col-xl-5">
                <h3 class="h6 mb-2">Assessment Notes</h3>
                <textarea
                  :value="ac.notes"
                  @blur="updateNotes(ac, $event.target.value)"
                  rows="2"
                  placeholder="Notes / evidence"
                  class="form-control form-control-sm mb-2"
                />
                <h3 class="h6 mb-2 mt-3">Owner Answers</h3>
                <div class="border rounded p-2 answer-panel">
                  <div v-for="a in ac.answers" :key="a.id" class="small mb-2">
                    <strong>Q:</strong> {{ a.questionText }}<br />
                    <strong>A:</strong> {{ a.answerText || '-' }}
                  </div>
                  <span v-if="!ac.answers?.length" class="text-muted">-</span>
                </div>
              </div>
              <div class="col-12 col-xl-3">
                <h3 class="h6 mb-2">File Evidence Upload</h3>
                <div class="border rounded p-2">
                  <p class="small text-muted mb-2">
                    Upload only. Include a clear description (min 8 chars). Max file size 10MB.
                  </p>
                  <div v-if="!ac.evidences?.length" class="text-muted small mb-2">No evidence attached.</div>
                  <div v-for="ev in ac.evidences || []" :key="ev.id" class="small border-top pt-2 mt-2">
                    <div>
                      <strong>{{ ev.fileName || ev.title }}</strong>
                      <span v-if="ev.stale" class="badge text-bg-danger ms-1">Stale</span>
                      <span v-else-if="ev.expiresAt" class="badge text-bg-light border ms-1">Fresh</span>
                      <span class="badge text-bg-light border ms-1">{{ ev.lifecycleStatus || 'ACTIVE' }}</span>
                      <span v-if="ev.legalHold" class="badge text-bg-dark ms-1">Legal Hold</span>
                    </div>
                    <div class="text-muted">{{ ev.notes || '-' }}</div>
                    <div class="text-muted">Review: {{ ev.reviewStatus }}</div>
                    <div class="text-muted">Expires: {{ formatDateTime(ev.expiresAt) }}</div>
                    <div class="text-muted">Retention: {{ formatDateTime(ev.retentionUntil) }}</div>
                    <div class="mt-1">
                      <button
                        type="button"
                        class="btn btn-outline-secondary btn-sm me-1"
                        :disabled="!ev.uri || ev.lifecycleStatus === 'DISPOSED'"
                        @click="downloadEvidence(ev)"
                      >
                        Download
                      </button>
                      <button class="btn btn-outline-success btn-sm me-1" @click="reviewEvidence(ac, ev, 'ACCEPTED')" :disabled="ev.reviewStatus === 'ACCEPTED'">
                        Accept
                      </button>
                      <button class="btn btn-outline-danger btn-sm" @click="reviewEvidence(ac, ev, 'REJECTED')" :disabled="ev.reviewStatus === 'REJECTED'">
                        Reject
                      </button>
                    </div>
                    <div class="mt-1">
                      <button class="btn btn-outline-dark btn-sm me-1" @click="toggleLegalHold(ac, ev, !ev.legalHold)">
                        {{ ev.legalHold ? 'Release Hold' : 'Legal Hold' }}
                      </button>
                      <button
                        class="btn btn-outline-secondary btn-sm me-1"
                        @click="archiveEvidence(ac, ev)"
                        :disabled="ev.lifecycleStatus === 'ARCHIVED' || ev.lifecycleStatus === 'DISPOSED' || ev.legalHold"
                      >
                        Archive
                      </button>
                      <button
                        class="btn btn-outline-danger btn-sm"
                        @click="disposeEvidence(ac, ev)"
                        :disabled="ev.lifecycleStatus !== 'ARCHIVED' || ev.legalHold"
                      >
                        Dispose
                      </button>
                    </div>
                  </div>
                  <div class="row g-2 mt-2">
                    <div class="col-12">
                      <textarea
                        v-model="evidenceDraft(ac.id).description"
                        class="form-control form-control-sm"
                        rows="2"
                        placeholder="Evidence description"
                      />
                    </div>
                    <div class="col-12">
                      <input type="file" accept=".pdf,.txt,.csv,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx" class="form-control form-control-sm" @change="setEvidenceFile(ac.id, $event)" />
                    </div>
                    <div class="col-12">
                      <button class="btn btn-outline-primary btn-sm w-100" @click="uploadEvidence(ac)">Upload File</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="activeWorkspaceTab === 'timeline'" class="card shadow-sm mt-3">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
          <h2 class="h5 mb-0">Activity Timeline</h2>
          <div class="d-flex gap-2 align-items-center flex-wrap">
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
              placeholder="Search actor or details..."
            />
          </div>
        </div>
        <div class="small text-muted mb-2">
          Showing {{ filteredActivityLogs.length }} of {{ activityLogs.length }} events
        </div>
        <div v-if="!filteredActivityLogs.length" class="text-muted">No matching activity events.</div>
        <div v-for="log in filteredActivityLogs" :key="log.id" class="border-top pt-2 mt-2 small">
          <div><strong>{{ log.activityType }}</strong> - {{ log.details || '-' }}</div>
          <div class="text-muted">{{ formatDateTime(log.createdAt) }} by {{ log.actorEmail || 'system' }}</div>
        </div>
      </div>
    </div>

    <BsModal v-model="isDetailsOpen" :title="detailsTitle" size="lg">
      <p><strong>Framework:</strong> {{ detailsModal?.framework }}</p>
      <p><strong>Category:</strong> {{ detailsModal?.category || 'General' }}</p>
      <p><strong>Description:</strong> {{ detailsModal?.description || 'No description available yet.' }}</p>

      <h3 class="h6">Examples / Guidance</h3>
      <ul v-if="detailExamples.length">
        <li v-for="(item, idx) in detailExamples" :key="idx">{{ item }}</li>
      </ul>
      <p v-else class="text-muted">No examples available yet. Add plain-English questions/help text for this control.</p>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isDetailsOpen = false">Close</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess, toastWarning } from '../../services/toast'

const route = useRoute()
const auditId = Number(route.params.auditId)
const audit = ref(null)
const auditControls = ref([])
const loading = ref(true)
const activeWorkspaceTab = ref('controls')
const detailsModal = ref(null)
const activityLogs = ref([])
const activityFilter = ref({
  category: 'all',
  search: ''
})
const evidenceDrafts = ref({})
const evidenceFiles = ref({})
const controlAssignmentDrafts = ref({})
const assignments = ref([])
const users = ref([])
const newAssignment = ref({ userId: null, role: 'DELEGATE' })

const isDetailsOpen = computed({
  get: () => !!detailsModal.value,
  set: (open) => {
    if (!open) detailsModal.value = null
  }
})

const detailsTitle = computed(() => {
  if (!detailsModal.value) return 'Control details'
  return `${detailsModal.value.controlId} - ${detailsModal.value.name}`
})

onMounted(async () => {
  try {
    const [auditRes, controlsRes, logsRes, assignmentsRes, usersRes] = await Promise.all([
      api.get(`/api/audits/${auditId}`),
      api.get(`/api/audits/${auditId}/controls`),
      api.get(`/api/audits/${auditId}/activity-logs`),
      api.get(`/api/audits/${auditId}/assignments`),
      api.get('/api/users')
    ])
    audit.value = auditRes.data
    auditControls.value = controlsRes.data || []
    activityLogs.value = logsRes.data || []
    assignments.value = assignmentsRes.data || []
    users.value = usersRes.data || []
  } finally {
    loading.value = false
  }
})

async function addAssignment() {
  if (!newAssignment.value.userId) {
    toastWarning('Select a user first.')
    return
  }
  try {
    const res = await api.post(`/api/audits/${auditId}/assignments`, {
      userId: newAssignment.value.userId,
      role: newAssignment.value.role
    })
    assignments.value = res.data || []
    newAssignment.value.userId = null
    newAssignment.value.role = 'DELEGATE'
    toastSuccess('Collaborator added.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to add collaborator')
  }
}

async function removeAssignment(assignmentId) {
  try {
    const res = await api.delete(`/api/audits/${auditId}/assignments/${assignmentId}`)
    assignments.value = res.data || []
    toastSuccess('Collaborator removed.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to remove collaborator')
  }
}

async function updateStatus(ac, status) {
  try {
    await api.put(`/api/audit-controls/${ac.id}`, { status })
    const idx = auditControls.value.findIndex((x) => x.id === ac.id)
    if (idx >= 0) auditControls.value[idx].status = status
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update')
  }
}

function controlAssignmentDraft(auditControlId) {
  if (!controlAssignmentDrafts.value[auditControlId]) {
    controlAssignmentDrafts.value[auditControlId] = { userId: null, role: 'CONTRIBUTOR' }
  }
  return controlAssignmentDrafts.value[auditControlId]
}

async function addControlAssignment(ac) {
  const draft = controlAssignmentDraft(ac.id)
  if (!draft.userId) {
    toastWarning('Select a user for task delegation.')
    return
  }
  try {
    await api.post(`/api/audit-controls/${ac.id}/assignments`, {
      userId: draft.userId,
      role: draft.role
    })
    draft.userId = null
    await reloadAuditControl(ac.id)
    toastSuccess('Task assignment added.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to add task assignment')
  }
}

async function removeControlAssignment(ac, assignmentId) {
  try {
    await api.delete(`/api/audit-controls/${ac.id}/assignments/${assignmentId}`)
    await reloadAuditControl(ac.id)
    toastSuccess('Task assignment removed.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to remove task assignment')
  }
}

async function reviewEvidence(ac, evidence, reviewStatus) {
  try {
    await api.put(`/api/evidences/${evidence.id}/review`, { reviewStatus })
    await reloadAuditControl(ac.id)
    await loadActivityLogs()
    toastSuccess(`Evidence ${reviewStatus === 'ACCEPTED' ? 'accepted' : 'rejected'}.`)
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to review evidence')
  }
}

async function toggleLegalHold(ac, evidence, legalHold) {
  try {
    await api.put(`/api/evidences/${evidence.id}/lifecycle/legal-hold`, { legalHold })
    await reloadAuditControl(ac.id)
    await loadActivityLogs()
    toastSuccess(legalHold ? 'Legal hold enabled.' : 'Legal hold released.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update legal hold')
  }
}

async function archiveEvidence(ac, evidence) {
  try {
    await api.put(`/api/evidences/${evidence.id}/lifecycle/archive`)
    await reloadAuditControl(ac.id)
    await loadActivityLogs()
    toastSuccess('Evidence archived.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to archive evidence')
  }
}

async function disposeEvidence(ac, evidence) {
  if (!confirm('Dispose this evidence? File content will be removed.')) return
  try {
    await api.put(`/api/evidences/${evidence.id}/lifecycle/dispose`)
    await reloadAuditControl(ac.id)
    await loadActivityLogs()
    toastSuccess('Evidence disposed.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to dispose evidence')
  }
}

function setEvidenceFile(auditControlId, event) {
  const file = event.target?.files?.[0] || null
  evidenceFiles.value[auditControlId] = file
}

async function uploadEvidence(ac) {
  const file = evidenceFiles.value[ac.id]
  const description = evidenceDraft(ac.id).description?.trim()
  if (!file) {
    toastWarning('Select a file to upload.')
    return
  }
  if (!description) {
    toastWarning('Enter an evidence description.')
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  formData.append('description', description)
  try {
    await api.post(`/api/audit-controls/${ac.id}/evidences/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    evidenceFiles.value[ac.id] = null
    evidenceDraft(ac.id).description = ''
    await reloadAuditControl(ac.id)
    await loadActivityLogs()
    toastSuccess('Evidence file uploaded.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to upload evidence file')
  }
}

async function downloadEvidence(evidence) {
  if (!evidence?.uri || evidence.lifecycleStatus === 'DISPOSED') {
    toastWarning('Disposed evidence is no longer downloadable.')
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

function evidenceDraft(auditControlId) {
  if (!evidenceDrafts.value[auditControlId]) {
    evidenceDrafts.value[auditControlId] = { description: '' }
  }
  return evidenceDrafts.value[auditControlId]
}

async function reloadAuditControl(auditControlId) {
  const res = await api.get(`/api/audits/${auditId}/controls`)
  auditControls.value = res.data || []
  if (!auditControls.value.find((x) => x.id === auditControlId)) {
    await loadActivityLogs()
  }
}

async function loadActivityLogs() {
  const logsRes = await api.get(`/api/audits/${auditId}/activity-logs`)
  activityLogs.value = logsRes.data || []
}

async function attestAudit() {
  try {
    const statement = prompt('Optional attestation statement', 'Audit reviewed and attested.')
    const res = await api.post(`/api/audits/${auditId}/attest`, { statement: statement || '' })
    audit.value = res.data
    await loadActivityLogs()
    toastSuccess('Audit attested.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to attest audit')
  }
}

async function markComplete() {
  try {
    const res = await api.put(`/api/audits/${auditId}`, { status: 'COMPLETE' })
    audit.value = res.data
    await loadActivityLogs()
    toastSuccess('Audit marked complete.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to mark complete')
  }
}

async function updateNotes(ac, notes) {
  if (notes === ac.notes) return
  try {
    await api.put(`/api/audit-controls/${ac.id}`, { notes })
    const idx = auditControls.value.findIndex((x) => x.id === ac.id)
    if (idx >= 0) auditControls.value[idx].notes = notes
    toastSuccess('Notes updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update')
  }
}

const detailExamples = computed(() => {
  if (!detailsModal.value?.questions?.length) return []
  return detailsModal.value.questions
    .map((q) => q.helpText || q.questionText)
    .filter(Boolean)
    .slice(0, 5)
})

const filteredActivityLogs = computed(() => {
  const filter = activityFilter.value
  const term = filter.search.trim().toLowerCase()
  return activityLogs.value.filter((log) => {
    const type = String(log.activityType || '')
    if (filter.category !== 'all') {
      const matchesCategory =
        (filter.category === 'finding' && type.startsWith('FINDING_')) ||
        (filter.category === 'exception' && type.startsWith('EXCEPTION_')) ||
        (filter.category === 'evidence' && type.startsWith('EVIDENCE_')) ||
        (filter.category === 'audit' && type.startsWith('AUDIT_'))
      if (!matchesCategory) return false
    }
    if (!term) return true
    const haystack = `${type} ${log.details || ''} ${log.actorEmail || ''}`.toLowerCase()
    return haystack.includes(term)
  })
})

async function openControlDetails(controlId) {
  try {
    const res = await api.get(`/api/controls/${controlId}?includeQuestions=true`)
    detailsModal.value = res.data
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load control details')
  }
}

function statusLabel(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'Completed - pending admin review'
    case 'COMPLETE':
      return 'Validated complete'
    case 'ATTESTED':
      return 'Attested'
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

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.assessment-meta {
  line-height: 1.5;
}

.assessment-top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.controls-stack {
  display: grid;
  gap: 0.9rem;
}

.control-status-select {
  min-width: 170px;
}

.answer-panel {
  max-height: 360px;
  overflow: auto;
}
</style>
