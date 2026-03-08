<template>
  <div>
    <h1 class="h3 mb-3">My Audits</h1>
    <p v-if="!items.length && !loading" class="text-muted">No audits assigned to you.</p>
    <div v-else class="card shadow-sm">
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>Application</th>
                <th>Year</th>
                <th>Status</th>
                <th>Completion</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in items" :key="a.id">
                <td>{{ a.applicationName }}</td>
                <td>{{ a.year }}</td>
                <td>
                  <span class="badge status-badge" :class="statusBadgeClass(a.status)">
                    {{ statusLabel(a.status) }}
                  </span>
                </td>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <div class="progress flex-grow-1" style="height: 8px; min-width: 120px;">
                      <div
                        class="progress-bar"
                        role="progressbar"
                        :style="{ width: `${a.completionPct || 0}%` }"
                        :aria-valuenow="a.completionPct || 0"
                        aria-valuemin="0"
                        aria-valuemax="100"
                      />
                    </div>
                    <span class="small text-muted">{{ a.completionPct || 0 }}%</span>
                  </div>
                </td>
                <td class="text-nowrap">
                  <router-link
                    :to="`/audits/${a.id}/respond`"
                    class="btn btn-primary btn-sm"
                    :class="{ disabled: a.status === 'SUBMITTED' || a.status === 'ATTESTED' || a.status === 'COMPLETE' }"
                  >
                    {{ actionLabel(a) }}
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../services/api'

const items = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [res, controlsCatalogRes] = await Promise.all([
      api.get('/api/my-audits'),
      api.get('/api/controls?includeQuestions=true')
    ])
    const audits = res.data || []
    const controlsCatalog = controlsCatalogRes.data || []
    const controlIdsWithAnyQuestion = new Set(
      controlsCatalog
        .filter((c) => Array.isArray(c.questions) && c.questions.length > 0)
        .map((c) => c.id)
    )
    const enriched = await Promise.all(
      audits.map(async (audit) => {
        try {
          const [questionsRes, controlsRes] = await Promise.all([
            api.get(`/api/audits/${audit.id}/questions`),
            api.get(`/api/audits/${audit.id}/controls`)
          ])
          const questions = questionsRes.data || []
          const controls = controlsRes.data || []
          const groupedQuestions = new Map()
          questions.forEach((q) => {
            if (!groupedQuestions.has(q.questionId)) {
              groupedQuestions.set(q.questionId, q.existingAnswerText || '')
            } else if (!groupedQuestions.get(q.questionId) && q.existingAnswerText) {
              groupedQuestions.set(q.questionId, q.existingAnswerText)
            }
          })
          const additionalControls = controls.filter((c) => !controlIdsWithAnyQuestion.has(c.id))
          const answeredQuestions = Array.from(groupedQuestions.values())
            .filter((a) => a && a.trim().length > 0)
            .length
          const answeredAdditional = additionalControls.filter((c) => ['PASS', 'FAIL', 'NA'].includes(c.status)).length
          const total = groupedQuestions.size + additionalControls.length
          const complete = answeredQuestions + answeredAdditional
          const completionPct = total > 0 ? Math.round((complete / total) * 100) : 0
          return { ...audit, completionPct }
        } catch (e) {
          return { ...audit, completionPct: 0 }
        }
      })
    )
    items.value = enriched
  } finally {
    loading.value = false
  }
})

function statusLabel(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'Completed - pending admin review'
    case 'COMPLETE':
      return 'Validated complete'
    case 'ATTESTED':
      return 'Attested by audit team'
    case 'IN_PROGRESS':
      return 'In progress'
    case 'DRAFT':
      return 'Draft'
    default:
      return status || '-'
  }
}

function actionLabel(audit) {
  if (audit.status === 'SUBMITTED' || audit.status === 'ATTESTED' || audit.status === 'COMPLETE') return 'View submission'
  return (audit.completionPct || 0) > 0 ? 'Resume audit' : 'Start audit'
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
