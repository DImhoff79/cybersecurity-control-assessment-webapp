<template>
  <div>
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
          <div>
            <h1 class="h3 mb-1">{{ audit?.applicationName }} - {{ audit?.year }}</h1>
            <p v-if="audit" class="text-muted assessment-meta mb-2">
              <span class="badge text-bg-light border me-2">{{ auditStatusLabel(audit.status) }}</span>
              <span class="badge text-bg-light border me-2">Stage: {{ auditStageLabel(audit.status) }}</span>
              Assigned to {{ audit.assignedToDisplayName || audit.assignedToEmail || '-' }} | Due {{ formatDate(audit.dueAt) }}
            </p>
          </div>
          <div class="assessment-top-actions">
            <button
              class="btn btn-outline-success btn-sm"
              :disabled="!audit || (audit.status !== 'AUDITOR_APPROVED' && audit.status !== 'ATTESTED')"
              @click="attestAudit"
            >
              Attest Audit
            </button>
            <button
              class="btn btn-success btn-sm"
              :disabled="!audit || audit.status !== 'ATTESTED'"
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

    <!-- Audit cockpit: shared program context (findings, risks, exceptions) for every persona with access -->
    <div v-if="audit && !loading" class="card shadow-sm mb-3 border-secondary-subtle">
      <div class="card-body">
        <h2 class="h6 text-secondary text-uppercase mb-3">Program context</h2>
        <p class="small text-muted mb-3">
          Same data as the risk and findings registers—scoped to this application and audit. Use the links to open the full lists.
        </p>
        <div v-if="cockpitLoading" class="text-muted small">Loading context…</div>
        <div v-else class="row g-3">
          <div class="col-md-4">
            <div class="border rounded p-3 h-100 bg-light bg-opacity-50">
              <div class="fw-semibold mb-2">Findings</div>
              <div class="h4 mb-2">{{ activeFindingsCount }}</div>
              <p class="small text-muted mb-2">Open or in progress for this audit.</p>
              <ul v-if="cockpitFindingsPreview.length" class="small mb-2 ps-3">
                <li v-for="f in cockpitFindingsPreview" :key="f.id">{{ f.title || `Finding #${f.id}` }}</li>
              </ul>
              <div class="d-flex flex-wrap gap-2">
                <button type="button" class="btn btn-outline-primary btn-sm" @click="goTimeline('finding')">Timeline</button>
                <router-link
                  v-if="canManageAudits"
                  class="btn btn-outline-secondary btn-sm"
                  :to="{ name: 'AdminFindings', query: { auditId: String(auditId) } }"
                >
                  All findings
                </router-link>
                <router-link
                  v-else-if="hasReportView"
                  class="btn btn-outline-secondary btn-sm"
                  :to="{ name: 'AdminIssueProgram', query: { auditId: String(auditId) } }"
                >
                  Issue program
                </router-link>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="border rounded p-3 h-100 bg-light bg-opacity-50">
              <div class="fw-semibold mb-2">Risk register</div>
              <div class="h4 mb-2">{{ activeRisksCount }}</div>
              <p class="small text-muted mb-2">Risks linked to this application (open or monitoring).</p>
              <ul v-if="cockpitRisksPreview.length" class="small mb-2 ps-3">
                <li v-for="r in cockpitRisksPreview" :key="r.id">{{ r.title || `Risk #${r.id}` }}</li>
              </ul>
              <router-link
                v-if="hasReportView && audit.applicationId"
                class="btn btn-outline-secondary btn-sm"
                :to="{
                  name: 'AdminRiskRegister',
                  query: { applicationId: String(audit.applicationId), applicationName: audit.applicationName || '' }
                }"
              >
                Open risk register
              </router-link>
              <p v-else class="small text-muted mb-0">Risk list requires report access.</p>
            </div>
          </div>
          <div class="col-md-4">
            <div class="border rounded p-3 h-100 bg-light bg-opacity-50">
              <div class="fw-semibold mb-2">Control exceptions</div>
              <div class="h4 mb-2">{{ pendingExceptionsCount }}</div>
              <p class="small text-muted mb-2">Requests awaiting decision for this audit.</p>
              <ul v-if="cockpitExceptionsPreview.length" class="small mb-2 ps-3">
                <li v-for="x in cockpitExceptionsPreview" :key="x.id">{{ x.controlName || x.controlId }} — {{ x.status }}</li>
              </ul>
              <div class="d-flex flex-wrap gap-2">
                <button type="button" class="btn btn-outline-primary btn-sm" @click="goTimeline('exception')">Timeline</button>
                <router-link
                  v-if="canManageAudits"
                  class="btn btn-outline-secondary btn-sm"
                  :to="{ name: 'AdminControlExceptions', query: { auditId: String(auditId) } }"
                >
                  Program exceptions
                </router-link>
                <router-link
                  v-else-if="authStore.hasRole('AUDITOR')"
                  class="btn btn-outline-secondary btn-sm"
                  to="/admin/workspace-exceptions"
                >
                  My exceptions
                </router-link>
                <router-link v-else class="btn btn-outline-secondary btn-sm" to="/my-exceptions">My exceptions</router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="activeWorkspaceTab === 'overview' && canManageAudits" class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-2">Primary auditor</h2>
        <p class="small text-muted mb-3">
          The lead auditor for this engagement (workbench, reminders, and primary assignment). Reassign anytime.
        </p>
        <div class="row g-2 align-items-end flex-wrap">
          <div class="col-md-6">
            <label class="form-label small mb-1">Auditor</label>
            <select v-model="primaryAuditorDraft" class="form-select form-select-sm">
              <option :value="null">Unassigned</option>
              <option v-for="u in auditorPool" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <button
              type="button"
              class="btn btn-primary btn-sm"
              :disabled="primaryAuditorDraft === (audit?.assignedToUserId ?? null) || primaryAuditorSaving"
              @click="savePrimaryAuditor"
            >
              {{ primaryAuditorSaving ? 'Saving…' : 'Save assignment' }}
            </button>
          </div>
        </div>
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
              <option :value="null">Select auditor</option>
              <option v-for="u in auditorPool" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
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

    <div v-if="activeWorkspaceTab === 'overview'" class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Approval workflow</h2>
        <div v-if="approvalLoading" class="text-muted small">Loading workflow…</div>
        <template v-else>
          <ol v-if="approvalSteps.length" class="mb-3 small">
            <li v-for="s in approvalSteps" :key="s.id" class="mb-1">
              <strong>{{ s.assignedUserDisplayName || s.assignedUserEmail }}</strong>
              <span class="badge text-bg-light border ms-2">{{ s.stepStatus }}</span>
              <span v-if="s.decisionNotes" class="text-muted d-block"> — {{ s.decisionNotes }}</span>
            </li>
          </ol>
          <p v-else class="text-muted small">
            No approval steps yet. Managers can define them below, or a step is created automatically from an active REVIEWER
            with role Auditor when the owner submits.
          </p>

          <div v-if="canManageAudits && canEditWorkflow" class="border rounded p-3 mb-3 bg-light">
            <h3 class="h6">Edit approvers (auditors)</h3>
            <p class="small text-muted mb-2">Build an ordered list. Save replaces the workflow for this audit.</p>
            <div class="d-flex flex-wrap gap-2 align-items-center mb-2">
              <select v-model="workflowAddUserId" class="form-select form-select-sm" style="max-width: 280px">
                <option :value="null">Add auditor…</option>
                <option v-for="u in auditorPool" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
              </select>
              <button type="button" class="btn btn-sm btn-outline-primary" @click="addWorkflowStep">Add</button>
            </div>
            <ul v-if="workflowDraftIds.length" class="list-group mb-2">
              <li
                v-for="(uid, idx) in workflowDraftIds"
                :key="`${uid}-${idx}`"
                class="list-group-item d-flex justify-content-between align-items-center py-1"
              >
                <span>{{ workflowUserLabel(uid) }}</span>
                <button type="button" class="btn btn-sm btn-outline-danger" @click="removeWorkflowStep(idx)">Remove</button>
              </li>
            </ul>
            <button type="button" class="btn btn-primary btn-sm" :disabled="!workflowDraftIds.length" @click="saveWorkflow">
              Save workflow
            </button>
          </div>

          <div v-if="canActOnApproval" class="d-flex flex-wrap gap-2 align-items-start">
            <button type="button" class="btn btn-success btn-sm" @click="approvalApprove">Approve</button>
            <div class="d-flex flex-wrap gap-2 align-items-center">
              <textarea
                v-model="returnNotes"
                class="form-control form-control-sm"
                rows="2"
                placeholder="Notes when returning for revision (required)"
                style="min-width: 240px"
              />
              <button type="button" class="btn btn-warning btn-sm" @click="approvalReturn">Return for revision</button>
            </div>
          </div>
        </template>
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
              <div class="col-12 col-xl-6">
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
              <div class="col-12 col-xl-6">
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
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess, toastWarning } from '../../services/toast'
import { useAuthStore } from '../../stores/auth'
import { auditStageLabel, auditStatusLabel } from '../../utils/auditStatus'

const route = useRoute()
const authStore = useAuthStore()
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
const assignments = ref([])
const users = ref([])
const newAssignment = ref({ userId: null, role: 'DELEGATE' })
const approvalSteps = ref([])
const approvalLoading = ref(true)
const auditorPool = ref([])
const workflowDraftIds = ref([])
const workflowAddUserId = ref(null)
const returnNotes = ref('')
const primaryAuditorDraft = ref(null)
const primaryAuditorSaving = ref(false)

const cockpitLoading = ref(false)
const cockpitFindings = ref([])
const cockpitRisks = ref([])
const cockpitExceptions = ref([])

const hasReportView = computed(() => authStore.hasPermission('REPORT_VIEW'))
const canManageAudits = computed(() => authStore.hasPermission('AUDIT_MANAGEMENT'))

const cockpitFindingsActive = computed(() =>
  cockpitFindings.value.filter((f) => f.status === 'OPEN' || f.status === 'IN_PROGRESS')
)
const activeFindingsCount = computed(() => cockpitFindingsActive.value.length)
const cockpitFindingsPreview = computed(() => cockpitFindingsActive.value.slice(0, 5))

const cockpitRisksForApp = computed(() => {
  const aid = audit.value?.applicationId
  if (aid == null) return []
  return cockpitRisks.value.filter(
    (r) => r.applicationId === aid && (r.status === 'OPEN' || r.status === 'MONITORING')
  )
})
const activeRisksCount = computed(() => cockpitRisksForApp.value.length)
const cockpitRisksPreview = computed(() => cockpitRisksForApp.value.slice(0, 5))

const cockpitExceptionsPending = computed(() => cockpitExceptions.value.filter((x) => x.status === 'REQUESTED'))
const pendingExceptionsCount = computed(() => cockpitExceptionsPending.value.length)
const cockpitExceptionsPreview = computed(() => cockpitExceptionsPending.value.slice(0, 5))
const canEditWorkflow = computed(() => audit.value && audit.value.status !== 'COMPLETE')
const pendingStep = computed(() => approvalSteps.value.find((s) => s.stepStatus === 'PENDING'))
const canActOnApproval = computed(() => {
  if (!audit.value || audit.value.status !== 'PENDING_APPROVAL') return false
  const p = pendingStep.value
  if (!p) return false
  const uid = authStore.user?.id
  if (canManageAudits.value) return true
  return uid != null && p.assignedUserId === uid
})

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

watch(
  () => audit.value?.assignedToUserId,
  (id) => {
    primaryAuditorDraft.value = id ?? null
  },
  { immediate: true }
)

onMounted(async () => {
  try {
    const [auditRes, controlsRes, logsRes, assignmentsRes, auditorsRes] = await Promise.all([
      api.get(`/api/audits/${auditId}`),
      api.get(`/api/audits/${auditId}/controls`),
      api.get(`/api/audits/${auditId}/activity-logs`),
      api.get(`/api/audits/${auditId}/assignments`),
      api.get('/api/users/auditors').catch(() => ({ data: [] }))
    ])
    audit.value = auditRes.data
    auditControls.value = controlsRes.data || []
    activityLogs.value = logsRes.data || []
    assignments.value = assignmentsRes.data || []
    auditorPool.value = auditorsRes.data || []
    users.value = auditorPool.value
    await loadApproval()
    await loadCockpit()
  } finally {
    loading.value = false
  }
})

async function loadCockpit() {
  if (!audit.value) return
  cockpitLoading.value = true
  try {
    const [fr, rr, er] = await Promise.all([
      api.get('/api/workspace/findings', { params: { auditId } }),
      hasReportView.value ? api.get('/api/risks') : Promise.resolve({ data: [] }),
      api.get('/api/workspace/control-exceptions')
    ])
    cockpitFindings.value = fr.data || []
    cockpitRisks.value = rr.data || []
    cockpitExceptions.value = (er.data || []).filter((e) => e.auditId === auditId)
  } catch {
    cockpitFindings.value = []
    cockpitRisks.value = []
    cockpitExceptions.value = []
  } finally {
    cockpitLoading.value = false
  }
}

function goTimeline(category) {
  activeWorkspaceTab.value = 'timeline'
  activityFilter.value.category = category
}

async function loadApproval() {
  approvalLoading.value = true
  try {
    const res = await api.get(`/api/audits/${auditId}/approval/workflow`)
    approvalSteps.value = res.data || []
    if (canManageAudits.value) {
      workflowDraftIds.value = approvalSteps.value.map((s) => s.assignedUserId)
    }
  } catch {
    approvalSteps.value = []
  } finally {
    approvalLoading.value = false
  }
}

function workflowUserLabel(uid) {
  const u = auditorPool.value.find((x) => x.id === uid) || users.value.find((x) => x.id === uid)
  return u ? u.displayName || u.email : `User #${uid}`
}

function addWorkflowStep() {
  if (workflowAddUserId.value == null) return
  workflowDraftIds.value = [...workflowDraftIds.value, workflowAddUserId.value]
  workflowAddUserId.value = null
}

function removeWorkflowStep(idx) {
  workflowDraftIds.value = workflowDraftIds.value.filter((_, i) => i !== idx)
}

async function saveWorkflow() {
  if (!workflowDraftIds.value.length) {
    toastWarning('Add at least one auditor.')
    return
  }
  try {
    await api.put(`/api/audits/${auditId}/approval/workflow`, { assigneeUserIds: workflowDraftIds.value })
    toastSuccess('Approval workflow saved.')
    await loadApproval()
    await loadActivityLogs()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save workflow')
  }
}

async function approvalApprove() {
  try {
    const res = await api.post(`/api/audits/${auditId}/approval/approve`, {})
    audit.value = res.data
    toastSuccess('Approval recorded.')
    await loadApproval()
    await loadActivityLogs()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to approve')
  }
}

async function approvalReturn() {
  const notes = returnNotes.value?.trim()
  if (!notes) {
    toastWarning('Enter notes for the application owner.')
    return
  }
  try {
    const res = await api.post(`/api/audits/${auditId}/approval/return`, { notes })
    audit.value = res.data
    returnNotes.value = ''
    toastSuccess('Returned for revision.')
    await loadApproval()
    await loadActivityLogs()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to return audit')
  }
}

async function savePrimaryAuditor() {
  if (!audit.value || primaryAuditorSaving.value) return
  primaryAuditorSaving.value = true
  try {
    const res = await api.put(`/api/audits/${auditId}/assign`, { userId: primaryAuditorDraft.value })
    audit.value = res.data
    const assignmentsRes = await api.get(`/api/audits/${auditId}/assignments`)
    assignments.value = assignmentsRes.data || []
    toastSuccess('Primary auditor updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update assignment')
  } finally {
    primaryAuditorSaving.value = false
  }
}

async function addAssignment() {
  if (!newAssignment.value.userId) {
    toastWarning('Select an auditor first.')
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
