<template>
  <div>
    <h1 class="page-title">{{ audit?.applicationName }} – {{ audit?.year }} Assessment</h1>
    <p v-if="audit" class="subtitle">Answer the questions below. Your responses are saved automatically and map to the control framework in the background.</p>
    <div v-if="loading">Loading...</div>
    <div v-else-if="questions.length === 0" class="card">No questions are configured for this audit. Contact your administrator.</div>
    <form v-else @submit.prevent="submit" class="questions-form">
      <div v-for="(q, idx) in questions" :key="q.questionId + '-' + q.auditControlId" class="card question-block">
        <div class="question-number">{{ idx + 1 }}.</div>
        <div class="question-body">
          <label class="question-text">{{ q.questionText }}</label>
          <p v-if="q.helpText" class="help-text">{{ q.helpText }}</p>
          <select v-model="answers[q.questionId + '-' + q.auditControlId]" class="answer-select">
            <option v-for="opt in answerOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
      </div>
      <div class="form-actions">
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

<style scoped>
.subtitle { color: #718096; margin-bottom: 1rem; }
.questions-form { margin-top: 1rem; }
.question-block { display: flex; gap: 1rem; align-items: flex-start; }
.question-number { font-weight: 600; min-width: 2rem; }
.question-body { flex: 1; }
.question-text { display: block; font-weight: 500; margin-bottom: 0.25rem; }
.help-text { font-size: 0.9rem; color: #718096; margin: 0.25rem 0; }
.answer-select { width: 100%; padding: 0.5rem; border: 1px solid #cbd5e0; border-radius: 6px; margin-top: 0.5rem; }
.form-actions { margin-top: 1.5rem; display: flex; gap: 0.75rem; }
</style>
