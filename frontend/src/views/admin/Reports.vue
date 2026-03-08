<template>
  <div>
    <h1 class="h3 mb-3">Audit Program Reports</h1>
    <div class="d-flex gap-2 mb-3">
      <button class="btn btn-outline-primary btn-sm" @click="downloadAuditsCsv">Export Audits CSV</button>
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else>
      <div class="row g-3 mb-3">
        <div class="col-md-4" v-for="card in cards" :key="card.label">
          <div class="card shadow-sm h-100">
            <div class="card-body">
              <div class="text-muted small">{{ card.label }}</div>
              <div class="display-6 fw-semibold">{{ card.value }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="card shadow-sm">
        <div class="card-body">
          <h2 class="h5 mb-3">By Year</h2>
          <div class="table-responsive">
            <table class="table table-striped mb-0">
              <thead>
                <tr>
                  <th>Year</th>
                  <th>Total</th>
                  <th>Draft</th>
                  <th>In Progress</th>
                  <th>Submitted</th>
                  <th>Attested</th>
                  <th>Complete</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in byYear" :key="row.year">
                  <td>{{ row.year }}</td>
                  <td>{{ row.total }}</td>
                  <td>{{ row.draft }}</td>
                  <td>{{ row.inProgress }}</td>
                  <td>{{ row.submitted }}</td>
                  <td>{{ row.attested }}</td>
                  <td>{{ row.complete }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError } from '../../services/toast'

const loading = ref(true)
const summary = ref(null)
const byYear = ref([])

const cards = computed(() => {
  const s = summary.value || {}
  return [
    { label: 'Applications', value: s.totalApplications ?? 0 },
    { label: 'Total Audits', value: s.totalAudits ?? 0 },
    { label: 'Open Audits', value: s.openAudits ?? 0 },
    { label: 'Overdue Audits', value: s.overdueAudits ?? 0 },
    { label: 'Submitted', value: s.submittedAudits ?? 0 },
    { label: 'Attested', value: s.attestedAudits ?? 0 },
    { label: 'Completed', value: s.completedAudits ?? 0 }
  ]
})

onMounted(async () => {
  try {
    const [res, byYearRes] = await Promise.all([
      api.get('/api/reports/summary'),
      api.get('/api/reports/by-year')
    ])
    summary.value = res.data || {}
    byYear.value = byYearRes.data || []
  } finally {
    loading.value = false
  }
})

async function downloadAuditsCsv() {
  try {
    const res = await api.get('/api/reports/audits.csv', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'audits-report.csv'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to export report')
  }
}
</script>
