<template>
  <div>
    <h1 class="h3 mb-1">{{ audit?.applicationName }} - {{ audit?.year }} Assessment</h1>
    <p v-if="audit" class="text-muted mb-3">
      Status: {{ statusLabel(audit.status) }} | Progress: {{ completionPct }}%
      <span v-if="audit.dueAt"> | Due: {{ new Date(audit.dueAt).toLocaleDateString() }}</span>
    </p>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else-if="totalSteps === 0" class="card shadow-sm">
      <div class="card-body">No questions are configured for this audit. Contact your administrator.</div>
    </div>
    <div v-else-if="isSubmitted" class="card shadow-sm">
      <div class="card-body">
        <p class="mb-2 fw-semibold">This assessment was already submitted.</p>
        <p class="text-muted mb-0">It is now waiting for admin review and validation.</p>
      </div>
    </div>

    <div v-else>
      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <strong>Step {{ currentStepNumber }} of {{ totalSteps }}</strong>
            <span class="small text-muted">{{ completionPct }}% complete</span>
          </div>
          <div class="progress" style="height: 10px;">
            <div
              class="progress-bar"
              role="progressbar"
              :style="{ width: `${completionPct}%` }"
              :aria-valuenow="completionPct"
              aria-valuemin="0"
              aria-valuemax="100"
            />
          </div>
        </div>
      </div>

      <div v-if="currentStage === 'human'" class="card shadow-sm mb-3">
        <div class="card-body">
          <p class="small text-muted mb-2">Guided questions in plain language (grouped by topic)</p>
          <div class="fw-semibold mb-1">{{ currentGuidedSection?.title }}</div>
          <p class="text-muted small mb-3">{{ currentGuidedSection?.description }}</p>

          <div
            v-for="(question, idx) in currentSectionQuestions"
            :key="question.questionId"
            class="border rounded p-3 mb-3"
          >
            <div class="fw-semibold mb-2">{{ idx + 1 }}. {{ question.questionText }}</div>
            <p v-if="question.mappings?.length > 1" class="text-muted small mb-2">
              This answer applies to {{ question.mappings.length }} related controls: {{ mappedControlSummary(question) }}.
            </p>
            <p v-else class="text-muted small mb-2">
              Control: {{ mappedControlSummary(question) }}
            </p>
            <p v-if="question.helpText" class="text-muted small mb-3">{{ question.helpText }}</p>

            <label class="form-label">Your answer</label>
            <select v-model="answers[questionKey(question.questionId)]" class="form-select">
              <option v-for="opt in answerOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
        </div>
      </div>

      <div v-else class="card shadow-sm mb-3">
        <div class="card-body">
          <p class="small text-muted mb-2">Additional controls without simple mapped questions</p>
          <div class="fw-semibold mb-3">
            {{ currentAdditionalIndex + 1 }}. {{ currentAdditionalControl?.controlControlId }} - {{ currentAdditionalControl?.controlName }}
          </div>

          <div class="mb-3">
            <label class="form-label">What best describes your current state for this area?</label>
            <select v-model="additionalResponses[currentAdditionalControl.id].status" class="form-select">
              <option value="NOT_STARTED">I have not started this yet</option>
              <option value="IN_PROGRESS">This is in progress</option>
              <option value="PASS">This is implemented and working</option>
              <option value="FAIL">This is not implemented yet</option>
              <option value="NA">This does not apply to my application</option>
            </select>
          </div>

          <div>
            <label class="form-label">Notes or evidence (optional but helpful)</label>
            <textarea
              v-model="additionalResponses[currentAdditionalControl.id].notes"
              rows="4"
              class="form-control"
              placeholder="Add details that will help the assessor understand your current state."
            />
          </div>
        </div>
      </div>

      <div class="d-flex gap-2 flex-wrap">
        <button type="button" class="btn btn-secondary" :disabled="saving || !canGoBack" @click="goBack">Back</button>
        <button type="button" class="btn btn-secondary" :disabled="saving" @click="saveDraft">Save draft</button>

        <button
          v-if="currentStage === 'human' && (!isLastHumanStep || additionalControls.length > 0)"
          type="button"
          class="btn btn-primary"
          :disabled="saving"
          @click="goNext"
        >
          {{ isLastHumanStep ? 'Continue to additional questions' : 'Next' }}
        </button>

        <button
          v-else-if="currentStage === 'additional' && !isLastAdditionalStep"
          type="button"
          class="btn btn-primary"
          :disabled="saving"
          @click="goNext"
        >
          Next
        </button>

        <button
          v-else
          type="button"
          class="btn btn-success"
          :disabled="saving"
          @click="finishAudit"
        >
          Submit assessment for review
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../services/api'
import { toastError, toastSuccess, toastWarning } from '../../services/toast'

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

const currentStage = ref('human') // 'human' | 'additional'
const currentHumanIndex = ref(0)
const currentAdditionalIndex = ref(0)

const answerOptions = [
  { value: 'UNANSWERED', label: 'Choose an answer' },
  { value: 'YES', label: 'Yes, this is in place' },
  { value: 'PARTIAL', label: 'Partially in place' },
  { value: 'NO', label: 'No, this is not in place yet' },
  { value: 'NOT_APPLICABLE', label: 'Not applicable to my application' }
]

const guidedQuestions = computed(() => {
  const byId = new Map()
  for (const item of questionItems.value) {
    if (!byId.has(item.questionId)) {
      byId.set(item.questionId, {
        questionId: item.questionId,
        questionText: item.questionText,
        helpText: item.helpText,
        displayOrder: item.displayOrder ?? 0,
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
const isSubmitted = computed(() => ['SUBMITTED', 'ATTESTED', 'COMPLETE'].includes(audit.value?.status))

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

    const controlsWithOwnerQuestions = new Set(questionItems.value.map((item) => item.auditControlId))
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
  await saveAllProgress()
}

async function finishAudit() {
  await saveAllProgress()

  if (completionPct.value < 100) {
    toastWarning('Please complete all remaining questions before submitting the audit.')
    return
  }

  try {
    const res = await api.post(`/api/audits/${auditId}/submit`)
    if (audit.value) audit.value.status = res.data?.status || 'SUBMITTED'
    toastSuccess('Assessment submitted. An admin will now review it.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to submit assessment.')
  }
}

async function saveAllProgress() {
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
  const supported = answerOptions.map((x) => x.value)
  return supported.includes(value) ? value : 'UNANSWERED'
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
    case 'COMPLETE':
      return 'Validated complete'
    case 'ATTESTED':
      return 'Attested by audit team'
    case 'SUBMITTED':
      return 'Submitted - pending admin review'
    default:
      return status || '-'
  }
}
</script>
