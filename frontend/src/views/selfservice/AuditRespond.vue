<template>
  <div>
    <h1 class="h3 mb-2">{{ audit?.applicationName }} - {{ audit?.year }} Assessment</h1>
    <p v-if="audit" class="text-muted mb-3">Answer the questions below. Your responses map to controls in the background.</p>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else-if="questions.length === 0" class="card shadow-sm">
      <div class="card-body">No questions are configured for this audit. Contact your administrator.</div>
    </div>

    <form v-else @submit.prevent="submit">
      <div v-for="(q, idx) in questions" :key="q.questionId + '-' + q.auditControlId" class="card shadow-sm mb-3">
        <div class="card-body">
          <div class="d-flex gap-2">
            <div class="fw-semibold">{{ idx + 1 }}.</div>
            <div class="flex-grow-1">
              <label class="form-label fw-semibold">{{ q.questionText }}</label>
              <p v-if="q.helpText" class="text-muted small mb-2">{{ q.helpText }}</p>
              <select v-model="answers[q.questionId + '-' + q.auditControlId]" class="form-select">
                <option v-for="opt in answerOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div class="d-flex gap-2">
        <button type="button" class="btn btn-secondary" @click="saveDraft">Save draft</button>
        <button type="submit" class="btn btn-primary">Submit answers</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../services/api'

const route = useRoute()
const auditId = Number(route.params.auditId)
const audit = ref(null)
const questions = ref([])
const answers = reactive({})
const loading = ref(true)
const saving = ref(false)

const answerOptions = [
  { value: 'UNANSWERED', label: 'Select an answer' },
  { value: 'YES', label: 'Yes' },
  { value: 'PARTIAL', label: 'Partially' },
  { value: 'NO', label: 'No' },
  { value: 'NOT_APPLICABLE', label: 'Not applicable' }
]

onMounted(async () => {
  try {
    const [auditRes, questionsRes] = await Promise.all([
      api.get(`/api/audits/${auditId}`),
      api.get(`/api/audits/${auditId}/questions`)
    ])
    audit.value = auditRes.data
    questions.value = questionsRes.data || []
    questions.value.forEach((q) => {
      const key = q.questionId + '-' + q.auditControlId
      answers[key] = normalizeExistingAnswer(q.existingAnswerText)
    })
  } finally {
    loading.value = false
  }
})

async function saveDraft() {
  await postAnswers()
}

async function submit() {
  await postAnswers()
}

async function postAnswers() {
  saving.value = true
  try {
    const payload = {
      answers: questions.value.map((q) => ({
        questionId: q.questionId,
        auditControlId: q.auditControlId,
        answerText: toStoredAnswer(answers[q.questionId + '-' + q.auditControlId])
      }))
    }
    await api.post(`/api/audits/${auditId}/answers`, payload)
    alert('Answers saved.')
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to save.')
  } finally {
    saving.value = false
  }
}

function normalizeExistingAnswer(value) {
  const supported = answerOptions.map((x) => x.value)
  return supported.includes(value) ? value : 'UNANSWERED'
}

function toStoredAnswer(value) {
  return value && value !== 'UNANSWERED' ? value : ''
}
</script>
