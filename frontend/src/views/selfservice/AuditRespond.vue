<template>
  <div class="respond-page w-100">
    <header class="respond-header mb-4">
      <h1 class="respond-page-title mb-2">{{ audit?.applicationName }} — {{ audit?.year }} assessment</h1>
      <p v-if="audit" class="text-muted mb-2 respond-header__meta">
        {{ statusLabel(audit.status) }}
        <span class="mx-1">·</span>
        Due {{ audit.dueAt ? new Date(audit.dueAt).toLocaleDateString() : '—' }}
      </p>
      <p v-if="pendingAuditorDisplay" class="text-muted small mb-2 respond-header__meta">
        <span class="fw-semibold text-secondary">Assigned auditor</span>
        {{ pendingAuditorDisplay }}
      </p>
      <p v-if="readOnly" class="text-muted mb-0">
        This assessment is <strong>view only</strong> while it is submitted, in review, or closed. Use
        <strong>Next</strong> to move through your answers and supporting files.
      </p>
      <p v-else-if="revisionsRequested" class="text-muted mb-0">
        The auditor has requested changes. Update your answers and evidence as needed, then use
        <strong>Submit for review</strong> when you are ready to send it back.
      </p>
      <p v-else class="text-muted mb-0">
        Answer each question, then use <strong>Save draft</strong> anytime. Optional notes and files can be added under each question.
      </p>
    </header>

    <div v-if="audit && !loading" class="card border-0 shadow-sm mb-4">
      <div class="card-body py-3 px-3 px-sm-4">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <p class="respond-label-muted mb-1">Security architecture review</p>
            <span
              v-if="audit.securityArchitectureReview"
              class="badge"
              :class="secReviewBadgeClass(audit.securityArchitectureReview.status)"
            >
              {{ secReviewLabel(audit.securityArchitectureReview.status) }}
            </span>
            <span v-else class="text-muted">—</span>
            <p v-if="audit.securityArchitectureReview?.notes" class="small text-muted mt-2 mb-0">
              {{ audit.securityArchitectureReview.notes }}
            </p>
          </div>
          <button type="button" class="btn btn-outline-secondary btn-sm" @click="toggleScopePanel">
            {{ scopePanelOpen ? 'Hide' : 'Show' }} assessment control set
          </button>
        </div>
        <div v-if="scopePanelOpen" class="mt-3 pt-3 border-top">
          <p class="small text-muted mb-2">
            Library controls for this application under the current regulatory filter. Your audit was seeded when it was created; use this as a reference for scope.
          </p>
          <div v-if="scopeLoading" class="text-muted small">Loading…</div>
          <div v-else class="table-responsive">
            <table class="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Control</th>
                  <th>Scopes</th>
                  <th>Filter</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in assessmentControlRows" :key="row.id">
                  <td class="small">
                    <span class="fw-medium">{{ row.controlId }}</span>
                    <span class="text-muted d-block">{{ row.name }}</span>
                  </td>
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
        </div>
      </div>
    </div>

    <div v-if="loading" class="text-muted py-4">Loading your assessment…</div>
    <div v-else-if="totalSteps === 0" class="card shadow-sm border-0">
      <div class="card-body">No questions are configured for this audit. Contact your administrator.</div>
    </div>

    <div v-else>
      <div
        v-if="revisionsRequested"
        class="alert alert-warning border-0 respond-revisions-banner mb-4"
        role="status"
      >
        <span class="fw-semibold">Revisions requested.</span>
        <span class="text-muted">
          The auditor returned this assessment for updates. Review any comments you received, change answers or evidence below, then submit again when complete.
        </span>
      </div>
      <div v-else-if="readOnly" class="alert alert-light border respond-readonly-banner mb-4" role="status">
        <span class="fw-semibold">Submitted or locked.</span>
        <span class="text-muted"> You can review responses below; editing is disabled.</span>
      </div>

      <!-- Progress -->
      <div class="respond-progress card respond-surface--progress mb-4">
        <div class="card-body px-3 py-3 px-sm-4">
          <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-2">
            <span class="respond-label-muted">Progress</span>
            <span class="fw-semibold">{{ completionPct }}% complete</span>
          </div>
          <div class="progress respond-progress__bar" style="height: 8px">
            <div
              class="progress-bar bg-primary"
              role="progressbar"
              :style="{ width: `${completionPct}%` }"
              :aria-valuenow="completionPct"
              aria-valuemin="0"
              aria-valuemax="100"
            />
          </div>
          <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mt-2">
            <span class="text-muted">{{ stepContextLabel }}</span>
            <span class="text-muted">Step {{ currentStepNumber }} of {{ totalSteps }}</span>
          </div>
        </div>
      </div>

      <!-- Guided questions -->
      <div v-if="currentStage === 'human'" class="card respond-surface--section mb-4">
        <div class="card-body respond-guided-canvas px-3 py-3 px-sm-4 py-md-4">
          <div class="respond-topic-intro mb-4">
            <p class="respond-label-muted mb-1">Current topic</p>
            <p class="respond-section-heading mb-2">{{ currentGuidedSection?.title }}</p>
            <p class="text-muted mb-0">{{ currentGuidedSection?.description }}</p>
          </div>

          <div
            v-for="(question, idx) in currentSectionQuestions"
            :key="question.questionId"
            class="respond-question respond-question-panel"
          >
            <div class="respond-question-panel__head d-flex align-items-center justify-content-between flex-wrap gap-2">
              <span class="respond-question-panel__label">Question {{ idx + 1 }} of {{ currentSectionQuestions.length }}</span>
            </div>
            <div class="d-flex align-items-start gap-2 mb-3">
              <span class="respond-question__index d-flex align-items-center justify-content-center flex-shrink-0">{{ idx + 1 }}</span>
              <div class="flex-grow-1 min-w-0">
                <p class="respond-question-text mb-2">{{ question.questionText }}</p>
                <p v-if="question.helpText" class="text-muted mb-0">{{ question.helpText }}</p>
              </div>
            </div>

            <p class="text-muted mb-2">
              <span class="fw-medium">Related controls:</span>
              {{ mappedControlSummary(question) }}
            </p>

            <label class="form-label" :for="'answer-' + question.questionId">{{ answerFieldLabel(question) }}</label>
            <p :id="'answer-hint-' + question.questionId" class="small text-muted mb-2">
              {{ answerFieldHint(question) }}
            </p>
            <select
              :id="'answer-' + question.questionId"
              v-model="answers[questionKey(question.questionId)]"
              class="form-select mb-0"
              :disabled="readOnly"
              :aria-describedby="'answer-hint-' + question.questionId"
            >
              <option v-for="opt in answerOptionsForQuestion(question)" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>

            <details class="respond-evidence-details mt-3">
              <summary class="respond-evidence-summary">
                <span class="respond-evidence-summary__inner">
                  <span class="respond-evidence-summary__text">Notes &amp; supporting files</span>
                  <span class="text-muted fw-normal">(optional)</span>
                  <span
                    v-if="uniqueMappings(question).length > 1"
                    class="badge rounded-pill text-bg-light border respond-evidence-summary__badge"
                  >
                    {{ uniqueMappings(question).length }} controls
                  </span>
                </span>
              </summary>
              <div class="respond-evidence-details__body pt-3">
                <p class="text-muted mb-3">
                  Add context or upload documents that help auditors validate your answer. Each control can have its own notes and files.
                </p>
                <div v-for="m in uniqueMappings(question)" :key="m.auditControlId" class="mb-3">
                  <OwnerEvidenceBlock
                    :audit-control-id="m.auditControlId"
                    :control-label="`${m.controlControlId} — ${m.controlName}`"
                    :notes="notesForControl(m.auditControlId)"
                    :read-only="readOnly"
                    compact
                    @control-updated="refreshControls"
                  />
                </div>
              </div>
            </details>
          </div>
        </div>
      </div>

      <!-- Additional controls -->
      <div v-else-if="currentAdditionalControl" class="card respond-surface--section mb-4">
        <div class="card-body respond-guided-canvas px-3 py-3 px-sm-4 py-md-4">
          <div class="respond-topic-intro respond-topic-intro--amber mb-4">
            <p class="respond-label-muted mb-1">Additional control</p>
            <p class="respond-section-heading mb-0">
              {{ currentAdditionalControl?.controlControlId }}
              <span class="fw-normal text-muted">—</span>
              {{ currentAdditionalControl?.controlName }}
            </p>
            <p class="text-muted mb-0 mt-2">
              These items don’t use the guided questions above. Set your status, then add notes or files if helpful.
            </p>
            <p class="respond-extra-step small text-muted mb-0 mt-2">
              Step {{ currentAdditionalIndex + 1 }} of {{ additionalControls.length }} in this section
            </p>
          </div>

          <div class="respond-question-panel respond-question-panel--flush">
            <div class="mb-3">
              <label class="form-label" for="additional-status">Status for this control</label>
              <select
                id="additional-status"
                v-model="additionalResponses[currentAdditionalControl.id].status"
                class="form-select"
                :disabled="readOnly"
              >
                <option value="NOT_STARTED">I have not started this yet</option>
                <option value="IN_PROGRESS">This is in progress</option>
                <option value="PASS">This is implemented and working</option>
                <option value="FAIL">This is not implemented yet</option>
                <option value="NA">This does not apply to my application</option>
              </select>
            </div>

            <details class="respond-evidence-details" open>
            <summary class="respond-evidence-summary">
              <span class="respond-evidence-summary__inner">
                <span class="respond-evidence-summary__text">Notes &amp; supporting files</span>
                <span class="text-muted fw-normal">(optional)</span>
              </span>
            </summary>
            <div class="respond-evidence-details__body pt-3">
              <OwnerEvidenceBlock
                v-if="currentAdditionalControl"
                :audit-control-id="currentAdditionalControl.id"
                :control-label="`${currentAdditionalControl.controlControlId} — ${currentAdditionalControl.controlName}`"
                :notes="additionalResponses[currentAdditionalControl.id]?.notes ?? ''"
                :read-only="readOnly"
                :show-title="false"
                compact
                @control-updated="refreshControls"
              />
            </div>
          </details>
          </div>
        </div>
      </div>
      <div v-else class="alert alert-light border">No additional controls in this audit.</div>

      <!-- Actions -->
      <div class="respond-actions card respond-surface--actions">
        <div class="card-body px-3 py-3 px-sm-4">
          <div class="d-flex justify-content-between align-items-center gap-3 flex-wrap">
            <div class="d-flex gap-2 flex-wrap">
              <button
                type="button"
                class="btn btn-outline-secondary btn-sm ws-btn-modal-secondary"
                :disabled="saving || !canGoBack"
                @click="goBack"
              >
                Back
              </button>
              <button
                v-if="!readOnly"
                type="button"
                class="btn btn-outline-primary btn-sm ws-btn-modal-secondary"
                :disabled="saving"
                @click="saveDraft"
              >
                Save draft
              </button>
            </div>
            <div class="d-flex gap-2 flex-wrap justify-content-end">
              <button
                v-if="currentStage === 'human' && (!isLastHumanStep || additionalControls.length > 0)"
                type="button"
                class="btn btn-primary btn-sm ws-btn-respond-forward"
                :disabled="saving"
                @click="goNext"
              >
                {{ isLastHumanStep ? 'Continue to additional questions' : 'Next section' }}
              </button>

              <button
                v-else-if="currentStage === 'additional' && !isLastAdditionalStep"
                type="button"
                class="btn btn-primary btn-sm ws-btn-respond-forward"
                :disabled="saving"
                @click="goNext"
              >
                Next control
              </button>

              <button
                v-else-if="!readOnly"
                type="button"
                class="btn btn-success btn-sm ws-btn-respond-forward"
                :disabled="saving"
                @click="finishAudit"
              >
                Submit for review
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../services/api'
import OwnerEvidenceBlock from '../../components/OwnerEvidenceBlock.vue'
import { toastError, toastSuccess, toastWarning } from '../../services/toast'
import { isAuditOwnerAnswerLocked } from '../../utils/auditStatus'
import { OWNER_ANSWER_VALUE_KEYS } from '../../utils/ownerAnswerOptions'

const route = useRoute()
const auditId = Number(route.params.auditId)

const audit = ref(null)
const questionItems = ref([])
const controls = ref([])
const additionalControls = ref([])
const answers = reactive({})
const additionalResponses = reactive({})
const loading = ref(true)
const saving = ref(false)
const scopePanelOpen = ref(false)
const scopeLoading = ref(false)
const assessmentControlRows = ref([])

const currentStage = ref('human') // 'human' | 'additional'
const currentHumanIndex = ref(0)
const currentAdditionalIndex = ref(0)

function answerOptionsForQuestion(question) {
  const opts = question?.ownerResponseOptions
  return Array.isArray(opts) && opts.length ? opts : []
}

function answerFieldLabel(question) {
  return question?.ownerResponseFieldLabel || 'Which option best describes your situation?'
}

function answerFieldHint(question) {
  return (
    question?.ownerResponseFieldHint ||
    'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.'
  )
}

function buildGroupedQuestions(items) {
  const byId = new Map()
  for (const item of items) {
    if (!byId.has(item.questionId)) {
      byId.set(item.questionId, {
        questionId: item.questionId,
        questionText: item.questionText,
        helpText: item.helpText,
        displayOrder: item.displayOrder ?? 0,
        ownerResponseOptions: item.ownerResponseOptions,
        ownerResponseFieldLabel: item.ownerResponseFieldLabel,
        ownerResponseFieldHint: item.ownerResponseFieldHint,
        mappings: [],
        existingAnswerText: item.existingAnswerText || ''
      })
    }
    const grouped = byId.get(item.questionId)
    grouped.mappings.push({
      auditControlId: item.auditControlId,
      controlId: item.controlId,
      controlControlId: item.controlControlId,
      controlName: item.controlName
    })
    if (!grouped.existingAnswerText && item.existingAnswerText) {
      grouped.existingAnswerText = item.existingAnswerText
    }
  }
  return Array.from(byId.values()).sort((a, b) => {
    if (a.displayOrder !== b.displayOrder) return a.displayOrder - b.displayOrder
    return a.questionId - b.questionId
  })
}

/** Library questions explicitly tagged for application owners (ask_owner = true); matches backend /questions + submit rules. */
const guidedQuestions = computed(() => {
  const ownerItems = questionItems.value.filter((i) => i.askOwner === true)
  return buildGroupedQuestions(ownerItems)
})

const guidedSections = computed(() => {
  const grouped = new Map()
  for (const question of guidedQuestions.value) {
    const section = inferQuestionSection(question)
    if (!grouped.has(section.key)) {
      grouped.set(section.key, {
        key: section.key,
        title: section.title,
        description: section.description,
        questions: []
      })
    }
    grouped.get(section.key).questions.push(question)
  }
  return Array.from(grouped.values())
})

const currentGuidedSection = computed(() => guidedSections.value[currentHumanIndex.value] || null)
const currentSectionQuestions = computed(() => currentGuidedSection.value?.questions || [])

function questionKey(questionId) {
  return `q-${questionId}`
}

function mappedControlSummary(question) {
  return (question.mappings || []).map((m) => m.controlControlId).join(', ')
}

/** One block per control when a question maps to multiple audit controls */
function uniqueMappings(question) {
  const seen = new Set()
  return (question.mappings || []).filter((m) => {
    if (seen.has(m.auditControlId)) return false
    seen.add(m.auditControlId)
    return true
  })
}

function notesForControl(auditControlId) {
  const c = controls.value.find((x) => x.id === auditControlId)
  return c?.notes ?? ''
}

async function refreshControls() {
  const controlsRes = await api.get(`/api/audits/${auditId}/controls`)
  controls.value = controlsRes.data || []
  const controlsWithOwnerQuestions = new Set(
    questionItems.value.filter((item) => item.askOwner === true).map((item) => item.auditControlId)
  )
  additionalControls.value = controls.value.filter((c) => !controlsWithOwnerQuestions.has(c.id))
  additionalControls.value.forEach((c) => {
    if (!additionalResponses[c.id]) {
      additionalResponses[c.id] = { status: c.status || 'NOT_STARTED', notes: c.notes || '' }
    } else {
      additionalResponses[c.id].notes = c.notes ?? additionalResponses[c.id].notes ?? ''
      if (c.status) additionalResponses[c.id].status = c.status
    }
  })
}

function inferQuestionSection(question) {
  const controlIds = (question.mappings || []).map((m) => m.controlControlId || '')
  if (controlIds.some((id) => id.startsWith('AC-') || id.startsWith('IA-') || id === 'PCI-7' || id === 'PCI-8' || id === 'SOX-ACCESS' || id.startsWith('HIPAA-164.308(a)(4)'))) {
    return {
      key: 'access-identity',
      title: 'Access and Identity',
      description: 'Who can access the application, and how access is validated.'
    }
  }
  if (controlIds.some((id) => id.startsWith('AU-') || id === 'PCI-10' || id === 'SOX-OPS' || id.startsWith('HIPAA-164.312(b)'))) {
    return {
      key: 'monitoring-logging',
      title: 'Monitoring and Logging',
      description: 'How activity is recorded and reviewed for unusual behavior.'
    }
  }
  if (controlIds.some((id) => id.startsWith('SI-') || id === 'PCI-5' || id === 'PCI-6' || id === 'SOX-LOGICAL')) {
    return {
      key: 'security-maintenance',
      title: 'Security Maintenance',
      description: 'How security updates, malware protection, and remediation are handled.'
    }
  }
  if (controlIds.some((id) => id.startsWith('IR-') || id.startsWith('HIPAA-164.308(a)(6)'))) {
    return {
      key: 'incident-response',
      title: 'Incident Response',
      description: 'How your team responds when a security incident occurs.'
    }
  }
  if (controlIds.some((id) => id.startsWith('AT-') || id === 'SOX-GOV' || id.startsWith('HIPAA-164.308(a)(5)'))) {
    return {
      key: 'awareness-governance',
      title: 'Awareness and Governance',
      description: 'Security awareness, roles, and day-to-day governance practices.'
    }
  }
  return {
    key: 'general-security',
    title: 'General Security Practices',
    description: 'General controls that support secure operation of the application.'
  }
}

const currentAdditionalControl = computed(() => additionalControls.value[currentAdditionalIndex.value] || null)

/** Short line under progress bar: topic name or phase */
const stepContextLabel = computed(() => {
  if (currentStage.value === 'human') {
    return currentGuidedSection.value?.title || 'Guided questions'
  }
  return 'Additional controls'
})

const humanAnsweredCount = computed(() => {
  return guidedQuestions.value.filter((q) => {
    const key = `q-${q.questionId}`
    return isHumanAnswered(answers[key])
  }).length
})

const additionalAnsweredCount = computed(() => {
  return additionalControls.value.filter((c) => {
    const response = additionalResponses[c.id]
    return response && ['PASS', 'FAIL', 'NA'].includes(response.status)
  }).length
})

const totalSteps = computed(() => guidedSections.value.length + additionalControls.value.length)
const completedSteps = computed(() => humanAnsweredCount.value + additionalAnsweredCount.value)
const completionPct = computed(() => {
  if (totalSteps.value === 0) return 0
  return Math.round((completedSteps.value / totalSteps.value) * 100)
})

const humanComplete = computed(() => humanAnsweredCount.value === guidedQuestions.value.length)
/** Owner cannot edit answers (submitted, in approval pipeline, attested, or complete). */
const readOnly = computed(() => isAuditOwnerAnswerLocked(audit.value?.status))

/** Auditor sent the assessment back for owner updates (editable). */
const revisionsRequested = computed(() => audit.value?.status === 'REVISIONS_REQUESTED')

/** Pending approval-step assignee (from API), with assignment fallback when needed. */
const pendingAuditorDisplay = computed(() => {
  const a = audit.value
  if (!a) return ''
  const f = a.pendingAuditorFirstName?.trim()
  const l = a.pendingAuditorLastName?.trim()
  if (f && l) return `${f} ${l}`
  if (f) return f
  if (l) return l
  if (a.pendingAuditorDisplayName?.trim()) return a.pendingAuditorDisplayName.trim()
  if (a.pendingAuditorEmail) return a.pendingAuditorEmail
  if (a.status === 'PENDING_APPROVAL' && a.assignments?.length) {
    const rev = a.assignments.find((x) => x.assignmentRole === 'REVIEWER')
    if (rev?.userDisplayName?.trim()) return rev.userDisplayName.trim()
    if (rev?.userEmail) return rev.userEmail
  }
  return ''
})

const isLastHumanStep = computed(() => {
  return currentHumanIndex.value >= guidedSections.value.length - 1
})

const isLastAdditionalStep = computed(() => {
  return currentAdditionalIndex.value >= additionalControls.value.length - 1
})

const currentStepNumber = computed(() => {
  if (currentStage.value === 'human') return currentHumanIndex.value + 1
  return guidedSections.value.length + currentAdditionalIndex.value + 1
})

const canGoBack = computed(() => {
  if (currentStage.value === 'human') return currentHumanIndex.value > 0
  return currentAdditionalIndex.value > 0 || guidedSections.value.length > 0
})

onMounted(load)

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

async function toggleScopePanel() {
  scopePanelOpen.value = !scopePanelOpen.value
  if (!scopePanelOpen.value || assessmentControlRows.value.length || !audit.value?.applicationId) {
    return
  }
  scopeLoading.value = true
  try {
    const res = await api.get(`/api/applications/${audit.value.applicationId}/assessment-controls`)
    assessmentControlRows.value = res.data || []
  } catch (e) {
    toastError(e.response?.data?.error || 'Could not load controls')
  } finally {
    scopeLoading.value = false
  }
}

async function load() {
  try {
    const [auditRes, questionsRes, controlsRes] = await Promise.all([
      api.get(`/api/audits/${auditId}`),
      api.get(`/api/audits/${auditId}/questions`),
      api.get(`/api/audits/${auditId}/controls`)
    ])

    audit.value = auditRes.data
    questionItems.value = questionsRes.data || []
    controls.value = controlsRes.data || []

    guidedQuestions.value.forEach((q) => {
      const key = questionKey(q.questionId)
      answers[key] = normalizeExistingAnswer(q.existingAnswerText)
    })

    const controlsWithOwnerQuestions = new Set(
      questionItems.value.filter((item) => item.askOwner === true).map((item) => item.auditControlId)
    )
    additionalControls.value = controls.value.filter((c) => !controlsWithOwnerQuestions.has(c.id))

    additionalControls.value.forEach((c) => {
      additionalResponses[c.id] = {
        status: c.status || 'NOT_STARTED',
        notes: c.notes || ''
      }
    })

    initializeStartingStep()
  } finally {
    loading.value = false
  }
}

function initializeStartingStep() {
  if (guidedSections.value.length === 0 && additionalControls.value.length > 0) {
    const firstUnansweredAdditional = additionalControls.value.findIndex((c) => {
      const response = additionalResponses[c.id]
      return !response || !['PASS', 'FAIL', 'NA'].includes(response.status)
    })
    currentStage.value = 'additional'
    currentAdditionalIndex.value = firstUnansweredAdditional >= 0 ? firstUnansweredAdditional : 0
    return
  }

  const firstUnansweredHumanQuestion = guidedQuestions.value.find((q) => {
    return !isHumanAnswered(answers[questionKey(q.questionId)])
  })

  if (firstUnansweredHumanQuestion && guidedSections.value.length > 0) {
    const sectionIndex = guidedSections.value.findIndex((section) =>
      section.questions.some((q) => q.questionId === firstUnansweredHumanQuestion.questionId)
    )
    currentStage.value = 'human'
    currentHumanIndex.value = sectionIndex >= 0 ? sectionIndex : 0
    return
  }

  if (additionalControls.value.length > 0) {
    const firstUnansweredAdditional = additionalControls.value.findIndex((c) => {
      const response = additionalResponses[c.id]
      return !response || !['PASS', 'FAIL', 'NA'].includes(response.status)
    })

    currentStage.value = 'additional'
    currentAdditionalIndex.value = firstUnansweredAdditional >= 0 ? firstUnansweredAdditional : 0
    return
  }

  currentStage.value = 'human'
  currentHumanIndex.value = 0
}

function goBack() {
  if (currentStage.value === 'additional') {
    if (currentAdditionalIndex.value > 0) {
      currentAdditionalIndex.value -= 1
      return
    }
    if (guidedSections.value.length > 0) {
      currentStage.value = 'human'
      currentHumanIndex.value = Math.max(guidedSections.value.length - 1, 0)
    }
    return
  }

  if (currentHumanIndex.value > 0) {
    currentHumanIndex.value -= 1
  }
}

function goNext() {
  if (readOnly.value) {
    if (currentStage.value === 'human') {
      if (!isLastHumanStep.value) {
        currentHumanIndex.value += 1
        return
      }
      if (additionalControls.value.length > 0) {
        currentStage.value = 'additional'
        currentAdditionalIndex.value = 0
      }
      return
    }
    if (currentStage.value === 'additional' && !isLastAdditionalStep.value) {
      currentAdditionalIndex.value += 1
    }
    return
  }

  if (currentStage.value === 'human') {
    const hasUnansweredInSection = currentSectionQuestions.value.some((question) =>
      !isHumanAnswered(answers[questionKey(question.questionId)])
    )
    if (hasUnansweredInSection) {
      toastWarning('Please answer all questions in this section before moving on.')
      return
    }

    if (!isLastHumanStep.value) {
      currentHumanIndex.value += 1
      return
    }

    if (!humanComplete.value) {
      toastWarning('Please answer all guided questions before moving to additional questions.')
      return
    }

    if (additionalControls.value.length > 0) {
      currentStage.value = 'additional'
      currentAdditionalIndex.value = 0
    }
    return
  }

  if (!isLastAdditionalStep.value) {
    currentAdditionalIndex.value += 1
  }
}

async function saveDraft() {
  if (readOnly.value) return
  await saveAllProgress()
}

async function finishAudit() {
  if (readOnly.value) return
  await saveAllProgress()

  if (completionPct.value < 100) {
    toastWarning('Please complete all remaining questions before submitting the audit.')
    return
  }

  try {
    const res = await api.post(`/api/audits/${auditId}/submit`)
    if (audit.value) audit.value.status = res.data?.status || 'PENDING_APPROVAL'
    toastSuccess('Assessment submitted for auditor approval.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to submit assessment.')
  }
}

async function saveAllProgress() {
  if (readOnly.value) return
  saving.value = true
  try {
    await saveHumanAnswers()
    await saveAdditionalControls()
    if (audit.value?.status === 'DRAFT') {
      await api.put(`/api/audits/${auditId}`, { status: 'IN_PROGRESS' })
      audit.value.status = 'IN_PROGRESS'
    }
    toastSuccess('Progress saved.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save progress.')
  } finally {
    saving.value = false
  }
}

async function saveHumanAnswers() {
  if (!guidedQuestions.value.length) return

  const payload = {
    answers: guidedQuestions.value.flatMap((q) =>
      q.mappings.map((mapping) => ({
        questionId: q.questionId,
        auditControlId: mapping.auditControlId,
        answerText: toStoredAnswer(answers[questionKey(q.questionId)])
      }))
    )
  }
  await api.post(`/api/audits/${auditId}/answers`, payload)
}

async function saveAdditionalControls() {
  if (!additionalControls.value.length) return

  const updates = additionalControls.value.map((control) => {
    const response = additionalResponses[control.id] || { status: 'NOT_STARTED', notes: '' }
    return {
      auditControlId: control.id,
      status: response.status,
      notes: response.notes
    }
  })
  await api.put(`/api/audits/${auditId}/controls/bulk`, updates)
}

function normalizeExistingAnswer(value) {
  return OWNER_ANSWER_VALUE_KEYS.includes(value) ? value : 'UNANSWERED'
}

function isHumanAnswered(value) {
  return !!(value && value !== 'UNANSWERED')
}

function toStoredAnswer(value) {
  return value && value !== 'UNANSWERED' ? value : ''
}

function statusLabel(status) {
  switch (status) {
    case 'DRAFT':
      return 'Draft'
    case 'IN_PROGRESS':
      return 'In progress'
    case 'REVISIONS_REQUESTED':
      return 'Revisions requested — please update and resubmit'
    case 'PENDING_APPROVAL':
    case 'SUBMITTED':
      return 'Submitted — pending auditor approval'
    case 'AUDITOR_APPROVED':
      return 'Auditor approved — pending attestation'
    case 'COMPLETE':
      return 'Validated complete'
    case 'ATTESTED':
      return 'Attested by audit team'
    default:
      return status || '-'
  }
}
</script>

<style scoped>
/* Full width of parent (Bootstrap container); no fixed max-width so layout adapts to viewport */
.respond-page {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  padding-bottom: 4rem;
  font-size: 1rem;
  line-height: 1.5;
  font-family: inherit;
  box-sizing: border-box;
}

.respond-page-title {
  font-size: 1.25rem;
  font-weight: 600;
  line-height: 1.35;
  margin: 0;
  color: #212529;
  overflow-wrap: anywhere;
  word-wrap: break-word;
}

.respond-label-muted {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #6c757d;
  margin: 0;
}

.respond-section-heading {
  font-size: 1.0625rem;
  font-weight: 600;
  line-height: 1.35;
  margin: 0;
  color: #212529;
  overflow-wrap: anywhere;
  word-wrap: break-word;
}

.respond-question-text {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.45;
  margin: 0;
  color: #212529;
  overflow-wrap: anywhere;
  word-wrap: break-word;
}

.respond-page .form-select,
.respond-page .form-label {
  font-size: 1rem;
  max-width: 100%;
}

.respond-question {
  min-width: 0;
}

.respond-header__meta {
  line-height: 1.4;
}

.respond-progress__bar {
  border-radius: 999px;
  overflow: hidden;
}

.respond-question__index {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: #e7f1ff;
  color: #0d6efd;
  font-size: 0.875rem;
  font-weight: 700;
}

.respond-evidence-details {
  border: 1px solid #e9ecef;
  border-radius: 0.5rem;
  background: #fafbfc;
  padding: 0.5rem 0.75rem;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.respond-evidence-summary {
  cursor: pointer;
  list-style: none;
  font-weight: 600;
  font-size: 1rem;
  line-height: 1.4;
  padding: 0.35rem 0;
  user-select: none;
}

.respond-evidence-summary__inner {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.25rem 0.5rem;
  max-width: 100%;
}

.respond-evidence-summary__badge {
  margin-left: 0 !important;
}

.respond-evidence-summary::-webkit-details-marker {
  display: none;
}

.respond-evidence-summary::before {
  content: '▸';
  display: inline-block;
  margin-right: 0.35rem;
  transition: transform 0.15s ease;
  color: #6c757d;
}

.respond-evidence-details[open] .respond-evidence-summary::before {
  transform: rotate(90deg);
}

.respond-evidence-details__body {
  border-top: 1px solid #e9ecef;
  margin-left: 0;
  padding-left: 0;
  min-width: 0;
  max-width: 100%;
}

.respond-actions {
  position: sticky;
  bottom: 0;
  z-index: 2;
  margin-top: 1rem;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.respond-page .card {
  max-width: 100%;
  min-width: 0;
}

/* Region surfaces: progress vs main section vs footer */
.respond-surface--progress {
  border: 1px solid #d8e4f5 !important;
  background: linear-gradient(180deg, #f5f9ff 0%, #ffffff 100%);
  box-shadow: 0 2px 8px rgba(13, 110, 253, 0.08);
}

.respond-surface--section {
  border: 1px solid #dee2e6 !important;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.respond-surface--actions {
  border: 1px solid #dee2e6 !important;
  background: #fafbfc;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

/* Canvas behind topic + questions */
.respond-guided-canvas {
  background: #eef1f6;
  border-radius: 0.375rem;
}

/* Topic banner (one per section) */
.respond-topic-intro {
  background: #fff;
  border-left: 4px solid #0d6efd;
  border-radius: 0.5rem;
  padding: 1rem 1.25rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.respond-topic-intro--amber {
  border-left-color: #fd7e14;
  background: linear-gradient(90deg, #fffaf5 0%, #ffffff 55%);
}

.respond-extra-step {
  border-top: 1px dashed #dee2e6;
  padding-top: 0.75rem;
}

/* Each question = distinct card on shaded canvas */
.respond-question-panel {
  background: #fff;
  border: 1px solid #d8dee6;
  border-radius: 0.5rem;
  padding: 1rem 1.15rem 1.15rem;
  margin-bottom: 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.respond-question-panel:last-child {
  margin-bottom: 0;
}

.respond-question-panel__head {
  border-bottom: 1px solid #e9ecef;
  padding-bottom: 0.65rem;
  margin-bottom: 0.75rem;
}

.respond-question-panel__label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #495057;
  background: #e9ecef;
  padding: 0.25rem 0.6rem;
  border-radius: 0.25rem;
}

.respond-question-panel--flush {
  margin-bottom: 0;
}

/* Nested evidence block: slightly inset */
.respond-question-panel .respond-evidence-details {
  background: #f8f9fb;
  border-color: #dce3ec;
}

.respond-question-panel .respond-evidence-details__body {
  border-top-color: #dce3ec;
}
</style>
