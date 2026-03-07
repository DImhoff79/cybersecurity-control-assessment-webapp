<template>
  <div>
    <h1 class="h3 mb-1">{{ audit?.applicationName }} - {{ audit?.year }} Assessment</h1>
    <p v-if="audit" class="text-muted mb-3">
      Status: {{ statusLabel(audit.status) }} | Progress: {{ completionPct }}%
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
          <p class="small text-muted mb-2">Guided questions in plain language</p>
          <div class="fw-semibold mb-2">{{ currentHumanIndex + 1 }}. {{ currentHumanQuestion?.questionText }}</div>
          <p v-if="currentHumanQuestion?.mappings?.length > 1" class="text-muted small mb-2">
            This one answer applies to {{ currentHumanQuestion.mappings.length }} related controls.
          </p>
          <p v-if="currentHumanQuestion?.helpText" class="text-muted small mb-3">{{ currentHumanQuestion.helpText }}</p>

          <label class="form-label">Your answer</label>
          <select v-model="answers[currentHumanKey]" class="form-select">
            <option v-for="opt in answerOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
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

const currentHumanQuestion = computed(() => guidedQuestions.value[currentHumanIndex.value] || null)
const currentHumanKey = computed(() => {
  if (!currentHumanQuestion.value) return ''
  return `q-${currentHumanQuestion.value.questionId}`
})

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

const totalSteps = computed(() => guidedQuestions.value.length + additionalControls.value.length)
const completedSteps = computed(() => humanAnsweredCount.value + additionalAnsweredCount.value)
const completionPct = computed(() => {
  if (totalSteps.value === 0) return 0
  return Math.round((completedSteps.value / totalSteps.value) * 100)
})

const humanComplete = computed(() => humanAnsweredCount.value === guidedQuestions.value.length)
const isSubmitted = computed(() => audit.value?.status === 'SUBMITTED' || audit.value?.status === 'COMPLETE')

const isLastHumanStep = computed(() => {
  return currentHumanIndex.value >= guidedQuestions.value.length - 1
})

const isLastAdditionalStep = computed(() => {
  return currentAdditionalIndex.value >= additionalControls.value.length - 1
})

const currentStepNumber = computed(() => {
  if (currentStage.value === 'human') return currentHumanIndex.value + 1
  return guidedQuestions.value.length + currentAdditionalIndex.value + 1
})

const canGoBack = computed(() => {
  if (currentStage.value === 'human') return currentHumanIndex.value > 0
  return currentAdditionalIndex.value > 0 || guidedQuestions.value.length > 0
})

onMounted(load)

async function load() {
  try {
    const [auditRes, questionsRes, controlsRes, controlsCatalogRes] = await Promise.all([
      api.get(`/api/audits/${auditId}`),
      api.get(`/api/audits/${auditId}/questions`),
      api.get(`/api/audits/${auditId}/controls`),
      api.get('/api/controls?includeQuestions=true')
    ])

    audit.value = auditRes.data
    questionItems.value = questionsRes.data || []
    controls.value = controlsRes.data || []
    const controlsCatalog = controlsCatalogRes.data || []

    guidedQuestions.value.forEach((q) => {
      const key = `q-${q.questionId}`
      answers[key] = normalizeExistingAnswer(q.existingAnswerText)
    })

    const controlIdsWithAnyQuestion = new Set(
      controlsCatalog
        .filter((c) => Array.isArray(c.questions) && c.questions.length > 0)
        .map((c) => c.id)
    )
    additionalControls.value = controls.value.filter((c) => !controlIdsWithAnyQuestion.has(c.id))

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
  const firstUnansweredHuman = guidedQuestions.value.findIndex((q) => {
    const key = `q-${q.questionId}`
    return !isHumanAnswered(answers[key])
  })

  if (firstUnansweredHuman >= 0) {
    currentStage.value = 'human'
    currentHumanIndex.value = firstUnansweredHuman
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
    if (guidedQuestions.value.length > 0) {
      currentStage.value = 'human'
      currentHumanIndex.value = Math.max(guidedQuestions.value.length - 1, 0)
    }
    return
  }

  if (currentHumanIndex.value > 0) {
    currentHumanIndex.value -= 1
  }
}

function goNext() {
  if (currentStage.value === 'human') {
    if (!isLastHumanStep.value) {
      currentHumanIndex.value += 1
      return
    }

    if (!humanComplete.value) {
      alert('Please answer all guided questions before moving to additional questions.')
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
    alert('Please complete all remaining questions before submitting the audit.')
    return
  }

  try {
    const res = await api.post(`/api/audits/${auditId}/submit`)
    if (audit.value) audit.value.status = res.data?.status || 'SUBMITTED'
    alert('Assessment submitted. An admin will now review it.')
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to submit assessment.')
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
    alert('Progress saved.')
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to save progress.')
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
        answerText: toStoredAnswer(answers[`q-${q.questionId}`])
      }))
    )
  }
  await api.post(`/api/audits/${auditId}/answers`, payload)
}

async function saveAdditionalControls() {
  if (!additionalControls.value.length) return

  await Promise.all(
    additionalControls.value.map((control) => {
      const response = additionalResponses[control.id] || { status: 'NOT_STARTED', notes: '' }
      return api.put(`/api/audit-controls/${control.id}`, {
        status: response.status,
        notes: response.notes
      })
    })
  )
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
    case 'SUBMITTED':
      return 'Submitted - pending admin review'
    default:
      return status || '-'
  }
}
</script>
