<template>
  <div>
    <h1 class="h3 mb-2">{{ audit?.applicationName }} - {{ audit?.year }} (Assessment view)</h1>
    <p v-if="audit" class="text-muted assessment-meta">
      Status: {{ statusLabel(audit.status) }} |
      Assigned to: {{ audit.assignedToDisplayName || audit.assignedToEmail || '-' }} |
      Due: {{ formatDate(audit.dueAt) }}
    </p>
    <div class="mb-3 assessment-top-actions">
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

    <div class="card shadow-sm mb-3">
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
    <div v-else class="card shadow-sm">
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0 assessment-table">
            <thead>
              <tr>
                <th>Control</th>
                <th>Name</th>
                <th>Status</th>
                <th>Evidence / Notes</th>
                <th>Owner answers</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ac in auditControls" :key="ac.id">
                <td>{{ ac.controlControlId }}</td>
                <td>
                  <button class="btn btn-link p-0 text-decoration-none" type="button" @click="openControlDetails(ac.controlId)">
                    {{ ac.controlName }}
                  </button>
                </td>
                <td>
                  <select :value="ac.status" @change="updateStatus(ac, $event.target.value)" class="form-select form-select-sm">
                    <option value="NOT_STARTED">Not started</option>
                    <option value="IN_PROGRESS">In progress</option>
                    <option value="PASS">Pass</option>
                    <option value="FAIL">Fail</option>
                    <option value="NA">N/A</option>
                  </select>
                  <div class="mt-2 border rounded p-2">
                    <div class="small fw-semibold mb-1">Task Delegation</div>
                    <div v-if="!(ac.assignments || []).length" class="text-muted small mb-1">No task assignees.</div>
                    <div v-for="ta in ac.assignments || []" :key="ta.id" class="small d-flex justify-content-between align-items-center border-top pt-1 mt-1">
                      <span>{{ ta.userDisplayName || ta.userEmail }} ({{ ta.assignmentRole }})</span>
                      <button class="btn btn-outline-danger btn-sm" @click="removeControlAssignment(ac, ta.id)">Remove</button>
                    </div>
                    <div class="row g-1 mt-1">
                      <div class="col-12 col-lg-5">
                        <select v-model="controlAssignmentDraft(ac.id).userId" class="form-select form-select-sm">
                          <option :value="null">Select user</option>
                          <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
                        </select>
                      </div>
                      <div class="col-8 col-lg-4">
                        <select v-model="controlAssignmentDraft(ac.id).role" class="form-select form-select-sm">
                          <option value="CONTRIBUTOR">CONTRIBUTOR</option>
                          <option value="REVIEWER">REVIEWER</option>
                        </select>
                      </div>
                      <div class="col-4 col-lg-3">
                        <button class="btn btn-primary btn-sm w-100" @click="addControlAssignment(ac)">Add</button>
                      </div>
                    </div>
                  </div>
                </td>
                <td>
                  <textarea
                    :value="ac.notes"
                    @blur="updateNotes(ac, $event.target.value)"
                    rows="2"
                    placeholder="Notes / evidence"
                    class="form-control form-control-sm"
                  />
                  <div class="mt-2 border rounded p-2">
                    <div class="small fw-semibold mb-1">Structured Evidence</div>
                    <div v-if="!ac.evidences?.length" class="text-muted small mb-2">No evidence attached.</div>
                    <div v-for="ev in ac.evidences || []" :key="ev.id" class="small border-top pt-2 mt-2">
                      <div><strong>{{ ev.title }}</strong> ({{ ev.evidenceType }})</div>
                      <div v-if="ev.uri"><a :href="ev.uri" target="_blank" rel="noopener noreferrer">{{ ev.uri }}</a></div>
                      <div class="text-muted">Review: {{ ev.reviewStatus }}</div>
                      <div class="mt-1">
                        <button
                          class="btn btn-outline-success btn-sm me-1"
                          @click="reviewEvidence(ac, ev, 'ACCEPTED')"
                          :disabled="ev.reviewStatus === 'ACCEPTED'"
                        >
                          Accept
                        </button>
                        <button
                          class="btn btn-outline-danger btn-sm"
                          @click="reviewEvidence(ac, ev, 'REJECTED')"
                          :disabled="ev.reviewStatus === 'REJECTED'"
                        >
                          Reject
                        </button>
                      </div>
                    </div>

                    <div class="row g-2 mt-2">
                      <div class="col-12 col-xl-3">
                        <select v-model="evidenceDraft(ac.id).evidenceType" class="form-select form-select-sm">
                          <option value="DOCUMENT">DOCUMENT</option>
                          <option value="SCREENSHOT">SCREENSHOT</option>
                          <option value="URL">URL</option>
                          <option value="TICKET">TICKET</option>
                          <option value="OTHER">OTHER</option>
                        </select>
                      </div>
                      <div class="col-12 col-xl-4">
                        <input v-model="evidenceDraft(ac.id).title" class="form-control form-control-sm" placeholder="Evidence title" />
                      </div>
                      <div class="col-12 col-xl-4">
                        <input v-model="evidenceDraft(ac.id).uri" class="form-control form-control-sm" placeholder="https://..." />
                      </div>
                      <div class="col-12 col-xl-1">
                        <button class="btn btn-primary btn-sm w-100" @click="addEvidence(ac)">Add</button>
                      </div>
                    </div>
                    <div class="row g-2 mt-2">
                      <div class="col-12 col-xl-10">
                        <input type="file" class="form-control form-control-sm" @change="setEvidenceFile(ac.id, $event)" />
                      </div>
                      <div class="col-12 col-xl-2">
                        <button class="btn btn-outline-primary btn-sm w-100" @click="uploadEvidence(ac)">Upload File</button>
                      </div>
                    </div>
                  </div>
                </td>
                <td>
                  <div v-for="a in ac.answers" :key="a.id" class="small mb-2">
                    <strong>Q:</strong> {{ a.questionText }}<br />
                    <strong>A:</strong> {{ a.answerText || '-' }}
                  </div>
                  <span v-if="!ac.answers?.length">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="card shadow-sm mt-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Activity Timeline</h2>
        <div v-if="!activityLogs.length" class="text-muted">No activity yet.</div>
        <div v-for="log in activityLogs" :key="log.id" class="border-top pt-2 mt-2 small">
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
const detailsModal = ref(null)
const activityLogs = ref([])
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

async function addEvidence(ac) {
  const draft = evidenceDraft(ac.id)
  if (!draft.title?.trim()) {
    toastWarning('Evidence title is required.')
    return
  }
  try {
    await api.post(`/api/audit-controls/${ac.id}/evidences`, {
      evidenceType: draft.evidenceType,
      title: draft.title,
      uri: draft.uri || null
    })
    draft.title = ''
    draft.uri = ''
    await reloadAuditControl(ac.id)
    await loadActivityLogs()
    toastSuccess('Evidence added.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to add evidence')
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

function setEvidenceFile(auditControlId, event) {
  const file = event.target?.files?.[0] || null
  evidenceFiles.value[auditControlId] = file
}

async function uploadEvidence(ac) {
  const file = evidenceFiles.value[ac.id]
  if (!file) {
    toastWarning('Select a file to upload.')
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  formData.append('evidenceType', evidenceDraft(ac.id).evidenceType || 'DOCUMENT')
  formData.append('title', evidenceDraft(ac.id).title || file.name)
  try {
    await api.post(`/api/audit-controls/${ac.id}/evidences/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    evidenceFiles.value[ac.id] = null
    evidenceDraft(ac.id).title = ''
    await reloadAuditControl(ac.id)
    await loadActivityLogs()
    toastSuccess('Evidence file uploaded.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to upload evidence file')
  }
}

function evidenceDraft(auditControlId) {
  if (!evidenceDrafts.value[auditControlId]) {
    evidenceDrafts.value[auditControlId] = { evidenceType: 'DOCUMENT', title: '', uri: '' }
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

.assessment-table td {
  vertical-align: top;
}

.assessment-table th:nth-child(1),
.assessment-table td:nth-child(1) {
  min-width: 90px;
}

.assessment-table th:nth-child(2),
.assessment-table td:nth-child(2) {
  min-width: 200px;
}

.assessment-table th:nth-child(3),
.assessment-table td:nth-child(3) {
  min-width: 320px;
}

.assessment-table th:nth-child(4),
.assessment-table td:nth-child(4) {
  min-width: 430px;
}

.assessment-table th:nth-child(5),
.assessment-table td:nth-child(5) {
  min-width: 280px;
}
</style>
